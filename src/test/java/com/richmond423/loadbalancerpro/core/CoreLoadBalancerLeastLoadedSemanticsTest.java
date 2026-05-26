package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoreLoadBalancerLeastLoadedSemanticsTest {
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
    void leastLoadedAllocatesEqualSharesAcrossUnequalHealthyLoadScores() {
        addServers(
                server("HIGH", 90.0),
                server("LOW", 10.0),
                server("MID", 50.0));

        Map<String, Double> first = balancer.leastLoaded(90.0);
        Map<String, Double> second = balancer.leastLoaded(90.0);

        assertAll("current least-loaded positive-load contract",
                () -> assertEquals(Map.of("LOW", 30.0, "MID", 30.0, "HIGH", 30.0), first),
                () -> assertEquals(first, second),
                () -> assertEquals(90.0, allocated(first), DELTA));
    }

    @Test
    void leastLoadedPositiveLoadMatchesRoundRobinShapeForSameHealthySet() {
        LoadBalancer leastLoadedBalancer = new LoadBalancer();
        LoadBalancer roundRobinBalancer = new LoadBalancer();
        try {
            addServers(leastLoadedBalancer,
                    server("LOW", 5.0),
                    server("MID", 45.0),
                    server("HIGH", 85.0));
            addServers(roundRobinBalancer,
                    server("LOW", 5.0),
                    server("MID", 45.0),
                    server("HIGH", 85.0));

            Map<String, Double> leastLoaded = leastLoadedBalancer.leastLoaded(120.0);
            Map<String, Double> roundRobin = roundRobinBalancer.roundRobin(120.0);

            assertAll("least-loaded currently uses equal positive-load shares like round robin",
                    () -> assertEquals(roundRobin, leastLoaded),
                    () -> assertEquals(40.0, leastLoaded.get("LOW"), DELTA),
                    () -> assertEquals(40.0, leastLoaded.get("MID"), DELTA),
                    () -> assertEquals(40.0, leastLoaded.get("HIGH"), DELTA),
                    () -> assertEquals(120.0, allocated(leastLoaded), DELTA));
        } finally {
            leastLoadedBalancer.shutdown();
            roundRobinBalancer.shutdown();
        }
    }

    @Test
    void leastLoadedZeroLoadCharacterizesLowestLoadScoreSelectionBeforeLoopStops() {
        addServers(
                server("HIGH", 90.0),
                server("MID", 50.0),
                server("LOW", 10.0));

        Map<String, Double> first = balancer.leastLoaded(0.0);
        Map<String, Double> second = balancer.leastLoaded(0.0);

        assertAll("zero-load least-loaded exposes sorted low-load first candidate",
                () -> assertEquals(Map.of("LOW", 0.0), first),
                () -> assertEquals(first, second),
                () -> assertEquals(0.0, allocated(first), DELTA));
    }

    @Test
    void leastLoadedRebalancePreservesEqualShareContractForAccumulatedLoad() {
        addServers(server("LOW", 10.0), server("HIGH", 90.0));

        balancer.roundRobin(120.0);
        balancer.setStrategy(LoadBalancer.Strategy.LEAST_LOADED);

        Map<String, Double> rebalanced = balancer.rebalanceExistingLoad();

        assertAll("least-loaded rebalance keeps current equal-share behavior",
                () -> assertEquals(Map.of("LOW", 60.0, "HIGH", 60.0), rebalanced),
                () -> assertEquals(120.0, allocated(rebalanced), DELTA));
    }

    @Test
    void leastLoadedEqualLoadScoreTiesAreRepeatableForIdenticalInputOrder() {
        addServers(server("ALPHA", 25.0), server("BETA", 25.0));

        Map<String, Double> first = balancer.leastLoaded(50.0);
        Map<String, Double> second = balancer.leastLoaded(50.0);

        assertAll("equal-load least-loaded ties remain deterministic for identical inputs",
                () -> assertEquals(Map.of("ALPHA", 25.0, "BETA", 25.0), first),
                () -> assertEquals(first, second),
                () -> assertEquals(50.0, allocated(first), DELTA));
    }

    private void addServers(Server... servers) {
        addServers(balancer, servers);
    }

    private static void addServers(LoadBalancer target, Server... servers) {
        for (Server server : servers) {
            target.addServer(server);
        }
    }

    private static Server server(String id, double loadScore) {
        Server server = new Server(id, loadScore, loadScore, loadScore);
        server.setWeight(1.0);
        server.setCapacity(100.0);
        return server;
    }

    private static double allocated(Map<String, Double> allocations) {
        return allocations.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }
}
