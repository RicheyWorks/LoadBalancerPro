package com.richmond423.loadbalancerpro.api;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioDrilldown;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioRunner;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioSummary;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enterprise-lab")
public class EnterpriseLabAdaptiveRoutingScenarioController {
    private final AdaptiveRoutingScenarioRunner runner;

    public EnterpriseLabAdaptiveRoutingScenarioController() {
        this(new AdaptiveRoutingScenarioRunner());
    }

    EnterpriseLabAdaptiveRoutingScenarioController(AdaptiveRoutingScenarioRunner runner) {
        this.runner = runner;
    }

    @GetMapping("/adaptive-routing-scenario-summary")
    public AdaptiveRoutingScenarioSummary adaptiveRoutingScenarioSummary() {
        return runner.runSummary();
    }

    @GetMapping("/adaptive-routing-scenario-detail")
    public AdaptiveRoutingScenarioDrilldown adaptiveRoutingScenarioDetail() {
        return runner.runDrilldown();
    }
}
