package com.richmond423.loadbalancerpro.lab;

record LocalLabImplementationReadinessCriterion(
        String criterionId,
        String criterionDescription,
        boolean passed,
        String evidenceSummary,
        String safetyBoundary,
        String notProvenBoundary) {

    LocalLabImplementationReadinessCriterion {
        requireText("criterionId", criterionId);
        requireText("criterionDescription", criterionDescription);
        requireText("evidenceSummary", evidenceSummary);
        requireText("safetyBoundary", safetyBoundary);
        requireText("notProvenBoundary", notProvenBoundary);
    }

    String statusLabel() {
        return passed ? "PASS" : "BLOCKED";
    }

    String deterministicText() {
        return String.join(" ",
                criterionId,
                criterionDescription,
                statusLabel(),
                evidenceSummary,
                safetyBoundary,
                notProvenBoundary);
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
