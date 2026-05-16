package com.richmond423.loadbalancerpro.api;

import com.richmond423.loadbalancerpro.core.ScoreFactorContribution;

public record ScoreFactorContributionResponse(
        String factorName,
        String rawValueDescription,
        String weightDescription,
        String direction,
        String contributionDescription,
        Double contributionValue,
        String exactness,
        String explanationText,
        String boundaryNote) {

    static ScoreFactorContributionResponse from(ScoreFactorContribution contribution) {
        return new ScoreFactorContributionResponse(
                contribution.factorName(),
                contribution.rawValueDescription(),
                contribution.weightDescription(),
                contribution.direction().name(),
                contribution.contributionDescription(),
                contribution.contributionValue().isPresent()
                        ? contribution.contributionValue().getAsDouble()
                        : null,
                contribution.exactness().name(),
                contribution.explanationText(),
                contribution.boundaryNote());
    }
}
