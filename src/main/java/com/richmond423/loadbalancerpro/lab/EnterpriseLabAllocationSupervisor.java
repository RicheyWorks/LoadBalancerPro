package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.DriftClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.ReconciliationReport;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.ReconciliationTrigger;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationTransactionCoordinator.TransactionReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationTransactionCoordinator.TransactionStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState;
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
    private final EnterpriseLabAllocationTransactionCoordinator.FailureInjector
            coordinatorFailureInjector;
    private final List<ReconciliationReport> reconciliationHistory = new ArrayList<>();

    private EnterpriseLabLoopbackAllocationRouter router;
    private EnterpriseLabAllocationTransactionCoordinator coordinator;
    private EnterpriseLabAllocationReconciler reconciler;
    private boolean closed;

    private EnterpriseLabAllocationSupervisor(
            EnterpriseLabAllocationStateStore store,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabEvidenceMutationAuthority mutationAuthority,
            EnterpriseLabAllocationReconciliationGate reconciliationGate,
            EnterpriseLabAllocationTransactionCoordinator.FailureInjector
                    coordinatorFailureInjector) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
        this.targetCatalog = Objects.requireNonNull(
                targetCatalog, "targetCatalog cannot be null");
        this.mutationAuthority = Objects.requireNonNull(
                mutationAuthority, "mutationAuthority cannot be null");
        this.reconciliationGate = Objects.requireNonNull(
                reconciliationGate, "reconciliationGate cannot be null");
        this.coordinatorFailureInjector = Objects.requireNonNull(
                coordinatorFailureInjector,
                "coordinatorFailureInjector cannot be null");
    }

    /** Creates the fixed store and completes startup reconciliation synchronously. */
    public static EnterpriseLabAllocationSupervisor create(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabLoopbackAllocationRouter startupRouter,
            EnterpriseLabEvidenceOwnershipGate ownershipGate,
            EnterpriseLabAllocationReconciliationGate reconciliationGate) {
        return create(
                trustedRoot,
                targetCatalog,
                startupRouter,
                ownershipGate,
                reconciliationGate,
                List.of());
    }

    /** Creates the fixed store and cross-checks bounded startup journal replay. */
    public static EnterpriseLabAllocationSupervisor create(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabLoopbackAllocationRouter startupRouter,
            EnterpriseLabEvidenceOwnershipGate ownershipGate,
            EnterpriseLabAllocationReconciliationGate reconciliationGate,
            List<ReconstructedExperimentState> replayedExperiments) {
        return createForProof(
                trustedRoot,
                targetCatalog,
                startupRouter,
                ownershipGate,
                reconciliationGate,
                replayedExperiments,
                checkpoint -> { });
    }

    /** Package-local deterministic failure seam used only by subprocess proofs. */
    static EnterpriseLabAllocationSupervisor createForProof(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabLoopbackAllocationRouter startupRouter,
            EnterpriseLabEvidenceOwnershipGate ownershipGate,
            EnterpriseLabAllocationReconciliationGate reconciliationGate,
            List<ReconstructedExperimentState> replayedExperiments,
            EnterpriseLabAllocationTransactionCoordinator.FailureInjector failureInjector) {
        EnterpriseLabAllocationSupervisor supervisor = new EnterpriseLabAllocationSupervisor(
                EnterpriseLabAllocationStateStore.create(
                        trustedRoot, targetCatalog, ownershipGate),
                targetCatalog,
                ownershipGate,
                reconciliationGate,
                failureInjector);
        try {
            ReconciliationReport report = supervisor.attachRouter(
                    startupRouter,
                    ReconciliationTrigger.STARTUP,
                    replayedExperiments,
                    failureInjector);
            if (!report.ready()) {
                throw new IllegalStateException(
                        "allocation startup reconciliation failed closed: "
                                + report.reasonCode()
                                + "/" + report.classification().name()
                                + "/" + report.action().name()
                                + "/" + report.durablePhase()
                                        .map(Enum::name).orElse("NONE"));
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
                reconciliationGate,
                checkpoint -> { });
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
        return attachRouter(attachedRouter, trigger, List.of());
    }

    /** Binds one router and cross-checks only caller-supplied verified replay. */
    public synchronized ReconciliationReport attachRouter(
            EnterpriseLabLoopbackAllocationRouter attachedRouter,
            ReconciliationTrigger trigger,
            List<ReconstructedExperimentState> replayedExperiments) {
        return attachRouter(
                attachedRouter,
                trigger,
                replayedExperiments,
                coordinatorFailureInjector);
    }

    private synchronized ReconciliationReport attachRouter(
            EnterpriseLabLoopbackAllocationRouter attachedRouter,
            ReconciliationTrigger trigger,
            List<ReconstructedExperimentState> replayedExperiments,
            EnterpriseLabAllocationTransactionCoordinator.FailureInjector failureInjector) {
        requireOpen();
        router = Objects.requireNonNull(attachedRouter, "attachedRouter cannot be null");
        coordinator = new EnterpriseLabAllocationTransactionCoordinator(
                store,
                router,
                targetCatalog,
                mutationAuthority,
                java.time.Clock.systemUTC(),
                Objects.requireNonNull(
                        failureInjector, "failureInjector cannot be null"),
                router::authoritativeInstalledSnapshot);
        reconciler = new EnterpriseLabAllocationReconciler(
                store,
                coordinator,
                router,
                mutationAuthority,
                reconciliationGate,
                java.time.Clock.systemUTC(),
                router::authoritativeInstalledSnapshot,
                checkpoint -> { });
        return remember(reconciler.reconcile(
                Objects.requireNonNull(trigger, "trigger cannot be null"),
                List.copyOf(Objects.requireNonNull(
                        replayedExperiments,
                        "replayedExperiments cannot be null"))));
    }

    /** Candidate intent is accepted only from the already-approved experiment decision. */
    public synchronized AllocationChangeReceipt applyCandidate(
            String transactionId,
            String experimentId,
            EnterpriseLabAdaptiveDecision decision,
            boolean explicitlyEnabled) {
        requireReady();
        EnterpriseLabLoopbackAllocationSnapshot previous = router.currentSnapshot();
        TransactionReceipt receipt;
        try {
            receipt = coordinator.applyCandidate(
                    transactionId, experimentId, decision, explicitlyEnabled);
        } catch (RuntimeException exception) {
            reconciliationGate.fail("ALLOCATION_MUTATION_UNVERIFIED");
            throw exception;
        }
        EnterpriseLabLoopbackAllocationSnapshot current = router.currentSnapshot();
        ChangeStatus status = changeStatus(receipt, previous, current);
        boolean trafficAction = receipt.trafficActionPerformed();
        if (status == ChangeStatus.APPLIED || status == ChangeStatus.RESTORED) {
            trafficAction = true;
        }
        if (status == ChangeStatus.FAILED) {
            reconciliationGate.fail("ALLOCATION_MUTATION_UNVERIFIED");
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
                    ? null : router.authoritativeInstalledSnapshot();
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
                    "sanitized fixed-target summaries only; no addresses, paths, raw allocations, or mutation inputs",
                    Optional.empty());
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
            String evidenceBoundary,
            Optional<IndependentSupervisorSummary> independentSupervisor) {
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
            independentSupervisor = Objects.requireNonNull(
                    independentSupervisor, "independentSupervisor cannot be null");
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

        public SupervisionStatus withIndependentSupervisor(
                IndependentSupervisorSummary summary) {
            return new SupervisionStatus(
                    schemaVersion,
                    configured,
                    ready,
                    readinessState,
                    reasonCode,
                    installed,
                    baseline,
                    committed,
                    currentPhase,
                    routerGeneration,
                    ownerGeneration,
                    allocationGeneration,
                    fingerprints,
                    driftClassification,
                    lastReconciliation,
                    history,
                    unresolvedCount,
                    quarantinedCount,
                    evidenceBoundary,
                    Optional.of(Objects.requireNonNull(
                            summary, "independent supervisor summary cannot be null")));
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
                    boundedText(boundary, "boundary", 256),
                    Optional.empty());
        }
    }

    /** Sanitized operator projection of the independently running supervisor. */
    public record IndependentSupervisorSummary(
            String configuredMode,
            boolean externalSupervisorRequired,
            boolean reachable,
            boolean supervisorReady,
            Optional<String> supervisorInstanceId,
            long supervisorGeneration,
            long durableStateGeneration,
            long applicationOwnershipGeneration,
            String installedAllocationFingerprint,
            String durableIntendedFingerprint,
            String baselineFingerprint,
            long routerGeneration,
            Optional<Instant> lastSuccessfulIpcVerification,
            String lastReconciliationClassification,
            Optional<String> activeTransactionId,
            String driftStatus,
            boolean mutationReady,
            String lastSupervisorRestartClassification,
            String lastApplicationRestartReconciliation,
            String failureReasonCode,
            String boundedFailureReason) {
        public IndependentSupervisorSummary {
            if (configuredMode == null
                    || !("in-process".equals(configuredMode)
                    || "external-supervisor-required".equals(configuredMode)
                    || "disabled".equals(configuredMode))) {
                throw new IllegalArgumentException(
                        "independent supervisor configured mode is invalid");
            }
            supervisorInstanceId = Objects.requireNonNull(
                    supervisorInstanceId, "supervisorInstanceId cannot be null");
            if (supervisorInstanceId.isPresent()
                    && !supervisorInstanceId.orElseThrow()
                            .matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")) {
                throw new IllegalArgumentException(
                        "supervisorInstanceId must be a bounded canonical identifier");
            }
            if (externalSupervisorRequired
                    != "external-supervisor-required".equals(configuredMode)
                    || externalSupervisorRequired != supervisorInstanceId.isPresent()
                    || supervisorGeneration < 0L
                    || supervisorGeneration > EnterpriseLabEvidenceOwnership.MAX_GENERATION
                    || durableStateGeneration < 0L
                    || durableStateGeneration
                            > EnterpriseLabSupervisorState.HARD_MAX_DURABLE_STATE_GENERATION
                    || applicationOwnershipGeneration < 0L
                    || applicationOwnershipGeneration
                            > EnterpriseLabEvidenceOwnership.MAX_GENERATION
                    || routerGeneration < 0L
                    || routerGeneration
                            > EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION
                    || supervisorReady && (!externalSupervisorRequired || !reachable)
                    || mutationReady && (!supervisorReady || !reachable)) {
                throw new IllegalArgumentException(
                        "independent supervisor counters or readiness are inconsistent");
            }
            installedAllocationFingerprint = requireStatusFingerprint(
                    installedAllocationFingerprint, "installedAllocationFingerprint");
            durableIntendedFingerprint = requireStatusFingerprint(
                    durableIntendedFingerprint, "durableIntendedFingerprint");
            baselineFingerprint = requireStatusFingerprint(
                    baselineFingerprint, "baselineFingerprint");
            lastSuccessfulIpcVerification = Objects.requireNonNull(
                    lastSuccessfulIpcVerification,
                    "lastSuccessfulIpcVerification cannot be null");
            lastReconciliationClassification = boundedText(
                    lastReconciliationClassification,
                    "lastReconciliationClassification",
                    96);
            activeTransactionId = Objects.requireNonNull(
                    activeTransactionId, "activeTransactionId cannot be null");
            activeTransactionId.ifPresent(value -> {
                if (!value.matches("[A-Za-z0-9._:-]{1,128}")) {
                    throw new IllegalArgumentException(
                            "activeTransactionId must be bounded canonical text");
                }
            });
            driftStatus = boundedText(driftStatus, "driftStatus", 96);
            lastSupervisorRestartClassification = boundedText(
                    lastSupervisorRestartClassification,
                    "lastSupervisorRestartClassification",
                    96);
            lastApplicationRestartReconciliation = boundedText(
                    lastApplicationRestartReconciliation,
                    "lastApplicationRestartReconciliation",
                    128);
            failureReasonCode = boundedText(
                    failureReasonCode, "failureReasonCode", 64);
            boundedFailureReason = boundedText(
                    boundedFailureReason, "boundedFailureReason", 256);
        }

        private static String requireStatusFingerprint(String value, String field) {
            if (value == null
                    || !(EnterpriseLabAllocationState.NO_FINGERPRINT.equals(value)
                    || value.matches("[0-9a-f]{64}"))) {
                throw new IllegalArgumentException(
                        field + " must be NONE or canonical SHA-256");
            }
            return value;
        }
    }
}
