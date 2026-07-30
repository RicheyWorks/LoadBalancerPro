package com.richmond423.loadbalancerpro.api.explain;

import java.util.List;

/**
 * Compact, read-only explanation of evidence already computed by a routing comparison.
 *
 * <p>The nested rows contain values, not parallel confidence, diagnostics, replay, or
 * reviewer-status restatements.</p>
 */
public record RoutingExplanation(
        boolean readOnly,
        boolean simulationOnly,
        String contractVersion,
        String strategyId,
        String status,
        String selectedCandidateId,
        List<CandidateFactors> candidates,
        DominantFactors dominantFactors,
        DecisionDelta decisionDelta,
        List<CounterfactualWeightScenario> counterfactualWeightScenarios,
        String boundaryNote) {

    public static final String CONTRACT_VERSION = "v2";

    public RoutingExplanation {
        readOnly = true;
        simulationOnly = true;
        contractVersion = CONTRACT_VERSION;
        candidates = List.copyOf(candidates);
        counterfactualWeightScenarios = List.copyOf(counterfactualWeightScenarios);
    }

    public record CandidateFactors(
            String candidateId,
            boolean selected,
            List<FactorContribution> factors) {

        public CandidateFactors {
            factors = List.copyOf(factors);
        }
    }

    public record FactorContribution(
            String factorName,
            Double contributionValue,
            String direction,
            String exactness) {
    }

    public record DominantFactors(
            String status,
            List<CandidateDominantFactor> candidates) {

        public DominantFactors {
            candidates = List.copyOf(candidates);
        }
    }

    public record CandidateDominantFactor(
            String candidateId,
            boolean selected,
            FactorContribution largestSupport,
            FactorContribution largestPenalty,
            FactorContribution largestAbsoluteImpact) {
    }

    public record DecisionDelta(
            String status,
            String closestAlternativeCandidateId,
            Double finalScoreGap,
            List<FactorDelta> factors) {

        public DecisionDelta {
            factors = List.copyOf(factors);
        }
    }

    public record FactorDelta(
            String factorName,
            Double selectedContribution,
            Double alternativeContribution,
            Double contributionDelta) {
    }

    /**
     * Arithmetic projection over returned factor evidence. It does not rerun a strategy,
     * mutate configured weights, or claim that the projected gap changes a real decision.
     */
    public record CounterfactualWeightScenario(
            String factorName,
            int weightShiftPercent,
            Double adjustedSelectedContribution,
            Double adjustedAlternativeContribution,
            Double adjustedContributionDelta,
            Double projectedFinalScoreGap) {
    }
}
