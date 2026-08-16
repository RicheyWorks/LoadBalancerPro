package com.richmond423.loadbalancerpro.api.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.richmond423.loadbalancerpro.core.NetworkAwarenessSignal;
import com.richmond423.loadbalancerpro.core.RoutingDecision;
import com.richmond423.loadbalancerpro.core.RoutingDecisionExplanation;
import com.richmond423.loadbalancerpro.core.RoutingStrategy;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import com.richmond423.loadbalancerpro.core.ServerStateVector;
import com.richmond423.loadbalancerpro.core.StatefulRoutingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
@ConditionalOnProperty(prefix = "loadbalancerpro.proxy", name = "enabled", havingValue = "true")
public class ReverseProxyService implements SmartLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(ReverseProxyService.class);
    private static final String PROXY_PREFIX = "/proxy";
    private static final String UPSTREAM_HEADER = "X-LoadBalancerPro-Upstream";
    private static final String STRATEGY_HEADER = "X-LoadBalancerPro-Strategy";
    private static final int RESPONSE_COPY_BUFFER_BYTES = 16 * 1024;
    private static final Duration MAXIMUM_SLOW_START_DURATION = Duration.ofHours(24);
    private static final Duration MAXIMUM_DRAIN_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration MAXIMUM_WEBSOCKET_CONNECT_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration MAXIMUM_WEBSOCKET_IDLE_TIMEOUT = Duration.ofHours(24);
    private static final Duration MAXIMUM_WEBSOCKET_SEND_TIMEOUT = Duration.ofMinutes(5);
    private static final long DRAIN_SWEEP_INTERVAL_MILLIS = 25;
    static final String DRAIN_THREAD_NAME = "loadbalancerpro-reload-drain";
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
    private final ReverseProxyHttpClientProvider httpClientProvider;
    private final ReverseProxyMetrics metrics;
    private final ReverseProxyAccessLog accessLog;
    private final RoutingStrategyRegistry registry;
    private final Clock clock;
    private final AtomicReference<ActiveProxyConfig> activeConfig;
    private final AtomicReference<ReloadState> reloadState;
    private final AtomicLong nextGeneration = new AtomicLong(1);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean drainSweepScheduled = new AtomicBoolean();
    private final Object configurationLock = new Object();
    private final ConcurrentMap<String, ResilienceState> resilienceStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UpstreamRuntimeStats> upstreamRuntimeStats = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DrainingUpstream> drainingUpstreams = new ConcurrentHashMap<>();
    private final UpstreamRuntimeStats globalRuntimeStats;
    private final LiveRoutingDecisionStore liveRoutingDecisions;
    private final LiveRoutingObservationSink liveObservationSink;
    private final ProxyDnsDiscoveryRuntime.Resolver dnsResolver;
    private final UpstreamHealthProber healthProber;
    private final ScheduledExecutorService drainScheduler;
    private volatile ProxyDnsDiscoveryRuntime dnsDiscoveryRuntime;

    @Autowired
    public ReverseProxyService(ReverseProxyProperties properties,
                               HttpClient httpClient,
                               ReverseProxyHttpClientProvider httpClientProvider,
                               ReverseProxyMetrics metrics,
                               ReverseProxyAccessLog accessLog,
                               ObjectProvider<LiveRoutingObservationSink> liveObservationSinkProvider,
                               ObjectProvider<RoutingStrategyRegistry> routingStrategyRegistryProvider) {
        this(properties, httpClient, httpClientProvider, metrics, accessLog,
                routingStrategyRegistryProvider.getIfAvailable(RoutingStrategyRegistry::defaultRegistry),
                Clock.systemUTC(),
                liveObservationSinkProvider.getIfAvailable(LiveRoutingObservationSink::disabled),
                ProxyDnsDiscoveryRuntime.Resolver.system());
    }

    ReverseProxyService(ReverseProxyProperties properties,
                        HttpClient httpClient,
                        ReverseProxyMetrics metrics,
                        RoutingStrategyRegistry registry,
                        Clock clock) {
        this(properties, httpClient, metrics, registry, clock, LiveRoutingObservationSink.disabled());
    }

    ReverseProxyService(ReverseProxyProperties properties,
                        HttpClient httpClient,
                        ReverseProxyMetrics metrics,
                        RoutingStrategyRegistry registry,
                        Clock clock,
                        LiveRoutingObservationSink liveObservationSink) {
        this(properties, httpClient,
                ReverseProxyHttpClientProvider.systemDefault(httpClient, properties.getConnectTimeout()),
                metrics, ReverseProxyAccessLog.disabled(), registry, clock, liveObservationSink,
                ProxyDnsDiscoveryRuntime.Resolver.system());
    }

    ReverseProxyService(ReverseProxyProperties properties,
                        HttpClient httpClient,
                        ReverseProxyMetrics metrics,
                        RoutingStrategyRegistry registry,
                        Clock clock,
                        LiveRoutingObservationSink liveObservationSink,
                        ReverseProxyAccessLog accessLog) {
        this(properties, httpClient,
                ReverseProxyHttpClientProvider.systemDefault(httpClient, properties.getConnectTimeout()),
                metrics, accessLog, registry, clock, liveObservationSink,
                ProxyDnsDiscoveryRuntime.Resolver.system());
    }

    ReverseProxyService(ReverseProxyProperties properties,
                        HttpClient httpClient,
                        ReverseProxyMetrics metrics,
                        RoutingStrategyRegistry registry,
                        Clock clock,
                        LiveRoutingObservationSink liveObservationSink,
                        ReverseProxyAccessLog accessLog,
                        ProxyDnsDiscoveryRuntime.Resolver dnsResolver) {
        this(properties, httpClient,
                ReverseProxyHttpClientProvider.systemDefault(httpClient, properties.getConnectTimeout()),
                metrics, accessLog, registry, clock, liveObservationSink, dnsResolver);
    }

    private ReverseProxyService(ReverseProxyProperties properties,
                                 HttpClient httpClient,
                                 ReverseProxyHttpClientProvider httpClientProvider,
                                 ReverseProxyMetrics metrics,
                                 ReverseProxyAccessLog accessLog,
                                 RoutingStrategyRegistry registry,
                                 Clock clock,
                                 LiveRoutingObservationSink liveObservationSink,
                                 ProxyDnsDiscoveryRuntime.Resolver dnsResolver) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.httpClientProvider = Objects.requireNonNull(httpClientProvider, "httpClientProvider cannot be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics cannot be null");
        this.accessLog = Objects.requireNonNull(accessLog, "accessLog cannot be null");
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.liveObservationSink = Objects.requireNonNull(liveObservationSink, "liveObservationSink cannot be null");
        this.dnsResolver = Objects.requireNonNull(dnsResolver, "dnsResolver cannot be null");
        this.globalRuntimeStats = new UpstreamRuntimeStats(clock);
        this.liveRoutingDecisions = new LiveRoutingDecisionStore(clock);
        ActiveProxyConfig startupConfig = buildActiveConfig(
                properties, nextGeneration.getAndIncrement(), List.of());
        prepareResilienceStates(startupConfig, Instant.now(clock));
        this.activeConfig = new AtomicReference<>(startupConfig);
        this.reloadState = new AtomicReference<>(ReloadState.notAttempted(startupConfig));
        this.healthProber = new UpstreamHealthProber(httpClient, clock, this::recordProbeOutcome);
        this.drainScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, DRAIN_THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        metrics.activateConfiguration(startupConfig.routes());
        configureHealthProber(startupConfig);
        configureDiscoveryRuntime(startupConfig);
        logStartupSummary();
    }

    @SuppressWarnings("java/ssrf")
    ReverseProxyResponse forward(HttpServletRequest request) {
        return forwardUnchecked(request, ProxyRequestBody.streaming(request));
    }

    @SuppressWarnings("java/ssrf")
    ReverseProxyResponse forward(HttpServletRequest request, byte[] requestBody) {
        return forwardUnchecked(request, ProxyRequestBody.repeatable(requestBody));
    }

    @SuppressWarnings("java/ssrf")
    ReverseProxyResponse forward(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return forward(request, ProxyRequestBody.streaming(request), new ServletProxyDownstream(response));
    }

    private ReverseProxyResponse forwardUnchecked(HttpServletRequest request, ProxyRequestBody requestBody) {
        try {
            return forward(request, requestBody, DiscardingProxyDownstream.INSTANCE);
        } catch (IOException exception) {
            throw new UncheckedIOException("Proxy response streaming failed", exception);
        }
    }

    private ReverseProxyResponse forward(
            HttpServletRequest request, ProxyRequestBody requestBody, ProxyDownstream downstream) throws IOException {
        ActiveProxyConfig config = acquireActiveConfig();
        try {
            return forward(config, request, requestBody, downstream);
        } finally {
            config.requestCompleted();
            sweepDrainingUpstreamsSafely();
        }
    }

    private ReverseProxyResponse forward(
            ActiveProxyConfig config,
            HttpServletRequest request,
            ProxyRequestBody requestBody,
            ProxyDownstream downstream) throws IOException {
        ReverseProxyProperties properties = config.properties();
        ProxyRequestObservation observation = null;
        if (properties.isEnabled()) {
            ReverseProxyMetrics.RequestObservation metricObservation = metrics.beginRequest();
            observation = new ProxyRequestObservation(
                    metricObservation,
                    accessLog.begin(request.getMethod(), metricObservation.startedAtNanos()));
        }
        ProxyDownstream observedDownstream = observation == null
                ? downstream
                : new CountingProxyDownstream(downstream, observation);
        ReverseProxyResponse terminalResponse = null;
        try {
            if (requestBody.declaredLength() > properties.getMaxRequestBytes()) {
                logger.warn("proxy.forward.failure reason=payload_too_large requestBytes={} maxRequestBytes={}",
                        requestBody.declaredLength(), properties.getMaxRequestBytes());
                metrics.recordFailure(null, HttpStatus.PAYLOAD_TOO_LARGE.value());
                if (observation != null) {
                    observation.terminal(
                            HttpStatus.PAYLOAD_TOO_LARGE.value(),
                            ReverseProxyMetrics.TerminalOutcome.REQUEST_SIZE_LIMIT);
                }
                terminalResponse = writeLocalResponse(request, observedDownstream,
                        proxyError(HttpStatus.PAYLOAD_TOO_LARGE, "proxy_payload_too_large",
                                "Proxy request body exceeds maximum size of "
                                        + properties.getMaxRequestBytes() + " bytes"));
                return terminalResponse;
            }

            String proxyPathSuffix;
            try {
                proxyPathSuffix = validatedProxyPathSuffix(request);
            } catch (IllegalArgumentException exception) {
                logger.warn("proxy.forward.failure reason=invalid_path exceptionType={}",
                        exception.getClass().getSimpleName());
                metrics.recordFailure(null, HttpStatus.BAD_REQUEST.value());
                if (observation != null) {
                    observation.terminal(
                            HttpStatus.BAD_REQUEST.value(),
                            ReverseProxyMetrics.TerminalOutcome.INVALID_PATH);
                }
                terminalResponse = writeLocalResponse(request, observedDownstream,
                        proxyError(HttpStatus.BAD_REQUEST, "proxy_path_invalid",
                                "Proxy request path is invalid."));
                return terminalResponse;
            }
            if (!properties.isEnabled()) {
                logger.warn("proxy.forward.failure reason=proxy_disabled");
                metrics.recordFailure(null, HttpStatus.SERVICE_UNAVAILABLE.value());
                return writeLocalResponse(request, observedDownstream,
                        proxyError(HttpStatus.SERVICE_UNAVAILABLE, "proxy_disabled",
                                "Proxy mode is disabled in the active configuration."));
            }
            Optional<ReverseProxyRoutePlanner.ConfiguredRoute> selectedRoute =
                    routeFor(config.routes(), request, proxyPathSuffix);
            if (selectedRoute.isEmpty()) {
                logger.warn("proxy.forward.failure reason=route_not_found pathSuffix={}", proxyPathSuffix);
                metrics.recordFailure(null, HttpStatus.NOT_FOUND.value());
                observation.terminal(
                        HttpStatus.NOT_FOUND.value(),
                        ReverseProxyMetrics.TerminalOutcome.ROUTE_NOT_FOUND);
                terminalResponse = writeLocalResponse(request, observedDownstream,
                        proxyError(HttpStatus.NOT_FOUND, "proxy_route_not_found",
                                "No configured proxy route matches the requested path."));
                return terminalResponse;
            }
            ReverseProxyRoutePlanner.ConfiguredRoute route = selectedRoute.get();
            observation.bindRoute(route.name());
            ProxyAdmissionControl.Admission admission =
                    config.admissionPolicy().tryAcquire(request, globalRuntimeStats);
            if (!admission.acquired()) {
                logger.warn("proxy.forward.rejected reason={} priority={} detail={}",
                        admission.errorCode(), admission.priority(), admission.reason());
                metrics.recordFailure(null, HttpStatus.SERVICE_UNAVAILABLE.value());
                ReverseProxyMetrics.TerminalOutcome outcome = "proxy_load_shed".equals(admission.errorCode())
                        ? ReverseProxyMetrics.TerminalOutcome.LOAD_SHED
                        : ReverseProxyMetrics.TerminalOutcome.GLOBAL_CONCURRENCY_LIMIT;
                observation.terminal(HttpStatus.SERVICE_UNAVAILABLE.value(), outcome);
                terminalResponse = writeLocalResponse(request, observedDownstream, overloadResponse(
                        admission.errorCode(), admission.message(), admission.retryAfterSeconds()));
                return terminalResponse;
            }
            long startedAtNanos = System.nanoTime();
            ReverseProxyResponse response = null;
            try {
                response = forwardAdmitted(
                        config, route, request, requestBody, observedDownstream, observation,
                        proxyPathSuffix, admission.retryAfterSeconds());
                terminalResponse = response;
                observation.terminalIfUnset(
                        response.statusCode(),
                        ReverseProxyMetrics.TerminalOutcome.fromStatus(response.statusCode()));
                return response;
            } finally {
                long elapsedNanos = Math.max(0, System.nanoTime() - startedAtNanos);
                globalRuntimeStats.requestCompleted(
                        Duration.ofNanos(elapsedNanos), response != null && response.statusCode() < 500);
                config.admissionPolicy().requestCompleted(globalRuntimeStats);
            }
        } catch (IOException | RuntimeException exception) {
            if (observation != null) {
                observation.terminalIfUnset(
                        terminalResponse == null ? 0 : terminalResponse.statusCode(),
                        ReverseProxyMetrics.TerminalOutcome.INTERNAL_ERROR);
            }
            throw exception;
        } finally {
            if (observation != null) {
                observation.complete(requestBody.consumedBytes());
            }
        }
    }

    private ActiveProxyConfig acquireActiveConfig() {
        synchronized (configurationLock) {
            ActiveProxyConfig config = activeConfig.get();
            config.requestStarted();
            return config;
        }
    }

    @SuppressWarnings("java/ssrf")
    ReverseProxyWebSocketPlan planWebSocket(HttpServletRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        ActiveProxyConfig config = acquireActiveConfig();
        ReverseProxyMetrics.RequestObservation metricObservation = metrics.beginRequest();
        ProxyRequestObservation observation = new ProxyRequestObservation(
                metricObservation,
                accessLog.begin(request.getMethod(), metricObservation.startedAtNanos()));
        long startedAtNanos = System.nanoTime();
        boolean admitted = false;
        UpstreamRuntimeStats selectedRuntimeStats = null;
        String selectedUpstreamId = null;
        try {
            ReverseProxyProperties properties = config.properties();
            if (!properties.getWebsocket().isEnabled()) {
                throw webSocketPlanningFailure(
                        HttpStatus.NOT_FOUND, "proxy_websocket_disabled", "WebSocket proxying is disabled.");
            }
            String proxyPathSuffix;
            try {
                proxyPathSuffix = validatedProxyPathSuffix(request);
            } catch (IllegalArgumentException exception) {
                throw webSocketPlanningFailure(
                        HttpStatus.BAD_REQUEST, "proxy_path_invalid", "Proxy request path is invalid.");
            }
            ReverseProxyRoutePlanner.ConfiguredRoute route = routeFor(config.routes(), request, proxyPathSuffix)
                    .orElseThrow(() -> webSocketPlanningFailure(
                            HttpStatus.NOT_FOUND,
                            "proxy_route_not_found",
                            "No configured proxy route matches the requested path."));
            observation.bindRoute(route.name());
            ProxyAdmissionControl.Admission admission =
                    config.admissionPolicy().tryAcquire(request, globalRuntimeStats);
            if (!admission.acquired()) {
                throw new ReverseProxyWebSocketPlanningException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        admission.errorCode(),
                        admission.message(),
                        admission.retryAfterSeconds());
            }
            admitted = true;

            String routingKey = route.selectionPolicy().routingKey(request);
            Optional<ReverseProxyRoutePlanner.ConfiguredSplit> selectedSplit = route.splitFor(routingKey);
            Set<String> splitTargetIds = selectedSplit
                    .map(ReverseProxyRoutePlanner.ConfiguredSplit::targetIds)
                    .orElse(null);
            RoutingStrategy selectionStrategy = selectedSplit
                    .map(ReverseProxyRoutePlanner.ConfiguredSplit::strategy)
                    .orElse(route.strategy());
            Set<String> capacityExcludedIds = new LinkedHashSet<>();

            while (true) {
                List<UpstreamCandidate> upstreams = configuredUpstreams(
                        config, route, splitTargetIds, capacityExcludedIds);
                if (upstreams.isEmpty()) {
                    String code = capacityExcludedIds.isEmpty()
                            ? "proxy_unavailable"
                            : "proxy_upstream_concurrency_limit";
                    String message = capacityExcludedIds.isEmpty()
                            ? "No healthy proxy upstreams are available."
                            : "All eligible proxy upstreams are at their in-flight limits.";
                    throw new ReverseProxyWebSocketPlanningException(
                            HttpStatus.SERVICE_UNAVAILABLE, code, message, admission.retryAfterSeconds());
                }

                List<ServerStateVector> candidateStates = upstreams.stream()
                        .map(UpstreamCandidate::state)
                        .toList();
                Optional<String> selectedServerId = route.selectionPolicy()
                        .affinityTarget(request, candidateStates);
                if (selectedServerId.isEmpty()) {
                    selectedServerId = chooseForKey(selectionStrategy, candidateStates, routingKey)
                            .explanation()
                            .chosenServerId();
                }
                if (selectedServerId.isEmpty()) {
                    throw new ReverseProxyWebSocketPlanningException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "proxy_unavailable",
                            "No healthy proxy upstreams are available.",
                            admission.retryAfterSeconds());
                }
                String upstreamId = selectedServerId.get();
                UpstreamCandidate upstream = upstreams.stream()
                        .filter(candidate -> upstreamId.equals(candidate.state().serverId()))
                        .findFirst()
                        .orElseThrow(() -> webSocketPlanningFailure(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "proxy_unavailable",
                                "Selected proxy upstream is not configured."));
                UpstreamRuntimeStats runtimeStats = runtimeStatsFor(upstreamId);
                if (!runtimeStats.tryRequestStarted(upstream.maxInFlight(), ignored -> true)) {
                    capacityExcludedIds.add(upstreamId);
                    continue;
                }
                selectedRuntimeStats = runtimeStats;
                selectedUpstreamId = upstreamId;
                observation.bindUpstream(route.name(), upstreamId);
                observation.recordDispatch(false, ReverseProxyMetrics.RetryReason.INITIAL);
                URI targetUri = webSocketTargetUri(targetUri(request, upstream, proxyPathSuffix));
                Map<String, List<String>> headers = ProxyRequestHeaders.webSocketHeaders(
                        request, config.forwardedPolicy(), route.headerRewrites());
                UpstreamRuntimeStats reservedRuntimeStats = runtimeStats;
                return new ReverseProxyWebSocketPlan(
                        route.name(),
                        upstreamId,
                        route.strategyId().externalName(),
                        targetUri,
                        upstream.httpClient(),
                        headers,
                        properties.getWebsocket().getConnectTimeout(),
                        properties.getWebsocket().getSendTimeout(),
                        properties.getWebsocket().getSendBufferBytes(),
                        (connected, successful, upstreamFailure, outcome, elapsed) -> completeWebSocket(
                                config,
                                upstreamId,
                                upstream.resilienceState(),
                                reservedRuntimeStats,
                                observation,
                                connected,
                                successful,
                                upstreamFailure,
                                outcome,
                                elapsed));
            }
        } catch (RuntimeException | Error exception) {
            if (selectedRuntimeStats != null) {
                selectedRuntimeStats.requestAborted();
            }
            if (admitted) {
                Duration elapsed = Duration.ofNanos(Math.max(0, System.nanoTime() - startedAtNanos));
                globalRuntimeStats.requestCompleted(elapsed, false);
                config.admissionPolicy().requestCompleted(globalRuntimeStats);
            }
            int statusCode = exception instanceof ReverseProxyWebSocketPlanningException planning
                    ? planning.status().value()
                    : HttpStatus.INTERNAL_SERVER_ERROR.value();
            metrics.recordFailure(
                    selectedUpstreamId,
                    statusCode);
            observation.terminal(statusCode, webSocketPlanningOutcome(exception));
            observation.complete(0);
            config.requestCompleted();
            sweepDrainingUpstreamsSafely();
            throw exception;
        }
    }

    private void completeWebSocket(
            ActiveProxyConfig config,
            String upstreamId,
            ResilienceState resilienceState,
            UpstreamRuntimeStats runtimeStats,
            ProxyRequestObservation observation,
            boolean connected,
            boolean successful,
            boolean upstreamFailure,
            ReverseProxyMetrics.TerminalOutcome outcome,
            Duration elapsed) {
        try {
            boolean healthyCompletion = !upstreamFailure;
            runtimeStats.requestCompleted(elapsed, healthyCompletion);
            globalRuntimeStats.requestCompleted(elapsed, healthyCompletion);
            if (successful) {
                metrics.recordForwarded(upstreamId, HttpStatus.SWITCHING_PROTOCOLS.value());
            } else {
                metrics.recordFailure(
                        upstreamFailure ? upstreamId : null,
                        upstreamFailure ? HttpStatus.BAD_GATEWAY.value() : 0);
            }
            if (upstreamFailure) {
                if (recordResilienceFailure(config.properties(), upstreamId, resilienceState)) {
                    metrics.recordCooldownActivation(upstreamId);
                    observation.cooldownActivated();
                }
            } else if (connected) {
                recordResilienceSuccess(upstreamId, resilienceState);
            }
            observation.terminal(
                    successful
                            ? HttpStatus.SWITCHING_PROTOCOLS.value()
                            : upstreamFailure ? HttpStatus.BAD_GATEWAY.value() : 0,
                    outcome);
            observation.complete(0);
        } finally {
            try {
                config.admissionPolicy().requestCompleted(globalRuntimeStats);
            } finally {
                config.requestCompleted();
                sweepDrainingUpstreamsSafely();
            }
        }
    }

    private static ReverseProxyWebSocketPlanningException webSocketPlanningFailure(
            HttpStatus status, String code, String message) {
        return new ReverseProxyWebSocketPlanningException(status, code, message);
    }

    private static ReverseProxyMetrics.TerminalOutcome webSocketPlanningOutcome(Throwable exception) {
        if (!(exception instanceof ReverseProxyWebSocketPlanningException planning)) {
            return ReverseProxyMetrics.TerminalOutcome.INTERNAL_ERROR;
        }
        return switch (planning.errorCode()) {
            case "proxy_path_invalid" -> ReverseProxyMetrics.TerminalOutcome.INVALID_PATH;
            case "proxy_route_not_found" -> ReverseProxyMetrics.TerminalOutcome.ROUTE_NOT_FOUND;
            case "proxy_load_shed" -> ReverseProxyMetrics.TerminalOutcome.LOAD_SHED;
            case "proxy_concurrency_limit" -> ReverseProxyMetrics.TerminalOutcome.GLOBAL_CONCURRENCY_LIMIT;
            case "proxy_upstream_concurrency_limit" ->
                    ReverseProxyMetrics.TerminalOutcome.UPSTREAM_CONCURRENCY_LIMIT;
            case "proxy_unavailable" -> ReverseProxyMetrics.TerminalOutcome.NO_UPSTREAM;
            default -> ReverseProxyMetrics.TerminalOutcome.fromStatus(planning.status().value());
        };
    }

    private ReverseProxyResponse forwardAdmitted(
            ActiveProxyConfig config,
            ReverseProxyRoutePlanner.ConfiguredRoute route,
            HttpServletRequest request,
            ProxyRequestBody requestBody,
            ProxyDownstream downstream,
            ProxyRequestObservation observation,
            String proxyPathSuffix,
            int retryAfterSeconds) throws IOException {
        ReverseProxyProperties properties = config.properties();
        int maxAttempts = requestBody.repeatable()
                ? maxAttemptsFor(request.getMethod(), properties)
                : 1;
        config.retryPolicy().recordPrimaryRequest();
        String routingKey = route.selectionPolicy().routingKey(request);
        Optional<ReverseProxyRoutePlanner.ConfiguredSplit> selectedSplit = route.splitFor(routingKey);
        Set<String> splitTargetIds = selectedSplit
                .map(ReverseProxyRoutePlanner.ConfiguredSplit::targetIds)
                .orElse(null);
        RoutingStrategy selectionStrategy = selectedSplit
                .map(ReverseProxyRoutePlanner.ConfiguredSplit::strategy)
                .orElse(route.strategy());
        Set<String> attemptedUpstreamIds = new LinkedHashSet<>();
        ForwardAttemptResult pendingAttempt = null;
        try {
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                Set<String> capacityExcludedIds = new LinkedHashSet<>();
                while (true) {
                    Set<String> excludedIds = new LinkedHashSet<>(attemptedUpstreamIds);
                    excludedIds.addAll(capacityExcludedIds);
                    List<UpstreamCandidate> upstreams = configuredUpstreams(
                            config, route, splitTargetIds, excludedIds);
                    if (upstreams.isEmpty()) {
                        if (pendingAttempt != null) {
                            ForwardAttemptResult finalAttempt = pendingAttempt;
                            pendingAttempt = null;
                            return deliverFinalResponse(
                                    request, downstream, route, finalAttempt, observation);
                        }
                        if (!capacityExcludedIds.isEmpty()) {
                            logger.warn(
                                    "proxy.forward.rejected reason=upstream_concurrency_limit route={} targets={}",
                                    route.name(), capacityExcludedIds);
                            metrics.recordFailure(null, HttpStatus.SERVICE_UNAVAILABLE.value());
                            return writeTerminalLocalResponse(request, downstream, observation,
                                    ReverseProxyMetrics.TerminalOutcome.UPSTREAM_CONCURRENCY_LIMIT, overloadResponse(
                                    "proxy_upstream_concurrency_limit",
                                    "All eligible proxy upstreams are at their in-flight limits.",
                                    retryAfterSeconds));
                        }
                        logger.warn("proxy.forward.failure reason=no_configured_upstreams route={} attempt={}",
                                route.name(), attempt);
                        metrics.recordFailure(null, HttpStatus.SERVICE_UNAVAILABLE.value());
                        return writeTerminalLocalResponse(request, downstream, observation,
                                ReverseProxyMetrics.TerminalOutcome.NO_UPSTREAM,
                                proxyError(HttpStatus.SERVICE_UNAVAILABLE, "proxy_unavailable",
                                        "No proxy upstreams are configured for route " + route.name() + "."));
                    }

                    List<ServerStateVector> candidateStates = upstreams.stream()
                            .map(UpstreamCandidate::state)
                            .toList();
                    Optional<String> selectedServerId = route.selectionPolicy()
                            .affinityTarget(request, candidateStates);
                    String selectionSource = selectedServerId.isPresent() ? "affinity" : "strategy";
                    RoutingDecisionExplanation selectionExplanation = null;
                    if (selectedServerId.isEmpty()) {
                        RoutingDecision decision = chooseForKey(
                                selectionStrategy, candidateStates, routingKey);
                        selectionExplanation = decision.explanation();
                        selectedServerId = selectionExplanation.chosenServerId();
                    }
                    if (selectedServerId.isEmpty()) {
                        if (pendingAttempt != null) {
                            ForwardAttemptResult finalAttempt = pendingAttempt;
                            pendingAttempt = null;
                            return deliverFinalResponse(request, downstream, route, finalAttempt, observation);
                        }
                        logger.warn("proxy.forward.failure reason=no_healthy_upstreams route={} attempt={}",
                                route.name(), attempt);
                        metrics.recordFailure(null, HttpStatus.SERVICE_UNAVAILABLE.value());
                        return writeTerminalLocalResponse(request, downstream, observation,
                                ReverseProxyMetrics.TerminalOutcome.NO_UPSTREAM,
                                proxyError(HttpStatus.SERVICE_UNAVAILABLE, "proxy_unavailable",
                                        "No healthy proxy upstreams are available."));
                    }

                    Map<String, UpstreamCandidate> upstreamById = upstreams.stream()
                            .collect(Collectors.toMap(
                                    candidate -> candidate.state().serverId(), Function.identity()));
                    UpstreamCandidate upstream = upstreamById.get(selectedServerId.get());
                    if (upstream == null) {
                        if (pendingAttempt != null) {
                            ForwardAttemptResult finalAttempt = pendingAttempt;
                            pendingAttempt = null;
                            return deliverFinalResponse(request, downstream, route, finalAttempt, observation);
                        }
                        logger.warn(
                                "proxy.forward.failure reason=selected_upstream_not_configured route={} upstreamId={}",
                                route.name(), selectedServerId.get());
                        metrics.recordFailure(selectedServerId.get(), HttpStatus.SERVICE_UNAVAILABLE.value());
                        return writeTerminalLocalResponse(request, downstream, observation,
                                ReverseProxyMetrics.TerminalOutcome.NO_UPSTREAM,
                                proxyError(HttpStatus.SERVICE_UNAVAILABLE, "proxy_unavailable",
                                        "Selected proxy upstream is not configured: " + selectedServerId.get()));
                    }

                    String upstreamId = upstream.state().serverId();
                    UpstreamRuntimeStats runtimeStats = runtimeStatsFor(upstreamId);
                    if (!runtimeStats.tryRequestStarted(upstream.maxInFlight(), ignored -> true)) {
                        capacityExcludedIds.add(upstreamId);
                        continue;
                    }
                    ReverseProxyMetrics.RetryReason dispatchReason = pendingAttempt == null
                            ? ReverseProxyMetrics.RetryReason.INITIAL
                            : pendingAttempt.retryReason();
                    if (pendingAttempt != null) {
                        pendingAttempt.discardForRetry();
                        pendingAttempt = null;
                    }
                    attemptedUpstreamIds.add(upstreamId);
                    observation.bindUpstream(route.name(), upstreamId);
                    if (attempt > 1) {
                        logger.info("proxy.forward.retry route={} attempt={} upstreamId={}",
                                route.name(), attempt, upstreamId);
                    }
                    long attemptStartedAtNanos = System.nanoTime();
                    ForwardAttemptResult attemptResult;
                    try {
                        attemptResult = forwardOnce(
                                properties, config.forwardedPolicy(), route.headerRewrites(), request, requestBody,
                                upstream, runtimeStats, route.strategyId().externalName(),
                                route.requestTimeout(), proxyPathSuffix, observation,
                                attempt > 1, dispatchReason);
                    } catch (RuntimeException | Error exception) {
                        runtimeStats.requestAborted();
                        throw exception;
                    }
                    if (!attemptResult.retriable()) {
                        attemptResult.prepareForDelivery(request);
                    }
                    if (attemptResult.retriable() && attempt < maxAttempts
                            && config.retryPolicy().tryAcquireRetry()) {
                        recordAttemptDecision(
                                config, route, attempt, selectionSource, upstreamId, upstreams,
                                candidateStates, selectionExplanation, attemptStartedAtNanos, attemptResult);
                        pendingAttempt = attemptResult;
                        try {
                            config.retryPolicy().pauseBeforeRetry(attempt);
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            logger.warn(
                                    "proxy.forward.retry_suppressed route={} attempt={} reason=backoff_interrupted",
                                    route.name(), attempt + 1);
                            ForwardAttemptResult finalAttempt = pendingAttempt;
                            pendingAttempt = null;
                            return deliverFinalResponse(request, downstream, route, finalAttempt, observation);
                        }
                        break;
                    }
                    if (attemptResult.retriable() && attempt < maxAttempts) {
                        logger.warn(
                                "proxy.forward.retry_suppressed route={} attempt={} reason=retry_budget_exhausted",
                                route.name(), attempt + 1);
                    }
                    attemptResult.prepareForDelivery(request);
                    try {
                        return deliverFinalResponse(request, downstream, route, attemptResult, observation);
                    } finally {
                        recordAttemptDecision(
                                config, route, attempt, selectionSource, upstreamId, upstreams,
                                candidateStates, selectionExplanation, attemptStartedAtNanos, attemptResult);
                    }
                }
            }
            ReverseProxyResponse unavailable = proxyError(
                    HttpStatus.SERVICE_UNAVAILABLE, "proxy_unavailable",
                    "No healthy proxy upstreams are available.");
            return writeTerminalLocalResponse(request, downstream, observation,
                    ReverseProxyMetrics.TerminalOutcome.NO_UPSTREAM, unavailable);
        } finally {
            if (pendingAttempt != null) {
                pendingAttempt.abortWithoutHealthPenalty();
            }
        }
    }

    private static RoutingDecision chooseForKey(
            RoutingStrategy strategy,
            List<ServerStateVector> currentCandidates,
            String routingKey) {
        if (strategy instanceof StatefulRoutingStrategy statefulStrategy) {
            synchronized (statefulStrategy) {
                statefulStrategy.onServerStates(currentCandidates);
                return statefulStrategy.chooseForKey(routingKey);
            }
        }
        return strategy.chooseForKey(currentCandidates, routingKey);
    }

    private void recordAttemptDecision(
            ActiveProxyConfig config,
            ReverseProxyRoutePlanner.ConfiguredRoute route,
            int attempt,
            String selectionSource,
            String upstreamId,
            List<UpstreamCandidate> upstreams,
            List<ServerStateVector> candidateStates,
            RoutingDecisionExplanation selectionExplanation,
            long attemptStartedAtNanos,
            ForwardAttemptResult attemptResult) {
        if (!attemptResult.markDecisionRecorded()) {
            return;
        }
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
                selectionExplanation,
                attemptResult.response().statusCode(),
                attemptLatencyMillis,
                attemptResult.retriable(),
                attemptResult.outcome());
        if (liveObservationSink.isEnabled()) {
            int telemetrySampleSize = upstreams.stream()
                    .mapToInt(UpstreamCandidate::telemetrySampleSize)
                    .reduce(0, ReverseProxyService::saturatedAdd);
            int initialConcurrencyLimit = liveShadowConcurrencyLimit(config, upstreams, candidateStates);
            liveObservationSink.submit(new LiveRoutingObservationSink.Observation(
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
    }

    private ReverseProxyResponse deliverFinalResponse(
            HttpServletRequest request,
            ProxyDownstream downstream,
            ReverseProxyRoutePlanner.ConfiguredRoute route,
            ForwardAttemptResult attemptResult,
            ProxyRequestObservation observation) throws IOException {
        try {
            attemptResult.prepareForDelivery(request);
            ReverseProxyResponse response = route.selectionPolicy().applyAffinityResponse(
                    attemptResult.response(), request, attemptResult.upstreamId(),
                    attemptResult.affinityEligible());
            attemptResult.response(response);
            if (!attemptResult.hasUpstreamBody()) {
                return writeLocalResponse(request, downstream, response);
            }

            applyResponseMetadata(downstream, response);
            if (!attemptResult.bodyAllowed() || attemptResult.prefetchedBytes() < 0) {
                attemptResult.completeHttpResponse();
                return response;
            }

            OutputStream output;
            try {
                output = downstream.outputStream();
            } catch (IOException exception) {
                attemptResult.downstreamDisconnected();
                throw exception;
            }
            boolean eventStream = isEventStream(response.headers());
            long streamedBytes = 0;
            try {
                output.write(attemptResult.copyBuffer(), 0, attemptResult.prefetchedBytes());
                streamedBytes = attemptResult.prefetchedBytes();
                output.flush();
            } catch (IOException exception) {
                attemptResult.downstreamDisconnected();
                throw exception;
            }

            while (true) {
                int read;
                try {
                    read = attemptResult.readNextChunk();
                } catch (IOException | RuntimeException exception) {
                    attemptResult.upstreamStreamFailed(false);
                    throw new UpstreamResponseStreamingException(
                            "Upstream response failed after downstream commitment", exception);
                }
                if (read < 0) {
                    break;
                }
                if (exceedsResponseLimit(
                        attemptResult.properties().getMaxResponseBytes(), streamedBytes, read)) {
                    attemptResult.responseLimitExceeded(false);
                    throw new UpstreamResponseStreamingException(
                            "Upstream response exceeded the configured streamed response limit");
                }
                try {
                    output.write(attemptResult.copyBuffer(), 0, read);
                    streamedBytes += read;
                    if (eventStream) {
                        output.flush();
                    }
                } catch (IOException exception) {
                    attemptResult.downstreamDisconnected();
                    throw exception;
                }
            }
            try {
                output.flush();
            } catch (IOException exception) {
                attemptResult.downstreamDisconnected();
                throw exception;
            }
            attemptResult.completeHttpResponse();
            return response;
        } catch (IOException | RuntimeException exception) {
            attemptResult.abortWithoutHealthPenalty();
            throw exception;
        } finally {
            observation.terminal(
                    attemptResult.response().statusCode(), attemptResult.terminalOutcome());
        }
    }

    private static ReverseProxyResponse writeTerminalLocalResponse(
            HttpServletRequest request,
            ProxyDownstream downstream,
            ProxyRequestObservation observation,
            ReverseProxyMetrics.TerminalOutcome outcome,
            ReverseProxyResponse response) throws IOException {
        observation.terminal(response.statusCode(), outcome);
        return writeLocalResponse(request, downstream, response);
    }

    private static ReverseProxyResponse writeLocalResponse(
            HttpServletRequest request,
            ProxyDownstream downstream,
            ReverseProxyResponse response) throws IOException {
        applyResponseMetadata(downstream, response);
        if (responsePermitsBody(request, response.statusCode()) && response.body().length > 0) {
            OutputStream output = downstream.outputStream();
            output.write(response.body());
            output.flush();
        }
        return response;
    }

    private static void applyResponseMetadata(
            ProxyDownstream downstream, ReverseProxyResponse response) {
        downstream.status(response.statusCode());
        response.headers().forEach((name, values) ->
                values.forEach(value -> downstream.header(name, value)));
    }

    private static boolean responsePermitsBody(HttpServletRequest request, int statusCode) {
        return !"HEAD".equalsIgnoreCase(request.getMethod())
                && statusCode >= 200
                && statusCode != HttpStatus.NO_CONTENT.value()
                && statusCode != HttpStatus.NOT_MODIFIED.value();
    }

    private static boolean isEventStream(HttpHeaders headers) {
        String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        return contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    private static boolean exceedsResponseLimit(long maximumBytes, long streamedBytes, int nextBytes) {
        return maximumBytes > 0 && nextBytes > maximumBytes - streamedBytes;
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
                        route.match().host(),
                        route.match().headerNames(),
                        route.splits().stream()
                                .map(split -> new ReverseProxyStatusResponse.SplitStatus(
                                        split.name(), split.percentage(),
                                        split.targetIds().stream().sorted().toList()))
                                .toList(),
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
                dnsDiscoveryStatuses(config),
                metricsSnapshot,
                ReverseProxyStatusSummaries.observability(properties.isEnabled(), routeStatuses, upstreamStatuses,
                        metricsSnapshot),
                ReverseProxyStatusSummaries.controllerNotAvailableSecurityBoundary(),
                PrivateNetworkLiveValidationStatusResponse.from(properties),
                reloadStatusSnapshot(config));
    }

    private List<ReverseProxyStatusResponse.DnsDiscoveryStatus> dnsDiscoveryStatuses(
            ActiveProxyConfig config) {
        ProxyDnsDiscoveryRuntime.Snapshot snapshot = config.discoverySnapshot();
        ProxyDnsDiscoveryRuntime runtime = dnsDiscoveryRuntime;
        if (runtime != null && runtime.snapshot().generation() == config.generation()) {
            snapshot = runtime.snapshot();
        }
        ProxyDnsDiscoveryRuntime.Snapshot current = snapshot;
        return current.statusByLogicalId().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String logicalId = entry.getKey();
                    ProxyDnsDiscoveryRuntime.Status status = entry.getValue();
                    List<ReverseProxyStatusResponse.DnsMemberStatus> members = current.membersByLogicalId()
                            .getOrDefault(logicalId, List.of()).stream()
                            .map(member -> new ReverseProxyStatusResponse.DnsMemberStatus(
                                    member.id(), member.address()))
                            .toList();
                    return new ReverseProxyStatusResponse.DnsDiscoveryStatus(
                            logicalId,
                            status.name(),
                            status.port(),
                            status.authorityMode(),
                            status.outcome().name(),
                            status.lookupInFlight(),
                            status.memberCount(),
                            status.lastSuccessAgeMillis(),
                            status.staleRemainingMillis(),
                            members);
                })
                .toList();
    }

    RecentProxyDecisionsResponse recentDecisionsSnapshot() {
        return liveRoutingDecisions.snapshot(true);
    }

    Optional<LiveRoutingDecisionRecord> retainedDecision(String decisionId) {
        return liveRoutingDecisions.find(decisionId);
    }

    PrivateNetworkLiveValidationCommandResponse privateNetworkLiveValidationCommand(
            PrivateNetworkLiveValidationCommandRequest request) {
        return PrivateNetworkLiveValidationCommandResponse.from(activeConfig.get().properties(), request);
    }

    ReverseProxyAdminConfigResponse adminConfigSnapshot() {
        return adminConfigResponse(activeConfig.get());
    }

    synchronized ReverseProxyAdminMutationResponse addUpstream(ReverseProxyUpstreamAddRequest request) {
        String action = "add";
        ActiveProxyConfig current = activeConfig.get();
        if (request == null) {
            return adminRejected("invalid", action, "", current,
                    "request body is required");
        }
        ReverseProxyAdminMutationResponse blocked = mutationPrecondition(
                request.expectedGeneration(), action, request.id(), current);
        if (blocked != null) {
            return blocked;
        }
        try {
            String upstreamId = ReverseProxyRoutePlanner.validateUpstreamId(
                    request.id(), "upstream id");
            String routeName = ReverseProxyRoutePlanner.validateRouteName(request.route());
            ReverseProxyProperties candidate = copyProperties(current.properties());
            if (configuredTargets(candidate).stream()
                    .anyMatch(target -> upstreamId.equals(target.getId().trim()))) {
                return adminRejected("invalid", action, upstreamId, current,
                        "upstream id is already configured");
            }

            ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
            upstream.setId(upstreamId);
            upstream.setUrl(request.url());
            upstream.setHealthy(request.healthy() == null || request.healthy());
            upstream.setWeight(request.weight() == null ? 1.0 : request.weight());
            upstream.setMaxInFlight(request.maxInFlight() == null ? 0 : request.maxInFlight());
            addTarget(candidate, routeName, upstream);
            return applyAdminMutation(action, upstreamId, candidate);
        } catch (RuntimeException exception) {
            return adminRejected("invalid", action, safeAdminUpstreamId(request.id()), current,
                    safeValidationError(exception));
        }
    }

    synchronized ReverseProxyAdminMutationResponse patchUpstream(
            String requestedUpstreamId,
            ReverseProxyUpstreamPatchRequest request) {
        String action = "patch";
        ActiveProxyConfig current = activeConfig.get();
        if (request == null) {
            return adminRejected("invalid", action, safeAdminUpstreamId(requestedUpstreamId), current,
                    "request body is required");
        }
        ReverseProxyAdminMutationResponse blocked = mutationPrecondition(
                request.expectedGeneration(), action, requestedUpstreamId, current);
        if (blocked != null) {
            return blocked;
        }
        try {
            String upstreamId = ReverseProxyRoutePlanner.validateUpstreamId(
                    requestedUpstreamId, "upstream id");
            if (request.weight() == null && request.healthy() == null && request.drain() == null) {
                return adminRejected("invalid", action, upstreamId, current,
                        "patch must include weight, healthy, or drain");
            }
            ReverseProxyProperties candidate = copyProperties(current.properties());
            List<ReverseProxyProperties.Upstream> matches = configuredTargets(candidate).stream()
                    .filter(target -> upstreamId.equals(target.getId().trim()))
                    .toList();
            if (matches.isEmpty()) {
                return adminRejected("not_found", action, upstreamId, current,
                        "upstream id is not configured");
            }
            if (matches.size() > 1) {
                return adminRejected("invalid", action, upstreamId, current,
                        "upstream id is ambiguous across configured routes");
            }

            ReverseProxyProperties.Upstream target = matches.get(0);
            double nextWeight = target.getWeight();
            if (request.weight() != null) {
                if (!Double.isFinite(request.weight()) || request.weight() < 0.0) {
                    return adminRejected("invalid", action, upstreamId, current,
                            "weight must be finite and non-negative");
                }
                nextWeight = request.weight();
            }
            if (Boolean.TRUE.equals(request.drain())) {
                if (request.weight() != null && Double.compare(request.weight(), 0.0) != 0) {
                    return adminRejected("invalid", action, upstreamId, current,
                            "drain=true requires weight to be omitted or zero");
                }
                nextWeight = 0.0;
            } else if (Boolean.FALSE.equals(request.drain())) {
                if (request.weight() != null && Double.compare(request.weight(), 0.0) == 0) {
                    return adminRejected("invalid", action, upstreamId, current,
                            "drain=false requires a positive weight");
                }
                if (request.weight() == null && Double.compare(nextWeight, 0.0) == 0) {
                    nextWeight = 1.0;
                }
            }
            boolean nextHealthy = request.healthy() == null ? target.isHealthy() : request.healthy();
            if (Double.compare(nextWeight, target.getWeight()) == 0
                    && nextHealthy == target.isHealthy()) {
                return adminRejected("invalid", action, upstreamId, current,
                        "patch does not change upstream state");
            }
            target.setWeight(nextWeight);
            target.setHealthy(nextHealthy);
            return applyAdminMutation(action, upstreamId, candidate);
        } catch (RuntimeException exception) {
            return adminRejected("invalid", action, safeAdminUpstreamId(requestedUpstreamId), current,
                    safeValidationError(exception));
        }
    }

    synchronized ReverseProxyAdminMutationResponse deleteUpstream(
            String requestedUpstreamId,
            Long expectedGeneration) {
        String action = "delete";
        ActiveProxyConfig current = activeConfig.get();
        ReverseProxyAdminMutationResponse blocked = mutationPrecondition(
                expectedGeneration, action, requestedUpstreamId, current);
        if (blocked != null) {
            return blocked;
        }
        try {
            String upstreamId = ReverseProxyRoutePlanner.validateUpstreamId(
                    requestedUpstreamId, "upstream id");
            ReverseProxyProperties candidate = copyProperties(current.properties());
            long matches = configuredTargets(candidate).stream()
                    .filter(target -> upstreamId.equals(target.getId().trim()))
                    .count();
            if (matches == 0) {
                return adminRejected("not_found", action, upstreamId, current,
                        "upstream id is not configured");
            }
            if (matches > 1) {
                return adminRejected("invalid", action, upstreamId, current,
                        "upstream id is ambiguous across configured routes");
            }
            removeTarget(candidate, upstreamId);
            return applyAdminMutation(action, upstreamId, candidate);
        } catch (RuntimeException exception) {
            return adminRejected("invalid", action, safeAdminUpstreamId(requestedUpstreamId), current,
                    safeValidationError(exception));
        }
    }

    private ReverseProxyAdminMutationResponse mutationPrecondition(
            Long expectedGeneration,
            String action,
            String requestedUpstreamId,
            ActiveProxyConfig current) {
        String upstreamId = safeAdminUpstreamId(requestedUpstreamId);
        if (!running.get()) {
            return adminRejected("unavailable", action, upstreamId, current,
                    "proxy runtime is stopping and cannot mutate configuration");
        }
        if (expectedGeneration == null || expectedGeneration < 1) {
            return adminRejected("invalid", action, upstreamId, current,
                    "expectedGeneration must be a positive generation");
        }
        if (expectedGeneration != current.generation()) {
            return adminRejected("generation_conflict", action, upstreamId, current,
                    "expectedGeneration does not match the active generation");
        }
        return null;
    }

    private ReverseProxyAdminMutationResponse applyAdminMutation(
            String action,
            String upstreamId,
            ReverseProxyProperties candidate) {
        ReverseProxyReloadResponse reloadResponse = reload(candidate);
        ActiveProxyConfig active = activeConfig.get();
        if (!reloadResponse.success()) {
            return ReverseProxyAdminMutationResponse.rejected(
                    "invalid", action, upstreamId, adminConfigResponse(active),
                    reloadResponse.validationErrors());
        }
        logger.info("proxy.admin.audit action={} upstreamId={} generation={} status=success",
                action, upstreamId, active.generation());
        return ReverseProxyAdminMutationResponse.success(
                action, upstreamId, adminConfigResponse(active));
    }

    private ReverseProxyAdminMutationResponse adminRejected(
            String status,
            String action,
            String upstreamId,
            ActiveProxyConfig current,
            String error) {
        return ReverseProxyAdminMutationResponse.rejected(
                status, action, upstreamId, adminConfigResponse(current), List.of(error));
    }

    private ReverseProxyAdminConfigResponse adminConfigResponse(ActiveProxyConfig config) {
        List<ReverseProxyAdminConfigResponse.RouteConfig> routes = logicalAdminRoutes(config);
        List<String> drainingIds = drainingUpstreams.keySet().stream().sorted().toList();
        return new ReverseProxyAdminConfigResponse(
                config.generation(),
                routes.size(),
                configuredTargets(config.properties()).size(),
                config.backendTargetCount(),
                routes,
                dnsDiscoveryStatuses(config),
                drainingIds);
    }

    private List<ReverseProxyAdminConfigResponse.RouteConfig> logicalAdminRoutes(ActiveProxyConfig config) {
        ReverseProxyProperties properties = config.properties();
        Map<String, ReverseProxyRoutePlanner.ConfiguredRoute> plannedRoutes = config.routes().stream()
                .collect(Collectors.toMap(
                        ReverseProxyRoutePlanner.ConfiguredRoute::name,
                        Function.identity()));
        if (properties.getRoutes().isEmpty()) {
            ReverseProxyRoutePlanner.ConfiguredRoute planned =
                    plannedRoutes.get(ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME);
            return List.of(new ReverseProxyAdminConfigResponse.RouteConfig(
                    ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME,
                    planned.pathPrefix(),
                    planned.match().host(),
                    planned.match().headerNames(),
                    List.of(),
                    planned.strategyId().externalName(),
                    logicalAdminUpstreams(config, properties.getUpstreams())));
        }
        return properties.getRoutes().entrySet().stream()
                .map(entry -> {
                    String routeName = entry.getKey();
                    ReverseProxyProperties.Route route = entry.getValue();
                    ReverseProxyRoutePlanner.ConfiguredRoute planned = plannedRoutes.get(routeName);
                    List<ReverseProxyAdminConfigResponse.SplitConfig> splits = route.getSplit().entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(split -> new ReverseProxyAdminConfigResponse.SplitConfig(
                                    split.getKey(),
                                    split.getValue().getPercentage(),
                                    split.getValue().getTargetIds().stream().sorted().toList()))
                            .toList();
                    return new ReverseProxyAdminConfigResponse.RouteConfig(
                            routeName,
                            planned.pathPrefix(),
                            planned.match().host(),
                            planned.match().headerNames(),
                            splits,
                            planned.strategyId().externalName(),
                            logicalAdminUpstreams(config, route.getTargets()));
                })
                .toList();
    }

    private List<ReverseProxyAdminConfigResponse.UpstreamConfig> logicalAdminUpstreams(
            ActiveProxyConfig config,
            List<ReverseProxyProperties.Upstream> targets) {
        return targets.stream()
                .map(target -> {
                    String id = target.getId().trim();
                    return new ReverseProxyAdminConfigResponse.UpstreamConfig(
                            id,
                            target.isHealthy(),
                            target.getWeight(),
                            Math.max(0, target.getMaxInFlight()),
                            Double.compare(target.getWeight(), 0.0) == 0,
                            Objects.requireNonNullElse(target.getDiscovery(), ""),
                            Objects.requireNonNullElse(target.getDiscoveryAuthority(), ""),
                            config.effectiveIdsByLogicalId().getOrDefault(id, List.of()));
                })
                .toList();
    }

    private static List<ReverseProxyProperties.Upstream> configuredTargets(ReverseProxyProperties properties) {
        if (properties.getRoutes().isEmpty()) {
            return properties.getUpstreams();
        }
        return properties.getRoutes().values().stream()
                .flatMap(route -> route.getTargets().stream())
                .toList();
    }

    private static void addTarget(
            ReverseProxyProperties properties,
            String routeName,
            ReverseProxyProperties.Upstream upstream) {
        if (properties.getRoutes().isEmpty()) {
            if (!ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME.equals(routeName)) {
                throw new IllegalStateException("route is not configured");
            }
            List<ReverseProxyProperties.Upstream> targets = new ArrayList<>(properties.getUpstreams());
            targets.add(upstream);
            properties.setUpstreams(targets);
            return;
        }
        ReverseProxyProperties.Route route = properties.getRoutes().get(routeName);
        if (route == null) {
            throw new IllegalStateException("route is not configured");
        }
        List<ReverseProxyProperties.Upstream> targets = new ArrayList<>(route.getTargets());
        targets.add(upstream);
        route.setTargets(targets);
    }

    private static void removeTarget(ReverseProxyProperties properties, String upstreamId) {
        if (properties.getRoutes().isEmpty()) {
            properties.setUpstreams(properties.getUpstreams().stream()
                    .filter(target -> !upstreamId.equals(target.getId().trim()))
                    .toList());
            return;
        }
        properties.getRoutes().values().forEach(route -> route.setTargets(route.getTargets().stream()
                .filter(target -> !upstreamId.equals(target.getId().trim()))
                .toList()));
    }

    private static String safeAdminUpstreamId(String value) {
        if (value == null) {
            return "";
        }
        String candidate = value.trim();
        return candidate.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}") ? candidate : "";
    }

    synchronized ReverseProxyReloadResponse reload(ReverseProxyProperties candidateProperties) {
        Instant attemptedAt = Instant.now(clock);
        ActiveProxyConfig previousConfig = activeConfig.get();
        try {
            if (!running.get()) {
                throw new IllegalStateException("proxy runtime is stopping and cannot reload configuration");
            }
            ActiveProxyConfig candidateConfig = buildActiveConfigForReload(
                    candidateProperties, previousConfig);
            synchronized (configurationLock) {
                rejectDrainingIdReuse(candidateConfig);
                configureHealthProber(candidateConfig);
                prepareResilienceStates(candidateConfig, attemptedAt);
                beginDrainingRemovedUpstreams(previousConfig, candidateConfig, attemptedAt);
                activeConfig.set(candidateConfig);
                metrics.activateConfiguration(candidateConfig.routes());
            }
            configureDiscoveryRuntime(candidateConfig);
            nextGeneration.updateAndGet(current -> Math.max(current, candidateConfig.generation() + 1));
            sweepDrainingUpstreamsSafely();
            scheduleDrainSweep();
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
        if (!ReverseProxyAccessLog.sameConfiguration(
                candidateProperties.getAccessLog(),
                previousConfig.properties().getAccessLog())) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.access-log configuration requires application restart"
                            + " and cannot change by reload");
        }
        if (!ProxyDnsDiscoverySettings.compile(candidateProperties.getDnsDiscovery()).equals(
                ProxyDnsDiscoverySettings.compile(previousConfig.properties().getDnsDiscovery()))) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.dns-discovery settings require application restart"
                            + " and cannot change by reload");
        }
        if (!sameWebSocketConfiguration(
                candidateProperties.getWebsocket(),
                previousConfig.properties().getWebsocket())) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.websocket configuration requires application restart"
                            + " and cannot change by reload");
        }
        long generation = nextGeneration.get();
        ProxyDnsDiscoveryRuntime.Snapshot carried = ProxyDnsEffectiveConfig.carryForward(
                candidateProperties,
                previousConfig.properties(),
                previousConfig.discoverySnapshot(),
                generation);
        return buildActiveConfig(
                candidateProperties, generation, previousConfig.routes(), carried);
    }

    private ActiveProxyConfig buildActiveConfig(
            ReverseProxyProperties candidateProperties,
            long generation,
            List<ReverseProxyRoutePlanner.ConfiguredRoute> previousRoutes) {
        return buildActiveConfig(
                candidateProperties,
                generation,
                previousRoutes,
                new ProxyDnsDiscoveryRuntime.Snapshot(generation, Map.of(), Map.of()));
    }

    private ActiveProxyConfig buildActiveConfig(
            ReverseProxyProperties candidateProperties,
            long generation,
            List<ReverseProxyRoutePlanner.ConfiguredRoute> previousRoutes,
            ProxyDnsDiscoveryRuntime.Snapshot discoverySnapshot) {
        ReverseProxyProperties safeProperties = copyProperties(
                Objects.requireNonNull(candidateProperties, "properties cannot be null"));
        ProxyDnsEffectiveConfig.Expansion expansion = ProxyDnsEffectiveConfig.expand(
                safeProperties, discoverySnapshot);
        List<ProxyDnsDiscoveryRuntime.Registration> registrations =
                ProxyDnsEffectiveConfig.registrations(safeProperties);
        List<ReverseProxyRoutePlanner.ConfiguredRoute> configuredRoutes;
        if (registrations.isEmpty()) {
            configuredRoutes = ReverseProxyRoutePlanner.buildEnabledRoutes(
                    safeProperties, registry, previousRoutes);
            validateRuntimeFields(safeProperties, configuredRoutes);
        } else {
            List<ReverseProxyRoutePlanner.ConfiguredRoute> logicalRoutes =
                    ReverseProxyRoutePlanner.buildEnabledRoutes(safeProperties, registry, List.of());
            validateRuntimeFields(safeProperties, logicalRoutes);
            configuredRoutes = ReverseProxyRoutePlanner.buildEnabledRoutes(
                    expansion.effectiveProperties(), registry, previousRoutes, expansion);
            validateEffectiveTargets(safeProperties, configuredRoutes);
        }
        ProxyRequestHeaders.ForwardedPolicy forwardedPolicy =
                ProxyRequestHeaders.compileForwarded(safeProperties.getForwarded());
        ProxyAdmissionControl.Policy admissionPolicy = ProxyAdmissionControl.compile(safeProperties, clock);
        ProxyRetryPolicy retryPolicy = ProxyRetryPolicy.compile(safeProperties.getRetry());
        return new ActiveProxyConfig(
                safeProperties,
                configuredRoutes,
                forwardedPolicy,
                admissionPolicy,
                retryPolicy,
                generation,
                discoverySnapshot,
                expansion.effectiveIdsByLogicalId(),
                expansion.logicalIdByEffectiveId());
    }

    private void validateEffectiveTargets(
            ReverseProxyProperties properties,
            List<ReverseProxyRoutePlanner.ConfiguredRoute> configuredRoutes) {
        for (ReverseProxyRoutePlanner.ConfiguredRoute route : configuredRoutes) {
            for (ReverseProxyProperties.Upstream upstream : route.targets()) {
                validateUpstreamRuntimeFields(upstream);
                httpClientProvider.clientFor(properties.getBackendTls(), upstream);
            }
        }
    }

    private void validateRuntimeFields(ReverseProxyProperties properties,
                                       List<ReverseProxyRoutePlanner.ConfiguredRoute> configuredRoutes) {
        positiveDuration(properties.getConnectTimeout(), "loadbalancerpro.proxy.connect-timeout");
        positiveDuration(properties.getRequestTimeout(), "loadbalancerpro.proxy.request-timeout");
        if (properties.getMaxRequestBytes() <= 0) {
            throw new IllegalStateException("loadbalancerpro.proxy.max-request-bytes must be greater than 0");
        }
        if (properties.getMaxResponseBytes() < 0) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.max-response-bytes must be zero or greater");
        }
        ReverseProxyAccessLog.validateConfiguration(properties.getAccessLog());
        ProxyDnsDiscoverySettings.compile(properties.getDnsDiscovery());
        validateWebSocketConfiguration(properties.getWebsocket());
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
        boundedPositiveDuration(
                properties.getReload().getDrainTimeout(),
                MAXIMUM_DRAIN_TIMEOUT,
                "loadbalancerpro.proxy.reload.drain-timeout");
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
                httpClientProvider.clientFor(properties.getBackendTls(), upstream);
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
                                             ProxyRequestBody requestBody,
                                             long maxRequestBytes,
                                             UpstreamCandidate upstream,
                                             Duration requestTimeout,
                                             String proxyPathSuffix,
                                             ProxyRequestHeaders.ForwardedPolicy forwardedPolicy,
                                             ProxyRequestHeaders.HeaderRewrites headerRewrites) throws IOException {
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
        HttpRequest.BodyPublisher publisher = requestBody.publisher(maxRequestBytes);
        return builder.method(request.getMethod().toUpperCase(Locale.ROOT), publisher).build();
    }

    private ForwardAttemptResult forwardOnce(ReverseProxyProperties properties,
                                             ProxyRequestHeaders.ForwardedPolicy forwardedPolicy,
                                             ProxyRequestHeaders.HeaderRewrites headerRewrites,
                                             HttpServletRequest request,
                                             ProxyRequestBody requestBody,
                                             UpstreamCandidate upstream,
                                             UpstreamRuntimeStats runtimeStats,
                                             String strategyName,
                                             Duration requestTimeout,
                                             String proxyPathSuffix,
                                             ProxyRequestObservation observation,
                                             boolean retryAttempt,
                                             ReverseProxyMetrics.RetryReason retryReason) {
        String upstreamId = upstream.state().serverId();
        long startedAtNanos = System.nanoTime();
        try {
            HttpRequest outbound = buildOutboundRequest(
                    request, requestBody, properties.getMaxRequestBytes(), upstream,
                    requestTimeout, proxyPathSuffix, forwardedPolicy, headerRewrites);
            observation.recordDispatch(retryAttempt, retryReason);
            if (retryAttempt) {
                metrics.recordRetryAttempt(upstreamId);
            }
            HttpResponse<InputStream> response = upstream.httpClient().send(
                    outbound, HttpResponse.BodyHandlers.ofInputStream());
            metrics.recordForwarded(upstreamId, response.statusCode());
            HttpHeaders responseHeaders = forwardedResponseHeaders(response.headers().map());
            responseHeaders.set(UPSTREAM_HEADER, upstreamId);
            responseHeaders.set(STRATEGY_HEADER, strategyName);
            ReverseProxyResponse proxyResponse =
                    new ReverseProxyResponse(response.statusCode(), responseHeaders, new byte[0]);
            boolean retryStatus = isRetryStatus(properties, response.statusCode());
            if (retryStatus) {
                logger.warn("proxy.forward.retryable_status upstreamId={} status={} reason=retry_status",
                        upstreamId, response.statusCode());
            }
            return new ForwardAttemptResult(
                    observation,
                    properties,
                    upstreamId,
                    upstream.resilienceState(),
                    runtimeStats,
                    startedAtNanos,
                    proxyResponse,
                    response.body(),
                    response.headers().firstValueAsLong(HttpHeaders.CONTENT_LENGTH).orElse(-1L),
                    retryStatus,
                    response.statusCode() < 500,
                    "upstream_response");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.warn("proxy.forward.failure upstreamId={} reason=interrupted", upstreamId);
            metrics.recordFailure(upstreamId, HttpStatus.BAD_GATEWAY.value());
            if (recordResilienceFailure(properties, upstreamId, upstream.resilienceState())) {
                observation.cooldownActivated();
            }
            runtimeStats.requestCompleted(
                    Duration.ofNanos(Math.max(0, System.nanoTime() - startedAtNanos)), false);
            return new ForwardAttemptResult(
                    observation,
                    proxyError(HttpStatus.BAD_GATEWAY, "proxy_upstream_failure",
                            "Proxy forwarding was interrupted while calling upstream " + upstreamId),
                    false,
                    false,
                    "interrupted");
        } catch (IOException | UncheckedIOException | IllegalArgumentException exception) {
            if (requestBody.limitExceeded()) {
                logger.warn("proxy.forward.failure reason=payload_too_large maxRequestBytes={}",
                        properties.getMaxRequestBytes());
                metrics.recordFailure(null, HttpStatus.PAYLOAD_TOO_LARGE.value());
                runtimeStats.requestCompleted(
                        Duration.ofNanos(Math.max(0, System.nanoTime() - startedAtNanos)), true);
                return new ForwardAttemptResult(
                        observation,
                        proxyError(HttpStatus.PAYLOAD_TOO_LARGE, "proxy_payload_too_large",
                                "Proxy request body exceeds maximum size of "
                                        + properties.getMaxRequestBytes() + " bytes"),
                        false,
                        false,
                        "request_body_too_large");
            }
            logger.warn("proxy.forward.failure upstreamId={} reason=upstream_unreachable exceptionType={}",
                    upstreamId, exception.getClass().getSimpleName());
            metrics.recordFailure(upstreamId, HttpStatus.BAD_GATEWAY.value());
            if (recordResilienceFailure(properties, upstreamId, upstream.resilienceState())) {
                observation.cooldownActivated();
            }
            runtimeStats.requestCompleted(
                    Duration.ofNanos(Math.max(0, System.nanoTime() - startedAtNanos)), false);
            return new ForwardAttemptResult(
                    observation,
                    proxyError(HttpStatus.BAD_GATEWAY, "proxy_upstream_failure",
                            "Proxy could not reach upstream " + upstreamId),
                    true,
                    false,
                    "upstream_failure");
        }
    }

    private URI targetUri(HttpServletRequest request, UpstreamCandidate upstream, String suffix) {
        String query = request.getQueryString();
        if (query != null && containsControlCharacter(query)) {
            throw new IllegalArgumentException("Proxy query string must not contain control characters.");
        }
        URI baseUri = upstream.baseUri();
        String targetPath = joinPath(baseUri.getRawPath(), suffix);
        try {
            StringBuilder rawTarget = new StringBuilder()
                    .append(baseUri.getScheme())
                    .append("://")
                    .append(baseUri.getRawAuthority())
                    .append(targetPath);
            if (query != null) {
                rawTarget.append('?').append(query);
            }
            URI target = URI.create(rawTarget.toString());
            if (!Objects.equals(target.getRawPath(), targetPath)
                    || !Objects.equals(target.getRawQuery(), query)
                    || target.getRawFragment() != null) {
                throw new IllegalArgumentException(
                        "Proxy path or query could not be preserved as raw URI components.");
            }
            validateConfiguredAuthority(baseUri, target);
            return target;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Proxy target URI could not be constructed for configured upstream.",
                    exception);
        }
    }

    private static void validateWebSocketConfiguration(ReverseProxyProperties.WebSocket websocket) {
        Objects.requireNonNull(websocket, "loadbalancerpro.proxy.websocket cannot be null");
        boundedPositiveDuration(websocket.getConnectTimeout(), MAXIMUM_WEBSOCKET_CONNECT_TIMEOUT,
                "loadbalancerpro.proxy.websocket.connect-timeout");
        boundedPositiveDuration(websocket.getIdleTimeout(), MAXIMUM_WEBSOCKET_IDLE_TIMEOUT,
                "loadbalancerpro.proxy.websocket.idle-timeout");
        boundedPositiveDuration(websocket.getSendTimeout(), MAXIMUM_WEBSOCKET_SEND_TIMEOUT,
                "loadbalancerpro.proxy.websocket.send-timeout");
        positiveInt(websocket.getMaxTextMessageBytes(),
                "loadbalancerpro.proxy.websocket.max-text-message-bytes");
        positiveInt(websocket.getMaxBinaryMessageBytes(),
                "loadbalancerpro.proxy.websocket.max-binary-message-bytes");
        positiveInt(websocket.getSendBufferBytes(),
                "loadbalancerpro.proxy.websocket.send-buffer-bytes");
        if (websocket.getAllowedOrigins().size() > 64) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.websocket.allowed-origins must contain at most 64 entries");
        }
        Set<String> normalizedOrigins = new LinkedHashSet<>();
        for (String origin : websocket.getAllowedOrigins()) {
            String normalized = validateWebSocketOrigin(origin);
            if (!normalizedOrigins.add(normalized)) {
                throw new IllegalStateException(
                        "loadbalancerpro.proxy.websocket.allowed-origins must contain unique exact origins");
            }
        }
        if (websocket.getSubprotocols().size() > 32) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.websocket.subprotocols must contain at most 32 entries");
        }
        Set<String> normalizedSubprotocols = new LinkedHashSet<>();
        for (String subprotocol : websocket.getSubprotocols()) {
            String normalized = subprotocol == null ? "" : subprotocol.trim();
            if (normalized.isEmpty() || normalized.length() > 128
                    || !normalized.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
                    || !normalizedSubprotocols.add(normalized)) {
                throw new IllegalStateException(
                        "loadbalancerpro.proxy.websocket.subprotocols must contain unique HTTP tokens");
            }
        }
    }

    private static String validateWebSocketOrigin(String origin) {
        String normalized = origin == null ? "" : origin.trim();
        if (normalized.isEmpty()
                || !normalized.equals(origin)
                || normalized.length() > 2_048
                || containsControlCharacter(normalized)) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.websocket.allowed-origins must contain bounded exact http(s) origins");
        }
        try {
            URI parsed = URI.create(normalized);
            String scheme = parsed.getScheme() == null
                    ? ""
                    : parsed.getScheme().toLowerCase(Locale.ROOT);
            if (!("http".equals(scheme) || "https".equals(scheme))
                    || parsed.getHost() == null
                    || parsed.getPort() == 0
                    || parsed.getPort() > 65_535
                    || parsed.getRawUserInfo() != null
                    || (parsed.getRawPath() != null && !parsed.getRawPath().isEmpty())
                    || parsed.getRawQuery() != null
                    || parsed.getRawFragment() != null) {
                throw new IllegalStateException(
                        "loadbalancerpro.proxy.websocket.allowed-origins must contain exact http(s) origins");
            }
            return scheme + "://" + parsed.getRawAuthority().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.websocket.allowed-origins must contain exact http(s) origins",
                    exception);
        }
    }

    private static boolean sameWebSocketConfiguration(
            ReverseProxyProperties.WebSocket left, ReverseProxyProperties.WebSocket right) {
        return left.isEnabled() == right.isEnabled()
                && Objects.equals(left.getConnectTimeout(), right.getConnectTimeout())
                && Objects.equals(left.getIdleTimeout(), right.getIdleTimeout())
                && Objects.equals(left.getSendTimeout(), right.getSendTimeout())
                && left.getMaxTextMessageBytes() == right.getMaxTextMessageBytes()
                && left.getMaxBinaryMessageBytes() == right.getMaxBinaryMessageBytes()
                && left.getSendBufferBytes() == right.getSendBufferBytes()
                && Objects.equals(left.getAllowedOrigins(), right.getAllowedOrigins())
                && Objects.equals(left.getSubprotocols(), right.getSubprotocols());
    }

    private static URI webSocketTargetUri(URI httpTarget) {
        String scheme = switch (httpTarget.getScheme().toLowerCase(Locale.ROOT)) {
            case "http" -> "ws";
            case "https" -> "wss";
            default -> throw new IllegalArgumentException(
                    "WebSocket upstream must use an http or https configured base URL.");
        };
        StringBuilder rawTarget = new StringBuilder()
                .append(scheme)
                .append("://")
                .append(httpTarget.getRawAuthority())
                .append(httpTarget.getRawPath());
        if (httpTarget.getRawQuery() != null) {
            rawTarget.append('?').append(httpTarget.getRawQuery());
        }
        URI target = URI.create(rawTarget.toString());
        if (!Objects.equals(target.getHost(), httpTarget.getHost())
                || target.getPort() != httpTarget.getPort()
                || !Objects.equals(target.getRawPath(), httpTarget.getRawPath())
                || !Objects.equals(target.getRawQuery(), httpTarget.getRawQuery())
                || target.getRawFragment() != null) {
            throw new IllegalArgumentException("WebSocket target escaped configured upstream authority.");
        }
        return target;
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
            HttpServletRequest request,
            String proxyPathSuffix) {
        return routes.stream()
                .filter(route -> ReverseProxyRoutePlanner.pathMatches(route.pathPrefix(), proxyPathSuffix))
                .filter(route -> route.match().matches(request))
                .sorted(Comparator
                        .comparingInt((ReverseProxyRoutePlanner.ConfiguredRoute route) ->
                                route.match().hostSpecificity()).reversed()
                        .thenComparing(Comparator.comparingInt(
                                (ReverseProxyRoutePlanner.ConfiguredRoute route) ->
                                        route.pathPrefix().length()).reversed())
                        .thenComparing(Comparator.comparingInt(
                                (ReverseProxyRoutePlanner.ConfiguredRoute route) ->
                                        route.match().headers().size()).reversed())
                        .thenComparing(ReverseProxyRoutePlanner.ConfiguredRoute::name))
                .findFirst();
    }

    private List<UpstreamCandidate> configuredUpstreams(ActiveProxyConfig config,
                                                         ReverseProxyRoutePlanner.ConfiguredRoute route,
                                                         Set<String> allowedUpstreamIds,
                                                         Set<String> excludedUpstreamIds) {
        Instant now = Instant.now(clock);
        return route.targets().stream()
                .filter(upstream -> allowedUpstreamIds == null
                        || allowedUpstreamIds.contains(requireNonBlank(
                                upstream.getId(), "loadbalancerpro.proxy.upstreams[].id")))
                .filter(upstream -> !config.isUpstreamRetired(requireNonBlank(
                        upstream.getId(), "loadbalancerpro.proxy.upstreams[].id")))
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
        ResilienceState resilienceState = resilienceState(id, timestamp);
        double effectiveWeight = resilienceState.effectiveWeight(
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
                baseUri, state, effectiveMaxInFlight, telemetrySampleSize, resilienceState,
                httpClientProvider.clientFor(properties.getBackendTls(), upstream));
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

    private void prepareResilienceStates(ActiveProxyConfig config, Instant addedAt) {
        configuredUpstreamIds(config).forEach(
                upstreamId -> resilienceStates.computeIfAbsent(
                        upstreamId, ignored -> new ResilienceState(addedAt)));
    }

    private void rejectDrainingIdReuse(ActiveProxyConfig candidateConfig) {
        Set<String> conflictingIds = configuredUpstreamIds(candidateConfig).stream()
                .filter(drainingUpstreams::containsKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!conflictingIds.isEmpty()) {
            throw new IllegalStateException(
                    "reload cannot reuse upstream ids while they are draining: " + conflictingIds);
        }
    }

    private void beginDrainingRemovedUpstreams(
            ActiveProxyConfig previousConfig,
            ActiveProxyConfig candidateConfig,
            Instant startedAt) {
        Set<String> removedIds = new LinkedHashSet<>(configuredUpstreamIds(previousConfig));
        removedIds.removeAll(configuredUpstreamIds(candidateConfig));
        Instant deadline = startedAt.plus(candidateConfig.properties().getReload().getDrainTimeout());
        for (String upstreamId : removedIds) {
            previousConfig.retireUpstream(upstreamId);
            drainingUpstreams.put(upstreamId, new DrainingUpstream(
                    upstreamId,
                    previousConfig,
                    upstreamRuntimeStats.get(upstreamId),
                    resilienceStates.get(upstreamId),
                    deadline));
            logger.info("proxy.config.reload upstreamId={} state=DRAINING", upstreamId);
        }
    }

    private void sweepDrainingUpstreamsSafely() {
        try {
            sweepDrainingUpstreams();
        } catch (RuntimeException exception) {
            logger.warn("proxy.config.reload drainSweep=failure exceptionType={}",
                    exception.getClass().getSimpleName());
        }
    }

    private void scheduleDrainSweep() {
        if (!running.get() || drainingUpstreams.isEmpty()
                || !drainSweepScheduled.compareAndSet(false, true)) {
            return;
        }
        try {
            drainScheduler.schedule(() -> {
                drainSweepScheduled.set(false);
                sweepDrainingUpstreamsSafely();
                scheduleDrainSweep();
            }, DRAIN_SWEEP_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.RejectedExecutionException exception) {
            drainSweepScheduled.set(false);
            if (running.get()) {
                throw exception;
            }
        }
    }

    private void sweepDrainingUpstreams() {
        synchronized (configurationLock) {
            if (drainingUpstreams.isEmpty()) {
                return;
            }
            Instant now = Instant.now(clock);
            Set<String> configuredIds = configuredUpstreamIds(activeConfig.get());
            for (Map.Entry<String, DrainingUpstream> entry : drainingUpstreams.entrySet()) {
                DrainingUpstream draining = entry.getValue();
                int upstreamInFlight = draining.runtimeStats() == null
                        ? 0
                        : draining.runtimeStats().snapshot().inFlightRequestCount();
                boolean drained = draining.previousConfig().activeRequestCount() == 0
                        && upstreamInFlight == 0;
                boolean timedOut = !now.isBefore(draining.deadline());
                if ((!drained && !timedOut) || !drainingUpstreams.remove(entry.getKey(), draining)) {
                    continue;
                }
                if (!configuredIds.contains(draining.upstreamId())) {
                    if (draining.runtimeStats() != null) {
                        upstreamRuntimeStats.remove(draining.upstreamId(), draining.runtimeStats());
                    }
                    if (draining.resilienceState() != null) {
                        resilienceStates.remove(draining.upstreamId(), draining.resilienceState());
                    }
                }
                logger.info("proxy.config.reload upstreamId={} state=REMOVED outcome={}",
                        draining.upstreamId(), drained ? "drained" : "timeout");
            }
        }
    }

    int drainingUpstreamCountForTesting() {
        return drainingUpstreams.size();
    }

    void sweepDrainingUpstreamsForTesting() {
        sweepDrainingUpstreams();
    }

    boolean drainSchedulerShutdownForTesting() {
        return drainScheduler.isShutdown();
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
        String targetPath = joinPath(baseUri.getRawPath(), healthPath);
        try {
            URI target = URI.create(baseUri.getScheme() + "://" + baseUri.getRawAuthority() + targetPath);
            if (!Objects.equals(target.getRawPath(), targetPath)
                    || target.getRawQuery() != null || target.getRawFragment() != null) {
                throw new IllegalArgumentException(
                        "Proxy health-check path could not be preserved as a raw URI component.");
            }
            validateConfiguredAuthority(baseUri, target);
            return target;
        } catch (IllegalArgumentException exception) {
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

    private boolean recordResilienceFailure(ReverseProxyProperties properties, String upstreamId) {
        if (upstreamId == null || upstreamId.isBlank()) {
            return false;
        }
        return recordResilienceFailure(properties, upstreamId, resilienceStates.get(upstreamId));
    }

    private boolean recordResilienceFailure(
            ReverseProxyProperties properties, String upstreamId, ResilienceState state) {
        if (state == null) {
            return false;
        }
        Instant now = Instant.now(clock);
        boolean activated = state.recordFailure(now, properties.getCooldown());
        if (activated) {
            metrics.recordCooldownActivation(upstreamId);
            logger.warn("proxy.cooldown.activated upstreamId={} threshold={} durationMillis={}",
                    upstreamId,
                    Math.max(1, properties.getCooldown().getConsecutiveFailureThreshold()),
                    Math.max(0, properties.getCooldown().getDuration().toMillis()));
        }
        return activated;
    }

    private void recordResilienceSuccess(String upstreamId, ResilienceState state) {
        if (upstreamId == null || upstreamId.isBlank()) {
            return;
        }
        if (state != null) {
            state.recordSuccess(Instant.now(clock));
        }
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
                "proxy.observability.route route={} pathPrefix={} hostMatchConfigured={} headerMatchCount={} "
                        + "splitCount={} strategy={} targetCount={} targetIds={}",
                route.name(),
                route.pathPrefix(),
                route.match().host() != null,
                route.match().headers().size(),
                route.splits().size(),
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
            healthProber.clear();
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
                            config.generation(),
                            httpClientProvider.clientFor(properties.getBackendTls(), upstream));
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
        metrics.recordHealth(upstreamId, snapshot.healthy());
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

    @Override
    public void start() {
        if (!running.get()) {
            throw new IllegalStateException("proxy runtime cannot restart after shutdown");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            ProxyDnsDiscoveryRuntime discovery = dnsDiscoveryRuntime;
            dnsDiscoveryRuntime = null;
            if (discovery != null) {
                discovery.close();
            }
            healthProber.stop();
            drainScheduler.shutdownNow();
            drainingUpstreams.clear();
            metrics.activateConfiguration(List.of());
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    void closeHealthProber() {
        stop();
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

    private static Duration boundedPositiveDuration(
            Duration value, Duration maximum, String fieldName) {
        positiveDuration(value, fieldName);
        if (value.compareTo(maximum) > 0) {
            throw new IllegalStateException(
                    fieldName + " must be no greater than " + maximum.toMinutes() + "m");
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

    static ReverseProxyProperties copyProperties(ReverseProxyProperties source) {
        ReverseProxyProperties copy = new ReverseProxyProperties();
        copy.setEnabled(source.isEnabled());
        copy.setStrategy(source.getStrategy());
        copy.setConnectTimeout(source.getConnectTimeout());
        copy.setRequestTimeout(source.getRequestTimeout());
        copy.setMaxRequestBytes(source.getMaxRequestBytes());
        copy.setMaxResponseBytes(source.getMaxResponseBytes());
        copy.setPrivateNetworkValidation(copyPrivateNetworkValidation(source.getPrivateNetworkValidation()));
        copy.setPrivateNetworkLiveValidation(copyPrivateNetworkLiveValidation(
                source.getPrivateNetworkLiveValidation()));
        copy.setHealthCheck(copyHealthCheck(source.getHealthCheck()));
        copy.setRetry(copyRetry(source.getRetry()));
        copy.setReload(copyReload(source.getReload()));
        copy.setCooldown(copyCooldown(source.getCooldown()));
        copy.setSlowStart(copySlowStart(source.getSlowStart()));
        copy.setForwarded(copyForwarded(source.getForwarded()));
        copy.setLimits(copyLimits(source.getLimits()));
        copy.setShedding(copyShedding(source.getShedding()));
        copy.setBackendTls(copyBackendTls(source.getBackendTls()));
        copy.setAccessLog(copyAccessLog(source.getAccessLog()));
        copy.setDnsDiscovery(copyDnsDiscovery(source.getDnsDiscovery()));
        copy.setWebsocket(copyWebSocket(source.getWebsocket()));
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
        copy.setMatch(copyMatch(source.getMatch()));
        Map<String, ReverseProxyProperties.SplitGroup> splits = new LinkedHashMap<>();
        source.getSplit().forEach((name, split) -> splits.put(name, copySplit(split)));
        copy.setSplit(splits);
        copy.setAffinity(copyAffinity(source.getAffinity()));
        copy.setHeaders(copyHeaders(source.getHeaders()));
        copy.setTargets(source.getTargets().stream()
                .map(ReverseProxyService::copyUpstream)
                .toList());
        return copy;
    }

    private static ReverseProxyProperties.Match copyMatch(ReverseProxyProperties.Match source) {
        ReverseProxyProperties.Match copy = new ReverseProxyProperties.Match();
        if (source != null) {
            copy.setHost(source.getHost());
            copy.setHeader(source.getHeader());
        }
        return copy;
    }

    private static ReverseProxyProperties.SplitGroup copySplit(ReverseProxyProperties.SplitGroup source) {
        ReverseProxyProperties.SplitGroup copy = new ReverseProxyProperties.SplitGroup();
        if (source != null) {
            copy.setPercentage(source.getPercentage());
            copy.setTargetIds(source.getTargetIds());
        }
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

    static ReverseProxyProperties.Upstream copyUpstream(ReverseProxyProperties.Upstream source) {
        ReverseProxyProperties.Upstream copy = new ReverseProxyProperties.Upstream();
        if (source == null) {
            return copy;
        }
        copy.setId(source.getId());
        copy.setUrl(source.getUrl());
        copy.setDiscovery(source.getDiscovery());
        copy.setDiscoveryAuthority(source.getDiscoveryAuthority());
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
        copy.setTls(copyTls(source.getTls()));
        return copy;
    }

    private static ReverseProxyProperties.BackendTls copyBackendTls(
            ReverseProxyProperties.BackendTls source) {
        ReverseProxyProperties.BackendTls copy = new ReverseProxyProperties.BackendTls();
        if (source != null) {
            copy.setTruststore(source.getTruststore());
        }
        return copy;
    }

    private static ReverseProxyProperties.AccessLog copyAccessLog(
            ReverseProxyProperties.AccessLog source) {
        ReverseProxyProperties.AccessLog copy = new ReverseProxyProperties.AccessLog();
        if (source != null) {
            copy.setEnabled(source.isEnabled());
            copy.setFormat(source.getFormat());
            copy.setPath(source.getPath());
            copy.setSampleRate(source.getSampleRate());
        }
        return copy;
    }

    private void configureDiscoveryRuntime(ActiveProxyConfig config) {
        List<ProxyDnsDiscoveryRuntime.Registration> registrations =
                ProxyDnsEffectiveConfig.registrations(config.properties());
        ProxyDnsDiscoveryRuntime runtime;
        ProxyDnsDiscoveryRuntime toClose = null;
        synchronized (configurationLock) {
            if (registrations.isEmpty()) {
                toClose = dnsDiscoveryRuntime;
                dnsDiscoveryRuntime = null;
                runtime = null;
            } else {
                runtime = dnsDiscoveryRuntime;
                if (runtime == null) {
                    runtime = new ProxyDnsDiscoveryRuntime(
                            ProxyDnsDiscoverySettings.compile(config.properties().getDnsDiscovery()),
                            dnsResolver,
                            this::applyDiscoverySnapshot);
                    dnsDiscoveryRuntime = runtime;
                }
            }
        }
        if (toClose != null) {
            toClose.close();
        }
        if (runtime != null) {
            runtime.replace(config.generation(), registrations);
        }
    }

    private void applyDiscoverySnapshot(ProxyDnsDiscoveryRuntime.Snapshot snapshot) {
        if (!running.get()) {
            return;
        }
        ActiveProxyConfig previous = activeConfig.get();
        if (snapshot.generation() != previous.generation()
                || snapshot.membersByLogicalId().equals(
                        previous.discoverySnapshot().membersByLogicalId())) {
            return;
        }
        ActiveProxyConfig candidate;
        try {
            candidate = buildActiveConfig(
                    previous.properties(), previous.generation(), previous.routes(), snapshot);
        } catch (RuntimeException invalidSnapshot) {
            logger.warn("proxy.dns.publication status=rejected generation={} reason=effective_snapshot_invalid",
                    previous.generation());
            return;
        }
        Instant now = Instant.now(clock);
        synchronized (configurationLock) {
            if (!running.get() || activeConfig.get() != previous) {
                return;
            }
            reinstateReappearedUpstreams(candidate);
            configureHealthProber(candidate);
            prepareResilienceStates(candidate, now);
            beginDrainingRemovedUpstreams(previous, candidate, now);
            activeConfig.set(candidate);
            metrics.activateConfiguration(candidate.routes());
        }
        sweepDrainingUpstreamsSafely();
        scheduleDrainSweep();
    }

    private void reinstateReappearedUpstreams(ActiveProxyConfig candidate) {
        for (String upstreamId : configuredUpstreamIds(candidate)) {
            drainingUpstreams.remove(upstreamId);
        }
    }

    private static ReverseProxyProperties.DnsDiscovery copyDnsDiscovery(
            ReverseProxyProperties.DnsDiscovery source) {
        ReverseProxyProperties.DnsDiscovery copy = new ReverseProxyProperties.DnsDiscovery();
        if (source != null) {
            copy.setTtlFloor(source.getTtlFloor());
            copy.setStaleAfter(source.getStaleAfter());
            copy.setResolutionTimeout(source.getResolutionTimeout());
            copy.setLookupThreads(source.getLookupThreads());
        }
        return copy;
    }

    private static ReverseProxyProperties.WebSocket copyWebSocket(
            ReverseProxyProperties.WebSocket source) {
        ReverseProxyProperties.WebSocket copy = new ReverseProxyProperties.WebSocket();
        if (source != null) {
            copy.setEnabled(source.isEnabled());
            copy.setConnectTimeout(source.getConnectTimeout());
            copy.setIdleTimeout(source.getIdleTimeout());
            copy.setSendTimeout(source.getSendTimeout());
            copy.setMaxTextMessageBytes(source.getMaxTextMessageBytes());
            copy.setMaxBinaryMessageBytes(source.getMaxBinaryMessageBytes());
            copy.setSendBufferBytes(source.getSendBufferBytes());
            copy.setAllowedOrigins(source.getAllowedOrigins());
            copy.setSubprotocols(source.getSubprotocols());
        }
        return copy;
    }

    private static ReverseProxyProperties.Tls copyTls(ReverseProxyProperties.Tls source) {
        ReverseProxyProperties.Tls copy = new ReverseProxyProperties.Tls();
        if (source != null) {
            copy.setVerify(source.isVerify());
            copy.setClientCert(source.getClientCert());
        }
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

    private static ReverseProxyProperties.Reload copyReload(ReverseProxyProperties.Reload source) {
        ReverseProxyProperties.Reload copy = new ReverseProxyProperties.Reload();
        if (source != null) {
            copy.setDrainTimeout(source.getDrainTimeout());
        }
        return copy;
    }

    private record ActiveProxyConfig(
            ReverseProxyProperties properties,
            List<ReverseProxyRoutePlanner.ConfiguredRoute> routes,
            ProxyRequestHeaders.ForwardedPolicy forwardedPolicy,
            ProxyAdmissionControl.Policy admissionPolicy,
            ProxyRetryPolicy retryPolicy,
            long generation,
            ProxyDnsDiscoveryRuntime.Snapshot discoverySnapshot,
            Map<String, List<String>> effectiveIdsByLogicalId,
            Map<String, String> logicalIdByEffectiveId,
            AtomicInteger activeRequests,
            Set<String> retiredUpstreamIds) {
        private ActiveProxyConfig(
                ReverseProxyProperties properties,
                List<ReverseProxyRoutePlanner.ConfiguredRoute> routes,
                ProxyRequestHeaders.ForwardedPolicy forwardedPolicy,
                ProxyAdmissionControl.Policy admissionPolicy,
                ProxyRetryPolicy retryPolicy,
                long generation,
                ProxyDnsDiscoveryRuntime.Snapshot discoverySnapshot,
                Map<String, List<String>> effectiveIdsByLogicalId,
                Map<String, String> logicalIdByEffectiveId) {
            this(properties, routes, forwardedPolicy, admissionPolicy, retryPolicy,
                    generation, discoverySnapshot, effectiveIdsByLogicalId, logicalIdByEffectiveId,
                    new AtomicInteger(), ConcurrentHashMap.newKeySet());
        }

        void requestStarted() {
            activeRequests.incrementAndGet();
        }

        void requestCompleted() {
            int remaining = activeRequests.decrementAndGet();
            if (remaining < 0) {
                activeRequests.incrementAndGet();
                throw new IllegalStateException("active proxy request count cannot be negative");
            }
        }

        int activeRequestCount() {
            return activeRequests.get();
        }

        void retireUpstream(String upstreamId) {
            retiredUpstreamIds.add(upstreamId);
        }

        boolean isUpstreamRetired(String upstreamId) {
            return retiredUpstreamIds.contains(upstreamId);
        }

        int routeCount() {
            return routes.size();
        }

        int backendTargetCount() {
            return routes.stream()
                    .mapToInt(route -> route.targets().size())
                    .sum();
        }
    }

    private record DrainingUpstream(
            String upstreamId,
            ActiveProxyConfig previousConfig,
            UpstreamRuntimeStats runtimeStats,
            ResilienceState resilienceState,
            Instant deadline) {
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
            int telemetrySampleSize,
            ResilienceState resilienceState,
            HttpClient httpClient) {
    }

    private record EffectiveHealth(
            boolean healthy,
            String source,
            Integer lastProbeStatusCode,
            String lastProbeOutcome) {
    }

    private static final class ProxyRequestObservation {
        private final ReverseProxyMetrics.RequestObservation metrics;
        private final ReverseProxyAccessLog.RequestLogObservation accessLog;

        private ProxyRequestObservation(
                ReverseProxyMetrics.RequestObservation metrics,
                ReverseProxyAccessLog.RequestLogObservation accessLog) {
            this.metrics = Objects.requireNonNull(metrics, "metrics cannot be null");
            this.accessLog = accessLog;
        }

        private void bindRoute(String route) {
            metrics.bindRoute(route);
            if (accessLog != null) {
                try {
                    accessLog.bindRoute(route);
                } catch (RuntimeException exception) {
                    // Access logging cannot alter the proxy request lifecycle.
                }
            }
        }

        private void bindUpstream(String route, String upstream) {
            metrics.bindUpstream(route, upstream);
            if (accessLog != null) {
                try {
                    accessLog.bindUpstream(route, upstream);
                } catch (RuntimeException exception) {
                    // Access logging cannot alter the proxy request lifecycle.
                }
            }
        }

        private void recordDispatch(boolean retry, ReverseProxyMetrics.RetryReason reason) {
            metrics.recordDispatch(retry, reason);
            if (accessLog != null) {
                try {
                    accessLog.recordDispatch(retry);
                } catch (RuntimeException exception) {
                    // Access logging cannot alter the proxy request lifecycle.
                }
            }
        }

        private void addResponseBytes(long bytes) {
            metrics.addResponseBytes(bytes);
            if (accessLog != null) {
                try {
                    accessLog.addResponseBytes(bytes);
                } catch (RuntimeException exception) {
                    // Access logging cannot alter the proxy request lifecycle.
                }
            }
        }

        private void cooldownActivated() {
            if (accessLog != null) {
                try {
                    accessLog.cooldownActivated();
                } catch (RuntimeException exception) {
                    // Access logging cannot alter the proxy request lifecycle.
                }
            }
        }

        private void terminal(int statusCode, ReverseProxyMetrics.TerminalOutcome outcome) {
            metrics.terminal(statusCode, outcome);
            if (accessLog != null) {
                try {
                    accessLog.terminal(statusCode, outcome);
                } catch (RuntimeException exception) {
                    // Access logging cannot alter the proxy request lifecycle.
                }
            }
        }

        private void terminalIfUnset(int statusCode, ReverseProxyMetrics.TerminalOutcome outcome) {
            metrics.terminalIfUnset(statusCode, outcome);
            if (accessLog != null) {
                try {
                    accessLog.terminalIfUnset(statusCode, outcome);
                } catch (RuntimeException exception) {
                    // Access logging cannot alter the proxy request lifecycle.
                }
            }
        }

        private void complete(long requestBytes) {
            long completedAtNanos = System.nanoTime();
            try {
                metrics.completeAt(requestBytes, completedAtNanos);
            } finally {
                if (accessLog != null) {
                    try {
                        accessLog.completeAt(requestBytes, completedAtNanos);
                    } catch (RuntimeException exception) {
                        // Access logging cannot alter the proxy request lifecycle.
                    }
                }
            }
        }
    }

    private interface ProxyDownstream {
        void status(int statusCode);

        void header(String name, String value);

        OutputStream outputStream() throws IOException;
    }

    private static final class CountingProxyDownstream implements ProxyDownstream {
        private final ProxyDownstream delegate;
        private final ProxyRequestObservation observation;
        private OutputStream countingOutput;

        private CountingProxyDownstream(
                ProxyDownstream delegate, ProxyRequestObservation observation) {
            this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
            this.observation = Objects.requireNonNull(observation, "observation cannot be null");
        }

        @Override
        public void status(int statusCode) {
            delegate.status(statusCode);
        }

        @Override
        public void header(String name, String value) {
            delegate.header(name, value);
        }

        @Override
        public synchronized OutputStream outputStream() throws IOException {
            if (countingOutput == null) {
                countingOutput = new CountingOutputStream(delegate.outputStream(), observation);
            }
            return countingOutput;
        }
    }

    private static final class CountingOutputStream extends OutputStream {
        private final OutputStream delegate;
        private final ProxyRequestObservation observation;

        private CountingOutputStream(
                OutputStream delegate, ProxyRequestObservation observation) {
            this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
            this.observation = Objects.requireNonNull(observation, "observation cannot be null");
        }

        @Override
        public void write(int value) throws IOException {
            delegate.write(value);
            observation.addResponseBytes(1);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            delegate.write(bytes, offset, length);
            observation.addResponseBytes(length);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    private record ServletProxyDownstream(HttpServletResponse response) implements ProxyDownstream {
        private ServletProxyDownstream {
            Objects.requireNonNull(response, "response cannot be null");
        }

        @Override
        public void status(int statusCode) {
            response.setStatus(statusCode);
        }

        @Override
        public void header(String name, String value) {
            response.addHeader(name, value);
        }

        @Override
        public OutputStream outputStream() throws IOException {
            return response.getOutputStream();
        }
    }

    private enum DiscardingProxyDownstream implements ProxyDownstream {
        INSTANCE;

        @Override
        public void status(int statusCode) {
        }

        @Override
        public void header(String name, String value) {
        }

        @Override
        public OutputStream outputStream() {
            return OutputStream.nullOutputStream();
        }
    }

    private final class ForwardAttemptResult {
        private final ProxyRequestObservation observation;
        private final ReverseProxyProperties properties;
        private final String upstreamId;
        private final ResilienceState resilienceState;
        private final UpstreamRuntimeStats runtimeStats;
        private final long runtimeStartedAtNanos;
        private final long declaredLength;
        private final boolean retryStatus;
        private final boolean runtimeSuccessfulStatus;
        private ReverseProxyResponse response;
        private InputStream upstreamBody;
        private boolean upstreamResponse;
        private boolean retriable;
        private boolean affinityEligible;
        private String outcome;
        private boolean runtimePending;
        private boolean prepared;
        private boolean bodyAllowed;
        private byte[] copyBuffer;
        private int prefetchedBytes = -1;
        private boolean decisionRecorded;

        private ForwardAttemptResult(
                ProxyRequestObservation observation,
                ReverseProxyProperties properties,
                String upstreamId,
                ResilienceState resilienceState,
                UpstreamRuntimeStats runtimeStats,
                long runtimeStartedAtNanos,
                ReverseProxyResponse response,
                InputStream upstreamBody,
                long declaredLength,
                boolean retryStatus,
                boolean runtimeSuccessfulStatus,
                String outcome) {
            this.observation = observation;
            this.properties = properties;
            this.upstreamId = upstreamId;
            this.resilienceState = resilienceState;
            this.runtimeStats = runtimeStats;
            this.runtimeStartedAtNanos = runtimeStartedAtNanos;
            this.response = response;
            this.upstreamBody = Objects.requireNonNull(upstreamBody, "upstreamBody cannot be null");
            this.declaredLength = declaredLength;
            this.retryStatus = retryStatus;
            this.runtimeSuccessfulStatus = runtimeSuccessfulStatus;
            this.upstreamResponse = true;
            this.retriable = retryStatus;
            this.affinityEligible = !retryStatus && runtimeSuccessfulStatus;
            this.outcome = outcome;
            this.runtimePending = true;
        }

        private ForwardAttemptResult(
                ProxyRequestObservation observation,
                ReverseProxyResponse response,
                boolean retriable,
                boolean affinityEligible,
                String outcome) {
            this.observation = observation;
            this.properties = null;
            this.upstreamId = null;
            this.resilienceState = null;
            this.runtimeStats = null;
            this.runtimeStartedAtNanos = 0;
            this.response = response;
            this.upstreamBody = null;
            this.declaredLength = -1;
            this.retryStatus = false;
            this.runtimeSuccessfulStatus = false;
            this.upstreamResponse = false;
            this.retriable = retriable;
            this.affinityEligible = affinityEligible;
            this.outcome = outcome;
            this.runtimePending = false;
            this.prepared = true;
        }

        private void prepareForDelivery(HttpServletRequest request) {
            if (prepared || !upstreamResponse) {
                return;
            }
            prepared = true;
            bodyAllowed = responsePermitsBody(request, response.statusCode());
            if (!bodyAllowed) {
                return;
            }
            long maximumBytes = properties.getMaxResponseBytes();
            if (maximumBytes > 0 && declaredLength > maximumBytes) {
                responseLimitExceededBeforeCommit();
                return;
            }
            copyBuffer = new byte[RESPONSE_COPY_BUFFER_BYTES];
            int readLength = RESPONSE_COPY_BUFFER_BYTES;
            if (maximumBytes > 0 && maximumBytes < RESPONSE_COPY_BUFFER_BYTES) {
                readLength = (int) Math.min(RESPONSE_COPY_BUFFER_BYTES, maximumBytes + 1);
            }
            try {
                prefetchedBytes = readChunk(readLength);
            } catch (IOException exception) {
                upstreamStreamFailed(true);
                return;
            } catch (RuntimeException exception) {
                upstreamStreamFailed(true);
                return;
            }
            if (prefetchedBytes >= 0 && maximumBytes > 0 && prefetchedBytes > maximumBytes) {
                responseLimitExceededBeforeCommit();
            }
        }

        private int readNextChunk() throws IOException {
            return readChunk(RESPONSE_COPY_BUFFER_BYTES);
        }

        private int readChunk(int length) throws IOException {
            int read;
            do {
                read = upstreamBody.read(copyBuffer, 0, length);
            } while (read == 0);
            return read;
        }

        private void discardForRetry() {
            closeBody();
            if (runtimePending) {
                if (retryStatus) {
                    recordAttemptResilienceFailure();
                }
                completeRuntime(runtimeSuccessfulStatus);
            }
        }

        private void completeHttpResponse() {
            closeBody();
            if (!runtimePending) {
                return;
            }
            if (retryStatus) {
                recordAttemptResilienceFailure();
            } else {
                recordResilienceSuccess(upstreamId, resilienceState);
            }
            completeRuntime(runtimeSuccessfulStatus);
        }

        private void upstreamStreamFailed(boolean beforeCommitment) {
            closeBody();
            if (runtimePending) {
                logger.warn("proxy.forward.failure upstreamId={} reason={}",
                        upstreamId,
                        beforeCommitment
                                ? "upstream_stream_failure_before_commit"
                                : "upstream_stream_failure_after_commit");
                metrics.recordFailure(upstreamId, HttpStatus.BAD_GATEWAY.value());
                recordAttemptResilienceFailure();
                completeRuntime(false);
            }
            retriable = beforeCommitment;
            affinityEligible = false;
            outcome = beforeCommitment
                    ? "upstream_stream_failure_before_commit"
                    : "upstream_stream_failure_after_commit";
            if (beforeCommitment) {
                upstreamResponse = false;
                response = proxyError(
                        HttpStatus.BAD_GATEWAY,
                        "proxy_upstream_failure",
                        "Proxy upstream response failed before downstream commitment");
            }
        }

        private void recordAttemptResilienceFailure() {
            if (recordResilienceFailure(properties, upstreamId, resilienceState) && observation != null) {
                observation.cooldownActivated();
            }
        }

        private void responseLimitExceededBeforeCommit() {
            responseLimitExceeded(true);
            upstreamResponse = false;
            response = proxyError(
                    HttpStatus.BAD_GATEWAY,
                    "proxy_response_too_large",
                    "Proxy upstream response exceeds maximum size of "
                            + properties.getMaxResponseBytes() + " bytes");
        }

        private void responseLimitExceeded(boolean beforeCommitment) {
            closeBody();
            if (runtimePending) {
                metrics.recordFailure(upstreamId, HttpStatus.BAD_GATEWAY.value());
                completeRuntime(false);
            }
            retriable = false;
            affinityEligible = false;
            outcome = beforeCommitment
                    ? "response_body_too_large_before_commit"
                    : "response_body_too_large_after_commit";
        }

        private void downstreamDisconnected() {
            closeBody();
            if (runtimePending) {
                runtimeStats.requestAborted();
                runtimePending = false;
            }
            logger.debug("proxy.forward.cancelled upstreamId={} reason=downstream_disconnect", upstreamId);
            retriable = false;
            affinityEligible = false;
            outcome = "downstream_disconnect";
        }

        private void abortWithoutHealthPenalty() {
            closeBody();
            if (runtimePending) {
                runtimeStats.requestAborted();
                runtimePending = false;
            }
        }

        private void completeRuntime(boolean successful) {
            runtimeStats.requestCompleted(
                    Duration.ofNanos(Math.max(0, System.nanoTime() - runtimeStartedAtNanos)), successful);
            runtimePending = false;
        }

        private void closeBody() {
            if (upstreamBody == null) {
                return;
            }
            try {
                upstreamBody.close();
            } catch (IOException | RuntimeException exception) {
                logger.debug("proxy.forward.body_close_failed upstreamId={} exceptionType={}",
                        upstreamId, exception.getClass().getSimpleName());
            } finally {
                upstreamBody = null;
            }
        }

        private boolean markDecisionRecorded() {
            if (decisionRecorded) {
                return false;
            }
            decisionRecorded = true;
            return true;
        }

        private ReverseProxyResponse response() {
            return response;
        }

        private void response(ReverseProxyResponse response) {
            this.response = response;
        }

        private boolean retriable() {
            return retriable;
        }

        private boolean affinityEligible() {
            return affinityEligible;
        }

        private String outcome() {
            return outcome;
        }

        private ReverseProxyMetrics.RetryReason retryReason() {
            if (retryStatus) {
                return ReverseProxyMetrics.RetryReason.RETRYABLE_STATUS;
            }
            return switch (outcome) {
                case "upstream_failure" -> ReverseProxyMetrics.RetryReason.TRANSPORT_FAILURE;
                case "upstream_stream_failure_before_commit" ->
                        ReverseProxyMetrics.RetryReason.PRECOMMIT_UPSTREAM_FAILURE;
                default -> ReverseProxyMetrics.RetryReason.OTHER;
            };
        }

        private ReverseProxyMetrics.TerminalOutcome terminalOutcome() {
            return switch (outcome) {
                case "upstream_response" ->
                        ReverseProxyMetrics.TerminalOutcome.fromStatus(response.statusCode());
                case "interrupted" -> ReverseProxyMetrics.TerminalOutcome.INTERRUPTED;
                case "request_body_too_large" -> ReverseProxyMetrics.TerminalOutcome.REQUEST_SIZE_LIMIT;
                case "upstream_failure" -> ReverseProxyMetrics.TerminalOutcome.UPSTREAM_TRANSPORT_FAILURE;
                case "upstream_stream_failure_before_commit" ->
                        ReverseProxyMetrics.TerminalOutcome.UPSTREAM_ABORT_PRECOMMIT;
                case "upstream_stream_failure_after_commit" ->
                        ReverseProxyMetrics.TerminalOutcome.UPSTREAM_ABORT_POSTCOMMIT;
                case "response_body_too_large_before_commit" ->
                        ReverseProxyMetrics.TerminalOutcome.RESPONSE_SIZE_LIMIT_PRECOMMIT;
                case "response_body_too_large_after_commit" ->
                        ReverseProxyMetrics.TerminalOutcome.RESPONSE_SIZE_LIMIT_POSTCOMMIT;
                case "downstream_disconnect" -> ReverseProxyMetrics.TerminalOutcome.DOWNSTREAM_DISCONNECT;
                default -> ReverseProxyMetrics.TerminalOutcome.OTHER;
            };
        }

        private String upstreamId() {
            return upstreamId;
        }

        private boolean hasUpstreamBody() {
            return upstreamResponse;
        }

        private boolean bodyAllowed() {
            return bodyAllowed;
        }

        private byte[] copyBuffer() {
            return copyBuffer;
        }

        private int prefetchedBytes() {
            return prefetchedBytes;
        }

        private ReverseProxyProperties properties() {
            return properties;
        }
    }

    private static final class UpstreamResponseStreamingException extends IOException {
        private UpstreamResponseStreamingException(String message) {
            super(message);
        }

        private UpstreamResponseStreamingException(String message, Throwable cause) {
            super(message, cause);
        }
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
