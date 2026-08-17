#!/usr/bin/env python3
"""Recompute a staging capacity envelope from strict raw scenario measurements."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import sys
from pathlib import Path
from typing import Any


SCENARIOS = ("equal", "slow", "failing", "draining", "recovering")
INJECTED = {"equal": False, "slow": False, "failing": True, "draining": False, "recovering": False}


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


def boolean(value: Any, name: str) -> bool:
    require(isinstance(value, bool), f"{name} must be boolean")
    return value


def load_object(path: Path, name: str) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    require(isinstance(value, dict), f"{name} root must be an object")
    return value


def load_measurements(path: Path) -> list[dict[str, Any]]:
    measurements: list[dict[str, Any]] = []
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        require(bool(line.strip()), f"measurements line {line_number} is empty")
        try:
            value = json.loads(line)
        except json.JSONDecodeError as exc:
            raise ContractError(f"measurements line {line_number} is invalid JSON") from exc
        require(isinstance(value, dict), f"measurements line {line_number} must be an object")
        measurements.append(value)
    require(bool(measurements), "measurements file must not be empty")
    return measurements


def validate_deployment(value: dict[str, Any], name: str, phase: str,
                        image_reference: str, revision: str) -> dict[str, Any]:
    object_at(value, name, {
        "accepted", "phase", "imageReference", "sourceRevision", "replicas", "zones",
        "maximumZoneSkewObserved", "elapsedMillis", "maximumElapsedMillis", "snapshotSha256"
    })
    require(value.get("accepted") is True and value.get("phase") == phase,
            f"{name} is not an accepted {phase} deployment validation")
    require(value.get("imageReference") == image_reference and value.get("sourceRevision") == revision,
            f"{name} is not bound to the reviewed phase artifact")
    for field in ("replicas", "zones", "maximumZoneSkewObserved", "elapsedMillis", "maximumElapsedMillis"):
        integer(value.get(field), f"{name}.{field}")
    snapshot_hash = value.get("snapshotSha256")
    require(isinstance(snapshot_hash, str) and len(snapshot_hash) == 64
            and all(character in "0123456789abcdef" for character in snapshot_hash),
            f"{name}.snapshotSha256 is invalid")
    return value


def saturation_signals(measurement: dict[str, Any], capacity: dict[str, Any],
                       objectives: dict[str, Any], upstream_ids: list[str],
                       maximum_proxy_overhead: float, scenario: str) -> tuple[bool, list[str]]:
    client = object_at(measurement.get("client"), "measurement.client", {
        "requests", "achievedThroughput", "throughputRatio", "completionRatio", "successRatio",
        "p50Millis", "p95Millis", "p99Millis", "p99BudgetMillis", "statusCodes", "errors"
    })
    metrics = object_at(measurement.get("deploymentMetrics"), "measurement.deploymentMetrics", {
        "requestsDelta", "retriesDelta", "retryRatio", "shedsDelta", "limitRejectionsDelta",
        "gcPauseCountDelta", "gcPauseSecondsDelta", "requestMetricCoverageRatio", "proxyOverheadP99Millis"
    })
    runtime = object_at(measurement.get("runtime"), "measurement.runtime", {
        "samples", "stableReplicaSet", "maxCpuUtilizationRatio", "maxMemoryUtilizationRatio",
        "maxOpenConnectionsPerReplica", "maxJvmLiveThreadsPerReplica", "maxInflight",
        "maxProxyP99Millis", "maxUpstreamP99Millis", "firstCounters", "lastCounters",
        "firstUpstreamRequests", "lastUpstreamRequests", "counterMonotonic"
    })
    integer(client.get("requests"), "measurement.client.requests", 1)
    for field in ("achievedThroughput", "throughputRatio", "completionRatio", "successRatio",
                  "p50Millis", "p95Millis", "p99Millis", "p99BudgetMillis"):
        number(client.get(field), f"measurement.client.{field}")
    require(client["p50Millis"] <= client["p95Millis"] <= client["p99Millis"],
            "client latency percentiles are unordered")
    require(client["successRatio"] <= 1, "client success ratio exceeds one")
    require(isinstance(client.get("statusCodes"), dict), "client.statusCodes must be an object")
    require(isinstance(client.get("errors"), list), "client.errors must be an array")
    for field in ("requestsDelta", "retriesDelta", "shedsDelta", "limitRejectionsDelta",
                  "gcPauseCountDelta", "gcPauseSecondsDelta"):
        number(metrics.get(field), f"measurement.deploymentMetrics.{field}")
    for field in ("retryRatio", "requestMetricCoverageRatio", "proxyOverheadP99Millis"):
        number(metrics.get(field), f"measurement.deploymentMetrics.{field}")
    integer(runtime.get("samples"), "measurement.runtime.samples", 3)
    for field in ("maxCpuUtilizationRatio", "maxMemoryUtilizationRatio",
                  "maxOpenConnectionsPerReplica", "maxJvmLiveThreadsPerReplica", "maxInflight",
                  "maxProxyP99Millis", "maxUpstreamP99Millis"):
        number(runtime.get(field), f"measurement.runtime.{field}")
    boolean(runtime.get("stableReplicaSet"), "measurement.runtime.stableReplicaSet")
    boolean(runtime.get("counterMonotonic"), "measurement.runtime.counterMonotonic")
    for field in ("firstCounters", "lastCounters"):
        counters = runtime.get(field)
        require(isinstance(counters, dict) and set(counters) == {
            "requestsTotal", "retriesTotal", "shedsTotal", "limitRejectionsTotal",
            "gcPauseCountTotal", "gcPauseSecondsTotal"
        }, f"measurement.runtime.{field} must contain the exact reviewed counters")
        for key, value in counters.items():
            number(value, f"measurement.runtime.{field}.{key}")
    for field in ("firstUpstreamRequests", "lastUpstreamRequests"):
        upstreams = runtime.get(field)
        require(isinstance(upstreams, dict) and bool(upstreams),
                f"measurement.runtime.{field} must be a non-empty object")
        require(set(upstreams) == set(upstream_ids),
                f"measurement.runtime.{field} does not cover reviewed upstreams")
        for key, value in upstreams.items():
            number(value, f"measurement.runtime.{field}.{key}")
    quiesced = boolean(measurement.get("quiesced"), "measurement.quiesced")
    delta_fields = {
        "requestsDelta": "requestsTotal",
        "retriesDelta": "retriesTotal",
        "shedsDelta": "shedsTotal",
        "limitRejectionsDelta": "limitRejectionsTotal",
        "gcPauseCountDelta": "gcPauseCountTotal",
        "gcPauseSecondsDelta": "gcPauseSecondsTotal",
    }
    for delta_name, counter_name in delta_fields.items():
        expected_delta = runtime["lastCounters"][counter_name] - runtime["firstCounters"][counter_name]
        require(expected_delta >= 0 and math.isclose(metrics[delta_name], expected_delta, abs_tol=1e-9),
                f"measurement.deploymentMetrics.{delta_name} does not match telemetry counters")
    expected_retry_ratio = metrics["retriesDelta"] / client["requests"]
    expected_coverage = metrics["requestsDelta"] / client["requests"]
    expected_overhead = max(client["p99Millis"] - runtime["maxUpstreamP99Millis"], 0)
    require(math.isclose(metrics["retryRatio"], expected_retry_ratio, abs_tol=1e-6),
            "measurement retry ratio does not match raw counters")
    require(math.isclose(metrics["requestMetricCoverageRatio"], expected_coverage, abs_tol=1e-6),
            "measurement metric coverage does not match raw counters")
    require(math.isclose(metrics["proxyOverheadP99Millis"], expected_overhead, abs_tol=1e-6),
            "measurement proxy overhead does not match client and upstream latency")

    expected_budget = {
        "equal": objectives["normalP99Millis"],
        "slow": objectives["slowP99Millis"],
        "failing": objectives["failureP99Millis"],
        "draining": objectives["normalP99Millis"],
        "recovering": objectives["normalP99Millis"],
    }[scenario]
    require(client["p99BudgetMillis"] == expected_budget,
            f"{scenario} p99 budget differs from the reviewed objective")
    expected_minimum_success = (capacity["failureCaseMinimumSuccessRatio"] if INJECTED[scenario]
                                else objectives["minimumSuccessRatio"])
    require(measurement.get("minimumSuccessRatio") == expected_minimum_success,
            f"{scenario} minimum success ratio differs from the reviewed objective")

    throughput_observation = client["completionRatio"] if INJECTED[scenario] else client["throughputRatio"]
    signals: list[str] = []
    if throughput_observation < capacity["minimumThroughputRatio"]:
        signals.append("throughput")
    if client["successRatio"] < expected_minimum_success:
        signals.append("success")
    if client["p99Millis"] > expected_budget:
        signals.append("p99")
    if not INJECTED[scenario] and metrics["retryRatio"] > capacity["maximumNonInjectedRetryRatio"]:
        signals.append("retries")
    if metrics["shedsDelta"] > 0:
        signals.append("sheds")
    if metrics["limitRejectionsDelta"] > 0:
        signals.append("safety-limit")
    if not (capacity["minimumRequestMetricCoverageRatio"]
            <= metrics["requestMetricCoverageRatio"]
            <= capacity["maximumRequestMetricOvercountRatio"]):
        signals.append("metric-coverage")
    if metrics["proxyOverheadP99Millis"] > maximum_proxy_overhead:
        signals.append("proxy-overhead")
    if metrics["gcPauseSecondsDelta"] > capacity["maximumGcPauseSecondsPerMeasurement"]:
        signals.append("gc-pause")
    if not quiesced:
        signals.append("inflight-not-quiesced")
    if runtime["maxCpuUtilizationRatio"] > capacity["maximumCpuUtilizationRatio"]:
        signals.append("cpu")
    if runtime["maxMemoryUtilizationRatio"] > capacity["maximumMemoryUtilizationRatio"]:
        signals.append("memory")
    if runtime["maxInflight"] >= capacity["maxInFlight"]:
        signals.append("max-inflight")
    if runtime["maxOpenConnectionsPerReplica"] > capacity["maximumOpenConnectionsPerReplica"]:
        signals.append("connections")
    if runtime["maxJvmLiveThreadsPerReplica"] > capacity["maximumJvmLiveThreadsPerReplica"]:
        signals.append("threads")
    if not runtime["stableReplicaSet"]:
        signals.append("replica-restart")
    if not runtime["counterMonotonic"]:
        signals.append("counter-regression")
    return not signals, signals


def evaluate(staging: dict[str, Any], capacity_profile: dict[str, Any],
             measurements: list[dict[str, Any]], deployments: dict[str, dict[str, Any]],
             staging_bytes: bytes) -> tuple[dict[str, Any], list[dict[str, Any]], list[dict[str, Any]]]:
    binding = capacity_profile.get("stagingBinding", {}).get("stagingProfileSha256")
    require(binding == hashlib.sha256(staging_bytes).hexdigest(),
            "capacity result is not bound to the exact staging profile bytes")
    settings = capacity_profile.get("capacity")
    objectives = capacity_profile.get("workload", {}).get("objectives")
    request_rate = capacity_profile.get("workload", {}).get("requestRate")
    upstream_ids = capacity_profile.get("workload", {}).get("upstreamIds")
    maximum_proxy_overhead = staging.get("thresholds", {}).get("maximumProxyOverheadP99Millis")
    require(isinstance(settings, dict) and isinstance(objectives, dict) and isinstance(request_rate, dict)
            and isinstance(upstream_ids, list) and bool(upstream_ids)
            and isinstance(maximum_proxy_overhead, (int, float)) and maximum_proxy_overhead > 0,
            "capacity profile is missing evaluation inputs")
    ladder = settings.get("ratesPerSecond")
    repeats = integer(settings.get("repeatsPerStep"), "capacity.repeatsPerStep", 3)
    require(isinstance(ladder, list) and bool(ladder), "capacity rate ladder is missing")

    seen: set[tuple[int, int, str]] = set()
    normalized: list[dict[str, Any]] = []
    root_fields = {
        "scenario", "rate", "repeat", "injectedFailure", "casePassed", "saturated", "quiesced",
        "client", "deploymentMetrics", "runtime", "minimumSuccessRatio", "saturationSignals"
    }
    for index, measurement in enumerate(measurements):
        object_at(measurement, f"measurement[{index}]", root_fields)
        scenario = measurement.get("scenario")
        require(scenario in SCENARIOS, f"measurement[{index}] has an unexpected scenario")
        rate = integer(measurement.get("rate"), f"measurement[{index}].rate", 1)
        repeat = integer(measurement.get("repeat"), f"measurement[{index}].repeat", 1)
        require(rate in ladder and repeat <= repeats, "measurement rate or repeat is outside the reviewed ladder")
        key = (rate, repeat, scenario)
        require(key not in seen, f"duplicate measurement {key}")
        seen.add(key)
        require(measurement.get("injectedFailure") is INJECTED[scenario],
                f"{scenario} injectedFailure classification is invalid")
        passed, signals = saturation_signals(
            measurement, settings, objectives, upstream_ids, float(maximum_proxy_overhead), scenario)
        require(measurement.get("casePassed") is passed,
                f"{scenario} casePassed does not match recomputed thresholds")
        saturated = (not INJECTED[scenario]) and not passed
        require(measurement.get("saturated") is saturated,
                f"{scenario} saturated does not match recomputed thresholds")
        claimed_signals = measurement.get("saturationSignals")
        require(isinstance(claimed_signals, list) and claimed_signals == signals,
                f"{scenario} saturationSignals do not match recomputed thresholds")
        normalized.append(measurement)

    executed_rates = sorted({item[0] for item in seen})
    require(executed_rates == ladder[:len(executed_rates)],
            "executed rates must be a non-empty prefix of the reviewed ladder")
    expected_keys = {
        (rate, repeat, scenario)
        for rate in executed_rates for repeat in range(1, repeats + 1) for scenario in SCENARIOS
    }
    require(seen == expected_keys, "measurement matrix is incomplete")

    repeat_summaries: list[dict[str, Any]] = []
    for rate in executed_rates:
        for repeat in range(1, repeats + 1):
            cases = [item for item in normalized if item["rate"] == rate and item["repeat"] == repeat]
            repeat_summaries.append({
                "rate": rate,
                "repeat": repeat,
                "saturated": any(item["saturated"] for item in cases),
                "casesPassed": all(item["casePassed"] for item in cases),
                "saturationSignals": sorted({signal for item in cases for signal in item["saturationSignals"]}),
            })

    majority = repeats // 2 + 1
    rate_summaries: list[dict[str, Any]] = []
    for rate in executed_rates:
        summaries = [item for item in repeat_summaries if item["rate"] == rate]
        saturated_repeats = sum(1 for item in summaries if item["saturated"])
        classification = "pass"
        if saturated_repeats >= majority:
            classification = "saturation"
        elif saturated_repeats:
            classification = "unstable"
        rate_summaries.append({
            "rate": rate,
            "classification": classification,
            "repeats": repeats,
            "saturatedRepeats": saturated_repeats,
            "caseFailureRepeats": sum(1 for item in summaries if not item["casesPassed"]),
        })
    first_observed_saturation = next((index for index, item in enumerate(rate_summaries)
                                      if item["saturatedRepeats"] > 0), None)
    if first_observed_saturation is not None:
        require(first_observed_saturation == len(rate_summaries) - 1,
                "runner continued after observing saturation")
    elif len(executed_rates) < len(ladder):
        raise ContractError("runner stopped before saturation or the end of the reviewed ladder")

    first_saturation = next((item["rate"] for item in rate_summaries
                             if item["classification"] == "saturation"), 0)
    unstable_steps = sum(1 for item in rate_summaries if item["classification"] == "unstable")
    highest_passing = max((item["rate"] for item in rate_summaries
                           if item["classification"] == "pass"), default=0)
    operating_envelope = 0
    if first_saturation:
        reserved_envelope = first_saturation * (100 - settings["headroomPercent"]) // 100
        operating_envelope = min(highest_passing, reserved_envelope)
    grown_peak = math.ceil(request_rate["peakPerSecond"]
                           * (100 + request_rate["expectedGrowthPercent"]) / 100)
    headroom_target = math.ceil(grown_peak * (100 + settings["headroomPercent"]) / 100)
    required_rate = max(headroom_target, request_rate["burstPerSecond"])
    qualification_rate = next((item["rate"] for item in rate_summaries
                               if item["classification"] == "pass" and item["rate"] >= required_rate), 0)
    qualification_failures = 1
    if qualification_rate:
        qualification_failures = sum(1 for item in normalized
                                     if item["rate"] <= qualification_rate and not item["casePassed"])
    accepted = bool(first_saturation and not unstable_steps and qualification_rate
                    and not qualification_failures and operating_envelope >= required_rate)

    result = {
        "accepted": accepted,
        "firstReproducibleSaturationRate": first_saturation,
        "highestFullyPassingRate": highest_passing,
        "recommendedOperatingEnvelopeRate": operating_envelope,
        "reservedHeadroomPercent": settings["headroomPercent"],
        "forecastPeakRate": request_rate["peakPerSecond"],
        "forecastGrowthPercent": request_rate["expectedGrowthPercent"],
        "grownForecastPeakRate": grown_peak,
        "forecastPeakPlusHeadroomRate": headroom_target,
        "burstRate": request_rate["burstPerSecond"],
        "requiredQualificationRate": required_rate,
        "passingQualificationStepAtOrAboveHeadroomTarget": qualification_rate,
        "unstableStepCount": unstable_steps,
        "qualificationCaseFailureCount": qualification_failures,
        "deployments": deployments,
        "boundary": ("Reviewed private staging target, exact candidate digest, hash-pinned actions "
                     "and telemetry adapter; no production-capacity claim."),
    }
    return result, repeat_summaries, rate_summaries


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--staging-profile", required=True, type=Path)
    parser.add_argument("--capacity-profile", required=True, type=Path)
    parser.add_argument("--measurements", required=True, type=Path)
    parser.add_argument("--prior-deployment", required=True, type=Path)
    parser.add_argument("--candidate-deployment", required=True, type=Path)
    parser.add_argument("--rollback-deployment", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--repeat-summary-output", required=True, type=Path)
    parser.add_argument("--rate-summary-output", required=True, type=Path)
    args = parser.parse_args()
    try:
        staging_bytes = args.staging_profile.read_bytes()
        staging = json.loads(staging_bytes.decode("utf-8"))
        require(isinstance(staging, dict), "staging profile root must be an object")
        capacity = load_object(args.capacity_profile, "capacity profile")
        artifact = staging.get("artifact", {})
        repository = artifact.get("registryRepository")
        prior_identity = artifact.get("prior", {})
        candidate_identity = artifact.get("candidate", {})
        deployments = {
            "prior": validate_deployment(load_object(args.prior_deployment, "prior deployment"),
                                         "prior deployment", "prior",
                                         f"{repository}@{prior_identity.get('imageDigest')}",
                                         prior_identity.get("sourceRevision")),
            "candidate": validate_deployment(load_object(args.candidate_deployment, "candidate deployment"),
                                             "candidate deployment", "candidate",
                                             f"{repository}@{candidate_identity.get('imageDigest')}",
                                             candidate_identity.get("sourceRevision")),
            "rollback": validate_deployment(load_object(args.rollback_deployment, "rollback deployment"),
                                            "rollback deployment", "rollback",
                                            f"{repository}@{prior_identity.get('imageDigest')}",
                                            prior_identity.get("sourceRevision")),
        }
        result, repeat_summaries, rate_summaries = evaluate(
            staging, capacity, load_measurements(args.measurements), deployments, staging_bytes)
        result["measurementsSha256"] = hashlib.sha256(args.measurements.read_bytes()).hexdigest()
        args.output.write_text(json.dumps(result, sort_keys=True, indent=2) + "\n", encoding="utf-8")
        args.repeat_summary_output.write_text(
            json.dumps(repeat_summaries, sort_keys=True, indent=2) + "\n", encoding="utf-8")
        args.rate_summary_output.write_text(
            json.dumps(rate_summaries, sort_keys=True, indent=2) + "\n", encoding="utf-8")
        return 0
    except (OSError, UnicodeDecodeError, json.JSONDecodeError, KeyError, TypeError, ContractError) as exc:
        print(f"staging capacity result rejected: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
