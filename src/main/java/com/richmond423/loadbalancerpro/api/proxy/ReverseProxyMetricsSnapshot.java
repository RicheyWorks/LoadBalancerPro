package com.richmond423.loadbalancerpro.api.proxy;

import java.util.List;
import java.util.Map;

public record ReverseProxyMetricsSnapshot(
        long totalForwarded,
        long totalFailures,
        Map<String, Long> statusClassCounts,
        String lastSelectedUpstream,
        List<UpstreamCounters> upstreams) {

    public record UpstreamCounters(String upstreamId, long forwarded, long failures) {
    }
}
