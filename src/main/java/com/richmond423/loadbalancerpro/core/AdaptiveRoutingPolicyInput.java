package com.richmond423.loadbalancerpro.core;

import java.util.List;

public record AdaptiveRoutingPolicyInput(
        String contextId,
        AdaptiveRoutingPolicyMode mode,
        List<Server> servers,
        double requestedLoad,
        String baselineDecision,
        String recommendedDecision,
        String recommendedAction,
        boolean recommendationRecorded,
        boolean signalsFresh,
        boolean conflictingSignals,
        boolean explicitExperimentContext,
        String recommendationSummary,
        String failureReason) {

    public AdaptiveRoutingPolicyInput {
        contextId = requireNonBlank(contextId, "contextId");
        mode = mode == null ? AdaptiveRoutingPolicyMode.OFF : mode;
        servers = List.copyOf(servers == null ? List.of() : servers);
        baselineDecision = normalizeNullable(baselineDecision);
        recommendedDecision = normalizeNullable(recommendedDecision);
        recommendedAction = normalizeNullable(recommendedAction);
        recommendationSummary = normalizeNullable(recommendationSummary);
        failureReason = normalizeNullable(failureReason);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
