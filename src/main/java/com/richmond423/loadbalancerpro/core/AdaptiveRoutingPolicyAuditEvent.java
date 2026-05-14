package com.richmond423.loadbalancerpro.core;

import java.time.Instant;
import java.util.List;

public record AdaptiveRoutingPolicyAuditEvent(
        String eventId,
        Instant timestamp,
        String mode,
        String contextId,
        String baselineDecision,
        String recommendation,
        String finalDecision,
        boolean changed,
        List<String> guardrailReasons,
        String rollbackReason,
        String explanationSummary) {

    public AdaptiveRoutingPolicyAuditEvent {
        eventId = requireNonBlank(eventId, "eventId");
        timestamp = timestamp == null ? Instant.EPOCH : timestamp;
        mode = requireNonBlank(mode, "mode");
        contextId = requireNonBlank(contextId, "contextId");
        guardrailReasons = List.copyOf(guardrailReasons == null ? List.of() : guardrailReasons);
        rollbackReason = requireNonBlank(rollbackReason, "rollbackReason");
        explanationSummary = requireNonBlank(explanationSummary, "explanationSummary");
    }

    public static AdaptiveRoutingPolicyAuditEvent from(
            String eventId,
            Instant timestamp,
            AdaptiveRoutingPolicyDecision decision) {
        return new AdaptiveRoutingPolicyAuditEvent(
                eventId,
                timestamp,
                decision.mode(),
                decision.contextId(),
                decision.baselineDecision(),
                decision.recommendedDecision(),
                decision.finalDecision(),
                decision.changed(),
                decision.guardrailReasons(),
                decision.rollbackReason(),
                decision.explanationSummary());
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
