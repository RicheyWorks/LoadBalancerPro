package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.Policy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReconciliationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
