package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.RecoveryClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransitionReason;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Strict canonical JSON codec for the supervisor's independently durable state. */
public final class EnterpriseLabSupervisorStateCodec {
    public static final int HARD_MAX_RECORD_BYTES = 192 * 1024;

    private static final Set<String> RECORD_FIELDS = Set.of(
            "schemaVersion",
            "supervisorInstanceId",
            "supervisorGeneration",
            "acceptedApplicationInstanceId",
            "acceptedApplicationOwnershipFingerprint",
            "acceptedApplicationGeneration",
            "previousApplicationInstanceId",
            "previousApplicationOwnershipFingerprint",
            "previousApplicationGeneration",
            "baselineAllocation",
            "installedAllocation",
            "intendedAllocation",
            "transactionId",
            "lastRequestId",
            "lastRequestFingerprint",
            "previousCommittedFingerprint",
            "transactionPhase",
            "lastCommitAt",
            "lastRecoveryClassification",
            "transitionReason",
            "durableStateGeneration",
            "predecessorRecordFingerprint",
            "currentRecordFingerprint");
    private static final Set<String> REASON_FIELDS = Set.of("code", "message");
    private static final Set<String> INSTALLED_FIELDS = Set.of(
            "schemaVersion",
            "routingSnapshot",
            "routerGeneration",
            "allocationFingerprint",
            "eligibleBackendIds",
            "excludedBackendIds",
            "installedAt",
            "installationReason",
            "ownerGeneration");
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(10)
                    .maxStringLength(HARD_MAX_RECORD_BYTES)
                    .maxNumberLength(32)
                    .build())
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper(JSON_FACTORY);

    private final EnterpriseLabInstalledAllocationSnapshotCodec installedCodec;

    public EnterpriseLabSupervisorStateCodec(
            EnterpriseLabExperimentTargetCatalog targetCatalog) {
        this.installedCodec = new EnterpriseLabInstalledAllocationSnapshotCodec(
                Objects.requireNonNull(targetCatalog, "targetCatalog cannot be null"));
    }

    public byte[] encode(EnterpriseLabSupervisorState state) {
        EnterpriseLabSupervisorState safe = Objects.requireNonNull(
                state, "state cannot be null");
        installedCodec.encode(safe.baselineAllocation());
        installedCodec.encode(safe.installedAllocation());
        safe.intendedAllocation().ifPresent(installedCodec::encode);
        ObjectNode root = canonicalContentNode(
                safe.schemaVersion(),
                safe.supervisorInstanceId(),
                safe.supervisorGeneration(),
                safe.acceptedApplicationInstanceId(),
                safe.acceptedApplicationOwnershipFingerprint(),
                safe.acceptedApplicationGeneration(),
                safe.previousApplicationInstanceId(),
                safe.previousApplicationOwnershipFingerprint(),
                safe.previousApplicationGeneration(),
                safe.baselineAllocation(),
                safe.installedAllocation(),
                safe.intendedAllocation(),
                safe.transactionId(),
                safe.lastRequestId(),
                safe.lastRequestFingerprint(),
                safe.previousCommittedFingerprint(),
                safe.transactionPhase(),
                safe.lastCommitAt(),
                safe.lastRecoveryClassification(),
                safe.transitionReason(),
                safe.durableStateGeneration(),
                safe.predecessorRecordFingerprint());
        root.put("currentRecordFingerprint", safe.currentRecordFingerprint());
        byte[] encoded = write(root);
        if (encoded.length > HARD_MAX_RECORD_BYTES) {
            throw failure(Failure.EXCEEDED_BOUNDS,
                    "supervisor state exceeds the hard byte limit");
        }
        return encoded;
    }

    public EnterpriseLabSupervisorState decode(byte[] encoded) {
        byte[] safe = requireInput(encoded);
        ObjectNode root;
        try (JsonParser parser = JSON_FACTORY.createParser(strictUtf8(safe))) {
            JsonNode parsed = MAPPER.readTree(parser);
            if (parsed == null || !parsed.isObject() || parser.nextToken() != null) {
                throw failure(Failure.MALFORMED_RECORD,
                        "supervisor state must contain exactly one JSON object");
            }
            root = (ObjectNode) parsed;
        } catch (CodecException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw failure(Failure.MALFORMED_RECORD,
                    "supervisor state is not valid strict JSON", exception);
        }
        requireExactFields(root, RECORD_FIELDS, "supervisor state");
        ObjectNode reason = object(root.get("transitionReason"), "transitionReason");
        requireExactFields(reason, REASON_FIELDS, "transitionReason");
        try {
            EnterpriseLabSupervisorState state = new EnterpriseLabSupervisorState(
                    text(root, "schemaVersion"),
                    text(root, "supervisorInstanceId"),
                    number(root, "supervisorGeneration"),
                    text(root, "acceptedApplicationInstanceId"),
                    text(root, "acceptedApplicationOwnershipFingerprint"),
                    number(root, "acceptedApplicationGeneration"),
                    text(root, "previousApplicationInstanceId"),
                    text(root, "previousApplicationOwnershipFingerprint"),
                    number(root, "previousApplicationGeneration"),
                    installed(root.get("baselineAllocation"), "baselineAllocation"),
                    installed(root.get("installedAllocation"), "installedAllocation"),
                    optionalInstalled(root.get("intendedAllocation")),
                    text(root, "transactionId"),
                    text(root, "lastRequestId"),
                    text(root, "lastRequestFingerprint"),
                    text(root, "previousCommittedFingerprint"),
                    enumValue(root, "transactionPhase", TransactionPhase.class),
                    instant(root, "lastCommitAt"),
                    enumValue(root, "lastRecoveryClassification", RecoveryClassification.class),
                    new TransitionReason(text(reason, "code"), text(reason, "message")),
                    number(root, "durableStateGeneration"),
                    text(root, "predecessorRecordFingerprint"),
                    text(root, "currentRecordFingerprint"));
            if (!Arrays.equals(safe, encode(state))) {
                throw failure(Failure.NON_CANONICAL_RECORD,
                        "supervisor state is not canonical");
            }
            return state;
        } catch (CodecException exception) {
            throw exception;
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw failure(Failure.INVALID_RECORD,
                    "supervisor state violates durable invariants", exception);
        }
    }

    static String canonicalRecordFingerprint(
            String schemaVersion,
            String supervisorInstanceId,
            long supervisorGeneration,
            String acceptedApplicationInstanceId,
            String acceptedApplicationOwnershipFingerprint,
            long acceptedApplicationGeneration,
            String previousApplicationInstanceId,
            String previousApplicationOwnershipFingerprint,
            long previousApplicationGeneration,
            EnterpriseLabInstalledAllocationSnapshot baselineAllocation,
            EnterpriseLabInstalledAllocationSnapshot installedAllocation,
            Optional<EnterpriseLabInstalledAllocationSnapshot> intendedAllocation,
            String transactionId,
            String lastRequestId,
            String lastRequestFingerprint,
            String previousCommittedFingerprint,
            TransactionPhase transactionPhase,
            Instant lastCommitAt,
            RecoveryClassification lastRecoveryClassification,
            TransitionReason transitionReason,
            long durableStateGeneration,
            String predecessorRecordFingerprint) {
        return sha256(write(canonicalContentNode(
                schemaVersion,
                supervisorInstanceId,
                supervisorGeneration,
                acceptedApplicationInstanceId,
                acceptedApplicationOwnershipFingerprint,
                acceptedApplicationGeneration,
                previousApplicationInstanceId,
                previousApplicationOwnershipFingerprint,
                previousApplicationGeneration,
                baselineAllocation,
                installedAllocation,
                intendedAllocation,
                transactionId,
                lastRequestId,
                lastRequestFingerprint,
                previousCommittedFingerprint,
                transactionPhase,
                lastCommitAt,
                lastRecoveryClassification,
                transitionReason,
                durableStateGeneration,
                predecessorRecordFingerprint)));
    }

    private EnterpriseLabInstalledAllocationSnapshot installed(JsonNode node, String field) {
        ObjectNode value = object(node, field);
        requireExactFields(value, INSTALLED_FIELDS, field);
        return installedCodec.decode(write(value));
    }

    private Optional<EnterpriseLabInstalledAllocationSnapshot> optionalInstalled(JsonNode node) {
        if (node == null) {
            throw failure(Failure.MALFORMED_RECORD, "intendedAllocation is required");
        }
        return node.isNull()
                ? Optional.empty()
                : Optional.of(installed(node, "intendedAllocation"));
    }

    private static ObjectNode canonicalContentNode(
            String schemaVersion,
            String supervisorInstanceId,
            long supervisorGeneration,
            String acceptedApplicationInstanceId,
            String acceptedApplicationOwnershipFingerprint,
            long acceptedApplicationGeneration,
            String previousApplicationInstanceId,
            String previousApplicationOwnershipFingerprint,
            long previousApplicationGeneration,
            EnterpriseLabInstalledAllocationSnapshot baselineAllocation,
            EnterpriseLabInstalledAllocationSnapshot installedAllocation,
            Optional<EnterpriseLabInstalledAllocationSnapshot> intendedAllocation,
            String transactionId,
            String lastRequestId,
            String lastRequestFingerprint,
            String previousCommittedFingerprint,
            TransactionPhase transactionPhase,
            Instant lastCommitAt,
            RecoveryClassification lastRecoveryClassification,
            TransitionReason transitionReason,
            long durableStateGeneration,
            String predecessorRecordFingerprint) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("schemaVersion", schemaVersion);
        root.put("supervisorInstanceId", supervisorInstanceId);
        root.put("supervisorGeneration", supervisorGeneration);
        root.put("acceptedApplicationInstanceId", acceptedApplicationInstanceId);
        root.put("acceptedApplicationOwnershipFingerprint",
                acceptedApplicationOwnershipFingerprint);
        root.put("acceptedApplicationGeneration", acceptedApplicationGeneration);
        root.put("previousApplicationInstanceId", previousApplicationInstanceId);
        root.put("previousApplicationOwnershipFingerprint",
                previousApplicationOwnershipFingerprint);
        root.put("previousApplicationGeneration", previousApplicationGeneration);
        root.set("baselineAllocation", installedNode(baselineAllocation));
        root.set("installedAllocation", installedNode(installedAllocation));
        intendedAllocation.ifPresentOrElse(
                value -> root.set("intendedAllocation", installedNode(value)),
                () -> root.putNull("intendedAllocation"));
        root.put("transactionId", transactionId);
        root.put("lastRequestId", lastRequestId);
        root.put("lastRequestFingerprint", lastRequestFingerprint);
        root.put("previousCommittedFingerprint", previousCommittedFingerprint);
        root.put("transactionPhase", transactionPhase.name());
        root.put("lastCommitAt", lastCommitAt.toString());
        root.put("lastRecoveryClassification", lastRecoveryClassification.name());
        ObjectNode reason = root.putObject("transitionReason");
        reason.put("code", transitionReason.code());
        reason.put("message", transitionReason.message());
        root.put("durableStateGeneration", durableStateGeneration);
        root.put("predecessorRecordFingerprint", predecessorRecordFingerprint);
        return root;
    }

    private static ObjectNode installedNode(EnterpriseLabInstalledAllocationSnapshot value) {
        EnterpriseLabInstalledAllocationSnapshot snapshot = Objects.requireNonNull(
                value, "installed allocation cannot be null");
        ObjectNode node = MAPPER.createObjectNode();
        node.put("schemaVersion", snapshot.schemaVersion());
        node.set("routingSnapshot",
                EnterpriseLabInstalledAllocationSnapshotCodec.canonicalRoutingNode(
                        snapshot.routingSnapshot()));
        node.put("routerGeneration", snapshot.routerGeneration());
        node.put("allocationFingerprint", snapshot.allocationFingerprint());
        node.set("eligibleBackendIds", identifiers(snapshot.eligibleBackendIds()));
        node.set("excludedBackendIds", identifiers(snapshot.excludedBackendIds()));
        node.put("installedAt", snapshot.installedAt().toString());
        node.put("installationReason", snapshot.installationReason());
        node.put("ownerGeneration", snapshot.ownerGeneration());
        return node;
    }

    private static ArrayNode identifiers(java.util.List<String> values) {
        ArrayNode node = MAPPER.createArrayNode();
        values.forEach(node::add);
        return node;
    }

    private static void requireExactFields(
            ObjectNode node, Set<String> expected, String subject) {
        Set<String> actual = new HashSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(expected)) {
            Set<String> unknown = new HashSet<>(actual);
            unknown.removeAll(expected);
            throw failure(
                    unknown.isEmpty() ? Failure.MALFORMED_RECORD : Failure.UNKNOWN_FIELD,
                    unknown.isEmpty()
                            ? subject + " is missing required fields"
                            : subject + " contains unknown fields");
        }
    }

    private static ObjectNode object(JsonNode value, String field) {
        if (value == null || !value.isObject()) {
            throw failure(Failure.MALFORMED_RECORD, field + " must be one JSON object");
        }
        return (ObjectNode) value;
    }

    private static String text(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw failure(Failure.MALFORMED_RECORD, field + " must be text");
        }
        return value.textValue();
    }

    private static long number(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw failure(Failure.MALFORMED_RECORD, field + " must be a bounded integer");
        }
        return value.longValue();
    }

    private static Instant instant(ObjectNode node, String field) {
        String encoded = text(node, field);
        Instant parsed = Instant.parse(encoded);
        if (!parsed.toString().equals(encoded)) {
            throw failure(Failure.NON_CANONICAL_RECORD,
                    field + " must use canonical UTC text");
        }
        return parsed;
    }

    private static <E extends Enum<E>> E enumValue(
            ObjectNode node, String field, Class<E> type) {
        try {
            return Enum.valueOf(type, text(node, field));
        } catch (IllegalArgumentException exception) {
            throw failure(Failure.INVALID_RECORD, field + " is unsupported", exception);
        }
    }

    private static byte[] requireInput(byte[] encoded) {
        byte[] safe = Arrays.copyOf(
                Objects.requireNonNull(encoded, "encoded cannot be null"), encoded.length);
        if (safe.length == 0 || safe.length > HARD_MAX_RECORD_BYTES) {
            throw failure(Failure.EXCEEDED_BOUNDS,
                    "supervisor state byte length is outside hard bounds");
        }
        return safe;
    }

    private static String strictUtf8(byte[] encoded) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded)).toString();
        } catch (CharacterCodingException exception) {
            throw failure(Failure.MALFORMED_RECORD,
                    "supervisor state is not strict UTF-8", exception);
        }
    }

    private static byte[] write(JsonNode node) {
        try {
            return MAPPER.writeValueAsBytes(node);
        } catch (IOException exception) {
            throw failure(Failure.MALFORMED_RECORD,
                    "supervisor state could not be encoded", exception);
        }
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static CodecException failure(Failure failure, String message) {
        return new CodecException(failure, message);
    }

    private static CodecException failure(
            Failure failure, String message, Throwable cause) {
        return new CodecException(failure, message, cause);
    }

    public enum Failure {
        EXCEEDED_BOUNDS,
        MALFORMED_RECORD,
        UNSUPPORTED_VERSION,
        UNKNOWN_FIELD,
        INVALID_RECORD,
        NON_CANONICAL_RECORD,
        FINGERPRINT_MISMATCH,
        TARGET_MISMATCH
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
