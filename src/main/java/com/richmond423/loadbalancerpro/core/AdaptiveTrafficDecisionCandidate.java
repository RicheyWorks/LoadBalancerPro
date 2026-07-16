package com.richmond423.loadbalancerpro.core;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;

public record AdaptiveTrafficDecisionCandidate(
        String serverId,
        boolean healthy,
        double configuredCapacity,
        double weight,
        int inFlightRequestCount,
        OptionalDouble estimatedConcurrencyLimit,
        OptionalInt queueDepth,
        List<ServerObservation> observations) {

    public AdaptiveTrafficDecisionCandidate {
        serverId = requireCanonicalId(serverId, "serverId");
        requireNonNegative(configuredCapacity, "configuredCapacity");
        requireNonNegative(weight, "weight");
        requireNonNegative(inFlightRequestCount, "inFlightRequestCount");
        Objects.requireNonNull(estimatedConcurrencyLimit, "estimatedConcurrencyLimit cannot be null");
        Objects.requireNonNull(queueDepth, "queueDepth cannot be null");
        estimatedConcurrencyLimit.ifPresent(value -> requirePositive(value, "estimatedConcurrencyLimit"));
        queueDepth.ifPresent(value -> requireNonNegative(value, "queueDepth"));
        Objects.requireNonNull(observations, "observations cannot be null");
        Set<String> observationIds = new HashSet<>();
        for (ServerObservation observation : observations) {
            Objects.requireNonNull(observation, "observations cannot contain null values");
            if (!serverId.equals(observation.serverId())) {
                throw new IllegalArgumentException("observation serverId must match candidate serverId");
            }
            if (!observationIds.add(observation.observationId())) {
                throw new IllegalArgumentException("candidate observationIds must be unique");
            }
        }
        observations = observations.stream()
                .sorted(Comparator.comparing(ServerObservation::observedAt)
                        .thenComparing(ServerObservation::observationId))
                .toList();
    }

    Server toServer() {
        Server server = new Server(serverId, 0.0, 0.0, 0.0, ServerType.ONSITE);
        server.setCapacity(configuredCapacity);
        server.setWeight(weight);
        server.setHealthy(healthy);
        return server;
    }

    private static String requireCanonicalId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must not have surrounding whitespace");
        }
        return value;
    }

    private static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }

    private static void requireNonNegative(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative");
        }
    }

    private static void requirePositive(double value, String fieldName) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and positive");
        }
    }
}
