package com.richmond423.loadbalancerpro.api.proxy;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.richmond423.loadbalancerpro.api.ApiErrorResponse;
import com.richmond423.loadbalancerpro.api.explain.LiveRoutingDecisionExplanation;
import com.richmond423.loadbalancerpro.api.explain.LiveRoutingExplanationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/proxy")
public class ReverseProxyStatusController {
    private static final String API_KEY_HEADER = "X-API-Key";

    private final ReverseProxyProperties properties;
    private final ReverseProxyMetrics metrics;
    private final ObjectProvider<ReverseProxyService> reverseProxyService;
    private final LiveRoutingExplanationService routingExplanationService;
    private final Environment environment;
    private final String configuredApiKey;

    public ReverseProxyStatusController(ReverseProxyProperties properties,
                                        ReverseProxyMetrics metrics,
                                        ObjectProvider<ReverseProxyService> reverseProxyService,
                                        LiveRoutingExplanationService routingExplanationService,
                                        Environment environment,
                                        @Value("${loadbalancerpro.api.key:}") String configuredApiKey) {
        this.properties = properties;
        this.metrics = metrics;
        this.reverseProxyService = reverseProxyService;
        this.routingExplanationService = routingExplanationService;
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
                        0,
                        upstream.getWeight(),
                        upstream.getWeight(),
                        false,
                        0,
                        Math.max(0, upstream.getMaxInFlight()),
                        new ReverseProxyStatusResponse.UpstreamRuntimeStatus(
                                0,
                                0,
                                0,
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                0,
                                0,
                                0.0,
                                null)))
                .toList();
        ReverseProxyMetricsSnapshot metricsSnapshot = metrics.snapshot(upstreamIds);
        return decorate(new ReverseProxyStatusResponse(
                false,
                properties.getStrategy(),
                healthCheckStatus(),
                retryStatus(),
                cooldownStatus(),
                limitsStatus(),
                sheddingStatus(),
                routes,
                upstreams,
                List.of(),
                metricsSnapshot,
                ReverseProxyStatusSummaries.observability(false, routes, upstreams, metricsSnapshot),
                ReverseProxyStatusSummaries.controllerNotAvailableSecurityBoundary(),
                PrivateNetworkLiveValidationStatusResponse.from(properties),
                reloadNotSupported()));
    }

    @GetMapping("/decisions/recent")
    public RecentProxyDecisionsResponse recentDecisions() {
        ReverseProxyService service = reverseProxyService.getIfAvailable();
        return service == null
                ? RecentProxyDecisionsResponse.empty(false, LiveRoutingDecisionStore.DEFAULT_MAX_RETAINED)
                : service.recentDecisionsSnapshot();
    }

    @GetMapping("/decisions/{decisionId}/explain")
    @Operation(
            summary = "Explain one retained live proxy decision",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(
                            schema = @Schema(implementation = LiveRoutingDecisionExplanation.class))),
                    @ApiResponse(responseCode = "404", content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<?> explainDecision(
            @PathVariable("decisionId") String decisionId,
            HttpServletRequest request) {
        ReverseProxyService service = reverseProxyService.getIfAvailable();
        if (service == null) {
            return decisionNotFound(request);
        }
        return service.retainedDecision(decisionId)
                .<ResponseEntity<?>>map(decision -> ResponseEntity.ok(
                        routingExplanationService.explain(decision)))
                .orElseGet(() -> decisionNotFound(request));
    }

    @GetMapping("/config")
    public ResponseEntity<?> adminConfig(HttpServletRequest request) {
        ResponseEntity<ApiErrorResponse> unauthorized = rejectAdminAuthentication(request);
        if (unauthorized != null) {
            return unauthorized;
        }
        ReverseProxyService service = reverseProxyService.getIfAvailable();
        if (service == null) {
            return adminUnavailable(request);
        }
        return ResponseEntity.ok(service.adminConfigSnapshot());
    }

    @PostMapping("/upstreams")
    public ResponseEntity<?> addUpstream(
            @RequestBody(required = false) ReverseProxyUpstreamAddRequest addRequest,
            HttpServletRequest request) {
        ResponseEntity<ApiErrorResponse> unauthorized = rejectAdminAuthentication(request);
        if (unauthorized != null) {
            return unauthorized;
        }
        ReverseProxyService service = reverseProxyService.getIfAvailable();
        if (service == null) {
            return adminUnavailable(request);
        }
        ReverseProxyAdminMutationResponse response = service.addUpstream(addRequest);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    @PatchMapping("/upstreams/{id}")
    public ResponseEntity<?> patchUpstream(
            @PathVariable("id") String upstreamId,
            @RequestBody(required = false) ReverseProxyUpstreamPatchRequest patchRequest,
            HttpServletRequest request) {
        ResponseEntity<ApiErrorResponse> unauthorized = rejectAdminAuthentication(request);
        if (unauthorized != null) {
            return unauthorized;
        }
        ReverseProxyService service = reverseProxyService.getIfAvailable();
        if (service == null) {
            return adminUnavailable(request);
        }
        ReverseProxyAdminMutationResponse response = service.patchUpstream(upstreamId, patchRequest);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    @DeleteMapping("/upstreams/{id}")
    public ResponseEntity<?> deleteUpstream(
            @PathVariable("id") String upstreamId,
            @RequestParam(name = "expectedGeneration", required = false) Long expectedGeneration,
            HttpServletRequest request) {
        ResponseEntity<ApiErrorResponse> unauthorized = rejectAdminAuthentication(request);
        if (unauthorized != null) {
            return unauthorized;
        }
        ReverseProxyService service = reverseProxyService.getIfAvailable();
        if (service == null) {
            return adminUnavailable(request);
        }
        ReverseProxyAdminMutationResponse response = service.deleteUpstream(upstreamId, expectedGeneration);
        return ResponseEntity.status(response.httpStatus()).body(response);
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
                response.limits(),
                response.shedding(),
                response.routes(),
                response.upstreams(),
                response.dnsDiscovery(),
                response.metrics(),
                ReverseProxyStatusSummaries.observability(response.proxyEnabled(), response.routes(),
                        response.upstreams(), response.metrics()),
                ReverseProxyStatusSummaries.securityBoundary(environment, configuredApiKey),
                response.privateNetworkLiveValidation(),
                response.reload());
    }

    private ReverseProxyStatusResponse.LimitsStatus limitsStatus() {
        int configured = Math.max(0, properties.getLimits().getMaxInFlight());
        return new ReverseProxyStatusResponse.LimitsStatus(
                configured, configured, 0, properties.getLimits().isAdaptive(), null, null, null);
    }

    private ReverseProxyStatusResponse.LoadSheddingStatus sheddingStatus() {
        ReverseProxyProperties.Shedding shedding = properties.getShedding();
        return new ReverseProxyStatusResponse.LoadSheddingStatus(
                shedding.isEnabled(),
                shedding.getSoftUtilizationThreshold(),
                shedding.getHardUtilizationThreshold(),
                shedding.getMaxQueueDepth(),
                shedding.getMaxP95LatencyMillis(),
                shedding.getMaxErrorRate(),
                shedding.isCriticalBypassEnabled(),
                shedding.isShedUserOnHardPressure(),
                shedding.getPriorityHeader() == null ? "" : shedding.getPriorityHeader().trim(),
                ProxyAdmissionControl.retryAfterSeconds(shedding.getRetryAfter()));
    }

    private ReverseProxyStatusResponse.HealthCheckStatus healthCheckStatus() {
        ReverseProxyProperties.HealthCheck healthCheck = properties.getHealthCheck();
        return new ReverseProxyStatusResponse.HealthCheckStatus(
                healthCheck.isEnabled(),
                normalizedPath(healthCheck.getPath()),
                healthCheck.getTimeout().toMillis(),
                healthCheck.getInterval().toMillis(),
                healthCheck.getHealthyThreshold(),
                healthCheck.getUnhealthyThreshold());
    }

    private ReverseProxyStatusResponse.RetryStatus retryStatus() {
        ReverseProxyProperties.Retry retry = properties.getRetry();
        ReverseProxyProperties.Backoff backoff = retry.getBackoff();
        return new ReverseProxyStatusResponse.RetryStatus(
                retry.isEnabled(),
                Math.max(1, retry.getMaxAttempts()),
                Math.max(0, Math.min(100, retry.getBudgetPercent())),
                Math.max(0, backoff.getBase().toMillis()),
                Math.max(0, backoff.getMax().toMillis()),
                0,
                0,
                0,
                0,
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
                cooldown.isRecoverOnSuccessfulHealthCheck(),
                Math.max(0, properties.getSlowStart().getDuration().toMillis()));
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
                        safeHostMatch(route),
                        safeHeaderMatchNames(route),
                        safeSplitStatuses(route),
                        ReverseProxyRoutePlanner.safeRouteStrategy(properties, route),
                        safeHashOn(route),
                        route.getAffinity() != null
                                && route.getAffinity().getCookieName() != null
                                && !route.getAffinity().getCookieName().isBlank(),
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
                    null,
                    List.of(),
                    List.of(),
                    properties.getStrategy(),
                    "client-ip",
                    false,
                    properties.getUpstreams().stream()
                            .map(ReverseProxyStatusController::safeUpstreamId)
                            .filter(id -> !id.isEmpty())
                            .toList()));
        }
        return List.of();
    }

    private static String safeHashOn(ReverseProxyProperties.Route route) {
        String hashOn = route.getHashOn();
        return hashOn == null || hashOn.isBlank() ? "client-ip" : hashOn.trim();
    }

    private static String safeHostMatch(ReverseProxyProperties.Route route) {
        ReverseProxyProperties.Match match = route.getMatch();
        if (match == null || match.getHost() == null || match.getHost().isBlank()) {
            return null;
        }
        try {
            return ReverseProxyRoutePlanner.normalizedHost(match.getHost(), "route match host");
        } catch (IllegalStateException exception) {
            return null;
        }
    }

    private static List<String> safeHeaderMatchNames(ReverseProxyProperties.Route route) {
        ReverseProxyProperties.Match match = route.getMatch();
        if (match == null || match.getHeader() == null) {
            return List.of();
        }
        return match.getHeader().keySet().stream()
                .filter(Objects::nonNull)
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .filter(name -> !name.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    private static List<ReverseProxyStatusResponse.SplitStatus> safeSplitStatuses(
            ReverseProxyProperties.Route route) {
        if (route.getSplit() == null) {
            return List.of();
        }
        return route.getSplit().entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    ReverseProxyProperties.SplitGroup split = entry.getValue();
                    return new ReverseProxyStatusResponse.SplitStatus(
                            entry.getKey(),
                            split == null ? 0 : split.getPercentage(),
                            split == null ? List.of() : split.getTargetIds().stream()
                                    .map(ReverseProxyStatusController::safeText)
                                    .filter(value -> !value.isEmpty())
                                    .distinct()
                                    .sorted()
                                    .toList());
                })
                .toList();
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
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

    private ResponseEntity<ApiErrorResponse> rejectAdminAuthentication(HttpServletRequest request) {
        if (oauth2Mode() || validApiKey(request)) {
            return null;
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiErrorResponse.unauthorized(
                request.getRequestURI(), "X-API-Key is required for proxy administration"));
    }

    private static ResponseEntity<ApiErrorResponse> adminUnavailable(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                "proxy_admin_unavailable",
                "Proxy mode must be enabled at startup before runtime administration is available.",
                request.getRequestURI(),
                Instant.now().toString(),
                List.of()));
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

    private static ResponseEntity<ApiErrorResponse> decisionNotFound(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.notFound(
                "Retained proxy decision was not found; it may never have existed or may have been evicted.",
                request.getRequestURI()));
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
