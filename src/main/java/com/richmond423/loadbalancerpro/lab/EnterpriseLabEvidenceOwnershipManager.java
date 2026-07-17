package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.AcquisitionResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnerIdentity;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.Policy;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.LockSupport;

/** Non-blocking single-host ownership acquisition through an exclusive JDK file lock. */
public final class EnterpriseLabEvidenceOwnershipManager {
    private static final ConcurrentMap<String, Object> ACTIVE_LOCAL_OWNERS =
            new ConcurrentHashMap<>();
    private static final FailureInjector NO_FAILURE = point -> {
    };
    private static final LockOperation JDK_EXCLUSIVE_LOCK = channel ->
            channel.tryLock(0L, Long.MAX_VALUE, false);

    private EnterpriseLabEvidenceOwnershipManager() {
    }

    public static AcquisitionAttempt acquire(
            Path trustedRoot,
            Policy policy,
            Clock clock) {
        Policy safePolicy = Objects.requireNonNull(policy, "policy cannot be null");
        Clock safeClock = Objects.requireNonNull(clock, "clock cannot be null");
        try {
            EnterpriseLabEvidenceOwnershipPaths paths =
                    EnterpriseLabEvidenceOwnershipPaths.create(trustedRoot);
            OwnerIdentity owner = newOwnerIdentity(paths);
            return acquire(paths, safePolicy, safeClock, owner, NO_FAILURE, JDK_EXCLUSIVE_LOCK);
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            return failedAttempt(exception.classification(), Optional.empty());
        }
    }

    static AcquisitionAttempt acquire(
            EnterpriseLabEvidenceOwnershipPaths paths,
            Policy policy,
            Clock clock,
            OwnerIdentity owner,
            FailureInjector failureInjector,
            LockOperation lockOperation) {
        EnterpriseLabEvidenceOwnershipPaths safePaths = Objects.requireNonNull(
                paths, "paths cannot be null");
        Policy safePolicy = Objects.requireNonNull(policy, "policy cannot be null");
        Clock safeClock = Objects.requireNonNull(clock, "clock cannot be null");
        OwnerIdentity safeOwner = Objects.requireNonNull(owner, "owner cannot be null");
        FailureInjector safeFailureInjector = Objects.requireNonNull(
                failureInjector, "failureInjector cannot be null");
        LockOperation safeLockOperation = Objects.requireNonNull(
                lockOperation, "lockOperation cannot be null");

        String localKey = safePaths.logicalLockIdentity();
        Object reservation = new Object();
        if (ACTIVE_LOCAL_OWNERS.putIfAbsent(localKey, reservation) != null) {
            return refusedAttempt(FailureClassification.DUPLICATE_ACQUISITION,
                    Optional.empty());
        }

        FileChannel channel = null;
        FileLock lock = null;
        boolean transferred = false;
        try {
            safeFailureInjector.check(FailurePoint.BEFORE_LOCK_OPEN);
            channel = safePaths.openLockChannel();
            lock = acquireBoundedLock(
                    channel, safePolicy, safePaths, safeFailureInjector, safeLockOperation);
            if (lock == null) {
                return refusedAttempt(FailureClassification.LIVE_COMPETING_OWNER,
                        Optional.empty());
            }
            if (!channel.isOpen() || !lock.isValid() || lock.isShared()) {
                return failedAttempt(FailureClassification.LOCK_LOST, Optional.empty());
            }

            safeFailureInjector.check(FailurePoint.AFTER_LOCK_ACQUIRED);
            safePaths.verifyDirectoryIdentity();
            verifyLockedPathStillAuthoritative(safePaths);
            String lockFileIdentity = safePaths.identityOfControlledRegularFile(
                    safePaths.lockFile());
            EnterpriseLabEvidenceOwnershipRecordStore recordStore =
                    new EnterpriseLabEvidenceOwnershipRecordStore(
                            safePaths, new EnterpriseLabEvidenceOwnershipCodec(), safeFailureInjector);
            Optional<OwnershipRecord> previous = recordStore.readIfPresent();
            if (previous.isPresent()) {
                OwnershipRecord prior = previous.orElseThrow();
                if (!lockFileIdentity.equals(prior.lockFileIdentity())) {
                    return failedAttempt(FailureClassification.LOCK_IDENTITY_MISMATCH, previous);
                }
                return refusedAttempt(FailureClassification.TAKEOVER_NOT_PERMITTED, previous);
            }

            OwnershipRecord record = OwnershipRecord.initial(
                    safeClock, safePolicy, safeOwner,
                    safePaths.directoryIdentity(), lockFileIdentity);
            OwnershipRecord verified = recordStore.writeNewAndVerify(record);
            if (!lock.isValid() || !channel.isOpen()) {
                return failedAttempt(FailureClassification.LOCK_LOST, Optional.of(verified));
            }
            safePaths.verifyDirectoryIdentity();

            FileChannel heldChannel = channel;
            FileLock heldLock = lock;
            EnterpriseLabEvidenceOwnershipLease lease = new EnterpriseLabEvidenceOwnershipLease(
                    safePaths,
                    recordStore,
                    heldChannel,
                    heldLock,
                    verified,
                    () -> ACTIVE_LOCAL_OWNERS.remove(localKey, reservation));
            transferred = true;
            return new AcquisitionAttempt(
                    new AcquisitionResult(
                            OperationStatus.SUCCEEDED,
                            FailureClassification.NONE,
                            Optional.of(verified),
                            "OWNERSHIP_ACQUIRED"),
                    Optional.of(lease));
        } catch (OverlappingFileLockException exception) {
            return refusedAttempt(FailureClassification.DUPLICATE_ACQUISITION,
                    Optional.empty());
        } catch (UnsupportedOperationException exception) {
            return failedAttempt(FailureClassification.LOCK_UNSUPPORTED, Optional.empty());
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            return failedAttempt(exception.classification(), Optional.empty());
        } catch (IOException exception) {
            return failedAttempt(FailureClassification.IO_FAILURE, Optional.empty());
        } catch (SecurityException exception) {
            return failedAttempt(FailureClassification.PERMISSION_DENIED, Optional.empty());
        } finally {
            if (!transferred) {
                closeResources(lock, channel);
                ACTIVE_LOCAL_OWNERS.remove(localKey, reservation);
            }
        }
    }

    private static FileLock acquireBoundedLock(
            FileChannel channel,
            Policy policy,
            EnterpriseLabEvidenceOwnershipPaths paths,
            FailureInjector failureInjector,
            LockOperation lockOperation) throws IOException {
        for (int attempt = 1; attempt <= policy.acquisitionAttempts(); attempt++) {
            failureInjector.check(FailurePoint.BEFORE_LOCK_ATTEMPT);
            paths.verifyDirectoryIdentity();
            FileLock lock = lockOperation.tryLock(channel);
            if (lock != null) {
                return lock;
            }
            if (attempt < policy.acquisitionAttempts()) {
                LockSupport.parkNanos(policy.retryDelay().toNanos());
                if (Thread.currentThread().isInterrupted()) {
                    throw new IOException("ownership acquisition was interrupted");
                }
            }
        }
        return null;
    }

    private static void verifyLockedPathStillAuthoritative(
            EnterpriseLabEvidenceOwnershipPaths paths) {
        try (FileChannel probeChannel = paths.openLockChannel()) {
            try {
                FileLock probe = probeChannel.tryLock(0L, Long.MAX_VALUE, false);
                if (probe != null) {
                    probe.release();
                }
                throw new EnterpriseLabEvidenceOwnershipException(
                        FailureClassification.LOCK_IDENTITY_MISMATCH,
                        "locked ownership file no longer matches its controlled path");
            } catch (OverlappingFileLockException expected) {
                // The controlled path resolves to the exact file already locked by this JVM.
            }
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            throw exception;
        } catch (UnsupportedOperationException exception) {
            throw new EnterpriseLabEvidenceOwnershipException(
                    FailureClassification.LOCK_UNSUPPORTED,
                    "ownership lock identity probe is unsupported", exception);
        } catch (IOException exception) {
            throw new EnterpriseLabEvidenceOwnershipException(
                    FailureClassification.IO_FAILURE,
                    "ownership lock identity probe failed", exception);
        }
    }

    private static OwnerIdentity newOwnerIdentity(
            EnterpriseLabEvidenceOwnershipPaths paths) {
        String ownerId = "owner-" + UUID.randomUUID();
        String applicationInstanceId = "instance-" + UUID.randomUUID();
        long processId;
        try {
            processId = ProcessHandle.current().pid();
        } catch (RuntimeException exception) {
            processId = 0L;
        }
        String hostDiagnostic = sha256(
                "enterprise-lab-single-host-diagnostic/v1\n" + paths.directoryIdentity());
        return new OwnerIdentity(ownerId, applicationInstanceId, processId, hostDiagnostic);
    }

    private static AcquisitionAttempt refusedAttempt(
            FailureClassification failure,
            Optional<OwnershipRecord> record) {
        return new AcquisitionAttempt(
                new AcquisitionResult(
                        OperationStatus.REFUSED,
                        failure,
                        record,
                        reasonCode(failure)),
                Optional.empty());
    }

    private static AcquisitionAttempt failedAttempt(
            FailureClassification failure,
            Optional<OwnershipRecord> record) {
        OperationStatus status = switch (failure) {
            case DUPLICATE_ACQUISITION, LIVE_COMPETING_OWNER, TAKEOVER_NOT_PERMITTED ->
                    OperationStatus.REFUSED;
            default -> OperationStatus.FAILED;
        };
        return new AcquisitionAttempt(
                new AcquisitionResult(status, failure, record, reasonCode(failure)),
                Optional.empty());
    }

    private static String reasonCode(FailureClassification failure) {
        return switch (failure) {
            case UNSAFE_PATH -> "UNSAFE_OWNERSHIP_PATH";
            case STORAGE_UNAVAILABLE -> "OWNERSHIP_STORAGE_UNAVAILABLE";
            case PERMISSION_DENIED -> "OWNERSHIP_PERMISSION_DENIED";
            case LOCK_UNSUPPORTED -> "OWNERSHIP_LOCK_UNSUPPORTED";
            case LIVE_COMPETING_OWNER -> "LIVE_OWNER_PRESENT";
            case DUPLICATE_ACQUISITION -> "DUPLICATE_LOCAL_ACQUISITION";
            case RECORD_MALFORMED -> "OWNER_RECORD_MALFORMED";
            case UNSUPPORTED_RECORD_VERSION -> "OWNER_RECORD_VERSION_UNSUPPORTED";
            case RECORD_FINGERPRINT_MISMATCH -> "OWNER_RECORD_FINGERPRINT_MISMATCH";
            case DIRECTORY_IDENTITY_MISMATCH -> "OWNERSHIP_DIRECTORY_REPLACED";
            case LOCK_IDENTITY_MISMATCH -> "OWNERSHIP_LOCK_FILE_REPLACED";
            case GENERATION_REGRESSION -> "OWNER_GENERATION_REGRESSED";
            case GENERATION_EXHAUSTED -> "OWNER_GENERATION_EXHAUSTED";
            case RENEWAL_DEADLINE_EXCEEDED -> "OWNER_RENEWAL_DEADLINE_EXCEEDED";
            case LOCK_LOST -> "OWNERSHIP_LOCK_LOST";
            case RECORD_REPLACED -> "OWNER_RECORD_REPLACED";
            case RELEASE_FAILED -> "OWNERSHIP_RELEASE_FAILED";
            case RECONCILIATION_FAILED -> "OWNERSHIP_RECONCILIATION_FAILED";
            case TAKEOVER_NOT_PERMITTED -> "PRIOR_RECORD_REQUIRES_TAKEOVER";
            case IO_FAILURE -> "OWNERSHIP_IO_FAILURE";
            case NONE -> throw new IllegalArgumentException("failure reason requires a failure");
        };
    }

    private static void closeResources(FileLock lock, FileChannel channel) {
        if (lock != null) {
            try {
                if (lock.isValid()) {
                    lock.release();
                }
            } catch (IOException ignored) {
                // Channel closure below is the final bounded release attempt.
            }
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
                // Acquisition already failed; no live capability is published.
            }
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record AcquisitionAttempt(
            AcquisitionResult result,
            Optional<EnterpriseLabEvidenceOwnershipLease> ownership) {
        public AcquisitionAttempt {
            result = Objects.requireNonNull(result, "result cannot be null");
            ownership = Objects.requireNonNull(ownership, "ownership cannot be null");
            if ((result.status() == OperationStatus.SUCCEEDED) != ownership.isPresent()) {
                throw new IllegalArgumentException(
                        "successful acquisition and live ownership resource must agree");
            }
        }
    }

    enum FailurePoint {
        BEFORE_LOCK_OPEN,
        BEFORE_LOCK_ATTEMPT,
        AFTER_LOCK_ACQUIRED,
        DURING_RECORD_WRITE,
        AFTER_RECORD_FORCE,
        AFTER_RECORD_INSTALL,
        DURING_RELEASE_RECORD_WRITE,
        AFTER_RELEASE_RECORD_FORCE,
        AFTER_RELEASE_RECORD_INSTALL
    }

    @FunctionalInterface
    interface FailureInjector {
        void check(FailurePoint point) throws IOException;
    }

    @FunctionalInterface
    interface LockOperation {
        FileLock tryLock(FileChannel channel) throws IOException;
    }
}
