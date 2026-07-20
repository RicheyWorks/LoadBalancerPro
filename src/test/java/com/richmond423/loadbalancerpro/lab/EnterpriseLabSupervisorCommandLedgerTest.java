package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.AuthenticationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.DuplicateClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.EventType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.MutationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ResponseClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ValidationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorCommandLedger.Failure;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorCommandLedger.StoreException;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorCommandLedger.SupervisorEventDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorCommandLedger.WriteCheckpoint;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.RequestDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabSupervisorCommandLedgerTest {
    private static final Instant NOW = Instant.parse("2026-07-20T00:00:00Z");
    private static final String OWNER = "a".repeat(64);

    @TempDir
    Path root;

    private EnterpriseLabExperimentTargetCatalog targets;
    private EnterpriseLabSupervisorProtocolCodec codec;
    private Clock clock;

    @BeforeEach
    void setUp() {
        targets = EnterpriseLabSupervisorConfiguration.approvedTargets();
        codec = new EnterpriseLabSupervisorProtocolCodec(targets);
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Test
    void forcedReceiptReplaysExactlyAcrossSupervisorRestart() throws IOException {
        byte[] durableBytes;
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            Request request = observation("command-1", state, NOW);

            var receipt = ledger.append(request, SupervisorEventDraft.receipt(state, NOW));

            assertEquals(1L, receipt.sequence());
            assertEquals(request.requestId(), receipt.correlationId());
            assertEquals(
                    EnterpriseLabSupervisorCommandLedger.SyncPolicy.FORCE_DATA_AND_METADATA,
                    receipt.syncPolicy());
            assertTrue(receipt.exactReadBackVerified());
            assertEquals(EventType.SUPERVISOR_RECEIPT_PERSISTED,
                    ledger.replay().head().orElseThrow().eventType());
            assertEquals(1, EnterpriseLabSupervisorCommandLedger.inspect(root)
                    .replay().events().size());
            durableBytes = Files.readAllBytes(ledger.controlledLedgerFile());
        }

        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState restarted = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            assertArrayEquals(durableBytes, Files.readAllBytes(ledger.controlledLedgerFile()));
            assertEquals(1, ledger.replay().events().size());

            var second = ledger.append(
                    observation("command-2", restarted, NOW.plusSeconds(1)),
                    SupervisorEventDraft.receipt(restarted, NOW.plusSeconds(1)));
            assertEquals(2L, second.sequence());
            assertEquals(2, ledger.replay().unresolvedHeads().size());
        }
    }

    @Test
    void firstCorrelationEventMustBeReceiptAndChangedReuseStartsNewReceiptEpisode() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            Request request = observation("command-1", state, NOW);

            StoreException missing = assertThrows(StoreException.class,
                    () -> ledger.append(request, validationRejected(state)));
            assertEquals(Failure.RECEIPT_MISSING, missing.failure());
            assertFalse(Files.exists(ledger.controlledLedgerFile()));

            ledger.append(request, SupervisorEventDraft.receipt(state, NOW));
            Request changed = observation("command-1", state, NOW.plusSeconds(1));
            assertNotEquals(request.requestFingerprint(), changed.requestFingerprint());
            ledger.append(changed,
                    SupervisorEventDraft.receipt(state, NOW.plusSeconds(1)));
            List<EnterpriseLabCommandLedgerEvent> episodes =
                    ledger.replay().eventsFor(request.requestId());
            assertEquals(2, episodes.size());
            assertNotEquals(episodes.get(0).requestFingerprint(),
                    episodes.get(1).requestFingerprint());
        }
    }

    @Test
    void responseLifecycleIsRequestAndResponseBoundAndRetryStartsWithReceipt() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            Request request = observation("command-1", state, NOW);
            Response response = response(request, state);

            ledger.append(request, SupervisorEventDraft.receipt(state, NOW));
            ledger.append(request, response, responseSent(state, response));
            EnterpriseLabCommandLedgerEvent sent = ledger.replay().head().orElseThrow();
            assertEquals(EventType.RESPONSE_SENT, sent.eventType());
            assertTrue(sent.observes(response));
            assertTrue(ledger.replay().unresolvedHeads().isEmpty());

            ledger.append(request,
                    SupervisorEventDraft.receipt(state, NOW.plusSeconds(2)));
            assertEquals(3, ledger.replay().eventsFor(request.requestId()).size());
            assertEquals(EventType.SUPERVISOR_RECEIPT_PERSISTED,
                    ledger.replay().unresolvedHeads().get(0).eventType());
        }
    }

    @Test
    void allocationMutationReceiptPreservesAbsentApplicationGenerationWithoutInventingOne() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            Request request = applyWithoutAllocationGeneration(state);

            ledger.append(request, SupervisorEventDraft.receipt(state, NOW));

            EnterpriseLabCommandLedgerEvent event = ledger.replay().head().orElseThrow();
            assertEquals(0L, event.allocationGeneration());
            assertEquals(request.transactionId(), event.transactionId());
            assertEquals(request.allocationFingerprint(),
                    event.requestedAllocationFingerprint());
            assertTrue(event.correlates(request));
        }
    }

    @Test
    void applicationSideEventCannotEnterSupervisorLedger() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            SupervisorEventDraft wrongSide = new SupervisorEventDraft(
                    EventType.APPLICATION_INTENT_PERSISTED,
                    state.installedAllocation().allocationFingerprint(),
                    EnterpriseLabCommandLedgerEvent.NONE,
                    state.installedAllocation().routerGeneration(),
                    state.installedAllocation().routerGeneration(),
                    AuthenticationResult.NOT_ATTEMPTED,
                    ValidationResult.NOT_ATTEMPTED,
                    DuplicateClassification.FIRST_OBSERVATION,
                    MutationStatus.NOT_ATTEMPTED,
                    ResponseClassification.NOT_ATTEMPTED,
                    EnterpriseLabCommandLedgerEvent.NONE,
                    0,
                    "WRONG_LEDGER_SIDE",
                    NOW,
                    Map.of());

            assertThrows(IllegalArgumentException.class,
                    () -> ledger.append(observation("command-1", state, NOW), wrongSide));
            assertFalse(Files.exists(ledger.controlledLedgerFile()));
        }
    }

    @Test
    void authenticationRejectionCannotFollowAnAuthenticatedReceipt() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            Request request = observation("command-1", state, NOW);
            ledger.append(request, SupervisorEventDraft.receipt(state, NOW));

            EnterpriseLabInstalledAllocationSnapshot installed = state.installedAllocation();
            SupervisorEventDraft rejected = new SupervisorEventDraft(
                    EventType.AUTHENTICATION_REJECTED,
                    installed.allocationFingerprint(),
                    installed.allocationFingerprint(),
                    installed.routerGeneration(),
                    installed.routerGeneration(),
                    AuthenticationResult.REJECTED,
                    ValidationResult.NOT_ATTEMPTED,
                    DuplicateClassification.NOT_EVALUATED,
                    MutationStatus.NOT_ATTEMPTED,
                    ResponseClassification.REJECTED,
                    EnterpriseLabCommandLedgerEvent.NONE,
                    0,
                    "AUTHENTICATION_REJECTED",
                    NOW.plusMillis(1),
                    Map.of());

            StoreException inconsistent = assertThrows(
                    StoreException.class, () -> ledger.append(request, rejected));
            assertEquals(Failure.ILLEGAL_EVENT_TRANSITION, inconsistent.failure());
            assertEquals(1, ledger.replay().events().size());
        }
    }

    @Test
    void concurrentReceiptsProduceOneContiguousCanonicalChain() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            try {
                List<Callable<Long>> calls = List.of(
                        () -> ledger.append(
                                observation("command-1", state, NOW),
                                SupervisorEventDraft.receipt(state, NOW)).sequence(),
                        () -> ledger.append(
                                observation("command-2", state, NOW.plusSeconds(1)),
                                SupervisorEventDraft.receipt(state, NOW.plusSeconds(1))).sequence());
                List<Long> sequences = executor.invokeAll(calls).stream()
                        .map(future -> {
                            try {
                                return future.get();
                            } catch (Exception exception) {
                                throw new IllegalStateException(exception);
                            }
                        })
                        .sorted()
                        .toList();
                assertEquals(List.of(1L, 2L), sequences);
                assertEquals(List.of(1L, 2L), ledger.replay().events().stream()
                        .map(EnterpriseLabCommandLedgerEvent::sequence)
                        .toList());
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void competingSupervisorCannotAcquireASecondLedgerWriterAuthority() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            service(ownership);
            EnterpriseLabSupervisorCommandLedger.create(ownership);

            EnterpriseLabSupervisorOwnership.OwnershipException competing = assertThrows(
                    EnterpriseLabSupervisorOwnership.OwnershipException.class,
                    () -> EnterpriseLabSupervisorOwnership.acquire(root));
            assertEquals(EnterpriseLabSupervisorOwnership.Failure.LIVE_COMPETING_SUPERVISOR,
                    competing.failure());
        }
    }

    @Test
    void mutationLifecycleRequiresOrderedApplyReadBackAndCommitEvidence() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            Request request = applyWithoutAllocationGeneration(state);
            ledger.append(request, SupervisorEventDraft.receipt(state, NOW));

            StoreException outOfOrder = assertThrows(StoreException.class,
                    () -> ledger.append(request, mutationDraft(
                            state, request, EventType.ALLOCATION_APPLIED,
                            MutationStatus.APPLIED, NOW.plusMillis(2))));
            assertEquals(Failure.ILLEGAL_EVENT_TRANSITION, outOfOrder.failure());
            assertEquals(1, ledger.replay().events().size());

            ledger.append(request, mutationDraft(
                    state, request, EventType.MUTATION_STARTED,
                    MutationStatus.STARTED, NOW.plusMillis(1)));
            ledger.append(request, mutationDraft(
                    state, request, EventType.ALLOCATION_APPLIED,
                    MutationStatus.APPLIED, NOW.plusMillis(2)));
            ledger.append(request, mutationDraft(
                    state, request, EventType.READ_BACK_VERIFIED,
                    MutationStatus.READ_BACK_VERIFIED, NOW.plusMillis(3)));
            ledger.append(request, mutationDraft(
                    state, request, EventType.SUPERVISOR_COMMITTED,
                    MutationStatus.COMMITTED, NOW.plusMillis(4)));

            assertEquals(List.of(
                    EventType.SUPERVISOR_RECEIPT_PERSISTED,
                    EventType.MUTATION_STARTED,
                    EventType.ALLOCATION_APPLIED,
                    EventType.READ_BACK_VERIFIED,
                    EventType.SUPERVISOR_COMMITTED),
                    ledger.replay().events().stream()
                            .map(EnterpriseLabCommandLedgerEvent::eventType)
                            .toList());
        }
    }

    @Test
    void truncatedTailIsPreservedAndRejectedOnReplay() throws IOException {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            ledger.append(observation("command-1", state, NOW),
                    SupervisorEventDraft.receipt(state, NOW));
            Files.writeString(
                    ledger.controlledLedgerFile(),
                    "{\"partial\":",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);

            StoreException truncated = assertThrows(StoreException.class,
                    () -> EnterpriseLabSupervisorCommandLedger.inspect(root).replay());
            assertEquals(Failure.TRUNCATED_TAIL, truncated.failure());
        }
    }

    @Test
    void corruptCompleteEventIsRejectedWithoutRepair() throws IOException {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            ledger.append(observation("command-1", state, NOW),
                    SupervisorEventDraft.receipt(state, NOW));
            Files.writeString(
                    ledger.controlledLedgerFile(),
                    "{\"unexpected\":true}\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);

            StoreException corrupt = assertThrows(StoreException.class,
                    () -> EnterpriseLabSupervisorCommandLedger.inspect(root).replay());
            assertEquals(Failure.CORRUPT_EVENT, corrupt.failure());
        }
    }

    @Test
    void duplicateSequenceAndWrongPredecessorChainsFailClosed() throws IOException {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            ledger.append(observation("command-1", state, NOW),
                    SupervisorEventDraft.receipt(state, NOW));
            Path file = ledger.controlledLedgerFile();
            byte[] validFirstFrame = Files.readAllBytes(file);

            Files.write(file, validFirstFrame, StandardOpenOption.APPEND);
            StoreException duplicate = assertThrows(StoreException.class,
                    () -> EnterpriseLabSupervisorCommandLedger.inspect(root).replay());
            assertEquals(Failure.SEQUENCE_MISMATCH, duplicate.failure());
            Files.write(file, validFirstFrame, StandardOpenOption.TRUNCATE_EXISTING);

            Path otherRoot = root.resolve("independent-chain");
            Files.createDirectory(otherRoot);
            EnterpriseLabCommandLedgerEvent unrelatedSecond;
            try (EnterpriseLabSupervisorOwnership otherOwnership =
                         EnterpriseLabSupervisorOwnership.acquire(otherRoot)) {
                EnterpriseLabSupervisorState otherState = service(otherOwnership).state();
                EnterpriseLabSupervisorCommandLedger otherLedger =
                        EnterpriseLabSupervisorCommandLedger.create(otherOwnership);
                otherLedger.append(observation("other-1", otherState, NOW),
                        SupervisorEventDraft.receipt(otherState, NOW));
                otherLedger.append(observation("other-2", otherState, NOW.plusSeconds(1)),
                        SupervisorEventDraft.receipt(otherState, NOW.plusSeconds(1)));
                unrelatedSecond = otherLedger.replay().head().orElseThrow();
            }
            EnterpriseLabCommandLedgerEventCodec eventCodec =
                    new EnterpriseLabCommandLedgerEventCodec();
            Files.write(file, eventCodec.encode(unrelatedSecond), StandardOpenOption.APPEND);
            Files.writeString(file, "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);

            StoreException predecessor = assertThrows(StoreException.class,
                    () -> EnterpriseLabSupervisorCommandLedger.inspect(root).replay());
            assertEquals(Failure.PREDECESSOR_MISMATCH, predecessor.failure());
        }
    }

    @Test
    void unexpectedEntryAndLedgerTypeEscapeFailClosed() throws IOException {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            Path rogue = ledger.controlledLedgerFile().getParent().resolve("rogue-entry");
            Files.writeString(rogue, "unexpected", StandardCharsets.UTF_8);
            StoreException unexpected = assertThrows(StoreException.class, ledger::replay);
            assertEquals(Failure.UNEXPECTED_STORAGE_ENTRY, unexpected.failure());
            Files.delete(rogue);

            Files.createDirectory(ledger.controlledLedgerFile());
            StoreException escaped = assertThrows(StoreException.class,
                    () -> ledger.append(
                            observation("command-1", state, NOW),
                            SupervisorEventDraft.receipt(state, NOW)));
            assertEquals(Failure.SYMLINK_OR_TYPE_ESCAPE, escaped.failure());
        }
    }

    @Test
    void eventAndByteLimitsRejectBeforeAdditionalAppend() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger oneEvent =
                    EnterpriseLabSupervisorCommandLedger.createForTesting(
                            ownership, EnterpriseLabSupervisorCommandLedger.HARD_MAX_LEDGER_BYTES,
                            1, (point, bytes) -> { });
            oneEvent.append(observation("command-1", state, NOW),
                    SupervisorEventDraft.receipt(state, NOW));
            StoreException eventLimit = assertThrows(StoreException.class,
                    () -> oneEvent.append(
                            observation("command-2", state, NOW.plusSeconds(1)),
                            SupervisorEventDraft.receipt(state, NOW.plusSeconds(1))));
            assertEquals(Failure.EVENT_LIMIT_EXCEEDED, eventLimit.failure());
            assertEquals(1, oneEvent.replay().events().size());
        }

        Path secondRoot = root.resolve("byte-limit");
        try {
            Files.createDirectory(secondRoot);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(secondRoot)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger oneByte =
                    EnterpriseLabSupervisorCommandLedger.createForTesting(
                            ownership, 1L, EnterpriseLabSupervisorCommandLedger.HARD_MAX_EVENTS,
                            (point, bytes) -> { });
            StoreException byteLimit = assertThrows(StoreException.class,
                    () -> oneByte.append(
                            observation("command-1", state, NOW),
                            SupervisorEventDraft.receipt(state, NOW)));
            assertEquals(Failure.LEDGER_SIZE_EXCEEDED, byteLimit.failure());
            assertFalse(Files.exists(oneByte.controlledLedgerFile()));
        }
    }

    @Test
    void ownershipLossLeavesDurableBytesUnchanged() throws IOException {
        EnterpriseLabSupervisorOwnership ownership =
                EnterpriseLabSupervisorOwnership.acquire(root);
        EnterpriseLabSupervisorState state = service(ownership).state();
        EnterpriseLabSupervisorCommandLedger ledger =
                EnterpriseLabSupervisorCommandLedger.create(ownership);
        ledger.append(observation("command-1", state, NOW),
                SupervisorEventDraft.receipt(state, NOW));
        byte[] before = Files.readAllBytes(ledger.controlledLedgerFile());

        ownership.close();
        assertThrows(EnterpriseLabSupervisorOwnership.OwnershipException.class,
                () -> ledger.append(
                        observation("command-2", state, NOW.plusSeconds(1)),
                        SupervisorEventDraft.receipt(state, NOW.plusSeconds(1))));
        assertArrayEquals(before, Files.readAllBytes(ledger.controlledLedgerFile()));
        assertEquals(1, EnterpriseLabSupervisorCommandLedger.inspect(root)
                .replay().events().size());
    }

    @Test
    void uncertainPartialWriteFailsWriterAndLeavesTruncationVisible() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.createForTesting(
                            ownership,
                            EnterpriseLabSupervisorCommandLedger.HARD_MAX_LEDGER_BYTES,
                            EnterpriseLabSupervisorCommandLedger.HARD_MAX_EVENTS,
                            (point, bytes) -> {
                                if (point == WriteCheckpoint.AFTER_WRITE_CHUNK) {
                                    throw new SimulatedFailure();
                                }
                            });

            assertThrows(SimulatedFailure.class,
                    () -> ledger.append(
                            observation("command-1", state, NOW),
                            SupervisorEventDraft.receipt(state, NOW)));
            StoreException failedWriter = assertThrows(StoreException.class,
                    () -> ledger.append(
                            observation("command-2", state, NOW.plusSeconds(1)),
                            SupervisorEventDraft.receipt(state, NOW.plusSeconds(1))));
            assertEquals(Failure.WRITER_FAILED, failedWriter.failure());
            StoreException truncated = assertThrows(StoreException.class,
                    () -> EnterpriseLabSupervisorCommandLedger.inspect(root).replay());
            assertEquals(Failure.TRUNCATED_TAIL, truncated.failure());
        }
    }

    @Test
    void appendWaitingBehindUncertainWriteObservesFailedWriter() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch afterSync = new CountDownLatch(1);
        CountDownLatch releaseFailure = new CountDownLatch(1);
        AtomicReference<Thread> waitingThread = new AtomicReference<>();
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorState state = service(ownership).state();
            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.createForTesting(
                            ownership,
                            EnterpriseLabSupervisorCommandLedger.HARD_MAX_LEDGER_BYTES,
                            EnterpriseLabSupervisorCommandLedger.HARD_MAX_EVENTS,
                            (point, bytes) -> {
                                if (point == WriteCheckpoint.AFTER_SYNC) {
                                    afterSync.countDown();
                                    await(releaseFailure);
                                    throw new SimulatedFailure();
                                }
                            });
            try {
                Future<?> uncertain = executor.submit(() -> ledger.append(
                        observation("command-1", state, NOW),
                        SupervisorEventDraft.receipt(state, NOW)));
                assertTrue(afterSync.await(2, TimeUnit.SECONDS));

                Future<?> waiting = executor.submit(() -> {
                    waitingThread.set(Thread.currentThread());
                    return ledger.append(
                            observation("command-2", state, NOW.plusSeconds(1)),
                            SupervisorEventDraft.receipt(state, NOW.plusSeconds(1)));
                });
                awaitBlocked(waitingThread);
                releaseFailure.countDown();

                assertTrue(assertThrows(ExecutionException.class, uncertain::get)
                        .getCause() instanceof SimulatedFailure);
                ExecutionException rejected = assertThrows(
                        ExecutionException.class, waiting::get);
                StoreException failedWriter = (StoreException) rejected.getCause();
                assertEquals(Failure.WRITER_FAILED, failedWriter.failure());
                assertEquals(1, ledger.replay().events().size());
            } finally {
                releaseFailure.countDown();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void inspectorDoesNotCreateSupervisorOrLedgerPaths() {
        EnterpriseLabSupervisorCommandLedger inspector =
                EnterpriseLabSupervisorCommandLedger.inspect(root);
        assertFalse(inspector.replay().ledgerPresent());
        assertFalse(Files.exists(root.resolve(
                EnterpriseLabSupervisorOwnership.DIRECTORY_NAME)));
        StoreException readOnly = assertThrows(StoreException.class,
                () -> inspector.append(null, (SupervisorEventDraft) null));
        assertEquals(Failure.READ_ONLY, readOnly.failure());
    }

    @Test
    void servicePersistsAuthenticatedReceiptBeforeSupervisorFenceValidation() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(ownership);
            EnterpriseLabSupervisorState before = service.state();
            Request stale = codec.issue(new RequestDraft(
                    "stale-supervisor",
                    CommandType.HEALTH,
                    "observer-app",
                    EnterpriseLabSupervisorProtocol.NONE,
                    0L,
                    before.supervisorInstanceId(),
                    before.supervisorGeneration() + 1L,
                    EnterpriseLabSupervisorProtocol.NONE,
                    Optional.empty(),
                    AllocationPurpose.RECONCILIATION_NO_OP,
                    Optional.empty(),
                    EnterpriseLabSupervisorProtocol.NONE,
                    EnterpriseLabSupervisorProtocol.NONE,
                    NOW,
                    Map.of()));

            Response response = service.dispatch(stale);

            assertEquals(ResponseStatus.REJECTED, response.status());
            assertEquals("STALE_SUPERVISOR_GENERATION", response.reasonCode());
            assertEquals(before, service.state());
            var lifecycle = EnterpriseLabSupervisorCommandLedger.inspect(root)
                    .replay().eventsFor(stale.requestId());
            assertEquals(2, lifecycle.size());
            EnterpriseLabCommandLedgerEvent receipt = lifecycle.get(0);
            assertEquals(EventType.SUPERVISOR_RECEIPT_PERSISTED, receipt.eventType());
            assertEquals(AuthenticationResult.ACCEPTED, receipt.authenticationResult());
            assertEquals(ValidationResult.NOT_ATTEMPTED, receipt.validationResult());
            assertEquals(stale.requestFingerprint(), receipt.requestFingerprint());
            EnterpriseLabCommandLedgerEvent rejected = lifecycle.get(1);
            assertEquals(EventType.VALIDATION_REJECTED, rejected.eventType());
            assertEquals(ValidationResult.REJECTED, rejected.validationResult());

            EnterpriseLabSupervisorCommandLedger ledger =
                    EnterpriseLabSupervisorCommandLedger.create(ownership);
            ledger.append(stale, response, responseSent(before, response));
            EnterpriseLabCommandLedgerEvent sent = ledger.replay().head().orElseThrow();
            assertEquals(EventType.RESPONSE_SENT, sent.eventType());
            assertTrue(sent.observes(response));
            assertNotEquals(sent.supervisorGeneration(), response.supervisorGeneration());
        }
    }

    @Test
    void serviceFailsBeforeMutationWhenReceiptCannotBeDurablyAppended() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorCommandLedger failingLedger =
                    EnterpriseLabSupervisorCommandLedger.createForTesting(
                            ownership,
                            EnterpriseLabSupervisorCommandLedger.HARD_MAX_LEDGER_BYTES,
                            EnterpriseLabSupervisorCommandLedger.HARD_MAX_EVENTS,
                            (point, bytes) -> {
                                if (point == WriteCheckpoint.BEFORE_APPEND) {
                                    throw new SimulatedFailure();
                                }
                            });
            EnterpriseLabSupervisorService service = service(ownership, failingLedger);
            EnterpriseLabSupervisorState before = service.state();

            Response response = service.dispatch(advance(before));

            assertEquals(ResponseStatus.FAILED, response.status());
            assertEquals("SUPERVISOR_COMMAND_FAILED", response.reasonCode());
            assertEquals(before, service.state());
            assertTrue(Files.isRegularFile(failingLedger.controlledLedgerFile()));
            assertTrue(failingLedger.replay().events().isEmpty());
        }
    }

    private EnterpriseLabSupervisorService service(
            EnterpriseLabSupervisorOwnership ownership) {
        return EnterpriseLabSupervisorService.startForTesting(
                ownership,
                targets,
                clock,
                request -> EnterpriseLabSupervisorService.OwnershipVerification.allow(),
                point -> { });
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for test checkpoint");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for test checkpoint", exception);
        }
    }

    private static void awaitBlocked(AtomicReference<Thread> thread) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            Thread candidate = thread.get();
            if (candidate != null && candidate.getState() == Thread.State.BLOCKED) {
                return;
            }
            Thread.onSpinWait();
        }
        throw new IllegalStateException("append did not block behind the in-flight writer");
    }

    private EnterpriseLabSupervisorService service(
            EnterpriseLabSupervisorOwnership ownership,
            EnterpriseLabSupervisorCommandLedger ledger) {
        return EnterpriseLabSupervisorService.startForTesting(
                ownership,
                targets,
                clock,
                request -> EnterpriseLabSupervisorService.OwnershipVerification.allow(),
                point -> { },
                ledger);
    }

    private Request observation(
            String requestId,
            EnterpriseLabSupervisorState state,
            Instant requestedAt) {
        return codec.issue(new RequestDraft(
                requestId,
                CommandType.HEALTH,
                "observer-app",
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                state.supervisorInstanceId(),
                state.supervisorGeneration(),
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabSupervisorProtocol.NONE,
                requestedAt,
                Map.of()));
    }

    private Request advance(EnterpriseLabSupervisorState state) {
        return codec.issue(new RequestDraft(
                "advance-owner",
                CommandType.ADVANCE_APPLICATION_OWNERSHIP,
                "application-owner",
                OWNER,
                1L,
                state.supervisorInstanceId(),
                state.supervisorGeneration(),
                "ownership-handoff-1",
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabSupervisorProtocol.NONE,
                NOW,
                Map.of()));
    }

    private Request applyWithoutAllocationGeneration(EnterpriseLabSupervisorState state) {
        EnterpriseLabLoopbackAllocationSnapshot allocation =
                new EnterpriseLabLoopbackAllocationSnapshot(
                        EnterpriseLabLoopbackAllocationSnapshot.SCHEMA_VERSION,
                        EnterpriseLabSupervisorConfiguration.SCENARIO_ID,
                        1L,
                        "candidate-decision",
                        Kind.CANDIDATE,
                        Map.of("blue", 0.10d, "green", 0.60d, "orange", 0.30d));
        return codec.issue(new RequestDraft(
                "apply-without-generation",
                CommandType.APPLY_ALLOCATION,
                "application-owner",
                OWNER,
                1L,
                state.supervisorInstanceId(),
                state.supervisorGeneration(),
                "allocation-transaction-1",
                Optional.of("experiment-1"),
                AllocationPurpose.EXPERIMENT_CANDIDATE,
                Optional.of(allocation),
                EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                        allocation.scenarioId(), allocation.allocations()),
                state.installedAllocation().allocationFingerprint(),
                NOW,
                Map.of("scope", "literal-loopback-only")));
    }

    private Response response(Request request, EnterpriseLabSupervisorState state) {
        return codec.issue(request, new ResponseDraft(
                request.requestId(),
                request.requestFingerprint(),
                request.commandType(),
                state.supervisorInstanceId(),
                state.supervisorGeneration(),
                0L,
                CommandClassification.OBSERVATION,
                ResponseStatus.ACCEPTED,
                false,
                Optional.of(state.installedAllocation()),
                state.installedAllocation().allocationFingerprint(),
                state.installedAllocation().routerGeneration(),
                state.durableStateGeneration(),
                VerificationResult.NOT_ATTEMPTED,
                "HEALTHY",
                "Supervisor process and durable state are readable",
                NOW.plusSeconds(1)));
    }

    private SupervisorEventDraft validationRejected(EnterpriseLabSupervisorState state) {
        EnterpriseLabInstalledAllocationSnapshot installed = state.installedAllocation();
        return new SupervisorEventDraft(
                EventType.VALIDATION_REJECTED,
                installed.allocationFingerprint(),
                installed.allocationFingerprint(),
                installed.routerGeneration(),
                installed.routerGeneration(),
                AuthenticationResult.ACCEPTED,
                ValidationResult.REJECTED,
                DuplicateClassification.NOT_EVALUATED,
                MutationStatus.NOT_ATTEMPTED,
                ResponseClassification.REJECTED,
                EnterpriseLabCommandLedgerEvent.NONE,
                0,
                "VALIDATION_REJECTED",
                NOW.plusMillis(1),
                Map.of());
    }

    private SupervisorEventDraft responseSent(
            EnterpriseLabSupervisorState state,
            Response response) {
        EnterpriseLabInstalledAllocationSnapshot installed = state.installedAllocation();
        return new SupervisorEventDraft(
                EventType.RESPONSE_SENT,
                installed.allocationFingerprint(),
                installed.allocationFingerprint(),
                installed.routerGeneration(),
                installed.routerGeneration(),
                AuthenticationResult.ACCEPTED,
                response.status() == ResponseStatus.ACCEPTED
                        ? ValidationResult.ACCEPTED
                        : ValidationResult.REJECTED,
                DuplicateClassification.FIRST_OBSERVATION,
                MutationStatus.NOT_ATTEMPTED,
                ResponseClassification.SENT,
                response.responseFingerprint(),
                0,
                "RESPONSE_SENT",
                NOW.plusSeconds(1),
                Map.of());
    }

    private SupervisorEventDraft mutationDraft(
            EnterpriseLabSupervisorState state,
            Request request,
            EventType eventType,
            MutationStatus mutationStatus,
            Instant occurredAt) {
        EnterpriseLabInstalledAllocationSnapshot installed = state.installedAllocation();
        boolean started = eventType == EventType.MUTATION_STARTED;
        String afterFingerprint = started
                ? installed.allocationFingerprint()
                : request.allocationFingerprint();
        long afterGeneration = started
                ? installed.routerGeneration()
                : request.allocation().orElseThrow().revision();
        return new SupervisorEventDraft(
                eventType,
                installed.allocationFingerprint(),
                afterFingerprint,
                installed.routerGeneration(),
                afterGeneration,
                AuthenticationResult.ACCEPTED,
                ValidationResult.ACCEPTED,
                DuplicateClassification.FIRST_OBSERVATION,
                mutationStatus,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE,
                0,
                eventType.name(),
                occurredAt,
                Map.of());
    }

    private static final class SimulatedFailure extends RuntimeException {
    }
}
