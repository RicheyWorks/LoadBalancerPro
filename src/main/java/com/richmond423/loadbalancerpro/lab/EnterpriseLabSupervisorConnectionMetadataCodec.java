package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Strict canonical codec for atomically published supervisor readiness. */
public final class EnterpriseLabSupervisorConnectionMetadataCodec {
    public static final int HARD_MAX_METADATA_BYTES = 4_096;

    private static final Set<String> FIELDS = Set.of(
            "schemaVersion",
            "address",
            "port",
            "supervisorInstanceId",
            "supervisorGeneration",
            "durableStateGeneration",
            "stateFingerprint",
            "publishedAt");
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxStringLength(HARD_MAX_METADATA_BYTES)
                    .maxNestingDepth(4)
                    .maxNumberLength(20)
                    .build())
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper(JSON_FACTORY);

    public byte[] encode(EnterpriseLabSupervisorConnectionMetadata metadata) {
        EnterpriseLabSupervisorConnectionMetadata safe = Objects.requireNonNull(
                metadata, "metadata cannot be null");
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("schemaVersion", safe.schemaVersion());
            root.put("address", safe.address());
            root.put("port", safe.port());
            root.put("supervisorInstanceId", safe.supervisorInstanceId());
            root.put("supervisorGeneration", safe.supervisorGeneration());
            root.put("durableStateGeneration", safe.durableStateGeneration());
            root.put("stateFingerprint", safe.stateFingerprint());
            root.put("publishedAt", safe.publishedAt().toString());
            byte[] json = MAPPER.writeValueAsBytes(root);
            if (json.length + 1 > HARD_MAX_METADATA_BYTES) {
                throw failure(Failure.EXCEEDED_BOUNDS,
                        "supervisor connection metadata exceeds its byte bound");
            }
            byte[] fileBytes = Arrays.copyOf(json, json.length + 1);
            fileBytes[fileBytes.length - 1] = (byte) '\n';
            return fileBytes;
        } catch (CodecException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.MALFORMED_METADATA,
                    "supervisor connection metadata could not be encoded");
        }
    }

    public EnterpriseLabSupervisorConnectionMetadata decode(byte[] encoded) {
        byte[] safe = Objects.requireNonNull(encoded, "encoded cannot be null");
        if (safe.length < 2 || safe.length > HARD_MAX_METADATA_BYTES) {
            throw failure(Failure.EXCEEDED_BOUNDS,
                    "supervisor connection metadata byte length is outside hard bounds");
        }
        if (safe[safe.length - 1] != (byte) '\n'
                || safe[safe.length - 2] == (byte) '\r') {
            throw failure(Failure.NON_CANONICAL_METADATA,
                    "supervisor connection metadata requires one canonical LF terminator");
        }
        byte[] json = Arrays.copyOf(safe, safe.length - 1);
        ObjectNode root = parse(json);
        requireExactFields(root);
        EnterpriseLabSupervisorConnectionMetadata decoded;
        try {
            decoded = new EnterpriseLabSupervisorConnectionMetadata(
                    text(root, "schemaVersion"),
                    text(root, "address"),
                    integer(root, "port"),
                    text(root, "supervisorInstanceId"),
                    number(root, "supervisorGeneration"),
                    number(root, "durableStateGeneration"),
                    text(root, "stateFingerprint"),
                    instant(root, "publishedAt"));
        } catch (IllegalArgumentException exception) {
            throw failure(Failure.INVALID_METADATA,
                    "supervisor connection metadata violates its contract");
        }
        if (!MessageDigest.isEqual(encode(decoded), safe)) {
            throw failure(Failure.NON_CANONICAL_METADATA,
                    "supervisor connection metadata is not canonical");
        }
        return decoded;
    }

    private static ObjectNode parse(byte[] encoded) {
        try (JsonParser parser = JSON_FACTORY.createParser(strictUtf8(encoded))) {
            JsonNode parsed = MAPPER.readTree(parser);
            if (parsed == null || !parsed.isObject() || parser.nextToken() != null) {
                throw failure(Failure.MALFORMED_METADATA,
                        "supervisor connection metadata must contain one JSON object");
            }
            return (ObjectNode) parsed;
        } catch (CodecException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw failure(Failure.MALFORMED_METADATA,
                    "supervisor connection metadata is not valid strict JSON");
        }
    }

    private static void requireExactFields(ObjectNode root) {
        Set<String> actual = new HashSet<>();
        root.fieldNames().forEachRemaining(actual::add);
        Set<String> unknown = new HashSet<>(actual);
        unknown.removeAll(FIELDS);
        if (!unknown.isEmpty()) {
            throw failure(Failure.UNKNOWN_FIELD,
                    "supervisor connection metadata contains unknown fields");
        }
        if (!actual.equals(FIELDS)) {
            throw failure(Failure.MISSING_FIELD,
                    "supervisor connection metadata is missing fields");
        }
    }

    private static String text(ObjectNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual()) {
            throw failure(Failure.MALFORMED_METADATA,
                    field + " must be a JSON string");
        }
        return value.textValue();
    }

    private static long number(ObjectNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw failure(Failure.MALFORMED_METADATA,
                    field + " must be a bounded integer");
        }
        return value.longValue();
    }

    private static int integer(ObjectNode root, String field) {
        long value = number(root, field);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw failure(Failure.MALFORMED_METADATA,
                    field + " must be a bounded integer");
        }
        return Math.toIntExact(value);
    }

    private static Instant instant(ObjectNode root, String field) {
        try {
            return Instant.parse(text(root, field));
        } catch (DateTimeParseException exception) {
            throw failure(Failure.MALFORMED_METADATA,
                    field + " must be a canonical UTC instant");
        }
    }

    private static String strictUtf8(byte[] encoded) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw failure(Failure.MALFORMED_METADATA,
                    "supervisor connection metadata must be strict UTF-8");
        }
    }

    private static CodecException failure(Failure failure, String message) {
        return new CodecException(failure, message);
    }

    public enum Failure {
        MALFORMED_METADATA,
        INVALID_METADATA,
        UNKNOWN_FIELD,
        MISSING_FIELD,
        NON_CANONICAL_METADATA,
        EXCEEDED_BOUNDS
    }

    public static final class CodecException extends IllegalArgumentException {
        private final Failure failure;

        private CodecException(Failure failure, String message) {
            super(message);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        public Failure failure() {
            return failure;
        }
    }
}
