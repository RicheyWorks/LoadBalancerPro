package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TrafficAllocationGuardrailDecisionTest {
    private static final Map<String, Double> BASELINE = Map.of("a", 0.50, "b", 0.50);
    private final AdaptiveRoutingPolicyEngine engine = new AdaptiveRoutingPolicyEngine();

    @Test
    void exposesObserveModeWithoutChangingLegacyBaselineBehavior() {
        assertEquals(AdaptiveRoutingPolicyMode.OBSERVE, AdaptiveRoutingPolicyMode.from("observe-only"));
        assertTrue(AdaptiveRoutingPolicyMode.wireValues().contains("observe"));

        AdaptiveRoutingPolicyDecision decision = engine.decide(new AdaptiveRoutingPolicyInput(
                "observe-context",
                AdaptiveRoutingPolicyMode.OBSERVE,
                List.of(),
                0.0,
                "a",
                "b",
                "NOOP",
                true,
                true,
                false,
                false,
                "observed",
                null));

        assertFalse(decision.influenceAllowed());
        assertFalse(decision.changed());
        assertEquals("a", decision.finalDecision());
        assertTrue(decision.guardrailReasons().contains("observe mode records inputs only"));
    }

    @Test
    void offAndObserveModesDenyAllocationInfluence() {
        for (AdaptiveRoutingPolicyMode mode : List.of(
                AdaptiveRoutingPolicyMode.OFF,
                AdaptiveRoutingPolicyMode.OBSERVE)) {
            TrafficAllocationGuardrailDecision decision = evaluate(
                    input(mode, recommendation(Map.of("a", 0.55, "b", 0.45))),
                    new TrafficAllocationGuardrailPolicy(1.0, 1.0));

            assertEquals(TrafficAllocationGuardrailAction.DENY, decision.action(), mode.name());
            assertFalse(decision.influenceAllowed(), mode.name());
            assertFalse(decision.changed(), mode.name());
            assertEquals(BASELINE, decision.effectiveAllocations(), mode.name());
        }
    }

    @Test
    void shadowAndRecommendApproveCandidateButRetainBaseline() {
        Map<String, Double> requested = Map.of("a", 0.55, "b", 0.45);
        for (AdaptiveRoutingPolicyMode mode : List.of(
                AdaptiveRoutingPolicyMode.SHADOW,
                AdaptiveRoutingPolicyMode.RECOMMEND)) {
            TrafficAllocationGuardrailDecision decision = evaluate(
                    input(mode, recommendation(requested)),
                    new TrafficAllocationGuardrailPolicy(1.0, 1.0));

            assertEquals(TrafficAllocationGuardrailAction.ALLOW, decision.action(), mode.name());
            assertEquals(requested, decision.approvedAllocations(), mode.name());
            assertEquals(BASELINE, decision.effectiveAllocations(), mode.name());
            assertFalse(decision.influenceAllowed(), mode.name());
            assertFalse(decision.changed(), mode.name());
        }
    }

    @Test
    void activeExperimentCanAuthorizeDecisionDataOnlyWhenEveryGatePasses() {
        Map<String, Double> requested = Map.of("a", 0.60, "b", 0.40);
        TrafficAllocationGuardrailDecision decision = evaluate(
                input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, recommendation(requested)),
                new TrafficAllocationGuardrailPolicy(1.0, 1.0));

        assertEquals(TrafficAllocationGuardrailAction.ALLOW, decision.action());
        assertTrue(decision.influenceAllowed());
        assertTrue(decision.changed());
        assertEquals(requested, decision.effectiveAllocations());
        assertTrue(decision.reasons().stream().anyMatch(reason -> reason.contains("no traffic action")));
    }

    @Test
    void deniesStaleInsufficientConflictingCooldownStoppedAndUnboundedInputs() {
        TrafficAllocationRecommendation recommendation = recommendation(Map.of("a", 0.60, "b", 0.40));

        assertDenied(input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, recommendation,
                false, true, false, false, true, false), "stale allocation signals");
        assertDenied(input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, recommendation,
                true, false, false, false, true, false), "insufficient allocation evidence");
        assertDenied(input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, recommendation,
                true, true, true, false, true, false), "conflicting allocation signals");
        assertDenied(input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, recommendation,
                true, true, false, true, true, false), "allocation cooldown is active");
        assertDenied(input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, recommendation,
                true, true, false, false, true, true), "operator stop requested");
        assertDenied(input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, recommendation,
                true, true, false, false, false, false),
                "active-experiment requires explicit bounded experiment context");
    }

    @Test
    void clampsMaximumBackendShareAndRenormalizesDeterministically() {
        TrafficAllocationGuardrailDecision decision = evaluate(
                input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT,
                        recommendation(Map.of("a", 0.90, "b", 0.10))),
                new TrafficAllocationGuardrailPolicy(0.70, 1.0));

        assertEquals(TrafficAllocationGuardrailAction.CLAMP, decision.action());
        assertEquals(0.70, decision.approvedAllocations().get("a"), 0.000000001);
        assertEquals(0.30, decision.approvedAllocations().get("b"), 0.000000001);
        assertEquals(1.0, total(decision.approvedAllocations()), 0.000000001);
        assertTrue(decision.reasons().contains("candidate clamped to maximum backend share"));
    }

    @Test
    void clampsTotalVariationAlongBaselineToCandidatePath() {
        TrafficAllocationGuardrailDecision decision = evaluate(
                input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT,
                        recommendation(Map.of("a", 0.90, "b", 0.10))),
                new TrafficAllocationGuardrailPolicy(1.0, 0.10));

        assertEquals(TrafficAllocationGuardrailAction.CLAMP, decision.action());
        assertEquals(0.60, decision.approvedAllocations().get("a"), 0.000000001);
        assertEquals(0.40, decision.approvedAllocations().get("b"), 0.000000001);
        assertEquals(0.10, TrafficAllocationMaps.totalVariation(
                BASELINE, decision.approvedAllocations()), 0.000000001);
        assertTrue(decision.reasons().contains("candidate clamped to maximum total share movement"));
    }

    @Test
    void permitsBoundedBackendIntroductionOnlyWhenHardCapCanBeReached() {
        Map<String, Double> singleBackendBaseline = Map.of("a", 1.0);
        TrafficAllocationRecommendation recommendation = recommendation(Map.of("a", 0.50, "b", 0.50));
        TrafficAllocationGuardrailInput input = input(
                "backend-introduction",
                AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT,
                singleBackendBaseline,
                recommendation,
                true,
                true,
                false,
                false,
                true,
                false);

        TrafficAllocationGuardrailDecision bounded = evaluate(
                input, new TrafficAllocationGuardrailPolicy(0.75, 0.25));
        assertEquals(TrafficAllocationGuardrailAction.CLAMP, bounded.action());
        assertEquals(Map.of("a", 0.75, "b", 0.25), bounded.approvedAllocations());

        TrafficAllocationGuardrailDecision tooSlow = evaluate(
                input, new TrafficAllocationGuardrailPolicy(0.75, 0.10));
        assertEquals(TrafficAllocationGuardrailAction.DENY, tooSlow.action());
        assertTrue(tooSlow.reasons().stream().anyMatch(reason -> reason.contains("cannot bring allocation")));
    }

    @Test
    void deniesFallbackOmittedBaselineBackendAndInfeasibleCap() {
        TrafficAllocationRecommendation fallback = new TrafficAllocationRecommendation(
                Map.of(), Map.of(), 1.0, true, false, List.of("test fallback"));
        assertDenied(input(AdaptiveRoutingPolicyMode.RECOMMEND, fallback),
                "allocation recommendation is in safe fallback");

        assertDenied(input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT,
                recommendation(Map.of("a", 1.0))),
                "candidate omits a backend with positive baseline allocation");

        TrafficAllocationGuardrailDecision infeasible = evaluate(
                input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT,
                        recommendation(Map.of("a", 0.50, "b", 0.50))),
                new TrafficAllocationGuardrailPolicy(0.40, 1.0));
        assertEquals(TrafficAllocationGuardrailAction.DENY, infeasible.action());
        assertTrue(infeasible.reasons().stream().anyMatch(reason -> reason.contains("infeasible")));
    }

    @Test
    void validatesContractsAndKeepsDecisionCollectionsImmutable() {
        assertThrows(IllegalArgumentException.class,
                () -> new TrafficAllocationGuardrailPolicy(0.0, 0.25));
        assertThrows(IllegalArgumentException.class, () -> input(
                "bad-context",
                AdaptiveRoutingPolicyMode.RECOMMEND,
                Map.of("a", 0.60, "b", 0.30),
                recommendation(Map.of("a", 0.50, "b", 0.50)),
                true,
                true,
                false,
                false,
                false,
                false));

        TrafficAllocationGuardrailDecision decision = evaluate(
                input(AdaptiveRoutingPolicyMode.RECOMMEND,
                        recommendation(Map.of("a", 0.55, "b", 0.45))),
                new TrafficAllocationGuardrailPolicy(1.0, 1.0));
        assertThrows(UnsupportedOperationException.class, () -> decision.approvedAllocations().clear());
        assertThrows(UnsupportedOperationException.class, () -> decision.reasons().clear());
    }

    private TrafficAllocationGuardrailDecision evaluate(
            TrafficAllocationGuardrailInput input,
            TrafficAllocationGuardrailPolicy policy) {
        return engine.evaluateAllocation(input, policy);
    }

    private void assertDenied(TrafficAllocationGuardrailInput input, String expectedReason) {
        TrafficAllocationGuardrailDecision decision = evaluate(input,
                new TrafficAllocationGuardrailPolicy(1.0, 1.0));
        assertEquals(TrafficAllocationGuardrailAction.DENY, decision.action());
        assertFalse(decision.influenceAllowed());
        assertFalse(decision.changed());
        assertEquals(BASELINE, decision.effectiveAllocations());
        assertTrue(decision.reasons().contains(expectedReason), decision.reasons().toString());
    }

    private static TrafficAllocationGuardrailInput input(
            AdaptiveRoutingPolicyMode mode,
            TrafficAllocationRecommendation recommendation) {
        return input(mode, recommendation, true, true, false, false, true, false);
    }

    private static TrafficAllocationGuardrailInput input(
            AdaptiveRoutingPolicyMode mode,
            TrafficAllocationRecommendation recommendation,
            boolean signalsFresh,
            boolean evidenceSufficient,
            boolean conflictingSignals,
            boolean cooldownActive,
            boolean explicitExperimentContext,
            boolean operatorStopRequested) {
        return input(
                "guardrail-context",
                mode,
                BASELINE,
                recommendation,
                signalsFresh,
                evidenceSufficient,
                conflictingSignals,
                cooldownActive,
                explicitExperimentContext,
                operatorStopRequested);
    }

    private static TrafficAllocationGuardrailInput input(
            String contextId,
            AdaptiveRoutingPolicyMode mode,
            Map<String, Double> baseline,
            TrafficAllocationRecommendation recommendation,
            boolean signalsFresh,
            boolean evidenceSufficient,
            boolean conflictingSignals,
            boolean cooldownActive,
            boolean explicitExperimentContext,
            boolean operatorStopRequested) {
        return new TrafficAllocationGuardrailInput(
                contextId,
                mode,
                baseline,
                recommendation,
                signalsFresh,
                evidenceSufficient,
                conflictingSignals,
                cooldownActive,
                explicitExperimentContext,
                operatorStopRequested);
    }

    private static TrafficAllocationRecommendation recommendation(Map<String, Double> allocations) {
        return new TrafficAllocationRecommendation(
                allocations,
                allocations,
                0.0,
                false,
                false,
                List.of("test recommendation"));
    }

    private static double total(Map<String, Double> allocations) {
        return allocations.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}
