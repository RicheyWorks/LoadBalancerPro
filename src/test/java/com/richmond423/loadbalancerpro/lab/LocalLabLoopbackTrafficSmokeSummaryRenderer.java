package com.richmond423.loadbalancerpro.lab;

import java.util.List;

final class LocalLabLoopbackTrafficSmokeSummaryRenderer {
    private static final List<Integer> COMMON_FIXED_PORTS = List.of(0, 80, 443, 8080, 8081, 9090);
    private static final String NO_PRODUCTION_PROOF_WARNING =
            "Deterministic loopback smoke observations are local test-scope context only; not production "
                    + "proof; not production certification; not live-cloud validation; not real-tenant "
                    + "validation.";
    private static final String NOT_PROVEN_BOUNDARY_SUMMARY =
            "Not proven: Docker/k6/Bruno/Toxiproxy implementation, replay execution, evidence/report "
                    + "generation, file writing, storage, export/download/upload/PDF/ZIP behavior, runtime "
                    + "behavior, production routing/proxy/scoring/API behavior, production endpoint behavior, "
                    + "live-cloud validation, real-tenant validation, production certification, or runtime "
                    + "enforcement.";
    private static final String NEXT_SAFE_STEP =
            "Use the in-memory smoke summary as reviewer context for a separately scoped local-lab step; "
                    + "keep future tool, replay, report, storage, export, and production validation work "
                    + "separately reviewed.";

    private LocalLabLoopbackTrafficSmokeSummaryRenderer() {
    }

    static LocalLabLoopbackTrafficSmokeSummary summarize(
            List<LocalLabLoopbackTrafficSmokeObservation> observations) {
        if (observations == null || observations.isEmpty()) {
            throw new IllegalArgumentException("observations are required");
        }

        boolean allLoopback = observations.stream()
                .allMatch(observation -> "127.0.0.1".equals(observation.host()));
        boolean allEphemeral = observations.stream()
                .allMatch(observation -> observation.assignedPort() > 0
                        && !COMMON_FIXED_PORTS.contains(observation.assignedPort()));

        return new LocalLabLoopbackTrafficSmokeSummary(
                "summary-local-lab-loopback-traffic-smoke",
                observations.size(),
                (int) observations.stream()
                        .filter(LocalLabLoopbackTrafficSmokeObservation::matchedExpectedFixture)
                        .filter(observation -> !observation.boundaryResponse())
                        .count(),
                (int) observations.stream()
                        .filter(LocalLabLoopbackTrafficSmokeObservation::boundaryResponse)
                        .count(),
                observations.stream()
                        .map(LocalLabLoopbackTrafficSmokeObservation::scenarioId)
                        .toList(),
                observations.stream()
                        .map(LocalLabLoopbackTrafficSmokeObservation::backendId)
                        .toList(),
                allLoopback
                        ? "All smoke observations targeted 127.0.0.1 loopback-only harness URLs."
                        : "Smoke observations include a non-loopback target and must be rejected.",
                allEphemeral
                        ? "All smoke observations used positive harness-assigned ephemeral ports and no common fixed ports."
                        : "Smoke observations include a non-ephemeral or common fixed port and must be rejected.",
                NO_PRODUCTION_PROOF_WARNING,
                NOT_PROVEN_BOUNDARY_SUMMARY,
                NEXT_SAFE_STEP);
    }
}
