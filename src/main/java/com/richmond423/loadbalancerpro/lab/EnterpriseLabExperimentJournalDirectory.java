package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournal.ReadResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournal.SyncPolicy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournal.TailStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalStorageException.Failure;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalVerifier.Outcome;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalVerifier.VerificationResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Creates journals only beneath a pre-existing, explicit, trusted local data root.
 * Experiment identifiers are hashed and never used as path components.
 */
public final class EnterpriseLabExperimentJournalDirectory {
    public static final long HARD_MAX_JOURNAL_BYTES = 16L * 1024L * 1024L;
    public static final int HARD_MAX_JOURNAL_ENTRIES = 4_096;

    private static final String NAMESPACE = "enterprise-lab-experiment-journals-v1";
    private static final String JOURNALS = "journals";
    private static final Pattern CANONICAL_EXPERIMENT_ID = Pattern.compile("[A-Za-z0-9._:-]+");
    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_PERMISSIONS =
            PosixFilePermissions.fromString("rw-------");
    private static final Map<Path, Object> ACTIVE_WRITERS = new ConcurrentHashMap<>();
    private static final FailureInjector NO_FAILURE = (checkpoint, bytesWritten) -> { };

    private final Path journalsDirectory;
    private final EnterpriseLabExperimentJournalCodec codec;
    private final long maxJournalBytes;
    private final int maxJournalEntries;

    private EnterpriseLabExperimentJournalDirectory(Path trustedRoot) {
        this(trustedRoot, HARD_MAX_JOURNAL_BYTES, HARD_MAX_JOURNAL_ENTRIES);
    }

    private EnterpriseLabExperimentJournalDirectory(
            Path trustedRoot,
            long maxJournalBytes,
            int maxJournalEntries) {
        if (maxJournalBytes < 1 || maxJournalBytes > HARD_MAX_JOURNAL_BYTES
                || maxJournalEntries < 1 || maxJournalEntries > HARD_MAX_JOURNAL_ENTRIES) {
            throw new IllegalArgumentException("journal test limits must remain within production hard limits");
        }
        this.codec = new EnterpriseLabExperimentJournalCodec();
        this.maxJournalBytes = maxJournalBytes;
        this.maxJournalEntries = maxJournalEntries;
        Path root = validateTrustedRoot(trustedRoot);
        Path namespace = controlledDirectory(root, NAMESPACE);
        this.journalsDirectory = controlledDirectory(namespace, JOURNALS);
    }

    public static EnterpriseLabExperimentJournalDirectory create(Path trustedRoot) {
        return new EnterpriseLabExperimentJournalDirectory(trustedRoot);
    }

    static EnterpriseLabExperimentJournalDirectory createForTesting(
            Path trustedRoot,
            long maxJournalBytes,
            int maxJournalEntries) {
        return new EnterpriseLabExperimentJournalDirectory(trustedRoot, maxJournalBytes, maxJournalEntries);
    }

    /** Opens a writer using the safety-first data-and-metadata synchronization policy. */
    public EnterpriseLabExperimentJournal openJournal(String experimentId) {
        return openJournal(experimentId, SyncPolicy.FORCE_DATA_AND_METADATA);
    }

    public EnterpriseLabExperimentJournal openJournal(String experimentId, SyncPolicy syncPolicy) {
        return openJournal(experimentId, syncPolicy, NO_FAILURE);
    }

    EnterpriseLabExperimentJournal openJournal(
            String experimentId,
            SyncPolicy syncPolicy,
            FailureInjector failureInjector) {
        String safeExperimentId = requireExperimentId(experimentId);
        SyncPolicy safeSyncPolicy = Objects.requireNonNull(syncPolicy, "syncPolicy cannot be null");
        FailureInjector safeInjector = Objects.requireNonNull(failureInjector, "failureInjector cannot be null");
        String journalId = journalId(safeExperimentId);
        Path journalPath = journalPath(journalId);
        Object owner = claim(journalPath);
        FileChannel channel = null;
        boolean handedOff = false;
        try {
            createJournalFileIfMissing(journalPath);
            VerificationResult verification = verifyOwned(journalPath, journalId, safeExperimentId);
            if (verification.outcome() == Outcome.VALID_WITH_RECOVERABLE_TRUNCATED_TAIL) {
                throw failure(Failure.PARTIAL_TAIL, "journal has a truncated tail and was preserved unchanged");
            }
            if (verification.outcome() != Outcome.VALID) {
                throw failure(Failure.VERIFICATION_FAILED,
                        "journal failed read-only chain verification: " + verification.classification().name());
            }
            ReadResult existing = new ReadResult(
                    journalId,
                    true,
                    verification.verifiedEvents(),
                    TailStatus.COMPLETE,
                    verification.completeBytes(),
                    0,
                    verification.totalBytes());
            channel = FileChannel.open(
                    journalPath,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND,
                    LinkOption.NOFOLLOW_LINKS);
            if (channel.size() != existing.totalBytes()) {
                throw failure(Failure.IO_FAILURE, "journal changed while its writer was opening");
            }
            EnterpriseLabExperimentLocalJournal journal = new EnterpriseLabExperimentLocalJournal(
                    this,
                    journalId,
                    safeExperimentId,
                    journalPath,
                    channel,
                    safeSyncPolicy,
                    codec,
                    existing,
                    safeInjector,
                    () -> ACTIVE_WRITERS.remove(journalPath, owner));
            handedOff = true;
            return journal;
        } catch (EnterpriseLabExperimentJournalStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE, "journal writer could not be opened", exception);
        } finally {
            if (!handedOff) {
                closeQuietly(channel);
                ACTIVE_WRITERS.remove(journalPath, owner);
            }
        }
    }

    public ReadResult read(String experimentId) {
        String safeExperimentId = requireExperimentId(experimentId);
        String journalId = journalId(safeExperimentId);
        Path journalPath = journalPath(journalId);
        Object owner = claim(journalPath);
        try {
            return scan(journalPath, journalId, safeExperimentId);
        } finally {
            ACTIVE_WRITERS.remove(journalPath, owner);
        }
    }

    /** Verifies a closed journal without exposing or mutating its controlled backing path. */
    public VerificationResult verify(String experimentId) {
        String safeExperimentId = requireExperimentId(experimentId);
        String journalId = journalId(safeExperimentId);
        Path journalPath = journalPath(journalId);
        Object owner;
        try {
            owner = claim(journalPath);
        } catch (EnterpriseLabExperimentJournalStorageException exception) {
            if (exception.failure() == Failure.WRITER_ALREADY_ACTIVE) {
                return VerificationResult.unavailable(journalId);
            }
            throw exception;
        }
        try {
            return verifyOwned(journalPath, journalId, safeExperimentId);
        } finally {
            ACTIVE_WRITERS.remove(journalPath, owner);
        }
    }

    VerificationResult verifyOwned(Path journalPath, String journalId, String experimentId) {
        return new EnterpriseLabExperimentJournalVerifier(
                codec, maxJournalBytes, maxJournalEntries)
                .verify(journalPath, journalId, experimentId);
    }

    ReadResult scanOwned(Path journalPath, String journalId, String experimentId) {
        return scan(journalPath, journalId, experimentId);
    }

    long maxJournalBytes() {
        return maxJournalBytes;
    }

    int maxJournalEntries() {
        return maxJournalEntries;
    }

    private ReadResult scan(Path journalPath, String journalId, String experimentId) {
        if (!Files.exists(journalPath, LinkOption.NOFOLLOW_LINKS)) {
            return new ReadResult(journalId, false, List.of(), TailStatus.COMPLETE, 0, 0, 0);
        }
        validateJournalFile(journalPath);
        List<EnterpriseLabExperimentJournalEvent> events = new ArrayList<>();
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        long observedBytes = 0;
        long completeBytes = 0;
        long declaredSize;
        try (FileChannel channel = FileChannel.open(
                journalPath, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            declaredSize = channel.size();
            if (declaredSize > maxJournalBytes) {
                throw failure(Failure.JOURNAL_SIZE_EXCEEDED,
                        "journal exceeds the bounded local size limit");
            }
            ByteBuffer buffer = ByteBuffer.allocate(8_192);
            int zeroReads = 0;
            while (true) {
                int read = channel.read(buffer);
                if (read < 0) {
                    break;
                }
                if (read == 0) {
                    zeroReads++;
                    if (zeroReads >= 3) {
                        throw failure(Failure.IO_FAILURE, "bounded journal read made no progress");
                    }
                    continue;
                }
                zeroReads = 0;
                buffer.flip();
                while (buffer.hasRemaining()) {
                    byte value = buffer.get();
                    observedBytes++;
                    if (observedBytes > maxJournalBytes) {
                        throw failure(Failure.JOURNAL_SIZE_EXCEEDED,
                                "journal exceeds the bounded local size limit");
                    }
                    if (value == '\n') {
                        decodeCompleteLine(line.toByteArray(), events, experimentId);
                        if (events.size() > maxJournalEntries) {
                            throw failure(Failure.ENTRY_LIMIT_EXCEEDED,
                                    "journal exceeds the bounded entry count");
                        }
                        line.reset();
                        completeBytes = observedBytes;
                    } else {
                        if (line.size() >= EnterpriseLabExperimentJournalCodec.HARD_MAX_ENTRY_BYTES) {
                            throw failure(Failure.INVALID_COMPLETE_ENTRY,
                                    "journal entry exceeds the bounded frame size");
                        }
                        line.write(value);
                    }
                }
                buffer.clear();
            }
        } catch (EnterpriseLabExperimentJournalStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE, "journal could not be read", exception);
        }
        if (observedBytes != declaredSize) {
            throw failure(Failure.IO_FAILURE, "journal changed during its bounded read");
        }
        long tailBytes = line.size();
        TailStatus tailStatus = tailBytes == 0 ? TailStatus.COMPLETE : TailStatus.TRUNCATED_TAIL;
        return new ReadResult(
                journalId,
                true,
                events,
                tailStatus,
                completeBytes,
                tailBytes,
                observedBytes);
    }

    private void decodeCompleteLine(
            byte[] encoded,
            List<EnterpriseLabExperimentJournalEvent> events,
            String experimentId) {
        if (encoded.length == 0) {
            throw failure(Failure.INVALID_COMPLETE_ENTRY, "journal contains an empty complete frame");
        }
        EnterpriseLabExperimentJournalEvent event;
        try {
            event = codec.decode(encoded);
        } catch (RuntimeException exception) {
            throw failure(Failure.INVALID_COMPLETE_ENTRY, "journal contains an invalid complete frame", exception);
        }
        if (!Arrays.equals(encoded, codec.encode(event))) {
            throw failure(Failure.NON_CANONICAL_ENTRY, "journal contains a non-canonical complete frame");
        }
        if (!experimentId.equals(event.experimentId())) {
            throw failure(Failure.IDENTITY_MISMATCH, "journal entry experiment identity does not match");
        }
        long expectedSequence = events.size() + 1L;
        if (event.sequence() != expectedSequence) {
            throw failure(Failure.SEQUENCE_MISMATCH, "journal entry sequence is not contiguous");
        }
        String expectedPrevious = events.isEmpty()
                ? EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT
                : events.get(events.size() - 1).currentEntryFingerprint();
        if (!expectedPrevious.equals(event.previousEntryFingerprint())) {
            throw failure(Failure.PREDECESSOR_MISMATCH, "journal entry predecessor does not match");
        }
        events.add(event);
    }

    private static Path validateTrustedRoot(Path trustedRoot) {
        if (trustedRoot == null || !trustedRoot.isAbsolute()) {
            throw failure(Failure.UNSAFE_DIRECTORY,
                    "journal data root must be an explicit absolute path");
        }
        Path normalized = trustedRoot.normalize();
        String scheme = normalized.toUri().getScheme();
        if (scheme == null || !"file".equalsIgnoreCase(scheme)
                || normalized.toString().startsWith("\\\\")) {
            throw failure(Failure.UNSAFE_DIRECTORY,
                    "journal data root must use a local file path");
        }
        if (normalized.getParent() == null) {
            throw failure(Failure.UNSAFE_DIRECTORY, "filesystem root cannot be used as journal data root");
        }
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    normalized, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isDirectory() || attributes.isSymbolicLink()) {
                throw failure(Failure.UNSAFE_DIRECTORY,
                        "journal data root must be an existing non-symbolic-link directory");
            }
            Path resolved = normalized.toRealPath();
            if (!resolved.equals(normalized)) {
                throw failure(Failure.UNSAFE_DIRECTORY,
                        "journal data root cannot traverse symbolic links");
            }
            return normalized;
        } catch (EnterpriseLabExperimentJournalStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.UNSAFE_DIRECTORY,
                    "journal data root must be an accessible existing directory", exception);
        }
    }

    private static Path controlledDirectory(Path parent, String name) {
        Path directory = parent.resolve(name).normalize();
        if (!directory.startsWith(parent)) {
            throw failure(Failure.UNSAFE_PATH, "journal namespace escaped its trusted root");
        }
        try {
            if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    createDirectory(directory);
                } catch (FileAlreadyExistsException ignored) {
                    // A concurrent creator is safe only if the validation below succeeds.
                }
            }
            BasicFileAttributes attributes = Files.readAttributes(
                    directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isDirectory() || attributes.isSymbolicLink()) {
                throw failure(Failure.UNSAFE_PATH,
                        "journal namespace must be a non-symbolic-link directory");
            }
            restrictPermissions(directory, DIRECTORY_PERMISSIONS);
            return directory;
        } catch (EnterpriseLabExperimentJournalStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE, "journal namespace could not be prepared", exception);
        }
    }

    private void createJournalFileIfMissing(Path journalPath) {
        try {
            if (!Files.exists(journalPath, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    createFile(journalPath);
                } catch (FileAlreadyExistsException ignored) {
                    // A concurrent external creator is accepted only after strict validation.
                }
            }
            validateJournalFile(journalPath);
            restrictPermissions(journalPath, FILE_PERMISSIONS);
        } catch (EnterpriseLabExperimentJournalStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE, "journal file could not be prepared", exception);
        }
    }

    private static void validateJournalFile(Path journalPath) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    journalPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile() || attributes.isSymbolicLink()) {
                throw failure(Failure.UNSAFE_PATH,
                        "journal storage target must be a non-symbolic-link regular file");
            }
        } catch (EnterpriseLabExperimentJournalStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE, "journal storage target could not be inspected", exception);
        }
    }

    private Path journalPath(String journalId) {
        Path candidate = journalsDirectory.resolve(journalId + ".jsonl").normalize();
        if (!candidate.startsWith(journalsDirectory) || !candidate.getParent().equals(journalsDirectory)) {
            throw failure(Failure.UNSAFE_PATH, "journal path escaped its controlled namespace");
        }
        return candidate;
    }

    private static Object claim(Path path) {
        Object owner = new Object();
        if (ACTIVE_WRITERS.putIfAbsent(path, owner) != null) {
            throw failure(Failure.WRITER_ALREADY_ACTIVE,
                    "a process-local journal writer or reader already owns this experiment");
        }
        return owner;
    }

    private static String requireExperimentId(String experimentId) {
        if (experimentId == null
                || experimentId.isBlank()
                || !experimentId.equals(experimentId.trim())
                || experimentId.length() > 128
                || !CANONICAL_EXPERIMENT_ID.matcher(experimentId).matches()) {
            throw new IllegalArgumentException("experimentId must be a bounded canonical identifier");
        }
        return experimentId;
    }

    private static String journalId(String experimentId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(experimentId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "journal-v1-" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void createDirectory(Path directory) throws IOException {
        try {
            Files.createDirectory(directory, directoryAttribute());
        } catch (UnsupportedOperationException exception) {
            Files.createDirectory(directory);
        }
    }

    private static void createFile(Path file) throws IOException {
        try {
            Files.createFile(file, fileAttribute());
        } catch (UnsupportedOperationException exception) {
            Files.createFile(file);
        }
    }

    private static FileAttribute<Set<PosixFilePermission>> directoryAttribute() {
        return PosixFilePermissions.asFileAttribute(DIRECTORY_PERMISSIONS);
    }

    private static FileAttribute<Set<PosixFilePermission>> fileAttribute() {
        return PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS);
    }

    private static void restrictPermissions(Path path, Set<PosixFilePermission> permissions) throws IOException {
        if (Files.getFileAttributeView(path, java.nio.file.attribute.PosixFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS) != null) {
            Files.setPosixFilePermissions(path, permissions);
        }
    }

    private static void closeQuietly(FileChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
                // The primary open failure remains authoritative.
            }
        }
    }

    static EnterpriseLabExperimentJournalStorageException failure(Failure failure, String message) {
        return new EnterpriseLabExperimentJournalStorageException(failure, message);
    }

    static EnterpriseLabExperimentJournalStorageException failure(
            Failure failure,
            String message,
            Throwable cause) {
        return new EnterpriseLabExperimentJournalStorageException(failure, message, cause);
    }

    @FunctionalInterface
    interface FailureInjector {
        void checkpoint(WriteCheckpoint checkpoint, int bytesWritten) throws IOException;
    }

    enum WriteCheckpoint {
        BEFORE_APPEND,
        AFTER_WRITE_CHUNK,
        AFTER_APPEND_BEFORE_SYNC,
        AFTER_SYNC
    }
}
