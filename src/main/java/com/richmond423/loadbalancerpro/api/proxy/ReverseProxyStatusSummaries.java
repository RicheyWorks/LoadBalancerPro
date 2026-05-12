package com.richmond423.loadbalancerpro.api.proxy;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

final class ReverseProxyStatusSummaries {
    private ReverseProxyStatusSummaries() {
    }

    static ReverseProxyStatusResponse.ObservabilitySummary observability(
            boolean proxyEnabled,
            List<ReverseProxyStatusResponse.RouteStatus> routes,
            List<ReverseProxyStatusResponse.UpstreamStatus> upstreams,
            ReverseProxyMetricsSnapshot metrics) {
        List<ReverseProxyStatusResponse.RouteStatus> safeRoutes = routes == null ? List.of() : routes;
        List<ReverseProxyStatusResponse.UpstreamStatus> safeUpstreams = upstreams == null ? List.of() : upstreams;
        ReverseProxyMetricsSnapshot safeMetrics = metrics == null
                ? new ReverseProxyMetricsSnapshot(0, 0, 0, 0, java.util.Map.of(), null, List.of())
                : metrics;
        int backendTargetCount = safeRoutes.stream()
                .map(ReverseProxyStatusResponse.RouteStatus::targetIds)
                .mapToInt(targetIds -> targetIds == null ? 0 : targetIds.size())
                .sum();
        int effectiveHealthyBackendCount = (int) safeUpstreams.stream()
                .filter(ReverseProxyStatusResponse.UpstreamStatus::effectiveHealthy)
                .count();
        int effectiveUnhealthyBackendCount = safeUpstreams.size() - effectiveHealthyBackendCount;
        int cooldownActiveBackendCount = (int) safeUpstreams.stream()
                .filter(ReverseProxyStatusResponse.UpstreamStatus::cooldownActive)
                .count();

        return new ReverseProxyStatusResponse.ObservabilitySummary(
                safeRoutes.size(),
                backendTargetCount,
                effectiveHealthyBackendCount,
                effectiveUnhealthyBackendCount,
                cooldownActiveBackendCount,
                safeMetrics.totalForwarded(),
                safeMetrics.totalFailures(),
                safeMetrics.totalRetryAttempts(),
                safeMetrics.totalCooldownActivations(),
                safeMetrics.lastSelectedUpstream(),
                readiness(proxyEnabled, safeRoutes.size(), backendTargetCount, effectiveHealthyBackendCount,
                        cooldownActiveBackendCount, safeMetrics));
    }

    static ReverseProxyStatusResponse.SecurityBoundaryStatus securityBoundary(
            Environment environment,
            String configuredApiKey) {
        String authMode = normalizeAuthMode(environment == null
                ? "api-key"
                : environment.getProperty("loadbalancerpro.auth.mode", "api-key"));
        List<String> activeProfiles = activeProfiles(environment);
        boolean prodLikeProfile = activeProfiles.stream()
                .anyMatch(profile -> "prod".equals(profile) || "cloud-sandbox".equals(profile));
        boolean oauth2Mode = "oauth2".equals(authMode);
        boolean apiKeyBoundary = "api-key".equals(authMode) && prodLikeProfile;
        boolean protectedBoundary = oauth2Mode || apiKeyBoundary;
        boolean apiKeyConfigured = StringUtils.hasText(configuredApiKey);

        return new ReverseProxyStatusResponse.SecurityBoundaryStatus(
                authMode,
                activeProfiles,
                apiKeyConfigured,
                protectedBoundary,
                protectedBoundary,
                boundaryNote(oauth2Mode, apiKeyBoundary));
    }

    static ReverseProxyStatusResponse.SecurityBoundaryStatus controllerNotAvailableSecurityBoundary() {
        return new ReverseProxyStatusResponse.SecurityBoundaryStatus(
                "not-reported",
                List.of(),
                false,
                false,
                false,
                "Security boundary details are reported by the /api/proxy/status controller.");
    }

    private static String readiness(boolean proxyEnabled,
                                    int routeCount,
                                    int backendTargetCount,
                                    int effectiveHealthyBackendCount,
                                    int cooldownActiveBackendCount,
                                    ReverseProxyMetricsSnapshot metrics) {
        if (!proxyEnabled) {
            return "proxy_disabled";
        }
        if (routeCount == 0 || backendTargetCount == 0) {
            return "proxy_enabled_without_configured_targets";
        }
        if (effectiveHealthyBackendCount == 0) {
            return "no_effective_healthy_backends";
        }
        if (cooldownActiveBackendCount > 0) {
            return "cooldown_active";
        }
        if (metrics.totalFailures() > 0) {
            return "request_failures_observed";
        }
        if (metrics.totalRetryAttempts() > 0) {
            return "retries_observed";
        }
        return "ready";
    }

    private static List<String> activeProfiles(Environment environment) {
        if (environment == null || environment.getActiveProfiles().length == 0) {
            return List.of("default");
        }
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile == null ? "" : profile.trim())
                .filter(profile -> !profile.isEmpty())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private static String normalizeAuthMode(String authMode) {
        String normalized = authMode == null ? "api-key" : authMode.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? "api-key" : normalized;
    }

    private static String boundaryNote(boolean oauth2Mode, boolean apiKeyBoundary) {
        if (oauth2Mode) {
            return "OAuth2 mode protects /proxy/** and GET /api/proxy/status with the configured operator role.";
        }
        if (apiKeyBoundary) {
            return "Prod/cloud-sandbox API-key mode protects /proxy/** and GET /api/proxy/status; apiKeyConfigured reports presence only.";
        }
        return "Local/default API-key mode permits demo surfaces; use deployment-level access control and TLS termination before exposure.";
    }
}
