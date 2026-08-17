#!/usr/bin/env python3
"""Validate a deployment-equivalent capacity profile against a staging profile."""

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
PROFILE_ID = re.compile(r"^[a-z0-9][a-z0-9._-]{0,62}$")
UPSTREAM_ID = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
PLACEHOLDER = re.compile(r"replace|example|todo", re.IGNORECASE)
METHODS = {"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"}
STRATEGIES = {
    "TAIL_LATENCY_POWER_OF_TWO",
    "WEIGHTED_LEAST_LOAD",
    "WEIGHTED_LEAST_CONNECTIONS",
    "WEIGHTED_ROUND_ROBIN",
    "ROUND_ROBIN",
    "CONSISTENT_HASH",
}


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


def number(value: Any, name: str, minimum: float = 0, maximum: float | None = None,
           exclusive_minimum: bool = False) -> float:
    require(isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value),
            f"{name} must be a finite number")
    numeric = float(value)
    require(numeric > minimum if exclusive_minimum else numeric >= minimum,
            f"{name} is below its minimum")
    require(maximum is None or numeric <= maximum, f"{name} exceeds its maximum")
    return numeric


def timestamp(value: Any, name: str, required: bool) -> None:
    require(isinstance(value, str), f"{name} must be a string")
    if not value and not required:
        return
    require(value.endswith("Z"), f"{name} must be an RFC3339 UTC timestamp")
    try:
        parsed = datetime.fromisoformat(value.removesuffix("Z") + "+00:00")
    except ValueError as exc:
        raise ContractError(f"{name} must be an RFC3339 UTC timestamp") from exc
    require(parsed <= datetime.now(timezone.utc), f"{name} cannot be in the future")


def load_object(path: Path, name: str) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    require(isinstance(value, dict), f"{name} root must be an object")
    return value


def validate(staging: dict[str, Any], capacity: dict[str, Any], execution: bool,
             staging_bytes: bytes) -> dict[str, Any]:
    object_at(capacity, "capacity profile", {
        "schemaVersion", "profileId", "review", "stagingBinding", "workload", "capacity", "telemetry"
    })
    require(capacity.get("schemaVersion") == 1, "schemaVersion must be 1")
    profile_id = capacity.get("profileId")
    require(isinstance(profile_id, str) and bool(PROFILE_ID.fullmatch(profile_id)),
            "profileId is not safe")

    review = object_at(capacity.get("review"), "review", {"status", "approvedBy", "approvedAt"})
    require(isinstance(review.get("status"), str) and bool(review["status"]),
            "review.status must be a non-empty string")
    require(isinstance(review.get("approvedBy"), str), "review.approvedBy must be a string")
    timestamp(review.get("approvedAt"), "review.approvedAt", execution)
    if execution:
        require(review.get("status") == "reviewed", "execution requires review.status reviewed")
        require(bool(review.get("approvedBy")) and not PLACEHOLDER.search(review["approvedBy"]),
                "execution requires a non-placeholder capacity approver")

    binding = object_at(capacity.get("stagingBinding"), "stagingBinding", {"stagingProfileSha256"})
    binding_hash = binding.get("stagingProfileSha256")
    require(isinstance(binding_hash, str) and bool(SHA256.fullmatch(binding_hash)),
            "stagingBinding.stagingProfileSha256 must be a lowercase SHA-256")
    actual_staging_hash = hashlib.sha256(staging_bytes).hexdigest()
    if execution or binding_hash != "0" * 64:
        require(binding_hash == actual_staging_hash,
                "capacity profile is not bound to the exact staging profile bytes")

    staging_workload = object_at(staging.get("workload"), "staging workload", {"routeMix", "payload"})
    workload = object_at(capacity.get("workload"), "workload", {
        "requestRate", "concurrency", "routeMix", "payload", "upstreamIds", "objectives", "failureModel"
    })
    routes = workload.get("routeMix")
    require(isinstance(routes, list) and bool(routes), "workload.routeMix must be non-empty")
    total_percent = 0
    strategies: set[str] = set()
    for index, route_value in enumerate(routes):
        route = object_at(route_value, f"workload.routeMix[{index}]", {
            "path", "method", "percent", "retryEligible", "affinity", "strategy"
        })
        require(isinstance(route.get("path"), str) and route["path"].startswith("/proxy/"),
                "capacity routes must remain below /proxy/")
        require(route.get("method") in METHODS, "capacity route method is unsupported")
        require(route.get("retryEligible") is (route.get("method") in {"GET", "HEAD"}),
                "retryEligible must match GET/HEAD idempotency")
        require(route.get("affinity") is False, "this runner cannot qualify affinity")
        require(route.get("strategy") in STRATEGIES, "capacity route strategy is unsupported")
        strategies.add(route["strategy"])
        total_percent += integer(route.get("percent"), f"workload.routeMix[{index}].percent", 1)
    require(total_percent == 100 and len(strategies) == 1,
            "capacity route percentages must total 100 and use one strategy")
    require(routes == staging_workload.get("routeMix"),
            "capacity route mix must exactly match the reviewed staging route mix")

    payload = object_at(workload.get("payload"), "workload.payload", {"requestBytes", "responseBytes"})
    for direction, maximum in (("requestBytes", 65_536), ("responseBytes", 1_048_576)):
        percentiles = object_at(payload.get(direction), f"workload.payload.{direction}", {"p50", "p95", "p99"})
        values = [integer(percentiles.get(name), f"workload.payload.{direction}.{name}")
                  for name in ("p50", "p95", "p99")]
        require(values == sorted(values) and values[-1] <= maximum,
                f"workload.payload.{direction} is unordered or too large")
    require(payload == staging_workload.get("payload"),
            "capacity payload must exactly match the reviewed staging payload")

    rates = object_at(workload.get("requestRate"), "workload.requestRate", {
        "normalPerSecond", "peakPerSecond", "burstPerSecond", "burstDurationSeconds",
        "expectedGrowthPercent"
    })
    normal = integer(rates.get("normalPerSecond"), "workload.requestRate.normalPerSecond", 1)
    peak = integer(rates.get("peakPerSecond"), "workload.requestRate.peakPerSecond", 1)
    burst = integer(rates.get("burstPerSecond"), "workload.requestRate.burstPerSecond", 1)
    burst_seconds = integer(rates.get("burstDurationSeconds"),
                            "workload.requestRate.burstDurationSeconds", 1)
    growth = integer(rates.get("expectedGrowthPercent"),
                     "workload.requestRate.expectedGrowthPercent")
    require(normal <= peak <= burst, "normal, peak, and burst rates must be ordered")

    staging_scenarios = object_at(staging.get("scenarios"), "staging scenarios", set(staging.get("scenarios", {})))
    require(staging_scenarios.get("steady", {}).get("ratePerSecond") == normal,
            "capacity normal rate must match reviewed staging steady rate")
    require(staging_scenarios.get("burst", {}).get("ratePerSecond") == burst,
            "capacity burst rate must match reviewed staging burst rate")
    require(staging_scenarios.get("burst", {}).get("durationSeconds") == burst_seconds,
            "capacity burst duration must match reviewed staging burst duration")

    concurrency = object_at(workload.get("concurrency"), "workload.concurrency", {
        "clientConnections", "keepAlive", "connectionChurn"
    })
    integer(concurrency.get("clientConnections"), "workload.concurrency.clientConnections", 1)
    require(isinstance(concurrency.get("keepAlive"), bool), "workload.concurrency.keepAlive must be boolean")
    require(concurrency.get("connectionChurn") == "vegeta-default",
            "only vegeta-default connection churn is supported")

    upstream_ids = workload.get("upstreamIds")
    require(isinstance(upstream_ids, list) and len(upstream_ids) >= 2
            and len(upstream_ids) == len(set(upstream_ids))
            and all(isinstance(item, str) and bool(UPSTREAM_ID.fullmatch(item)) for item in upstream_ids),
            "workload.upstreamIds must contain at least two unique safe IDs")
    objectives = object_at(workload.get("objectives"), "workload.objectives", {
        "minimumSuccessRatio", "normalP99Millis", "slowP99Millis", "failureP99Millis"
    })
    minimum_success = number(objectives.get("minimumSuccessRatio"),
                             "workload.objectives.minimumSuccessRatio", 0, 1, True)
    normal_p99 = number(objectives.get("normalP99Millis"),
                        "workload.objectives.normalP99Millis", 0, exclusive_minimum=True)
    slow_p99 = number(objectives.get("slowP99Millis"),
                      "workload.objectives.slowP99Millis", 0, exclusive_minimum=True)
    failure_p99 = number(objectives.get("failureP99Millis"),
                         "workload.objectives.failureP99Millis", 0, exclusive_minimum=True)
    require(staging_scenarios.get("steady", {}).get("minimumSuccessRatio") == minimum_success
            and staging_scenarios.get("steady", {}).get("p99Millis") == normal_p99
            and staging_scenarios.get("slow", {}).get("p99Millis") == slow_p99
            and staging_scenarios.get("failure", {}).get("p99Millis") == failure_p99,
            "capacity objectives must match the reviewed staging objectives")
    require(workload.get("failureModel") == ["slow", "failure", "drain", "recovery"],
            "failureModel must contain exactly slow, failure, drain, and recovery in order")

    settings = object_at(capacity.get("capacity"), "capacity", {
        "ratesPerSecond", "repeatsPerStep", "warmupSeconds", "measurementSeconds",
        "cooldownSeconds", "sampleIntervalSeconds", "minimumThroughputRatio",
        "failureCaseMinimumSuccessRatio", "maximumNonInjectedRetryRatio",
        "minimumRequestMetricCoverageRatio", "maximumRequestMetricOvercountRatio", "headroomPercent",
        "maximumCpuUtilizationRatio", "maximumMemoryUtilizationRatio", "maxInFlight",
        "maximumGcPauseSecondsPerMeasurement", "maximumOpenConnectionsPerReplica",
        "maximumJvmLiveThreadsPerReplica"
    })
    ladder = settings.get("ratesPerSecond")
    require(isinstance(ladder, list) and len(ladder) >= 2
            and all(isinstance(item, int) and not isinstance(item, bool) and item > 0 for item in ladder)
            and ladder == sorted(set(ladder)),
            "capacity.ratesPerSecond must be a strictly increasing integer ladder")
    integer(settings.get("repeatsPerStep"), "capacity.repeatsPerStep", 3)
    integer(settings.get("warmupSeconds"), "capacity.warmupSeconds", 10)
    measurement = integer(settings.get("measurementSeconds"), "capacity.measurementSeconds", 30)
    require(measurement >= burst_seconds,
            "capacity measurement must cover the reviewed burst duration")
    integer(settings.get("cooldownSeconds"), "capacity.cooldownSeconds", 5)
    sample_interval = integer(settings.get("sampleIntervalSeconds"),
                              "capacity.sampleIntervalSeconds", 1)
    require(sample_interval <= measurement // 3,
            "capacity sampling must produce at least three in-window samples")
    number(settings.get("minimumThroughputRatio"), "capacity.minimumThroughputRatio", 0, 1, True)
    number(settings.get("failureCaseMinimumSuccessRatio"),
           "capacity.failureCaseMinimumSuccessRatio", 0, 1, True)
    number(settings.get("maximumNonInjectedRetryRatio"),
           "capacity.maximumNonInjectedRetryRatio", 0, 1)
    minimum_metric_coverage = number(settings.get("minimumRequestMetricCoverageRatio"),
                                     "capacity.minimumRequestMetricCoverageRatio", 0, 1, True)
    maximum_metric_overcount = number(settings.get("maximumRequestMetricOvercountRatio"),
                                      "capacity.maximumRequestMetricOvercountRatio", 1, 2)
    require(minimum_metric_coverage <= maximum_metric_overcount,
            "capacity metric coverage bounds are inverted")
    headroom = integer(settings.get("headroomPercent"), "capacity.headroomPercent", 1)
    require(headroom < 100, "capacity.headroomPercent must be below 100")
    number(settings.get("maximumCpuUtilizationRatio"),
           "capacity.maximumCpuUtilizationRatio", 0, 1, True)
    number(settings.get("maximumMemoryUtilizationRatio"),
           "capacity.maximumMemoryUtilizationRatio", 0, 1, True)
    number(settings.get("maximumGcPauseSecondsPerMeasurement"),
           "capacity.maximumGcPauseSecondsPerMeasurement", 0, exclusive_minimum=True)
    integer(settings.get("maxInFlight"), "capacity.maxInFlight", 1)
    integer(settings.get("maximumOpenConnectionsPerReplica"),
            "capacity.maximumOpenConnectionsPerReplica", 1)
    integer(settings.get("maximumJvmLiveThreadsPerReplica"),
            "capacity.maximumJvmLiveThreadsPerReplica", 1)
    grown_peak = math.ceil(peak * (100 + growth) / 100)
    required_rate = max(burst, math.ceil(grown_peak * (100 + headroom) / 100))
    require(max(ladder) >= required_rate,
            f"capacity ladder must reach forecast growth, headroom, and burst load ({required_rate}/s)")

    telemetry = object_at(capacity.get("telemetry"), "telemetry", {
        "samplerSha256", "maximumSampleAgeSeconds", "maximumSamplerSeconds"
    })
    sampler_hash = telemetry.get("samplerSha256")
    require(isinstance(sampler_hash, str) and bool(SHA256.fullmatch(sampler_hash)),
            "telemetry.samplerSha256 must be a lowercase SHA-256")
    if execution:
        require(sampler_hash != "0" * 64, "execution requires a reviewed telemetry sampler hash")
    maximum_sample_age = integer(telemetry.get("maximumSampleAgeSeconds"),
                                 "telemetry.maximumSampleAgeSeconds", 5)
    require(maximum_sample_age <= 300, "telemetry.maximumSampleAgeSeconds must be <= 300")
    maximum_sampler_seconds = integer(telemetry.get("maximumSamplerSeconds"),
                                      "telemetry.maximumSamplerSeconds", 1)
    require(maximum_sampler_seconds < sample_interval,
            "telemetry sampler timeout must be shorter than the sampling interval")

    if execution:
        staging_review = object_at(staging.get("review"), "staging review",
                                   {"status", "approvedBy", "approvedAt"})
        require(staging_review.get("status") == "reviewed",
                "execution requires the bound staging profile to be reviewed")
        environment = staging.get("environment", {})
        require(environment.get("billableImpactReviewed") is True
                and environment.get("productionTrafficAuthorized") is False,
                "execution requires reviewed non-production cost authority")

    return {
        "accepted": True,
        "executionAuthorized": execution,
        "profileId": profile_id,
        "stagingProfileId": staging.get("profileId"),
        "stagingProfileSha256": actual_staging_hash,
        "candidateImageReference": (
            f"{staging['artifact']['registryRepository']}@{staging['artifact']['candidate']['imageDigest']}"
        ),
        "candidateSourceRevision": staging["artifact"]["candidate"]["sourceRevision"],
        "replicas": staging["deployment"]["replicas"],
        "minimumZones": staging["deployment"]["minimumZones"],
        "requiredQualificationRate": required_rate,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--staging-profile", required=True, type=Path)
    parser.add_argument("--capacity-profile", required=True, type=Path)
    parser.add_argument("--execution", action="store_true")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    try:
        staging_bytes = args.staging_profile.read_bytes()
        staging = json.loads(staging_bytes.decode("utf-8"))
        capacity = load_object(args.capacity_profile, "capacity profile")
        require(isinstance(staging, dict), "staging profile root must be an object")
        result = validate(staging, capacity, args.execution, staging_bytes)
        result["capacityProfileSha256"] = hashlib.sha256(args.capacity_profile.read_bytes()).hexdigest()
        encoded = json.dumps(result, sort_keys=True, indent=2) + "\n"
        if args.output:
            args.output.write_text(encoded, encoding="utf-8")
        else:
            sys.stdout.write(encoded)
        return 0
    except (OSError, UnicodeDecodeError, json.JSONDecodeError, KeyError, ContractError) as exc:
        print(f"staging capacity profile rejected: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
