package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoreLoadBalancerConsistentHashingInvariantTest {
    private static final double DELTA = 0.0001;

    private LoadBalancer balancer;

    @BeforeEach
    void setUp() {
        balancer = new LoadBalancer();
    }

    @AfterEach
    void tearDown() {
        balancer.shutdown();
    }

    @Test
    void fixedServerSetRoutesKeysDeterministicallyAndPreservesRequestedLoad() {
        addServers(
                server("ALPHA"),
                server("BETA"),
                server("GAMMA"));

        Map<String, Double> first = balancer.consistentHashing(120.0, 12);
        Map<String, Double> second = balancer.consistentHashing(120.0, 12);

        assertAll("consistent hashing fixed-server routing contract",
                () -> assertFalse(first.isEmpty()),
                () -> assertEquals(first, second),
                () -> assertTrue(Set.of("ALPHA", "BETA", "GAMMA").containsAll(first.keySet())),
                () -> assertEquals(120.0, allocatedTotal(first), DELTA),
                () -> assertNoNegativeAllocations(first));
    }

    @Test
    void invalidKeyCountsAreRejectedBeforeRouting() {
        addServers(server("ALPHA"), server("BETA"));

        assertAll("invalid consistent-hashing key-count contract",
                () -> assertThrows(IllegalArgumentException.class,
                        () -> balancer.consistentHashing(10.0, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> balancer.consistentHashing(10.0, -1)));
    }

    @Test
    void emptyAndAllUnhealthyServerSetsFailClosed() {
        Map<String, Double> empty = balancer.consistentHashing(90.0, 9);

        Server first = server("FIRST");
        Server second = server("SECOND");
        first.setHealthy(false);
        second.setHealthy(false);
        addServers(first, second);

        Map<String, Double> allUnhealthy = balancer.consistentHashing(90.0, 9);

        assertAll("consistent hashing fail-closed contract",
                () -> assertTrue(empty.isEmpty()),
                () -> assertTrue(allUnhealthy.isEmpty()));
    }

    @Test
    void removedServerDoesNotRemainInConsistentHashingAllocations() {
        Server removed = server("REMOVE");
        Server keep = server("KEEP");
        addServers(removed, keep);

        balancer.removeServer("REMOVE");

        Map<String, Double> allocation = balancer.consistentHashing(100.0, 10);

        assertAll("consistent hashing removal contract",
                () -> assertSame(keep, balancer.getServer("KEEP")),
                () -> assertTrue(allocation.containsKey("KEEP")),
                () -> assertFalse(allocation.containsKey("REMOVE")),
                () -> assertEquals(100.0, allocatedTotal(allocation), DELTA),
                () -> assertNoNegativeAllocations(allocation));
    }

    @Test
    void duplicateServerReplacementDoesNotLeaveRemovedHashParticipation() {
        Server original = server("SAME");
        Server peer = server("PEER");
        Server replacement = server("SAME");
        original.setHealthy(false);
        addServers(original, peer);

        balancer.addServer(replacement);
        Server registeredReplacement = balancer.getServer("SAME");
        Map<String, Double> afterReplacement = balancer.consistentHashing(100.0, 10);

        balancer.removeServer("SAME");
        Map<String, Double> afterRemoval = balancer.consistentHashing(100.0, 10);

        assertAll("consistent hashing duplicate replacement contract",
                () -> assertSame(replacement, registeredReplacement),
                () -> assertTrue(afterReplacement.containsKey("SAME")),
                () -> assertTrue(afterReplacement.containsKey("PEER")),
                () -> assertEquals(100.0, allocatedTotal(afterReplacement), DELTA),
                () -> assertFalse(afterRemoval.containsKey("SAME")),
                () -> assertEquals(Set.of("PEER"), afterRemoval.keySet()),
                () -> assertEquals(100.0, allocatedTotal(afterRemoval), DELTA));
    }

    @Test
    void zeroLoadHashingKeepsSelectedBucketsZeroValued() {
        addServers(server("ALPHA"), server("BETA"));

        Map<String, Double> allocation = balancer.consistentHashing(0.0, 6);

        assertAll("consistent hashing zero-load characterization",
                () -> assertFalse(allocation.isEmpty()),
                () -> assertTrue(Set.of("ALPHA", "BETA").containsAll(allocation.keySet())),
                () -> assertEquals(0.0, allocatedTotal(allocation), DELTA),
                () -> assertTrue(allocation.values().stream().allMatch(value -> value == 0.0)));
    }

    private void addServers(Server... servers) {
        for (Server server : servers) {
            balancer.addServer(server);
        }
    }

    private static Server server(String id) {
        Server server = new Server(id, 10.0, 10.0, 10.0);
        server.setWeight(1.0);
        server.setCapacity(100.0);
        return server;
    }

    private static double allocatedTotal(Map<String, Double> allocation) {
        return allocation.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private static void assertNoNegativeAllocations(Map<String, Double> allocation) {
        assertTrue(allocation.values().stream().allMatch(value -> value >= 0.0),
                "consistent hashing allocations must never go negative");
    }
}
