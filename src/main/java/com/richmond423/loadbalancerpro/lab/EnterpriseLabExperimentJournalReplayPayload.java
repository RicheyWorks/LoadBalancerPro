package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Strict data-only checkpoint carried inside a v1 journal event payload.
 * It contains only the bounded facts required for deterministic recovery replay.
 */
public final class EnterpriseLabExperimentJournalReplayPayload {
    public static final String SCHEMA_VERSION = "enterprise-lab-experiment-replay-checkpoint/v1";

    private static final Set<String> ROOT_FIELDS = Set.of(
            "replaySchemaVersion",
            "configuration",
            "baselineAllocation",
            "candidateAllocation",
            "appliedAllocation",
            "requestCount",
            "observationCount",
            "completedHoldDownCycles",
            "rollbackStatus",
            "restorationStatus");
    private static final Set<String> CONFIGURATION_FIELDS = Set.of(
            "configurationFingerprint",
            "decisionFingerprint",
            "maximumRequestCount",
            "maximumDurationMillis",
            "minimumEvidenceCount",
            "holdDownCycles",
            "rollbackPolicy",
            "operatingMode",
            "operatorAuthorized",
            "createdAt",
            "expiresAt");
    private static final Set<String> ALLOCATION_FIELDS = Set.of(
            "schemaVersion",
            "scenarioId",
            "revision",
            "sourceDecisionId",
            "kind",
            "allocations");
    private static final Set<String> ROLLBACK_POLICY_FIELDS = Set.of(
            "maximumFailureRate",
            "maximumTimeoutRate",
            "maximumLatencyRegressionRatio",
            "minimumHealthyBackends",
            "maximumConsecutiveTransportFailures",
            "maximumPartiallyDegradedBackends",
            "maximumObservationLossRate");

    private EnterpriseLabExperimentJournalReplayPayload() {
    }

    public static JsonNode encode(Checkpoint checkpoint) {
        Checkpoint safe = Objects.requireNonNull(checkpoint, "checkpoint cannot be null");
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("replaySchemaVersion", SCHEMA_VERSION);
        root.set("configuration", encodeConfiguration(safe.configuration()));
        root.set("baselineAllocation", encodeAllocation(safe.baselineAllocation()));
        if (safe.candidateAllocation().isPresent()) {
            root.set("candidateAllocation", encodeAllocation(safe.candidateAllocation().orElseThrow()));
        } else {
            root.putNull("candidateAllocation");
        }
        root.set("appliedAllocation", encodeAllocation(safe.appliedAllocation()));
        root.put("requestCount", safe.requestCount());
        root.put("observationCount", safe.observationCount());
        root.put("completedHoldDownCycles", safe.completedHoldDownCycles());
        root.put("rollbackStatus", safe.rollbackStatus().name());
        root.put("restorationStatus", safe.restorationStatus().name());
        return EnterpriseLabExperimentJournalCodec.canonicalPayloadCopy(root);
    }

    public static Checkpoint decode(JsonNode payload) {
        try {
            ObjectNode root = requireObject(payload, "replay checkpoint");
            requireExactFields(root, ROOT_FIELDS, "replay checkpoint");
            String version = requireText(root, "replaySchemaVersion");
            if (!SCHEMA_VERSION.equals(version)) {
                throw new PayloadException(Failure.UNSUPPORTED_VERSION,
                        "unsupported replay checkpoint schemaVersion");
            }
            ConfigurationEvidence configuration = decodeConfiguration(root.get("configuration"));
            EnterpriseLabLoopbackAllocationSnapshot baseline = decodeAllocation(
                    root.get("baselineAllocation"), "baselineAllocation");
            Optional<EnterpriseLabLoopbackAllocationSnapshot> candidate = optionalAllocation(
                    root.get("candidateAllocation"), "candidateAllocation");
            EnterpriseLabLoopbackAllocationSnapshot applied = decodeAllocation(
                    root.get("appliedAllocation"), "appliedAllocation");
            return new Checkpoint(
                    configuration,
                    baseline,
                    candidate,
                    applied,
                    requireInt(root, "requestCount"),
                    requireInt(root, "observationCount"),
                    requireInt(root, "completedHoldDownCycles"),
                    requireEnum(root, "rollbackStatus", RollbackStatus.class),
                    requireEnum(root, "restorationStatus", RestorationStatus.class));
        } catch (PayloadException exception) {
            throw exception;
        } catch (DateTimeParseException | IllegalArgumentException exception) {
            throw new PayloadException(Failure.MALFORMED_PAYLOAD,
                    "replay checkpoint contains invalid bounded data");
        }
    }

    /** Stable reference used by the journal envelope for an allocation snapshot. */
    public static String allocationFingerprint(EnterpriseLabLoopbackAllocationSnapshot snapshot) {
        EnterpriseLabLoopbackAllocationSnapshot safe = Objects.requireNonNull(
                snapshot, "snapshot cannot be null");
        MessageDigest digest = sha256();
        update(digest, safe.schemaVersion());
        update(digest, safe.scenarioId());
        update(digest, Long.toString(safe.revision()));
        update(digest, safe.sourceDecisionId());
        update(digest, safe.kind().name());
        safe.allocations().forEach((backendId, share) -> {
            update(digest, backendId);
            update(digest, Double.toString(share));
        });
        return HexFormat.of().formatHex(digest.digest());
    }

    private static ObjectNode encodeConfiguration(ConfigurationEvidence configuration) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("configurationFingerprint", configuration.configurationFingerprint());
        node.put("decisionFingerprint", configuration.decisionFingerprint());
        node.put("maximumRequestCount", configuration.maximumRequestCount());
        node.put("maximumDurationMillis", configuration.maximumDurationMillis());
        node.put("minimumEvidenceCount", configuration.minimumEvidenceCount());
        node.put("holdDownCycles", configuration.holdDownCycles());
        node.set("rollbackPolicy", encodeRollbackPolicy(configuration.rollbackPolicy()));
        node.put("operatingMode", configuration.operatingMode().name());
        node.put("operatorAuthorized", configuration.operatorAuthorized());
        node.put("createdAt", configuration.createdAt().toString());
        node.put("expiresAt", configuration.expiresAt().toString());
        return node;
    }

    private static ConfigurationEvidence decodeConfiguration(JsonNode value) {
        ObjectNode node = requireObject(value, "configuration");
        requireExactFields(node, CONFIGURATION_FIELDS, "configuration");
        return new ConfigurationEvidence(
                requireText(node, "configurationFingerprint"),
                requireText(node, "decisionFingerprint"),
                requireInt(node, "maximumRequestCount"),
                requireLong(node, "maximumDurationMillis"),
                requireInt(node, "minimumEvidenceCount"),
                requireInt(node, "holdDownCycles"),
                decodeRollbackPolicy(node.get("rollbackPolicy")),
                requireEnum(node, "operatingMode", AdaptiveRoutingPolicyMode.class),
                requireBoolean(node, "operatorAuthorized"),
                Instant.parse(requireText(node, "createdAt")),
                Instant.parse(requireText(node, "expiresAt")));
    }

    private static ObjectNode encodeRollbackPolicy(EnterpriseLabExperimentRollbackPolicy policy) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("maximumFailureRate", policy.maximumFailureRate());
        node.put("maximumTimeoutRate", policy.maximumTimeoutRate());
        node.put("maximumLatencyRegressionRatio", policy.maximumLatencyRegressionRatio());
        node.put("minimumHealthyBackends", policy.minimumHealthyBackends());
        node.put("maximumConsecutiveTransportFailures", policy.maximumConsecutiveTransportFailures());
        node.put("maximumPartiallyDegradedBackends", policy.maximumPartiallyDegradedBackends());
        node.put("maximumObservationLossRate", policy.maximumObservationLossRate());
        return node;
    }

    private static EnterpriseLabExperimentRollbackPolicy decodeRollbackPolicy(JsonNode value) {
        ObjectNode node = requireObject(value, "rollbackPolicy");
        requireExactFields(node, ROLLBACK_POLICY_FIELDS, "rollbackPolicy");
        return new EnterpriseLabExperimentRollbackPolicy(
                requireDouble(node, "maximumFailureRate"),
                requireDouble(node, "maximumTimeoutRate"),
                requireDouble(node, "maximumLatencyRegressionRatio"),
                requireInt(node, "minimumHealthyBackends"),
                requireInt(node, "maximumConsecutiveTransportFailures"),
                requireInt(node, "maximumPartiallyDegradedBackends"),
                requireDouble(node, "maximumObservationLossRate"));
    }

    private static ObjectNode encodeAllocation(EnterpriseLabLoopbackAllocationSnapshot snapshot) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("schemaVersion", snapshot.schemaVersion());
        node.put("scenarioId", snapshot.scenarioId());
        node.put("revision", snapshot.revision());
        node.put("sourceDecisionId", snapshot.sourceDecisionId());
        node.put("kind", snapshot.kind().name());
        ObjectNode allocations = node.putObject("allocations");
        snapshot.allocations().forEach(allocations::put);
        return node;
    }

    private static Optional<EnterpriseLabLoopbackAllocationSnapshot> optionalAllocation(
            JsonNode value,
            String fieldName) {
        if (value == null) {
            throw new PayloadException(Failure.MALFORMED_PAYLOAD, fieldName + " is required");
        }
        return value.isNull() ? Optional.empty() : Optional.of(decodeAllocation(value, fieldName));
    }

    private static EnterpriseLabLoopbackAllocationSnapshot decodeAllocation(
            JsonNode value,
            String fieldName) {
        ObjectNode node = requireObject(value, fieldName);
        requireExactFields(node, ALLOCATION_FIELDS, fieldName);
        ObjectNode allocationsNode = requireObject(node.get("allocations"), fieldName + ".allocations");
        if (allocationsNode.isEmpty()
                || allocationsNode.size() > EnterpriseLabLoopbackAllocationSnapshot.HARD_MAX_BACKENDS) {
            throw new PayloadException(Failure.MALFORMED_PAYLOAD,
                    fieldName + " allocation count is outside the bounded range");
        }
        Map<String, Double> allocations = new TreeMap<>();
        allocationsNode.fields().forEachRemaining(entry -> {
            JsonNode share = entry.getValue();
            if (!share.isNumber() || !Double.isFinite(share.doubleValue())) {
                throw new PayloadException(Failure.MALFORMED_PAYLOAD,
                        fieldName + " contains a non-numeric allocation");
            }
            allocations.put(entry.getKey(), share.doubleValue());
        });
        return new EnterpriseLabLoopbackAllocationSnapshot(
                requireText(node, "schemaVersion"),
                requireText(node, "scenarioId"),
                requireLong(node, "revision"),
                requireText(node, "sourceDecisionId"),
                requireEnum(node, "kind", Kind.class),
                new LinkedHashMap<>(allocations));
    }

    private static ObjectNode requireObject(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) {
            throw new PayloadException(Failure.MALFORMED_PAYLOAD, fieldName + " must be an object");
        }
        return (ObjectNode) node;
    }

    private static void requireExactFields(ObjectNode node, Set<String> expected, String fieldName) {
        Set<String> actual = new java.util.HashSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(expected)) {
            throw new PayloadException(Failure.MALFORMED_PAYLOAD,
                    fieldName + " must contain exactly the supported fields");
        }
    }

    private static String requireText(ObjectNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isTextual() || value.textValue().isBlank()
                || !value.textValue().equals(value.textValue().trim())) {
            throw new PayloadException(Failure.MALFORMED_PAYLOAD,
                    fieldName + " must be bounded canonical text");
        }
        return value.textValue();
    }

    private static long requireLong(ObjectNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw new PayloadException(Failure.MALFORMED_PAYLOAD, fieldName + " must be an integer");
        }
        return value.longValue();
    }

    private static int requireInt(ObjectNode node, String fieldName) {
        long value = requireLong(node, fieldName);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new PayloadException(Failure.MALFORMED_PAYLOAD, fieldName + " is outside integer bounds");
        }
        return (int) value;
    }

    private static boolean requireBoolean(ObjectNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isBoolean()) {
            throw new PayloadException(Failure.MALFORMED_PAYLOAD, fieldName + " must be boolean");
        }
        return value.booleanValue();
    }

    private static double requireDouble(ObjectNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isNumber() || !Double.isFinite(value.doubleValue())) {
            throw new PayloadException(Failure.MALFORMED_PAYLOAD, fieldName + " must be a finite number");
        }
        return value.doubleValue();
    }

    private static <E extends Enum<E>> E requireEnum(
            ObjectNode node,
            String fieldName,
            Class<E> enumType) {
        String value = requireText(node, fieldName);
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new PayloadException(Failure.MALFORMED_PAYLOAD,
                    fieldName + " contains an unsupported value");
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    public record ConfigurationEvidence(
            String configurationFingerprint,
            String decisionFingerprint,
            int maximumRequestCount,
            long maximumDurationMillis,
            int minimumEvidenceCount,
            int holdDownCycles,
            EnterpriseLabExperimentRollbackPolicy rollbackPolicy,
            AdaptiveRoutingPolicyMode operatingMode,
            boolean operatorAuthorized,
            Instant createdAt,
            Instant expiresAt) {
        public ConfigurationEvidence {
            configurationFingerprint = requireSha256(
                    configurationFingerprint, "configurationFingerprint");
            decisionFingerprint = requireSha256(decisionFingerprint, "decisionFingerprint");
            if (maximumRequestCount < 1
                    || maximumRequestCount > EnterpriseLabExperimentConfiguration.HARD_MAX_REQUEST_COUNT) {
                throw new IllegalArgumentException("maximumRequestCount is outside Enterprise Lab bounds");
            }
            if (maximumDurationMillis < 1
                    || maximumDurationMillis > EnterpriseLabExperimentConfiguration.HARD_MAX_DURATION.toMillis()) {
                throw new IllegalArgumentException("maximumDurationMillis is outside Enterprise Lab bounds");
            }
            if (minimumEvidenceCount < 1 || minimumEvidenceCount > maximumRequestCount) {
                throw new IllegalArgumentException("minimumEvidenceCount is inconsistent");
            }
            if (holdDownCycles < 1
                    || holdDownCycles > EnterpriseLabExperimentConfiguration.HARD_MAX_HOLD_DOWN_CYCLES) {
                throw new IllegalArgumentException("holdDownCycles is outside Enterprise Lab bounds");
            }
            rollbackPolicy = Objects.requireNonNull(rollbackPolicy, "rollbackPolicy cannot be null");
            operatingMode = Objects.requireNonNull(operatingMode, "operatingMode cannot be null");
            createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
            expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
            Duration expiration = Duration.between(createdAt, expiresAt);
            if (expiration.isZero() || expiration.isNegative()
                    || expiration.compareTo(Duration.ofMinutes(15)) > 0
                    || expiration.toMillis() < maximumDurationMillis) {
                throw new IllegalArgumentException("configuration time bounds are inconsistent");
            }
        }
    }

    public record Checkpoint(
            ConfigurationEvidence configuration,
            EnterpriseLabLoopbackAllocationSnapshot baselineAllocation,
            Optional<EnterpriseLabLoopbackAllocationSnapshot> candidateAllocation,
            EnterpriseLabLoopbackAllocationSnapshot appliedAllocation,
            int requestCount,
            int observationCount,
            int completedHoldDownCycles,
            RollbackStatus rollbackStatus,
            RestorationStatus restorationStatus) {
        public Checkpoint {
            configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
            baselineAllocation = Objects.requireNonNull(
                    baselineAllocation, "baselineAllocation cannot be null");
            candidateAllocation = Objects.requireNonNull(
                    candidateAllocation, "candidateAllocation cannot be null");
            appliedAllocation = Objects.requireNonNull(
                    appliedAllocation, "appliedAllocation cannot be null");
            rollbackStatus = Objects.requireNonNull(rollbackStatus, "rollbackStatus cannot be null");
            restorationStatus = Objects.requireNonNull(
                    restorationStatus, "restorationStatus cannot be null");
            if (baselineAllocation.kind() != Kind.BASELINE) {
                throw new IllegalArgumentException("baselineAllocation must have BASELINE kind");
            }
            if (candidateAllocation.filter(candidate -> candidate.kind() != Kind.CANDIDATE).isPresent()) {
                throw new IllegalArgumentException("candidateAllocation must have CANDIDATE kind");
            }
            if (!baselineAllocation.scenarioId().equals(appliedAllocation.scenarioId())
                    || candidateAllocation.isPresent()
                    && !baselineAllocation.scenarioId().equals(
                            candidateAllocation.orElseThrow().scenarioId())) {
                throw new IllegalArgumentException("checkpoint allocations must describe one scenario");
            }
            if (appliedAllocation.kind() == Kind.CANDIDATE) {
                EnterpriseLabLoopbackAllocationSnapshot candidate = candidateAllocation.orElseThrow(() ->
                        new IllegalArgumentException("candidate applied allocation requires candidate evidence"));
                if (!candidate.equals(appliedAllocation)) {
                    throw new IllegalArgumentException("applied candidate must match candidateAllocation");
                }
            } else if (!EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                    baselineAllocation.allocations(), appliedAllocation.allocations())) {
                throw new IllegalArgumentException("baseline-kind applied allocation must match baseline shares");
            }
            if (requestCount < 0 || requestCount > configuration.maximumRequestCount()
                    || observationCount < 0 || observationCount > requestCount
                    || completedHoldDownCycles < 0
                    || completedHoldDownCycles > configuration.holdDownCycles()) {
                throw new IllegalArgumentException("checkpoint counters are inconsistent");
            }
            if (restorationStatus == RestorationStatus.SUCCEEDED
                    && appliedAllocation.kind() == Kind.CANDIDATE) {
                throw new IllegalArgumentException("successful restoration cannot retain candidate allocation");
            }
            if (rollbackStatus == RollbackStatus.COMPLETED
                    && restorationStatus != RestorationStatus.SUCCEEDED) {
                throw new IllegalArgumentException("completed rollback requires successful restoration");
            }
        }
    }

    public enum RollbackStatus {
        NOT_REQUESTED,
        REQUESTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    public enum RestorationStatus {
        NOT_ATTEMPTED,
        ATTEMPTED,
        SUCCEEDED,
        FAILED
    }

    public enum Failure {
        UNSUPPORTED_VERSION,
        MALFORMED_PAYLOAD
    }

    public static final class PayloadException extends RuntimeException {
        private final Failure failure;

        private PayloadException(Failure failure, String message) {
            super(message, null, false, false);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        public Failure failure() {
            return failure;
        }
    }

    private static String requireSha256(String value, String fieldName) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(fieldName + " must be lowercase SHA-256");
        }
        return value;
    }
}
