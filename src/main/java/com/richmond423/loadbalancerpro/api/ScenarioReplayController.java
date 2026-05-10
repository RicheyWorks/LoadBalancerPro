package com.richmond423.loadbalancerpro.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioReplayController {
    private final ScenarioReplayService scenarioReplayService;

    public ScenarioReplayController(ScenarioReplayService scenarioReplayService) {
        this.scenarioReplayService = scenarioReplayService;
    }

    @PostMapping("/replay")
    public ScenarioReplayResponse replay(@Valid @RequestBody ScenarioReplayRequest request) {
        return scenarioReplayService.replay(request);
    }
}
