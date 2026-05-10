package com.richmond423.loadbalancerpro.api;

import java.util.Locale;

enum ScenarioReplayEventType {
    ALLOCATE,
    EVALUATE,
    ROUTE,
    MARK_UNHEALTHY,
    MARK_HEALTHY,
    OVERLOAD;

    static ScenarioReplayEventType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("step type is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return ScenarioReplayEventType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unsupported scenario step type: " + value.trim());
        }
    }
}
