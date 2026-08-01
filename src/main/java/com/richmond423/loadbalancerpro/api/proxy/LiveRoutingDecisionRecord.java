package com.richmond423.loadbalancerpro.api.proxy;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.richmond423.loadbalancerpro.core.RoutingDecisionExplanation;
import com.richmond423.loadbalancerpro.core.RoutingDecisionFingerprint;
import com.richmond423.loadbalancerpro.core.ScoreFactorContribution;
import com.richmond423.loadbalancerpro.core.ServerStateVector;

/**
 * Immutable, process-local evidence for one actual upstream forwarding attempt.
 */
public record LiveRoutingDecisionRecord(
        String decisionId,
        Instant capturedAt,
        long configurationGeneration,
        String routeName,
        String strategy,
        int attempt,
        String selectionSource,
        String chosenUpstreamId,
        List<CandidateState> candidates,
        @JsonIgnore SelectionEvidence selectionEvidence,
        int responseStatus,
        double latencyMillis,
        boolean retriable,
        String outcome) {

    public LiveRoutingDecisionRecord {
        decisionId = requireNonBlank(decisionId, "decisionId");
        Objects.requireNonNull(capturedAt, "capturedAt cannot be null");
        if (configurationGeneration < 1) {
            throw new IllegalArgumentException("configurationGeneration must be positive");
        }
        routeName = requireNonBlank(routeName, "routeName");
        strategy = requireNonBlank(strategy, "strategy");
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be positive");
        }
        selectionSource = requireNonBlank(selectionSource, "selectionSource");
        chosenUpstreamId = requireNonBlank(chosenUpstreamId, "chosenUpstreamId");
        candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates cannot be null"));
        boolean chosenCandidatePresent = false;
        for (CandidateState candidate : candidates) {
            if (candidate.upstreamId().equals(chosenUpstreamId)) {
                chosenCandidatePresent = true;
                break;
            }
        }
        if (!chosenCandidatePresent) {
            throw new IllegalArgumentException("chosenUpstreamId must belong to candidates");
        }
        Objects.requireNonNull(selectionEvidence, "selectionEvidence cannot be null");
        List<String> candidateIds = candidates.stream().map(CandidateState::upstreamId).toList();
        if (!candidateIds.containsAll(selectionEvidence.consideredCandidateIds())) {
            throw new IllegalArgumentException("selectionEvidence must stay within candidates");
        }
        if ("CAPTURED".equals(selectionEvidence.status())
                && !selectionEvidence.consideredCandidateIds().contains(chosenUpstreamId)) {
            throw new IllegalArgumentException("captured selectionEvidence must include chosenUpstreamId");
        }
        if (responseStatus < 100 || responseStatus > 599) {
            throw new IllegalArgumentException("responseStatus must be a valid HTTP status");
        }
        if (!Double.isFinite(latencyMillis) || latencyMillis < 0.0) {
            throw new IllegalArgumentException("latencyMillis must be finite and non-negative");
        }
        outcome = requireNonBlank(outcome, "outcome");
    }

    /**
     * Compact strategy evidence captured at the same instant as the actual selection. It is
     * retained internally for the per-decision explanation endpoint and omitted from the
     * recent-decision list so that list payloads stay bounded and compact.
     */
    public record SelectionEvidence(
            String status,
            String reason,
            String scorePreference,
            List<String> consideredCandidateIds,
            Map<String, Double> scores,
            Map<String, List<FactorContribution>> factorContributions,
            String decisionFingerprint) {

        public SelectionEvidence {
            status = requireNonBlank(status, "selectionEvidence.status");
            reason = requireNonBlank(reason, "selectionEvidence.reason");
            scorePreference = requireNonBlank(scorePreference, "selectionEvidence.scorePreference");
            consideredCandidateIds = immutableCandidateIds(consideredCandidateIds);
            scores = immutableFiniteScores(scores);
            factorContributions = immutableFactors(factorContributions);
            if (!consideredCandidateIds.containsAll(scores.keySet())
                    || !consideredCandidateIds.containsAll(factorContributions.keySet())) {
                throw new IllegalArgumentException(
                        "selectionEvidence scores and factors must stay within consideredCandidateIds");
            }
            decisionFingerprint = blankToNull(decisionFingerprint);
        }

        static SelectionEvidence capture(
                String strategy,
                String selectionSource,
                String chosenUpstreamId,
                List<CandidateState> candidates,
                RoutingDecisionExplanation explanation) {
            String source = requireNonBlank(selectionSource, "selectionSource");
            if (!"strategy".equals(source)) {
                return new SelectionEvidence(
                        "NOT_APPLICABLE",
                        "The upstream was selected by configured affinity; the affinity key and value are not retained, "
                                + "and the route strategy score model did not run for this attempt.",
                        "NOT_APPLICABLE",
                        candidates.stream().map(CandidateState::upstreamId).toList(),
                        Map.of(),
                        Map.of(),
                        null);
            }
            Objects.requireNonNull(explanation, "strategy selection explanation cannot be null");
            if (!strategy.equals(explanation.strategyUsed())) {
                throw new IllegalArgumentException("strategy selection explanation must match the route strategy");
            }
            if (!explanation.chosenServerId().filter(chosenUpstreamId::equals).isPresent()) {
                throw new IllegalArgumentException("strategy selection explanation must match chosenUpstreamId");
            }
            List<String> candidateIds = candidates.stream().map(CandidateState::upstreamId).toList();
            if (!candidateIds.containsAll(explanation.candidateServersConsidered())) {
                throw new IllegalArgumentException(
                        "strategy selection explanation contains a candidate outside the actual candidate set");
            }
            Map<String, List<FactorContribution>> factors = new LinkedHashMap<>();
            explanation.factorContributions().forEach((candidateId, contributions) ->
                    factors.put(candidateId, contributions.stream()
                            .map(FactorContribution::from)
                            .toList()));
            return new SelectionEvidence(
                    "CAPTURED",
                    explanation.reason(),
                    scorePreference(strategy),
                    explanation.candidateServersConsidered(),
                    explanation.scores(),
                    factors,
                    RoutingDecisionFingerprint.from(explanation));
        }

        private static String scorePreference(String strategy) {
            return switch (strategy) {
                case "TAIL_LATENCY_POWER_OF_TWO", "WEIGHTED_LEAST_LOAD", "WEIGHTED_LEAST_CONNECTIONS" ->
                        "LOWER_WINS";
                case "WEIGHTED_ROUND_ROBIN" -> "HIGHER_WINS";
                case "ROUND_ROBIN" -> "POSITIONAL";
                case "CONSISTENT_HASH" -> "KEYED_RING";
                default -> "UNKNOWN";
            };
        }

        private static Map<String, Double> immutableFiniteScores(Map<String, Double> source) {
            Objects.requireNonNull(source, "selectionEvidence.scores cannot be null");
            Map<String, Double> copy = new LinkedHashMap<>();
            source.forEach((candidateId, score) -> {
                String id = requireNonBlank(candidateId, "selectionEvidence.scores candidateId");
                if (score == null || !Double.isFinite(score)) {
                    throw new IllegalArgumentException("selectionEvidence scores must be finite");
                }
                copy.put(id, score);
            });
            return Collections.unmodifiableMap(copy);
        }

        private static List<String> immutableCandidateIds(List<String> source) {
            Objects.requireNonNull(source, "selectionEvidence.consideredCandidateIds cannot be null");
            List<String> copy = source.stream()
                    .map(candidateId -> requireNonBlank(
                            candidateId, "selectionEvidence.consideredCandidateIds candidateId"))
                    .toList();
            if (copy.stream().distinct().count() != copy.size()) {
                throw new IllegalArgumentException(
                        "selectionEvidence.consideredCandidateIds cannot contain duplicates");
            }
            return copy;
        }

        private static Map<String, List<FactorContribution>> immutableFactors(
                Map<String, List<FactorContribution>> source) {
            Objects.requireNonNull(source, "selectionEvidence.factorContributions cannot be null");
            Map<String, List<FactorContribution>> copy = new LinkedHashMap<>();
            source.forEach((candidateId, contributions) -> copy.put(
                    requireNonBlank(candidateId, "selectionEvidence.factorContributions candidateId"),
                    List.copyOf(Objects.requireNonNull(
                            contributions, "selectionEvidence factor contribution list cannot be null"))));
            return Collections.unmodifiableMap(copy);
        }
    }

    public record FactorContribution(
            String factorName,
            Double contributionValue,
            String direction,
            String exactness) {

        public FactorContribution {
            factorName = requireNonBlank(factorName, "factorName");
            if (contributionValue != null && !Double.isFinite(contributionValue)) {
                throw new IllegalArgumentException("contributionValue must be finite when present");
            }
            direction = requireNonBlank(direction, "direction");
            exactness = requireNonBlank(exactness, "exactness");
        }

        static FactorContribution from(ScoreFactorContribution contribution) {
            Objects.requireNonNull(contribution, "contribution cannot be null");
            return new FactorContribution(
                    contribution.factorName(),
                    contribution.contributionValue().isPresent()
                            ? contribution.contributionValue().getAsDouble()
                            : null,
                    contribution.direction().name(),
                    contribution.exactness().name());
        }
    }

    public record CandidateState(
            String upstreamId,
            boolean healthy,
            int inFlightRequestCount,
            Double configuredCapacity,
            Double estimatedConcurrencyLimit,
            double effectiveWeight,
            double averageLatencyMillis,
            double p95LatencyMillis,
            double p99LatencyMillis,
            double recentErrorRate,
            Integer queueDepth,
            Instant observedAt) {

        public CandidateState {
            upstreamId = requireNonBlank(upstreamId, "upstreamId");
            Objects.requireNonNull(observedAt, "observedAt cannot be null");
        }

        static CandidateState from(ServerStateVector state) {
            Objects.requireNonNull(state, "state cannot be null");
            return new CandidateState(
                    state.serverId(),
                    state.healthy(),
                    state.inFlightRequestCount(),
                    state.configuredCapacity().isPresent()
                            ? state.configuredCapacity().getAsDouble()
                            : null,
                    state.estimatedConcurrencyLimit().isPresent()
                            ? state.estimatedConcurrencyLimit().getAsDouble()
                            : null,
                    state.weight(),
                    state.averageLatencyMillis(),
                    state.p95LatencyMillis(),
                    state.p99LatencyMillis(),
                    state.recentErrorRate(),
                    state.queueDepth().isPresent() ? state.queueDepth().getAsInt() : null,
                    state.timestamp());
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
