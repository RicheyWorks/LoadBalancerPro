package com.richmond423.loadbalancerpro.core;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AdaptiveRoutingScenarioEvidencePacket(
        String packetName,
        String packetVersion,
        String mode,
        boolean deterministic,
        Instant generatedAt,
        String generatedAtPolicy,
        String dashboardPath,
        String apiPath,
        List<String> sourceEndpoints,
        AdaptiveRoutingScenarioSummary scenarioSummary,
        AdaptiveRoutingScenarioDrilldown scenarioDrilldowns,
        List<String> strategiesCompared,
        int totalDecisions,
        Map<String, Map<String, Map<String, Integer>>> selectedServerDistributions,
        List<String> explanationNotes,
        List<String> warnings,
        String readinessForCiGate,
        List<String> localEvidencePaths,
        List<String> notProvenBoundaries,
        List<String> safetyBoundaries,
        List<String> reviewerChecklist,
        List<String> recommendedNextSteps,
        List<AdaptiveRoutingScenarioEvidenceSection> evidenceSections) {

    public AdaptiveRoutingScenarioEvidencePacket {
        packetName = requireNonBlank(packetName, "packetName");
        packetVersion = requireNonBlank(packetVersion, "packetVersion");
        mode = requireNonBlank(mode, "mode");
        Objects.requireNonNull(generatedAt, "generatedAt cannot be null");
        generatedAtPolicy = requireNonBlank(generatedAtPolicy, "generatedAtPolicy");
        dashboardPath = requireNonBlank(dashboardPath, "dashboardPath");
        apiPath = requireNonBlank(apiPath, "apiPath");
        Objects.requireNonNull(sourceEndpoints, "sourceEndpoints cannot be null");
        Objects.requireNonNull(scenarioSummary, "scenarioSummary cannot be null");
        Objects.requireNonNull(scenarioDrilldowns, "scenarioDrilldowns cannot be null");
        Objects.requireNonNull(strategiesCompared, "strategiesCompared cannot be null");
        requireNonNegative(totalDecisions, "totalDecisions");
        Objects.requireNonNull(selectedServerDistributions, "selectedServerDistributions cannot be null");
        Objects.requireNonNull(explanationNotes, "explanationNotes cannot be null");
        Objects.requireNonNull(warnings, "warnings cannot be null");
        readinessForCiGate = requireNonBlank(readinessForCiGate, "readinessForCiGate");
        Objects.requireNonNull(localEvidencePaths, "localEvidencePaths cannot be null");
        Objects.requireNonNull(notProvenBoundaries, "notProvenBoundaries cannot be null");
        Objects.requireNonNull(safetyBoundaries, "safetyBoundaries cannot be null");
        Objects.requireNonNull(reviewerChecklist, "reviewerChecklist cannot be null");
        Objects.requireNonNull(recommendedNextSteps, "recommendedNextSteps cannot be null");
        Objects.requireNonNull(evidenceSections, "evidenceSections cannot be null");
        sourceEndpoints = List.copyOf(sourceEndpoints);
        strategiesCompared = List.copyOf(strategiesCompared);
        selectedServerDistributions = immutableScenarioStrategyCounts(selectedServerDistributions);
        explanationNotes = List.copyOf(explanationNotes);
        warnings = List.copyOf(warnings);
        localEvidencePaths = List.copyOf(localEvidencePaths);
        notProvenBoundaries = List.copyOf(notProvenBoundaries);
        safetyBoundaries = List.copyOf(safetyBoundaries);
        reviewerChecklist = List.copyOf(reviewerChecklist);
        recommendedNextSteps = List.copyOf(recommendedNextSteps);
        evidenceSections = List.copyOf(evidenceSections);
    }

    private static Map<String, Map<String, Map<String, Integer>>> immutableScenarioStrategyCounts(
            Map<String, Map<String, Map<String, Integer>>> input) {
        Map<String, Map<String, Map<String, Integer>>> scenarioCopy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Integer>>> scenarioEntry : input.entrySet()) {
            Map<String, Map<String, Integer>> strategyCopy = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Integer>> strategyEntry : scenarioEntry.getValue().entrySet()) {
                strategyCopy.put(
                        strategyEntry.getKey(),
                        Collections.unmodifiableMap(new LinkedHashMap<>(strategyEntry.getValue())));
            }
            scenarioCopy.put(scenarioEntry.getKey(), Collections.unmodifiableMap(strategyCopy));
        }
        return Collections.unmodifiableMap(scenarioCopy);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }
}
