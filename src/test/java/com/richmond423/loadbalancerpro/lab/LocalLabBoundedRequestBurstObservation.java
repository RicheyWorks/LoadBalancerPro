package com.richmond423.loadbalancerpro.lab;

record LocalLabBoundedRequestBurstObservation(
        String burstCaseId,
        String scenarioId,
        String backendId,
        int repetitionIndex,
        String loopbackHost,
        int assignedPort,
        int responseStatusCode,
        boolean fixtureMatched,
        boolean evidenceLabelObserved,
        boolean safetyBoundaryObserved,
        boolean notProvenBoundaryObserved,
        boolean boundaryResponse,
        String localOnlyConfirmation) {

    LocalLabBoundedRequestBurstObservation {
        requireText("burstCaseId", burstCaseId);
        requireText("scenarioId", scenarioId);
        requireText("backendId", backendId);
        if (repetitionIndex < 1) {
            throw new IllegalArgumentException("repetitionIndex must be positive");
        }
        requireText("loopbackHost", loopbackHost);
        if (assignedPort <= 0) {
            throw new IllegalArgumentException("assignedPort must be positive");
        }
        if (responseStatusCode < 100 || responseStatusCode > 599) {
            throw new IllegalArgumentException("responseStatusCode must be an HTTP-style status code");
        }
        requireText("localOnlyConfirmation", localOnlyConfirmation);
    }

    String deterministicText() {
        return String.join(" ",
                burstCaseId,
                scenarioId,
                backendId,
                Integer.toString(repetitionIndex),
                loopbackHost,
                Integer.toString(assignedPort),
                Integer.toString(responseStatusCode),
                Boolean.toString(fixtureMatched),
                Boolean.toString(evidenceLabelObserved),
                Boolean.toString(safetyBoundaryObserved),
                Boolean.toString(notProvenBoundaryObserved),
                Boolean.toString(boundaryResponse),
                localOnlyConfirmation);
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
