package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceReviewerClosureRollupResponse(
        String status,
        String disposition,
        int resultCount,
        int resultsWithClosureSummary,
        int resultsMissingClosureSummary,
        int completeWithLimitationsCount,
        int unknownCount,
        boolean reviewerReady,
        String summary,
        List<String> notProvenBoundaries) {
    public RoutingDecisionReplayEvidenceReviewerClosureRollupResponse {
        notProvenBoundaries = notProvenBoundaries == null ? List.of() : List.copyOf(notProvenBoundaries);
    }
}
