package com.richmond423.loadbalancerpro.api.proxy;

import java.util.List;

public record ReverseProxyStatusResponse(
        boolean proxyEnabled,
        String strategy,
        HealthCheckStatus healthCheck,
        RetryStatus retry,
        CooldownStatus cooldown,
        List<RouteStatus> routes,
        List<UpstreamStatus> upstreams,
        ReverseProxyMetricsSnapshot metrics) {

    public record HealthCheckStatus(
            boolean enabled,
            String path,
            long timeoutMillis,
            long intervalMillis) {
    }

    public record RetryStatus(
            boolean enabled,
            int maxAttempts,
            boolean retryNonIdempotent,
            List<String> methods,
            List<Integer> retryStatuses) {
    }

    public record CooldownStatus(
            boolean enabled,
            int consecutiveFailureThreshold,
            long durationMillis,
            boolean recoverOnSuccessfulHealthCheck) {
    }

    public record RouteStatus(
            String name,
            String pathPrefix,
            String strategy,
            List<String> targetIds) {
    }

    public record UpstreamStatus(
            String id,
            String url,
            boolean configuredHealthy,
            boolean effectiveHealthy,
            String healthSource,
            Integer lastProbeStatusCode,
            String lastProbeOutcome,
            int consecutiveFailures,
            boolean cooldownActive,
            long cooldownRemainingMillis) {
    }
}
