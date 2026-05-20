package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse(
        boolean readOnly,
        String reviewerHandoffSummarySchemaVersion,
        String source,
        String status,
        String handoffPriority,
        String reviewerSnapshotStatus,
        String reviewerGuidanceStatus,
        String consistencyStatus,
        String strategyId,
        String selectedCandidateId,
        int candidateCount,
        int totalLaneCount,
        int availableLaneCount,
        int partialLaneCount,
        int unknownLaneCount,
        List<String> handoffBullets,
        List<String> operatorFollowUpItems,
        List<String> evidenceSurfacesReferenced,
        List<String> cautionNotes,
        String summaryText,
        List<String> limitations,
        String boundaryNote) {

    public RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse {
        handoffBullets = handoffBullets == null ? List.of() : List.copyOf(handoffBullets);
        operatorFollowUpItems = operatorFollowUpItems == null ? List.of() : List.copyOf(operatorFollowUpItems);
        evidenceSurfacesReferenced = evidenceSurfacesReferenced == null
                ? List.of()
                : List.copyOf(evidenceSurfacesReferenced);
        cautionNotes = cautionNotes == null ? List.of() : List.copyOf(cautionNotes);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }
}
