package com.richmond423.loadbalancerpro.lab;

record LocalLabPassiveReviewerChecklistItem(
        String checklistItemId,
        String scenarioId,
        String transcriptId,
        String reviewerQuestion,
        String observedSummaryValue,
        String evidenceExpectation,
        String safetyBoundary,
        String notProvenBoundary,
        String reviewerDispositionLabel) {

    LocalLabPassiveReviewerChecklistItem {
        requireText("checklistItemId", checklistItemId);
        requireText("scenarioId", scenarioId);
        requireText("transcriptId", transcriptId);
        requireText("reviewerQuestion", reviewerQuestion);
        requireText("observedSummaryValue", observedSummaryValue);
        requireText("evidenceExpectation", evidenceExpectation);
        requireText("safetyBoundary", safetyBoundary);
        requireText("notProvenBoundary", notProvenBoundary);
        requireText("reviewerDispositionLabel", reviewerDispositionLabel);
    }

    String deterministicText() {
        return String.join(" ",
                checklistItemId,
                scenarioId,
                transcriptId,
                reviewerQuestion,
                observedSummaryValue,
                evidenceExpectation,
                safetyBoundary,
                notProvenBoundary,
                reviewerDispositionLabel);
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
