package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabLoopbackFakeBackendServerLifecycleTest {
    private static final List<Integer> COMMON_FIXED_PORTS = List.of(0, 80, 443, 8080, 8081, 9090);

    @Test
    void serverReportsLoopbackHostAndEphemeralAssignedPortOnly() throws Exception {
        try (LocalLabLoopbackFakeBackendServer server = LocalLabLoopbackFakeBackendServer.start()) {
            assertEquals("127.0.0.1", server.host());
            assertEquals(0, server.requestedPort());
            assertTrue(server.port() > 0);
            assertFalse(COMMON_FIXED_PORTS.contains(server.port()));
            assertEquals("127.0.0.1", server.endpointUri().getHost());
            assertEquals(LocalLabLoopbackFakeBackendServer.ENDPOINT_PATH, server.endpointUri().getPath());
            assertFalse(server.endpointUri().toString().contains("0.0.0.0"));
            assertFalse(server.endpointUri().toString().contains("localhost"));
            assertFalse(server.stopped());
        }
    }

    @Test
    void repeatedStartStopCyclesSucceedAndLeaveStableLifecycleState() throws Exception {
        for (int cycle = 0; cycle < 4; cycle++) {
            LocalLabLoopbackFakeBackendServer server = LocalLabLoopbackFakeBackendServer.start();
            String host = server.host();
            int requestedPort = server.requestedPort();
            int assignedPort = server.port();
            URI endpoint = server.endpointUri();

            assertEquals("127.0.0.1", host);
            assertEquals(0, requestedPort);
            assertTrue(assignedPort > 0);
            assertFalse(COMMON_FIXED_PORTS.contains(assignedPort));
            assertFalse(server.stopped());

            server.close();
            assertTrue(server.stopped());
            assertEquals(host, server.host());
            assertEquals(requestedPort, server.requestedPort());
            assertEquals(assignedPort, server.port());
            assertEquals(endpoint, server.endpointUri());
            server.close();
            assertTrue(server.stopped());
        }
    }

    @Test
    void sequentialInstancesStartWithoutFixedPortCollision() throws Exception {
        try (LocalLabLoopbackFakeBackendServer first = LocalLabLoopbackFakeBackendServer.start()) {
            assertEquals("127.0.0.1", first.host());
            assertEquals(0, first.requestedPort());
            assertTrue(first.port() > 0);
            assertFalse(COMMON_FIXED_PORTS.contains(first.port()));
        }

        try (LocalLabLoopbackFakeBackendServer second = LocalLabLoopbackFakeBackendServer.start()) {
            assertEquals("127.0.0.1", second.host());
            assertEquals(0, second.requestedPort());
            assertTrue(second.port() > 0);
            assertFalse(COMMON_FIXED_PORTS.contains(second.port()));
        }
    }

    @Test
    void healthyFastResponseWorksAfterLifecycleStart() throws Exception {
        LocalLabFakeBackendResponseFixture fixture = fixtureFor(
                LocalLabFakeBackendBehaviorProfile.HEALTHY_FAST);

        try (LocalLabLoopbackFakeBackendServer server = LocalLabLoopbackFakeBackendServer.start()) {
            LoopbackResponse response = get(requestUri(server, fixture));

            assertEquals(fixture.responseStatusCode(), response.statusCode());
            assertTrue(response.body().contains("scenarioId=" + fixture.scenarioId()));
            assertTrue(response.body().contains("backendId=" + fixture.backendId()));
            assertTrue(response.body().contains("statusCode=200"));
            assertTrue(response.body().contains(fixture.responseLatencyLabel()));
            assertTrue(response.body().contains(fixture.responseBodySummary()));
            assertTrue(response.body().contains(fixture.evidenceNote()));
            assertTrue(normalize(response.body()).contains("test-scope loopback fake backend server harness only"));
            assertFalse(server.stopped());
        }
    }

    @Test
    void unknownLabelBoundaryResponseWorksAfterLifecycleStart() throws Exception {
        try (LocalLabLoopbackFakeBackendServer server = LocalLabLoopbackFakeBackendServer.start()) {
            URI uri = requestUri(
                    server,
                    "unknown-scenario-label",
                    "unknown-backend-label",
                    "GET label",
                    "/local-lab/lifecycle/unknown path label");
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
            assertFalse(server.stopped());
        }
    }

    @Test
    void serverEndpointRemainsTestHarnessOnlyAndNotProductionEndpoint() throws Exception {
        LocalLabFakeBackendResponseFixture fixture = fixtureFor(
                LocalLabFakeBackendBehaviorProfile.HEALTHY_FAST);

        try (LocalLabLoopbackFakeBackendServer server = LocalLabLoopbackFakeBackendServer.start()) {
            LoopbackResponse response = get(requestUri(server, fixture));
            String normalized = normalize(response.body());

            assertEquals("127.0.0.1", server.endpointUri().getHost());
            assertEquals(LocalLabLoopbackFakeBackendServer.ENDPOINT_PATH, server.endpointUri().getPath());
            assertTrue(normalized.contains("test-scope loopback fake backend server harness only"));
            assertTrue(normalized.contains("does not add production endpoints"));
            assertTrue(normalized.contains("production api behavior"));
            assertTrue(normalized.contains("routing"));
            assertTrue(normalized.contains("scoring"));
            assertTrue(normalized.contains("strategy"));
            assertTrue(normalized.contains("proxy"));
            assertTrue(normalized.contains("runtime behavior"));
            assertTrue(normalized.contains("not production proof"));
        }
    }

    @Test
    void lifecycleResponsesAvoidToolingRuntimeAndValidationOverclaims() throws Exception {
        try (LocalLabLoopbackFakeBackendServer server = LocalLabLoopbackFakeBackendServer.start()) {
            List<LoopbackResponse> responses = List.of(
                    get(requestUri(server, fixtureFor(LocalLabFakeBackendBehaviorProfile.HEALTHY_FAST))),
                    get(requestUri(
                            server,
                            "unknown-scenario-label",
                            "unknown-backend-label",
                            "GET label",
                            "/local-lab/lifecycle/unknown path label")));

            for (LoopbackResponse response : responses) {
                String normalized = normalize(response.body());

                assertTrue(normalized.contains("docker"));
                assertTrue(normalized.contains("k6"));
                assertTrue(normalized.contains("bruno"));
                assertTrue(normalized.contains("toxiproxy"));
                assertTrue(normalized.contains("prometheus/grafana"));
                assertFalse(normalized.contains("docker compose is implemented"));
                assertFalse(normalized.contains("k6 scenario is implemented"));
                assertFalse(normalized.contains("bruno collection is implemented"));
                assertFalse(normalized.contains("toxiproxy config is implemented"));
                assertFalse(normalized.contains("prometheus/grafana dashboard is implemented"));
                assertFalse(normalized.contains("production-ready"));
                assertFalse(normalized.contains("production certified"));
                assertFalse(normalized.contains("production certification is proven"));
                assertFalse(normalized.contains("live-cloud validated"));
                assertFalse(normalized.contains("real-tenant validated"));
                assertFalse(normalized.contains("runtime behavior is changed"));
                assertFalse(normalized.contains("replay execution is implemented"));
                assertFalse(normalized.contains("evidence report is generated"));
                assertFalse(normalized.contains("storage is implemented"));
                assertFalse(normalized.contains("export behavior is implemented"));
            }
        }
    }

    private static URI requestUri(
            LocalLabLoopbackFakeBackendServer server,
            LocalLabFakeBackendResponseFixture fixture) {
        LocalLabPassiveTranscriptEntry entry = LocalLabPassiveTranscriptCatalog.findByScenarioId(
                fixture.scenarioId()).orElseThrow().entries().get(0);
        return requestUri(
                server,
                fixture.scenarioId(),
                fixture.backendId(),
                entry.simulatedRequestMethodLabel(),
                entry.simulatedRequestPathLabel());
    }

    private static URI requestUri(
            LocalLabLoopbackFakeBackendServer server,
            String scenarioId,
            String backendId,
            String requestMethodLabel,
            String requestPathLabel) {
        String query = String.join("&",
                parameter("scenarioId", scenarioId),
                parameter("backendId", backendId),
                parameter("requestMethodLabel", requestMethodLabel),
                parameter("requestPathLabel", requestPathLabel),
                parameter("requestBodySummary", "loopback lifecycle test request body label for " + scenarioId),
                parameter("requestPurposeLabel", "test-scope loopback lifecycle request label"));
        return URI.create(server.endpointUri() + "?" + query);
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

    private static LocalLabFakeBackendResponseFixture fixtureFor(
            LocalLabFakeBackendBehaviorProfile behaviorProfile) {
        return LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                .filter(fixture -> fixture.behaviorProfile() == behaviorProfile)
                .findFirst()
                .orElseThrow();
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private record LoopbackResponse(int statusCode, String body) {
    }
}
