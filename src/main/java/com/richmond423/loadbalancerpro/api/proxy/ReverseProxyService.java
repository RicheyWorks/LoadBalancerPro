package com.richmond423.loadbalancerpro.api.proxy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.richmond423.loadbalancerpro.core.NetworkAwarenessSignal;
import com.richmond423.loadbalancerpro.core.RoutingDecision;
import com.richmond423.loadbalancerpro.core.RoutingStrategy;
import com.richmond423.loadbalancerpro.core.RoutingStrategyId;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import com.richmond423.loadbalancerpro.core.ServerStateVector;
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

    private final ReverseProxyProperties properties;
    private final HttpClient httpClient;
    private final RoutingStrategy strategy;
    private final RoutingStrategyId strategyId;
    private final Clock clock;

    @Autowired
    public ReverseProxyService(ReverseProxyProperties properties, HttpClient httpClient) {
        this(properties, httpClient, RoutingStrategyRegistry.defaultRegistry(), Clock.systemUTC());
    }

    ReverseProxyService(ReverseProxyProperties properties,
                        HttpClient httpClient,
                        RoutingStrategyRegistry registry,
                        Clock clock) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        Objects.requireNonNull(registry, "registry cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.strategyId = RoutingStrategyId.fromName(properties.getStrategy())
                .orElseThrow(() -> new IllegalStateException(
                        "Unsupported proxy routing strategy: " + properties.getStrategy()));
        this.strategy = registry.find(strategyId)
                .orElseThrow(() -> new IllegalStateException(
                        "Proxy routing strategy is not registered: " + strategyId.externalName()));
    }

    ReverseProxyResponse forward(HttpServletRequest request, byte[] requestBody) {
        byte[] body = requestBody == null ? new byte[0] : requestBody.clone();
        if (body.length > properties.getMaxRequestBytes()) {
            return proxyError(HttpStatus.PAYLOAD_TOO_LARGE, "proxy_payload_too_large",
                    "Proxy request body exceeds maximum size of " + properties.getMaxRequestBytes() + " bytes");
        }

        List<UpstreamCandidate> upstreams = configuredUpstreams();
        if (upstreams.isEmpty()) {
            return proxyError(HttpStatus.SERVICE_UNAVAILABLE, "proxy_unavailable",
                    "No proxy upstreams are configured.");
        }

        RoutingDecision decision = strategy.choose(upstreams.stream()
                .map(UpstreamCandidate::state)
                .toList());
        Optional<String> selectedServerId = decision.explanation().chosenServerId();
        if (selectedServerId.isEmpty()) {
            return proxyError(HttpStatus.SERVICE_UNAVAILABLE, "proxy_unavailable",
                    "No healthy proxy upstreams are available.");
        }

        Map<String, UpstreamCandidate> upstreamById = upstreams.stream()
                .collect(Collectors.toMap(candidate -> candidate.state().serverId(), Function.identity()));
        UpstreamCandidate upstream = upstreamById.get(selectedServerId.get());
        if (upstream == null) {
            return proxyError(HttpStatus.SERVICE_UNAVAILABLE, "proxy_unavailable",
                    "Selected proxy upstream is not configured: " + selectedServerId.get());
        }

        try {
            HttpRequest outbound = buildOutboundRequest(request, body, upstream);
            HttpResponse<byte[]> response = httpClient.send(outbound, HttpResponse.BodyHandlers.ofByteArray());
            HttpHeaders responseHeaders = forwardedResponseHeaders(response.headers().map());
            responseHeaders.set(UPSTREAM_HEADER, upstream.state().serverId());
            responseHeaders.set(STRATEGY_HEADER, strategyId.externalName());
            return new ReverseProxyResponse(response.statusCode(), responseHeaders, response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return proxyError(HttpStatus.BAD_GATEWAY, "proxy_upstream_failure",
                    "Proxy forwarding was interrupted while calling upstream " + upstream.state().serverId());
        } catch (IOException | IllegalArgumentException exception) {
            return proxyError(HttpStatus.BAD_GATEWAY, "proxy_upstream_failure",
                    "Proxy could not reach upstream " + upstream.state().serverId());
        }
    }

    private HttpRequest buildOutboundRequest(HttpServletRequest request, byte[] body, UpstreamCandidate upstream) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(targetUri(request, upstream))
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

    private URI targetUri(HttpServletRequest request, UpstreamCandidate upstream) {
        String suffix = proxyPathSuffix(request);
        String query = request.getQueryString();
        String base = upstream.baseUri().toString();
        String separator = base.endsWith("/") || suffix.startsWith("/") ? "" : "/";
        String normalizedBase = base.endsWith("/") && suffix.startsWith("/")
                ? base.substring(0, base.length() - 1)
                : base;
        String target = normalizedBase + separator + suffix + (query == null || query.isBlank() ? "" : "?" + query);
        return URI.create(target);
    }

    private static String proxyPathSuffix(HttpServletRequest request) {
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String requestUri = request.getRequestURI();
        String path = requestUri.startsWith(contextPath) ? requestUri.substring(contextPath.length()) : requestUri;
        if (path.length() <= PROXY_PREFIX.length()) {
            return "/";
        }
        String suffix = path.substring(PROXY_PREFIX.length());
        return suffix.startsWith("/") ? suffix : "/" + suffix;
    }

    private List<UpstreamCandidate> configuredUpstreams() {
        Instant now = Instant.now(clock);
        return properties.getUpstreams().stream()
                .map(upstream -> toCandidate(upstream, now))
                .toList();
    }

    private UpstreamCandidate toCandidate(ReverseProxyProperties.Upstream upstream, Instant timestamp) {
        String id = requireNonBlank(upstream.getId(), "loadbalancerpro.proxy.upstreams[].id");
        URI baseUri = URI.create(requireNonBlank(upstream.getUrl(), "loadbalancerpro.proxy.upstreams[].url"));
        if (!"http".equalsIgnoreCase(baseUri.getScheme()) && !"https".equalsIgnoreCase(baseUri.getScheme())) {
            throw new IllegalArgumentException("Proxy upstream URL must use http or https: " + id);
        }
        ServerStateVector state = new ServerStateVector(
                id,
                upstream.isHealthy(),
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

    private record UpstreamCandidate(URI baseUri, ServerStateVector state) {
    }
}
