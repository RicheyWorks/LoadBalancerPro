package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Immutable, bounded evidence envelope for one durable experiment journal entry.
 * Fingerprints detect content changes; they do not authenticate an author.
 */
public final class EnterpriseLabExperimentJournalEvent {
    public static final String SCHEMA_VERSION = "enterprise-lab-experiment-journal-event/v1";
    public static final String PAYLOAD_SCHEMA_VERSION = "enterprise-lab-experiment-journal-payload/v1";
    public static final String GENESIS_FINGERPRINT = "GENESIS";
    public static final String NO_FINGERPRINT = "NONE";
    public static final long HARD_MAX_SEQUENCE = 1_000_000;
    public static final long HARD_MAX_LOGICAL_CYCLE = 1_000_000;
    public static final int HARD_MAX_METADATA_ENTRIES = 16;

    private static final int MAX_ID_LENGTH = 128;
    private static final int MAX_REASON_CODE_LENGTH = 64;
    private static final int MAX_REASON_MESSAGE_LENGTH = 512;
    private static final int MAX_METADATA_KEY_LENGTH = 64;
    private static final int MAX_METADATA_VALUE_LENGTH = 256;
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern CANONICAL_ID = Pattern.compile("[A-Za-z0-9._:-]+");
    private static final Pattern REASON_CODE = Pattern.compile("[A-Z0-9][A-Z0-9_.:-]*");
    private static final Pattern CREDENTIAL_VALUE = Pattern.compile(
            "(?i)(?:\\bbearer\\s+[A-Za-z0-9._~+/-]{12,}={0,2}"
                    + "|-----BEGIN [^-\\r\\n]{1,40}PRIVATE KEY-----"
                    + "|\\b(?:api[_-]?key|password|access[_-]?token|refresh[_-]?token|client[_-]?secret)"
                    + "\\s*[:=]\\s*\\S+|\\bcookie\\s*:\\s*\\S+)");
    private static final Pattern STACK_TRACE = Pattern.compile(
            "(?m)(?:^|\\R)\\s*at\\s+[A-Za-z0-9_.$]+\\([^\\r\\n]*\\.java:\\d+\\)");
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "authorization", "password", "passwd", "apikey", "accesstoken", "refreshtoken",
            "cookie", "setcookie", "privatekey", "clientsecret", "credential", "credentials",
            "stacktrace", "throwable");

    private final String schemaVersion;
    private final long sequence;
    private final String experimentId;
    private final String scenarioId;
    private final EnterpriseLabExperimentJournalEventType eventType;
    private final EnterpriseLabExperimentState stateBefore;
    private final EnterpriseLabExperimentState stateAfter;
    private final long logicalCycle;
    private final Instant occurredAt;
    private final String configurationFingerprint;
    private final String decisionFingerprint;
    private final String baselineAllocationFingerprint;
    private final String candidateAllocationFingerprint;
    private final String appliedAllocationFingerprint;
    private final Reason reason;
    private final String previousEntryFingerprint;
    private final Map<String, String> metadata;
    private final String payloadSchemaVersion;
    private final JsonNode payload;
    private final String currentEntryFingerprint;

    private EnterpriseLabExperimentJournalEvent(
            String schemaVersion,
            long sequence,
            String experimentId,
            String scenarioId,
            EnterpriseLabExperimentJournalEventType eventType,
            EnterpriseLabExperimentState stateBefore,
            EnterpriseLabExperimentState stateAfter,
            long logicalCycle,
            Instant occurredAt,
            String configurationFingerprint,
            String decisionFingerprint,
            String baselineAllocationFingerprint,
            String candidateAllocationFingerprint,
            String appliedAllocationFingerprint,
            Reason reason,
            String previousEntryFingerprint,
            Map<String, String> metadata,
            String payloadSchemaVersion,
            JsonNode payload) {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported journal event schemaVersion");
        }
        if (sequence < 1 || sequence > HARD_MAX_SEQUENCE) {
            throw new IllegalArgumentException("sequence must be between 1 and 1000000");
        }
        this.schemaVersion = schemaVersion;
        this.sequence = sequence;
        this.experimentId = requireCanonicalId(experimentId, "experimentId");
        this.scenarioId = requireCanonicalId(scenarioId, "scenarioId");
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.stateBefore = Objects.requireNonNull(stateBefore, "stateBefore cannot be null");
        this.stateAfter = Objects.requireNonNull(stateAfter, "stateAfter cannot be null");
        if (logicalCycle < 0 || logicalCycle > HARD_MAX_LOGICAL_CYCLE) {
            throw new IllegalArgumentException("logicalCycle must be between 0 and 1000000");
        }
        this.logicalCycle = logicalCycle;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        this.configurationFingerprint = requireOptionalFingerprint(
                configurationFingerprint, "configurationFingerprint");
        this.decisionFingerprint = requireOptionalFingerprint(decisionFingerprint, "decisionFingerprint");
        this.baselineAllocationFingerprint = requireOptionalFingerprint(
                baselineAllocationFingerprint, "baselineAllocationFingerprint");
        this.candidateAllocationFingerprint = requireOptionalFingerprint(
                candidateAllocationFingerprint, "candidateAllocationFingerprint");
        this.appliedAllocationFingerprint = requireOptionalFingerprint(
                appliedAllocationFingerprint, "appliedAllocationFingerprint");
        this.reason = Objects.requireNonNull(reason, "reason cannot be null");
        this.previousEntryFingerprint = requirePreviousFingerprint(previousEntryFingerprint, sequence);
        this.metadata = immutableMetadata(metadata);
        if (!PAYLOAD_SCHEMA_VERSION.equals(payloadSchemaVersion)) {
            throw new IllegalArgumentException("unsupported journal payload schemaVersion");
        }
        this.payloadSchemaVersion = payloadSchemaVersion;
        this.payload = EnterpriseLabExperimentJournalCodec.canonicalPayloadCopy(payload);
        validatePayloadSafety(this.payload);
        this.currentEntryFingerprint = EnterpriseLabExperimentJournalCodec.canonicalFingerprint(this);
    }

    /** Creates an event using only the supplied clock for its timestamp. */
    public static EnterpriseLabExperimentJournalEvent create(Clock clock, Draft draft) {
        Clock safeClock = Objects.requireNonNull(clock, "clock cannot be null");
        Draft safeDraft = Objects.requireNonNull(draft, "draft cannot be null");
        return reconstitute(
                SCHEMA_VERSION,
                safeDraft.sequence(),
                safeDraft.experimentId(),
                safeDraft.scenarioId(),
                safeDraft.eventType(),
                safeDraft.stateBefore(),
                safeDraft.stateAfter(),
                safeDraft.logicalCycle(),
                safeClock.instant(),
                safeDraft.configurationFingerprint(),
                safeDraft.decisionFingerprint(),
                safeDraft.baselineAllocationFingerprint(),
                safeDraft.candidateAllocationFingerprint(),
                safeDraft.appliedAllocationFingerprint(),
                safeDraft.reason(),
                safeDraft.previousEntryFingerprint(),
                safeDraft.metadata(),
                PAYLOAD_SCHEMA_VERSION,
                safeDraft.payload());
    }

    static EnterpriseLabExperimentJournalEvent reconstitute(
            String schemaVersion,
            long sequence,
            String experimentId,
            String scenarioId,
            EnterpriseLabExperimentJournalEventType eventType,
            EnterpriseLabExperimentState stateBefore,
            EnterpriseLabExperimentState stateAfter,
            long logicalCycle,
            Instant occurredAt,
            String configurationFingerprint,
            String decisionFingerprint,
            String baselineAllocationFingerprint,
            String candidateAllocationFingerprint,
            String appliedAllocationFingerprint,
            Reason reason,
            String previousEntryFingerprint,
            Map<String, String> metadata,
            String payloadSchemaVersion,
            JsonNode payload) {
        return new EnterpriseLabExperimentJournalEvent(
                schemaVersion, sequence, experimentId, scenarioId, eventType, stateBefore, stateAfter,
                logicalCycle, occurredAt, configurationFingerprint, decisionFingerprint,
                baselineAllocationFingerprint, candidateAllocationFingerprint, appliedAllocationFingerprint,
                reason, previousEntryFingerprint, metadata, payloadSchemaVersion, payload);
    }

    public String schemaVersion() {
        return schemaVersion;
    }

    public long sequence() {
        return sequence;
    }

    public String experimentId() {
        return experimentId;
    }

    public String scenarioId() {
        return scenarioId;
    }

    public EnterpriseLabExperimentJournalEventType eventType() {
        return eventType;
    }

    public EnterpriseLabExperimentState stateBefore() {
        return stateBefore;
    }

    public EnterpriseLabExperimentState stateAfter() {
        return stateAfter;
    }

    public long logicalCycle() {
        return logicalCycle;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public String configurationFingerprint() {
        return configurationFingerprint;
    }

    public String decisionFingerprint() {
        return decisionFingerprint;
    }

    public String baselineAllocationFingerprint() {
        return baselineAllocationFingerprint;
    }

    public String candidateAllocationFingerprint() {
        return candidateAllocationFingerprint;
    }

    public String appliedAllocationFingerprint() {
        return appliedAllocationFingerprint;
    }

    public Reason reason() {
        return reason;
    }

    public String previousEntryFingerprint() {
        return previousEntryFingerprint;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public String payloadSchemaVersion() {
        return payloadSchemaVersion;
    }

    public JsonNode payload() {
        return payload.deepCopy();
    }

    JsonNode payloadInternal() {
        return payload.deepCopy();
    }

    public String currentEntryFingerprint() {
        return currentEntryFingerprint;
    }

    private static String requireCanonicalId(String value, String fieldName) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > MAX_ID_LENGTH || !CANONICAL_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a bounded canonical identifier");
        }
        return value;
    }

    private static String requireOptionalFingerprint(String value, String fieldName) {
        if (NO_FINGERPRINT.equals(value) || value != null && SHA_256.matcher(value).matches()) {
            return value;
        }
        throw new IllegalArgumentException(fieldName + " must be NONE or lowercase SHA-256");
    }

    private static String requirePreviousFingerprint(String value, long sequence) {
        if (sequence == 1 && GENESIS_FINGERPRINT.equals(value)) {
            return value;
        }
        if (sequence > 1 && value != null && SHA_256.matcher(value).matches()) {
            return value;
        }
        throw new IllegalArgumentException(
                "previousEntryFingerprint must be GENESIS only for sequence 1 and lowercase SHA-256 otherwise");
    }

    private static Map<String, String> immutableMetadata(Map<String, String> values) {
        Objects.requireNonNull(values, "metadata cannot be null");
        if (values.size() > HARD_MAX_METADATA_ENTRIES) {
            throw new IllegalArgumentException("metadata cannot exceed 16 entries");
        }
        Map<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = requireBoundedPlainText(entry.getKey(), "metadata key", MAX_METADATA_KEY_LENGTH);
            String value = requireBoundedPlainText(entry.getValue(), "metadata value", MAX_METADATA_VALUE_LENGTH);
            rejectSensitiveKey(key);
            rejectUnsafeEvidenceText(value);
            if (sorted.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("metadata keys must be unique");
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    private static String requireBoundedPlainText(String value, String fieldName, int maximumLength) {
        if (value == null || value.isBlank() || !value.equals(value.trim()) || value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " must be non-blank, trimmed, and bounded");
        }
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                throw new IllegalArgumentException(fieldName + " cannot contain control characters");
            }
        }
        return value;
    }

    private static void validatePayloadSafety(JsonNode node) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                rejectSensitiveKey(entry.getKey());
                validatePayloadSafety(entry.getValue());
            });
        } else if (node.isArray()) {
            node.forEach(EnterpriseLabExperimentJournalEvent::validatePayloadSafety);
        } else if (node.isTextual()) {
            rejectUnsafeEvidenceText(node.textValue());
        }
    }

    private static void rejectSensitiveKey(String key) {
        String normalized = key.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (SENSITIVE_KEYS.contains(normalized)) {
            throw new EnterpriseLabExperimentJournalCodec.CodecException(
                    EnterpriseLabExperimentJournalCodec.Failure.SENSITIVE_CONTENT,
                    "journal evidence cannot contain credential or stack-trace fields");
        }
    }

    private static void rejectUnsafeEvidenceText(String value) {
        if (CREDENTIAL_VALUE.matcher(value).find() || STACK_TRACE.matcher(value).find()) {
            throw new EnterpriseLabExperimentJournalCodec.CodecException(
                    EnterpriseLabExperimentJournalCodec.Failure.SENSITIVE_CONTENT,
                    "journal evidence cannot contain credentials or stack traces");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EnterpriseLabExperimentJournalEvent that)) {
            return false;
        }
        return sequence == that.sequence
                && logicalCycle == that.logicalCycle
                && schemaVersion.equals(that.schemaVersion)
                && experimentId.equals(that.experimentId)
                && scenarioId.equals(that.scenarioId)
                && eventType == that.eventType
                && stateBefore == that.stateBefore
                && stateAfter == that.stateAfter
                && occurredAt.equals(that.occurredAt)
                && configurationFingerprint.equals(that.configurationFingerprint)
                && decisionFingerprint.equals(that.decisionFingerprint)
                && baselineAllocationFingerprint.equals(that.baselineAllocationFingerprint)
                && candidateAllocationFingerprint.equals(that.candidateAllocationFingerprint)
                && appliedAllocationFingerprint.equals(that.appliedAllocationFingerprint)
                && reason.equals(that.reason)
                && previousEntryFingerprint.equals(that.previousEntryFingerprint)
                && metadata.equals(that.metadata)
                && payloadSchemaVersion.equals(that.payloadSchemaVersion)
                && payload.equals(that.payload)
                && currentEntryFingerprint.equals(that.currentEntryFingerprint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                schemaVersion, sequence, experimentId, scenarioId, eventType, stateBefore, stateAfter,
                logicalCycle, occurredAt, configurationFingerprint, decisionFingerprint,
                baselineAllocationFingerprint, candidateAllocationFingerprint, appliedAllocationFingerprint,
                reason, previousEntryFingerprint, metadata, payloadSchemaVersion, payload,
                currentEntryFingerprint);
    }

    @Override
    public String toString() {
        return "EnterpriseLabExperimentJournalEvent[sequence=" + sequence
                + ", experimentId=" + experimentId
                + ", eventType=" + eventType
                + ", currentEntryFingerprint=" + currentEntryFingerprint + "]";
    }

    public record Reason(String code, String message) {
        public Reason {
            code = requireBoundedPlainText(code, "reason code", MAX_REASON_CODE_LENGTH);
            if (!REASON_CODE.matcher(code).matches()) {
                throw new IllegalArgumentException("reason code must use canonical uppercase characters");
            }
            message = requireBoundedPlainText(message, "reason message", MAX_REASON_MESSAGE_LENGTH);
            rejectUnsafeEvidenceText(message);
        }
    }

    /** Input to {@link #create(Clock, Draft)}; normalization occurs when the event is created. */
    public record Draft(
            long sequence,
            String experimentId,
            String scenarioId,
            EnterpriseLabExperimentJournalEventType eventType,
            EnterpriseLabExperimentState stateBefore,
            EnterpriseLabExperimentState stateAfter,
            long logicalCycle,
            String configurationFingerprint,
            String decisionFingerprint,
            String baselineAllocationFingerprint,
            String candidateAllocationFingerprint,
            String appliedAllocationFingerprint,
            Reason reason,
            String previousEntryFingerprint,
            Map<String, String> metadata,
            JsonNode payload) {
    }
}
