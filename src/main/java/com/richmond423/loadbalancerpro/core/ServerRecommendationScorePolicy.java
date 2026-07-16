package com.richmond423.loadbalancerpro.core;

public record ServerRecommendationScorePolicy(
        double maximumLatencyMillis,
        double maximumJitterMillis,
        double ineligiblePenaltyWeight) {

    public ServerRecommendationScorePolicy {
        requirePositive(maximumLatencyMillis, "maximumLatencyMillis");
        requirePositive(maximumJitterMillis, "maximumJitterMillis");
        requirePositive(ineligiblePenaltyWeight, "ineligiblePenaltyWeight");
    }

    public static ServerRecommendationScorePolicy localLabDefaults() {
        return new ServerRecommendationScorePolicy(1_000.0, 500.0, 100.0);
    }

    private static void requirePositive(double value, String fieldName) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and positive");
        }
    }
}
