package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabLoopbackFakeBackendServerTest {
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path READINESS_DOC = Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final Path SERVER_SOURCE =
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabLoopbackFakeBackendServer.java");

    @Test
    void serverBindsOnlyToLoopbackAndUsesEphemeralAssignedPort() throws Exception {
        try (LocalLabLoopbackFakeBackendServer server = LocalLabLoopbackFakeBackendServer.start()) {
            assertEquals("127.0.0.1", server.host());
            assertEquals(0, server.requestedPort());
            assertTrue(server.port() > 0);
            assertTrue(server.endpointUri().toString().startsWith("http://127.0.0.1:"));
            assertTrue(server.endpointUri().toString().endsWith(LocalLabLoopbackFakeBackendServer.ENDPOINT_PATH));
        }
    }

    @Test
    void serverStartsAndStopsInsideTestScope() throws Exception {
        LocalLabLoopbackFakeBackendServer server = LocalLabLoopbackFakeBackendServer.start();
        try {
            assertEquals("127.0.0.1", server.host());
            assertTrue(server.port() > 0);
        } finally {
            server.close();
            server.close();
        }
    }

    @Test
    void healthyFastRequestReturnsExpectedHandlerResponse() throws Exception {
        assertLoopbackMatchesFixture(LocalLabFakeBackendBehaviorProfile.HEALTHY_FAST);
    }

    @Test
    void slowTailLatencyRequestReturnsExpectedHandlerResponse() throws Exception {
        assertLoopbackMatchesFixture(LocalLabFakeBackendBehaviorProfile.SLOW_TAIL_LATENCY);
    }

    @Test
    void partialDegradationRequestReturnsExpectedHandlerResponse() throws Exception {
        assertLoopbackMatchesFixture(LocalLabFakeBackendBehaviorProfile.PARTIAL_DEGRADATION);
    }

    @Test
    void errorProneRequestReturnsExpectedHandlerResponse() throws Exception {
        assertLoopbackMatchesFixture(LocalLabFakeBackendBehaviorProfile.ERROR_PRONE);
    }

    @Test
    void overloadedQueuePressureRequestReturnsExpectedHandlerResponse() throws Exception {
        assertLoopbackMatchesFixture(LocalLabFakeBackendBehaviorProfile.OVERLOADED_QUEUE_PRESSURE);
    }

    @Test
    void allUnhealthyOrNoGoodChoiceRequestReturnsExpectedHandlerResponse() throws Exception {
        assertLoopbackMatchesFixture(LocalLabFakeBackendBehaviorProfile.ALL_UNHEALTHY_OR_NO_GOOD_CHOICE);
    }

    @Test
    void recoveryRequestReturnsExpectedHandlerResponse() throws Exception {
        assertLoopbackMatchesFixture(LocalLabFakeBackendBehaviorProfile.RECOVERY);
    }

    @Test
    void unknownLabelsReturnStableBoundaryResponse() throws Exception {
        try (LocalLabLoopbackFakeBackendServer server = LocalLabLoopbackFakeBackendServer.start()) {
            URI uri = requestUri(
                    server,
                    "unknown-scenario-label",
                    "unknown-backend-label",
                    "GET label",
                    "/local-lab/fake-backend/unknown path label");

            LoopbackResponse first = get(uri);
            LoopbackResponse second = get(uri);
            String normalized = normalize(first.body());

            assertEquals(first, second);
            assertEquals(404, first.statusCode());
            assertTrue(first.body().contains("scenarioId=unknown-scenario-label"));
            assertTrue(first.body().contains("backendId=unknown-backend-label"));
            assertTrue(normalized.contains("boundary response for unknown local lab fake backend labels"));
            assertTrue(normalized.contains("no matching test-scope response fixture"));
            assertTrue(normalized.contains("test-scope loopback fake backend server harness only"));
            assertTrue(normalized.contains("not production proof"));
        }
    }

    @Test
    void repeatedCallsAreDeterministic() throws Exception {
        try (LocalLabLoopbackFakeBackendServer server = LocalLabLoopbackFakeBackendServer.start()) {
            LocalLabFakeBackendResponseFixture fixture = fixtureFor(
                    LocalLabFakeBackendBehaviorProfile.SLOW_TAIL_LATENCY);
            URI uri = requestUri(server, fixture);

            LoopbackResponse first = get(uri);
            LoopbackResponse second = get(uri);

            assertEquals(first, second);
        }
    }

    @Test
    void responseContentIncludesEvidenceSafetyAndNotProvenBoundaryLabels() throws Exception {
        try (LocalLabLoopbackFakeBackendServer server = LocalLabLoopbackFakeBackendServer.start()) {
            for (LocalLabFakeBackendResponseFixture fixture : LocalLabFakeBackendResponseFixtureCatalog.fixtures()) {
                LoopbackResponse response = get(requestUri(server, fixture));
                String normalized = normalize(response.body());

                assertTrue(response.body().contains("evidenceNote="));
                assertTrue(response.body().contains("safetyBoundary="));
                assertTrue(response.body().contains("notProvenBoundary="));
                assertTrue(response.body().contains("serverBoundary="));
                assertTrue(normalized.contains("handler mapped simulated request labels"));
                assertTrue(normalized.contains("test-scope loopback fake backend server harness only"));
                assertTrue(normalized.contains("os-assigned ephemeral port"));
                assertTrue(normalized.contains("does not add production endpoints"));
                assertTrue(normalized.contains("not production proof"));
                assertTrue(normalized.contains("not live-cloud validation"));
                assertTrue(normalized.contains("not real-tenant validation"));
            }
        }
    }

    @Test
    void serverHarnessAvoidsToolRuntimeAndValidationOverclaims() throws Exception {
        try (LocalLabLoopbackFakeBackendServer server = LocalLabLoopbackFakeBackendServer.start()) {
            for (LocalLabFakeBackendResponseFixture fixture : LocalLabFakeBackendResponseFixtureCatalog.fixtures()) {
                String normalized = normalize(get(requestUri(server, fixture)).body());

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
                    assertFalse(normalized.contains(forbidden), fixture.fixtureId() + " must not overclaim "
                            + forbidden);
                }
            }
        }
    }

    @Test
    void serverSourceStaysTestScopeLoopbackAndAvoidsDisallowedSideEffects() throws Exception {
        String source = normalize(Files.readString(SERVER_SOURCE, StandardCharsets.UTF_8));

        assertTrue(source.contains("httpserver.create"));
        assertTrue(source.contains("\"127.0.0.1\""));
        assertTrue(source.contains("ephemeral_port_request = 0"));
        assertFalse(source.contains("\"0.0.0.0\""));
        assertFalse(source.contains("localhost"));
        assertFalse(source.contains("new random"));
        assertFalse(source.contains("securerandom"));
        assertFalse(source.contains("uuid"));
        assertFalse(source.contains("system.getenv"));
        assertFalse(source.contains("system.getproperty"));
        assertFalse(source.contains("currenttimemillis"));
        assertFalse(source.contains("nanotime"));
        assertFalse(source.contains("processbuilder"));
        assertFalse(source.contains("runtime.getruntime"));
        assertFalse(source.contains("thread.sleep"));
        assertFalse(source.contains("new thread"));
        assertFalse(source.contains("executors."));
        assertFalse(source.contains("files."));
        assertFalse(source.contains("java.nio.file"));
        assertFalse(source.contains("path.of"));
        assertFalse(source.contains("8080"));
        assertFalse(source.contains("9090"));
    }

    @Test
    void docsDescribeTestScopeLoopbackFakeBackendServerBoundary() throws Exception {
        for (Path path : List.of(ADR_0009, MATRIX, TRUST_MAP, READINESS_DOC)) {
            String doc = read(path);

            assertTrue(doc.contains(
                    "This PR adds a test-scope loopback fake backend server harness only."));
            assertTrue(doc.contains("The harness lives under `src/test/java`."));
            assertTrue(doc.contains("It binds to `127.0.0.1` only and uses OS-assigned ephemeral ports."));
            assertTrue(doc.contains("It does not add production endpoints."));
            assertTrue(doc.contains(
                    "It does not change production routing, proxy, scoring, strategy, or API behavior."));
            assertTrue(doc.contains("It does not add Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana."));
            assertTrue(doc.contains("It does not execute replay."));
            assertTrue(doc.contains("It does not generate evidence reports."));
            assertTrue(doc.contains("It does not write files."));
            assertTrue(doc.contains("It does not persist storage."));
            assertTrue(doc.contains("It does not export/download/upload/PDF/ZIP anything."));
            assertTrue(doc.contains("Passing loopback tests is not production proof."));
            assertTrue(doc.contains(
                    "Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven."));
        }
    }

    private static void assertLoopbackMatchesFixture(LocalLabFakeBackendBehaviorProfile behaviorProfile)
            throws Exception {
        LocalLabFakeBackendResponseFixture fixture = fixtureFor(behaviorProfile);
        LocalLabFakeBackendHandledResponse handledResponse = LocalLabFakeBackendHandler.handle(requestFor(fixture));

        try (LocalLabLoopbackFakeBackendServer server = LocalLabLoopbackFakeBackendServer.start()) {
            LoopbackResponse loopbackResponse = get(requestUri(server, fixture));

            assertEquals(fixture.responseStatusCode(), loopbackResponse.statusCode());
            assertTrue(loopbackResponse.body().contains("scenarioId=" + fixture.scenarioId()));
            assertTrue(loopbackResponse.body().contains("backendId=" + fixture.backendId()));
            assertTrue(loopbackResponse.body().contains("statusCode=" + handledResponse.statusCode()));
            assertTrue(loopbackResponse.body().contains("latencyLabel=" + handledResponse.latencyLabel()));
            assertTrue(loopbackResponse.body().contains("bodySummary=" + handledResponse.bodySummary()));
            assertTrue(loopbackResponse.body().contains("errorLabel=" + handledResponse.errorLabel()));
            assertTrue(loopbackResponse.body().contains("loadLabel=" + handledResponse.loadLabel()));
            assertTrue(loopbackResponse.body().contains(handledResponse.evidenceNote()));
            assertTrue(loopbackResponse.body().contains(handledResponse.safetyBoundary()));
            assertTrue(loopbackResponse.body().contains(handledResponse.notProvenBoundary()));
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
                parameter("requestBodySummary", "loopback test request body label for " + scenarioId),
                parameter("requestPurposeLabel", "test-scope loopback fake backend server request label"));
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

    private static LocalLabFakeBackendRequest requestFor(LocalLabFakeBackendResponseFixture fixture) {
        LocalLabPassiveTranscriptEntry entry = LocalLabPassiveTranscriptCatalog.findByScenarioId(
                fixture.scenarioId()).orElseThrow().entries().get(0);
        return new LocalLabFakeBackendRequest(
                fixture.scenarioId(),
                fixture.backendId(),
                entry.simulatedRequestMethodLabel(),
                entry.simulatedRequestPathLabel(),
                "loopback test request body label for " + fixture.scenarioId(),
                "test-scope loopback fake backend server request label");
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private record LoopbackResponse(int statusCode, String body) {
    }
}
