package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReleaseResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReleaseStatus;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Objects;
import java.util.Optional;

/** Live, non-detachable ownership resource that retains the OS lock and channel. */
public final class EnterpriseLabEvidenceOwnershipLease implements AutoCloseable {
    private final EnterpriseLabEvidenceOwnershipPaths paths;
    private final EnterpriseLabEvidenceOwnershipRecordStore recordStore;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private final Runnable releaseLocalReservation;

    private OwnershipRecord record;
    private ReleaseResult finalReleaseResult;
    private boolean localReservationReleased;

    EnterpriseLabEvidenceOwnershipLease(
            EnterpriseLabEvidenceOwnershipPaths paths,
            EnterpriseLabEvidenceOwnershipRecordStore recordStore,
            FileChannel lockChannel,
            FileLock lock,
            OwnershipRecord record,
            Runnable releaseLocalReservation) {
        this.paths = Objects.requireNonNull(paths, "paths cannot be null");
        this.recordStore = Objects.requireNonNull(recordStore, "recordStore cannot be null");
        this.lockChannel = Objects.requireNonNull(lockChannel, "lockChannel cannot be null");
        this.lock = Objects.requireNonNull(lock, "lock cannot be null");
        this.record = Objects.requireNonNull(record, "record cannot be null");
        this.releaseLocalReservation = Objects.requireNonNull(
                releaseLocalReservation, "releaseLocalReservation cannot be null");
        if (!lockChannel.isOpen() || !lock.isValid() || lock.isShared()) {
            throw new IllegalArgumentException(
                    "live ownership requires an open channel and valid exclusive lock");
        }
    }

    public synchronized OwnershipRecord record() {
        return record;
    }

    public synchronized boolean operatingSystemLockValid() {
        return finalReleaseResult == null && lockChannel.isOpen() && lock.isValid() && !lock.isShared();
    }

    public synchronized ReleaseResult release() {
        if (finalReleaseResult != null) {
            return finalReleaseResult;
        }

        EnterpriseLabEvidenceOwnershipException releaseFailure = null;
        OwnershipRecord intendedRelease = null;
        if (!operatingSystemLockValid()) {
            releaseFailure = failure(FailureClassification.LOCK_LOST,
                    "ownership lock was lost before release publication");
        } else {
            try {
                paths.verifyDirectoryIdentity();
                intendedRelease = releasedRecord(record);
                record = recordStore.replaceAndVerify(record, intendedRelease);
            } catch (EnterpriseLabEvidenceOwnershipException exception) {
                releaseFailure = exception;
            } catch (RuntimeException exception) {
                releaseFailure = failure(FailureClassification.RELEASE_FAILED,
                        "ownership release record could not be published", exception);
            }
        }

        if (releaseFailure != null && intendedRelease != null) {
            try {
                Optional<OwnershipRecord> installed = recordStore.readIfPresent();
                if (installed.isPresent() && installed.orElseThrow().equals(intendedRelease)) {
                    record = installed.orElseThrow();
                    releaseFailure = null;
                }
            } catch (EnterpriseLabEvidenceOwnershipException ignored) {
                // Preserve the original release failure when read-back cannot prove installation.
            }
        }

        boolean operatingSystemLockReleased = releaseOperatingSystemResources();
        releaseReservationOnce();
        if (releaseFailure == null && operatingSystemLockReleased) {
            finalReleaseResult = new ReleaseResult(
                    OperationStatus.SUCCEEDED,
                    FailureClassification.NONE,
                    Optional.of(record),
                    true,
                    "OWNERSHIP_RELEASED");
        } else {
            FailureClassification classification = releaseFailure == null
                    ? FailureClassification.RELEASE_FAILED
                    : releaseFailure.classification();
            finalReleaseResult = new ReleaseResult(
                    OperationStatus.FAILED,
                    classification,
                    Optional.of(record),
                    operatingSystemLockReleased,
                    releaseReasonCode(classification));
        }
        return finalReleaseResult;
    }

    @Override
    public synchronized void close() {
        ReleaseResult result = release();
        if (result.status() != OperationStatus.SUCCEEDED) {
            throw failure(result.failure(), "ownership release did not complete cleanly");
        }
    }

    private static OwnershipRecord releasedRecord(OwnershipRecord current) {
        return OwnershipRecord.create(
                current.directoryIdentity(),
                current.lockFileIdentity(),
                current.owner(),
                current.generation(),
                OwnershipState.RELEASED,
                current.acquiredAt(),
                current.lastRenewedAt(),
                current.leaseExpiresAt(),
                current.previousOwnerFingerprint(),
                current.takeoverReasonCode(),
                current.takeoverSequence(),
                current.reconciliationStatus(),
                ReleaseStatus.RELEASED);
    }

    private boolean releaseOperatingSystemResources() {
        try {
            if (lock.isValid()) {
                lock.release();
            }
        } catch (IOException ignored) {
            // Closing the channel below is the final bounded OS release attempt.
        }
        try {
            lockChannel.close();
        } catch (IOException ignored) {
            // Final validity checks below determine whether release is proven.
        }
        return !lock.isValid() && !lockChannel.isOpen();
    }

    private void releaseReservationOnce() {
        if (!localReservationReleased) {
            localReservationReleased = true;
            releaseLocalReservation.run();
        }
    }

    private static String releaseReasonCode(FailureClassification failure) {
        return switch (failure) {
            case LOCK_LOST -> "OWNERSHIP_LOCK_LOST";
            case DIRECTORY_IDENTITY_MISMATCH -> "OWNERSHIP_DIRECTORY_REPLACED";
            case LOCK_IDENTITY_MISMATCH -> "OWNERSHIP_LOCK_FILE_REPLACED";
            case RECORD_REPLACED -> "OWNER_RECORD_REPLACED";
            case RECORD_MALFORMED -> "OWNER_RECORD_MALFORMED";
            case RECORD_FINGERPRINT_MISMATCH -> "OWNER_RECORD_FINGERPRINT_MISMATCH";
            case UNSUPPORTED_RECORD_VERSION -> "OWNER_RECORD_VERSION_UNSUPPORTED";
            case PERMISSION_DENIED -> "OWNERSHIP_PERMISSION_DENIED";
            case STORAGE_UNAVAILABLE -> "OWNERSHIP_STORAGE_UNAVAILABLE";
            case IO_FAILURE, RELEASE_FAILED -> "OWNERSHIP_RELEASE_FAILED";
            default -> "OWNERSHIP_RELEASE_FAILED";
        };
    }

    private static EnterpriseLabEvidenceOwnershipException failure(
            FailureClassification classification,
            String message) {
        return new EnterpriseLabEvidenceOwnershipException(classification, message);
    }

    private static EnterpriseLabEvidenceOwnershipException failure(
            FailureClassification classification,
            String message,
            Throwable cause) {
        return new EnterpriseLabEvidenceOwnershipException(classification, message, cause);
    }
}
