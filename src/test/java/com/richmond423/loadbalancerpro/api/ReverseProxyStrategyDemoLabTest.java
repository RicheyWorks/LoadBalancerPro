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
class ReverseProxyRoundRobinStrategyDemoLabTest {
    private static final ProxyStrategyDemoUpstream BACKEND_A =
            ProxyStrategyDemoUpstream.start("backend-a", 200);
    private static final ProxyStrategyDemoUpstream BACKEND_B =
            ProxyStrategyDemoUpstream.start("backend-b", 200);

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        ProxyStrategyDemoSupport.registerCommonProxyProperties(registry, "ROUND_ROBIN", BACKEND_A, BACKEND_B);
    }

    @AfterAll
    static void stopUpstreams() {
        BACKEND_A.stop();
        BACKEND_B.stop();
    }

    @Test
    void roundRobinForwardsRealHttpTrafficToBothBackends() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            expectForward("/proxy/demo?step=1", "backend-a", "ROUND_ROBIN");
            expectForward("/proxy/demo?step=2", "backend-b", "ROUND_ROBIN");
            expectForward("/proxy/demo?step=3", "backend-a", "ROUND_ROBIN");
            expectForward("/proxy/demo?step=4", "backend-b", "ROUND_ROBIN");

            mockMvc.perform(get("/api/proxy/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.strategy").value("ROUND_ROBIN"))
                    .andExpect(jsonPath("$.metrics.totalForwarded").value(4))
                    .andExpect(jsonPath("$.metrics.statusClassCounts['2xx']").value(4))
                    .andExpect(jsonPath("$.metrics.upstreams[0].upstreamId").value("backend-a"))
                    .andExpect(jsonPath("$.metrics.upstreams[0].forwarded").value(2))
                    .andExpect(jsonPath("$.metrics.upstreams[1].upstreamId").value("backend-b"))
                    .andExpect(jsonPath("$.metrics.upstreams[1].forwarded").value(2));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Strategy demo proxy traffic must not construct CloudManager.");
        }
    }

    private void expectForward(String path, String upstream, String strategy) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", upstream))
                .andExpect(header().string("X-LoadBalancerPro-Strategy", strategy))
                .andExpect(header().string("X-Fixture-Upstream", upstream))
                .andExpect(content().string(containsString(upstream + " GET " + path.substring("/proxy".length()))));
    }
}

@SpringBootTest
@AutoConfigureMockMvc
class ReverseProxyWeightedRoundRobinStrategyDemoLabTest {
    private static final ProxyStrategyDemoUpstream BACKEND_A =
            ProxyStrategyDemoUpstream.start("backend-a", 200);
    private static final ProxyStrategyDemoUpstream BACKEND_B =
            ProxyStrategyDemoUpstream.start("backend-b", 200);

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        ProxyStrategyDemoSupport.registerCommonProxyProperties(
                registry, "WEIGHTED_ROUND_ROBIN", BACKEND_A, BACKEND_B);
        registry.add("loadbalancerpro.proxy.upstreams[0].weight", () -> "3.0");
        registry.add("loadbalancerpro.proxy.upstreams[1].weight", () -> "1.0");
    }

    @AfterAll
    static void stopUpstreams() {
        BACKEND_A.stop();
        BACKEND_B.stop();
    }

    @Test
    void weightedRoundRobinUsesConfiguredWeightsForRealHttpTraffic() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            expectForward("/proxy/weighted?step=1", "backend-a", "WEIGHTED_ROUND_ROBIN");
            expectForward("/proxy/weighted?step=2", "backend-a", "WEIGHTED_ROUND_ROBIN");
            expectForward("/proxy/weighted?step=3", "backend-b", "WEIGHTED_ROUND_ROBIN");
            expectForward("/proxy/weighted?step=4", "backend-a", "WEIGHTED_ROUND_ROBIN");

            mockMvc.perform(get("/api/proxy/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.strategy").value("WEIGHTED_ROUND_ROBIN"))
                    .andExpect(jsonPath("$.metrics.totalForwarded").value(4))
                    .andExpect(jsonPath("$.metrics.statusClassCounts['2xx']").value(4))
                    .andExpect(jsonPath("$.metrics.lastSelectedUpstream").value("backend-a"))
                    .andExpect(jsonPath("$.metrics.upstreams[0].upstreamId").value("backend-a"))
                    .andExpect(jsonPath("$.metrics.upstreams[0].forwarded").value(3))
                    .andExpect(jsonPath("$.metrics.upstreams[1].upstreamId").value("backend-b"))
                    .andExpect(jsonPath("$.metrics.upstreams[1].forwarded").value(1));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Weighted proxy strategy demo traffic must not construct CloudManager.");
        }
    }

    private void expectForward(String path, String upstream, String strategy) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", upstream))
                .andExpect(header().string("X-LoadBalancerPro-Strategy", strategy))
                .andExpect(header().string("X-Fixture-Upstream", upstream))
                .andExpect(content().string(containsString(upstream + " GET " + path.substring("/proxy".length()))));
    }
}

@SpringBootTest
@AutoConfigureMockMvc
class ReverseProxyHealthAwareFailoverStrategyDemoLabTest {
    private static final ProxyStrategyDemoUpstream BACKEND_A =
            ProxyStrategyDemoUpstream.start("backend-a", 200);
    private static final ProxyStrategyDemoUpstream BACKEND_B =
            ProxyStrategyDemoUpstream.start("backend-b", 503);

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        ProxyStrategyDemoSupport.registerCommonProxyProperties(registry, "ROUND_ROBIN", BACKEND_A, BACKEND_B);
        registry.add("loadbalancerpro.proxy.health-check.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.health-check.path", () -> "/health");
        registry.add("loadbalancerpro.proxy.health-check.interval", () -> "0s");
        registry.add("loadbalancerpro.proxy.health-check.timeout", () -> "1s");
    }

    @AfterAll
    static void stopUpstreams() {
        BACKEND_A.stop();
        BACKEND_B.stop();
    }

    @Test
    void healthAwareFailoverSkipsFailingBackendForRealHttpTraffic() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(get("/proxy/failover?step=1"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-LoadBalancerPro-Upstream", "backend-a"))
                    .andExpect(header().string("X-LoadBalancerPro-Strategy", "ROUND_ROBIN"))
                    .andExpect(content().string(containsString("backend-a GET /failover?step=1")));

            mockMvc.perform(get("/api/proxy/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.strategy").value("ROUND_ROBIN"))
                    .andExpect(jsonPath("$.healthCheck.enabled").value(true))
                    .andExpect(jsonPath("$.upstreams[0].id").value("backend-a"))
                    .andExpect(jsonPath("$.upstreams[0].effectiveHealthy").value(true))
                    .andExpect(jsonPath("$.upstreams[1].id").value("backend-b"))
                    .andExpect(jsonPath("$.upstreams[1].configuredHealthy").value(true))
                    .andExpect(jsonPath("$.upstreams[1].effectiveHealthy").value(false))
                    .andExpect(jsonPath("$.upstreams[1].lastProbeStatusCode").value(503))
                    .andExpect(jsonPath("$.metrics.totalForwarded").value(1))
                    .andExpect(jsonPath("$.metrics.upstreams[0].forwarded").value(1))
                    .andExpect(jsonPath("$.metrics.upstreams[1].forwarded").value(0));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Health-aware proxy failover demo traffic must not construct CloudManager.");
        }
    }
}

final class ProxyStrategyDemoSupport {
    private ProxyStrategyDemoSupport() {
    }

    static void registerCommonProxyProperties(DynamicPropertyRegistry registry,
                                              String strategy,
                                              ProxyStrategyDemoUpstream backendA,
                                              ProxyStrategyDemoUpstream backendB) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.strategy", () -> strategy);
        registry.add("loadbalancerpro.proxy.upstreams[0].id", () -> "backend-a");
        registry.add("loadbalancerpro.proxy.upstreams[0].url", backendA::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[0].healthy", () -> "true");
        registry.add("loadbalancerpro.proxy.upstreams[1].id", () -> "backend-b");
        registry.add("loadbalancerpro.proxy.upstreams[1].url", backendB::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[1].healthy", () -> "true");
    }
}

final class ProxyStrategyDemoUpstream {
    private final String id;
    private final int healthStatus;
    private final HttpServer server;
    private final ExecutorService executor;

    private ProxyStrategyDemoUpstream(String id, int healthStatus, HttpServer server, ExecutorService executor) {
        this.id = id;
        this.healthStatus = healthStatus;
        this.server = server;
        this.executor = executor;
    }

    static ProxyStrategyDemoUpstream start(String id, int healthStatus) {
        try {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            ProxyStrategyDemoUpstream upstream = new ProxyStrategyDemoUpstream(id, healthStatus, server, executor);
            server.createContext("/", upstream::handle);
            server.setExecutor(executor);
            server.start();
            return upstream;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    void stop() {
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
        exchange.getResponseHeaders().set("X-Fixture-Upstream", id);
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
