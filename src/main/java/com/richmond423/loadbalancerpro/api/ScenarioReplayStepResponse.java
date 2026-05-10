package com.richmond423.loadbalancerpro.api;

import java.util.List;
import java.util.Map;

public record ScenarioReplayStepResponse(
        String stepId,
        String type,
        String status,
        String strategy,
        Map<String, Double> allocations,
        double acceptedLoad,
        double rejectedLoad,
        double unallocatedLoad,
        int recommendedAdditionalServers,
        ScalingSimulationResult scalingSimulation,
        LoadSheddingEvaluation loadShedding,
        AllocationEvaluationMetricsPreview metricsPreview,
        String selectedServerId,
        List<RoutingComparisonResultResponse> routingResults,
        List<ScenarioServerState> serverStates,
        String reason) {
}
