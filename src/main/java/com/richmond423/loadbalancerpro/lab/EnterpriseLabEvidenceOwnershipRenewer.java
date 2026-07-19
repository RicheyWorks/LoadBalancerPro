package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.RenewalResult;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** One bounded daemon task that renews only an already-published live owner. */
final class EnterpriseLabEvidenceOwnershipRenewer implements AutoCloseable {
    private final EnterpriseLabEvidenceOwnershipGate ownershipGate;
    private final EnterpriseLabExperimentRecoveryGate recoveryGate;
    private final Optional<EnterpriseLabAllocationReconciliationGate>
            allocationReconciliationGate;
    private final ScheduledThreadPoolExecutor executor;
    private final ScheduledFuture<?> renewalTask;

    private RenewalResult lastResult;
    private boolean closed;

    EnterpriseLabEvidenceOwnershipRenewer(
            EnterpriseLabEvidenceOwnershipGate ownershipGate,
            EnterpriseLabExperimentRecoveryGate recoveryGate,
            Duration renewalInterval) {
        this(ownershipGate, recoveryGate, Optional.empty(), renewalInterval);
    }

    EnterpriseLabEvidenceOwnershipRenewer(
            EnterpriseLabEvidenceOwnershipGate ownershipGate,
            EnterpriseLabExperimentRecoveryGate recoveryGate,
            Optional<EnterpriseLabAllocationReconciliationGate> allocationReconciliationGate,
            Duration renewalInterval) {
        this.ownershipGate = Objects.requireNonNull(
                ownershipGate, "ownershipGate cannot be null");
        this.recoveryGate = Objects.requireNonNull(
                recoveryGate, "recoveryGate cannot be null");
        this.allocationReconciliationGate = Objects.requireNonNull(
                allocationReconciliationGate,
                "allocationReconciliationGate cannot be null");
        Duration safeInterval = Objects.requireNonNull(
                renewalInterval, "renewalInterval cannot be null");
        if (safeInterval.isZero() || safeInterval.isNegative()
                || safeInterval.compareTo(
                        EnterpriseLabEvidenceOwnership.Policy.HARD_MAX_RENEWAL_INTERVAL) > 0) {
            throw new IllegalArgumentException("renewalInterval is outside bounded ownership limits");
        }
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "enterprise-lab-ownership-renewer");
            thread.setDaemon(true);
            return thread;
        };
        this.executor = new ScheduledThreadPoolExecutor(1, threadFactory);
        this.executor.setRemoveOnCancelPolicy(true);
        this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        long intervalMillis = Math.max(1L, safeInterval.toMillis());
        this.renewalTask = executor.scheduleAtFixedRate(
                this::renewOnce,
                intervalMillis,
                intervalMillis,
                TimeUnit.MILLISECONDS);
    }

    synchronized Optional<RenewalResult> lastResult() {
        return Optional.ofNullable(lastResult);
    }

    private void renewOnce() {
        synchronized (this) {
            if (closed) {
                return;
            }
        }
        RenewalResult result;
        try {
            result = ownershipGate.renew();
        } catch (RuntimeException exception) {
            failAdmission();
            closeAfterFailure();
            return;
        }
        synchronized (this) {
            lastResult = result;
        }
        if (result.status() != OperationStatus.SUCCEEDED) {
            failAdmission();
            closeAfterFailure();
        }
    }

    private void failAdmission() {
        recoveryGate.fail("OWNERSHIP_RENEWAL_FAILED");
        allocationReconciliationGate.ifPresent(
                gate -> gate.fail("OWNERSHIP_RENEWAL_FAILED"));
    }

    private synchronized void closeAfterFailure() {
        if (!closed) {
            closed = true;
            renewalTask.cancel(false);
            executor.shutdown();
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        renewalTask.cancel(false);
        executor.shutdown();
    }
}
