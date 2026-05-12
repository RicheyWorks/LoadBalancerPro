package com.richmond423.loadbalancerpro.api.proxy;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.richmond423.loadbalancerpro.core.NetworkAwarenessSignal;
import com.richmond423.loadbalancerpro.core.RoutingDecision;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import com.richmond423.loadbalancerpro.core.ServerStateVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

@Service
@ConditionalOnProperty(prefix = "loadbalancerpro.proxy", name = "enabled", havingValue = "true")
public class ReverseProxyService {
    private static final Logger logger = LoggerFactory.getLogger(ReverseProxyService.class);
    private static final String PROXY_PREFIX = "/proxy";
    private static final String UPSTREAM_HEADER = "X-LoadBalancerPro-Upstream";
    private static final String STRATEGY_HEADER = "X-LoadBalancerPro-Strategy";
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "content-length",
            "expect",
            "host",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade");

    private final HttpClient httpClient;
    private final ReverseProxyMetrics metrics;
    private final RoutingStrategyRegistry registry;
    private final Clock clock;
    private final AtomicReference<ActiveProxyConfig> activeConfig;
    private final AtomicReference<ReloadState> reloadState;
    private final AtomicLong nextGeneration = new AtomicLong(1);
    private final ConcurrentMap<String, ProbeState> probeStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ResilienceState> resilienceStates = new ConcurrentHashMap<>();

    @Autowired
    public ReverseProxyService(ReverseProxyProperties properties,
                               HttpClient httpClient,
                               ReverseProxyMetrics metrics) {
        this(properties, httpClient, metrics, RoutingStrategyRegistry.defaultRegistry(), Clock.systemUTC());
    }

    ReverseProxyService(ReverseProxyProperties properties,
                        HttpClient httpClient,
                        ReverseProxyMetrics metrics,
                        RoutingStrategyRegistry registry,
                        Clock clock) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics cannot be null");
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        ActiveProxyConfig startupConfig = buildActiveConfig(properties, nextGeneration.getAndIncrement());
        this.activeConfig = new AtomicReference<>(startupConfig);
        this.reloadState = new AtomicReference<>(ReloadState.notAttempted(startupConfig));
        logStartupSummary();
    }

    @SuppressWarnings("java/ssrf")
    ReverseProxyResponse forward(HttpServletRequest request, byte[] requestBody) {
        ActiveProxyConfig config = activeConfig.get();
        ReverseProxyProperties properties = config.properties();
        byte[] body = requestBody == null ? new byte[0] : requestBody.clone();
        if (body.length > properties.getMaxRequestBytes()) {
            logger.warn("proxy.forward.failure reason=payload_too_large requestBytes={} maxRequestBytes={}",
                    body.length, properties.getMaxRequestBytes());
            metrics.recordFailure(null, HttpStatus.PAYLOAD_TOO_LARGE.value());
            return proxyError(HttpStatus.PAYLOAD_TOO_LARGE, "proxy_payload_too_large",
                    "Proxy request body exceeds maximum size of " + properties.getMaxRequestBytes() + " bytes");
        }

        String proxyPathSuffix;
        try {
            proxyPathSuffix = validatedProxyPathSuffix(request);
        } catch (IllegalArgumentException exception) {
            logger.warn("proxy.forward.failure reason=invalid_path message={}", exception.getMessage());
            metrics.recordFailure(null, HttpStatus.BAD_REQUEST.value());
            return proxyError(HttpStatus.BAD_REQUEST, "proxy_path_invalid", exception.getMessage());
        }
        if (!properties.isEnabled()) {
            logger.warn("proxy.forward.failure reason=proxy_disabled");
            metrics.recordFailure(null, HttpStatus.SERVICE_UNAVAILABLE.value());
            return proxyError(HttpStatus.SERVICE_UNAVAILABLE, "proxy_disabled",
                    "Proxy mode is disabled in the active configuration.");
        }
        Optional<ReverseProxyRoutePlanner.ConfiguredRoute> selectedRoute = routeFor(config.routes(), proxyPathSuffix);
        if (selectedRoute.isEmpty()) {
            logger.warn("proxy.forward.failure reason=route_not_found pathSuffix={}", proxyPathSuffix);
            metrics.recordFailure(null, HttpStatus.NOT_FOUND.value());
            return proxyError(HttpStatus.NOT_FOUND, "proxy_route_not_found",
                    "No configured proxy route matches path " + proxyPathSuffix);
        }
        ReverseProxyRoutePlanner.ConfiguredRoute route = selectedRoute.get();
        int maxAttempts = maxAttemptsFor(request.getMethod(), properties);
        Set<String> attemptedUpstreamIds = new LinkedHashSet<>();
        ReverseProxyResponse lastResponse = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            List<UpstreamCandidate> upstreams = configuredUpstreams(properties, route, attemptedUpstreamIds);
            if (upstreams.isEmpty()) {
                if (lastResponse != null) {
                    return lastResponse;
                }
                logger.warn("proxy.forward.failure reason=no_configured_upstreams route={} attempt={}",
                        route.name(), attempt);
                metrics.recordFailure(null, HttpStatus.SERVICE_UNAVAILABLE.value());
                return proxyError(HttpStatus.SERVICE_UNAVAILABLE, "proxy_unavailable",
                        "No proxy upstreams are configured for route " + route.name() + ".");
            }

            RoutingDecision decision = route.strategy().choose(upstreams.stream()
                    .map(UpstreamCandidate::state)
                    .toList());
            Optional<String> selectedServerId = decision.explanation().chosenServerId();
            if (selectedServerId.isEmpty()) {
                if (lastResponse != null) {
                    return lastResponse;
                }
                logger.warn("proxy.forward.failure reason=no_healthy_upstreams route={} attempt={}",
                        route.name(), attempt);
                metrics.recordFailure(null, HttpStatus.SERVICE_UNAVAILABLE.value());
                return proxyError(HttpStatus.SERVICE_UNAVAILABLE, "proxy_unavailable",
                        "No healthy proxy upstreams are available.");
            }

            Map<String, UpstreamCandidate> upstreamById = upstreams.stream()
                    .collect(Collectors.toMap(candidate -> candidate.state().serverId(), Function.identity()));
            UpstreamCandidate upstream = upstreamById.get(selectedServerId.get());
            if (upstream == null) {
                if (lastResponse != null) {
                    return lastResponse;
                }
                logger.warn("proxy.forward.failure reason=selected_upstream_not_configured route={} upstreamId={}",
                        route.name(), selectedServerId.get());
                metrics.recordFailure(selectedServerId.get(), HttpStatus.SERVICE_UNAVAILABLE.value());
                return proxyError(HttpStatus.SERVICE_UNAVAILABLE, "proxy_unavailable",
                        "Selected proxy upstream is not configured: " + selectedServerId.get());
            }

            String upstreamId = upstream.state().serverId();
            attemptedUpstreamIds.add(upstreamId);
            if (attempt > 1) {
                logger.info("proxy.forward.retry route={} attempt={} upstreamId={}",
                        route.name(), attempt, upstreamId);
                metrics.recordRetryAttempt(upstreamId);
            }
            ForwardAttemptResult attemptResult =
                    forwardOnce(properties, request, body, upstream, route.strategyId().externalName(),
                            proxyPathSuffix);
            lastResponse = attemptResult.response();
            if (!attemptResult.retriable() || attempt == maxAttempts) {
                return attemptResult.response();
            }
        }
        return lastResponse == null
                ? proxyError(HttpStatus.SERVICE_UNAVAILABLE, "proxy_unavailable",
                        "No healthy proxy upstreams are available.")
                : lastResponse;
    }

    ReverseProxyStatusResponse statusSnapshot() {
        ActiveProxyConfig config = activeConfig.get();
        ReverseProxyProperties properties = config.properties();
        List<ReverseProxyRoutePlanner.ConfiguredRoute> routes = config.routes();
        List<ReverseProxyStatusResponse.UpstreamStatus> upstreamStatuses =
                configuredUpstreamStatuses(properties, routes, Instant.now(clock));
        List<String> upstreamIds = upstreamStatuses.stream()
                .map(ReverseProxyStatusResponse.UpstreamStatus::id)
                .toList();
        ReverseProxyProperties.HealthCheck healthCheck = properties.getHealthCheck();
        ReverseProxyProperties.Retry retry = properties.getRetry();
        ReverseProxyProperties.Cooldown cooldown = properties.getCooldown();
        List<ReverseProxyStatusResponse.RouteStatus> routeStatuses = routes.stream()
                .map(route -> new ReverseProxyStatusResponse.RouteStatus(
                        route.name(),
                        route.pathPrefix(),
                        route.strategyId().externalName(),
                        route.targets().stream()
                                .map(ReverseProxyProperties.Upstream::getId)
                                .toList()))
                .toList();
        ReverseProxyMetricsSnapshot metricsSnapshot = metrics.snapshot(upstreamIds);
        return new ReverseProxyStatusResponse(
                properties.isEnabled(),
                routes.size() == 1 ? routes.get(0).strategyId().externalName() : properties.getStrategy(),
                new ReverseProxyStatusResponse.HealthCheckStatus(
                        healthCheck.isEnabled(),
                        normalizedHealthCheckPath(healthCheck.getPath()),
                        healthCheck.getTimeout().toMillis(),
                        healthCheck.getInterval().toMillis()),
                new ReverseProxyStatusResponse.RetryStatus(
                        retry.isEnabled(),
                        Math.max(1, retry.getMaxAttempts()),
                        retry.isRetryNonIdempotent(),
                        normalizedRetryMethods(properties).stream().sorted().toList(),
                        normalizedRetryStatuses(properties).stream().sorted().toList()),
                new ReverseProxyStatusResponse.CooldownStatus(
                        cooldown.isEnabled(),
                        Math.max(1, cooldown.getConsecutiveFailureThreshold()),
                        Math.max(0, cooldown.getDuration().toMillis()),
                        cooldown.isRecoverOnSuccessfulHealthCheck()),
                routeStatuses,
                upstreamStatuses,
                metricsSnapshot,
                ReverseProxyStatusSummaries.observability(properties.isEnabled(), routeStatuses, upstreamStatuses,
                        metricsSnapshot),
                ReverseProxyStatusSummaries.controllerNotAvailableSecurityBoundary(),
                reloadStatusSnapshot(config));
    }

    synchronized ReverseProxyReloadResponse reload(ReverseProxyProperties candidateProperties) {
        Instant attemptedAt = Instant.now(clock);
        ActiveProxyConfig previousConfig = activeConfig.get();
        try {
            ActiveProxyConfig candidateConfig = buildActiveConfigForReload(candidateProperties);
            activeConfig.set(candidateConfig);
            nextGeneration.updateAndGet(current -> Math.max(current, candidateConfig.generation() + 1));
            probeStates.clear();
            resilienceStates.clear();
            ReloadState successState = ReloadState.success(attemptedAt, candidateConfig);
            reloadState.set(successState);
            logger.info("proxy.config.reload status=success generation={} routeCount={} backendTargetCount={}",
                    candidateConfig.generation(), candidateConfig.routeCount(), candidateConfig.backendTargetCount());
            return reloadResponse(true, successState, candidateConfig);
        } catch (RuntimeException exception) {
            List<String> errors = List.of(safeValidationError(exception));
            ReloadState failureState = ReloadState.failure(attemptedAt, previousConfig, errors);
            reloadState.set(failureState);
            logger.warn("proxy.config.reload status=failure generation={} validationErrors={}",
                    previousConfig.generation(), errors);
            return reloadResponse(false, failureState, previousConfig);
        }
    }

    private ActiveProxyConfig buildActiveConfigForReload(ReverseProxyProperties candidateProperties) {
        if (candidateProperties == null) {
            throw new IllegalStateException("reload payload must include proxy configuration");
        }
        if (!candidateProperties.isEnabled()) {
            throw new IllegalStateException("loadbalancerpro.proxy.enabled must be true for runtime reload");
        }
        return buildActiveConfig(candidateProperties, nextGeneration.get());
    }

    private ActiveProxyConfig buildActiveConfig(ReverseProxyProperties candidateProperties, long generation) {
        ReverseProxyProperties safeProperties = copyProperties(
                Objects.requireNonNull(candidateProperties, "properties cannot be null"));
        List<ReverseProxyRoutePlanner.ConfiguredRoute> configuredRoutes =
                ReverseProxyRoutePlanner.buildEnabledRoutes(safeProperties, registry);
        validateRuntimeFields(safeProperties, configuredRoutes);
        return new ActiveProxyConfig(safeProperties, configuredRoutes, generation);
    }

    private void validateRuntimeFields(ReverseProxyProperties properties,
                                       List<ReverseProxyRoutePlanner.ConfiguredRoute> configuredRoutes) {
        if (properties.getMaxRequestBytes() <= 0) {
            throw new IllegalStateException("loadbalancerpro.proxy.max-request-bytes must be greater than 0");
        }
        normalizedHealthCheckPath(properties.getHealthCheck().getPath());
        normalizedRetryMethods(properties);
        normalizedRetryStatuses(properties);
        for (ReverseProxyRoutePlanner.ConfiguredRoute route : configuredRoutes) {
            for (ReverseProxyProperties.Upstream upstream : route.targets()) {
                validateUpstreamRuntimeFields(upstream);
            }
        }
    }

    private static void validateUpstreamRuntimeFields(ReverseProxyProperties.Upstream upstream) {
        nonNegative(upstream.getInFlightRequestCount(), "inFlightRequestCount");
        optionalNonNegative(upstream.getConfiguredCapacity(), "configuredCapacity");
        optionalPositive(upstream.getEstimatedConcurrencyLimit(), "estimatedConcurrencyLimit");
        nonNegative(upstream.getAverageLatencyMillis(), "averageLatencyMillis");
        nonNegative(upstream.getP95LatencyMillis(), "p95LatencyMillis");
        nonNegative(upstream.getP99LatencyMillis(), "p99LatencyMillis");
        rate(upstream.getRecentErrorRate(), "recentErrorRate");
        optionalNonNegativeInt(upstream.getQueueDepth(), "queueDepth");
    }

    private ReverseProxyStatusResponse.ReloadStatus reloadStatusSnapshot(ActiveProxyConfig config) {
        ReloadState state = reloadState.get();
        return new ReverseProxyStatusResponse.ReloadStatus(
                true,
                config.generation(),
                isoInstant(state.lastReloadAttemptedAt()),
                isoInstant(state.lastReloadSucceededAt()),
                isoInstant(state.lastReloadFailedAt()),
                state.lastReloadStatus(),
                state.lastReloadValidationErrors(),
                config.routeCount(),
                config.backendTargetCount());
    }

    private ReverseProxyReloadResponse reloadResponse(boolean success,
                                                      ReloadState state,
                                                      ActiveProxyConfig config) {
        ReverseProxyStatusResponse.ReloadStatus status = new ReverseProxyStatusResponse.ReloadStatus(
                true,
                config.generation(),
                isoInstant(state.lastReloadAttemptedAt()),
                isoInstant(state.lastReloadSucceededAt()),
                isoInstant(state.lastReloadFailedAt()),
                state.lastReloadStatus(),
                state.lastReloadValidationErrors(),
                config.routeCount(),
                config.backendTargetCount());
        return new ReverseProxyReloadResponse(
                success,
                state.lastReloadStatus(),
                config.generation(),
                config.routeCount(),
                config.backendTargetCount(),
                state.lastReloadValidationErrors(),
                status);
    }

    @SuppressWarnings("java/ssrf")
    private HttpRequest buildOutboundRequest(ReverseProxyProperties properties,
                                             HttpServletRequest request,
                                             byte[] body,
                                             UpstreamCandidate upstream,
                                             String proxyPathSuffix) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(targetUri(request, upstream, proxyPathSuffix))
                .timeout(properties.getRequestTimeout());
        Collections.list(request.getHeaderNames()).forEach(headerName -> {
            if (isForwardableHeader(headerName)) {
                Collections.list(request.getHeaders(headerName))
                        .forEach(headerValue -> builder.header(headerName, headerValue));
            }
        });
        HttpRequest.BodyPublisher publisher = body.length == 0
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(body);
        return builder.method(request.getMethod().toUpperCase(Locale.ROOT), publisher).build();
    }

    private ForwardAttemptResult forwardOnce(ReverseProxyProperties properties,
                                             HttpServletRequest request,
                                             byte[] body,
                                             UpstreamCandidate upstream,
                                             String strategyName,
                                             String proxyPathSuffix) {
        String upstreamId = upstream.state().serverId();
        try {
            HttpRequest outbound = buildOutboundRequest(properties, request, body, upstream, proxyPathSuffix);
            HttpResponse<byte[]> response = httpClient.send(outbound, HttpResponse.BodyHandlers.ofByteArray());
            metrics.recordForwarded(upstreamId, response.statusCode());
            HttpHeaders responseHeaders = forwardedResponseHeaders(response.headers().map());
            responseHeaders.set(UPSTREAM_HEADER, upstreamId);
            responseHeaders.set(STRATEGY_HEADER, strategyName);
            ReverseProxyResponse proxyResponse =
                    new ReverseProxyResponse(response.statusCode(), responseHeaders, response.body());
            if (isRetryStatus(properties, response.statusCode())) {
                logger.warn("proxy.forward.retryable_status upstreamId={} status={} reason=retry_status",
                        upstreamId, response.statusCode());
                recordResilienceFailure(properties, upstreamId);
                return new ForwardAttemptResult(proxyResponse, true);
            }
            recordResilienceSuccess(upstreamId);
            return new ForwardAttemptResult(proxyResponse, false);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.warn("proxy.forward.failure upstreamId={} reason=interrupted", upstreamId);
            metrics.recordFailure(upstreamId, HttpStatus.BAD_GATEWAY.value());
            recordResilienceFailure(properties, upstreamId);
            return new ForwardAttemptResult(
                    proxyError(HttpStatus.BAD_GATEWAY, "proxy_upstream_failure",
                            "Proxy forwarding was interrupted while calling upstream " + upstreamId),
                    true);
        } catch (IOException | IllegalArgumentException exception) {
            logger.warn("proxy.forward.failure upstreamId={} reason=upstream_unreachable exceptionType={}",
                    upstreamId, exception.getClass().getSimpleName());
            metrics.recordFailure(upstreamId, HttpStatus.BAD_GATEWAY.value());
            recordResilienceFailure(properties, upstreamId);
            return new ForwardAttemptResult(
                    proxyError(HttpStatus.BAD_GATEWAY, "proxy_upstream_failure",
                            "Proxy could not reach upstream " + upstreamId),
                    true);
        }
    }

    private URI targetUri(HttpServletRequest request, UpstreamCandidate upstream, String suffix) {
        String query = request.getQueryString();
        if (query != null && containsControlCharacter(query)) {
            throw new IllegalArgumentException("Proxy query string must not contain control characters.");
        }
        URI baseUri = upstream.baseUri();
        String targetPath = joinPath(baseUri.getPath(), suffix);
        try {
            URI target = new URI(baseUri.getScheme(), null, baseUri.getHost(), baseUri.getPort(),
                    targetPath, query, null);
            validateConfiguredAuthority(baseUri, target);
            return target;
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Proxy target URI could not be constructed for configured upstream.",
                    exception);
        }
    }

    private static String validatedProxyPathSuffix(HttpServletRequest request) {
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String requestUri = request.getRequestURI();
        String path = requestUri.startsWith(contextPath) ? requestUri.substring(contextPath.length()) : requestUri;
        if (path.length() <= PROXY_PREFIX.length()) {
            return "/";
        }
        String suffix = path.substring(PROXY_PREFIX.length());
        String normalizedSuffix = suffix.startsWith("/") ? suffix : "/" + suffix;
        if (normalizedSuffix.startsWith("//") || normalizedSuffix.contains("\\")
                || containsControlCharacter(normalizedSuffix)) {
            throw new IllegalArgumentException("Proxy path suffix must remain within the configured upstream path.");
        }
        return normalizedSuffix;
    }

    private static String joinPath(String basePath, String suffix) {
        String normalizedBase = basePath == null || basePath.isBlank()
                ? ""
                : (basePath.startsWith("/") ? basePath : "/" + basePath);
        if (normalizedBase.isEmpty()) {
            return suffix;
        }
        return normalizedBase.endsWith("/") && suffix.startsWith("/")
                ? normalizedBase.substring(0, normalizedBase.length() - 1) + suffix
                : normalizedBase + suffix;
    }

    private static void validateConfiguredAuthority(URI configuredBaseUri, URI targetUri) {
        if (!Objects.equals(configuredBaseUri.getScheme(), targetUri.getScheme())
                || !Objects.equals(configuredBaseUri.getHost(), targetUri.getHost())
                || configuredBaseUri.getPort() != targetUri.getPort()) {
            throw new IllegalArgumentException("Proxy target escaped configured upstream authority.");
        }
    }

    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(character -> character < 0x20 || character == 0x7f);
    }

    private Optional<ReverseProxyRoutePlanner.ConfiguredRoute> routeFor(
            List<ReverseProxyRoutePlanner.ConfiguredRoute> routes,
            String proxyPathSuffix) {
        return routes.stream()
                .filter(route -> ReverseProxyRoutePlanner.pathMatches(route.pathPrefix(), proxyPathSuffix))
                .max(Comparator.comparingInt(route -> route.pathPrefix().length()));
    }

    private List<UpstreamCandidate> configuredUpstreams(ReverseProxyProperties properties,
                                                        ReverseProxyRoutePlanner.ConfiguredRoute route,
                                                        Set<String> excludedUpstreamIds) {
        Instant now = Instant.now(clock);
        return route.targets().stream()
                .filter(upstream -> excludedUpstreamIds == null
                        || !excludedUpstreamIds.contains(requireNonBlank(
                                upstream.getId(), "loadbalancerpro.proxy.upstreams[].id")))
                .map(upstream -> toCandidate(properties, upstream, now))
                .toList();
    }

    private UpstreamCandidate toCandidate(ReverseProxyProperties properties,
                                          ReverseProxyProperties.Upstream upstream,
                                          Instant timestamp) {
        String id = requireNonBlank(upstream.getId(), "loadbalancerpro.proxy.upstreams[].id");
        URI baseUri = configuredBaseUri(upstream, id);
        EffectiveHealth effectiveHealth = effectiveHealth(properties, upstream, id, baseUri, timestamp);
        ServerStateVector state = new ServerStateVector(
                id,
                effectiveHealth.healthy(),
                nonNegative(upstream.getInFlightRequestCount(), "inFlightRequestCount"),
                optionalNonNegative(upstream.getConfiguredCapacity(), "configuredCapacity"),
                optionalPositive(upstream.getEstimatedConcurrencyLimit(), "estimatedConcurrencyLimit"),
                nonNegative(upstream.getWeight(), "weight"),
                nonNegative(upstream.getAverageLatencyMillis(), "averageLatencyMillis"),
                nonNegative(upstream.getP95LatencyMillis(), "p95LatencyMillis"),
                nonNegative(upstream.getP99LatencyMillis(), "p99LatencyMillis"),
                rate(upstream.getRecentErrorRate(), "recentErrorRate"),
                optionalNonNegativeInt(upstream.getQueueDepth(), "queueDepth"),
                NetworkAwarenessSignal.neutral(id, timestamp),
                timestamp);
        return new UpstreamCandidate(baseUri, state);
    }

    private List<ReverseProxyStatusResponse.UpstreamStatus> configuredUpstreamStatuses(
            ReverseProxyProperties properties,
            List<ReverseProxyRoutePlanner.ConfiguredRoute> routes,
            Instant now) {
        return routes.stream()
                .flatMap(route -> route.targets().stream()
                        .map(upstream -> {
                            String id = requireNonBlank(upstream.getId(), "loadbalancerpro.proxy.upstreams[].id");
                            URI baseUri = configuredBaseUri(upstream, id);
                            EffectiveHealth health = effectiveHealth(properties, upstream, id, baseUri, now);
                            ResilienceState resilienceState = resilienceState(id);
                            return new ReverseProxyStatusResponse.UpstreamStatus(
                                    id,
                                    safeConfiguredUrl(baseUri),
                                    upstream.isHealthy(),
                                    health.healthy(),
                                    health.source(),
                                    health.lastProbeStatusCode(),
                                    health.lastProbeOutcome(),
                                    resilienceState.consecutiveFailures(),
                                    resilienceState.cooldownActive(now),
                                    resilienceState.cooldownRemainingMillis(now));
                        }))
                .toList();
    }

    private URI configuredBaseUri(ReverseProxyProperties.Upstream upstream, String id) {
        URI baseUri = URI.create(requireNonBlank(upstream.getUrl(), "loadbalancerpro.proxy.upstreams[].url"));
        if (!"http".equalsIgnoreCase(baseUri.getScheme()) && !"https".equalsIgnoreCase(baseUri.getScheme())) {
            throw new IllegalArgumentException("Proxy upstream URL must use http or https: " + id);
        }
        if (baseUri.getHost() == null || baseUri.getUserInfo() != null) {
            throw new IllegalArgumentException("Proxy upstream URL must provide a host and must not include user info: "
                    + id);
        }
        return baseUri;
    }

    private EffectiveHealth effectiveHealth(ReverseProxyProperties properties,
                                            ReverseProxyProperties.Upstream upstream,
                                            String id,
                                            URI baseUri,
                                            Instant now) {
        if (!upstream.isHealthy()) {
            return new EffectiveHealth(false, "CONFIGURED_DISABLED", null, "configured healthy=false");
        }
        ReverseProxyProperties.HealthCheck healthCheck = properties.getHealthCheck();
        ResilienceState resilienceState = resilienceState(id);
        if (properties.getCooldown().isEnabled() && resilienceState.cooldownActive(now)) {
            if (!healthCheck.isEnabled() || !properties.getCooldown().isRecoverOnSuccessfulHealthCheck()
                    || !probeDue(properties, probeStates.get(id), now)) {
                return new EffectiveHealth(false, "COOLDOWN", null, "temporary cooldown active");
            }
        }
        if (!healthCheck.isEnabled()) {
            if (properties.getCooldown().isEnabled()) {
                resilienceState.clearExpiredCooldown(now);
                if (resilienceState.cooldownActive(now)) {
                    return new EffectiveHealth(false, "COOLDOWN", null, "temporary cooldown active");
                }
            }
            return new EffectiveHealth(true, "CONFIGURED", null, "active health checks disabled");
        }
        ProbeState existing = probeStates.get(id);
        ProbeState current = probeDue(properties, existing, now) ? probeUpstream(properties, id, baseUri, now) : existing;
        if (current != existing) {
            probeStates.put(id, current);
        }
        if (current.healthy()) {
            recordResilienceSuccess(id);
        } else {
            recordResilienceFailure(properties, id);
        }
        if (properties.getCooldown().isEnabled() && resilienceState.cooldownActive(now)) {
            return new EffectiveHealth(false, "COOLDOWN", current.statusCode(), "temporary cooldown active");
        }
        return new EffectiveHealth(
                current.healthy(),
                "ACTIVE_PROBE",
                current.statusCode(),
                current.outcome());
    }

    private boolean probeDue(ReverseProxyProperties properties, ProbeState existing, Instant now) {
        if (existing == null) {
            return true;
        }
        Duration interval = properties.getHealthCheck().getInterval();
        if (interval.isZero() || interval.isNegative()) {
            return true;
        }
        return !existing.checkedAt().plus(interval).isAfter(now);
    }

    @SuppressWarnings("java/ssrf")
    private ProbeState probeUpstream(ReverseProxyProperties properties, String id, URI baseUri, Instant now) {
        try {
            HttpRequest request = HttpRequest.newBuilder(healthCheckUri(properties, baseUri))
                    .timeout(properties.getHealthCheck().getTimeout())
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int statusCode = response.statusCode();
            boolean healthy = statusCode >= 200 && statusCode <= 399;
            return new ProbeState(
                    healthy,
                    statusCode,
                    healthy ? "2xx/3xx probe response" : "non-2xx/3xx probe response",
                    now);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new ProbeState(false, null, "probe interrupted", now);
        } catch (IOException | IllegalArgumentException exception) {
            return new ProbeState(false, null, "probe failed", now);
        }
    }

    private URI healthCheckUri(ReverseProxyProperties properties, URI baseUri) {
        String healthPath = normalizedHealthCheckPath(properties.getHealthCheck().getPath());
        String targetPath = joinPath(baseUri.getPath(), healthPath);
        try {
            URI target = new URI(baseUri.getScheme(), null, baseUri.getHost(), baseUri.getPort(),
                    targetPath, null, null);
            validateConfiguredAuthority(baseUri, target);
            return target;
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Proxy health-check URI could not be constructed for upstream.",
                    exception);
        }
    }

    private static String normalizedHealthCheckPath(String configuredPath) {
        String path = configuredPath == null || configuredPath.isBlank() ? "/health" : configuredPath.trim();
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        if (normalizedPath.startsWith("//") || normalizedPath.contains("\\") || normalizedPath.contains("?")
                || containsControlCharacter(normalizedPath)) {
            throw new IllegalArgumentException("Proxy health-check path must be a relative absolute path.");
        }
        return normalizedPath;
    }

    private int maxAttemptsFor(String method, ReverseProxyProperties properties) {
        ReverseProxyProperties.Retry retry = properties.getRetry();
        if (!retry.isEnabled() || !retryAllowedFor(method, properties)) {
            return 1;
        }
        return Math.max(1, retry.getMaxAttempts());
    }

    private boolean retryAllowedFor(String method, ReverseProxyProperties properties) {
        ReverseProxyProperties.Retry retry = properties.getRetry();
        if (retry.isRetryNonIdempotent()) {
            return true;
        }
        String normalizedMethod = method == null ? "" : method.trim().toUpperCase(Locale.ROOT);
        return normalizedRetryMethods(properties).contains(normalizedMethod);
    }

    private Set<String> normalizedRetryMethods(ReverseProxyProperties properties) {
        return properties.getRetry().getMethods().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Integer> normalizedRetryStatuses(ReverseProxyProperties properties) {
        return properties.getRetry().getRetryStatuses().stream()
                .filter(Objects::nonNull)
                .filter(statusCode -> statusCode >= 100 && statusCode <= 599)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isRetryStatus(ReverseProxyProperties properties, int statusCode) {
        return properties.getRetry().isEnabled() && normalizedRetryStatuses(properties).contains(statusCode);
    }

    private void recordResilienceFailure(ReverseProxyProperties properties, String upstreamId) {
        if (upstreamId == null || upstreamId.isBlank()) {
            return;
        }
        boolean activated = resilienceState(upstreamId).recordFailure(Instant.now(clock), properties.getCooldown());
        if (activated) {
            metrics.recordCooldownActivation(upstreamId);
            logger.warn("proxy.cooldown.activated upstreamId={} threshold={} durationMillis={}",
                    upstreamId,
                    Math.max(1, properties.getCooldown().getConsecutiveFailureThreshold()),
                    Math.max(0, properties.getCooldown().getDuration().toMillis()));
        }
    }

    private void recordResilienceSuccess(String upstreamId) {
        if (upstreamId == null || upstreamId.isBlank()) {
            return;
        }
        resilienceState(upstreamId).recordSuccess();
    }

    private ResilienceState resilienceState(String upstreamId) {
        return resilienceStates.computeIfAbsent(upstreamId, ignored -> new ResilienceState());
    }

    private static String safeConfiguredUrl(URI baseUri) {
        try {
            return new URI(baseUri.getScheme(), null, baseUri.getHost(), baseUri.getPort(),
                    baseUri.getPath(), null, null).toString();
        } catch (URISyntaxException exception) {
            return "invalid";
        }
    }

    private static HttpHeaders forwardedResponseHeaders(Map<String, List<String>> upstreamHeaders) {
        HttpHeaders headers = new HttpHeaders();
        upstreamHeaders.forEach((name, values) -> {
            if (isForwardableHeader(name)) {
                values.forEach(value -> headers.add(name, value));
            }
        });
        return headers;
    }

    private static boolean isForwardableHeader(String headerName) {
        return headerName != null && !HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
    }

    private static ReverseProxyResponse proxyError(HttpStatus status, String error, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\":\"" + jsonEscape(error) + "\",\"message\":\"" + jsonEscape(message) + "\"}";
        return new ReverseProxyResponse(status.value(), headers, body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private void logStartupSummary() {
        ActiveProxyConfig config = activeConfig.get();
        ReverseProxyProperties properties = config.properties();
        logger.info(
                "proxy.observability.startup proxyEnabled={} routeCount={} backendTargetCount={} "
                        + "healthCheckEnabled={} retryEnabled={} cooldownEnabled={}",
                properties.isEnabled(),
                config.routeCount(),
                config.backendTargetCount(),
                properties.getHealthCheck().isEnabled(),
                properties.getRetry().isEnabled(),
                properties.getCooldown().isEnabled());
        config.routes().forEach(route -> logger.info(
                "proxy.observability.route route={} pathPrefix={} strategy={} targetCount={} targetIds={}",
                route.name(),
                route.pathPrefix(),
                route.strategyId().externalName(),
                route.targets().size(),
                route.targets().stream()
                        .map(ReverseProxyProperties.Upstream::getId)
                        .toList()));
    }

    private static OptionalDouble optionalNonNegative(Double value, String fieldName) {
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(nonNegative(value, fieldName));
    }

    private static OptionalDouble optionalPositive(Double value, String fieldName) {
        if (value == null) {
            return OptionalDouble.empty();
        }
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and positive");
        }
        return OptionalDouble.of(value);
    }

    private static OptionalInt optionalNonNegativeInt(Integer value, String fieldName) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(nonNegative(value, fieldName));
    }

    private static int nonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return value;
    }

    private static double nonNegative(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative");
        }
        return value;
    }

    private static double rate(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
        }
        return value;
    }

    private static String safeValidationError(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.replaceAll("(?i)(X-API-Key|api[-_ ]?key)\\s*[:=]\\s*[^\\s,;]+", "$1=<redacted>");
    }

    private static String isoInstant(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static ReverseProxyProperties copyProperties(ReverseProxyProperties source) {
        ReverseProxyProperties copy = new ReverseProxyProperties();
        copy.setEnabled(source.isEnabled());
        copy.setStrategy(source.getStrategy());
        copy.setRequestTimeout(source.getRequestTimeout());
        copy.setMaxRequestBytes(source.getMaxRequestBytes());
        copy.setHealthCheck(copyHealthCheck(source.getHealthCheck()));
        copy.setRetry(copyRetry(source.getRetry()));
        copy.setCooldown(copyCooldown(source.getCooldown()));
        copy.setUpstreams(source.getUpstreams().stream()
                .map(ReverseProxyService::copyUpstream)
                .toList());
        Map<String, ReverseProxyProperties.Route> routes = new LinkedHashMap<>();
        source.getRoutes().forEach((name, route) -> routes.put(name, copyRoute(route)));
        copy.setRoutes(routes);
        return copy;
    }

    private static ReverseProxyProperties.Route copyRoute(ReverseProxyProperties.Route source) {
        ReverseProxyProperties.Route copy = new ReverseProxyProperties.Route();
        if (source == null) {
            return copy;
        }
        copy.setPathPrefix(source.getPathPrefix());
        copy.setStrategy(source.getStrategy());
        copy.setTargets(source.getTargets().stream()
                .map(ReverseProxyService::copyUpstream)
                .toList());
        return copy;
    }

    private static ReverseProxyProperties.Upstream copyUpstream(ReverseProxyProperties.Upstream source) {
        ReverseProxyProperties.Upstream copy = new ReverseProxyProperties.Upstream();
        if (source == null) {
            return copy;
        }
        copy.setId(source.getId());
        copy.setUrl(source.getUrl());
        copy.setHealthy(source.isHealthy());
        copy.setInFlightRequestCount(source.getInFlightRequestCount());
        copy.setConfiguredCapacity(source.getConfiguredCapacity());
        copy.setEstimatedConcurrencyLimit(source.getEstimatedConcurrencyLimit());
        copy.setWeight(source.getWeight());
        copy.setAverageLatencyMillis(source.getAverageLatencyMillis());
        copy.setP95LatencyMillis(source.getP95LatencyMillis());
        copy.setP99LatencyMillis(source.getP99LatencyMillis());
        copy.setRecentErrorRate(source.getRecentErrorRate());
        copy.setQueueDepth(source.getQueueDepth());
        return copy;
    }

    private static ReverseProxyProperties.HealthCheck copyHealthCheck(ReverseProxyProperties.HealthCheck source) {
        ReverseProxyProperties.HealthCheck copy = new ReverseProxyProperties.HealthCheck();
        if (source == null) {
            return copy;
        }
        copy.setEnabled(source.isEnabled());
        copy.setPath(source.getPath());
        copy.setTimeout(source.getTimeout());
        copy.setInterval(source.getInterval());
        return copy;
    }

    private static ReverseProxyProperties.Retry copyRetry(ReverseProxyProperties.Retry source) {
        ReverseProxyProperties.Retry copy = new ReverseProxyProperties.Retry();
        if (source == null) {
            return copy;
        }
        copy.setEnabled(source.isEnabled());
        copy.setMaxAttempts(source.getMaxAttempts());
        copy.setRetryNonIdempotent(source.isRetryNonIdempotent());
        copy.setMethods(source.getMethods());
        copy.setRetryStatuses(source.getRetryStatuses());
        return copy;
    }

    private static ReverseProxyProperties.Cooldown copyCooldown(ReverseProxyProperties.Cooldown source) {
        ReverseProxyProperties.Cooldown copy = new ReverseProxyProperties.Cooldown();
        if (source == null) {
            return copy;
        }
        copy.setEnabled(source.isEnabled());
        copy.setConsecutiveFailureThreshold(source.getConsecutiveFailureThreshold());
        copy.setDuration(source.getDuration());
        copy.setRecoverOnSuccessfulHealthCheck(source.isRecoverOnSuccessfulHealthCheck());
        return copy;
    }

    private record ActiveProxyConfig(
            ReverseProxyProperties properties,
            List<ReverseProxyRoutePlanner.ConfiguredRoute> routes,
            long generation) {
        int routeCount() {
            return routes.size();
        }

        int backendTargetCount() {
            return routes.stream()
                    .mapToInt(route -> route.targets().size())
                    .sum();
        }
    }

    private record ReloadState(
            Instant lastReloadAttemptedAt,
            Instant lastReloadSucceededAt,
            Instant lastReloadFailedAt,
            String lastReloadStatus,
            List<String> lastReloadValidationErrors) {
        static ReloadState notAttempted(ActiveProxyConfig config) {
            return new ReloadState(null, null, null, "not_attempted", List.of());
        }

        static ReloadState success(Instant attemptedAt, ActiveProxyConfig config) {
            return new ReloadState(attemptedAt, attemptedAt, null, "success", List.of());
        }

        static ReloadState failure(Instant attemptedAt, ActiveProxyConfig config, List<String> errors) {
            return new ReloadState(attemptedAt, null, attemptedAt, "failure",
                    errors == null ? List.of() : List.copyOf(errors));
        }
    }

    private record UpstreamCandidate(URI baseUri, ServerStateVector state) {
    }

    private record EffectiveHealth(
            boolean healthy,
            String source,
            Integer lastProbeStatusCode,
            String lastProbeOutcome) {
    }

    private record ProbeState(
            boolean healthy,
            Integer statusCode,
            String outcome,
            Instant checkedAt) {
    }

    private record ForwardAttemptResult(ReverseProxyResponse response, boolean retriable) {
    }

    private static final class ResilienceState {
        private int consecutiveFailures;
        private Instant cooldownUntil;

        synchronized boolean recordFailure(Instant now, ReverseProxyProperties.Cooldown cooldown) {
            if (cooldown.isEnabled()) {
                clearExpiredCooldown(now);
            }
            consecutiveFailures++;
            if (!cooldown.isEnabled()) {
                return false;
            }
            if (cooldownActive(now)) {
                return false;
            }
            int threshold = Math.max(1, cooldown.getConsecutiveFailureThreshold());
            if (consecutiveFailures < threshold) {
                return false;
            }
            Duration duration = cooldown.getDuration();
            Duration safeDuration = duration == null || duration.isNegative() ? Duration.ZERO : duration;
            cooldownUntil = now.plus(safeDuration);
            return safeDuration.toMillis() > 0;
        }

        synchronized void recordSuccess() {
            consecutiveFailures = 0;
            cooldownUntil = null;
        }

        synchronized void clearExpiredCooldown(Instant now) {
            if (cooldownUntil != null && !cooldownUntil.isAfter(now)) {
                cooldownUntil = null;
                consecutiveFailures = 0;
            }
        }

        synchronized boolean cooldownActive(Instant now) {
            clearExpiredCooldown(now);
            return cooldownUntil != null && cooldownUntil.isAfter(now);
        }

        synchronized long cooldownRemainingMillis(Instant now) {
            if (!cooldownActive(now)) {
                return 0;
            }
            return Math.max(0, Duration.between(now, cooldownUntil).toMillis());
        }

        synchronized int consecutiveFailures() {
            return consecutiveFailures;
        }
    }
}
