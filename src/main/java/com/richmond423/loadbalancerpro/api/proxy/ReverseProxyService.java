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

import com.richmond423.loadbalancerpro.api.LaseShadowRuntime;
import com.richmond423.loadbalancerpro.core.LiveRoutingShadowObservation;
import com.richmond423.loadbalancerpro.core.NetworkAwarenessSignal;
import com.richmond423.loadbalancerpro.core.RoutingDecision;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import com.richmond423.loadbalancerpro.core.ServerStateVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;

@Service
@ConditionalOnProperty(prefix = "loadbalancerpro.proxy", name = "enabled", havingValue = "true")
public class ReverseProxyService {
    private static final Logger logger = LoggerFactory.getLogger(ReverseProxyService.class);
    private static final String PROXY_PREFIX = "/proxy";
    private static final String UPSTREAM_HEADER = "X-LoadBalancerPro-Upstream";
    private static final String STRATEGY_HEADER = "X-LoadBalancerPro-Strategy";
    private static final Duration MAXIMUM_SLOW_START_DURATION = Duration.ofHours(24);
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "content-length",
            "expect",
            "host",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "proxy-connection",
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
    private final ConcurrentMap<String, ResilienceState> resilienceStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UpstreamRuntimeStats> upstreamRuntimeStats = new ConcurrentHashMap<>();
    private final UpstreamRuntimeStats globalRuntimeStats;
    private final LiveRoutingDecisionStore liveRoutingDecisions;
    private final LaseShadowRuntime laseShadowRuntime;
    private final UpstreamHealthProber healthProber;

    @Autowired
    public ReverseProxyService(ReverseProxyProperties properties,
                               HttpClient httpClient,
                               ReverseProxyMetrics metrics,
                               ObjectProvider<LaseShadowRuntime> laseShadowRuntimeProvider) {
        this(properties, httpClient, metrics, RoutingStrategyRegistry.defaultRegistry(), Clock.systemUTC(),
                laseShadowRuntimeProvider.getIfAvailable(LaseShadowRuntime::disabled));
    }

    ReverseProxyService(ReverseProxyProperties properties,
                        HttpClient httpClient,
                        ReverseProxyMetrics metrics,
                        RoutingStrategyRegistry registry,
                        Clock clock) {
        this(properties, httpClient, metrics, registry, clock, LaseShadowRuntime.disabled());
    }

    ReverseProxyService(ReverseProxyProperties properties,
                        HttpClient httpClient,
                        ReverseProxyMetrics metrics,
                        RoutingStrategyRegistry registry,
                        Clock clock,
                        LaseShadowRuntime laseShadowRuntime) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics cannot be null");
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.laseShadowRuntime = Objects.requireNonNull(laseShadowRuntime, "laseShadowRuntime cannot be null");
        this.globalRuntimeStats = new UpstreamRuntimeStats(clock);
        this.liveRoutingDecisions = new LiveRoutingDecisionStore(clock);
        ActiveProxyConfig startupConfig = buildActiveConfig(
                properties, nextGeneration.getAndIncrement(), List.of());
        prepareResilienceStates(startupConfig, Instant.now(clock));
        this.activeConfig = new AtomicReference<>(startupConfig);
        this.reloadState = new AtomicReference<>(ReloadState.notAttempted(startupConfig));
        this.healthProber = new UpstreamHealthProber(httpClient, clock, this::recordProbeOutcome);
        configureHealthProber(startupConfig);
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
        ProxyAdmissionControl.Admission admission =
                config.admissionPolicy().tryAcquire(request, globalRuntimeStats);
        if (!admission.acquired()) {
            logger.warn("proxy.forward.rejected reason={} priority={} detail={}",
                    admission.errorCode(), admission.priority(), admission.reason());
            metrics.recordFailure(null, HttpStatus.SERVICE_UNAVAILABLE.value());
            return overloadResponse(
                    admission.errorCode(), admission.message(), admission.retryAfterSeconds());
        }
        long startedAtNanos = System.nanoTime();
        ReverseProxyResponse response = null;
        try {
            response = forwardAdmitted(
                    config, route, request, body, proxyPathSuffix, admission.retryAfterSeconds());
            return response;
        } finally {
            long elapsedNanos = Math.max(0, System.nanoTime() - startedAtNanos);
            globalRuntimeStats.requestCompleted(
                    Duration.ofNanos(elapsedNanos), response != null && response.statusCode() < 500);
            config.admissionPolicy().requestCompleted(globalRuntimeStats);
        }
    }

    private ReverseProxyResponse forwardAdmitted(
            ActiveProxyConfig config,
            ReverseProxyRoutePlanner.ConfiguredRoute route,
            HttpServletRequest request,
            byte[] body,
            String proxyPathSuffix,
            int retryAfterSeconds) {
        ReverseProxyProperties properties = config.properties();
        int maxAttempts = maxAttemptsFor(request.getMethod(), properties);
        config.retryPolicy().recordPrimaryRequest();
        String routingKey = route.selectionPolicy().routingKey(request);
        Set<String> attemptedUpstreamIds = new LinkedHashSet<>();
        ReverseProxyResponse lastResponse = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Set<String> capacityExcludedIds = new LinkedHashSet<>();
            while (true) {
                Set<String> excludedIds = new LinkedHashSet<>(attemptedUpstreamIds);
                excludedIds.addAll(capacityExcludedIds);
                List<UpstreamCandidate> upstreams = configuredUpstreams(config, route, excludedIds);
                if (upstreams.isEmpty()) {
                    if (lastResponse != null) {
                        return lastResponse;
                    }
                    if (!capacityExcludedIds.isEmpty()) {
                        logger.warn("proxy.forward.rejected reason=upstream_concurrency_limit route={} targets={}",
                                route.name(), capacityExcludedIds);
                        metrics.recordFailure(null, HttpStatus.SERVICE_UNAVAILABLE.value());
                        return overloadResponse(
                                "proxy_upstream_concurrency_limit",
                                "All eligible proxy upstreams are at their in-flight limits.",
                                retryAfterSeconds);
                    }
                    logger.warn("proxy.forward.failure reason=no_configured_upstreams route={} attempt={}",
                            route.name(), attempt);
                    metrics.recordFailure(null, HttpStatus.SERVICE_UNAVAILABLE.value());
                    return proxyError(HttpStatus.SERVICE_UNAVAILABLE, "proxy_unavailable",
                            "No proxy upstreams are configured for route " + route.name() + ".");
                }

                List<ServerStateVector> candidateStates = upstreams.stream()
                        .map(UpstreamCandidate::state)
                        .toList();
                Optional<String> selectedServerId = route.selectionPolicy()
                        .affinityTarget(request, candidateStates);
                String selectionSource = selectedServerId.isPresent() ? "affinity" : "strategy";
                if (selectedServerId.isEmpty()) {
                    RoutingDecision decision = route.strategy().chooseForKey(candidateStates, routingKey);
                    selectedServerId = decision.explanation().chosenServerId();
                }
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
                UpstreamRuntimeStats runtimeStats = runtimeStatsFor(upstreamId);
                if (!runtimeStats.tryRequestStarted(upstream.maxInFlight(), ignored -> true)) {
                    capacityExcludedIds.add(upstreamId);
                    continue;
                }
                attemptedUpstreamIds.add(upstreamId);
                if (attempt > 1) {
                    logger.info("proxy.forward.retry route={} attempt={} upstreamId={}",
                            route.name(), attempt, upstreamId);
                    metrics.recordRetryAttempt(upstreamId);
                }
                long attemptStartedAtNanos = System.nanoTime();
                ForwardAttemptResult attemptResult =
                        forwardOnce(properties, config.forwardedPolicy(), route.headerRewrites(), request, body,
                                upstream, runtimeStats, route.strategyId().externalName(),
                                route.requestTimeout(), proxyPathSuffix);
                double attemptLatencyMillis = Math.max(0, System.nanoTime() - attemptStartedAtNanos)
                        / 1_000_000.0;
                LiveRoutingDecisionRecord liveDecision = liveRoutingDecisions.record(
                        config.generation(),
                        route.name(),
                        route.strategyId().externalName(),
                        attempt,
                        selectionSource,
                        upstreamId,
                        candidateStates,
                        attemptResult.response().statusCode(),
                        attemptLatencyMillis,
                        attemptResult.retriable(),
                        attemptResult.outcome());
                if (laseShadowRuntime.isLiveProxyEnabled()) {
                    int telemetrySampleSize = upstreams.stream()
                            .mapToInt(UpstreamCandidate::telemetrySampleSize)
                            .reduce(0, ReverseProxyService::saturatedAdd);
                    int initialConcurrencyLimit = liveShadowConcurrencyLimit(config, upstreams, candidateStates);
                    laseShadowRuntime.submitLiveRouting(new LiveRoutingShadowObservation(
                            liveDecision.decisionId(),
                            liveDecision.capturedAt(),
                            liveDecision.routeName(),
                            liveDecision.strategy(),
                            liveDecision.selectionSource(),
                            liveDecision.chosenUpstreamId(),
                            candidateStates,
                            initialConcurrencyLimit,
                            telemetrySampleSize));
                }
                lastResponse = route.selectionPolicy().applyAffinityResponse(
                        attemptResult.response(), request, upstreamId, attemptResult.affinityEligible());
                if (!attemptResult.retriable() || attempt == maxAttempts) {
                    return lastResponse;
                }
                if (!config.retryPolicy().tryAcquireRetry()) {
                    logger.warn("proxy.forward.retry_suppressed route={} attempt={} reason=retry_budget_exhausted",
                            route.name(), attempt + 1);
                    return lastResponse;
                }
                try {
                    config.retryPolicy().pauseBeforeRetry(attempt);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    logger.warn("proxy.forward.retry_suppressed route={} attempt={} reason=backoff_interrupted",
                            route.name(), attempt + 1);
                    return lastResponse;
                }
                break;
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
                configuredUpstreamStatuses(config, Instant.now(clock));
        List<String> upstreamIds = upstreamStatuses.stream()
                .map(ReverseProxyStatusResponse.UpstreamStatus::id)
                .toList();
        ReverseProxyProperties.HealthCheck healthCheck = properties.getHealthCheck();
        ReverseProxyProperties.Retry retry = properties.getRetry();
        ReverseProxyProperties.Cooldown cooldown = properties.getCooldown();
        ProxyRetryPolicy.Snapshot retryPolicy = config.retryPolicy().snapshot();
        ProxyAdmissionControl.Status admissionStatus = config.admissionPolicy().status(globalRuntimeStats);
        List<ReverseProxyStatusResponse.RouteStatus> routeStatuses = routes.stream()
                .map(route -> new ReverseProxyStatusResponse.RouteStatus(
                        route.name(),
                        route.pathPrefix(),
                        route.strategyId().externalName(),
                        route.selectionPolicy().hashOnDescription(),
                        route.selectionPolicy().affinityEnabled(),
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
                        healthCheck.getInterval().toMillis(),
                        healthCheck.getHealthyThreshold(),
                        healthCheck.getUnhealthyThreshold()),
                new ReverseProxyStatusResponse.RetryStatus(
                        retry.isEnabled(),
                        Math.max(1, retry.getMaxAttempts()),
                        retryPolicy.budgetPercent(),
                        retryPolicy.backoffBaseMillis(),
                        retryPolicy.backoffMaxMillis(),
                        retryPolicy.primaryRequests(),
                        retryPolicy.grantedRetries(),
                        retryPolicy.rejectedRetries(),
                        retryPolicy.availableCreditsPercent(),
                        retry.isRetryNonIdempotent(),
                        normalizedRetryMethods(properties).stream().sorted().toList(),
                        normalizedRetryStatuses(properties).stream().sorted().toList()),
                new ReverseProxyStatusResponse.CooldownStatus(
                        cooldown.isEnabled(),
                        Math.max(1, cooldown.getConsecutiveFailureThreshold()),
                        Math.max(0, cooldown.getDuration().toMillis()),
                        cooldown.isRecoverOnSuccessfulHealthCheck(),
                        Math.max(0, properties.getSlowStart().getDuration().toMillis())),
                new ReverseProxyStatusResponse.LimitsStatus(
                        admissionStatus.configuredMaxInFlight(),
                        admissionStatus.effectiveMaxInFlight(),
                        admissionStatus.currentInFlight(),
                        admissionStatus.adaptiveEnabled(),
                        admissionStatus.lastAdaptiveAction(),
                        admissionStatus.lastAdaptiveReason(),
                        isoInstant(admissionStatus.lastAdaptiveUpdate())),
                new ReverseProxyStatusResponse.LoadSheddingStatus(
                        admissionStatus.sheddingEnabled(),
                        admissionStatus.sheddingConfig().softUtilizationThreshold(),
                        admissionStatus.sheddingConfig().hardUtilizationThreshold(),
                        admissionStatus.sheddingConfig().maxQueueDepth(),
                        admissionStatus.sheddingConfig().maxP95LatencyMillis(),
                        admissionStatus.sheddingConfig().maxErrorRate(),
                        admissionStatus.sheddingConfig().criticalBypassEnabled(),
                        admissionStatus.sheddingConfig().shedUserOnHardPressure(),
                        admissionStatus.priorityHeader(),
                        admissionStatus.retryAfterSeconds()),
                routeStatuses,
                upstreamStatuses,
                metricsSnapshot,
                ReverseProxyStatusSummaries.observability(properties.isEnabled(), routeStatuses, upstreamStatuses,
                        metricsSnapshot),
                ReverseProxyStatusSummaries.controllerNotAvailableSecurityBoundary(),
                PrivateNetworkLiveValidationStatusResponse.from(properties),
                reloadStatusSnapshot(config));
    }

    RecentProxyDecisionsResponse recentDecisionsSnapshot() {
        return liveRoutingDecisions.snapshot(true);
    }

    PrivateNetworkLiveValidationCommandResponse privateNetworkLiveValidationCommand(
            PrivateNetworkLiveValidationCommandRequest request) {
        return PrivateNetworkLiveValidationCommandResponse.from(activeConfig.get().properties(), request);
    }

    synchronized ReverseProxyReloadResponse reload(ReverseProxyProperties candidateProperties) {
        Instant attemptedAt = Instant.now(clock);
        ActiveProxyConfig previousConfig = activeConfig.get();
        try {
            ActiveProxyConfig candidateConfig = buildActiveConfigForReload(
                    candidateProperties, previousConfig);
            prepareResilienceStates(candidateConfig, attemptedAt);
            activeConfig.set(candidateConfig);
            nextGeneration.updateAndGet(current -> Math.max(current, candidateConfig.generation() + 1));
            retainConfiguredResilienceStates(candidateConfig);
            retainConfiguredRuntimeStats(candidateConfig);
            configureHealthProber(candidateConfig);
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

    private ActiveProxyConfig buildActiveConfigForReload(
            ReverseProxyProperties candidateProperties,
            ActiveProxyConfig previousConfig) {
        if (candidateProperties == null) {
            throw new IllegalStateException("reload payload must include proxy configuration");
        }
        if (!candidateProperties.isEnabled()) {
            throw new IllegalStateException("loadbalancerpro.proxy.enabled must be true for runtime reload");
        }
        if (!Objects.equals(
                candidateProperties.getConnectTimeout(),
                previousConfig.properties().getConnectTimeout())) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.connect-timeout requires application restart and cannot change by reload");
        }
        return buildActiveConfig(
                candidateProperties, nextGeneration.get(), previousConfig.routes());
    }

    private ActiveProxyConfig buildActiveConfig(
            ReverseProxyProperties candidateProperties,
            long generation,
            List<ReverseProxyRoutePlanner.ConfiguredRoute> previousRoutes) {
        ReverseProxyProperties safeProperties = copyProperties(
                Objects.requireNonNull(candidateProperties, "properties cannot be null"));
        List<ReverseProxyRoutePlanner.ConfiguredRoute> configuredRoutes =
                ReverseProxyRoutePlanner.buildEnabledRoutes(safeProperties, registry, previousRoutes);
        validateRuntimeFields(safeProperties, configuredRoutes);
        ProxyRequestHeaders.ForwardedPolicy forwardedPolicy =
                ProxyRequestHeaders.compileForwarded(safeProperties.getForwarded());
        ProxyAdmissionControl.Policy admissionPolicy = ProxyAdmissionControl.compile(safeProperties, clock);
        ProxyRetryPolicy retryPolicy = ProxyRetryPolicy.compile(safeProperties.getRetry());
        return new ActiveProxyConfig(
                safeProperties, configuredRoutes, forwardedPolicy, admissionPolicy, retryPolicy, generation);
    }

    private void validateRuntimeFields(ReverseProxyProperties properties,
                                       List<ReverseProxyRoutePlanner.ConfiguredRoute> configuredRoutes) {
        positiveDuration(properties.getConnectTimeout(), "loadbalancerpro.proxy.connect-timeout");
        positiveDuration(properties.getRequestTimeout(), "loadbalancerpro.proxy.request-timeout");
        if (properties.getMaxRequestBytes() <= 0) {
            throw new IllegalStateException("loadbalancerpro.proxy.max-request-bytes must be greater than 0");
        }
        normalizedHealthCheckPath(properties.getHealthCheck().getPath());
        positiveDuration(properties.getHealthCheck().getTimeout(),
                "loadbalancerpro.proxy.health-check.timeout");
        positiveDuration(properties.getHealthCheck().getInterval(),
                "loadbalancerpro.proxy.health-check.interval");
        positiveInt(properties.getHealthCheck().getHealthyThreshold(),
                "loadbalancerpro.proxy.health-check.healthy-threshold");
        positiveInt(properties.getHealthCheck().getUnhealthyThreshold(),
                "loadbalancerpro.proxy.health-check.unhealthy-threshold");
        normalizedRetryMethods(properties);
        normalizedRetryStatuses(properties);
        positiveInt(properties.getRetry().getMaxAttempts(), "loadbalancerpro.proxy.retry.max-attempts");
        nonNegativeDuration(properties.getCooldown().getDuration(), "loadbalancerpro.proxy.cooldown.duration");
        boundedNonNegativeDuration(
                properties.getSlowStart().getDuration(),
                MAXIMUM_SLOW_START_DURATION,
                "loadbalancerpro.proxy.slow-start.duration");
        for (ReverseProxyRoutePlanner.ConfiguredRoute route : configuredRoutes) {
            positiveDuration(
                    route.requestTimeout(),
                    "loadbalancerpro.proxy.routes." + route.name() + ".request-timeout");
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
        nonNegative(upstream.getMaxInFlight(), "maxInFlight");
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
    private HttpRequest buildOutboundRequest(HttpServletRequest request,
                                             byte[] body,
                                             UpstreamCandidate upstream,
                                             Duration requestTimeout,
                                             String proxyPathSuffix,
                                             ProxyRequestHeaders.ForwardedPolicy forwardedPolicy,
                                             ProxyRequestHeaders.HeaderRewrites headerRewrites) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(targetUri(request, upstream, proxyPathSuffix))
                .timeout(requestTimeout);
        Set<String> connectionHeaders = connectionHeaderTokens(request);
        Collections.list(request.getHeaderNames()).forEach(headerName -> {
            if (isForwardableHeader(headerName)
                    && !connectionHeaders.contains(headerName.toLowerCase(Locale.ROOT))
                    && !ProxyRequestHeaders.isSpoofable(headerName)
                    && !headerRewrites.removes(headerName)) {
                Collections.list(request.getHeaders(headerName))
                        .forEach(headerValue -> builder.header(headerName, headerValue));
            }
        });
        forwardedPolicy.apply(builder, request, headerRewrites);
        headerRewrites.apply(builder);
        HttpRequest.BodyPublisher publisher = body.length == 0
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(body);
        return builder.method(request.getMethod().toUpperCase(Locale.ROOT), publisher).build();
    }

    private ForwardAttemptResult forwardOnce(ReverseProxyProperties properties,
                                             ProxyRequestHeaders.ForwardedPolicy forwardedPolicy,
                                             ProxyRequestHeaders.HeaderRewrites headerRewrites,
                                             HttpServletRequest request,
                                             byte[] body,
                                             UpstreamCandidate upstream,
                                             UpstreamRuntimeStats runtimeStats,
                                             String strategyName,
                                             Duration requestTimeout,
                                             String proxyPathSuffix) {
        String upstreamId = upstream.state().serverId();
        long startedAtNanos = System.nanoTime();
        boolean runtimeSuccessful = false;
        try {
            HttpRequest outbound = buildOutboundRequest(
                    request, body, upstream, requestTimeout, proxyPathSuffix, forwardedPolicy, headerRewrites);
            HttpResponse<byte[]> response = httpClient.send(outbound, HttpResponse.BodyHandlers.ofByteArray());
            runtimeSuccessful = response.statusCode() < 500;
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
                return new ForwardAttemptResult(proxyResponse, true, false, "upstream_response");
            }
            recordResilienceSuccess(upstreamId);
            return new ForwardAttemptResult(proxyResponse, false, runtimeSuccessful, "upstream_response");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.warn("proxy.forward.failure upstreamId={} reason=interrupted", upstreamId);
            metrics.recordFailure(upstreamId, HttpStatus.BAD_GATEWAY.value());
            recordResilienceFailure(properties, upstreamId);
            return new ForwardAttemptResult(
                    proxyError(HttpStatus.BAD_GATEWAY, "proxy_upstream_failure",
                            "Proxy forwarding was interrupted while calling upstream " + upstreamId),
                    false,
                    false,
                    "interrupted");
        } catch (IOException | IllegalArgumentException exception) {
            logger.warn("proxy.forward.failure upstreamId={} reason=upstream_unreachable exceptionType={}",
                    upstreamId, exception.getClass().getSimpleName());
            metrics.recordFailure(upstreamId, HttpStatus.BAD_GATEWAY.value());
            recordResilienceFailure(properties, upstreamId);
            return new ForwardAttemptResult(
                    proxyError(HttpStatus.BAD_GATEWAY, "proxy_upstream_failure",
                            "Proxy could not reach upstream " + upstreamId),
                    true,
                    false,
                    "upstream_failure");
        } finally {
            long elapsedNanos = Math.max(0, System.nanoTime() - startedAtNanos);
            runtimeStats.requestCompleted(Duration.ofNanos(elapsedNanos), runtimeSuccessful);
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

    private List<UpstreamCandidate> configuredUpstreams(ActiveProxyConfig config,
                                                         ReverseProxyRoutePlanner.ConfiguredRoute route,
                                                         Set<String> excludedUpstreamIds) {
        Instant now = Instant.now(clock);
        return route.targets().stream()
                .filter(upstream -> excludedUpstreamIds == null
                        || !excludedUpstreamIds.contains(requireNonBlank(
                                upstream.getId(), "loadbalancerpro.proxy.upstreams[].id")))
                .map(upstream -> toCandidate(config, upstream, now))
                .toList();
    }

    private UpstreamCandidate toCandidate(ActiveProxyConfig config,
                                          ReverseProxyProperties.Upstream upstream,
                                          Instant timestamp) {
        ReverseProxyProperties properties = config.properties();
        String id = requireNonBlank(upstream.getId(), "loadbalancerpro.proxy.upstreams[].id");
        URI baseUri = configuredBaseUri(upstream, id);
        EffectiveHealth effectiveHealth =
                effectiveHealth(properties, upstream, id, config.generation(), timestamp);
        int configuredInFlight = nonNegative(upstream.getInFlightRequestCount(), "inFlightRequestCount");
        double configuredAverageLatency =
                nonNegative(upstream.getAverageLatencyMillis(), "averageLatencyMillis");
        double configuredP95Latency = nonNegative(upstream.getP95LatencyMillis(), "p95LatencyMillis");
        double configuredP99Latency = nonNegative(upstream.getP99LatencyMillis(), "p99LatencyMillis");
        double configuredErrorRate = rate(upstream.getRecentErrorRate(), "recentErrorRate");
        OptionalInt configuredQueueDepth = optionalNonNegativeInt(upstream.getQueueDepth(), "queueDepth");
        UpstreamRuntimeStats.Snapshot runtime = runtimeStatsFor(id).snapshot();
        boolean hasRuntimeActivity =
                runtime.inFlightRequestCount() > 0 || runtime.completedRequestCount() > 0;
        boolean hasLatencySamples = runtime.latencySampleCount() > 0;
        boolean hasRecentOutcomes =
                runtime.recentSuccessCount() + runtime.recentFailureCount() > 0;
        double configuredWeight = nonNegative(upstream.getWeight(), "weight");
        double effectiveWeight = resilienceState(id, timestamp).effectiveWeight(
                configuredWeight, timestamp, properties.getSlowStart().getDuration());
        int effectiveMaxInFlight = effectiveUpstreamMaxInFlight(config, id);
        OptionalDouble estimatedConcurrencyLimit = optionalPositive(
                upstream.getEstimatedConcurrencyLimit(), "estimatedConcurrencyLimit");
        if (estimatedConcurrencyLimit.isEmpty() && effectiveMaxInFlight > 0) {
            estimatedConcurrencyLimit = OptionalDouble.of(effectiveMaxInFlight);
        }
        ServerStateVector state = new ServerStateVector(
                id,
                effectiveHealth.healthy(),
                hasRuntimeActivity ? runtime.inFlightRequestCount() : configuredInFlight,
                optionalNonNegative(upstream.getConfiguredCapacity(), "configuredCapacity"),
                estimatedConcurrencyLimit,
                effectiveWeight,
                hasLatencySamples ? runtime.ewmaLatencyMillis() : configuredAverageLatency,
                hasLatencySamples ? runtime.p95LatencyMillis() : configuredP95Latency,
                hasLatencySamples ? runtime.p99LatencyMillis() : configuredP99Latency,
                hasRecentOutcomes ? runtime.recentErrorRate() : configuredErrorRate,
                hasRuntimeActivity ? OptionalInt.of(runtime.inFlightRequestCount()) : configuredQueueDepth,
                NetworkAwarenessSignal.neutral(id, timestamp),
                timestamp);
        int telemetrySampleSize = Math.max(
                runtime.latencySampleCount(),
                saturatedAdd(runtime.recentSuccessCount(), runtime.recentFailureCount()));
        return new UpstreamCandidate(
                baseUri, state, effectiveMaxInFlight, telemetrySampleSize);
    }

    private int liveShadowConcurrencyLimit(
            ActiveProxyConfig config,
            List<UpstreamCandidate> upstreams,
            List<ServerStateVector> candidateStates) {
        int globalLimit = config.admissionPolicy().status(globalRuntimeStats).effectiveMaxInFlight();
        if (globalLimit > 0) {
            return globalLimit;
        }
        boolean everyCandidateBounded = upstreams.stream()
                .allMatch(candidate -> candidate.maxInFlight() > 0);
        if (everyCandidateBounded) {
            return upstreams.stream()
                    .mapToInt(UpstreamCandidate::maxInFlight)
                    .reduce(0, ReverseProxyService::saturatedAdd);
        }
        int observedInFlight = candidateStates.stream()
                .mapToInt(ServerStateVector::inFlightRequestCount)
                .reduce(0, ReverseProxyService::saturatedAdd);
        return Math.max(1, observedInFlight);
    }

    private List<ReverseProxyStatusResponse.UpstreamStatus> configuredUpstreamStatuses(
            ActiveProxyConfig config,
            Instant now) {
        ReverseProxyProperties properties = config.properties();
        return config.routes().stream()
                .flatMap(route -> route.targets().stream()
                        .map(upstream -> {
                            String id = requireNonBlank(upstream.getId(), "loadbalancerpro.proxy.upstreams[].id");
                            URI baseUri = configuredBaseUri(upstream, id);
                            EffectiveHealth health =
                                    effectiveHealth(properties, upstream, id, config.generation(), now);
                            ResilienceState resilienceState = resilienceStates.get(id);
                            UpstreamRuntimeStats runtimeStats = upstreamRuntimeStats.get(id);
                            UpstreamRuntimeStats.Snapshot runtime = runtimeStats == null
                                    ? UpstreamRuntimeStats.emptySnapshot()
                                    : runtimeStats.snapshot();
                            double configuredWeight = nonNegative(upstream.getWeight(), "weight");
                            ResilienceState.SlowStartSnapshot slowStart = resilienceState == null
                                    ? ResilienceState.SlowStartSnapshot.full(configuredWeight)
                                    : resilienceState.slowStartSnapshot(
                                            configuredWeight, now, properties.getSlowStart().getDuration());
                            return new ReverseProxyStatusResponse.UpstreamStatus(
                                    id,
                                    safeConfiguredUrl(baseUri),
                                    upstream.isHealthy(),
                                    health.healthy(),
                                    health.source(),
                                    health.lastProbeStatusCode(),
                                    health.lastProbeOutcome(),
                                    resilienceState == null ? 0 : resilienceState.consecutiveFailures(now),
                                    resilienceState != null && resilienceState.cooldownActive(now),
                                    resilienceState == null ? 0 : resilienceState.cooldownRemainingMillis(now),
                                    configuredWeight,
                                    slowStart.effectiveWeight(),
                                    slowStart.active(),
                                    slowStart.remainingMillis(),
                                    effectiveUpstreamMaxInFlight(config, id),
                                    new ReverseProxyStatusResponse.UpstreamRuntimeStatus(
                                            runtime.inFlightRequestCount(),
                                            runtime.completedRequestCount(),
                                            runtime.latencySampleCount(),
                                            runtime.ewmaLatencyMillis(),
                                            runtime.p50LatencyMillis(),
                                            runtime.p95LatencyMillis(),
                                            runtime.p99LatencyMillis(),
                                            runtime.recentSuccessCount(),
                                            runtime.recentFailureCount(),
                                            runtime.recentErrorRate(),
                                            isoInstant(runtime.lastUpdatedAt())));
                        }))
                .toList();
    }

    private static int effectiveUpstreamMaxInFlight(ActiveProxyConfig config, String upstreamId) {
        return config.routes().stream()
                .flatMap(route -> route.targets().stream())
                .filter(target -> target.getId() != null && upstreamId.equals(target.getId().trim()))
                .mapToInt(ReverseProxyProperties.Upstream::getMaxInFlight)
                .filter(limit -> limit > 0)
                .min()
                .orElse(0);
    }

    private UpstreamRuntimeStats runtimeStatsFor(String upstreamId) {
        return upstreamRuntimeStats.computeIfAbsent(
                requireNonBlank(upstreamId, "upstreamId"),
                ignored -> new UpstreamRuntimeStats(clock));
    }

    private void retainConfiguredRuntimeStats(ActiveProxyConfig config) {
        Set<String> configuredIds = configuredUpstreamIds(config);
        upstreamRuntimeStats.keySet().retainAll(configuredIds);
    }

    private void prepareResilienceStates(ActiveProxyConfig config, Instant addedAt) {
        configuredUpstreamIds(config).forEach(
                upstreamId -> resilienceStates.computeIfAbsent(
                        upstreamId, ignored -> new ResilienceState(addedAt)));
    }

    private void retainConfiguredResilienceStates(ActiveProxyConfig config) {
        resilienceStates.keySet().retainAll(configuredUpstreamIds(config));
    }

    private static Set<String> configuredUpstreamIds(ActiveProxyConfig config) {
        return config.routes().stream()
                .flatMap(route -> route.targets().stream())
                .map(ReverseProxyProperties.Upstream::getId)
                .map(String::trim)
                .collect(Collectors.toSet());
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
                                            long configurationGeneration,
                                            Instant now) {
        if (!upstream.isHealthy()) {
            return new EffectiveHealth(false, "CONFIGURED_DISABLED", null, "configured healthy=false");
        }
        ReverseProxyProperties.HealthCheck healthCheck = properties.getHealthCheck();
        ResilienceState resilienceState = resilienceStates.get(id);
        Optional<UpstreamHealthProber.HealthSnapshot> probeSnapshot =
                healthProber.snapshot(id, configurationGeneration);
        if (properties.getCooldown().isEnabled()
                && resilienceState != null
                && resilienceState.cooldownActive(now)) {
            return new EffectiveHealth(
                    false,
                    "COOLDOWN",
                    probeSnapshot.map(UpstreamHealthProber.HealthSnapshot::statusCode).orElse(null),
                    "temporary cooldown active");
        }
        if (!healthCheck.isEnabled()) {
            return new EffectiveHealth(true, "CONFIGURED", null, "active health checks disabled");
        }
        return probeSnapshot
                .map(snapshot -> new EffectiveHealth(
                        snapshot.healthy(),
                        snapshot.checkedAt().equals(Instant.EPOCH) ? "ACTIVE_PROBE_PENDING" : "ACTIVE_PROBE",
                        snapshot.statusCode(),
                        snapshot.outcome()))
                .orElseGet(() -> new EffectiveHealth(
                        true,
                        "ACTIVE_PROBE_PENDING",
                        null,
                        "awaiting first background probe"));
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
        Instant now = Instant.now(clock);
        boolean activated = resilienceState(upstreamId, now).recordFailure(now, properties.getCooldown());
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
        Instant now = Instant.now(clock);
        resilienceState(upstreamId, now).recordSuccess(now);
    }

    private ResilienceState resilienceState(String upstreamId, Instant firstEligibleAt) {
        return resilienceStates.computeIfAbsent(
                upstreamId, ignored -> new ResilienceState(firstEligibleAt));
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

    static boolean isHopByHopHeader(String headerName) {
        return headerName != null && HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
    }

    private static boolean isForwardableHeader(String headerName) {
        return headerName != null && !isHopByHopHeader(headerName);
    }

    private static Set<String> connectionHeaderTokens(HttpServletRequest request) {
        Set<String> tokens = new LinkedHashSet<>();
        java.util.Enumeration<String> connectionValues = request.getHeaders("Connection");
        if (connectionValues == null) {
            return Set.of();
        }
        Collections.list(connectionValues).forEach(value -> {
            for (String token : value.split(",")) {
                String normalized = token.trim().toLowerCase(Locale.ROOT);
                if (!normalized.isEmpty()) {
                    tokens.add(normalized);
                }
            }
        });
        return Set.copyOf(tokens);
    }

    private static ReverseProxyResponse proxyError(HttpStatus status, String error, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\":\"" + jsonEscape(error) + "\",\"message\":\"" + jsonEscape(message) + "\"}";
        return new ReverseProxyResponse(status.value(), headers, body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static ReverseProxyResponse overloadResponse(String error, String message, int retryAfterSeconds) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.RETRY_AFTER, Integer.toString(Math.max(1, retryAfterSeconds)));
        String body = "{\"error\":\"" + jsonEscape(error) + "\",\"message\":\"" + jsonEscape(message) + "\"}";
        return new ReverseProxyResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                headers,
                body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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

    private void configureHealthProber(ActiveProxyConfig config) {
        ReverseProxyProperties properties = config.properties();
        ReverseProxyProperties.HealthCheck healthCheck = properties.getHealthCheck();
        if (!healthCheck.isEnabled()) {
            healthProber.stop();
            return;
        }
        List<UpstreamHealthProber.Target> targets = config.routes().stream()
                .flatMap(route -> route.targets().stream())
                .map(upstream -> {
                    String id = requireNonBlank(upstream.getId(), "loadbalancerpro.proxy.upstreams[].id");
                    URI baseUri = configuredBaseUri(upstream, id);
                    return new UpstreamHealthProber.Target(
                            id,
                            healthCheckUri(properties, baseUri),
                            healthCheck.getTimeout(),
                            config.generation());
                })
                .toList();
        healthProber.configure(
                targets,
                healthCheck.getInterval(),
                healthCheck.getHealthyThreshold(),
                healthCheck.getUnhealthyThreshold());
    }

    private synchronized void recordProbeOutcome(
            UpstreamHealthProber.Target target,
            boolean successful,
            UpstreamHealthProber.HealthSnapshot snapshot) {
        ActiveProxyConfig config = activeConfig.get();
        if (target.configurationGeneration() != config.generation()) {
            return;
        }
        String upstreamId = target.id();
        ReverseProxyProperties properties = config.properties();
        if (!properties.getHealthCheck().isEnabled()
                || config.routes().stream()
                        .flatMap(route -> route.targets().stream())
                        .map(ReverseProxyProperties.Upstream::getId)
                        .noneMatch(upstreamId::equals)) {
            return;
        }
        if (!successful) {
            recordResilienceFailure(properties, upstreamId);
            return;
        }
        ResilienceState resilienceState = resilienceStates.get(upstreamId);
        if (resilienceState == null) {
            return;
        }
        boolean mayRecover = !properties.getCooldown().isEnabled()
                || properties.getCooldown().isRecoverOnSuccessfulHealthCheck()
                || !resilienceState.cooldownActive(snapshot.checkedAt());
        if (mayRecover) {
            resilienceState.recordSuccess(snapshot.checkedAt());
        }
    }

    @PreDestroy
    void closeHealthProber() {
        healthProber.close();
    }

    private static Duration positiveDuration(Duration value, String fieldName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException(fieldName + " must be greater than zero");
        }
        return value;
    }

    private static Duration nonNegativeDuration(Duration value, String fieldName) {
        if (value == null || value.isNegative()) {
            throw new IllegalStateException(fieldName + " must be greater than or equal to zero");
        }
        return value;
    }

    private static Duration boundedNonNegativeDuration(
            Duration value, Duration maximum, String fieldName) {
        nonNegativeDuration(value, fieldName);
        if (value.compareTo(maximum) > 0) {
            throw new IllegalStateException(
                    fieldName + " must be no greater than " + maximum.toHours() + "h");
        }
        return value;
    }

    private static int positiveInt(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalStateException(fieldName + " must be greater than zero");
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
        copy.setConnectTimeout(source.getConnectTimeout());
        copy.setRequestTimeout(source.getRequestTimeout());
        copy.setMaxRequestBytes(source.getMaxRequestBytes());
        copy.setPrivateNetworkValidation(copyPrivateNetworkValidation(source.getPrivateNetworkValidation()));
        copy.setPrivateNetworkLiveValidation(copyPrivateNetworkLiveValidation(
                source.getPrivateNetworkLiveValidation()));
        copy.setHealthCheck(copyHealthCheck(source.getHealthCheck()));
        copy.setRetry(copyRetry(source.getRetry()));
        copy.setCooldown(copyCooldown(source.getCooldown()));
        copy.setSlowStart(copySlowStart(source.getSlowStart()));
        copy.setForwarded(copyForwarded(source.getForwarded()));
        copy.setLimits(copyLimits(source.getLimits()));
        copy.setShedding(copyShedding(source.getShedding()));
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
        copy.setHashOn(source.getHashOn());
        copy.setRequestTimeout(source.getRequestTimeout());
        copy.setAffinity(copyAffinity(source.getAffinity()));
        copy.setHeaders(copyHeaders(source.getHeaders()));
        copy.setTargets(source.getTargets().stream()
                .map(ReverseProxyService::copyUpstream)
                .toList());
        return copy;
    }

    private static ReverseProxyProperties.Affinity copyAffinity(ReverseProxyProperties.Affinity source) {
        ReverseProxyProperties.Affinity copy = new ReverseProxyProperties.Affinity();
        if (source == null) {
            return copy;
        }
        copy.setCookieName(source.getCookieName());
        copy.setHmacKey(source.getHmacKey());
        return copy;
    }

    private static ReverseProxyProperties.Forwarded copyForwarded(ReverseProxyProperties.Forwarded source) {
        ReverseProxyProperties.Forwarded copy = new ReverseProxyProperties.Forwarded();
        if (source == null) {
            return copy;
        }
        copy.setMode(source.getMode());
        copy.setTrustedProxies(source.getTrustedProxies());
        return copy;
    }

    private static ReverseProxyProperties.Headers copyHeaders(ReverseProxyProperties.Headers source) {
        ReverseProxyProperties.Headers copy = new ReverseProxyProperties.Headers();
        if (source == null) {
            return copy;
        }
        copy.setAdd(source.getAdd());
        copy.setSet(source.getSet());
        copy.setRemove(source.getRemove());
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
        copy.setMaxInFlight(source.getMaxInFlight());
        copy.setWeight(source.getWeight());
        copy.setAverageLatencyMillis(source.getAverageLatencyMillis());
        copy.setP95LatencyMillis(source.getP95LatencyMillis());
        copy.setP99LatencyMillis(source.getP99LatencyMillis());
        copy.setRecentErrorRate(source.getRecentErrorRate());
        copy.setQueueDepth(source.getQueueDepth());
        return copy;
    }

    private static ReverseProxyProperties.Limits copyLimits(ReverseProxyProperties.Limits source) {
        ReverseProxyProperties.Limits copy = new ReverseProxyProperties.Limits();
        if (source == null) {
            return copy;
        }
        copy.setMaxInFlight(source.getMaxInFlight());
        copy.setAdaptive(source.isAdaptive());
        return copy;
    }

    private static ReverseProxyProperties.Shedding copyShedding(ReverseProxyProperties.Shedding source) {
        ReverseProxyProperties.Shedding copy = new ReverseProxyProperties.Shedding();
        if (source == null) {
            return copy;
        }
        copy.setEnabled(source.isEnabled());
        copy.setSoftUtilizationThreshold(source.getSoftUtilizationThreshold());
        copy.setHardUtilizationThreshold(source.getHardUtilizationThreshold());
        copy.setMaxQueueDepth(source.getMaxQueueDepth());
        copy.setMaxP95LatencyMillis(source.getMaxP95LatencyMillis());
        copy.setMaxErrorRate(source.getMaxErrorRate());
        copy.setCriticalBypassEnabled(source.isCriticalBypassEnabled());
        copy.setShedUserOnHardPressure(source.isShedUserOnHardPressure());
        copy.setPriorityHeader(source.getPriorityHeader());
        copy.setRetryAfter(source.getRetryAfter());
        return copy;
    }

    private static ReverseProxyProperties.PrivateNetworkValidation copyPrivateNetworkValidation(
            ReverseProxyProperties.PrivateNetworkValidation source) {
        ReverseProxyProperties.PrivateNetworkValidation copy =
                new ReverseProxyProperties.PrivateNetworkValidation();
        if (source == null) {
            return copy;
        }
        copy.setEnabled(source.isEnabled());
        return copy;
    }

    private static ReverseProxyProperties.PrivateNetworkLiveValidation copyPrivateNetworkLiveValidation(
            ReverseProxyProperties.PrivateNetworkLiveValidation source) {
        ReverseProxyProperties.PrivateNetworkLiveValidation copy =
                new ReverseProxyProperties.PrivateNetworkLiveValidation();
        if (source == null) {
            return copy;
        }
        copy.setEnabled(source.isEnabled());
        copy.setOperatorApproved(source.isOperatorApproved());
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
        copy.setHealthyThreshold(source.getHealthyThreshold());
        copy.setUnhealthyThreshold(source.getUnhealthyThreshold());
        return copy;
    }

    private static ReverseProxyProperties.Retry copyRetry(ReverseProxyProperties.Retry source) {
        ReverseProxyProperties.Retry copy = new ReverseProxyProperties.Retry();
        if (source == null) {
            return copy;
        }
        copy.setEnabled(source.isEnabled());
        copy.setMaxAttempts(source.getMaxAttempts());
        copy.setBudgetPercent(source.getBudgetPercent());
        ReverseProxyProperties.Backoff backoff = new ReverseProxyProperties.Backoff();
        backoff.setBase(source.getBackoff().getBase());
        backoff.setMax(source.getBackoff().getMax());
        copy.setBackoff(backoff);
        copy.setRetryNonIdempotent(source.isRetryNonIdempotent());
        copy.setMethods(source.getMethods());
        copy.setRetryStatuses(source.getRetryStatuses());
        return copy;
    }

    private static ReverseProxyProperties.SlowStart copySlowStart(ReverseProxyProperties.SlowStart source) {
        ReverseProxyProperties.SlowStart copy = new ReverseProxyProperties.SlowStart();
        if (source != null) {
            copy.setDuration(source.getDuration());
        }
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
            ProxyRequestHeaders.ForwardedPolicy forwardedPolicy,
            ProxyAdmissionControl.Policy admissionPolicy,
            ProxyRetryPolicy retryPolicy,
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

    private static int saturatedAdd(long left, long right) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, left) + Math.max(0L, right));
    }

    private static int saturatedAdd(int left, int right) {
        return saturatedAdd((long) left, right);
    }

    private record UpstreamCandidate(
            URI baseUri,
            ServerStateVector state,
            int maxInFlight,
            int telemetrySampleSize) {
    }

    private record EffectiveHealth(
            boolean healthy,
            String source,
            Integer lastProbeStatusCode,
            String lastProbeOutcome) {
    }

    private record ForwardAttemptResult(
            ReverseProxyResponse response, boolean retriable, boolean affinityEligible, String outcome) {
    }

    static final class ResilienceState {
        private int consecutiveFailures;
        private Instant cooldownUntil;
        private Instant slowStartStartedAt;

        ResilienceState(Instant firstEligibleAt) {
            this.slowStartStartedAt = Objects.requireNonNull(
                    firstEligibleAt, "firstEligibleAt cannot be null");
        }

        synchronized boolean recordFailure(Instant now, ReverseProxyProperties.Cooldown cooldown) {
            if (cooldown.isEnabled()) {
                recoverExpiredCooldown(now);
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

        synchronized void recordSuccess(Instant now) {
            Objects.requireNonNull(now, "now cannot be null");
            boolean recoveredEarly = cooldownUntil != null && cooldownUntil.isAfter(now);
            recoverExpiredCooldown(now);
            if (recoveredEarly) {
                slowStartStartedAt = now;
            }
            consecutiveFailures = 0;
            cooldownUntil = null;
        }

        private synchronized void recoverExpiredCooldown(Instant now) {
            if (cooldownUntil != null && !cooldownUntil.isAfter(now)) {
                Instant recoveredAt = cooldownUntil;
                cooldownUntil = null;
                consecutiveFailures = consecutiveFailures == 0
                        ? 0
                        : Math.max(1, consecutiveFailures / 2);
                slowStartStartedAt = recoveredAt;
            }
        }

        synchronized boolean cooldownActive(Instant now) {
            recoverExpiredCooldown(now);
            return cooldownUntil != null && cooldownUntil.isAfter(now);
        }

        synchronized long cooldownRemainingMillis(Instant now) {
            if (!cooldownActive(now)) {
                return 0;
            }
            return Math.max(0, Duration.between(now, cooldownUntil).toMillis());
        }

        synchronized int consecutiveFailures(Instant now) {
            recoverExpiredCooldown(now);
            return consecutiveFailures;
        }

        synchronized double effectiveWeight(double configuredWeight, Instant now, Duration slowStartDuration) {
            return slowStartSnapshot(configuredWeight, now, slowStartDuration).effectiveWeight();
        }

        synchronized SlowStartSnapshot slowStartSnapshot(
                double configuredWeight, Instant now, Duration slowStartDuration) {
            recoverExpiredCooldown(now);
            Duration duration = slowStartDuration == null ? Duration.ZERO : slowStartDuration;
            if (configuredWeight <= 0.0 || duration.isZero() || duration.isNegative()
                    || slowStartStartedAt == null) {
                return SlowStartSnapshot.full(configuredWeight);
            }
            Duration elapsed = Duration.between(slowStartStartedAt, now);
            if (elapsed.isNegative() || elapsed.isZero()) {
                return new SlowStartSnapshot(0.0, true, duration.toMillis());
            }
            if (elapsed.compareTo(duration) >= 0) {
                slowStartStartedAt = null;
                return SlowStartSnapshot.full(configuredWeight);
            }
            double fraction = elapsed.toNanos() / (double) duration.toNanos();
            long remainingMillis = Math.max(0, duration.minus(elapsed).toMillis());
            return new SlowStartSnapshot(configuredWeight * fraction, true, remainingMillis);
        }

        record SlowStartSnapshot(
                double effectiveWeight, boolean active, long remainingMillis) {
            static SlowStartSnapshot full(double configuredWeight) {
                return new SlowStartSnapshot(configuredWeight, false, 0);
            }
        }
    }
}
