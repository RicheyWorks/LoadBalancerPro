package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

class ReverseProxyLoadSheddingTest {
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(5);

    @Test
    void globalCapRejectsExcessFastAndUpstreamsNeverExceedTheirCaps() throws Exception {
        try (BlockingBackend first = BlockingBackend.start();
             BlockingBackend second = BlockingBackend.start()) {
            ReverseProxyProperties properties = properties(
                    List.of(upstream("first", first.url(), 1), upstream("second", second.url(), 1)), 2);
            ReverseProxyService service = service(properties);
            ExecutorService callers = Executors.newFixedThreadPool(2);
            List<Future<ReverseProxyResponse>> admitted = new ArrayList<>();
            try {
                admitted.add(callers.submit(() -> service.forward(request(null), new byte[0])));
                admitted.add(callers.submit(() -> service.forward(request(null), new byte[0])));
                awaitStarted(2, first, second);

                long startedAt = System.nanoTime();
                ReverseProxyResponse rejected = service.forward(request(null), new byte[0]);
                Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

                assertOverload(rejected, "proxy_concurrency_limit");
                assertTrue(elapsed.compareTo(Duration.ofSeconds(1)) < 0,
                        "global rejection should not wait for an upstream timeout");
                assertEquals(2, service.statusSnapshot().limits().currentInFlight());
                assertEquals(1, service.statusSnapshot().upstreams().get(0).maxInFlight());
            } finally {
                first.release();
                second.release();
                assertSuccessful(admitted);
                callers.shutdownNow();
                service.closeHealthProber();
            }

            assertTrue(first.maxConcurrent() <= 1);
            assertTrue(second.maxConcurrent() <= 1);
        }
    }

    @Test
    void perUpstreamCapsRerouteThenRejectWhenEveryTargetIsFull() throws Exception {
        try (BlockingBackend first = BlockingBackend.start();
             BlockingBackend second = BlockingBackend.start()) {
            ReverseProxyProperties properties = properties(
                    List.of(upstream("first", first.url(), 1), upstream("second", second.url(), 1)), 4);
            ReverseProxyService service = service(properties);
            ExecutorService callers = Executors.newFixedThreadPool(2);
            List<Future<ReverseProxyResponse>> admitted = new ArrayList<>();
            try {
                admitted.add(callers.submit(() -> service.forward(request(null), new byte[0])));
                admitted.add(callers.submit(() -> service.forward(request(null), new byte[0])));
                awaitStarted(2, first, second);

                ReverseProxyResponse rejected = service.forward(request(null), new byte[0]);

                assertOverload(rejected, "proxy_upstream_concurrency_limit");
                assertEquals(2, first.started() + second.started(),
                        "a capacity rejection must not reach either backend");
            } finally {
                first.release();
                second.release();
                assertSuccessful(admitted);
                callers.shutdownNow();
                service.closeHealthProber();
            }

            assertEquals(1, first.maxConcurrent());
            assertEquals(1, second.maxConcurrent());
        }
    }

    @Test
    void criticalRequestsAreShedAfterLowerPrioritiesButCannotExceedStrictCap() throws Exception {
        try (BlockingBackend backend = BlockingBackend.start()) {
            ReverseProxyProperties properties = properties(
                    List.of(upstream("only", backend.url(), 0)), 10);
            properties.getShedding().setEnabled(true);
            properties.getShedding().setSoftUtilizationThreshold(0.60);
            properties.getShedding().setHardUtilizationThreshold(0.80);
            properties.getShedding().setMaxP95LatencyMillis(60_000);
            properties.getShedding().setPriorityHeader("X-Proxy-Priority");
            ReverseProxyService service = service(properties);
            ExecutorService callers = Executors.newFixedThreadPool(10);
            List<Future<ReverseProxyResponse>> admitted = new ArrayList<>();
            try {
                submit(callers, admitted, service, 5, "USER");
                awaitStarted(5, backend);

                assertOverload(service.forward(request("PREFETCH"), new byte[0]), "proxy_load_shed");

                submit(callers, admitted, service, 2, "USER");
                awaitStarted(7, backend);
                assertOverload(service.forward(request("BACKGROUND"), new byte[0]), "proxy_load_shed");
                assertOverload(service.forward(request("USER"), new byte[0]), "proxy_load_shed");
                assertOverload(service.forward(request("caller-invented"), new byte[0]), "proxy_load_shed");

                submit(callers, admitted, service, 3, "CRITICAL");
                awaitStarted(10, backend);
                assertOverload(service.forward(request("CRITICAL"), new byte[0]),
                        "proxy_concurrency_limit");
                assertEquals(10, service.statusSnapshot().limits().currentInFlight());
            } finally {
                backend.release();
                assertSuccessful(admitted);
                callers.shutdownNow();
                service.closeHealthProber();
            }

            assertEquals(10, backend.maxConcurrent());
        }
    }

    @Test
    void adaptiveLimitUsesObservedProxyLatencyFeedback() {
        ReverseProxyProperties properties = properties(
                List.of(upstream("only", "http://127.0.0.1:18081", 0)), 10);
        properties.getLimits().setAdaptive(true);
        properties.getShedding().setMaxP95LatencyMillis(10);
        ProxyAdmissionControl.Policy policy = ProxyAdmissionControl.compile(properties, Clock.systemUTC());
        UpstreamRuntimeStats stats = new UpstreamRuntimeStats(Clock.systemUTC());

        for (int sample = 0; sample < 20; sample++) {
            assertTrue(policy.tryAcquire(request(null), stats).acquired());
            stats.requestCompleted(Duration.ofMillis(50), true);
            policy.requestCompleted(stats);
        }

        ProxyAdmissionControl.Status status = policy.status(stats);
        assertEquals(8, status.effectiveMaxInFlight());
        assertEquals("DECREASE", status.lastAdaptiveAction());
        assertTrue(status.lastAdaptiveReason().contains("p95 latency"));
    }

    @Test
    void limitsAndSheddingPropertiesBindFromOperatorConfiguration() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.ofEntries(
                Map.entry("loadbalancerpro.proxy.limits.max-in-flight", "40"),
                Map.entry("loadbalancerpro.proxy.limits.adaptive", "true"),
                Map.entry("loadbalancerpro.proxy.shedding.enabled", "true"),
                Map.entry("loadbalancerpro.proxy.shedding.soft-utilization-threshold", "0.60"),
                Map.entry("loadbalancerpro.proxy.shedding.hard-utilization-threshold", "0.85"),
                Map.entry("loadbalancerpro.proxy.shedding.priority-header", "X-Proxy-Priority"),
                Map.entry("loadbalancerpro.proxy.shedding.retry-after", "2500ms"),
                Map.entry("loadbalancerpro.proxy.upstreams[0].max-in-flight", "7")));

        ReverseProxyProperties properties = new Binder(source)
                .bind("loadbalancerpro.proxy", Bindable.of(ReverseProxyProperties.class))
                .orElseThrow(() -> new AssertionError("proxy properties did not bind"));

        assertEquals(40, properties.getLimits().getMaxInFlight());
        assertTrue(properties.getLimits().isAdaptive());
        assertTrue(properties.getShedding().isEnabled());
        assertEquals(0.60, properties.getShedding().getSoftUtilizationThreshold());
        assertEquals(0.85, properties.getShedding().getHardUtilizationThreshold());
        assertEquals("X-Proxy-Priority", properties.getShedding().getPriorityHeader());
        assertEquals(Duration.ofMillis(2_500), properties.getShedding().getRetryAfter());
        assertEquals(7, properties.getUpstreams().get(0).getMaxInFlight());
        ProxyAdmissionControl.Policy policy = ProxyAdmissionControl.compile(properties, Clock.systemUTC());
        assertEquals(3, policy.status(new UpstreamRuntimeStats(Clock.systemUTC())).retryAfterSeconds());
    }

    @Test
    void invalidLimitsThresholdsAndPriorityHeadersFailClosed() {
        ReverseProxyProperties negativeGlobal = properties(List.of(upstream(
                "only", "http://127.0.0.1:18081", 0)), -1);
        assertThrows(IllegalStateException.class, () -> service(negativeGlobal));

        ReverseProxyProperties negativeUpstream = properties(List.of(upstream(
                "only", "http://127.0.0.1:18081", -1)), 1);
        assertThrows(IllegalArgumentException.class, () -> service(negativeUpstream));

        ReverseProxyProperties adaptiveWithoutCap = properties(List.of(upstream(
                "only", "http://127.0.0.1:18081", 0)), 0);
        adaptiveWithoutCap.getLimits().setAdaptive(true);
        assertThrows(IllegalStateException.class, () -> service(adaptiveWithoutCap));

        ReverseProxyProperties sheddingWithoutCap = properties(List.of(upstream(
                "only", "http://127.0.0.1:18081", 0)), 0);
        sheddingWithoutCap.getShedding().setEnabled(true);
        assertThrows(IllegalStateException.class, () -> service(sheddingWithoutCap));

        ReverseProxyProperties invalidThreshold = properties(List.of(upstream(
                "only", "http://127.0.0.1:18081", 0)), 1);
        invalidThreshold.getShedding().setSoftUtilizationThreshold(0.9);
        invalidThreshold.getShedding().setHardUtilizationThreshold(0.8);
        assertThrows(IllegalStateException.class, () -> service(invalidThreshold));

        ReverseProxyProperties forbiddenHeader = properties(List.of(upstream(
                "only", "http://127.0.0.1:18081", 0)), 1);
        forbiddenHeader.getShedding().setPriorityHeader("Connection");
        assertThrows(IllegalStateException.class, () -> service(forbiddenHeader));
    }

    @Test
    void reloadAtomicallyReplacesLimitsAndRetainsPriorPolicyAfterValidationFailure() {
        ReverseProxyProperties initial = properties(List.of(upstream(
                "only", "http://127.0.0.1:18081", 1)), 1);
        ReverseProxyService service = service(initial);
        try {
            ReverseProxyProperties replacement = properties(List.of(upstream(
                    "only", "http://127.0.0.1:18081", 2)), 2);
            assertTrue(service.reload(replacement).success());
            assertEquals(2, service.statusSnapshot().limits().configuredMaxInFlight());
            assertEquals(2, service.statusSnapshot().upstreams().get(0).maxInFlight());

            ReverseProxyProperties invalid = properties(List.of(upstream(
                    "only", "http://127.0.0.1:18081", 2)), -1);
            assertFalse(service.reload(invalid).success());
            assertEquals(2, service.statusSnapshot().limits().configuredMaxInFlight());
            assertEquals(2, service.statusSnapshot().upstreams().get(0).maxInFlight());
        } finally {
            service.closeHealthProber();
        }
    }

    private static void submit(ExecutorService callers,
                               List<Future<ReverseProxyResponse>> futures,
                               ReverseProxyService service,
                               int count,
                               String priority) {
        for (int index = 0; index < count; index++) {
            futures.add(callers.submit(() -> service.forward(request(priority), new byte[0])));
        }
    }

    private static void assertOverload(ReverseProxyResponse response, String expectedError) {
        assertEquals(503, response.statusCode());
        assertEquals("1", response.headers().getFirst(HttpHeaders.RETRY_AFTER));
        assertTrue(new String(response.body(), StandardCharsets.UTF_8)
                .contains("\"error\":\"" + expectedError + "\""));
    }

    private static void assertSuccessful(List<Future<ReverseProxyResponse>> futures) throws Exception {
        for (Future<ReverseProxyResponse> future : futures) {
            assertEquals(200, future.get(AWAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).statusCode());
        }
    }

    private static void awaitStarted(int expected, BlockingBackend... backends) throws InterruptedException {
        long deadline = System.nanoTime() + AWAIT_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            int started = 0;
            for (BlockingBackend backend : backends) {
                started += backend.started();
            }
            if (started >= expected) {
                return;
            }
            Thread.sleep(10);
        }
        int observed = 0;
        for (BlockingBackend backend : backends) {
            observed += backend.started();
        }
        throw new AssertionError("expected " + expected + " backend requests but observed " + observed);
    }

    private static ReverseProxyService service(ReverseProxyProperties properties) {
        return new ReverseProxyService(
                properties,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.systemUTC());
    }

    private static ReverseProxyProperties properties(List<ReverseProxyProperties.Upstream> upstreams,
                                                     int globalMaxInFlight) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setRequestTimeout(Duration.ofSeconds(5));
        properties.setStrategy("ROUND_ROBIN");
        properties.setUpstreams(upstreams);
        properties.getLimits().setMaxInFlight(globalMaxInFlight);
        return properties;
    }

    private static ReverseProxyProperties.Upstream upstream(String id, String url, int maxInFlight) {
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId(id);
        upstream.setUrl(url);
        upstream.setMaxInFlight(maxInFlight);
        return upstream;
    }

    private static MockHttpServletRequest request(String priority) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("");
        request.setRequestURI("/proxy/load");
        request.setMethod("GET");
        request.setRemoteAddr("127.0.0.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(80);
        request.addHeader("Host", "localhost");
        if (priority != null) {
            request.addHeader("X-Proxy-Priority", priority);
        }
        return request;
    }

    private static final class BlockingBackend implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicInteger started = new AtomicInteger();
        private final AtomicInteger current = new AtomicInteger();
        private final AtomicInteger maxConcurrent = new AtomicInteger();

        private BlockingBackend(HttpServer server, ExecutorService executor) {
            this.server = server;
            this.executor = executor;
        }

        static BlockingBackend start() {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                ExecutorService executor = Executors.newCachedThreadPool();
                BlockingBackend backend = new BlockingBackend(server, executor);
                server.createContext("/", backend::handle);
                server.setExecutor(executor);
                server.start();
                return backend;
            } catch (IOException exception) {
                throw new IllegalStateException("failed to start loopback blocking backend", exception);
            }
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        int started() {
            return started.get();
        }

        int maxConcurrent() {
            return maxConcurrent.get();
        }

        void release() {
            release.countDown();
        }

        private void handle(HttpExchange exchange) throws IOException {
            started.incrementAndGet();
            int active = current.incrementAndGet();
            maxConcurrent.accumulateAndGet(active, Math::max);
            try {
                if (!release.await(AWAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new IOException("test backend release timed out");
                }
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().close();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("test backend interrupted", exception);
            } finally {
                current.decrementAndGet();
                exchange.close();
            }
        }

        @Override
        public void close() {
            release();
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
