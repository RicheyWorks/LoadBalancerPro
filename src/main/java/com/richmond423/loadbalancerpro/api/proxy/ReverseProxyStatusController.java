package com.richmond423.loadbalancerpro.api.proxy;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/proxy")
public class ReverseProxyStatusController {
    private static final String API_KEY_HEADER = "X-API-Key";

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
                ReverseProxyStatusSummaries.controllerNotAvailableSecurityBoundary(),
                PrivateNetworkLiveValidationStatusResponse.from(properties),
                reloadNotSupported()));
    }

    @PostMapping("/reload")
    public ResponseEntity<ReverseProxyReloadResponse> reload(@RequestBody ReverseProxyProperties candidate,
                                                             HttpServletRequest request) {
        if (!oauth2Mode() && !validApiKey(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(reloadRejected("unauthorized", List.of("X-API-Key is required for proxy config reload")));
        }
        ReverseProxyService service = reverseProxyService.getIfAvailable();
        if (service == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ReverseProxyReloadResponse(
                            false,
                            "unsupported",
                            0,
                            0,
                            0,
                            List.of("Proxy mode must be enabled at startup before runtime reload is available."),
                            reloadNotSupported()));
        }
        ReverseProxyReloadResponse response = service.reload(candidate);
        return ResponseEntity.status(response.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PostMapping("/private-network-live-validation")
    public ResponseEntity<PrivateNetworkLiveValidationCommandResponse> privateNetworkLiveValidationCommand(
            @RequestBody(required = false) PrivateNetworkLiveValidationCommandRequest request) {
        ReverseProxyService service = reverseProxyService.getIfAvailable();
        PrivateNetworkLiveValidationCommandResponse response = service == null
                ? PrivateNetworkLiveValidationCommandResponse.from(properties, request)
                : service.privateNetworkLiveValidationCommand(request);
        return ResponseEntity.status(commandHttpStatus(response)).body(response);
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
                ReverseProxyStatusSummaries.securityBoundary(environment, configuredApiKey),
                response.privateNetworkLiveValidation(),
                response.reload());
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

    private boolean oauth2Mode() {
        String configuredMode = environment.getProperty("loadbalancerpro.auth.mode", "api-key");
        return "oauth2".equals(configuredMode == null
                ? "api-key"
                : configuredMode.trim().replace('_', '-').toLowerCase(Locale.ROOT));
    }

    private boolean validApiKey(HttpServletRequest request) {
        String expected = configuredApiKey == null ? "" : configuredApiKey.trim();
        if (expected.isEmpty()) {
            return false;
        }
        String presented = request.getHeader(API_KEY_HEADER);
        return presented != null && !presented.isBlank()
                && constantTimeEquals(expected.getBytes(StandardCharsets.UTF_8),
                        presented.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean constantTimeEquals(byte[] expected, byte[] actual) {
        return MessageDigest.isEqual(sha256(expected), sha256(actual));
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest algorithm is unavailable", exception);
        }
    }

    private static ReverseProxyReloadResponse reloadRejected(String status, List<String> errors) {
        return new ReverseProxyReloadResponse(
                false,
                status,
                0,
                0,
                0,
                errors,
                reloadNotSupported());
    }

    private static HttpStatus commandHttpStatus(PrivateNetworkLiveValidationCommandResponse response) {
        return "INVALID_REQUEST".equals(response.status()) ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
    }

    private static ReverseProxyStatusResponse.ReloadStatus reloadNotSupported() {
        return new ReverseProxyStatusResponse.ReloadStatus(
                false,
                0,
                null,
                null,
                null,
                "unsupported",
                List.of(),
                0,
                0);
    }
}
