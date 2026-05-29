package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerCounterfactualFingerprintBuilderTest {
    private static final String BOUNDARY_NOTE = "local-only counterfactual fingerprints";

    private final DecisionExplorerCounterfactualFingerprintBuilder builder =
            new DecisionExplorerCounterfactualFingerprintBuilder();

    @Test
    void buildsExpectedReproducibilityKeyAndFingerprintInputs() {
        DecisionExplorerCounterfactualFingerprintBuilder.FingerprintResult result = build(
                "UNKNOWN",
                List.of(),
                List.of(),
                List.of());

        assertEquals("counterfactual:v1:UNKNOWN:UNKNOWN:UNKNOWN:quality=UNKNOWN:"
                        + "sufficiency=INSUFFICIENT:replay=UNKNOWN:scenarios=0:outcomes=0:"
                        + "factorWeightDeltas=0",
                result.reproducibilityKey());
        assertTrue(result.diagnosticFingerprint().startsWith("counterfactual-analysis|v1|"));
        assertTrue(result.fingerprintInputs().contains("analysisObject=DecisionExplorerCounterfactualAnalysisV1"));
        assertTrue(result.fingerprintInputs().contains("policyWeightScenarioCount=0"));
        assertTrue(result.fingerprintInputs().contains("factorWeightDeltaCount=0"));
    }

    @Test
    void fingerprintOutputIsDeterministicAndChangesWhenComputedRowsChange() {
        DecisionExplorerCounterfactualFingerprintBuilder.FingerprintResult first = build(
                "STABLE",
                List.of(scenario("BASELINE_RETURNED_EVIDENCE", "STABLE")),
                List.of(outcome("edge-a", true, "SELECTED_STABLE")),
                List.of(factorDelta("latency", "STABILIZING")));
        DecisionExplorerCounterfactualFingerprintBuilder.FingerprintResult second = build(
                "STABLE",
                List.of(scenario("BASELINE_RETURNED_EVIDENCE", "STABLE")),
                List.of(outcome("edge-a", true, "SELECTED_STABLE")),
                List.of(factorDelta("latency", "STABILIZING")));
        DecisionExplorerCounterfactualFingerprintBuilder.FingerprintResult close = build(
                "CLOSE_CALL",
                List.of(scenario("BASELINE_RETURNED_EVIDENCE", "CLOSE_CALL")),
                List.of(outcome("edge-a", true, "SELECTED_SENSITIVE")),
                List.of(factorDelta("capacity", "DESTABILIZING")));

        assertEquals(first, second);
        assertNotEquals(first.diagnosticFingerprint(), close.diagnosticFingerprint());
        assertNotEquals(first.reproducibilityKey(), close.reproducibilityKey());
        assertTrue(first.fingerprintInputs().stream()
                .anyMatch(input -> input.startsWith("factorWeightDeltas=")
                        && input.contains("latency:classification=STABILIZING")));
    }

    @Test
    void nullListsAreSafeAndDoNotInventRows() {
        DecisionExplorerCounterfactualFingerprintBuilder.FingerprintResult result = builder.build(
                "UNKNOWN",
                DecisionExplorerConfidenceSummaryV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerRouteTradeoffAnalysisV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerShadowDecisionQualityEvaluationV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerEvidenceSufficiencyV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerReplayReadinessDiagnosticV1.unknown(BOUNDARY_NOTE),
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

        assertTrue(result.fingerprintInputs().contains("policyWeightScenarios=[]"));
        assertTrue(result.fingerprintInputs().contains("counterfactualCandidateOutcomes=[]"));
        assertTrue(result.fingerprintInputs().contains("factorWeightDeltas=[]"));
    }

    private DecisionExplorerCounterfactualFingerprintBuilder.FingerprintResult build(
            String label,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios,
            List<DecisionExplorerCounterfactualCandidateOutcomeV1> outcomes,
            List<DecisionExplorerCounterfactualFactorWeightDeltaV1> factorDeltas) {
        return builder.build(
                label,
                DecisionExplorerConfidenceSummaryV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerRouteTradeoffAnalysisV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerShadowDecisionQualityEvaluationV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerEvidenceSufficiencyV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerReplayReadinessDiagnosticV1.unknown(BOUNDARY_NOTE),
                scenarios,
                outcomes,
                factorDeltas,
                List.of("stable signal"),
                List.of("sensitivity signal"),
                List.of("limitation signal"),
                List.of("reason"),
                List.of("warning"),
                List.of("unknown"),
                List.of("source"));
    }

    private static DecisionExplorerCounterfactualPolicyWeightScenarioV1 scenario(
            String scenarioId,
            String label) {
        return new DecisionExplorerCounterfactualPolicyWeightScenarioV1(
                true,
                true,
                true,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_OBJECT,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.CONTRACT_VERSION,
                scenarioId,
                1,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_RETURNED_EVIDENCE_WEIGHTS,
                0,
                label,
                DecisionExplorerCounterfactualAnalysisV1.bandFor(label),
                "edge-a",
                "edge-b",
                5.0,
                true,
                false,
                "SELECTED_ADVANTAGE",
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY,
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL,
                "scenario summary",
                List.of(),
                List.of(),
                List.of(),
                List.of("SCENARIO_" + label),
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
                "BASELINE_RETURNED_EVIDENCE",
                "SELECTED_ADVANTAGE",
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
                1,
                classification,
                DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_STABILIZING.equals(classification)
                        ? DecisionExplorerFactorTradeoffDeltaV1.DELTA_ADVANTAGE
                        : DecisionExplorerFactorTradeoffDeltaV1.DELTA_DISADVANTAGE,
                "SUPPORTING",
                "WARNING",
                "STRONG",
                "PARTIAL",
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_RETURNED_EVIDENCE_WEIGHTS,
                "STABLE",
                "CLOSE_CALL",
                "BASELINE_RETURNED_EVIDENCE",
                0,
                DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_STABILIZING.equals(classification),
                DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_DESTABILIZING.equals(classification),
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
