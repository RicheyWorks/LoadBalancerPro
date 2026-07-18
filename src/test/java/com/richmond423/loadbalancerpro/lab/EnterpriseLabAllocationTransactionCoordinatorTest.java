package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationTransactionCoordinator.Checkpoint;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationTransactionCoordinator.TransactionReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationTransactionCoordinator.TransactionStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabAllocationTransactionCoordinatorTest {
    private static final String SCENARIO = "tail-latency-pressure";
    private static final Instant NOW = Instant.parse("2026-07-18T13:00:00Z");

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
    void normalTransactionPersistsIntentBeforeApplyAndCommitsOnlyAfterExactReadBack() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();

        TransactionReceipt baseline = coordinator.establishSafeBaseline("allocation-baseline-1");
        clock.advanceSeconds(1);
        TransactionReceipt committed = coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true);

        assertEquals(TransactionStatus.BASELINE_COMMITTED, baseline.status());
        assertEquals(TransactionStatus.COMMITTED, committed.status());
        assertEquals(TransactionPhase.COMMITTED, committed.durablePhase().orElseThrow());
        assertEquals(committed.intendedFingerprint(), committed.installedFingerprint());
        assertEquals(1L, committed.ownerGeneration());
        assertEquals(2L, committed.allocationGeneration());
        assertEquals(1L, committed.routerGeneration());
        assertEquals(6, committed.durableRecordCount());
        assertTrue(committed.trafficActionPerformed());
        assertFalse(committed.baselineRestorationAttempted());
        assertFalse(committed.baselineRestored());
        assertEquals(Kind.CANDIDATE, router.installedSnapshot().routingSnapshot().kind());

        List<EnterpriseLabAllocationState> records = store.replay().records();
        assertEquals(List.of(
                TransactionPhase.COMMITTED,
                TransactionPhase.INTENT_PERSISTED,
                TransactionPhase.APPLYING,
                TransactionPhase.APPLIED,
                TransactionPhase.VERIFYING,
                TransactionPhase.COMMITTED), phases(records));
        assertFalse(records.get(1).actionPerformed());
        assertFalse(records.get(2).actionPerformed());
        assertTrue(records.get(3).actionPerformed());
        assertEquals(EnterpriseLabAllocationState.VerificationResult.MATCHED,
                records.get(4).verificationResult());
        assertEquals(records.get(4).routerReadBackFingerprint(),
                records.get(5).routerReadBackFingerprint());
        assertTrue(records.stream().allMatch(record -> record.ownerGeneration() == 1L));
    }

    @Test
    void unauthorizedCandidateIsRejectedBeforeDurableIntentOrRouterMutation() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        EnterpriseLabInstalledAllocationSnapshot before = router.installedSnapshot();

        TransactionReceipt rejected = coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, false);

        assertEquals(TransactionStatus.REJECTED, rejected.status());
        assertEquals("CANDIDATE_NOT_AUTHORIZED", rejected.reasonCode());
        assertEquals(1, store.replay().records().size());
        assertEquals(before, router.installedSnapshot());
        assertFalse(rejected.trafficActionPerformed());
    }

    @Test
    void transactionReceiptRejectsUnsafeEvidenceText() {
        TransactionReceipt baseline = coordinator().establishSafeBaseline("allocation-baseline-1");

        assertThrows(IllegalArgumentException.class, () -> new TransactionReceipt(
                baseline.schemaVersion(),
                baseline.transactionId(),
                baseline.status(),
                baseline.ownerGeneration(),
                baseline.allocationGeneration(),
                baseline.durablePhase(),
                baseline.intendedFingerprint(),
                baseline.installedFingerprint(),
                baseline.routerGeneration(),
                baseline.durableRecordCount(),
                baseline.trafficActionPerformed(),
                baseline.baselineRestorationAttempted(),
                baseline.baselineRestored(),
                baseline.reasonCode(),
                "api" + "_key=do-not-record"));
    }

    @Test
    void crashAfterDurableIntentLeavesBaselineInstalledAndNoCommit() {
        AtomicReference<Checkpoint> crashAt = new AtomicReference<>(Checkpoint.AFTER_INTENT_PERSIST);
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator(
                checkpoint -> {
                    if (checkpoint == crashAt.get()) {
                        throw new SimulatedCrash(checkpoint);
                    }
                }, router::installedSnapshot);
        coordinator.establishSafeBaseline("allocation-baseline-1");

        SimulatedCrash crash = assertThrows(SimulatedCrash.class, () -> coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true));

        assertEquals(Checkpoint.AFTER_INTENT_PERSIST, crash.checkpoint);
        assertEquals(Kind.BASELINE, router.installedSnapshot().routingSnapshot().kind());
        assertEquals(List.of(TransactionPhase.COMMITTED, TransactionPhase.INTENT_PERSISTED),
                phases(store.replay().records()));
    }

    @Test
    void crashAfterApplyLeavesApplyingIntentAndObservableCandidateWithoutCommit() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator(
                checkpoint -> {
                    if (checkpoint == Checkpoint.AFTER_ROUTER_APPLY) {
                        throw new SimulatedCrash(checkpoint);
                    }
                }, router::installedSnapshot);
        coordinator.establishSafeBaseline("allocation-baseline-1");

        assertThrows(SimulatedCrash.class, () -> coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true));

        assertEquals(Kind.CANDIDATE, router.installedSnapshot().routingSnapshot().kind());
        assertEquals(List.of(
                TransactionPhase.COMMITTED,
                TransactionPhase.INTENT_PERSISTED,
                TransactionPhase.APPLYING), phases(store.replay().records()));
        assertTrue(store.replay().records().stream()
                .noneMatch(record -> record.transactionPhase() == TransactionPhase.COMMITTED
                        && record.allocationGeneration() == 2L));
    }

    @Test
    void responseLossAfterCommitReplaysIdempotentlyWithoutDuplicateMutationOrRecord() {
        EnterpriseLabAllocationTransactionCoordinator crashing = coordinator(
                checkpoint -> {
                    if (checkpoint == Checkpoint.BEFORE_RESPONSE) {
                        throw new SimulatedCrash(checkpoint);
                    }
                }, router::installedSnapshot);
        crashing.establishSafeBaseline("allocation-baseline-1");
        assertThrows(SimulatedCrash.class, () -> crashing.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true));
        EnterpriseLabInstalledAllocationSnapshot committedInstalled = router.installedSnapshot();
        int recordCount = store.replay().records().size();

        TransactionReceipt replay = coordinator().applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true);

        assertEquals(TransactionStatus.IDEMPOTENT, replay.status());
        assertEquals("TRANSACTION_ALREADY_COMMITTED", replay.reasonCode());
        assertEquals(recordCount, store.replay().records().size());
        assertEquals(committedInstalled, router.installedSnapshot());
        assertFalse(replay.trafficActionPerformed());
    }

    @Test
    void committedReplayDetectsRouterDriftInsteadOfClaimingIdempotentSuccess() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        TransactionReceipt committed = coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true);
        assertEquals(TransactionStatus.COMMITTED, committed.status());
        int recordCount = store.replay().records().size();
        router.restoreBaseline("controlled drift injection");

        TransactionReceipt drift = coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true);

        assertEquals(TransactionStatus.DURABLE_STATE_UNCERTAIN, drift.status());
        assertEquals("COMMITTED_ROUTER_DRIFT", drift.reasonCode());
        assertEquals(recordCount, store.replay().records().size());
        assertFalse(drift.trafficActionPerformed());
    }

    @Test
    void durableCommitFailureRestoresBaselineAndReturnsUncertainWithoutSuccess() {
        store.close();
        store = EnterpriseLabAllocationStateStore.createForTesting(
                temporaryDirectory,
                targetCatalog,
                EnterpriseLabAllocationStateStore.HARD_MAX_STORE_BYTES,
                5,
                authority,
                (checkpoint, bytesWritten) -> { });
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");

        TransactionReceipt receipt = coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true);

        assertEquals(TransactionStatus.DURABLE_STATE_UNCERTAIN, receipt.status());
        assertEquals("DURABLE_STATE_UNCERTAIN", receipt.reasonCode());
        assertTrue(receipt.trafficActionPerformed());
        assertTrue(receipt.baselineRestorationAttempted());
        assertTrue(receipt.baselineRestored());
        assertEquals(Kind.RESTORED_BASELINE, router.installedSnapshot().routingSnapshot().kind());
        assertEquals(TransactionPhase.VERIFYING,
                store.replay().chainHead().orElseThrow().transactionPhase());
        assertTrue(store.replay().records().stream().noneMatch(record ->
                record.allocationGeneration() == 2L
                        && record.transactionPhase() == TransactionPhase.COMMITTED));
    }

    @Test
    void mismatchedReadBackNeverCommitsCandidateAndRestoresVerifiedBaseline() {
        AtomicInteger reads = new AtomicInteger();
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator(
                checkpoint -> { },
                () -> reads.incrementAndGet() == 3
                        ? router.baselineInstalledSnapshot()
                        : router.installedSnapshot());
        coordinator.establishSafeBaseline("allocation-baseline-1");

        TransactionReceipt receipt = coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true);

        assertEquals(TransactionStatus.FAILED_RESTORED, receipt.status());
        assertTrue(receipt.trafficActionPerformed());
        assertTrue(receipt.baselineRestorationAttempted());
        assertTrue(receipt.baselineRestored());
        assertEquals(Kind.RESTORED_BASELINE, router.installedSnapshot().routingSnapshot().kind());
        assertNotEquals(receipt.intendedFingerprint(), receipt.installedFingerprint());
        assertEquals(List.of(
                TransactionPhase.COMMITTED,
                TransactionPhase.INTENT_PERSISTED,
                TransactionPhase.APPLYING,
                TransactionPhase.APPLIED,
                TransactionPhase.VERIFYING,
                TransactionPhase.RESTORE_REQUIRED,
                TransactionPhase.RESTORING,
                TransactionPhase.RESTORED), phases(store.replay().records()));
        assertTrue(store.replay().records().stream()
                .noneMatch(record -> record.transactionPhase() == TransactionPhase.COMMITTED
                        && record.allocationGeneration() == 2L));
    }

    @Test
    void unavailableCandidateReadBackTriggersVerifiedBaselineRestoration() {
        AtomicInteger reads = new AtomicInteger();
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator(
                checkpoint -> { },
                () -> {
                    if (reads.incrementAndGet() == 3) {
                        throw new IllegalStateException("injected bounded read-back failure");
                    }
                    return router.installedSnapshot();
                });
        coordinator.establishSafeBaseline("allocation-baseline-1");

        TransactionReceipt receipt = coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true);

        assertEquals(TransactionStatus.FAILED_RESTORED, receipt.status());
        assertTrue(receipt.baselineRestored());
        assertEquals(TransactionPhase.RESTORED, receipt.durablePhase().orElseThrow());
        assertEquals(Kind.RESTORED_BASELINE, router.installedSnapshot().routingSnapshot().kind());
        assertTrue(store.replay().records().stream().anyMatch(record ->
                record.verificationResult()
                        == EnterpriseLabAllocationState.VerificationResult.READ_BACK_FAILED));
    }

    @Test
    void unverifiableRestorationRemainsFailedClosedWithoutSafeClaim() {
        AtomicInteger reads = new AtomicInteger();
        AtomicReference<EnterpriseLabInstalledAllocationSnapshot> candidate = new AtomicReference<>();
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator(
                checkpoint -> { },
                () -> {
                    int read = reads.incrementAndGet();
                    if (read == 3) {
                        candidate.set(router.installedSnapshot());
                        return router.baselineInstalledSnapshot();
                    }
                    if (read == 4) {
                        return candidate.get();
                    }
                    return router.installedSnapshot();
                });
        coordinator.establishSafeBaseline("allocation-baseline-1");

        TransactionReceipt receipt = coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true);

        assertEquals(TransactionStatus.FAILED_NOT_RESTORED, receipt.status());
        assertTrue(receipt.baselineRestorationAttempted());
        assertFalse(receipt.baselineRestored());
        assertEquals(TransactionPhase.FAILED, receipt.durablePhase().orElseThrow());
        assertEquals("RESTORATION_NOT_VERIFIED", receipt.reasonCode());
        assertEquals(Kind.RESTORED_BASELINE, router.installedSnapshot().routingSnapshot().kind());
    }

    @Test
    void ownershipChangeAfterIntentFencesRouterMutationAndDurableCommit() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator(
                checkpoint -> {
                    if (checkpoint == Checkpoint.AFTER_INTENT_PERSIST) {
                        authority.replaceOwner("replacement-owner", 2L);
                    }
                }, router::installedSnapshot);
        coordinator.establishSafeBaseline("allocation-baseline-1");

        TransactionReceipt receipt = coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true);

        assertEquals(TransactionStatus.OWNERSHIP_LOST, receipt.status());
        assertEquals(Kind.BASELINE, router.installedSnapshot().routingSnapshot().kind());
        assertEquals(TransactionPhase.INTENT_PERSISTED,
                store.replay().chainHead().orElseThrow().transactionPhase());
        assertTrue(store.replay().records().stream()
                .noneMatch(record -> record.allocationGeneration() == 2L
                        && record.transactionPhase() == TransactionPhase.COMMITTED));
    }

    @Test
    void ownershipChangeAfterApplyLeavesCandidateUncommittedForTakeoverReconciliation() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator(
                checkpoint -> {
                    if (checkpoint == Checkpoint.AFTER_ROUTER_APPLY) {
                        authority.replaceOwner("replacement-owner", 2L);
                    }
                }, router::installedSnapshot);
        coordinator.establishSafeBaseline("allocation-baseline-1");

        TransactionReceipt receipt = coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true);

        assertEquals(TransactionStatus.OWNERSHIP_LOST, receipt.status());
        assertTrue(receipt.trafficActionPerformed());
        assertEquals(Kind.CANDIDATE, router.installedSnapshot().routingSnapshot().kind());
        assertEquals(TransactionPhase.APPLYING,
                store.replay().chainHead().orElseThrow().transactionPhase());
        assertTrue(store.replay().records().stream().noneMatch(record ->
                record.allocationGeneration() == 2L
                        && record.transactionPhase() == TransactionPhase.COMMITTED));
    }

    @Test
    void ownershipChangeAfterMatchingReadBackPreventsDurableCommit() {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator(
                checkpoint -> {
                    if (checkpoint == Checkpoint.BEFORE_COMMIT_PERSIST) {
                        authority.replaceOwner("replacement-owner", 2L);
                    }
                }, router::installedSnapshot);
        coordinator.establishSafeBaseline("allocation-baseline-1");

        TransactionReceipt receipt = coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true);

        assertEquals(TransactionStatus.OWNERSHIP_LOST, receipt.status());
        assertEquals(TransactionPhase.VERIFYING,
                store.replay().chainHead().orElseThrow().transactionPhase());
        assertEquals(Kind.CANDIDATE, router.installedSnapshot().routingSnapshot().kind());
        assertTrue(store.replay().records().stream().noneMatch(record ->
                record.allocationGeneration() == 2L
                        && record.transactionPhase() == TransactionPhase.COMMITTED));
    }

    @Test
    void readBackWithWrongOwnerGenerationRestoresBaselineDespiteMatchingAllocation() {
        AtomicInteger reads = new AtomicInteger();
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator(
                checkpoint -> { },
                () -> {
                    EnterpriseLabInstalledAllocationSnapshot actual = router.installedSnapshot();
                    if (reads.incrementAndGet() != 3) {
                        return actual;
                    }
                    return new EnterpriseLabInstalledAllocationSnapshot(
                            actual.schemaVersion(),
                            actual.routingSnapshot(),
                            actual.routerGeneration(),
                            actual.allocationFingerprint(),
                            actual.eligibleBackendIds(),
                            actual.excludedBackendIds(),
                            actual.installedAt(),
                            actual.installationReason(),
                            2L);
                });
        coordinator.establishSafeBaseline("allocation-baseline-1");

        TransactionReceipt receipt = coordinator.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true);

        assertEquals(TransactionStatus.FAILED_RESTORED, receipt.status());
        assertTrue(receipt.baselineRestored());
        assertEquals(Kind.RESTORED_BASELINE, router.installedSnapshot().routingSnapshot().kind());
        EnterpriseLabAllocationState verification = store.replay().records().get(4);
        assertEquals(TransactionPhase.VERIFYING, verification.transactionPhase());
        assertEquals(EnterpriseLabAllocationState.VerificationResult.MATCHED,
                verification.verificationResult());
        assertEquals(EnterpriseLabAllocationState.RecoveryClassification.DRIFT_DETECTED,
                verification.recoveryClassification());
    }

    @Test
    void concurrentIdenticalRequestsProduceOneCommitAndOneIdempotentReplay() throws Exception {
        EnterpriseLabAllocationTransactionCoordinator coordinator = coordinator();
        coordinator.establishSafeBaseline("allocation-baseline-1");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<TransactionReceipt> first = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return coordinator.applyCandidate(
                        "allocation-candidate-2", "experiment-1", decision, true);
            });
            Future<TransactionReceipt> second = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return coordinator.applyCandidate(
                        "allocation-candidate-2", "experiment-1", decision, true);
            });
            start.countDown();

            List<TransactionStatus> statuses = List.of(
                    first.get(10, TimeUnit.SECONDS).status(),
                    second.get(10, TimeUnit.SECONDS).status());
            assertTrue(statuses.contains(TransactionStatus.COMMITTED));
            assertTrue(statuses.contains(TransactionStatus.IDEMPOTENT));
            assertEquals(6, store.replay().records().size());
            assertEquals(1L, router.installedSnapshot().routerGeneration());
        } finally {
            executor.shutdownNow();
        }
    }

    private EnterpriseLabAllocationTransactionCoordinator coordinator() {
        return coordinator(checkpoint -> { }, router::installedSnapshot);
    }

    private EnterpriseLabAllocationTransactionCoordinator coordinator(
            EnterpriseLabAllocationTransactionCoordinator.FailureInjector failureInjector,
            EnterpriseLabAllocationTransactionCoordinator.InstalledStateReader reader) {
        return new EnterpriseLabAllocationTransactionCoordinator(
                store, router, targetCatalog, authority, clock, failureInjector, reader);
    }

    private EnterpriseLabLoopbackAllocationRouter router() {
        List<String> backendIds = targets.stream()
                .map(EnterpriseLabLoopbackTarget::backendId)
                .toList();
        return new EnterpriseLabLoopbackAllocationRouter(
                targets,
                new EnterpriseLabLoopbackObservationIngress(
                        backendIds,
                        com.richmond423.loadbalancerpro.core.ServerObservationWindowPolicy.localLabDefaults(),
                        16,
                        EnterpriseLabLoopbackObservationIngress.DEFAULT_MAX_MEASURED_LATENCY,
                        clock,
                        System::nanoTime),
                decision.decision().guardrailDecision().baselineAllocations(),
                java.util.Optional.of(authority),
                clock);
    }

    private static List<TransactionPhase> phases(List<EnterpriseLabAllocationState> records) {
        return records.stream().map(EnterpriseLabAllocationState::transactionPhase).toList();
    }

    private static List<EnterpriseLabLoopbackTarget> targets() {
        List<EnterpriseLabLoopbackTarget> values = new ArrayList<>();
        values.add(new EnterpriseLabLoopbackTarget(
                SCENARIO, "blue", URI.create("http://127.0.0.1:18081/health")));
        values.add(new EnterpriseLabLoopbackTarget(
                SCENARIO, "green", URI.create("http://127.0.0.1:18082/health")));
        values.add(new EnterpriseLabLoopbackTarget(
                SCENARIO, "orange", URI.create("http://[::1]:18083/health")));
        return List.copyOf(values);
    }

    private static final class SimulatedCrash extends RuntimeException {
        private final Checkpoint checkpoint;

        private SimulatedCrash(Checkpoint checkpoint) {
            super("simulated crash at " + checkpoint);
            this.checkpoint = checkpoint;
        }
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void advanceSeconds(long seconds) {
            current = current.plusSeconds(seconds);
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
            return current;
        }
    }
}
