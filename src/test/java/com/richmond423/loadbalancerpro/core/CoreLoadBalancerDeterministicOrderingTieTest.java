package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoreLoadBalancerDeterministicOrderingTieTest {
    private static final double DELTA = 0.0001;
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

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
    void facadeAllocationShapesRepeatForIdenticalInputs() {
        addServers(
                server("A", 10.0, 100.0, 1.0),
                server("B", 30.0, 100.0, 2.0),
                server("C", 50.0, 100.0, 3.0));

        Map<String, Double> roundRobin = balancer.roundRobin(90.0);
        Map<String, Double> leastLoaded = balancer.leastLoaded(90.0);
        Map<String, Double> weighted = balancer.weightedDistribution(90.0);
        Map<String, Double> consistentHashing = balancer.consistentHashing(90.0, 9);
        LoadDistributionResult capacityAware = balancer.capacityAwareWithResult(90.0);
        LoadDistributionResult predictive = balancer.predictiveLoadBalancingWithResult(90.0);

        assertAll("same inputs keep reviewer-visible facade allocation shapes stable",
                () -> assertEquals(roundRobin, balancer.roundRobin(90.0)),
                () -> assertEquals(leastLoaded, balancer.leastLoaded(90.0)),
                () -> assertEquals(weighted, balancer.weightedDistribution(90.0)),
                () -> assertEquals(consistentHashing, balancer.consistentHashing(90.0, 9)),
                () -> assertEquals(capacityAware.allocations(), balancer.capacityAwareWithResult(90.0).allocations()),
                () -> assertEquals(capacityAware.unallocatedLoad(),
                        balancer.capacityAwareWithResult(90.0).unallocatedLoad(), DELTA),
                () -> assertEquals(predictive.allocations(),
                        balancer.predictiveLoadBalancingWithResult(90.0).allocations()),
                () -> assertEquals(predictive.unallocatedLoad(),
                        balancer.predictiveLoadBalancingWithResult(90.0).unallocatedLoad(), DELTA));
    }

    @Test
    void capacityAwareTieOrderingUsesServerIdAcrossInsertionOrderAndRepeatedCalls() {
        LoadBalancer first = new LoadBalancer();
        LoadBalancer second = new LoadBalancer();
        try {
            first.addServer(server("B", 0.0, 80.0, 1.0));
            first.addServer(server("A", 0.0, 40.0, 1.0));
            second.addServer(server("A", 0.0, 40.0, 1.0));
            second.addServer(server("B", 0.0, 80.0, 1.0));

            LoadDistributionResult firstResult = first.capacityAwareWithResult(60.0);
            LoadDistributionResult secondResult = second.capacityAwareWithResult(60.0);
            LoadDistributionResult repeated = first.capacityAwareWithResult(60.0);

            assertAll("capacity-aware deterministic tied load-ratio ordering",
                    () -> assertEquals(List.of("A", "B"), keyOrder(firstResult.allocations())),
                    () -> assertEquals(List.of("A", "B"), keyOrder(secondResult.allocations())),
                    () -> assertEquals(firstResult.allocations(), secondResult.allocations()),
                    () -> assertEquals(firstResult.allocations(), repeated.allocations()),
                    () -> assertEquals(20.0, firstResult.allocations().get("A"), DELTA),
                    () -> assertEquals(40.0, firstResult.allocations().get("B"), DELTA),
                    () -> assertEquals(0.0, firstResult.unallocatedLoad(), DELTA));
        } finally {
            first.shutdown();
            second.shutdown();
        }
    }

    @Test
    void predictiveTieOrderingUsesServerIdAcrossInsertionOrderAndRepeatedCalls() {
        LoadBalancer first = new LoadBalancer();
        LoadBalancer second = new LoadBalancer();
        try {
            first.addServer(server("B", 25.0, 100.0, 1.0));
            first.addServer(server("A", 25.0, 100.0, 1.0));
            second.addServer(server("A", 25.0, 100.0, 1.0));
            second.addServer(server("B", 25.0, 100.0, 1.0));

            LoadDistributionResult firstResult = first.predictiveLoadBalancingWithResult(80.0);
            LoadDistributionResult secondResult = second.predictiveLoadBalancingWithResult(80.0);
            LoadDistributionResult repeated = first.predictiveLoadBalancingWithResult(80.0);

            assertAll("predictive deterministic equal predicted-capacity ordering",
                    () -> assertEquals(List.of("A", "B"), keyOrder(firstResult.allocations())),
                    () -> assertEquals(List.of("A", "B"), keyOrder(secondResult.allocations())),
                    () -> assertEquals(firstResult.allocations(), secondResult.allocations()),
                    () -> assertEquals(firstResult.allocations(), repeated.allocations()),
                    () -> assertEquals(40.0, firstResult.allocations().get("A"), DELTA),
                    () -> assertEquals(40.0, firstResult.allocations().get("B"), DELTA),
                    () -> assertEquals(0.0, firstResult.unallocatedLoad(), DELTA));
        } finally {
            first.shutdown();
            second.shutdown();
        }
    }

    @Test
    void requestLevelTieStrategiesUseStableServerIdWhileRoundRobinDocumentsRotation() {
        List<ServerStateVector> reversedEquivalentCandidates = List.of(
                state("beta"),
                state("alpha"));
        WeightedLeastLoadStrategy leastLoad = new WeightedLeastLoadStrategy(FIXED_CLOCK);
        WeightedLeastConnectionsRoutingStrategy leastConnections =
                new WeightedLeastConnectionsRoutingStrategy(FIXED_CLOCK);
        TailLatencyPowerOfTwoStrategy tailLatency = new TailLatencyPowerOfTwoStrategy(
                new ServerScoreCalculator(), new Random(7), FIXED_CLOCK);
        RoundRobinRoutingStrategy roundRobin = new RoundRobinRoutingStrategy(FIXED_CLOCK);

        RoutingDecision leastLoadDecision = leastLoad.choose(reversedEquivalentCandidates);
        RoutingDecision leastConnectionsDecision = leastConnections.choose(reversedEquivalentCandidates);
        RoutingDecision tailLatencyDecision = tailLatency.choose(reversedEquivalentCandidates);
        RoutingDecision firstRoundRobin = roundRobin.choose(reversedEquivalentCandidates);
        RoutingDecision secondRoundRobin = roundRobin.choose(reversedEquivalentCandidates);

        assertAll("request-level tie and repeated-call behavior",
                () -> assertEquals("alpha", leastLoadDecision.chosenServer().orElseThrow().serverId()),
                () -> assertEquals(List.of("alpha", "beta"),
                        leastLoadDecision.explanation().candidateServersConsidered()),
                () -> assertEquals("alpha", leastConnectionsDecision.chosenServer().orElseThrow().serverId()),
                () -> assertEquals(List.of("alpha", "beta"),
                        leastConnectionsDecision.explanation().candidateServersConsidered()),
                () -> assertEquals("alpha", tailLatencyDecision.chosenServer().orElseThrow().serverId()),
                () -> assertTrue(tailLatencyDecision.explanation().reason().contains("stable server-id tie-break")),
                () -> assertEquals("beta", firstRoundRobin.chosenServer().orElseThrow().serverId()),
                () -> assertEquals("alpha", secondRoundRobin.chosenServer().orElseThrow().serverId()),
                () -> assertEquals(List.of("beta", "alpha"),
                        firstRoundRobin.explanation().candidateServersConsidered()),
                () -> assertTrue(firstRoundRobin.explanation().reason().contains("position 1 of 2")),
                () -> assertTrue(secondRoundRobin.explanation().reason().contains("position 2 of 2")));
    }

    @Test
    void routingComparisonReportRepeatsStableTieChoicesAndStrategyOrder() {
        RoutingComparisonEngine engine = new RoutingComparisonEngine(
                new RoutingStrategyRegistry(List.of(
                        new WeightedLeastLoadStrategy(FIXED_CLOCK),
                        new WeightedLeastConnectionsRoutingStrategy(FIXED_CLOCK),
                        new TailLatencyPowerOfTwoStrategy(new ServerScoreCalculator(), new Random(3), FIXED_CLOCK))),
                FIXED_CLOCK);
        List<RoutingStrategyId> requested = List.of(
                RoutingStrategyId.WEIGHTED_LEAST_LOAD,
                RoutingStrategyId.WEIGHTED_LEAST_CONNECTIONS,
                RoutingStrategyId.TAIL_LATENCY_POWER_OF_TWO);
        List<ServerStateVector> candidates = List.of(state("beta"), state("alpha"));

        RoutingComparisonReport first = engine.compare(candidates, requested);
        RoutingComparisonReport second = engine.compare(candidates, requested);

        assertAll("routing comparison tie report determinism",
                () -> assertEquals(requested, first.requestedStrategies()),
                () -> assertEquals(requested, second.requestedStrategies()),
                () -> assertEquals(strategyIds(first), strategyIds(second)),
                () -> assertEquals(chosenServerIds(first), chosenServerIds(second)),
                () -> assertEquals(List.of("alpha", "alpha", "alpha"), chosenServerIds(first)),
                () -> assertEquals(NOW, first.timestamp()),
                () -> assertEquals(NOW, second.timestamp()));
    }

    private void addServers(Server... servers) {
        for (Server server : servers) {
            balancer.addServer(server);
        }
    }

    private static Server server(String id, double loadScore, double capacity, double weight) {
        Server server = new Server(id, loadScore, loadScore, loadScore);
        server.setCapacity(capacity);
        server.setWeight(weight);
        return server;
    }

    private static ServerStateVector state(String id) {
        return new ServerStateVector(id, true, 10, 100.0, 100.0,
                25.0, 50.0, 100.0, 0.01, 2, NOW);
    }

    private static List<String> keyOrder(Map<String, Double> allocations) {
        return new ArrayList<>(allocations.keySet());
    }

    private static List<RoutingStrategyId> strategyIds(RoutingComparisonReport report) {
        return report.results().stream()
                .map(RoutingComparisonResult::strategyId)
                .toList();
    }

    private static List<String> chosenServerIds(RoutingComparisonReport report) {
        return report.results().stream()
                .map(result -> result.decision().orElseThrow().chosenServer().orElseThrow().serverId())
                .toList();
    }
}
