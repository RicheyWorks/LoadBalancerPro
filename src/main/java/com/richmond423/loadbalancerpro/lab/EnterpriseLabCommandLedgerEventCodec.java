package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ApplicationCommitStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.AuthenticationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.Draft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.DuplicateClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.EventType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.LedgerSide;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.MutationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ResponseClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ValidationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** Strict bounded canonical JSON codec for command-ledger events. */
public final class EnterpriseLabCommandLedgerEventCodec {
    private static final String EMPTY_FINGERPRINT = "0".repeat(64);
    private static final Set<String> FIELDS = Set.of(
            "schemaVersion",
            "ledgerSide",
            "sequence",
            "eventType",
            "correlationId",
            "requestFingerprint",
            "transactionId",
            "experimentId",
            "commandType",
            "applicationInstanceId",
            "applicationOwnerGeneration",
            "supervisorInstanceId",
            "supervisorGeneration",
            "allocationGeneration",
            "requestedAllocationFingerprint",
            "previousCommittedFingerprint",
            "installedFingerprintBefore",
            "installedFingerprintAfter",
            "routerGenerationBefore",
            "routerGenerationAfter",
            "authenticationResult",
            "validationResult",
            "duplicateClassification",
            "mutationStatus",
            "responseClassification",
            "responseFingerprint",
            "observedSupervisorEventFingerprint",
            "applicationCommitStatus",
            "retryAttempt",
            "reasonCode",
            "occurredAt",
            "metadata",
            "predecessorFingerprint",
            "currentFingerprint");
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper(JSON_FACTORY);

    public EnterpriseLabCommandLedgerEvent issue(Draft draft) {
        Draft safe = Objects.requireNonNull(draft, "draft cannot be null");
        EnterpriseLabCommandLedgerEvent unsigned = event(safe, EMPTY_FINGERPRINT);
        return event(safe, canonicalFingerprint(unsigned));
    }

    public EnterpriseLabCommandLedgerEvent issue(Request request, Draft draft) {
        Request safeRequest = Objects.requireNonNull(request, "request cannot be null");
        EnterpriseLabCommandLedgerEvent event = issue(draft);
        if (!event.correlates(safeRequest)) {
            throw failure(Failure.REQUEST_MISMATCH,
                    "command ledger event does not match the canonical request identity");
        }
        return event;
    }

    public EnterpriseLabCommandLedgerEvent issue(
            Request request,
            Response response,
            Draft draft) {
        Request safeRequest = Objects.requireNonNull(request, "request cannot be null");
        Response safeResponse = Objects.requireNonNull(response, "response cannot be null");
        if (!safeResponse.validatesAgainst(safeRequest)) {
            throw failure(Failure.RESPONSE_MISMATCH,
                    "supervisor response does not match the canonical request");
        }
        EnterpriseLabCommandLedgerEvent event = issue(safeRequest, draft);
        if (!event.observes(safeResponse)) {
            throw failure(Failure.RESPONSE_MISMATCH,
                    "command ledger event does not match the canonical response identity");
        }
        return event;
    }

    public byte[] encode(EnterpriseLabCommandLedgerEvent event) {
        EnterpriseLabCommandLedgerEvent safe = Objects.requireNonNull(
                event, "event cannot be null");
        requireFingerprint(
                canonicalFingerprint(safe),
                safe.currentFingerprint(),
                "currentFingerprint");
        byte[] encoded = write(eventNode(safe, true));
        if (encoded.length > EnterpriseLabCommandLedgerEvent.HARD_MAX_EVENT_BYTES) {
            throw failure(Failure.EXCEEDED_BOUNDS,
                    "command ledger event exceeds the hard byte limit");
        }
        return encoded;
    }

    public EnterpriseLabCommandLedgerEvent decode(byte[] encoded) {
        byte[] safe = requireInput(encoded);
        ObjectNode root = parse(safe);
        requireExactFields(root);
        requireSchema(text(root, "schemaVersion"));
        EnterpriseLabCommandLedgerEvent event;
        try {
            event = new EnterpriseLabCommandLedgerEvent(
                    text(root, "schemaVersion"),
                    enumValue(root, "ledgerSide", LedgerSide.class),
                    number(root, "sequence"),
                    enumValue(root, "eventType", EventType.class),
                    text(root, "correlationId"),
                    text(root, "requestFingerprint"),
                    text(root, "transactionId"),
                    optionalText(root, "experimentId"),
                    enumValue(root, "commandType", CommandType.class),
                    text(root, "applicationInstanceId"),
                    number(root, "applicationOwnerGeneration"),
                    text(root, "supervisorInstanceId"),
                    number(root, "supervisorGeneration"),
                    number(root, "allocationGeneration"),
                    text(root, "requestedAllocationFingerprint"),
                    text(root, "previousCommittedFingerprint"),
                    text(root, "installedFingerprintBefore"),
                    text(root, "installedFingerprintAfter"),
                    number(root, "routerGenerationBefore"),
                    number(root, "routerGenerationAfter"),
                    enumValue(root, "authenticationResult", AuthenticationResult.class),
                    enumValue(root, "validationResult", ValidationResult.class),
                    enumValue(root, "duplicateClassification", DuplicateClassification.class),
                    enumValue(root, "mutationStatus", MutationStatus.class),
                    enumValue(root, "responseClassification", ResponseClassification.class),
                    text(root, "responseFingerprint"),
                    text(root, "observedSupervisorEventFingerprint"),
                    enumValue(root, "applicationCommitStatus", ApplicationCommitStatus.class),
                    boundedInt(root, "retryAttempt"),
                    text(root, "reasonCode"),
                    instant(root, "occurredAt"),
                    metadata(root, "metadata"),
                    text(root, "predecessorFingerprint"),
                    text(root, "currentFingerprint"));
            requireFingerprint(
                    canonicalFingerprint(event),
                    event.currentFingerprint(),
                    "currentFingerprint");
        } catch (CodecException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw failure(Failure.INVALID_EVENT,
                    "command ledger event violates model invariants", exception);
        }
        if (!Arrays.equals(safe, encode(event))) {
            throw failure(Failure.NON_CANONICAL_EVENT,
                    "command ledger event is not canonical");
        }
        return event;
    }

    public String canonicalFingerprint(EnterpriseLabCommandLedgerEvent event) {
        EnterpriseLabCommandLedgerEvent safe = Objects.requireNonNull(
                event, "event cannot be null");
        return sha256(write(eventNode(safe, false)));
    }

    private static EnterpriseLabCommandLedgerEvent event(Draft draft, String fingerprint) {
        return new EnterpriseLabCommandLedgerEvent(
                EnterpriseLabCommandLedgerEvent.SCHEMA_VERSION,
                draft.ledgerSide(),
                draft.sequence(),
                draft.eventType(),
                draft.correlationId(),
                draft.requestFingerprint(),
                draft.transactionId(),
                draft.experimentId(),
                draft.commandType(),
                draft.applicationInstanceId(),
                draft.applicationOwnerGeneration(),
                draft.supervisorInstanceId(),
                draft.supervisorGeneration(),
                draft.allocationGeneration(),
                draft.requestedAllocationFingerprint(),
                draft.previousCommittedFingerprint(),
                draft.installedFingerprintBefore(),
                draft.installedFingerprintAfter(),
                draft.routerGenerationBefore(),
                draft.routerGenerationAfter(),
                draft.authenticationResult(),
                draft.validationResult(),
                draft.duplicateClassification(),
                draft.mutationStatus(),
                draft.responseClassification(),
                draft.responseFingerprint(),
                draft.observedSupervisorEventFingerprint(),
                draft.applicationCommitStatus(),
                draft.retryAttempt(),
                draft.reasonCode(),
                draft.occurredAt(),
                draft.metadata(),
                draft.predecessorFingerprint(),
                fingerprint);
    }

    private static ObjectNode eventNode(
            EnterpriseLabCommandLedgerEvent event,
            boolean includeFingerprint) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("schemaVersion", event.schemaVersion());
        node.put("ledgerSide", event.ledgerSide().name());
        node.put("sequence", event.sequence());
        node.put("eventType", event.eventType().name());
        node.put("correlationId", event.correlationId());
        node.put("requestFingerprint", event.requestFingerprint());
        node.put("transactionId", event.transactionId());
        optionalText(node, "experimentId", event.experimentId());
        node.put("commandType", event.commandType().name());
        node.put("applicationInstanceId", event.applicationInstanceId());
        node.put("applicationOwnerGeneration", event.applicationOwnerGeneration());
        node.put("supervisorInstanceId", event.supervisorInstanceId());
        node.put("supervisorGeneration", event.supervisorGeneration());
        node.put("allocationGeneration", event.allocationGeneration());
        node.put("requestedAllocationFingerprint", event.requestedAllocationFingerprint());
        node.put("previousCommittedFingerprint", event.previousCommittedFingerprint());
        node.put("installedFingerprintBefore", event.installedFingerprintBefore());
        node.put("installedFingerprintAfter", event.installedFingerprintAfter());
        node.put("routerGenerationBefore", event.routerGenerationBefore());
        node.put("routerGenerationAfter", event.routerGenerationAfter());
        node.put("authenticationResult", event.authenticationResult().name());
        node.put("validationResult", event.validationResult().name());
        node.put("duplicateClassification", event.duplicateClassification().name());
        node.put("mutationStatus", event.mutationStatus().name());
        node.put("responseClassification", event.responseClassification().name());
        node.put("responseFingerprint", event.responseFingerprint());
        node.put("observedSupervisorEventFingerprint",
                event.observedSupervisorEventFingerprint());
        node.put("applicationCommitStatus", event.applicationCommitStatus().name());
        node.put("retryAttempt", event.retryAttempt());
        node.put("reasonCode", event.reasonCode());
        node.put("occurredAt", event.occurredAt().toString());
        node.set("metadata", metadataNode(event.metadata()));
        node.put("predecessorFingerprint", event.predecessorFingerprint());
        if (includeFingerprint) {
            node.put("currentFingerprint", event.currentFingerprint());
        }
        return node;
    }

    private static ObjectNode metadataNode(Map<String, String> metadata) {
        ObjectNode node = MAPPER.createObjectNode();
        new TreeMap<>(metadata).forEach(node::put);
        return node;
    }

    private static Map<String, String> metadata(ObjectNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isObject()
                || value.size() > EnterpriseLabCommandLedgerEvent.HARD_MAX_METADATA_ENTRIES) {
            throw failure(Failure.MALFORMED_EVENT,
                    field + " must be a bounded JSON object");
        }
        TreeMap<String, String> result = new TreeMap<>();
        value.fields().forEachRemaining(entry -> {
            if (!entry.getValue().isTextual()) {
                throw failure(Failure.MALFORMED_EVENT,
                        field + " values must be JSON strings");
            }
            result.put(entry.getKey(), entry.getValue().textValue());
        });
        return Map.copyOf(result);
    }

    private static ObjectNode parse(byte[] encoded) {
        try (JsonParser parser = JSON_FACTORY.createParser(strictUtf8(encoded))) {
            JsonNode parsed = MAPPER.readTree(parser);
            if (parsed == null || !parsed.isObject() || parser.nextToken() != null) {
                throw failure(Failure.MALFORMED_EVENT,
                        "command ledger event must contain one JSON object");
            }
            return (ObjectNode) parsed;
        } catch (CodecException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw failure(Failure.MALFORMED_EVENT,
                    "command ledger event is not valid strict JSON", exception);
        }
    }

    private static void requireExactFields(ObjectNode node) {
        Set<String> actual = new HashSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        Set<String> unknown = new HashSet<>(actual);
        unknown.removeAll(FIELDS);
        if (!unknown.isEmpty()) {
            throw failure(Failure.UNKNOWN_FIELD,
                    "command ledger event contains unknown fields");
        }
        if (!actual.equals(FIELDS)) {
            throw failure(Failure.MISSING_FIELD,
                    "command ledger event is missing required fields");
        }
    }

    private static void requireSchema(String value) {
        if (!EnterpriseLabCommandLedgerEvent.SCHEMA_VERSION.equals(value)) {
            throw failure(Failure.UNSUPPORTED_VERSION,
                    "command ledger schemaVersion is unsupported");
        }
    }

    private static String text(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw failure(Failure.MALFORMED_EVENT,
                    field + " must be a JSON string");
        }
        return value.textValue();
    }

    private static Optional<String> optionalText(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null) {
            throw failure(Failure.MISSING_FIELD, field + " is missing");
        }
        if (value.isNull()) {
            return Optional.empty();
        }
        if (!value.isTextual()) {
            throw failure(Failure.MALFORMED_EVENT,
                    field + " must be null or a JSON string");
        }
        return Optional.of(value.textValue());
    }

    private static void optionalText(
            ObjectNode node,
            String field,
            Optional<String> value) {
        if (value.isPresent()) {
            node.put(field, value.orElseThrow());
        } else {
            node.putNull(field);
        }
    }

    private static long number(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw failure(Failure.MALFORMED_EVENT,
                    field + " must be a bounded integer");
        }
        return value.longValue();
    }

    private static int boundedInt(ObjectNode node, String field) {
        long value = number(node, field);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw failure(Failure.MALFORMED_EVENT,
                    field + " must fit a bounded integer");
        }
        return (int) value;
    }

    private static Instant instant(ObjectNode node, String field) {
        String value = text(node, field);
        try {
            Instant parsed = Instant.parse(value);
            if (!parsed.toString().equals(value)) {
                throw failure(Failure.NON_CANONICAL_EVENT,
                        field + " must use canonical UTC form");
            }
            return parsed;
        } catch (DateTimeParseException exception) {
            throw failure(Failure.MALFORMED_EVENT,
                    field + " must be an ISO-8601 UTC instant", exception);
        }
    }

    private static <E extends Enum<E>> E enumValue(
            ObjectNode node,
            String field,
            Class<E> enumType) {
        String value = text(node, field);
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw failure(Failure.UNKNOWN_EVENT_OR_VALUE,
                    field + " contains an unsupported value", exception);
        }
    }

    private static byte[] requireInput(byte[] encoded) {
        if (encoded == null || encoded.length == 0) {
            throw failure(Failure.MALFORMED_EVENT,
                    "command ledger event cannot be null or empty");
        }
        if (encoded.length > EnterpriseLabCommandLedgerEvent.HARD_MAX_EVENT_BYTES) {
            throw failure(Failure.EXCEEDED_BOUNDS,
                    "command ledger event exceeds the hard byte limit");
        }
        return Arrays.copyOf(encoded, encoded.length);
    }

    private static String strictUtf8(byte[] encoded) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw failure(Failure.MALFORMED_EVENT,
                    "command ledger event is not valid UTF-8", exception);
        }
    }

    private static byte[] write(JsonNode node) {
        try {
            return MAPPER.writeValueAsBytes(node);
        } catch (JsonProcessingException exception) {
            throw failure(Failure.MALFORMED_EVENT,
                    "command ledger event could not be encoded", exception);
        }
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    private static void requireFingerprint(
            String expected,
            String actual,
            String field) {
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII))) {
            throw failure(Failure.FINGERPRINT_MISMATCH,
                    field + " does not match canonical event content");
        }
    }

    private static CodecException failure(Failure failure, String message) {
        return new CodecException(failure, message);
    }

    private static CodecException failure(
            Failure failure,
            String message,
            Throwable cause) {
        return new CodecException(failure, message, cause);
    }

    public enum Failure {
        MALFORMED_EVENT,
        UNSUPPORTED_VERSION,
        UNKNOWN_FIELD,
        MISSING_FIELD,
        UNKNOWN_EVENT_OR_VALUE,
        INVALID_EVENT,
        FINGERPRINT_MISMATCH,
        REQUEST_MISMATCH,
        RESPONSE_MISMATCH,
        NON_CANONICAL_EVENT,
        EXCEEDED_BOUNDS
    }

    public static final class CodecException extends IllegalArgumentException {
        private final Failure failure;

        private CodecException(Failure failure, String message) {
            super(message);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        private CodecException(Failure failure, String message, Throwable cause) {
            super(message, cause);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        public Failure failure() {
            return failure;
        }
    }
}
