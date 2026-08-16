package com.richmond423.loadbalancerpro.api.proxy;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.richmond423.loadbalancerpro.core.ServerStateVector;

/**
 * Optional boundary from the production proxy into lab-only routing analysis.
 */
public interface LiveRoutingObservationSink {
    boolean isEnabled();

    boolean submit(Observation observation);

    static LiveRoutingObservationSink disabled() {
        return DisabledHolder.INSTANCE;
    }

    record Observation(
            String decisionId,
            Instant observedAt,
            String routeName,
            String strategy,
            String selectionSource,
            String actualSelectedServerId,
            List<ServerStateVector> candidates,
            int initialConcurrencyLimit,
            int telemetrySampleSize) {
        public Observation {
            decisionId = requireNonBlank(decisionId, "decisionId");
            Objects.requireNonNull(observedAt, "observedAt cannot be null");
            routeName = requireNonBlank(routeName, "routeName");
            strategy = requireNonBlank(strategy, "strategy");
            selectionSource = requireNonBlank(selectionSource, "selectionSource");
            actualSelectedServerId = requireNonBlank(actualSelectedServerId, "actualSelectedServerId");
            candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates cannot be null"));
            if (candidates.isEmpty() || candidates.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("candidates must contain at least one non-null candidate");
            }
            boolean selectedCandidatePresent = false;
            for (ServerStateVector candidate : candidates) {
                if (actualSelectedServerId.equals(candidate.serverId())) {
                    selectedCandidatePresent = true;
                    break;
                }
            }
            if (!selectedCandidatePresent) {
                throw new IllegalArgumentException("actualSelectedServerId must belong to candidates");
            }
            if (initialConcurrencyLimit < 1) {
                throw new IllegalArgumentException("initialConcurrencyLimit must be positive");
            }
            if (telemetrySampleSize < 0) {
                throw new IllegalArgumentException("telemetrySampleSize cannot be negative");
            }
        }

        private static String requireNonBlank(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " cannot be null or blank");
            }
            return value.trim();
        }
    }

    final class DisabledHolder {
        private static final LiveRoutingObservationSink INSTANCE = new LiveRoutingObservationSink() {
            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public boolean submit(Observation observation) {
                Objects.requireNonNull(observation, "observation cannot be null");
                return false;
            }
        };

        private DisabledHolder() {
        }
    }
}
