package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplaySnapshotResponse(
        boolean readOnly,
        String snapshotSchemaVersion,
        String source,
        String status,
        String snapshotFingerprint,
        String fingerprintAlgorithm,
        String selectedCandidateId,
        List<String> candidateIdsConsidered,
        int candidateCount,
        String strategyId,
        String decisionVectorStatus,
        String dominantFactorAnalysisStatus,
        String decisionDeltaAnalysisStatus,
        String closestAlternativeCandidateId,
        Double finalScoreGap,
        String largestDeltaFactorName,
        String explanation,
        String boundaryNote,
        String productionNotProvenBoundary) {

    public RoutingDecisionReplaySnapshotResponse {
        candidateIdsConsidered = candidateIdsConsidered == null ? List.of() : List.copyOf(candidateIdsConsidered);
    }
}
