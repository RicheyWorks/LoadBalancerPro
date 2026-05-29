package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerCounterfactualFactorWeightDeltaEvaluatorTest {
    private static final String BOUNDARY_NOTE = "local-only counterfactual factor diagnostics";

    private final DecisionExplorerCounterfactualFactorWeightDeltaEvaluator evaluator =
            new DecisionExplorerCounterfactualFactorWeightDeltaEvaluator();

    @Test
    void classifiesFactorWeightDeltasWithDeterministicOrdering() {
        List<DecisionExplorerCounterfactualFactorWeightDeltaV1> deltas = evaluator.evaluate(
                tradeoff(List.of(
                        factorDelta("latency", 2, DecisionExplorerFactorTradeoffDeltaV1.DELTA_ADVANTAGE,
                                "SUPPORTING", "WARNING", "CLOSE", 0.5),
                        factorDelta("capacity", 1, DecisionExplorerFactorTradeoffDeltaV1.DELTA_DISADVANTAGE,
                                "WARNING", "SUPPORTING", "MATERIAL", 4.0),
                        factorDelta("region", 3, DecisionExplorerFactorTradeoffDeltaV1.DELTA_NEUTRAL,
                                "NEUTRAL", "NEUTRAL", "MATERIAL", 4.0))),
                stableScenarios(),
                BOUNDARY_NOTE);

        assertEquals(List.of("capacity", "latency", "region"), deltas.stream()
                .map(DecisionExplorerCounterfactualFactorWeightDeltaV1::factorName)
                .toList());

        DecisionExplorerCounterfactualFactorWeightDeltaV1 capacity = deltas.get(0);
        assertEquals("DESTABILIZING", capacity.factorWeightDeltaClassification());
        assertEquals("ALTERNATIVE_SUPPORT_PLUS_10", capacity.strongestScenarioId());
        assertTrue(capacity.alternativeSupportCanChallengeSelection());
        assertTrue(capacity.destabilizingSignals().contains("alternative factor has returned-evidence advantage"));
        assertTrue(capacity.reasonCodes().contains("COUNTERFACTUAL_FACTOR_WEIGHT_DELTA_DESTABILIZING"));

        DecisionExplorerCounterfactualFactorWeightDeltaV1 latency = deltas.get(1);
        assertEquals("STABILIZING", latency.factorWeightDeltaClassification());
        assertEquals("SELECTED_SUPPORT_PLUS_10", latency.strongestScenarioId());
        assertTrue(latency.selectedSupportStabilizesDecision());
        assertFalse(latency.alternativeSupportCanChallengeSelection());
        assertTrue(latency.stabilizingSignals().contains("selected factor has returned-evidence advantage"));

        DecisionExplorerCounterfactualFactorWeightDeltaV1 region = deltas.get(2);
        assertEquals("NEUTRAL", region.factorWeightDeltaClassification());
        assertEquals("BASELINE_RETURNED_EVIDENCE", region.strongestScenarioId());
    }

    @Test
    void degradedAndUnknownDeltasDoNotInventHealthyCounterfactualInterpretation() {
        List<DecisionExplorerCounterfactualFactorWeightDeltaV1> deltas = evaluator.evaluate(
                tradeoff(List.of(
                        factorDelta("healthState", 1, DecisionExplorerFactorTradeoffDeltaV1.DELTA_DEGRADED,
                                "SUPPORTING", "SUPPORTING", "MATERIAL", 6.0),
                        factorDelta("cost", 2, DecisionExplorerFactorTradeoffDeltaV1.DELTA_UNKNOWN,
                                "UNKNOWN", "SUPPORTING", "UNKNOWN", null))),
                degradedScenarios(),
                BOUNDARY_NOTE);

        assertEquals("DEGRADED", deltas.get(0).factorWeightDeltaClassification());
        assertEquals("BASELINE_RETURNED_EVIDENCE", deltas.get(0).strongestScenarioId());
        assertTrue(deltas.get(0).limitationSignals().contains("factor-weight delta includes degraded evidence"));

        assertEquals("DEGRADED", deltas.get(1).factorWeightDeltaClassification());
        assertTrue(deltas.get(1).reasonCodes().contains("BASELINE_FACTOR_DELTA_UNKNOWN"));
    }

    @Test
    void missingPolicyScenariosKeepFactorWeightDeltasUnknownWhenEvidenceIsIncomplete() {
        List<DecisionExplorerCounterfactualFactorWeightDeltaV1> deltas = evaluator.evaluate(
                tradeoff(List.of(factorDelta("cost", 1, DecisionExplorerFactorTradeoffDeltaV1.DELTA_UNKNOWN,
                        "UNKNOWN", "SUPPORTING", "UNKNOWN", null))),
                List.of(),
                BOUNDARY_NOTE);

        assertEquals(1, deltas.size());
        assertEquals("UNKNOWN", deltas.get(0).factorWeightDeltaClassification());
        assertEquals("UNKNOWN", deltas.get(0).strongestScenarioId());
        assertTrue(deltas.get(0).limitationSignals()
                .contains("policy-weight scenarios were unavailable for factor sensitivity"));
    }

    @Test
    void nullTradeoffOrEmptyFactorDeltasReturnEmptyRows() {
        assertTrue(evaluator.evaluate(null, stableScenarios(), BOUNDARY_NOTE).isEmpty());
        assertTrue(evaluator.evaluate(tradeoff(List.of()), stableScenarios(), BOUNDARY_NOTE).isEmpty());
    }

    @Test
    void dtoNormalizesInvalidValuesAndBuildsStableFingerprintInput() {
        DecisionExplorerCounterfactualFactorWeightDeltaV1 delta =
                new DecisionExplorerCounterfactualFactorWeightDeltaV1(
                        false,
                        false,
                        false,
                        null,
                        null,
                        "latency",
                        "edge-a",
                        "edge-b",
                        -1,
                        "MAYBE",
                        "MAYBE",
                        "SUPPORTING",
                        "WARNING",
                        "STRONG",
                        "PARTIAL",
                        null,
                        "MAYBE",
                        "MAYBE",
                        "SCENARIO",
                        99,
                        true,
                        false,
                        "CLOSE",
                        Double.NaN,
                        "summary",
                        null,
                        null,
                        null,
                        null,
                        null,
                        BOUNDARY_NOTE);

        assertTrue(delta.readOnly());
        assertTrue(delta.simulationOnly());
        assertTrue(delta.localOnly());
        assertEquals("UNKNOWN", delta.factorWeightDeltaClassification());
        assertEquals("UNKNOWN", delta.baselineDeltaClassification());
        assertEquals(10, delta.localAssumptionWeightShiftPercent());
        assertEquals("UNKNOWN", delta.selectedSupportScenarioLabel());
        assertEquals("UNKNOWN", delta.alternativeSupportScenarioLabel());
        assertEquals("latency:classification=UNKNOWN:baseline=UNKNOWN:selected=edge-a:alternative=edge-b:"
                        + "scenario=SCENARIO:shift=10:selectedStabilizes=true:alternativeChallenges=false:"
                        + "gap=CLOSE:delta=null",
                delta.fingerprintInput());
    }

    private static DecisionExplorerRouteTradeoffAnalysisV1 tradeoff(
            List<DecisionExplorerFactorTradeoffDeltaV1> factorDeltas) {
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
                2,
                1,
                1,
                "edge-b",
                5.0,
                List.of(),
                List.of(),
                factorDeltas,
                DecisionExplorerEvidenceSufficiencyV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerReplayReadinessDiagnosticV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "fingerprint",
                "route-tradeoff:v1:test",
                "summary",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("test"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerFactorTradeoffDeltaV1 factorDelta(
            String factorName,
            int displayOrder,
            String classification,
            String selectedContribution,
            String alternativeContribution,
            String scoreGapCategory,
            Double scoreDelta) {
        return new DecisionExplorerFactorTradeoffDeltaV1(
                factorName,
                "edge-a",
                "edge-b",
                displayOrder,
                classification,
                selectedContribution,
                alternativeContribution,
                "STRONG",
                "PARTIAL",
                "AVAILABLE",
                "AVAILABLE",
                "selected",
                "alternative",
                "ALTERNATIVE_CLOSE",
                scoreGapCategory,
                scoreDelta,
                "factor delta summary",
                List.of("selected signal"),
                List.of("alternative signal"),
                classification.equals(DecisionExplorerFactorTradeoffDeltaV1.DELTA_DEGRADED)
                        ? List.of("factor tradeoff delta includes degraded evidence")
                        : List.of(),
                List.of("FACTOR_TRADEOFF_DELTA_" + classification),
                List.of("factor-source-" + factorName),
                BOUNDARY_NOTE);
    }

    private static List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> stableScenarios() {
        return List.of(
                scenario(
                        DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_BASELINE_RETURNED_EVIDENCE,
                        1,
                        DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_RETURNED_EVIDENCE_WEIGHTS,
                        0,
                        "STABLE",
                        true,
                        false),
                scenario(
                        DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_SELECTED_SUPPORT_PLUS_10,
                        2,
                        DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_LOCAL_SELECTED_SUPPORT_PLUS_10,
                        10,
                        "STABLE",
                        true,
                        false),
                scenario(
                        DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_ALTERNATIVE_SUPPORT_PLUS_10,
                        3,
                        DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_LOCAL_ALTERNATIVE_SUPPORT_PLUS_10,
                        10,
                        "STABLE",
                        true,
                        false));
    }

    private static List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> degradedScenarios() {
        return List.of(
                scenario(
                        DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_BASELINE_RETURNED_EVIDENCE,
                        1,
                        DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_RETURNED_EVIDENCE_WEIGHTS,
                        0,
                        "DEGRADED",
                        false,
                        false));
    }

    private static DecisionExplorerCounterfactualPolicyWeightScenarioV1 scenario(
            String scenarioId,
            int displayOrder,
            String profile,
            int weightShift,
            String label,
            boolean selectedRemainsSupported,
            boolean alternativeClose) {
        return new DecisionExplorerCounterfactualPolicyWeightScenarioV1(
                true,
                true,
                true,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_OBJECT,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.CONTRACT_VERSION,
                scenarioId,
                displayOrder,
                profile,
                weightShift,
                label,
                DecisionExplorerCounterfactualAnalysisV1.bandFor(label),
                "edge-a",
                "edge-b",
                alternativeClose ? 0.5 : 5.0,
                selectedRemainsSupported,
                alternativeClose,
                alternativeClose ? "CLOSE_ALTERNATIVE" : "SELECTED_ADVANTAGE",
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY,
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL,
                "scenario summary",
                selectedRemainsSupported ? List.of("selected remains supported") : List.of(),
                alternativeClose ? List.of("alternative can be close") : List.of(),
                DecisionExplorerCounterfactualAnalysisV1.LABEL_DEGRADED.equals(label)
                        ? List.of("degraded scenario")
                        : List.of(),
                List.of("SCENARIO_" + scenarioId),
                List.of("scenario-source-" + scenarioId),
                BOUNDARY_NOTE);
    }
}
