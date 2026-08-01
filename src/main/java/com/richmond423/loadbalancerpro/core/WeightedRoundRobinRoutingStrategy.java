package com.richmond423.loadbalancerpro.core;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

public final class WeightedRoundRobinRoutingStrategy implements RoutingStrategy {
    public static final String STRATEGY_NAME = "WEIGHTED_ROUND_ROBIN";

    private final Map<String, Double> currentWeights = new LinkedHashMap<>();
    private final Clock clock;

    public WeightedRoundRobinRoutingStrategy() {
        this(Clock.systemUTC());
    }

    WeightedRoundRobinRoutingStrategy(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public RoutingStrategyId id() {
        return RoutingStrategyId.WEIGHTED_ROUND_ROBIN;
    }

    @Override
    public synchronized RoutingDecision choose(List<ServerStateVector> servers) {
        Objects.requireNonNull(servers, "servers cannot be null");
        List<ServerStateVector> eligible = servers.stream()
                .filter(Objects::nonNull)
                .filter(ServerStateVector::healthy)
                .filter(state -> state.weight() > 0.0)
                .toList();
        if (eligible.isEmpty()) {
            currentWeights.clear();
            return noCandidateDecision(
                    "No healthy eligible servers with positive routing weight were available.");
        }

        retainOnly(eligible);
        Map<String, Double> effectiveWeights = effectiveWeights(eligible);
        Map<String, Double> carriedWeights = new LinkedHashMap<>();
        Map<String, Double> selectionScores = new LinkedHashMap<>();
        double totalWeight = effectiveWeights.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        ServerStateVector chosen = null;
        double highestCurrentWeight = Double.NEGATIVE_INFINITY;
        for (ServerStateVector candidate : eligible) {
            String serverId = candidate.serverId();
            double carriedWeight = currentWeights.getOrDefault(serverId, 0.0);
            double currentWeight = carriedWeight
                    + effectiveWeights.get(serverId);
            carriedWeights.put(serverId, carriedWeight);
            selectionScores.put(serverId, currentWeight);
            currentWeights.put(serverId, currentWeight);
            if (chosen == null || currentWeight > highestCurrentWeight) {
                chosen = candidate;
                highestCurrentWeight = currentWeight;
            }
        }

        currentWeights.computeIfPresent(chosen.serverId(), (serverId, currentWeight) -> currentWeight - totalWeight);

        RoutingDecisionExplanation explanation = new RoutingDecisionExplanation(
                STRATEGY_NAME,
                eligible.stream().map(ServerStateVector::serverId).toList(),
                Optional.of(chosen.serverId()),
                selectionScores,
                factorContributions(eligible, effectiveWeights, carriedWeights),
                reasonForChoice(chosen, eligible.size(), effectiveWeights, selectionScores, totalWeight),
                Instant.now(clock));
        return new RoutingDecision(Optional.of(chosen), explanation);
    }

    private RoutingDecision noCandidateDecision(String reason) {
        RoutingDecisionExplanation explanation = new RoutingDecisionExplanation(
                STRATEGY_NAME, List.of(), Optional.empty(), Map.of(), reason, Instant.now(clock));
        return new RoutingDecision(Optional.empty(), explanation);
    }

    private void retainOnly(List<ServerStateVector> eligible) {
        Set<String> activeServerIds = eligible.stream()
                .map(ServerStateVector::serverId)
                .collect(Collectors.toSet());
        currentWeights.keySet().removeIf(serverId -> !activeServerIds.contains(serverId));
    }

    private Map<String, Double> effectiveWeights(List<ServerStateVector> candidates) {
        Map<String, Double> weights = new LinkedHashMap<>();
        for (ServerStateVector candidate : candidates) {
            weights.put(candidate.serverId(), effectiveWeight(candidate.weight()));
        }
        return weights;
    }

    private double effectiveWeight(double weight) {
        return weight;
    }

    private Map<String, List<ScoreFactorContribution>> factorContributions(
            List<ServerStateVector> candidates,
            Map<String, Double> effectiveWeights,
            Map<String, Double> carriedWeights) {
        Map<String, List<ScoreFactorContribution>> contributions = new LinkedHashMap<>();
        for (ServerStateVector candidate : candidates) {
            double effectiveRoutingWeight = effectiveWeights.get(candidate.serverId());
            double carriedWeight = carriedWeights.get(candidate.serverId());
            List<ScoreFactorContribution> candidateContributions = new java.util.ArrayList<>();
            if (carriedWeight != 0.0d) {
                candidateContributions.add(new ScoreFactorContribution(
                        "smoothWeightCarry",
                        "accumulatedWeightBeforeThisCycle=" + formatWeight(carriedWeight),
                        "state carried from prior smooth weighted round-robin cycles",
                        carriedWeight > 0.0d
                                ? ScoreFactorDirection.SUPPORTS_SELECTION
                                : ScoreFactorDirection.WEAKENS_SELECTION,
                        "contribution = accumulatedWeightBeforeThisCycle = "
                                + formatWeight(carriedWeight),
                        OptionalDouble.of(carriedWeight),
                        ScoreFactorExactness.EXACT_FROM_STRATEGY_MODEL,
                        "This is the route-owned smooth weighted round-robin carry used by this selection cycle.",
                        "Exact for this in-process route strategy instance and cycle only; it resets on route "
                                + "replacement or process restart and is not distributed state."));
            }
            candidateContributions.add(new ScoreFactorContribution(
                    "effectiveRoutingWeight",
                    "configuredRoutingWeight=" + formatWeight(candidate.weight()),
                    "smooth weighted round-robin adds the effective routing weight each selection cycle",
                    ScoreFactorDirection.SUPPORTS_SELECTION,
                    "contribution = effectiveRoutingWeight = "
                            + formatWeight(effectiveRoutingWeight),
                    OptionalDouble.of(effectiveRoutingWeight),
                    ScoreFactorExactness.EXACT_FROM_STRATEGY_MODEL,
                    "The effective routing weight is added to prior carry; the candidate with the highest resulting "
                            + "selection score wins this cycle.",
                    "Exact for this selection cycle; the live route carry is process-local and is not distributed."));
            contributions.put(candidate.serverId(), List.copyOf(candidateContributions));
        }
        return contributions;
    }

    private String reasonForChoice(ServerStateVector chosen,
                                   int candidateCount,
                                   Map<String, Double> effectiveWeights,
                                   Map<String, Double> selectionScores,
                                   double totalWeight) {
        double chosenWeight = effectiveWeights.get(chosen.serverId());
        double chosenScore = selectionScores.get(chosen.serverId());
        if (candidateCount == 1) {
            return "Chose " + chosen.serverId() + " because it was the only healthy candidate with effective "
                    + "routing weight " + formatWeight(chosenWeight) + ".";
        }
        return "Chose " + chosen.serverId() + " using smooth weighted round-robin with effective routing weight "
                + formatWeight(chosenWeight) + ", accumulated selection score " + formatWeight(chosenScore)
                + ", and total effective weight " + formatWeight(totalWeight) + " across "
                + candidateCount + " healthy candidates.";
    }

    private String formatWeight(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
