package com.richmond423.loadbalancerpro.lab;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable fingerprint-chained record of one actual lifecycle state change.
 */
public record EnterpriseLabExperimentTransition(
        String schemaVersion,
        long sequence,
        String experimentId,
        EnterpriseLabExperimentState fromState,
        EnterpriseLabExperimentState toState,
        Instant occurredAt,
        String commandId,
        String reason,
        int requestCount,
        int evidenceCount,
        int completedHoldDownCycles,
        long allocationRevision,
        String previousFingerprint) {
    public static final String SCHEMA_VERSION = "enterprise-lab-experiment-transition/v1";
    public static final String GENESIS_FINGERPRINT = "GENESIS";
    private static final int MAX_TEXT_LENGTH = 256;

    public EnterpriseLabExperimentTransition {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported experiment transition schemaVersion");
        }
        if (sequence < 1) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        experimentId = requireBoundedText(experimentId, "experimentId");
        fromState = Objects.requireNonNull(fromState, "fromState cannot be null");
        toState = Objects.requireNonNull(toState, "toState cannot be null");
        if (fromState == toState) {
            throw new IllegalArgumentException("transition must change lifecycle state");
        }
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        commandId = requireBoundedText(commandId, "commandId");
        reason = requireBoundedText(reason, "reason");
        if (requestCount < 0 || evidenceCount < 0 || evidenceCount > requestCount) {
            throw new IllegalArgumentException("request and evidence counts are inconsistent");
        }
        if (completedHoldDownCycles < 0 || allocationRevision < 0) {
            throw new IllegalArgumentException("cycle count and allocation revision cannot be negative");
        }
        previousFingerprint = requirePreviousFingerprint(previousFingerprint);
    }

    public String contentFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, schemaVersion);
            update(digest, Long.toString(sequence));
            update(digest, experimentId);
            update(digest, fromState.name());
            update(digest, toState.name());
            update(digest, occurredAt.toString());
            update(digest, commandId);
            update(digest, reason);
            update(digest, Integer.toString(requestCount));
            update(digest, Integer.toString(evidenceCount));
            update(digest, Integer.toString(completedHoldDownCycles));
            update(digest, Long.toString(allocationRevision));
            update(digest, previousFingerprint);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String requirePreviousFingerprint(String value) {
        String fingerprint = requireBoundedText(value, "previousFingerprint");
        if (!GENESIS_FINGERPRINT.equals(fingerprint) && !fingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("previousFingerprint must be GENESIS or lowercase SHA-256");
        }
        return fingerprint;
    }

    private static String requireBoundedText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        String text = value.trim();
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(fieldName + " cannot exceed 256 characters");
        }
        return text;
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
