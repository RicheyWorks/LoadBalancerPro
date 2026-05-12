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
        ReverseProxyMetricsSnapshot metrics,
        ObservabilitySummary observability,
        SecurityBoundaryStatus securityBoundary) {

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

    public record ObservabilitySummary(
            int routeCount,
            int backendTargetCount,
            int effectiveHealthyBackendCount,
            int effectiveUnhealthyBackendCount,
            int cooldownActiveBackendCount,
            long totalForwarded,
            long totalFailures,
            long totalRetryAttempts,
            long totalCooldownActivations,
            String lastSelectedUpstream,
            String readiness) {
    }

    public record SecurityBoundaryStatus(
            String authMode,
            List<String> activeProfiles,
            boolean apiKeyConfigured,
            boolean proxyStatusProtected,
            boolean proxyForwardingProtected,
            String note) {
    }
}
