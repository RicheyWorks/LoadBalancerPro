package com.richmond423.loadbalancerpro.core;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record ServerScoreBreakdown(
        String serverId,
        double totalScore,
        List<ScoreFactorContribution> factorContributions) {

    public ServerScoreBreakdown {
        serverId = requireNonBlank(serverId, "serverId");
        if (!Double.isFinite(totalScore)) {
            throw new IllegalArgumentException("totalScore must be finite");
        }
        Objects.requireNonNull(factorContributions, "factorContributions cannot be null");
        factorContributions = List.copyOf(factorContributions);
        for (ScoreFactorContribution contribution : factorContributions) {
            Objects.requireNonNull(contribution, "factorContributions cannot contain null values");
        }
    }

    public double exactContributionTotal() {
        return factorContributions.stream()
                .filter(ScoreFactorContribution::hasExactContributionValue)
                .mapToDouble(contribution -> contribution.contributionValue().orElseThrow())
                .sum();
    }

    public List<ScoreFactorContribution> materialPenaltyFactors() {
        return factorContributions.stream()
                .filter(contribution -> contribution.direction() == ScoreFactorDirection.WEAKENS_SELECTION)
                .filter(ScoreFactorContribution::hasExactContributionValue)
                .filter(contribution -> contribution.contributionValue().orElseThrow() > 0.0)
                .sorted(Comparator
                        .comparingDouble((ScoreFactorContribution contribution) ->
                                contribution.contributionValue().orElseThrow())
                        .reversed()
                        .thenComparing(ScoreFactorContribution::factorName))
                .toList();
    }

    public List<String> topPenaltyFactorNames(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
        return materialPenaltyFactors().stream()
                .limit(limit)
                .map(ScoreFactorContribution::factorName)
                .toList();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
