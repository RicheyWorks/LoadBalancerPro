package com.richmond423.loadbalancerpro.lab;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared bounded-I/O engine for the local append-only JSONL chains.
 *
 * <p>Store-specific codecs and chain-transition rules remain with their
 * owning domain types. This engine owns the mechanics which must not diverge:
 * one process mutex per fixed path, cooperating OS file locks, one complete
 * frame buffer per append, bounded reads, synchronization while the exclusive
 * lock is still held, and pinned file-key plus creation-time identity.</p>
 */
final class ChainedJsonlStore {
    private static final int READ_BUFFER_BYTES = 8_192;
    private static final int MAX_ZERO_PROGRESS = 3;
    private static final Map<Path, Object> PROCESS_MUTEXES = new ConcurrentHashMap<>();

    private final Path file;
    private final long maxBytes;
    private final Object processMutex;
    private FileIdentity pinnedIdentity;

    ChainedJsonlStore(Path file, long maxBytes) {
        this.file = Objects.requireNonNull(file, "file cannot be null")
                .toAbsolutePath().normalize();
        if (this.file.getParent() == null) {
            throw new IllegalArgumentException("file must have a controlled parent");
        }
        if (maxBytes < 1L || maxBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "maxBytes must fit the bounded in-memory replay contract");
        }
        this.maxBytes = maxBytes;
        this.processMutex = PROCESS_MUTEXES.computeIfAbsent(this.file, ignored -> new Object());
    }

    Object processMutex() {
        return processMutex;
    }

    Path file() {
        return file;
    }

    /**
     * Fails when a file previously observed by this engine has disappeared.
     * A never-created read-only store remains a valid empty store.
     */
    void requireMissingFileWasNeverObserved() throws StoreIOException {
        synchronized (this) {
            if (pinnedIdentity != null) {
                throw failure(
                        Failure.FILE_IDENTITY_CHANGED,
                        "the pinned chained JSONL file is no longer present");
            }
        }
    }

    byte[] readBoundedBytes() throws StoreIOException {
        synchronized (processMutex) {
            return readBoundedBytesLocked();
        }
    }

    private byte[] readBoundedBytesLocked() throws StoreIOException {
        FileIdentity before = pinCurrentIdentity();
        try (FileChannel channel = FileChannel.open(
                file,
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS);
             FileLock ignored = channel.lock(0L, Long.MAX_VALUE, true)) {
            requireCurrentIdentity(before);
            long declaredSize = channel.size();
            requireBoundedSize(declaredSize);

            ByteArrayOutputStream output = new ByteArrayOutputStream(
                    Math.toIntExact(declaredSize));
            ByteBuffer buffer = ByteBuffer.allocate(READ_BUFFER_BYTES);
            long observed = 0L;
            int zeroReads = 0;
            while (true) {
                int read = channel.read(buffer);
                if (read < 0) {
                    break;
                }
                if (read == 0) {
                    if (++zeroReads >= MAX_ZERO_PROGRESS) {
                        throw failure(
                                Failure.IO_FAILURE,
                                "bounded chained JSONL read made no progress");
                    }
                    continue;
                }
                zeroReads = 0;
                observed += read;
                requireBoundedSize(observed);
                buffer.flip();
                output.write(buffer.array(), 0, buffer.remaining());
                buffer.clear();
            }
            if (observed != declaredSize || channel.size() != declaredSize) {
                throw failure(
                        Failure.CONCURRENT_CHANGE,
                        "chained JSONL file changed during bounded replay");
            }
            requireCurrentIdentity(before);
            return output.toByteArray();
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "chained JSONL file could not be read under its shared lock",
                    exception);
        }
    }

    long currentSize() throws StoreIOException {
        return readBoundedBytes().length;
    }

    <T> ChainReplay<T> replayChain(
            FrameCodec<T> codec,
            ChainValidator<T> chainValidator,
            int maxEntries,
            int maxFrameBytes,
            TailPolicy tailPolicy) throws StoreIOException {
        synchronized (processMutex) {
            return decodeChain(
                    readBoundedBytesLocked(),
                    codec,
                    chainValidator,
                    maxEntries,
                    maxFrameBytes,
                    tailPolicy);
        }
    }

    <T> ChainReplay<T> decodeChain(
            byte[] bytes,
            FrameCodec<T> codec,
            ChainValidator<T> chainValidator,
            int maxEntries,
            int maxFrameBytes,
            TailPolicy tailPolicy) throws StoreIOException {
        byte[] safeBytes = Objects.requireNonNull(bytes, "bytes cannot be null");
        FrameCodec<T> safeCodec = Objects.requireNonNull(codec, "codec cannot be null");
        ChainValidator<T> safeValidator = Objects.requireNonNull(
                chainValidator, "chainValidator cannot be null");
        if (maxEntries < 1 || maxFrameBytes < 1) {
            throw new IllegalArgumentException("frame bounds must be positive");
        }
        Objects.requireNonNull(tailPolicy, "tailPolicy cannot be null");
        if (safeBytes.length > maxBytes) {
            throw failure(
                    Failure.SIZE_LIMIT_EXCEEDED,
                    "chained JSONL replay exceeds its bounded byte size");
        }

        List<T> entries = new ArrayList<>();
        int start = 0;
        int completeBytes = 0;
        for (int index = 0; index < safeBytes.length; index++) {
            if (safeBytes[index] != '\n') {
                if (index - start >= maxFrameBytes) {
                    throw failure(
                            Failure.FRAME_SIZE_EXCEEDED,
                            "chained JSONL frame exceeds its bounded size");
                }
                continue;
            }
            if (index == start) {
                throw failure(
                        Failure.INVALID_COMPLETE_FRAME,
                        "chained JSONL contains an empty complete frame");
            }
            if (entries.size() >= maxEntries) {
                throw failure(
                        Failure.ENTRY_LIMIT_EXCEEDED,
                        "chained JSONL exceeds its bounded entry count");
            }
            byte[] encoded = Arrays.copyOfRange(safeBytes, start, index);
            T entry;
            try {
                entry = safeCodec.decode(encoded);
            } catch (RuntimeException exception) {
                throw failure(
                        Failure.INVALID_COMPLETE_FRAME,
                        "chained JSONL contains an invalid complete frame",
                        exception);
            }
            byte[] canonical;
            try {
                canonical = safeCodec.encode(entry);
            } catch (RuntimeException exception) {
                throw failure(
                        Failure.INVALID_COMPLETE_FRAME,
                        "chained JSONL frame could not be canonically re-encoded",
                        exception);
            }
            if (!Arrays.equals(encoded, canonical)) {
                throw failure(
                        Failure.NON_CANONICAL_FRAME,
                        "chained JSONL contains a non-canonical complete frame");
            }
            safeValidator.validateNext(entries, entry);
            entries.add(entry);
            start = index + 1;
            completeBytes = start;
        }

        int tailBytes = safeBytes.length - completeBytes;
        if (tailBytes > maxFrameBytes) {
            throw failure(
                    Failure.FRAME_SIZE_EXCEEDED,
                    "chained JSONL tail exceeds its bounded frame size");
        }
        if (tailBytes != 0 && tailPolicy == TailPolicy.REJECT) {
            throw failure(
                    Failure.INCOMPLETE_TAIL,
                    "chained JSONL has an incomplete final frame");
        }
        return new ChainReplay<>(
                entries,
                safeBytes.length,
                completeBytes,
                tailBytes);
    }

    /**
     * Appends one newline-terminated encoded frame and keeps the exclusive OS
     * lock through the selected force operation and final identity check.
     */
    void appendFrame(
            byte[] encoded,
            long expectedSize,
            ForceMode forceMode,
            Guard guard,
            Checkpoint afterWriteAttempt,
            Checkpoint afterAppendBeforeSync,
            Checkpoint afterSync) throws StoreIOException {
        synchronized (processMutex) {
            appendFrameLocked(
                    encoded,
                    expectedSize,
                    forceMode,
                    guard,
                    afterWriteAttempt,
                    afterAppendBeforeSync,
                    afterSync);
        }
    }

    private void appendFrameLocked(
            byte[] encoded,
            long expectedSize,
            ForceMode forceMode,
            Guard guard,
            Checkpoint afterWriteAttempt,
            Checkpoint afterAppendBeforeSync,
            Checkpoint afterSync) throws StoreIOException {
        byte[] safeEncoded = Objects.requireNonNull(encoded, "encoded cannot be null");
        if (safeEncoded.length == 0) {
            throw new IllegalArgumentException("encoded frame cannot be empty");
        }
        if (safeEncoded[safeEncoded.length - 1] == '\n') {
            throw new IllegalArgumentException(
                    "encoded frame must not include the JSONL delimiter");
        }
        if (expectedSize < 0L || expectedSize > maxBytes) {
            throw new IllegalArgumentException("expectedSize is outside the store bound");
        }
        Objects.requireNonNull(forceMode, "forceMode cannot be null");
        Guard safeGuard = Objects.requireNonNull(guard, "guard cannot be null");
        Checkpoint safeWriteObserver = Objects.requireNonNull(
                afterWriteAttempt, "afterWriteAttempt cannot be null");
        Checkpoint safeAppendObserver = Objects.requireNonNull(
                afterAppendBeforeSync, "afterAppendBeforeSync cannot be null");
        Checkpoint safeSyncObserver = Objects.requireNonNull(
                afterSync, "afterSync cannot be null");

        byte[] frame = Arrays.copyOf(safeEncoded, safeEncoded.length + 1);
        frame[frame.length - 1] = '\n';
        if (expectedSize + frame.length > maxBytes) {
            throw failure(
                    Failure.SIZE_LIMIT_EXCEEDED,
                    "chained JSONL append would exceed its bounded byte size");
        }

        FileIdentity before = pinCurrentIdentity();
        try (FileChannel channel = FileChannel.open(
                file,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND,
                LinkOption.NOFOLLOW_LINKS);
             FileLock ignored = channel.lock()) {
            requireCurrentIdentity(before);
            if (channel.size() != expectedSize) {
                throw failure(
                        Failure.CONCURRENT_CHANGE,
                        "chained JSONL file changed before append");
            }

            ByteBuffer buffer = ByteBuffer.wrap(frame);
            int zeroWrites = 0;
            int writtenBytes = 0;
            while (buffer.hasRemaining()) {
                safeGuard.requireCurrent();
                int written = channel.write(buffer);
                if (written == 0) {
                    if (++zeroWrites >= MAX_ZERO_PROGRESS) {
                        throw failure(
                                Failure.IO_FAILURE,
                                "bounded chained JSONL write made no progress");
                    }
                    continue;
                }
                zeroWrites = 0;
                writtenBytes += written;
                safeWriteObserver.checkpoint(writtenBytes);
            }

            safeAppendObserver.checkpoint(frame.length);
            safeGuard.requireCurrent();
            if (forceMode == ForceMode.DATA) {
                channel.force(false);
            } else if (forceMode == ForceMode.DATA_AND_METADATA) {
                channel.force(true);
            }
            if (forceMode != ForceMode.NONE) {
                safeSyncObserver.checkpoint(frame.length);
            }
            safeGuard.requireCurrent();
            requireCurrentIdentity(before);
            if (channel.size() != expectedSize + frame.length) {
                throw failure(
                        Failure.CONCURRENT_CHANGE,
                        "chained JSONL append did not produce the exact expected size");
            }
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "chained JSONL frame append did not complete under its exclusive lock",
                    exception);
        }
    }

    static FileIdentity identityOfControlledRegularFile(Path value)
            throws StoreIOException {
        Path file = Objects.requireNonNull(value, "file cannot be null")
                .toAbsolutePath().normalize();
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || !attributes.isRegularFile()) {
                throw failure(
                        Failure.UNSAFE_FILE,
                        "chained JSONL identity requires a non-symbolic-link regular file");
            }
            Path realParent = file.getParent().toRealPath();
            Path realFile = file.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!realFile.getParent().equals(realParent)) {
                throw failure(
                        Failure.UNSAFE_FILE,
                        "chained JSONL file escaped its controlled parent");
            }
            return new FileIdentity(
                    String.valueOf(attributes.fileKey()),
                    attributes.creationTime());
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "chained JSONL file identity could not be read",
                    exception);
        }
    }

    private FileIdentity pinCurrentIdentity() throws StoreIOException {
        FileIdentity current = identityOfControlledRegularFile(file);
        synchronized (this) {
            if (pinnedIdentity == null) {
                pinnedIdentity = current;
            } else if (!pinnedIdentity.equals(current)) {
                throw failure(
                        Failure.FILE_IDENTITY_CHANGED,
                        "chained JSONL path no longer names its pinned file identity");
            }
            return pinnedIdentity;
        }
    }

    private void requireCurrentIdentity(FileIdentity expected)
            throws StoreIOException {
        FileIdentity current = identityOfControlledRegularFile(file);
        if (!expected.equals(current)) {
            throw failure(
                    Failure.FILE_IDENTITY_CHANGED,
                    "chained JSONL file identity changed during the operation");
        }
        synchronized (this) {
            if (pinnedIdentity == null) {
                pinnedIdentity = current;
            } else if (!pinnedIdentity.equals(current)) {
                throw failure(
                        Failure.FILE_IDENTITY_CHANGED,
                        "chained JSONL path no longer names its pinned file identity");
            }
        }
    }

    private void requireBoundedSize(long size) throws StoreIOException {
        if (size < 0L || size > maxBytes) {
            throw failure(
                    Failure.SIZE_LIMIT_EXCEEDED,
                    "chained JSONL file exceeds its bounded byte size");
        }
    }

    private static StoreIOException failure(Failure failure, String message) {
        return new StoreIOException(failure, message);
    }

    private static StoreIOException failure(
            Failure failure,
            String message,
            Throwable cause) {
        return new StoreIOException(failure, message, cause);
    }

    enum ForceMode {
        NONE,
        DATA,
        DATA_AND_METADATA
    }

    enum Failure {
        SIZE_LIMIT_EXCEEDED,
        ENTRY_LIMIT_EXCEEDED,
        FRAME_SIZE_EXCEEDED,
        INVALID_COMPLETE_FRAME,
        NON_CANONICAL_FRAME,
        INCOMPLETE_TAIL,
        CONCURRENT_CHANGE,
        FILE_IDENTITY_CHANGED,
        UNSAFE_FILE,
        IO_FAILURE
    }

    @FunctionalInterface
    interface Guard {
        void requireCurrent();
    }

    @FunctionalInterface
    interface Checkpoint {
        void checkpoint(int bytesWritten) throws IOException;
    }

    interface FrameCodec<T> {
        T decode(byte[] encoded);

        byte[] encode(T value);
    }

    @FunctionalInterface
    interface ChainValidator<T> {
        void validateNext(List<T> prior, T next);
    }

    enum TailPolicy {
        REJECT,
        ALLOW
    }

    record ChainReplay<T>(
            List<T> entries,
            long totalBytes,
            long completeBytes,
            long tailBytes) {
        ChainReplay {
            entries = List.copyOf(Objects.requireNonNull(entries, "entries cannot be null"));
            if (totalBytes < 0L
                    || completeBytes < 0L
                    || tailBytes < 0L
                    || completeBytes + tailBytes != totalBytes) {
                throw new IllegalArgumentException(
                        "chained JSONL replay byte counts are inconsistent");
            }
        }
    }

    record FileIdentity(String fileKey, FileTime creationTime) {
        FileIdentity {
            fileKey = Objects.requireNonNull(fileKey, "fileKey cannot be null");
            creationTime = Objects.requireNonNull(
                    creationTime, "creationTime cannot be null");
        }
    }

    static final class StoreIOException extends IOException {
        private final Failure failure;

        private StoreIOException(Failure failure, String message) {
            super(message);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        private StoreIOException(
                Failure failure,
                String message,
                Throwable cause) {
            super(message, cause);
            this.failure = Objects.requireNonNull(failure, "failure cannot be null");
        }

        Failure failure() {
            return failure;
        }
    }
}
