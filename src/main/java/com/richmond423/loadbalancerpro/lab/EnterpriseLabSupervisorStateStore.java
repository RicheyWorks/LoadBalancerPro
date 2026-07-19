package com.richmond423.loadbalancerpro.lab;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Fixed-path atomic supervisor state publication. A completed current file is
 * authoritative; an interrupted temporary write is preserved once for bounded
 * recovery evidence and is never interpreted as a committed allocation.
 */
public final class EnterpriseLabSupervisorStateStore {
    public static final long HARD_MAX_TOTAL_STORAGE_BYTES = 768L * 1024L;

    static final String STATE_FILE_NAME = "supervisor-state-v1.json";
    static final String TEMPORARY_FILE_NAME = "supervisor-state-v1.tmp";
    static final String INTERRUPTED_FILE_NAME = "supervisor-state-v1.interrupted";

    private final EnterpriseLabSupervisorOwnership ownership;
    private final EnterpriseLabSupervisorStateCodec codec;
    private final Path directory;
    private final Path stateFile;
    private final Path temporaryFile;
    private final Path interruptedFile;

    public EnterpriseLabSupervisorStateStore(
            EnterpriseLabSupervisorOwnership ownership,
            EnterpriseLabExperimentTargetCatalog targetCatalog) {
        this.ownership = Objects.requireNonNull(ownership, "ownership cannot be null");
        this.codec = new EnterpriseLabSupervisorStateCodec(
                Objects.requireNonNull(targetCatalog, "targetCatalog cannot be null"));
        this.directory = ownership.supervisorDirectory();
        this.stateFile = EnterpriseLabSupervisorOwnership.controlledPath(
                directory, STATE_FILE_NAME);
        this.temporaryFile = EnterpriseLabSupervisorOwnership.controlledPath(
                directory, TEMPORARY_FILE_NAME);
        this.interruptedFile = EnterpriseLabSupervisorOwnership.controlledPath(
                directory, INTERRUPTED_FILE_NAME);
        preserveInterruptedTemporary();
        enforceTotalStorageBound();
    }

    public synchronized Optional<EnterpriseLabSupervisorState> readIfPresent() {
        ownership.requireHeld();
        if (!Files.exists(stateFile, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        return Optional.of(readRequired(stateFile, "current supervisor state"));
    }

    /**
     * Atomically installs and verifies one exact successor. The expected value
     * prevents a process-local stale writer even while the OS lock remains held.
     */
    public synchronized EnterpriseLabSupervisorState install(
            Optional<EnterpriseLabSupervisorState> expected,
            EnterpriseLabSupervisorState replacement) {
        ownership.requireHeld();
        Optional<EnterpriseLabSupervisorState> safeExpected = Objects.requireNonNull(
                expected, "expected cannot be null");
        EnterpriseLabSupervisorState safeReplacement = Objects.requireNonNull(
                replacement, "replacement cannot be null");
        Optional<EnterpriseLabSupervisorState> current = readIfPresent();
        if (!sameRecord(current, safeExpected)) {
            throw failure(Failure.CONCURRENT_CHANGE,
                    "supervisor state changed before atomic installation");
        }
        if (current.isEmpty()
                && !EnterpriseLabSupervisorState.GENESIS_FINGERPRINT.equals(
                safeReplacement.predecessorRecordFingerprint())) {
            throw failure(Failure.INVALID_SUCCESSOR,
                    "initial supervisor state must follow genesis");
        }
        if (current.isPresent()) {
            EnterpriseLabSupervisorState prior = current.orElseThrow();
            if (!prior.currentRecordFingerprint().equals(
                    safeReplacement.predecessorRecordFingerprint())
                    || safeReplacement.durableStateGeneration()
                    != prior.durableStateGeneration() + 1L
                    || safeReplacement.supervisorGeneration() < prior.supervisorGeneration()
                    || safeReplacement.installedAllocation().routerGeneration()
                    < prior.installedAllocation().routerGeneration()) {
                throw failure(Failure.INVALID_SUCCESSOR,
                        "supervisor state successor regressed or broke its fingerprint chain");
            }
        }

        byte[] encoded = codec.encode(safeReplacement);
        if (encoded.length > EnterpriseLabSupervisorStateCodec.HARD_MAX_RECORD_BYTES) {
            throw failure(Failure.STORAGE_LIMIT_EXCEEDED,
                    "supervisor state exceeds its bounded record size");
        }
        try {
            ownership.requireHeld();
            writeTemporary(encoded);
            ownership.requireHeld();
            try {
                Files.move(
                        temporaryFile,
                        stateFile,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw failure(Failure.ATOMIC_MOVE_UNSUPPORTED,
                        "supervisor state requires same-directory atomic replacement", exception);
            }
            forceDirectoryMetadata();
            EnterpriseLabSupervisorState installed = readRequired(
                    stateFile, "installed supervisor state");
            if (!installed.equals(safeReplacement)) {
                throw failure(Failure.READ_BACK_MISMATCH,
                        "atomically installed supervisor state did not verify exactly");
            }
            enforceTotalStorageBound();
            return installed;
        } catch (StoreException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE,
                    "supervisor state installation failed safely", exception);
        }
    }

    public synchronized Optional<InterruptedEvidence> interruptedEvidence() {
        ownership.requireHeld();
        if (!Files.exists(interruptedFile, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        byte[] bytes = readBounded(interruptedFile, "interrupted supervisor state");
        try {
            EnterpriseLabSupervisorState state = codec.decode(bytes);
            return Optional.of(new InterruptedEvidence(
                    bytes.length,
                    Optional.of(state.currentRecordFingerprint()),
                    true));
        } catch (RuntimeException exception) {
            return Optional.of(new InterruptedEvidence(bytes.length, Optional.empty(), false));
        }
    }

    Path stateFile() {
        return stateFile;
    }

    private void preserveInterruptedTemporary() {
        ownership.requireHeld();
        if (!Files.exists(temporaryFile, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        readBounded(temporaryFile, "interrupted supervisor temporary state");
        if (Files.exists(interruptedFile, LinkOption.NOFOLLOW_LINKS)) {
            throw failure(Failure.INTERRUPTED_EVIDENCE_LIMIT_EXCEEDED,
                    "bounded interrupted supervisor evidence is already occupied");
        }
        try {
            Files.move(temporaryFile, interruptedFile, StandardCopyOption.ATOMIC_MOVE);
            forceDirectoryMetadata();
        } catch (AtomicMoveNotSupportedException exception) {
            throw failure(Failure.ATOMIC_MOVE_UNSUPPORTED,
                    "interrupted supervisor evidence requires atomic preservation", exception);
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE,
                    "interrupted supervisor evidence could not be preserved", exception);
        }
    }

    private void writeTemporary(byte[] encoded) throws IOException {
        if (Files.exists(temporaryFile, LinkOption.NOFOLLOW_LINKS)) {
            throw failure(Failure.INTERRUPTED_EVIDENCE_LIMIT_EXCEEDED,
                    "unexpected supervisor temporary state already exists");
        }
        try (FileChannel channel = FileChannel.open(
                temporaryFile,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            EnterpriseLabSupervisorOwnership.restrictFilePermissions(temporaryFile);
            ByteBuffer buffer = ByteBuffer.wrap(encoded);
            int zeroProgress = 0;
            while (buffer.hasRemaining()) {
                int written = channel.write(buffer);
                if (written == 0 && ++zeroProgress >= 3) {
                    throw new IOException("bounded supervisor state write made no progress");
                }
                if (written > 0) {
                    zeroProgress = 0;
                }
            }
            channel.force(true);
        }
    }

    private EnterpriseLabSupervisorState readRequired(Path path, String subject) {
        byte[] bytes = readBounded(path, subject);
        try {
            return codec.decode(bytes);
        } catch (RuntimeException exception) {
            throw failure(Failure.INVALID_STATE,
                    subject + " is malformed, unsupported, non-canonical, or corrupt", exception);
        }
    }

    private byte[] readBounded(Path path, String subject) {
        try {
            EnterpriseLabSupervisorOwnership.validateControlledFile(path, directory);
            BasicFileAttributes before = Files.readAttributes(
                    path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            long size = before.size();
            if (size < 1 || size > EnterpriseLabSupervisorStateCodec.HARD_MAX_RECORD_BYTES) {
                throw failure(Failure.STORAGE_LIMIT_EXCEEDED,
                        subject + " byte size is outside hard bounds");
            }
            byte[] bytes = Files.readAllBytes(path);
            BasicFileAttributes after = Files.readAttributes(
                    path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (bytes.length != size || after.size() != size
                    || !Objects.equals(before.fileKey(), after.fileKey())) {
                throw failure(Failure.CONCURRENT_CHANGE,
                        subject + " changed during bounded read");
            }
            return Arrays.copyOf(bytes, bytes.length);
        } catch (StoreException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE, subject + " could not be read", exception);
        }
    }

    private void enforceTotalStorageBound() {
        long total = 0L;
        for (Path path : java.util.List.of(stateFile, temporaryFile, interruptedFile)) {
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            try {
                EnterpriseLabSupervisorOwnership.validateControlledFile(path, directory);
                total = Math.addExact(total, Files.size(path));
            } catch (ArithmeticException exception) {
                throw failure(Failure.STORAGE_LIMIT_EXCEEDED,
                        "supervisor storage byte count overflowed", exception);
            } catch (IOException exception) {
                throw failure(Failure.IO_FAILURE,
                        "supervisor storage could not be measured", exception);
            }
        }
        if (total > HARD_MAX_TOTAL_STORAGE_BYTES) {
            throw failure(Failure.STORAGE_LIMIT_EXCEEDED,
                    "supervisor storage exceeds its hard total byte bound");
        }
    }

    private void forceDirectoryMetadata() {
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (UnsupportedOperationException ignored) {
            // The state file itself is forced; some Windows providers cannot open directories.
        } catch (IOException ignored) {
            // Same portability boundary as the existing controlled stores.
        }
    }

    private static boolean sameRecord(
            Optional<EnterpriseLabSupervisorState> first,
            Optional<EnterpriseLabSupervisorState> second) {
        if (first.isEmpty() || second.isEmpty()) {
            return first.isEmpty() && second.isEmpty();
        }
        return MessageDigestSupport.equal(
                first.orElseThrow().currentRecordFingerprint(),
                second.orElseThrow().currentRecordFingerprint());
    }

    private static StoreException failure(Failure failure, String message) {
        return new StoreException(failure, message);
    }

    private static StoreException failure(
            Failure failure, String message, Throwable cause) {
        return new StoreException(failure, message, cause);
    }

    public enum Failure {
        INVALID_STATE,
        INVALID_SUCCESSOR,
        CONCURRENT_CHANGE,
        STORAGE_LIMIT_EXCEEDED,
        INTERRUPTED_EVIDENCE_LIMIT_EXCEEDED,
        ATOMIC_MOVE_UNSUPPORTED,
        READ_BACK_MISMATCH,
        IO_FAILURE
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

    public record InterruptedEvidence(
            long bytes,
            Optional<String> recordFingerprint,
            boolean canonical) {
        public InterruptedEvidence {
            if (bytes < 1 || bytes > EnterpriseLabSupervisorStateCodec.HARD_MAX_RECORD_BYTES) {
                throw new IllegalArgumentException(
                        "interrupted evidence bytes are outside hard bounds");
            }
            recordFingerprint = Objects.requireNonNull(
                    recordFingerprint, "recordFingerprint cannot be null");
            if (canonical != recordFingerprint.isPresent()) {
                throw new IllegalArgumentException(
                        "canonical interrupted evidence must expose its fingerprint");
            }
        }
    }

    private static final class MessageDigestSupport {
        private MessageDigestSupport() {
        }

        private static boolean equal(String first, String second) {
            return java.security.MessageDigest.isEqual(
                    first.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    second.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        }
    }
}
