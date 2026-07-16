package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class ServerObservationWindowTest {
    private static final Instant BASE_TIME = Instant.parse("2026-07-16T15:00:00Z");

    @Test
    void keepsOnlyTheDeterministicallyNewestBoundedSamples() {
        ServerObservationWindow window = ServerObservationWindow.create("server-a", policy(3, 2, 2))
                .append(success("observation-4", 4, 40), BASE_TIME.plusSeconds(5))
                .append(success("observation-1", 1, 10), BASE_TIME.plusSeconds(5))
                .append(success("observation-3", 3, 30), BASE_TIME.plusSeconds(5))
                .append(success("observation-2", 2, 20), BASE_TIME.plusSeconds(5));

        assertEquals(3, window.size());
        assertEquals(
                List.of("observation-2", "observation-3", "observation-4"),
                window.observations().stream().map(ServerObservation::observationId).toList());
        assertThrows(UnsupportedOperationException.class, () -> window.observations().clear());
    }

    @Test
    void identicalObservationIdsAreIdempotentButConflictsFailClosed() {
        ServerObservation observation = success("observation-1", 1, 10);
        ServerObservationWindow window = ServerObservationWindow.create("server-a", policy(4, 2, 2))
                .append(observation, BASE_TIME.plusSeconds(2));

        assertSame(window, window.append(observation, BASE_TIME.plusSeconds(2)));
        assertThrows(IllegalArgumentException.class, () -> window.append(
                success("observation-1", 1, 99), BASE_TIME.plusSeconds(2)));
    }

    @Test
    void rejectsMismatchedAndFutureDatedIngestion() {
        ServerObservationWindow window = ServerObservationWindow.create("server-a", policy(4, 2, 2));

        assertThrows(IllegalArgumentException.class, () -> window.append(
                ServerObservation.success(
                        "observation-1",
                        "server-b",
                        ServerObservationSource.ENTERPRISE_LAB,
                        10,
                        BASE_TIME),
                BASE_TIME));
        assertThrows(IllegalArgumentException.class, () -> window.append(
                success("observation-2", 2, 10), BASE_TIME.plusSeconds(1)));
    }

    @Test
    void distinguishesMissingStaleAndSparseEvidenceWithoutInventingTailPercentiles() {
        ServerObservationWindowPolicy policy = policy(8, 3, 3);
        ServerRollingSignalState missing = ServerObservationWindow.create("server-a", policy)
                .snapshot(BASE_TIME);
        ServerRollingSignalState stale = ServerObservationWindow.create("server-a", policy)
                .append(success("observation-old", 0, 10), BASE_TIME)
                .snapshot(BASE_TIME.plusSeconds(11));
        ServerRollingSignalState sparse = ServerObservationWindow.create("server-a", policy)
                .append(success("observation-new", 9, 15), BASE_TIME.plusSeconds(9))
                .snapshot(BASE_TIME.plusSeconds(10));

        assertEquals(ServerSignalEvidence.MISSING, missing.evidence());
        assertEquals(ServerSignalConfidence.NONE, missing.confidence());
        assertEquals(ServerDegradationState.UNKNOWN, missing.degradationState());
        assertFalse(missing.recommendationEligible());

        assertEquals(ServerSignalEvidence.STALE, stale.evidence());
        assertEquals(0, stale.sampleCount());
        assertEquals(BASE_TIME, stale.latestObservationAt().orElseThrow());
        assertTrue(stale.reasons().contains("latest observation exceeds max sample age"));

        assertEquals(ServerSignalEvidence.SPARSE, sparse.evidence());
        assertEquals(ServerSignalConfidence.LOW, sparse.confidence());
        assertEquals(ServerDegradationState.HEALTHY, sparse.degradationState());
        assertFalse(sparse.recommendationEligible());
        assertTrue(sparse.latencyWindowSignal().rollingAverageLatencyMillis().isPresent());
        assertTrue(sparse.latencyWindowSignal().rollingP95LatencyMillis().isEmpty());
        assertTrue(sparse.latencyWindowSignal().rollingP99LatencyMillis().isEmpty());
    }

    private static ServerObservation success(String id, long secondOffset, double latencyMillis) {
        return ServerObservation.success(
                id,
                "server-a",
                ServerObservationSource.ENTERPRISE_LAB,
                latencyMillis,
                BASE_TIME.plusSeconds(secondOffset));
    }

    private static ServerObservationWindowPolicy policy(int maxSamples, int minimumEvidence, int minimumTail) {
        return new ServerObservationWindowPolicy(
                maxSamples,
                Duration.ofSeconds(10),
                minimumEvidence,
                minimumTail,
                0.20,
                0.75,
                2,
                2,
                0.50);
    }
}
