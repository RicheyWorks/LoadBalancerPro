package com.richmond423.loadbalancerpro.core;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AdaptiveRoutingScenarioDrilldown(
        String runnerName,
        String runnerVersion,
        String mode,
        boolean deterministic,
        Instant generatedAt,
        String generatedAtPolicy,
        String dashboardPath,
        String summaryApiPath,
        String detailApiPath,
        int scenarioCount,
        int totalDecisions,
        List<AdaptiveRoutingScenarioDetail> scenarios,
        List<String> localEvidencePaths,
        List<String> warnings,
        List<String> notProvenBoundaries,
        List<String> safetyBoundaries) {

    public AdaptiveRoutingScenarioDrilldown {
        runnerName = requireNonBlank(runnerName, "runnerName");
        runnerVersion = requireNonBlank(runnerVersion, "runnerVersion");
        mode = requireNonBlank(mode, "mode");
        Objects.requireNonNull(generatedAt, "generatedAt cannot be null");
        generatedAtPolicy = requireNonBlank(generatedAtPolicy, "generatedAtPolicy");
        dashboardPath = requireNonBlank(dashboardPath, "dashboardPath");
        summaryApiPath = requireNonBlank(summaryApiPath, "summaryApiPath");
        detailApiPath = requireNonBlank(detailApiPath, "detailApiPath");
        requireNonNegative(scenarioCount, "scenarioCount");
        requireNonNegative(totalDecisions, "totalDecisions");
        Objects.requireNonNull(scenarios, "scenarios cannot be null");
        Objects.requireNonNull(localEvidencePaths, "localEvidencePaths cannot be null");
        Objects.requireNonNull(warnings, "warnings cannot be null");
        Objects.requireNonNull(notProvenBoundaries, "notProvenBoundaries cannot be null");
        Objects.requireNonNull(safetyBoundaries, "safetyBoundaries cannot be null");
        scenarios = List.copyOf(scenarios);
        localEvidencePaths = List.copyOf(localEvidencePaths);
        warnings = List.copyOf(warnings);
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
