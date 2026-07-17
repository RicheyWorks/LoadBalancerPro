package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.Checkpoint;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.ConfigurationEvidence;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.PayloadException;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.RestorationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.RollbackStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalVerifier.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.LifecycleSnapshot;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure bounded replay of an already verified journal chain.
 * This component has no filesystem, network, allocation-router, lifecycle-command, or append capability.
 */
public final class EnterpriseLabExperimentJournalReplayEngine {
    public static final int HARD_MAX_OPERATIONS = 1_000_000;
    public static final ReplayLimits DEFAULT_LIMITS = new ReplayLimits(
            EnterpriseLabExperimentJournalDirectory.HARD_MAX_JOURNAL_ENTRIES,
            EnterpriseLabExperimentJournalDirectory.HARD_MAX_JOURNAL_BYTES,
            HARD_MAX_OPERATIONS);

    private final ReplayLimits limits;

    public EnterpriseLabExperimentJournalReplayEngine() {
        this(DEFAULT_LIMITS);
    }

    public EnterpriseLabExperimentJournalReplayEngine(ReplayLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits cannot be null");
    }

    public ReplayResult replay(VerificationResult verification) {
        VerificationResult source = Objects.requireNonNull(verification, "verification cannot be null");
        if (source.outcome() != EnterpriseLabExperimentJournalVerifier.Outcome.VALID) {
            return rejected(source.journalId(), Classification.SOURCE_NOT_EXACTLY_VALID, 0,
                    "SOURCE_NOT_EXACTLY_VALID",
                    "replay requires a valid journal with no partial tail or verification finding");
        }
        if (source.verifiedEvents().isEmpty()) {
            return rejected(source.journalId(), Classification.EMPTY_JOURNAL, 0,
                    "EMPTY_JOURNAL", "an empty journal cannot reconstruct an experiment");
        }
        if (source.verifiedEvents().size() > limits.maximumEvents()) {
            return rejected(source.journalId(), Classification.EVENT_LIMIT_EXCEEDED, 0,
                    "EVENT_LIMIT_EXCEEDED", "verified event count exceeds the configured replay bound");
        }
        if (source.totalBytes() > limits.maximumSourceBytes()) {
            return rejected(source.journalId(), Classification.SOURCE_BYTE_LIMIT_EXCEEDED, 0,
                    "SOURCE_BYTE_LIMIT_EXCEEDED", "verified journal bytes exceed the configured replay bound");
        }

        ReplayAccumulator accumulator = new ReplayAccumulator(source.journalId(), limits.maximumOperations());
        try {
            for (EnterpriseLabExperimentJournalEvent event : source.verifiedEvents()) {
                accumulator.accept(event);
            }
            ReconstructedExperimentState state = accumulator.finish(
                    source.lastVerifiedSequence(), source.lastVerifiedFingerprint());
            return ReplayResult.reconstructed(state, accumulator.operations());
        } catch (ReplayFailure failure) {
            return rejected(source.journalId(), failure.classification(), failure.sequence(),
                    failure.code(), failure.getMessage());
        } catch (PayloadException exception) {
            Classification classification = exception.failure()
                    == EnterpriseLabExperimentJournalReplayPayload.Failure.UNSUPPORTED_VERSION
                    ? Classification.UNSUPPORTED_REPLAY_PAYLOAD
                    : Classification.MALFORMED_REPLAY_PAYLOAD;
            return rejected(source.journalId(), classification, accumulator.nextSequence(),
                    classification.name(), exception.getMessage());
        } catch (RuntimeException exception) {
            return rejected(source.journalId(), Classification.ILLEGAL_RECONSTRUCTION,
                    accumulator.nextSequence(), "ILLEGAL_RECONSTRUCTION",
                    "verified events produced an invalid reconstructed experiment state");
        }
    }

    private static ReplayResult rejected(
            String journalId,
            Classification classification,
            long sequence,
            String code,
            String message) {
        return ReplayResult.rejected(journalId,
                new Finding(classification, sequence, code, message));
    }

    private static final class ReplayAccumulator {
        private final String journalId;
        private final long maximumOperations;
        private final List<EnterpriseLabExperimentTransition> transitions = new ArrayList<>();

        private long operations;
        private EnterpriseLabExperimentJournalEvent previousEvent;
        private Checkpoint previousCheckpoint;
        private ConfigurationEvidence configuration;
        private EnterpriseLabLoopbackAllocationSnapshot baselineAllocation;
        private EnterpriseLabLoopbackAllocationSnapshot candidateAllocation;
        private Instant startedAt;
        private EnterpriseLabExperimentJournalEvent terminalEvent;
        private long previousLogicalCycle;

        private ReplayAccumulator(String journalId, long maximumOperations) {
            this.journalId = journalId;
            this.maximumOperations = maximumOperations;
        }

        private void accept(EnterpriseLabExperimentJournalEvent event) {
            charge(1, event.sequence());
            long expectedSequence = previousEvent == null ? 1 : previousEvent.sequence() + 1;
            String expectedPrevious = previousEvent == null
                    ? EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT
                    : previousEvent.currentEntryFingerprint();
            if (event.sequence() != expectedSequence
                    || !event.previousEntryFingerprint().equals(expectedPrevious)
                    || previousEvent != null
                    && (!event.experimentId().equals(previousEvent.experimentId())
                    || !event.scenarioId().equals(previousEvent.scenarioId()))) {
                fail(Classification.SOURCE_CHAIN_INCONSISTENT, event.sequence(),
                        "SOURCE_CHAIN_INCONSISTENT",
                        "verification result contains discontinuous sequence, predecessor, or identity evidence");
            }
            EnterpriseLabExperimentJournalVerifier.Classification chainFailure =
                    EnterpriseLabExperimentJournalVerifier.nextEventFailure(previousEvent, event);
            if (chainFailure != null) {
                fail(Classification.SOURCE_CHAIN_INCONSISTENT, event.sequence(),
                        "SOURCE_CHAIN_INCONSISTENT",
                        "verification result contains events inconsistent with the lifecycle chain");
            }
            if (previousEvent == null && (event.stateBefore() != EnterpriseLabExperimentState.IDLE
                    || (event.eventType() != EnterpriseLabExperimentJournalEventType.EXPERIMENT_ARMED
                    && event.eventType() != EnterpriseLabExperimentJournalEventType.EXPERIMENT_REJECTED))) {
                fail(Classification.EVENT_SEMANTIC_VIOLATION, event.sequence(),
                        "INVALID_INITIAL_EVENT",
                        "replay must begin with an armed or rejected transition from IDLE");
            }
            if (previousEvent != null && event.logicalCycle() < previousLogicalCycle) {
                fail(Classification.COUNTER_REGRESSION, event.sequence(),
                        "LOGICAL_CYCLE_REGRESSION", "logical cycle cannot move backwards during replay");
            }

            Checkpoint checkpoint = EnterpriseLabExperimentJournalReplayPayload.decode(event.payload());
            charge(1L + checkpoint.baselineAllocation().allocations().size()
                    + checkpoint.candidateAllocation().map(value -> value.allocations().size()).orElse(0)
                    + checkpoint.appliedAllocation().allocations().size(), event.sequence());
            validateEnvelopeReferences(event, checkpoint);
            validateStableEvidence(event, checkpoint);
            validateCounters(event, checkpoint);
            validateEventSemantics(event, checkpoint);
            validateStateCombination(event, checkpoint);
            reconstructTransition(event, checkpoint);

            if (startedAt == null && event.stateAfter() == EnterpriseLabExperimentState.RUNNING) {
                startedAt = event.occurredAt();
            }
            if (terminalEvent == null && event.stateAfter().terminal()) {
                terminalEvent = event;
            }
            previousEvent = event;
            previousCheckpoint = checkpoint;
            previousLogicalCycle = event.logicalCycle();
        }

        private void validateEnvelopeReferences(
                EnterpriseLabExperimentJournalEvent event,
                Checkpoint checkpoint) {
            if (!event.configurationFingerprint().equals(
                    checkpoint.configuration().configurationFingerprint())
                    || !event.decisionFingerprint().equals(
                    checkpoint.configuration().decisionFingerprint())
                    || !event.baselineAllocationFingerprint().equals(
                    EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(
                            checkpoint.baselineAllocation()))
                    || !event.appliedAllocationFingerprint().equals(
                    EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(
                            checkpoint.appliedAllocation()))) {
                fail(Classification.FINGERPRINT_REFERENCE_MISMATCH, event.sequence(),
                        "FINGERPRINT_REFERENCE_MISMATCH",
                        "journal envelope fingerprint references do not match replay payload evidence");
            }
            String expectedCandidate = checkpoint.candidateAllocation()
                    .map(EnterpriseLabExperimentJournalReplayPayload::allocationFingerprint)
                    .orElse(EnterpriseLabExperimentJournalEvent.NO_FINGERPRINT);
            if (!event.candidateAllocationFingerprint().equals(expectedCandidate)) {
                fail(Classification.FINGERPRINT_REFERENCE_MISMATCH, event.sequence(),
                        "CANDIDATE_FINGERPRINT_MISMATCH",
                        "candidate allocation fingerprint does not match replay payload evidence");
            }
            if (!event.scenarioId().equals(checkpoint.baselineAllocation().scenarioId())) {
                fail(Classification.IDENTITY_MISMATCH, event.sequence(), "SCENARIO_IDENTITY_MISMATCH",
                        "replay allocation evidence does not match the journal scenario");
            }
            if (event.occurredAt().isBefore(checkpoint.configuration().createdAt())) {
                fail(Classification.EVENT_SEMANTIC_VIOLATION, event.sequence(),
                        "EVENT_BEFORE_CONFIGURATION", "journal event predates its configuration evidence");
            }
        }

        private void validateStableEvidence(
                EnterpriseLabExperimentJournalEvent event,
                Checkpoint checkpoint) {
            if (configuration == null) {
                configuration = checkpoint.configuration();
                baselineAllocation = checkpoint.baselineAllocation();
                candidateAllocation = checkpoint.candidateAllocation().orElse(null);
                return;
            }
            if (!configuration.equals(checkpoint.configuration())) {
                fail(Classification.CONFIGURATION_CHANGED, event.sequence(), "CONFIGURATION_CHANGED",
                        "replay configuration evidence changed within one journal");
            }
            if (!baselineAllocation.equals(checkpoint.baselineAllocation())) {
                fail(Classification.ALLOCATION_CHANGED, event.sequence(), "BASELINE_ALLOCATION_CHANGED",
                        "recorded baseline allocation changed within one journal");
            }
            if (candidateAllocation == null && checkpoint.candidateAllocation().isPresent()) {
                candidateAllocation = checkpoint.candidateAllocation().orElseThrow();
            } else if (candidateAllocation != null
                    && checkpoint.candidateAllocation().filter(candidateAllocation::equals).isEmpty()) {
                fail(Classification.ALLOCATION_CHANGED, event.sequence(), "CANDIDATE_ALLOCATION_CHANGED",
                        "recorded candidate allocation changed or disappeared within one journal");
            }
        }

        private void validateCounters(
                EnterpriseLabExperimentJournalEvent event,
                Checkpoint checkpoint) {
            if (previousCheckpoint == null) {
                if (checkpoint.requestCount() != 0 || checkpoint.observationCount() != 0
                        || checkpoint.completedHoldDownCycles() != 0) {
                    fail(Classification.EVENT_SEMANTIC_VIOLATION, event.sequence(),
                            "NONZERO_INITIAL_COUNTERS", "initial replay counters must be zero");
                }
                return;
            }
            if (checkpoint.requestCount() < previousCheckpoint.requestCount()
                    || checkpoint.observationCount() < previousCheckpoint.observationCount()
                    || checkpoint.completedHoldDownCycles()
                    < previousCheckpoint.completedHoldDownCycles()) {
                fail(Classification.COUNTER_REGRESSION, event.sequence(), "COUNTER_REGRESSION",
                        "request, observation, and hold counters cannot move backwards");
            }
            if (progressRank(checkpoint.rollbackStatus())
                    < progressRank(previousCheckpoint.rollbackStatus())
                    || progressRank(checkpoint.restorationStatus())
                    < progressRank(previousCheckpoint.restorationStatus())
                    || previousCheckpoint.rollbackStatus() == RollbackStatus.COMPLETED
                    && checkpoint.rollbackStatus() != RollbackStatus.COMPLETED
                    || previousCheckpoint.rollbackStatus() == RollbackStatus.FAILED
                    && checkpoint.rollbackStatus() != RollbackStatus.FAILED
                    || previousCheckpoint.restorationStatus() == RestorationStatus.SUCCEEDED
                    && checkpoint.restorationStatus() != RestorationStatus.SUCCEEDED
                    || previousCheckpoint.restorationStatus() == RestorationStatus.FAILED
                    && checkpoint.restorationStatus() != RestorationStatus.FAILED) {
                fail(Classification.COUNTER_REGRESSION, event.sequence(), "RECOVERY_STATUS_REGRESSION",
                        "rollback and restoration status cannot move backwards or change final outcome");
            }
            boolean requestCountersChanged = checkpoint.requestCount() != previousCheckpoint.requestCount()
                    || checkpoint.observationCount() != previousCheckpoint.observationCount();
            if (requestCountersChanged
                    && event.eventType() != EnterpriseLabExperimentJournalEventType.OBSERVATION_CHECKPOINT) {
                fail(Classification.EVENT_SEMANTIC_VIOLATION, event.sequence(),
                        "COUNTER_CHANGE_WITHOUT_CHECKPOINT",
                        "request and observation counts may change only at an observation checkpoint");
            }
            boolean holdChanged = checkpoint.completedHoldDownCycles()
                    != previousCheckpoint.completedHoldDownCycles();
            if (holdChanged && (event.eventType() != EnterpriseLabExperimentJournalEventType.HOLD_EVALUATED
                    || checkpoint.completedHoldDownCycles()
                    != previousCheckpoint.completedHoldDownCycles() + 1)) {
                fail(Classification.EVENT_SEMANTIC_VIOLATION, event.sequence(),
                        "INVALID_HOLD_PROGRESS",
                        "hold progress must advance one bounded cycle at a hold evaluation");
            }
        }

        private void validateEventSemantics(
                EnterpriseLabExperimentJournalEvent event,
                Checkpoint checkpoint) {
            EnterpriseLabExperimentState before = event.stateBefore();
            EnterpriseLabExperimentState after = event.stateAfter();
            switch (event.eventType()) {
                case EXPERIMENT_ARMED -> {
                    requireTransition(event, EnterpriseLabExperimentState.IDLE,
                            EnterpriseLabExperimentState.ARMED);
                    if (!checkpoint.configuration().operatorAuthorized()
                            || checkpoint.configuration().operatingMode()
                            != AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT) {
                        semanticFailure(event, "armed replay evidence requires authorized active-experiment mode");
                    }
                }
                case EXPERIMENT_REJECTED -> requireTransition(event,
                        EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.REJECTED);
                case CANDIDATE_ALLOCATION_APPLIED -> {
                    requireSameState(event, EnterpriseLabExperimentState.ARMED);
                    requireCandidateApplied(event, checkpoint);
                }
                case EXPERIMENT_STARTED -> {
                    requireTransition(event, EnterpriseLabExperimentState.ARMED,
                            EnterpriseLabExperimentState.RUNNING);
                    requireCandidateApplied(event, checkpoint);
                }
                case OBSERVATION_CHECKPOINT -> {
                    if (before != after || (after != EnterpriseLabExperimentState.RUNNING
                            && after != EnterpriseLabExperimentState.HOLDING)) {
                        semanticFailure(event, "observation checkpoints require RUNNING or HOLDING state");
                    }
                }
                case HOLD_EVALUATED -> requireSameState(event, EnterpriseLabExperimentState.HOLDING);
                case ROLLBACK_REQUESTED -> {
                    if (after != EnterpriseLabExperimentState.ROLLING_BACK || before == after) {
                        semanticFailure(event, "rollback request must enter ROLLING_BACK");
                    }
                    requireCandidateApplied(event, checkpoint);
                }
                case BASELINE_RESTORATION_ATTEMPTED -> {
                    if (before != after || (after != EnterpriseLabExperimentState.ROLLING_BACK
                            && after != EnterpriseLabExperimentState.COMPLETING)
                            || (checkpoint.restorationStatus() != RestorationStatus.ATTEMPTED
                            && checkpoint.restorationStatus() != RestorationStatus.FAILED)) {
                        semanticFailure(event, "restoration attempt evidence is inconsistent");
                    }
                }
                case BASELINE_RESTORED -> {
                    if (before != after || (after != EnterpriseLabExperimentState.ROLLING_BACK
                            && after != EnterpriseLabExperimentState.COMPLETING)
                            || checkpoint.restorationStatus() != RestorationStatus.SUCCEEDED
                            || checkpoint.appliedAllocation().kind() == Kind.CANDIDATE) {
                        semanticFailure(event, "baseline restored evidence is inconsistent");
                    }
                }
                case EXPERIMENT_CANCELLED -> requireTransition(event,
                        EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.CANCELLED);
                case EXPERIMENT_COMPLETED -> requireTransition(event,
                        EnterpriseLabExperimentState.COMPLETING, EnterpriseLabExperimentState.COMPLETED);
                case EXPERIMENT_ROLLED_BACK -> requireTransition(event,
                        EnterpriseLabExperimentState.ROLLING_BACK, EnterpriseLabExperimentState.ROLLED_BACK);
                case EXPERIMENT_FAILED -> requireTransition(event,
                        EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.FAILED);
                case LIFECYCLE_TRANSITION -> {
                    if (before == after || !EnterpriseLabExperimentLifecycle.allowsStateChange(before, after)) {
                        semanticFailure(event, "generic lifecycle transition is not allowed by the lifecycle");
                    }
                }
                case RECOVERY_ACTION, QUARANTINE_FINDING -> {
                    if (before != after) {
                        semanticFailure(event, "recovery and quarantine evidence cannot imply a state transition");
                    }
                }
            }
        }

        private void validateStateCombination(
                EnterpriseLabExperimentJournalEvent event,
                Checkpoint checkpoint) {
            EnterpriseLabExperimentState state = event.stateAfter();
            Kind appliedKind = checkpoint.appliedAllocation().kind();
            if (state == EnterpriseLabExperimentState.IDLE && appliedKind == Kind.CANDIDATE) {
                semanticFailure(event, "inactive replay state cannot retain candidate allocation");
            }
            if (state == EnterpriseLabExperimentState.ARMED && appliedKind == Kind.CANDIDATE
                    && event.eventType()
                    != EnterpriseLabExperimentJournalEventType.CANDIDATE_ALLOCATION_APPLIED) {
                semanticFailure(event, "armed state can record candidate allocation only at its apply boundary");
            }
            if ((state == EnterpriseLabExperimentState.RUNNING || state == EnterpriseLabExperimentState.HOLDING)
                    && appliedKind != Kind.CANDIDATE) {
                semanticFailure(event, "active replay state requires the recorded candidate allocation");
            }
            if (state == EnterpriseLabExperimentState.COMPLETING && appliedKind != Kind.CANDIDATE
                    && checkpoint.restorationStatus() != RestorationStatus.SUCCEEDED) {
                semanticFailure(event, "completing state may leave candidate allocation only after restoration");
            }
            if (state.terminal() && appliedKind == Kind.CANDIDATE) {
                semanticFailure(event, "terminal replay state cannot retain candidate allocation");
            }

            RollbackStatus rollback = checkpoint.rollbackStatus();
            RestorationStatus restoration = checkpoint.restorationStatus();
            switch (state) {
                case IDLE, ARMED, RUNNING, HOLDING -> {
                    if (rollback != RollbackStatus.NOT_REQUESTED
                            || restoration != RestorationStatus.NOT_ATTEMPTED) {
                        semanticFailure(event, "pre-recovery state has impossible rollback or restoration status");
                    }
                }
                case COMPLETING -> {
                    if (rollback != RollbackStatus.NOT_REQUESTED) {
                        semanticFailure(event, "completion path cannot report rollback progress");
                    }
                }
                case ROLLING_BACK -> {
                    if (rollback != RollbackStatus.REQUESTED
                            && rollback != RollbackStatus.IN_PROGRESS
                            && rollback != RollbackStatus.FAILED) {
                        semanticFailure(event, "rolling-back state requires rollback progress evidence");
                    }
                }
                case ROLLED_BACK -> {
                    if (rollback != RollbackStatus.COMPLETED
                            || restoration != RestorationStatus.SUCCEEDED) {
                        semanticFailure(event, "rolled-back terminal state requires verified restoration");
                    }
                }
                case COMPLETED -> {
                    if (rollback != RollbackStatus.NOT_REQUESTED
                            || restoration != RestorationStatus.SUCCEEDED) {
                        semanticFailure(event, "completed state requires verified baseline restoration");
                    }
                }
                case REJECTED, FAILED, CANCELLED -> {
                    if (rollback != RollbackStatus.NOT_REQUESTED
                            || restoration != RestorationStatus.NOT_ATTEMPTED) {
                        semanticFailure(event, "inactive terminal state has impossible recovery status");
                    }
                }
            }
        }

        private void reconstructTransition(
                EnterpriseLabExperimentJournalEvent event,
                Checkpoint checkpoint) {
            if (event.stateBefore() == event.stateAfter()) {
                return;
            }
            if (!EnterpriseLabExperimentLifecycle.allowsStateChange(
                    event.stateBefore(), event.stateAfter())) {
                semanticFailure(event, "journal transition diverges from the existing lifecycle graph");
            }
            if (transitions.size() >= EnterpriseLabExperimentLifecycle.MAX_TRANSITIONS) {
                fail(Classification.TRANSITION_LIMIT_EXCEEDED, event.sequence(),
                        "TRANSITION_LIMIT_EXCEEDED",
                        "reconstructed lifecycle transition history exceeds its bounded capacity");
            }
            String previousFingerprint = transitions.isEmpty()
                    ? EnterpriseLabExperimentTransition.GENESIS_FINGERPRINT
                    : transitions.get(transitions.size() - 1).contentFingerprint();
            transitions.add(new EnterpriseLabExperimentTransition(
                    EnterpriseLabExperimentTransition.SCHEMA_VERSION,
                    transitions.size() + 1L,
                    event.experimentId(),
                    event.stateBefore(),
                    event.stateAfter(),
                    event.occurredAt(),
                    "journal-entry-" + event.sequence(),
                    event.reason().message(),
                    checkpoint.requestCount(),
                    checkpoint.observationCount(),
                    checkpoint.completedHoldDownCycles(),
                    checkpoint.appliedAllocation().revision(),
                    previousFingerprint));
        }

        private ReconstructedExperimentState finish(long latestSequence, String latestFingerprint) {
            if (previousEvent == null || previousCheckpoint == null || configuration == null
                    || baselineAllocation == null) {
                throw new IllegalStateException("replay accumulator is empty");
            }
            if (latestSequence != previousEvent.sequence()
                    || !latestFingerprint.equals(previousEvent.currentEntryFingerprint())) {
                fail(Classification.SOURCE_CHAIN_INCONSISTENT, previousEvent.sequence(),
                        "SOURCE_SUMMARY_MISMATCH",
                        "verification summary does not match the last replayed event");
            }
            String lastTransitionFingerprint = transitions.isEmpty()
                    ? EnterpriseLabExperimentTransition.GENESIS_FINGERPRINT
                    : transitions.get(transitions.size() - 1).contentFingerprint();
            EnterpriseLabExperimentState finalState = previousEvent.stateAfter();
            boolean candidateActive = !finalState.terminal()
                    && (previousCheckpoint.appliedAllocation().kind() == Kind.CANDIDATE
                    || finalState == EnterpriseLabExperimentState.RUNNING
                    || finalState == EnterpriseLabExperimentState.HOLDING
                    || finalState == EnterpriseLabExperimentState.COMPLETING
                    || finalState == EnterpriseLabExperimentState.ROLLING_BACK);
            LifecycleSnapshot lifecycle = new LifecycleSnapshot(
                    EnterpriseLabExperimentLifecycle.SNAPSHOT_SCHEMA_VERSION,
                    previousEvent.experimentId(),
                    finalState,
                    previousCheckpoint.requestCount(),
                    previousCheckpoint.observationCount(),
                    previousCheckpoint.completedHoldDownCycles(),
                    Optional.ofNullable(startedAt),
                    Optional.of(previousEvent.occurredAt()),
                    candidateActive,
                    previousCheckpoint.appliedAllocation().revision(),
                    transitions,
                    lastTransitionFingerprint,
                    finalState.terminal(),
                    previousEvent.reason().message());
            Optional<TerminalExperimentRecord> terminalRecord = Optional.ofNullable(terminalEvent)
                    .map(event -> new TerminalExperimentRecord(
                            TerminalExperimentRecord.SCHEMA_VERSION,
                            event.experimentId(),
                            event.scenarioId(),
                            event.stateAfter(),
                            event.occurredAt(),
                            event.reason(),
                            event.sequence(),
                            event.currentEntryFingerprint()));
            return ReconstructedExperimentState.create(
                    journalId,
                    previousEvent.experimentId(),
                    previousEvent.scenarioId(),
                    configuration,
                    lifecycle,
                    baselineAllocation,
                    Optional.ofNullable(candidateAllocation),
                    previousCheckpoint.appliedAllocation(),
                    previousCheckpoint.rollbackStatus(),
                    previousCheckpoint.restorationStatus(),
                    terminalRecord,
                    latestSequence,
                    latestFingerprint);
        }

        private void requireTransition(
                EnterpriseLabExperimentJournalEvent event,
                EnterpriseLabExperimentState before,
                EnterpriseLabExperimentState after) {
            if (event.stateBefore() != before || event.stateAfter() != after) {
                semanticFailure(event, "event type does not match its lifecycle transition");
            }
        }

        private void requireSameState(
                EnterpriseLabExperimentJournalEvent event,
                EnterpriseLabExperimentState state) {
            if (event.stateBefore() != state || event.stateAfter() != state) {
                semanticFailure(event, "event type does not match its lifecycle state");
            }
        }

        private void requireCandidateApplied(
                EnterpriseLabExperimentJournalEvent event,
                Checkpoint checkpoint) {
            if (checkpoint.candidateAllocation().isEmpty()
                    || checkpoint.appliedAllocation().kind() != Kind.CANDIDATE) {
                semanticFailure(event, "event requires the recorded candidate allocation");
            }
        }

        private void semanticFailure(EnterpriseLabExperimentJournalEvent event, String message) {
            fail(Classification.EVENT_SEMANTIC_VIOLATION, event.sequence(),
                    "EVENT_SEMANTIC_VIOLATION", message);
        }

        private void charge(long amount, long sequence) {
            if (amount < 0 || operations > maximumOperations - amount) {
                fail(Classification.OPERATION_LIMIT_EXCEEDED, sequence,
                        "OPERATION_LIMIT_EXCEEDED",
                        "deterministic replay operation budget was exhausted");
            }
            operations += amount;
        }

        private long operations() {
            return operations;
        }

        private long nextSequence() {
            return previousEvent == null ? 1 : previousEvent.sequence() + 1;
        }

        private static void fail(
                Classification classification,
                long sequence,
                String code,
                String message) {
            throw new ReplayFailure(classification, sequence, code, message);
        }

        private static int progressRank(RollbackStatus status) {
            return switch (status) {
                case NOT_REQUESTED -> 0;
                case REQUESTED -> 1;
                case IN_PROGRESS -> 2;
                case COMPLETED, FAILED -> 3;
            };
        }

        private static int progressRank(RestorationStatus status) {
            return switch (status) {
                case NOT_ATTEMPTED -> 0;
                case ATTEMPTED -> 1;
                case SUCCEEDED, FAILED -> 2;
            };
        }
    }

    public record ReplayLimits(int maximumEvents, long maximumSourceBytes, long maximumOperations) {
        public ReplayLimits {
            if (maximumEvents < 1
                    || maximumEvents > EnterpriseLabExperimentJournalDirectory.HARD_MAX_JOURNAL_ENTRIES) {
                throw new IllegalArgumentException("maximumEvents is outside journal bounds");
            }
            if (maximumSourceBytes < 1
                    || maximumSourceBytes > EnterpriseLabExperimentJournalDirectory.HARD_MAX_JOURNAL_BYTES) {
                throw new IllegalArgumentException("maximumSourceBytes is outside journal bounds");
            }
            if (maximumOperations < 1 || maximumOperations > HARD_MAX_OPERATIONS) {
                throw new IllegalArgumentException("maximumOperations is outside replay bounds");
            }
        }
    }

    public enum Outcome {
        RECONSTRUCTED,
        REJECTED
    }

    public enum Classification {
        RECONSTRUCTED,
        SOURCE_NOT_EXACTLY_VALID,
        EMPTY_JOURNAL,
        EVENT_LIMIT_EXCEEDED,
        SOURCE_BYTE_LIMIT_EXCEEDED,
        OPERATION_LIMIT_EXCEEDED,
        TRANSITION_LIMIT_EXCEEDED,
        UNSUPPORTED_REPLAY_PAYLOAD,
        MALFORMED_REPLAY_PAYLOAD,
        SOURCE_CHAIN_INCONSISTENT,
        IDENTITY_MISMATCH,
        FINGERPRINT_REFERENCE_MISMATCH,
        CONFIGURATION_CHANGED,
        ALLOCATION_CHANGED,
        COUNTER_REGRESSION,
        EVENT_SEMANTIC_VIOLATION,
        ILLEGAL_RECONSTRUCTION
    }

    public record Finding(
            Classification classification,
            long sequence,
            String code,
            String message) {
        public Finding {
            classification = Objects.requireNonNull(classification, "classification cannot be null");
            if (sequence < 0) {
                throw new IllegalArgumentException("finding sequence cannot be negative");
            }
            code = requireText(code, "code", 64);
            message = requireText(message, "message", 256);
        }
    }

    public record ReplayResult(
            String journalId,
            Outcome outcome,
            Classification classification,
            Optional<ReconstructedExperimentState> reconstructedState,
            List<Finding> findings,
            long operationCount) {
        public ReplayResult {
            journalId = requireText(journalId, "journalId", 96);
            outcome = Objects.requireNonNull(outcome, "outcome cannot be null");
            classification = Objects.requireNonNull(classification, "classification cannot be null");
            reconstructedState = Objects.requireNonNull(
                    reconstructedState, "reconstructedState cannot be null");
            findings = List.copyOf(Objects.requireNonNull(findings, "findings cannot be null"));
            if (operationCount < 0 || operationCount > HARD_MAX_OPERATIONS) {
                throw new IllegalArgumentException("operationCount is outside replay bounds");
            }
            if ((outcome == Outcome.RECONSTRUCTED) != reconstructedState.isPresent()
                    || (outcome == Outcome.RECONSTRUCTED) != findings.isEmpty()
                    || (outcome == Outcome.RECONSTRUCTED)
                    != (classification == Classification.RECONSTRUCTED)) {
                throw new IllegalArgumentException("replay result outcome and evidence are inconsistent");
            }
        }

        private static ReplayResult reconstructed(
                ReconstructedExperimentState state,
                long operationCount) {
            return new ReplayResult(
                    state.journalId(), Outcome.RECONSTRUCTED, Classification.RECONSTRUCTED,
                    Optional.of(state), List.of(), operationCount);
        }

        private static ReplayResult rejected(String journalId, Finding finding) {
            return new ReplayResult(
                    journalId, Outcome.REJECTED, finding.classification(),
                    Optional.empty(), List.of(finding), 0);
        }
    }

    public record ReconstructedExperimentState(
            String schemaVersion,
            String journalId,
            String experimentId,
            String scenarioId,
            ConfigurationEvidence configuration,
            LifecycleSnapshot lifecycle,
            EnterpriseLabLoopbackAllocationSnapshot baselineAllocation,
            Optional<EnterpriseLabLoopbackAllocationSnapshot> candidateAllocation,
            EnterpriseLabLoopbackAllocationSnapshot lastAppliedAllocation,
            RollbackStatus rollbackStatus,
            RestorationStatus restorationStatus,
            Optional<TerminalExperimentRecord> terminalRecord,
            long latestSequence,
            String latestFingerprint,
            String contentFingerprint) {
        public static final String SCHEMA_VERSION = "enterprise-lab-reconstructed-experiment/v1";

        public ReconstructedExperimentState {
            if (!SCHEMA_VERSION.equals(schemaVersion)) {
                throw new IllegalArgumentException("unsupported reconstructed experiment schemaVersion");
            }
            journalId = requireText(journalId, "journalId", 96);
            experimentId = requireText(experimentId, "experimentId", 128);
            scenarioId = requireText(scenarioId, "scenarioId", 128);
            configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
            lifecycle = Objects.requireNonNull(lifecycle, "lifecycle cannot be null");
            baselineAllocation = Objects.requireNonNull(
                    baselineAllocation, "baselineAllocation cannot be null");
            candidateAllocation = Objects.requireNonNull(
                    candidateAllocation, "candidateAllocation cannot be null");
            lastAppliedAllocation = Objects.requireNonNull(
                    lastAppliedAllocation, "lastAppliedAllocation cannot be null");
            rollbackStatus = Objects.requireNonNull(rollbackStatus, "rollbackStatus cannot be null");
            restorationStatus = Objects.requireNonNull(
                    restorationStatus, "restorationStatus cannot be null");
            terminalRecord = Objects.requireNonNull(terminalRecord, "terminalRecord cannot be null");
            if (!experimentId.equals(lifecycle.experimentId())
                    || !scenarioId.equals(baselineAllocation.scenarioId())
                    || !scenarioId.equals(lastAppliedAllocation.scenarioId())
                    || candidateAllocation.isPresent()
                    && !scenarioId.equals(candidateAllocation.orElseThrow().scenarioId())
                    || lifecycle.terminal() != terminalRecord.isPresent()) {
                throw new IllegalArgumentException("reconstructed components describe inconsistent identities or state");
            }
            if (latestSequence < 1 || !latestFingerprint.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("latest journal reference is invalid");
            }
            String expected = fingerprint(
                    journalId, experimentId, scenarioId, configuration, lifecycle,
                    baselineAllocation, candidateAllocation, lastAppliedAllocation,
                    rollbackStatus, restorationStatus, terminalRecord,
                    latestSequence, latestFingerprint);
            if (!expected.equals(contentFingerprint)) {
                throw new IllegalArgumentException("reconstructed contentFingerprint does not match content");
            }
        }

        private static ReconstructedExperimentState create(
                String journalId,
                String experimentId,
                String scenarioId,
                ConfigurationEvidence configuration,
                LifecycleSnapshot lifecycle,
                EnterpriseLabLoopbackAllocationSnapshot baselineAllocation,
                Optional<EnterpriseLabLoopbackAllocationSnapshot> candidateAllocation,
                EnterpriseLabLoopbackAllocationSnapshot lastAppliedAllocation,
                RollbackStatus rollbackStatus,
                RestorationStatus restorationStatus,
                Optional<TerminalExperimentRecord> terminalRecord,
                long latestSequence,
                String latestFingerprint) {
            String fingerprint = fingerprint(
                    journalId, experimentId, scenarioId, configuration, lifecycle,
                    baselineAllocation, candidateAllocation, lastAppliedAllocation,
                    rollbackStatus, restorationStatus, terminalRecord,
                    latestSequence, latestFingerprint);
            return new ReconstructedExperimentState(
                    SCHEMA_VERSION, journalId, experimentId, scenarioId, configuration, lifecycle,
                    baselineAllocation, candidateAllocation, lastAppliedAllocation,
                    rollbackStatus, restorationStatus, terminalRecord,
                    latestSequence, latestFingerprint, fingerprint);
        }

        private static String fingerprint(
                String journalId,
                String experimentId,
                String scenarioId,
                ConfigurationEvidence configuration,
                LifecycleSnapshot lifecycle,
                EnterpriseLabLoopbackAllocationSnapshot baselineAllocation,
                Optional<EnterpriseLabLoopbackAllocationSnapshot> candidateAllocation,
                EnterpriseLabLoopbackAllocationSnapshot lastAppliedAllocation,
                RollbackStatus rollbackStatus,
                RestorationStatus restorationStatus,
                Optional<TerminalExperimentRecord> terminalRecord,
                long latestSequence,
                String latestFingerprint) {
            MessageDigest digest = sha256();
            update(digest, SCHEMA_VERSION);
            update(digest, journalId);
            update(digest, experimentId);
            update(digest, scenarioId);
            update(digest, configuration.toString());
            update(digest, lifecycle.toString());
            update(digest, EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(
                    baselineAllocation));
            update(digest, candidateAllocation
                    .map(EnterpriseLabExperimentJournalReplayPayload::allocationFingerprint)
                    .orElse(EnterpriseLabExperimentJournalEvent.NO_FINGERPRINT));
            update(digest, EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(
                    lastAppliedAllocation));
            update(digest, rollbackStatus.name());
            update(digest, restorationStatus.name());
            update(digest, terminalRecord.map(TerminalExperimentRecord::contentFingerprint).orElse("ACTIVE"));
            update(digest, Long.toString(latestSequence));
            update(digest, latestFingerprint);
            return HexFormat.of().formatHex(digest.digest());
        }
    }

    public record TerminalExperimentRecord(
            String schemaVersion,
            String experimentId,
            String scenarioId,
            EnterpriseLabExperimentState terminalState,
            Instant completedAt,
            EnterpriseLabExperimentJournalEvent.Reason reason,
            long journalSequence,
            String journalFingerprint) {
        public static final String SCHEMA_VERSION = "enterprise-lab-reconstructed-terminal-record/v1";

        public TerminalExperimentRecord {
            if (!SCHEMA_VERSION.equals(schemaVersion)) {
                throw new IllegalArgumentException("unsupported terminal record schemaVersion");
            }
            experimentId = requireText(experimentId, "experimentId", 128);
            scenarioId = requireText(scenarioId, "scenarioId", 128);
            terminalState = Objects.requireNonNull(terminalState, "terminalState cannot be null");
            if (!terminalState.terminal()) {
                throw new IllegalArgumentException("terminal record requires a terminal lifecycle state");
            }
            completedAt = Objects.requireNonNull(completedAt, "completedAt cannot be null");
            reason = Objects.requireNonNull(reason, "reason cannot be null");
            if (journalSequence < 1 || !journalFingerprint.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("terminal journal reference is invalid");
            }
        }

        public String contentFingerprint() {
            MessageDigest digest = sha256();
            update(digest, schemaVersion);
            update(digest, experimentId);
            update(digest, scenarioId);
            update(digest, terminalState.name());
            update(digest, completedAt.toString());
            update(digest, reason.code());
            update(digest, reason.message());
            update(digest, Long.toString(journalSequence));
            update(digest, journalFingerprint);
            return HexFormat.of().formatHex(digest.digest());
        }
    }

    private static final class ReplayFailure extends RuntimeException {
        private final Classification classification;
        private final long sequence;
        private final String code;

        private ReplayFailure(
                Classification classification,
                long sequence,
                String code,
                String message) {
            super(message, null, false, false);
            this.classification = classification;
            this.sequence = sequence;
            this.code = code;
        }

        private Classification classification() {
            return classification;
        }

        private long sequence() {
            return sequence;
        }

        private String code() {
            return code;
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static String requireText(String value, String fieldName, int maximumLength) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " must be non-blank, trimmed, and bounded");
        }
        return value;
    }
}
