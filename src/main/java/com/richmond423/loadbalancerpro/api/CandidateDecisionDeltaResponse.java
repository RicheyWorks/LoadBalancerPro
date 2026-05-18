package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record CandidateDecisionDeltaResponse(
        String selectedCandidateId,
        String closestAlternativeCandidateId,
        Double selectedFinalScore,
        Double alternativeFinalScore,
        Double finalScoreGap,
        Double absoluteFinalScoreGap,
        int selectedContributionCount,
        int alternativeContributionCount,
        int comparedFactorCount,
        List<String> comparedFactorNames,
        List<String> omittedFactorNames,
        String explanation) {

    public CandidateDecisionDeltaResponse {
        comparedFactorNames = List.copyOf(comparedFactorNames);
        omittedFactorNames = List.copyOf(omittedFactorNames);
    }
}
