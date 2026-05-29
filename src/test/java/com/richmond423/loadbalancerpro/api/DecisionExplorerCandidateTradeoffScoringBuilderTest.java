package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerCandidateTradeoffScoringBuilderTest {
    private static final String BOUNDARY_NOTE = "read-only route tradeoff diagnostics";

    private final DecisionExplorerCandidateTradeoffScoringBuilder builder =
            new DecisionExplorerCandidateTradeoffScoringBuilder();

    @Test
    void buildsSelectedFirstScoringExplanationsWithFactorRollups() {
        DecisionExplorerRouteTradeoffRowV1 alternative = row(
                "edge-b",
                false,
                1,
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_CLOSE,
                DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_TRADEOFF,
                DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL,
                11.0,
                1.0);
        DecisionExplorerRouteTradeoffRowV1 selected = row(
                "edge-a",
                true,
                2,
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE,
                DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE,
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                10.0,
                0.0);

        List<DecisionExplorerCandidateTradeoffScoringExplanationV1> explanations = builder.build(
                List.of(alternative, selected),
                List.of(
                        factor("edge-a", DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_SUPPORTING,
                                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG),
                        factor("edge-b", DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_WARNING,
                                DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL)),
                BOUNDARY_NOTE);

        assertEquals(List.of("edge-a", "edge-b"), explanations.stream()
                .map(DecisionExplorerCandidateTradeoffScoringExplanationV1::candidateId)
                .toList());
        DecisionExplorerCandidateTradeoffScoringExplanationV1 selectedExplanation = explanations.get(0);
        assertEquals(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                selectedExplanation.explanationStatus());
        assertEquals(DecisionExplorerCandidateTradeoffScoringExplanationV1.SCORE_EVIDENCE_SELECTED_BASELINE_PRESENT,
                selectedExplanation.scoreEvidenceState());
        assertEquals(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                selectedExplanation.factorStatusRollup());
        assertTrue(selectedExplanation.scoringSignals().contains("selected final score is present"));
        assertTrue(selectedExplanation.scoringSignals().contains("factor diagnostics returned for candidate"));

        DecisionExplorerCandidateTradeoffScoringExplanationV1 alternativeExplanation = explanations.get(1);
        assertEquals(DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL,
                alternativeExplanation.explanationStatus());
        assertEquals(DecisionExplorerCandidateTradeoffScoringExplanationV1.SCORE_EVIDENCE_ALTERNATIVE_DELTA_PRESENT,
                alternativeExplanation.scoreEvidenceState());
        assertEquals(DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL,
                alternativeExplanation.factorStatusRollup());
        assertTrue(alternativeExplanation.scoringSignals().contains("alternative score delta is present"));
        assertTrue(alternativeExplanation.reasonCodes().contains("SCORING_EXPLANATION_STATUS_PARTIAL"));
    }

    @Test
    void missingScoreAndMissingFactorsProduceUnknownExplanationWithoutInventedEvidence() {
        DecisionExplorerRouteTradeoffRowV1 row = row(
                "edge-b",
                false,
                1,
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_UNKNOWN,
                DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_UNKNOWN,
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                null,
                null);

        DecisionExplorerCandidateTradeoffScoringExplanationV1 explanation =
                builder.build(List.of(row), List.of(), BOUNDARY_NOTE).get(0);

        assertEquals(DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN, explanation.explanationStatus());
        assertEquals(DecisionExplorerCandidateTradeoffScoringExplanationV1.SCORE_EVIDENCE_ALTERNATIVE_DELTA_UNKNOWN,
                explanation.scoreEvidenceState());
        assertEquals(DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN, explanation.factorStatusRollup());
        assertTrue(explanation.limitationSignals().contains("score evidence is incomplete for tradeoff explanation"));
        assertTrue(explanation.limitationSignals().contains("factor diagnostics were not returned for candidate"));
        assertTrue(explanation.reasonCodes().contains("FACTOR_STATUS_ROLLUP_UNKNOWN"));
    }

    @Test
    void degradedFactorRollupProducesDegradedExplanation() {
        DecisionExplorerRouteTradeoffRowV1 selected = row(
                "edge-a",
                true,
                1,
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE,
                DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE,
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                10.0,
                0.0);

        DecisionExplorerCandidateTradeoffScoringExplanationV1 explanation = builder.build(
                List.of(selected),
                List.of(factor("edge-a", DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_DEGRADED,
                        DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED)),
                BOUNDARY_NOTE).get(0);

        assertEquals(DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED, explanation.explanationStatus());
        assertEquals(DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED, explanation.factorStatusRollup());
        assertTrue(explanation.reasonCodes().contains("SCORING_EXPLANATION_STATUS_DEGRADED"));
    }

    private static DecisionExplorerRouteTradeoffRowV1 row(
            String candidateId,
            boolean selected,
            int displayOrder,
            String tradeoffCategory,
            String classification,
            String diagnosticStatus,
            Double finalScore,
            Double scoreDelta) {
        return new DecisionExplorerRouteTradeoffRowV1(
                candidateId,
                candidateId,
                selected,
                displayOrder,
                tradeoffCategory,
                classification,
                diagnosticStatus,
                DecisionExplorerCandidateDiagnosticV1.RISK_LOW,
                DecisionExplorerCandidateConfidenceV1.HEALTHY,
                finalScore,
                scoreDelta,
                DecisionExplorerRouteTradeoffRowV1.scoreGapCategoryFor(selected, scoreDelta),
                "scoring explanation",
                "evidence summary",
                List.of("benefit"),
                List.of("risk"),
                List.of("unknown"),
                List.of(),
                List.of("ROW_REASON"),
                List.of("source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerFactorDiagnosticV1 factor(String candidateId, String contribution, String status) {
        return new DecisionExplorerFactorDiagnosticV1(
                candidateId,
                "latency",
                1,
                contribution,
                status,
                "AVAILABLE",
                "42",
                "SUPPORTS_SELECTION",
                DecisionExplorerFactorDiagnosticV1.severityFor(contribution),
                0,
                0,
                DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_DEGRADED.equals(contribution) ? 1 : 0,
                0,
                "factor summary",
                List.of("factor support"),
                List.of(),
                List.of(),
                DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_DEGRADED.equals(contribution)
                        ? List.of("factor degraded")
                        : List.of(),
                List.of("FACTOR_REASON"),
                List.of("source"),
                BOUNDARY_NOTE);
    }
}
