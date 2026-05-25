package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;

import org.junit.jupiter.api.Test;

class TailLatencyPowerOfTwoHysteresisTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ServerScoreCalculator calculator = new ServerScoreCalculator();

    @Test
    void nearTieUsesStableServerIdWithinAntiFlappingBand() {
        ServerStateVector stableId = state("alpha", 50.0, 100.0, 150.0);
        ServerStateVector slightlyLowerScore = state("beta", 50.0, 99.0, 149.0);

        assertTrue(calculator.score(slightlyLowerScore) < calculator.score(stableId));

        RoutingDecision decision = strategy().choose(List.of(slightlyLowerScore, stableId));

        assertEquals("alpha", decision.chosenServer().orElseThrow().serverId());
        assertTrue(decision.explanation().reason().contains("anti-flapping band"));
        assertTrue(decision.explanation().reason().contains("scoreDelta"));
        assertTrue(decision.explanation().reason().contains("effectiveTailLatencyPressureDelta"));
    }

    @Test
    void materialTailLatencyDifferenceStillSelectsLowerScore() {
        ServerStateVector stableButWorseTail = state("alpha", 50.0, 120.0, 200.0);
        ServerStateVector materiallyBetter = state("beta", 50.0, 100.0, 150.0);

        assertTrue(calculator.score(materiallyBetter) < calculator.score(stableButWorseTail));

        RoutingDecision decision = strategy().choose(List.of(stableButWorseTail, materiallyBetter));

        assertEquals("beta", decision.chosenServer().orElseThrow().serverId());
        assertFalse(decision.explanation().reason().contains("anti-flapping band"));
    }

    @Test
    void hysteresisUsesLatencyWindowSignalInputs() {
        ServerStateVector stableId = stateWithWindow("alpha", 100.0, 100.0, 150.0);
        ServerStateVector slightlyLowerScore = stateWithWindow("beta", 99.0, 99.0, 149.0);

        assertEquals(100.0, stableId.effectiveP95LatencyMillis(), 0.0);
        assertEquals(149.0, slightlyLowerScore.effectiveP99LatencyMillis(), 0.0);
        assertTrue(calculator.score(slightlyLowerScore) < calculator.score(stableId));

        RoutingDecision decision = strategy().choose(List.of(slightlyLowerScore, stableId));

        assertEquals("alpha", decision.chosenServer().orElseThrow().serverId());
        assertTrue(decision.explanation().reason().contains("anti-flapping band"));
    }

    @Test
    void neutralDefaultTieRemainsDeterministic() {
        ServerStateVector alpha = state("alpha", 50.0, 100.0, 150.0);
        ServerStateVector beta = state("beta", 50.0, 100.0, 150.0);

        RoutingDecision first = strategy().choose(List.of(beta, alpha));
        RoutingDecision second = strategy().choose(List.of(beta, alpha));

        assertEquals("alpha", first.chosenServer().orElseThrow().serverId());
        assertEquals(first.chosenServer().orElseThrow().serverId(),
                second.chosenServer().orElseThrow().serverId());
    }

    private TailLatencyPowerOfTwoStrategy strategy() {
        return new TailLatencyPowerOfTwoStrategy(calculator, new Random(1), FIXED_CLOCK);
    }

    private ServerStateVector state(String id,
                                    double averageLatencyMillis,
                                    double p95LatencyMillis,
                                    double p99LatencyMillis) {
        return new ServerStateVector(id, true, 0, 100.0, 100.0, averageLatencyMillis,
                p95LatencyMillis, p99LatencyMillis, 0.0, 0, NOW);
    }

    private ServerStateVector stateWithWindow(String id,
                                              double rollingAverageLatencyMillis,
                                              double rollingP95LatencyMillis,
                                              double rollingP99LatencyMillis) {
        return new ServerStateVector(
                id,
                true,
                0,
                OptionalDouble.of(100.0),
                OptionalDouble.of(100.0),
                1.0,
                500.0,
                500.0,
                600.0,
                0.0,
                OptionalInt.of(0),
                NetworkAwarenessSignal.neutral(id, NOW),
                new LatencyWindowSignal(
                        4,
                        OptionalDouble.of(rollingAverageLatencyMillis),
                        OptionalDouble.of(rollingAverageLatencyMillis),
                        OptionalDouble.of(rollingP95LatencyMillis),
                        OptionalDouble.of(rollingP99LatencyMillis)),
                NOW);
    }
}
