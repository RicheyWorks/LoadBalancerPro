package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournal.AppendReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournal.PersistenceStage;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournal.ReadResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournal.SyncPolicy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalDirectory.FailureInjector;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalDirectory.WriteCheckpoint;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalStorageException.Failure;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalVerifier.Classification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalVerifier.Outcome;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalVerifier.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceMutationAuthority.MutationAuthorization;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Package-private controlled-directory writer; callers use the journal interface. */
final class EnterpriseLabExperimentLocalJournal implements EnterpriseLabExperimentJournal {
    private final EnterpriseLabExperimentJournalDirectory directory;
    private final String journalId;
    private final String experimentId;
    private final Path journalPath;
    private final ChainedJsonlStore jsonlStore;
    private final SyncPolicy syncPolicy;
    private final EnterpriseLabExperimentJournalCodec codec;
    private final MutationAuthorization mutationAuthorization;
    private final FailureInjector failureInjector;
    private final Runnable releaseOwnership;

    private long nextSequence;
    private String previousFingerprint;
    private long totalBytes;
    private int entryCount;
    private EnterpriseLabExperimentJournalEvent lastEvent;
    private boolean failed;
    private boolean closed;
    private boolean ownershipReleased;

    EnterpriseLabExperimentLocalJournal(
            EnterpriseLabExperimentJournalDirectory directory,
            String journalId,
            String experimentId,
            Path journalPath,
            ChainedJsonlStore jsonlStore,
            SyncPolicy syncPolicy,
            EnterpriseLabExperimentJournalCodec codec,
            ReadResult existing,
            MutationAuthorization mutationAuthorization,
            FailureInjector failureInjector,
            Runnable releaseOwnership) {
        this.directory = Objects.requireNonNull(directory, "directory cannot be null");
        this.journalId = Objects.requireNonNull(journalId, "journalId cannot be null");
        this.experimentId = Objects.requireNonNull(experimentId, "experimentId cannot be null");
        this.journalPath = Objects.requireNonNull(journalPath, "journalPath cannot be null");
        this.jsonlStore = Objects.requireNonNull(jsonlStore, "jsonlStore cannot be null");
        this.syncPolicy = Objects.requireNonNull(syncPolicy, "syncPolicy cannot be null");
        this.codec = Objects.requireNonNull(codec, "codec cannot be null");
        this.mutationAuthorization = Objects.requireNonNull(
                mutationAuthorization, "mutationAuthorization cannot be null");
        this.failureInjector = Objects.requireNonNull(failureInjector, "failureInjector cannot be null");
        this.releaseOwnership = Objects.requireNonNull(releaseOwnership, "releaseOwnership cannot be null");
        this.entryCount = existing.events().size();
        this.lastEvent = entryCount == 0 ? null : existing.events().get(entryCount - 1);
        this.nextSequence = entryCount + 1L;
        this.previousFingerprint = entryCount == 0
                ? EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT
                : existing.events().get(entryCount - 1).currentEntryFingerprint();
        this.totalBytes = existing.totalBytes();
    }

    @Override
    public synchronized AppendReceipt append(EnterpriseLabExperimentJournalEvent event) {
        ensureOpen();
        try {
            directory.requireSameMutationAuthorization(mutationAuthorization);
            validateAppend(event);
            byte[] encoded = codec.encode(event);
            if (encoded.length > EnterpriseLabExperimentJournalCodec.HARD_MAX_ENTRY_BYTES) {
                throw EnterpriseLabExperimentJournalDirectory.failure(
                        Failure.ENTRY_LIMIT_EXCEEDED, "journal entry exceeds the bounded frame size");
            }
            long frameBytes = encoded.length + 1L;
            if (entryCount >= directory.maxJournalEntries()) {
                throw EnterpriseLabExperimentJournalDirectory.failure(
                        Failure.ENTRY_LIMIT_EXCEEDED, "journal has reached its bounded entry count");
            }
            if (totalBytes + frameBytes > directory.maxJournalBytes()) {
                throw EnterpriseLabExperimentJournalDirectory.failure(
                        Failure.JOURNAL_SIZE_EXCEEDED, "journal has reached its bounded local size limit");
            }
            failureInjector.checkpoint(WriteCheckpoint.BEFORE_APPEND, 0);
            directory.requireSameMutationAuthorization(mutationAuthorization);
            appendFrame(encoded);

            PersistenceStage stage;
            boolean forceCompleted;
            if (syncPolicy == SyncPolicy.FORCE_DATA) {
                stage = PersistenceStage.DATA_FORCE_COMPLETE;
                forceCompleted = true;
            } else if (syncPolicy == SyncPolicy.FORCE_DATA_AND_METADATA) {
                stage = PersistenceStage.DATA_AND_METADATA_FORCE_COMPLETE;
                forceCompleted = true;
            } else {
                stage = PersistenceStage.OPERATING_SYSTEM_WRITE_COMPLETE;
                forceCompleted = false;
            }

            entryCount++;
            nextSequence++;
            previousFingerprint = event.currentEntryFingerprint();
            lastEvent = event;
            totalBytes += frameBytes;
            return new AppendReceipt(
                    journalId,
                    event.sequence(),
                    event.currentEntryFingerprint(),
                    syncPolicy,
                    stage,
                    false,
                    true,
                    forceCompleted);
        } catch (EnterpriseLabExperimentJournalStorageException exception) {
            failAndClose();
            throw exception;
        } catch (IOException exception) {
            failAndClose();
            throw EnterpriseLabExperimentJournalDirectory.failure(
                    Failure.IO_FAILURE, "journal append did not complete", exception);
        } catch (RuntimeException exception) {
            failAndClose();
            throw exception;
        }
    }

    @Override
    public synchronized ReadResult read() {
        ensureOpen();
        try {
            return directory.scanOwned(journalPath, journalId, experimentId);
        } catch (RuntimeException exception) {
            failAndClose();
            throw exception;
        }
    }

    @Override
    public synchronized VerificationResult verify() {
        ensureOpen();
        VerificationResult result = directory.verifyOwned(journalPath, journalId, experimentId);
        if (result.outcome() != Outcome.VALID) {
            failAndClose();
        }
        return result;
    }

    @Override
    public String journalId() {
        return journalId;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        releaseOwnershipOnce();
    }

    private void appendFrame(byte[] encoded) {
        ChainedJsonlStore.ForceMode forceMode = switch (syncPolicy) {
            case WRITE_TO_OS -> ChainedJsonlStore.ForceMode.NONE;
            case FORCE_DATA -> ChainedJsonlStore.ForceMode.DATA;
            case FORCE_DATA_AND_METADATA -> ChainedJsonlStore.ForceMode.DATA_AND_METADATA;
        };
        try {
            jsonlStore.appendFrame(
                    encoded,
                    totalBytes,
                    forceMode,
                    () -> directory.requireSameMutationAuthorization(mutationAuthorization),
                    writtenBytes -> failureInjector.checkpoint(
                            WriteCheckpoint.AFTER_WRITE_ATTEMPT, writtenBytes),
                    frameBytes -> failureInjector.checkpoint(
                            WriteCheckpoint.AFTER_APPEND_BEFORE_SYNC, frameBytes),
                    frameBytes -> failureInjector.checkpoint(
                            WriteCheckpoint.AFTER_SYNC, frameBytes));
        } catch (ChainedJsonlStore.StoreIOException exception) {
            Failure mapped = switch (exception.failure()) {
                case SIZE_LIMIT_EXCEEDED -> Failure.JOURNAL_SIZE_EXCEEDED;
                case UNSAFE_FILE -> Failure.UNSAFE_PATH;
                case ENTRY_LIMIT_EXCEEDED, ARCHIVE_LIMIT_EXCEEDED,
                        FRAME_SIZE_EXCEEDED,
                        INVALID_COMPLETE_FRAME, NON_CANONICAL_FRAME,
                        INCOMPLETE_TAIL, CONCURRENT_CHANGE,
                        FILE_IDENTITY_CHANGED, IO_FAILURE ->
                        Failure.IO_FAILURE;
            };
            throw EnterpriseLabExperimentJournalDirectory.failure(
                    mapped, "journal chained JSONL append did not complete", exception);
        }
    }

    private void validateAppend(EnterpriseLabExperimentJournalEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }
        if (!experimentId.equals(event.experimentId())) {
            throw EnterpriseLabExperimentJournalDirectory.failure(
                    Failure.IDENTITY_MISMATCH, "journal event experiment identity does not match");
        }
        if (event.sequence() != nextSequence) {
            throw EnterpriseLabExperimentJournalDirectory.failure(
                    Failure.SEQUENCE_MISMATCH, "journal event sequence is not the next contiguous value");
        }
        if (!previousFingerprint.equals(event.previousEntryFingerprint())) {
            throw EnterpriseLabExperimentJournalDirectory.failure(
                    Failure.PREDECESSOR_MISMATCH, "journal event predecessor fingerprint does not match");
        }
        Classification lifecycleFailure =
                EnterpriseLabExperimentJournalVerifier.nextEventFailure(lastEvent, event);
        if (lifecycleFailure != null) {
            throw EnterpriseLabExperimentJournalDirectory.failure(
                    Failure.VERIFICATION_FAILED,
                    "journal event failed lifecycle verification: " + lifecycleFailure.name());
        }
    }

    private void ensureOpen() {
        if (failed) {
            throw EnterpriseLabExperimentJournalDirectory.failure(
                    Failure.WRITER_FAILED, "journal writer is failed and cannot be reused");
        }
        if (closed) {
            throw EnterpriseLabExperimentJournalDirectory.failure(
                    Failure.CLOSED, "journal writer is closed");
        }
    }

    private void failAndClose() {
        failed = true;
        if (!closed) {
            closed = true;
            releaseOwnershipOnce();
        }
    }

    private void releaseOwnershipOnce() {
        if (!ownershipReleased) {
            ownershipReleased = true;
            releaseOwnership.run();
        }
    }
}
