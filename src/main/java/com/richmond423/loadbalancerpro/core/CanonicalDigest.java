package com.richmond423.loadbalancerpro.core;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Length-prefixed canonical SHA-256 input writer.
 *
 * <p>Every variable-length value carries an explicit byte length, and every optional value
 * carries a presence marker. Callers therefore cannot create delimiter-shaped collisions such
 * as one {@code "a,b"} value versus two values {@code "a"} and {@code "b"}.</p>
 */
public final class CanonicalDigest {
    private final MessageDigest digest;

    private CanonicalDigest(String namespace) {
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
        putString(namespace);
    }

    public static CanonicalDigest sha256(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace cannot be null or blank");
        }
        return new CanonicalDigest(namespace.trim());
    }

    public CanonicalDigest putString(String value) {
        if (value == null) {
            return putInt(-1);
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        putInt(bytes.length);
        digest.update(bytes);
        return this;
    }

    public CanonicalDigest putBoolean(boolean value) {
        digest.update((byte) (value ? 1 : 0));
        return this;
    }

    public CanonicalDigest putInt(int value) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
        return this;
    }

    public CanonicalDigest putLong(long value) {
        digest.update(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
        return this;
    }

    public CanonicalDigest putDouble(double value) {
        return putLong(Double.doubleToLongBits(value));
    }

    public CanonicalDigest putOptionalDouble(OptionalDouble value) {
        if (value == null || value.isEmpty()) {
            return putBoolean(false);
        }
        putBoolean(true);
        return putDouble(value.getAsDouble());
    }

    public CanonicalDigest putOptionalInt(OptionalInt value) {
        if (value == null || value.isEmpty()) {
            return putBoolean(false);
        }
        putBoolean(true);
        return putInt(value.getAsInt());
    }

    public String hexDigest() {
        return HexFormat.of().formatHex(digest.digest());
    }

    public long longDigest() {
        return ByteBuffer.wrap(digest.digest()).getLong();
    }
}
