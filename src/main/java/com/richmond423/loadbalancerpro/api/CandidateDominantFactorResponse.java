package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record CandidateDominantFactorResponse(
        String candidateId,
        boolean selected,
        boolean available,
        int sourceContributionCount,
        List<String> sourceFactorNames,
        DominantFactorResponse largestPositiveContributor,
        DominantFactorResponse largestPenaltyContributor,
        DominantFactorResponse largestAbsoluteImpact,
        String explanation,
        String boundaryNote) {

    public CandidateDominantFactorResponse {
        sourceFactorNames = List.copyOf(sourceFactorNames);
    }
}
