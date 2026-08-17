package com.richmond423.loadbalancerpro.telemetry;

/**
 * Bounded, thread-safe rolling latency samples with exact nearest-rank
 * percentiles.
 */
public interface RankedLatencyWindow {
    /**
     * Records one non-negative latency sample, evicting the oldest sample when
     * the window is full.
     *
     * @param latencyNanos latency in nanoseconds
     */
    void record(long latencyNanos);

    /**
     * Returns the exact nearest-rank percentile, or {@code 0} when empty.
     *
     * @param percentile percentile in the inclusive range {@code [0, 100]}
     * @return latency in nanoseconds
     */
    long percentileNanos(double percentile);

    default long p50() {
        return percentileNanos(50.0);
    }

    default long p95() {
        return percentileNanos(95.0);
    }

    default long p99() {
        return percentileNanos(99.0);
    }

    int size();

    void clear();
}
