package com.richmond423.loadbalancerpro.lab;

record LocalLabFakeBackendResponseFixture(
        String fixtureId,
        String scenarioId,
        String backendId,
        LocalLabFakeBackendBehaviorProfile behaviorProfile,
        int responseStatusCode,
        String responseLatencyLabel,
        String responseBodySummary,
        String simulatedErrorLabel,
        String simulatedLoadLabel,
        String evidenceNote,
        String notProvenBoundary) {

    LocalLabFakeBackendResponseFixture {
        requireText("fixtureId", fixtureId);
        requireText("scenarioId", scenarioId);
        requireText("backendId", backendId);
        if (behaviorProfile == null) {
            throw new IllegalArgumentException("behaviorProfile is required");
        }
        if (responseStatusCode < 100 || responseStatusCode > 599) {
            throw new IllegalArgumentException("responseStatusCode must be an HTTP-style status code");
        }
        requireText("responseLatencyLabel", responseLatencyLabel);
        requireText("responseBodySummary", responseBodySummary);
        requireText("simulatedErrorLabel", simulatedErrorLabel);
        requireText("simulatedLoadLabel", simulatedLoadLabel);
        requireText("evidenceNote", evidenceNote);
        requireText("notProvenBoundary", notProvenBoundary);
    }

    String behaviorType() {
        return behaviorProfile.name();
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
