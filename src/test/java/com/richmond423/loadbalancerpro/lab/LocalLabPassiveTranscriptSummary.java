package com.richmond423.loadbalancerpro.lab;

import java.util.List;

record LocalLabPassiveTranscriptSummary(
        String summaryId,
        String scenarioId,
        String scenarioBehaviorType,
        String transcriptId,
        int orderedStepCount,
        List<String> backendIdsObserved,
        List<String> requestLabelsObserved,
        List<String> responseStatusLabelsObserved,
        List<String> latencyLabelsObserved,
        List<String> errorLoadLabelsObserved,
        String evidenceNoteSummary,
        String safetyBoundarySummary,
        String notProvenBoundarySummary) {

    LocalLabPassiveTranscriptSummary {
        requireText("summaryId", summaryId);
        requireText("scenarioId", scenarioId);
        requireText("scenarioBehaviorType", scenarioBehaviorType);
        requireText("transcriptId", transcriptId);
        if (orderedStepCount < 1) {
            throw new IllegalArgumentException("orderedStepCount must be positive");
        }
        backendIdsObserved = copyNonEmpty("backendIdsObserved", backendIdsObserved);
        requestLabelsObserved = copyNonEmpty("requestLabelsObserved", requestLabelsObserved);
        responseStatusLabelsObserved = copyNonEmpty("responseStatusLabelsObserved", responseStatusLabelsObserved);
        latencyLabelsObserved = copyNonEmpty("latencyLabelsObserved", latencyLabelsObserved);
        errorLoadLabelsObserved = copyNonEmpty("errorLoadLabelsObserved", errorLoadLabelsObserved);
        requireText("evidenceNoteSummary", evidenceNoteSummary);
        requireText("safetyBoundarySummary", safetyBoundarySummary);
        requireText("notProvenBoundarySummary", notProvenBoundarySummary);
    }

    String deterministicText() {
        return String.join(" ",
                summaryId,
                scenarioId,
                scenarioBehaviorType,
                transcriptId,
                Integer.toString(orderedStepCount),
                String.join(" | ", backendIdsObserved),
                String.join(" | ", requestLabelsObserved),
                String.join(" | ", responseStatusLabelsObserved),
                String.join(" | ", latencyLabelsObserved),
                String.join(" | ", errorLoadLabelsObserved),
                evidenceNoteSummary,
                safetyBoundarySummary,
                notProvenBoundarySummary);
    }

    private static List<String> copyNonEmpty(String field, List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        for (String value : values) {
            requireText(field, value);
        }
        return List.copyOf(values);
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
