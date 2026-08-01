package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class ReverseProxyRetryBudgetSlowStartTest {
    private static final Instant START = Instant.parse("2026-08-01T00:00:00Z");

    @Test
    void brownoutRetriesAreCappedAtTwentyPercentOfPrimaryRequests() throws Exception {
        AtomicInteger upstreamCalls = new AtomicInteger();
        HttpClient client = clientReturning(503, upstreamCalls);
        ReverseProxyProperties properties = properties("ROUND_ROBIN", List.of(alpha(), beta()));
        configureRetry(properties, 20);
        ReverseProxyService service = service(properties, client, new MutableClock(START));

        for (int requestNumber = 0; requestNumber < 100; requestNumber++) {
            assertEquals(503, service.forward(request(), new byte[0]).statusCode());
        }

        ReverseProxyStatusResponse.RetryStatus retry = service.statusSnapshot().retry();
        assertEquals(120, upstreamCalls.get());
        assertEquals(100, retry.budgetPrimaryRequests());
        assertEquals(20, retry.budgetGrantedRetries());
        assertEquals(80, retry.budgetRejectedRetries());
        assertEquals(20, service.statusSnapshot().metrics().totalRetryAttempts());

        RecentProxyDecisionsResponse decisions = service.recentDecisionsSnapshot();
        assertEquals(100, decisions.maxRetained());
        assertEquals(100, decisions.retainedCount());
        assertEquals(120, decisions.totalCaptured());
        assertEquals(20, decisions.totalDropped());
        assertEquals("proxy-decision-00000021", decisions.decisions().get(0).decisionId());
        assertEquals("proxy-decision-00000120", decisions.decisions().get(99).decisionId());
        assertTrue(decisions.decisions().stream()
                .anyMatch(decision -> decision.attempt() == 2));
        assertEquals(100, decisions.decisions().stream()
                .filter(decision -> decision.responseStatus() == 503)
                .count());
    }

    @Test
    void newlyAddedUpstreamRampsLinearlyBeforeReceivingItsFullShare() throws Exception {
        AtomicInteger upstreamCalls = new AtomicInteger();
        HttpClient client = clientReturning(200, upstreamCalls);
        MutableClock clock = new MutableClock(START);
        ReverseProxyProperties initial = properties("WEIGHTED_ROUND_ROBIN", List.of(alpha()));
        initial.getSlowStart().setDuration(Duration.ofSeconds(10));
        ReverseProxyService service = service(initial, client, clock);
        clock.advance(Duration.ofSeconds(20));

        ReverseProxyProperties reloaded = properties("WEIGHTED_ROUND_ROBIN", List.of(alpha(), beta()));
        reloaded.getSlowStart().setDuration(Duration.ofSeconds(10));
        assertTrue(service.reload(reloaded).success());

        Map<String, ReverseProxyStatusResponse.UpstreamStatus> atAddition = statusById(service);
        assertEquals(1.0, atAddition.get("alpha").effectiveWeight(), 0.0001);
        assertEquals(0.0, atAddition.get("beta").effectiveWeight(), 0.0001);
        assertTrue(atAddition.get("beta").slowStartActive());

        for (int requestNumber = 0; requestNumber < 30; requestNumber++) {
            assertEquals("alpha", upstream(service.forward(request(), new byte[0])));
        }

        clock.advance(Duration.ofSeconds(5));
        Map<String, ReverseProxyStatusResponse.UpstreamStatus> halfway = statusById(service);
        assertEquals(0.5, halfway.get("beta").effectiveWeight(), 0.0001);
        assertEquals(5_000, halfway.get("beta").slowStartRemainingMillis());

        int alphaSelections = 0;
        int betaSelections = 0;
        for (int requestNumber = 0; requestNumber < 60; requestNumber++) {
            if ("alpha".equals(upstream(service.forward(request(), new byte[0])))) {
                alphaSelections++;
            } else {
                betaSelections++;
            }
        }
        assertEquals(40, alphaSelections);
        assertEquals(20, betaSelections);

        clock.advance(Duration.ofSeconds(5));
        assertFalse(statusById(service).get("beta").slowStartActive());
    }

    @Test
    void successfulReloadPreservesUnchangedUpstreamCooldownState() throws Exception {
        HttpClient client = clientReturning(503, new AtomicInteger());
        MutableClock clock = new MutableClock(START);
        ReverseProxyProperties initial = properties("ROUND_ROBIN", List.of(alpha()));
        configureRetry(initial, 0);
        configureCooldown(initial);
        ReverseProxyService service = service(initial, client, clock);

        assertEquals(503, service.forward(request(), new byte[0]).statusCode());
        ReverseProxyStatusResponse.UpstreamStatus beforeReload = statusById(service).get("alpha");
        assertTrue(beforeReload.cooldownActive());
        assertEquals(1, beforeReload.consecutiveFailures());

        ReverseProxyProperties reloaded = properties("ROUND_ROBIN", List.of(alpha()));
        configureRetry(reloaded, 0);
        configureCooldown(reloaded);
        assertTrue(service.reload(reloaded).success());

        ReverseProxyStatusResponse.UpstreamStatus afterReload = statusById(service).get("alpha");
        assertTrue(afterReload.cooldownActive());
        assertEquals(1, afterReload.consecutiveFailures());
        assertEquals(30_000, afterReload.cooldownRemainingMillis());
    }

    @Test
    void cooldownExpiryKeepsHalfFailureMemoryAndStartsRecoveryRamp() {
        ReverseProxyService.ResilienceState state =
                new ReverseProxyService.ResilienceState(START.minusSeconds(60));
        ReverseProxyProperties.Cooldown cooldown = new ReverseProxyProperties.Cooldown();
        cooldown.setEnabled(true);
        cooldown.setConsecutiveFailureThreshold(4);
        cooldown.setDuration(Duration.ofSeconds(10));

        assertFalse(state.recordFailure(START, cooldown));
        assertFalse(state.recordFailure(START, cooldown));
        assertFalse(state.recordFailure(START, cooldown));
        assertTrue(state.recordFailure(START, cooldown));
        assertTrue(state.cooldownActive(START.plusSeconds(9)));

        Instant fiveSecondsIntoRecovery = START.plusSeconds(15);
        assertFalse(state.cooldownActive(fiveSecondsIntoRecovery));
        assertEquals(2, state.consecutiveFailures(fiveSecondsIntoRecovery));
        assertEquals(0.25, state.effectiveWeight(
                1.0, fiveSecondsIntoRecovery, Duration.ofSeconds(20)), 0.0001);
    }

    @Test
    void successfulEarlyCooldownRecoveryStartsRampAtRecoveryTime() {
        ReverseProxyService.ResilienceState state =
                new ReverseProxyService.ResilienceState(START.minusSeconds(60));
        ReverseProxyProperties.Cooldown cooldown = new ReverseProxyProperties.Cooldown();
        cooldown.setEnabled(true);
        cooldown.setConsecutiveFailureThreshold(1);
        cooldown.setDuration(Duration.ofSeconds(30));

        assertTrue(state.recordFailure(START, cooldown));
        Instant recoveredAt = START.plusSeconds(5);
        state.recordSuccess(recoveredAt);

        assertFalse(state.cooldownActive(recoveredAt));
        assertEquals(0, state.consecutiveFailures(recoveredAt));
        assertEquals(0.25, state.effectiveWeight(
                1.0, recoveredAt.plusSeconds(5), Duration.ofSeconds(20)), 0.0001);
    }

    @Test
    void invalidBudgetBackoffAndSlowStartConfigurationFailClosed() throws Exception {
        HttpClient client = clientReturning(200, new AtomicInteger());
        ReverseProxyProperties invalidBudget = properties("ROUND_ROBIN", List.of(alpha()));
        configureRetry(invalidBudget, 101);
        assertThrows(IllegalStateException.class,
                () -> service(invalidBudget, client, new MutableClock(START)));

        ReverseProxyProperties invalidBackoff = properties("ROUND_ROBIN", List.of(alpha()));
        configureRetry(invalidBackoff, 20);
        invalidBackoff.getRetry().getBackoff().setBase(Duration.ofSeconds(2));
        invalidBackoff.getRetry().getBackoff().setMax(Duration.ofSeconds(1));
        assertThrows(IllegalStateException.class,
                () -> service(invalidBackoff, client, new MutableClock(START)));

        ReverseProxyProperties invalidSlowStart = properties("ROUND_ROBIN", List.of(alpha()));
        invalidSlowStart.getSlowStart().setDuration(Duration.ofSeconds(-1));
        assertThrows(IllegalStateException.class,
                () -> service(invalidSlowStart, client, new MutableClock(START)));

        ReverseProxyProperties excessiveSlowStart = properties("ROUND_ROBIN", List.of(alpha()));
        excessiveSlowStart.getSlowStart().setDuration(Duration.ofHours(25));
        assertThrows(IllegalStateException.class,
                () -> service(excessiveSlowStart, client, new MutableClock(START)));
    }

    private static ReverseProxyService service(
            ReverseProxyProperties properties, HttpClient client, Clock clock) {
        return new ReverseProxyService(
                properties,
                client,
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                clock);
    }

    private static HttpClient clientReturning(int statusCode, AtomicInteger calls) throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenAnswer(invocation -> {
                    calls.incrementAndGet();
                    HttpResponse<byte[]> response = mock(HttpResponse.class);
                    when(response.statusCode()).thenReturn(statusCode);
                    when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
                    when(response.body()).thenReturn(new byte[0]);
                    return response;
                });
        return client;
    }

    private static ReverseProxyProperties properties(
            String strategy, List<ReverseProxyProperties.Upstream> upstreams) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        ReverseProxyProperties.Route route = new ReverseProxyProperties.Route();
        route.setPathPrefix("/api");
        route.setStrategy(strategy);
        route.setTargets(upstreams);
        properties.setRoutes(Map.of("api", route));
        return properties;
    }

    private static void configureRetry(ReverseProxyProperties properties, int budgetPercent) {
        ReverseProxyProperties.Retry retry = new ReverseProxyProperties.Retry();
        retry.setEnabled(true);
        retry.setMaxAttempts(2);
        retry.setBudgetPercent(budgetPercent);
        ReverseProxyProperties.Backoff backoff = new ReverseProxyProperties.Backoff();
        backoff.setBase(Duration.ZERO);
        backoff.setMax(Duration.ZERO);
        retry.setBackoff(backoff);
        properties.setRetry(retry);
    }

    private static void configureCooldown(ReverseProxyProperties properties) {
        ReverseProxyProperties.Cooldown cooldown = new ReverseProxyProperties.Cooldown();
        cooldown.setEnabled(true);
        cooldown.setConsecutiveFailureThreshold(1);
        cooldown.setDuration(Duration.ofSeconds(30));
        properties.setCooldown(cooldown);
    }

    private static ReverseProxyProperties.Upstream alpha() {
        return upstream("alpha", "http://127.0.0.1:18081");
    }

    private static ReverseProxyProperties.Upstream beta() {
        return upstream("beta", "http://127.0.0.1:18082");
    }

    private static ReverseProxyProperties.Upstream upstream(String id, String url) {
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId(id);
        upstream.setUrl(url);
        upstream.setWeight(1.0);
        return upstream;
    }

    private static HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/proxy/api/brownout");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getRemoteAddr()).thenReturn("198.51.100.10");
        return request;
    }

    private static String upstream(ReverseProxyResponse response) {
        return response.headers().getFirst("X-LoadBalancerPro-Upstream");
    }

    private static Map<String, ReverseProxyStatusResponse.UpstreamStatus> statusById(
            ReverseProxyService service) {
        return service.statusSnapshot().upstreams().stream().collect(
                java.util.stream.Collectors.toMap(
                        ReverseProxyStatusResponse.UpstreamStatus::id,
                        upstream -> upstream));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
