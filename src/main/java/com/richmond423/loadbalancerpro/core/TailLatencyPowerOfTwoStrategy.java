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
import java.util.Random;
import java.util.stream.Collectors;

public final class TailLatencyPowerOfTwoStrategy implements RoutingStrategy {
    public static final String STRATEGY_NAME = "TAIL_LATENCY_POWER_OF_TWO";
    private static final double SCORE_HYSTERESIS_MARGIN = 2.5;
    private static final double TAIL_PRESSURE_HYSTERESIS_MARGIN = 0.05;

    private final ServerScoreCalculator scoreCalculator;
    private final Random random;
    private final Clock clock;

    public TailLatencyPowerOfTwoStrategy() {
        this(new ServerScoreCalculator(), new Random(), Clock.systemUTC());
    }

    public TailLatencyPowerOfTwoStrategy(ServerScoreCalculator scoreCalculator, Random random, Clock clock) {
        this.scoreCalculator = Objects.requireNonNull(scoreCalculator, "scoreCalculator cannot be null");
        this.random = Objects.requireNonNull(random, "random cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public RoutingStrategyId id() {
        return RoutingStrategyId.TAIL_LATENCY_POWER_OF_TWO;
    }

    @Override
    public RoutingDecision choose(List<ServerStateVector> servers) {
        Objects.requireNonNull(servers, "servers cannot be null");
        List<ServerStateVector> eligible = servers.stream()
                .filter(Objects::nonNull)
                .filter(ServerStateVector::healthy)
                .toList();
        if (eligible.isEmpty()) {
            return noCandidateDecision("No healthy eligible servers were available.");
        }

        List<ServerStateVector> candidates = sampleCandidates(eligible);
        Map<String, ServerScoreBreakdown> breakdowns = scoreCandidateBreakdowns(candidates);
        Map<String, Double> scores = scoresFromBreakdowns(breakdowns);
        CandidateSelection selection = selectCandidate(candidates, scores);
        ServerStateVector chosen = selection.chosen();

        String reason = reasonForChoice(selection, candidates, scores, breakdowns);
        RoutingDecisionExplanation explanation = new RoutingDecisionExplanation(
                STRATEGY_NAME,
                candidates.stream().map(ServerStateVector::serverId).toList(),
                Optional.of(chosen.serverId()),
                scores,
                reason,
                Instant.now(clock));
        return new RoutingDecision(Optional.of(chosen), explanation);
    }

    private RoutingDecision noCandidateDecision(String reason) {
        RoutingDecisionExplanation explanation = new RoutingDecisionExplanation(
                STRATEGY_NAME, List.of(), Optional.empty(), Map.of(), reason, Instant.now(clock));
        return new RoutingDecision(Optional.empty(), explanation);
    }

    private List<ServerStateVector> sampleCandidates(List<ServerStateVector> eligible) {
        if (eligible.size() <= 2) {
            return List.copyOf(eligible);
        }
        int firstIndex = random.nextInt(eligible.size());
        int secondIndex = random.nextInt(eligible.size() - 1);
        if (secondIndex >= firstIndex) {
            secondIndex++;
        }
        return List.of(eligible.get(firstIndex), eligible.get(secondIndex));
    }

    private Map<String, ServerScoreBreakdown> scoreCandidateBreakdowns(List<ServerStateVector> candidates) {
        Map<String, ServerScoreBreakdown> breakdowns = new LinkedHashMap<>();
        for (ServerStateVector candidate : candidates) {
            breakdowns.put(candidate.serverId(), scoreCalculator.scoreBreakdown(candidate));
        }
        return breakdowns;
    }

    private Map<String, Double> scoresFromBreakdowns(Map<String, ServerScoreBreakdown> breakdowns) {
        Map<String, Double> scores = new LinkedHashMap<>();
        for (Map.Entry<String, ServerScoreBreakdown> entry : breakdowns.entrySet()) {
            scores.put(entry.getKey(), entry.getValue().totalScore());
        }
        return scores;
    }

    private CandidateSelection selectCandidate(List<ServerStateVector> candidates, Map<String, Double> scores) {
        if (candidates.size() == 1) {
            ServerStateVector only = candidates.get(0);
            return new CandidateSelection(only, only, false, 0.0, 0.0);
        }

        List<ServerStateVector> sortedByScore = candidates.stream()
                .sorted(Comparator.comparingDouble((ServerStateVector state) -> scores.get(state.serverId()))
                        .thenComparing(ServerStateVector::serverId))
                .toList();
        ServerStateVector scoreWinner = sortedByScore.get(0);
        ServerStateVector closestAlternative = sortedByScore.get(1);
        double scoreDelta = Math.max(0.0,
                scores.get(closestAlternative.serverId()) - scores.get(scoreWinner.serverId()));
        double tailPressureDelta = Math.abs(scoreWinner.effectiveTailLatencyPressure()
                - closestAlternative.effectiveTailLatencyPressure());

        if (scoreDelta <= SCORE_HYSTERESIS_MARGIN
                && tailPressureDelta <= TAIL_PRESSURE_HYSTERESIS_MARGIN) {
            ServerStateVector stableChoice = List.of(scoreWinner, closestAlternative).stream()
                    .min(Comparator.comparing(ServerStateVector::serverId))
                    .orElse(scoreWinner);
            ServerStateVector alternative = stableChoice.serverId().equals(scoreWinner.serverId())
                    ? closestAlternative : scoreWinner;
            return new CandidateSelection(stableChoice, alternative, true, scoreDelta, tailPressureDelta);
        }

        return new CandidateSelection(scoreWinner, closestAlternative, false, scoreDelta, tailPressureDelta);
    }

    private String reasonForChoice(CandidateSelection selection,
                                   List<ServerStateVector> candidates,
                                   Map<String, Double> scores,
                                   Map<String, ServerScoreBreakdown> breakdowns) {
        ServerStateVector chosen = selection.chosen();
        if (candidates.size() == 1) {
            return "Chose " + chosen.serverId() + " because it was the only healthy candidate with score "
                    + formatScore(scores.get(chosen.serverId()))
                    + penaltySummarySentence("Primary penalty factors", breakdowns.get(chosen.serverId()));
        }
        ServerStateVector other = selection.alternative();
        if (selection.hysteresisApplied()) {
            return "Chose " + chosen.serverId()
                    + " because candidate scores and effective tail-latency pressure were within the "
                    + "anti-flapping band; stable server-id tie-break selected " + chosen.serverId()
                    + " over " + other.serverId()
                    + " (scoreDelta=" + formatScore(selection.scoreDelta())
                    + ", scoreMargin=" + formatScore(SCORE_HYSTERESIS_MARGIN)
                    + ", effectiveTailLatencyPressureDelta=" + formatScore(selection.tailPressureDelta())
                    + ", tailPressureMargin=" + formatScore(TAIL_PRESSURE_HYSTERESIS_MARGIN)
                    + ", " + chosen.serverId() + " score " + formatScore(scores.get(chosen.serverId()))
                    + ", " + other.serverId() + " score " + formatScore(scores.get(other.serverId())) + ")"
                    + penaltySummarySentence("Primary penalty factors for " + other.serverId(),
                    breakdowns.get(other.serverId()));
        }
        return "Chose " + chosen.serverId() + " because its score "
                + formatScore(scores.get(chosen.serverId())) + " was lower than "
                + other.serverId() + " score " + formatScore(scores.get(other.serverId()))
                + penaltySummarySentence("Primary penalty factors for " + other.serverId(),
                breakdowns.get(other.serverId()));
    }

    private String penaltySummarySentence(String label, ServerScoreBreakdown breakdown) {
        if (breakdown == null) {
            return ".";
        }
        List<String> factorNames = breakdown.topPenaltyFactorNames(3);
        if (factorNames.isEmpty()) {
            return ".";
        }
        return ". " + label + ": " + factorNames.stream().collect(Collectors.joining(", ")) + ".";
    }

    private String formatScore(double score) {
        return String.format(Locale.ROOT, "%.3f", score);
    }

    private record CandidateSelection(
            ServerStateVector chosen,
            ServerStateVector alternative,
            boolean hysteresisApplied,
            double scoreDelta,
            double tailPressureDelta) {
    }
}
