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
import java.nio.file.StandardCopyOption;
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
import java.util.Comparator;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    public static final int HARD_MAX_DISCOVERED_JOURNALS = 256;

    static final String NAMESPACE = "enterprise-lab-experiment-journals-v1";
    private static final String JOURNALS = "journals";
    private static final String QUARANTINE = "quarantine";
    private static final String COMPACTED = "compacted";
    private static final Pattern JOURNAL_FILE_NAME =
            Pattern.compile("journal-v1-[0-9a-f]{64}\\.jsonl");
    private static final Pattern MANIFEST_FILE_NAME =
            Pattern.compile("terminal-v1-[0-9a-f]{64}\\.json");
    private static final Pattern CANONICAL_EXPERIMENT_ID = Pattern.compile("[A-Za-z0-9._:-]+");
    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_PERMISSIONS =
            PosixFilePermissions.fromString("rw-------");
    private static final Map<Path, Object> ACTIVE_WRITERS = new ConcurrentHashMap<>();
    private static final FailureInjector NO_FAILURE = (checkpoint, bytesWritten) -> { };

    private final Path journalsDirectory;
    private final Path quarantineDirectory;
    private final Path compactedDirectory;
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
        this.quarantineDirectory = controlledDirectory(namespace, QUARANTINE);
        this.compactedDirectory = controlledDirectory(namespace, COMPACTED);
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

    /** Verifies and deterministically reconstructs one closed controlled journal. */
    public EnterpriseLabExperimentJournalReplayEngine.ReplayResult replay(String experimentId) {
        EnterpriseLabExperimentJournalReplayEngine.ReplayLimits replayLimits =
                new EnterpriseLabExperimentJournalReplayEngine.ReplayLimits(
                        maxJournalEntries,
                        maxJournalBytes,
                        EnterpriseLabExperimentJournalReplayEngine.HARD_MAX_OPERATIONS);
        return new EnterpriseLabExperimentJournalReplayEngine(replayLimits).replay(verify(experimentId));
    }

    /**
     * Discovers the bounded controlled journal namespace without accepting caller paths.
     * The experiment identity is recovered only from a canonical first frame whose hash
     * matches the controlled filename.
     */
    public List<JournalDiscovery> discover() {
        List<Path> paths = new ArrayList<>();
        try (var entries = Files.newDirectoryStream(journalsDirectory)) {
            for (Path path : entries) {
                if (paths.size() >= HARD_MAX_DISCOVERED_JOURNALS) {
                    throw failure(Failure.DISCOVERY_LIMIT_EXCEEDED,
                            "journal discovery exceeds the bounded journal count");
                }
                paths.add(path);
            }
        } catch (EnterpriseLabExperimentJournalStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE, "controlled journal discovery failed", exception);
        }
        paths.sort(Comparator.comparing(path -> path.getFileName().toString()));
        List<JournalDiscovery> discoveries = new ArrayList<>(paths.size());
        for (Path path : paths) {
            discoveries.add(discover(path));
        }
        return List.copyOf(discoveries);
    }

    /**
     * Installs a verified, canonical terminal manifest before removing the exact
     * controlled source journal. Active, invalid, or non-terminal journals are preserved.
     */
    public CompactionResult compactTerminal(String experimentId, Clock clock, String reasonCode) {
        String safeExperimentId = requireExperimentId(experimentId);
        Instant compactedAt = Objects.requireNonNull(clock, "clock cannot be null").instant();
        String safeReasonCode = requireReasonCode(reasonCode);
        String journalId = journalId(safeExperimentId);
        Path source = journalPath(journalId);
        Object owner = claim(source);
        try {
            if (!Files.exists(source, LinkOption.NOFOLLOW_LINKS)) {
                Path existingPath = manifestPath(
                        "terminal-v1-" + journalId.substring("journal-v1-".length()));
                if (Files.exists(existingPath, LinkOption.NOFOLLOW_LINKS)) {
                    EnterpriseLabExperimentTerminalManifest existing = readManifest(existingPath);
                    if (!existing.experimentId().equals(safeExperimentId)
                            || !existing.journalId().equals(journalId)) {
                        throw failure(Failure.IDENTITY_MISMATCH,
                                "existing terminal manifest identity does not match request");
                    }
                    return new CompactionResult(
                            CompactionOutcome.COMPLETED_IDEMPOTENTLY,
                            existing, true, "TERMINAL_MANIFEST_REUSED");
                }
                throw failure(Failure.VERIFICATION_FAILED,
                        "no source journal or verified terminal manifest exists");
            }
            VerificationResult verification = verifyOwned(source, journalId, safeExperimentId);
            if (verification.outcome() != Outcome.VALID) {
                throw failure(Failure.VERIFICATION_FAILED,
                        "only an exactly valid terminal journal can be compacted");
            }
            var replay = new EnterpriseLabExperimentJournalReplayEngine().replay(verification);
            if (replay.outcome() != EnterpriseLabExperimentJournalReplayEngine.Outcome.RECONSTRUCTED
                    || replay.reconstructedState().orElseThrow().terminalRecord().isEmpty()) {
                throw failure(Failure.VERIFICATION_FAILED,
                        "active or semantically invalid journals cannot be compacted");
            }
            EnterpriseLabExperimentTerminalManifest manifest =
                    EnterpriseLabExperimentTerminalManifest.create(
                            verification, replay.reconstructedState().orElseThrow(),
                            compactedAt, safeReasonCode);
            Path destination = manifestPath(manifest.manifestId());
            boolean newlyInstalled = !Files.exists(destination, LinkOption.NOFOLLOW_LINKS)
                    && installManifest(destination, manifest);
            EnterpriseLabExperimentTerminalManifest installed = readManifest(destination);
            if (newlyInstalled ? !installed.equals(manifest) : !sameSourceEvidence(installed, manifest)) {
                throw failure(Failure.VERIFICATION_FAILED,
                        "installed terminal manifest does not match verified source evidence");
            }
            validateJournalFile(source);
            try {
                Files.delete(source);
            } catch (IOException exception) {
                throw failure(Failure.IO_FAILURE,
                        "verified terminal manifest was installed but source cleanup failed safely", exception);
            }
            return new CompactionResult(
                    newlyInstalled ? CompactionOutcome.COMPACTED : CompactionOutcome.COMPLETED_IDEMPOTENTLY,
                    installed, true,
                    newlyInstalled ? "TERMINAL_MANIFEST_INSTALLED" : "TERMINAL_MANIFEST_REUSED");
        } finally {
            ACTIVE_WRITERS.remove(source, owner);
        }
    }

    /** Lists verified canonical compacted manifests; no backing paths are exposed. */
    public List<EnterpriseLabExperimentTerminalManifest> compactedManifests() {
        List<Path> paths = boundedControlledEntries(compactedDirectory, "compacted manifest");
        List<EnterpriseLabExperimentTerminalManifest> manifests = new ArrayList<>();
        for (Path path : paths) {
            if (!MANIFEST_FILE_NAME.matcher(path.getFileName().toString()).matches()) {
                throw failure(Failure.VERIFICATION_FAILED,
                        "unrecognized content exists in the compacted evidence namespace");
            }
            manifests.add(readManifest(path));
        }
        manifests.sort(Comparator.comparing(
                EnterpriseLabExperimentTerminalManifest::terminalOccurredAt)
                .thenComparing(EnterpriseLabExperimentTerminalManifest::manifestId));
        return List.copyOf(manifests);
    }

    /** Sanitized quarantine inventory. Original corrupt bytes remain untouched. */
    public List<QuarantineMetadata> quarantineMetadata() {
        List<Path> paths = boundedControlledEntries(quarantineDirectory, "quarantine entry");
        List<QuarantineMetadata> results = new ArrayList<>();
        for (Path path : paths) {
            try {
                validateJournalFile(path);
                results.add(new QuarantineMetadata(
                        opaqueJournalId(path.getFileName().toString()),
                        Files.size(path),
                        Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toInstant(),
                        "FORENSIC_BYTES_RETAINED"));
            } catch (IOException exception) {
                throw failure(Failure.IO_FAILURE, "quarantine metadata could not be inspected", exception);
            }
        }
        return List.copyOf(results);
    }

    /** Plans or applies bounded compaction of the oldest exactly valid terminal journals. */
    public RetentionReport enforceRetention(RetentionPolicy policy, boolean dryRun, Clock clock) {
        RetentionPolicy safePolicy = Objects.requireNonNull(policy, "policy cannot be null");
        Clock safeClock = Objects.requireNonNull(clock, "clock cannot be null");
        List<TerminalCandidate> terminal = new ArrayList<>();
        int unresolved = 0;
        for (JournalDiscovery discovery : discover()) {
            if (discovery.outcome() != DiscoveryOutcome.VERIFIED
                    || discovery.verification().orElseThrow().outcome() != Outcome.VALID) {
                unresolved++;
                continue;
            }
            var replay = new EnterpriseLabExperimentJournalReplayEngine()
                    .replay(discovery.verification().orElseThrow());
            if (replay.outcome() != EnterpriseLabExperimentJournalReplayEngine.Outcome.RECONSTRUCTED
                    || replay.reconstructedState().orElseThrow().terminalRecord().isEmpty()) {
                continue;
            }
            var state = replay.reconstructedState().orElseThrow();
            terminal.add(new TerminalCandidate(
                    state.experimentId(),
                    state.terminalRecord().orElseThrow().completedAt(),
                    discovery.verification().orElseThrow().totalBytes()));
        }
        terminal.sort(Comparator.comparing(TerminalCandidate::terminalAt)
                .thenComparing(TerminalCandidate::experimentId));
        int compactCount = Math.max(0, terminal.size() - safePolicy.maximumTerminalJournals());
        List<RetentionAction> actions = new ArrayList<>();
        for (int index = 0; index < compactCount; index++) {
            TerminalCandidate candidate = terminal.get(index);
            if (dryRun) {
                actions.add(new RetentionAction(
                        candidate.experimentId(), candidate.sourceBytes(), false,
                        "TERMINAL_COMPACTION_PLANNED"));
            } else {
                CompactionResult result = compactTerminal(
                        candidate.experimentId(), safeClock, "TERMINAL_RETENTION_LIMIT");
                actions.add(new RetentionAction(
                        candidate.experimentId(), candidate.sourceBytes(),
                        result.sourceRemoved(), result.reasonCode()));
            }
        }
        return new RetentionReport(
                RetentionReport.SCHEMA_VERSION, dryRun, safeClock.instant(),
                safePolicy.maximumTerminalJournals(), terminal.size(), unresolved,
                quarantineMetadata().size(), actions);
    }

    /** Moves one discovered invalid journal into the controlled forensic quarantine namespace. */
    QuarantineRecord quarantine(JournalDiscovery discovery, java.time.Clock clock, String reasonCode) {
        JournalDiscovery safeDiscovery = Objects.requireNonNull(discovery, "discovery cannot be null");
        java.time.Instant quarantinedAt = Objects.requireNonNull(clock, "clock cannot be null").instant();
        String safeReason = requireReasonCode(reasonCode);
        if (!safeDiscovery.journalId().matches("journal-v1-[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "only a recognized controlled journal file can be quarantined");
        }
        Path source = journalPath(safeDiscovery.journalId());
        Object owner = claim(source);
        try {
            validateJournalFile(source);
            String quarantineId = safeDiscovery.journalId() + "-"
                    + quarantinedAt.toEpochMilli() + "-" + quarantinedAt.getNano();
            Path destination = quarantinePath(quarantineId);
            try {
                restrictPermissions(source, FILE_PERMISSIONS);
                Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                throw failure(Failure.QUARANTINE_FAILED,
                        "journal could not be atomically preserved in quarantine", exception);
            }
            return new QuarantineRecord(
                    quarantineId,
                    safeDiscovery.journalId(),
                    safeDiscovery.experimentId(),
                    safeDiscovery.classification(),
                    safeReason,
                    quarantinedAt,
                    true);
        } finally {
            ACTIVE_WRITERS.remove(source, owner);
        }
    }

    private JournalDiscovery discover(Path path) {
        String fileName = path.getFileName().toString();
        if (!JOURNAL_FILE_NAME.matcher(fileName).matches()) {
            return JournalDiscovery.unrecognized(opaqueJournalId(fileName));
        }
        String discoveredJournalId = fileName.substring(0, fileName.length() - ".jsonl".length());
        Object owner;
        try {
            owner = claim(path);
        } catch (EnterpriseLabExperimentJournalStorageException exception) {
            return JournalDiscovery.unavailable(discoveredJournalId);
        }
        try {
            validateJournalFile(path);
            Optional<EnterpriseLabExperimentJournalEvent> first = readCanonicalFirstFrame(path);
            if (first.isEmpty()) {
                return JournalDiscovery.unidentified(discoveredJournalId);
            }
            EnterpriseLabExperimentJournalEvent event = first.orElseThrow();
            if (event.sequence() != 1
                    || !event.previousEntryFingerprint().equals(
                            EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT)
                    || !journalId(event.experimentId()).equals(discoveredJournalId)) {
                return JournalDiscovery.identityMismatch(discoveredJournalId);
            }
            VerificationResult verification = verifyOwned(path, discoveredJournalId, event.experimentId());
            return JournalDiscovery.verified(discoveredJournalId, event.experimentId(), verification);
        } catch (EnterpriseLabExperimentJournalStorageException exception) {
            return JournalDiscovery.storageFailure(discoveredJournalId, exception.failure().name());
        } finally {
            ACTIVE_WRITERS.remove(path, owner);
        }
    }

    private Optional<EnterpriseLabExperimentJournalEvent> readCanonicalFirstFrame(Path path) {
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            if (channel.size() > maxJournalBytes) {
                return Optional.empty();
            }
            ByteBuffer one = ByteBuffer.allocate(1);
            int zeroReads = 0;
            while (frame.size() <= EnterpriseLabExperimentJournalCodec.HARD_MAX_ENTRY_BYTES) {
                int read = channel.read(one);
                if (read < 0) {
                    return Optional.empty();
                }
                if (read == 0) {
                    if (++zeroReads >= 3) {
                        return Optional.empty();
                    }
                    continue;
                }
                zeroReads = 0;
                one.flip();
                byte value = one.get();
                one.clear();
                if (value == '\n') {
                    if (frame.size() == 0) {
                        return Optional.empty();
                    }
                    byte[] encoded = frame.toByteArray();
                    EnterpriseLabExperimentJournalEvent event = codec.decode(encoded);
                    return Arrays.equals(encoded, codec.encode(event))
                            ? Optional.of(event)
                            : Optional.empty();
                }
                frame.write(value);
            }
            return Optional.empty();
        } catch (IOException | RuntimeException exception) {
            return Optional.empty();
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

    private Path quarantinePath(String quarantineId) {
        Path candidate = quarantineDirectory.resolve(quarantineId + ".quarantined").normalize();
        if (!candidate.startsWith(quarantineDirectory)
                || !candidate.getParent().equals(quarantineDirectory)) {
            throw failure(Failure.UNSAFE_PATH, "quarantine path escaped its controlled namespace");
        }
        return candidate;
    }

    private Path manifestPath(String manifestId) {
        if (manifestId == null || !manifestId.matches("terminal-v1-[0-9a-f]{64}")) {
            throw new IllegalArgumentException("manifestId must be a controlled terminal identifier");
        }
        Path candidate = compactedDirectory.resolve(manifestId + ".json").normalize();
        if (!candidate.startsWith(compactedDirectory)
                || !candidate.getParent().equals(compactedDirectory)) {
            throw failure(Failure.UNSAFE_PATH, "compacted path escaped its controlled namespace");
        }
        return candidate;
    }

    private boolean installManifest(
            Path destination,
            EnterpriseLabExperimentTerminalManifest manifest) {
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        Path temporary = destination.resolveSibling(destination.getFileName() + ".installing").normalize();
        if (!temporary.startsWith(compactedDirectory)
                || !temporary.getParent().equals(compactedDirectory)) {
            throw failure(Failure.UNSAFE_PATH, "manifest temporary path escaped its controlled namespace");
        }
        byte[] bytes = manifest.encode();
        try (FileChannel channel = FileChannel.open(
                temporary,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                LinkOption.NOFOLLOW_LINKS)) {
            restrictPermissions(temporary, FILE_PERMISSIONS);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE, "terminal manifest could not be synchronized", exception);
        }
        try {
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            restrictPermissions(destination, FILE_PERMISSIONS);
            return true;
        } catch (FileAlreadyExistsException exception) {
            return false;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE,
                    "atomic terminal manifest installation is unavailable; source was preserved", exception);
        }
    }

    private EnterpriseLabExperimentTerminalManifest readManifest(Path path) {
        validateJournalFile(path);
        try {
            long size = Files.size(path);
            if (size < 1 || size > EnterpriseLabExperimentTerminalManifest.HARD_MAX_MANIFEST_BYTES) {
                throw failure(Failure.VERIFICATION_FAILED,
                        "terminal manifest is outside bounded size");
            }
            return EnterpriseLabExperimentTerminalManifest.decode(Files.readAllBytes(path));
        } catch (EnterpriseLabExperimentJournalStorageException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw failure(Failure.VERIFICATION_FAILED,
                    "terminal manifest failed canonical fingerprint verification", exception);
        }
    }

    private static boolean sameSourceEvidence(
            EnterpriseLabExperimentTerminalManifest left,
            EnterpriseLabExperimentTerminalManifest right) {
        return left.manifestId().equals(right.manifestId())
                && left.journalId().equals(right.journalId())
                && left.experimentId().equals(right.experimentId())
                && left.scenarioId().equals(right.scenarioId())
                && left.configurationFingerprint().equals(right.configurationFingerprint())
                && left.decisionFingerprint().equals(right.decisionFingerprint())
                && left.baselineAllocationFingerprint().equals(right.baselineAllocationFingerprint())
                && left.candidateAllocationFingerprint().equals(right.candidateAllocationFingerprint())
                && left.appliedAllocationFingerprint().equals(right.appliedAllocationFingerprint())
                && left.terminalState() == right.terminalState()
                && left.rollbackStatus() == right.rollbackStatus()
                && left.restorationStatus() == right.restorationStatus()
                && left.sourceEventCount() == right.sourceEventCount()
                && left.sourceByteCount() == right.sourceByteCount()
                && left.sourceTerminalSequence() == right.sourceTerminalSequence()
                && left.sourceTerminalFingerprint().equals(right.sourceTerminalFingerprint())
                && left.reconstructedStateFingerprint().equals(right.reconstructedStateFingerprint())
                && left.terminalOccurredAt().equals(right.terminalOccurredAt());
    }

    private List<Path> boundedControlledEntries(Path directory, String contentName) {
        List<Path> paths = new ArrayList<>();
        try (var entries = Files.newDirectoryStream(directory)) {
            for (Path path : entries) {
                if (paths.size() >= HARD_MAX_DISCOVERED_JOURNALS) {
                    throw failure(Failure.DISCOVERY_LIMIT_EXCEEDED,
                            contentName + " discovery exceeds the bounded count");
                }
                paths.add(path);
            }
        } catch (EnterpriseLabExperimentJournalStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(Failure.IO_FAILURE, contentName + " discovery failed", exception);
        }
        paths.sort(Comparator.comparing(path -> path.getFileName().toString()));
        return paths;
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

    private static String opaqueJournalId(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "unrecognized-v1-" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String requireReasonCode(String value) {
        if (value == null || !value.matches("[A-Z0-9][A-Z0-9_.:-]{0,63}")) {
            throw new IllegalArgumentException("reasonCode must be a bounded canonical code");
        }
        return value;
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

    public enum DiscoveryOutcome {
        VERIFIED,
        UNIDENTIFIED_CONTENT,
        IDENTITY_MISMATCH,
        UNRECOGNIZED_NAMESPACE_ENTRY,
        UNAVAILABLE,
        STORAGE_FAILURE
    }

    public enum CompactionOutcome {
        COMPACTED,
        COMPLETED_IDEMPOTENTLY
    }

    public record CompactionResult(
            CompactionOutcome outcome,
            EnterpriseLabExperimentTerminalManifest manifest,
            boolean sourceRemoved,
            String reasonCode) {
        public CompactionResult {
            outcome = Objects.requireNonNull(outcome, "outcome cannot be null");
            manifest = Objects.requireNonNull(manifest, "manifest cannot be null");
            reasonCode = requireReasonCode(reasonCode);
            if (!sourceRemoved) {
                throw new IllegalArgumentException("successful compaction must remove the verified source");
            }
        }
    }

    public record RetentionPolicy(int maximumTerminalJournals) {
        public RetentionPolicy {
            if (maximumTerminalJournals < 0
                    || maximumTerminalJournals > HARD_MAX_DISCOVERED_JOURNALS) {
                throw new IllegalArgumentException("maximumTerminalJournals is outside bounded retention limits");
            }
        }
    }

    public record RetentionAction(
            String experimentId,
            long sourceBytes,
            boolean sourceRemoved,
            String reasonCode) {
        public RetentionAction {
            experimentId = requireExperimentId(experimentId);
            if (sourceBytes < 1 || sourceBytes > HARD_MAX_JOURNAL_BYTES) {
                throw new IllegalArgumentException("retention sourceBytes is outside journal bounds");
            }
            reasonCode = requireReasonCode(reasonCode);
        }
    }

    public record RetentionReport(
            String schemaVersion,
            boolean dryRun,
            Instant evaluatedAt,
            int maximumTerminalJournals,
            int terminalJournalsObserved,
            int unresolvedJournalsRetained,
            int quarantineEntriesRetained,
            List<RetentionAction> actions) {
        public static final String SCHEMA_VERSION = "enterprise-lab-retention-report/v1";

        public RetentionReport {
            if (!SCHEMA_VERSION.equals(schemaVersion)) {
                throw new IllegalArgumentException("unsupported retention report schemaVersion");
            }
            evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt cannot be null");
            actions = List.copyOf(Objects.requireNonNull(actions, "actions cannot be null"));
            if (maximumTerminalJournals < 0
                    || maximumTerminalJournals > HARD_MAX_DISCOVERED_JOURNALS
                    || terminalJournalsObserved < 0
                    || terminalJournalsObserved > HARD_MAX_DISCOVERED_JOURNALS
                    || unresolvedJournalsRetained < 0
                    || unresolvedJournalsRetained > HARD_MAX_DISCOVERED_JOURNALS
                    || quarantineEntriesRetained < 0
                    || quarantineEntriesRetained > HARD_MAX_DISCOVERED_JOURNALS
                    || actions.size() > HARD_MAX_DISCOVERED_JOURNALS) {
                throw new IllegalArgumentException("retention report is outside bounded limits");
            }
        }
    }

    public record QuarantineMetadata(
            String quarantineId,
            long retainedBytes,
            Instant lastModifiedAt,
            String status) {
        public QuarantineMetadata {
            quarantineId = requireBoundedDiscoveryText(quarantineId, "quarantineId", 96);
            if (retainedBytes < 0 || retainedBytes > HARD_MAX_JOURNAL_BYTES) {
                throw new IllegalArgumentException("quarantine retainedBytes is outside bounds");
            }
            lastModifiedAt = Objects.requireNonNull(lastModifiedAt, "lastModifiedAt cannot be null");
            status = requireBoundedDiscoveryText(status, "status", 64);
        }
    }

    private record TerminalCandidate(String experimentId, Instant terminalAt, long sourceBytes) {
    }

    /** Sanitized discovery result; no backing path or raw malformed content is exposed. */
    public record JournalDiscovery(
            String journalId,
            Optional<String> experimentId,
            DiscoveryOutcome outcome,
            String classification,
            Optional<VerificationResult> verification) {
        public JournalDiscovery {
            journalId = requireBoundedDiscoveryText(journalId, "journalId", 96);
            experimentId = Objects.requireNonNull(experimentId, "experimentId cannot be null");
            outcome = Objects.requireNonNull(outcome, "outcome cannot be null");
            classification = requireBoundedDiscoveryText(classification, "classification", 64);
            verification = Objects.requireNonNull(verification, "verification cannot be null");
            if ((outcome == DiscoveryOutcome.VERIFIED) != experimentId.isPresent()
                    || (outcome == DiscoveryOutcome.VERIFIED) != verification.isPresent()) {
                throw new IllegalArgumentException("verified discovery requires identity and verification evidence");
            }
            if (verification.isPresent()
                    && !journalId.equals(verification.orElseThrow().journalId())) {
                throw new IllegalArgumentException("discovery and verification journal identities differ");
            }
        }

        private static JournalDiscovery verified(
                String journalId,
                String experimentId,
                VerificationResult verification) {
            return new JournalDiscovery(
                    journalId,
                    Optional.of(experimentId),
                    DiscoveryOutcome.VERIFIED,
                    verification.classification().name(),
                    Optional.of(verification));
        }

        private static JournalDiscovery unidentified(String journalId) {
            return rejected(journalId, DiscoveryOutcome.UNIDENTIFIED_CONTENT,
                    "UNIDENTIFIED_CONTENT");
        }

        private static JournalDiscovery identityMismatch(String journalId) {
            return rejected(journalId, DiscoveryOutcome.IDENTITY_MISMATCH, "IDENTITY_MISMATCH");
        }

        private static JournalDiscovery unrecognized(String journalId) {
            return rejected(journalId, DiscoveryOutcome.UNRECOGNIZED_NAMESPACE_ENTRY,
                    "UNRECOGNIZED_NAMESPACE_ENTRY");
        }

        private static JournalDiscovery unavailable(String journalId) {
            return rejected(journalId, DiscoveryOutcome.UNAVAILABLE, "WRITER_ALREADY_ACTIVE");
        }

        private static JournalDiscovery storageFailure(String journalId, String classification) {
            return rejected(journalId, DiscoveryOutcome.STORAGE_FAILURE, classification);
        }

        private static JournalDiscovery rejected(
                String journalId,
                DiscoveryOutcome outcome,
                String classification) {
            return new JournalDiscovery(
                    journalId, Optional.empty(), outcome, classification, Optional.empty());
        }
    }

    /** Evidence that the original bytes were atomically moved, never deleted or rewritten. */
    public record QuarantineRecord(
            String quarantineId,
            String sourceJournalId,
            Optional<String> experimentId,
            String sourceClassification,
            String reasonCode,
            java.time.Instant quarantinedAt,
            boolean originalBytesPreserved) {
        public QuarantineRecord {
            quarantineId = requireBoundedDiscoveryText(quarantineId, "quarantineId", 160);
            sourceJournalId = requireBoundedDiscoveryText(sourceJournalId, "sourceJournalId", 96);
            experimentId = Objects.requireNonNull(experimentId, "experimentId cannot be null");
            sourceClassification = requireBoundedDiscoveryText(
                    sourceClassification, "sourceClassification", 64);
            reasonCode = requireReasonCode(reasonCode);
            quarantinedAt = Objects.requireNonNull(quarantinedAt, "quarantinedAt cannot be null");
            if (!originalBytesPreserved) {
                throw new IllegalArgumentException("quarantine must preserve the original bytes");
            }
        }
    }

    private static String requireBoundedDiscoveryText(
            String value,
            String fieldName,
            int maximumLength) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " must be trimmed and bounded");
        }
        return value;
    }
}
