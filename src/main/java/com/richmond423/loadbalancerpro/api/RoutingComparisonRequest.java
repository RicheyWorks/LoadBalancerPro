package com.richmond423.loadbalancerpro.api;

import java.util.List;

import com.richmond423.loadbalancerpro.api.config.RoutingApiLimitsProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoutingComparisonRequest(
        @Size(max = RoutingApiLimitsProperties.ABSOLUTE_MAX_STRATEGIES,
                message = "strategies must contain at most 6 strategies")
        List<String> strategies,

        @Valid
        @NotNull(message = "servers is required")
        @Size(min = 1, max = RoutingApiLimitsProperties.ABSOLUTE_MAX_CANDIDATES,
                message = "servers must contain at least 1 and at most 32 candidates")
        List<@NotNull(message = "server input cannot be null") @Valid RoutingServerStateInput> servers) {
}
