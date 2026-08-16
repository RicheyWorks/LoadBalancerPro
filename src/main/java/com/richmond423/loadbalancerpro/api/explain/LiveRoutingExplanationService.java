package com.richmond423.loadbalancerpro.api.explain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

import com.richmond423.loadbalancerpro.api.proxy.LiveRoutingDecisionRecord;

/** Builds explanations only from privacy-bounded decisions captured by the production proxy. */
@Service
public final class LiveRoutingExplanationService {
    private static final String UNKNOWN = "UNKNOWN";
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

    public LiveRoutingDecisionExplanation explain(LiveRoutingDecisionRecord decision) {
        Objects.requireNonNull(decision, "decision cannot be null");
        LiveRoutingDecisionRecord.SelectionEvidence evidence = decision.selectionEvidence();
        List<RoutingExplanation.CandidateFactors> candidates = candidateFactors(decision, evidence);
        RoutingExplanation.DominantFactors dominantFactors = dominantFactors(candidates);
        RoutingExplanation.DecisionDelta decisionDelta = decisionDelta(decision, evidence, candidates);
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
                changeThresholds(evidence, decisionDelta),
                LIVE_BOUNDARY_NOTE);
    }

    private static List<RoutingExplanation.CandidateFactors> candidateFactors(
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

    private static RoutingExplanation.DominantFactors dominantFactors(
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
        return new RoutingExplanation.DominantFactors(available ? "AVAILABLE" : UNKNOWN, rows);
    }

    private static RoutingExplanation.FactorContribution largestFactor(
            List<RoutingExplanation.FactorContribution> factors,
            boolean supportOnly,
            boolean penaltyOnly) {
        return factors.stream()
                .filter(factor -> !supportOnly || "SUPPORTS_SELECTION".equals(factor.direction()))
                .filter(factor -> !penaltyOnly || "WEAKENS_SELECTION".equals(factor.direction()))
                .max(Comparator
                        .comparingDouble((RoutingExplanation.FactorContribution factor) ->
                                Math.abs(factor.contributionValue()))
                        .thenComparing(RoutingExplanation.FactorContribution::factorName,
                                Comparator.reverseOrder()))
                .orElse(null);
    }

    private static RoutingExplanation.DecisionDelta decisionDelta(
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
        Map<String, RoutingExplanation.FactorContribution> selectedFactors = factorsByName(
                candidates, decision.chosenUpstreamId());
        Map<String, RoutingExplanation.FactorContribution> alternativeFactors = factorsByName(
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
        return new RoutingExplanation.DecisionDelta(
                deltas.isEmpty() || omitted ? "PARTIAL" : "AVAILABLE",
                alternativeId,
                round(selectedScore - evidence.scores().get(alternativeId)),
                deltas);
    }

    private static Map<String, RoutingExplanation.FactorContribution> factorsByName(
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

    private static List<LiveRoutingDecisionExplanation.DecisionChangeThreshold> changeThresholds(
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
