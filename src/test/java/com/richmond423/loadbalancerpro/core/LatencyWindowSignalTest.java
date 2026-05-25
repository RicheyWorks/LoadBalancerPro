package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class LatencyWindowSignalTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final ServerScoreCalculator calculator = new ServerScoreCalculator();

    @Test
    void emptySignalPreservesFallbackLatencyValues() {
        LatencyWindowSignal empty = LatencyWindowSignal.empty();

        assertFalse(empty.hasLatencyWindowValues());
        assertEquals(40.0, empty.effectiveAverageLatencyMillis(40.0), 0.0);
        assertEquals(80.0, empty.effectiveP95LatencyMillis(80.0), 0.0);
        assertEquals(120.0, empty.effectiveP99LatencyMillis(120.0), 0.0);
        assertEquals(40.0, empty.effectiveTailLatencySpreadMillis(80.0, 120.0), 0.0);
        assertEquals(0.5, empty.effectiveTailLatencyPressure(80.0, 120.0), 0.0);
    }

    @Test
    void samplesProduceBoundedEwmaAndRollingTailSignals() {
        LatencyWindowSignal signal = LatencyWindowSignal.fromSamples(List.of(10.0, 20.0, 30.0, 100.0), 0.5, 3);

        assertEquals(3, signal.sampleCount());
        assertTrue(signal.hasLatencyWindowValues());
        assertTrue(signal.hasTailLatencyWindowValues());
        assertEquals(62.5, signal.ewmaLatencyMillis().orElseThrow(), 0.000001);
        assertEquals(50.0, signal.rollingAverageLatencyMillis().orElseThrow(), 0.000001);
        assertEquals(100.0, signal.rollingP95LatencyMillis().orElseThrow(), 0.000001);
        assertEquals(100.0, signal.rollingP99LatencyMillis().orElseThrow(), 0.000001);
    }

    @Test
    void invalidSignalsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> LatencyWindowSignal.fromSamples(List.of(-1.0), 0.5));
        assertThrows(IllegalArgumentException.class, () -> LatencyWindowSignal.fromSamples(List.of(1.0), 0.0));
        assertThrows(IllegalArgumentException.class, () -> LatencyWindowSignal.fromSamples(List.of(1.0), 1.5));
        assertThrows(IllegalArgumentException.class, () -> LatencyWindowSignal.fromSamples(List.of(1.0), 0.5, 0));
        assertThrows(IllegalArgumentException.class, () -> new LatencyWindowSignal(
                0,
                OptionalDouble.of(10.0),
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                OptionalDouble.empty()));
    }

    @Test
    void stateVectorUsesWindowedLatencyWhenPresentWithoutChangingNeutralCompatibility() {
        ServerStateVector currentOnly = stateWithWindow("current-only", LatencyWindowSignal.empty());
        ServerStateVector withWindow = stateWithWindow("with-window",
                LatencyWindowSignal.fromSamples(List.of(90.0, 100.0, 110.0, 180.0), 0.5));

        assertEquals(50.0, currentOnly.effectiveAverageLatencyMillis(), 0.0);
        assertEquals(100.0, currentOnly.effectiveP95LatencyMillis(), 0.0);
        assertEquals(120.0, currentOnly.effectiveP99LatencyMillis(), 0.0);
        assertEquals(120.0, withWindow.effectiveAverageLatencyMillis(), 0.000001);
        assertEquals(180.0, withWindow.effectiveP95LatencyMillis(), 0.000001);
        assertEquals(180.0, withWindow.effectiveP99LatencyMillis(), 0.000001);
        assertTrue(calculator.score(withWindow) > calculator.score(currentOnly));
    }

    @Test
    void scoreBreakdownExposesLatencyWindowSignalWithoutStandalonePenalty() {
        ServerStateVector withWindow = stateWithWindow("with-window",
                LatencyWindowSignal.fromSamples(List.of(90.0, 100.0, 110.0, 180.0), 0.5));
        Map<String, ScoreFactorContribution> contributions = calculator.factorContributions(withWindow)
                .stream()
                .collect(Collectors.toMap(ScoreFactorContribution::factorName, Function.identity()));

        ScoreFactorContribution latencyWindow = contributions.get("latencyWindowSignal");
        assertEquals(ScoreFactorDirection.NEUTRAL, latencyWindow.direction());
        assertEquals(0.0, latencyWindow.contributionValue().orElseThrow(), 0.0);
        assertTrue(latencyWindow.rawValueDescription().contains("sampleCount=4"));
        assertTrue(latencyWindow.boundaryNote().contains("not p95/p99 production proof"));
        assertEquals(81.0, contributions.get("p95LatencyMillis").contributionValue().orElseThrow(), 0.000001);
        assertTrue(contributions.get("p95LatencyMillis").rawValueDescription()
                .contains("latencyWindowSamples=4"));
    }

    private ServerStateVector stateWithWindow(String serverId, LatencyWindowSignal latencyWindowSignal) {
        return new ServerStateVector(
                serverId,
                true,
                10,
                OptionalDouble.of(100.0),
                OptionalDouble.of(100.0),
                1.0,
                50.0,
                100.0,
                120.0,
                0.0,
                OptionalInt.of(0),
                NetworkAwarenessSignal.neutral(serverId, NOW),
                latencyWindowSignal,
                NOW);
    }
}
