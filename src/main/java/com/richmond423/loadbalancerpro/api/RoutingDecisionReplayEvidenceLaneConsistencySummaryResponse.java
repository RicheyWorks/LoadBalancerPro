package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse(
        boolean readOnly,
        String laneConsistencySummarySchemaVersion,
        String source,
        String status,
        String referenceIndexStatus,
        String dependencySummaryStatus,
        String statusRollupStatus,
        String dependencyMapStatus,
        String strategyId,
        String selectedCandidateId,
        int candidateCount,
        int totalLaneCount,
        int availableLaneCount,
        int partialLaneCount,
        int unknownLaneCount,
        int dependencyMapLaneCount,
        int referenceIndexLaneCount,
        int dependencySummaryLaneCount,
        List<String> mismatchedCountFields,
        List<String> missingSurfaces,
        List<DecisionReplayEvidenceLaneConsistencyCheckResponse> consistencyChecks,
        String summaryText,
        List<String> limitations,
        String boundaryNote) {

    public RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse {
        mismatchedCountFields = mismatchedCountFields == null ? List.of() : List.copyOf(mismatchedCountFields);
        missingSurfaces = missingSurfaces == null ? List.of() : List.copyOf(missingSurfaces);
        consistencyChecks = consistencyChecks == null ? List.of() : List.copyOf(consistencyChecks);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }
}
