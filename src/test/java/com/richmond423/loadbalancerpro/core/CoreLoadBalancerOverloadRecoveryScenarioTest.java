package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoreLoadBalancerOverloadRecoveryScenarioTest {
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
    void capacityExhaustionReportsUnallocatedLoadUntilCapacityIsRestored() {
        Server saturated = server("SATURATED", 90.0, 100.0);
        Server limited = server("LIMITED", 80.0, 100.0);
        addServers(saturated, limited);

        LoadDistributionResult capacityExhausted = balancer.capacityAwareWithResult(60.0);
        LoadDistributionResult predictiveExhausted = balancer.predictiveLoadBalancingWithResult(60.0);

        saturated.setCapacity(160.0);
        limited.setCapacity(160.0);

        LoadDistributionResult capacityRestored = balancer.capacityAwareWithResult(60.0);
        LoadDistributionResult predictiveRestored = balancer.predictiveLoadBalancingWithResult(60.0);

        assertAll("capacity exhaustion and restored capacity contract",
                () -> assertEquals(Set.of("LIMITED", "SATURATED"), capacityExhausted.allocations().keySet()),
                () -> assertEquals(20.0, capacityExhausted.allocations().get("LIMITED"), DELTA),
                () -> assertEquals(10.0, capacityExhausted.allocations().get("SATURATED"), DELTA),
                () -> assertEquals(30.0, capacityExhausted.unallocatedLoad(), DELTA),
                () -> assertEquals(Set.of("LIMITED", "SATURATED"), predictiveExhausted.allocations().keySet()),
                () -> assertEquals(12.0, predictiveExhausted.allocations().get("LIMITED"), DELTA),
                () -> assertEquals(1.0, predictiveExhausted.allocations().get("SATURATED"), DELTA),
                () -> assertEquals(47.0, predictiveExhausted.unallocatedLoad(), DELTA),
                () -> assertTrue(predictiveExhausted.unallocatedLoad()
                        > capacityExhausted.unallocatedLoad()),
                () -> assertEquals(60.0, allocated(capacityRestored), DELTA),
                () -> assertEquals(0.0, capacityRestored.unallocatedLoad(), DELTA),
                () -> assertEquals(60.0, allocated(predictiveRestored), DELTA),
                () -> assertEquals(0.0, predictiveRestored.unallocatedLoad(), DELTA),
                () -> assertNoNegativeAllocations(capacityExhausted),
                () -> assertNoNegativeAllocations(predictiveExhausted),
                () -> assertNoNegativeAllocations(capacityRestored),
                () -> assertNoNegativeAllocations(predictiveRestored));
    }

    @Test
    void allUnhealthyDegradationFailsClosedUntilServerRecovers() {
        Server primary = server("PRIMARY", 20.0, 100.0);
        Server secondary = server("SECONDARY", 30.0, 100.0);
        primary.setHealthy(false);
        secondary.setHealthy(false);
        addServers(primary, secondary);

        LoadDistributionResult degradedCapacity = balancer.capacityAwareWithResult(80.0);
        LoadDistributionResult degradedPredictive = balancer.predictiveLoadBalancingWithResult(80.0);

        primary.setHealthy(true);

        Map<String, Double> recoveredRoundRobin = balancer.roundRobin(40.0);
        LoadDistributionResult recoveredCapacity = balancer.capacityAwareWithResult(80.0);
        LoadDistributionResult recoveredPredictive = balancer.predictiveLoadBalancingWithResult(80.0);

        secondary.setHealthy(true);

        LoadDistributionResult bothRecovered = balancer.capacityAwareWithResult(80.0);

        assertAll("all-unhealthy degradation and recovered candidate re-entry",
                () -> assertTrue(degradedCapacity.allocations().isEmpty()),
                () -> assertEquals(80.0, degradedCapacity.unallocatedLoad(), DELTA),
                () -> assertTrue(degradedPredictive.allocations().isEmpty()),
                () -> assertEquals(80.0, degradedPredictive.unallocatedLoad(), DELTA),
                () -> assertEquals(Map.of("PRIMARY", 40.0), recoveredRoundRobin),
                () -> assertEquals(Map.of("PRIMARY", 80.0), recoveredCapacity.allocations()),
                () -> assertEquals(0.0, recoveredCapacity.unallocatedLoad(), DELTA),
                () -> assertEquals(Map.of("PRIMARY", 78.0), recoveredPredictive.allocations()),
                () -> assertEquals(2.0, recoveredPredictive.unallocatedLoad(), DELTA),
                () -> assertFalse(recoveredCapacity.allocations().containsKey("SECONDARY")),
                () -> assertEquals(80.0, allocated(bothRecovered), DELTA),
                () -> assertEquals(0.0, bothRecovered.unallocatedLoad(), DELTA),
                () -> assertTrue(bothRecovered.allocations().containsKey("PRIMARY")),
                () -> assertTrue(bothRecovered.allocations().containsKey("SECONDARY")));
    }

    @Test
    void healthCheckAllUnhealthyDegradationCanRecoverThroughRestoredCandidate() {
        Server failing = server("FAILING", 20.0, 100.0);
        addServers(failing);
        balancer.roundRobin(60.0);

        failing.updateMetrics(100.0, 100.0, 100.0);
        balancer.checkServerHealth();

        LoadDistributionResult degraded = balancer.capacityAwareWithResult(60.0);

        Server restored = server("RESTORED", 10.0, 100.0);
        balancer.addServer(restored);

        LoadDistributionResult recovered = balancer.capacityAwareWithResult(60.0);
        Map<String, Double> hashed = balancer.consistentHashing(60.0, 6);

        assertAll("health-check degradation and restored candidate contract",
                () -> assertFalse(failing.isHealthy()),
                () -> assertNull(balancer.getServer("FAILING")),
                () -> assertTrue(degraded.allocations().isEmpty()),
                () -> assertEquals(60.0, degraded.unallocatedLoad(), DELTA),
                () -> assertSame(restored, balancer.getServer("RESTORED")),
                () -> assertEquals(Map.of("RESTORED", 60.0), recovered.allocations()),
                () -> assertEquals(0.0, recovered.unallocatedLoad(), DELTA),
                () -> assertEquals(Set.of("RESTORED"), hashed.keySet()),
                () -> assertNoNegativeAllocations(recovered));
    }

    @Test
    void restoredZeroCapacityScenarioBecomesRepeatableAfterCapacityReturns() {
        Server first = server("FIRST", 30.0, 0.0);
        Server second = server("SECOND", 40.0, 0.0);
        addServers(first, second);

        LoadDistributionResult noCapacity = balancer.capacityAwareWithResult(100.0);
        LoadDistributionResult noPredictedCapacity = balancer.predictiveLoadBalancingWithResult(100.0);

        first.setCapacity(120.0);
        second.setCapacity(120.0);

        LoadDistributionResult restoredCapacity = balancer.capacityAwareWithResult(100.0);
        LoadDistributionResult repeatedRestoredCapacity = balancer.capacityAwareWithResult(100.0);
        LoadDistributionResult restoredPredictive = balancer.predictiveLoadBalancingWithResult(100.0);
        LoadDistributionResult repeatedRestoredPredictive = balancer.predictiveLoadBalancingWithResult(100.0);

        assertAll("restored zero-capacity scenario contract",
                () -> assertTrue(noCapacity.allocations().isEmpty()),
                () -> assertEquals(100.0, noCapacity.unallocatedLoad(), DELTA),
                () -> assertTrue(noPredictedCapacity.allocations().isEmpty()),
                () -> assertEquals(100.0, noPredictedCapacity.unallocatedLoad(), DELTA),
                () -> assertEquals(restoredCapacity.allocations(), repeatedRestoredCapacity.allocations()),
                () -> assertEquals(restoredCapacity.unallocatedLoad(),
                        repeatedRestoredCapacity.unallocatedLoad(), DELTA),
                () -> assertEquals(restoredPredictive.allocations(), repeatedRestoredPredictive.allocations()),
                () -> assertEquals(restoredPredictive.unallocatedLoad(),
                        repeatedRestoredPredictive.unallocatedLoad(), DELTA),
                () -> assertEquals(100.0, allocated(restoredCapacity), DELTA),
                () -> assertEquals(0.0, restoredCapacity.unallocatedLoad(), DELTA),
                () -> assertEquals(100.0, allocated(restoredPredictive), DELTA),
                () -> assertEquals(0.0, restoredPredictive.unallocatedLoad(), DELTA),
                () -> assertNoNegativeAllocations(restoredCapacity),
                () -> assertNoNegativeAllocations(restoredPredictive));
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

    private static void assertNoNegativeAllocations(LoadDistributionResult result) {
        assertTrue(result.unallocatedLoad() >= 0.0, "unallocated load must never be negative");
        assertTrue(result.allocations().values().stream().allMatch(value -> value >= 0.0),
                "allocations must never be negative");
    }
}
