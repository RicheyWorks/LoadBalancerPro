package com.richmond423.loadbalancerpro.core;

public record TrafficAllocationGuardrailPolicy(
        double maximumBackendShare,
        double maximumTotalShareMovement) {

    public TrafficAllocationGuardrailPolicy {
        requireUnitInterval(maximumBackendShare, "maximumBackendShare");
        requireUnitInterval(maximumTotalShareMovement, "maximumTotalShareMovement");
        if (maximumBackendShare == 0.0) {
            throw new IllegalArgumentException("maximumBackendShare must be greater than zero");
        }
    }

    public static TrafficAllocationGuardrailPolicy localLabDefaults() {
        return new TrafficAllocationGuardrailPolicy(0.75, 0.25);
    }

    private static void requireUnitInterval(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and between 0.0 and 1.0");
        }
    }
}
