package com.richmond423.loadbalancerpro.core;

import java.time.Instant;
import java.util.Objects;
import java.util.OptionalDouble;

public record ServerObservation(
        String observationId,
        String serverId,
        ServerObservationSource source,
        ServerObservationOutcome outcome,
        OptionalDouble latencyMillis,
        Instant observedAt) {

    public ServerObservation {
        observationId = requireNonBlank(observationId, "observationId");
        serverId = requireNonBlank(serverId, "serverId");
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(outcome, "outcome cannot be null");
        Objects.requireNonNull(latencyMillis, "latencyMillis cannot be null");
        Objects.requireNonNull(observedAt, "observedAt cannot be null");
        latencyMillis.ifPresent(value -> requireNonNegative(value, "latencyMillis"));
        if (outcome.successful() && latencyMillis.isEmpty()) {
            throw new IllegalArgumentException("successful observations require latencyMillis");
        }
    }

    public static ServerObservation success(
            String observationId,
            String serverId,
            ServerObservationSource source,
            double latencyMillis,
            Instant observedAt) {
        return new ServerObservation(observationId, serverId, source, ServerObservationOutcome.SUCCESS,
                OptionalDouble.of(latencyMillis), observedAt);
    }

    public static ServerObservation failure(
            String observationId,
            String serverId,
            ServerObservationSource source,
            Instant observedAt) {
        return new ServerObservation(observationId, serverId, source, ServerObservationOutcome.FAILURE,
                OptionalDouble.empty(), observedAt);
    }

    public static ServerObservation timeout(
            String observationId,
            String serverId,
            ServerObservationSource source,
            double elapsedMillis,
            Instant observedAt) {
        return new ServerObservation(observationId, serverId, source, ServerObservationOutcome.TIMEOUT,
                OptionalDouble.of(elapsedMillis), observedAt);
    }

    public static ServerObservation connectionFailure(
            String observationId,
            String serverId,
            ServerObservationSource source,
            Instant observedAt) {
        return new ServerObservation(observationId, serverId, source, ServerObservationOutcome.CONNECTION_FAILURE,
                OptionalDouble.empty(), observedAt);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private static void requireNonNegative(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative");
        }
    }
}
