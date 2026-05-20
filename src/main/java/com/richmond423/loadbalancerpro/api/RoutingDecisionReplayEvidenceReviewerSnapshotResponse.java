package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceReviewerSnapshotResponse(
        boolean readOnly,
        String reviewerSnapshotSchemaVersion,
        String source,
        String status,
        String consistencyStatus,
        String referenceIndexStatus,
        String dependencySummaryStatus,
        String statusRollupStatus,
        String dependencyMapStatus,
        String strategyId,
        String selectedCandidateId,
        int candidateCount,
        int totalLaneCount,
        int availableLaneCount,
        int partialLaneCount,
        int unknownLaneCount,
        int checkedSurfaceCount,
        int missingSurfaceCount,
        List<String> missingSurfaces,
        List<String> reviewerHighlights,
        List<String> reviewerWarnings,
        String summaryText,
        List<String> limitations,
        String boundaryNote) {

    public RoutingDecisionReplayEvidenceReviewerSnapshotResponse {
        missingSurfaces = missingSurfaces == null ? List.of() : List.copyOf(missingSurfaces);
        reviewerHighlights = reviewerHighlights == null ? List.of() : List.copyOf(reviewerHighlights);
        reviewerWarnings = reviewerWarnings == null ? List.of() : List.copyOf(reviewerWarnings);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }
}
