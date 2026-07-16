package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.core.TrafficAllocationGuardrailAction;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.AllocationChangeReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.ChangeStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.RouteExecution;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackObservationIngress.ReceiptStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Synchronous, bounded command/cycle runner for one Enterprise Lab experiment.
 */
public final class EnterpriseLabExperimentLifecycle {
    public static final String SNAPSHOT_SCHEMA_VERSION = "enterprise-lab-experiment-lifecycle/v1";
    private static final int MAX_TRANSITIONS = 16;
    private static final int MAX_COMMAND_RESULTS = 64;
    private static final int MAX_ID_LENGTH = 128;
    private static final int MAX_REASON_LENGTH = 256;

    private final List<EnterpriseLabExperimentTransition> transitions = new ArrayList<>();
    private final Map<String, CachedCommand> commandResults = new LinkedHashMap<>();
    private final Map<String, CachedProgress> requestResults = new LinkedHashMap<>();
    private final Map<String, CachedProgress> holdCycleResults = new LinkedHashMap<>();
    private final Set<String> evidenceRequestIds = new LinkedHashSet<>();

    private EnterpriseLabExperimentState state = EnterpriseLabExperimentState.IDLE;
    private EnterpriseLabExperimentConfiguration configuration;
    private int requestCount;
    private int evidenceCount;
    private int completedHoldDownCycles;
    private Instant startedAt;
    private Instant lastActivityAt;
    private boolean candidateAllocationActive;
    private long allocationRevision;

    public synchronized CommandReceipt arm(
            String commandId,
            EnterpriseLabExperimentConfiguration requestedConfiguration,
            Instant occurredAt) {
        String safeCommandId = requireCanonicalId(commandId, "commandId");
        EnterpriseLabExperimentConfiguration safeConfiguration = Objects.requireNonNull(
                requestedConfiguration, "requestedConfiguration cannot be null");
        Instant safeTime = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        String signature = signature("arm", safeTime, safeConfiguration.contentFingerprint());
        CommandReceipt duplicate = duplicateCommand(safeCommandId, signature);
        if (duplicate != null) {
            return duplicate;
        }
        if (!canAcceptCommand()) {
            return capacityRejected();
        }
        if (state != EnterpriseLabExperimentState.IDLE) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.ILLEGAL, "only an idle lifecycle can be armed"));
        }

        this.configuration = safeConfiguration;
        this.lastActivityAt = safeTime;
        String rejection = armRejectionReason(safeConfiguration, safeTime);
        if (rejection != null) {
            EnterpriseLabExperimentTransition transition = changeState(
                    EnterpriseLabExperimentState.REJECTED, safeCommandId, safeTime, rejection);
            return store(safeCommandId, signature, commandWithTransition(
                    CommandStatus.REJECTED, transition, rejection));
        }
        EnterpriseLabExperimentTransition transition = changeState(
                EnterpriseLabExperimentState.ARMED,
                safeCommandId,
                safeTime,
                "bounded Enterprise Lab experiment configuration validated and armed");
        return store(safeCommandId, signature, commandWithTransition(
                CommandStatus.APPLIED, transition, "experiment armed"));
    }

    public synchronized CommandReceipt start(
            String commandId,
            AllocationChangeReceipt allocationReceipt,
            Instant occurredAt) {
        String safeCommandId = requireCanonicalId(commandId, "commandId");
        AllocationChangeReceipt safeReceipt = Objects.requireNonNull(
                allocationReceipt, "allocationReceipt cannot be null");
        Instant safeTime = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        String signature = signature("start", safeTime, safeReceipt.toString());
        CommandReceipt duplicate = duplicateCommand(safeCommandId, signature);
        if (duplicate != null) {
            return duplicate;
        }
        if (!canAcceptCommand()) {
            return capacityRejected();
        }
        if (state != EnterpriseLabExperimentState.ARMED) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.ILLEGAL, "only an armed experiment can start"));
        }
        if (timePrecedesActivity(safeTime)) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.ILLEGAL, "start time cannot precede recorded lifecycle activity"));
        }
        if (safeTime.isBefore(configuration.createdAt()) || !safeTime.isBefore(configuration.expiresAt())) {
            EnterpriseLabExperimentTransition transition = changeState(
                    EnterpriseLabExperimentState.CANCELLED,
                    safeCommandId,
                    safeTime,
                    "experiment expired before candidate allocation start");
            return store(safeCommandId, signature, commandWithTransition(
                    CommandStatus.REJECTED, transition, "expired experiment was cancelled before start"));
        }
        if (!candidateReceiptMatches(safeReceipt)) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.REJECTED,
                    "candidate allocation receipt does not match the armed decision and target scope"));
        }

        candidateAllocationActive = true;
        allocationRevision = safeReceipt.currentSnapshot().revision();
        startedAt = safeTime;
        lastActivityAt = safeTime;
        EnterpriseLabExperimentTransition transition = changeState(
                EnterpriseLabExperimentState.RUNNING,
                safeCommandId,
                safeTime,
                "approved candidate allocation confirmed active for bounded loopback requests");
        return store(safeCommandId, signature, commandWithTransition(
                CommandStatus.APPLIED, transition, "experiment started"));
    }

    public synchronized ProgressReceipt recordRequest(
            RouteExecution routeExecution,
            Instant occurredAt) {
        RouteExecution safeExecution = Objects.requireNonNull(routeExecution, "routeExecution cannot be null");
        String requestId = safeExecution.requestExecution().requestId();
        if (configuration == null
                || !safeExecution.candidateAllocationUsed()
                || !safeExecution.trafficActionPerformed()
                || !safeExecution.requestExecution().requestSent()
                || safeExecution.allocationSnapshot().kind() != Kind.CANDIDATE
                || !safeExecution.allocationSnapshot().scenarioId().equals(configuration.scenarioId())
                || !safeExecution.allocationSnapshot().sourceDecisionId().equals(configuration.candidateDecisionId())) {
            return progressWithoutRecord(
                    ProgressStatus.DENIED,
                    requireCanonicalId(requestId, "requestId"),
                    "request progress requires an actual approved candidate loopback route");
        }
        boolean observationRecorded = safeExecution.requestExecution()
                .observationReceipt().status() == ReceiptStatus.RECORDED;
        return recordRequestInternal(
                requestId,
                observationRecorded,
                occurredAt,
                "route:" + safeExecution.allocationSnapshot().revision() + ":"
                        + safeExecution.selectedBackendId() + ":" + observationRecorded);
    }

    synchronized ProgressReceipt recordRequestProgress(
            String requestId,
            boolean observationRecorded,
            Instant occurredAt) {
        return recordRequestInternal(
                requestId, observationRecorded, occurredAt, "progress:" + observationRecorded);
    }

    private ProgressReceipt recordRequestInternal(
            String requestId,
            boolean observationRecorded,
            Instant occurredAt,
            String evidenceSignature) {
        String safeRequestId = requireCanonicalId(requestId, "requestId");
        Instant safeTime = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        String signature = signature("request", safeTime, evidenceSignature);
        ProgressReceipt duplicate = duplicateProgress(requestResults, safeRequestId, signature);
        if (duplicate != null) {
            return duplicate;
        }
        if (state != EnterpriseLabExperimentState.RUNNING || !candidateAllocationActive) {
            return progressWithoutRecord(
                    ProgressStatus.DENIED, safeRequestId, "requests are accepted only while candidate routing is running");
        }
        if (safeTime.isBefore(startedAt)
                || !safeTime.isBefore(configuration.expiresAt())
                || !safeTime.isBefore(startedAt.plus(configuration.maximumDuration()))) {
            return progressWithoutRecord(
                    ProgressStatus.DENIED, safeRequestId, "request is outside the bounded experiment time window");
        }
        if (requestCount >= configuration.maximumRequestCount()) {
            return progressWithoutRecord(
                    ProgressStatus.DENIED, safeRequestId, "maximum bounded request count has been reached");
        }

        requestCount++;
        if (observationRecorded) {
            evidenceCount++;
            evidenceRequestIds.add(safeRequestId);
        }
        if (lastActivityAt == null || safeTime.isAfter(lastActivityAt)) {
            lastActivityAt = safeTime;
        }
        ProgressReceipt recorded = new ProgressReceipt(
                ProgressStatus.RECORDED,
                safeRequestId,
                snapshot("request progress recorded"),
                requestCount >= configuration.maximumRequestCount(),
                "bounded request progress recorded");
        requestResults.put(safeRequestId, new CachedProgress(signature, recorded));
        return recorded;
    }

    public synchronized CommandReceipt advance(String commandId, Instant occurredAt) {
        String safeCommandId = requireCanonicalId(commandId, "commandId");
        Instant safeTime = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        String signature = signature("advance", safeTime, state.name());
        CommandReceipt duplicate = duplicateCommand(safeCommandId, signature);
        if (duplicate != null) {
            return duplicate;
        }
        if (!canAcceptCommand()) {
            return capacityRejected();
        }
        if (configuration == null || state.terminal()) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.ILLEGAL, "terminal or unconfigured lifecycle cannot advance"));
        }
        if (lastActivityAt != null && safeTime.isBefore(lastActivityAt)) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.ILLEGAL, "advance time cannot precede recorded lifecycle activity"));
        }

        if (!safeTime.isBefore(configuration.expiresAt())) {
            if (state == EnterpriseLabExperimentState.ARMED) {
                EnterpriseLabExperimentTransition transition = changeState(
                        EnterpriseLabExperimentState.CANCELLED,
                        safeCommandId,
                        safeTime,
                        "armed experiment expired before candidate start");
                return store(safeCommandId, signature, commandWithTransition(
                        CommandStatus.APPLIED, transition, "expired armed experiment cancelled"));
            }
            if (candidateAllocationActive && (state == EnterpriseLabExperimentState.RUNNING
                    || state == EnterpriseLabExperimentState.HOLDING
                    || state == EnterpriseLabExperimentState.COMPLETING)) {
                EnterpriseLabExperimentTransition transition = changeState(
                        EnterpriseLabExperimentState.ROLLING_BACK,
                        safeCommandId,
                        safeTime,
                        "experiment expiration requires safe-baseline restoration");
                return store(safeCommandId, signature, commandWithTransition(
                        CommandStatus.APPLIED, transition, "expired active experiment entered rollback"));
            }
        }

        if (state == EnterpriseLabExperimentState.RUNNING
                && (requestCount >= configuration.maximumRequestCount()
                || !safeTime.isBefore(startedAt.plus(configuration.maximumDuration())))) {
            EnterpriseLabExperimentTransition transition = changeState(
                    EnterpriseLabExperimentState.HOLDING,
                    safeCommandId,
                    safeTime,
                    "bounded request count or duration reached; hold-down evaluation begins");
            return store(safeCommandId, signature, commandWithTransition(
                    CommandStatus.APPLIED, transition, "experiment entered hold-down"));
        }

        return store(safeCommandId, signature, commandWithoutTransition(
                CommandStatus.NO_CHANGE, "no deterministic lifecycle boundary was reached"));
    }

    public synchronized ProgressReceipt recordHoldDownCycle(String cycleId, Instant occurredAt) {
        String safeCycleId = requireCanonicalId(cycleId, "cycleId");
        Instant safeTime = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        String signature = signature("hold-cycle", safeTime, state.name());
        ProgressReceipt duplicate = duplicateProgress(holdCycleResults, safeCycleId, signature);
        if (duplicate != null) {
            return duplicate;
        }
        if (state != EnterpriseLabExperimentState.HOLDING || !candidateAllocationActive) {
            return progressWithoutRecord(
                    ProgressStatus.DENIED, safeCycleId, "hold-down cycles require an active holding experiment");
        }
        if (timePrecedesActivity(safeTime)) {
            return progressWithoutRecord(
                    ProgressStatus.DENIED, safeCycleId, "hold-down cycle cannot precede lifecycle activity");
        }
        if (!safeTime.isBefore(configuration.expiresAt())) {
            return progressWithoutRecord(
                    ProgressStatus.DENIED, safeCycleId, "hold-down cycle is outside experiment expiration");
        }
        if (completedHoldDownCycles >= configuration.holdDownCycles()) {
            return progressWithoutRecord(
                    ProgressStatus.DENIED, safeCycleId, "configured hold-down cycle count is already complete");
        }

        completedHoldDownCycles++;
        if (lastActivityAt == null || safeTime.isAfter(lastActivityAt)) {
            lastActivityAt = safeTime;
        }
        boolean ready = completedHoldDownCycles >= configuration.holdDownCycles();
        ProgressReceipt recorded = new ProgressReceipt(
                ProgressStatus.RECORDED,
                safeCycleId,
                snapshot("hold-down cycle recorded"),
                ready,
                ready ? "configured hold-down is complete" : "bounded hold-down cycle recorded");
        holdCycleResults.put(safeCycleId, new CachedProgress(signature, recorded));
        return recorded;
    }

    public synchronized CommandReceipt beginCompletion(String commandId, Instant occurredAt) {
        String safeCommandId = requireCanonicalId(commandId, "commandId");
        Instant safeTime = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        String signature = signature("begin-completion", safeTime, countersSignature());
        CommandReceipt duplicate = duplicateCommand(safeCommandId, signature);
        if (duplicate != null) {
            return duplicate;
        }
        if (!canAcceptCommand()) {
            return capacityRejected();
        }
        if (state != EnterpriseLabExperimentState.HOLDING || !candidateAllocationActive) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.ILLEGAL, "completion requires an active holding experiment"));
        }
        if (timePrecedesActivity(safeTime)) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.ILLEGAL, "completion time cannot precede lifecycle activity"));
        }
        if (evidenceCount < configuration.minimumEvidenceCount()
                || completedHoldDownCycles < configuration.holdDownCycles()) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.REJECTED,
                    "minimum evidence and configured hold-down cycles must complete before completion"));
        }
        if (!safeTime.isBefore(configuration.expiresAt())) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.REJECTED, "expired experiment must roll back instead of completing"));
        }
        EnterpriseLabExperimentTransition transition = changeState(
                EnterpriseLabExperimentState.COMPLETING,
                safeCommandId,
                safeTime,
                "hold-down and evidence gates passed; safe completion confirmation requested");
        return store(safeCommandId, signature, commandWithTransition(
                CommandStatus.APPLIED, transition, "experiment entered completing state"));
    }

    public synchronized CommandReceipt confirmCompletion(
            String commandId,
            AllocationChangeReceipt baselineReceipt,
            Instant occurredAt) {
        return confirmSafeTerminal(
                commandId,
                baselineReceipt,
                occurredAt,
                EnterpriseLabExperimentState.COMPLETING,
                EnterpriseLabExperimentState.COMPLETED,
                "confirm-completion",
                "candidate removed and safe baseline confirmed before completion");
    }

    public synchronized CommandReceipt beginRollback(
            String commandId,
            String reason,
            Instant occurredAt) {
        String safeCommandId = requireCanonicalId(commandId, "commandId");
        String safeReason = requireBoundedReason(reason);
        Instant safeTime = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        String signature = signature("begin-rollback", safeTime, safeReason);
        CommandReceipt duplicate = duplicateCommand(safeCommandId, signature);
        if (duplicate != null) {
            return duplicate;
        }
        if (!canAcceptCommand()) {
            return capacityRejected();
        }
        if (!candidateAllocationActive || (state != EnterpriseLabExperimentState.RUNNING
                && state != EnterpriseLabExperimentState.HOLDING
                && state != EnterpriseLabExperimentState.COMPLETING)) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.ILLEGAL, "rollback requires an active candidate allocation"));
        }
        if (timePrecedesActivity(safeTime)) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.ILLEGAL, "rollback time cannot precede lifecycle activity"));
        }
        EnterpriseLabExperimentTransition transition = changeState(
                EnterpriseLabExperimentState.ROLLING_BACK,
                safeCommandId,
                safeTime,
                safeReason);
        return store(safeCommandId, signature, commandWithTransition(
                CommandStatus.APPLIED, transition, "experiment entered rolling-back state"));
    }

    public synchronized CommandReceipt confirmRollback(
            String commandId,
            AllocationChangeReceipt baselineReceipt,
            Instant occurredAt) {
        return confirmSafeTerminal(
                commandId,
                baselineReceipt,
                occurredAt,
                EnterpriseLabExperimentState.ROLLING_BACK,
                EnterpriseLabExperimentState.ROLLED_BACK,
                "confirm-rollback",
                "safe baseline confirmed after rollback");
    }

    public synchronized CommandReceipt cancel(String commandId, String reason, Instant occurredAt) {
        return inactiveTerminal(
                commandId,
                reason,
                occurredAt,
                EnterpriseLabExperimentState.CANCELLED,
                "cancel");
    }

    public synchronized CommandReceipt fail(String commandId, String reason, Instant occurredAt) {
        return inactiveTerminal(
                commandId,
                reason,
                occurredAt,
                EnterpriseLabExperimentState.FAILED,
                "fail");
    }

    public synchronized LifecycleSnapshot snapshot() {
        return snapshot("current lifecycle state");
    }

    public synchronized List<EnterpriseLabExperimentTransition> transitionHistory() {
        return List.copyOf(transitions);
    }

    /**
     * Bounded identifiers for actual candidate routes whose PR1 observation receipts were recorded.
     */
    public synchronized List<String> recordedEvidenceRequestIds() {
        return List.copyOf(evidenceRequestIds);
    }

    public synchronized Optional<EnterpriseLabExperimentConfiguration> configuration() {
        return Optional.ofNullable(configuration);
    }

    private CommandReceipt confirmSafeTerminal(
            String commandId,
            AllocationChangeReceipt baselineReceipt,
            Instant occurredAt,
            EnterpriseLabExperimentState requiredState,
            EnterpriseLabExperimentState terminalState,
            String operation,
            String transitionReason) {
        String safeCommandId = requireCanonicalId(commandId, "commandId");
        AllocationChangeReceipt safeReceipt = Objects.requireNonNull(
                baselineReceipt, "baselineReceipt cannot be null");
        Instant safeTime = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        String signature = signature(operation, safeTime, safeReceipt.toString());
        CommandReceipt duplicate = duplicateCommand(safeCommandId, signature);
        if (duplicate != null) {
            return duplicate;
        }
        if (!canAcceptCommand()) {
            return capacityRejected();
        }
        if (state != requiredState || !candidateAllocationActive) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.ILLEGAL, operation + " is not valid from the current lifecycle state"));
        }
        if (timePrecedesActivity(safeTime)) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.ILLEGAL, operation + " time cannot precede lifecycle activity"));
        }
        if (!safeBaselineReceiptMatches(safeReceipt)) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.REJECTED,
                    "terminal transition requires an atomic receipt confirming the recorded safe baseline"));
        }
        candidateAllocationActive = false;
        allocationRevision = safeReceipt.currentSnapshot().revision();
        EnterpriseLabExperimentTransition transition = changeState(
                terminalState, safeCommandId, safeTime, transitionReason);
        return store(safeCommandId, signature, commandWithTransition(
                CommandStatus.APPLIED, transition, transitionReason));
    }

    private CommandReceipt inactiveTerminal(
            String commandId,
            String reason,
            Instant occurredAt,
            EnterpriseLabExperimentState terminalState,
            String operation) {
        String safeCommandId = requireCanonicalId(commandId, "commandId");
        String safeReason = requireBoundedReason(reason);
        Instant safeTime = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        String signature = signature(operation, safeTime, safeReason);
        CommandReceipt duplicate = duplicateCommand(safeCommandId, signature);
        if (duplicate != null) {
            return duplicate;
        }
        if (!canAcceptCommand()) {
            return capacityRejected();
        }
        if (state != EnterpriseLabExperimentState.ARMED || candidateAllocationActive) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.ILLEGAL,
                    "only an armed experiment without candidate traffic can become "
                            + terminalState.name().toLowerCase()));
        }
        if (timePrecedesActivity(safeTime)) {
            return store(safeCommandId, signature, commandWithoutTransition(
                    CommandStatus.ILLEGAL, operation + " time cannot precede lifecycle activity"));
        }
        EnterpriseLabExperimentTransition transition = changeState(
                terminalState, safeCommandId, safeTime, safeReason);
        return store(safeCommandId, signature, commandWithTransition(
                CommandStatus.APPLIED, transition, safeReason));
    }

    private String armRejectionReason(
            EnterpriseLabExperimentConfiguration requested,
            Instant occurredAt) {
        if (occurredAt.isBefore(requested.createdAt()) || !occurredAt.isBefore(requested.expiresAt())) {
            return "configuration is not within its bounded creation and expiration window";
        }
        if (!requested.operatorAuthorized()) {
            return "explicit operator authorization is required";
        }
        if (requested.operatingMode() != AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT) {
            return "only bounded active-experiment mode can be armed";
        }
        Optional<EnterpriseLabScenarioMetadata> metadata = new EnterpriseLabScenarioCatalogService()
                .findScenarioMetadata(requested.scenarioId());
        if (metadata.isEmpty() || !metadata.orElseThrow().safeForInfluenceExperiment()) {
            return "scenario is not repository-approved for an influence experiment";
        }
        var guardrail = requested.candidateDecision().decision().guardrailDecision();
        if (!guardrail.influenceAllowed() || guardrail.action() == TrafficAllocationGuardrailAction.DENY) {
            return "adaptive guardrail did not authorize the candidate allocation";
        }
        return null;
    }

    private boolean candidateReceiptMatches(AllocationChangeReceipt receipt) {
        return (receipt.status() == ChangeStatus.APPLIED || receipt.status() == ChangeStatus.NO_CHANGE)
                && receipt.currentSnapshot().kind() == Kind.CANDIDATE
                && receipt.currentSnapshot().scenarioId().equals(configuration.scenarioId())
                && receipt.currentSnapshot().sourceDecisionId().equals(configuration.candidateDecisionId())
                && EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                        receipt.currentSnapshot().allocations(),
                        configuration.candidateDecision().decision().effectiveAllocations());
    }

    private boolean safeBaselineReceiptMatches(AllocationChangeReceipt receipt) {
        Kind kind = receipt.currentSnapshot().kind();
        return (receipt.status() == ChangeStatus.RESTORED || receipt.status() == ChangeStatus.NO_CHANGE)
                && (kind == Kind.BASELINE || kind == Kind.RESTORED_BASELINE)
                && receipt.currentSnapshot().scenarioId().equals(configuration.scenarioId())
                && EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                        receipt.currentSnapshot().allocations(), configuration.baselineSnapshot().allocations());
    }

    private EnterpriseLabExperimentTransition changeState(
            EnterpriseLabExperimentState targetState,
            String commandId,
            Instant occurredAt,
            String reason) {
        if (transitions.size() >= MAX_TRANSITIONS) {
            throw new IllegalStateException("bounded lifecycle transition capacity exhausted");
        }
        if (timePrecedesActivity(occurredAt)) {
            throw new IllegalStateException("lifecycle transition time cannot move backwards");
        }
        String previousFingerprint = transitions.isEmpty()
                ? EnterpriseLabExperimentTransition.GENESIS_FINGERPRINT
                : transitions.get(transitions.size() - 1).contentFingerprint();
        EnterpriseLabExperimentTransition transition = new EnterpriseLabExperimentTransition(
                EnterpriseLabExperimentTransition.SCHEMA_VERSION,
                transitions.size() + 1L,
                configuration.experimentId(),
                state,
                targetState,
                occurredAt,
                commandId,
                reason,
                requestCount,
                evidenceCount,
                completedHoldDownCycles,
                allocationRevision,
                previousFingerprint);
        transitions.add(transition);
        state = targetState;
        if (lastActivityAt == null || occurredAt.isAfter(lastActivityAt)) {
            lastActivityAt = occurredAt;
        }
        return transition;
    }

    private CommandReceipt duplicateCommand(String commandId, String signature) {
        CachedCommand cached = commandResults.get(commandId);
        if (cached == null) {
            return null;
        }
        if (!cached.signature().equals(signature)) {
            return commandWithoutTransition(
                    CommandStatus.CONFLICT, "commandId was already used for a different lifecycle request");
        }
        return new CommandReceipt(
                CommandStatus.IDEMPOTENT,
                cached.receipt().transition(),
                snapshot("idempotent lifecycle command replay"),
                "identical command replay caused no additional transition");
    }

    private ProgressReceipt duplicateProgress(
            Map<String, CachedProgress> results,
            String progressId,
            String signature) {
        CachedProgress cached = results.get(progressId);
        if (cached == null) {
            return null;
        }
        if (!cached.signature().equals(signature)) {
            return progressWithoutRecord(
                    ProgressStatus.CONFLICT,
                    progressId,
                    "progress identity was already used with different evidence");
        }
        return new ProgressReceipt(
                ProgressStatus.IDEMPOTENT,
                progressId,
                snapshot("idempotent progress replay"),
                cached.receipt().boundaryReached(),
                "identical progress replay caused no additional count");
    }

    private CommandReceipt store(String commandId, String signature, CommandReceipt receipt) {
        commandResults.put(commandId, new CachedCommand(signature, receipt));
        return receipt;
    }

    private boolean canAcceptCommand() {
        return commandResults.size() < MAX_COMMAND_RESULTS;
    }

    private boolean timePrecedesActivity(Instant occurredAt) {
        return lastActivityAt != null && occurredAt.isBefore(lastActivityAt);
    }

    private CommandReceipt capacityRejected() {
        return commandWithoutTransition(
                CommandStatus.REJECTED, "bounded lifecycle command-result capacity is exhausted");
    }

    private CommandReceipt commandWithTransition(
            CommandStatus status,
            EnterpriseLabExperimentTransition transition,
            String reason) {
        return new CommandReceipt(status, Optional.of(transition), snapshot(reason), reason);
    }

    private CommandReceipt commandWithoutTransition(CommandStatus status, String reason) {
        return new CommandReceipt(status, Optional.empty(), snapshot(reason), reason);
    }

    private ProgressReceipt progressWithoutRecord(
            ProgressStatus status,
            String progressId,
            String reason) {
        return new ProgressReceipt(status, progressId, snapshot(reason), false, reason);
    }

    private LifecycleSnapshot snapshot(String reason) {
        String experimentId = configuration == null ? "unconfigured" : configuration.experimentId();
        String lastFingerprint = transitions.isEmpty()
                ? EnterpriseLabExperimentTransition.GENESIS_FINGERPRINT
                : transitions.get(transitions.size() - 1).contentFingerprint();
        return new LifecycleSnapshot(
                SNAPSHOT_SCHEMA_VERSION,
                experimentId,
                state,
                requestCount,
                evidenceCount,
                completedHoldDownCycles,
                Optional.ofNullable(startedAt),
                Optional.ofNullable(lastActivityAt),
                candidateAllocationActive,
                allocationRevision,
                List.copyOf(transitions),
                lastFingerprint,
                state.terminal(),
                reason);
    }

    private String countersSignature() {
        return requestCount + ":" + evidenceCount + ":" + completedHoldDownCycles;
    }

    private static String signature(String operation, Instant occurredAt, String payload) {
        return operation + "|" + occurredAt + "|" + payload;
    }

    private static String requireCanonicalId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        if (!value.equals(value.trim()) || value.length() > MAX_ID_LENGTH
                || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException(fieldName + " must be a bounded canonical identifier");
        }
        return value;
    }

    private static String requireBoundedReason(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("reason cannot be null or blank");
        }
        String reason = value.trim();
        if (reason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("reason cannot exceed 256 characters");
        }
        return reason;
    }

    public enum CommandStatus {
        APPLIED,
        IDEMPOTENT,
        REJECTED,
        ILLEGAL,
        CONFLICT,
        NO_CHANGE
    }

    public enum ProgressStatus {
        RECORDED,
        IDEMPOTENT,
        DENIED,
        CONFLICT
    }

    public record CommandReceipt(
            CommandStatus status,
            Optional<EnterpriseLabExperimentTransition> transition,
            LifecycleSnapshot snapshot,
            String reason) {
        public CommandReceipt {
            status = Objects.requireNonNull(status, "status cannot be null");
            transition = Objects.requireNonNull(transition, "transition cannot be null");
            snapshot = Objects.requireNonNull(snapshot, "snapshot cannot be null");
            reason = requireBoundedReason(reason);
            if (status == CommandStatus.APPLIED && transition.isEmpty()) {
                throw new IllegalArgumentException("applied command requires a transition");
            }
        }
    }

    public record ProgressReceipt(
            ProgressStatus status,
            String progressId,
            LifecycleSnapshot snapshot,
            boolean boundaryReached,
            String reason) {
        public ProgressReceipt {
            status = Objects.requireNonNull(status, "status cannot be null");
            progressId = requireCanonicalId(progressId, "progressId");
            snapshot = Objects.requireNonNull(snapshot, "snapshot cannot be null");
            reason = requireBoundedReason(reason);
        }
    }

    public record LifecycleSnapshot(
            String schemaVersion,
            String experimentId,
            EnterpriseLabExperimentState state,
            int requestCount,
            int evidenceCount,
            int completedHoldDownCycles,
            Optional<Instant> startedAt,
            Optional<Instant> lastActivityAt,
            boolean candidateAllocationActive,
            long allocationRevision,
            List<EnterpriseLabExperimentTransition> transitions,
            String lastTransitionFingerprint,
            boolean terminal,
            String reason) {
        public LifecycleSnapshot {
            if (!SNAPSHOT_SCHEMA_VERSION.equals(schemaVersion)) {
                throw new IllegalArgumentException("unsupported lifecycle snapshot schemaVersion");
            }
            experimentId = requireCanonicalId(experimentId, "experimentId");
            state = Objects.requireNonNull(state, "state cannot be null");
            if (requestCount < 0 || evidenceCount < 0 || evidenceCount > requestCount
                    || completedHoldDownCycles < 0 || allocationRevision < 0) {
                throw new IllegalArgumentException("lifecycle snapshot counters are inconsistent");
            }
            startedAt = Objects.requireNonNull(startedAt, "startedAt cannot be null");
            lastActivityAt = Objects.requireNonNull(lastActivityAt, "lastActivityAt cannot be null");
            transitions = List.copyOf(Objects.requireNonNull(transitions, "transitions cannot be null"));
            if (transitions.size() > MAX_TRANSITIONS) {
                throw new IllegalArgumentException("transition history exceeds bounded capacity");
            }
            lastTransitionFingerprint = Objects.requireNonNull(
                    lastTransitionFingerprint, "lastTransitionFingerprint cannot be null");
            if (terminal != state.terminal()) {
                throw new IllegalArgumentException("terminal indicator must match lifecycle state");
            }
            if (terminal && candidateAllocationActive) {
                throw new IllegalArgumentException("terminal lifecycle cannot retain candidate allocation");
            }
            reason = requireBoundedReason(reason);
        }
    }

    private record CachedCommand(String signature, CommandReceipt receipt) {
    }

    private record CachedProgress(String signature, ProgressReceipt receipt) {
    }
}
