package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.core.ServerObservationWindowPolicy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentEvaluator.EvaluationReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentEvaluator.EvaluationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.CommandReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.CommandStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.LifecycleSnapshot;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.ProgressReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.ProgressStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorRecord.OperatorActionEvidence;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.AllocationChangeReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.ChangeStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.RouteExecution;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackObservationIngress.ReceiptStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.LongSupplier;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceMutationAuthority.MutationAuthorization;

/**
 * Auth-bound coordinator for one active, bounded Enterprise Lab loopback experiment.
 * It creates no targets, scheduler, executor, or background traffic source.
 */
public final class EnterpriseLabExperimentOperatorService implements AutoCloseable {
    public static final int DEFAULT_MAX_RETAINED_EXPERIMENTS = 128;
    public static final int MAX_API_REQUEST_COUNT = 64;
    public static final int MAX_REQUESTS_PER_BATCH = 32;
    public static final Duration MAX_API_DURATION = Duration.ofMinutes(5);
    public static final Duration MAX_REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_OPERATOR_COMMANDS = 128;
    private static final int RESERVED_TERMINATION_COMMANDS = 1;
    private static final int RESERVED_TERMINATION_ACTIONS = 2;
    private static final int MAX_ID_LENGTH = 128;
    private static final int MAX_REASON_LENGTH = 256;
    private static final Duration MAX_EXPIRATION_WINDOW = Duration.ofMinutes(15);

    private final EnterpriseLabExperimentTargetCatalog targetCatalog;
    private final EnterpriseLabScenarioCatalogService scenarioCatalog;
    private final EnterpriseLabAdaptiveDecisionService decisionService;
    private final Clock clock;
    private final LongSupplier monotonicNanos;
    private final int maxRetainedExperiments;
    private final EnterpriseLabExperimentRecoveryGate recoveryGate;
    private final Optional<EnterpriseLabExperimentDurableEvidenceRepository> durableEvidence;
    private final Optional<EnterpriseLabEvidenceOwnershipLease> ownershipLease;
    private final Optional<EnterpriseLabEvidenceOwnershipRenewer> ownershipRenewer;
    private final Map<String, Session> sessions = new LinkedHashMap<>();
    private boolean closed;

    public EnterpriseLabExperimentOperatorService(EnterpriseLabExperimentTargetCatalog targetCatalog) {
        this(targetCatalog, EnterpriseLabExperimentRecoveryGate.inMemoryOnly());
    }

    public EnterpriseLabExperimentOperatorService(
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabExperimentRecoveryGate recoveryGate) {
        this(
                targetCatalog,
                new EnterpriseLabScenarioCatalogService(),
                new EnterpriseLabAdaptiveDecisionService(),
                Clock.systemUTC(),
                System::nanoTime,
                DEFAULT_MAX_RETAINED_EXPERIMENTS,
                recoveryGate,
                Optional.empty(),
                Optional.empty());
    }

    public EnterpriseLabExperimentOperatorService(
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabExperimentRecoveryGate recoveryGate,
            EnterpriseLabExperimentDurableEvidenceRepository durableEvidence) {
        this(
                targetCatalog,
                new EnterpriseLabScenarioCatalogService(),
                new EnterpriseLabAdaptiveDecisionService(),
                Clock.systemUTC(),
                System::nanoTime,
                DEFAULT_MAX_RETAINED_EXPERIMENTS,
                recoveryGate,
                Optional.of(Objects.requireNonNull(durableEvidence, "durableEvidence cannot be null")),
                Optional.empty());
    }

    public EnterpriseLabExperimentOperatorService(
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabExperimentRecoveryGate recoveryGate,
            EnterpriseLabExperimentDurableEvidenceRepository durableEvidence,
            EnterpriseLabEvidenceOwnershipLease ownershipLease) {
        this(
                targetCatalog,
                new EnterpriseLabScenarioCatalogService(),
                new EnterpriseLabAdaptiveDecisionService(),
                Clock.systemUTC(),
                System::nanoTime,
                DEFAULT_MAX_RETAINED_EXPERIMENTS,
                recoveryGate,
                Optional.of(Objects.requireNonNull(durableEvidence, "durableEvidence cannot be null")),
                Optional.of(Objects.requireNonNull(ownershipLease, "ownershipLease cannot be null")));
    }

    EnterpriseLabExperimentOperatorService(
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabScenarioCatalogService scenarioCatalog,
            EnterpriseLabAdaptiveDecisionService decisionService,
            Clock clock,
            LongSupplier monotonicNanos,
            int maxRetainedExperiments) {
        this(targetCatalog, scenarioCatalog, decisionService, clock, monotonicNanos,
                maxRetainedExperiments, EnterpriseLabExperimentRecoveryGate.inMemoryOnly(),
                Optional.empty(), Optional.empty());
    }

    EnterpriseLabExperimentOperatorService(
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabScenarioCatalogService scenarioCatalog,
            EnterpriseLabAdaptiveDecisionService decisionService,
            Clock clock,
            LongSupplier monotonicNanos,
            int maxRetainedExperiments,
            EnterpriseLabExperimentRecoveryGate recoveryGate) {
        this(targetCatalog, scenarioCatalog, decisionService, clock, monotonicNanos,
                maxRetainedExperiments, recoveryGate, Optional.empty(), Optional.empty());
    }

    EnterpriseLabExperimentOperatorService(
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabScenarioCatalogService scenarioCatalog,
            EnterpriseLabAdaptiveDecisionService decisionService,
            Clock clock,
            LongSupplier monotonicNanos,
            int maxRetainedExperiments,
            EnterpriseLabExperimentRecoveryGate recoveryGate,
            Optional<EnterpriseLabExperimentDurableEvidenceRepository> durableEvidence) {
        this(targetCatalog, scenarioCatalog, decisionService, clock, monotonicNanos,
                maxRetainedExperiments, recoveryGate, durableEvidence, Optional.empty());
    }

    EnterpriseLabExperimentOperatorService(
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabScenarioCatalogService scenarioCatalog,
            EnterpriseLabAdaptiveDecisionService decisionService,
            Clock clock,
            LongSupplier monotonicNanos,
            int maxRetainedExperiments,
            EnterpriseLabExperimentRecoveryGate recoveryGate,
            Optional<EnterpriseLabExperimentDurableEvidenceRepository> durableEvidence,
            Optional<EnterpriseLabEvidenceOwnershipLease> ownershipLease) {
        this.targetCatalog = Objects.requireNonNull(targetCatalog, "targetCatalog cannot be null");
        this.scenarioCatalog = Objects.requireNonNull(scenarioCatalog, "scenarioCatalog cannot be null");
        this.decisionService = Objects.requireNonNull(decisionService, "decisionService cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.monotonicNanos = Objects.requireNonNull(monotonicNanos, "monotonicNanos cannot be null");
        this.recoveryGate = Objects.requireNonNull(recoveryGate, "recoveryGate cannot be null");
        this.durableEvidence = Objects.requireNonNull(
                durableEvidence, "durableEvidence cannot be null");
        this.ownershipLease = Objects.requireNonNull(
                ownershipLease, "ownershipLease cannot be null");
        if (ownershipLease.isPresent() && durableEvidence.isEmpty()) {
            throw new IllegalArgumentException(
                    "owned service shutdown requires durable evidence");
        }
        if (ownershipLease.isPresent() && !recoveryGate.admissionAllowed()) {
            throw new IllegalStateException(
                    "owned service renewal requires completed startup reconciliation");
        }
        if (maxRetainedExperiments < 1 || maxRetainedExperiments > DEFAULT_MAX_RETAINED_EXPERIMENTS) {
            throw new IllegalArgumentException("maxRetainedExperiments must be between 1 and 128");
        }
        this.maxRetainedExperiments = maxRetainedExperiments;
        this.ownershipRenewer = ownershipLease.map(lease ->
                new EnterpriseLabEvidenceOwnershipRenewer(
                        lease.ownershipGate(), recoveryGate, lease.renewalInterval()));
    }

    public synchronized OperatorReceipt arm(ArmRequest request, boolean activeExperimentExplicitlyEnabled) {
        ArmRequest safeRequest = Objects.requireNonNull(request, "request cannot be null");
        if (closed) {
            return denied("arm", safeRequest.operatorRequestId(), safeRequest.experimentId(),
                    "SERVICE_CLOSED", "experiment operator service is closed");
        }
        if (!recoveryGate.admissionAllowed()) {
            return denied("arm", safeRequest.operatorRequestId(), safeRequest.experimentId(),
                    "RECOVERY_NOT_READY",
                    "startup journal reconciliation has not reached a safe admission state");
        }
        Optional<MutationAuthorization> authorization = requireMutationOwnership();
        Session existing = sessions.get(safeRequest.experimentId());
        if (existing != null) {
            return replayOrConflict(existing, safeRequest.operatorRequestId(), safeRequest.signature(), "arm");
        }
        if (!activeExperimentExplicitlyEnabled) {
            return denied("arm", safeRequest.operatorRequestId(), safeRequest.experimentId(),
                    "EXPLICIT_ENABLEMENT_REQUIRED",
                    "active-experiment mode and its explicit enablement property must both be active");
        }
        Optional<Session> active = activeSession();
        if (active.isPresent()) {
            return denied(OperatorStatus.CONFLICT, "arm", safeRequest.operatorRequestId(),
                    safeRequest.experimentId(), "ACTIVE_EXPERIMENT_CONFLICT",
                    "another non-terminal Enterprise Lab experiment is already active", Optional.of(record(active.get())));
        }
        if (sessions.size() >= maxRetainedExperiments) {
            return denied(OperatorStatus.CAPACITY_REJECTED, "arm", safeRequest.operatorRequestId(),
                    safeRequest.experimentId(), "EXPERIMENT_CAPACITY_EXHAUSTED",
                    "bounded experiment history capacity is exhausted", Optional.empty());
        }
        if (scenarioCatalog.findScenarioMetadata(safeRequest.scenarioId()).isEmpty()) {
            return denied("arm", safeRequest.operatorRequestId(), safeRequest.experimentId(),
                    "UNKNOWN_SCENARIO", "scenario is not present in the repository-approved Enterprise Lab catalog");
        }
        List<EnterpriseLabLoopbackTarget> targets = targetCatalog.findTargets(safeRequest.scenarioId())
                .orElse(null);
        if (targets == null) {
            return denied("arm", safeRequest.operatorRequestId(), safeRequest.experimentId(),
                    "APPROVED_TARGETS_UNAVAILABLE",
                    "no repository-controlled loopback target binding is active for this scenario");
        }

        EnterpriseLabAdaptiveDecision decision = decisionService.decide(
                safeRequest.scenarioId(),
                AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT.wireValue(),
                true,
                false,
                false);
        Set<String> backendIds = new TreeSet<>();
        targets.forEach(target -> backendIds.add(target.backendId()));
        EnterpriseLabLoopbackObservationIngress ingress = new EnterpriseLabLoopbackObservationIngress(
                backendIds,
                ServerObservationWindowPolicy.localLabDefaults(),
                Math.min(EnterpriseLabLoopbackObservationIngress.DEFAULT_MAX_IN_FLIGHT_REQUESTS,
                        safeRequest.maximumRequestCount()),
                EnterpriseLabLoopbackObservationIngress.DEFAULT_MAX_MEASURED_LATENCY,
                clock,
                monotonicNanos);
        EnterpriseLabLoopbackAllocationRouter router = new EnterpriseLabLoopbackAllocationRouter(
                targets,
                ingress,
                decision.decision().guardrailDecision().baselineAllocations(),
                durableEvidence.map(repository ->
                        (EnterpriseLabEvidenceMutationAuthority)
                                repository::requireMutationAuthorization));
        Instant now = clock.instant();
        EnterpriseLabExperimentConfiguration configuration = new EnterpriseLabExperimentConfiguration(
                EnterpriseLabExperimentConfiguration.SCHEMA_VERSION,
                safeRequest.experimentId(),
                decision,
                router.baselineSnapshot(),
                safeRequest.maximumRequestCount(),
                safeRequest.maximumDuration(),
                safeRequest.minimumEvidenceCount(),
                safeRequest.holdDownCycles(),
                EnterpriseLabExperimentRollbackPolicy.localLabDefaults(),
                AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT,
                true,
                now,
                now.plus(safeRequest.expirationWindow()));
        EnterpriseLabExperimentLifecycle lifecycle = new EnterpriseLabExperimentLifecycle();
        EnterpriseLabExperimentState before = lifecycle.snapshot().state();
        requireSameMutationOwnership(authorization);
        CommandReceipt lifecycleReceipt = lifecycle.arm(
                safeRequest.operatorRequestId(), configuration, now);
        Session session = new Session(configuration, lifecycle, router, ingress);
        requireSameMutationOwnership(authorization);
        sessions.put(configuration.experimentId(), session);
        recordAction(session, safeRequest.operatorRequestId(), "arm", now, before,
                lifecycleReceipt.snapshot().state(), false, lifecycleReceipt.reason());
        OperatorStatus status = lifecycleReceipt.status() == CommandStatus.APPLIED
                ? OperatorStatus.APPLIED
                : OperatorStatus.REJECTED;
        String code = lifecycleReceipt.status() == CommandStatus.APPLIED
                ? "EXPERIMENT_ARMED"
                : "EXPERIMENT_ARM_REJECTED";
        OperatorReceipt receipt = receipt(
                status,
                "arm",
                safeRequest.operatorRequestId(),
                configuration.experimentId(),
                code,
                false,
                lifecycleReceipt.reason(),
                Optional.of(record(session)));
        cache(session, safeRequest.operatorRequestId(), safeRequest.signature(), receipt);
        return receipt;
    }

    public synchronized OperatorReceipt start(
            String experimentId,
            String operatorRequestId,
            boolean activeExperimentExplicitlyEnabled) {
        String safeExperimentId = requireCanonicalId(experimentId, "experimentId");
        String safeRequestId = requireCanonicalId(operatorRequestId, "operatorRequestId");
        if (!recoveryGate.admissionAllowed()) {
            return denied("start", safeRequestId, safeExperimentId,
                    "RECOVERY_NOT_READY",
                    "startup journal reconciliation has not reached a safe admission state");
        }
        Optional<MutationAuthorization> authorization = requireMutationOwnership();
        Session session = sessions.get(safeExperimentId);
        if (session == null) {
            return notFound("start", safeRequestId, safeExperimentId);
        }
        String signature = "start|" + safeExperimentId;
        OperatorReceipt cached = cachedOperator(session, safeRequestId, signature, "start");
        if (cached != null) {
            return cached;
        }
        if (!activeExperimentExplicitlyEnabled) {
            return cacheAndReturn(session, safeRequestId, signature,
                    deniedWithRecord(session, "start", safeRequestId, "EXPLICIT_ENABLEMENT_REQUIRED",
                            "active-experiment mode and explicit enablement are required at start"));
        }
        if (!canAcceptCommand(session)) {
            return capacity(session, "start", safeRequestId, signature);
        }
        if (hasPendingRequestBatch(session)) {
            return cacheAndReturn(session, safeRequestId, signature,
                    deniedWithRecord(session, OperatorStatus.CONFLICT, "start", safeRequestId,
                            "REQUEST_BATCH_IN_PROGRESS",
                            "start cannot interleave with an in-progress baseline request batch"));
        }

        Instant now = clock.instant();
        LifecycleSnapshot before = session.lifecycle.snapshot();
        if (before.state() != EnterpriseLabExperimentState.ARMED) {
            return cacheAndReturn(session, safeRequestId, signature,
                    deniedWithRecord(session, OperatorStatus.REJECTED, "start", safeRequestId,
                            "ILLEGAL_LIFECYCLE_TRANSITION", "only an armed experiment can start"));
        }
        session.observationBaseline = EnterpriseLabExperimentObservationBaseline.capture(
                session.configuration, session.ingress, now);
        AllocationChangeReceipt allocation;
        if (!now.isBefore(session.configuration.expiresAt())) {
            EnterpriseLabLoopbackAllocationSnapshot current = session.router.currentSnapshot();
            allocation = new AllocationChangeReceipt(
                    ChangeStatus.DENIED,
                    current,
                    current,
                    session.router.baselineSnapshot(),
                    false,
                    "no routing state altered",
                    "expired experiment cannot install a candidate allocation");
        } else {
            requireSameMutationOwnership(authorization);
            allocation = session.router.applyCandidate(
                    session.configuration.candidateDecision(), true);
        }
        requireSameMutationOwnership(authorization);
        CommandReceipt started = session.lifecycle.start(safeRequestId, allocation, now);
        boolean trafficAction = allocation.trafficActionPerformed();
        if (started.status() == CommandStatus.APPLIED
                && started.snapshot().state() == EnterpriseLabExperimentState.RUNNING) {
            session.evaluator = new EnterpriseLabExperimentEvaluator(
                    session.lifecycle,
                    session.router,
                    session.ingress,
                    session.observationBaseline);
        } else if (session.router.currentSnapshot().kind() == Kind.CANDIDATE) {
            requireSameMutationOwnership(authorization);
            AllocationChangeReceipt restored = session.router.restoreBaseline("failed start rollback");
            trafficAction = trafficAction || restored.trafficActionPerformed();
        }
        LifecycleSnapshot after = session.lifecycle.snapshot();
        recordAction(session, safeRequestId, "start", now, before.state(), after.state(),
                trafficAction, started.reason());
        OperatorStatus status = started.status() == CommandStatus.APPLIED
                ? OperatorStatus.APPLIED
                : OperatorStatus.REJECTED;
        String code;
        if (started.status() == CommandStatus.APPLIED) {
            code = "EXPERIMENT_STARTED";
        } else if (after.state() == EnterpriseLabExperimentState.CANCELLED) {
            code = "EXPERIMENT_EXPIRED";
        } else if (allocation.status() == ChangeStatus.DENIED) {
            code = "ALLOCATION_APPLICATION_DENIED";
        } else {
            code = "EXPERIMENT_START_REJECTED";
        }
        OperatorReceipt receipt = receipt(status, "start", safeRequestId, safeExperimentId, code,
                trafficAction, started.reason(), Optional.of(record(session)));
        return cacheAndReturn(session, safeRequestId, signature, receipt);
    }

    public RequestBatchReceipt executeRequests(
            String experimentId,
            RequestBatchRequest request,
            boolean activeExperimentExplicitlyEnabled) {
        String safeExperimentId = requireCanonicalId(experimentId, "experimentId");
        RequestBatchRequest safeRequest = Objects.requireNonNull(request, "request cannot be null");
        Session session;
        Optional<MutationAuthorization> authorization;
        String signature = safeRequest.signature(safeExperimentId);
        synchronized (this) {
            if (!recoveryGate.admissionAllowed()) {
                return recoveryBatchDenied(safeRequest, safeExperimentId);
            }
            authorization = requireMutationOwnership();
            session = sessions.get(safeExperimentId);
            if (session == null) {
                return batchNotFound(safeRequest.operatorRequestId(), safeExperimentId);
            }
            RequestBatchReceipt cached = cachedBatch(session, safeRequest, signature);
            if (cached != null) {
                return cached;
            }
            if (!activeExperimentExplicitlyEnabled) {
                rollbackForDisablement(session, safeRequest.operatorRequestId());
                return cacheAndReturnBatch(session, safeRequest.operatorRequestId(), signature,
                        batchDenied(session, safeRequest, "EXPLICIT_ENABLEMENT_REQUIRED",
                                "active-experiment mode and explicit enablement are required for loopback requests"));
            }
            if (!canAcceptCommand(session)) {
                return batchCapacity(session, safeRequest, signature);
            }
            if (hasPendingRequestBatch(session)) {
                return cacheAndReturnBatch(session, safeRequest.operatorRequestId(), signature,
                        batchDenied(session, safeRequest, "REQUEST_BATCH_IN_PROGRESS",
                                "another bounded request batch is already in progress"));
            }
            session.commandResults.put(safeRequest.operatorRequestId(), new CachedResponse(signature, null));
        }

        List<RequestOutcomeEvidence> outcomes = new ArrayList<>();
        EnterpriseLabExperimentState stateBefore;
        boolean trafficAction = false;
        int observationsRecorded = 0;
        int candidateRequestsRecorded = 0;
        String code;
        String reason;
        synchronized (session.requestLock) {
            LifecycleSnapshot before = session.lifecycle.snapshot();
            stateBefore = before.state();
            boolean baselinePhase = stateBefore == EnterpriseLabExperimentState.ARMED;
            boolean candidatePhase = stateBefore == EnterpriseLabExperimentState.RUNNING
                    || stateBefore == EnterpriseLabExperimentState.HOLDING;
            if (!baselinePhase && !candidatePhase) {
                synchronized (this) {
                    RequestBatchReceipt denied = batchDenied(session, safeRequest,
                            "ILLEGAL_LIFECYCLE_TRANSITION",
                            "requests are allowed only while armed, running, or holding");
                    session.commandResults.put(safeRequest.operatorRequestId(),
                            new CachedResponse(signature, denied));
                    return denied;
                }
            }
            int used = baselinePhase ? session.baselineRequestCount : before.requestCount();
            if (safeRequest.count() > session.configuration.maximumRequestCount() - used) {
                synchronized (this) {
                    RequestBatchReceipt denied = batchDenied(session, safeRequest,
                            "REQUEST_LIMIT_EXCEEDED",
                            "request batch would exceed the configured bounded request count");
                    session.commandResults.put(safeRequest.operatorRequestId(),
                            new CachedResponse(signature, denied));
                    return denied;
                }
            }

            for (int index = 0; index < safeRequest.count(); index++) {
                requireSameMutationOwnership(authorization);
                String requestId = routeRequestId(safeExperimentId, ++session.nextRequestSequence);
                RouteExecution route = session.router.route(
                        requestId,
                        session.selectionOrdinal++,
                        safeRequest.timeout());
                requireSameMutationOwnership(authorization);
                boolean observationRecorded = route.requestExecution().observationReceipt().status()
                        == ReceiptStatus.RECORDED;
                if (observationRecorded) {
                    observationsRecorded++;
                }
                ProgressReceipt progress = null;
                if (candidatePhase) {
                    progress = session.lifecycle.recordRequest(route, clock.instant());
                    if (progress.status() == ProgressStatus.RECORDED) {
                        candidateRequestsRecorded++;
                    }
                } else if (route.requestExecution().requestSent()) {
                    session.baselineRequestCount++;
                }
                trafficAction = trafficAction || route.trafficActionPerformed();
                outcomes.add(RequestOutcomeEvidence.from(route, observationRecorded,
                        progress == null ? "BASELINE_OBSERVATION" : progress.status().name()));
            }
            int sentCount = outcomes.stream().mapToInt(value -> value.requestSent() ? 1 : 0).sum();
            if (candidatePhase && candidateRequestsRecorded < sentCount) {
                code = "POST_TERMINAL_REQUEST_COMPLETION_RECORDED";
                reason = "in-flight loopback outcomes were retained after rollback, but lifecycle progress was denied";
            } else {
                code = baselinePhase ? "BASELINE_REQUESTS_RECORDED" : "CANDIDATE_REQUESTS_RECORDED";
                reason = baselinePhase
                        ? "bounded requests used the recorded safe baseline and captured loopback observations"
                        : "bounded requests used the approved candidate and were correlated with lifecycle evidence";
            }
        }

        synchronized (this) {
            requireSameMutationOwnership(authorization);
            Instant now = clock.instant();
            LifecycleSnapshot after = session.lifecycle.snapshot();
            recordAction(session, safeRequest.operatorRequestId(), "execute-requests", now,
                    stateBefore, after.state(), trafficAction, reason);
            RequestBatchReceipt receipt = new RequestBatchReceipt(
                    OperatorStatus.RECORDED,
                    safeRequest.operatorRequestId(),
                    safeExperimentId,
                    code,
                    safeRequest.count(),
                    outcomes.stream().mapToInt(value -> value.requestSent() ? 1 : 0).sum(),
                    observationsRecorded,
                    candidateRequestsRecorded,
                    trafficAction,
                    List.copyOf(outcomes),
                    reason,
                    Optional.of(record(session)));
            session.commandResults.put(safeRequest.operatorRequestId(), new CachedResponse(signature, receipt));
            return receipt;
        }
    }

    public synchronized OperatorReceipt evaluate(
            String experimentId,
            String operatorRequestId,
            boolean activeExperimentExplicitlyEnabled) {
        String safeExperimentId = requireCanonicalId(experimentId, "experimentId");
        String safeRequestId = requireCanonicalId(operatorRequestId, "operatorRequestId");
        Session session = sessions.get(safeExperimentId);
        if (session == null) {
            return notFound("evaluate", safeRequestId, safeExperimentId);
        }
        String signature = "evaluate|" + safeExperimentId;
        OperatorReceipt cached = cachedOperator(session, safeRequestId, signature, "evaluate");
        if (cached != null) {
            return cached;
        }
        if (!canAcceptCommand(session)) {
            return capacity(session, "evaluate", safeRequestId, signature);
        }
        if (hasPendingRequestBatch(session)) {
            return cacheAndReturn(session, safeRequestId, signature,
                    deniedWithRecord(session, OperatorStatus.CONFLICT, "evaluate", safeRequestId,
                            "REQUEST_BATCH_IN_PROGRESS",
                            "evaluation cannot interleave with an in-progress request batch"));
        }
        if (session.evaluator == null) {
            return cacheAndReturn(session, safeRequestId, signature,
                    deniedWithRecord(session, OperatorStatus.REJECTED, "evaluate", safeRequestId,
                            "ILLEGAL_LIFECYCLE_TRANSITION", "experiment must start before evaluation"));
        }

        Optional<MutationAuthorization> authorization = requireMutationOwnership();
        Instant now = clock.instant();
        LifecycleSnapshot before = session.lifecycle.snapshot();
        requireSameMutationOwnership(authorization);
        EvaluationReceipt evaluation = activeExperimentExplicitlyEnabled
                ? session.evaluator.evaluate(safeRequestId, now)
                : session.evaluator.cancel(safeRequestId,
                        "active-experiment enablement was removed", now);
        requireSameMutationOwnership(authorization);
        LifecycleSnapshot after = session.lifecycle.snapshot();
        boolean trafficAction = after.allocationRevision() != before.allocationRevision();
        recordAction(session, safeRequestId,
                activeExperimentExplicitlyEnabled ? "evaluate" : "disablement-rollback",
                now, before.state(), after.state(), trafficAction, evaluation.reason());
        OperatorStatus status = evaluation.status() == EvaluationStatus.RECORDED
                ? OperatorStatus.RECORDED
                : OperatorStatus.REJECTED;
        String code = evaluation.status() == EvaluationStatus.RECORDED
                ? activeExperimentExplicitlyEnabled ? "EVALUATION_RECORDED" : "DISABLEMENT_ROLLBACK_RECORDED"
                : "EVALUATION_REJECTED";
        OperatorReceipt receipt = receipt(status, "evaluate", safeRequestId, safeExperimentId, code,
                trafficAction, evaluation.reason(), Optional.of(record(session)));
        return cacheAndReturn(session, safeRequestId, signature, receipt);
    }

    public synchronized OperatorReceipt cancel(
            String experimentId,
            String operatorRequestId,
            String reason) {
        String safeExperimentId = requireCanonicalId(experimentId, "experimentId");
        String safeRequestId = requireCanonicalId(operatorRequestId, "operatorRequestId");
        String safeReason = requireReason(reason);
        Session session = sessions.get(safeExperimentId);
        if (session == null) {
            return notFound("cancel", safeRequestId, safeExperimentId);
        }
        String signature = "cancel|" + safeExperimentId + "|" + safeReason;
        OperatorReceipt cached = cachedOperator(session, safeRequestId, signature, "cancel");
        if (cached != null) {
            return cached;
        }
        if (!canAcceptCommand(session)) {
            OperatorReceipt receipt = cancelInternal(session, safeRequestId, safeReason, "cancel");
            if (session.commandResults.size() < MAX_OPERATOR_COMMANDS) {
                cache(session, safeRequestId, signature, receipt);
            }
            return receipt;
        }
        OperatorReceipt receipt = cancelInternal(session, safeRequestId, safeReason, "cancel");
        return cacheAndReturn(session, safeRequestId, signature, receipt);
    }

    public synchronized Optional<EnterpriseLabExperimentOperatorRecord> findRecord(String experimentId) {
        Session session = sessions.get(requireCanonicalId(experimentId, "experimentId"));
        return session == null ? Optional.empty() : Optional.of(record(session));
    }

    public synchronized Optional<EnterpriseLabExperimentOperatorRecord> findFinalRecord(String experimentId) {
        return findRecord(experimentId).filter(value -> value.lifecycle().terminal());
    }

    public synchronized Optional<EnterpriseLabExperimentOperatorRecord> currentRecord() {
        return activeSession().map(this::record);
    }

    public synchronized List<EnterpriseLabExperimentOperatorRecord> records() {
        return sessions.values().stream().map(this::record).toList();
    }

    public int maxRetainedExperiments() {
        return maxRetainedExperiments;
    }

    public List<String> boundScenarioIds() {
        return targetCatalog.boundScenarioIds();
    }

    public EnterpriseLabExperimentRecoveryGate.AdmissionStatus recoveryStatus() {
        return recoveryGate.admissionStatus();
    }

    public Optional<EnterpriseLabExperimentDurableEvidenceRepository> durableEvidence() {
        return durableEvidence;
    }

    /** Returns a bounded snapshot without reading caller-selected storage or mutating ownership. */
    public synchronized Optional<EnterpriseLabEvidenceOwnershipStatus> ownershipStatus() {
        return ownershipLease.map(lease -> EnterpriseLabEvidenceOwnershipStatus.from(
                lease,
                ownershipRenewer.flatMap(EnterpriseLabEvidenceOwnershipRenewer::lastResult),
                recoveryGate.admissionStatus(),
                Optional.empty()));
    }

    /** Explicitly verifies the live OS lock and durable record and closes admission on uncertainty. */
    public synchronized Optional<EnterpriseLabEvidenceOwnershipStatus> verifyOwnership() {
        return ownershipLease.map(lease -> {
            var verification = lease.ownershipGate().verifyCurrentOwnership();
            if (verification.status()
                    != EnterpriseLabEvidenceOwnership.OperationStatus.SUCCEEDED) {
                recoveryGate.fail("OWNERSHIP_VERIFICATION_FAILED");
            }
            return EnterpriseLabEvidenceOwnershipStatus.from(
                    lease,
                    ownershipRenewer.flatMap(EnterpriseLabEvidenceOwnershipRenewer::lastResult),
                    recoveryGate.admissionStatus(),
                    Optional.of(verification));
        });
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            activeSession().ifPresent(session -> {
                String requestId = "service-shutdown-" + (session.actions.size() + 1L);
                try {
                    cancelInternal(session, requestId,
                            "application shutdown requested safe experiment termination", "shutdown");
                } catch (RuntimeException exception) {
                    requireMutationOwnership();
                    AllocationChangeReceipt restored = session.router.restoreBaseline(
                            "shutdown fail-closed restoration");
                    LifecycleSnapshot before = session.lifecycle.snapshot();
                    if (before.candidateAllocationActive()) {
                        session.lifecycle.beginRollback(
                                requestId + "-begin", "shutdown fail-closed restoration", clock.instant());
                        session.lifecycle.confirmRollback(requestId + "-confirm", restored, clock.instant());
                    } else if (before.state() == EnterpriseLabExperimentState.ARMED) {
                        session.lifecycle.cancel(
                                requestId + "-cancel", "application shutdown before start", clock.instant());
                    }
                    throw exception;
                }
            });
        } finally {
            try {
                durableEvidence.ifPresent(EnterpriseLabExperimentDurableEvidenceRepository::close);
            } finally {
                try {
                    ownershipRenewer.ifPresent(EnterpriseLabEvidenceOwnershipRenewer::close);
                } finally {
                    ownershipLease.ifPresent(EnterpriseLabEvidenceOwnershipLease::close);
                }
            }
        }
    }

    private OperatorReceipt cancelInternal(
            Session session,
            String requestId,
            String reason,
            String operation) {
        Optional<MutationAuthorization> authorization = requireMutationOwnership();
        Instant now = clock.instant();
        LifecycleSnapshot before = session.lifecycle.snapshot();
        boolean trafficAction = false;
        String receiptReason;
        OperatorStatus status;
        String code;
        requireSameMutationOwnership(authorization);
        if (session.evaluator != null) {
            EvaluationReceipt evaluation = session.evaluator.cancel(requestId, reason, now);
            LifecycleSnapshot after = session.lifecycle.snapshot();
            trafficAction = after.allocationRevision() != before.allocationRevision();
            receiptReason = evaluation.reason();
            status = evaluation.status() == EvaluationStatus.RECORDED
                    ? OperatorStatus.RECORDED : OperatorStatus.REJECTED;
            code = evaluation.status() == EvaluationStatus.RECORDED
                    ? "CANCELLATION_RECORDED" : "CANCELLATION_REJECTED";
        } else {
            CommandReceipt cancellation = session.lifecycle.cancel(requestId, reason, now);
            receiptReason = cancellation.reason();
            status = cancellation.status() == CommandStatus.APPLIED
                    ? OperatorStatus.APPLIED : OperatorStatus.REJECTED;
            code = cancellation.status() == CommandStatus.APPLIED
                    ? "ARMED_EXPERIMENT_CANCELLED" : "CANCELLATION_REJECTED";
        }
        requireSameMutationOwnership(authorization);
        LifecycleSnapshot after = session.lifecycle.snapshot();
        recordAction(session, requestId, operation, now, before.state(), after.state(),
                trafficAction, receiptReason);
        return receipt(status, operation, requestId, session.configuration.experimentId(), code,
                trafficAction, receiptReason, Optional.of(record(session)));
    }

    private void rollbackForDisablement(Session session, String operatorRequestId) {
        LifecycleSnapshot snapshot = session.lifecycle.snapshot();
        if (snapshot.candidateAllocationActive() && session.evaluator != null && !snapshot.terminal()) {
            cancelInternal(session, internalRequestId("disablement", operatorRequestId),
                    "active-experiment enablement was removed", "disablement-rollback");
        }
    }

    private void recordAction(
            Session session,
            String requestId,
            String operation,
            Instant occurredAt,
            EnterpriseLabExperimentState before,
            EnterpriseLabExperimentState after,
            boolean trafficActionPerformed,
            String reason) {
        Optional<MutationAuthorization> authorization = requireMutationOwnership();
        if (session.actions.size() >= EnterpriseLabExperimentOperatorRecord.MAX_OPERATOR_ACTIONS) {
            throw new IllegalStateException("bounded operator action history capacity is exhausted");
        }
        String previous = session.actions.isEmpty()
                ? OperatorActionEvidence.GENESIS_FINGERPRINT
                : session.actions.get(session.actions.size() - 1).contentFingerprint();
        LifecycleSnapshot snapshot = session.lifecycle.snapshot();
        requireSameMutationOwnership(authorization);
        session.actions.add(OperatorActionEvidence.create(
                session.actions.size() + 1L,
                session.configuration.experimentId(),
                requestId,
                operation,
                occurredAt,
                before,
                after,
                snapshot.requestCount(),
                snapshot.evidenceCount(),
                trafficActionPerformed,
                reason,
                previous));
        durableEvidence.ifPresent(repository -> {
            try {
                repository.record(
                        session.configuration,
                        snapshot,
                        session.router.currentSnapshot(),
                        requestId,
                        operation,
                        occurredAt,
                        before,
                        after,
                        trafficActionPerformed,
                        reason);
            } catch (RuntimeException exception) {
                failClosedAfterDurableAppend(session, requestId, exception);
            }
        });
    }

    private void failClosedAfterDurableAppend(
            Session session,
            String requestId,
            RuntimeException appendFailure) {
        recoveryGate.fail("DURABLE_APPEND_FAILED");
        LifecycleSnapshot snapshot = session.lifecycle.snapshot();
        if (snapshot.candidateAllocationActive()) {
            try {
                Optional<MutationAuthorization> authorization = requireMutationOwnership();
                requireSameMutationOwnership(authorization);
                AllocationChangeReceipt restored = session.router.restoreBaseline(
                        "durable journal append failed closed");
                String internalId = internalRequestId("durable-append-failure", requestId);
                if (snapshot.state() == EnterpriseLabExperimentState.RUNNING
                        || snapshot.state() == EnterpriseLabExperimentState.HOLDING) {
                    session.lifecycle.beginRollback(
                            internalId + "-begin",
                            "durable journal append failed closed",
                            clock.instant());
                    session.lifecycle.confirmRollback(
                            internalId + "-confirm",
                            restored,
                            clock.instant());
                }
            } catch (EnterpriseLabEvidenceOwnershipException ownershipFailure) {
                appendFailure.addSuppressed(ownershipFailure);
            }
        }
        throw new IllegalStateException(
                "durable journal append failed; mutation admission closed fail safe",
                appendFailure);
    }

    private Optional<MutationAuthorization> requireMutationOwnership() {
        if (durableEvidence.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(durableEvidence.orElseThrow().requireMutationAuthorization());
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            recoveryGate.fail("OWNERSHIP_UNCERTAIN");
            throw exception;
        }
    }

    private void requireSameMutationOwnership(
            Optional<MutationAuthorization> expected) {
        Optional<MutationAuthorization> safeExpected = Objects.requireNonNull(
                expected, "expected ownership cannot be null");
        if (safeExpected.isEmpty()) {
            return;
        }
        try {
            durableEvidence.orElseThrow().requireSameMutationAuthorization(
                    safeExpected.orElseThrow());
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            recoveryGate.fail("OWNERSHIP_UNCERTAIN");
            throw exception;
        }
    }

    private EnterpriseLabExperimentOperatorRecord record(Session session) {
        List<EnterpriseLabExperimentEvaluation> evaluations = session.evaluator == null
                ? List.of()
                : session.evaluator.evaluationHistory();
        return EnterpriseLabExperimentOperatorRecord.create(
                session.configuration,
                session.lifecycle.snapshot(),
                Optional.ofNullable(session.observationBaseline),
                session.router.currentSnapshot(),
                evaluations,
                session.actions);
    }

    private Optional<Session> activeSession() {
        return sessions.values().stream()
                .filter(session -> !session.lifecycle.snapshot().terminal())
                .findFirst();
    }

    private boolean canAcceptCommand(Session session) {
        return session.commandResults.size() < MAX_OPERATOR_COMMANDS - RESERVED_TERMINATION_COMMANDS
                && session.actions.size()
                < EnterpriseLabExperimentOperatorRecord.MAX_OPERATOR_ACTIONS - RESERVED_TERMINATION_ACTIONS;
    }

    private static boolean hasPendingRequestBatch(Session session) {
        return session.commandResults.values().stream().anyMatch(value -> value.response() == null);
    }

    private void cache(Session session, String requestId, String signature, Object response) {
        session.commandResults.put(requestId, new CachedResponse(signature, response));
    }

    private OperatorReceipt cacheAndReturn(
            Session session,
            String requestId,
            String signature,
            OperatorReceipt receipt) {
        cache(session, requestId, signature, receipt);
        return receipt;
    }

    private RequestBatchReceipt cacheAndReturnBatch(
            Session session,
            String requestId,
            String signature,
            RequestBatchReceipt receipt) {
        cache(session, requestId, signature, receipt);
        return receipt;
    }

    private OperatorReceipt replayOrConflict(
            Session session,
            String requestId,
            String signature,
            String operation) {
        OperatorReceipt cached = cachedOperator(session, requestId, signature, operation);
        if (cached != null) {
            return cached;
        }
        return denied(OperatorStatus.CONFLICT, operation, requestId,
                session.configuration.experimentId(), "EXPERIMENT_ID_CONFLICT",
                "experimentId already identifies a different retained experiment", Optional.of(record(session)));
    }

    private OperatorReceipt cachedOperator(
            Session session,
            String requestId,
            String signature,
            String operation) {
        CachedResponse cached = session.commandResults.get(requestId);
        if (cached == null) {
            return null;
        }
        if (!cached.signature().equals(signature) || !(cached.response() instanceof OperatorReceipt original)) {
            return denied(OperatorStatus.CONFLICT, operation, requestId,
                    session.configuration.experimentId(), "OPERATOR_REQUEST_ID_CONFLICT",
                    "operatorRequestId was already used for a different command", Optional.of(record(session)));
        }
        return receipt(OperatorStatus.IDEMPOTENT, operation, requestId,
                session.configuration.experimentId(), "IDEMPOTENT_REPLAY", false,
                "identical operator command replay caused no additional action", Optional.of(record(session)));
    }

    private RequestBatchReceipt cachedBatch(
            Session session,
            RequestBatchRequest request,
            String signature) {
        CachedResponse cached = session.commandResults.get(request.operatorRequestId());
        if (cached == null) {
            return null;
        }
        if (!cached.signature().equals(signature)) {
            return batchDenied(session, request,
                    "OPERATOR_REQUEST_ID_CONFLICT",
                    "operatorRequestId was already used for a different command");
        }
        if (cached.response() == null) {
            return batchDenied(session, request,
                    "OPERATOR_REQUEST_IN_PROGRESS",
                    "identical request batch is already in progress");
        }
        if (!(cached.response() instanceof RequestBatchReceipt original)) {
            return batchDenied(session, request,
                    "OPERATOR_REQUEST_ID_CONFLICT",
                    "operatorRequestId was already used for a different command");
        }
        return new RequestBatchReceipt(
                OperatorStatus.IDEMPOTENT,
                request.operatorRequestId(),
                session.configuration.experimentId(),
                "IDEMPOTENT_REPLAY",
                original.requestedCount(),
                original.sentCount(),
                original.observationsRecorded(),
                original.candidateRequestsRecorded(),
                false,
                original.outcomes(),
                "identical request batch replay caused no additional loopback requests",
                Optional.of(record(session)));
    }

    private OperatorReceipt capacity(
            Session session,
            String operation,
            String requestId,
            String signature) {
        Optional<OperatorReceipt> termination = terminateForCapacity(session, operation, requestId);
        boolean trafficAction = termination.map(OperatorReceipt::trafficActionPerformed).orElse(false);
        String code = termination.isPresent()
                ? "OPERATOR_CAPACITY_SAFE_TERMINATION"
                : "OPERATOR_COMMAND_CAPACITY_EXHAUSTED";
        String reason = termination.isPresent()
                ? "bounded operator capacity was reached and the non-terminal experiment was safely terminated"
                : "bounded operator command or action capacity is exhausted";
        OperatorReceipt receipt = receipt(OperatorStatus.CAPACITY_REJECTED, operation, requestId,
                session.configuration.experimentId(), code, trafficAction, reason, Optional.of(record(session)));
        if (session.commandResults.size() < MAX_OPERATOR_COMMANDS) {
            cache(session, requestId, signature, receipt);
        }
        return receipt;
    }

    private RequestBatchReceipt batchCapacity(
            Session session,
            RequestBatchRequest request,
            String signature) {
        Optional<OperatorReceipt> termination = terminateForCapacity(
                session, "execute-requests", request.operatorRequestId());
        boolean trafficAction = termination.map(OperatorReceipt::trafficActionPerformed).orElse(false);
        String code = termination.isPresent()
                ? "OPERATOR_CAPACITY_SAFE_TERMINATION"
                : "OPERATOR_COMMAND_CAPACITY_EXHAUSTED";
        String reason = termination.isPresent()
                ? "bounded operator capacity was reached and the non-terminal experiment was safely terminated"
                : "bounded operator command or action capacity is exhausted";
        RequestBatchReceipt receipt = new RequestBatchReceipt(
                OperatorStatus.CAPACITY_REJECTED,
                request.operatorRequestId(),
                session.configuration.experimentId(),
                code,
                request.count(),
                0,
                0,
                0,
                trafficAction,
                List.of(),
                reason,
                Optional.of(record(session)));
        if (session.commandResults.size() < MAX_OPERATOR_COMMANDS) {
            cache(session, request.operatorRequestId(), signature, receipt);
        }
        return receipt;
    }

    private Optional<OperatorReceipt> terminateForCapacity(
            Session session,
            String operation,
            String requestId) {
        if (session.lifecycle.snapshot().terminal()) {
            return Optional.empty();
        }
        return Optional.of(cancelInternal(
                session,
                internalRequestId("capacity-" + operation, requestId),
                "bounded operator capacity required safe experiment termination",
                "capacity-termination"));
    }

    private OperatorReceipt deniedWithRecord(
            Session session,
            String operation,
            String requestId,
            String code,
            String reason) {
        return deniedWithRecord(session, OperatorStatus.DENIED, operation, requestId, code, reason);
    }

    private OperatorReceipt deniedWithRecord(
            Session session,
            OperatorStatus status,
            String operation,
            String requestId,
            String code,
            String reason) {
        return denied(status, operation, requestId, session.configuration.experimentId(), code,
                reason, Optional.of(record(session)));
    }

    private OperatorReceipt denied(
            String operation,
            String requestId,
            String experimentId,
            String code,
            String reason) {
        return denied(OperatorStatus.DENIED, operation, requestId, experimentId, code, reason, Optional.empty());
    }

    private OperatorReceipt denied(
            OperatorStatus status,
            String operation,
            String requestId,
            String experimentId,
            String code,
            String reason,
            Optional<EnterpriseLabExperimentOperatorRecord> record) {
        return receipt(status, operation, requestId, experimentId, code, false, reason, record);
    }

    private OperatorReceipt notFound(String operation, String requestId, String experimentId) {
        return receipt(OperatorStatus.NOT_FOUND, operation, requestId, experimentId,
                "UNKNOWN_EXPERIMENT", false, "experimentId is not present in the bounded operator store",
                Optional.empty());
    }

    private RequestBatchReceipt batchNotFound(String requestId, String experimentId) {
        return new RequestBatchReceipt(
                OperatorStatus.NOT_FOUND,
                requestId,
                experimentId,
                "UNKNOWN_EXPERIMENT",
                0,
                0,
                0,
                0,
                false,
                List.of(),
                "experimentId is not present in the bounded operator store",
                Optional.empty());
    }

    private RequestBatchReceipt recoveryBatchDenied(
            RequestBatchRequest request,
            String experimentId) {
        return new RequestBatchReceipt(
                OperatorStatus.DENIED,
                request.operatorRequestId(),
                experimentId,
                "RECOVERY_NOT_READY",
                request.count(),
                0,
                0,
                0,
                false,
                List.of(),
                "startup journal reconciliation has not reached a safe admission state",
                Optional.empty());
    }

    private RequestBatchReceipt batchDenied(
            Session session,
            RequestBatchRequest request,
            String code,
            String reason) {
        OperatorStatus status = code.contains("CONFLICT") || code.contains("IN_PROGRESS")
                ? OperatorStatus.CONFLICT : OperatorStatus.DENIED;
        return new RequestBatchReceipt(
                status,
                request.operatorRequestId(),
                session.configuration.experimentId(),
                code,
                request.count(),
                0,
                0,
                0,
                false,
                List.of(),
                reason,
                Optional.of(record(session)));
    }

    private static OperatorReceipt receipt(
            OperatorStatus status,
            String operation,
            String requestId,
            String experimentId,
            String code,
            boolean trafficActionPerformed,
            String reason,
            Optional<EnterpriseLabExperimentOperatorRecord> record) {
        return new OperatorReceipt(status, operation, requestId, experimentId, code,
                trafficActionPerformed, reason, record);
    }

    public enum OperatorStatus {
        APPLIED,
        RECORDED,
        IDEMPOTENT,
        DENIED,
        REJECTED,
        CONFLICT,
        NOT_FOUND,
        CAPACITY_REJECTED
    }

    public record ArmRequest(
            String operatorRequestId,
            String experimentId,
            String scenarioId,
            int maximumRequestCount,
            Duration maximumDuration,
            int minimumEvidenceCount,
            int holdDownCycles,
            Duration expirationWindow) {

        public ArmRequest {
            operatorRequestId = requireCanonicalId(operatorRequestId, "operatorRequestId");
            experimentId = requireCanonicalId(experimentId, "experimentId");
            scenarioId = requireCanonicalId(scenarioId, "scenarioId");
            if (maximumRequestCount < 1 || maximumRequestCount > MAX_API_REQUEST_COUNT) {
                throw new IllegalArgumentException("maximumRequestCount must be between 1 and 64 for the operator API");
            }
            maximumDuration = Objects.requireNonNull(maximumDuration, "maximumDuration cannot be null");
            if (maximumDuration.isZero() || maximumDuration.isNegative()
                    || maximumDuration.compareTo(MAX_API_DURATION) > 0) {
                throw new IllegalArgumentException("maximumDuration must be positive and no greater than five minutes");
            }
            if (minimumEvidenceCount < 1 || minimumEvidenceCount > maximumRequestCount) {
                throw new IllegalArgumentException(
                        "minimumEvidenceCount must be between 1 and maximumRequestCount");
            }
            if (holdDownCycles < 1
                    || holdDownCycles > EnterpriseLabExperimentConfiguration.HARD_MAX_HOLD_DOWN_CYCLES) {
                throw new IllegalArgumentException("holdDownCycles must be between 1 and 1000");
            }
            expirationWindow = Objects.requireNonNull(expirationWindow, "expirationWindow cannot be null");
            if (expirationWindow.compareTo(maximumDuration) < 0
                    || expirationWindow.compareTo(MAX_EXPIRATION_WINDOW) > 0) {
                throw new IllegalArgumentException(
                        "expirationWindow must cover maximumDuration and be no greater than fifteen minutes");
            }
        }

        private String signature() {
            return "arm|" + experimentId + "|" + scenarioId + "|" + maximumRequestCount + "|"
                    + maximumDuration + "|" + minimumEvidenceCount + "|" + holdDownCycles + "|" + expirationWindow;
        }
    }

    public record RequestBatchRequest(String operatorRequestId, int count, Duration timeout) {
        public RequestBatchRequest {
            operatorRequestId = requireCanonicalId(operatorRequestId, "operatorRequestId");
            if (count < 1 || count > MAX_REQUESTS_PER_BATCH) {
                throw new IllegalArgumentException("request batch count must be between 1 and 32");
            }
            timeout = Objects.requireNonNull(timeout, "timeout cannot be null");
            if (timeout.isZero() || timeout.isNegative() || timeout.compareTo(MAX_REQUEST_TIMEOUT) > 0) {
                throw new IllegalArgumentException("request timeout must be positive and no greater than five seconds");
            }
        }

        private String signature(String experimentId) {
            return "requests|" + experimentId + "|" + count + "|" + timeout;
        }
    }

    public record OperatorReceipt(
            OperatorStatus status,
            String operation,
            String operatorRequestId,
            String experimentId,
            String reasonCode,
            boolean trafficActionPerformed,
            String reason,
            Optional<EnterpriseLabExperimentOperatorRecord> experimentRecord) {
        public OperatorReceipt {
            status = Objects.requireNonNull(status, "status cannot be null");
            operation = requireText(operation, "operation");
            operatorRequestId = requireCanonicalId(operatorRequestId, "operatorRequestId");
            experimentId = requireCanonicalId(experimentId, "experimentId");
            reasonCode = requireCanonicalId(reasonCode, "reasonCode");
            reason = requireReason(reason);
            experimentRecord = Objects.requireNonNull(experimentRecord, "experimentRecord cannot be null");
        }
    }

    public record RequestBatchReceipt(
            OperatorStatus status,
            String operatorRequestId,
            String experimentId,
            String reasonCode,
            int requestedCount,
            int sentCount,
            int observationsRecorded,
            int candidateRequestsRecorded,
            boolean trafficActionPerformed,
            List<RequestOutcomeEvidence> outcomes,
            String reason,
            Optional<EnterpriseLabExperimentOperatorRecord> experimentRecord) {
        public RequestBatchReceipt {
            status = Objects.requireNonNull(status, "status cannot be null");
            operatorRequestId = requireCanonicalId(operatorRequestId, "operatorRequestId");
            experimentId = requireCanonicalId(experimentId, "experimentId");
            reasonCode = requireCanonicalId(reasonCode, "reasonCode");
            if (requestedCount < 0 || sentCount < 0 || sentCount > requestedCount
                    || observationsRecorded < 0 || observationsRecorded > sentCount
                    || candidateRequestsRecorded < 0 || candidateRequestsRecorded > sentCount) {
                throw new IllegalArgumentException("request batch counters are inconsistent");
            }
            outcomes = List.copyOf(Objects.requireNonNull(outcomes, "outcomes cannot be null"));
            if (!outcomes.isEmpty() && outcomes.size() != requestedCount) {
                throw new IllegalArgumentException("request outcomes must be empty or match requestedCount");
            }
            reason = requireReason(reason);
            experimentRecord = Objects.requireNonNull(experimentRecord, "experimentRecord cannot be null");
        }
    }

    public record RequestOutcomeEvidence(
            String requestId,
            String backendId,
            boolean requestSent,
            String outcome,
            OptionalInt responseStatusCode,
            boolean observationRecorded,
            boolean candidateAllocationUsed,
            String lifecycleProgress,
            String targetScope,
            String reason) {
        public RequestOutcomeEvidence {
            requestId = requireText(requestId, "requestId");
            backendId = requireText(backendId, "backendId");
            outcome = requireText(outcome, "outcome");
            responseStatusCode = Objects.requireNonNull(responseStatusCode, "responseStatusCode cannot be null");
            lifecycleProgress = requireText(lifecycleProgress, "lifecycleProgress");
            targetScope = requireText(targetScope, "targetScope");
            reason = requireReason(reason);
        }

        private static RequestOutcomeEvidence from(
                RouteExecution route,
                boolean observationRecorded,
                String lifecycleProgress) {
            return new RequestOutcomeEvidence(
                    route.requestExecution().requestId(),
                    route.selectedBackendId(),
                    route.requestExecution().requestSent(),
                    route.requestExecution().outcome().map(Enum::name).orElse("NOT_SENT"),
                    route.requestExecution().responseStatusCode(),
                    observationRecorded,
                    route.candidateAllocationUsed(),
                    lifecycleProgress,
                    route.requestExecution().targetScope(),
                    route.requestExecution().reason());
        }
    }

    private static final class Session {
        private final EnterpriseLabExperimentConfiguration configuration;
        private final EnterpriseLabExperimentLifecycle lifecycle;
        private final EnterpriseLabLoopbackAllocationRouter router;
        private final EnterpriseLabLoopbackObservationIngress ingress;
        private final List<OperatorActionEvidence> actions = new ArrayList<>();
        private final Map<String, CachedResponse> commandResults = new LinkedHashMap<>();
        private final Object requestLock = new Object();
        private EnterpriseLabExperimentObservationBaseline observationBaseline;
        private EnterpriseLabExperimentEvaluator evaluator;
        private long selectionOrdinal;
        private long nextRequestSequence;
        private int baselineRequestCount;

        private Session(
                EnterpriseLabExperimentConfiguration configuration,
                EnterpriseLabExperimentLifecycle lifecycle,
                EnterpriseLabLoopbackAllocationRouter router,
                EnterpriseLabLoopbackObservationIngress ingress) {
            this.configuration = configuration;
            this.lifecycle = lifecycle;
            this.router = router;
            this.ingress = ingress;
        }
    }

    private record CachedResponse(String signature, Object response) {
    }

    private static String requireCanonicalId(String value, String fieldName) {
        String safe = requireText(value, fieldName);
        if (!safe.equals(value) || safe.length() > MAX_ID_LENGTH || !safe.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException(fieldName + " must be a bounded canonical identifier");
        }
        return safe;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private static String requireReason(String value) {
        String reason = requireText(value, "reason").replace('\r', ' ').replace('\n', ' ');
        if (reason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("reason cannot exceed 256 characters");
        }
        return reason;
    }

    private static String routeRequestId(String experimentId, long sequence) {
        return "route:" + Integer.toUnsignedString(experimentId.hashCode()) + ":" + sequence;
    }

    private static String internalRequestId(String operation, String correlationId) {
        return operation + ":" + Integer.toUnsignedString(correlationId.hashCode());
    }
}
