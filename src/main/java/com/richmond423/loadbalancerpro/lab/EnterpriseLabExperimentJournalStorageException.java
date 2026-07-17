package com.richmond423.loadbalancerpro.lab;

import java.util.Objects;

/** Typed local-storage failure that intentionally omits backing paths. */
public final class EnterpriseLabExperimentJournalStorageException extends IllegalStateException {
    private final Failure failure;

    public EnterpriseLabExperimentJournalStorageException(Failure failure, String message) {
        super(message);
        this.failure = Objects.requireNonNull(failure, "failure cannot be null");
    }

    public EnterpriseLabExperimentJournalStorageException(Failure failure, String message, Throwable cause) {
        super(message, cause);
        this.failure = Objects.requireNonNull(failure, "failure cannot be null");
    }

    public Failure failure() {
        return failure;
    }

    public enum Failure {
        UNSAFE_DIRECTORY,
        UNSAFE_PATH,
        WRITER_ALREADY_ACTIVE,
        PARTIAL_TAIL,
        INVALID_COMPLETE_ENTRY,
        NON_CANONICAL_ENTRY,
        IDENTITY_MISMATCH,
        SEQUENCE_MISMATCH,
        PREDECESSOR_MISMATCH,
        ENTRY_LIMIT_EXCEEDED,
        JOURNAL_SIZE_EXCEEDED,
        DISCOVERY_LIMIT_EXCEEDED,
        QUARANTINE_FAILED,
        VERIFICATION_FAILED,
        IO_FAILURE,
        WRITER_FAILED,
        CLOSED
    }
}
