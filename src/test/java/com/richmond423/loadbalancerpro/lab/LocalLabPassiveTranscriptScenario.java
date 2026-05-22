package com.richmond423.loadbalancerpro.lab;

import java.util.List;

record LocalLabPassiveTranscriptScenario(
        String transcriptId,
        String scenarioId,
        String fixtureId,
        LocalLabFakeBackendBehaviorProfile behaviorProfile,
        List<LocalLabPassiveTranscriptEntry> entries,
        String notProvenBoundary) {

    LocalLabPassiveTranscriptScenario {
        requireText("transcriptId", transcriptId);
        requireText("scenarioId", scenarioId);
        requireText("fixtureId", fixtureId);
        if (behaviorProfile == null) {
            throw new IllegalArgumentException("behaviorProfile is required");
        }
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("entries are required");
        }
        entries = List.copyOf(entries);
        requireText("notProvenBoundary", notProvenBoundary);
        validateEntries(transcriptId, scenarioId, fixtureId, entries);
    }

    String behaviorType() {
        return behaviorProfile.name();
    }

    private static void validateEntries(
            String transcriptId,
            String scenarioId,
            String fixtureId,
            List<LocalLabPassiveTranscriptEntry> entries) {
        int previousStep = 0;
        for (LocalLabPassiveTranscriptEntry entry : entries) {
            if (!transcriptId.equals(entry.transcriptId())) {
                throw new IllegalArgumentException("entry transcriptId must match transcript");
            }
            if (!scenarioId.equals(entry.scenarioId())) {
                throw new IllegalArgumentException("entry scenarioId must match transcript");
            }
            if (!fixtureId.equals(entry.fixtureId())) {
                throw new IllegalArgumentException("entry fixtureId must match transcript");
            }
            if (entry.stepNumber() <= previousStep) {
                throw new IllegalArgumentException("entry step numbers must be sorted and unique");
            }
            previousStep = entry.stepNumber();
        }
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
