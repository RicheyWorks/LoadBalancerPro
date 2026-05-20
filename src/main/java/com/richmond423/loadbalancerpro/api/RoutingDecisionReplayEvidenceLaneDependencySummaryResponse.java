package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceLaneDependencySummaryResponse(
        boolean readOnly,
        String laneDependencySummarySchemaVersion,
        String source,
        String status,
        String strategyId,
        String selectedCandidateId,
        int candidateCount,
        int totalLaneCount,
        int availableLaneCount,
        int partialLaneCount,
        int unknownLaneCount,
        int rootLaneCount,
        int terminalLaneCount,
        int maxDependencyCount,
        int maxDownstreamCount,
        List<String> densestDependencyLaneIds,
        List<String> widestDownstreamLaneIds,
        String summaryText,
        List<String> limitations,
        String boundaryNote) {

    public RoutingDecisionReplayEvidenceLaneDependencySummaryResponse {
        densestDependencyLaneIds = densestDependencyLaneIds == null ? List.of() : List.copyOf(densestDependencyLaneIds);
        widestDownstreamLaneIds = widestDownstreamLaneIds == null ? List.of() : List.copyOf(widestDownstreamLaneIds);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }
}
