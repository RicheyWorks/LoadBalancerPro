package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ApplicationCommitStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.AuthenticationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.Draft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.DuplicateClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.EventType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.LedgerSide;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.MutationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ResponseClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ValidationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceMutationAuthority.MutationAuthorization;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ownership-fenced application-side command evidence. The fixed local file is
 * append-only: startup and every append replay the complete bounded canonical
 * chain, and an uncertain write fails the writer until a fresh replay.
 */
public final class EnterpriseLabApplicationCommandLedger implements AutoCloseable {
    public static final long HARD_MAX_LEDGER_BYTES = 8L * 1024L * 1024L;
    public static final int HARD_MAX_EVENTS = 4_096;

    static final String DIRECTORY_NAME = "application-command-ledger-v1";
    static final String FILE_NAME = "application-command-events-v1.jsonl";

    private static final int READ_BUFFER_BYTES = 8_192;
    private static final int MAX_WRITE_CHUNK_BYTES = 256;
    private static final int MAX_ZERO_PROGRESS = 3;
    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_PERMISSIONS =
            PosixFilePermissions.fromString("rw-------");
    private static final Map<Path, Object> PROCESS_MUTEXES = new ConcurrentHashMap<>();
    private static final Map<Path, Object> ACTIVE_WRITERS = new ConcurrentHashMap<>();
    private static final FailureInjector NO_FAILURE = (checkpoint, bytesWritten) -> { };
    private static final EnterpriseLabEvidenceMutationAuthority READ_ONLY = () -> {
        throw new EnterpriseLabEvidenceOwnershipException(
                EnterpriseLabEvidenceOwnership.FailureClassification.LOCK_LOST,
                "application command ledger has no live ownership capability");
    };
    private static final RequestOwnershipVerifier TEST_OWNERSHIP = (request, authorization) -> { };
    private static final Set<EventType> TERMINAL_EVENTS = Set.of(
            EventType.APPLICATION_COMMITTED,
            EventType.COMMAND_FAILED,
            EventType.COMMAND_QUARANTINED);

    private final Path trustedRoot;
    private final Path namespace;
    private final Path ledgerDirectory;
    private final Path ledgerFile;
    private final EnterpriseLabCommandLedgerEventCodec codec;
    private final EnterpriseLabEvidenceMutationAuthority mutationAuthority;
    private final RequestOwnershipVerifier requestOwnershipVerifier;
    private final long maxLedgerBytes;
    private final int maxEvents;
    private final FailureInjector failureInjector;
    private final Object processMutex;
    private final Object writerClaim;

    private boolean writerClaimReleased;
    private boolean failed;
    private boolean closed;

    private EnterpriseLabApplicationCommandLedger(
            Path trustedRoot,
            long maxLedgerBytes,
            int maxEvents,
            EnterpriseLabEvidenceMutationAuthority mutationAuthority,
            RequestOwnershipVerifier requestOwnershipVerifier,
            FailureInjector failureInjector,
            boolean prepareForMutation) {
        if (maxLedgerBytes < 1L || maxLedgerBytes > HARD_MAX_LEDGER_BYTES
                || maxEvents < 1 || maxEvents > HARD_MAX_EVENTS) {
            throw new IllegalArgumentException(
                    "application ledger test limits must remain within production hard limits");
        }
        this.trustedRoot = validateTrustedRoot(trustedRoot);
        this.codec = new EnterpriseLabCommandLedgerEventCodec();
        this.maxLedgerBytes = maxLedgerBytes;
        this.maxEvents = maxEvents;
        this.mutationAuthority = Objects.requireNonNull(
                mutationAuthority, "mutationAuthority cannot be null");
        this.requestOwnershipVerifier = Objects.requireNonNull(
                requestOwnershipVerifier, "requestOwnershipVerifier cannot be null");
        this.failureInjector = Objects.requireNonNull(
                failureInjector, "failureInjector cannot be null");

        MutationAuthorization authorization = prepareForMutation
                ? requireMutationAuthorization()
                : null;
        this.namespace = prepareForMutation
                ? prepareControlledDirectory(
                        this.trustedRoot, EnterpriseLabExperimentJournalDirectory.NAMESPACE)
                : controlledPath(this.trustedRoot, EnterpriseLabExperimentJournalDirectory.NAMESPACE);
        if (prepareForMutation) {
            requireSameMutationAuthorization(authorization);
        }
        this.ledgerDirectory = prepareForMutation
                ? prepareControlledDirectory(namespace, DIRECTORY_NAME)
                : controlledPath(namespace, DIRECTORY_NAME);
        if (prepareForMutation) {
            requireSameMutationAuthorization(authorization);
        }
        this.ledgerFile = controlledPath(ledgerDirectory, FILE_NAME);
        this.processMutex = PROCESS_MUTEXES.computeIfAbsent(ledgerFile, ignored -> new Object());
        this.writerClaim = prepareForMutation ? new Object() : null;

        if (prepareForMutation) {
            Object existing = ACTIVE_WRITERS.putIfAbsent(ledgerFile, writerClaim);
            if (existing != null) {
                throw failure(Failure.WRITER_ALREADY_ACTIVE,
                        "one process-local application ledger writer is already active");
            }
            boolean ready = false;
            try {
                synchronized (processMutex) {
                    requireSameMutationAuthorization(authorization);
                    replayLocked();
                }
                ready = true;
            } finally {
                if (!ready) {
                    releaseWriterClaim();
                }
            }
        }
    }

    /** Opens the fixed application ledger without creating any path. */
    public static EnterpriseLabApplicationCommandLedger inspect(Path trustedRoot) {
        return new EnterpriseLabApplicationCommandLedger(
                trustedRoot,
                HARD_MAX_LEDGER_BYTES,
                HARD_MAX_EVENTS,
                READ_ONLY,
                TEST_OWNERSHIP,
                NO_FAILURE,
                false);
    }

    /** Opens the one writable application ledger through the live ownership gate. */
    public static EnterpriseLabApplicationCommandLedger create(
            Path trustedRoot,
            EnterpriseLabEvidenceOwnershipGate ownershipGate) {
        EnterpriseLabEvidenceOwnershipGate safeGate = Objects.requireNonNull(
                ownershipGate, "ownershipGate cannot be null");
        return new EnterpriseLabApplicationCommandLedger(
                trustedRoot,
                HARD_MAX_LEDGER_BYTES,
                HARD_MAX_EVENTS,
                safeGate,
                (request, authorization) -> verifyRequestOwnership(
                        safeGate, request, authorization),
                NO_FAILURE,
                true);
    }

    static EnterpriseLabApplicationCommandLedger createOwned(
            Path trustedRoot,
            EnterpriseLabEvidenceMutationAuthority mutationAuthority) {
        return new EnterpriseLabApplicationCommandLedger(
                trustedRoot,
                HARD_MAX_LEDGER_BYTES,
                HARD_MAX_EVENTS,
                mutationAuthority,
                TEST_OWNERSHIP,
                NO_FAILURE,
                true);
    }

    static EnterpriseLabApplicationCommandLedger createForTesting(
            Path trustedRoot,
            long maxLedgerBytes,
            int maxEvents,
            EnterpriseLabEvidenceMutationAuthority mutationAuthority,
            FailureInjector failureInjector) {
        return new EnterpriseLabApplicationCommandLedger(
                trustedRoot,
                maxLedgerBytes,
                maxEvents,
                mutationAuthority,
                TEST_OWNERSHIP,
                failureInjector,
                true);
    }

    /** Appends one request-bound application event with a forced exact read-back. */
    public AppendReceipt append(Request request, ApplicationEventDraft draft) {
        return appendBound(request, null, draft);
    }

    /** Appends one event bound to both the exact request and exact response. */
    public AppendReceipt append(
            Request request,
            Response response,
            ApplicationEventDraft draft) {
        return appendBound(
                request,
                Objects.requireNonNull(response, "response cannot be null"),
                draft);
    }

    /** Deterministically reconstructs the complete application ledger. */
    public synchronized ReadResult replay() {
        ensureReadable();
        synchronized (processMutex) {
            return replayLocked();
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        releaseWriterClaim();
    }

    Path controlledLedgerFile() {
        return ledgerFile;
    }

    private synchronized AppendReceipt appendBound(
            Request request,
            Response response,
            ApplicationEventDraft eventDraft) {
        ensureWritable();
        Request safeRequest = Objects.requireNonNull(request, "request cannot be null");
        ApplicationEventDraft safeDraft = Objects.requireNonNull(
                eventDraft, "eventDraft cannot be null");
        MutationAuthorization authorization = requireMutationAuthorization();
        if (safeRequest.applicationOwnerGeneration() != authorization.generation()) {
            throw failure(Failure.OWNER_GENERATION_MISMATCH,
                    "request generation is not the live application owner generation");
        }
        requestOwnershipVerifier.verify(safeRequest, authorization);

        synchronized (processMutex) {
            requireSameMutationAuthorization(authorization);
            requestOwnershipVerifier.verify(safeRequest, authorization);
            ReadResult before = replayLocked();
            Draft codecDraft = codecDraft(
                    safeRequest,
                    safeDraft,
                    before.events().size() + 1L,
                    before.head()
                            .map(EnterpriseLabCommandLedgerEvent::currentFingerprint)
                            .orElse(EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT));
            EnterpriseLabCommandLedgerEvent event = response == null
                    ? codec.issue(safeRequest, codecDraft)
                    : codec.issue(safeRequest, response, codecDraft);
            validateNext(before.events(), event);
            byte[] encoded = codec.encode(event);
            long frameBytes = encoded.length + 1L;
            if (before.events().size() >= maxEvents) {
                throw failure(Failure.EVENT_LIMIT_EXCEEDED,
                        "application ledger has reached its bounded event count");
            }
            if (before.totalBytes() + frameBytes > maxLedgerBytes) {
                throw failure(Failure.LEDGER_SIZE_EXCEEDED,
                        "application ledger has reached its bounded byte size");
            }

            boolean writeStarted = false;
            try {
                requireSameMutationAuthorization(authorization);
                requestOwnershipVerifier.verify(safeRequest, authorization);
                prepareLedgerFile();
                ReadResult stable = replayLocked();
                if (!stable.events().equals(before.events())
                        || stable.totalBytes() != before.totalBytes()) {
                    throw failure(Failure.CONCURRENT_CHANGE,
                            "application ledger changed before append");
                }
                failureInjector.checkpoint(WriteCheckpoint.BEFORE_APPEND, 0);
                requireSameMutationAuthorization(authorization);
                requestOwnershipVerifier.verify(safeRequest, authorization);
                writeStarted = true;
                appendFrame(encoded);
                failureInjector.checkpoint(
                        WriteCheckpoint.AFTER_APPEND_BEFORE_SYNC, Math.toIntExact(frameBytes));
                requireSameMutationAuthorization(authorization);
                forceLedgerFile();
                failureInjector.checkpoint(
                        WriteCheckpoint.AFTER_SYNC, Math.toIntExact(frameBytes));
                requireSameMutationAuthorization(authorization);
                requestOwnershipVerifier.verify(safeRequest, authorization);

                ReadResult after = replayLocked();
                if (after.events().size() != before.events().size() + 1
                        || after.totalBytes() != before.totalBytes() + frameBytes
                        || !after.head().orElseThrow().equals(event)) {
                    throw failure(Failure.READ_BACK_MISMATCH,
                            "durable application ledger append did not verify exactly");
                }
                return new AppendReceipt(
                        event.sequence(),
                        event.correlationId(),
                        event.currentFingerprint(),
                        after.totalBytes(),
                        SyncPolicy.FORCE_DATA_AND_METADATA,
                        true);
            } catch (IOException exception) {
                if (writeStarted) {
                    failWriter();
                }
                throw failure(Failure.IO_FAILURE,
                        "application ledger append did not complete", exception);
            } catch (RuntimeException exception) {
                if (writeStarted) {
                    failWriter();
                }
                throw exception;
            }
        }
    }

    private ReadResult replayLocked() {
        if (!Files.exists(namespace, LinkOption.NOFOLLOW_LINKS)) {
            return ReadResult.empty();
        }
        validateControlledDirectory(namespace, trustedRoot, "application ledger namespace");
        if (!Files.exists(ledgerDirectory, LinkOption.NOFOLLOW_LINKS)) {
            return ReadResult.empty();
        }
        validateControlledDirectory(
                ledgerDirectory, namespace, "application ledger directory");
        validateOnlyControlledFile();
        if (!Files.exists(ledgerFile, LinkOption.NOFOLLOW_LINKS)) {
            return ReadResult.empty();
        }
        validateLedgerFile();
        byte[] bytes = readBoundedBytes();
        if (bytes.length == 0) {
            return new ReadResult(true, List.of(), 0L);
        }
        if (bytes[bytes.length - 1] != '\n') {
            throw failure(Failure.TRUNCATED_TAIL,
                    "application ledger has an incomplete tail and was preserved unchanged");
        }

        List<EnterpriseLabCommandLedgerEvent> events = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < bytes.length; index++) {
            if (bytes[index] != '\n') {
                continue;
            }
            if (index == start) {
                throw failure(Failure.CORRUPT_EVENT,
                        "application ledger contains an empty complete event");
            }
            if (events.size() >= maxEvents) {
                throw failure(Failure.EVENT_LIMIT_EXCEEDED,
                        "application ledger exceeds its bounded event count");
            }
            byte[] encoded = Arrays.copyOfRange(bytes, start, index);
            EnterpriseLabCommandLedgerEvent event;
            try {
                event = codec.decode(encoded);
            } catch (RuntimeException exception) {
                throw failure(Failure.CORRUPT_EVENT,
                        "application ledger contains an invalid complete event", exception);
            }
            if (!Arrays.equals(encoded, codec.encode(event))) {
                throw failure(Failure.NON_CANONICAL_EVENT,
                        "application ledger contains a non-canonical complete event");
            }
            validateNext(events, event);
            events.add(event);
            start = index + 1;
        }
        return new ReadResult(true, events, bytes.length);
    }

    private static void validateNext(
            List<EnterpriseLabCommandLedgerEvent> prior,
            EnterpriseLabCommandLedgerEvent event) {
        if (event.ledgerSide() != LedgerSide.APPLICATION) {
            throw failure(Failure.LEDGER_SIDE_MISMATCH,
                    "application ledger cannot contain supervisor-side evidence");
        }
        long expectedSequence = prior.size() + 1L;
        if (event.sequence() != expectedSequence) {
            throw failure(Failure.SEQUENCE_MISMATCH,
                    "application ledger event sequence is not contiguous");
        }
        String expectedPredecessor = prior.isEmpty()
                ? EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT
                : prior.get(prior.size() - 1).currentFingerprint();
        if (!expectedPredecessor.equals(event.predecessorFingerprint())) {
            throw failure(Failure.PREDECESSOR_MISMATCH,
                    "application ledger predecessor fingerprint does not match");
        }
        if (!prior.isEmpty()
                && event.applicationOwnerGeneration()
                < prior.get(prior.size() - 1).applicationOwnerGeneration()) {
            throw failure(Failure.OWNER_GENERATION_REGRESSION,
                    "application owner generation regressed in the ledger chain");
        }

        EnterpriseLabCommandLedgerEvent first = null;
        EnterpriseLabCommandLedgerEvent head = null;
        boolean dispatched = false;
        for (EnterpriseLabCommandLedgerEvent candidate : prior) {
            if (!candidate.correlationId().equals(event.correlationId())) {
                continue;
            }
            if (first == null) {
                first = candidate;
            }
            head = candidate;
            dispatched |= candidate.eventType() == EventType.DISPATCH_ATTEMPTED;
        }
        if (first == null) {
            if (event.eventType() != EventType.APPLICATION_INTENT_PERSISTED) {
                throw failure(Failure.INTENT_MISSING,
                        "the first correlation event must be a durable application intent");
            }
            return;
        }
        requireSameCommandIdentity(first, event);
        if (TERMINAL_EVENTS.contains(head.eventType())) {
            throw failure(Failure.EVENT_AFTER_TERMINAL,
                    "application ledger cannot append after a terminal correlation event");
        }
        switch (event.eventType()) {
            case APPLICATION_INTENT_PERSISTED -> throw failure(Failure.DUPLICATE_INTENT,
                    "a command correlation can have only one durable intent");
            case DISPATCH_ATTEMPTED -> {
                if (head.eventType() != EventType.APPLICATION_INTENT_PERSISTED
                        && head.eventType() != EventType.RETRY_ISSUED) {
                    throw failure(Failure.ILLEGAL_EVENT_TRANSITION,
                            "dispatch must follow durable intent or a bounded retry event");
                }
            }
            case RESPONSE_LOST, TIMEOUT_OBSERVED -> {
                if (head.eventType() != EventType.DISPATCH_ATTEMPTED) {
                    throw failure(Failure.ILLEGAL_EVENT_TRANSITION,
                            "loss or timeout evidence must follow a dispatch attempt");
                }
            }
            case RETRY_ISSUED -> {
                if (head.eventType() != EventType.RESPONSE_LOST
                        && head.eventType() != EventType.TIMEOUT_OBSERVED) {
                    throw failure(Failure.ILLEGAL_EVENT_TRANSITION,
                            "retry evidence must follow response loss or timeout");
                }
            }
            case APPLICATION_RESPONSE_RECEIVED -> {
                if (!dispatched) {
                    throw failure(Failure.ILLEGAL_EVENT_TRANSITION,
                            "application response evidence requires an earlier dispatch");
                }
            }
            case APPLICATION_COMMITTED -> {
                if (head.eventType() != EventType.APPLICATION_RESPONSE_RECEIVED
                        && head.eventType() != EventType.RECONCILIATION_COMPLETED) {
                    throw failure(Failure.ILLEGAL_EVENT_TRANSITION,
                            "application commit requires response or reconciliation evidence");
                }
            }
            default -> {
                // Shared failure, quarantine, and reconciliation events retain explicit model fields.
            }
        }
    }

    private static void requireSameCommandIdentity(
            EnterpriseLabCommandLedgerEvent first,
            EnterpriseLabCommandLedgerEvent event) {
        if (!first.requestFingerprint().equals(event.requestFingerprint())
                || !first.transactionId().equals(event.transactionId())
                || !first.experimentId().equals(event.experimentId())
                || first.commandType() != event.commandType()
                || !first.applicationInstanceId().equals(event.applicationInstanceId())
                || first.applicationOwnerGeneration() != event.applicationOwnerGeneration()
                || !first.supervisorInstanceId().equals(event.supervisorInstanceId())
                || first.supervisorGeneration() != event.supervisorGeneration()
                || first.allocationGeneration() != event.allocationGeneration()
                || !first.requestedAllocationFingerprint()
                .equals(event.requestedAllocationFingerprint())
                || !first.previousCommittedFingerprint()
                .equals(event.previousCommittedFingerprint())) {
            throw failure(Failure.CORRELATION_REUSED,
                    "correlation ID was reused with different canonical command identity");
        }
    }

    private static Draft codecDraft(
            Request request,
            ApplicationEventDraft draft,
            long sequence,
            String predecessor) {
        return new Draft(
                LedgerSide.APPLICATION,
                sequence,
                draft.eventType(),
                request.requestId(),
                request.requestFingerprint(),
                request.transactionId(),
                request.experimentId(),
                request.commandType(),
                request.applicationInstanceId(),
                request.applicationOwnerGeneration(),
                request.expectedSupervisorInstanceId(),
                request.expectedSupervisorGeneration(),
                requestAllocationGeneration(request),
                request.allocationFingerprint(),
                request.previousCommittedFingerprint(),
                draft.installedFingerprintBefore(),
                draft.installedFingerprintAfter(),
                draft.routerGenerationBefore(),
                draft.routerGenerationAfter(),
                draft.authenticationResult(),
                draft.validationResult(),
                draft.duplicateClassification(),
                draft.mutationStatus(),
                draft.responseClassification(),
                draft.responseFingerprint(),
                draft.observedSupervisorEventFingerprint(),
                draft.applicationCommitStatus(),
                draft.retryAttempt(),
                draft.reasonCode(),
                draft.occurredAt(),
                draft.metadata(),
                predecessor);
    }

    private static long requestAllocationGeneration(Request request) {
        String value = request.metadata().get("applicationAllocationGeneration");
        if (value == null) {
            return 0L;
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 1L
                    || parsed > EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION
                    || !Long.toString(parsed).equals(value)) {
                return -1L;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }

    private static void verifyRequestOwnership(
            EnterpriseLabEvidenceOwnershipGate ownershipGate,
            Request request,
            MutationAuthorization authorization) {
        OwnershipRecord ownership = ownershipGate.requireCurrentOwnership();
        MutationAuthorization observed = MutationAuthorization.from(
                authorization.trustedRoot(), ownership);
        authorization.requireSameEpoch(observed);
        if (!ownership.owner().applicationInstanceId().equals(request.applicationInstanceId())
                || ownership.generation() != request.applicationOwnerGeneration()
                || !ownership.recordFingerprint()
                .equals(request.applicationOwnershipRecordFingerprint())) {
            throw failure(Failure.OWNER_IDENTITY_MISMATCH,
                    "request identity does not match current durable application ownership");
        }
    }

    private MutationAuthorization requireMutationAuthorization() {
        MutationAuthorization authorization = mutationAuthority.requireMutationAuthorization();
        if (!trustedRoot.equals(authorization.trustedRoot())) {
            throw new EnterpriseLabEvidenceOwnershipException(
                    EnterpriseLabEvidenceOwnership.FailureClassification.DIRECTORY_IDENTITY_MISMATCH,
                    "ownership capability belongs to a different evidence directory");
        }
        return authorization;
    }

    private void requireSameMutationAuthorization(MutationAuthorization expected) {
        Objects.requireNonNull(expected, "expected authorization cannot be null")
                .requireSameEpoch(requireMutationAuthorization());
    }

    private void prepareLedgerFile() throws IOException {
        validateControlledDirectory(
                ledgerDirectory, namespace, "application ledger directory");
        if (Files.exists(ledgerFile, LinkOption.NOFOLLOW_LINKS)) {
            validateLedgerFile();
            return;
        }
        try (FileChannel ignored = openFileForCreation(ledgerFile)) {
            // Creation only; the first event appends after validating the empty file.
        } catch (FileAlreadyExistsException ignored) {
            // A repository-controlled initializer won the process-local race.
        }
        restrictPermissions(ledgerFile, FILE_PERMISSIONS);
        validateLedgerFile();
        forceDirectoryMetadataIfSupported(ledgerDirectory);
    }

    private void appendFrame(byte[] encoded) throws IOException {
        byte[] frame = Arrays.copyOf(encoded, encoded.length + 1);
        frame[frame.length - 1] = '\n';
        try (FileChannel channel = FileChannel.open(
                ledgerFile,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND,
                LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer buffer = ByteBuffer.wrap(frame);
            int writtenBytes = 0;
            int zeroWrites = 0;
            while (buffer.hasRemaining()) {
                int originalLimit = buffer.limit();
                buffer.limit(Math.min(
                        originalLimit, buffer.position() + MAX_WRITE_CHUNK_BYTES));
                int written;
                try {
                    written = channel.write(buffer);
                } finally {
                    buffer.limit(originalLimit);
                }
                if (written == 0) {
                    zeroWrites++;
                    if (zeroWrites >= MAX_ZERO_PROGRESS) {
                        throw new IOException(
                                "bounded application ledger write made no progress");
                    }
                    continue;
                }
                zeroWrites = 0;
                writtenBytes += written;
                failureInjector.checkpoint(
                        WriteCheckpoint.AFTER_WRITE_CHUNK, writtenBytes);
            }
        }
    }

    private void forceLedgerFile() throws IOException {
        try (FileChannel channel = FileChannel.open(
                ledgerFile,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS)) {
            channel.force(true);
        }
    }

    private byte[] readBoundedBytes() {
        long declaredSize;
        try (FileChannel channel = FileChannel.open(
                ledgerFile,
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            declaredSize = channel.size();
            if (declaredSize > maxLedgerBytes) {
                throw failure(Failure.LEDGER_SIZE_EXCEEDED,
                        "application ledger exceeds its bounded byte size");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream(
                    Math.toIntExact(declaredSize));
            ByteBuffer buffer = ByteBuffer.allocate(READ_BUFFER_BYTES);
            int zeroReads = 0;
            long observed = 0L;
            while (true) {
                int read = channel.read(buffer);
                if (read < 0) {
                    break;
                }
                if (read == 0) {
                    zeroReads++;
                    if (zeroReads >= MAX_ZERO_PROGRESS) {
                        throw failure(Failure.IO_FAILURE,
                                "bounded application ledger read made no progress");
                    }
                    continue;
                }
                zeroReads = 0;
                observed += read;
                if (observed > maxLedgerBytes) {
                    throw failure(Failure.LEDGER_SIZE_EXCEEDED,
                            "application ledger exceeds its bounded byte size");
                }
                buffer.flip();
                output.write(buffer.array(), 0, buffer.remaining());
                buffer.clear();
            }
            if (observed != declaredSize || channel.size() != declaredSize) {
                throw failure(Failure.CONCURRENT_CHANGE,
                        "application ledger changed during bounded replay");
            }
            return output.toByteArray();
        } catch (StoreException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE,
                    "application ledger could not be read", exception);
        }
    }

    private void validateOnlyControlledFile() {
        try (var entries = Files.newDirectoryStream(ledgerDirectory)) {
            for (Path entry : entries) {
                Path normalized = entry.toAbsolutePath().normalize();
                if (!normalized.equals(ledgerFile)) {
                    throw failure(Failure.UNEXPECTED_STORAGE_ENTRY,
                            "application ledger directory contains an unexpected entry");
                }
            }
        } catch (StoreException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE,
                    "application ledger directory could not be inspected", exception);
        }
    }

    private void validateLedgerFile() {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    ledgerFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || !attributes.isRegularFile()) {
                throw failure(Failure.SYMLINK_OR_TYPE_ESCAPE,
                        "application ledger must be a non-symbolic-link regular file");
            }
            Path realParent = ledgerDirectory.toRealPath();
            Path realFile = ledgerFile.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!realFile.getParent().equals(realParent)) {
                throw failure(Failure.SYMLINK_OR_TYPE_ESCAPE,
                        "application ledger escaped its controlled directory");
            }
        } catch (StoreException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(Failure.PERMISSION_DENIED,
                    "application ledger permission was denied", exception);
        } catch (IOException exception) {
            throw failure(Failure.STORAGE_UNAVAILABLE,
                    "application ledger could not be inspected", exception);
        }
    }

    private void ensureWritable() {
        if (failed) {
            throw failure(Failure.WRITER_FAILED,
                    "application ledger writer is failed and must be reopened");
        }
        ensureReadable();
    }

    private void ensureReadable() {
        if (closed) {
            throw failure(Failure.CLOSED, "application ledger is closed");
        }
    }

    private void failWriter() {
        failed = true;
        releaseWriterClaim();
    }

    private void releaseWriterClaim() {
        if (writerClaim != null && !writerClaimReleased) {
            writerClaimReleased = true;
            ACTIVE_WRITERS.remove(ledgerFile, writerClaim);
        }
    }

    private static Path validateTrustedRoot(Path value) {
        if (value == null || !value.isAbsolute()) {
            throw failure(Failure.INVALID_TRUSTED_ROOT,
                    "application ledger root must be an explicit absolute path");
        }
        Path normalized = value.normalize();
        if (normalized.getParent() == null) {
            throw failure(Failure.INVALID_TRUSTED_ROOT,
                    "application ledger root must be a non-root local file path");
        }
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    normalized, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
                throw failure(Failure.INVALID_TRUSTED_ROOT,
                        "application ledger root must be an existing non-symbolic-link directory");
            }
            if (!normalized.toRealPath().equals(normalized)) {
                throw failure(Failure.SYMLINK_OR_TYPE_ESCAPE,
                        "application ledger root cannot traverse symbolic links");
            }
            return normalized;
        } catch (StoreException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(Failure.PERMISSION_DENIED,
                    "application ledger root is not accessible", exception);
        } catch (IOException exception) {
            throw failure(Failure.STORAGE_UNAVAILABLE,
                    "application ledger root is unavailable", exception);
        }
    }

    private static Path prepareControlledDirectory(Path parent, String name) {
        Path directory = controlledPath(parent, name);
        try {
            if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    Files.createDirectory(directory, directoryAttribute());
                } catch (UnsupportedOperationException exception) {
                    Files.createDirectory(directory);
                } catch (FileAlreadyExistsException ignored) {
                    // A repository-controlled initializer won the race.
                }
            }
            restrictPermissions(directory, DIRECTORY_PERMISSIONS);
            validateControlledDirectory(
                    directory, parent, "application ledger storage directory");
            return directory;
        } catch (StoreException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(Failure.PERMISSION_DENIED,
                    "application ledger directory permission was denied", exception);
        } catch (IOException exception) {
            throw failure(Failure.STORAGE_UNAVAILABLE,
                    "application ledger directory could not be prepared", exception);
        }
    }

    private static Path controlledPath(Path parent, String name) {
        Path candidate = parent.resolve(name).toAbsolutePath().normalize();
        if (!candidate.getParent().equals(parent)
                || !candidate.startsWith(parent)
                || !candidate.getFileName().toString().equals(name)) {
            throw failure(Failure.SYMLINK_OR_TYPE_ESCAPE,
                    "application ledger path escaped its fixed parent");
        }
        return candidate;
    }

    private static void validateControlledDirectory(
            Path directory,
            Path parent,
            String subject) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
                throw failure(Failure.SYMLINK_OR_TYPE_ESCAPE,
                        subject + " must be a non-symbolic-link directory");
            }
            Path realParent = parent.toRealPath();
            Path realDirectory = directory.toRealPath();
            if (!realDirectory.getParent().equals(realParent)) {
                throw failure(Failure.SYMLINK_OR_TYPE_ESCAPE,
                        subject + " escaped its controlled parent");
            }
        } catch (StoreException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(Failure.PERMISSION_DENIED,
                    subject + " permission was denied", exception);
        } catch (IOException exception) {
            throw failure(Failure.STORAGE_UNAVAILABLE,
                    subject + " could not be inspected", exception);
        }
    }

    private static FileChannel openFileForCreation(Path file) throws IOException {
        try {
            return FileChannel.open(
                    file,
                    Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                            LinkOption.NOFOLLOW_LINKS),
                    fileAttribute());
        } catch (UnsupportedOperationException exception) {
            return FileChannel.open(
                    file,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS);
        }
    }

    private static void restrictPermissions(
            Path path,
            Set<PosixFilePermission> permissions) throws IOException {
        if (Files.getFileAttributeView(
                path,
                java.nio.file.attribute.PosixFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS) != null) {
            Files.setPosixFilePermissions(path, permissions);
        }
    }

    private static void forceDirectoryMetadataIfSupported(Path directory) {
        if (Files.getFileAttributeView(
                directory,
                java.nio.file.attribute.PosixFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS) == null) {
            return;
        }
        try (FileChannel channel = FileChannel.open(
                directory, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            channel.force(true);
        } catch (AccessDeniedException exception) {
            throw failure(Failure.PERMISSION_DENIED,
                    "application ledger directory metadata could not be synchronized",
                    exception);
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(Failure.STORAGE_UNAVAILABLE,
                    "application ledger directory metadata synchronization is unavailable",
                    exception);
        }
    }

    private static FileAttribute<Set<PosixFilePermission>> directoryAttribute() {
        return PosixFilePermissions.asFileAttribute(DIRECTORY_PERMISSIONS);
    }

    private static FileAttribute<Set<PosixFilePermission>> fileAttribute() {
        return PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS);
    }

    private static StoreException failure(Failure classification, String message) {
        return new StoreException(classification, message);
    }

    private static StoreException failure(
            Failure classification,
            String message,
            Throwable cause) {
        return new StoreException(classification, message, cause);
    }

    public record ApplicationEventDraft(
            EventType eventType,
            String installedFingerprintBefore,
            String installedFingerprintAfter,
            long routerGenerationBefore,
            long routerGenerationAfter,
            AuthenticationResult authenticationResult,
            ValidationResult validationResult,
            DuplicateClassification duplicateClassification,
            MutationStatus mutationStatus,
            ResponseClassification responseClassification,
            String responseFingerprint,
            String observedSupervisorEventFingerprint,
            ApplicationCommitStatus applicationCommitStatus,
            int retryAttempt,
            String reasonCode,
            Instant occurredAt,
            Map<String, String> metadata) {

        public ApplicationEventDraft {
            eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
            metadata = Map.copyOf(Objects.requireNonNull(
                    metadata, "metadata cannot be null"));
        }

        public static ApplicationEventDraft intent(
                String installedFingerprintBefore,
                long routerGeneration,
                Instant occurredAt,
                Map<String, String> metadata) {
            return basic(
                    EventType.APPLICATION_INTENT_PERSISTED,
                    installedFingerprintBefore,
                    routerGeneration,
                    ApplicationCommitStatus.PENDING,
                    "APPLICATION_INTENT_DURABLE",
                    occurredAt,
                    metadata);
        }

        public static ApplicationEventDraft dispatch(
                String installedFingerprintBefore,
                long routerGeneration,
                Instant occurredAt,
                Map<String, String> metadata) {
            return basic(
                    EventType.DISPATCH_ATTEMPTED,
                    installedFingerprintBefore,
                    routerGeneration,
                    ApplicationCommitStatus.PENDING,
                    "DISPATCH_ATTEMPTED",
                    occurredAt,
                    metadata);
        }

        private static ApplicationEventDraft basic(
                EventType eventType,
                String installedFingerprintBefore,
                long routerGeneration,
                ApplicationCommitStatus commitStatus,
                String reasonCode,
                Instant occurredAt,
                Map<String, String> metadata) {
            return new ApplicationEventDraft(
                    eventType,
                    installedFingerprintBefore,
                    EnterpriseLabCommandLedgerEvent.NONE,
                    routerGeneration,
                    routerGeneration,
                    AuthenticationResult.NOT_ATTEMPTED,
                    ValidationResult.NOT_ATTEMPTED,
                    DuplicateClassification.FIRST_OBSERVATION,
                    MutationStatus.NOT_ATTEMPTED,
                    ResponseClassification.NOT_ATTEMPTED,
                    EnterpriseLabCommandLedgerEvent.NONE,
                    EnterpriseLabCommandLedgerEvent.NONE,
                    commitStatus,
                    0,
                    reasonCode,
                    occurredAt,
                    metadata);
        }
    }

    public enum SyncPolicy {
        FORCE_DATA_AND_METADATA
    }

    public enum Failure {
        CLOSED,
        WRITER_FAILED,
        WRITER_ALREADY_ACTIVE,
        INVALID_TRUSTED_ROOT,
        PERMISSION_DENIED,
        STORAGE_UNAVAILABLE,
        SYMLINK_OR_TYPE_ESCAPE,
        UNEXPECTED_STORAGE_ENTRY,
        IO_FAILURE,
        CORRUPT_EVENT,
        NON_CANONICAL_EVENT,
        TRUNCATED_TAIL,
        LEDGER_SIDE_MISMATCH,
        SEQUENCE_MISMATCH,
        PREDECESSOR_MISMATCH,
        OWNER_IDENTITY_MISMATCH,
        OWNER_GENERATION_MISMATCH,
        OWNER_GENERATION_REGRESSION,
        CORRELATION_REUSED,
        DUPLICATE_INTENT,
        INTENT_MISSING,
        EVENT_AFTER_TERMINAL,
        ILLEGAL_EVENT_TRANSITION,
        EVENT_LIMIT_EXCEEDED,
        LEDGER_SIZE_EXCEEDED,
        CONCURRENT_CHANGE,
        READ_BACK_MISMATCH
    }

    public static final class StoreException extends IllegalStateException {
        private final Failure failure;

        private StoreException(Failure failure, String message) {
            super(message);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        private StoreException(Failure failure, String message, Throwable cause) {
            super(message, cause);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        public Failure failure() {
            return failure;
        }
    }

    public record AppendReceipt(
            long sequence,
            String correlationId,
            String eventFingerprint,
            long totalBytes,
            SyncPolicy syncPolicy,
            boolean exactReadBackVerified) {

        public AppendReceipt {
            if (sequence < 1L || totalBytes < 1L) {
                throw new IllegalArgumentException(
                        "append receipt sequence and totalBytes must be positive");
            }
            Objects.requireNonNull(correlationId, "correlationId cannot be null");
            Objects.requireNonNull(eventFingerprint, "eventFingerprint cannot be null");
            syncPolicy = Objects.requireNonNull(syncPolicy, "syncPolicy cannot be null");
            if (!exactReadBackVerified) {
                throw new IllegalArgumentException(
                        "application append receipt requires exact durable read-back");
            }
        }
    }

    public record ReadResult(
            boolean ledgerPresent,
            List<EnterpriseLabCommandLedgerEvent> events,
            long totalBytes) {

        public ReadResult {
            events = List.copyOf(Objects.requireNonNull(events, "events cannot be null"));
            if (totalBytes < 0L || (!ledgerPresent && (!events.isEmpty() || totalBytes != 0L))) {
                throw new IllegalArgumentException("application ledger read result is inconsistent");
            }
        }

        static ReadResult empty() {
            return new ReadResult(false, List.of(), 0L);
        }

        public Optional<EnterpriseLabCommandLedgerEvent> head() {
            return events.isEmpty()
                    ? Optional.empty()
                    : Optional.of(events.get(events.size() - 1));
        }

        public List<EnterpriseLabCommandLedgerEvent> eventsFor(String correlationId) {
            Objects.requireNonNull(correlationId, "correlationId cannot be null");
            return events.stream()
                    .filter(event -> event.correlationId().equals(correlationId))
                    .toList();
        }

        /** Returns the latest event for each correlation that has no terminal head. */
        public List<EnterpriseLabCommandLedgerEvent> unresolvedHeads() {
            Map<String, EnterpriseLabCommandLedgerEvent> heads = new LinkedHashMap<>();
            events.forEach(event -> heads.put(event.correlationId(), event));
            return heads.values().stream()
                    .filter(event -> !TERMINAL_EVENTS.contains(event.eventType()))
                    .toList();
        }
    }

    @FunctionalInterface
    interface FailureInjector {
        void checkpoint(WriteCheckpoint checkpoint, int bytesWritten);
    }

    public enum WriteCheckpoint {
        BEFORE_APPEND,
        AFTER_WRITE_CHUNK,
        AFTER_APPEND_BEFORE_SYNC,
        AFTER_SYNC
    }

    @FunctionalInterface
    private interface RequestOwnershipVerifier {
        void verify(Request request, MutationAuthorization authorization);
    }
}
