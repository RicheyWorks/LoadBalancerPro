package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceMutationAuthority.MutationAuthorization;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One fixed, append-only allocation transaction chain beneath the controlled
 * Enterprise Lab evidence namespace. Mutation is possible only through a live
 * ownership authority; read-only replay never manufactures that authority.
 */
public final class EnterpriseLabAllocationStateStore implements AutoCloseable {
    public static final long HARD_MAX_STORE_BYTES = 8L * 1024L * 1024L;
    public static final int HARD_MAX_RECORDS = 4_096;

    static final String DIRECTORY_NAME = "allocation-state-v1";
    static final String FILE_NAME = "allocation-transactions-v1.jsonl";

    private static final int READ_BUFFER_BYTES = 8_192;
    private static final int MAX_WRITE_CHUNK_BYTES = 256;
    private static final int MAX_ZERO_PROGRESS = 3;
    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_PERMISSIONS =
            PosixFilePermissions.fromString("rw-------");
    private static final Map<Path, Object> PROCESS_MUTEXES = new ConcurrentHashMap<>();
    private static final FailureInjector NO_FAILURE = (checkpoint, bytesWritten) -> { };
    private static final EnterpriseLabEvidenceMutationAuthority READ_ONLY = () -> {
        throw new EnterpriseLabEvidenceOwnershipException(
                EnterpriseLabEvidenceOwnership.FailureClassification.LOCK_LOST,
                "allocation store has no live ownership capability");
    };

    private final Path trustedRoot;
    private final Path namespace;
    private final Path storeDirectory;
    private final Path storeFile;
    private final EnterpriseLabAllocationStateCodec codec;
    private final EnterpriseLabEvidenceMutationAuthority mutationAuthority;
    private final long maxStoreBytes;
    private final int maxRecords;
    private final FailureInjector failureInjector;
    private final Object processMutex;

    private boolean failed;
    private boolean closed;

    private EnterpriseLabAllocationStateStore(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            long maxStoreBytes,
            int maxRecords,
            EnterpriseLabEvidenceMutationAuthority mutationAuthority,
            FailureInjector failureInjector,
            boolean prepareForMutation) {
        if (maxStoreBytes < 1 || maxStoreBytes > HARD_MAX_STORE_BYTES
                || maxRecords < 1 || maxRecords > HARD_MAX_RECORDS) {
            throw new IllegalArgumentException(
                    "allocation store test limits must remain within production hard limits");
        }
        this.trustedRoot = validateTrustedRoot(trustedRoot);
        this.codec = new EnterpriseLabAllocationStateCodec(
                Objects.requireNonNull(targetCatalog, "targetCatalog cannot be null"));
        this.maxStoreBytes = maxStoreBytes;
        this.maxRecords = maxRecords;
        this.mutationAuthority = Objects.requireNonNull(
                mutationAuthority, "mutationAuthority cannot be null");
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
        this.storeDirectory = prepareForMutation
                ? prepareControlledDirectory(namespace, DIRECTORY_NAME)
                : controlledPath(namespace, DIRECTORY_NAME);
        if (prepareForMutation) {
            requireSameMutationAuthorization(authorization);
        }
        this.storeFile = controlledPath(storeDirectory, FILE_NAME);
        this.processMutex = PROCESS_MUTEXES.computeIfAbsent(storeFile, ignored -> new Object());
    }

    /** Opens the fixed store for bounded read-only inspection without creating paths. */
    public static EnterpriseLabAllocationStateStore inspect(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog) {
        return new EnterpriseLabAllocationStateStore(
                trustedRoot,
                targetCatalog,
                HARD_MAX_STORE_BYTES,
                HARD_MAX_RECORDS,
                READ_ONLY,
                NO_FAILURE,
                false);
    }

    /** Opens the fixed store for ownership-fenced durable mutation. */
    public static EnterpriseLabAllocationStateStore create(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabEvidenceOwnershipGate ownershipGate) {
        return createOwned(trustedRoot, targetCatalog, ownershipGate);
    }

    static EnterpriseLabAllocationStateStore createOwned(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabEvidenceMutationAuthority mutationAuthority) {
        return new EnterpriseLabAllocationStateStore(
                trustedRoot,
                targetCatalog,
                HARD_MAX_STORE_BYTES,
                HARD_MAX_RECORDS,
                mutationAuthority,
                NO_FAILURE,
                true);
    }

    static EnterpriseLabAllocationStateStore createForTesting(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            long maxStoreBytes,
            int maxRecords,
            EnterpriseLabEvidenceMutationAuthority mutationAuthority,
            FailureInjector failureInjector) {
        return new EnterpriseLabAllocationStateStore(
                trustedRoot,
                targetCatalog,
                maxStoreBytes,
                maxRecords,
                mutationAuthority,
                failureInjector,
                true);
    }

    /**
     * Appends, synchronizes, replays, and exactly verifies one immutable record.
     * A post-write uncertainty fails the writer; callers must reopen and replay.
     */
    public synchronized AppendReceipt append(EnterpriseLabAllocationState state) {
        ensureWritable();
        EnterpriseLabAllocationState safe = Objects.requireNonNull(state, "state cannot be null");
        MutationAuthorization authorization = requireMutationAuthorization();
        if (safe.ownerGeneration() != authorization.generation()) {
            throw failure(Failure.OWNER_GENERATION_MISMATCH,
                    "allocation record owner generation is not the live owner generation");
        }
        synchronized (processMutex) {
            requireSameMutationAuthorization(authorization);
            ReadResult before = replayLocked();
            validateNext(before.records(), safe);
            byte[] encoded = codec.encode(safe);
            long frameBytes = encoded.length + 1L;
            if (before.records().size() >= maxRecords) {
                throw failure(Failure.RECORD_LIMIT_EXCEEDED,
                        "allocation store has reached its bounded record count");
            }
            if (before.totalBytes() + frameBytes > maxStoreBytes) {
                throw failure(Failure.STORE_SIZE_EXCEEDED,
                        "allocation store has reached its bounded byte size");
            }

            boolean writeStarted = false;
            try {
                requireSameMutationAuthorization(authorization);
                prepareStoreFile();
                ReadResult stable = replayLocked();
                if (!stable.records().equals(before.records())
                        || stable.totalBytes() != before.totalBytes()) {
                    throw failure(Failure.CONCURRENT_CHANGE,
                            "allocation store changed before append");
                }
                failureInjector.checkpoint(WriteCheckpoint.BEFORE_APPEND, 0);
                requireSameMutationAuthorization(authorization);
                writeStarted = true;
                appendFrame(encoded);
                requireSameMutationAuthorization(authorization);
                failureInjector.checkpoint(
                        WriteCheckpoint.AFTER_APPEND_BEFORE_SYNC, Math.toIntExact(frameBytes));
                forceStoreFile();
                failureInjector.checkpoint(
                        WriteCheckpoint.AFTER_SYNC, Math.toIntExact(frameBytes));
                requireSameMutationAuthorization(authorization);

                ReadResult after = replayLocked();
                if (after.records().size() != before.records().size() + 1
                        || after.totalBytes() != before.totalBytes() + frameBytes
                        || !after.records().get(after.records().size() - 1).equals(safe)) {
                    throw failure(Failure.READ_BACK_MISMATCH,
                            "durable allocation append did not verify exactly");
                }
                return new AppendReceipt(
                        after.records().size(),
                        safe.currentRecordFingerprint(),
                        after.totalBytes(),
                        SyncPolicy.FORCE_DATA_AND_METADATA,
                        true);
            } catch (IOException exception) {
                if (writeStarted) {
                    failed = true;
                }
                throw failure(Failure.IO_FAILURE,
                        "allocation append did not complete", exception);
            } catch (RuntimeException exception) {
                if (writeStarted) {
                    failed = true;
                }
                throw exception;
            }
        }
    }

    /** Deterministically replays the complete canonical chain or fails closed. */
    public synchronized ReadResult replay() {
        ensureReadable();
        synchronized (processMutex) {
            return replayLocked();
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    private ReadResult replayLocked() {
        if (!Files.exists(namespace, LinkOption.NOFOLLOW_LINKS)) {
            return ReadResult.empty();
        }
        validateControlledDirectory(namespace, trustedRoot, "allocation namespace");
        if (!Files.exists(storeDirectory, LinkOption.NOFOLLOW_LINKS)) {
            return ReadResult.empty();
        }
        validateControlledDirectory(storeDirectory, namespace, "allocation store directory");
        validateOnlyControlledFile();
        if (!Files.exists(storeFile, LinkOption.NOFOLLOW_LINKS)) {
            return ReadResult.empty();
        }
        validateStoreFile();
        byte[] bytes = readBoundedBytes();
        if (bytes.length == 0) {
            return new ReadResult(true, List.of(), 0L);
        }
        if (bytes[bytes.length - 1] != '\n') {
            throw failure(Failure.TRUNCATED_TAIL,
                    "allocation store has an incomplete final record and was preserved unchanged");
        }

        List<EnterpriseLabAllocationState> records = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < bytes.length; index++) {
            if (bytes[index] != '\n') {
                continue;
            }
            if (index == start) {
                throw failure(Failure.CORRUPT_RECORD,
                        "allocation store contains an empty complete record");
            }
            if (records.size() >= maxRecords) {
                throw failure(Failure.RECORD_LIMIT_EXCEEDED,
                        "allocation store exceeds its bounded record count");
            }
            byte[] encoded = Arrays.copyOfRange(bytes, start, index);
            EnterpriseLabAllocationState state;
            try {
                state = codec.decode(encoded);
            } catch (RuntimeException exception) {
                throw failure(Failure.CORRUPT_RECORD,
                        "allocation store contains an invalid complete record", exception);
            }
            if (!Arrays.equals(encoded, codec.encode(state))) {
                throw failure(Failure.NON_CANONICAL_RECORD,
                        "allocation store contains a non-canonical complete record");
            }
            validateNext(records, state);
            records.add(state);
            start = index + 1;
        }
        return new ReadResult(true, records, bytes.length);
    }

    private void validateNext(
            List<EnterpriseLabAllocationState> existing,
            EnterpriseLabAllocationState next) {
        if (existing.isEmpty()) {
            validateInitialBaseline(next);
            return;
        }
        EnterpriseLabAllocationState previous = existing.get(existing.size() - 1);
        if (!previous.currentRecordFingerprint().equals(next.predecessorRecordFingerprint())) {
            throw failure(Failure.PREDECESSOR_MISMATCH,
                    "allocation record predecessor does not match the durable chain head");
        }
        if (next.ownerGeneration() < previous.ownerGeneration()) {
            throw failure(Failure.OWNER_GENERATION_REGRESSION,
                    "allocation record owner generation regressed");
        }
        boolean sameTransaction = previous.allocationTransactionId()
                .equals(next.allocationTransactionId());
        if (sameTransaction) {
            if (previous.allocationGeneration() != next.allocationGeneration()) {
                throw failure(Failure.ALLOCATION_GENERATION_MISMATCH,
                        "one allocation transaction changed logical generation");
            }
            requireStableTransactionIntent(previous, next);
        } else {
            if (next.allocationGeneration() != previous.allocationGeneration() + 1L) {
                throw failure(Failure.ALLOCATION_GENERATION_MISMATCH,
                        "new allocation transaction generation is not contiguous");
            }
            for (EnterpriseLabAllocationState record : existing) {
                if (record.allocationTransactionId().equals(next.allocationTransactionId())) {
                    throw failure(Failure.TRANSACTION_REAPPEARED,
                            "allocation transaction identity reappeared after a later transaction");
                }
            }
        }
    }

    private static void requireStableTransactionIntent(
            EnterpriseLabAllocationState previous,
            EnterpriseLabAllocationState next) {
        if (!previous.experimentId().equals(next.experimentId())
                || !previous.scenarioId().equals(next.scenarioId())
                || previous.allocationPurpose() != next.allocationPurpose()
                || !previous.baselineAllocation().equals(next.baselineAllocation())
                || !previous.requestedAllocation().equals(next.requestedAllocation())
                || !previous.guardrailApprovedAllocation().equals(
                        next.guardrailApprovedAllocation())
                || !previous.previousCommittedAllocationFingerprint().equals(
                        next.previousCommittedAllocationFingerprint())) {
            throw failure(Failure.TRANSACTION_INTENT_CHANGED,
                    "allocation transaction intent changed within one logical generation");
        }
    }

    private static void validateInitialBaseline(EnterpriseLabAllocationState state) {
        String allocationFingerprint = state.normalizedAllocationFingerprint();
        if (state.allocationGeneration() != 1L
                || state.allocationPurpose() != AllocationPurpose.INITIAL_SAFE_BASELINE
                || state.transactionPhase() != TransactionPhase.COMMITTED
                || !EnterpriseLabAllocationState.GENESIS_FINGERPRINT.equals(
                        state.predecessorRecordFingerprint())
                || !EnterpriseLabAllocationState.NO_FINGERPRINT.equals(
                        state.previousCommittedAllocationFingerprint())
                || !state.baselineAllocation().equals(state.requestedAllocation())
                || !state.baselineAllocation().equals(state.guardrailApprovedAllocation())
                || !state.baselineAllocation().equals(state.installedAllocation())
                || !allocationFingerprint.equals(state.routerReadBackFingerprint())
                || state.verificationResult() != VerificationResult.MATCHED
                || state.lastVerifiedAt().isEmpty()) {
            throw failure(Failure.INVALID_INITIAL_BASELINE,
                    "first allocation record must be a verified committed safe baseline");
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

    private void prepareStoreFile() throws IOException {
        validateControlledDirectory(storeDirectory, namespace, "allocation store directory");
        if (Files.exists(storeFile, LinkOption.NOFOLLOW_LINKS)) {
            validateStoreFile();
            return;
        }
        try (FileChannel ignored = openFileForCreation(storeFile)) {
            // Creation only; append happens after the empty file is validated.
        } catch (FileAlreadyExistsException ignored) {
            // Another repository-controlled store instance won the process-local race.
        }
        restrictPermissions(storeFile, FILE_PERMISSIONS);
        validateStoreFile();
        forceDirectoryMetadataIfSupported(storeDirectory);
    }

    private void appendFrame(byte[] encoded) throws IOException {
        byte[] frame = Arrays.copyOf(encoded, encoded.length + 1);
        frame[frame.length - 1] = '\n';
        try (FileChannel channel = FileChannel.open(
                storeFile,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND,
                LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer buffer = ByteBuffer.wrap(frame);
            int writtenBytes = 0;
            int zeroWrites = 0;
            while (buffer.hasRemaining()) {
                int originalLimit = buffer.limit();
                buffer.limit(Math.min(originalLimit, buffer.position() + MAX_WRITE_CHUNK_BYTES));
                int written;
                try {
                    written = channel.write(buffer);
                } finally {
                    buffer.limit(originalLimit);
                }
                if (written == 0) {
                    zeroWrites++;
                    if (zeroWrites >= MAX_ZERO_PROGRESS) {
                        throw new IOException("bounded allocation store write made no progress");
                    }
                    continue;
                }
                zeroWrites = 0;
                writtenBytes += written;
                failureInjector.checkpoint(WriteCheckpoint.AFTER_WRITE_CHUNK, writtenBytes);
            }
        }
    }

    private void forceStoreFile() throws IOException {
        try (FileChannel channel = FileChannel.open(
                storeFile,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS)) {
            channel.force(true);
        }
    }

    private byte[] readBoundedBytes() {
        long declaredSize;
        try (FileChannel channel = FileChannel.open(
                storeFile,
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            declaredSize = channel.size();
            if (declaredSize > maxStoreBytes) {
                throw failure(Failure.STORE_SIZE_EXCEEDED,
                        "allocation store exceeds its bounded byte size");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream(
                    Math.toIntExact(declaredSize));
            ByteBuffer buffer = ByteBuffer.allocate(READ_BUFFER_BYTES);
            int zeroReads = 0;
            long observed = 0;
            while (true) {
                int read = channel.read(buffer);
                if (read < 0) {
                    break;
                }
                if (read == 0) {
                    zeroReads++;
                    if (zeroReads >= MAX_ZERO_PROGRESS) {
                        throw failure(Failure.IO_FAILURE,
                                "bounded allocation store read made no progress");
                    }
                    continue;
                }
                zeroReads = 0;
                observed += read;
                if (observed > maxStoreBytes) {
                    throw failure(Failure.STORE_SIZE_EXCEEDED,
                            "allocation store exceeds its bounded byte size");
                }
                buffer.flip();
                output.write(buffer.array(), 0, buffer.remaining());
                buffer.clear();
            }
            if (observed != declaredSize || channel.size() != declaredSize) {
                throw failure(Failure.CONCURRENT_CHANGE,
                        "allocation store changed during bounded replay");
            }
            return output.toByteArray();
        } catch (StoreException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE,
                    "allocation store could not be read", exception);
        }
    }

    private void validateOnlyControlledFile() {
        try (var entries = Files.newDirectoryStream(storeDirectory)) {
            for (Path entry : entries) {
                Path normalized = entry.toAbsolutePath().normalize();
                if (!normalized.equals(storeFile)) {
                    throw failure(Failure.UNEXPECTED_STORAGE_ENTRY,
                            "allocation store directory contains an unexpected entry");
                }
            }
        } catch (StoreException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE,
                    "allocation store directory could not be inspected", exception);
        }
    }

    private void validateStoreFile() {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    storeFile,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || !attributes.isRegularFile()) {
                throw failure(Failure.SYMLINK_OR_TYPE_ESCAPE,
                        "allocation store file must be a non-symbolic-link regular file");
            }
            Path realParent = storeDirectory.toRealPath();
            Path realFile = storeFile.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!realFile.getParent().equals(realParent)) {
                throw failure(Failure.SYMLINK_OR_TYPE_ESCAPE,
                        "allocation store file escaped its controlled directory");
            }
        } catch (StoreException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(Failure.PERMISSION_DENIED,
                    "allocation store file permission was denied", exception);
        } catch (IOException exception) {
            throw failure(Failure.STORAGE_UNAVAILABLE,
                    "allocation store file could not be inspected", exception);
        }
    }

    private void ensureWritable() {
        if (failed) {
            throw failure(Failure.WRITER_FAILED,
                    "allocation store writer is failed and must be reopened");
        }
        ensureReadable();
    }

    private void ensureReadable() {
        if (closed) {
            throw failure(Failure.CLOSED, "allocation store is closed");
        }
    }

    private static Path validateTrustedRoot(Path value) {
        if (value == null || !value.isAbsolute()) {
            throw failure(Failure.INVALID_TRUSTED_ROOT,
                    "allocation data root must be an explicit absolute path");
        }
        Path normalized = value.normalize();
        if (normalized.getParent() == null) {
            throw failure(Failure.INVALID_TRUSTED_ROOT,
                    "allocation data root must be a non-root local file path");
        }
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    normalized, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
                throw failure(Failure.INVALID_TRUSTED_ROOT,
                        "allocation data root must be an existing non-symbolic-link directory");
            }
            if (!normalized.toRealPath().equals(normalized)) {
                throw failure(Failure.SYMLINK_OR_TYPE_ESCAPE,
                        "allocation data root cannot traverse symbolic links");
            }
            return normalized;
        } catch (StoreException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(Failure.PERMISSION_DENIED,
                    "allocation data root is not accessible", exception);
        } catch (IOException exception) {
            throw failure(Failure.STORAGE_UNAVAILABLE,
                    "allocation data root is unavailable", exception);
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
                    // A repository-controlled concurrent initializer won the race.
                }
            }
            restrictPermissions(directory, DIRECTORY_PERMISSIONS);
            validateControlledDirectory(directory, parent, "allocation storage directory");
            return directory;
        } catch (StoreException exception) {
            throw exception;
        } catch (AccessDeniedException exception) {
            throw failure(Failure.PERMISSION_DENIED,
                    "allocation storage directory permission was denied", exception);
        } catch (IOException exception) {
            throw failure(Failure.STORAGE_UNAVAILABLE,
                    "allocation storage directory could not be prepared", exception);
        }
    }

    private static Path controlledPath(Path parent, String name) {
        Path candidate = parent.resolve(name).toAbsolutePath().normalize();
        if (!candidate.getParent().equals(parent)
                || !candidate.startsWith(parent)
                || !candidate.getFileName().toString().equals(name)) {
            throw failure(Failure.SYMLINK_OR_TYPE_ESCAPE,
                    "allocation storage path escaped its fixed parent");
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
                    "allocation store directory metadata could not be synchronized", exception);
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(Failure.STORAGE_UNAVAILABLE,
                    "allocation store directory metadata synchronization is unavailable", exception);
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

    public enum SyncPolicy {
        FORCE_DATA_AND_METADATA
    }

    public enum Failure {
        CLOSED,
        WRITER_FAILED,
        INVALID_TRUSTED_ROOT,
        PERMISSION_DENIED,
        STORAGE_UNAVAILABLE,
        SYMLINK_OR_TYPE_ESCAPE,
        UNEXPECTED_STORAGE_ENTRY,
        IO_FAILURE,
        CORRUPT_RECORD,
        NON_CANONICAL_RECORD,
        TRUNCATED_TAIL,
        PREDECESSOR_MISMATCH,
        OWNER_GENERATION_MISMATCH,
        OWNER_GENERATION_REGRESSION,
        ALLOCATION_GENERATION_MISMATCH,
        TRANSACTION_REAPPEARED,
        TRANSACTION_INTENT_CHANGED,
        INVALID_INITIAL_BASELINE,
        RECORD_LIMIT_EXCEEDED,
        STORE_SIZE_EXCEEDED,
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
            int recordCount,
            String recordFingerprint,
            long totalBytes,
            SyncPolicy syncPolicy,
            boolean exactReadBackVerified) {
        public AppendReceipt {
            if (recordCount < 1 || recordCount > HARD_MAX_RECORDS) {
                throw new IllegalArgumentException("recordCount is outside hard bounds");
            }
            recordFingerprint = requireFingerprint(recordFingerprint);
            if (totalBytes < 1 || totalBytes > HARD_MAX_STORE_BYTES) {
                throw new IllegalArgumentException("totalBytes is outside hard bounds");
            }
            syncPolicy = Objects.requireNonNull(syncPolicy, "syncPolicy cannot be null");
            if (!exactReadBackVerified) {
                throw new IllegalArgumentException("append receipt requires exact durable read-back");
            }
        }
    }

    public record ReadResult(
            boolean storePresent,
            List<EnterpriseLabAllocationState> records,
            long totalBytes) {
        public ReadResult {
            records = List.copyOf(Objects.requireNonNull(records, "records cannot be null"));
            if (records.size() > HARD_MAX_RECORDS
                    || totalBytes < 0
                    || totalBytes > HARD_MAX_STORE_BYTES
                    || (!storePresent && (!records.isEmpty() || totalBytes != 0L))) {
                throw new IllegalArgumentException("allocation read result is inconsistent");
            }
        }

        static ReadResult empty() {
            return new ReadResult(false, List.of(), 0L);
        }

        public Optional<EnterpriseLabAllocationState> baseline() {
            return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
        }

        public Optional<EnterpriseLabAllocationState> chainHead() {
            return records.isEmpty()
                    ? Optional.empty()
                    : Optional.of(records.get(records.size() - 1));
        }

        public Optional<EnterpriseLabAllocationState> lastCommitted() {
            for (int index = records.size() - 1; index >= 0; index--) {
                TransactionPhase phase = records.get(index).transactionPhase();
                if (phase == TransactionPhase.COMMITTED || phase == TransactionPhase.RESTORED) {
                    return Optional.of(records.get(index));
                }
            }
            return Optional.empty();
        }
    }

    enum WriteCheckpoint {
        BEFORE_APPEND,
        AFTER_WRITE_CHUNK,
        AFTER_APPEND_BEFORE_SYNC,
        AFTER_SYNC
    }

    @FunctionalInterface
    interface FailureInjector {
        void checkpoint(WriteCheckpoint checkpoint, int bytesWritten) throws IOException;
    }

    private static String requireFingerprint(String value) {
        String safe = Objects.requireNonNull(value, "recordFingerprint cannot be null");
        if (!safe.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("recordFingerprint must be canonical SHA-256");
        }
        return safe;
    }
}
