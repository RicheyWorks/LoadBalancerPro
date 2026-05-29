package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerShadowCandidateOutcomeBuilderTest {
    private static final String BOUNDARY_NOTE = "local-only shadow decision-quality evaluation";
    private final DecisionExplorerShadowCandidateOutcomeBuilder builder =
            new DecisionExplorerShadowCandidateOutcomeBuilder();

    @Test
    void buildsDeterministicSelectedFirstOrdering() {
        List<DecisionExplorerShadowCandidateOutcomeV1> outcomes = builder.build(List.of(
                row("edge-c", false, 3, DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_TRAILS_SELECTED,
                        DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_SELECTED_ADVANTAGE,
                        DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                        DecisionExplorerCandidateDiagnosticV1.RISK_LOW, 16.0, 6.0),
                row("edge-b", false, 2, DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_CLOSE,
                        DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_TRADEOFF,
                        DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                        DecisionExplorerCandidateDiagnosticV1.RISK_REVIEW, 10.5, 0.5),
                row("edge-a", true, 99, DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE,
                        DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE,
                        DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                        DecisionExplorerCandidateDiagnosticV1.RISK_LOW, 10.0, 0.0)),
                BOUNDARY_NOTE);

        assertEquals(List.of("edge-a", "edge-b", "edge-c"), outcomes.stream()
                .map(DecisionExplorerShadowCandidateOutcomeV1::candidateId)
                .toList());
        assertEquals(DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SELECTED_BASELINE,
                outcomes.get(0).outcomeLabel());
        assertEquals(DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_CLOSE_CALL,
                outcomes.get(1).outcomeLabel());
        assertEquals(DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_ACCEPTABLE_ALTERNATIVE,
                outcomes.get(2).outcomeLabel());
    }

    @Test
    void mapsSelectedDegradedEvidenceToRiskSignal() {
        DecisionExplorerShadowCandidateOutcomeV1 outcome = builder.build(List.of(
                row("edge-a", true, 1, DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE,
                        DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE,
                        DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED,
                        DecisionExplorerCandidateDiagnosticV1.RISK_HIGH, 10.0, 0.0,
                        List.of("selected candidate evidence degraded"), List.of())),
                BOUNDARY_NOTE).get(0);

        assertEquals(DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_DEGRADED_SELECTED, outcome.outcomeLabel());
        assertEquals(DecisionExplorerShadowCandidateOutcomeV1.IMPACT_RISK_SIGNAL, outcome.qualityImpact());
        assertTrue(outcome.summaryText().contains("local shadow quality risk signal"));
        assertTrue(outcome.reasonCodes().contains("SHADOW_CANDIDATE_OUTCOME_DEGRADED_SELECTED"));
        assertTrue(outcome.reasonCodes().contains("SHADOW_CANDIDATE_IMPACT_RISK_SIGNAL"));
    }

    @Test
    void mapsAlternativeTradeoffsToReviewUnknownAndSupportSignals() {
        List<DecisionExplorerShadowCandidateOutcomeV1> outcomes = builder.build(List.of(
                row("edge-a", false, 1, DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_BEATS_SELECTED,
                        DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_RISK,
                        DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                        DecisionExplorerCandidateDiagnosticV1.RISK_LOW, 8.0, -2.0),
                row("edge-b", false, 2, DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_UNKNOWN,
                        DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_UNKNOWN,
                        DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                        DecisionExplorerCandidateDiagnosticV1.RISK_UNKNOWN, null, null),
                row("edge-c", false, 3, DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_TRAILS_SELECTED,
                        DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_SELECTED_ADVANTAGE,
                        DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                        DecisionExplorerCandidateDiagnosticV1.RISK_LOW, 15.0, 5.0)),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SAFER_ALTERNATIVE,
                outcomes.get(0).outcomeLabel());
        assertEquals(DecisionExplorerShadowCandidateOutcomeV1.IMPACT_REVIEW_SIGNAL,
                outcomes.get(0).qualityImpact());
        assertTrue(outcomes.get(0).summaryText().contains("returned score delta of -2.0"));

        assertEquals(DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_UNKNOWN_ALTERNATIVE,
                outcomes.get(1).outcomeLabel());
        assertEquals(DecisionExplorerShadowCandidateOutcomeV1.IMPACT_UNKNOWN,
                outcomes.get(1).qualityImpact());
        assertEquals("UNKNOWN_GAP", outcomes.get(1).scoreGapCategory());

        assertEquals(DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_ACCEPTABLE_ALTERNATIVE,
                outcomes.get(2).outcomeLabel());
        assertEquals(DecisionExplorerShadowCandidateOutcomeV1.IMPACT_SUPPORTS_DECISION,
                outcomes.get(2).qualityImpact());
    }

    @Test
    void nullRowsFallbackToEmptyOutcomeList() {
        assertTrue(builder.build(null, BOUNDARY_NOTE).isEmpty());
    }

    private static DecisionExplorerRouteTradeoffRowV1 row(
            String candidateId,
            boolean selected,
            int displayOrder,
            String tradeoffCategory,
            String classification,
            String status,
            String riskLevel,
            Double finalScore,
            Double scoreDelta) {
        return row(candidateId, selected, displayOrder, tradeoffCategory, classification, status, riskLevel,
                finalScore, scoreDelta, List.of(), List.of());
    }

    private static DecisionExplorerRouteTradeoffRowV1 row(
            String candidateId,
            boolean selected,
            int displayOrder,
            String tradeoffCategory,
            String classification,
            String status,
            String riskLevel,
            Double finalScore,
            Double scoreDelta,
            List<String> degradedSignals,
            List<String> unknownSignals) {
        return new DecisionExplorerRouteTradeoffRowV1(
                candidateId,
                candidateId,
                selected,
                displayOrder,
                tradeoffCategory,
                classification,
                status,
                riskLevel,
                DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(status) ? "DEGRADED" : "HEALTHY",
                finalScore,
                scoreDelta,
                DecisionExplorerRouteTradeoffRowV1.scoreGapCategoryFor(selected, scoreDelta),
                "scoring explanation",
                "evidence summary",
                List.of("benefit signal"),
                DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_RISK.equals(classification)
                        ? List.of("risk signal")
                        : List.of(),
                unknownSignals,
                degradedSignals,
                List.of("TRADEOFF_CATEGORY_" + tradeoffCategory),
                List.of("route-tradeoff:" + candidateId),
                BOUNDARY_NOTE);
    }
}
