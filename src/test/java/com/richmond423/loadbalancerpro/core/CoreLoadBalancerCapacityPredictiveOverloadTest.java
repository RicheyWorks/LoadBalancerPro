package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoreLoadBalancerCapacityPredictiveOverloadTest {
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
    void capacityAwareOverloadCapsAllocationsAndReportsExactUnallocatedLoad() {
        Server constrained = server("CONSTRAINED", 80.0, 100.0);
        Server open = server("OPEN", 10.0, 80.0);
        addServers(constrained, open);

        LoadDistributionResult result = balancer.capacityAwareWithResult(130.0);

        assertAll("capacity-aware overload accounting",
                () -> assertEquals(70.0, result.allocations().get("OPEN"), DELTA),
                () -> assertEquals(20.0, result.allocations().get("CONSTRAINED"), DELTA),
                () -> assertEquals(40.0, result.unallocatedLoad(), DELTA),
                () -> assertEquals(130.0, allocated(result) + result.unallocatedLoad(), DELTA),
                () -> assertAllocationDoesNotExceed(result, open),
                () -> assertAllocationDoesNotExceed(result, constrained),
                () -> assertNoNegativeAllocations(result.allocations()),
                () -> assertNonNegativeUnallocatedLoad(result));
    }

    @Test
    void predictiveOverloadCapsAllocationsByPredictedCapacityAndIsRepeatable() {
        Server open = server("PREDICTED_OPEN", 20.0, 100.0);
        Server tight = server("PREDICTED_TIGHT", 60.0, 100.0);
        Server exhausted = server("PREDICTED_EXHAUSTED", 95.0, 100.0);
        addServers(open, tight, exhausted);

        LoadDistributionResult first = balancer.predictiveLoadBalancingWithResult(150.0);
        LoadDistributionResult second = balancer.predictiveLoadBalancingWithResult(150.0);

        assertAll("predictive overload accounting",
                () -> assertFalse(first.allocations().containsKey("PREDICTED_EXHAUSTED")),
                () -> assertEquals(first.allocations(), second.allocations()),
                () -> assertEquals(first.unallocatedLoad(), second.unallocatedLoad(), DELTA),
                () -> assertEquals(78.0, first.allocations().get("PREDICTED_OPEN"), DELTA),
                () -> assertEquals(34.0, first.allocations().get("PREDICTED_TIGHT"), DELTA),
                () -> assertEquals(38.0, first.unallocatedLoad(), DELTA),
                () -> assertEquals(150.0, allocated(first) + first.unallocatedLoad(), DELTA),
                () -> assertPredictiveAllocationDoesNotExceed(first, open),
                () -> assertPredictiveAllocationDoesNotExceed(first, tight),
                () -> assertNoNegativeAllocations(first.allocations()),
                () -> assertNonNegativeUnallocatedLoad(first));
    }

    @Test
    void capacityAwareAndPredictiveResultsExposeDeterministicLocalOverloadDifference() {
        Server lowLoad = server("LOW_LOAD", 20.0, 100.0);
        Server highLoad = server("HIGH_LOAD", 50.0, 100.0);
        addServers(lowLoad, highLoad);

        LoadDistributionResult capacityAware = balancer.capacityAwareWithResult(140.0);
        LoadDistributionResult predictive = balancer.predictiveLoadBalancingWithResult(140.0);

        assertAll("paired overload behavior for the same server set",
                () -> assertEquals(80.0, capacityAware.allocations().get("LOW_LOAD"), DELTA),
                () -> assertEquals(50.0, capacityAware.allocations().get("HIGH_LOAD"), DELTA),
                () -> assertEquals(10.0, capacityAware.unallocatedLoad(), DELTA),
                () -> assertEquals(78.0, predictive.allocations().get("LOW_LOAD"), DELTA),
                () -> assertEquals(45.0, predictive.allocations().get("HIGH_LOAD"), DELTA),
                () -> assertEquals(17.0, predictive.unallocatedLoad(), DELTA),
                () -> assertTrue(predictive.unallocatedLoad() > capacityAware.unallocatedLoad()),
                () -> assertEquals(140.0, allocated(capacityAware) + capacityAware.unallocatedLoad(), DELTA),
                () -> assertEquals(140.0, allocated(predictive) + predictive.unallocatedLoad(), DELTA),
                () -> assertNoNegativeAllocations(capacityAware.allocations()),
                () -> assertNoNegativeAllocations(predictive.allocations()),
                () -> assertNonNegativeUnallocatedLoad(capacityAware),
                () -> assertNonNegativeUnallocatedLoad(predictive));
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

    private static double allocated(LoadDistributionResult result) {
        return result.allocations().values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private static void assertAllocationDoesNotExceed(LoadDistributionResult result, Server server) {
        double allocation = result.allocations().getOrDefault(server.getServerId(), 0.0);
        assertTrue(allocation <= availableCapacity(server) + DELTA,
                server.getServerId() + " allocation must stay within available capacity");
    }

    private static void assertPredictiveAllocationDoesNotExceed(LoadDistributionResult result, Server server) {
        double allocation = result.allocations().getOrDefault(server.getServerId(), 0.0);
        assertTrue(allocation <= predictedAvailableCapacity(server) + DELTA,
                server.getServerId() + " allocation must stay within predicted capacity");
    }

    private static double availableCapacity(Server server) {
        return Math.max(0.0, server.getCapacity() - server.getLoadScore());
    }

    private static double predictedAvailableCapacity(Server server) {
        return Math.max(0.0, server.getCapacity() - (server.getLoadScore() * 1.1));
    }

    private static void assertNoNegativeAllocations(Map<String, Double> allocations) {
        assertTrue(allocations.values().stream().allMatch(value -> value >= 0.0),
                "allocations must never be negative");
    }

    private static void assertNonNegativeUnallocatedLoad(LoadDistributionResult result) {
        assertTrue(result.unallocatedLoad() >= 0.0, "unallocated load must never be negative");
    }
}
