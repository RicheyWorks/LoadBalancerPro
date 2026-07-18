package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.Policy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReleaseResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReleaseStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.RenewalResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.FailureInjector;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.FailurePoint;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.LockSupport;

/** Live, non-detachable ownership resource that retains the OS lock and channel. */
public final class EnterpriseLabEvidenceOwnershipLease implements AutoCloseable {
    private final EnterpriseLabEvidenceOwnershipPaths paths;
    private final EnterpriseLabEvidenceOwnershipRecordStore recordStore;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private final Policy policy;
    private final Clock clock;
    private final FailureInjector failureInjector;
    private final Runnable releaseLocalReservation;
    private final EnterpriseLabEvidenceOwnershipGate ownershipGate;

    private OwnershipRecord record;
    private ReleaseResult finalReleaseResult;
    private FailureClassification terminalFailure;
    private String terminalReasonCode;
    private boolean localReservationReleased;

    EnterpriseLabEvidenceOwnershipLease(
            EnterpriseLabEvidenceOwnershipPaths paths,
            EnterpriseLabEvidenceOwnershipRecordStore recordStore,
            FileChannel lockChannel,
            FileLock lock,
            OwnershipRecord record,
            Policy policy,
            Clock clock,
            FailureInjector failureInjector,
            Runnable releaseLocalReservation) {
        this.paths = Objects.requireNonNull(paths, "paths cannot be null");
        this.recordStore = Objects.requireNonNull(recordStore, "recordStore cannot be null");
        this.lockChannel = Objects.requireNonNull(lockChannel, "lockChannel cannot be null");
        this.lock = Objects.requireNonNull(lock, "lock cannot be null");
        this.record = Objects.requireNonNull(record, "record cannot be null");
        this.policy = Objects.requireNonNull(policy, "policy cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.failureInjector = Objects.requireNonNull(
                failureInjector, "failureInjector cannot be null");
        this.releaseLocalReservation = Objects.requireNonNull(
                releaseLocalReservation, "releaseLocalReservation cannot be null");
        if (!lockChannel.isOpen() || !lock.isValid() || lock.isShared()) {
            throw new IllegalArgumentException(
                    "live ownership requires an open channel and valid exclusive lock");
        }
        this.ownershipGate = new EnterpriseLabEvidenceOwnershipGate(this);
    }

    public synchronized OwnershipRecord record() {
        return record;
    }

    public synchronized boolean operatingSystemLockValid() {
        return finalReleaseResult == null && lockChannel.isOpen() && lock.isValid() && !lock.isShared();
    }

    public EnterpriseLabEvidenceOwnershipGate ownershipGate() {
        return ownershipGate;
    }

    synchronized VerificationResult verifyCurrentOwnership() {
        if (finalReleaseResult != null) {
            return failedVerification(
                    FailureClassification.LOCK_LOST,
                    "OWNERSHIP_NOT_ACTIVE",
                    false);
        }
        if (terminalFailure != null) {
            return failedVerification(
                    terminalFailure,
                    terminalReasonCode,
                    operatingSystemLockValid());
        }
        try {
            VerificationSnapshot verified = verifyAuthoritativeSnapshot();
            return new VerificationResult(
                    OperationStatus.SUCCEEDED,
                    FailureClassification.NONE,
                    Optional.of(verified.record()),
                    true,
                    "OWNERSHIP_VERIFIED");
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            latchFailure(exception.classification());
            return failedVerification(
                    terminalFailure,
                    terminalReasonCode,
                    operatingSystemLockValid());
        } catch (RuntimeException exception) {
            latchFailure(FailureClassification.IO_FAILURE);
            return failedVerification(
                    terminalFailure,
                    terminalReasonCode,
                    operatingSystemLockValid());
        }
    }

    synchronized RenewalResult renewCurrentOwnership() {
        if (finalReleaseResult != null) {
            return failedRenewal(FailureClassification.LOCK_LOST, "OWNERSHIP_NOT_ACTIVE");
        }
        if (terminalFailure != null) {
            return failedRenewal(terminalFailure, terminalReasonCode);
        }

        VerificationSnapshot verified = null;
        EnterpriseLabEvidenceOwnershipException renewalFailure = null;
        for (int attempt = 1; attempt <= policy.renewalAttempts(); attempt++) {
            try {
                failureInjector.check(FailurePoint.BEFORE_RENEWAL_ATTEMPT);
                verified = verifyAuthoritativeSnapshot();
                renewalFailure = null;
                break;
            } catch (IOException exception) {
                renewalFailure = failure(
                        FailureClassification.IO_FAILURE,
                        "ownership renewal preflight failed", exception);
            } catch (EnterpriseLabEvidenceOwnershipException exception) {
                renewalFailure = exception;
            } catch (RuntimeException exception) {
                renewalFailure = failure(
                        FailureClassification.IO_FAILURE,
                        "ownership renewal preflight failed", exception);
            }
            if (!isRetryable(renewalFailure.classification())
                    || attempt == policy.renewalAttempts()) {
                break;
            }
            renewalFailure = awaitRetryDelay();
            if (renewalFailure != null) {
                break;
            }
        }

        if (renewalFailure != null || verified == null) {
            FailureClassification classification = renewalFailure == null
                    ? FailureClassification.IO_FAILURE
                    : renewalFailure.classification();
            latchFailure(classification);
            return failedRenewal(terminalFailure, terminalReasonCode);
        }

        Instant renewedAt = verified.verifiedAt();
        if (renewedAt.isBefore(record.lastRenewedAt())) {
            latchFailure(FailureClassification.CLOCK_REGRESSION);
            return failedRenewal(terminalFailure, terminalReasonCode);
        }
        if (renewedAt.equals(record.lastRenewedAt())) {
            return new RenewalResult(
                    OperationStatus.SUCCEEDED,
                    FailureClassification.NONE,
                    Optional.of(record),
                    "OWNERSHIP_RENEWAL_CURRENT");
        }

        OwnershipRecord intended = renewedRecord(record, renewedAt, policy);
        try {
            record = recordStore.renewAndVerify(record, intended);
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            renewalFailure = exception;
        } catch (RuntimeException exception) {
            renewalFailure = failure(
                    FailureClassification.IO_FAILURE,
                    "ownership renewal record could not be published", exception);
        }

        if (renewalFailure != null) {
            try {
                Optional<OwnershipRecord> installed = recordStore.readIfPresent();
                if (installed.isPresent() && installed.orElseThrow().equals(intended)) {
                    record = installed.orElseThrow();
                    renewalFailure = null;
                }
            } catch (EnterpriseLabEvidenceOwnershipException ignored) {
                // Preserve the original renewal failure when read-back cannot prove installation.
            }
        }

        if (renewalFailure != null) {
            latchFailure(renewalFailure.classification());
            return failedRenewal(terminalFailure, terminalReasonCode);
        }

        try {
            VerificationSnapshot postWrite = verifyAuthoritativeSnapshot();
            return new RenewalResult(
                    OperationStatus.SUCCEEDED,
                    FailureClassification.NONE,
                    Optional.of(postWrite.record()),
                    "OWNERSHIP_RENEWED");
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            latchFailure(exception.classification());
            return failedRenewal(terminalFailure, terminalReasonCode);
        } catch (RuntimeException exception) {
            latchFailure(FailureClassification.IO_FAILURE);
            return failedRenewal(terminalFailure, terminalReasonCode);
        }
    }

    private VerificationSnapshot verifyAuthoritativeSnapshot() {
        if (!operatingSystemLockValid()) {
            throw failure(FailureClassification.LOCK_LOST,
                    "ownership lock is no longer valid");
        }
        paths.verifyDirectoryIdentity();
        String currentLockIdentity = paths.identityOfControlledRegularFile(paths.lockFile());
        if (!record.lockFileIdentity().equals(currentLockIdentity)) {
            throw failure(FailureClassification.LOCK_IDENTITY_MISMATCH,
                    "ownership lock file identity changed");
        }

        OwnershipRecord installed = recordStore.readIfPresent()
                .orElseThrow(() -> failure(
                        FailureClassification.RECORD_REPLACED,
                        "ownership record is absent"));
        if (installed.generation() < record.generation()) {
            throw failure(FailureClassification.GENERATION_REGRESSION,
                    "ownership generation regressed");
        }
        if (installed.generation() != record.generation()
                || !installed.owner().equals(record.owner())
                || !installed.lockFileIdentity().equals(currentLockIdentity)
                || !installed.equals(record)) {
            throw failure(FailureClassification.RECORD_REPLACED,
                    "ownership record no longer identifies the live owner");
        }

        Instant verifiedAt;
        try {
            verifiedAt = clock.instant();
        } catch (RuntimeException exception) {
            throw failure(FailureClassification.IO_FAILURE,
                    "ownership clock is unavailable", exception);
        }
        if (verifiedAt.isBefore(installed.lastRenewedAt())) {
            throw failure(FailureClassification.CLOCK_REGRESSION,
                    "ownership clock regressed before the durable renewal time");
        }
        if (verifiedAt.isAfter(installed.leaseExpiresAt())) {
            throw failure(FailureClassification.RENEWAL_DEADLINE_EXCEEDED,
                    "ownership renewal deadline was exceeded");
        }
        return new VerificationSnapshot(installed, verifiedAt);
    }

    private EnterpriseLabEvidenceOwnershipException awaitRetryDelay() {
        if (policy.retryDelay().isZero()) {
            return null;
        }
        LockSupport.parkNanos(policy.retryDelay().toNanos());
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            return failure(FailureClassification.IO_FAILURE,
                    "ownership renewal retry was interrupted");
        }
        return null;
    }

    private void latchFailure(FailureClassification classification) {
        if (terminalFailure == null) {
            terminalFailure = Objects.requireNonNull(
                    classification, "classification cannot be null");
            terminalReasonCode = ownershipReasonCode(classification);
        }
    }

    private VerificationResult failedVerification(
            FailureClassification classification,
            String reasonCode,
            boolean operatingSystemLockValid) {
        return new VerificationResult(
                OperationStatus.FAILED,
                classification,
                Optional.of(record),
                operatingSystemLockValid,
                reasonCode);
    }

    private RenewalResult failedRenewal(
            FailureClassification classification,
            String reasonCode) {
        return new RenewalResult(
                OperationStatus.FAILED,
                classification,
                Optional.of(record),
                reasonCode);
    }

    private static boolean isRetryable(FailureClassification classification) {
        return classification == FailureClassification.IO_FAILURE;
    }

    private static OwnershipRecord renewedRecord(
            OwnershipRecord current,
            Instant renewedAt,
            Policy policy) {
        return OwnershipRecord.create(
                current.directoryIdentity(),
                current.lockFileIdentity(),
                current.owner(),
                current.generation(),
                OwnershipState.OWNED,
                current.acquiredAt(),
                renewedAt,
                renewedAt.plus(policy.leaseDuration()),
                current.previousOwnerFingerprint(),
                current.takeoverReasonCode(),
                current.takeoverSequence(),
                current.reconciliationStatus(),
                current.releaseStatus());
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

    private static String ownershipReasonCode(FailureClassification failure) {
        return switch (failure) {
            case LOCK_LOST -> "OWNERSHIP_LOCK_LOST";
            case DIRECTORY_IDENTITY_MISMATCH -> "OWNERSHIP_DIRECTORY_REPLACED";
            case LOCK_IDENTITY_MISMATCH -> "OWNERSHIP_LOCK_FILE_REPLACED";
            case RECORD_REPLACED -> "OWNER_RECORD_REPLACED";
            case RECORD_MALFORMED -> "OWNER_RECORD_MALFORMED";
            case RECORD_FINGERPRINT_MISMATCH -> "OWNER_RECORD_FINGERPRINT_MISMATCH";
            case UNSUPPORTED_RECORD_VERSION -> "OWNER_RECORD_VERSION_UNSUPPORTED";
            case GENERATION_REGRESSION -> "OWNER_GENERATION_REGRESSED";
            case CLOCK_REGRESSION -> "OWNER_CLOCK_REGRESSION";
            case RENEWAL_DEADLINE_EXCEEDED -> "OWNER_RENEWAL_DEADLINE_EXCEEDED";
            case PERMISSION_DENIED -> "OWNERSHIP_PERMISSION_DENIED";
            case STORAGE_UNAVAILABLE -> "OWNERSHIP_STORAGE_UNAVAILABLE";
            case IO_FAILURE -> "OWNERSHIP_VERIFICATION_FAILED";
            default -> "OWNERSHIP_VERIFICATION_FAILED";
        };
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

    private record VerificationSnapshot(
            OwnershipRecord record,
            Instant verifiedAt) {
    }
}
