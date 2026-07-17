package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournal.AppendReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournal.PersistenceStage;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournal.ReadResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournal.SyncPolicy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournal.TailStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalDirectory.WriteCheckpoint;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalEvent.Draft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalEvent.Reason;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalStorageException.Failure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabExperimentLocalJournalTest {
    private static final String CONFIGURATION_FINGERPRINT = "a".repeat(64);
    private static final String DECISION_FINGERPRINT = "b".repeat(64);
    private static final String BASELINE_FINGERPRINT = "c".repeat(64);
    private static final String CANDIDATE_FINGERPRINT = "d".repeat(64);
    private static final String APPLIED_FINGERPRINT = "e".repeat(64);

    @TempDir
    Path tempDirectory;

    @Test
    void appendsReadsClosesAndReopensWithoutOverwritingHistory() {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        assertFalse(directory.read("experiment-append").exists());

        EnterpriseLabExperimentJournalEvent first = event(
                "experiment-append", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT);
        String journalId;
        try (EnterpriseLabExperimentJournal journal = directory.openJournal(
                "experiment-append", SyncPolicy.FORCE_DATA_AND_METADATA)) {
            journalId = journal.journalId();
            AppendReceipt receipt = journal.append(first);
            assertEquals(journalId, receipt.journalId());
            assertEquals(PersistenceStage.DATA_AND_METADATA_FORCE_COMPLETE, receipt.persistenceStage());
            assertFalse(receipt.userSpaceBufferRetained());
            assertTrue(receipt.operatingSystemWriteCompleted());
            assertTrue(receipt.forceCompleted());
            assertEquals(List.of(first), journal.read().events());
            journal.close();
        }

        EnterpriseLabExperimentJournalEvent second = event(
                "experiment-append", 2, first.currentEntryFingerprint());
        try (EnterpriseLabExperimentJournal reopened = directory.openJournal(
                "experiment-append", SyncPolicy.FORCE_DATA)) {
            assertEquals(journalId, reopened.journalId());
            reopened.append(second);
        }

        ReadResult result = directory.read("experiment-append");
        assertEquals(List.of(first, second), result.events());
        assertEquals(TailStatus.COMPLETE, result.tailStatus());
        assertEquals(result.totalBytes(), result.completeBytes());
        assertEquals(0, result.tailBytes());
        assertTrue(result.totalBytes() > 2);
    }

    @Test
    void defaultWriterUsesTheSafetyFirstMetadataSynchronizationPolicy() {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        try (EnterpriseLabExperimentJournal journal = directory.openJournal("experiment-default-sync")) {
            AppendReceipt receipt = journal.append(event(
                    "experiment-default-sync", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT));
            assertEquals(SyncPolicy.FORCE_DATA_AND_METADATA, receipt.syncPolicy());
            assertEquals(PersistenceStage.DATA_AND_METADATA_FORCE_COMPLETE, receipt.persistenceStage());
            assertTrue(receipt.forceCompleted());
        }
    }

    @Test
    void reportsEachSynchronizationBoundaryWithoutClaimingPowerLossDurability() {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        List<PersistenceStage> stages = new ArrayList<>();
        List<Boolean> forced = new ArrayList<>();
        AtomicInteger syncCheckpoints = new AtomicInteger();

        for (SyncPolicy policy : SyncPolicy.values()) {
            String experimentId = "experiment-sync-" + policy.name().toLowerCase();
            try (EnterpriseLabExperimentJournal journal = directory.openJournal(
                    experimentId,
                    policy,
                    (checkpoint, bytesWritten) -> {
                        if (checkpoint == WriteCheckpoint.AFTER_SYNC) {
                            syncCheckpoints.incrementAndGet();
                        }
                    })) {
                AppendReceipt receipt = journal.append(event(
                        experimentId, 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT));
                stages.add(receipt.persistenceStage());
                forced.add(receipt.forceCompleted());
                assertTrue(receipt.operatingSystemWriteCompleted());
                assertFalse(receipt.userSpaceBufferRetained());
            }
        }

        assertEquals(List.of(
                PersistenceStage.OPERATING_SYSTEM_WRITE_COMPLETE,
                PersistenceStage.DATA_FORCE_COMPLETE,
                PersistenceStage.DATA_AND_METADATA_FORCE_COMPLETE), stages);
        assertEquals(List.of(false, true, true), forced);
        assertEquals(2, syncCheckpoints.get());
    }

    @Test
    void failureBeforeAppendLeavesAnEmptyJournalAndPermanentlyFailsThatWriter() {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        EnterpriseLabExperimentJournal journal = directory.openJournal(
                "experiment-before",
                SyncPolicy.FORCE_DATA_AND_METADATA,
                (checkpoint, bytesWritten) -> {
                    if (checkpoint == WriteCheckpoint.BEFORE_APPEND) {
                        throw new IOException("injected before append");
                    }
                });

        assertStorage(Failure.IO_FAILURE, () -> journal.append(event(
                "experiment-before", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT)));
        assertStorage(Failure.WRITER_FAILED, journal::read);
        ReadResult preserved = directory.read("experiment-before");
        assertTrue(preserved.exists());
        assertEquals(0, preserved.totalBytes());
        assertEquals(TailStatus.COMPLETE, preserved.tailStatus());
    }

    @Test
    void partialWriteFailureLeavesADetectableTailThatReadDoesNotMutate() throws Exception {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        EnterpriseLabExperimentJournal journal = directory.openJournal(
                "experiment-partial",
                SyncPolicy.FORCE_DATA_AND_METADATA,
                (checkpoint, bytesWritten) -> {
                    if (checkpoint == WriteCheckpoint.AFTER_WRITE_CHUNK) {
                        throw new IOException("injected partial write");
                    }
                });

        assertStorage(Failure.IO_FAILURE, () -> journal.append(event(
                "experiment-partial", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT)));
        Path file = findOnlyJournalFile(tempDirectory);
        byte[] beforeRead = Files.readAllBytes(file);
        assertEquals(256, beforeRead.length);

        ReadResult result = directory.read("experiment-partial");
        assertEquals(TailStatus.TRUNCATED_TAIL, result.tailStatus());
        assertEquals(0, result.completeBytes());
        assertEquals(beforeRead.length, result.tailBytes());
        assertEquals(List.of(), result.events());
        assertArrayEquals(beforeRead, Files.readAllBytes(file));
        assertStorage(Failure.PARTIAL_TAIL,
                () -> directory.openJournal("experiment-partial", SyncPolicy.FORCE_DATA));
        assertArrayEquals(beforeRead, Files.readAllBytes(file));
    }

    @Test
    void failureAfterCompleteWriteBeforeForceMayLeaveOneCompleteEvent() {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        EnterpriseLabExperimentJournal journal = directory.openJournal(
                "experiment-preforce",
                SyncPolicy.FORCE_DATA_AND_METADATA,
                (checkpoint, bytesWritten) -> {
                    if (checkpoint == WriteCheckpoint.AFTER_APPEND_BEFORE_SYNC) {
                        throw new IOException("injected before force");
                    }
                });
        EnterpriseLabExperimentJournalEvent event = event(
                "experiment-preforce", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT);

        assertStorage(Failure.IO_FAILURE, () -> journal.append(event));
        ReadResult result = directory.read("experiment-preforce");
        assertEquals(TailStatus.COMPLETE, result.tailStatus());
        assertEquals(List.of(event), result.events());
    }

    @Test
    void failureAfterForceMayLeaveOneCompleteEventWithoutReturningAReceipt() {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        EnterpriseLabExperimentJournal journal = directory.openJournal(
                "experiment-postforce",
                SyncPolicy.FORCE_DATA,
                (checkpoint, bytesWritten) -> {
                    if (checkpoint == WriteCheckpoint.AFTER_SYNC) {
                        throw new IOException("injected after force");
                    }
                });
        EnterpriseLabExperimentJournalEvent event = event(
                "experiment-postforce", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT);

        assertStorage(Failure.IO_FAILURE, () -> journal.append(event));
        ReadResult result = directory.read("experiment-postforce");
        assertEquals(TailStatus.COMPLETE, result.tailStatus());
        assertEquals(List.of(event), result.events());
    }

    @Test
    void rejectsIdentitySequenceAndPredecessorMismatchBeforeWritingBytes() {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);

        EnterpriseLabExperimentJournal identity = directory.openJournal(
                "experiment-identity", SyncPolicy.WRITE_TO_OS);
        assertStorage(Failure.IDENTITY_MISMATCH, () -> identity.append(event(
                "experiment-other", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT)));
        assertEquals(0, directory.read("experiment-identity").totalBytes());

        EnterpriseLabExperimentJournal sequence = directory.openJournal(
                "experiment-sequence", SyncPolicy.WRITE_TO_OS);
        assertStorage(Failure.SEQUENCE_MISMATCH, () -> sequence.append(event(
                "experiment-sequence", 2, "f".repeat(64))));
        assertEquals(0, directory.read("experiment-sequence").totalBytes());

        EnterpriseLabExperimentJournal predecessor = directory.openJournal(
                "experiment-predecessor", SyncPolicy.WRITE_TO_OS);
        EnterpriseLabExperimentJournalEvent first = event(
                "experiment-predecessor", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT);
        predecessor.append(first);
        assertStorage(Failure.PREDECESSOR_MISMATCH, () -> predecessor.append(event(
                "experiment-predecessor", 2, "f".repeat(64))));
        assertEquals(List.of(first), directory.read("experiment-predecessor").events());
    }

    @Test
    void enforcesProcessLocalExclusiveOwnershipAndAllowsOwnedSnapshots() {
        EnterpriseLabExperimentJournalDirectory firstDirectory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        EnterpriseLabExperimentJournalDirectory secondDirectory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        EnterpriseLabExperimentJournal writer = firstDirectory.openJournal(
                "experiment-owned", SyncPolicy.WRITE_TO_OS);
        EnterpriseLabExperimentJournalEvent event = event(
                "experiment-owned", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT);
        writer.append(event);

        assertStorage(Failure.WRITER_ALREADY_ACTIVE,
                () -> secondDirectory.openJournal("experiment-owned", SyncPolicy.WRITE_TO_OS));
        assertStorage(Failure.WRITER_ALREADY_ACTIVE,
                () -> secondDirectory.read("experiment-owned"));
        assertEquals(List.of(event), writer.read().events());

        writer.close();
        writer.close();
        assertStorage(Failure.CLOSED, writer::read);
        assertEquals(List.of(event), secondDirectory.read("experiment-owned").events());
    }

    @Test
    void invalidOwnedSnapshotFailsTheWriterBeforeAnyLaterAppend() throws Exception {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        EnterpriseLabExperimentJournal writer = directory.openJournal(
                "experiment-suspect", SyncPolicy.WRITE_TO_OS);
        EnterpriseLabExperimentJournalEvent first = event(
                "experiment-suspect", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT);
        writer.append(first);
        Files.writeString(findOnlyJournalFile(tempDirectory), "{}\n", StandardCharsets.UTF_8);

        assertStorage(Failure.INVALID_COMPLETE_ENTRY, writer::read);
        assertStorage(Failure.WRITER_FAILED, () -> writer.append(event(
                "experiment-suspect", 2, first.currentEntryFingerprint())));
        assertStorage(Failure.INVALID_COMPLETE_ENTRY, () -> directory.read("experiment-suspect"));
    }

    @Test
    void statusReadWaitsForAnInFlightAppendAndThenObservesItsCompleteFrame() throws Exception {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        CountDownLatch appendReachedCheckpoint = new CountDownLatch(1);
        CountDownLatch permitAppend = new CountDownLatch(1);
        EnterpriseLabExperimentJournal journal = directory.openJournal(
                "experiment-read-concurrency",
                SyncPolicy.WRITE_TO_OS,
                (checkpoint, bytesWritten) -> {
                    if (checkpoint == WriteCheckpoint.BEFORE_APPEND) {
                        appendReachedCheckpoint.countDown();
                        awaitBoundedRelease(permitAppend);
                    }
                });
        EnterpriseLabExperimentJournalEvent event = event(
                "experiment-read-concurrency", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT);
        AtomicReference<Throwable> appendFailure = new AtomicReference<>();
        AtomicReference<ReadResult> readResult = new AtomicReference<>();
        AtomicReference<Throwable> readFailure = new AtomicReference<>();
        CountDownLatch readAttempted = new CountDownLatch(1);
        Thread appendThread = new Thread(() -> {
            try {
                journal.append(event);
            } catch (Throwable throwable) {
                appendFailure.set(throwable);
            }
        }, "journal-append-test");
        Thread readThread = new Thread(() -> {
            readAttempted.countDown();
            try {
                readResult.set(journal.read());
            } catch (Throwable throwable) {
                readFailure.set(throwable);
            }
        }, "journal-read-test");

        appendThread.start();
        assertTrue(appendReachedCheckpoint.await(5, TimeUnit.SECONDS));
        readThread.start();
        assertTrue(readAttempted.await(5, TimeUnit.SECONDS));
        assertThreadBlocks(readThread);
        permitAppend.countDown();
        appendThread.join(5_000);
        readThread.join(5_000);

        assertFalse(appendThread.isAlive());
        assertFalse(readThread.isAlive());
        assertEquals(null, appendFailure.get());
        assertEquals(null, readFailure.get());
        assertEquals(List.of(event), readResult.get().events());
        journal.close();
    }

    @Test
    void closeWaitsForAnInFlightAppendAndReleasesOwnershipAfterward() throws Exception {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        CountDownLatch appendReachedCheckpoint = new CountDownLatch(1);
        CountDownLatch permitAppend = new CountDownLatch(1);
        EnterpriseLabExperimentJournal journal = directory.openJournal(
                "experiment-close-concurrency",
                SyncPolicy.WRITE_TO_OS,
                (checkpoint, bytesWritten) -> {
                    if (checkpoint == WriteCheckpoint.BEFORE_APPEND) {
                        appendReachedCheckpoint.countDown();
                        awaitBoundedRelease(permitAppend);
                    }
                });
        EnterpriseLabExperimentJournalEvent event = event(
                "experiment-close-concurrency", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT);
        AtomicReference<Throwable> appendFailure = new AtomicReference<>();
        AtomicReference<Throwable> closeFailure = new AtomicReference<>();
        CountDownLatch closeAttempted = new CountDownLatch(1);
        Thread appendThread = new Thread(() -> {
            try {
                journal.append(event);
            } catch (Throwable throwable) {
                appendFailure.set(throwable);
            }
        }, "journal-append-close-test");
        Thread closeThread = new Thread(() -> {
            closeAttempted.countDown();
            try {
                journal.close();
            } catch (Throwable throwable) {
                closeFailure.set(throwable);
            }
        }, "journal-close-test");

        appendThread.start();
        assertTrue(appendReachedCheckpoint.await(5, TimeUnit.SECONDS));
        closeThread.start();
        assertTrue(closeAttempted.await(5, TimeUnit.SECONDS));
        assertThreadBlocks(closeThread);
        permitAppend.countDown();
        appendThread.join(5_000);
        closeThread.join(5_000);

        assertFalse(appendThread.isAlive());
        assertFalse(closeThread.isAlive());
        assertEquals(null, appendFailure.get());
        assertEquals(null, closeFailure.get());
        assertEquals(List.of(event), directory.read("experiment-close-concurrency").events());
    }

    @Test
    void reducedTestLimitsProveEntryCountAndJournalSizeStopBeforeAWrite() {
        Path countRoot = createDirectory("count-root");
        EnterpriseLabExperimentJournalDirectory countDirectory =
                EnterpriseLabExperimentJournalDirectory.createForTesting(countRoot, 1_000_000, 1);
        EnterpriseLabExperimentJournal countJournal = countDirectory.openJournal(
                "experiment-count", SyncPolicy.WRITE_TO_OS);
        EnterpriseLabExperimentJournalEvent first = event(
                "experiment-count", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT);
        countJournal.append(first);
        assertStorage(Failure.ENTRY_LIMIT_EXCEEDED, () -> countJournal.append(event(
                "experiment-count", 2, first.currentEntryFingerprint())));
        assertEquals(List.of(first), countDirectory.read("experiment-count").events());

        Path sizeRoot = createDirectory("size-root");
        EnterpriseLabExperimentJournalDirectory sizeDirectory =
                EnterpriseLabExperimentJournalDirectory.createForTesting(sizeRoot, 1, 10);
        EnterpriseLabExperimentJournal sizeJournal = sizeDirectory.openJournal(
                "experiment-size", SyncPolicy.WRITE_TO_OS);
        assertStorage(Failure.JOURNAL_SIZE_EXCEEDED, () -> sizeJournal.append(event(
                "experiment-size", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT)));
        assertEquals(0, sizeDirectory.read("experiment-size").totalBytes());
    }

    @Test
    void readerRejectsAnAlreadyOversizedFileWithoutAllocatingItsContents() throws Exception {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.createForTesting(tempDirectory, 1, 10);
        EnterpriseLabExperimentJournal journal = directory.openJournal(
                "experiment-oversized", SyncPolicy.WRITE_TO_OS);
        journal.close();
        Files.write(findOnlyJournalFile(tempDirectory), new byte[] {'x', 'y'});

        assertStorage(Failure.JOURNAL_SIZE_EXCEEDED, () -> directory.read("experiment-oversized"));
    }

    @Test
    void hashesCanonicalExperimentIdentityIntoAControlledFilename() throws Exception {
        String experimentId = "tenant:alpha.experiment-001";
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        String journalId;
        try (EnterpriseLabExperimentJournal journal = directory.openJournal(
                experimentId, SyncPolicy.WRITE_TO_OS)) {
            journalId = journal.journalId();
            journal.append(event(
                    experimentId, 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT));
        }

        Path file = findOnlyJournalFile(tempDirectory);
        assertEquals(journalId + ".jsonl", file.getFileName().toString());
        assertTrue(journalId.matches("journal-v1-[0-9a-f]{64}"));
        assertFalse(file.getFileName().toString().contains(experimentId));
        assertNotEquals(experimentId, journalId);
        assertThrows(IllegalArgumentException.class,
                () -> directory.openJournal("../escape", SyncPolicy.WRITE_TO_OS));
        assertEquals(1, countJournalFiles(tempDirectory));
    }

    @Test
    void requiresAnExistingAbsoluteNonRootDirectory() throws Exception {
        assertStorage(Failure.UNSAFE_DIRECTORY,
                () -> EnterpriseLabExperimentJournalDirectory.create(Path.of("relative-journal-root")));
        assertStorage(Failure.UNSAFE_DIRECTORY,
                () -> EnterpriseLabExperimentJournalDirectory.create(tempDirectory.resolve("missing")));
        assertStorage(Failure.UNSAFE_DIRECTORY,
                () -> EnterpriseLabExperimentJournalDirectory.create(tempDirectory.getRoot()));
        Path file = Files.createFile(tempDirectory.resolve("not-a-directory"));
        assertStorage(Failure.UNSAFE_DIRECTORY,
                () -> EnterpriseLabExperimentJournalDirectory.create(file));
        if (java.nio.file.FileSystems.getDefault().getSeparator().equals("\\")) {
            assertStorage(Failure.UNSAFE_DIRECTORY,
                    () -> EnterpriseLabExperimentJournalDirectory.create(
                            Path.of("\\\\server\\share\\journal")));
        }
    }

    @Test
    void unavailableControlledJournalDirectoryFailsClosedWithoutEscapingTheNamespace() throws Exception {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        Path journals = tempDirectory
                .resolve("enterprise-lab-experiment-journals-v1")
                .resolve("journals");
        Files.delete(journals);
        Files.createFile(journals);

        EnterpriseLabExperimentJournalStorageException exception = assertThrows(
                EnterpriseLabExperimentJournalStorageException.class,
                () -> directory.openJournal("experiment-unavailable", SyncPolicy.WRITE_TO_OS));
        assertTrue(exception.failure() == Failure.IO_FAILURE || exception.failure() == Failure.UNSAFE_PATH);
        assertEquals(0, countJournalFiles(tempDirectory));
    }

    @Test
    void rejectsSymbolicLinkRootsAndJournalTargetsWhenPlatformSupportsLinks() throws Exception {
        Path actualRoot = Files.createDirectory(tempDirectory.resolve("actual-root"));
        Path rootLink = tempDirectory.resolve("root-link");
        try {
            Files.createSymbolicLink(rootLink, actualRoot);
        } catch (UnsupportedOperationException | FileSystemException exception) {
            return;
        }
        assertStorage(Failure.UNSAFE_DIRECTORY,
                () -> EnterpriseLabExperimentJournalDirectory.create(rootLink));

        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(actualRoot);
        EnterpriseLabExperimentJournal journal = directory.openJournal(
                "experiment-link", SyncPolicy.WRITE_TO_OS);
        journal.close();
        Path journalFile = findOnlyJournalFile(actualRoot);
        Path target = Files.writeString(actualRoot.resolve("link-target"), "not journal data");
        Files.delete(journalFile);
        Files.createSymbolicLink(journalFile, target);

        assertStorage(Failure.UNSAFE_PATH, () -> directory.read("experiment-link"));
        assertStorage(Failure.UNSAFE_PATH,
                () -> directory.openJournal("experiment-link", SyncPolicy.WRITE_TO_OS));
        assertEquals("not journal data", Files.readString(target));
    }

    @Test
    void appliesRestrictivePosixPermissionsWhereSupported() throws Exception {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        EnterpriseLabExperimentJournal journal = directory.openJournal(
                "experiment-permissions", SyncPolicy.WRITE_TO_OS);
        journal.close();
        Path file = findOnlyJournalFile(tempDirectory);
        PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class);
        if (view == null) {
            return;
        }

        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file));
        assertEquals(PosixFilePermissions.fromString("rwx------"),
                Files.getPosixFilePermissions(file.getParent()));
        assertEquals(PosixFilePermissions.fromString("rwx------"),
                Files.getPosixFilePermissions(file.getParent().getParent()));
    }

    @Test
    void refusesMalformedAndNonCanonicalCompleteFramesWithoutChangingThem() throws Exception {
        Path malformedRoot = createDirectory("malformed-root");
        EnterpriseLabExperimentJournalDirectory malformedDirectory =
                EnterpriseLabExperimentJournalDirectory.create(malformedRoot);
        EnterpriseLabExperimentJournal malformedJournal = malformedDirectory.openJournal(
                "experiment-malformed", SyncPolicy.WRITE_TO_OS);
        malformedJournal.close();
        Path malformedFile = findOnlyJournalFile(malformedRoot);
        byte[] malformed = "{}\n".getBytes(StandardCharsets.UTF_8);
        Files.write(malformedFile, malformed);
        assertStorage(Failure.INVALID_COMPLETE_ENTRY,
                () -> malformedDirectory.read("experiment-malformed"));
        assertArrayEquals(malformed, Files.readAllBytes(malformedFile));

        Path canonicalRoot = createDirectory("canonical-root");
        EnterpriseLabExperimentJournalDirectory canonicalDirectory =
                EnterpriseLabExperimentJournalDirectory.create(canonicalRoot);
        EnterpriseLabExperimentJournal canonicalJournal = canonicalDirectory.openJournal(
                "experiment-noncanonical", SyncPolicy.WRITE_TO_OS);
        canonicalJournal.append(event(
                "experiment-noncanonical", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT));
        canonicalJournal.close();
        Path canonicalFile = findOnlyJournalFile(canonicalRoot);
        byte[] original = Files.readAllBytes(canonicalFile);
        byte[] nonCanonical = new byte[original.length + 1];
        System.arraycopy(original, 0, nonCanonical, 0, original.length - 1);
        nonCanonical[original.length - 1] = ' ';
        nonCanonical[original.length] = '\n';
        Files.write(canonicalFile, nonCanonical);

        assertStorage(Failure.NON_CANONICAL_ENTRY,
                () -> canonicalDirectory.read("experiment-noncanonical"));
        assertArrayEquals(nonCanonical, Files.readAllBytes(canonicalFile));
    }

    @Test
    void scanRejectsCompleteFramesWithWrongIdentitySequenceOrPredecessor() throws Exception {
        assertCorruptCompleteFrameFailure(
                "identity-root",
                "experiment-expected",
                event("experiment-other", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT),
                Failure.IDENTITY_MISMATCH);
        assertCorruptCompleteFrameFailure(
                "sequence-root",
                "experiment-sequence-read",
                event("experiment-sequence-read", 2, "f".repeat(64)),
                Failure.SEQUENCE_MISMATCH);

        Path root = createDirectory("predecessor-root");
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(root);
        EnterpriseLabExperimentJournal journal = directory.openJournal(
                "experiment-predecessor-read", SyncPolicy.WRITE_TO_OS);
        journal.close();
        EnterpriseLabExperimentJournalCodec codec = new EnterpriseLabExperimentJournalCodec();
        EnterpriseLabExperimentJournalEvent first = event(
                "experiment-predecessor-read", 1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT);
        EnterpriseLabExperimentJournalEvent second = event(
                "experiment-predecessor-read", 2, "f".repeat(64));
        byte[] firstBytes = codec.encode(first);
        byte[] secondBytes = codec.encode(second);
        byte[] frames = new byte[firstBytes.length + secondBytes.length + 2];
        System.arraycopy(firstBytes, 0, frames, 0, firstBytes.length);
        frames[firstBytes.length] = '\n';
        System.arraycopy(secondBytes, 0, frames, firstBytes.length + 1, secondBytes.length);
        frames[frames.length - 1] = '\n';
        Files.write(findOnlyJournalFile(root), frames);

        assertStorage(Failure.PREDECESSOR_MISMATCH,
                () -> directory.read("experiment-predecessor-read"));
    }

    private void assertCorruptCompleteFrameFailure(
            String rootName,
            String expectedExperimentId,
            EnterpriseLabExperimentJournalEvent event,
            Failure failure) throws Exception {
        Path root = createDirectory(rootName);
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(root);
        EnterpriseLabExperimentJournal journal = directory.openJournal(
                expectedExperimentId, SyncPolicy.WRITE_TO_OS);
        journal.close();
        byte[] encoded = new EnterpriseLabExperimentJournalCodec().encode(event);
        byte[] frame = new byte[encoded.length + 1];
        System.arraycopy(encoded, 0, frame, 0, encoded.length);
        frame[frame.length - 1] = '\n';
        Files.write(findOnlyJournalFile(root), frame);
        assertStorage(failure, () -> directory.read(expectedExperimentId));
    }

    private Path createDirectory(String name) {
        try {
            return Files.createDirectory(tempDirectory.resolve(name));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static EnterpriseLabExperimentJournalEvent event(
            String experimentId,
            long sequence,
            String previousFingerprint) {
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-16T23:00:00Z").plusSeconds(sequence), ZoneOffset.UTC);
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("requestCount", 4);
        payload.putArray("backendIds").add("loopback-b").add("loopback-a");
        EnterpriseLabExperimentJournalEventType eventType = sequence == 1
                ? EnterpriseLabExperimentJournalEventType.EXPERIMENT_STARTED
                : EnterpriseLabExperimentJournalEventType.OBSERVATION_CHECKPOINT;
        EnterpriseLabExperimentState stateBefore = sequence == 1
                ? EnterpriseLabExperimentState.ARMED
                : EnterpriseLabExperimentState.RUNNING;
        return EnterpriseLabExperimentJournalEvent.create(clock, new Draft(
                sequence,
                experimentId,
                "stable-steady-state",
                eventType,
                stateBefore,
                EnterpriseLabExperimentState.RUNNING,
                2,
                CONFIGURATION_FINGERPRINT,
                DECISION_FINGERPRINT,
                BASELINE_FINGERPRINT,
                CANDIDATE_FINGERPRINT,
                APPLIED_FINGERPRINT,
                new Reason("EXPERIMENT_STARTED", "candidate allocation applied in the bounded loopback lab"),
                previousFingerprint,
                Map.of("source", "operator"),
                payload));
    }

    private static Path findOnlyJournalFile(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> files = paths
                    .filter(path -> Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .toList();
            assertEquals(1, files.size());
            return files.get(0);
        }
    }

    private static long countJournalFiles(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(path -> Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .count();
        }
    }

    private static EnterpriseLabExperimentJournalStorageException assertStorage(
            Failure failure,
            org.junit.jupiter.api.function.Executable executable) {
        EnterpriseLabExperimentJournalStorageException exception = assertThrows(
                EnterpriseLabExperimentJournalStorageException.class, executable);
        assertEquals(failure, exception.failure());
        assertFalse(exception.getMessage().contains("\\"));
        assertFalse(exception.getMessage().contains("/"));
        return exception;
    }

    private static void assertThreadBlocks(Thread thread) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (thread.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertEquals(Thread.State.BLOCKED, thread.getState());
    }

    private static void awaitBoundedRelease(CountDownLatch release) throws IOException {
        try {
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new IOException("timed out waiting for bounded test release");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("bounded test wait was interrupted", exception);
        }
    }
}
