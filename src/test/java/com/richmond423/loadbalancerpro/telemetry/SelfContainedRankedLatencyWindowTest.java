package com.richmond423.loadbalancerpro.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

class SelfContainedRankedLatencyWindowTest {
    private static final double[] PERCENTILES = {0.0, 50.0, 90.0, 95.0, 99.0, 100.0};

    @Test
    void seededStreamsMatchBruteForceNearestRankOracle() {
        Random random = new Random(0x5EED_C0DEL);

        for (int capacity : List.of(1, 2, 3, 7, 16, 64, 256)) {
            RankedLatencyWindow window = new SelfContainedRankedLatencyWindow(capacity);
            Deque<Long> oracle = new ArrayDeque<>();
            for (int sample = 0; sample < 3_000; sample++) {
                long latency = random.nextInt(1_000);
                window.record(latency);
                if (oracle.size() == capacity) {
                    oracle.removeFirst();
                }
                oracle.addLast(latency);

                if (sample % 7 == 0 || sample == 2_999) {
                    assertEquals(oracle.size(), window.size());
                    for (double percentile : PERCENTILES) {
                        assertEquals(nearestRank(oracle, percentile),
                                window.percentileNanos(percentile),
                                "capacity=" + capacity + ", sample=" + sample
                                        + ", percentile=" + percentile);
                    }
                }
            }
        }
    }

    @Test
    void duplicatesEvictIndependentlyAndClearResetsTheWindow() {
        RankedLatencyWindow window = new SelfContainedRankedLatencyWindow(3);
        window.record(10);
        window.record(10);
        window.record(100);

        assertEquals(10, window.p50());
        window.record(20);
        assertEquals(20, window.p50());
        assertEquals(100, window.p99());

        window.clear();
        assertEquals(0, window.size());
        assertEquals(0, window.p95());
    }

    @Test
    void rejectsInvalidCapacitySamplesAndPercentiles() {
        assertThrows(IllegalArgumentException.class,
                () -> new SelfContainedRankedLatencyWindow(0));

        RankedLatencyWindow window = new SelfContainedRankedLatencyWindow(4);
        assertThrows(IllegalArgumentException.class, () -> window.record(-1));
        assertThrows(IllegalArgumentException.class, () -> window.percentileNanos(-0.01));
        assertThrows(IllegalArgumentException.class, () -> window.percentileNanos(100.01));
        assertThrows(IllegalArgumentException.class,
                () -> window.percentileNanos(Double.NaN));
    }

    @Test
    void concurrentReadersAndWritersPreserveTheBound() throws Exception {
        int capacity = 256;
        RankedLatencyWindow window = new SelfContainedRankedLatencyWindow(capacity);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<?>> tasks = new ArrayList<>();

        try {
            for (int writer = 0; writer < 8; writer++) {
                int writerId = writer;
                tasks.add(executor.submit(() -> {
                    for (int sample = 0; sample < 2_000; sample++) {
                        window.record(writerId * 10_000L + sample);
                    }
                }));
            }
            for (int reader = 0; reader < 2; reader++) {
                tasks.add(executor.submit(() -> {
                    for (int read = 0; read < 2_000; read++) {
                        long p50 = window.p50();
                        long p99 = window.p99();
                        assertTrue(p50 >= 0);
                        assertTrue(p99 >= 0);
                    }
                }));
            }
            for (Future<?> task : tasks) {
                task.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(capacity, window.size());
        assertTrue(window.p99() >= window.p50());
    }

    private static long nearestRank(Deque<Long> samples, double percentile) {
        List<Long> sorted = new ArrayList<>(samples);
        sorted.sort(Long::compareTo);
        int rank = Math.max(0, (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1);
        return sorted.get(rank);
    }
}
