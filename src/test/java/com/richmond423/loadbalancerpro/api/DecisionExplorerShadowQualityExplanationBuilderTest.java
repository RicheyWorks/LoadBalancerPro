package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerShadowQualityExplanationBuilderTest {
    private static final String BOUNDARY_NOTE = "local shadow decision-quality diagnostics only";

    private final DecisionExplorerShadowQualityExplanationBuilder builder =
            new DecisionExplorerShadowQualityExplanationBuilder();

    @Test
    void explanationTextUsesUnavailableCandidateOutcomeFallback() {
        DecisionExplorerConfidenceSummaryV1 summary = DecisionExplorerConfidenceSummaryV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerRouteTradeoffAnalysisV1 tradeoff =
                DecisionExplorerRouteTradeoffAnalysisV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = tradeoff.evidenceSufficiency();
        DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness = tradeoff.replayReadinessDiagnostic();

        String explanation = builder.explanationText(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_UNKNOWN,
                summary,
                tradeoff,
                sufficiency,
                replayReadiness,
                List.of(),
                DecisionExplorerShadowPolicySensitivityDiagnosticV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerShadowScenarioInputQualityV1.unknown(BOUNDARY_NOTE),
                "shadow-decision-quality:v1:UNKNOWN:UNKNOWN:UNKNOWN:outcomes=0");

        assertTrue(explanation.contains("Shadow decision-quality explanation is UNKNOWN"));
        assertTrue(explanation.contains("Candidate outcome comparison rows were unavailable."));
        assertTrue(explanation.contains("no production routing decision is changed."));
    }

    @Test
    void explanationTextSummarizesSelectedOutcomeAndReviewAlternative() {
        DecisionExplorerConfidenceSummaryV1 summary = DecisionExplorerConfidenceSummaryV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerRouteTradeoffAnalysisV1 tradeoff =
                DecisionExplorerRouteTradeoffAnalysisV1.unknown(BOUNDARY_NOTE);
        List<DecisionExplorerShadowCandidateOutcomeV1> outcomes = List.of(
                outcome("edge-a", true, DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SELECTED_BASELINE,
                        DecisionExplorerShadowCandidateOutcomeV1.IMPACT_SUPPORTS_DECISION),
                outcome("edge-b", false, DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_CLOSE_CALL,
                        DecisionExplorerShadowCandidateOutcomeV1.IMPACT_REVIEW_SIGNAL));

        String explanation = builder.explanationText(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_REVIEW_RECOMMENDED,
                summary,
                tradeoff,
                tradeoff.evidenceSufficiency(),
                tradeoff.replayReadinessDiagnostic(),
                outcomes,
                DecisionExplorerShadowPolicySensitivityDiagnosticV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerShadowScenarioInputQualityV1.unknown(BOUNDARY_NOTE),
                "shadow-decision-quality:v1:REVIEW");

        assertTrue(explanation.contains(
                "Candidate outcomes show selected outcome SELECTED_BASELINE for edge-a and alternative edge-b "
                        + "as CLOSE_CALL/REVIEW_SIGNAL."));
        assertTrue(explanation.contains("policy sensitivity UNKNOWN/UNKNOWN"));
    }

    @Test
    void summaryHelpersPreserveNoProductionMutationWordingAndUnknownScoreFallback() {
        DecisionExplorerConfidenceSummaryV1 summary = DecisionExplorerConfidenceSummaryV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerRoutingDiagnosticsV1 diagnostics =
                DecisionExplorerRoutingDiagnosticsV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerRouteTradeoffAnalysisV1 tradeoff =
                DecisionExplorerRouteTradeoffAnalysisV1.unknown(BOUNDARY_NOTE);

        assertEquals(
                "Shadow decision-quality evaluation classified selected candidate UNKNOWN as UNKNOWN from confidence "
                        + "UNKNOWN, route tradeoff UNKNOWN, evidence sufficiency INSUFFICIENT, and replay readiness "
                        + "UNKNOWN; no production routing decision is changed.",
                builder.evidenceBasisSummary(
                        DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_UNKNOWN,
                        summary,
                        tradeoff,
                        tradeoff.evidenceSufficiency(),
                        tradeoff.replayReadinessDiagnostic()));
        assertEquals(
                "Selected candidate UNKNOWN is evaluated as the returned decision baseline; closest alternative "
                        + "UNKNOWN has score delta UNKNOWN.",
                builder.selectedCandidateBasisSummary(diagnostics, tradeoff));
    }

    private static DecisionExplorerShadowCandidateOutcomeV1 outcome(
            String candidateId,
            boolean selected,
            String outcomeLabel,
            String qualityImpact) {
        return new DecisionExplorerShadowCandidateOutcomeV1(
                candidateId,
                candidateId,
                selected,
                selected ? 0 : 1,
                outcomeLabel,
                qualityImpact,
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_CLOSE,
                "BALANCED",
                selected ? DecisionExplorerConfidenceSummaryV1.STATUS_STRONG
                        : DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL,
                "LOW",
                "CLOSE",
                selected ? 10.0 : 11.0,
                selected ? 0.0 : 1.0,
                "summary",
                List.of("benefit"),
                List.of("risk"),
                List.of(),
                List.of(),
                List.of("REASON"),
                List.of("source"),
                BOUNDARY_NOTE);
    }
}
