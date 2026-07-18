package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.Draft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.RecoveryClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransitionReason;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationStateStore.Failure;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationStateStore.StoreException;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationStateStore.WriteCheckpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabAllocationStateStoreTest {
    private static final Instant NOW = Instant.parse("2026-07-18T10:00:00Z");
    private static final String SCENARIO = "tail-latency-pressure";
    private static final Map<String, Double> BASELINE =
            Map.of("blue", 0.5, "green", 0.25, "orange", 0.25);
    private static final Map<String, Double> CANDIDATE =
            Map.of("blue", 0.25, "green", 0.5, "orange", 0.25);

    @TempDir
    Path temporaryDirectory;

    private EnterpriseLabExperimentTargetCatalog targets;
    private EnterpriseLabMutationTestAuthority authority;

    @BeforeEach
    void setUp() {
        targets = targetCatalog();
        authority = new EnterpriseLabMutationTestAuthority(temporaryDirectory);
    }

    @Test
    void committedBaselineAppendIsForcedVerifiedAndDeterministicAcrossRestart() {
        EnterpriseLabAllocationState baseline = baseline();

        EnterpriseLabAllocationStateStore.AppendReceipt receipt;
        try (EnterpriseLabAllocationStateStore store = ownedStore()) {
            receipt = store.append(baseline);
            assertEquals(1, receipt.recordCount());
            assertEquals(baseline.currentRecordFingerprint(), receipt.recordFingerprint());
            assertEquals(EnterpriseLabAllocationStateStore.SyncPolicy.FORCE_DATA_AND_METADATA,
                    receipt.syncPolicy());
            assertTrue(receipt.exactReadBackVerified());
            assertEquals(List.of(baseline), store.replay().records());
        }

        byte[] firstReplayBytes = readStoreBytes();
        try (EnterpriseLabAllocationStateStore inspection =
                     EnterpriseLabAllocationStateStore.inspect(temporaryDirectory, targets)) {
            var replay = inspection.replay();
            assertTrue(replay.storePresent());
            assertEquals(List.of(baseline), replay.records());
            assertEquals(Optional.of(baseline), replay.baseline());
            assertEquals(Optional.of(baseline), replay.chainHead());
            assertEquals(Optional.of(baseline), replay.lastCommitted());
            assertEquals(firstReplayBytes.length, replay.totalBytes());
        }
        assertArrayEquals(firstReplayBytes, readStoreBytes());
    }

    @Test
    void candidateIntentAppendsWithoutReplacingTheOnlyCommittedSafeBaseline() {
        EnterpriseLabAllocationState baseline = baseline();
        EnterpriseLabAllocationState candidate = candidate(
                "allocation-tx-2", 2, baseline.currentRecordFingerprint());

        try (EnterpriseLabAllocationStateStore store = ownedStore()) {
            store.append(baseline);
            store.append(candidate);
            var replay = store.replay();

            assertEquals(List.of(baseline, candidate), replay.records());
            assertEquals(Optional.of(baseline), replay.baseline());
            assertEquals(Optional.of(baseline), replay.lastCommitted());
            assertEquals(Optional.of(candidate), replay.chainHead());
        }
    }

    @Test
    void firstRecordMustBeTheCanonicalVerifiedSafeBaselineAndCannotBeReset() {
        EnterpriseLabAllocationState candidate = candidate(
                "allocation-tx-1", 1, EnterpriseLabAllocationState.GENESIS_FINGERPRINT);
        try (EnterpriseLabAllocationStateStore store = ownedStore()) {
            StoreException firstFailure = assertThrows(
                    StoreException.class, () -> store.append(candidate));
            assertEquals(Failure.INVALID_INITIAL_BASELINE, firstFailure.failure());

            EnterpriseLabAllocationState baseline = baseline();
            store.append(baseline);
            StoreException resetFailure = assertThrows(
                    StoreException.class, () -> store.append(baseline()));
            assertEquals(Failure.PREDECESSOR_MISMATCH, resetFailure.failure());
            assertEquals(List.of(baseline), store.replay().records());
        }
    }

    @Test
    void predecessorAndLogicalGenerationMustRemainContiguous() {
        EnterpriseLabAllocationState baseline = baseline();
        try (EnterpriseLabAllocationStateStore store = ownedStore()) {
            store.append(baseline);

            StoreException predecessor = assertThrows(StoreException.class, () -> store.append(
                    candidate("allocation-tx-2", 2, "0".repeat(64))));
            assertEquals(Failure.PREDECESSOR_MISMATCH, predecessor.failure());

            StoreException generation = assertThrows(StoreException.class, () -> store.append(
                    candidate("allocation-tx-3", 3, baseline.currentRecordFingerprint())));
            assertEquals(Failure.ALLOCATION_GENERATION_MISMATCH, generation.failure());
            assertEquals(List.of(baseline), store.replay().records());
        }
    }

    @Test
    void mutationRequiresLiveOwnershipAndExactCurrentGeneration() {
        EnterpriseLabAllocationState staleBaseline = baseline();
        authority.replaceOwner("replacement-owner", 2);

        try (EnterpriseLabAllocationStateStore store = ownedStore()) {
            StoreException stale = assertThrows(
                    StoreException.class, () -> store.append(staleBaseline));
            assertEquals(Failure.OWNER_GENERATION_MISMATCH, stale.failure());

            EnterpriseLabAllocationState currentBaseline = baseline();
            store.append(currentBaseline);
            assertEquals(2, store.replay().chainHead().orElseThrow().ownerGeneration());
        }

        try (EnterpriseLabAllocationStateStore inspection =
                     EnterpriseLabAllocationStateStore.inspect(temporaryDirectory, targets)) {
            assertThrows(EnterpriseLabEvidenceOwnershipException.class,
                    () -> inspection.append(baseline()));
        }
    }

    @Test
    void boundedRecordAndByteLimitsFailBeforeWriting() {
        EnterpriseLabAllocationState baseline = baseline();
        long baselineBytes = new EnterpriseLabAllocationStateCodec(targets).encode(baseline).length + 1L;
        try (EnterpriseLabAllocationStateStore store = EnterpriseLabAllocationStateStore.createForTesting(
                temporaryDirectory,
                targets,
                baselineBytes,
                1,
                authority,
                (checkpoint, bytesWritten) -> { })) {
            store.append(baseline);
            StoreException count = assertThrows(StoreException.class, () -> store.append(
                    candidate("allocation-tx-2", 2, baseline.currentRecordFingerprint())));
            assertEquals(Failure.RECORD_LIMIT_EXCEEDED, count.failure());
            assertEquals(baselineBytes, Files.size(storeFile()));
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }

        Path secondRoot = temporaryDirectory.resolve("size-limit");
        try {
            Files.createDirectory(secondRoot);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
        EnterpriseLabMutationTestAuthority secondAuthority =
                new EnterpriseLabMutationTestAuthority(secondRoot);
        EnterpriseLabAllocationState secondBaseline = create(secondAuthority, NOW, baselineDraft());
        try (EnterpriseLabAllocationStateStore store = EnterpriseLabAllocationStateStore.createForTesting(
                secondRoot,
                targets,
                baselineBytes,
                2,
                secondAuthority,
                (checkpoint, bytesWritten) -> { })) {
            store.append(secondBaseline);
            StoreException size = assertThrows(StoreException.class, () -> store.append(create(
                    secondAuthority,
                    NOW.plusSeconds(1),
                    candidateDraft("allocation-tx-2", 2, secondBaseline.currentRecordFingerprint()))));
            assertEquals(Failure.STORE_SIZE_EXCEEDED, size.failure());
        }
    }

    @Test
    void truncatedCandidateTailFailsClosedWhilePriorBaselineBytesRemainIntact() throws IOException {
        EnterpriseLabAllocationState baseline = baseline();
        EnterpriseLabAllocationState candidate = candidate(
                "allocation-tx-2", 2, baseline.currentRecordFingerprint());
        try (EnterpriseLabAllocationStateStore store = ownedStore()) {
            store.append(baseline);
        }
        byte[] completeBaseline = readStoreBytes();
        byte[] candidateBytes = new EnterpriseLabAllocationStateCodec(targets).encode(candidate);
        Files.write(storeFile(), java.util.Arrays.copyOf(candidateBytes, 37),
                StandardOpenOption.APPEND);

        try (EnterpriseLabAllocationStateStore inspection =
                     EnterpriseLabAllocationStateStore.inspect(temporaryDirectory, targets)) {
            StoreException failure = assertThrows(StoreException.class, inspection::replay);
            assertEquals(Failure.TRUNCATED_TAIL, failure.failure());
        }
        assertArrayEquals(completeBaseline,
                java.util.Arrays.copyOf(readStoreBytes(), completeBaseline.length));
    }

    @Test
    void completeRecordCorruptionFailsClosedWithoutRepairOrOverwrite() throws IOException {
        try (EnterpriseLabAllocationStateStore store = ownedStore()) {
            store.append(baseline());
        }
        byte[] corrupted = readStoreBytes();
        String text = new String(corrupted, StandardCharsets.UTF_8)
                .replace("safe baseline verified", "safe baseline altered");
        Files.write(storeFile(), text.getBytes(StandardCharsets.UTF_8));
        byte[] preserved = readStoreBytes();

        try (EnterpriseLabAllocationStateStore inspection =
                     EnterpriseLabAllocationStateStore.inspect(temporaryDirectory, targets)) {
            StoreException failure = assertThrows(StoreException.class, inspection::replay);
            assertEquals(Failure.CORRUPT_RECORD, failure.failure());
        }
        assertArrayEquals(preserved, readStoreBytes());
    }

    @Test
    void injectedPartialWriteFailsWriterAndRestartDetectsTheTail() throws IOException {
        EnterpriseLabAllocationState baseline = baseline();
        try (EnterpriseLabAllocationStateStore store = ownedStore()) {
            store.append(baseline);
        }
        byte[] completeBaseline = readStoreBytes();
        EnterpriseLabAllocationState candidate = candidate(
                "allocation-tx-2", 2, baseline.currentRecordFingerprint());

        EnterpriseLabAllocationStateStore failing = EnterpriseLabAllocationStateStore.createForTesting(
                temporaryDirectory,
                targets,
                EnterpriseLabAllocationStateStore.HARD_MAX_STORE_BYTES,
                EnterpriseLabAllocationStateStore.HARD_MAX_RECORDS,
                authority,
                (checkpoint, bytesWritten) -> {
                    if (checkpoint == WriteCheckpoint.AFTER_WRITE_CHUNK) {
                        throw new IOException("injected crash after partial record write");
                    }
                });
        StoreException writeFailure = assertThrows(StoreException.class, () -> failing.append(candidate));
        assertEquals(Failure.IO_FAILURE, writeFailure.failure());
        StoreException failedWriter = assertThrows(StoreException.class, () -> failing.append(candidate));
        assertEquals(Failure.WRITER_FAILED, failedWriter.failure());
        failing.close();

        assertArrayEquals(completeBaseline,
                java.util.Arrays.copyOf(readStoreBytes(), completeBaseline.length));
        try (EnterpriseLabAllocationStateStore inspection =
                     EnterpriseLabAllocationStateStore.inspect(temporaryDirectory, targets)) {
            StoreException replayFailure = assertThrows(StoreException.class, inspection::replay);
            assertEquals(Failure.TRUNCATED_TAIL, replayFailure.failure());
        }
    }

    @Test
    void failureAfterForceReportsUncertaintyButRestartVerifiesTheDurableRecord() {
        EnterpriseLabAllocationState baseline = baseline();
        try (EnterpriseLabAllocationStateStore store = ownedStore()) {
            store.append(baseline);
        }
        EnterpriseLabAllocationState candidate = candidate(
                "allocation-tx-2", 2, baseline.currentRecordFingerprint());

        EnterpriseLabAllocationStateStore uncertain = EnterpriseLabAllocationStateStore.createForTesting(
                temporaryDirectory,
                targets,
                EnterpriseLabAllocationStateStore.HARD_MAX_STORE_BYTES,
                EnterpriseLabAllocationStateStore.HARD_MAX_RECORDS,
                authority,
                (checkpoint, bytesWritten) -> {
                    if (checkpoint == WriteCheckpoint.AFTER_SYNC) {
                        throw new IOException("injected loss after force");
                    }
                });
        StoreException failure = assertThrows(StoreException.class, () -> uncertain.append(candidate));
        assertEquals(Failure.IO_FAILURE, failure.failure());
        uncertain.close();

        try (EnterpriseLabAllocationStateStore inspection =
                     EnterpriseLabAllocationStateStore.inspect(temporaryDirectory, targets)) {
            assertEquals(List.of(baseline, candidate), inspection.replay().records());
        }
    }

    @Test
    void processLocalConcurrentWritersAllowOnlyOneChainHeadSuccess() throws Exception {
        EnterpriseLabAllocationState baseline = baseline();
        try (EnterpriseLabAllocationStateStore store = ownedStore()) {
            store.append(baseline);
        }
        EnterpriseLabAllocationState first = candidate(
                "allocation-tx-a", 2, baseline.currentRecordFingerprint());
        EnterpriseLabAllocationState second = candidate(
                "allocation-tx-b", 2, baseline.currentRecordFingerprint());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (EnterpriseLabAllocationStateStore firstStore = ownedStore();
             EnterpriseLabAllocationStateStore secondStore = ownedStore()) {
            List<Callable<Boolean>> calls = List.of(
                    () -> appendSucceeded(firstStore, first),
                    () -> appendSucceeded(secondStore, second));
            List<Boolean> outcomes = executor.invokeAll(calls).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .toList();
            assertEquals(1, outcomes.stream().filter(Boolean::booleanValue).count());
        } finally {
            executor.shutdownNow();
        }

        try (EnterpriseLabAllocationStateStore inspection =
                     EnterpriseLabAllocationStateStore.inspect(temporaryDirectory, targets)) {
            assertEquals(2, inspection.replay().records().size());
        }
    }

    @Test
    void fixedPathRejectsUnexpectedEntriesWrongTypesAndSymlinkNamespace() throws IOException {
        try (EnterpriseLabAllocationStateStore store = ownedStore()) {
            store.append(baseline());
        }
        Files.writeString(storeFile().getParent().resolve("unexpected.txt"), "unexpected");
        try (EnterpriseLabAllocationStateStore inspection =
                     EnterpriseLabAllocationStateStore.inspect(temporaryDirectory, targets)) {
            StoreException unexpected = assertThrows(StoreException.class, inspection::replay);
            assertEquals(Failure.UNEXPECTED_STORAGE_ENTRY, unexpected.failure());
        }

        Path wrongTypeRoot = temporaryDirectory.resolve("wrong-type");
        Files.createDirectory(wrongTypeRoot);
        Path wrongNamespace = wrongTypeRoot.resolve(EnterpriseLabExperimentJournalDirectory.NAMESPACE);
        Files.writeString(wrongNamespace, "not a directory");
        try (EnterpriseLabAllocationStateStore inspection =
                     EnterpriseLabAllocationStateStore.inspect(wrongTypeRoot, targets)) {
            StoreException wrongType = assertThrows(StoreException.class, inspection::replay);
            assertEquals(Failure.SYMLINK_OR_TYPE_ESCAPE, wrongType.failure());
        }

        Path symlinkRoot = temporaryDirectory.resolve("symlink-root");
        Path outside = temporaryDirectory.resolve("outside");
        Files.createDirectory(symlinkRoot);
        Files.createDirectory(outside);
        Path namespace = symlinkRoot.resolve(EnterpriseLabExperimentJournalDirectory.NAMESPACE);
        try {
            Files.createSymbolicLink(namespace, outside);
        } catch (IOException | UnsupportedOperationException exception) {
            Files.writeString(namespace, "symlink creation unavailable; wrong type remains fail closed");
        }
        try (EnterpriseLabAllocationStateStore inspection =
                     EnterpriseLabAllocationStateStore.inspect(symlinkRoot, targets)) {
            StoreException escape = assertThrows(StoreException.class, inspection::replay);
            assertEquals(Failure.SYMLINK_OR_TYPE_ESCAPE, escape.failure());
        }
    }

    @Test
    void relativeOrNonDirectoryRootAndClosedStoreAreRejected() throws IOException {
        StoreException relative = assertThrows(StoreException.class,
                () -> EnterpriseLabAllocationStateStore.inspect(Path.of("relative"), targets));
        assertEquals(Failure.INVALID_TRUSTED_ROOT, relative.failure());

        Path fileRoot = temporaryDirectory.resolve("file-root");
        Files.writeString(fileRoot, "not a directory");
        StoreException file = assertThrows(StoreException.class,
                () -> EnterpriseLabAllocationStateStore.inspect(fileRoot, targets));
        assertEquals(Failure.INVALID_TRUSTED_ROOT, file.failure());

        EnterpriseLabAllocationStateStore closed = ownedStore();
        closed.close();
        StoreException replay = assertThrows(StoreException.class, closed::replay);
        assertEquals(Failure.CLOSED, replay.failure());
        StoreException append = assertThrows(StoreException.class, () -> closed.append(baseline()));
        assertEquals(Failure.CLOSED, append.failure());
    }

    private EnterpriseLabAllocationStateStore ownedStore() {
        return EnterpriseLabAllocationStateStore.createOwned(
                temporaryDirectory, targets, authority);
    }

    private EnterpriseLabAllocationState baseline() {
        return create(authority, NOW, baselineDraft());
    }

    private EnterpriseLabAllocationState candidate(
            String transactionId,
            long allocationGeneration,
            String predecessor) {
        return create(
                authority,
                NOW.plusSeconds(allocationGeneration),
                candidateDraft(transactionId, allocationGeneration, predecessor));
    }

    private Draft baselineDraft() {
        String fingerprint = EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                SCENARIO, BASELINE);
        return new Draft(
                "allocation-baseline-1",
                Optional.empty(),
                SCENARIO,
                1,
                AllocationPurpose.INITIAL_SAFE_BASELINE,
                BASELINE,
                BASELINE,
                BASELINE,
                BASELINE,
                fingerprint,
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                TransactionPhase.COMMITTED,
                new TransitionReason("BASELINE_VERIFIED", "safe baseline verified"),
                true,
                Optional.of(NOW),
                VerificationResult.MATCHED,
                RecoveryClassification.NOT_REQUIRED,
                EnterpriseLabAllocationState.GENESIS_FINGERPRINT,
                Map.of("boundary", "literal-loopback-only"));
    }

    private Draft candidateDraft(
            String transactionId,
            long allocationGeneration,
            String predecessor) {
        String baselineFingerprint = EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                SCENARIO, BASELINE);
        return new Draft(
                transactionId,
                Optional.of("experiment-1"),
                SCENARIO,
                allocationGeneration,
                AllocationPurpose.EXPERIMENT_CANDIDATE,
                BASELINE,
                CANDIDATE,
                CANDIDATE,
                BASELINE,
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                baselineFingerprint,
                TransactionPhase.PREPARED,
                new TransitionReason("INTENT_PREPARED", "candidate intent prepared"),
                false,
                Optional.empty(),
                VerificationResult.NOT_ATTEMPTED,
                RecoveryClassification.NOT_REQUIRED,
                predecessor,
                Map.of("boundary", "literal-loopback-only"));
    }

    private EnterpriseLabAllocationState create(
            EnterpriseLabMutationTestAuthority stateAuthority,
            Instant instant,
            Draft draft) {
        return EnterpriseLabAllocationState.create(
                Clock.fixed(instant, ZoneOffset.UTC), stateAuthority, targets, draft);
    }

    private byte[] readStoreBytes() {
        try {
            return Files.readAllBytes(storeFile());
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private Path storeFile() {
        return temporaryDirectory
                .resolve(EnterpriseLabExperimentJournalDirectory.NAMESPACE)
                .resolve(EnterpriseLabAllocationStateStore.DIRECTORY_NAME)
                .resolve(EnterpriseLabAllocationStateStore.FILE_NAME);
    }

    private static boolean appendSucceeded(
            EnterpriseLabAllocationStateStore store,
            EnterpriseLabAllocationState state) {
        try {
            store.append(state);
            return true;
        } catch (StoreException exception) {
            assertEquals(Failure.PREDECESSOR_MISMATCH, exception.failure());
            return false;
        }
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
}
