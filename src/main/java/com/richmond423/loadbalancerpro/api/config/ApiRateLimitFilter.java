package com.richmond423.loadbalancerpro.api.config;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.api.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER + 2)
public class ApiRateLimitFilter extends OncePerRequestFilter {
    private static final List<String> API_RATE_LIMITED_PREFIXES = List.of(
            "/api/allocate/",
            "/api/routing/",
            "/api/scenarios/replay",
            "/api/remediation",
            "/api/proxy/",
            "/api/lase/shadow");
    private static final String PROXY_SURFACE = "proxy";

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int capacity;
    private final int refillTokens;
    private final long refillPeriodMillis;
    private final boolean trustForwardedFor;
    private final Clock clock;
    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Autowired
    public ApiRateLimitFilter(ObjectMapper objectMapper,
                              @Value("${loadbalancerpro.api.rate-limit.enabled:false}") boolean enabled,
                              @Value("${loadbalancerpro.api.rate-limit.capacity:60}") int capacity,
                              @Value("${loadbalancerpro.api.rate-limit.refill-tokens:60}") int refillTokens,
                              @Value("${loadbalancerpro.api.rate-limit.refill-period:PT1M}")
                              Duration refillPeriod,
                              @Value("${loadbalancerpro.api.rate-limit.trust-forwarded-for:false}")
                              boolean trustForwardedFor) {
        this(objectMapper, enabled, capacity, refillTokens, refillPeriod, trustForwardedFor, Clock.systemUTC());
    }

    ApiRateLimitFilter(ObjectMapper objectMapper, boolean enabled, int capacity, int refillTokens,
                       Duration refillPeriod, boolean trustForwardedFor, Clock clock) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.capacity = Math.max(1, capacity);
        this.refillTokens = Math.max(1, refillTokens);
        this.refillPeriodMillis = Math.max(1L, refillPeriod.toMillis());
        this.trustForwardedFor = trustForwardedFor;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled || !isRateLimitedSurface(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        long nowMillis = clock.millis();
        TokenBucket bucket = buckets.computeIfAbsent(bucketKey(request),
                ignored -> new TokenBucket(capacity, refillTokens, refillPeriodMillis, nowMillis));
        if (bucket.tryConsume(nowMillis)) {
            filterChain.doFilter(request, response);
            return;
        }

        writeRateLimited(request, response, bucket.retryAfterSeconds(nowMillis));
    }

    private boolean isRateLimitedSurface(HttpServletRequest request) {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return false;
        }
        String uri = request.getRequestURI();
        if ("/api/health".equals(uri)) {
            return false;
        }
        if ("/proxy".equals(uri) || uri.startsWith("/proxy/")) {
            return true;
        }
        return API_RATE_LIMITED_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    private String bucketKey(HttpServletRequest request) {
        return clientKey(request) + "|" + surfaceKey(request);
    }

    private String clientKey(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return "xff:" + sanitizeClientToken(forwardedFor.split(",", 2)[0]);
            }
        }
        String remoteAddress = request.getRemoteAddr();
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "remote:unknown";
        }
        return "remote:" + sanitizeClientToken(remoteAddress);
    }

    private static String sanitizeClientToken(String token) {
        return token.strip().replaceAll("[^A-Za-z0-9:._-]", "_");
    }

    private static String surfaceKey(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if ("/proxy".equals(uri) || uri.startsWith("/proxy/")) {
            return PROXY_SURFACE;
        }
        if (uri.startsWith("/api/allocate/")) {
            return "api-allocate";
        }
        if (uri.startsWith("/api/routing/")) {
            return "api-routing";
        }
        if (uri.startsWith("/api/scenarios/replay")) {
            return "api-scenarios-replay";
        }
        if (uri.startsWith("/api/remediation")) {
            return "api-remediation";
        }
        if (uri.startsWith("/api/proxy/")) {
            return "api-proxy";
        }
        if (uri.startsWith("/api/lase/shadow")) {
            return "api-lase-shadow";
        }
        return "api-other";
    }

    private void writeRateLimited(HttpServletRequest request, HttpServletResponse response,
                                  long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ApiErrorResponse.rateLimited(request.getRequestURI(), retryAfterSeconds));
    }

    private static final class TokenBucket {
        private final int capacity;
        private final int refillTokens;
        private final long refillPeriodMillis;
        private int tokens;
        private long lastRefillMillis;

        private TokenBucket(int capacity, int refillTokens, long refillPeriodMillis, long nowMillis) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillPeriodMillis = refillPeriodMillis;
            this.tokens = capacity;
            this.lastRefillMillis = nowMillis;
        }

        private synchronized boolean tryConsume(long nowMillis) {
            refill(nowMillis);
            if (tokens <= 0) {
                return false;
            }
            tokens--;
            return true;
        }

        private synchronized long retryAfterSeconds(long nowMillis) {
            refill(nowMillis);
            if (tokens > 0) {
                return 0L;
            }
            long nextRefillMillis = lastRefillMillis + refillPeriodMillis;
            long waitMillis = Math.max(1L, nextRefillMillis - nowMillis);
            return Math.max(1L, (waitMillis + 999L) / 1000L);
        }

        private void refill(long nowMillis) {
            if (nowMillis <= lastRefillMillis) {
                return;
            }
            long elapsedPeriods = (nowMillis - lastRefillMillis) / refillPeriodMillis;
            if (elapsedPeriods <= 0) {
                return;
            }
            long addedTokens = elapsedPeriods >= capacity
                    ? capacity
                    : Math.min((long) capacity, elapsedPeriods * refillTokens);
            tokens = (int) Math.min((long) capacity, tokens + addedTokens);
            lastRefillMillis += elapsedPeriods * refillPeriodMillis;
        }
    }
}
