#!/usr/bin/env python3
"""Generated Kubernetes staging action/sampler runtime. Compile it; do not invoke the template."""

from __future__ import annotations

from copy import deepcopy
from datetime import datetime, timezone
import hashlib
import json
import math
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
from typing import Any


ADAPTER_CONFIG_JSON = None  # @generated-config@
MAX_JSON_BYTES = 4 * 1024 * 1024
MAX_METRICS_BYTES = 4 * 1024 * 1024
METRIC_LINE = re.compile(
    r"^([A-Za-z_:][A-Za-z0-9_:]*)(?:\{(.*)\})?\s+"
    r"([-+]?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)(?:[eE][-+]?[0-9]+)?|NaN|[+-]Inf)$"
)
LABEL = re.compile(r'(\w+)="((?:\\.|[^"\\])*)"(?:,|$)')
REVISION = re.compile(r"^[0-9a-f]{40}$")
DIGEST = re.compile(r"sha256:[0-9a-f]{64}")
ROLE_NAMES = {
    "verify-deployment", "rollout-candidate", "rollback-prior", "slow", "failure",
    "reload", "drain", "restart", "certificate-rotation", "reset",
    "capacity-sampler", "inspect",
}


class AdapterError(RuntimeError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AdapterError(message)


def canonical_bytes(value: Any) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=True).encode("utf-8")


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def now_utc() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")


def load_config() -> dict[str, Any]:
    require(isinstance(ADAPTER_CONFIG_JSON, str),
            "this is an uncompiled adapter template; use prepare-kubernetes-staging-adapters.py")
    value = json.loads(ADAPTER_CONFIG_JSON)
    require(isinstance(value, dict) and value.get("platform") == "kubernetes",
            "embedded adapter configuration is invalid")
    return value


CONFIG = load_config()
KUBECTL: str = ""


def locate_kubectl() -> str:
    executable = shutil.which("kubectl")
    require(executable is not None, "kubectl is required")
    path = Path(executable)
    require(path.is_file(), "kubectl must resolve to a regular file")
    actual = sha256_bytes(path.read_bytes())
    require(actual == CONFIG["kubectl"]["executableSha256"],
            "kubectl executable hash differs from the reviewed adapter profile")
    return str(path.resolve())


def command_prefix(namespaced: bool = True) -> list[str]:
    prefix = [KUBECTL, "--context", CONFIG["cluster"]["context"]]
    if namespaced:
        prefix.extend(["--namespace", CONFIG["cluster"]["namespace"]])
    prefix.append(f"--request-timeout={CONFIG['kubectl']['maximumCommandSeconds']}s")
    return prefix


def run_command(
        arguments: list[str], *, namespaced: bool = True, stdin: bytes | None = None,
        maximum_bytes: int = MAX_JSON_BYTES) -> bytes:
    require(all(isinstance(argument, str) and "\x00" not in argument for argument in arguments),
            "invalid kubectl argument")
    try:
        result = subprocess.run(
            command_prefix(namespaced) + arguments,
            input=stdin,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=CONFIG["kubectl"]["maximumCommandSeconds"],
            check=False,
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        raise AdapterError(f"kubectl operation did not complete: {arguments[0]}") from exc
    require(result.returncode == 0,
            f"kubectl operation failed safely: {arguments[0]} (exit {result.returncode})")
    require(len(result.stdout) <= maximum_bytes, "kubectl response exceeded its bounded size")
    return result.stdout


def raw_current_context() -> str:
    try:
        result = subprocess.run(
            [KUBECTL, "config", "current-context"], stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            timeout=CONFIG["kubectl"]["maximumCommandSeconds"], check=False,
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        raise AdapterError("kubectl current-context did not complete") from exc
    require(result.returncode == 0 and len(result.stdout) <= 4096,
            "kubectl current-context failed safely")
    return result.stdout.decode("utf-8", errors="strict").strip()


def json_command(arguments: list[str], *, namespaced: bool = True) -> dict[str, Any]:
    raw = run_command(arguments, namespaced=namespaced)
    try:
        value = json.loads(raw)
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise AdapterError(f"kubectl returned invalid JSON for {arguments[0]}") from exc
    require(isinstance(value, dict), f"kubectl returned a non-object for {arguments[0]}")
    return value


def get_object(kind: str, name: str, *, namespaced: bool = True) -> dict[str, Any]:
    return json_command(["get", kind.lower(), name, "-o", "json"], namespaced=namespaced)


def preflight() -> None:
    require(raw_current_context() == CONFIG["cluster"]["context"],
            "current kubectl context is not the reviewed staging context")
    view = json_command(["config", "view", "--minify", "-o", "json"], namespaced=False)
    clusters = view.get("clusters")
    require(isinstance(clusters, list) and len(clusters) == 1, "kubectl context must resolve one cluster")
    observed_server = clusters[0].get("cluster", {}).get("server")
    require(observed_server == CONFIG["cluster"]["apiServer"],
            "kubectl context API server differs from the reviewed staging server")
    namespace = get_object("namespace", CONFIG["cluster"]["namespace"], namespaced=False)
    metadata = namespace.get("metadata", {})
    require(metadata.get("uid") == CONFIG["cluster"]["namespaceUid"],
            "staging namespace UID differs from the reviewed namespace")
    environment_label = CONFIG["cluster"]["environmentLabel"]
    require(metadata.get("labels", {}).get(environment_label["key"]) == environment_label["value"],
            "staging namespace environment label is absent or changed")


def staging_profile() -> dict[str, Any]:
    path_value = os.environ.get("LBP_STAGING_REVIEWED_PROFILE", "")
    require(bool(path_value), "LBP_STAGING_REVIEWED_PROFILE is required")
    path = Path(path_value)
    require(path.is_file() and not path.is_symlink(), "reviewed staging profile must be a regular non-symlink file")
    require(path.stat().st_size <= 256 * 1024, "reviewed staging profile exceeds its bounded size")
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise AdapterError("reviewed staging profile could not be read") from exc
    require(isinstance(value, dict), "reviewed staging profile root must be an object")
    require(value.get("review", {}).get("status") == "reviewed",
            "adapter requires a reviewed staging profile")
    require(value.get("environment", {}).get("productionTrafficAuthorized") is False,
            "adapter refuses a profile authorizing production traffic")
    binding = CONFIG["stagingBinding"]
    require(value.get("environment", {}).get("changeTicket") == binding["changeTicket"],
            "staging change ticket differs from the compiled adapter binding")
    require(value.get("environment", {}).get("changeTicket") == os.environ.get("LBP_STAGING_CHANGE_TICKET"),
            "staging change ticket environment differs from the reviewed profile")
    require(value.get("artifact") == binding["artifact"],
            "staging artifact identity differs from the compiled adapter binding")
    return value


def expected_identity(profile: dict[str, Any], phase: str) -> tuple[str, str, str]:
    artifact_name = "candidate" if phase == "candidate" else "prior"
    artifact = profile["artifact"][artifact_name]
    reference = f"{profile['artifact']['registryRepository']}@{artifact['imageDigest']}"
    environment_reference = os.environ.get(
        "LBP_STAGING_CANDIDATE_IMAGE_REFERENCE" if artifact_name == "candidate"
        else "LBP_STAGING_PRIOR_IMAGE_REFERENCE", "")
    environment_revision = os.environ.get(
        "LBP_STAGING_CANDIDATE_SOURCE_REVISION" if artifact_name == "candidate"
        else "LBP_STAGING_PRIOR_SOURCE_REVISION", "")
    require(environment_reference == reference and environment_revision == artifact["sourceRevision"],
            "runner artifact environment differs from the reviewed staging profile")
    return reference, artifact["imageDigest"], artifact["sourceRevision"]


def sanitize_template(deployment: dict[str, Any]) -> dict[str, Any]:
    template = deepcopy(deployment.get("spec", {}).get("template", {}))
    metadata = template.setdefault("metadata", {})
    annotations = metadata.setdefault("annotations", {})
    for key in (
            CONFIG["proxy"]["sourceRevisionAnnotation"],
            CONFIG["proxy"]["changeTicketAnnotation"],
            "kubectl.kubernetes.io/restartedAt"):
        annotations.pop(key, None)
    for container in template.get("spec", {}).get("containers", []):
        if container.get("name") == CONFIG["proxy"]["container"]:
            container["image"] = "<reviewed-phase-image>"
    return template


def sanitized_configuration_object(reference: dict[str, str]) -> dict[str, Any]:
    value = get_object(reference["kind"], reference["name"])
    metadata = value.get("metadata", {})
    require(value.get("apiVersion") == reference["apiVersion"] and value.get("kind") == reference["kind"],
            f"configuration object identity changed: {reference['name']}")
    require(metadata.get("name") == reference["name"]
            and metadata.get("namespace") == CONFIG["cluster"]["namespace"]
            and isinstance(metadata.get("uid"), str),
            f"configuration object metadata changed: {reference['name']}")
    require(value.get("immutable") is True,
            f"configuration object is not immutable: {reference['name']}")
    base: dict[str, Any] = {
        "apiVersion": value["apiVersion"], "kind": value["kind"],
        "metadata": {"name": metadata["name"], "namespace": metadata["namespace"], "uid": metadata["uid"]},
        "immutable": True,
    }
    if reference["kind"] == "Secret":
        secret_material = {"type": value.get("type", ""), "data": value.get("data", {})}
        require(isinstance(secret_material["data"], dict), "secret data must be an object")
        base["secretDataSha256"] = sha256_bytes(canonical_bytes(secret_material))
    else:
        base["data"] = value.get("data", {})
        base["binaryData"] = value.get("binaryData", {})
    return base


def sanitized_ingress_object(reference: dict[str, str]) -> dict[str, Any]:
    value = get_object(reference["kind"], reference["name"])
    metadata = value.get("metadata", {})
    require(value.get("apiVersion") == reference["apiVersion"] and value.get("kind") == reference["kind"],
            f"ingress object identity changed: {reference['name']}")
    require(metadata.get("name") == reference["name"]
            and metadata.get("namespace") == CONFIG["cluster"]["namespace"]
            and isinstance(metadata.get("uid"), str),
            f"ingress object metadata changed: {reference['name']}")
    return {
        "apiVersion": value["apiVersion"], "kind": value["kind"],
        "metadata": {"name": metadata["name"], "namespace": metadata["namespace"], "uid": metadata["uid"]},
        "spec": value.get("spec", {}),
    }


def live_fingerprints(deployment: dict[str, Any] | None = None) -> tuple[str, str]:
    current = deployment or get_object("deployment", CONFIG["proxy"]["deployment"])
    configuration = {
        "deploymentTemplate": sanitize_template(current),
        "objects": [sanitized_configuration_object(reference)
                    for reference in CONFIG["proxy"]["configurationObjects"]],
    }
    ingress = {
        "objects": [sanitized_ingress_object(reference)
                    for reference in CONFIG["proxy"]["ingressObjects"]],
    }
    return sha256_bytes(canonical_bytes(configuration)), sha256_bytes(canonical_bytes(ingress))


def parse_cpu_millis(value: str) -> float:
    require(isinstance(value, str) and bool(value), "Kubernetes CPU quantity is invalid")
    suffixes = (("n", 1e-6), ("u", 1e-3), ("m", 1.0))
    for suffix, multiplier in suffixes:
        if value.endswith(suffix):
            number = float(value[:-1]) * multiplier
            require(math.isfinite(number) and number >= 0, "Kubernetes CPU quantity is invalid")
            return number
    number = float(value) * 1000.0
    require(math.isfinite(number) and number >= 0, "Kubernetes CPU quantity is invalid")
    return number


def parse_bytes(value: str) -> float:
    require(isinstance(value, str) and bool(value), "Kubernetes memory quantity is invalid")
    binary = {"Ki": 1024.0, "Mi": 1024.0 ** 2, "Gi": 1024.0 ** 3, "Ti": 1024.0 ** 4}
    decimal = {"k": 1000.0, "M": 1000.0 ** 2, "G": 1000.0 ** 3, "T": 1000.0 ** 4}
    for suffix, multiplier in {**binary, **decimal}.items():
        if value.endswith(suffix):
            number = float(value[:-len(suffix)]) * multiplier
            require(math.isfinite(number) and number >= 0, "Kubernetes memory quantity is invalid")
            return number
    number = float(value)
    require(math.isfinite(number) and number >= 0, "Kubernetes memory quantity is invalid")
    return number


def ready_condition(pod: dict[str, Any]) -> bool:
    if pod.get("metadata", {}).get("deletionTimestamp") is not None:
        return False
    return any(condition.get("type") == "Ready" and condition.get("status") == "True"
               for condition in pod.get("status", {}).get("conditions", []))


def proxy_container(value: dict[str, Any], path: tuple[str, ...]) -> dict[str, Any]:
    current: Any = value
    for field in path:
        current = current.get(field, {}) if isinstance(current, dict) else {}
    require(isinstance(current, list), "deployment container list is absent")
    matches = [container for container in current if container.get("name") == CONFIG["proxy"]["container"]]
    require(len(matches) == 1, "deployment must contain exactly one reviewed proxy container")
    return matches[0]


def pod_image_digest(pod: dict[str, Any]) -> str:
    statuses = pod.get("status", {}).get("containerStatuses", [])
    matches = [status for status in statuses if status.get("name") == CONFIG["proxy"]["container"]]
    require(len(matches) == 1, "ready pod is missing the reviewed container status")
    image_id = matches[0].get("imageID", "")
    digests = DIGEST.findall(image_id)
    require(len(digests) == 1, "ready pod imageID is not an immutable SHA-256 digest")
    return digests[0]


def list_proxy_pods() -> list[dict[str, Any]]:
    value = json_command(["get", "pods", "-l", CONFIG["proxy"]["podSelector"], "-o", "json"])
    items = value.get("items")
    require(isinstance(items, list), "pod listing is invalid")
    return [pod for pod in items if ready_condition(pod)]


def node_zone(node_name: str) -> str:
    node = get_object("node", node_name, namespaced=False)
    zone = node.get("metadata", {}).get("labels", {}).get(CONFIG["proxy"]["zoneLabel"])
    require(isinstance(zone, str) and bool(zone), f"node {node_name} lacks the reviewed zone label")
    return zone


def pod_http(pod_name: str, path: str, method: str = "GET", payload: bytes | None = None) -> bytes:
    api = CONFIG["proxy"]["localApi"]
    url = f"https://{api['tlsHost']}:{api['port']}{path}"
    shell = (
        'key="$(cat "$1")"; exec curl --fail --silent --show-error '
        '--cacert "$2" --resolve "$3:$4:127.0.0.1" --header "X-API-Key: $key" '
    )
    arguments = ["exec", "-i", pod_name, "-c", CONFIG["proxy"]["container"], "--", "sh", "-ceu"]
    if method == "POST":
        shell += '--request POST --header "Content-Type: application/json" --data-binary @- "$5"'
    else:
        shell += '"$5"'
    arguments.extend([shell, "adapter", api["apiKeyPath"], api["caPath"], api["tlsHost"],
                      str(api["port"]), url])
    return run_command(arguments, stdin=payload, maximum_bytes=MAX_METRICS_BYTES)


def parse_labels(raw: str) -> dict[str, str]:
    if not raw:
        return {}
    labels: dict[str, str] = {}
    position = 0
    while position < len(raw):
        match = LABEL.match(raw, position)
        require(match is not None, "Prometheus metric contains invalid labels")
        labels[match.group(1)] = bytes(match.group(2), "utf-8").decode("unicode_escape")
        position = match.end()
    return labels


def parse_metrics(raw: bytes) -> list[tuple[str, dict[str, str], float]]:
    require(len(raw) <= MAX_METRICS_BYTES, "metrics scrape exceeded its bounded size")
    try:
        text = raw.decode("utf-8", errors="strict")
    except UnicodeDecodeError as exc:
        raise AdapterError("metrics scrape is not UTF-8") from exc
    samples: list[tuple[str, dict[str, str], float]] = []
    for line in text.splitlines():
        if not line or line.startswith("#"):
            continue
        match = METRIC_LINE.fullmatch(line.strip())
        require(match is not None, "metrics scrape contains an invalid sample")
        value = float(match.group(3))
        require(math.isfinite(value) and value >= 0, "metrics scrape contains a non-finite or negative value")
        samples.append((match.group(1), parse_labels(match.group(2) or ""), value))
    return samples


def metric_sum(samples: list[tuple[str, dict[str, str], float]], name: str,
               required_labels: dict[str, str] | None = None) -> float:
    labels = required_labels or {}
    values = [value for metric, observed, value in samples
              if metric == name and all(observed.get(key) == expected for key, expected in labels.items())]
    require(bool(values), f"required metric is absent: {name}")
    return sum(values)


def histogram_p99(samples: list[tuple[str, dict[str, str], float]]) -> float:
    buckets: dict[float, float] = {}
    infinite = 0.0
    for name, labels, value in samples:
        if name != "lbp_proxy_latency_seconds_bucket" or "le" not in labels:
            continue
        if labels["le"] == "+Inf":
            infinite += value
        else:
            boundary = float(labels["le"])
            require(math.isfinite(boundary) and boundary >= 0, "latency histogram boundary is invalid")
            buckets[boundary] = buckets.get(boundary, 0.0) + value
    if infinite <= 0:
        return 0.0
    target = infinite * 0.99
    for boundary in sorted(buckets):
        if buckets[boundary] >= target:
            return boundary * 1000.0
    require(bool(buckets), "latency histogram has no finite buckets")
    return max(buckets) * 1000.0


def scrape_pods(
        pods: list[dict[str, Any]],
) -> tuple[
        list[tuple[str, dict[str, str], float]],
        dict[str, dict[str, Any]],
        dict[str, list[tuple[str, dict[str, str], float]]],
]:
    metrics: list[tuple[str, dict[str, str], float]] = []
    statuses: dict[str, dict[str, Any]] = {}
    metrics_by_pod: dict[str, list[tuple[str, dict[str, str], float]]] = {}
    for pod in pods:
        name = pod["metadata"]["name"]
        pod_metrics = parse_metrics(pod_http(name, CONFIG["proxy"]["localApi"]["metricsPath"]))
        metrics.extend(pod_metrics)
        metrics_by_pod[name] = pod_metrics
        try:
            status = json.loads(pod_http(name, CONFIG["proxy"]["localApi"]["statusPath"]))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise AdapterError(f"proxy status is invalid for pod {name}") from exc
        require(isinstance(status, dict), f"proxy status is invalid for pod {name}")
        statuses[name] = status
    return metrics, statuses, metrics_by_pod


def resource_metrics() -> dict[str, dict[str, str]]:
    namespace = CONFIG["cluster"]["namespace"]
    value = json_command(
        ["get", "--raw", f"/apis/metrics.k8s.io/v1beta1/namespaces/{namespace}/pods"],
        namespaced=False,
    )
    result: dict[str, dict[str, str]] = {}
    for pod in value.get("items", []):
        name = pod.get("metadata", {}).get("name")
        containers = pod.get("containers", [])
        matches = [container for container in containers if container.get("name") == CONFIG["proxy"]["container"]]
        if isinstance(name, str) and len(matches) == 1:
            usage = matches[0].get("usage", {})
            if isinstance(usage.get("cpu"), str) and isinstance(usage.get("memory"), str):
                result[name] = {"cpu": usage["cpu"], "memory": usage["memory"]}
    return result


def deployment_resources(deployment: dict[str, Any]) -> dict[str, int]:
    container = proxy_container(deployment, ("spec", "template", "spec", "containers"))
    resources = container.get("resources", {})
    requests = resources.get("requests", {})
    limits = resources.get("limits", {})
    return {
        "cpuRequestMillis": round(parse_cpu_millis(requests.get("cpu", ""))),
        "cpuLimitMillis": round(parse_cpu_millis(limits.get("cpu", ""))),
        "memoryRequestMiB": round(parse_bytes(requests.get("memory", "")) / (1024 ** 2)),
        "memoryLimitMiB": round(parse_bytes(limits.get("memory", "")) / (1024 ** 2)),
    }


def validate_rollout_strategy(profile: dict[str, Any], deployment: dict[str, Any]) -> None:
    strategy = deployment.get("spec", {}).get("strategy", {})
    rolling = strategy.get("rollingUpdate", {})
    reviewed = profile["deployment"]["rollout"]
    require(strategy.get("type") == "RollingUpdate",
            "live Kubernetes deployment is not using the reviewed rolling-update strategy")
    require(rolling.get("maxUnavailable") == reviewed["maximumUnavailable"],
            "live Kubernetes maxUnavailable differs from the reviewed rollout limit")
    require(rolling.get("maxSurge") == reviewed["maximumSurge"],
            "live Kubernetes maxSurge differs from the reviewed rollout limit")


def validated_pods(profile: dict[str, Any], phase: str) -> tuple[list[dict[str, Any]], str, str, str]:
    reference, digest, revision = expected_identity(profile, phase)
    pods = list_proxy_pods()
    desired = profile["deployment"]["replicas"]
    require(len(pods) == desired, "ready proxy pod count differs from the reviewed replica count")
    for pod in pods:
        spec_container = proxy_container(pod, ("spec", "containers"))
        require(spec_container.get("image") == reference, "ready pod spec is not the reviewed image reference")
        require(pod_image_digest(pod) == digest, "ready pod runtime image digest differs from the reviewed digest")
        require(pod.get("metadata", {}).get("annotations", {}).get(
            CONFIG["proxy"]["sourceRevisionAnnotation"]) == revision,
            "ready pod source revision annotation differs from the reviewed revision")
    return pods, reference, digest, revision


def deployment_snapshot(
        profile: dict[str, Any], phase: str, elapsed_millis: int,
        maximum_unavailable: int, maximum_surge: int, drain_completed: bool) -> dict[str, Any]:
    deployment = get_object("deployment", CONFIG["proxy"]["deployment"])
    validate_rollout_strategy(profile, deployment)
    pods, reference, digest, revision = validated_pods(profile, phase)
    configuration_sha, ingress_sha = live_fingerprints(deployment)
    expected_configuration = profile["deployment"]["configurationSha256"]
    expected_ingress = profile["deployment"]["ingressIdentitySha256"]
    require(configuration_sha == expected_configuration,
            "live Kubernetes configuration fingerprint differs from the reviewed staging profile")
    require(ingress_sha == expected_ingress,
            "live Kubernetes ingress fingerprint differs from the reviewed staging profile")
    status = deployment.get("status", {})
    desired = profile["deployment"]["replicas"]
    zones: dict[str, int] = {}
    for pod in pods:
        node_name = pod.get("spec", {}).get("nodeName")
        require(isinstance(node_name, str) and bool(node_name), "ready pod is not scheduled to a node")
        zone = node_zone(node_name)
        zones[zone] = zones.get(zone, 0) + 1
    metrics, _, metrics_by_pod = scrape_pods(pods)
    required_signals = profile["deployment"]["observability"]["requiredSignals"]
    for pod_name, pod_metrics in metrics_by_pod.items():
        present = {name for name, _, _ in pod_metrics}
        missing = sorted(set(required_signals) - present)
        require(not missing, f"proxy metrics for pod {pod_name} omit a reviewed deployment signal")
    observed_images: dict[str, int] = {}
    for pod in pods:
        observed = pod_image_digest(pod)
        observed_images[observed] = observed_images.get(observed, 0) + 1
    return {
        "schemaVersion": 1,
        "phase": phase,
        "observedAt": now_utc(),
        "sourceRevision": revision,
        "imageReference": reference,
        "imageDigest": digest,
        "configurationSha256": configuration_sha,
        "ingressIdentitySha256": ingress_sha,
        "replicas": {
            "desired": deployment.get("spec", {}).get("replicas", 0),
            "ready": status.get("readyReplicas", 0),
            "available": status.get("availableReplicas", 0),
            "updated": status.get("updatedReplicas", 0),
            "images": [{"imageDigest": key, "replicas": value}
                       for key, value in sorted(observed_images.items())],
        },
        "placement": {"replicasByZone": [value for _, value in sorted(zones.items())]},
        "resources": deployment_resources(deployment),
        "observability": {
            "scrapeHealthy": True,
            "readyReplicaMetrics": len(pods),
            "signals": required_signals,
        },
        "transition": {
            "elapsedMillis": elapsed_millis,
            "maximumUnavailableObserved": maximum_unavailable,
            "maximumSurgeObserved": maximum_surge,
            "drainCompleted": drain_completed,
        },
    }


def convergence_state(profile: dict[str, Any], phase: str, old_uids: set[str]) -> tuple[bool, int, int, bool]:
    deployment = get_object("deployment", CONFIG["proxy"]["deployment"])
    status = deployment.get("status", {})
    desired = profile["deployment"]["replicas"]
    available = int(status.get("availableReplicas", 0) or 0)
    current = int(status.get("replicas", 0) or 0)
    unavailable = max(0, desired - available)
    surge = max(0, current - desired)
    converged = (
        deployment.get("spec", {}).get("replicas") == desired
        and status.get("observedGeneration", 0) >= deployment.get("metadata", {}).get("generation", 0)
        and status.get("readyReplicas", 0) == desired
        and status.get("availableReplicas", 0) == desired
        and status.get("updatedReplicas", 0) == desired
    )
    replacement_complete = False
    if converged:
        try:
            pods, _, _, _ = validated_pods(profile, phase)
            new_uids = {pod.get("metadata", {}).get("uid") for pod in pods}
            replacement_complete = bool(old_uids) and old_uids.isdisjoint(new_uids)
        except AdapterError:
            converged = False
    return converged, unavailable, surge, replacement_complete


def wait_for_main_rollout(profile: dict[str, Any], phase: str, old_uids: set[str], maximum_seconds: int) -> tuple[int, int, int]:
    started = time.monotonic()
    maximum_unavailable = 0
    maximum_surge = 0
    while time.monotonic() - started <= maximum_seconds:
        converged, unavailable, surge, replacement_complete = convergence_state(profile, phase, old_uids)
        maximum_unavailable = max(maximum_unavailable, unavailable)
        maximum_surge = max(maximum_surge, surge)
        if converged and replacement_complete:
            return round((time.monotonic() - started) * 1000), maximum_unavailable, maximum_surge
        time.sleep(1)
    raise AdapterError("proxy deployment did not converge inside the reviewed transition window")


def mutate_main_image(profile: dict[str, Any], phase: str) -> dict[str, Any]:
    reference, _, revision = expected_identity(profile, phase)
    validate_rollout_strategy(profile, get_object("deployment", CONFIG["proxy"]["deployment"]))
    old_uids = {pod.get("metadata", {}).get("uid") for pod in list_proxy_pods()}
    require(len(old_uids) == profile["deployment"]["replicas"],
            "cannot start rollout without every reviewed replica ready")
    run_command(["set", "image", f"deployment/{CONFIG['proxy']['deployment']}",
                 f"{CONFIG['proxy']['container']}={reference}"])
    annotations = {
        CONFIG["proxy"]["sourceRevisionAnnotation"]: revision,
        CONFIG["proxy"]["changeTicketAnnotation"]: profile["environment"]["changeTicket"],
    }
    patch = {"spec": {"template": {"metadata": {"annotations": annotations}}}}
    run_command(["patch", "deployment", CONFIG["proxy"]["deployment"],
                 "--type=merge", "-p", json.dumps(patch, separators=(",", ":"))])
    maximum_seconds = profile["deployment"]["rollout"][
        "maximumRolloutSeconds" if phase == "candidate" else "maximumRollbackSeconds"]
    elapsed, unavailable, surge = wait_for_main_rollout(profile, phase, old_uids, maximum_seconds)
    return deployment_snapshot(profile, phase, elapsed, unavailable, surge, True)


def wait_auxiliary_rollout(deployment: str) -> None:
    timeout_seconds = CONFIG["kubectl"]["maximumCommandSeconds"]
    run_command(["rollout", "status", f"deployment/{deployment}", f"--timeout={timeout_seconds}s"])


def set_slow(active: bool) -> None:
    slow = CONFIG["faults"]["slow"]
    value = slow["activeValue"] if active else slow["baselineValue"]
    run_command(["set", "env", f"deployment/{slow['deployment']}",
                 f"--containers={slow['container']}", f"{slow['environmentVariable']}={value}"])
    wait_auxiliary_rollout(slow["deployment"])


def set_failure(active: bool) -> None:
    failure = CONFIG["faults"]["failure"]
    replicas = 0 if active else failure["baselineReplicas"]
    run_command(["scale", f"deployment/{failure['deployment']}", f"--replicas={replicas}"])
    wait_auxiliary_rollout(failure["deployment"])


def reviewed_payload(action: str) -> bytes:
    reference = CONFIG["faults"][action]
    config_map = get_object("configmap", reference["configMap"])
    metadata = config_map.get("metadata", {})
    require(config_map.get("apiVersion") == "v1" and config_map.get("kind") == "ConfigMap"
            and metadata.get("name") == reference["configMap"]
            and metadata.get("namespace") == CONFIG["cluster"]["namespace"]
            and isinstance(metadata.get("uid"), str) and config_map.get("immutable") is True,
            f"reviewed {action} payload ConfigMap identity or immutability changed")
    payload = config_map.get("data", {}).get(reference["key"])
    require(isinstance(payload, str) and 2 <= len(payload.encode("utf-8")) <= 65_536,
            f"reviewed {action} payload is absent or outside its bounded size")
    require(sha256_bytes(payload.encode("utf-8")) == reference["payloadSha256"],
            f"reviewed {action} payload hash changed")
    try:
        decoded = json.loads(payload)
    except json.JSONDecodeError as exc:
        raise AdapterError(f"reviewed {action} payload is not JSON") from exc
    require(isinstance(decoded, dict), f"reviewed {action} payload must be a JSON object")
    return payload.encode("utf-8")


def post_reload(action: str, profile: dict[str, Any]) -> None:
    payload = reviewed_payload(action)
    pods, _, _, _ = validated_pods(profile, "candidate")
    for pod in pods:
        pod_http(pod["metadata"]["name"], CONFIG["proxy"]["localApi"]["reloadPath"], "POST", payload)


def patch_tls_secret(secret_name: str, profile: dict[str, Any]) -> None:
    deployment = get_object("deployment", CONFIG["proxy"]["deployment"])
    volumes = deployment.get("spec", {}).get("template", {}).get("spec", {}).get("volumes", [])
    matches = [index for index, value in enumerate(volumes)
               if value.get("name") == CONFIG["faults"]["certificateRotation"]["volumeName"]]
    require(len(matches) == 1, "reviewed server-TLS volume is absent or duplicated")
    old_uids = {pod.get("metadata", {}).get("uid") for pod in list_proxy_pods()}
    patch = [{
        "op": "replace",
        "path": f"/spec/template/spec/volumes/{matches[0]}/secret/secretName",
        "value": secret_name,
    }]
    run_command(["patch", "deployment", CONFIG["proxy"]["deployment"],
                 "--type=json", "-p", json.dumps(patch, separators=(",", ":"))])
    wait_for_main_rollout(profile, "candidate", old_uids, profile["deployment"]["rollout"]["maximumRolloutSeconds"])


def restart_main(profile: dict[str, Any]) -> None:
    old_uids = {pod.get("metadata", {}).get("uid") for pod in list_proxy_pods()}
    run_command(["rollout", "restart", f"deployment/{CONFIG['proxy']['deployment']}"])
    wait_for_main_rollout(profile, "candidate", old_uids, profile["deployment"]["rollout"]["maximumRolloutSeconds"])


def reset_environment(profile: dict[str, Any]) -> None:
    set_slow(False)
    set_failure(False)
    post_reload("reset", profile)
    rotation = CONFIG["faults"]["certificateRotation"]
    deployment = get_object("deployment", CONFIG["proxy"]["deployment"])
    volumes = deployment.get("spec", {}).get("template", {}).get("spec", {}).get("volumes", [])
    current = [value.get("secret", {}).get("secretName") for value in volumes
               if value.get("name") == rotation["volumeName"]]
    require(len(current) == 1, "reviewed server-TLS volume is absent or duplicated")
    if current[0] != rotation["baselineSecret"]:
        patch_tls_secret(rotation["baselineSecret"], profile)


def capacity_sample(profile: dict[str, Any]) -> dict[str, Any]:
    phase = os.environ.get("LBP_STAGING_EXPECTED_PHASE", "")
    require(phase == "candidate", "capacity sampler requires the candidate phase")
    pods, reference, _, revision = validated_pods(profile, phase)
    configuration_sha, ingress_sha = live_fingerprints()
    require(configuration_sha == profile["deployment"]["configurationSha256"],
            "capacity sample configuration fingerprint differs from the reviewed staging profile")
    require(ingress_sha == profile["deployment"]["ingressIdentitySha256"],
            "capacity sample ingress fingerprint differs from the reviewed staging profile")
    metrics, statuses, metrics_by_pod = scrape_pods(pods)
    resources = resource_metrics()
    replicas: list[dict[str, Any]] = []
    for pod in pods:
        metadata = pod.get("metadata", {})
        name = metadata.get("name")
        require(name in resources, f"metrics.k8s.io is missing ready pod {name}")
        node_name = pod.get("spec", {}).get("nodeName")
        pod_samples = metrics_by_pod[name]
        replicas.append({
            "id": metadata.get("uid"),
            "zone": node_zone(node_name),
            "ready": True,
            "imageReference": reference,
            "sourceRevision": revision,
            "cpuUsageMillis": parse_cpu_millis(resources[name]["cpu"]),
            "memoryWorkingSetMiB": parse_bytes(resources[name]["memory"]) / (1024 ** 2),
            "openConnections": round(metric_sum(
                pod_samples, CONFIG["capacity"]["openConnectionsMetric"])),
            "jvmLiveThreads": round(metric_sum(pod_samples, CONFIG["capacity"]["jvmThreadsMetric"])),
        })
    upstream_p99 = 0.0
    for status in statuses.values():
        upstreams = status.get("upstreams", [])
        require(isinstance(upstreams, list), "proxy status upstream list is invalid")
        for upstream in upstreams:
            runtime = upstream.get("runtimeStats", {})
            value = runtime.get("p99LatencyMillis", 0)
            require(isinstance(value, (int, float)) and math.isfinite(value) and value >= 0,
                    "proxy status contains an invalid upstream p99")
            upstream_p99 = max(upstream_p99, float(value))
    upstream_totals = {
        upstream: metric_sum(metrics, "lbp_proxy_requests_total", {"upstream": upstream})
        for upstream in CONFIG["capacity"]["upstreamIds"]
    }
    return {
        "schemaVersion": 1,
        "observedAt": now_utc(),
        "phase": "candidate",
        "imageReference": reference,
        "sourceRevision": revision,
        "configurationSha256": configuration_sha,
        "ingressIdentitySha256": ingress_sha,
        "replicas": replicas,
        "metrics": {
            "requestsTotal": metric_sum(metrics, "lbp_proxy_requests_total"),
            "retriesTotal": metric_sum(metrics, "lbp_proxy_retries_total"),
            "shedsTotal": metric_sum(metrics, "lbp_proxy_sheds_total"),
            "limitRejectionsTotal": metric_sum(metrics, "lbp_proxy_limit_rejections_total"),
            "inflight": round(metric_sum(metrics, "lbp_proxy_inflight")),
            "gcPauseCountTotal": metric_sum(metrics, "jvm_gc_pause_seconds_count"),
            "gcPauseSecondsTotal": metric_sum(metrics, "jvm_gc_pause_seconds_sum"),
            "proxyP99Millis": histogram_p99(metrics),
            "upstreamP99Millis": upstream_p99,
            "upstreamRequestsTotal": upstream_totals,
        },
    }


def inspect_cluster() -> dict[str, Any]:
    deployment = get_object("deployment", CONFIG["proxy"]["deployment"])
    configuration_sha, ingress_sha = live_fingerprints(deployment)
    container = proxy_container(deployment, ("spec", "template", "spec", "containers"))
    pods = list_proxy_pods()
    return {
        "schemaVersion": 1,
        "adapterId": CONFIG["adapterId"],
        "observedAt": now_utc(),
        "context": CONFIG["cluster"]["context"],
        "apiServer": CONFIG["cluster"]["apiServer"],
        "namespace": CONFIG["cluster"]["namespace"],
        "namespaceUid": CONFIG["cluster"]["namespaceUid"],
        "deployment": CONFIG["proxy"]["deployment"],
        "configuredImageReference": container.get("image"),
        "readyReplicaIds": sorted(pod.get("metadata", {}).get("uid") for pod in pods),
        "configurationSha256": configuration_sha,
        "ingressIdentitySha256": ingress_sha,
        "resources": deployment_resources(deployment),
        "clusterMutationPerformed": False,
    }


def main() -> int:
    global KUBECTL
    role = Path(sys.argv[0]).name.removesuffix(".sh")
    try:
        require(role in ROLE_NAMES, f"unsupported generated adapter role: {role}")
        KUBECTL = locate_kubectl()
        preflight()
        if role == "inspect":
            result = inspect_cluster()
        else:
            profile = staging_profile()
            if role == "verify-deployment":
                phase = os.environ.get("LBP_STAGING_EXPECTED_PHASE", "")
                require(phase in {"prior", "candidate", "rollback"},
                        "verify-deployment requires a supported expected phase")
                result = deployment_snapshot(profile, phase, 0, 0, 0, True)
            elif role == "rollout-candidate":
                require(os.environ.get("LBP_STAGING_EXPECTED_PHASE") == "candidate",
                        "candidate rollout requires the candidate phase")
                result = mutate_main_image(profile, "candidate")
            elif role == "rollback-prior":
                require(os.environ.get("LBP_STAGING_EXPECTED_PHASE") == "rollback",
                        "rollback requires the rollback phase")
                result = mutate_main_image(profile, "rollback")
            elif role == "slow":
                set_slow(True)
                return 0
            elif role == "failure":
                set_failure(True)
                return 0
            elif role == "reload":
                post_reload("reload", profile)
                return 0
            elif role == "drain":
                post_reload("drain", profile)
                return 0
            elif role == "restart":
                restart_main(profile)
                return 0
            elif role == "certificate-rotation":
                patch_tls_secret(CONFIG["faults"]["certificateRotation"]["candidateSecret"], profile)
                return 0
            elif role == "reset":
                reset_environment(profile)
                return 0
            elif role == "capacity-sampler":
                result = capacity_sample(profile)
            else:
                raise AdapterError("unsupported generated adapter role")
        sys.stdout.write(json.dumps(result, sort_keys=True, separators=(",", ":")) + "\n")
        return 0
    except (AdapterError, json.JSONDecodeError, KeyError, TypeError, ValueError) as exc:
        print(f"Kubernetes staging adapter failed safely: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
