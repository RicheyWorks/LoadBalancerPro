package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;

class ServerScoreCalculatorFactorContributionTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final ServerScoreCalculator calculator = new ServerScoreCalculator();

    @Test
    void factorContributionsSumToExistingScoreWithoutChangingCalculatorSemantics() {
        ServerStateVector state = representativeState(true);

        List<ScoreFactorContribution> contributions = calculator.factorContributions(state);
        double contributionTotal = contributions.stream()
                .filter(ScoreFactorContribution::hasExactContributionValue)
                .mapToDouble(contribution -> contribution.contributionValue().orElseThrow())
                .sum();

        assertEquals(631.5, calculator.score(state), 0.000001);
        assertEquals(calculator.score(state), contributionTotal, 0.000001);
        assertEquals(calculator.factorContributions(state), contributions,
                "Contribution output should be deterministic for the same state vector");
    }

    @Test
    void factorContributionsExposeEachCurrentCalculatorComponent() {
        Map<String, ScoreFactorContribution> contributions = calculator.factorContributions(representativeState(true))
                .stream()
                .collect(Collectors.toMap(ScoreFactorContribution::factorName, Function.identity()));

        assertEquals(16, contributions.size());
        assertContribution(contributions.get("p95LatencyMillis"), 27.0, ScoreFactorDirection.WEAKENS_SELECTION);
        assertContribution(contributions.get("p99LatencyMillis"), 31.5, ScoreFactorDirection.WEAKENS_SELECTION);
        assertContribution(contributions.get("averageLatencyMillis"), 3.0, ScoreFactorDirection.WEAKENS_SELECTION);
        assertContribution(contributions.get("capacityBasis"), 0.0, ScoreFactorDirection.NEUTRAL);
        assertContribution(contributions.get("inFlightRequestRatio"), 20.0, ScoreFactorDirection.WEAKENS_SELECTION);
        assertContribution(contributions.get("queueDepthRatio"), 5.0, ScoreFactorDirection.WEAKENS_SELECTION);
        assertContribution(contributions.get("recentErrorRate"), 20.0, ScoreFactorDirection.WEAKENS_SELECTION);
        assertContribution(contributions.get("timeoutRate"), 80.0, ScoreFactorDirection.WEAKENS_SELECTION);
        assertContribution(contributions.get("retryRate"), 70.0, ScoreFactorDirection.WEAKENS_SELECTION);
        assertContribution(contributions.get("connectionFailureRate"), 45.0, ScoreFactorDirection.WEAKENS_SELECTION);
        assertContribution(contributions.get("latencyJitterMillis"), 20.0, ScoreFactorDirection.WEAKENS_SELECTION);
        assertContribution(contributions.get("recentErrorBurst"), 250.0, ScoreFactorDirection.WEAKENS_SELECTION);
        assertContribution(contributions.get("requestTimeoutCount"), 60.0, ScoreFactorDirection.WEAKENS_SELECTION);
        assertContribution(contributions.get("healthPenalty"), 0.0, ScoreFactorDirection.SUPPORTS_SELECTION);
        assertContribution(contributions.get("serverWeightNotApplied"), 0.0, ScoreFactorDirection.NEUTRAL);

        ScoreFactorContribution hidden = contributions.get("hiddenRoutingInternals");
        assertEquals(ScoreFactorDirection.UNKNOWN, hidden.direction());
        assertEquals(ScoreFactorExactness.NOT_EXPOSED, hidden.exactness());
        assertTrue(hidden.contributionValue().isEmpty());
        assertTrue(hidden.explanationText().contains("not inferred"));
    }

    @Test
    void unhealthyPenaltyIsExplicitAndPreservesExistingScoreDelta() {
        ServerStateVector healthy = representativeState(true);
        ServerStateVector unhealthy = representativeState(false);
        Map<String, ScoreFactorContribution> unhealthyContributions = calculator.factorContributions(unhealthy)
                .stream()
                .collect(Collectors.toMap(ScoreFactorContribution::factorName, Function.identity()));

        assertEquals(calculator.score(healthy) + 1_000_000.0, calculator.score(unhealthy), 0.000001);
        assertContribution(unhealthyContributions.get("healthPenalty"),
                1_000_000.0, ScoreFactorDirection.WEAKENS_SELECTION);
        assertTrue(unhealthyContributions.get("healthPenalty").boundaryNote()
                .contains("not production certification"));
    }

    @Test
    void missingCapacityFallsBackToOneAndStaysExplicit() {
        ServerStateVector state = new ServerStateVector(
                "missing-capacity",
                true,
                2,
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                7.0,
                10.0,
                20.0,
                30.0,
                0.0,
                OptionalInt.empty(),
                NetworkAwarenessSignal.neutral("missing-capacity", NOW),
                NOW);

        Map<String, ScoreFactorContribution> contributions = calculator.factorContributions(state)
                .stream()
                .collect(Collectors.toMap(ScoreFactorContribution::factorName, Function.identity()));

        assertTrue(contributions.get("capacityBasis").rawValueDescription()
                .contains("configuredCapacity=not exposed"));
        assertContribution(contributions.get("inFlightRequestRatio"), 200.0, ScoreFactorDirection.WEAKENS_SELECTION);
        assertContribution(contributions.get("queueDepthRatio"), 0.0, ScoreFactorDirection.NEUTRAL);
        assertContribution(contributions.get("serverWeightNotApplied"), 0.0, ScoreFactorDirection.NEUTRAL);
    }

    @Test
    void networkRiskContributionsSumToExistingNetworkRiskScore() {
        NetworkAwarenessSignal signal = networkSignal();

        double contributionTotal = calculator.networkRiskFactorContributions(signal).stream()
                .filter(ScoreFactorContribution::hasExactContributionValue)
                .mapToDouble(contribution -> contribution.contributionValue().orElseThrow())
                .sum();

        assertEquals(calculator.networkRiskScore(signal), contributionTotal, 0.000001);
        assertEquals(525.0, contributionTotal, 0.000001);
        assertTrue(calculator.networkRiskFactorContributions(NetworkAwarenessSignal.neutral("neutral", NOW))
                .stream()
                .allMatch(contribution -> contribution.direction() == ScoreFactorDirection.NEUTRAL));
    }

    @Test
    void contributionTextIsBoundedToLocalCalculatorExplainability() {
        String normalized = calculator.factorContributions(representativeState(true)).stream()
                .map(contribution -> String.join(" ",
                        contribution.factorName(),
                        contribution.rawValueDescription(),
                        contribution.weightDescription(),
                        contribution.contributionDescription(),
                        contribution.explanationText(),
                        contribution.boundaryNote()))
                .collect(Collectors.joining(" "))
                .toLowerCase();

        assertTrue(normalized.contains("not production telemetry proof"));
        assertTrue(normalized.contains("exact production scoring is not claimed"));
        assertTrue(normalized.contains("hidden routing internals are not inferred"));
        assertTrue(normalized.contains("does not add a weight term"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("exact production scoring proof"));
        assertFalse(normalized.contains("completed replay"));
        assertFalse(normalized.contains("completed what-if"));
    }

    private ServerStateVector representativeState(boolean healthy) {
        return new ServerStateVector(
                healthy ? "representative-healthy" : "representative-unhealthy",
                healthy,
                20,
                OptionalDouble.of(200.0),
                OptionalDouble.of(100.0),
                5.0,
                30.0,
                60.0,
                90.0,
                0.02,
                OptionalInt.of(5),
                networkSignal(),
                NOW);
    }

    private NetworkAwarenessSignal networkSignal() {
        return new NetworkAwarenessSignal("representative", 0.10, 0.20, 0.05, 40.0, true, 3, 100, NOW);
    }

    private void assertContribution(ScoreFactorContribution contribution,
                                    double contributionValue,
                                    ScoreFactorDirection direction) {
        assertEquals(ScoreFactorExactness.EXACT_FROM_CALCULATOR, contribution.exactness());
        assertEquals(contributionValue, contribution.contributionValue().orElseThrow(), 0.000001);
        assertEquals(direction, contribution.direction());
    }
}
