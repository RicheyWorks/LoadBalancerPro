package com.richmond423.loadbalancerpro.api;

public record DecisionReplayEvidenceLaneNavigationItemResponse(
        String laneId,
        String label,
        String status,
        String responseFieldPath,
        String uiSectionLabel,
        String docsReferenceLabel,
        boolean readOnly,
        boolean boundaryPresent,
        String navigationSummary,
        String boundaryNote) {
}
