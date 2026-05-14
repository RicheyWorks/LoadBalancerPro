package com.richmond423.loadbalancerpro.core;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

public final class AdaptiveRoutingObservabilityMetrics {
    private final LongAdder labRunsCreated = new LongAdder();
    private final LongAdder labScenariosExecuted = new LongAdder();
    private final LongAdder explanationCoverage = new LongAdder();
    private final LongAdder recommendationsProduced = new LongAdder();
    private final LongAdder activeExperimentChangesAllowed = new LongAdder();
    private final LongAdder rollbackFailClosedEvents = new LongAdder();
    private final LongAdder auditEventsRetained = new LongAdder();
    private final LongAdder auditEventsDropped = new LongAdder();
    private final ConcurrentMap<String, LongAdder> policyDecisionsByMode = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> guardrailBlocksByReason = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> rateLimitEventsBySurface = new ConcurrentHashMap<>();

    public void recordLabRun(String mode, int scenarioCount, int explanationCoverageCount) {
        labRunsCreated.increment();
        labScenariosExecuted.add(Math.max(0, scenarioCount));
        explanationCoverage.add(Math.max(0, explanationCoverageCount));
    }

    public void recordPolicyDecision(AdaptiveRoutingPolicyDecision decision) {
        if (decision == null) {
            return;
        }
        increment(policyDecisionsByMode, normalizeLabel(decision.mode()));
        if (decision.recommendationAvailable()) {
            recommendationsProduced.increment();
        }
        if (decision.changed() && "active-experiment".equals(decision.mode())) {
            activeExperimentChangesAllowed.increment();
        }
        if (!decision.influenceAllowed()) {
            for (String reason : decision.guardrailReasons()) {
                increment(guardrailBlocksByReason, normalizeLabel(reason));
            }
        }
        if (isRollbackOrFailClosed(decision.rollbackReason())) {
            rollbackFailClosedEvents.increment();
        }
    }

    public void recordAuditEventRetained() {
        auditEventsRetained.increment();
    }

    public void recordAuditEventDropped() {
        auditEventsDropped.increment();
    }

    public void recordRateLimited(String surface) {
        increment(rateLimitEventsBySurface, normalizeLabel(surface));
    }

    public AdaptiveRoutingObservabilitySnapshot snapshot() {
        return new AdaptiveRoutingObservabilitySnapshot(
                labRunsCreated.sum(),
                labScenariosExecuted.sum(),
                explanationCoverage.sum(),
                recommendationsProduced.sum(),
                activeExperimentChangesAllowed.sum(),
                rollbackFailClosedEvents.sum(),
                auditEventsRetained.sum(),
                auditEventsDropped.sum(),
                sorted(policyDecisionsByMode),
                sorted(guardrailBlocksByReason),
                sorted(rateLimitEventsBySurface),
                "process-local lab-grade metrics; not production SLO certification");
    }

    private static void increment(ConcurrentMap<String, LongAdder> counters, String label) {
        counters.computeIfAbsent(label, ignored -> new LongAdder()).increment();
    }

    public static String normalizeLabel(String value) {
        if (value == null || value.isBlank()) {
            return "none";
        }
        String normalized = value.trim()
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("(^_+|_+$)", "");
        return normalized.isBlank() ? "none" : normalized;
    }

    private static boolean isRollbackOrFailClosed(String rollbackReason) {
        if (rollbackReason == null || rollbackReason.isBlank()) {
            return false;
        }
        String normalized = rollbackReason.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("rollback")
                || normalized.contains("fail-closed")
                || normalized.contains("baseline retained");
    }

    private static Map<String, Long> sorted(ConcurrentMap<String, LongAdder> counters) {
        Map<String, Long> values = new TreeMap<>();
        counters.forEach((key, value) -> values.put(key, value.sum()));
        return Collections.unmodifiableMap(values);
    }
}
