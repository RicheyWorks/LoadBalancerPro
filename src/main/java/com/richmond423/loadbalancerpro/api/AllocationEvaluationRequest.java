package com.richmond423.loadbalancerpro.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AllocationEvaluationRequest(
        @DecimalMin(value = "0.0", inclusive = true, message = "requestedLoad must be non-negative")
        double requestedLoad,

        @Valid
        @NotNull(message = "servers is required")
        @Size(min = 1, message = "servers must contain at least one server")
        List<ServerInput> servers,

        String strategy,

        String priority,

        @Min(value = 0, message = "currentInFlightRequestCount must be non-negative")
        Integer currentInFlightRequestCount,

        @Min(value = 1, message = "concurrencyLimit must be positive")
        Integer concurrencyLimit,

        @Min(value = 0, message = "queueDepth must be non-negative")
        Integer queueDepth,

        @DecimalMin(value = "0.0", inclusive = true,
                message = "observedP95LatencyMillis must be non-negative")
        Double observedP95LatencyMillis,

        @DecimalMin(value = "0.0", inclusive = true, message = "observedErrorRate must be at least 0.0")
        @DecimalMax(value = "1.0", inclusive = true, message = "observedErrorRate must be at most 1.0")
        Double observedErrorRate) {
}
