package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.AcquisitionResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnerIdentity;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.Policy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReconciliationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReleaseResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReleaseStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.StaleClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.StaleOwnerFinding;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipCodec.CodecException;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipCodec.Failure;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabEvidenceOwnershipCodecTest {
    private static final Instant NOW = Instant.parse("2026-07-17T08:30:00.123456Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String DIRECTORY_IDENTITY = "a".repeat(64);
    private static final String LOCK_IDENTITY = "b".repeat(64);
    private static final String HOST_DIAGNOSTIC = "c".repeat(64);
    private static final OwnerIdentity OWNER = new OwnerIdentity(
            "owner-001", "instance-001", 42L, HOST_DIAGNOSTIC);
    private static final EnterpriseLabEvidenceOwnershipCodec CODEC =
            new EnterpriseLabEvidenceOwnershipCodec();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void initialRecordUsesOnlyTheInjectedClockAndBoundedPolicy() {
        OwnershipRecord record = initial();

        assertEquals(NOW, record.acquiredAt());
        assertEquals(NOW, record.lastRenewedAt());
        assertEquals(NOW.plusSeconds(30), record.leaseExpiresAt());
        assertEquals(EnterpriseLabEvidenceOwnership.INITIAL_GENERATION, record.generation());
        assertEquals(EnterpriseLabEvidenceOwnership.GENESIS_FINGERPRINT,
                record.previousOwnerFingerprint());
        assertEquals("INITIAL_ACQUISITION", record.takeoverReasonCode());
        assertEquals(OwnershipState.OWNED, record.state());
        assertEquals(ReconciliationStatus.NOT_STARTED, record.reconciliationStatus());
        assertEquals(ReleaseStatus.NOT_REQUESTED, record.releaseStatus());
    }

    @Test
    void canonicalEncodingFingerprintAndRoundTripAreStable() throws Exception {
        OwnershipRecord record = initial();
        byte[] encoded = CODEC.encode(record);
        OwnershipRecord decoded = CODEC.decode(encoded);

        assertEquals(record, decoded);
        assertArrayEquals(encoded, CODEC.encode(decoded));
        assertEquals(64, record.recordFingerprint().length());
        assertFalse(new String(encoded, StandardCharsets.UTF_8).endsWith("\n"));
        assertTrue(encoded.length < EnterpriseLabEvidenceOwnershipCodec.HARD_MAX_RECORD_BYTES);
    }

    @Test
    void equivalentEnvelopeFieldOrderDecodesAndReencodesCanonically() throws Exception {
        byte[] canonical = CODEC.encode(initial());
        ObjectNode original = (ObjectNode) MAPPER.readTree(canonical);
        List<java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields =
                new ArrayList<>();
        original.fields().forEachRemaining(fields::add);
        Collections.reverse(fields);
        ObjectNode reversed = MAPPER.createObjectNode();
        fields.forEach(entry -> reversed.set(entry.getKey(), entry.getValue()));
        byte[] nonCanonical = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(reversed);

        OwnershipRecord decoded = CODEC.decode(nonCanonical);

        assertArrayEquals(canonical, CODEC.encode(decoded));
        assertFalse(java.util.Arrays.equals(canonical, nonCanonical));
    }

    @Test
    void unsupportedUnknownDuplicateTrailingAndMissingContentFailDeliberately() throws Exception {
        ObjectNode root = encodedTree();
        root.put("schemaVersion", "enterprise-lab-evidence-owner-record/v2");
        assertFailure(Failure.UNSUPPORTED_VERSION, MAPPER.writeValueAsBytes(root));

        root = encodedTree();
        root.put("futureField", true);
        assertFailure(Failure.UNKNOWN_FIELD, MAPPER.writeValueAsBytes(root));

        root = encodedTree();
        ((ObjectNode) root.get("owner")).put("username", "not-allowed");
        assertFailure(Failure.UNKNOWN_FIELD, MAPPER.writeValueAsBytes(root));

        String encoded = new String(CODEC.encode(initial()), StandardCharsets.UTF_8);
        assertFailure(Failure.MALFORMED_RECORD,
                ("{\"generation\":1," + encoded.substring(1)).getBytes(StandardCharsets.UTF_8));
        assertFailure(Failure.MALFORMED_RECORD,
                (encoded + "{}").getBytes(StandardCharsets.UTF_8));

        root = encodedTree();
        root.remove("leaseExpiresAt");
        assertFailure(Failure.UNKNOWN_FIELD, MAPPER.writeValueAsBytes(root));
    }

    @Test
    void contentAndFingerprintModificationAreDetected() throws Exception {
        ObjectNode root = encodedTree();
        root.put("generation", 2L);
        assertFailure(Failure.FINGERPRINT_MISMATCH, MAPPER.writeValueAsBytes(root));

        root = encodedTree();
        root.put("recordFingerprint", "0".repeat(64));
        assertFailure(Failure.FINGERPRINT_MISMATCH, MAPPER.writeValueAsBytes(root));
    }

    @Test
    void malformedTypesTimestampsEnumsAndBoundsAreRejected() throws Exception {
        ObjectNode root = encodedTree();
        root.put("generation", "1");
        assertFailure(Failure.MALFORMED_RECORD, MAPPER.writeValueAsBytes(root));

        root = encodedTree();
        root.put("acquiredAt", "yesterday");
        assertFailure(Failure.MALFORMED_RECORD, MAPPER.writeValueAsBytes(root));

        root = encodedTree();
        root.put("acquiredAt", "2026-07-17T08:30:00.123456+00:00");
        assertFailure(Failure.MALFORMED_RECORD, MAPPER.writeValueAsBytes(root));

        root = encodedTree();
        root.put("state", "FORCE_UNLOCKED");
        assertFailure(Failure.MALFORMED_RECORD, MAPPER.writeValueAsBytes(root));

        assertFailure(Failure.EXCEEDED_BOUNDS,
                new byte[EnterpriseLabEvidenceOwnershipCodec.HARD_MAX_RECORD_BYTES + 1]);
        assertFailure(Failure.EXCEEDED_BOUNDS, new byte[0]);
    }

    @Test
    void ownerIdentityRejectsPathsCommandsAndUnboundedOrUnavailableProcessValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new OwnerIdentity("../owner", "instance", 1, HOST_DIAGNOSTIC));
        assertThrows(IllegalArgumentException.class,
                () -> new OwnerIdentity("owner", "java -jar app.jar", 1, HOST_DIAGNOSTIC));
        assertThrows(IllegalArgumentException.class,
                () -> new OwnerIdentity("owner", "instance", -1, HOST_DIAGNOSTIC));
        assertThrows(IllegalArgumentException.class,
                () -> new OwnerIdentity("owner", "instance", 1, "host.example"));
        assertThrows(IllegalArgumentException.class,
                () -> new OwnerIdentity("x".repeat(129), "instance", 1, HOST_DIAGNOSTIC));
    }

    @Test
    void policyRejectsUnboundedTimingAttemptsAndUnsafeRenewalRelationship() {
        Policy defaults = Policy.safetyFirstDefaults();
        assertEquals(Duration.ofSeconds(30), defaults.leaseDuration());
        assertEquals(1, defaults.acquisitionAttempts());

        assertThrows(IllegalArgumentException.class,
                () -> new Policy(Duration.ZERO, Duration.ofSeconds(1), 1, 1, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new Policy(Duration.ofSeconds(10), Duration.ofSeconds(6), 1, 1, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new Policy(Duration.ofSeconds(30), Duration.ofSeconds(10), 9, 1, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new Policy(Duration.ofMinutes(11), Duration.ofSeconds(10), 1, 1, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new Policy(Duration.ofSeconds(30), Duration.ofSeconds(10), 1, 1,
                        Duration.ofSeconds(3)));
    }

    @Test
    void generationRulesRejectRegressionGenesisReuseAndOverflow() {
        assertEquals(2L, EnterpriseLabEvidenceOwnership.nextGeneration(1L));
        assertThrows(IllegalArgumentException.class,
                () -> EnterpriseLabEvidenceOwnership.nextGeneration(0L));
        assertThrows(IllegalStateException.class,
                () -> EnterpriseLabEvidenceOwnership.nextGeneration(
                        EnterpriseLabEvidenceOwnership.MAX_GENERATION));

        assertThrows(IllegalArgumentException.class, () -> OwnershipRecord.create(
                DIRECTORY_IDENTITY, LOCK_IDENTITY, OWNER, 2L, OwnershipState.OWNED,
                NOW, NOW, NOW.plusSeconds(30),
                EnterpriseLabEvidenceOwnership.GENESIS_FINGERPRINT,
                "ABRUPT_OWNER_LOSS", 1L,
                ReconciliationStatus.NOT_STARTED, ReleaseStatus.NOT_REQUESTED));
        assertThrows(IllegalArgumentException.class, () -> OwnershipRecord.create(
                DIRECTORY_IDENTITY, LOCK_IDENTITY, OWNER, 1L, OwnershipState.OWNED,
                NOW, NOW, NOW.plusSeconds(30), "d".repeat(64),
                "INITIAL_ACQUISITION", 0L,
                ReconciliationStatus.NOT_STARTED, ReleaseStatus.NOT_REQUESTED));
    }

    @Test
    void laterGenerationPreservesPriorFingerprintAndTakeoverEvidence() {
        OwnershipRecord previous = initial();
        OwnershipRecord takeover = OwnershipRecord.create(
                DIRECTORY_IDENTITY,
                LOCK_IDENTITY,
                new OwnerIdentity("owner-002", "instance-002", 43L, HOST_DIAGNOSTIC),
                EnterpriseLabEvidenceOwnership.nextGeneration(previous.generation()),
                OwnershipState.TAKEOVER_PENDING,
                NOW.plusSeconds(31),
                NOW.plusSeconds(31),
                NOW.plusSeconds(61),
                previous.recordFingerprint(),
                "ABRUPT_OWNER_LOSS",
                1L,
                ReconciliationStatus.IN_PROGRESS,
                ReleaseStatus.NOT_REQUESTED);

        assertEquals(2L, takeover.generation());
        assertEquals(previous.recordFingerprint(), takeover.previousOwnerFingerprint());
        assertEquals(1L, takeover.takeoverSequence());
    }

    @Test
    void resultModelsCannotOverclaimSuccessfulOwnershipOrStaleDetection() {
        OwnershipRecord record = initial();
        AcquisitionResult acquired = new AcquisitionResult(
                OperationStatus.SUCCEEDED, FailureClassification.NONE,
                Optional.of(record), "OWNERSHIP_ACQUIRED");
        assertEquals(record, acquired.record().orElseThrow());

        assertThrows(IllegalArgumentException.class, () -> new AcquisitionResult(
                OperationStatus.SUCCEEDED, FailureClassification.LIVE_COMPETING_OWNER,
                Optional.of(record), "OWNERSHIP_ACQUIRED"));
        assertThrows(IllegalArgumentException.class, () -> new VerificationResult(
                OperationStatus.SUCCEEDED, FailureClassification.NONE,
                Optional.of(record), false, "OWNERSHIP_VERIFIED"));
        assertThrows(IllegalArgumentException.class, () -> new StaleOwnerFinding(
                StaleClassification.STALE_CANDIDATE, Optional.of(record), false,
                "STALE_OWNER_CANDIDATE"));
        assertThrows(IllegalArgumentException.class, () -> new StaleOwnerFinding(
                StaleClassification.LIVE_COMPETING_OWNER, Optional.of(record), true,
                "LIVE_OWNER_PRESENT"));
        assertThrows(IllegalArgumentException.class, () -> new StaleOwnerFinding(
                StaleClassification.NO_PREVIOUS_OWNER, Optional.of(record), true,
                "NO_PREVIOUS_OWNER"));
        assertThrows(IllegalArgumentException.class, () -> new StaleOwnerFinding(
                StaleClassification.CLEANLY_RELEASED, Optional.empty(), true,
                "CLEANLY_RELEASED"));

        OwnershipRecord released = OwnershipRecord.create(
                DIRECTORY_IDENTITY, LOCK_IDENTITY, OWNER, 1L, OwnershipState.RELEASED,
                NOW, NOW, NOW.plusSeconds(30), EnterpriseLabEvidenceOwnership.GENESIS_FINGERPRINT,
                "INITIAL_ACQUISITION", 0L, ReconciliationStatus.SUCCEEDED,
                ReleaseStatus.RELEASED);
        assertThrows(IllegalArgumentException.class, () -> new AcquisitionResult(
                OperationStatus.SUCCEEDED, FailureClassification.NONE,
                Optional.of(released), "OWNERSHIP_ACQUIRED"));
        assertThrows(IllegalArgumentException.class, () -> new ReleaseResult(
                OperationStatus.SUCCEEDED, FailureClassification.NONE,
                Optional.of(record), true, "OWNERSHIP_RELEASED"));
    }

    @Test
    void canonicalEvidenceContainsNoUsernameCommandLinePathOrCredentialField() {
        String encoded = new String(CODEC.encode(initial()), StandardCharsets.UTF_8);
        String lower = encoded.toLowerCase(java.util.Locale.ROOT);

        assertFalse(lower.contains("username"));
        assertFalse(lower.contains("command"));
        assertFalse(lower.contains("authorization"));
        assertFalse(lower.contains("password"));
        assertFalse(lower.contains("token"));
        assertFalse(encoded.contains("C:\\"));
        assertFalse(encoded.contains("/home/"));
    }

    private static OwnershipRecord initial() {
        return OwnershipRecord.initial(
                CLOCK, Policy.safetyFirstDefaults(), OWNER, DIRECTORY_IDENTITY, LOCK_IDENTITY);
    }

    private static ObjectNode encodedTree() throws Exception {
        return (ObjectNode) MAPPER.readTree(CODEC.encode(initial()));
    }

    private static void assertFailure(Failure expected, byte[] encoded) {
        CodecException exception = assertThrows(CodecException.class, () -> CODEC.decode(encoded));
        assertEquals(expected, exception.failure());
    }
}
