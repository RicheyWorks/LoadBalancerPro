package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.DriftClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.ReconciliationReport;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.ReconciliationTrigger;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationTransactionCoordinator.TransactionReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationTransactionCoordinator.TransactionStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.AllocationChangeReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.ChangeStatus;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Ownership-fenced, bounded operator supervision for the one durable
 * Enterprise Lab loopback allocation chain. It has no candidate-allocation
 * API: candidate input remains exclusively owned by the experiment lifecycle.
 */
public final class EnterpriseLabAllocationSupervisor implements AutoCloseable {
    public static final String STATUS_SCHEMA_VERSION =
            "enterprise-lab-allocation-supervision-status/v1";
    public static final int MAX_HISTORY_SUMMARIES = 16;
    public static final int MAX_RECONCILIATION_HISTORY = 32;

    private final EnterpriseLabAllocationStateStore store;
    private final EnterpriseLabExperimentTargetCatalog targetCatalog;
    private final EnterpriseLabEvidenceMutationAuthority mutationAuthority;
    private final EnterpriseLabAllocationReconciliationGate reconciliationGate;
    private final List<ReconciliationReport> reconciliationHistory = new ArrayList<>();

    private EnterpriseLabLoopbackAllocationRouter router;
    private EnterpriseLabAllocationTransactionCoordinator coordinator;
    private EnterpriseLabAllocationReconciler reconciler;
    private boolean closed;

    private EnterpriseLabAllocationSupervisor(
            EnterpriseLabAllocationStateStore store,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabEvidenceMutationAuthority mutationAuthority,
            EnterpriseLabAllocationReconciliationGate reconciliationGate) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
        this.targetCatalog = Objects.requireNonNull(
                targetCatalog, "targetCatalog cannot be null");
        this.mutationAuthority = Objects.requireNonNull(
                mutationAuthority, "mutationAuthority cannot be null");
        this.reconciliationGate = Objects.requireNonNull(
                reconciliationGate, "reconciliationGate cannot be null");
    }

    /** Creates the fixed store and completes startup reconciliation synchronously. */
    public static EnterpriseLabAllocationSupervisor create(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabLoopbackAllocationRouter startupRouter,
            EnterpriseLabEvidenceOwnershipGate ownershipGate,
            EnterpriseLabAllocationReconciliationGate reconciliationGate) {
        EnterpriseLabAllocationSupervisor supervisor = new EnterpriseLabAllocationSupervisor(
                EnterpriseLabAllocationStateStore.create(
                        trustedRoot, targetCatalog, ownershipGate),
                targetCatalog,
                ownershipGate,
                reconciliationGate);
        try {
            ReconciliationReport report = supervisor.attachRouter(
                    startupRouter, ReconciliationTrigger.STARTUP);
            if (!report.ready()) {
                throw new IllegalStateException(
                        "allocation startup reconciliation failed closed: "
                                + report.reasonCode());
            }
            return supervisor;
        } catch (RuntimeException exception) {
            supervisor.close();
            throw exception;
        }
    }

    static EnterpriseLabAllocationSupervisor createOwned(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabLoopbackAllocationRouter startupRouter,
            EnterpriseLabEvidenceMutationAuthority mutationAuthority,
            EnterpriseLabAllocationReconciliationGate reconciliationGate) {
        EnterpriseLabAllocationSupervisor supervisor = new EnterpriseLabAllocationSupervisor(
                EnterpriseLabAllocationStateStore.createOwned(
                        trustedRoot, targetCatalog, mutationAuthority),
                targetCatalog,
                mutationAuthority,
                reconciliationGate);
        try {
            ReconciliationReport report = supervisor.attachRouter(
                    startupRouter, ReconciliationTrigger.STARTUP);
            if (!report.ready()) {
                throw new IllegalStateException(
                        "allocation startup reconciliation failed closed: "
                                + report.reasonCode());
            }
            return supervisor;
        } catch (RuntimeException exception) {
            supervisor.close();
            throw exception;
        }
    }

    /**
     * Binds the exact router used by a newly armed lifecycle and reconciles it
     * before any candidate mutation can be admitted.
     */
    public synchronized ReconciliationReport attachRouter(
            EnterpriseLabLoopbackAllocationRouter attachedRouter,
            ReconciliationTrigger trigger) {
        requireOpen();
        router = Objects.requireNonNull(attachedRouter, "attachedRouter cannot be null");
        coordinator = new EnterpriseLabAllocationTransactionCoordinator(
                store,
                router,
                targetCatalog,
                mutationAuthority,
                java.time.Clock.systemUTC(),
                checkpoint -> { },
                router::installedSnapshot);
        reconciler = new EnterpriseLabAllocationReconciler(
                store,
                coordinator,
                router,
                mutationAuthority,
                reconciliationGate,
                java.time.Clock.systemUTC(),
                router::installedSnapshot,
                checkpoint -> { });
        return remember(reconciler.reconcile(
                Objects.requireNonNull(trigger, "trigger cannot be null"), List.of()));
    }

    /** Candidate intent is accepted only from the already-approved experiment decision. */
    public synchronized AllocationChangeReceipt applyCandidate(
            String transactionId,
            String experimentId,
            EnterpriseLabAdaptiveDecision decision,
            boolean explicitlyEnabled) {
        requireReady();
        EnterpriseLabLoopbackAllocationSnapshot previous = router.currentSnapshot();
        TransactionReceipt receipt = coordinator.applyCandidate(
                transactionId, experimentId, decision, explicitlyEnabled);
        EnterpriseLabLoopbackAllocationSnapshot current = router.currentSnapshot();
        ChangeStatus status = changeStatus(receipt, previous, current);
        boolean trafficAction = receipt.trafficActionPerformed();
        if (status == ChangeStatus.APPLIED || status == ChangeStatus.RESTORED) {
            trafficAction = true;
        }
        return new AllocationChangeReceipt(
                status,
                previous,
                current,
                router.baselineSnapshot(),
                trafficAction,
                trafficAction
                        ? "Enterprise Lab literal-loopback routing only"
                        : "no routing state altered",
                boundedReason(receipt.reasonCode(), receipt.reason()));
    }

    /** Replays, reads back, restores if required, and republishes readiness. */
    public synchronized ReconciliationReport verify() {
        requireAttached();
        return remember(reconciler.reconcile(
                ReconciliationTrigger.OPERATOR_VERIFICATION, List.of()));
    }

    /**
     * Explicit safe reset accepts no allocation, generation, phase, path, or
     * bypass input. The durable baseline is the only restoration authority.
     */
    public synchronized AllocationChangeReceipt restoreSafeBaseline(String reason) {
        requireAttached();
        String safeReason = boundedText(reason, "reason", 160);
        EnterpriseLabLoopbackAllocationSnapshot previous = router.currentSnapshot();
        ReconciliationReport report = remember(reconciler.reconcile(
                ReconciliationTrigger.OPERATOR_VERIFICATION, List.of()));
        EnterpriseLabLoopbackAllocationSnapshot current = router.currentSnapshot();
        boolean changed = !previous.equals(current);
        ChangeStatus status;
        if (!report.ready()) {
            status = ChangeStatus.FAILED;
        } else if (changed) {
            status = ChangeStatus.RESTORED;
        } else {
            status = ChangeStatus.NO_CHANGE;
        }
        return new AllocationChangeReceipt(
                status,
                previous,
                current,
                router.baselineSnapshot(),
                changed || report.trafficActionPerformed(),
                changed || report.trafficActionPerformed()
                        ? "Enterprise Lab literal-loopback routing only"
                        : "no routing state altered",
                boundedReason(report.reasonCode(), safeReason + ": " + report.reason()));
    }

    /** Sanitized bounded snapshot; no addresses, paths, raw allocations, or stack traces. */
    public synchronized SupervisionStatus status() {
        requireOpen();
        try {
            EnterpriseLabAllocationStateStore.ReadResult replay = store.replay();
            EnterpriseLabInstalledAllocationSnapshot installed = router == null
                    ? null : router.installedSnapshot();
            var gateStatus = reconciliationGate.admissionStatus();
            Optional<EnterpriseLabAllocationState> baseline = replay.baseline();
            Optional<EnterpriseLabAllocationState> committed = replay.lastCommitted();
            Optional<EnterpriseLabAllocationState> head = replay.chainHead();
            List<TransactionSummary> history = tail(replay.records()).stream()
                    .map(TransactionSummary::from)
                    .toList();
            int unresolved = (int) replay.records().stream()
                    .filter(value -> unresolved(value.transactionPhase()))
                    .count();
            int quarantined = (int) replay.records().stream()
                    .filter(value -> value.transactionPhase() == TransactionPhase.QUARANTINED)
                    .count();
            Optional<ReconciliationReport> last = gateStatus.report();
            String installedFingerprint = installed == null
                    ? EnterpriseLabAllocationState.NO_FINGERPRINT
                    : installed.allocationFingerprint();
            String baselineFingerprint = baseline
                    .map(value -> EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                            value.scenarioId(), value.baselineAllocation()))
                    .orElse(EnterpriseLabAllocationState.NO_FINGERPRINT);
            String committedFingerprint = committed
                    .map(EnterpriseLabAllocationState::normalizedAllocationFingerprint)
                    .orElse(EnterpriseLabAllocationState.NO_FINGERPRINT);
            boolean baselineMatchesInstalled = baselineFingerprint.equals(installedFingerprint);
            boolean committedMatchesInstalled = committedFingerprint.equals(installedFingerprint);
            DriftClassification currentClassification = last
                    .map(ReconciliationReport::classification)
                    .orElse(DriftClassification.RECONCILIATION_FAILED);
            if (!baselineMatchesInstalled) {
                currentClassification = committedMatchesInstalled
                        ? DriftClassification.COMMITTED_CANDIDATE_REQUIRES_BASELINE
                        : DriftClassification.COMMITTED_ROUTER_DRIFT;
            }
            return new SupervisionStatus(
                    STATUS_SCHEMA_VERSION,
                    true,
                    gateStatus.admissionAllowed(),
                    gateStatus.state(),
                    gateStatus.reasonCode(),
                    Optional.ofNullable(installed).map(InstalledSummary::from),
                    baseline.map(value -> DurableSummary.from("BASELINE", value)),
                    committed.map(value -> DurableSummary.from("LAST_COMMITTED", value)),
                    head.map(EnterpriseLabAllocationState::transactionPhase),
                    installed == null ? 0L : installed.routerGeneration(),
                    installed == null ? 0L : installed.ownerGeneration(),
                    head.map(EnterpriseLabAllocationState::allocationGeneration).orElse(0L),
                    new FingerprintComparison(
                            baselineFingerprint,
                            committedFingerprint,
                            installedFingerprint,
                            baselineMatchesInstalled,
                            committedMatchesInstalled,
                            head.map(value -> value.routerReadBackFingerprint()
                                            .equals(installedFingerprint))
                                    .orElse(false)),
                    currentClassification,
                    last,
                    history,
                    Math.min(unresolved, EnterpriseLabAllocationStateStore.HARD_MAX_RECORDS),
                    Math.min(quarantined, EnterpriseLabAllocationStateStore.HARD_MAX_RECORDS),
                    "sanitized fixed-target summaries only; no addresses, paths, raw allocations, or mutation inputs");
        } catch (RuntimeException exception) {
            reconciliationGate.fail("ALLOCATION_STATUS_UNAVAILABLE");
            return SupervisionStatus.failed(
                    reconciliationGate.admissionStatus(),
                    "allocation evidence could not be inspected safely");
        }
    }

    public EnterpriseLabAllocationReconciliationGate reconciliationGate() {
        return reconciliationGate;
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            store.close();
        }
    }

    private ReconciliationReport remember(ReconciliationReport report) {
        ReconciliationReport safe = Objects.requireNonNull(report, "report cannot be null");
        if (reconciliationHistory.size() == MAX_RECONCILIATION_HISTORY) {
            reconciliationHistory.remove(0);
        }
        reconciliationHistory.add(safe);
        return safe;
    }

    private void requireReady() {
        requireAttached();
        if (!reconciliationGate.admissionAllowed()) {
            throw new IllegalStateException(
                    "allocation supervision is not ready for mutation");
        }
    }

    private void requireAttached() {
        requireOpen();
        if (router == null || coordinator == null || reconciler == null) {
            throw new IllegalStateException("allocation supervisor has no attached router");
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("allocation supervisor is closed");
        }
    }

    private static ChangeStatus changeStatus(
            TransactionReceipt receipt,
            EnterpriseLabLoopbackAllocationSnapshot previous,
            EnterpriseLabLoopbackAllocationSnapshot current) {
        if (receipt.status() == TransactionStatus.COMMITTED) {
            return previous.equals(current) ? ChangeStatus.NO_CHANGE : ChangeStatus.APPLIED;
        }
        if (receipt.status() == TransactionStatus.IDEMPOTENT) {
            return ChangeStatus.NO_CHANGE;
        }
        if (receipt.baselineRestored()) {
            return previous.equals(current) ? ChangeStatus.NO_CHANGE : ChangeStatus.RESTORED;
        }
        if (receipt.trafficActionPerformed()
                || receipt.status() == TransactionStatus.FAILED_NOT_RESTORED
                || receipt.status() == TransactionStatus.DURABLE_STATE_UNCERTAIN
                || receipt.status() == TransactionStatus.OWNERSHIP_LOST) {
            return ChangeStatus.FAILED;
        }
        return ChangeStatus.DENIED;
    }

    private static List<EnterpriseLabAllocationState> tail(
            List<EnterpriseLabAllocationState> records) {
        int start = Math.max(0, records.size() - MAX_HISTORY_SUMMARIES);
        return List.copyOf(records.subList(start, records.size()));
    }

    private static boolean unresolved(TransactionPhase phase) {
        return phase == TransactionPhase.PREPARED
                || phase == TransactionPhase.INTENT_PERSISTED
                || phase == TransactionPhase.APPLYING
                || phase == TransactionPhase.APPLIED
                || phase == TransactionPhase.VERIFYING
                || phase == TransactionPhase.RESTORE_REQUIRED
                || phase == TransactionPhase.RESTORING
                || phase == TransactionPhase.FAILED
                || phase == TransactionPhase.QUARANTINED;
    }

    private static String boundedReason(String code, String reason) {
        String joined = boundedText(code, "reasonCode", 64) + ": "
                + boundedText(reason, "reason", 256);
        return joined.length() <= 256 ? joined : joined.substring(0, 256);
    }

    private static String boundedText(String value, String field, int maximum) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be null or blank");
        }
        String safe = value.trim().replace('\r', ' ').replace('\n', ' ');
        if (safe.length() > maximum) {
            safe = safe.substring(0, maximum);
        }
        return safe;
    }

    public record InstalledSummary(
            String scenarioId,
            EnterpriseLabLoopbackAllocationSnapshot.Kind kind,
            List<String> eligibleBackendIds,
            List<String> excludedBackendIds,
            String fingerprint,
            long routerGeneration,
            long ownerGeneration,
            Instant installedAt) {
        private static InstalledSummary from(EnterpriseLabInstalledAllocationSnapshot value) {
            return new InstalledSummary(
                    value.routingSnapshot().scenarioId(),
                    value.routingSnapshot().kind(),
                    value.eligibleBackendIds(),
                    value.excludedBackendIds(),
                    value.allocationFingerprint(),
                    value.routerGeneration(),
                    value.ownerGeneration(),
                    value.installedAt());
        }
    }

    public record DurableSummary(
            String summaryKind,
            String scenarioId,
            List<String> backendIds,
            String fingerprint,
            long allocationGeneration,
            long ownerGeneration,
            TransactionPhase phase,
            Instant recordedAt) {
        private static DurableSummary from(String kind, EnterpriseLabAllocationState value) {
            return new DurableSummary(
                    kind,
                    value.scenarioId(),
                    List.copyOf(value.installedAllocation().keySet()),
                    value.normalizedAllocationFingerprint(),
                    value.allocationGeneration(),
                    value.ownerGeneration(),
                    value.transactionPhase(),
                    value.createdAt());
        }
    }

    public record FingerprintComparison(
            String baselineFingerprint,
            String committedFingerprint,
            String installedFingerprint,
            boolean baselineMatchesInstalled,
            boolean committedMatchesInstalled,
            boolean durableReadBackMatchesInstalled) {
    }

    public record TransactionSummary(
            String transactionId,
            Optional<String> experimentId,
            long ownerGeneration,
            long allocationGeneration,
            TransactionPhase phase,
            EnterpriseLabAllocationState.VerificationResult verification,
            EnterpriseLabAllocationState.RecoveryClassification recovery,
            String intendedFingerprint,
            String installedFingerprint,
            String reasonCode,
            Instant recordedAt) {
        private static TransactionSummary from(EnterpriseLabAllocationState value) {
            return new TransactionSummary(
                    value.allocationTransactionId(),
                    value.experimentId(),
                    value.ownerGeneration(),
                    value.allocationGeneration(),
                    value.transactionPhase(),
                    value.verificationResult(),
                    value.recoveryClassification(),
                    value.normalizedAllocationFingerprint(),
                    value.routerReadBackFingerprint(),
                    value.transitionReason().code(),
                    value.createdAt());
        }
    }

    public record SupervisionStatus(
            String schemaVersion,
            boolean configured,
            boolean ready,
            EnterpriseLabAllocationReconciliationGate.InitializationState readinessState,
            String reasonCode,
            Optional<InstalledSummary> installed,
            Optional<DurableSummary> baseline,
            Optional<DurableSummary> committed,
            Optional<TransactionPhase> currentPhase,
            long routerGeneration,
            long ownerGeneration,
            long allocationGeneration,
            FingerprintComparison fingerprints,
            DriftClassification driftClassification,
            Optional<ReconciliationReport> lastReconciliation,
            List<TransactionSummary> history,
            int unresolvedCount,
            int quarantinedCount,
            String evidenceBoundary) {
        public SupervisionStatus {
            if (!STATUS_SCHEMA_VERSION.equals(schemaVersion)) {
                throw new IllegalArgumentException(
                        "unsupported allocation supervision status schemaVersion");
            }
            readinessState = Objects.requireNonNull(
                    readinessState, "readinessState cannot be null");
            reasonCode = boundedText(reasonCode, "reasonCode", 64);
            installed = Objects.requireNonNull(installed, "installed cannot be null");
            baseline = Objects.requireNonNull(baseline, "baseline cannot be null");
            committed = Objects.requireNonNull(committed, "committed cannot be null");
            currentPhase = Objects.requireNonNull(
                    currentPhase, "currentPhase cannot be null");
            fingerprints = Objects.requireNonNull(
                    fingerprints, "fingerprints cannot be null");
            driftClassification = Objects.requireNonNull(
                    driftClassification, "driftClassification cannot be null");
            lastReconciliation = Objects.requireNonNull(
                    lastReconciliation, "lastReconciliation cannot be null");
            history = List.copyOf(Objects.requireNonNull(history, "history cannot be null"));
            evidenceBoundary = boundedText(evidenceBoundary, "evidenceBoundary", 256);
            if (history.size() > MAX_HISTORY_SUMMARIES
                    || unresolvedCount < 0
                    || quarantinedCount < 0
                    || routerGeneration < 0
                    || ownerGeneration < 0
                    || allocationGeneration < 0
                    || ready != (readinessState
                            == EnterpriseLabAllocationReconciliationGate.InitializationState.READY)) {
                throw new IllegalArgumentException(
                        "allocation supervision status counters or readiness are inconsistent");
            }
        }

        private static SupervisionStatus failed(
                EnterpriseLabAllocationReconciliationGate.AdmissionStatus gate,
                String boundary) {
            String none = EnterpriseLabAllocationState.NO_FINGERPRINT;
            return new SupervisionStatus(
                    STATUS_SCHEMA_VERSION,
                    true,
                    false,
                    gate.state(),
                    gate.reasonCode(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    0L,
                    0L,
                    0L,
                    new FingerprintComparison(none, none, none, false, false, false),
                    DriftClassification.RECONCILIATION_FAILED,
                    gate.report(),
                    List.of(),
                    0,
                    0,
                    boundedText(boundary, "boundary", 256));
        }
    }
}
