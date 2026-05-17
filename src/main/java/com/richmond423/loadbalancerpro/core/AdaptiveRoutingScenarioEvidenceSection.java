package com.richmond423.loadbalancerpro.core;

import java.util.List;
import java.util.Objects;

public record AdaptiveRoutingScenarioEvidenceSection(
        String sectionName,
        String summary,
        List<String> reviewItems) {

    public AdaptiveRoutingScenarioEvidenceSection {
        sectionName = requireNonBlank(sectionName, "sectionName");
        summary = requireNonBlank(summary, "summary");
        Objects.requireNonNull(reviewItems, "reviewItems cannot be null");
        reviewItems = List.copyOf(reviewItems);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
