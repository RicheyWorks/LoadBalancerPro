package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerCounterfactualExplanationBuilderTest {
    private static final String BOUNDARY_NOTE = "local-only counterfactual explanations";

    private final DecisionExplorerCounterfactualExplanationBuilder builder =
            new DecisionExplorerCounterfactualExplanationBuilder();

    @Test
    void explanationUsesComputedPolicyCandidateAndFactorFields() {
        String explanation = builder.build(
                "STABLE",
                summary("STRONG", "edge-a"),
                tradeoff("SELECTED_ADVANTAGE"),
                sufficiency("REPLAY_STYLE_READY"),
                replay("READY"),
                List.of(
                        scenario("BASELINE_RETURNED_EVIDENCE", "STABLE", false),
                        scenario("ALTERNATIVE_SUPPORT_PLUS_10", "STABLE", false)),
                List.of(
                        outcome("edge-a", true, "SELECTED_STABLE"),
                        outcome("edge-b", false, "ALTERNATIVE_TRAILING")),
                List.of(factorDelta("latency", "STABILIZING")),
                "counterfactual:v1:STABLE:edge-a");

        assertTrue(explanation.contains(
                "selected candidate edge-a is STABLE with confidence STRONG, route tradeoff SELECTED_ADVANTAGE"));
        assertTrue(explanation.contains(
                "Policy scenarios show baseline BASELINE_RETURNED_EVIDENCE/STABLE across 2 bounded local "
                        + "scenarios and alternative-support ALTERNATIVE_SUPPORT_PLUS_10/STABLE with "
                        + "alternativeClose=false."));
        assertTrue(explanation.contains(
                "Candidate outcomes show selected edge-a as SELECTED_STABLE and alternative edge-b as "
                        + "ALTERNATIVE_TRAILING."));
        assertTrue(explanation.contains(
                "Factor-weight deltas show STABILIZING=1, DESTABILIZING=0, NEUTRAL=0, DEGRADED=0, UNKNOWN=0 "
                        + "with primary factor latency/STABILIZING."));
        assertTrue(explanation.contains("no production routing, scoring, proxying"));
    }

    @Test
    void explanationPrioritizesDestabilizingFactorsAndCloseAlternatives() {
        String stable = builder.build(
                "STABLE",
                summary("STRONG", "edge-a"),
                tradeoff("SELECTED_ADVANTAGE"),
                sufficiency("REPLAY_STYLE_READY"),
                replay("READY"),
                List.of(scenario("BASELINE_RETURNED_EVIDENCE", "STABLE", false)),
                List.of(outcome("edge-a", true, "SELECTED_STABLE")),
                List.of(factorDelta("latency", "STABILIZING")),
                "counterfactual:v1:STABLE");
        String close = builder.build(
                "CLOSE_CALL",
                summary("PARTIAL", "edge-a"),
                tradeoff("CLOSE_ALTERNATIVE"),
                sufficiency("TRADEOFF_READY"),
                replay("PARTIAL"),
                List.of(
                        scenario("BASELINE_RETURNED_EVIDENCE", "CLOSE_CALL", true),
                        scenario("ALTERNATIVE_SUPPORT_PLUS_10", "CLOSE_CALL", true)),
                List.of(
                        outcome("edge-a", true, "SELECTED_SENSITIVE"),
                        outcome("edge-b", false, "ALTERNATIVE_CLOSE_CALL")),
                List.of(
                        factorDelta("capacity", "DESTABILIZING"),
                        factorDelta("latency", "STABILIZING")),
                "counterfactual:v1:CLOSE_CALL");

        assertNotEquals(stable, close);
        assertTrue(close.contains("alternative-support ALTERNATIVE_SUPPORT_PLUS_10/CLOSE_CALL "
                + "with alternativeClose=true"));
        assertTrue(close.contains("alternative edge-b as ALTERNATIVE_CLOSE_CALL"));
        assertTrue(close.contains("primary factor capacity/DESTABILIZING"));
    }

    @Test
    void missingComputedRowsUseSafeFallbackText() {
        String explanation = builder.build(
                "UNKNOWN",
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null);

        assertTrue(explanation.contains("selected candidate UNKNOWN is UNKNOWN with confidence UNKNOWN"));
        assertTrue(explanation.contains("Policy-weight scenarios were unavailable."));
        assertTrue(explanation.contains("Counterfactual candidate outcomes were unavailable."));
        assertTrue(explanation.contains("Factor-weight deltas were unavailable."));
        assertTrue(explanation.contains("Reproducibility key UNKNOWN"));
    }

    private static DecisionExplorerConfidenceSummaryV1 summary(String status, String selectedCandidateId) {
        return new DecisionExplorerConfidenceSummaryV1(
                true,
                true,
                DecisionExplorerConfidenceSummaryV1.SUMMARY_OBJECT,
                DecisionExplorerConfidenceSummaryV1.CONTRACT_VERSION,
                status,
                DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(status),
                selectedCandidateId,
                2,
                1,
                1,
                0,
                0,
                0,
                0,
                1,
                List.of("summary evidence"),
                List.of("SUMMARY_STATUS_" + status),
                List.of(),
                List.of(),
                List.of("summary-source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRouteTradeoffAnalysisV1 tradeoff(String tradeoffCategory) {
        return new DecisionExplorerRouteTradeoffAnalysisV1(
                true,
                true,
                DecisionExplorerRouteTradeoffAnalysisV1.ANALYSIS_OBJECT,
                DecisionExplorerRouteTradeoffAnalysisV1.CONTRACT_VERSION,
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_COMPLETE,
                "edge-a",
                tradeoffCategory,
                "selected summary",
                "alternative summary",
                2,
                1,
                1,
                "edge-b",
                0.5,
                List.of(),
                List.of(),
                List.of(),
                DecisionExplorerEvidenceSufficiencyV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerReplayReadinessDiagnosticV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "route-tradeoff|test",
                "route-tradeoff:v1:test",
                "route explanation",
                List.of("fingerprint"),
                List.of("ROUTE_TRADEOFF_CATEGORY_" + tradeoffCategory),
                List.of(),
                List.of(),
                List.of("route-source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerEvidenceSufficiencyV1 sufficiency(String level) {
        return new DecisionExplorerEvidenceSufficiencyV1(
                true,
                true,
                DecisionExplorerEvidenceSufficiencyV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerEvidenceSufficiencyV1.CONTRACT_VERSION,
                level,
                90,
                true,
                true,
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY.equals(level),
                2,
                1,
                1,
                1,
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
                "evidence-sufficiency|test",
                "evidence-sufficiency:v1:test",
                List.of("sufficiencyLevel=" + level),
                List.of("sufficiency-source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerReplayReadinessDiagnosticV1 replay(String readinessStatus) {
        return new DecisionExplorerReplayReadinessDiagnosticV1(
                true,
                true,
                DecisionExplorerReplayReadinessDiagnosticV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerReplayReadinessDiagnosticV1.CONTRACT_VERSION,
                readinessStatus,
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY,
                90,
                false,
                false,
                false,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_PARTIAL,
                List.of("candidate evidence present"),
                List.of("factor evidence partial"),
                List.of(),
                List.of(),
                List.of("replay execution is intentionally unavailable"),
                List.of("candidate evidence: AVAILABLE"),
                List.of("replay execution is intentionally unavailable"),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "replay-readiness|test",
                "replay-readiness:v1:test",
                List.of("readinessStatus=" + readinessStatus),
                List.of("replay-source"),
                "replay summary",
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerCounterfactualPolicyWeightScenarioV1 scenario(
            String scenarioId,
            String sensitivityLabel,
            boolean alternativeClose) {
        return new DecisionExplorerCounterfactualPolicyWeightScenarioV1(
                true,
                true,
                true,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_OBJECT,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.CONTRACT_VERSION,
                scenarioId,
                "BASELINE_RETURNED_EVIDENCE".equals(scenarioId) ? 1 : 2,
                "BASELINE_RETURNED_EVIDENCE".equals(scenarioId)
                        ? DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_RETURNED_EVIDENCE_WEIGHTS
                        : DecisionExplorerCounterfactualPolicyWeightScenarioV1
                                .PROFILE_LOCAL_ALTERNATIVE_SUPPORT_PLUS_10,
                "BASELINE_RETURNED_EVIDENCE".equals(scenarioId) ? 0 : 10,
                sensitivityLabel,
                DecisionExplorerCounterfactualAnalysisV1.bandFor(sensitivityLabel),
                "edge-a",
                "edge-b",
                alternativeClose ? 0.5 : 5.0,
                true,
                alternativeClose,
                alternativeClose ? "CLOSE_ALTERNATIVE" : "SELECTED_ADVANTAGE",
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY,
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL,
                "scenario summary",
                List.of(),
                List.of(),
                List.of(),
                List.of("SCENARIO_" + scenarioId),
                List.of("scenario-source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerCounterfactualCandidateOutcomeV1 outcome(
            String candidateId,
            boolean selected,
            String outcomeLabel) {
        return new DecisionExplorerCounterfactualCandidateOutcomeV1(
                true,
                true,
                true,
                DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_OBJECT,
                DecisionExplorerCounterfactualCandidateOutcomeV1.CONTRACT_VERSION,
                candidateId,
                candidateId,
                selected,
                selected ? 1 : 2,
                selected ? "SELECTED_BASELINE" : "ALTERNATIVE",
                outcomeLabel,
                selected ? "STABILIZING" : "SENSITIVE",
                selected ? "SELECTED_SUPPORT_PLUS_10" : "ALTERNATIVE_SUPPORT_PLUS_10",
                selected ? "SELECTED_ADVANTAGE" : "CLOSE_ALTERNATIVE",
                selected ? "BENEFIT" : "RISK",
                selected ? "BASELINE" : "CLOSE",
                selected ? 10.0 : 10.5,
                selected ? 0.0 : 0.5,
                List.of(),
                List.of(),
                "candidate summary",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("OUTCOME_" + outcomeLabel),
                List.of("candidate-source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerCounterfactualFactorWeightDeltaV1 factorDelta(
            String factorName,
            String classification) {
        return new DecisionExplorerCounterfactualFactorWeightDeltaV1(
                true,
                true,
                true,
                DecisionExplorerCounterfactualFactorWeightDeltaV1.DELTA_OBJECT,
                DecisionExplorerCounterfactualFactorWeightDeltaV1.CONTRACT_VERSION,
                factorName,
                "edge-a",
                "edge-b",
                "latency".equals(factorName) ? 2 : 1,
                classification,
                "STABILIZING".equals(classification)
                        ? DecisionExplorerFactorTradeoffDeltaV1.DELTA_ADVANTAGE
                        : DecisionExplorerFactorTradeoffDeltaV1.DELTA_DISADVANTAGE,
                "SUPPORTING",
                "WARNING",
                "STRONG",
                "PARTIAL",
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_RETURNED_EVIDENCE_WEIGHTS,
                "STABLE",
                "CLOSE_CALL",
                "STABILIZING".equals(classification)
                        ? "SELECTED_SUPPORT_PLUS_10"
                        : "ALTERNATIVE_SUPPORT_PLUS_10",
                10,
                "STABILIZING".equals(classification),
                "DESTABILIZING".equals(classification),
                "CLOSE",
                0.5,
                "factor summary",
                List.of(),
                List.of(),
                List.of(),
                List.of("COUNTERFACTUAL_FACTOR_WEIGHT_DELTA_" + classification),
                List.of("factor-source"),
                BOUNDARY_NOTE);
    }
}
