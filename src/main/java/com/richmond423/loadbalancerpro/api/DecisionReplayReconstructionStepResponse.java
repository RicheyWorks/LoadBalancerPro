package com.richmond423.loadbalancerpro.api;

public record DecisionReplayReconstructionStepResponse(
        String stepId,
        String status,
        String evidenceSourceFieldPath,
        String explanation,
        String missingEvidenceReason) {
}
