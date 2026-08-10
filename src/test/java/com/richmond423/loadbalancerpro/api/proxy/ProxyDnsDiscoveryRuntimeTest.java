package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class ProxyDnsDiscoveryRuntimeTest {
    private static final ProxyDnsDiscoverySettings SETTINGS = new ProxyDnsDiscoverySettings(
            Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofMillis(500), 2);

    @Test
    void resolvesOnlyAfterSchedulerAndLookupExecutorsRun() throws Exception {
        Harness harness = new Harness(name -> List.of(literal(127, 0, 0, 1)));
        try {
            harness.runtime.replace(1, List.of(registration("backend", "service.example", false)));
            assertEquals(0, harness.resolverCalls.get());
            assertEquals(0, harness.runtime.snapshot().generation());

            harness.scheduler.runReady();
            assertEquals(0, harness.resolverCalls.get());
            assertEquals(1, harness.lookup.queuedTaskCount());

            harness.lookup.runNext();
            assertEquals(1, harness.resolverCalls.get());
            assertTrue(harness.runtime.snapshot().membersByLogicalId().get("backend").isEmpty());

            harness.scheduler.runReady();
            ProxyDnsDiscoveryRuntime.Snapshot snapshot = harness.runtime.snapshot();
            assertEquals(1, snapshot.generation());
            assertEquals(List.of("127.0.0.1"), snapshot.membersByLogicalId().get("backend").stream()
                    .map(ProxyDnsDiscovery.Member::address).toList());
            assertEquals(ProxyDnsDiscoveryRuntime.Outcome.SUCCESS,
                    snapshot.statusByLogicalId().get("backend").outcome());
        } finally {
            harness.close();
        }
    }

    @Test
    void timeoutDoesNotStartASecondLookupForTheSameName() throws Exception {
        Harness harness = new Harness(name -> List.of(literal(127, 0, 0, 1)));
        try {
            harness.runtime.replace(1, List.of(registration("backend", "service.example", false)));
            harness.scheduler.runReady();
            assertEquals(1, harness.lookup.queuedTaskCount());

            harness.scheduler.advance(Duration.ofMillis(500));
            assertEquals(ProxyDnsDiscoveryRuntime.Outcome.TIMEOUT,
                    harness.runtime.snapshot().statusByLogicalId().get("backend").outcome());
            harness.scheduler.advance(Duration.ofMillis(500));
            assertEquals(1, harness.lookup.queuedTaskCount(), "timed-out work is still the one in-flight lookup");

            harness.lookup.runNext();
            harness.scheduler.runReady();
            assertEquals(1, harness.lookup.queuedTaskCount(), "completion starts one fresh lookup, not a fan-out");
        } finally {
            harness.close();
        }
    }

    @Test
    void blockedLookupForOneNameDoesNotStopAnotherNameOrStaleScheduler() throws Exception {
        Harness harness = new Harness(name -> List.of(literal(
                127, 0, 0, name.startsWith("a") ? 1 : 2)));
        try {
            harness.runtime.replace(1, List.of(
                    registration("a-backend", "a.service.example", false),
                    registration("b-backend", "b.service.example", false)));
            harness.scheduler.runReady();
            assertEquals(2, harness.lookup.queuedTaskCount());

            harness.lookup.runLast();
            harness.scheduler.runReady();

            assertTrue(harness.runtime.snapshot().membersByLogicalId().get("a-backend").isEmpty());
            assertEquals("127.0.0.2",
                    harness.runtime.snapshot().membersByLogicalId().get("b-backend").get(0).address());
            assertTrue(harness.runtime.snapshot().statusByLogicalId().get("a-backend").lookupInFlight());
        } finally {
            harness.close();
        }
    }

    @Test
    void retainsLastGoodAcrossFailureAndEmptyAnswerUntilMonotonicStaleDeadline() throws Exception {
        AtomicReference<Mode> mode = new AtomicReference<>(Mode.SUCCESS);
        Harness harness = new Harness(name -> switch (mode.get()) {
            case SUCCESS -> List.of(literal(127, 0, 0, 1));
            case EMPTY -> List.of();
            case FAILURE -> throw new IllegalStateException("resolver detail must not escape");
        });
        try {
            harness.runtime.replace(1, List.of(registration("backend", "service.example", false)));
            harness.resolveNext();
            assertEquals(1, harness.runtime.snapshot().membersByLogicalId().get("backend").size());

            mode.set(Mode.FAILURE);
            harness.scheduler.advance(Duration.ofSeconds(1));
            harness.resolveNext();
            assertEquals(ProxyDnsDiscoveryRuntime.Outcome.FAILURE,
                    harness.runtime.snapshot().statusByLogicalId().get("backend").outcome());
            assertEquals(1, harness.runtime.snapshot().membersByLogicalId().get("backend").size());

            mode.set(Mode.EMPTY);
            harness.scheduler.advance(Duration.ofSeconds(1));
            harness.resolveNext();
            assertEquals(ProxyDnsDiscoveryRuntime.Outcome.EMPTY,
                    harness.runtime.snapshot().statusByLogicalId().get("backend").outcome());
            assertEquals(1, harness.runtime.snapshot().membersByLogicalId().get("backend").size());

            harness.scheduler.advance(Duration.ofSeconds(1));
            assertTrue(harness.runtime.snapshot().membersByLogicalId().get("backend").isEmpty());
            assertEquals(ProxyDnsDiscoveryRuntime.Outcome.STALE,
                    harness.runtime.snapshot().statusByLogicalId().get("backend").outcome());
        } finally {
            harness.close();
        }
    }

    @Test
    void suppressesCompletionFromAPriorConfigurationGeneration() throws Exception {
        AtomicReference<InetAddress> answer = new AtomicReference<>(literal(127, 0, 0, 1));
        Harness harness = new Harness(name -> List.of(answer.get()));
        try {
            harness.runtime.replace(1, List.of(registration("backend", "service.example", false)));
            harness.scheduler.runReady();
            harness.runtime.replace(2, List.of(registration("backend", "service.example", false)));
            harness.scheduler.runReady();

            harness.lookup.runNext();
            harness.scheduler.runReady();
            assertTrue(harness.runtime.snapshot().membersByLogicalId().get("backend").isEmpty());
            assertEquals(1, harness.lookup.queuedTaskCount());

            answer.set(literal(127, 0, 0, 2));
            harness.lookup.runNext();
            harness.scheduler.runReady();
            assertEquals(2, harness.runtime.snapshot().generation());
            assertEquals("127.0.0.2",
                    harness.runtime.snapshot().membersByLogicalId().get("backend").get(0).address());
        } finally {
            harness.close();
        }
    }

    @Test
    void overflowingRefreshRetainsPriorSnapshotWithoutPartialPublication() throws Exception {
        AtomicReference<List<InetAddress>> answers = new AtomicReference<>(List.of(literal(10, 0, 0, 1)));
        Harness harness = new Harness(name -> answers.get());
        try {
            harness.runtime.replace(1, List.of(registration("backend", "service.example", true)));
            harness.resolveNext();
            assertEquals(1, harness.runtime.snapshot().membersByLogicalId().get("backend").size());

            List<InetAddress> overflow = new ArrayList<>();
            for (int value = 1; value <= 33; value++) {
                overflow.add(literal(10, 0, 1, value));
            }
            answers.set(overflow);
            harness.scheduler.advance(Duration.ofSeconds(1));
            harness.resolveNext();

            assertEquals(ProxyDnsDiscoveryRuntime.Outcome.INVALID_ANSWER,
                    harness.runtime.snapshot().statusByLogicalId().get("backend").outcome());
            assertEquals("10.0.0.1",
                    harness.runtime.snapshot().membersByLogicalId().get("backend").get(0).address());
        } finally {
            harness.close();
        }
    }

    @Test
    void refreshAttemptsRespectTheConfiguredFloor() throws Exception {
        Harness harness = new Harness(name -> List.of(literal(127, 0, 0, 1)));
        try {
            harness.runtime.replace(1, List.of(registration("backend", "service.example", false)));
            harness.resolveNext();
            assertEquals(1, harness.resolverCalls.get());

            harness.scheduler.advance(Duration.ofMillis(999));
            assertEquals(0, harness.lookup.queuedTaskCount());
            harness.scheduler.advance(Duration.ofMillis(1));
            assertEquals(1, harness.lookup.queuedTaskCount());
        } finally {
            harness.close();
        }
    }

    @Test
    void productionExecutorsUseSeparateDaemonThreadsAndShutDownCleanly() throws Exception {
        CountDownLatch lookupStarted = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        AtomicReference<String> callbackThread = new AtomicReference<>();
        ProxyDnsDiscoveryRuntime runtime = new ProxyDnsDiscoveryRuntime(
                SETTINGS,
                name -> {
                    lookupStarted.countDown();
                    try {
                        new CountDownLatch(1).await();
                    } catch (InterruptedException expected) {
                        interrupted.countDown();
                        Thread.currentThread().interrupt();
                    }
                    return List.of();
                },
                snapshot -> callbackThread.set(Thread.currentThread().getName()));

        runtime.replace(1, List.of(registration("backend", "service.example", false)));
        assertTrue(lookupStarted.await(2, TimeUnit.SECONDS));
        assertTrue(waitUntil(() -> callbackThread.get() != null, Duration.ofSeconds(2)));
        assertEquals(ProxyDnsDiscoveryRuntime.PUBLICATION_THREAD_NAME, callbackThread.get());
        assertNotEquals(ProxyDnsDiscoveryRuntime.SCHEDULER_THREAD_NAME, callbackThread.get());

        runtime.close();

        assertTrue(interrupted.await(2, TimeUnit.SECONDS));
        assertTrue(waitUntil(() -> discoveryThreads().isEmpty(), Duration.ofSeconds(2)),
                () -> "discovery threads still alive: " + discoveryThreads());
    }

    @Test
    void runtimeUsesBoundedQueuesAndNoUnboundedScheduledExecutor() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/richmond423/loadbalancerpro/api/proxy/ProxyDnsDiscoveryRuntime.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("new ArrayBlockingQueue<>(MAX_NAMES)"));
        assertTrue(source.contains("new PriorityQueue<>(capacity)"));
        assertTrue(source.contains("CoalescingPublisher"));
        assertFalse(source.contains("ScheduledThreadPoolExecutor"));
        assertFalse(source.contains("LinkedBlockingQueue"));
        assertFalse(source.contains("ConcurrentLinkedQueue"));
        assertFalse(source.contains("newCachedThreadPool"));
    }

    private static ProxyDnsDiscoveryRuntime.Registration registration(
            String logicalId, String name, boolean privateNetworkOnly) {
        ProxyDnsDiscovery.Spec spec = ProxyDnsDiscovery.compile(
                "dns:" + name + ":8080", "http://" + name + ":8080/base",
                "address", "upstream.discovery");
        return new ProxyDnsDiscoveryRuntime.Registration(logicalId, spec, privateNetworkOnly);
    }

    private static InetAddress literal(int... octets) throws Exception {
        byte[] bytes = new byte[octets.length];
        for (int index = 0; index < octets.length; index++) {
            bytes[index] = (byte) octets[index];
        }
        return InetAddress.getByAddress(bytes);
    }

    private static boolean waitUntil(Check check, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!check.evaluate() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        return check.evaluate();
    }

    private static List<String> discoveryThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isAlive)
                .map(Thread::getName)
                .filter(name -> name.equals(ProxyDnsDiscoveryRuntime.SCHEDULER_THREAD_NAME)
                        || name.equals(ProxyDnsDiscoveryRuntime.PUBLICATION_THREAD_NAME)
                        || name.startsWith(ProxyDnsDiscoveryRuntime.LOOKUP_THREAD_PREFIX))
                .sorted()
                .toList();
    }

    private enum Mode {
        SUCCESS,
        EMPTY,
        FAILURE
    }

    @FunctionalInterface
    private interface Check {
        boolean evaluate() throws Exception;
    }

    private static final class Harness implements AutoCloseable {
        private final ManualScheduler scheduler = new ManualScheduler();
        private final ManualLookupExecutor lookup = new ManualLookupExecutor();
        private final AtomicInteger resolverCalls = new AtomicInteger();
        private final ProxyDnsDiscoveryRuntime runtime;

        private Harness(ProxyDnsDiscoveryRuntime.Resolver resolver) {
            runtime = new ProxyDnsDiscoveryRuntime(
                    SETTINGS,
                    name -> {
                        resolverCalls.incrementAndGet();
                        return resolver.resolve(name);
                    },
                    scheduler,
                    lookup,
                    new DirectPublisher());
        }

        private void resolveNext() {
            scheduler.runReady();
            lookup.runNext();
            scheduler.runReady();
        }

        @Override
        public void close() {
            runtime.close();
        }
    }

    private static final class DirectPublisher implements ProxyDnsDiscoveryRuntime.Publisher {
        @Override
        public void publish(ProxyDnsDiscoveryRuntime.Snapshot snapshot) {
        }

        @Override
        public void close() {
        }
    }

    private static final class ManualLookupExecutor implements ProxyDnsDiscoveryRuntime.LookupExecutor {
        private final Deque<Runnable> tasks = new ArrayDeque<>();

        @Override
        public boolean submit(Runnable task) {
            if (tasks.size() >= ProxyDnsDiscoveryRuntime.MAX_NAMES) {
                return false;
            }
            tasks.addLast(task);
            return true;
        }

        private void runNext() {
            Runnable task = tasks.pollFirst();
            if (task == null) {
                throw new AssertionError("no lookup task queued");
            }
            task.run();
        }

        private void runLast() {
            Runnable task = tasks.pollLast();
            if (task == null) {
                throw new AssertionError("no lookup task queued");
            }
            task.run();
        }

        @Override
        public int queuedTaskCount() {
            return tasks.size();
        }

        @Override
        public void close() {
            tasks.clear();
        }
    }

    private static final class ManualScheduler implements ProxyDnsDiscoveryRuntime.Scheduler {
        private final List<Task> tasks = new ArrayList<>();
        private long now;
        private long sequence;

        @Override
        public long nanoTime() {
            return now;
        }

        @Override
        public ProxyDnsDiscoveryRuntime.Cancellable schedule(Runnable task, long delayNanos) {
            Task scheduled = new Task(now + Math.max(0, delayNanos), ++sequence, task);
            tasks.add(scheduled);
            return () -> tasks.remove(scheduled);
        }

        private void advance(Duration duration) {
            now += duration.toNanos();
            runReady();
        }

        private void runReady() {
            while (true) {
                Task next = tasks.stream()
                        .filter(task -> task.deadlineNanos <= now)
                        .min(Comparator.comparingLong((Task task) -> task.deadlineNanos)
                                .thenComparingLong(task -> task.sequence))
                        .orElse(null);
                if (next == null) {
                    return;
                }
                tasks.remove(next);
                next.command.run();
            }
        }

        @Override
        public int queuedTaskCount() {
            return tasks.size();
        }

        @Override
        public void close() {
            tasks.clear();
        }

        private record Task(long deadlineNanos, long sequence, Runnable command) {
        }
    }
}
