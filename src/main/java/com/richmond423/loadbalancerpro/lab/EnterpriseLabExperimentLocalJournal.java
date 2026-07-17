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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Objects;

/** Package-private controlled-directory writer; callers use the journal interface. */
final class EnterpriseLabExperimentLocalJournal implements EnterpriseLabExperimentJournal {
    private static final int MAX_WRITE_CHUNK_BYTES = 256;
    private static final int MAX_CONSECUTIVE_ZERO_WRITES = 3;

    private final EnterpriseLabExperimentJournalDirectory directory;
    private final String journalId;
    private final String experimentId;
    private final Path journalPath;
    private final FileChannel channel;
    private final SyncPolicy syncPolicy;
    private final EnterpriseLabExperimentJournalCodec codec;
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
            FileChannel channel,
            SyncPolicy syncPolicy,
            EnterpriseLabExperimentJournalCodec codec,
            ReadResult existing,
            FailureInjector failureInjector,
            Runnable releaseOwnership) {
        this.directory = Objects.requireNonNull(directory, "directory cannot be null");
        this.journalId = Objects.requireNonNull(journalId, "journalId cannot be null");
        this.experimentId = Objects.requireNonNull(experimentId, "experimentId cannot be null");
        this.journalPath = Objects.requireNonNull(journalPath, "journalPath cannot be null");
        this.channel = Objects.requireNonNull(channel, "channel cannot be null");
        this.syncPolicy = Objects.requireNonNull(syncPolicy, "syncPolicy cannot be null");
        this.codec = Objects.requireNonNull(codec, "codec cannot be null");
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
            long observedSize = channel.size();
            if (observedSize != totalBytes) {
                throw EnterpriseLabExperimentJournalDirectory.failure(
                        Failure.IO_FAILURE, "journal changed outside its process-local writer");
            }

            byte[] frame = new byte[encoded.length + 1];
            System.arraycopy(encoded, 0, frame, 0, encoded.length);
            frame[frame.length - 1] = '\n';
            failureInjector.checkpoint(WriteCheckpoint.BEFORE_APPEND, 0);
            writeFrame(frame);
            failureInjector.checkpoint(WriteCheckpoint.AFTER_APPEND_BEFORE_SYNC, frame.length);

            PersistenceStage stage;
            boolean forceCompleted;
            if (syncPolicy == SyncPolicy.FORCE_DATA) {
                channel.force(false);
                failureInjector.checkpoint(WriteCheckpoint.AFTER_SYNC, frame.length);
                stage = PersistenceStage.DATA_FORCE_COMPLETE;
                forceCompleted = true;
            } else if (syncPolicy == SyncPolicy.FORCE_DATA_AND_METADATA) {
                channel.force(true);
                failureInjector.checkpoint(WriteCheckpoint.AFTER_SYNC, frame.length);
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
        IOException closeFailure = null;
        try {
            channel.close();
        } catch (IOException exception) {
            closeFailure = exception;
        } finally {
            releaseOwnershipOnce();
        }
        if (closeFailure != null) {
            throw EnterpriseLabExperimentJournalDirectory.failure(
                    Failure.IO_FAILURE, "journal writer could not be closed", closeFailure);
        }
    }

    private void writeFrame(byte[] frame) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(frame);
        int zeroWrites = 0;
        while (buffer.hasRemaining()) {
            int oldLimit = buffer.limit();
            buffer.limit(Math.min(oldLimit, buffer.position() + MAX_WRITE_CHUNK_BYTES));
            int written;
            try {
                written = channel.write(buffer);
            } finally {
                buffer.limit(oldLimit);
            }
            if (written == 0) {
                zeroWrites++;
                if (zeroWrites >= MAX_CONSECUTIVE_ZERO_WRITES) {
                    throw new IOException("bounded journal write made no progress");
                }
            } else {
                zeroWrites = 0;
                failureInjector.checkpoint(WriteCheckpoint.AFTER_WRITE_CHUNK, buffer.position());
            }
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
            try {
                channel.close();
            } catch (IOException ignored) {
                // The append failure remains authoritative.
            } finally {
                releaseOwnershipOnce();
            }
        }
    }

    private void releaseOwnershipOnce() {
        if (!ownershipReleased) {
            ownershipReleased = true;
            releaseOwnership.run();
        }
    }
}
