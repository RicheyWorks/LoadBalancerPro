package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnerIdentity;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.Policy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReconciliationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReleaseStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.StaleClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.AcquisitionAttempt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.FailurePoint;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.TakeoverAttempt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.AllocationInspection;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.AllocationRecoveryPort;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.BaselineRestorationReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabEvidenceOwnershipTakeoverTest {
    private static final Instant NOW = Instant.parse("2026-07-18T04:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Policy POLICY = new Policy(
            Duration.ofSeconds(30), Duration.ofSeconds(10), 1, 2, Duration.ZERO);
    private static final OwnerIdentity FIRST_OWNER = new OwnerIdentity(
            "owner-first", "instance-first", 8101L, "a".repeat(64));
    private static final OwnerIdentity NEXT_OWNER = new OwnerIdentity(
            "owner-next", "instance-next", 8102L, "b".repeat(64));

    @TempDir
    Path temporaryDirectory;

    @Test
    void cleanReleaseTakeoverArchivesPriorReconcilesAndPublishesHigherGeneration() {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("clean-release");
        EnterpriseLabEvidenceOwnershipLease first = acquire(paths, FIRST_OWNER)
                .ownership().orElseThrow();
        OwnershipRecord released = first.release().record().orElseThrow();

        TakeoverAttempt attempt = takeover(paths, NEXT_OWNER, reconciler(paths));

        assertEquals(OperationStatus.SUCCEEDED, attempt.result().status());
        assertEquals(StaleClassification.CLEANLY_RELEASED,
                attempt.staleOwnerFinding().orElseThrow().classification());
        OwnershipRecord complete = attempt.result().record().orElseThrow();
        assertEquals(released.generation() + 1L, complete.generation());
        assertEquals(released.recordFingerprint(), complete.previousOwnerFingerprint());
        assertEquals(OwnershipState.TAKEOVER_COMPLETE, complete.state());
        assertEquals(ReconciliationStatus.SUCCEEDED, complete.reconciliationStatus());
        assertEquals(released, decode(paths.historyRecordFile(released)));
        EnterpriseLabEvidenceOwnershipLease ownership = attempt.ownership().orElseThrow();
        assertEquals(OperationStatus.SUCCEEDED,
                ownership.ownershipGate().verifyCurrentOwnership().status());
        assertEquals(OwnershipState.TAKEOVER_COMPLETE,
                ownership.ownershipGate().renew().record().orElseThrow().state());
        assertEquals(OwnershipState.RELEASED,
                ownership.release().record().orElseThrow().state());
    }

    @Test
    void expiredAbruptOwnerBecomesStaleOnlyAfterTheOsLockCanBeAcquired() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("expired-abrupt");
        OwnershipRecord abrupt = installPrior(paths, Clock.fixed(
                NOW.minusSeconds(90), ZoneOffset.UTC), OwnershipState.OWNED,
                1L, 0L, ReconciliationStatus.NOT_STARTED);

        TakeoverAttempt attempt = takeover(paths, NEXT_OWNER, reconciler(paths));

        assertEquals(OperationStatus.SUCCEEDED, attempt.result().status());
        assertEquals(StaleClassification.STALE_CANDIDATE,
                attempt.staleOwnerFinding().orElseThrow().classification());
        assertEquals(abrupt.generation() + 1L,
                attempt.result().record().orElseThrow().generation());
        attempt.ownership().orElseThrow().release();
    }

    @Test
    void unexpiredActiveLookingRecordIsPreservedAndRefusedEvenWithoutALockHolder()
            throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("unexpired-abrupt");
        OwnershipRecord prior = installPrior(paths, CLOCK, OwnershipState.OWNED,
                1L, 0L, ReconciliationStatus.NOT_STARTED);

        TakeoverAttempt attempt = takeover(paths, NEXT_OWNER, reconciler(paths));

        assertEquals(OperationStatus.REFUSED, attempt.result().status());
        assertEquals(FailureClassification.TAKEOVER_NOT_PERMITTED,
                attempt.result().failure());
        assertEquals(StaleClassification.ACTIVE_LOOKING_WITHOUT_LOCK,
                attempt.staleOwnerFinding().orElseThrow().classification());
        assertEquals(prior, decode(paths.recordFile()));
        assertFalse(Files.exists(paths.historyRecordFile(prior)));
        assertLockAvailable(paths);
    }

    @Test
    void liveLockAlwaysWinsOverAnExpiredTimestamp() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("live-expired");
        installPrior(paths, Clock.fixed(NOW.minusSeconds(90), ZoneOffset.UTC),
                OwnershipState.OWNED, 1L, 0L, ReconciliationStatus.NOT_STARTED);
        try (FileChannel channel = paths.openLockChannel(); FileLock ignored = channel.tryLock()) {
            TakeoverAttempt attempt = takeover(paths, NEXT_OWNER, reconciler(paths));

            assertEquals(OperationStatus.REFUSED, attempt.result().status());
            assertEquals(FailureClassification.DUPLICATE_ACQUISITION,
                    attempt.result().failure());
            assertEquals(StaleClassification.LIVE_COMPETING_OWNER,
                    attempt.staleOwnerFinding().orElseThrow().classification());
            assertTrue(attempt.ownership().isEmpty());
        }
    }

    @Test
    void malformedPriorIsClassifiedPreservedAndNeverAssignedAGuessedGeneration()
            throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("malformed");
        try (FileChannel ignored = paths.openLockChannel()) {
            // Create the controlled lock before installing malformed prior evidence.
        }
        Files.writeString(paths.recordFile(), "{\"truncated\":", StandardOpenOption.CREATE_NEW);

        TakeoverAttempt attempt = takeover(paths, NEXT_OWNER, reconciler(paths));

        assertEquals(OperationStatus.FAILED, attempt.result().status());
        assertEquals(FailureClassification.RECORD_MALFORMED, attempt.result().failure());
        assertEquals(StaleClassification.MALFORMED_RECORD,
                attempt.staleOwnerFinding().orElseThrow().classification());
        assertEquals("{\"truncated\":", Files.readString(paths.recordFile()));
        assertTrue(attempt.ownership().isEmpty());
    }

    @Test
    void unsupportedAndFingerprintMismatchedRecordsRemainUntouchedAndClassified()
            throws Exception {
        for (boolean unsupportedVersion : java.util.List.of(true, false)) {
            EnterpriseLabEvidenceOwnershipPaths paths = paths(
                    unsupportedVersion ? "unsupported" : "fingerprint-mismatch");
            OwnershipRecord prior = initialFor(paths, Clock.fixed(
                    NOW.minusSeconds(90), ZoneOffset.UTC));
            String canonical = new String(
                    new EnterpriseLabEvidenceOwnershipCodec().encode(prior),
                    StandardCharsets.UTF_8);
            String invalid = unsupportedVersion
                    ? canonical.replace(
                            EnterpriseLabEvidenceOwnership.RECORD_SCHEMA_VERSION,
                            "enterprise-lab-evidence-owner-record/v2")
                    : canonical.replace(prior.recordFingerprint(), "f".repeat(64));
            Files.writeString(paths.recordFile(), invalid, StandardOpenOption.CREATE_NEW);

            TakeoverAttempt attempt = takeover(paths, NEXT_OWNER, reconciler(paths));

            assertEquals(OperationStatus.FAILED, attempt.result().status());
            assertEquals(unsupportedVersion
                            ? FailureClassification.UNSUPPORTED_RECORD_VERSION
                            : FailureClassification.RECORD_FINGERPRINT_MISMATCH,
                    attempt.result().failure());
            assertEquals(unsupportedVersion
                            ? StaleClassification.UNSUPPORTED_RECORD
                            : StaleClassification.FINGERPRINT_MISMATCH,
                    attempt.staleOwnerFinding().orElseThrow().classification());
            assertEquals(invalid, Files.readString(paths.recordFile()));
            assertTrue(attempt.ownership().isEmpty());
        }
    }

    @Test
    void mismatchedDirectoryIdentityIsReportedWithDecodedPriorEvidence() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("directory-mismatch");
        EnterpriseLabEvidenceOwnershipPaths other = paths("other-directory");
        OwnershipRecord currentIdentity = initialFor(paths, Clock.fixed(
                NOW.minusSeconds(90), ZoneOffset.UTC));
        OwnershipRecord foreign = OwnershipRecord.create(
                other.directoryIdentity(),
                currentIdentity.lockFileIdentity(),
                currentIdentity.owner(),
                currentIdentity.generation(),
                currentIdentity.state(),
                currentIdentity.acquiredAt(),
                currentIdentity.lastRenewedAt(),
                currentIdentity.leaseExpiresAt(),
                currentIdentity.previousOwnerFingerprint(),
                currentIdentity.takeoverReasonCode(),
                currentIdentity.takeoverSequence(),
                currentIdentity.reconciliationStatus(),
                currentIdentity.releaseStatus());
        Files.write(paths.recordFile(),
                new EnterpriseLabEvidenceOwnershipCodec().encode(foreign),
                StandardOpenOption.CREATE_NEW);

        TakeoverAttempt attempt = takeover(paths, NEXT_OWNER, reconciler(paths));

        assertEquals(OperationStatus.FAILED, attempt.result().status());
        assertEquals(FailureClassification.DIRECTORY_IDENTITY_MISMATCH,
                attempt.result().failure());
        assertEquals(foreign, attempt.result().record().orElseThrow());
        assertEquals(StaleClassification.DIRECTORY_IDENTITY_MISMATCH,
                attempt.staleOwnerFinding().orElseThrow().classification());
        assertEquals(foreign, decode(paths.recordFile()));
    }

    @Test
    void localClockRegressionCannotMakeAnOwnerLookStale() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("clock-regression");
        OwnershipRecord prior = installPrior(paths, Clock.fixed(
                NOW.plusSeconds(1), ZoneOffset.UTC), OwnershipState.OWNED,
                1L, 0L, ReconciliationStatus.NOT_STARTED);

        TakeoverAttempt attempt = takeover(paths, NEXT_OWNER, reconciler(paths));

        assertEquals(OperationStatus.FAILED, attempt.result().status());
        assertEquals(FailureClassification.CLOCK_REGRESSION, attempt.result().failure());
        assertEquals(StaleClassification.TIMESTAMP_INVALID,
                attempt.staleOwnerFinding().orElseThrow().classification());
        assertEquals(prior, decode(paths.recordFile()));
    }

    @Test
    void generationOverflowFailsWithoutArchivingOrReplacingThePrior() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("generation-overflow");
        OwnershipRecord prior = installPrior(paths, Clock.fixed(
                        NOW.minusSeconds(90), ZoneOffset.UTC), OwnershipState.OWNED,
                EnterpriseLabEvidenceOwnership.MAX_GENERATION, 1L,
                ReconciliationStatus.NOT_STARTED);

        TakeoverAttempt attempt = takeover(paths, NEXT_OWNER, reconciler(paths));

        assertEquals(OperationStatus.FAILED, attempt.result().status());
        assertEquals(FailureClassification.GENERATION_EXHAUSTED,
                attempt.result().failure());
        assertEquals(StaleClassification.GENERATION_INVALID,
                attempt.staleOwnerFinding().orElseThrow().classification());
        assertEquals(prior, decode(paths.recordFile()));
        assertFalse(Files.exists(paths.historyRecordFile(prior)));
    }

    @Test
    void failureBeforeReconciliationLeavesDurablePendingEvidenceButNoCapability()
            throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("before-reconciliation-failure");
        OwnershipRecord prior = installPrior(paths, Clock.fixed(
                NOW.minusSeconds(90), ZoneOffset.UTC), OwnershipState.OWNED,
                1L, 0L, ReconciliationStatus.NOT_STARTED);

        TakeoverAttempt attempt = EnterpriseLabEvidenceOwnershipManager.takeover(
                paths, POLICY, CLOCK, NEXT_OWNER, reconciler(paths), point -> {
                    if (point == FailurePoint.BEFORE_TAKEOVER_RECONCILIATION) {
                        throw new IOException("injected before reconciliation");
                    }
                }, FileChannel::tryLock);

        assertEquals(OperationStatus.FAILED, attempt.result().status());
        assertEquals(FailureClassification.IO_FAILURE, attempt.result().failure());
        assertEquals(ReconciliationStatus.IN_PROGRESS,
                attempt.result().reconciliationStatus());
        assertTrue(attempt.ownership().isEmpty());
        OwnershipRecord pending = decode(paths.recordFile());
        assertEquals(OwnershipState.TAKEOVER_PENDING, pending.state());
        assertEquals(prior.generation() + 1L, pending.generation());
        assertEquals(prior, decode(paths.historyRecordFile(prior)));
        assertLockAvailable(paths);
    }

    @Test
    void unsafeReconciliationPublishesFailedTakeoverAndNeverPublishesCapability()
            throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("unsafe-reconciliation");
        OwnershipRecord prior = installPrior(paths, Clock.fixed(
                NOW.minusSeconds(90), ZoneOffset.UTC), OwnershipState.OWNED,
                1L, 0L, ReconciliationStatus.NOT_STARTED);
        EnterpriseLabExperimentStartupReconciler reconciler = reconciler(paths);
        Path journals = paths.trustedRoot()
                .resolve(EnterpriseLabExperimentJournalDirectory.NAMESPACE)
                .resolve("journals");
        Files.writeString(journals.resolve("unexpected-entry"), "preserve-me",
                StandardOpenOption.CREATE_NEW);

        TakeoverAttempt attempt = takeover(paths, NEXT_OWNER, reconciler);

        assertEquals(OperationStatus.FAILED, attempt.result().status());
        assertEquals(FailureClassification.RECONCILIATION_FAILED,
                attempt.result().failure());
        assertEquals(ReconciliationStatus.FAILED,
                attempt.result().reconciliationStatus());
        assertTrue(attempt.ownership().isEmpty());
        OwnershipRecord failed = attempt.result().record().orElseThrow();
        assertEquals(OwnershipState.FAILED, failed.state());
        assertEquals(prior.generation() + 1L, failed.generation());
        assertEquals(ReconciliationStatus.FAILED, failed.reconciliationStatus());
        assertEquals("preserve-me", Files.readString(journals.resolve("unexpected-entry")));
        assertLockAvailable(paths);
    }

    @Test
    void postInstallHistoryAndRecordFailuresRecoverOnlyAfterExactReadback()
            throws Exception {
        for (FailurePoint injected : java.util.List.of(
                FailurePoint.AFTER_TAKEOVER_HISTORY_INSTALL,
                FailurePoint.AFTER_TAKEOVER_RECORD_INSTALL)) {
            EnterpriseLabEvidenceOwnershipPaths paths = paths("recover-" + injected.name());
            OwnershipRecord prior = installPrior(paths, Clock.fixed(
                    NOW.minusSeconds(90), ZoneOffset.UTC), OwnershipState.OWNED,
                    1L, 0L, ReconciliationStatus.NOT_STARTED);

            TakeoverAttempt attempt = EnterpriseLabEvidenceOwnershipManager.takeover(
                    paths, POLICY, CLOCK, NEXT_OWNER, reconciler(paths), point -> {
                        if (point == injected) {
                            throw new IOException("injected " + injected);
                        }
                    }, FileChannel::tryLock);

            assertEquals(OperationStatus.SUCCEEDED, attempt.result().status(), injected.name());
            assertEquals(prior, decode(paths.historyRecordFile(prior)), injected.name());
            assertEquals(OwnershipState.TAKEOVER_COMPLETE,
                    decode(paths.recordFile()).state(), injected.name());
            attempt.ownership().orElseThrow().release();
        }
    }

    @Test
    void retryRecoversBoundedNonAuthoritativeTemporariesAfterInterruptedWrites()
            throws Exception {
        for (FailurePoint injected : java.util.List.of(
                FailurePoint.DURING_TAKEOVER_HISTORY_WRITE,
                FailurePoint.AFTER_TAKEOVER_HISTORY_FORCE,
                FailurePoint.DURING_TAKEOVER_RECORD_WRITE,
                FailurePoint.AFTER_TAKEOVER_RECORD_FORCE)) {
            EnterpriseLabEvidenceOwnershipPaths paths = paths("retry-" + injected.name());
            OwnershipRecord prior = installPrior(paths, Clock.fixed(
                    NOW.minusSeconds(90), ZoneOffset.UTC), OwnershipState.OWNED,
                    1L, 0L, ReconciliationStatus.NOT_STARTED);
            TakeoverAttempt interrupted = EnterpriseLabEvidenceOwnershipManager.takeover(
                    paths, POLICY, CLOCK, NEXT_OWNER, reconciler(paths), point -> {
                        if (point == injected) {
                            throw new IOException("injected " + injected);
                        }
                    }, FileChannel::tryLock);
            assertEquals(OperationStatus.FAILED, interrupted.result().status(), injected.name());
            assertEquals(prior, decode(paths.recordFile()), injected.name());
            assertTrue(Files.exists(paths.temporaryRecordFile())
                    || Files.exists(paths.historyTemporaryRecordFile(prior)), injected.name());

            TakeoverAttempt recovered = takeover(paths, NEXT_OWNER, reconciler(paths));

            assertEquals(OperationStatus.SUCCEEDED, recovered.result().status(), injected.name());
            assertFalse(Files.exists(paths.temporaryRecordFile()), injected.name());
            assertFalse(Files.exists(paths.historyTemporaryRecordFile(prior)), injected.name());
            assertEquals(prior, decode(paths.historyRecordFile(prior)), injected.name());
            recovered.ownership().orElseThrow().release();
        }
    }

    @Test
    void incompleteTakeoverCanBeRecoveredIntoAnotherMonotonicGeneration() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("incomplete-takeover");
        OwnershipRecord incomplete = installPrior(paths, Clock.fixed(
                        NOW.minusSeconds(90), ZoneOffset.UTC), OwnershipState.TAKEOVER_PENDING,
                2L, 1L, ReconciliationStatus.IN_PROGRESS);

        TakeoverAttempt attempt = takeover(paths, NEXT_OWNER, reconciler(paths));

        assertEquals(OperationStatus.SUCCEEDED, attempt.result().status());
        assertEquals(StaleClassification.TAKEOVER_INCOMPLETE,
                attempt.staleOwnerFinding().orElseThrow().classification());
        assertEquals(3L, attempt.result().record().orElseThrow().generation());
        assertEquals(2L, attempt.result().record().orElseThrow().takeoverSequence());
        assertEquals(incomplete, decode(paths.historyRecordFile(incomplete)));
        attempt.ownership().orElseThrow().release();
    }

    @Test
    void competingTakeoverCallsPublishExactlyOneLiveOwner() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("competing-takeover");
        EnterpriseLabEvidenceOwnershipLease first = acquire(paths, FIRST_OWNER)
                .ownership().orElseThrow();
        first.release();
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> {
                start.await();
                return takeover(paths, NEXT_OWNER, reconciler(paths));
            });
            var b = executor.submit(() -> {
                start.await();
                return takeover(paths, new OwnerIdentity(
                        "owner-third", "instance-third", 8103L, "c".repeat(64)),
                        reconciler(paths));
            });
            start.countDown();
            TakeoverAttempt firstAttempt = a.get(10, TimeUnit.SECONDS);
            TakeoverAttempt secondAttempt = b.get(10, TimeUnit.SECONDS);
            long successes = java.util.stream.Stream.of(firstAttempt, secondAttempt)
                    .filter(candidate -> candidate.result().status() == OperationStatus.SUCCEEDED)
                    .count();
            assertEquals(1L, successes);
            TakeoverAttempt winner = firstAttempt.ownership().isPresent()
                    ? firstAttempt : secondAttempt;
            TakeoverAttempt loser = firstAttempt.ownership().isEmpty()
                    ? firstAttempt : secondAttempt;
            assertTrue(loser.result().failure() == FailureClassification.DUPLICATE_ACQUISITION
                    || loser.result().failure() == FailureClassification.LIVE_COMPETING_OWNER);
            winner.ownership().orElseThrow().release();
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private AcquisitionAttempt acquire(
            EnterpriseLabEvidenceOwnershipPaths paths,
            OwnerIdentity owner) {
        return EnterpriseLabEvidenceOwnershipManager.acquire(
                paths, POLICY, CLOCK, owner, point -> { }, FileChannel::tryLock);
    }

    private TakeoverAttempt takeover(
            EnterpriseLabEvidenceOwnershipPaths paths,
            OwnerIdentity owner,
            EnterpriseLabExperimentStartupReconciler reconciler) {
        return EnterpriseLabEvidenceOwnershipManager.takeover(
                paths, POLICY, CLOCK, owner, reconciler,
                point -> { }, FileChannel::tryLock);
    }

    private EnterpriseLabExperimentStartupReconciler reconciler(
            EnterpriseLabEvidenceOwnershipPaths paths) {
        return new EnterpriseLabExperimentStartupReconciler(
                EnterpriseLabMutationTestAuthority.ownedDirectory(paths.trustedRoot()),
                new AllocationRecoveryPort() {
                    @Override
                    public AllocationInspection inspect(ReconstructedExperimentState state) {
                        throw new AssertionError("empty journal set must not inspect allocation");
                    }

                    @Override
                    public BaselineRestorationReceipt restoreBaseline(
                            ReconstructedExperimentState state,
                            String reason) {
                        throw new AssertionError("empty journal set must not restore allocation");
                    }
                },
                EnterpriseLabExperimentRecoveryGate.pending(),
                CLOCK);
    }

    private OwnershipRecord installPrior(
            EnterpriseLabEvidenceOwnershipPaths paths,
            Clock recordClock,
            OwnershipState state,
            long generation,
            long takeoverSequence,
            ReconciliationStatus reconciliationStatus) throws Exception {
        OwnershipRecord initial = initialFor(paths, recordClock);
        OwnershipRecord record = generation == EnterpriseLabEvidenceOwnership.INITIAL_GENERATION
                && state == OwnershipState.OWNED
                ? initial
                : OwnershipRecord.create(
                        initial.directoryIdentity(),
                        initial.lockFileIdentity(),
                        FIRST_OWNER,
                        generation,
                        state,
                        initial.acquiredAt(),
                        initial.lastRenewedAt(),
                        initial.leaseExpiresAt(),
                        "d".repeat(64),
                        "PRIOR_TAKEOVER",
                        takeoverSequence,
                        reconciliationStatus,
                        ReleaseStatus.NOT_REQUESTED);
        return new EnterpriseLabEvidenceOwnershipRecordStore(
                paths, new EnterpriseLabEvidenceOwnershipCodec(), point -> { })
                .writeNewAndVerify(record);
    }

    private OwnershipRecord initialFor(
            EnterpriseLabEvidenceOwnershipPaths paths,
            Clock recordClock) throws Exception {
        String lockIdentity;
        try (FileChannel ignored = paths.openLockChannel()) {
            lockIdentity = paths.identityOfControlledRegularFile(paths.lockFile());
        }
        return OwnershipRecord.initial(
                recordClock, POLICY, FIRST_OWNER, paths.directoryIdentity(), lockIdentity);
    }

    private EnterpriseLabEvidenceOwnershipPaths paths(String name) {
        try {
            Path root = Files.createDirectory(temporaryDirectory.resolve(name))
                    .toAbsolutePath().normalize();
            return EnterpriseLabEvidenceOwnershipPaths.create(root);
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

    private static void assertLockAvailable(EnterpriseLabEvidenceOwnershipPaths paths)
            throws Exception {
        try (FileChannel channel = paths.openLockChannel(); FileLock lock = channel.tryLock()) {
            assertNotNull(lock);
        }
    }
}
