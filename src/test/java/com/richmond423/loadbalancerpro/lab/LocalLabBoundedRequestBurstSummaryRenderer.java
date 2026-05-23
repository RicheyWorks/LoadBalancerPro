package com.richmond423.loadbalancerpro.lab;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class LocalLabBoundedRequestBurstSummaryRenderer {
    private static final List<Integer> COMMON_FIXED_PORTS = List.of(0, 80, 443, 8080, 8081, 9090);
    private static final String NO_PRODUCTION_PROOF_WARNING =
            "Bounded request burst smoke observations are local test-scope context only; not production "
                    + "traffic; not production proof; not production certification; not live-cloud "
                    + "validation; not real-tenant validation; not runtime enforcement.";
    private static final String NO_LOAD_TEST_WARNING =
            "This bounded burst uses fixed small request counts only; not load testing; not stress testing; "
                    + "not performance benchmarking; not throughput evidence; not latency measurement; "
                    + "not p95/p99 evidence.";
    private static final String FUTURE_TOOLING_BOUNDARY_SUMMARY =
            "No Docker/k6/Bruno/Toxiproxy implementation; no replay execution; no evidence/report "
                    + "generation; no file writing; no environment reads; no process execution; no fixed "
                    + "ports; no non-loopback target calls or binds; no storage/export/download/upload/PDF/ZIP "
                    + "behavior; no runtime behavior; no production routing/proxy/scoring/API behavior.";
    private static final String NEXT_SAFE_STEP =
            "Use this in-memory bounded burst summary as reviewer context before separately scoped "
                    + "fault-fixture expansion or docs-only k6/Bruno/Toxiproxy planning; keep load testing, "
                    + "benchmarks, replay, reports, storage, export, and production validation separately reviewed.";

    private LocalLabBoundedRequestBurstSummaryRenderer() {
    }

    static LocalLabBoundedRequestBurstSummary summarize(
            List<LocalLabBoundedRequestBurstObservation> observations) {
        if (observations == null || observations.isEmpty()) {
            throw new IllegalArgumentException("observations are required");
        }

        Set<String> burstCases = new LinkedHashSet<>();
        Set<String> scenarios = new LinkedHashSet<>();
        Set<String> backends = new LinkedHashSet<>();
        Set<Integer> repetitionIndexes = new LinkedHashSet<>();
        for (LocalLabBoundedRequestBurstObservation observation : observations) {
            burstCases.add(observation.burstCaseId());
            scenarios.add(observation.scenarioId());
            backends.add(observation.backendId());
            repetitionIndexes.add(observation.repetitionIndex());
        }

        boolean allLoopback = observations.stream()
                .allMatch(observation -> "127.0.0.1".equals(observation.loopbackHost()));
        boolean allEphemeral = observations.stream()
                .allMatch(observation -> observation.assignedPort() > 0
                        && !COMMON_FIXED_PORTS.contains(observation.assignedPort()));

        return new LocalLabBoundedRequestBurstSummary(
                "summary-local-lab-bounded-request-burst-smoke",
                burstCases.size(),
                observations.size(),
                repetitionIndexes.size(),
                (int) observations.stream()
                        .filter(LocalLabBoundedRequestBurstObservation::fixtureMatched)
                        .filter(observation -> !observation.boundaryResponse())
                        .count(),
                (int) observations.stream()
                        .filter(LocalLabBoundedRequestBurstObservation::boundaryResponse)
                        .count(),
                (int) observations.stream()
                        .filter(observation -> !observation.boundaryResponse())
                        .map(LocalLabBoundedRequestBurstObservation::scenarioId)
                        .distinct()
                        .count(),
                (int) observations.stream()
                        .filter(observation -> !observation.boundaryResponse())
                        .map(LocalLabBoundedRequestBurstObservation::backendId)
                        .distinct()
                        .count(),
                List.copyOf(scenarios),
                List.copyOf(backends),
                allLoopback
                        ? "All bounded burst observations targeted 127.0.0.1 loopback-only harness URLs."
                        : "Bounded burst observations include a non-loopback target and must be rejected.",
                allEphemeral
                        ? "All bounded burst observations used positive harness-assigned ephemeral ports and no common fixed ports."
                        : "Bounded burst observations include a non-ephemeral or common fixed port and must be rejected.",
                "Bounded burst output is deterministic in memory across repeated calls.",
                NO_PRODUCTION_PROOF_WARNING,
                NO_LOAD_TEST_WARNING,
                FUTURE_TOOLING_BOUNDARY_SUMMARY,
                NEXT_SAFE_STEP);
    }
}
