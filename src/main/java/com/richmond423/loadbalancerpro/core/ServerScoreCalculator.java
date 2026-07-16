package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

public final class ServerScoreCalculator {
    private static final double UNHEALTHY_PENALTY = 1_000_000.0;
    private static final double P95_WEIGHT = 0.45;
    private static final double P99_WEIGHT = 0.35;
    private static final double AVERAGE_LATENCY_WEIGHT = 0.10;
    private static final double IN_FLIGHT_RATIO_WEIGHT = 100.0;
    private static final double QUEUE_RATIO_WEIGHT = 100.0;
    private static final double ERROR_RATE_WEIGHT = 1_000.0;
    private static final double TIMEOUT_RATE_WEIGHT = 800.0;
    private static final double RETRY_RATE_WEIGHT = 350.0;
    private static final double CONNECTION_FAILURE_RATE_WEIGHT = 900.0;
    private static final double LATENCY_JITTER_WEIGHT = 0.50;
    private static final double RECENT_ERROR_BURST_PENALTY = 250.0;
    private static final double REQUEST_TIMEOUT_COUNT_WEIGHT = 20.0;
    private static final double RECOMMENDATION_AVERAGE_LATENCY_WEIGHT = 5.0;
    private static final double RECOMMENDATION_P95_LATENCY_WEIGHT = 12.0;
    private static final double RECOMMENDATION_P99_LATENCY_WEIGHT = 18.0;
    private static final double RECOMMENDATION_IN_FLIGHT_WEIGHT = 10.0;
    private static final double RECOMMENDATION_QUEUE_WEIGHT = 5.0;
    private static final double RECOMMENDATION_ERROR_RATE_WEIGHT = 15.0;
    private static final double RECOMMENDATION_TIMEOUT_RATE_WEIGHT = 8.0;
    private static final double RECOMMENDATION_CONNECTION_FAILURE_WEIGHT = 8.0;
    private static final double RECOMMENDATION_JITTER_WEIGHT = 4.0;
    private static final double RECOMMENDATION_CONFIDENCE_WEIGHT = 5.0;
    private static final double RECOMMENDATION_DEGRADATION_WEIGHT = 10.0;
    private static final double RECOMMENDATION_BASE_WEIGHT_TOTAL = RECOMMENDATION_AVERAGE_LATENCY_WEIGHT
            + RECOMMENDATION_P95_LATENCY_WEIGHT
            + RECOMMENDATION_P99_LATENCY_WEIGHT
            + RECOMMENDATION_IN_FLIGHT_WEIGHT
            + RECOMMENDATION_QUEUE_WEIGHT
            + RECOMMENDATION_ERROR_RATE_WEIGHT
            + RECOMMENDATION_TIMEOUT_RATE_WEIGHT
            + RECOMMENDATION_CONNECTION_FAILURE_WEIGHT
            + RECOMMENDATION_JITTER_WEIGHT
            + RECOMMENDATION_CONFIDENCE_WEIGHT
            + RECOMMENDATION_DEGRADATION_WEIGHT;

    public double score(ServerStateVector state) {
        Objects.requireNonNull(state, "state cannot be null");
        double score = (state.effectiveP95LatencyMillis() * P95_WEIGHT)
                + (state.effectiveP99LatencyMillis() * P99_WEIGHT)
                + (state.effectiveAverageLatencyMillis() * AVERAGE_LATENCY_WEIGHT)
                + (state.inFlightPressure() * IN_FLIGHT_RATIO_WEIGHT)
                + (state.queuePressure() * QUEUE_RATIO_WEIGHT)
                + (state.recentErrorRate() * ERROR_RATE_WEIGHT)
                + networkRiskScore(state.networkAwarenessSignal());
        return state.healthy() ? score : score + UNHEALTHY_PENALTY;
    }

    public ServerScoreBreakdown scoreBreakdown(ServerStateVector state) {
        Objects.requireNonNull(state, "state cannot be null");
        List<ScoreFactorContribution> contributions = factorContributions(state);
        double totalScore = contributions.stream()
                .filter(ScoreFactorContribution::hasExactContributionValue)
                .mapToDouble(contribution -> contribution.contributionValue().orElseThrow())
                .sum();
        return new ServerScoreBreakdown(state.serverId(), totalScore, contributions);
    }

    public double recommendationScore(
            ServerStateVector state,
            ServerRollingSignalState signalState,
            ServerRecommendationScorePolicy policy) {
        return recommendationScoreBreakdown(state, signalState, policy).totalScore();
    }

    public ServerScoreBreakdown recommendationScoreBreakdown(
            ServerStateVector state,
            ServerRollingSignalState signalState,
            ServerRecommendationScorePolicy policy) {
        Objects.requireNonNull(state, "state cannot be null");
        Objects.requireNonNull(signalState, "signalState cannot be null");
        Objects.requireNonNull(policy, "policy cannot be null");
        if (!state.serverId().equals(signalState.serverId())) {
            throw new IllegalArgumentException("state serverId must match signalState serverId");
        }
        if (!state.timestamp().equals(signalState.evaluatedAt())) {
            throw new IllegalArgumentException("state timestamp must match signalState evaluatedAt");
        }
        requireSameRate(state.recentErrorRate(), signalState.failureRate(),
                "state recentErrorRate must match signalState failureRate");
        requireSameRate(state.networkAwarenessSignal().timeoutRate(), signalState.timeoutRate(),
                "state timeoutRate must match signalState timeoutRate");
        requireSameRate(state.networkAwarenessSignal().connectionFailureRate(), signalState.connectionFailureRate(),
                "state connectionFailureRate must match signalState connectionFailureRate");

        List<ScoreFactorContribution> contributions = List.of(
                recommendationFactor(
                        "boundedAverageLatency",
                        state.effectiveAverageLatencyMillis(),
                        normalize(state.effectiveAverageLatencyMillis(), policy.maximumLatencyMillis()),
                        RECOMMENDATION_AVERAGE_LATENCY_WEIGHT,
                        ScoreFactorDirection.NEUTRAL,
                        "Effective average latency is clamped to the configured recommendation latency bound."),
                recommendationFactor(
                        "boundedP95Latency",
                        state.effectiveP95LatencyMillis(),
                        normalize(state.effectiveP95LatencyMillis(), policy.maximumLatencyMillis()),
                        RECOMMENDATION_P95_LATENCY_WEIGHT,
                        ScoreFactorDirection.NEUTRAL,
                        "Effective p95 latency is clamped to the configured recommendation latency bound."),
                recommendationFactor(
                        "boundedP99Latency",
                        state.effectiveP99LatencyMillis(),
                        normalize(state.effectiveP99LatencyMillis(), policy.maximumLatencyMillis()),
                        RECOMMENDATION_P99_LATENCY_WEIGHT,
                        ScoreFactorDirection.NEUTRAL,
                        "Effective p99 latency is clamped to the configured recommendation latency bound."),
                recommendationFactor(
                        "boundedInFlightPressure",
                        state.inFlightPressure(),
                        state.boundedInFlightPressure(),
                        RECOMMENDATION_IN_FLIGHT_WEIGHT,
                        ScoreFactorDirection.NEUTRAL,
                        "In-flight pressure is clamped to the unit interval."),
                recommendationFactor(
                        "boundedQueuePressure",
                        state.queuePressure(),
                        state.boundedQueuePressure(),
                        RECOMMENDATION_QUEUE_WEIGHT,
                        ScoreFactorDirection.NEUTRAL,
                        "Queue pressure is clamped to the unit interval."),
                recommendationFactor(
                        "boundedRecentErrorRate",
                        state.recentErrorRate(),
                        clampUnit(state.recentErrorRate()),
                        RECOMMENDATION_ERROR_RATE_WEIGHT,
                        ScoreFactorDirection.NEUTRAL,
                        "Recent error rate is already a validated unit-interval signal."),
                recommendationFactor(
                        "boundedTimeoutRate",
                        signalState.timeoutRate(),
                        clampUnit(signalState.timeoutRate()),
                        RECOMMENDATION_TIMEOUT_RATE_WEIGHT,
                        ScoreFactorDirection.NEUTRAL,
                        "Timeout rate is already a validated unit-interval rolling signal."),
                recommendationFactor(
                        "boundedConnectionFailureRate",
                        signalState.connectionFailureRate(),
                        clampUnit(signalState.connectionFailureRate()),
                        RECOMMENDATION_CONNECTION_FAILURE_WEIGHT,
                        ScoreFactorDirection.NEUTRAL,
                        "Connection failure rate is already a validated unit-interval rolling signal."),
                recommendationFactor(
                        "boundedLatencyJitter",
                        state.networkAwarenessSignal().latencyJitterMillis(),
                        normalize(state.networkAwarenessSignal().latencyJitterMillis(),
                                policy.maximumJitterMillis()),
                        RECOMMENDATION_JITTER_WEIGHT,
                        ScoreFactorDirection.NEUTRAL,
                        "Latency jitter is clamped to the configured recommendation jitter bound."),
                recommendationFactor(
                        "confidencePenalty",
                        confidencePenalty(signalState.confidence()),
                        confidencePenalty(signalState.confidence()),
                        RECOMMENDATION_CONFIDENCE_WEIGHT,
                        ScoreFactorDirection.SUPPORTS_SELECTION,
                        "Confidence maps deterministically to a bounded evidence penalty."),
                recommendationFactor(
                        "degradationPenalty",
                        degradationPenalty(signalState.degradationState()),
                        degradationPenalty(signalState.degradationState()),
                        RECOMMENDATION_DEGRADATION_WEIGHT,
                        ScoreFactorDirection.SUPPORTS_SELECTION,
                        "Degradation state maps deterministically to a bounded state penalty."),
                recommendationFactor(
                        "recommendationEligibilityPenalty",
                        state.healthy() ? 0.0 : 1.0,
                        state.healthy() ? 0.0 : 1.0,
                        policy.ineligiblePenaltyWeight(),
                        ScoreFactorDirection.SUPPORTS_SELECTION,
                        "Ineligible score vectors receive the explicit configured fail-closed penalty."));

        double totalScore = contributions.stream()
                .mapToDouble(contribution -> contribution.contributionValue().orElseThrow())
                .sum();
        double maximumScore = maximumRecommendationScore(policy);
        if (totalScore < 0.0 || totalScore > maximumScore + 0.000000001) {
            throw new IllegalStateException("recommendation score exceeded its configured bounds");
        }
        return new ServerScoreBreakdown(state.serverId(), totalScore, contributions);
    }

    public double maximumRecommendationScore(ServerRecommendationScorePolicy policy) {
        Objects.requireNonNull(policy, "policy cannot be null");
        return RECOMMENDATION_BASE_WEIGHT_TOTAL + policy.ineligiblePenaltyWeight();
    }

    public List<ScoreFactorContribution> factorContributions(ServerStateVector state) {
        Objects.requireNonNull(state, "state cannot be null");
        double capacityBasis = state.capacityBasis();
        double inFlightRatio = state.inFlightPressure();
        double queueRatio = state.queuePressure();
        double effectiveP95LatencyMillis = state.effectiveP95LatencyMillis();
        double effectiveP99LatencyMillis = state.effectiveP99LatencyMillis();
        double effectiveAverageLatencyMillis = state.effectiveAverageLatencyMillis();
        List<ScoreFactorContribution> contributions = new ArrayList<>();

        contributions.add(exactFactor(
                "p95LatencyMillis",
                effectiveLatencyDescription("p95LatencyMillis", state.p95LatencyMillis(),
                        "effectiveP95LatencyMillis", effectiveP95LatencyMillis, state.latencyWindowSignal()),
                "P95_WEIGHT=" + format(P95_WEIGHT),
                effectiveP95LatencyMillis * P95_WEIGHT,
                directionForPositivePenalty(effectiveP95LatencyMillis),
                "Effective p95 latency contribution = effectiveP95LatencyMillis * P95_WEIGHT.",
                "Tail latency is an exact current calculator input, not production telemetry proof."));
        contributions.add(exactFactor(
                "p99LatencyMillis",
                effectiveLatencyDescription("p99LatencyMillis", state.p99LatencyMillis(),
                        "effectiveP99LatencyMillis", effectiveP99LatencyMillis, state.latencyWindowSignal()),
                "P99_WEIGHT=" + format(P99_WEIGHT),
                effectiveP99LatencyMillis * P99_WEIGHT,
                directionForPositivePenalty(effectiveP99LatencyMillis),
                "Effective p99 latency contribution = effectiveP99LatencyMillis * P99_WEIGHT.",
                "Tail latency is an exact current calculator input, not production telemetry proof."));
        contributions.add(exactFactor(
                "averageLatencyMillis",
                effectiveLatencyDescription("averageLatencyMillis", state.averageLatencyMillis(),
                        "effectiveAverageLatencyMillis", effectiveAverageLatencyMillis, state.latencyWindowSignal()),
                "AVERAGE_LATENCY_WEIGHT=" + format(AVERAGE_LATENCY_WEIGHT),
                effectiveAverageLatencyMillis * AVERAGE_LATENCY_WEIGHT,
                directionForPositivePenalty(effectiveAverageLatencyMillis),
                "Effective average latency contribution = effectiveAverageLatencyMillis * AVERAGE_LATENCY_WEIGHT.",
                "Average latency is an exact current calculator input, not production monitoring proof."));
        contributions.add(exactFactor(
                "latencyWindowSignal",
                latencyWindowDescription(state),
                "No standalone score weight; effective latency factors may use bounded window values when present.",
                0.0,
                ScoreFactorDirection.NEUTRAL,
                "Latency window signal has no standalone additive contribution; it can shape latency factor inputs.",
                "Latency window signal is deterministic local state-vector evidence, not p95/p99 production proof."));
        contributions.add(exactFactor(
                "capacityBasis",
                capacityBasisDescription(state, capacityBasis),
                "Used as denominator for in-flight and queue ratios; no standalone score weight.",
                0.0,
                ScoreFactorDirection.NEUTRAL,
                "Capacity basis has no standalone additive contribution; it shapes ratio factors.",
                "Configured capacity or estimated concurrency is visible lab input, not a production capacity claim."));
        contributions.add(exactFactor(
                "inFlightRequestRatio",
                "inFlightRequestCount=" + state.inFlightRequestCount()
                        + ", capacityBasis=" + format(capacityBasis)
                        + ", ratio=" + format(inFlightRatio),
                "IN_FLIGHT_RATIO_WEIGHT=" + format(IN_FLIGHT_RATIO_WEIGHT),
                inFlightRatio * IN_FLIGHT_RATIO_WEIGHT,
                directionForPositivePenalty(inFlightRatio),
                "In-flight pressure contribution = inFlightRequestRatio * IN_FLIGHT_RATIO_WEIGHT.",
                "In-flight pressure is exact for the current local lab state vector only."));
        contributions.add(exactFactor(
                "queueDepthRatio",
                "queueDepth=" + state.queueDepth().orElse(0)
                        + ", capacityBasis=" + format(capacityBasis)
                        + ", ratio=" + format(queueRatio),
                "QUEUE_RATIO_WEIGHT=" + format(QUEUE_RATIO_WEIGHT),
                queueRatio * QUEUE_RATIO_WEIGHT,
                directionForPositivePenalty(queueRatio),
                "Queue pressure contribution = queueDepthRatio * QUEUE_RATIO_WEIGHT.",
                "Queue pressure is exact for the current local lab state vector only."));
        contributions.add(exactFactor(
                "recentErrorRate",
                "recentErrorRate=" + format(state.recentErrorRate()),
                "ERROR_RATE_WEIGHT=" + format(ERROR_RATE_WEIGHT),
                state.recentErrorRate() * ERROR_RATE_WEIGHT,
                directionForPositivePenalty(state.recentErrorRate()),
                "Recent error contribution = recentErrorRate * ERROR_RATE_WEIGHT.",
                "Recent error rate is exact for the current local lab state vector only."));

        contributions.addAll(networkRiskFactorContributions(state.networkAwarenessSignal()));

        contributions.add(exactFactor(
                "healthPenalty",
                "healthy=" + state.healthy(),
                "UNHEALTHY_PENALTY=" + format(UNHEALTHY_PENALTY),
                state.healthy() ? 0.0 : UNHEALTHY_PENALTY,
                state.healthy() ? ScoreFactorDirection.SUPPORTS_SELECTION : ScoreFactorDirection.WEAKENS_SELECTION,
                "Health contribution is zero for healthy candidates and adds the unhealthy penalty otherwise.",
                "Health penalty explains current calculator behavior; it is not production certification."));
        contributions.add(exactFactor(
                "serverWeightNotApplied",
                "weight=" + format(state.weight()),
                "No ServerScoreCalculator weight is applied to this score.",
                0.0,
                ScoreFactorDirection.NEUTRAL,
                "Server weight is visible on the state vector, but this calculator does not add a weight term.",
                "Do not infer hidden score weighting from the visible weight field."));
        contributions.add(boundaryFactor(
                "hiddenRoutingInternals",
                "not exposed by this calculator contract",
                "No hidden weight is exposed.",
                ScoreFactorDirection.UNKNOWN,
                "Hidden routing internals are not inferred.",
                "Exact production scoring is not claimed; this contract explains current local calculator components only."));
        return List.copyOf(contributions);
    }

    public double networkRiskScore(NetworkAwarenessSignal signal) {
        Objects.requireNonNull(signal, "signal cannot be null");
        return (signal.timeoutRate() * TIMEOUT_RATE_WEIGHT)
                + (signal.retryRate() * RETRY_RATE_WEIGHT)
                + (signal.connectionFailureRate() * CONNECTION_FAILURE_RATE_WEIGHT)
                + (signal.latencyJitterMillis() * LATENCY_JITTER_WEIGHT)
                + (signal.recentErrorBurst() ? RECENT_ERROR_BURST_PENALTY : 0.0)
                + (signal.requestTimeoutCount() * REQUEST_TIMEOUT_COUNT_WEIGHT);
    }

    public List<ScoreFactorContribution> networkRiskFactorContributions(NetworkAwarenessSignal signal) {
        Objects.requireNonNull(signal, "signal cannot be null");
        return List.of(
                exactFactor(
                        "timeoutRate",
                        "timeoutRate=" + format(signal.timeoutRate()),
                        "TIMEOUT_RATE_WEIGHT=" + format(TIMEOUT_RATE_WEIGHT),
                        signal.timeoutRate() * TIMEOUT_RATE_WEIGHT,
                        directionForPositivePenalty(signal.timeoutRate()),
                        "Timeout contribution = timeoutRate * TIMEOUT_RATE_WEIGHT.",
                        "Network signal contribution is local lab evidence, not production telemetry proof."),
                exactFactor(
                        "retryRate",
                        "retryRate=" + format(signal.retryRate()),
                        "RETRY_RATE_WEIGHT=" + format(RETRY_RATE_WEIGHT),
                        signal.retryRate() * RETRY_RATE_WEIGHT,
                        directionForPositivePenalty(signal.retryRate()),
                        "Retry contribution = retryRate * RETRY_RATE_WEIGHT.",
                        "Network signal contribution is local lab evidence, not production telemetry proof."),
                exactFactor(
                        "connectionFailureRate",
                        "connectionFailureRate=" + format(signal.connectionFailureRate()),
                        "CONNECTION_FAILURE_RATE_WEIGHT=" + format(CONNECTION_FAILURE_RATE_WEIGHT),
                        signal.connectionFailureRate() * CONNECTION_FAILURE_RATE_WEIGHT,
                        directionForPositivePenalty(signal.connectionFailureRate()),
                        "Connection failure contribution = connectionFailureRate * CONNECTION_FAILURE_RATE_WEIGHT.",
                        "Network signal contribution is local lab evidence, not production telemetry proof."),
                exactFactor(
                        "latencyJitterMillis",
                        "latencyJitterMillis=" + format(signal.latencyJitterMillis()),
                        "LATENCY_JITTER_WEIGHT=" + format(LATENCY_JITTER_WEIGHT),
                        signal.latencyJitterMillis() * LATENCY_JITTER_WEIGHT,
                        directionForPositivePenalty(signal.latencyJitterMillis()),
                        "Latency jitter contribution = latencyJitterMillis * LATENCY_JITTER_WEIGHT.",
                        "Network signal contribution is local lab evidence, not production telemetry proof."),
                exactFactor(
                        "recentErrorBurst",
                        "recentErrorBurst=" + signal.recentErrorBurst(),
                        "RECENT_ERROR_BURST_PENALTY=" + format(RECENT_ERROR_BURST_PENALTY),
                        signal.recentErrorBurst() ? RECENT_ERROR_BURST_PENALTY : 0.0,
                        signal.recentErrorBurst() ? ScoreFactorDirection.WEAKENS_SELECTION : ScoreFactorDirection.NEUTRAL,
                        "Recent error burst contribution adds the burst penalty when true.",
                        "Network signal contribution is local lab evidence, not production telemetry proof."),
                exactFactor(
                        "requestTimeoutCount",
                        "requestTimeoutCount=" + signal.requestTimeoutCount(),
                        "REQUEST_TIMEOUT_COUNT_WEIGHT=" + format(REQUEST_TIMEOUT_COUNT_WEIGHT),
                        signal.requestTimeoutCount() * REQUEST_TIMEOUT_COUNT_WEIGHT,
                        directionForPositivePenalty(signal.requestTimeoutCount()),
                        "Request timeout count contribution = requestTimeoutCount * REQUEST_TIMEOUT_COUNT_WEIGHT.",
                        "Network signal contribution is local lab evidence, not production telemetry proof."));
    }

    private ScoreFactorContribution exactFactor(String factorName,
                                                String rawValueDescription,
                                                String weightDescription,
                                                double contributionValue,
                                                ScoreFactorDirection direction,
                                                String contributionDescription,
                                                String boundaryNote) {
        return new ScoreFactorContribution(
                factorName,
                rawValueDescription,
                weightDescription,
                direction,
                contributionDescription,
                OptionalDouble.of(contributionValue),
                ScoreFactorExactness.EXACT_FROM_CALCULATOR,
                contributionDescription + " Lower additive score favors selection.",
                boundaryNote);
    }

    private ScoreFactorContribution boundaryFactor(String factorName,
                                                   String rawValueDescription,
                                                   String weightDescription,
                                                   ScoreFactorDirection direction,
                                                   String contributionDescription,
                                                   String boundaryNote) {
        return new ScoreFactorContribution(
                factorName,
                rawValueDescription,
                weightDescription,
                direction,
                contributionDescription,
                OptionalDouble.empty(),
                ScoreFactorExactness.NOT_EXPOSED,
                contributionDescription,
                boundaryNote);
    }

    private ScoreFactorContribution recommendationFactor(
            String factorName,
            double rawValue,
            double normalizedValue,
            double weight,
            ScoreFactorDirection zeroDirection,
            String explanation) {
        double contributionValue = normalizedValue * weight;
        ScoreFactorDirection direction = contributionValue > 0.0
                ? ScoreFactorDirection.WEAKENS_SELECTION
                : zeroDirection;
        return new ScoreFactorContribution(
                factorName,
                "rawValue=" + format(rawValue) + ", normalizedValue=" + format(normalizedValue),
                "weight=" + format(weight),
                direction,
                "contribution = normalizedValue * weight = " + format(contributionValue),
                OptionalDouble.of(contributionValue),
                ScoreFactorExactness.EXACT_FROM_CALCULATOR,
                explanation + " Lower bounded additive score favors selection.",
                "This is deterministic local recommendation scoring, not production telemetry or routing proof.",
                OptionalDouble.of(rawValue),
                OptionalDouble.of(normalizedValue),
                OptionalDouble.of(weight));
    }

    private double confidencePenalty(ServerSignalConfidence confidence) {
        return switch (confidence) {
            case NONE -> 1.0;
            case LOW -> 0.75;
            case MEDIUM -> 0.25;
            case HIGH -> 0.0;
        };
    }

    private double degradationPenalty(ServerDegradationState state) {
        return switch (state) {
            case UNKNOWN, FAILED -> 1.0;
            case RECOVERING -> 0.75;
            case PARTIALLY_DEGRADED -> 0.50;
            case HEALTHY -> 0.0;
        };
    }

    private double normalize(double value, double maximum) {
        return clampUnit(value / maximum);
    }

    private double clampUnit(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private void requireSameRate(double first, double second, String message) {
        if (Math.abs(first - second) > 0.000000001) {
            throw new IllegalArgumentException(message);
        }
    }

    private ScoreFactorDirection directionForPositivePenalty(double value) {
        return value > 0.0 ? ScoreFactorDirection.WEAKENS_SELECTION : ScoreFactorDirection.NEUTRAL;
    }

    private String capacityBasisDescription(ServerStateVector state, double capacityBasis) {
        if (state.estimatedConcurrencyLimit().isPresent()) {
            return "estimatedConcurrencyLimit=" + format(state.estimatedConcurrencyLimit().getAsDouble())
                    + ", capacityBasis=" + format(capacityBasis);
        }
        if (state.configuredCapacity().isPresent()) {
            return "configuredCapacity=" + format(state.configuredCapacity().getAsDouble())
                    + ", capacityBasis=" + format(capacityBasis);
        }
        return "configuredCapacity=not exposed, estimatedConcurrencyLimit=not exposed, capacityBasis="
                + format(capacityBasis);
    }

    private String effectiveLatencyDescription(String rawLabel,
                                               double rawValue,
                                               String effectiveLabel,
                                               double effectiveValue,
                                               LatencyWindowSignal latencyWindowSignal) {
        return rawLabel + "=" + format(rawValue)
                + ", " + effectiveLabel + "=" + format(effectiveValue)
                + ", latencyWindowSamples=" + latencyWindowSignal.sampleCount();
    }

    private String latencyWindowDescription(ServerStateVector state) {
        LatencyWindowSignal signal = state.latencyWindowSignal();
        if (!signal.hasLatencyWindowValues()) {
            return "latencyWindowSignal=empty, effective latency values use current state vector values";
        }
        return "latencyWindowSignal=present, sampleCount=" + signal.sampleCount()
                + ", effectiveAverageLatencyMillis=" + format(state.effectiveAverageLatencyMillis())
                + ", effectiveP95LatencyMillis=" + format(state.effectiveP95LatencyMillis())
                + ", effectiveP99LatencyMillis=" + format(state.effectiveP99LatencyMillis());
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.6f", value);
    }
}
