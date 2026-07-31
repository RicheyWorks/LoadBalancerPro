package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;

class ReverseProxyForwardedHeadersTest {
    @Test
    void stripAndSetReplacesSpoofableHeadersAtLoopbackBackend() {
        try (HeaderBackend backend = HeaderBackend.start()) {
            ReverseProxyProperties properties = legacyProperties(backend.url());
            ReverseProxyService service = service(properties);
            try {
                MockHttpServletRequest request = request("203.0.113.9", "https", "app.example", 443);
                request.addHeader("X-Forwarded-For", "192.0.2.45");
                request.addHeader("X-Forwarded-Proto", "ftp");
                request.addHeader("X-Forwarded-Host", "spoof.example");
                request.addHeader("Forwarded", "for=192.0.2.45;proto=ftp");
                request.addHeader("Connection", "X-Connection-Only");
                request.addHeader("X-Connection-Only", "must-not-forward");
                request.addHeader("X-Application", "kept");

                assertEquals(200, service.forward(request, new byte[0]).statusCode());

                Map<String, List<String>> headers = backend.lastHeaders();
                assertEquals(List.of("203.0.113.9"), headers.get("X-Forwarded-For"));
                assertEquals(List.of("https"), headers.get("X-Forwarded-Proto"));
                assertEquals(List.of("app.example"), headers.get("X-Forwarded-Host"));
                assertEquals(List.of("for=203.0.113.9;proto=https;host=\"app.example\""),
                        headers.get("Forwarded"));
                assertEquals(List.of("kept"), headers.get("X-Application"));
                assertFalse(headers.containsKey("X-Connection-Only"));
            } finally {
                service.closeHealthProber();
            }
        }
    }

    @Test
    void appendTrustsOnlyConfiguredImmediateProxy() {
        try (HeaderBackend backend = HeaderBackend.start()) {
            ReverseProxyProperties properties = legacyProperties(backend.url());
            ReverseProxyProperties.Forwarded forwarded = new ReverseProxyProperties.Forwarded();
            forwarded.setMode("append");
            forwarded.setTrustedProxies(List.of("10.0.0.0/8"));
            properties.setForwarded(forwarded);
            ReverseProxyService service = service(properties);
            try {
                MockHttpServletRequest request = request("10.1.2.3", "http", "proxy.internal", 8080);
                request.addHeader("X-Forwarded-For", "198.51.100.7");
                request.addHeader("X-Forwarded-Proto", "https");
                request.addHeader("X-Forwarded-Host", "client.example");
                request.addHeader("Forwarded", "for=198.51.100.7;proto=https;host=\"client.example\"");

                assertEquals(200, service.forward(request, new byte[0]).statusCode());

                Map<String, List<String>> headers = backend.lastHeaders();
                assertEquals(List.of("198.51.100.7, 10.1.2.3"), headers.get("X-Forwarded-For"));
                assertEquals(List.of("https, http"), headers.get("X-Forwarded-Proto"));
                assertEquals(List.of("client.example, proxy.internal:8080"), headers.get("X-Forwarded-Host"));
                assertEquals(List.of("for=198.51.100.7;proto=https;host=\"client.example\", "
                                + "for=10.1.2.3;proto=http;host=\"proxy.internal:8080\""),
                        headers.get("Forwarded"));
            } finally {
                service.closeHealthProber();
            }
        }
    }

    @Test
    void appendDropsSpoofedChainFromUntrustedPeer() {
        try (HeaderBackend backend = HeaderBackend.start()) {
            ReverseProxyProperties properties = legacyProperties(backend.url());
            ReverseProxyProperties.Forwarded forwarded = new ReverseProxyProperties.Forwarded();
            forwarded.setMode("append");
            forwarded.setTrustedProxies(List.of("10.0.0.0/8"));
            properties.setForwarded(forwarded);
            ReverseProxyService service = service(properties);
            try {
                MockHttpServletRequest request = request("203.0.113.11", "http", "direct.example", 80);
                request.addHeader("X-Forwarded-For", "198.51.100.99");
                request.addHeader("Forwarded", "for=198.51.100.99");

                service.forward(request, new byte[0]);

                assertEquals(List.of("203.0.113.11"), backend.lastHeaders().get("X-Forwarded-For"));
                assertEquals(List.of("for=203.0.113.11;proto=http;host=\"direct.example\""),
                        backend.lastHeaders().get("Forwarded"));
            } finally {
                service.closeHealthProber();
            }
        }
    }

    @Test
    void offStripsForwardingMetadataWithoutSynthesizingReplacement() {
        try (HeaderBackend backend = HeaderBackend.start()) {
            ReverseProxyProperties properties = legacyProperties(backend.url());
            ReverseProxyProperties.Forwarded forwarded = new ReverseProxyProperties.Forwarded();
            forwarded.setMode("off");
            properties.setForwarded(forwarded);
            ReverseProxyService service = service(properties);
            try {
                MockHttpServletRequest request = request("203.0.113.12", "http", "direct.example", 80);
                request.addHeader("X-Forwarded-For", "198.51.100.99");
                request.addHeader("Forwarded", "for=198.51.100.99");

                service.forward(request, new byte[0]);

                assertFalse(backend.lastHeaders().containsKey("X-Forwarded-For"));
                assertFalse(backend.lastHeaders().containsKey("X-Forwarded-Proto"));
                assertFalse(backend.lastHeaders().containsKey("X-Forwarded-Host"));
                assertFalse(backend.lastHeaders().containsKey("Forwarded"));
            } finally {
                service.closeHealthProber();
            }
        }
    }

    @Test
    void forwardedUsesQuotedRfc7239NodeForIpv6Peer() throws Exception {
        try (HeaderBackend backend = HeaderBackend.start()) {
            ReverseProxyService service = service(legacyProperties(backend.url()));
            try {
                String expectedAddress = InetAddress.getByName("2001:db8::42").getHostAddress();

                service.forward(request("2001:db8::42", "https", "ipv6.example", 443), new byte[0]);

                assertEquals(List.of(expectedAddress), backend.lastHeaders().get("X-Forwarded-For"));
                assertEquals(List.of("for=\"[" + expectedAddress
                                + "]\";proto=https;host=\"ipv6.example\""),
                        backend.lastHeaders().get("Forwarded"));
            } finally {
                service.closeHealthProber();
            }
        }
    }

    @Test
    void routeRewritesRemoveThenSetThenAddAtLoopbackBackend() {
        try (HeaderBackend backend = HeaderBackend.start()) {
            ReverseProxyProperties properties = routeProperties(backend.url());
            ReverseProxyProperties.Headers rewrites = new ReverseProxyProperties.Headers();
            rewrites.setRemove(Map.of("X-Remove", true, "X-Forwarded-For", true));
            rewrites.setSet(Map.of("X-Set", "replacement"));
            rewrites.setAdd(Map.of("X-Add", "static"));
            properties.getRoutes().get("api").setHeaders(rewrites);
            ReverseProxyService service = service(properties);
            try {
                MockHttpServletRequest request = request("203.0.113.13", "https", "app.example", 443);
                request.addHeader("X-Remove", "sensitive");
                request.addHeader("X-Set", "caller-value");
                request.addHeader("X-Add", "caller-value");

                service.forward(request, new byte[0]);

                Map<String, List<String>> headers = backend.lastHeaders();
                assertFalse(headers.containsKey("X-Remove"));
                assertFalse(headers.containsKey("X-Forwarded-For"));
                assertEquals(List.of("replacement"), headers.get("X-Set"));
                assertEquals(List.of("caller-value", "static"), headers.get("X-Add"));
            } finally {
                service.closeHealthProber();
            }
        }
    }

    @Test
    void invalidModeCidrsAndRewriteValuesFailClosed() {
        ReverseProxyProperties invalidMode = legacyProperties("http://127.0.0.1:18081");
        invalidMode.getForwarded().setMode("passthrough");
        assertThrows(IllegalStateException.class, () -> service(invalidMode));

        ReverseProxyProperties invalidCidr = legacyProperties("http://127.0.0.1:18081");
        invalidCidr.getForwarded().setTrustedProxies(List.of("trusted.example/24"));
        assertThrows(IllegalStateException.class, () -> service(invalidCidr));

        ReverseProxyProperties invalidRewrite = routeProperties("http://127.0.0.1:18081");
        invalidRewrite.getRoutes().get("api").getHeaders().setSet(Map.of("Host", "forbidden.example"));
        assertThrows(IllegalStateException.class, () -> service(invalidRewrite));

        ReverseProxyProperties controlCharacter = routeProperties("http://127.0.0.1:18081");
        controlCharacter.getRoutes().get("api").getHeaders().setAdd(Map.of("X-Test", "bad\rvalue"));
        assertThrows(IllegalStateException.class, () -> service(controlCharacter));
    }

    @Test
    void trustedRangesSupportLiteralIpv4AndIpv6WithoutDns() throws Exception {
        ProxyRequestHeaders.NetworkRange ipv4 = ProxyRequestHeaders.NetworkRange.parse(
                "10.20.0.0/16", "test.ipv4");
        assertTrue(ipv4.contains(InetAddress.getByName("10.20.4.5").getAddress()));
        assertFalse(ipv4.contains(InetAddress.getByName("10.21.4.5").getAddress()));

        ProxyRequestHeaders.NetworkRange ipv6 = ProxyRequestHeaders.NetworkRange.parse(
                "2001:db8::/32", "test.ipv6");
        assertTrue(ipv6.contains(InetAddress.getByName("2001:db8::42").getAddress()));
        assertFalse(ipv6.contains(InetAddress.getByName("2001:db9::42").getAddress()));
    }

    @Test
    void forwardedAndRewritePropertiesBindFromOperatorConfiguration() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "loadbalancerpro.proxy.forwarded.mode", "append",
                "loadbalancerpro.proxy.forwarded.trusted-proxies[0]", "127.0.0.1/32",
                "loadbalancerpro.proxy.routes.api.headers.add.x-route-add", "static",
                "loadbalancerpro.proxy.routes.api.headers.set.x-route-set", "replacement",
                "loadbalancerpro.proxy.routes.api.headers.remove.x-route-remove", "true"));

        ReverseProxyProperties properties = new Binder(source)
                .bind("loadbalancerpro.proxy", Bindable.of(ReverseProxyProperties.class))
                .orElseThrow(() -> new AssertionError("proxy properties did not bind"));

        assertEquals("append", properties.getForwarded().getMode());
        assertEquals(List.of("127.0.0.1/32"), properties.getForwarded().getTrustedProxies());
        assertEquals("static", properties.getRoutes().get("api").getHeaders().getAdd().get("x-route-add"));
        assertEquals("replacement", properties.getRoutes().get("api").getHeaders().getSet().get("x-route-set"));
        assertEquals(true, properties.getRoutes().get("api").getHeaders().getRemove().get("x-route-remove"));
    }

    @Test
    void reloadAtomicallyReplacesForwardedPolicyAndRouteRewrites() {
        try (HeaderBackend backend = HeaderBackend.start()) {
            ReverseProxyService service = service(routeProperties(backend.url()));
            try {
                ReverseProxyProperties candidate = routeProperties(backend.url());
                candidate.getForwarded().setMode("off");
                candidate.getRoutes().get("api").getHeaders().setSet(Map.of("X-Reloaded", "active"));

                assertTrue(service.reload(candidate).success());
                service.forward(request("203.0.113.20", "https", "app.example", 443), new byte[0]);

                assertEquals(List.of("active"), backend.lastHeaders().get("X-Reloaded"));
                assertFalse(backend.lastHeaders().containsKey("X-Forwarded-For"));
            } finally {
                service.closeHealthProber();
            }
        }
    }

    private static ReverseProxyService service(ReverseProxyProperties properties) {
        return new ReverseProxyService(
                properties,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.systemUTC());
    }

    private static ReverseProxyProperties legacyProperties(String url) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setUpstreams(List.of(upstream("backend", url)));
        return properties;
    }

    private static ReverseProxyProperties routeProperties(String url) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        ReverseProxyProperties.Route route = new ReverseProxyProperties.Route();
        route.setPathPrefix("/api");
        route.setTargets(List.of(upstream("backend", url)));
        properties.setRoutes(Map.of("api", route));
        return properties;
    }

    private static ReverseProxyProperties.Upstream upstream(String id, String url) {
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId(id);
        upstream.setUrl(url);
        return upstream;
    }

    private static MockHttpServletRequest request(String remoteAddress, String scheme, String host, int port) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("");
        request.setRequestURI("/proxy/api/headers");
        request.setMethod("GET");
        request.setRemoteAddr(remoteAddress);
        request.setScheme(scheme);
        request.setServerName(host);
        request.setServerPort(port);
        request.addHeader("Host", port == 80 || port == 443 ? host : host + ":" + port);
        return request;
    }

    private static final class HeaderBackend implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;
        private final AtomicReference<Map<String, List<String>>> headers = new AtomicReference<>(Map.of());

        private HeaderBackend(HttpServer server, ExecutorService executor) {
            this.server = server;
            this.executor = executor;
        }

        static HeaderBackend start() {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                HeaderBackend backend = new HeaderBackend(server, executor);
                server.createContext("/", backend::handle);
                server.setExecutor(executor);
                server.start();
                return backend;
            } catch (IOException exception) {
                throw new IllegalStateException("failed to start loopback header backend", exception);
            }
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        Map<String, List<String>> lastHeaders() {
            return headers.get();
        }

        private void handle(HttpExchange exchange) throws IOException {
            Map<String, List<String>> captured = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            exchange.getRequestHeaders().forEach((name, values) ->
                    captured.put(name, List.copyOf(new ArrayList<>(values))));
            headers.set(Collections.unmodifiableMap(captured));
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        }

        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
