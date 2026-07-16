package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.core.ServerDegradationState;
import com.richmond423.loadbalancerpro.core.ServerRollingSignalState;
import com.richmond423.loadbalancerpro.core.TrafficAllocationGuardrailAction;

import java.util.Map;

import org.junit.jupiter.api.Test;

class EnterpriseLabAdaptiveDecisionServiceTest {
    private final EnterpriseLabAdaptiveDecisionService service = new EnterpriseLabAdaptiveDecisionService();

    @Test
    void recommendDecisionIsDeterministicExplainableAndRetainsBaseline() {
        EnterpriseLabAdaptiveDecision first = service.decide(
                "normal-balanced-load", "recommend", false, false, false);
        EnterpriseLabAdaptiveDecision second = service.decide(
                "normal-balanced-load", "recommend", false, false, false);

        assertEquals(first, second);
        assertEquals(EnterpriseLabAdaptiveDecisionService.CONTRACT_VERSION, first.contractVersion());
        assertEquals("adaptive-traffic-decision/v1", first.decision().schemaVersion());
        assertEquals(3, first.decision().rollingStates().size());
        assertTrue(first.decision().observations().values().stream().allMatch(values -> values.size() == 5));
        assertTrue(first.decision().scoreBreakdowns().values().stream()
                .allMatch(breakdown -> breakdown.factorContributions().size() == 12));
        assertEquals(1.0, total(first.decision().allocationRecommendation().allocations()), 0.000000001);
        assertNotEquals(
                TrafficAllocationGuardrailAction.DENY,
                first.decision().guardrailDecision().action());
        assertFalse(first.decision().guardrailDecision().influenceAllowed());
        assertEquals(
                first.decision().request().baselineAllocations(),
                first.decision().effectiveAllocations());
        assertFalse(first.decision().trafficActionPerformed());
        assertEquals(first.decision().contentFingerprint(), first.contentFingerprint());
        assertFalse(first.trafficActionPerformed());
        assertTrue(first.decision().contentFingerprint().matches("[0-9a-f]{64}"));
    }

    @Test
    void observeAndOffModesExposeSignalsAndScoresWithoutRecommendation() {
        for (String mode : new String[] {"observe", "off"}) {
            EnterpriseLabAdaptiveDecision result = service.decide(
                    "normal-balanced-load", mode, false, false, false);

            assertTrue(result.decision().rollingStates().values().stream()
                    .allMatch(ServerRollingSignalState::sufficientEvidence));
            assertFalse(result.decision().scoreBreakdowns().isEmpty());
            assertTrue(result.decision().allocationRecommendation().allocations().isEmpty());
            assertEquals(TrafficAllocationGuardrailAction.DENY, result.decision().guardrailDecision().action());
            assertEquals(result.decision().request().baselineAllocations(), result.decision().effectiveAllocations());
        }
    }

    @Test
    void explicitActiveExperimentProducesBoundedDecisionDataButNoTrafficAction() {
        EnterpriseLabAdaptiveDecision result = service.decide(
                "tail-latency-pressure", "active-experiment", true, false, false);

        assertEquals(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, result.decision().mode());
        assertTrue(result.decision().scoreBreakdowns().get("blue").totalScore()
                > result.decision().scoreBreakdowns().get("green").totalScore());
        assertTrue(result.decision().allocationRecommendation().allocations().get("blue")
                < result.decision().allocationRecommendation().allocations().get("green"));
        assertTrue(result.decision().guardrailDecision().influenceAllowed());
        assertTrue(result.decision().guardrailDecision().changed());
        assertNotEquals(result.decision().request().baselineAllocations(), result.decision().effectiveAllocations());
        assertEquals(1.0, total(result.decision().effectiveAllocations()), 0.000000001);
        assertFalse(result.decision().trafficActionPerformed());
    }

    @Test
    void activeExperimentWithoutExplicitContextFailsClosedToBaseline() {
        EnterpriseLabAdaptiveDecision result = service.decide(
                "tail-latency-pressure", "active-experiment", false, false, false);

        assertEquals(TrafficAllocationGuardrailAction.DENY, result.decision().guardrailDecision().action());
        assertTrue(result.decision().guardrailDecision().reasons().stream()
                .anyMatch(reason -> reason.contains("explicit bounded experiment context")));
        assertEquals(result.decision().request().baselineAllocations(), result.decision().effectiveAllocations());
    }

    @Test
    void staleFixtureRetainsTimestampReferenceAndFailsClosed() {
        EnterpriseLabAdaptiveDecision result = service.decide(
                "stale-signal", "recommend", false, false, false);

        assertTrue(result.decision().rollingStates().values().stream().allMatch(ServerRollingSignalState::stale));
        assertTrue(result.decision().rollingStates().values().stream()
                .allMatch(state -> state.latestObservationAt().isPresent()));
        assertTrue(result.decision().allocationRecommendation().fallbackApplied());
        assertTrue(result.decision().allocationRecommendation().allocations().isEmpty());
        assertEquals(TrafficAllocationGuardrailAction.DENY, result.decision().guardrailDecision().action());
        assertEquals(result.decision().request().baselineAllocations(), result.decision().effectiveAllocations());
    }

    @Test
    void conflictAndAllUnhealthyFixturesFailClosedForDifferentStructuredReasons() {
        EnterpriseLabAdaptiveDecision conflict = service.decide(
                "conflicting-signal", "active-experiment", true, false, false);
        assertTrue(conflict.decision().guardrailDecision().reasons().contains("conflicting allocation signals"));

        EnterpriseLabAdaptiveDecision unhealthy = service.decide(
                "all-unhealthy-degradation", "active-experiment", true, false, false);
        assertTrue(unhealthy.decision().stateVectors().values().stream().noneMatch(vector -> vector.healthy()));
        assertTrue(unhealthy.decision().allocationRecommendation().fallbackApplied());
        assertTrue(unhealthy.decision().guardrailDecision().reasons().contains(
                "allocation recommendation is in safe fallback"));

        for (EnterpriseLabAdaptiveDecision result : new EnterpriseLabAdaptiveDecision[] {conflict, unhealthy}) {
            assertEquals(TrafficAllocationGuardrailAction.DENY, result.decision().guardrailDecision().action());
            assertEquals(result.decision().request().baselineAllocations(), result.decision().effectiveAllocations());
            assertFalse(result.decision().trafficActionPerformed());
        }
    }

    @Test
    void recoveryFixtureRepresentsRecoveryAndKeepsRollbackTarget() {
        EnterpriseLabAdaptiveDecision result = service.decide(
                "recovery-transition", "active-experiment", true, false, false);

        assertEquals(ServerDegradationState.RECOVERING,
                result.decision().rollingStates().get("green").degradationState());
        assertTrue(result.decision().rollingStates().get("green").recovering());
        assertFalse(result.decision().stateVectors().get("green").healthy());
        assertTrue(result.decision().allocationRecommendation().fallbackApplied());
        assertEquals(TrafficAllocationGuardrailAction.DENY, result.decision().guardrailDecision().action());
        assertEquals(result.decision().request().baselineAllocations(), result.decision().effectiveAllocations());
    }

    @Test
    void invalidScenarioAndModeAreRejectedWithoutFallbackToAction() {
        assertThrows(IllegalArgumentException.class,
                () -> service.decide("missing-scenario", "recommend", false, false, false));
        assertThrows(IllegalArgumentException.class,
                () -> service.decide("normal-balanced-load", "live", false, false, false));
        assertThrows(IllegalArgumentException.class,
                () -> service.decide(" ", "recommend", false, false, false));
    }

    private static double total(Map<String, Double> allocations) {
        return allocations.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}
