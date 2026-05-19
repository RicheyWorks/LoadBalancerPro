package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class RoutingDecisionReplayEvidenceLaneDependencyMapServiceTest {
    private final RoutingDecisionReplayEvidenceLaneDependencyMapService service =
            new RoutingDecisionReplayEvidenceLaneDependencyMapService();

    @Test
    void dependencyItemsAreDeterministicForEquivalentAlreadyBuiltEvidence() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceLaneDependencyMapResponse first = laneDependencyMap(result);
        RoutingDecisionReplayEvidenceLaneDependencyMapResponse second = laneDependencyMap(result);

        assertEquals(first, second);
        assertTrue(first.readOnly());
        assertEquals("decision-replay-evidence-lane-dependency-map/v1",
                first.laneDependencyMapSchemaVersion());
        assertTrue(List.of("AVAILABLE", "PARTIAL").contains(first.status()));
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", first.strategyId());
        assertEquals("green", first.selectedCandidateId());
        assertEquals(2, first.candidateCount());
        assertTrue(first.availableLaneCount() > 0);
        assertTrue(first.partialLaneCount() >= 0);
        assertEquals(0, first.unknownLaneCount());
        assertEquals(List.of(
                        "decision-vector-dependency",
                        "dominant-factor-analysis-dependency",
                        "decision-delta-analysis-dependency",
                        "replay-snapshot-dependency",
                        "reconstruction-trace-dependency",
                        "replay-capsule-dependency",
                        "readiness-checklist-dependency",
                        "evidence-source-map-dependency",
                        "evidence-boundary-summary-dependency",
                        "evidence-field-inventory-dependency",
                        "evidence-null-safety-dependency",
                        "evidence-status-rollup-dependency",
                        "evidence-lane-navigation-dependency"),
                first.dependencyItems().stream()
                        .map(DecisionReplayEvidenceLaneDependencyItemResponse::laneId)
                        .toList());
        assertEquals("results[].decisionVector", first.dependencyItems().get(0).responseFieldPath());
        assertEquals(List.of(), first.dependencyItems().get(0).dependsOnLaneIds());
        assertEquals(0, first.dependencyItems().get(0).dependencyCount());
        assertEquals(12, first.dependencyItems().get(0).downstreamCount());
        assertEquals("dominant-factor-analysis", first.dependencyItems().get(0).downstreamLaneIds().get(0));
        assertEquals(List.of("decision-vector"), first.dependencyItems().get(1).dependsOnLaneIds());
        assertEquals(1, first.dependencyItems().get(1).dependencyCount());
        assertEquals(List.of(
                        "decision-vector",
                        "dominant-factor-analysis",
                        "decision-delta-analysis",
                        "replay-snapshot",
                        "reconstruction-trace",
                        "replay-capsule",
                        "readiness-checklist",
                        "evidence-source-map",
                        "evidence-boundary-summary",
                        "evidence-field-inventory",
                        "evidence-null-safety",
                        "evidence-status-rollup"),
                first.dependencyItems().get(12).dependsOnLaneIds());
        assertEquals(12, first.dependencyItems().get(12).dependencyCount());
        assertEquals(0, first.dependencyItems().get(12).downstreamCount());
        assertTrue(first.dependencyItems().get(0).readOnly());
        assertTrue(first.dependencyItems().get(0).boundaryPresent());
        assertThrows(UnsupportedOperationException.class,
                () -> first.dependencyItems().add(first.dependencyItems().get(0)));
        assertThrows(UnsupportedOperationException.class,
                () -> first.dependencyItems().get(0).downstreamLaneIds().add("other"));
        assertTrue(first.explanation().contains("derived from already-built lab compare evidence only"));
        assertTrue(first.boundaryNote().contains("does not execute replay"));
        assertTrue(first.boundaryNote().contains("does not perform what-if mutation"));
        assertTrue(first.boundaryNote().contains("does not change routing behavior"));
        assertTrue(first.boundaryNote().contains("does not recompute scores"));
        assertTrue(first.productionNotProvenBoundary().contains("not production certification"));
        assertTrue(first.productionNotProvenBoundary().contains("not guaranteed replay"));
        assertFalse(first.toString().contains("laneDependencyFingerprint"));
    }

    @Test
    void partialEvidenceReturnsPartialWithoutInventingUnavailableValues() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceLaneDependencyMapResponse partial = service.laneDependencyMap(
                "TAIL_LATENCY_POWER_OF_TWO",
                result.decisionVector(),
                result.dominantFactorAnalysis(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertEquals("PARTIAL", partial.status());
        assertEquals("green", partial.selectedCandidateId());
        assertEquals(2, partial.candidateCount());
        assertTrue(partial.partialLaneCount() >= 0);
        assertTrue(partial.unknownLaneCount() > 0);
        assertEquals("AVAILABLE", partial.dependencyItems().get(0).status());
        assertEquals("AVAILABLE", partial.dependencyItems().get(1).status());
        assertEquals("UNKNOWN", partial.dependencyItems().get(2).status());
        assertEquals(List.of("decision-vector"), partial.dependencyItems().get(2).dependsOnLaneIds());
        assertEquals(1, partial.dependencyItems().get(2).dependencyCount());
        assertFalse(partial.toString().contains("laneDependencyFingerprint"));
    }

    @Test
    void missingEvidenceReturnsUnknownWithoutInventingDependencyDetails() {
        RoutingDecisionReplayEvidenceLaneDependencyMapResponse summary = service.laneDependencyMap(
                "TAIL_LATENCY_POWER_OF_TWO",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertEquals("UNKNOWN", summary.status());
        assertNull(summary.selectedCandidateId());
        assertEquals(0, summary.candidateCount());
        assertEquals(0, summary.availableLaneCount());
        assertEquals(0, summary.partialLaneCount());
        assertEquals(13, summary.unknownLaneCount());
        assertEquals("UNKNOWN", summary.dependencyItems().get(0).status());
        assertEquals("UNKNOWN", summary.dependencyItems().get(12).status());
        assertFalse(summary.dependencyItems().get(0).readOnly());
        assertFalse(summary.dependencyItems().get(0).boundaryPresent());
        assertEquals(12, summary.dependencyItems().get(12).dependencyCount());
        assertEquals(0, summary.dependencyItems().get(12).downstreamCount());
        assertTrue(summary.explanation().contains("No replay execution"));
        assertTrue(summary.explanation().contains("guaranteed replay"));
        assertFalse(summary.explanation().contains("guaranteed replay is proven"));
        assertFalse(summary.explanation().contains("production certification is proven"));
    }

    @Test
    void noHealthyComparePathKeepsSafeUnknownDependencyMapWithoutInventedEvidence() {
        RoutingComparisonResultResponse result = new RoutingComparisonService().compare(new RoutingComparisonRequest(
                List.of("TAIL_LATENCY_POWER_OF_TWO"),
                List.of(new RoutingServerStateInput(
                        "green",
                        false,
                        1,
                        null,
                        null,
                        null,
                        10.0,
                        20.0,
                        30.0,
                        0.0,
                        null,
                        null))))
                .results()
                .get(0);

        RoutingDecisionReplayEvidenceLaneDependencyMapResponse summary =
                result.decisionReplayEvidenceLaneDependencyMap();

        assertEquals("UNKNOWN", summary.status());
        assertNull(summary.selectedCandidateId());
        assertEquals(0, summary.candidateCount());
        assertEquals("UNKNOWN", summary.dependencyItems().get(0).status());
        assertEquals("decision-vector-dependency", summary.dependencyItems().get(0).laneId());
        assertEquals("evidence-lane-navigation-dependency", summary.dependencyItems().get(12).laneId());
        assertFalse(summary.dependencyItems().get(0).readOnly());
        assertFalse(summary.dependencyItems().get(0).boundaryPresent());
        assertTrue(summary.dependencyItems().get(12).boundaryPresent());
        assertEquals(12, summary.dependencyItems().get(12).dependencyCount());
        assertEquals(0, summary.dependencyItems().get(12).downstreamCount());
        assertFalse(summary.toString().contains("laneDependencyFingerprint"));
        assertFalse(summary.toString().contains("quality ranking is proven"));
        assertFalse(summary.toString().contains("approval is granted"));
        assertFalse(summary.toString().contains("correctness validation is proven"));
    }

    @Test
    void laneDependencyMapDoesNotUseScoringReflectionFingerprintPersistenceExportOrEnvironmentInputs()
            throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayEvidenceLaneDependencyMapService.java"),
                StandardCharsets.UTF_8);
        String normalized = source.toLowerCase(Locale.ROOT);

        assertFalse(source.contains("ServerScoreCalculator"));
        assertFalse(source.contains("ServerStateVector"));
        assertFalse(source.contains("MessageDigest"));
        assertFalse(source.contains("SHA-256"));
        assertFalse(normalized.contains("sha256"));
        assertFalse(normalized.contains("instant.now"));
        assertFalse(normalized.contains("system.getenv"));
        assertFalse(normalized.contains("system.getproperty"));
        assertFalse(normalized.contains("randomuuid"));
        assertFalse(normalized.contains("hostname"));
        assertFalse(normalized.contains("files.write"));
        assertFalse(normalized.contains("@postmapping"));
        assertFalse(normalized.contains("@getmapping"));
        assertFalse(normalized.contains("zipoutputstream"));
        assertFalse(normalized.contains("processbuilder"));
        assertFalse(normalized.contains("runtime.getruntime"));
        assertFalse(source.contains(".getDeclared"));
        assertFalse(source.contains(".getFields"));
        assertFalse(source.contains(".getMethods"));
        assertFalse(normalized.contains("executed replay"));
        assertFalse(normalized.contains("what-if mutation is performed"));
        assertFalse(normalized.contains("lanedependencyfingerprint"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("quality ranking is proven"));
        assertFalse(normalized.contains("approval is granted"));
        assertFalse(normalized.contains("correctness validation is proven"));
        assertTrue(source.contains("does not inspect raw server input"));
        assertTrue(source.contains("does not inspect raw"));
        assertTrue(source.contains("request payloads"));
        assertTrue(source.contains("does not recompute scores"));
        assertTrue(source.contains("not production certification"));
        assertTrue(source.contains("not guaranteed replay"));
    }

    private static RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap(
            RoutingComparisonResultResponse result) {
        return new RoutingDecisionReplayEvidenceLaneDependencyMapService().laneDependencyMap(
                result.strategyId(),
                result.decisionVector(),
                result.dominantFactorAnalysis(),
                result.decisionDeltaAnalysis(),
                result.decisionReplaySnapshot(),
                result.decisionReplayReconstructionTrace(),
                result.decisionReplayCapsule(),
                result.decisionReplayReadinessChecklist(),
                result.decisionReplayEvidenceSourceMap(),
                result.decisionReplayEvidenceBoundarySummary(),
                result.decisionReplayEvidenceFieldInventory(),
                result.decisionReplayEvidenceNullSafetySummary(),
                result.decisionReplayEvidenceStatusRollup(),
                result.decisionReplayEvidenceLaneNavigationSummary());
    }

    private static RoutingComparisonResultResponse healthyCompareResult() {
        return new RoutingComparisonService().compare(new RoutingComparisonRequest(
                List.of("TAIL_LATENCY_POWER_OF_TWO"),
                List.of(
                        new RoutingServerStateInput(
                                "green",
                                true,
                                5,
                                100.0,
                                100.0,
                                null,
                                20.0,
                                40.0,
                                80.0,
                                0.01,
                                1,
                                null),
                        new RoutingServerStateInput(
                                "blue",
                                true,
                                75,
                                100.0,
                                100.0,
                                null,
                                35.0,
                                120.0,
                                220.0,
                                0.15,
                                10,
                                null))))
                .results()
                .get(0);
    }
}
