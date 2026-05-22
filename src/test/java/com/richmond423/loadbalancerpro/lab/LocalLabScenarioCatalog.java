package com.richmond423.loadbalancerpro.lab;

import java.util.List;
import java.util.Optional;

final class LocalLabScenarioCatalog {
    private static final String NOT_PROVEN_BOUNDARY =
            "Test-scope scenario metadata only; not production proof; not live-cloud validation; "
                    + "not real-tenant validation; no Docker, k6, Bruno, or Toxiproxy implementation is required; "
                    + "no fake backend server, port listener, generated traffic, routing, scoring, "
                    + "strategy, proxy, API, replay, storage, report, export, or evidence-generation behavior is added.";

    private static final List<LocalLabFakeBackendNodeScenario> SCENARIOS = List.of(
            scenario(
                    "backend-healthy-fast",
                    "Backend 1 Healthy Fast",
                    LocalLabFakeBackendBehaviorProfile.HEALTHY_FAST,
                    "Selected baseline candidate, rejected alternatives, low latency, low error, low load, "
                            + "policy gate status, safety mode, and not-proven boundary should be explainable."),
            scenario(
                    "backend-slow-tail-latency",
                    "Backend 2 Slow Tail Latency",
                    LocalLabFakeBackendBehaviorProfile.SLOW_TAIL_LATENCY,
                    "Tail latency observations, selected or rejected candidate rationale, p95/p99 warning label, "
                            + "policy gate status, safety mode, and local simulation boundary should be explainable."),
            scenario(
                    "backend-partial-degradation",
                    "Backend 3 Partial Degradation",
                    LocalLabFakeBackendBehaviorProfile.PARTIAL_DEGRADATION,
                    "Mixed health observations, degraded-but-not-down posture, selected or rejected candidate "
                            + "rationale, policy gate status, safety mode, and not-proven boundary should be explainable."),
            scenario(
                    "backend-error-prone",
                    "Backend 4 Error Prone",
                    LocalLabFakeBackendBehaviorProfile.ERROR_PRONE,
                    "Error burst observations, selected or rejected candidate rationale, error-rate warning, "
                            + "policy gate status, safety mode, and local simulation boundary should be explainable."),
            scenario(
                    "backend-overloaded-queue-pressure",
                    "Backend 5 Overloaded Queue Pressure",
                    LocalLabFakeBackendBehaviorProfile.OVERLOADED_QUEUE_PRESSURE,
                    "Queue pressure, active load, rejected options, what changed during overload, policy gate status, "
                            + "safety mode, and not-proven boundary should be explainable."),
            scenario(
                    "backend-all-unhealthy-no-good-choice",
                    "Backend Group All Unhealthy Or No Good Choice",
                    LocalLabFakeBackendBehaviorProfile.ALL_UNHEALTHY_OR_NO_GOOD_CHOICE,
                    "No-good-choice posture, selected fallback if any, rejected options, policy gate status, "
                            + "safety mode, and not-proven boundary should be explainable."),
            scenario(
                    "backend-recovery",
                    "Backend Recovery Transition",
                    LocalLabFakeBackendBehaviorProfile.RECOVERY,
                    "Recovery marker, error-rate improvement, latency improvement, queue reduction, selected or "
                            + "rejected candidate changes, policy gate status, safety mode, and not-proven boundary "
                            + "should be explainable."));

    private LocalLabScenarioCatalog() {
    }

    static List<LocalLabFakeBackendNodeScenario> scenarios() {
        return SCENARIOS;
    }

    static Optional<LocalLabFakeBackendNodeScenario> findByBackendId(String backendId) {
        return SCENARIOS.stream()
                .filter(scenario -> scenario.backendId().equals(backendId))
                .findFirst();
    }

    private static LocalLabFakeBackendNodeScenario scenario(
            String backendId,
            String backendName,
            LocalLabFakeBackendBehaviorProfile behaviorProfile,
            String evidenceExpectationSummary) {
        return new LocalLabFakeBackendNodeScenario(
                backendId,
                backendName,
                behaviorProfile,
                evidenceExpectationSummary,
                NOT_PROVEN_BOUNDARY);
    }
}
