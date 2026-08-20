package com.richmond423.loadbalancerpro.api.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Verifies the required primary API key and one optional, operator-bounded rotation key.
 */
public final class ApiKeyVerifier {
    private final boolean configured;
    private final byte[] primaryKeyDigest;
    private final byte[] rotationKeyDigest;

    ApiKeyVerifier(String primaryKey, String rotationKey) {
        byte[] normalizedPrimaryKey = normalizedBytes(primaryKey);
        this.configured = normalizedPrimaryKey.length > 0;
        this.primaryKeyDigest = sha256(normalizedPrimaryKey);
        this.rotationKeyDigest = sha256(normalizedBytes(rotationKey));
    }

    public boolean isConfigured() {
        return configured;
    }

    public boolean matches(String presentedKey) {
        if (presentedKey == null || presentedKey.isBlank()) {
            return false;
        }
        byte[] presentedDigest = sha256(presentedKey.getBytes(StandardCharsets.UTF_8));
        boolean primaryMatch = MessageDigest.isEqual(primaryKeyDigest, presentedDigest);
        boolean rotationMatch = MessageDigest.isEqual(rotationKeyDigest, presentedDigest);
        return primaryMatch | rotationMatch;
    }

    private static byte[] normalizedBytes(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest algorithm is unavailable", exception);
        }
    }
}
