package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentScenario;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.core.TrafficAllocationGuardrailAction;
import com.richmond423.loadbalancerpro.core.TrafficAllocationGuardrailDecision;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackRequestClient.Execution;

import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceMutationAuthority.MutationAuthorization;

/**
 * Atomic data-plane seam for explicitly enabled Enterprise Lab loopback experiments only.
 */
public final class EnterpriseLabLoopbackAllocationRouter {
    private static final int MAX_REASON_LENGTH = 256;

    private final String scenarioId;
    private final Set<String> approvedBackendIds;
    private final boolean scenarioAllowsInfluence;
    private final EnterpriseLabLoopbackRequestClient requestClient;
    private final EnterpriseLabLoopbackAllocationSnapshot baseline;
    private final EnterpriseLabInstalledAllocationSnapshot baselineInstalled;
    private final AtomicReference<EnterpriseLabInstalledAllocationSnapshot> current;
    private final Optional<EnterpriseLabEvidenceMutationAuthority> mutationAuthority;
    private final Clock clock;

    public EnterpriseLabLoopbackAllocationRouter(
            Collection<EnterpriseLabLoopbackTarget> targets,
            EnterpriseLabLoopbackObservationIngress observationIngress,
            Map<String, Double> baselineAllocations) {
        this(targets, observationIngress, baselineAllocations, Optional.empty(), Clock.systemUTC());
    }

    EnterpriseLabLoopbackAllocationRouter(
            Collection<EnterpriseLabLoopbackTarget> targets,
            EnterpriseLabLoopbackObservationIngress observationIngress,
            Map<String, Double> baselineAllocations,
            Optional<EnterpriseLabEvidenceMutationAuthority> mutationAuthority) {
        this(targets, observationIngress, baselineAllocations, mutationAuthority, Clock.systemUTC());
    }

    EnterpriseLabLoopbackAllocationRouter(
            Collection<EnterpriseLabLoopbackTarget> targets,
            EnterpriseLabLoopbackObservationIngress observationIngress,
            Map<String, Double> baselineAllocations,
            Optional<EnterpriseLabEvidenceMutationAuthority> mutationAuthority,
            Clock clock) {
        List<EnterpriseLabLoopbackTarget> safeTargets = List.copyOf(
                Objects.requireNonNull(targets, "targets cannot be null"));
        if (safeTargets.isEmpty() || safeTargets.size() > EnterpriseLabLoopbackAllocationSnapshot.HARD_MAX_BACKENDS) {
            throw new IllegalArgumentException("target count must be between 1 and 64");
        }
        this.scenarioId = singleScenarioId(safeTargets);
        AdaptiveRoutingExperimentScenario scenario = new EnterpriseLabScenarioCatalogService()
                .findScenario(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "targets must belong to a repository-approved Enterprise Lab scenario"));
        this.approvedBackendIds = approvedBackends(safeTargets, scenario);
        this.scenarioAllowsInfluence = new EnterpriseLabScenarioCatalogService()
                .findScenarioMetadata(scenarioId)
                .orElseThrow()
                .safeForInfluenceExperiment();
        this.requestClient = new EnterpriseLabLoopbackRequestClient(safeTargets, observationIngress);
        this.mutationAuthority = Objects.requireNonNull(
                mutationAuthority, "mutationAuthority cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.baseline = EnterpriseLabLoopbackAllocationSnapshot.normalized(
                scenarioId,
                0,
                "recorded-baseline",
                Kind.BASELINE,
                approvedBackendIds,
                baselineAllocations);
        Optional<MutationAuthorization> initialAuthorization = requireMutationAuthorization();
        this.baselineInstalled = EnterpriseLabInstalledAllocationSnapshot.installed(
                baseline,
                this.clock,
                "SAFE_DEFAULT_INITIALIZED",
                ownerGeneration(initialAuthorization));
        this.current = new AtomicReference<>(baselineInstalled);
    }

    public EnterpriseLabLoopbackAllocationSnapshot baselineSnapshot() {
        return baseline;
    }

    public EnterpriseLabLoopbackAllocationSnapshot currentSnapshot() {
        return installedSnapshot().routingSnapshot();
    }

    public EnterpriseLabInstalledAllocationSnapshot baselineInstalledSnapshot() {
        return baselineInstalled;
    }

    /** Atomic side-effect-free read-back of the object used for new route selection. */
    public EnterpriseLabInstalledAllocationSnapshot installedSnapshot() {
        return current.get();
    }

    public synchronized AllocationChangeReceipt applyCandidate(
            EnterpriseLabAdaptiveDecision adaptiveDecision,
            boolean experimentExplicitlyEnabled) {
        Optional<MutationAuthorization> authorization = requireMutationAuthorization();
        EnterpriseLabInstalledAllocationSnapshot previousInstalled = current.get();
        requireNonRegressingOwnerGeneration(authorization, previousInstalled);
        EnterpriseLabLoopbackAllocationSnapshot previous = previousInstalled.routingSnapshot();
        if (!experimentExplicitlyEnabled) {
            return AllocationChangeReceipt.denied(previous, baseline,
                    "Enterprise Lab allocation actuation requires explicit enablement");
        }
        if (adaptiveDecision == null) {
            return AllocationChangeReceipt.denied(previous, baseline,
                    "an approved adaptive decision is required");
        }
        if (!scenarioId.equals(adaptiveDecision.scenarioId())) {
            return AllocationChangeReceipt.denied(previous, baseline,
                    "adaptive decision scenario does not match the fixed loopback target set");
        }
        if (!scenarioAllowsInfluence) {
            return AllocationChangeReceipt.denied(previous, baseline,
                    "the repository-approved scenario is not eligible for influence experiments");
        }

        TrafficAllocationGuardrailDecision guardrail = adaptiveDecision.decision().guardrailDecision();
        if (adaptiveDecision.decision().mode() != AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT
                || guardrail.mode() != AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT) {
            return AllocationChangeReceipt.denied(previous, baseline,
                    "only active-experiment decisions can alter loopback allocation");
        }
        if (!guardrail.influenceAllowed()
                || guardrail.action() == TrafficAllocationGuardrailAction.DENY) {
            return AllocationChangeReceipt.denied(previous, baseline,
                    "the adaptive allocation guardrail did not authorize influence");
        }
        if (!EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                baseline.allocations(), guardrail.baselineAllocations())) {
            return AllocationChangeReceipt.denied(previous, baseline,
                    "guardrail baseline does not match the recorded restorable baseline");
        }

        EnterpriseLabLoopbackAllocationSnapshot candidate;
        try {
            candidate = EnterpriseLabLoopbackAllocationSnapshot.normalized(
                    scenarioId,
                    previousInstalled.routerGeneration() + 1L,
                    adaptiveDecision.decision().decisionId(),
                    Kind.CANDIDATE,
                    approvedBackendIds,
                    guardrail.effectiveAllocations());
        } catch (IllegalArgumentException exception) {
            return AllocationChangeReceipt.denied(previous, baseline,
                    "candidate allocation failed closed: " + exception.getMessage());
        }
        if (previous.sameAllocations(candidate)) {
            return AllocationChangeReceipt.noChange(previous, baseline,
                    "approved allocation already matches the current loopback allocation");
        }

        requireSameMutationAuthorization(authorization);
        requireNonRegressingOwnerGeneration(authorization, previousInstalled);
        EnterpriseLabInstalledAllocationSnapshot installed =
                EnterpriseLabInstalledAllocationSnapshot.installed(
                        candidate,
                        clock,
                        "APPROVED_CANDIDATE_APPLIED",
                        ownerGeneration(authorization));
        current.set(installed);
        return AllocationChangeReceipt.applied(previous, candidate, baseline,
                "approved allocation atomically replaced the Enterprise Lab loopback snapshot");
    }

    public synchronized AllocationChangeReceipt restoreBaseline(String reason) {
        Optional<MutationAuthorization> authorization = requireMutationAuthorization();
        String safeReason = requireBoundedReason(reason);
        EnterpriseLabInstalledAllocationSnapshot previousInstalled = current.get();
        requireNonRegressingOwnerGeneration(authorization, previousInstalled);
        EnterpriseLabLoopbackAllocationSnapshot previous = previousInstalled.routingSnapshot();
        if (previous.sameAllocations(baseline)) {
            return AllocationChangeReceipt.noChange(previous, baseline,
                    "recorded baseline is already active: " + safeReason);
        }
        requireSameMutationAuthorization(authorization);
        requireNonRegressingOwnerGeneration(authorization, previousInstalled);
        EnterpriseLabLoopbackAllocationSnapshot restored =
                EnterpriseLabLoopbackAllocationSnapshot.normalized(
                        scenarioId,
                        previousInstalled.routerGeneration() + 1L,
                        "baseline-restore",
                        Kind.RESTORED_BASELINE,
                        approvedBackendIds,
                        baseline.allocations());
        EnterpriseLabInstalledAllocationSnapshot installed =
                EnterpriseLabInstalledAllocationSnapshot.installed(
                        restored,
                        clock,
                        "BASELINE_RESTORED: " + safeReason,
                        ownerGeneration(authorization));
        current.set(installed);
        return AllocationChangeReceipt.restored(previous, restored, baseline,
                "recorded baseline atomically restored: " + safeReason);
    }

    public RouteExecution route(
            String requestId,
            long selectionOrdinal,
            Duration timeout) {
        Optional<MutationAuthorization> authorization = requireMutationAuthorization();
        EnterpriseLabInstalledAllocationSnapshot selectedInstalled = current.get();
        requireNonRegressingOwnerGeneration(authorization, selectedInstalled);
        EnterpriseLabLoopbackAllocationSnapshot selectedSnapshot =
                selectedInstalled.routingSnapshot();
        String backendId = selectedSnapshot.selectBackend(selectionOrdinal);
        requireSameMutationAuthorization(authorization);
        Execution execution = requestClient.get(requestId, backendId, timeout);
        boolean candidateUsed = selectedSnapshot.kind() == Kind.CANDIDATE && execution.requestSent();
        return new RouteExecution(
                selectedSnapshot,
                backendId,
                execution,
                candidateUsed,
                candidateUsed,
                candidateUsed
                        ? "approved candidate allocation selected an Enterprise Lab loopback backend"
                        : "recorded safe allocation selected an Enterprise Lab loopback backend");
    }

    private Optional<MutationAuthorization> requireMutationAuthorization() {
        return mutationAuthority.map(
                EnterpriseLabEvidenceMutationAuthority::requireMutationAuthorization);
    }

    private void requireSameMutationAuthorization(
            Optional<MutationAuthorization> expected) {
        if (expected.isEmpty()) {
            return;
        }
        MutationAuthorization currentAuthorization = mutationAuthority.orElseThrow()
                .requireMutationAuthorization();
        expected.orElseThrow().requireSameEpoch(currentAuthorization);
    }

    private static long ownerGeneration(Optional<MutationAuthorization> authorization) {
        return authorization.map(MutationAuthorization::generation)
                .orElse(EnterpriseLabInstalledAllocationSnapshot.UNOWNED_GENERATION);
    }

    private static void requireNonRegressingOwnerGeneration(
            Optional<MutationAuthorization> authorization,
            EnterpriseLabInstalledAllocationSnapshot installed) {
        long currentGeneration = ownerGeneration(authorization);
        if (currentGeneration < installed.ownerGeneration()) {
            throw new EnterpriseLabEvidenceOwnershipException(
                    EnterpriseLabEvidenceOwnership.FailureClassification.RECORD_REPLACED,
                    "router ownership generation regressed below installed state");
        }
    }

    private static String singleScenarioId(List<EnterpriseLabLoopbackTarget> targets) {
        Set<String> scenarioIds = new TreeSet<>();
        targets.forEach(target -> scenarioIds.add(
                Objects.requireNonNull(target, "targets cannot contain null").scenarioId()));
        if (scenarioIds.size() != 1) {
            throw new IllegalArgumentException("all loopback targets must belong to one approved scenario");
        }
        return scenarioIds.iterator().next();
    }

    private static Set<String> approvedBackends(
            List<EnterpriseLabLoopbackTarget> targets,
            AdaptiveRoutingExperimentScenario scenario) {
        Set<String> targetBackendIds = new TreeSet<>();
        for (EnterpriseLabLoopbackTarget target : targets) {
            if (!targetBackendIds.add(target.backendId())) {
                throw new IllegalArgumentException("loopback target backendIds must be unique");
            }
        }
        Set<String> catalogBackendIds = new TreeSet<>();
        scenario.servers().forEach(server -> catalogBackendIds.add(server.id()));
        if (!targetBackendIds.equals(catalogBackendIds)) {
            throw new IllegalArgumentException(
                    "loopback targets must exactly match the approved scenario-catalog backend set");
        }
        return Set.copyOf(targetBackendIds);
    }

    private static String requireBoundedReason(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("restore reason cannot be null or blank");
        }
        String reason = value.trim();
        if (reason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("restore reason cannot exceed 256 characters");
        }
        return reason;
    }

    public enum ChangeStatus {
        APPLIED,
        RESTORED,
        DENIED,
        NO_CHANGE
    }

    public record AllocationChangeReceipt(
            ChangeStatus status,
            EnterpriseLabLoopbackAllocationSnapshot previousSnapshot,
            EnterpriseLabLoopbackAllocationSnapshot currentSnapshot,
            EnterpriseLabLoopbackAllocationSnapshot baselineSnapshot,
            boolean trafficActionPerformed,
            String scope,
            String reason) {
        public AllocationChangeReceipt {
            status = Objects.requireNonNull(status, "status cannot be null");
            previousSnapshot = Objects.requireNonNull(previousSnapshot, "previousSnapshot cannot be null");
            currentSnapshot = Objects.requireNonNull(currentSnapshot, "currentSnapshot cannot be null");
            baselineSnapshot = Objects.requireNonNull(baselineSnapshot, "baselineSnapshot cannot be null");
            scope = requireBoundedReason(scope);
            reason = requireBoundedReason(reason);
            if (trafficActionPerformed != (status == ChangeStatus.APPLIED || status == ChangeStatus.RESTORED)) {
                throw new IllegalArgumentException("trafficActionPerformed must reflect an atomic allocation change");
            }
        }

        private static AllocationChangeReceipt applied(
                EnterpriseLabLoopbackAllocationSnapshot previous,
                EnterpriseLabLoopbackAllocationSnapshot current,
                EnterpriseLabLoopbackAllocationSnapshot baseline,
                String reason) {
            return new AllocationChangeReceipt(ChangeStatus.APPLIED, previous, current, baseline, true,
                    "Enterprise Lab literal-loopback routing only", reason);
        }

        private static AllocationChangeReceipt restored(
                EnterpriseLabLoopbackAllocationSnapshot previous,
                EnterpriseLabLoopbackAllocationSnapshot current,
                EnterpriseLabLoopbackAllocationSnapshot baseline,
                String reason) {
            return new AllocationChangeReceipt(ChangeStatus.RESTORED, previous, current, baseline, true,
                    "Enterprise Lab literal-loopback routing only", reason);
        }

        private static AllocationChangeReceipt denied(
                EnterpriseLabLoopbackAllocationSnapshot current,
                EnterpriseLabLoopbackAllocationSnapshot baseline,
                String reason) {
            return new AllocationChangeReceipt(ChangeStatus.DENIED, current, current, baseline, false,
                    "no routing state altered", reason);
        }

        private static AllocationChangeReceipt noChange(
                EnterpriseLabLoopbackAllocationSnapshot current,
                EnterpriseLabLoopbackAllocationSnapshot baseline,
                String reason) {
            return new AllocationChangeReceipt(ChangeStatus.NO_CHANGE, current, current, baseline, false,
                    "no routing state altered", reason);
        }
    }

    public record RouteExecution(
            EnterpriseLabLoopbackAllocationSnapshot allocationSnapshot,
            String selectedBackendId,
            Execution requestExecution,
            boolean candidateAllocationUsed,
            boolean trafficActionPerformed,
            String reason) {
        public RouteExecution {
            allocationSnapshot = Objects.requireNonNull(allocationSnapshot, "allocationSnapshot cannot be null");
            selectedBackendId = Objects.requireNonNull(selectedBackendId, "selectedBackendId cannot be null");
            requestExecution = Objects.requireNonNull(requestExecution, "requestExecution cannot be null");
            reason = requireBoundedReason(reason);
            if (candidateAllocationUsed != trafficActionPerformed) {
                throw new IllegalArgumentException("candidate use must match the local traffic action indicator");
            }
            if (trafficActionPerformed && !requestExecution.requestSent()) {
                throw new IllegalArgumentException("traffic action requires an actual loopback request");
            }
            if (!selectedBackendId.equals(requestExecution.backendId())) {
                throw new IllegalArgumentException("selected backend must match the loopback execution");
            }
        }
    }
}
