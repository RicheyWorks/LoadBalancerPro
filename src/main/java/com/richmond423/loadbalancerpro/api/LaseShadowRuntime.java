package com.richmond423.loadbalancerpro.api;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import com.richmond423.loadbalancerpro.core.LaseEvaluationReport;
import com.richmond423.loadbalancerpro.core.LaseShadowAdvisor;
import com.richmond423.loadbalancerpro.core.LaseShadowDispatchSnapshot;
import com.richmond423.loadbalancerpro.core.LaseShadowEventLog;
import com.richmond423.loadbalancerpro.core.LaseShadowObservabilitySnapshot;
import com.richmond423.loadbalancerpro.core.LiveRoutingShadowObservation;
import com.richmond423.loadbalancerpro.core.LoadDistributionResult;
import com.richmond423.loadbalancerpro.core.Server;

/**
 * Application-scoped owner for LASE shadow state and bounded live-proxy dispatch.
 */
@Service
public final class LaseShadowRuntime implements SmartLifecycle {
    public static final int LIVE_QUEUE_CAPACITY = 100;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 2;

    private final boolean liveProxyEnabled;
    private final LaseShadowEventLog eventLog;
    private final LaseShadowAdvisor advisor;
    private final Consumer<LiveRoutingShadowObservation> liveObserver;
    private final ThreadPoolExecutor liveExecutor;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong totalAccepted = new AtomicLong();
    private final AtomicLong totalCompleted = new AtomicLong();
    private final AtomicLong totalDropped = new AtomicLong();

    @Autowired
    public LaseShadowRuntime(Environment environment) {
        this(AllocatorService.resolveLaseShadowEnabled(environment));
    }

    private LaseShadowRuntime(boolean liveProxyEnabled) {
        this.liveProxyEnabled = liveProxyEnabled;
        this.eventLog = new LaseShadowEventLog();
        this.advisor = new LaseShadowAdvisor(true, eventLog);
        this.liveObserver = advisor::observeLiveRouting;
        this.liveExecutor = liveProxyEnabled ? newExecutor(LIVE_QUEUE_CAPACITY) : null;
    }

    LaseShadowRuntime(
            boolean liveProxyEnabled,
            LaseShadowEventLog eventLog,
            LaseShadowAdvisor advisor,
            Consumer<LiveRoutingShadowObservation> liveObserver,
            int queueCapacity) {
        if (queueCapacity < 1) {
            throw new IllegalArgumentException("queueCapacity must be positive");
        }
        this.liveProxyEnabled = liveProxyEnabled;
        this.eventLog = Objects.requireNonNull(eventLog, "eventLog cannot be null");
        this.advisor = Objects.requireNonNull(advisor, "advisor cannot be null");
        this.liveObserver = Objects.requireNonNull(liveObserver, "liveObserver cannot be null");
        this.liveExecutor = liveProxyEnabled ? newExecutor(queueCapacity) : null;
    }

    public static LaseShadowRuntime disabled() {
        return new LaseShadowRuntime(false);
    }

    public boolean isLiveProxyEnabled() {
        return liveProxyEnabled;
    }

    public Optional<LaseEvaluationReport> observeAllocation(
            String strategy,
            List<Server> servers,
            double requestedLoad,
            LoadDistributionResult result,
            boolean enabledForCall) {
        if (!enabledForCall) {
            return Optional.empty();
        }
        return advisor.observe(strategy, servers, requestedLoad, result);
    }

    public boolean submitLiveRouting(LiveRoutingShadowObservation observation) {
        Objects.requireNonNull(observation, "observation cannot be null");
        if (!running.get() || !liveProxyEnabled || liveExecutor == null || liveExecutor.isShutdown()) {
            return false;
        }
        try {
            liveExecutor.execute(() -> {
                try {
                    liveObserver.accept(observation);
                } finally {
                    totalCompleted.incrementAndGet();
                }
            });
            totalAccepted.incrementAndGet();
            return true;
        } catch (java.util.concurrent.RejectedExecutionException exception) {
            totalDropped.incrementAndGet();
            return false;
        }
    }

    public LaseShadowObservabilitySnapshot snapshot() {
        return eventLog.snapshot(dispatchSnapshot());
    }

    private LaseShadowDispatchSnapshot dispatchSnapshot() {
        if (liveExecutor == null) {
            return LaseShadowDispatchSnapshot.inactive(LIVE_QUEUE_CAPACITY);
        }
        return new LaseShadowDispatchSnapshot(
                liveProxyEnabled,
                liveExecutor.getQueue().remainingCapacity() + liveExecutor.getQueue().size(),
                liveExecutor.getQueue().size(),
                liveExecutor.getActiveCount(),
                totalAccepted.get(),
                totalCompleted.get(),
                totalDropped.get(),
                liveExecutor.isShutdown());
    }

    private static ThreadPoolExecutor newExecutor(int queueCapacity) {
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "lase-shadow-live-proxy");
            thread.setDaemon(true);
            return thread;
        };
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
        executor.prestartCoreThread();
        return executor;
    }

    @Override
    public void start() {
        if (!running.get()) {
            throw new IllegalStateException("LASE shadow runtime cannot restart after shutdown");
        }
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false) || liveExecutor == null) {
            return;
        }
        liveExecutor.shutdown();
        try {
            if (!liveExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                totalDropped.addAndGet(liveExecutor.shutdownNow().size());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            totalDropped.addAndGet(liveExecutor.shutdownNow().size());
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    void close() {
        stop();
    }
}
