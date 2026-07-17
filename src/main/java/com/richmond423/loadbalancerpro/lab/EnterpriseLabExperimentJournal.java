package com.richmond423.loadbalancerpro.lab;

import java.util.List;

/**
 * Narrow append-only contract for one local Enterprise Lab experiment journal.
 * Implementations must not expose their backing filesystem path.
 */
public interface EnterpriseLabExperimentJournal extends AutoCloseable {

    AppendReceipt append(EnterpriseLabExperimentJournalEvent event);

    ReadResult read();

    EnterpriseLabExperimentJournalVerifier.VerificationResult verify();

    String journalId();

    @Override
    void close();

    enum SyncPolicy {
        WRITE_TO_OS,
        FORCE_DATA,
        FORCE_DATA_AND_METADATA
    }

    enum PersistenceStage {
        OPERATING_SYSTEM_WRITE_COMPLETE,
        DATA_FORCE_COMPLETE,
        DATA_AND_METADATA_FORCE_COMPLETE
    }

    enum TailStatus {
        COMPLETE,
        TRUNCATED_TAIL
    }

    record AppendReceipt(
            String journalId,
            long sequence,
            String entryFingerprint,
            SyncPolicy syncPolicy,
            PersistenceStage persistenceStage,
            boolean userSpaceBufferRetained,
            boolean operatingSystemWriteCompleted,
            boolean forceCompleted) {
    }

    record ReadResult(
            String journalId,
            boolean exists,
            List<EnterpriseLabExperimentJournalEvent> events,
            TailStatus tailStatus,
            long completeBytes,
            long tailBytes,
            long totalBytes) {

        public ReadResult {
            events = List.copyOf(events);
        }
    }
}
