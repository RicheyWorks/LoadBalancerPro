package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SimulationCoreCorrectnessBatchTest {
    private static final Instant NOW = Instant.parse("2026-07-29T00:00:00Z");
    private static final double DELTA = 0.0001;

    private final List<LoadBalancer> balancers = new CopyOnWriteArrayList<>();

    @AfterEach
    void shutdownBalancers() {
        balancers.forEach(LoadBalancer::shutdown);
    }

    @Test
    void criticalTrafficIsNeverShedBeforeProtectedUserTrafficAtTheSamePressure() {
        LoadSheddingPolicy policy = new LoadSheddingPolicy();
        LoadSheddingConfig userProtectedWithoutCriticalBypass =
                new LoadSheddingConfig(0.75, 0.90, 20, 250.0, 0.10, false, false);
        LoadSheddingSignal hardPressure =
                new LoadSheddingSignal("cluster", 95, 100, 2, 80.0, 0.01, NOW);

        LoadSheddingDecision user =
                policy.decide(RequestPriority.USER, hardPressure, userProtectedWithoutCriticalBypass);
        LoadSheddingDecision critical =
                policy.decide(RequestPriority.CRITICAL, hardPressure, userProtectedWithoutCriticalBypass);

        assertAll("higher-priority traffic cannot be rejected while USER is protected",
                () -> assertEquals(LoadSheddingDecision.Action.ALLOW, user.action()),
                () -> assertEquals(LoadSheddingDecision.Action.ALLOW, critical.action()));
    }

    @Test
    void healthRedistributionMergesWithTheSurvivorsExistingAccumulatedLoad() {
        LoadBalancer balancer = managedBalancer();
        Server failing = server("FAIL");
        Server survivor = server("KEEP");
        balancer.addServer(failing);
        balancer.addServer(survivor);
        assertEquals(Map.of("FAIL", 50.0, "KEEP", 50.0), balancer.roundRobin(100.0));

        failing.updateMetrics(100.0, 100.0, 100.0);
        balancer.checkServerHealth();
        balancer.checkServerHealth();
        balancer.checkServerHealth();

        Map<String, Double> rebalanced = balancer.rebalanceExistingLoad();

        assertEquals(Map.of("KEEP", 100.0), rebalanced,
                "redistribution must preserve the survivor's existing 50 plus the failed server's 50");
    }

    @Test
    void consistentHashingParticipatesInTheServerReadLock() throws Exception {
        LoadBalancer balancer = managedBalancer();
        balancer.addServer(server("HASH"));
        ReentrantReadWriteLock serverLock = serverLock(balancer);
        List<Thread> workers = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread worker = new Thread(runnable, "consistent-hashing-read-lock-test");
            workers.add(worker);
            return worker;
        });
        CountDownLatch started = new CountDownLatch(1);
        Future<Map<String, Double>> future;

        serverLock.writeLock().lock();
        try {
            future = executor.submit(() -> {
                started.countDown();
                return balancer.consistentHashing(10.0, 1);
            });
            assertTrue(started.await(1, TimeUnit.SECONDS));
            awaitBlocked(workers, future);
            assertFalse(future.isDone(),
                    "consistent hashing must wait behind a server write lock");
        } finally {
            serverLock.writeLock().unlock();
        }

        try {
            assertEquals(Map.of("HASH", 10.0), future.get(1, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void consistentHashingKeepsEveryRegistryAndRingReadInsideOneReadLockScope() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/richmond423/loadbalancerpro/core/LoadBalancer.java"),
                StandardCharsets.UTF_8);
        int methodStart = source.indexOf(
                "public Map<String, Double> consistentHashing(double totalData, int numKeys)");
        int methodEnd = source.indexOf(
                "public Map<String, Double> capacityAware(double totalData)", methodStart);
        String method = source.substring(methodStart, methodEnd);
        int lock = method.indexOf("serverLock.readLock().lock()");
        int firstRegistryRead = method.indexOf("serverRegistry.isEmpty()");
        int unlock = method.indexOf("serverLock.readLock().unlock()");

        assertAll("consistent hashing outer read-lock scope",
                () -> assertTrue(lock >= 0 && lock < firstRegistryRead,
                        "the read lock must be acquired before the first registry/ring check"),
                () -> assertTrue(method.contains("finally"),
                        "the read lock must be released from a finally block"),
                () -> assertTrue(unlock > firstRegistryRead,
                        "the read lock must be released after all registry/ring reads"));
    }

    @Test
    void metricHistoryIndexUsesFloorModAfterIntegerOverflow() throws Exception {
        Server server = server("HISTORY");
        Field historyIndexField = Server.class.getDeclaredField("historyIndex");
        historyIndexField.setAccessible(true);
        AtomicInteger historyIndex = (AtomicInteger) historyIndexField.get(server);
        historyIndex.set(Integer.MIN_VALUE);

        assertDoesNotThrow(() -> server.updateMetrics(40.0, 50.0, 60.0));
        assertEquals(Integer.MIN_VALUE + 1, historyIndex.get());
    }

    @Test
    void invalidMetricInputCannotReplaceTheLastValidRollbackSnapshot() {
        Server server = server("SNAPSHOT");
        server.updateMetrics(40.0, 50.0, 60.0);

        assertThrows(IllegalArgumentException.class,
                () -> server.updateMetrics(-1.0, 70.0, 80.0));
        server.rollbackToLastSnapshot();

        assertAll("invalid input must leave the previous rollback point intact",
                () -> assertEquals(10.0, server.getCpuUsage(), DELTA),
                () -> assertEquals(20.0, server.getMemoryUsage(), DELTA),
                () -> assertEquals(30.0, server.getDiskUsage(), DELTA));
    }

    @Test
    void concurrentSingleMetricSettersDoNotLoseEachOthersUpdates() throws Exception {
        Server server = server("SETTERS");
        List<Thread> workers = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread worker = new Thread(runnable, "single-metric-setter-test");
            workers.add(worker);
            return worker;
        });
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        Future<?> cpuFuture = executor.submit(() -> {
            ready.countDown();
            await(go);
            server.setCpuUsage(80.0);
        });
        Future<?> memoryFuture = executor.submit(() -> {
            ready.countDown();
            await(go);
            server.setMemoryUsage(90.0);
        });

        assertTrue(ready.await(1, TimeUnit.SECONDS));
        synchronized (server) {
            go.countDown();
            awaitBlocked(workers, cpuFuture, memoryFuture);
        }

        try {
            cpuFuture.get(1, TimeUnit.SECONDS);
            memoryFuture.get(1, TimeUnit.SECONDS);
            assertAll("both single-metric writes must survive",
                    () -> assertEquals(80.0, server.getCpuUsage(), DELTA),
                    () -> assertEquals(90.0, server.getMemoryUsage(), DELTA),
                    () -> assertEquals(30.0, server.getDiskUsage(), DELTA));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void registryDoesNotRetainTheDeadLoadQueue() {
        assertThrows(NoSuchFieldException.class,
                () -> ServerRegistry.class.getDeclaredField("loadQueue"));
    }

    @Test
    void globalCloudPropertyCannotOverrideAnExplicitServerType() {
        String previous = System.getProperty("isCloudServer");
        try {
            System.setProperty("isCloudServer", "true");

            Server onsite = new Server("EXPLICIT-ONSITE", 10.0, 20.0, 30.0, ServerType.ONSITE);

            assertEquals(ServerType.ONSITE, onsite.getServerType());
            assertFalse(onsite.isCloudInstance());
        } finally {
            restoreProperty("isCloudServer", previous);
        }
    }

    private LoadBalancer managedBalancer() {
        LoadBalancer balancer = new LoadBalancer();
        balancers.add(balancer);
        return balancer;
    }

    private static Server server(String id) {
        return new Server(id, 10.0, 20.0, 30.0);
    }

    private static ReentrantReadWriteLock serverLock(LoadBalancer balancer) throws Exception {
        Field field = LoadBalancer.class.getDeclaredField("serverLock");
        field.setAccessible(true);
        return (ReentrantReadWriteLock) field.get(balancer);
    }

    private static void awaitBlocked(List<Thread> workers, Future<?>... futures) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            boolean allWorkersBlocked = !workers.isEmpty()
                    && workers.stream().allMatch(thread -> thread.getState() == Thread.State.BLOCKED
                    || thread.getState() == Thread.State.WAITING);
            boolean anyFutureCompleted = List.of(futures).stream().anyMatch(Future::isDone);
            if (allWorkersBlocked || anyFutureCompleted) {
                return;
            }
            Thread.sleep(5);
        }
        throw new AssertionError("workers did not reach the expected blocked state");
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(1, TimeUnit.SECONDS)) {
                throw new AssertionError("timed out waiting for test coordination");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("test coordination was interrupted", interrupted);
        }
    }

    private static void restoreProperty(String name, String previous) {
        if (previous == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, previous);
        }
    }
}
