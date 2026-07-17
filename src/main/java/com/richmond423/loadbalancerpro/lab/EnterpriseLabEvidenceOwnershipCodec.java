package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnerIdentity;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReconciliationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReleaseStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

/** Strict canonical JSON codec for the bounded versioned ownership record. */
public final class EnterpriseLabEvidenceOwnershipCodec {
    public static final int HARD_MAX_RECORD_BYTES = 16_384;

    private static final Set<String> RECORD_FIELDS = Set.of(
            "schemaVersion", "directoryIdentity", "lockFileIdentity", "owner",
            "generation", "state", "acquiredAt", "lastRenewedAt", "leaseExpiresAt",
            "previousOwnerFingerprint", "takeoverReasonCode", "takeoverSequence",
            "reconciliationStatus", "releaseStatus", "recordFingerprint");
    private static final Set<String> OWNER_FIELDS = Set.of(
            "ownerId", "applicationInstanceId", "processId", "hostDiagnosticFingerprint");
    private static final ObjectMapper MAPPER = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(8)
                    .maxStringLength(HARD_MAX_RECORD_BYTES)
                    .maxNumberLength(32)
                    .build())
            .build());

    public byte[] encode(OwnershipRecord value) {
        OwnershipRecord safe = java.util.Objects.requireNonNull(value, "value cannot be null");
        ObjectNode node = contentNode(
                safe.schemaVersion(), safe.directoryIdentity(), safe.lockFileIdentity(), safe.owner(),
                safe.generation(), safe.state(), safe.acquiredAt(), safe.lastRenewedAt(),
                safe.leaseExpiresAt(), safe.previousOwnerFingerprint(), safe.takeoverReasonCode(),
                safe.takeoverSequence(), safe.reconciliationStatus(), safe.releaseStatus());
        node.put("recordFingerprint", safe.recordFingerprint());
        byte[] encoded = write(node);
        if (encoded.length > HARD_MAX_RECORD_BYTES) {
            throw new CodecException(Failure.EXCEEDED_BOUNDS,
                    "ownership record exceeds its bounded canonical size");
        }
        return encoded;
    }

    public byte[] canonicalContentBytes(OwnershipRecord value) {
        OwnershipRecord safe = java.util.Objects.requireNonNull(value, "value cannot be null");
        return write(contentNode(
                safe.schemaVersion(), safe.directoryIdentity(), safe.lockFileIdentity(), safe.owner(),
                safe.generation(), safe.state(), safe.acquiredAt(), safe.lastRenewedAt(),
                safe.leaseExpiresAt(), safe.previousOwnerFingerprint(), safe.takeoverReasonCode(),
                safe.takeoverSequence(), safe.reconciliationStatus(), safe.releaseStatus()));
    }

    public OwnershipRecord decode(byte[] encoded) {
        if (encoded == null || encoded.length == 0 || encoded.length > HARD_MAX_RECORD_BYTES) {
            throw new CodecException(Failure.EXCEEDED_BOUNDS,
                    "ownership record input is outside bounded size limits");
        }
        try (JsonParser parser = MAPPER.getFactory().createParser(encoded)) {
            JsonNode parsed = MAPPER.readTree(parser);
            if (parsed == null || !parsed.isObject() || parser.nextToken() != null) {
                throw new CodecException(Failure.MALFORMED_RECORD,
                        "ownership record must contain exactly one JSON object");
            }
            ObjectNode node = (ObjectNode) parsed;
            requireExactFields(node, RECORD_FIELDS, "ownership record");
            String schemaVersion = text(node, "schemaVersion");
            if (!EnterpriseLabEvidenceOwnership.RECORD_SCHEMA_VERSION.equals(schemaVersion)) {
                throw new CodecException(Failure.UNSUPPORTED_VERSION,
                        "ownership record schemaVersion is unsupported");
            }
            JsonNode ownerNode = node.get("owner");
            if (ownerNode == null || !ownerNode.isObject()) {
                throw new CodecException(Failure.MALFORMED_RECORD,
                        "ownership owner identity must be one JSON object");
            }
            ObjectNode ownerObject = (ObjectNode) ownerNode;
            requireExactFields(ownerObject, OWNER_FIELDS, "ownership owner identity");
            OwnerIdentity owner = new OwnerIdentity(
                    text(ownerObject, "ownerId"),
                    text(ownerObject, "applicationInstanceId"),
                    number(ownerObject, "processId"),
                    text(ownerObject, "hostDiagnosticFingerprint"));
            String directoryIdentity = text(node, "directoryIdentity");
            String lockFileIdentity = text(node, "lockFileIdentity");
            long generation = number(node, "generation");
            OwnershipState state = enumValue(node, "state", OwnershipState.class);
            Instant acquiredAt = instant(node, "acquiredAt");
            Instant lastRenewedAt = instant(node, "lastRenewedAt");
            Instant leaseExpiresAt = instant(node, "leaseExpiresAt");
            String previousOwnerFingerprint = text(node, "previousOwnerFingerprint");
            String takeoverReasonCode = text(node, "takeoverReasonCode");
            long takeoverSequence = number(node, "takeoverSequence");
            ReconciliationStatus reconciliationStatus = enumValue(
                    node, "reconciliationStatus", ReconciliationStatus.class);
            ReleaseStatus releaseStatus = enumValue(node, "releaseStatus", ReleaseStatus.class);
            String recordFingerprint = text(node, "recordFingerprint");
            String expected = canonicalFingerprint(
                    schemaVersion, directoryIdentity, lockFileIdentity, owner, generation, state,
                    acquiredAt, lastRenewedAt, leaseExpiresAt, previousOwnerFingerprint,
                    takeoverReasonCode, takeoverSequence, reconciliationStatus, releaseStatus);
            if (!MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    recordFingerprint.getBytes(StandardCharsets.US_ASCII))) {
                throw new CodecException(Failure.FINGERPRINT_MISMATCH,
                        "ownership record fingerprint does not match canonical content");
            }
            return new OwnershipRecord(
                    schemaVersion, directoryIdentity, lockFileIdentity, owner, generation, state,
                    acquiredAt, lastRenewedAt, leaseExpiresAt, previousOwnerFingerprint,
                    takeoverReasonCode, takeoverSequence, reconciliationStatus, releaseStatus,
                    recordFingerprint);
        } catch (CodecException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            throw new CodecException(Failure.MALFORMED_RECORD,
                    "ownership record is malformed", exception);
        }
    }

    static String canonicalFingerprint(
            String schemaVersion,
            String directoryIdentity,
            String lockFileIdentity,
            OwnerIdentity owner,
            long generation,
            OwnershipState state,
            Instant acquiredAt,
            Instant lastRenewedAt,
            Instant leaseExpiresAt,
            String previousOwnerFingerprint,
            String takeoverReasonCode,
            long takeoverSequence,
            ReconciliationStatus reconciliationStatus,
            ReleaseStatus releaseStatus) {
        return fingerprint(write(contentNode(
                schemaVersion, directoryIdentity, lockFileIdentity, owner, generation, state,
                acquiredAt, lastRenewedAt, leaseExpiresAt, previousOwnerFingerprint,
                takeoverReasonCode, takeoverSequence, reconciliationStatus, releaseStatus)));
    }

    private static ObjectNode contentNode(
            String schemaVersion,
            String directoryIdentity,
            String lockFileIdentity,
            OwnerIdentity owner,
            long generation,
            OwnershipState state,
            Instant acquiredAt,
            Instant lastRenewedAt,
            Instant leaseExpiresAt,
            String previousOwnerFingerprint,
            String takeoverReasonCode,
            long takeoverSequence,
            ReconciliationStatus reconciliationStatus,
            ReleaseStatus releaseStatus) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("schemaVersion", schemaVersion);
        node.put("directoryIdentity", directoryIdentity);
        node.put("lockFileIdentity", lockFileIdentity);
        ObjectNode ownerNode = node.putObject("owner");
        ownerNode.put("ownerId", owner.ownerId());
        ownerNode.put("applicationInstanceId", owner.applicationInstanceId());
        ownerNode.put("processId", owner.processId());
        ownerNode.put("hostDiagnosticFingerprint", owner.hostDiagnosticFingerprint());
        node.put("generation", generation);
        node.put("state", state.name());
        node.put("acquiredAt", acquiredAt.toString());
        node.put("lastRenewedAt", lastRenewedAt.toString());
        node.put("leaseExpiresAt", leaseExpiresAt.toString());
        node.put("previousOwnerFingerprint", previousOwnerFingerprint);
        node.put("takeoverReasonCode", takeoverReasonCode);
        node.put("takeoverSequence", takeoverSequence);
        node.put("reconciliationStatus", reconciliationStatus.name());
        node.put("releaseStatus", releaseStatus.name());
        return node;
    }

    private static void requireExactFields(ObjectNode node, Set<String> expected, String subject) {
        Set<String> actual = new HashSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(expected)) {
            throw new CodecException(Failure.UNKNOWN_FIELD,
                    subject + " fields do not match the supported schema");
        }
    }

    private static String text(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw new CodecException(Failure.MALFORMED_RECORD, field + " must be text");
        }
        return value.textValue();
    }

    private static long number(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw new CodecException(Failure.MALFORMED_RECORD, field + " must be an integer");
        }
        return value.longValue();
    }

    private static Instant instant(ObjectNode node, String field) {
        try {
            String encoded = text(node, field);
            Instant parsed = Instant.parse(encoded);
            if (!parsed.toString().equals(encoded)) {
                throw new CodecException(Failure.MALFORMED_RECORD,
                        field + " must use canonical UTC instant encoding");
            }
            return parsed;
        } catch (java.time.DateTimeException exception) {
            throw new CodecException(Failure.MALFORMED_RECORD,
                    field + " must be a canonical instant", exception);
        }
    }

    private static <E extends Enum<E>> E enumValue(
            ObjectNode node,
            String field,
            Class<E> type) {
        try {
            return Enum.valueOf(type, text(node, field));
        } catch (IllegalArgumentException exception) {
            throw new CodecException(Failure.MALFORMED_RECORD,
                    field + " uses an unknown value", exception);
        }
    }

    private static byte[] write(ObjectNode node) {
        try {
            return MAPPER.writeValueAsBytes(node);
        } catch (IOException exception) {
            throw new IllegalStateException("canonical ownership JSON encoding failed", exception);
        }
    }

    private static String fingerprint(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public enum Failure {
        MALFORMED_RECORD,
        UNKNOWN_FIELD,
        UNSUPPORTED_VERSION,
        FINGERPRINT_MISMATCH,
        EXCEEDED_BOUNDS
    }

    public static final class CodecException extends IllegalArgumentException {
        private final Failure failure;

        CodecException(Failure failure, String message) {
            super(message);
            this.failure = java.util.Objects.requireNonNull(failure, "failure cannot be null");
        }

        CodecException(Failure failure, String message, Throwable cause) {
            super(message, cause);
            this.failure = java.util.Objects.requireNonNull(failure, "failure cannot be null");
        }

        public Failure failure() {
            return failure;
        }
    }
}
