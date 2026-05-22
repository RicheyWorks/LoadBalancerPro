package com.richmond423.loadbalancerpro.lab;

record LocalLabPassiveTranscriptEntry(
        String transcriptId,
        String scenarioId,
        String fixtureId,
        int stepNumber,
        String simulatedRequestMethodLabel,
        String simulatedRequestPathLabel,
        String backendId,
        int expectedResponseStatusCode,
        String expectedLatencyLabel,
        String expectedResponseBodySummary,
        String expectedErrorLabel,
        String expectedLoadLabel,
        String routingEvidenceObservationNote,
        String safetyBoundaryNote,
        String notProvenBoundary) {

    LocalLabPassiveTranscriptEntry {
        requireText("transcriptId", transcriptId);
        requireText("scenarioId", scenarioId);
        requireText("fixtureId", fixtureId);
        if (stepNumber < 1) {
            throw new IllegalArgumentException("stepNumber must be positive");
        }
        requireText("simulatedRequestMethodLabel", simulatedRequestMethodLabel);
        requireText("simulatedRequestPathLabel", simulatedRequestPathLabel);
        requireText("backendId", backendId);
        if (expectedResponseStatusCode < 100 || expectedResponseStatusCode > 599) {
            throw new IllegalArgumentException("expectedResponseStatusCode must be an HTTP-style status code");
        }
        requireText("expectedLatencyLabel", expectedLatencyLabel);
        requireText("expectedResponseBodySummary", expectedResponseBodySummary);
        requireText("expectedErrorLabel", expectedErrorLabel);
        requireText("expectedLoadLabel", expectedLoadLabel);
        requireText("routingEvidenceObservationNote", routingEvidenceObservationNote);
        requireText("safetyBoundaryNote", safetyBoundaryNote);
        requireText("notProvenBoundary", notProvenBoundary);
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
