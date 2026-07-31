package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "loadbalancerpro.auth.mode=none")
@AutoConfigureMockMvc
@DirtiesContext
class ReverseProxyTimeoutIntegrationTest {
    private static final TimeoutFixture UPSTREAM = TimeoutFixture.start();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.connect-timeout", () -> "200ms");
        registry.add("loadbalancerpro.proxy.request-timeout", () -> "800ms");

        registry.add("loadbalancerpro.proxy.routes.short.path-prefix", () -> "/short");
        registry.add("loadbalancerpro.proxy.routes.short.request-timeout", () -> "100ms");
        registry.add("loadbalancerpro.proxy.routes.short.targets[0].id", () -> "blackhole");
        registry.add("loadbalancerpro.proxy.routes.short.targets[0].url", UPSTREAM::baseUrl);

        registry.add("loadbalancerpro.proxy.routes.inherited.path-prefix", () -> "/inherited");
        registry.add("loadbalancerpro.proxy.routes.inherited.targets[0].id", () -> "slow");
        registry.add("loadbalancerpro.proxy.routes.inherited.targets[0].url", UPSTREAM::baseUrl);
    }

    @AfterAll
    static void stopUpstream() {
        UPSTREAM.close();
    }

    @Test
    void routeOverrideBoundsAnAcceptThenBlackholeRequest() throws Exception {
        long started = System.nanoTime();

        mockMvc.perform(get("/proxy/short/blackhole"))
                .andExpect(status().isBadGateway())
                .andExpect(content().string(containsString("\"error\":\"proxy_upstream_failure\"")));

        long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
        assertTrue(elapsedMillis >= 50,
                () -> "timeout returned implausibly early: " + elapsedMillis + "ms");
        assertTrue(elapsedMillis < 1_200,
                () -> "route timeout did not bound the blackhole request: " + elapsedMillis + "ms");
    }

    @Test
    void routeWithoutOverrideInheritsGlobalRequestTimeout() throws Exception {
        long started = System.nanoTime();

        mockMvc.perform(get("/proxy/inherited/slow"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("slow response after 250ms")));

        long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
        assertTrue(elapsedMillis >= 150,
                () -> "slow fixture did not exercise the inherited timeout: " + elapsedMillis + "ms");
        assertTrue(elapsedMillis < 800,
                () -> "inherited global timeout was not observed: " + elapsedMillis + "ms");
    }

    private static final class TimeoutFixture implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;

        private TimeoutFixture(HttpServer server, ExecutorService executor) {
            this.server = server;
            this.executor = executor;
        }

        private static TimeoutFixture start() {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newCachedThreadPool();
                TimeoutFixture fixture = new TimeoutFixture(server, executor);
                server.createContext("/", fixture::handle);
                server.setExecutor(executor);
                server.start();
                return fixture;
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private void handle(HttpExchange exchange) throws IOException {
            try {
                if (exchange.getRequestURI().getPath().endsWith("/blackhole")) {
                    pause(1_000);
                    return;
                }
                pause(250);
                byte[] response = "slow response after 250ms".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } finally {
                exchange.close();
            }
        }

        private static void pause(long millis) throws IOException {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("timeout fixture interrupted", exception);
            }
        }

        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
