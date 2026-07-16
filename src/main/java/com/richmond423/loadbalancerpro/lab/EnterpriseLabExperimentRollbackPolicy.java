package com.richmond423.loadbalancerpro.lab;

/**
 * Bounded thresholds carried by lifecycle configuration for the PR4 evaluator.
 */
public record EnterpriseLabExperimentRollbackPolicy(
        double maximumFailureRate,
        double maximumTimeoutRate,
        double maximumLatencyRegressionRatio,
        int minimumHealthyBackends,
        int maximumConsecutiveTransportFailures,
        int maximumPartiallyDegradedBackends,
        double maximumObservationLossRate) {

    public EnterpriseLabExperimentRollbackPolicy {
        requireRate(maximumFailureRate, "maximumFailureRate");
        requireRate(maximumTimeoutRate, "maximumTimeoutRate");
        if (!Double.isFinite(maximumLatencyRegressionRatio)
                || maximumLatencyRegressionRatio < 1.0
                || maximumLatencyRegressionRatio > 10.0) {
            throw new IllegalArgumentException(
                    "maximumLatencyRegressionRatio must be finite and between 1.0 and 10.0");
        }
        if (minimumHealthyBackends < 1
                || minimumHealthyBackends > EnterpriseLabLoopbackAllocationSnapshot.HARD_MAX_BACKENDS) {
            throw new IllegalArgumentException("minimumHealthyBackends must be between 1 and 64");
        }
        if (maximumConsecutiveTransportFailures < 1 || maximumConsecutiveTransportFailures > 1_000) {
            throw new IllegalArgumentException(
                    "maximumConsecutiveTransportFailures must be between 1 and 1000");
        }
        if (maximumPartiallyDegradedBackends < 0
                || maximumPartiallyDegradedBackends > EnterpriseLabLoopbackAllocationSnapshot.HARD_MAX_BACKENDS) {
            throw new IllegalArgumentException("maximumPartiallyDegradedBackends must be between 0 and 64");
        }
        requireRate(maximumObservationLossRate, "maximumObservationLossRate");
    }

    public static EnterpriseLabExperimentRollbackPolicy localLabDefaults() {
        return new EnterpriseLabExperimentRollbackPolicy(0.25, 0.10, 1.50, 2, 3, 0, 0.10);
    }

    private static void requireRate(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and between 0.0 and 1.0");
        }
    }
}
