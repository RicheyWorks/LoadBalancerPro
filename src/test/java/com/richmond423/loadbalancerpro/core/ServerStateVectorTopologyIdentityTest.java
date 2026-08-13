package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

class ServerStateVectorTopologyIdentityTest {
    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");

    @Test
    void topologyNodeIdentityIsOpaqueAndIndependentFromServerIdentity() {
        ServerStateVector state = state("server-process-17", Optional.of("rack-a/node-04"));

        assertEquals("server-process-17", state.serverId());
        assertEquals(Optional.of("rack-a/node-04"), state.topologyNodeId());

        RoutingDecision decision = new RoundRobinRoutingStrategy().choose(List.of(state));
        assertSame(state, decision.chosenServer().orElseThrow());
    }

    @Test
    void existingFullAndConvenienceConstructorsDefaultTopologyIdentityToEmpty() {
        ServerStateVector full = new ServerStateVector(
                "full",
                true,
                0,
                OptionalDouble.of(10.0),
                OptionalDouble.empty(),
                1.0,
                5.0,
                10.0,
                15.0,
                0.0,
                OptionalInt.empty(),
                NetworkAwarenessSignal.neutral("full", NOW),
                LatencyWindowSignal.empty(),
                NOW);
        ServerStateVector convenience = new ServerStateVector(
                "convenience", true, 0, 10.0, 10.0, 5.0, 10.0, 15.0, 0.0, 0, NOW);

        assertTrue(full.topologyNodeId().isEmpty());
        assertTrue(convenience.topologyNodeId().isEmpty());
        assertTrue(ServerStateVector.fromServer(new Server("from-server", 0.0, 0.0, 0.0),
                0, 5.0, 10.0, 15.0, 0.0, 0, NOW).topologyNodeId().isEmpty());
    }

    @Test
    void topologyIdentityContainerAndPresentValueAreValidated() {
        assertThrows(NullPointerException.class, () -> state("server", null));
        assertThrows(IllegalArgumentException.class, () -> state("server", Optional.of(" ")));
    }

    private static ServerStateVector state(String serverId, Optional<String> topologyNodeId) {
        return new ServerStateVector(
                serverId,
                topologyNodeId,
                true,
                0,
                OptionalDouble.of(10.0),
                OptionalDouble.empty(),
                1.0,
                5.0,
                10.0,
                15.0,
                0.0,
                OptionalInt.empty(),
                NetworkAwarenessSignal.neutral(serverId, NOW),
                LatencyWindowSignal.empty(),
                NOW);
    }
}
