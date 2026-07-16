package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.ServerObservationSource;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.ChangeStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabLoopbackAllocationRouterTest {
    private static final String SCENARIO = "tail-latency-pressure";
    private static final Set<String> BACKEND_IDS = Set.of("blue", "green", "orange");
    private final EnterpriseLabAdaptiveDecisionService decisionService = new EnterpriseLabAdaptiveDecisionService();

    @Test
    void approvedCandidateIsAppliedIdempotentlyAndBaselineIsRestored() {
        EnterpriseLabAdaptiveDecision candidate = activeDecision(SCENARIO);
        EnterpriseLabLoopbackAllocationRouter router = router(
                targets(SCENARIO), candidate.decision().request().baselineAllocations());

        var applied = router.applyCandidate(candidate, true);

        assertEquals(ChangeStatus.APPLIED, applied.status());
        assertTrue(applied.trafficActionPerformed());
        assertEquals(Kind.CANDIDATE, applied.currentSnapshot().kind());
        assertEquals(candidate.decision().decisionId(), applied.currentSnapshot().sourceDecisionId());
        assertTrue(same(candidate.decision().effectiveAllocations(), applied.currentSnapshot().allocations()));
        assertNotEquals(applied.baselineSnapshot().allocations(), applied.currentSnapshot().allocations());

        var repeated = router.applyCandidate(candidate, true);
        assertEquals(ChangeStatus.NO_CHANGE, repeated.status());
        assertFalse(repeated.trafficActionPerformed());
        assertEquals(applied.currentSnapshot(), repeated.currentSnapshot());

        var restored = router.restoreBaseline("focused test completed");
        assertEquals(ChangeStatus.RESTORED, restored.status());
        assertTrue(restored.trafficActionPerformed());
        assertEquals(Kind.RESTORED_BASELINE, restored.currentSnapshot().kind());
        assertEquals(router.baselineSnapshot().allocations(), restored.currentSnapshot().allocations());
        assertEquals(2, restored.currentSnapshot().revision());

        var repeatedRestore = router.restoreBaseline("already safe");
        assertEquals(ChangeStatus.NO_CHANGE, repeatedRestore.status());
        assertFalse(repeatedRestore.trafficActionPerformed());
    }

    @Test
    void disabledDeniedUnsafeMismatchedAndInvalidBaselineCandidatesFailClosed() {
        EnterpriseLabAdaptiveDecision candidate = activeDecision(SCENARIO);
        EnterpriseLabLoopbackAllocationRouter router = router(
                targets(SCENARIO), candidate.decision().request().baselineAllocations());
        var baseline = router.currentSnapshot();

        assertDenied(router.applyCandidate(candidate, false), baseline);
        assertDenied(router.applyCandidate(null, true), baseline);
        assertDenied(router.applyCandidate(
                decisionService.decide(SCENARIO, "recommend", false, false, false), true), baseline);
        assertDenied(router.applyCandidate(
                decisionService.decide(SCENARIO, "active-experiment", false, false, false), true), baseline);
        assertDenied(router.applyCandidate(activeDecision("normal-balanced-load"), true), baseline);

        EnterpriseLabAdaptiveDecision stale = activeDecision("stale-signal");
        EnterpriseLabLoopbackAllocationRouter staleRouter = router(
                targets("stale-signal"), stale.decision().request().baselineAllocations());
        assertDenied(staleRouter.applyCandidate(stale, true), staleRouter.baselineSnapshot());

        EnterpriseLabLoopbackAllocationRouter wrongBaseline = router(
                targets(SCENARIO), Map.of("blue", 0.5, "green", 0.25, "orange", 0.25));
        assertDenied(wrongBaseline.applyCandidate(candidate, true), wrongBaseline.baselineSnapshot());
    }

    @Test
    void targetSetMustExactlyMatchOneApprovedCatalogScenario() {
        EnterpriseLabAdaptiveDecision decision = activeDecision(SCENARIO);
        Map<String, Double> baseline = decision.decision().request().baselineAllocations();

        assertThrows(IllegalArgumentException.class,
                () -> router(List.of(target(SCENARIO, "blue", 49101)), baseline));
        assertThrows(IllegalArgumentException.class,
                () -> router(List.of(
                        target(SCENARIO, "blue", 49101),
                        target(SCENARIO, "green", 49102),
                        target(SCENARIO, "rogue", 49103)), baseline));
        assertThrows(IllegalArgumentException.class,
                () -> router(List.of(
                        target(SCENARIO, "blue", 49101),
                        target(SCENARIO, "green", 49102),
                        target("normal-balanced-load", "orange", 49103)), baseline));
    }

    @Test
    void actualCandidateRoutingUsesSelectedLoopbackBackendsAndRecordsOutcomes() throws Exception {
        EnterpriseLabAdaptiveDecision candidate = activeDecision(SCENARIO);
        try (LoopbackBackend blue = LoopbackBackend.start("blue");
                LoopbackBackend green = LoopbackBackend.start("green");
                LoopbackBackend orange = LoopbackBackend.start("orange")) {
            List<LoopbackBackend> backends = List.of(blue, green, orange);
            List<EnterpriseLabLoopbackTarget> targets = backends.stream()
                    .map(backend -> new EnterpriseLabLoopbackTarget(SCENARIO, backend.backendId, backend.uri()))
                    .toList();
            EnterpriseLabLoopbackObservationIngress ingress =
                    new EnterpriseLabLoopbackObservationIngress(BACKEND_IDS);
            EnterpriseLabLoopbackAllocationRouter router = new EnterpriseLabLoopbackAllocationRouter(
                    targets, ingress, candidate.decision().request().baselineAllocations());
            assertEquals(ChangeStatus.APPLIED, router.applyCandidate(candidate, true).status());

            Map<String, Integer> expected = new LinkedHashMap<>();
            BACKEND_IDS.forEach(backendId -> expected.put(backendId, 0));
            for (int ordinal = 0; ordinal < 60; ordinal++) {
                String selected = router.currentSnapshot().selectBackend(ordinal);
                expected.computeIfPresent(selected, (backendId, count) -> count + 1);
                var execution = router.route("allocated-request-" + ordinal, ordinal, Duration.ofSeconds(1));
                assertEquals(selected, execution.selectedBackendId());
                assertTrue(execution.requestExecution().requestSent());
                assertEquals(204, execution.requestExecution().responseStatusCode().orElseThrow());
                assertTrue(execution.candidateAllocationUsed());
                assertTrue(execution.trafficActionPerformed());
            }

            for (LoopbackBackend backend : backends) {
                assertEquals(expected.get(backend.backendId).intValue(), backend.requests.get());
                assertEquals(expected.get(backend.backendId).intValue(), ingress.observations(backend.backendId).size());
                assertTrue(ingress.observations(backend.backendId).stream()
                        .allMatch(value -> value.source() == ServerObservationSource.ENTERPRISE_LAB_LOOPBACK));
            }

            router.restoreBaseline("integration proof complete");
            var safeRoute = router.route("baseline-request", 61, Duration.ofSeconds(1));
            assertFalse(safeRoute.candidateAllocationUsed());
            assertFalse(safeRoute.trafficActionPerformed());
            assertEquals(Kind.RESTORED_BASELINE, safeRoute.allocationSnapshot().kind());
        }
    }

    @Test
    void concurrentRoutingReadersObserveOnlyCompleteBaselineOrCandidateSnapshots() throws Exception {
        EnterpriseLabAdaptiveDecision candidate = activeDecision(SCENARIO);
        EnterpriseLabLoopbackAllocationRouter router = router(
                targets(SCENARIO), candidate.decision().request().baselineAllocations());
        Map<String, Double> baseline = router.baselineSnapshot().allocations();
        Map<String, Double> proposed = candidate.decision().effectiveAllocations();
        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        threads.add(thread("allocation-writer", ready, start, failure, () -> {
            for (int index = 0; index < 250; index++) {
                router.applyCandidate(candidate, true);
                router.restoreBaseline("concurrency-cycle-" + index);
            }
        }));
        for (int reader = 0; reader < 3; reader++) {
            threads.add(thread("allocation-reader-" + reader, ready, start, failure, () -> {
                for (int index = 0; index < 2_000; index++) {
                    EnterpriseLabLoopbackAllocationSnapshot snapshot = router.currentSnapshot();
                    Map<String, Double> observed = snapshot.allocations();
                    if (!same(observed, baseline) && !same(observed, proposed)) {
                        throw new AssertionError("reader observed a partial allocation: " + observed);
                    }
                    if (!observed.keySet().equals(BACKEND_IDS)
                            || Math.abs(total(observed) - 1.0) > 0.000000001) {
                        throw new AssertionError("reader observed an invalid allocation: " + observed);
                    }
                    String selectedBackend = snapshot.selectBackend(index);
                    if (observed.get(selectedBackend) <= 0.0) {
                        throw new AssertionError("routing selected a zero-share backend: " + selectedBackend);
                    }
                }
            }));
        }

        threads.forEach(Thread::start);
        assertTrue(ready.await(2, TimeUnit.SECONDS));
        start.countDown();
        for (Thread thread : threads) {
            thread.join(10_000);
            assertFalse(thread.isAlive(), "bounded concurrency test thread must complete");
        }
        assertNull(failure.get(), () -> "concurrency failure: " + failure.get());
    }

    private EnterpriseLabAdaptiveDecision activeDecision(String scenarioId) {
        return decisionService.decide(scenarioId, "active-experiment", true, false, false);
    }

    private static void assertDenied(
            EnterpriseLabLoopbackAllocationRouter.AllocationChangeReceipt receipt,
            EnterpriseLabLoopbackAllocationSnapshot expected) {
        assertEquals(ChangeStatus.DENIED, receipt.status());
        assertFalse(receipt.trafficActionPerformed());
        assertEquals(expected, receipt.previousSnapshot());
        assertEquals(expected, receipt.currentSnapshot());
    }

    private static EnterpriseLabLoopbackAllocationRouter router(
            Collection<EnterpriseLabLoopbackTarget> targets,
            Map<String, Double> baseline) {
        Set<String> backendIds = targets.stream().map(EnterpriseLabLoopbackTarget::backendId).collect(
                java.util.stream.Collectors.toSet());
        return new EnterpriseLabLoopbackAllocationRouter(
                targets, new EnterpriseLabLoopbackObservationIngress(backendIds), baseline);
    }

    private static List<EnterpriseLabLoopbackTarget> targets(String scenarioId) {
        return List.of(
                target(scenarioId, "blue", 49101),
                target(scenarioId, "green", 49102),
                target(scenarioId, "orange", 49103));
    }

    private static EnterpriseLabLoopbackTarget target(String scenarioId, String backendId, int port) {
        return new EnterpriseLabLoopbackTarget(
                scenarioId,
                backendId,
                URI.create("http://127.0.0.1:" + port + "/enterprise-lab/probe"));
    }

    private static Thread thread(
            String name,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicReference<Throwable> failure,
            ThrowingRunnable action) {
        return new Thread(() -> {
            ready.countDown();
            try {
                start.await();
                action.run();
            } catch (Throwable throwable) {
                failure.compareAndSet(null, throwable);
            }
        }, name);
    }

    private static boolean same(Map<String, Double> first, Map<String, Double> second) {
        return first.keySet().equals(second.keySet()) && first.keySet().stream()
                .allMatch(backendId -> Math.abs(first.get(backendId) - second.get(backendId)) <= 0.000000001);
    }

    private static double total(Map<String, Double> allocations) {
        return allocations.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
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
            server.createContext("/enterprise-lab/probe", exchange -> {
                backend.requests.incrementAndGet();
                respond(exchange, 204);
            });
            server.setExecutor(null);
            server.start();
            return backend;
        }

        URI uri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                    + "/enterprise-lab/probe");
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
