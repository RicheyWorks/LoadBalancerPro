package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ServerRollingSignalStateTest {
    private static final Instant BASE_TIME = Instant.parse("2026-07-16T15:00:00Z");
    private static final ServerObservationWindowPolicy POLICY = new ServerObservationWindowPolicy(
            12,
            Duration.ofSeconds(30),
            4,
            3,
            0.20,
            0.75,
            2,
            2,
            0.50);

    @Test
    void computesDeterministicCountsRatesLatencyAndConfidence() {
        ServerRollingSignalState state = window()
                .append(success("observation-1", 1, 10), BASE_TIME.plusSeconds(5))
                .append(success("observation-2", 2, 20), BASE_TIME.plusSeconds(5))
                .append(success("observation-3", 3, 30), BASE_TIME.plusSeconds(5))
                .append(success("observation-4", 4, 100), BASE_TIME.plusSeconds(5))
                .snapshot(BASE_TIME.plusSeconds(5));

        assertEquals(4, state.sampleCount());
        assertEquals(4, state.successCount());
        assertEquals(0, state.failureCount());
        assertEquals(1.0, state.successRate());
        assertEquals(0.0, state.failureRate());
        assertEquals(4, state.consecutiveSuccessCount());
        assertEquals(ServerSignalEvidence.SUFFICIENT, state.evidence());
        assertEquals(ServerSignalConfidence.MEDIUM, state.confidence());
        assertEquals(ServerDegradationState.HEALTHY, state.degradationState());
        assertTrue(state.latencyWindowSignal().hasTailLatencyWindowValues());
        assertEquals(40.0, state.latencyWindowSignal().rollingAverageLatencyMillis().orElseThrow());
        assertEquals(100.0, state.latencyWindowSignal().rollingP95LatencyMillis().orElseThrow());
        assertEquals(100.0, state.latencyWindowSignal().rollingP99LatencyMillis().orElseThrow());
        assertTrue(state.recommendationEligible());
    }

    @Test
    void representsPartialDegradationWithoutCollapsingItIntoFailure() {
        ServerRollingSignalState state = window()
                .append(success("observation-1", 1, 25), BASE_TIME.plusSeconds(5))
                .append(timeout("observation-2", 2, 250), BASE_TIME.plusSeconds(5))
                .append(success("observation-3", 3, 25), BASE_TIME.plusSeconds(5))
                .append(success("observation-4", 4, 25), BASE_TIME.plusSeconds(5))
                .snapshot(BASE_TIME.plusSeconds(5));

        assertEquals(3, state.successCount());
        assertEquals(1, state.failureCount());
        assertEquals(1, state.timeoutCount());
        assertEquals(0.25, state.failureRate());
        assertEquals(0.25, state.timeoutRate());
        assertEquals(ServerDegradationState.PARTIALLY_DEGRADED, state.degradationState());
        assertFalse(state.recovering());
        assertTrue(state.recommendationEligible());
        assertEquals(0.25, state.networkAwarenessSignal().timeoutRate());
        assertEquals(1, state.networkAwarenessSignal().requestTimeoutCount());
    }

    @Test
    void consecutiveFailuresFailClosedAndRecoveryRequiresAConfiguredSuccessRun() {
        ServerObservationWindow failedWindow = window()
                .append(success("observation-1", 1, 25), BASE_TIME.plusSeconds(6))
                .append(failure("observation-2", 2), BASE_TIME.plusSeconds(6))
                .append(timeout("observation-3", 3, 250), BASE_TIME.plusSeconds(6));
        ServerRollingSignalState failed = failedWindow.snapshot(BASE_TIME.plusSeconds(6));
        ServerRollingSignalState recovering = failedWindow
                .append(success("observation-4", 4, 30), BASE_TIME.plusSeconds(6))
                .snapshot(BASE_TIME.plusSeconds(6));
        ServerRollingSignalState recoveredToPartial = failedWindow
                .append(success("observation-4", 4, 30), BASE_TIME.plusSeconds(6))
                .append(success("observation-5", 5, 30), BASE_TIME.plusSeconds(6))
                .snapshot(BASE_TIME.plusSeconds(6));

        assertEquals(2, failed.consecutiveFailureCount());
        assertEquals(ServerDegradationState.FAILED, failed.degradationState());
        assertFalse(failed.recommendationEligible());

        assertEquals(1, recovering.consecutiveSuccessCount());
        assertTrue(recovering.recovering());
        assertEquals(ServerDegradationState.RECOVERING, recovering.degradationState());
        assertFalse(recovering.recommendationEligible());

        assertEquals(2, recoveredToPartial.consecutiveSuccessCount());
        assertFalse(recoveredToPartial.recovering());
        assertEquals(ServerDegradationState.PARTIALLY_DEGRADED, recoveredToPartial.degradationState());
        assertTrue(recoveredToPartial.recommendationEligible());
    }

    @Test
    void exposesOnlyFreshSourcesAndKeepsReasonCollectionsImmutable() {
        ServerRollingSignalState state = window()
                .append(ServerObservation.success(
                        "observation-1",
                        "server-a",
                        ServerObservationSource.LOCAL_HEALTH_CHECK,
                        10,
                        BASE_TIME.plusSeconds(1)), BASE_TIME.plusSeconds(5))
                .append(success("observation-2", 2, 20), BASE_TIME.plusSeconds(5))
                .snapshot(BASE_TIME.plusSeconds(5));

        assertEquals(
                List.of(ServerObservationSource.LOCAL_HEALTH_CHECK, ServerObservationSource.ENTERPRISE_LAB),
                state.sources());
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> state.reasons().clear());
    }

    @Test
    void publicStateConstructorRejectsInconsistentDerivedValues() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
                new ServerRollingSignalState(
                        "server-a",
                        2,
                        2,
                        0,
                        0,
                        0,
                        0.5,
                        0.5,
                        0.0,
                        0.0,
                        0,
                        2,
                        LatencyWindowSignal.fromSamples(List.of(10.0, 20.0), 0.5),
                        ServerSignalEvidence.SUFFICIENT,
                        ServerSignalConfidence.MEDIUM,
                        ServerDegradationState.HEALTHY,
                        false,
                        Optional.of(BASE_TIME.plusSeconds(2)),
                        List.of(ServerObservationSource.ENTERPRISE_LAB),
                        List.of("test"),
                        BASE_TIME.plusSeconds(3)));
    }

    private static ServerObservationWindow window() {
        return ServerObservationWindow.create("server-a", POLICY);
    }

    private static ServerObservation success(String id, long offsetSeconds, double latencyMillis) {
        return ServerObservation.success(
                id,
                "server-a",
                ServerObservationSource.ENTERPRISE_LAB,
                latencyMillis,
                BASE_TIME.plusSeconds(offsetSeconds));
    }

    private static ServerObservation timeout(String id, long offsetSeconds, double elapsedMillis) {
        return ServerObservation.timeout(
                id,
                "server-a",
                ServerObservationSource.ENTERPRISE_LAB,
                elapsedMillis,
                BASE_TIME.plusSeconds(offsetSeconds));
    }

    private static ServerObservation failure(String id, long offsetSeconds) {
        return ServerObservation.failure(
                id,
                "server-a",
                ServerObservationSource.ENTERPRISE_LAB,
                BASE_TIME.plusSeconds(offsetSeconds));
    }
}
