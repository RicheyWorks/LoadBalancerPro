package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse(
        boolean readOnly,
        String laneNavigationSchemaVersion,
        String source,
        String status,
        String strategyId,
        String selectedCandidateId,
        int candidateCount,
        int availableLaneCount,
        int partialLaneCount,
        int unknownLaneCount,
        List<DecisionReplayEvidenceLaneNavigationItemResponse> navigationItems,
        String explanation,
        String boundaryNote,
        String productionNotProvenBoundary) {

    public RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse {
        navigationItems = navigationItems == null ? List.of() : List.copyOf(navigationItems);
    }
}
