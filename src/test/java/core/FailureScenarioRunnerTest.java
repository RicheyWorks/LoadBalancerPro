package test.core;

import core.FailureScenarioConfig;
import core.FailureScenarioResult;
import core.FailureScenarioRunner;
import core.FailureScenarioSignal;
import core.FailureScenarioType;
import core.FailureSeverity;
import core.MitigationAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FailureScenarioRunnerTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final FailureScenarioConfig CONFIG =
            new FailureScenarioConfig(20, 200.0, 350.0, 0.10, 0.85, 0.60, 10);

    private final FailureScenarioRunner runner = new FailureScenarioRunner();

    @Test
    void lowSampleSizeReturnsLowSeverityHold() {
        FailureScenarioResult result = runner.evaluate(
                signal("low-sample", FailureScenarioType.ERROR_STORM, "checkout", 4, 4,
                        30, 40, 30, 500.0, 800.0, 0.50, 9), CONFIG);

        assertEquals(FailureSeverity.LOW, result.severity());
        assertEquals(List.of(MitigationAction.HOLD), result.recommendations());
        assertTrue(result.reason().contains("insufficient sample"));
    }

    @Test
    void normalLowPressureReturnsLowSeverityHold() {
        FailureScenarioResult result = runner.evaluate(normalSignal(FailureScenarioType.TRAFFIC_SPIKE), CONFIG);

        assertEquals(FailureSeverity.LOW, result.severity());
        assertEquals(List.of(MitigationAction.HOLD), result.recommendations());
        assertTrue(result.reason().contains("low pressure"));
    }

    @Test
    void trafficSpikeRecommendsScaleUpShadow() {
        FailureScenarioResult result = runner.evaluate(
                signal("spike", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, 4,
                        36, 40, 5, 120.0, 180.0, 0.01, 100), CONFIG);

        assertEquals(FailureSeverity.HIGH, result.severity());
        assertTrue(result.recommendations().contains(MitigationAction.SCALE_UP_SHADOW));
        assertFalse(result.recommendations().contains(MitigationAction.SHED_LOW_PRIORITY));
        assertTrue(result.reason().contains("utilization"));
    }

    @Test
    void trafficSpikeWithQueuePressureAlsoShedsLowPriority() {
        FailureScenarioResult result = runner.evaluate(
                signal("spike-queue", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, 4,
                        36, 40, 25, 120.0, 180.0, 0.01, 100), CONFIG);

        assertEquals(FailureSeverity.HIGH, result.severity());
        assertTrue(result.recommendations().contains(MitigationAction.SCALE_UP_SHADOW));
        assertTrue(result.recommendations().contains(MitigationAction.SHED_LOW_PRIORITY));
        assertTrue(result.reason().contains("queue depth"));
    }

    @Test
    void slowServerRecommendsReduceConcurrencyAndRouteAround() {
        FailureScenarioResult result = runner.evaluate(
                signal("slow-server", FailureScenarioType.SLOW_SERVER, "api-1", 4, 4,
                        8, 40, 2, 260.0, 420.0, 0.01, 100), CONFIG);

        assertEquals(FailureSeverity.HIGH, result.severity());
        assertEquals(List.of(MitigationAction.REDUCE_CONCURRENCY, MitigationAction.ROUTE_AROUND),
                result.recommendations());
        assertTrue(result.reason().contains("p95 latency"));
        assertTrue(result.reason().contains("p99 latency"));
    }

    @Test
    void queueBacklogRecommendsShedLowPriority() {
        FailureScenarioResult result = runner.evaluate(
                signal("queue-backlog", FailureScenarioType.QUEUE_BACKLOG, "checkout", 4, 4,
                        20, 40, 40, 120.0, 180.0, 0.01, 100), CONFIG);

        assertEquals(FailureSeverity.HIGH, result.severity());
        assertTrue(result.recommendations().contains(MitigationAction.SHED_LOW_PRIORITY));
        assertTrue(result.recommendations().contains(MitigationAction.SCALE_UP_SHADOW));
    }

    @Test
    void errorStormRecommendsInvestigate() {
        FailureScenarioResult result = runner.evaluate(
                signal("error-storm", FailureScenarioType.ERROR_STORM, "checkout", 4, 4,
                        8, 40, 2, 120.0, 180.0, 0.20, 100), CONFIG);

        assertEquals(FailureSeverity.HIGH, result.severity());
        assertEquals(List.of(MitigationAction.INVESTIGATE), result.recommendations());
        assertTrue(result.reason().contains("error rate"));
    }

    @Test
    void severeErrorStormCanRecommendFailClosed() {
        FailureScenarioResult result = runner.evaluate(
                signal("severe-error-storm", FailureScenarioType.ERROR_STORM, "checkout", 4, 4,
                        8, 40, 2, 120.0, 180.0, 0.55, 100), CONFIG);

        assertEquals(FailureSeverity.CRITICAL, result.severity());
        assertEquals(List.of(MitigationAction.INVESTIGATE, MitigationAction.FAIL_CLOSED),
                result.recommendations());
        assertTrue(result.reason().contains("severe error rate"));
    }

    @Test
    void flappingServerRecommendsRouteAroundAndInvestigate() {
        FailureScenarioResult result = runner.evaluate(
                signal("flapping", FailureScenarioType.FLAPPING_SERVER, "api-1", 5, 2,
                        8, 40, 2, 120.0, 180.0, 0.12, 100), CONFIG);

        assertEquals(FailureSeverity.HIGH, result.severity());
        assertEquals(List.of(MitigationAction.ROUTE_AROUND, MitigationAction.INVESTIGATE),
                result.recommendations());
        assertTrue(result.reason().contains("healthy ratio"));
        assertTrue(result.reason().contains("error rate"));
    }

    @Test
    void partialOutageRecommendsCriticalSeverity() {
        FailureScenarioResult result = runner.evaluate(
                signal("partial-outage", FailureScenarioType.PARTIAL_OUTAGE, "checkout", 10, 4,
                        10, 40, 8, 120.0, 180.0, 0.03, 100), CONFIG);

        assertEquals(FailureSeverity.CRITICAL, result.severity());
        assertEquals(List.of(MitigationAction.ROUTE_AROUND, MitigationAction.SHED_LOW_PRIORITY,
                MitigationAction.INVESTIGATE), result.recommendations());
        assertTrue(result.reason().contains("healthy ratio"));
    }

    @Test
    void capacitySaturationRecommendsScaleUpShadow() {
        FailureScenarioResult result = runner.evaluate(
                signal("saturation", FailureScenarioType.CAPACITY_SATURATION, "checkout", 4, 4,
                        38, 40, 5, 120.0, 180.0, 0.01, 100), CONFIG);

        assertEquals(FailureSeverity.HIGH, result.severity());
        assertEquals(List.of(MitigationAction.SCALE_UP_SHADOW, MitigationAction.REDUCE_CONCURRENCY),
                result.recommendations());
        assertTrue(result.reason().contains("utilization"));
    }

    @Test
    void healthyRatioAndUtilizationCalculationsAreCorrect() {
        FailureScenarioSignal signal = signal("math", FailureScenarioType.PARTIAL_OUTAGE, "checkout", 5, 3,
                30, 40, 6, 120.0, 180.0, 0.01, 100);

        assertEquals(0.60, signal.healthyRatio());
        assertEquals(0.75, signal.utilization());

        FailureScenarioResult result = runner.evaluate(signal, CONFIG);
        assertEquals(0.60, result.healthyRatio());
        assertEquals(0.75, result.utilization());
    }

    @Test
    void invalidConfigIsRejected() {
        assertAll("invalid config",
                () -> assertInvalid(() -> new FailureScenarioConfig(-1, 200.0, 350.0, 0.10, 0.85, 0.60, 10)),
                () -> assertInvalid(() -> new FailureScenarioConfig(20, 0.0, 350.0, 0.10, 0.85, 0.60, 10)),
                () -> assertInvalid(() -> new FailureScenarioConfig(20, Double.NaN, 350.0, 0.10, 0.85, 0.60, 10)),
                () -> assertInvalid(() -> new FailureScenarioConfig(20, 200.0, Double.POSITIVE_INFINITY, 0.10, 0.85, 0.60, 10)),
                () -> assertInvalid(() -> new FailureScenarioConfig(20, 200.0, 350.0, -0.01, 0.85, 0.60, 10)),
                () -> assertInvalid(() -> new FailureScenarioConfig(20, 200.0, 350.0, 1.01, 0.85, 0.60, 10)),
                () -> assertInvalid(() -> new FailureScenarioConfig(20, 200.0, 350.0, Double.NaN, 0.85, 0.60, 10)),
                () -> assertInvalid(() -> new FailureScenarioConfig(20, 200.0, 350.0, 0.10, -0.01, 0.60, 10)),
                () -> assertInvalid(() -> new FailureScenarioConfig(20, 200.0, 350.0, 0.10, 1.01, 0.60, 10)),
                () -> assertInvalid(() -> new FailureScenarioConfig(20, 200.0, 350.0, 0.10, Double.POSITIVE_INFINITY, 0.60, 10)),
                () -> assertInvalid(() -> new FailureScenarioConfig(20, 200.0, 350.0, 0.10, 0.85, -0.01, 10)),
                () -> assertInvalid(() -> new FailureScenarioConfig(20, 200.0, 350.0, 0.10, 0.85, 1.01, 10)),
                () -> assertInvalid(() -> new FailureScenarioConfig(20, 200.0, 350.0, 0.10, 0.85, Double.NaN, 10)),
                () -> assertInvalid(() -> new FailureScenarioConfig(20, 200.0, 350.0, 0.10, 0.85, 0.60, -1))
        );
    }

    @Test
    void invalidSignalIsRejected() {
        assertAll("invalid signal",
                () -> assertInvalid(() -> signal(null, FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, 4, 1, 1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> signal("   ", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, 4, 1, 1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> signal("s", null, "checkout", 4, 4, 1, 1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> signal("s", FailureScenarioType.TRAFFIC_SPIKE, null, 4, 4, 1, 1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> signal("s", FailureScenarioType.TRAFFIC_SPIKE, "   ", 4, 4, 1, 1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> signal("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 0, 0, 1, 1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> signal("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, -1, 1, 1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> signal("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, 5, 1, 1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> signal("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, 4, -1, 1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> signal("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, 4, 1, 0, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> signal("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, 4, 1, 1, -1, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> signal("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, 4, 1, 1, 0, Double.NaN, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> signal("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, 4, 1, 1, 0, 1.0, Double.POSITIVE_INFINITY, 0.0, 100)),
                () -> assertInvalid(() -> signal("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, 4, 1, 1, 0, 1.0, 1.0, -0.01, 100)),
                () -> assertInvalid(() -> signal("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, 4, 1, 1, 0, 1.0, 1.0, 1.01, 100)),
                () -> assertInvalid(() -> signal("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, 4, 1, 1, 0, 1.0, 1.0, Double.NaN, 100)),
                () -> assertInvalid(() -> signal("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout", 4, 4, 1, 1, 0, 1.0, 1.0, 0.0, -1)),
                () -> assertInvalid(() -> new FailureScenarioSignal("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout",
                        4, 4, 1, 1, 0, 1.0, 1.0, 0.0, 100, null))
        );
    }

    @Test
    void invalidResultIsRejected() {
        assertAll("invalid result",
                () -> assertInvalid(() -> new FailureScenarioResult(null, FailureScenarioType.TRAFFIC_SPIKE, "checkout",
                        FailureSeverity.LOW, List.of(MitigationAction.HOLD), "reason", NOW, 1.0, 0.1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> new FailureScenarioResult("   ", FailureScenarioType.TRAFFIC_SPIKE, "checkout",
                        FailureSeverity.LOW, List.of(MitigationAction.HOLD), "reason", NOW, 1.0, 0.1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> new FailureScenarioResult("s", null, "checkout",
                        FailureSeverity.LOW, List.of(MitigationAction.HOLD), "reason", NOW, 1.0, 0.1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> new FailureScenarioResult("s", FailureScenarioType.TRAFFIC_SPIKE, "   ",
                        FailureSeverity.LOW, List.of(MitigationAction.HOLD), "reason", NOW, 1.0, 0.1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> new FailureScenarioResult("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout",
                        null, List.of(MitigationAction.HOLD), "reason", NOW, 1.0, 0.1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> new FailureScenarioResult("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout",
                        FailureSeverity.LOW, null, "reason", NOW, 1.0, 0.1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> new FailureScenarioResult("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout",
                        FailureSeverity.LOW, List.of(), "reason", NOW, 1.0, 0.1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> new FailureScenarioResult("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout",
                        FailureSeverity.LOW, List.of(MitigationAction.HOLD), "   ", NOW, 1.0, 0.1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> new FailureScenarioResult("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout",
                        FailureSeverity.LOW, List.of(MitigationAction.HOLD), "reason", null, 1.0, 0.1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> new FailureScenarioResult("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout",
                        FailureSeverity.LOW, List.of(MitigationAction.HOLD), "reason", NOW, -0.01, 0.1, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> new FailureScenarioResult("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout",
                        FailureSeverity.LOW, List.of(MitigationAction.HOLD), "reason", NOW, 1.0, Double.NaN, 0, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> new FailureScenarioResult("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout",
                        FailureSeverity.LOW, List.of(MitigationAction.HOLD), "reason", NOW, 1.0, 0.1, -1, 1.0, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> new FailureScenarioResult("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout",
                        FailureSeverity.LOW, List.of(MitigationAction.HOLD), "reason", NOW, 1.0, 0.1, 0, Double.POSITIVE_INFINITY, 1.0, 0.0, 100)),
                () -> assertInvalid(() -> new FailureScenarioResult("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout",
                        FailureSeverity.LOW, List.of(MitigationAction.HOLD), "reason", NOW, 1.0, 0.1, 0, 1.0, 1.0, 1.01, 100)),
                () -> assertInvalid(() -> new FailureScenarioResult("s", FailureScenarioType.TRAFFIC_SPIKE, "checkout",
                        FailureSeverity.LOW, List.of(MitigationAction.HOLD), "reason", NOW, 1.0, 0.1, 0, 1.0, 1.0, 0.0, -1))
        );
    }

    @Test
    void reasonIncludesActivePressureSignals() {
        FailureScenarioResult result = runner.evaluate(
                signal("multi-pressure", FailureScenarioType.QUEUE_BACKLOG, "checkout", 6, 3,
                        38, 40, 40, 260.0, 450.0, 0.25, 100), CONFIG);

        assertTrue(result.reason().contains("healthy ratio"));
        assertTrue(result.reason().contains("utilization"));
        assertTrue(result.reason().contains("queue depth"));
        assertTrue(result.reason().contains("p95 latency"));
        assertTrue(result.reason().contains("p99 latency"));
        assertTrue(result.reason().contains("error rate"));
    }

    @Test
    void behaviorIsDeterministic() {
        FailureScenarioSignal signal = signal("deterministic", FailureScenarioType.CAPACITY_SATURATION, "checkout",
                4, 4, 38, 40, 25, 260.0, 450.0, 0.25, 100);

        FailureScenarioResult first = runner.evaluate(signal, CONFIG);
        FailureScenarioResult second = runner.evaluate(signal, CONFIG);

        assertEquals(first, second);
    }

    @Test
    void enumsExposeClearRecommendationVocabulary() {
        assertEquals(List.of(FailureScenarioType.TRAFFIC_SPIKE, FailureScenarioType.SLOW_SERVER,
                FailureScenarioType.QUEUE_BACKLOG, FailureScenarioType.ERROR_STORM,
                FailureScenarioType.FLAPPING_SERVER, FailureScenarioType.PARTIAL_OUTAGE,
                FailureScenarioType.CAPACITY_SATURATION), List.of(FailureScenarioType.values()));
        assertEquals(List.of(MitigationAction.HOLD, MitigationAction.ROUTE_AROUND,
                MitigationAction.REDUCE_CONCURRENCY, MitigationAction.SHED_LOW_PRIORITY,
                MitigationAction.SCALE_UP_SHADOW, MitigationAction.INVESTIGATE,
                MitigationAction.FAIL_CLOSED), List.of(MitigationAction.values()));
    }

    private FailureScenarioSignal normalSignal(FailureScenarioType type) {
        return signal("normal-" + type.name().toLowerCase(), type, "checkout", 4, 4,
                8, 40, 2, 100.0, 150.0, 0.01, 100);
    }

    private FailureScenarioSignal signal(String scenarioId,
                                         FailureScenarioType type,
                                         String targetId,
                                         int totalServers,
                                         int healthyServers,
                                         int currentInFlightRequestCount,
                                         int concurrencyLimit,
                                         int queueDepth,
                                         double observedP95LatencyMillis,
                                         double observedP99LatencyMillis,
                                         double observedErrorRate,
                                         int sampleSize) {
        return new FailureScenarioSignal(scenarioId, type, targetId, totalServers, healthyServers,
                currentInFlightRequestCount, concurrencyLimit, queueDepth, observedP95LatencyMillis,
                observedP99LatencyMillis, observedErrorRate, sampleSize, NOW);
    }

    private void assertInvalid(Executable executable) {
        RuntimeException thrown = assertThrows(RuntimeException.class, executable);
        assertNotNull(thrown.getMessage());
        assertFalse(thrown.getMessage().isBlank());
    }
}
