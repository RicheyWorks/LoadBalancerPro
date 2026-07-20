package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabApplicationCommandDispatcher.DispatchEvidence;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabApplicationCommandLedger.ApplicationEventDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabApplicationCommandLedger.Failure;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabApplicationCommandLedger.StoreException;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabApplicationCommandLedger.WriteCheckpoint;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ApplicationCommitStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.AuthenticationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.DuplicateClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.EventType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.MutationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ResponseClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ValidationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.Policy;
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabApplicationCommandLedgerTest {
    private static final Instant NOW = Instant.parse("2026-07-19T20:00:00Z");
    private static final String OWNERSHIP = "a".repeat(64);
    private static final String INSTALLED = "b".repeat(64);
    private static final String SUPERVISOR_EVENT = "c".repeat(64);
    private static final String SCENARIO = "tail-latency-pressure";

    @TempDir
    Path temporaryDirectory;

    private EnterpriseLabSupervisorProtocolCodec protocolCodec;
    private EnterpriseLabMutationTestAuthority authority;

    @BeforeEach
    void setUp() {
        protocolCodec = new EnterpriseLabSupervisorProtocolCodec(targetCatalog());
        authority = new EnterpriseLabMutationTestAuthority(temporaryDirectory);
    }

    @Test
    void forcedAppendReplaysExactlyAcrossWriterRestart() throws IOException {
        Request request = request("command-1", 1L, NOW);
        byte[] durableBytes;
        try (EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            var intent = ledger.append(request, intent());
            var dispatch = ledger.append(request, dispatch());

            assertEquals(1L, intent.sequence());
            assertEquals(2L, dispatch.sequence());
            assertEquals(request.requestId(), intent.correlationId());
            assertEquals(EnterpriseLabApplicationCommandLedger.SyncPolicy.FORCE_DATA_AND_METADATA,
                    dispatch.syncPolicy());
            assertTrue(intent.exactReadBackVerified());
            assertTrue(dispatch.exactReadBackVerified());
            assertEquals(List.of(
                    EventType.APPLICATION_INTENT_PERSISTED,
                    EventType.DISPATCH_ATTEMPTED),
                    ledger.replay().events().stream()
                            .map(EnterpriseLabCommandLedgerEvent::eventType)
                            .toList());
            durableBytes = Files.readAllBytes(ledger.controlledLedgerFile());
        }

        try (EnterpriseLabApplicationCommandLedger reopened = ownedLedger()) {
            assertArrayEquals(durableBytes, Files.readAllBytes(reopened.controlledLedgerFile()));
            assertEquals(2, reopened.replay().events().size());
            var next = reopened.append(request("command-2", 1L, NOW.plusSeconds(1)), intent());
            assertEquals(3L, next.sequence());
            assertEquals(2, reopened.replay().unresolvedHeads().size());
        }
    }

    @Test
    void firstEventMustBeIntentAndIntentCannotRepeat() {
        Request request = request("command-1", 1L, NOW);
        try (EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            StoreException missing = assertThrows(
                    StoreException.class, () -> ledger.append(request, dispatch()));
            assertEquals(Failure.INTENT_MISSING, missing.failure());
            assertFalse(ledger.replay().ledgerPresent());

            ledger.append(request, intent());
            StoreException duplicate = assertThrows(
                    StoreException.class, () -> ledger.append(request, intent()));
            assertEquals(Failure.DUPLICATE_INTENT, duplicate.failure());
            assertEquals(1, ledger.replay().events().size());
        }
    }

    @Test
    void correlationReuseWithDifferentRequestContentFailsClosed() {
        Request first = request("command-1", 1L, NOW);
        Request changed = request("command-1", 1L, NOW.plusSeconds(1));
        assertNotEquals(first.requestFingerprint(), changed.requestFingerprint());

        try (EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            ledger.append(first, intent());
            StoreException conflict = assertThrows(
                    StoreException.class, () -> ledger.append(changed, dispatch()));
            assertEquals(Failure.CORRELATION_REUSED, conflict.failure());
            assertEquals(1, ledger.replay().events().size());
        }
    }

    @Test
    void ownerGenerationMustRemainLiveAndCannotRegress() {
        Request generationOne = request("command-1", 1L, NOW);
        try (EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            ledger.append(generationOne, intent());
            StoreException ahead = assertThrows(StoreException.class,
                    () -> ledger.append(request("command-2", 2L, NOW), intent()));
            assertEquals(Failure.OWNER_GENERATION_MISMATCH, ahead.failure());

            authority.replaceOwner("replacement-owner", 2L);
            StoreException stale = assertThrows(StoreException.class,
                    () -> ledger.append(generationOne, dispatch()));
            assertEquals(Failure.OWNER_GENERATION_MISMATCH, stale.failure());
            assertEquals(1, ledger.replay().events().size());
        }

        try (EnterpriseLabApplicationCommandLedger replacement = ownedLedger()) {
            replacement.append(request("command-2", 2L, NOW.plusSeconds(2)), intent());
            assertEquals(2L, replacement.replay().head().orElseThrow()
                    .applicationOwnerGeneration());
        }
    }

    @Test
    void processLocalSecondWriterIsRejectedUntilFirstCloses() {
        EnterpriseLabApplicationCommandLedger first = ownedLedger();
        try {
            StoreException active = assertThrows(
                    StoreException.class, this::ownedLedger);
            assertEquals(Failure.WRITER_ALREADY_ACTIVE, active.failure());
        } finally {
            first.close();
        }
        try (EnterpriseLabApplicationCommandLedger replacement = ownedLedger()) {
            assertFalse(replacement.replay().ledgerPresent());
        }
    }

    @Test
    void concurrentCallsOnOneWriterProduceOneContiguousChain() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            try {
                List<Callable<Long>> calls = List.of(
                        () -> ledger.append(request("command-1", 1L, NOW), intent()).sequence(),
                        () -> ledger.append(request("command-2", 1L, NOW), intent()).sequence());
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
                assertEquals(2, ledger.replay().events().size());
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void truncatedTailIsPreservedAndRejectedOnRestart() throws IOException {
        Path ledgerFile;
        try (EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            ledger.append(request("command-1", 1L, NOW), intent());
            ledgerFile = ledger.controlledLedgerFile();
        }
        byte[] complete = Files.readAllBytes(ledgerFile);
        Files.writeString(
                ledgerFile, "{\"partial\"", StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        try (EnterpriseLabApplicationCommandLedger inspection =
                     EnterpriseLabApplicationCommandLedger.inspect(temporaryDirectory)) {
            StoreException truncated = assertThrows(StoreException.class, inspection::replay);
            assertEquals(Failure.TRUNCATED_TAIL, truncated.failure());
        }
        byte[] observed = Files.readAllBytes(ledgerFile);
        assertEquals(complete.length + "{\"partial\"".getBytes(StandardCharsets.UTF_8).length,
                observed.length);
        assertArrayEquals(complete, java.util.Arrays.copyOf(observed, complete.length));
    }

    @Test
    void completeFingerprintTamperingIsRejectedWithoutRepair() throws IOException {
        Path ledgerFile;
        try (EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            ledger.append(request("command-1", 1L, NOW), intent());
            ledgerFile = ledger.controlledLedgerFile();
        }
        String original = Files.readString(ledgerFile, StandardCharsets.UTF_8);
        String tampered = original.replace("command-1", "command-9");
        assertNotEquals(original, tampered);
        Files.writeString(ledgerFile, tampered, StandardCharsets.UTF_8);

        try (EnterpriseLabApplicationCommandLedger inspection =
                     EnterpriseLabApplicationCommandLedger.inspect(temporaryDirectory)) {
            StoreException corrupt = assertThrows(StoreException.class, inspection::replay);
            assertEquals(Failure.CORRUPT_EVENT, corrupt.failure());
        }
        assertEquals(tampered, Files.readString(ledgerFile, StandardCharsets.UTF_8));
    }

    @Test
    void partialWriteFailsWriterAndFreshReplayClassifiesTail() {
        EnterpriseLabApplicationCommandLedger ledger =
                EnterpriseLabApplicationCommandLedger.createForTesting(
                        temporaryDirectory,
                        EnterpriseLabApplicationCommandLedger.HARD_MAX_LEDGER_BYTES,
                        EnterpriseLabApplicationCommandLedger.HARD_MAX_EVENTS,
                        authority,
                        (checkpoint, bytes) -> {
                            if (checkpoint == WriteCheckpoint.AFTER_WRITE_CHUNK) {
                                throw new IllegalStateException("injected partial write");
                            }
                        });
        try {
            assertThrows(IllegalStateException.class,
                    () -> ledger.append(request("command-1", 1L, NOW), intent()));
            StoreException failed = assertThrows(StoreException.class,
                    () -> ledger.append(request("command-2", 1L, NOW), intent()));
            assertEquals(Failure.WRITER_FAILED, failed.failure());
        } finally {
            ledger.close();
        }

        try (EnterpriseLabApplicationCommandLedger inspection =
                     EnterpriseLabApplicationCommandLedger.inspect(temporaryDirectory)) {
            StoreException truncated = assertThrows(StoreException.class, inspection::replay);
            assertEquals(Failure.TRUNCATED_TAIL, truncated.failure());
        }
    }

    @Test
    void failureAfterSyncLeavesACompleteReplayableIntent() {
        EnterpriseLabApplicationCommandLedger ledger =
                EnterpriseLabApplicationCommandLedger.createForTesting(
                        temporaryDirectory,
                        EnterpriseLabApplicationCommandLedger.HARD_MAX_LEDGER_BYTES,
                        EnterpriseLabApplicationCommandLedger.HARD_MAX_EVENTS,
                        authority,
                        (checkpoint, bytes) -> {
                            if (checkpoint == WriteCheckpoint.AFTER_SYNC) {
                                throw new IllegalStateException("injected uncertain acknowledgement");
                            }
                        });
        try {
            assertThrows(IllegalStateException.class,
                    () -> ledger.append(request("command-1", 1L, NOW), intent()));
        } finally {
            ledger.close();
        }

        try (EnterpriseLabApplicationCommandLedger inspection =
                     EnterpriseLabApplicationCommandLedger.inspect(temporaryDirectory)) {
            var replay = inspection.replay();
            assertEquals(1, replay.events().size());
            assertEquals(EventType.APPLICATION_INTENT_PERSISTED,
                    replay.head().orElseThrow().eventType());
        }
    }

    @Test
    void boundedCountAndByteLimitsRejectBeforeMutation() {
        try (EnterpriseLabApplicationCommandLedger countBound =
                     EnterpriseLabApplicationCommandLedger.createForTesting(
                             temporaryDirectory,
                             EnterpriseLabApplicationCommandLedger.HARD_MAX_LEDGER_BYTES,
                             1,
                             authority,
                             (checkpoint, bytes) -> { })) {
            Request request = request("command-1", 1L, NOW);
            countBound.append(request, intent());
            StoreException count = assertThrows(
                    StoreException.class, () -> countBound.append(request, dispatch()));
            assertEquals(Failure.EVENT_LIMIT_EXCEEDED, count.failure());
        }

        Path another = temporaryDirectory.resolve("byte-bound");
        assertTrue(another.toFile().mkdir());
        EnterpriseLabMutationTestAuthority anotherAuthority =
                new EnterpriseLabMutationTestAuthority(another);
        try (EnterpriseLabApplicationCommandLedger byteBound =
                     EnterpriseLabApplicationCommandLedger.createForTesting(
                             another, 1L, 1, anotherAuthority,
                             (checkpoint, bytes) -> { })) {
            StoreException size = assertThrows(StoreException.class,
                    () -> byteBound.append(request("command-1", 1L, NOW), intent()));
            assertEquals(Failure.LEDGER_SIZE_EXCEEDED, size.failure());
            assertFalse(Files.exists(byteBound.controlledLedgerFile()));
        }
    }

    @Test
    void readOnlyInspectionDoesNotCreatePathsAndUnexpectedEntryFailsClosed()
            throws IOException {
        try (EnterpriseLabApplicationCommandLedger inspection =
                     EnterpriseLabApplicationCommandLedger.inspect(temporaryDirectory)) {
            assertFalse(inspection.replay().ledgerPresent());
        }
        assertFalse(Files.exists(temporaryDirectory.resolve(
                EnterpriseLabExperimentJournalDirectory.NAMESPACE)));

        Path ledgerDirectory;
        try (EnterpriseLabApplicationCommandLedger writer = ownedLedger()) {
            ledgerDirectory = writer.controlledLedgerFile().getParent();
        }
        Files.writeString(ledgerDirectory.resolve("unexpected.txt"), "unexpected");
        try (EnterpriseLabApplicationCommandLedger inspection =
                     EnterpriseLabApplicationCommandLedger.inspect(temporaryDirectory)) {
            StoreException unexpected = assertThrows(StoreException.class, inspection::replay);
            assertEquals(Failure.UNEXPECTED_STORAGE_ENTRY, unexpected.failure());
        }
    }

    @Test
    void relativeRootAndClosedLedgerAreRejected() {
        StoreException relative = assertThrows(StoreException.class,
                () -> EnterpriseLabApplicationCommandLedger.inspect(Path.of("relative")));
        assertEquals(Failure.INVALID_TRUSTED_ROOT, relative.failure());

        EnterpriseLabApplicationCommandLedger closed = ownedLedger();
        closed.close();
        StoreException replay = assertThrows(StoreException.class, closed::replay);
        assertEquals(Failure.CLOSED, replay.failure());
        StoreException append = assertThrows(StoreException.class,
                () -> closed.append(request("command-1", 1L, NOW), intent()));
        assertEquals(Failure.CLOSED, append.failure());
    }

    @Test
    void responseAndApplicationCommitReachATerminalHead() {
        Request request = request("command-1", 1L, NOW);
        Response response = response(request);
        try (EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            ledger.append(request, intent());
            ledger.append(request, dispatch());
            ledger.append(request, response, responseReceived(response));
            ledger.append(request, committed(response));

            assertEquals(EventType.APPLICATION_COMMITTED,
                    ledger.replay().head().orElseThrow().eventType());
            assertTrue(ledger.replay().unresolvedHeads().isEmpty());
            StoreException terminal = assertThrows(
                    StoreException.class, () -> ledger.append(request, dispatch()));
            assertEquals(Failure.EVENT_AFTER_TERMINAL, terminal.failure());
        }
    }

    @Test
    void responseBoundAppendRejectsAResponseForAnotherRequest() {
        Request request = request("command-1", 1L, NOW);
        Response wrong = response(request("command-2", 1L, NOW));
        try (EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            ledger.append(request, intent());
            ledger.append(request, dispatch());
            assertThrows(EnterpriseLabCommandLedgerEventCodec.CodecException.class,
                    () -> ledger.append(request, wrong, responseReceived(wrong)));
            assertEquals(2, ledger.replay().events().size());
        }
    }

    @Test
    void dispatcherInvokesTransportOnlyAfterTwoDurableEvents() {
        Request request = request("command-1", 1L, NOW);
        Response response = response(request);
        AtomicBoolean invoked = new AtomicBoolean();
        try (EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            EnterpriseLabApplicationCommandDispatcher dispatcher =
                    new EnterpriseLabApplicationCommandDispatcher(
                            ledger, Clock.fixed(NOW, ZoneOffset.UTC));
            var result = dispatcher.dispatch(
                    request,
                    evidence(),
                    observed -> {
                        invoked.set(true);
                        assertEquals(request, observed);
                        assertEquals(List.of(
                                EventType.APPLICATION_INTENT_PERSISTED,
                                EventType.DISPATCH_ATTEMPTED),
                                ledger.replay().events().stream()
                                        .map(EnterpriseLabCommandLedgerEvent::eventType)
                                        .toList());
                        return response;
                    });

            assertTrue(invoked.get());
            assertEquals(1L, result.intentReceipt().sequence());
            assertEquals(2L, result.dispatchReceipt().sequence());
            assertEquals(response, result.response());
        }
    }

    @Test
    void verifiedDispatcherBindsResponseSentAndCommitsObservation() {
        try (EnterpriseLabSupervisorOwnership supervisorOwnership =
                     EnterpriseLabSupervisorOwnership.acquire(temporaryDirectory);
             EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            EnterpriseLabSupervisorService service = EnterpriseLabSupervisorService
                    .startForTesting(
                            supervisorOwnership,
                            targetCatalog(),
                            Clock.fixed(NOW, ZoneOffset.UTC),
                            request -> EnterpriseLabSupervisorService
                                    .OwnershipVerification.allow(),
                            point -> { });
            Request request = requestForSupervisor(
                    "verified-command-1", service.state());
            EnterpriseLabApplicationCommandDispatcher dispatcher =
                    new EnterpriseLabApplicationCommandDispatcher(
                            ledger, Clock.fixed(NOW, ZoneOffset.UTC));

            var result = dispatcher.dispatchVerified(
                    request,
                    supervisorEvidence(),
                    observed -> {
                        Response response = service.dispatch(observed);
                        service.recordResponseSent(observed, response);
                        return response;
                    },
                    (observed, response) -> EnterpriseLabSupervisorCommandLedger
                            .inspect(temporaryDirectory)
                            .replay().eventsFor(observed.requestId()).stream()
                            .reduce((first, second) -> second).orElseThrow(),
                    false);

            assertEquals(ResponseStatus.ACCEPTED, result.response().status());
            assertTrue(result.terminalReceipt().isPresent());
            assertEquals(List.of(
                            EventType.APPLICATION_INTENT_PERSISTED,
                            EventType.DISPATCH_ATTEMPTED,
                            EventType.APPLICATION_RESPONSE_RECEIVED,
                            EventType.APPLICATION_COMMITTED),
                    ledger.replay().eventsFor(request.requestId()).stream()
                            .map(EnterpriseLabCommandLedgerEvent::eventType)
                            .toList());
            EnterpriseLabCommandLedgerEvent responseSent =
                    EnterpriseLabSupervisorCommandLedger.inspect(temporaryDirectory)
                            .replay().eventsFor(request.requestId()).get(1);
            assertEquals(EventType.RESPONSE_SENT, responseSent.eventType());
            assertTrue(responseSent.observes(result.response()));
            assertEquals(
                    responseSent.currentFingerprint(),
                    result.supervisorOutcomeFingerprint());
        }
    }

    @Test
    void verifiedDispatcherRetriesOnlyAfterDurableLossWithSameIdentity() {
        try (EnterpriseLabSupervisorOwnership supervisorOwnership =
                     EnterpriseLabSupervisorOwnership.acquire(temporaryDirectory);
             EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            EnterpriseLabSupervisorService service = EnterpriseLabSupervisorService
                    .startForTesting(
                            supervisorOwnership,
                            targetCatalog(),
                            Clock.fixed(NOW, ZoneOffset.UTC),
                            request -> EnterpriseLabSupervisorService
                                    .OwnershipVerification.allow(),
                            point -> { });
            Request request = requestForSupervisor(
                    "verified-retry-1", service.state());
            EnterpriseLabApplicationCommandDispatcher dispatcher =
                    new EnterpriseLabApplicationCommandDispatcher(
                            ledger, Clock.fixed(NOW, ZoneOffset.UTC));
            assertThrows(IllegalStateException.class, () -> dispatcher.dispatchVerified(
                    request,
                    supervisorEvidence(),
                    observed -> {
                        throw new IllegalStateException("injected response loss");
                    },
                    (observed, response) -> {
                        throw new AssertionError("outcome read must follow a response");
                    },
                    false));

            Request conflicting = protocolCodec.issue(new RequestDraft(
                    request.requestId(),
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
                    Map.of("changed", "true")));
            AtomicBoolean conflictingTransport = new AtomicBoolean();
            assertThrows(IllegalStateException.class, () -> dispatcher.dispatchVerified(
                    conflicting,
                    supervisorEvidence(),
                    observed -> {
                        conflictingTransport.set(true);
                        return service.dispatch(observed);
                    },
                    (observed, response) -> {
                        throw new AssertionError("conflict must not read supervisor evidence");
                    },
                    false));
            assertFalse(conflictingTransport.get());

            var retried = dispatcher.dispatchVerified(
                    request,
                    supervisorEvidence(),
                    observed -> {
                        Response response = service.dispatch(observed);
                        service.recordResponseSent(observed, response);
                        return response;
                    },
                    (observed, response) -> EnterpriseLabSupervisorCommandLedger
                            .inspect(temporaryDirectory)
                            .replay().eventsFor(observed.requestId()).stream()
                            .reduce((first, second) -> second).orElseThrow(),
                    false);
            assertEquals(1, retried.retryAttempt());
            assertEquals(List.of(
                            EventType.APPLICATION_INTENT_PERSISTED,
                            EventType.DISPATCH_ATTEMPTED,
                            EventType.RESPONSE_LOST,
                            EventType.RETRY_ISSUED,
                            EventType.DISPATCH_ATTEMPTED,
                            EventType.APPLICATION_RESPONSE_RECEIVED,
                            EventType.APPLICATION_COMMITTED),
                    ledger.replay().eventsFor(request.requestId()).stream()
                            .map(EnterpriseLabCommandLedgerEvent::eventType)
                            .toList());
        }
    }

    @Test
    void verifiedDispatcherRejectsResponseFromOldSupervisorEpochBeforeAcknowledgement() {
        Request oldRequest = request("epoch-command-1", 1L, NOW);
        Response oldResponse = response(oldRequest);
        Request currentRequest = protocolCodec.issue(new RequestDraft(
                oldRequest.requestId(),
                oldRequest.commandType(),
                oldRequest.applicationInstanceId(),
                oldRequest.applicationOwnershipRecordFingerprint(),
                oldRequest.applicationOwnerGeneration(),
                "supervisor-instance-2",
                2L,
                oldRequest.transactionId(),
                oldRequest.experimentId(),
                oldRequest.allocationPurpose(),
                oldRequest.allocation(),
                oldRequest.allocationFingerprint(),
                oldRequest.previousCommittedFingerprint(),
                oldRequest.requestedAt(),
                oldRequest.metadata()));
        AtomicBoolean outcomeRead = new AtomicBoolean();
        try (EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            EnterpriseLabApplicationCommandDispatcher dispatcher =
                    new EnterpriseLabApplicationCommandDispatcher(
                            ledger, Clock.fixed(NOW, ZoneOffset.UTC));
            assertThrows(IllegalStateException.class, () -> dispatcher.dispatchVerified(
                    currentRequest,
                    supervisorEvidence(),
                    observed -> oldResponse,
                    (observed, response) -> {
                        outcomeRead.set(true);
                        throw new AssertionError("old response must fail before evidence read");
                    },
                    false));
            assertFalse(outcomeRead.get());
            assertEquals(List.of(
                            EventType.APPLICATION_INTENT_PERSISTED,
                            EventType.DISPATCH_ATTEMPTED),
                    ledger.replay().eventsFor(currentRequest.requestId()).stream()
                            .map(EnterpriseLabCommandLedgerEvent::eventType)
                            .toList());
        }
    }

    @Test
    void dispatcherNeverInvokesTransportWhenIntentCannotPersist() {
        AtomicBoolean invoked = new AtomicBoolean();
        try (EnterpriseLabApplicationCommandLedger ledger =
                     EnterpriseLabApplicationCommandLedger.createForTesting(
                             temporaryDirectory,
                             1L,
                             1,
                             authority,
                             (checkpoint, bytes) -> { })) {
            EnterpriseLabApplicationCommandDispatcher dispatcher =
                    new EnterpriseLabApplicationCommandDispatcher(
                            ledger, Clock.fixed(NOW, ZoneOffset.UTC));
            StoreException failure = assertThrows(StoreException.class,
                    () -> dispatcher.dispatch(
                            request("command-1", 1L, NOW),
                            evidence(),
                            observed -> {
                                invoked.set(true);
                                return response(observed);
                            }));
            assertEquals(Failure.LEDGER_SIZE_EXCEEDED, failure.failure());
            assertFalse(invoked.get());
            assertFalse(ledger.replay().ledgerPresent());
        }
    }

    @Test
    void dispatcherNeverInvokesTransportWhenDispatchEvidenceCannotPersist() {
        AtomicBoolean invoked = new AtomicBoolean();
        AtomicInteger beforeAppend = new AtomicInteger();
        try (EnterpriseLabApplicationCommandLedger ledger =
                     EnterpriseLabApplicationCommandLedger.createForTesting(
                             temporaryDirectory,
                             EnterpriseLabApplicationCommandLedger.HARD_MAX_LEDGER_BYTES,
                             EnterpriseLabApplicationCommandLedger.HARD_MAX_EVENTS,
                             authority,
                             (checkpoint, bytes) -> {
                                 if (checkpoint == WriteCheckpoint.BEFORE_APPEND
                                         && beforeAppend.incrementAndGet() == 2) {
                                     throw new IllegalStateException(
                                             "injected before dispatch append");
                                 }
                             })) {
            EnterpriseLabApplicationCommandDispatcher dispatcher =
                    new EnterpriseLabApplicationCommandDispatcher(
                            ledger, Clock.fixed(NOW, ZoneOffset.UTC));
            assertThrows(IllegalStateException.class,
                    () -> dispatcher.dispatch(
                            request("command-1", 1L, NOW),
                            evidence(),
                            observed -> {
                                invoked.set(true);
                                return response(observed);
                            }));
            assertFalse(invoked.get());
            assertEquals(List.of(EventType.APPLICATION_INTENT_PERSISTED),
                    ledger.replay().events().stream()
                            .map(EnterpriseLabCommandLedgerEvent::eventType)
                            .toList());
        }
    }

    @Test
    void transportFailureLeavesDispatchAttemptUnresolved() {
        try (EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            EnterpriseLabApplicationCommandDispatcher dispatcher =
                    new EnterpriseLabApplicationCommandDispatcher(
                            ledger, Clock.fixed(NOW, ZoneOffset.UTC));
            assertThrows(IllegalStateException.class,
                    () -> dispatcher.dispatch(
                            request("command-1", 1L, NOW),
                            evidence(),
                            observed -> {
                                throw new IllegalStateException("injected transport failure");
                            }));
            var replay = ledger.replay();
            assertEquals(EventType.DISPATCH_ATTEMPTED,
                    replay.head().orElseThrow().eventType());
            assertEquals(1, replay.unresolvedHeads().size());
        }
    }

    @Test
    void ownershipLossBeforeAppendLeavesLedgerBytesUnchanged() throws IOException {
        try (EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            ledger.append(request("command-1", 1L, NOW), intent());
            byte[] before = Files.readAllBytes(ledger.controlledLedgerFile());
            authority.fail(FailureClassification.LOCK_LOST);
            assertThrows(EnterpriseLabEvidenceOwnershipException.class,
                    () -> ledger.append(request("command-1", 1L, NOW), dispatch()));
            authority.clearFailure();
            assertArrayEquals(before, Files.readAllBytes(ledger.controlledLedgerFile()));
            assertEquals(1, ledger.replay().events().size());
        }
    }

    @Test
    void productionFactoryBindsRequestToTheLiveOwnershipRecord() {
        var acquisition = EnterpriseLabEvidenceOwnershipManager.acquire(
                temporaryDirectory,
                Policy.safetyFirstDefaults(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        assertEquals(OperationStatus.SUCCEEDED, acquisition.result().status());
        try (EnterpriseLabEvidenceOwnershipLease lease =
                     acquisition.ownership().orElseThrow();
             EnterpriseLabApplicationCommandLedger ledger =
                     EnterpriseLabApplicationCommandLedger.create(
                             temporaryDirectory, lease.ownershipGate())) {
            var ownership = lease.record();
            Request exact = request(
                    "command-owned",
                    ownership.owner().applicationInstanceId(),
                    ownership.recordFingerprint(),
                    ownership.generation(),
                    NOW,
                    Map.of());
            ledger.append(exact, intent());

            Request forgedInstance = request(
                    "command-forged",
                    "different-application-instance",
                    ownership.recordFingerprint(),
                    ownership.generation(),
                    NOW,
                    Map.of());
            StoreException mismatch = assertThrows(StoreException.class,
                    () -> ledger.append(forgedInstance, intent()));
            assertEquals(Failure.OWNER_IDENTITY_MISMATCH, mismatch.failure());
            assertEquals(1, ledger.replay().events().size());
        }
    }

    @Test
    void ownershipRenewalAfterIntentKeepsTheSameInFlightCommandValid() {
        MutableClock clock = new MutableClock(NOW);
        var acquisition = EnterpriseLabEvidenceOwnershipManager.acquire(
                temporaryDirectory,
                Policy.safetyFirstDefaults(),
                clock);
        assertEquals(OperationStatus.SUCCEEDED, acquisition.result().status());
        try (EnterpriseLabEvidenceOwnershipLease lease =
                     acquisition.ownership().orElseThrow();
             EnterpriseLabApplicationCommandLedger ledger =
                     EnterpriseLabApplicationCommandLedger.create(
                             temporaryDirectory, lease.ownershipGate())) {
            var original = lease.record();
            Request inFlight = request(
                    "command-renewed-in-flight",
                    original.owner().applicationInstanceId(),
                    original.recordFingerprint(),
                    original.generation(),
                    NOW,
                    Map.of());
            ledger.append(inFlight, intent());

            clock.advance(Duration.ofSeconds(1));
            var renewal = lease.ownershipGate().renew();
            assertEquals(OperationStatus.SUCCEEDED, renewal.status());
            assertNotEquals(original.recordFingerprint(),
                    renewal.record().orElseThrow().recordFingerprint());

            ledger.append(inFlight, dispatch());
            assertEquals(List.of(
                            EventType.APPLICATION_INTENT_PERSISTED,
                            EventType.DISPATCH_ATTEMPTED),
                    ledger.replay().events().stream()
                            .map(EnterpriseLabCommandLedgerEvent::eventType)
                            .toList());

            Request newCommandWithOldRecord = request(
                    "command-stale-record",
                    original.owner().applicationInstanceId(),
                    original.recordFingerprint(),
                    original.generation(),
                    NOW.plusSeconds(1),
                    Map.of());
            StoreException staleRecord = assertThrows(StoreException.class,
                    () -> ledger.append(newCommandWithOldRecord, intent()));
            assertEquals(Failure.OWNER_IDENTITY_MISMATCH, staleRecord.failure());
            assertEquals(2, ledger.replay().events().size());
        }
    }

    @Test
    void boundedCurrentOwnershipOperationPreventsRenewalFromOvertakingNewIntent()
            throws Exception {
        MutableClock clock = new MutableClock(NOW);
        var acquisition = EnterpriseLabEvidenceOwnershipManager.acquire(
                temporaryDirectory,
                Policy.safetyFirstDefaults(),
                clock);
        assertEquals(OperationStatus.SUCCEEDED, acquisition.result().status());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (EnterpriseLabEvidenceOwnershipLease lease =
                     acquisition.ownership().orElseThrow();
             EnterpriseLabApplicationCommandLedger ledger =
                     EnterpriseLabApplicationCommandLedger.create(
                             temporaryDirectory, lease.ownershipGate())) {
            clock.advance(Duration.ofSeconds(1));
            CountDownLatch renewalStarted = new CountDownLatch(1);
            AtomicReference<Future<EnterpriseLabEvidenceOwnership.RenewalResult>>
                    renewal = new AtomicReference<>();

            Request request = lease.ownershipGate().withCurrentOwnership(ownership -> {
                Request current = request(
                        "command-atomic-intent",
                        ownership.owner().applicationInstanceId(),
                        ownership.recordFingerprint(),
                        ownership.generation(),
                        NOW,
                        Map.of());
                renewal.set(executor.submit(() -> {
                    renewalStarted.countDown();
                    return lease.ownershipGate().renew();
                }));
                try {
                    assertTrue(renewalStarted.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                assertFalse(renewal.get().isDone());
                ledger.append(current, intent());
                assertFalse(renewal.get().isDone());
                return current;
            });

            assertEquals(
                    OperationStatus.SUCCEEDED,
                    renewal.get().get(5, TimeUnit.SECONDS).status());
            assertEquals(
                    request.requestFingerprint(),
                    ledger.replay().events().get(0).requestFingerprint());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void supervisorEventAndNoncanonicalAllocationGenerationNeverCreateAFile() {
        Request request = request(
                "command-1",
                "application-instance-1",
                OWNERSHIP,
                1L,
                NOW,
                Map.of("applicationAllocationGeneration", "01"));
        try (EnterpriseLabApplicationCommandLedger ledger = ownedLedger()) {
            assertThrows(IllegalArgumentException.class,
                    () -> ledger.append(request, intent()));
            assertFalse(Files.exists(ledger.controlledLedgerFile()));

            ApplicationEventDraft supervisorEvent = new ApplicationEventDraft(
                    EventType.SUPERVISOR_RECEIPT_PERSISTED,
                    INSTALLED,
                    EnterpriseLabCommandLedgerEvent.NONE,
                    7L,
                    7L,
                    AuthenticationResult.ACCEPTED,
                    ValidationResult.NOT_ATTEMPTED,
                    DuplicateClassification.FIRST_OBSERVATION,
                    MutationStatus.NOT_ATTEMPTED,
                    ResponseClassification.NOT_ATTEMPTED,
                    EnterpriseLabCommandLedgerEvent.NONE,
                    EnterpriseLabCommandLedgerEvent.NONE,
                    ApplicationCommitStatus.PENDING,
                    0,
                    "SUPERVISOR_RECEIPT",
                    NOW,
                    Map.of());
            assertThrows(IllegalArgumentException.class,
                    () -> ledger.append(request("command-2", 1L, NOW), supervisorEvent));
            assertFalse(Files.exists(ledger.controlledLedgerFile()));
        }
    }

    private EnterpriseLabApplicationCommandLedger ownedLedger() {
        return EnterpriseLabApplicationCommandLedger.createOwned(
                temporaryDirectory, authority);
    }

    private Request request(String requestId, long generation, Instant requestedAt) {
        return request(
                requestId,
                "application-instance-" + generation,
                OWNERSHIP,
                generation,
                requestedAt,
                Map.of());
    }

    private Request request(
            String requestId,
            String applicationInstanceId,
            String ownershipFingerprint,
            long generation,
            Instant requestedAt,
            Map<String, String> metadata) {
        return protocolCodec.issue(new RequestDraft(
                requestId,
                CommandType.HEALTH,
                applicationInstanceId,
                ownershipFingerprint,
                generation,
                "supervisor-instance-1",
                1L,
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabSupervisorProtocol.NONE,
                requestedAt,
                metadata));
    }

    private Response response(Request request) {
        return protocolCodec.issue(request, new ResponseDraft(
                request.requestId(),
                request.requestFingerprint(),
                request.commandType(),
                request.expectedSupervisorInstanceId(),
                request.expectedSupervisorGeneration(),
                request.applicationOwnerGeneration(),
                CommandClassification.OBSERVATION,
                ResponseStatus.ACCEPTED,
                false,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                3L,
                VerificationResult.NOT_ATTEMPTED,
                "HEALTHY",
                "Supervisor process and durable state are readable",
                NOW.plusSeconds(1)));
    }

    private Request requestForSupervisor(
            String requestId,
            EnterpriseLabSupervisorState state) {
        return protocolCodec.issue(new RequestDraft(
                requestId,
                CommandType.HEALTH,
                "application-instance-1",
                "a".repeat(64),
                1L,
                state.supervisorInstanceId(),
                state.supervisorGeneration(),
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabSupervisorProtocol.NONE,
                NOW,
                Map.of()));
    }

    private static EnterpriseLabExperimentTargetCatalog targetCatalog() {
        List<EnterpriseLabLoopbackTarget> targets = new ArrayList<>();
        targets.add(new EnterpriseLabLoopbackTarget(
                SCENARIO, "blue", URI.create("http://127.0.0.1:18081/health")));
        targets.add(new EnterpriseLabLoopbackTarget(
                SCENARIO, "green", URI.create("http://127.0.0.1:18082/health")));
        targets.add(new EnterpriseLabLoopbackTarget(
                SCENARIO, "orange", URI.create("http://127.0.0.1:18083/health")));
        return new EnterpriseLabExperimentTargetCatalog(targets);
    }

    private ApplicationEventDraft intent() {
        return ApplicationEventDraft.intent(
                INSTALLED, 7L, NOW, Map.of("boundary", "application-ledger"));
    }

    private ApplicationEventDraft dispatch() {
        return ApplicationEventDraft.dispatch(
                INSTALLED, 7L, NOW.plusMillis(1),
                Map.of("boundary", "application-ledger"));
    }

    private ApplicationEventDraft responseReceived(Response response) {
        return new ApplicationEventDraft(
                EventType.APPLICATION_RESPONSE_RECEIVED,
                INSTALLED,
                EnterpriseLabCommandLedgerEvent.NONE,
                7L,
                7L,
                AuthenticationResult.NOT_ATTEMPTED,
                ValidationResult.ACCEPTED,
                DuplicateClassification.FIRST_OBSERVATION,
                MutationStatus.NOT_ATTEMPTED,
                ResponseClassification.RECEIVED,
                response.responseFingerprint(),
                SUPERVISOR_EVENT,
                ApplicationCommitStatus.PENDING,
                0,
                "APPLICATION_RESPONSE_RECEIVED",
                NOW.plusSeconds(2),
                Map.of("boundary", "application-ledger"));
    }

    private ApplicationEventDraft committed(Response response) {
        return new ApplicationEventDraft(
                EventType.APPLICATION_COMMITTED,
                INSTALLED,
                EnterpriseLabCommandLedgerEvent.NONE,
                7L,
                7L,
                AuthenticationResult.NOT_ATTEMPTED,
                ValidationResult.ACCEPTED,
                DuplicateClassification.FIRST_OBSERVATION,
                MutationStatus.NOT_ATTEMPTED,
                ResponseClassification.RECEIVED,
                response.responseFingerprint(),
                SUPERVISOR_EVENT,
                ApplicationCommitStatus.COMMITTED,
                0,
                "APPLICATION_COMMIT_DURABLE",
                NOW.plusSeconds(3),
                Map.of("boundary", "application-ledger"));
    }

    private DispatchEvidence evidence() {
        return new DispatchEvidence(
                INSTALLED, 7L, Map.of("boundary", "application-ledger"));
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now;

        private MutableClock(Instant initial) {
            now = new AtomicReference<>(initial);
        }

        private void advance(Duration duration) {
            now.updateAndGet(value -> value.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }

    private DispatchEvidence supervisorEvidence() {
        return new DispatchEvidence(
                EnterpriseLabCommandLedgerEvent.NONE,
                0L,
                Map.of("boundary", "supervisor-ipc"));
    }
}
