package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class ServerRecommendationScoreTest {
    private static final Instant BASE_TIME = Instant.parse("2026-07-16T16:00:00Z");
    private static final ServerObservationWindowPolicy OBSERVATION_POLICY = new ServerObservationWindowPolicy(
            16,
            Duration.ofSeconds(30),
            4,
            4,
            0.20,
            0.75,
            3,
            2,
            0.50);
    private static final ServerRecommendationScorePolicy SCORE_POLICY =
            new ServerRecommendationScorePolicy(100.0, 100.0, 100.0);

    private final ServerScoreCalculator calculator = new ServerScoreCalculator();

    @Test
    void producesTypedDeterministicBoundedFactorContributions() {
        ServerRollingSignalState signalState = successes("stable", 8, 50.0)
                .snapshot(BASE_TIME.plusSeconds(10));
        ServerStateVector state = vector(server("stable"), signalState, 0, 0);

        ServerScoreBreakdown first = calculator.recommendationScoreBreakdown(state, signalState, SCORE_POLICY);
        ServerScoreBreakdown second = calculator.recommendationScoreBreakdown(state, signalState, SCORE_POLICY);
        Map<String, ScoreFactorContribution> factors = first.factorContributions().stream()
                .collect(Collectors.toMap(ScoreFactorContribution::factorName, Function.identity()));

        assertEquals(first, second);
        assertEquals(17.5, first.totalScore(), 0.000001);
        assertEquals(first.totalScore(), first.exactContributionTotal(), 0.000001);
        assertEquals(200.0, calculator.maximumRecommendationScore(SCORE_POLICY));
        assertTrue(first.factorContributions().stream()
                .allMatch(ScoreFactorContribution::hasWeightedNormalizedValue));

        ScoreFactorContribution p99 = factors.get("boundedP99Latency");
        assertEquals(50.0, p99.rawValue().orElseThrow());
        assertEquals(0.5, p99.normalizedValue().orElseThrow());
        assertEquals(18.0, p99.weight().orElseThrow());
        assertEquals(9.0, p99.contributionValue().orElseThrow());
        assertEquals(ScoreFactorDirection.WEAKENS_SELECTION, p99.direction());
        assertEquals(ScoreFactorDirection.SUPPORTS_SELECTION, factors.get("confidencePenalty").direction());
        assertEquals(ScoreFactorDirection.SUPPORTS_SELECTION,
                factors.get("recommendationEligibilityPenalty").direction());
    }

    @Test
    void partialDegradationRemainsEligibleButScoresWorseThanStableState() {
        ServerRollingSignalState stable = successes("stable", 4, 25.0)
                .snapshot(BASE_TIME.plusSeconds(10));
        ServerRollingSignalState partial = ServerObservationWindow.create("partial", OBSERVATION_POLICY)
                .append(success("partial-1", "partial", 1, 25), BASE_TIME.plusSeconds(10))
                .append(timeout("partial-2", "partial", 2, 100), BASE_TIME.plusSeconds(10))
                .append(success("partial-3", "partial", 3, 25), BASE_TIME.plusSeconds(10))
                .append(success("partial-4", "partial", 4, 25), BASE_TIME.plusSeconds(10))
                .snapshot(BASE_TIME.plusSeconds(10));
        ServerStateVector stableVector = vector(server("stable"), stable, 0, 0);
        ServerStateVector partialVector = vector(server("partial"), partial, 0, 0);

        double stableScore = calculator.recommendationScore(stableVector, stable, SCORE_POLICY);
        ServerScoreBreakdown partialScore = calculator.recommendationScoreBreakdown(
                partialVector, partial, SCORE_POLICY);

        assertEquals(ServerDegradationState.PARTIALLY_DEGRADED, partial.degradationState());
        assertTrue(partial.recommendationEligible());
        assertTrue(partialVector.healthy());
        assertTrue(partialScore.totalScore() > stableScore);
        assertEquals(0.0, factor(partialScore, "recommendationEligibilityPenalty")
                .contributionValue().orElseThrow());
        assertTrue(factor(partialScore, "boundedRecentErrorRate").contributionValue().orElseThrow() > 0.0);
        assertTrue(factor(partialScore, "boundedTimeoutRate").contributionValue().orElseThrow() > 0.0);
        assertTrue(factor(partialScore, "degradationPenalty").contributionValue().orElseThrow() > 0.0);
    }

    @Test
    void missingStaleAndSparseEvidenceReceiveExplicitFailClosedPenalty() {
        ServerRollingSignalState missing = ServerObservationWindow.create("missing", OBSERVATION_POLICY)
                .snapshot(BASE_TIME);
        ServerRollingSignalState stale = successes("stale", 4, 25.0)
                .snapshot(BASE_TIME.plusSeconds(40));
        ServerRollingSignalState sparse = successes("sparse", 1, 50.0)
                .snapshot(BASE_TIME.plusSeconds(10));

        ServerScoreBreakdown missingScore = calculator.recommendationScoreBreakdown(
                vector(server("missing"), missing, 0, 0), missing, SCORE_POLICY);
        ServerScoreBreakdown staleScore = calculator.recommendationScoreBreakdown(
                vector(server("stale"), stale, 0, 0), stale, SCORE_POLICY);
        ServerScoreBreakdown sparseScore = calculator.recommendationScoreBreakdown(
                vector(server("sparse"), sparse, 0, 0), sparse, SCORE_POLICY);

        for (ServerScoreBreakdown breakdown : List.of(missingScore, staleScore, sparseScore)) {
            assertEquals(100.0, factor(breakdown, "recommendationEligibilityPenalty")
                    .contributionValue().orElseThrow());
            assertTrue(breakdown.totalScore() >= 100.0);
            assertTrue(breakdown.totalScore() <= calculator.maximumRecommendationScore(SCORE_POLICY));
        }
        assertEquals(115.0, missingScore.totalScore(), 0.000001);
        assertEquals(missingScore.totalScore(), staleScore.totalScore(), 0.000001);
        assertTrue(sparseScore.totalScore() > missingScore.totalScore());
    }

    @Test
    void clampsExtremeSignalsAndNeverExceedsConfiguredMaximum() {
        ServerObservationWindow failures = ServerObservationWindow.create("failed", OBSERVATION_POLICY);
        for (int index = 1; index <= 8; index++) {
            failures = failures.append(
                    timeout("timeout-" + index, "failed", index, 10_000),
                    BASE_TIME.plusSeconds(10));
        }
        ServerRollingSignalState signalState = failures.snapshot(BASE_TIME.plusSeconds(10));
        ServerStateVector state = vector(server("failed"), signalState, 10_000, 10_000);
        ServerScoreBreakdown breakdown = calculator.recommendationScoreBreakdown(state, signalState, SCORE_POLICY);

        assertFalse(state.healthy());
        assertTrue(breakdown.totalScore() <= calculator.maximumRecommendationScore(SCORE_POLICY));
        assertEquals(1.0, factor(breakdown, "boundedP99Latency").normalizedValue().orElseThrow());
        assertEquals(1.0, factor(breakdown, "boundedInFlightPressure").normalizedValue().orElseThrow());
        assertEquals(1.0, factor(breakdown, "boundedQueuePressure").normalizedValue().orElseThrow());
        assertEquals(1.0, factor(breakdown, "boundedRecentErrorRate").normalizedValue().orElseThrow());
        assertEquals(1.0, factor(breakdown, "boundedTimeoutRate").normalizedValue().orElseThrow());
    }

    @Test
    void rejectsMismatchedStateSignalAndMalformedStructuredContributions() {
        ServerRollingSignalState stable = successes("stable", 4, 25.0)
                .snapshot(BASE_TIME.plusSeconds(10));
        ServerRollingSignalState other = successes("other", 4, 25.0)
                .snapshot(BASE_TIME.plusSeconds(10));
        ServerStateVector stableVector = vector(server("stable"), stable, 0, 0);

        assertThrows(IllegalArgumentException.class,
                () -> calculator.recommendationScore(stableVector, other, SCORE_POLICY));
        assertThrows(IllegalArgumentException.class,
                () -> new ServerRecommendationScorePolicy(0.0, 100.0, 100.0));
        assertThrows(IllegalArgumentException.class, () -> new ScoreFactorContribution(
                "factor",
                "raw",
                "weight",
                ScoreFactorDirection.WEAKENS_SELECTION,
                "contribution",
                OptionalDouble.of(3.0),
                ScoreFactorExactness.EXACT_FROM_CALCULATOR,
                "explanation",
                "boundary",
                OptionalDouble.of(1.0),
                OptionalDouble.of(0.5),
                OptionalDouble.of(4.0)));
        assertThrows(IllegalArgumentException.class,
                () -> new ServerScoreBreakdown("server", 1.0, List.of()));
    }

    private static ScoreFactorContribution factor(ServerScoreBreakdown breakdown, String name) {
        return breakdown.factorContributions().stream()
                .filter(contribution -> contribution.factorName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static ServerObservationWindow successes(String serverId, int count, double latencyMillis) {
        ServerObservationWindow window = ServerObservationWindow.create(serverId, OBSERVATION_POLICY);
        for (int index = 1; index <= count; index++) {
            window = window.append(
                    success(serverId + "-" + index, serverId, index, latencyMillis),
                    BASE_TIME.plusSeconds(10));
        }
        return window;
    }

    private static ServerObservation success(
            String observationId,
            String serverId,
            int offsetSeconds,
            double latencyMillis) {
        return ServerObservation.success(
                observationId,
                serverId,
                ServerObservationSource.ENTERPRISE_LAB,
                latencyMillis,
                BASE_TIME.plusSeconds(offsetSeconds));
    }

    private static ServerObservation timeout(
            String observationId,
            String serverId,
            int offsetSeconds,
            double elapsedMillis) {
        return ServerObservation.timeout(
                observationId,
                serverId,
                ServerObservationSource.ENTERPRISE_LAB,
                elapsedMillis,
                BASE_TIME.plusSeconds(offsetSeconds));
    }

    private static ServerStateVector vector(
            Server server,
            ServerRollingSignalState signalState,
            int inFlight,
            int queueDepth) {
        return ServerStateVector.fromObservationState(
                server,
                signalState,
                inFlight,
                OptionalDouble.empty(),
                OptionalInt.of(queueDepth));
    }

    private static Server server(String serverId) {
        Server server = new Server(serverId, 10, 10, 10, ServerType.ONSITE);
        server.setCapacity(100.0);
        server.setHealthy(true);
        return server;
    }
}
