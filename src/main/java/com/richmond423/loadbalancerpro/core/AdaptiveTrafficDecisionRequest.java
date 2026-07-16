package com.richmond423.loadbalancerpro.core;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record AdaptiveTrafficDecisionRequest(
        String decisionId,
        String contextId,
        AdaptiveRoutingPolicyMode mode,
        List<AdaptiveTrafficDecisionCandidate> candidates,
        Map<String, Double> baselineAllocations,
        Instant evaluatedAt,
        boolean conflictingSignals,
        boolean cooldownActive,
        boolean explicitExperimentContext,
        boolean operatorStopRequested) {

    public AdaptiveTrafficDecisionRequest {
        decisionId = requireNonBlank(decisionId, "decisionId");
        contextId = requireNonBlank(contextId, "contextId");
        mode = mode == null ? AdaptiveRoutingPolicyMode.OFF : mode;
        Objects.requireNonNull(candidates, "candidates cannot be null");
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates cannot be empty");
        }
        Set<String> serverIds = new HashSet<>();
        for (AdaptiveTrafficDecisionCandidate candidate : candidates) {
            Objects.requireNonNull(candidate, "candidates cannot contain null values");
            if (!serverIds.add(candidate.serverId())) {
                throw new IllegalArgumentException("candidate serverIds must be unique");
            }
        }
        candidates = candidates.stream()
                .sorted(Comparator.comparing(AdaptiveTrafficDecisionCandidate::serverId))
                .toList();
        baselineAllocations = TrafficAllocationMaps.immutableNormalized(
                baselineAllocations, "baselineAllocations", false);
        Objects.requireNonNull(evaluatedAt, "evaluatedAt cannot be null");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
