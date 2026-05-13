package com.richmond423.loadbalancerpro.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ServerInput(
        @NotBlank(message = "server id is required")
        String id,

        @DecimalMin(value = "0.0", inclusive = true, message = "cpuUsage must be at least 0")
        @DecimalMax(value = "100.0", inclusive = true, message = "cpuUsage must be at most 100")
        @NotNull(message = "cpuUsage is required")
        Double cpuUsage,

        @DecimalMin(value = "0.0", inclusive = true, message = "memoryUsage must be at least 0")
        @DecimalMax(value = "100.0", inclusive = true, message = "memoryUsage must be at most 100")
        @NotNull(message = "memoryUsage is required")
        Double memoryUsage,

        @DecimalMin(value = "0.0", inclusive = true, message = "diskUsage must be at least 0")
        @DecimalMax(value = "100.0", inclusive = true, message = "diskUsage must be at most 100")
        @NotNull(message = "diskUsage is required")
        Double diskUsage,

        @DecimalMin(value = "0.0", inclusive = true, message = "capacity must be non-negative")
        @NotNull(message = "capacity is required")
        Double capacity,

        @DecimalMin(value = "0.0", inclusive = true, message = "weight must be non-negative")
        @NotNull(message = "weight is required")
        Double weight,

        @NotNull(message = "healthy is required")
        Boolean healthy) {
}
