package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoreLoadBalancerRebalanceInvariantTest {
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
    void rebalanceWithoutAccumulatedLoadReturnsEmptyAndAllowsFutureAllocation() {
        addServers(server("ALPHA", 10.0, 100.0), server("BETA", 20.0, 100.0));

        Map<String, Double> emptyRebalance = balancer.rebalanceExistingLoad();
        Map<String, Double> futureAllocation = balancer.roundRobin(40.0);

        assertAll("empty rebalance contract",
                () -> assertTrue(emptyRebalance.isEmpty()),
                () -> assertEquals(Map.of("ALPHA", 20.0, "BETA", 20.0), futureAllocation),
                () -> assertEquals(40.0, allocated(futureAllocation), DELTA),
                () -> assertNoNegativeAllocations(futureAllocation));
    }

    @Test
    void roundRobinAccumulatedLoadRebalancesAndRemainsRepeatable() {
        addServers(
                server("ALPHA", 10.0, 100.0),
                server("BETA", 20.0, 100.0),
                server("GAMMA", 30.0, 100.0));

        Map<String, Double> original = balancer.roundRobin(90.0);
        balancer.setStrategy(LoadBalancer.Strategy.ROUND_ROBIN);

        Map<String, Double> firstRebalance = balancer.rebalanceExistingLoad();
        Map<String, Double> secondRebalance = balancer.rebalanceExistingLoad();

        assertAll("round-robin accumulated load rebalance contract",
                () -> assertEquals(Map.of("ALPHA", 30.0, "BETA", 30.0, "GAMMA", 30.0), original),
                () -> assertEquals(original, firstRebalance),
                () -> assertEquals(firstRebalance, secondRebalance),
                () -> assertEquals(90.0, allocated(firstRebalance), DELTA),
                () -> assertNoNegativeAllocations(firstRebalance));
    }

    @Test
    void leastLoadedStrategyRebalancePreservesCurrentEqualShareContract() {
        addServers(server("LOW", 10.0, 100.0), server("HIGH", 90.0, 100.0));
        balancer.roundRobin(120.0);
        balancer.setStrategy(LoadBalancer.Strategy.LEAST_LOADED);

        Map<String, Double> rebalanced = balancer.rebalanceExistingLoad();

        assertAll("least-loaded rebalance contract",
                () -> assertEquals(Map.of("LOW", 60.0, "HIGH", 60.0), rebalanced),
                () -> assertEquals(120.0, allocated(rebalanced), DELTA),
                () -> assertNoNegativeAllocations(rebalanced));
    }

    @Test
    void removedServerAccumulatedLoadIsReconciledBeforeRebalance() {
        Server removed = server("REMOVE", 10.0, 100.0);
        Server keep = server("KEEP", 20.0, 100.0);
        addServers(removed, keep);
        balancer.roundRobin(100.0);

        balancer.removeServer("REMOVE");

        Map<String, Double> rebalanced = balancer.rebalanceExistingLoad();

        assertAll("removed server accumulated-load reconciliation",
                () -> assertFalse(rebalanced.containsKey("REMOVE")),
                () -> assertEquals(Map.of("KEEP", 50.0), rebalanced),
                () -> assertEquals(50.0, allocated(rebalanced), DELTA),
                () -> assertNoNegativeAllocations(rebalanced));
    }

    @Test
    void duplicateReplacementClearsStaleAccumulatedLoadBeforeNewAllocation() {
        Server original = server("SAME", 10.0, 100.0);
        Server replacement = server("SAME", 20.0, 100.0);
        Server peer = server("PEER", 30.0, 100.0);
        balancer.addServer(original);
        balancer.roundRobin(80.0);
        balancer.addServer(peer);

        balancer.addServer(replacement);

        Map<String, Double> staleRebalance = balancer.rebalanceExistingLoad();
        Map<String, Double> replacementAllocation = balancer.roundRobin(60.0);
        Map<String, Double> replacementRebalance = balancer.rebalanceExistingLoad();

        assertAll("duplicate replacement accumulated-load contract",
                () -> assertTrue(staleRebalance.isEmpty()),
                () -> assertEquals(Map.of("PEER", 30.0, "SAME", 30.0), replacementAllocation),
                () -> assertEquals(replacementAllocation, replacementRebalance),
                () -> assertEquals(60.0, allocated(replacementRebalance), DELTA),
                () -> assertNoNegativeAllocations(replacementRebalance));
    }

    @Test
    void capacityAwareAndPredictiveResultAllocationsFeedRebalanceWithoutUnallocatedLoad() {
        addServers(server("OPEN", 20.0, 100.0), server("TIGHT", 40.0, 100.0));

        LoadDistributionResult capacityAware = balancer.capacityAwareWithResult(70.0);
        Map<String, Double> capacityRebalance = balancer.rebalanceExistingLoad();

        LoadBalancer predictiveBalancer = new LoadBalancer();
        try {
            predictiveBalancer.addServer(server("OPEN", 20.0, 100.0));
            predictiveBalancer.addServer(server("TIGHT", 40.0, 100.0));

            LoadDistributionResult predictive = predictiveBalancer.predictiveLoadBalancingWithResult(70.0);
            Map<String, Double> predictiveRebalance = predictiveBalancer.rebalanceExistingLoad();

            assertAll("result allocation rebalance contract",
                    () -> assertEquals(0.0, capacityAware.unallocatedLoad(), DELTA),
                    () -> assertEquals(70.0, allocated(capacityAware.allocations()), DELTA),
                    () -> assertEquals(Map.of("OPEN", 35.0, "TIGHT", 35.0), capacityRebalance),
                    () -> assertEquals(0.0, predictive.unallocatedLoad(), DELTA),
                    () -> assertEquals(70.0, allocated(predictive.allocations()), DELTA),
                    () -> assertEquals(Map.of("OPEN", 35.0, "TIGHT", 35.0), predictiveRebalance),
                    () -> assertNoNegativeAllocations(capacityAware.allocations()),
                    () -> assertNoNegativeAllocations(predictive.allocations()),
                    () -> assertNoNegativeAllocations(capacityRebalance),
                    () -> assertNoNegativeAllocations(predictiveRebalance));
        } finally {
            predictiveBalancer.shutdown();
        }
    }

    private void addServers(Server... servers) {
        for (Server server : servers) {
            balancer.addServer(server);
        }
    }

    private static Server server(String id, double loadScore, double capacity) {
        Server server = new Server(id, loadScore, loadScore, loadScore);
        server.setWeight(1.0);
        server.setCapacity(capacity);
        return server;
    }

    private static double allocated(Map<String, Double> allocations) {
        return allocations.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private static void assertNoNegativeAllocations(Map<String, Double> allocations) {
        assertTrue(allocations.values().stream().allMatch(value -> value >= 0.0),
                "allocations must never be negative");
    }
}
