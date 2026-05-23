package com.richmond423.loadbalancerpro.lab;

import java.util.ArrayList;
import java.util.List;

final class LocalLabTrafficMatrixCatalog {
    static final String UNKNOWN_MATRIX_SCENARIO_ID = "unknown-local-lab-traffic-matrix-scenario";
    static final String UNKNOWN_MATRIX_BACKEND_ID = "unknown-local-lab-traffic-matrix-backend";
    private static final String HANDLER_SAFETY_LABEL = "Test-scope fake backend handler only";
    private static final String NOT_PROVEN_LABEL = "not production proof";
    private static final String NOT_PRODUCTION_PROOF_LABEL =
            "Local loopback traffic matrix is not production proof.";

    private static final List<LocalLabTrafficMatrixCase> CASES = buildCases();

    private LocalLabTrafficMatrixCatalog() {
    }

    static List<LocalLabTrafficMatrixCase> cases() {
        return CASES;
    }

    private static List<LocalLabTrafficMatrixCase> buildCases() {
        List<LocalLabTrafficMatrixCase> cases = new ArrayList<>();
        for (LocalLabFakeBackendResponseFixture fixture : LocalLabFakeBackendResponseFixtureCatalog.fixtures()) {
            LocalLabPassiveTranscriptEntry entry =
                    LocalLabPassiveTranscriptCatalog.findByScenarioId(fixture.scenarioId())
                            .orElseThrow()
                            .entries()
                            .get(0);
            cases.add(new LocalLabTrafficMatrixCase(
                    "matrix-case-" + fixture.scenarioId(),
                    fixture.scenarioId(),
                    fixture.backendId(),
                    entry.simulatedRequestMethodLabel() + " " + entry.simulatedRequestPathLabel(),
                    fixture.responseStatusCode(),
                    fixture.responseLatencyLabel(),
                    fixture.simulatedErrorLabel() + " | " + fixture.simulatedLoadLabel(),
                    fixture.responseBodySummary(),
                    fixture.evidenceNote(),
                    HANDLER_SAFETY_LABEL,
                    NOT_PROVEN_LABEL,
                    NOT_PRODUCTION_PROOF_LABEL,
                    false));
        }
        cases.add(new LocalLabTrafficMatrixCase(
                "matrix-case-unknown-label-boundary",
                UNKNOWN_MATRIX_SCENARIO_ID,
                UNKNOWN_MATRIX_BACKEND_ID,
                "GET label /local-lab/traffic-matrix/unknown path label",
                404,
                "boundary response with no fixture latency label",
                "unknown scenario or backend label | load not evaluated because no fixture matched",
                "Boundary response for unknown local lab fake backend labels.",
                "no matching test-scope response fixture",
                HANDLER_SAFETY_LABEL,
                NOT_PROVEN_LABEL,
                NOT_PRODUCTION_PROOF_LABEL,
                true));
        return List.copyOf(cases);
    }
}
