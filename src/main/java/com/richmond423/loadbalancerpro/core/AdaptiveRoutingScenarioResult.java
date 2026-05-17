package com.richmond423.loadbalancerpro.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AdaptiveRoutingScenarioResult(
        String scenarioName,
        String mode,
        boolean deterministic,
        List<String> strategiesCompared,
        int totalDecisions,
        Map<String, Map<String, Integer>> selectedServerCounts,
        Map<String, Integer> noSelectionCounts,
        List<String> decisionNotes,
        List<String> warnings,
        String readinessForCiGate,
        List<String> localEvidencePaths,
        List<String> notProvenBoundaries,
        AdaptiveRoutingMeasurementSummary measurementSummary) {

    public AdaptiveRoutingScenarioResult {
        scenarioName = requireNonBlank(scenarioName, "scenarioName");
        mode = requireNonBlank(mode, "mode");
        Objects.requireNonNull(strategiesCompared, "strategiesCompared cannot be null");
        requireNonNegative(totalDecisions, "totalDecisions");
        Objects.requireNonNull(selectedServerCounts, "selectedServerCounts cannot be null");
        Objects.requireNonNull(noSelectionCounts, "noSelectionCounts cannot be null");
        Objects.requireNonNull(decisionNotes, "decisionNotes cannot be null");
        Objects.requireNonNull(warnings, "warnings cannot be null");
        readinessForCiGate = requireNonBlank(readinessForCiGate, "readinessForCiGate");
        Objects.requireNonNull(localEvidencePaths, "localEvidencePaths cannot be null");
        Objects.requireNonNull(notProvenBoundaries, "notProvenBoundaries cannot be null");
        Objects.requireNonNull(measurementSummary, "measurementSummary cannot be null");
        strategiesCompared = List.copyOf(strategiesCompared);
        selectedServerCounts = immutableNestedCounts(selectedServerCounts);
        noSelectionCounts = Collections.unmodifiableMap(new LinkedHashMap<>(noSelectionCounts));
        decisionNotes = List.copyOf(decisionNotes);
        warnings = List.copyOf(warnings);
        localEvidencePaths = List.copyOf(localEvidencePaths);
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
