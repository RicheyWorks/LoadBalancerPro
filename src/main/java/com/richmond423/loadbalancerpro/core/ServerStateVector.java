package com.richmond423.loadbalancerpro.core;

import java.time.Instant;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public record ServerStateVector(
        String serverId,
        boolean healthy,
        int inFlightRequestCount,
        OptionalDouble configuredCapacity,
        OptionalDouble estimatedConcurrencyLimit,
        double weight,
        double averageLatencyMillis,
        double p95LatencyMillis,
        double p99LatencyMillis,
        double recentErrorRate,
        OptionalInt queueDepth,
        NetworkAwarenessSignal networkAwarenessSignal,
        LatencyWindowSignal latencyWindowSignal,
        Instant timestamp) {
    private static final double MINIMUM_CAPACITY_BASIS = 1.0;
    private static final double MINIMUM_LATENCY_BASIS_MILLIS = 1.0;

    public ServerStateVector {
        serverId = requireNonBlank(serverId, "serverId");
        Objects.requireNonNull(configuredCapacity, "configuredCapacity cannot be null");
        Objects.requireNonNull(estimatedConcurrencyLimit, "estimatedConcurrencyLimit cannot be null");
        Objects.requireNonNull(queueDepth, "queueDepth cannot be null");
        Objects.requireNonNull(networkAwarenessSignal, "networkAwarenessSignal cannot be null");
        Objects.requireNonNull(latencyWindowSignal, "latencyWindowSignal cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        requireNonNegative(inFlightRequestCount, "inFlightRequestCount");
        configuredCapacity.ifPresent(value -> requireNonNegative(value, "configuredCapacity"));
        estimatedConcurrencyLimit.ifPresent(value -> requirePositive(value, "estimatedConcurrencyLimit"));
        requireNonNegative(weight, "weight");
        requireNonNegative(averageLatencyMillis, "averageLatencyMillis");
        requireNonNegative(p95LatencyMillis, "p95LatencyMillis");
        requireNonNegative(p99LatencyMillis, "p99LatencyMillis");
        requireRate(recentErrorRate, "recentErrorRate");
        queueDepth.ifPresent(value -> requireNonNegative(value, "queueDepth"));
    }

    public ServerStateVector(String serverId,
                             boolean healthy,
                             int inFlightRequestCount,
                             OptionalDouble configuredCapacity,
                             OptionalDouble estimatedConcurrencyLimit,
                             double weight,
                             double averageLatencyMillis,
                             double p95LatencyMillis,
                             double p99LatencyMillis,
                             double recentErrorRate,
                             OptionalInt queueDepth,
                             NetworkAwarenessSignal networkAwarenessSignal,
                             Instant timestamp) {
        this(serverId, healthy, inFlightRequestCount, configuredCapacity, estimatedConcurrencyLimit, weight,
                averageLatencyMillis, p95LatencyMillis, p99LatencyMillis, recentErrorRate, queueDepth,
                networkAwarenessSignal, LatencyWindowSignal.empty(), timestamp);
    }

    public ServerStateVector(String serverId,
                             boolean healthy,
                             int inFlightRequestCount,
                             OptionalDouble configuredCapacity,
                             OptionalDouble estimatedConcurrencyLimit,
                             double averageLatencyMillis,
                             double p95LatencyMillis,
                             double p99LatencyMillis,
                             double recentErrorRate,
                             OptionalInt queueDepth,
                             NetworkAwarenessSignal networkAwarenessSignal,
                             Instant timestamp) {
        this(serverId, healthy, inFlightRequestCount, configuredCapacity, estimatedConcurrencyLimit, 1.0,
                averageLatencyMillis, p95LatencyMillis, p99LatencyMillis, recentErrorRate, queueDepth,
                networkAwarenessSignal, LatencyWindowSignal.empty(), timestamp);
    }

    public ServerStateVector(String serverId,
                             boolean healthy,
                             int inFlightRequestCount,
                             OptionalDouble configuredCapacity,
                             OptionalDouble estimatedConcurrencyLimit,
                             double weight,
                             double averageLatencyMillis,
                             double p95LatencyMillis,
                             double p99LatencyMillis,
                             double recentErrorRate,
                             OptionalInt queueDepth,
                             Instant timestamp) {
        this(serverId, healthy, inFlightRequestCount, configuredCapacity, estimatedConcurrencyLimit, weight,
                averageLatencyMillis, p95LatencyMillis, p99LatencyMillis, recentErrorRate, queueDepth,
                NetworkAwarenessSignal.neutral(serverId, timestamp), LatencyWindowSignal.empty(), timestamp);
    }

    public ServerStateVector(String serverId,
                             boolean healthy,
                             int inFlightRequestCount,
                             double configuredCapacity,
                             double estimatedConcurrencyLimit,
                             double averageLatencyMillis,
                             double p95LatencyMillis,
                             double p99LatencyMillis,
                             double recentErrorRate,
                             int queueDepth,
                             Instant timestamp) {
        this(serverId, healthy, inFlightRequestCount, OptionalDouble.of(configuredCapacity),
                OptionalDouble.of(estimatedConcurrencyLimit), 1.0, averageLatencyMillis, p95LatencyMillis,
                p99LatencyMillis, recentErrorRate, OptionalInt.of(queueDepth),
                NetworkAwarenessSignal.neutral(serverId, timestamp), LatencyWindowSignal.empty(), timestamp);
    }

    public ServerStateVector(String serverId,
                             boolean healthy,
                             int inFlightRequestCount,
                             double configuredCapacity,
                             double estimatedConcurrencyLimit,
                             double averageLatencyMillis,
                             double p95LatencyMillis,
                             double p99LatencyMillis,
                             double recentErrorRate,
                             int queueDepth,
                             NetworkAwarenessSignal networkAwarenessSignal,
                             Instant timestamp) {
        this(serverId, healthy, inFlightRequestCount, OptionalDouble.of(configuredCapacity),
                OptionalDouble.of(estimatedConcurrencyLimit), 1.0, averageLatencyMillis, p95LatencyMillis,
                p99LatencyMillis, recentErrorRate, OptionalInt.of(queueDepth), networkAwarenessSignal,
                LatencyWindowSignal.empty(), timestamp);
    }

    public static ServerStateVector fromServer(Server server,
                                               int inFlightRequestCount,
                                               double averageLatencyMillis,
                                               double p95LatencyMillis,
                                               double p99LatencyMillis,
                                               double recentErrorRate,
                                               int queueDepth,
                                               Instant timestamp) {
        Objects.requireNonNull(server, "server cannot be null");
        return new ServerStateVector(server.getServerId(), server.isHealthy(), inFlightRequestCount,
                OptionalDouble.of(server.getCapacity()), OptionalDouble.empty(), server.getWeight(), averageLatencyMillis,
                p95LatencyMillis, p99LatencyMillis, recentErrorRate, OptionalInt.of(queueDepth),
                NetworkAwarenessSignal.neutral(server.getServerId(), timestamp), LatencyWindowSignal.empty(), timestamp);
    }

    public double capacityBasis() {
        if (estimatedConcurrencyLimit.isPresent()) {
            return Math.max(MINIMUM_CAPACITY_BASIS, estimatedConcurrencyLimit.getAsDouble());
        }
        if (configuredCapacity.isPresent()) {
            return Math.max(MINIMUM_CAPACITY_BASIS, configuredCapacity.getAsDouble());
        }
        return MINIMUM_CAPACITY_BASIS;
    }

    public double inFlightPressure() {
        return inFlightRequestCount / capacityBasis();
    }

    public double queuePressure() {
        return queueDepth.orElse(0) / capacityBasis();
    }

    public double boundedInFlightPressure() {
        return clampUnit(inFlightPressure());
    }

    public double boundedQueuePressure() {
        return clampUnit(queuePressure());
    }

    public double tailLatencySpreadMillis() {
        return Math.max(0.0, p99LatencyMillis - p95LatencyMillis);
    }

    public double tailLatencyPressure() {
        return boundedRatio(tailLatencySpreadMillis(), Math.max(MINIMUM_LATENCY_BASIS_MILLIS, p95LatencyMillis));
    }

    public double effectiveAverageLatencyMillis() {
        return latencyWindowSignal.effectiveAverageLatencyMillis(averageLatencyMillis);
    }

    public double effectiveP95LatencyMillis() {
        return latencyWindowSignal.effectiveP95LatencyMillis(p95LatencyMillis);
    }

    public double effectiveP99LatencyMillis() {
        return latencyWindowSignal.effectiveP99LatencyMillis(p99LatencyMillis);
    }

    public double effectiveTailLatencySpreadMillis() {
        return latencyWindowSignal.effectiveTailLatencySpreadMillis(p95LatencyMillis, p99LatencyMillis);
    }

    public double effectiveTailLatencyPressure() {
        return latencyWindowSignal.effectiveTailLatencyPressure(p95LatencyMillis, p99LatencyMillis);
    }

    public double errorPressure() {
        return recentErrorRate;
    }

    public double networkRiskPressure() {
        NetworkAwarenessSignal signal = networkAwarenessSignal;
        double timeoutCountPressure = signal.sampleSize() > 0
                ? boundedRatio(signal.requestTimeoutCount(), signal.sampleSize())
                : (signal.requestTimeoutCount() > 0 ? 1.0 : 0.0);
        double jitterPressure = boundedRatio(signal.latencyJitterMillis(),
                Math.max(MINIMUM_LATENCY_BASIS_MILLIS, p99LatencyMillis));
        double ratePressure = Math.max(signal.timeoutRate(),
                Math.max(signal.retryRate(), signal.connectionFailureRate()));
        double burstPressure = signal.recentErrorBurst() ? 1.0 : 0.0;
        return Math.max(ratePressure, Math.max(jitterPressure, Math.max(burstPressure, timeoutCountPressure)));
    }

    public double normalizedHealthPressure() {
        if (!healthy) {
            return 1.0;
        }
        return Math.max(tailLatencyPressure(),
                Math.max(effectiveTailLatencyPressure(),
                        Math.max(boundedInFlightPressure(),
                                Math.max(boundedQueuePressure(),
                                        Math.max(errorPressure(), networkRiskPressure())))));
    }

    public boolean hasMaterialRisk() {
        return !healthy || normalizedHealthPressure() > 0.0;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }

    private static void requireNonNegative(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative");
        }
    }

    private static void requirePositive(double value, String fieldName) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and positive");
        }
    }

    private static void requireRate(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
        }
    }

    private static double boundedRatio(double numerator, double denominator) {
        if (denominator <= 0.0) {
            return numerator > 0.0 ? 1.0 : 0.0;
        }
        return clampUnit(numerator / denominator);
    }

    private static double clampUnit(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
