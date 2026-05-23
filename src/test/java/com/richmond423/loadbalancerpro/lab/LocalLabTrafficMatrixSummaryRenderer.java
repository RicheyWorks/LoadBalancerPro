package com.richmond423.loadbalancerpro.lab;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class LocalLabTrafficMatrixSummaryRenderer {
    private static final List<Integer> COMMON_FIXED_PORTS = List.of(0, 80, 443, 8080, 8081, 9090);
    private static final String NO_PRODUCTION_PROOF_WARNING =
            "Deterministic traffic matrix observations are local test-scope context only; not load testing; "
                    + "not stress testing; not production traffic; not production proof; not production "
                    + "certification; not live-cloud validation; not real-tenant validation.";
    private static final String FUTURE_TOOLING_BOUNDARY_SUMMARY =
            "No Docker/k6/Bruno/Toxiproxy implementation; no replay execution; no evidence/report "
                    + "generation; no file writing; no environment reads; no process execution; no fixed "
                    + "ports; no non-loopback target calls or binds; no storage/export/download/upload/PDF/ZIP "
                    + "behavior; no runtime behavior; no production routing/proxy/scoring/API behavior.";
    private static final String NEXT_SAFE_STEP =
            "Use this in-memory matrix summary as reviewer context before separately scoped k6/Bruno "
                    + "planning docs; keep future tooling, replay, reports, storage, export, and production "
                    + "validation separately reviewed.";

    private LocalLabTrafficMatrixSummaryRenderer() {
    }

    static LocalLabTrafficMatrixSummary summarize(List<LocalLabTrafficMatrixObservation> observations) {
        if (observations == null || observations.isEmpty()) {
            throw new IllegalArgumentException("observations are required");
        }

        Set<String> scenarios = new LinkedHashSet<>();
        Set<String> backends = new LinkedHashSet<>();
        for (LocalLabTrafficMatrixObservation observation : observations) {
            scenarios.add(observation.scenarioId());
            backends.add(observation.backendId());
        }

        boolean allLoopback = observations.stream()
                .allMatch(observation -> "127.0.0.1".equals(observation.loopbackHost()));
        boolean allEphemeral = observations.stream()
                .allMatch(observation -> observation.assignedPort() > 0
                        && !COMMON_FIXED_PORTS.contains(observation.assignedPort()));

        return new LocalLabTrafficMatrixSummary(
                "summary-local-lab-deterministic-traffic-matrix",
                observations.size(),
                (int) observations.stream()
                        .filter(LocalLabTrafficMatrixObservation::fixtureMatched)
                        .filter(observation -> !observation.boundaryResponse())
                        .count(),
                (int) observations.stream()
                        .filter(LocalLabTrafficMatrixObservation::boundaryResponse)
                        .count(),
                (int) observations.stream()
                        .filter(observation -> !observation.boundaryResponse())
                        .map(LocalLabTrafficMatrixObservation::scenarioId)
                        .distinct()
                        .count(),
                (int) observations.stream()
                        .filter(observation -> !observation.boundaryResponse())
                        .map(LocalLabTrafficMatrixObservation::backendId)
                        .distinct()
                        .count(),
                List.copyOf(scenarios),
                List.copyOf(backends),
                allLoopback
                        ? "All traffic matrix observations targeted 127.0.0.1 loopback-only harness URLs."
                        : "Traffic matrix observations include a non-loopback target and must be rejected.",
                allEphemeral
                        ? "All traffic matrix observations used positive harness-assigned ephemeral ports and no common fixed ports."
                        : "Traffic matrix observations include a non-ephemeral or common fixed port and must be rejected.",
                "Traffic matrix output is deterministic in memory across repeated calls.",
                NO_PRODUCTION_PROOF_WARNING,
                FUTURE_TOOLING_BOUNDARY_SUMMARY,
                NEXT_SAFE_STEP);
    }
}
