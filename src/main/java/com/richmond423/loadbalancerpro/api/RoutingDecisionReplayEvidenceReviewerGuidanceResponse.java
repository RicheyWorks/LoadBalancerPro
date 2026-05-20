package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceReviewerGuidanceResponse(
        boolean readOnly,
        String reviewerGuidanceSchemaVersion,
        String source,
        String status,
        String reviewerPriority,
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
        String primaryReviewerFocus,
        List<String> suggestedReviewSteps,
        List<String> evidenceSurfacesToInspect,
        List<String> cautionNotes,
        String summaryText,
        List<String> limitations,
        String boundaryNote) {
    public RoutingDecisionReplayEvidenceReviewerGuidanceResponse {
        missingSurfaces = missingSurfaces == null ? List.of() : List.copyOf(missingSurfaces);
        suggestedReviewSteps = suggestedReviewSteps == null ? List.of() : List.copyOf(suggestedReviewSteps);
        evidenceSurfacesToInspect =
                evidenceSurfacesToInspect == null ? List.of() : List.copyOf(evidenceSurfacesToInspect);
        cautionNotes = cautionNotes == null ? List.of() : List.copyOf(cautionNotes);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }
}
