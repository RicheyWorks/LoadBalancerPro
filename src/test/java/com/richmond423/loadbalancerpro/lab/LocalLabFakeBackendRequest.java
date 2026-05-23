package com.richmond423.loadbalancerpro.lab;

record LocalLabFakeBackendRequest(
        String scenarioId,
        String backendId,
        String requestMethodLabel,
        String requestPathLabel,
        String requestBodySummary,
        String requestPurposeLabel) {

    LocalLabFakeBackendRequest {
        requireText("scenarioId", scenarioId);
        requireText("backendId", backendId);
        requireText("requestMethodLabel", requestMethodLabel);
        requireText("requestPathLabel", requestPathLabel);
        requireText("requestBodySummary", requestBodySummary);
        requireText("requestPurposeLabel", requestPurposeLabel);
    }

    String deterministicText() {
        return String.join(" ",
                scenarioId,
                backendId,
                requestMethodLabel,
                requestPathLabel,
                requestBodySummary,
                requestPurposeLabel);
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
