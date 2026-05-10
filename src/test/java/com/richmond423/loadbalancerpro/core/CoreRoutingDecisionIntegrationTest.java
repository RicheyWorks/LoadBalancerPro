package com.richmond423.loadbalancerpro.core;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CoreRoutingDecisionIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void loadBalancerFacadeExcludesUnhealthyServersAndReincludesRecoveredServer() {
        LoadBalancer balancer = new LoadBalancer();
        try {
            Server stable = server("stable", 10.0, 10.0, 10.0, 1.0, 100.0);
            Server recovering = server("recovering", 20.0, 20.0, 20.0, 3.0, 120.0);
            Server down = server("down", 1.0, 1.0, 1.0, 10.0, 100.0);
            recovering.setHealthy(false);
            down.setHealthy(false);

            balancer.addServer(stable);
            balancer.addServer(recovering);
            balancer.addServer(down);

            Map<String, Double> onlyStableRoundRobin = balancer.roundRobin(60.0);
            Map<String, Double> onlyStableWeighted = balancer.weightedDistribution(80.0);
            LoadDistributionResult onlyStableCapacity = balancer.capacityAwareWithResult(40.0);
            Map<String, Double> onlyStablePredictive = balancer.predictiveLoadBalancing(40.0);

            assertAll("unhealthy servers excluded while only stable is healthy",
                    () -> assertEquals(60.0, onlyStableRoundRobin.get("stable"), 0.01),
                    () -> assertEquals(80.0, onlyStableWeighted.get("stable"), 0.01),
                    () -> assertEquals(40.0, onlyStableCapacity.allocations().get("stable"), 0.01),
                    () -> assertEquals(0.0, onlyStableCapacity.unallocatedLoad(), 0.01),
                    () -> assertEquals(40.0, onlyStablePredictive.get("stable"), 0.01),
                    () -> assertOnlyContains(onlyStableRoundRobin, "stable"),
                    () -> assertOnlyContains(onlyStableWeighted, "stable"),
                    () -> assertOnlyContains(onlyStableCapacity.allocations(), "stable"),
                    () -> assertOnlyContains(onlyStablePredictive, "stable"));

            recovering.setHealthy(true);

            Map<String, Double> recoveredRoundRobin = balancer.roundRobin(80.0);
            Map<String, Double> recoveredWeighted = balancer.weightedDistribution(80.0);
            LoadDistributionResult recoveredCapacity = balancer.capacityAwareWithResult(100.0);
            Map<String, Double> recoveredPredictive = balancer.predictiveLoadBalancing(80.0);

            assertAll("recovered server re-enters routing while down server remains excluded",
                    () -> assertEquals(40.0, recoveredRoundRobin.get("stable"), 0.01),
                    () -> assertEquals(40.0, recoveredRoundRobin.get("recovering"), 0.01),
                    () -> assertEquals(20.0, recoveredWeighted.get("stable"), 0.01),
                    () -> assertEquals(60.0, recoveredWeighted.get("recovering"), 0.01),
                    () -> assertEquals(100.0, sum(recoveredCapacity.allocations()), 0.01),
                    () -> assertEquals(0.0, recoveredCapacity.unallocatedLoad(), 0.01),
                    () -> assertTrue(recoveredCapacity.allocations().get("stable") > 0.0),
                    () -> assertTrue(recoveredCapacity.allocations().get("recovering") > 0.0),
                    () -> assertEquals(80.0, sum(recoveredPredictive), 0.01),
                    () -> assertTrue(recoveredPredictive.get("stable") > 0.0),
                    () -> assertTrue(recoveredPredictive.get("recovering") > 0.0),
                    () -> assertFalse(recoveredRoundRobin.containsKey("down")),
                    () -> assertFalse(recoveredWeighted.containsKey("down")),
                    () -> assertFalse(recoveredCapacity.allocations().containsKey("down")),
                    () -> assertFalse(recoveredPredictive.containsKey("down")));
        } finally {
            balancer.shutdown();
        }
    }

    @Test
    void routingComparisonEngineExcludesUnhealthyCandidatesAcrossWeightedAndLeastConnectionStrategies() {
        RoutingComparisonEngine engine = routingEngine();
        List<ServerStateVector> candidates = List.of(
                state("unhealthy-fast", false, 0, 100.0, 100.0, 50.0, 5.0, 10.0, 20.0, 0.0, 0),
                state("quiet", true, 3, 1.0, 100.0, 100.0, 18.0, 30.0, 60.0, 0.01, 1),
                state("weighted", true, 8, 4.0, 100.0, 100.0, 20.0, 40.0, 80.0, 0.01, 2));

        RoutingComparisonReport report = engine.compare(candidates);

        assertEquals(5, report.results().size());
        assertTrue(report.results().stream().allMatch(RoutingComparisonResult::successful));
        for (RoutingComparisonResult result : report.results()) {
            RoutingDecision decision = result.decision().orElseThrow();
            assertTrue(decision.chosenServer().isPresent(), "Expected a healthy choice for " + result.strategyId());
            assertNotEquals("unhealthy-fast", decision.chosenServer().orElseThrow().serverId());
            assertFalse(decision.explanation().candidateServersConsidered().contains("unhealthy-fast"));
        }

        assertEquals("weighted", resultFor(report, RoutingStrategyId.WEIGHTED_LEAST_CONNECTIONS)
                .decision().orElseThrow().chosenServer().orElseThrow().serverId());
        assertEquals("weighted", resultFor(report, RoutingStrategyId.WEIGHTED_ROUND_ROBIN)
                .decision().orElseThrow().chosenServer().orElseThrow().serverId());
        assertEquals("quiet", resultFor(report, RoutingStrategyId.ROUND_ROBIN)
                .decision().orElseThrow().chosenServer().orElseThrow().serverId());
    }

    @Test
    void routingComparisonEngineReturnsNoDecisionForEveryStrategyWhenNoHealthyCandidatesExist() {
        RoutingComparisonEngine engine = routingEngine();
        List<ServerStateVector> allUnhealthy = List.of(
                state("unhealthy-a", false, 0, 5.0, 100.0, 100.0, 5.0, 10.0, 20.0, 0.0, 0),
                state("unhealthy-b", false, 0, 7.0, 100.0, 100.0, 4.0, 8.0, 16.0, 0.0, 0));

        RoutingComparisonReport report = engine.compare(allUnhealthy);

        assertEquals(List.of(
                RoutingStrategyId.TAIL_LATENCY_POWER_OF_TWO,
                RoutingStrategyId.WEIGHTED_LEAST_LOAD,
                RoutingStrategyId.WEIGHTED_LEAST_CONNECTIONS,
                RoutingStrategyId.WEIGHTED_ROUND_ROBIN,
                RoutingStrategyId.ROUND_ROBIN), report.requestedStrategies());
        for (RoutingComparisonResult result : report.results()) {
            RoutingDecision decision = result.decision().orElseThrow();
            assertEquals(RoutingComparisonResult.Status.SUCCESS, result.status());
            assertTrue(decision.chosenServer().isEmpty(), "Expected no choice for " + result.strategyId());
            assertTrue(decision.explanation().candidateServersConsidered().isEmpty());
            assertTrue(result.reason().contains("No healthy eligible servers"));
        }
    }

    @Test
    void routingComparisonEngineRoundRobinProgressesAndWrapsAcrossCalls() {
        RoutingComparisonEngine engine = new RoutingComparisonEngine(
                new RoutingStrategyRegistry(List.of(new RoundRobinRoutingStrategy(FIXED_CLOCK))), FIXED_CLOCK);
        List<ServerStateVector> candidates = List.of(
                state("unhealthy-first", false, 0, 9.0, 100.0, 100.0, 1.0, 2.0, 3.0, 0.0, 0),
                state("alpha", true, 0, 1.0, 100.0, 100.0, 10.0, 20.0, 30.0, 0.0, 0),
                state("beta", true, 0, 1.0, 100.0, 100.0, 10.0, 20.0, 30.0, 0.0, 0));

        assertEquals("alpha", chosen(engine.compare(candidates)));
        assertEquals("beta", chosen(engine.compare(candidates)));
        assertEquals("alpha", chosen(engine.compare(candidates)));
    }

    private RoutingComparisonEngine routingEngine() {
        return new RoutingComparisonEngine(
                new RoutingStrategyRegistry(List.of(
                        new TailLatencyPowerOfTwoStrategy(new ServerScoreCalculator(), new Random(3), FIXED_CLOCK),
                        new WeightedLeastLoadStrategy(FIXED_CLOCK),
                        new WeightedLeastConnectionsRoutingStrategy(FIXED_CLOCK),
                        new WeightedRoundRobinRoutingStrategy(FIXED_CLOCK),
                        new RoundRobinRoutingStrategy(FIXED_CLOCK))),
                FIXED_CLOCK);
    }

    private Server server(String id,
                          double cpuUsage,
                          double memoryUsage,
                          double diskUsage,
                          double weight,
                          double capacity) {
        Server server = new Server(id, cpuUsage, memoryUsage, diskUsage);
        server.setWeight(weight);
        server.setCapacity(capacity);
        return server;
    }

    private ServerStateVector state(String id,
                                    boolean healthy,
                                    int inFlight,
                                    double weight,
                                    double configuredCapacity,
                                    double estimatedConcurrencyLimit,
                                    double averageLatencyMillis,
                                    double p95LatencyMillis,
                                    double p99LatencyMillis,
                                    double recentErrorRate,
                                    int queueDepth) {
        return new ServerStateVector(
                id,
                healthy,
                inFlight,
                OptionalDouble.of(configuredCapacity),
                OptionalDouble.of(estimatedConcurrencyLimit),
                weight,
                averageLatencyMillis,
                p95LatencyMillis,
                p99LatencyMillis,
                recentErrorRate,
                OptionalInt.of(queueDepth),
                NetworkAwarenessSignal.neutral(id, NOW),
                NOW);
    }

    private RoutingComparisonResult resultFor(RoutingComparisonReport report, RoutingStrategyId strategyId) {
        return report.results().stream()
                .filter(result -> result.strategyId() == strategyId)
                .findFirst()
                .orElseThrow();
    }

    private String chosen(RoutingComparisonReport report) {
        return report.results().get(0)
                .decision().orElseThrow()
                .chosenServer().orElseThrow()
                .serverId();
    }

    private void assertOnlyContains(Map<String, Double> allocations, String serverId) {
        assertEquals(1, allocations.size());
        assertTrue(allocations.containsKey(serverId));
    }

    private double sum(Map<String, Double> allocations) {
        return allocations.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}
