package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.ServerDegradationState;
import com.richmond423.loadbalancerpro.core.ServerSignalEvidence;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;

/**
 * Immutable, fingerprint-chained evidence for one deterministic local experiment evaluation.
 */
public record EnterpriseLabExperimentEvaluation(
        String schemaVersion,
        long sequence,
        String evaluationId,
        String experimentId,
        Instant evaluatedAt,
        Disposition disposition,
        List<Trigger> triggers,
        Map<String, BackendEvidence> backendEvidence,
        int correlatedObservationCount,
        int lifecycleRequestCount,
        int lifecycleEvidenceCount,
        int healthyBackendCount,
        double aggregateFailureRate,
        double aggregateTimeoutRate,
        double observationLossRate,
        OptionalDouble candidateP95LatencyMillis,
        OptionalDouble candidateP99LatencyMillis,
        OptionalDouble baselineP95LatencyMillis,
        OptionalDouble baselineP99LatencyMillis,
        OptionalDouble maximumLatencyRegressionRatio,
        boolean allocationGuardrailValid,
        boolean baselineRestorationAttempted,
        boolean baselineRestorationSucceeded,
        EnterpriseLabExperimentState stateBefore,
        EnterpriseLabExperimentState stateAfter,
        long allocationRevisionBefore,
        long allocationRevisionAfter,
        String observationBaselineFingerprint,
        String previousFingerprint,
        String reason) {
    public static final String SCHEMA_VERSION = "enterprise-lab-experiment-evaluation/v1";
    public static final String GENESIS_FINGERPRINT = "GENESIS";
    private static final int MAX_ID_LENGTH = 128;
    private static final int MAX_REASON_LENGTH = 256;

    public EnterpriseLabExperimentEvaluation {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported experiment evaluation schemaVersion");
        }
        if (sequence < 1) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        evaluationId = requireCanonicalId(evaluationId, "evaluationId");
        experimentId = requireCanonicalId(experimentId, "experimentId");
        evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt cannot be null");
        disposition = Objects.requireNonNull(disposition, "disposition cannot be null");
        triggers = List.copyOf(Objects.requireNonNull(triggers, "triggers cannot be null"));
        if (triggers.size() > Trigger.values().length || triggers.stream().distinct().count() != triggers.size()) {
            throw new IllegalArgumentException("evaluation triggers must be bounded and unique");
        }
        Objects.requireNonNull(backendEvidence, "backendEvidence cannot be null");
        if (backendEvidence.isEmpty()
                || backendEvidence.size() > EnterpriseLabLoopbackAllocationSnapshot.HARD_MAX_BACKENDS) {
            throw new IllegalArgumentException("backend evidence count must be between 1 and 64");
        }
        Map<String, BackendEvidence> sorted = new TreeMap<>();
        backendEvidence.forEach((backendId, evidence) -> {
            String safeBackendId = requireCanonicalId(backendId, "backend evidence key");
            BackendEvidence safeEvidence = Objects.requireNonNull(evidence, "backend evidence cannot be null");
            if (!safeBackendId.equals(safeEvidence.backendId())) {
                throw new IllegalArgumentException("backend evidence key must match backendId");
            }
            sorted.put(safeBackendId, safeEvidence);
        });
        backendEvidence = Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
        if (correlatedObservationCount < 0 || lifecycleRequestCount < 0 || lifecycleEvidenceCount < 0
                || lifecycleEvidenceCount > lifecycleRequestCount || healthyBackendCount < 0
                || healthyBackendCount > backendEvidence.size()) {
            throw new IllegalArgumentException("evaluation counters are inconsistent");
        }
        requireRate(aggregateFailureRate, "aggregateFailureRate");
        requireRate(aggregateTimeoutRate, "aggregateTimeoutRate");
        requireRate(observationLossRate, "observationLossRate");
        candidateP95LatencyMillis = requireLatency(candidateP95LatencyMillis, "candidateP95LatencyMillis");
        candidateP99LatencyMillis = requireLatency(candidateP99LatencyMillis, "candidateP99LatencyMillis");
        baselineP95LatencyMillis = requireLatency(baselineP95LatencyMillis, "baselineP95LatencyMillis");
        baselineP99LatencyMillis = requireLatency(baselineP99LatencyMillis, "baselineP99LatencyMillis");
        maximumLatencyRegressionRatio = requireRatio(
                maximumLatencyRegressionRatio, "maximumLatencyRegressionRatio");
        if (candidateP95LatencyMillis.isPresent() != candidateP99LatencyMillis.isPresent()
                || baselineP95LatencyMillis.isPresent() != baselineP99LatencyMillis.isPresent()) {
            throw new IllegalArgumentException("p95 and p99 latency evidence must be present together");
        }
        if (baselineRestorationSucceeded && !baselineRestorationAttempted) {
            throw new IllegalArgumentException("baseline restoration success requires an attempt");
        }
        stateBefore = Objects.requireNonNull(stateBefore, "stateBefore cannot be null");
        stateAfter = Objects.requireNonNull(stateAfter, "stateAfter cannot be null");
        if (allocationRevisionBefore < 0 || allocationRevisionAfter < 0) {
            throw new IllegalArgumentException("allocation revisions cannot be negative");
        }
        observationBaselineFingerprint = requireSha256(
                observationBaselineFingerprint, "observationBaselineFingerprint");
        previousFingerprint = requirePreviousFingerprint(previousFingerprint);
        reason = requireReason(reason);
    }

    public String contentFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, schemaVersion);
            update(digest, Long.toString(sequence));
            update(digest, evaluationId);
            update(digest, experimentId);
            update(digest, evaluatedAt.toString());
            update(digest, disposition.name());
            triggers.forEach(trigger -> update(digest, trigger.name()));
            backendEvidence.forEach((backendId, evidence) -> {
                update(digest, backendId);
                update(digest, evidence.toString());
            });
            update(digest, Integer.toString(correlatedObservationCount));
            update(digest, Integer.toString(lifecycleRequestCount));
            update(digest, Integer.toString(lifecycleEvidenceCount));
            update(digest, Integer.toString(healthyBackendCount));
            update(digest, Double.toString(aggregateFailureRate));
            update(digest, Double.toString(aggregateTimeoutRate));
            update(digest, Double.toString(observationLossRate));
            update(digest, candidateP95LatencyMillis.toString());
            update(digest, candidateP99LatencyMillis.toString());
            update(digest, baselineP95LatencyMillis.toString());
            update(digest, baselineP99LatencyMillis.toString());
            update(digest, maximumLatencyRegressionRatio.toString());
            update(digest, Boolean.toString(allocationGuardrailValid));
            update(digest, Boolean.toString(baselineRestorationAttempted));
            update(digest, Boolean.toString(baselineRestorationSucceeded));
            update(digest, stateBefore.name());
            update(digest, stateAfter.name());
            update(digest, Long.toString(allocationRevisionBefore));
            update(digest, Long.toString(allocationRevisionAfter));
            update(digest, observationBaselineFingerprint);
            update(digest, previousFingerprint);
            update(digest, reason);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public enum Disposition {
        CONTINUE_RUNNING,
        CONTINUE_HOLDING,
        COMPLETED,
        ROLLED_BACK_HARMFUL,
        ROLLED_BACK_INSUFFICIENT,
        ROLLED_BACK_INVARIANT,
        CANCELLED,
        CANCELLED_AND_ROLLED_BACK,
        FAILED_CLOSED,
        TERMINAL_NO_CHANGE
    }

    public enum Trigger {
        FAILURE_RATE,
        TIMEOUT_RATE,
        LATENCY_REGRESSION,
        PARTIAL_DEGRADATION,
        HEALTHY_BACKEND_FLOOR,
        CONSECUTIVE_TRANSPORT_FAILURES,
        STALE_EVIDENCE,
        MISSING_EVIDENCE,
        SPARSE_EVIDENCE_AT_BOUNDARY,
        OBSERVATION_CAPTURE_LOSS,
        GUARDRAIL_VIOLATION,
        DURATION_EXCEEDED,
        REQUEST_LIMIT_EXCEEDED,
        LIFECYCLE_INVARIANT,
        NO_VIABLE_FALLBACK,
        OPERATOR_CANCELLATION,
        BASELINE_RESTORATION_FAILED,
        INTERNAL_EVALUATION_FAILURE
    }

    public record BackendEvidence(
            String backendId,
            int sampleCount,
            int successCount,
            int failureCount,
            int timeoutCount,
            int connectionFailureCount,
            double failureRate,
            double timeoutRate,
            int consecutiveTransportFailureCount,
            ServerSignalEvidence evidence,
            ServerDegradationState degradationState,
            OptionalDouble p95LatencyMillis,
            OptionalDouble p99LatencyMillis,
            OptionalDouble baselineP95LatencyMillis,
            OptionalDouble baselineP99LatencyMillis,
            OptionalDouble latencyRegressionRatio,
            Optional<Instant> latestObservationAt) {

        public BackendEvidence {
            backendId = requireCanonicalId(backendId, "backendId");
            if (sampleCount < 0 || successCount < 0 || failureCount < 0 || timeoutCount < 0
                    || connectionFailureCount < 0 || sampleCount != successCount + failureCount
                    || timeoutCount > failureCount || connectionFailureCount > failureCount
                    || consecutiveTransportFailureCount < 0 || consecutiveTransportFailureCount > sampleCount) {
                throw new IllegalArgumentException("backend evidence counters are inconsistent");
            }
            requireRate(failureRate, "failureRate");
            requireRate(timeoutRate, "timeoutRate");
            evidence = Objects.requireNonNull(evidence, "evidence cannot be null");
            degradationState = Objects.requireNonNull(degradationState, "degradationState cannot be null");
            p95LatencyMillis = requireLatency(p95LatencyMillis, "p95LatencyMillis");
            p99LatencyMillis = requireLatency(p99LatencyMillis, "p99LatencyMillis");
            baselineP95LatencyMillis = requireLatency(baselineP95LatencyMillis, "baselineP95LatencyMillis");
            baselineP99LatencyMillis = requireLatency(baselineP99LatencyMillis, "baselineP99LatencyMillis");
            latencyRegressionRatio = requireRatio(latencyRegressionRatio, "latencyRegressionRatio");
            latestObservationAt = Objects.requireNonNull(latestObservationAt, "latestObservationAt cannot be null");
            if (p95LatencyMillis.isPresent() != p99LatencyMillis.isPresent()
                    || baselineP95LatencyMillis.isPresent() != baselineP99LatencyMillis.isPresent()) {
                throw new IllegalArgumentException("backend p95 and p99 latency must be present together");
            }
            if (p95LatencyMillis.isPresent()
                    && p99LatencyMillis.getAsDouble() < p95LatencyMillis.getAsDouble()) {
                throw new IllegalArgumentException("backend p99 latency cannot be below p95 latency");
            }
            if (baselineP95LatencyMillis.isPresent()
                    && baselineP99LatencyMillis.getAsDouble() < baselineP95LatencyMillis.getAsDouble()) {
                throw new IllegalArgumentException("backend baseline p99 latency cannot be below p95 latency");
            }
        }
    }

    private static OptionalDouble requireLatency(OptionalDouble value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.isPresent() && (!Double.isFinite(value.getAsDouble()) || value.getAsDouble() < 0.0)) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative");
        }
        return value;
    }

    private static OptionalDouble requireRatio(OptionalDouble value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.isPresent() && (!Double.isFinite(value.getAsDouble()) || value.getAsDouble() < 0.0)) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative");
        }
        return value;
    }

    private static void requireRate(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and between 0.0 and 1.0");
        }
    }

    private static String requireCanonicalId(String value, String fieldName) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > MAX_ID_LENGTH || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException(fieldName + " must be a bounded canonical identifier");
        }
        return value;
    }

    private static String requireReason(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("reason cannot be null or blank");
        }
        String safe = value.replace('\r', ' ').replace('\n', ' ').trim();
        if (safe.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("reason cannot exceed 256 characters");
        }
        return safe;
    }

    private static String requireSha256(String value, String fieldName) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(fieldName + " must be lowercase SHA-256");
        }
        return value;
    }

    private static String requirePreviousFingerprint(String value) {
        if (GENESIS_FINGERPRINT.equals(value)) {
            return value;
        }
        return requireSha256(value, "previousFingerprint");
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
