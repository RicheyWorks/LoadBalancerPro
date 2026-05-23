package com.richmond423.loadbalancerpro.lab;

import java.util.List;

final class LocalLabBoundedRequestBurstCatalog {
    static final int FIXED_REPETITIONS_PER_CASE = 2;
    static final String UNKNOWN_BURST_SCENARIO_ID =
            LocalLabTrafficMatrixCatalog.UNKNOWN_MATRIX_SCENARIO_ID;
    static final String UNKNOWN_BURST_BACKEND_ID =
            LocalLabTrafficMatrixCatalog.UNKNOWN_MATRIX_BACKEND_ID;

    private static final String NOT_PRODUCTION_PROOF_LABEL =
            "Local bounded request burst smoke is not production proof.";
    private static final List<LocalLabBoundedRequestBurstCase> CASES = buildCases();

    private LocalLabBoundedRequestBurstCatalog() {
    }

    static List<LocalLabBoundedRequestBurstCase> cases() {
        return CASES;
    }

    private static List<LocalLabBoundedRequestBurstCase> buildCases() {
        return LocalLabTrafficMatrixCatalog.cases().stream()
                .map(LocalLabBoundedRequestBurstCatalog::burstCase)
                .toList();
    }

    private static LocalLabBoundedRequestBurstCase burstCase(LocalLabTrafficMatrixCase matrixCase) {
        return new LocalLabBoundedRequestBurstCase(
                matrixCase.matrixCaseId().replace("matrix-case-", "burst-case-"),
                matrixCase.scenarioId(),
                matrixCase.backendId(),
                matrixCase.requestLabel(),
                FIXED_REPETITIONS_PER_CASE,
                matrixCase.expectedStatusCode(),
                matrixCase.expectedLatencyLabel(),
                matrixCase.expectedErrorLoadLabel(),
                matrixCase.expectedResponseLabel(),
                matrixCase.expectedEvidenceLabel(),
                matrixCase.expectedSafetyBoundaryLabel(),
                matrixCase.expectedNotProvenBoundaryLabel(),
                NOT_PRODUCTION_PROOF_LABEL,
                matrixCase.boundaryCase());
    }
}
