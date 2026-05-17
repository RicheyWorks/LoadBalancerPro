package com.richmond423.loadbalancerpro.core;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AdaptiveRoutingMeasurementSummary(
        String runnerVersion,
        String measurementKind,
        Instant generatedAt,
        String generatedAtPolicy,
        int scenarioIterations,
        int candidateCount,
        int healthyCandidateCount,
        int strategyCount,
        int attemptedDecisions,
        int selectedDecisionCount,
        int noSelectionDecisionCount,
        List<String> notes) {

    public AdaptiveRoutingMeasurementSummary {
        runnerVersion = requireNonBlank(runnerVersion, "runnerVersion");
        measurementKind = requireNonBlank(measurementKind, "measurementKind");
        Objects.requireNonNull(generatedAt, "generatedAt cannot be null");
        generatedAtPolicy = requireNonBlank(generatedAtPolicy, "generatedAtPolicy");
        requireNonNegative(scenarioIterations, "scenarioIterations");
        requireNonNegative(candidateCount, "candidateCount");
        requireNonNegative(healthyCandidateCount, "healthyCandidateCount");
        requireNonNegative(strategyCount, "strategyCount");
        requireNonNegative(attemptedDecisions, "attemptedDecisions");
        requireNonNegative(selectedDecisionCount, "selectedDecisionCount");
        requireNonNegative(noSelectionDecisionCount, "noSelectionDecisionCount");
        Objects.requireNonNull(notes, "notes cannot be null");
        notes = List.copyOf(notes);
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
