package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.richmond423.loadbalancerpro.core.CloudManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReverseProxyRetryCooldownTest {
    private static final MutableUpstream FLAKY_BACKEND = MutableUpstream.start("flaky-backend", 503, 200);
    private static final MutableUpstream HEALTHY_BACKEND = MutableUpstream.start("healthy-backend", 200, 200);

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.strategy", () -> "ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.retry.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.retry.max-attempts", () -> "2");
        registry.add("loadbalancerpro.proxy.retry.retry-statuses", () -> "503");
        registry.add("loadbalancerpro.proxy.cooldown.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.cooldown.consecutive-failure-threshold", () -> "1");
        registry.add("loadbalancerpro.proxy.cooldown.duration", () -> "30s");
        registry.add("loadbalancerpro.proxy.cooldown.recover-on-successful-health-check", () -> "true");
        registry.add("loadbalancerpro.proxy.health-check.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.health-check.path", () -> "/health");
        registry.add("loadbalancerpro.proxy.health-check.interval", () -> "0s");
        registry.add("loadbalancerpro.proxy.health-check.timeout", () -> "1s");
        registry.add("loadbalancerpro.proxy.upstreams[0].id", () -> "flaky-backend");
        registry.add("loadbalancerpro.proxy.upstreams[0].url", FLAKY_BACKEND::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[0].healthy", () -> "true");
        registry.add("loadbalancerpro.proxy.upstreams[1].id", () -> "healthy-backend");
        registry.add("loadbalancerpro.proxy.upstreams[1].url", HEALTHY_BACKEND::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[1].healthy", () -> "true");
    }

    @AfterAll
    static void stopUpstreams() {
        FLAKY_BACKEND.stop();
        HEALTHY_BACKEND.stop();
    }

    @Test
    void getRetryUsesAlternateUpstreamAndCooldownRecoversAfterHealthyProbe() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(get("/proxy/resilience"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-LoadBalancerPro-Upstream", "healthy-backend"))
                    .andExpect(content().string("healthy-backend GET /resilience"));

            FLAKY_BACKEND.setHealthStatus(500);

            mockMvc.perform(get("/api/proxy/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.retry.enabled").value(true))
                    .andExpect(jsonPath("$.retry.maxAttempts").value(2))
                    .andExpect(jsonPath("$.cooldown.enabled").value(true))
                    .andExpect(jsonPath("$.metrics.totalForwarded").value(2))
                    .andExpect(jsonPath("$.metrics.totalRetryAttempts").value(1))
                    .andExpect(jsonPath("$.metrics.totalCooldownActivations").value(1))
                    .andExpect(jsonPath("$.metrics.statusClassCounts['2xx']").value(1))
                    .andExpect(jsonPath("$.metrics.statusClassCounts['5xx']").value(1))
                    .andExpect(jsonPath("$.metrics.upstreams[0].upstreamId").value("flaky-backend"))
                    .andExpect(jsonPath("$.metrics.upstreams[0].cooldownActivations").value(1))
                    .andExpect(jsonPath("$.metrics.upstreams[1].upstreamId").value("healthy-backend"))
                    .andExpect(jsonPath("$.metrics.upstreams[1].retryAttempts").value(1))
                    .andExpect(jsonPath("$.upstreams[0].healthSource").value("COOLDOWN"))
                    .andExpect(jsonPath("$.upstreams[0].cooldownActive").value(true))
                    .andExpect(jsonPath("$.upstreams[0].cooldownRemainingMillis", greaterThan(0)));

            mockMvc.perform(get("/proxy/during-cooldown"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-LoadBalancerPro-Upstream", "healthy-backend"))
                    .andExpect(content().string("healthy-backend GET /during-cooldown"));

            assertEquals(1, FLAKY_BACKEND.proxyRequests(),
                    "Cooled-down upstream should not receive another proxied request.");
            assertEquals(2, HEALTHY_BACKEND.proxyRequests(),
                    "Healthy upstream should handle the original retry and the cooldown request.");

            FLAKY_BACKEND.setProxyStatus(200);
            FLAKY_BACKEND.setHealthStatus(200);

            mockMvc.perform(get("/api/proxy/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.upstreams[0].effectiveHealthy").value(true))
                    .andExpect(jsonPath("$.upstreams[0].healthSource").value("ACTIVE_PROBE"))
                    .andExpect(jsonPath("$.upstreams[0].consecutiveFailures").value(0))
                    .andExpect(jsonPath("$.upstreams[0].cooldownActive").value(false))
                    .andExpect(jsonPath("$.upstreams[0].cooldownRemainingMillis").value(0));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Proxy retry and cooldown paths must not construct CloudManager or enter cloud paths.");
        }
    }

    private static final class MutableUpstream {
        private final String id;
        private final AtomicInteger proxyStatus;
        private final AtomicInteger healthStatus;
        private final AtomicInteger proxyRequests = new AtomicInteger();
        private final HttpServer server;
        private final ExecutorService executor;

        private MutableUpstream(String id,
                                int proxyStatus,
                                int healthStatus,
                                HttpServer server,
                                ExecutorService executor) {
            this.id = id;
            this.proxyStatus = new AtomicInteger(proxyStatus);
            this.healthStatus = new AtomicInteger(healthStatus);
            this.server = server;
            this.executor = executor;
        }

        private static MutableUpstream start(String id, int proxyStatus, int healthStatus) {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                MutableUpstream upstream = new MutableUpstream(id, proxyStatus, healthStatus, server, executor);
                server.createContext("/", upstream::handle);
                server.setExecutor(executor);
                server.start();
                return upstream;
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private void setProxyStatus(int status) {
            proxyStatus.set(status);
        }

        private void setHealthStatus(int status) {
            healthStatus.set(status);
        }

        private int proxyRequests() {
            return proxyRequests.get();
        }

        private void stop() {
            server.stop(0);
            executor.shutdownNow();
        }

        private void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if ("/health".equals(path)) {
                byte[] response = (id + " health " + healthStatus.get()).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(healthStatus.get(), response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
                return;
            }
            proxyRequests.incrementAndGet();
            String body = id + " " + exchange.getRequestMethod() + " " + exchange.getRequestURI();
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(proxyStatus.get(), response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }
    }
}
