package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.Policy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReconciliationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.SocketException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabSupervisorAllocationBridgeTest {
    private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");

    @TempDir
    Path root;

    @Test
    void externalRequiredPathFencesOwnerAndSupervisorAndCommitsOnlyAfterReadBack()
            throws Exception {
        MutableClock clock = new MutableClock(NOW);
        EnterpriseLabExperimentTargetCatalog targets =
                EnterpriseLabSupervisorConfiguration.approvedTargets();
        EnterpriseLabExperimentRecoveryGate recoveryGate =
                EnterpriseLabExperimentRecoveryGate.pending();
        Policy policy = new Policy(
                Duration.ofMinutes(2),
                Duration.ofSeconds(30),
                2,
                2,
                Duration.ZERO);
        EnterpriseLabEvidenceOwnershipLease applicationOwnership =
                EnterpriseLabEvidenceOwnershipManager.acquire(root, policy, clock)
                        .ownership().orElseThrow();
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(
                        root, applicationOwnership.ownershipGate());
        new EnterpriseLabExperimentStartupReconciler(
                directory,
                new EnterpriseLabProcessLocalAllocationRecovery(targets),
                recoveryGate,
                clock).initialize();
        assertEquals(
                ReconciliationStatus.SUCCEEDED,
                applicationOwnership.completeApplicationReconciliation(recoveryGate)
                        .reconciliationStatus());

        try (EnterpriseLabSupervisorOwnership supervisorOwnership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service =
                    EnterpriseLabSupervisorService.start(
                            supervisorOwnership, targets, clock);
            try (RunningServer running = start(supervisorOwnership, service, targets, clock);
                 EnterpriseLabSupervisorAllocationBridge bridge =
                         EnterpriseLabSupervisorAllocationBridge.connect(
                                 root,
                                 targets,
                                 applicationOwnership.ownershipGate(),
                                 clock)) {
                String scenarioId = targets.boundScenarioIds().get(0);
                var loopbackTargets = targets.findTargets(scenarioId).orElseThrow();
                var decision = new EnterpriseLabAdaptiveDecisionService().decide(
                        scenarioId,
                        AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT.wireValue(),
                        true,
                        false,
                        false);
                EnterpriseLabLoopbackAllocationRouter router =
                        EnterpriseLabLoopbackAllocationRouter.supervised(
                                loopbackTargets,
                                new EnterpriseLabLoopbackObservationIngress(
                                        loopbackTargets.stream()
                                                .map(EnterpriseLabLoopbackTarget::backendId)
                                                .toList()),
                                decision.decision().guardrailDecision()
                                        .baselineAllocations(),
                                applicationOwnership.ownershipGate(),
                                bridge);
                EnterpriseLabAllocationReconciliationGate allocationGate =
                        EnterpriseLabAllocationReconciliationGate.pending();
                EnterpriseLabAllocationSupervisor applicationSupervisor =
                        EnterpriseLabAllocationSupervisor.create(
                                root,
                                targets,
                                router,
                                applicationOwnership.ownershipGate(),
                                allocationGate);
                EnterpriseLabExperimentDurableEvidenceRepository durableEvidence =
                        new EnterpriseLabExperimentDurableEvidenceRepository(
                                directory, recoveryGate, clock);
                EnterpriseLabExperimentOperatorService operatorService =
                        new EnterpriseLabExperimentOperatorService(
                                targets,
                                recoveryGate,
                                durableEvidence,
                                applicationOwnership,
                                allocationGate,
                                applicationSupervisor,
                                bridge);
                try (EnterpriseLabSupervisorAllocationBridge staleBridge =
                             EnterpriseLabSupervisorAllocationBridge.connect(
                                     root,
                                     targets,
                                     applicationOwnership.ownershipGate(),
                                     clock)) {
                    try (operatorService) {
                        assertEquals(
                                EnterpriseLabAllocationRuntimeMode
                                        .EXTERNAL_SUPERVISOR_REQUIRED,
                                operatorService.allocationRuntimeMode());
                        assertTrue(allocationGate.admissionAllowed());
                        assertEquals(
                                applicationOwnership.record().generation(),
                                bridge.readAuthoritative().ownerGeneration());

                        clock.advance(Duration.ofSeconds(1));
                        assertEquals(
                                OperationStatus.SUCCEEDED,
                                applicationOwnership.ownershipGate().renew().status());

                        var armed = operatorService.arm(
                                new EnterpriseLabExperimentOperatorService.ArmRequest(
                                        "external-arm-1",
                                        "external-experiment-1",
                                        scenarioId,
                                        10,
                                        Duration.ofSeconds(30),
                                        2,
                                        1,
                                        Duration.ofSeconds(60)),
                                true);
                        assertEquals(
                                EnterpriseLabExperimentOperatorService.OperatorStatus.APPLIED,
                                armed.status());
                        var applied = operatorService.start(
                                "external-experiment-1",
                                "external-start-1",
                                true);
                        assertEquals(
                                EnterpriseLabExperimentOperatorService.OperatorStatus.APPLIED,
                                applied.status());
                        var installed = bridge.readAuthoritative();
                        assertEquals(
                                EnterpriseLabLoopbackAllocationSnapshot.Kind.CANDIDATE,
                                installed.routingSnapshot().kind());
                        assertEquals(
                                applicationOwnership.record().generation(),
                                installed.ownerGeneration());
                        assertTrue(applicationSupervisor.status()
                                .fingerprints().committedMatchesInstalled());

                        var restored = operatorService.cancel(
                                "external-experiment-1",
                                "external-cancel-1",
                                "focused external operator integration completed");
                        assertEquals(
                                EnterpriseLabExperimentOperatorService.OperatorStatus.RECORDED,
                                restored.status());
                        assertEquals(
                                EnterpriseLabLoopbackAllocationSnapshot.Kind.RESTORED_BASELINE,
                                bridge.readAuthoritative().routingSnapshot().kind());
                    }

                    assertEquals(
                            EnterpriseLabLoopbackAllocationSnapshot.Kind.RESTORED_BASELINE,
                            staleBridge.read().routingSnapshot().kind());
                    var stale = assertThrows(
                            EnterpriseLabEvidenceOwnershipException.class,
                            staleBridge::readAuthoritative);
                    assertEquals(
                            EnterpriseLabEvidenceOwnership.FailureClassification.LOCK_LOST,
                            stale.classification());
                }
            }
        } finally {
            if (applicationOwnership.operatingSystemLockValid()) {
                applicationOwnership.close();
            }
        }
    }

    @Test
    void supervisorRestartClosesAdmissionUntilExplicitReconnectRestoresAndTerminatesCandidate()
            throws Exception {
        MutableClock clock = new MutableClock(NOW);
        EnterpriseLabExperimentTargetCatalog targets =
                EnterpriseLabSupervisorConfiguration.approvedTargets();
        EnterpriseLabExperimentRecoveryGate recoveryGate =
                EnterpriseLabExperimentRecoveryGate.pending();
        Policy policy = new Policy(
                Duration.ofMinutes(2),
                Duration.ofSeconds(30),
                2,
                2,
                Duration.ZERO);
        EnterpriseLabEvidenceOwnershipLease applicationOwnership =
                EnterpriseLabEvidenceOwnershipManager.acquire(root, policy, clock)
                        .ownership().orElseThrow();
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(
                        root, applicationOwnership.ownershipGate());
        new EnterpriseLabExperimentStartupReconciler(
                directory,
                new EnterpriseLabProcessLocalAllocationRecovery(targets),
                recoveryGate,
                clock).initialize();
        applicationOwnership.completeApplicationReconciliation(recoveryGate);

        try (EnterpriseLabSupervisorOwnership supervisorOwnership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService firstService =
                    EnterpriseLabSupervisorService.start(
                            supervisorOwnership, targets, clock);
            RunningServer first = start(
                    supervisorOwnership, firstService, targets, clock);
            EnterpriseLabSupervisorAllocationBridge bridge =
                    EnterpriseLabSupervisorAllocationBridge.connect(
                            root,
                            targets,
                            applicationOwnership.ownershipGate(),
                            clock);
            String scenarioId = targets.boundScenarioIds().get(0);
            var loopbackTargets = targets.findTargets(scenarioId).orElseThrow();
            var decision = new EnterpriseLabAdaptiveDecisionService().decide(
                    scenarioId,
                    AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT.wireValue(),
                    true,
                    false,
                    false);
            EnterpriseLabLoopbackAllocationRouter router =
                    EnterpriseLabLoopbackAllocationRouter.supervised(
                            loopbackTargets,
                            new EnterpriseLabLoopbackObservationIngress(
                                    loopbackTargets.stream()
                                            .map(EnterpriseLabLoopbackTarget::backendId)
                                            .toList()),
                            decision.decision().guardrailDecision()
                                    .baselineAllocations(),
                            applicationOwnership.ownershipGate(),
                            bridge);
            EnterpriseLabAllocationReconciliationGate allocationGate =
                    EnterpriseLabAllocationReconciliationGate.pending();
            EnterpriseLabAllocationSupervisor applicationSupervisor =
                    EnterpriseLabAllocationSupervisor.create(
                            root,
                            targets,
                            router,
                            applicationOwnership.ownershipGate(),
                            allocationGate);
            EnterpriseLabExperimentDurableEvidenceRepository durableEvidence =
                    new EnterpriseLabExperimentDurableEvidenceRepository(
                            directory, recoveryGate, clock);
            EnterpriseLabExperimentOperatorService operatorService =
                    new EnterpriseLabExperimentOperatorService(
                            targets,
                            recoveryGate,
                            durableEvidence,
                            applicationOwnership,
                            allocationGate,
                            applicationSupervisor,
                            bridge);
            EnterpriseLabSupervisorConnectionMetadata firstEpoch =
                    bridge.connectionMetadata();
            try (operatorService) {
                assertEquals(
                        EnterpriseLabExperimentOperatorService.OperatorStatus.APPLIED,
                        operatorService.arm(
                                new EnterpriseLabExperimentOperatorService.ArmRequest(
                                        "restart-arm-1",
                                        "restart-experiment-1",
                                        scenarioId,
                                        10,
                                        Duration.ofSeconds(30),
                                        2,
                                        1,
                                        Duration.ofSeconds(60)),
                                true).status());
                assertEquals(
                        EnterpriseLabExperimentOperatorService.OperatorStatus.APPLIED,
                        operatorService.start(
                                "restart-experiment-1",
                                "restart-start-1",
                                true).status());
                assertEquals(
                        EnterpriseLabLoopbackAllocationSnapshot.Kind.CANDIDATE,
                        bridge.readAuthoritative().routingSnapshot().kind());

                first.close();
                clock.advance(Duration.ofSeconds(1));
                EnterpriseLabSupervisorService restartedService =
                        EnterpriseLabSupervisorService.start(
                                supervisorOwnership, targets, clock);
                try (RunningServer restarted = start(
                        supervisorOwnership, restartedService, targets, clock)) {
                    var denied = operatorService.evaluate(
                            "restart-experiment-1",
                            "restart-evaluate-1",
                            true);
                    assertEquals(
                            EnterpriseLabExperimentOperatorService.OperatorStatus.DENIED,
                            denied.status());
                    assertEquals("SUPERVISOR_SESSION_UNAVAILABLE", denied.reasonCode());
                    assertFalse(allocationGate.admissionAllowed());
                    assertEquals(
                            "SUPERVISOR_SESSION_UNAVAILABLE",
                            operatorService.start(
                                    "restart-experiment-1",
                                    "restart-start-after-loss-1",
                                    true).reasonCode());
                    assertEquals(
                            "SUPERVISOR_SESSION_UNAVAILABLE",
                            operatorService.executeRequests(
                                    "restart-experiment-1",
                                    new EnterpriseLabExperimentOperatorService
                                            .RequestBatchRequest(
                                                    "restart-batch-after-loss-1",
                                                    1,
                                                    Duration.ofSeconds(1)),
                                    true).reasonCode());
                    assertEquals(
                            "SUPERVISOR_SESSION_UNAVAILABLE",
                            operatorService.cancel(
                                    "restart-experiment-1",
                                    "restart-cancel-after-loss-1",
                                    "reconnect must precede cancellation")
                                    .reasonCode());
                    assertEquals(
                            "SUPERVISOR_SESSION_UNAVAILABLE",
                            operatorService.arm(
                                    new EnterpriseLabExperimentOperatorService.ArmRequest(
                                            "restart-second-arm-after-loss-1",
                                            "restart-second-experiment-1",
                                            scenarioId,
                                            10,
                                            Duration.ofSeconds(30),
                                            2,
                                            1,
                                            Duration.ofSeconds(60)),
                                    true).reasonCode());

                    Object evaluator = replaceSessionEvaluator(
                            operatorService,
                            "restart-experiment-1",
                            null);
                    try {
                        var incomplete = operatorService.verifyAllocationSupervision()
                                .orElseThrow();
                        assertFalse(incomplete.ready(), incomplete.toString());
                        assertFalse(allocationGate.admissionAllowed());
                        assertFalse(operatorService.findRecord("restart-experiment-1")
                                .orElseThrow().lifecycle().terminal());
                    } finally {
                        replaceSessionEvaluator(
                                operatorService,
                                "restart-experiment-1",
                                evaluator);
                    }

                    var verified = operatorService.verifyAllocationSupervision()
                            .orElseThrow();
                    EnterpriseLabSupervisorConnectionMetadata secondEpoch =
                            bridge.connectionMetadata();
                    assertTrue(verified.ready(), verified.toString());
                    assertEquals(
                            firstEpoch.supervisorGeneration() + 1L,
                            secondEpoch.supervisorGeneration());
                    assertNotEquals(
                            firstEpoch.supervisorInstanceId(),
                            secondEpoch.supervisorInstanceId());
                    assertEquals(
                            EnterpriseLabLoopbackAllocationSnapshot.Kind.RESTORED_BASELINE,
                            bridge.readAuthoritative().routingSnapshot().kind());
                    assertEquals(
                            EnterpriseLabExperimentState.ROLLED_BACK,
                            operatorService.findRecord("restart-experiment-1")
                                    .orElseThrow().lifecycle().state());
                    assertTrue(allocationGate.admissionAllowed());
                }
            } finally {
                if (!first.future().isDone()) {
                    first.close();
                }
            }
        } finally {
            if (applicationOwnership.operatingSystemLockValid()) {
                applicationOwnership.close();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void applicationAndDualRestartUseOrderedReplayWithoutResumingCandidate(
            boolean restartSupervisor)
            throws Exception {
        MutableClock clock = new MutableClock(NOW);
        EnterpriseLabExperimentTargetCatalog targets =
                EnterpriseLabSupervisorConfiguration.approvedTargets();
        Policy policy = new Policy(
                Duration.ofMinutes(2),
                Duration.ofSeconds(30),
                2,
                2,
                Duration.ZERO);
        EnterpriseLabExperimentRecoveryGate firstRecovery =
                EnterpriseLabExperimentRecoveryGate.pending();
        EnterpriseLabEvidenceOwnershipLease firstOwnership =
                EnterpriseLabEvidenceOwnershipManager.acquire(root, policy, clock)
                        .ownership().orElseThrow();
        long firstApplicationGeneration = firstOwnership.record().generation();
        EnterpriseLabExperimentJournalDirectory firstDirectory =
                EnterpriseLabExperimentJournalDirectory.create(
                        root, firstOwnership.ownershipGate());
        new EnterpriseLabExperimentStartupReconciler(
                firstDirectory,
                new EnterpriseLabProcessLocalAllocationRecovery(targets),
                firstRecovery,
                clock).initialize();
        firstOwnership.completeApplicationReconciliation(firstRecovery);

        try (EnterpriseLabSupervisorOwnership supervisorOwnership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService supervisorService =
                    EnterpriseLabSupervisorService.start(
                            supervisorOwnership, targets, clock);
            RunningServer running = start(
                    supervisorOwnership, supervisorService, targets, clock);
            try {
                EnterpriseLabSupervisorAllocationBridge firstBridge =
                        EnterpriseLabSupervisorAllocationBridge.connect(
                                root,
                                targets,
                                firstOwnership.ownershipGate(),
                                clock);
                String scenarioId = targets.boundScenarioIds().get(0);
                var loopbackTargets = targets.findTargets(scenarioId).orElseThrow();
                var decision = new EnterpriseLabAdaptiveDecisionService().decide(
                        scenarioId,
                        AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT.wireValue(),
                        true,
                        false,
                        false);
                EnterpriseLabLoopbackAllocationRouter firstRouter =
                        EnterpriseLabLoopbackAllocationRouter.supervised(
                                loopbackTargets,
                                new EnterpriseLabLoopbackObservationIngress(
                                        loopbackTargets.stream()
                                                .map(EnterpriseLabLoopbackTarget::backendId)
                                                .toList()),
                                decision.decision().guardrailDecision()
                                        .baselineAllocations(),
                                firstOwnership.ownershipGate(),
                                firstBridge);
                EnterpriseLabAllocationReconciliationGate firstAllocationGate =
                        EnterpriseLabAllocationReconciliationGate.pending();
                EnterpriseLabAllocationSupervisor firstApplicationSupervisor =
                        EnterpriseLabAllocationSupervisor.create(
                                root,
                                targets,
                                firstRouter,
                                firstOwnership.ownershipGate(),
                                firstAllocationGate);
                EnterpriseLabExperimentDurableEvidenceRepository firstEvidence =
                        new EnterpriseLabExperimentDurableEvidenceRepository(
                                firstDirectory, firstRecovery, clock);
                EnterpriseLabExperimentOperatorService abandonedApplication =
                        new EnterpriseLabExperimentOperatorService(
                                targets,
                                firstRecovery,
                                firstEvidence,
                                firstOwnership,
                                firstAllocationGate,
                                firstApplicationSupervisor,
                                firstBridge);

                assertEquals(
                        EnterpriseLabExperimentOperatorService.OperatorStatus.APPLIED,
                        abandonedApplication.arm(
                                new EnterpriseLabExperimentOperatorService.ArmRequest(
                                        "app-restart-arm-1",
                                        "app-restart-experiment-1",
                                        scenarioId,
                                        10,
                                        Duration.ofSeconds(30),
                                        2,
                                        1,
                                        Duration.ofSeconds(60)),
                                true).status());
                assertEquals(
                        EnterpriseLabExperimentOperatorService.OperatorStatus.APPLIED,
                        abandonedApplication.start(
                                "app-restart-experiment-1",
                                "app-restart-start-1",
                                true).status());
                assertEquals(
                        EnterpriseLabLoopbackAllocationSnapshot.Kind.CANDIDATE,
                        firstBridge.readAuthoritative().routingSnapshot().kind());
                EnterpriseLabSupervisorConnectionMetadata firstSupervisorEpoch =
                        firstBridge.connectionMetadata();

                stopRenewerWithoutLifecycleShutdown(abandonedApplication);
                firstApplicationSupervisor.close();
                firstEvidence.close();
                firstOwnership.close();

                RunningServer restartedRunning = null;
                if (restartSupervisor) {
                    running.close();
                    clock.advance(Duration.ofSeconds(1));
                    EnterpriseLabSupervisorService restartedService =
                            EnterpriseLabSupervisorService.start(
                                    supervisorOwnership, targets, clock);
                    restartedRunning = start(
                            supervisorOwnership, restartedService, targets, clock);
                }
                clock.advance(Duration.ofSeconds(1));
                EnterpriseLabExperimentRecoveryGate restartedRecovery =
                        EnterpriseLabExperimentRecoveryGate.pending();
                var restartedAcquisition =
                        EnterpriseLabEvidenceOwnershipManager.acquire(
                                root, policy, clock);
                EnterpriseLabEvidenceOwnershipLease restartedOwnership;
                if (restartedAcquisition.ownership().isPresent()) {
                    restartedOwnership = restartedAcquisition.ownership().orElseThrow();
                } else {
                    assertEquals(
                            EnterpriseLabEvidenceOwnership.FailureClassification
                                    .TAKEOVER_NOT_PERMITTED,
                            restartedAcquisition.result().failure());
                    var takeover = EnterpriseLabEvidenceOwnershipManager.takeover(
                            root,
                            policy,
                            clock,
                            new EnterpriseLabExperimentStartupReconciler(
                                    EnterpriseLabExperimentJournalDirectory.create(root),
                                    new EnterpriseLabProcessLocalAllocationRecovery(targets),
                                    restartedRecovery,
                                    clock));
                    assertEquals(OperationStatus.SUCCEEDED, takeover.result().status());
                    restartedOwnership = takeover.ownership().orElseThrow();
                }
                try {
                    assertTrue(
                            restartedOwnership.record().generation()
                                    > firstApplicationGeneration);
                    EnterpriseLabExperimentJournalDirectory restartedDirectory =
                            EnterpriseLabExperimentJournalDirectory.create(
                                    root, restartedOwnership.ownershipGate());
                    var recoveryReport = new EnterpriseLabExperimentStartupReconciler(
                            restartedDirectory,
                            new EnterpriseLabProcessLocalAllocationRecovery(targets),
                            restartedRecovery,
                            clock).initialize();
                    assertTrue(recoveryReport.admissionAllowed(), recoveryReport.toString());
                    var replayed = restartedDirectory.replay(
                                    "app-restart-experiment-1")
                            .reconstructedState().orElseThrow();
                    assertEquals(
                            EnterpriseLabExperimentState.ROLLED_BACK,
                            replayed.lifecycle().state());
                    String durableBaselineFingerprint;
                    java.util.Map<String, Double> durableBaselineAllocation;
                    try (EnterpriseLabAllocationStateStore inspectionStore =
                                 EnterpriseLabAllocationStateStore.create(
                                         root,
                                         targets,
                                         restartedOwnership.ownershipGate())) {
                        EnterpriseLabAllocationState baseline = inspectionStore
                                .replay().baseline().orElseThrow();
                        durableBaselineFingerprint =
                                EnterpriseLabAllocationStateCodec
                                        .canonicalAllocationFingerprint(
                                                baseline.scenarioId(),
                                                baseline.baselineAllocation());
                        durableBaselineAllocation = baseline.baselineAllocation();
                    }
                    var replayEvidence = EnterpriseLabAllocationReconciler
                            .ExperimentAllocationEvidence.from(replayed);
                    assertEquals(
                            durableBaselineAllocation,
                            replayed.baselineAllocation().allocations(),
                            "experiment and allocation baseline maps must match");
                    assertEquals(
                            durableBaselineFingerprint,
                            replayEvidence.baselineFingerprint(),
                            "experiment baseline and allocation baseline must match");
                    assertEquals(
                            durableBaselineFingerprint,
                            replayEvidence.lastAppliedFingerprint(),
                            "terminal experiment allocation and allocation baseline must match");
                    restartedOwnership.completeApplicationReconciliation(
                            restartedRecovery);

                    try (EnterpriseLabSupervisorAllocationBridge restartedBridge =
                                 EnterpriseLabSupervisorAllocationBridge.connect(
                                         root,
                                         targets,
                                         restartedOwnership.ownershipGate(),
                                         clock)) {
                        if (restartSupervisor) {
                            assertTrue(
                                    restartedBridge.connectionMetadata()
                                            .supervisorGeneration()
                                            > firstSupervisorEpoch.supervisorGeneration());
                            assertNotEquals(
                                    firstSupervisorEpoch.supervisorInstanceId(),
                                    restartedBridge.connectionMetadata()
                                            .supervisorInstanceId());
                        } else {
                            assertEquals(
                                    firstSupervisorEpoch,
                                    restartedBridge.connectionMetadata());
                        }
                        assertEquals(
                                EnterpriseLabLoopbackAllocationSnapshot.Kind.CANDIDATE,
                                restartedBridge.readAuthoritative()
                                        .routingSnapshot().kind());
                        EnterpriseLabLoopbackAllocationRouter restartedRouter =
                                EnterpriseLabLoopbackAllocationRouter.supervised(
                                        loopbackTargets,
                                        new EnterpriseLabLoopbackObservationIngress(
                                                loopbackTargets.stream()
                                                        .map(EnterpriseLabLoopbackTarget::backendId)
                                                        .toList()),
                                        decision.decision().guardrailDecision()
                                                .baselineAllocations(),
                                        restartedOwnership.ownershipGate(),
                                        restartedBridge);
                        EnterpriseLabAllocationReconciliationGate restartedAllocationGate =
                                EnterpriseLabAllocationReconciliationGate.pending();
                        try (EnterpriseLabAllocationSupervisor restartedApplicationSupervisor =
                                     EnterpriseLabAllocationSupervisor.create(
                                             root,
                                             targets,
                                             restartedRouter,
                                             restartedOwnership.ownershipGate(),
                                             restartedAllocationGate,
                                             java.util.List.of(replayed))) {
                            assertTrue(restartedAllocationGate.admissionAllowed());
                            assertEquals(
                                    EnterpriseLabLoopbackAllocationSnapshot.Kind.RESTORED_BASELINE,
                                    restartedBridge.readAuthoritative()
                                            .routingSnapshot().kind());
                            assertEquals(
                                    restartedOwnership.record().generation(),
                                    restartedBridge.readAuthoritative()
                                            .ownerGeneration());
                        }
                    }

                    var stale = assertThrows(
                            EnterpriseLabEvidenceOwnershipException.class,
                            firstBridge::readAuthoritative);
                    assertEquals(
                            EnterpriseLabEvidenceOwnership.FailureClassification.LOCK_LOST,
                            stale.classification());
                } finally {
                    firstBridge.close();
                    restartedOwnership.close();
                    if (restartedRunning != null) {
                        restartedRunning.close();
                    }
                }
            } finally {
                if (!running.future().isDone()) {
                    running.close();
                }
            }
        } finally {
            if (firstOwnership.operatingSystemLockValid()) {
                firstOwnership.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void stopRenewerWithoutLifecycleShutdown(
            EnterpriseLabExperimentOperatorService service) throws Exception {
        var field = EnterpriseLabExperimentOperatorService.class
                .getDeclaredField("ownershipRenewer");
        field.setAccessible(true);
        Optional<EnterpriseLabEvidenceOwnershipRenewer> renewer =
                (Optional<EnterpriseLabEvidenceOwnershipRenewer>) field.get(service);
        renewer.ifPresent(EnterpriseLabEvidenceOwnershipRenewer::close);
    }

    @SuppressWarnings("unchecked")
    private static Object replaceSessionEvaluator(
            EnterpriseLabExperimentOperatorService service,
            String experimentId,
            Object replacement) throws Exception {
        var sessionsField = EnterpriseLabExperimentOperatorService.class
                .getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        var sessions = (java.util.Map<String, Object>) sessionsField.get(service);
        Object session = sessions.get(experimentId);
        if (session == null) {
            throw new IllegalStateException("test session is absent");
        }
        var evaluatorField = session.getClass().getDeclaredField("evaluator");
        evaluatorField.setAccessible(true);
        Object previous = evaluatorField.get(session);
        evaluatorField.set(session, replacement);
        return previous;
    }

    private RunningServer start(
            EnterpriseLabSupervisorOwnership ownership,
            EnterpriseLabSupervisorService service,
            EnterpriseLabExperimentTargetCatalog targets,
            Clock clock) throws Exception {
        EnterpriseLabSupervisorServer server = new EnterpriseLabSupervisorServer(
                ownership, service, targets, clock, 0);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<EnterpriseLabSupervisorServer.RunResult> future =
                executor.submit(server::run);
        long deadline = System.nanoTime()
                + EnterpriseLabSupervisorConfiguration.STARTUP_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            Optional<Integer> port =
                    EnterpriseLabSupervisorServer.readReadyPortForTesting(root);
            if (port.isPresent()) {
                return new RunningServer(server, executor, future);
            }
            Thread.sleep(10L);
        }
        server.close();
        executor.shutdownNow();
        throw new IllegalStateException("supervisor readiness timed out");
    }

    private record RunningServer(
            EnterpriseLabSupervisorServer server,
            ExecutorService executor,
            Future<EnterpriseLabSupervisorServer.RunResult> future)
            implements AutoCloseable {
        @Override
        public void close() throws Exception {
            server.close();
            executor.shutdownNow();
            try {
                future.get();
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                if (!(cause instanceof EnterpriseLabSupervisorServer.ServerException)
                        || !(cause.getCause() instanceof SocketException socket)
                        || !"Socket closed".equals(socket.getMessage())) {
                    throw exception;
                }
            }
        }
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (!ZoneOffset.UTC.equals(zone)) {
                throw new IllegalArgumentException("test clock is fixed to UTC");
            }
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
