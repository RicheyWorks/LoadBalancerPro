package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.AcquisitionResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnerIdentity;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.Policy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReconciliationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReleaseStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.StaleClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.StaleOwnerFinding;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.TakeoverResult;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.LockSupport;

/** Non-blocking single-host ownership acquisition and takeover through an exclusive JDK file lock. */
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

    /**
     * Evaluates and, when safe, replaces a durable prior owner while holding the
     * repository-controlled OS lock. No live ownership capability is published
     * until the existing startup reconciler has completed successfully.
     */
    public static TakeoverAttempt takeover(
            Path trustedRoot,
            Policy policy,
            Clock clock,
            EnterpriseLabExperimentStartupReconciler reconciler) {
        Policy safePolicy = Objects.requireNonNull(policy, "policy cannot be null");
        Clock safeClock = Objects.requireNonNull(clock, "clock cannot be null");
        EnterpriseLabExperimentStartupReconciler safeReconciler = Objects.requireNonNull(
                reconciler, "reconciler cannot be null");
        try {
            EnterpriseLabEvidenceOwnershipPaths paths =
                    EnterpriseLabEvidenceOwnershipPaths.create(trustedRoot);
            if (!paths.trustedRoot().equals(safeReconciler.trustedRoot())) {
                return failedTakeover(
                        FailureClassification.DIRECTORY_IDENTITY_MISMATCH,
                        Optional.empty(), Optional.empty(), ReconciliationStatus.NOT_STARTED);
            }
            OwnerIdentity owner = newOwnerIdentity(paths);
            return takeover(paths, safePolicy, safeClock, owner, safeReconciler,
                    NO_FAILURE, JDK_EXCLUSIVE_LOCK);
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            return failedTakeover(exception.classification(), Optional.empty(),
                    Optional.empty(), ReconciliationStatus.NOT_STARTED);
        }
    }

    static TakeoverAttempt takeover(
            EnterpriseLabEvidenceOwnershipPaths paths,
            Policy policy,
            Clock clock,
            OwnerIdentity owner,
            EnterpriseLabExperimentStartupReconciler reconciler,
            FailureInjector failureInjector,
            LockOperation lockOperation) {
        EnterpriseLabEvidenceOwnershipPaths safePaths = Objects.requireNonNull(
                paths, "paths cannot be null");
        Policy safePolicy = Objects.requireNonNull(policy, "policy cannot be null");
        Clock safeClock = Objects.requireNonNull(clock, "clock cannot be null");
        OwnerIdentity safeOwner = Objects.requireNonNull(owner, "owner cannot be null");
        EnterpriseLabExperimentStartupReconciler safeReconciler = Objects.requireNonNull(
                reconciler, "reconciler cannot be null");
        FailureInjector safeFailureInjector = Objects.requireNonNull(
                failureInjector, "failureInjector cannot be null");
        LockOperation safeLockOperation = Objects.requireNonNull(
                lockOperation, "lockOperation cannot be null");
        if (!safePaths.trustedRoot().equals(safeReconciler.trustedRoot())) {
            return failedTakeover(
                    FailureClassification.DIRECTORY_IDENTITY_MISMATCH,
                    Optional.empty(), Optional.empty(), ReconciliationStatus.NOT_STARTED);
        }

        String localKey = safePaths.logicalLockIdentity();
        Object reservation = new Object();
        if (ACTIVE_LOCAL_OWNERS.putIfAbsent(localKey, reservation) != null) {
            StaleOwnerFinding finding = staleFinding(
                    StaleClassification.LIVE_COMPETING_OWNER,
                    Optional.empty(), false, "LIVE_LOCAL_OWNER_PRESENT");
            return refusedTakeover(FailureClassification.DUPLICATE_ACQUISITION,
                    Optional.empty(), Optional.of(finding));
        }

        FileChannel channel = null;
        FileLock lock = null;
        boolean transferred = false;
        Optional<OwnershipRecord> observed = Optional.empty();
        Optional<StaleOwnerFinding> finding = Optional.empty();
        ReconciliationStatus reconciliationStatus = ReconciliationStatus.NOT_STARTED;
        try {
            safeFailureInjector.check(FailurePoint.BEFORE_LOCK_OPEN);
            channel = safePaths.openLockChannel();
            lock = acquireBoundedLock(
                    channel, safePolicy, safePaths, safeFailureInjector, safeLockOperation);
            if (lock == null) {
                StaleOwnerFinding live = staleFinding(
                        StaleClassification.LIVE_COMPETING_OWNER,
                        Optional.empty(), false, "LIVE_OWNER_PRESENT");
                return refusedTakeover(FailureClassification.LIVE_COMPETING_OWNER,
                        Optional.empty(), Optional.of(live));
            }
            if (!channel.isOpen() || !lock.isValid() || lock.isShared()) {
                return failedTakeover(FailureClassification.LOCK_LOST,
                        Optional.empty(), Optional.empty(), reconciliationStatus);
            }

            safeFailureInjector.check(FailurePoint.AFTER_LOCK_ACQUIRED);
            safePaths.verifyDirectoryIdentity();
            verifyLockedPathStillAuthoritative(safePaths);
            String lockFileIdentity = safePaths.identityOfControlledRegularFile(
                    safePaths.lockFile());
            EnterpriseLabEvidenceOwnershipRecordStore recordStore =
                    new EnterpriseLabEvidenceOwnershipRecordStore(
                            safePaths, new EnterpriseLabEvidenceOwnershipCodec(),
                            safeFailureInjector);
            observed = recordStore.readIfPresentForTakeover();
            if (observed.isEmpty()) {
                StaleOwnerFinding absent = staleFinding(
                        StaleClassification.NO_PREVIOUS_OWNER,
                        Optional.empty(), true, "NO_PREVIOUS_OWNER_USE_ACQUIRE");
                return refusedTakeover(FailureClassification.TAKEOVER_NOT_PERMITTED,
                        Optional.empty(), Optional.of(absent));
            }

            OwnershipRecord prior = observed.orElseThrow();
            if (!safePaths.directoryIdentity().equals(prior.directoryIdentity())) {
                StaleOwnerFinding mismatch = staleFinding(
                        StaleClassification.DIRECTORY_IDENTITY_MISMATCH,
                        observed, true, "OWNERSHIP_DIRECTORY_IDENTITY_MISMATCH");
                return failedTakeover(FailureClassification.DIRECTORY_IDENTITY_MISMATCH,
                        observed, Optional.of(mismatch), reconciliationStatus);
            }
            if (!lockFileIdentity.equals(prior.lockFileIdentity())) {
                StaleOwnerFinding mismatch = staleFinding(
                        StaleClassification.LOCK_IDENTITY_MISMATCH,
                        observed, true, "LOCK_FILE_IDENTITY_MISMATCH");
                return failedTakeover(FailureClassification.LOCK_IDENTITY_MISMATCH,
                        observed, Optional.of(mismatch), reconciliationStatus);
            }
            finding = Optional.of(classifyPrior(prior, safeClock.instant()));
            StaleClassification classification = finding.orElseThrow().classification();
            if (classification == StaleClassification.TIMESTAMP_INVALID) {
                return failedTakeover(FailureClassification.CLOCK_REGRESSION,
                        observed, finding, reconciliationStatus);
            }
            if (classification == StaleClassification.ACTIVE_LOOKING_WITHOUT_LOCK) {
                return refusedTakeover(FailureClassification.TAKEOVER_NOT_PERMITTED,
                        observed, finding);
            }
            if (prior.generation() >= EnterpriseLabEvidenceOwnership.MAX_GENERATION
                    || prior.takeoverSequence()
                    >= EnterpriseLabEvidenceOwnership.MAX_TAKEOVER_SEQUENCE) {
                StaleOwnerFinding exhausted = staleFinding(
                        StaleClassification.GENERATION_INVALID,
                        observed, true, "TAKEOVER_GENERATION_EXHAUSTED");
                return failedTakeover(FailureClassification.GENERATION_EXHAUSTED,
                        observed, Optional.of(exhausted), reconciliationStatus);
            }
            if (classification != StaleClassification.CLEANLY_RELEASED
                    && classification != StaleClassification.STALE_CANDIDATE
                    && classification != StaleClassification.TAKEOVER_INCOMPLETE) {
                return refusedTakeover(FailureClassification.TAKEOVER_NOT_PERMITTED,
                        observed, finding);
            }

            recordStore.recoverInterruptedTakeoverTemporaries(prior);
            archiveWithRecovery(recordStore, prior);
            OwnershipRecord pending = takeoverRecord(
                    prior, safeOwner, safePolicy, safeClock.instant(),
                    OwnershipState.TAKEOVER_PENDING,
                    ReconciliationStatus.IN_PROGRESS,
                    finding.orElseThrow().reasonCode());
            OwnershipRecord verifiedPending = replaceWithRecovery(
                    recordStore, prior, pending);
            observed = Optional.of(verifiedPending);
            reconciliationStatus = ReconciliationStatus.IN_PROGRESS;
            safeFailureInjector.check(FailurePoint.BEFORE_TAKEOVER_RECONCILIATION);

            EnterpriseLabExperimentStartupReconciler.RecoveryReport report;
            try {
                report = safeReconciler.initialize();
            } catch (RuntimeException exception) {
                OwnershipRecord failed = publishTakeoverFailure(
                        recordStore, verifiedPending, safePolicy, safeClock.instant());
                observed = Optional.of(failed);
                return failedTakeover(FailureClassification.RECONCILIATION_FAILED,
                        observed, finding, ReconciliationStatus.FAILED);
            }
            if (!report.admissionAllowed()) {
                OwnershipRecord failed = publishTakeoverFailure(
                        recordStore, verifiedPending, safePolicy, safeClock.instant());
                observed = Optional.of(failed);
                return failedTakeover(FailureClassification.RECONCILIATION_FAILED,
                        observed, finding, ReconciliationStatus.FAILED);
            }

            OwnershipRecord complete = takeoverRecord(
                    verifiedPending,
                    verifiedPending.owner(),
                    safePolicy,
                    safeClock.instant(),
                    OwnershipState.TAKEOVER_COMPLETE,
                    ReconciliationStatus.SUCCEEDED,
                    verifiedPending.takeoverReasonCode());
            OwnershipRecord verifiedComplete = replaceWithRecovery(
                    recordStore, verifiedPending, complete);
            observed = Optional.of(verifiedComplete);
            reconciliationStatus = ReconciliationStatus.SUCCEEDED;
            if (!lock.isValid() || !channel.isOpen()) {
                return failedTakeover(FailureClassification.LOCK_LOST,
                        observed, finding, reconciliationStatus);
            }
            safePaths.verifyDirectoryIdentity();

            FileChannel heldChannel = channel;
            FileLock heldLock = lock;
            EnterpriseLabEvidenceOwnershipLease lease = new EnterpriseLabEvidenceOwnershipLease(
                    safePaths,
                    recordStore,
                    heldChannel,
                    heldLock,
                    verifiedComplete,
                    safePolicy,
                    safeClock,
                    safeFailureInjector,
                    () -> ACTIVE_LOCAL_OWNERS.remove(localKey, reservation));
            transferred = true;
            return new TakeoverAttempt(
                    new TakeoverResult(
                            OperationStatus.SUCCEEDED,
                            FailureClassification.NONE,
                            Optional.of(verifiedComplete),
                            ReconciliationStatus.SUCCEEDED,
                            "OWNERSHIP_TAKEOVER_COMPLETE"),
                    finding,
                    Optional.of(lease));
        } catch (OverlappingFileLockException exception) {
            StaleOwnerFinding live = staleFinding(
                    StaleClassification.LIVE_COMPETING_OWNER,
                    Optional.empty(), false, "LIVE_LOCAL_OWNER_PRESENT");
            return refusedTakeover(FailureClassification.DUPLICATE_ACQUISITION,
                    observed, Optional.of(live));
        } catch (UnsupportedOperationException exception) {
            return failedTakeover(FailureClassification.LOCK_UNSUPPORTED,
                    observed, finding, reconciliationStatus);
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            Optional<StaleOwnerFinding> classified = finding.isPresent()
                    ? finding
                    : findingForFailure(exception.classification(), observed, lock != null);
            return failedTakeover(exception.classification(), observed,
                    classified, reconciliationStatus);
        } catch (IOException exception) {
            return failedTakeover(FailureClassification.IO_FAILURE,
                    observed, finding, reconciliationStatus);
        } catch (SecurityException exception) {
            return failedTakeover(FailureClassification.PERMISSION_DENIED,
                    observed, finding, reconciliationStatus);
        } finally {
            if (!transferred) {
                closeResources(lock, channel);
                ACTIVE_LOCAL_OWNERS.remove(localKey, reservation);
            }
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
                    safePolicy,
                    safeClock,
                    safeFailureInjector,
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

    private static StaleOwnerFinding classifyPrior(
            OwnershipRecord prior,
            Instant now) {
        if (now.isBefore(prior.lastRenewedAt())) {
            return staleFinding(
                    StaleClassification.TIMESTAMP_INVALID,
                    Optional.of(prior), true, "CLOCK_PRECEDES_OWNER_RENEWAL");
        }
        if (prior.state() == OwnershipState.RELEASED) {
            return staleFinding(
                    StaleClassification.CLEANLY_RELEASED,
                    Optional.of(prior), true, "CLEAN_RELEASED_TAKEOVER");
        }
        if (now.isBefore(prior.leaseExpiresAt())) {
            return staleFinding(
                    StaleClassification.ACTIVE_LOOKING_WITHOUT_LOCK,
                    Optional.of(prior), true, "UNEXPIRED_OWNER_WITHOUT_LOCK");
        }
        if (prior.state() == OwnershipState.TAKEOVER_PENDING
                || (prior.generation() > EnterpriseLabEvidenceOwnership.INITIAL_GENERATION
                && (prior.reconciliationStatus() == ReconciliationStatus.IN_PROGRESS
                || prior.reconciliationStatus() == ReconciliationStatus.FAILED))) {
            return staleFinding(
                    StaleClassification.TAKEOVER_INCOMPLETE,
                    Optional.of(prior), true, "INCOMPLETE_TAKEOVER_RECOVERY");
        }
        return staleFinding(
                StaleClassification.STALE_CANDIDATE,
                Optional.of(prior), true, "EXPIRED_OWNER_WITHOUT_LOCK");
    }

    private static OwnershipRecord takeoverRecord(
            OwnershipRecord current,
            OwnerIdentity owner,
            Policy policy,
            Instant now,
            OwnershipState state,
            ReconciliationStatus reconciliationStatus,
            String reasonCode) {
        boolean newGeneration = state == OwnershipState.TAKEOVER_PENDING;
        long generation = newGeneration
                ? EnterpriseLabEvidenceOwnership.nextGeneration(current.generation())
                : current.generation();
        long sequence = newGeneration
                ? Math.addExact(current.takeoverSequence(), 1L)
                : current.takeoverSequence();
        Instant acquiredAt = newGeneration ? now : current.acquiredAt();
        String previousFingerprint = newGeneration
                ? current.recordFingerprint()
                : current.previousOwnerFingerprint();
        return OwnershipRecord.create(
                current.directoryIdentity(),
                current.lockFileIdentity(),
                owner,
                generation,
                state,
                acquiredAt,
                now,
                now.plus(policy.leaseDuration()),
                previousFingerprint,
                reasonCode,
                sequence,
                reconciliationStatus,
                ReleaseStatus.NOT_REQUESTED);
    }

    private static OwnershipRecord publishTakeoverFailure(
            EnterpriseLabEvidenceOwnershipRecordStore store,
            OwnershipRecord pending,
            Policy policy,
            Instant now) {
        OwnershipRecord failed = takeoverRecord(
                pending,
                pending.owner(),
                policy,
                now,
                OwnershipState.FAILED,
                ReconciliationStatus.FAILED,
                pending.takeoverReasonCode());
        return replaceWithRecovery(store, pending, failed);
    }

    private static OwnershipRecord replaceWithRecovery(
            EnterpriseLabEvidenceOwnershipRecordStore store,
            OwnershipRecord expected,
            OwnershipRecord replacement) {
        try {
            return store.takeoverAndVerify(expected, replacement);
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            Optional<OwnershipRecord> installed;
            try {
                installed = store.readIfPresent();
            } catch (EnterpriseLabEvidenceOwnershipException verificationFailure) {
                exception.addSuppressed(verificationFailure);
                throw exception;
            }
            if (installed.filter(replacement::equals).isPresent()) {
                return replacement;
            }
            throw exception;
        }
    }

    private static void archiveWithRecovery(
            EnterpriseLabEvidenceOwnershipRecordStore store,
            OwnershipRecord prior) {
        try {
            store.archivePriorAndVerify(prior);
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            try {
                if (store.archivePriorAndVerify(prior).equals(prior)) {
                    return;
                }
            } catch (EnterpriseLabEvidenceOwnershipException verificationFailure) {
                exception.addSuppressed(verificationFailure);
            }
            throw exception;
        }
    }

    private static Optional<StaleOwnerFinding> findingForFailure(
            FailureClassification failure,
            Optional<OwnershipRecord> record,
            boolean exclusiveLockAcquired) {
        StaleClassification classification = switch (failure) {
            case RECORD_MALFORMED -> StaleClassification.MALFORMED_RECORD;
            case UNSUPPORTED_RECORD_VERSION -> StaleClassification.UNSUPPORTED_RECORD;
            case RECORD_FINGERPRINT_MISMATCH -> StaleClassification.FINGERPRINT_MISMATCH;
            case DIRECTORY_IDENTITY_MISMATCH -> StaleClassification.DIRECTORY_IDENTITY_MISMATCH;
            case LOCK_IDENTITY_MISMATCH -> StaleClassification.LOCK_IDENTITY_MISMATCH;
            case GENERATION_REGRESSION, GENERATION_EXHAUSTED ->
                    StaleClassification.GENERATION_INVALID;
            case CLOCK_REGRESSION -> StaleClassification.TIMESTAMP_INVALID;
            case LIVE_COMPETING_OWNER, DUPLICATE_ACQUISITION ->
                    StaleClassification.LIVE_COMPETING_OWNER;
            default -> null;
        };
        if (classification == null) {
            return Optional.empty();
        }
        boolean requiresDecodedRecord = switch (classification) {
            case CLEANLY_RELEASED, STALE_CANDIDATE, ACTIVE_LOOKING_WITHOUT_LOCK,
                    DIRECTORY_IDENTITY_MISMATCH, LOCK_IDENTITY_MISMATCH,
                    GENERATION_INVALID, TIMESTAMP_INVALID, TAKEOVER_INCOMPLETE -> true;
            default -> false;
        };
        if (requiresDecodedRecord && record.isEmpty()) {
            return Optional.empty();
        }
        boolean lockAcquired = classification == StaleClassification.LIVE_COMPETING_OWNER
                ? false
                : exclusiveLockAcquired;
        return Optional.of(staleFinding(
                classification, record, lockAcquired, staleReasonCode(classification)));
    }

    private static StaleOwnerFinding staleFinding(
            StaleClassification classification,
            Optional<OwnershipRecord> record,
            boolean exclusiveLockAcquired,
            String reasonCode) {
        return new StaleOwnerFinding(
                classification, record, exclusiveLockAcquired, reasonCode);
    }

    private static String staleReasonCode(StaleClassification classification) {
        return switch (classification) {
            case NO_PREVIOUS_OWNER -> "NO_PREVIOUS_OWNER_USE_ACQUIRE";
            case CLEANLY_RELEASED -> "CLEAN_RELEASED_TAKEOVER";
            case LIVE_COMPETING_OWNER -> "LIVE_OWNER_PRESENT";
            case STALE_CANDIDATE -> "EXPIRED_OWNER_WITHOUT_LOCK";
            case ACTIVE_LOOKING_WITHOUT_LOCK -> "UNEXPIRED_OWNER_WITHOUT_LOCK";
            case MALFORMED_RECORD -> "OWNER_RECORD_MALFORMED";
            case UNSUPPORTED_RECORD -> "OWNER_RECORD_VERSION_UNSUPPORTED";
            case FINGERPRINT_MISMATCH -> "OWNER_RECORD_FINGERPRINT_MISMATCH";
            case DIRECTORY_IDENTITY_MISMATCH -> "OWNERSHIP_DIRECTORY_REPLACED";
            case LOCK_IDENTITY_MISMATCH -> "OWNERSHIP_LOCK_FILE_REPLACED";
            case GENERATION_INVALID -> "TAKEOVER_GENERATION_INVALID";
            case TIMESTAMP_INVALID -> "OWNER_CLOCK_REGRESSION";
            case TAKEOVER_INCOMPLETE -> "INCOMPLETE_TAKEOVER_RECOVERY";
        };
    }

    private static TakeoverAttempt refusedTakeover(
            FailureClassification failure,
            Optional<OwnershipRecord> record,
            Optional<StaleOwnerFinding> finding) {
        return new TakeoverAttempt(
                new TakeoverResult(
                        OperationStatus.REFUSED,
                        failure,
                        record,
                        ReconciliationStatus.NOT_STARTED,
                        reasonCode(failure)),
                finding,
                Optional.empty());
    }

    private static TakeoverAttempt failedTakeover(
            FailureClassification failure,
            Optional<OwnershipRecord> record,
            Optional<StaleOwnerFinding> finding,
            ReconciliationStatus reconciliationStatus) {
        return new TakeoverAttempt(
                new TakeoverResult(
                        OperationStatus.FAILED,
                        failure,
                        record,
                        reconciliationStatus,
                        reasonCode(failure)),
                finding,
                Optional.empty());
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
            case CLOCK_REGRESSION -> "OWNER_CLOCK_REGRESSION";
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

    public record TakeoverAttempt(
            TakeoverResult result,
            Optional<StaleOwnerFinding> staleOwnerFinding,
            Optional<EnterpriseLabEvidenceOwnershipLease> ownership) {
        public TakeoverAttempt {
            result = Objects.requireNonNull(result, "result cannot be null");
            staleOwnerFinding = Objects.requireNonNull(
                    staleOwnerFinding, "staleOwnerFinding cannot be null");
            ownership = Objects.requireNonNull(ownership, "ownership cannot be null");
            if ((result.status() == OperationStatus.SUCCEEDED) != ownership.isPresent()) {
                throw new IllegalArgumentException(
                        "successful takeover and live ownership resource must agree");
            }
            if (result.status() == OperationStatus.SUCCEEDED
                    && staleOwnerFinding.isEmpty()) {
                throw new IllegalArgumentException(
                        "successful takeover requires stale-owner evidence");
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
        BEFORE_RENEWAL_ATTEMPT,
        DURING_RENEWAL_RECORD_WRITE,
        AFTER_RENEWAL_RECORD_FORCE,
        AFTER_RENEWAL_RECORD_INSTALL,
        DURING_TAKEOVER_HISTORY_WRITE,
        AFTER_TAKEOVER_HISTORY_FORCE,
        AFTER_TAKEOVER_HISTORY_INSTALL,
        DURING_TAKEOVER_RECORD_WRITE,
        AFTER_TAKEOVER_RECORD_FORCE,
        AFTER_TAKEOVER_RECORD_INSTALL,
        BEFORE_TAKEOVER_RECONCILIATION,
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
