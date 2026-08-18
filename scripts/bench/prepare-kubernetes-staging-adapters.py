#!/usr/bin/env python3
"""Validate and deterministically compile reviewed Kubernetes staging adapters."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import sys
from typing import Any
from urllib.parse import urlparse


SHA256 = re.compile(r"^[0-9a-f]{64}$")
SAFE_ID = re.compile(r"^[a-z0-9](?:[a-z0-9._-]{0,61}[a-z0-9])?$")
DNS_LABEL = re.compile(r"^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$")
KUBE_KEY = re.compile(
    r"^(?:[a-z0-9](?:[a-z0-9.-]{0,251}[a-z0-9])?/)?"
    r"[A-Za-z0-9](?:[A-Za-z0-9._-]{0,61}[A-Za-z0-9])?$"
)
SELECTOR = re.compile(r"^[A-Za-z0-9._/-]+=[A-Za-z0-9._-]+(?:,[A-Za-z0-9._/-]+=[A-Za-z0-9._-]+)*$")
METRIC = re.compile(r"^[a-z][a-z0-9_:]{2,127}$")
ENVIRONMENT_VARIABLE = re.compile(r"^[A-Z][A-Z0-9_]{1,126}$")
UUID = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
PLACEHOLDER = re.compile(r"replace|example|invalid|todo", re.IGNORECASE)
PRODUCTION = re.compile(r"(^|[._/-])(prod|production|live)([._/-]|$)", re.IGNORECASE)
REGISTRY_REPOSITORY = re.compile(
    r"^(?=.{3,255}$)[a-z0-9]+(?:[._-][a-z0-9]+)*(?::[0-9]{1,5})?"
    r"(?:/[a-z0-9]+(?:[._-][a-z0-9]+)*)+$"
)
REVISION = re.compile(r"^[0-9a-f]{40}$")
ACTION_NAMES = (
    "verify-deployment", "rollout-candidate", "rollback-prior", "slow", "failure",
    "reload", "drain", "restart", "certificate-rotation", "reset",
)
ROOT_FIELDS = {
    "schemaVersion", "adapterId", "platform", "review", "kubectl", "cluster",
    "stagingBinding", "proxy", "faults", "capacity",
}


class ContractError(ValueError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ContractError(message)


def exact_object(value: Any, name: str, fields: set[str]) -> dict[str, Any]:
    require(isinstance(value, dict), f"{name} must be an object")
    require(set(value) == fields, f"{name} contains unexpected or missing fields")
    return value


def safe_string(value: Any, name: str, pattern: re.Pattern[str] = SAFE_ID) -> str:
    require(isinstance(value, str) and bool(pattern.fullmatch(value)), f"{name} is invalid")
    return value


def non_placeholder(value: Any, name: str) -> str:
    require(isinstance(value, str) and bool(value.strip()), f"{name} must be a non-empty string")
    normalized = value.strip()
    require(not PLACEHOLDER.search(normalized), f"{name} contains a placeholder")
    return normalized


def positive_integer(value: Any, name: str, minimum: int = 1, maximum: int = 2_147_483_647) -> int:
    require(isinstance(value, int) and not isinstance(value, bool) and minimum <= value <= maximum,
            f"{name} must be an integer between {minimum} and {maximum}")
    return value


def reviewed_timestamp(value: Any, execution: bool) -> str:
    require(isinstance(value, str), "review.approvedAt must be a string")
    if not execution and not value:
        return value
    require(value.endswith("Z"), "review.approvedAt must be an RFC3339 UTC timestamp")
    try:
        parsed = datetime.fromisoformat(value.removesuffix("Z") + "+00:00")
    except ValueError as exc:
        raise ContractError("review.approvedAt must be an RFC3339 UTC timestamp") from exc
    require(parsed <= datetime.now(timezone.utc), "review.approvedAt cannot be in the future")
    return value


def validate_object_reference(value: Any, name: str, allowed_kinds: set[str]) -> dict[str, str]:
    reference = exact_object(value, name, {"apiVersion", "kind", "name"})
    api_version = non_placeholder(reference.get("apiVersion"), f"{name}.apiVersion")
    require(bool(re.fullmatch(r"[a-z0-9.]+(?:/[a-z0-9]+)?", api_version)),
            f"{name}.apiVersion is invalid")
    kind = reference.get("kind")
    require(kind in allowed_kinds, f"{name}.kind is not allowed")
    object_name = safe_string(reference.get("name"), f"{name}.name", DNS_LABEL)
    return {"apiVersion": api_version, "kind": kind, "name": object_name}


def validate(profile: dict[str, Any], execution: bool) -> dict[str, Any]:
    require(set(profile) == ROOT_FIELDS, "adapter profile contains unexpected or missing root fields")
    require(profile.get("schemaVersion") == 1, "schemaVersion must be 1")
    adapter_id = safe_string(profile.get("adapterId"), "adapterId")
    require(profile.get("platform") == "kubernetes", "platform must be kubernetes")

    review = exact_object(profile.get("review"), "review", {"status", "approvedBy", "approvedAt"})
    require(isinstance(review.get("status"), str), "review.status must be a string")
    require(isinstance(review.get("approvedBy"), str), "review.approvedBy must be a string")
    require(isinstance(review.get("approvedAt"), str), "review.approvedAt must be a string")
    if execution:
        require(review.get("status") == "reviewed", "build mode requires review.status=reviewed")
        non_placeholder(review.get("approvedBy"), "review.approvedBy")
    reviewed_timestamp(review.get("approvedAt"), execution)

    kubectl = exact_object(profile.get("kubectl"), "kubectl",
                           {"executableSha256", "maximumCommandSeconds"})
    kubectl_sha = kubectl.get("executableSha256")
    require(isinstance(kubectl_sha, str) and bool(SHA256.fullmatch(kubectl_sha)),
            "kubectl.executableSha256 must be a lowercase SHA-256")
    if execution:
        require(kubectl_sha != "0" * 64, "build mode requires a non-placeholder kubectl hash")
    positive_integer(kubectl.get("maximumCommandSeconds"), "kubectl.maximumCommandSeconds", 5, 60)

    cluster = exact_object(profile.get("cluster"), "cluster",
                           {"context", "apiServer", "namespace", "namespaceUid", "environmentLabel"})
    context = cluster.get("context")
    namespace = cluster.get("namespace")
    require(isinstance(context, str) and 1 <= len(context) <= 253 and "\n" not in context,
            "cluster.context is invalid")
    require(isinstance(namespace, str) and bool(DNS_LABEL.fullmatch(namespace)),
            "cluster.namespace is invalid")
    require(not PRODUCTION.search(context) and not PRODUCTION.search(namespace),
            "cluster context and namespace must not look production-like")
    require(namespace not in {"default", "kube-system", "kube-public", "kube-node-lease"},
            "cluster.namespace must be a dedicated staging namespace")
    if execution:
        non_placeholder(context, "cluster.context")
        non_placeholder(namespace, "cluster.namespace")
        require("stag" in context.lower() and "stag" in namespace.lower(),
                "reviewed context and namespace must explicitly identify staging")
    server = cluster.get("apiServer")
    require(isinstance(server, str), "cluster.apiServer must be a string")
    parsed_server = urlparse(server)
    require(parsed_server.scheme == "https" and bool(parsed_server.hostname)
            and parsed_server.username is None and parsed_server.password is None
            and parsed_server.query == "" and parsed_server.fragment == ""
            and parsed_server.path in {"", "/"},
            "cluster.apiServer must be an HTTPS origin without credentials or path")
    host = (parsed_server.hostname or "").lower()
    require(host not in {"localhost", "127.0.0.1", "::1"} and not PRODUCTION.search(host),
            "cluster.apiServer must identify a non-production non-loopback server")
    if execution:
        require(not PLACEHOLDER.search(host), "cluster.apiServer contains a placeholder")
    namespace_uid = cluster.get("namespaceUid")
    require(isinstance(namespace_uid, str)
            and (bool(UUID.fullmatch(namespace_uid))
                 or namespace_uid == "00000000-0000-0000-0000-000000000000"),
            "cluster.namespaceUid must be a lowercase Kubernetes UID")
    if execution:
        require(namespace_uid != "00000000-0000-0000-0000-000000000000",
                "build mode requires the observed namespace UID")
    environment_label = exact_object(cluster.get("environmentLabel"), "cluster.environmentLabel", {"key", "value"})
    safe_string(environment_label.get("key"), "cluster.environmentLabel.key", KUBE_KEY)
    require(environment_label.get("value") == "staging",
            "cluster.environmentLabel.value must be staging")

    staging_binding = exact_object(
        profile.get("stagingBinding"), "stagingBinding", {"changeTicket", "artifact"}
    )
    change_ticket = staging_binding.get("changeTicket")
    require(isinstance(change_ticket, str) and bool(re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._/-]{2,127}", change_ticket)),
            "stagingBinding.changeTicket is invalid")
    if execution:
        non_placeholder(change_ticket, "stagingBinding.changeTicket")
    artifact = exact_object(
        staging_binding.get("artifact"), "stagingBinding.artifact", {"registryRepository", "prior", "candidate"}
    )
    repository = artifact.get("registryRepository")
    require(isinstance(repository, str) and bool(REGISTRY_REPOSITORY.fullmatch(repository))
            and "@" not in repository, "stagingBinding.artifact.registryRepository is invalid")
    if execution:
        non_placeholder(repository, "stagingBinding.artifact.registryRepository")
    artifact_digests: list[str] = []
    for phase in ("prior", "candidate"):
        identity = exact_object(
            artifact.get(phase), f"stagingBinding.artifact.{phase}", {"imageDigest", "sourceRevision"}
        )
        digest = identity.get("imageDigest")
        revision = identity.get("sourceRevision")
        require(isinstance(digest, str) and digest.startswith("sha256:")
                and bool(SHA256.fullmatch(digest.removeprefix("sha256:"))),
                f"stagingBinding.artifact.{phase}.imageDigest is invalid")
        require(isinstance(revision, str) and bool(REVISION.fullmatch(revision)),
                f"stagingBinding.artifact.{phase}.sourceRevision is invalid")
        if execution:
            require(digest != "sha256:" + "0" * 64 and revision != "0" * 40,
                    f"build mode requires a reviewed stagingBinding.artifact.{phase} identity")
        artifact_digests.append(digest)
    require(len(set(artifact_digests)) == 2,
            "stagingBinding prior and candidate image digests must differ")

    proxy = exact_object(profile.get("proxy"), "proxy", {
        "deployment", "container", "podSelector", "zoneLabel", "sourceRevisionAnnotation",
        "changeTicketAnnotation", "localApi", "configurationObjects", "ingressObjects",
    })
    proxy_deployment = safe_string(proxy.get("deployment"), "proxy.deployment", DNS_LABEL)
    safe_string(proxy.get("container"), "proxy.container", DNS_LABEL)
    safe_string(proxy.get("podSelector"), "proxy.podSelector", SELECTOR)
    safe_string(proxy.get("zoneLabel"), "proxy.zoneLabel", KUBE_KEY)
    safe_string(proxy.get("sourceRevisionAnnotation"), "proxy.sourceRevisionAnnotation", KUBE_KEY)
    safe_string(proxy.get("changeTicketAnnotation"), "proxy.changeTicketAnnotation", KUBE_KEY)
    local_api = exact_object(proxy.get("localApi"), "proxy.localApi", {
        "tlsHost", "port", "apiKeyPath", "caPath", "healthPath", "metricsPath", "statusPath", "reloadPath",
    })
    tls_host = local_api.get("tlsHost")
    require(isinstance(tls_host, str) and 3 <= len(tls_host) <= 253 and "." in tls_host
            and not PRODUCTION.search(tls_host), "proxy.localApi.tlsHost is invalid")
    if execution:
        non_placeholder(tls_host, "proxy.localApi.tlsHost")
    positive_integer(local_api.get("port"), "proxy.localApi.port", 1, 65535)
    for field in ("apiKeyPath", "caPath"):
        path = local_api.get(field)
        require(isinstance(path, str) and path.startswith("/run/") and ".." not in path
                and "\n" not in path, f"proxy.localApi.{field} must be a fixed /run path")
    for field in ("healthPath", "metricsPath", "statusPath", "reloadPath"):
        path = local_api.get(field)
        require(isinstance(path, str) and path.startswith("/") and ".." not in path
                and "?" not in path and "#" not in path, f"proxy.localApi.{field} is invalid")

    configuration_values = proxy.get("configurationObjects")
    require(isinstance(configuration_values, list) and bool(configuration_values),
            "proxy.configurationObjects must be a non-empty array")
    configuration_objects = [
        validate_object_reference(value, f"proxy.configurationObjects[{index}]", {"ConfigMap", "Secret"})
        for index, value in enumerate(configuration_values)
    ]
    ingress_values = proxy.get("ingressObjects")
    require(isinstance(ingress_values, list) and bool(ingress_values),
            "proxy.ingressObjects must be a non-empty array")
    ingress_objects = [
        validate_object_reference(value, f"proxy.ingressObjects[{index}]",
                                  {"Service", "Ingress", "Gateway", "HTTPRoute"})
        for index, value in enumerate(ingress_values)
    ]
    references = [(item["apiVersion"], item["kind"], item["name"])
                  for item in configuration_objects + ingress_objects]
    require(len(references) == len(set(references)), "Kubernetes object references must be unique")

    faults = exact_object(profile.get("faults"), "faults",
                          {"slow", "failure", "reload", "drain", "reset", "certificateRotation"})
    slow = exact_object(faults.get("slow"), "faults.slow", {
        "deployment", "container", "environmentVariable", "baselineValue", "activeValue",
    })
    slow_deployment = safe_string(slow.get("deployment"), "faults.slow.deployment", DNS_LABEL)
    safe_string(slow.get("container"), "faults.slow.container", DNS_LABEL)
    safe_string(slow.get("environmentVariable"), "faults.slow.environmentVariable", ENVIRONMENT_VARIABLE)
    for field in ("baselineValue", "activeValue"):
        value = slow.get(field)
        require(isinstance(value, str) and 1 <= len(value) <= 64 and "\n" not in value,
                f"faults.slow.{field} is invalid")
    require(slow.get("baselineValue") != slow.get("activeValue"),
            "slow fault baseline and active values must differ")
    failure = exact_object(faults.get("failure"), "faults.failure", {"deployment", "baselineReplicas"})
    failure_deployment = safe_string(failure.get("deployment"), "faults.failure.deployment", DNS_LABEL)
    positive_integer(failure.get("baselineReplicas"), "faults.failure.baselineReplicas", 1, 100)
    require(len({proxy_deployment, slow_deployment, failure_deployment}) == 3,
            "proxy, slow-fault, and failure-fault deployments must be distinct")
    payload_references: list[tuple[str, str]] = []
    for action in ("reload", "drain", "reset"):
        payload = exact_object(
            faults.get(action), f"faults.{action}", {"configMap", "key", "payloadSha256"}
        )
        config_map = safe_string(payload.get("configMap"), f"faults.{action}.configMap", DNS_LABEL)
        key = payload.get("key")
        require(isinstance(key, str) and bool(re.fullmatch(r"[A-Za-z0-9._-]{1,128}", key))
                and key.endswith(".json"), f"faults.{action}.key must be a JSON ConfigMap key")
        payload_sha = payload.get("payloadSha256")
        require(isinstance(payload_sha, str) and bool(SHA256.fullmatch(payload_sha)),
                f"faults.{action}.payloadSha256 must be a lowercase SHA-256")
        if execution:
            require(payload_sha != "0" * 64,
                    f"build mode requires a reviewed faults.{action}.payloadSha256")
        payload_references.append((config_map, key))
    require(len(payload_references) == len(set(payload_references)),
            "reload, drain, and reset must use distinct reviewed payload keys")
    rotation = exact_object(faults.get("certificateRotation"), "faults.certificateRotation",
                            {"volumeName", "baselineSecret", "candidateSecret"})
    safe_string(rotation.get("volumeName"), "faults.certificateRotation.volumeName", DNS_LABEL)
    baseline_secret = safe_string(rotation.get("baselineSecret"),
                                  "faults.certificateRotation.baselineSecret", DNS_LABEL)
    candidate_secret = safe_string(rotation.get("candidateSecret"),
                                   "faults.certificateRotation.candidateSecret", DNS_LABEL)
    require(baseline_secret != candidate_secret, "certificate rotation secrets must differ")
    if execution:
        for name, value in (("baselineSecret", baseline_secret), ("candidateSecret", candidate_secret)):
            non_placeholder(value, f"faults.certificateRotation.{name}")

    capacity = exact_object(profile.get("capacity"), "capacity",
                            {"upstreamIds", "openConnectionsMetric", "jvmThreadsMetric"})
    upstream_values = capacity.get("upstreamIds")
    require(isinstance(upstream_values, list) and len(upstream_values) >= 2,
            "capacity.upstreamIds must contain at least two upstream IDs")
    upstream_ids = [safe_string(value, "capacity.upstreamIds[]") for value in upstream_values]
    require(len(upstream_ids) == len(set(upstream_ids)), "capacity.upstreamIds must be unique")
    for field in ("openConnectionsMetric", "jvmThreadsMetric"):
        safe_string(capacity.get(field), f"capacity.{field}", METRIC)

    return {
        "adapterId": adapter_id,
        "configurationObjects": len(configuration_objects),
        "ingressObjects": len(ingress_objects),
        "upstreamIds": upstream_ids,
        "executionAuthorized": execution,
    }


def load_profile(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    require(isinstance(value, dict), "adapter profile root must be an object")
    return value


def compile_runtime(runtime_template: Path, profile: dict[str, Any]) -> bytes:
    source = runtime_template.read_text(encoding="utf-8")
    marker = "ADAPTER_CONFIG_JSON = None  # @generated-config@"
    require(source.count(marker) == 1, "runtime template does not contain exactly one generation marker")
    normalized = json.dumps(profile, sort_keys=True, separators=(",", ":"), ensure_ascii=True)
    generated = source.replace(marker, f"ADAPTER_CONFIG_JSON = {normalized!r}")
    return generated.encode("utf-8")


def build(output: Path, runtime: bytes, profile_path: Path, summary: dict[str, Any]) -> dict[str, Any]:
    repo_root = Path(__file__).resolve().parents[2]
    requested_parent = output.parent
    require(bool(re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}", output.name))
            and output.name not in {".", ".."}, "output directory name is invalid")
    require(not requested_parent.is_symlink(), "output parent must not be a symbolic link")
    parent = requested_parent.resolve(strict=True)
    resolved_output = parent / output.name
    require(not resolved_output.exists(), "output path must not already exist")
    require(not resolved_output.is_relative_to(repo_root), "generated adapters must remain outside the repository")
    executable_sha = hashlib.sha256(runtime).hexdigest()
    profile_sha = hashlib.sha256(profile_path.read_bytes()).hexdigest()
    created = False
    try:
        resolved_output.mkdir(mode=0o700)
        created = True
        action_dir = resolved_output / "actions"
        action_dir.mkdir(mode=0o700)
        for name in ACTION_NAMES:
            path = action_dir / f"{name}.sh"
            path.write_bytes(runtime)
            path.chmod(0o500)
        sampler = resolved_output / "capacity-sampler.sh"
        sampler.write_bytes(runtime)
        sampler.chmod(0o500)
        inspector = resolved_output / "inspect.sh"
        inspector.write_bytes(runtime)
        inspector.chmod(0o500)
        bindings = {
            "schemaVersion": 1,
            "adapterId": summary["adapterId"],
            "adapterProfileSha256": profile_sha,
            "actions": {name: executable_sha for name in ACTION_NAMES},
            "capacitySamplerSha256": executable_sha,
            "inspectorSha256": executable_sha,
        }
        bindings_path = resolved_output / "bindings.json"
        bindings_path.write_text(json.dumps(bindings, sort_keys=True, indent=2) + "\n", encoding="utf-8")
        bindings_path.chmod(0o400)
        action_dir.chmod(0o500)
        return {
            **bindings,
            "actionDirectory": str(action_dir),
            "capacitySampler": str(sampler),
            "inspector": str(inspector),
        }
    except Exception:
        if created and resolved_output.parent == parent:
            shutil.rmtree(resolved_output, ignore_errors=True)
        raise


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=("validate", "build"), default="validate")
    parser.add_argument("--profile", required=True, type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    try:
        profile = load_profile(args.profile)
        execution = args.mode == "build"
        summary = validate(profile, execution)
        result: dict[str, Any] = {
            **summary,
            "profileSha256": hashlib.sha256(args.profile.read_bytes()).hexdigest(),
            "networkAccessPerformed": False,
            "clusterMutationPerformed": False,
        }
        if execution:
            require(args.output is not None, "build mode requires --output")
            runtime_template = Path(__file__).with_name("kubernetes-staging-adapter-runtime.py")
            result.update(build(args.output, compile_runtime(runtime_template, profile), args.profile, summary))
        elif args.output is not None:
            raise ContractError("validate mode does not accept --output")
        sys.stdout.write(json.dumps(result, sort_keys=True, indent=2) + "\n")
        return 0
    except (OSError, json.JSONDecodeError, ContractError) as exc:
        print(f"Kubernetes staging adapter profile rejected: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
