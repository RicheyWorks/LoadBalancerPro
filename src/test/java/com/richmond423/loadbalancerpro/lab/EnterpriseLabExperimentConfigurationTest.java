package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabExperimentConfigurationTest {
    private static final Instant CREATED_AT = Instant.parse("2026-07-16T20:00:00Z");
    private final EnterpriseLabAdaptiveDecision decision = new EnterpriseLabAdaptiveDecisionService().decide(
            "tail-latency-pressure", "active-experiment", true, false, false);
    private final EnterpriseLabLoopbackAllocationSnapshot baseline = baseline(decision);

    @Test
    void configurationIsImmutableBoundedAndFingerprintable() {
        EnterpriseLabExperimentConfiguration first = configuration(
                decision, baseline, true, 25, Duration.ofSeconds(45), 10, 3,
                EnterpriseLabExperimentRollbackPolicy.localLabDefaults(),
                CREATED_AT, CREATED_AT.plusSeconds(90));
        EnterpriseLabExperimentConfiguration second = configuration(
                decision, baseline, true, 25, Duration.ofSeconds(45), 10, 3,
                EnterpriseLabExperimentRollbackPolicy.localLabDefaults(),
                CREATED_AT, CREATED_AT.plusSeconds(90));

        assertEquals(first, second);
        assertEquals(first.contentFingerprint(), second.contentFingerprint());
        assertTrue(first.contentFingerprint().matches("[0-9a-f]{64}"));
        assertEquals("tail-latency-pressure", first.scenarioId());
        assertEquals(decision.decision().decisionId(), first.candidateDecisionId());

        var changed = configuration(
                decision, baseline, true, 24, Duration.ofSeconds(45), 10, 3,
                EnterpriseLabExperimentRollbackPolicy.localLabDefaults(),
                CREATED_AT, CREATED_AT.plusSeconds(90));
        assertNotEquals(first.contentFingerprint(), changed.contentFingerprint());
    }

    @Test
    void configurationRejectsUnboundedCountsDurationsCyclesAndExpiration() {
        assertThrows(IllegalArgumentException.class, () -> configuration(
                decision, baseline, true, 0, Duration.ofSeconds(10), 1, 1,
                policy(), CREATED_AT, CREATED_AT.plusSeconds(20)));
        assertThrows(IllegalArgumentException.class, () -> configuration(
                decision, baseline, true, 10_001, Duration.ofSeconds(10), 1, 1,
                policy(), CREATED_AT, CREATED_AT.plusSeconds(20)));
        assertThrows(IllegalArgumentException.class, () -> configuration(
                decision, baseline, true, 10, Duration.ofMinutes(11), 1, 1,
                policy(), CREATED_AT, CREATED_AT.plus(Duration.ofMinutes(12))));
        assertThrows(IllegalArgumentException.class, () -> configuration(
                decision, baseline, true, 10, Duration.ofSeconds(30), 11, 1,
                policy(), CREATED_AT, CREATED_AT.plusSeconds(60)));
        assertThrows(IllegalArgumentException.class, () -> configuration(
                decision, baseline, true, 10, Duration.ofSeconds(30), 1, 1_001,
                policy(), CREATED_AT, CREATED_AT.plusSeconds(60)));
        assertThrows(IllegalArgumentException.class, () -> configuration(
                decision, baseline, true, 10, Duration.ofSeconds(60), 1, 1,
                policy(), CREATED_AT, CREATED_AT.plusSeconds(30)));
        assertThrows(IllegalArgumentException.class, () -> configuration(
                decision, baseline, true, 10, Duration.ofSeconds(60), 1, 1,
                policy(), CREATED_AT, CREATED_AT.plus(Duration.ofMinutes(16))));
    }

    @Test
    void configurationRejectsMismatchedBaselineAndRollbackThresholds() {
        EnterpriseLabAdaptiveDecision other = new EnterpriseLabAdaptiveDecisionService().decide(
                "normal-balanced-load", "active-experiment", true, false, false);
        assertThrows(IllegalArgumentException.class, () -> configuration(
                decision, baseline(other), true, 10, Duration.ofSeconds(30), 3, 2,
                policy(), CREATED_AT, CREATED_AT.plusSeconds(60)));

        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabExperimentRollbackPolicy(-0.1, 0.1, 1.5, 2, 3));
        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabExperimentRollbackPolicy(0.2, 1.1, 1.5, 2, 3));
        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabExperimentRollbackPolicy(0.2, 0.1, 0.9, 2, 3));
        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabExperimentRollbackPolicy(0.2, 0.1, 1.5, 65, 3));
        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabExperimentRollbackPolicy(0.2, 0.1, 1.5, 2, 1_001));
    }

    private static EnterpriseLabExperimentConfiguration configuration(
            EnterpriseLabAdaptiveDecision adaptiveDecision,
            EnterpriseLabLoopbackAllocationSnapshot baselineSnapshot,
            boolean authorized,
            int maximumRequests,
            Duration maximumDuration,
            int minimumEvidence,
            int holdCycles,
            EnterpriseLabExperimentRollbackPolicy rollbackPolicy,
            Instant createdAt,
            Instant expiresAt) {
        return new EnterpriseLabExperimentConfiguration(
                EnterpriseLabExperimentConfiguration.SCHEMA_VERSION,
                "experiment-configuration-test",
                adaptiveDecision,
                baselineSnapshot,
                maximumRequests,
                maximumDuration,
                minimumEvidence,
                holdCycles,
                rollbackPolicy,
                AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT,
                authorized,
                createdAt,
                expiresAt);
    }

    private static EnterpriseLabLoopbackAllocationSnapshot baseline(EnterpriseLabAdaptiveDecision value) {
        Map<String, Double> allocation = value.decision().guardrailDecision().baselineAllocations();
        return EnterpriseLabLoopbackAllocationSnapshot.normalized(
                value.scenarioId(),
                0,
                "recorded-baseline",
                Kind.BASELINE,
                new ArrayList<>(allocation.keySet()),
                allocation);
    }

    private static EnterpriseLabExperimentRollbackPolicy policy() {
        return EnterpriseLabExperimentRollbackPolicy.localLabDefaults();
    }
}
