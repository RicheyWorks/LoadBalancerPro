package com.richmond423.loadbalancerpro.lab;

record LocalLabTrafficMatrixCase(
        String matrixCaseId,
        String scenarioId,
        String backendId,
        String requestLabel,
        int expectedStatusCode,
        String expectedLatencyLabel,
        String expectedErrorLoadLabel,
        String expectedResponseLabel,
        String expectedEvidenceLabel,
        String expectedSafetyBoundaryLabel,
        String expectedNotProvenBoundaryLabel,
        String notProductionProofLabel,
        boolean boundaryCase) {

    LocalLabTrafficMatrixCase {
        requireText("matrixCaseId", matrixCaseId);
        requireText("scenarioId", scenarioId);
        requireText("backendId", backendId);
        requireText("requestLabel", requestLabel);
        if (expectedStatusCode < 100 || expectedStatusCode > 599) {
            throw new IllegalArgumentException("expectedStatusCode must be an HTTP-style status code");
        }
        requireText("expectedLatencyLabel", expectedLatencyLabel);
        requireText("expectedErrorLoadLabel", expectedErrorLoadLabel);
        requireText("expectedResponseLabel", expectedResponseLabel);
        requireText("expectedEvidenceLabel", expectedEvidenceLabel);
        requireText("expectedSafetyBoundaryLabel", expectedSafetyBoundaryLabel);
        requireText("expectedNotProvenBoundaryLabel", expectedNotProvenBoundaryLabel);
        requireText("notProductionProofLabel", notProductionProofLabel);
    }

    String deterministicText() {
        return String.join(" ",
                matrixCaseId,
                scenarioId,
                backendId,
                requestLabel,
                Integer.toString(expectedStatusCode),
                expectedLatencyLabel,
                expectedErrorLoadLabel,
                expectedResponseLabel,
                expectedEvidenceLabel,
                expectedSafetyBoundaryLabel,
                expectedNotProvenBoundaryLabel,
                notProductionProofLabel,
                Boolean.toString(boundaryCase));
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
