package com.richmond423.loadbalancerpro.core;

import java.time.Duration;
import java.util.Objects;

public record ServerObservationWindowPolicy(
        int maxSampleCount,
        Duration maxSampleAge,
        int minimumEvidenceSamples,
        int minimumTailLatencySamples,
        double partialFailureRateThreshold,
        double failedFailureRateThreshold,
        int consecutiveFailureThreshold,
        int recoverySuccessThreshold,
        double ewmaAlpha) {

    public ServerObservationWindowPolicy {
        requirePositive(maxSampleCount, "maxSampleCount");
        Objects.requireNonNull(maxSampleAge, "maxSampleAge cannot be null");
        if (maxSampleAge.isZero() || maxSampleAge.isNegative()) {
            throw new IllegalArgumentException("maxSampleAge must be positive");
        }
        requireWithinWindow(minimumEvidenceSamples, maxSampleCount, "minimumEvidenceSamples");
        requireWithinWindow(minimumTailLatencySamples, maxSampleCount, "minimumTailLatencySamples");
        requireRate(partialFailureRateThreshold, "partialFailureRateThreshold");
        requireRate(failedFailureRateThreshold, "failedFailureRateThreshold");
        if (failedFailureRateThreshold < partialFailureRateThreshold) {
            throw new IllegalArgumentException(
                    "failedFailureRateThreshold must be greater than or equal to partialFailureRateThreshold");
        }
        requireWithinWindow(consecutiveFailureThreshold, maxSampleCount, "consecutiveFailureThreshold");
        requireWithinWindow(recoverySuccessThreshold, maxSampleCount, "recoverySuccessThreshold");
        if (!Double.isFinite(ewmaAlpha) || ewmaAlpha <= 0.0 || ewmaAlpha > 1.0) {
            throw new IllegalArgumentException(
                    "ewmaAlpha must be finite and between 0.0 exclusive and 1.0 inclusive");
        }
    }

    public static ServerObservationWindowPolicy localLabDefaults() {
        return new ServerObservationWindowPolicy(
                64,
                Duration.ofSeconds(30),
                5,
                5,
                0.10,
                0.50,
                3,
                3,
                0.30);
    }

    private static void requirePositive(int value, String fieldName) {
        if (value < 1) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
    }

    private static void requireWithinWindow(int value, int maxSampleCount, String fieldName) {
        requirePositive(value, fieldName);
        if (value > maxSampleCount) {
            throw new IllegalArgumentException(fieldName + " cannot exceed maxSampleCount");
        }
    }

    private static void requireRate(double value, String fieldName) {
        if (!Double.isFinite(value) || value <= 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 exclusive and 1.0 inclusive");
        }
    }
}
