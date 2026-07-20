package com.richmond423.loadbalancerpro.lab;

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
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEventCodec.CodecException;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEventCodec.Failure;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseStatus;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabCommandLedgerEventCodecTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Instant NOW = Instant.parse("2026-07-20T01:00:00Z");
    private static final String REQUEST = "1".repeat(64);
    private static final String REQUESTED = "2".repeat(64);
    private static final String PREVIOUS = "3".repeat(64);
    private static final String INSTALLED_BEFORE = "4".repeat(64);
    private static final String INSTALLED_AFTER = "5".repeat(64);
    private static final String PREDECESSOR = "6".repeat(64);
    private static final String RESPONSE = "7".repeat(64);
    private static final String SUPERVISOR_EVENT = "8".repeat(64);

    private final EnterpriseLabCommandLedgerEventCodec codec =
            new EnterpriseLabCommandLedgerEventCodec();

    @Test
    void canonicalEncodingIsStableAndMetadataOrderingIndependent() {
        LinkedHashMap<String, String> firstMetadata = new LinkedHashMap<>();
        firstMetadata.put("scope", "literal-loopback-only");
        firstMetadata.put("boundary", "intent-before-dispatch");
        LinkedHashMap<String, String> secondMetadata = new LinkedHashMap<>();
        secondMetadata.put("boundary", "intent-before-dispatch");
        secondMetadata.put("scope", "literal-loopback-only");

        var first = codec.issue(draft(
                LedgerSide.APPLICATION,
                EventType.APPLICATION_INTENT_PERSISTED,
                "correlation-1",
                REQUEST,
                "transaction-1",
                7L,
                11L,
                firstMetadata,
                1L,
                EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT));
        var second = codec.issue(draft(
                LedgerSide.APPLICATION,
                EventType.APPLICATION_INTENT_PERSISTED,
                "correlation-1",
                REQUEST,
                "transaction-1",
                7L,
                11L,
                secondMetadata,
                1L,
                EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT));

        assertEquals(first.currentFingerprint(), second.currentFingerprint());
        assertArrayEquals(codec.encode(first), codec.encode(second));
        assertEquals(first, codec.decode(codec.encode(first)));
        String json = new String(codec.encode(first), StandardCharsets.UTF_8);
        assertTrue(json.startsWith(
                "{\"schemaVersion\":\"enterprise-lab-supervisor-command-ledger-event/v1\","
                        + "\"ledgerSide\":\"APPLICATION\",\"sequence\":1,"));
        assertTrue(json.indexOf("\"boundary\"") < json.indexOf("\"scope\""));
    }

    @Test
    void fingerprintBindsCorrelationTransactionAndBothProcessGenerations() {
        var base = codec.issue(applicationIntent(
                "correlation-1", REQUEST, "transaction-1", 7L, 11L, Map.of()));
        var correlation = codec.issue(applicationIntent(
                "correlation-2", REQUEST, "transaction-1", 7L, 11L, Map.of()));
        var request = codec.issue(applicationIntent(
                "correlation-1", "a".repeat(64), "transaction-1", 7L, 11L, Map.of()));
        var transaction = codec.issue(applicationIntent(
                "correlation-1", REQUEST, "transaction-2", 7L, 11L, Map.of()));
        var applicationGeneration = codec.issue(applicationIntent(
                "correlation-1", REQUEST, "transaction-1", 8L, 11L, Map.of()));
        var supervisorGeneration = codec.issue(applicationIntent(
                "correlation-1", REQUEST, "transaction-1", 7L, 12L, Map.of()));

        assertNotEquals(base.currentFingerprint(), correlation.currentFingerprint());
        assertNotEquals(base.currentFingerprint(), request.currentFingerprint());
        assertNotEquals(base.currentFingerprint(), transaction.currentFingerprint());
        assertNotEquals(base.currentFingerprint(), applicationGeneration.currentFingerprint());
        assertNotEquals(base.currentFingerprint(), supervisorGeneration.currentFingerprint());
    }

    @Test
    void requestBoundIssueRejectsAnyCorrelationIdentityMismatch() {
        Request request = healthRequest();
        Draft matching = healthResponseSentDraft(EnterpriseLabCommandLedgerEvent.NONE, RESPONSE);

        assertTrue(codec.issue(request, matching).correlates(request));

        CodecException mismatch = assertThrows(CodecException.class, () -> codec.issue(
                request,
                healthResponseSentDraft("different-transaction", RESPONSE)));
        assertEquals(Failure.REQUEST_MISMATCH, mismatch.failure());
    }

    @Test
    void responseBoundIssueRequiresTheExactCanonicalResponseFingerprint() {
        Request request = healthRequest();
        Response response = healthResponse();
        var event = codec.issue(
                request,
                response,
                healthResponseSentDraft(EnterpriseLabCommandLedgerEvent.NONE, RESPONSE));

        assertTrue(event.correlates(request));
        assertTrue(event.observes(response));

        CodecException mismatch = assertThrows(CodecException.class, () -> codec.issue(
                request,
                response,
                healthResponseSentDraft(
                        EnterpriseLabCommandLedgerEvent.NONE, "a".repeat(64))));
        assertEquals(Failure.RESPONSE_MISMATCH, mismatch.failure());
    }

    @Test
    void allRequiredEventTypesHaveAValidSideAndBoundedOutcome() {
        for (EventType eventType : EventType.values()) {
            LedgerSide side = switch (eventType) {
                case APPLICATION_INTENT_PERSISTED,
                        DISPATCH_ATTEMPTED,
                        APPLICATION_RESPONSE_RECEIVED,
                        APPLICATION_COMMITTED,
                        RESPONSE_LOST,
                        TIMEOUT_OBSERVED,
                        RETRY_ISSUED,
                        RECONCILIATION_COMPLETED,
                        COMMAND_FAILED -> LedgerSide.APPLICATION;
                default -> LedgerSide.SUPERVISOR;
            };
            var event = codec.issue(draft(
                    side,
                    eventType,
                    "correlation-" + eventType.ordinal(),
                    REQUEST,
                    "transaction-1",
                    7L,
                    11L,
                    Map.of("evidence", "bounded-event"),
                    1L,
                    EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT));

            assertEquals(eventType, codec.decode(codec.encode(event)).eventType());
        }
    }

    @Test
    void eventTypesCannotCrossApplicationAndSupervisorLedgers() {
        assertThrows(IllegalArgumentException.class, () -> codec.issue(draft(
                LedgerSide.SUPERVISOR,
                EventType.APPLICATION_INTENT_PERSISTED,
                "correlation-1", REQUEST, "transaction-1", 7L, 11L,
                Map.of(), 1L, EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT)));
        assertThrows(IllegalArgumentException.class, () -> codec.issue(draft(
                LedgerSide.APPLICATION,
                EventType.SUPERVISOR_RECEIPT_PERSISTED,
                "correlation-1", REQUEST, "transaction-1", 7L, 11L,
                Map.of(), 1L, EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT)));
    }

    @Test
    void allocationMutationEvidenceRequiresExistingTransactionGenerationAndFingerprint() {
        Draft valid = applicationIntent(
                "correlation-1", REQUEST, "transaction-1", 7L, 11L, Map.of());
        assertThrows(IllegalArgumentException.class, () -> codec.issue(replaceIdentity(
                valid,
                EnterpriseLabCommandLedgerEvent.NONE,
                valid.allocationGeneration(),
                valid.requestedAllocationFingerprint())));
        assertThrows(IllegalArgumentException.class, () -> codec.issue(replaceIdentity(
                valid,
                valid.transactionId(),
                0L,
                valid.requestedAllocationFingerprint())));
        assertThrows(IllegalArgumentException.class, () -> codec.issue(replaceIdentity(
                valid,
                valid.transactionId(),
                valid.allocationGeneration(),
                EnterpriseLabCommandLedgerEvent.NONE)));
    }

    @Test
    void predecessorRulesBindGenesisAndSuccessorSequence() {
        var first = codec.issue(applicationIntent(
                "correlation-1", REQUEST, "transaction-1", 7L, 11L, Map.of()));
        var second = codec.issue(draft(
                LedgerSide.APPLICATION,
                EventType.DISPATCH_ATTEMPTED,
                "correlation-1",
                REQUEST,
                "transaction-1",
                7L,
                11L,
                Map.of(),
                2L,
                first.currentFingerprint()));

        assertEquals(first.currentFingerprint(), second.predecessorFingerprint());
        assertThrows(IllegalArgumentException.class, () -> codec.issue(draft(
                LedgerSide.APPLICATION,
                EventType.DISPATCH_ATTEMPTED,
                "correlation-1", REQUEST, "transaction-1", 7L, 11L,
                Map.of(), 2L, EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT)));
        assertThrows(IllegalArgumentException.class, () -> codec.issue(draft(
                LedgerSide.APPLICATION,
                EventType.APPLICATION_INTENT_PERSISTED,
                "correlation-1", REQUEST, "transaction-1", 7L, 11L,
                Map.of(), 1L, PREDECESSOR)));
    }

    @Test
    void exactOutcomeClassificationsAreRequiredForSafetyRelevantEvents() {
        Draft receipt = draft(
                LedgerSide.SUPERVISOR,
                EventType.SUPERVISOR_RECEIPT_PERSISTED,
                "correlation-1", REQUEST, "transaction-1", 7L, 11L,
                Map.of(), 1L, EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT);
        assertEquals(AuthenticationResult.ACCEPTED, codec.issue(receipt).authenticationResult());
        assertThrows(IllegalArgumentException.class, () -> codec.issue(replaceOutcomes(
                receipt,
                AuthenticationResult.NOT_ATTEMPTED,
                receipt.validationResult(),
                receipt.duplicateClassification(),
                receipt.mutationStatus(),
                receipt.responseClassification(),
                receipt.applicationCommitStatus(),
                receipt.retryAttempt())));

        Draft duplicate = draft(
                LedgerSide.SUPERVISOR,
                EventType.DUPLICATE_REJECTED,
                "correlation-1", REQUEST, "transaction-1", 7L, 11L,
                Map.of(), 1L, EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT);
        assertThrows(IllegalArgumentException.class, () -> codec.issue(replaceOutcomes(
                duplicate,
                duplicate.authenticationResult(),
                duplicate.validationResult(),
                DuplicateClassification.IDENTICAL_RETRY,
                duplicate.mutationStatus(),
                duplicate.responseClassification(),
                duplicate.applicationCommitStatus(),
                duplicate.retryAttempt())));
    }

    @Test
    void secretsLocationsControlsAndStackTracesAreRejectedFromMetadata() {
        assertUnsafeMetadata("authorization", "Bearer abcdefghijklmnop");
        assertUnsafeMetadata("note", "password=abcdefghijk");
        assertUnsafeMetadata("path", "safe-looking-value");
        assertUnsafeMetadata("note", "https://example.invalid/value");
        assertUnsafeMetadata("note", " at example.Type.method(Type.java:42)");
        assertUnsafeMetadata("backend_address", "internal-value");
    }

    @Test
    void metadataAndInputBytesHaveHardBounds() {
        Map<String, String> tooMany = new LinkedHashMap<>();
        for (int index = 0;
             index <= EnterpriseLabCommandLedgerEvent.HARD_MAX_METADATA_ENTRIES;
             index++) {
            tooMany.put("key-" + index, "value");
        }
        assertThrows(IllegalArgumentException.class, () -> codec.issue(applicationIntent(
                "correlation-1", REQUEST, "transaction-1", 7L, 11L, tooMany)));

        CodecException oversized = assertThrows(CodecException.class, () -> codec.decode(
                new byte[EnterpriseLabCommandLedgerEvent.HARD_MAX_EVENT_BYTES + 1]));
        assertEquals(Failure.EXCEEDED_BOUNDS, oversized.failure());
    }

    @Test
    void unsupportedVersionUnknownAndMissingFieldsFailClosed() throws Exception {
        ObjectNode valid = object(codec.issue(applicationIntent(
                "correlation-1", REQUEST, "transaction-1", 7L, 11L, Map.of())));

        ObjectNode version = valid.deepCopy();
        version.put("schemaVersion", "enterprise-lab-supervisor-command-ledger-event/v2");
        assertFailure(Failure.UNSUPPORTED_VERSION, version);

        ObjectNode unknown = valid.deepCopy();
        unknown.put("futureField", "not-accepted");
        assertFailure(Failure.UNKNOWN_FIELD, unknown);

        ObjectNode missing = valid.deepCopy();
        missing.remove("transactionId");
        assertFailure(Failure.MISSING_FIELD, missing);
    }

    @Test
    void malformedUnknownEnumAndDuplicateJsonFieldFailClosed() throws Exception {
        ObjectNode valid = object(codec.issue(applicationIntent(
                "correlation-1", REQUEST, "transaction-1", 7L, 11L, Map.of())));
        ObjectNode unknownEnum = valid.deepCopy();
        unknownEnum.put("eventType", "FUTURE_EVENT");
        assertFailure(Failure.UNKNOWN_EVENT_OR_VALUE, unknownEnum);

        String json = MAPPER.writeValueAsString(valid);
        byte[] duplicate = json.replace(
                "\"sequence\":1", "\"sequence\":1,\"sequence\":1")
                .getBytes(StandardCharsets.UTF_8);
        CodecException duplicateFailure = assertThrows(
                CodecException.class, () -> codec.decode(duplicate));
        assertEquals(Failure.MALFORMED_EVENT, duplicateFailure.failure());

        CodecException invalidUtf8 = assertThrows(
                CodecException.class, () -> codec.decode(new byte[]{(byte) 0xc3, (byte) 0x28}));
        assertEquals(Failure.MALFORMED_EVENT, invalidUtf8.failure());
    }

    @Test
    void fingerprintTamperingAndNonCanonicalEncodingAreRejected() throws Exception {
        var event = codec.issue(applicationIntent(
                "correlation-1", REQUEST, "transaction-1", 7L, 11L, Map.of()));
        ObjectNode tampered = object(event);
        tampered.put("reasonCode", "TAMPERED");
        assertFailure(Failure.FINGERPRINT_MISMATCH, tampered);

        byte[] pretty = MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(object(event));
        CodecException nonCanonical = assertThrows(
                CodecException.class, () -> codec.decode(pretty));
        assertEquals(Failure.NON_CANONICAL_EVENT, nonCanonical.failure());
    }

    @Test
    void canonicalTimestampMustUseExactUtcRepresentation() throws Exception {
        ObjectNode value = object(codec.issue(applicationIntent(
                "correlation-1", REQUEST, "transaction-1", 7L, 11L, Map.of())));
        value.put("occurredAt", "2026-07-19T18:00:00-07:00");
        CodecException exception = assertThrows(
                CodecException.class, () -> codec.decode(MAPPER.writeValueAsBytes(value)));
        assertEquals(Failure.NON_CANONICAL_EVENT, exception.failure());
    }

    private Draft applicationIntent(
            String correlation,
            String requestFingerprint,
            String transaction,
            long applicationGeneration,
            long supervisorGeneration,
            Map<String, String> metadata) {
        return draft(
                LedgerSide.APPLICATION,
                EventType.APPLICATION_INTENT_PERSISTED,
                correlation,
                requestFingerprint,
                transaction,
                applicationGeneration,
                supervisorGeneration,
                metadata,
                1L,
                EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT);
    }

    private Request healthRequest() {
        return new Request(
                EnterpriseLabSupervisorProtocol.SCHEMA_VERSION,
                "correlation-health",
                REQUEST,
                CommandType.HEALTH,
                "application-instance-1",
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                "supervisor-instance-1",
                11L,
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                NOW,
                Map.of());
    }

    private Response healthResponse() {
        return new Response(
                EnterpriseLabSupervisorProtocol.SCHEMA_VERSION,
                "correlation-health",
                REQUEST,
                CommandType.HEALTH,
                "supervisor-instance-1",
                11L,
                0L,
                CommandClassification.OBSERVATION,
                ResponseStatus.ACCEPTED,
                false,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                9L,
                VerificationResult.NOT_ATTEMPTED,
                "HEALTHY",
                "Supervisor process and durable state are readable",
                NOW,
                RESPONSE);
    }

    private Draft healthResponseSentDraft(String transactionId, String responseFingerprint) {
        return new Draft(
                LedgerSide.SUPERVISOR,
                1L,
                EventType.RESPONSE_SENT,
                "correlation-health",
                REQUEST,
                transactionId,
                Optional.empty(),
                CommandType.HEALTH,
                "application-instance-1",
                0L,
                "supervisor-instance-1",
                11L,
                0L,
                EnterpriseLabCommandLedgerEvent.NONE,
                EnterpriseLabCommandLedgerEvent.NONE,
                EnterpriseLabCommandLedgerEvent.NONE,
                EnterpriseLabCommandLedgerEvent.NONE,
                0L,
                0L,
                AuthenticationResult.ACCEPTED,
                ValidationResult.ACCEPTED,
                DuplicateClassification.FIRST_OBSERVATION,
                MutationStatus.NOT_ATTEMPTED,
                ResponseClassification.SENT,
                responseFingerprint,
                EnterpriseLabCommandLedgerEvent.NONE,
                ApplicationCommitStatus.NOT_ATTEMPTED,
                0,
                "RESPONSE_SENT",
                NOW,
                Map.of("boundary", "bounded-response"),
                EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT);
    }

    private Draft draft(
            LedgerSide side,
            EventType eventType,
            String correlation,
            String requestFingerprint,
            String transaction,
            long applicationGeneration,
            long supervisorGeneration,
            Map<String, String> metadata,
            long sequence,
            String predecessor) {
        AuthenticationResult authentication = eventType == EventType.AUTHENTICATION_REJECTED
                ? AuthenticationResult.REJECTED
                : side == LedgerSide.SUPERVISOR
                ? AuthenticationResult.ACCEPTED : AuthenticationResult.NOT_ATTEMPTED;
        ValidationResult validation = eventType == EventType.VALIDATION_REJECTED
                ? ValidationResult.REJECTED : ValidationResult.NOT_ATTEMPTED;
        DuplicateClassification duplicate = switch (eventType) {
            case DUPLICATE_ACCEPTED, RETRY_ISSUED -> DuplicateClassification.IDENTICAL_RETRY;
            case DUPLICATE_REJECTED -> DuplicateClassification.CONFLICTING_CORRELATION;
            default -> DuplicateClassification.FIRST_OBSERVATION;
        };
        MutationStatus mutation = switch (eventType) {
            case MUTATION_STARTED -> MutationStatus.STARTED;
            case ALLOCATION_APPLIED -> MutationStatus.APPLIED;
            case READ_BACK_VERIFIED -> MutationStatus.READ_BACK_VERIFIED;
            case SUPERVISOR_COMMITTED -> MutationStatus.COMMITTED;
            case COMMAND_QUARANTINED -> MutationStatus.QUARANTINED;
            default -> MutationStatus.NOT_ATTEMPTED;
        };
        ResponseClassification response = switch (eventType) {
            case RESPONSE_SENT -> ResponseClassification.SENT;
            case APPLICATION_RESPONSE_RECEIVED -> ResponseClassification.RECEIVED;
            case RESPONSE_LOST -> ResponseClassification.LOST;
            case TIMEOUT_OBSERVED -> ResponseClassification.TIMED_OUT;
            default -> ResponseClassification.NOT_ATTEMPTED;
        };
        ApplicationCommitStatus applicationCommit =
                eventType == EventType.APPLICATION_COMMITTED
                        ? ApplicationCommitStatus.COMMITTED
                        : eventType == EventType.APPLICATION_INTENT_PERSISTED
                        ? ApplicationCommitStatus.PENDING
                        : ApplicationCommitStatus.NOT_ATTEMPTED;
        String responseFingerprint = eventType == EventType.RESPONSE_SENT
                || eventType == EventType.APPLICATION_RESPONSE_RECEIVED
                ? RESPONSE : EnterpriseLabCommandLedgerEvent.NONE;
        String observedSupervisorEvent = eventType == EventType.APPLICATION_RESPONSE_RECEIVED
                || eventType == EventType.APPLICATION_COMMITTED
                ? SUPERVISOR_EVENT : EnterpriseLabCommandLedgerEvent.NONE;
        boolean afterRequired = eventType == EventType.READ_BACK_VERIFIED
                || eventType == EventType.SUPERVISOR_COMMITTED;
        return new Draft(
                side,
                sequence,
                eventType,
                correlation,
                requestFingerprint,
                transaction,
                Optional.of("experiment-1"),
                CommandType.APPLY_ALLOCATION,
                "application-instance-1",
                applicationGeneration,
                "supervisor-instance-1",
                supervisorGeneration,
                3L,
                REQUESTED,
                PREVIOUS,
                INSTALLED_BEFORE,
                afterRequired ? INSTALLED_AFTER : EnterpriseLabCommandLedgerEvent.NONE,
                8L,
                afterRequired || eventType == EventType.ALLOCATION_APPLIED ? 9L : 8L,
                authentication,
                validation,
                duplicate,
                mutation,
                response,
                responseFingerprint,
                observedSupervisorEvent,
                applicationCommit,
                eventType == EventType.RETRY_ISSUED ? 1 : 0,
                eventType.name(),
                NOW,
                metadata,
                predecessor);
    }

    private static Draft replaceIdentity(
            Draft source,
            String transactionId,
            long allocationGeneration,
            String requestedFingerprint) {
        return new Draft(
                source.ledgerSide(), source.sequence(), source.eventType(),
                source.correlationId(), source.requestFingerprint(), transactionId,
                source.experimentId(), source.commandType(), source.applicationInstanceId(),
                source.applicationOwnerGeneration(), source.supervisorInstanceId(),
                source.supervisorGeneration(), allocationGeneration, requestedFingerprint,
                source.previousCommittedFingerprint(), source.installedFingerprintBefore(),
                source.installedFingerprintAfter(), source.routerGenerationBefore(),
                source.routerGenerationAfter(), source.authenticationResult(),
                source.validationResult(), source.duplicateClassification(),
                source.mutationStatus(), source.responseClassification(),
                source.responseFingerprint(), source.observedSupervisorEventFingerprint(),
                source.applicationCommitStatus(), source.retryAttempt(), source.reasonCode(),
                source.occurredAt(), source.metadata(), source.predecessorFingerprint());
    }

    private static Draft replaceOutcomes(
            Draft source,
            AuthenticationResult authentication,
            ValidationResult validation,
            DuplicateClassification duplicate,
            MutationStatus mutation,
            ResponseClassification response,
            ApplicationCommitStatus applicationCommit,
            int retryAttempt) {
        return new Draft(
                source.ledgerSide(), source.sequence(), source.eventType(),
                source.correlationId(), source.requestFingerprint(), source.transactionId(),
                source.experimentId(), source.commandType(), source.applicationInstanceId(),
                source.applicationOwnerGeneration(), source.supervisorInstanceId(),
                source.supervisorGeneration(), source.allocationGeneration(),
                source.requestedAllocationFingerprint(), source.previousCommittedFingerprint(),
                source.installedFingerprintBefore(), source.installedFingerprintAfter(),
                source.routerGenerationBefore(), source.routerGenerationAfter(), authentication,
                validation, duplicate, mutation, response, source.responseFingerprint(),
                source.observedSupervisorEventFingerprint(), applicationCommit, retryAttempt,
                source.reasonCode(), source.occurredAt(), source.metadata(),
                source.predecessorFingerprint());
    }

    private void assertUnsafeMetadata(String key, String value) {
        assertThrows(IllegalArgumentException.class, () -> codec.issue(applicationIntent(
                "correlation-1",
                REQUEST,
                "transaction-1",
                7L,
                11L,
                Map.of(key, value))));
    }

    private ObjectNode object(EnterpriseLabCommandLedgerEvent event) throws Exception {
        return (ObjectNode) MAPPER.readTree(codec.encode(event));
    }

    private void assertFailure(Failure failure, ObjectNode value) throws Exception {
        CodecException exception = assertThrows(
                CodecException.class, () -> codec.decode(MAPPER.writeValueAsBytes(value)));
        assertEquals(failure, exception.failure());
    }
}
