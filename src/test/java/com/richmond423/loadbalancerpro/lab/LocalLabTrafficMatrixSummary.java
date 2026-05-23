package com.richmond423.loadbalancerpro.lab;

import java.util.List;

record LocalLabTrafficMatrixSummary(
        String summaryId,
        int matrixCaseCount,
        int matchedFixtureCount,
        int boundaryCaseCount,
        int scenarioCoverageCount,
        int backendCoverageCount,
        List<String> scenarioIdsCovered,
        List<String> backendIdsCovered,
        String loopbackOnlyConfirmation,
        String ephemeralPortConfirmation,
        String deterministicOutputConfirmation,
        String noProductionProofWarning,
        String futureToolingBoundarySummary,
        String nextSafeStepRecommendation) {

    LocalLabTrafficMatrixSummary {
        requireText("summaryId", summaryId);
        requirePositive("matrixCaseCount", matrixCaseCount);
        requireNonNegative("matchedFixtureCount", matchedFixtureCount);
        requireNonNegative("boundaryCaseCount", boundaryCaseCount);
        requireNonNegative("scenarioCoverageCount", scenarioCoverageCount);
        requireNonNegative("backendCoverageCount", backendCoverageCount);
        scenarioIdsCovered = copyNonEmpty("scenarioIdsCovered", scenarioIdsCovered);
        backendIdsCovered = copyNonEmpty("backendIdsCovered", backendIdsCovered);
        requireText("loopbackOnlyConfirmation", loopbackOnlyConfirmation);
        requireText("ephemeralPortConfirmation", ephemeralPortConfirmation);
        requireText("deterministicOutputConfirmation", deterministicOutputConfirmation);
        requireText("noProductionProofWarning", noProductionProofWarning);
        requireText("futureToolingBoundarySummary", futureToolingBoundarySummary);
        requireText("nextSafeStepRecommendation", nextSafeStepRecommendation);
    }

    String deterministicText() {
        return String.join(" ",
                summaryId,
                Integer.toString(matrixCaseCount),
                Integer.toString(matchedFixtureCount),
                Integer.toString(boundaryCaseCount),
                Integer.toString(scenarioCoverageCount),
                Integer.toString(backendCoverageCount),
                String.join(" | ", scenarioIdsCovered),
                String.join(" | ", backendIdsCovered),
                loopbackOnlyConfirmation,
                ephemeralPortConfirmation,
                deterministicOutputConfirmation,
                noProductionProofWarning,
                futureToolingBoundarySummary,
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

    private static void requirePositive(String field, int value) {
        if (value < 1) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private static void requireNonNegative(String field, int value) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must be non-negative");
        }
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
