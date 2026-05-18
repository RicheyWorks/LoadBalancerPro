package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionReplayCapsuleCandidateEvidenceResponse(
        String candidateId,
        boolean selected,
        Double finalScore,
        List<String> factorNames,
        int contributionCount,
        List<String> dominantFactorNames,
        String status) {

    public DecisionReplayCapsuleCandidateEvidenceResponse {
        factorNames = factorNames == null ? List.of() : List.copyOf(factorNames);
        dominantFactorNames = dominantFactorNames == null ? List.of() : List.copyOf(dominantFactorNames);
    }
}
