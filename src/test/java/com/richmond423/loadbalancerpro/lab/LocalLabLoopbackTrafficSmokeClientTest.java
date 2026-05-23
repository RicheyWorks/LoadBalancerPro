package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LocalLabLoopbackTrafficSmokeClientTest {
    private static final List<Integer> COMMON_FIXED_PORTS = List.of(0, 80, 443, 8080, 8081, 9090);
    private static final List<String> REQUIRED_PROFILES = List.of(
            LocalLabFakeBackendBehaviorProfile.HEALTHY_FAST.name(),
            LocalLabFakeBackendBehaviorProfile.SLOW_TAIL_LATENCY.name(),
            LocalLabFakeBackendBehaviorProfile.PARTIAL_DEGRADATION.name(),
            LocalLabFakeBackendBehaviorProfile.ERROR_PRONE.name(),
            LocalLabFakeBackendBehaviorProfile.OVERLOADED_QUEUE_PRESSURE.name(),
            LocalLabFakeBackendBehaviorProfile.ALL_UNHEALTHY_OR_NO_GOOD_CHOICE.name(),
            LocalLabFakeBackendBehaviorProfile.RECOVERY.name());

    @Test
    void smokeClientCoversEveryRequiredScenarioProfileAndUnknownBoundary() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            List<LocalLabLoopbackTrafficSmokeObservation> observations =
                    LocalLabLoopbackTrafficSmokeClient.smoke(harness);

            assertEquals(LocalLabFakeBackendResponseFixtureCatalog.fixtures().size() + 1, observations.size());
            assertEquals(REQUIRED_PROFILES, observations.stream()
                    .filter(observation -> !observation.boundaryResponse())
                    .map(LocalLabLoopbackTrafficSmokeObservation::behaviorType)
                    .toList());
            assertEquals(LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                    .map(LocalLabFakeBackendResponseFixture::scenarioId)
                    .toList(), observations.stream()
                    .filter(observation -> !observation.boundaryResponse())
                    .map(LocalLabLoopbackTrafficSmokeObservation::scenarioId)
                    .toList());
            assertEquals("UNKNOWN_BOUNDARY", observations.get(observations.size() - 1).behaviorType());
            assertTrue(observations.get(observations.size() - 1).boundaryResponse());
        }
    }

    @Test
    void smokeClientOnlyCallsLoopbackHarnessTargetsAndRejectsNonLoopbackTargets() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            List<LocalLabLoopbackTrafficSmokeRequest> requests =
                    LocalLabLoopbackTrafficSmokeClient.requestsFor(harness.descriptors());

            for (LocalLabLoopbackTrafficSmokeRequest request : requests) {
                assertEquals("127.0.0.1", request.loopbackUri().getHost());
                assertTrue(request.loopbackUri().toString().startsWith("http://127.0.0.1:"));
                assertFalse(request.loopbackUri().toString().contains("0.0.0.0"));
                assertFalse(request.loopbackUri().toString().contains("localhost"));
                assertTrue(request.loopbackUri().getPort() > 0);
                assertFalse(COMMON_FIXED_PORTS.contains(request.loopbackUri().getPort()));
            }
        }

        assertThrows(IllegalArgumentException.class, () -> requestFor("http://localhost:49152/local-lab/fake-backend"));
        assertThrows(IllegalArgumentException.class, () -> requestFor("http://0.0.0.0:49152/local-lab/fake-backend"));
        assertThrows(IllegalArgumentException.class, () -> requestFor("http://192.168.1.10:49152/local-lab/fake-backend"));
        assertThrows(IllegalArgumentException.class, () -> requestFor("https://127.0.0.1:49152/local-lab/fake-backend"));
        assertThrows(IllegalArgumentException.class, () -> requestFor("http://127.0.0.1:8080/local-lab/fake-backend"));
    }

    @Test
    void everyFixtureResponseMatchesExpectedStatusBodyEvidenceAndBoundaries() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            List<LocalLabLoopbackTrafficSmokeObservation> observations =
                    LocalLabLoopbackTrafficSmokeClient.smoke(harness);

            for (LocalLabLoopbackTrafficSmokeObservation observation : observations) {
                assertEquals("127.0.0.1", observation.host());
                assertTrue(observation.assignedPort() > 0);
                assertFalse(COMMON_FIXED_PORTS.contains(observation.assignedPort()));
                assertEquals(observation.expectedStatusCode(), observation.responseStatusCode());
                assertTrue(observation.evidenceLabelFound());
                assertTrue(observation.safetyBoundaryLabelFound());
                assertTrue(observation.notProvenBoundaryLabelFound());

                String normalized = normalize(observation.responseBodySummary());
                assertTrue(normalized.contains("test-scope fake backend handler only"));
                assertTrue(normalized.contains("test-scope loopback fake backend server harness only"));
                assertTrue(normalized.contains("not production proof"));
                assertTrue(normalized.contains("not live-cloud validation"));
                assertTrue(normalized.contains("not real-tenant validation"));

                if (!observation.boundaryResponse()) {
                    LocalLabFakeBackendResponseFixture fixture =
                            LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId(observation.scenarioId())
                                    .orElseThrow();
                    assertTrue(observation.matchedExpectedFixture());
                    assertTrue(observation.responseBodySummary().contains("scenarioId=" + fixture.scenarioId()));
                    assertTrue(observation.responseBodySummary().contains("backendId=" + fixture.backendId()));
                    assertTrue(observation.responseBodySummary().contains("statusCode="
                            + fixture.responseStatusCode()));
                    assertTrue(observation.responseBodySummary().contains("latencyLabel="
                            + fixture.responseLatencyLabel()));
                    assertTrue(observation.responseBodySummary().contains("bodySummary="
                            + fixture.responseBodySummary()));
                    assertTrue(observation.responseBodySummary().contains("errorLabel="
                            + fixture.simulatedErrorLabel()));
                    assertTrue(observation.responseBodySummary().contains("loadLabel="
                            + fixture.simulatedLoadLabel()));
                    assertTrue(observation.responseBodySummary().contains(fixture.evidenceNote()));
                }
            }
        }
    }

    @Test
    void unknownLabelBoundaryResponseIsStableThroughSmokeClient() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            List<LocalLabLoopbackTrafficSmokeObservation> first =
                    LocalLabLoopbackTrafficSmokeClient.smoke(harness);
            List<LocalLabLoopbackTrafficSmokeObservation> second =
                    LocalLabLoopbackTrafficSmokeClient.smoke(harness);
            LocalLabLoopbackTrafficSmokeObservation boundary = first.get(first.size() - 1);
            String normalized = normalize(boundary.responseBodySummary());

            assertEquals(first, second);
            assertTrue(boundary.boundaryResponse());
            assertFalse(boundary.matchedExpectedFixture());
            assertEquals(404, boundary.responseStatusCode());
            assertTrue(normalized.contains("unknown scenario or backend label"));
            assertTrue(normalized.contains("no matching test-scope response fixture"));
            assertTrue(normalized.contains("not production proof"));
        }
    }

    @Test
    void repeatedSmokeRunsAreDeterministicAndHarnessStopStateRemainsClean() throws Exception {
        LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start();
        List<LocalLabLoopbackTrafficSmokeObservation> first = LocalLabLoopbackTrafficSmokeClient.smoke(harness);
        List<LocalLabLoopbackTrafficSmokeObservation> second = LocalLabLoopbackTrafficSmokeClient.smoke(harness);

        assertEquals(first, second);
        assertFalse(harness.stopped());

        harness.close();

        assertTrue(harness.stopped());
        assertEquals(first.stream().map(LocalLabLoopbackTrafficSmokeObservation::scenarioId).toList(),
                second.stream().map(LocalLabLoopbackTrafficSmokeObservation::scenarioId).toList());
    }

    @Test
    void smokeClientBoundaryAvoidsToolRuntimeAndValidationOverclaims() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            Set<Integer> assignedPorts = harness.descriptors().stream()
                    .map(LocalLabMultiBackendLoopbackHarness.Descriptor::assignedPort)
                    .collect(java.util.stream.Collectors.toSet());
            List<LocalLabLoopbackTrafficSmokeObservation> observations =
                    LocalLabLoopbackTrafficSmokeClient.smoke(harness);

            assertEquals(assignedPorts, observations.stream()
                    .map(LocalLabLoopbackTrafficSmokeObservation::assignedPort)
                    .collect(java.util.stream.Collectors.toSet()));

            for (LocalLabLoopbackTrafficSmokeObservation observation : observations) {
                String normalized = normalize(observation.localOnlyBoundaryLabel());

                assertTrue(normalized.contains("test-scope deterministic loopback traffic smoke client only"));
                assertTrue(normalized.contains("127.0.0.1"));
                assertTrue(normalized.contains("harness-assigned ephemeral"));
                assertTrue(normalized.contains("not k6"));
                assertTrue(normalized.contains("not bruno"));
                assertTrue(normalized.contains("not docker"));
                assertTrue(normalized.contains("not toxiproxy"));
                assertTrue(normalized.contains("not production traffic"));
                assertTrue(normalized.contains("not production proof"));
                assertTrue(normalized.contains("not live-cloud validation"));
                assertTrue(normalized.contains("not real-tenant validation"));
                assertTrue(normalized.contains("not production certification"));

                for (String forbidden : forbiddenOverclaims()) {
                    assertFalse(normalized.contains(forbidden), observation.scenarioId()
                            + " must not overclaim " + forbidden);
                }
            }
        }
    }

    private static LocalLabLoopbackTrafficSmokeRequest requestFor(String uri) {
        return new LocalLabLoopbackTrafficSmokeRequest(
                "scenario",
                "backend",
                URI.create(uri),
                "GET label",
                "/local-lab/fake-backend path label",
                200,
                "latency label",
                "error label | load label",
                "evidence label",
                "safety label",
                "not production proof");
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
                "production api behavior is changed");
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
