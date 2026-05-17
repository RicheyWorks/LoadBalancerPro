package com.richmond423.loadbalancerpro.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AdaptiveRoutingStrategyScenarioCell(
        String scenarioName,
        String strategyName,
        int decisionCount,
        Map<String, Integer> selectedServerDistribution,
        String dominantSelectedServer,
        int distinctSelectedServers,
        String diversitySignal,
        String consistencySignal,
        List<String> explanationNotes,
        List<String> warnings,
        List<String> reviewerActions,
        List<String> notProvenBoundaries) {

    public AdaptiveRoutingStrategyScenarioCell {
        scenarioName = requireNonBlank(scenarioName, "scenarioName");
        strategyName = requireNonBlank(strategyName, "strategyName");
        requireNonNegative(decisionCount, "decisionCount");
        Objects.requireNonNull(selectedServerDistribution, "selectedServerDistribution cannot be null");
        dominantSelectedServer = requireNonBlank(dominantSelectedServer, "dominantSelectedServer");
        requireNonNegative(distinctSelectedServers, "distinctSelectedServers");
        diversitySignal = requireNonBlank(diversitySignal, "diversitySignal");
        consistencySignal = requireNonBlank(consistencySignal, "consistencySignal");
        Objects.requireNonNull(explanationNotes, "explanationNotes cannot be null");
        Objects.requireNonNull(warnings, "warnings cannot be null");
        Objects.requireNonNull(reviewerActions, "reviewerActions cannot be null");
        Objects.requireNonNull(notProvenBoundaries, "notProvenBoundaries cannot be null");
        selectedServerDistribution = Collections.unmodifiableMap(new LinkedHashMap<>(selectedServerDistribution));
        explanationNotes = List.copyOf(explanationNotes);
        warnings = List.copyOf(warnings);
        reviewerActions = List.copyOf(reviewerActions);
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
