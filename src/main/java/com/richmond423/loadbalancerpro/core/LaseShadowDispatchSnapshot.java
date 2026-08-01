package com.richmond423.loadbalancerpro.core;

/** Process-local status for the bounded live-proxy shadow evaluation queue. */
public record LaseShadowDispatchSnapshot(
        boolean enabled,
        int queueCapacity,
        int queuedEvaluations,
        int activeEvaluations,
        long totalAccepted,
        long totalCompleted,
        long totalDropped,
        boolean shutdown) {

    public LaseShadowDispatchSnapshot {
        requireNonNegative(queueCapacity, "queueCapacity");
        requireNonNegative(queuedEvaluations, "queuedEvaluations");
        requireNonNegative(activeEvaluations, "activeEvaluations");
        requireNonNegative(totalAccepted, "totalAccepted");
        requireNonNegative(totalCompleted, "totalCompleted");
        requireNonNegative(totalDropped, "totalDropped");
    }

    public static LaseShadowDispatchSnapshot inactive(int queueCapacity) {
        return new LaseShadowDispatchSnapshot(false, queueCapacity, 0, 0, 0, 0, 0, false);
    }

    private static void requireNonNegative(long value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
    }
}
