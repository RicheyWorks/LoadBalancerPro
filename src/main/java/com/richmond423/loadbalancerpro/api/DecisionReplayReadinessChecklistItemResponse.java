package com.richmond423.loadbalancerpro.api;

public record DecisionReplayReadinessChecklistItemResponse(
        String itemId,
        String label,
        String status,
        String evidenceSourceFieldPath,
        String explanation,
        String missingEvidenceReason) {
}
