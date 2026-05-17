package com.richmond423.loadbalancerpro.core;

import java.util.List;
import java.util.Objects;

public record AdaptiveRoutingStrategyComparisonRow(
        String strategyName,
        int scenarioCount,
        int totalDecisions,
        List<AdaptiveRoutingStrategyScenarioCell> cells,
        List<String> warnings,
        List<String> reviewerActions) {

    public AdaptiveRoutingStrategyComparisonRow {
        strategyName = requireNonBlank(strategyName, "strategyName");
        requireNonNegative(scenarioCount, "scenarioCount");
        requireNonNegative(totalDecisions, "totalDecisions");
        Objects.requireNonNull(cells, "cells cannot be null");
        Objects.requireNonNull(warnings, "warnings cannot be null");
        Objects.requireNonNull(reviewerActions, "reviewerActions cannot be null");
        cells = List.copyOf(cells);
        warnings = List.copyOf(warnings);
        reviewerActions = List.copyOf(reviewerActions);
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
