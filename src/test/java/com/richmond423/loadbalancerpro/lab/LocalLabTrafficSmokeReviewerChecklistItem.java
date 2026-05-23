package com.richmond423.loadbalancerpro.lab;

record LocalLabTrafficSmokeReviewerChecklistItem(
        String itemId,
        String smokeSummaryId,
        String reviewerQuestion,
        String observedValue,
        String expectedValue,
        String dispositionLabel,
        String safetyBoundary,
        String notProvenBoundary) {

    LocalLabTrafficSmokeReviewerChecklistItem {
        requireText("itemId", itemId);
        requireText("smokeSummaryId", smokeSummaryId);
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
                smokeSummaryId,
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
