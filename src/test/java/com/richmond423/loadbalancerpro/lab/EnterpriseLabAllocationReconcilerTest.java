package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.Checkpoint;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.DriftClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.ExperimentAllocationEvidence;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.ReconciliationAction;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.ReconciliationTrigger;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationTransactionCoordinator.TransactionStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabAllocationReconcilerTest {
    private static final String SCENARIO = "tail-latency-pressure";
    private static final Instant NOW = Instant.parse("2026-07-18T14:00:00Z");

    @TempDir
    Path temporaryDirectory;

    private List<EnterpriseLabLoopbackTarget> targets;
    private EnterpriseLabExperimentTargetCatalog targetCatalog;
    private EnterpriseLabMutationTestAuthority authority;
    private MutableClock clock;
    private EnterpriseLabAdaptiveDecision decision;
    private EnterpriseLabLoopbackAllocationRouter router;
    private EnterpriseLabAllocationStateStore store;

    @BeforeEach
    void setUp() {
        targets = targets();
        targetCatalog = new EnterpriseLabExperimentTargetCatalog(targets);
        authority = new EnterpriseLabMutationTestAuthority(temporaryDirectory);
        clock = new MutableClock(NOW);
        decision = new EnterpriseLabAdaptiveDecisionService()
                .decide(SCENARIO, "active-experiment", true, false, false);
        router = router();
        store = EnterpriseLabAllocationStateStore.createOwned(
                temporaryDirectory, targetCatalog, authority);
    }

    @Test
    void startupWithNoPriorEvidenceEstablishesExactDurableBaselineAndReadiness() {
        EnterpriseLabAllocationReconciliationGate gate =
                EnterpriseLabAllocationReconciliationGate.pending();

        var report = reconciler(coordinator(), gate).reconcileEvidence(
                ReconciliationTrigger.STARTUP, List.of());

        assertTrue(report.ready(), report.toString());
        assertEquals(DriftClassification.NO_PRIOR_ALLOCATION_EVIDENCE,
                report.classification());
        assertEquals(ReconciliationAction.BASELINE_ESTABLISHED, report.action());
        assertEquals(report.baselineFingerprint(), report.installedFingerprint());
        assertEquals(TransactionPhase.COMMITTED, report.durablePhase().orElseThrow());
        assertEquals(1, report.durableRecordCount());
        assertTrue(gate.admissionAllowed());
    }

    @Test
    void repeatedSafeReconciliationIsStableAndDoesNotAppendOrMutate() {
        EnterpriseLabAllocationReconciliationGate gate =
                EnterpriseLabAllocationReconciliationGate.pending();
        EnterpriseLabAllocationReconciler reconciler = reconciler(coordinator(), gate);
        reconciler.reconcileEvidence(ReconciliationTrigger.STARTUP, List.of());
        int records = store.replay().records().size();
        EnterpriseLabInstalledAllocationSnapshot installed = router.installedSnapshot();

        var repeated = reconciler.reconcileEvidence(
                ReconciliationTrigger.RUNTIME_CHECKPOINT, List.of());

        assertTrue(repeated.ready());
        assertEquals(DriftClassification.SAFE_BASELINE_INSTALLED,
                repeated.classification());
        assertEquals(ReconciliationAction.VERIFIED_NO_OP, repeated.action());
        assertEquals(records, store.replay().records().size());
        assertEquals(installed, router.installedSnapshot());
        assertFalse(repeated.trafficActionPerformed());
    }

    @Test
    void crashBeforeApplyRejectsUnappliedIntentWithoutRouterMutation() {
        EnterpriseLabAllocationTransactionCoordinator crashing = coordinator(
                checkpoint -> {
                    if (checkpoint
                            == EnterpriseLabAllocationTransactionCoordinator.Checkpoint
                                    .AFTER_INTENT_PERSIST) {
                        throw new SimulatedCrash();
                    }
                });
        crashing.establishSafeBaseline("allocation-baseline-1");
        assertThrows(SimulatedCrash.class, () -> crashing.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true));
        EnterpriseLabInstalledAllocationSnapshot before = router.installedSnapshot();

        var report = reconciler(
                coordinator(), EnterpriseLabAllocationReconciliationGate.pending())
                .reconcileEvidence(ReconciliationTrigger.STARTUP, List.of());

        assertTrue(report.ready());
        assertEquals(DriftClassification.UNAPPLIED_INTENT, report.classification());
        assertEquals(ReconciliationAction.INCOMPLETE_INTENT_REJECTED, report.action());
        assertEquals(before, router.installedSnapshot());
        assertEquals(List.of(
                TransactionPhase.COMMITTED,
                TransactionPhase.INTENT_PERSISTED,
                TransactionPhase.REJECTED), phases());
        int recordCount = store.replay().records().size();
        var repeated = reconciler(
                coordinator(), EnterpriseLabAllocationReconciliationGate.pending())
                .reconcileEvidence(ReconciliationTrigger.RUNTIME_CHECKPOINT, List.of());
        assertTrue(repeated.ready());
        assertEquals(ReconciliationAction.VERIFIED_NO_OP, repeated.action());
        assertEquals(recordCount, store.replay().records().size());
    }

    @Test
    void crashAfterApplyRestoresBaselineAndTerminatesIncompleteTransaction() {
        EnterpriseLabAllocationTransactionCoordinator crashing = coordinator(
                checkpoint -> {
                    if (checkpoint
                            == EnterpriseLabAllocationTransactionCoordinator.Checkpoint
                                    .AFTER_ROUTER_APPLY) {
                        throw new SimulatedCrash();
                    }
                });
        crashing.establishSafeBaseline("allocation-baseline-1");
        assertThrows(SimulatedCrash.class, () -> crashing.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true));
        assertEquals(Kind.CANDIDATE, router.installedSnapshot().routingSnapshot().kind());

        var report = reconciler(
                coordinator(), EnterpriseLabAllocationReconciliationGate.pending())
                .reconcileEvidence(ReconciliationTrigger.STARTUP, List.of());

        assertTrue(report.ready());
        assertEquals(DriftClassification.PARTIAL_TRANSACTION, report.classification());
        assertEquals(ReconciliationAction.BASELINE_RESTORED, report.action());
        assertTrue(report.trafficActionPerformed());
        assertEquals(Kind.RESTORED_BASELINE,
                router.installedSnapshot().routingSnapshot().kind());
        assertEquals(TransactionPhase.RESTORED,
                store.replay().chainHead().orElseThrow().transactionPhase());
        int recordCount = store.replay().records().size();
        var repeated = reconciler(
                coordinator(), EnterpriseLabAllocationReconciliationGate.pending())
                .reconcileEvidence(ReconciliationTrigger.RUNTIME_CHECKPOINT, List.of());
        assertTrue(repeated.ready());
        assertEquals(ReconciliationAction.VERIFIED_NO_OP, repeated.action());
        assertEquals(recordCount, store.replay().records().size());
    }

    @Test
    void committedCandidateWithoutActiveExperimentReturnsToFixedBaseline() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        assertEquals(TransactionStatus.COMMITTED, coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true).status());

        var report = reconciler(
                coordinator, EnterpriseLabAllocationReconciliationGate.pending())
                .reconcileEvidence(ReconciliationTrigger.PRE_ADMISSION, List.of());

        assertTrue(report.ready());
        assertEquals(DriftClassification.COMMITTED_CANDIDATE_REQUIRES_BASELINE,
                report.classification());
        assertEquals(ReconciliationAction.BASELINE_RESTORED, report.action());
        assertEquals(TransactionPhase.RESTORED,
                store.replay().chainHead().orElseThrow().transactionPhase());
        assertEquals(store.replay().baseline().orElseThrow().baselineAllocation(),
                router.installedSnapshot().routingSnapshot().allocations());
    }

    @Test
    void processRestartResetIsClassifiedAndDurablyReconciledWithoutCandidateResume() {
        EnterpriseLabAllocationTransactionCoordinator original = coordinator();
        original.establishSafeBaseline("allocation-baseline-1");
        original.applyCandidate("allocation-candidate-2", "experiment-1", decision, true);
        EnterpriseLabLoopbackAllocationRouter restartedRouter = router();
        EnterpriseLabAllocationTransactionCoordinator restartedCoordinator =
                new EnterpriseLabAllocationTransactionCoordinator(
                        store, restartedRouter, targetCatalog, authority, clock,
                        checkpoint -> { }, restartedRouter::installedSnapshot);
        EnterpriseLabAllocationReconciliationGate gate =
                EnterpriseLabAllocationReconciliationGate.pending();
        EnterpriseLabAllocationReconciler restarted = new EnterpriseLabAllocationReconciler(
                store, restartedCoordinator, restartedRouter, authority, gate, clock,
                restartedRouter::installedSnapshot, checkpoint -> { });

        var report = restarted.reconcileEvidence(
                ReconciliationTrigger.STARTUP, List.of());

        assertTrue(report.ready(), report.toString());
        assertEquals(DriftClassification.ROUTER_RESET_AFTER_RESTART,
                report.classification());
        assertEquals(ReconciliationAction.BASELINE_RESTORED, report.action());
        assertFalse(report.trafficActionPerformed());
        assertEquals(Kind.BASELINE, restartedRouter.installedSnapshot().routingSnapshot().kind());
        assertEquals(TransactionPhase.RESTORED,
                store.replay().chainHead().orElseThrow().transactionPhase());
    }

    @Test
    void takeoverRestampsMatchingBaselineWithNewOwnerGeneration() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        assertEquals(1L, router.installedSnapshot().ownerGeneration());
        authority.replaceOwner("replacement-owner", 2L);

        var report = reconciler(
                coordinator, EnterpriseLabAllocationReconciliationGate.pending())
                .reconcileEvidence(ReconciliationTrigger.TAKEOVER, List.of());

        assertTrue(report.ready());
        assertEquals(DriftClassification.STALE_OWNER_GENERATION,
                report.classification());
        assertEquals(ReconciliationAction.BASELINE_RESTORED, report.action());
        assertTrue(report.trafficActionPerformed());
        assertEquals(2L, router.installedSnapshot().ownerGeneration());
        assertEquals(1L, router.installedSnapshot().routerGeneration());
        assertEquals(EnterpriseLabAllocationState.AllocationPurpose.TAKEOVER_RESTORATION,
                store.replay().chainHead().orElseThrow().allocationPurpose());
    }

    @Test
    void newProcessRouterWithCurrentOwnerStillAdvancesDurableTakeoverEpoch() {
        coordinator().establishSafeBaseline("allocation-baseline-1");
        assertEquals(1L, store.replay().chainHead().orElseThrow().ownerGeneration());
        authority.replaceOwner("replacement-owner", 2L);
        EnterpriseLabLoopbackAllocationRouter replacementRouter = router();
        assertEquals(2L, replacementRouter.installedSnapshot().ownerGeneration());
        EnterpriseLabAllocationTransactionCoordinator replacementCoordinator =
                new EnterpriseLabAllocationTransactionCoordinator(
                        store,
                        replacementRouter,
                        targetCatalog,
                        authority,
                        clock,
                        checkpoint -> { },
                        replacementRouter::installedSnapshot);
        EnterpriseLabAllocationReconciliationGate gate =
                EnterpriseLabAllocationReconciliationGate.pending();
        EnterpriseLabAllocationReconciler replacementReconciler =
                new EnterpriseLabAllocationReconciler(
                        store,
                        replacementCoordinator,
                        replacementRouter,
                        authority,
                        gate,
                        clock,
                        replacementRouter::installedSnapshot,
                        checkpoint -> { });

        var report = replacementReconciler.reconcileEvidence(
                ReconciliationTrigger.TAKEOVER, List.of());

        assertTrue(report.ready(), report.toString());
        assertEquals(DriftClassification.STALE_OWNER_GENERATION,
                report.classification());
        assertEquals(ReconciliationAction.BASELINE_RESTORED, report.action());
        assertFalse(report.trafficActionPerformed());
        assertEquals(2L, store.replay().chainHead().orElseThrow().ownerGeneration());
        assertEquals(TransactionPhase.RESTORED,
                store.replay().chainHead().orElseThrow().transactionPhase());
        assertEquals(Kind.BASELINE,
                replacementRouter.installedSnapshot().routingSnapshot().kind());
    }

    @Test
    void takeoverRestoresCandidateLeftByIncompletePriorOwnerTransaction() {
        EnterpriseLabAllocationTransactionCoordinator priorOwner = coordinator(
                checkpoint -> {
                    if (checkpoint
                            == EnterpriseLabAllocationTransactionCoordinator.Checkpoint
                                    .AFTER_ROUTER_APPLY) {
                        throw new SimulatedCrash();
                    }
                });
        priorOwner.establishSafeBaseline("allocation-baseline-1");
        assertThrows(SimulatedCrash.class, () -> priorOwner.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true));
        assertEquals(Kind.CANDIDATE, router.installedSnapshot().routingSnapshot().kind());
        authority.replaceOwner("takeover-owner", 2L);

        var report = reconciler(
                coordinator(), EnterpriseLabAllocationReconciliationGate.pending())
                .reconcileEvidence(ReconciliationTrigger.TAKEOVER, List.of());

        assertTrue(report.ready());
        assertEquals(DriftClassification.STALE_OWNER_GENERATION,
                report.classification());
        assertEquals(ReconciliationAction.BASELINE_RESTORED, report.action());
        assertEquals(2L, router.installedSnapshot().ownerGeneration());
        assertEquals(TransactionPhase.RESTORED,
                store.replay().chainHead().orElseThrow().transactionPhase());
        assertTrue(store.replay().records().stream()
                .filter(record -> record.allocationGeneration() == 2L)
                .skip(2)
                .allMatch(record -> record.ownerGeneration() == 2L));
    }

    @Test
    void runtimeCandidateDriftWithoutDurableIntentIsDetectedAndRestored() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        router.applyCandidate(decision, true);
        assertEquals(Kind.CANDIDATE, router.installedSnapshot().routingSnapshot().kind());

        var report = reconciler(
                coordinator, EnterpriseLabAllocationReconciliationGate.pending())
                .reconcileEvidence(ReconciliationTrigger.RUNTIME_CHECKPOINT, List.of());

        assertTrue(report.ready());
        assertEquals(DriftClassification.COMMITTED_ROUTER_DRIFT,
                report.classification());
        assertEquals(ReconciliationAction.BASELINE_RESTORED, report.action());
        assertTrue(report.trafficActionPerformed());
        assertEquals(Kind.RESTORED_BASELINE,
                router.installedSnapshot().routingSnapshot().kind());
    }

    @Test
    void missingBackendReadBackIsClassifiedAndRestoredBeforeAdmission() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        EnterpriseLabLoopbackAllocationSnapshot missingBackend =
                new EnterpriseLabLoopbackAllocationSnapshot(
                        EnterpriseLabLoopbackAllocationSnapshot.SCHEMA_VERSION,
                        SCENARIO,
                        7L,
                        "fault-missing-backend",
                        Kind.CANDIDATE,
                        Map.of("blue", 1.0));

        var report = reconcileWithSingleDriftReadBack(
                missingBackend, "missing backend fault");

        assertTrue(report.ready(), report.toString());
        assertEquals(DriftClassification.ROUTER_BACKEND_SET_DRIFT,
                report.classification());
        assertEquals(ReconciliationAction.BASELINE_RESTORED, report.action());
        assertEquals(router.baselineSnapshot().allocations(),
                router.installedSnapshot().routingSnapshot().allocations());
        assertTrue(store.replay().records().stream().anyMatch(record ->
                record.metadata().containsKey("observed-router-fingerprint")
                        && record.metadata().get("observed-router-state")
                        .equals("invalid-scenario-or-backend-set")));
    }

    @Test
    void unexpectedBackendReadBackIsClassifiedAndRestoredBeforeAdmission() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        EnterpriseLabLoopbackAllocationSnapshot unexpectedBackend =
                new EnterpriseLabLoopbackAllocationSnapshot(
                        EnterpriseLabLoopbackAllocationSnapshot.SCHEMA_VERSION,
                        SCENARIO,
                        8L,
                        "fault-unexpected-backend",
                        Kind.CANDIDATE,
                        Map.of(
                                "blue", 0.9,
                                "green", 0.0,
                                "orange", 0.0,
                                "unknown-loopback-backend", 0.1));

        var report = reconcileWithSingleDriftReadBack(
                unexpectedBackend, "unexpected backend fault");

        assertTrue(report.ready(), report.toString());
        assertEquals(DriftClassification.ROUTER_BACKEND_SET_DRIFT,
                report.classification());
        assertEquals(ReconciliationAction.BASELINE_RESTORED, report.action());
        assertEquals(router.baselineSnapshot().allocations(),
                router.installedSnapshot().routingSnapshot().allocations());
        assertTrue(store.replay().records().stream().anyMatch(record ->
                record.metadata().containsKey("observed-router-fingerprint")
                        && record.metadata().get("observed-router-state")
                        .equals("invalid-scenario-or-backend-set")));
    }

    @Test
    void interruptedExperimentRestoresBaselineButKeepsAdmissionClosedUntilJournalTerminal() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        var candidate = coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true);
        String baselineFingerprint = store.replay().baseline().orElseThrow()
                .normalizedAllocationFingerprint();
        ExperimentAllocationEvidence active = new ExperimentAllocationEvidence(
                "experiment-1",
                SCENARIO,
                EnterpriseLabExperimentState.RUNNING,
                false,
                baselineFingerprint,
                candidate.installedFingerprint(),
                "a".repeat(64));
        EnterpriseLabAllocationReconciliationGate gate =
                EnterpriseLabAllocationReconciliationGate.pending();

        var report = reconciler(coordinator, gate).reconcileEvidence(
                ReconciliationTrigger.JOURNAL_RECOVERY, List.of(active));

        assertFalse(report.ready());
        assertEquals(DriftClassification.INTERRUPTED_ACTIVE_EXPERIMENT,
                report.classification());
        assertEquals(ReconciliationAction.BASELINE_RESTORED, report.action());
        assertFalse(gate.admissionAllowed());
        assertEquals(Kind.RESTORED_BASELINE,
                router.installedSnapshot().routingSnapshot().kind());
    }

    @Test
    void terminalJournalDisagreementFailsClosedWithoutTrustingEitherCandidate() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        String baselineFingerprint = store.replay().baseline().orElseThrow()
                .normalizedAllocationFingerprint();
        ExperimentAllocationEvidence inconsistent = new ExperimentAllocationEvidence(
                "experiment-1",
                SCENARIO,
                EnterpriseLabExperimentState.COMPLETED,
                true,
                baselineFingerprint,
                "b".repeat(64),
                "c".repeat(64));

        var report = reconciler(
                coordinator, EnterpriseLabAllocationReconciliationGate.pending())
                .reconcileEvidence(ReconciliationTrigger.STARTUP, List.of(inconsistent));

        assertFalse(report.ready());
        assertEquals(DriftClassification.EXPERIMENT_EVIDENCE_MISMATCH,
                report.classification());
        assertEquals(ReconciliationAction.VERIFIED_NO_OP, report.action());
        assertEquals(1, store.replay().records().size());
        assertEquals(Kind.BASELINE, router.installedSnapshot().routingSnapshot().kind());
    }

    @Test
    void journalDisagreementStillRestoresAnInstalledCandidateBeforeFailingClosed() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        var candidate = coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true);
        String baselineFingerprint = store.replay().baseline().orElseThrow()
                .normalizedAllocationFingerprint();
        ExperimentAllocationEvidence inconsistent = new ExperimentAllocationEvidence(
                "experiment-1",
                SCENARIO,
                EnterpriseLabExperimentState.COMPLETED,
                true,
                baselineFingerprint,
                candidate.installedFingerprint(),
                "d".repeat(64));

        var report = reconciler(
                coordinator, EnterpriseLabAllocationReconciliationGate.pending())
                .reconcileEvidence(ReconciliationTrigger.STARTUP, List.of(inconsistent));

        assertFalse(report.ready());
        assertEquals(DriftClassification.EXPERIMENT_EVIDENCE_MISMATCH,
                report.classification());
        assertEquals(ReconciliationAction.BASELINE_RESTORED, report.action());
        assertTrue(report.trafficActionPerformed());
        assertEquals(Kind.RESTORED_BASELINE,
                router.installedSnapshot().routingSnapshot().kind());
        assertEquals(TransactionPhase.RESTORED,
                store.replay().chainHead().orElseThrow().transactionPhase());
    }

    @Test
    void restorationFailurePreservesCandidateAndReadinessRemainsClosed() {
        EnterpriseLabAllocationTransactionCoordinator normal = coordinator();
        normal.establishSafeBaseline("allocation-baseline-1");
        normal.applyCandidate("allocation-candidate-2", "experiment-1", decision, true);
        EnterpriseLabAllocationTransactionCoordinator failing = coordinator(
                checkpoint -> {
                    if (checkpoint
                            == EnterpriseLabAllocationTransactionCoordinator.Checkpoint
                                    .BEFORE_BASELINE_RESTORE) {
                        throw new SimulatedCrash();
                    }
                });
        EnterpriseLabAllocationReconciliationGate gate =
                EnterpriseLabAllocationReconciliationGate.pending();

        var report = reconciler(failing, gate).reconcileEvidence(
                ReconciliationTrigger.RUNTIME_CHECKPOINT, List.of());

        assertFalse(report.ready());
        assertEquals(ReconciliationAction.FAILED_CLOSED, report.action());
        assertFalse(gate.admissionAllowed());
        assertEquals(Kind.CANDIDATE, router.installedSnapshot().routingSnapshot().kind());
        assertEquals(TransactionPhase.RESTORING,
                store.replay().chainHead().orElseThrow().transactionPhase());

        var recovered = reconciler(
                coordinator(), EnterpriseLabAllocationReconciliationGate.pending())
                .reconcileEvidence(ReconciliationTrigger.STARTUP, List.of());
        assertTrue(recovered.ready());
        assertEquals(ReconciliationAction.BASELINE_RESTORED, recovered.action());
        assertEquals(TransactionPhase.RESTORED,
                store.replay().chainHead().orElseThrow().transactionPhase());
    }

    @Test
    void unavailableReadBackAttemptsRestorationButNeverPublishesReadiness() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        coordinator.applyCandidate("allocation-candidate-2", "experiment-1", decision, true);
        EnterpriseLabAllocationReconciliationGate gate =
                EnterpriseLabAllocationReconciliationGate.pending();
        EnterpriseLabAllocationReconciler unavailable = new EnterpriseLabAllocationReconciler(
                store, coordinator, router, authority, gate, clock,
                () -> {
                    throw new IllegalStateException("injected read-back failure");
                }, checkpoint -> { });

        var report = unavailable.reconcileEvidence(
                ReconciliationTrigger.OPERATOR_VERIFICATION, List.of());

        assertFalse(report.ready());
        assertEquals(DriftClassification.ROUTER_STATE_UNAVAILABLE,
                report.classification());
        assertEquals(ReconciliationAction.BASELINE_RESTORATION_ATTEMPTED,
                report.action());
        assertFalse(gate.admissionAllowed());
        assertEquals(Kind.RESTORED_BASELINE,
                router.installedSnapshot().routingSnapshot().kind());
    }

    @Test
    void ownershipChangeBeforeReadinessPublicationFailsClosed() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        EnterpriseLabAllocationReconciliationGate gate =
                EnterpriseLabAllocationReconciliationGate.pending();
        EnterpriseLabAllocationReconciler replacing = new EnterpriseLabAllocationReconciler(
                store, coordinator, router, authority, gate, clock,
                router::installedSnapshot,
                checkpoint -> {
                    if (checkpoint == Checkpoint.BEFORE_READINESS_PUBLICATION) {
                        authority.replaceOwner("replacement-owner", 2L);
                    }
                });

        var report = replacing.reconcileEvidence(
                ReconciliationTrigger.PRE_ADMISSION, List.of());

        assertFalse(report.ready());
        assertEquals(DriftClassification.OWNERSHIP_UNCERTAIN,
                report.classification());
        assertFalse(gate.admissionAllowed());
    }

    @Test
    void corruptTransactionTailIsClassifiedWithoutRouterMutation() throws Exception {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        EnterpriseLabInstalledAllocationSnapshot before = router.installedSnapshot();
        Files.writeString(
                store.controlledStoreFile(),
                "corrupt-tail",
                StandardOpenOption.APPEND);
        EnterpriseLabAllocationReconciliationGate gate =
                EnterpriseLabAllocationReconciliationGate.pending();

        var report = reconciler(coordinator, gate).reconcileEvidence(
                ReconciliationTrigger.STARTUP, List.of());

        assertFalse(report.ready());
        assertEquals(DriftClassification.TRANSACTION_CHAIN_INVALID,
                report.classification());
        assertFalse(gate.admissionAllowed());
        assertEquals(before, router.installedSnapshot());
    }

    @Test
    void injectedOwnershipUncertaintyClosesGateWithoutDurableProgress() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        int before = store.replay().records().size();
        authority.fail(FailureClassification.LOCK_LOST);
        EnterpriseLabAllocationReconciliationGate gate =
                EnterpriseLabAllocationReconciliationGate.pending();

        var report = reconciler(coordinator, gate).reconcileEvidence(
                ReconciliationTrigger.OWNERSHIP_UNCERTAINTY, List.of());

        assertFalse(report.ready());
        assertEquals(DriftClassification.OWNERSHIP_UNCERTAIN,
                report.classification());
        authority.clearFailure();
        assertEquals(before, store.replay().records().size());
    }

    private EnterpriseLabAllocationReconciler reconciler(
            EnterpriseLabAllocationTransactionCoordinator coordinator,
            EnterpriseLabAllocationReconciliationGate gate) {
        return new EnterpriseLabAllocationReconciler(
                store, coordinator, router, authority, gate, clock,
                router::installedSnapshot, checkpoint -> { });
    }

    private EnterpriseLabAllocationReconciler.ReconciliationReport
            reconcileWithSingleDriftReadBack(
                    EnterpriseLabLoopbackAllocationSnapshot drift,
                    String reason) {
        AtomicInteger reads = new AtomicInteger();
        EnterpriseLabInstalledAllocationSnapshot driftReadBack =
                EnterpriseLabInstalledAllocationSnapshot.installed(
                        drift,
                        clock,
                        reason,
                        router.installedSnapshot().ownerGeneration());
        EnterpriseLabAllocationReconciliationGate gate =
                EnterpriseLabAllocationReconciliationGate.pending();
        EnterpriseLabAllocationTransactionCoordinator driftCoordinator =
                new EnterpriseLabAllocationTransactionCoordinator(
                        store,
                        router,
                        targetCatalog,
                        authority,
                        clock,
                        checkpoint -> { },
                        () -> reads.getAndIncrement() < 2
                                ? driftReadBack : router.installedSnapshot());
        EnterpriseLabAllocationReconciler reconciler =
                new EnterpriseLabAllocationReconciler(
                        store,
                        driftCoordinator,
                        router,
                        authority,
                        gate,
                        clock,
                        () -> reads.getAndIncrement() < 2
                                ? driftReadBack : router.installedSnapshot(),
                        checkpoint -> { });
        return reconciler.reconcileEvidence(
                ReconciliationTrigger.RUNTIME_CHECKPOINT, List.of());
    }

    private EnterpriseLabAllocationTransactionCoordinator coordinator() {
        return coordinator(checkpoint -> { });
    }

    private EnterpriseLabAllocationTransactionCoordinator coordinator(
            EnterpriseLabAllocationTransactionCoordinator.FailureInjector failureInjector) {
        return new EnterpriseLabAllocationTransactionCoordinator(
                store, router, targetCatalog, authority, clock,
                failureInjector, router::installedSnapshot);
    }

    private EnterpriseLabLoopbackAllocationRouter router() {
        List<String> backendIds = targets.stream()
                .map(EnterpriseLabLoopbackTarget::backendId)
                .toList();
        return new EnterpriseLabLoopbackAllocationRouter(
                targets,
                new EnterpriseLabLoopbackObservationIngress(
                        backendIds,
                        com.richmond423.loadbalancerpro.core.ServerObservationWindowPolicy
                                .localLabDefaults(),
                        16,
                        EnterpriseLabLoopbackObservationIngress.DEFAULT_MAX_MEASURED_LATENCY,
                        clock,
                        System::nanoTime),
                decision.decision().guardrailDecision().baselineAllocations(),
                java.util.Optional.of(authority),
                clock);
    }

    private List<TransactionPhase> phases() {
        return store.replay().records().stream()
                .map(EnterpriseLabAllocationState::transactionPhase)
                .toList();
    }

    private static List<EnterpriseLabLoopbackTarget> targets() {
        return List.of(
                new EnterpriseLabLoopbackTarget(
                        SCENARIO, "blue", URI.create("http://127.0.0.1:18081/health")),
                new EnterpriseLabLoopbackTarget(
                        SCENARIO, "green", URI.create("http://127.0.0.1:18082/health")),
                new EnterpriseLabLoopbackTarget(
                        SCENARIO, "orange", URI.create("http://[::1]:18083/health")));
    }

    private static final class SimulatedCrash extends RuntimeException {
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            current = current.plusMillis(1L);
            return current;
        }
    }
}
