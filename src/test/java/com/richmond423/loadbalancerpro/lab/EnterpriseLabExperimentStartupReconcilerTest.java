package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalEvent.Draft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalEvent.Reason;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.Checkpoint;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.ConfigurationEvidence;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.RestorationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.RollbackStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.AllocationInspection;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.AllocationRecoveryPort;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.BaselineRestorationReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.RecoveryAction;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.RecoveryClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnterpriseLabExperimentStartupReconcilerTest {
    private static final Instant BASE_TIME = Instant.parse("2026-07-18T01:00:00Z");
    private static final String EXPERIMENT_ID = "startup-recovery-experiment";
    private static final String SCENARIO_ID = "normal-balanced-load";
    private static final ConfigurationEvidence CONFIGURATION = new ConfigurationEvidence(
            "a".repeat(64),
            "b".repeat(64),
            20,
            60_000,
            2,
            2,
            EnterpriseLabExperimentRollbackPolicy.localLabDefaults(),
            AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT,
            true,
            BASE_TIME,
            BASE_TIME.plusSeconds(120));
    private static final EnterpriseLabLoopbackAllocationSnapshot BASELINE = allocation(
            0, "recorded-baseline", Kind.BASELINE,
            Map.of("blue", 0.34, "green", 0.33, "orange", 0.33));
    private static final EnterpriseLabLoopbackAllocationSnapshot CANDIDATE = allocation(
            1, "candidate-decision", Kind.CANDIDATE,
            Map.of("blue", 0.60, "green", 0.25, "orange", 0.15));
    private static final EnterpriseLabLoopbackAllocationSnapshot RESTORED = allocation(
            2, "baseline-restore", Kind.RESTORED_BASELINE,
            Map.of("blue", 0.34, "green", 0.33, "orange", 0.33));
    private static final Clock RECOVERY_CLOCK = Clock.fixed(
            BASE_TIME.plusSeconds(300), ZoneOffset.UTC);

    @TempDir
    Path tempDirectory;

    @Test
    void interruptedRunningExperimentRestoresBaselineTerminalizesAndRepeatsIdempotently() {
        EnterpriseLabExperimentJournalDirectory directory = directoryWith(runningChain());
        FakeAllocationRecovery recovery = new FakeAllocationRecovery(CANDIDATE, true);
        EnterpriseLabExperimentRecoveryGate gate = EnterpriseLabExperimentRecoveryGate.pending();
        EnterpriseLabExperimentStartupReconciler reconciler = reconciler(directory, recovery, gate);

        var first = reconciler.initialize();

        assertTrue(first.admissionAllowed(), first::toString);
        assertTrue(gate.admissionAllowed());
        assertEquals(1, recovery.restoreCalls);
        assertEquals(RecoveryClassification.INTERRUPTED_ROLLED_BACK,
                first.experiments().get(0).classification());
        assertEquals(RecoveryAction.BASELINE_RESTORATION_SUCCEEDED,
                first.experiments().get(0).action());
        var reconstructed = directory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow();
        assertEquals(EnterpriseLabExperimentState.ROLLED_BACK, reconstructed.lifecycle().state());
        assertEquals(RestorationStatus.SUCCEEDED, reconstructed.restorationStatus());
        assertFalse(reconstructed.lifecycle().candidateAllocationActive());
        long terminalSequence = reconstructed.latestSequence();

        assertEquals(first, reconciler.initialize());
        assertEquals(1, recovery.restoreCalls);
        assertEquals(terminalSequence,
                directory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow().latestSequence());

        EnterpriseLabExperimentRecoveryGate restartedGate = EnterpriseLabExperimentRecoveryGate.pending();
        var restarted = reconciler(directory, recovery, restartedGate).initialize();
        assertTrue(restarted.admissionAllowed(), restarted::toString);
        assertEquals(RecoveryClassification.TERMINAL_PRESERVED,
                restarted.experiments().get(0).classification());
        assertEquals(1, recovery.restoreCalls);
        assertEquals(terminalSequence,
                directory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow().latestSequence());
    }

    @Test
    void armedExperimentAtBaselineIsCancelledWithoutStartingOrMutatingTraffic() {
        EnterpriseLabExperimentJournalDirectory directory = directoryWith(armedChain());
        FakeAllocationRecovery recovery = new FakeAllocationRecovery(BASELINE, true);
        EnterpriseLabExperimentRecoveryGate gate = EnterpriseLabExperimentRecoveryGate.pending();

        var report = reconciler(directory, recovery, gate).initialize();

        assertTrue(report.admissionAllowed(), report::toString);
        assertEquals(0, recovery.restoreCalls);
        assertEquals(RecoveryAction.NO_OP_RECONCILIATION,
                report.experiments().get(0).action());
        assertEquals(EnterpriseLabExperimentState.CANCELLED,
                directory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow().lifecycle().state());
    }

    @Test
    void interruptedCompletionFinishesOnlyAfterBaselineVerification() {
        EnterpriseLabExperimentJournalDirectory directory = directoryWith(completingChain());
        FakeAllocationRecovery recovery = new FakeAllocationRecovery(CANDIDATE, true);
        EnterpriseLabExperimentRecoveryGate gate = EnterpriseLabExperimentRecoveryGate.pending();

        var report = reconciler(directory, recovery, gate).initialize();

        assertTrue(report.admissionAllowed(), report::toString);
        assertEquals(RecoveryClassification.COMPLETION_FINALIZED,
                report.experiments().get(0).classification());
        assertEquals(EnterpriseLabExperimentState.COMPLETED,
                directory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow().lifecycle().state());
        assertEquals(1, recovery.restoreCalls);
    }

    @Test
    void interruptedHoldingAndRollbackStatesBothCompleteFailClosedRecovery() {
        EnterpriseLabExperimentJournalDirectory holdingDirectory = directoryWith(holdingChain());
        FakeAllocationRecovery holdingRecovery = new FakeAllocationRecovery(CANDIDATE, true);
        EnterpriseLabExperimentRecoveryGate holdingGate = EnterpriseLabExperimentRecoveryGate.pending();

        var holdingReport = reconciler(holdingDirectory, holdingRecovery, holdingGate).initialize();

        assertTrue(holdingReport.admissionAllowed(), holdingReport::toString);
        assertEquals(EnterpriseLabExperimentState.ROLLED_BACK,
                holdingDirectory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow().lifecycle().state());

        Path rollbackRoot = tempDirectory.resolve("rollback-root");
        try {
            Files.createDirectory(rollbackRoot);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        EnterpriseLabExperimentJournalDirectory rollbackDirectory =
                EnterpriseLabExperimentJournalDirectory.create(rollbackRoot);
        try (EnterpriseLabExperimentJournal journal = rollbackDirectory.openJournal(EXPERIMENT_ID)) {
            rollingBackChain().forEach(journal::append);
        }
        FakeAllocationRecovery rollbackRecovery = new FakeAllocationRecovery(CANDIDATE, true);
        EnterpriseLabExperimentRecoveryGate rollbackGate = EnterpriseLabExperimentRecoveryGate.pending();

        var rollbackReport = reconciler(rollbackDirectory, rollbackRecovery, rollbackGate).initialize();

        assertTrue(rollbackReport.admissionAllowed(), rollbackReport::toString);
        assertEquals(EnterpriseLabExperimentState.ROLLED_BACK,
                rollbackDirectory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow().lifecycle().state());
        assertEquals(1, rollbackRecovery.restoreCalls);
    }

    @Test
    void previouslyRestoredRollbackReportsTheAdditionalDriftRestoration() {
        EnterpriseLabExperimentJournalDirectory directory = directoryWith(
                rollingBackRestoredChain());
        FakeAllocationRecovery recovery = new FakeAllocationRecovery(CANDIDATE, true);
        EnterpriseLabExperimentRecoveryGate gate = EnterpriseLabExperimentRecoveryGate.pending();

        var report = reconciler(directory, recovery, gate).initialize();

        assertTrue(report.admissionAllowed(), report::toString);
        assertEquals(RecoveryAction.BASELINE_RESTORATION_SUCCEEDED,
                report.experiments().get(0).action());
        assertEquals(1, recovery.restoreCalls);
        assertEquals(EnterpriseLabExperimentState.ROLLED_BACK,
                directory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow().lifecycle().state());
    }

    @Test
    void armedCandidateIsRestoredBeforeCancellationAndTerminalDriftIsRestored() throws Exception {
        EnterpriseLabExperimentJournalDirectory armedDirectory = directoryWith(candidateArmedChain());
        FakeAllocationRecovery armedRecovery = new FakeAllocationRecovery(CANDIDATE, true);
        EnterpriseLabExperimentRecoveryGate armedGate = EnterpriseLabExperimentRecoveryGate.pending();

        var armedReport = reconciler(armedDirectory, armedRecovery, armedGate).initialize();

        assertTrue(armedReport.admissionAllowed(), armedReport::toString);
        assertEquals(RecoveryAction.BASELINE_RESTORATION_SUCCEEDED,
                armedReport.experiments().get(0).action());
        assertEquals(EnterpriseLabExperimentState.CANCELLED,
                armedDirectory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow().lifecycle().state());

        Path terminalRoot = tempDirectory.resolve("terminal-drift-root");
        Files.createDirectory(terminalRoot);
        EnterpriseLabExperimentJournalDirectory terminalDirectory =
                EnterpriseLabExperimentJournalDirectory.create(terminalRoot);
        try (EnterpriseLabExperimentJournal journal = terminalDirectory.openJournal(EXPERIMENT_ID)) {
            completedChain().forEach(journal::append);
        }
        FakeAllocationRecovery terminalRecovery = new FakeAllocationRecovery(CANDIDATE, true);
        EnterpriseLabExperimentRecoveryGate terminalGate = EnterpriseLabExperimentRecoveryGate.pending();

        var terminalReport = reconciler(terminalDirectory, terminalRecovery, terminalGate).initialize();

        assertTrue(terminalReport.admissionAllowed(), terminalReport::toString);
        assertEquals(RecoveryClassification.TERMINAL_DRIFT_RESTORED,
                terminalReport.experiments().get(0).classification());
        assertEquals(EnterpriseLabExperimentState.COMPLETED,
                terminalDirectory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow().lifecycle().state());
    }

    @Test
    void restorationFailureIsDurablyRecordedAndAdmissionFailsClosed() {
        EnterpriseLabExperimentJournalDirectory directory = directoryWith(runningChain());
        FakeAllocationRecovery recovery = new FakeAllocationRecovery(CANDIDATE, false);
        EnterpriseLabExperimentRecoveryGate gate = EnterpriseLabExperimentRecoveryGate.pending();

        var report = reconciler(directory, recovery, gate).initialize();

        assertFalse(report.admissionAllowed());
        assertFalse(gate.admissionAllowed());
        assertEquals(RecoveryClassification.RESTORATION_FAILED,
                report.experiments().get(0).classification());
        var state = directory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow();
        assertEquals(EnterpriseLabExperimentState.ROLLING_BACK, state.lifecycle().state());
        assertEquals(RollbackStatus.FAILED, state.rollbackStatus());
        assertEquals(RestorationStatus.FAILED, state.restorationStatus());
    }

    @Test
    void restorationExceptionIsReportedAsAttemptedWithIndeterminateOutcome() {
        EnterpriseLabExperimentJournalDirectory directory = directoryWith(runningChain());
        AllocationRecoveryPort recovery = new AllocationRecoveryPort() {
            @Override
            public AllocationInspection inspect(
                    EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState state) {
                return new AllocationInspection(CANDIDATE, "TEST_ALLOCATION_INSPECTED");
            }

            @Override
            public BaselineRestorationReceipt restoreBaseline(
                    EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState state,
                    String reason) {
                throw new IllegalStateException("controlled restoration exception");
            }
        };
        EnterpriseLabExperimentRecoveryGate gate = EnterpriseLabExperimentRecoveryGate.pending();

        var report = reconciler(directory, recovery, gate).initialize();

        assertFalse(report.admissionAllowed());
        assertEquals(RecoveryAction.BASELINE_RESTORATION_ATTEMPTED,
                report.experiments().get(0).action());
        assertEquals("RESTORATION_ATTEMPT_INDETERMINATE",
                report.experiments().get(0).reasonCode());
        var state = directory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow();
        assertEquals(EnterpriseLabExperimentState.ROLLING_BACK, state.lifecycle().state());
        assertEquals(RestorationStatus.ATTEMPTED, state.restorationStatus());
    }

    @Test
    void armedUnverifiedRestorationIsNotClaimedAsSuccessOrRetriedAutomatically() {
        EnterpriseLabExperimentJournalDirectory directory = directoryWith(candidateArmedChain());
        AllocationRecoveryPort unverifiedRecovery = new AllocationRecoveryPort() {
            @Override
            public AllocationInspection inspect(
                    EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState state) {
                return new AllocationInspection(CANDIDATE, "TEST_ALLOCATION_INSPECTED");
            }

            @Override
            public BaselineRestorationReceipt restoreBaseline(
                    EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState state,
                    String reason) {
                return new BaselineRestorationReceipt(
                        true, false, CANDIDATE, "TEST_RESTORATION_UNVERIFIED");
            }
        };
        EnterpriseLabExperimentRecoveryGate gate = EnterpriseLabExperimentRecoveryGate.pending();

        var first = reconciler(directory, unverifiedRecovery, gate).initialize();

        assertFalse(first.admissionAllowed());
        assertEquals(RecoveryAction.BASELINE_RESTORATION_FAILED,
                first.experiments().get(0).action());
        assertEquals("TEST_RESTORATION_UNVERIFIED", first.experiments().get(0).reasonCode());
        assertEquals(RestorationStatus.FAILED,
                directory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow().restorationStatus());

        FakeAllocationRecovery laterRecovery = new FakeAllocationRecovery(CANDIDATE, true);
        EnterpriseLabExperimentRecoveryGate restartedGate = EnterpriseLabExperimentRecoveryGate.pending();
        var restarted = reconciler(directory, laterRecovery, restartedGate).initialize();

        assertFalse(restarted.admissionAllowed());
        assertEquals(RecoveryAction.NONE, restarted.experiments().get(0).action());
        assertEquals(0, laterRecovery.restoreCalls);
    }

    @Test
    void corruptedJournalIsQuarantinedWithoutTrustingOrRestoringItsAllocation() throws Exception {
        EnterpriseLabExperimentJournalDirectory directory = directoryWith(runningChain());
        Path journalFile = Files.walk(tempDirectory)
                .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                .findFirst()
                .orElseThrow();
        String content = Files.readString(journalFile, StandardCharsets.UTF_8);
        Files.writeString(journalFile, content.replaceFirst("candidate-decision", "candidate-decisioN"),
                StandardCharsets.UTF_8);
        FakeAllocationRecovery recovery = new FakeAllocationRecovery(CANDIDATE, true);
        EnterpriseLabExperimentRecoveryGate gate = EnterpriseLabExperimentRecoveryGate.pending();

        var report = reconciler(directory, recovery, gate).initialize();

        assertFalse(report.admissionAllowed());
        assertFalse(gate.admissionAllowed());
        assertEquals(0, recovery.inspectCalls);
        assertEquals(0, recovery.restoreCalls);
        assertEquals(RecoveryClassification.QUARANTINED,
                report.experiments().get(0).classification());
        assertTrue(report.experiments().get(0).quarantine().orElseThrow().originalBytesPreserved());
        assertFalse(Files.exists(journalFile));
        assertEquals(1, Files.walk(tempDirectory)
                .filter(path -> path.getFileName().toString().endsWith(".quarantined"))
                .count());
    }

    @Test
    void truncatedTailIsQuarantinedAndAnActiveWriterMakesRecoveryUnavailable() throws Exception {
        EnterpriseLabExperimentJournalDirectory truncatedDirectory = directoryWith(runningChain());
        Path journalFile = Files.walk(tempDirectory)
                .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                .findFirst()
                .orElseThrow();
        Files.writeString(journalFile, "{\"partial\"", StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND);
        FakeAllocationRecovery truncatedRecovery = new FakeAllocationRecovery(CANDIDATE, true);
        EnterpriseLabExperimentRecoveryGate truncatedGate = EnterpriseLabExperimentRecoveryGate.pending();

        var truncatedReport = reconciler(
                truncatedDirectory, truncatedRecovery, truncatedGate).initialize();

        assertFalse(truncatedReport.admissionAllowed());
        assertEquals(RecoveryClassification.QUARANTINED,
                truncatedReport.experiments().get(0).classification());
        assertEquals(0, truncatedRecovery.inspectCalls);

        Path writerRoot = tempDirectory.resolve("active-writer-root");
        Files.createDirectory(writerRoot);
        EnterpriseLabExperimentJournalDirectory writerDirectory =
                EnterpriseLabExperimentJournalDirectory.create(writerRoot);
        EnterpriseLabExperimentRecoveryGate writerGate = EnterpriseLabExperimentRecoveryGate.pending();
        try (EnterpriseLabExperimentJournal journal = writerDirectory.openJournal(EXPERIMENT_ID)) {
            armedChain().forEach(journal::append);

            var writerReport = reconciler(
                    writerDirectory,
                    new FakeAllocationRecovery(BASELINE, true),
                    writerGate).initialize();

            assertFalse(writerReport.admissionAllowed());
            assertEquals(RecoveryClassification.QUARANTINE_FAILED,
                    writerReport.experiments().get(0).classification());
            assertFalse(writerGate.admissionAllowed());
        }
    }

    @Test
    void terminalRecordAtBaselineIsPreservedWithoutNewEvidenceOrTrafficAction() {
        EnterpriseLabExperimentJournalDirectory directory = directoryWith(completedChain());
        long before = directory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow().latestSequence();
        FakeAllocationRecovery recovery = new FakeAllocationRecovery(BASELINE, true);
        EnterpriseLabExperimentRecoveryGate gate = EnterpriseLabExperimentRecoveryGate.pending();

        var report = reconciler(directory, recovery, gate).initialize();

        assertEquals(RecoveryClassification.TERMINAL_PRESERVED,
                report.experiments().get(0).classification());
        assertEquals(RecoveryAction.REPLAY_ONLY, report.experiments().get(0).action());
        assertEquals(0, recovery.restoreCalls);
        assertEquals(before,
                directory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow().latestSequence());
    }

    @Test
    void processLocalRecoveryReconstructsOnlyApprovedLoopbackRouterAtBaselineWithoutRequests() {
        EnterpriseLabExperimentJournalDirectory directory = directoryWith(runningChain());
        var reconstructed = directory.replay(EXPERIMENT_ID).reconstructedState().orElseThrow();
        var scenario = new EnterpriseLabScenarioCatalogService().findScenario(SCENARIO_ID).orElseThrow();
        java.util.concurrent.atomic.AtomicInteger port = new java.util.concurrent.atomic.AtomicInteger(48_100);
        List<EnterpriseLabLoopbackTarget> targets = scenario.servers().stream()
                .map(server -> new EnterpriseLabLoopbackTarget(
                        SCENARIO_ID,
                        server.id(),
                        URI.create("http://127.0.0.1:" + port.getAndIncrement() + "/recovery")))
                .toList();
        EnterpriseLabProcessLocalAllocationRecovery recovery =
                new EnterpriseLabProcessLocalAllocationRecovery(
                        new EnterpriseLabExperimentTargetCatalog(targets));

        var inspection = recovery.inspect(reconstructed);
        var restoration = recovery.restoreBaseline(reconstructed, "focused process-local verification");

        assertTrue(EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                BASELINE.allocations(), inspection.currentAllocation().allocations()));
        assertTrue(restoration.succeeded());
        assertFalse(restoration.trafficActionPerformed());
        assertEquals("PROCESS_LOCAL_BASELINE_VERIFIED", restoration.reasonCode());
    }

    @Test
    void discoveryCountIsHardBoundedBeforeStartupCanAdmitExperiments() throws Exception {
        Path root = tempDirectory.resolve("discovery-bound-root");
        Files.createDirectory(root);
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(root);
        Path journals = Files.walk(root)
                .filter(path -> path.getFileName().toString().equals("journals"))
                .findFirst()
                .orElseThrow();
        for (int index = 0;
                index <= EnterpriseLabExperimentJournalDirectory.HARD_MAX_DISCOVERED_JOURNALS;
                index++) {
            Files.createFile(journals.resolve("unexpected-" + index));
        }

        EnterpriseLabExperimentJournalStorageException exception = assertThrows(
                EnterpriseLabExperimentJournalStorageException.class,
                directory::discover);

        assertEquals(EnterpriseLabExperimentJournalStorageException.Failure.DISCOVERY_LIMIT_EXCEEDED,
                exception.failure());
    }

    @Test
    void operatorAdmissionRejectsArmStartAndRequestTrafficWhileReconciliationIsPending() {
        EnterpriseLabExperimentRecoveryGate gate = EnterpriseLabExperimentRecoveryGate.pending();
        EnterpriseLabExperimentOperatorService service = new EnterpriseLabExperimentOperatorService(
                EnterpriseLabExperimentTargetCatalog.empty(), gate);
        var arm = service.arm(new EnterpriseLabExperimentOperatorService.ArmRequest(
                "arm-request", "pending-experiment", SCENARIO_ID,
                10, java.time.Duration.ofSeconds(30), 2, 1,
                java.time.Duration.ofSeconds(60)), true);

        assertEquals("RECOVERY_NOT_READY", arm.reasonCode());
        assertEquals("RECOVERY_NOT_READY",
                service.start("pending-experiment", "start-request", true).reasonCode());
        assertEquals("RECOVERY_NOT_READY", service.executeRequests(
                "pending-experiment",
                new EnterpriseLabExperimentOperatorService.RequestBatchRequest(
                        "batch-request", 1, java.time.Duration.ofSeconds(1)),
                true).reasonCode());
    }

    private EnterpriseLabExperimentStartupReconciler reconciler(
            EnterpriseLabExperimentJournalDirectory directory,
            AllocationRecoveryPort recovery,
            EnterpriseLabExperimentRecoveryGate gate) {
        return new EnterpriseLabExperimentStartupReconciler(
                directory, recovery, gate, RECOVERY_CLOCK);
    }

    private EnterpriseLabExperimentJournalDirectory directoryWith(
            List<EnterpriseLabExperimentJournalEvent> events) {
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(tempDirectory);
        try (EnterpriseLabExperimentJournal journal = directory.openJournal(EXPERIMENT_ID)) {
            events.forEach(journal::append);
        }
        return directory;
    }

    private static List<EnterpriseLabExperimentJournalEvent> armedChain() {
        List<EnterpriseLabExperimentJournalEvent> events = new ArrayList<>();
        add(events, EnterpriseLabExperimentJournalEventType.EXPERIMENT_ARMED,
                EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.ARMED,
                checkpoint(BASELINE, RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        return events;
    }

    private static List<EnterpriseLabExperimentJournalEvent> runningChain() {
        List<EnterpriseLabExperimentJournalEvent> events = armedChain();
        add(events, EnterpriseLabExperimentJournalEventType.CANDIDATE_ALLOCATION_APPLIED,
                EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.ARMED,
                checkpoint(CANDIDATE, RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.EXPERIMENT_STARTED,
                EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.RUNNING,
                checkpoint(CANDIDATE, RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        return events;
    }

    private static List<EnterpriseLabExperimentJournalEvent> candidateArmedChain() {
        List<EnterpriseLabExperimentJournalEvent> events = armedChain();
        add(events, EnterpriseLabExperimentJournalEventType.CANDIDATE_ALLOCATION_APPLIED,
                EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.ARMED,
                checkpoint(CANDIDATE, RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        return events;
    }

    private static List<EnterpriseLabExperimentJournalEvent> holdingChain() {
        List<EnterpriseLabExperimentJournalEvent> events = runningChain();
        add(events, EnterpriseLabExperimentJournalEventType.LIFECYCLE_TRANSITION,
                EnterpriseLabExperimentState.RUNNING, EnterpriseLabExperimentState.HOLDING,
                checkpoint(CANDIDATE, RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        return events;
    }

    private static List<EnterpriseLabExperimentJournalEvent> rollingBackChain() {
        List<EnterpriseLabExperimentJournalEvent> events = runningChain();
        add(events, EnterpriseLabExperimentJournalEventType.ROLLBACK_REQUESTED,
                EnterpriseLabExperimentState.RUNNING, EnterpriseLabExperimentState.ROLLING_BACK,
                checkpoint(CANDIDATE, RollbackStatus.REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.BASELINE_RESTORATION_ATTEMPTED,
                EnterpriseLabExperimentState.ROLLING_BACK, EnterpriseLabExperimentState.ROLLING_BACK,
                checkpoint(CANDIDATE, RollbackStatus.IN_PROGRESS, RestorationStatus.ATTEMPTED));
        return events;
    }

    private static List<EnterpriseLabExperimentJournalEvent> rollingBackRestoredChain() {
        List<EnterpriseLabExperimentJournalEvent> events = rollingBackChain();
        add(events, EnterpriseLabExperimentJournalEventType.BASELINE_RESTORED,
                EnterpriseLabExperimentState.ROLLING_BACK, EnterpriseLabExperimentState.ROLLING_BACK,
                checkpoint(RESTORED, RollbackStatus.IN_PROGRESS, RestorationStatus.SUCCEEDED));
        return events;
    }

    private static List<EnterpriseLabExperimentJournalEvent> completingChain() {
        List<EnterpriseLabExperimentJournalEvent> events = holdingChain();
        add(events, EnterpriseLabExperimentJournalEventType.LIFECYCLE_TRANSITION,
                EnterpriseLabExperimentState.HOLDING, EnterpriseLabExperimentState.COMPLETING,
                checkpoint(CANDIDATE, RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        return events;
    }

    private static List<EnterpriseLabExperimentJournalEvent> completedChain() {
        List<EnterpriseLabExperimentJournalEvent> events = completingChain();
        add(events, EnterpriseLabExperimentJournalEventType.BASELINE_RESTORATION_ATTEMPTED,
                EnterpriseLabExperimentState.COMPLETING, EnterpriseLabExperimentState.COMPLETING,
                checkpoint(CANDIDATE, RollbackStatus.NOT_REQUESTED, RestorationStatus.ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.BASELINE_RESTORED,
                EnterpriseLabExperimentState.COMPLETING, EnterpriseLabExperimentState.COMPLETING,
                checkpoint(RESTORED, RollbackStatus.NOT_REQUESTED, RestorationStatus.SUCCEEDED));
        add(events, EnterpriseLabExperimentJournalEventType.EXPERIMENT_COMPLETED,
                EnterpriseLabExperimentState.COMPLETING, EnterpriseLabExperimentState.COMPLETED,
                checkpoint(RESTORED, RollbackStatus.NOT_REQUESTED, RestorationStatus.SUCCEEDED));
        return events;
    }

    private static Checkpoint checkpoint(
            EnterpriseLabLoopbackAllocationSnapshot applied,
            RollbackStatus rollback,
            RestorationStatus restoration) {
        return new Checkpoint(
                CONFIGURATION, BASELINE, Optional.of(CANDIDATE), applied,
                0, 0, 0, rollback, restoration);
    }

    private static void add(
            List<EnterpriseLabExperimentJournalEvent> events,
            EnterpriseLabExperimentJournalEventType type,
            EnterpriseLabExperimentState before,
            EnterpriseLabExperimentState after,
            Checkpoint checkpoint) {
        long sequence = events.size() + 1L;
        String previous = events.isEmpty()
                ? EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT
                : events.get(events.size() - 1).currentEntryFingerprint();
        events.add(EnterpriseLabExperimentJournalEvent.create(
                Clock.fixed(BASE_TIME.plusSeconds(sequence), ZoneOffset.UTC),
                new Draft(
                        sequence,
                        EXPERIMENT_ID,
                        SCENARIO_ID,
                        type,
                        before,
                        after,
                        sequence,
                        CONFIGURATION.configurationFingerprint(),
                        CONFIGURATION.decisionFingerprint(),
                        EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(BASELINE),
                        EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(CANDIDATE),
                        EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(
                                checkpoint.appliedAllocation()),
                        new Reason("EVENT_" + sequence, "startup test event " + sequence),
                        previous,
                        Map.of("source", "startup-test"),
                        EnterpriseLabExperimentJournalReplayPayload.encode(checkpoint))));
    }

    private static EnterpriseLabLoopbackAllocationSnapshot allocation(
            long revision,
            String source,
            Kind kind,
            Map<String, Double> allocations) {
        return new EnterpriseLabLoopbackAllocationSnapshot(
                EnterpriseLabLoopbackAllocationSnapshot.SCHEMA_VERSION,
                SCENARIO_ID,
                revision,
                source,
                kind,
                allocations);
    }

    private static final class FakeAllocationRecovery implements AllocationRecoveryPort {
        private EnterpriseLabLoopbackAllocationSnapshot current;
        private final boolean restorationSucceeds;
        private int inspectCalls;
        private int restoreCalls;

        private FakeAllocationRecovery(
                EnterpriseLabLoopbackAllocationSnapshot current,
                boolean restorationSucceeds) {
            this.current = current;
            this.restorationSucceeds = restorationSucceeds;
        }

        @Override
        public AllocationInspection inspect(
                EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState state) {
            inspectCalls++;
            return new AllocationInspection(current, "TEST_ALLOCATION_INSPECTED");
        }

        @Override
        public BaselineRestorationReceipt restoreBaseline(
                EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState state,
                String reason) {
            restoreCalls++;
            if (!restorationSucceeds) {
                return new BaselineRestorationReceipt(
                        false, false, current, "TEST_RESTORATION_FAILED");
            }
            boolean action = !EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                    current.allocations(), BASELINE.allocations());
            current = RESTORED;
            return new BaselineRestorationReceipt(
                    true, action, current, "TEST_BASELINE_VERIFIED");
        }
    }
}
