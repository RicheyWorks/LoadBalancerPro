package com.richmond423.loadbalancerpro.lab;

record LocalLabLoopbackTrafficSmokeObservation(
        String scenarioId,
        String backendId,
        String behaviorType,
        String host,
        int assignedPort,
        int expectedStatusCode,
        int responseStatusCode,
        String responseBodySummary,
        boolean matchedExpectedFixture,
        boolean evidenceLabelFound,
        boolean safetyBoundaryLabelFound,
        boolean notProvenBoundaryLabelFound,
        boolean boundaryResponse,
        String localOnlyBoundaryLabel) {

    LocalLabLoopbackTrafficSmokeObservation {
        requireText("scenarioId", scenarioId);
        requireText("backendId", backendId);
        requireText("behaviorType", behaviorType);
        requireText("host", host);
        if (assignedPort <= 0) {
            throw new IllegalArgumentException("assignedPort must be positive");
        }
        if (expectedStatusCode < 100 || expectedStatusCode > 599) {
            throw new IllegalArgumentException("expectedStatusCode must be an HTTP-style status code");
        }
        if (responseStatusCode < 100 || responseStatusCode > 599) {
            throw new IllegalArgumentException("responseStatusCode must be an HTTP-style status code");
        }
        requireText("responseBodySummary", responseBodySummary);
        requireText("localOnlyBoundaryLabel", localOnlyBoundaryLabel);
    }

    String deterministicText() {
        return String.join(" ",
                scenarioId,
                backendId,
                behaviorType,
                host,
                Integer.toString(assignedPort),
                Integer.toString(expectedStatusCode),
                Integer.toString(responseStatusCode),
                responseBodySummary,
                Boolean.toString(matchedExpectedFixture),
                Boolean.toString(evidenceLabelFound),
                Boolean.toString(safetyBoundaryLabelFound),
                Boolean.toString(notProvenBoundaryLabelFound),
                Boolean.toString(boundaryResponse),
                localOnlyBoundaryLabel);
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
