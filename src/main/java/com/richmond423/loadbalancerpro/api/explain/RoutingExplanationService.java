package com.richmond423.loadbalancerpro.api.explain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

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
import com.richmond423.loadbalancerpro.api.proxy.LiveRoutingDecisionRecord;
import org.springframework.stereotype.Service;

@Service
public final class RoutingExplanationService {
    private static final String UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Read-only, simulation-only arithmetic over evidence returned by the routing comparison. "
                    + "It does not execute replay, rerun a strategy, mutate weights or traffic, call external "
                    + "systems, persist evidence, or prove production behavior.";
    private static final String LIVE_BOUNDARY_NOTE =
            "Read-only arithmetic over one bounded process-local proxy decision record and the strategy evidence "
                    + "captured with its actual selection. It retains no request path, query, method, body, headers, "
                    + "cookies, routing key, affinity value, upstream URL, or credential; it does not rerun routing, "
                    + "execute replay, mutate weights or traffic, call external systems, persist durable evidence, "
                    + "or prove production readiness, certification, SLOs, or multi-instance behavior.";
    private static final String CHANGE_THRESHOLD_BOUNDARY =
            "The threshold is additive score arithmetic over captured factor contributions. Crossing a tie in the "
                    + "same direction changes only the captured score ordering; it does not rerun stateful, sampled, "
                    + "positional, keyed, or affinity selection and is not an actuation recommendation.";

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
                valueOrUnknown(result == null ? null : result.decisionFingerprint()),
                candidates,
                dominantFactors,
                decisionDelta,
                counterfactualWeightScenarios(decisionDelta),
                BOUNDARY_NOTE);
    }

    public LiveRoutingDecisionExplanation explain(LiveRoutingDecisionRecord decision) {
        Objects.requireNonNull(decision, "decision cannot be null");
        LiveRoutingDecisionRecord.SelectionEvidence evidence = decision.selectionEvidence();
        List<RoutingExplanation.CandidateFactors> candidates = liveCandidateFactors(decision, evidence);
        RoutingExplanation.DominantFactors dominantFactors = liveDominantFactors(candidates);
        RoutingExplanation.DecisionDelta decisionDelta = liveDecisionDelta(decision, evidence, candidates);
        RoutingExplanation analysis = new RoutingExplanation(
                true,
                false,
                RoutingExplanation.CONTRACT_VERSION,
                decision.strategy(),
                evidence.status(),
                decision.chosenUpstreamId(),
                valueOrUnknown(evidence.decisionFingerprint()),
                candidates,
                dominantFactors,
                decisionDelta,
                counterfactualWeightScenarios(decisionDelta),
                LIVE_BOUNDARY_NOTE);
        return new LiveRoutingDecisionExplanation(
                true,
                true,
                LiveRoutingDecisionExplanation.PROCESS_LOCAL,
                LiveRoutingDecisionExplanation.CONTRACT_VERSION,
                decision.decisionId(),
                decision.capturedAt(),
                decision.configurationGeneration(),
                decision.routeName(),
                decision.strategy(),
                decision.attempt(),
                decision.selectionSource(),
                decision.chosenUpstreamId(),
                decision.candidates().stream()
                        .map(LiveRoutingDecisionRecord.CandidateState::upstreamId)
                        .toList(),
                decision.candidates(),
                evidence.consideredCandidateIds(),
                decision.responseStatus(),
                decision.latencyMillis(),
                decision.retriable(),
                decision.outcome(),
                evidence.status(),
                evidence.reason(),
                evidence.scorePreference(),
                analysis,
                liveChangeThresholds(evidence, decisionDelta),
                LIVE_BOUNDARY_NOTE);
    }

    private static List<RoutingExplanation.CandidateFactors> liveCandidateFactors(
            LiveRoutingDecisionRecord decision,
            LiveRoutingDecisionRecord.SelectionEvidence evidence) {
        List<String> consideredIds = evidence.consideredCandidateIds().isEmpty()
                ? decision.candidates().stream()
                        .map(LiveRoutingDecisionRecord.CandidateState::upstreamId)
                        .toList()
                : evidence.consideredCandidateIds();
        return consideredIds.stream()
                .map(candidateId -> new RoutingExplanation.CandidateFactors(
                        candidateId,
                        candidateId.equals(decision.chosenUpstreamId()),
                        evidence.factorContributions().getOrDefault(candidateId, List.of()).stream()
                                .map(factor -> new RoutingExplanation.FactorContribution(
                                        factor.factorName(),
                                        finiteOrNull(factor.contributionValue()),
                                        factor.direction(),
                                        factor.exactness()))
                                .sorted(Comparator.comparing(RoutingExplanation.FactorContribution::factorName))
                                .toList()))
                .toList();
    }

    private static RoutingExplanation.DominantFactors liveDominantFactors(
            List<RoutingExplanation.CandidateFactors> candidates) {
        List<RoutingExplanation.CandidateDominantFactor> rows = candidates.stream()
                .map(candidate -> {
                    List<RoutingExplanation.FactorContribution> finite = candidate.factors().stream()
                            .filter(factor -> factor.contributionValue() != null)
                            .toList();
                    return new RoutingExplanation.CandidateDominantFactor(
                            candidate.candidateId(),
                            candidate.selected(),
                            largestFactor(finite, true, false),
                            largestFactor(finite, false, true),
                            largestFactor(finite, false, false));
                })
                .toList();
        boolean available = rows.stream().anyMatch(row -> row.largestAbsoluteImpact() != null);
        return new RoutingExplanation.DominantFactors(available ? "AVAILABLE" : "UNKNOWN", rows);
    }

    private static RoutingExplanation.FactorContribution largestFactor(
            List<RoutingExplanation.FactorContribution> factors,
            boolean supportOnly,
            boolean penaltyOnly) {
        return factors.stream()
                .filter(factor -> !supportOnly || supportsSelection(factor))
                .filter(factor -> !penaltyOnly || weakensSelection(factor))
                .max(Comparator
                        .comparingDouble((RoutingExplanation.FactorContribution factor) ->
                                Math.abs(factor.contributionValue()))
                        .thenComparing(RoutingExplanation.FactorContribution::factorName,
                                Comparator.reverseOrder()))
                .orElse(null);
    }

    private static boolean supportsSelection(RoutingExplanation.FactorContribution factor) {
        return "SUPPORTS_SELECTION".equals(factor.direction());
    }

    private static boolean weakensSelection(RoutingExplanation.FactorContribution factor) {
        return "WEAKENS_SELECTION".equals(factor.direction());
    }

    private static RoutingExplanation.DecisionDelta liveDecisionDelta(
            LiveRoutingDecisionRecord decision,
            LiveRoutingDecisionRecord.SelectionEvidence evidence,
            List<RoutingExplanation.CandidateFactors> candidates) {
        if (!("LOWER_WINS".equals(evidence.scorePreference())
                || "HIGHER_WINS".equals(evidence.scorePreference()))) {
            return new RoutingExplanation.DecisionDelta("NOT_APPLICABLE", UNKNOWN, null, List.of());
        }
        Double selectedScore = finiteOrNull(evidence.scores().get(decision.chosenUpstreamId()));
        if (selectedScore == null) {
            return new RoutingExplanation.DecisionDelta(UNKNOWN, UNKNOWN, null, List.of());
        }
        String alternativeId = evidence.consideredCandidateIds().stream()
                .filter(candidateId -> !candidateId.equals(decision.chosenUpstreamId()))
                .filter(candidateId -> finiteOrNull(evidence.scores().get(candidateId)) != null)
                .min(Comparator
                        .comparingDouble((String candidateId) ->
                                Math.abs(selectedScore - evidence.scores().get(candidateId)))
                        .thenComparing(candidateId -> candidateId))
                .orElse(null);
        if (alternativeId == null) {
            return new RoutingExplanation.DecisionDelta(UNKNOWN, UNKNOWN, null, List.of());
        }
        Map<String, RoutingExplanation.FactorContribution> selectedFactors = liveFactorsByName(
                candidates, decision.chosenUpstreamId());
        Map<String, RoutingExplanation.FactorContribution> alternativeFactors = liveFactorsByName(
                candidates, alternativeId);
        Set<String> factorNames = new TreeSet<>();
        factorNames.addAll(selectedFactors.keySet());
        factorNames.addAll(alternativeFactors.keySet());
        List<RoutingExplanation.FactorDelta> deltas = factorNames.stream()
                .filter(name -> selectedFactors.containsKey(name) && alternativeFactors.containsKey(name))
                .map(name -> {
                    double selected = selectedFactors.get(name).contributionValue();
                    double alternative = alternativeFactors.get(name).contributionValue();
                    return new RoutingExplanation.FactorDelta(
                            name,
                            selected,
                            alternative,
                            round(selected - alternative));
                })
                .sorted(Comparator
                        .comparingDouble((RoutingExplanation.FactorDelta delta) ->
                                Math.abs(delta.contributionDelta()))
                        .reversed()
                        .thenComparing(RoutingExplanation.FactorDelta::factorName))
                .toList();
        boolean omitted = factorNames.size() != deltas.size();
        String status = deltas.isEmpty() || omitted ? "PARTIAL" : "AVAILABLE";
        return new RoutingExplanation.DecisionDelta(
                status,
                alternativeId,
                round(selectedScore - evidence.scores().get(alternativeId)),
                deltas);
    }

    private static Map<String, RoutingExplanation.FactorContribution> liveFactorsByName(
            List<RoutingExplanation.CandidateFactors> candidates,
            String candidateId) {
        Map<String, RoutingExplanation.FactorContribution> byName = new LinkedHashMap<>();
        candidates.stream()
                .filter(candidate -> candidate.candidateId().equals(candidateId))
                .findFirst()
                .stream()
                .flatMap(candidate -> candidate.factors().stream())
                .filter(factor -> factor.contributionValue() != null)
                .forEach(factor -> byName.putIfAbsent(factor.factorName(), factor));
        return Map.copyOf(byName);
    }

    private static List<LiveRoutingDecisionExplanation.DecisionChangeThreshold> liveChangeThresholds(
            LiveRoutingDecisionRecord.SelectionEvidence evidence,
            RoutingExplanation.DecisionDelta delta) {
        if (delta.finalScoreGap() == null || delta.factors().isEmpty()) {
            return List.of();
        }
        return delta.factors().stream()
                .map(factor -> {
                    double factorDelta = factor.contributionDelta();
                    Double sharedWeightShift = factorDelta == 0.0d
                            ? null
                            : finiteOrNull(round(-delta.finalScoreGap() / factorDelta * 100.0d));
                    return new LiveRoutingDecisionExplanation.DecisionChangeThreshold(
                            factor.factorName(),
                            delta.closestAlternativeCandidateId(),
                            evidence.scorePreference(),
                            round(-delta.finalScoreGap()),
                            round(delta.finalScoreGap()),
                            sharedWeightShift,
                            CHANGE_THRESHOLD_BOUNDARY);
                })
                .toList();
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
