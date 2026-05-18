package com.richmond423.loadbalancerpro.api;

public record DecisionReplayCapsuleFactorEvidenceResponse(
        String factorName,
        boolean appearedInSelectedCandidate,
        boolean appearedInClosestAlternative,
        Double selectedCandidateContribution,
        Double closestAlternativeContribution,
        Double contributionDelta,
        String status) {
}
