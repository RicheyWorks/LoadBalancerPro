package com.richmond423.loadbalancerpro.api.proxy;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.stereotype.Component;

@Component
public class ReverseProxyMetrics {
    private static final List<String> STATUS_CLASSES = List.of("2xx", "3xx", "4xx", "5xx", "other");

    private final LongAdder totalForwarded = new LongAdder();
    private final LongAdder totalFailures = new LongAdder();
    private final LongAdder totalRetryAttempts = new LongAdder();
    private final LongAdder totalCooldownActivations = new LongAdder();
    private final ConcurrentMap<String, UpstreamCounterState> upstreamCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> statusClassCounters = new ConcurrentHashMap<>();
    private final AtomicReference<String> lastSelectedUpstream = new AtomicReference<>("none");

    public ReverseProxyMetrics() {
        STATUS_CLASSES.forEach(statusClass -> statusClassCounters.put(statusClass, new LongAdder()));
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
        }
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

    private UpstreamCounterState countersFor(String upstreamId) {
        return upstreamCounters.computeIfAbsent(upstreamId, ignored -> new UpstreamCounterState());
    }

    private static String normalizeUpstreamId(String upstreamId) {
        return upstreamId == null ? "" : upstreamId.trim();
    }

    private static String statusClass(int statusCode) {
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
        return "other";
    }

    private static final class UpstreamCounterState {
        private final LongAdder forwarded = new LongAdder();
        private final LongAdder failures = new LongAdder();
        private final LongAdder retryAttempts = new LongAdder();
        private final LongAdder cooldownActivations = new LongAdder();
    }
}
