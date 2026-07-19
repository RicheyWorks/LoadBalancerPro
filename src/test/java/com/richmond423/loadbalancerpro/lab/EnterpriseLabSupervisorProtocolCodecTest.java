package com.richmond423.loadbalancerpro.lab;

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
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocolCodec.CodecException;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocolCodec.Failure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabSupervisorProtocolCodecTest {
    private static final String OWNERSHIP_FINGERPRINT = "1".repeat(64);
    private static final String PREVIOUS_FINGERPRINT = "2".repeat(64);
    private static final Instant NOW = Instant.parse("2026-07-18T20:00:00Z");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EnterpriseLabExperimentTargetCatalog targetCatalog;
    private EnterpriseLabSupervisorProtocolCodec codec;
    private EnterpriseLabLoopbackAllocationSnapshot baseline;
    private EnterpriseLabLoopbackAllocationSnapshot candidate;
    private EnterpriseLabInstalledAllocationSnapshot installedBaseline;

    @BeforeEach
    void setUp() {
        targetCatalog = targets("blue", "green", "orange");
        codec = new EnterpriseLabSupervisorProtocolCodec(targetCatalog);
        baseline = allocation(
                0L,
                "baseline-decision",
                EnterpriseLabLoopbackAllocationSnapshot.Kind.BASELINE,
                Map.of("blue", 0.5d, "green", 0.3d, "orange", 0.2d));
        candidate = allocation(
                1L,
                "candidate-decision",
                EnterpriseLabLoopbackAllocationSnapshot.Kind.CANDIDATE,
                Map.of("blue", 0.3d, "green", 0.5d, "orange", 0.2d));
        installedBaseline = EnterpriseLabInstalledAllocationSnapshot.installed(
                baseline,
                Clock.fixed(NOW, ZoneOffset.UTC),
                "SUPERVISOR_BASELINE_ESTABLISHED",
                1L);
    }

    @Test
    void requestEncodingIsCanonicalStableAndOrderingIndependent() {
        LinkedHashMap<String, String> firstMetadata = new LinkedHashMap<>();
        firstMetadata.put("scope", "literal-loopback-only");
        firstMetadata.put("evidence", "bounded-protocol");
        LinkedHashMap<String, String> secondMetadata = new LinkedHashMap<>();
        secondMetadata.put("evidence", "bounded-protocol");
        secondMetadata.put("scope", "literal-loopback-only");

        Request first = codec.issue(healthDraft(firstMetadata));
        Request second = codec.issue(healthDraft(secondMetadata));

        assertEquals(first.requestFingerprint(), second.requestFingerprint());
        assertArrayEquals(codec.encodeRequest(first), codec.encodeRequest(second));
        assertEquals(first, codec.decodeRequest(codec.encodeRequest(first)));
        assertTrue(new String(codec.encodeRequest(first), StandardCharsets.UTF_8)
                .startsWith("{\"schemaVersion\":\"enterprise-lab-allocation-supervisor-ipc/v1\""));
    }

    @Test
    void requestFingerprintChangesWithBusinessContent() {
        Request first = codec.issue(healthDraft(Map.of("scope", "literal-loopback-only")));
        Request second = codec.issue(new RequestDraft(
                "health-request-2",
                CommandType.HEALTH,
                "application-a",
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                NOW,
                Map.of("scope", "literal-loopback-only")));

        assertNotEquals(first.requestFingerprint(), second.requestFingerprint());
        assertNotEquals(
                new String(codec.encodeRequest(first), StandardCharsets.UTF_8),
                new String(codec.encodeRequest(second), StandardCharsets.UTF_8));
    }

    @Test
    void allocationCommandCarriesTheExistingCanonicalAllocationRepresentation() {
        Request request = codec.issue(baselineDraft(baseline));

        assertEquals(baseline, request.allocation().orElseThrow());
        assertEquals(
                EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                        baseline.scenarioId(), baseline.allocations()),
                request.allocationFingerprint());
        assertEquals(request, codec.decodeRequest(codec.encodeRequest(request)));
    }

    @Test
    void responseRoundTripBindsExactRequestAndInstalledReadBack() {
        Request request = codec.issue(baselineDraft(baseline));
        Response response = codec.issue(request, new ResponseDraft(
                request.requestId(),
                request.requestFingerprint(),
                request.commandType(),
                "supervisor-a",
                1L,
                1L,
                CommandClassification.ALLOCATION_MUTATION,
                ResponseStatus.ACCEPTED,
                true,
                Optional.of(installedBaseline),
                installedBaseline.allocationFingerprint(),
                installedBaseline.routerGeneration(),
                1L,
                VerificationResult.MATCHED,
                "BASELINE_ESTABLISHED",
                "the fixed Enterprise Lab baseline was durably installed and read back",
                NOW.plusSeconds(1L)));

        assertTrue(response.correlates(request));
        assertEquals(response, codec.decodeResponse(codec.encodeResponse(response), request));
        assertEquals(
                response.responseFingerprint(),
                codec.canonicalResponseFingerprint(response));
    }

    @Test
    void responseCorrelationRejectsChangedRequestIdentityFingerprintOrCommand() {
        Request request = codec.issue(baselineDraft(baseline));
        Response response = acceptedBaselineResponse(request);
        Request changedId = codec.issue(new RequestDraft(
                "baseline-request-2",
                request.commandType(),
                request.applicationInstanceId(),
                request.applicationOwnershipRecordFingerprint(),
                request.applicationOwnerGeneration(),
                request.expectedSupervisorInstanceId(),
                request.expectedSupervisorGeneration(),
                request.transactionId(),
                request.experimentId(),
                request.allocationPurpose(),
                request.allocation(),
                request.allocationFingerprint(),
                request.previousCommittedFingerprint(),
                request.requestedAt(),
                request.metadata()));

        assertFalse(response.correlates(changedId));
        assertFalse(response.correlates(codec.issue(healthDraft(Map.of()))));
        assertEquals(Failure.REQUEST_MISMATCH, assertThrows(CodecException.class,
                () -> codec.decodeResponse(codec.encodeResponse(response), changedId)).failure());
    }

    @Test
    void strictDecoderRejectsUnknownSchemaCommandAndFields() throws Exception {
        byte[] encoded = codec.encodeRequest(codec.issue(healthDraft(Map.of())));
        ObjectNode unknownSchema = (ObjectNode) MAPPER.readTree(encoded);
        unknownSchema.put("schemaVersion", "enterprise-lab-allocation-supervisor-ipc/v2");
        CodecException schemaFailure = assertThrows(CodecException.class,
                () -> codec.decodeRequest(MAPPER.writeValueAsBytes(unknownSchema)));
        assertEquals(Failure.UNSUPPORTED_VERSION, schemaFailure.failure());

        ObjectNode unknownCommand = (ObjectNode) MAPPER.readTree(encoded);
        unknownCommand.put("commandType", "EXECUTE_ARBITRARY_COMMAND");
        CodecException commandFailure = assertThrows(CodecException.class,
                () -> codec.decodeRequest(MAPPER.writeValueAsBytes(unknownCommand)));
        assertEquals(Failure.UNKNOWN_COMMAND_OR_VALUE, commandFailure.failure());

        ObjectNode unknownField = (ObjectNode) MAPPER.readTree(encoded);
        unknownField.put("supervisorAddress", "forbidden");
        CodecException fieldFailure = assertThrows(CodecException.class,
                () -> codec.decodeRequest(MAPPER.writeValueAsBytes(unknownField)));
        assertEquals(Failure.UNKNOWN_FIELD, fieldFailure.failure());
    }

    @Test
    void strictDecoderRejectsMissingDuplicateAndNonCanonicalJson() throws Exception {
        byte[] encoded = codec.encodeRequest(codec.issue(healthDraft(Map.of())));
        ObjectNode missing = (ObjectNode) MAPPER.readTree(encoded);
        missing.remove("requestedAt");
        assertEquals(Failure.MISSING_FIELD, assertThrows(CodecException.class,
                () -> codec.decodeRequest(MAPPER.writeValueAsBytes(missing))).failure());

        String json = new String(encoded, StandardCharsets.UTF_8);
        String duplicate = json.replaceFirst(
                "\\{",
                "{\"requestId\":\"duplicate-request\",");
        assertEquals(Failure.MALFORMED_MESSAGE, assertThrows(CodecException.class,
                () -> codec.decodeRequest(duplicate.getBytes(StandardCharsets.UTF_8))).failure());

        byte[] spaced = (" " + json).getBytes(StandardCharsets.UTF_8);
        assertEquals(Failure.NON_CANONICAL_MESSAGE, assertThrows(CodecException.class,
                () -> codec.decodeRequest(spaced)).failure());
    }

    @Test
    void strictDecoderRejectsFingerprintTampering() throws Exception {
        byte[] encoded = codec.encodeRequest(codec.issue(healthDraft(Map.of())));
        ObjectNode tampered = (ObjectNode) MAPPER.readTree(encoded);
        tampered.put("requestFingerprint", "f".repeat(64));

        CodecException failure = assertThrows(CodecException.class,
                () -> codec.decodeRequest(MAPPER.writeValueAsBytes(tampered)));

        assertEquals(Failure.FINGERPRINT_MISMATCH, failure.failure());
    }

    @Test
    void strictDecoderRejectsOversizedAndInvalidUtf8Input() {
        byte[] oversized = new byte[EnterpriseLabSupervisorProtocol.HARD_MAX_REQUEST_BYTES + 1];
        assertEquals(Failure.EXCEEDED_BOUNDS, assertThrows(CodecException.class,
                () -> codec.decodeRequest(oversized)).failure());
        assertEquals(Failure.MALFORMED_MESSAGE, assertThrows(CodecException.class,
                () -> codec.decodeRequest(new byte[]{(byte) 0xC3, (byte) 0x28})).failure());
    }

    @Test
    void mutationRequiresOwnershipSupervisorIdentityAndTransactionFences() {
        RequestDraft invalid = new RequestDraft(
                "baseline-request",
                CommandType.ESTABLISH_INITIAL_BASELINE,
                "application-a",
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.INITIAL_SAFE_BASELINE,
                Optional.of(baseline),
                fingerprint(baseline),
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                NOW,
                Map.of());

        assertThrows(IllegalArgumentException.class, () -> codec.issue(invalid));
    }

    @Test
    void requiredIdentitiesRejectNoneAndOptionalFencesMustBePaired() {
        RequestDraft noneApplication = new RequestDraft(
                "health-request",
                CommandType.HEALTH,
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                NOW,
                Map.of());
        assertThrows(IllegalArgumentException.class, () -> codec.issue(noneApplication));

        RequestDraft unpairedSupervisor = new RequestDraft(
                "health-request",
                CommandType.HEALTH,
                "application-a",
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                "supervisor-a",
                0L,
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                NOW,
                Map.of());
        assertThrows(IllegalArgumentException.class, () -> codec.issue(unpairedSupervisor));
    }

    @Test
    void applyRequiresExperimentCandidatePurposeAndCandidatePayload() {
        Request valid = codec.issue(applyDraft(candidate));
        assertEquals(CommandType.APPLY_ALLOCATION, valid.commandType());

        RequestDraft missingExperiment = new RequestDraft(
                valid.requestId(),
                valid.commandType(),
                valid.applicationInstanceId(),
                valid.applicationOwnershipRecordFingerprint(),
                valid.applicationOwnerGeneration(),
                valid.expectedSupervisorInstanceId(),
                valid.expectedSupervisorGeneration(),
                valid.transactionId(),
                Optional.empty(),
                valid.allocationPurpose(),
                valid.allocation(),
                valid.allocationFingerprint(),
                valid.previousCommittedFingerprint(),
                valid.requestedAt(),
                valid.metadata());
        assertThrows(IllegalArgumentException.class, () -> codec.issue(missingExperiment));

        RequestDraft baselineAsCandidate = new RequestDraft(
                valid.requestId(),
                valid.commandType(),
                valid.applicationInstanceId(),
                valid.applicationOwnershipRecordFingerprint(),
                valid.applicationOwnerGeneration(),
                valid.expectedSupervisorInstanceId(),
                valid.expectedSupervisorGeneration(),
                valid.transactionId(),
                valid.experimentId(),
                valid.allocationPurpose(),
                Optional.of(baseline),
                fingerprint(baseline),
                valid.previousCommittedFingerprint(),
                valid.requestedAt(),
                valid.metadata());
        assertThrows(IllegalArgumentException.class, () -> codec.issue(baselineAsCandidate));

        RequestDraft missingPriorFingerprint = new RequestDraft(
                valid.requestId(),
                valid.commandType(),
                valid.applicationInstanceId(),
                valid.applicationOwnershipRecordFingerprint(),
                valid.applicationOwnerGeneration(),
                valid.expectedSupervisorInstanceId(),
                valid.expectedSupervisorGeneration(),
                valid.transactionId(),
                valid.experimentId(),
                valid.allocationPurpose(),
                valid.allocation(),
                valid.allocationFingerprint(),
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                valid.requestedAt(),
                valid.metadata());
        assertThrows(IllegalArgumentException.class, () -> codec.issue(missingPriorFingerprint));
    }

    @Test
    void restoreRequiresPriorFingerprintAndVerificationCannotCarryOne() {
        RequestDraft restoreWithoutPrior = fencedDraft(
                CommandType.RESTORE_BASELINE,
                AllocationPurpose.STARTUP_RESTORATION,
                fingerprint(baseline),
                EnterpriseLabAllocationState.NO_FINGERPRINT);
        assertThrows(IllegalArgumentException.class, () -> codec.issue(restoreWithoutPrior));

        RequestDraft verifyWithPrior = fencedDraft(
                CommandType.VERIFY_ALLOCATION,
                AllocationPurpose.RECONCILIATION_NO_OP,
                fingerprint(baseline),
                PREVIOUS_FINGERPRINT);
        assertThrows(IllegalArgumentException.class, () -> codec.issue(verifyWithPrior));

        assertEquals(CommandType.RESTORE_BASELINE, codec.issue(fencedDraft(
                CommandType.RESTORE_BASELINE,
                AllocationPurpose.STARTUP_RESTORATION,
                fingerprint(baseline),
                PREVIOUS_FINGERPRINT)).commandType());
        assertEquals(CommandType.VERIFY_ALLOCATION, codec.issue(fencedDraft(
                CommandType.VERIFY_ALLOCATION,
                AllocationPurpose.RECONCILIATION_NO_OP,
                fingerprint(baseline),
                EnterpriseLabAllocationState.NO_FINGERPRINT)).commandType());
    }

    @Test
    void allocationFingerprintAndApprovedBackendBindingAreBothVerified() {
        RequestDraft wrongFingerprint = new RequestDraft(
                "baseline-request",
                CommandType.ESTABLISH_INITIAL_BASELINE,
                "application-a",
                OWNERSHIP_FINGERPRINT,
                1L,
                "supervisor-a",
                1L,
                "transaction-a",
                Optional.empty(),
                AllocationPurpose.INITIAL_SAFE_BASELINE,
                Optional.of(baseline),
                "9".repeat(64),
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                NOW,
                Map.of());
        assertThrows(IllegalArgumentException.class, () -> codec.issue(wrongFingerprint));

        EnterpriseLabLoopbackAllocationSnapshot unknownBackends =
                new EnterpriseLabLoopbackAllocationSnapshot(
                        EnterpriseLabLoopbackAllocationSnapshot.SCHEMA_VERSION,
                        "tail-latency-pressure",
                        0L,
                        "baseline-decision",
                        EnterpriseLabLoopbackAllocationSnapshot.Kind.BASELINE,
                        Map.of("unknown-a", 0.5d, "unknown-b", 0.5d));
        CodecException targetFailure = assertThrows(CodecException.class,
                () -> codec.issue(baselineDraft(unknownBackends)));
        assertEquals(Failure.TARGET_MISMATCH, targetFailure.failure());
    }

    @Test
    void metadataRejectsCredentialsPathsAddressesControlKeysAndExcessEntries() {
        assertThrows(IllegalArgumentException.class,
                () -> codec.issue(healthDraft(Map.of("apiKey", "masked"))));
        assertThrows(IllegalArgumentException.class,
                () -> codec.issue(healthDraft(Map.of("note", "password=unsafe"))));
        assertThrows(IllegalArgumentException.class,
                () -> codec.issue(healthDraft(Map.of("note", "http://127.0.0.1:9000"))));
        assertThrows(IllegalArgumentException.class,
                () -> codec.issue(healthDraft(Map.of("note", "/var/run/supervisor.sock"))));
        assertThrows(IllegalArgumentException.class,
                () -> codec.issue(healthDraft(Map.of("note", "127.0.0.1:9000"))));
        assertThrows(IllegalArgumentException.class,
                () -> codec.issue(healthDraft(Map.of("path", "bounded"))));

        Map<String, String> excessive = new LinkedHashMap<>();
        for (int index = 0; index <= EnterpriseLabSupervisorProtocol.HARD_MAX_METADATA_ENTRIES;
                index++) {
            excessive.put("key-" + index, "value-" + index);
        }
        assertThrows(IllegalArgumentException.class,
                () -> codec.issue(healthDraft(excessive)));
    }

    @Test
    void businessPayloadContainsNoTransportCredentialOrAddressField() {
        String encoded = new String(
                codec.encodeRequest(codec.issue(baselineDraft(baseline))),
                StandardCharsets.UTF_8);

        assertFalse(encoded.contains("authentication"));
        assertFalse(encoded.contains("credential"));
        assertFalse(encoded.contains("secret"));
        assertFalse(encoded.contains("host"));
        assertFalse(encoded.contains("port"));
        assertFalse(encoded.contains("path"));
        assertFalse(encoded.contains("url"));
    }

    @Test
    void rejectedAndObservationResponsesCannotClaimMutation() {
        Request health = codec.issue(healthDraft(Map.of()));
        ResponseDraft invalidObservation = new ResponseDraft(
                health.requestId(),
                health.requestFingerprint(),
                health.commandType(),
                "supervisor-a",
                1L,
                0L,
                CommandClassification.OBSERVATION,
                ResponseStatus.ACCEPTED,
                true,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                0L,
                VerificationResult.NOT_ATTEMPTED,
                "HEALTHY",
                "the bounded supervisor process is alive",
                NOW);
        assertThrows(IllegalArgumentException.class,
                () -> codec.issue(health, invalidObservation));

        Request baselineRequest = codec.issue(baselineDraft(baseline));
        ResponseDraft invalidRejected = new ResponseDraft(
                baselineRequest.requestId(),
                baselineRequest.requestFingerprint(),
                baselineRequest.commandType(),
                "supervisor-a",
                1L,
                1L,
                CommandClassification.ALLOCATION_MUTATION,
                ResponseStatus.REJECTED,
                true,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                0L,
                VerificationResult.NOT_ATTEMPTED,
                "REQUEST_REJECTED",
                "the request was rejected without changing installed state",
                NOW);
        assertThrows(IllegalArgumentException.class,
                () -> codec.issue(baselineRequest, invalidRejected));
    }

    @Test
    void acceptedAllocationResponseRequiresExactReadBack() {
        Request request = codec.issue(baselineDraft(baseline));
        ResponseDraft missingReadBack = new ResponseDraft(
                request.requestId(),
                request.requestFingerprint(),
                request.commandType(),
                "supervisor-a",
                1L,
                1L,
                CommandClassification.ALLOCATION_MUTATION,
                ResponseStatus.ACCEPTED,
                false,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                1L,
                VerificationResult.NOT_ATTEMPTED,
                "BASELINE_UNVERIFIED",
                "the baseline was not independently read back",
                NOW);
        assertThrows(IllegalArgumentException.class,
                () -> codec.issue(request, missingReadBack));

        ResponseDraft mismatchedSummary = new ResponseDraft(
                request.requestId(),
                request.requestFingerprint(),
                request.commandType(),
                "supervisor-a",
                1L,
                1L,
                CommandClassification.ALLOCATION_MUTATION,
                ResponseStatus.ACCEPTED,
                true,
                Optional.of(installedBaseline),
                "8".repeat(64),
                installedBaseline.routerGeneration(),
                1L,
                VerificationResult.MATCHED,
                "BASELINE_ESTABLISHED",
                "the baseline summary does not match read-back",
                NOW);
        assertThrows(IllegalArgumentException.class,
                () -> codec.issue(request, mismatchedSummary));
    }

    @Test
    void acceptedResponseMustMatchExactRequestFencesAndRequestedAllocation() {
        Request request = codec.issue(baselineDraft(baseline));

        ResponseDraft wrongSupervisor = acceptedBaselineDraft(request, installedBaseline);
        wrongSupervisor = new ResponseDraft(
                wrongSupervisor.requestId(),
                wrongSupervisor.requestFingerprint(),
                wrongSupervisor.commandType(),
                "supervisor-b",
                wrongSupervisor.supervisorGeneration(),
                wrongSupervisor.observedApplicationGeneration(),
                wrongSupervisor.commandClassification(),
                wrongSupervisor.status(),
                wrongSupervisor.actionPerformed(),
                wrongSupervisor.installedAllocation(),
                wrongSupervisor.installedFingerprint(),
                wrongSupervisor.routerGeneration(),
                wrongSupervisor.durableStateGeneration(),
                wrongSupervisor.verificationResult(),
                wrongSupervisor.reasonCode(),
                wrongSupervisor.reason(),
                wrongSupervisor.respondedAt());
        ResponseDraft mismatchedSupervisor = wrongSupervisor;
        assertEquals(Failure.REQUEST_MISMATCH, assertThrows(CodecException.class,
                () -> codec.issue(request, mismatchedSupervisor)).failure());

        EnterpriseLabInstalledAllocationSnapshot installedCandidate =
                EnterpriseLabInstalledAllocationSnapshot.installed(
                        candidate,
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        "SUPERVISOR_CANDIDATE_INSTALLED",
                        2L);
        ResponseDraft wrongAllocation = acceptedBaselineDraft(request, installedCandidate);
        assertEquals(Failure.REQUEST_MISMATCH, assertThrows(CodecException.class,
                () -> codec.issue(request, wrongAllocation)).failure());
    }

    @Test
    void acceptedInstalledReadBackDoesNotClaimAllocationVerification() {
        Request request = codec.issue(new RequestDraft(
                "read-installed-request",
                CommandType.READ_INSTALLED_ALLOCATION,
                "application-a",
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                NOW,
                Map.of()));
        Response response = codec.issue(request, new ResponseDraft(
                request.requestId(),
                request.requestFingerprint(),
                request.commandType(),
                "supervisor-a",
                1L,
                0L,
                CommandClassification.OBSERVATION,
                ResponseStatus.ACCEPTED,
                false,
                Optional.of(installedBaseline),
                installedBaseline.allocationFingerprint(),
                installedBaseline.routerGeneration(),
                1L,
                VerificationResult.NOT_ATTEMPTED,
                "INSTALLED_ALLOCATION_READ",
                "the current installed allocation was read without target verification",
                NOW.plusSeconds(1L)));

        assertEquals(VerificationResult.NOT_ATTEMPTED, response.verificationResult());
        assertEquals(response, codec.decodeResponse(codec.encodeResponse(response), request));
    }

    @Test
    void responseReasonRejectsAddressAndPathShapedEvidence() {
        Request request = codec.issue(healthDraft(Map.of()));
        ResponseDraft unsafe = new ResponseDraft(
                request.requestId(),
                request.requestFingerprint(),
                request.commandType(),
                "supervisor-a",
                1L,
                0L,
                CommandClassification.OBSERVATION,
                ResponseStatus.FAILED,
                false,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                0L,
                VerificationResult.NOT_ATTEMPTED,
                "READ_FAILED",
                "failed to read /var/run/supervisor.sock",
                NOW);

        assertThrows(IllegalArgumentException.class, () -> codec.issue(request, unsafe));
    }

    @Test
    void responseFingerprintTamperingIsRejected() throws Exception {
        Request request = codec.issue(baselineDraft(baseline));
        Response response = acceptedBaselineResponse(request);
        ObjectNode tampered = (ObjectNode) MAPPER.readTree(codec.encodeResponse(response));
        tampered.put("responseFingerprint", "e".repeat(64));

        CodecException failure = assertThrows(CodecException.class,
                () -> codec.decodeResponse(MAPPER.writeValueAsBytes(tampered), request));

        assertEquals(Failure.FINGERPRINT_MISMATCH, failure.failure());
    }

    private RequestDraft healthDraft(Map<String, String> metadata) {
        return new RequestDraft(
                "health-request-1",
                CommandType.HEALTH,
                "application-a",
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                NOW,
                metadata);
    }

    private RequestDraft baselineDraft(EnterpriseLabLoopbackAllocationSnapshot snapshot) {
        return new RequestDraft(
                "baseline-request-1",
                CommandType.ESTABLISH_INITIAL_BASELINE,
                "application-a",
                OWNERSHIP_FINGERPRINT,
                1L,
                "supervisor-a",
                1L,
                "baseline-transaction-1",
                Optional.empty(),
                AllocationPurpose.INITIAL_SAFE_BASELINE,
                Optional.of(snapshot),
                fingerprint(snapshot),
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                NOW,
                Map.of("scope", "literal-loopback-only"));
    }

    private RequestDraft applyDraft(EnterpriseLabLoopbackAllocationSnapshot snapshot) {
        return new RequestDraft(
                "apply-request-1",
                CommandType.APPLY_ALLOCATION,
                "application-a",
                OWNERSHIP_FINGERPRINT,
                1L,
                "supervisor-a",
                1L,
                "candidate-transaction-1",
                Optional.of("experiment-1"),
                AllocationPurpose.EXPERIMENT_CANDIDATE,
                Optional.of(snapshot),
                fingerprint(snapshot),
                PREVIOUS_FINGERPRINT,
                NOW,
                Map.of("scope", "literal-loopback-only"));
    }

    private Response acceptedBaselineResponse(Request request) {
        return codec.issue(request, acceptedBaselineDraft(request, installedBaseline));
    }

    private ResponseDraft acceptedBaselineDraft(
            Request request,
            EnterpriseLabInstalledAllocationSnapshot installed) {
        return new ResponseDraft(
                request.requestId(),
                request.requestFingerprint(),
                request.commandType(),
                "supervisor-a",
                1L,
                1L,
                CommandClassification.ALLOCATION_MUTATION,
                ResponseStatus.ACCEPTED,
                true,
                Optional.of(installed),
                installed.allocationFingerprint(),
                installed.routerGeneration(),
                1L,
                VerificationResult.MATCHED,
                "BASELINE_ESTABLISHED",
                "the fixed Enterprise Lab baseline was durably installed and read back",
                NOW.plusSeconds(1L));
    }

    private RequestDraft fencedDraft(
            CommandType command,
            AllocationPurpose purpose,
            String allocationFingerprint,
            String previousFingerprint) {
        return new RequestDraft(
                "fenced-request-1",
                command,
                "application-a",
                OWNERSHIP_FINGERPRINT,
                1L,
                "supervisor-a",
                1L,
                "fenced-transaction-1",
                Optional.empty(),
                purpose,
                Optional.empty(),
                allocationFingerprint,
                previousFingerprint,
                NOW,
                Map.of());
    }

    private EnterpriseLabLoopbackAllocationSnapshot allocation(
            long revision,
            String sourceDecisionId,
            EnterpriseLabLoopbackAllocationSnapshot.Kind kind,
            Map<String, Double> allocations) {
        return new EnterpriseLabLoopbackAllocationSnapshot(
                EnterpriseLabLoopbackAllocationSnapshot.SCHEMA_VERSION,
                "tail-latency-pressure",
                revision,
                sourceDecisionId,
                kind,
                allocations);
    }

    private static String fingerprint(EnterpriseLabLoopbackAllocationSnapshot snapshot) {
        return EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                snapshot.scenarioId(), snapshot.allocations());
    }

    private static EnterpriseLabExperimentTargetCatalog targets(String... backendIds) {
        return new EnterpriseLabExperimentTargetCatalog(java.util.Arrays.stream(backendIds)
                .map(backendId -> new EnterpriseLabLoopbackTarget(
                        "tail-latency-pressure",
                        backendId,
                        URI.create("http://127.0.0.1:1/supervisor-protocol")))
                .toList());
    }
}
