package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.ChangeStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabAllocationSupervisorTest {
    private static final String SCENARIO = "tail-latency-pressure";

    @TempDir
    Path temporaryDirectory;

    private EnterpriseLabExperimentTargetCatalog catalog;
    private EnterpriseLabMutationTestAuthority authority;
    private EnterpriseLabLoopbackAllocationRouter router;
    private EnterpriseLabAllocationReconciliationGate gate;

    @BeforeEach
    void setUp() {
        catalog = EnterpriseLabAllocationProofStateHolder.fixedTargets();
        authority = new EnterpriseLabMutationTestAuthority(temporaryDirectory);
        var decision = decision();
        router = new EnterpriseLabLoopbackAllocationRouter(
                catalog.findTargets(SCENARIO).orElseThrow(),
                new EnterpriseLabLoopbackObservationIngress(
                        Set.of("blue", "green", "orange")),
                decision.decision().guardrailDecision().baselineAllocations(),
                Optional.of(authority));
        gate = EnterpriseLabAllocationReconciliationGate.pending();
    }

    @Test
    void statusApplyRestoreAndRepeatedVerificationRemainBoundedAndExact() throws Exception {
        try (EnterpriseLabAllocationSupervisor supervisor =
                EnterpriseLabAllocationSupervisor.createOwned(
                        temporaryDirectory, catalog, router, authority, gate)) {
            var initial = supervisor.status();
            assertTrue(initial.ready());
            assertEquals(TransactionPhase.COMMITTED, initial.currentPhase().orElseThrow());
            assertEquals(Kind.BASELINE, initial.installed().orElseThrow().kind());
            assertTrue(initial.fingerprints().baselineMatchesInstalled());

            var applied = supervisor.applyCandidate(
                    "supervisor-candidate",
                    "supervisor-experiment",
                    decision(),
                    true);
            assertEquals(ChangeStatus.APPLIED, applied.status());
            assertTrue(applied.trafficActionPerformed());
            var candidate = supervisor.status();
            assertEquals(Kind.CANDIDATE, candidate.installed().orElseThrow().kind());
            assertTrue(candidate.fingerprints().committedMatchesInstalled());
            assertEquals(TransactionPhase.COMMITTED, candidate.currentPhase().orElseThrow());
            assertEquals(
                    EnterpriseLabAllocationReconciler.DriftClassification
                            .COMMITTED_CANDIDATE_REQUIRES_BASELINE,
                    candidate.driftClassification());

            var restored = supervisor.restoreSafeBaseline("test verified reset");
            assertEquals(ChangeStatus.RESTORED, restored.status());
            var safe = supervisor.status();
            assertTrue(safe.ready());
            assertTrue(safe.fingerprints().baselineMatchesInstalled());
            assertFalse(safe.installed().orElseThrow().kind() == Kind.CANDIDATE);
            int history = safe.history().size();

            supervisor.verify();
            supervisor.verify();
            var repeated = supervisor.status();
            assertEquals(history, repeated.history().size());
            assertEquals(safe.fingerprints(), repeated.fingerprints());
            assertEquals(safe.routerGeneration(), repeated.routerGeneration());
            assertTrue(repeated.ready());

            String json = new ObjectMapper().findAndRegisterModules()
                    .writeValueAsString(repeated);
            assertFalse(json.contains("127.0.0.1"));
            assertFalse(json.contains("allocation-proof"));
            assertFalse(json.contains(temporaryDirectory.toString()));
            assertFalse(json.contains("baselineAllocation"));
            assertFalse(json.contains("requestedAllocation"));
            assertTrue(json.length() < 64_000);
            assertNotNull(repeated.evidenceBoundary());
        }
    }

    @Test
    void statusHistoryIsHardBoundedToMostRecentTransactionEvidence() {
        try (EnterpriseLabAllocationSupervisor supervisor =
                EnterpriseLabAllocationSupervisor.createOwned(
                        temporaryDirectory, catalog, router, authority, gate)) {
            for (int index = 0; index < 3; index++) {
                supervisor.applyCandidate(
                        "candidate-" + index,
                        "experiment-" + index,
                        decision(),
                        true);
                supervisor.restoreSafeBaseline("bounded reset " + index);
            }

            var status = supervisor.status();
            assertTrue(status.history().size()
                    <= EnterpriseLabAllocationSupervisor.MAX_HISTORY_SUMMARIES);
            assertTrue(status.unresolvedCount() <= EnterpriseLabAllocationStateStore.HARD_MAX_RECORDS);
            assertEquals(0, status.quarantinedCount());
        }
    }

    private static EnterpriseLabAdaptiveDecision decision() {
        return new EnterpriseLabAdaptiveDecisionService().decide(
                SCENARIO, "active-experiment", true, false, false);
    }
}
