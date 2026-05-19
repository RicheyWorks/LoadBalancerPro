package com.richmond423.loadbalancerpro.api;

public record DecisionReplayEvidenceLaneReferenceIndexItemResponse(
        String laneId,
        String label,
        String status,
        String responseFieldPath,
        String uiSectionLabel,
        String docsReferenceLabel,
        int dependencyCount,
        int downstreamCount,
        boolean readOnly,
        boolean boundaryPresent,
        String referenceSummary,
        String boundaryNote) {
}
