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
    private final InstalledStateStore installedStateStore;
    private final Optional<EnterpriseLabEvidenceMutationAuthority> mutationAuthority;
    private final Clock clock;

    public EnterpriseLabLoopbackAllocationRouter(
            Collection<EnterpriseLabLoopbackTarget> targets,
            EnterpriseLabLoopbackObservationIngress observationIngress,
            Map<String, Double> baselineAllocations) {
        this(targets, observationIngress, baselineAllocations, Optional.empty(), Clock.systemUTC(), Optional.empty());
    }

    public static EnterpriseLabLoopbackAllocationRouter owned(
            Collection<EnterpriseLabLoopbackTarget> targets,
            EnterpriseLabLoopbackObservationIngress observationIngress,
            Map<String, Double> baselineAllocations,
            EnterpriseLabEvidenceOwnershipGate ownershipGate) {
        return new EnterpriseLabLoopbackAllocationRouter(
                targets,
                observationIngress,
                baselineAllocations,
                Optional.of(Objects.requireNonNull(
                        ownershipGate, "ownershipGate cannot be null")),
                Clock.systemUTC(),
                Optional.empty());
    }

    EnterpriseLabLoopbackAllocationRouter(
            Collection<EnterpriseLabLoopbackTarget> targets,
            EnterpriseLabLoopbackObservationIngress observationIngress,
            Map<String, Double> baselineAllocations,
            Optional<EnterpriseLabEvidenceMutationAuthority> mutationAuthority) {
        this(targets, observationIngress, baselineAllocations, mutationAuthority, Clock.systemUTC(), Optional.empty());
    }

    EnterpriseLabLoopbackAllocationRouter(
            Collection<EnterpriseLabLoopbackTarget> targets,
            EnterpriseLabLoopbackObservationIngress observationIngress,
            Map<String, Double> baselineAllocations,
            Optional<EnterpriseLabEvidenceMutationAuthority> mutationAuthority,
            Clock clock) {
        this(targets, observationIngress, baselineAllocations, mutationAuthority, clock, Optional.empty());
    }

    /**
     * Proof-only composition seam for an independently hosted installed-state
     * holder. Production construction never supplies this seam.
     */
    EnterpriseLabLoopbackAllocationRouter(
            Collection<EnterpriseLabLoopbackTarget> targets,
            EnterpriseLabLoopbackObservationIngress observationIngress,
            Map<String, Double> baselineAllocations,
            Optional<EnterpriseLabEvidenceMutationAuthority> mutationAuthority,
            Clock clock,
            InstalledStateStore proofInstalledStateStore) {
        this(targets, observationIngress, baselineAllocations, mutationAuthority, clock,
                Optional.of(Objects.requireNonNull(
                        proofInstalledStateStore, "proofInstalledStateStore cannot be null")));
    }

    private EnterpriseLabLoopbackAllocationRouter(
            Collection<EnterpriseLabLoopbackTarget> targets,
            EnterpriseLabLoopbackObservationIngress observationIngress,
            Map<String, Double> baselineAllocations,
            Optional<EnterpriseLabEvidenceMutationAuthority> mutationAuthority,
            Clock clock,
            Optional<InstalledStateStore> proofInstalledStateStore) {
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
        this.installedStateStore = proofInstalledStateStore.orElseGet(
                () -> new InMemoryInstalledStateStore(baselineInstalled));
        validateInstalledState(this.installedStateStore.read());
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
        return validateInstalledState(installedStateStore.read());
    }

    /**
     * Validates and canonically normalizes candidate intent without mutating
     * router state. The coordinator uses this same seam before durable intent.
     */
    synchronized CandidateIntentValidation validateCandidateIntent(
            EnterpriseLabAdaptiveDecision adaptiveDecision,
            boolean experimentExplicitlyEnabled) {
        if (!experimentExplicitlyEnabled) {
            return CandidateIntentValidation.denied(
                    "Enterprise Lab allocation actuation requires explicit enablement");
        }
        if (adaptiveDecision == null) {
            return CandidateIntentValidation.denied(
                    "an approved adaptive decision is required");
        }
        if (!scenarioId.equals(adaptiveDecision.scenarioId())) {
            return CandidateIntentValidation.denied(
                    "adaptive decision scenario does not match the fixed loopback target set");
        }
        if (!scenarioAllowsInfluence) {
            return CandidateIntentValidation.denied(
                    "the repository-approved scenario is not eligible for influence experiments");
        }

        TrafficAllocationGuardrailDecision guardrail = adaptiveDecision.decision().guardrailDecision();
        if (adaptiveDecision.decision().mode() != AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT
                || guardrail.mode() != AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT) {
            return CandidateIntentValidation.denied(
                    "only active-experiment decisions can alter loopback allocation");
        }
        if (!guardrail.influenceAllowed()
                || guardrail.action() == TrafficAllocationGuardrailAction.DENY) {
            return CandidateIntentValidation.denied(
                    "the adaptive allocation guardrail did not authorize influence");
        }
        if (!EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                baseline.allocations(), guardrail.baselineAllocations())) {
            return CandidateIntentValidation.denied(
                    "guardrail baseline does not match the recorded restorable baseline");
        }

        try {
            Map<String, Double> requested =
                    EnterpriseLabLoopbackAllocationSnapshot.exactNormalizedAllocations(
                            approvedBackendIds, guardrail.requestedAllocations());
            Map<String, Double> approved =
                    EnterpriseLabLoopbackAllocationSnapshot.exactNormalizedAllocations(
                            approvedBackendIds, guardrail.effectiveAllocations());
            return CandidateIntentValidation.authorized(
                    adaptiveDecision.decision().decisionId(),
                    scenarioId,
                    baseline.allocations(),
                    requested,
                    approved);
        } catch (IllegalArgumentException exception) {
            return CandidateIntentValidation.denied(
                    "candidate allocation failed closed: " + exception.getMessage());
        }
    }

    public synchronized AllocationChangeReceipt applyCandidate(
            EnterpriseLabAdaptiveDecision adaptiveDecision,
            boolean experimentExplicitlyEnabled) {
        Optional<MutationAuthorization> authorization = requireMutationAuthorization();
        EnterpriseLabInstalledAllocationSnapshot previousInstalled = installedSnapshot();
        requireNonRegressingOwnerGeneration(authorization, previousInstalled);
        EnterpriseLabLoopbackAllocationSnapshot previous = previousInstalled.routingSnapshot();
        CandidateIntentValidation validation = validateCandidateIntent(
                adaptiveDecision, experimentExplicitlyEnabled);
        if (!validation.authorized()) {
            return AllocationChangeReceipt.denied(previous, baseline, validation.reason());
        }

        EnterpriseLabLoopbackAllocationSnapshot candidate;
        try {
            candidate = EnterpriseLabLoopbackAllocationSnapshot.normalized(
                    scenarioId,
                    previousInstalled.routerGeneration() + 1L,
                    validation.decisionId(),
                    Kind.CANDIDATE,
                    approvedBackendIds,
                    validation.approvedAllocation());
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
        replaceInstalled(previousInstalled, installed);
        return AllocationChangeReceipt.applied(previous, candidate, baseline,
                "approved allocation atomically replaced the Enterprise Lab loopback snapshot");
    }

    public synchronized AllocationChangeReceipt restoreBaseline(String reason) {
        Optional<MutationAuthorization> authorization = requireMutationAuthorization();
        String safeReason = requireBoundedReason(reason);
        EnterpriseLabInstalledAllocationSnapshot previousInstalled = installedSnapshot();
        requireNonRegressingOwnerGeneration(authorization, previousInstalled);
        EnterpriseLabLoopbackAllocationSnapshot previous = previousInstalled.routingSnapshot();
        if (previous.sameAllocations(baseline)
                && previousInstalled.ownerGeneration() == ownerGeneration(authorization)) {
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
        replaceInstalled(previousInstalled, installed);
        return AllocationChangeReceipt.restored(previous, restored, baseline,
                "recorded baseline atomically restored: " + safeReason);
    }

    public RouteExecution route(
            String requestId,
            long selectionOrdinal,
            Duration timeout) {
        Optional<MutationAuthorization> authorization = requireMutationAuthorization();
        EnterpriseLabInstalledAllocationSnapshot selectedInstalled = installedSnapshot();
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

    record CandidateIntentValidation(
            boolean authorized,
            String decisionId,
            String scenarioId,
            Map<String, Double> baselineAllocation,
            Map<String, Double> requestedAllocation,
            Map<String, Double> approvedAllocation,
            String reason) {
        CandidateIntentValidation {
            decisionId = decisionId == null ? "NONE" : decisionId;
            scenarioId = scenarioId == null ? "NONE" : scenarioId;
            baselineAllocation = Map.copyOf(Objects.requireNonNull(
                    baselineAllocation, "baselineAllocation cannot be null"));
            requestedAllocation = Map.copyOf(Objects.requireNonNull(
                    requestedAllocation, "requestedAllocation cannot be null"));
            approvedAllocation = Map.copyOf(Objects.requireNonNull(
                    approvedAllocation, "approvedAllocation cannot be null"));
            reason = requireBoundedReason(reason);
            if (authorized && (baselineAllocation.isEmpty()
                    || requestedAllocation.isEmpty()
                    || approvedAllocation.isEmpty())) {
                throw new IllegalArgumentException(
                        "authorized candidate intent requires complete allocations");
            }
        }

        private static CandidateIntentValidation authorized(
                String decisionId,
                String scenarioId,
                Map<String, Double> baseline,
                Map<String, Double> requested,
                Map<String, Double> approved) {
            return new CandidateIntentValidation(
                    true, decisionId, scenarioId, baseline, requested, approved,
                    "adaptive decision and guardrail authorized bounded loopback allocation intent");
        }

        private static CandidateIntentValidation denied(String reason) {
            return new CandidateIntentValidation(
                    false, "NONE", "NONE", Map.of(), Map.of(), Map.of(), reason);
        }
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

    private EnterpriseLabInstalledAllocationSnapshot validateInstalledState(
            EnterpriseLabInstalledAllocationSnapshot installed) {
        EnterpriseLabInstalledAllocationSnapshot safe = Objects.requireNonNull(
                installed, "installed state store returned null");
        if (!scenarioId.equals(safe.routingSnapshot().scenarioId())
                || !approvedBackendIds.equals(safe.routingSnapshot().allocations().keySet())) {
            throw new IllegalStateException(
                    "installed state store returned an allocation outside the fixed router identity");
        }
        return safe;
    }

    private void replaceInstalled(
            EnterpriseLabInstalledAllocationSnapshot expected,
            EnterpriseLabInstalledAllocationSnapshot update) {
        validateInstalledState(expected);
        validateInstalledState(update);
        if (!installedStateStore.compareAndSet(expected, update)) {
            throw new IllegalStateException(
                    "installed allocation changed before atomic replacement");
        }
    }

    interface InstalledStateStore {
        EnterpriseLabInstalledAllocationSnapshot read();

        boolean compareAndSet(
                EnterpriseLabInstalledAllocationSnapshot expected,
                EnterpriseLabInstalledAllocationSnapshot update);
    }

    private static final class InMemoryInstalledStateStore implements InstalledStateStore {
        private final AtomicReference<EnterpriseLabInstalledAllocationSnapshot> current;

        private InMemoryInstalledStateStore(
                EnterpriseLabInstalledAllocationSnapshot initial) {
            this.current = new AtomicReference<>(Objects.requireNonNull(
                    initial, "initial installed state cannot be null"));
        }

        @Override
        public EnterpriseLabInstalledAllocationSnapshot read() {
            return current.get();
        }

        @Override
        public boolean compareAndSet(
                EnterpriseLabInstalledAllocationSnapshot expected,
                EnterpriseLabInstalledAllocationSnapshot update) {
            return current.compareAndSet(expected, update);
        }
    }

    public enum ChangeStatus {
        APPLIED,
        RESTORED,
        FAILED,
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
            boolean requiresTrafficAction = status == ChangeStatus.APPLIED
                    || status == ChangeStatus.RESTORED;
            boolean forbidsTrafficAction = status == ChangeStatus.DENIED
                    || status == ChangeStatus.NO_CHANGE;
            if (requiresTrafficAction && !trafficActionPerformed
                    || forbidsTrafficAction && trafficActionPerformed) {
                throw new IllegalArgumentException(
                        "trafficActionPerformed must reflect an atomic allocation change");
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
