package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class OperatorRemediationPlannerTest {
    private final OperatorRemediationPlanner planner = new OperatorRemediationPlanner();

    @Test
    void healthyEvaluationReturnsNoActionAdvisoryPlan() {
        RemediationPlan plan = planner.planForEvaluation(
                40.0,
                0.0,
                0.0,
                0,
                loadShedding("USER", "ALLOW"),
                List.of(server("green", true)));

        assertEquals("HEALTHY", plan.status());
        assertTrue(plan.advisoryOnly());
        assertTrue(plan.readOnly());
        assertFalse(plan.cloudMutation());
        assertEquals(List.of(RemediationAction.NO_ACTION), actions(plan));
        assertEquals(RemediationPriority.LOW, plan.recommendations().get(0).priority());
        assertFalse(plan.recommendations().get(0).executable());
    }

    @Test
    void overloadedEvaluationRanksScaleUpShedLoadAndUnhealthyInvestigation() {
        RemediationPlan plan = planner.planForEvaluation(
                100.0,
                50.0,
                50.0,
                1,
                loadShedding("BACKGROUND", "SHED"),
                List.of(server("primary", true), server("failed", false)));

        assertEquals("OVERLOADED", plan.status());
        assertEquals(List.of(
                RemediationAction.SCALE_UP,
                RemediationAction.SHED_LOAD,
                RemediationAction.INVESTIGATE_UNHEALTHY), actions(plan));
        assertEquals(1, plan.recommendations().get(0).rank());
        assertEquals(RemediationPriority.HIGH, plan.recommendations().get(0).priority());
        assertEquals(1, plan.recommendations().get(0).serverCount());
        assertEquals(50.0, plan.recommendations().get(0).loadAmount(), 0.01);
        assertEquals(RemediationPriority.MEDIUM, plan.recommendations().get(1).priority());
        assertFalse(plan.recommendations().get(2).executable());
    }

    @Test
    void allUnhealthyEvaluationRestoresCapacityBeforeRetry() {
        RemediationPlan plan = planner.planForEvaluation(
                0.0,
                80.0,
                80.0,
                0,
                loadShedding("USER", "SHED"),
                List.of(server("red", false)));

        assertEquals("NO_HEALTHY_CAPACITY", plan.status());
        assertEquals(List.of(
                RemediationAction.RESTORE_CAPACITY,
                RemediationAction.RETRY_WHEN_HEALTHY), actions(plan));
        assertEquals(RemediationPriority.HIGH, plan.recommendations().get(0).priority());
        assertEquals(80.0, plan.recommendations().get(0).loadAmount(), 0.01);
        assertFalse(plan.recommendations().get(0).executable());
    }

    @Test
    void replayPlanIsDeterministicAndReadOnly() {
        ScenarioReplayStepResponse overload = new ScenarioReplayStepResponse(
                "overload-check",
                "OVERLOAD",
                "OK",
                "CAPACITY_AWARE",
                Map.of("primary", 100.0),
                100.0,
                50.0,
                50.0,
                1,
                new ScalingSimulationResult(1, "simulated scale-up", true),
                loadShedding("BACKGROUND", "SHED"),
                null,
                null,
                List.of(),
                List.of(ScenarioServerState.from(server("primary", true))),
                "Simulated overload preview.");
        ScenarioReplayStepResponse unhealthy = new ScenarioReplayStepResponse(
                "fail-blue",
                "MARK_UNHEALTHY",
                "OK",
                null,
                Map.of(),
                0.0,
                0.0,
                0.0,
                0,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(ScenarioServerState.from(server("blue", false))),
                "Server blue marked unhealthy for this replay only.");

        RemediationPlan first = planner.planForReplay(List.of(overload, unhealthy));
        RemediationPlan second = planner.planForReplay(List.of(overload, unhealthy));

        assertEquals(first, second);
        assertEquals("OVERLOADED", first.status());
        assertTrue(first.advisoryOnly());
        assertTrue(first.readOnly());
        assertFalse(first.cloudMutation());
        assertEquals(List.of(
                RemediationAction.SCALE_UP,
                RemediationAction.SHED_LOAD,
                RemediationAction.INVESTIGATE_UNHEALTHY), actions(first));
    }

    private List<RemediationAction> actions(RemediationPlan plan) {
        return plan.recommendations().stream()
                .map(RemediationRecommendation::action)
                .toList();
    }

    private static LoadSheddingEvaluation loadShedding(String priority, String action) {
        return new LoadSheddingEvaluation(priority, action, "test decision", "allocation-evaluation",
                1, 1, 0, 1.0, 0.0, 0.0);
    }

    private static ServerInput server(String id, boolean healthy) {
        return new ServerInput(id, 10.0, 10.0, 10.0, 100.0, 1.0, healthy);
    }
}
