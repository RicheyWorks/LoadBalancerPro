package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Strict data-only canonical JSON boundary for durable experiment journal events.
 */
public final class EnterpriseLabExperimentJournalCodec {
    public static final int HARD_MAX_ENTRY_BYTES = 65_536;
    public static final int HARD_MAX_PAYLOAD_BYTES = 32_768;
    public static final int HARD_MAX_PAYLOAD_DEPTH = 12;
    public static final int HARD_MAX_PAYLOAD_NODES = 512;
    public static final int HARD_MAX_COLLECTION_ENTRIES = 64;
    public static final int HARD_MAX_PAYLOAD_STRING_LENGTH = 2_048;
    public static final int HARD_MAX_PAYLOAD_FIELD_LENGTH = 128;

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Set<String> ENVELOPE_FIELDS = Set.of(
            "schemaVersion",
            "sequence",
            "experimentId",
            "scenarioId",
            "eventType",
            "stateBefore",
            "stateAfter",
            "logicalCycle",
            "occurredAt",
            "configurationFingerprint",
            "decisionFingerprint",
            "baselineAllocationFingerprint",
            "candidateAllocationFingerprint",
            "appliedAllocationFingerprint",
            "reason",
            "previousEntryFingerprint",
            "metadata",
            "payloadSchemaVersion",
            "payload",
            "currentEntryFingerprint");
    private static final Set<String> REASON_FIELDS = Set.of("code", "message");
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(HARD_MAX_PAYLOAD_DEPTH + 4)
                    .maxStringLength(HARD_MAX_ENTRY_BYTES)
                    .maxNumberLength(128)
                    .build())
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(JSON_FACTORY);

    /** Encodes a complete entry, including the fingerprint of its canonical content. */
    public byte[] encode(EnterpriseLabExperimentJournalEvent event) {
        EnterpriseLabExperimentJournalEvent safeEvent = Objects.requireNonNull(event, "event cannot be null");
        ObjectNode complete = canonicalContentNode(safeEvent);
        complete.put("currentEntryFingerprint", safeEvent.currentEntryFingerprint());
        byte[] bytes = write(complete);
        if (bytes.length > HARD_MAX_ENTRY_BYTES) {
            throw new CodecException(Failure.EXCEEDED_BOUNDS, "canonical journal entry exceeds 65536 bytes");
        }
        return bytes;
    }

    /** Returns canonical bytes covered by currentEntryFingerprint. */
    public byte[] canonicalContentBytes(EnterpriseLabExperimentJournalEvent event) {
        return write(canonicalContentNode(Objects.requireNonNull(event, "event cannot be null")));
    }

    /** Strictly decodes and verifies one complete event without executing payload behavior. */
    public EnterpriseLabExperimentJournalEvent decode(byte[] encoded) {
        byte[] safeEncoded = requireInput(encoded);
        String json = strictUtf8(safeEncoded);
        if (!json.isEmpty() && json.charAt(0) == '\ufeff') {
            throw new CodecException(Failure.MALFORMED_ENTRY, "UTF-8 byte order marks are not accepted");
        }
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            JsonNode root = OBJECT_MAPPER.readTree(parser);
            if (root == null || parser.nextToken() != null) {
                throw new CodecException(Failure.MALFORMED_ENTRY, "journal entry must contain one JSON value");
            }
            return decodeRoot(root);
        } catch (CodecException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new CodecException(Failure.MALFORMED_ENTRY, "journal entry is not valid strict JSON", exception);
        } catch (DateTimeParseException exception) {
            throw new CodecException(Failure.MALFORMED_ENTRY, "occurredAt must be a valid ISO-8601 instant", exception);
        } catch (IllegalArgumentException exception) {
            throw new CodecException(Failure.MALFORMED_ENTRY, "journal entry violates the event model", exception);
        }
    }

    static String canonicalFingerprint(EnterpriseLabExperimentJournalEvent event) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(write(canonicalContentNode(event))));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    static JsonNode canonicalPayloadCopy(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new CodecException(Failure.MALFORMED_ENTRY, "journal payload must be a JSON object");
        }
        Counter counter = new Counter();
        JsonNode canonical = canonicalize(payload, 1, counter);
        byte[] bytes = write(canonical);
        if (bytes.length > HARD_MAX_PAYLOAD_BYTES) {
            throw new CodecException(Failure.EXCEEDED_BOUNDS, "canonical journal payload exceeds 32768 bytes");
        }
        return canonical;
    }

    private EnterpriseLabExperimentJournalEvent decodeRoot(JsonNode root) {
        ObjectNode object = requireObject(root, "journal entry");
        requireExactFields(object, ENVELOPE_FIELDS, "journal entry");
        String schemaVersion = requireText(object, "schemaVersion");
        if (!EnterpriseLabExperimentJournalEvent.SCHEMA_VERSION.equals(schemaVersion)) {
            throw new CodecException(Failure.UNSUPPORTED_VERSION, "unsupported journal event schemaVersion");
        }
        String payloadSchemaVersion = requireText(object, "payloadSchemaVersion");
        if (!EnterpriseLabExperimentJournalEvent.PAYLOAD_SCHEMA_VERSION.equals(payloadSchemaVersion)) {
            throw new CodecException(Failure.UNSUPPORTED_VERSION, "unsupported journal payload schemaVersion");
        }
        ObjectNode reasonNode = requireObject(object.get("reason"), "reason");
        requireExactFields(reasonNode, REASON_FIELDS, "reason");
        Map<String, String> metadata = readMetadata(requireObject(object.get("metadata"), "metadata"));
        String suppliedFingerprint = requireText(object, "currentEntryFingerprint");
        if (!SHA_256.matcher(suppliedFingerprint).matches()) {
            throw new CodecException(
                    Failure.MALFORMED_ENTRY, "currentEntryFingerprint must be lowercase SHA-256");
        }

        EnterpriseLabExperimentJournalEvent event = EnterpriseLabExperimentJournalEvent.reconstitute(
                schemaVersion,
                requireLong(object, "sequence"),
                requireText(object, "experimentId"),
                requireText(object, "scenarioId"),
                requireEnum(object, "eventType", EnterpriseLabExperimentJournalEventType.class),
                requireEnum(object, "stateBefore", EnterpriseLabExperimentState.class),
                requireEnum(object, "stateAfter", EnterpriseLabExperimentState.class),
                requireLong(object, "logicalCycle"),
                Instant.parse(requireText(object, "occurredAt")),
                requireText(object, "configurationFingerprint"),
                requireText(object, "decisionFingerprint"),
                requireText(object, "baselineAllocationFingerprint"),
                requireText(object, "candidateAllocationFingerprint"),
                requireText(object, "appliedAllocationFingerprint"),
                new EnterpriseLabExperimentJournalEvent.Reason(
                        requireText(reasonNode, "code"), requireText(reasonNode, "message")),
                requireText(object, "previousEntryFingerprint"),
                metadata,
                payloadSchemaVersion,
                object.get("payload"));
        if (!MessageDigest.isEqual(
                suppliedFingerprint.getBytes(StandardCharsets.US_ASCII),
                event.currentEntryFingerprint().getBytes(StandardCharsets.US_ASCII))) {
            throw new CodecException(
                    Failure.FINGERPRINT_MISMATCH,
                    "currentEntryFingerprint does not match canonical journal content");
        }
        return event;
    }

    private static ObjectNode canonicalContentNode(EnterpriseLabExperimentJournalEvent event) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("schemaVersion", event.schemaVersion());
        root.put("sequence", event.sequence());
        root.put("experimentId", event.experimentId());
        root.put("scenarioId", event.scenarioId());
        root.put("eventType", event.eventType().name());
        root.put("stateBefore", event.stateBefore().name());
        root.put("stateAfter", event.stateAfter().name());
        root.put("logicalCycle", event.logicalCycle());
        root.put("occurredAt", event.occurredAt().toString());
        root.put("configurationFingerprint", event.configurationFingerprint());
        root.put("decisionFingerprint", event.decisionFingerprint());
        root.put("baselineAllocationFingerprint", event.baselineAllocationFingerprint());
        root.put("candidateAllocationFingerprint", event.candidateAllocationFingerprint());
        root.put("appliedAllocationFingerprint", event.appliedAllocationFingerprint());
        ObjectNode reason = root.putObject("reason");
        reason.put("code", event.reason().code());
        reason.put("message", event.reason().message());
        root.put("previousEntryFingerprint", event.previousEntryFingerprint());
        ObjectNode metadata = root.putObject("metadata");
        event.metadata().forEach(metadata::put);
        root.put("payloadSchemaVersion", event.payloadSchemaVersion());
        root.set("payload", event.payloadInternal());
        return root;
    }

    private static JsonNode canonicalize(JsonNode node, int depth, Counter counter) {
        if (depth > HARD_MAX_PAYLOAD_DEPTH) {
            throw new CodecException(Failure.EXCEEDED_BOUNDS, "journal payload exceeds maximum depth 12");
        }
        counter.increment();
        if (node.isObject()) {
            if (node.size() > HARD_MAX_COLLECTION_ENTRIES) {
                throw new CodecException(Failure.EXCEEDED_BOUNDS, "payload object exceeds 64 fields");
            }
            Map<String, JsonNode> sorted = new TreeMap<>();
            node.fields().forEachRemaining(entry -> {
                String key = requirePayloadField(entry.getKey());
                sorted.put(key, canonicalize(entry.getValue(), depth + 1, counter));
            });
            ObjectNode canonical = JsonNodeFactory.instance.objectNode();
            sorted.forEach(canonical::set);
            return canonical;
        }
        if (node.isArray()) {
            if (node.size() > HARD_MAX_COLLECTION_ENTRIES) {
                throw new CodecException(Failure.EXCEEDED_BOUNDS, "payload collection exceeds 64 values");
            }
            List<JsonNode> values = new ArrayList<>();
            node.forEach(value -> values.add(canonicalize(value, depth + 1, counter)));
            values.sort(Comparator.comparing(EnterpriseLabExperimentJournalCodec::canonicalSortKey));
            ArrayNode canonical = JsonNodeFactory.instance.arrayNode();
            values.forEach(canonical::add);
            return canonical;
        }
        if (node.isTextual()) {
            String text = node.textValue();
            if (text.length() > HARD_MAX_PAYLOAD_STRING_LENGTH || hasInvalidSurrogate(text)) {
                throw new CodecException(Failure.EXCEEDED_BOUNDS, "payload text is not bounded valid Unicode");
            }
            return TextNode.valueOf(text);
        }
        if (node.isIntegralNumber()) {
            BigInteger value = node.bigIntegerValue();
            if (value.bitLength() < Long.SIZE) {
                return LongNode.valueOf(value.longValue());
            }
            return BigIntegerNode.valueOf(value);
        }
        if (node.isFloatingPointNumber()) {
            BigDecimal normalized = node.decimalValue().stripTrailingZeros();
            return DecimalNode.valueOf(new BigDecimal(normalized.toPlainString()));
        }
        if (node.isBoolean()) {
            return BooleanNode.valueOf(node.booleanValue());
        }
        if (node.isNull()) {
            return NullNode.instance;
        }
        throw new CodecException(Failure.MALFORMED_ENTRY, "payload contains an unsupported JSON value");
    }

    private static String canonicalSortKey(JsonNode node) {
        return new String(write(node), StandardCharsets.UTF_8);
    }

    private static String requirePayloadField(String field) {
        if (field == null || field.isBlank() || !field.equals(field.trim())
                || field.length() > HARD_MAX_PAYLOAD_FIELD_LENGTH || hasInvalidSurrogate(field)) {
            throw new CodecException(Failure.EXCEEDED_BOUNDS, "payload field names must be trimmed and bounded");
        }
        return field;
    }

    private static Map<String, String> readMetadata(ObjectNode node) {
        if (node.size() > EnterpriseLabExperimentJournalEvent.HARD_MAX_METADATA_ENTRIES) {
            throw new CodecException(Failure.EXCEEDED_BOUNDS, "metadata cannot exceed 16 entries");
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            if (!entry.getValue().isTextual()) {
                throw new CodecException(Failure.MALFORMED_ENTRY, "metadata values must be strings");
            }
            metadata.put(entry.getKey(), entry.getValue().textValue());
        });
        return metadata;
    }

    private static ObjectNode requireObject(JsonNode node, String name) {
        if (node == null || !node.isObject()) {
            throw new CodecException(Failure.MALFORMED_ENTRY, name + " must be a JSON object");
        }
        return (ObjectNode) node;
    }

    private static void requireExactFields(ObjectNode node, Set<String> expected, String name) {
        Set<String> actual = new HashSet<>();
        Iterator<String> fields = node.fieldNames();
        fields.forEachRemaining(actual::add);
        Set<String> unknown = new HashSet<>(actual);
        unknown.removeAll(expected);
        if (!unknown.isEmpty()) {
            throw new CodecException(Failure.UNKNOWN_FIELD, name + " contains unsupported fields");
        }
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(actual);
        if (!missing.isEmpty()) {
            throw new CodecException(Failure.MALFORMED_ENTRY, name + " is missing required fields");
        }
    }

    private static String requireText(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw new CodecException(Failure.MALFORMED_ENTRY, field + " must be a string");
        }
        return value.textValue();
    }

    private static long requireLong(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw new CodecException(Failure.MALFORMED_ENTRY, field + " must be a bounded integer");
        }
        return value.longValue();
    }

    private static <E extends Enum<E>> E requireEnum(ObjectNode node, String field, Class<E> enumType) {
        String value = requireText(node, field);
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new CodecException(Failure.MALFORMED_ENTRY, field + " contains an unsupported value", exception);
        }
    }

    private static byte[] requireInput(byte[] encoded) {
        if (encoded == null || encoded.length == 0) {
            throw new CodecException(Failure.MALFORMED_ENTRY, "journal entry bytes cannot be empty");
        }
        if (encoded.length > HARD_MAX_ENTRY_BYTES) {
            throw new CodecException(Failure.EXCEEDED_BOUNDS, "journal entry exceeds 65536 bytes");
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
            throw new CodecException(Failure.MALFORMED_ENTRY, "journal entry must be strict UTF-8", exception);
        }
    }

    private static byte[] write(JsonNode node) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(node);
        } catch (IOException exception) {
            throw new IllegalStateException("canonical JSON encoding failed", exception);
        }
    }

    private static boolean hasInvalidSurrogate(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    return true;
                }
                index++;
            } else if (Character.isLowSurrogate(current)) {
                return true;
            }
        }
        return false;
    }

    public enum Failure {
        MALFORMED_ENTRY,
        EXCEEDED_BOUNDS,
        UNSUPPORTED_VERSION,
        UNKNOWN_FIELD,
        FINGERPRINT_MISMATCH,
        SENSITIVE_CONTENT
    }

    public static final class CodecException extends IllegalArgumentException {
        private final Failure failure;

        public CodecException(Failure failure, String message) {
            super(message);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        public CodecException(Failure failure, String message, Throwable cause) {
            super(message, cause);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        public Failure failure() {
            return failure;
        }
    }

    private static final class Counter {
        private int value;

        private void increment() {
            value++;
            if (value > HARD_MAX_PAYLOAD_NODES) {
                throw new CodecException(Failure.EXCEEDED_BOUNDS, "journal payload exceeds 512 nodes");
            }
        }
    }
}
