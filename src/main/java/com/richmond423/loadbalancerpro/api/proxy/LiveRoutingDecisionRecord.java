package com.richmond423.loadbalancerpro.api.proxy;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.richmond423.loadbalancerpro.core.ServerStateVector;

/**
 * Immutable, process-local evidence for one actual upstream forwarding attempt.
 */
public record LiveRoutingDecisionRecord(
        String decisionId,
        Instant capturedAt,
        long configurationGeneration,
        String routeName,
        String strategy,
        int attempt,
        String selectionSource,
        String chosenUpstreamId,
        List<CandidateState> candidates,
        int responseStatus,
        double latencyMillis,
        boolean retriable,
        String outcome) {

    public LiveRoutingDecisionRecord {
        decisionId = requireNonBlank(decisionId, "decisionId");
        Objects.requireNonNull(capturedAt, "capturedAt cannot be null");
        if (configurationGeneration < 1) {
            throw new IllegalArgumentException("configurationGeneration must be positive");
        }
        routeName = requireNonBlank(routeName, "routeName");
        strategy = requireNonBlank(strategy, "strategy");
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be positive");
        }
        selectionSource = requireNonBlank(selectionSource, "selectionSource");
        chosenUpstreamId = requireNonBlank(chosenUpstreamId, "chosenUpstreamId");
        candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates cannot be null"));
        if (responseStatus < 100 || responseStatus > 599) {
            throw new IllegalArgumentException("responseStatus must be a valid HTTP status");
        }
        if (!Double.isFinite(latencyMillis) || latencyMillis < 0.0) {
            throw new IllegalArgumentException("latencyMillis must be finite and non-negative");
        }
        outcome = requireNonBlank(outcome, "outcome");
    }

    public record CandidateState(
            String upstreamId,
            boolean healthy,
            int inFlightRequestCount,
            Double configuredCapacity,
            Double estimatedConcurrencyLimit,
            double effectiveWeight,
            double averageLatencyMillis,
            double p95LatencyMillis,
            double p99LatencyMillis,
            double recentErrorRate,
            Integer queueDepth,
            Instant observedAt) {

        public CandidateState {
            upstreamId = requireNonBlank(upstreamId, "upstreamId");
            Objects.requireNonNull(observedAt, "observedAt cannot be null");
        }

        static CandidateState from(ServerStateVector state) {
            Objects.requireNonNull(state, "state cannot be null");
            return new CandidateState(
                    state.serverId(),
                    state.healthy(),
                    state.inFlightRequestCount(),
                    state.configuredCapacity().isPresent()
                            ? state.configuredCapacity().getAsDouble()
                            : null,
                    state.estimatedConcurrencyLimit().isPresent()
                            ? state.estimatedConcurrencyLimit().getAsDouble()
                            : null,
                    state.weight(),
                    state.averageLatencyMillis(),
                    state.p95LatencyMillis(),
                    state.p99LatencyMillis(),
                    state.recentErrorRate(),
                    state.queueDepth().isPresent() ? state.queueDepth().getAsInt() : null,
                    state.timestamp());
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
