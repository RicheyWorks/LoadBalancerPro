package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerShadowQualityFingerprintBuilderTest {
    private static final String BOUNDARY_NOTE = "local shadow decision-quality diagnostics only";

    private final DecisionExplorerShadowQualityFingerprintBuilder builder =
            new DecisionExplorerShadowQualityFingerprintBuilder();

    @Test
    void buildsCanonicalInputsAndReproducibilityKeyFromComputedFields() {
        DecisionExplorerConfidenceSummaryV1 summary = DecisionExplorerConfidenceSummaryV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerRoutingDiagnosticsV1 diagnostics =
                DecisionExplorerRoutingDiagnosticsV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerRouteTradeoffAnalysisV1 tradeoff =
                DecisionExplorerRouteTradeoffAnalysisV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = tradeoff.evidenceSufficiency();
        DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness = tradeoff.replayReadinessDiagnostic();
        List<DecisionExplorerShadowCandidateOutcomeV1> outcomes = List.of(
                outcome("edge-a", true, DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SELECTED_BASELINE,
                        DecisionExplorerShadowCandidateOutcomeV1.IMPACT_SUPPORTS_DECISION, 0.0),
                outcome("edge-b", false, DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_CLOSE_CALL,
                        DecisionExplorerShadowCandidateOutcomeV1.IMPACT_REVIEW_SIGNAL, 1.25));
        DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity =
                policySensitivity(outcomes.size());
        DecisionExplorerShadowScenarioInputQualityV1 scenarioInputQuality =
                scenarioInputQuality(outcomes.size());

        List<String> inputs = builder.fingerprintInputs(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_REVIEW_RECOMMENDED,
                65,
                summary,
                diagnostics,
                tradeoff,
                sufficiency,
                replayReadiness,
                outcomes,
                policySensitivity,
                scenarioInputQuality,
                List.of("basis-b", "basis-a"),
                List.of("selected-b", "selected-a"),
                List.of("reason-b", "reason-a"),
                List.of("warning-b", "warning-a"),
                List.of("unknown-b", "unknown-a"),
                List.of("source-b", "source-a"));

        assertEquals("evaluationObject=DecisionExplorerShadowDecisionQualityEvaluationV1", inputs.get(0));
        assertTrue(inputs.contains("candidateOutcomeCount=2"));
        assertTrue(inputs.contains("evidenceBasis=basis-a;basis-b"));
        assertTrue(inputs.contains("qualityReasons=reason-a;reason-b"));
        assertTrue(inputs.stream().anyMatch(value -> value.contains("candidate=edge-b")
                && value.contains("outcome=CLOSE_CALL")
                && value.contains("impact=REVIEW_SIGNAL")
                && value.contains("delta=1.25")));
        assertTrue(inputs.stream().anyMatch(value -> value.startsWith("policySensitivity=level=MEDIUM")));
        assertTrue(inputs.stream().anyMatch(value -> value.startsWith("scenarioInputQuality=label=PARTIAL_INPUT")));

        assertEquals(
                "shadow-decision-quality:v1:REVIEW_RECOMMENDED:UNKNOWN:UNKNOWN:outcomes=2:"
                        + "policy=MEDIUM:scenario=PARTIAL_INPUT:sufficiency=INSUFFICIENT:replay=UNKNOWN",
                builder.reproducibilityKey(
                        DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_REVIEW_RECOMMENDED,
                        summary,
                        tradeoff,
                        sufficiency,
                        replayReadiness,
                        outcomes,
                        policySensitivity,
                        scenarioInputQuality));
    }

    @Test
    void diagnosticFingerprintCanonicalizesWhitespaceAndFallbackNamespace() {
        assertEquals(
                "diagnostic|v1|inputs=none",
                builder.diagnosticFingerprint(null, Arrays.asList("", null, "   ")));

        String fingerprint = builder.diagnosticFingerprint(
                " shadow-decision-quality|v1 \n",
                Arrays.asList(" alpha | beta ", null, "gamma\r\ndelta"));

        assertEquals("shadow-decision-quality|v1|alpha / beta|gamma delta", fingerprint);
        assertFalse(fingerprint.contains("\n"));
        assertFalse(fingerprint.contains("\r"));
    }

    private static DecisionExplorerShadowCandidateOutcomeV1 outcome(
            String candidateId,
            boolean selected,
            String outcomeLabel,
            String qualityImpact,
            Double scoreDelta) {
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
                selected ? 10.0 : 11.25,
                scoreDelta,
                "summary",
                List.of("benefit"),
                List.of("risk"),
                List.of("unknown"),
                List.of(),
                List.of("reason-z", "reason-a"),
                List.of("source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity(int outcomeCount) {
        return new DecisionExplorerShadowPolicySensitivityDiagnosticV1(
                true,
                true,
                DecisionExplorerShadowPolicySensitivityDiagnosticV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerShadowPolicySensitivityDiagnosticV1.CONTRACT_VERSION,
                DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_MEDIUM,
                DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_CLOSE_ALTERNATIVE,
                50,
                "edge-a",
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_CLOSE,
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT,
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN,
                outcomeCount,
                "summary",
                List.of("stable-b", "stable-a"),
                List.of("review"),
                List.of("missing"),
                List.of(),
                List.of("POLICY_B", "POLICY_A"),
                List.of("source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerShadowScenarioInputQualityV1 scenarioInputQuality(int outcomeCount) {
        return new DecisionExplorerShadowScenarioInputQualityV1(
                true,
                true,
                DecisionExplorerShadowScenarioInputQualityV1.EVALUATION_OBJECT,
                DecisionExplorerShadowScenarioInputQualityV1.CONTRACT_VERSION,
                DecisionExplorerShadowScenarioInputQualityV1.LABEL_PARTIAL_INPUT,
                DecisionExplorerShadowScenarioInputQualityV1.BAND_MEDIUM,
                55,
                "edge-a",
                DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL,
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT,
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN,
                2,
                1,
                outcomeCount,
                1,
                1,
                0,
                "summary",
                List.of("candidate-b", "candidate-a"),
                List.of("factor-a"),
                List.of("partial"),
                List.of("missing"),
                List.of(),
                List.of("SCENARIO_B", "SCENARIO_A"),
                List.of("source"),
                BOUNDARY_NOTE);
    }
}
