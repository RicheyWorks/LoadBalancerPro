package com.richmond423.loadbalancerpro.core;

import java.util.List;

public record AdaptiveRoutingExperimentScenario(
        String name,
        String description,
        String strategy,
        double requestedLoad,
        List<AdaptiveRoutingExperimentServer> servers,
        boolean signalsFresh,
        List<String> replayEventOrder,
        String expectedPressure) {

    public AdaptiveRoutingExperimentScenario {
        name = requireNonBlank(name, "name");
        description = requireNonBlank(description, "description");
        strategy = requireNonBlank(strategy, "strategy").toUpperCase().replace('-', '_');
        if (!"CAPACITY_AWARE".equals(strategy) && !"PREDICTIVE".equals(strategy)) {
            throw new IllegalArgumentException("strategy must be CAPACITY_AWARE or PREDICTIVE");
        }
        if (!Double.isFinite(requestedLoad) || requestedLoad < 0.0) {
            throw new IllegalArgumentException("requestedLoad must be finite and non-negative");
        }
        if (servers == null || servers.isEmpty()) {
            throw new IllegalArgumentException("servers must contain at least one server");
        }
        servers = List.copyOf(servers);
        replayEventOrder = List.copyOf(replayEventOrder == null ? List.of() : replayEventOrder);
        expectedPressure = requireNonBlank(expectedPressure, "expectedPressure");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
