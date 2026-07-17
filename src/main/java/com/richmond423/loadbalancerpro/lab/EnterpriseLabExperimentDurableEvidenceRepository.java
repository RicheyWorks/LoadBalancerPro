package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalEvent.Draft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalEvent.Reason;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.Checkpoint;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.ConfigurationEvidence;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.RestorationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.RollbackStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.LifecycleSnapshot;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Force-synchronized durable projection of the existing operator lifecycle.
 * It does not own traffic or create a second lifecycle; it records already
 * accepted immutable lifecycle/allocation facts in replay-valid order.
 */
public final class EnterpriseLabExperimentDurableEvidenceRepository implements AutoCloseable {
    public static final int MAX_OPEN_JOURNALS = 1;

    private final EnterpriseLabExperimentJournalDirectory directory;
    private final EnterpriseLabExperimentRecoveryGate recoveryGate;
    private final Clock clock;
    private final Map<String, LiveJournal> openJournals = new LinkedHashMap<>();
    private boolean closed;

    public EnterpriseLabExperimentDurableEvidenceRepository(
            EnterpriseLabExperimentJournalDirectory directory,
            EnterpriseLabExperimentRecoveryGate recoveryGate,
            Clock clock) {
        this.directory = Objects.requireNonNull(directory, "directory cannot be null");
        this.recoveryGate = Objects.requireNonNull(recoveryGate, "recoveryGate cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    public synchronized void record(
            EnterpriseLabExperimentConfiguration configuration,
            LifecycleSnapshot lifecycle,
            EnterpriseLabLoopbackAllocationSnapshot currentAllocation,
            String operatorRequestId,
            String operation,
            Instant occurredAt,
            EnterpriseLabExperimentState stateBefore,
            EnterpriseLabExperimentState stateAfter,
            boolean trafficActionPerformed,
            String reason) {
        requireOpen();
        EnterpriseLabExperimentConfiguration safeConfiguration = Objects.requireNonNull(
                configuration, "configuration cannot be null");
        LifecycleSnapshot safeLifecycle = Objects.requireNonNull(lifecycle, "lifecycle cannot be null");
        EnterpriseLabLoopbackAllocationSnapshot safeAllocation = Objects.requireNonNull(
                currentAllocation, "currentAllocation cannot be null");
        Instant safeOccurredAt = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        EnterpriseLabExperimentState safeBefore = Objects.requireNonNull(
                stateBefore, "stateBefore cannot be null");
        EnterpriseLabExperimentState safeAfter = Objects.requireNonNull(
                stateAfter, "stateAfter cannot be null");
        String safeRequestId = requireId(operatorRequestId, "operatorRequestId");
        String safeOperation = requireText(operation, "operation", 64);
        String safeReason = requireText(reason, "reason", 256);
        if (!safeConfiguration.experimentId().equals(safeLifecycle.experimentId())
                || !safeConfiguration.scenarioId().equals(safeAllocation.scenarioId())
                || safeLifecycle.state() != safeAfter) {
            throw new IllegalArgumentException("durable evidence components must describe the accepted current state");
        }

        LiveJournal live = openJournals.get(safeConfiguration.experimentId());
        if (live == null) {
            if (openJournals.size() >= MAX_OPEN_JOURNALS) {
                throw new IllegalStateException("only one active durable experiment journal is permitted");
            }
            if (safeBefore != EnterpriseLabExperimentState.IDLE) {
                throw new IllegalStateException("a live journal must begin at the experiment arm boundary");
            }
            live = new LiveJournal(
                    safeConfiguration,
                    directory.openJournal(safeConfiguration.experimentId()),
                    expectedCandidate(safeConfiguration));
            openJournals.put(safeConfiguration.experimentId(), live);
        }

        try {
            project(live, safeLifecycle, safeAllocation, safeRequestId, safeOperation,
                    safeOccurredAt, safeBefore, safeAfter, trafficActionPerformed, safeReason);
            if (safeAfter.terminal()) {
                live.close();
                openJournals.remove(safeConfiguration.experimentId());
            }
        } catch (RuntimeException exception) {
            recoveryGate.fail("DURABLE_APPEND_FAILED");
            throw exception;
        }
    }

    public synchronized EnterpriseLabExperimentJournalVerifier.VerificationResult verify(String experimentId) {
        requireOpen();
        return directory.verify(requireId(experimentId, "experimentId"));
    }

    public synchronized EnterpriseLabExperimentJournalReplayEngine.ReplayResult replay(String experimentId) {
        requireOpen();
        return directory.replay(requireId(experimentId, "experimentId"));
    }

    public synchronized List<EnterpriseLabExperimentJournalDirectory.JournalDiscovery> discover() {
        requireOpen();
        return directory.discover();
    }

    public synchronized EnterpriseLabExperimentJournalDirectory.CompactionResult compactTerminal(
            String experimentId) {
        requireOpen();
        return directory.compactTerminal(
                requireId(experimentId, "experimentId"), clock, "OPERATOR_REQUESTED");
    }

    public synchronized List<EnterpriseLabExperimentTerminalManifest> compactedManifests() {
        requireOpen();
        return directory.compactedManifests();
    }

    public synchronized List<EnterpriseLabExperimentJournalDirectory.QuarantineMetadata>
            quarantineMetadata() {
        requireOpen();
        return directory.quarantineMetadata();
    }

    public synchronized EnterpriseLabExperimentJournalDirectory.RetentionReport enforceRetention(
            int maximumTerminalJournals,
            boolean dryRun) {
        requireOpen();
        return directory.enforceRetention(
                new EnterpriseLabExperimentJournalDirectory.RetentionPolicy(maximumTerminalJournals),
                dryRun,
                clock);
    }

    public EnterpriseLabExperimentRecoveryGate.AdmissionStatus recoveryStatus() {
        return recoveryGate.admissionStatus();
    }

    EnterpriseLabExperimentJournalDirectory directory() {
        return directory;
    }

    synchronized void simulateProcessInterruption() {
        closeWriters();
        closed = true;
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closeWriters();
            closed = true;
        }
    }

    private void project(
            LiveJournal live,
            LifecycleSnapshot lifecycle,
            EnterpriseLabLoopbackAllocationSnapshot currentAllocation,
            String requestId,
            String operation,
            Instant occurredAt,
            EnterpriseLabExperimentState before,
            EnterpriseLabExperimentState after,
            boolean trafficActionPerformed,
            String reason) {
        if (live.previous == null) {
            EnterpriseLabExperimentJournalEventType initial = after == EnterpriseLabExperimentState.ARMED
                    ? EnterpriseLabExperimentJournalEventType.EXPERIMENT_ARMED
                    : EnterpriseLabExperimentJournalEventType.EXPERIMENT_REJECTED;
            live.append(initial, before, after, currentAllocation,
                    lifecycle.requestCount(), lifecycle.evidenceCount(), lifecycle.completedHoldDownCycles(),
                    RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED,
                    code(operation, "RECORDED"), reason, requestId, trafficActionPerformed, occurredAt);
            return;
        }

        if (before == EnterpriseLabExperimentState.RUNNING
                && after == EnterpriseLabExperimentState.HOLDING) {
            int priorHoldCycles = live.lastCheckpoint.completedHoldDownCycles();
            live.append(
                    EnterpriseLabExperimentJournalEventType.LIFECYCLE_TRANSITION,
                    before, after, currentAllocation,
                    lifecycle.requestCount(), lifecycle.evidenceCount(), priorHoldCycles,
                    RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED,
                    code(operation, after.name()), reason,
                    requestId, trafficActionPerformed, occurredAt);
            appendHoldCycles(live, lifecycle, requestId, occurredAt);
            return;
        }
        appendProgress(live, lifecycle, currentAllocation, requestId, operation,
                occurredAt, before, after, trafficActionPerformed, reason);
        if (before == after) {
            return;
        }
        if (before == EnterpriseLabExperimentState.ARMED
                && after == EnterpriseLabExperimentState.RUNNING) {
            live.append(
                    EnterpriseLabExperimentJournalEventType.CANDIDATE_ALLOCATION_APPLIED,
                    EnterpriseLabExperimentState.ARMED,
                    EnterpriseLabExperimentState.ARMED,
                    currentAllocation,
                    lifecycle.requestCount(), lifecycle.evidenceCount(),
                    lifecycle.completedHoldDownCycles(),
                    RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED,
                    "CANDIDATE_ALLOCATION_APPLIED",
                    "accepted start installed the bounded loopback candidate allocation",
                    requestId, trafficActionPerformed, occurredAt);
            live.append(
                    EnterpriseLabExperimentJournalEventType.EXPERIMENT_STARTED,
                    EnterpriseLabExperimentState.ARMED,
                    EnterpriseLabExperimentState.RUNNING,
                    currentAllocation,
                    lifecycle.requestCount(), lifecycle.evidenceCount(),
                    lifecycle.completedHoldDownCycles(),
                    RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED,
                    "EXPERIMENT_STARTED", reason, requestId, trafficActionPerformed, occurredAt);
            return;
        }
        if (after == EnterpriseLabExperimentState.ROLLED_BACK) {
            appendRollback(live, lifecycle, currentAllocation, requestId, occurredAt,
                    before, trafficActionPerformed, reason);
            return;
        }
        if (after == EnterpriseLabExperimentState.COMPLETED) {
            appendCompletion(live, lifecycle, currentAllocation, requestId, occurredAt,
                    before, trafficActionPerformed, reason);
            return;
        }
        EnterpriseLabExperimentJournalEventType type = transitionType(before, after);
        live.append(type, before, after, currentAllocation,
                lifecycle.requestCount(), lifecycle.evidenceCount(), lifecycle.completedHoldDownCycles(),
                RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED,
                code(operation, after.name()), reason, requestId, trafficActionPerformed, occurredAt);
    }

    private void appendProgress(
            LiveJournal live,
            LifecycleSnapshot lifecycle,
            EnterpriseLabLoopbackAllocationSnapshot currentAllocation,
            String requestId,
            String operation,
            Instant occurredAt,
            EnterpriseLabExperimentState before,
            EnterpriseLabExperimentState after,
            boolean trafficActionPerformed,
            String reason) {
        Checkpoint previousCheckpoint = live.lastCheckpoint;
        if (previousCheckpoint == null) {
            return;
        }
        boolean countersChanged = lifecycle.requestCount() != previousCheckpoint.requestCount()
                || lifecycle.evidenceCount() != previousCheckpoint.observationCount();
        if (countersChanged) {
            if (before != after || (after != EnterpriseLabExperimentState.RUNNING
                    && after != EnterpriseLabExperimentState.HOLDING)) {
                throw new IllegalStateException(
                        "request counter change must be recorded before a separate lifecycle transition");
            }
            live.append(
                    EnterpriseLabExperimentJournalEventType.OBSERVATION_CHECKPOINT,
                    after, after, currentAllocation,
                    lifecycle.requestCount(), lifecycle.evidenceCount(),
                    previousCheckpoint.completedHoldDownCycles(),
                    RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED,
                    "OBSERVATION_CHECKPOINT_RECORDED", reason,
                    requestId, trafficActionPerformed, occurredAt);
        }
        appendHoldCycles(live, lifecycle, requestId, occurredAt);
    }

    private void appendHoldCycles(
            LiveJournal live,
            LifecycleSnapshot lifecycle,
            String requestId,
            Instant occurredAt) {
        int recordedHoldCycles = live.lastCheckpoint.completedHoldDownCycles();
        while (recordedHoldCycles < lifecycle.completedHoldDownCycles()) {
            recordedHoldCycles++;
            live.append(
                    EnterpriseLabExperimentJournalEventType.HOLD_EVALUATED,
                    EnterpriseLabExperimentState.HOLDING,
                    EnterpriseLabExperimentState.HOLDING,
                    live.lastCheckpoint.appliedAllocation(),
                    lifecycle.requestCount(), lifecycle.evidenceCount(), recordedHoldCycles,
                    RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED,
                    "HOLD_CYCLE_RECORDED", "bounded hold-down evaluation was durably recorded",
                    requestId, false, occurredAt);
        }
    }

    private void appendRollback(
            LiveJournal live,
            LifecycleSnapshot lifecycle,
            EnterpriseLabLoopbackAllocationSnapshot restored,
            String requestId,
            Instant occurredAt,
            EnterpriseLabExperimentState before,
            boolean trafficActionPerformed,
            String reason) {
        EnterpriseLabLoopbackAllocationSnapshot candidate = live.lastCheckpoint.appliedAllocation();
        if (candidate.kind() != Kind.CANDIDATE) {
            candidate = live.expectedCandidate;
        }
        live.append(EnterpriseLabExperimentJournalEventType.ROLLBACK_REQUESTED,
                before, EnterpriseLabExperimentState.ROLLING_BACK, candidate,
                lifecycle.requestCount(), lifecycle.evidenceCount(), lifecycle.completedHoldDownCycles(),
                RollbackStatus.REQUESTED, RestorationStatus.NOT_ATTEMPTED,
                "ROLLBACK_REQUESTED", reason, requestId, false, occurredAt);
        live.append(EnterpriseLabExperimentJournalEventType.BASELINE_RESTORATION_ATTEMPTED,
                EnterpriseLabExperimentState.ROLLING_BACK, EnterpriseLabExperimentState.ROLLING_BACK,
                candidate, lifecycle.requestCount(), lifecycle.evidenceCount(),
                lifecycle.completedHoldDownCycles(), RollbackStatus.IN_PROGRESS,
                RestorationStatus.ATTEMPTED, "BASELINE_RESTORATION_ATTEMPTED",
                "accepted rollback attempted recorded baseline restoration",
                requestId, false, occurredAt);
        live.append(EnterpriseLabExperimentJournalEventType.BASELINE_RESTORED,
                EnterpriseLabExperimentState.ROLLING_BACK, EnterpriseLabExperimentState.ROLLING_BACK,
                restored, lifecycle.requestCount(), lifecycle.evidenceCount(),
                lifecycle.completedHoldDownCycles(), RollbackStatus.IN_PROGRESS,
                RestorationStatus.SUCCEEDED, "BASELINE_RESTORED",
                "accepted rollback verified the recorded baseline allocation",
                requestId, trafficActionPerformed, occurredAt);
        live.append(EnterpriseLabExperimentJournalEventType.EXPERIMENT_ROLLED_BACK,
                EnterpriseLabExperimentState.ROLLING_BACK, EnterpriseLabExperimentState.ROLLED_BACK,
                restored, lifecycle.requestCount(), lifecycle.evidenceCount(),
                lifecycle.completedHoldDownCycles(), RollbackStatus.COMPLETED,
                RestorationStatus.SUCCEEDED, "EXPERIMENT_ROLLED_BACK", reason,
                requestId, false, occurredAt);
    }

    private void appendCompletion(
            LiveJournal live,
            LifecycleSnapshot lifecycle,
            EnterpriseLabLoopbackAllocationSnapshot restored,
            String requestId,
            Instant occurredAt,
            EnterpriseLabExperimentState before,
            boolean trafficActionPerformed,
            String reason) {
        EnterpriseLabLoopbackAllocationSnapshot candidate = live.lastCheckpoint.appliedAllocation();
        if (candidate.kind() != Kind.CANDIDATE) {
            candidate = live.expectedCandidate;
        }
        if (before != EnterpriseLabExperimentState.COMPLETING) {
            live.append(EnterpriseLabExperimentJournalEventType.LIFECYCLE_TRANSITION,
                    before, EnterpriseLabExperimentState.COMPLETING, candidate,
                    lifecycle.requestCount(), lifecycle.evidenceCount(), lifecycle.completedHoldDownCycles(),
                    RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED,
                    "COMPLETION_STARTED", "accepted evaluation entered bounded completion",
                    requestId, false, occurredAt);
        }
        live.append(EnterpriseLabExperimentJournalEventType.BASELINE_RESTORATION_ATTEMPTED,
                EnterpriseLabExperimentState.COMPLETING, EnterpriseLabExperimentState.COMPLETING,
                candidate, lifecycle.requestCount(), lifecycle.evidenceCount(),
                lifecycle.completedHoldDownCycles(), RollbackStatus.NOT_REQUESTED,
                RestorationStatus.ATTEMPTED, "COMPLETION_BASELINE_RESTORATION_ATTEMPTED",
                "accepted completion attempted recorded baseline restoration",
                requestId, false, occurredAt);
        live.append(EnterpriseLabExperimentJournalEventType.BASELINE_RESTORED,
                EnterpriseLabExperimentState.COMPLETING, EnterpriseLabExperimentState.COMPLETING,
                restored, lifecycle.requestCount(), lifecycle.evidenceCount(),
                lifecycle.completedHoldDownCycles(), RollbackStatus.NOT_REQUESTED,
                RestorationStatus.SUCCEEDED, "COMPLETION_BASELINE_RESTORED",
                "accepted completion verified the recorded baseline allocation",
                requestId, trafficActionPerformed, occurredAt);
        live.append(EnterpriseLabExperimentJournalEventType.EXPERIMENT_COMPLETED,
                EnterpriseLabExperimentState.COMPLETING, EnterpriseLabExperimentState.COMPLETED,
                restored, lifecycle.requestCount(), lifecycle.evidenceCount(),
                lifecycle.completedHoldDownCycles(), RollbackStatus.NOT_REQUESTED,
                RestorationStatus.SUCCEEDED, "EXPERIMENT_COMPLETED", reason,
                requestId, false, occurredAt);
    }

    private static EnterpriseLabExperimentJournalEventType transitionType(
            EnterpriseLabExperimentState before,
            EnterpriseLabExperimentState after) {
        if (before == EnterpriseLabExperimentState.ARMED
                && after == EnterpriseLabExperimentState.CANCELLED) {
            return EnterpriseLabExperimentJournalEventType.EXPERIMENT_CANCELLED;
        }
        if (before == EnterpriseLabExperimentState.ARMED
                && after == EnterpriseLabExperimentState.FAILED) {
            return EnterpriseLabExperimentJournalEventType.EXPERIMENT_FAILED;
        }
        return EnterpriseLabExperimentJournalEventType.LIFECYCLE_TRANSITION;
    }

    private void closeWriters() {
        RuntimeException failure = null;
        for (LiveJournal live : openJournals.values()) {
            try {
                live.close();
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                }
            }
        }
        openJournals.clear();
        if (failure != null) {
            throw failure;
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("durable evidence repository is closed");
        }
    }

    private static EnterpriseLabLoopbackAllocationSnapshot expectedCandidate(
            EnterpriseLabExperimentConfiguration configuration) {
        return new EnterpriseLabLoopbackAllocationSnapshot(
                EnterpriseLabLoopbackAllocationSnapshot.SCHEMA_VERSION,
                configuration.scenarioId(),
                configuration.baselineSnapshot().revision() + 1,
                configuration.candidateDecisionId(),
                Kind.CANDIDATE,
                configuration.candidateDecision().decision().effectiveAllocations());
    }

    private static ConfigurationEvidence configurationEvidence(
            EnterpriseLabExperimentConfiguration configuration) {
        return new ConfigurationEvidence(
                configuration.contentFingerprint(),
                configuration.candidateDecision().contentFingerprint(),
                configuration.maximumRequestCount(),
                configuration.maximumDuration().toMillis(),
                configuration.minimumEvidenceCount(),
                configuration.holdDownCycles(),
                configuration.rollbackPolicy(),
                configuration.operatingMode(),
                configuration.operatorAuthorized(),
                configuration.createdAt(),
                configuration.expiresAt());
    }

    private static String code(String operation, String suffix) {
        String canonical = operation.toUpperCase(java.util.Locale.ROOT)
                .replace('-', '_').replace(':', '_');
        canonical = canonical.replaceAll("[^A-Z0-9_]", "_");
        String combined = canonical + "_" + suffix;
        return combined.length() <= 64 ? combined : combined.substring(0, 64);
    }

    private static String requireId(String value, String fieldName) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > 128 || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException(fieldName + " must be a bounded canonical identifier");
        }
        return value;
    }

    private static String requireText(String value, String fieldName, int maximumLength) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " must be trimmed and bounded");
        }
        return value;
    }

    private static final class LiveJournal implements AutoCloseable {
        private final EnterpriseLabExperimentConfiguration configuration;
        private final ConfigurationEvidence configurationEvidence;
        private final EnterpriseLabExperimentJournal journal;
        private final EnterpriseLabLoopbackAllocationSnapshot expectedCandidate;
        private EnterpriseLabExperimentJournalEvent previous;
        private Checkpoint lastCheckpoint;

        private LiveJournal(
                EnterpriseLabExperimentConfiguration configuration,
                EnterpriseLabExperimentJournal journal,
                EnterpriseLabLoopbackAllocationSnapshot expectedCandidate) {
            this.configuration = configuration;
            this.configurationEvidence = configurationEvidence(configuration);
            this.journal = journal;
            this.expectedCandidate = expectedCandidate;
        }

        private void append(
                EnterpriseLabExperimentJournalEventType type,
                EnterpriseLabExperimentState before,
                EnterpriseLabExperimentState after,
                EnterpriseLabLoopbackAllocationSnapshot applied,
                int requestCount,
                int observationCount,
                int completedHoldDownCycles,
                RollbackStatus rollback,
                RestorationStatus restoration,
                String code,
                String reason,
                String requestId,
                boolean trafficActionPerformed,
                Instant occurredAt) {
            Checkpoint checkpoint = new Checkpoint(
                    configurationEvidence,
                    configuration.baselineSnapshot(),
                    Optional.of(expectedCandidate),
                    applied,
                    requestCount,
                    observationCount,
                    completedHoldDownCycles,
                    rollback,
                    restoration);
            Instant monotonicTime = previous == null || !occurredAt.isBefore(previous.occurredAt())
                    ? occurredAt : previous.occurredAt();
            long sequence = previous == null ? 1 : previous.sequence() + 1;
            long logicalCycle = previous == null
                    ? completedHoldDownCycles
                    : Math.max(previous.logicalCycle(), completedHoldDownCycles);
            String predecessor = previous == null
                    ? EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT
                    : previous.currentEntryFingerprint();
            EnterpriseLabExperimentJournalEvent event = EnterpriseLabExperimentJournalEvent.create(
                    Clock.fixed(monotonicTime, ZoneOffset.UTC),
                    new Draft(
                            sequence,
                            configuration.experimentId(),
                            configuration.scenarioId(),
                            type,
                            before,
                            after,
                            logicalCycle,
                            configurationEvidence.configurationFingerprint(),
                            configurationEvidence.decisionFingerprint(),
                            EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(
                                    configuration.baselineSnapshot()),
                            EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(expectedCandidate),
                            EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(applied),
                            new Reason(code, reason),
                            predecessor,
                            Map.of(
                                    "operatorRequestId", requestId,
                                    "source", "live-operator-journal",
                                    "trafficActionPerformed", Boolean.toString(trafficActionPerformed)),
                            EnterpriseLabExperimentJournalReplayPayload.encode(checkpoint)));
            journal.append(event);
            previous = event;
            lastCheckpoint = checkpoint;
        }

        @Override
        public void close() {
            journal.close();
        }
    }
}
