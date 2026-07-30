package com.richmond423.loadbalancerpro.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Shared threshold classifier for the LASE pressure consumers.
 *
 * <p>The classifier owns the boundary operators used by failure scenarios, load shedding, and
 * shadow autoscaling. Each consumer remains responsible for its own action policy and reason
 * wording.</p>
 */
public final class PressureClassifier {
    private PressureClassifier() {
    }

    public static Assessment assess(Observation observation, Thresholds thresholds) {
        Objects.requireNonNull(observation, "observation cannot be null");
        Objects.requireNonNull(thresholds, "thresholds cannot be null");

        EnumSet<Dimension> active = EnumSet.noneOf(Dimension.class);
        if (thresholds.minimumHealthyRatio() != null
                && observation.healthyRatio() < thresholds.minimumHealthyRatio()) {
            active.add(Dimension.HEALTH);
        }
        if (thresholds.utilizationAtLeast() != null
                && observation.utilization() >= thresholds.utilizationAtLeast()) {
            active.add(Dimension.UTILIZATION);
        }
        if (thresholds.queueDepthAbove() != null
                && observation.queueDepth() > thresholds.queueDepthAbove()) {
            active.add(Dimension.QUEUE);
        }
        if (thresholds.p95LatencyAbove() != null
                && observation.p95LatencyMillis() > thresholds.p95LatencyAbove()) {
            active.add(Dimension.P95_LATENCY);
        }
        if (thresholds.p99LatencyAbove() != null
                && observation.p99LatencyMillis() > thresholds.p99LatencyAbove()) {
            active.add(Dimension.P99_LATENCY);
        }
        if (thresholds.errorRateAbove() != null
                && observation.errorRate() > thresholds.errorRateAbove()) {
            active.add(Dimension.ERROR_RATE);
        }
        return new Assessment(active);
    }

    static Observation from(FailureScenarioSignal signal) {
        Objects.requireNonNull(signal, "signal cannot be null");
        return new Observation(
                signal.healthyRatio(),
                signal.utilization(),
                signal.queueDepth(),
                signal.observedP95LatencyMillis(),
                signal.observedP99LatencyMillis(),
                signal.observedErrorRate());
    }

    static Observation from(LoadSheddingSignal signal) {
        Objects.requireNonNull(signal, "signal cannot be null");
        return new Observation(
                1.0,
                signal.utilization(),
                signal.queueDepth(),
                signal.observedP95LatencyMillis(),
                0.0,
                signal.observedErrorRate());
    }

    static Observation from(AutoscalingSignal signal) {
        Objects.requireNonNull(signal, "signal cannot be null");
        return new Observation(
                1.0,
                signal.utilization(),
                signal.queueDepth(),
                signal.observedP95LatencyMillis(),
                signal.observedP99LatencyMillis(),
                signal.observedErrorRate());
    }

    static Thresholds forFailureScenario(FailureScenarioConfig config) {
        Objects.requireNonNull(config, "config cannot be null");
        return new Thresholds(
                config.partialOutageHealthyRatioThreshold(),
                config.saturationUtilizationThreshold(),
                config.highQueueDepthThreshold(),
                config.highP95LatencyMillis(),
                config.highP99LatencyMillis(),
                config.highErrorRate());
    }

    static Thresholds forHardLoadShedding(LoadSheddingConfig config) {
        Objects.requireNonNull(config, "config cannot be null");
        return new Thresholds(
                null,
                config.hardUtilizationThreshold(),
                config.maxQueueDepth(),
                config.maxP95LatencyMillis(),
                null,
                config.maxErrorRate());
    }

    static Thresholds forSoftLoadShedding(LoadSheddingConfig config) {
        Objects.requireNonNull(config, "config cannot be null");
        return Thresholds.utilizationOnly(config.softUtilizationThreshold());
    }

    static Thresholds forScaleUp(ShadowAutoscalerConfig config) {
        Objects.requireNonNull(config, "config cannot be null");
        return new Thresholds(
                null,
                config.utilizationScaleUpThreshold(),
                config.queueScaleUpThreshold(),
                config.targetP95LatencyMillis(),
                config.targetP99LatencyMillis(),
                config.maxErrorRate());
    }

    public enum Dimension {
        HEALTH,
        UTILIZATION,
        QUEUE,
        P95_LATENCY,
        P99_LATENCY,
        ERROR_RATE
    }

    public record Observation(
            double healthyRatio,
            double utilization,
            int queueDepth,
            double p95LatencyMillis,
            double p99LatencyMillis,
            double errorRate) {

        public Observation {
            requireRate(healthyRatio, "healthyRatio");
            requireFiniteNonNegative(utilization, "utilization");
            if (queueDepth < 0) {
                throw new IllegalArgumentException("queueDepth must be non-negative");
            }
            requireFiniteNonNegative(p95LatencyMillis, "p95LatencyMillis");
            requireFiniteNonNegative(p99LatencyMillis, "p99LatencyMillis");
            requireRate(errorRate, "errorRate");
        }
    }

    public record Thresholds(
            Double minimumHealthyRatio,
            Double utilizationAtLeast,
            Integer queueDepthAbove,
            Double p95LatencyAbove,
            Double p99LatencyAbove,
            Double errorRateAbove) {

        public Thresholds {
            requireOptionalRate(minimumHealthyRatio, "minimumHealthyRatio");
            requireOptionalFiniteNonNegative(utilizationAtLeast, "utilizationAtLeast");
            if (queueDepthAbove != null && queueDepthAbove < 0) {
                throw new IllegalArgumentException("queueDepthAbove must be non-negative");
            }
            requireOptionalFiniteNonNegative(p95LatencyAbove, "p95LatencyAbove");
            requireOptionalFiniteNonNegative(p99LatencyAbove, "p99LatencyAbove");
            requireOptionalRate(errorRateAbove, "errorRateAbove");
        }

        public static Thresholds utilizationOnly(double threshold) {
            return new Thresholds(null, threshold, null, null, null, null);
        }
    }

    public record Assessment(Set<Dimension> activeDimensions) {
        public Assessment {
            Objects.requireNonNull(activeDimensions, "activeDimensions cannot be null");
            EnumSet<Dimension> copy = activeDimensions.isEmpty()
                    ? EnumSet.noneOf(Dimension.class)
                    : EnumSet.copyOf(activeDimensions);
            activeDimensions = Collections.unmodifiableSet(copy);
        }

        public boolean has(Dimension dimension) {
            return activeDimensions.contains(Objects.requireNonNull(dimension, "dimension cannot be null"));
        }

        public boolean hasPressure() {
            return !activeDimensions.isEmpty();
        }
    }

    private static void requireOptionalRate(Double value, String fieldName) {
        if (value != null) {
            requireRate(value, fieldName);
        }
    }

    private static void requireOptionalFiniteNonNegative(Double value, String fieldName) {
        if (value != null) {
            requireFiniteNonNegative(value, fieldName);
        }
    }

    private static void requireRate(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
        }
    }

    private static void requireFiniteNonNegative(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative");
        }
    }
}
