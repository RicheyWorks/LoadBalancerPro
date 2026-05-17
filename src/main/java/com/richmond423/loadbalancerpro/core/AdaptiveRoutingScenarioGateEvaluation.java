package com.richmond423.loadbalancerpro.core;

import java.util.List;
import java.util.Objects;

public record AdaptiveRoutingScenarioGateEvaluation(
        String evaluationName,
        String evaluationVersion,
        String mode,
        boolean deterministic,
        String enforcementStatus,
        String decision,
        String dashboardPath,
        String apiPath,
        String packetSourceEndpoint,
        String packetVersion,
        List<String> sourceEndpoints,
        int scenarioCount,
        int strategyCount,
        int totalDecisions,
        int totalFindings,
        int passedFindings,
        int warningFindings,
        int failedFindings,
        List<AdaptiveRoutingScenarioGateFinding> findings,
        String reviewerSummary,
        List<String> reviewerActions,
        List<String> recommendedNextSteps,
        List<String> notProvenBoundaries,
        List<String> safetyBoundaries) {

    public AdaptiveRoutingScenarioGateEvaluation {
        evaluationName = requireNonBlank(evaluationName, "evaluationName");
        evaluationVersion = requireNonBlank(evaluationVersion, "evaluationVersion");
        mode = requireNonBlank(mode, "mode");
        enforcementStatus = requireNonBlank(enforcementStatus, "enforcementStatus");
        decision = requireNonBlank(decision, "decision");
        dashboardPath = requireNonBlank(dashboardPath, "dashboardPath");
        apiPath = requireNonBlank(apiPath, "apiPath");
        packetSourceEndpoint = requireNonBlank(packetSourceEndpoint, "packetSourceEndpoint");
        packetVersion = requireNonBlank(packetVersion, "packetVersion");
        Objects.requireNonNull(sourceEndpoints, "sourceEndpoints cannot be null");
        requireNonNegative(scenarioCount, "scenarioCount");
        requireNonNegative(strategyCount, "strategyCount");
        requireNonNegative(totalDecisions, "totalDecisions");
        requireNonNegative(totalFindings, "totalFindings");
        requireNonNegative(passedFindings, "passedFindings");
        requireNonNegative(warningFindings, "warningFindings");
        requireNonNegative(failedFindings, "failedFindings");
        Objects.requireNonNull(findings, "findings cannot be null");
        reviewerSummary = requireNonBlank(reviewerSummary, "reviewerSummary");
        Objects.requireNonNull(reviewerActions, "reviewerActions cannot be null");
        Objects.requireNonNull(recommendedNextSteps, "recommendedNextSteps cannot be null");
        Objects.requireNonNull(notProvenBoundaries, "notProvenBoundaries cannot be null");
        Objects.requireNonNull(safetyBoundaries, "safetyBoundaries cannot be null");
        sourceEndpoints = List.copyOf(sourceEndpoints);
        findings = List.copyOf(findings);
        reviewerActions = List.copyOf(reviewerActions);
        recommendedNextSteps = List.copyOf(recommendedNextSteps);
        notProvenBoundaries = List.copyOf(notProvenBoundaries);
        safetyBoundaries = List.copyOf(safetyBoundaries);
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
