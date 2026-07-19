package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.ArmRequest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.OperatorStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.RequestBatchRequest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabExperimentOperatorServiceTest {
    private static final String SCENARIO = "tail-latency-pressure";
    private static final Instant NOW = Instant.parse("2026-07-16T23:00:00Z");

    @Test
    void allocationReadinessGateClosesNewExperimentAdmission() {
        EnterpriseLabAllocationReconciliationGate allocationGate =
                EnterpriseLabAllocationReconciliationGate.pending();
        EnterpriseLabExperimentOperatorService service =
                new EnterpriseLabExperimentOperatorService(
                        EnterpriseLabExperimentTargetCatalog.empty(),
                        new EnterpriseLabScenarioCatalogService(),
                        new EnterpriseLabAdaptiveDecisionService(),
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        System::nanoTime,
                        EnterpriseLabExperimentOperatorService.DEFAULT_MAX_RETAINED_EXPERIMENTS,
                        EnterpriseLabExperimentRecoveryGate.inMemoryOnly(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(allocationGate));

        var denied = service.arm(
                arm("allocation-pending", "experiment-allocation-pending", 10, 2, 1),
                true);

        assertEquals(OperatorStatus.DENIED, denied.status());
        assertEquals("ALLOCATION_RECONCILIATION_NOT_READY", denied.reasonCode());
        assertTrue(service.allocationReconciliationStatus().isPresent());
        assertFalse(service.allocationReconciliationStatus().orElseThrow().admissionAllowed());
    }

    @Test
    void authenticatedWorkflowUsesRealLoopbackRequestsAndReturnsImmutableFinalEvidence() throws Exception {
        try (Fixture fixture = Fixture.start()) {
            var armed = fixture.service.arm(arm("arm-1", "experiment-1", 60, 15, 1), true);
            assertEquals(OperatorStatus.APPLIED, armed.status());
            assertEquals(EnterpriseLabExperimentState.ARMED,
                    armed.experimentRecord().orElseThrow().lifecycle().state());
            assertFalse(armed.trafficActionPerformed());

            fixture.clock.advance(Duration.ofSeconds(1));
            var baseline = fixture.service.executeRequests(
                    "experiment-1", new RequestBatchRequest("baseline-1", 30, Duration.ofSeconds(1)), true);
            assertEquals(OperatorStatus.RECORDED, baseline.status());
            assertEquals(30, baseline.sentCount());
            assertEquals(30, baseline.observationsRecorded());
            assertEquals(0, baseline.candidateRequestsRecorded());
            assertFalse(baseline.trafficActionPerformed());
            var baselineSecond = fixture.service.executeRequests(
                    "experiment-1", new RequestBatchRequest("baseline-2", 30, Duration.ofSeconds(1)), true);
            assertEquals(30, baselineSecond.observationsRecorded());

            fixture.clock.advance(Duration.ofSeconds(1));
            var started = fixture.service.start("experiment-1", "start-1", true);
            assertEquals(OperatorStatus.APPLIED, started.status());
            assertEquals(EnterpriseLabExperimentState.RUNNING,
                    started.experimentRecord().orElseThrow().lifecycle().state());
            assertTrue(started.trafficActionPerformed());
            assertEquals(Kind.CANDIDATE,
                    started.experimentRecord().orElseThrow().currentAllocation().kind());

            var replay = fixture.service.start("experiment-1", "start-1", true);
            assertEquals(OperatorStatus.IDEMPOTENT, replay.status());
            assertFalse(replay.trafficActionPerformed());

            fixture.clock.advance(Duration.ofSeconds(1));
            var candidate = fixture.service.executeRequests(
                    "experiment-1", new RequestBatchRequest("candidate-1", 30, Duration.ofSeconds(1)), true);
            assertEquals(30, candidate.sentCount());
            assertEquals(30, candidate.observationsRecorded());
            assertEquals(30, candidate.candidateRequestsRecorded());
            assertTrue(candidate.trafficActionPerformed());
            assertTrue(candidate.outcomes().stream().allMatch(value -> value.candidateAllocationUsed()));
            assertTrue(candidate.outcomes().stream().allMatch(value -> value.targetScope().contains("loopback")));
            var candidateSecond = fixture.service.executeRequests(
                    "experiment-1", new RequestBatchRequest("candidate-2", 30, Duration.ofSeconds(1)), true);
            assertEquals(30, candidateSecond.candidateRequestsRecorded());

            fixture.clock.advance(Duration.ofSeconds(1));
            var evaluated = fixture.service.evaluate("experiment-1", "evaluate-1", true);
            assertEquals(OperatorStatus.RECORDED, evaluated.status());
            EnterpriseLabExperimentOperatorRecord record = evaluated.experimentRecord().orElseThrow();
            assertEquals(EnterpriseLabExperimentState.COMPLETED, record.lifecycle().state(),
                    () -> "unexpected evaluation evidence: " + record.evaluations());
            assertEquals(Kind.RESTORED_BASELINE, record.currentAllocation().kind());
            assertTrue(evaluated.trafficActionPerformed());
            assertTrue(record.completedAt().isPresent());
            assertTrue(record.observationBaseline().isPresent());
            assertEquals(1, record.evaluations().size());
            assertEquals(record, fixture.service.findFinalRecord("experiment-1").orElseThrow());
            String previousActionFingerprint =
                    EnterpriseLabExperimentOperatorRecord.OperatorActionEvidence.GENESIS_FINGERPRINT;
            for (int index = 0; index < record.operatorActions().size(); index++) {
                var action = record.operatorActions().get(index);
                assertEquals(index + 1L, action.sequence());
                assertEquals(previousActionFingerprint, action.previousFingerprint());
                previousActionFingerprint = action.contentFingerprint();
            }

            var evaluationReplay = fixture.service.evaluate("experiment-1", "evaluate-1", true);
            assertEquals(OperatorStatus.IDEMPOTENT, evaluationReplay.status());
            assertEquals(record.contentFingerprint(),
                    evaluationReplay.experimentRecord().orElseThrow().contentFingerprint());
            assertEquals(120, fixture.backends.stream().mapToInt(value -> value.requestCount.get()).sum());
        }
    }

    @Test
    void explicitEnablementCatalogAndSingleActiveExperimentBoundariesFailClosed() throws Exception {
        EnterpriseLabExperimentOperatorService unbound =
                new EnterpriseLabExperimentOperatorService(EnterpriseLabExperimentTargetCatalog.empty());
        assertEquals("EXPLICIT_ENABLEMENT_REQUIRED",
                unbound.arm(arm("disabled", "experiment-disabled", 10, 2, 1), false).reasonCode());
        assertEquals("UNKNOWN_SCENARIO",
                unbound.arm(new ArmRequest(
                        "unknown", "experiment-unknown", "missing-scenario", 10,
                        Duration.ofSeconds(30), 2, 1, Duration.ofSeconds(60)), true).reasonCode());
        assertEquals("APPROVED_TARGETS_UNAVAILABLE",
                unbound.arm(arm("unbound", "experiment-unbound", 10, 2, 1), true).reasonCode());
        assertTrue(unbound.records().isEmpty());

        try (Fixture fixture = Fixture.start()) {
            assertEquals(OperatorStatus.APPLIED,
                    fixture.service.arm(arm("arm-a", "experiment-a", 10, 2, 1), true).status());
            var conflict = fixture.service.arm(arm("arm-b", "experiment-b", 10, 2, 1), true);
            assertEquals(OperatorStatus.CONFLICT, conflict.status());
            assertEquals("ACTIVE_EXPERIMENT_CONFLICT", conflict.reasonCode());
            assertEquals(OperatorStatus.IDEMPOTENT,
                    fixture.service.arm(arm("arm-a", "experiment-a", 10, 2, 1), true).status());
            assertEquals(OperatorStatus.CONFLICT,
                    fixture.service.arm(arm("arm-a", "experiment-a", 11, 2, 1), true).status());
        }
    }

    @Test
    void cancellationAndShutdownRestoreBaselineWithoutAdditionalCandidateUse() throws Exception {
        try (Fixture fixture = Fixture.start()) {
            fixture.service.arm(arm("arm-cancel", "experiment-cancel", 10, 2, 1), true);
            fixture.clock.advance(Duration.ofSeconds(1));
            fixture.service.start("experiment-cancel", "start-cancel", true);
            fixture.clock.advance(Duration.ofSeconds(1));
            var cancelled = fixture.service.cancel(
                    "experiment-cancel", "cancel-1", "operator requested bounded cancellation");
            assertEquals(OperatorStatus.RECORDED, cancelled.status());
            assertEquals(EnterpriseLabExperimentState.ROLLED_BACK,
                    cancelled.experimentRecord().orElseThrow().lifecycle().state());
            assertTrue(cancelled.trafficActionPerformed());
            assertEquals(Kind.RESTORED_BASELINE,
                    cancelled.experimentRecord().orElseThrow().currentAllocation().kind());
            assertEquals(OperatorStatus.IDEMPOTENT,
                    fixture.service.cancel(
                            "experiment-cancel", "cancel-1", "operator requested bounded cancellation").status());
        }

        Fixture shutdownFixture = Fixture.start();
        try {
            shutdownFixture.service.arm(arm("arm-shutdown", "experiment-shutdown", 10, 2, 1), true);
            shutdownFixture.clock.advance(Duration.ofSeconds(1));
            shutdownFixture.service.start("experiment-shutdown", "start-shutdown", true);
            shutdownFixture.clock.advance(Duration.ofSeconds(1));
            shutdownFixture.service.close();
            EnterpriseLabExperimentOperatorRecord record =
                    shutdownFixture.service.findFinalRecord("experiment-shutdown").orElseThrow();
            assertEquals(EnterpriseLabExperimentState.ROLLED_BACK, record.lifecycle().state());
            assertNotEquals(Kind.CANDIDATE, record.currentAllocation().kind());
            assertTrue(record.operatorActions().stream()
                    .anyMatch(action -> action.operation().equals("shutdown")));
            shutdownFixture.service.close();
        } finally {
            shutdownFixture.close();
        }
    }

    @Test
    void requestDurationTransitionAndTargetValidationRejectUnsafeInputs() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> arm("too-many", "experiment-too-many", 65, 2, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ArmRequest(
                        "too-long", "experiment-too-long", SCENARIO, 10,
                        Duration.ofMinutes(6), 2, 1, Duration.ofMinutes(6)));
        assertThrows(IllegalArgumentException.class,
                () -> new RequestBatchRequest("batch", 33, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new RequestBatchRequest("batch", 1, Duration.ofSeconds(6)));
        assertThrows(IllegalArgumentException.class,
                () -> new EnterpriseLabLoopbackTarget(
                        SCENARIO, "blue", URI.create("http://192.0.2.10:8080/backend")));

        try (LoopbackBackend blue = LoopbackBackend.start("blue")) {
            assertThrows(IllegalArgumentException.class,
                    () -> new EnterpriseLabExperimentTargetCatalog(List.of(
                            new EnterpriseLabLoopbackTarget(SCENARIO, "blue", blue.uri()))));
        }

        try (Fixture fixture = Fixture.start()) {
            fixture.service.arm(arm("arm-limit", "experiment-limit", 3, 2, 1), true);
            var startBeforeBaseline = fixture.service.start("experiment-limit", "start-limit", true);
            assertEquals(OperatorStatus.APPLIED, startBeforeBaseline.status());
            var excessive = fixture.service.executeRequests(
                    "experiment-limit", new RequestBatchRequest("batch-limit", 4, Duration.ofSeconds(1)), true);
            assertEquals(OperatorStatus.DENIED, excessive.status());
            assertEquals("REQUEST_LIMIT_EXCEEDED", excessive.reasonCode());
            assertFalse(excessive.trafficActionPerformed());
            var afterDisabled = fixture.service.executeRequests(
                    "experiment-limit", new RequestBatchRequest("disabled-route", 1, Duration.ofSeconds(1)), false);
            assertEquals("EXPLICIT_ENABLEMENT_REQUIRED", afterDisabled.reasonCode());
            assertEquals(EnterpriseLabExperimentState.ROLLED_BACK,
                    afterDisabled.experimentRecord().orElseThrow().lifecycle().state());
        }

        try (Fixture fixture = Fixture.start()) {
            fixture.service.arm(arm("arm-expired", "experiment-expired", 3, 2, 1), true);
            fixture.clock.advance(Duration.ofSeconds(60));
            var expired = fixture.service.start("experiment-expired", "start-expired", true);
            assertEquals(OperatorStatus.REJECTED, expired.status());
            assertEquals("EXPERIMENT_EXPIRED", expired.reasonCode());
            assertEquals(EnterpriseLabExperimentState.CANCELLED,
                    expired.experimentRecord().orElseThrow().lifecycle().state());
            assertEquals(Kind.BASELINE,
                    expired.experimentRecord().orElseThrow().currentAllocation().kind());
            assertFalse(expired.trafficActionPerformed(),
                    "expired experiments must fail before candidate allocation is installed");
        }

        try (Fixture fixture = Fixture.start()) {
            String maximumLengthExperimentId = "e".repeat(128);
            fixture.service.arm(arm("arm-long-id", maximumLengthExperimentId, 3, 2, 1), true);
            fixture.clock.advance(Duration.ofSeconds(1));
            fixture.service.start(maximumLengthExperimentId, "start-long-id", true);
            var routed = fixture.service.executeRequests(
                    maximumLengthExperimentId,
                    new RequestBatchRequest("route-long-id", 1, Duration.ofSeconds(1)),
                    true);
            assertEquals(1, routed.sentCount(),
                    "derived loopback request IDs must remain bounded when experimentId is at its maximum length");
            var disabled = fixture.service.executeRequests(
                    maximumLengthExperimentId,
                    new RequestBatchRequest("r".repeat(128), 1, Duration.ofSeconds(1)),
                    false);
            assertEquals(EnterpriseLabExperimentState.ROLLED_BACK,
                    disabled.experimentRecord().orElseThrow().lifecycle().state());
        }
    }

    @Test
    void startCannotInterleaveWithInFlightBaselineRequestBatch() throws Exception {
        HandlerGate gate = new HandlerGate();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Fixture fixture = Fixture.start(gate)) {
            fixture.service.arm(arm("arm-interleave", "experiment-interleave", 10, 2, 1), true);
            Future<?> baseline = executor.submit(() -> fixture.service.executeRequests(
                    "experiment-interleave",
                    new RequestBatchRequest("baseline-interleave", 1, Duration.ofSeconds(5)),
                    true));
            assertTrue(gate.entered.await(5, TimeUnit.SECONDS),
                    "loopback backend did not receive the bounded baseline request");

            var deniedStart = fixture.service.start(
                    "experiment-interleave", "start-during-baseline", true);
            assertEquals(OperatorStatus.CONFLICT, deniedStart.status());
            assertEquals("REQUEST_BATCH_IN_PROGRESS", deniedStart.reasonCode());
            assertFalse(deniedStart.trafficActionPerformed());

            var deniedBatch = fixture.service.executeRequests(
                    "experiment-interleave",
                    new RequestBatchRequest("second-baseline-interleave", 1, Duration.ofSeconds(1)),
                    true);
            assertEquals(OperatorStatus.CONFLICT, deniedBatch.status());
            assertEquals("REQUEST_BATCH_IN_PROGRESS", deniedBatch.reasonCode());
            assertEquals(0, deniedBatch.sentCount());

            gate.release.countDown();
            baseline.get(5, TimeUnit.SECONDS);
            assertEquals(OperatorStatus.APPLIED,
                    fixture.service.start("experiment-interleave", "start-after-baseline", true).status());
        } finally {
            gate.release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void simultaneousStatusReadsDoNotBlockCancellationOfInFlightCandidateRequest() throws Exception {
        HandlerGate gate = new HandlerGate();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        try (Fixture fixture = Fixture.start(gate)) {
            fixture.service.arm(arm("arm-concurrent", "experiment-concurrent", 10, 2, 1), true);
            fixture.clock.advance(Duration.ofSeconds(1));
            fixture.service.start("experiment-concurrent", "start-concurrent", true);

            Future<EnterpriseLabExperimentOperatorService.RequestBatchReceipt> route = executor.submit(
                    () -> fixture.service.executeRequests(
                            "experiment-concurrent",
                            new RequestBatchRequest("candidate-concurrent", 1, Duration.ofSeconds(5)),
                            true));
            assertTrue(gate.entered.await(5, TimeUnit.SECONDS),
                    "loopback backend did not receive the bounded candidate request");

            List<Future<String>> statusReads = new ArrayList<>();
            for (int index = 0; index < 4; index++) {
                statusReads.add(executor.submit(() -> fixture.service.findRecord("experiment-concurrent")
                        .orElseThrow()
                        .contentFingerprint()));
            }
            for (Future<String> statusRead : statusReads) {
                assertFalse(statusRead.get(5, TimeUnit.SECONDS).isBlank(),
                        "status reads must complete while loopback transport is in flight");
            }

            var cancelled = fixture.service.cancel(
                    "experiment-concurrent", "cancel-concurrent", "operator cancelled in-flight request");
            assertEquals(EnterpriseLabExperimentState.ROLLED_BACK,
                    cancelled.experimentRecord().orElseThrow().lifecycle().state());
            gate.release.countDown();

            var completedAfterRollback = route.get(5, TimeUnit.SECONDS);
            assertEquals(1, completedAfterRollback.sentCount());
            assertEquals(1, completedAfterRollback.observationsRecorded());
            assertEquals(0, completedAfterRollback.candidateRequestsRecorded());
            assertEquals("POST_TERMINAL_REQUEST_COMPLETION_RECORDED",
                    completedAfterRollback.reasonCode());
            assertEquals("DENIED", completedAfterRollback.outcomes().get(0).lifecycleProgress());

            var postRollback = fixture.service.executeRequests(
                    "experiment-concurrent",
                    new RequestBatchRequest("post-rollback-route", 1, Duration.ofSeconds(1)),
                    true);
            assertEquals(0, postRollback.sentCount());
            assertFalse(postRollback.trafficActionPerformed());
            assertEquals("ILLEGAL_LIFECYCLE_TRANSITION", postRollback.reasonCode());
        } finally {
            gate.release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void manualCancellationRemainsAvailableAtBoundedCommandCapacity() throws Exception {
        try (Fixture fixture = Fixture.start()) {
            fixture.service.arm(arm("arm-capacity", "experiment-capacity", 10, 2, 1), true);
            fixture.clock.advance(Duration.ofSeconds(1));
            fixture.service.start("experiment-capacity", "start-capacity", true);

            for (int index = 0; index < 125; index++) {
                var rejected = fixture.service.start(
                        "experiment-capacity", "invalid-start-" + index, true);
                assertEquals(OperatorStatus.REJECTED, rejected.status());
            }

            var cancelled = fixture.service.cancel(
                    "experiment-capacity", "cancel-at-capacity", "operator capacity safety test");
            assertEquals(OperatorStatus.RECORDED, cancelled.status());
            assertEquals(EnterpriseLabExperimentState.ROLLED_BACK,
                    cancelled.experimentRecord().orElseThrow().lifecycle().state());
            assertEquals(Kind.RESTORED_BASELINE,
                    cancelled.experimentRecord().orElseThrow().currentAllocation().kind());
            assertTrue(cancelled.trafficActionPerformed());
            assertEquals(OperatorStatus.IDEMPOTENT,
                    fixture.service.cancel(
                            "experiment-capacity", "cancel-at-capacity", "operator capacity safety test").status());
        }
    }

    private static ArmRequest arm(
            String requestId,
            String experimentId,
            int maximumRequests,
            int minimumEvidence,
            int holdCycles) {
        return new ArmRequest(
                requestId,
                experimentId,
                SCENARIO,
                maximumRequests,
                Duration.ofSeconds(30),
                minimumEvidence,
                holdCycles,
                Duration.ofSeconds(60));
    }

    private static final class Fixture implements AutoCloseable {
        private final List<LoopbackBackend> backends;
        private final MutableClock clock;
        private final EnterpriseLabExperimentOperatorService service;

        private Fixture(
                List<LoopbackBackend> backends,
                MutableClock clock,
                EnterpriseLabExperimentOperatorService service) {
            this.backends = backends;
            this.clock = clock;
            this.service = service;
        }

        private static Fixture start() throws IOException {
            return start(null);
        }

        private static Fixture start(HandlerGate gate) throws IOException {
            List<LoopbackBackend> backends = new ArrayList<>();
            try {
                backends.add(LoopbackBackend.start("blue", gate));
                backends.add(LoopbackBackend.start("green", gate));
                backends.add(LoopbackBackend.start("orange", gate));
                EnterpriseLabExperimentTargetCatalog targets = new EnterpriseLabExperimentTargetCatalog(
                        backends.stream()
                                .map(backend -> new EnterpriseLabLoopbackTarget(
                                        SCENARIO, backend.backendId, backend.uri()))
                                .toList());
                MutableClock clock = new MutableClock(NOW);
                AtomicLong nanos = new AtomicLong();
                EnterpriseLabExperimentOperatorService service = new EnterpriseLabExperimentOperatorService(
                        targets,
                        new EnterpriseLabScenarioCatalogService(),
                        new EnterpriseLabAdaptiveDecisionService(),
                        clock,
                        () -> nanos.addAndGet(1_000_000L),
                        8);
                return new Fixture(List.copyOf(backends), clock, service);
            } catch (RuntimeException | IOException exception) {
                backends.forEach(LoopbackBackend::close);
                throw exception;
            }
        }

        @Override
        public void close() {
            service.close();
            backends.forEach(LoopbackBackend::close);
        }
    }

    private static final class LoopbackBackend implements AutoCloseable {
        private final String backendId;
        private final HttpServer server;
        private final HandlerGate gate;
        private final AtomicInteger requestCount = new AtomicInteger();

        private LoopbackBackend(String backendId, HttpServer server, HandlerGate gate) {
            this.backendId = backendId;
            this.server = server;
            this.gate = gate;
        }

        private static LoopbackBackend start(String backendId) throws IOException {
            return start(backendId, null);
        }

        private static LoopbackBackend start(String backendId, HandlerGate gate) throws IOException {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
            LoopbackBackend backend = new LoopbackBackend(backendId, server, gate);
            server.createContext("/enterprise-lab/operator", backend::handle);
            server.start();
            return backend;
        }

        private void handle(HttpExchange exchange) throws IOException {
            requestCount.incrementAndGet();
            if (gate != null) {
                gate.entered.countDown();
                try {
                    if (!gate.release.await(5, TimeUnit.SECONDS)) {
                        exchange.sendResponseHeaders(504, -1);
                        exchange.close();
                        return;
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    exchange.sendResponseHeaders(503, -1);
                    exchange.close();
                    return;
                }
            }
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        }

        private URI uri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                    + "/enterprise-lab/operator");
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static final class HandlerGate {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
