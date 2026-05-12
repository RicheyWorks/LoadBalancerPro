package com.richmond423.loadbalancerpro.api.proxy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proxy")
public class ReverseProxyStatusController {
    private final ReverseProxyProperties properties;
    private final ReverseProxyMetrics metrics;
    private final ObjectProvider<ReverseProxyService> reverseProxyService;
    private final Environment environment;
    private final String configuredApiKey;

    public ReverseProxyStatusController(ReverseProxyProperties properties,
                                        ReverseProxyMetrics metrics,
                                        ObjectProvider<ReverseProxyService> reverseProxyService,
                                        Environment environment,
                                        @Value("${loadbalancerpro.api.key:}") String configuredApiKey) {
        this.properties = properties;
        this.metrics = metrics;
        this.reverseProxyService = reverseProxyService;
        this.environment = environment;
        this.configuredApiKey = configuredApiKey;
    }

    @GetMapping("/status")
    public ReverseProxyStatusResponse status() {
        ReverseProxyService service = reverseProxyService.getIfAvailable();
        if (service != null) {
            return decorate(service.statusSnapshot());
        }
        List<String> upstreamIds = properties.getUpstreams().stream()
                .map(ReverseProxyStatusController::safeUpstreamId)
                .filter(id -> !id.isEmpty())
                .toList();
        List<ReverseProxyStatusResponse.RouteStatus> routes = disabledRouteStatuses();
        List<ReverseProxyStatusResponse.UpstreamStatus> upstreams = properties.getUpstreams().stream()
                .map(upstream -> new ReverseProxyStatusResponse.UpstreamStatus(
                        safeUpstreamId(upstream),
                        safeUrl(upstream.getUrl()),
                        upstream.isHealthy(),
                        upstream.isHealthy(),
                        "CONFIGURED",
                        null,
                        "proxy disabled; active probes not run",
                        0,
                        false,
                        0))
                .toList();
        ReverseProxyMetricsSnapshot metricsSnapshot = metrics.snapshot(upstreamIds);
        return decorate(new ReverseProxyStatusResponse(
                false,
                properties.getStrategy(),
                healthCheckStatus(),
                retryStatus(),
                cooldownStatus(),
                routes,
                upstreams,
                metricsSnapshot,
                ReverseProxyStatusSummaries.observability(false, routes, upstreams, metricsSnapshot),
                ReverseProxyStatusSummaries.controllerNotAvailableSecurityBoundary()));
    }

    private ReverseProxyStatusResponse decorate(ReverseProxyStatusResponse response) {
        return new ReverseProxyStatusResponse(
                response.proxyEnabled(),
                response.strategy(),
                response.healthCheck(),
                response.retry(),
                response.cooldown(),
                response.routes(),
                response.upstreams(),
                response.metrics(),
                ReverseProxyStatusSummaries.observability(response.proxyEnabled(), response.routes(),
                        response.upstreams(), response.metrics()),
                ReverseProxyStatusSummaries.securityBoundary(environment, configuredApiKey));
    }

    private ReverseProxyStatusResponse.HealthCheckStatus healthCheckStatus() {
        ReverseProxyProperties.HealthCheck healthCheck = properties.getHealthCheck();
        return new ReverseProxyStatusResponse.HealthCheckStatus(
                healthCheck.isEnabled(),
                normalizedPath(healthCheck.getPath()),
                healthCheck.getTimeout().toMillis(),
                healthCheck.getInterval().toMillis());
    }

    private ReverseProxyStatusResponse.RetryStatus retryStatus() {
        ReverseProxyProperties.Retry retry = properties.getRetry();
        return new ReverseProxyStatusResponse.RetryStatus(
                retry.isEnabled(),
                Math.max(1, retry.getMaxAttempts()),
                retry.isRetryNonIdempotent(),
                retry.getMethods().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(method -> !method.isEmpty())
                        .map(method -> method.toUpperCase(Locale.ROOT))
                        .sorted()
                        .toList(),
                retry.getRetryStatuses().stream()
                        .filter(Objects::nonNull)
                        .sorted()
                        .toList());
    }

    private ReverseProxyStatusResponse.CooldownStatus cooldownStatus() {
        ReverseProxyProperties.Cooldown cooldown = properties.getCooldown();
        return new ReverseProxyStatusResponse.CooldownStatus(
                cooldown.isEnabled(),
                Math.max(1, cooldown.getConsecutiveFailureThreshold()),
                Math.max(0, cooldown.getDuration().toMillis()),
                cooldown.isRecoverOnSuccessfulHealthCheck());
    }

    private List<ReverseProxyStatusResponse.RouteStatus> disabledRouteStatuses() {
        if (!properties.getRoutes().isEmpty()) {
            List<ReverseProxyStatusResponse.RouteStatus> routes = new ArrayList<>();
            for (Map.Entry<String, ReverseProxyProperties.Route> entry : properties.getRoutes().entrySet()) {
                ReverseProxyProperties.Route route =
                        Objects.requireNonNullElseGet(entry.getValue(), ReverseProxyProperties.Route::new);
                routes.add(new ReverseProxyStatusResponse.RouteStatus(
                        entry.getKey(),
                        safePathPrefix(route.getPathPrefix()),
                        ReverseProxyRoutePlanner.safeRouteStrategy(properties, route),
                        route.getTargets().stream()
                                .map(ReverseProxyStatusController::safeUpstreamId)
                                .filter(id -> !id.isEmpty())
                                .toList()));
            }
            return routes;
        }
        if (!properties.getUpstreams().isEmpty()) {
            return List.of(new ReverseProxyStatusResponse.RouteStatus(
                    ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME,
                    "/",
                    properties.getStrategy(),
                    properties.getUpstreams().stream()
                            .map(ReverseProxyStatusController::safeUpstreamId)
                            .filter(id -> !id.isEmpty())
                            .toList()));
        }
        return List.of();
    }

    private static String safeUpstreamId(ReverseProxyProperties.Upstream upstream) {
        return upstream.getId() == null ? "" : upstream.getId().trim();
    }

    private static String safeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(url.trim());
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null)
                    .toString();
        } catch (IllegalArgumentException | URISyntaxException exception) {
            return "invalid";
        }
    }

    private static String normalizedPath(String path) {
        String value = path == null || path.isBlank() ? "/health" : path.trim();
        return value.startsWith("/") ? value : "/" + value;
    }

    private static String safePathPrefix(String pathPrefix) {
        String value = pathPrefix == null || pathPrefix.isBlank() ? "/" : pathPrefix.trim();
        return value.startsWith("/") ? value : "/" + value;
    }
}
