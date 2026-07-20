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
 * Supervisor-owned append-only command evidence. The existing supervisor OS
 * lock is the cross-process writer authority; a process-local mutex serializes
 * every replay and append against the one fixed bounded JSONL file.
 */
public final class EnterpriseLabSupervisorCommandLedger {
    public static final long HARD_MAX_LEDGER_BYTES = 8L * 1024L * 1024L;
    public static final int HARD_MAX_EVENTS = 4_096;

    static final String DIRECTORY_NAME = "supervisor-command-ledger-v1";
    static final String FILE_NAME = "supervisor-command-events-v1.jsonl";

    private static final int READ_BUFFER_BYTES = 8_192;
    private static final int MAX_WRITE_CHUNK_BYTES = 256;
    private static final int MAX_ZERO_PROGRESS = 3;
    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_PERMISSIONS =
            PosixFilePermissions.fromString("rw-------");
    private static final Map<Path, Object> PROCESS_MUTEXES = new ConcurrentHashMap<>();
    private static final FailureInjector NO_FAILURE = (checkpoint, bytesWritten) -> { };
    private static final Set<EventType> TERMINAL_EVENTS = Set.of(
            EventType.RESPONSE_SENT,
            EventType.COMMAND_QUARANTINED);

    private final Path trustedRoot;
    private final Path supervisorDirectory;
    private final Path ledgerDirectory;
    private final Path ledgerFile;
    private final EnterpriseLabSupervisorOwnership ownership;
    private final EnterpriseLabCommandLedgerEventCodec codec;
    private final long maxLedgerBytes;
    private final int maxEvents;
    private final FailureInjector failureInjector;
    private final Object processMutex;

    private boolean failed;

    private EnterpriseLabSupervisorCommandLedger(
            Path trustedRoot,
            EnterpriseLabSupervisorOwnership ownership,
            long maxLedgerBytes,
            int maxEvents,
            FailureInjector failureInjector,
            boolean prepareForMutation) {
        if (maxLedgerBytes < 1L || maxLedgerBytes > HARD_MAX_LEDGER_BYTES
                || maxEvents < 1 || maxEvents > HARD_MAX_EVENTS) {
            throw new IllegalArgumentException(
                    "supervisor ledger test limits must remain within production hard limits");
        }
        this.trustedRoot = validateTrustedRoot(trustedRoot);
        this.ownership = ownership;
        this.codec = new EnterpriseLabCommandLedgerEventCodec();
        this.maxLedgerBytes = maxLedgerBytes;
        this.maxEvents = maxEvents;
        this.failureInjector = Objects.requireNonNull(
                failureInjector, "failureInjector cannot be null");

        if (prepareForMutation) {
            requireOwnership();
            if (!this.trustedRoot.equals(ownership.trustedRoot())) {
                throw failure(Failure.OWNERSHIP_ROOT_MISMATCH,
                        "supervisor ownership belongs to a different trusted root");
            }
            this.supervisorDirectory = ownership.supervisorDirectory();
            requireOwnership();
            this.ledgerDirectory = prepareControlledDirectory(
                    supervisorDirectory, DIRECTORY_NAME);
            requireOwnership();
        } else {
            this.supervisorDirectory = controlledPath(
                    this.trustedRoot, EnterpriseLabSupervisorOwnership.DIRECTORY_NAME);
            this.ledgerDirectory = controlledPath(supervisorDirectory, DIRECTORY_NAME);
        }
        this.ledgerFile = controlledPath(ledgerDirectory, FILE_NAME);
        this.processMutex = PROCESS_MUTEXES.computeIfAbsent(ledgerFile, ignored -> new Object());

        if (prepareForMutation) {
            synchronized (processMutex) {
                requireOwnership();
                replayLocked();
            }
        }
    }

    /** Opens the fixed supervisor ledger without creating any path. */
    public static EnterpriseLabSupervisorCommandLedger inspect(Path trustedRoot) {
        return new EnterpriseLabSupervisorCommandLedger(
                trustedRoot,
                null,
                HARD_MAX_LEDGER_BYTES,
                HARD_MAX_EVENTS,
                NO_FAILURE,
                false);
    }

    /** Opens the fixed writable ledger through the existing supervisor lock. */
    public static EnterpriseLabSupervisorCommandLedger create(
            EnterpriseLabSupervisorOwnership ownership) {
        EnterpriseLabSupervisorOwnership safe = Objects.requireNonNull(
                ownership, "ownership cannot be null");
        return new EnterpriseLabSupervisorCommandLedger(
                safe.trustedRoot(),
                safe,
                HARD_MAX_LEDGER_BYTES,
                HARD_MAX_EVENTS,
                NO_FAILURE,
                true);
    }

    static EnterpriseLabSupervisorCommandLedger createForTesting(
            EnterpriseLabSupervisorOwnership ownership,
            long maxLedgerBytes,
            int maxEvents,
            FailureInjector failureInjector) {
        EnterpriseLabSupervisorOwnership safe = Objects.requireNonNull(
                ownership, "ownership cannot be null");
        return new EnterpriseLabSupervisorCommandLedger(
                safe.trustedRoot(),
                safe,
                maxLedgerBytes,
                maxEvents,
                failureInjector,
                true);
    }

    /** Appends one exact request-bound supervisor event and verifies its durable replay. */
    public AppendReceipt append(Request request, SupervisorEventDraft draft) {
        return appendBound(request, null, draft);
    }

    /** Appends one supervisor event bound to both the exact request and response. */
    public AppendReceipt append(
            Request request,
            Response response,
            SupervisorEventDraft draft) {
        return appendBound(
                request,
                Objects.requireNonNull(response, "response cannot be null"),
                draft);
    }

    /** Deterministically reconstructs the complete supervisor ledger. */
    public ReadResult replay() {
        synchronized (processMutex) {
            return replayLocked();
        }
    }

    Path controlledLedgerFile() {
        return ledgerFile;
    }

    private AppendReceipt appendBound(
            Request request,
            Response response,
            SupervisorEventDraft eventDraft) {
        synchronized (processMutex) {
            ensureWritable();
            Request safeRequest = Objects.requireNonNull(request, "request cannot be null");
            SupervisorEventDraft safeDraft = Objects.requireNonNull(
                    eventDraft, "eventDraft cannot be null");
            requireOwnership();
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
                        "supervisor ledger has reached its bounded event count");
            }
            if (before.totalBytes() + frameBytes > maxLedgerBytes) {
                throw failure(Failure.LEDGER_SIZE_EXCEEDED,
                        "supervisor ledger has reached its bounded byte size");
            }

            boolean writeStarted = false;
            try {
                requireOwnership();
                prepareLedgerFile();
                ReadResult stable = replayLocked();
                if (!stable.events().equals(before.events())
                        || stable.totalBytes() != before.totalBytes()) {
                    throw failure(Failure.CONCURRENT_CHANGE,
                            "supervisor ledger changed before append");
                }
                failureInjector.checkpoint(WriteCheckpoint.BEFORE_APPEND, 0);
                requireOwnership();
                writeStarted = true;
                appendFrame(encoded);
                failureInjector.checkpoint(
                        WriteCheckpoint.AFTER_APPEND_BEFORE_SYNC,
                        Math.toIntExact(frameBytes));
                requireOwnership();
                forceLedgerFile();
                failureInjector.checkpoint(
                        WriteCheckpoint.AFTER_SYNC, Math.toIntExact(frameBytes));
                requireOwnership();

                ReadResult after = replayLocked();
                if (after.events().size() != before.events().size() + 1
                        || after.totalBytes() != before.totalBytes() + frameBytes
                        || !after.head().orElseThrow().equals(event)) {
                    throw failure(Failure.READ_BACK_MISMATCH,
                            "durable supervisor ledger append did not verify exactly");
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
                    failed = true;
                }
                throw failure(Failure.IO_FAILURE,
                        "supervisor ledger append did not complete", exception);
            } catch (RuntimeException exception) {
                if (writeStarted) {
                    failed = true;
                }
                throw exception;
            }
        }
    }

    private ReadResult replayLocked() {
        if (!Files.exists(supervisorDirectory, LinkOption.NOFOLLOW_LINKS)) {
            return ReadResult.empty();
        }
        validateControlledDirectory(
                supervisorDirectory, trustedRoot, "supervisor storage directory");
        if (!Files.exists(ledgerDirectory, LinkOption.NOFOLLOW_LINKS)) {
            return ReadResult.empty();
        }
        validateControlledDirectory(
                ledgerDirectory, supervisorDirectory, "supervisor ledger directory");
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
                    "supervisor ledger has an incomplete tail and was preserved unchanged");
        }

        List<EnterpriseLabCommandLedgerEvent> events = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < bytes.length; index++) {
            if (bytes[index] != '\n') {
                continue;
            }
            if (index == start) {
                throw failure(Failure.CORRUPT_EVENT,
                        "supervisor ledger contains an empty complete event");
            }
            if (events.size() >= maxEvents) {
                throw failure(Failure.EVENT_LIMIT_EXCEEDED,
                        "supervisor ledger exceeds its bounded event count");
            }
            byte[] encoded = Arrays.copyOfRange(bytes, start, index);
            EnterpriseLabCommandLedgerEvent event;
            try {
                event = codec.decode(encoded);
            } catch (RuntimeException exception) {
                throw failure(Failure.CORRUPT_EVENT,
                        "supervisor ledger contains an invalid complete event", exception);
            }
            if (!Arrays.equals(encoded, codec.encode(event))) {
                throw failure(Failure.NON_CANONICAL_EVENT,
                        "supervisor ledger contains a non-canonical complete event");
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
        if (event.ledgerSide() != LedgerSide.SUPERVISOR) {
            throw failure(Failure.LEDGER_SIDE_MISMATCH,
                    "supervisor ledger cannot contain application-side evidence");
        }
        long expectedSequence = prior.size() + 1L;
        if (event.sequence() != expectedSequence) {
            throw failure(Failure.SEQUENCE_MISMATCH,
                    "supervisor ledger event sequence is not contiguous");
        }
        String expectedPredecessor = prior.isEmpty()
                ? EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT
                : prior.get(prior.size() - 1).currentFingerprint();
        if (!expectedPredecessor.equals(event.predecessorFingerprint())) {
            throw failure(Failure.PREDECESSOR_MISMATCH,
                    "supervisor ledger predecessor fingerprint does not match");
        }

        EnterpriseLabCommandLedgerEvent latestReceipt = null;
        EnterpriseLabCommandLedgerEvent head = null;
        for (EnterpriseLabCommandLedgerEvent candidate : prior) {
            if (!candidate.correlationId().equals(event.correlationId())) {
                continue;
            }
            if (candidate.eventType() == EventType.SUPERVISOR_RECEIPT_PERSISTED) {
                latestReceipt = candidate;
            }
            head = candidate;
        }
        if (head == null) {
            if (event.eventType() != EventType.SUPERVISOR_RECEIPT_PERSISTED) {
                throw failure(Failure.RECEIPT_MISSING,
                        "the first correlation event must be a durable supervisor receipt");
            }
            return;
        }
        if (event.eventType() == EventType.SUPERVISOR_RECEIPT_PERSISTED) {
            if (head.eventType() != EventType.SUPERVISOR_RECEIPT_PERSISTED
                    && !TERMINAL_EVENTS.contains(head.eventType())) {
                throw failure(Failure.ILLEGAL_EVENT_TRANSITION,
                        "a retry receipt cannot interrupt an unresolved supervisor lifecycle");
            }
            return;
        }
        requireSameCommandIdentity(
                Objects.requireNonNull(latestReceipt, "latestReceipt cannot be null"), event);
        if (TERMINAL_EVENTS.contains(head.eventType())) {
            throw failure(Failure.EVENT_AFTER_TERMINAL,
                    "supervisor ledger requires a new receipt after a terminal event");
        }

        switch (event.eventType()) {
            case AUTHENTICATION_REJECTED -> throw failure(
                    Failure.ILLEGAL_EVENT_TRANSITION,
                    "an unauthenticated transport rejection cannot follow a durable receipt");
            case VALIDATION_REJECTED, DUPLICATE_ACCEPTED,
                    DUPLICATE_REJECTED, MUTATION_STARTED ->
                    requireHead(head, EventType.SUPERVISOR_RECEIPT_PERSISTED,
                            "supervisor classification or mutation start must follow receipt");
            case ALLOCATION_APPLIED -> requireHead(
                    head, EventType.MUTATION_STARTED,
                    "allocation apply must follow mutation start");
            case READ_BACK_VERIFIED -> requireHead(
                    head, EventType.ALLOCATION_APPLIED,
                    "read-back verification must follow allocation apply");
            case SUPERVISOR_COMMITTED -> requireHead(
                    head, EventType.READ_BACK_VERIFIED,
                    "supervisor commit must follow read-back verification");
            case RESPONSE_SENT -> {
                if (head.eventType() != EventType.SUPERVISOR_RECEIPT_PERSISTED
                        && head.eventType() != EventType.AUTHENTICATION_REJECTED
                        && head.eventType() != EventType.VALIDATION_REJECTED
                        && head.eventType() != EventType.DUPLICATE_ACCEPTED
                        && head.eventType() != EventType.DUPLICATE_REJECTED
                        && head.eventType() != EventType.SUPERVISOR_COMMITTED
                        && head.eventType() != EventType.COMMAND_FAILED
                        && head.eventType() != EventType.RECONCILIATION_COMPLETED) {
                    throw failure(Failure.ILLEGAL_EVENT_TRANSITION,
                            "response evidence cannot precede a bounded supervisor outcome");
                }
            }
            case COMMAND_FAILED, COMMAND_QUARANTINED, RECONCILIATION_COMPLETED -> {
                // These shared recovery events retain explicit bounded model fields.
            }
            default -> throw failure(Failure.LEDGER_SIDE_MISMATCH,
                    "supervisor ledger received an application-only event");
        }
    }

    private static void requireHead(
            EnterpriseLabCommandLedgerEvent head,
            EventType expected,
            String message) {
        if (head.eventType() != expected) {
            throw failure(Failure.ILLEGAL_EVENT_TRANSITION, message);
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
            SupervisorEventDraft draft,
            long sequence,
            String predecessor) {
        return new Draft(
                LedgerSide.SUPERVISOR,
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
                EnterpriseLabCommandLedgerEvent.NONE,
                ApplicationCommitStatus.NOT_ATTEMPTED,
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

    private void prepareLedgerFile() throws IOException {
        validateControlledDirectory(
                ledgerDirectory, supervisorDirectory, "supervisor ledger directory");
        validateOnlyControlledFile();
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
                                "bounded supervisor ledger write made no progress");
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
                        "supervisor ledger exceeds its bounded byte size");
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
                                "bounded supervisor ledger read made no progress");
                    }
                    continue;
                }
                zeroReads = 0;
                observed += read;
                if (observed > maxLedgerBytes) {
                    throw failure(Failure.LEDGER_SIZE_EXCEEDED,
                            "supervisor ledger exceeds its bounded byte size");
                }
                buffer.flip();
                output.write(buffer.array(), 0, buffer.remaining());
                buffer.clear();
            }
            if (observed != declaredSize || channel.size() != declaredSize) {
                throw failure(Failure.CONCURRENT_CHANGE,
                        "supervisor ledger changed during bounded replay");
            }
            return output.toByteArray();
        } catch (StoreException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE,
                    "supervisor ledger could not be read", exception);
        }
    }

    private void validateOnlyControlledFile() {
        try (var entries = Files.newDirectoryStream(ledgerDirectory)) {
            for (Path entry : entries) {
                Path normalized = entry.toAbsolutePath().normalize();
                if (!normalized.equals(ledgerFile)) {
                    throw failure(Failure.UNEXPECTED_STORAGE_ENTRY,
                            "supervisor ledger directory contains an unexpected entry");
                }
            }
        } catch (StoreException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE,
                    "supervisor ledger directory could not be inspected", exception);
        }
    }

    private void validateLedgerFile() {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    ledgerFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || !attributes.isRegularFile()) {
                throw failure(Failure.SYMLINK_OR_TYPE_ESCAPE,
                        "supervisor ledger must be a non-symbolic-link regular file");
            }
            Path realParent = ledgerDirectory.toRealPath();
            Path realFile = ledgerFile.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!realFile.getParent().equals(realParent)) {
                throw failure(Failure.SYMLINK_OR_TYPE_ESCAPE,
                        "supervisor ledger escaped its controlled directory");
            }
        } catch (StoreException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(Failure.PERMISSION_DENIED,
                    "supervisor ledger permission was denied", exception);
        } catch (IOException exception) {
            throw failure(Failure.STORAGE_UNAVAILABLE,
                    "supervisor ledger could not be inspected", exception);
        }
    }

    private void ensureWritable() {
        if (failed) {
            throw failure(Failure.WRITER_FAILED,
                    "supervisor ledger writer is failed and must be reopened");
        }
        requireOwnership();
    }

    private void requireOwnership() {
        if (ownership == null) {
            throw failure(Failure.READ_ONLY,
                    "supervisor ledger inspector cannot append");
        }
        ownership.requireHeld();
    }

    private static Path validateTrustedRoot(Path value) {
        if (value == null || !value.isAbsolute()) {
            throw failure(Failure.INVALID_TRUSTED_ROOT,
                    "supervisor ledger root must be an explicit absolute path");
        }
        Path normalized = value.normalize();
        if (normalized.getParent() == null) {
            throw failure(Failure.INVALID_TRUSTED_ROOT,
                    "supervisor ledger root must be a non-root local file path");
        }
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    normalized, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
                throw failure(Failure.INVALID_TRUSTED_ROOT,
                        "supervisor ledger root must be an existing non-symbolic-link directory");
            }
            if (!normalized.toRealPath().equals(normalized)) {
                throw failure(Failure.SYMLINK_OR_TYPE_ESCAPE,
                        "supervisor ledger root cannot traverse symbolic links");
            }
            return normalized;
        } catch (StoreException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(Failure.PERMISSION_DENIED,
                    "supervisor ledger root is not accessible", exception);
        } catch (IOException exception) {
            throw failure(Failure.STORAGE_UNAVAILABLE,
                    "supervisor ledger root is unavailable", exception);
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
                    directory, parent, "supervisor ledger storage directory");
            return directory;
        } catch (StoreException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(Failure.PERMISSION_DENIED,
                    "supervisor ledger directory permission was denied", exception);
        } catch (IOException exception) {
            throw failure(Failure.STORAGE_UNAVAILABLE,
                    "supervisor ledger directory could not be prepared", exception);
        }
    }

    private static Path controlledPath(Path parent, String name) {
        Path safeParent = parent.toAbsolutePath().normalize();
        Path candidate = safeParent.resolve(name).toAbsolutePath().normalize();
        if (!candidate.getParent().equals(safeParent)
                || !candidate.startsWith(safeParent)
                || !candidate.getFileName().toString().equals(name)) {
            throw failure(Failure.SYMLINK_OR_TYPE_ESCAPE,
                    "supervisor ledger path escaped its fixed parent");
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
                    "supervisor ledger directory metadata could not be synchronized",
                    exception);
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(Failure.STORAGE_UNAVAILABLE,
                    "supervisor ledger directory metadata synchronization is unavailable",
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

    public record SupervisorEventDraft(
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
            int retryAttempt,
            String reasonCode,
            Instant occurredAt,
            Map<String, String> metadata) {

        public SupervisorEventDraft {
            eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
            metadata = Map.copyOf(Objects.requireNonNull(
                    metadata, "metadata cannot be null"));
        }

        public static SupervisorEventDraft receipt(
                EnterpriseLabSupervisorState state,
                Instant occurredAt) {
            EnterpriseLabSupervisorState safe = Objects.requireNonNull(
                    state, "state cannot be null");
            EnterpriseLabInstalledAllocationSnapshot installed = safe.installedAllocation();
            return new SupervisorEventDraft(
                    EventType.SUPERVISOR_RECEIPT_PERSISTED,
                    installed.allocationFingerprint(),
                    installed.allocationFingerprint(),
                    installed.routerGeneration(),
                    installed.routerGeneration(),
                    AuthenticationResult.ACCEPTED,
                    ValidationResult.NOT_ATTEMPTED,
                    DuplicateClassification.NOT_EVALUATED,
                    MutationStatus.NOT_ATTEMPTED,
                    ResponseClassification.NOT_ATTEMPTED,
                    EnterpriseLabCommandLedgerEvent.NONE,
                    0,
                    "AUTHENTICATED_RECEIPT_DURABLE",
                    Objects.requireNonNull(occurredAt, "occurredAt cannot be null"),
                    Map.of());
        }
    }

    public enum SyncPolicy {
        FORCE_DATA_AND_METADATA
    }

    public enum Failure {
        READ_ONLY,
        WRITER_FAILED,
        INVALID_TRUSTED_ROOT,
        OWNERSHIP_ROOT_MISMATCH,
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
        CORRELATION_REUSED,
        RECEIPT_MISSING,
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
                        "supervisor append receipt requires exact durable read-back");
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
                throw new IllegalArgumentException("supervisor ledger read result is inconsistent");
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

        /** Returns the latest event for each correlation without a terminal head. */
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
}
