package com.richmond423.loadbalancerpro.api;

public record LoadSheddingEvaluation(
        String priority,
        String action,
        String reason,
        String targetId,
        int currentInFlightRequestCount,
        int concurrencyLimit,
        int queueDepth,
        double utilization,
        double observedP95LatencyMillis,
        double observedErrorRate) {
}
