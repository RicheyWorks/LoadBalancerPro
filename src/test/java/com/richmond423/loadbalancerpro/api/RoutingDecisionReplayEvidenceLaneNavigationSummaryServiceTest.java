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

class RoutingDecisionReplayEvidenceLaneNavigationSummaryServiceTest {
    private final RoutingDecisionReplayEvidenceLaneNavigationSummaryService service =
            new RoutingDecisionReplayEvidenceLaneNavigationSummaryService();

    @Test
    void navigationItemsAreDeterministicForEquivalentAlreadyBuiltEvidence() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse first = laneNavigation(result);
        RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse second = laneNavigation(result);

        assertEquals(first, second);
        assertTrue(first.readOnly());
        assertEquals("decision-replay-evidence-lane-navigation-summary/v1",
                first.laneNavigationSchemaVersion());
        assertTrue(List.of("AVAILABLE", "PARTIAL").contains(first.status()));
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", first.strategyId());
        assertEquals("green", first.selectedCandidateId());
        assertEquals(2, first.candidateCount());
        assertTrue(first.availableLaneCount() > 0);
        assertTrue(first.partialLaneCount() >= 0);
        assertEquals(0, first.unknownLaneCount());
        assertEquals(List.of(
                        "decision-vector-navigation",
                        "dominant-factor-analysis-navigation",
                        "decision-delta-analysis-navigation",
                        "replay-snapshot-navigation",
                        "reconstruction-trace-navigation",
                        "replay-capsule-navigation",
                        "readiness-checklist-navigation",
                        "evidence-source-map-navigation",
                        "evidence-boundary-summary-navigation",
                        "evidence-field-inventory-navigation",
                        "evidence-null-safety-navigation",
                        "evidence-status-rollup-navigation"),
                first.navigationItems().stream()
                        .map(DecisionReplayEvidenceLaneNavigationItemResponse::laneId)
                        .toList());
        assertEquals("results[].decisionVector", first.navigationItems().get(0).responseFieldPath());
        assertEquals("Decision Vector", first.navigationItems().get(0).uiSectionLabel());
        assertEquals("Enterprise Lab Decision Vector", first.navigationItems().get(0).docsReferenceLabel());
        assertTrue(first.navigationItems().get(0).readOnly());
        assertTrue(first.navigationItems().get(0).boundaryPresent());
        assertEquals("results[].decisionReplayEvidenceStatusRollup",
                first.navigationItems().get(11).responseFieldPath());
        assertEquals("Decision Evidence Status Rollup", first.navigationItems().get(11).uiSectionLabel());
        assertThrows(UnsupportedOperationException.class,
                () -> first.navigationItems().add(first.navigationItems().get(0)));
        assertTrue(first.explanation().contains("derived from already-built lab compare evidence only"));
        assertTrue(first.boundaryNote().contains("does not execute replay"));
        assertTrue(first.boundaryNote().contains("does not perform what-if mutation"));
        assertTrue(first.boundaryNote().contains("does not change routing behavior"));
        assertTrue(first.boundaryNote().contains("does not recompute scores"));
        assertTrue(first.productionNotProvenBoundary().contains("not production certification"));
        assertTrue(first.productionNotProvenBoundary().contains("not guaranteed replay"));
        assertFalse(first.toString().contains("laneNavigationFingerprint"));
    }

    @Test
    void partialEvidenceReturnsPartialWithoutInventingUnavailableValues() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse partial = service.laneNavigationSummary(
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
                null);

        assertEquals("PARTIAL", partial.status());
        assertEquals("green", partial.selectedCandidateId());
        assertEquals(2, partial.candidateCount());
        assertTrue(partial.partialLaneCount() >= 0);
        assertTrue(partial.unknownLaneCount() > 0);
        assertEquals("AVAILABLE", partial.navigationItems().get(0).status());
        assertEquals("AVAILABLE", partial.navigationItems().get(1).status());
        assertEquals("UNKNOWN", partial.navigationItems().get(2).status());
        assertEquals("results[].decisionDeltaAnalysis", partial.navigationItems().get(2).responseFieldPath());
        assertFalse(partial.toString().contains("laneNavigationFingerprint"));
    }

    @Test
    void missingEvidenceReturnsUnknownWithoutInventingNavigationDetails() {
        RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse summary = service.laneNavigationSummary(
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
                null);

        assertEquals("UNKNOWN", summary.status());
        assertNull(summary.selectedCandidateId());
        assertEquals(0, summary.candidateCount());
        assertEquals(0, summary.availableLaneCount());
        assertEquals(0, summary.partialLaneCount());
        assertEquals(12, summary.unknownLaneCount());
        assertEquals("UNKNOWN", summary.navigationItems().get(0).status());
        assertEquals("UNKNOWN", summary.navigationItems().get(11).status());
        assertFalse(summary.navigationItems().get(0).readOnly());
        assertFalse(summary.navigationItems().get(0).boundaryPresent());
        assertTrue(summary.explanation().contains("No replay execution"));
        assertTrue(summary.explanation().contains("guaranteed replay"));
        assertFalse(summary.explanation().contains("guaranteed replay is proven"));
        assertFalse(summary.explanation().contains("production certification is proven"));
    }

    @Test
    void noHealthyComparePathKeepsSafeUnknownNavigationWithoutInventedEvidence() {
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

        RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse summary =
                result.decisionReplayEvidenceLaneNavigationSummary();

        assertEquals("UNKNOWN", summary.status());
        assertNull(summary.selectedCandidateId());
        assertEquals(0, summary.candidateCount());
        assertEquals("UNKNOWN", summary.navigationItems().get(0).status());
        assertEquals("decision-vector-navigation", summary.navigationItems().get(0).laneId());
        assertEquals("evidence-status-rollup-navigation", summary.navigationItems().get(11).laneId());
        assertFalse(summary.navigationItems().get(0).readOnly());
        assertFalse(summary.navigationItems().get(0).boundaryPresent());
        assertTrue(summary.navigationItems().get(11).boundaryPresent());
        assertFalse(summary.toString().contains("laneNavigationFingerprint"));
        assertFalse(summary.toString().contains("quality ranking is proven"));
        assertFalse(summary.toString().contains("approval is granted"));
    }

    @Test
    void laneNavigationDoesNotUseScoringReflectionFingerprintPersistenceExportOrEnvironmentInputs()
            throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayEvidenceLaneNavigationSummaryService.java"),
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
        assertFalse(normalized.contains("lanenavigationfingerprint"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("quality ranking is proven"));
        assertFalse(normalized.contains("approval is granted"));
        assertTrue(source.contains("does not inspect raw server input"));
        assertTrue(source.contains("does not inspect raw"));
        assertTrue(source.contains("request payloads"));
        assertTrue(source.contains("does not recompute scores"));
        assertTrue(source.contains("not production certification"));
        assertTrue(source.contains("not guaranteed replay"));
    }

    private static RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse laneNavigation(
            RoutingComparisonResultResponse result) {
        return new RoutingDecisionReplayEvidenceLaneNavigationSummaryService().laneNavigationSummary(
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
                result.decisionReplayEvidenceStatusRollup());
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
