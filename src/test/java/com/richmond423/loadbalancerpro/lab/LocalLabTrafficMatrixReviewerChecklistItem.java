package com.richmond423.loadbalancerpro.lab;

record LocalLabTrafficMatrixReviewerChecklistItem(
        String itemId,
        String matrixSummaryId,
        String reviewerQuestion,
        String observedValue,
        String expectedValue,
        String dispositionLabel,
        String safetyBoundary,
        String notProvenBoundary) {

    LocalLabTrafficMatrixReviewerChecklistItem {
        requireText("itemId", itemId);
        requireText("matrixSummaryId", matrixSummaryId);
        requireText("reviewerQuestion", reviewerQuestion);
        requireText("observedValue", observedValue);
        requireText("expectedValue", expectedValue);
        requireText("dispositionLabel", dispositionLabel);
        requireText("safetyBoundary", safetyBoundary);
        requireText("notProvenBoundary", notProvenBoundary);
    }

    String deterministicText() {
        return String.join(" ",
                itemId,
                matrixSummaryId,
                reviewerQuestion,
                observedValue,
                expectedValue,
                dispositionLabel,
                safetyBoundary,
                notProvenBoundary);
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
