package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

class TrafficAllocationRecommendationTest {
    private static final Instant NOW = Instant.parse("2026-07-16T17:00:00Z");

    @Test
    void convertsLowerScoresToHigherExactlyNormalizedShares() {
        TrafficAllocationRecommendation recommendation = LoadDistributionPlanner.recommendTrafficShares(
                List.of(state("slow", true), state("fast", true)),
                Map.of("slow", score("slow", 1.0), "fast", score("fast", 0.0)),
                Map.of(),
                new TrafficAllocationPolicy(0.0, 1.0, 1.0));

        assertEquals(2.0 / 3.0, recommendation.allocations().get("fast"), 0.000000001);
        assertEquals(1.0 / 3.0, recommendation.allocations().get("slow"), 0.000000001);
        assertEquals(1.0, total(recommendation.allocations()), 0.000000001);
        assertEquals(recommendation.rawAllocations(), recommendation.allocations());
        assertEquals(0.0, recommendation.unallocatedShare());
        assertFalse(recommendation.fallbackApplied());
        assertFalse(recommendation.rateLimited());
    }

    @Test
    void isDeterministicAcrossCandidateAndMapInsertionOrder() {
        Map<String, ServerScoreBreakdown> firstScores = new HashMap<>();
        firstScores.put("a", score("a", 0.0));
        firstScores.put("b", score("b", 4.0));
        Map<String, ServerScoreBreakdown> secondScores = new HashMap<>();
        secondScores.put("b", score("b", 4.0));
        secondScores.put("a", score("a", 0.0));
        TrafficAllocationPolicy policy = new TrafficAllocationPolicy(0.0, 1.0, 1.0);

        TrafficAllocationRecommendation first = LoadDistributionPlanner.recommendTrafficShares(
                List.of(state("a", true), state("b", true)), firstScores, Map.of(), policy);
        TrafficAllocationRecommendation second = LoadDistributionPlanner.recommendTrafficShares(
                List.of(state("b", true), state("a", true)), secondScores, Map.of(), policy);

        assertEquals(first, second);
        assertEquals(List.of("a", "b"), first.allocations().keySet().stream().toList());
    }

    @Test
    void projectsRawSharesOntoPerBackendMinimumAndMaximumBounds() {
        TrafficAllocationRecommendation recommendation = LoadDistributionPlanner.recommendTrafficShares(
                List.of(state("a", true), state("b", true)),
                Map.of("a", score("a", 0.0), "b", score("b", 9.0)),
                Map.of(),
                new TrafficAllocationPolicy(0.30, 0.70, 1.0));

        assertEquals(0.70, recommendation.allocations().get("a"), 0.000000001);
        assertEquals(0.30, recommendation.allocations().get("b"), 0.000000001);
        assertEquals(1.0, total(recommendation.allocations()), 0.000000001);
        assertFalse(recommendation.rateLimited());
        assertTrue(recommendation.reasons().contains("per-backend minimum and maximum shares applied"));
    }

    @Test
    void intersectsBoundsWithPerDecisionRateLimits() {
        TrafficAllocationRecommendation recommendation = LoadDistributionPlanner.recommendTrafficShares(
                List.of(state("a", true), state("b", true)),
                Map.of("a", score("a", 0.0), "b", score("b", 9.0)),
                Map.of("a", 0.50, "b", 0.50),
                new TrafficAllocationPolicy(0.0, 1.0, 0.10));

        assertEquals(0.60, recommendation.allocations().get("a"), 0.000000001);
        assertEquals(0.40, recommendation.allocations().get("b"), 0.000000001);
        assertTrue(recommendation.rateLimited());
        assertTrue(recommendation.reasons().contains(
                "per-decision share-change limit constrained the recommendation"));
    }

    @Test
    void excludesIneligibleAndUnscoredBackendsBeforeNormalization() {
        TrafficAllocationRecommendation recommendation = LoadDistributionPlanner.recommendTrafficShares(
                List.of(state("healthy", true), state("unhealthy", false), state("unscored", true)),
                Map.of(
                        "healthy", score("healthy", 10.0),
                        "unhealthy", score("unhealthy", 0.0)),
                Map.of(),
                TrafficAllocationPolicy.localLabDefaults());

        assertEquals(Map.of("healthy", 1.0), recommendation.allocations());
        assertTrue(recommendation.reasons().contains("excluded ineligible backend unhealthy"));
        assertTrue(recommendation.reasons().contains("excluded backend without score unscored"));
    }

    @Test
    void returnsExplicitNoAllocationFallbackForInfeasibleConstraints() {
        TrafficAllocationRecommendation recommendation = LoadDistributionPlanner.recommendTrafficShares(
                List.of(state("a", true), state("b", true)),
                Map.of("a", score("a", 0.0), "b", score("b", 1.0)),
                Map.of(),
                new TrafficAllocationPolicy(0.0, 0.40, 1.0));

        assertTrue(recommendation.fallbackApplied());
        assertTrue(recommendation.allocations().isEmpty());
        assertEquals(1.0, recommendation.unallocatedShare());
        assertEquals(1.0, total(recommendation.rawAllocations()), 0.000000001);
        assertTrue(recommendation.reasons().stream().anyMatch(reason -> reason.contains("infeasible")));
    }

    @Test
    void failsClosedWhenPreviousTrafficTargetsANowIneligibleBackend() {
        TrafficAllocationRecommendation recommendation = LoadDistributionPlanner.recommendTrafficShares(
                List.of(state("a", true), state("b", false)),
                Map.of("a", score("a", 0.0), "b", score("b", 1.0)),
                Map.of("a", 0.50, "b", 0.50),
                new TrafficAllocationPolicy(0.0, 1.0, 0.25));

        assertTrue(recommendation.fallbackApplied());
        assertEquals(1.0, recommendation.unallocatedShare());
        assertTrue(recommendation.reasons().stream().anyMatch(reason -> reason.contains("redistribution is unsafe")));
    }

    @Test
    void rejectsMalformedInputsAndKeepsResultsImmutable() {
        TrafficAllocationPolicy policy = TrafficAllocationPolicy.localLabDefaults();
        assertThrows(IllegalArgumentException.class,
                () -> new TrafficAllocationPolicy(0.6, 0.5, 0.1));
        assertThrows(IllegalArgumentException.class, () -> LoadDistributionPlanner.recommendTrafficShares(
                List.of(state("a", true), state("a", true)),
                Map.of("a", score("a", 0.0)),
                Map.of(),
                policy));
        assertThrows(IllegalArgumentException.class, () -> LoadDistributionPlanner.recommendTrafficShares(
                List.of(state("a", true)),
                Map.of("a", score("a", 0.0)),
                Map.of("a", 0.75),
                policy));
        assertThrows(IllegalArgumentException.class, () -> LoadDistributionPlanner.recommendTrafficShares(
                List.of(state("a", true)),
                Map.of("a", score("a", 0.0)),
                Map.of(" a ", 1.0),
                policy));

        TrafficAllocationRecommendation recommendation = LoadDistributionPlanner.recommendTrafficShares(
                List.of(state("a", true)),
                Map.of("a", score("a", 0.0)),
                Map.of(),
                policy);
        assertThrows(UnsupportedOperationException.class, () -> recommendation.allocations().clear());
        assertThrows(UnsupportedOperationException.class, () -> recommendation.reasons().clear());
    }

    private static ServerStateVector state(String serverId, boolean healthy) {
        return new ServerStateVector(
                serverId,
                healthy,
                0,
                OptionalDouble.of(100.0),
                OptionalDouble.empty(),
                1.0,
                0.0,
                0.0,
                0.0,
                0.0,
                OptionalInt.empty(),
                NOW);
    }

    private static ServerScoreBreakdown score(String serverId, double value) {
        ScoreFactorContribution contribution = new ScoreFactorContribution(
                "testScore",
                "raw score=" + value,
                "test weight",
                value > 0.0 ? ScoreFactorDirection.WEAKENS_SELECTION : ScoreFactorDirection.NEUTRAL,
                "test contribution",
                OptionalDouble.of(value),
                ScoreFactorExactness.EXACT_FROM_CALCULATOR,
                "test score contribution",
                "test-only local value");
        return new ServerScoreBreakdown(serverId, value, List.of(contribution));
    }

    private static double total(Map<String, Double> shares) {
        return shares.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}
