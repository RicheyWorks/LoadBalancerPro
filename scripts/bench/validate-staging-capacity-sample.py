#!/usr/bin/env python3
"""Validate one exact-candidate, per-replica staging capacity telemetry sample."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import math
import re
import sys
from pathlib import Path
from typing import Any


SHA256 = re.compile(r"^[0-9a-f]{64}$")
REVISION = re.compile(r"^[0-9a-f]{40}$")
IDENTIFIER = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._:/-]{0,255}$")


class ContractError(ValueError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ContractError(message)


def object_at(value: Any, name: str, fields: set[str]) -> dict[str, Any]:
    require(isinstance(value, dict), f"{name} must be an object")
    require(set(value) == fields, f"{name} contains unexpected or missing fields")
    return value


def integer(value: Any, name: str, minimum: int = 0) -> int:
    require(isinstance(value, int) and not isinstance(value, bool) and value >= minimum,
            f"{name} must be an integer >= {minimum}")
    return value


def number(value: Any, name: str, minimum: float = 0) -> float:
    require(isinstance(value, (int, float)) and not isinstance(value, bool)
            and math.isfinite(value) and value >= minimum,
            f"{name} must be a finite number >= {minimum}")
    return float(value)


def fingerprint(value: Any, name: str) -> str:
    require(isinstance(value, str) and bool(SHA256.fullmatch(value)),
            f"{name} must be a lowercase SHA-256")
    return value


def load_object(path: Path, name: str) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    require(isinstance(value, dict), f"{name} root must be an object")
    return value


def observed_at(value: Any, maximum_age_seconds: int) -> str:
    require(isinstance(value, str) and value.endswith("Z"),
            "observedAt must be an RFC3339 UTC timestamp")
    try:
        parsed = datetime.fromisoformat(value.removesuffix("Z") + "+00:00")
    except ValueError as exc:
        raise ContractError("observedAt must be an RFC3339 UTC timestamp") from exc
    age_seconds = (datetime.now(timezone.utc) - parsed).total_seconds()
    require(-30 <= age_seconds <= maximum_age_seconds,
            "observedAt is stale or implausibly far in the future")
    return value


def validate(staging: dict[str, Any], capacity: dict[str, Any], sample: dict[str, Any]) -> dict[str, Any]:
    object_at(sample, "sample", {
        "schemaVersion", "observedAt", "phase", "imageReference", "sourceRevision",
        "configurationSha256", "ingressIdentitySha256", "replicas", "metrics"
    })
    require(sample.get("schemaVersion") == 1, "sample.schemaVersion must be 1")
    require(sample.get("phase") == "candidate", "capacity telemetry must report candidate phase")
    maximum_age = integer(capacity.get("telemetry", {}).get("maximumSampleAgeSeconds"),
                          "capacity.telemetry.maximumSampleAgeSeconds", 1)
    timestamp = observed_at(sample.get("observedAt"), maximum_age)

    artifact = object_at(staging.get("artifact"), "staging.artifact",
                         {"registryRepository", "prior", "candidate"})
    candidate = object_at(artifact.get("candidate"), "staging.artifact.candidate",
                          {"imageDigest", "sourceRevision"})
    expected_reference = f"{artifact.get('registryRepository')}@{candidate.get('imageDigest')}"
    require(sample.get("imageReference") == expected_reference,
            "sample imageReference is not the reviewed candidate")
    expected_revision = candidate.get("sourceRevision")
    require(isinstance(expected_revision, str) and bool(REVISION.fullmatch(expected_revision)),
            "reviewed candidate sourceRevision is invalid")
    require(sample.get("sourceRevision") == expected_revision,
            "sample sourceRevision is not the reviewed candidate")

    deployment = object_at(staging.get("deployment"), "staging.deployment", {
        "replicas", "minimumZones", "maximumZoneSkew", "ingressIdentitySha256",
        "configurationSha256", "resources", "observability", "rollout"
    })
    require(fingerprint(sample.get("configurationSha256"), "sample.configurationSha256")
            == deployment.get("configurationSha256"),
            "sample configuration fingerprint differs from the reviewed deployment")
    require(fingerprint(sample.get("ingressIdentitySha256"), "sample.ingressIdentitySha256")
            == deployment.get("ingressIdentitySha256"),
            "sample ingress identity differs from the reviewed deployment")
    resources = object_at(deployment.get("resources"), "staging.deployment.resources", {
        "cpuRequestMillis", "cpuLimitMillis", "memoryRequestMiB", "memoryLimitMiB"
    })
    cpu_limit = integer(resources.get("cpuLimitMillis"), "staging CPU limit", 1)
    memory_limit = integer(resources.get("memoryLimitMiB"), "staging memory limit", 1)
    desired = integer(deployment.get("replicas"), "staging.deployment.replicas", 2)

    replicas = sample.get("replicas")
    require(isinstance(replicas, list) and len(replicas) == desired,
            "sample must report every reviewed replica exactly once")
    replica_ids: list[str] = []
    zone_counts: dict[str, int] = {}
    cpu_ratios: list[float] = []
    memory_ratios: list[float] = []
    open_connections: list[int] = []
    live_threads: list[int] = []
    for index, value in enumerate(replicas):
        replica = object_at(value, f"sample.replicas[{index}]", {
            "id", "zone", "ready", "imageReference", "sourceRevision", "cpuUsageMillis",
            "memoryWorkingSetMiB", "openConnections", "jvmLiveThreads"
        })
        replica_id = replica.get("id")
        zone = replica.get("zone")
        require(isinstance(replica_id, str) and bool(IDENTIFIER.fullmatch(replica_id)),
                f"sample.replicas[{index}].id is invalid")
        require(isinstance(zone, str) and bool(IDENTIFIER.fullmatch(zone)),
                f"sample.replicas[{index}].zone is invalid")
        require(replica.get("ready") is True, "all sampled replicas must be ready")
        require(replica.get("imageReference") == expected_reference,
                "sampled replica is not running the reviewed candidate image")
        require(replica.get("sourceRevision") == expected_revision,
                "sampled replica is not running the reviewed candidate revision")
        replica_ids.append(replica_id)
        zone_counts[zone] = zone_counts.get(zone, 0) + 1
        cpu_ratios.append(number(replica.get("cpuUsageMillis"),
                                 f"sample.replicas[{index}].cpuUsageMillis") / cpu_limit)
        memory_ratios.append(number(replica.get("memoryWorkingSetMiB"),
                                    f"sample.replicas[{index}].memoryWorkingSetMiB") / memory_limit)
        open_connections.append(integer(replica.get("openConnections"),
                                        f"sample.replicas[{index}].openConnections"))
        live_threads.append(integer(replica.get("jvmLiveThreads"),
                                    f"sample.replicas[{index}].jvmLiveThreads"))
    require(len(replica_ids) == len(set(replica_ids)), "sample replica IDs must be unique")
    minimum_zones = integer(deployment.get("minimumZones"), "staging.deployment.minimumZones", 2)
    require(len(zone_counts) >= minimum_zones, "sample does not cover the reviewed minimum zones")
    maximum_skew = integer(deployment.get("maximumZoneSkew"),
                           "staging.deployment.maximumZoneSkew")
    require(max(zone_counts.values()) - min(zone_counts.values()) <= maximum_skew,
            "sample exceeds the reviewed maximum zone skew")

    metrics = object_at(sample.get("metrics"), "sample.metrics", {
        "requestsTotal", "retriesTotal", "shedsTotal", "limitRejectionsTotal", "inflight",
        "gcPauseCountTotal", "gcPauseSecondsTotal", "proxyP99Millis", "upstreamP99Millis",
        "upstreamRequestsTotal"
    })
    counters: dict[str, float] = {}
    for name in ("requestsTotal", "retriesTotal", "shedsTotal", "limitRejectionsTotal",
                 "gcPauseCountTotal", "gcPauseSecondsTotal"):
        counters[name] = number(metrics.get(name), f"sample.metrics.{name}")
    inflight = integer(metrics.get("inflight"), "sample.metrics.inflight")
    proxy_p99 = number(metrics.get("proxyP99Millis"), "sample.metrics.proxyP99Millis")
    upstream_p99 = number(metrics.get("upstreamP99Millis"), "sample.metrics.upstreamP99Millis")
    upstream_totals = metrics.get("upstreamRequestsTotal")
    expected_upstreams = capacity.get("workload", {}).get("upstreamIds")
    require(isinstance(upstream_totals, dict) and set(upstream_totals) == set(expected_upstreams),
            "sample upstream request counters must exactly cover reviewed upstream IDs")
    normalized_upstreams = {
        key: number(value, f"sample.metrics.upstreamRequestsTotal.{key}")
        for key, value in upstream_totals.items()
    }

    return {
        "accepted": True,
        "observedAt": timestamp,
        "imageReference": expected_reference,
        "sourceRevision": expected_revision,
        "replicaIds": sorted(replica_ids),
        "zones": len(zone_counts),
        "maximumZoneSkewObserved": max(zone_counts.values()) - min(zone_counts.values()),
        "maximumCpuUtilizationRatio": max(cpu_ratios),
        "maximumMemoryUtilizationRatio": max(memory_ratios),
        "maximumOpenConnectionsPerReplica": max(open_connections),
        "maximumJvmLiveThreadsPerReplica": max(live_threads),
        "inflight": inflight,
        "proxyP99Millis": proxy_p99,
        "upstreamP99Millis": upstream_p99,
        "counters": counters,
        "upstreamRequestsTotal": normalized_upstreams,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--staging-profile", required=True, type=Path)
    parser.add_argument("--capacity-profile", required=True, type=Path)
    parser.add_argument("--sample", required=True, type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    try:
        result = validate(load_object(args.staging_profile, "staging profile"),
                          load_object(args.capacity_profile, "capacity profile"),
                          load_object(args.sample, "sample"))
        result["sampleSha256"] = hashlib.sha256(args.sample.read_bytes()).hexdigest()
        encoded = json.dumps(result, sort_keys=True, indent=2) + "\n"
        if args.output:
            args.output.write_text(encoded, encoding="utf-8")
        else:
            sys.stdout.write(encoded)
        return 0
    except (OSError, json.JSONDecodeError, KeyError, TypeError, ContractError) as exc:
        print(f"staging capacity telemetry rejected: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
