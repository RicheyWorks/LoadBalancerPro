package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoreLoadBalancerCrossStrategyEdgeInvariantTest {
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
    void emptyServerSetFailsClosedAcrossFacadeStrategies() {
        LoadDistributionResult capacityAware = balancer.capacityAwareWithResult(120.0);
        LoadDistributionResult predictive = balancer.predictiveLoadBalancingWithResult(120.0);

        assertAll("empty server set facade contract",
                () -> assertTrue(balancer.roundRobin(120.0).isEmpty()),
                () -> assertTrue(balancer.leastLoaded(120.0).isEmpty()),
                () -> assertTrue(balancer.weightedDistribution(120.0).isEmpty()),
                () -> assertTrue(balancer.consistentHashing(120.0, 12).isEmpty()),
                () -> assertTrue(capacityAware.allocations().isEmpty()),
                () -> assertEquals(120.0, capacityAware.unallocatedLoad(), DELTA),
                () -> assertTrue(predictive.allocations().isEmpty()),
                () -> assertEquals(120.0, predictive.unallocatedLoad(), DELTA));
    }

    @Test
    void allUnhealthyServerSetFailsClosedAcrossFacadeStrategies() {
        addServers(unhealthy("S1"), unhealthy("S2"));

        LoadDistributionResult capacityAware = balancer.capacityAwareWithResult(90.0);
        LoadDistributionResult predictive = balancer.predictiveLoadBalancingWithResult(90.0);

        assertAll("all unhealthy server set facade contract",
                () -> assertTrue(balancer.roundRobin(90.0).isEmpty()),
                () -> assertTrue(balancer.leastLoaded(90.0).isEmpty()),
                () -> assertTrue(balancer.weightedDistribution(90.0).isEmpty()),
                () -> assertTrue(balancer.consistentHashing(90.0, 9).isEmpty()),
                () -> assertTrue(capacityAware.allocations().isEmpty()),
                () -> assertEquals(90.0, capacityAware.unallocatedLoad(), DELTA),
                () -> assertTrue(predictive.allocations().isEmpty()),
                () -> assertEquals(90.0, predictive.unallocatedLoad(), DELTA));
    }

    @Test
    void negativeLoadIsRejectedBeforeAllocationAcrossFacadeStrategies() {
        addServers(server("S1", 10.0), server("S2", 20.0));

        assertAll("negative load rejection",
                () -> assertThrows(IllegalArgumentException.class, () -> balancer.roundRobin(-0.1)),
                () -> assertThrows(IllegalArgumentException.class, () -> balancer.leastLoaded(-0.1)),
                () -> assertThrows(IllegalArgumentException.class, () -> balancer.weightedDistribution(-0.1)),
                () -> assertThrows(IllegalArgumentException.class, () -> balancer.consistentHashing(-0.1, 2)),
                () -> assertThrows(IllegalArgumentException.class, () -> balancer.capacityAwareWithResult(-0.1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> balancer.predictiveLoadBalancingWithResult(-0.1)));
    }

    @Test
    void consistentHashingRejectsInvalidKeyCounts() {
        addServers(server("S1", 10.0), server("S2", 20.0));

        assertAll("invalid key count rejection",
                () -> assertThrows(IllegalArgumentException.class, () -> balancer.consistentHashing(10.0, 0)),
                () -> assertThrows(IllegalArgumentException.class, () -> balancer.consistentHashing(10.0, -1)));
    }

    @Test
    void zeroLoadCharacterizesCurrentFacadeAllocationShapeWithoutNegativeRemainder() {
        addServers(server("LOW", 10.0), server("HIGH", 60.0));

        Map<String, Double> roundRobin = balancer.roundRobin(0.0);
        Map<String, Double> leastLoaded = balancer.leastLoaded(0.0);
        Map<String, Double> weighted = balancer.weightedDistribution(0.0);
        Map<String, Double> consistentHashing = balancer.consistentHashing(0.0, 6);
        LoadDistributionResult capacityAware = balancer.capacityAwareWithResult(0.0);
        LoadDistributionResult predictive = balancer.predictiveLoadBalancingWithResult(0.0);

        assertAll("zero-load facade characterization",
                () -> assertEquals(Map.of("LOW", 0.0, "HIGH", 0.0), roundRobin),
                () -> assertEquals(Map.of("LOW", 0.0), leastLoaded),
                () -> assertEquals(Map.of("LOW", 0.0, "HIGH", 0.0), weighted),
                () -> assertFalse(consistentHashing.isEmpty()),
                () -> assertOnlyZeroValues(consistentHashing),
                () -> assertEquals(Map.of("LOW", 0.0), capacityAware.allocations()),
                () -> assertEquals(0.0, capacityAware.unallocatedLoad(), DELTA),
                () -> assertEquals(Map.of("LOW", 0.0), predictive.allocations()),
                () -> assertEquals(0.0, predictive.unallocatedLoad(), DELTA));
    }

    private void addServers(Server... servers) {
        for (Server server : servers) {
            balancer.addServer(server);
        }
    }

    private static Server server(String id, double loadScore) {
        Server server = new Server(id, loadScore, loadScore, loadScore);
        server.setWeight(1.0);
        server.setCapacity(100.0);
        return server;
    }

    private static Server unhealthy(String id) {
        Server server = server(id, 10.0);
        server.setHealthy(false);
        return server;
    }

    private static void assertOnlyZeroValues(Map<String, Double> allocations) {
        assertFalse(allocations.isEmpty(), "zero-load hashing should still record selected zero-valued buckets");
        for (double value : allocations.values()) {
            assertEquals(0.0, value, DELTA);
        }
    }
}
