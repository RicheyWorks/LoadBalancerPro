package com.richmond423.loadbalancerpro.core;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

public final class WeightedLeastConnectionsRoutingStrategy implements RoutingStrategy {
    public static final String STRATEGY_NAME = "WEIGHTED_LEAST_CONNECTIONS";

    private final Clock clock;

    public WeightedLeastConnectionsRoutingStrategy() {
        this(Clock.systemUTC());
    }

    WeightedLeastConnectionsRoutingStrategy(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public RoutingStrategyId id() {
        return RoutingStrategyId.WEIGHTED_LEAST_CONNECTIONS;
    }

    @Override
    public RoutingDecision choose(List<ServerStateVector> servers) {
        Objects.requireNonNull(servers, "servers cannot be null");
        List<ServerStateVector> eligible = servers.stream()
                .filter(Objects::nonNull)
                .filter(ServerStateVector::healthy)
                .filter(state -> state.weight() > 0.0)
                .sorted(Comparator.comparing(ServerStateVector::serverId))
                .toList();
        if (eligible.isEmpty()) {
            return noCandidateDecision(
                    "No healthy eligible servers with positive routing weight were available.");
        }

        Map<String, ServerScoreBreakdown> breakdowns = scoreCandidateBreakdowns(eligible);
        Map<String, Double> scores = scoresFromBreakdowns(breakdowns);
        ServerStateVector chosen = eligible.stream()
                .min(Comparator.comparingDouble((ServerStateVector state) -> scores.get(state.serverId()))
                        .thenComparing(ServerStateVector::serverId))
                .orElseThrow();

        RoutingDecisionExplanation explanation = new RoutingDecisionExplanation(
                STRATEGY_NAME,
                eligible.stream().map(ServerStateVector::serverId).toList(),
                Optional.of(chosen.serverId()),
                scores,
                factorContributionsFromBreakdowns(breakdowns),
                reasonForChoice(chosen, eligible, scores),
                Instant.now(clock));
        return new RoutingDecision(Optional.of(chosen), explanation);
    }

    private RoutingDecision noCandidateDecision(String reason) {
        RoutingDecisionExplanation explanation = new RoutingDecisionExplanation(
                STRATEGY_NAME, List.of(), Optional.empty(), Map.of(), reason, Instant.now(clock));
        return new RoutingDecision(Optional.empty(), explanation);
    }

    private Map<String, ServerScoreBreakdown> scoreCandidateBreakdowns(
            List<ServerStateVector> candidates) {
        Map<String, ServerScoreBreakdown> breakdowns = new LinkedHashMap<>();
        for (ServerStateVector candidate : candidates) {
            breakdowns.put(candidate.serverId(), scoreBreakdown(candidate));
        }
        return breakdowns;
    }

    private ServerScoreBreakdown scoreBreakdown(ServerStateVector state) {
        double effectiveRoutingWeight = effectiveWeight(state.weight());
        double score = state.inFlightRequestCount() / effectiveRoutingWeight;
        ScoreFactorContribution contribution = new ScoreFactorContribution(
                "weightedConnectionPressure",
                "inFlightRequestCount=" + state.inFlightRequestCount()
                        + ", effectiveRoutingWeight=" + formatScore(effectiveRoutingWeight),
                "inFlightRequestCount / effectiveRoutingWeight",
                score > 0.0
                        ? ScoreFactorDirection.WEAKENS_SELECTION
                        : ScoreFactorDirection.NEUTRAL,
                "contribution = inFlightRequestCount / effectiveRoutingWeight = "
                        + formatScore(score),
                OptionalDouble.of(score),
                ScoreFactorExactness.EXACT_FROM_STRATEGY_MODEL,
                "This is the complete weighted least-connections score; lower score wins.",
                "Exact for the returned local state vector and strategy formula only; no production proof.");
        return new ServerScoreBreakdown(state.serverId(), score, List.of(contribution));
    }

    private Map<String, Double> scoresFromBreakdowns(
            Map<String, ServerScoreBreakdown> breakdowns) {
        Map<String, Double> scores = new LinkedHashMap<>();
        breakdowns.forEach((candidateId, breakdown) ->
                scores.put(candidateId, breakdown.totalScore()));
        return scores;
    }

    private Map<String, List<ScoreFactorContribution>> factorContributionsFromBreakdowns(
            Map<String, ServerScoreBreakdown> breakdowns) {
        Map<String, List<ScoreFactorContribution>> contributions = new LinkedHashMap<>();
        breakdowns.forEach((candidateId, breakdown) ->
                contributions.put(candidateId, breakdown.factorContributions()));
        return contributions;
    }

    private double effectiveWeight(double weight) {
        return weight;
    }

    private String reasonForChoice(ServerStateVector chosen,
                                   List<ServerStateVector> candidates,
                                   Map<String, Double> scores) {
        if (candidates.size() == 1) {
            return "Chose " + chosen.serverId() + " because it was the only healthy candidate with weighted "
                    + "least-connections score " + formatScore(scores.get(chosen.serverId())) + ".";
        }
        return "Chose " + chosen.serverId() + " because its weighted least-connections score "
                + formatScore(scores.get(chosen.serverId())) + " was the lowest across "
                + candidates.size() + " healthy candidates.";
    }

    private String formatScore(double score) {
        return String.format(Locale.ROOT, "%.3f", score);
    }
}
