package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.AcquisitionResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnerIdentity;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.Policy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReleaseStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.AcquisitionAttempt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.FailurePoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabEvidenceOwnershipManagerTest {
    private static final Instant NOW = Instant.parse("2026-07-17T22:55:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Policy POLICY = new Policy(
            Duration.ofSeconds(30), Duration.ofSeconds(10), 1, 2, Duration.ZERO);
    private static final OwnerIdentity OWNER = new OwnerIdentity(
            "owner-test-a", "instance-test-a", 4242L, "a".repeat(64));

    @TempDir
    Path temporaryDirectory;

    @Test
    void acquiresPublishesVerifiesAndReleasesWithoutDeletingTheLockFile() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("normal");
        AcquisitionAttempt attempt = acquire(paths, OWNER);

        assertEquals(OperationStatus.SUCCEEDED, attempt.result().status());
        assertEquals(FailureClassification.NONE, attempt.result().failure());
        EnterpriseLabEvidenceOwnershipLease ownership = attempt.ownership().orElseThrow();
        assertTrue(ownership.operatingSystemLockValid());
        assertTrue(Files.isRegularFile(paths.lockFile(), LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.isRegularFile(paths.recordFile(), LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.exists(paths.temporaryRecordFile(), LinkOption.NOFOLLOW_LINKS));

        OwnershipRecord durable = decode(paths.recordFile());
        assertEquals(ownership.record(), durable);
        assertEquals(OwnershipState.OWNED, durable.state());
        assertEquals(ReleaseStatus.NOT_REQUESTED, durable.releaseStatus());
        assertEquals(paths.directoryIdentity(), durable.directoryIdentity());
        assertEquals(paths.identityOfControlledRegularFile(paths.lockFile()),
                durable.lockFileIdentity());

        var released = ownership.release();
        assertEquals(OperationStatus.SUCCEEDED, released.status());
        assertTrue(released.operatingSystemLockReleased());
        assertEquals(OwnershipState.RELEASED, released.record().orElseThrow().state());
        assertEquals(ReleaseStatus.RELEASED, released.record().orElseThrow().releaseStatus());
        assertFalse(ownership.operatingSystemLockValid());
        assertEquals(released, ownership.release());
        ownership.close();
        assertEquals(released.record().orElseThrow(), decode(paths.recordFile()));
        assertTrue(Files.isRegularFile(paths.lockFile(), LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    void liveLeaseExcludesAnotherChannelAndTheOsLockIsAvailableAfterRelease() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("os-contention");
        EnterpriseLabEvidenceOwnershipLease ownership = acquire(paths, OWNER)
                .ownership().orElseThrow();

        try (FileChannel contender = FileChannel.open(
                paths.lockFile(), StandardOpenOption.READ, StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS)) {
            assertThrows(OverlappingFileLockException.class, contender::tryLock);
        }

        ownership.release();
        try (FileChannel contender = paths.openLockChannel();
             FileLock acquired = contender.tryLock()) {
            assertNotNull(acquired);
            assertTrue(acquired.isValid());
        }
        assertTrue(Files.exists(paths.lockFile(), LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    void duplicateAcquisitionIsRefusedWithoutDisturbingTheLiveOwner() {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("duplicate");
        EnterpriseLabEvidenceOwnershipLease first = acquire(paths, OWNER)
                .ownership().orElseThrow();

        AcquisitionAttempt duplicate = acquire(paths, new OwnerIdentity(
                "owner-test-b", "instance-test-b", 4343L, "b".repeat(64)));

        assertEquals(OperationStatus.REFUSED, duplicate.result().status());
        assertEquals(FailureClassification.DUPLICATE_ACQUISITION,
                duplicate.result().failure());
        assertTrue(duplicate.ownership().isEmpty());
        assertTrue(first.operatingSystemLockValid());
        first.release();
    }

    @Test
    void simultaneousThreadsPublishExactlyOneLiveOwnershipResource() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("thread-race");
        int contenders = 8;
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(contenders);
        List<java.util.concurrent.Future<AcquisitionAttempt>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < contenders; index++) {
                int ownerIndex = index;
                futures.add(executor.submit(() -> {
                    start.await();
                    return acquire(paths, new OwnerIdentity(
                            "owner-race-" + ownerIndex,
                            "instance-race-" + ownerIndex,
                            5000L + ownerIndex,
                            "c".repeat(64)));
                }));
            }
            start.countDown();

            List<AcquisitionAttempt> attempts = new ArrayList<>();
            for (var future : futures) {
                attempts.add(future.get(10, TimeUnit.SECONDS));
            }
            List<EnterpriseLabEvidenceOwnershipLease> winners = attempts.stream()
                    .flatMap(attempt -> attempt.ownership().stream())
                    .toList();
            assertEquals(1, winners.size());
            assertEquals(7L, attempts.stream()
                    .map(AcquisitionAttempt::result)
                    .filter(result -> result.failure()
                            == FailureClassification.DUPLICATE_ACQUISITION)
                    .count());
            winners.get(0).release();
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void boundedNullLockAttemptsRefuseALiveOwnerWithoutSpinning() {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("bounded-live-owner");
        AtomicInteger attempts = new AtomicInteger();
        Policy threeAttempts = new Policy(
                Duration.ofSeconds(30), Duration.ofSeconds(10), 3, 1, Duration.ZERO);

        AcquisitionAttempt result = EnterpriseLabEvidenceOwnershipManager.acquire(
                paths, threeAttempts, CLOCK, OWNER, point -> {
                }, channel -> {
                    attempts.incrementAndGet();
                    return null;
                });

        assertEquals(3, attempts.get());
        assertEquals(OperationStatus.REFUSED, result.result().status());
        assertEquals(FailureClassification.LIVE_COMPETING_OWNER,
                result.result().failure());
        assertTrue(result.ownership().isEmpty());
        assertTrue(Files.isRegularFile(paths.lockFile(), LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    void unsupportedLockingAndUnexpectedChannelClosureFailDeliberately() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths unsupportedPaths = paths("unsupported-lock");
        AcquisitionAttempt unsupported = EnterpriseLabEvidenceOwnershipManager.acquire(
                unsupportedPaths, POLICY, CLOCK, OWNER, point -> {
                }, channel -> {
                    throw new UnsupportedOperationException("simulated unsupported lock");
                });
        assertEquals(FailureClassification.LOCK_UNSUPPORTED, unsupported.result().failure());
        assertTrue(unsupported.ownership().isEmpty());

        EnterpriseLabEvidenceOwnershipPaths closedPaths = paths("closed-channel");
        AcquisitionAttempt closed = EnterpriseLabEvidenceOwnershipManager.acquire(
                closedPaths, POLICY, CLOCK, OWNER, point -> {
                }, channel -> {
                    FileLock lock = channel.tryLock();
                    channel.close();
                    return lock;
                });
        assertEquals(FailureClassification.LOCK_LOST, closed.result().failure());
        assertTrue(closed.ownership().isEmpty());
        try (FileChannel channel = closedPaths.openLockChannel();
             FileLock lock = channel.tryLock()) {
            assertNotNull(lock);
        }
    }

    @Test
    void prePublicationFailuresReleaseResourcesAndPreserveForcedTemporaryEvidence()
            throws Exception {
        Set<FailurePoint> points = Set.of(
                FailurePoint.BEFORE_LOCK_OPEN,
                FailurePoint.AFTER_LOCK_ACQUIRED,
                FailurePoint.DURING_RECORD_WRITE,
                FailurePoint.AFTER_RECORD_FORCE);
        for (FailurePoint point : points) {
            EnterpriseLabEvidenceOwnershipPaths paths = paths("failure-" + point.name());
            AcquisitionAttempt failed = EnterpriseLabEvidenceOwnershipManager.acquire(
                    paths, POLICY, CLOCK, OWNER, candidate -> {
                        if (candidate == point) {
                            throw new IOException("injected " + point);
                        }
                    }, FileChannel::tryLock);

            assertEquals(OperationStatus.FAILED, failed.result().status(), point.name());
            assertEquals(FailureClassification.IO_FAILURE, failed.result().failure(), point.name());
            assertTrue(failed.ownership().isEmpty(), point.name());
            assertFalse(Files.exists(paths.recordFile(), LinkOption.NOFOLLOW_LINKS), point.name());
            boolean tempExpected = point == FailurePoint.DURING_RECORD_WRITE
                    || point == FailurePoint.AFTER_RECORD_FORCE;
            assertEquals(tempExpected,
                    Files.exists(paths.temporaryRecordFile(), LinkOption.NOFOLLOW_LINKS),
                    point.name());
            try (FileChannel channel = paths.openLockChannel();
                 FileLock lock = channel.tryLock()) {
                assertNotNull(lock, point.name());
            }
        }
    }

    @Test
    void failureAfterInstallLeavesAbruptOwnerEvidenceButNoLiveLock() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("after-install");
        AcquisitionAttempt failed = EnterpriseLabEvidenceOwnershipManager.acquire(
                paths, POLICY, CLOCK, OWNER, point -> {
                    if (point == FailurePoint.AFTER_RECORD_INSTALL) {
                        throw new IOException("injected after install");
                    }
                }, FileChannel::tryLock);

        assertEquals(FailureClassification.IO_FAILURE, failed.result().failure());
        assertTrue(failed.ownership().isEmpty());
        assertEquals(OwnershipState.OWNED, decode(paths.recordFile()).state());
        try (FileChannel channel = paths.openLockChannel();
             FileLock lock = channel.tryLock()) {
            assertNotNull(lock);
        }

        AcquisitionAttempt next = acquire(paths, new OwnerIdentity(
                "owner-after-install", "instance-after-install", 6001L, "d".repeat(64)));
        assertEquals(OperationStatus.REFUSED, next.result().status());
        assertEquals(FailureClassification.TAKEOVER_NOT_PERMITTED,
                next.result().failure());
        assertEquals(OwnershipState.OWNED,
                next.result().record().orElseThrow().state());
    }

    @Test
    void releaseRecordFailureStillClosesTheLockAndIsIdempotentlyReported() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("release-failure");
        AcquisitionAttempt acquired = EnterpriseLabEvidenceOwnershipManager.acquire(
                paths, POLICY, CLOCK, OWNER, point -> {
                    if (point == FailurePoint.DURING_RELEASE_RECORD_WRITE) {
                        throw new IOException("injected release write failure");
                    }
                }, FileChannel::tryLock);
        EnterpriseLabEvidenceOwnershipLease ownership = acquired.ownership().orElseThrow();

        var failed = ownership.release();
        assertEquals(OperationStatus.FAILED, failed.status());
        assertEquals(FailureClassification.IO_FAILURE, failed.failure());
        assertTrue(failed.operatingSystemLockReleased());
        assertEquals(OwnershipState.OWNED, decode(paths.recordFile()).state());
        assertSame(failed, ownership.release());
        assertThrows(EnterpriseLabEvidenceOwnershipException.class, ownership::close);
        try (FileChannel channel = paths.openLockChannel();
             FileLock lock = channel.tryLock()) {
            assertNotNull(lock);
        }
    }

    @Test
    void releaseRecoversWhenTheDurableInstallCompletedBeforeStatusPublication() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("release-installed-before-status");
        AcquisitionAttempt acquired = EnterpriseLabEvidenceOwnershipManager.acquire(
                paths, POLICY, CLOCK, OWNER, point -> {
                    if (point == FailurePoint.AFTER_RELEASE_RECORD_INSTALL) {
                        throw new IOException("injected after release install");
                    }
                }, FileChannel::tryLock);
        EnterpriseLabEvidenceOwnershipLease ownership = acquired.ownership().orElseThrow();

        var released = ownership.release();

        assertEquals(OperationStatus.SUCCEEDED, released.status());
        assertTrue(released.operatingSystemLockReleased());
        assertEquals(OwnershipState.RELEASED, released.record().orElseThrow().state());
        assertEquals(released.record().orElseThrow(), decode(paths.recordFile()));
        assertEquals(released, ownership.release());
        try (FileChannel channel = paths.openLockChannel();
             FileLock lock = channel.tryLock()) {
            assertNotNull(lock);
        }
    }

    @Test
    void replacedOwnerRecordBlocksCleanReleaseButNeverLeaksTheOsLock() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("record-replaced");
        EnterpriseLabEvidenceOwnershipLease ownership = acquire(paths, OWNER)
                .ownership().orElseThrow();
        byte[] original = Files.readAllBytes(paths.recordFile());
        byte[] changed = new String(original, StandardCharsets.UTF_8)
                .replace("OWNED", "FAILED")
                .getBytes(StandardCharsets.UTF_8);
        Files.write(paths.recordFile(), changed, StandardOpenOption.TRUNCATE_EXISTING);

        var failed = ownership.release();
        assertEquals(OperationStatus.FAILED, failed.status());
        assertEquals(FailureClassification.RECORD_FINGERPRINT_MISMATCH, failed.failure());
        assertTrue(failed.operatingSystemLockReleased());
        try (FileChannel channel = paths.openLockChannel();
             FileLock lock = channel.tryLock()) {
            assertNotNull(lock);
        }
    }

    @Test
    void releasedRecordRequiresTheLaterTakeoverGenerationPath() {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("released-prior");
        EnterpriseLabEvidenceOwnershipLease first = acquire(paths, OWNER)
                .ownership().orElseThrow();
        OwnershipRecord released = first.release().record().orElseThrow();

        AcquisitionAttempt next = acquire(paths, new OwnerIdentity(
                "owner-next", "instance-next", 7001L, "e".repeat(64)));

        assertEquals(OperationStatus.REFUSED, next.result().status());
        assertEquals(FailureClassification.TAKEOVER_NOT_PERMITTED,
                next.result().failure());
        assertEquals(released, next.result().record().orElseThrow());
        assertTrue(next.ownership().isEmpty());
    }

    @Test
    void malformedAndUnsupportedPriorRecordsArePreservedAndClassified() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths malformedPaths = paths("malformed-prior");
        Files.writeString(malformedPaths.recordFile(), "{}", StandardOpenOption.CREATE_NEW);
        AcquisitionAttempt malformed = acquire(malformedPaths, OWNER);
        assertEquals(FailureClassification.RECORD_MALFORMED,
                malformed.result().failure());
        assertEquals("{}", Files.readString(malformedPaths.recordFile()));

        EnterpriseLabEvidenceOwnershipPaths unsupportedPaths = paths("unsupported-prior");
        try (FileChannel ignored = unsupportedPaths.openLockChannel()) {
            // Prepare the fixed lock file before deriving its identity.
        }
        OwnershipRecord record = OwnershipRecord.initial(
                CLOCK, POLICY, OWNER, unsupportedPaths.directoryIdentity(),
                unsupportedPaths.identityOfControlledRegularFile(unsupportedPaths.lockFile()));
        String unsupportedJson = new String(
                new EnterpriseLabEvidenceOwnershipCodec().encode(record), StandardCharsets.UTF_8)
                .replace(EnterpriseLabEvidenceOwnership.RECORD_SCHEMA_VERSION,
                        "enterprise-lab-evidence-owner-record/v2");
        Files.writeString(unsupportedPaths.recordFile(), unsupportedJson,
                StandardOpenOption.CREATE_NEW);
        AcquisitionAttempt unsupported = acquire(unsupportedPaths, OWNER);
        assertEquals(FailureClassification.UNSUPPORTED_RECORD_VERSION,
                unsupported.result().failure());
        assertTrue(unsupported.ownership().isEmpty());
        assertTrue(Files.readString(unsupportedPaths.recordFile()).contains("/v2"));
    }

    @Test
    void replacedLockFileIsDetectedBeforeAnyNewOwnerRecordCanBePublished() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("replaced-lock-file");
        EnterpriseLabEvidenceOwnershipLease ownership = acquire(paths, OWNER)
                .ownership().orElseThrow();
        OwnershipRecord released = ownership.release().record().orElseThrow();

        Files.delete(paths.lockFile());
        Files.createFile(paths.lockFile());
        String replacementIdentity = paths.identityOfControlledRegularFile(paths.lockFile());
        assertNotEquals(released.lockFileIdentity(), replacementIdentity);

        AcquisitionAttempt next = acquire(paths, new OwnerIdentity(
                "owner-lock-replacement", "instance-lock-replacement", 7100L,
                "f".repeat(64)));
        assertEquals(OperationStatus.FAILED, next.result().status());
        assertEquals(FailureClassification.LOCK_IDENTITY_MISMATCH,
                next.result().failure());
        assertEquals(released, next.result().record().orElseThrow());
        assertTrue(Files.isRegularFile(paths.lockFile(), LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    void unsafePublicRootReturnsStructuredFailureWithoutCreatingOwnership() {
        AcquisitionAttempt result = EnterpriseLabEvidenceOwnershipManager.acquire(
                Path.of("relative-evidence-root"), POLICY, CLOCK);

        assertEquals(OperationStatus.FAILED, result.result().status());
        assertEquals(FailureClassification.UNSAFE_PATH, result.result().failure());
        assertTrue(result.ownership().isEmpty());
    }

    @Test
    void publicAcquisitionGeneratesSafeBoundedOwnerIdentityInternally() {
        Path root = directory("public-owner");
        AcquisitionAttempt attempt = EnterpriseLabEvidenceOwnershipManager.acquire(
                root, POLICY, CLOCK);
        EnterpriseLabEvidenceOwnershipLease ownership = attempt.ownership().orElseThrow();

        OwnerIdentity generated = ownership.record().owner();
        assertTrue(generated.ownerId().matches("owner-[0-9a-f-]{36}"));
        assertTrue(generated.applicationInstanceId().matches("instance-[0-9a-f-]{36}"));
        assertTrue(generated.hostDiagnosticFingerprint().matches("[0-9a-f]{64}"));
        assertTrue(generated.processId() >= 0L);
        assertFalse(new String(new EnterpriseLabEvidenceOwnershipCodec()
                        .encode(ownership.record()), StandardCharsets.UTF_8)
                .contains(root.toString()));
        ownership.release();
    }

    @Test
    void symbolicLinkLockFileIsRejectedWhereSupported() throws Exception {
        EnterpriseLabEvidenceOwnershipPaths paths = paths("lock-link");
        Path outside = Files.createFile(temporaryDirectory.resolve("outside-lock"));
        try {
            Files.createSymbolicLink(paths.lockFile(), outside);
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException exception) {
            assertFalse(Files.exists(paths.lockFile(), LinkOption.NOFOLLOW_LINKS));
            return;
        }

        AcquisitionAttempt result = acquire(paths, OWNER);
        assertEquals(FailureClassification.UNSAFE_PATH, result.result().failure());
        assertTrue(result.ownership().isEmpty());
        assertTrue(Files.isSymbolicLink(paths.lockFile()));
    }

    private AcquisitionAttempt acquire(
            EnterpriseLabEvidenceOwnershipPaths paths,
            OwnerIdentity owner) {
        return EnterpriseLabEvidenceOwnershipManager.acquire(
                paths, POLICY, CLOCK, owner, point -> {
                }, FileChannel::tryLock);
    }

    private EnterpriseLabEvidenceOwnershipPaths paths(String name) {
        return EnterpriseLabEvidenceOwnershipPaths.create(directory(name));
    }

    private Path directory(String name) {
        try {
            return Files.createDirectory(temporaryDirectory.resolve(name))
                    .toAbsolutePath().normalize();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static OwnershipRecord decode(Path path) throws IOException {
        return new EnterpriseLabEvidenceOwnershipCodec().decode(Files.readAllBytes(path));
    }
}
