package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceLaneReferenceIndexResponse(
        boolean readOnly,
        String laneReferenceIndexSchemaVersion,
        String source,
        String status,
        String strategyId,
        String selectedCandidateId,
        int candidateCount,
        int availableLaneCount,
        int partialLaneCount,
        int unknownLaneCount,
        List<DecisionReplayEvidenceLaneReferenceIndexItemResponse> referenceItems,
        String explanation,
        String boundaryNote,
        String productionNotProvenBoundary) {

    public RoutingDecisionReplayEvidenceLaneReferenceIndexResponse {
        referenceItems = referenceItems == null ? List.of() : List.copyOf(referenceItems);
    }
}
