package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LocalLabFakeBackendResponseFixtureTest {
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final List<Path> FIXTURE_SOURCES = List.of(
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabFakeBackendResponseFixture.java"),
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabFakeBackendResponseFixtureCatalog.java"));

    @Test
    void everyScenarioInCatalogHasAResponseFixture() {
        List<LocalLabFakeBackendNodeScenario> scenarios = LocalLabScenarioCatalog.scenarios();
        List<LocalLabFakeBackendResponseFixture> fixtures = LocalLabFakeBackendResponseFixtureCatalog.fixtures();

        assertEquals(scenarios.stream().map(LocalLabFakeBackendNodeScenario::backendId).toList(),
                fixtures.stream().map(LocalLabFakeBackendResponseFixture::scenarioId).toList());
        assertEquals(scenarios.stream().map(LocalLabFakeBackendNodeScenario::backendId).toList(),
                fixtures.stream().map(LocalLabFakeBackendResponseFixture::backendId).toList());

        for (LocalLabFakeBackendNodeScenario scenario : scenarios) {
            LocalLabFakeBackendResponseFixture fixture =
                    LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId(scenario.backendId()).orElseThrow();
            assertEquals(scenario.backendId(), fixture.scenarioId());
            assertEquals(scenario.backendId(), fixture.backendId());
            assertEquals(scenario.behaviorProfile(), fixture.behaviorProfile());
            assertEquals(scenario.behaviorType(), fixture.behaviorType());
        }
    }

    @Test
    void fixtureOrderingAndIdsAreStableUniqueAndImmutable() {
        List<LocalLabFakeBackendResponseFixture> fixtures = LocalLabFakeBackendResponseFixtureCatalog.fixtures();
        List<String> fixtureIds = fixtures.stream().map(LocalLabFakeBackendResponseFixture::fixtureId).toList();

        assertEquals(List.of(
                "fixture-backend-healthy-fast",
                "fixture-backend-slow-tail-latency",
                "fixture-backend-partial-degradation",
                "fixture-backend-error-prone",
                "fixture-backend-overloaded-queue-pressure",
                "fixture-backend-all-unhealthy-no-good-choice",
                "fixture-backend-recovery"), fixtureIds);
        assertEquals(fixtureIds.size(), Set.copyOf(fixtureIds).size());
        assertEquals(fixtureIds, LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                .map(LocalLabFakeBackendResponseFixture::fixtureId)
                .toList());
        assertThrows(UnsupportedOperationException.class, () -> fixtures.add(fixtures.get(0)));
    }

    @Test
    void fixtureStatusAndLabelsMatchScenarioBehaviorTypes() {
        for (LocalLabFakeBackendResponseFixture fixture : LocalLabFakeBackendResponseFixtureCatalog.fixtures()) {
            String latency = fixture.responseLatencyLabel().toLowerCase(Locale.ROOT);
            String error = fixture.simulatedErrorLabel().toLowerCase(Locale.ROOT);
            String load = fixture.simulatedLoadLabel().toLowerCase(Locale.ROOT);
            String body = fixture.responseBodySummary().toLowerCase(Locale.ROOT);

            switch (fixture.behaviorProfile()) {
                case HEALTHY_FAST -> {
                    assertEquals(200, fixture.responseStatusCode());
                    assertTrue(latency.contains("low-latency"));
                    assertTrue(error.contains("no simulated application error"));
                    assertTrue(load.contains("low load"));
                    assertTrue(body.contains("successful baseline"));
                }
                case SLOW_TAIL_LATENCY -> {
                    assertEquals(200, fixture.responseStatusCode());
                    assertTrue(latency.contains("p95/p99"));
                    assertTrue(latency.contains("tail-latency"));
                    assertTrue(error.contains("timeout risk"));
                    assertTrue(load.contains("latency pressure"));
                    assertTrue(body.contains("slow response"));
                }
                case PARTIAL_DEGRADATION -> {
                    assertEquals(200, fixture.responseStatusCode());
                    assertTrue(latency.contains("degraded"));
                    assertTrue(error.contains("soft failure"));
                    assertTrue(load.contains("unstable capacity"));
                    assertTrue(body.contains("still responding"));
                }
                case ERROR_PRONE -> {
                    assertEquals(500, fixture.responseStatusCode());
                    assertTrue(latency.contains("500-style"));
                    assertTrue(error.contains("500-style error"));
                    assertTrue(load.contains("error rate increases"));
                    assertTrue(body.contains("error-prone"));
                }
                case OVERLOADED_QUEUE_PRESSURE -> {
                    assertEquals(503, fixture.responseStatusCode());
                    assertTrue(latency.contains("queue-pressure"));
                    assertTrue(error.contains("rejected-work"));
                    assertTrue(load.contains("elevated queue depth"));
                    assertTrue(body.contains("overload"));
                }
                case ALL_UNHEALTHY_OR_NO_GOOD_CHOICE -> {
                    assertEquals(503, fixture.responseStatusCode());
                    assertTrue(latency.contains("failure-boundary"));
                    assertTrue(error.contains("all candidates failing"));
                    assertTrue(load.contains("unsafe load"));
                    assertTrue(body.contains("no-good-choice"));
                }
                case RECOVERY -> {
                    assertEquals(200, fixture.responseStatusCode());
                    assertTrue(latency.contains("improving"));
                    assertTrue(error.contains("trending down"));
                    assertTrue(load.contains("returning toward baseline"));
                    assertTrue(body.contains("recovery"));
                }
            }
        }
    }

    @Test
    void everyFixtureCarriesEvidenceNotesAndNotProvenBoundaries() {
        for (LocalLabFakeBackendResponseFixture fixture : LocalLabFakeBackendResponseFixtureCatalog.fixtures()) {
            assertFalse(fixture.evidenceNote().isBlank());
            assertTrue(fixture.evidenceNote().contains("Evidence should note"));
            assertTrue(fixture.evidenceNote().contains("backend"));
            assertTrue(fixture.evidenceNote().contains("local simulation boundary"));

            assertFalse(fixture.notProvenBoundary().isBlank());
            assertTrue(fixture.notProvenBoundary().contains("not production proof"));
            assertTrue(fixture.notProvenBoundary().contains("not live-cloud validation"));
            assertTrue(fixture.notProvenBoundary().contains("not real-tenant validation"));
            assertTrue(fixture.notProvenBoundary().contains(
                    "no Docker, k6, Bruno, or Toxiproxy execution is required"));
            assertTrue(fixture.notProvenBoundary().contains("no fake backend server"));
            assertTrue(fixture.notProvenBoundary().contains("listener"));
            assertTrue(fixture.notProvenBoundary().contains("port"));
            assertTrue(fixture.notProvenBoundary().contains("loopback HTTP call"));
            assertTrue(fixture.notProvenBoundary().contains("generated traffic"));
            assertTrue(fixture.notProvenBoundary().contains("runtime behavior"));
        }
    }

    @Test
    void fixturesAvoidValidationRuntimeAndToolingOverclaims() {
        for (LocalLabFakeBackendResponseFixture fixture : LocalLabFakeBackendResponseFixtureCatalog.fixtures()) {
            String normalized = fixtureText(fixture).toLowerCase(Locale.ROOT);

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
                    "runtime behavior is changed",
                    "routing behavior is changed")) {
                assertFalse(normalized.contains(forbidden), fixture.fixtureId() + " must not overclaim "
                        + forbidden);
            }
        }
    }

    @Test
    void fixtureCatalogSourcesStayPassiveAndAvoidRuntimeSideEffects() throws Exception {
        for (Path source : FIXTURE_SOURCES) {
            String text = Files.readString(source, StandardCharsets.UTF_8);
            String normalized = text.toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "thread.sleep",
                    "java.time",
                    "random",
                    "uuid",
                    "system.getenv",
                    "system.getproperty",
                    "currenttimemillis",
                    "nanotime",
                    "processbuilder",
                    "runtime.getruntime",
                    "files.",
                    "path.",
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
    void docsDescribeResponseFixtureSprintAsTestScopeOnly() throws Exception {
        String adr = read(ADR_0009);
        String matrix = read(MATRIX);
        String trustMap = read(TRUST_MAP);

        for (String doc : List.of(adr, matrix, trustMap)) {
            assertTrue(doc.contains("This PR adds test-scope response fixtures only."));
            assertTrue(doc.contains("The fixtures describe future fake backend response expectations."));
            assertTrue(doc.contains("It is not fake backend server implementation."));
            assertTrue(doc.contains("It does not implement fake backend servers."));
            assertTrue(doc.contains(
                    "It does not start listeners, open ports, call localhost, generate traffic, run Docker, k6, Bruno, Toxiproxy, Prometheus/Grafana, scripts, networking, or runtime behavior."));
            assertTrue(doc.contains("Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling"));
            assertTrue(doc.contains("this is not production proof"));
        }
    }

    private static String fixtureText(LocalLabFakeBackendResponseFixture fixture) {
        return String.join(" ",
                fixture.fixtureId(),
                fixture.scenarioId(),
                fixture.backendId(),
                fixture.behaviorType(),
                Integer.toString(fixture.responseStatusCode()),
                fixture.responseLatencyLabel(),
                fixture.responseBodySummary(),
                fixture.simulatedErrorLabel(),
                fixture.simulatedLoadLabel(),
                fixture.evidenceNote(),
                fixture.notProvenBoundary());
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
