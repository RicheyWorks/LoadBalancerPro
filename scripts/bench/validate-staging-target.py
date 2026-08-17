#!/usr/bin/env python3
"""Validate a staging qualification profile and optionally resolve its private target."""

from __future__ import annotations

import argparse
import ipaddress
import json
import re
import socket
import sys
from pathlib import Path
from typing import Any


PRIVATE_ROOTS = tuple(
    ipaddress.ip_network(value)
    for value in ("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "fc00::/7")
)
SCENARIOS = (
    "steady",
    "burst",
    "slow",
    "failure",
    "reload",
    "drain",
    "restart",
    "certificateRotation",
    "candidateRollout",
    "priorRollback",
)
HOOKS = ("verify-deployment", "rollout-candidate", "rollback-prior", "slow", "failure",
         "reload", "drain", "restart", "certificate-rotation", "reset")
METHODS = {"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"}
STRATEGIES = {
    "TAIL_LATENCY_POWER_OF_TWO",
    "WEIGHTED_LEAST_LOAD",
    "WEIGHTED_LEAST_CONNECTIONS",
    "WEIGHTED_ROUND_ROBIN",
    "ROUND_ROBIN",
    "CONSISTENT_HASH",
}
SECRET_KEY = re.compile(r"(?:api.?key|password|secret|token|credential|private.?key)", re.IGNORECASE)
HOST = re.compile(r"(?=^.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$")
SHA256 = re.compile(r"^[0-9a-f]{64}$")
REVISION = re.compile(r"^[0-9a-f]{40}$")
METRIC = re.compile(r"^[a-z][a-z0-9_:]{2,127}$")
REGISTRY_REPOSITORY = re.compile(
    r"^(?=.{3,255}$)[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?(?::[1-9][0-9]{0,4})?"
    r"(?:/[a-z0-9]+(?:[._-][a-z0-9]+)*)+$"
)
PRODUCTION_NAME = re.compile(r"(^|[.-])(prod|production|live)([.-]|$)")
REQUIRED_DEPLOYMENT_SIGNALS = {
    "lbp_proxy_requests_total",
    "lbp_proxy_latency_seconds_count",
    "lbp_proxy_inflight",
    "lbp_proxy_retries_total",
    "lbp_proxy_sheds_total",
    "lbp_proxy_limit_rejections_total",
    "process_cpu_usage",
    "jvm_memory_used_bytes",
    "jvm_gc_pause_seconds_count",
}

Network = ipaddress.IPv4Network | ipaddress.IPv6Network
Address = ipaddress.IPv4Address | ipaddress.IPv6Address


class ContractError(ValueError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ContractError(message)


def object_at(value: Any, name: str) -> dict[str, Any]:
    require(isinstance(value, dict), f"{name} must be an object")
    return value


def nonempty(value: Any, name: str) -> str:
    require(isinstance(value, str) and bool(value.strip()), f"{name} must be a non-empty string")
    return value.strip()


def positive_int(value: Any, name: str, minimum: int = 1) -> int:
    require(isinstance(value, int) and not isinstance(value, bool) and value >= minimum,
            f"{name} must be an integer >= {minimum}")
    return value


def positive_number(value: Any, name: str) -> float:
    require(isinstance(value, (int, float)) and not isinstance(value, bool) and value > 0,
            f"{name} must be a positive number")
    return float(value)


def reject_embedded_secrets(value: Any, path: str = "$") -> None:
    if isinstance(value, dict):
        for key, child in value.items():
            require(not SECRET_KEY.search(str(key)),
                    f"{path}.{key} looks like embedded secret material; use runtime secret files")
            reject_embedded_secrets(child, f"{path}.{key}")
    elif isinstance(value, list):
        for index, child in enumerate(value):
            reject_embedded_secrets(child, f"{path}[{index}]")


def private_network(value: str) -> ipaddress.IPv4Network | ipaddress.IPv6Network:
    try:
        network = ipaddress.ip_network(value, strict=True)
    except ValueError as exc:
        raise ContractError(f"target.allowedCidrs contains invalid network {value!r}") from exc
    require(any(network.version == root.version and network.subnet_of(root) for root in PRIVATE_ROOTS),
            f"target.allowedCidrs network {network} is outside RFC1918/ULA private space")
    return network


def validate(profile: dict[str, Any]) -> tuple[str, int, list[Network]]:
    require(profile.get("schemaVersion") == 1, "schemaVersion must be 1")
    profile_id = nonempty(profile.get("profileId"), "profileId")
    require(bool(re.fullmatch(r"[a-z0-9][a-z0-9._-]{0,62}", profile_id)), "profileId is not safe")
    reject_embedded_secrets(profile)

    environment = object_at(profile.get("environment"), "environment")
    require(environment.get("classification") == "non-production-staging",
            "environment.classification must be non-production-staging")
    environment_name = nonempty(environment.get("name"), "environment.name").lower()
    require(not PRODUCTION_NAME.search(environment_name), "environment.name looks production-like")
    nonempty(environment.get("changeTicket"), "environment.changeTicket")
    require(isinstance(environment.get("billableImpactReviewed"), bool),
            "environment.billableImpactReviewed must be boolean")
    require(isinstance(environment.get("cleanupAuthority"), str),
            "environment.cleanupAuthority must be a string")
    require(environment.get("productionTrafficAuthorized") is False,
            "productionTrafficAuthorized must remain false")

    review = object_at(profile.get("review"), "review")
    nonempty(review.get("status"), "review.status")
    require(isinstance(review.get("approvedBy"), str), "review.approvedBy must be a string")
    require(isinstance(review.get("approvedAt"), str), "review.approvedAt must be a string")

    artifact = object_at(profile.get("artifact"), "artifact")
    require(set(artifact) == {"registryRepository", "prior", "candidate"},
            "artifact must contain a registry repository and exactly prior/candidate identities")
    repository = nonempty(artifact.get("registryRepository"), "artifact.registryRepository")
    require(bool(REGISTRY_REPOSITORY.fullmatch(repository)),
            "artifact.registryRepository must be a lowercase registry/repository without a tag or digest")
    artifact_digests: list[str] = []
    for name in ("prior", "candidate"):
        identity = object_at(artifact.get(name), f"artifact.{name}")
        require(set(identity) == {"imageDigest", "sourceRevision"},
                f"artifact.{name} must contain exactly imageDigest and sourceRevision")
        digest = nonempty(identity.get("imageDigest"), f"artifact.{name}.imageDigest")
        require(digest.startswith("sha256:") and bool(SHA256.fullmatch(digest.removeprefix("sha256:"))),
                f"artifact.{name}.imageDigest must be a sha256 digest")
        require(bool(REVISION.fullmatch(nonempty(identity.get("sourceRevision"),
                                                 f"artifact.{name}.sourceRevision"))),
                f"artifact.{name}.sourceRevision must be a 40-character lowercase Git revision")
        artifact_digests.append(digest)
    require(len(set(artifact_digests)) == 2, "prior and candidate image digests must differ")

    deployment = object_at(profile.get("deployment"), "deployment")
    require(set(deployment) == {"replicas", "minimumZones", "maximumZoneSkew", "ingressIdentitySha256",
                                "configurationSha256", "resources", "observability", "rollout"},
            "deployment contains unexpected or missing fields")
    replicas = positive_int(deployment.get("replicas"), "deployment.replicas", 2)
    minimum_zones = positive_int(deployment.get("minimumZones"), "deployment.minimumZones", 2)
    require(minimum_zones <= replicas, "deployment.minimumZones cannot exceed replicas")
    maximum_zone_skew = positive_int(deployment.get("maximumZoneSkew"),
                                     "deployment.maximumZoneSkew")
    require(maximum_zone_skew <= 1, "deployment.maximumZoneSkew must remain at one")
    for name in ("ingressIdentitySha256", "configurationSha256"):
        require(bool(SHA256.fullmatch(nonempty(deployment.get(name), f"deployment.{name}"))),
                f"deployment.{name} must be a lowercase SHA-256")
    resources = object_at(deployment.get("resources"), "deployment.resources")
    require(set(resources) == {"cpuRequestMillis", "cpuLimitMillis", "memoryRequestMiB",
                               "memoryLimitMiB"},
            "deployment.resources contains unexpected or missing fields")
    cpu_request = positive_int(resources.get("cpuRequestMillis"),
                               "deployment.resources.cpuRequestMillis")
    cpu_limit = positive_int(resources.get("cpuLimitMillis"),
                             "deployment.resources.cpuLimitMillis")
    memory_request = positive_int(resources.get("memoryRequestMiB"),
                                  "deployment.resources.memoryRequestMiB")
    memory_limit = positive_int(resources.get("memoryLimitMiB"),
                                "deployment.resources.memoryLimitMiB")
    require(cpu_request <= cpu_limit, "deployment CPU request cannot exceed its limit")
    require(memory_request <= memory_limit, "deployment memory request cannot exceed its limit")
    observability = object_at(deployment.get("observability"), "deployment.observability")
    require(set(observability) == {"requiredSignals"},
            "deployment.observability must contain exactly requiredSignals")
    required_signals = observability.get("requiredSignals")
    require(isinstance(required_signals, list) and len(required_signals) == len(set(required_signals)),
            "deployment.observability.requiredSignals must be a unique array")
    require(all(isinstance(signal, str) and bool(METRIC.fullmatch(signal)) for signal in required_signals),
            "deployment.observability.requiredSignals contains an invalid metric name")
    require(REQUIRED_DEPLOYMENT_SIGNALS.issubset(set(required_signals)),
            "deployment observability omits a required proxy or resource signal")
    rollout = object_at(deployment.get("rollout"), "deployment.rollout")
    require(set(rollout) == {"maximumUnavailable", "maximumSurge", "maximumRolloutSeconds",
                             "maximumRollbackSeconds"},
            "deployment.rollout contains unexpected or missing fields")
    require(rollout.get("maximumUnavailable") == 0,
            "deployment rollout must keep maximumUnavailable at zero")
    require(rollout.get("maximumSurge") == 1,
            "deployment rollout must keep maximumSurge at one")
    maximum_rollout_seconds = positive_int(rollout.get("maximumRolloutSeconds"),
                                           "deployment.rollout.maximumRolloutSeconds", 10)
    maximum_rollback_seconds = positive_int(rollout.get("maximumRollbackSeconds"),
                                            "deployment.rollout.maximumRollbackSeconds", 10)

    target = object_at(profile.get("target"), "target")
    require(target.get("scheme") == "https", "target.scheme must be https")
    host = nonempty(target.get("host"), "target.host").lower()
    tls_name = nonempty(target.get("tlsServerName"), "target.tlsServerName").lower()
    require(bool(HOST.fullmatch(host)) and bool(HOST.fullmatch(tls_name)), "target host names are invalid")
    require(not PRODUCTION_NAME.search(host), "target.host looks production-like")
    require(not PRODUCTION_NAME.search(tls_name),
            "target.tlsServerName looks production-like")
    require(host not in {"localhost", "lbp.local"}, "staging target cannot be loopback")
    port = positive_int(target.get("port"), "target.port")
    require(port <= 65535, "target.port must be <= 65535")
    cidrs = target.get("allowedCidrs")
    require(isinstance(cidrs, list) and bool(cidrs), "target.allowedCidrs must be a non-empty array")
    networks = [private_network(nonempty(value, "target.allowedCidrs[]")) for value in cidrs]

    workload = object_at(profile.get("workload"), "workload")
    routes = workload.get("routeMix")
    require(isinstance(routes, list) and bool(routes), "workload.routeMix must be non-empty")
    total = 0
    strategies: set[str] = set()
    for index, route_value in enumerate(routes):
        route = object_at(route_value, f"workload.routeMix[{index}]")
        path = nonempty(route.get("path"), f"workload.routeMix[{index}].path")
        require(path.startswith("/proxy/"), "all staging paths must remain below /proxy/")
        method = nonempty(route.get("method"), f"workload.routeMix[{index}].method")
        require(method in METHODS, f"unsupported route method {method}")
        require(route.get("retryEligible") is (method in {"GET", "HEAD"}),
                "retryEligible must match idempotent GET/HEAD behavior")
        require(route.get("affinity") is False, "this staging runner cannot qualify affinity")
        strategy = nonempty(route.get("strategy"), f"workload.routeMix[{index}].strategy")
        require(strategy in STRATEGIES, f"unsupported routing strategy {strategy}")
        strategies.add(strategy)
        total += positive_int(route.get("percent"), f"workload.routeMix[{index}].percent")
    require(total == 100, "route percentages must total 100")
    require(len(strategies) == 1, "one staging run can qualify only one routing strategy")

    payload = object_at(workload.get("payload"), "workload.payload")
    for direction, maximum in (("requestBytes", 65_536), ("responseBytes", 1_048_576)):
        values = object_at(payload.get(direction), f"workload.payload.{direction}")
        ordered = [positive_int(values.get(key), f"workload.payload.{direction}.{key}", 0)
                   for key in ("p50", "p95", "p99")]
        require(ordered == sorted(ordered) and ordered[-1] <= maximum,
                f"workload.payload.{direction} percentiles are unordered or exceed {maximum}")

    scenarios = object_at(profile.get("scenarios"), "scenarios")
    require(set(scenarios) == set(SCENARIOS), "scenarios must contain exactly the required staging cases")
    for name in SCENARIOS:
        scenario = object_at(scenarios[name], f"scenarios.{name}")
        positive_int(scenario.get("ratePerSecond"), f"scenarios.{name}.ratePerSecond")
        positive_int(scenario.get("durationSeconds"), f"scenarios.{name}.durationSeconds", 30)
        positive_number(scenario.get("p99Millis"), f"scenarios.{name}.p99Millis")
        ratio = positive_number(scenario.get("minimumSuccessRatio"), f"scenarios.{name}.minimumSuccessRatio")
        require(ratio <= 1, f"scenarios.{name}.minimumSuccessRatio must be <= 1")
    require(scenarios["candidateRollout"]["durationSeconds"] >= maximum_rollout_seconds + 10,
            "candidateRollout traffic must outlive the maximum rollout window by ten seconds")
    require(scenarios["priorRollback"]["durationSeconds"] >= maximum_rollback_seconds + 10,
            "priorRollback traffic must outlive the maximum rollback window by ten seconds")

    hooks = object_at(profile.get("hooks"), "hooks")
    require(set(hooks) == set(HOOKS), "hooks must contain exactly the required staging actions")
    for name in HOOKS:
        require(bool(SHA256.fullmatch(nonempty(hooks[name], f"hooks.{name}"))),
                f"hooks.{name} must be a lowercase SHA-256")

    thresholds = object_at(profile.get("thresholds"), "thresholds")
    positive_number(thresholds.get("maximumProxyOverheadP99Millis"),
                    "thresholds.maximumProxyOverheadP99Millis")
    positive_number(thresholds.get("maximumHealthCheckRequestsPerSecond"),
                    "thresholds.maximumHealthCheckRequestsPerSecond")
    positive_int(thresholds.get("recoveryWindowSeconds"), "thresholds.recoveryWindowSeconds")
    return host, port, networks


def resolve(host: str, port: int, networks: list[Network]) -> list[str]:
    addresses: set[Address] = set()
    try:
        records = socket.getaddrinfo(host, port, type=socket.SOCK_STREAM)
    except socket.gaierror as exc:
        raise ContractError(f"target DNS resolution failed for {host}: {exc}") from exc
    for record in records:
        address = ipaddress.ip_address(record[4][0].split("%")[0])
        require(not (address.is_loopback or address.is_link_local or address.is_multicast or address.is_unspecified),
                f"resolved address {address} is not an eligible staging address")
        require(any(address.version == network.version and address in network for network in networks),
                f"resolved address {address} is outside target.allowedCidrs")
        addresses.add(address)
    require(bool(addresses), "target DNS resolution returned no addresses")
    return [str(address) for address in sorted(addresses, key=lambda item: (item.version, int(item)))]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile", required=True, type=Path)
    parser.add_argument("--resolve", action="store_true")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    try:
        profile = json.loads(args.profile.read_text(encoding="utf-8"))
        require(isinstance(profile, dict), "profile root must be an object")
        host, port, networks = validate(profile)
        result: dict[str, Any] = {
            "profileId": profile["profileId"],
            "structurallyValid": True,
            "networkResolutionPerformed": args.resolve,
        }
        if args.resolve:
            addresses = resolve(host, port, networks)
            result.update({"host": host, "port": port, "resolvedAddresses": addresses,
                           "pinnedAddress": addresses[0]})
        encoded = json.dumps(result, sort_keys=True, indent=2) + "\n"
        if args.output:
            args.output.write_text(encoded, encoding="utf-8")
        else:
            sys.stdout.write(encoded)
        return 0
    except (OSError, json.JSONDecodeError, ContractError) as exc:
        print(f"staging profile rejected: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
