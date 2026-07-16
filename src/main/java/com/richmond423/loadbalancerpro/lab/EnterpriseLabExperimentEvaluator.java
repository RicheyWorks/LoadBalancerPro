package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.core.ServerDegradationState;
import com.richmond423.loadbalancerpro.core.ServerObservation;
import com.richmond423.loadbalancerpro.core.ServerObservationOutcome;
import com.richmond423.loadbalancerpro.core.ServerObservationSource;
import com.richmond423.loadbalancerpro.core.ServerObservationWindow;
import com.richmond423.loadbalancerpro.core.ServerObservationWindowPolicy;
import com.richmond423.loadbalancerpro.core.ServerRollingSignalState;
import com.richmond423.loadbalancerpro.core.ServerSignalEvidence;
import com.richmond423.loadbalancerpro.core.TrafficAllocationGuardrailAction;
import com.richmond423.loadbalancerpro.core.TrafficAllocationGuardrailDecision;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentEvaluation.BackendEvidence;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentEvaluation.Disposition;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentEvaluation.Trigger;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.CommandReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.CommandStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.LifecycleSnapshot;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.ProgressReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.ProgressStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.AllocationChangeReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.ChangeStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Deterministic loopback-only hold-down, completion, cancellation, and rollback evaluator.
 */
public final class EnterpriseLabExperimentEvaluator {
    public static final int MAX_RETAINED_EVALUATIONS = 1_024;
    private static final int MAX_ID_LENGTH = 128;
    private static final int MAX_REASON_LENGTH = 256;

    private final EnterpriseLabExperimentLifecycle lifecycle;
    private final EnterpriseLabLoopbackAllocationRouter router;
    private final EnterpriseLabLoopbackObservationIngress ingress;
    private final EnterpriseLabExperimentObservationBaseline observationBaseline;
    private final EnterpriseLabExperimentConfiguration configuration;
    private final List<EnterpriseLabExperimentEvaluation> evaluations = new ArrayList<>();
    private final Map<String, CachedEvaluation> evaluationResults = new LinkedHashMap<>();

    public EnterpriseLabExperimentEvaluator(
            EnterpriseLabExperimentLifecycle lifecycle,
            EnterpriseLabLoopbackAllocationRouter router,
            EnterpriseLabLoopbackObservationIngress ingress,
            EnterpriseLabExperimentObservationBaseline observationBaseline) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle cannot be null");
        this.router = Objects.requireNonNull(router, "router cannot be null");
        this.ingress = Objects.requireNonNull(ingress, "ingress cannot be null");
        this.observationBaseline = Objects.requireNonNull(
                observationBaseline, "observationBaseline cannot be null");
        this.configuration = lifecycle.configuration()
                .orElseThrow(() -> new IllegalArgumentException("lifecycle must be configured before evaluation"));
        validateComposition();
    }

    public synchronized EvaluationReceipt evaluate(String evaluationId, Instant evaluatedAt) {
        return requestEvaluation(evaluationId, evaluatedAt, false, "deterministic policy evaluation");
    }

    public synchronized EvaluationReceipt cancel(
            String evaluationId,
            String operatorReason,
            Instant evaluatedAt) {
        return requestEvaluation(
                evaluationId,
                evaluatedAt,
                true,
                requireReason(operatorReason));
    }

    public synchronized List<EnterpriseLabExperimentEvaluation> evaluationHistory() {
        return List.copyOf(evaluations);
    }

    private EvaluationReceipt requestEvaluation(
            String evaluationId,
            Instant evaluatedAt,
            boolean cancellationRequested,
            String requestReason) {
        String safeEvaluationId = requireCanonicalId(evaluationId, "evaluationId");
        Instant safeTime = Objects.requireNonNull(evaluatedAt, "evaluatedAt cannot be null");
        String signature = (cancellationRequested ? "cancel" : "evaluate") + "|" + safeTime + "|" + requestReason;
        CachedEvaluation cached = evaluationResults.get(safeEvaluationId);
        if (cached != null) {
            if (cached.signature().equals(signature)) {
                return new EvaluationReceipt(
                        EvaluationStatus.IDEMPOTENT,
                        Optional.of(cached.evaluation()),
                        "idempotent evaluation replay");
            }
            return new EvaluationReceipt(
                    EvaluationStatus.CONFLICT,
                    Optional.empty(),
                    "evaluationId was already used with different inputs");
        }
        if (evaluations.size() >= MAX_RETAINED_EVALUATIONS) {
            return new EvaluationReceipt(
                    EvaluationStatus.CAPACITY_REJECTED,
                    Optional.empty(),
                    "bounded evaluation history capacity is exhausted");
        }

        LifecycleSnapshot before = lifecycle.snapshot();
        if (safeTime.isBefore(observationBaseline.capturedAt())
                || before.lastActivityAt().filter(last -> safeTime.isBefore(last)).isPresent()) {
            return new EvaluationReceipt(
                    EvaluationStatus.REJECTED,
                    Optional.empty(),
                    "evaluation time cannot precede baseline capture or lifecycle activity");
        }

        SignalSummary summary;
        ActionOutcome outcome;
        try {
            summary = collectSignals(before, safeTime);
            outcome = execute(before, summary, safeTime, cancellationRequested, requestReason);
        } catch (RuntimeException exception) {
            summary = emptySignals(before);
            outcome = failClosed(before, safeTime, exception);
        }
        LifecycleSnapshot after = lifecycle.snapshot();
        EnterpriseLabExperimentEvaluation evaluation = recordEvaluation(
                safeEvaluationId, safeTime, before, after, summary, outcome);
        evaluationResults.put(safeEvaluationId, new CachedEvaluation(signature, evaluation));
        return new EvaluationReceipt(
                EvaluationStatus.RECORDED,
                Optional.of(evaluation),
                evaluation.reason());
    }

    private ActionOutcome execute(
            LifecycleSnapshot before,
            SignalSummary summary,
            Instant evaluatedAt,
            boolean cancellationRequested,
            String requestReason) {
        if (before.terminal()) {
            return new ActionOutcome(
                    Disposition.TERMINAL_NO_CHANGE,
                    List.of(),
                    false,
                    false,
                    "terminal experiment required no evaluation action");
        }
        if (cancellationRequested) {
            return rollback(
                    Disposition.CANCELLED_AND_ROLLED_BACK,
                    EnumSet.of(Trigger.OPERATOR_CANCELLATION),
                    "operator cancellation: " + requestReason,
                    evaluatedAt);
        }

        Classification classification = classify(before, summary, evaluatedAt);
        if (classification.rollback()) {
            return rollback(
                    classification.disposition(),
                    EnumSet.copyOf(classification.triggers()),
                    classification.reason(),
                    evaluatedAt);
        }

        LifecycleSnapshot current = before;
        if (current.state() == EnterpriseLabExperimentState.RUNNING && boundaryReached(current, evaluatedAt)) {
            CommandReceipt advance = lifecycle.advance(commandId("advance"), evaluatedAt);
            current = advance.snapshot();
            if (current.state() == EnterpriseLabExperimentState.ROLLING_BACK) {
                return rollback(
                        Disposition.ROLLED_BACK_INVARIANT,
                        EnumSet.of(Trigger.DURATION_EXCEEDED),
                        "experiment expiration required baseline restoration",
                        evaluatedAt);
            }
            if (advance.status() != CommandStatus.APPLIED
                    || current.state() != EnterpriseLabExperimentState.HOLDING) {
                return rollback(
                        Disposition.ROLLED_BACK_INVARIANT,
                        EnumSet.of(Trigger.LIFECYCLE_INVARIANT),
                        "lifecycle could not enter bounded hold-down",
                        evaluatedAt);
            }
        }

        if (current.state() == EnterpriseLabExperimentState.RUNNING) {
            return new ActionOutcome(
                    Disposition.CONTINUE_RUNNING,
                    List.of(),
                    false,
                    false,
                    "healthy candidate remains inside bounded request and duration limits");
        }
        if (current.state() == EnterpriseLabExperimentState.HOLDING) {
            ProgressReceipt hold = lifecycle.recordHoldDownCycle(commandId("hold"), evaluatedAt);
            if (hold.status() != ProgressStatus.RECORDED && hold.status() != ProgressStatus.IDEMPOTENT) {
                return rollback(
                        Disposition.ROLLED_BACK_INVARIANT,
                        EnumSet.of(Trigger.LIFECYCLE_INVARIANT),
                        "bounded hold-down cycle could not be recorded",
                        evaluatedAt);
            }
            if (!hold.boundaryReached()) {
                return new ActionOutcome(
                        Disposition.CONTINUE_HOLDING,
                        List.of(),
                        false,
                        false,
                        "healthy evidence retained; additional bounded hold-down cycles remain");
            }
            return complete(evaluatedAt);
        }
        if (current.state() == EnterpriseLabExperimentState.COMPLETING) {
            return complete(evaluatedAt);
        }
        if (current.state() == EnterpriseLabExperimentState.ROLLING_BACK) {
            return rollback(
                    Disposition.ROLLED_BACK_INVARIANT,
                    EnumSet.of(Trigger.LIFECYCLE_INVARIANT),
                    "pre-existing rolling-back state required baseline restoration",
                    evaluatedAt);
        }
        return rollback(
                Disposition.ROLLED_BACK_INVARIANT,
                EnumSet.of(Trigger.LIFECYCLE_INVARIANT),
                "active evaluator encountered an unsupported lifecycle state",
                evaluatedAt);
    }

    private Classification classify(
            LifecycleSnapshot snapshot,
            SignalSummary summary,
            Instant evaluatedAt) {
        EnumSet<Trigger> invariant = EnumSet.noneOf(Trigger.class);
        EnumSet<Trigger> harmful = EnumSet.noneOf(Trigger.class);
        EnumSet<Trigger> insufficient = EnumSet.noneOf(Trigger.class);
        EnterpriseLabExperimentRollbackPolicy policy = configuration.rollbackPolicy();
        boolean boundary = snapshot.state() == EnterpriseLabExperimentState.HOLDING
                || snapshot.state() == EnterpriseLabExperimentState.COMPLETING
                || boundaryReached(snapshot, evaluatedAt);

        if (!summary.allocationGuardrailValid()) {
            invariant.add(Trigger.GUARDRAIL_VIOLATION);
        }
        if (!summary.fallbackViable()) {
            invariant.add(Trigger.NO_VIABLE_FALLBACK);
        }
        if (snapshot.requestCount() > configuration.maximumRequestCount()) {
            invariant.add(Trigger.REQUEST_LIMIT_EXCEEDED);
        }
        Instant maximumEnd = snapshot.startedAt().orElseThrow().plus(configuration.maximumDuration());
        if ((snapshot.state() == EnterpriseLabExperimentState.RUNNING && evaluatedAt.isAfter(maximumEnd))
                || !evaluatedAt.isBefore(configuration.expiresAt())) {
            invariant.add(Trigger.DURATION_EXCEEDED);
        }
        if (lifecycle.recordedEvidenceRequestIds().size() != snapshot.evidenceCount()
                || summary.correlatedObservationCount() > snapshot.evidenceCount()
                || (snapshot.candidateAllocationActive()
                        && snapshot.allocationRevision() != router.currentSnapshot().revision())) {
            invariant.add(Trigger.LIFECYCLE_INVARIANT);
        }

        if (summary.aggregateFailureRate() > policy.maximumFailureRate()) {
            harmful.add(Trigger.FAILURE_RATE);
        }
        if (summary.aggregateTimeoutRate() > policy.maximumTimeoutRate()) {
            harmful.add(Trigger.TIMEOUT_RATE);
        }
        if (summary.maximumLatencyRegressionRatio().isPresent()
                && summary.maximumLatencyRegressionRatio().getAsDouble()
                        > policy.maximumLatencyRegressionRatio()) {
            harmful.add(Trigger.LATENCY_REGRESSION);
        }
        if (summary.partiallyDegradedBackendCount() > policy.maximumPartiallyDegradedBackends()) {
            harmful.add(Trigger.PARTIAL_DEGRADATION);
        }
        if (summary.maximumConsecutiveTransportFailures()
                > policy.maximumConsecutiveTransportFailures()) {
            harmful.add(Trigger.CONSECUTIVE_TRANSPORT_FAILURES);
        }
        if (summary.freshObservationCount() >= configuration.minimumEvidenceCount()
                && summary.missingBackendCount() == 0
                && summary.staleBackendCount() == 0
                && summary.healthyBackendCount() < policy.minimumHealthyBackends()) {
            harmful.add(Trigger.HEALTHY_BACKEND_FLOOR);
        }

        if (snapshot.requestCount() > 0
                && summary.observationLossRate() > policy.maximumObservationLossRate()) {
            insufficient.add(Trigger.OBSERVATION_CAPTURE_LOSS);
        }
        if (snapshot.requestCount() > 0 && summary.staleBackendCount() > 0) {
            insufficient.add(Trigger.STALE_EVIDENCE);
        }
        if (boundary) {
            if (summary.missingBackendCount() > 0) {
                insufficient.add(Trigger.MISSING_EVIDENCE);
            }
            if (summary.sparseBackendCount() > 0
                    || summary.freshObservationCount() < configuration.minimumEvidenceCount()
                    || snapshot.evidenceCount() < configuration.minimumEvidenceCount()
                    || summary.candidateP95LatencyMillis().isEmpty()
                    || summary.baselineP95LatencyMillis().isEmpty()) {
                insufficient.add(Trigger.SPARSE_EVIDENCE_AT_BOUNDARY);
            }
        }

        if (!invariant.isEmpty()) {
            return new Classification(
                    Disposition.ROLLED_BACK_INVARIANT,
                    List.copyOf(invariant),
                    "allocation or lifecycle invariant required fail-closed rollback");
        }
        if (!harmful.isEmpty()) {
            return new Classification(
                    Disposition.ROLLED_BACK_HARMFUL,
                    List.copyOf(harmful),
                    "harmful post-allocation loopback evidence exceeded rollback policy");
        }
        if (!insufficient.isEmpty()) {
            return new Classification(
                    Disposition.ROLLED_BACK_INSUFFICIENT,
                    List.copyOf(insufficient),
                    "missing, stale, sparse, or lost evidence required conservative rollback");
        }
        return Classification.continueEvaluation();
    }

    private ActionOutcome complete(Instant evaluatedAt) {
        LifecycleSnapshot current = lifecycle.snapshot();
        if (current.state() == EnterpriseLabExperimentState.HOLDING) {
            CommandReceipt begin = lifecycle.beginCompletion(commandId("complete"), evaluatedAt);
            if (begin.status() != CommandStatus.APPLIED
                    || begin.snapshot().state() != EnterpriseLabExperimentState.COMPLETING) {
                return rollback(
                        Disposition.ROLLED_BACK_INVARIANT,
                        EnumSet.of(Trigger.LIFECYCLE_INVARIANT),
                        "lifecycle rejected safe completion after hold-down",
                        evaluatedAt);
            }
        }

        AllocationChangeReceipt restore;
        try {
            restore = router.restoreBaseline("normal bounded experiment completion");
        } catch (RuntimeException exception) {
            return new ActionOutcome(
                    Disposition.FAILED_CLOSED,
                    List.of(Trigger.BASELINE_RESTORATION_FAILED),
                    true,
                    false,
                    "safe completion could not restore the recorded baseline");
        }
        boolean restored = baselineReceiptVerified(restore);
        CommandReceipt confirm = lifecycle.confirmCompletion(commandId("confirm-complete"), restore, evaluatedAt);
        if (restored && confirm.snapshot().state() == EnterpriseLabExperimentState.COMPLETED) {
            return new ActionOutcome(
                    Disposition.COMPLETED,
                    List.of(),
                    true,
                    true,
                    "healthy hold-down completed and the recorded baseline was restored and confirmed");
        }
        EnumSet<Trigger> triggers = EnumSet.of(Trigger.LIFECYCLE_INVARIANT);
        if (!restored) {
            triggers.add(Trigger.BASELINE_RESTORATION_FAILED);
        }
        return new ActionOutcome(
                Disposition.FAILED_CLOSED,
                List.copyOf(triggers),
                true,
                restored,
                "completion failed closed because safe terminal confirmation was unavailable");
    }

    private ActionOutcome rollback(
            Disposition desiredDisposition,
            EnumSet<Trigger> triggers,
            String reason,
            Instant evaluatedAt) {
        LifecycleSnapshot current = lifecycle.snapshot();
        if (current.state() != EnterpriseLabExperimentState.ROLLING_BACK) {
            lifecycle.beginRollback(commandId("rollback"), boundedActionReason(reason), evaluatedAt);
        }

        AllocationChangeReceipt restore = null;
        boolean restored = false;
        try {
            restore = router.restoreBaseline(boundedActionReason(reason));
            restored = baselineReceiptVerified(restore);
        } catch (RuntimeException exception) {
            triggers.add(Trigger.BASELINE_RESTORATION_FAILED);
        }

        if (restore != null && lifecycle.snapshot().state() == EnterpriseLabExperimentState.ROLLING_BACK) {
            lifecycle.confirmRollback(commandId("confirm-rollback"), restore, evaluatedAt);
        }
        LifecycleSnapshot after = lifecycle.snapshot();
        if (!restored) {
            triggers.add(Trigger.BASELINE_RESTORATION_FAILED);
        }
        if (after.state() != EnterpriseLabExperimentState.ROLLED_BACK) {
            triggers.add(Trigger.LIFECYCLE_INVARIANT);
            return new ActionOutcome(
                    Disposition.FAILED_CLOSED,
                    List.copyOf(triggers),
                    true,
                    restored,
                    "rollback failed closed because baseline restoration or confirmation was unavailable");
        }
        return new ActionOutcome(
                desiredDisposition,
                List.copyOf(triggers),
                true,
                restored,
                boundedActionReason(reason));
    }

    private ActionOutcome failClosed(
            LifecycleSnapshot before,
            Instant evaluatedAt,
            RuntimeException exception) {
        EnumSet<Trigger> triggers = EnumSet.of(Trigger.INTERNAL_EVALUATION_FAILURE);
        String reason = "internal local evaluation failure: " + exception.getClass().getSimpleName();
        if (before.candidateAllocationActive()
                && (before.state() == EnterpriseLabExperimentState.RUNNING
                        || before.state() == EnterpriseLabExperimentState.HOLDING
                        || before.state() == EnterpriseLabExperimentState.COMPLETING
                        || before.state() == EnterpriseLabExperimentState.ROLLING_BACK)) {
            return rollback(Disposition.FAILED_CLOSED, triggers, reason, evaluatedAt);
        }
        return new ActionOutcome(
                Disposition.FAILED_CLOSED,
                List.copyOf(triggers),
                false,
                false,
                reason);
    }

    private SignalSummary collectSignals(LifecycleSnapshot lifecycleSnapshot, Instant evaluatedAt) {
        Set<String> evidenceRequestIds = Set.copyOf(lifecycle.recordedEvidenceRequestIds());
        ServerObservationWindowPolicy policy = ingress.observationWindowPolicy();
        Instant startedAt = lifecycleSnapshot.startedAt().orElseThrow();
        List<String> eligibleBackendIds = expectedCandidateBackendIds();
        Map<String, BackendEvidence> evidenceByBackend = new TreeMap<>();
        Map<String, ServerRollingSignalState> signals = new TreeMap<>();
        int correlated = 0;

        for (String backendId : configuration.baselineSnapshot().allocations().keySet()) {
            List<ServerObservation> observations = ingress.observations(backendId).stream()
                    .filter(observation -> evidenceRequestIds.contains(observation.observationId()))
                    .filter(observation -> observation.source() == ServerObservationSource.ENTERPRISE_LAB_LOOPBACK)
                    .filter(observation -> !observation.observedAt().isBefore(startedAt))
                    .filter(observation -> !observation.observedAt().isAfter(evaluatedAt))
                    .sorted(Comparator.comparing(ServerObservation::observedAt)
                            .thenComparing(ServerObservation::observationId))
                    .toList();
            correlated += observations.size();
            ServerObservationWindow window = ServerObservationWindow.create(backendId, policy)
                    .appendAll(observations, evaluatedAt);
            ServerRollingSignalState signal = window.snapshot(evaluatedAt);
            signals.put(backendId, signal);
            EnterpriseLabExperimentObservationBaseline.BackendBaseline baseline =
                    observationBaseline.backends().get(backendId);
            OptionalDouble regression = latencyRegression(
                    signal.latencyWindowSignal().rollingP95LatencyMillis(),
                    signal.latencyWindowSignal().rollingP99LatencyMillis(),
                    baseline.p95LatencyMillis(),
                    baseline.p99LatencyMillis());
            List<ServerObservation> fresh = observations.stream()
                    .filter(observation -> !observation.observedAt().isBefore(
                            evaluatedAt.minus(policy.maxSampleAge())))
                    .toList();
            evidenceByBackend.put(backendId, new BackendEvidence(
                    backendId,
                    signal.sampleCount(),
                    signal.successCount(),
                    signal.failureCount(),
                    signal.timeoutCount(),
                    signal.connectionFailureCount(),
                    signal.failureRate(),
                    signal.timeoutRate(),
                    consecutiveTransportFailures(fresh),
                    signal.evidence(),
                    signal.degradationState(),
                    signal.latencyWindowSignal().rollingP95LatencyMillis(),
                    signal.latencyWindowSignal().rollingP99LatencyMillis(),
                    baseline.p95LatencyMillis(),
                    baseline.p99LatencyMillis(),
                    regression,
                    signal.latestObservationAt()));
        }

        int freshCount = sum(eligibleBackendIds, signals, ServerRollingSignalState::sampleCount);
        int failureCount = sum(eligibleBackendIds, signals, ServerRollingSignalState::failureCount);
        int timeoutCount = sum(eligibleBackendIds, signals, ServerRollingSignalState::timeoutCount);
        double failureRate = rate(failureCount, freshCount);
        double timeoutRate = rate(timeoutCount, freshCount);
        double observationLossRate = rate(
                lifecycleSnapshot.requestCount() - lifecycleSnapshot.evidenceCount(),
                lifecycleSnapshot.requestCount());
        int healthy = count(eligibleBackendIds, signals,
                signal -> signal.degradationState() == ServerDegradationState.HEALTHY);
        int partial = count(eligibleBackendIds, signals,
                signal -> signal.degradationState() == ServerDegradationState.PARTIALLY_DEGRADED);
        int missing = count(eligibleBackendIds, signals, ServerRollingSignalState::missing);
        int stale = count(eligibleBackendIds, signals, ServerRollingSignalState::stale);
        int sparse = count(eligibleBackendIds, signals,
                signal -> signal.evidence() == ServerSignalEvidence.SPARSE);
        int maxConsecutive = eligibleBackendIds.stream()
                .map(evidenceByBackend::get)
                .mapToInt(BackendEvidence::consecutiveTransportFailureCount)
                .max()
                .orElse(0);
        OptionalDouble candidateP95 = maximumLatency(
                eligibleBackendIds, evidenceByBackend, BackendEvidence::p95LatencyMillis);
        OptionalDouble candidateP99 = maximumLatency(
                eligibleBackendIds, evidenceByBackend, BackendEvidence::p99LatencyMillis);
        OptionalDouble baselineP95 = maximumLatency(
                eligibleBackendIds, evidenceByBackend, BackendEvidence::baselineP95LatencyMillis);
        OptionalDouble baselineP99 = maximumLatency(
                eligibleBackendIds, evidenceByBackend, BackendEvidence::baselineP99LatencyMillis);
        OptionalDouble maxRegression = maximumLatency(
                eligibleBackendIds, evidenceByBackend, BackendEvidence::latencyRegressionRatio);

        return new SignalSummary(
                Map.copyOf(evidenceByBackend),
                correlated,
                freshCount,
                healthy,
                partial,
                missing,
                stale,
                sparse,
                maxConsecutive,
                failureRate,
                timeoutRate,
                observationLossRate,
                candidateP95,
                candidateP99,
                baselineP95,
                baselineP99,
                maxRegression,
                allocationGuardrailValid(lifecycleSnapshot),
                fallbackViable());
    }

    private SignalSummary emptySignals(LifecycleSnapshot snapshot) {
        Map<String, BackendEvidence> empty = new TreeMap<>();
        observationBaseline.backends().forEach((backendId, baseline) -> empty.put(backendId, new BackendEvidence(
                backendId,
                0,
                0,
                0,
                0,
                0,
                0.0,
                0.0,
                0,
                ServerSignalEvidence.MISSING,
                ServerDegradationState.UNKNOWN,
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                baseline.p95LatencyMillis(),
                baseline.p99LatencyMillis(),
                OptionalDouble.empty(),
                Optional.empty())));
        return new SignalSummary(
                Map.copyOf(empty),
                0,
                0,
                0,
                0,
                expectedCandidateBackendIds().size(),
                0,
                0,
                0,
                0.0,
                0.0,
                rate(snapshot.requestCount() - snapshot.evidenceCount(), snapshot.requestCount()),
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                false,
                fallbackViable());
    }

    private EnterpriseLabExperimentEvaluation recordEvaluation(
            String evaluationId,
            Instant evaluatedAt,
            LifecycleSnapshot before,
            LifecycleSnapshot after,
            SignalSummary summary,
            ActionOutcome outcome) {
        String previousFingerprint = evaluations.isEmpty()
                ? EnterpriseLabExperimentEvaluation.GENESIS_FINGERPRINT
                : evaluations.get(evaluations.size() - 1).contentFingerprint();
        EnterpriseLabExperimentEvaluation evaluation = new EnterpriseLabExperimentEvaluation(
                EnterpriseLabExperimentEvaluation.SCHEMA_VERSION,
                evaluations.size() + 1L,
                evaluationId,
                configuration.experimentId(),
                evaluatedAt,
                outcome.disposition(),
                outcome.triggers(),
                summary.backendEvidence(),
                summary.correlatedObservationCount(),
                before.requestCount(),
                before.evidenceCount(),
                summary.healthyBackendCount(),
                summary.aggregateFailureRate(),
                summary.aggregateTimeoutRate(),
                summary.observationLossRate(),
                summary.candidateP95LatencyMillis(),
                summary.candidateP99LatencyMillis(),
                summary.baselineP95LatencyMillis(),
                summary.baselineP99LatencyMillis(),
                summary.maximumLatencyRegressionRatio(),
                summary.allocationGuardrailValid(),
                outcome.baselineRestorationAttempted(),
                outcome.baselineRestorationSucceeded(),
                before.state(),
                after.state(),
                before.allocationRevision(),
                after.allocationRevision(),
                observationBaseline.contentFingerprint(),
                previousFingerprint,
                outcome.reason());
        evaluations.add(evaluation);
        return evaluation;
    }

    private void validateComposition() {
        LifecycleSnapshot snapshot = lifecycle.snapshot();
        Instant startedAt = snapshot.startedAt()
                .orElseThrow(() -> new IllegalArgumentException("experiment must be started before evaluation"));
        if (!configuration.experimentId().equals(observationBaseline.experimentId())
                || !configuration.scenarioId().equals(observationBaseline.scenarioId())
                || !configuration.contentFingerprint().equals(observationBaseline.configurationFingerprint())) {
            throw new IllegalArgumentException("observation baseline must match the configured experiment");
        }
        if (observationBaseline.capturedAt().isAfter(startedAt)) {
            throw new IllegalArgumentException("observation baseline must be captured before candidate start");
        }
        Set<String> expected = configuration.baselineSnapshot().allocations().keySet();
        if (!new TreeSet<>(expected).equals(new TreeSet<>(observationBaseline.backends().keySet()))
                || !new TreeSet<>(expected).equals(new TreeSet<>(ingress.approvedBackendIds()))
                || !new TreeSet<>(expected).equals(new TreeSet<>(router.baselineSnapshot().allocations().keySet()))) {
            throw new IllegalArgumentException("evaluator backends must exactly match the configured loopback set");
        }
        if (!EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                configuration.baselineSnapshot().allocations(),
                router.baselineSnapshot().allocations())) {
            throw new IllegalArgumentException("router baseline must match the configured restorable baseline");
        }
    }

    private boolean allocationGuardrailValid(LifecycleSnapshot snapshot) {
        TrafficAllocationGuardrailDecision guardrail = configuration.candidateDecision()
                .decision()
                .guardrailDecision();
        EnterpriseLabLoopbackAllocationSnapshot current = router.currentSnapshot();
        return guardrail.mode() == AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT
                && guardrail.action() != TrafficAllocationGuardrailAction.DENY
                && guardrail.influenceAllowed()
                && current.kind() == Kind.CANDIDATE
                && current.scenarioId().equals(configuration.scenarioId())
                && current.sourceDecisionId().equals(configuration.candidateDecisionId())
                && EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                        current.allocations(), guardrail.effectiveAllocations())
                && snapshot.candidateAllocationActive();
    }

    private boolean fallbackViable() {
        return !router.baselineSnapshot().eligibleBackendIds().isEmpty()
                && EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                        configuration.baselineSnapshot().allocations(),
                        router.baselineSnapshot().allocations());
    }

    private boolean baselineReceiptVerified(AllocationChangeReceipt receipt) {
        return receipt != null
                && (receipt.status() == ChangeStatus.RESTORED || receipt.status() == ChangeStatus.NO_CHANGE)
                && receipt.currentSnapshot().scenarioId().equals(configuration.scenarioId())
                && receipt.currentSnapshot().kind() != Kind.CANDIDATE
                && EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                        receipt.currentSnapshot().allocations(),
                        configuration.baselineSnapshot().allocations())
                && EnterpriseLabLoopbackAllocationSnapshot.sameAllocations(
                        router.currentSnapshot().allocations(),
                        configuration.baselineSnapshot().allocations());
    }

    private boolean boundaryReached(LifecycleSnapshot snapshot, Instant evaluatedAt) {
        return snapshot.requestCount() >= configuration.maximumRequestCount()
                || !evaluatedAt.isBefore(snapshot.startedAt().orElseThrow().plus(configuration.maximumDuration()));
    }

    private List<String> expectedCandidateBackendIds() {
        return configuration.candidateDecision().decision().guardrailDecision().effectiveAllocations().entrySet().stream()
                .filter(entry -> entry.getValue() > 0.0)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    private String commandId(String operation) {
        return "evaluation-" + (evaluations.size() + 1L) + "-" + operation;
    }

    private static OptionalDouble latencyRegression(
            OptionalDouble candidateP95,
            OptionalDouble candidateP99,
            OptionalDouble baselineP95,
            OptionalDouble baselineP99) {
        if (candidateP95.isEmpty() || candidateP99.isEmpty()
                || baselineP95.isEmpty() || baselineP99.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Math.max(
                ratio(candidateP95.getAsDouble(), baselineP95.getAsDouble()),
                ratio(candidateP99.getAsDouble(), baselineP99.getAsDouble())));
    }

    private static double ratio(double candidate, double baseline) {
        if (baseline == 0.0) {
            return candidate == 0.0 ? 1.0 : 1_000_000.0;
        }
        return candidate / baseline;
    }

    private static OptionalDouble maximumLatency(
            List<String> backendIds,
            Map<String, BackendEvidence> evidence,
            Function<BackendEvidence, OptionalDouble> getter) {
        double maximum = 0.0;
        for (String backendId : backendIds) {
            OptionalDouble value = getter.apply(evidence.get(backendId));
            if (value.isEmpty()) {
                return OptionalDouble.empty();
            }
            maximum = Math.max(maximum, value.getAsDouble());
        }
        return backendIds.isEmpty() ? OptionalDouble.empty() : OptionalDouble.of(maximum);
    }

    private static int sum(
            List<String> backendIds,
            Map<String, ServerRollingSignalState> signals,
            java.util.function.ToIntFunction<ServerRollingSignalState> getter) {
        return backendIds.stream().map(signals::get).mapToInt(getter).sum();
    }

    private static int count(
            List<String> backendIds,
            Map<String, ServerRollingSignalState> signals,
            java.util.function.Predicate<ServerRollingSignalState> predicate) {
        return (int) backendIds.stream().map(signals::get).filter(predicate).count();
    }

    private static int consecutiveTransportFailures(List<ServerObservation> observations) {
        int count = 0;
        for (int index = observations.size() - 1; index >= 0; index--) {
            ServerObservationOutcome outcome = observations.get(index).outcome();
            if (outcome != ServerObservationOutcome.TIMEOUT
                    && outcome != ServerObservationOutcome.CONNECTION_FAILURE) {
                break;
            }
            count++;
        }
        return count;
    }

    private static double rate(int numerator, int denominator) {
        return denominator <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, (double) numerator / denominator));
    }

    private static String requireCanonicalId(String value, String fieldName) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > MAX_ID_LENGTH || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException(fieldName + " must be a bounded canonical identifier");
        }
        return value;
    }

    private static String requireReason(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("reason cannot be null or blank");
        }
        String safe = value.replace('\r', ' ').replace('\n', ' ').trim();
        if (safe.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("reason cannot exceed 256 characters");
        }
        return safe;
    }

    private static String boundedActionReason(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("reason cannot be null or blank");
        }
        String reason = value.replace('\r', ' ').replace('\n', ' ').trim();
        return reason.length() <= 220 ? reason : reason.substring(0, 220);
    }

    public enum EvaluationStatus {
        RECORDED,
        IDEMPOTENT,
        CONFLICT,
        REJECTED,
        CAPACITY_REJECTED
    }

    public record EvaluationReceipt(
            EvaluationStatus status,
            Optional<EnterpriseLabExperimentEvaluation> evaluation,
            String reason) {

        public EvaluationReceipt {
            status = Objects.requireNonNull(status, "status cannot be null");
            evaluation = Objects.requireNonNull(evaluation, "evaluation cannot be null");
            reason = requireReason(reason);
            boolean requiresEvaluation = status == EvaluationStatus.RECORDED
                    || status == EvaluationStatus.IDEMPOTENT;
            if (requiresEvaluation != evaluation.isPresent()) {
                throw new IllegalArgumentException("evaluation presence must match receipt status");
            }
        }
    }

    private record SignalSummary(
            Map<String, BackendEvidence> backendEvidence,
            int correlatedObservationCount,
            int freshObservationCount,
            int healthyBackendCount,
            int partiallyDegradedBackendCount,
            int missingBackendCount,
            int staleBackendCount,
            int sparseBackendCount,
            int maximumConsecutiveTransportFailures,
            double aggregateFailureRate,
            double aggregateTimeoutRate,
            double observationLossRate,
            OptionalDouble candidateP95LatencyMillis,
            OptionalDouble candidateP99LatencyMillis,
            OptionalDouble baselineP95LatencyMillis,
            OptionalDouble baselineP99LatencyMillis,
            OptionalDouble maximumLatencyRegressionRatio,
            boolean allocationGuardrailValid,
            boolean fallbackViable) {
    }

    private record Classification(
            Disposition disposition,
            List<Trigger> triggers,
            String reason) {

        private static Classification continueEvaluation() {
            return new Classification(null, List.of(), "no rollback trigger detected");
        }

        private boolean rollback() {
            return disposition != null;
        }
    }

    private record ActionOutcome(
            Disposition disposition,
            List<Trigger> triggers,
            boolean baselineRestorationAttempted,
            boolean baselineRestorationSucceeded,
            String reason) {

        private ActionOutcome {
            disposition = Objects.requireNonNull(disposition, "disposition cannot be null");
            triggers = List.copyOf(Objects.requireNonNull(triggers, "triggers cannot be null"));
            reason = requireReason(reason);
        }
    }

    private record CachedEvaluation(String signature, EnterpriseLabExperimentEvaluation evaluation) {
    }
}
