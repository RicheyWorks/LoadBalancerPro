package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.richmond423.loadbalancerpro.api.LaseShadowRuntime;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ReverseProxyDnsDiscoveryIntegrationTest {
    @Test
    void resolvesOffThreadAndForwardsWithLiteralAuthorityAndUnchangedRawPath() throws Exception {
        AtomicReference<String> backendHost = new AtomicReference<>();
        AtomicReference<String> backendRawPath = new AtomicReference<>();
        HttpServer backend = HttpServer.create(
                new InetSocketAddress(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 0), 0);
        backend.createContext("/", exchange -> {
            backendHost.set(exchange.getRequestHeaders().getFirst("Host"));
            backendRawPath.set(exchange.getRequestURI().getRawPath());
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        backend.start();

        AtomicReference<String> resolverThread = new AtomicReference<>();
        ReverseProxyProperties properties = discoveredProperties(
                backend.getAddress().getPort(), "/base%2Froot");
        ReverseProxyService service = new ReverseProxyService(
                properties,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.systemUTC(),
                LaseShadowRuntime.disabled(),
                ReverseProxyAccessLog.disabled(),
                name -> {
                    resolverThread.set(Thread.currentThread().getName());
                    return List.of(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}));
                });
        try {
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 1,
                    Duration.ofSeconds(3)));
            assertTrue(resolverThread.get().startsWith(ProxyDnsDiscoveryRuntime.LOOKUP_THREAD_PREFIX));

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/proxy/hello%2Fworld");
            request.setRequestURI("/proxy/hello%2Fworld");
            ReverseProxyResponse response = service.forward(request, new byte[0]);

            assertEquals(204, response.statusCode());
            assertEquals("127.0.0.1:" + backend.getAddress().getPort(), backendHost.get());
            assertEquals("/base%2Froot/hello%2Fworld", backendRawPath.get());
            ReverseProxyStatusResponse.UpstreamStatus upstream = service.statusSnapshot().upstreams().get(0);
            assertTrue(upstream.url().startsWith("http://127.0.0.1:"));
            assertFalse(upstream.url().contains("service.example"));
        } finally {
            service.stop();
            backend.stop(0);
        }
    }

    @Test
    void preservesUnchangedMemberStateAcrossAdditionAndRemovesDepartedMember() throws Exception {
        HttpServer backend = backend(exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        AtomicReference<List<InetAddress>> answers = new AtomicReference<>(
                List.of(literal(127, 0, 0, 1)));
        ReverseProxyProperties properties = discoveredProperties(backend.getAddress().getPort(), "");
        properties.getDnsDiscovery().setTtlFloor(Duration.ofSeconds(1));
        properties.getDnsDiscovery().setStaleAfter(Duration.ofSeconds(5));
        properties.getDnsDiscovery().setResolutionTimeout(Duration.ofMillis(100));
        ReverseProxyService service = service(properties, name -> answers.get());
        try {
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 1,
                    Duration.ofSeconds(3)));
            String unchangedId = service.statusSnapshot().upstreams().get(0).id();
            assertEquals(204, service.forward(request("/proxy/state"), new byte[0]).statusCode());
            assertEquals(1, service.statusSnapshot().upstreams().get(0)
                    .runtimeStats().completedRequestCount());

            answers.set(List.of(literal(127, 0, 0, 1), literal(127, 0, 0, 2)));
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 2,
                    Duration.ofSeconds(3)));
            ReverseProxyStatusResponse.UpstreamStatus unchanged = service.statusSnapshot().upstreams().stream()
                    .filter(status -> status.id().equals(unchangedId))
                    .findFirst().orElseThrow();
            ReverseProxyStatusResponse.UpstreamStatus added = service.statusSnapshot().upstreams().stream()
                    .filter(status -> !status.id().equals(unchangedId))
                    .findFirst().orElseThrow();
            assertEquals(1, unchanged.runtimeStats().completedRequestCount());
            assertEquals(0, added.runtimeStats().completedRequestCount());

            answers.set(List.of(literal(127, 0, 0, 2)));
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 1
                            && !service.statusSnapshot().upstreams().get(0).id().equals(unchangedId),
                    Duration.ofSeconds(3)));
        } finally {
            service.stop();
            backend.stop(0);
        }
    }

    @Test
    void requestUsingOldImmutableSnapshotFinishesAfterMemberRemoval() throws Exception {
        CountDownLatch requestArrived = new CountDownLatch(1);
        CountDownLatch releaseResponse = new CountDownLatch(1);
        HttpServer backend = backend(exchange -> {
            requestArrived.countDown();
            try {
                releaseResponse.await(5, TimeUnit.SECONDS);
                exchange.sendResponseHeaders(204, -1);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        AtomicReference<List<InetAddress>> answers = new AtomicReference<>(
                List.of(literal(127, 0, 0, 1)));
        ReverseProxyProperties properties = discoveredProperties(backend.getAddress().getPort(), "");
        properties.getDnsDiscovery().setTtlFloor(Duration.ofSeconds(1));
        properties.getDnsDiscovery().setStaleAfter(Duration.ofSeconds(5));
        properties.getDnsDiscovery().setResolutionTimeout(Duration.ofMillis(100));
        ReverseProxyService service = service(properties, name -> answers.get());
        ExecutorService requests = Executors.newSingleThreadExecutor();
        try {
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 1,
                    Duration.ofSeconds(3)));
            String removedId = service.statusSnapshot().upstreams().get(0).id();
            Future<ReverseProxyResponse> response = requests.submit(
                    () -> service.forward(request("/proxy/slow"), new byte[0]));
            assertTrue(requestArrived.await(2, TimeUnit.SECONDS));

            answers.set(List.of(literal(127, 0, 0, 2)));
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 1
                            && !service.statusSnapshot().upstreams().get(0).id().equals(removedId),
                    Duration.ofSeconds(3)));
            releaseResponse.countDown();

            assertEquals(204, response.get(2, TimeUnit.SECONDS).statusCode());
        } finally {
            releaseResponse.countDown();
            requests.shutdownNow();
            service.stop();
            backend.stop(0);
        }
    }

    private static ReverseProxyProperties discoveredProperties(int port, String rawBasePath) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.getPrivateNetworkValidation().setEnabled(true);
        ReverseProxyProperties.Upstream target = new ReverseProxyProperties.Upstream();
        target.setId("backend");
        target.setUrl("http://service.example:" + port + rawBasePath);
        target.setDiscovery("dns:service.example:" + port);
        target.setDiscoveryAuthority("address");
        properties.setUpstreams(List.of(target));
        return properties;
    }

    private static ReverseProxyService service(
            ReverseProxyProperties properties, ProxyDnsDiscoveryRuntime.Resolver resolver) {
        return new ReverseProxyService(
                properties,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.systemUTC(),
                LaseShadowRuntime.disabled(),
                ReverseProxyAccessLog.disabled(),
                resolver);
    }

    private static HttpServer backend(com.sun.net.httpserver.HttpHandler handler) throws Exception {
        HttpServer backend = HttpServer.create(
                new InetSocketAddress(literal(127, 0, 0, 1), 0), 0);
        backend.createContext("/", handler);
        backend.start();
        return backend;
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }

    private static InetAddress literal(int... octets) throws Exception {
        byte[] bytes = new byte[octets.length];
        for (int index = 0; index < octets.length; index++) {
            bytes[index] = (byte) octets[index];
        }
        return InetAddress.getByAddress(bytes);
    }

    private static boolean waitUntil(Check check, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!check.evaluate() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        return check.evaluate();
    }

    @FunctionalInterface
    private interface Check {
        boolean evaluate() throws Exception;
    }
}
