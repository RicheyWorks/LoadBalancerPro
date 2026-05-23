package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabFakeBackendHandlerTest {
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path READINESS_DOC = Path.of("docs/LOCAL_LAB_IMPLEMENTATION_READINESS_GATE.md");
    private static final List<Path> HANDLER_SOURCES = List.of(
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabFakeBackendRequest.java"),
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabFakeBackendHandledResponse.java"),
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabFakeBackendHandler.java"));

    @Test
    void handlerReturnsExpectedResponseForHealthyFast() {
        assertHandledFixture(LocalLabFakeBackendBehaviorProfile.HEALTHY_FAST);
    }

    @Test
    void handlerReturnsExpectedResponseForSlowTailLatency() {
        assertHandledFixture(LocalLabFakeBackendBehaviorProfile.SLOW_TAIL_LATENCY);
    }

    @Test
    void handlerReturnsExpectedResponseForPartialDegradation() {
        assertHandledFixture(LocalLabFakeBackendBehaviorProfile.PARTIAL_DEGRADATION);
    }

    @Test
    void handlerReturnsExpectedResponseForErrorProne() {
        assertHandledFixture(LocalLabFakeBackendBehaviorProfile.ERROR_PRONE);
    }

    @Test
    void handlerReturnsExpectedResponseForOverloadedQueuePressure() {
        assertHandledFixture(LocalLabFakeBackendBehaviorProfile.OVERLOADED_QUEUE_PRESSURE);
    }

    @Test
    void handlerReturnsExpectedResponseForAllUnhealthyOrNoGoodChoice() {
        assertHandledFixture(LocalLabFakeBackendBehaviorProfile.ALL_UNHEALTHY_OR_NO_GOOD_CHOICE);
    }

    @Test
    void handlerReturnsExpectedResponseForRecovery() {
        assertHandledFixture(LocalLabFakeBackendBehaviorProfile.RECOVERY);
    }

    @Test
    void everyExistingResponseFixtureCanBeHandledThroughTheHandler() {
        List<LocalLabFakeBackendHandledResponse> handledResponses =
                LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                        .map(LocalLabFakeBackendHandlerTest::requestFor)
                        .map(LocalLabFakeBackendHandler::handle)
                        .toList();

        assertEquals(LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                .map(LocalLabFakeBackendResponseFixture::scenarioId)
                .toList(), handledResponses.stream()
                .map(LocalLabFakeBackendHandledResponse::scenarioId)
                .toList());
        assertEquals(LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                .map(LocalLabFakeBackendResponseFixture::backendId)
                .toList(), handledResponses.stream()
                .map(LocalLabFakeBackendHandledResponse::backendId)
                .toList());
    }

    @Test
    void responseValuesAreDeterministicAcrossRepeatedCalls() {
        LocalLabFakeBackendRequest request = requestFor(fixtureFor(
                LocalLabFakeBackendBehaviorProfile.SLOW_TAIL_LATENCY));

        LocalLabFakeBackendHandledResponse first = LocalLabFakeBackendHandler.handle(request);
        LocalLabFakeBackendHandledResponse second = LocalLabFakeBackendHandler.handle(request);

        assertEquals(first, second);
        assertEquals(first.deterministicText(), second.deterministicText());
        assertEquals(request.deterministicText(), request.deterministicText());
    }

    @Test
    void unknownScenarioAndBackendLabelsReturnStableBoundaryResponses() {
        LocalLabFakeBackendRequest unknown = new LocalLabFakeBackendRequest(
                "unknown-scenario-label",
                "unknown-backend-label",
                "GET label",
                "/local-lab/test-scope/unknown path label",
                "unknown request body label",
                "unknown boundary request label");
        LocalLabFakeBackendRequest mismatched = new LocalLabFakeBackendRequest(
                "backend-healthy-fast",
                "unknown-backend-label",
                "GET label",
                "/local-lab/test-scope/mismatched path label",
                "mismatched request body label",
                "mismatched boundary request label");

        LocalLabFakeBackendHandledResponse first = LocalLabFakeBackendHandler.handle(unknown);
        LocalLabFakeBackendHandledResponse second = LocalLabFakeBackendHandler.handle(unknown);
        LocalLabFakeBackendHandledResponse mismatch = LocalLabFakeBackendHandler.handle(mismatched);
        LocalLabFakeBackendHandledResponse missing = LocalLabFakeBackendHandler.handle(null);

        assertEquals(first, second);
        assertEquals(404, first.statusCode());
        assertEquals("unknown-scenario-label", first.scenarioId());
        assertEquals("unknown-backend-label", first.backendId());
        assertTrue(first.errorLabel().contains("unknown scenario or backend label"));
        assertTrue(first.evidenceNote().contains("no matching test-scope response fixture"));
        assertEquals(404, mismatch.statusCode());
        assertEquals("backend-healthy-fast", mismatch.scenarioId());
        assertEquals("unknown-backend-label", mismatch.backendId());
        assertEquals(404, missing.statusCode());
        assertEquals("missing-scenario-label", missing.scenarioId());
        assertEquals("missing-backend-label", missing.backendId());
    }

    @Test
    void handledResponsesCarryEvidenceSafetyAndNotProvenBoundaries() {
        for (LocalLabFakeBackendResponseFixture fixture : LocalLabFakeBackendResponseFixtureCatalog.fixtures()) {
            LocalLabFakeBackendHandledResponse response = LocalLabFakeBackendHandler.handle(requestFor(fixture));
            String normalized = normalize(response.deterministicText());

            assertTrue(response.evidenceNote().contains("Evidence should note"));
            assertTrue(response.evidenceNote().contains("Handler mapped simulated request labels"));
            assertTrue(normalized.contains("test-scope fake backend handler only"));
            assertTrue(normalized.contains("does not implement a fake backend server"));
            assertTrue(normalized.contains("start listeners"));
            assertTrue(normalized.contains("open ports"));
            assertTrue(normalized.contains("call loopback endpoints"));
            assertTrue(normalized.contains("generate traffic"));
            assertTrue(normalized.contains("not production proof"));
            assertTrue(normalized.contains("not live-cloud validation"));
            assertTrue(normalized.contains("not real-tenant validation"));
            assertTrue(normalized.contains("not replay execution"));
            assertTrue(normalized.contains("not storage"));
            assertTrue(normalized.contains("export"));
            assertTrue(normalized.contains("runtime"));
        }
    }

    @Test
    void handledResponsesAvoidValidationRuntimeExecutionStorageAndExportOverclaims() {
        for (LocalLabFakeBackendResponseFixture fixture : LocalLabFakeBackendResponseFixtureCatalog.fixtures()) {
            String normalized = normalize(LocalLabFakeBackendHandler.handle(requestFor(fixture)).deterministicText());

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
                    "fake backend server is implemented",
                    "actual traffic is generated",
                    "http request is executed",
                    "replay execution is implemented",
                    "evidence report is generated",
                    "report generation is implemented",
                    "storage is implemented",
                    "export behavior is implemented",
                    "runtime behavior is changed",
                    "routing behavior is changed")) {
                assertFalse(normalized.contains(forbidden), fixture.fixtureId() + " must not overclaim "
                        + forbidden);
            }
        }
    }

    @Test
    void handlerSourcesStayTestScopeAndAvoidSideEffectApis() throws Exception {
        for (Path source : HANDLER_SOURCES) {
            String normalized = normalize(Files.readString(source, StandardCharsets.UTF_8));

            for (String forbidden : List.of(
                    "thread.sleep",
                    "java.time",
                    "new random",
                    "securerandom",
                    "uuid",
                    "system.getenv",
                    "system.getproperty",
                    "currenttimemillis",
                    "nanotime",
                    "processbuilder",
                    "runtime.getruntime",
                    "files.",
                    "path.",
                    "java.io",
                    "socket",
                    "serversocket",
                    "httpclient",
                    "urlconnection",
                    "localhost",
                    "127.0.0.1",
                    "http://",
                    "https://",
                    "new thread",
                    "executor")) {
                assertFalse(normalized.contains(forbidden), source + " must not use " + forbidden);
            }
        }
    }

    @Test
    void docsDescribeTestScopeFakeBackendHandlerBoundary() throws Exception {
        for (Path path : List.of(ADR_0009, MATRIX, TRUST_MAP, READINESS_DOC)) {
            String doc = read(path);

            assertTrue(doc.contains("This PR adds a test-scope fake backend handler only."));
            assertTrue(doc.contains(
                    "The handler maps simulated request labels to existing response fixtures in memory."));
            assertTrue(doc.contains("It does not implement fake backend servers."));
            assertTrue(doc.contains("It does not start listeners."));
            assertTrue(doc.contains("It does not open ports."));
            assertTrue(doc.contains("It does not call localhost."));
            assertTrue(doc.contains("It does not generate traffic."));
            assertTrue(doc.contains("It does not execute replay."));
            assertTrue(doc.contains("It does not generate evidence reports."));
            assertTrue(doc.contains("It does not write files."));
            assertTrue(doc.contains("It does not persist storage."));
            assertTrue(doc.contains("It does not export/download/upload/PDF/ZIP anything."));
            assertTrue(doc.contains("Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling."));
            assertTrue(doc.contains("Passing handler tests is not production proof."));
        }
    }

    private static void assertHandledFixture(LocalLabFakeBackendBehaviorProfile behaviorProfile) {
        LocalLabFakeBackendResponseFixture fixture = fixtureFor(behaviorProfile);
        LocalLabFakeBackendHandledResponse response = LocalLabFakeBackendHandler.handle(requestFor(fixture));

        assertEquals(fixture.scenarioId(), response.scenarioId());
        assertEquals(fixture.backendId(), response.backendId());
        assertEquals(fixture.responseStatusCode(), response.statusCode());
        assertEquals(fixture.responseLatencyLabel(), response.latencyLabel());
        assertEquals(fixture.responseBodySummary(), response.bodySummary());
        assertEquals(fixture.simulatedErrorLabel(), response.errorLabel());
        assertEquals(fixture.simulatedLoadLabel(), response.loadLabel());
        assertTrue(response.evidenceNote().contains(fixture.evidenceNote()));
        assertTrue(response.notProvenBoundary().contains(fixture.notProvenBoundary()));
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
                "test-scope request body label for " + fixture.scenarioId(),
                "test-scope handler fixture lookup label");
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
