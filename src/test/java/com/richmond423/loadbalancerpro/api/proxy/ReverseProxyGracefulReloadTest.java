package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class ReverseProxyGracefulReloadTest {
    private static final Instant START = Instant.parse("2026-08-02T00:00:00Z");

    @Test
    void removedTargetDrainsExistingRequestWhileNewRequestsUseReplacement() throws Exception {
        MutableClock clock = new MutableClock(START);
        CountDownLatch oldRequestStarted = new CountDownLatch(1);
        CountDownLatch releaseOldRequest = new CountDownLatch(1);
        HttpClient client = blockingClient(oldRequestStarted, releaseOldRequest);
        ReverseProxyService service = service(properties(alpha(), Duration.ofSeconds(5)), client, clock);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<ReverseProxyResponse> oldRequest =
                    executor.submit(() -> service.forward(request(), new byte[0]));
            assertTrue(oldRequestStarted.await(2, TimeUnit.SECONDS));

            ReverseProxyReloadResponse reload =
                    service.reload(properties(beta(), Duration.ofSeconds(5)));
            assertTrue(reload.success());
            assertEquals(1, service.drainingUpstreamCountForTesting());

            ReverseProxyResponse replacement = service.forward(request(), new byte[0]);
            assertEquals(200, replacement.statusCode());
            assertEquals("beta", upstream(replacement));
            assertFalse(oldRequest.isDone());

            releaseOldRequest.countDown();
            ReverseProxyResponse completed = oldRequest.get(2, TimeUnit.SECONDS);
            assertEquals(200, completed.statusCode());
            assertEquals("alpha", upstream(completed));
            await(() -> service.drainingUpstreamCountForTesting() == 0, Duration.ofSeconds(2));
        } finally {
            releaseOldRequest.countDown();
            executor.shutdownNow();
            service.stop();
        }

        assertFalse(service.isRunning());
        assertTrue(service.drainSchedulerShutdownForTesting());
    }

    @Test
    void drainTimeoutBoundsRetentionAndIdReuseFailsClosedUntilExpiry() throws Exception {
        MutableClock clock = new MutableClock(START);
        CountDownLatch oldRequestStarted = new CountDownLatch(1);
        CountDownLatch releaseOldRequest = new CountDownLatch(1);
        ReverseProxyService service = service(
                properties(alpha(), Duration.ofSeconds(1)),
                blockingClient(oldRequestStarted, releaseOldRequest),
                clock);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<ReverseProxyResponse> oldRequest =
                    executor.submit(() -> service.forward(request(), new byte[0]));
            assertTrue(oldRequestStarted.await(2, TimeUnit.SECONDS));
            assertTrue(service.reload(properties(beta(), Duration.ofSeconds(1))).success());

            ReverseProxyReloadResponse conflicting =
                    service.reload(properties(alpha(), Duration.ofSeconds(1)));
            assertFalse(conflicting.success());
            assertTrue(conflicting.validationErrors().get(0).contains("while they are draining"));
            assertFalse(conflicting.validationErrors().get(0).contains("http://"));

            clock.advance(Duration.ofSeconds(2));
            service.sweepDrainingUpstreamsForTesting();
            assertEquals(0, service.drainingUpstreamCountForTesting());

            ReverseProxyProperties readded = properties(alpha(), Duration.ofSeconds(1));
            readded.getSlowStart().setDuration(Duration.ofSeconds(10));
            assertTrue(service.reload(readded).success());
            ReverseProxyStatusResponse.UpstreamStatus alpha = service.statusSnapshot().upstreams().get(0);
            assertEquals(0.0, alpha.effectiveWeight(), 0.0001);
            assertTrue(alpha.slowStartActive());

            releaseOldRequest.countDown();
            assertEquals(200, oldRequest.get(2, TimeUnit.SECONDS).statusCode());
            assertEquals(0.0, service.statusSnapshot().upstreams().get(0).effectiveWeight(), 0.0001,
                    "a timed-out prior generation must not mutate the re-added target's slow-start state");
        } finally {
            releaseOldRequest.countDown();
            executor.shutdownNow();
            service.stop();
        }
    }

    @Test
    void requestStartedBeforeReloadDoesNotRetryOntoAnotherRemovedTarget() throws Exception {
        CountDownLatch oldRequestStarted = new CountDownLatch(1);
        CountDownLatch releaseOldRequest = new CountDownLatch(1);
        AtomicInteger removedRetryCalls = new AtomicInteger();
        ReverseProxyProperties initial = properties(
                List.of(alpha(), gamma(), beta()), Duration.ofSeconds(5));
        initial.getRetry().setEnabled(true);
        initial.getRetry().setMaxAttempts(2);
        initial.getRetry().setBudgetPercent(100);
        ReverseProxyService service = service(
                initial,
                drainingRetryClient(oldRequestStarted, releaseOldRequest, removedRetryCalls),
                Clock.systemUTC());
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<ReverseProxyResponse> oldRequest =
                    executor.submit(() -> service.forward(request(), new byte[0]));
            assertTrue(oldRequestStarted.await(2, TimeUnit.SECONDS));
            assertTrue(service.reload(properties(beta(), Duration.ofSeconds(5))).success());

            releaseOldRequest.countDown();
            ReverseProxyResponse completed = oldRequest.get(2, TimeUnit.SECONDS);
            assertEquals(200, completed.statusCode());
            assertEquals("beta", upstream(completed));
            assertEquals(0, removedRetryCalls.get(),
                    "a pre-reload request must not create a new attempt on a draining target");
        } finally {
            releaseOldRequest.countDown();
            executor.shutdownNow();
            service.stop();
        }
    }

    @Test
    void unchangedTargetKeepsRuntimeStatsAndCooldownAcrossReload() throws Exception {
        MutableClock clock = new MutableClock(START);
        ReverseProxyProperties initial = properties(beta(), Duration.ofSeconds(5));
        enableCooldown(initial);
        ReverseProxyService service = service(initial, respondingClient("beta", 503), clock);

        try {
            assertEquals(503, service.forward(request(), new byte[0]).statusCode());
            ReverseProxyStatusResponse.UpstreamStatus before = service.statusSnapshot().upstreams().get(0);
            assertTrue(before.cooldownActive());
            assertEquals(1, before.runtimeStats().recentFailureCount());

            ReverseProxyProperties unchanged = properties(beta(), Duration.ofSeconds(5));
            enableCooldown(unchanged);
            assertTrue(service.reload(unchanged).success());

            ReverseProxyStatusResponse.UpstreamStatus after = service.statusSnapshot().upstreams().get(0);
            assertTrue(after.cooldownActive());
            assertEquals(before.runtimeStats(), after.runtimeStats());
        } finally {
            service.stop();
        }
    }

    @Test
    void accessLogSinkChangesRequireRestartAndLeaveActiveGenerationUntouched() throws Exception {
        ReverseProxyProperties initial = properties(beta(), Duration.ofSeconds(5));
        ReverseProxyService service = service(initial, respondingClient("beta", 200), Clock.systemUTC());

        try {
            long generation = service.statusSnapshot().reload().activeConfigGeneration();
            ReverseProxyProperties candidate = properties(beta(), Duration.ofSeconds(5));
            candidate.getAccessLog().setEnabled(true);
            candidate.getAccessLog().setPath("logs/reconfigured-access.log");

            ReverseProxyReloadResponse response = service.reload(candidate);

            assertFalse(response.success());
            assertTrue(response.validationErrors().get(0).contains(
                    "access-log configuration requires application restart"));
            assertEquals(generation, service.statusSnapshot().reload().activeConfigGeneration());
            assertEquals(200, service.forward(request(), new byte[0]).statusCode());
        } finally {
            service.stop();
        }
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

    private static HttpClient blockingClient(
            CountDownLatch oldRequestStarted, CountDownLatch releaseOldRequest) throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenAnswer(invocation -> {
                    HttpRequest outbound = invocation.getArgument(0);
                    String id = outbound.uri().getPort() == 18081 ? "alpha" : "beta";
                    if ("alpha".equals(id)) {
                        oldRequestStarted.countDown();
                        if (!releaseOldRequest.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("test request was not released");
                        }
                    }
                    HttpResponse<InputStream> response = mock(HttpResponse.class);
                    when(response.statusCode()).thenReturn(200);
                    when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
                    when(response.body()).thenReturn(new ByteArrayInputStream(id.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    return response;
                });
        return client;
    }

    private static HttpClient drainingRetryClient(
            CountDownLatch oldRequestStarted,
            CountDownLatch releaseOldRequest,
            AtomicInteger removedRetryCalls) throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenAnswer(invocation -> {
                    int port = invocation.<HttpRequest>getArgument(0).uri().getPort();
                    String id = port == 18081 ? "alpha" : port == 18083 ? "gamma" : "beta";
                    int status = 200;
                    if ("alpha".equals(id)) {
                        oldRequestStarted.countDown();
                        if (!releaseOldRequest.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("test request was not released");
                        }
                        status = 503;
                    } else if ("gamma".equals(id)) {
                        removedRetryCalls.incrementAndGet();
                    }
                    HttpResponse<InputStream> response = mock(HttpResponse.class);
                    when(response.statusCode()).thenReturn(status);
                    when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
                    when(response.body()).thenReturn(new ByteArrayInputStream(id.getBytes(
                            java.nio.charset.StandardCharsets.UTF_8)));
                    return response;
                });
        return client;
    }

    private static HttpClient respondingClient(String id, int status) throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenAnswer(invocation -> {
                    HttpResponse<InputStream> response = mock(HttpResponse.class);
                    when(response.statusCode()).thenReturn(status);
                    when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
                    when(response.body()).thenReturn(new ByteArrayInputStream(id.getBytes(
                            java.nio.charset.StandardCharsets.UTF_8)));
                    return response;
                });
        return client;
    }

    private static void enableCooldown(ReverseProxyProperties properties) {
        properties.getRetry().setEnabled(true);
        properties.getRetry().setMaxAttempts(1);
        properties.getCooldown().setEnabled(true);
        properties.getCooldown().setConsecutiveFailureThreshold(1);
        properties.getCooldown().setDuration(Duration.ofSeconds(30));
    }

    private static ReverseProxyProperties properties(
            ReverseProxyProperties.Upstream target, Duration drainTimeout) {
        return properties(List.of(target), drainTimeout);
    }

    private static ReverseProxyProperties properties(
            List<ReverseProxyProperties.Upstream> targets, Duration drainTimeout) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.getReload().setDrainTimeout(drainTimeout);
        ReverseProxyProperties.Route route = new ReverseProxyProperties.Route();
        route.setPathPrefix("/api");
        route.setStrategy("ROUND_ROBIN");
        route.setTargets(targets);
        properties.setRoutes(Map.of("api", route));
        return properties;
    }

    private static ReverseProxyProperties.Upstream alpha() {
        return upstream("alpha", "http://127.0.0.1:18081");
    }

    private static ReverseProxyProperties.Upstream beta() {
        return upstream("beta", "http://127.0.0.1:18082");
    }

    private static ReverseProxyProperties.Upstream gamma() {
        return upstream("gamma", "http://127.0.0.1:18083");
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
        when(request.getRequestURI()).thenReturn("/proxy/api/drain");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getRemoteAddr()).thenReturn("198.51.100.10");
        return request;
    }

    private static String upstream(ReverseProxyResponse response) {
        return response.headers().getFirst("X-LoadBalancerPro-Upstream");
    }

    private static void await(BooleanSupplier condition, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertTrue(condition.getAsBoolean(), "condition did not converge within " + timeout);
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private MutableClock(Instant instant) {
            this.instant = new AtomicReference<>(instant);
        }

        void advance(Duration duration) {
            instant.updateAndGet(current -> current.plus(duration));
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
            return instant.get();
        }
    }
}
