package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.AllocationInspection;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.AllocationRecoveryPort;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.BaselineRestorationReceipt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Restart allocation adapter for the existing process-local loopback router.
 * Constructing and inspecting a router sends no requests. A mutation is limited
 * to the router's existing atomic recorded-baseline restoration operation.
 */
public final class EnterpriseLabProcessLocalAllocationRecovery implements AllocationRecoveryPort {
    private final EnterpriseLabExperimentTargetCatalog targetCatalog;
    private final Map<String, EnterpriseLabLoopbackAllocationRouter> routers = new LinkedHashMap<>();

    public EnterpriseLabProcessLocalAllocationRecovery(
            EnterpriseLabExperimentTargetCatalog targetCatalog) {
        this.targetCatalog = Objects.requireNonNull(targetCatalog, "targetCatalog cannot be null");
    }

    @Override
    public synchronized AllocationInspection inspect(ReconstructedExperimentState reconstructedState) {
        ReconstructedExperimentState state = Objects.requireNonNull(
                reconstructedState, "reconstructedState cannot be null");
        EnterpriseLabLoopbackAllocationRouter router = routers.computeIfAbsent(
                state.experimentId(), ignored -> createRouter(state));
        return new AllocationInspection(router.currentSnapshot(), "PROCESS_LOCAL_ALLOCATION_INSPECTED");
    }

    @Override
    public synchronized BaselineRestorationReceipt restoreBaseline(
            ReconstructedExperimentState reconstructedState,
            String reason) {
        ReconstructedExperimentState state = Objects.requireNonNull(
                reconstructedState, "reconstructedState cannot be null");
        EnterpriseLabLoopbackAllocationRouter router = routers.computeIfAbsent(
                state.experimentId(), ignored -> createRouter(state));
        EnterpriseLabLoopbackAllocationRouter.AllocationChangeReceipt receipt =
                router.restoreBaseline(reason);
        boolean verified = EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                receipt.currentSnapshot().allocations(), state.baselineAllocation().allocations());
        return new BaselineRestorationReceipt(
                verified,
                receipt.trafficActionPerformed(),
                receipt.currentSnapshot(),
                verified ? "PROCESS_LOCAL_BASELINE_VERIFIED" : "PROCESS_LOCAL_BASELINE_UNVERIFIED");
    }

    private EnterpriseLabLoopbackAllocationRouter createRouter(ReconstructedExperimentState state) {
        if (routers.size() >= EnterpriseLabExperimentJournalDirectory.HARD_MAX_DISCOVERED_JOURNALS) {
            throw new IllegalStateException("bounded process-local recovery router capacity is exhausted");
        }
        var targets = targetCatalog.findTargets(state.scenarioId())
                .orElseThrow(() -> new IllegalStateException(
                        "recovered scenario has no repository-controlled loopback target binding"));
        Set<String> backendIds = new TreeSet<>();
        targets.forEach(target -> backendIds.add(target.backendId()));
        EnterpriseLabLoopbackObservationIngress ingress =
                new EnterpriseLabLoopbackObservationIngress(backendIds);
        return new EnterpriseLabLoopbackAllocationRouter(
                targets, ingress, state.baselineAllocation().allocations());
    }
}
