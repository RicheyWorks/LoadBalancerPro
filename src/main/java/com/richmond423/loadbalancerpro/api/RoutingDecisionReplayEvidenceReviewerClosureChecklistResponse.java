package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse(
        String status,
        boolean reviewerReady,
        List<RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse> items,
        String summary,
        List<String> notProvenBoundaries) {
    public RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse {
        items = items == null ? List.of() : List.copyOf(items);
        notProvenBoundaries = notProvenBoundaries == null ? List.of() : List.copyOf(notProvenBoundaries);
    }
}
