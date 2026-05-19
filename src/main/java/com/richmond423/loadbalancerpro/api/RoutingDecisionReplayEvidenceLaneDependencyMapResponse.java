package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceLaneDependencyMapResponse(
        boolean readOnly,
        String laneDependencyMapSchemaVersion,
        String source,
        String status,
        String strategyId,
        String selectedCandidateId,
        int candidateCount,
        int availableLaneCount,
        int partialLaneCount,
        int unknownLaneCount,
        List<DecisionReplayEvidenceLaneDependencyItemResponse> dependencyItems,
        String explanation,
        String boundaryNote,
        String productionNotProvenBoundary) {

    public RoutingDecisionReplayEvidenceLaneDependencyMapResponse {
        dependencyItems = dependencyItems == null ? List.of() : List.copyOf(dependencyItems);
    }
}
