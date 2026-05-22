package com.richmond423.loadbalancerpro.lab;

import java.util.List;
import java.util.Optional;

final class LocalLabFakeBackendResponseFixtureCatalog {
    private static final String NOT_PROVEN_BOUNDARY =
            "Test-scope response fixture data only; not production proof; not live-cloud validation; "
                    + "not real-tenant validation; no Docker, k6, Bruno, or Toxiproxy execution is required; "
                    + "no fake backend server, listener, port, loopback HTTP call, generated traffic, runtime "
                    + "behavior, routing, scoring, strategy, proxy, API, replay, storage, report, export, or "
                    + "evidence-generation behavior is added.";

    private static final List<LocalLabFakeBackendResponseFixture> FIXTURES = List.of(
            fixture(
                    "fixture-backend-healthy-fast",
                    LocalLabScenarioCatalog.findByBackendId("backend-healthy-fast").orElseThrow(),
                    200,
                    "low-latency successful response label",
                    "Successful baseline response body for a healthy fast backend.",
                    "no simulated application error",
                    "low load and no queue pressure",
                    "Evidence should note the selected backend, low latency, low error posture, low load, "
                            + "and the local simulation boundary."),
            fixture(
                    "fixture-backend-slow-tail-latency",
                    LocalLabScenarioCatalog.findByBackendId("backend-slow-tail-latency").orElseThrow(),
                    200,
                    "successful response with p95/p99 tail-latency pressure label",
                    "Successful but slow response body for a tail-heavy backend.",
                    "no immediate application error; timeout risk remains a label",
                    "normal load with latency pressure",
                    "Evidence should note tail latency, selected or rejected backend rationale, p95/p99 warning, "
                            + "and the local simulation boundary."),
            fixture(
                    "fixture-backend-partial-degradation",
                    LocalLabScenarioCatalog.findByBackendId("backend-partial-degradation").orElseThrow(),
                    200,
                    "mixed-latency degraded response label",
                    "Degraded but still responding body for a partially degraded backend.",
                    "intermittent soft failure label without total outage",
                    "moderate load with unstable capacity",
                    "Evidence should note degraded-but-not-down behavior, mixed health, selected or rejected "
                            + "backend rationale, and the local simulation boundary."),
            fixture(
                    "fixture-backend-error-prone",
                    LocalLabScenarioCatalog.findByBackendId("backend-error-prone").orElseThrow(),
                    500,
                    "normal-to-mixed latency with intermittent 500-style error label",
                    "Intermittent 500-style response body for an error-prone backend.",
                    "intermittent 500-style error",
                    "normal load while error rate increases",
                    "Evidence should note error burst behavior, selected or rejected backend rationale, "
                            + "error-rate warning, and the local simulation boundary."),
            fixture(
                    "fixture-backend-overloaded-queue-pressure",
                    LocalLabScenarioCatalog.findByBackendId("backend-overloaded-queue-pressure").orElseThrow(),
                    503,
                    "high-latency queue-pressure response label",
                    "Overload response body for a backend under queue pressure.",
                    "timeout or rejected-work risk label",
                    "high load and elevated queue depth",
                    "Evidence should note backend queue pressure, active load, rejected options, what changed "
                            + "during overload, and the local simulation boundary."),
            fixture(
                    "fixture-backend-all-unhealthy-no-good-choice",
                    LocalLabScenarioCatalog.findByBackendId("backend-all-unhealthy-no-good-choice").orElseThrow(),
                    503,
                    "unavailable or unacceptable-latency failure-boundary label",
                    "No-good-choice response body for an all-unhealthy candidate set.",
                    "all candidates failing or too degraded label",
                    "unsafe load or queue pressure across candidates",
                    "Evidence should note backend no-good-choice posture, selected fallback if any, rejected "
                            + "options, safety mode, and the local simulation boundary."),
            fixture(
                    "fixture-backend-recovery",
                    LocalLabScenarioCatalog.findByBackendId("backend-recovery").orElseThrow(),
                    200,
                    "improving-latency recovery response label",
                    "Recovery response body for a backend returning toward healthy behavior.",
                    "error rate trending down label",
                    "load and queue pressure returning toward baseline",
                    "Evidence should note backend recovery marker, error-rate improvement, latency improvement, "
                            + "queue reduction, selected or rejected backend changes, and the local simulation "
                            + "boundary."));

    private LocalLabFakeBackendResponseFixtureCatalog() {
    }

    static List<LocalLabFakeBackendResponseFixture> fixtures() {
        return FIXTURES;
    }

    static Optional<LocalLabFakeBackendResponseFixture> findByScenarioId(String scenarioId) {
        return FIXTURES.stream()
                .filter(fixture -> fixture.scenarioId().equals(scenarioId))
                .findFirst();
    }

    private static LocalLabFakeBackendResponseFixture fixture(
            String fixtureId,
            LocalLabFakeBackendNodeScenario scenario,
            int responseStatusCode,
            String responseLatencyLabel,
            String responseBodySummary,
            String simulatedErrorLabel,
            String simulatedLoadLabel,
            String evidenceNote) {
        return new LocalLabFakeBackendResponseFixture(
                fixtureId,
                scenario.backendId(),
                scenario.backendId(),
                scenario.behaviorProfile(),
                responseStatusCode,
                responseLatencyLabel,
                responseBodySummary,
                simulatedErrorLabel,
                simulatedLoadLabel,
                evidenceNote,
                NOT_PROVEN_BOUNDARY);
    }
}
