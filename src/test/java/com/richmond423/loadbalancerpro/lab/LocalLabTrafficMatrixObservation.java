package com.richmond423.loadbalancerpro.lab;

record LocalLabTrafficMatrixObservation(
        String matrixCaseId,
        String scenarioId,
        String backendId,
        String loopbackHost,
        int assignedPort,
        int responseStatusCode,
        boolean fixtureMatched,
        boolean evidenceLabelObserved,
        boolean safetyBoundaryObserved,
        boolean notProvenBoundaryObserved,
        boolean boundaryResponse,
        String localOnlyConfirmation) {

    LocalLabTrafficMatrixObservation {
        requireText("matrixCaseId", matrixCaseId);
        requireText("scenarioId", scenarioId);
        requireText("backendId", backendId);
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
                matrixCaseId,
                scenarioId,
                backendId,
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
