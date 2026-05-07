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

public final class WeightedLeastConnectionsRoutingStrategy implements RoutingStrategy {
    public static final String STRATEGY_NAME = "WEIGHTED_LEAST_CONNECTIONS";

    private static final double DEFAULT_WEIGHT = 1.0;
    private static final double MIN_POSITIVE_WEIGHT = 0.1;

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
                .sorted(Comparator.comparing(ServerStateVector::serverId))
                .toList();
        if (eligible.isEmpty()) {
            return noCandidateDecision("No healthy eligible servers were available.");
        }

        Map<String, Double> scores = scoreCandidates(eligible);
        ServerStateVector chosen = eligible.stream()
                .min(Comparator.comparingDouble((ServerStateVector state) -> scores.get(state.serverId()))
                        .thenComparing(ServerStateVector::serverId))
                .orElseThrow();

        RoutingDecisionExplanation explanation = new RoutingDecisionExplanation(
                STRATEGY_NAME,
                eligible.stream().map(ServerStateVector::serverId).toList(),
                Optional.of(chosen.serverId()),
                scores,
                reasonForChoice(chosen, eligible, scores),
                Instant.now(clock));
        return new RoutingDecision(Optional.of(chosen), explanation);
    }

    private RoutingDecision noCandidateDecision(String reason) {
        RoutingDecisionExplanation explanation = new RoutingDecisionExplanation(
                STRATEGY_NAME, List.of(), Optional.empty(), Map.of(), reason, Instant.now(clock));
        return new RoutingDecision(Optional.empty(), explanation);
    }

    private Map<String, Double> scoreCandidates(List<ServerStateVector> candidates) {
        Map<String, Double> scores = new LinkedHashMap<>();
        for (ServerStateVector candidate : candidates) {
            scores.put(candidate.serverId(), score(candidate));
        }
        return scores;
    }

    private double score(ServerStateVector state) {
        return state.inFlightRequestCount() / effectiveWeight(state.weight());
    }

    private double effectiveWeight(double weight) {
        if (weight == 0.0) {
            return DEFAULT_WEIGHT;
        }
        return Math.max(MIN_POSITIVE_WEIGHT, weight);
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
