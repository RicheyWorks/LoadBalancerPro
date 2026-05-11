package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReverseProxyControllerTest {
    private static final TestUpstream BACKEND_A = TestUpstream.start("backend-a");
    private static final TestUpstream BACKEND_B = TestUpstream.start("backend-b");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.strategy", () -> "ROUND_ROBIN");
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
    @Order(1)
    void getRequestIsForwardedToFirstLocalUpstreamWithQueryStringAndHeaders() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(get("/proxy/service/widgets?color=blue&size=small")
                            .header("X-Demo", "present"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-LoadBalancerPro-Upstream", "backend-a"))
                    .andExpect(header().string("X-LoadBalancerPro-Strategy", "ROUND_ROBIN"))
                    .andExpect(header().string("X-Fixture-Upstream", "backend-a"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string(containsString(
                            "backend-a GET /service/widgets?color=blue&size=small")))
                    .andExpect(content().string(containsString("x-demo=present")));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Proxy forwarding must not construct CloudManager or enter cloud paths.");
        }
    }

    @Test
    @Order(2)
    void postRequestForwardsBodyToSecondRoundRobinUpstream() throws Exception {
        mockMvc.perform(post("/proxy/orders?source=test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order\":42}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", "backend-b"))
                .andExpect(header().string("X-LoadBalancerPro-Strategy", "ROUND_ROBIN"))
                .andExpect(header().string("X-Fixture-Upstream", "backend-b"))
                .andExpect(content().string(containsString("backend-b POST /orders?source=test")))
                .andExpect(content().string(containsString("body={\"order\":42}")));
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
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            String demoHeader = exchange.getRequestHeaders().getFirst("X-Demo");
            String body = id + " " + exchange.getRequestMethod() + " " + exchange.getRequestURI()
                    + " body=" + new String(requestBody, StandardCharsets.UTF_8)
                    + " x-demo=" + (demoHeader == null ? "" : demoHeader);
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.getResponseHeaders().set("X-Fixture-Upstream", id);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }
    }
}
