package com.richmond423.loadbalancerpro.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

public record ServerRollingSignalState(
        String serverId,
        int sampleCount,
        int successCount,
        int failureCount,
        int timeoutCount,
        int connectionFailureCount,
        double successRate,
        double failureRate,
        double timeoutRate,
        double connectionFailureRate,
        int consecutiveFailureCount,
        int consecutiveSuccessCount,
        LatencyWindowSignal latencyWindowSignal,
        ServerSignalEvidence evidence,
        ServerSignalConfidence confidence,
        ServerDegradationState degradationState,
        boolean recovering,
        Optional<Instant> latestObservationAt,
        List<ServerObservationSource> sources,
        List<String> reasons,
        Instant evaluatedAt) {

    public ServerRollingSignalState {
        serverId = requireNonBlank(serverId, "serverId");
        requireNonNegative(sampleCount, "sampleCount");
        requireNonNegative(successCount, "successCount");
        requireNonNegative(failureCount, "failureCount");
        requireNonNegative(timeoutCount, "timeoutCount");
        requireNonNegative(connectionFailureCount, "connectionFailureCount");
        requireRate(successRate, "successRate");
        requireRate(failureRate, "failureRate");
        requireRate(timeoutRate, "timeoutRate");
        requireRate(connectionFailureRate, "connectionFailureRate");
        requireNonNegative(consecutiveFailureCount, "consecutiveFailureCount");
        requireNonNegative(consecutiveSuccessCount, "consecutiveSuccessCount");
        Objects.requireNonNull(latencyWindowSignal, "latencyWindowSignal cannot be null");
        Objects.requireNonNull(evidence, "evidence cannot be null");
        Objects.requireNonNull(confidence, "confidence cannot be null");
        Objects.requireNonNull(degradationState, "degradationState cannot be null");
        Objects.requireNonNull(latestObservationAt, "latestObservationAt cannot be null");
        Objects.requireNonNull(sources, "sources cannot be null");
        Objects.requireNonNull(reasons, "reasons cannot be null");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt cannot be null");
        sources = List.copyOf(sources);
        reasons = List.copyOf(reasons);
        if (sampleCount != successCount + failureCount) {
            throw new IllegalArgumentException("sampleCount must equal successCount plus failureCount");
        }
        requireDerivedRate(successRate, successCount, sampleCount, "successRate");
        requireDerivedRate(failureRate, failureCount, sampleCount, "failureRate");
        requireDerivedRate(timeoutRate, timeoutCount, sampleCount, "timeoutRate");
        requireDerivedRate(connectionFailureRate, connectionFailureCount, sampleCount, "connectionFailureRate");
        if (timeoutCount > failureCount || connectionFailureCount > failureCount) {
            throw new IllegalArgumentException("specialized failure counts cannot exceed failureCount");
        }
        if (consecutiveFailureCount > sampleCount || consecutiveSuccessCount > sampleCount) {
            throw new IllegalArgumentException("consecutive counts cannot exceed sampleCount");
        }
        if (consecutiveFailureCount > 0 && consecutiveSuccessCount > 0) {
            throw new IllegalArgumentException("only one consecutive outcome count can be positive");
        }
        if (latencyWindowSignal.sampleCount() > sampleCount) {
            throw new IllegalArgumentException("latency sample count cannot exceed sampleCount");
        }
        if (sampleCount == 0 && !sources.isEmpty()) {
            throw new IllegalArgumentException("sources must be empty when no fresh samples are present");
        }
        if (sampleCount > 0 && sources.isEmpty()) {
            throw new IllegalArgumentException("sources must be present when fresh samples are present");
        }
        boolean noFreshEvidence = evidence == ServerSignalEvidence.MISSING || evidence == ServerSignalEvidence.STALE;
        if (noFreshEvidence != (sampleCount == 0)) {
            throw new IllegalArgumentException("evidence must agree with fresh sample presence");
        }
        if ((evidence == ServerSignalEvidence.MISSING) != latestObservationAt.isEmpty()) {
            throw new IllegalArgumentException("missing evidence must agree with latest observation presence");
        }
        if ((confidence == ServerSignalConfidence.NONE) != noFreshEvidence) {
            throw new IllegalArgumentException("confidence must agree with evidence freshness");
        }
        if ((degradationState == ServerDegradationState.UNKNOWN) != noFreshEvidence) {
            throw new IllegalArgumentException("degradation state must agree with evidence freshness");
        }
        if (recovering != (degradationState == ServerDegradationState.RECOVERING)) {
            throw new IllegalArgumentException("recovering must agree with degradationState");
        }
        if (latestObservationAt.isPresent() && latestObservationAt.orElseThrow().isAfter(evaluatedAt)) {
            throw new IllegalArgumentException("latestObservationAt cannot be after evaluatedAt");
        }
    }

    static ServerRollingSignalState fromObservations(
            String serverId,
            List<ServerObservation> observations,
            ServerObservationWindowPolicy policy,
            Instant evaluatedAt) {
        Objects.requireNonNull(observations, "observations cannot be null");
        Objects.requireNonNull(policy, "policy cannot be null");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt cannot be null");
        String normalizedServerId = requireNonBlank(serverId, "serverId");

        List<ServerObservation> ordered = observations.stream()
                .sorted(Comparator.comparing(ServerObservation::observedAt)
                        .thenComparing(ServerObservation::observationId))
                .toList();
        for (ServerObservation observation : ordered) {
            if (!normalizedServerId.equals(observation.serverId())) {
                throw new IllegalArgumentException("observation serverId must match state serverId");
            }
            if (observation.observedAt().isAfter(evaluatedAt)) {
                throw new IllegalArgumentException("observation timestamp cannot be after evaluatedAt");
            }
        }

        Optional<Instant> latestObservationAt = ordered.isEmpty()
                ? Optional.empty()
                : Optional.of(ordered.get(ordered.size() - 1).observedAt());
        Instant cutoff = evaluatedAt.minus(policy.maxSampleAge());
        List<ServerObservation> fresh = ordered.stream()
                .filter(observation -> !observation.observedAt().isBefore(cutoff))
                .toList();

        int sampleCount = fresh.size();
        int successCount = count(fresh, ServerObservationOutcome.SUCCESS);
        int timeoutCount = count(fresh, ServerObservationOutcome.TIMEOUT);
        int connectionFailureCount = count(fresh, ServerObservationOutcome.CONNECTION_FAILURE);
        int failureCount = sampleCount - successCount;
        double successRate = rate(successCount, sampleCount);
        double failureRate = rate(failureCount, sampleCount);
        double timeoutRate = rate(timeoutCount, sampleCount);
        double connectionFailureRate = rate(connectionFailureCount, sampleCount);
        int consecutiveFailureCount = consecutiveCount(fresh, true);
        int consecutiveSuccessCount = consecutiveCount(fresh, false);

        List<Double> latencySamples = fresh.stream()
                .filter(observation -> observation.latencyMillis().isPresent())
                .map(observation -> observation.latencyMillis().getAsDouble())
                .toList();
        LatencyWindowSignal latencyWindowSignal = latencySignal(latencySamples, policy);
        ServerSignalEvidence evidence = evidence(ordered, fresh, policy);
        ServerSignalConfidence confidence = confidence(evidence, sampleCount, latencyWindowSignal, policy);
        boolean recovering = failureCount > 0
                && consecutiveSuccessCount > 0
                && consecutiveSuccessCount < policy.recoverySuccessThreshold();
        ServerDegradationState degradationState = degradationState(
                evidence,
                failureRate,
                failureCount,
                timeoutCount,
                connectionFailureCount,
                consecutiveFailureCount,
                recovering,
                policy);
        List<ServerObservationSource> sources = sources(fresh);
        List<String> reasons = reasons(evidence, latencyWindowSignal, degradationState, sampleCount, policy);

        return new ServerRollingSignalState(
                normalizedServerId,
                sampleCount,
                successCount,
                failureCount,
                timeoutCount,
                connectionFailureCount,
                successRate,
                failureRate,
                timeoutRate,
                connectionFailureRate,
                consecutiveFailureCount,
                consecutiveSuccessCount,
                latencyWindowSignal,
                evidence,
                confidence,
                degradationState,
                recovering,
                latestObservationAt,
                sources,
                reasons,
                evaluatedAt);
    }

    public boolean sufficientEvidence() {
        return evidence == ServerSignalEvidence.SUFFICIENT;
    }

    public boolean stale() {
        return evidence == ServerSignalEvidence.STALE;
    }

    public boolean missing() {
        return evidence == ServerSignalEvidence.MISSING;
    }

    public boolean recommendationEligible() {
        return sufficientEvidence()
                && (degradationState == ServerDegradationState.HEALTHY
                        || degradationState == ServerDegradationState.PARTIALLY_DEGRADED);
    }

    public NetworkAwarenessSignal networkAwarenessSignal() {
        double jitter = 0.0;
        if (latencyWindowSignal.rollingP95LatencyMillis().isPresent()
                && latencyWindowSignal.rollingP99LatencyMillis().isPresent()) {
            jitter = Math.max(0.0,
                    latencyWindowSignal.rollingP99LatencyMillis().getAsDouble()
                            - latencyWindowSignal.rollingP95LatencyMillis().getAsDouble());
        }
        return new NetworkAwarenessSignal(
                serverId,
                timeoutRate,
                0.0,
                connectionFailureRate,
                jitter,
                consecutiveFailureCount > 1,
                timeoutCount,
                sampleCount,
                evaluatedAt);
    }

    private static LatencyWindowSignal latencySignal(
            List<Double> latencySamples,
            ServerObservationWindowPolicy policy) {
        LatencyWindowSignal complete = LatencyWindowSignal.fromSamples(
                latencySamples,
                policy.ewmaAlpha(),
                policy.maxSampleCount());
        if (complete.sampleCount() >= policy.minimumTailLatencySamples()) {
            return complete;
        }
        return new LatencyWindowSignal(
                complete.sampleCount(),
                complete.ewmaLatencyMillis(),
                complete.rollingAverageLatencyMillis(),
                OptionalDouble.empty(),
                OptionalDouble.empty());
    }

    private static ServerSignalEvidence evidence(
            List<ServerObservation> ordered,
            List<ServerObservation> fresh,
            ServerObservationWindowPolicy policy) {
        if (ordered.isEmpty()) {
            return ServerSignalEvidence.MISSING;
        }
        if (fresh.isEmpty()) {
            return ServerSignalEvidence.STALE;
        }
        if (fresh.size() < policy.minimumEvidenceSamples()) {
            return ServerSignalEvidence.SPARSE;
        }
        return ServerSignalEvidence.SUFFICIENT;
    }

    private static ServerSignalConfidence confidence(
            ServerSignalEvidence evidence,
            int sampleCount,
            LatencyWindowSignal latencyWindowSignal,
            ServerObservationWindowPolicy policy) {
        if (evidence == ServerSignalEvidence.MISSING || evidence == ServerSignalEvidence.STALE) {
            return ServerSignalConfidence.NONE;
        }
        if (evidence == ServerSignalEvidence.SPARSE) {
            return ServerSignalConfidence.LOW;
        }
        long highConfidenceThreshold = (long) policy.minimumEvidenceSamples() * 2L;
        if (sampleCount >= highConfidenceThreshold && latencyWindowSignal.hasTailLatencyWindowValues()) {
            return ServerSignalConfidence.HIGH;
        }
        return ServerSignalConfidence.MEDIUM;
    }

    private static ServerDegradationState degradationState(
            ServerSignalEvidence evidence,
            double failureRate,
            int failureCount,
            int timeoutCount,
            int connectionFailureCount,
            int consecutiveFailureCount,
            boolean recovering,
            ServerObservationWindowPolicy policy) {
        if (evidence == ServerSignalEvidence.MISSING || evidence == ServerSignalEvidence.STALE) {
            return ServerDegradationState.UNKNOWN;
        }
        if (consecutiveFailureCount >= policy.consecutiveFailureThreshold()) {
            return ServerDegradationState.FAILED;
        }
        if (recovering) {
            return ServerDegradationState.RECOVERING;
        }
        if (evidence == ServerSignalEvidence.SUFFICIENT
                && failureRate >= policy.failedFailureRateThreshold()) {
            return ServerDegradationState.FAILED;
        }
        if (failureCount > 0
                && (failureRate >= policy.partialFailureRateThreshold()
                        || timeoutCount > 0
                        || connectionFailureCount > 0)) {
            return ServerDegradationState.PARTIALLY_DEGRADED;
        }
        return ServerDegradationState.HEALTHY;
    }

    private static List<String> reasons(
            ServerSignalEvidence evidence,
            LatencyWindowSignal latencyWindowSignal,
            ServerDegradationState degradationState,
            int sampleCount,
            ServerObservationWindowPolicy policy) {
        List<String> reasons = new ArrayList<>();
        switch (evidence) {
            case MISSING -> reasons.add("no observations available");
            case STALE -> reasons.add("latest observation exceeds max sample age");
            case SPARSE -> reasons.add("fresh sample count " + sampleCount + " is below minimum "
                    + policy.minimumEvidenceSamples());
            case SUFFICIENT -> reasons.add("fresh sample count meets minimum evidence threshold");
        }
        if (latencyWindowSignal.sampleCount() == 0) {
            reasons.add("no latency samples available");
        } else if (latencyWindowSignal.hasTailLatencyWindowValues()) {
            reasons.add("tail latency sample threshold met");
        } else {
            reasons.add("tail latency withheld until minimum sample threshold");
        }
        reasons.add("degradation state is " + degradationState.name().toLowerCase().replace('_', '-'));
        return List.copyOf(reasons);
    }

    private static List<ServerObservationSource> sources(List<ServerObservation> observations) {
        EnumSet<ServerObservationSource> sources = EnumSet.noneOf(ServerObservationSource.class);
        observations.forEach(observation -> sources.add(observation.source()));
        return List.copyOf(sources);
    }

    private static int count(List<ServerObservation> observations, ServerObservationOutcome outcome) {
        return (int) observations.stream().filter(observation -> observation.outcome() == outcome).count();
    }

    private static int consecutiveCount(List<ServerObservation> observations, boolean failures) {
        int count = 0;
        for (int index = observations.size() - 1; index >= 0; index--) {
            boolean matches = failures
                    ? observations.get(index).outcome().failed()
                    : observations.get(index).outcome().successful();
            if (!matches) {
                break;
            }
            count++;
        }
        return count;
    }

    private static double rate(int count, int sampleCount) {
        return sampleCount == 0 ? 0.0 : (double) count / sampleCount;
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

    private static void requireRate(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
        }
    }

    private static void requireDerivedRate(double value, int count, int sampleCount, String fieldName) {
        double expected = rate(count, sampleCount);
        if (Double.compare(value, expected) != 0) {
            throw new IllegalArgumentException(fieldName + " must match its count and sampleCount");
        }
    }
}
