package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EnterpriseProxyRouteIntegrationTest {
    private static final TestUpstream BACKEND_A = TestUpstream.start("local-a");
    private static final TestUpstream BACKEND_B = TestUpstream.start("local-b");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.routes.api.path-prefix", () -> "/api");
        registry.add("loadbalancerpro.proxy.routes.api.strategy", () -> "ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.routes.api.targets[0].id", () -> "local-a");
        registry.add("loadbalancerpro.proxy.routes.api.targets[0].url", BACKEND_A::baseUrl);
        registry.add("loadbalancerpro.proxy.routes.api.targets[0].weight", () -> "1");
        registry.add("loadbalancerpro.proxy.routes.api.targets[1].id", () -> "local-b");
        registry.add("loadbalancerpro.proxy.routes.api.targets[1].url", BACKEND_B::baseUrl);
        registry.add("loadbalancerpro.proxy.routes.api.targets[1].weight", () -> "1");
    }

    @AfterAll
    static void stopUpstreams() {
        BACKEND_A.stop();
        BACKEND_B.stop();
    }

    @Test
    void operatorConfiguredRouteForwardsLoopbackTrafficAndAppearsInStatus() throws Exception {
        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proxyEnabled").value(true))
                .andExpect(jsonPath("$.routes[0].name").value("api"))
                .andExpect(jsonPath("$.routes[0].pathPrefix").value("/api"))
                .andExpect(jsonPath("$.routes[0].strategy").value("ROUND_ROBIN"))
                .andExpect(jsonPath("$.routes[0].targetIds[0]").value("local-a"))
                .andExpect(jsonPath("$.routes[0].targetIds[1]").value("local-b"))
                .andExpect(jsonPath("$.upstreams[0].id").value("local-a"))
                .andExpect(jsonPath("$.upstreams[1].id").value("local-b"));

        mockMvc.perform(get("/proxy/api/orders?source=operator"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", "local-a"))
                .andExpect(header().string("X-LoadBalancerPro-Strategy", "ROUND_ROBIN"))
                .andExpect(content().string(containsString("local-a GET /api/orders?source=operator")));

        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.totalForwarded").value(1))
                .andExpect(jsonPath("$.metrics.upstreams[0].upstreamId").value("local-a"))
                .andExpect(jsonPath("$.metrics.upstreams[0].forwarded").value(1));
    }

    @Test
    void unmatchedOperatorRouteReturnsClearNotFound() throws Exception {
        mockMvc.perform(get("/proxy/admin/status"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("\"error\":\"proxy_route_not_found\"")));
    }

    private static final class TestUpstream {
        private final String id;
        private final HttpServer server;
        private final ExecutorService executor;

        private TestUpstream(String id, HttpServer server, ExecutorService executor) {
            this.id = id;
            this.server = server;
            this.executor = executor;
        }

        private static TestUpstream start(String id) {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                TestUpstream upstream = new TestUpstream(id, server, executor);
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
            String body = id + " " + exchange.getRequestMethod() + " " + exchange.getRequestURI();
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }
    }
}
