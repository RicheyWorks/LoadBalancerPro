package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class UpstreamRuntimeStatsTest {
    private static final Instant START = Instant.parse("2026-07-30T00:00:00Z");

    @Test
    void concurrentCompletionsReturnInFlightToZeroAndPreserveBoundedCounts() throws Exception {
        UpstreamRuntimeStats stats = new UpstreamRuntimeStats(Clock.fixed(START, ZoneOffset.UTC));
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> completions = new ArrayList<>();

        try {
            for (int request = 0; request < 2_000; request++) {
                int requestNumber = request;
                completions.add(executor.submit(() -> {
                    stats.requestStarted();
                    stats.requestCompleted(
                            Duration.ofMillis(1 + requestNumber % 100),
                            requestNumber % 5 != 0);
                }));
            }
            for (Future<?> completion : completions) {
                completion.get();
            }
        } finally {
            executor.shutdownNow();
        }

        UpstreamRuntimeStats.Snapshot snapshot = stats.snapshot();
        assertEquals(0, snapshot.inFlightRequestCount());
        assertEquals(2_000, snapshot.completedRequestCount());
        assertEquals(UpstreamRuntimeStats.LATENCY_WINDOW_SIZE, snapshot.latencySampleCount());
        assertEquals(1_600, snapshot.recentSuccessCount());
        assertEquals(400, snapshot.recentFailureCount());
        assertEquals(0.2, snapshot.recentErrorRate(), 0.000_001);
        assertNotNull(snapshot.lastUpdatedAt());
    }

    @Test
    void latencyWindowRetainsOnlyTheNewestSamplesAndUsesNearestRankPercentiles() {
        UpstreamRuntimeStats stats = new UpstreamRuntimeStats(Clock.fixed(START, ZoneOffset.UTC));

        for (int latencyMillis = 1; latencyMillis <= 300; latencyMillis++) {
            stats.requestStarted();
            stats.requestCompleted(Duration.ofMillis(latencyMillis), true);
        }

        UpstreamRuntimeStats.Snapshot snapshot = stats.snapshot();
        assertEquals(300, snapshot.completedRequestCount());
        assertEquals(256, snapshot.latencySampleCount());
        assertEquals(172.0, snapshot.p50LatencyMillis());
        assertEquals(288.0, snapshot.p95LatencyMillis());
        assertEquals(298.0, snapshot.p99LatencyMillis());
    }

    @Test
    void outcomeWindowExpiresCompletedRequestsAfterThirtySeconds() {
        MutableClock clock = new MutableClock(START);
        UpstreamRuntimeStats stats = new UpstreamRuntimeStats(clock);

        stats.requestStarted();
        stats.requestCompleted(Duration.ofMillis(10), true);
        stats.requestStarted();
        stats.requestCompleted(Duration.ofMillis(20), false);

        clock.advance(Duration.ofSeconds(29));
        assertEquals(1, stats.snapshot().recentSuccessCount());
        assertEquals(1, stats.snapshot().recentFailureCount());

        clock.advance(Duration.ofSeconds(1));
        assertEquals(0, stats.snapshot().recentSuccessCount());
        assertEquals(0, stats.snapshot().recentFailureCount());
        assertEquals(0.0, stats.snapshot().recentErrorRate());
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now;

        private MutableClock(Instant initial) {
            now = new AtomicReference<>(initial);
        }

        private void advance(Duration duration) {
            now.updateAndGet(current -> current.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }
}
