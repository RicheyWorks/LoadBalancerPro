package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoreLoadBalancerWeightedDistributionInvariantTest {
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
    void positiveWeightsAllocateProportionallyAndPreserveRequestedTotal() {
        addServers(
                server("SMALL", 1.0),
                server("MEDIUM", 2.0),
                server("LARGE", 3.0));

        Map<String, Double> allocation = balancer.weightedDistribution(600.0);

        assertAll("positive weighted distribution contract",
                () -> assertEquals(3, allocation.size()),
                () -> assertEquals(100.0, allocation.get("SMALL"), DELTA),
                () -> assertEquals(200.0, allocation.get("MEDIUM"), DELTA),
                () -> assertEquals(300.0, allocation.get("LARGE"), DELTA),
                () -> assertEquals(600.0, allocatedTotal(allocation), DELTA),
                () -> assertNoNegativeAllocations(allocation));
    }

    @Test
    void zeroWeightServerReceivesZeroWhenPositiveWeightsExist() {
        addServers(
                server("PRIMARY", 5.0),
                server("ZERO", 0.0),
                server("SECONDARY", 5.0));

        Map<String, Double> allocation = balancer.weightedDistribution(90.0);

        assertAll("mixed zero and positive weight contract",
                () -> assertEquals(45.0, allocation.get("PRIMARY"), DELTA),
                () -> assertEquals(0.0, allocation.get("ZERO"), DELTA),
                () -> assertEquals(45.0, allocation.get("SECONDARY"), DELTA),
                () -> assertEquals(90.0, allocatedTotal(allocation), DELTA),
                () -> assertNoNegativeAllocations(allocation));
    }

    @Test
    void allZeroWeightsFallBackToEqualAllocation() {
        addServers(
                server("A", 0.0),
                server("B", 0.0),
                server("C", 0.0));

        Map<String, Double> allocation = balancer.weightedDistribution(75.0);

        assertAll("all-zero weighted distribution fallback",
                () -> assertEquals(25.0, allocation.get("A"), DELTA),
                () -> assertEquals(25.0, allocation.get("B"), DELTA),
                () -> assertEquals(25.0, allocation.get("C"), DELTA),
                () -> assertEquals(75.0, allocatedTotal(allocation), DELTA),
                () -> assertNoNegativeAllocations(allocation));
    }

    @Test
    void mixedLargeAndSmallWeightsRemainDeterministicAcrossRepeatedCalls() {
        addServers(
                server("BULK", 1_000.0),
                server("TRACE", 1.0),
                server("DISABLED", 0.0));

        Map<String, Double> first = balancer.weightedDistribution(1001.0);
        Map<String, Double> second = balancer.weightedDistribution(1001.0);

        assertAll("large and small weighted distribution determinism",
                () -> assertEquals(first, second),
                () -> assertEquals(1000.0, first.get("BULK"), DELTA),
                () -> assertEquals(1.0, first.get("TRACE"), DELTA),
                () -> assertEquals(0.0, first.get("DISABLED"), DELTA),
                () -> assertEquals(1001.0, allocatedTotal(first), DELTA),
                () -> assertNoNegativeAllocations(first));
    }

    @Test
    void invalidNegativeWeightIsRejectedBeforeDistribution() {
        Server server = new Server("INVALID", 10.0, 10.0, 10.0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> server.setWeight(-0.01));

        assertTrue(exception.getMessage().contains("Weight"));
    }

    private void addServers(Server... servers) {
        for (Server server : servers) {
            balancer.addServer(server);
        }
    }

    private static Server server(String id, double weight) {
        Server server = new Server(id, 10.0, 10.0, 10.0);
        server.setWeight(weight);
        server.setCapacity(500.0);
        return server;
    }

    private static double allocatedTotal(Map<String, Double> allocation) {
        return allocation.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private static void assertNoNegativeAllocations(Map<String, Double> allocation) {
        assertTrue(allocation.values().stream().allMatch(value -> value >= 0.0),
                "weighted allocations must never go negative");
    }
}
