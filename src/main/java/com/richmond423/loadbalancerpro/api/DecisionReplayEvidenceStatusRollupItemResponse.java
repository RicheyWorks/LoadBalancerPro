package com.richmond423.loadbalancerpro.api;

public record DecisionReplayEvidenceStatusRollupItemResponse(
        String laneId,
        String label,
        String status,
        String sourceFieldPath,
        boolean readOnly,
        boolean selectedCandidatePresent,
        int candidateCount,
        boolean boundaryPresent,
        String evidenceSummary,
        String boundaryNote) {
}
