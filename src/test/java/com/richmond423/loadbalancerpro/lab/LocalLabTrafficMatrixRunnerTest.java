package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class LocalLabTrafficMatrixRunnerTest {
    private static final List<Integer> COMMON_FIXED_PORTS = List.of(0, 80, 443, 8080, 8081, 9090);
    private static final List<String> REQUIRED_SCENARIOS = LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
            .map(LocalLabFakeBackendResponseFixture::scenarioId)
            .toList();

    @Test
    void matrixCatalogIncludesEveryRequiredScenarioProfilePlusUnknownBoundary() {
        List<LocalLabTrafficMatrixCase> cases = LocalLabTrafficMatrixCatalog.cases();
        List<String> scenarioIds = new ArrayList<>(REQUIRED_SCENARIOS);
        scenarioIds.add(LocalLabTrafficMatrixCatalog.UNKNOWN_MATRIX_SCENARIO_ID);
        List<String> caseIds = cases.stream()
                .map(LocalLabTrafficMatrixCase::matrixCaseId)
                .toList();

        assertEquals(LocalLabFakeBackendResponseFixtureCatalog.fixtures().size() + 1, cases.size());
        assertEquals(scenarioIds, cases.stream()
                .map(LocalLabTrafficMatrixCase::scenarioId)
                .toList());
        assertEquals(caseIds.size(), Set.copyOf(caseIds).size());
        assertEquals(List.of(
                "matrix-case-backend-healthy-fast",
                "matrix-case-backend-slow-tail-latency",
                "matrix-case-backend-partial-degradation",
                "matrix-case-backend-error-prone",
                "matrix-case-backend-overloaded-queue-pressure",
                "matrix-case-backend-all-unhealthy-no-good-choice",
                "matrix-case-backend-recovery",
                "matrix-case-unknown-label-boundary"), caseIds);
        assertTrue(cases.get(cases.size() - 1).boundaryCase());
        assertThrows(UnsupportedOperationException.class, () -> cases.add(cases.get(0)));
        assertEquals(cases, LocalLabTrafficMatrixCatalog.cases());
    }

    @Test
    void matrixRunnerCoversEveryCaseWithLoopbackOnlyHarnessAssignedEphemeralPorts() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            List<LocalLabTrafficMatrixObservation> observations = LocalLabTrafficMatrixRunner.run(harness);
            Set<Integer> harnessPorts = harness.descriptors().stream()
                    .map(LocalLabMultiBackendLoopbackHarness.Descriptor::assignedPort)
                    .collect(Collectors.toSet());

            assertEquals(LocalLabTrafficMatrixCatalog.cases().size(), observations.size());
            assertEquals(LocalLabTrafficMatrixCatalog.cases().stream()
                    .map(LocalLabTrafficMatrixCase::matrixCaseId)
                    .toList(), observations.stream()
                    .map(LocalLabTrafficMatrixObservation::matrixCaseId)
                    .toList());

            for (LocalLabTrafficMatrixObservation observation : observations) {
                assertEquals("127.0.0.1", observation.loopbackHost());
                assertTrue(observation.assignedPort() > 0);
                assertFalse(COMMON_FIXED_PORTS.contains(observation.assignedPort()));
                assertTrue(harnessPorts.contains(observation.assignedPort()));
                assertTrue(normalize(observation.localOnlyConfirmation()).contains(
                        "test-scope deterministic traffic matrix only"));
            }
        }
    }

    @Test
    void runnerRejectsNonLoopbackAndCommonFixedPortTargets() {
        LocalLabTrafficMatrixCase matrixCase = LocalLabTrafficMatrixCatalog.cases().get(0);

        assertThrows(IllegalArgumentException.class, () -> LocalLabTrafficMatrixRunner.requestsFor(
                List.of(descriptor("localhost", 49152, "http://localhost:49152/local-lab/fake-backend")),
                List.of(matrixCase)));
        assertThrows(IllegalArgumentException.class, () -> LocalLabTrafficMatrixRunner.requestsFor(
                List.of(descriptor("0.0.0.0", 49152, "http://0.0.0.0:49152/local-lab/fake-backend")),
                List.of(matrixCase)));
        assertThrows(IllegalArgumentException.class, () -> LocalLabTrafficMatrixRunner.requestsFor(
                List.of(descriptor("127.0.0.1", 8080, "http://127.0.0.1:8080/local-lab/fake-backend")),
                List.of(matrixCase)));
        assertThrows(IllegalArgumentException.class, () -> LocalLabTrafficMatrixRunner.requestsFor(
                List.of(descriptor("127.0.0.1", 49152, "http://127.0.0.1:49153/local-lab/fake-backend")),
                List.of(matrixCase)));
    }

    @Test
    void everyMatrixResponseMatchesFixtureOrStableBoundaryExpectation() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            List<LocalLabTrafficMatrixCase> cases = LocalLabTrafficMatrixCatalog.cases();
            List<LocalLabTrafficMatrixObservation> observations = LocalLabTrafficMatrixRunner.run(harness);

            for (int i = 0; i < cases.size(); i++) {
                LocalLabTrafficMatrixCase matrixCase = cases.get(i);
                LocalLabTrafficMatrixObservation observation = observations.get(i);

                assertEquals(matrixCase.expectedStatusCode(), observation.responseStatusCode());
                assertTrue(observation.evidenceLabelObserved(), matrixCase.matrixCaseId());
                assertTrue(observation.safetyBoundaryObserved(), matrixCase.matrixCaseId());
                assertTrue(observation.notProvenBoundaryObserved(), matrixCase.matrixCaseId());

                if (matrixCase.boundaryCase()) {
                    assertTrue(observation.boundaryResponse());
                    assertFalse(observation.fixtureMatched());
                    assertEquals(404, observation.responseStatusCode());
                } else {
                    assertFalse(observation.boundaryResponse());
                    assertTrue(observation.fixtureMatched(), matrixCase.matrixCaseId());
                    LocalLabFakeBackendResponseFixture fixture =
                            LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId(matrixCase.scenarioId())
                                    .orElseThrow();
                    assertEquals(fixture.responseStatusCode(), observation.responseStatusCode());
                }
            }
        }
    }

    @Test
    void repeatedMatrixRunsAreDeterministicAndHarnessStopStateRemainsClean() throws Exception {
        LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start();
        List<LocalLabTrafficMatrixObservation> first = LocalLabTrafficMatrixRunner.run(harness);
        List<LocalLabTrafficMatrixObservation> second = LocalLabTrafficMatrixRunner.run(harness);

        assertEquals(first, second);
        assertEquals(first.stream().map(LocalLabTrafficMatrixObservation::deterministicText).toList(),
                second.stream().map(LocalLabTrafficMatrixObservation::deterministicText).toList());
        assertFalse(harness.stopped());

        harness.close();

        assertTrue(harness.stopped());
    }

    @Test
    void matrixRunnerBoundaryAvoidsToolLoadReplayStorageRuntimeAndValidationOverclaims() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            List<LocalLabTrafficMatrixObservation> observations = LocalLabTrafficMatrixRunner.run(harness);

            for (LocalLabTrafficMatrixObservation observation : observations) {
                String normalized = normalize(observation.localOnlyConfirmation());

                assertTrue(normalized.contains("not k6"));
                assertTrue(normalized.contains("not bruno"));
                assertTrue(normalized.contains("not docker"));
                assertTrue(normalized.contains("not toxiproxy"));
                assertTrue(normalized.contains("not load testing"));
                assertTrue(normalized.contains("not stress testing"));
                assertTrue(normalized.contains("not production traffic"));
                assertTrue(normalized.contains("not production proof"));
                assertTrue(normalized.contains("no fixed ports"));
                assertTrue(normalized.contains("no non-loopback target calls"));
                assertTrue(normalized.contains("no replay execution"));
                assertTrue(normalized.contains("evidence/report generation"));
                assertTrue(normalized.contains("file writing"));
                assertTrue(normalized.contains("environment reads"));
                assertTrue(normalized.contains("process execution"));
                assertTrue(normalized.contains("storage"));
                assertTrue(normalized.contains("export"));

                for (String forbidden : forbiddenOverclaims()) {
                    assertFalse(normalized.contains(forbidden), "matrix runner must not overclaim " + forbidden);
                }
            }
        }
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
