package com.richmond423.loadbalancerpro.core;

public record AdaptiveRoutingStrategyComparisonInsight(
        String insightId,
        String title,
        String evidenceSource,
        String explanation,
        String reviewerAction) {

    public AdaptiveRoutingStrategyComparisonInsight {
        insightId = requireNonBlank(insightId, "insightId");
        title = requireNonBlank(title, "title");
        evidenceSource = requireNonBlank(evidenceSource, "evidenceSource");
        explanation = requireNonBlank(explanation, "explanation");
        reviewerAction = requireNonBlank(reviewerAction, "reviewerAction");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
