package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayEvidenceReviewerClosurePacketResponse(
        String status,
        boolean reviewerReady,
        String packetVersion,
        List<RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse> sections,
        String summary,
        List<String> reviewerGuidance,
        List<String> notProvenBoundaries) {
    public RoutingDecisionReplayEvidenceReviewerClosurePacketResponse {
        sections = sections == null ? List.of() : List.copyOf(sections);
        reviewerGuidance = reviewerGuidance == null ? List.of() : List.copyOf(reviewerGuidance);
        notProvenBoundaries = notProvenBoundaries == null ? List.of() : List.copyOf(notProvenBoundaries);
    }
}
