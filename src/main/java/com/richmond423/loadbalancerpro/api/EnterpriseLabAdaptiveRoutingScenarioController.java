package com.richmond423.loadbalancerpro.api;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioDrilldown;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioEvidencePacket;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioEvidencePacketBuilder;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioGateEvaluation;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioGateEvaluator;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioRunner;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioSummary;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enterprise-lab")
public class EnterpriseLabAdaptiveRoutingScenarioController {
    private final AdaptiveRoutingScenarioRunner runner;
    private final AdaptiveRoutingScenarioEvidencePacketBuilder evidencePacketBuilder;
    private final AdaptiveRoutingScenarioGateEvaluator gateEvaluator;

    public EnterpriseLabAdaptiveRoutingScenarioController() {
        this(new AdaptiveRoutingScenarioRunner());
    }

    EnterpriseLabAdaptiveRoutingScenarioController(AdaptiveRoutingScenarioRunner runner) {
        this.runner = runner;
        this.evidencePacketBuilder = new AdaptiveRoutingScenarioEvidencePacketBuilder(runner);
        this.gateEvaluator = new AdaptiveRoutingScenarioGateEvaluator(evidencePacketBuilder);
    }

    @GetMapping("/adaptive-routing-scenario-summary")
    public AdaptiveRoutingScenarioSummary adaptiveRoutingScenarioSummary() {
        return runner.runSummary();
    }

    @GetMapping("/adaptive-routing-scenario-detail")
    public AdaptiveRoutingScenarioDrilldown adaptiveRoutingScenarioDetail() {
        return runner.runDrilldown();
    }

    @GetMapping("/adaptive-routing-scenario-evidence-packet")
    public AdaptiveRoutingScenarioEvidencePacket adaptiveRoutingScenarioEvidencePacket() {
        return evidencePacketBuilder.build();
    }

    @GetMapping("/adaptive-routing-scenario-gate-evaluation")
    public AdaptiveRoutingScenarioGateEvaluation adaptiveRoutingScenarioGateEvaluation() {
        return gateEvaluator.evaluate();
    }
}
