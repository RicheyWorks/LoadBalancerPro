package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceSourceMapResponse(
        boolean readOnly,
        String sourceMapSchemaVersion,
        String source,
        String status,
        String strategyId,
        String selectedCandidateId,
        int candidateCount,
        String linkedReplaySnapshotFingerprint,
        String linkedReconstructionTraceFingerprint,
        String linkedReplayCapsuleFingerprint,
        String decisionVectorStatus,
        String dominantFactorAnalysisStatus,
        String decisionDeltaAnalysisStatus,
        String decisionReplaySnapshotStatus,
        String decisionReplayReconstructionTraceStatus,
        String decisionReplayCapsuleStatus,
        String decisionReplayReadinessChecklistStatus,
        List<DecisionReplayEvidenceSourceMapEntryResponse> sourceMapEntries,
        String explanation,
        String boundaryNote,
        String productionNotProvenBoundary) {

    public RoutingDecisionReplayEvidenceSourceMapResponse {
        sourceMapEntries = sourceMapEntries == null ? List.of() : List.copyOf(sourceMapEntries);
    }
}
