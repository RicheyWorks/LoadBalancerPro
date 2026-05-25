package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

class ServerStateVectorSignalExpansionTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final ServerScoreCalculator calculator = new ServerScoreCalculator();

    @Test
    void neutralVectorExposesStableZeroPressureSignals() {
        ServerStateVector neutral = state("neutral", true, 0, 100.0, 100.0,
                20.0, 20.0, 20.0, 0.0, 0, NetworkAwarenessSignal.neutral("neutral", NOW));

        assertEquals(100.0, neutral.capacityBasis(), 0.0);
        assertEquals(0.0, neutral.inFlightPressure(), 0.0);
        assertEquals(0.0, neutral.queuePressure(), 0.0);
        assertEquals(0.0, neutral.boundedInFlightPressure(), 0.0);
        assertEquals(0.0, neutral.boundedQueuePressure(), 0.0);
        assertEquals(0.0, neutral.tailLatencySpreadMillis(), 0.0);
        assertEquals(0.0, neutral.tailLatencyPressure(), 0.0);
        assertEquals(0.0, neutral.errorPressure(), 0.0);
        assertEquals(0.0, neutral.networkRiskPressure(), 0.0);
        assertEquals(0.0, neutral.normalizedHealthPressure(), 0.0);
        assertFalse(neutral.hasMaterialRisk());
    }

    @Test
    void capacityBasisPrefersEstimatedConcurrencyAndPreservesRawPressureForScoring() {
        ServerStateVector saturated = state("saturated", true, 150, 200.0, 100.0,
                30.0, 60.0, 90.0, 0.01, 40, NetworkAwarenessSignal.neutral("saturated", NOW));

        assertEquals(100.0, saturated.capacityBasis(), 0.0);
        assertEquals(1.5, saturated.inFlightPressure(), 0.000001);
        assertEquals(0.4, saturated.queuePressure(), 0.000001);
        assertEquals(1.0, saturated.boundedInFlightPressure(), 0.0);
        assertEquals(0.4, saturated.boundedQueuePressure(), 0.000001);
        assertTrue(calculator.factorContributions(saturated).stream()
                .anyMatch(contribution -> contribution.factorName().equals("inFlightRequestRatio")
                        && contribution.contributionValue().orElseThrow() == 150.0));
    }

    @Test
    void missingCapacityFallsBackToOneWithoutChangingCalculatorCompatibility() {
        ServerStateVector missingCapacity = new ServerStateVector(
                "missing-capacity",
                true,
                2,
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                7.0,
                10.0,
                20.0,
                0.0,
                OptionalInt.empty(),
                NetworkAwarenessSignal.neutral("missing-capacity", NOW),
                NOW);

        assertEquals(1.0, missingCapacity.capacityBasis(), 0.0);
        assertEquals(2.0, missingCapacity.inFlightPressure(), 0.0);
        assertEquals(1.0, missingCapacity.boundedInFlightPressure(), 0.0);
        assertEquals(0.0, missingCapacity.queuePressure(), 0.0);
        assertEquals(200.0, calculator.factorContributions(missingCapacity).stream()
                .filter(contribution -> contribution.factorName().equals("inFlightRequestRatio"))
                .findFirst()
                .orElseThrow()
                .contributionValue()
                .orElseThrow(), 0.000001);
    }

    @Test
    void tailLatencyPressureIncreasesAsTailSpreadWidens() {
        ServerStateVector lowerTailPressure = state("lower-tail", true, 10, 100.0, 100.0,
                50.0, 100.0, 110.0, 0.0, 0, NetworkAwarenessSignal.neutral("lower-tail", NOW));
        ServerStateVector higherTailPressure = state("higher-tail", true, 10, 100.0, 100.0,
                50.0, 100.0, 180.0, 0.0, 0, NetworkAwarenessSignal.neutral("higher-tail", NOW));

        assertEquals(10.0, lowerTailPressure.tailLatencySpreadMillis(), 0.0);
        assertEquals(80.0, higherTailPressure.tailLatencySpreadMillis(), 0.0);
        assertTrue(higherTailPressure.tailLatencyPressure() > lowerTailPressure.tailLatencyPressure());
        assertTrue(calculator.score(higherTailPressure) > calculator.score(lowerTailPressure));
    }

    @Test
    void errorAndNetworkRiskPressuresAreBoundedAndCanReturnToNeutral() {
        ServerStateVector neutral = state("network-neutral", true, 10, 100.0, 100.0,
                40.0, 80.0, 100.0, 0.0, 0, NetworkAwarenessSignal.neutral("network-neutral", NOW));
        ServerStateVector risky = state("network-risky", true, 10, 100.0, 100.0,
                40.0, 80.0, 100.0, 0.35, 0,
                new NetworkAwarenessSignal("network-risky", 0.20, 0.40, 0.10,
                        80.0, true, 5, 100, NOW));

        assertEquals(0.0, neutral.errorPressure(), 0.0);
        assertEquals(0.0, neutral.networkRiskPressure(), 0.0);
        assertEquals(0.35, risky.errorPressure(), 0.0);
        assertTrue(risky.networkRiskPressure() > neutral.networkRiskPressure());
        assertTrue(risky.networkRiskPressure() <= 1.0);
        assertTrue(risky.normalizedHealthPressure() <= 1.0);
        assertTrue(risky.hasMaterialRisk());
    }

    @Test
    void unhealthyVectorReportsMaxNormalizedHealthPressure() {
        ServerStateVector unhealthy = state("unhealthy", false, 0, 100.0, 100.0,
                20.0, 20.0, 20.0, 0.0, 0, NetworkAwarenessSignal.neutral("unhealthy", NOW));

        assertEquals(1.0, unhealthy.normalizedHealthPressure(), 0.0);
        assertTrue(unhealthy.hasMaterialRisk());
    }

    @Test
    void scoreCalculatorStillPreservesExistingRepresentativeScore() {
        ServerStateVector representative = new ServerStateVector(
                "representative-healthy",
                true,
                20,
                OptionalDouble.of(200.0),
                OptionalDouble.of(100.0),
                5.0,
                30.0,
                60.0,
                90.0,
                0.02,
                OptionalInt.of(5),
                new NetworkAwarenessSignal("representative", 0.10, 0.20, 0.05,
                        40.0, true, 3, 100, NOW),
                NOW);

        assertEquals(100.0, representative.capacityBasis(), 0.0);
        assertEquals(0.2, representative.inFlightPressure(), 0.000001);
        assertEquals(0.05, representative.queuePressure(), 0.000001);
        assertEquals(631.5, calculator.score(representative), 0.000001);
        assertEquals(calculator.score(representative),
                calculator.scoreBreakdown(representative).exactContributionTotal(), 0.000001);
    }

    private ServerStateVector state(String id,
                                    boolean healthy,
                                    int inFlight,
                                    double configuredCapacity,
                                    double estimatedConcurrencyLimit,
                                    double averageLatencyMillis,
                                    double p95LatencyMillis,
                                    double p99LatencyMillis,
                                    double recentErrorRate,
                                    int queueDepth,
                                    NetworkAwarenessSignal networkAwarenessSignal) {
        return new ServerStateVector(id, healthy, inFlight, configuredCapacity, estimatedConcurrencyLimit,
                averageLatencyMillis, p95LatencyMillis, p99LatencyMillis, recentErrorRate, queueDepth,
                networkAwarenessSignal, NOW);
    }
}
