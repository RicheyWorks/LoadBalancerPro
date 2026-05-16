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

    public double score(ServerStateVector state) {
        Objects.requireNonNull(state, "state cannot be null");
        double capacityBasis = capacityBasis(state);
        double inFlightRatio = state.inFlightRequestCount() / capacityBasis;
        double queueRatio = state.queueDepth().orElse(0) / capacityBasis;
        double score = (state.p95LatencyMillis() * P95_WEIGHT)
                + (state.p99LatencyMillis() * P99_WEIGHT)
                + (state.averageLatencyMillis() * AVERAGE_LATENCY_WEIGHT)
                + (inFlightRatio * IN_FLIGHT_RATIO_WEIGHT)
                + (queueRatio * QUEUE_RATIO_WEIGHT)
                + (state.recentErrorRate() * ERROR_RATE_WEIGHT)
                + networkRiskScore(state.networkAwarenessSignal());
        return state.healthy() ? score : score + UNHEALTHY_PENALTY;
    }

    public List<ScoreFactorContribution> factorContributions(ServerStateVector state) {
        Objects.requireNonNull(state, "state cannot be null");
        double capacityBasis = capacityBasis(state);
        double inFlightRatio = state.inFlightRequestCount() / capacityBasis;
        double queueRatio = state.queueDepth().orElse(0) / capacityBasis;
        List<ScoreFactorContribution> contributions = new ArrayList<>();

        contributions.add(exactFactor(
                "p95LatencyMillis",
                "p95LatencyMillis=" + format(state.p95LatencyMillis()),
                "P95_WEIGHT=" + format(P95_WEIGHT),
                state.p95LatencyMillis() * P95_WEIGHT,
                directionForPositivePenalty(state.p95LatencyMillis()),
                "p95 latency contribution = p95LatencyMillis * P95_WEIGHT.",
                "Tail latency is an exact current calculator input, not production telemetry proof."));
        contributions.add(exactFactor(
                "p99LatencyMillis",
                "p99LatencyMillis=" + format(state.p99LatencyMillis()),
                "P99_WEIGHT=" + format(P99_WEIGHT),
                state.p99LatencyMillis() * P99_WEIGHT,
                directionForPositivePenalty(state.p99LatencyMillis()),
                "p99 latency contribution = p99LatencyMillis * P99_WEIGHT.",
                "Tail latency is an exact current calculator input, not production telemetry proof."));
        contributions.add(exactFactor(
                "averageLatencyMillis",
                "averageLatencyMillis=" + format(state.averageLatencyMillis()),
                "AVERAGE_LATENCY_WEIGHT=" + format(AVERAGE_LATENCY_WEIGHT),
                state.averageLatencyMillis() * AVERAGE_LATENCY_WEIGHT,
                directionForPositivePenalty(state.averageLatencyMillis()),
                "Average latency contribution = averageLatencyMillis * AVERAGE_LATENCY_WEIGHT.",
                "Average latency is an exact current calculator input, not production monitoring proof."));
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

    private double capacityBasis(ServerStateVector state) {
        if (state.estimatedConcurrencyLimit().isPresent()) {
            return Math.max(1.0, state.estimatedConcurrencyLimit().getAsDouble());
        }
        if (state.configuredCapacity().isPresent()) {
            return Math.max(1.0, state.configuredCapacity().getAsDouble());
        }
        return 1.0;
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

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.6f", value);
    }
}
