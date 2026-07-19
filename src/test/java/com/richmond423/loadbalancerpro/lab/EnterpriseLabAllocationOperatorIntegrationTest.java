package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.ArmRequest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.OperatorStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabAllocationOperatorIntegrationTest {
    private static final String SCENARIO = "tail-latency-pressure";

    @TempDir
    Path temporaryDirectory;

    @Test
    void liveOperatorStartUsesDurableCoordinatorAndCancellationRestoresThroughSupervisor() {
        EnterpriseLabExperimentTargetCatalog catalog =
                EnterpriseLabAllocationProofStateHolder.fixedTargets();
        EnterpriseLabMutationTestAuthority authority =
                new EnterpriseLabMutationTestAuthority(temporaryDirectory);
        EnterpriseLabExperimentRecoveryGate recoveryGate =
                EnterpriseLabExperimentRecoveryGate.inMemoryOnly();
        EnterpriseLabAllocationReconciliationGate allocationGate =
                EnterpriseLabAllocationReconciliationGate.pending();
        var decision = new EnterpriseLabAdaptiveDecisionService().decide(
                SCENARIO, "active-experiment", true, false, false);
        EnterpriseLabLoopbackAllocationRouter startupRouter =
                new EnterpriseLabLoopbackAllocationRouter(
                        catalog.findTargets(SCENARIO).orElseThrow(),
                        new EnterpriseLabLoopbackObservationIngress(
                                Set.of("blue", "green", "orange")),
                        decision.decision().guardrailDecision().baselineAllocations(),
                        Optional.of(authority));
        EnterpriseLabAllocationSupervisor supervisor =
                EnterpriseLabAllocationSupervisor.createOwned(
                        temporaryDirectory,
                        catalog,
                        startupRouter,
                        authority,
                        allocationGate);
        EnterpriseLabExperimentDurableEvidenceRepository durable =
                new EnterpriseLabExperimentDurableEvidenceRepository(
                        EnterpriseLabExperimentJournalDirectory.createOwned(
                                temporaryDirectory, authority),
                        recoveryGate,
                        Clock.systemUTC());

        try (EnterpriseLabExperimentOperatorService service =
                new EnterpriseLabExperimentOperatorService(
                        catalog,
                        new EnterpriseLabScenarioCatalogService(),
                        new EnterpriseLabAdaptiveDecisionService(),
                        Clock.systemUTC(),
                        System::nanoTime,
                        8,
                        recoveryGate,
                        Optional.of(durable),
                        Optional.empty(),
                        Optional.of(allocationGate),
                        Optional.of(supervisor))) {
            var arm = service.arm(new ArmRequest(
                    "operator-arm",
                    "operator-allocation-experiment",
                    SCENARIO,
                    8,
                    Duration.ofSeconds(30),
                    2,
                    1,
                    Duration.ofSeconds(60)), true);
            assertEquals(OperatorStatus.APPLIED, arm.status(), arm.toString());

            var started = service.start(
                    "operator-allocation-experiment", "operator-start", true);
            assertEquals(OperatorStatus.APPLIED, started.status(), started.toString());
            assertTrue(started.trafficActionPerformed());
            var active = service.allocationSupervisionStatus().orElseThrow();
            assertEquals(Kind.CANDIDATE, active.installed().orElseThrow().kind());
            assertTrue(active.fingerprints().committedMatchesInstalled());

            var cancelled = service.cancel(
                    "operator-allocation-experiment",
                    "operator-cancel",
                    "bounded operator cancellation");
            assertEquals(OperatorStatus.RECORDED, cancelled.status(), cancelled.toString());
            var safe = service.allocationSupervisionStatus().orElseThrow();
            assertTrue(safe.ready());
            assertFalse(safe.installed().orElseThrow().kind() == Kind.CANDIDATE);
            assertTrue(safe.fingerprints().baselineMatchesInstalled());

            int history = safe.history().size();
            var verified = service.verifyAllocationSupervision().orElseThrow();
            assertEquals(history, verified.history().size());
            assertEquals(safe.fingerprints(), verified.fingerprints());
        }
    }
}
