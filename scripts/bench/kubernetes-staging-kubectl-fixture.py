#!/usr/bin/env python3
"""Hermetic kubectl fixture for the Kubernetes staging adapter contract test."""

from __future__ import annotations

import json
import os
from pathlib import Path
import sys
from typing import Any


STATE_PATH = Path(os.environ["LBP_KUBECTL_FIXTURE_STATE"])
NAMESPACE = "lbp-contract-staging"
NAMESPACE_UID = "123e4567-e89b-42d3-a456-426614174000"
CONTEXT = "contract-staging-context"
SERVER = "https://api.staging.internal"
PROXY = "loadbalancerpro"
CONTAINER = "loadbalancerpro"


def load_state() -> dict[str, Any]:
    return json.loads(STATE_PATH.read_text(encoding="utf-8"))


def save_state(state: dict[str, Any]) -> None:
    STATE_PATH.write_text(json.dumps(state, sort_keys=True) + "\n", encoding="utf-8")


def emit(value: Any) -> None:
    sys.stdout.write(json.dumps(value, sort_keys=True, separators=(",", ":")) + "\n")


def metadata(name: str, uid: str, *, labels: dict[str, str] | None = None) -> dict[str, Any]:
    return {"name": name, "namespace": NAMESPACE, "uid": uid, "labels": labels or {}}


def deployment(state: dict[str, Any], name: str) -> dict[str, Any]:
    if name == PROXY:
        return {
            "apiVersion": "apps/v1",
            "kind": "Deployment",
            "metadata": {
                **metadata(PROXY, "20000000-0000-4000-8000-000000000001"),
                "generation": state["generation"],
            },
            "spec": {
                "replicas": 2,
                "strategy": {
                    "type": "RollingUpdate",
                    "rollingUpdate": {
                        "maxUnavailable": state.get("maxUnavailable", 0),
                        "maxSurge": state.get("maxSurge", 1),
                    },
                },
                "selector": {"matchLabels": {"app": PROXY}},
                "template": {
                    "metadata": {
                        "labels": {"app": PROXY},
                        "annotations": {
                            "loadbalancerpro.io/source-revision": state["revision"],
                            "loadbalancerpro.io/change-ticket": "CHG-CONTRACT-1",
                        },
                    },
                    "spec": {
                        "terminationGracePeriodSeconds": 45,
                        "containers": [{
                            "name": CONTAINER,
                            "image": state["imageReference"],
                            "env": [{"name": "SPRING_PROFILES_ACTIVE", "value": "prod,proxy-prod"}],
                            "resources": {
                                "requests": {"cpu": "100m", "memory": "256Mi"},
                                "limits": {"cpu": "1", "memory": "512Mi"},
                            },
                            "lifecycle": {"preStop": {"exec": {"command": ["sh", "-c", "sleep 10"]}}},
                            "volumeMounts": [{"name": "server-tls", "mountPath": "/run/tls", "readOnly": True}],
                        }],
                        "volumes": [{"name": "server-tls", "secret": {"secretName": state["tlsSecret"]}}],
                    },
                },
            },
            "status": {
                "observedGeneration": state["generation"],
                "replicas": 2,
                "readyReplicas": 2,
                "availableReplicas": 2,
                "updatedReplicas": 2,
            },
        }
    replicas = state["failureReplicas"] if name == "backend-b" else 1
    return {
        "apiVersion": "apps/v1", "kind": "Deployment",
        "metadata": {**metadata(name, f"30000000-0000-4000-8000-{1 if name == 'backend-a' else 2:012d}"),
                     "generation": state["generation"]},
        "spec": {"replicas": replicas},
        "status": {"observedGeneration": state["generation"], "replicas": replicas,
                   "readyReplicas": replicas, "availableReplicas": replicas, "updatedReplicas": replicas},
    }


def proxy_pods(state: dict[str, Any]) -> dict[str, Any]:
    pods = []
    for suffix, node in (("a", "node-a"), ("b", "node-b")):
        name = f"loadbalancerpro-{suffix}-{state['podGeneration']}"
        uid = f"40000000-0000-4000-8000-{state['podGeneration'] * 10 + (1 if suffix == 'a' else 2):012d}"
        pods.append({
            "apiVersion": "v1", "kind": "Pod",
            "metadata": {
                **metadata(name, uid, labels={"app": PROXY}),
                "annotations": {"loadbalancerpro.io/source-revision": state["revision"]},
            },
            "spec": {"nodeName": node, "containers": [{"name": CONTAINER, "image": state["imageReference"]}]},
            "status": {
                "conditions": [{"type": "Ready", "status": "True"}],
                "containerStatuses": [{
                    "name": CONTAINER,
                    "image": state["imageReference"],
                    "imageID": f"docker-pullable://{state['imageReference']}",
                    "ready": True,
                }],
            },
        })
    return {"apiVersion": "v1", "kind": "List", "items": pods}


def metrics_api(state: dict[str, Any]) -> dict[str, Any]:
    items = []
    for pod in proxy_pods(state)["items"]:
        items.append({
            "metadata": {"name": pod["metadata"]["name"], "namespace": NAMESPACE},
            "containers": [{"name": CONTAINER, "usage": {"cpu": "250m", "memory": "300Mi"}}],
        })
    return {"apiVersion": "metrics.k8s.io/v1beta1", "kind": "PodMetricsList", "items": items}


def config_map(state: dict[str, Any], name: str) -> dict[str, Any]:
    data: dict[str, str]
    if name == "loadbalancerpro-proxy-prod":
        data = {"LBP_UPSTREAM_0_URL": "http://backend-a:8080", "LBP_UPSTREAM_1_URL": "http://backend-b:8080"}
        if state.get("configurationDrift"):
            data["LBP_UPSTREAM_1_URL"] = "http://unreviewed-backend:8080"
    elif name == "loadbalancerpro-external-config":
        data = {"application.properties": "loadbalancerpro.proxy.enabled=true\n"}
    elif name == "loadbalancerpro-staging-actions":
        data = {
            "reload.json": '{"routes":[{"name":"capacity","pathPrefix":"/proxy/capacity"}]}',
            "drain.json": '{"routes":[{"name":"capacity","pathPrefix":"/proxy/capacity","targets":[]}]}',
            "baseline.json": '{"routes":[{"name":"capacity","pathPrefix":"/proxy/capacity"}]}',
        }
        if state.get("actionPayloadDrift"):
            data["reload.json"] = '{"routes":[{"name":"unreviewed","pathPrefix":"/"}]}'
    else:
        raise KeyError(name)
    return {
        "apiVersion": "v1", "kind": "ConfigMap",
        "metadata": metadata(name, f"50000000-0000-4000-8000-{len(name):012d}"),
        "immutable": True, "data": data,
    }


def secret(name: str) -> dict[str, Any]:
    secret_number = {
        "loadbalancerpro-backend-trust": 1,
        "loadbalancerpro-backend-identity": 2,
        "loadbalancerpro-server-tls-a": 3,
        "loadbalancerpro-server-tls-b": 4,
    }[name]
    material = "Y2FuZGlkYXRlLWNvbnRyYWN0" if name.endswith("-tls-b") else "YmFzZWxpbmUtY29udHJhY3Q="
    return {
        "apiVersion": "v1", "kind": "Secret", "type": "Opaque",
        "metadata": metadata(name, f"60000000-0000-4000-8000-{secret_number:012d}"),
        "immutable": True, "data": {"bundle": material},
    }


def service(state: dict[str, Any]) -> dict[str, Any]:
    return {
        "apiVersion": "v1", "kind": "Service",
        "metadata": metadata(PROXY, "70000000-0000-4000-8000-000000000001"),
        "spec": {
            "type": "ClusterIP", "clusterIP": "10.96.0.21" if state.get("ingressDrift") else "10.96.0.20",
            "selector": {"app": PROXY},
            "ports": [{"name": "https", "port": 443, "protocol": "TCP", "targetPort": "https"}],
        },
    }


def prometheus_metrics(state: dict[str, Any], pod_name: str) -> str:
    metrics = """# TYPE lbp_proxy_requests_total counter
lbp_proxy_requests_total{route="capacity",upstream="backend-a"} 500
lbp_proxy_requests_total{route="capacity",upstream="backend-b"} 500
lbp_proxy_latency_seconds_count{route="capacity"} 1000
lbp_proxy_latency_seconds_bucket{le="0.1"} 900
lbp_proxy_latency_seconds_bucket{le="0.25"} 990
lbp_proxy_latency_seconds_bucket{le="1.0"} 1000
lbp_proxy_latency_seconds_bucket{le="+Inf"} 1000
lbp_proxy_inflight 0
lbp_proxy_retries_total 0
lbp_proxy_sheds_total 0
lbp_proxy_limit_rejections_total 0
process_cpu_usage 0.25
jvm_memory_used_bytes{area="heap"} 104857600
jvm_gc_pause_seconds_count 5
jvm_gc_pause_seconds_sum 0.125
tomcat_connections_current_connections 20
jvm_threads_live_threads 80
"""
    if state.get("missingMetricPod") == pod_name:
        metrics = "\n".join(
            line for line in metrics.splitlines()
            if not line.startswith("lbp_proxy_limit_rejections_total ")
        ) + "\n"
    return metrics


def proxy_status() -> dict[str, Any]:
    return {
        "proxyEnabled": True,
        "upstreams": [
            {"id": "backend-a", "runtimeStats": {"p99LatencyMillis": 85.0}},
            {"id": "backend-b", "runtimeStats": {"p99LatencyMillis": 90.0}},
        ],
    }


def stripped_arguments() -> list[str]:
    arguments = sys.argv[1:]
    result: list[str] = []
    index = 0
    while index < len(arguments):
        argument = arguments[index]
        if argument in {"--context", "--namespace"}:
            index += 2
        elif argument.startswith("--request-timeout="):
            index += 1
        else:
            result.append(argument)
            index += 1
    return result


def main() -> int:
    args = stripped_arguments()
    state = load_state()
    if args == ["config", "current-context"]:
        sys.stdout.write(state.get("currentContext", CONTEXT) + "\n")
        return 0
    if args[:2] == ["config", "view"]:
        emit({"apiVersion": "v1", "kind": "Config", "clusters": [{"name": CONTEXT, "cluster": {"server": state.get("apiServer", SERVER)}}]})
        return 0
    if args[:2] == ["get", "--raw"]:
        emit(metrics_api(state))
        return 0
    if len(args) >= 3 and args[0] == "get":
        kind, name = args[1], args[2]
        if kind == "namespace":
            emit({
                "apiVersion": "v1", "kind": "Namespace",
                "metadata": {"name": NAMESPACE, "uid": state.get("namespaceUid", NAMESPACE_UID),
                             "labels": {"loadbalancerpro.io/environment": state.get("environmentLabel", "staging")}},
            })
        elif kind == "deployment":
            emit(deployment(state, name))
        elif kind == "pods":
            emit(proxy_pods(state))
        elif kind == "node":
            zone = "zone-a" if name == "node-a" else "zone-b"
            emit({"apiVersion": "v1", "kind": "Node", "metadata": {"name": name, "uid": f"80000000-0000-4000-8000-{1 if name == 'node-a' else 2:012d}",
                                                                         "labels": {"topology.kubernetes.io/zone": zone}}})
        elif kind == "configmap":
            emit(config_map(state, name))
        elif kind == "secret":
            emit(secret(name))
        elif kind == "service":
            emit(service(state))
        else:
            return 2
        return 0
    if args[:2] == ["set", "image"]:
        assignment = args[-1]
        state["imageReference"] = assignment.split("=", 1)[1]
        state["digest"] = state["imageReference"].rsplit("@", 1)[1]
        state["generation"] += 1
        state["podGeneration"] += 1
        save_state(state)
        return 0
    if args[:2] == ["set", "env"]:
        assignment = args[-1]
        state["slowValue"] = assignment.split("=", 1)[1]
        save_state(state)
        return 0
    if args and args[0] == "scale":
        replicas = int(next(value for value in args if value.startswith("--replicas=")).split("=", 1)[1])
        state["failureReplicas"] = replicas
        save_state(state)
        return 0
    if args[:2] == ["patch", "deployment"]:
        patch = json.loads(args[args.index("-p") + 1])
        if "--type=merge" in args:
            annotations = patch["spec"]["template"]["metadata"]["annotations"]
            state["revision"] = annotations["loadbalancerpro.io/source-revision"]
        else:
            state["tlsSecret"] = patch[0]["value"]
        state["generation"] += 1
        state["podGeneration"] += 1
        save_state(state)
        return 0
    if args[:2] == ["rollout", "restart"]:
        state["generation"] += 1
        state["podGeneration"] += 1
        save_state(state)
        return 0
    if args[:2] == ["rollout", "status"]:
        return 0
    if args and args[0] == "exec":
        pod_name = args[2]
        url = args[-1]
        if url.endswith("/actuator/prometheus"):
            sys.stdout.write(prometheus_metrics(state, pod_name))
        elif url.endswith("/api/proxy/status"):
            emit(proxy_status())
        elif url.endswith("/actuator/health"):
            emit({"status": "UP"})
        elif url.endswith("/api/proxy/reload"):
            state["reloadCalls"] = state.get("reloadCalls", 0) + 1
            save_state(state)
            emit({"status": "reloaded"})
        else:
            return 2
        return 0
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
