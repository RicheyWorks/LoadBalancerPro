package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
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
class ReverseProxyHealthMetricsTest {
    private static final TestUpstream HEALTHY_BACKEND = TestUpstream.start("backend-a", 200);
    private static final TestUpstream FAILING_HEALTH_BACKEND = TestUpstream.start("backend-b", 500);

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.strategy", () -> "ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.health-check.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.health-check.path", () -> "/health");
        registry.add("loadbalancerpro.proxy.health-check.interval", () -> "0s");
        registry.add("loadbalancerpro.proxy.health-check.timeout", () -> "1s");
        registry.add("loadbalancerpro.proxy.upstreams[0].id", () -> "backend-a");
        registry.add("loadbalancerpro.proxy.upstreams[0].url", HEALTHY_BACKEND::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[0].healthy", () -> "true");
        registry.add("loadbalancerpro.proxy.upstreams[1].id", () -> "backend-b");
        registry.add("loadbalancerpro.proxy.upstreams[1].url", FAILING_HEALTH_BACKEND::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[1].healthy", () -> "true");
    }

    @AfterAll
    static void stopUpstreams() {
        HEALTHY_BACKEND.stop();
        FAILING_HEALTH_BACKEND.stop();
    }

    @Test
    void activeHealthChecksSkipFailingBackendAndStatusReportsCounters() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(get("/api/proxy/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.proxyEnabled").value(true))
                    .andExpect(jsonPath("$.healthCheck.enabled").value(true))
                    .andExpect(jsonPath("$.healthCheck.path").value("/health"))
                    .andExpect(jsonPath("$.upstreams[0].id").value("backend-a"))
                    .andExpect(jsonPath("$.upstreams[0].effectiveHealthy").value(true))
                    .andExpect(jsonPath("$.upstreams[0].lastProbeStatusCode").value(200))
                    .andExpect(jsonPath("$.upstreams[1].id").value("backend-b"))
                    .andExpect(jsonPath("$.upstreams[1].configuredHealthy").value(true))
                    .andExpect(jsonPath("$.upstreams[1].effectiveHealthy").value(false))
                    .andExpect(jsonPath("$.upstreams[1].lastProbeStatusCode").value(500))
                    .andExpect(jsonPath("$.upstreams[1].healthSource").value("ACTIVE_PROBE"))
                    .andExpect(jsonPath("$.metrics.totalForwarded").value(0))
                    .andExpect(jsonPath("$.metrics.totalFailures").value(0));

            mockMvc.perform(get("/proxy/live?step=one"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-LoadBalancerPro-Upstream", "backend-a"))
                    .andExpect(header().string("X-LoadBalancerPro-Strategy", "ROUND_ROBIN"))
                    .andExpect(content().string(containsString("backend-a GET /live?step=one")));

            mockMvc.perform(get("/api/proxy/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.upstreams[1].effectiveHealthy").value(false))
                    .andExpect(jsonPath("$.metrics.totalForwarded").value(1))
                    .andExpect(jsonPath("$.metrics.totalFailures").value(0))
                    .andExpect(jsonPath("$.metrics.statusClassCounts['2xx']").value(1))
                    .andExpect(jsonPath("$.metrics.statusClassCounts['5xx']").value(0))
                    .andExpect(jsonPath("$.metrics.lastSelectedUpstream").value("backend-a"))
                    .andExpect(jsonPath("$.metrics.upstreams[0].upstreamId").value("backend-a"))
                    .andExpect(jsonPath("$.metrics.upstreams[0].forwarded").value(1))
                    .andExpect(jsonPath("$.metrics.upstreams[1].upstreamId").value("backend-b"))
                    .andExpect(jsonPath("$.metrics.upstreams[1].forwarded").value(0));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Proxy health checks and metrics must not construct CloudManager or enter cloud paths.");
        }
    }

    private static final class TestUpstream {
        private final String id;
        private final int healthStatus;
        private final HttpServer server;
        private final ExecutorService executor;

        private TestUpstream(String id, int healthStatus, HttpServer server, ExecutorService executor) {
            this.id = id;
            this.healthStatus = healthStatus;
            this.server = server;
            this.executor = executor;
        }

        private static TestUpstream start(String id, int healthStatus) {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                TestUpstream upstream = new TestUpstream(id, healthStatus, server, executor);
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

        private void stop() {
            server.stop(0);
            executor.shutdownNow();
        }

        private void handle(HttpExchange exchange) throws IOException {
            if ("/health".equals(exchange.getRequestURI().getPath())) {
                byte[] response = (id + " health " + healthStatus).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(healthStatus, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
                return;
            }
            String body = id + " " + exchange.getRequestMethod() + " " + exchange.getRequestURI();
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }
    }
}
