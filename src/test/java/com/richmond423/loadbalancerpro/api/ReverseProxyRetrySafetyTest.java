package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReverseProxyRetrySafetyTest {
    private static final MutableUpstream BACKEND_A = MutableUpstream.start("backend-a", 503);
    private static final MutableUpstream BACKEND_B = MutableUpstream.start("backend-b", 200);

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.strategy", () -> "ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.retry.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.retry.max-attempts", () -> "2");
        registry.add("loadbalancerpro.proxy.retry.retry-non-idempotent", () -> "false");
        registry.add("loadbalancerpro.proxy.retry.methods", () -> "GET,HEAD");
        registry.add("loadbalancerpro.proxy.retry.retry-statuses", () -> "503");
        registry.add("loadbalancerpro.proxy.cooldown.enabled", () -> "false");
        registry.add("loadbalancerpro.proxy.upstreams[0].id", () -> "backend-a");
        registry.add("loadbalancerpro.proxy.upstreams[0].url", BACKEND_A::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[0].healthy", () -> "true");
        registry.add("loadbalancerpro.proxy.upstreams[1].id", () -> "backend-b");
        registry.add("loadbalancerpro.proxy.upstreams[1].url", BACKEND_B::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[1].healthy", () -> "true");
    }

    @AfterAll
    static void stopUpstreams() {
        BACKEND_A.stop();
        BACKEND_B.stop();
    }

    @Test
    void postDoesNotRetryByDefaultAndGetAttemptsStayBounded() throws Exception {
        mockMvc.perform(post("/proxy/write")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("side-effect"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", "backend-a"))
                .andExpect(content().string("backend-a POST /write body=side-effect"));

        assertEquals(1, BACKEND_A.proxyRequests(),
                "POST should reach the selected upstream exactly once.");
        assertEquals(0, BACKEND_B.proxyRequests(),
                "POST should not retry to another upstream by default.");

        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retry.enabled").value(true))
                .andExpect(jsonPath("$.retry.retryNonIdempotent").value(false))
                .andExpect(jsonPath("$.metrics.totalRetryAttempts").value(0));

        BACKEND_B.setProxyStatus(503);

        mockMvc.perform(get("/proxy/bounded"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", "backend-a"))
                .andExpect(content().string("backend-a GET /bounded body="));

        assertEquals(2, BACKEND_A.proxyRequests(),
                "Bounded GET retry should make only one additional attempt to backend-a.");
        assertEquals(1, BACKEND_B.proxyRequests(),
                "Bounded GET retry should make only one attempt to backend-b.");

        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.totalForwarded").value(3))
                .andExpect(jsonPath("$.metrics.totalRetryAttempts").value(1))
                .andExpect(jsonPath("$.metrics.statusClassCounts['5xx']").value(3))
                .andExpect(jsonPath("$.metrics.upstreams[0].upstreamId").value("backend-a"))
                .andExpect(jsonPath("$.metrics.upstreams[0].retryAttempts").value(1))
                .andExpect(jsonPath("$.metrics.upstreams[1].upstreamId").value("backend-b"))
                .andExpect(jsonPath("$.metrics.upstreams[1].retryAttempts").value(0));
    }

    private static final class MutableUpstream {
        private final String id;
        private final AtomicInteger proxyStatus;
        private final AtomicInteger proxyRequests = new AtomicInteger();
        private final HttpServer server;
        private final ExecutorService executor;

        private MutableUpstream(String id, int proxyStatus, HttpServer server, ExecutorService executor) {
            this.id = id;
            this.proxyStatus = new AtomicInteger(proxyStatus);
            this.server = server;
            this.executor = executor;
        }

        private static MutableUpstream start(String id, int proxyStatus) {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                MutableUpstream upstream = new MutableUpstream(id, proxyStatus, server, executor);
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

        private int proxyRequests() {
            return proxyRequests.get();
        }

        private void stop() {
            server.stop(0);
            executor.shutdownNow();
        }

        private void handle(HttpExchange exchange) throws IOException {
            proxyRequests.incrementAndGet();
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            String body = id + " " + exchange.getRequestMethod() + " " + exchange.getRequestURI()
                    + " body=" + new String(requestBody, StandardCharsets.UTF_8);
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(proxyStatus.get(), response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }
    }
}
