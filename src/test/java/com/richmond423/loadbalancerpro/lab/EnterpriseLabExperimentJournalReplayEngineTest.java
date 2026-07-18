package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournal.SyncPolicy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalEvent.Draft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalEvent.Reason;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayEngine.Classification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayEngine.Outcome;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayEngine.ReplayLimits;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayEngine.ReplayResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.Checkpoint;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.ConfigurationEvidence;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.RestorationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayPayload.RollbackStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalVerifier.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabExperimentJournalReplayEngineTest {
    private static final Instant BASE_TIME = Instant.parse("2026-07-17T05:00:00Z");
    private static final String EXPERIMENT_ID = "replay-experiment";
    private static final String SCENARIO_ID = "stable-steady-state";
    private static final String CONFIGURATION_FINGERPRINT = "a".repeat(64);
    private static final String DECISION_FINGERPRINT = "b".repeat(64);
    private static final EnterpriseLabExperimentJournalCodec CODEC =
            new EnterpriseLabExperimentJournalCodec();

    private static final ConfigurationEvidence CONFIGURATION = new ConfigurationEvidence(
            CONFIGURATION_FINGERPRINT,
            DECISION_FINGERPRINT,
            100,
            60_000,
            2,
            2,
            EnterpriseLabExperimentRollbackPolicy.localLabDefaults(),
            AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT,
            true,
            BASE_TIME,
            BASE_TIME.plusSeconds(120));
    private static final EnterpriseLabLoopbackAllocationSnapshot BASELINE = allocation(
            0, "baseline-decision", Kind.BASELINE, Map.of("backend-a", 0.5, "backend-b", 0.5));
    private static final EnterpriseLabLoopbackAllocationSnapshot CANDIDATE = allocation(
            1, "candidate-decision", Kind.CANDIDATE, Map.of("backend-a", 0.75, "backend-b", 0.25));
    private static final EnterpriseLabLoopbackAllocationSnapshot RESTORED = allocation(
            2, "baseline-decision", Kind.RESTORED_BASELINE, Map.of("backend-a", 0.5, "backend-b", 0.5));

    @TempDir
    Path tempDirectory;

    @Test
    void replayPayloadRoundTripsStrictlyAndAllocationFingerprintIgnoresMapInsertionOrder() {
        Map<String, Double> reversed = new LinkedHashMap<>();
        reversed.put("backend-b", 0.5);
        reversed.put("backend-a", 0.5);
        EnterpriseLabLoopbackAllocationSnapshot equivalent = allocation(
                0, "baseline-decision", Kind.BASELINE, reversed);
        Checkpoint checkpoint = checkpoint(BASELINE, 0, 0, 0,
                RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED);

        JsonNode encoded = EnterpriseLabExperimentJournalReplayPayload.encode(checkpoint);

        assertEquals(checkpoint, EnterpriseLabExperimentJournalReplayPayload.decode(encoded));
        assertEquals(
                EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(BASELINE),
                EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(equivalent));
        ObjectNode unknown = (ObjectNode) encoded.deepCopy();
        unknown.put("futureField", true);
        assertEquals(EnterpriseLabExperimentJournalReplayPayload.Failure.MALFORMED_PAYLOAD,
                assertThrows(EnterpriseLabExperimentJournalReplayPayload.PayloadException.class,
                        () -> EnterpriseLabExperimentJournalReplayPayload.decode(unknown)).failure());
    }

    @Test
    void replaysARealVerifiedCompletedJournalWithoutMutatingForensicBytes() throws Exception {
        List<EnterpriseLabExperimentJournalEvent> events = completedChain();
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabMutationTestAuthority.ownedDirectory(tempDirectory);
        try (EnterpriseLabExperimentJournal journal = directory.openJournal(
                EXPERIMENT_ID, SyncPolicy.FORCE_DATA_AND_METADATA)) {
            events.forEach(journal::append);
        }
        Path journalFile = journalFile(tempDirectory);
        byte[] before = Files.readAllBytes(journalFile);

        ReplayResult result = directory.replay(EXPERIMENT_ID);

        assertEquals(Outcome.RECONSTRUCTED, result.outcome());
        var state = result.reconstructedState().orElseThrow();
        assertEquals(EnterpriseLabExperimentState.COMPLETED, state.lifecycle().state());
        assertTrue(state.lifecycle().terminal());
        assertFalse(state.lifecycle().candidateAllocationActive());
        assertEquals(2, state.lifecycle().requestCount());
        assertEquals(2, state.lifecycle().evidenceCount());
        assertEquals(2, state.lifecycle().completedHoldDownCycles());
        assertEquals(RESTORED, state.lastAppliedAllocation());
        assertEquals(RestorationStatus.SUCCEEDED, state.restorationStatus());
        assertEquals(EnterpriseLabExperimentState.COMPLETED,
                state.terminalRecord().orElseThrow().terminalState());
        assertEquals(events.size(), state.latestSequence());
        assertArrayEquals(before, Files.readAllBytes(journalFile));
    }

    @Test
    void identicalVerifiedInputReplaysDeterministicallyAndIdempotently() {
        VerificationResult source = valid(completedChain());
        EnterpriseLabExperimentJournalReplayEngine engine =
                new EnterpriseLabExperimentJournalReplayEngine();

        ReplayResult first = engine.replay(source);
        ReplayResult second = engine.replay(source);

        assertEquals(first, second);
        assertEquals(first.reconstructedState().orElseThrow().contentFingerprint(),
                second.reconstructedState().orElseThrow().contentFingerprint());
        assertTrue(first.operationCount() > source.verifiedEvents().size());
    }

    @Test
    void reconstructsCandidateApplyRunningHoldingAndRollbackCrashBoundaries() {
        List<EnterpriseLabExperimentJournalEvent> completed = completedChain();
        var applied = replay(completed.subList(0, 2));
        assertEquals(EnterpriseLabExperimentState.ARMED, applied.lifecycle().state());
        assertTrue(applied.lifecycle().candidateAllocationActive());
        assertEquals(CANDIDATE, applied.lastAppliedAllocation());

        var running = replay(completed.subList(0, 4));
        assertEquals(EnterpriseLabExperimentState.RUNNING, running.lifecycle().state());
        assertEquals(Optional.of(BASE_TIME.plusSeconds(3)), running.lifecycle().startedAt());
        assertEquals(2, running.lifecycle().requestCount());

        var holding = replay(completed.subList(0, 6));
        assertEquals(EnterpriseLabExperimentState.HOLDING, holding.lifecycle().state());
        assertEquals(1, holding.lifecycle().completedHoldDownCycles());

        List<EnterpriseLabExperimentJournalEvent> rollback = rolledBackChain();
        var rollingBack = replay(rollback.subList(0, 5));
        assertEquals(EnterpriseLabExperimentState.ROLLING_BACK, rollingBack.lifecycle().state());
        assertEquals(RollbackStatus.IN_PROGRESS, rollingBack.rollbackStatus());
        assertEquals(RestorationStatus.ATTEMPTED, rollingBack.restorationStatus());
        assertTrue(rollingBack.lifecycle().candidateAllocationActive());
    }

    @Test
    void reconstructsRolledBackTerminalEvidenceAndVerifiedBaseline() {
        var state = replay(rolledBackChain());

        assertEquals(EnterpriseLabExperimentState.ROLLED_BACK, state.lifecycle().state());
        assertEquals(RollbackStatus.COMPLETED, state.rollbackStatus());
        assertEquals(RestorationStatus.SUCCEEDED, state.restorationStatus());
        assertEquals(RESTORED, state.lastAppliedAllocation());
        assertEquals(EnterpriseLabExperimentState.ROLLED_BACK,
                state.terminalRecord().orElseThrow().terminalState());
    }

    @Test
    void postTerminalRecoveryEvidenceCannotRewriteTheOriginalTerminalRecord() {
        List<EnterpriseLabExperimentJournalEvent> events = new ArrayList<>(completedChain());
        EnterpriseLabExperimentJournalEvent terminal = events.get(events.size() - 1);
        add(events, EnterpriseLabExperimentJournalEventType.RECOVERY_ACTION,
                EnterpriseLabExperimentState.COMPLETED, EnterpriseLabExperimentState.COMPLETED,
                checkpoint(RESTORED, 2, 2, 2,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.SUCCEEDED));

        var state = replay(events);

        assertEquals(terminal.sequence(), state.terminalRecord().orElseThrow().journalSequence());
        assertEquals(terminal.currentEntryFingerprint(),
                state.terminalRecord().orElseThrow().journalFingerprint());
        assertEquals(events.size(), state.latestSequence());
    }

    @Test
    void refusesPartialTailInvalidUnavailableAndEmptySourcesWithoutPartialState() {
        List<EnterpriseLabExperimentJournalEvent> events = completedChain();
        long bytes = encodedBytes(events);
        VerificationResult partial = VerificationResult.recoverableTail(
                journalId(), events, bytes, 10, bytes + 10);
        VerificationResult invalid = VerificationResult.invalid(
                journalId(),
                new EnterpriseLabExperimentJournalVerifier.Finding(
                        EnterpriseLabExperimentJournalVerifier.Classification.MALFORMED_ENTRY,
                        1, 0, "MALFORMED_ENTRY", "malformed source"),
                0, 0, 1);
        VerificationResult empty = VerificationResult.valid(journalId(), List.of(), 0);

        assertRejected(partial, Classification.SOURCE_NOT_EXACTLY_VALID);
        assertRejected(invalid, Classification.SOURCE_NOT_EXACTLY_VALID);
        assertRejected(VerificationResult.unavailable(journalId()),
                Classification.SOURCE_NOT_EXACTLY_VALID);
        assertRejected(empty, Classification.EMPTY_JOURNAL);
    }

    @Test
    void rejectsUnsupportedAndMalformedReplayPayloads() {
        Checkpoint armed = checkpoint(BASELINE, 0, 0, 0,
                RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED);
        ObjectNode unsupported = (ObjectNode) EnterpriseLabExperimentJournalReplayPayload
                .encode(armed).deepCopy();
        unsupported.put("replaySchemaVersion", "enterprise-lab-experiment-replay-checkpoint/v2");
        assertEquals(Classification.UNSUPPORTED_REPLAY_PAYLOAD,
                replaySinglePayload(unsupported, armed).classification());

        ObjectNode malformed = (ObjectNode) EnterpriseLabExperimentJournalReplayPayload
                .encode(armed).deepCopy();
        malformed.remove("configuration");
        assertEquals(Classification.MALFORMED_REPLAY_PAYLOAD,
                replaySinglePayload(malformed, armed).classification());
    }

    @Test
    void rejectsEnvelopeFingerprintAndScenarioReferenceMismatches() {
        Checkpoint armed = checkpoint(BASELINE, 0, 0, 0,
                RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED);
        EnterpriseLabExperimentJournalEvent wrongFingerprint = event(
                1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT,
                EnterpriseLabExperimentJournalEventType.EXPERIMENT_ARMED,
                EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.ARMED,
                armed, EnterpriseLabExperimentJournalReplayPayload.encode(armed), "c".repeat(64), SCENARIO_ID);
        assertEquals(Classification.FINGERPRINT_REFERENCE_MISMATCH,
                new EnterpriseLabExperimentJournalReplayEngine()
                        .replay(valid(List.of(wrongFingerprint))).classification());

        EnterpriseLabExperimentJournalEvent wrongScenario = event(
                1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT,
                EnterpriseLabExperimentJournalEventType.EXPERIMENT_ARMED,
                EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.ARMED,
                armed, EnterpriseLabExperimentJournalReplayPayload.encode(armed),
                EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(BASELINE),
                "degraded-backend");
        assertEquals(Classification.IDENTITY_MISMATCH,
                new EnterpriseLabExperimentJournalReplayEngine()
                        .replay(valid(List.of(wrongScenario))).classification());
    }

    @Test
    void rejectsConfigurationBaselineAndCandidateMutationWithinAChain() {
        List<EnterpriseLabExperimentJournalEvent> configurationChange = new ArrayList<>();
        add(configurationChange, EnterpriseLabExperimentJournalEventType.EXPERIMENT_ARMED,
                EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.ARMED,
                checkpoint(BASELINE, 0, 0, 0,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        ConfigurationEvidence changedConfiguration = new ConfigurationEvidence(
                CONFIGURATION_FINGERPRINT, DECISION_FINGERPRINT, 99, 60_000, 2, 2,
                EnterpriseLabExperimentRollbackPolicy.localLabDefaults(),
                AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, true,
                BASE_TIME, BASE_TIME.plusSeconds(120));
        add(configurationChange, EnterpriseLabExperimentJournalEventType.RECOVERY_ACTION,
                EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.ARMED,
                checkpoint(changedConfiguration, BASELINE, CANDIDATE, BASELINE,
                        0, 0, 0, RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        assertEquals(Classification.CONFIGURATION_CHANGED,
                new EnterpriseLabExperimentJournalReplayEngine()
                        .replay(valid(configurationChange)).classification());

        EnterpriseLabLoopbackAllocationSnapshot changedBaseline = allocation(
                0, "baseline-decision", Kind.BASELINE,
                Map.of("backend-a", 0.6, "backend-b", 0.4));
        List<EnterpriseLabExperimentJournalEvent> baselineChange = new ArrayList<>();
        add(baselineChange, EnterpriseLabExperimentJournalEventType.EXPERIMENT_ARMED,
                EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.ARMED,
                checkpoint(BASELINE, 0, 0, 0,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(baselineChange, EnterpriseLabExperimentJournalEventType.CANDIDATE_ALLOCATION_APPLIED,
                EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.ARMED,
                checkpoint(CONFIGURATION, changedBaseline, CANDIDATE, CANDIDATE,
                        0, 0, 0, RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        assertEquals(Classification.ALLOCATION_CHANGED,
                new EnterpriseLabExperimentJournalReplayEngine()
                        .replay(valid(baselineChange)).classification());
    }

    @Test
    void rejectsCounterAndRecoveryStatusRegression() {
        List<EnterpriseLabExperimentJournalEvent> running = new ArrayList<>(completedChain().subList(0, 4));
        add(running, EnterpriseLabExperimentJournalEventType.OBSERVATION_CHECKPOINT,
                EnterpriseLabExperimentState.RUNNING, EnterpriseLabExperimentState.RUNNING,
                checkpoint(CANDIDATE, 1, 1, 0,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        assertEquals(Classification.COUNTER_REGRESSION,
                new EnterpriseLabExperimentJournalReplayEngine().replay(valid(running)).classification());

        List<EnterpriseLabExperimentJournalEvent> rollback =
                new ArrayList<>(rolledBackChain().subList(0, 5));
        add(rollback, EnterpriseLabExperimentJournalEventType.RECOVERY_ACTION,
                EnterpriseLabExperimentState.ROLLING_BACK, EnterpriseLabExperimentState.ROLLING_BACK,
                checkpoint(CANDIDATE, 0, 0, 0,
                        RollbackStatus.IN_PROGRESS, RestorationStatus.NOT_ATTEMPTED));
        assertEquals(Classification.COUNTER_REGRESSION,
                new EnterpriseLabExperimentJournalReplayEngine().replay(valid(rollback)).classification());
    }

    @Test
    void rejectsJournalEventsWhosePayloadSemanticsContradictTheirTypeOrState() {
        List<EnterpriseLabExperimentJournalEvent> events =
                new ArrayList<>(completedChain().subList(0, 3));
        add(events, EnterpriseLabExperimentJournalEventType.CANDIDATE_ALLOCATION_APPLIED,
                EnterpriseLabExperimentState.RUNNING, EnterpriseLabExperimentState.RUNNING,
                checkpoint(CANDIDATE, 0, 0, 0,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));

        ReplayResult result = new EnterpriseLabExperimentJournalReplayEngine().replay(valid(events));

        assertEquals(Classification.EVENT_SEMANTIC_VIOLATION, result.classification());
        assertTrue(result.reconstructedState().isEmpty());
    }

    @Test
    void rejectsForgedValidResultWhoseEventsDoNotFormTheVerifiedLifecycleChain() {
        List<EnterpriseLabExperimentJournalEvent> events = new ArrayList<>();
        add(events, EnterpriseLabExperimentJournalEventType.EXPERIMENT_ARMED,
                EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.ARMED,
                checkpoint(BASELINE, 0, 0, 0,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.OBSERVATION_CHECKPOINT,
                EnterpriseLabExperimentState.RUNNING, EnterpriseLabExperimentState.RUNNING,
                checkpoint(CANDIDATE, 0, 0, 0,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));

        assertEquals(Classification.SOURCE_CHAIN_INCONSISTENT,
                new EnterpriseLabExperimentJournalReplayEngine().replay(valid(events)).classification());
    }

    @Test
    void enforcesConfiguredEventByteAndOperationBoundsBeforeExposingState() {
        VerificationResult source = valid(completedChain());
        assertEquals(Classification.EVENT_LIMIT_EXCEEDED,
                new EnterpriseLabExperimentJournalReplayEngine(new ReplayLimits(
                        1, EnterpriseLabExperimentJournalDirectory.HARD_MAX_JOURNAL_BYTES,
                        EnterpriseLabExperimentJournalReplayEngine.HARD_MAX_OPERATIONS))
                        .replay(source).classification());
        assertEquals(Classification.SOURCE_BYTE_LIMIT_EXCEEDED,
                new EnterpriseLabExperimentJournalReplayEngine(new ReplayLimits(
                        EnterpriseLabExperimentJournalDirectory.HARD_MAX_JOURNAL_ENTRIES,
                        1, EnterpriseLabExperimentJournalReplayEngine.HARD_MAX_OPERATIONS))
                        .replay(source).classification());
        assertEquals(Classification.OPERATION_LIMIT_EXCEEDED,
                new EnterpriseLabExperimentJournalReplayEngine(new ReplayLimits(
                        EnterpriseLabExperimentJournalDirectory.HARD_MAX_JOURNAL_ENTRIES,
                        EnterpriseLabExperimentJournalDirectory.HARD_MAX_JOURNAL_BYTES, 1))
                        .replay(source).classification());
    }

    @Test
    void reconstructedLifecycleAndAllocationCollectionsRemainImmutable() {
        var state = replay(completedChain());

        assertThrows(UnsupportedOperationException.class,
                () -> state.lifecycle().transitions().add(state.lifecycle().transitions().get(0)));
        assertThrows(UnsupportedOperationException.class,
                () -> state.baselineAllocation().allocations().put("backend-c", 0.0));
        assertThrows(IllegalArgumentException.class, () -> new EnterpriseLabExperimentJournalReplayEngine(
                new ReplayLimits(0, 1, 1)));
    }

    private static List<EnterpriseLabExperimentJournalEvent> completedChain() {
        List<EnterpriseLabExperimentJournalEvent> events = new ArrayList<>();
        add(events, EnterpriseLabExperimentJournalEventType.EXPERIMENT_ARMED,
                EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.ARMED,
                checkpoint(BASELINE, 0, 0, 0,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.CANDIDATE_ALLOCATION_APPLIED,
                EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.ARMED,
                checkpoint(CANDIDATE, 0, 0, 0,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.EXPERIMENT_STARTED,
                EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.RUNNING,
                checkpoint(CANDIDATE, 0, 0, 0,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.OBSERVATION_CHECKPOINT,
                EnterpriseLabExperimentState.RUNNING, EnterpriseLabExperimentState.RUNNING,
                checkpoint(CANDIDATE, 2, 2, 0,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.LIFECYCLE_TRANSITION,
                EnterpriseLabExperimentState.RUNNING, EnterpriseLabExperimentState.HOLDING,
                checkpoint(CANDIDATE, 2, 2, 0,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.HOLD_EVALUATED,
                EnterpriseLabExperimentState.HOLDING, EnterpriseLabExperimentState.HOLDING,
                checkpoint(CANDIDATE, 2, 2, 1,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.HOLD_EVALUATED,
                EnterpriseLabExperimentState.HOLDING, EnterpriseLabExperimentState.HOLDING,
                checkpoint(CANDIDATE, 2, 2, 2,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.LIFECYCLE_TRANSITION,
                EnterpriseLabExperimentState.HOLDING, EnterpriseLabExperimentState.COMPLETING,
                checkpoint(CANDIDATE, 2, 2, 2,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.BASELINE_RESTORATION_ATTEMPTED,
                EnterpriseLabExperimentState.COMPLETING, EnterpriseLabExperimentState.COMPLETING,
                checkpoint(CANDIDATE, 2, 2, 2,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.BASELINE_RESTORED,
                EnterpriseLabExperimentState.COMPLETING, EnterpriseLabExperimentState.COMPLETING,
                checkpoint(RESTORED, 2, 2, 2,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.SUCCEEDED));
        add(events, EnterpriseLabExperimentJournalEventType.EXPERIMENT_COMPLETED,
                EnterpriseLabExperimentState.COMPLETING, EnterpriseLabExperimentState.COMPLETED,
                checkpoint(RESTORED, 2, 2, 2,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.SUCCEEDED));
        return List.copyOf(events);
    }

    private static List<EnterpriseLabExperimentJournalEvent> rolledBackChain() {
        List<EnterpriseLabExperimentJournalEvent> events = new ArrayList<>();
        add(events, EnterpriseLabExperimentJournalEventType.EXPERIMENT_ARMED,
                EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.ARMED,
                checkpoint(BASELINE, 0, 0, 0,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.CANDIDATE_ALLOCATION_APPLIED,
                EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.ARMED,
                checkpoint(CANDIDATE, 0, 0, 0,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.EXPERIMENT_STARTED,
                EnterpriseLabExperimentState.ARMED, EnterpriseLabExperimentState.RUNNING,
                checkpoint(CANDIDATE, 0, 0, 0,
                        RollbackStatus.NOT_REQUESTED, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.ROLLBACK_REQUESTED,
                EnterpriseLabExperimentState.RUNNING, EnterpriseLabExperimentState.ROLLING_BACK,
                checkpoint(CANDIDATE, 0, 0, 0,
                        RollbackStatus.IN_PROGRESS, RestorationStatus.NOT_ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.BASELINE_RESTORATION_ATTEMPTED,
                EnterpriseLabExperimentState.ROLLING_BACK, EnterpriseLabExperimentState.ROLLING_BACK,
                checkpoint(CANDIDATE, 0, 0, 0,
                        RollbackStatus.IN_PROGRESS, RestorationStatus.ATTEMPTED));
        add(events, EnterpriseLabExperimentJournalEventType.BASELINE_RESTORED,
                EnterpriseLabExperimentState.ROLLING_BACK, EnterpriseLabExperimentState.ROLLING_BACK,
                checkpoint(RESTORED, 0, 0, 0,
                        RollbackStatus.IN_PROGRESS, RestorationStatus.SUCCEEDED));
        add(events, EnterpriseLabExperimentJournalEventType.EXPERIMENT_ROLLED_BACK,
                EnterpriseLabExperimentState.ROLLING_BACK, EnterpriseLabExperimentState.ROLLED_BACK,
                checkpoint(RESTORED, 0, 0, 0,
                        RollbackStatus.COMPLETED, RestorationStatus.SUCCEEDED));
        return List.copyOf(events);
    }

    private static EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState replay(
            List<EnterpriseLabExperimentJournalEvent> events) {
        ReplayResult result = new EnterpriseLabExperimentJournalReplayEngine().replay(valid(events));
        assertEquals(Outcome.RECONSTRUCTED, result.outcome(), () -> result.findings().toString());
        return result.reconstructedState().orElseThrow();
    }

    private static ReplayResult replaySinglePayload(JsonNode payload, Checkpoint references) {
        EnterpriseLabExperimentJournalEvent event = event(
                1, EnterpriseLabExperimentJournalEvent.GENESIS_FINGERPRINT,
                EnterpriseLabExperimentJournalEventType.EXPERIMENT_ARMED,
                EnterpriseLabExperimentState.IDLE, EnterpriseLabExperimentState.ARMED,
                references, payload,
                EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(BASELINE), SCENARIO_ID);
        return new EnterpriseLabExperimentJournalReplayEngine().replay(valid(List.of(event)));
    }

    private static void assertRejected(VerificationResult source, Classification classification) {
        ReplayResult result = new EnterpriseLabExperimentJournalReplayEngine().replay(source);
        assertEquals(Outcome.REJECTED, result.outcome());
        assertEquals(classification, result.classification());
        assertTrue(result.reconstructedState().isEmpty());
        assertEquals(1, result.findings().size());
    }

    private static Checkpoint checkpoint(
            EnterpriseLabLoopbackAllocationSnapshot applied,
            int requests,
            int observations,
            int holdCycles,
            RollbackStatus rollback,
            RestorationStatus restoration) {
        return checkpoint(CONFIGURATION, BASELINE, CANDIDATE, applied,
                requests, observations, holdCycles, rollback, restoration);
    }

    private static Checkpoint checkpoint(
            ConfigurationEvidence configuration,
            EnterpriseLabLoopbackAllocationSnapshot baseline,
            EnterpriseLabLoopbackAllocationSnapshot candidate,
            EnterpriseLabLoopbackAllocationSnapshot applied,
            int requests,
            int observations,
            int holdCycles,
            RollbackStatus rollback,
            RestorationStatus restoration) {
        return new Checkpoint(
                configuration, baseline, Optional.of(candidate), applied,
                requests, observations, holdCycles, rollback, restoration);
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
        events.add(event(
                sequence, previous, type, before, after, checkpoint,
                EnterpriseLabExperimentJournalReplayPayload.encode(checkpoint),
                EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(
                        checkpoint.baselineAllocation()),
                SCENARIO_ID));
    }

    private static EnterpriseLabExperimentJournalEvent event(
            long sequence,
            String previous,
            EnterpriseLabExperimentJournalEventType type,
            EnterpriseLabExperimentState before,
            EnterpriseLabExperimentState after,
            Checkpoint checkpoint,
            JsonNode payload,
            String baselineFingerprint,
            String scenarioId) {
        return EnterpriseLabExperimentJournalEvent.create(
                Clock.fixed(BASE_TIME.plusSeconds(sequence), ZoneOffset.UTC),
                new Draft(
                        sequence,
                        EXPERIMENT_ID,
                        scenarioId,
                        type,
                        before,
                        after,
                        sequence,
                        checkpoint.configuration().configurationFingerprint(),
                        checkpoint.configuration().decisionFingerprint(),
                        baselineFingerprint,
                        checkpoint.candidateAllocation()
                                .map(EnterpriseLabExperimentJournalReplayPayload::allocationFingerprint)
                                .orElse(EnterpriseLabExperimentJournalEvent.NO_FINGERPRINT),
                        EnterpriseLabExperimentJournalReplayPayload.allocationFingerprint(
                                checkpoint.appliedAllocation()),
                        new Reason("EVENT_" + sequence, "journal event " + sequence),
                        previous,
                        Map.of("source", "replay-test"),
                        payload));
    }

    private static VerificationResult valid(List<EnterpriseLabExperimentJournalEvent> events) {
        return VerificationResult.valid(journalId(), events, encodedBytes(events));
    }

    private static long encodedBytes(List<EnterpriseLabExperimentJournalEvent> events) {
        return events.stream().mapToLong(event -> CODEC.encode(event).length + 1L).sum();
    }

    private static String journalId() {
        return "journal-v1-" + "f".repeat(64);
    }

    private static EnterpriseLabLoopbackAllocationSnapshot allocation(
            long revision,
            String sourceDecision,
            Kind kind,
            Map<String, Double> shares) {
        return new EnterpriseLabLoopbackAllocationSnapshot(
                EnterpriseLabLoopbackAllocationSnapshot.SCHEMA_VERSION,
                SCENARIO_ID,
                revision,
                sourceDecision,
                kind,
                shares);
    }

    private static Path journalFile(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
