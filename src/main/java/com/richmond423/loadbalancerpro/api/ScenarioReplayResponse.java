package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record ScenarioReplayResponse(
        String scenarioId,
        boolean readOnly,
        boolean cloudMutation,
        List<ScenarioReplayStepResponse> steps) {
}
