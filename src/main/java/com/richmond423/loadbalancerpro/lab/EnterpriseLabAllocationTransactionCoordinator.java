package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.Draft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.RecoveryClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransitionReason;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceMutationAuthority.MutationAuthorization;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.AllocationChangeReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.CandidateIntentValidation;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.ChangeStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Purpose-built single-host allocation transaction coordinator for the
 * Enterprise Lab literal-loopback router. It does not provide database or
 * distributed transaction semantics.
 */
public final class EnterpriseLabAllocationTransactionCoordinator {
    public static final String RECEIPT_SCHEMA_VERSION =
            "enterprise-lab-allocation-transaction-receipt/v1";

    private static final int MAX_ID_LENGTH = 128;
    private static final int MAX_REASON_LENGTH = 512;
    private static final Pattern CANONICAL_ID = Pattern.compile("[A-Za-z0-9._:-]+");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern UNSAFE_EVIDENCE = Pattern.compile(
            "(?i)(?:password|secret|token|api[_-]?key|credential)\\s*[:=]");
    private static final Map<String, String> EVIDENCE_METADATA = Map.of(
            "boundary", "enterprise-lab-literal-loopback-only",
            "coordinator", "crash-safe-local-v1");
    private static final FailureInjector NO_FAILURE = checkpoint -> { };

    private final EnterpriseLabAllocationStateStore store;
    private final EnterpriseLabLoopbackAllocationRouter router;
    private final EnterpriseLabExperimentTargetCatalog targetCatalog;
    private final EnterpriseLabEvidenceMutationAuthority mutationAuthority;
    private final Clock clock;
    private final FailureInjector failureInjector;
    private final InstalledStateReader installedStateReader;

    public EnterpriseLabAllocationTransactionCoordinator(
            EnterpriseLabAllocationStateStore store,
            EnterpriseLabLoopbackAllocationRouter router,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabEvidenceOwnershipGate ownershipGate) {
        this(store, router, targetCatalog, ownershipGate, Clock.systemUTC(),
                NO_FAILURE, router::installedSnapshot);
    }

    EnterpriseLabAllocationTransactionCoordinator(
            EnterpriseLabAllocationStateStore store,
            EnterpriseLabLoopbackAllocationRouter router,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabEvidenceMutationAuthority mutationAuthority,
            Clock clock,
            FailureInjector failureInjector,
            InstalledStateReader installedStateReader) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
        this.router = Objects.requireNonNull(router, "router cannot be null");
        this.targetCatalog = Objects.requireNonNull(
                targetCatalog, "targetCatalog cannot be null");
        this.mutationAuthority = Objects.requireNonNull(
                mutationAuthority, "mutationAuthority cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.failureInjector = Objects.requireNonNull(
                failureInjector, "failureInjector cannot be null");
        this.installedStateReader = Objects.requireNonNull(
                installedStateReader, "installedStateReader cannot be null");
    }

    /** Establishes the first durable baseline only after exact router read-back. */
    public synchronized TransactionReceipt establishSafeBaseline(String transactionId) {
        String safeTransactionId = requireId(transactionId, "transactionId");
        MutationAuthorization authorization = requireAuthorization();
        EnterpriseLabAllocationStateStore.ReadResult durable = store.replay();
        if (!durable.records().isEmpty()) {
            EnterpriseLabAllocationState baseline = durable.baseline().orElseThrow();
            if (baseline.allocationTransactionId().equals(safeTransactionId)
                    && baseline.transactionPhase() == TransactionPhase.COMMITTED) {
                EnterpriseLabInstalledAllocationSnapshot installed = readInstalled();
                boolean stillExact = installed.allocationFingerprint().equals(
                        baseline.normalizedAllocationFingerprint())
                        && installed.ownerGeneration() == authorization.generation();
                return receipt(
                        safeTransactionId,
                        stillExact ? TransactionStatus.IDEMPOTENT
                                : TransactionStatus.DURABLE_STATE_UNCERTAIN,
                        authorization.generation(),
                        baseline.allocationGeneration(),
                        Optional.of(baseline.transactionPhase()),
                        baseline.normalizedAllocationFingerprint(),
                        installed.allocationFingerprint(),
                        installed.routerGeneration(),
                        durable.records().size(),
                        false,
                        false,
                        false,
                        stillExact ? "BASELINE_ALREADY_COMMITTED" : "BASELINE_DRIFTED",
                        stillExact
                                ? "identical durable safe baseline transaction already exists"
                                : "durable baseline exists but current router read-back differs");
            }
            return rejected(
                    safeTransactionId,
                    authorization,
                    durable,
                    "BASELINE_ALREADY_EXISTS",
                    "the fixed allocation store already has a durable baseline");
        }

        EnterpriseLabInstalledAllocationSnapshot installed = readInstalled();
        EnterpriseLabLoopbackAllocationSnapshot baseline = router.baselineSnapshot();
        String baselineFingerprint = allocationFingerprint(
                baseline.scenarioId(), baseline.allocations());
        if (!installed.routingSnapshot().sameAllocations(baseline)
                || !installed.allocationFingerprint().equals(baselineFingerprint)
                || installed.ownerGeneration() != authorization.generation()) {
            return receipt(
                    safeTransactionId,
                    TransactionStatus.REJECTED,
                    authorization.generation(),
                    0L,
                    Optional.empty(),
                    baselineFingerprint,
                    installed.allocationFingerprint(),
                    installed.routerGeneration(),
                    0,
                    false,
                    false,
                    false,
                    "BASELINE_READ_BACK_MISMATCH",
                    "router read-back did not exactly match the owned safe baseline");
        }

        failureInjector.checkpoint(Checkpoint.BEFORE_BASELINE_COMMIT);
        requireSameAuthorization(authorization);
        TransactionContext context = TransactionContext.baseline(
                safeTransactionId, baseline, baselineFingerprint);
        append(
                context,
                TransactionPhase.COMMITTED,
                baseline.allocations(),
                baselineFingerprint,
                false,
                Optional.of(clock.instant()),
                VerificationResult.MATCHED,
                RecoveryClassification.NOT_REQUIRED,
                "BASELINE_VERIFIED",
                "safe baseline matched independent router read-back");
        failureInjector.checkpoint(Checkpoint.AFTER_BASELINE_COMMIT);
        return receipt(
                context,
                TransactionStatus.BASELINE_COMMITTED,
                installed,
                false,
                false,
                false,
                "BASELINE_COMMITTED",
                "verified safe baseline was durably committed");
    }

    /**
     * Executes one authorized candidate transaction. Success is returned only
     * after durable intent, router apply, exact read-back, and durable commit.
     */
    public synchronized TransactionReceipt applyCandidate(
            String transactionId,
            String experimentId,
            EnterpriseLabAdaptiveDecision decision,
            boolean experimentExplicitlyEnabled) {
        String safeTransactionId = requireId(transactionId, "transactionId");
        String safeExperimentId = requireId(experimentId, "experimentId");
        MutationAuthorization authorization = requireAuthorization();
        CandidateIntentValidation validation = router.validateCandidateIntent(
                decision, experimentExplicitlyEnabled);
        if (!validation.authorized()) {
            return receipt(
                    safeTransactionId,
                    TransactionStatus.REJECTED,
                    authorization.generation(),
                    0L,
                    Optional.empty(),
                    EnterpriseLabAllocationState.NO_FINGERPRINT,
                    router.installedSnapshot().allocationFingerprint(),
                    router.installedSnapshot().routerGeneration(),
                    store.replay().records().size(),
                    false,
                    false,
                    false,
                    "CANDIDATE_NOT_AUTHORIZED",
                    validation.reason());
        }

        EnterpriseLabAllocationStateStore.ReadResult durable = store.replay();
        Optional<EnterpriseLabAllocationState> replayed = durable.records().stream()
                .filter(record -> record.allocationTransactionId().equals(safeTransactionId))
                .reduce((first, second) -> second);
        if (replayed.isPresent()) {
            EnterpriseLabAllocationState existing = replayed.orElseThrow();
            if (durable.chainHead().orElseThrow().equals(existing)
                    && existing.transactionPhase() == TransactionPhase.COMMITTED
                    && existing.experimentId().equals(Optional.of(safeExperimentId))) {
                EnterpriseLabInstalledAllocationSnapshot installed = readInstalled();
                boolean stillExact = installed.allocationFingerprint().equals(
                        existing.normalizedAllocationFingerprint())
                        && installed.routingSnapshot().allocations().equals(
                                existing.installedAllocation())
                        && installed.ownerGeneration() == authorization.generation();
                return receipt(
                        safeTransactionId,
                        stillExact ? TransactionStatus.IDEMPOTENT
                                : TransactionStatus.DURABLE_STATE_UNCERTAIN,
                        authorization.generation(),
                        existing.allocationGeneration(),
                        Optional.of(existing.transactionPhase()),
                        existing.normalizedAllocationFingerprint(),
                        installed.allocationFingerprint(),
                        installed.routerGeneration(),
                        durable.records().size(),
                        false,
                        false,
                        false,
                        stillExact ? "TRANSACTION_ALREADY_COMMITTED"
                                : "COMMITTED_ROUTER_DRIFT",
                        stillExact
                                ? "identical committed transaction replay caused no allocation mutation"
                                : "committed transaction exists but current router read-back differs");
            }
            return rejected(
                    safeTransactionId,
                    authorization,
                    durable,
                    "TRANSACTION_ID_CONFLICT",
                    "transactionId already identifies incomplete or different durable evidence");
        }
        EnterpriseLabAllocationState baseline = durable.baseline().orElse(null);
        EnterpriseLabAllocationState head = durable.chainHead().orElse(null);
        if (baseline == null || head == null) {
            return rejected(
                    safeTransactionId,
                    authorization,
                    durable,
                    "BASELINE_UNAVAILABLE",
                    "candidate allocation requires a verified durable safe baseline");
        }
        if ((head.transactionPhase() != TransactionPhase.COMMITTED
                && head.transactionPhase() != TransactionPhase.RESTORED)
                || !head.installedAllocation().equals(baseline.baselineAllocation())) {
            return rejected(
                    safeTransactionId,
                    authorization,
                    durable,
                    "DURABLE_ALLOCATION_NOT_READY",
                    "durable allocation head is incomplete or does not record the safe baseline");
        }
        if (!baseline.scenarioId().equals(validation.scenarioId())
                || !baseline.baselineAllocation().equals(validation.baselineAllocation())) {
            return rejected(
                    safeTransactionId,
                    authorization,
                    durable,
                    "BASELINE_INTENT_MISMATCH",
                    "candidate guardrail baseline does not match durable baseline evidence");
        }

        EnterpriseLabInstalledAllocationSnapshot before = readInstalled();
        String baselineFingerprint = allocationFingerprint(
                validation.scenarioId(), validation.baselineAllocation());
        if (!before.allocationFingerprint().equals(baselineFingerprint)
                || before.ownerGeneration() != authorization.generation()) {
            return receipt(
                    safeTransactionId,
                    TransactionStatus.REJECTED,
                    authorization.generation(),
                    0L,
                    Optional.of(head.transactionPhase()),
                    allocationFingerprint(validation.scenarioId(), validation.approvedAllocation()),
                    before.allocationFingerprint(),
                    before.routerGeneration(),
                    durable.records().size(),
                    false,
                    false,
                    false,
                    "ROUTER_NOT_AT_SAFE_BASELINE",
                    "candidate intent was not persisted because router baseline read-back was not exact");
        }

        TransactionContext context = TransactionContext.candidate(
                safeTransactionId,
                safeExperimentId,
                head.allocationGeneration() + 1L,
                validation,
                durable.lastCommitted().orElseThrow().routerReadBackFingerprint(),
                head.currentRecordFingerprint(),
                durable.records().size());
        failureInjector.checkpoint(Checkpoint.BEFORE_INTENT_PERSIST);
        requireSameAuthorization(authorization);
        append(
                context,
                TransactionPhase.INTENT_PERSISTED,
                before.routingSnapshot().allocations(),
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                false,
                Optional.empty(),
                VerificationResult.NOT_ATTEMPTED,
                RecoveryClassification.NOT_REQUIRED,
                "INTENT_DURABLE",
                "candidate intent was forced and exactly read back before router mutation");
        failureInjector.checkpoint(Checkpoint.AFTER_INTENT_PERSIST);

        try {
            requireSameAuthorization(authorization);
            append(
                    context,
                    TransactionPhase.APPLYING,
                    before.routingSnapshot().allocations(),
                    EnterpriseLabAllocationState.NO_FINGERPRINT,
                    false,
                    Optional.empty(),
                    VerificationResult.NOT_ATTEMPTED,
                    RecoveryClassification.NOT_REQUIRED,
                    "APPLY_AUTHORIZED",
                    "live ownership was reverified immediately before router apply");
        } catch (EnterpriseLabEvidenceOwnershipException ownershipFailure) {
            return ownershipLost(context, before, false,
                    "ownership changed after durable intent and before router apply");
        }
        failureInjector.checkpoint(Checkpoint.BEFORE_ROUTER_APPLY);

        AllocationChangeReceipt applied;
        try {
            requireSameAuthorization(authorization);
            applied = router.applyCandidate(decision, experimentExplicitlyEnabled);
        } catch (EnterpriseLabEvidenceOwnershipException ownershipFailure) {
            return ownershipLost(context, before, false,
                    "ownership changed before candidate router apply completed");
        } catch (RuntimeException applyFailure) {
            return handleApplyFailure(context, authorization, before);
        }
        failureInjector.checkpoint(Checkpoint.AFTER_ROUTER_APPLY);
        if (applied.status() == ChangeStatus.DENIED) {
            try {
                requireSameAuthorization(authorization);
                append(
                        context,
                        TransactionPhase.REJECTED,
                        applied.currentSnapshot().allocations(),
                        EnterpriseLabAllocationState.NO_FINGERPRINT,
                        false,
                        Optional.empty(),
                        VerificationResult.NOT_ATTEMPTED,
                        RecoveryClassification.REJECTED,
                        "ROUTER_APPLY_REJECTED",
                        "router rejected candidate after durable intent without altering allocation");
            } catch (EnterpriseLabEvidenceOwnershipException ownershipFailure) {
                return ownershipLost(context, router.installedSnapshot(), false,
                        "ownership changed before router rejection evidence was durable");
            }
            return receipt(
                    context,
                    TransactionStatus.REJECTED,
                    readInstalled(),
                    false,
                    false,
                    false,
                    "ROUTER_APPLY_REJECTED",
                    applied.reason());
        }

        boolean actionPerformed = applied.trafficActionPerformed();
        try {
            requireSameAuthorization(authorization);
            append(
                    context,
                    TransactionPhase.APPLIED,
                    applied.currentSnapshot().allocations(),
                    EnterpriseLabAllocationState.NO_FINGERPRINT,
                    actionPerformed,
                    Optional.empty(),
                    VerificationResult.NOT_ATTEMPTED,
                    RecoveryClassification.NOT_REQUIRED,
                    "ROUTER_APPLIED",
                    "router reported a complete atomic candidate allocation result");
        } catch (EnterpriseLabEvidenceOwnershipException ownershipFailure) {
            return ownershipLost(context, router.installedSnapshot(), actionPerformed,
                    "ownership changed after router apply and before durable applied evidence");
        } catch (EnterpriseLabAllocationStateStore.StoreException durableFailure) {
            return restoreWithoutDurableProgress(context, authorization, actionPerformed,
                    "durable applied evidence could not be verified");
        }

        failureInjector.checkpoint(Checkpoint.BEFORE_READ_BACK);
        EnterpriseLabInstalledAllocationSnapshot installed;
        try {
            installed = readInstalled();
        } catch (RuntimeException readFailure) {
            return handleReadBackFailure(context, authorization, applied, actionPerformed);
        }
        failureInjector.checkpoint(Checkpoint.AFTER_READ_BACK);
        boolean matched = matchesCandidate(context, authorization, applied, installed);
        VerificationResult allocationVerification = verificationAgainstIntent(context, installed);
        Instant verifiedAt = clock.instant();
        try {
            requireSameAuthorization(authorization);
            append(
                    context,
                    TransactionPhase.VERIFYING,
                    installed.routingSnapshot().allocations(),
                    installed.allocationFingerprint(),
                    actionPerformed,
                    Optional.of(verifiedAt),
                    allocationVerification,
                    matched ? RecoveryClassification.NOT_REQUIRED
                            : RecoveryClassification.DRIFT_DETECTED,
                    matched ? "READ_BACK_MATCHED" : "READ_BACK_MISMATCH",
                    matched
                            ? "independent router read-back exactly matched durable candidate intent"
                            : "independent router read-back or owner epoch differed from durable intent");
        } catch (EnterpriseLabEvidenceOwnershipException ownershipFailure) {
            return ownershipLost(context, installed, actionPerformed,
                    "ownership changed after router read-back and before verification evidence");
        }
        if (!matched) {
            return restoreAfterMismatch(context, authorization, installed, actionPerformed,
                    allocationVerification,
                    "candidate router read-back did not match durable intent");
        }

        failureInjector.checkpoint(Checkpoint.BEFORE_COMMIT_PERSIST);
        try {
            requireSameAuthorization(authorization);
            append(
                    context,
                    TransactionPhase.COMMITTED,
                    installed.routingSnapshot().allocations(),
                    installed.allocationFingerprint(),
                    actionPerformed,
                    Optional.of(verifiedAt),
                    VerificationResult.MATCHED,
                    RecoveryClassification.NOT_REQUIRED,
                    "TRANSACTION_COMMITTED",
                    "exact candidate read-back and live ownership were durably committed");
        } catch (EnterpriseLabEvidenceOwnershipException ownershipFailure) {
            return ownershipLost(context, installed, actionPerformed,
                    "ownership changed after read-back and before durable commit");
        } catch (EnterpriseLabAllocationStateStore.StoreException durableFailure) {
            return restoreWithoutDurableProgress(context, authorization, actionPerformed,
                    "durable commit evidence could not be forced and exactly verified");
        }
        failureInjector.checkpoint(Checkpoint.AFTER_COMMIT_PERSIST);
        TransactionReceipt committed = receipt(
                context,
                TransactionStatus.COMMITTED,
                installed,
                actionPerformed,
                false,
                false,
                "TRANSACTION_COMMITTED",
                "candidate allocation succeeded only after exact durable commit read-back");
        failureInjector.checkpoint(Checkpoint.BEFORE_RESPONSE);
        return committed;
    }

    private TransactionReceipt handleApplyFailure(
            TransactionContext context,
            MutationAuthorization authorization,
            EnterpriseLabInstalledAllocationSnapshot before) {
        EnterpriseLabInstalledAllocationSnapshot observed;
        try {
            observed = readInstalled();
        } catch (RuntimeException readFailure) {
            try {
                requireSameAuthorization(authorization);
                append(
                        context,
                        TransactionPhase.FAILED,
                        before.routingSnapshot().allocations(),
                        EnterpriseLabAllocationState.NO_FINGERPRINT,
                        false,
                        Optional.empty(),
                        VerificationResult.READ_BACK_FAILED,
                        RecoveryClassification.FAILED,
                        "APPLY_STATE_UNKNOWN",
                        "router apply failed and installed state could not be independently read");
            } catch (EnterpriseLabEvidenceOwnershipException ownershipFailure) {
                return ownershipLost(context, before, false,
                        "ownership changed while failed router apply state was unavailable");
            }
            return receipt(
                    context,
                    TransactionStatus.FAILED_NOT_RESTORED,
                    before,
                    false,
                    false,
                    false,
                    "APPLY_STATE_UNKNOWN",
                    "candidate apply failed closed with unavailable router read-back");
        }
        String baselineFingerprint = allocationFingerprint(
                context.scenarioId, context.baselineAllocation);
        if (observed.allocationFingerprint().equals(baselineFingerprint)) {
            try {
                requireSameAuthorization(authorization);
                append(
                        context,
                        TransactionPhase.FAILED,
                        observed.routingSnapshot().allocations(),
                        EnterpriseLabAllocationState.NO_FINGERPRINT,
                        false,
                        Optional.empty(),
                        VerificationResult.NOT_ATTEMPTED,
                        RecoveryClassification.FAILED,
                        "APPLY_FAILED_BASELINE_RETAINED",
                        "router apply failed and independent read-back retained the safe baseline");
            } catch (EnterpriseLabEvidenceOwnershipException ownershipFailure) {
                return ownershipLost(context, observed, false,
                        "ownership changed while failed router apply retained the baseline");
            }
            return receipt(
                    context,
                    TransactionStatus.FAILED_NOT_RESTORED,
                    observed,
                    false,
                    false,
                    true,
                    "APPLY_FAILED_BASELINE_RETAINED",
                    "candidate apply failed without changing the verified baseline");
        }
        return restoreAfterMismatch(context, authorization, observed, true,
                verificationAgainstIntent(context, observed),
                "router apply failed after leaving a non-baseline installed state");
    }

    private TransactionReceipt handleReadBackFailure(
            TransactionContext context,
            MutationAuthorization authorization,
            AllocationChangeReceipt applied,
            boolean actionPerformed) {
        try {
            requireSameAuthorization(authorization);
            append(
                    context,
                    TransactionPhase.RESTORE_REQUIRED,
                    applied.currentSnapshot().allocations(),
                    EnterpriseLabAllocationState.NO_FINGERPRINT,
                    actionPerformed,
                    Optional.empty(),
                    VerificationResult.READ_BACK_FAILED,
                    RecoveryClassification.BASELINE_RESTORATION_REQUIRED,
                    "READ_BACK_UNAVAILABLE",
                    "candidate read-back failed and required verified baseline restoration");
        } catch (EnterpriseLabEvidenceOwnershipException ownershipFailure) {
            return ownershipLost(context, router.installedSnapshot(), actionPerformed,
                    "ownership changed while candidate read-back was unavailable");
        }
        return performRestoration(context, authorization, actionPerformed,
                "candidate read-back was unavailable");
    }

    private TransactionReceipt restoreAfterMismatch(
            TransactionContext context,
            MutationAuthorization authorization,
            EnterpriseLabInstalledAllocationSnapshot observed,
            boolean actionPerformed,
            VerificationResult verification,
            String reason) {
        try {
            requireSameAuthorization(authorization);
            append(
                    context,
                    TransactionPhase.RESTORE_REQUIRED,
                    observed.routingSnapshot().allocations(),
                    observed.allocationFingerprint(),
                    actionPerformed,
                    Optional.of(clock.instant()),
                    verification,
                    RecoveryClassification.BASELINE_RESTORATION_REQUIRED,
                    "RESTORE_REQUIRED",
                    "unsafe or ambiguous candidate state required safe baseline restoration");
        } catch (EnterpriseLabEvidenceOwnershipException ownershipFailure) {
            return ownershipLost(context, observed, actionPerformed,
                    "ownership changed before baseline restoration could be requested");
        }
        return performRestoration(context, authorization, actionPerformed, reason);
    }

    private TransactionReceipt performRestoration(
            TransactionContext context,
            MutationAuthorization authorization,
            boolean priorAction,
            String reason) {
        requireSameAuthorization(authorization);
        EnterpriseLabInstalledAllocationSnapshot beforeRestore = router.installedSnapshot();
        append(
                context,
                TransactionPhase.RESTORING,
                beforeRestore.routingSnapshot().allocations(),
                EnterpriseLabAllocationState.NO_FINGERPRINT,
                priorAction,
                Optional.empty(),
                VerificationResult.NOT_ATTEMPTED,
                RecoveryClassification.BASELINE_RESTORATION_REQUIRED,
                "RESTORATION_STARTED",
                "live ownership was reverified before safe baseline restoration");
        failureInjector.checkpoint(Checkpoint.BEFORE_BASELINE_RESTORE);
        AllocationChangeReceipt restored;
        try {
            requireSameAuthorization(authorization);
            restored = router.restoreBaseline("allocation transaction verification failure");
        } catch (EnterpriseLabEvidenceOwnershipException ownershipFailure) {
            return ownershipLost(context, beforeRestore, priorAction,
                    "ownership changed before baseline restoration completed");
        } catch (RuntimeException restorationFailure) {
            return appendRestorationFailure(context, authorization, beforeRestore, priorAction,
                    "baseline restoration mutation failed closed");
        }
        failureInjector.checkpoint(Checkpoint.AFTER_BASELINE_RESTORE);
        EnterpriseLabInstalledAllocationSnapshot installed;
        try {
            installed = readInstalled();
        } catch (RuntimeException readFailure) {
            return appendRestorationFailure(context, authorization, router.installedSnapshot(),
                    priorAction || restored.trafficActionPerformed(),
                    "baseline restoration read-back was unavailable");
        }
        String baselineFingerprint = allocationFingerprint(
                context.scenarioId, context.baselineAllocation);
        boolean baselineMatched = installed.allocationFingerprint().equals(baselineFingerprint)
                && installed.routingSnapshot().allocations().equals(context.baselineAllocation)
                && installed.ownerGeneration() == authorization.generation();
        boolean actionPerformed = priorAction || restored.trafficActionPerformed();
        if (!baselineMatched) {
            VerificationResult result = verificationAgainstIntent(context, installed);
            try {
                requireSameAuthorization(authorization);
                append(
                        context,
                        TransactionPhase.FAILED,
                        installed.routingSnapshot().allocations(),
                        installed.allocationFingerprint(),
                        actionPerformed,
                        Optional.of(clock.instant()),
                        result,
                        RecoveryClassification.FAILED,
                        "RESTORATION_NOT_VERIFIED",
                        "safe baseline restoration did not produce an exact owned read-back");
            } catch (EnterpriseLabEvidenceOwnershipException ownershipFailure) {
                return receipt(
                        context,
                        TransactionStatus.OWNERSHIP_LOST,
                        installed,
                        actionPerformed,
                        true,
                        false,
                        "OWNERSHIP_LOST",
                        "ownership changed before failed restoration evidence was durable");
            } catch (EnterpriseLabAllocationStateStore.StoreException durableFailure) {
                return receipt(
                        context,
                        TransactionStatus.DURABLE_STATE_UNCERTAIN,
                        installed,
                        actionPerformed,
                        true,
                        false,
                        "RESTORATION_EVIDENCE_UNCERTAIN",
                        "failed restoration evidence requires durable replay");
            }
            return receipt(
                    context,
                    TransactionStatus.FAILED_NOT_RESTORED,
                    installed,
                    actionPerformed,
                    true,
                    false,
                    "RESTORATION_NOT_VERIFIED",
                    reason + "; baseline restoration remained unverified");
        }

        try {
            requireSameAuthorization(authorization);
            append(
                    context,
                    TransactionPhase.RESTORED,
                    installed.routingSnapshot().allocations(),
                    installed.allocationFingerprint(),
                    actionPerformed,
                    Optional.of(clock.instant()),
                    verificationAgainstIntent(context, installed),
                    RecoveryClassification.BASELINE_RESTORED,
                    "BASELINE_RESTORED",
                    "safe baseline restoration was independently read back and verified");
        } catch (EnterpriseLabEvidenceOwnershipException ownershipFailure) {
            return receipt(
                    context,
                    TransactionStatus.OWNERSHIP_LOST,
                    installed,
                    actionPerformed,
                    true,
                    false,
                    "OWNERSHIP_LOST",
                    "ownership changed before verified restoration evidence was durable");
        } catch (EnterpriseLabAllocationStateStore.StoreException durableFailure) {
            return receipt(
                    context,
                    TransactionStatus.DURABLE_STATE_UNCERTAIN,
                    installed,
                    actionPerformed,
                    true,
                    true,
                    "RESTORATION_EVIDENCE_UNCERTAIN",
                    "baseline was read back but restoration evidence requires durable replay");
        }
        return receipt(
                context,
                TransactionStatus.FAILED_RESTORED,
                installed,
                actionPerformed,
                true,
                true,
                "CANDIDATE_FAILED_BASELINE_RESTORED",
                reason + "; verified safe baseline was restored");
    }

    private TransactionReceipt appendRestorationFailure(
            TransactionContext context,
            MutationAuthorization authorization,
            EnterpriseLabInstalledAllocationSnapshot installed,
            boolean actionPerformed,
            String reason) {
        boolean failureEvidenceDurable = true;
        try {
            requireSameAuthorization(authorization);
            append(
                    context,
                    TransactionPhase.FAILED,
                    installed.routingSnapshot().allocations(),
                    EnterpriseLabAllocationState.NO_FINGERPRINT,
                    actionPerformed,
                    Optional.empty(),
                    VerificationResult.READ_BACK_FAILED,
                    RecoveryClassification.FAILED,
                    "RESTORATION_FAILED",
                    "safe baseline restoration could not be verified");
        } catch (RuntimeException ignored) {
            failureEvidenceDurable = false;
        }
        return receipt(
                context,
                failureEvidenceDurable
                        ? TransactionStatus.FAILED_NOT_RESTORED
                        : TransactionStatus.DURABLE_STATE_UNCERTAIN,
                installed,
                actionPerformed,
                true,
                false,
                failureEvidenceDurable ? "RESTORATION_FAILED" : "RESTORATION_EVIDENCE_UNCERTAIN",
                failureEvidenceDurable
                        ? reason
                        : reason + "; failure evidence requires durable replay");
    }

    private TransactionReceipt restoreWithoutDurableProgress(
            TransactionContext context,
            MutationAuthorization authorization,
            boolean actionPerformed,
            String reason) {
        boolean restored = false;
        EnterpriseLabInstalledAllocationSnapshot installed = router.installedSnapshot();
        try {
            requireSameAuthorization(authorization);
            router.restoreBaseline("durable transaction evidence failure");
            installed = readInstalled();
            restored = installed.routingSnapshot().allocations().equals(context.baselineAllocation)
                    && installed.ownerGeneration() == authorization.generation();
        } catch (RuntimeException ignored) {
            restored = false;
        }
        return receipt(
                context,
                TransactionStatus.DURABLE_STATE_UNCERTAIN,
                installed,
                actionPerformed,
                true,
                restored,
                "DURABLE_STATE_UNCERTAIN",
                reason + "; transaction requires replay before any new mutation");
    }

    private TransactionReceipt ownershipLost(
            TransactionContext context,
            EnterpriseLabInstalledAllocationSnapshot installed,
            boolean actionPerformed,
            String reason) {
        return receipt(
                context,
                TransactionStatus.OWNERSHIP_LOST,
                installed,
                actionPerformed,
                false,
                false,
                "OWNERSHIP_LOST",
                reason + "; no success commit or stale-owner restoration was attempted");
    }

    private boolean matchesCandidate(
            TransactionContext context,
            MutationAuthorization authorization,
            AllocationChangeReceipt applied,
            EnterpriseLabInstalledAllocationSnapshot installed) {
        return installed.routingSnapshot().scenarioId().equals(context.scenarioId)
                && installed.routingSnapshot().allocations().equals(context.approvedAllocation)
                && installed.allocationFingerprint().equals(context.intendedFingerprint)
                && installed.ownerGeneration() == authorization.generation()
                && installed.routerGeneration() == applied.currentSnapshot().revision()
                && installed.routingSnapshot().equals(applied.currentSnapshot());
    }

    private VerificationResult verificationAgainstIntent(
            TransactionContext context,
            EnterpriseLabInstalledAllocationSnapshot installed) {
        return installed.allocationFingerprint().equals(context.intendedFingerprint)
                ? VerificationResult.MATCHED
                : VerificationResult.MISMATCHED;
    }

    private EnterpriseLabAllocationState append(
            TransactionContext context,
            TransactionPhase phase,
            Map<String, Double> installedAllocation,
            String routerFingerprint,
            boolean actionPerformed,
            Optional<Instant> lastVerifiedAt,
            VerificationResult verification,
            RecoveryClassification recovery,
            String reasonCode,
            String reason) {
        Draft draft = new Draft(
                context.transactionId,
                context.experimentId,
                context.scenarioId,
                context.allocationGeneration,
                context.purpose,
                context.baselineAllocation,
                context.requestedAllocation,
                context.approvedAllocation,
                installedAllocation,
                routerFingerprint,
                context.previousCommittedFingerprint,
                phase,
                new TransitionReason(reasonCode, reason),
                actionPerformed,
                lastVerifiedAt,
                verification,
                recovery,
                context.predecessorFingerprint,
                EVIDENCE_METADATA);
        EnterpriseLabAllocationState state = EnterpriseLabAllocationState.create(
                clock, mutationAuthority, targetCatalog, draft);
        EnterpriseLabAllocationStateStore.AppendReceipt append = store.append(state);
        context.predecessorFingerprint = append.recordFingerprint();
        context.recordCount = append.recordCount();
        context.phase = phase;
        return state;
    }

    private EnterpriseLabInstalledAllocationSnapshot readInstalled() {
        return Objects.requireNonNull(
                installedStateReader.read(), "installed state reader returned null");
    }

    private MutationAuthorization requireAuthorization() {
        return mutationAuthority.requireMutationAuthorization();
    }

    private void requireSameAuthorization(MutationAuthorization expected) {
        expected.requireSameEpoch(requireAuthorization());
    }

    private static String allocationFingerprint(
            String scenarioId,
            Map<String, Double> allocation) {
        return EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                scenarioId, allocation);
    }

    private TransactionReceipt rejected(
            String transactionId,
            MutationAuthorization authorization,
            EnterpriseLabAllocationStateStore.ReadResult durable,
            String reasonCode,
            String reason) {
        EnterpriseLabInstalledAllocationSnapshot installed = router.installedSnapshot();
        EnterpriseLabAllocationState head = durable.chainHead().orElse(null);
        return receipt(
                transactionId,
                TransactionStatus.REJECTED,
                authorization.generation(),
                head == null ? 0L : head.allocationGeneration(),
                head == null ? Optional.empty() : Optional.of(head.transactionPhase()),
                head == null ? EnterpriseLabAllocationState.NO_FINGERPRINT
                        : head.normalizedAllocationFingerprint(),
                installed.allocationFingerprint(),
                installed.routerGeneration(),
                durable.records().size(),
                false,
                false,
                false,
                reasonCode,
                reason);
    }

    private static TransactionReceipt receipt(
            TransactionContext context,
            TransactionStatus status,
            EnterpriseLabInstalledAllocationSnapshot installed,
            boolean actionPerformed,
            boolean restorationAttempted,
            boolean baselineRestored,
            String reasonCode,
            String reason) {
        return receipt(
                context.transactionId,
                status,
                installed.ownerGeneration(),
                context.allocationGeneration,
                Optional.ofNullable(context.phase),
                context.intendedFingerprint,
                installed.allocationFingerprint(),
                installed.routerGeneration(),
                context.recordCount,
                actionPerformed,
                restorationAttempted,
                baselineRestored,
                reasonCode,
                reason);
    }

    private static TransactionReceipt receipt(
            String transactionId,
            TransactionStatus status,
            long ownerGeneration,
            long allocationGeneration,
            Optional<TransactionPhase> durablePhase,
            String intendedFingerprint,
            String installedFingerprint,
            long routerGeneration,
            int durableRecordCount,
            boolean actionPerformed,
            boolean restorationAttempted,
            boolean baselineRestored,
            String reasonCode,
            String reason) {
        return new TransactionReceipt(
                RECEIPT_SCHEMA_VERSION,
                transactionId,
                status,
                ownerGeneration,
                allocationGeneration,
                durablePhase,
                intendedFingerprint,
                installedFingerprint,
                routerGeneration,
                durableRecordCount,
                actionPerformed,
                restorationAttempted,
                baselineRestored,
                reasonCode,
                reason);
    }

    private static String requireId(String value, String fieldName) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > MAX_ID_LENGTH || !CANONICAL_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a bounded canonical identifier");
        }
        return value;
    }

    private static String requireFingerprint(String value, String fieldName) {
        if (EnterpriseLabAllocationState.NO_FINGERPRINT.equals(value)
                || (value != null && SHA_256.matcher(value).matches())) {
            return value;
        }
        throw new IllegalArgumentException(fieldName + " must be NONE or canonical SHA-256");
    }

    private static String requireReason(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("reason cannot be null or blank");
        }
        String safe = value.trim().replace('\r', ' ').replace('\n', ' ');
        if (safe.length() > MAX_REASON_LENGTH || UNSAFE_EVIDENCE.matcher(safe).find()) {
            throw new IllegalArgumentException(
                    "reason must be bounded sanitized plain text");
        }
        return safe;
    }

    public enum TransactionStatus {
        BASELINE_COMMITTED,
        COMMITTED,
        IDEMPOTENT,
        REJECTED,
        OWNERSHIP_LOST,
        FAILED_RESTORED,
        FAILED_NOT_RESTORED,
        DURABLE_STATE_UNCERTAIN
    }

    public record TransactionReceipt(
            String schemaVersion,
            String transactionId,
            TransactionStatus status,
            long ownerGeneration,
            long allocationGeneration,
            Optional<TransactionPhase> durablePhase,
            String intendedFingerprint,
            String installedFingerprint,
            long routerGeneration,
            int durableRecordCount,
            boolean trafficActionPerformed,
            boolean baselineRestorationAttempted,
            boolean baselineRestored,
            String reasonCode,
            String reason) {
        public TransactionReceipt {
            if (!RECEIPT_SCHEMA_VERSION.equals(schemaVersion)) {
                throw new IllegalArgumentException("unsupported transaction receipt schemaVersion");
            }
            transactionId = requireId(transactionId, "transactionId");
            status = Objects.requireNonNull(status, "status cannot be null");
            if (ownerGeneration < EnterpriseLabInstalledAllocationSnapshot.UNOWNED_GENERATION
                    || ownerGeneration > EnterpriseLabEvidenceOwnership.MAX_GENERATION
                    || allocationGeneration < 0
                    || allocationGeneration > EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION
                    || routerGeneration < 0
                    || routerGeneration > EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION
                    || durableRecordCount < 0
                    || durableRecordCount > EnterpriseLabAllocationStateStore.HARD_MAX_RECORDS) {
                throw new IllegalArgumentException("transaction receipt counters are outside hard bounds");
            }
            durablePhase = Objects.requireNonNull(
                    durablePhase, "durablePhase cannot be null");
            intendedFingerprint = requireFingerprint(
                    intendedFingerprint, "intendedFingerprint");
            installedFingerprint = requireFingerprint(
                    installedFingerprint, "installedFingerprint");
            reasonCode = requireId(reasonCode, "reasonCode");
            reason = requireReason(reason);
            if (baselineRestored && !baselineRestorationAttempted) {
                throw new IllegalArgumentException(
                        "baselineRestored requires a restoration attempt");
            }
            if (status == TransactionStatus.COMMITTED
                    && (!intendedFingerprint.equals(installedFingerprint)
                    || durablePhase.orElse(null) != TransactionPhase.COMMITTED
                    || baselineRestorationAttempted)) {
                throw new IllegalArgumentException(
                        "committed receipt requires exact durable and installed agreement");
            }
        }
    }

    enum Checkpoint {
        BEFORE_BASELINE_COMMIT,
        AFTER_BASELINE_COMMIT,
        BEFORE_INTENT_PERSIST,
        AFTER_INTENT_PERSIST,
        BEFORE_ROUTER_APPLY,
        AFTER_ROUTER_APPLY,
        BEFORE_READ_BACK,
        AFTER_READ_BACK,
        BEFORE_COMMIT_PERSIST,
        AFTER_COMMIT_PERSIST,
        BEFORE_RESPONSE,
        BEFORE_BASELINE_RESTORE,
        AFTER_BASELINE_RESTORE
    }

    @FunctionalInterface
    interface FailureInjector {
        void checkpoint(Checkpoint checkpoint);
    }

    @FunctionalInterface
    interface InstalledStateReader {
        EnterpriseLabInstalledAllocationSnapshot read();
    }

    private static final class TransactionContext {
        private final String transactionId;
        private final Optional<String> experimentId;
        private final String scenarioId;
        private final long allocationGeneration;
        private final AllocationPurpose purpose;
        private final Map<String, Double> baselineAllocation;
        private final Map<String, Double> requestedAllocation;
        private final Map<String, Double> approvedAllocation;
        private final String intendedFingerprint;
        private final String previousCommittedFingerprint;
        private String predecessorFingerprint;
        private int recordCount;
        private TransactionPhase phase;

        private TransactionContext(
                String transactionId,
                Optional<String> experimentId,
                String scenarioId,
                long allocationGeneration,
                AllocationPurpose purpose,
                Map<String, Double> baselineAllocation,
                Map<String, Double> requestedAllocation,
                Map<String, Double> approvedAllocation,
                String intendedFingerprint,
                String previousCommittedFingerprint,
                String predecessorFingerprint,
                int recordCount) {
            this.transactionId = transactionId;
            this.experimentId = experimentId;
            this.scenarioId = scenarioId;
            this.allocationGeneration = allocationGeneration;
            this.purpose = purpose;
            this.baselineAllocation = Map.copyOf(baselineAllocation);
            this.requestedAllocation = Map.copyOf(requestedAllocation);
            this.approvedAllocation = Map.copyOf(approvedAllocation);
            this.intendedFingerprint = intendedFingerprint;
            this.previousCommittedFingerprint = previousCommittedFingerprint;
            this.predecessorFingerprint = predecessorFingerprint;
            this.recordCount = recordCount;
        }

        private static TransactionContext baseline(
                String transactionId,
                EnterpriseLabLoopbackAllocationSnapshot baseline,
                String fingerprint) {
            return new TransactionContext(
                    transactionId,
                    Optional.empty(),
                    baseline.scenarioId(),
                    1L,
                    AllocationPurpose.INITIAL_SAFE_BASELINE,
                    baseline.allocations(),
                    baseline.allocations(),
                    baseline.allocations(),
                    fingerprint,
                    EnterpriseLabAllocationState.NO_FINGERPRINT,
                    EnterpriseLabAllocationState.GENESIS_FINGERPRINT,
                    0);
        }

        private static TransactionContext candidate(
                String transactionId,
                String experimentId,
                long allocationGeneration,
                CandidateIntentValidation validation,
                String previousCommittedFingerprint,
                String predecessorFingerprint,
                int recordCount) {
            return new TransactionContext(
                    transactionId,
                    Optional.of(experimentId),
                    validation.scenarioId(),
                    allocationGeneration,
                    AllocationPurpose.EXPERIMENT_CANDIDATE,
                    validation.baselineAllocation(),
                    validation.requestedAllocation(),
                    validation.approvedAllocation(),
                    allocationFingerprint(
                            validation.scenarioId(), validation.approvedAllocation()),
                    previousCommittedFingerprint,
                    predecessorFingerprint,
                    recordCount);
        }
    }
}
