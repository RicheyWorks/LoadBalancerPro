package com.richmond423.loadbalancerpro.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/routing")
public class RoutingController {
    private final RoutingComparisonService routingComparisonService;
    private final DecisionExplorerPayloadService decisionExplorerPayloadService;
    private final DecisionExplorerScenarioCatalogService decisionExplorerScenarioCatalogService;
    private final DecisionExplorerResponseSizeGuard decisionExplorerResponseSizeGuard;

    public RoutingController(RoutingComparisonService routingComparisonService,
                             DecisionExplorerPayloadService decisionExplorerPayloadService,
                             DecisionExplorerScenarioCatalogService decisionExplorerScenarioCatalogService,
                             DecisionExplorerResponseSizeGuard decisionExplorerResponseSizeGuard) {
        this.routingComparisonService = routingComparisonService;
        this.decisionExplorerPayloadService = decisionExplorerPayloadService;
        this.decisionExplorerScenarioCatalogService = decisionExplorerScenarioCatalogService;
        this.decisionExplorerResponseSizeGuard = decisionExplorerResponseSizeGuard;
    }

    @PostMapping("/compare")
    public RoutingComparisonResponse compare(@Valid @RequestBody RoutingComparisonRequest request) {
        return routingComparisonService.compare(request);
    }

    @PostMapping("/decision-explorer")
    public List<DecisionExplorerPayloadV1> decisionExplorer(@Valid @RequestBody RoutingComparisonRequest request) {
        List<DecisionExplorerPayloadV1> response =
                decisionExplorerPayloadService.buildPayloads(routingComparisonService.compare(request));
        decisionExplorerResponseSizeGuard.requireWithinLimit(response);
        return response;
    }

    @GetMapping("/decision-explorer/scenarios")
    public DecisionExplorerScenarioCatalogV1 decisionExplorerScenarios() {
        return decisionExplorerScenarioCatalogService.buildCatalog();
    }
}
