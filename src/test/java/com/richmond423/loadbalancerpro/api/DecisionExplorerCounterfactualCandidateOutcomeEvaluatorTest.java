package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerCounterfactualCandidateOutcomeEvaluatorTest {
    private static final String BOUNDARY_NOTE = "local-only counterfactual candidate outcomes";
    private final DecisionExplorerShadowCandidateOutcomeBuilder shadowOutcomeBuilder =
            new DecisionExplorerShadowCandidateOutcomeBuilder();
    private final DecisionExplorerCounterfactualCandidateOutcomeEvaluator evaluator =
            new DecisionExplorerCounterfactualCandidateOutcomeEvaluator();

    @Test
    void stableSelectedAdvantageKeepsSelectedStableAndAlternativeTrailing() {
        List<DecisionExplorerRouteTradeoffRowV1> rows = List.of(
                row("edge-b", false, 2, DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_TRAILS_SELECTED,
                        DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_SELECTED_ADVANTAGE,
                        DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                        DecisionExplorerCandidateDiagnosticV1.RISK_LOW, 15.0, 5.0),
                row("edge-a", true, 1, DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE,
                        DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE,
                        DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                        DecisionExplorerCandidateDiagnosticV1.RISK_LOW, 10.0, 0.0));

        List<DecisionExplorerCounterfactualCandidateOutcomeV1> outcomes =
                evaluate(rows, stableScenarios(5.0));

        assertEquals(List.of("edge-a", "edge-b"), outcomes.stream()
                .map(DecisionExplorerCounterfactualCandidateOutcomeV1::candidateId)
                .toList());
        assertEquals(DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_SELECTED_STABLE,
                outcomes.get(0).counterfactualOutcomeLabel());
        assertEquals(DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_ALTERNATIVE_TRAILING,
                outcomes.get(1).counterfactualOutcomeLabel());
        assertEquals(DecisionExplorerCounterfactualCandidateOutcomeV1.INFLUENCE_STABILIZING,
                outcomes.get(1).policyScenarioInfluence());
        assertTrue(outcomes.get(0).supportingScenarioIds()
                .contains(DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_SELECTED_SUPPORT_PLUS_10));
    }

    @Test
    void closeAlternativeMarksSelectedSensitiveAndAlternativeCloseCall() {
        List<DecisionExplorerRouteTradeoffRowV1> rows = List.of(
                row("edge-a", true, 1, DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE,
                        DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE,
                        DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                        DecisionExplorerCandidateDiagnosticV1.RISK_LOW, 10.0, 0.0),
                row("edge-b", false, 2, DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_CLOSE,
                        DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_TRADEOFF,
                        DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL,
                        DecisionExplorerCandidateDiagnosticV1.RISK_REVIEW, 10.5, 0.5));

        List<DecisionExplorerCounterfactualCandidateOutcomeV1> outcomes =
                evaluate(rows, closeCallScenarios(0.5));

        assertEquals(DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_SELECTED_SENSITIVE,
                outcomes.get(0).counterfactualOutcomeLabel());
        assertEquals(DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_ALTERNATIVE_CLOSE_CALL,
                outcomes.get(1).counterfactualOutcomeLabel());
        assertEquals(DecisionExplorerCounterfactualCandidateOutcomeV1.INFLUENCE_SENSITIVE,
                outcomes.get(1).policyScenarioInfluence());
        assertTrue(outcomes.get(1).challengingScenarioIds()
                .contains(DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_ALTERNATIVE_SUPPORT_PLUS_10));
    }

    @Test
    void saferAlternativeBecomesChallengingOutcome() {
        List<DecisionExplorerRouteTradeoffRowV1> rows = List.of(
                row("edge-a", true, 1, DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE,
                        DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE,
                        DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                        DecisionExplorerCandidateDiagnosticV1.RISK_LOW, 10.0, 0.0),
                row("edge-b", false, 2, DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_BEATS_SELECTED,
                        DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_RISK,
                        DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                        DecisionExplorerCandidateDiagnosticV1.RISK_LOW, 8.0, -2.0));

        DecisionExplorerCounterfactualCandidateOutcomeV1 alternative =
                evaluate(rows, closeCallScenarios(-2.0)).get(1);

        assertEquals(DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_ALTERNATIVE_CHALLENGES_SELECTED,
                alternative.counterfactualOutcomeLabel());
        assertEquals(DecisionExplorerCounterfactualCandidateOutcomeV1.INFLUENCE_CHALLENGING,
                alternative.policyScenarioInfluence());
        assertTrue(alternative.riskSignals()
                .contains("alternative can challenge selected candidate under returned score evidence"));
    }

    @Test
    void degradedSelectedCandidatePreservesDegradedOutcome() {
        List<DecisionExplorerRouteTradeoffRowV1> rows = List.of(row(
                "edge-a",
                true,
                1,
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE,
                DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE,
                DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED,
                DecisionExplorerCandidateDiagnosticV1.RISK_HIGH,
                10.0,
                0.0,
                List.of("selected candidate evidence degraded"),
                List.of()));

        DecisionExplorerCounterfactualCandidateOutcomeV1 selected =
                evaluate(rows, degradedScenarios()).get(0);

        assertEquals(DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_SELECTED_DEGRADED,
                selected.counterfactualOutcomeLabel());
        assertEquals(DecisionExplorerCounterfactualCandidateOutcomeV1.INFLUENCE_DEGRADED,
                selected.policyScenarioInfluence());
        assertTrue(selected.degradedSignals().contains("selected candidate evidence degraded"));
    }

    @Test
    void insufficientScenarioFallsBackConservatively() {
        List<DecisionExplorerRouteTradeoffRowV1> rows = List.of(row(
                "edge-a",
                true,
                1,
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE,
                DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE,
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                DecisionExplorerCandidateDiagnosticV1.RISK_UNKNOWN,
                null,
                null));

        DecisionExplorerCounterfactualCandidateOutcomeV1 selected =
                evaluate(rows, insufficientScenarios()).get(0);

        assertEquals(DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_INSUFFICIENT_EVIDENCE,
                selected.counterfactualOutcomeLabel());
        assertEquals(DecisionExplorerCounterfactualCandidateOutcomeV1.INFLUENCE_INSUFFICIENT,
                selected.policyScenarioInfluence());
    }

    @Test
    void emptyTradeoffRowsReturnEmptyOutcomeList() {
        assertTrue(evaluator.evaluate(tradeoff(List.of()), quality(List.of()), stableScenarios(5.0),
                BOUNDARY_NOTE).isEmpty());
    }

    private List<DecisionExplorerCounterfactualCandidateOutcomeV1> evaluate(
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios) {
        List<DecisionExplorerShadowCandidateOutcomeV1> shadowOutcomes =
                shadowOutcomeBuilder.build(rows, BOUNDARY_NOTE);
        return evaluator.evaluate(tradeoff(rows), quality(shadowOutcomes), scenarios, BOUNDARY_NOTE);
    }

    private static DecisionExplorerRouteTradeoffAnalysisV1 tradeoff(List<DecisionExplorerRouteTradeoffRowV1> rows) {
        return new DecisionExplorerRouteTradeoffAnalysisV1(
                true,
                true,
                DecisionExplorerRouteTradeoffAnalysisV1.ANALYSIS_OBJECT,
                DecisionExplorerRouteTradeoffAnalysisV1.CONTRACT_VERSION,
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_COMPLETE,
                "edge-a",
                "SELECTED_ADVANTAGE",
                "selected summary",
                "alternative summary",
                rows.size(),
                1,
                1,
                "edge-b",
                rows.stream()
                        .filter(row -> !row.selected())
                        .map(DecisionExplorerRouteTradeoffRowV1::scoreDeltaFromSelected)
                        .filter(value -> value != null)
                        .findFirst()
                        .orElse(null),
                rows,
                List.of(),
                List.of(),
                sufficiency(DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY),
                replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "tradeoff-fingerprint",
                "tradeoff-key",
                "tradeoff explanation",
                List.of("tradeoff=input"),
                List.of("TRADEOFF_TEST"),
                List.of(),
                List.of(),
                List.of("route-tradeoff:test"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerShadowDecisionQualityEvaluationV1 quality(
            List<DecisionExplorerShadowCandidateOutcomeV1> outcomes) {
        return new DecisionExplorerShadowDecisionQualityEvaluationV1(
                true,
                true,
                DecisionExplorerShadowDecisionQualityEvaluationV1.EVALUATION_OBJECT,
                DecisionExplorerShadowDecisionQualityEvaluationV1.CONTRACT_VERSION,
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_ACCEPTABLE,
                DecisionExplorerShadowDecisionQualityEvaluationV1.BAND_LOW,
                90,
                "edge-a",
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_COMPLETE,
                "SELECTED_ADVANTAGE",
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY,
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY,
                outcomes.size(),
                outcomes,
                DecisionExplorerShadowPolicySensitivityDiagnosticV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerShadowScenarioInputQualityV1.unknown(BOUNDARY_NOTE),
                1,
                1,
                "evidence basis",
                "selected basis",
                List.of("basis"),
                List.of("selected"),
                List.of("QUALITY_TEST"),
                List.of(),
                List.of(),
                List.of("shadow-quality:test"),
                "quality explanation",
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "quality-fingerprint",
                "quality-key",
                List.of("quality=input"),
                BOUNDARY_NOTE);
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

    private static List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> stableScenarios(Double delta) {
        return scenarios(delta,
                DecisionExplorerCounterfactualAnalysisV1.LABEL_STABLE,
                DecisionExplorerCounterfactualAnalysisV1.LABEL_STABLE,
                DecisionExplorerCounterfactualAnalysisV1.LABEL_STABLE);
    }

    private static List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> closeCallScenarios(Double delta) {
        return scenarios(delta,
                DecisionExplorerCounterfactualAnalysisV1.LABEL_CLOSE_CALL,
                DecisionExplorerCounterfactualAnalysisV1.LABEL_SENSITIVE,
                DecisionExplorerCounterfactualAnalysisV1.LABEL_CLOSE_CALL);
    }

    private static List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> degradedScenarios() {
        return scenarios(0.0,
                DecisionExplorerCounterfactualAnalysisV1.LABEL_DEGRADED,
                DecisionExplorerCounterfactualAnalysisV1.LABEL_DEGRADED,
                DecisionExplorerCounterfactualAnalysisV1.LABEL_DEGRADED);
    }

    private static List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> insufficientScenarios() {
        return List.of(scenario(
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_BASELINE_RETURNED_EVIDENCE,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_RETURNED_EVIDENCE_WEIGHTS,
                0,
                DecisionExplorerCounterfactualAnalysisV1.LABEL_INSUFFICIENT_EVIDENCE,
                null));
    }

    private static List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios(
            Double delta,
            String baselineLabel,
            String selectedLabel,
            String alternativeLabel) {
        return List.of(
                scenario(
                        DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_BASELINE_RETURNED_EVIDENCE,
                        DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_RETURNED_EVIDENCE_WEIGHTS,
                        0,
                        baselineLabel,
                        delta),
                scenario(
                        DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_SELECTED_SUPPORT_PLUS_10,
                        DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_LOCAL_SELECTED_SUPPORT_PLUS_10,
                        10,
                        selectedLabel,
                        delta),
                scenario(
                        DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_ALTERNATIVE_SUPPORT_PLUS_10,
                        DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_LOCAL_ALTERNATIVE_SUPPORT_PLUS_10,
                        10,
                        alternativeLabel,
                        delta));
    }

    private static DecisionExplorerCounterfactualPolicyWeightScenarioV1 scenario(
            String scenarioId,
            String profile,
            int shift,
            String label,
            Double delta) {
        return new DecisionExplorerCounterfactualPolicyWeightScenarioV1(
                true,
                true,
                true,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_OBJECT,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.CONTRACT_VERSION,
                scenarioId,
                scenarioId.equals(DecisionExplorerCounterfactualPolicyWeightScenarioV1
                        .SCENARIO_BASELINE_RETURNED_EVIDENCE) ? 1 : scenarioId.equals(
                                DecisionExplorerCounterfactualPolicyWeightScenarioV1
                                        .SCENARIO_SELECTED_SUPPORT_PLUS_10) ? 2 : 3,
                profile,
                shift,
                label,
                DecisionExplorerCounterfactualAnalysisV1.bandFor(label),
                "edge-a",
                "edge-b",
                delta,
                !DecisionExplorerCounterfactualAnalysisV1.LABEL_DEGRADED.equals(label),
                DecisionExplorerCounterfactualAnalysisV1.LABEL_CLOSE_CALL.equals(label),
                "SELECTED_ADVANTAGE",
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY,
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY,
                "scenario summary",
                List.of("stable"),
                List.of("sensitive"),
                List.of(),
                List.of("SCENARIO_" + scenarioId),
                List.of("policy-scenario:" + scenarioId),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerEvidenceSufficiencyV1 sufficiency(String level) {
        return new DecisionExplorerEvidenceSufficiencyV1(
                true,
                true,
                DecisionExplorerEvidenceSufficiencyV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerEvidenceSufficiencyV1.CONTRACT_VERSION,
                level,
                80,
                true,
                true,
                false,
                2,
                1,
                0,
                2,
                0,
                0,
                0,
                0,
                List.of("candidate evidence present"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("EVIDENCE_SUFFICIENCY_" + level),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "sufficiency-fingerprint",
                "sufficiency-key",
                List.of("sufficiency=" + level),
                List.of("sufficiency:test"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness(String status) {
        return new DecisionExplorerReplayReadinessDiagnosticV1(
                true,
                true,
                DecisionExplorerReplayReadinessDiagnosticV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerReplayReadinessDiagnosticV1.CONTRACT_VERSION,
                status,
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY,
                75,
                false,
                false,
                false,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                List.of("present"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("checklist"),
                List.of(),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "replay-fingerprint",
                "replay-key",
                List.of("replay=" + status),
                List.of("replay:test"),
                "replay explanation",
                BOUNDARY_NOTE);
    }
}
