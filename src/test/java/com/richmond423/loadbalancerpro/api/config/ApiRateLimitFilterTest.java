package com.richmond423.loadbalancerpro.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiRateLimitFilterTest {
    private static final Duration REFILL_PERIOD = Duration.ofHours(1);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void disabledLimiterPreservesLocalDemoConvenience() throws Exception {
        ApiRateLimitFilter filter = filter(false, 1, mutableClock());
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(sensitivePost("/api/allocate/evaluate"), new MockHttpServletResponse(),
                (ServletRequest request, ServletResponse response) -> chainCalls.incrementAndGet());
        filter.doFilter(sensitivePost("/api/allocate/evaluate"), new MockHttpServletResponse(),
                (ServletRequest request, ServletResponse response) -> chainCalls.incrementAndGet());

        assertEquals(2, chainCalls.get(), "Disabled app limiter must not throttle local/default demo traffic.");
    }

    @Test
    void enabledLimiterReturns429ForRepeatedSensitiveEndpointBurst() throws Exception {
        ApiRateLimitFilter filter = filter(true, 1, mutableClock());
        AtomicInteger chainCalls = new AtomicInteger();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        filter.doFilter(sensitivePost("/api/allocate/evaluate"), firstResponse,
                (ServletRequest request, ServletResponse response) -> chainCalls.incrementAndGet());
        filter.doFilter(sensitivePost("/api/allocate/evaluate"), secondResponse,
                (ServletRequest request, ServletResponse response) -> chainCalls.incrementAndGet());

        assertEquals(1, chainCalls.get());
        assertEquals(200, firstResponse.getStatus());
        assertRateLimited(secondResponse, "/api/allocate/evaluate", 3600);
    }

    @Test
    void healthAndOptionsRemainOutsideTheOptionalLimiter() throws Exception {
        ApiRateLimitFilter filter = filter(true, 1, mutableClock());
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(request("GET", "/api/health"), new MockHttpServletResponse(),
                (ServletRequest request, ServletResponse response) -> chainCalls.incrementAndGet());
        filter.doFilter(request("GET", "/api/health"), new MockHttpServletResponse(),
                (ServletRequest request, ServletResponse response) -> chainCalls.incrementAndGet());
        filter.doFilter(request("OPTIONS", "/api/allocate/evaluate"), new MockHttpServletResponse(),
                (ServletRequest request, ServletResponse response) -> chainCalls.incrementAndGet());

        assertEquals(3, chainCalls.get());
    }

    @Test
    void bucketRefillsAfterConfiguredPeriod() throws Exception {
        MutableClock clock = mutableClock();
        ApiRateLimitFilter filter = filter(true, 1, clock);
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(sensitivePost("/api/routing/compare"), new MockHttpServletResponse(),
                (ServletRequest request, ServletResponse response) -> chainCalls.incrementAndGet());
        MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
        filter.doFilter(sensitivePost("/api/routing/compare"), limitedResponse,
                (ServletRequest request, ServletResponse response) -> chainCalls.incrementAndGet());

        clock.advance(REFILL_PERIOD);
        MockHttpServletResponse refilledResponse = new MockHttpServletResponse();
        filter.doFilter(sensitivePost("/api/routing/compare"), refilledResponse,
                (ServletRequest request, ServletResponse response) -> chainCalls.incrementAndGet());

        assertRateLimited(limitedResponse, "/api/routing/compare", 3600);
        assertEquals(2, chainCalls.get());
        assertEquals(200, refilledResponse.getStatus());
    }

    @Test
    void forwardedForIsIgnoredUnlessExplicitlyTrusted() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(objectMapper, true, 1, 1, REFILL_PERIOD,
                false, mutableClock());
        AtomicBoolean chainReached = new AtomicBoolean(false);
        MockHttpServletRequest request = sensitivePost("/api/proxy/status");
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.99");

        filter.doFilter(request, new MockHttpServletResponse(),
                (ServletRequest servletRequest, ServletResponse servletResponse) -> chainReached.set(true));

        assertTrue(chainReached.get());
    }

    private ApiRateLimitFilter filter(boolean enabled, int capacity, Clock clock) {
        return new ApiRateLimitFilter(objectMapper, enabled, capacity, 1, REFILL_PERIOD, false, clock);
    }

    private static MockHttpServletRequest sensitivePost(String path) {
        return request("POST", path);
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private void assertRateLimited(MockHttpServletResponse response, String path, int retryAfterSeconds)
            throws Exception {
        assertEquals(429, response.getStatus());
        assertEquals(Integer.toString(retryAfterSeconds), response.getHeader("Retry-After"));
        Map<String, Object> body = objectMapper.readValue(response.getContentAsString(),
                new TypeReference<>() {});
        assertEquals(429, body.get("status"));
        assertEquals("rate_limited", body.get("error"));
        assertEquals(path, body.get("path"));
        assertTrue(((String) body.get("message")).contains("retry after"));
        assertFalse(response.getContentAsString().contains("X-API-Key"));
    }

    private static MutableClock mutableClock() {
        return new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
