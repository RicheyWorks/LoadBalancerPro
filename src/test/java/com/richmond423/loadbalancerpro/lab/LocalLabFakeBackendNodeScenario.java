package com.richmond423.loadbalancerpro.lab;

record LocalLabFakeBackendNodeScenario(
        String backendId,
        String backendName,
        LocalLabFakeBackendBehaviorProfile behaviorProfile,
        String evidenceExpectationSummary,
        String notProvenBoundary) {

    LocalLabFakeBackendNodeScenario {
        requireText("backendId", backendId);
        requireText("backendName", backendName);
        if (behaviorProfile == null) {
            throw new IllegalArgumentException("behaviorProfile is required");
        }
        requireText("evidenceExpectationSummary", evidenceExpectationSummary);
        requireText("notProvenBoundary", notProvenBoundary);
    }

    String behaviorType() {
        return behaviorProfile.name();
    }

    String behaviorLabel() {
        return behaviorProfile.behaviorLabel();
    }

    String expectedLatencyBand() {
        return behaviorProfile.expectedLatencyBand();
    }

    String expectedErrorBehavior() {
        return behaviorProfile.expectedErrorBehavior();
    }

    String expectedLoadQueueBehavior() {
        return behaviorProfile.expectedLoadQueueBehavior();
    }

    String expectedHealthPosture() {
        return behaviorProfile.expectedHealthPosture();
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
