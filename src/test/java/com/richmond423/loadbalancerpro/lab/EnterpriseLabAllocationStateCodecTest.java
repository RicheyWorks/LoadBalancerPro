package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.Draft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.RecoveryClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransitionReason;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationStateCodec.CodecException;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationStateCodec.Failure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.RecordComponent;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
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

class EnterpriseLabAllocationStateCodecTest {
    private static final String SCENARIO = "tail-latency-pressure";
    private static final Instant NOW = Instant.parse("2026-07-18T09:00:00Z");
    private static final Map<String, Double> BASELINE =
            Map.of("blue", 0.5, "green", 0.25, "orange", 0.25);
    private static final Map<String, Double> CANDIDATE =
            Map.of("blue", 0.25, "green", 0.5, "orange", 0.25);

    @TempDir
    Path temporaryDirectory;

    private EnterpriseLabExperimentTargetCatalog targets;
    private EnterpriseLabMutationTestAuthority authority;
    private EnterpriseLabAllocationStateCodec codec;

    @BeforeEach
    void setUp() {
        targets = targetCatalog();
        authority = new EnterpriseLabMutationTestAuthority(temporaryDirectory);
        codec = new EnterpriseLabAllocationStateCodec(targets);
    }

    @Test
    void identicalLogicalRecordsProduceIdenticalCanonicalBytesAndRoundTrip() {
        Map<String, Double> orderedBaseline = new LinkedHashMap<>();
        orderedBaseline.put("orange", 0.25);
        orderedBaseline.put("blue", 0.5);
        orderedBaseline.put("green", 0.25);
        Map<String, String> orderedMetadata = new LinkedHashMap<>();
        orderedMetadata.put("source", "focused-test");
        orderedMetadata.put("boundary", "literal-loopback-only");

        EnterpriseLabAllocationState first = create(draft(
                orderedBaseline,
                CANDIDATE,
                Map.of("source", "focused-test", "boundary", "literal-loopback-only")));
        EnterpriseLabAllocationState second = create(draft(
                new LinkedHashMap<>(BASELINE),
                reversed(CANDIDATE),
                orderedMetadata));

        assertEquals(first, second);
        assertArrayEquals(codec.encode(first), codec.encode(second));
        assertArrayEquals(codec.canonicalContentBytes(first), codec.canonicalContentBytes(second));
        assertEquals(first.currentRecordFingerprint(), second.currentRecordFingerprint());
        assertEquals(first.normalizedAllocationFingerprint(), second.normalizedAllocationFingerprint());
        assertEquals(first, codec.decode(codec.encode(first)));
    }

    @Test
    void exactBinarySharesUseCanonicalHexAndSurviveRoundTrip() {
        EnterpriseLabAllocationState state = create(draft(BASELINE, CANDIDATE, Map.of()));
        String encoded = new String(codec.encode(state), StandardCharsets.UTF_8);
        EnterpriseLabAllocationState decoded = codec.decode(codec.encode(state));

        assertTrue(encoded.contains("\"blue\":\"0x1.0p-2\""));
        assertTrue(encoded.contains("\"green\":\"0x1.0p-1\""));
        CANDIDATE.forEach((backendId, share) -> assertEquals(
                Double.doubleToLongBits(share),
                Double.doubleToLongBits(decoded.guardrailApprovedAllocation().get(backendId))));
    }

    @Test
    void normalizationCanonicalizesNegativeZeroBeforeFingerprinting() {
        Map<String, Double> positiveZero = Map.of("blue", 0.0, "green", 0.5, "orange", 0.5);
        Map<String, Double> negativeZero = Map.of("blue", -0.0, "green", 0.5, "orange", 0.5);

        EnterpriseLabAllocationState first = create(draft(BASELINE, positiveZero, Map.of()));
        EnterpriseLabAllocationState second = create(draft(BASELINE, negativeZero, Map.of()));

        assertEquals(first.normalizedAllocationFingerprint(), second.normalizedAllocationFingerprint());
        assertArrayEquals(codec.encode(first), codec.encode(second));
        assertEquals(
                Double.doubleToLongBits(0.0),
                Double.doubleToLongBits(second.guardrailApprovedAllocation().get("blue")));
    }

    @Test
    void exactApprovedBackendSetAndValidSharesAreMandatory() {
        assertInvalidAllocation(Map.of("blue", 0.5, "green", 0.5));
        assertInvalidAllocation(Map.of("blue", 0.5, "green", 0.25, "rogue", 0.25));
        assertInvalidAllocation(Map.of("blue", -0.1, "green", 0.6, "orange", 0.5));
        assertInvalidAllocation(Map.of("blue", 1.1, "green", 0.0, "orange", -0.1));
        assertInvalidAllocation(Map.of("blue", 0.4, "green", 0.4, "orange", 0.1));
        assertInvalidAllocation(Map.of("blue", Double.NaN, "green", 0.5, "orange", 0.5));
    }

    @Test
    void targetCatalogMustRemainRepositoryApprovedAndLiteralLoopback() {
        assertThrows(IllegalArgumentException.class, () -> new EnterpriseLabLoopbackTarget(
                SCENARIO, "blue", URI.create("http://192.0.2.20:18081/health")));
        assertThrows(IllegalArgumentException.class, () -> new EnterpriseLabExperimentTargetCatalog(List.of(
                new EnterpriseLabLoopbackTarget(
                        SCENARIO, "blue", URI.create("http://127.0.0.1:18081/health")))));
        assertThrows(IllegalArgumentException.class, () -> EnterpriseLabAllocationState.create(
                fixedClock(), authority, EnterpriseLabExperimentTargetCatalog.empty(),
                draft(BASELINE, CANDIDATE, Map.of())));

        EnterpriseLabAllocationState valid = create(draft(BASELINE, CANDIDATE, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabAllocationStateCodec(
                        EnterpriseLabExperimentTargetCatalog.empty()).encode(valid));
    }

    @Test
    void ownerGenerationComesOnlyFromLiveAuthorityAndIsFencedDuringCreation() {
        authority.replaceOwner("generation-seven", 7);
        EnterpriseLabAllocationState state = create(draft(BASELINE, CANDIDATE, Map.of()));

        assertEquals(7, state.ownerGeneration());
        assertEquals(0, EnterpriseLabAllocationState.class.getConstructors().length);
        List<String> draftFields = Arrays.stream(Draft.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
        assertFalse(draftFields.contains("ownerGeneration"));

        authority.fail(EnterpriseLabEvidenceOwnership.FailureClassification.RECORD_REPLACED);
        assertThrows(EnterpriseLabEvidenceOwnershipException.class,
                () -> create(draft(BASELINE, CANDIDATE, Map.of())));
    }

    @Test
    void verificationFingerprintsMustMatchTheNormalizedInstalledAllocation() {
        String candidateFingerprint = EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                SCENARIO, CANDIDATE);
        Draft matched = new Draft(
                "allocation-tx-1", Optional.of("experiment-1"), SCENARIO, 1,
                AllocationPurpose.EXPERIMENT_CANDIDATE,
                BASELINE, CANDIDATE, CANDIDATE, CANDIDATE,
                candidateFingerprint, EnterpriseLabAllocationState.NO_FINGERPRINT,
                TransactionPhase.COMMITTED,
                new TransitionReason("READ_BACK_MATCHED", "router allocation matched durable intent"),
                true, Optional.of(NOW), VerificationResult.MATCHED,
                RecoveryClassification.NOT_REQUIRED,
                EnterpriseLabAllocationState.GENESIS_FINGERPRINT, Map.of());
        EnterpriseLabAllocationState state = create(matched);

        assertEquals(VerificationResult.MATCHED, codec.decode(codec.encode(state)).verificationResult());
        assertThrows(IllegalArgumentException.class, () -> create(new Draft(
                matched.allocationTransactionId(), matched.experimentId(), matched.scenarioId(),
                matched.allocationGeneration(), matched.allocationPurpose(), matched.baselineAllocation(),
                matched.requestedAllocation(), matched.guardrailApprovedAllocation(), BASELINE,
                candidateFingerprint, matched.previousCommittedAllocationFingerprint(),
                matched.transactionPhase(), matched.transitionReason(), matched.actionPerformed(),
                matched.lastVerifiedAt(), matched.verificationResult(), matched.recoveryClassification(),
                matched.predecessorRecordFingerprint(), matched.metadata())));
    }

    @Test
    void recordAndAllocationFingerprintsChangeWithCoveredContent() {
        EnterpriseLabAllocationState first = create(draft(BASELINE, CANDIDATE, Map.of()));
        Map<String, Double> changed = Map.of("blue", 0.25, "green", 0.25, "orange", 0.5);
        EnterpriseLabAllocationState second = create(draft(BASELINE, changed, Map.of()));
        EnterpriseLabAllocationState metadataChanged = create(draft(
                BASELINE, CANDIDATE, Map.of("classification", "prepared")));

        assertNotEquals(first.normalizedAllocationFingerprint(), second.normalizedAllocationFingerprint());
        assertNotEquals(first.currentRecordFingerprint(), second.currentRecordFingerprint());
        assertNotEquals(first.currentRecordFingerprint(), metadataChanged.currentRecordFingerprint());
    }

    @Test
    void decoderRejectsSchemaFieldsDuplicatesAndFingerprintTampering() {
        String encoded = json(create(draft(BASELINE, CANDIDATE, Map.of())));

        CodecException version = assertThrows(CodecException.class, () -> codec.decode(bytes(
                encoded.replace(EnterpriseLabAllocationState.SCHEMA_VERSION,
                        "enterprise-lab-allocation-state/v999"))));
        assertEquals(Failure.UNSUPPORTED_VERSION, version.failure());

        CodecException unknown = assertThrows(CodecException.class,
                () -> codec.decode(bytes(encoded.replaceFirst("\\{", "{\"unexpected\":true,"))));
        assertEquals(Failure.UNKNOWN_FIELD, unknown.failure());

        String schemaField = "\"schemaVersion\":\"" + EnterpriseLabAllocationState.SCHEMA_VERSION + "\",";
        CodecException duplicate = assertThrows(CodecException.class,
                () -> codec.decode(bytes(encoded.replace(schemaField, schemaField + schemaField))));
        assertEquals(Failure.MALFORMED_RECORD, duplicate.failure());

        CodecException content = assertThrows(CodecException.class,
                () -> codec.decode(bytes(encoded.replace(
                        "allocation intent prepared", "allocation intent changed"))));
        assertEquals(Failure.FINGERPRINT_MISMATCH, content.failure());

        String fingerprint = create(draft(BASELINE, CANDIDATE, Map.of()))
                .normalizedAllocationFingerprint();
        CodecException allocation = assertThrows(CodecException.class,
                () -> codec.decode(bytes(encoded.replace(fingerprint, "0".repeat(64)))));
        assertEquals(Failure.FINGERPRINT_MISMATCH, allocation.failure());
    }

    @Test
    void decoderRejectsNonCanonicalSharesDuplicateBackendsAndBoundViolations() {
        String encoded = json(create(draft(BASELINE, CANDIDATE, Map.of())));

        CodecException decimal = assertThrows(CodecException.class,
                () -> codec.decode(bytes(encoded.replaceFirst("0x1\\.0p-2", "0.25"))));
        assertEquals(Failure.MALFORMED_RECORD, decimal.failure());

        String blueShare = "\"blue\":\"0x1.0p-1\"";
        CodecException duplicate = assertThrows(CodecException.class,
                () -> codec.decode(bytes(encoded.replaceFirst(
                        java.util.regex.Pattern.quote(blueShare), blueShare + "," + blueShare))));
        assertEquals(Failure.MALFORMED_RECORD, duplicate.failure());

        byte[] oversized = new byte[EnterpriseLabAllocationStateCodec.HARD_MAX_RECORD_BYTES + 1];
        Arrays.fill(oversized, (byte) 'x');
        CodecException bounds = assertThrows(CodecException.class, () -> codec.decode(oversized));
        assertEquals(Failure.EXCEEDED_BOUNDS, bounds.failure());

        assertThrows(CodecException.class, () -> codec.decode(new byte[]{(byte) 0xC3, (byte) 0x28}));
        assertThrows(CodecException.class, () -> codec.decode(new byte[0]));
    }

    @Test
    void metadataAndReasonRejectCredentialAndRawStackTraceContent() {
        CodecException sensitiveKey = assertThrows(CodecException.class, () -> create(draft(
                BASELINE, CANDIDATE, Map.of("api_key", "redacted-is-not-a-secret"))));
        assertEquals(Failure.SENSITIVE_CONTENT, sensitiveKey.failure());

        CodecException sensitiveValue = assertThrows(CodecException.class, () -> create(draft(
                BASELINE, CANDIDATE, Map.of("note", "password=super-secret-value"))));
        assertEquals(Failure.SENSITIVE_CONTENT, sensitiveValue.failure());

        CodecException stack = assertThrows(CodecException.class, () -> create(new Draft(
                "allocation-tx-1", Optional.of("experiment-1"), SCENARIO, 1,
                AllocationPurpose.EXPERIMENT_CANDIDATE,
                BASELINE, CANDIDATE, CANDIDATE, BASELINE,
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                TransactionPhase.FAILED,
                new TransitionReason("FAILED", "at example.Type.run(Type.java:12)"),
                false, Optional.empty(), VerificationResult.READ_BACK_FAILED,
                RecoveryClassification.FAILED,
                EnterpriseLabAllocationState.GENESIS_FINGERPRINT, Map.of())));
        assertEquals(Failure.SENSITIVE_CONTENT, stack.failure());
    }

    @Test
    void metadataAndRecordSizeAreHardBounded() {
        Map<String, String> metadata = new LinkedHashMap<>();
        for (int index = 0; index <= EnterpriseLabAllocationState.HARD_MAX_METADATA_ENTRIES; index++) {
            metadata.put("key-" + index, "value-" + index);
        }
        assertThrows(IllegalArgumentException.class,
                () -> create(draft(BASELINE, CANDIDATE, metadata)));
        assertThrows(IllegalArgumentException.class, () -> create(draft(
                BASELINE, CANDIDATE, Map.of("note", "x".repeat(257)))));
    }

    private EnterpriseLabAllocationState create(Draft draft) {
        return EnterpriseLabAllocationState.create(fixedClock(), authority, targets, draft);
    }

    private Draft draft(
            Map<String, Double> baseline,
            Map<String, Double> candidate,
            Map<String, String> metadata) {
        return new Draft(
                "allocation-tx-1",
                Optional.of("experiment-1"),
                SCENARIO,
                1,
                AllocationPurpose.EXPERIMENT_CANDIDATE,
                baseline,
                candidate,
                candidate,
                baseline,
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                TransactionPhase.PREPARED,
                new TransitionReason("INTENT_PREPARED", "allocation intent prepared"),
                false,
                Optional.empty(),
                VerificationResult.NOT_ATTEMPTED,
                RecoveryClassification.NOT_REQUIRED,
                EnterpriseLabAllocationState.GENESIS_FINGERPRINT,
                metadata);
    }

    private void assertInvalidAllocation(Map<String, Double> allocation) {
        assertThrows(IllegalArgumentException.class,
                () -> create(draft(BASELINE, allocation, Map.of())));
    }

    private static EnterpriseLabExperimentTargetCatalog targetCatalog() {
        List<EnterpriseLabLoopbackTarget> targets = new ArrayList<>();
        targets.add(new EnterpriseLabLoopbackTarget(
                SCENARIO, "blue", URI.create("http://127.0.0.1:18081/health")));
        targets.add(new EnterpriseLabLoopbackTarget(
                SCENARIO, "green", URI.create("http://127.0.0.1:18082/health")));
        targets.add(new EnterpriseLabLoopbackTarget(
                SCENARIO, "orange", URI.create("http://[::1]:18083/health")));
        return new EnterpriseLabExperimentTargetCatalog(targets);
    }

    private static Map<String, Double> reversed(Map<String, Double> values) {
        Map<String, Double> reversed = new LinkedHashMap<>();
        reversed.put("orange", values.get("orange"));
        reversed.put("green", values.get("green"));
        reversed.put("blue", values.get("blue"));
        return reversed;
    }

    private static Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    private String json(EnterpriseLabAllocationState state) {
        return new String(codec.encode(state), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
