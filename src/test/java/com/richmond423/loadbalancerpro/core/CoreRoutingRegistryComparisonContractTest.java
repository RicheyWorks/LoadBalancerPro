package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

class CoreRoutingRegistryComparisonContractTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final List<RoutingStrategyId> DEFAULT_ORDER = List.of(
            RoutingStrategyId.TAIL_LATENCY_POWER_OF_TWO,
            RoutingStrategyId.WEIGHTED_LEAST_LOAD,
            RoutingStrategyId.WEIGHTED_LEAST_CONNECTIONS,
            RoutingStrategyId.WEIGHTED_ROUND_ROBIN,
            RoutingStrategyId.ROUND_ROBIN);

    @Test
    void defaultRegistryOrderAndStrategyTypesAreReviewerVisible() {
        RoutingStrategyRegistry registry = RoutingStrategyRegistry.defaultRegistry();

        assertAll("default routing registry contract",
                () -> assertEquals(DEFAULT_ORDER, registry.registeredIds()),
                () -> assertInstanceOf(TailLatencyPowerOfTwoStrategy.class,
                        registry.require(RoutingStrategyId.TAIL_LATENCY_POWER_OF_TWO)),
                () -> assertInstanceOf(WeightedLeastLoadStrategy.class,
                        registry.require(RoutingStrategyId.WEIGHTED_LEAST_LOAD)),
                () -> assertInstanceOf(WeightedLeastConnectionsRoutingStrategy.class,
                        registry.require(RoutingStrategyId.WEIGHTED_LEAST_CONNECTIONS)),
                () -> assertInstanceOf(WeightedRoundRobinRoutingStrategy.class,
                        registry.require(RoutingStrategyId.WEIGHTED_ROUND_ROBIN)),
                () -> assertInstanceOf(RoundRobinRoutingStrategy.class,
                        registry.require(RoutingStrategyId.ROUND_ROBIN)),
                () -> assertTrue(registry.find(null).isEmpty()));
    }

    @Test
    void strategyIdLookupSupportsKnownAliasesAndRejectsUnknownNamesSafely() {
        assertAll("routing strategy id lookup contract",
                () -> assertEquals(RoutingStrategyId.TAIL_LATENCY_POWER_OF_TWO,
                        RoutingStrategyId.fromName("tail-latency-power-of-two").orElseThrow()),
                () -> assertEquals(RoutingStrategyId.WEIGHTED_LEAST_LOAD,
                        RoutingStrategyId.fromName("weighted_least_load").orElseThrow()),
                () -> assertEquals(RoutingStrategyId.WEIGHTED_LEAST_CONNECTIONS,
                        RoutingStrategyId.fromName("weighted-least-connections").orElseThrow()),
                () -> assertEquals(RoutingStrategyId.WEIGHTED_ROUND_ROBIN,
                        RoutingStrategyId.fromName("weighted-round-robin").orElseThrow()),
                () -> assertEquals(RoutingStrategyId.ROUND_ROBIN,
                        RoutingStrategyId.fromName("round-robin").orElseThrow()),
                () -> assertTrue(RoutingStrategyId.fromName("missing-strategy").isEmpty()),
                () -> assertTrue(RoutingStrategyId.fromName(" ").isEmpty()),
                () -> assertTrue(RoutingStrategyId.fromName(null).isEmpty()));
    }

    @Test
    void registryRejectsDuplicateStrategyIdsAndRequireReportsAbsentStrategies() {
        RoutingStrategyRegistry emptyRegistry = new RoutingStrategyRegistry(List.of());

        assertAll("registry absent and duplicate strategy contract",
                () -> assertTrue(emptyRegistry.find(RoutingStrategyId.ROUND_ROBIN).isEmpty()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> emptyRegistry.require(RoutingStrategyId.ROUND_ROBIN)),
                () -> assertThrows(IllegalArgumentException.class, () -> new RoutingStrategyRegistry(List.of(
                        new WeightedLeastLoadStrategy(FIXED_CLOCK),
                        new WeightedLeastLoadStrategy(FIXED_CLOCK)))));
    }

    @Test
    void comparisonEngineUsesRequestedOrderAndPreservesDuplicateRequests() {
        RoutingComparisonEngine engine = comparisonEngineWithFixedStrategies();
        List<RoutingStrategyId> requested = List.of(
                RoutingStrategyId.WEIGHTED_LEAST_LOAD,
                RoutingStrategyId.TAIL_LATENCY_POWER_OF_TWO,
                RoutingStrategyId.WEIGHTED_LEAST_LOAD);

        RoutingComparisonReport report = engine.compare(healthyCandidates(), requested);

        assertAll("requested strategy filtering contract",
                () -> assertEquals(requested, report.requestedStrategies()),
                () -> assertEquals(requested, strategyIds(report)),
                () -> assertEquals(3, report.results().size()),
                () -> assertTrue(report.results().stream().allMatch(RoutingComparisonResult::successful)),
                () -> assertEquals(NOW, report.timestamp()));
    }

    @Test
    void comparisonEngineReportsUnregisteredRequestedStrategiesWithoutCrashing() {
        RoutingComparisonEngine engine = new RoutingComparisonEngine(
                new RoutingStrategyRegistry(List.of(new WeightedLeastLoadStrategy(FIXED_CLOCK))), FIXED_CLOCK);

        RoutingComparisonReport report = engine.compare(healthyCandidates(), List.of(
                RoutingStrategyId.WEIGHTED_LEAST_LOAD,
                RoutingStrategyId.ROUND_ROBIN));

        RoutingComparisonResult supported = report.results().get(0);
        RoutingComparisonResult missing = report.results().get(1);

        assertAll("missing requested strategy contract",
                () -> assertEquals(RoutingComparisonResult.Status.SUCCESS, supported.status()),
                () -> assertTrue(supported.decision().isPresent()),
                () -> assertEquals(RoutingComparisonResult.Status.FAILED, missing.status()),
                () -> assertTrue(missing.decision().isEmpty()),
                () -> assertTrue(missing.reason().contains("not registered")),
                () -> assertEquals(List.of(RoutingStrategyId.WEIGHTED_LEAST_LOAD, RoutingStrategyId.ROUND_ROBIN),
                        report.requestedStrategies()));
    }

    @Test
    void comparisonResultsExposeStableDecisionAndExplanationFields() {
        RoutingComparisonReport report = comparisonEngineWithFixedStrategies().compare(healthyCandidates());

        assertAll("comparison report shape",
                () -> assertEquals(DEFAULT_ORDER, report.requestedStrategies()),
                () -> assertEquals(DEFAULT_ORDER, strategyIds(report)),
                () -> assertEquals(2, report.candidateCount()),
                () -> assertEquals(NOW, report.timestamp()),
                () -> assertTrue(report.results().stream().allMatch(RoutingComparisonResult::successful)));

        for (RoutingComparisonResult result : report.results()) {
            RoutingDecision decision = result.decision().orElseThrow();
            RoutingDecisionExplanation explanation = decision.explanation();

            assertAll("comparison explanation for " + result.strategyId(),
                    () -> assertEquals(result.reason(), explanation.reason()),
                    () -> assertFalse(explanation.strategyUsed().isBlank()),
                    () -> assertFalse(explanation.reason().isBlank()),
                    () -> assertEquals(NOW, explanation.timestamp()),
                    () -> assertTrue(decision.chosenServer().isPresent()),
                    () -> assertEquals(decision.chosenServer().orElseThrow().serverId(),
                            explanation.chosenServerId().orElseThrow()),
                    () -> assertFalse(explanation.candidateServersConsidered().isEmpty()));
        }
    }

    @Test
    void comparisonEngineReturnsSafeNoCandidateDecisionsForNoHealthyCandidates() {
        RoutingComparisonEngine engine = comparisonEngineWithFixedStrategies();

        RoutingComparisonReport emptyReport = engine.compare(List.of(), DEFAULT_ORDER);
        RoutingComparisonReport unhealthyReport = engine.compare(List.of(
                state("down-a", false, 10, 100.0, 100.0, 20.0, 40.0, 80.0, 0.01, 1),
                state("down-b", false, 20, 100.0, 100.0, 25.0, 50.0, 100.0, 0.02, 2)),
                DEFAULT_ORDER);

        assertNoHealthyCandidateReport(emptyReport, 0);
        assertNoHealthyCandidateReport(unhealthyReport, 2);
    }

    private static RoutingComparisonEngine comparisonEngineWithFixedStrategies() {
        return new RoutingComparisonEngine(new RoutingStrategyRegistry(List.of(
                new TailLatencyPowerOfTwoStrategy(new ServerScoreCalculator(), new Random(3), FIXED_CLOCK),
                new WeightedLeastLoadStrategy(FIXED_CLOCK),
                new WeightedLeastConnectionsRoutingStrategy(FIXED_CLOCK),
                new WeightedRoundRobinRoutingStrategy(FIXED_CLOCK),
                new RoundRobinRoutingStrategy(FIXED_CLOCK))), FIXED_CLOCK);
    }

    private static void assertNoHealthyCandidateReport(RoutingComparisonReport report, int candidateCount) {
        assertAll("safe no-healthy comparison report",
                () -> assertEquals(DEFAULT_ORDER, report.requestedStrategies()),
                () -> assertEquals(DEFAULT_ORDER, strategyIds(report)),
                () -> assertEquals(candidateCount, report.candidateCount()),
                () -> assertEquals(NOW, report.timestamp()),
                () -> assertTrue(report.results().stream().allMatch(RoutingComparisonResult::successful)));

        for (RoutingComparisonResult result : report.results()) {
            RoutingDecision decision = result.decision().orElseThrow();
            RoutingDecisionExplanation explanation = decision.explanation();

            assertAll("safe no-healthy decision for " + result.strategyId(),
                    () -> assertTrue(decision.chosenServer().isEmpty()),
                    () -> assertTrue(explanation.chosenServerId().isEmpty()),
                    () -> assertEquals(List.of(), explanation.candidateServersConsidered()),
                    () -> assertEquals(Map.of(), explanation.scores()),
                    () -> assertTrue(result.reason().contains("No healthy eligible servers")),
                    () -> assertEquals(result.reason(), explanation.reason()));
        }
    }

    private static List<RoutingStrategyId> strategyIds(RoutingComparisonReport report) {
        return report.results().stream()
                .map(RoutingComparisonResult::strategyId)
                .toList();
    }

    private static List<ServerStateVector> healthyCandidates() {
        return List.of(
                state("lower-risk", true, 5, 100.0, 100.0, 20.0, 40.0, 80.0, 0.01, 1),
                state("higher-risk", true, 75, 100.0, 100.0, 35.0, 120.0, 220.0, 0.15, 10));
    }

    private static ServerStateVector state(String id,
                                           boolean healthy,
                                           int inFlight,
                                           double configuredCapacity,
                                           double estimatedConcurrencyLimit,
                                           double averageLatencyMillis,
                                           double p95LatencyMillis,
                                           double p99LatencyMillis,
                                           double recentErrorRate,
                                           int queueDepth) {
        return new ServerStateVector(id, healthy, inFlight, configuredCapacity, estimatedConcurrencyLimit,
                averageLatencyMillis, p95LatencyMillis, p99LatencyMillis, recentErrorRate, queueDepth, NOW);
    }
}
