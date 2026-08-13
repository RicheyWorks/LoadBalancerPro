package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import example.websocket.LoopbackWebSocketBackend;

class ReverseProxyWebSocketIntegrationTest {
    private static final String API_KEY = "WEBSOCKET_PROXY_TEST_KEY";

    @Test
    void proxiesTextBinaryHeadersQueryAndSubprotocolOverOneBoundedTunnel() throws Exception {
        try (LoopbackWebSocketBackend backend = LoopbackWebSocketBackend.start();
             ConfigurableApplicationContext application = startApplication(backend.baseUrl())) {
            int port = ((WebServerApplicationContext) application).getWebServer().getPort();
            URI proxyUri = URI.create("ws://127.0.0.1:" + port + "/proxy/echo?mode=bridge");

            ExecutionException unauthenticated = assertThrows(ExecutionException.class, () ->
                    HttpClient.newHttpClient()
                            .newWebSocketBuilder()
                            .connectTimeout(Duration.ofSeconds(5))
                            .buildAsync(proxyUri, new CapturingListener())
                            .get(5, TimeUnit.SECONDS));
            WebSocketHandshakeException handshakeFailure = assertInstanceOf(
                    WebSocketHandshakeException.class, unauthenticated.getCause());
            assertEquals(401, handshakeFailure.getResponse().statusCode());

            CapturingListener listener = new CapturingListener();
            WebSocket client = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .header("X-API-Key", API_KEY)
                    .header("X-Test-Bridge", "present")
                    .subprotocols("lbp-test")
                    .connectTimeout(Duration.ofSeconds(5))
                    .buildAsync(proxyUri, listener)
                    .get(5, TimeUnit.SECONDS);

            assertEquals("lbp-test", client.getSubprotocol());
            client.sendText("hello", true).get(5, TimeUnit.SECONDS);
            assertEquals("hello", listener.text().get(5, TimeUnit.SECONDS));

            byte[] payload = "binary".getBytes(StandardCharsets.UTF_8);
            client.sendBinary(ByteBuffer.wrap(payload), true).get(5, TimeUnit.SECONDS);
            assertEquals("binary", listener.binary().get(5, TimeUnit.SECONDS));

            assertEquals("present", backend.testHeader());
            assertTrue(backend.forwardedFor().startsWith("127.0.0.1"));
            assertEquals("mode=bridge", backend.query());
            assertNull(backend.apiKey(), "the proxy authentication credential must not reach the upstream");

            CapturingListener capacityListener = new CapturingListener();
            ExecutionException capacityRejected = assertThrows(ExecutionException.class, () ->
                    HttpClient.newHttpClient()
                            .newWebSocketBuilder()
                            .header("X-API-Key", API_KEY)
                            .subprotocols("lbp-test")
                            .connectTimeout(Duration.ofSeconds(5))
                            .buildAsync(
                                    URI.create("ws://127.0.0.1:" + port + "/proxy/echo"),
                                    capacityListener)
                            .get(5, TimeUnit.SECONDS));
            WebSocketHandshakeException capacityFailure = assertInstanceOf(
                    WebSocketHandshakeException.class, capacityRejected.getCause());
            assertEquals(503, capacityFailure.getResponse().statusCode());

            client.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(5, TimeUnit.SECONDS);
            assertEquals(WebSocket.NORMAL_CLOSURE, listener.closed().get(5, TimeUnit.SECONDS));

            CapturingListener oversizedListener = new CapturingListener();
            WebSocket oversizedClient = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .header("X-API-Key", API_KEY)
                    .subprotocols("lbp-test")
                    .connectTimeout(Duration.ofSeconds(5))
                    .buildAsync(
                            URI.create("ws://127.0.0.1:" + port + "/proxy/echo"),
                            oversizedListener)
                    .get(5, TimeUnit.SECONDS);
            oversizedClient.sendText("oversize", true).get(5, TimeUnit.SECONDS);
            assertEquals(1009, oversizedListener.closed().get(5, TimeUnit.SECONDS));

            MeterRegistry registry = application.getBean(MeterRegistry.class);
            await(() -> registry.find("lbp.proxy.inflight")
                    .tag("route", "echo")
                    .tag("upstream", "echo-backend")
                    .gauge()
                    .value() == 0.0);
            double observedTunnels = registry.find("lbp.proxy.requests")
                    .tag("route", "echo")
                    .tag("upstream", "echo-backend")
                    .counters()
                    .stream()
                    .mapToDouble(counter -> counter.count())
                    .sum();
            assertEquals(2.0, observedTunnels);
        }
    }

    private static void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean(), "condition was not met before timeout");
    }

    private static ConfigurableApplicationContext startApplication(String backendUrl) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("server.address", "127.0.0.1");
        properties.put("server.port", "0");
        properties.put("spring.main.banner-mode", "off");
        properties.put("loadbalancerpro.auth.mode", "api-key");
        properties.put("loadbalancerpro.api.key", API_KEY);
        properties.put("management.endpoints.enabled-by-default", "false");
        properties.put("loadbalancerpro.proxy.enabled", "true");
        properties.put("loadbalancerpro.proxy.websocket.enabled", "true");
        properties.put("loadbalancerpro.proxy.websocket.subprotocols[0]", "lbp-test");
        properties.put("loadbalancerpro.proxy.websocket.max-text-message-bytes", "1024");
        properties.put("loadbalancerpro.proxy.websocket.max-binary-message-bytes", "1024");
        properties.put("loadbalancerpro.proxy.websocket.send-buffer-bytes", "4096");
        properties.put("loadbalancerpro.proxy.routes.echo.path-prefix", "/echo");
        properties.put("loadbalancerpro.proxy.routes.echo.headers.remove.x-api-key", "true");
        properties.put("loadbalancerpro.proxy.routes.echo.targets[0].id", "echo-backend");
        properties.put("loadbalancerpro.proxy.routes.echo.targets[0].url", backendUrl);
        properties.put("loadbalancerpro.proxy.routes.echo.targets[0].max-in-flight", "1");
        String[] arguments = properties.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
        return new SpringApplicationBuilder(LoadBalancerApiApplication.class).run(arguments);
    }

    private static final class CapturingListener implements WebSocket.Listener {
        private final CompletableFuture<String> text = new CompletableFuture<>();
        private final CompletableFuture<String> binary = new CompletableFuture<>();
        private final CompletableFuture<Integer> closed = new CompletableFuture<>();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (last) {
                text.complete(data.toString());
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            if (last) {
                byte[] bytes = new byte[data.remaining()];
                data.get(bytes);
                binary.complete(new String(bytes, StandardCharsets.UTF_8));
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closed.complete(statusCode);
            return CompletableFuture.completedFuture(null);
        }

        private CompletableFuture<String> text() {
            return text;
        }

        private CompletableFuture<String> binary() {
            return binary;
        }

        private CompletableFuture<Integer> closed() {
            return closed;
        }
    }

}
