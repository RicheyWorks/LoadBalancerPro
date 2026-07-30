package com.richmond423.loadbalancerpro.api.explain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.richmond423.loadbalancerpro.api.CandidateDecisionDeltaResponse;
import com.richmond423.loadbalancerpro.api.CandidateDecisionVectorResponse;
import com.richmond423.loadbalancerpro.api.CandidateDominantFactorResponse;
import com.richmond423.loadbalancerpro.api.DominantFactorAnalysisResponse;
import com.richmond423.loadbalancerpro.api.DominantFactorResponse;
import com.richmond423.loadbalancerpro.api.RoutingComparisonResponse;
import com.richmond423.loadbalancerpro.api.RoutingComparisonResultResponse;
import com.richmond423.loadbalancerpro.api.RoutingDecisionDeltaAnalysisResponse;
import com.richmond423.loadbalancerpro.api.ScoreFactorContributionResponse;
import com.richmond423.loadbalancerpro.api.ScoreFactorDeltaResponse;
import org.springframework.stereotype.Service;

@Service
public final class RoutingExplanationService {
    private static final String UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Read-only, simulation-only arithmetic over evidence returned by the routing comparison. "
                    + "It does not execute replay, rerun a strategy, mutate weights or traffic, call external "
                    + "systems, persist evidence, or prove production behavior.";

    public List<RoutingExplanation> explain(RoutingComparisonResponse comparison) {
        if (comparison == null || comparison.results() == null) {
            return List.of();
        }
        return comparison.results().stream()
                .map(this::explain)
                .toList();
    }

    public RoutingExplanation explain(RoutingComparisonResultResponse result) {
        List<RoutingExplanation.CandidateFactors> candidates = candidateFactors(result);
        RoutingExplanation.DominantFactors dominantFactors = dominantFactors(result);
        RoutingExplanation.DecisionDelta decisionDelta = decisionDelta(result);
        return new RoutingExplanation(
                true,
                true,
                RoutingExplanation.CONTRACT_VERSION,
                valueOrUnknown(result == null ? null : result.strategyId()),
                valueOrUnknown(result == null ? null : result.status()),
                valueOrUnknown(result == null ? null : result.chosenServerId()),
                candidates,
                dominantFactors,
                decisionDelta,
                counterfactualWeightScenarios(decisionDelta),
                BOUNDARY_NOTE);
    }

    private static List<RoutingExplanation.CandidateFactors> candidateFactors(
            RoutingComparisonResultResponse result) {
        if (result == null || result.decisionVector() == null
                || result.decisionVector().candidateSummaries() == null) {
            return List.of();
        }
        return result.decisionVector().candidateSummaries().stream()
                .filter(candidate -> candidate != null)
                .sorted(Comparator.comparing(CandidateDecisionVectorResponse::candidateId,
                        Comparator.nullsLast(String::compareTo)))
                .map(candidate -> new RoutingExplanation.CandidateFactors(
                        valueOrUnknown(candidate.candidateId()),
                        candidate.selected(),
                        candidate.factorContributions().stream()
                                .filter(factor -> factor != null)
                                .sorted(Comparator.comparing(ScoreFactorContributionResponse::factorName,
                                        Comparator.nullsLast(String::compareTo)))
                                .map(RoutingExplanationService::factorContribution)
                                .toList()))
                .toList();
    }

    private static RoutingExplanation.FactorContribution factorContribution(
            ScoreFactorContributionResponse factor) {
        return new RoutingExplanation.FactorContribution(
                valueOrUnknown(factor.factorName()),
                finiteOrNull(factor.contributionValue()),
                valueOrUnknown(factor.direction()),
                valueOrUnknown(factor.exactness()));
    }

    private static RoutingExplanation.FactorContribution factorContribution(
            DominantFactorResponse factor) {
        if (factor == null) {
            return null;
        }
        return new RoutingExplanation.FactorContribution(
                valueOrUnknown(factor.factorName()),
                finiteOrNull(factor.contributionValue()),
                valueOrUnknown(factor.direction()),
                "RETURNED_ANALYSIS");
    }

    private static RoutingExplanation.DominantFactors dominantFactors(
            RoutingComparisonResultResponse result) {
        DominantFactorAnalysisResponse analysis = result == null ? null : result.dominantFactorAnalysis();
        if (analysis == null) {
            return new RoutingExplanation.DominantFactors(UNKNOWN, List.of());
        }
        List<RoutingExplanation.CandidateDominantFactor> candidates = analysis.candidateAnalyses().stream()
                .filter(candidate -> candidate != null)
                .sorted(Comparator.comparing(CandidateDominantFactorResponse::candidateId,
                        Comparator.nullsLast(String::compareTo)))
                .map(candidate -> new RoutingExplanation.CandidateDominantFactor(
                        valueOrUnknown(candidate.candidateId()),
                        candidate.selected(),
                        factorContribution(candidate.largestPositiveContributor()),
                        factorContribution(candidate.largestPenaltyContributor()),
                        factorContribution(candidate.largestAbsoluteImpact())))
                .toList();
        return new RoutingExplanation.DominantFactors(valueOrUnknown(analysis.status()), candidates);
    }

    private static RoutingExplanation.DecisionDelta decisionDelta(
            RoutingComparisonResultResponse result) {
        RoutingDecisionDeltaAnalysisResponse analysis = result == null ? null : result.decisionDeltaAnalysis();
        if (analysis == null) {
            return new RoutingExplanation.DecisionDelta(UNKNOWN, UNKNOWN, null, List.of());
        }
        CandidateDecisionDeltaResponse comparison = analysis.comparison();
        List<RoutingExplanation.FactorDelta> factors = analysis.factorDeltas().stream()
                .filter(factor -> factor != null)
                .sorted(Comparator.comparing(ScoreFactorDeltaResponse::factorName,
                        Comparator.nullsLast(String::compareTo)))
                .map(factor -> new RoutingExplanation.FactorDelta(
                        valueOrUnknown(factor.factorName()),
                        finiteOrNull(factor.selectedCandidateContribution()),
                        finiteOrNull(factor.alternativeCandidateContribution()),
                        finiteOrNull(factor.contributionDelta())))
                .toList();
        return new RoutingExplanation.DecisionDelta(
                valueOrUnknown(analysis.status()),
                valueOrUnknown(comparison == null ? null : comparison.closestAlternativeCandidateId()),
                finiteOrNull(comparison == null ? null : comparison.finalScoreGap()),
                factors);
    }

    private static List<RoutingExplanation.CounterfactualWeightScenario> counterfactualWeightScenarios(
            RoutingExplanation.DecisionDelta delta) {
        List<RoutingExplanation.CounterfactualWeightScenario> scenarios = new ArrayList<>();
        for (RoutingExplanation.FactorDelta factor : delta.factors()) {
            if (factor.selectedContribution() == null
                    || factor.alternativeContribution() == null
                    || factor.contributionDelta() == null
                    || factor.contributionDelta() == 0.0d) {
                continue;
            }
            scenarios.add(weightScenario(delta, factor, -10));
            scenarios.add(weightScenario(delta, factor, 10));
        }
        return List.copyOf(scenarios);
    }

    private static RoutingExplanation.CounterfactualWeightScenario weightScenario(
            RoutingExplanation.DecisionDelta delta,
            RoutingExplanation.FactorDelta factor,
            int shiftPercent) {
        double multiplier = 1.0d + shiftPercent / 100.0d;
        double adjustedSelected = round(factor.selectedContribution() * multiplier);
        double adjustedAlternative = round(factor.alternativeContribution() * multiplier);
        double adjustedDelta = round(adjustedSelected - adjustedAlternative);
        Double projectedGap = delta.finalScoreGap() == null
                ? null
                : round(delta.finalScoreGap() + adjustedDelta - factor.contributionDelta());
        return new RoutingExplanation.CounterfactualWeightScenario(
                factor.factorName(),
                shiftPercent,
                adjustedSelected,
                adjustedAlternative,
                adjustedDelta,
                projectedGap);
    }

    private static Double finiteOrNull(Double value) {
        return value == null || !Double.isFinite(value) ? null : value;
    }

    private static double round(double value) {
        return Math.round(value * 1_000_000.0d) / 1_000_000.0d;
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? UNKNOWN : value.trim();
    }
}
