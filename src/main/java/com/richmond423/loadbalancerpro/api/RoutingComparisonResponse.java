package com.richmond423.loadbalancerpro.api;

import java.time.Instant;
import java.util.List;

public record RoutingComparisonResponse(
        List<String> requestedStrategies,
        int candidateCount,
        Instant timestamp,
        RoutingDecisionReplayEvidenceReviewerClosureRollupResponse decisionReplayEvidenceReviewerClosureRollup,
        RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse decisionReplayEvidenceReviewerClosureChecklist,
        List<RoutingComparisonResultResponse> results) {
}
