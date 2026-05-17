package com.richmond423.loadbalancerpro.core;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AdaptiveRoutingScenarioSummary(
        String runnerName,
        String runnerVersion,
        String mode,
        boolean deterministic,
        Instant generatedAt,
        String generatedAtPolicy,
        String dashboardPath,
        String apiPath,
        int scenarioCount,
        List<String> strategiesCompared,
        int totalDecisions,
        List<AdaptiveRoutingScenarioResult> scenarios,
        List<String> localEvidencePaths,
        List<String> warnings,
        List<String> notProvenBoundaries,
        List<String> safetyBoundaries,
        List<String> recommendedNextSteps) {

    public AdaptiveRoutingScenarioSummary {
        runnerName = requireNonBlank(runnerName, "runnerName");
        runnerVersion = requireNonBlank(runnerVersion, "runnerVersion");
        mode = requireNonBlank(mode, "mode");
        Objects.requireNonNull(generatedAt, "generatedAt cannot be null");
        generatedAtPolicy = requireNonBlank(generatedAtPolicy, "generatedAtPolicy");
        dashboardPath = requireNonBlank(dashboardPath, "dashboardPath");
        apiPath = requireNonBlank(apiPath, "apiPath");
        requireNonNegative(scenarioCount, "scenarioCount");
        Objects.requireNonNull(strategiesCompared, "strategiesCompared cannot be null");
        requireNonNegative(totalDecisions, "totalDecisions");
        Objects.requireNonNull(scenarios, "scenarios cannot be null");
        Objects.requireNonNull(localEvidencePaths, "localEvidencePaths cannot be null");
        Objects.requireNonNull(warnings, "warnings cannot be null");
        Objects.requireNonNull(notProvenBoundaries, "notProvenBoundaries cannot be null");
        Objects.requireNonNull(safetyBoundaries, "safetyBoundaries cannot be null");
        Objects.requireNonNull(recommendedNextSteps, "recommendedNextSteps cannot be null");
        strategiesCompared = List.copyOf(strategiesCompared);
        scenarios = List.copyOf(scenarios);
        localEvidencePaths = List.copyOf(localEvidencePaths);
        warnings = List.copyOf(warnings);
        notProvenBoundaries = List.copyOf(notProvenBoundaries);
        safetyBoundaries = List.copyOf(safetyBoundaries);
        recommendedNextSteps = List.copyOf(recommendedNextSteps);
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
