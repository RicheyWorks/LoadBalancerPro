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
)
HOOKS = ("verify-artifact", "slow", "failure", "reload", "drain", "restart",
         "certificate-rotation", "reset")
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
PRODUCTION_NAME = re.compile(r"(^|[.-])(prod|production|live)([.-]|$)")

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
    digest = nonempty(artifact.get("imageDigest"), "artifact.imageDigest")
    require(digest.startswith("sha256:") and bool(SHA256.fullmatch(digest.removeprefix("sha256:"))),
            "artifact.imageDigest must be a sha256 digest")
    require(bool(REVISION.fullmatch(nonempty(artifact.get("sourceRevision"), "artifact.sourceRevision"))),
            "artifact.sourceRevision must be a 40-character lowercase Git revision")

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
