package com.richmond423.loadbalancerpro.api.proxy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Runs bounded active-health probes away from request and status threads.
 */
final class UpstreamHealthProber implements AutoCloseable, SmartLifecycle {
    static final String THREAD_NAME_PREFIX = "loadbalancerpro-health-probe-";
    private static final Logger logger = LoggerFactory.getLogger(UpstreamHealthProber.class);

    private final ScheduledExecutorService scheduler;
    private final ProbeTransport transport;
    private final Clock clock;
    private final ProbeListener listener;
    private final LongUnaryOperator initialJitterNanos;
    private final ConcurrentMap<String, HealthState> states = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> tasks = new LinkedHashMap<>();
    private final AtomicLong generation = new AtomicLong();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private ProbeConfiguration activeConfiguration;

    UpstreamHealthProber(HttpClient httpClient, Clock clock, ProbeListener listener) {
        this(
                newScheduler(),
                target -> httpProbe(httpClient, target),
                clock,
                listener,
                intervalNanos -> intervalNanos <= 1
                        ? 0
                        : ThreadLocalRandom.current().nextLong(intervalNanos));
    }

    UpstreamHealthProber(
            ScheduledExecutorService scheduler,
            ProbeTransport transport,
            Clock clock,
            ProbeListener listener,
            LongUnaryOperator initialJitterNanos) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler cannot be null");
        this.transport = Objects.requireNonNull(transport, "transport cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.listener = Objects.requireNonNull(listener, "listener cannot be null");
        this.initialJitterNanos =
                Objects.requireNonNull(initialJitterNanos, "initialJitterNanos cannot be null");
    }

    synchronized void configure(
            Collection<Target> configuredTargets,
            Duration interval,
            int healthyThreshold,
            int unhealthyThreshold) {
        Objects.requireNonNull(configuredTargets, "configuredTargets cannot be null");
        long intervalNanos = positiveNanos(interval, "interval");
        if (healthyThreshold <= 0) {
            throw new IllegalArgumentException("healthyThreshold must be greater than zero");
        }
        if (unhealthyThreshold <= 0) {
            throw new IllegalArgumentException("unhealthyThreshold must be greater than zero");
        }
        if (!running.get()) {
            throw new IllegalStateException("health prober is stopped");
        }

        long configuredGeneration = generation.incrementAndGet();
        cancelTasks();
        ProbeConfiguration nextConfiguration =
                new ProbeConfiguration(intervalNanos, healthyThreshold, unhealthyThreshold);
        boolean preserveUnchanged = nextConfiguration.equals(activeConfiguration);
        Map<String, Target> uniqueTargets = new LinkedHashMap<>();
        for (Target target : configuredTargets) {
            Target validated = Objects.requireNonNull(target, "configuredTargets cannot contain null");
            uniqueTargets.putIfAbsent(validated.id(), validated);
        }
        Map<String, HealthState> nextStates = new LinkedHashMap<>();
        uniqueTargets.forEach((id, target) -> {
            HealthState prior = preserveUnchanged ? states.get(id) : null;
            HealthState state = prior != null && prior.matches(target)
                    ? prior.retarget(target)
                    : new HealthState(target);
            nextStates.put(id, state);
        });
        states.clear();
        states.putAll(nextStates);
        activeConfiguration = nextConfiguration;
        uniqueTargets.forEach((id, target) -> {
            long requestedJitter = initialJitterNanos.applyAsLong(intervalNanos);
            long initialDelay = Math.max(0, Math.min(intervalNanos - 1, requestedJitter));
            ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                    () -> probe(configuredGeneration, target, nextStates.get(id),
                            healthyThreshold, unhealthyThreshold),
                    initialDelay,
                    intervalNanos,
                    TimeUnit.NANOSECONDS);
            tasks.put(id, task);
        });
    }

    synchronized void clear() {
        generation.incrementAndGet();
        cancelTasks();
        states.clear();
        activeConfiguration = null;
    }

    Optional<HealthSnapshot> snapshot(String upstreamId, long configurationGeneration) {
        if (upstreamId == null || upstreamId.isBlank()) {
            return Optional.empty();
        }
        HealthState state = states.get(upstreamId.trim());
        return state == null || !state.belongsTo(configurationGeneration)
                ? Optional.empty()
                : Optional.of(state.snapshot());
    }

    private void probe(
            long configuredGeneration,
            Target target,
            HealthState state,
            int healthyThreshold,
            int unhealthyThreshold) {
        if (generation.get() != configuredGeneration) {
            return;
        }
        ProbeResult result;
        try {
            result = Objects.requireNonNull(transport.probe(target), "probe transport returned null");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            result = new ProbeResult(false, null, "probe interrupted");
        } catch (Exception exception) {
            result = new ProbeResult(false, null, "probe failed");
        }
        if (generation.get() != configuredGeneration) {
            return;
        }
        HealthSnapshot snapshot =
                state.record(result, healthyThreshold, unhealthyThreshold, Instant.now(clock));
        try {
            listener.onProbe(target, result.successful(), snapshot);
        } catch (RuntimeException exception) {
            logger.warn("proxy.health.probe_listener_failure upstreamId={} exceptionType={}",
                    target.id(), exception.getClass().getSimpleName());
        }
    }

    private synchronized void cancelTasks() {
        tasks.values().forEach(task -> task.cancel(true));
        tasks.clear();
    }

    @Override
    public void start() {
        if (scheduler.isShutdown()) {
            throw new IllegalStateException("health prober cannot restart after shutdown");
        }
        running.set(true);
    }

    @Override
    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            clear();
            scheduler.shutdownNow();
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

    @Override
    public void close() {
        stop();
    }

    private static ScheduledExecutorService newScheduler() {
        AtomicInteger threadNumber = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME_PREFIX + threadNumber.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newScheduledThreadPool(2, threadFactory);
    }

    @SuppressWarnings("java/ssrf")
    private static ProbeResult httpProbe(HttpClient httpClient, Target target)
            throws IOException, InterruptedException {
        HttpClient selectedClient = target.httpClient() == null
                ? Objects.requireNonNull(httpClient, "httpClient cannot be null")
                : target.httpClient();
        HttpRequest request = HttpRequest.newBuilder(target.uri())
                .timeout(target.timeout())
                .GET()
                .build();
        HttpResponse<Void> response = selectedClient.send(request, HttpResponse.BodyHandlers.discarding());
        int statusCode = response.statusCode();
        boolean successful = statusCode >= 200 && statusCode <= 399;
        return new ProbeResult(
                successful,
                statusCode,
                successful ? "2xx/3xx probe response" : "non-2xx/3xx probe response");
    }

    private static long positiveNanos(Duration duration, String fieldName) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        try {
            return duration.toNanos();
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    record Target(String id, URI uri, Duration timeout, long configurationGeneration, HttpClient httpClient) {
        Target(String id, URI uri, Duration timeout) {
            this(id, uri, timeout, 0, null);
        }

        Target(String id, URI uri, Duration timeout, long configurationGeneration) {
            this(id, uri, timeout, configurationGeneration, null);
        }

        Target {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("id cannot be null or blank");
            }
            id = id.trim();
            Objects.requireNonNull(uri, "uri cannot be null");
            positiveNanos(timeout, "timeout");
            if (configurationGeneration < 0) {
                throw new IllegalArgumentException("configurationGeneration must be non-negative");
            }
        }
    }

    private record ProbeConfiguration(long intervalNanos, int healthyThreshold, int unhealthyThreshold) {
    }

    record HealthSnapshot(
            boolean healthy,
            Integer statusCode,
            String outcome,
            Instant checkedAt,
            int consecutiveSuccesses,
            int consecutiveFailures) {
        HealthSnapshot {
            outcome = outcome == null || outcome.isBlank() ? "probe result unavailable" : outcome;
            Objects.requireNonNull(checkedAt, "checkedAt cannot be null");
            if (consecutiveSuccesses < 0 || consecutiveFailures < 0) {
                throw new IllegalArgumentException("probe streaks must be non-negative");
            }
        }
    }

    record ProbeResult(boolean successful, Integer statusCode, String outcome) {
        ProbeResult {
            outcome = outcome == null || outcome.isBlank() ? "probe result unavailable" : outcome;
        }
    }

    @FunctionalInterface
    interface ProbeTransport {
        ProbeResult probe(Target target) throws Exception;
    }

    @FunctionalInterface
    interface ProbeListener {
        void onProbe(Target target, boolean successful, HealthSnapshot snapshot);
    }

    private static final class HealthState {
        private Target target;
        private final long firstCompatibleConfigurationGeneration;
        private boolean healthy = true;
        private int consecutiveSuccesses;
        private int consecutiveFailures;
        private volatile HealthSnapshot snapshot = new HealthSnapshot(
                true,
                null,
                "awaiting first background probe",
                Instant.EPOCH,
                0,
                0);

        private HealthState(Target target) {
            this.target = target;
            this.firstCompatibleConfigurationGeneration = target.configurationGeneration();
        }

        private synchronized boolean matches(Target candidate) {
            return target.uri().equals(candidate.uri())
                    && target.timeout().equals(candidate.timeout())
                    && target.httpClient() == candidate.httpClient();
        }

        private synchronized HealthState retarget(Target candidate) {
            target = candidate;
            return this;
        }

        private synchronized boolean belongsTo(long expectedGeneration) {
            return expectedGeneration >= firstCompatibleConfigurationGeneration
                    && expectedGeneration <= target.configurationGeneration();
        }

        private synchronized HealthSnapshot record(
                ProbeResult result,
                int healthyThreshold,
                int unhealthyThreshold,
                Instant now) {
            if (result.successful()) {
                consecutiveSuccesses++;
                consecutiveFailures = 0;
                if (!healthy && consecutiveSuccesses >= healthyThreshold) {
                    healthy = true;
                }
            } else {
                consecutiveFailures++;
                consecutiveSuccesses = 0;
                if (healthy && consecutiveFailures >= unhealthyThreshold) {
                    healthy = false;
                }
            }
            snapshot = new HealthSnapshot(
                    healthy,
                    result.statusCode(),
                    result.outcome(),
                    now,
                    consecutiveSuccesses,
                    consecutiveFailures);
            return snapshot;
        }

        private HealthSnapshot snapshot() {
            return snapshot;
        }
    }
}
