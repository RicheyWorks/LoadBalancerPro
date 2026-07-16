package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentEvaluation.Disposition;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentEvaluation.Trigger;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentProofReport.ScenarioEvidence;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyEngine;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.core.TrafficAllocationGuardrailAction;
import com.richmond423.loadbalancerpro.core.TrafficAllocationGuardrailInput;
import com.richmond423.loadbalancerpro.core.TrafficAllocationGuardrailPolicy;
import com.richmond423.loadbalancerpro.core.TrafficAllocationRecommendation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabExperimentProofRunnerTest {

    @Test
    void allSuiteProvesCompletionAndRollbackThroughActualLoopbackStack() throws Exception {
        EnterpriseLabExperimentProofReport report = new EnterpriseLabExperimentProofRunner().run("all");

        assertTrue(report.allPassed());
        assertEquals("all", report.requestedSuite());
        assertEquals(13, report.scenarios().size());
        assertEquals(837, report.totalActualRequests());
        assertEquals(3, report.backendCount());
        assertTrue(report.contentFingerprint().matches("[0-9a-f]{64}"));
        assertTrue(report.scopeBoundaries().stream().anyMatch(value -> value.contains("not production routing")));

        Map<String, ScenarioEvidence> scenarios = report.scenarios().stream()
                .collect(Collectors.toMap(ScenarioEvidence::proofId, Function.identity()));
        assertEquals(EnterpriseLabExperimentState.COMPLETED,
                scenarios.get("stable-completion").finalRecord().lifecycle().state());
        assertEquals("ALLOW", scenarios.get("stable-completion").guardrailAction());
        assertFalse(scenarios.get("stable-completion").guardrailClamped());
        assertEquals(
                java.util.List.of(Disposition.CONTINUE_HOLDING, Disposition.COMPLETED),
                scenarios.get("stable-completion").evaluationDispositions());
        assertTrue(scenarios.get("stable-completion").idempotencyVerified());

        ScenarioEvidence recovery = scenarios.get("transient-recovery-completion");
        assertEquals(1, recovery.observedOutcomes().get("FAILURE"));
        assertEquals(119, recovery.observedOutcomes().get("SUCCESS"));
        assertEquals(EnterpriseLabExperimentState.COMPLETED, recovery.finalRecord().lifecycle().state());
        assertEquals(60, scenarios.get("duration-completion").finalRecord().lifecycle().requestCount());

        assertTrigger(scenarios, "tail-latency-rollback", Trigger.LATENCY_REGRESSION);
        assertTrigger(scenarios, "failure-rate-rollback", Trigger.FAILURE_RATE);
        assertTrigger(scenarios, "timeout-rate-rollback", Trigger.TIMEOUT_RATE);
        assertTrigger(scenarios, "partial-degradation-rollback", Trigger.PARTIAL_DEGRADATION);
        assertTrigger(scenarios, "hold-degradation-rollback", Trigger.FAILURE_RATE);
        assertTrigger(scenarios, "insufficient-evidence-rollback", Trigger.SPARSE_EVIDENCE_AT_BOUNDARY);
        assertTrigger(scenarios, "stale-evidence-rollback", Trigger.STALE_EVIDENCE);
        assertEquals(12, scenarios.get("timeout-rate-rollback").observedOutcomes().get("TIMEOUT"));
        assertEquals(3, scenarios.get("request-limit-rollback").actualRequestCount());
        assertTrue(scenarios.get("operator-cancel-rollback").idempotencyVerified());
        assertTrue(scenarios.get("shutdown-rollback").idempotencyVerified());

        report.scenarios().forEach(scenario -> {
            assertTrue(scenario.passed(), scenario.proofId());
            assertTrue(scenario.boundVerified(), scenario.proofId());
            assertTrue(scenario.baselineRestored(), scenario.proofId());
            assertFalse(scenario.finalRecord().currentAllocation().kind()
                    == EnterpriseLabLoopbackAllocationSnapshot.Kind.CANDIDATE, scenario.proofId());
            assertTrue(scenario.finalRecord().contentFingerprint().matches("[0-9a-f]{64}"), scenario.proofId());
        });
    }

    @Test
    void suiteSelectionAndUnsafeValuesFailClosed() throws Exception {
        EnterpriseLabExperimentProofReport completion = new EnterpriseLabExperimentProofRunner().run("completion");
        assertEquals(3, completion.scenarios().size());
        assertTrue(completion.scenarios().stream().allMatch(
                scenario -> scenario.finalRecord().lifecycle().state() == EnterpriseLabExperimentState.COMPLETED));

        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabExperimentProofRunner().run("external"));
    }

    @Test
    void deterministicGuardrailCalculationClampsExtremeCandidateBeforeAnyRouterAction() {
        Map<String, Double> baseline = Map.of("blue", 0.50, "green", 0.50);
        TrafficAllocationRecommendation recommendation = new TrafficAllocationRecommendation(
                Map.of("blue", 0.90, "green", 0.10),
                Map.of("blue", 0.90, "green", 0.10),
                0.0,
                false,
                false,
                java.util.List.of("deterministic PR6 clamp proof candidate"));
        TrafficAllocationGuardrailInput input = new TrafficAllocationGuardrailInput(
                "enterprise-lab:pr6-clamp-proof",
                AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT,
                baseline,
                recommendation,
                true,
                true,
                false,
                false,
                true,
                false);

        var decision = new AdaptiveRoutingPolicyEngine().evaluateAllocation(
                input, new TrafficAllocationGuardrailPolicy(1.0, 0.10));

        assertEquals(TrafficAllocationGuardrailAction.CLAMP, decision.action());
        assertTrue(decision.influenceAllowed());
        assertEquals(Map.of("blue", 0.60, "green", 0.40), decision.approvedAllocations());
        assertEquals(decision.approvedAllocations(), decision.effectiveAllocations());
        assertTrue(decision.reasons().contains("candidate clamped to maximum total share movement"));
        assertFalse(decision.reasons().stream().anyMatch(reason -> reason.contains("traffic action performed")));
    }

    private static void assertTrigger(
            Map<String, ScenarioEvidence> scenarios,
            String scenarioId,
            Trigger trigger) {
        ScenarioEvidence scenario = scenarios.get(scenarioId);
        assertTrue(scenario.rollbackTriggers().contains(trigger), scenarioId);
        assertEquals(EnterpriseLabExperimentState.ROLLED_BACK, scenario.finalRecord().lifecycle().state());
    }
}
