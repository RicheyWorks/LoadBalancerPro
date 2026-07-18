package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnerIdentity;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.Policy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.AcquisitionAttempt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.FailureInjector;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.FailurePoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabEvidenceOwnershipGateTest {
    private static final Instant NOW = Instant.parse("2026-07-17T23:45:00Z");
    private static final Policy POLICY = new Policy(
            Duration.ofSeconds(30), Duration.ofSeconds(10), 1, 3, Duration.ZERO);
    private static final OwnerIdentity OWNER = new OwnerIdentity(
            "owner-gate-a", "instance-gate-a", 8201L, "a".repeat(64));

    @TempDir
    Path temporaryDirectory;

    @Test
    void oneStableGateVerifiesTheLiveLockOwnerGenerationAndRecord() {
        MutableClock clock = new MutableClock(NOW);
        OwnershipFixture fixture = acquire("verify", POLICY, clock, point -> {
        });

        EnterpriseLabEvidenceOwnershipGate gate = fixture.lease().ownershipGate();
        assertSame(gate, fixture.lease().ownershipGate());
        var verified = gate.verifyCurrentOwnership();

        assertEquals(OperationStatus.SUCCEEDED, verified.status());
        assertEquals(FailureClassification.NONE, verified.failure());
        assertTrue(verified.operatingSystemLockValid());
        assertEquals(OWNER, verified.record().orElseThrow().owner());
        assertEquals(EnterpriseLabEvidenceOwnership.INITIAL_GENERATION,
                verified.record().orElseThrow().generation());
        assertEquals(fixture.lease().record(), gate.requireCurrentOwnership());
        assertEquals(fixture.lease().record(), decode(fixture.paths().recordFile()));
        assertEquals(OperationStatus.SUCCEEDED, fixture.lease().release().status());
    }

    @Test
    void explicitRenewalDurablyAdvancesTheLeaseWithoutChangingOwnerOrGeneration() {
        MutableClock clock = new MutableClock(NOW);
        OwnershipFixture fixture = acquire("renew", POLICY, clock, point -> {
        });
        OwnershipRecord initial = fixture.lease().record();
        clock.set(NOW.plusSeconds(10));

        var renewed = fixture.lease().ownershipGate().renew();
        OwnershipRecord durable = decode(fixture.paths().recordFile());

        assertEquals(OperationStatus.SUCCEEDED, renewed.status());
        assertEquals("OWNERSHIP_RENEWED", renewed.reasonCode());
        assertEquals(NOW.plusSeconds(10), durable.lastRenewedAt());
        assertEquals(NOW.plusSeconds(40), durable.leaseExpiresAt());
        assertEquals(initial.acquiredAt(), durable.acquiredAt());
        assertEquals(initial.owner(), durable.owner());
        assertEquals(initial.generation(), durable.generation());
        assertEquals(durable, renewed.record().orElseThrow());
        assertEquals(OperationStatus.SUCCEEDED,
                fixture.lease().ownershipGate().verifyCurrentOwnership().status());
        assertEquals(OperationStatus.SUCCEEDED, fixture.lease().release().status());
    }

    @Test
    void renewalAtTheAlreadyDurableInstantIsAnIdempotentNoOp() {
        MutableClock clock = new MutableClock(NOW);
        OwnershipFixture fixture = acquire("idempotent-renewal", POLICY, clock, point -> {
        });
        OwnershipRecord initial = fixture.lease().record();

        var renewed = fixture.lease().ownershipGate().renew();

        assertEquals(OperationStatus.SUCCEEDED, renewed.status());
        assertEquals("OWNERSHIP_RENEWAL_CURRENT", renewed.reasonCode());
        assertEquals(initial, renewed.record().orElseThrow());
        assertEquals(initial, decode(fixture.paths().recordFile()));
        assertEquals(OperationStatus.SUCCEEDED, fixture.lease().release().status());
    }

    @Test
    void exceededDeadlineClosesTheGateButTheLiveOsLockStillExcludesTakeover() throws Exception {
        MutableClock clock = new MutableClock(NOW);
        OwnershipFixture fixture = acquire("deadline", POLICY, clock, point -> {
        });
        clock.set(NOW.plusSeconds(31));

        var failed = fixture.lease().ownershipGate().verifyCurrentOwnership();

        assertEquals(OperationStatus.FAILED, failed.status());
        assertEquals(FailureClassification.RENEWAL_DEADLINE_EXCEEDED, failed.failure());
        assertTrue(failed.operatingSystemLockValid());
        assertTrue(fixture.lease().operatingSystemLockValid());
        try (FileChannel contender = FileChannel.open(
                fixture.paths().lockFile(),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS)) {
            assertThrows(OverlappingFileLockException.class, contender::tryLock);
        }
        assertEquals(FailureClassification.RENEWAL_DEADLINE_EXCEEDED,
                fixture.lease().ownershipGate().renew().failure());
        assertThrows(EnterpriseLabEvidenceOwnershipException.class,
                fixture.lease().ownershipGate()::requireCurrentOwnership);
        assertEquals(OperationStatus.SUCCEEDED, fixture.lease().release().status());
    }

    @Test
    void clockRegressionLatchesUncertaintyEvenAfterTheClockMovesForward() {
        MutableClock clock = new MutableClock(NOW);
        OwnershipFixture fixture = acquire("clock-regression", POLICY, clock, point -> {
        });
        clock.set(NOW.minusMillis(1));

        var failed = fixture.lease().ownershipGate().verifyCurrentOwnership();
        clock.set(NOW.plusSeconds(1));
        var repeated = fixture.lease().ownershipGate().verifyCurrentOwnership();

        assertEquals(FailureClassification.CLOCK_REGRESSION, failed.failure());
        assertEquals(FailureClassification.CLOCK_REGRESSION, repeated.failure());
        assertTrue(repeated.operatingSystemLockValid());
        assertThrows(EnterpriseLabEvidenceOwnershipException.class,
                fixture.lease().ownershipGate()::requireCurrentOwnership);
        assertEquals(OperationStatus.SUCCEEDED, fixture.lease().release().status());
    }

    @Test
    void changedOwnerRecordFailsClosedAndCannotBeReopenedByRestoringBytes() throws Exception {
        MutableClock clock = new MutableClock(NOW);
        OwnershipFixture fixture = acquire("owner-replaced", POLICY, clock, point -> {
        });
        byte[] original = Files.readAllBytes(fixture.paths().recordFile());
        OwnershipRecord current = fixture.lease().record();
        OwnershipRecord replacement = OwnershipRecord.create(
                current.directoryIdentity(), current.lockFileIdentity(),
                new OwnerIdentity("owner-gate-b", "instance-gate-b", 8202L,
                        "b".repeat(64)),
                current.generation(), current.state(), current.acquiredAt(),
                current.lastRenewedAt(), current.leaseExpiresAt(),
                current.previousOwnerFingerprint(), current.takeoverReasonCode(),
                current.takeoverSequence(), current.reconciliationStatus(),
                current.releaseStatus());
        overwrite(fixture.paths().recordFile(), new EnterpriseLabEvidenceOwnershipCodec()
                .encode(replacement));

        var failed = fixture.lease().ownershipGate().verifyCurrentOwnership();
        overwrite(fixture.paths().recordFile(), original);
        var repeated = fixture.lease().ownershipGate().verifyCurrentOwnership();

        assertEquals(FailureClassification.RECORD_REPLACED, failed.failure());
        assertEquals(FailureClassification.RECORD_REPLACED, repeated.failure());
        assertTrue(repeated.operatingSystemLockValid());
        assertThrows(EnterpriseLabEvidenceOwnershipException.class,
                fixture.lease().ownershipGate()::requireCurrentOwnership);
        assertEquals(OperationStatus.SUCCEEDED, fixture.lease().release().status());
    }

    @Test
    void changedGenerationIsRejectedWhileTheOriginalLockRemainsHeld() throws Exception {
        MutableClock clock = new MutableClock(NOW);
        OwnershipFixture fixture = acquire("generation-changed", POLICY, clock, point -> {
        });
        OwnershipRecord current = fixture.lease().record();
        OwnershipRecord replacement = OwnershipRecord.create(
                current.directoryIdentity(), current.lockFileIdentity(), current.owner(),
                EnterpriseLabEvidenceOwnership.nextGeneration(current.generation()),
                OwnershipState.OWNED, current.acquiredAt(), current.lastRenewedAt(),
                current.leaseExpiresAt(), current.recordFingerprint(), "TEST_TAKEOVER",
                1L, current.reconciliationStatus(), current.releaseStatus());
        overwrite(fixture.paths().recordFile(), new EnterpriseLabEvidenceOwnershipCodec()
                .encode(replacement));

        var failed = fixture.lease().ownershipGate().verifyCurrentOwnership();

        assertEquals(FailureClassification.RECORD_REPLACED, failed.failure());
        assertTrue(failed.operatingSystemLockValid());
        assertEquals(OperationStatus.FAILED, fixture.lease().release().status());
        assertFalse(fixture.lease().operatingSystemLockValid());
    }

    @Test
    void corruptRecordFingerprintIsClassifiedAndTheGateStaysClosed() throws Exception {
        MutableClock clock = new MutableClock(NOW);
        OwnershipFixture fixture = acquire("fingerprint-corrupt", POLICY, clock, point -> {
        });
        String changed = Files.readString(fixture.paths().recordFile())
                .replace("owner-gate-a", "owner-gate-z");
        overwrite(fixture.paths().recordFile(),
                changed.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        var failed = fixture.lease().ownershipGate().verifyCurrentOwnership();

        assertEquals(FailureClassification.RECORD_FINGERPRINT_MISMATCH, failed.failure());
        assertTrue(failed.operatingSystemLockValid());
        assertEquals(FailureClassification.RECORD_FINGERPRINT_MISMATCH,
                fixture.lease().ownershipGate().renew().failure());
        assertEquals(OperationStatus.FAILED, fixture.lease().release().status());
        assertFalse(fixture.lease().operatingSystemLockValid());
    }

    @Test
    void missingOwnerRecordFailsVerificationWhileTheOsLockRemainsAuthoritative() throws Exception {
        MutableClock clock = new MutableClock(NOW);
        OwnershipFixture fixture = acquire("record-missing", POLICY, clock, point -> {
        });
        Files.delete(fixture.paths().recordFile());

        var failed = fixture.lease().ownershipGate().verifyCurrentOwnership();

        assertEquals(FailureClassification.RECORD_REPLACED, failed.failure());
        assertTrue(failed.operatingSystemLockValid());
        assertEquals(FailureClassification.RECORD_REPLACED,
                fixture.lease().ownershipGate().renew().failure());
        assertEquals(OperationStatus.FAILED, fixture.lease().release().status());
        assertFalse(fixture.lease().operatingSystemLockValid());
    }

    @Test
    void unexpectedLockChannelClosureIsDetectedAndPermanentlyClosesTheGate() throws Exception {
        MutableClock clock = new MutableClock(NOW);
        OwnershipFixture fixture = acquire("lock-channel-closed", POLICY, clock, point -> {
        });
        closeLockChannel(fixture.lease());

        var failed = fixture.lease().ownershipGate().verifyCurrentOwnership();

        assertEquals(FailureClassification.LOCK_LOST, failed.failure());
        assertFalse(failed.operatingSystemLockValid());
        assertEquals(FailureClassification.LOCK_LOST,
                fixture.lease().ownershipGate().renew().failure());
        assertThrows(EnterpriseLabEvidenceOwnershipException.class,
                fixture.lease().ownershipGate()::requireCurrentOwnership);
        assertEquals(FailureClassification.LOCK_LOST, fixture.lease().release().failure());
        assertFalse(fixture.lease().operatingSystemLockValid());
    }

    @Test
    void replacedDirectoryIdentityClosesVerificationAndRelease() throws Exception {
        MutableClock clock = new MutableClock(NOW);
        OwnershipFixture fixture = acquire("directory-replaced", POLICY, clock, point -> {
        });
        overwrite(fixture.paths().directoryIdentityFile(),
                ("f".repeat(64) + "\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));

        var failed = fixture.lease().ownershipGate().verifyCurrentOwnership();

        assertEquals(FailureClassification.DIRECTORY_IDENTITY_MISMATCH, failed.failure());
        assertTrue(failed.operatingSystemLockValid());
        assertEquals(FailureClassification.DIRECTORY_IDENTITY_MISMATCH,
                fixture.lease().release().failure());
        assertFalse(fixture.lease().operatingSystemLockValid());
    }

    @Test
    void transientRenewalPreflightFailuresUseOnlyTheBoundedAttemptCount() {
        MutableClock clock = new MutableClock(NOW);
        AtomicInteger attempts = new AtomicInteger();
        OwnershipFixture fixture = acquire("retry-success", POLICY, clock, point -> {
            if (point == FailurePoint.BEFORE_RENEWAL_ATTEMPT
                    && attempts.incrementAndGet() < POLICY.renewalAttempts()) {
                throw new IOException("transient renewal preflight");
            }
        });
        clock.set(NOW.plusSeconds(10));

        var renewed = fixture.lease().ownershipGate().renew();

        assertEquals(OperationStatus.SUCCEEDED, renewed.status());
        assertEquals(POLICY.renewalAttempts(), attempts.get());
        assertEquals(NOW.plusSeconds(10), renewed.record().orElseThrow().lastRenewedAt());
        assertEquals(OperationStatus.SUCCEEDED, fixture.lease().release().status());
    }

    @Test
    void exhaustedRenewalAttemptsLatchFailureWithoutChangingDurableEvidence() {
        MutableClock clock = new MutableClock(NOW);
        AtomicInteger attempts = new AtomicInteger();
        OwnershipFixture fixture = acquire("retry-exhausted", POLICY, clock, point -> {
            if (point == FailurePoint.BEFORE_RENEWAL_ATTEMPT) {
                attempts.incrementAndGet();
                throw new IOException("persistent renewal preflight");
            }
        });
        clock.set(NOW.plusSeconds(10));
        OwnershipRecord initial = fixture.lease().record();

        var failed = fixture.lease().ownershipGate().renew();

        assertEquals(OperationStatus.FAILED, failed.status());
        assertEquals(FailureClassification.IO_FAILURE, failed.failure());
        assertEquals(POLICY.renewalAttempts(), attempts.get());
        assertEquals(initial, decode(fixture.paths().recordFile()));
        assertEquals(FailureClassification.IO_FAILURE,
                fixture.lease().ownershipGate().verifyCurrentOwnership().failure());
        assertEquals(OperationStatus.SUCCEEDED, fixture.lease().release().status());
    }

    @Test
    void renewalFailuresBeforeInstallPreserveTemporaryEvidenceAndCloseAdmission() {
        for (FailurePoint failurePoint : Set.of(
                FailurePoint.DURING_RENEWAL_RECORD_WRITE,
                FailurePoint.AFTER_RENEWAL_RECORD_FORCE)) {
            MutableClock clock = new MutableClock(NOW);
            OwnershipFixture fixture = acquire(
                    "write-failure-" + failurePoint.name(), POLICY, clock, point -> {
                        if (point == failurePoint) {
                            throw new IOException("renewal publication failed at " + point);
                        }
                    });
            clock.set(NOW.plusSeconds(10));
            OwnershipRecord initial = fixture.lease().record();

            var failed = fixture.lease().ownershipGate().renew();

            assertEquals(FailureClassification.IO_FAILURE, failed.failure(), failurePoint.name());
            assertEquals(initial, decode(fixture.paths().recordFile()), failurePoint.name());
            assertTrue(Files.isRegularFile(
                    fixture.paths().temporaryRecordFile(), LinkOption.NOFOLLOW_LINKS),
                    failurePoint.name());
            assertTrue(fixture.lease().operatingSystemLockValid(), failurePoint.name());
            assertEquals(FailureClassification.IO_FAILURE,
                    fixture.lease().ownershipGate().verifyCurrentOwnership().failure(),
                    failurePoint.name());
            assertEquals(OperationStatus.FAILED,
                    fixture.lease().release().status(), failurePoint.name());
            assertFalse(fixture.lease().operatingSystemLockValid(), failurePoint.name());
        }
    }

    @Test
    void installedRenewalIsRecoveredWhenStatusPublicationFailsAfterAtomicInstall() {
        MutableClock clock = new MutableClock(NOW);
        OwnershipFixture fixture = acquire("installed-before-status", POLICY, clock, point -> {
            if (point == FailurePoint.AFTER_RENEWAL_RECORD_INSTALL) {
                throw new IOException("renewal installed before status");
            }
        });
        clock.set(NOW.plusSeconds(10));

        var renewed = fixture.lease().ownershipGate().renew();

        assertEquals(OperationStatus.SUCCEEDED, renewed.status());
        assertEquals(NOW.plusSeconds(10), renewed.record().orElseThrow().lastRenewedAt());
        assertEquals(renewed.record().orElseThrow(), decode(fixture.paths().recordFile()));
        assertFalse(Files.exists(
                fixture.paths().temporaryRecordFile(), LinkOption.NOFOLLOW_LINKS));
        assertEquals(OperationStatus.SUCCEEDED, fixture.lease().release().status());
    }

    @Test
    void releasePreservesTheLastVerifiedRenewalAndPermanentlyClosesTheGate() {
        MutableClock clock = new MutableClock(NOW);
        OwnershipFixture fixture = acquire("release-after-renewal", POLICY, clock, point -> {
        });
        EnterpriseLabEvidenceOwnershipGate gate = fixture.lease().ownershipGate();
        clock.set(NOW.plusSeconds(10));
        OwnershipRecord renewed = gate.renew().record().orElseThrow();

        var released = fixture.lease().release();
        var afterRelease = gate.verifyCurrentOwnership();

        assertEquals(OperationStatus.SUCCEEDED, released.status());
        assertEquals(OwnershipState.RELEASED, released.record().orElseThrow().state());
        assertEquals(renewed.lastRenewedAt(), released.record().orElseThrow().lastRenewedAt());
        assertEquals(released.record().orElseThrow(), decode(fixture.paths().recordFile()));
        assertEquals(FailureClassification.LOCK_LOST, afterRelease.failure());
        assertFalse(afterRelease.operatingSystemLockValid());
        assertThrows(EnterpriseLabEvidenceOwnershipException.class,
                gate::requireCurrentOwnership);
    }

    private OwnershipFixture acquire(
            String name,
            Policy policy,
            MutableClock clock,
            FailureInjector failureInjector) {
        EnterpriseLabEvidenceOwnershipPaths paths = EnterpriseLabEvidenceOwnershipPaths.create(
                directory(name));
        AcquisitionAttempt attempt = EnterpriseLabEvidenceOwnershipManager.acquire(
                paths, policy, clock, OWNER, failureInjector, FileChannel::tryLock);
        assertEquals(OperationStatus.SUCCEEDED, attempt.result().status());
        return new OwnershipFixture(paths, attempt.ownership().orElseThrow());
    }

    private Path directory(String name) {
        try {
            return Files.createDirectory(temporaryDirectory.resolve(name))
                    .toAbsolutePath().normalize();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static OwnershipRecord decode(Path path) {
        try {
            return new EnterpriseLabEvidenceOwnershipCodec().decode(Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void overwrite(Path path, byte[] bytes) throws IOException {
        Files.write(path, bytes,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void closeLockChannel(EnterpriseLabEvidenceOwnershipLease lease)
            throws ReflectiveOperationException, IOException {
        Field field = EnterpriseLabEvidenceOwnershipLease.class.getDeclaredField("lockChannel");
        field.setAccessible(true);
        ((FileChannel) field.get(lease)).close();
    }

    private record OwnershipFixture(
            EnterpriseLabEvidenceOwnershipPaths paths,
            EnterpriseLabEvidenceOwnershipLease lease) {
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant value) {
            instant = value;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (!ZoneOffset.UTC.equals(zone)) {
                throw new IllegalArgumentException("test clock supports UTC only");
            }
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
