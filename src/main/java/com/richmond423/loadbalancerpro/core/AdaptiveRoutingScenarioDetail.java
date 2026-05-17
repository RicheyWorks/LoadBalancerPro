package com.richmond423.loadbalancerpro.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AdaptiveRoutingScenarioDetail(
        String scenarioName,
        String description,
        String mode,
        boolean deterministic,
        int iterations,
        List<AdaptiveRoutingCandidateSignal> candidates,
        int totalDecisions,
        Map<String, Map<String, Integer>> selectedServerCounts,
        List<AdaptiveRoutingStrategyExplanation> strategyExplanations,
        List<String> warnings,
        String explanationPolicy,
        List<String> notProvenBoundaries) {

    public AdaptiveRoutingScenarioDetail {
        scenarioName = requireNonBlank(scenarioName, "scenarioName");
        description = requireNonBlank(description, "description");
        mode = requireNonBlank(mode, "mode");
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        Objects.requireNonNull(candidates, "candidates cannot be null");
        requireNonNegative(totalDecisions, "totalDecisions");
        Objects.requireNonNull(selectedServerCounts, "selectedServerCounts cannot be null");
        Objects.requireNonNull(strategyExplanations, "strategyExplanations cannot be null");
        Objects.requireNonNull(warnings, "warnings cannot be null");
        explanationPolicy = requireNonBlank(explanationPolicy, "explanationPolicy");
        Objects.requireNonNull(notProvenBoundaries, "notProvenBoundaries cannot be null");
        candidates = List.copyOf(candidates);
        selectedServerCounts = immutableNestedCounts(selectedServerCounts);
        strategyExplanations = List.copyOf(strategyExplanations);
        warnings = List.copyOf(warnings);
        notProvenBoundaries = List.copyOf(notProvenBoundaries);
    }

    private static Map<String, Map<String, Integer>> immutableNestedCounts(
            Map<String, Map<String, Integer>> input) {
        Map<String, Map<String, Integer>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : input.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }
}
