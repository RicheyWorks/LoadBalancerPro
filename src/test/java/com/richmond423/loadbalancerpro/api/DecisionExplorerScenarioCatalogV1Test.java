package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class DecisionExplorerScenarioCatalogV1Test {

    @Test
    void catalogForcesReadOnlySimulationOnlyAndStableVocabulary() {
        DecisionExplorerScenarioCatalogV1 catalog = new DecisionExplorerScenarioCatalogV1(
                false,
                false,
                null,
                null,
                "AdaptiveRoutingExperimentFixtureCatalog",
                List.of(healthyBaseline()),
                List.of(),
                List.of(),
                notProvenBoundaries(),
                "scenario catalog model is read-only");

        assertTrue(catalog.readOnly());
        assertTrue(catalog.simulationOnly());
        assertEquals("DecisionExplorerScenarioCatalogV1", catalog.payloadObject());
        assertEquals("v1", catalog.contractVersion());
        assertEquals("DecisionExplorerScenarioV1", catalog.scenarios().get(0).scenarioObject());
    }

    @Test
    void catalogSortsScenariosByDisplayOrderThenScenarioId() {
        DecisionExplorerScenarioCatalogV1 first = new DecisionExplorerScenarioCatalogV1(
                true,
                true,
                null,
                null,
                "fixtures",
                List.of(noHealthyUnknown(), partialEvidence(), healthyBaseline()),
                List.of(),
                List.of(),
                notProvenBoundaries(),
                "read-only");
        DecisionExplorerScenarioCatalogV1 second = new DecisionExplorerScenarioCatalogV1(
                true,
                true,
                null,
                null,
                "fixtures",
                List.of(partialEvidence(), healthyBaseline(), noHealthyUnknown()),
                List.of(),
                List.of(),
                notProvenBoundaries(),
                "read-only");

        assertEquals(first, second);
        assertEquals(List.of("normal-balanced-load", "stale-signal", "all-unhealthy-degradation"),
                first.scenarios().stream().map(DecisionExplorerScenarioV1::scenarioId).toList());
    }

    @Test
    void scenarioModelCoversRequiredPhaseTwoReviewCases() {
        DecisionExplorerScenarioCatalogV1 catalog = new DecisionExplorerScenarioCatalogV1(
                true,
                true,
                null,
                null,
                "AdaptiveRoutingExperimentFixtureCatalog local synthetic fixtures",
                List.of(noHealthyUnknown(), partialEvidence(), healthyBaseline()),
                List.of("catalog is a model slice only; API exposure follows in a later PR"),
                List.of("hidden routing internals"),
                notProvenBoundaries(),
                "No runtime endpoint or UI behavior is changed by the model.");

        assertEquals(List.of("HEALTHY_BASELINE", "PARTIAL_EVIDENCE", "NO_HEALTHY_SERVER"),
                catalog.scenarios().stream().map(DecisionExplorerScenarioV1::scenarioCategory).toList());
        assertEquals(List.of("AVAILABLE", "PARTIAL", "UNKNOWN"),
                catalog.scenarios().stream().map(DecisionExplorerScenarioV1::evidenceStatus).toList());
        assertTrue(catalog.scenarios().get(0).sourceReferenceIds()
                .contains("AdaptiveRoutingExperimentFixtureCatalog:normal-balanced-load"));
        assertTrue(catalog.scenarios().get(1).warnings()
                .contains("stale or partial evidence must stay visible"));
        assertTrue(catalog.scenarios().get(2).unknowns()
                .contains("selected route is unavailable when no healthy server is returned"));
    }

    @Test
    void collectionsAreCopiedAndRemainUnmodifiable() {
        List<DecisionExplorerScenarioV1> scenarios = new ArrayList<>();
        scenarios.add(healthyBaseline());
        List<String> warnings = new ArrayList<>();
        warnings.add("first warning");

        DecisionExplorerScenarioCatalogV1 catalog = new DecisionExplorerScenarioCatalogV1(
                true,
                true,
                null,
                null,
                "fixtures",
                scenarios,
                warnings,
                List.of(),
                notProvenBoundaries(),
                "read-only");

        scenarios.add(partialEvidence());
        warnings.add("late warning");

        assertEquals(1, catalog.scenarios().size());
        assertEquals(List.of("first warning"), catalog.warnings());
        assertThrows(UnsupportedOperationException.class, () -> catalog.scenarios().add(partialEvidence()));
        assertThrows(UnsupportedOperationException.class, () -> catalog.warnings().add("mutated"));
    }

    @Test
    void nullScenarioFieldsNormalizeWithoutInventingEvidence() {
        DecisionExplorerScenarioV1 scenario = new DecisionExplorerScenarioV1(
                null,
                null,
                null,
                null,
                null,
                -1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertEquals("DecisionExplorerScenarioV1", scenario.scenarioObject());
        assertEquals("UNKNOWN", scenario.scenarioId());
        assertEquals(0, scenario.displayOrder());
        assertTrue(scenario.sourceReferenceIds().isEmpty());
        assertTrue(scenario.unknowns().isEmpty());
        assertEquals("UNKNOWN", scenario.boundaryNote());
    }

    @Test
    void boundaryLanguageDoesNotOverclaimScenarioCatalogEvidence() {
        String normalized = new DecisionExplorerScenarioCatalogV1(
                        true,
                        true,
                        null,
                        null,
                        "fixtures",
                        List.of(healthyBaseline(), partialEvidence(), noHealthyUnknown()),
                        List.of("model only"),
                        List.of("hidden routing internals"),
                        notProvenBoundaries(),
                        "read-only and simulation-only catalog model")
                .toString()
                .toLowerCase(Locale.ROOT);

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
    void sourceDoesNotAddSideEffectsOrRoutingMutation() throws Exception {
        String catalogSource = Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                + "DecisionExplorerScenarioCatalogV1.java"), StandardCharsets.UTF_8);
        String scenarioSource = Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                + "DecisionExplorerScenarioV1.java"), StandardCharsets.UTF_8);
        String combined = (catalogSource + "\n" + scenarioSource).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "serverstatevector",
                "serverscorecalculator",
                "core.loadbalancer",
                "new loadbalancer(",
                "routingcontroller",
                "httpclient",
                "urlconnection",
                "socket",
                "files.write",
                "system.getenv",
                "system.getproperty",
                "randomuuid",
                "instant.now")) {
            assertFalse(combined.contains(forbidden), "model source must not use " + forbidden);
        }
    }

    private static DecisionExplorerScenarioV1 healthyBaseline() {
        return scenario(
                "normal-balanced-load",
                "Healthy baseline",
                "HEALTHY_BASELINE",
                "AVAILABLE",
                10,
                "Balanced healthy servers with source-visible synthetic fixture evidence.",
                "normal-balanced-load",
                List.of("AdaptiveRoutingExperimentFixtureCatalog:normal-balanced-load",
                        "DecisionExplorerPayloadV1"),
                List.of("Which route would the reviewer inspect first?"),
                List.of("healthy-baseline", "deterministic-evidence"),
                List.of(),
                List.of(),
                "healthy baseline scenario remains read-only and simulation-only");
    }

    private static DecisionExplorerScenarioV1 partialEvidence() {
        return scenario(
                "stale-signal",
                "Partial evidence",
                "PARTIAL_EVIDENCE",
                "PARTIAL",
                20,
                "Stale signal fixture keeps partial evidence visible to reviewers.",
                "stale-signal",
                List.of("AdaptiveRoutingExperimentFixtureCatalog:stale-signal",
                        "DecisionExplorerPayloadV1.warnings"),
                List.of("Which evidence is stale or incomplete?"),
                List.of("partial-evidence", "warning"),
                List.of("stale or partial evidence must stay visible"),
                List.of("freshness of hidden routing internals"),
                "partial evidence scenario does not invent missing signals");
    }

    private static DecisionExplorerScenarioV1 noHealthyUnknown() {
        return scenario(
                "all-unhealthy-degradation",
                "No healthy server",
                "NO_HEALTHY_SERVER",
                "UNKNOWN",
                30,
                "All unhealthy fixture keeps the no-selection path visible.",
                "all-unhealthy-degradation",
                List.of("AdaptiveRoutingExperimentFixtureCatalog:all-unhealthy-degradation",
                        "DecisionExplorerPayloadV1.unknowns"),
                List.of("What remains unknown when no healthy route is available?"),
                List.of("unknown", "no-healthy-server"),
                List.of("no healthy selected route is invented"),
                List.of("selected route is unavailable when no healthy server is returned"),
                "unknown scenario preserves no-live-mutation boundaries");
    }

    private static DecisionExplorerScenarioV1 scenario(
            String scenarioId,
            String scenarioLabel,
            String scenarioCategory,
            String evidenceStatus,
            int displayOrder,
            String description,
            String requestPresetId,
            List<String> sourceReferenceIds,
            List<String> expectedReviewerQuestions,
            List<String> tags,
            List<String> warnings,
            List<String> unknowns,
            String boundaryNote) {
        return new DecisionExplorerScenarioV1(
                null,
                scenarioId,
                scenarioLabel,
                scenarioCategory,
                evidenceStatus,
                displayOrder,
                description,
                requestPresetId,
                sourceReferenceIds,
                expectedReviewerQuestions,
                tags,
                warnings,
                unknowns,
                notProvenBoundaries(),
                boundaryNote);
    }

    private static List<String> notProvenBoundaries() {
        return List.of(
                "no production readiness",
                "no production certification",
                "no live-cloud validation",
                "no real-tenant validation",
                "no benchmark/load/stress proof",
                "no throughput/p95/p99 proof",
                "no replay/export proof",
                "no storage proof",
                "no evidence-packet generation",
                "no autonomous production action");
    }
}
