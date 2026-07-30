package com.richmond423.loadbalancerpro.core;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record RoutingDecisionExplanation(
        String strategyUsed,
        List<String> candidateServersConsidered,
        Optional<String> chosenServerId,
        Map<String, Double> scores,
        Map<String, List<ScoreFactorContribution>> factorContributions,
        String reason,
        Instant timestamp) {

    public RoutingDecisionExplanation {
        strategyUsed = requireNonBlank(strategyUsed, "strategyUsed");
        Objects.requireNonNull(candidateServersConsidered, "candidateServersConsidered cannot be null");
        Objects.requireNonNull(chosenServerId, "chosenServerId cannot be null");
        Objects.requireNonNull(scores, "scores cannot be null");
        Objects.requireNonNull(factorContributions, "factorContributions cannot be null");
        reason = requireNonBlank(reason, "reason");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        candidateServersConsidered = List.copyOf(candidateServersConsidered);
        scores = Collections.unmodifiableMap(new LinkedHashMap<>(scores));
        Map<String, List<ScoreFactorContribution>> copiedContributions = new LinkedHashMap<>();
        factorContributions.forEach((candidateId, contributions) -> {
            String copiedCandidateId = requireNonBlank(candidateId, "factorContributions candidateId");
            Objects.requireNonNull(contributions, "factorContributions values cannot be null");
            copiedContributions.put(copiedCandidateId, List.copyOf(contributions));
        });
        factorContributions = Collections.unmodifiableMap(copiedContributions);
    }

    public RoutingDecisionExplanation(
            String strategyUsed,
            List<String> candidateServersConsidered,
            Optional<String> chosenServerId,
            Map<String, Double> scores,
            String reason,
            Instant timestamp) {
        this(strategyUsed, candidateServersConsidered, chosenServerId, scores, Map.of(), reason, timestamp);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
