package com.richmond423.loadbalancerpro.core;

import java.util.List;
import java.util.Objects;

final class RoutingComparisonDeterminism {
    private static final String SEED_NAMESPACE = "routing-comparison-seed-v1";

    private RoutingComparisonDeterminism() {
    }

    static long seed(RoutingStrategyId strategyId, List<ServerStateVector> candidates) {
        Objects.requireNonNull(strategyId, "strategyId cannot be null");
        Objects.requireNonNull(candidates, "candidates cannot be null");
        CanonicalDigest digest = CanonicalDigest.sha256(SEED_NAMESPACE)
                .putString(strategyId.externalName())
                .putInt(candidates.size());
        for (ServerStateVector candidate : candidates) {
            Objects.requireNonNull(candidate, "candidates cannot contain null values");
            appendCandidate(digest, candidate);
        }
        return digest.longDigest();
    }

    private static void appendCandidate(CanonicalDigest digest, ServerStateVector candidate) {
        NetworkAwarenessSignal network = candidate.networkAwarenessSignal();
        LatencyWindowSignal latencyWindow = candidate.latencyWindowSignal();
        digest.putString(candidate.serverId())
                .putBoolean(candidate.healthy())
                .putInt(candidate.inFlightRequestCount())
                .putOptionalDouble(candidate.configuredCapacity())
                .putOptionalDouble(candidate.estimatedConcurrencyLimit())
                .putDouble(candidate.weight())
                .putDouble(candidate.averageLatencyMillis())
                .putDouble(candidate.p95LatencyMillis())
                .putDouble(candidate.p99LatencyMillis())
                .putDouble(candidate.recentErrorRate())
                .putOptionalInt(candidate.queueDepth())
                .putDouble(network.timeoutRate())
                .putDouble(network.retryRate())
                .putDouble(network.connectionFailureRate())
                .putDouble(network.latencyJitterMillis())
                .putBoolean(network.recentErrorBurst())
                .putInt(network.requestTimeoutCount())
                .putInt(network.sampleSize())
                .putInt(latencyWindow.sampleCount())
                .putOptionalDouble(latencyWindow.ewmaLatencyMillis())
                .putOptionalDouble(latencyWindow.rollingAverageLatencyMillis())
                .putOptionalDouble(latencyWindow.rollingP95LatencyMillis())
                .putOptionalDouble(latencyWindow.rollingP99LatencyMillis());
    }
}
