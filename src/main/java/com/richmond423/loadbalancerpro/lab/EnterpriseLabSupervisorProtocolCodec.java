package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.RequestDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
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
import java.util.TreeSet;

/** Strict bounded canonical JSON codec for supervisor business messages. */
public final class EnterpriseLabSupervisorProtocolCodec {
    private static final String EMPTY_FINGERPRINT = "0".repeat(64);
    private static final Set<String> REQUEST_FIELDS = Set.of(
            "schemaVersion",
            "requestId",
            "requestFingerprint",
            "commandType",
            "applicationInstanceId",
            "applicationOwnershipRecordFingerprint",
            "applicationOwnerGeneration",
            "expectedSupervisorInstanceId",
            "expectedSupervisorGeneration",
            "transactionId",
            "experimentId",
            "allocationPurpose",
            "allocation",
            "allocationFingerprint",
            "previousCommittedFingerprint",
            "requestedAt",
            "metadata");
    private static final Set<String> RESPONSE_FIELDS = Set.of(
            "schemaVersion",
            "requestId",
            "requestFingerprint",
            "commandType",
            "supervisorInstanceId",
            "supervisorGeneration",
            "observedApplicationGeneration",
            "commandClassification",
            "status",
            "actionPerformed",
            "installedAllocation",
            "installedFingerprint",
            "routerGeneration",
            "durableStateGeneration",
            "verificationResult",
            "reasonCode",
            "reason",
            "respondedAt",
            "responseFingerprint");
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper(JSON_FACTORY);

    private final EnterpriseLabExperimentTargetCatalog targetCatalog;
    private final EnterpriseLabInstalledAllocationSnapshotCodec installedCodec;

    public EnterpriseLabSupervisorProtocolCodec(
            EnterpriseLabExperimentTargetCatalog targetCatalog) {
        this.targetCatalog = Objects.requireNonNull(
                targetCatalog, "targetCatalog cannot be null");
        this.installedCodec = new EnterpriseLabInstalledAllocationSnapshotCodec(targetCatalog);
    }

    public Request issue(RequestDraft draft) {
        RequestDraft safe = Objects.requireNonNull(draft, "draft cannot be null");
        Request unsigned = request(safe, EMPTY_FINGERPRINT);
        validateTargetBinding(unsigned.allocation());
        return request(safe, canonicalRequestFingerprint(unsigned));
    }

    public Response issue(Request request, ResponseDraft draft) {
        Request safeRequest = Objects.requireNonNull(request, "request cannot be null");
        ResponseDraft safe = Objects.requireNonNull(draft, "draft cannot be null");
        Response unsigned = response(safe, EMPTY_FINGERPRINT);
        validateTargetBinding(unsigned.installedAllocation());
        Response issued = response(safe, canonicalResponseFingerprint(unsigned));
        requireResponseForRequest(issued, safeRequest);
        return issued;
    }

    public byte[] encodeRequest(Request request) {
        Request safe = Objects.requireNonNull(request, "request cannot be null");
        validateTargetBinding(safe.allocation());
        requireFingerprint(
                canonicalRequestFingerprint(safe),
                safe.requestFingerprint(),
                "requestFingerprint");
        return boundedWrite(
                requestNode(safe, true),
                EnterpriseLabSupervisorProtocol.HARD_MAX_REQUEST_BYTES,
                "supervisor request");
    }

    public Request decodeRequest(byte[] encoded) {
        byte[] safe = requireInput(
                encoded,
                EnterpriseLabSupervisorProtocol.HARD_MAX_REQUEST_BYTES,
                "supervisor request");
        ObjectNode root = parse(safe, "supervisor request");
        requireExactFields(root, REQUEST_FIELDS, "supervisor request");
        requireSchema(text(root, "schemaVersion"));
        Request request;
        try {
            request = new Request(
                    text(root, "schemaVersion"),
                    text(root, "requestId"),
                    text(root, "requestFingerprint"),
                    enumValue(root, "commandType", CommandType.class),
                    text(root, "applicationInstanceId"),
                    text(root, "applicationOwnershipRecordFingerprint"),
                    number(root, "applicationOwnerGeneration"),
                    text(root, "expectedSupervisorInstanceId"),
                    number(root, "expectedSupervisorGeneration"),
                    text(root, "transactionId"),
                    optionalText(root, "experimentId"),
                    enumValue(root, "allocationPurpose", AllocationPurpose.class),
                    optionalRouting(root, "allocation"),
                    text(root, "allocationFingerprint"),
                    text(root, "previousCommittedFingerprint"),
                    instant(root, "requestedAt"),
                    metadata(root, "metadata"));
            validateTargetBinding(request.allocation());
            requireFingerprint(
                    canonicalRequestFingerprint(request),
                    request.requestFingerprint(),
                    "requestFingerprint");
        } catch (CodecException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw failure(Failure.INVALID_MESSAGE,
                    "supervisor request violates protocol invariants", exception);
        }
        if (!Arrays.equals(safe, encodeRequest(request))) {
            throw failure(Failure.NON_CANONICAL_MESSAGE,
                    "supervisor request is not canonical");
        }
        return request;
    }

    public byte[] encodeResponse(Response response) {
        Response safe = Objects.requireNonNull(response, "response cannot be null");
        validateTargetBinding(safe.installedAllocation());
        requireFingerprint(
                canonicalResponseFingerprint(safe),
                safe.responseFingerprint(),
                "responseFingerprint");
        return boundedWrite(
                responseNode(safe, true),
                EnterpriseLabSupervisorProtocol.HARD_MAX_RESPONSE_BYTES,
                "supervisor response");
    }

    public Response decodeResponse(byte[] encoded, Request request) {
        Request safeRequest = Objects.requireNonNull(request, "request cannot be null");
        byte[] safe = requireInput(
                encoded,
                EnterpriseLabSupervisorProtocol.HARD_MAX_RESPONSE_BYTES,
                "supervisor response");
        ObjectNode root = parse(safe, "supervisor response");
        requireExactFields(root, RESPONSE_FIELDS, "supervisor response");
        requireSchema(text(root, "schemaVersion"));
        Response response;
        try {
            response = new Response(
                    text(root, "schemaVersion"),
                    text(root, "requestId"),
                    text(root, "requestFingerprint"),
                    enumValue(root, "commandType", CommandType.class),
                    text(root, "supervisorInstanceId"),
                    number(root, "supervisorGeneration"),
                    number(root, "observedApplicationGeneration"),
                    enumValue(root, "commandClassification", CommandClassification.class),
                    enumValue(root, "status", ResponseStatus.class),
                    bool(root, "actionPerformed"),
                    optionalInstalled(root, "installedAllocation"),
                    text(root, "installedFingerprint"),
                    number(root, "routerGeneration"),
                    number(root, "durableStateGeneration"),
                    enumValue(root, "verificationResult", VerificationResult.class),
                    text(root, "reasonCode"),
                    text(root, "reason"),
                    instant(root, "respondedAt"),
                    text(root, "responseFingerprint"));
            validateTargetBinding(response.installedAllocation());
            requireFingerprint(
                    canonicalResponseFingerprint(response),
                    response.responseFingerprint(),
                    "responseFingerprint");
        } catch (CodecException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw failure(Failure.INVALID_MESSAGE,
                    "supervisor response violates protocol invariants", exception);
        }
        if (!Arrays.equals(safe, encodeResponse(response))) {
            throw failure(Failure.NON_CANONICAL_MESSAGE,
                    "supervisor response is not canonical");
        }
        requireResponseForRequest(response, safeRequest);
        return response;
    }

    public String canonicalRequestFingerprint(Request request) {
        Request safe = Objects.requireNonNull(request, "request cannot be null");
        return sha256(write(requestNode(safe, false)));
    }

    public String canonicalResponseFingerprint(Response response) {
        Response safe = Objects.requireNonNull(response, "response cannot be null");
        return sha256(write(responseNode(safe, false)));
    }

    private Request request(RequestDraft draft, String fingerprint) {
        return new Request(
                EnterpriseLabSupervisorProtocol.SCHEMA_VERSION,
                draft.requestId(),
                fingerprint,
                draft.commandType(),
                draft.applicationInstanceId(),
                draft.applicationOwnershipRecordFingerprint(),
                draft.applicationOwnerGeneration(),
                draft.expectedSupervisorInstanceId(),
                draft.expectedSupervisorGeneration(),
                draft.transactionId(),
                draft.experimentId(),
                draft.allocationPurpose(),
                draft.allocation(),
                draft.allocationFingerprint(),
                draft.previousCommittedFingerprint(),
                draft.requestedAt(),
                draft.metadata());
    }

    private Response response(ResponseDraft draft, String fingerprint) {
        return new Response(
                EnterpriseLabSupervisorProtocol.SCHEMA_VERSION,
                draft.requestId(),
                draft.requestFingerprint(),
                draft.commandType(),
                draft.supervisorInstanceId(),
                draft.supervisorGeneration(),
                draft.observedApplicationGeneration(),
                draft.commandClassification(),
                draft.status(),
                draft.actionPerformed(),
                draft.installedAllocation(),
                draft.installedFingerprint(),
                draft.routerGeneration(),
                draft.durableStateGeneration(),
                draft.verificationResult(),
                draft.reasonCode(),
                draft.reason(),
                draft.respondedAt(),
                fingerprint);
    }

    private ObjectNode requestNode(Request request, boolean includeFingerprint) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("schemaVersion", request.schemaVersion());
        node.put("requestId", request.requestId());
        if (includeFingerprint) {
            node.put("requestFingerprint", request.requestFingerprint());
        }
        node.put("commandType", request.commandType().name());
        node.put("applicationInstanceId", request.applicationInstanceId());
        node.put("applicationOwnershipRecordFingerprint",
                request.applicationOwnershipRecordFingerprint());
        node.put("applicationOwnerGeneration", request.applicationOwnerGeneration());
        node.put("expectedSupervisorInstanceId", request.expectedSupervisorInstanceId());
        node.put("expectedSupervisorGeneration", request.expectedSupervisorGeneration());
        node.put("transactionId", request.transactionId());
        optionalText(node, "experimentId", request.experimentId());
        node.put("allocationPurpose", request.allocationPurpose().name());
        if (request.allocation().isPresent()) {
            node.set("allocation", EnterpriseLabInstalledAllocationSnapshotCodec
                    .canonicalRoutingNode(request.allocation().orElseThrow()));
        } else {
            node.putNull("allocation");
        }
        node.put("allocationFingerprint", request.allocationFingerprint());
        node.put("previousCommittedFingerprint", request.previousCommittedFingerprint());
        node.put("requestedAt", request.requestedAt().toString());
        node.set("metadata", metadataNode(request.metadata()));
        return node;
    }

    private ObjectNode responseNode(Response response, boolean includeFingerprint) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("schemaVersion", response.schemaVersion());
        node.put("requestId", response.requestId());
        node.put("requestFingerprint", response.requestFingerprint());
        node.put("commandType", response.commandType().name());
        node.put("supervisorInstanceId", response.supervisorInstanceId());
        node.put("supervisorGeneration", response.supervisorGeneration());
        node.put("observedApplicationGeneration", response.observedApplicationGeneration());
        node.put("commandClassification", response.commandClassification().name());
        node.put("status", response.status().name());
        node.put("actionPerformed", response.actionPerformed());
        if (response.installedAllocation().isPresent()) {
            node.set("installedAllocation", installedNode(
                    response.installedAllocation().orElseThrow()));
        } else {
            node.putNull("installedAllocation");
        }
        node.put("installedFingerprint", response.installedFingerprint());
        node.put("routerGeneration", response.routerGeneration());
        node.put("durableStateGeneration", response.durableStateGeneration());
        node.put("verificationResult", response.verificationResult().name());
        node.put("reasonCode", response.reasonCode());
        node.put("reason", response.reason());
        node.put("respondedAt", response.respondedAt().toString());
        if (includeFingerprint) {
            node.put("responseFingerprint", response.responseFingerprint());
        }
        return node;
    }

    private JsonNode installedNode(EnterpriseLabInstalledAllocationSnapshot installed) {
        try {
            return MAPPER.readTree(installedCodec.encode(installed));
        } catch (IOException exception) {
            throw failure(Failure.MALFORMED_MESSAGE,
                    "installed allocation could not be embedded canonically", exception);
        }
    }

    private Optional<EnterpriseLabInstalledAllocationSnapshot> optionalInstalled(
            ObjectNode node,
            String field) {
        JsonNode value = node.get(field);
        if (value == null) {
            throw failure(Failure.MISSING_FIELD, field + " is missing");
        }
        if (value.isNull()) {
            return Optional.empty();
        }
        return Optional.of(installedCodec.decode(write(value)));
    }

    private Optional<EnterpriseLabLoopbackAllocationSnapshot> optionalRouting(
            ObjectNode node,
            String field) {
        JsonNode value = node.get(field);
        if (value == null) {
            throw failure(Failure.MISSING_FIELD, field + " is missing");
        }
        if (value.isNull()) {
            return Optional.empty();
        }
        return Optional.of(EnterpriseLabInstalledAllocationSnapshotCodec.decodeRoutingNode(value));
    }

    private void validateTargetBinding(Optional<?> value) {
        if (value.isEmpty()) {
            return;
        }
        EnterpriseLabLoopbackAllocationSnapshot snapshot;
        Object item = value.orElseThrow();
        if (item instanceof EnterpriseLabInstalledAllocationSnapshot installed) {
            snapshot = installed.routingSnapshot();
            installedCodec.encode(installed);
        } else if (item instanceof EnterpriseLabLoopbackAllocationSnapshot routing) {
            snapshot = routing;
        } else {
            throw new IllegalArgumentException("unsupported allocation target binding");
        }
        var targets = targetCatalog.findTargets(snapshot.scenarioId())
                .orElseThrow(() -> failure(Failure.TARGET_MISMATCH,
                        "supervisor allocation is not bound to an approved loopback scenario"));
        TreeSet<String> expected = new TreeSet<>();
        targets.forEach(target -> expected.add(target.backendId()));
        if (!expected.equals(new TreeSet<>(snapshot.allocations().keySet()))) {
            throw failure(Failure.TARGET_MISMATCH,
                    "supervisor allocation backend set does not match approved loopback targets");
        }
    }

    private static ObjectNode parse(byte[] encoded, String subject) {
        try (JsonParser parser = JSON_FACTORY.createParser(strictUtf8(encoded))) {
            JsonNode parsed = MAPPER.readTree(parser);
            if (parsed == null || !parsed.isObject() || parser.nextToken() != null) {
                throw failure(Failure.MALFORMED_MESSAGE,
                        subject + " must contain one JSON object");
            }
            return (ObjectNode) parsed;
        } catch (CodecException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw failure(Failure.MALFORMED_MESSAGE,
                    subject + " is not valid strict JSON", exception);
        }
    }

    private static void requireExactFields(
            ObjectNode node,
            Set<String> expected,
            String subject) {
        Set<String> actual = new HashSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        Set<String> unknown = new HashSet<>(actual);
        unknown.removeAll(expected);
        if (!unknown.isEmpty()) {
            throw failure(Failure.UNKNOWN_FIELD,
                    subject + " contains unknown fields");
        }
        if (!actual.equals(expected)) {
            throw failure(Failure.MISSING_FIELD,
                    subject + " is missing required fields");
        }
    }

    private static void requireSchema(String value) {
        if (!EnterpriseLabSupervisorProtocol.SCHEMA_VERSION.equals(value)) {
            throw failure(Failure.UNSUPPORTED_VERSION,
                    "supervisor protocol schemaVersion is unsupported");
        }
    }

    private static String text(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw failure(Failure.MALFORMED_MESSAGE, field + " must be a JSON string");
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
            throw failure(Failure.MALFORMED_MESSAGE,
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
            throw failure(Failure.MALFORMED_MESSAGE,
                    field + " must be a bounded integer");
        }
        return value.longValue();
    }

    private static boolean bool(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isBoolean()) {
            throw failure(Failure.MALFORMED_MESSAGE,
                    field + " must be a JSON boolean");
        }
        return value.booleanValue();
    }

    private static Instant instant(ObjectNode node, String field) {
        String value = text(node, field);
        try {
            Instant parsed = Instant.parse(value);
            if (!parsed.toString().equals(value)) {
                throw failure(Failure.NON_CANONICAL_MESSAGE,
                        field + " must use canonical UTC form");
            }
            return parsed;
        } catch (DateTimeParseException exception) {
            throw failure(Failure.MALFORMED_MESSAGE,
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
            throw failure(Failure.UNKNOWN_COMMAND_OR_VALUE,
                    field + " contains an unsupported value", exception);
        }
    }

    private static ObjectNode metadataNode(Map<String, String> metadata) {
        ObjectNode node = MAPPER.createObjectNode();
        new TreeMap<>(metadata).forEach(node::put);
        return node;
    }

    private static Map<String, String> metadata(ObjectNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isObject()
                || value.size() > EnterpriseLabSupervisorProtocol.HARD_MAX_METADATA_ENTRIES) {
            throw failure(Failure.MALFORMED_MESSAGE,
                    field + " must be a bounded JSON object");
        }
        TreeMap<String, String> metadata = new TreeMap<>();
        value.fields().forEachRemaining(entry -> {
            if (!entry.getValue().isTextual()) {
                throw failure(Failure.MALFORMED_MESSAGE,
                        field + " values must be JSON strings");
            }
            metadata.put(entry.getKey(), entry.getValue().textValue());
        });
        return Map.copyOf(metadata);
    }

    private static byte[] requireInput(byte[] encoded, int maximum, String subject) {
        if (encoded == null || encoded.length == 0) {
            throw failure(Failure.MALFORMED_MESSAGE, subject + " cannot be null or empty");
        }
        if (encoded.length > maximum) {
            throw failure(Failure.EXCEEDED_BOUNDS, subject + " exceeds the hard byte limit");
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
            throw failure(Failure.MALFORMED_MESSAGE,
                    "supervisor protocol message is not valid UTF-8", exception);
        }
    }

    private static byte[] boundedWrite(JsonNode node, int maximum, String subject) {
        byte[] encoded = write(node);
        if (encoded.length > maximum) {
            throw failure(Failure.EXCEEDED_BOUNDS, subject + " exceeds the hard byte limit");
        }
        return encoded;
    }

    private static byte[] write(JsonNode node) {
        try {
            return MAPPER.writeValueAsBytes(node);
        } catch (JsonProcessingException exception) {
            throw failure(Failure.MALFORMED_MESSAGE,
                    "supervisor protocol message could not be encoded", exception);
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

    private static void requireFingerprint(String expected, String actual, String field) {
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII))) {
            throw failure(Failure.FINGERPRINT_MISMATCH,
                    field + " does not match canonical protocol content");
        }
    }

    private static void requireResponseForRequest(Response response, Request request) {
        if (!response.validatesAgainst(request)) {
            throw failure(Failure.REQUEST_MISMATCH,
                    "supervisor response does not match the exact request and accepted fences");
        }
    }

    private static CodecException failure(Failure classification, String message) {
        return new CodecException(classification, message);
    }

    private static CodecException failure(
            Failure classification,
            String message,
            Throwable cause) {
        return new CodecException(classification, message, cause);
    }

    public enum Failure {
        MALFORMED_MESSAGE,
        UNSUPPORTED_VERSION,
        UNKNOWN_FIELD,
        MISSING_FIELD,
        UNKNOWN_COMMAND_OR_VALUE,
        INVALID_MESSAGE,
        TARGET_MISMATCH,
        FINGERPRINT_MISMATCH,
        NON_CANONICAL_MESSAGE,
        REQUEST_MISMATCH,
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
