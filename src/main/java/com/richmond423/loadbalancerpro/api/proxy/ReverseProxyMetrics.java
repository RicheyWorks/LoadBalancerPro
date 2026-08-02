package com.richmond423.loadbalancerpro.api.proxy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Component
public class ReverseProxyMetrics {
    static final String REQUESTS = "lbp.proxy.requests";
    static final String LATENCY = "lbp.proxy.latency";
    static final String INFLIGHT = "lbp.proxy.inflight";
    static final String ATTEMPTS = "lbp.proxy.attempts";
    static final String RETRIES = "lbp.proxy.retries";
    static final String REQUEST_BYTES = "lbp.proxy.request.bytes";
    static final String RESPONSE_BYTES = "lbp.proxy.response.bytes";
    static final String LIMIT_REJECTIONS = "lbp.proxy.limit.rejections";
    static final String SHEDS = "lbp.proxy.sheds";
    static final String HEALTH = "lbp.proxy.health";
    static final String COOLDOWN_TRIPS = "lbp.proxy.cooldown.trips";

    static final String NONE = "NONE";
    static final String OTHER = "OTHER";
    static final String UNMATCHED = "UNMATCHED";

    private static final List<String> STATUS_CLASSES = List.of("2xx", "3xx", "4xx", "5xx", "other");
    private static final Duration[] LATENCY_BUCKETS = {
            Duration.ofMillis(5),
            Duration.ofMillis(25),
            Duration.ofMillis(100),
            Duration.ofMillis(500),
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            Duration.ofSeconds(30)
    };

    private final LongAdder totalForwarded = new LongAdder();
    private final LongAdder totalFailures = new LongAdder();
    private final LongAdder totalRetryAttempts = new LongAdder();
    private final LongAdder totalCooldownActivations = new LongAdder();
    private final ConcurrentMap<String, UpstreamCounterState> upstreamCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> statusClassCounters = new ConcurrentHashMap<>();
    private final AtomicReference<String> lastSelectedUpstream = new AtomicReference<>("none");

    private final MeterRegistry registry;
    private final ConcurrentMap<SeriesKey, MeterSeries> meterSeries = new ConcurrentHashMap<>();
    private final MeterSeries unmatchedSeries;
    private volatile Map<String, List<MeterSeries>> activeSeriesByUpstream = Map.of();

    public ReverseProxyMetrics() {
        this(new SimpleMeterRegistry());
    }

    @Autowired
    public ReverseProxyMetrics(ObjectProvider<MeterRegistry> registryProvider) {
        this(registryProvider.getIfAvailable(SimpleMeterRegistry::new));
    }

    ReverseProxyMetrics(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        STATUS_CLASSES.forEach(statusClass -> statusClassCounters.put(statusClass, new LongAdder()));
        this.unmatchedSeries = seriesFor(new SeriesKey(UNMATCHED, NONE));
        this.unmatchedSeries.configured = true;
    }

    void activateConfiguration(List<ReverseProxyRoutePlanner.ConfiguredRoute> routes) {
        try {
            activateConfigurationSafely(routes == null ? List.of() : routes);
        } catch (RuntimeException exception) {
            // Metrics must never block proxy startup or reload.
        }
    }

    private synchronized void activateConfigurationSafely(
            List<ReverseProxyRoutePlanner.ConfiguredRoute> routes) {
        Set<SeriesKey> configuredKeys = new LinkedHashSet<>();
        configuredKeys.add(unmatchedSeries.key);
        Map<String, List<MeterSeries>> byUpstream = new LinkedHashMap<>();
        for (ReverseProxyRoutePlanner.ConfiguredRoute route : routes) {
            SeriesKey routeOnly = new SeriesKey(route.name(), NONE);
            configuredKeys.add(routeOnly);
            seriesFor(routeOnly).configured = true;
            for (ReverseProxyProperties.Upstream upstream : route.targets()) {
                String upstreamId = upstream.getId().trim();
                SeriesKey key = new SeriesKey(route.name(), upstreamId);
                configuredKeys.add(key);
                MeterSeries series = seriesFor(key);
                series.configured = true;
                series.health.set(upstream.isHealthy() ? 1 : 0);
                byUpstream.computeIfAbsent(upstreamId, ignored -> new ArrayList<>()).add(series);
            }
        }
        for (MeterSeries series : List.copyOf(meterSeries.values())) {
            if (!configuredKeys.contains(series.key)) {
                series.configured = false;
                retireIfIdle(series);
            }
        }
        Map<String, List<MeterSeries>> immutable = new LinkedHashMap<>();
        byUpstream.forEach((id, series) -> immutable.put(id, List.copyOf(series)));
        activeSeriesByUpstream = Map.copyOf(immutable);
    }

    RequestObservation beginRequest() {
        try {
            return new RequestObservation(unmatchedSeries);
        } catch (RuntimeException exception) {
            return new RequestObservation();
        }
    }

    void recordForwarded(String upstreamId, int statusCode) {
        String normalizedUpstreamId = normalizeUpstreamId(upstreamId);
        totalForwarded.increment();
        countersFor(normalizedUpstreamId).forwarded.increment();
        statusClassCounters.computeIfAbsent(statusClass(statusCode), ignored -> new LongAdder()).increment();
        lastSelectedUpstream.set(normalizedUpstreamId);
    }

    void recordFailure(String upstreamId, int statusCode) {
        String normalizedUpstreamId = normalizeUpstreamId(upstreamId);
        totalFailures.increment();
        if (!normalizedUpstreamId.isEmpty()) {
            countersFor(normalizedUpstreamId).failures.increment();
            lastSelectedUpstream.set(normalizedUpstreamId);
        }
        statusClassCounters.computeIfAbsent(statusClass(statusCode), ignored -> new LongAdder()).increment();
    }

    void recordRetryAttempt(String upstreamId) {
        String normalizedUpstreamId = normalizeUpstreamId(upstreamId);
        totalRetryAttempts.increment();
        if (!normalizedUpstreamId.isEmpty()) {
            countersFor(normalizedUpstreamId).retryAttempts.increment();
        }
    }

    void recordCooldownActivation(String upstreamId) {
        String normalizedUpstreamId = normalizeUpstreamId(upstreamId);
        totalCooldownActivations.increment();
        if (!normalizedUpstreamId.isEmpty()) {
            countersFor(normalizedUpstreamId).cooldownActivations.increment();
            activeSeriesByUpstream.getOrDefault(normalizedUpstreamId, List.of())
                    .forEach(series -> safeIncrement(series.cooldownTrips));
        }
    }

    void recordHealth(String upstreamId, boolean healthy) {
        String normalizedUpstreamId = normalizeUpstreamId(upstreamId);
        if (normalizedUpstreamId.isEmpty()) {
            return;
        }
        activeSeriesByUpstream.getOrDefault(normalizedUpstreamId, List.of())
                .forEach(series -> series.health.set(healthy ? 1 : 0));
    }

    ReverseProxyMetricsSnapshot snapshot(List<String> orderedUpstreamIds) {
        Set<String> orderedIds = new LinkedHashSet<>();
        if (orderedUpstreamIds != null) {
            orderedUpstreamIds.stream()
                    .map(ReverseProxyMetrics::normalizeUpstreamId)
                    .filter(id -> !id.isEmpty())
                    .forEach(orderedIds::add);
        }
        upstreamCounters.keySet().stream()
                .filter(id -> !orderedIds.contains(id))
                .sorted(Comparator.naturalOrder())
                .forEach(orderedIds::add);

        List<ReverseProxyMetricsSnapshot.UpstreamCounters> upstreamSnapshots = orderedIds.stream()
                .map(id -> {
                    UpstreamCounterState counters = countersFor(id);
                    return new ReverseProxyMetricsSnapshot.UpstreamCounters(
                            id,
                            counters.forwarded.sum(),
                            counters.failures.sum(),
                            counters.retryAttempts.sum(),
                            counters.cooldownActivations.sum());
                })
                .toList();

        Map<String, Long> statusClasses = new LinkedHashMap<>();
        STATUS_CLASSES.forEach(statusClass -> statusClasses.put(
                statusClass,
                statusClassCounters.computeIfAbsent(statusClass, ignored -> new LongAdder()).sum()));
        return new ReverseProxyMetricsSnapshot(
                totalForwarded.sum(),
                totalFailures.sum(),
                totalRetryAttempts.sum(),
                totalCooldownActivations.sum(),
                statusClasses,
                lastSelectedUpstream.get(),
                upstreamSnapshots);
    }

    private MeterSeries seriesFor(SeriesKey key) {
        return meterSeries.computeIfAbsent(key, MeterSeries::new);
    }

    private synchronized void retireIfIdle(MeterSeries series) {
        if (series.configured || series.inflight.get() != 0
                || series == unmatchedSeries) {
            return;
        }
        if (meterSeries.remove(series.key, series)) {
            series.removeMeters();
        }
    }

    private Meter register(MeterRegistration registration) {
        try {
            return registration.register();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Counter counter(String name, Tags tags, Set<Meter> meters) {
        Meter meter = register(() -> Counter.builder(name).tags(tags).register(registry));
        if (meter instanceof Counter counter) {
            meters.add(counter);
            return counter;
        }
        return null;
    }

    private static void safeIncrement(Counter counter) {
        if (counter == null) {
            return;
        }
        try {
            counter.increment();
        } catch (RuntimeException exception) {
            // A monitoring failure cannot alter the proxied request.
        }
    }

    private static void safeRecord(Timer timer, long nanos) {
        if (timer == null) {
            return;
        }
        try {
            timer.record(Math.max(0L, nanos), java.util.concurrent.TimeUnit.NANOSECONDS);
        } catch (RuntimeException exception) {
            // A monitoring failure cannot alter the proxied request.
        }
    }

    private static void safeRecord(DistributionSummary summary, long bytes) {
        if (summary == null) {
            return;
        }
        try {
            summary.record(Math.max(0L, bytes));
        } catch (RuntimeException exception) {
            // A monitoring failure cannot alter the proxied request.
        }
    }

    private UpstreamCounterState countersFor(String upstreamId) {
        return upstreamCounters.computeIfAbsent(upstreamId, ignored -> new UpstreamCounterState());
    }

    private static String normalizeUpstreamId(String upstreamId) {
        return upstreamId == null ? "" : upstreamId.trim();
    }

    static String statusClass(int statusCode) {
        if (statusCode >= 100 && statusCode <= 199) {
            return "1xx";
        }
        if (statusCode >= 200 && statusCode <= 299) {
            return "2xx";
        }
        if (statusCode >= 300 && statusCode <= 399) {
            return "3xx";
        }
        if (statusCode >= 400 && statusCode <= 499) {
            return "4xx";
        }
        if (statusCode >= 500 && statusCode <= 599) {
            return "5xx";
        }
        return statusCode == 0 ? NONE : OTHER;
    }

    enum TerminalOutcome {
        SUCCESS,
        UPSTREAM_4XX,
        UPSTREAM_5XX,
        INVALID_PATH,
        ROUTE_NOT_FOUND,
        LOAD_SHED,
        GLOBAL_CONCURRENCY_LIMIT,
        UPSTREAM_CONCURRENCY_LIMIT,
        NO_UPSTREAM,
        REQUEST_SIZE_LIMIT,
        RESPONSE_SIZE_LIMIT_PRECOMMIT,
        RESPONSE_SIZE_LIMIT_POSTCOMMIT,
        UPSTREAM_TRANSPORT_FAILURE,
        UPSTREAM_ABORT_PRECOMMIT,
        UPSTREAM_ABORT_POSTCOMMIT,
        DOWNSTREAM_DISCONNECT,
        INTERRUPTED,
        INTERNAL_ERROR,
        OTHER;

        static TerminalOutcome fromStatus(int statusCode) {
            if (statusCode >= 200 && statusCode <= 399) {
                return SUCCESS;
            }
            if (statusCode >= 400 && statusCode <= 499) {
                return UPSTREAM_4XX;
            }
            if (statusCode >= 500 && statusCode <= 599) {
                return UPSTREAM_5XX;
            }
            return OTHER;
        }
    }

    enum RetryReason {
        INITIAL,
        RETRYABLE_STATUS,
        TRANSPORT_FAILURE,
        PRECOMMIT_UPSTREAM_FAILURE,
        OTHER
    }

    private enum AttemptKind {
        PRIMARY,
        RETRY
    }

    private enum LimitDirection {
        REQUEST,
        RESPONSE
    }

    private enum LimitPhase {
        PRECOMMIT,
        POSTCOMMIT
    }

    private enum ShedReason {
        LOAD_SHED,
        GLOBAL_CONCURRENCY_LIMIT,
        UPSTREAM_CONCURRENCY_LIMIT,
        OTHER
    }

    final class RequestObservation {
        private final long startedAtNanos;
        private final AtomicBoolean completed = new AtomicBoolean();
        private final AtomicBoolean terminalAssigned = new AtomicBoolean();
        private final AtomicLong responseBytes = new AtomicLong();
        private MeterSeries currentSeries;
        private int statusCode;
        private TerminalOutcome outcome = TerminalOutcome.INTERNAL_ERROR;
        private final boolean enabled;

        private RequestObservation(MeterSeries initialSeries) {
            this.startedAtNanos = System.nanoTime();
            this.currentSeries = initialSeries;
            this.enabled = true;
            initialSeries.inflight.incrementAndGet();
        }

        private RequestObservation() {
            this.startedAtNanos = 0L;
            this.currentSeries = null;
            this.enabled = false;
        }

        synchronized void bindRoute(String route) {
            moveTo(new SeriesKey(route, NONE));
        }

        synchronized void bindUpstream(String route, String upstream) {
            moveTo(new SeriesKey(route, upstream));
        }

        private void moveTo(SeriesKey key) {
            if (!enabled || completed.get() || currentSeries.key.equals(key)) {
                return;
            }
            MeterSeries next = seriesFor(key);
            next.inflight.incrementAndGet();
            MeterSeries previous = currentSeries;
            currentSeries = next;
            previous.inflight.decrementAndGet();
            retireIfIdle(previous);
        }

        void recordDispatch(boolean retry, RetryReason reason) {
            if (!enabled || completed.get()) {
                return;
            }
            RetryReason safeReason = reason == null ? RetryReason.OTHER : reason;
            MeterSeries series = currentSeries;
            safeIncrement(series.attempts.get(new AttemptKey(
                    retry ? AttemptKind.RETRY : AttemptKind.PRIMARY, safeReason)));
            if (retry) {
                safeIncrement(series.retries.get(safeReason));
            }
        }

        void addResponseBytes(long bytes) {
            if (enabled && bytes > 0 && !completed.get()) {
                responseBytes.addAndGet(bytes);
            }
        }

        void terminal(int statusCode, TerminalOutcome outcome) {
            if (!enabled || completed.get()) {
                return;
            }
            this.statusCode = statusCode;
            this.outcome = outcome == null ? TerminalOutcome.OTHER : outcome;
            terminalAssigned.set(true);
        }

        void terminalIfUnset(int statusCode, TerminalOutcome outcome) {
            if (!enabled || completed.get() || !terminalAssigned.compareAndSet(false, true)) {
                return;
            }
            this.statusCode = statusCode;
            this.outcome = outcome == null ? TerminalOutcome.OTHER : outcome;
        }

        void complete(long requestBytes) {
            if (!enabled || !completed.compareAndSet(false, true)) {
                return;
            }
            MeterSeries series = currentSeries;
            try {
                TerminalKey terminalKey = new TerminalKey(statusClass(statusCode), outcome);
                TerminalMeters terminalMeters = series.terminalMeters.computeIfAbsent(
                        terminalKey, series::registerTerminalMeters);
                safeIncrement(terminalMeters.requests);
                safeRecord(series.latency, System.nanoTime() - startedAtNanos);
                safeRecord(series.requestBytes, requestBytes);
                safeRecord(series.responseBytes, responseBytes.get());
                recordTerminalSideMeters(series, outcome);
            } catch (RuntimeException exception) {
                // A monitoring failure cannot alter the proxied request.
            } finally {
                series.inflight.decrementAndGet();
                retireIfIdle(series);
            }
        }
    }

    private void recordTerminalSideMeters(MeterSeries series, TerminalOutcome outcome) {
        switch (outcome) {
            case REQUEST_SIZE_LIMIT -> safeIncrement(series.limitRejections.get(
                    new LimitKey(LimitDirection.REQUEST, LimitPhase.PRECOMMIT)));
            case RESPONSE_SIZE_LIMIT_PRECOMMIT -> safeIncrement(series.limitRejections.get(
                    new LimitKey(LimitDirection.RESPONSE, LimitPhase.PRECOMMIT)));
            case RESPONSE_SIZE_LIMIT_POSTCOMMIT -> safeIncrement(series.limitRejections.get(
                    new LimitKey(LimitDirection.RESPONSE, LimitPhase.POSTCOMMIT)));
            case LOAD_SHED -> safeIncrement(series.sheds.get(ShedReason.LOAD_SHED));
            case GLOBAL_CONCURRENCY_LIMIT -> safeIncrement(
                    series.sheds.get(ShedReason.GLOBAL_CONCURRENCY_LIMIT));
            case UPSTREAM_CONCURRENCY_LIMIT -> safeIncrement(
                    series.sheds.get(ShedReason.UPSTREAM_CONCURRENCY_LIMIT));
            default -> {
            }
        }
    }

    private final class MeterSeries {
        private final SeriesKey key;
        private final Tags baseTags;
        private final AtomicInteger inflight = new AtomicInteger();
        private final AtomicInteger health = new AtomicInteger(1);
        private final Set<Meter> meters = ConcurrentHashMap.newKeySet();
        private final ConcurrentMap<TerminalKey, TerminalMeters> terminalMeters = new ConcurrentHashMap<>();
        private final Map<AttemptKey, Counter> attempts = new LinkedHashMap<>();
        private final Map<RetryReason, Counter> retries = new LinkedHashMap<>();
        private final Map<LimitKey, Counter> limitRejections = new LinkedHashMap<>();
        private final Map<ShedReason, Counter> sheds = new LinkedHashMap<>();
        private final Timer latency;
        private final DistributionSummary requestBytes;
        private final DistributionSummary responseBytes;
        private final Counter cooldownTrips;
        private volatile boolean configured;

        private MeterSeries(SeriesKey key) {
            this.key = key;
            this.baseTags = Tags.of("route", key.route, "upstream", key.upstream);
            Meter inflightMeter = register(() -> Gauge.builder(INFLIGHT, inflight, AtomicInteger::doubleValue)
                    .tags(baseTags).register(registry));
            track(inflightMeter);
            Meter healthMeter = register(() -> Gauge.builder(HEALTH, health, AtomicInteger::doubleValue)
                    .tags(baseTags).register(registry));
            track(healthMeter);
            Meter latencyMeter = register(() -> Timer.builder(LATENCY)
                    .tags(baseTags)
                    .serviceLevelObjectives(LATENCY_BUCKETS)
                    .register(registry));
            this.latency = latencyMeter instanceof Timer timer ? timer : null;
            track(latencyMeter);
            Meter requestBytesMeter = register(() -> DistributionSummary.builder(REQUEST_BYTES)
                    .tags(baseTags).baseUnit("bytes").register(registry));
            this.requestBytes = requestBytesMeter instanceof DistributionSummary summary ? summary : null;
            track(requestBytesMeter);
            Meter responseBytesMeter = register(() -> DistributionSummary.builder(RESPONSE_BYTES)
                    .tags(baseTags).baseUnit("bytes").register(registry));
            this.responseBytes = responseBytesMeter instanceof DistributionSummary summary ? summary : null;
            track(responseBytesMeter);
            this.cooldownTrips = counter(COOLDOWN_TRIPS, baseTags, meters);
            registerBoundedCounters();
            terminalMeters.put(
                    new TerminalKey(NONE, TerminalOutcome.OTHER),
                    registerTerminalMeters(new TerminalKey(NONE, TerminalOutcome.OTHER)));
        }

        private void registerBoundedCounters() {
            for (AttemptKind kind : AttemptKind.values()) {
                for (RetryReason reason : RetryReason.values()) {
                    AttemptKey key = new AttemptKey(kind, reason);
                    attempts.put(key, counter(ATTEMPTS,
                            baseTags.and("kind", kind.name(), "reason", reason.name()), meters));
                }
            }
            for (RetryReason reason : RetryReason.values()) {
                retries.put(reason, counter(RETRIES, baseTags.and("reason", reason.name()), meters));
            }
            for (LimitDirection direction : LimitDirection.values()) {
                for (LimitPhase phase : LimitPhase.values()) {
                    LimitKey key = new LimitKey(direction, phase);
                    limitRejections.put(key, counter(LIMIT_REJECTIONS,
                            baseTags.and("direction", direction.name(), "phase", phase.name()), meters));
                }
            }
            for (ShedReason reason : ShedReason.values()) {
                sheds.put(reason, counter(SHEDS, baseTags.and("reason", reason.name()), meters));
            }
        }

        private TerminalMeters registerTerminalMeters(TerminalKey terminalKey) {
            Counter requests = counter(REQUESTS,
                    baseTags.and(
                            "status_class", terminalKey.statusClass,
                            "outcome", terminalKey.outcome.name()),
                    meters);
            return new TerminalMeters(requests);
        }

        private void track(Meter meter) {
            if (meter != null) {
                meters.add(meter);
            }
        }

        private void removeMeters() {
            for (Meter meter : List.copyOf(meters)) {
                try {
                    registry.remove(meter);
                } catch (RuntimeException exception) {
                    // Meter cleanup failure must not affect reload or request completion.
                }
            }
            meters.clear();
            terminalMeters.clear();
        }
    }

    private record SeriesKey(String route, String upstream) {
        private SeriesKey {
            route = safeLogicalTag(route, UNMATCHED);
            upstream = safeLogicalTag(upstream, OTHER);
        }

        private static String safeLogicalTag(String value, String fallback) {
            if (value == null || value.isBlank() || value.length() > 64
                    || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) {
                return fallback;
            }
            return value;
        }
    }

    private record TerminalKey(String statusClass, TerminalOutcome outcome) {
        private TerminalKey {
            statusClass = Set.of("1xx", "2xx", "3xx", "4xx", "5xx", NONE, OTHER)
                    .contains(statusClass) ? statusClass : OTHER;
            outcome = outcome == null ? TerminalOutcome.OTHER : outcome;
        }
    }

    private record TerminalMeters(Counter requests) {
    }

    private record AttemptKey(AttemptKind kind, RetryReason reason) {
    }

    private record LimitKey(LimitDirection direction, LimitPhase phase) {
    }

    @FunctionalInterface
    private interface MeterRegistration {
        Meter register();
    }

    private static final class UpstreamCounterState {
        private final LongAdder forwarded = new LongAdder();
        private final LongAdder failures = new LongAdder();
        private final LongAdder retryAttempts = new LongAdder();
        private final LongAdder cooldownActivations = new LongAdder();
    }
}
