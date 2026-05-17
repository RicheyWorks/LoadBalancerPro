package com.richmond423.loadbalancerpro.core;

public record AdaptiveRoutingScenarioGateFinding(
        String id,
        String title,
        String status,
        String severity,
        String evidenceSource,
        String explanation,
        String reviewerAction) {

    public AdaptiveRoutingScenarioGateFinding {
        id = requireNonBlank(id, "id");
        title = requireNonBlank(title, "title");
        status = requireAllowed(status, "status", "PASS", "WARN", "FAIL");
        severity = requireAllowed(severity, "severity", "info", "warning", "blocking-for-review-only");
        evidenceSource = requireNonBlank(evidenceSource, "evidenceSource");
        explanation = requireNonBlank(explanation, "explanation");
        reviewerAction = requireNonBlank(reviewerAction, "reviewerAction");
    }

    private static String requireAllowed(String value, String fieldName, String... allowedValues) {
        String normalized = requireNonBlank(value, fieldName);
        for (String allowed : allowedValues) {
            if (allowed.equals(normalized)) {
                return normalized;
            }
        }
        throw new IllegalArgumentException(fieldName + " has unsupported value " + value);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
