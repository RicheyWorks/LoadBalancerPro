package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class StatefulRoutingStrategyTest {
    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");

    @Test
    void listCompatibilityPathAppliesSnapshotBeforeNoListSelection() {
        RecordingStatefulStrategy strategy = new RecordingStatefulStrategy();

        RoutingDecision decision = strategy.choose(List.of(state("alpha"), state("beta")));

        assertEquals(List.of("alpha", "beta"), strategy.currentServerIds());
        assertEquals("alpha", decision.chosenServer().orElseThrow().serverId());
        assertEquals(1, strategy.chooseCount());
    }

    @Test
    void keyedAndComparisonCompatibilityPathsForwardTheirSelectionContext() {
        RecordingStatefulStrategy strategy = new RecordingStatefulStrategy();
        List<ServerStateVector> current = List.of(state("alpha"));

        strategy.chooseForKey(current, "tenant-17");
        strategy.chooseForComparison(current, 42L);

        assertEquals("tenant-17", strategy.lastKey());
        assertEquals(42L, strategy.lastSeed());
        assertEquals(2, strategy.snapshotCount());
    }

    @Test
    void defaultSnapshotValidationRejectsNullContainersAndMembers() {
        RecordingStatefulStrategy strategy = new RecordingStatefulStrategy();

        assertThrows(NullPointerException.class, () -> strategy.onServerStates(null));
        assertThrows(NullPointerException.class, () -> strategy.onServerStates(
                new ArrayList<>(java.util.Arrays.asList(state("alpha"), null))));
        assertEquals(List.of(), strategy.currentServerIds(),
                "snapshot validation must finish before applying incremental updates");
    }

    @Test
    void comparisonEngineUsesTheStatefulNoListPath() {
        RecordingStatefulStrategy strategy = new RecordingStatefulStrategy(
                RoutingStrategyId.ROUND_ROBIN, true);
        RoutingComparisonEngine engine = new RoutingComparisonEngine(
                new RoutingStrategyRegistry(List.of(strategy)),
                Clock.fixed(NOW, ZoneOffset.UTC));

        RoutingComparisonResult result = engine.compare(
                List.of(state("alpha"), state("beta")),
                List.of(RoutingStrategyId.ROUND_ROBIN)).results().get(0);

        assertEquals(RoutingComparisonResult.Status.SUCCESS, result.status());
        assertEquals(List.of("alpha", "beta"), strategy.currentServerIds());
        assertEquals(1, strategy.snapshotCount());
        assertEquals(1, strategy.chooseCount());
    }

    private static ServerStateVector state(String serverId) {
        return new ServerStateVector(
                serverId, true, 0, 10.0, 10.0, 5.0, 10.0, 15.0, 0.0, 0, NOW);
    }

    private static final class RecordingStatefulStrategy implements StatefulRoutingStrategy {
        private final Map<String, ServerStateVector> currentServers = new LinkedHashMap<>();
        private final RoutingStrategyIdentifier id;
        private final boolean rejectListCompatibilityPath;
        private final boolean requireHostLock;
        private int snapshotCount;
        private int chooseCount;
        private String lastKey;
        private long lastSeed;

        private RecordingStatefulStrategy() {
            this(RoutingStrategyIdentifier.of("recording-stateful"), false);
        }

        private RecordingStatefulStrategy(
                RoutingStrategyIdentifier id,
                boolean rejectListCompatibilityPath) {
            this.id = id;
            this.rejectListCompatibilityPath = rejectListCompatibilityPath;
            this.requireHostLock = rejectListCompatibilityPath;
        }

        @Override
        public RoutingStrategyIdentifier id() {
            return id;
        }

        @Override
        public void onServerState(ServerStateVector updated) {
            currentServers.put(updated.serverId(), updated);
        }

        @Override
        public void onServerStates(List<ServerStateVector> currentServers) {
            assertHostLockIfRequired();
            snapshotCount++;
            this.currentServers.clear();
            StatefulRoutingStrategy.super.onServerStates(currentServers);
        }

        @Override
        public RoutingDecision choose() {
            chooseCount++;
            List<ServerStateVector> candidates = List.copyOf(currentServers.values());
            Optional<ServerStateVector> chosen = candidates.stream().findFirst();
            return new RoutingDecision(
                    chosen,
                    new RoutingDecisionExplanation(
                            id().externalName(),
                            candidates.stream().map(ServerStateVector::serverId).toList(),
                            chosen.map(ServerStateVector::serverId),
                            Map.of(),
                            "Selected from retained state.",
                            NOW));
        }

        @Override
        public RoutingDecision choose(List<ServerStateVector> servers) {
            if (rejectListCompatibilityPath) {
                throw new AssertionError("comparison engine used the list compatibility path");
            }
            return StatefulRoutingStrategy.super.choose(servers);
        }

        @Override
        public RoutingDecision chooseForKey(String key) {
            lastKey = key;
            return choose();
        }

        @Override
        public RoutingDecision chooseForComparison(long deterministicSeed) {
            assertHostLockIfRequired();
            lastSeed = deterministicSeed;
            return choose();
        }

        private void assertHostLockIfRequired() {
            if (requireHostLock && !Thread.holdsLock(this)) {
                throw new AssertionError("host did not serialize the stateful selection cycle");
            }
        }

        List<String> currentServerIds() {
            return List.copyOf(currentServers.keySet());
        }

        int snapshotCount() {
            return snapshotCount;
        }

        int chooseCount() {
            return chooseCount;
        }

        String lastKey() {
            return lastKey;
        }

        long lastSeed() {
            return lastSeed;
        }
    }
}
