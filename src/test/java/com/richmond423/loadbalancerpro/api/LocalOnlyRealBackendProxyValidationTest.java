package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
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
class LocalOnlyRealBackendProxyValidationTest {
    private static final LoopbackBackendFixture BACKEND_A = LoopbackBackendFixture.start("real-local-a");
    private static final LoopbackBackendFixture BACKEND_B = LoopbackBackendFixture.start("real-local-b");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.strategy", () -> "ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.routes.real.path-prefix", () -> "/real");
        registry.add("loadbalancerpro.proxy.routes.real.strategy", () -> "ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.routes.real.targets[0].id", () -> "real-local-a");
        registry.add("loadbalancerpro.proxy.routes.real.targets[0].url", BACKEND_A::baseUrl);
        registry.add("loadbalancerpro.proxy.routes.real.targets[0].healthy", () -> "true");
        registry.add("loadbalancerpro.proxy.routes.real.targets[1].id", () -> "real-local-b");
        registry.add("loadbalancerpro.proxy.routes.real.targets[1].url", BACKEND_B::baseUrl);
        registry.add("loadbalancerpro.proxy.routes.real.targets[1].healthy", () -> "true");
        registry.add("loadbalancerpro.proxy.routes.unavailable.path-prefix", () -> "/unavailable");
        registry.add("loadbalancerpro.proxy.routes.unavailable.strategy", () -> "ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.routes.unavailable.targets[0].id", () -> "disabled-local-backend");
        registry.add("loadbalancerpro.proxy.routes.unavailable.targets[0].url", BACKEND_A::baseUrl);
        registry.add("loadbalancerpro.proxy.routes.unavailable.targets[0].healthy", () -> "false");
    }

    @AfterAll
    static void stopBackends() {
        BACKEND_A.stop();
        BACKEND_B.stop();
    }

    @Test
    void forwardsMethodPathQueryBodyAndHeadersToLoopbackBackends() throws Exception {
        mockMvc.perform(post("/proxy/real/api/widgets?trace=reviewer")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("X-Reviewer-Trace", "source-visible")
                        .content("hello=loopback"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", "real-local-a"))
                .andExpect(header().string("X-LoadBalancerPro-Strategy", "ROUND_ROBIN"))
                .andExpect(header().string("X-Local-Backend-Fixture", "real-local-a"))
                .andExpect(header().string("X-Backend-Fixture-Mode", "loopback-only"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("id=real-local-a")))
                .andExpect(content().string(containsString("method=POST")))
                .andExpect(content().string(containsString("uri=/real/api/widgets?trace=reviewer")))
                .andExpect(content().string(containsString("body=hello=loopback")))
                .andExpect(content().string(containsString("x-reviewer-trace=source-visible")));

        mockMvc.perform(get("/proxy/real/api/widgets?trace=second"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", "real-local-b"))
                .andExpect(header().string("X-LoadBalancerPro-Strategy", "ROUND_ROBIN"))
                .andExpect(header().string("X-Local-Backend-Fixture", "real-local-b"))
                .andExpect(content().string(containsString("id=real-local-b")))
                .andExpect(content().string(containsString("method=GET")))
                .andExpect(content().string(containsString("uri=/real/api/widgets?trace=second")));

        assertEquals(1, BACKEND_A.requestCount());
        assertEquals(1, BACKEND_B.requestCount());

        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proxyEnabled").value(true))
                .andExpect(content().string(containsString("\"name\":\"real\"")))
                .andExpect(content().string(containsString("\"pathPrefix\":\"/real\"")))
                .andExpect(content().string(containsString("\"real-local-a\"")))
                .andExpect(content().string(containsString("\"real-local-b\"")))
                .andExpect(jsonPath("$.metrics.totalForwarded").value(2))
                .andExpect(content().string(containsString("\"upstreamId\":\"real-local-a\"")))
                .andExpect(content().string(containsString("\"upstreamId\":\"real-local-b\"")))
                .andExpect(content().string(containsString("\"forwarded\":1")));
    }

    @Test
    void configuredUnavailableBackendFailsClosedWithoutCallingLoopbackFixture() throws Exception {
        int requestsBefore = BACKEND_A.requestCount() + BACKEND_B.requestCount();

        mockMvc.perform(get("/proxy/unavailable/check"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().doesNotExist("X-LoadBalancerPro-Upstream"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"error\":\"proxy_unavailable\"")))
                .andExpect(content().string(containsString("No healthy proxy upstreams are available")));

        assertEquals(requestsBefore, BACKEND_A.requestCount() + BACKEND_B.requestCount(),
                "Configured unavailable backends must fail before any loopback fixture call.");
    }

    private static final class LoopbackBackendFixture {
        private final String id;
        private final HttpServer server;
        private final ExecutorService executor;
        private final AtomicInteger requestCount = new AtomicInteger();

        private LoopbackBackendFixture(String id, HttpServer server, ExecutorService executor) {
            this.id = id;
            this.server = server;
            this.executor = executor;
        }

        private static LoopbackBackendFixture start(String id) {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                LoopbackBackendFixture fixture = new LoopbackBackendFixture(id, server, executor);
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

        private int requestCount() {
            return requestCount.get();
        }

        private void stop() {
            server.stop(0);
            executor.shutdownNow();
        }

        private void handle(HttpExchange exchange) throws IOException {
            requestCount.incrementAndGet();
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            String reviewerTrace = exchange.getRequestHeaders().getFirst("X-Reviewer-Trace");
            String body = "id=" + id
                    + "\nmethod=" + exchange.getRequestMethod()
                    + "\nuri=" + exchange.getRequestURI()
                    + "\nbody=" + new String(requestBody, StandardCharsets.UTF_8)
                    + "\nx-reviewer-trace=" + (reviewerTrace == null ? "" : reviewerTrace);
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.getResponseHeaders().set("X-Local-Backend-Fixture", id);
            exchange.getResponseHeaders().set("X-Backend-Fixture-Mode", "loopback-only");
            exchange.sendResponseHeaders(202, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }
    }
}
