package com.richmond423.loadbalancerpro.core;

public record AdaptiveRoutingExperimentServer(
        String id,
        double cpuUsage,
        double memoryUsage,
        double diskUsage,
        double capacity,
        double weight,
        boolean healthy) {

    public AdaptiveRoutingExperimentServer {
        id = requireNonBlank(id, "id");
        requirePercent(cpuUsage, "cpuUsage");
        requirePercent(memoryUsage, "memoryUsage");
        requirePercent(diskUsage, "diskUsage");
        requireNonNegative(capacity, "capacity");
        requireNonNegative(weight, "weight");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private static void requirePercent(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0 || value > 100.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and between 0 and 100");
        }
    }

    private static void requireNonNegative(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative");
        }
    }
}
