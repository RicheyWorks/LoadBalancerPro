package com.richmond423.loadbalancerpro.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AdaptiveRoutingExperimentResult(
        String scenarioName,
        String description,
        String expectedPressure,
        List<String> replayEventOrder,
        String baselineSelectedBackend,
        Map<String, Double> baselineAllocations,
        double baselineUnallocatedLoad,
        List<String> laseSignalsConsidered,
        boolean shadowObservationRecorded,
        String shadowRecommendedBackend,
        String shadowRecommendedAction,
        String shadowSummary,
        boolean activeInfluenceEnabled,
        String influencedSelectedBackend,
        Map<String, Double> influencedAllocations,
        double influencedUnallocatedLoad,
        boolean resultChanged,
        String explanation,
        String guardrailReason) {

    public AdaptiveRoutingExperimentResult {
        scenarioName = requireNonBlank(scenarioName, "scenarioName");
        description = requireNonBlank(description, "description");
        expectedPressure = requireNonBlank(expectedPressure, "expectedPressure");
        replayEventOrder = List.copyOf(replayEventOrder == null ? List.of() : replayEventOrder);
        baselineAllocations = immutableCopy(baselineAllocations);
        laseSignalsConsidered = List.copyOf(laseSignalsConsidered == null ? List.of() : laseSignalsConsidered);
        influencedAllocations = immutableCopy(influencedAllocations);
        explanation = requireNonBlank(explanation, "explanation");
        guardrailReason = requireNonBlank(guardrailReason, "guardrailReason");
    }

    private static Map<String, Double> immutableCopy(Map<String, Double> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values == null ? Map.of() : values));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
