package com.richmond423.loadbalancerpro.core;

import java.util.Objects;

public record AdaptiveTrafficDecisionPolicy(
        int maximumCandidateCount,
        ServerObservationWindowPolicy observationWindowPolicy,
        ServerRecommendationScorePolicy scorePolicy,
        TrafficAllocationPolicy allocationPolicy,
        TrafficAllocationGuardrailPolicy guardrailPolicy) {

    public AdaptiveTrafficDecisionPolicy {
        if (maximumCandidateCount < 1) {
            throw new IllegalArgumentException("maximumCandidateCount must be greater than zero");
        }
        Objects.requireNonNull(observationWindowPolicy, "observationWindowPolicy cannot be null");
        Objects.requireNonNull(scorePolicy, "scorePolicy cannot be null");
        Objects.requireNonNull(allocationPolicy, "allocationPolicy cannot be null");
        Objects.requireNonNull(guardrailPolicy, "guardrailPolicy cannot be null");
    }

    public static AdaptiveTrafficDecisionPolicy localLabDefaults() {
        return new AdaptiveTrafficDecisionPolicy(
                32,
                ServerObservationWindowPolicy.localLabDefaults(),
                ServerRecommendationScorePolicy.localLabDefaults(),
                TrafficAllocationPolicy.localLabDefaults(),
                TrafficAllocationGuardrailPolicy.localLabDefaults());
    }
}
