package com.richmond423.loadbalancerpro.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ServerObservationWindow {
    private static final Comparator<ServerObservation> OBSERVATION_ORDER = Comparator
            .comparing(ServerObservation::observedAt)
            .thenComparing(ServerObservation::observationId);

    private final String serverId;
    private final ServerObservationWindowPolicy policy;
    private final List<ServerObservation> observations;

    private ServerObservationWindow(
            String serverId,
            ServerObservationWindowPolicy policy,
            List<ServerObservation> observations) {
        this.serverId = requireNonBlank(serverId, "serverId");
        this.policy = Objects.requireNonNull(policy, "policy cannot be null");
        this.observations = List.copyOf(observations);
    }

    public static ServerObservationWindow create(String serverId, ServerObservationWindowPolicy policy) {
        return new ServerObservationWindow(serverId, policy, List.of());
    }

    public ServerObservationWindow append(ServerObservation observation, Instant receivedAt) {
        Objects.requireNonNull(observation, "observation cannot be null");
        Objects.requireNonNull(receivedAt, "receivedAt cannot be null");
        requireMatchingServer(observation);
        if (observation.observedAt().isAfter(receivedAt)) {
            throw new IllegalArgumentException("observation timestamp cannot be after receivedAt");
        }

        for (ServerObservation existing : observations) {
            if (existing.observationId().equals(observation.observationId())) {
                if (existing.equals(observation)) {
                    return this;
                }
                throw new IllegalArgumentException("observationId is already present with different content");
            }
        }

        List<ServerObservation> updated = new ArrayList<>(observations);
        updated.add(observation);
        updated.sort(OBSERVATION_ORDER);
        int startIndex = Math.max(0, updated.size() - policy.maxSampleCount());
        return new ServerObservationWindow(serverId, policy, updated.subList(startIndex, updated.size()));
    }

    public ServerObservationWindow appendAll(
            Collection<ServerObservation> newObservations,
            Instant receivedAt) {
        Objects.requireNonNull(newObservations, "newObservations cannot be null");
        ServerObservationWindow updated = this;
        for (ServerObservation observation : newObservations) {
            updated = updated.append(observation, receivedAt);
        }
        return updated;
    }

    public ServerRollingSignalState snapshot(Instant evaluatedAt) {
        return ServerRollingSignalState.fromObservations(serverId, observations, policy, evaluatedAt);
    }

    public String serverId() {
        return serverId;
    }

    public ServerObservationWindowPolicy policy() {
        return policy;
    }

    public List<ServerObservation> observations() {
        return observations;
    }

    public int size() {
        return observations.size();
    }

    private void requireMatchingServer(ServerObservation observation) {
        if (!serverId.equals(observation.serverId())) {
            throw new IllegalArgumentException("observation serverId must match window serverId");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
