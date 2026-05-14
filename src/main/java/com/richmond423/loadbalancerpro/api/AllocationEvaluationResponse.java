package com.richmond423.loadbalancerpro.api;

import java.util.Map;

public record AllocationEvaluationResponse(
        String strategy,
        Map<String, Double> allocations,
        double acceptedLoad,
        double rejectedLoad,
        double unallocatedLoad,
        int recommendedAdditionalServers,
        ScalingSimulationResult scalingSimulation,
        LoadSheddingEvaluation loadShedding,
        AllocationEvaluationMetricsPreview metricsPreview,
        LaseAllocationShadowSummary laseShadow,
        com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyDecision lasePolicy,
        boolean readOnly,
        RemediationPlan remediationPlan,
        String decisionReason) {
}
