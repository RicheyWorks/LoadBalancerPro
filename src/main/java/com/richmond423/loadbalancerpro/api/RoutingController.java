package com.richmond423.loadbalancerpro.api;

import java.util.List;

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

    public RoutingController(RoutingComparisonService routingComparisonService,
                             DecisionExplorerPayloadService decisionExplorerPayloadService) {
        this.routingComparisonService = routingComparisonService;
        this.decisionExplorerPayloadService = decisionExplorerPayloadService;
    }

    @PostMapping("/compare")
    public RoutingComparisonResponse compare(@Valid @RequestBody RoutingComparisonRequest request) {
        return routingComparisonService.compare(request);
    }

    @PostMapping("/decision-explorer")
    public List<DecisionExplorerPayloadV1> decisionExplorer(@Valid @RequestBody RoutingComparisonRequest request) {
        return decisionExplorerPayloadService.buildPayloads(routingComparisonService.compare(request));
    }
}
