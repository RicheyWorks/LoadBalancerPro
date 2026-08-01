package com.richmond423.loadbalancerpro.api.proxy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;

/**
 * Bounded process-local measurements for one configured upstream id.
 */
final class UpstreamRuntimeStats {
    static final int LATENCY_WINDOW_SIZE = 256;
    static final int ERROR_WINDOW_SECONDS = 30;
    private static final double EWMA_ALPHA = 0.2;
    private static final Snapshot EMPTY_SNAPSHOT =
            new Snapshot(0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0, null);

    private final Clock clock;
    private final AtomicInteger inFlight = new AtomicInteger();
    private final Object windowLock = new Object();
    private final long[] latencyNanos = new long[LATENCY_WINDOW_SIZE];
    private final long[] bucketEpochSeconds = new long[ERROR_WINDOW_SECONDS];
    private final long[] bucketSuccesses = new long[ERROR_WINDOW_SECONDS];
    private final long[] bucketFailures = new long[ERROR_WINDOW_SECONDS];

    private int latencyCursor;
    private int latencySampleCount;
    private long completedRequestCount;
    private double ewmaLatencyMillis;
    private Instant lastUpdatedAt;

    UpstreamRuntimeStats(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        Arrays.fill(bucketEpochSeconds, Long.MIN_VALUE);
    }

    void requestStarted() {
        if (!tryRequestStarted(0, ignored -> true)) {
            throw new IllegalStateException("in-flight request counter is saturated");
        }
    }

    boolean tryRequestStarted(int maxInFlight, IntPredicate admissionForProspectiveCount) {
        if (maxInFlight < 0) {
            throw new IllegalArgumentException("maxInFlight must be non-negative");
        }
        Objects.requireNonNull(admissionForProspectiveCount, "admissionForProspectiveCount cannot be null");
        while (true) {
            int current = inFlight.get();
            if (current == Integer.MAX_VALUE) {
                return false;
            }
            int prospective = current + 1;
            if (maxInFlight > 0 && prospective > maxInFlight) {
                return false;
            }
            if (!admissionForProspectiveCount.test(prospective)) {
                return false;
            }
            if (inFlight.compareAndSet(current, prospective)) {
                return true;
            }
        }
    }

    void requestCompleted(Duration latency, boolean successful) {
        try {
            long measuredNanos = safeNanos(latency);
            Instant completedAt = clock.instant();
            synchronized (windowLock) {
                latencyNanos[latencyCursor] = measuredNanos;
                latencyCursor = (latencyCursor + 1) % LATENCY_WINDOW_SIZE;
                latencySampleCount = Math.min(LATENCY_WINDOW_SIZE, latencySampleCount + 1);
                completedRequestCount++;

                double latencyMillis = nanosToMillis(measuredNanos);
                ewmaLatencyMillis = completedRequestCount == 1
                        ? latencyMillis
                        : EWMA_ALPHA * latencyMillis + (1.0 - EWMA_ALPHA) * ewmaLatencyMillis;

                long epochSecond = completedAt.getEpochSecond();
                int bucket = (int) Math.floorMod(epochSecond, ERROR_WINDOW_SECONDS);
                if (bucketEpochSeconds[bucket] != epochSecond) {
                    bucketEpochSeconds[bucket] = epochSecond;
                    bucketSuccesses[bucket] = 0;
                    bucketFailures[bucket] = 0;
                }
                if (successful) {
                    bucketSuccesses[bucket]++;
                } else {
                    bucketFailures[bucket]++;
                }
                lastUpdatedAt = completedAt;
            }
        } finally {
            inFlight.updateAndGet(current -> Math.max(0, current - 1));
        }
    }

    void requestAborted() {
        inFlight.updateAndGet(current -> Math.max(0, current - 1));
    }

    Snapshot snapshot() {
        synchronized (windowLock) {
            long[] samples = Arrays.copyOf(latencyNanos, latencySampleCount);
            Arrays.sort(samples);

            long nowEpochSecond = clock.instant().getEpochSecond();
            long recentSuccesses = 0;
            long recentFailures = 0;
            for (int bucket = 0; bucket < ERROR_WINDOW_SECONDS; bucket++) {
                long ageSeconds = nowEpochSecond - bucketEpochSeconds[bucket];
                if (ageSeconds >= 0 && ageSeconds < ERROR_WINDOW_SECONDS) {
                    recentSuccesses += bucketSuccesses[bucket];
                    recentFailures += bucketFailures[bucket];
                }
            }
            long recentRequests = recentSuccesses + recentFailures;
            double recentErrorRate = recentRequests == 0
                    ? 0.0
                    : (double) recentFailures / recentRequests;

            return new Snapshot(
                    inFlight.get(),
                    completedRequestCount,
                    latencySampleCount,
                    ewmaLatencyMillis,
                    percentileMillis(samples, 0.50),
                    percentileMillis(samples, 0.95),
                    percentileMillis(samples, 0.99),
                    recentSuccesses,
                    recentFailures,
                    recentErrorRate,
                    lastUpdatedAt);
        }
    }

    long completedRequestCount() {
        synchronized (windowLock) {
            return completedRequestCount;
        }
    }

    static Snapshot emptySnapshot() {
        return EMPTY_SNAPSHOT;
    }

    private static long safeNanos(Duration latency) {
        if (latency == null || latency.isNegative()) {
            return 0;
        }
        try {
            return latency.toNanos();
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private static double percentileMillis(long[] sortedSamples, double percentile) {
        if (sortedSamples.length == 0) {
            return 0.0;
        }
        int index = Math.max(0, (int) Math.ceil(percentile * sortedSamples.length) - 1);
        return nanosToMillis(sortedSamples[index]);
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    record Snapshot(
            int inFlightRequestCount,
            long completedRequestCount,
            int latencySampleCount,
            double ewmaLatencyMillis,
            double p50LatencyMillis,
            double p95LatencyMillis,
            double p99LatencyMillis,
            long recentSuccessCount,
            long recentFailureCount,
            double recentErrorRate,
            Instant lastUpdatedAt) {
    }
}
