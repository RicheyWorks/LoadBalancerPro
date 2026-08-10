package com.richmond423.loadbalancerpro.api.proxy;

import java.util.List;

public record ReverseProxyStatusResponse(
        boolean proxyEnabled,
        String strategy,
        HealthCheckStatus healthCheck,
        RetryStatus retry,
        CooldownStatus cooldown,
        LimitsStatus limits,
        LoadSheddingStatus shedding,
        List<RouteStatus> routes,
        List<UpstreamStatus> upstreams,
        List<DnsDiscoveryStatus> dnsDiscovery,
        ReverseProxyMetricsSnapshot metrics,
        ObservabilitySummary observability,
        SecurityBoundaryStatus securityBoundary,
        PrivateNetworkLiveValidationStatusResponse privateNetworkLiveValidation,
        ReloadStatus reload) {

    public record HealthCheckStatus(
            boolean enabled,
            String path,
            long timeoutMillis,
            long intervalMillis,
            int healthyThreshold,
            int unhealthyThreshold) {
    }

    public record RetryStatus(
            boolean enabled,
            int maxAttempts,
            int budgetPercent,
            long backoffBaseMillis,
            long backoffMaxMillis,
            long budgetPrimaryRequests,
            long budgetGrantedRetries,
            long budgetRejectedRetries,
            int budgetAvailableCreditsPercent,
            boolean retryNonIdempotent,
            List<String> methods,
            List<Integer> retryStatuses) {
    }

    public record CooldownStatus(
            boolean enabled,
            int consecutiveFailureThreshold,
            long durationMillis,
            boolean recoverOnSuccessfulHealthCheck,
            long slowStartDurationMillis) {
    }

    public record LimitsStatus(
            int configuredMaxInFlight,
            int effectiveMaxInFlight,
            int currentInFlight,
            boolean adaptiveEnabled,
            String lastAdaptiveAction,
            String lastAdaptiveReason,
            String lastAdaptiveUpdate) {
    }

    public record LoadSheddingStatus(
            boolean enabled,
            double softUtilizationThreshold,
            double hardUtilizationThreshold,
            int maxQueueDepth,
            double maxP95LatencyMillis,
            double maxErrorRate,
            boolean criticalBypassEnabled,
            boolean shedUserOnHardPressure,
            String priorityHeader,
            int retryAfterSeconds) {
    }

    public record RouteStatus(
            String name,
            String pathPrefix,
            String hostMatch,
            List<String> headerMatchNames,
            List<SplitStatus> splits,
            String strategy,
            String hashOn,
            boolean affinityEnabled,
            List<String> targetIds) {

        public RouteStatus(
                String name,
                String pathPrefix,
                String strategy,
                String hashOn,
                boolean affinityEnabled,
                List<String> targetIds) {
            this(name, pathPrefix, null, List.of(), List.of(),
                    strategy, hashOn, affinityEnabled, targetIds);
        }
    }

    public record SplitStatus(
            String name,
            int percentage,
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
            long cooldownRemainingMillis,
            double configuredWeight,
            double effectiveWeight,
            boolean slowStartActive,
            long slowStartRemainingMillis,
            int maxInFlight,
            UpstreamRuntimeStatus runtimeStats) {
    }

    public record UpstreamRuntimeStatus(
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
            String lastUpdatedAt) {
    }

    public record DnsDiscoveryStatus(
            String logicalUpstreamId,
            String name,
            int port,
            String authorityMode,
            String outcome,
            boolean lookupInFlight,
            int memberCount,
            long lastSuccessAgeMillis,
            long staleRemainingMillis,
            List<DnsMemberStatus> members) {
    }

    public record DnsMemberStatus(
            String id,
            String address) {
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

    public record ReloadStatus(
            boolean configReloadSupported,
            long activeConfigGeneration,
            String lastReloadAttemptedAt,
            String lastReloadSucceededAt,
            String lastReloadFailedAt,
            String lastReloadStatus,
            List<String> lastReloadValidationErrors,
            int activeRouteCount,
            int activeBackendTargetCount) {
    }
}
