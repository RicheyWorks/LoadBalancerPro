package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.CommandStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentLifecycle.ProgressStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.AllocationChangeReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabExperimentLifecycleTest {
    private static final String SCENARIO = "tail-latency-pressure";
    private static final Instant CREATED_AT = Instant.parse("2026-07-16T20:00:00Z");
    private final EnterpriseLabAdaptiveDecisionService decisionService = new EnterpriseLabAdaptiveDecisionService();

    @Test
    void successfulLifecycleIsExplicitIdempotentFingerprintChainedAndSafeAtTerminal() {
        Fixture fixture = fixture(true, 3, 2, 2, Duration.ofSeconds(30), CREATED_AT.plusSeconds(90));
        EnterpriseLabExperimentLifecycle lifecycle = new EnterpriseLabExperimentLifecycle();

        var armed = lifecycle.arm("command-arm", fixture.configuration, CREATED_AT);
        assertEquals(CommandStatus.APPLIED, armed.status());
        assertEquals(EnterpriseLabExperimentState.ARMED, armed.snapshot().state());
        assertEquals(CommandStatus.IDEMPOTENT,
                lifecycle.arm("command-arm", fixture.configuration, CREATED_AT).status());
        assertEquals(CommandStatus.CONFLICT,
                lifecycle.arm("command-arm", fixture.configuration, CREATED_AT.plusSeconds(1)).status());
        assertEquals(1, lifecycle.transitionHistory().size());

        AllocationChangeReceipt applied = fixture.router.applyCandidate(fixture.decision, true);
        var started = lifecycle.start("command-start", applied, CREATED_AT.plusSeconds(1));
        assertEquals(CommandStatus.APPLIED, started.status());
        assertTrue(started.snapshot().candidateAllocationActive());

        assertEquals(ProgressStatus.RECORDED,
                lifecycle.recordRequestProgress("request-1", true, CREATED_AT.plusSeconds(2)).status());
        assertEquals(ProgressStatus.RECORDED,
                lifecycle.recordRequestProgress("request-2", true, CREATED_AT.plusSeconds(3)).status());
        assertEquals(ProgressStatus.IDEMPOTENT,
                lifecycle.recordRequestProgress("request-2", true, CREATED_AT.plusSeconds(3)).status());
        assertEquals(ProgressStatus.CONFLICT,
                lifecycle.recordRequestProgress("request-2", false, CREATED_AT.plusSeconds(3)).status());
        var third = lifecycle.recordRequestProgress("request-3", false, CREATED_AT.plusSeconds(4));
        assertTrue(third.boundaryReached());
        assertEquals(3, third.snapshot().requestCount());
        assertEquals(2, third.snapshot().evidenceCount());
        assertEquals(ProgressStatus.DENIED,
                lifecycle.recordRequestProgress("request-4", true, CREATED_AT.plusSeconds(5)).status());
        assertEquals(CommandStatus.ILLEGAL,
                lifecycle.advance("command-time-regression", CREATED_AT.plusSeconds(3)).status());

        var holding = lifecycle.advance("command-hold", CREATED_AT.plusSeconds(5));
        assertEquals(EnterpriseLabExperimentState.HOLDING, holding.snapshot().state());
        assertEquals(CommandStatus.REJECTED,
                lifecycle.beginCompletion("command-early-complete", CREATED_AT.plusSeconds(6)).status());
        assertEquals(ProgressStatus.RECORDED,
                lifecycle.recordHoldDownCycle("cycle-1", CREATED_AT.plusSeconds(7)).status());
        assertEquals(ProgressStatus.IDEMPOTENT,
                lifecycle.recordHoldDownCycle("cycle-1", CREATED_AT.plusSeconds(7)).status());
        assertTrue(lifecycle.recordHoldDownCycle("cycle-2", CREATED_AT.plusSeconds(8)).boundaryReached());

        var completing = lifecycle.beginCompletion("command-completing", CREATED_AT.plusSeconds(9));
        assertEquals(EnterpriseLabExperimentState.COMPLETING, completing.snapshot().state());
        assertEquals(CommandStatus.REJECTED,
                lifecycle.confirmCompletion("command-unsafe-complete", applied, CREATED_AT.plusSeconds(10)).status());

        AllocationChangeReceipt restored = fixture.router.restoreBaseline("successful experiment complete");
        var completed = lifecycle.confirmCompletion(
                "command-completed", restored, CREATED_AT.plusSeconds(11));
        assertEquals(EnterpriseLabExperimentState.COMPLETED, completed.snapshot().state());
        assertTrue(completed.snapshot().terminal());
        assertFalse(completed.snapshot().candidateAllocationActive());
        assertEquals(CommandStatus.ILLEGAL,
                lifecycle.beginRollback("command-too-late", "terminal", CREATED_AT.plusSeconds(12)).status());

        assertEquals(
                List.of(
                        EnterpriseLabExperimentState.ARMED,
                        EnterpriseLabExperimentState.RUNNING,
                        EnterpriseLabExperimentState.HOLDING,
                        EnterpriseLabExperimentState.COMPLETING,
                        EnterpriseLabExperimentState.COMPLETED),
                lifecycle.transitionHistory().stream()
                        .map(EnterpriseLabExperimentTransition::toState)
                        .toList());
        assertFingerprintChain(lifecycle.transitionHistory());
    }

    @Test
    void rollbackRequiresAtomicSafeBaselineConfirmation() {
        Fixture fixture = fixture(true, 5, 2, 2, Duration.ofSeconds(30), CREATED_AT.plusSeconds(90));
        EnterpriseLabExperimentLifecycle lifecycle = armedAndRunning(fixture);
        AllocationChangeReceipt candidate = fixture.router.applyCandidate(fixture.decision, true);

        var rollingBack = lifecycle.beginRollback(
                "command-rollback", "operator stop requested", CREATED_AT.plusSeconds(2));
        assertEquals(EnterpriseLabExperimentState.ROLLING_BACK, rollingBack.snapshot().state());
        assertTrue(rollingBack.snapshot().candidateAllocationActive());
        assertEquals(CommandStatus.REJECTED,
                lifecycle.confirmRollback("command-unsafe-rollback", candidate, CREATED_AT.plusSeconds(3)).status());

        AllocationChangeReceipt restored = fixture.router.restoreBaseline("rollback threshold reached");
        var rolledBack = lifecycle.confirmRollback(
                "command-rolled-back", restored, CREATED_AT.plusSeconds(4));
        assertEquals(EnterpriseLabExperimentState.ROLLED_BACK, rolledBack.snapshot().state());
        assertTrue(rolledBack.snapshot().terminal());
        assertFalse(rolledBack.snapshot().candidateAllocationActive());
        assertEquals(fixture.configuration.baselineSnapshot().allocations(),
                restored.currentSnapshot().allocations());
        assertFingerprintChain(lifecycle.transitionHistory());
    }

    @Test
    void actualCandidateLoopbackRouteDrivesRequestAndEvidenceProgress() throws Exception {
        EnterpriseLabAdaptiveDecision decision = activeDecision();
        EnterpriseLabLoopbackAllocationSnapshot baseline = baseline(decision);
        try (LoopbackBackend blue = LoopbackBackend.start("blue");
                LoopbackBackend green = LoopbackBackend.start("green");
                LoopbackBackend orange = LoopbackBackend.start("orange")) {
            List<LoopbackBackend> backends = List.of(blue, green, orange);
            List<EnterpriseLabLoopbackTarget> targets = backends.stream()
                    .map(backend -> new EnterpriseLabLoopbackTarget(
                            SCENARIO, backend.backendId, backend.uri()))
                    .toList();
            EnterpriseLabLoopbackAllocationRouter router = new EnterpriseLabLoopbackAllocationRouter(
                    targets,
                    new EnterpriseLabLoopbackObservationIngress(Set.of("blue", "green", "orange")),
                    baseline.allocations());
            EnterpriseLabExperimentConfiguration configuration = new EnterpriseLabExperimentConfiguration(
                    EnterpriseLabExperimentConfiguration.SCHEMA_VERSION,
                    "experiment-actual-route",
                    decision,
                    baseline,
                    2,
                    Duration.ofSeconds(30),
                    1,
                    1,
                    EnterpriseLabExperimentRollbackPolicy.localLabDefaults(),
                    AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT,
                    true,
                    CREATED_AT,
                    CREATED_AT.plusSeconds(60));
            EnterpriseLabExperimentLifecycle lifecycle = new EnterpriseLabExperimentLifecycle();
            lifecycle.arm("arm-actual-route", configuration, CREATED_AT);
            lifecycle.start(
                    "start-actual-route",
                    router.applyCandidate(decision, true),
                    CREATED_AT.plusSeconds(1));

            var routed = router.route("real-lifecycle-request", 0, Duration.ofSeconds(1));
            var recorded = lifecycle.recordRequest(routed, CREATED_AT.plusSeconds(2));

            assertEquals(ProgressStatus.RECORDED, recorded.status());
            assertEquals(1, recorded.snapshot().requestCount());
            assertEquals(1, recorded.snapshot().evidenceCount());
            assertEquals(1, backends.stream().mapToInt(value -> value.requests.get()).sum());

            router.restoreBaseline("negative lifecycle route proof");
            var baselineRoute = router.route("baseline-lifecycle-request", 1, Duration.ofSeconds(1));
            assertEquals(ProgressStatus.DENIED,
                    lifecycle.recordRequest(baselineRoute, CREATED_AT.plusSeconds(3)).status());
            assertEquals(1, lifecycle.snapshot().requestCount());
        }
    }

    @Test
    void deterministicAdvanceUsesRequestDurationAndExpirationBoundaries() {
        Fixture requestBound = fixture(true, 2, 1, 1, Duration.ofSeconds(30), CREATED_AT.plusSeconds(90));
        EnterpriseLabExperimentLifecycle byRequest = armedAndRunning(requestBound);
        byRequest.recordRequestProgress("request-1", true, CREATED_AT.plusSeconds(2));
        assertEquals(CommandStatus.NO_CHANGE,
                byRequest.advance("advance-before-boundary", CREATED_AT.plusSeconds(3)).status());
        byRequest.recordRequestProgress("request-2", true, CREATED_AT.plusSeconds(4));
        assertEquals(EnterpriseLabExperimentState.HOLDING,
                byRequest.advance("advance-request-boundary", CREATED_AT.plusSeconds(5)).snapshot().state());

        Fixture durationBound = fixture(true, 20, 1, 1, Duration.ofSeconds(5), CREATED_AT.plusSeconds(20));
        EnterpriseLabExperimentLifecycle byDuration = armedAndRunning(durationBound);
        assertEquals(EnterpriseLabExperimentState.HOLDING,
                byDuration.advance("advance-duration-boundary", CREATED_AT.plusSeconds(6)).snapshot().state());
        assertEquals(EnterpriseLabExperimentState.ROLLING_BACK,
                byDuration.advance("advance-expiration", CREATED_AT.plusSeconds(20)).snapshot().state());

        Fixture notStarted = fixture(true, 20, 1, 1, Duration.ofSeconds(5), CREATED_AT.plusSeconds(20));
        EnterpriseLabExperimentLifecycle expiredArmed = new EnterpriseLabExperimentLifecycle();
        expiredArmed.arm("arm-before-expiry", notStarted.configuration, CREATED_AT);
        assertEquals(EnterpriseLabExperimentState.CANCELLED,
                expiredArmed.advance("advance-armed-expiry", CREATED_AT.plusSeconds(20)).snapshot().state());
    }

    @Test
    void holdDownAcceptsOnlyRemainingBoundedCandidateEvidenceUntilExpiration() {
        Fixture fixture = fixture(true, 3, 1, 2, Duration.ofSeconds(5), CREATED_AT.plusSeconds(20));
        EnterpriseLabExperimentLifecycle lifecycle = armedAndRunning(fixture);
        assertEquals(ProgressStatus.RECORDED,
                lifecycle.recordRequestProgress("hold-request-1", true, CREATED_AT.plusSeconds(2)).status());
        assertEquals(EnterpriseLabExperimentState.HOLDING,
                lifecycle.advance("advance-to-hold", CREATED_AT.plusSeconds(6)).snapshot().state());

        assertEquals(ProgressStatus.RECORDED,
                lifecycle.recordRequestProgress("hold-request-2", true, CREATED_AT.plusSeconds(7)).status());
        assertTrue(lifecycle.recordRequestProgress(
                "hold-request-3", true, CREATED_AT.plusSeconds(8)).boundaryReached());
        assertEquals(ProgressStatus.DENIED,
                lifecycle.recordRequestProgress("hold-request-over-limit", true, CREATED_AT.plusSeconds(9)).status());
        assertEquals(ProgressStatus.DENIED,
                lifecycle.recordRequestProgress("hold-request-expired", true, CREATED_AT.plusSeconds(20)).status());
        assertEquals(3, lifecycle.snapshot().requestCount());
        assertEquals(3, lifecycle.snapshot().evidenceCount());
        assertEquals(EnterpriseLabExperimentState.HOLDING, lifecycle.snapshot().state());
    }

    @Test
    void invalidAuthorizationGuardrailAndIllegalTerminalRequestsFailClosed() {
        Fixture unauthorized = fixture(false, 3, 1, 1, Duration.ofSeconds(10), CREATED_AT.plusSeconds(30));
        EnterpriseLabExperimentLifecycle rejected = new EnterpriseLabExperimentLifecycle();
        assertEquals(CommandStatus.REJECTED,
                rejected.arm("arm-unauthorized", unauthorized.configuration, CREATED_AT).status());
        assertEquals(EnterpriseLabExperimentState.REJECTED, rejected.snapshot().state());

        EnterpriseLabAdaptiveDecision deniedDecision = decisionService.decide(
                SCENARIO, "active-experiment", false, false, false);
        Fixture denied = fixture(
                deniedDecision, true, 3, 1, 1, Duration.ofSeconds(10), CREATED_AT.plusSeconds(30));
        EnterpriseLabExperimentLifecycle guardrailRejected = new EnterpriseLabExperimentLifecycle();
        assertEquals(EnterpriseLabExperimentState.REJECTED,
                guardrailRejected.arm("arm-denied", denied.configuration, CREATED_AT).snapshot().state());

        Fixture cancellable = fixture(true, 3, 1, 1, Duration.ofSeconds(10), CREATED_AT.plusSeconds(30));
        EnterpriseLabExperimentLifecycle cancelled = new EnterpriseLabExperimentLifecycle();
        cancelled.arm("arm-cancel", cancellable.configuration, CREATED_AT);
        assertEquals(EnterpriseLabExperimentState.CANCELLED,
                cancelled.cancel("cancel-command", "operator cancelled before start", CREATED_AT.plusSeconds(1))
                        .snapshot().state());

        Fixture fallible = fixture(true, 3, 1, 1, Duration.ofSeconds(10), CREATED_AT.plusSeconds(30));
        EnterpriseLabExperimentLifecycle failed = new EnterpriseLabExperimentLifecycle();
        failed.arm("arm-fail", fallible.configuration, CREATED_AT);
        assertEquals(EnterpriseLabExperimentState.FAILED,
                failed.fail("fail-command", "validation dependency unavailable", CREATED_AT.plusSeconds(1))
                        .snapshot().state());

        Fixture active = fixture(true, 3, 1, 1, Duration.ofSeconds(10), CREATED_AT.plusSeconds(30));
        EnterpriseLabExperimentLifecycle running = armedAndRunning(active);
        assertEquals(CommandStatus.ILLEGAL,
                running.cancel("cancel-active", "must restore baseline", CREATED_AT.plusSeconds(2)).status());
        assertEquals(CommandStatus.ILLEGAL,
                running.fail("fail-active", "must restore baseline", CREATED_AT.plusSeconds(2)).status());
        assertEquals(EnterpriseLabExperimentState.RUNNING, running.snapshot().state());
        assertTrue(running.snapshot().candidateAllocationActive());
    }

    @Test
    void malformedBoundsAndIdentifiersAreRejectedWithoutStateMutation() {
        Fixture fixture = fixture(true, 3, 1, 1, Duration.ofSeconds(10), CREATED_AT.plusSeconds(30));
        EnterpriseLabExperimentLifecycle lifecycle = new EnterpriseLabExperimentLifecycle();
        assertThrows(IllegalArgumentException.class,
                () -> lifecycle.arm("bad command", fixture.configuration, CREATED_AT));
        assertEquals(EnterpriseLabExperimentState.IDLE, lifecycle.snapshot().state());
        assertTrue(lifecycle.transitionHistory().isEmpty());

        lifecycle.arm("command-arm", fixture.configuration, CREATED_AT);
        assertThrows(IllegalArgumentException.class,
                () -> lifecycle.cancel("cancel", "x".repeat(257), CREATED_AT.plusSeconds(1)));
        assertEquals(EnterpriseLabExperimentState.ARMED, lifecycle.snapshot().state());
    }

    @Test
    void commandReplayLedgerFailsClosedAtItsHardBound() {
        Fixture fixture = fixture(true, 3, 1, 1, Duration.ofSeconds(10), CREATED_AT.plusSeconds(30));
        EnterpriseLabExperimentLifecycle lifecycle = new EnterpriseLabExperimentLifecycle();
        lifecycle.arm("command-arm", fixture.configuration, CREATED_AT);

        for (int index = 0; index < 63; index++) {
            assertEquals(CommandStatus.NO_CHANGE,
                    lifecycle.advance("bounded-advance-" + index, CREATED_AT.plusSeconds(1)).status());
        }
        var rejected = lifecycle.advance("bounded-advance-overflow", CREATED_AT.plusSeconds(1));

        assertEquals(CommandStatus.REJECTED, rejected.status());
        assertTrue(rejected.reason().contains("capacity"));
        assertEquals(EnterpriseLabExperimentState.ARMED, lifecycle.snapshot().state());
        assertEquals(1, lifecycle.transitionHistory().size());
    }

    private EnterpriseLabExperimentLifecycle armedAndRunning(Fixture fixture) {
        EnterpriseLabExperimentLifecycle lifecycle = new EnterpriseLabExperimentLifecycle();
        lifecycle.arm("command-arm", fixture.configuration, CREATED_AT);
        AllocationChangeReceipt applied = fixture.router.applyCandidate(fixture.decision, true);
        lifecycle.start("command-start", applied, CREATED_AT.plusSeconds(1));
        return lifecycle;
    }

    private Fixture fixture(
            boolean authorized,
            int maxRequests,
            int minEvidence,
            int holdCycles,
            Duration maxDuration,
            Instant expiresAt) {
        return fixture(activeDecision(), authorized, maxRequests, minEvidence, holdCycles, maxDuration, expiresAt);
    }

    private Fixture fixture(
            EnterpriseLabAdaptiveDecision decision,
            boolean authorized,
            int maxRequests,
            int minEvidence,
            int holdCycles,
            Duration maxDuration,
            Instant expiresAt) {
        EnterpriseLabLoopbackAllocationSnapshot baseline = baseline(decision);
        EnterpriseLabLoopbackAllocationRouter router = router(decision.scenarioId(), baseline.allocations());
        EnterpriseLabExperimentConfiguration configuration = new EnterpriseLabExperimentConfiguration(
                EnterpriseLabExperimentConfiguration.SCHEMA_VERSION,
                "experiment-lifecycle-test",
                decision,
                baseline,
                maxRequests,
                maxDuration,
                minEvidence,
                holdCycles,
                EnterpriseLabExperimentRollbackPolicy.localLabDefaults(),
                AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT,
                authorized,
                CREATED_AT,
                expiresAt);
        return new Fixture(decision, router, configuration);
    }

    private EnterpriseLabAdaptiveDecision activeDecision() {
        return decisionService.decide(SCENARIO, "active-experiment", true, false, false);
    }

    private static EnterpriseLabLoopbackAllocationSnapshot baseline(EnterpriseLabAdaptiveDecision decision) {
        Map<String, Double> allocation = decision.decision().guardrailDecision().baselineAllocations();
        return EnterpriseLabLoopbackAllocationSnapshot.normalized(
                decision.scenarioId(), 0, "recorded-baseline", Kind.BASELINE,
                new ArrayList<>(allocation.keySet()), allocation);
    }

    private static EnterpriseLabLoopbackAllocationRouter router(
            String scenarioId,
            Map<String, Double> baseline) {
        List<EnterpriseLabLoopbackTarget> targets = List.of(
                target(scenarioId, "blue", 49111),
                target(scenarioId, "green", 49112),
                target(scenarioId, "orange", 49113));
        return new EnterpriseLabLoopbackAllocationRouter(
                targets,
                new EnterpriseLabLoopbackObservationIngress(Set.of("blue", "green", "orange")),
                baseline);
    }

    private static EnterpriseLabLoopbackTarget target(String scenarioId, String backendId, int port) {
        return new EnterpriseLabLoopbackTarget(
                scenarioId,
                backendId,
                URI.create("http://127.0.0.1:" + port + "/enterprise-lab/lifecycle"));
    }

    private static void assertFingerprintChain(List<EnterpriseLabExperimentTransition> transitions) {
        assertFalse(transitions.isEmpty());
        assertEquals(EnterpriseLabExperimentTransition.GENESIS_FINGERPRINT,
                transitions.get(0).previousFingerprint());
        for (int index = 0; index < transitions.size(); index++) {
            EnterpriseLabExperimentTransition transition = transitions.get(index);
            assertEquals(index + 1L, transition.sequence());
            assertTrue(transition.contentFingerprint().matches("[0-9a-f]{64}"));
            if (index > 0) {
                assertEquals(transitions.get(index - 1).contentFingerprint(), transition.previousFingerprint());
                assertNotEquals(transitions.get(index - 1).contentFingerprint(), transition.contentFingerprint());
            }
        }
        assertThrows(UnsupportedOperationException.class, () -> transitions.add(transitions.get(0)));
    }

    private record Fixture(
            EnterpriseLabAdaptiveDecision decision,
            EnterpriseLabLoopbackAllocationRouter router,
            EnterpriseLabExperimentConfiguration configuration) {
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
            server.createContext("/enterprise-lab/lifecycle", exchange -> {
                backend.requests.incrementAndGet();
                respond(exchange, 204);
            });
            server.setExecutor(null);
            server.start();
            return backend;
        }

        URI uri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                    + "/enterprise-lab/lifecycle");
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
