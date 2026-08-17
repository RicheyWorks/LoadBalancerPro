#!/usr/bin/env python3
"""Validate a deployment adapter snapshot against the reviewed staging profile."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any


SHA256 = re.compile(r"^[0-9a-f]{64}$")
REVISION = re.compile(r"^[0-9a-f]{40}$")
PHASES = {"prior", "candidate", "rollback"}


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


def sha256(value: Any, name: str, prefix: bool = False) -> str:
    require(isinstance(value, str), f"{name} must be a string")
    encoded = value.removeprefix("sha256:") if prefix else value
    require((not prefix or value.startswith("sha256:")) and bool(SHA256.fullmatch(encoded)),
            f"{name} must be a lowercase SHA-256{' digest' if prefix else ''}")
    return value


def observed_at(value: Any) -> str:
    require(isinstance(value, str) and value.endswith("Z"), "observedAt must be an RFC3339 UTC timestamp")
    try:
        parsed = datetime.fromisoformat(value.removesuffix("Z") + "+00:00")
    except ValueError as exc:
        raise ContractError("observedAt must be an RFC3339 UTC timestamp") from exc
    age_seconds = (datetime.now(timezone.utc) - parsed).total_seconds()
    require(-30 <= age_seconds <= 300,
            "observedAt must be current within the five-minute deployment snapshot window")
    return value


def validate(profile: dict[str, Any], snapshot: dict[str, Any], expected_phase: str) -> dict[str, Any]:
    require(expected_phase in PHASES, "phase must be prior, candidate, or rollback")
    root_fields = {"schemaVersion", "phase", "observedAt", "sourceRevision", "imageReference",
                   "imageDigest", "configurationSha256", "ingressIdentitySha256", "replicas",
                   "placement", "resources", "observability", "transition"}
    object_at(snapshot, "snapshot", root_fields)
    require(snapshot.get("schemaVersion") == 1, "snapshot.schemaVersion must be 1")
    require(snapshot.get("phase") == expected_phase, "snapshot phase does not match the requested transition")
    observed_at(snapshot.get("observedAt"))

    artifact = object_at(profile.get("artifact"), "profile.artifact",
                         {"registryRepository", "prior", "candidate"})
    artifact_name = "candidate" if expected_phase == "candidate" else "prior"
    identity = object_at(artifact.get(artifact_name), f"profile.artifact.{artifact_name}",
                         {"imageDigest", "sourceRevision"})
    expected_digest = sha256(identity.get("imageDigest"), f"profile.artifact.{artifact_name}.imageDigest", True)
    expected_revision = identity.get("sourceRevision")
    require(isinstance(expected_revision, str) and bool(REVISION.fullmatch(expected_revision)),
            f"profile.artifact.{artifact_name}.sourceRevision is invalid")
    repository = artifact.get("registryRepository")
    require(isinstance(repository, str) and "/" in repository and "@" not in repository,
            "profile artifact registry repository is invalid")
    expected_reference = f"{repository}@{expected_digest}"
    require(snapshot.get("imageDigest") == expected_digest, "snapshot image digest is not the reviewed digest")
    require(snapshot.get("imageReference") == expected_reference,
            "snapshot image reference is not the reviewed registry repository and digest")
    require(snapshot.get("sourceRevision") == expected_revision,
            "snapshot source revision is not the reviewed artifact revision")

    deployment = object_at(profile.get("deployment"), "profile.deployment",
                           {"replicas", "minimumZones", "maximumZoneSkew", "ingressIdentitySha256",
                            "configurationSha256", "resources", "observability", "rollout"})
    sha256(deployment.get("configurationSha256"), "profile.deployment.configurationSha256")
    sha256(deployment.get("ingressIdentitySha256"), "profile.deployment.ingressIdentitySha256")
    sha256(snapshot.get("configurationSha256"), "snapshot.configurationSha256")
    sha256(snapshot.get("ingressIdentitySha256"), "snapshot.ingressIdentitySha256")
    require(snapshot.get("configurationSha256") == deployment.get("configurationSha256"),
            "snapshot configuration fingerprint differs from the reviewed configuration")
    require(snapshot.get("ingressIdentitySha256") == deployment.get("ingressIdentitySha256"),
            "snapshot ingress identity differs from the reviewed ingress")

    desired = integer(deployment.get("replicas"), "profile.deployment.replicas", 2)
    replicas = object_at(snapshot.get("replicas"), "snapshot.replicas",
                         {"desired", "ready", "available", "updated", "images"})
    for name in ("desired", "ready", "available", "updated"):
        require(integer(replicas.get(name), f"snapshot.replicas.{name}") == desired,
                f"snapshot.replicas.{name} must equal the reviewed replica count")
    images = replicas.get("images")
    require(isinstance(images, list) and len(images) == 1,
            "snapshot must report exactly one converged image identity")
    image = object_at(images[0], "snapshot.replicas.images[0]", {"imageDigest", "replicas"})
    require(image.get("imageDigest") == expected_digest and image.get("replicas") == desired,
            "all ready replicas must run the reviewed phase digest")

    placement = object_at(snapshot.get("placement"), "snapshot.placement", {"replicasByZone"})
    by_zone = placement.get("replicasByZone")
    require(isinstance(by_zone, list), "snapshot.placement.replicasByZone must be an array")
    counts = [integer(value, "snapshot.placement.replicasByZone[]", 1) for value in by_zone]
    require(len(counts) >= integer(deployment.get("minimumZones"), "profile.deployment.minimumZones", 2),
            "snapshot does not prove the reviewed minimum zone count")
    require(sum(counts) == desired, "zone replica counts must total the reviewed replica count")
    maximum_skew = integer(deployment.get("maximumZoneSkew"),
                           "profile.deployment.maximumZoneSkew")
    require(max(counts) - min(counts) <= maximum_skew,
            "snapshot exceeds the reviewed maximum zone skew")

    expected_resources = object_at(deployment.get("resources"), "profile.deployment.resources",
                                   {"cpuRequestMillis", "cpuLimitMillis", "memoryRequestMiB",
                                    "memoryLimitMiB"})
    resources = object_at(snapshot.get("resources"), "snapshot.resources", set(expected_resources))
    for name, expected in expected_resources.items():
        integer(expected, f"profile.deployment.resources.{name}", 1)
        require(integer(resources.get(name), f"snapshot.resources.{name}", 1) == expected,
                f"snapshot resource {name} differs from the reviewed deployment")

    expected_observability = object_at(deployment.get("observability"), "profile.deployment.observability",
                                       {"requiredSignals"})
    required_signals = expected_observability.get("requiredSignals")
    require(isinstance(required_signals, list) and bool(required_signals)
            and len(required_signals) == len(set(required_signals)),
            "profile deployment requiredSignals must be a unique non-empty array")
    observability = object_at(snapshot.get("observability"), "snapshot.observability",
                              {"scrapeHealthy", "readyReplicaMetrics", "signals"})
    require(observability.get("scrapeHealthy") is True, "deployment metrics scrape must be healthy")
    require(integer(observability.get("readyReplicaMetrics"),
                    "snapshot.observability.readyReplicaMetrics") == desired,
            "metrics must cover every ready replica")
    signals = observability.get("signals")
    require(isinstance(signals, list) and len(signals) == len(set(signals))
            and set(signals) == set(required_signals),
            "deployment snapshot does not contain the exact reviewed metric signals")

    rollout = object_at(deployment.get("rollout"), "profile.deployment.rollout",
                        {"maximumUnavailable", "maximumSurge", "maximumRolloutSeconds",
                         "maximumRollbackSeconds"})
    transition = object_at(snapshot.get("transition"), "snapshot.transition",
                           {"elapsedMillis", "maximumUnavailableObserved", "maximumSurgeObserved",
                            "drainCompleted"})
    elapsed = integer(transition.get("elapsedMillis"), "snapshot.transition.elapsedMillis")
    unavailable = integer(transition.get("maximumUnavailableObserved"),
                          "snapshot.transition.maximumUnavailableObserved")
    surge = integer(transition.get("maximumSurgeObserved"),
                    "snapshot.transition.maximumSurgeObserved")
    maximum_unavailable = integer(rollout.get("maximumUnavailable"),
                                  "profile.deployment.rollout.maximumUnavailable")
    maximum_surge = integer(rollout.get("maximumSurge"),
                            "profile.deployment.rollout.maximumSurge")
    require(unavailable <= maximum_unavailable,
            "transition exceeded the reviewed unavailable-replica limit")
    require(surge <= maximum_surge, "transition exceeded the reviewed surge limit")
    require(transition.get("drainCompleted") is True, "transition did not prove accepted-work drain")
    if expected_phase == "prior":
        require(elapsed == 0 and surge == 0, "initial prior snapshot cannot claim a deployment transition")
        maximum_millis = 0
    else:
        seconds_field = "maximumRolloutSeconds" if expected_phase == "candidate" else "maximumRollbackSeconds"
        maximum_millis = integer(rollout.get(seconds_field), f"profile.deployment.rollout.{seconds_field}", 1) * 1000
        require(elapsed <= maximum_millis, f"{expected_phase} transition exceeded its reviewed time window")

    return {
        "accepted": True,
        "phase": expected_phase,
        "imageReference": expected_reference,
        "sourceRevision": expected_revision,
        "replicas": desired,
        "zones": len(counts),
        "maximumZoneSkewObserved": max(counts) - min(counts),
        "elapsedMillis": elapsed,
        "maximumElapsedMillis": maximum_millis,
    }


def load_object(path: Path, name: str) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    require(isinstance(value, dict), f"{name} root must be an object")
    return value


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile", required=True, type=Path)
    parser.add_argument("--snapshot", required=True, type=Path)
    parser.add_argument("--phase", required=True, choices=sorted(PHASES))
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    try:
        result = validate(load_object(args.profile, "profile"), load_object(args.snapshot, "snapshot"), args.phase)
        result["snapshotSha256"] = hashlib.sha256(args.snapshot.read_bytes()).hexdigest()
        encoded = json.dumps(result, sort_keys=True, indent=2) + "\n"
        if args.output:
            args.output.write_text(encoded, encoding="utf-8")
        else:
            sys.stdout.write(encoded)
        return 0
    except (OSError, json.JSONDecodeError, ContractError) as exc:
        print(f"staging deployment snapshot rejected: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
