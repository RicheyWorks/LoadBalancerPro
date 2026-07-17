package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalCodec.CodecException;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalCodec.Failure;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalEvent.Draft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalEvent.Reason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabExperimentJournalCodecTest {
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-16T23:00:00.123456Z");
    private static final Clock CLOCK = Clock.fixed(OCCURRED_AT, ZoneOffset.UTC);
    private static final String CONFIGURATION_FINGERPRINT = "a".repeat(64);
    private static final String DECISION_FINGERPRINT = "b".repeat(64);
    private static final String BASELINE_FINGERPRINT = "c".repeat(64);
    private static final String CANDIDATE_FINGERPRINT = "d".repeat(64);
    private static final String APPLIED_FINGERPRINT = "e".repeat(64);
    private static final EnterpriseLabExperimentJournalCodec CODEC =
            new EnterpriseLabExperimentJournalCodec();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void canonicalEncodingAndFingerprintIgnoreMapObjectAndCollectionOrder() {
        Map<String, String> firstMetadata = new LinkedHashMap<>();
        firstMetadata.put("zeta", "last");
        firstMetadata.put("alpha", "first");
        ObjectNode firstPayload = JsonNodeFactory.instance.objectNode();
        firstPayload.put("zeta", "last");
        firstPayload.put("number", new BigDecimal("1.00"));
        firstPayload.putArray("backends").add("backend-z").add("backend-a");
        firstPayload.putObject("allocation").put("zeta", 0.25).put("alpha", 0.75);

        Map<String, String> secondMetadata = new LinkedHashMap<>();
        secondMetadata.put("alpha", "first");
        secondMetadata.put("zeta", "last");
        ObjectNode secondPayload = JsonNodeFactory.instance.objectNode();
        secondPayload.putObject("allocation").put("alpha", 0.7500).put("zeta", 0.25000);
        secondPayload.putArray("backends").add("backend-a").add("backend-z");
        secondPayload.put("number", 1);
        secondPayload.put("zeta", "last");

        EnterpriseLabExperimentJournalEvent first = event(1, "GENESIS", firstMetadata, firstPayload);
        EnterpriseLabExperimentJournalEvent second = event(1, "GENESIS", secondMetadata, secondPayload);

        assertEquals(first.currentEntryFingerprint(), second.currentEntryFingerprint());
        assertArrayEquals(CODEC.canonicalContentBytes(first), CODEC.canonicalContentBytes(second));
        assertArrayEquals(CODEC.encode(first), CODEC.encode(second));
    }

    @Test
    void fingerprintIsSha256OfCanonicalContentAndRoundTripIsStable() throws Exception {
        EnterpriseLabExperimentJournalEvent event = event(1, "GENESIS", Map.of("source", "operator"), payload());

        String expected = java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(CODEC.canonicalContentBytes(event)));
        byte[] encoded = CODEC.encode(event);
        EnterpriseLabExperimentJournalEvent decoded = CODEC.decode(encoded);

        assertEquals(expected, event.currentEntryFingerprint());
        assertEquals(event, decoded);
        assertArrayEquals(encoded, CODEC.encode(decoded));
        assertFalse(new String(encoded, StandardCharsets.UTF_8).endsWith("\n"));
    }

    @Test
    void injectedClockIsTheOnlyEventTimestampSource() {
        EnterpriseLabExperimentJournalEvent event = event(1, "GENESIS", Map.of(), payload());

        assertEquals(OCCURRED_AT, event.occurredAt());
        assertTrue(new String(CODEC.encode(event), StandardCharsets.UTF_8)
                .contains("\"occurredAt\":\"2026-07-16T23:00:00.123456Z\""));
    }

    @Test
    void decoderAcceptsEquivalentEnvelopeFieldOrderButReencodesCanonically() throws Exception {
        EnterpriseLabExperimentJournalEvent event = event(1, "GENESIS", Map.of(), payload());
        ObjectNode original = (ObjectNode) OBJECT_MAPPER.readTree(CODEC.encode(event));
        List<Map.Entry<String, JsonNode>> fields = new ArrayList<>();
        original.fields().forEachRemaining(fields::add);
        Collections.reverse(fields);
        ObjectNode reversed = JsonNodeFactory.instance.objectNode();
        fields.forEach(entry -> reversed.set(entry.getKey(), entry.getValue()));
        byte[] nonCanonical = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(reversed);

        EnterpriseLabExperimentJournalEvent decoded = CODEC.decode(nonCanonical);

        assertEquals(event, decoded);
        assertArrayEquals(CODEC.encode(event), CODEC.encode(decoded));
        assertFalse(java.util.Arrays.equals(nonCanonical, CODEC.encode(decoded)));
    }

    @Test
    void predecessorMustBeGenesisOnlyForFirstEntryAndFingerprintAfterward() {
        assertThrows(IllegalArgumentException.class,
                () -> event(1, "f".repeat(64), Map.of(), payload()));
        assertThrows(IllegalArgumentException.class,
                () -> event(2, "GENESIS", Map.of(), payload()));

        EnterpriseLabExperimentJournalEvent first = event(1, "GENESIS", Map.of(), payload());
        EnterpriseLabExperimentJournalEvent second = event(
                2, first.currentEntryFingerprint(), Map.of(), payload());

        assertEquals(first.currentEntryFingerprint(), second.previousEntryFingerprint());
        assertFalse(first.currentEntryFingerprint().equals(second.currentEntryFingerprint()));
    }

    @Test
    void payloadAccessorCannotMutateCanonicalEvidence() {
        EnterpriseLabExperimentJournalEvent event = event(1, "GENESIS", Map.of(), payload());
        byte[] before = CODEC.encode(event);

        ((ObjectNode) event.payload()).put("mutated", true);
        ((ObjectNode) event.payloadInternal()).put("packageMutated", true);

        assertArrayEquals(before, CODEC.encode(event));
        assertFalse(event.payload().has("mutated"));
        assertFalse(event.payload().has("packageMutated"));
    }

    @Test
    void eventVocabularyCoversEveryRequiredSafetyRelevantFact() {
        assertEquals(EnumSet.of(
                        EnterpriseLabExperimentJournalEventType.EXPERIMENT_ARMED,
                        EnterpriseLabExperimentJournalEventType.EXPERIMENT_STARTED,
                        EnterpriseLabExperimentJournalEventType.CANDIDATE_ALLOCATION_APPLIED,
                        EnterpriseLabExperimentJournalEventType.LIFECYCLE_TRANSITION,
                        EnterpriseLabExperimentJournalEventType.OBSERVATION_CHECKPOINT,
                        EnterpriseLabExperimentJournalEventType.HOLD_EVALUATED,
                        EnterpriseLabExperimentJournalEventType.ROLLBACK_REQUESTED,
                        EnterpriseLabExperimentJournalEventType.BASELINE_RESTORATION_ATTEMPTED,
                        EnterpriseLabExperimentJournalEventType.BASELINE_RESTORED,
                        EnterpriseLabExperimentJournalEventType.EXPERIMENT_CANCELLED,
                        EnterpriseLabExperimentJournalEventType.EXPERIMENT_COMPLETED,
                        EnterpriseLabExperimentJournalEventType.EXPERIMENT_ROLLED_BACK,
                        EnterpriseLabExperimentJournalEventType.EXPERIMENT_REJECTED,
                        EnterpriseLabExperimentJournalEventType.EXPERIMENT_FAILED,
                        EnterpriseLabExperimentJournalEventType.RECOVERY_ACTION,
                        EnterpriseLabExperimentJournalEventType.QUARANTINE_FINDING),
                EnumSet.allOf(EnterpriseLabExperimentJournalEventType.class));
    }

    @Test
    void unknownEnvelopeAndReasonFieldsAreRejectedDeliberately() throws Exception {
        ObjectNode root = encodedTree();
        root.put("futureField", true);

        assertFailure(Failure.UNKNOWN_FIELD, OBJECT_MAPPER.writeValueAsBytes(root));

        root = encodedTree();
        ((ObjectNode) root.get("reason")).put("futureField", true);
        assertFailure(Failure.UNKNOWN_FIELD, OBJECT_MAPPER.writeValueAsBytes(root));
    }

    @Test
    void unsupportedEnvelopeAndPayloadVersionsFailClosed() throws Exception {
        ObjectNode root = encodedTree();
        root.put("schemaVersion", "enterprise-lab-experiment-journal-event/v2");
        assertFailure(Failure.UNSUPPORTED_VERSION, OBJECT_MAPPER.writeValueAsBytes(root));

        root = encodedTree();
        root.put("payloadSchemaVersion", "enterprise-lab-experiment-journal-payload/v2");
        assertFailure(Failure.UNSUPPORTED_VERSION, OBJECT_MAPPER.writeValueAsBytes(root));
    }

    @Test
    void duplicateFieldsTrailingDataAndInvalidUtf8AreRejected() {
        String encoded = new String(CODEC.encode(event(1, "GENESIS", Map.of(), payload())), StandardCharsets.UTF_8);
        byte[] duplicate = ("{\"sequence\":1," + encoded.substring(1)).getBytes(StandardCharsets.UTF_8);
        assertFailure(Failure.MALFORMED_ENTRY, duplicate);
        assertFailure(Failure.MALFORMED_ENTRY, (encoded + "{}").getBytes(StandardCharsets.UTF_8));
        assertFailure(Failure.MALFORMED_ENTRY, new byte[] {(byte) 0xc3, (byte) 0x28});
        assertFailure(Failure.MALFORMED_ENTRY,
                ("\ufeff" + encoded).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void malformedTypesMissingFieldsAndUnknownEnumsAreRejected() throws Exception {
        ObjectNode root = encodedTree();
        root.put("sequence", "1");
        assertFailure(Failure.MALFORMED_ENTRY, OBJECT_MAPPER.writeValueAsBytes(root));

        root = encodedTree();
        root.remove("scenarioId");
        assertFailure(Failure.MALFORMED_ENTRY, OBJECT_MAPPER.writeValueAsBytes(root));

        root = encodedTree();
        root.put("eventType", "EXECUTE_CLASS");
        assertFailure(Failure.MALFORMED_ENTRY, OBJECT_MAPPER.writeValueAsBytes(root));
    }

    @Test
    void fingerprintMismatchIsRejectedAfterCanonicalDecoding() throws Exception {
        ObjectNode root = encodedTree();
        root.put("currentEntryFingerprint", "0".repeat(64));

        assertFailure(Failure.FINGERPRINT_MISMATCH, OBJECT_MAPPER.writeValueAsBytes(root));

        root = encodedTree();
        root.put("currentEntryFingerprint", "NOT_A_FINGERPRINT");
        assertFailure(Failure.MALFORMED_ENTRY, OBJECT_MAPPER.writeValueAsBytes(root));
    }

    @Test
    void entryPayloadCollectionDepthAndNodeBoundsAreEnforced() {
        assertFailure(Failure.EXCEEDED_BOUNDS,
                new byte[EnterpriseLabExperimentJournalCodec.HARD_MAX_ENTRY_BYTES + 1]);

        ObjectNode longText = JsonNodeFactory.instance.objectNode();
        longText.put("value", "x".repeat(EnterpriseLabExperimentJournalCodec.HARD_MAX_PAYLOAD_STRING_LENGTH + 1));
        assertCreationFailure(Failure.EXCEEDED_BOUNDS, longText);

        ObjectNode tooMany = JsonNodeFactory.instance.objectNode();
        ArrayNode values = tooMany.putArray("values");
        for (int index = 0; index <= EnterpriseLabExperimentJournalCodec.HARD_MAX_COLLECTION_ENTRIES; index++) {
            values.add(index);
        }
        assertCreationFailure(Failure.EXCEEDED_BOUNDS, tooMany);

        ObjectNode deep = JsonNodeFactory.instance.objectNode();
        ObjectNode cursor = deep;
        for (int index = 0; index <= EnterpriseLabExperimentJournalCodec.HARD_MAX_PAYLOAD_DEPTH; index++) {
            cursor = cursor.putObject("level" + index);
        }
        assertCreationFailure(Failure.EXCEEDED_BOUNDS, deep);
    }

    @Test
    void credentialFieldsCredentialValuesAndStackTracesAreRejected() {
        ObjectNode credentialField = JsonNodeFactory.instance.objectNode();
        credentialField.put("authorization", "omitted");
        assertCreationFailure(Failure.SENSITIVE_CONTENT, credentialField);

        ObjectNode bearerValue = JsonNodeFactory.instance.objectNode();
        bearerValue.put("note", "Bearer abcdefghijklmnopqrstuvwxyz");
        assertCreationFailure(Failure.SENSITIVE_CONTENT, bearerValue);

        ObjectNode stackTrace = JsonNodeFactory.instance.objectNode();
        stackTrace.put("note", "failure\n  at example.Work.run(Work.java:42)");
        assertCreationFailure(Failure.SENSITIVE_CONTENT, stackTrace);
    }

    @Test
    void metadataIsSortedBoundedAndCredentialSafe() {
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 0; index <= EnterpriseLabExperimentJournalEvent.HARD_MAX_METADATA_ENTRIES; index++) {
            values.put("key" + index, "value" + index);
        }
        assertThrows(IllegalArgumentException.class, () -> event(1, "GENESIS", values, payload()));
        assertThrows(CodecException.class,
                () -> event(1, "GENESIS", Map.of("apiKey", "omitted"), payload()));

        EnterpriseLabExperimentJournalEvent safe = event(
                1, "GENESIS", Map.of("zeta", "last", "alpha", "first"), payload());
        assertEquals(List.of("alpha", "zeta"), List.copyOf(safe.metadata().keySet()));
    }

    @Test
    void payloadMustRemainDataOnlyJsonObject() {
        CodecException nullPayload = assertThrows(
                CodecException.class, () -> event(1, "GENESIS", Map.of(), null));
        assertEquals(Failure.MALFORMED_ENTRY, nullPayload.failure());

        ArrayNode arrayPayload = JsonNodeFactory.instance.arrayNode().add("value");
        CodecException array = assertThrows(
                CodecException.class, () -> event(1, "GENESIS", Map.of(), arrayPayload));
        assertEquals(Failure.MALFORMED_ENTRY, array.failure());
    }

    private static EnterpriseLabExperimentJournalEvent event(
            long sequence,
            String previousFingerprint,
            Map<String, String> metadata,
            JsonNode payload) {
        return EnterpriseLabExperimentJournalEvent.create(CLOCK, new Draft(
                sequence,
                "experiment-001",
                "stable-steady-state",
                EnterpriseLabExperimentJournalEventType.EXPERIMENT_STARTED,
                EnterpriseLabExperimentState.ARMED,
                EnterpriseLabExperimentState.RUNNING,
                2,
                CONFIGURATION_FINGERPRINT,
                DECISION_FINGERPRINT,
                BASELINE_FINGERPRINT,
                CANDIDATE_FINGERPRINT,
                APPLIED_FINGERPRINT,
                new Reason("EXPERIMENT_STARTED", "candidate allocation applied in the bounded loopback lab"),
                previousFingerprint,
                metadata,
                payload));
    }

    private static ObjectNode payload() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("requestCount", 4);
        payload.putArray("backendIds").add("loopback-b").add("loopback-a");
        return payload;
    }

    private static ObjectNode encodedTree() throws Exception {
        return (ObjectNode) OBJECT_MAPPER.readTree(CODEC.encode(event(1, "GENESIS", Map.of(), payload())));
    }

    private static void assertFailure(Failure expected, byte[] bytes) {
        CodecException exception = assertThrows(CodecException.class, () -> CODEC.decode(bytes));
        assertEquals(expected, exception.failure());
    }

    private static void assertCreationFailure(Failure expected, JsonNode payload) {
        CodecException exception = assertThrows(
                CodecException.class, () -> event(1, "GENESIS", Map.of(), payload));
        assertEquals(expected, exception.failure());
    }
}
