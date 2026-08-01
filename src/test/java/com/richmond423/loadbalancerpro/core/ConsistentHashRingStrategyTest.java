package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

class ConsistentHashRingStrategyTest {
    private static final Instant NOW = Instant.parse("2026-07-31T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void sameKeyAlwaysSelectsTheSameHealthyConfiguredServerWithoutExposingTheKey() {
        ConsistentHashRingStrategy strategy = strategy(List.of("alpha", "beta", "gamma"));
        List<ServerStateVector> candidates = states("alpha", "beta", "gamma");
        String privateKey = "tenant-private-value";

        RoutingDecision first = strategy.chooseForKey(candidates, privateKey);
        for (int attempt = 0; attempt < 100; attempt++) {
            assertEquals(chosen(first), chosen(strategy.chooseForKey(candidates, privateKey)));
        }

        assertEquals(RoutingStrategyId.CONSISTENT_HASH, strategy.id());
        assertEquals(ConsistentHashRingStrategy.STRATEGY_NAME, first.explanation().strategyUsed());
        assertFalse(first.explanation().reason().contains(privateKey));
        assertEquals(NOW, first.explanation().timestamp());
    }

    @Test
    void removingOneOfFourServersOnlyRemapsItsPriorShare() {
        List<String> originalIds = List.of("alpha", "beta", "gamma", "delta");
        String removedId = "gamma";
        ConsistentHashRingStrategy before = strategy(originalIds);
        ConsistentHashRingStrategy after = strategy(List.of("alpha", "beta", "delta"));
        List<ServerStateVector> beforeStates = states(originalIds.toArray(String[]::new));
        List<ServerStateVector> afterStates = states("alpha", "beta", "delta");
        int keys = 20_000;
        int removedSelections = 0;
        int changedSelections = 0;

        for (int index = 0; index < keys; index++) {
            String key = "tenant-" + index;
            String original = chosen(before.chooseForKey(beforeStates, key));
            String replacement = chosen(after.chooseForKey(afterStates, key));
            if (removedId.equals(original)) {
                removedSelections++;
                assertNotEquals(removedId, replacement);
            } else {
                assertEquals(original, replacement,
                        "a surviving server's key must not move when one peer is removed");
            }
            if (!original.equals(replacement)) {
                changedSelections++;
            }
        }

        assertEquals(removedSelections, changedSelections);
        double removedShare = removedSelections / (double) keys;
        assertTrue(removedShare > 0.15 && removedShare < 0.35,
                "the removed server should own approximately one quarter of the key space: " + removedShare);
    }

    @Test
    void unhealthyOrZeroWeightRingMembersAreSkippedWithoutRebuildingTheConfiguredRing() {
        ConsistentHashRingStrategy strategy = strategy(List.of("alpha", "beta", "gamma"));
        List<ServerStateVector> allHealthy = states("alpha", "beta", "gamma");
        String alphaKey = keySelecting(strategy, allHealthy, "alpha");
        List<ServerStateVector> eligible = List.of(
                state("alpha", false, 1.0),
                state("beta", true, 0.0),
                state("gamma", true, 1.0));

        RoutingDecision decision = strategy.chooseForKey(eligible, alphaKey);

        assertEquals("gamma", chosen(decision));
        assertEquals(List.of("gamma"), decision.explanation().candidateServersConsidered());
    }

    private static ConsistentHashRingStrategy strategy(List<String> ids) {
        return new ConsistentHashRingStrategy(CLOCK, 128, ids);
    }

    private static String keySelecting(
            ConsistentHashRingStrategy strategy, List<ServerStateVector> states, String expected) {
        for (int index = 0; index < 100_000; index++) {
            String key = "key-" + index;
            if (expected.equals(chosen(strategy.chooseForKey(states, key)))) {
                return key;
            }
        }
        throw new AssertionError("no bounded key selected " + expected);
    }

    private static String chosen(RoutingDecision decision) {
        return decision.chosenServer().orElseThrow().serverId();
    }

    private static List<ServerStateVector> states(String... ids) {
        List<ServerStateVector> states = new ArrayList<>();
        for (String id : ids) {
            states.add(state(id, true, 1.0));
        }
        return List.copyOf(states);
    }

    private static ServerStateVector state(String id, boolean healthy, double weight) {
        return new ServerStateVector(
                id, healthy, 0, OptionalDouble.of(100.0), OptionalDouble.empty(), weight,
                10.0, 20.0, 30.0, 0.0, OptionalInt.of(0), NOW);
    }
}
