package com.richmond423.loadbalancerpro.lab;

import java.util.List;
import java.util.Optional;

final class LocalLabPassiveTranscriptCatalog {
    private static final String NOT_PROVEN_BOUNDARY =
            "Test-scope passive transcript fixture data only; not production proof; not live-cloud validation; "
                    + "not real-tenant validation; no Docker, k6, Bruno, or Toxiproxy execution is required; "
                    + "no fake backend server, listener, port, loopback HTTP call, generated traffic, replay "
                    + "execution, storage, report, export, runtime behavior, routing, scoring, strategy, proxy, "
                    + "API, or evidence-generation behavior is added.";

    private static final List<LocalLabPassiveTranscriptScenario> TRANSCRIPTS = List.of(
            transcript(
                    "transcript-backend-healthy-fast",
                    LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId("backend-healthy-fast").orElseThrow(),
                    "GET label",
                    "/local-lab/passive/healthy-fast path label",
                    "Passive observation should describe a low-latency selected backend and explicitly keep "
                            + "the request/response exchange as fixture data only.",
                    "Safety note: observe-only local lab transcript label; no listener, traffic, replay, storage, "
                            + "or runtime decision is executed."),
            transcript(
                    "transcript-backend-slow-tail-latency",
                    LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId("backend-slow-tail-latency")
                            .orElseThrow(),
                    "GET label",
                    "/local-lab/passive/slow-tail-latency path label",
                    "Passive observation should connect p95/p99 tail-latency pressure to selected or rejected "
                            + "backend rationale without executing a request.",
                    "Safety note: observe-only local lab transcript label; no listener, traffic, replay, storage, "
                            + "or runtime decision is executed."),
            transcript(
                    "transcript-backend-partial-degradation",
                    LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId("backend-partial-degradation")
                            .orElseThrow(),
                    "GET label",
                    "/local-lab/passive/partial-degradation path label",
                    "Passive observation should describe degraded-but-not-down behavior, mixed health, and "
                            + "reviewer-readable routing evidence expectations.",
                    "Safety note: observe-only local lab transcript label; no listener, traffic, replay, storage, "
                            + "or runtime decision is executed."),
            transcript(
                    "transcript-backend-error-prone",
                    LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId("backend-error-prone").orElseThrow(),
                    "POST label",
                    "/local-lab/passive/error-prone path label",
                    "Passive observation should describe intermittent 500-style evidence and rejected-option "
                            + "rationale without starting a backend.",
                    "Safety note: observe-only local lab transcript label; no listener, traffic, replay, storage, "
                            + "or runtime decision is executed."),
            transcript(
                    "transcript-backend-overloaded-queue-pressure",
                    LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId("backend-overloaded-queue-pressure")
                            .orElseThrow(),
                    "GET label",
                    "/local-lab/passive/overloaded-queue-pressure path label",
                    "Passive observation should describe queue pressure, active load, rejected options, and "
                            + "what changed during overload as fixture labels.",
                    "Safety note: observe-only local lab transcript label; no listener, traffic, replay, storage, "
                            + "or runtime decision is executed."),
            transcript(
                    "transcript-backend-all-unhealthy-no-good-choice",
                    LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId(
                            "backend-all-unhealthy-no-good-choice").orElseThrow(),
                    "GET label",
                    "/local-lab/passive/all-unhealthy-no-good-choice path label",
                    "Passive observation should describe the no-good-choice boundary, rejected candidates, "
                            + "fallback posture, and safety-mode expectation.",
                    "Safety note: observe-only local lab transcript label; no listener, traffic, replay, storage, "
                            + "or runtime decision is executed."),
            transcript(
                    "transcript-backend-recovery",
                    LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId("backend-recovery").orElseThrow(),
                    "GET label",
                    "/local-lab/passive/recovery path label",
                    "Passive observation should describe recovery markers, improving latency, reduced errors, "
                            + "and selected or rejected backend changes.",
                    "Safety note: observe-only local lab transcript label; no listener, traffic, replay, storage, "
                            + "or runtime decision is executed."));

    private LocalLabPassiveTranscriptCatalog() {
    }

    static List<LocalLabPassiveTranscriptScenario> transcripts() {
        return TRANSCRIPTS;
    }

    static Optional<LocalLabPassiveTranscriptScenario> findByScenarioId(String scenarioId) {
        return TRANSCRIPTS.stream()
                .filter(transcript -> transcript.scenarioId().equals(scenarioId))
                .findFirst();
    }

    private static LocalLabPassiveTranscriptScenario transcript(
            String transcriptId,
            LocalLabFakeBackendResponseFixture fixture,
            String simulatedRequestMethodLabel,
            String simulatedRequestPathLabel,
            String routingEvidenceObservationNote,
            String safetyBoundaryNote) {
        LocalLabPassiveTranscriptEntry entry = new LocalLabPassiveTranscriptEntry(
                transcriptId,
                fixture.scenarioId(),
                fixture.fixtureId(),
                1,
                simulatedRequestMethodLabel,
                simulatedRequestPathLabel,
                fixture.backendId(),
                fixture.responseStatusCode(),
                fixture.responseLatencyLabel(),
                fixture.responseBodySummary(),
                fixture.simulatedErrorLabel(),
                fixture.simulatedLoadLabel(),
                routingEvidenceObservationNote,
                safetyBoundaryNote,
                NOT_PROVEN_BOUNDARY);
        return new LocalLabPassiveTranscriptScenario(
                transcriptId,
                fixture.scenarioId(),
                fixture.fixtureId(),
                fixture.behaviorProfile(),
                List.of(entry),
                NOT_PROVEN_BOUNDARY);
    }
}
