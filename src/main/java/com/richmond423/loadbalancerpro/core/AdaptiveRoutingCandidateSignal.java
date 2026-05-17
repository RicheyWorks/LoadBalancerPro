package com.richmond423.loadbalancerpro.core;

import java.util.List;
import java.util.Objects;

public record AdaptiveRoutingCandidateSignal(
        String serverId,
        boolean healthy,
        int inFlightRequestCount,
        double configuredCapacity,
        double estimatedConcurrencyLimit,
        double weight,
        double averageLatencyMillis,
        double p95LatencyMillis,
        double p99LatencyMillis,
        double recentErrorRate,
        int queueDepth,
        List<String> signalNotes) {

    public AdaptiveRoutingCandidateSignal {
        serverId = requireNonBlank(serverId, "serverId");
        requireNonNegative(inFlightRequestCount, "inFlightRequestCount");
        requireNonNegative(configuredCapacity, "configuredCapacity");
        requireNonNegative(estimatedConcurrencyLimit, "estimatedConcurrencyLimit");
        requireNonNegative(weight, "weight");
        requireNonNegative(averageLatencyMillis, "averageLatencyMillis");
        requireNonNegative(p95LatencyMillis, "p95LatencyMillis");
        requireNonNegative(p99LatencyMillis, "p99LatencyMillis");
        requireRate(recentErrorRate, "recentErrorRate");
        requireNonNegative(queueDepth, "queueDepth");
        Objects.requireNonNull(signalNotes, "signalNotes cannot be null");
        signalNotes = List.copyOf(signalNotes);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
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

    private static void requireRate(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
        }
    }
}
