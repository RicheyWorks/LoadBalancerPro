package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.RecoveryClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransitionReason;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.VerificationResult;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** Strict canonical JSON boundary for bounded durable allocation-state records. */
public final class EnterpriseLabAllocationStateCodec {
    public static final int HARD_MAX_RECORD_BYTES = 65_536;
    public static final String ALLOCATION_FINGERPRINT_SCHEMA_VERSION =
            "enterprise-lab-loopback-allocation-fingerprint/v1";

    private static final Set<String> RECORD_FIELDS = Set.of(
            "schemaVersion",
            "allocationTransactionId",
            "experimentId",
            "scenarioId",
            "ownerGeneration",
            "allocationGeneration",
            "allocationPurpose",
            "baselineAllocation",
            "requestedAllocation",
            "guardrailApprovedAllocation",
            "installedAllocation",
            "normalizedAllocationFingerprint",
            "routerReadBackFingerprint",
            "previousCommittedAllocationFingerprint",
            "transactionPhase",
            "transitionReason",
            "actionPerformed",
            "createdAt",
            "lastVerifiedAt",
            "verificationResult",
            "recoveryClassification",
            "predecessorRecordFingerprint",
            "metadata",
            "currentRecordFingerprint");
    private static final Set<String> REASON_FIELDS = Set.of("code", "message");
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(8)
                    .maxStringLength(HARD_MAX_RECORD_BYTES)
                    .maxNumberLength(32)
                    .build())
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper(JSON_FACTORY);

    private final EnterpriseLabExperimentTargetCatalog targetCatalog;

    public EnterpriseLabAllocationStateCodec(
            EnterpriseLabExperimentTargetCatalog targetCatalog) {
        this.targetCatalog = Objects.requireNonNull(targetCatalog, "targetCatalog cannot be null");
    }

    /** Encodes the complete record, including its canonical content fingerprint. */
    public byte[] encode(EnterpriseLabAllocationState state) {
        EnterpriseLabAllocationState safeState = Objects.requireNonNull(
                state, "state cannot be null");
        safeState.validateAgainst(targetCatalog);
        ObjectNode node = canonicalContentNode(safeState);
        node.put("currentRecordFingerprint", safeState.currentRecordFingerprint());
        byte[] encoded = write(node);
        if (encoded.length > HARD_MAX_RECORD_BYTES) {
            throw new CodecException(Failure.EXCEEDED_BOUNDS,
                    "allocation state exceeds its bounded canonical size");
        }
        return encoded;
    }

    /** Returns the canonical bytes covered by currentRecordFingerprint. */
    public byte[] canonicalContentBytes(EnterpriseLabAllocationState state) {
        EnterpriseLabAllocationState safeState = Objects.requireNonNull(
                state, "state cannot be null");
        safeState.validateAgainst(targetCatalog);
        return write(canonicalContentNode(safeState));
    }

    /** Strictly decodes and verifies one record without executing allocation behavior. */
    public EnterpriseLabAllocationState decode(byte[] encoded) {
        byte[] safeEncoded = requireInput(encoded);
        String json = strictUtf8(safeEncoded);
        if (!json.isEmpty() && json.charAt(0) == '\ufeff') {
            throw new CodecException(Failure.MALFORMED_RECORD,
                    "UTF-8 byte order marks are not accepted");
        }
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            JsonNode parsed = MAPPER.readTree(parser);
            if (parsed == null || !parsed.isObject() || parser.nextToken() != null) {
                throw new CodecException(Failure.MALFORMED_RECORD,
                        "allocation state must contain exactly one JSON object");
            }
            return decodeRoot((ObjectNode) parsed);
        } catch (CodecException exception) {
            throw exception;
        } catch (IOException | DateTimeException | IllegalArgumentException exception) {
            throw new CodecException(Failure.MALFORMED_RECORD,
                    "allocation state is malformed", exception);
        }
    }

    private EnterpriseLabAllocationState decodeRoot(ObjectNode node) {
        requireExactFields(node, RECORD_FIELDS, "allocation state");
        String schemaVersion = text(node, "schemaVersion");
        if (!EnterpriseLabAllocationState.SCHEMA_VERSION.equals(schemaVersion)) {
            throw new CodecException(Failure.UNSUPPORTED_VERSION,
                    "allocation state schemaVersion is unsupported");
        }
        ObjectNode reasonNode = object(node.get("transitionReason"), "transitionReason");
        requireExactFields(reasonNode, REASON_FIELDS, "transitionReason");
        String suppliedNormalizedFingerprint = text(node, "normalizedAllocationFingerprint");
        String suppliedCurrentFingerprint = text(node, "currentRecordFingerprint");

        EnterpriseLabAllocationState state = EnterpriseLabAllocationState.reconstitute(
                schemaVersion,
                text(node, "allocationTransactionId"),
                optionalText(node, "experimentId"),
                text(node, "scenarioId"),
                number(node, "ownerGeneration"),
                number(node, "allocationGeneration"),
                enumValue(node, "allocationPurpose", AllocationPurpose.class),
                allocation(node, "baselineAllocation"),
                allocation(node, "requestedAllocation"),
                allocation(node, "guardrailApprovedAllocation"),
                allocation(node, "installedAllocation"),
                text(node, "routerReadBackFingerprint"),
                text(node, "previousCommittedAllocationFingerprint"),
                enumValue(node, "transactionPhase", TransactionPhase.class),
                new TransitionReason(text(reasonNode, "code"), text(reasonNode, "message")),
                bool(node, "actionPerformed"),
                instant(node, "createdAt"),
                optionalInstant(node, "lastVerifiedAt"),
                enumValue(node, "verificationResult", VerificationResult.class),
                enumValue(node, "recoveryClassification", RecoveryClassification.class),
                text(node, "predecessorRecordFingerprint"),
                metadata(node),
                targetCatalog);

        requireFingerprintMatch(
                state.normalizedAllocationFingerprint(),
                suppliedNormalizedFingerprint,
                "normalizedAllocationFingerprint does not match the canonical approved allocation");
        requireFingerprintMatch(
                state.currentRecordFingerprint(),
                suppliedCurrentFingerprint,
                "currentRecordFingerprint does not match canonical allocation-state content");
        return state;
    }

    static String canonicalRecordFingerprint(EnterpriseLabAllocationState state) {
        return fingerprint(write(canonicalContentNode(
                Objects.requireNonNull(state, "state cannot be null"))));
    }

    static String canonicalAllocationFingerprint(
            String scenarioId,
            Map<String, Double> allocation) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("schemaVersion", ALLOCATION_FINGERPRINT_SCHEMA_VERSION);
        root.put("scenarioId", Objects.requireNonNull(scenarioId, "scenarioId cannot be null"));
        root.set("allocation", allocationNode(allocation));
        return fingerprint(write(root));
    }

    private static ObjectNode canonicalContentNode(EnterpriseLabAllocationState state) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("schemaVersion", state.schemaVersion());
        node.put("allocationTransactionId", state.allocationTransactionId());
        state.experimentId().ifPresentOrElse(
                value -> node.put("experimentId", value),
                () -> node.putNull("experimentId"));
        node.put("scenarioId", state.scenarioId());
        node.put("ownerGeneration", state.ownerGeneration());
        node.put("allocationGeneration", state.allocationGeneration());
        node.put("allocationPurpose", state.allocationPurpose().name());
        node.set("baselineAllocation", allocationNode(state.baselineAllocation()));
        node.set("requestedAllocation", allocationNode(state.requestedAllocation()));
        node.set("guardrailApprovedAllocation", allocationNode(state.guardrailApprovedAllocation()));
        node.set("installedAllocation", allocationNode(state.installedAllocation()));
        node.put("normalizedAllocationFingerprint", state.normalizedAllocationFingerprint());
        node.put("routerReadBackFingerprint", state.routerReadBackFingerprint());
        node.put("previousCommittedAllocationFingerprint", state.previousCommittedAllocationFingerprint());
        node.put("transactionPhase", state.transactionPhase().name());
        ObjectNode reason = node.putObject("transitionReason");
        reason.put("code", state.transitionReason().code());
        reason.put("message", state.transitionReason().message());
        node.put("actionPerformed", state.actionPerformed());
        node.put("createdAt", state.createdAt().toString());
        state.lastVerifiedAt().ifPresentOrElse(
                value -> node.put("lastVerifiedAt", value.toString()),
                () -> node.putNull("lastVerifiedAt"));
        node.put("verificationResult", state.verificationResult().name());
        node.put("recoveryClassification", state.recoveryClassification().name());
        node.put("predecessorRecordFingerprint", state.predecessorRecordFingerprint());
        ObjectNode metadata = node.putObject("metadata");
        state.metadata().forEach(metadata::put);
        return node;
    }

    private static ObjectNode allocationNode(Map<String, Double> allocation) {
        Objects.requireNonNull(allocation, "allocation cannot be null");
        ObjectNode node = MAPPER.createObjectNode();
        new TreeMap<>(allocation).forEach(
                (backendId, share) -> node.put(backendId, canonicalShare(share)));
        return node;
    }

    private static String canonicalShare(Double value) {
        double share = Objects.requireNonNull(value, "allocation share cannot be null");
        if (!Double.isFinite(share)) {
            throw new IllegalArgumentException("allocation share must be finite");
        }
        if (share == 0.0d) {
            share = 0.0d;
        }
        return Double.toHexString(share);
    }

    private static Map<String, Double> allocation(ObjectNode node, String field) {
        ObjectNode allocation = object(node.get(field), field);
        if (allocation.size() == 0
                || allocation.size() > EnterpriseLabLoopbackAllocationSnapshot.HARD_MAX_BACKENDS) {
            throw new CodecException(Failure.EXCEEDED_BOUNDS,
                    field + " backend count is outside hard bounds");
        }
        Map<String, Double> values = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = allocation.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (!entry.getValue().isTextual()) {
                throw new CodecException(Failure.MALFORMED_RECORD,
                        field + " shares must use canonical hexadecimal strings");
            }
            String encodedShare = entry.getValue().textValue();
            double share;
            try {
                share = Double.valueOf(encodedShare);
            } catch (NumberFormatException exception) {
                throw new CodecException(Failure.MALFORMED_RECORD,
                        field + " contains a malformed share", exception);
            }
            if (!canonicalShare(share).equals(encodedShare)) {
                throw new CodecException(Failure.MALFORMED_RECORD,
                        field + " contains a non-canonical share");
            }
            values.put(entry.getKey(), share);
        }
        return values;
    }

    private static Map<String, String> metadata(ObjectNode node) {
        ObjectNode metadata = object(node.get("metadata"), "metadata");
        if (metadata.size() > EnterpriseLabAllocationState.HARD_MAX_METADATA_ENTRIES) {
            throw new CodecException(Failure.EXCEEDED_BOUNDS,
                    "metadata exceeds 16 entries");
        }
        Map<String, String> values = new LinkedHashMap<>();
        metadata.fields().forEachRemaining(entry -> {
            if (!entry.getValue().isTextual()) {
                throw new CodecException(Failure.MALFORMED_RECORD,
                        "metadata values must be strings");
            }
            values.put(entry.getKey(), entry.getValue().textValue());
        });
        return values;
    }

    private static ObjectNode object(JsonNode node, String subject) {
        if (node == null || !node.isObject()) {
            throw new CodecException(Failure.MALFORMED_RECORD,
                    subject + " must be one JSON object");
        }
        return (ObjectNode) node;
    }

    private static void requireExactFields(ObjectNode node, Set<String> expected, String subject) {
        Set<String> actual = new HashSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        Set<String> unknown = new HashSet<>(actual);
        unknown.removeAll(expected);
        if (!unknown.isEmpty()) {
            throw new CodecException(Failure.UNKNOWN_FIELD,
                    subject + " contains unsupported fields");
        }
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(actual);
        if (!missing.isEmpty()) {
            throw new CodecException(Failure.MALFORMED_RECORD,
                    subject + " is missing required fields");
        }
    }

    private static String text(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw new CodecException(Failure.MALFORMED_RECORD, field + " must be text");
        }
        return value.textValue();
    }

    private static Optional<String> optionalText(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null) {
            throw new CodecException(Failure.MALFORMED_RECORD, field + " is required");
        }
        if (value.isNull()) {
            return Optional.empty();
        }
        if (!value.isTextual()) {
            throw new CodecException(Failure.MALFORMED_RECORD, field + " must be text or null");
        }
        return Optional.of(value.textValue());
    }

    private static long number(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw new CodecException(Failure.MALFORMED_RECORD, field + " must be an integer");
        }
        return value.longValue();
    }

    private static boolean bool(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isBoolean()) {
            throw new CodecException(Failure.MALFORMED_RECORD, field + " must be boolean");
        }
        return value.booleanValue();
    }

    private static Instant instant(ObjectNode node, String field) {
        String encoded = text(node, field);
        Instant parsed = Instant.parse(encoded);
        if (!parsed.toString().equals(encoded)) {
            throw new CodecException(Failure.MALFORMED_RECORD,
                    field + " must use canonical UTC instant encoding");
        }
        return parsed;
    }

    private static Optional<Instant> optionalInstant(ObjectNode node, String field) {
        Optional<String> encoded = optionalText(node, field);
        return encoded.map(value -> {
            Instant parsed = Instant.parse(value);
            if (!parsed.toString().equals(value)) {
                throw new CodecException(Failure.MALFORMED_RECORD,
                        field + " must use canonical UTC instant encoding");
            }
            return parsed;
        });
    }

    private static <E extends Enum<E>> E enumValue(
            ObjectNode node,
            String field,
            Class<E> type) {
        try {
            return Enum.valueOf(type, text(node, field));
        } catch (IllegalArgumentException exception) {
            throw new CodecException(Failure.MALFORMED_RECORD,
                    field + " uses an unsupported value", exception);
        }
    }

    private static byte[] requireInput(byte[] encoded) {
        if (encoded == null || encoded.length == 0) {
            throw new CodecException(Failure.MALFORMED_RECORD,
                    "allocation state bytes cannot be empty");
        }
        if (encoded.length > HARD_MAX_RECORD_BYTES) {
            throw new CodecException(Failure.EXCEEDED_BOUNDS,
                    "allocation state exceeds 65536 bytes");
        }
        return encoded;
    }

    private static String strictUtf8(byte[] encoded) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new CodecException(Failure.MALFORMED_RECORD,
                    "allocation state must be strict UTF-8", exception);
        }
    }

    private static void requireFingerprintMatch(
            String expected,
            String supplied,
            String message) {
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                supplied.getBytes(StandardCharsets.US_ASCII))) {
            throw new CodecException(Failure.FINGERPRINT_MISMATCH, message);
        }
    }

    private static byte[] write(JsonNode node) {
        try {
            return MAPPER.writeValueAsBytes(node);
        } catch (IOException exception) {
            throw new IllegalStateException("canonical allocation-state JSON encoding failed", exception);
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
        EXCEEDED_BOUNDS,
        SENSITIVE_CONTENT
    }

    public static final class CodecException extends IllegalArgumentException {
        private final Failure failure;

        CodecException(Failure failure, String message) {
            super(message);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        CodecException(Failure failure, String message, Throwable cause) {
            super(message, cause);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        public Failure failure() {
            return failure;
        }
    }
}
