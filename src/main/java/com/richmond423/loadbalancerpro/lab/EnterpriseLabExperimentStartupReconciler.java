package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayEngine.ReplayResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.Checkpoint;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.RestorationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.RollbackStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalDirectory.DiscoveryOutcome;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalDirectory.JournalDiscovery;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalDirectory.QuarantineRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalEvent.Draft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalEvent.Reason;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalVerifier.Outcome;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Bounded, synchronous restart reconciliation. Verification and pure replay
 * always precede allocation inspection or mutation. Candidate traffic is never
 * resumed; non-terminal experiments return to a terminal baseline-safe state.
 */
public final class EnterpriseLabExperimentStartupReconciler {
    private static final String RECOVERY_REASON =
            "startup recovery returned the interrupted Enterprise Lab experiment to its recorded baseline";

    private final EnterpriseLabExperimentJournalDirectory directory;
    private final AllocationRecoveryPort allocationRecovery;
    private final EnterpriseLabExperimentRecoveryGate gate;
    private final Clock clock;

    public EnterpriseLabExperimentStartupReconciler(
            EnterpriseLabExperimentJournalDirectory directory,
            AllocationRecoveryPort allocationRecovery,
            EnterpriseLabExperimentRecoveryGate gate,
            Clock clock) {
        this.directory = Objects.requireNonNull(directory, "directory cannot be null");
        this.allocationRecovery = Objects.requireNonNull(
                allocationRecovery, "allocationRecovery cannot be null");
        this.gate = Objects.requireNonNull(gate, "gate cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    public synchronized RecoveryReport initialize() {
        Optional<RecoveryReport> existing = gate.admissionStatus().recoveryReport();
        if (existing.isPresent()) {
            return existing.orElseThrow();
        }
        gate.begin();
        Instant startedAt = clock.instant();
        List<ExperimentRecoveryResult> results = new ArrayList<>();
        boolean safe = true;
        try {
            for (JournalDiscovery discovery : directory.discover()) {
                ExperimentRecoveryResult result = reconcile(discovery);
                results.add(result);
                safe &= result.safeForNewExperimentAdmission();
            }
            Instant completedAt = clock.instant();
            RecoveryReport report = new RecoveryReport(
                    RecoveryReport.SCHEMA_VERSION,
                    safe,
                    safe ? "RECOVERY_READY" : "RECOVERY_FAILED_CLOSED",
                    startedAt,
                    completedAt,
                    results);
            gate.complete(report);
            return report;
        } catch (RuntimeException exception) {
            gate.fail("RECOVERY_INITIALIZATION_FAILED");
            throw exception;
        }
    }

    Path trustedRoot() {
        return directory.trustedRoot();
    }

    private ExperimentRecoveryResult reconcile(JournalDiscovery discovery) {
        if (discovery.outcome() != DiscoveryOutcome.VERIFIED) {
            return quarantineOrFail(discovery, discovery.classification());
        }
        var verification = discovery.verification().orElseThrow();
        if (verification.outcome() != Outcome.VALID) {
            return quarantineOrFail(discovery, verification.classification().name());
        }
        ReplayResult replay = new EnterpriseLabExperimentJournalReplayEngine().replay(verification);
        if (replay.outcome() != EnterpriseLabExperimentJournalReplayEngine.Outcome.RECONSTRUCTED) {
            return quarantineOrFail(discovery, replay.classification().name());
        }
        ReconstructedExperimentState state = replay.reconstructedState().orElseThrow();
        try {
            AllocationInspection inspection = allocationRecovery.inspect(state);
            if (!state.scenarioId().equals(inspection.currentAllocation().scenarioId())) {
                return blocked(state, "ALLOCATION_IDENTITY_MISMATCH",
                        "actual local allocation belongs to a different scenario");
            }
            if (state.lifecycle().terminal()) {
                return reconcileTerminal(discovery, state, inspection);
            }
            return reconcileInterrupted(discovery, state, inspection);
        } catch (RestorationAttemptException exception) {
            return result(state, state.lifecycle().state(), RecoveryClassification.RESTORATION_FAILED,
                    RecoveryAction.BASELINE_RESTORATION_ATTEMPTED, false,
                    "RESTORATION_ATTEMPT_INDETERMINATE",
                    "baseline restoration began but did not return a verifiable outcome",
                    Optional.empty());
        } catch (RuntimeException exception) {
            return blocked(state, "ALLOCATION_RECONCILIATION_FAILED",
                    "local allocation reconciliation failed closed");
        }
    }

    private ExperimentRecoveryResult reconcileTerminal(
            JournalDiscovery discovery,
            ReconstructedExperimentState state,
            AllocationInspection inspection) {
        if (sameAsBaseline(inspection.currentAllocation(), state)) {
            return result(state, state.lifecycle().state(), RecoveryClassification.TERMINAL_PRESERVED,
                    RecoveryAction.REPLAY_ONLY, true, "TERMINAL_RECORD_PRESERVED",
                    "terminal journal replayed without traffic or allocation mutation", Optional.empty());
        }
        BaselineRestorationReceipt restored = restoreBaseline(state);
        if (!restored.succeeded() || !sameAsBaseline(restored.verifiedAllocation(), state)) {
            return result(state, state.lifecycle().state(), RecoveryClassification.RESTORATION_FAILED,
                    RecoveryAction.BASELINE_RESTORATION_FAILED, false, restored.reasonCode(),
                    "terminal allocation drift could not be restored and verified", Optional.empty());
        }
        try (EnterpriseLabExperimentJournal journal = directory.openJournal(state.experimentId())) {
            RecoveryAppender appender = new RecoveryAppender(journal, discovery, state, clock);
            appender.append(
                    EnterpriseLabExperimentJournalEventType.RECOVERY_ACTION,
                    state.lifecycle().state(),
                    state.lifecycle().state(),
                    restored.verifiedAllocation(),
                    state.rollbackStatus(),
                    RestorationStatus.SUCCEEDED,
                    "TERMINAL_DRIFT_RESTORED",
                    "startup recovery restored terminal allocation drift",
                    restored.trafficActionPerformed());
        }
        return verifiedResult(state.experimentId(), state.lifecycle().state(),
                RecoveryClassification.TERMINAL_DRIFT_RESTORED,
                action(restored), "TERMINAL_DRIFT_RESTORED",
                "terminal record was preserved after verified baseline restoration");
    }

    private ExperimentRecoveryResult reconcileInterrupted(
            JournalDiscovery discovery,
            ReconstructedExperimentState state,
            AllocationInspection inspection) {
        return switch (state.lifecycle().state()) {
            case ARMED -> reconcileArmed(discovery, state, inspection);
            case RUNNING, HOLDING -> recoverRollback(discovery, state);
            case COMPLETING -> recoverCompletion(discovery, state);
            case ROLLING_BACK -> continueRollback(discovery, state);
            default -> blocked(state, "UNSUPPORTED_NON_TERMINAL_STATE",
                    "reconstructed state has no safe startup recovery path");
        };
    }

    private ExperimentRecoveryResult reconcileArmed(
            JournalDiscovery discovery,
            ReconstructedExperimentState state,
            AllocationInspection inspection) {
        if (state.restorationStatus() == RestorationStatus.FAILED) {
            return result(state, EnterpriseLabExperimentState.ARMED,
                    RecoveryClassification.RESTORATION_FAILED,
                    RecoveryAction.NONE, false,
                    "PREVIOUS_RESTORATION_FAILED",
                    "a recorded armed restoration failure requires deliberate operator intervention",
                    Optional.empty());
        }
        BaselineRestorationReceipt restoration = null;
        try (EnterpriseLabExperimentJournal journal = directory.openJournal(state.experimentId())) {
            RecoveryAppender appender = new RecoveryAppender(journal, discovery, state, clock);
            RestorationStatus restorationStatus = state.restorationStatus();
            EnterpriseLabLoopbackAllocationSnapshot applied = state.lastAppliedAllocation();
            if (!sameAsBaseline(inspection.currentAllocation(), state)) {
                if (restorationStatus != RestorationStatus.SUCCEEDED) {
                    appender.append(
                            EnterpriseLabExperimentJournalEventType.RECOVERY_ACTION,
                            EnterpriseLabExperimentState.ARMED,
                            EnterpriseLabExperimentState.ARMED,
                            applied,
                            RollbackStatus.NOT_REQUESTED,
                            RestorationStatus.ATTEMPTED,
                            "ARMED_BASELINE_RESTORATION_ATTEMPTED",
                            "startup recovery attempted baseline restoration before cancelling an armed experiment",
                            false);
                }
                restoration = restoreBaseline(state);
                boolean verifiedRestoration = restoration.succeeded()
                        && sameAsBaseline(restoration.verifiedAllocation(), state);
                restorationStatus = verifiedRestoration
                        ? RestorationStatus.SUCCEEDED : RestorationStatus.FAILED;
                applied = verifiedRestoration ? restoration.verifiedAllocation() : applied;
                if (!verifiedRestoration && state.restorationStatus() == RestorationStatus.SUCCEEDED) {
                    return result(state, EnterpriseLabExperimentState.ARMED,
                            RecoveryClassification.RESTORATION_FAILED,
                            RecoveryAction.BASELINE_RESTORATION_FAILED, false,
                            restoration.reasonCode(),
                            "armed allocation drift could not be restored after prior verified restoration",
                            Optional.empty());
                }
                appender.append(
                        EnterpriseLabExperimentJournalEventType.RECOVERY_ACTION,
                        EnterpriseLabExperimentState.ARMED,
                        EnterpriseLabExperimentState.ARMED,
                        applied,
                        RollbackStatus.NOT_REQUESTED,
                        restorationStatus,
                        verifiedRestoration
                                ? "ARMED_BASELINE_RESTORED" : "ARMED_BASELINE_RESTORATION_FAILED",
                        verifiedRestoration
                                ? "startup recovery verified the baseline before armed cancellation"
                                : "startup recovery could not verify baseline restoration",
                        restoration.trafficActionPerformed());
                if (restorationStatus == RestorationStatus.FAILED) {
                    return result(state, EnterpriseLabExperimentState.ARMED,
                            RecoveryClassification.RESTORATION_FAILED,
                            RecoveryAction.BASELINE_RESTORATION_FAILED, false,
                            restoration.reasonCode(),
                            "armed experiment remains blocked after failed baseline restoration",
                            Optional.empty());
                }
            }
            appender.append(
                    EnterpriseLabExperimentJournalEventType.EXPERIMENT_CANCELLED,
                    EnterpriseLabExperimentState.ARMED,
                    EnterpriseLabExperimentState.CANCELLED,
                    applied,
                    RollbackStatus.NOT_REQUESTED,
                    restorationStatus,
                    "INTERRUPTED_ARMED_CANCELLED",
                    "startup recovery safely cancelled an armed experiment without starting traffic",
                    false);
        }
        return verifiedResult(state.experimentId(), EnterpriseLabExperimentState.CANCELLED,
                RecoveryClassification.ARMED_CANCELLED,
                restoration == null ? RecoveryAction.NO_OP_RECONCILIATION : action(restoration),
                "INTERRUPTED_ARMED_CANCELLED",
                "armed experiment was safely cancelled and cannot resume automatically");
    }

    private ExperimentRecoveryResult recoverRollback(
            JournalDiscovery discovery,
            ReconstructedExperimentState state) {
        try (EnterpriseLabExperimentJournal journal = directory.openJournal(state.experimentId())) {
            RecoveryAppender appender = new RecoveryAppender(journal, discovery, state, clock);
            appender.append(
                    EnterpriseLabExperimentJournalEventType.ROLLBACK_REQUESTED,
                    state.lifecycle().state(),
                    EnterpriseLabExperimentState.ROLLING_BACK,
                    state.lastAppliedAllocation(),
                    RollbackStatus.REQUESTED,
                    state.restorationStatus(),
                    "INTERRUPTED_ACTIVE_ROLLBACK",
                    "startup recovery converted interrupted candidate activity into rollback",
                    false);
            return restoreAndFinishRollback(state, appender);
        }
    }

    private ExperimentRecoveryResult continueRollback(
            JournalDiscovery discovery,
            ReconstructedExperimentState state) {
        if (state.rollbackStatus() == RollbackStatus.FAILED
                || state.restorationStatus() == RestorationStatus.FAILED) {
            return result(state, EnterpriseLabExperimentState.ROLLING_BACK,
                    RecoveryClassification.RESTORATION_FAILED,
                    RecoveryAction.NONE, false,
                    "PREVIOUS_RESTORATION_FAILED",
                    "a final recorded restoration failure requires deliberate operator intervention",
                    Optional.empty());
        }
        try (EnterpriseLabExperimentJournal journal = directory.openJournal(state.experimentId())) {
            RecoveryAppender appender = new RecoveryAppender(journal, discovery, state, clock);
            if (state.restorationStatus() == RestorationStatus.SUCCEEDED) {
                RecoveryAction recoveryAction = RecoveryAction.NO_OP_RECONCILIATION;
                AllocationInspection inspection = allocationRecovery.inspect(state);
                if (!sameAsBaseline(inspection.currentAllocation(), state)) {
                    BaselineRestorationReceipt restored = restoreBaseline(state);
                    if (!restored.succeeded() || !sameAsBaseline(restored.verifiedAllocation(), state)) {
                        return result(state, EnterpriseLabExperimentState.ROLLING_BACK,
                                RecoveryClassification.RESTORATION_FAILED,
                                RecoveryAction.BASELINE_RESTORATION_FAILED, false,
                                restored.reasonCode(),
                                "previously restored rollback drifted and could not be restored again",
                                Optional.empty());
                    }
                    appender.append(
                            EnterpriseLabExperimentJournalEventType.RECOVERY_ACTION,
                            EnterpriseLabExperimentState.ROLLING_BACK,
                            EnterpriseLabExperimentState.ROLLING_BACK,
                            restored.verifiedAllocation(),
                            RollbackStatus.IN_PROGRESS,
                            RestorationStatus.SUCCEEDED,
                            "RECOVERY_ROLLBACK_DRIFT_RESTORED",
                            "startup recovery restored allocation drift after recorded rollback restoration",
                            restored.trafficActionPerformed());
                    recoveryAction = action(restored);
                }
                appender.append(
                        EnterpriseLabExperimentJournalEventType.EXPERIMENT_ROLLED_BACK,
                        EnterpriseLabExperimentState.ROLLING_BACK,
                        EnterpriseLabExperimentState.ROLLED_BACK,
                        appender.appliedAllocation(),
                        RollbackStatus.COMPLETED,
                        RestorationStatus.SUCCEEDED,
                        "INTERRUPTED_ROLLBACK_FINALIZED",
                        "startup recovery finalized the already restored rollback",
                        false);
                return verifiedResult(state.experimentId(), EnterpriseLabExperimentState.ROLLED_BACK,
                        RecoveryClassification.INTERRUPTED_ROLLED_BACK,
                        recoveryAction,
                        "INTERRUPTED_ROLLBACK_FINALIZED",
                        "interrupted rollback was finalized without candidate traffic",
                        appender.verify());
            }
            return restoreAndFinishRollback(state, appender);
        }
    }

    private ExperimentRecoveryResult restoreAndFinishRollback(
            ReconstructedExperimentState state,
            RecoveryAppender appender) {
        appender.append(
                EnterpriseLabExperimentJournalEventType.BASELINE_RESTORATION_ATTEMPTED,
                EnterpriseLabExperimentState.ROLLING_BACK,
                EnterpriseLabExperimentState.ROLLING_BACK,
                appender.appliedAllocation(),
                RollbackStatus.IN_PROGRESS,
                RestorationStatus.ATTEMPTED,
                "RECOVERY_BASELINE_RESTORATION_ATTEMPTED",
                "startup recovery attempted idempotent baseline restoration",
                false);
        BaselineRestorationReceipt restored = restoreBaseline(state);
        if (!restored.succeeded() || !sameAsBaseline(restored.verifiedAllocation(), state)) {
            appender.append(
                    EnterpriseLabExperimentJournalEventType.RECOVERY_ACTION,
                    EnterpriseLabExperimentState.ROLLING_BACK,
                    EnterpriseLabExperimentState.ROLLING_BACK,
                    appender.appliedAllocation(),
                    RollbackStatus.FAILED,
                    RestorationStatus.FAILED,
                    "RECOVERY_BASELINE_RESTORATION_FAILED",
                    "startup recovery could not verify baseline restoration",
                    restored.trafficActionPerformed());
            return result(state, EnterpriseLabExperimentState.ROLLING_BACK,
                    RecoveryClassification.RESTORATION_FAILED,
                    RecoveryAction.BASELINE_RESTORATION_FAILED, false,
                    restored.reasonCode(),
                    "interrupted experiment remains blocked after restoration failure",
                    Optional.empty());
        }
        appender.append(
                EnterpriseLabExperimentJournalEventType.BASELINE_RESTORED,
                EnterpriseLabExperimentState.ROLLING_BACK,
                EnterpriseLabExperimentState.ROLLING_BACK,
                restored.verifiedAllocation(),
                RollbackStatus.IN_PROGRESS,
                RestorationStatus.SUCCEEDED,
                "RECOVERY_BASELINE_RESTORED",
                "startup recovery verified the recorded baseline allocation",
                restored.trafficActionPerformed());
        appender.append(
                EnterpriseLabExperimentJournalEventType.EXPERIMENT_ROLLED_BACK,
                EnterpriseLabExperimentState.ROLLING_BACK,
                EnterpriseLabExperimentState.ROLLED_BACK,
                restored.verifiedAllocation(),
                RollbackStatus.COMPLETED,
                RestorationStatus.SUCCEEDED,
                "INTERRUPTED_EXPERIMENT_ROLLED_BACK",
                "startup recovery terminalized the interrupted experiment at baseline",
                false);
        return verifiedResult(state.experimentId(), EnterpriseLabExperimentState.ROLLED_BACK,
                RecoveryClassification.INTERRUPTED_ROLLED_BACK, action(restored),
                "INTERRUPTED_EXPERIMENT_ROLLED_BACK",
                "interrupted candidate activity was not resumed and baseline was verified",
                appender.verify());
    }

    private ExperimentRecoveryResult recoverCompletion(
            JournalDiscovery discovery,
            ReconstructedExperimentState state) {
        try (EnterpriseLabExperimentJournal journal = directory.openJournal(state.experimentId())) {
            RecoveryAppender appender = new RecoveryAppender(journal, discovery, state, clock);
            AllocationInspection inspection = allocationRecovery.inspect(state);
            BaselineRestorationReceipt restored = null;
            if (state.restorationStatus() != RestorationStatus.SUCCEEDED
                    || !sameAsBaseline(inspection.currentAllocation(), state)) {
                appender.append(
                        EnterpriseLabExperimentJournalEventType.BASELINE_RESTORATION_ATTEMPTED,
                        EnterpriseLabExperimentState.COMPLETING,
                        EnterpriseLabExperimentState.COMPLETING,
                        state.lastAppliedAllocation(),
                        RollbackStatus.NOT_REQUESTED,
                        RestorationStatus.ATTEMPTED,
                        "RECOVERY_COMPLETION_RESTORATION_ATTEMPTED",
                        "startup recovery continued the interrupted completion restoration",
                        false);
                restored = restoreBaseline(state);
                if (!restored.succeeded() || !sameAsBaseline(restored.verifiedAllocation(), state)) {
                    appender.append(
                            EnterpriseLabExperimentJournalEventType.RECOVERY_ACTION,
                            EnterpriseLabExperimentState.COMPLETING,
                            EnterpriseLabExperimentState.COMPLETING,
                            state.lastAppliedAllocation(),
                            RollbackStatus.NOT_REQUESTED,
                            RestorationStatus.FAILED,
                            "RECOVERY_COMPLETION_RESTORATION_FAILED",
                            "startup recovery could not verify completion baseline restoration",
                            restored.trafficActionPerformed());
                    return result(state, EnterpriseLabExperimentState.COMPLETING,
                            RecoveryClassification.RESTORATION_FAILED,
                            RecoveryAction.BASELINE_RESTORATION_FAILED, false,
                            restored.reasonCode(),
                            "completion remains blocked after restoration failure", Optional.empty());
                }
                appender.append(
                        EnterpriseLabExperimentJournalEventType.BASELINE_RESTORED,
                        EnterpriseLabExperimentState.COMPLETING,
                        EnterpriseLabExperimentState.COMPLETING,
                        restored.verifiedAllocation(),
                        RollbackStatus.NOT_REQUESTED,
                        RestorationStatus.SUCCEEDED,
                        "RECOVERY_COMPLETION_BASELINE_RESTORED",
                        "startup recovery verified the completion baseline",
                        restored.trafficActionPerformed());
            }
            appender.append(
                    EnterpriseLabExperimentJournalEventType.EXPERIMENT_COMPLETED,
                    EnterpriseLabExperimentState.COMPLETING,
                    EnterpriseLabExperimentState.COMPLETED,
                    appender.appliedAllocation(),
                    RollbackStatus.NOT_REQUESTED,
                    RestorationStatus.SUCCEEDED,
                    "INTERRUPTED_COMPLETION_FINALIZED",
                    "startup recovery finalized completion after baseline verification",
                    false);
            return verifiedResult(state.experimentId(), EnterpriseLabExperimentState.COMPLETED,
                    RecoveryClassification.COMPLETION_FINALIZED,
                    restored == null ? RecoveryAction.NO_OP_RECONCILIATION : action(restored),
                    "INTERRUPTED_COMPLETION_FINALIZED",
                    "interrupted completion was terminalized without resuming traffic",
                    appender.verify());
        }
    }

    private ExperimentRecoveryResult verifiedResult(
            String experimentId,
            EnterpriseLabExperimentState expectedState,
            RecoveryClassification classification,
            RecoveryAction action,
            String code,
            String message) {
        return verifiedResult(experimentId, expectedState, classification, action, code, message,
                directory.verify(experimentId));
    }

    private ExperimentRecoveryResult verifiedResult(
            String experimentId,
            EnterpriseLabExperimentState expectedState,
            RecoveryClassification classification,
            RecoveryAction action,
            String code,
            String message,
            EnterpriseLabExperimentJournalVerifier.VerificationResult verification) {
        ReplayResult replay = new EnterpriseLabExperimentJournalReplayEngine().replay(verification);
        if (replay.outcome() != EnterpriseLabExperimentJournalReplayEngine.Outcome.RECONSTRUCTED
                || replay.reconstructedState().orElseThrow().lifecycle().state() != expectedState) {
            String replayEvidence = verification.outcome() != Outcome.VALID
                    ? verification.classification().name() + ":"
                            + verification.findings().get(0).code()
                    : replay.findings().isEmpty()
                            ? replay.classification().name()
                            : replay.classification().name() + ":" + replay.findings().get(0).code();
            return new ExperimentRecoveryResult(
                    replay.journalId(), Optional.of(experimentId), Optional.empty(),
                    Optional.of(expectedState), RecoveryClassification.RECOVERY_EVIDENCE_INVALID,
                    action, false, "RECOVERY_EVIDENCE_INVALID",
                    "recovery append failed exact terminal replay: " + replayEvidence,
                    Optional.empty());
        }
        ReconstructedExperimentState reconstructed = replay.reconstructedState().orElseThrow();
        return result(reconstructed, expectedState, classification, action, true, code, message, Optional.empty());
    }

    private ExperimentRecoveryResult quarantineOrFail(
            JournalDiscovery discovery,
            String sourceClassification) {
        if (!discovery.journalId().matches("journal-v1-[0-9a-f]{64}")) {
            return new ExperimentRecoveryResult(
                    discovery.journalId(), Optional.empty(), Optional.empty(), Optional.empty(),
                    RecoveryClassification.UNRESOLVED_NAMESPACE_ENTRY,
                    RecoveryAction.NONE, false, "UNRECOGNIZED_NAMESPACE_ENTRY",
                    "unrecognized controlled-namespace content was preserved and admission failed closed",
                    Optional.empty());
        }
        try {
            QuarantineRecord quarantine = directory.quarantine(
                    discovery, clock, "STARTUP_RECOVERY_REJECTED");
            return new ExperimentRecoveryResult(
                    discovery.journalId(), discovery.experimentId(), Optional.empty(), Optional.empty(),
                    RecoveryClassification.QUARANTINED,
                    RecoveryAction.QUARANTINE_MOVE, false, sourceClassification,
                    "invalid or ambiguous journal bytes were preserved in quarantine and require intervention",
                    Optional.of(quarantine));
        } catch (RuntimeException exception) {
            return new ExperimentRecoveryResult(
                    discovery.journalId(), discovery.experimentId(), Optional.empty(), Optional.empty(),
                    RecoveryClassification.QUARANTINE_FAILED,
                    RecoveryAction.NONE, false, "QUARANTINE_FAILED",
                    "journal remained preserved but could not be atomically quarantined",
                    Optional.empty());
        }
    }

    private BaselineRestorationReceipt restoreBaseline(ReconstructedExperimentState state) {
        try {
            return allocationRecovery.restoreBaseline(state, RECOVERY_REASON);
        } catch (RuntimeException exception) {
            throw new RestorationAttemptException(exception);
        }
    }

    private static ExperimentRecoveryResult blocked(
            ReconstructedExperimentState state,
            String code,
            String message) {
        return result(state, state.lifecycle().state(), RecoveryClassification.RECONCILIATION_FAILED,
                RecoveryAction.NONE, false, code, message, Optional.empty());
    }

    private static ExperimentRecoveryResult result(
            ReconstructedExperimentState state,
            EnterpriseLabExperimentState finalState,
            RecoveryClassification classification,
            RecoveryAction action,
            boolean admissionSafe,
            String code,
            String message,
            Optional<QuarantineRecord> quarantine) {
        return new ExperimentRecoveryResult(
                state.journalId(), Optional.of(state.experimentId()),
                Optional.of(state.lifecycle().state()), Optional.of(finalState),
                classification, action, admissionSafe, code, message, quarantine);
    }

    private static boolean sameAsBaseline(
            EnterpriseLabLoopbackAllocationSnapshot actual,
            ReconstructedExperimentState state) {
        return actual.scenarioId().equals(state.scenarioId())
                && EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                        actual.allocations(), state.baselineAllocation().allocations());
    }

    private static RecoveryAction action(BaselineRestorationReceipt receipt) {
        if (!receipt.succeeded()) {
            return RecoveryAction.BASELINE_RESTORATION_FAILED;
        }
        return receipt.trafficActionPerformed()
                ? RecoveryAction.BASELINE_RESTORATION_SUCCEEDED
                : RecoveryAction.NO_OP_RECONCILIATION;
    }

    private static final class RecoveryAppender {
        private final EnterpriseLabExperimentJournal journal;
        private final ReconstructedExperimentState reconstructed;
        private final Clock clock;
        private EnterpriseLabExperimentJournalEvent previous;
        private EnterpriseLabLoopbackAllocationSnapshot appliedAllocation;

        private RecoveryAppender(
                EnterpriseLabExperimentJournal journal,
                JournalDiscovery discovery,
                ReconstructedExperimentState reconstructed,
                Clock clock) {
            this.journal = journal;
            this.reconstructed = reconstructed;
            this.clock = clock;
            this.previous = discovery.verification().orElseThrow().verifiedEvents().get(
                    discovery.verification().orElseThrow().verifiedEvents().size() - 1);
            this.appliedAllocation = reconstructed.lastAppliedAllocation();
        }

        private void append(
                EnterpriseLabExperimentJournalEventType type,
                EnterpriseLabExperimentState before,
                EnterpriseLabExperimentState after,
                EnterpriseLabLoopbackAllocationSnapshot applied,
                RollbackStatus rollback,
                RestorationStatus restoration,
                String code,
                String message,
                boolean trafficActionPerformed) {
            Checkpoint checkpoint = new Checkpoint(
                    reconstructed.configuration(),
                    reconstructed.baselineAllocation(),
                    reconstructed.candidateAllocation(),
                    applied,
                    reconstructed.lifecycle().requestCount(),
                    reconstructed.lifecycle().evidenceCount(),
                    reconstructed.lifecycle().completedHoldDownCycles(),
                    rollback,
                    restoration);
            Instant supplied = clock.instant();
            Instant occurredAt = supplied.isBefore(previous.occurredAt())
                    ? previous.occurredAt() : supplied;
            EnterpriseLabExperimentJournalEvent event = EnterpriseLabExperimentJournalEvent.create(
                    Clock.fixed(occurredAt, ZoneOffset.UTC),
                    new Draft(
                            previous.sequence() + 1,
                            reconstructed.experimentId(),
                            reconstructed.scenarioId(),
                            type,
                            before,
                            after,
                            previous.logicalCycle(),
                            reconstructed.configuration().configurationFingerprint(),
                            reconstructed.configuration().decisionFingerprint(),
                            EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(
                                    reconstructed.baselineAllocation()),
                            reconstructed.candidateAllocation()
                                    .map(EnterpriseLabExperimentJournalReplayPayload::allocationFingerprint)
                                    .orElse(EnterpriseLabExperimentJournalEvent.NO_FINGERPRINT),
                            EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(applied),
                            new Reason(code, message),
                            previous.currentEntryFingerprint(),
                            Map.of(
                                    "source", "startup-reconciliation",
                                    "trafficActionPerformed", Boolean.toString(trafficActionPerformed)),
                            EnterpriseLabExperimentJournalReplayPayload.encode(checkpoint)));
            journal.append(event);
            previous = event;
            appliedAllocation = applied;
        }

        private EnterpriseLabLoopbackAllocationSnapshot appliedAllocation() {
            return appliedAllocation;
        }

        private EnterpriseLabExperimentJournalVerifier.VerificationResult verify() {
            return journal.verify();
        }
    }

    public interface AllocationRecoveryPort {
        AllocationInspection inspect(ReconstructedExperimentState reconstructedState);

        BaselineRestorationReceipt restoreBaseline(
                ReconstructedExperimentState reconstructedState,
                String reason);
    }

    public record AllocationInspection(
            EnterpriseLabLoopbackAllocationSnapshot currentAllocation,
            String reasonCode) {
        public AllocationInspection {
            currentAllocation = Objects.requireNonNull(
                    currentAllocation, "currentAllocation cannot be null");
            reasonCode = requireCode(reasonCode);
        }
    }

    public record BaselineRestorationReceipt(
            boolean succeeded,
            boolean trafficActionPerformed,
            EnterpriseLabLoopbackAllocationSnapshot verifiedAllocation,
            String reasonCode) {
        public BaselineRestorationReceipt {
            verifiedAllocation = Objects.requireNonNull(
                    verifiedAllocation, "verifiedAllocation cannot be null");
            reasonCode = requireCode(reasonCode);
            if (!succeeded && trafficActionPerformed) {
                throw new IllegalArgumentException(
                        "failed restoration cannot claim a successful traffic action");
            }
        }
    }

    public enum RecoveryClassification {
        TERMINAL_PRESERVED,
        TERMINAL_DRIFT_RESTORED,
        ARMED_CANCELLED,
        INTERRUPTED_ROLLED_BACK,
        COMPLETION_FINALIZED,
        RESTORATION_FAILED,
        RECONCILIATION_FAILED,
        QUARANTINED,
        QUARANTINE_FAILED,
        UNRESOLVED_NAMESPACE_ENTRY,
        RECOVERY_EVIDENCE_INVALID
    }

    public enum RecoveryAction {
        NONE,
        REPLAY_ONLY,
        NO_OP_RECONCILIATION,
        BASELINE_RESTORATION_ATTEMPTED,
        BASELINE_RESTORATION_SUCCEEDED,
        BASELINE_RESTORATION_FAILED,
        QUARANTINE_MOVE
    }

    public record ExperimentRecoveryResult(
            String journalId,
            Optional<String> experimentId,
            Optional<EnterpriseLabExperimentState> recoveredState,
            Optional<EnterpriseLabExperimentState> finalState,
            RecoveryClassification classification,
            RecoveryAction action,
            boolean safeForNewExperimentAdmission,
            String reasonCode,
            String reason,
            Optional<QuarantineRecord> quarantine) {
        public ExperimentRecoveryResult {
            journalId = requireText(journalId, "journalId", 96);
            experimentId = Objects.requireNonNull(experimentId, "experimentId cannot be null");
            recoveredState = Objects.requireNonNull(recoveredState, "recoveredState cannot be null");
            finalState = Objects.requireNonNull(finalState, "finalState cannot be null");
            classification = Objects.requireNonNull(classification, "classification cannot be null");
            action = Objects.requireNonNull(action, "action cannot be null");
            reasonCode = requireCode(reasonCode);
            reason = requireText(reason, "reason", 256);
            quarantine = Objects.requireNonNull(quarantine, "quarantine cannot be null");
            if (classification == RecoveryClassification.QUARANTINED
                    != quarantine.isPresent()) {
                throw new IllegalArgumentException("quarantine evidence must match classification");
            }
        }
    }

    public record RecoveryReport(
            String schemaVersion,
            boolean admissionAllowed,
            String reasonCode,
            Instant startedAt,
            Instant completedAt,
            List<ExperimentRecoveryResult> experiments) {
        public static final String SCHEMA_VERSION = "enterprise-lab-startup-recovery-report/v1";

        public RecoveryReport {
            if (!SCHEMA_VERSION.equals(schemaVersion)) {
                throw new IllegalArgumentException("unsupported recovery report schemaVersion");
            }
            reasonCode = requireCode(reasonCode);
            startedAt = Objects.requireNonNull(startedAt, "startedAt cannot be null");
            completedAt = Objects.requireNonNull(completedAt, "completedAt cannot be null");
            experiments = List.copyOf(Objects.requireNonNull(experiments, "experiments cannot be null"));
            if (completedAt.isBefore(startedAt)
                    || experiments.size() > EnterpriseLabExperimentJournalDirectory.HARD_MAX_DISCOVERED_JOURNALS
                    || admissionAllowed != experiments.stream()
                            .allMatch(ExperimentRecoveryResult::safeForNewExperimentAdmission)) {
                throw new IllegalArgumentException("recovery report bounds or admission summary are inconsistent");
            }
        }
    }

    private static String requireCode(String value) {
        if (value == null || !value.matches("[A-Z0-9][A-Z0-9_.:-]{0,63}")) {
            throw new IllegalArgumentException("reason code must be bounded canonical text");
        }
        return value;
    }

    private static final class RestorationAttemptException extends RuntimeException {
        private RestorationAttemptException(RuntimeException cause) {
            super(cause);
        }
    }

    private static String requireText(String value, String fieldName, int maximumLength) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " must be non-blank, trimmed, and bounded");
        }
        return value;
    }
}
