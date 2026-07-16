package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable bounded configuration for one repository-approved loopback experiment.
 */
public record EnterpriseLabExperimentConfiguration(
        String schemaVersion,
        String experimentId,
        EnterpriseLabAdaptiveDecision candidateDecision,
        EnterpriseLabLoopbackAllocationSnapshot baselineSnapshot,
        int maximumRequestCount,
        Duration maximumDuration,
        int minimumEvidenceCount,
        int holdDownCycles,
        EnterpriseLabExperimentRollbackPolicy rollbackPolicy,
        AdaptiveRoutingPolicyMode operatingMode,
        boolean operatorAuthorized,
        Instant createdAt,
        Instant expiresAt) {
    public static final String SCHEMA_VERSION = "enterprise-lab-experiment-configuration/v1";
    public static final int HARD_MAX_REQUEST_COUNT = 10_000;
    public static final Duration HARD_MAX_DURATION = Duration.ofMinutes(10);
    public static final int HARD_MAX_HOLD_DOWN_CYCLES = 1_000;
    private static final Duration HARD_MAX_EXPIRATION_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_ID_LENGTH = 128;

    public EnterpriseLabExperimentConfiguration {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported experiment configuration schemaVersion");
        }
        experimentId = requireCanonicalId(experimentId, "experimentId");
        candidateDecision = Objects.requireNonNull(candidateDecision, "candidateDecision cannot be null");
        baselineSnapshot = Objects.requireNonNull(baselineSnapshot, "baselineSnapshot cannot be null");
        if (baselineSnapshot.kind() != Kind.BASELINE) {
            throw new IllegalArgumentException("baselineSnapshot must be the recorded baseline");
        }
        if (!candidateDecision.scenarioId().equals(baselineSnapshot.scenarioId())) {
            throw new IllegalArgumentException("candidate decision and baseline scenario must match");
        }
        if (!EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                baselineSnapshot.allocations(),
                candidateDecision.decision().guardrailDecision().baselineAllocations())) {
            throw new IllegalArgumentException("candidate decision baseline must match baselineSnapshot");
        }
        if (maximumRequestCount < 1 || maximumRequestCount > HARD_MAX_REQUEST_COUNT) {
            throw new IllegalArgumentException("maximumRequestCount must be between 1 and 10000");
        }
        maximumDuration = Objects.requireNonNull(maximumDuration, "maximumDuration cannot be null");
        if (maximumDuration.isZero() || maximumDuration.isNegative()
                || maximumDuration.compareTo(HARD_MAX_DURATION) > 0) {
            throw new IllegalArgumentException("maximumDuration must be positive and no greater than ten minutes");
        }
        if (minimumEvidenceCount < 1 || minimumEvidenceCount > maximumRequestCount) {
            throw new IllegalArgumentException(
                    "minimumEvidenceCount must be between 1 and maximumRequestCount");
        }
        if (holdDownCycles < 1 || holdDownCycles > HARD_MAX_HOLD_DOWN_CYCLES) {
            throw new IllegalArgumentException("holdDownCycles must be between 1 and 1000");
        }
        rollbackPolicy = Objects.requireNonNull(rollbackPolicy, "rollbackPolicy cannot be null");
        operatingMode = Objects.requireNonNull(operatingMode, "operatingMode cannot be null");
        if (operatingMode != candidateDecision.decision().mode()) {
            throw new IllegalArgumentException("operatingMode must match the candidate decision mode");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        Duration expirationWindow = Duration.between(createdAt, expiresAt);
        if (expirationWindow.isZero() || expirationWindow.isNegative()
                || expirationWindow.compareTo(HARD_MAX_EXPIRATION_WINDOW) > 0) {
            throw new IllegalArgumentException(
                    "expiration window must be positive and no greater than fifteen minutes");
        }
        if (expirationWindow.compareTo(maximumDuration) < 0) {
            throw new IllegalArgumentException("expiration window cannot be shorter than maximumDuration");
        }
    }

    public String scenarioId() {
        return candidateDecision.scenarioId();
    }

    public String candidateDecisionId() {
        return candidateDecision.decision().decisionId();
    }

    public String contentFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, schemaVersion);
            update(digest, experimentId);
            update(digest, candidateDecision.contentFingerprint());
            update(digest, baselineSnapshot.toString());
            update(digest, Integer.toString(maximumRequestCount));
            update(digest, maximumDuration.toString());
            update(digest, Integer.toString(minimumEvidenceCount));
            update(digest, Integer.toString(holdDownCycles));
            update(digest, rollbackPolicy.toString());
            update(digest, operatingMode.wireValue());
            update(digest, Boolean.toString(operatorAuthorized));
            update(digest, createdAt.toString());
            update(digest, expiresAt.toString());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static String requireCanonicalId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        if (!value.equals(value.trim()) || value.length() > MAX_ID_LENGTH
                || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException(fieldName + " must be a bounded canonical identifier");
        }
        return value;
    }
}
