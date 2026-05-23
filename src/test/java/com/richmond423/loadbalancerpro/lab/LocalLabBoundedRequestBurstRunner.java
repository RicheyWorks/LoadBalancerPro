package com.richmond423.loadbalancerpro.lab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class LocalLabBoundedRequestBurstRunner {
    static final String BOUNDED_BURST_BOUNDARY =
            "Test-scope deterministic bounded request burst smoke only; fixed small request counts; "
                    + "uses the existing src/test/java multi-backend loopback harness and smoke client; "
                    + "calls only 127.0.0.1 harness URLs with harness-assigned ephemeral ports; not k6; "
                    + "not Bruno; not Docker; not Toxiproxy; not load testing; not stress testing; "
                    + "not performance benchmarking; not throughput evidence; not p95/p99 evidence; "
                    + "not production traffic; not production proof; not live-cloud validation; not "
                    + "real-tenant validation; not production certification; no fixed ports; no non-loopback "
                    + "target calls; no unbounded loops; no time-based loops; no sleeps or timers; no replay "
                    + "execution, evidence/report generation, file writing, environment reads, process "
                    + "execution, storage, export, runtime, routing, scoring, strategy, proxy, or API behavior "
                    + "changes.";

    private LocalLabBoundedRequestBurstRunner() {
    }

    static List<LocalLabBoundedRequestBurstObservation> run(
            LocalLabMultiBackendLoopbackHarness harness) throws IOException {
        if (harness == null) {
            throw new IllegalArgumentException("harness is required");
        }
        return run(harness.descriptors(), LocalLabBoundedRequestBurstCatalog.cases());
    }

    static List<LocalLabBoundedRequestBurstObservation> run(
            List<LocalLabMultiBackendLoopbackHarness.Descriptor> descriptors,
            List<LocalLabBoundedRequestBurstCase> cases) throws IOException {
        List<ExpandedRequest> expandedRequests = expandedRequestsFor(descriptors, cases);
        List<LocalLabLoopbackTrafficSmokeObservation> smokeObservations =
                LocalLabLoopbackTrafficSmokeClient.smoke(expandedRequests.stream()
                        .map(ExpandedRequest::request)
                        .toList());
        List<LocalLabBoundedRequestBurstObservation> burstObservations = new ArrayList<>();

        for (int i = 0; i < expandedRequests.size(); i++) {
            ExpandedRequest expandedRequest = expandedRequests.get(i);
            LocalLabLoopbackTrafficSmokeObservation smokeObservation = smokeObservations.get(i);
            burstObservations.add(new LocalLabBoundedRequestBurstObservation(
                    expandedRequest.burstCase().burstCaseId(),
                    smokeObservation.scenarioId(),
                    smokeObservation.backendId(),
                    expandedRequest.repetitionIndex(),
                    smokeObservation.host(),
                    smokeObservation.assignedPort(),
                    smokeObservation.responseStatusCode(),
                    smokeObservation.matchedExpectedFixture(),
                    smokeObservation.evidenceLabelFound(),
                    smokeObservation.safetyBoundaryLabelFound(),
                    smokeObservation.notProvenBoundaryLabelFound(),
                    smokeObservation.boundaryResponse(),
                    BOUNDED_BURST_BOUNDARY));
        }
        return List.copyOf(burstObservations);
    }

    static List<LocalLabLoopbackTrafficSmokeRequest> requestsFor(
            List<LocalLabMultiBackendLoopbackHarness.Descriptor> descriptors,
            List<LocalLabBoundedRequestBurstCase> cases) {
        return expandedRequestsFor(descriptors, cases).stream()
                .map(ExpandedRequest::request)
                .toList();
    }

    private static List<ExpandedRequest> expandedRequestsFor(
            List<LocalLabMultiBackendLoopbackHarness.Descriptor> descriptors,
            List<LocalLabBoundedRequestBurstCase> cases) {
        if (cases == null || cases.isEmpty()) {
            throw new IllegalArgumentException("burst cases are required");
        }
        validateFixedSmallRepetitionCounts(cases);

        List<ExpandedRequest> expanded = new ArrayList<>();
        for (LocalLabBoundedRequestBurstCase burstCase : cases) {
            LocalLabLoopbackTrafficSmokeRequest baseRequest =
                    LocalLabTrafficMatrixRunner.requestsFor(descriptors, List.of(burstCase.toMatrixCase()))
                            .get(0);
            for (int repetition = 1; repetition <= burstCase.fixedRepetitionCount(); repetition++) {
                expanded.add(new ExpandedRequest(burstCase, repetition, baseRequest));
            }
        }
        return List.copyOf(expanded);
    }

    private static void validateFixedSmallRepetitionCounts(List<LocalLabBoundedRequestBurstCase> cases) {
        int expected = cases.get(0).fixedRepetitionCount();
        if (expected != LocalLabBoundedRequestBurstCatalog.FIXED_REPETITIONS_PER_CASE) {
            throw new IllegalArgumentException("burst runner requires the catalog fixed repetition count");
        }
        for (LocalLabBoundedRequestBurstCase burstCase : cases) {
            if (burstCase.fixedRepetitionCount() != expected) {
                throw new IllegalArgumentException("all burst cases must use one fixed small repetition count");
            }
        }
    }

    private record ExpandedRequest(
            LocalLabBoundedRequestBurstCase burstCase,
            int repetitionIndex,
            LocalLabLoopbackTrafficSmokeRequest request) {
    }
}
