package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionReplayEvidenceLaneDependencyItemResponse(
        String laneId,
        String label,
        String status,
        String responseFieldPath,
        List<String> dependsOnLaneIds,
        List<String> downstreamLaneIds,
        int dependencyCount,
        int downstreamCount,
        boolean readOnly,
        boolean boundaryPresent,
        String dependencySummary,
        String boundaryNote) {

    public DecisionReplayEvidenceLaneDependencyItemResponse {
        dependsOnLaneIds = dependsOnLaneIds == null ? List.of() : List.copyOf(dependsOnLaneIds);
        downstreamLaneIds = downstreamLaneIds == null ? List.of() : List.copyOf(downstreamLaneIds);
    }
}
