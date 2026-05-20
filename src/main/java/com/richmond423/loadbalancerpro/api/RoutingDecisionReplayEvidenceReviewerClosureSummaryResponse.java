package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse(
        boolean readOnly,
        String reviewerClosureSummarySchemaVersion,
        String source,
        String status,
        String closureDisposition,
        String reviewerSnapshotStatus,
        String reviewerGuidanceStatus,
        String reviewerHandoffStatus,
        String consistencyStatus,
        String strategyId,
        String selectedCandidateId,
        int candidateCount,
        int totalLaneCount,
        int availableLaneCount,
        int partialLaneCount,
        int unknownLaneCount,
        List<String> closureBullets,
        List<String> safeConclusions,
        List<String> unresolvedBoundaries,
        List<String> evidenceSurfacesReferenced,
        String summaryText,
        List<String> limitations,
        String boundaryNote) {
    public RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse {
        closureBullets = closureBullets == null ? List.of() : List.copyOf(closureBullets);
        safeConclusions = safeConclusions == null ? List.of() : List.copyOf(safeConclusions);
        unresolvedBoundaries = unresolvedBoundaries == null ? List.of() : List.copyOf(unresolvedBoundaries);
        evidenceSurfacesReferenced = evidenceSurfacesReferenced == null
                ? List.of()
                : List.copyOf(evidenceSurfacesReferenced);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }
}
