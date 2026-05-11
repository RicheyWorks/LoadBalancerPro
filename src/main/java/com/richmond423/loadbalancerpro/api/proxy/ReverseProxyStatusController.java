package com.richmond423.loadbalancerpro.api.proxy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proxy")
public class ReverseProxyStatusController {
    private final ReverseProxyProperties properties;
    private final ReverseProxyMetrics metrics;
    private final ObjectProvider<ReverseProxyService> reverseProxyService;

    public ReverseProxyStatusController(ReverseProxyProperties properties,
                                        ReverseProxyMetrics metrics,
                                        ObjectProvider<ReverseProxyService> reverseProxyService) {
        this.properties = properties;
        this.metrics = metrics;
        this.reverseProxyService = reverseProxyService;
    }

    @GetMapping("/status")
    public ReverseProxyStatusResponse status() {
        ReverseProxyService service = reverseProxyService.getIfAvailable();
        if (service != null) {
            return service.statusSnapshot();
        }
        List<String> upstreamIds = properties.getUpstreams().stream()
                .map(ReverseProxyStatusController::safeUpstreamId)
                .filter(id -> !id.isEmpty())
                .toList();
        return new ReverseProxyStatusResponse(
                false,
                properties.getStrategy(),
                healthCheckStatus(),
                retryStatus(),
                cooldownStatus(),
                properties.getUpstreams().stream()
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
                        .toList(),
                metrics.snapshot(upstreamIds));
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
}
