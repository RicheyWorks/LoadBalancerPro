package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationTransactionCoordinator.TransactionReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationTransactionCoordinator.TransactionStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceMutationAuthority.MutationAuthorization;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Bounded synchronous allocation reconciliation for one owned Enterprise Lab
 * loopback router. It never installs or resumes an experiment candidate.
 */
public final class EnterpriseLabAllocationReconciler {
    public static final String REPORT_SCHEMA_VERSION =
            "enterprise-lab-allocation-reconciliation-report/v1";
    public static final int HARD_MAX_EXPERIMENT_EVIDENCE = 128;

    private static final String NONE = EnterpriseLabAllocationState.NO_FINGERPRINT;
    private static final Pattern FINGERPRINT = Pattern.compile("[0-9a-f]{64}");
    private static final FailureInjector NO_FAILURE = checkpoint -> { };

    private final EnterpriseLabAllocationStateStore store;
    private final EnterpriseLabAllocationTransactionCoordinator coordinator;
    private final EnterpriseLabLoopbackAllocationRouter router;
    private final EnterpriseLabEvidenceMutationAuthority mutationAuthority;
    private final EnterpriseLabAllocationReconciliationGate gate;
    private final Clock clock;
    private final InstalledStateReader installedStateReader;
    private final FailureInjector failureInjector;

    public EnterpriseLabAllocationReconciler(
            EnterpriseLabAllocationStateStore store,
            EnterpriseLabAllocationTransactionCoordinator coordinator,
            EnterpriseLabLoopbackAllocationRouter router,
            EnterpriseLabEvidenceOwnershipGate ownershipGate,
            EnterpriseLabAllocationReconciliationGate gate) {
        this(store, coordinator, router, ownershipGate, gate, Clock.systemUTC(),
                router::installedSnapshot, NO_FAILURE);
    }

    EnterpriseLabAllocationReconciler(
            EnterpriseLabAllocationStateStore store,
            EnterpriseLabAllocationTransactionCoordinator coordinator,
            EnterpriseLabLoopbackAllocationRouter router,
            EnterpriseLabEvidenceMutationAuthority mutationAuthority,
            EnterpriseLabAllocationReconciliationGate gate,
            Clock clock,
            InstalledStateReader installedStateReader,
            FailureInjector failureInjector) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator cannot be null");
        this.router = Objects.requireNonNull(router, "router cannot be null");
        this.mutationAuthority = Objects.requireNonNull(
                mutationAuthority, "mutationAuthority cannot be null");
        this.gate = Objects.requireNonNull(gate, "gate cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.installedStateReader = Objects.requireNonNull(
                installedStateReader, "installedStateReader cannot be null");
        this.failureInjector = Objects.requireNonNull(
                failureInjector, "failureInjector cannot be null");
    }

    /** Reconciles only already-verified and replayed experiment state. */
    public ReconciliationReport reconcile(
            ReconciliationTrigger trigger,
            List<ReconstructedExperimentState> replayedExperiments) {
        List<ReconstructedExperimentState> suppliedStates = Objects.requireNonNull(
                replayedExperiments, "replayedExperiments cannot be null");
        if (suppliedStates.size() > HARD_MAX_EXPERIMENT_EVIDENCE) {
            throw new IllegalArgumentException(
                    "replayed experiment evidence exceeds the hard bound");
        }
        List<ReconstructedExperimentState> safeStates = List.copyOf(suppliedStates);
        List<ExperimentAllocationEvidence> evidence = safeStates.stream()
                .map(ExperimentAllocationEvidence::from)
                .toList();
        return reconcileEvidence(trigger, evidence);
    }

    synchronized ReconciliationReport reconcileEvidence(
            ReconciliationTrigger trigger,
            List<ExperimentAllocationEvidence> experimentEvidence) {
        ReconciliationTrigger safeTrigger = Objects.requireNonNull(
                trigger, "trigger cannot be null");
        List<ExperimentAllocationEvidence> suppliedEvidence = Objects.requireNonNull(
                experimentEvidence, "experimentEvidence cannot be null");
        if (suppliedEvidence.size() > HARD_MAX_EXPERIMENT_EVIDENCE) {
            throw new IllegalArgumentException(
                    "experiment evidence exceeds the hard bound");
        }
        List<ExperimentAllocationEvidence> safeEvidence = List.copyOf(suppliedEvidence);

        gate.begin();
        Instant startedAt = clock.instant();
        Optional<MutationAuthorization> authorization = Optional.empty();
        Optional<EnterpriseLabAllocationStateStore.ReadResult> durable = Optional.empty();
        Optional<EnterpriseLabInstalledAllocationSnapshot> installed = Optional.empty();
        DriftClassification classification = DriftClassification.RECONCILIATION_FAILED;
        ReconciliationAction action = ReconciliationAction.NONE;
        boolean trafficActionPerformed = false;
        try {
            failureInjector.checkpoint(Checkpoint.BEFORE_DURABLE_REPLAY);
            authorization = Optional.of(mutationAuthority.requireMutationAuthorization());
            durable = Optional.of(store.replay());
            failureInjector.checkpoint(Checkpoint.AFTER_DURABLE_REPLAY);

            EnterpriseLabAllocationStateStore.ReadResult initialDurable = durable.orElseThrow();
            EvidenceAssessment assessment = assessEvidence(
                    safeEvidence,
                    initialDurable.baseline(),
                    router.baselineSnapshot());
            failureInjector.checkpoint(Checkpoint.BEFORE_ROUTER_READ_BACK);
            try {
                installed = Optional.of(readInstalled());
            } catch (RuntimeException readFailure) {
                classification = DriftClassification.ROUTER_STATE_UNAVAILABLE;
                action = attemptRestorationWithoutReadBack(
                        safeTrigger, authorization.orElseThrow(), initialDurable);
                return publish(failedReport(
                        safeTrigger,
                        classification,
                        action,
                        startedAt,
                        authorization,
                        durable,
                        Optional.empty(),
                        false,
                        "ROUTER_READ_BACK_UNAVAILABLE",
                        "installed router state was unavailable and readiness remained closed"));
            }
            failureInjector.checkpoint(Checkpoint.AFTER_ROUTER_READ_BACK);

            classification = classify(
                    initialDurable,
                    installed.orElseThrow(),
                    authorization.orElseThrow(),
                    assessment);
            if (assessment == EvidenceAssessment.INCONSISTENT) {
                if (!initialDurable.records().isEmpty()) {
                    failureInjector.checkpoint(Checkpoint.BEFORE_RECONCILIATION_MUTATION);
                    TransactionReceipt receipt = coordinator.reconcileToSafeBaseline(
                            transactionId(
                                    authorization.orElseThrow(),
                                    nextAllocationGeneration(initialDurable)),
                            restorationPurpose(safeTrigger),
                            reconciliationReason(safeTrigger));
                    failureInjector.checkpoint(Checkpoint.AFTER_RECONCILIATION_MUTATION);
                    action = action(receipt);
                    trafficActionPerformed = receipt.trafficActionPerformed();
                    authorization.orElseThrow().requireSameEpoch(
                            mutationAuthority.requireMutationAuthorization());
                    durable = Optional.of(store.replay());
                    installed = Optional.of(readInstalled());
                }
                return publish(failedReport(
                        safeTrigger,
                        classification,
                        action,
                        startedAt,
                        authorization,
                        durable,
                        installed,
                        trafficActionPerformed,
                        "EXPERIMENT_ALLOCATION_EVIDENCE_MISMATCH",
                        "replayed experiment evidence disagreed with the durable allocation baseline"));
            }

            TransactionReceipt receipt = null;
            if (initialDurable.records().isEmpty()) {
                failureInjector.checkpoint(Checkpoint.BEFORE_RECONCILIATION_MUTATION);
                receipt = coordinator.establishSafeBaseline(transactionId(
                        authorization.orElseThrow(), 1L));
                failureInjector.checkpoint(Checkpoint.AFTER_RECONCILIATION_MUTATION);
                action = receipt.status() == TransactionStatus.BASELINE_COMMITTED
                        ? ReconciliationAction.BASELINE_ESTABLISHED
                        : ReconciliationAction.NONE;
            } else if (requiresSafeBaseline(
                    initialDurable,
                    installed.orElseThrow(),
                    authorization.orElseThrow(),
                    assessment)) {
                failureInjector.checkpoint(Checkpoint.BEFORE_RECONCILIATION_MUTATION);
                receipt = coordinator.reconcileToSafeBaseline(
                        transactionId(
                                authorization.orElseThrow(),
                                nextAllocationGeneration(initialDurable)),
                        restorationPurpose(safeTrigger),
                        reconciliationReason(safeTrigger));
                failureInjector.checkpoint(Checkpoint.AFTER_RECONCILIATION_MUTATION);
                action = action(receipt);
            } else {
                action = ReconciliationAction.VERIFIED_NO_OP;
            }
            if (receipt != null) {
                trafficActionPerformed = receipt.trafficActionPerformed();
            }

            authorization.orElseThrow().requireSameEpoch(
                    mutationAuthority.requireMutationAuthorization());
            durable = Optional.of(store.replay());
            installed = Optional.of(readInstalled());
            boolean ready = finalStateSafe(
                    durable.orElseThrow(),
                    installed.orElseThrow(),
                    authorization.orElseThrow(),
                    assessment);
            String reasonCode = ready
                    ? readyReasonCode(action)
                    : assessment == EvidenceAssessment.INTERRUPTED
                            ? "EXPERIMENT_RECOVERY_NOT_TERMINAL"
                            : "ALLOCATION_RECONCILIATION_NOT_SAFE";
            String reason = ready
                    ? "durable baseline, terminal transaction head, installed router state, and owner epoch match"
                    : assessment == EvidenceAssessment.INTERRUPTED
                            ? "baseline was reconciled but interrupted experiment evidence is not yet terminal"
                            : "allocation reconciliation did not produce an exact safe terminal state";
            ReconciliationReport report = report(
                    ready,
                    safeTrigger,
                    classification,
                    action,
                    startedAt,
                    authorization,
                    durable,
                    installed,
                    trafficActionPerformed,
                    reasonCode,
                    reason);
            failureInjector.checkpoint(Checkpoint.BEFORE_READINESS_PUBLICATION);
            authorization.orElseThrow().requireSameEpoch(
                    mutationAuthority.requireMutationAuthorization());
            return publish(report);
        } catch (RuntimeException failure) {
            DriftClassification failedClassification = failure instanceof
                    EnterpriseLabEvidenceOwnershipException
                    ? DriftClassification.OWNERSHIP_UNCERTAIN
                    : failure instanceof EnterpriseLabAllocationStateStore.StoreException
                            ? DriftClassification.TRANSACTION_CHAIN_INVALID
                            : classification;
            return publish(failedReport(
                    safeTrigger,
                    failedClassification,
                    action == ReconciliationAction.NONE
                            ? ReconciliationAction.FAILED_CLOSED : action,
                    startedAt,
                    authorization,
                    durable,
                    installed,
                    trafficActionPerformed,
                    "ALLOCATION_RECONCILIATION_FAILED",
                    "allocation reconciliation failed closed with preserved bounded evidence"));
        }
    }

    private ReconciliationAction attemptRestorationWithoutReadBack(
            ReconciliationTrigger trigger,
            MutationAuthorization authorization,
            EnterpriseLabAllocationStateStore.ReadResult durable) {
        if (durable.records().isEmpty()) {
            return ReconciliationAction.FAILED_CLOSED;
        }
        try {
            coordinator.reconcileToSafeBaseline(
                    transactionId(authorization, nextAllocationGeneration(durable)),
                    restorationPurpose(trigger),
                    reconciliationReason(trigger));
            return ReconciliationAction.BASELINE_RESTORATION_ATTEMPTED;
        } catch (RuntimeException ignored) {
            return ReconciliationAction.BASELINE_RESTORATION_ATTEMPTED;
        }
    }

    private ReconciliationReport publish(ReconciliationReport report) {
        gate.complete(report);
        return report;
    }

    private EnterpriseLabInstalledAllocationSnapshot readInstalled() {
        return Objects.requireNonNull(
                installedStateReader.read(), "installed state reader returned null");
    }

    private static EvidenceAssessment assessEvidence(
            List<ExperimentAllocationEvidence> evidence,
            Optional<EnterpriseLabAllocationState> durableBaseline,
            EnterpriseLabLoopbackAllocationSnapshot routerBaseline) {
        String scenarioId = durableBaseline.map(EnterpriseLabAllocationState::scenarioId)
                .orElse(routerBaseline.scenarioId());
        String baselineFingerprint = durableBaseline
                .map(state -> EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                        state.scenarioId(), state.baselineAllocation()))
                .orElseGet(() -> EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                        routerBaseline.scenarioId(), routerBaseline.allocations()));
        boolean interrupted = false;
        for (ExperimentAllocationEvidence item : evidence) {
            if (!item.scenarioId().equals(scenarioId)
                    || !item.baselineFingerprint().equals(baselineFingerprint)
                    || item.terminal()
                    && !item.lastAppliedFingerprint().equals(baselineFingerprint)) {
                return EvidenceAssessment.INCONSISTENT;
            }
            interrupted |= !item.terminal();
        }
        return interrupted ? EvidenceAssessment.INTERRUPTED : EvidenceAssessment.SAFE;
    }

    private static DriftClassification classify(
            EnterpriseLabAllocationStateStore.ReadResult durable,
            EnterpriseLabInstalledAllocationSnapshot installed,
            MutationAuthorization authorization,
            EvidenceAssessment assessment) {
        if (durable.records().isEmpty()) {
            return DriftClassification.NO_PRIOR_ALLOCATION_EVIDENCE;
        }
        EnterpriseLabAllocationState baseline = durable.baseline().orElseThrow();
        EnterpriseLabAllocationState head = durable.chainHead().orElseThrow();
        String baselineFingerprint = EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                baseline.scenarioId(), baseline.baselineAllocation());
        boolean installedBaseline = installed.routingSnapshot().scenarioId().equals(
                baseline.scenarioId())
                && installed.allocationFingerprint().equals(baselineFingerprint)
                && installed.routingSnapshot().allocations().equals(baseline.baselineAllocation());
        if (assessment == EvidenceAssessment.INCONSISTENT) {
            return DriftClassification.EXPERIMENT_EVIDENCE_MISMATCH;
        }
        if (assessment == EvidenceAssessment.INTERRUPTED) {
            return DriftClassification.INTERRUPTED_ACTIVE_EXPERIMENT;
        }
        if (installed.ownerGeneration() != authorization.generation()
                || head.ownerGeneration() != authorization.generation()) {
            return DriftClassification.STALE_OWNER_GENERATION;
        }
        if (head.transactionPhase() == TransactionPhase.PREPARED
                || head.transactionPhase() == TransactionPhase.INTENT_PERSISTED) {
            return installedBaseline
                    ? DriftClassification.UNAPPLIED_INTENT
                    : DriftClassification.PARTIAL_TRANSACTION;
        }
        if (isIncomplete(head.transactionPhase())) {
            return DriftClassification.PARTIAL_TRANSACTION;
        }
        if (head.transactionPhase() == TransactionPhase.FAILED
                || head.transactionPhase() == TransactionPhase.QUARANTINED) {
            return DriftClassification.UNSAFE_DURABLE_STATE;
        }
        if (installedBaseline
                && installed.routerGeneration() == 0L
                && !head.installedAllocation().equals(baseline.baselineAllocation())) {
            return DriftClassification.ROUTER_RESET_AFTER_RESTART;
        }
        if (!installed.routingSnapshot().allocations().keySet().equals(
                baseline.baselineAllocation().keySet())) {
            return DriftClassification.ROUTER_BACKEND_SET_DRIFT;
        }
        if (!installedBaseline) {
            if (head.transactionPhase() == TransactionPhase.COMMITTED
                    && installed.allocationFingerprint().equals(
                            head.normalizedAllocationFingerprint())) {
                return DriftClassification.COMMITTED_CANDIDATE_REQUIRES_BASELINE;
            }
            if (head.transactionPhase() == TransactionPhase.COMMITTED) {
                return DriftClassification.COMMITTED_ROUTER_DRIFT;
            }
            return DriftClassification.ROUTER_ALLOCATION_DRIFT;
        }
        return DriftClassification.SAFE_BASELINE_INSTALLED;
    }

    private static boolean requiresSafeBaseline(
            EnterpriseLabAllocationStateStore.ReadResult durable,
            EnterpriseLabInstalledAllocationSnapshot installed,
            MutationAuthorization authorization,
            EvidenceAssessment assessment) {
        if (assessment != EvidenceAssessment.SAFE) {
            return true;
        }
        EnterpriseLabAllocationState baseline = durable.baseline().orElseThrow();
        EnterpriseLabAllocationState head = durable.chainHead().orElseThrow();
        String baselineFingerprint = EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                baseline.scenarioId(), baseline.baselineAllocation());
        return isIncomplete(head.transactionPhase())
                || head.transactionPhase() == TransactionPhase.FAILED
                || head.transactionPhase() == TransactionPhase.QUARANTINED
                || !installed.routingSnapshot().scenarioId().equals(baseline.scenarioId())
                || !installed.routingSnapshot().allocations().equals(
                        baseline.baselineAllocation())
                || !installed.allocationFingerprint().equals(baselineFingerprint)
                || installed.ownerGeneration() != authorization.generation()
                || head.ownerGeneration() != authorization.generation()
                || !head.installedAllocation().equals(baseline.baselineAllocation())
                || (head.transactionPhase() == TransactionPhase.COMMITTED
                && !head.normalizedAllocationFingerprint().equals(baselineFingerprint));
    }

    private static boolean finalStateSafe(
            EnterpriseLabAllocationStateStore.ReadResult durable,
            EnterpriseLabInstalledAllocationSnapshot installed,
            MutationAuthorization authorization,
            EvidenceAssessment assessment) {
        if (assessment != EvidenceAssessment.SAFE || durable.records().isEmpty()) {
            return false;
        }
        EnterpriseLabAllocationState baseline = durable.baseline().orElseThrow();
        EnterpriseLabAllocationState head = durable.chainHead().orElseThrow();
        String baselineFingerprint = EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                baseline.scenarioId(), baseline.baselineAllocation());
        return isSafeTerminal(head.transactionPhase())
                && head.installedAllocation().equals(baseline.baselineAllocation())
                && installed.routingSnapshot().scenarioId().equals(baseline.scenarioId())
                && installed.routingSnapshot().allocations().equals(baseline.baselineAllocation())
                && installed.allocationFingerprint().equals(baselineFingerprint)
                && installed.ownerGeneration() == authorization.generation()
                && head.ownerGeneration() == authorization.generation();
    }

    private ReconciliationReport failedReport(
            ReconciliationTrigger trigger,
            DriftClassification classification,
            ReconciliationAction action,
            Instant startedAt,
            Optional<MutationAuthorization> authorization,
            Optional<EnterpriseLabAllocationStateStore.ReadResult> durable,
            Optional<EnterpriseLabInstalledAllocationSnapshot> installed,
            boolean trafficActionPerformed,
            String reasonCode,
            String reason) {
        return report(
                false,
                trigger,
                classification,
                action,
                startedAt,
                authorization,
                durable,
                installed,
                trafficActionPerformed,
                reasonCode,
                reason);
    }

    private ReconciliationReport report(
            boolean ready,
            ReconciliationTrigger trigger,
            DriftClassification classification,
            ReconciliationAction action,
            Instant startedAt,
            Optional<MutationAuthorization> authorization,
            Optional<EnterpriseLabAllocationStateStore.ReadResult> durable,
            Optional<EnterpriseLabInstalledAllocationSnapshot> installed,
            boolean trafficActionPerformed,
            String reasonCode,
            String reason) {
        EnterpriseLabAllocationStateStore.ReadResult replay = durable.orElse(
                EnterpriseLabAllocationStateStore.ReadResult.empty());
        Optional<EnterpriseLabAllocationState> baseline = replay.baseline();
        Optional<EnterpriseLabAllocationState> committed = replay.lastCommitted();
        Optional<EnterpriseLabAllocationState> head = replay.chainHead();
        return new ReconciliationReport(
                REPORT_SCHEMA_VERSION,
                ready,
                trigger,
                classification,
                action,
                startedAt,
                clock.instant(),
                authorization.map(MutationAuthorization::generation).orElse(0L),
                head.map(EnterpriseLabAllocationState::allocationGeneration).orElse(0L),
                head.map(EnterpriseLabAllocationState::transactionPhase),
                baseline.map(state -> EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                        state.scenarioId(), state.baselineAllocation())).orElse(NONE),
                committed.map(EnterpriseLabAllocationState::normalizedAllocationFingerprint).orElse(NONE),
                installed.map(EnterpriseLabInstalledAllocationSnapshot::allocationFingerprint).orElse(NONE),
                installed.map(EnterpriseLabInstalledAllocationSnapshot::routerGeneration).orElse(0L),
                replay.records().size(),
                trafficActionPerformed,
                reasonCode,
                reason);
    }

    private static ReconciliationAction action(TransactionReceipt receipt) {
        if (receipt.status() == TransactionStatus.BASELINE_COMMITTED) {
            return ReconciliationAction.BASELINE_ESTABLISHED;
        }
        if (receipt.baselineRestored()) {
            return ReconciliationAction.BASELINE_RESTORED;
        }
        if (receipt.status() == TransactionStatus.RECONCILED_BASELINE
                && receipt.durablePhase().orElse(null) == TransactionPhase.REJECTED) {
            return ReconciliationAction.INCOMPLETE_INTENT_REJECTED;
        }
        if (receipt.status() == TransactionStatus.IDEMPOTENT) {
            return ReconciliationAction.VERIFIED_NO_OP;
        }
        return receipt.baselineRestorationAttempted()
                ? ReconciliationAction.BASELINE_RESTORATION_ATTEMPTED
                : ReconciliationAction.FAILED_CLOSED;
    }

    private static String readyReasonCode(ReconciliationAction action) {
        return switch (action) {
            case BASELINE_ESTABLISHED -> "ALLOCATION_BASELINE_ESTABLISHED";
            case BASELINE_RESTORED -> "ALLOCATION_BASELINE_RESTORED";
            case INCOMPLETE_INTENT_REJECTED -> "UNAPPLIED_INTENT_REJECTED";
            default -> "ALLOCATION_RECONCILIATION_READY";
        };
    }

    private static AllocationPurpose restorationPurpose(ReconciliationTrigger trigger) {
        return switch (trigger) {
            case TAKEOVER -> AllocationPurpose.TAKEOVER_RESTORATION;
            case OPERATOR_VERIFICATION -> AllocationPurpose.OPERATOR_REQUESTED_SAFE_RESET;
            default -> AllocationPurpose.STARTUP_RESTORATION;
        };
    }

    private static String reconciliationReason(ReconciliationTrigger trigger) {
        return switch (trigger) {
            case STARTUP -> "startup allocation reconciliation required safe baseline";
            case TAKEOVER -> "takeover allocation reconciliation required safe baseline";
            case JOURNAL_RECOVERY -> "journal recovery required allocation baseline verification";
            case OWNERSHIP_UNCERTAINTY -> "ownership uncertainty required allocation baseline verification";
            case PRE_ADMISSION -> "experiment admission required allocation baseline verification";
            case OPERATOR_VERIFICATION -> "operator verification required allocation baseline verification";
            case RUNTIME_CHECKPOINT -> "runtime checkpoint detected allocation reconciliation need";
        };
    }

    private static String transactionId(
            MutationAuthorization authorization,
            long allocationGeneration) {
        return "allocation-reconcile-g" + authorization.generation()
                + "-a" + allocationGeneration;
    }

    private static long nextAllocationGeneration(
            EnterpriseLabAllocationStateStore.ReadResult durable) {
        long current = durable.chainHead().orElseThrow().allocationGeneration();
        if (current >= EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION) {
            throw new IllegalStateException(
                    "allocation generation is exhausted; reconciliation failed closed");
        }
        return current + 1L;
    }

    private static boolean isIncomplete(TransactionPhase phase) {
        return phase == TransactionPhase.PREPARED
                || phase == TransactionPhase.INTENT_PERSISTED
                || phase == TransactionPhase.APPLYING
                || phase == TransactionPhase.APPLIED
                || phase == TransactionPhase.VERIFYING
                || phase == TransactionPhase.RESTORE_REQUIRED
                || phase == TransactionPhase.RESTORING;
    }

    private static boolean isSafeTerminal(TransactionPhase phase) {
        return phase == TransactionPhase.COMMITTED
                || phase == TransactionPhase.RESTORED
                || phase == TransactionPhase.REJECTED;
    }

    public enum ReconciliationTrigger {
        STARTUP,
        TAKEOVER,
        JOURNAL_RECOVERY,
        OWNERSHIP_UNCERTAINTY,
        PRE_ADMISSION,
        OPERATOR_VERIFICATION,
        RUNTIME_CHECKPOINT
    }

    public enum DriftClassification {
        NO_PRIOR_ALLOCATION_EVIDENCE,
        SAFE_BASELINE_INSTALLED,
        UNAPPLIED_INTENT,
        PARTIAL_TRANSACTION,
        COMMITTED_CANDIDATE_REQUIRES_BASELINE,
        COMMITTED_ROUTER_DRIFT,
        ROUTER_RESET_AFTER_RESTART,
        ROUTER_BACKEND_SET_DRIFT,
        ROUTER_ALLOCATION_DRIFT,
        STALE_OWNER_GENERATION,
        INTERRUPTED_ACTIVE_EXPERIMENT,
        EXPERIMENT_EVIDENCE_MISMATCH,
        UNSAFE_DURABLE_STATE,
        ROUTER_STATE_UNAVAILABLE,
        TRANSACTION_CHAIN_INVALID,
        OWNERSHIP_UNCERTAIN,
        RECONCILIATION_FAILED
    }

    public enum ReconciliationAction {
        NONE,
        VERIFIED_NO_OP,
        BASELINE_ESTABLISHED,
        INCOMPLETE_INTENT_REJECTED,
        BASELINE_RESTORATION_ATTEMPTED,
        BASELINE_RESTORED,
        FAILED_CLOSED
    }

    public record ExperimentAllocationEvidence(
            String experimentId,
            String scenarioId,
            EnterpriseLabExperimentState lifecycleState,
            boolean terminal,
            String baselineFingerprint,
            String lastAppliedFingerprint,
            String replayFingerprint) {
        public ExperimentAllocationEvidence {
            experimentId = requireId(experimentId, "experimentId");
            scenarioId = requireId(scenarioId, "scenarioId");
            lifecycleState = Objects.requireNonNull(
                    lifecycleState, "lifecycleState cannot be null");
            baselineFingerprint = requireFingerprint(
                    baselineFingerprint, "baselineFingerprint");
            lastAppliedFingerprint = requireFingerprint(
                    lastAppliedFingerprint, "lastAppliedFingerprint");
            replayFingerprint = requireFingerprint(
                    replayFingerprint, "replayFingerprint");
            if (terminal != lifecycleState.terminal()) {
                throw new IllegalArgumentException(
                        "terminal evidence must match the replayed lifecycle state");
            }
        }

        static ExperimentAllocationEvidence from(ReconstructedExperimentState state) {
            ReconstructedExperimentState safe = Objects.requireNonNull(
                    state, "reconstructed experiment state cannot be null");
            return new ExperimentAllocationEvidence(
                    safe.experimentId(),
                    safe.scenarioId(),
                    safe.lifecycle().state(),
                    safe.lifecycle().terminal(),
                    EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(
                            safe.baselineAllocation()),
                    EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(
                            safe.lastAppliedAllocation()),
                    safe.contentFingerprint());
        }
    }

    public record ReconciliationReport(
            String schemaVersion,
            boolean ready,
            ReconciliationTrigger trigger,
            DriftClassification classification,
            ReconciliationAction action,
            Instant startedAt,
            Instant completedAt,
            long ownerGeneration,
            long allocationGeneration,
            Optional<TransactionPhase> durablePhase,
            String baselineFingerprint,
            String committedFingerprint,
            String installedFingerprint,
            long routerGeneration,
            int durableRecordCount,
            boolean trafficActionPerformed,
            String reasonCode,
            String reason) {
        public ReconciliationReport {
            if (!REPORT_SCHEMA_VERSION.equals(schemaVersion)) {
                throw new IllegalArgumentException(
                        "unsupported allocation reconciliation report schemaVersion");
            }
            trigger = Objects.requireNonNull(trigger, "trigger cannot be null");
            classification = Objects.requireNonNull(
                    classification, "classification cannot be null");
            action = Objects.requireNonNull(action, "action cannot be null");
            startedAt = Objects.requireNonNull(startedAt, "startedAt cannot be null");
            completedAt = Objects.requireNonNull(completedAt, "completedAt cannot be null");
            durablePhase = Objects.requireNonNull(
                    durablePhase, "durablePhase cannot be null");
            baselineFingerprint = requireOptionalFingerprint(
                    baselineFingerprint, "baselineFingerprint");
            committedFingerprint = requireOptionalFingerprint(
                    committedFingerprint, "committedFingerprint");
            installedFingerprint = requireOptionalFingerprint(
                    installedFingerprint, "installedFingerprint");
            reasonCode = requireCode(reasonCode);
            reason = requireReason(reason);
            if (completedAt.isBefore(startedAt)
                    || ownerGeneration < 0
                    || ownerGeneration > EnterpriseLabEvidenceOwnership.MAX_GENERATION
                    || allocationGeneration < 0
                    || allocationGeneration
                    > EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION
                    || routerGeneration < 0
                    || routerGeneration
                    > EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION
                    || durableRecordCount < 0
                    || durableRecordCount > EnterpriseLabAllocationStateStore.HARD_MAX_RECORDS) {
                throw new IllegalArgumentException(
                        "reconciliation report counters or timestamps are outside bounds");
            }
            if (ready && (!baselineFingerprint.equals(installedFingerprint)
                    || durablePhase.isEmpty())) {
                throw new IllegalArgumentException(
                        "ready report requires exact baseline read-back and a durable phase");
            }
        }
    }

    enum Checkpoint {
        BEFORE_DURABLE_REPLAY,
        AFTER_DURABLE_REPLAY,
        BEFORE_ROUTER_READ_BACK,
        AFTER_ROUTER_READ_BACK,
        BEFORE_RECONCILIATION_MUTATION,
        AFTER_RECONCILIATION_MUTATION,
        BEFORE_READINESS_PUBLICATION
    }

    @FunctionalInterface
    interface InstalledStateReader {
        EnterpriseLabInstalledAllocationSnapshot read();
    }

    @FunctionalInterface
    interface FailureInjector {
        void checkpoint(Checkpoint checkpoint);
    }

    private enum EvidenceAssessment {
        SAFE,
        INTERRUPTED,
        INCONSISTENT
    }

    private static String requireId(String value, String fieldName) {
        if (value == null || !value.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new IllegalArgumentException(
                    fieldName + " must be a bounded canonical identifier");
        }
        return value;
    }

    private static String requireFingerprint(String value, String fieldName) {
        String safe = Objects.requireNonNull(value, fieldName + " cannot be null");
        if (!FINGERPRINT.matcher(safe).matches()) {
            throw new IllegalArgumentException(fieldName + " must be canonical SHA-256");
        }
        return safe;
    }

    private static String requireOptionalFingerprint(String value, String fieldName) {
        if (NONE.equals(value)) {
            return value;
        }
        return requireFingerprint(value, fieldName);
    }

    private static String requireCode(String value) {
        if (value == null || !value.matches("[A-Z0-9][A-Z0-9_.:-]{0,63}")) {
            throw new IllegalArgumentException(
                    "reason code must be bounded canonical text");
        }
        return value;
    }

    private static String requireReason(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > 256
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "reason must be bounded sanitized plain text");
        }
        return value;
    }
}
