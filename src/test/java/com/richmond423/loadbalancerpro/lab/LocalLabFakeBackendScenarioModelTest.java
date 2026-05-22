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

class LocalLabFakeBackendScenarioModelTest {
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final List<Path> MODEL_SOURCES = List.of(
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabFakeBackendBehaviorProfile.java"),
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabFakeBackendNodeScenario.java"),
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabScenarioCatalog.java"));

    @Test
    void catalogContainsExpectedScenarioCategoriesInDeterministicOrder() {
        List<LocalLabFakeBackendNodeScenario> scenarios = LocalLabScenarioCatalog.scenarios();

        assertEquals(List.of(
                "backend-healthy-fast",
                "backend-slow-tail-latency",
                "backend-partial-degradation",
                "backend-error-prone",
                "backend-overloaded-queue-pressure",
                "backend-all-unhealthy-no-good-choice",
                "backend-recovery"), scenarios.stream().map(LocalLabFakeBackendNodeScenario::backendId).toList());
        assertEquals(List.of(
                LocalLabFakeBackendBehaviorProfile.HEALTHY_FAST,
                LocalLabFakeBackendBehaviorProfile.SLOW_TAIL_LATENCY,
                LocalLabFakeBackendBehaviorProfile.PARTIAL_DEGRADATION,
                LocalLabFakeBackendBehaviorProfile.ERROR_PRONE,
                LocalLabFakeBackendBehaviorProfile.OVERLOADED_QUEUE_PRESSURE,
                LocalLabFakeBackendBehaviorProfile.ALL_UNHEALTHY_OR_NO_GOOD_CHOICE,
                LocalLabFakeBackendBehaviorProfile.RECOVERY),
                scenarios.stream().map(LocalLabFakeBackendNodeScenario::behaviorProfile).toList());
        assertEquals(Set.of(LocalLabFakeBackendBehaviorProfile.values()),
                Set.copyOf(scenarios.stream().map(LocalLabFakeBackendNodeScenario::behaviorProfile).toList()));
        assertThrows(UnsupportedOperationException.class, () -> scenarios.add(scenarios.get(0)));
    }

    @Test
    void eachScenarioHasStableIdentityEvidenceExpectationsAndBoundaries() {
        for (LocalLabFakeBackendNodeScenario scenario : LocalLabScenarioCatalog.scenarios()) {
            assertFalse(scenario.backendId().isBlank());
            assertTrue(scenario.backendId().startsWith("backend-"));
            assertFalse(scenario.backendName().isBlank());
            assertFalse(scenario.behaviorType().isBlank());
            assertFalse(scenario.behaviorLabel().isBlank());
            assertFalse(scenario.expectedLatencyBand().isBlank());
            assertFalse(scenario.expectedErrorBehavior().isBlank());
            assertFalse(scenario.expectedLoadQueueBehavior().isBlank());
            assertFalse(scenario.expectedHealthPosture().isBlank());
            assertTrue(scenario.evidenceExpectationSummary().contains("policy gate status"));
            assertTrue(scenario.evidenceExpectationSummary().contains("safety mode"));
            assertTrue(scenario.evidenceExpectationSummary().contains("explainable"));
            assertTrue(scenario.notProvenBoundary().contains("not production proof"));
            assertTrue(scenario.notProvenBoundary().contains("not live-cloud validation"));
            assertTrue(scenario.notProvenBoundary().contains("not real-tenant validation"));
            assertTrue(scenario.notProvenBoundary().contains(
                    "no Docker, k6, Bruno, or Toxiproxy implementation is required"));
        }
    }

    @Test
    void scenariosAvoidValidationAndImplementationOverclaims() {
        for (LocalLabFakeBackendNodeScenario scenario : LocalLabScenarioCatalog.scenarios()) {
            String normalized = scenarioText(scenario).toLowerCase(Locale.ROOT);

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
                    "fake backend server is implemented")) {
                assertFalse(normalized.contains(forbidden), scenario.backendId() + " must not overclaim "
                        + forbidden);
            }
        }
    }

    @Test
    void catalogLookupIsStableAndClosedForUnknownIds() {
        assertTrue(LocalLabScenarioCatalog.findByBackendId("backend-healthy-fast").isPresent());
        assertEquals(LocalLabFakeBackendBehaviorProfile.RECOVERY,
                LocalLabScenarioCatalog.findByBackendId("backend-recovery").orElseThrow().behaviorProfile());
        assertTrue(LocalLabScenarioCatalog.findByBackendId("missing-backend").isEmpty());
        assertEquals(LocalLabScenarioCatalog.scenarios().stream().map(LocalLabFakeBackendNodeScenario::backendId)
                .toList(), LocalLabScenarioCatalog.scenarios().stream().map(LocalLabFakeBackendNodeScenario::backendId)
                .toList());
    }

    @Test
    void modelSourcesStayPassiveAndDoNotUseRuntimeSideEffects() throws Exception {
        for (Path source : MODEL_SOURCES) {
            String text = Files.readString(source, StandardCharsets.UTF_8);
            String normalized = text.toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
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
                    "new thread",
                    "executor")) {
                assertFalse(normalized.contains(forbidden), source + " must not use " + forbidden);
            }
        }
    }

    @Test
    void docsDescribePr250AsTestScopeScenarioModelOnly() throws Exception {
        String adr = read(ADR_0009);
        String matrix = read(MATRIX);
        String trustMap = read(TRUST_MAP);

        for (String doc : List.of(adr, matrix, trustMap)) {
            assertTrue(doc.contains("PR #250 adds only a test-scope scenario model/catalog."));
            assertTrue(doc.contains("It does not implement fake backend servers."));
            assertTrue(doc.contains(
                    "It does not implement Docker Compose, k6, Bruno, Toxiproxy, Prometheus/Grafana, scripts, networking, or runtime behavior."));
            assertTrue(doc.contains("It is a stepping stone toward future local lab tooling."));
        }
    }

    private static String scenarioText(LocalLabFakeBackendNodeScenario scenario) {
        return String.join(" ",
                scenario.backendId(),
                scenario.backendName(),
                scenario.behaviorType(),
                scenario.behaviorLabel(),
                scenario.expectedLatencyBand(),
                scenario.expectedErrorBehavior(),
                scenario.expectedLoadQueueBehavior(),
                scenario.expectedHealthPosture(),
                scenario.evidenceExpectationSummary(),
                scenario.notProvenBoundary());
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
