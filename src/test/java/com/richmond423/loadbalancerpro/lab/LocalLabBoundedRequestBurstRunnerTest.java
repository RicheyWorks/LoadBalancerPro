package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class LocalLabBoundedRequestBurstRunnerTest {
    private static final List<Integer> COMMON_FIXED_PORTS = List.of(0, 80, 443, 8080, 8081, 9090);
    private static final int EXPECTED_REPETITIONS =
            LocalLabBoundedRequestBurstCatalog.FIXED_REPETITIONS_PER_CASE;

    @Test
    void burstCatalogIncludesEveryRequiredScenarioProfilePlusUnknownBoundary() {
        List<LocalLabBoundedRequestBurstCase> cases = LocalLabBoundedRequestBurstCatalog.cases();
        List<String> scenarioIds = new ArrayList<>(LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                .map(LocalLabFakeBackendResponseFixture::scenarioId)
                .toList());
        scenarioIds.add(LocalLabBoundedRequestBurstCatalog.UNKNOWN_BURST_SCENARIO_ID);
        List<String> caseIds = cases.stream()
                .map(LocalLabBoundedRequestBurstCase::burstCaseId)
                .toList();

        assertEquals(LocalLabFakeBackendResponseFixtureCatalog.fixtures().size() + 1, cases.size());
        assertEquals(scenarioIds, cases.stream()
                .map(LocalLabBoundedRequestBurstCase::scenarioId)
                .toList());
        assertEquals(caseIds.size(), Set.copyOf(caseIds).size());
        assertEquals(List.of(
                "burst-case-backend-healthy-fast",
                "burst-case-backend-slow-tail-latency",
                "burst-case-backend-partial-degradation",
                "burst-case-backend-error-prone",
                "burst-case-backend-overloaded-queue-pressure",
                "burst-case-backend-all-unhealthy-no-good-choice",
                "burst-case-backend-recovery",
                "burst-case-unknown-label-boundary"), caseIds);
        assertTrue(cases.get(cases.size() - 1).boundaryCase());
        assertTrue(cases.stream().allMatch(burstCase ->
                burstCase.fixedRepetitionCount() == EXPECTED_REPETITIONS));
        assertTrue(EXPECTED_REPETITIONS > 0 && EXPECTED_REPETITIONS <= 3);
        assertThrows(UnsupportedOperationException.class, () -> cases.add(cases.get(0)));
        assertEquals(cases, LocalLabBoundedRequestBurstCatalog.cases());
    }

    @Test
    void burstRunnerCoversEveryCaseWithExpectedTotalRequestCountAndLoopbackEphemeralPorts()
            throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            List<LocalLabBoundedRequestBurstCase> cases = LocalLabBoundedRequestBurstCatalog.cases();
            List<LocalLabBoundedRequestBurstObservation> observations =
                    LocalLabBoundedRequestBurstRunner.run(harness);
            Set<Integer> harnessPorts = harness.descriptors().stream()
                    .map(LocalLabMultiBackendLoopbackHarness.Descriptor::assignedPort)
                    .collect(Collectors.toSet());

            assertEquals(cases.size() * EXPECTED_REPETITIONS, observations.size());
            assertEquals(expandedCaseIds(cases), observations.stream()
                    .map(LocalLabBoundedRequestBurstObservation::burstCaseId)
                    .toList());
            assertEquals(expandedRepetitionIndexes(cases), observations.stream()
                    .map(LocalLabBoundedRequestBurstObservation::repetitionIndex)
                    .toList());

            for (LocalLabBoundedRequestBurstObservation observation : observations) {
                assertEquals("127.0.0.1", observation.loopbackHost());
                assertTrue(observation.assignedPort() > 0);
                assertFalse(COMMON_FIXED_PORTS.contains(observation.assignedPort()));
                assertTrue(harnessPorts.contains(observation.assignedPort()));
                assertTrue(normalize(observation.localOnlyConfirmation()).contains(
                        "test-scope deterministic bounded request burst smoke only"));
            }
        }
    }

    @Test
    void runnerRejectsNonLoopbackCommonFixedPortAndNonCatalogRepetitionTargets() {
        LocalLabBoundedRequestBurstCase burstCase = LocalLabBoundedRequestBurstCatalog.cases().get(0);

        assertThrows(IllegalArgumentException.class, () -> LocalLabBoundedRequestBurstRunner.requestsFor(
                List.of(descriptor("localhost", 49152, "http://localhost:49152/local-lab/fake-backend")),
                List.of(burstCase)));
        assertThrows(IllegalArgumentException.class, () -> LocalLabBoundedRequestBurstRunner.requestsFor(
                List.of(descriptor("0.0.0.0", 49152, "http://0.0.0.0:49152/local-lab/fake-backend")),
                List.of(burstCase)));
        assertThrows(IllegalArgumentException.class, () -> LocalLabBoundedRequestBurstRunner.requestsFor(
                List.of(descriptor("127.0.0.1", 8080, "http://127.0.0.1:8080/local-lab/fake-backend")),
                List.of(burstCase)));
        assertThrows(IllegalArgumentException.class, () -> LocalLabBoundedRequestBurstRunner.requestsFor(
                List.of(descriptor("127.0.0.1", 49152, "http://127.0.0.1:49153/local-lab/fake-backend")),
                List.of(burstCase)));

        LocalLabBoundedRequestBurstCase nonCatalogCount = new LocalLabBoundedRequestBurstCase(
                burstCase.burstCaseId(),
                burstCase.scenarioId(),
                burstCase.backendId(),
                burstCase.requestLabel(),
                1,
                burstCase.expectedStatusCode(),
                burstCase.expectedLatencyLabel(),
                burstCase.expectedErrorLoadLabel(),
                burstCase.expectedResponseLabel(),
                burstCase.expectedEvidenceLabel(),
                burstCase.expectedSafetyBoundaryLabel(),
                burstCase.expectedNotProvenBoundaryLabel(),
                burstCase.notProductionProofLabel(),
                burstCase.boundaryCase());
        assertThrows(IllegalArgumentException.class, () -> LocalLabBoundedRequestBurstRunner.requestsFor(
                List.of(descriptor("127.0.0.1", 49152, "http://127.0.0.1:49152/local-lab/fake-backend")),
                List.of(nonCatalogCount)));
    }

    @Test
    void everyBurstResponseMatchesExpectedFixtureOrStableBoundaryExpectation() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            List<LocalLabBoundedRequestBurstCase> cases = LocalLabBoundedRequestBurstCatalog.cases();
            Map<String, LocalLabBoundedRequestBurstCase> casesById = cases.stream()
                    .collect(Collectors.toMap(
                            LocalLabBoundedRequestBurstCase::burstCaseId,
                            burstCase -> burstCase));
            List<LocalLabBoundedRequestBurstObservation> observations =
                    LocalLabBoundedRequestBurstRunner.run(harness);

            for (LocalLabBoundedRequestBurstObservation observation : observations) {
                LocalLabBoundedRequestBurstCase burstCase = casesById.get(observation.burstCaseId());

                assertEquals(burstCase.expectedStatusCode(), observation.responseStatusCode());
                assertTrue(observation.evidenceLabelObserved(), burstCase.burstCaseId());
                assertTrue(observation.safetyBoundaryObserved(), burstCase.burstCaseId());
                assertTrue(observation.notProvenBoundaryObserved(), burstCase.burstCaseId());

                if (burstCase.boundaryCase()) {
                    assertTrue(observation.boundaryResponse());
                    assertFalse(observation.fixtureMatched());
                    assertEquals(404, observation.responseStatusCode());
                } else {
                    assertFalse(observation.boundaryResponse());
                    assertTrue(observation.fixtureMatched(), burstCase.burstCaseId());
                    LocalLabFakeBackendResponseFixture fixture =
                            LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId(burstCase.scenarioId())
                                    .orElseThrow();
                    assertEquals(fixture.responseStatusCode(), observation.responseStatusCode());
                }
            }
        }
    }

    @Test
    void repeatedBurstRunsAreDeterministicAndHarnessStopStateRemainsClean() throws Exception {
        LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start();
        List<LocalLabBoundedRequestBurstObservation> first = LocalLabBoundedRequestBurstRunner.run(harness);
        List<LocalLabBoundedRequestBurstObservation> second = LocalLabBoundedRequestBurstRunner.run(harness);

        assertEquals(first, second);
        assertEquals(first.stream().map(LocalLabBoundedRequestBurstObservation::deterministicText).toList(),
                second.stream().map(LocalLabBoundedRequestBurstObservation::deterministicText).toList());
        assertFalse(harness.stopped());

        harness.close();

        assertTrue(harness.stopped());
    }

    @Test
    void burstRunnerBoundaryAvoidsToolLoadBenchmarkReplayStorageRuntimeAndValidationOverclaims()
            throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            List<LocalLabBoundedRequestBurstObservation> observations =
                    LocalLabBoundedRequestBurstRunner.run(harness);

            for (LocalLabBoundedRequestBurstObservation observation : observations) {
                String normalized = normalize(observation.localOnlyConfirmation());

                assertTrue(normalized.contains("fixed small request counts"));
                assertTrue(normalized.contains("not k6"));
                assertTrue(normalized.contains("not bruno"));
                assertTrue(normalized.contains("not docker"));
                assertTrue(normalized.contains("not toxiproxy"));
                assertTrue(normalized.contains("not load testing"));
                assertTrue(normalized.contains("not stress testing"));
                assertTrue(normalized.contains("not performance benchmarking"));
                assertTrue(normalized.contains("not throughput evidence"));
                assertTrue(normalized.contains("not p95/p99 evidence"));
                assertTrue(normalized.contains("not production traffic"));
                assertTrue(normalized.contains("not production proof"));
                assertTrue(normalized.contains("no fixed ports"));
                assertTrue(normalized.contains("no non-loopback target calls"));
                assertTrue(normalized.contains("no unbounded loops"));
                assertTrue(normalized.contains("no time-based loops"));
                assertTrue(normalized.contains("no sleeps or timers"));
                assertTrue(normalized.contains("no replay execution"));
                assertTrue(normalized.contains("evidence/report generation"));
                assertTrue(normalized.contains("file writing"));
                assertTrue(normalized.contains("environment reads"));
                assertTrue(normalized.contains("process execution"));
                assertTrue(normalized.contains("storage"));
                assertTrue(normalized.contains("export"));

                for (String forbidden : forbiddenOverclaims()) {
                    assertFalse(normalized.contains(forbidden), "burst runner must not overclaim " + forbidden);
                }
            }
        }
    }

    private static List<String> expandedCaseIds(List<LocalLabBoundedRequestBurstCase> cases) {
        List<String> expanded = new ArrayList<>();
        for (LocalLabBoundedRequestBurstCase burstCase : cases) {
            for (int repetition = 0; repetition < burstCase.fixedRepetitionCount(); repetition++) {
                expanded.add(burstCase.burstCaseId());
            }
        }
        return expanded;
    }

    private static List<Integer> expandedRepetitionIndexes(List<LocalLabBoundedRequestBurstCase> cases) {
        List<Integer> expanded = new ArrayList<>();
        for (LocalLabBoundedRequestBurstCase burstCase : cases) {
            for (int repetition = 1; repetition <= burstCase.fixedRepetitionCount(); repetition++) {
                expanded.add(repetition);
            }
        }
        return expanded;
    }

    private static LocalLabMultiBackendLoopbackHarness.Descriptor descriptor(
            String host,
            int port,
            String localUrl) {
        LocalLabFakeBackendResponseFixture fixture = LocalLabFakeBackendResponseFixtureCatalog.fixtures().get(0);
        return new LocalLabMultiBackendLoopbackHarness.Descriptor(
                fixture.scenarioId(),
                fixture.backendId(),
                host,
                port,
                fixture.behaviorType(),
                fixture.behaviorProfile().behaviorLabel(),
                localUrl,
                LocalLabMultiBackendLoopbackHarness.MULTI_BACKEND_BOUNDARY);
    }

    private static List<String> forbiddenOverclaims() {
        return List.of(
                "production-ready",
                "production certified",
                "production validation is complete",
                "live-cloud validated",
                "live-cloud validation is complete",
                "real-tenant validated",
                "real-tenant validation is complete",
                "runtime enforcement is implemented",
                "load testing is complete",
                "stress testing is complete",
                "benchmarking is complete",
                "throughput benchmark",
                "p95/p99 benchmark",
                "docker compose is implemented",
                "k6 scenario is implemented",
                "bruno collection is implemented",
                "toxiproxy config is implemented",
                "prometheus/grafana dashboard is implemented",
                "replay execution is implemented",
                "evidence report is generated",
                "report generation is implemented",
                "storage is implemented",
                "export behavior is implemented",
                "runtime behavior is changed",
                "routing behavior is changed",
                "production api behavior is changed",
                "production endpoint is added");
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
