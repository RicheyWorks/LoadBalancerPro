package com.richmond423.loadbalancerpro.api;

public record ScoreFactorDeltaResponse(
        String factorName,
        Double selectedCandidateContribution,
        Double alternativeCandidateContribution,
        Double contributionDelta,
        Double absoluteDelta,
        String selectedCandidateDirection,
        String alternativeCandidateDirection,
        String explanation) {
}
