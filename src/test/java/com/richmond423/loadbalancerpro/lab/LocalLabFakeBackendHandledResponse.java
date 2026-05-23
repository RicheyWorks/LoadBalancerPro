package com.richmond423.loadbalancerpro.lab;

record LocalLabFakeBackendHandledResponse(
        String scenarioId,
        String backendId,
        int statusCode,
        String latencyLabel,
        String bodySummary,
        String errorLabel,
        String loadLabel,
        String evidenceNote,
        String safetyBoundary,
        String notProvenBoundary) {

    LocalLabFakeBackendHandledResponse {
        requireText("scenarioId", scenarioId);
        requireText("backendId", backendId);
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("statusCode must be an HTTP-style status code");
        }
        requireText("latencyLabel", latencyLabel);
        requireText("bodySummary", bodySummary);
        requireText("errorLabel", errorLabel);
        requireText("loadLabel", loadLabel);
        requireText("evidenceNote", evidenceNote);
        requireText("safetyBoundary", safetyBoundary);
        requireText("notProvenBoundary", notProvenBoundary);
    }

    String deterministicText() {
        return String.join(" ",
                scenarioId,
                backendId,
                Integer.toString(statusCode),
                latencyLabel,
                bodySummary,
                errorLabel,
                loadLabel,
                evidenceNote,
                safetyBoundary,
                notProvenBoundary);
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
