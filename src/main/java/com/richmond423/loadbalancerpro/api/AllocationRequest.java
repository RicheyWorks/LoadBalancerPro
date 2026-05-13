package com.richmond423.loadbalancerpro.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AllocationRequest(
        @NotNull(message = "requestedLoad is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "requestedLoad must be non-negative")
        Double requestedLoad,

        @Valid
        @NotNull(message = "servers is required")
        @Size(min = 1, message = "servers must contain at least one server")
        List<ServerInput> servers) {
}
