package com.richmond423.loadbalancerpro.core;

public record TrafficAllocationPolicy(
        double minimumBackendShare,
        double maximumBackendShare,
        double maximumShareChangePerDecision) {

    public TrafficAllocationPolicy {
        requireUnitInterval(minimumBackendShare, "minimumBackendShare");
        requireUnitInterval(maximumBackendShare, "maximumBackendShare");
        requireUnitInterval(maximumShareChangePerDecision, "maximumShareChangePerDecision");
        if (minimumBackendShare > maximumBackendShare) {
            throw new IllegalArgumentException("minimumBackendShare cannot exceed maximumBackendShare");
        }
        if (maximumShareChangePerDecision == 0.0) {
            throw new IllegalArgumentException("maximumShareChangePerDecision must be greater than zero");
        }
    }

    public static TrafficAllocationPolicy localLabDefaults() {
        return new TrafficAllocationPolicy(0.0, 1.0, 0.25);
    }

    private static void requireUnitInterval(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and between 0.0 and 1.0");
        }
    }
}
