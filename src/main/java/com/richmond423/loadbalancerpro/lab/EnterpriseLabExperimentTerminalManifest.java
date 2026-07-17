package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Canonical, fingerprinted terminal evidence installed before a verified source
 * journal can be removed by bounded retention.
 */
public record EnterpriseLabExperimentTerminalManifest(
        String schemaVersion,
        String manifestId,
        String journalId,
        String experimentId,
        String scenarioId,
        String configurationFingerprint,
        String decisionFingerprint,
        String baselineAllocationFingerprint,
        String candidateAllocationFingerprint,
        String appliedAllocationFingerprint,
        EnterpriseLabExperimentState terminalState,
        EnterpriseLabExperimentJournalReplayPayload.RollbackStatus rollbackStatus,
        EnterpriseLabExperimentJournalReplayPayload.RestorationStatus restorationStatus,
        int sourceEventCount,
        long sourceByteCount,
        long sourceTerminalSequence,
        String sourceTerminalFingerprint,
        String reconstructedStateFingerprint,
        Instant terminalOccurredAt,
        Instant compactedAt,
        String reasonCode,
        String manifestFingerprint) {
    public static final String SCHEMA_VERSION = "enterprise-lab-terminal-manifest/v1";
    public static final int HARD_MAX_MANIFEST_BYTES = 65_536;
    private static final String EMPTY_CANDIDATE = "NONE";
    private static final Set<String> FIELDS = Set.of(
            "schemaVersion", "manifestId", "journalId", "experimentId", "scenarioId",
            "configurationFingerprint", "decisionFingerprint", "baselineAllocationFingerprint",
            "candidateAllocationFingerprint", "appliedAllocationFingerprint", "terminalState",
            "rollbackStatus", "restorationStatus", "sourceEventCount", "sourceByteCount",
            "sourceTerminalSequence", "sourceTerminalFingerprint", "reconstructedStateFingerprint",
            "terminalOccurredAt", "compactedAt", "reasonCode", "manifestFingerprint");
    private static final ObjectMapper MAPPER = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build());

    public EnterpriseLabExperimentTerminalManifest {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported terminal manifest schemaVersion");
        }
        manifestId = requireText(manifestId, "manifestId", 96);
        journalId = requireText(journalId, "journalId", 96);
        experimentId = requireText(experimentId, "experimentId", 128);
        scenarioId = requireText(scenarioId, "scenarioId", 128);
        configurationFingerprint = requireSha(configurationFingerprint, "configurationFingerprint");
        decisionFingerprint = requireSha(decisionFingerprint, "decisionFingerprint");
        baselineAllocationFingerprint = requireSha(
                baselineAllocationFingerprint, "baselineAllocationFingerprint");
        if (!EMPTY_CANDIDATE.equals(candidateAllocationFingerprint)) {
            candidateAllocationFingerprint = requireSha(
                    candidateAllocationFingerprint, "candidateAllocationFingerprint");
        }
        appliedAllocationFingerprint = requireSha(
                appliedAllocationFingerprint, "appliedAllocationFingerprint");
        terminalState = Objects.requireNonNull(terminalState, "terminalState cannot be null");
        rollbackStatus = Objects.requireNonNull(rollbackStatus, "rollbackStatus cannot be null");
        restorationStatus = Objects.requireNonNull(
                restorationStatus, "restorationStatus cannot be null");
        sourceTerminalFingerprint = requireSha(
                sourceTerminalFingerprint, "sourceTerminalFingerprint");
        reconstructedStateFingerprint = requireSha(
                reconstructedStateFingerprint, "reconstructedStateFingerprint");
        terminalOccurredAt = Objects.requireNonNull(terminalOccurredAt, "terminalOccurredAt cannot be null");
        compactedAt = Objects.requireNonNull(compactedAt, "compactedAt cannot be null");
        reasonCode = requireCode(reasonCode);
        manifestFingerprint = requireSha(manifestFingerprint, "manifestFingerprint");
        if (!terminalState.terminal() || sourceEventCount < 1
                || sourceEventCount > EnterpriseLabExperimentJournalDirectory.HARD_MAX_JOURNAL_ENTRIES
                || sourceByteCount < 1
                || sourceByteCount > EnterpriseLabExperimentJournalDirectory.HARD_MAX_JOURNAL_BYTES
                || sourceTerminalSequence != sourceEventCount
                || compactedAt.isBefore(terminalOccurredAt)) {
            throw new IllegalArgumentException("terminal manifest bounds or state are inconsistent");
        }
        String expected = fingerprint(contentBytes(
                schemaVersion, manifestId, journalId, experimentId, scenarioId,
                configurationFingerprint, decisionFingerprint, baselineAllocationFingerprint,
                candidateAllocationFingerprint, appliedAllocationFingerprint, terminalState,
                rollbackStatus, restorationStatus, sourceEventCount, sourceByteCount,
                sourceTerminalSequence, sourceTerminalFingerprint, reconstructedStateFingerprint,
                terminalOccurredAt, compactedAt, reasonCode));
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                manifestFingerprint.getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("manifestFingerprint does not match canonical content");
        }
    }

    static EnterpriseLabExperimentTerminalManifest create(
            EnterpriseLabExperimentJournalVerifier.VerificationResult verification,
            ReconstructedExperimentState state,
            Instant compactedAt,
            String reasonCode) {
        Objects.requireNonNull(verification, "verification cannot be null");
        ReconstructedExperimentState safeState = Objects.requireNonNull(state, "state cannot be null");
        var terminal = safeState.terminalRecord().orElseThrow(
                () -> new IllegalArgumentException("only terminal reconstructed state can be compacted"));
        String manifestId = "terminal-v1-" + safeState.journalId().substring("journal-v1-".length());
        String candidateFingerprint = safeState.candidateAllocation()
                .map(EnterpriseLabExperimentJournalReplayPayload::allocationFingerprint)
                .orElse(EMPTY_CANDIDATE);
        String baselineFingerprint = EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(
                safeState.baselineAllocation());
        String appliedFingerprint = EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(
                safeState.lastAppliedAllocation());
        byte[] content = contentBytes(
                SCHEMA_VERSION, manifestId, safeState.journalId(), safeState.experimentId(),
                safeState.scenarioId(), safeState.configuration().configurationFingerprint(),
                safeState.configuration().decisionFingerprint(), baselineFingerprint,
                candidateFingerprint, appliedFingerprint, safeState.lifecycle().state(),
                safeState.rollbackStatus(), safeState.restorationStatus(),
                verification.verifiedEvents().size(), verification.totalBytes(),
                safeState.latestSequence(), safeState.latestFingerprint(), safeState.contentFingerprint(),
                terminal.completedAt(), compactedAt, reasonCode);
        return new EnterpriseLabExperimentTerminalManifest(
                SCHEMA_VERSION, manifestId, safeState.journalId(), safeState.experimentId(),
                safeState.scenarioId(), safeState.configuration().configurationFingerprint(),
                safeState.configuration().decisionFingerprint(), baselineFingerprint,
                candidateFingerprint, appliedFingerprint, safeState.lifecycle().state(),
                safeState.rollbackStatus(), safeState.restorationStatus(),
                verification.verifiedEvents().size(), verification.totalBytes(),
                safeState.latestSequence(), safeState.latestFingerprint(), safeState.contentFingerprint(),
                terminal.completedAt(), compactedAt, reasonCode, fingerprint(content));
    }

    byte[] encode() {
        ObjectNode node = contentNode(this);
        node.put("manifestFingerprint", manifestFingerprint);
        byte[] encoded = write(node);
        if (encoded.length > HARD_MAX_MANIFEST_BYTES) {
            throw new IllegalArgumentException("terminal manifest exceeds its bounded size");
        }
        return encoded;
    }

    static EnterpriseLabExperimentTerminalManifest decode(byte[] encoded) {
        if (encoded == null || encoded.length == 0 || encoded.length > HARD_MAX_MANIFEST_BYTES) {
            throw new IllegalArgumentException("terminal manifest input is outside bounds");
        }
        try {
            JsonNode parsed = MAPPER.readTree(encoded);
            if (parsed == null || !parsed.isObject()) {
                throw new IllegalArgumentException("terminal manifest must be one JSON object");
            }
            ObjectNode node = (ObjectNode) parsed;
            Set<String> actual = new HashSet<>();
            node.fieldNames().forEachRemaining(actual::add);
            if (!actual.equals(FIELDS)) {
                throw new IllegalArgumentException("terminal manifest fields are not exactly supported");
            }
            EnterpriseLabExperimentTerminalManifest manifest = new EnterpriseLabExperimentTerminalManifest(
                    text(node, "schemaVersion"), text(node, "manifestId"), text(node, "journalId"),
                    text(node, "experimentId"), text(node, "scenarioId"),
                    text(node, "configurationFingerprint"), text(node, "decisionFingerprint"),
                    text(node, "baselineAllocationFingerprint"), text(node, "candidateAllocationFingerprint"),
                    text(node, "appliedAllocationFingerprint"),
                    EnterpriseLabExperimentState.valueOf(text(node, "terminalState")),
                    EnterpriseLabExperimentJournalReplayPayload.RollbackStatus.valueOf(
                            text(node, "rollbackStatus")),
                    EnterpriseLabExperimentJournalReplayPayload.RestorationStatus.valueOf(
                            text(node, "restorationStatus")),
                    integer(node, "sourceEventCount"), number(node, "sourceByteCount"),
                    number(node, "sourceTerminalSequence"), text(node, "sourceTerminalFingerprint"),
                    text(node, "reconstructedStateFingerprint"),
                    Instant.parse(text(node, "terminalOccurredAt")),
                    Instant.parse(text(node, "compactedAt")), text(node, "reasonCode"),
                    text(node, "manifestFingerprint"));
            if (!java.util.Arrays.equals(encoded, manifest.encode())) {
                throw new IllegalArgumentException("terminal manifest is not canonical JSON");
            }
            return manifest;
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof IllegalArgumentException illegal) {
                throw illegal;
            }
            throw new IllegalArgumentException("terminal manifest is not strict JSON", exception);
        }
    }

    private static byte[] contentBytes(
            String schemaVersion, String manifestId, String journalId, String experimentId,
            String scenarioId, String configurationFingerprint, String decisionFingerprint,
            String baselineAllocationFingerprint, String candidateAllocationFingerprint,
            String appliedAllocationFingerprint, EnterpriseLabExperimentState terminalState,
            EnterpriseLabExperimentJournalReplayPayload.RollbackStatus rollbackStatus,
            EnterpriseLabExperimentJournalReplayPayload.RestorationStatus restorationStatus,
            int sourceEventCount, long sourceByteCount, long sourceTerminalSequence,
            String sourceTerminalFingerprint, String reconstructedStateFingerprint,
            Instant terminalOccurredAt, Instant compactedAt, String reasonCode) {
        return write(contentNode(new Raw(
                schemaVersion, manifestId, journalId, experimentId, scenarioId,
                configurationFingerprint, decisionFingerprint, baselineAllocationFingerprint,
                candidateAllocationFingerprint, appliedAllocationFingerprint, terminalState,
                rollbackStatus, restorationStatus, sourceEventCount, sourceByteCount,
                sourceTerminalSequence, sourceTerminalFingerprint, reconstructedStateFingerprint,
                terminalOccurredAt, compactedAt, reasonCode)));
    }

    private static ObjectNode contentNode(EnterpriseLabExperimentTerminalManifest value) {
        return contentNode(new Raw(
                value.schemaVersion, value.manifestId, value.journalId, value.experimentId,
                value.scenarioId, value.configurationFingerprint, value.decisionFingerprint,
                value.baselineAllocationFingerprint, value.candidateAllocationFingerprint,
                value.appliedAllocationFingerprint, value.terminalState, value.rollbackStatus,
                value.restorationStatus, value.sourceEventCount, value.sourceByteCount,
                value.sourceTerminalSequence, value.sourceTerminalFingerprint,
                value.reconstructedStateFingerprint, value.terminalOccurredAt, value.compactedAt,
                value.reasonCode));
    }

    private static ObjectNode contentNode(Raw value) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("schemaVersion", value.schemaVersion);
        node.put("manifestId", value.manifestId);
        node.put("journalId", value.journalId);
        node.put("experimentId", value.experimentId);
        node.put("scenarioId", value.scenarioId);
        node.put("configurationFingerprint", value.configurationFingerprint);
        node.put("decisionFingerprint", value.decisionFingerprint);
        node.put("baselineAllocationFingerprint", value.baselineAllocationFingerprint);
        node.put("candidateAllocationFingerprint", value.candidateAllocationFingerprint);
        node.put("appliedAllocationFingerprint", value.appliedAllocationFingerprint);
        node.put("terminalState", value.terminalState.name());
        node.put("rollbackStatus", value.rollbackStatus.name());
        node.put("restorationStatus", value.restorationStatus.name());
        node.put("sourceEventCount", value.sourceEventCount);
        node.put("sourceByteCount", value.sourceByteCount);
        node.put("sourceTerminalSequence", value.sourceTerminalSequence);
        node.put("sourceTerminalFingerprint", value.sourceTerminalFingerprint);
        node.put("reconstructedStateFingerprint", value.reconstructedStateFingerprint);
        node.put("terminalOccurredAt", value.terminalOccurredAt.toString());
        node.put("compactedAt", value.compactedAt.toString());
        node.put("reasonCode", value.reasonCode);
        return node;
    }

    private static byte[] write(ObjectNode node) {
        try {
            return MAPPER.writeValueAsBytes(node);
        } catch (IOException exception) {
            throw new IllegalStateException("canonical terminal manifest could not be encoded", exception);
        }
    }

    private static String fingerprint(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String text(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException(field + " must be text");
        }
        return value.textValue();
    }

    private static long number(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw new IllegalArgumentException(field + " must be an integer");
        }
        return value.longValue();
    }

    private static int integer(ObjectNode node, String field) {
        long value = number(node, field);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(field + " is outside integer bounds");
        }
        return (int) value;
    }

    private static String requireText(String value, String field, int maximum) {
        if (value == null || value.isBlank() || !value.equals(value.trim()) || value.length() > maximum) {
            throw new IllegalArgumentException(field + " must be bounded canonical text");
        }
        return value;
    }

    private static String requireSha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
        return value;
    }

    private static String requireCode(String value) {
        if (value == null || !value.matches("[A-Z0-9][A-Z0-9_.:-]{0,63}")) {
            throw new IllegalArgumentException("reasonCode must be a bounded canonical code");
        }
        return value;
    }

    private record Raw(
            String schemaVersion, String manifestId, String journalId, String experimentId,
            String scenarioId, String configurationFingerprint, String decisionFingerprint,
            String baselineAllocationFingerprint, String candidateAllocationFingerprint,
            String appliedAllocationFingerprint, EnterpriseLabExperimentState terminalState,
            EnterpriseLabExperimentJournalReplayPayload.RollbackStatus rollbackStatus,
            EnterpriseLabExperimentJournalReplayPayload.RestorationStatus restorationStatus,
            int sourceEventCount, long sourceByteCount, long sourceTerminalSequence,
            String sourceTerminalFingerprint, String reconstructedStateFingerprint,
            Instant terminalOccurredAt, Instant compactedAt, String reasonCode) {
    }
}
