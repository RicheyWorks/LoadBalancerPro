package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

class ServerObservationTest {

    @Test
    void factoriesPreserveBoundedOutcomeSourceAndLatencySemantics() {
        Instant now = Instant.parse("2026-07-16T15:00:00Z");

        ServerObservation success = ServerObservation.success(
                " observation-1 ",
                " server-a ",
                ServerObservationSource.LOCAL_ROUTING_PATH,
                12.5,
                now);
        ServerObservation timeout = ServerObservation.timeout(
                "observation-2",
                "server-a",
                ServerObservationSource.LOCAL_HEALTH_CHECK,
                500.0,
                now);

        assertEquals("observation-1", success.observationId());
        assertEquals("server-a", success.serverId());
        assertTrue(success.outcome().successful());
        assertEquals(12.5, success.latencyMillis().orElseThrow());
        assertTrue(timeout.outcome().failed());
        assertEquals(ServerObservationOutcome.TIMEOUT, timeout.outcome());
    }

    @Test
    void rejectsMalformedOrAmbiguousObservations() {
        Instant now = Instant.parse("2026-07-16T15:00:00Z");

        assertThrows(IllegalArgumentException.class, () -> new ServerObservation(
                "observation-1",
                "server-a",
                ServerObservationSource.ENTERPRISE_LAB,
                ServerObservationOutcome.SUCCESS,
                OptionalDouble.empty(),
                now));
        assertThrows(IllegalArgumentException.class, () -> ServerObservation.success(
                "observation-1",
                "server-a",
                ServerObservationSource.ENTERPRISE_LAB,
                Double.NaN,
                now));
        assertThrows(IllegalArgumentException.class, () -> ServerObservation.failure(
                " ",
                "server-a",
                ServerObservationSource.ENTERPRISE_LAB,
                now));
    }
}
