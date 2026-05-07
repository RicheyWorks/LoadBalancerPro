package com.richmond423.loadbalancerpro.core;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class WeightedLeastConnectionsRoutingStrategyTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final WeightedLeastConnectionsRoutingStrategy strategy =
            new WeightedLeastConnectionsRoutingStrategy(FIXED_CLOCK);

    @Test
    void choosesLowestInFlightConnectionScore() {
        RoutingDecision decision = strategy.choose(List.of(
                state("busy", 12, 1.0),
                state("quiet", 4, 1.0)));

        assertEquals("quiet", decision.chosenServer().orElseThrow().serverId());
        assertTrue(score(decision, "quiet") < score(decision, "busy"));
    }

    @Test
    void weightAdjustsConnectionScore() {
        RoutingDecision decision = strategy.choose(List.of(
                state("base", 5, 1.0),
                state("weighted", 12, 4.0)));

        assertEquals("weighted", decision.chosenServer().orElseThrow().serverId());
        assertEquals(5.0, score(decision, "base"), 0.0);
        assertEquals(3.0, score(decision, "weighted"), 0.0);
    }

    @Test
    void skipsUnhealthyCandidates() {
        RoutingDecision decision = strategy.choose(List.of(
                state("unhealthy-quiet", false, 0, 10.0),
                state("healthy-busy", true, 20, 1.0)));

        assertEquals("healthy-busy", decision.chosenServer().orElseThrow().serverId());
        assertEquals(List.of("healthy-busy"), decision.explanation().candidateServersConsidered());
        assertFalse(decision.explanation().scores().containsKey("unhealthy-quiet"));
    }

    @Test
    void emptyCandidateListReturnsSafeNoDecision() {
        RoutingDecision decision = strategy.choose(List.of());

        assertTrue(decision.chosenServer().isEmpty());
        assertEquals(WeightedLeastConnectionsRoutingStrategy.STRATEGY_NAME,
                decision.explanation().strategyUsed());
        assertTrue(decision.explanation().candidateServersConsidered().isEmpty());
        assertTrue(decision.explanation().chosenServerId().isEmpty());
        assertTrue(decision.explanation().scores().isEmpty());
        assertTrue(decision.explanation().reason().contains("No healthy eligible servers"));
        assertEquals(NOW, decision.explanation().timestamp());
    }

    @Test
    void allUnhealthyCandidatesReturnSafeNoDecision() {
        RoutingDecision decision = strategy.choose(List.of(
                state("a", false, 0, 1.0),
                state("b", false, 0, 1.0)));

        assertTrue(decision.chosenServer().isEmpty());
        assertTrue(decision.explanation().candidateServersConsidered().isEmpty());
        assertTrue(decision.explanation().scores().isEmpty());
        assertTrue(decision.explanation().chosenServerId().isEmpty());
    }

    @Test
    void missingAndZeroWeightDefaultSafely() {
        ServerStateVector missingWeight = stateWithoutWeight("missing", 8);
        ServerStateVector zeroWeight = state("zero", 8, 0.0);

        RoutingDecision decision = strategy.choose(List.of(missingWeight, zeroWeight));

        assertEquals(1.0, missingWeight.weight(), 0.0);
        assertEquals(8.0, score(decision, "missing"), 0.0);
        assertEquals(8.0, score(decision, "zero"), 0.0);
    }

    @Test
    void verySmallPositiveWeightClampsSafely() {
        RoutingDecision decision = strategy.choose(List.of(
                state("min", 1, 0.1),
                state("tiny", 1, 0.01)));

        assertEquals(10.0, score(decision, "min"), 0.0);
        assertEquals(10.0, score(decision, "tiny"), 0.0);
    }

    @Test
    void deterministicTieBreakingUsesServerId() {
        RoutingDecision decision = strategy.choose(List.of(
                state("zeta", 5, 1.0),
                state("alpha", 5, 1.0)));

        assertEquals("alpha", decision.chosenServer().orElseThrow().serverId());
        assertEquals(List.of("alpha", "zeta"), decision.explanation().candidateServersConsidered());
    }

    @Test
    void doesNotMutateInputState() {
        List<ServerStateVector> candidates = new ArrayList<>(List.of(
                state("b", 10, 1.0),
                state("a", 5, 1.0)));
        List<ServerStateVector> before = List.copyOf(candidates);

        strategy.choose(candidates);

        assertEquals(before, candidates);
    }

    @Test
    void invalidWeightsAreRejectedByStateValidation() {
        assertAll("invalid weight",
                () -> assertThrows(IllegalArgumentException.class,
                        () -> state("negative", 1, -0.1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> state("nan", 1, Double.NaN)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> state("infinite", 1, Double.POSITIVE_INFINITY)));
    }

    @Test
    void producesCompleteDecisionMetadata() {
        RoutingDecision decision = strategy.choose(List.of(
                state("b", 10, 1.0),
                state("a", 5, 1.0),
                state("c", 20, 5.0)));

        assertEquals(WeightedLeastConnectionsRoutingStrategy.STRATEGY_NAME,
                decision.explanation().strategyUsed());
        assertEquals(List.of("a", "b", "c"), decision.explanation().candidateServersConsidered());
        assertEquals("c", decision.explanation().chosenServerId().orElseThrow());
        assertEquals(3, decision.explanation().scores().size());
        assertEquals(4.0, score(decision, "c"), 0.0);
        assertTrue(decision.explanation().reason().contains("weighted least-connections score"));
        assertEquals(NOW, decision.explanation().timestamp());
    }

    private double score(RoutingDecision decision, String serverId) {
        return decision.explanation().scores().get(serverId);
    }

    private ServerStateVector state(String id, int inFlight, double weight) {
        return state(id, true, inFlight, weight);
    }

    private ServerStateVector state(String id, boolean healthy, int inFlight, double weight) {
        return new ServerStateVector(
                id,
                healthy,
                inFlight,
                OptionalDouble.of(100.0),
                OptionalDouble.of(100.0),
                weight,
                10.0,
                20.0,
                40.0,
                0.0,
                OptionalInt.of(0),
                NetworkAwarenessSignal.neutral(id, NOW),
                NOW);
    }

    private ServerStateVector stateWithoutWeight(String id, int inFlight) {
        return new ServerStateVector(
                id,
                true,
                inFlight,
                OptionalDouble.of(100.0),
                OptionalDouble.of(100.0),
                10.0,
                20.0,
                40.0,
                0.0,
                OptionalInt.of(0),
                NetworkAwarenessSignal.neutral(id, NOW),
                NOW);
    }
}
