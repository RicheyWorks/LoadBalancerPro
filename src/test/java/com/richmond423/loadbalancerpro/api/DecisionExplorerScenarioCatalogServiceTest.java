package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class DecisionExplorerScenarioCatalogServiceTest {
    private final DecisionExplorerScenarioCatalogService service = new DecisionExplorerScenarioCatalogService();

    @Test
    void buildCatalogReturnsDeterministicLocalFixtureScenariosInStableOrder() {
        DecisionExplorerScenarioCatalogV1 first = service.buildCatalog();
        DecisionExplorerScenarioCatalogV1 second = service.buildCatalog();

        assertEquals(first, second);
        assertTrue(first.readOnly());
        assertTrue(first.simulationOnly());
        assertEquals("DecisionExplorerScenarioCatalogV1", first.payloadObject());
        assertEquals("v1", first.contractVersion());
        assertEquals(10, first.scenarios().size());
        assertEquals(List.of(
                "normal-balanced-load",
                "tail-latency-pressure",
                "overload-pressure",
                "stale-signal",
                "conflicting-signal",
                "all-unhealthy-degradation",
                "recovery-transition",
                "capacity-skew",
                "repeated-replay-event-order",
                "zero-edge-metric-values"), first.scenarios().stream()
                .map(DecisionExplorerScenarioV1::scenarioId)
                .toList());
    }

    @Test
    void buildCatalogClassifiesRequiredReviewerScenarioStates() {
        DecisionExplorerScenarioCatalogV1 catalog = service.buildCatalog();

        DecisionExplorerScenarioV1 healthy = scenario(catalog, "normal-balanced-load");
        DecisionExplorerScenarioV1 partial = scenario(catalog, "stale-signal");
        DecisionExplorerScenarioV1 unknown = scenario(catalog, "all-unhealthy-degradation");

        assertEquals("HEALTHY_BASELINE", healthy.scenarioCategory());
        assertEquals("AVAILABLE", healthy.evidenceStatus());
        assertEquals("PARTIAL_EVIDENCE", partial.scenarioCategory());
        assertEquals("PARTIAL", partial.evidenceStatus());
        assertTrue(partial.warnings().contains("stale or partial evidence must stay visible"));
        assertEquals("NO_HEALTHY_SERVER", unknown.scenarioCategory());
        assertEquals("UNKNOWN", unknown.evidenceStatus());
        assertTrue(unknown.unknowns()
                .contains("selected route is unavailable when no healthy server is returned"));
    }

    @Test
    void buildCatalogPreservesSourceReferencesAndBoundaryLanguage() {
        DecisionExplorerScenarioCatalogV1 catalog = service.buildCatalog();
        DecisionExplorerScenarioV1 first = catalog.scenarios().get(0);

        assertTrue(catalog.source().contains("AdaptiveRoutingExperimentFixtureCatalog"));
        assertTrue(first.sourceReferenceIds()
                .contains("AdaptiveRoutingExperimentFixtureCatalog:normal-balanced-load"));
        assertTrue(first.sourceReferenceIds().contains("POST /api/routing/decision-explorer"));
        assertTrue(catalog.notProvenBoundaries().contains("no production readiness"));
        assertTrue(catalog.notProvenBoundaries().contains("no storage proof"));
        assertTrue(catalog.notProvenBoundaries().contains("no evidence-packet generation"));
        assertTrue(catalog.boundaryNote().contains("does not run routing"));
    }

    @Test
    void boundaryLanguageDoesNotOverclaimScenarioApiEvidence() {
        String normalized = service.buildCatalog().toString().toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "read-only",
                "simulation-only",
                "no production readiness",
                "no live-cloud validation",
                "no real-tenant validation",
                "no benchmark/load/stress proof",
                "no throughput/p95/p99 proof",
                "no replay/export proof",
                "no storage proof",
                "no autonomous production action")) {
            assertTrue(normalized.contains(expected), "catalog should preserve " + expected);
        }

        for (String forbidden : List.of(
                "production readiness proven",
                "certification complete",
                "live-cloud validation complete",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "replay export is implemented",
                "autonomous production action enabled")) {
            assertFalse(normalized.contains(forbidden), "catalog must not overclaim " + forbidden);
        }
    }

    @Test
    void sourceDoesNotCallRoutingCalculationMutationStorageOrExternalSystems() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                + "DecisionExplorerScenarioCatalogService.java"), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "serverscorecalculator",
                "routingcomparisonservice",
                "cloudmanager",
                "httpclient",
                "urlconnection",
                "socket",
                "files.write",
                "system.getenv",
                "system.getproperty",
                "randomuuid",
                "instant.now")) {
            assertFalse(source.contains(forbidden), "scenario catalog API source must not use " + forbidden);
        }
    }

    private static DecisionExplorerScenarioV1 scenario(
            DecisionExplorerScenarioCatalogV1 catalog,
            String scenarioId) {
        return catalog.scenarios().stream()
                .filter(candidate -> candidate.scenarioId().equals(scenarioId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing scenario " + scenarioId));
    }
}
