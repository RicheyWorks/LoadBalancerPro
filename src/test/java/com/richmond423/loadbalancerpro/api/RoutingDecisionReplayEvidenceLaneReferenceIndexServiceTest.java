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

class RoutingDecisionReplayEvidenceLaneReferenceIndexServiceTest {
    private final RoutingDecisionReplayEvidenceLaneReferenceIndexService service =
            new RoutingDecisionReplayEvidenceLaneReferenceIndexService();

    @Test
    void referenceItemsAreDeterministicForEquivalentAlreadyBuiltEvidence() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceLaneReferenceIndexResponse first = laneReferenceIndex(result);
        RoutingDecisionReplayEvidenceLaneReferenceIndexResponse second = laneReferenceIndex(result);

        assertEquals(first, second);
        assertTrue(first.readOnly());
        assertEquals("decision-replay-evidence-lane-reference-index/v1",
                first.laneReferenceIndexSchemaVersion());
        assertTrue(List.of("AVAILABLE", "PARTIAL").contains(first.status()));
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", first.strategyId());
        assertEquals("green", first.selectedCandidateId());
        assertEquals(2, first.candidateCount());
        assertTrue(first.availableLaneCount() > 0);
        assertTrue(first.partialLaneCount() >= 0);
        assertEquals(0, first.unknownLaneCount());
        assertEquals(expectedLaneIds(), first.referenceItems().stream()
                .map(DecisionReplayEvidenceLaneReferenceIndexItemResponse::laneId)
                .toList());
        assertEquals("Decision Vector reference", first.referenceItems().get(0).label());
        assertEquals("Decision Replay Evidence Lane Dependency Map reference",
                first.referenceItems().get(13).label());
        assertEquals("results[].decisionVector", first.referenceItems().get(0).responseFieldPath());
        assertEquals("results[].decisionReplayEvidenceLaneDependencyMap",
                first.referenceItems().get(13).responseFieldPath());
        assertEquals("Decision Vector", first.referenceItems().get(0).uiSectionLabel());
        assertEquals("Enterprise Lab Decision Vector", first.referenceItems().get(0).docsReferenceLabel());
        assertEquals("Decision Evidence Lane Dependency Map", first.referenceItems().get(13).uiSectionLabel());
        assertEquals("Decision Replay Evidence Lane Dependency Map",
                first.referenceItems().get(13).docsReferenceLabel());
        assertEquals(0, first.referenceItems().get(0).dependencyCount());
        assertEquals(12, first.referenceItems().get(0).downstreamCount());
        assertEquals(12, first.referenceItems().get(12).dependencyCount());
        assertEquals(0, first.referenceItems().get(12).downstreamCount());
        assertEquals(13, first.referenceItems().get(13).dependencyCount());
        assertEquals(0, first.referenceItems().get(13).downstreamCount());
        assertTrue(first.referenceItems().get(0).readOnly());
        assertTrue(first.referenceItems().get(0).boundaryPresent());
        assertTrue(first.referenceItems().get(13).readOnly());
        assertTrue(first.referenceItems().get(13).boundaryPresent());
        assertThrows(UnsupportedOperationException.class,
                () -> first.referenceItems().add(first.referenceItems().get(0)));
        assertTrue(first.explanation().contains("derived from already-built lab compare evidence only"));
        assertTrue(first.boundaryNote().contains("does not execute replay"));
        assertTrue(first.boundaryNote().contains("does not perform what-if mutation"));
        assertTrue(first.boundaryNote().contains("does not change routing behavior"));
        assertTrue(first.boundaryNote().contains("does not recompute scores"));
        assertTrue(first.boundaryNote().contains("does not inspect raw server input"));
        assertTrue(first.boundaryNote().contains("does not inspect raw request payloads"));
        assertTrue(first.productionNotProvenBoundary().contains("not production certification"));
        assertTrue(first.productionNotProvenBoundary().contains("not guaranteed replay"));
        assertFalse(first.toString().contains("laneReferenceIndexFingerprint"));
    }

    @Test
    void partialEvidenceReturnsPartialWithoutInventingUnavailableValues() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceLaneReferenceIndexResponse partial = service.laneReferenceIndex(
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
                null,
                null);

        assertEquals("PARTIAL", partial.status());
        assertEquals("green", partial.selectedCandidateId());
        assertEquals(2, partial.candidateCount());
        assertTrue(partial.partialLaneCount() >= 0);
        assertEquals(12, partial.unknownLaneCount());
        assertEquals("AVAILABLE", partial.referenceItems().get(0).status());
        assertEquals("AVAILABLE", partial.referenceItems().get(1).status());
        assertEquals("UNKNOWN", partial.referenceItems().get(2).status());
        assertEquals(0, partial.referenceItems().get(2).dependencyCount());
        assertEquals(0, partial.referenceItems().get(2).downstreamCount());
        assertEquals("results[].decisionDeltaAnalysis", partial.referenceItems().get(2).responseFieldPath());
        assertFalse(partial.toString().contains("laneReferenceIndexFingerprint"));
    }

    @Test
    void missingEvidenceReturnsUnknownWithoutInventingReferenceDetails() {
        RoutingDecisionReplayEvidenceLaneReferenceIndexResponse summary = service.laneReferenceIndex(
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
                null,
                null);

        assertEquals("UNKNOWN", summary.status());
        assertNull(summary.selectedCandidateId());
        assertEquals(0, summary.candidateCount());
        assertEquals(0, summary.availableLaneCount());
        assertEquals(0, summary.partialLaneCount());
        assertEquals(14, summary.unknownLaneCount());
        assertEquals("UNKNOWN", summary.referenceItems().get(0).status());
        assertEquals("UNKNOWN", summary.referenceItems().get(13).status());
        assertFalse(summary.referenceItems().get(0).readOnly());
        assertFalse(summary.referenceItems().get(0).boundaryPresent());
        assertEquals(0, summary.referenceItems().get(13).dependencyCount());
        assertEquals(0, summary.referenceItems().get(13).downstreamCount());
        assertTrue(summary.explanation().contains("No selected candidate"));
        assertTrue(summary.explanation().contains("guaranteed replay"));
        assertFalse(summary.explanation().contains("guaranteed replay is proven"));
        assertFalse(summary.explanation().contains("production certification is proven"));
        assertFalse(summary.toString().contains("laneReferenceIndexFingerprint"));
    }

    @Test
    void noHealthyComparePathKeepsSafeUnknownReferenceIndexWithoutInventedEvidence() {
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

        RoutingDecisionReplayEvidenceLaneReferenceIndexResponse summary =
                result.decisionReplayEvidenceLaneReferenceIndex();

        assertEquals("UNKNOWN", summary.status());
        assertNull(summary.selectedCandidateId());
        assertEquals(0, summary.candidateCount());
        assertEquals("UNKNOWN", summary.referenceItems().get(0).status());
        assertEquals("decision-vector-reference", summary.referenceItems().get(0).laneId());
        assertEquals("evidence-lane-dependency-map-reference", summary.referenceItems().get(13).laneId());
        assertFalse(summary.referenceItems().get(0).readOnly());
        assertFalse(summary.referenceItems().get(0).boundaryPresent());
        assertTrue(summary.referenceItems().get(13).readOnly());
        assertTrue(summary.referenceItems().get(13).boundaryPresent());
        assertEquals(13, summary.referenceItems().get(13).dependencyCount());
        assertEquals(0, summary.referenceItems().get(13).downstreamCount());
        String text = summary.toString().toLowerCase(Locale.ROOT);
        assertFalse(text.contains("lanereferenceindexfingerprint"));
        assertFalse(text.contains("candidate set is invented"));
        assertFalse(text.contains("score gap is invented"));
        assertFalse(text.contains("largest delta factor is invented"));
        assertFalse(text.contains("quality ranking is proven"));
        assertFalse(text.contains("approval is granted"));
        assertFalse(text.contains("correctness validation is proven"));
    }

    @Test
    void laneReferenceIndexDoesNotUseScoringReflectionFingerprintPersistenceExportOrEnvironmentInputs()
            throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayEvidenceLaneReferenceIndexService.java"),
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
        assertFalse(normalized.contains("lanereferenceindexfingerprint"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("quality ranking is proven"));
        assertFalse(normalized.contains("approval is granted"));
        assertFalse(normalized.contains("correctness validation is proven"));
        assertTrue(source.contains("does not inspect raw server input"));
        assertTrue(source.contains("does not inspect raw request payloads"));
        assertTrue(source.contains("does not recompute scores"));
        assertTrue(source.contains("does not infer hidden scoring"));
        assertTrue(source.contains("does not retune weights"));
        assertTrue(source.contains("does not change routing behavior"));
        assertTrue(source.contains("not production certification"));
        assertTrue(source.contains("not guaranteed replay"));
    }

    private static RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex(
            RoutingComparisonResultResponse result) {
        return new RoutingDecisionReplayEvidenceLaneReferenceIndexService().laneReferenceIndex(
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
                result.decisionReplayEvidenceLaneNavigationSummary(),
                result.decisionReplayEvidenceLaneDependencyMap());
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

    private static List<String> expectedLaneIds() {
        return List.of(
                "decision-vector-reference",
                "dominant-factor-analysis-reference",
                "decision-delta-analysis-reference",
                "replay-snapshot-reference",
                "reconstruction-trace-reference",
                "replay-capsule-reference",
                "readiness-checklist-reference",
                "evidence-source-map-reference",
                "evidence-boundary-summary-reference",
                "evidence-field-inventory-reference",
                "evidence-null-safety-reference",
                "evidence-status-rollup-reference",
                "evidence-lane-navigation-reference",
                "evidence-lane-dependency-map-reference");
    }
}
