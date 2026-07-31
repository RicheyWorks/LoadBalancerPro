package com.richmond423.loadbalancerpro.lab;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared bounded-I/O engine for the local append-only JSONL chains.
 *
 * <p>Store-specific codecs and chain-transition rules remain with their
 * owning domain types. This engine owns the mechanics which must not diverge:
 * one process mutex per fixed path, cooperating OS file locks, one complete
 * frame buffer per append, bounded reads, synchronization while the exclusive
 * lock is still held, and pinned file-key plus creation-time identity.</p>
 */
final class ChainedJsonlStore implements AutoCloseable {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ChainedJsonlStore.class);
    static final String SEGMENTS_DIRECTORY_NAME = "segments-v1";
    static final String REPAIR_QUARANTINE_DIRECTORY_NAME =
            "repair-quarantine-v1";
    static final int HARD_MAX_ARCHIVED_SEGMENTS = 64;

    private static final int READ_BUFFER_BYTES = 8_192;
    private static final int MAX_ZERO_PROGRESS = 3;
    private static final Pattern SEGMENT_FILE_NAME =
            Pattern.compile("segment-v1-([0-9]{8})\\.jsonl");
    private static final Pattern SEGMENT_INSTALLING_FILE_NAME =
            Pattern.compile("segment-v1-([0-9]{8})\\.jsonl\\.installing");
    private static final Pattern REPAIR_INSTALLING_FILE_NAME =
            Pattern.compile("repair-v1-[0-9a-f]{64}\\.jsonl\\.installing");
    private static final Pattern REPAIR_FILE_NAME =
            Pattern.compile("repair-v1-([0-9a-f]{64})\\.jsonl");
    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_PERMISSIONS =
            PosixFilePermissions.fromString("rw-------");
    private static final Cleaner PROCESS_MUTEX_CLEANER = Cleaner.create();
    private static final Map<Path, ProcessMutexEntry> PROCESS_MUTEXES =
            new HashMap<>();

    private final Path file;
    private final Path segmentsDirectory;
    private final Path repairQuarantineDirectory;
    private final long maxBytes;
    private final Object processMutex;
    private final EnterpriseLabStorageDurability.DirectorySyncer directorySyncer;
    private final Cleaner.Cleanable processMutexCleanable;
    private FileIdentity pinnedIdentity;
    private StorageVersion lastObservedVersion;
    private volatile boolean closed;

    ChainedJsonlStore(Path file, long maxBytes) {
        this(
                file,
                maxBytes,
                EnterpriseLabStorageDurability.SYSTEM_DIRECTORY_SYNCER);
    }

    ChainedJsonlStore(
            Path file,
            long maxBytes,
            EnterpriseLabStorageDurability.DirectorySyncer directorySyncer) {
        this.file = Objects.requireNonNull(file, "file cannot be null")
                .toAbsolutePath().normalize();
        if (this.file.getParent() == null) {
            throw new IllegalArgumentException("file must have a controlled parent");
        }
        this.segmentsDirectory = this.file.getParent()
                .resolve(SEGMENTS_DIRECTORY_NAME)
                .toAbsolutePath()
                .normalize();
        if (!this.segmentsDirectory.getParent().equals(this.file.getParent())) {
            throw new IllegalArgumentException(
                    "segments directory must remain beneath the controlled parent");
        }
        this.repairQuarantineDirectory = this.file.getParent()
                .resolve(REPAIR_QUARANTINE_DIRECTORY_NAME)
                .toAbsolutePath()
                .normalize();
        if (!this.repairQuarantineDirectory.getParent()
                .equals(this.file.getParent())) {
            throw new IllegalArgumentException(
                    "repair quarantine must remain beneath the controlled parent");
        }
        if (maxBytes < 1L || maxBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "maxBytes must fit the bounded in-memory replay contract");
        }
        this.maxBytes = maxBytes;
        this.directorySyncer = Objects.requireNonNull(
                directorySyncer, "directorySyncer cannot be null");
        ProcessMutexLease lease = acquireProcessMutex(this.file);
        this.processMutex = lease.entry().mutex();
        this.processMutexCleanable =
                PROCESS_MUTEX_CLEANER.register(this, lease);
    }

    Object processMutex() {
        if (closed) {
            throw new IllegalStateException("chained JSONL store is closed");
        }
        return processMutex;
    }

    static int processMutexCountForTesting() {
        synchronized (PROCESS_MUTEXES) {
            return PROCESS_MUTEXES.size();
        }
    }

    @Override
    public void close() {
        synchronized (processMutex) {
            if (closed) {
                return;
            }
            closed = true;
            processMutexCleanable.clean();
        }
    }

    Path file() {
        return file;
    }

    Path segmentsDirectory() {
        return segmentsDirectory;
    }

    Path repairQuarantineDirectory() {
        return repairQuarantineDirectory;
    }

    void validateRepairQuarantine() throws StoreIOException {
        synchronized (processMutex) {
            requireOpen();
            if (!Files.exists(
                    repairQuarantineDirectory,
                    LinkOption.NOFOLLOW_LINKS)) {
                return;
            }
            validateControlledDirectory(
                    repairQuarantineDirectory,
                    "repair quarantine");
            int observed = 0;
            try (var entries = Files.newDirectoryStream(
                repairQuarantineDirectory)) {
                for (Path path : entries) {
                    if (++observed > HARD_MAX_ARCHIVED_SEGMENTS) {
                        throw failure(
                                Failure.ARCHIVE_LIMIT_EXCEEDED,
                                "repair quarantine exceeds its bounded entry count");
                    }
                    Matcher repair = REPAIR_FILE_NAME.matcher(
                            path.getFileName().toString());
                    if (!repair.matches()) {
                        throw failure(
                                Failure.UNSAFE_FILE,
                                "repair quarantine contains an unexpected entry");
                    }
                    byte[] bytes = readControlledRepairBytes(path);
                    if (bytes.length < 1) {
                        throw failure(
                                Failure.SIZE_LIMIT_EXCEEDED,
                                "repair quarantine entry must retain source bytes");
                    }
                    if (!sha256(bytes).equals(repair.group(1))) {
                        throw failure(
                                Failure.CONCURRENT_CHANGE,
                                "repair quarantine fingerprint does not match its source bytes");
                    }
                }
            } catch (StoreIOException exception) {
                throw exception;
            } catch (IOException exception) {
                throw failure(
                        Failure.IO_FAILURE,
                        "repair quarantine could not be inspected",
                        exception);
            }
        }
    }

    /**
     * Fails when a file previously observed by this engine has disappeared.
     * A never-created read-only store remains a valid empty store.
     */
    void requireMissingFileWasNeverObserved() throws StoreIOException {
        synchronized (processMutex) {
            requireOpen();
            synchronized (this) {
                if (pinnedIdentity != null) {
                    throw failure(
                            Failure.FILE_IDENTITY_CHANGED,
                            "the pinned chained JSONL file is no longer present");
                }
            }
        }
    }

    byte[] readBoundedBytes() throws StoreIOException {
        synchronized (processMutex) {
            requireOpen();
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
            lastObservedVersion = storageVersionUnderLock(channel);
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

    StorageVersion storageVersion() throws StoreIOException {
        synchronized (processMutex) {
            requireOpen();
            if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.exists(segmentsDirectory, LinkOption.NOFOLLOW_LINKS)
                        && !listSegmentsLocked().isEmpty()) {
                    throw failure(
                            Failure.FILE_IDENTITY_CHANGED,
                            "archived chained JSONL segments exist without the fixed current file");
                }
                return StorageVersion.missing();
            }
            FileIdentity before = pinCurrentIdentity();
            try (FileChannel channel = FileChannel.open(
                    file,
                    StandardOpenOption.READ,
                    LinkOption.NOFOLLOW_LINKS);
                 FileLock ignored = channel.lock(0L, Long.MAX_VALUE, true)) {
                requireCurrentIdentity(before);
                StorageVersion version = storageVersionUnderLock(channel);
                lastObservedVersion = version;
                return version;
            } catch (StoreIOException exception) {
                throw exception;
            } catch (IOException | UnsupportedOperationException exception) {
                throw failure(
                        Failure.IO_FAILURE,
                        "chained JSONL storage version could not be inspected",
                        exception);
            }
        }
    }

    void prepareRotationDirectory() throws StoreIOException {
        synchronized (processMutex) {
            requireOpen();
            prepareRotationDirectoryLocked();
        }
    }

    RotationRecovery recoverRotationArtifacts(Guard guard)
            throws StoreIOException {
        synchronized (processMutex) {
            requireOpen();
            Guard safeGuard = Objects.requireNonNull(guard, "guard cannot be null");
            EnterpriseLabDirectorySyncStatus directorySyncStatus =
                    prepareRotationDirectoryLocked();
            if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                directorySyncStatus = EnterpriseLabDirectorySyncStatus.combine(
                        directorySyncStatus,
                        cleanupExistingRepairInstallingFilesLocked(safeGuard));
                InstallingCleanup cleanup =
                        removeInstallingFilesLocked(safeGuard);
                directorySyncStatus = EnterpriseLabDirectorySyncStatus.combine(
                        directorySyncStatus,
                        cleanup.directorySyncStatus());
                return new RotationRecovery(
                        cleanup.removedFiles(),
                        false,
                        directorySyncStatus);
            }

            FileIdentity before = pinCurrentIdentity();
            try (FileChannel channel = FileChannel.open(
                    file,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS);
                 FileLock ignored = channel.lock()) {
                safeGuard.requireCurrent();
                requireCurrentIdentity(before);
                InstallingCleanup cleanup =
                        removeInstallingFilesLocked(safeGuard);
                directorySyncStatus = EnterpriseLabDirectorySyncStatus.combine(
                        directorySyncStatus,
                        cleanup.directorySyncStatus());
                directorySyncStatus = EnterpriseLabDirectorySyncStatus.combine(
                        directorySyncStatus,
                        cleanupExistingRepairInstallingFilesLocked(safeGuard));
                List<Path> segments = listSegmentsLocked();
                if (segments.isEmpty()) {
                    return new RotationRecovery(
                            cleanup.removedFiles(),
                            false,
                            directorySyncStatus);
                }
                byte[] current = readChannelBounded(channel);
                byte[] lastArchive = readArchiveBytes(
                        segments.get(segments.size() - 1));
                if (!Arrays.equals(lastArchive, current)) {
                    return new RotationRecovery(
                            cleanup.removedFiles(),
                            false,
                            directorySyncStatus);
                }
                safeGuard.requireCurrent();
                directorySyncStatus = EnterpriseLabDirectorySyncStatus.combine(
                        directorySyncStatus,
                        synchronizeDirectory(
                                segmentsDirectory,
                                "pending chained JSONL rotation"));
                channel.truncate(0L);
                channel.force(true);
                requireCurrentIdentity(before);
                lastObservedVersion = storageVersionUnderLock(channel);
                return new RotationRecovery(
                        cleanup.removedFiles(),
                        true,
                        directorySyncStatus);
            } catch (StoreIOException exception) {
                throw exception;
            } catch (IOException | UnsupportedOperationException exception) {
                throw failure(
                        Failure.IO_FAILURE,
                        "chained JSONL rotation recovery did not complete",
                        exception);
            }
        }
    }

    RotationReceipt rotateCurrentSegment(Guard guard)
            throws StoreIOException {
        synchronized (processMutex) {
            requireOpen();
            Guard safeGuard = Objects.requireNonNull(guard, "guard cannot be null");
            EnterpriseLabDirectorySyncStatus directorySyncStatus =
                    prepareRotationDirectoryLocked();
            FileIdentity before = pinCurrentIdentity();
            try (FileChannel channel = FileChannel.open(
                    file,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS);
                 FileLock ignored = channel.lock()) {
                safeGuard.requireCurrent();
                requireCurrentIdentity(before);
                requireLastObservedVersion(channel);
                if (!listInstallingFilesLocked().isEmpty()) {
                    throw failure(
                            Failure.CONCURRENT_CHANGE,
                            "chained JSONL rotation has an unresolved installing file");
                }
                byte[] current = readChannelBounded(channel);
                if (current.length == 0) {
                    return RotationReceipt.notRotated(directorySyncStatus);
                }
                if (current[current.length - 1] != '\n') {
                    throw failure(
                            Failure.INCOMPLETE_TAIL,
                            "chained JSONL cannot rotate an incomplete current segment");
                }

                List<Path> segments = listSegmentsLocked();
                if (segments.size() >= HARD_MAX_ARCHIVED_SEGMENTS) {
                    throw failure(
                            Failure.ARCHIVE_LIMIT_EXCEEDED,
                            "chained JSONL has reached its bounded archive count");
                }
                int nextIndex = segments.size() + 1;
                Path destination = segmentPath(nextIndex);
                Path installing = installingPath(nextIndex);
                if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)
                        || Files.exists(installing, LinkOption.NOFOLLOW_LINKS)) {
                    throw failure(
                            Failure.CONCURRENT_CHANGE,
                            "chained JSONL rotation destination already exists");
                }

                safeGuard.requireCurrent();
                writeInstallingArchive(installing, current);
                safeGuard.requireCurrent();
                Files.move(
                        installing,
                        destination,
                        StandardCopyOption.ATOMIC_MOVE);
                restrictPermissions(destination, FILE_PERMISSIONS);
                directorySyncStatus = EnterpriseLabDirectorySyncStatus.combine(
                        directorySyncStatus,
                        synchronizeMove(
                                installing,
                                destination,
                                "chained JSONL archive installation"));
                if (!Arrays.equals(current, readArchiveBytes(destination))) {
                    throw failure(
                            Failure.CONCURRENT_CHANGE,
                            "installed chained JSONL archive failed exact read-back");
                }

                safeGuard.requireCurrent();
                channel.truncate(0L);
                channel.force(true);
                safeGuard.requireCurrent();
                requireCurrentIdentity(before);
                if (channel.size() != 0L) {
                    throw failure(
                            Failure.CONCURRENT_CHANGE,
                            "chained JSONL current segment did not truncate exactly");
                }
                lastObservedVersion = storageVersionUnderLock(channel);
                return new RotationReceipt(
                        true,
                        nextIndex,
                        current.length,
                        directorySyncStatus);
            } catch (StoreIOException exception) {
                throw exception;
            } catch (IOException | UnsupportedOperationException exception) {
                throw failure(
                        Failure.IO_FAILURE,
                        "chained JSONL archive-and-truncate rotation did not complete",
                        exception);
            }
        }
    }

    TailRepairReceipt repairTruncatedCurrentTail(
            long expectedCompleteBytes,
            long expectedTailBytes,
            StorageVersion expectedVersion,
            Guard guard) throws StoreIOException {
        if (expectedCompleteBytes < 0L
                || expectedTailBytes < 1L
                || expectedCompleteBytes + expectedTailBytes > maxBytes) {
            throw new IllegalArgumentException(
                    "tail repair byte bounds are inconsistent");
        }
        StorageVersion safeExpected = Objects.requireNonNull(
                expectedVersion, "expectedVersion cannot be null");
        Guard safeGuard = Objects.requireNonNull(
                guard, "guard cannot be null");
        synchronized (processMutex) {
            requireOpen();
            safeGuard.requireCurrent();
            EnterpriseLabDirectorySyncStatus directorySyncStatus =
                    prepareRepairQuarantineDirectoryLocked();
            FileIdentity before = pinCurrentIdentity();
            try (FileChannel channel = FileChannel.open(
                    file,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS);
                 FileLock ignored = channel.lock()) {
                safeGuard.requireCurrent();
                requireCurrentIdentity(before);
                StorageVersion actual = storageVersionUnderLock(channel);
                if (!safeExpected.equals(actual)
                        || !safeExpected.equals(lastObservedVersion)) {
                    throw failure(
                            Failure.CONCURRENT_CHANGE,
                            "chained JSONL changed after its repair plan");
                }
                byte[] current = readChannelBounded(channel);
                if (current.length
                        != expectedCompleteBytes + expectedTailBytes
                        || current[current.length - 1] == '\n') {
                    throw failure(
                            Failure.CONCURRENT_CHANGE,
                            "chained JSONL tail no longer matches its repair plan");
                }
                String fingerprint = sha256(current);
                Path quarantine = repairQuarantineDirectory.resolve(
                        "repair-v1-" + fingerprint + ".jsonl")
                        .toAbsolutePath()
                        .normalize();
                Path installing = quarantine.resolveSibling(
                        quarantine.getFileName() + ".installing")
                        .toAbsolutePath()
                        .normalize();
                requireRepairPath(quarantine);
                requireRepairPath(installing);
                directorySyncStatus = EnterpriseLabDirectorySyncStatus.combine(
                        directorySyncStatus,
                        cleanupRepairInstallingFilesLocked(safeGuard));
                safeGuard.requireCurrent();
                directorySyncStatus = EnterpriseLabDirectorySyncStatus.combine(
                        directorySyncStatus,
                        installRepairQuarantine(
                                installing, quarantine, current));
                if (!Arrays.equals(
                        current, readControlledRepairBytes(quarantine))) {
                    throw failure(
                            Failure.CONCURRENT_CHANGE,
                            "repair quarantine failed exact read-back");
                }
                safeGuard.requireCurrent();
                channel.truncate(expectedCompleteBytes);
                channel.force(true);
                if (channel.size() != expectedCompleteBytes) {
                    throw failure(
                            Failure.CONCURRENT_CHANGE,
                            "chained JSONL repair did not truncate at the exact frame boundary");
                }
                requireCurrentIdentity(before);
                lastObservedVersion = storageVersionUnderLock(channel);
                return new TailRepairReceipt(
                        expectedTailBytes,
                        fingerprint,
                        quarantine.getFileName().toString(),
                        true,
                        directorySyncStatus);
            } catch (StoreIOException exception) {
                throw exception;
            } catch (IOException | UnsupportedOperationException exception) {
                throw failure(
                        Failure.IO_FAILURE,
                        "chained JSONL tail repair did not complete",
                        exception);
            }
        }
    }

    <T> SegmentedChainReplay<T> replaySegmentedChain(
            FrameCodec<T> codec,
            ChainValidator<T> chainValidator,
            int maxEntriesPerSegment,
            int maxFrameBytes,
            TailPolicy tailPolicy) throws StoreIOException {
        synchronized (processMutex) {
            requireOpen();
            SegmentedBytes source = readSegmentedBytesLocked();
            SegmentedChainReplay<T> replay = decodeSegmentedChain(
                    source,
                    codec,
                    chainValidator,
                    maxEntriesPerSegment,
                    maxFrameBytes,
                    tailPolicy);
            lastObservedVersion = replay.version();
            return replay;
        }
    }

    <T> ChainReplay<T> replayChain(
            FrameCodec<T> codec,
            ChainValidator<T> chainValidator,
            int maxEntries,
            int maxFrameBytes,
            TailPolicy tailPolicy) throws StoreIOException {
        synchronized (processMutex) {
            requireOpen();
            return decodeChain(
                    readBoundedBytesLocked(),
                    codec,
                    chainValidator,
                    maxEntries,
                    maxFrameBytes,
                    tailPolicy);
        }
    }

    private <T> SegmentedChainReplay<T> decodeSegmentedChain(
            SegmentedBytes source,
            FrameCodec<T> codec,
            ChainValidator<T> chainValidator,
            int maxEntriesPerSegment,
            int maxFrameBytes,
            TailPolicy tailPolicy) throws StoreIOException {
        SegmentedBytes safeSource = Objects.requireNonNull(
                source, "source cannot be null");
        FrameCodec<T> safeCodec = Objects.requireNonNull(
                codec, "codec cannot be null");
        ChainValidator<T> safeValidator = Objects.requireNonNull(
                chainValidator, "chainValidator cannot be null");
        if (maxEntriesPerSegment < 1 || maxFrameBytes < 1) {
            throw new IllegalArgumentException("segment frame bounds must be positive");
        }
        Objects.requireNonNull(tailPolicy, "tailPolicy cannot be null");

        List<T> entries = new ArrayList<>();
        long totalBytes = 0L;
        int currentEntries = 0;
        long currentCompleteBytes = 0L;
        long currentTailBytes = 0L;
        for (int segmentIndex = 0;
             segmentIndex < safeSource.segments().size();
             segmentIndex++) {
            SegmentBytes segment = safeSource.segments().get(segmentIndex);
            boolean current = segment.current();
            byte[] bytes = segment.bytes();
            if (bytes.length > maxBytes) {
                throw failure(
                        Failure.SIZE_LIMIT_EXCEEDED,
                        "chained JSONL segment exceeds its bounded byte size");
            }
            int start = 0;
            int segmentEntries = 0;
            int completeBytes = 0;
            for (int index = 0; index < bytes.length; index++) {
                if (bytes[index] != '\n') {
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
                if (segmentEntries >= maxEntriesPerSegment) {
                    throw failure(
                            Failure.ENTRY_LIMIT_EXCEEDED,
                            "chained JSONL segment exceeds its bounded entry count");
                }
                byte[] encoded = Arrays.copyOfRange(bytes, start, index);
                T entry = decodeCanonicalFrame(encoded, safeCodec);
                safeValidator.validateNext(entries, entry);
                entries.add(entry);
                segmentEntries++;
                start = index + 1;
                completeBytes = start;
            }
            int tailBytes = bytes.length - completeBytes;
            if (tailBytes > maxFrameBytes) {
                throw failure(
                        Failure.FRAME_SIZE_EXCEEDED,
                        "chained JSONL tail exceeds its bounded frame size");
            }
            if (!current && tailBytes != 0) {
                throw failure(
                        Failure.INCOMPLETE_TAIL,
                        "archived chained JSONL segment has an incomplete tail");
            }
            if (current && tailBytes != 0 && tailPolicy == TailPolicy.REJECT) {
                throw failure(
                        Failure.INCOMPLETE_TAIL,
                        "chained JSONL has an incomplete final frame");
            }
            totalBytes = Math.addExact(totalBytes, bytes.length);
            if (current) {
                currentEntries = segmentEntries;
                currentCompleteBytes = completeBytes;
                currentTailBytes = tailBytes;
            }
        }
            return new SegmentedChainReplay<>(
                entries,
                totalBytes,
                currentCompleteBytes,
                currentTailBytes,
                currentEntries,
                safeSource.archiveCount(),
                safeSource.currentBytes(),
                safeSource.version());
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
            requireOpen();
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
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS);
             FileLock ignored = channel.lock()) {
            requireCurrentIdentity(before);
            requireLastObservedVersion(channel);
            if (channel.size() != expectedSize) {
                throw failure(
                        Failure.CONCURRENT_CHANGE,
                        "chained JSONL file changed before append");
            }
            channel.position(expectedSize);

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
            ByteBuffer readBack = ByteBuffer.allocate(frame.length);
            long offset = expectedSize;
            int zeroReads = 0;
            while (readBack.hasRemaining()) {
                int read = channel.read(readBack, offset + readBack.position());
                if (read < 0) {
                    break;
                }
                if (read == 0) {
                    if (++zeroReads >= MAX_ZERO_PROGRESS) {
                        throw failure(
                                Failure.IO_FAILURE,
                                "chained JSONL append read-back made no progress");
                    }
                    continue;
                }
                zeroReads = 0;
            }
            if (readBack.hasRemaining()
                    || !Arrays.equals(frame, readBack.array())) {
                throw failure(
                        Failure.CONCURRENT_CHANGE,
                        "chained JSONL appended frame failed exact read-back");
            }
            lastObservedVersion = storageVersionUnderLock(channel);
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "chained JSONL frame append did not complete under its exclusive lock",
                    exception);
        }
    }

    private SegmentedBytes readSegmentedBytesLocked() throws StoreIOException {
        boolean currentExists = Files.exists(file, LinkOption.NOFOLLOW_LINKS);
        boolean segmentsExist = Files.exists(
                segmentsDirectory, LinkOption.NOFOLLOW_LINKS);
        if (!currentExists) {
            if (segmentsExist && !listSegmentsLocked().isEmpty()) {
                throw failure(
                        Failure.FILE_IDENTITY_CHANGED,
                        "archived chained JSONL segments exist without the fixed current file");
            }
            return SegmentedBytes.empty();
        }

        FileIdentity before = pinCurrentIdentity();
        try (FileChannel channel = FileChannel.open(
                file,
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS);
             FileLock ignored = channel.lock(0L, Long.MAX_VALUE, true)) {
            requireCurrentIdentity(before);
            List<Path> paths = segmentsExist
                    ? listSegmentsLocked()
                    : List.of();
            List<SegmentBytes> segments = new ArrayList<>(paths.size() + 1);
            List<FileVersion> archiveVersions =
                    new ArrayList<>(paths.size());
            for (Path path : paths) {
                segments.add(new SegmentBytes(
                        path.getFileName().toString(),
                        readArchiveBytes(path),
                        false));
                archiveVersions.add(fileVersion(path));
            }
            byte[] current = readChannelBounded(channel);
            if (!segments.isEmpty()
                    && Arrays.equals(
                    segments.get(segments.size() - 1).bytes(),
                    current)) {
                current = new byte[0];
            }
            segments.add(new SegmentBytes(
                    file.getFileName().toString(),
                    current,
                    true));
            requireCurrentIdentity(before);
            return new SegmentedBytes(
                    segments,
                    paths.size(),
                    current.length,
                    new StorageVersion(
                            true,
                            archiveVersions,
                            fileVersion(file)));
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "segmented chained JSONL replay could not acquire its shared lock",
                    exception);
        }
    }

    private byte[] readChannelBounded(FileChannel channel)
            throws IOException, StoreIOException {
        long declaredSize = channel.size();
        requireBoundedSize(declaredSize);
        channel.position(0L);
        ByteArrayOutputStream output = new ByteArrayOutputStream(
                Math.toIntExact(declaredSize));
        ByteBuffer buffer = ByteBuffer.allocate(READ_BUFFER_BYTES);
        long observed = 0L;
        int zeroReads = 0;
        while (observed < declaredSize) {
            int read = channel.read(buffer);
            if (read < 0) {
                break;
            }
            if (read == 0) {
                if (++zeroReads >= MAX_ZERO_PROGRESS) {
                    throw failure(
                            Failure.IO_FAILURE,
                            "bounded chained JSONL segment read made no progress");
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
                    "chained JSONL segment changed during bounded replay");
        }
        return output.toByteArray();
    }

    private byte[] readArchiveBytes(Path path) throws StoreIOException {
        FileIdentity before = identityOfControlledRegularFile(path);
        try (FileChannel channel = FileChannel.open(
                path,
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = readChannelBounded(channel);
            FileIdentity after = identityOfControlledRegularFile(path);
            if (!before.equals(after)) {
                throw failure(
                        Failure.FILE_IDENTITY_CHANGED,
                        "chained JSONL archive identity changed during replay");
            }
            if (bytes.length == 0 || bytes[bytes.length - 1] != '\n') {
                throw failure(
                        Failure.INCOMPLETE_TAIL,
                        "chained JSONL archive must contain complete frames");
            }
            return bytes;
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "chained JSONL archive could not be read",
                    exception);
        }
    }

    private static FileVersion fileVersion(Path path)
            throws StoreIOException {
        Path safe = Objects.requireNonNull(path, "path cannot be null")
                .toAbsolutePath()
                .normalize();
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    safe,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || !attributes.isRegularFile()) {
                throw failure(
                        Failure.UNSAFE_FILE,
                        "chained JSONL version requires a controlled regular file");
            }
            return new FileVersion(
                    safe.getFileName().toString(),
                    new FileIdentity(
                            String.valueOf(attributes.fileKey()),
                            attributes.creationTime()),
                    attributes.size(),
                    attributes.lastModifiedTime());
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "chained JSONL file version could not be read",
                    exception);
        }
    }

    private StorageVersion storageVersionUnderLock(FileChannel currentChannel)
            throws StoreIOException, IOException {
        List<FileVersion> archives = new ArrayList<>();
        for (Path path : listSegmentsLocked()) {
            archives.add(fileVersion(path));
        }
        FileVersion current = fileVersion(file);
        if (currentChannel.size() != current.size()) {
            throw failure(
                    Failure.CONCURRENT_CHANGE,
                    "chained JSONL current size changed during version inspection");
        }
        requireCurrentIdentity(current.identity());
        return new StorageVersion(true, archives, current);
    }

    private void requireLastObservedVersion(FileChannel currentChannel)
            throws StoreIOException, IOException {
        if (lastObservedVersion == null) {
            return;
        }
        StorageVersion current = storageVersionUnderLock(currentChannel);
        if (!lastObservedVersion.equals(current)) {
            throw failure(
                    Failure.CONCURRENT_CHANGE,
                    "chained JSONL storage changed after its last verified replay");
        }
    }

    private EnterpriseLabDirectorySyncStatus prepareRotationDirectoryLocked()
            throws StoreIOException {
        validateControlledParent();
        if (!Files.exists(segmentsDirectory, LinkOption.NOFOLLOW_LINKS)) {
            try {
                try {
                    Files.createDirectory(
                            segmentsDirectory,
                            PosixFilePermissions.asFileAttribute(
                                    DIRECTORY_PERMISSIONS));
                } catch (UnsupportedOperationException exception) {
                    Files.createDirectory(segmentsDirectory);
                }
            } catch (java.nio.file.FileAlreadyExistsException ignored) {
                // A cooperating initializer is accepted only after validation.
            } catch (IOException exception) {
                throw failure(
                        Failure.IO_FAILURE,
                        "chained JSONL archive directory could not be created",
                        exception);
            }
        }
        validateRotationDirectory();
        try {
            restrictPermissions(segmentsDirectory, DIRECTORY_PERMISSIONS);
        } catch (IOException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "chained JSONL archive directory permissions could not be restricted",
                    exception);
        }
        return synchronizeDirectory(
                segmentsDirectory.getParent(),
                "chained JSONL archive-directory preparation");
    }

    private void validateControlledParent() throws StoreIOException {
        Path parent = file.getParent();
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    parent,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
                throw failure(
                        Failure.UNSAFE_FILE,
                        "chained JSONL controlled parent is not a safe directory");
            }
            if (!parent.toRealPath().equals(parent)) {
                throw failure(
                        Failure.UNSAFE_FILE,
                        "chained JSONL controlled parent identity is unsafe");
            }
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "chained JSONL controlled parent could not be validated",
                    exception);
        }
    }

    private void validateRotationDirectory() throws StoreIOException {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    segmentsDirectory,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink()
                    || !attributes.isDirectory()
                    || !segmentsDirectory.getParent().equals(file.getParent())
                    || !segmentsDirectory.toRealPath().getParent()
                    .equals(file.getParent().toRealPath())) {
                throw failure(
                        Failure.UNSAFE_FILE,
                        "chained JSONL archive directory identity is unsafe");
            }
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "chained JSONL archive directory could not be validated",
                    exception);
        }
    }

    private EnterpriseLabDirectorySyncStatus prepareRepairQuarantineDirectoryLocked()
            throws StoreIOException {
        validateControlledParent();
        if (!Files.exists(
                repairQuarantineDirectory,
                LinkOption.NOFOLLOW_LINKS)) {
            try {
                try {
                    Files.createDirectory(
                            repairQuarantineDirectory,
                            PosixFilePermissions.asFileAttribute(
                                    DIRECTORY_PERMISSIONS));
                } catch (UnsupportedOperationException exception) {
                    Files.createDirectory(repairQuarantineDirectory);
                }
            } catch (java.nio.file.FileAlreadyExistsException ignored) {
                // A competing creator is accepted only after validation.
            } catch (IOException exception) {
                throw failure(
                        Failure.IO_FAILURE,
                        "repair quarantine directory could not be created",
                        exception);
            }
        }
        validateControlledDirectory(
                repairQuarantineDirectory,
                "repair quarantine");
        try {
            restrictPermissions(
                    repairQuarantineDirectory,
                    DIRECTORY_PERMISSIONS);
        } catch (IOException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "repair quarantine permissions could not be restricted",
                    exception);
        }
        return synchronizeDirectory(
                repairQuarantineDirectory.getParent(),
                "repair-quarantine directory preparation");
    }

    private void validateControlledDirectory(
            Path directory,
            String description) throws StoreIOException {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    directory,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink()
                    || !attributes.isDirectory()
                    || !directory.getParent().equals(file.getParent())
                    || !directory.toRealPath().getParent()
                    .equals(file.getParent().toRealPath())) {
                throw failure(
                        Failure.UNSAFE_FILE,
                        description + " directory identity is unsafe");
            }
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    description + " directory could not be validated",
                    exception);
        }
    }

    private EnterpriseLabDirectorySyncStatus cleanupRepairInstallingFilesLocked(
            Guard guard)
            throws StoreIOException {
        EnterpriseLabDirectorySyncStatus directorySyncStatus =
                EnterpriseLabDirectorySyncStatus.NOT_REQUIRED_EXISTING_ENTRY;
        try (var entries = Files.newDirectoryStream(
                repairQuarantineDirectory)) {
            for (Path path : entries) {
                if (!REPAIR_INSTALLING_FILE_NAME
                        .matcher(path.getFileName().toString())
                        .matches()) {
                    continue;
                }
                FileIdentity ignored =
                        identityOfControlledRegularFile(path);
                guard.requireCurrent();
                Files.delete(path);
                directorySyncStatus = EnterpriseLabDirectorySyncStatus.combine(
                        directorySyncStatus,
                        synchronizeDirectory(
                                repairQuarantineDirectory,
                                "repair installing-file deletion"));
            }
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "repair installing-file cleanup failed",
                    exception);
        }
        return directorySyncStatus;
    }

    private EnterpriseLabDirectorySyncStatus
    cleanupExistingRepairInstallingFilesLocked(Guard guard)
            throws StoreIOException {
        if (!Files.exists(
                repairQuarantineDirectory,
                LinkOption.NOFOLLOW_LINKS)) {
            return EnterpriseLabDirectorySyncStatus
                    .NOT_REQUIRED_EXISTING_ENTRY;
        }
        validateControlledDirectory(
                repairQuarantineDirectory,
                "repair quarantine");
        EnterpriseLabDirectorySyncStatus directorySyncStatus =
                cleanupRepairInstallingFilesLocked(guard);
        validateRepairQuarantine();
        return directorySyncStatus;
    }

    private EnterpriseLabDirectorySyncStatus installRepairQuarantine(
            Path installing,
            Path destination,
            byte[] bytes) throws StoreIOException {
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            if (!Arrays.equals(
                    bytes, readControlledRepairBytes(destination))) {
                throw failure(
                        Failure.CONCURRENT_CHANGE,
                        "existing repair quarantine does not match source bytes");
            }
            return synchronizeDirectory(
                    repairQuarantineDirectory,
                    "existing repair-quarantine installation");
        }
        try (FileChannel channel = FileChannel.open(
                installing,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS)) {
            restrictPermissions(installing, FILE_PERMISSIONS);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            int zeroWrites = 0;
            while (buffer.hasRemaining()) {
                int written = channel.write(buffer);
                if (written == 0) {
                    if (++zeroWrites >= MAX_ZERO_PROGRESS) {
                        throw failure(
                                Failure.IO_FAILURE,
                                "repair quarantine write made no progress");
                    }
                    continue;
                }
                zeroWrites = 0;
            }
            channel.force(true);
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "repair quarantine could not be synchronized",
                    exception);
        }
        try {
            Files.move(
                    installing,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE);
            restrictPermissions(destination, FILE_PERMISSIONS);
            return synchronizeMove(
                    installing,
                    destination,
                    "repair-quarantine installation");
        } catch (IOException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "repair quarantine could not be installed atomically",
                    exception);
        }
    }

    private byte[] readControlledRepairBytes(Path path)
            throws StoreIOException {
        requireRepairPath(path);
        FileIdentity before = identityOfControlledRegularFile(path);
        try (FileChannel channel = FileChannel.open(
                path,
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = readChannelBounded(channel);
            if (!before.equals(identityOfControlledRegularFile(path))) {
                throw failure(
                        Failure.FILE_IDENTITY_CHANGED,
                        "repair quarantine identity changed during read-back");
            }
            return bytes;
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "repair quarantine could not be read",
                    exception);
        }
    }

    private void requireRepairPath(Path path) {
        Path safe = Objects.requireNonNull(path, "path cannot be null")
                .toAbsolutePath()
                .normalize();
        if (!safe.getParent().equals(repairQuarantineDirectory)
                || !safe.startsWith(repairQuarantineDirectory)) {
            throw new IllegalArgumentException(
                    "repair quarantine path escaped its controlled directory");
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable", exception);
        }
    }

    private List<Path> listSegmentsLocked() throws StoreIOException {
        if (!Files.exists(segmentsDirectory, LinkOption.NOFOLLOW_LINKS)) {
            return List.of();
        }
        validateRotationDirectory();
        List<IndexedPath> indexed = new ArrayList<>();
        try (var entries = Files.newDirectoryStream(segmentsDirectory)) {
            for (Path path : entries) {
                String name = path.getFileName().toString();
                Matcher segment = SEGMENT_FILE_NAME.matcher(name);
                if (!segment.matches()) {
                    if (SEGMENT_INSTALLING_FILE_NAME.matcher(name).matches()) {
                        throw failure(
                                Failure.CONCURRENT_CHANGE,
                                "chained JSONL archive has an unresolved installing file");
                    }
                    throw failure(
                            Failure.UNSAFE_FILE,
                            "chained JSONL archive directory contains an unexpected entry");
                }
                if (indexed.size() >= HARD_MAX_ARCHIVED_SEGMENTS) {
                    throw failure(
                            Failure.ARCHIVE_LIMIT_EXCEEDED,
                            "chained JSONL exceeds its bounded archive count");
                }
                int index = Integer.parseInt(segment.group(1));
                indexed.add(new IndexedPath(index, path.toAbsolutePath().normalize()));
            }
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "chained JSONL archive directory could not be listed",
                    exception);
        }
        indexed.sort(Comparator.comparingInt(IndexedPath::index));
        for (int position = 0; position < indexed.size(); position++) {
            if (indexed.get(position).index() != position + 1) {
                throw failure(
                        Failure.CONCURRENT_CHANGE,
                        "chained JSONL archive sequence is not contiguous");
            }
        }
        return indexed.stream().map(IndexedPath::path).toList();
    }

    private List<Path> listInstallingFilesLocked() throws StoreIOException {
        if (!Files.exists(segmentsDirectory, LinkOption.NOFOLLOW_LINKS)) {
            return List.of();
        }
        validateRotationDirectory();
        List<Path> installing = new ArrayList<>();
        try (var entries = Files.newDirectoryStream(segmentsDirectory)) {
            for (Path path : entries) {
                if (SEGMENT_INSTALLING_FILE_NAME
                        .matcher(path.getFileName().toString())
                        .matches()) {
                    installing.add(path.toAbsolutePath().normalize());
                }
            }
        } catch (IOException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "chained JSONL installing files could not be listed",
                    exception);
        }
        installing.sort(Comparator.comparing(
                path -> path.getFileName().toString()));
        return installing;
    }

    private InstallingCleanup removeInstallingFilesLocked(Guard guard)
            throws StoreIOException {
        int removed = 0;
        EnterpriseLabDirectorySyncStatus directorySyncStatus =
                EnterpriseLabDirectorySyncStatus.NOT_REQUIRED_EXISTING_ENTRY;
        for (Path path : listInstallingFilesLocked()) {
            FileIdentity ignored = identityOfControlledRegularFile(path);
            try {
                guard.requireCurrent();
                if (Files.deleteIfExists(path)) {
                    removed++;
                    directorySyncStatus =
                            EnterpriseLabDirectorySyncStatus.combine(
                                    directorySyncStatus,
                                    synchronizeDirectory(
                                            segmentsDirectory,
                                            "orphan archive installing-file deletion"));
                }
            } catch (RuntimeException exception) {
                throw exception;
            } catch (IOException exception) {
                throw failure(
                        Failure.IO_FAILURE,
                        "orphan chained JSONL installing file could not be removed",
                        exception);
            }
        }
        return new InstallingCleanup(removed, directorySyncStatus);
    }

    private Path segmentPath(int index) {
        return controlledSegmentPath(String.format(
                java.util.Locale.ROOT,
                "segment-v1-%08d.jsonl",
                index));
    }

    private Path installingPath(int index) {
        return controlledSegmentPath(String.format(
                java.util.Locale.ROOT,
                "segment-v1-%08d.jsonl.installing",
                index));
    }

    private Path controlledSegmentPath(String name) {
        Path path = segmentsDirectory.resolve(name)
                .toAbsolutePath()
                .normalize();
        if (!path.getParent().equals(segmentsDirectory)
                || !path.startsWith(segmentsDirectory)) {
            throw new IllegalArgumentException(
                    "chained JSONL segment path escaped its controlled directory");
        }
        return path;
    }

    private void writeInstallingArchive(Path installing, byte[] bytes)
            throws StoreIOException {
        try (FileChannel channel = FileChannel.open(
                installing,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS)) {
            restrictPermissions(installing, FILE_PERMISSIONS);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            int zeroWrites = 0;
            while (buffer.hasRemaining()) {
                int written = channel.write(buffer);
                if (written == 0) {
                    if (++zeroWrites >= MAX_ZERO_PROGRESS) {
                        throw failure(
                                Failure.IO_FAILURE,
                                "bounded chained JSONL archive write made no progress");
                    }
                    continue;
                }
                zeroWrites = 0;
            }
            channel.force(true);
        } catch (StoreIOException exception) {
            throw exception;
        } catch (IOException | UnsupportedOperationException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    "chained JSONL installing archive could not be synchronized",
                    exception);
        }
    }

    private static <T> T decodeCanonicalFrame(
            byte[] encoded,
            FrameCodec<T> codec) throws StoreIOException {
        T entry;
        try {
            entry = codec.decode(encoded);
        } catch (RuntimeException exception) {
            throw failure(
                    Failure.INVALID_COMPLETE_FRAME,
                    "chained JSONL contains an invalid complete frame",
                    exception);
        }
        byte[] canonical;
        try {
            canonical = codec.encode(entry);
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
        return entry;
    }

    private static void restrictPermissions(
            Path path,
            Set<PosixFilePermission> permissions) throws IOException {
        PosixFileAttributeView view = Files.getFileAttributeView(
                path,
                PosixFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS);
        if (view != null) {
            Files.setPosixFilePermissions(path, permissions);
        }
    }

    private static ProcessMutexLease acquireProcessMutex(Path file) {
        synchronized (PROCESS_MUTEXES) {
            ProcessMutexEntry entry = PROCESS_MUTEXES.computeIfAbsent(
                    file, ignored -> new ProcessMutexEntry());
            entry.users++;
            return new ProcessMutexLease(file, entry);
        }
    }

    private EnterpriseLabDirectorySyncStatus synchronizeDirectory(
            Path directory,
            String operation) throws StoreIOException {
        try {
            return EnterpriseLabStorageDurability.synchronizeDirectory(
                    directory, directorySyncer);
        } catch (IOException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    operation + " parent directory could not be synchronized",
                    exception);
        }
    }

    private EnterpriseLabDirectorySyncStatus synchronizeMove(
            Path source,
            Path destination,
            String operation) throws StoreIOException {
        try {
            return EnterpriseLabStorageDurability.synchronizeMove(
                    source, destination, directorySyncer);
        } catch (IOException exception) {
            throw failure(
                    Failure.IO_FAILURE,
                    operation + " parent directory could not be synchronized",
                    exception);
        }
    }

    private void requireOpen() throws StoreIOException {
        if (closed) {
            throw failure(
                    Failure.IO_FAILURE,
                    "chained JSONL store is closed");
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
        LOGGER.warn(
                "Enterprise Lab chained JSONL storage failure [{}]: {}",
                failure,
                message);
        return new StoreIOException(failure, message);
    }

    private static StoreIOException failure(
            Failure failure,
            String message,
            Throwable cause) {
        LOGGER.error(
                "Enterprise Lab chained JSONL storage failure [{}]: {}; cause={}: {}",
                failure,
                message,
                cause.getClass().getSimpleName(),
                cause.getMessage());
        LOGGER.debug(
                "Enterprise Lab chained JSONL storage failure stack [{}]",
                failure,
                cause);
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
        ARCHIVE_LIMIT_EXCEEDED,
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

    record SegmentedChainReplay<T>(
            List<T> entries,
            long totalBytes,
            long currentCompleteBytes,
            long currentTailBytes,
            int currentEntries,
            int archiveCount,
            long currentBytes,
            StorageVersion version) {
        SegmentedChainReplay {
            entries = List.copyOf(Objects.requireNonNull(
                    entries, "entries cannot be null"));
            if (totalBytes < 0L
                    || currentCompleteBytes < 0L
                    || currentTailBytes < 0L
                    || currentEntries < 0
                    || archiveCount < 0
                    || archiveCount > HARD_MAX_ARCHIVED_SEGMENTS
                    || currentBytes < 0L
                    || currentCompleteBytes + currentTailBytes != currentBytes
                    || currentBytes > totalBytes) {
                throw new IllegalArgumentException(
                        "segmented chained JSONL replay counts are inconsistent");
            }
            version = Objects.requireNonNull(
                    version, "version cannot be null");
        }
    }

    record RotationReceipt(
            boolean rotated,
            int archiveIndex,
            long archivedBytes,
            EnterpriseLabDirectorySyncStatus directorySyncStatus) {
        RotationReceipt {
            if (rotated
                    ? archiveIndex < 1 || archivedBytes < 1L
                    : archiveIndex != 0 || archivedBytes != 0L) {
                throw new IllegalArgumentException(
                        "chained JSONL rotation receipt is inconsistent");
            }
            directorySyncStatus = Objects.requireNonNull(
                    directorySyncStatus,
                    "directorySyncStatus cannot be null");
        }

        static RotationReceipt notRotated(
                EnterpriseLabDirectorySyncStatus directorySyncStatus) {
            return new RotationReceipt(
                    false,
                    0,
                    0L,
                    directorySyncStatus);
        }
    }

    record RotationRecovery(
            int removedInstallingFiles,
            boolean completedPendingTruncate,
            EnterpriseLabDirectorySyncStatus directorySyncStatus) {
        RotationRecovery {
            if (removedInstallingFiles < 0) {
                throw new IllegalArgumentException(
                        "removed installing-file count cannot be negative");
            }
            directorySyncStatus = Objects.requireNonNull(
                    directorySyncStatus,
                    "directorySyncStatus cannot be null");
        }
    }

    record TailRepairReceipt(
            long removedTailBytes,
            String sourceFingerprint,
            String quarantineFileName,
            boolean exactTruncateVerified,
            EnterpriseLabDirectorySyncStatus directorySyncStatus) {
        TailRepairReceipt {
            if (removedTailBytes < 1L || !exactTruncateVerified) {
                throw new IllegalArgumentException(
                        "tail repair receipt requires a verified positive repair");
            }
            if (sourceFingerprint == null
                    || !sourceFingerprint.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException(
                        "tail repair source fingerprint must be canonical SHA-256");
            }
            if (quarantineFileName == null
                    || !quarantineFileName.equals(
                    "repair-v1-" + sourceFingerprint + ".jsonl")) {
                throw new IllegalArgumentException(
                        "tail repair quarantine filename is inconsistent");
            }
            directorySyncStatus = Objects.requireNonNull(
                    directorySyncStatus,
                    "directorySyncStatus cannot be null");
        }
    }

    private record SegmentedBytes(
            List<SegmentBytes> segments,
            int archiveCount,
            long currentBytes,
            StorageVersion version) {
        private SegmentedBytes {
            segments = List.copyOf(Objects.requireNonNull(
                    segments, "segments cannot be null"));
            if (archiveCount < 0
                    || archiveCount > HARD_MAX_ARCHIVED_SEGMENTS
                    || currentBytes < 0L
                    || (segments.isEmpty()
                    ? archiveCount != 0 || currentBytes != 0L
                    : archiveCount != segments.size() - 1
                    || !segments.get(segments.size() - 1).current()
                    || segments.get(segments.size() - 1).bytes().length
                    != currentBytes)) {
                throw new IllegalArgumentException(
                        "segmented chained JSONL source is inconsistent");
            }
            version = Objects.requireNonNull(
                    version, "version cannot be null");
        }

        static SegmentedBytes empty() {
            return new SegmentedBytes(
                    List.of(),
                    0,
                    0L,
                    StorageVersion.missing());
        }
    }

    private record SegmentBytes(
            String name,
            byte[] bytes,
            boolean current) {
        private SegmentBytes {
            name = Objects.requireNonNull(name, "name cannot be null");
            bytes = Objects.requireNonNull(bytes, "bytes cannot be null").clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }

    private record IndexedPath(int index, Path path) {
        private IndexedPath {
            if (index < 1 || index > HARD_MAX_ARCHIVED_SEGMENTS) {
                throw new IllegalArgumentException(
                        "chained JSONL archive index is outside its bound");
            }
            path = Objects.requireNonNull(path, "path cannot be null");
        }
    }

    private record InstallingCleanup(
            int removedFiles,
            EnterpriseLabDirectorySyncStatus directorySyncStatus) {
        private InstallingCleanup {
            if (removedFiles < 0) {
                throw new IllegalArgumentException(
                        "removed installing-file count cannot be negative");
            }
            directorySyncStatus = Objects.requireNonNull(
                    directorySyncStatus,
                    "directorySyncStatus cannot be null");
        }
    }

    private static final class ProcessMutexEntry {
        private final Object mutex = new Object();
        private int users;

        private Object mutex() {
            return mutex;
        }
    }

    private static final class ProcessMutexLease implements Runnable {
        private final Path file;
        private final ProcessMutexEntry entry;

        private ProcessMutexLease(Path file, ProcessMutexEntry entry) {
            this.file = file;
            this.entry = entry;
        }

        private ProcessMutexEntry entry() {
            return entry;
        }

        @Override
        public void run() {
            synchronized (PROCESS_MUTEXES) {
                if (entry.users < 1) {
                    return;
                }
                entry.users--;
                if (entry.users == 0) {
                    PROCESS_MUTEXES.remove(file, entry);
                }
            }
        }
    }

    record StorageVersion(
            boolean present,
            List<FileVersion> archives,
            FileVersion current) {
        StorageVersion {
            archives = List.copyOf(Objects.requireNonNull(
                    archives, "archives cannot be null"));
            if (present != (current != null)
                    || !present && !archives.isEmpty()
                    || archives.size() > HARD_MAX_ARCHIVED_SEGMENTS) {
                throw new IllegalArgumentException(
                        "chained JSONL storage version is inconsistent");
            }
        }

        static StorageVersion missing() {
            return new StorageVersion(false, List.of(), null);
        }
    }

    record FileVersion(
            String name,
            FileIdentity identity,
            long size,
            FileTime lastModifiedTime) {
        FileVersion {
            name = Objects.requireNonNull(name, "name cannot be null");
            identity = Objects.requireNonNull(
                    identity, "identity cannot be null");
            lastModifiedTime = Objects.requireNonNull(
                    lastModifiedTime, "lastModifiedTime cannot be null");
            if (size < 0L) {
                throw new IllegalArgumentException(
                        "chained JSONL file size cannot be negative");
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
