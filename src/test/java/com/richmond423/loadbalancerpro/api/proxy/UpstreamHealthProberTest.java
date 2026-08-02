package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

class UpstreamHealthProberTest {

    @Test
    void alternatingResultsDoNotFlapAndRiseFallThresholdsControlTransitions() throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        AtomicReference<Mode> mode = new AtomicReference<>(Mode.ALTERNATING);
        AtomicInteger probes = new AtomicInteger();
        List<UpstreamHealthProber.HealthSnapshot> snapshots = new CopyOnWriteArrayList<>();
        UpstreamHealthProber prober = new UpstreamHealthProber(
                scheduler,
                target -> {
                    int probe = probes.incrementAndGet();
                    boolean successful = switch (mode.get()) {
                        case ALTERNATING -> probe % 2 == 0;
                        case PASS -> true;
                        case FAIL -> false;
                    };
                    return new UpstreamHealthProber.ProbeResult(
                            successful,
                            successful ? 200 : 503,
                            successful ? "healthy" : "unhealthy");
                },
                Clock.systemUTC(),
                (target, successful, snapshot) -> snapshots.add(snapshot),
                interval -> 0);

        try {
            prober.configure(
                    List.of(new UpstreamHealthProber.Target(
                            "backend", URI.create("http://127.0.0.1:18081/health"), Duration.ofSeconds(1))),
                    Duration.ofMillis(5),
                    2,
                    3);

            await(() -> snapshots.size() >= 6, Duration.ofSeconds(2));
            assertTrue(snapshots.stream().limit(6).allMatch(UpstreamHealthProber.HealthSnapshot::healthy),
                    "alternating single results must not cross the configured fall threshold");

            mode.set(Mode.FAIL);
            int failStart = snapshots.size();
            await(() -> snapshots.stream().skip(failStart).anyMatch(snapshot -> !snapshot.healthy()),
                    Duration.ofSeconds(2));
            UpstreamHealthProber.HealthSnapshot fallen = snapshots.stream()
                    .skip(failStart)
                    .filter(snapshot -> !snapshot.healthy())
                    .findFirst()
                    .orElseThrow();
            assertTrue(fallen.consecutiveFailures() >= 3);

            mode.set(Mode.PASS);
            int passStart = snapshots.size();
            await(() -> snapshots.stream().skip(passStart)
                            .anyMatch(snapshot -> snapshot.consecutiveSuccesses() == 1),
                    Duration.ofSeconds(2));
            UpstreamHealthProber.HealthSnapshot firstSuccess = snapshots.stream()
                    .skip(passStart)
                    .filter(snapshot -> snapshot.consecutiveSuccesses() == 1)
                    .findFirst()
                    .orElseThrow();
            assertFalse(firstSuccess.healthy(),
                    "one successful probe must not immediately recover a fallen upstream");
            await(() -> snapshots.stream().skip(passStart).anyMatch(snapshot ->
                            snapshot.healthy() && snapshot.consecutiveSuccesses() >= 2),
                    Duration.ofSeconds(2));
        } finally {
            prober.close();
        }
    }

    @Test
    void reconfigureDiscardsAnInFlightResultFromThePreviousGeneration() throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch oldProbeStarted = new CountDownLatch(1);
        CountDownLatch releaseOldProbe = new CountDownLatch(1);
        List<String> completedTargets = new CopyOnWriteArrayList<>();
        UpstreamHealthProber prober = new UpstreamHealthProber(
                scheduler,
                target -> {
                    if ("old".equals(target.id())) {
                        oldProbeStarted.countDown();
                        boolean interrupted = false;
                        while (true) {
                            try {
                                releaseOldProbe.await();
                                break;
                            } catch (InterruptedException exception) {
                                interrupted = true;
                            }
                        }
                        if (interrupted) {
                            Thread.currentThread().interrupt();
                        }
                        return new UpstreamHealthProber.ProbeResult(false, 503, "stale failure");
                    }
                    return new UpstreamHealthProber.ProbeResult(true, 200, "current success");
                },
                Clock.systemUTC(),
                (target, successful, snapshot) -> completedTargets.add(target.id()),
                interval -> 0);

        try {
            prober.configure(
                    List.of(target("old")), Duration.ofHours(1), 1, 1);
            assertTrue(oldProbeStarted.await(2, TimeUnit.SECONDS), "old probe did not start");

            prober.configure(
                    List.of(target("current")), Duration.ofHours(1), 1, 1);
            releaseOldProbe.countDown();

            await(() -> completedTargets.contains("current"), Duration.ofSeconds(2));
            assertEquals(List.of("current"), completedTargets,
                    "a cancelled configuration must not publish a stale in-flight result");
            assertTrue(prober.snapshot("old", 0).isEmpty());
            assertTrue(prober.snapshot("current", 1).isEmpty(),
                    "a snapshot must not cross the active configuration generation");
            assertTrue(prober.snapshot("current", 0).orElseThrow().healthy());
        } finally {
            releaseOldProbe.countDown();
            prober.close();
        }
    }

    @Test
    void reconfigureCarriesHealthOnlyForAnUnchangedTargetAndPolicy() throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger probes = new AtomicInteger();
        CountDownLatch replacementProbeStarted = new CountDownLatch(1);
        CountDownLatch releaseReplacementProbe = new CountDownLatch(1);
        UpstreamHealthProber prober = new UpstreamHealthProber(
                scheduler,
                target -> {
                    if (probes.incrementAndGet() > 1) {
                        replacementProbeStarted.countDown();
                        releaseReplacementProbe.await();
                    }
                    return new UpstreamHealthProber.ProbeResult(false, 503, "unhealthy");
                },
                Clock.systemUTC(),
                (target, successful, snapshot) -> { },
                interval -> 0);

        try {
            prober.configure(List.of(target("backend", 1)), Duration.ofHours(1), 1, 1);
            await(() -> prober.snapshot("backend", 1)
                            .map(snapshot -> !snapshot.healthy())
                            .orElse(false),
                    Duration.ofSeconds(2));
            UpstreamHealthProber.HealthSnapshot before = prober.snapshot("backend", 1).orElseThrow();

            prober.configure(List.of(target("backend", 2)), Duration.ofHours(1), 1, 1);
            assertTrue(replacementProbeStarted.await(2, TimeUnit.SECONDS));

            UpstreamHealthProber.HealthSnapshot after = prober.snapshot("backend", 2).orElseThrow();
            assertFalse(after.healthy());
            assertEquals(before.consecutiveFailures(), after.consecutiveFailures());
            assertEquals(before.checkedAt(), after.checkedAt());
            assertEquals(before, prober.snapshot("backend", 1).orElseThrow(),
                    "the prior generation may finish requests against the same unchanged target");

            prober.configure(List.of(target("backend", 3)), Duration.ofHours(1), 1, 1);
            assertEquals(before, prober.snapshot("backend", 1).orElseThrow(),
                    "overlapping requests may outlive more than one unchanged reload");
        } finally {
            releaseReplacementProbe.countDown();
            prober.close();
        }
    }

    @Test
    void lifecycleStopCancelsTasksAndRejectsFurtherConfiguration() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        UpstreamHealthProber prober = new UpstreamHealthProber(
                scheduler,
                target -> new UpstreamHealthProber.ProbeResult(true, 200, "healthy"),
                Clock.systemUTC(),
                (target, successful, snapshot) -> { },
                interval -> 0);

        assertTrue(prober.isRunning());
        prober.stop();

        assertFalse(prober.isRunning());
        assertTrue(scheduler.isShutdown());
        assertThrows(IllegalStateException.class,
                () -> prober.configure(List.of(target("backend")), Duration.ofSeconds(1), 1, 1));
    }

    private static UpstreamHealthProber.Target target(String id) {
        return new UpstreamHealthProber.Target(
                id, URI.create("http://127.0.0.1:18081/health"), Duration.ofSeconds(1));
    }

    private static UpstreamHealthProber.Target target(String id, long generation) {
        return new UpstreamHealthProber.Target(
                id, URI.create("http://127.0.0.1:18081/health"), Duration.ofSeconds(1), generation);
    }

    private static void await(BooleanSupplier condition, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertTrue(condition.getAsBoolean(), "condition did not converge within " + timeout);
    }

    private enum Mode {
        ALTERNATING,
        PASS,
        FAIL
    }
}
