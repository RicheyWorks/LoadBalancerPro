package com.richmond423.loadbalancerpro.api;

import java.util.List;

import com.richmond423.loadbalancerpro.core.LaseEvaluationReport;

public record LaseAllocationShadowSummary(
        boolean enabled,
        String mode,
        List<String> signalsConsidered,
        boolean observationRecorded,
        boolean alteredLiveDecision,
        String recommendedServerId,
        String recommendedAction,
        boolean failSafe,
        String decisionImpact,
        String reason) {

    private static final List<String> SIGNALS_CONSIDERED = List.of(
            "tail latency",
            "queue depth",
            "error rate",
            "adaptive concurrency",
            "load shedding",
            "shadow autoscaling",
            "failure scenario");

    public LaseAllocationShadowSummary {
        mode = requireNonBlank(mode, "mode");
        signalsConsidered = List.copyOf(signalsConsidered == null ? List.of() : signalsConsidered);
        decisionImpact = requireNonBlank(decisionImpact, "decisionImpact");
        reason = requireNonBlank(reason, "reason");
    }

    public static LaseAllocationShadowSummary disabled() {
        return new LaseAllocationShadowSummary(
                false,
                "disabled",
                List.of(),
                false,
                false,
                null,
                null,
                false,
                "LASE shadow mode is disabled; live allocation and read-only evaluation behavior are unchanged.",
                "Enable loadbalancerpro.lase.shadow.enabled=true to record shadow-only adaptive-routing signals.");
    }

    public static LaseAllocationShadowSummary observed(LaseEvaluationReport report) {
        String recommendedServerId = report.routingDecision().chosenServer()
                .map(server -> server.serverId())
                .or(() -> report.routingDecision().explanation().chosenServerId())
                .orElse(null);
        return new LaseAllocationShadowSummary(
                true,
                "shadow-only",
                SIGNALS_CONSIDERED,
                true,
                false,
                recommendedServerId,
                report.autoscalingRecommendation().action().name(),
                false,
                "Shadow-only observation recorded; live allocation was not altered.",
                report.summary());
    }

    public static LaseAllocationShadowSummary failSafe(String reason) {
        return new LaseAllocationShadowSummary(
                true,
                "shadow-only",
                SIGNALS_CONSIDERED,
                false,
                false,
                null,
                "FAIL_SAFE",
                true,
                "LASE shadow observation failed safely; live allocation was not altered.",
                reason);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
