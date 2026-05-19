package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceStatusRollupResponse(
        boolean readOnly,
        String statusRollupSchemaVersion,
        String source,
        String status,
        String strategyId,
        String selectedCandidateId,
        int candidateCount,
        int availableLaneCount,
        int partialLaneCount,
        int unknownLaneCount,
        List<DecisionReplayEvidenceStatusRollupItemResponse> statusItems,
        String explanation,
        String boundaryNote,
        String productionNotProvenBoundary) {

    public RoutingDecisionReplayEvidenceStatusRollupResponse {
        statusItems = statusItems == null ? List.of() : List.copyOf(statusItems);
    }
}
