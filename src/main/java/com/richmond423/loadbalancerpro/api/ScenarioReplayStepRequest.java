package com.richmond423.loadbalancerpro.api;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record ScenarioReplayStepRequest(
        String stepId,

        @NotBlank(message = "step type is required")
        String type,

        Double requestedLoad,
        String strategy,
        String priority,
        String serverId,
        List<String> routingStrategies,
        Integer currentInFlightRequestCount,
        Integer concurrencyLimit,
        Integer queueDepth,
        Double observedP95LatencyMillis,
        Double observedErrorRate) {
}
