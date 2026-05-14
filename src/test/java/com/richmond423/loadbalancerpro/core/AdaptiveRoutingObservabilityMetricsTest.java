package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

class AdaptiveRoutingObservabilityMetricsTest {

    @Test
    void recordsPolicyModesGuardrailsAuditBoundsAndPrometheusOutput() {
        AdaptiveRoutingObservabilityMetrics metrics = new AdaptiveRoutingObservabilityMetrics();
        AdaptiveRoutingPolicyAuditLog auditLog = new AdaptiveRoutingPolicyAuditLog(1,
                Clock.fixed(Instant.parse("2026-05-14T00:00:00Z"), ZoneOffset.UTC),
                metrics);

        auditLog.record(decision("shadow", false, false, "shadow mode observes only",
                "shadow mode cannot alter allocation"));
        auditLog.record(decision("active-experiment", true, true, "all active-experiment policy gates passed",
                "operator can return policy mode to off"));
        metrics.recordLabRun("active-experiment", 3, 3);
        metrics.recordRateLimited("api-lab");

        AdaptiveRoutingObservabilitySnapshot snapshot = metrics.snapshot();
        assertEquals(1, snapshot.labRunsCreated());
        assertEquals(3, snapshot.labScenariosExecuted());
        assertEquals(3, snapshot.explanationCoverageCount());
        assertEquals(2, snapshot.recommendationsProduced());
        assertEquals(1, snapshot.activeExperimentChangesAllowed());
        assertEquals(2, snapshot.auditEventsRetained());
        assertEquals(1, snapshot.auditEventsDropped());
        assertEquals(1, snapshot.policyDecisionsByMode().get("shadow"));
        assertEquals(1, snapshot.policyDecisionsByMode().get("active_experiment"));
        assertEquals(1, snapshot.guardrailBlocksByReason().get("shadow_mode_observes_only"));
        assertEquals(1, snapshot.rateLimitEventsBySurface().get("api_lab"));

        String prometheus = AdaptiveRoutingPrometheusFormatter.format(snapshot);
        assertTrue(prometheus.contains("loadbalancerpro_lab_runs_total 1"));
        assertTrue(prometheus.contains("loadbalancerpro_lase_policy_decisions_total{mode=\"shadow\"} 1"));
        assertTrue(prometheus.contains("loadbalancerpro_api_rate_limit_events_total{surface=\"api_lab\"} 1"));
        assertFalse(prometheus.toLowerCase().contains("x-api-key"));
        assertFalse(prometheus.toLowerCase().contains("bearer "));
    }

    private static AdaptiveRoutingPolicyDecision decision(
            String mode,
            boolean influenceAllowed,
            boolean changed,
            String guardrail,
            String rollbackReason) {
        return new AdaptiveRoutingPolicyDecision(
                "test-context",
                mode,
                influenceAllowed,
                true,
                "baseline",
                "candidate",
                "NO_ACTION",
                changed ? "candidate" : "baseline",
                changed,
                List.of(guardrail),
                rollbackReason,
                "test explanation",
                java.util.Map.of());
    }
}
