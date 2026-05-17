package com.richmond423.loadbalancerpro.core;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AdaptiveRoutingStrategyComparisonMatrix(
        String matrixName,
        String matrixVersion,
        String mode,
        boolean deterministic,
        Instant generatedAt,
        String generatedAtPolicy,
        String dashboardPath,
        String apiPath,
        List<String> sourceEndpoints,
        int scenarioCount,
        int strategyCount,
        int totalDecisions,
        List<AdaptiveRoutingStrategyComparisonRow> rows,
        List<AdaptiveRoutingStrategyComparisonInsight> insights,
        List<String> warnings,
        List<String> reviewerActions,
        List<String> localEvidencePaths,
        List<String> notProvenBoundaries,
        List<String> safetyBoundaries) {

    public AdaptiveRoutingStrategyComparisonMatrix {
        matrixName = requireNonBlank(matrixName, "matrixName");
        matrixVersion = requireNonBlank(matrixVersion, "matrixVersion");
        mode = requireNonBlank(mode, "mode");
        Objects.requireNonNull(generatedAt, "generatedAt cannot be null");
        generatedAtPolicy = requireNonBlank(generatedAtPolicy, "generatedAtPolicy");
        dashboardPath = requireNonBlank(dashboardPath, "dashboardPath");
        apiPath = requireNonBlank(apiPath, "apiPath");
        Objects.requireNonNull(sourceEndpoints, "sourceEndpoints cannot be null");
        requireNonNegative(scenarioCount, "scenarioCount");
        requireNonNegative(strategyCount, "strategyCount");
        requireNonNegative(totalDecisions, "totalDecisions");
        Objects.requireNonNull(rows, "rows cannot be null");
        Objects.requireNonNull(insights, "insights cannot be null");
        Objects.requireNonNull(warnings, "warnings cannot be null");
        Objects.requireNonNull(reviewerActions, "reviewerActions cannot be null");
        Objects.requireNonNull(localEvidencePaths, "localEvidencePaths cannot be null");
        Objects.requireNonNull(notProvenBoundaries, "notProvenBoundaries cannot be null");
        Objects.requireNonNull(safetyBoundaries, "safetyBoundaries cannot be null");
        sourceEndpoints = List.copyOf(sourceEndpoints);
        rows = List.copyOf(rows);
        insights = List.copyOf(insights);
        warnings = List.copyOf(warnings);
        reviewerActions = List.copyOf(reviewerActions);
        localEvidencePaths = List.copyOf(localEvidencePaths);
        notProvenBoundaries = List.copyOf(notProvenBoundaries);
        safetyBoundaries = List.copyOf(safetyBoundaries);
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
