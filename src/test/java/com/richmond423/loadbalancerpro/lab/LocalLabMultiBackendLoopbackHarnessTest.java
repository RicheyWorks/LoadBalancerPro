package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LocalLabMultiBackendLoopbackHarnessTest {
    private static final List<Integer> COMMON_FIXED_PORTS = List.of(0, 80, 443, 8080, 8081, 9090);
    private static final List<LocalLabFakeBackendBehaviorProfile> REQUIRED_PROFILES = List.of(
            LocalLabFakeBackendBehaviorProfile.HEALTHY_FAST,
            LocalLabFakeBackendBehaviorProfile.SLOW_TAIL_LATENCY,
            LocalLabFakeBackendBehaviorProfile.PARTIAL_DEGRADATION,
            LocalLabFakeBackendBehaviorProfile.ERROR_PRONE,
            LocalLabFakeBackendBehaviorProfile.OVERLOADED_QUEUE_PRESSURE,
            LocalLabFakeBackendBehaviorProfile.ALL_UNHEALTHY_OR_NO_GOOD_CHOICE,
            LocalLabFakeBackendBehaviorProfile.RECOVERY);

    @Test
    void harnessStartsOneLoopbackServerPerRequiredScenarioProfile() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            List<LocalLabMultiBackendLoopbackHarness.Descriptor> descriptors = harness.descriptors();

            assertEquals(REQUIRED_PROFILES.size(), descriptors.size());
            assertEquals(
                    REQUIRED_PROFILES.stream().map(Enum::name).toList(),
                    descriptors.stream().map(LocalLabMultiBackendLoopbackHarness.Descriptor::behaviorType).toList());
            assertEquals(
                    LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                            .map(LocalLabFakeBackendResponseFixture::scenarioId)
                            .toList(),
                    descriptors.stream().map(LocalLabMultiBackendLoopbackHarness.Descriptor::scenarioId).toList());
            assertFalse(harness.stopped());
        }
    }

    @Test
    void everyServerBindsToLoopbackAndUsesUniqueEphemeralAssignedPorts() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            Set<Integer> assignedPorts = new HashSet<>();

            for (LocalLabMultiBackendLoopbackHarness.Descriptor descriptor : harness.descriptors()) {
                assertEquals("127.0.0.1", descriptor.host());
                assertTrue(descriptor.assignedPort() > 0);
                assertFalse(COMMON_FIXED_PORTS.contains(descriptor.assignedPort()));
                assertTrue(assignedPorts.add(descriptor.assignedPort()));
                assertTrue(descriptor.localUrl().startsWith("http://127.0.0.1:"));
                assertTrue(descriptor.localUrl().endsWith(LocalLabLoopbackFakeBackendServer.ENDPOINT_PATH));
                assertFalse(descriptor.localUrl().contains("0.0.0.0"));
                assertFalse(descriptor.localUrl().contains("localhost"));
            }
        }
    }

    @Test
    void everyServerRespondsWithExpectedScenarioProfileFixtureOutput() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            for (LocalLabMultiBackendLoopbackHarness.Descriptor descriptor : harness.descriptors()) {
                LocalLabFakeBackendResponseFixture fixture = fixtureFor(descriptor);
                LoopbackResponse response = get(requestUri(descriptor, fixture));
                String normalized = normalize(response.body());

                assertEquals(fixture.responseStatusCode(), response.statusCode());
                assertTrue(response.body().contains("scenarioId=" + fixture.scenarioId()));
                assertTrue(response.body().contains("backendId=" + fixture.backendId()));
                assertTrue(response.body().contains("statusCode=" + fixture.responseStatusCode()));
                assertTrue(response.body().contains("latencyLabel=" + fixture.responseLatencyLabel()));
                assertTrue(response.body().contains("bodySummary=" + fixture.responseBodySummary()));
                assertTrue(response.body().contains("errorLabel=" + fixture.simulatedErrorLabel()));
                assertTrue(response.body().contains("loadLabel=" + fixture.simulatedLoadLabel()));
                assertTrue(response.body().contains(fixture.evidenceNote()));
                assertTrue(normalized.contains("test-scope loopback fake backend server harness only"));
                assertTrue(normalized.contains("os-assigned ephemeral port"));
                assertTrue(normalized.contains("not production proof"));
            }
        }
    }

    @Test
    void unknownLabelResponseRemainsStableThroughHarness() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            LocalLabMultiBackendLoopbackHarness.Descriptor descriptor = harness.descriptors().get(0);
            URI uri = requestUri(
                    descriptor,
                    "unknown-scenario-label",
                    "unknown-backend-label",
                    "GET label",
                    "/local-lab/multi-backend/unknown path label");

            LoopbackResponse first = get(uri);
            LoopbackResponse second = get(uri);
            String normalized = normalize(first.body());

            assertEquals(first, second);
            assertEquals(404, first.statusCode());
            assertTrue(first.body().contains("scenarioId=unknown-scenario-label"));
            assertTrue(first.body().contains("backendId=unknown-backend-label"));
            assertTrue(normalized.contains("unknown scenario or backend label"));
            assertTrue(normalized.contains("no matching test-scope response fixture"));
            assertTrue(normalized.contains("not production proof"));
        }
    }

    @Test
    void harnessStopsAllServersCleanlyAndKeepsStableDeterministicState() throws Exception {
        LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start();
        List<LocalLabMultiBackendLoopbackHarness.Descriptor> descriptorsBeforeStop = harness.descriptors();

        assertFalse(harness.stopped());
        harness.close();

        assertTrue(harness.stopped());
        assertEquals(descriptorsBeforeStop, harness.descriptors());

        harness.close();

        assertTrue(harness.stopped());
        assertEquals(descriptorsBeforeStop, harness.descriptors());
    }

    @Test
    void repeatedHarnessStartStopCyclesWork() throws Exception {
        for (int cycle = 0; cycle < 3; cycle++) {
            LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start();

            assertEquals(REQUIRED_PROFILES.size(), harness.descriptors().size());
            assertFalse(harness.stopped());

            harness.close();

            assertTrue(harness.stopped());
            assertEquals(REQUIRED_PROFILES.size(), harness.descriptors().size());
        }
    }

    @Test
    void harnessBoundaryAvoidsToolRuntimeAndValidationOverclaims() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            for (LocalLabMultiBackendLoopbackHarness.Descriptor descriptor : harness.descriptors()) {
                String normalized = normalize(descriptor.notProductionProofBoundary());

                assertTrue(normalized.contains("test-scope multi-backend loopback harness only"));
                assertTrue(normalized.contains("127.0.0.1"));
                assertTrue(normalized.contains("os-assigned ephemeral"));
                assertTrue(normalized.contains("docker"));
                assertTrue(normalized.contains("k6"));
                assertTrue(normalized.contains("bruno"));
                assertTrue(normalized.contains("toxiproxy"));
                assertTrue(normalized.contains("prometheus/grafana"));
                assertTrue(normalized.contains("does not add production endpoints"));
                assertTrue(normalized.contains("runtime behavior"));
                assertTrue(normalized.contains("not production proof"));
                assertTrue(normalized.contains("not live-cloud validation"));
                assertTrue(normalized.contains("not real-tenant validation"));
                assertTrue(normalized.contains("not production certification"));

                for (String forbidden : List.of(
                        "production-ready",
                        "production certified",
                        "production certification is proven",
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
                        "production api behavior is changed")) {
                    assertFalse(normalized.contains(forbidden), descriptor.backendId() + " must not overclaim "
                            + forbidden);
                }
            }
        }
    }

    private static LocalLabFakeBackendResponseFixture fixtureFor(
            LocalLabMultiBackendLoopbackHarness.Descriptor descriptor) {
        return LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId(descriptor.scenarioId())
                .orElseThrow();
    }

    private static URI requestUri(
            LocalLabMultiBackendLoopbackHarness.Descriptor descriptor,
            LocalLabFakeBackendResponseFixture fixture) {
        LocalLabPassiveTranscriptEntry entry = LocalLabPassiveTranscriptCatalog.findByScenarioId(
                fixture.scenarioId()).orElseThrow().entries().get(0);
        return requestUri(
                descriptor,
                fixture.scenarioId(),
                fixture.backendId(),
                entry.simulatedRequestMethodLabel(),
                entry.simulatedRequestPathLabel());
    }

    private static URI requestUri(
            LocalLabMultiBackendLoopbackHarness.Descriptor descriptor,
            String scenarioId,
            String backendId,
            String requestMethodLabel,
            String requestPathLabel) {
        String query = String.join("&",
                parameter("scenarioId", scenarioId),
                parameter("backendId", backendId),
                parameter("requestMethodLabel", requestMethodLabel),
                parameter("requestPathLabel", requestPathLabel),
                parameter("requestBodySummary", "multi-backend harness test request body label for " + scenarioId),
                parameter("requestPurposeLabel", "test-scope multi-backend loopback harness request label"));
        return URI.create(descriptor.localUrl() + "?" + query);
    }

    private static String parameter(String key, String value) {
        return encode(key) + "=" + encode(value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static LoopbackResponse get(URI uri) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("GET");
        try {
            int statusCode = connection.getResponseCode();
            String body = new String(
                    (statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream())
                            .readAllBytes(),
                    StandardCharsets.UTF_8);
            return new LoopbackResponse(statusCode, body);
        } finally {
            connection.disconnect();
        }
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private record LoopbackResponse(int statusCode, String body) {
    }
}
