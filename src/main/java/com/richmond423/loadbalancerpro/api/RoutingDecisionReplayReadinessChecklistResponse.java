package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayReadinessChecklistResponse(
        boolean readOnly,
        String checklistSchemaVersion,
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
        int availableItemCount,
        int partialItemCount,
        int unknownItemCount,
        List<DecisionReplayReadinessChecklistItemResponse> checklistItems,
        String explanation,
        String boundaryNote,
        String productionNotProvenBoundary) {

    public RoutingDecisionReplayReadinessChecklistResponse {
        checklistItems = checklistItems == null ? List.of() : List.copyOf(checklistItems);
    }
}
