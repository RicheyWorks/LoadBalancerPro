package com.richmond423.loadbalancerpro.core;

import java.util.Map;
import java.util.Objects;

public record TrafficAllocationGuardrailInput(
        String contextId,
        AdaptiveRoutingPolicyMode mode,
        Map<String, Double> baselineAllocations,
        TrafficAllocationRecommendation recommendation,
        boolean signalsFresh,
        boolean evidenceSufficient,
        boolean conflictingSignals,
        boolean cooldownActive,
        boolean explicitExperimentContext,
        boolean operatorStopRequested) {

    public TrafficAllocationGuardrailInput {
        contextId = requireNonBlank(contextId, "contextId");
        mode = mode == null ? AdaptiveRoutingPolicyMode.OFF : mode;
        baselineAllocations = TrafficAllocationMaps.immutableNormalized(
                baselineAllocations, "baselineAllocations", false);
        recommendation = Objects.requireNonNull(recommendation, "recommendation cannot be null");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
