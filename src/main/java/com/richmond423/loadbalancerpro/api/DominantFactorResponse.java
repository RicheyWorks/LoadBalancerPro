package com.richmond423.loadbalancerpro.api;

public record DominantFactorResponse(
        String factorName,
        String direction,
        Double contributionValue,
        Double absoluteImpact,
        String contributionDescription,
        String explanationText,
        String boundaryNote) {
}
