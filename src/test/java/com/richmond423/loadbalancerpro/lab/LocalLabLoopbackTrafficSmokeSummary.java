package com.richmond423.loadbalancerpro.lab;

import java.util.List;

record LocalLabLoopbackTrafficSmokeSummary(
        String summaryId,
        int totalRequests,
        int matchedFixtureCount,
        int boundaryResponseCount,
        List<String> scenarioIdsCovered,
        List<String> backendIdsCovered,
        String loopbackHostConfirmation,
        String ephemeralPortConfirmation,
        String noProductionProofWarning,
        String notProvenBoundarySummary,
        String nextSafeStepRecommendation) {

    LocalLabLoopbackTrafficSmokeSummary {
        requireText("summaryId", summaryId);
        if (totalRequests < 1) {
            throw new IllegalArgumentException("totalRequests must be positive");
        }
        if (matchedFixtureCount < 0) {
            throw new IllegalArgumentException("matchedFixtureCount must be non-negative");
        }
        if (boundaryResponseCount < 0) {
            throw new IllegalArgumentException("boundaryResponseCount must be non-negative");
        }
        scenarioIdsCovered = copyNonEmpty("scenarioIdsCovered", scenarioIdsCovered);
        backendIdsCovered = copyNonEmpty("backendIdsCovered", backendIdsCovered);
        requireText("loopbackHostConfirmation", loopbackHostConfirmation);
        requireText("ephemeralPortConfirmation", ephemeralPortConfirmation);
        requireText("noProductionProofWarning", noProductionProofWarning);
        requireText("notProvenBoundarySummary", notProvenBoundarySummary);
        requireText("nextSafeStepRecommendation", nextSafeStepRecommendation);
    }

    String deterministicText() {
        return String.join(" ",
                summaryId,
                Integer.toString(totalRequests),
                Integer.toString(matchedFixtureCount),
                Integer.toString(boundaryResponseCount),
                String.join(" | ", scenarioIdsCovered),
                String.join(" | ", backendIdsCovered),
                loopbackHostConfirmation,
                ephemeralPortConfirmation,
                noProductionProofWarning,
                notProvenBoundarySummary,
                nextSafeStepRecommendation);
    }

    private static List<String> copyNonEmpty(String field, List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        for (String value : values) {
            requireText(field, value);
        }
        return List.copyOf(values);
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
