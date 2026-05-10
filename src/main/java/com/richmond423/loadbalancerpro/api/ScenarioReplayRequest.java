package com.richmond423.loadbalancerpro.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScenarioReplayRequest(
        String scenarioId,

        @Valid
        @NotNull(message = "servers is required")
        @Size(min = 1, message = "servers must contain at least one server")
        List<ServerInput> servers,

        @Valid
        @NotNull(message = "steps are required")
        @Size(min = 1, message = "steps must contain at least one scenario step")
        List<ScenarioReplayStepRequest> steps) {
}
