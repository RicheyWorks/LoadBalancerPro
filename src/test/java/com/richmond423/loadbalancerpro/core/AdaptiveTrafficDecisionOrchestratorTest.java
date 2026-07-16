package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

class AdaptiveTrafficDecisionOrchestratorTest {
    private static final Instant NOW = Instant.parse("2026-07-16T18:00:00Z");
    private static final Map<String, Double> BASELINE = Map.of("a", 0.50, "b", 0.50);
    private final AdaptiveTrafficDecisionOrchestrator orchestrator = new AdaptiveTrafficDecisionOrchestrator();

    @Test
    void composesEveryAdaptiveStageIntoImmutableActiveExperimentRecord() {
        AdaptiveTrafficDecisionRecord record = orchestrator.decide(
                request(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, goodCandidates(), false, false, true, false),
                AdaptiveTrafficDecisionPolicy.localLabDefaults());

        assertEquals(AdaptiveTrafficDecisionOrchestrator.DECISION_SCHEMA_VERSION, record.schemaVersion());
        assertEquals(List.of("a", "b"), record.rollingStates().keySet().stream().toList());
        assertEquals(5, record.observations().get("a").size());
        assertEquals(goodCandidates(), record.request().candidates());
        assertTrue(record.request().explicitExperimentContext());
        assertEquals(32, record.policy().maximumCandidateCount());
        assertTrue(record.rollingStates().values().stream().allMatch(ServerRollingSignalState::sufficientEvidence));
        assertTrue(record.stateVectors().values().stream().allMatch(ServerStateVector::healthy));
        assertTrue(record.scoreBreakdowns().get("a").totalScore()
                < record.scoreBreakdowns().get("b").totalScore());
        assertTrue(record.allocationRecommendation().allocations().get("a")
                > record.allocationRecommendation().allocations().get("b"));
        assertEquals(1.0, total(record.allocationRecommendation().allocations()), 0.000000001);
        assertEquals(TrafficAllocationGuardrailAction.ALLOW, record.guardrailDecision().action());
        assertTrue(record.guardrailDecision().influenceAllowed());
        assertTrue(record.guardrailDecision().changed());
        assertNotEquals(BASELINE, record.effectiveAllocations());
        assertFalse(record.trafficActionPerformed());
        assertTrue(record.contentFingerprint().matches("[0-9a-f]{64}"));
        assertTrue(record.reasons().contains("decision record only; no traffic action performed"));
    }

    @Test
    void recommendModeExposesApprovedCandidateButKeepsBaselineEffective() {
        AdaptiveTrafficDecisionRecord record = orchestrator.decide(
                request(AdaptiveRoutingPolicyMode.RECOMMEND, goodCandidates(), false, false, false, false),
                AdaptiveTrafficDecisionPolicy.localLabDefaults());

        assertEquals(TrafficAllocationGuardrailAction.ALLOW, record.guardrailDecision().action());
        assertFalse(record.guardrailDecision().influenceAllowed());
        assertFalse(record.guardrailDecision().changed());
        assertEquals(BASELINE, record.effectiveAllocations());
        assertNotEquals(BASELINE, record.guardrailDecision().approvedAllocations());
    }

    @Test
    void missingOrStaleEvidenceFailsClosedToNoAllocation() {
        List<AdaptiveTrafficDecisionCandidate> stale = List.of(
                candidate("a", observations("a", 10.0, 60)),
                candidate("b", observations("b", 20.0, 60)));
        AdaptiveTrafficDecisionRecord record = orchestrator.decide(
                request(AdaptiveRoutingPolicyMode.RECOMMEND, stale, false, false, false, false),
                AdaptiveTrafficDecisionPolicy.localLabDefaults());

        assertTrue(record.rollingStates().values().stream().allMatch(ServerRollingSignalState::stale));
        assertTrue(record.allocationRecommendation().fallbackApplied());
        assertTrue(record.allocationRecommendation().allocations().isEmpty());
        assertEquals(TrafficAllocationGuardrailAction.DENY, record.guardrailDecision().action());
        assertEquals(BASELINE, record.effectiveAllocations());
    }

    @Test
    void observeConflictCooldownAndOperatorStopAllRetainBaseline() {
        AdaptiveTrafficDecisionRecord observe = orchestrator.decide(
                request(AdaptiveRoutingPolicyMode.OBSERVE, goodCandidates(), false, false, false, false),
                AdaptiveTrafficDecisionPolicy.localLabDefaults());
        assertEquals(TrafficAllocationGuardrailAction.DENY, observe.guardrailDecision().action());

        AdaptiveTrafficDecisionRecord conflict = orchestrator.decide(
                request(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, goodCandidates(), true, false, true, false),
                AdaptiveTrafficDecisionPolicy.localLabDefaults());
        assertTrue(conflict.guardrailDecision().reasons().contains("conflicting allocation signals"));

        AdaptiveTrafficDecisionRecord cooldown = orchestrator.decide(
                request(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, goodCandidates(), false, true, true, false),
                AdaptiveTrafficDecisionPolicy.localLabDefaults());
        assertTrue(cooldown.guardrailDecision().reasons().contains("allocation cooldown is active"));

        AdaptiveTrafficDecisionRecord stopped = orchestrator.decide(
                request(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, goodCandidates(), false, false, true, true),
                AdaptiveTrafficDecisionPolicy.localLabDefaults());
        assertTrue(stopped.guardrailDecision().reasons().contains("operator stop requested"));

        for (AdaptiveTrafficDecisionRecord record : List.of(observe, conflict, cooldown, stopped)) {
            assertEquals(BASELINE, record.effectiveAllocations());
            assertFalse(record.trafficActionPerformed());
        }
    }

    @Test
    void integratedGuardrailClampsTotalShareMovement() {
        AdaptiveTrafficDecisionPolicy policy = new AdaptiveTrafficDecisionPolicy(
                4,
                ServerObservationWindowPolicy.localLabDefaults(),
                ServerRecommendationScorePolicy.localLabDefaults(),
                new TrafficAllocationPolicy(0.0, 1.0, 1.0),
                new TrafficAllocationGuardrailPolicy(1.0, 0.05));
        AdaptiveTrafficDecisionRecord record = orchestrator.decide(
                request(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, goodCandidates(), false, false, true, false),
                policy);

        assertEquals(TrafficAllocationGuardrailAction.CLAMP, record.guardrailDecision().action());
        assertEquals(0.05, TrafficAllocationMaps.totalVariation(BASELINE, record.effectiveAllocations()), 0.000000001);
        assertEquals(1.0, total(record.effectiveAllocations()), 0.000000001);
    }

    @Test
    void canonicalOrderingProducesDeterministicRecordAndFingerprint() {
        List<AdaptiveTrafficDecisionCandidate> firstOrder = goodCandidates();
        List<AdaptiveTrafficDecisionCandidate> secondOrder = new ArrayList<>(firstOrder);
        Collections.reverse(secondOrder);

        AdaptiveTrafficDecisionRecord first = orchestrator.decide(
                request(AdaptiveRoutingPolicyMode.RECOMMEND, firstOrder, false, false, false, false),
                AdaptiveTrafficDecisionPolicy.localLabDefaults());
        AdaptiveTrafficDecisionRecord second = orchestrator.decide(
                request(AdaptiveRoutingPolicyMode.RECOMMEND, secondOrder, false, false, false, false),
                AdaptiveTrafficDecisionPolicy.localLabDefaults());

        assertEquals(first, second);
        assertEquals(first.contentFingerprint(), second.contentFingerprint());
        assertEquals(List.of("a", "b"), first.observations().keySet().stream().toList());
    }

    @Test
    void enforcesCandidateAndObservationBoundsBeforeComposition() {
        AdaptiveTrafficDecisionPolicy oneCandidatePolicy = new AdaptiveTrafficDecisionPolicy(
                1,
                ServerObservationWindowPolicy.localLabDefaults(),
                ServerRecommendationScorePolicy.localLabDefaults(),
                TrafficAllocationPolicy.localLabDefaults(),
                TrafficAllocationGuardrailPolicy.localLabDefaults());
        assertThrows(IllegalArgumentException.class, () -> orchestrator.decide(
                request(AdaptiveRoutingPolicyMode.RECOMMEND, goodCandidates(), false, false, false, false),
                oneCandidatePolicy));

        List<ServerObservation> excessive = new ArrayList<>();
        for (int index = 0; index < 65; index++) {
            excessive.add(ServerObservation.success(
                    "a-excess-" + index,
                    "a",
                    ServerObservationSource.ENTERPRISE_LAB,
                    10.0,
                    NOW.minusSeconds(index + 1L)));
        }
        AdaptiveTrafficDecisionRequest excessiveRequest = new AdaptiveTrafficDecisionRequest(
                "decision-excess",
                "context-excess",
                AdaptiveRoutingPolicyMode.RECOMMEND,
                List.of(candidate("a", excessive)),
                Map.of("a", 1.0),
                NOW,
                false,
                false,
                false,
                false);
        assertThrows(IllegalArgumentException.class, () -> orchestrator.decide(
                excessiveRequest, AdaptiveTrafficDecisionPolicy.localLabDefaults()));
    }

    @Test
    void rejectsMalformedIdentityBaselineAndFutureEvidence() {
        AdaptiveTrafficDecisionCandidate candidate = goodCandidates().get(0);
        assertThrows(IllegalArgumentException.class, () -> new AdaptiveTrafficDecisionRequest(
                "duplicate",
                "context",
                AdaptiveRoutingPolicyMode.RECOMMEND,
                List.of(candidate, candidate),
                BASELINE,
                NOW,
                false,
                false,
                false,
                false));
        assertThrows(IllegalArgumentException.class, () -> new AdaptiveTrafficDecisionRequest(
                "bad-baseline",
                "context",
                AdaptiveRoutingPolicyMode.RECOMMEND,
                goodCandidates(),
                Map.of("a", 0.40, "b", 0.40),
                NOW,
                false,
                false,
                false,
                false));

        AdaptiveTrafficDecisionCandidate future = candidate("a", List.of(ServerObservation.success(
                "future",
                "a",
                ServerObservationSource.ENTERPRISE_LAB,
                10.0,
                NOW.plusSeconds(1))));
        AdaptiveTrafficDecisionRequest futureRequest = new AdaptiveTrafficDecisionRequest(
                "future-decision",
                "future-context",
                AdaptiveRoutingPolicyMode.RECOMMEND,
                List.of(future),
                Map.of("a", 1.0),
                NOW,
                false,
                false,
                false,
                false);
        assertThrows(IllegalArgumentException.class, () -> orchestrator.decide(
                futureRequest, AdaptiveTrafficDecisionPolicy.localLabDefaults()));
    }

    @Test
    void decisionCollectionsAreDeeplyImmutable() {
        AdaptiveTrafficDecisionRecord record = orchestrator.decide(
                request(AdaptiveRoutingPolicyMode.RECOMMEND, goodCandidates(), false, false, false, false),
                AdaptiveTrafficDecisionPolicy.localLabDefaults());

        assertThrows(UnsupportedOperationException.class, () -> record.observations().clear());
        assertThrows(UnsupportedOperationException.class, () -> record.observations().get("a").clear());
        assertThrows(UnsupportedOperationException.class, () -> record.request().candidates().clear());
        assertThrows(UnsupportedOperationException.class, () -> record.request().baselineAllocations().clear());
        assertThrows(UnsupportedOperationException.class, () -> record.rollingStates().clear());
        assertThrows(UnsupportedOperationException.class, () -> record.reasons().clear());
    }

    private static AdaptiveTrafficDecisionRequest request(
            AdaptiveRoutingPolicyMode mode,
            List<AdaptiveTrafficDecisionCandidate> candidates,
            boolean conflictingSignals,
            boolean cooldownActive,
            boolean explicitExperimentContext,
            boolean operatorStopRequested) {
        return new AdaptiveTrafficDecisionRequest(
                "decision-0001",
                "context-0001",
                mode,
                candidates,
                BASELINE,
                NOW,
                conflictingSignals,
                cooldownActive,
                explicitExperimentContext,
                operatorStopRequested);
    }

    private static List<AdaptiveTrafficDecisionCandidate> goodCandidates() {
        return List.of(
                candidate("a", observations("a", 10.0, 5)),
                candidate("b", observations("b", 500.0, 5)));
    }

    private static AdaptiveTrafficDecisionCandidate candidate(
            String serverId,
            List<ServerObservation> observations) {
        return new AdaptiveTrafficDecisionCandidate(
                serverId,
                true,
                100.0,
                1.0,
                0,
                OptionalDouble.empty(),
                OptionalInt.of(0),
                observations);
    }

    private static List<ServerObservation> observations(
            String serverId,
            double latencyMillis,
            int oldestAgeSeconds) {
        List<ServerObservation> observations = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            observations.add(ServerObservation.success(
                    serverId + "-observation-" + index + "-" + oldestAgeSeconds,
                    serverId,
                    ServerObservationSource.ENTERPRISE_LAB,
                    latencyMillis,
                    NOW.minusSeconds(oldestAgeSeconds - index)));
        }
        return observations;
    }

    private static double total(Map<String, Double> allocations) {
        return allocations.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}
