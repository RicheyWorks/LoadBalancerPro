package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

class AdaptiveRoutingPolicyEngineTest {
    private final AdaptiveRoutingPolicyEngine engine = new AdaptiveRoutingPolicyEngine();

    @Test
    void offShadowAndRecommendNeverChangeFinalDecision() {
        for (AdaptiveRoutingPolicyMode mode : List.of(
                AdaptiveRoutingPolicyMode.OFF,
                AdaptiveRoutingPolicyMode.SHADOW,
                AdaptiveRoutingPolicyMode.RECOMMEND)) {
            AdaptiveRoutingPolicyDecision decision = engine.decide(input(mode, true, false, true, "green"));

            assertFalse(decision.changed(), mode.name());
            assertFalse(decision.influenceAllowed(), mode.name());
            assertEquals("blue", decision.finalDecision(), mode.name());
            assertTrue(decision.rollbackReason().contains("baseline")
                    || decision.rollbackReason().contains("shadow")
                    || decision.rollbackReason().contains("recommendation"));
        }
    }

    @Test
    void activeExperimentCanChangeWhenEveryGatePasses() {
        AdaptiveRoutingPolicyDecision decision = engine.decide(
                input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, true, false, true, "green"));

        assertTrue(decision.influenceAllowed());
        assertTrue(decision.changed());
        assertEquals("green", decision.finalDecision());
        assertTrue(decision.guardrailReasons().contains("all active-experiment policy gates passed"));
    }

    @Test
    void activeExperimentBlocksEveryGuardrailClassDeterministically() {
        assertBlocked(input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, false, false, true, "green"),
                "active-experiment requires explicit bounded experiment context");
        assertBlocked(input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, true, false, false, "green"),
                "stale signal");
        assertBlocked(input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, true, true, true, "green"),
                "conflicting signal");
        assertBlocked(input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, true, false, true, null),
                "no LASE backend recommendation");
        assertBlocked(input(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, true, false, true, "missing"),
                "recommended backend not eligible");
        assertBlocked(inputWithServers(List.of(server("blue", 100, true), server("green", 100, false)),
                        AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, true, false, true, "green", 50),
                "recommended backend unhealthy");
        assertBlocked(inputWithServers(List.of(server("blue", 0, true), server("green", 0, true)),
                        AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, true, false, true, "green", 10),
                "recommended backend has no available capacity");
        assertBlocked(inputWithServers(List.of(server("blue", 10, true), server("green", 10, true)),
                        AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, true, false, true, "green", 50),
                "capacity constraints failed");
        assertBlocked(inputWithServers(List.of(server("blue", 100, false), server("green", 100, false)),
                        AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, true, false, true, "green", 50),
                "all backends unhealthy");
    }

    @Test
    void auditLogIsBoundedAndRedactionFriendly() {
        AdaptiveRoutingPolicyAuditLog log = new AdaptiveRoutingPolicyAuditLog(2,
                Clock.fixed(Instant.parse("2026-05-14T00:00:00Z"), ZoneOffset.UTC));

        log.record(engine.decide(input(AdaptiveRoutingPolicyMode.OFF, true, false, true, "green")));
        log.record(engine.decide(input(AdaptiveRoutingPolicyMode.SHADOW, true, false, true, "green")));
        log.record(engine.decide(input(AdaptiveRoutingPolicyMode.RECOMMEND, true, false, true, "green")));

        assertEquals(2, log.snapshot().size());
        assertEquals("lase-policy-0002", log.snapshot().get(0).eventId());
        String combined = log.snapshot().toString().toLowerCase();
        assertFalse(combined.contains("x-api-key"));
        assertFalse(combined.contains("bearer "));
        assertFalse(combined.contains("password"));
    }

    private void assertBlocked(AdaptiveRoutingPolicyInput input, String expectedGuardrail) {
        AdaptiveRoutingPolicyDecision decision = engine.decide(input);

        assertFalse(decision.changed());
        assertFalse(decision.influenceAllowed());
        assertEquals(input.baselineDecision(), decision.finalDecision());
        assertTrue(decision.guardrailReasons().contains(expectedGuardrail), decision.guardrailReasons().toString());
    }

    private AdaptiveRoutingPolicyInput input(
            AdaptiveRoutingPolicyMode mode,
            boolean explicitExperimentContext,
            boolean conflictingSignals,
            boolean signalsFresh,
            String recommendedDecision) {
        return inputWithServers(List.of(server("blue", 100, true), server("green", 100, true)),
                mode, explicitExperimentContext, conflictingSignals, signalsFresh, recommendedDecision, 50);
    }

    private AdaptiveRoutingPolicyInput inputWithServers(
            List<Server> servers,
            AdaptiveRoutingPolicyMode mode,
            boolean explicitExperimentContext,
            boolean conflictingSignals,
            boolean signalsFresh,
            String recommendedDecision,
            double requestedLoad) {
        return new AdaptiveRoutingPolicyInput(
                "test-context",
                mode,
                servers,
                requestedLoad,
                "blue",
                recommendedDecision,
                "NOOP",
                recommendedDecision != null,
                signalsFresh,
                conflictingSignals,
                explicitExperimentContext,
                "summary",
                null);
    }

    private static Server server(String id, double capacity, boolean healthy) {
        Server server = new Server(id, 10, 10, 10, ServerType.ONSITE);
        server.setCapacity(capacity);
        server.setHealthy(healthy);
        return server;
    }
}
