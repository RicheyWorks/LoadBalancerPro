package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.core.ServerObservationOutcome;
import com.richmond423.loadbalancerpro.core.ServerObservationWindowPolicy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentEvaluation.Disposition;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentEvaluation.Trigger;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentEvaluator.EvaluationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackObservationIngress.ReceiptStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabExperimentEvaluatorTest {
    private static final String SCENARIO = "tail-latency-pressure";
    private static final Set<String> BACKENDS = Set.of("blue", "green", "orange");
    private static final Instant CREATED_AT = Instant.parse("2026-07-16T22:00:00Z");
    private static final ServerObservationWindowPolicy EVIDENCE_POLICY = new ServerObservationWindowPolicy(
            64,
            Duration.ofSeconds(60),
            2,
            2,
            0.25,
            0.75,
            3,
            2,
            0.5);
    private static final EnterpriseLabExperimentRollbackPolicy ROLLBACK_POLICY =
            new EnterpriseLabExperimentRollbackPolicy(0.25, 0.10, 1.50, 2, 3, 0, 0.10);
    private final EnterpriseLabAdaptiveDecisionService decisionService = new EnterpriseLabAdaptiveDecisionService();

    @Test
    void healthyActualCandidateRoutesCompleteAfterBoundedHoldAndRestoreBaseline() throws Exception {
        EnterpriseLabAdaptiveDecision decision = activeDecision();
        List<String> eligible = eligibleBackendIds(decision);
        try (LoopbackBackend blue = LoopbackBackend.start("blue");
                LoopbackBackend green = LoopbackBackend.start("green");
                LoopbackBackend orange = LoopbackBackend.start("orange")) {
            List<LoopbackBackend> backends = List.of(blue, green, orange);
            MutableClock clock = new MutableClock(CREATED_AT);
            AtomicLong nanos = new AtomicLong();
            ServerObservationWindowPolicy actualRoutePolicy = new ServerObservationWindowPolicy(
                    64, Duration.ofSeconds(60), 1, 1, 0.25, 0.75, 3, 2, 0.5);
            EnterpriseLabLoopbackObservationIngress ingress = ingress(clock, nanos, actualRoutePolicy);
            EnterpriseLabLoopbackAllocationRouter router = new EnterpriseLabLoopbackAllocationRouter(
                    backends.stream()
                            .map(backend -> new EnterpriseLabLoopbackTarget(
                                    SCENARIO, backend.backendId, backend.uri()))
                            .toList(),
                    ingress,
                    decision.decision().guardrailDecision().baselineAllocations());
            EnterpriseLabExperimentConfiguration configuration = configuration(
                    decision, router.baselineSnapshot(), eligible.size(), eligible.size(), 2,
                    Duration.ofSeconds(30), ROLLBACK_POLICY);
            seedHealthyBaseline(ingress, clock, nanos, 1, 100.0);
            EnterpriseLabExperimentObservationBaseline baseline =
                    EnterpriseLabExperimentObservationBaseline.capture(configuration, ingress, CREATED_AT);
            EnterpriseLabExperimentLifecycle lifecycle = start(configuration, decision, router);
            EnterpriseLabExperimentEvaluator evaluator =
                    new EnterpriseLabExperimentEvaluator(lifecycle, router, ingress, baseline);

            long ordinal = 0;
            for (String backendId : eligible) {
                while (!router.currentSnapshot().selectBackend(ordinal).equals(backendId)) {
                    ordinal++;
                }
                Instant requestTime = CREATED_AT.plusSeconds(2).plusMillis(ordinal);
                clock.set(requestTime);
                var route = router.route("actual-evaluator-" + backendId, ordinal, Duration.ofSeconds(1));
                assertEquals(backendId, route.selectedBackendId());
                assertEquals(ReceiptStatus.RECORDED, route.requestExecution().observationReceipt().status());
                lifecycle.recordRequest(route, requestTime);
                ordinal++;
            }

            var first = evaluator.evaluate("evaluation-healthy-1", CREATED_AT.plusSeconds(3));
            assertEquals(EvaluationStatus.RECORDED, first.status());
            assertEquals(Disposition.CONTINUE_HOLDING, first.evaluation().orElseThrow().disposition());
            assertEquals(EnterpriseLabExperimentState.HOLDING, lifecycle.snapshot().state());
            assertEquals(Kind.CANDIDATE, router.currentSnapshot().kind());

            var second = evaluator.evaluate("evaluation-healthy-2", CREATED_AT.plusSeconds(4));
            EnterpriseLabExperimentEvaluation completed = second.evaluation().orElseThrow();
            assertEquals(Disposition.COMPLETED, completed.disposition());
            assertEquals(EnterpriseLabExperimentState.COMPLETED, lifecycle.snapshot().state());
            assertTrue(completed.baselineRestorationAttempted());
            assertTrue(completed.baselineRestorationSucceeded());
            assertEquals(router.baselineSnapshot().allocations(), router.currentSnapshot().allocations());
            assertFalse(lifecycle.snapshot().candidateAllocationActive());
            assertEquals(eligible.size(), backends.stream().mapToInt(value -> value.requests.get()).sum());
            assertEvaluationChain(evaluator.evaluationHistory());

            assertEquals(EvaluationStatus.IDEMPOTENT,
                    evaluator.evaluate("evaluation-healthy-2", CREATED_AT.plusSeconds(4)).status());
            assertEquals(EvaluationStatus.CONFLICT,
                    evaluator.evaluate("evaluation-healthy-2", CREATED_AT.plusSeconds(5)).status());
        }
    }

    @Test
    void harmfulTimeoutAndFailureEvidenceRollsBackImmediately() {
        Fixture fixture = fixture(10, 3, 2, EVIDENCE_POLICY, ROLLBACK_POLICY);
        String backendId = eligibleBackendIds(fixture.decision).get(0);
        for (int index = 0; index < 3; index++) {
            record(fixture, "timeout-" + index, backendId, ServerObservationOutcome.TIMEOUT,
                    20.0, CREATED_AT.plusSeconds(2).plusMillis(index));
        }

        EnterpriseLabExperimentEvaluation evaluation = fixture.evaluator
                .evaluate("evaluation-timeout", CREATED_AT.plusSeconds(3))
                .evaluation()
                .orElseThrow();

        assertEquals(Disposition.ROLLED_BACK_HARMFUL, evaluation.disposition());
        assertTrue(evaluation.triggers().contains(Trigger.FAILURE_RATE));
        assertTrue(evaluation.triggers().contains(Trigger.TIMEOUT_RATE));
        assertEquals(EnterpriseLabExperimentState.ROLLED_BACK, fixture.lifecycle.snapshot().state());
        assertEquals(fixture.router.baselineSnapshot().allocations(), fixture.router.currentSnapshot().allocations());
        assertTrue(evaluation.baselineRestorationSucceeded());
    }

    @Test
    void observationCaptureLossAndStaleBoundaryEvidenceAreDistinctConservativeRollbacks() {
        Fixture loss = fixture(10, 2, 1, EVIDENCE_POLICY, ROLLBACK_POLICY);
        loss.lifecycle.recordRequestProgress("lost-observation", false, CREATED_AT.plusSeconds(2));

        EnterpriseLabExperimentEvaluation lost = loss.evaluator
                .evaluate("evaluation-loss", CREATED_AT.plusSeconds(3))
                .evaluation()
                .orElseThrow();
        assertEquals(Disposition.ROLLED_BACK_INSUFFICIENT, lost.disposition());
        assertTrue(lost.triggers().contains(Trigger.OBSERVATION_CAPTURE_LOSS));

        ServerObservationWindowPolicy shortAge = new ServerObservationWindowPolicy(
                64, Duration.ofSeconds(2), 2, 2, 0.25, 0.75, 3, 2, 0.5);
        Fixture stale = fixture(4, 4, 1, shortAge, ROLLBACK_POLICY);
        List<String> eligible = eligibleBackendIds(stale.decision);
        for (int index = 0; index < 4; index++) {
            record(stale, "stale-" + index, eligible.get(index % eligible.size()),
                    ServerObservationOutcome.SUCCESS, 10.0,
                    CREATED_AT.plusSeconds(2).plusMillis(index));
        }

        EnterpriseLabExperimentEvaluation staleEvaluation = stale.evaluator
                .evaluate("evaluation-stale", CREATED_AT.plusSeconds(5))
                .evaluation()
                .orElseThrow();
        assertEquals(Disposition.ROLLED_BACK_INSUFFICIENT, staleEvaluation.disposition());
        assertTrue(staleEvaluation.triggers().contains(Trigger.STALE_EVIDENCE));
        assertTrue(staleEvaluation.triggers().contains(Trigger.SPARSE_EVIDENCE_AT_BOUNDARY));
    }

    @Test
    void tailLatencyRegressionUsesCapturedBaselineAndRollsBack() {
        Fixture fixture = fixture(6, 6, 1, EVIDENCE_POLICY, ROLLBACK_POLICY);
        List<String> eligible = eligibleBackendIds(fixture.decision);
        for (int index = 0; index < 6; index++) {
            record(fixture, "slow-" + index, eligible.get(index % eligible.size()),
                    ServerObservationOutcome.SUCCESS, 40.0,
                    CREATED_AT.plusSeconds(2).plusMillis(index));
        }

        EnterpriseLabExperimentEvaluation evaluation = fixture.evaluator
                .evaluate("evaluation-latency", CREATED_AT.plusSeconds(3))
                .evaluation()
                .orElseThrow();

        assertEquals(Disposition.ROLLED_BACK_HARMFUL, evaluation.disposition());
        assertTrue(evaluation.triggers().contains(Trigger.LATENCY_REGRESSION));
        assertTrue(evaluation.maximumLatencyRegressionRatio().orElseThrow() > 1.5);
        assertEquals(EnterpriseLabExperimentState.ROLLED_BACK, fixture.lifecycle.snapshot().state());
    }

    @Test
    void partialDegradationAndHealthyBackendFloorRemainDistinctHarmfulTriggers() {
        int eligibleCount = eligibleBackendIds(activeDecision()).size();
        int requestCount = eligibleCount * 2;
        Fixture partial = fixture(requestCount, requestCount, 1, EVIDENCE_POLICY, ROLLBACK_POLICY);
        List<String> eligible = eligibleBackendIds(partial.decision);
        int requestIndex = 0;
        for (String backendId : eligible) {
            record(partial, "partial-" + requestIndex++, backendId,
                    ServerObservationOutcome.SUCCESS,
                    10.0, CREATED_AT.plusSeconds(2).plusMillis(requestIndex));
            record(partial, "partial-" + requestIndex++, backendId,
                    backendId.equals(eligible.get(0))
                            ? ServerObservationOutcome.FAILURE
                            : ServerObservationOutcome.SUCCESS,
                    10.0, CREATED_AT.plusSeconds(2).plusMillis(requestIndex));
        }
        EnterpriseLabExperimentEvaluation partialEvaluation = partial.evaluator
                .evaluate("evaluation-partial", CREATED_AT.plusSeconds(3))
                .evaluation()
                .orElseThrow();
        assertEquals(Disposition.ROLLED_BACK_HARMFUL, partialEvaluation.disposition());
        assertTrue(partialEvaluation.triggers().contains(Trigger.PARTIAL_DEGRADATION));

        EnterpriseLabExperimentRollbackPolicy floorPolicy = new EnterpriseLabExperimentRollbackPolicy(
                1.0, 1.0, 10.0, eligibleCount, 100, 64, 1.0);
        Fixture floor = fixture(requestCount, requestCount, 1, EVIDENCE_POLICY, floorPolicy);
        requestIndex = 0;
        for (String backendId : eligibleBackendIds(floor.decision)) {
            record(floor, "floor-" + requestIndex++, backendId,
                    ServerObservationOutcome.SUCCESS,
                    10.0, CREATED_AT.plusSeconds(2).plusMillis(requestIndex));
            record(floor, "floor-" + requestIndex++, backendId,
                    backendId.equals(eligible.get(0))
                            ? ServerObservationOutcome.FAILURE
                            : ServerObservationOutcome.SUCCESS,
                    10.0, CREATED_AT.plusSeconds(2).plusMillis(requestIndex));
        }
        EnterpriseLabExperimentEvaluation floorEvaluation = floor.evaluator
                .evaluate("evaluation-floor", CREATED_AT.plusSeconds(3))
                .evaluation()
                .orElseThrow();
        assertEquals(Disposition.ROLLED_BACK_HARMFUL, floorEvaluation.disposition());
        assertEquals(List.of(Trigger.HEALTHY_BACKEND_FLOOR), floorEvaluation.triggers());
    }

    @Test
    void missingBoundaryEvidenceAndLateDurationAreDistinctRollbackClasses() {
        Fixture missing = fixture(2, 2, 1, EVIDENCE_POLICY, ROLLBACK_POLICY);
        String selectedBackend = eligibleBackendIds(missing.decision).get(0);
        record(missing, "missing-0", selectedBackend,
                ServerObservationOutcome.SUCCESS, 10.0, CREATED_AT.plusSeconds(2));
        record(missing, "missing-1", selectedBackend,
                ServerObservationOutcome.SUCCESS, 10.0, CREATED_AT.plusSeconds(2).plusMillis(1));

        EnterpriseLabExperimentEvaluation missingEvaluation = missing.evaluator
                .evaluate("evaluation-missing", CREATED_AT.plusSeconds(3))
                .evaluation()
                .orElseThrow();
        assertEquals(Disposition.ROLLED_BACK_INSUFFICIENT, missingEvaluation.disposition());
        assertTrue(missingEvaluation.triggers().contains(Trigger.MISSING_EVIDENCE));
        assertTrue(missingEvaluation.triggers().contains(Trigger.SPARSE_EVIDENCE_AT_BOUNDARY));

        Fixture late = fixture(10, 2, 1, EVIDENCE_POLICY, ROLLBACK_POLICY);
        EnterpriseLabExperimentEvaluation duration = late.evaluator
                .evaluate("evaluation-duration-exceeded", CREATED_AT.plusSeconds(32))
                .evaluation()
                .orElseThrow();
        assertEquals(Disposition.ROLLED_BACK_INVARIANT, duration.disposition());
        assertTrue(duration.triggers().contains(Trigger.DURATION_EXCEEDED));
        assertEquals(EnterpriseLabExperimentState.ROLLED_BACK, late.lifecycle.snapshot().state());
    }

    @Test
    void guardrailDriftAndOperatorCancellationUseVerifiedIdempotentRestoration() {
        Fixture drift = fixture(10, 2, 1, EVIDENCE_POLICY, ROLLBACK_POLICY);
        drift.router.restoreBaseline("external local safety action");

        EnterpriseLabExperimentEvaluation invariant = drift.evaluator
                .evaluate("evaluation-drift", CREATED_AT.plusSeconds(2))
                .evaluation()
                .orElseThrow();
        assertEquals(Disposition.ROLLED_BACK_INVARIANT, invariant.disposition());
        assertTrue(invariant.triggers().contains(Trigger.GUARDRAIL_VIOLATION));
        assertTrue(invariant.triggers().contains(Trigger.LIFECYCLE_INVARIANT));
        assertTrue(invariant.baselineRestorationSucceeded());

        Fixture cancelled = fixture(10, 2, 1, EVIDENCE_POLICY, ROLLBACK_POLICY);
        EnterpriseLabExperimentEvaluation cancellation = cancelled.evaluator
                .cancel("evaluation-cancel", "operator requested local stop", CREATED_AT.plusSeconds(2))
                .evaluation()
                .orElseThrow();
        assertEquals(Disposition.CANCELLED_AND_ROLLED_BACK, cancellation.disposition());
        assertEquals(List.of(Trigger.OPERATOR_CANCELLATION), cancellation.triggers());
        assertEquals(EnterpriseLabExperimentState.ROLLED_BACK, cancelled.lifecycle.snapshot().state());
        assertTrue(cancellation.baselineRestorationSucceeded());
    }

    @Test
    void unavailableLifecycleConfirmationLeavesRoutingSafeAndRecordsFailedClosedEvidence() {
        Fixture fixture = fixture(10, 2, 1, EVIDENCE_POLICY, ROLLBACK_POLICY);
        String backendId = eligibleBackendIds(fixture.decision).get(0);
        record(fixture, "harmful-before-capacity", backendId,
                ServerObservationOutcome.TIMEOUT, 20.0, CREATED_AT.plusSeconds(2));
        for (int index = 0; index < 62; index++) {
            fixture.lifecycle.advance("capacity-advance-" + index, CREATED_AT.plusSeconds(2));
        }

        EnterpriseLabExperimentEvaluation evaluation = fixture.evaluator
                .evaluate("evaluation-capacity-fail-closed", CREATED_AT.plusSeconds(3))
                .evaluation()
                .orElseThrow();

        assertEquals(Disposition.FAILED_CLOSED, evaluation.disposition());
        assertTrue(evaluation.triggers().contains(Trigger.LIFECYCLE_INVARIANT));
        assertTrue(evaluation.baselineRestorationAttempted());
        assertTrue(evaluation.baselineRestorationSucceeded());
        assertEquals(fixture.router.baselineSnapshot().allocations(), fixture.router.currentSnapshot().allocations());
        assertFalse(fixture.router.route("safe-after-failed-confirm", 0, Duration.ofMillis(10))
                .candidateAllocationUsed());
    }

    @Test
    void baselineAndEvaluationEvidenceAreImmutableFingerprintChainedAndBounded() {
        Fixture fixture = fixture(10, 2, 1, EVIDENCE_POLICY, ROLLBACK_POLICY);
        assertThrows(UnsupportedOperationException.class,
                () -> fixture.observationBaseline.backends().clear());
        assertTrue(fixture.observationBaseline.contentFingerprint().matches("[0-9a-f]{64}"));

        for (int index = 0; index < EnterpriseLabExperimentEvaluator.MAX_RETAINED_EVALUATIONS; index++) {
            assertEquals(EvaluationStatus.RECORDED,
                    fixture.evaluator.evaluate("bounded-evaluation-" + index, CREATED_AT.plusSeconds(2)).status());
        }
        assertEquals(EvaluationStatus.CAPACITY_REJECTED,
                fixture.evaluator.evaluate("bounded-evaluation-overflow", CREATED_AT.plusSeconds(2)).status());
        assertEquals(EnterpriseLabExperimentEvaluator.MAX_RETAINED_EVALUATIONS,
                fixture.evaluator.evaluationHistory().size());
        assertThrows(UnsupportedOperationException.class,
                () -> fixture.evaluator.evaluationHistory().clear());
        assertEvaluationChain(fixture.evaluator.evaluationHistory());
    }

    @Test
    void evaluatorRejectsMismatchedOrPostStartObservationBaselines() {
        Fixture fixture = fixture(10, 2, 1, EVIDENCE_POLICY, ROLLBACK_POLICY);
        EnterpriseLabExperimentObservationBaseline postStart = new EnterpriseLabExperimentObservationBaseline(
                EnterpriseLabExperimentObservationBaseline.SCHEMA_VERSION,
                fixture.configuration.experimentId(),
                fixture.configuration.scenarioId(),
                fixture.configuration.contentFingerprint(),
                CREATED_AT.plusSeconds(2),
                fixture.observationBaseline.backends());
        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabExperimentEvaluator(
                        fixture.lifecycle, fixture.router, fixture.ingress, postStart));

        EnterpriseLabExperimentObservationBaseline wrongFingerprint = new EnterpriseLabExperimentObservationBaseline(
                EnterpriseLabExperimentObservationBaseline.SCHEMA_VERSION,
                fixture.configuration.experimentId(),
                fixture.configuration.scenarioId(),
                "0".repeat(64),
                CREATED_AT,
                fixture.observationBaseline.backends());
        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabExperimentEvaluator(
                        fixture.lifecycle, fixture.router, fixture.ingress, wrongFingerprint));
        assertEquals(EvaluationStatus.REJECTED,
                fixture.evaluator.evaluate("time-regression", CREATED_AT).status());
    }

    private Fixture fixture(
            int maxRequests,
            int minimumEvidence,
            int holdCycles,
            ServerObservationWindowPolicy observationPolicy,
            EnterpriseLabExperimentRollbackPolicy rollbackPolicy) {
        EnterpriseLabAdaptiveDecision decision = activeDecision();
        MutableClock clock = new MutableClock(CREATED_AT);
        AtomicLong nanos = new AtomicLong();
        EnterpriseLabLoopbackObservationIngress ingress = ingress(clock, nanos, observationPolicy);
        EnterpriseLabLoopbackAllocationRouter router = new EnterpriseLabLoopbackAllocationRouter(
                List.of(
                        target("blue", 49211),
                        target("green", 49212),
                        target("orange", 49213)),
                ingress,
                decision.decision().guardrailDecision().baselineAllocations());
        EnterpriseLabExperimentConfiguration configuration = configuration(
                decision,
                router.baselineSnapshot(),
                maxRequests,
                minimumEvidence,
                holdCycles,
                Duration.ofSeconds(30),
                rollbackPolicy);
        seedHealthyBaseline(ingress, clock, nanos, 2, 10.0);
        EnterpriseLabExperimentObservationBaseline observationBaseline =
                EnterpriseLabExperimentObservationBaseline.capture(configuration, ingress, CREATED_AT);
        EnterpriseLabExperimentLifecycle lifecycle = start(configuration, decision, router);
        EnterpriseLabExperimentEvaluator evaluator =
                new EnterpriseLabExperimentEvaluator(lifecycle, router, ingress, observationBaseline);
        return new Fixture(
                decision,
                configuration,
                lifecycle,
                router,
                ingress,
                observationBaseline,
                evaluator,
                clock,
                nanos);
    }

    private EnterpriseLabExperimentConfiguration configuration(
            EnterpriseLabAdaptiveDecision decision,
            EnterpriseLabLoopbackAllocationSnapshot baseline,
            int maxRequests,
            int minimumEvidence,
            int holdCycles,
            Duration maximumDuration,
            EnterpriseLabExperimentRollbackPolicy rollbackPolicy) {
        return new EnterpriseLabExperimentConfiguration(
                EnterpriseLabExperimentConfiguration.SCHEMA_VERSION,
                "experiment-evaluator-test",
                decision,
                baseline,
                maxRequests,
                maximumDuration,
                minimumEvidence,
                holdCycles,
                rollbackPolicy,
                AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT,
                true,
                CREATED_AT,
                CREATED_AT.plusSeconds(90));
    }

    private static EnterpriseLabExperimentLifecycle start(
            EnterpriseLabExperimentConfiguration configuration,
            EnterpriseLabAdaptiveDecision decision,
            EnterpriseLabLoopbackAllocationRouter router) {
        EnterpriseLabExperimentLifecycle lifecycle = new EnterpriseLabExperimentLifecycle();
        lifecycle.arm("evaluator-arm", configuration, CREATED_AT);
        lifecycle.start("evaluator-start", router.applyCandidate(decision, true), CREATED_AT.plusSeconds(1));
        return lifecycle;
    }

    private static void seedHealthyBaseline(
            EnterpriseLabLoopbackObservationIngress ingress,
            MutableClock clock,
            AtomicLong nanos,
            int samplesPerBackend,
            double latencyMillis) {
        for (String backendId : BACKENDS) {
            for (int index = 0; index < samplesPerBackend; index++) {
                String requestId = "baseline-" + backendId + "-" + index;
                clock.set(CREATED_AT);
                var begin = ingress.begin(requestId, backendId);
                nanos.addAndGet(Duration.ofNanos((long) (latencyMillis * 1_000_000.0)).toNanos());
                assertEquals(ReceiptStatus.RECORDED,
                        ingress.completeHttp(begin.attempt().orElseThrow(), 204).status());
            }
        }
    }

    private static void record(
            Fixture fixture,
            String requestId,
            String backendId,
            ServerObservationOutcome outcome,
            double latencyMillis,
            Instant observedAt) {
        fixture.clock.set(observedAt);
        var begin = fixture.ingress.begin(requestId, backendId);
        fixture.nanos.addAndGet((long) (latencyMillis * 1_000_000.0));
        var receipt = switch (outcome) {
            case SUCCESS -> fixture.ingress.completeHttp(begin.attempt().orElseThrow(), 204);
            case FAILURE -> fixture.ingress.completeHttp(begin.attempt().orElseThrow(), 503);
            case TIMEOUT -> fixture.ingress.completeTimeout(begin.attempt().orElseThrow());
            case CONNECTION_FAILURE -> fixture.ingress.completeConnectionFailure(
                    begin.attempt().orElseThrow(), "test connection failure");
        };
        assertEquals(ReceiptStatus.RECORDED, receipt.status());
        fixture.lifecycle.recordRequestProgress(requestId, true, observedAt);
    }

    private EnterpriseLabAdaptiveDecision activeDecision() {
        return decisionService.decide(SCENARIO, "active-experiment", true, false, false);
    }

    private static List<String> eligibleBackendIds(EnterpriseLabAdaptiveDecision decision) {
        return decision.decision().guardrailDecision().effectiveAllocations().entrySet().stream()
                .filter(entry -> entry.getValue() > 0.0)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    private static EnterpriseLabLoopbackObservationIngress ingress(
            MutableClock clock,
            AtomicLong nanos,
            ServerObservationWindowPolicy policy) {
        return new EnterpriseLabLoopbackObservationIngress(
                BACKENDS,
                policy,
                16,
                Duration.ofSeconds(1),
                clock,
                nanos::get);
    }

    private static EnterpriseLabLoopbackTarget target(String backendId, int port) {
        return new EnterpriseLabLoopbackTarget(
                SCENARIO,
                backendId,
                URI.create("http://127.0.0.1:" + port + "/enterprise-lab/evaluator"));
    }

    private static void assertEvaluationChain(List<EnterpriseLabExperimentEvaluation> evaluations) {
        assertFalse(evaluations.isEmpty());
        assertEquals(EnterpriseLabExperimentEvaluation.GENESIS_FINGERPRINT,
                evaluations.get(0).previousFingerprint());
        for (int index = 0; index < evaluations.size(); index++) {
            EnterpriseLabExperimentEvaluation evaluation = evaluations.get(index);
            assertEquals(index + 1L, evaluation.sequence());
            assertTrue(evaluation.contentFingerprint().matches("[0-9a-f]{64}"));
            if (index > 0) {
                assertEquals(evaluations.get(index - 1).contentFingerprint(), evaluation.previousFingerprint());
                assertNotEquals(evaluations.get(index - 1).contentFingerprint(), evaluation.contentFingerprint());
            }
        }
    }

    private record Fixture(
            EnterpriseLabAdaptiveDecision decision,
            EnterpriseLabExperimentConfiguration configuration,
            EnterpriseLabExperimentLifecycle lifecycle,
            EnterpriseLabLoopbackAllocationRouter router,
            EnterpriseLabLoopbackObservationIngress ingress,
            EnterpriseLabExperimentObservationBaseline observationBaseline,
            EnterpriseLabExperimentEvaluator evaluator,
            MutableClock clock,
            AtomicLong nanos) {
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;

        private MutableClock(Instant initial) {
            this.current = new AtomicReference<>(initial);
        }

        void set(Instant instant) {
            current.set(instant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (!ZoneOffset.UTC.equals(zone)) {
                throw new IllegalArgumentException("test clock supports UTC only");
            }
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }

    private static final class LoopbackBackend implements AutoCloseable {
        private final String backendId;
        private final HttpServer server;
        private final AtomicInteger requests = new AtomicInteger();

        private LoopbackBackend(String backendId, HttpServer server) {
            this.backendId = backendId;
            this.server = server;
        }

        static LoopbackBackend start(String backendId) throws IOException {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
            LoopbackBackend backend = new LoopbackBackend(backendId, server);
            server.createContext("/enterprise-lab/evaluator", exchange -> {
                backend.requests.incrementAndGet();
                respond(exchange, 204);
            });
            server.setExecutor(null);
            server.start();
            return backend;
        }

        URI uri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                    + "/enterprise-lab/evaluator");
        }

        @Override
        public void close() {
            server.stop(0);
        }

        private static void respond(HttpExchange exchange, int statusCode) throws IOException {
            exchange.sendResponseHeaders(statusCode, -1);
            exchange.close();
        }
    }
}
