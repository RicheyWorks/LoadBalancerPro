package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Strict bounded canonical JSON evidence codec for installed router read-back. */
public final class EnterpriseLabInstalledAllocationSnapshotCodec {
    public static final int HARD_MAX_SNAPSHOT_BYTES = 32_768;

    private static final Set<String> SNAPSHOT_FIELDS = Set.of(
            "schemaVersion",
            "routingSnapshot",
            "routerGeneration",
            "allocationFingerprint",
            "eligibleBackendIds",
            "excludedBackendIds",
            "installedAt",
            "installationReason",
            "ownerGeneration");
    private static final Set<String> ROUTING_FIELDS = Set.of(
            "schemaVersion",
            "scenarioId",
            "revision",
            "sourceDecisionId",
            "kind",
            "allocations");
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper(JSON_FACTORY);

    private final EnterpriseLabExperimentTargetCatalog targetCatalog;

    public EnterpriseLabInstalledAllocationSnapshotCodec(
            EnterpriseLabExperimentTargetCatalog targetCatalog) {
        this.targetCatalog = Objects.requireNonNull(
                targetCatalog, "targetCatalog cannot be null");
    }

    public byte[] encode(EnterpriseLabInstalledAllocationSnapshot snapshot) {
        EnterpriseLabInstalledAllocationSnapshot safe = Objects.requireNonNull(
                snapshot, "snapshot cannot be null");
        validateTargetBinding(safe.routingSnapshot());
        byte[] encoded = write(canonicalNode(safe));
        if (encoded.length > HARD_MAX_SNAPSHOT_BYTES) {
            throw failure(Failure.EXCEEDED_BOUNDS,
                    "installed allocation snapshot exceeds the hard byte limit");
        }
        return encoded;
    }

    public EnterpriseLabInstalledAllocationSnapshot decode(byte[] encoded) {
        byte[] safe = requireInput(encoded);
        ObjectNode root;
        try (JsonParser parser = JSON_FACTORY.createParser(strictUtf8(safe))) {
            JsonNode parsed = MAPPER.readTree(parser);
            if (parsed == null || !parsed.isObject() || parser.nextToken() != null) {
                throw failure(Failure.MALFORMED_SNAPSHOT,
                        "installed allocation evidence must contain one JSON object");
            }
            root = (ObjectNode) parsed;
        } catch (CodecException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw failure(Failure.MALFORMED_SNAPSHOT,
                    "installed allocation evidence is not valid strict JSON", exception);
        }

        requireExactFields(root, SNAPSHOT_FIELDS, "installed allocation snapshot");
        String schemaVersion = text(root, "schemaVersion");
        if (!EnterpriseLabInstalledAllocationSnapshot.SCHEMA_VERSION.equals(schemaVersion)) {
            throw failure(Failure.UNSUPPORTED_VERSION,
                    "installed allocation snapshot schemaVersion is unsupported");
        }
        EnterpriseLabInstalledAllocationSnapshot snapshot;
        try {
            EnterpriseLabLoopbackAllocationSnapshot routingSnapshot =
                    decodeRoutingNode(root.get("routingSnapshot"));
            snapshot = new EnterpriseLabInstalledAllocationSnapshot(
                    schemaVersion,
                    routingSnapshot,
                    number(root, "routerGeneration"),
                    text(root, "allocationFingerprint"),
                    identifiers(root, "eligibleBackendIds"),
                    identifiers(root, "excludedBackendIds"),
                    instant(root, "installedAt"),
                    text(root, "installationReason"),
                    number(root, "ownerGeneration"));
            validateTargetBinding(routingSnapshot);
        } catch (CodecException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw failure(Failure.INVALID_SNAPSHOT,
                    "installed allocation evidence violates router invariants", exception);
        }
        if (!Arrays.equals(safe, encode(snapshot))) {
            throw failure(Failure.NON_CANONICAL_SNAPSHOT,
                    "installed allocation evidence is not canonical");
        }
        return snapshot;
    }

    private void validateTargetBinding(EnterpriseLabLoopbackAllocationSnapshot routingSnapshot) {
        List<EnterpriseLabLoopbackTarget> targets = targetCatalog
                .findTargets(routingSnapshot.scenarioId())
                .orElseThrow(() -> failure(Failure.TARGET_MISMATCH,
                        "installed allocation scenario is not bound to approved loopback targets"));
        TreeSet<String> expected = new TreeSet<>();
        targets.forEach(target -> expected.add(target.backendId()));
        if (!expected.equals(new TreeSet<>(routingSnapshot.allocations().keySet()))) {
            throw failure(Failure.TARGET_MISMATCH,
                    "installed allocation backend set does not match approved loopback targets");
        }
    }

    private static ObjectNode canonicalNode(EnterpriseLabInstalledAllocationSnapshot snapshot) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("schemaVersion", snapshot.schemaVersion());
        node.set("routingSnapshot", canonicalRoutingNode(snapshot.routingSnapshot()));
        node.put("routerGeneration", snapshot.routerGeneration());
        node.put("allocationFingerprint", snapshot.allocationFingerprint());
        node.set("eligibleBackendIds", identifiersNode(snapshot.eligibleBackendIds()));
        node.set("excludedBackendIds", identifiersNode(snapshot.excludedBackendIds()));
        node.put("installedAt", snapshot.installedAt().toString());
        node.put("installationReason", snapshot.installationReason());
        node.put("ownerGeneration", snapshot.ownerGeneration());
        return node;
    }

    static ObjectNode canonicalRoutingNode(EnterpriseLabLoopbackAllocationSnapshot snapshot) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("schemaVersion", snapshot.schemaVersion());
        node.put("scenarioId", snapshot.scenarioId());
        node.put("revision", snapshot.revision());
        node.put("sourceDecisionId", snapshot.sourceDecisionId());
        node.put("kind", snapshot.kind().name());
        ObjectNode allocations = MAPPER.createObjectNode();
        snapshot.allocations().forEach((backendId, share) ->
                allocations.put(backendId, canonicalShare(share)));
        node.set("allocations", allocations);
        return node;
    }

    static EnterpriseLabLoopbackAllocationSnapshot decodeRoutingNode(JsonNode value) {
        ObjectNode node = object(value, "routingSnapshot");
        requireExactFields(node, ROUTING_FIELDS, "routingSnapshot");
        String schemaVersion = text(node, "schemaVersion");
        if (!EnterpriseLabLoopbackAllocationSnapshot.SCHEMA_VERSION.equals(schemaVersion)) {
            throw failure(Failure.UNSUPPORTED_VERSION,
                    "routing snapshot schemaVersion is unsupported");
        }
        try {
            return new EnterpriseLabLoopbackAllocationSnapshot(
                    schemaVersion,
                    text(node, "scenarioId"),
                    number(node, "revision"),
                    text(node, "sourceDecisionId"),
                    enumValue(node, "kind", EnterpriseLabLoopbackAllocationSnapshot.Kind.class),
                    allocation(node, "allocations"));
        } catch (CodecException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw failure(Failure.INVALID_SNAPSHOT,
                    "routing snapshot violates allocation invariants", exception);
        }
    }

    private static ArrayNode identifiersNode(List<String> identifiers) {
        ArrayNode node = MAPPER.createArrayNode();
        identifiers.forEach(node::add);
        return node;
    }

    private static java.util.Map<String, Double> allocation(ObjectNode parent, String field) {
        ObjectNode node = object(parent.get(field), field);
        if (node.isEmpty() || node.size() > EnterpriseLabLoopbackAllocationSnapshot.HARD_MAX_BACKENDS) {
            throw failure(Failure.INVALID_SNAPSHOT,
                    field + " backend count is outside hard bounds");
        }
        java.util.Map<String, Double> allocations = new java.util.TreeMap<>();
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (!value.isTextual()) {
                throw failure(Failure.MALFORMED_SNAPSHOT,
                        field + " shares must use canonical hexadecimal strings");
            }
            String encoded = value.textValue();
            double share;
            try {
                share = Double.parseDouble(encoded);
            } catch (NumberFormatException exception) {
                throw failure(Failure.MALFORMED_SNAPSHOT,
                        field + " contains an invalid share", exception);
            }
            if (!encoded.equals(canonicalShare(share))) {
                throw failure(Failure.NON_CANONICAL_SNAPSHOT,
                        field + " contains a non-canonical share");
            }
            allocations.put(entry.getKey(), share);
        });
        return allocations;
    }

    private static List<String> identifiers(ObjectNode parent, String field) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isArray()
                || node.size() > EnterpriseLabLoopbackAllocationSnapshot.HARD_MAX_BACKENDS) {
            throw failure(Failure.MALFORMED_SNAPSHOT,
                    field + " must be a bounded JSON array");
        }
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        Set<String> unique = new HashSet<>();
        String previous = null;
        for (JsonNode value : node) {
            if (!value.isTextual() || value.textValue().isBlank()) {
                throw failure(Failure.MALFORMED_SNAPSHOT,
                        field + " entries must be canonical identifiers");
            }
            String identifier = value.textValue();
            if (!unique.add(identifier)
                    || (previous != null && previous.compareTo(identifier) >= 0)) {
                throw failure(Failure.NON_CANONICAL_SNAPSHOT,
                        field + " entries must be unique and sorted");
            }
            values.add(identifier);
            previous = identifier;
        }
        return List.copyOf(values);
    }

    private static ObjectNode object(JsonNode node, String subject) {
        if (node == null || !node.isObject()) {
            throw failure(Failure.MALFORMED_SNAPSHOT,
                    subject + " must be a JSON object");
        }
        return (ObjectNode) node;
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

    private static String text(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw failure(Failure.MALFORMED_SNAPSHOT,
                    field + " must be a JSON string");
        }
        return value.textValue();
    }

    private static long number(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw failure(Failure.MALFORMED_SNAPSHOT,
                    field + " must be a bounded integer");
        }
        return value.longValue();
    }

    private static Instant instant(ObjectNode node, String field) {
        String value = text(node, field);
        try {
            Instant parsed = Instant.parse(value);
            if (!parsed.toString().equals(value)) {
                throw failure(Failure.NON_CANONICAL_SNAPSHOT,
                        field + " must use canonical UTC form");
            }
            return parsed;
        } catch (DateTimeParseException exception) {
            throw failure(Failure.MALFORMED_SNAPSHOT,
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
            throw failure(Failure.INVALID_SNAPSHOT,
                    field + " contains an unsupported value", exception);
        }
    }

    private static String canonicalShare(Double value) {
        double share = Objects.requireNonNull(value, "allocation shares cannot be null");
        if (share == 0.0d) {
            share = 0.0d;
        }
        return Double.toHexString(share);
    }

    private static byte[] requireInput(byte[] encoded) {
        if (encoded == null || encoded.length == 0) {
            throw failure(Failure.MALFORMED_SNAPSHOT,
                    "installed allocation evidence cannot be null or empty");
        }
        if (encoded.length > HARD_MAX_SNAPSHOT_BYTES) {
            throw failure(Failure.EXCEEDED_BOUNDS,
                    "installed allocation evidence exceeds the hard byte limit");
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
            throw failure(Failure.MALFORMED_SNAPSHOT,
                    "installed allocation evidence is not valid UTF-8", exception);
        }
    }

    private static byte[] write(JsonNode node) {
        try {
            return MAPPER.writeValueAsBytes(node);
        } catch (JsonProcessingException exception) {
            throw failure(Failure.MALFORMED_SNAPSHOT,
                    "installed allocation evidence could not be encoded", exception);
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
        MALFORMED_SNAPSHOT,
        UNSUPPORTED_VERSION,
        UNKNOWN_FIELD,
        MISSING_FIELD,
        INVALID_SNAPSHOT,
        TARGET_MISMATCH,
        NON_CANONICAL_SNAPSHOT,
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
