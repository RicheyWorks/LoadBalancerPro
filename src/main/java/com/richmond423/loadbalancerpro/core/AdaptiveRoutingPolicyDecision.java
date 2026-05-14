package com.richmond423.loadbalancerpro.core;

import java.util.List;
import java.util.Map;

public record AdaptiveRoutingPolicyDecision(
        String contextId,
        String mode,
        boolean influenceAllowed,
        boolean recommendationAvailable,
        String baselineDecision,
        String recommendedDecision,
        String recommendedAction,
        String finalDecision,
        boolean changed,
        List<String> guardrailReasons,
        String rollbackReason,
        String explanationSummary,
        Map<String, String> auditFields) {

    public AdaptiveRoutingPolicyDecision {
        contextId = requireNonBlank(contextId, "contextId");
        mode = requireNonBlank(mode, "mode");
        finalDecision = normalizeNullable(finalDecision);
        guardrailReasons = List.copyOf(guardrailReasons == null ? List.of() : guardrailReasons);
        rollbackReason = requireNonBlank(rollbackReason, "rollbackReason");
        explanationSummary = requireNonBlank(explanationSummary, "explanationSummary");
        auditFields = Map.copyOf(auditFields == null ? Map.of() : auditFields);
    }

    public static AdaptiveRoutingPolicyDecision disabled(String contextId, String baselineDecision) {
        return new AdaptiveRoutingPolicyDecision(
                contextId,
                AdaptiveRoutingPolicyMode.OFF.wireValue(),
                false,
                false,
                baselineDecision,
                null,
                null,
                baselineDecision,
                false,
                List.of("policy mode off"),
                "baseline retained because controlled active LASE policy is off",
                "Adaptive routing policy is off; baseline allocation remains final.",
                Map.of("policyMode", AdaptiveRoutingPolicyMode.OFF.wireValue()));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
