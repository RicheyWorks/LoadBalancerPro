package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceFieldInventoryResponse(
        boolean readOnly,
        String fieldInventorySchemaVersion,
        String source,
        String status,
        String strategyId,
        String selectedCandidateId,
        int candidateCount,
        String decisionVectorStatus,
        String dominantFactorAnalysisStatus,
        String decisionDeltaAnalysisStatus,
        String decisionReplaySnapshotStatus,
        String decisionReplayReconstructionTraceStatus,
        String decisionReplayCapsuleStatus,
        String decisionReplayReadinessChecklistStatus,
        String decisionReplayEvidenceSourceMapStatus,
        String decisionReplayEvidenceBoundarySummaryStatus,
        int availableInventoryGroupCount,
        int partialInventoryGroupCount,
        int unknownInventoryGroupCount,
        List<DecisionReplayEvidenceFieldInventoryEntryResponse> inventoryEntries,
        String explanation,
        String boundaryNote,
        String productionNotProvenBoundary) {

    public RoutingDecisionReplayEvidenceFieldInventoryResponse {
        inventoryEntries = inventoryEntries == null ? List.of() : List.copyOf(inventoryEntries);
    }
}
