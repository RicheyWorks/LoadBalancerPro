package com.richmond423.loadbalancerpro.core;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Privacy-bounded input for evaluating one real proxy choice in LASE shadow mode.
 */
public record LiveRoutingShadowObservation(
        String decisionId,
        Instant observedAt,
        String routeName,
        String strategy,
        String selectionSource,
        String actualSelectedServerId,
        List<ServerStateVector> candidates,
        int initialConcurrencyLimit,
        int telemetrySampleSize) {

    public LiveRoutingShadowObservation {
        decisionId = requireNonBlank(decisionId, "decisionId");
        Objects.requireNonNull(observedAt, "observedAt cannot be null");
        routeName = requireNonBlank(routeName, "routeName");
        strategy = requireNonBlank(strategy, "strategy");
        selectionSource = requireNonBlank(selectionSource, "selectionSource");
        actualSelectedServerId = requireNonBlank(actualSelectedServerId, "actualSelectedServerId");
        candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates cannot be null"));
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates cannot be empty");
        }
        if (telemetrySampleSize < 0) {
            throw new IllegalArgumentException("telemetrySampleSize cannot be negative");
        }
        if (initialConcurrencyLimit < 1) {
            throw new IllegalArgumentException("initialConcurrencyLimit must be positive");
        }
        if (candidates.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("candidates cannot contain null candidates");
        }
        boolean actualCandidatePresent = false;
        for (ServerStateVector candidate : candidates) {
            if (actualSelectedServerId.equals(candidate.serverId())) {
                actualCandidatePresent = true;
                break;
            }
        }
        if (!actualCandidatePresent) {
            throw new IllegalArgumentException("actualSelectedServerId must belong to candidates");
        }
    }

    public List<String> candidateServerIds() {
        return candidates.stream().map(ServerStateVector::serverId).toList();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
