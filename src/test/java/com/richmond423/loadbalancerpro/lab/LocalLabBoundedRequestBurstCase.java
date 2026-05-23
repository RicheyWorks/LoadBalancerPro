package com.richmond423.loadbalancerpro.lab;

record LocalLabBoundedRequestBurstCase(
        String burstCaseId,
        String scenarioId,
        String backendId,
        String requestLabel,
        int fixedRepetitionCount,
        int expectedStatusCode,
        String expectedLatencyLabel,
        String expectedErrorLoadLabel,
        String expectedResponseLabel,
        String expectedEvidenceLabel,
        String expectedSafetyBoundaryLabel,
        String expectedNotProvenBoundaryLabel,
        String notProductionProofLabel,
        boolean boundaryCase) {

    LocalLabBoundedRequestBurstCase {
        requireText("burstCaseId", burstCaseId);
        requireText("scenarioId", scenarioId);
        requireText("backendId", backendId);
        requireText("requestLabel", requestLabel);
        if (fixedRepetitionCount < 1 || fixedRepetitionCount > 3) {
            throw new IllegalArgumentException("fixedRepetitionCount must be a small fixed request count");
        }
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

    LocalLabTrafficMatrixCase toMatrixCase() {
        return new LocalLabTrafficMatrixCase(
                burstCaseId.replace("burst-case-", "matrix-case-"),
                scenarioId,
                backendId,
                requestLabel,
                expectedStatusCode,
                expectedLatencyLabel,
                expectedErrorLoadLabel,
                expectedResponseLabel,
                expectedEvidenceLabel,
                expectedSafetyBoundaryLabel,
                expectedNotProvenBoundaryLabel,
                notProductionProofLabel,
                boundaryCase);
    }

    String deterministicText() {
        return String.join(" ",
                burstCaseId,
                scenarioId,
                backendId,
                requestLabel,
                Integer.toString(fixedRepetitionCount),
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
