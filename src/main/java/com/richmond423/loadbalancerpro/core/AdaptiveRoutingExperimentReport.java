package com.richmond423.loadbalancerpro.core;

import java.util.List;

public record AdaptiveRoutingExperimentReport(
        String mode,
        boolean activeInfluenceEnabled,
        List<AdaptiveRoutingExperimentResult> results,
        List<String> safetyNotes) {

    public AdaptiveRoutingExperimentReport {
        mode = requireNonBlank(mode, "mode");
        results = List.copyOf(results == null ? List.of() : results);
        safetyNotes = List.copyOf(safetyNotes == null ? List.of() : safetyNotes);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
