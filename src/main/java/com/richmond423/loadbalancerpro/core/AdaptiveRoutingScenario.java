package com.richmond423.loadbalancerpro.core;

import java.util.List;
import java.util.Objects;

public record AdaptiveRoutingScenario(
        String scenarioName,
        String description,
        String mode,
        int iterations,
        List<ServerStateVector> candidates,
        List<String> expectedSignals) {

    public AdaptiveRoutingScenario {
        scenarioName = requireNonBlank(scenarioName, "scenarioName");
        description = requireNonBlank(description, "description");
        mode = requireNonBlank(mode, "mode");
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        Objects.requireNonNull(candidates, "candidates cannot be null");
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }
        Objects.requireNonNull(expectedSignals, "expectedSignals cannot be null");
        candidates = List.copyOf(candidates);
        expectedSignals = List.copyOf(expectedSignals);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
