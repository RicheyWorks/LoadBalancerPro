package com.richmond423.loadbalancerpro.api.proxy;

import java.util.List;

public record ReverseProxyStatusResponse(
        boolean proxyEnabled,
        String strategy,
        HealthCheckStatus healthCheck,
        List<UpstreamStatus> upstreams,
        ReverseProxyMetricsSnapshot metrics) {

    public record HealthCheckStatus(
            boolean enabled,
            String path,
            long timeoutMillis,
            long intervalMillis) {
    }

    public record UpstreamStatus(
            String id,
            String url,
            boolean configuredHealthy,
            boolean effectiveHealthy,
            String healthSource,
            Integer lastProbeStatusCode,
            String lastProbeOutcome) {
    }
}
