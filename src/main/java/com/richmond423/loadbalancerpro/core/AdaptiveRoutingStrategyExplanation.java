package com.richmond423.loadbalancerpro.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AdaptiveRoutingStrategyExplanation(
        String scenarioName,
        String strategyName,
        int totalSelections,
        Map<String, Integer> selectedServerCounts,
        String dominantSelectedServer,
        List<String> candidateServersConsidered,
        Map<String, Double> scoreSnapshot,
        List<String> observedInputSignals,
        List<String> explanationNotes,
        List<String> cautionNotes,
        List<String> notProvenBoundaries) {

    public AdaptiveRoutingStrategyExplanation {
        scenarioName = requireNonBlank(scenarioName, "scenarioName");
        strategyName = requireNonBlank(strategyName, "strategyName");
        requireNonNegative(totalSelections, "totalSelections");
        Objects.requireNonNull(selectedServerCounts, "selectedServerCounts cannot be null");
        dominantSelectedServer = requireNonBlank(dominantSelectedServer, "dominantSelectedServer");
        Objects.requireNonNull(candidateServersConsidered, "candidateServersConsidered cannot be null");
        Objects.requireNonNull(scoreSnapshot, "scoreSnapshot cannot be null");
        Objects.requireNonNull(observedInputSignals, "observedInputSignals cannot be null");
        Objects.requireNonNull(explanationNotes, "explanationNotes cannot be null");
        Objects.requireNonNull(cautionNotes, "cautionNotes cannot be null");
        Objects.requireNonNull(notProvenBoundaries, "notProvenBoundaries cannot be null");
        selectedServerCounts = Collections.unmodifiableMap(new LinkedHashMap<>(selectedServerCounts));
        candidateServersConsidered = List.copyOf(candidateServersConsidered);
        scoreSnapshot = Collections.unmodifiableMap(new LinkedHashMap<>(scoreSnapshot));
        observedInputSignals = List.copyOf(observedInputSignals);
        explanationNotes = List.copyOf(explanationNotes);
        cautionNotes = List.copyOf(cautionNotes);
        notProvenBoundaries = List.copyOf(notProvenBoundaries);
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
