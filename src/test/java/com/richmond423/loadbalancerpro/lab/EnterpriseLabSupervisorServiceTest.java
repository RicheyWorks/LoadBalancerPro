package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.RecoveryClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorOwnership.Failure;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorOwnership.OwnershipException;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.RequestDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabSupervisorServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-19T08:00:00Z");
    private static final String APP_A = "application-a";
    private static final String APP_B = "application-b";
    private static final String OWNER_A = "a".repeat(64);
    private static final String OWNER_B = "b".repeat(64);

    @TempDir
    Path root;

    private EnterpriseLabExperimentTargetCatalog targets;
    private EnterpriseLabSupervisorProtocolCodec codec;
    private Clock clock;

    @BeforeEach
    void setUp() {
        targets = EnterpriseLabSupervisorConfiguration.approvedTargets();
        codec = new EnterpriseLabSupervisorProtocolCodec(targets);
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Test
    void initializesFingerprintChainAndCanonicalDurableState() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(ownership);
            EnterpriseLabSupervisorState state = service.state();

            assertEquals(1L, state.supervisorGeneration());
            assertEquals(1L, state.durableStateGeneration());
            assertEquals(0L, state.installedAllocation().routerGeneration());
            assertEquals(TransactionPhase.COMMITTED, state.transactionPhase());
            assertTrue(state.installedAllocation().safeDefault());
            assertEquals(state.baselineAllocation(), state.installedAllocation());
            assertEquals(state.installedAllocation().allocationFingerprint(),
                    state.previousCommittedFingerprint());
            assertFalse(state.applicationOwnerEstablished());

            EnterpriseLabSupervisorStateCodec stateCodec =
                    new EnterpriseLabSupervisorStateCodec(targets);
            byte[] encoded = stateCodec.encode(state);
            assertArrayEquals(encoded, stateCodec.encode(stateCodec.decode(encoded)));
            String changed = new String(encoded, StandardCharsets.UTF_8)
                    .replace(state.currentRecordFingerprint(), "f".repeat(64));
            assertThrows(
                    EnterpriseLabSupervisorStateCodec.CodecException.class,
                    () -> stateCodec.decode(changed.getBytes(StandardCharsets.UTF_8)));
            String unknown = new String(encoded, StandardCharsets.UTF_8)
                    .replaceFirst("\\{", "{\"unexpected\":true,");
            assertEquals(
                    EnterpriseLabSupervisorStateCodec.Failure.UNKNOWN_FIELD,
                    assertThrows(
                            EnterpriseLabSupervisorStateCodec.CodecException.class,
                            () -> stateCodec.decode(unknown.getBytes(StandardCharsets.UTF_8)))
                            .failure());
        }
    }

    @Test
    void oneOsOwnerWinsAndReleasedLockCanBeReacquired() {
        EnterpriseLabSupervisorOwnership first =
                EnterpriseLabSupervisorOwnership.acquire(root);
        try {
            OwnershipException competing = assertThrows(
                    OwnershipException.class,
                    () -> EnterpriseLabSupervisorOwnership.acquire(root));
            assertEquals(Failure.LIVE_COMPETING_SUPERVISOR, competing.failure());
        } finally {
            first.close();
        }

        try (EnterpriseLabSupervisorOwnership reacquired =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            assertTrue(reacquired.held());
        }
    }

    @Test
    void advancesVerifiedOwnerAppliesAtomicallyAndRejectsStaleApplication() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(ownership);
            EnterpriseLabSupervisorState initial = service.state();

            Response advanced = service.dispatch(advance(initial, APP_A, OWNER_A, 1L));
            assertEquals(ResponseStatus.ACCEPTED, advanced.status());
            assertTrue(advanced.actionPerformed());
            assertEquals(1L, advanced.observedApplicationGeneration());

            EnterpriseLabSupervisorState owned = service.state();
            Request apply = apply(owned, APP_A, OWNER_A, 1L, "apply-1", "transaction-1");
            Response committed = service.dispatch(apply);
            assertEquals(ResponseStatus.ACCEPTED, committed.status());
            assertTrue(committed.actionPerformed());
            assertEquals(apply.allocationFingerprint(), committed.installedFingerprint());
            assertEquals(1L, committed.routerGeneration());
            assertEquals(TransactionPhase.COMMITTED, service.state().transactionPhase());
            assertFalse(service.state().transactionIncomplete());
            assertEquals(5L, service.state().durableStateGeneration());

            Response duplicate = service.dispatch(apply);
            assertEquals(ResponseStatus.ACCEPTED, duplicate.status());
            assertFalse(duplicate.actionPerformed());
            assertEquals("DUPLICATE_REQUEST_REPLAYED", duplicate.reasonCode());

            EnterpriseLabSupervisorState afterCommit = service.state();
            Response nextOwner = service.dispatch(advance(
                    afterCommit, APP_B, OWNER_B, 2L));
            assertEquals(ResponseStatus.ACCEPTED, nextOwner.status());
            assertEquals(APP_A, service.state().previousApplicationInstanceId());
            assertEquals(OWNER_A,
                    service.state().previousApplicationOwnershipFingerprint());
            assertEquals(1L, service.state().previousApplicationGeneration());

            Request stale = apply(
                    service.state(), APP_A, OWNER_A, 1L, "apply-stale", "transaction-stale");
            Response rejected = service.dispatch(stale);
            assertEquals(ResponseStatus.REJECTED, rejected.status());
            assertEquals("STALE_APPLICATION_OWNER", rejected.reasonCode());
        }
    }

    @Test
    void cleanShutdownFencesEveryLaterMutationWithoutChangingDurableState() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(ownership);
            service.dispatch(advance(service.state(), APP_A, OWNER_A, 1L));
            EnterpriseLabSupervisorState owned = service.state();
            Request shutdown = codec.issue(new RequestDraft(
                    "shutdown-1",
                    CommandType.CLEAN_SHUTDOWN,
                    APP_A,
                    OWNER_A,
                    1L,
                    owned.supervisorInstanceId(),
                    owned.supervisorGeneration(),
                    "shutdown-transaction-1",
                    Optional.empty(),
                    AllocationPurpose.RECONCILIATION_NO_OP,
                    Optional.empty(),
                    EnterpriseLabSupervisorProtocol.NONE,
                    EnterpriseLabSupervisorProtocol.NONE,
                    NOW,
                    Map.of()));
            assertEquals(ResponseStatus.ACCEPTED, service.dispatch(shutdown).status());
            assertTrue(service.shutdownRequested());

            EnterpriseLabSupervisorState beforeRejectedMutation = service.state();
            Response rejected = service.dispatch(apply(
                    beforeRejectedMutation,
                    APP_A,
                    OWNER_A,
                    1L,
                    "apply-after-shutdown",
                    "transaction-after-shutdown"));
            assertEquals(ResponseStatus.REJECTED, rejected.status());
            assertEquals("SUPERVISOR_SHUTTING_DOWN", rejected.reasonCode());
            assertEquals(beforeRejectedMutation, service.state());
        }
    }

    @Test
    void committedInstalledAllocationSurvivesApplicationAndSupervisorRestart() {
        EnterpriseLabInstalledAllocationSnapshot committed;
        String firstInstance;
        long firstDurableGeneration;
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(ownership);
            service.dispatch(advance(service.state(), APP_A, OWNER_A, 1L));
            Response response = service.dispatch(apply(
                    service.state(), APP_A, OWNER_A, 1L,
                    "apply-persistent", "transaction-persistent"));
            assertEquals(ResponseStatus.ACCEPTED, response.status());
            committed = service.state().installedAllocation();
            firstInstance = service.state().supervisorInstanceId();
            firstDurableGeneration = service.state().durableStateGeneration();
        }

        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService restarted = service(ownership);
            assertEquals(committed, restarted.state().installedAllocation());
            assertNotEquals(firstInstance, restarted.state().supervisorInstanceId());
            assertEquals(2L, restarted.state().supervisorGeneration());
            assertEquals(firstDurableGeneration + 1L,
                    restarted.state().durableStateGeneration());
            assertEquals(1L, restarted.state().acceptedApplicationGeneration());
            assertEquals(RecoveryClassification.NOT_REQUIRED,
                    restarted.state().lastRecoveryClassification());
        }
    }

    @Test
    void restartRestoresBaselineAfterCrashFollowingInstalledApply() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(
                    ownership,
                    point -> {
                        if (point == EnterpriseLabSupervisorService.FailurePoint.AFTER_APPLY_INSTALL) {
                            throw new SimulatedCrash();
                        }
                    });
            service.dispatch(advance(service.state(), APP_A, OWNER_A, 1L));
            Response failed = service.dispatch(apply(
                    service.state(), APP_A, OWNER_A, 1L,
                    "apply-crash", "transaction-crash"));
            assertEquals(ResponseStatus.FAILED, failed.status());
            assertEquals(TransactionPhase.APPLIED, service.state().transactionPhase());
            assertTrue(service.state().transactionIncomplete());
            assertEquals(Kind.CANDIDATE,
                    service.state().installedAllocation().routingSnapshot().kind());
        }

        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService recovered = service(ownership);
            assertEquals(2L, recovered.state().supervisorGeneration());
            assertEquals(TransactionPhase.RESTORED, recovered.state().transactionPhase());
            assertFalse(recovered.state().transactionIncomplete());
            assertEquals(RecoveryClassification.BASELINE_RESTORED,
                    recovered.state().lastRecoveryClassification());
            assertEquals(Kind.RESTORED_BASELINE,
                    recovered.state().installedAllocation().routingSnapshot().kind());
            assertEquals(recovered.state().baselineAllocation().allocationFingerprint(),
                    recovered.state().installedAllocation().allocationFingerprint());
            assertEquals(2L, recovered.state().installedAllocation().routerGeneration());
        }
    }

    @Test
    void staleSupervisorFenceAndChangedDuplicateFailClosed() {
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(ownership);
            EnterpriseLabSupervisorState initial = service.state();
            Request advance = advance(initial, APP_A, OWNER_A, 1L);
            assertEquals(ResponseStatus.ACCEPTED, service.dispatch(advance).status());

            Request changedDuplicate = codec.issue(new RequestDraft(
                    advance.requestId(),
                    CommandType.ADVANCE_APPLICATION_OWNERSHIP,
                    APP_A,
                    OWNER_A,
                    1L,
                    service.state().supervisorInstanceId(),
                    service.state().supervisorGeneration(),
                    "changed-transaction",
                    Optional.empty(),
                    AllocationPurpose.RECONCILIATION_NO_OP,
                    Optional.empty(),
                    EnterpriseLabSupervisorProtocol.NONE,
                    EnterpriseLabSupervisorProtocol.NONE,
                    NOW,
                    Map.of()));
            Response duplicateRejection = service.dispatch(changedDuplicate);
            assertEquals(ResponseStatus.REJECTED, duplicateRejection.status());
            assertEquals("DUPLICATE_REQUEST_CHANGED", duplicateRejection.reasonCode());

            Request staleSupervisor = codec.issue(new RequestDraft(
                    "stale-supervisor-read",
                    CommandType.READ_STATUS,
                    "observer-app",
                    EnterpriseLabSupervisorProtocol.NONE,
                    0L,
                    initial.supervisorInstanceId(),
                    initial.supervisorGeneration() + 1L,
                    EnterpriseLabSupervisorProtocol.NONE,
                    Optional.empty(),
                    AllocationPurpose.RECONCILIATION_NO_OP,
                    Optional.empty(),
                    EnterpriseLabSupervisorProtocol.NONE,
                    EnterpriseLabSupervisorProtocol.NONE,
                    NOW,
                    Map.of()));
            Response staleRejection = service.dispatch(staleSupervisor);
            assertEquals(ResponseStatus.REJECTED, staleRejection.status());
            assertEquals("STALE_SUPERVISOR_GENERATION", staleRejection.reasonCode());
        }
    }

    @Test
    void interruptedTemporaryIsPreservedOnceAndBounded() throws Exception {
        byte[] canonical;
        Path temporary;
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service = service(ownership);
            canonical = new EnterpriseLabSupervisorStateCodec(targets)
                    .encode(service.state());
            temporary = ownership.supervisorDirectory()
                    .resolve(EnterpriseLabSupervisorStateStore.TEMPORARY_FILE_NAME);
        }
        Files.write(temporary, canonical);

        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorStateStore store =
                    new EnterpriseLabSupervisorStateStore(ownership, targets);
            var interrupted = store.interruptedEvidence().orElseThrow();
            assertTrue(interrupted.canonical());
            assertEquals(canonical.length, interrupted.bytes());
            assertFalse(Files.exists(temporary));
        }
    }

    private EnterpriseLabSupervisorService service(
            EnterpriseLabSupervisorOwnership ownership) {
        return service(ownership, point -> { });
    }

    private EnterpriseLabSupervisorService service(
            EnterpriseLabSupervisorOwnership ownership,
            EnterpriseLabSupervisorService.FailureInjector failureInjector) {
        return EnterpriseLabSupervisorService.startForTesting(
                ownership,
                targets,
                clock,
                request -> EnterpriseLabSupervisorService.OwnershipVerification.allow(),
                failureInjector);
    }

    private Request advance(
            EnterpriseLabSupervisorState state,
            String applicationId,
            String ownershipFingerprint,
            long generation) {
        return codec.issue(new RequestDraft(
                "advance-" + generation,
                CommandType.ADVANCE_APPLICATION_OWNERSHIP,
                applicationId,
                ownershipFingerprint,
                generation,
                state.supervisorInstanceId(),
                state.supervisorGeneration(),
                "handoff-" + generation,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabSupervisorProtocol.NONE,
                NOW,
                Map.of("evidence", "repository-owned")));
    }

    private Request apply(
            EnterpriseLabSupervisorState state,
            String applicationId,
            String ownershipFingerprint,
            long generation,
            String requestId,
            String transactionId) {
        EnterpriseLabLoopbackAllocationSnapshot allocation =
                new EnterpriseLabLoopbackAllocationSnapshot(
                        EnterpriseLabLoopbackAllocationSnapshot.SCHEMA_VERSION,
                        EnterpriseLabSupervisorConfiguration.SCENARIO_ID,
                        99L,
                        "candidate-decision",
                        Kind.CANDIDATE,
                        Map.of("blue", 0.10d, "green", 0.60d, "orange", 0.30d));
        return codec.issue(new RequestDraft(
                requestId,
                CommandType.APPLY_ALLOCATION,
                applicationId,
                ownershipFingerprint,
                generation,
                state.supervisorInstanceId(),
                state.supervisorGeneration(),
                transactionId,
                Optional.of("experiment-1"),
                AllocationPurpose.EXPERIMENT_CANDIDATE,
                Optional.of(allocation),
                EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                        allocation.scenarioId(), allocation.allocations()),
                state.installedAllocation().allocationFingerprint(),
                NOW,
                Map.of("scope", "literal-loopback-only")));
    }

    private static final class SimulatedCrash extends RuntimeException {
    }
}
