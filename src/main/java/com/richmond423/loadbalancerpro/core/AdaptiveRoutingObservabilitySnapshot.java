package com.richmond423.loadbalancerpro.core;

import java.util.Map;

public record AdaptiveRoutingObservabilitySnapshot(
        long labRunsCreated,
        long labScenariosExecuted,
        long explanationCoverageCount,
        long recommendationsProduced,
        long activeExperimentChangesAllowed,
        long rollbackFailClosedEvents,
        long auditEventsRetained,
        long auditEventsDropped,
        Map<String, Long> policyDecisionsByMode,
        Map<String, Long> guardrailBlocksByReason,
        Map<String, Long> rateLimitEventsBySurface,
        String warning) {

    public AdaptiveRoutingObservabilitySnapshot {
        policyDecisionsByMode = Map.copyOf(policyDecisionsByMode == null ? Map.of() : policyDecisionsByMode);
        guardrailBlocksByReason = Map.copyOf(guardrailBlocksByReason == null ? Map.of() : guardrailBlocksByReason);
        rateLimitEventsBySurface = Map.copyOf(rateLimitEventsBySurface == null ? Map.of() : rateLimitEventsBySurface);
        warning = warning == null || warning.isBlank()
                ? "process-local lab-grade metrics; not production SLO certification"
                : warning.trim();
    }
}
