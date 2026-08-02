package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.richmond423.loadbalancerpro.core.LaseShadowAdvisor;
import com.richmond423.loadbalancerpro.core.LaseShadowDispatchSnapshot;
import com.richmond423.loadbalancerpro.core.LaseShadowEventLog;
import com.richmond423.loadbalancerpro.core.LiveRoutingShadowObservation;
import com.richmond423.loadbalancerpro.core.ServerStateVector;

class LaseShadowRuntimeTest {
    private static final Instant NOW = Instant.parse("2026-07-31T12:00:00Z");

    @Test
    void lifecycleStopIsIdempotentAndTerminal() {
        LaseShadowRuntime runtime = LaseShadowRuntime.disabled();

        assertTrue(runtime.isRunning());
        runtime.stop();
        runtime.stop();

        assertFalse(runtime.isRunning());
        assertThrows(IllegalStateException.class, runtime::start);
    }

    @Test
    void liveDispatchIsNonBlockingBoundedAndObservable() throws Exception {
        CountDownLatch workerStarted = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        LaseShadowEventLog eventLog = new LaseShadowEventLog();
        LaseShadowAdvisor advisor = new LaseShadowAdvisor(true, eventLog);
        LaseShadowRuntime runtime = new LaseShadowRuntime(
                true,
                eventLog,
                advisor,
                observation -> {
                    workerStarted.countDown();
                    try {
                        releaseWorker.await();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                },
                2);
        try {
            LiveRoutingShadowObservation observation = observation();
            assertTrue(runtime.submitLiveRouting(observation));
            assertTrue(workerStarted.await(1, TimeUnit.SECONDS));

            assertTimeout(Duration.ofMillis(500), () -> {
                assertTrue(runtime.submitLiveRouting(observation));
                assertTrue(runtime.submitLiveRouting(observation));
                assertFalse(runtime.submitLiveRouting(observation));
            });

            LaseShadowDispatchSnapshot queued = runtime.snapshot().liveProxyDispatch();
            assertEquals(2, queued.queueCapacity());
            assertEquals(2, queued.queuedEvaluations());
            assertEquals(1, queued.activeEvaluations());
            assertEquals(3, queued.totalAccepted());
            assertEquals(0, queued.totalCompleted());
            assertEquals(1, queued.totalDropped());

            releaseWorker.countDown();
            awaitCompleted(runtime, 3);
            LaseShadowDispatchSnapshot completed = runtime.snapshot().liveProxyDispatch();
            assertEquals(3, completed.totalCompleted());
            assertEquals(0, completed.queuedEvaluations());
        } finally {
            releaseWorker.countDown();
            runtime.close();
        }
    }

    private static void awaitCompleted(LaseShadowRuntime runtime, long expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (runtime.snapshot().liveProxyDispatch().totalCompleted() < expected
                && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(expected, runtime.snapshot().liveProxyDispatch().totalCompleted());
    }

    private static LiveRoutingShadowObservation observation() {
        ServerStateVector candidate = new ServerStateVector(
                "upstream-1", true, 1, 10.0, 10.0, 20.0, 30.0, 40.0, 0.0, 0, NOW);
        return new LiveRoutingShadowObservation(
                "proxy-decision-00000001",
                NOW,
                "checkout",
                "ROUND_ROBIN",
                "strategy",
                "upstream-1",
                List.of(candidate),
                10,
                1);
    }
}
