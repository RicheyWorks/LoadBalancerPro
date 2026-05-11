package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class ReverseProxyHealthAwareTest {
    private static final TestUpstream HEALTHY_BACKEND = TestUpstream.start("healthy-backend");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.strategy", () -> "ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.upstreams[0].id", () -> "configured-unhealthy");
        registry.add("loadbalancerpro.proxy.upstreams[0].url", () -> "http://127.0.0.1:1");
        registry.add("loadbalancerpro.proxy.upstreams[0].healthy", () -> "false");
        registry.add("loadbalancerpro.proxy.upstreams[1].id", () -> "healthy-backend");
        registry.add("loadbalancerpro.proxy.upstreams[1].url", HEALTHY_BACKEND::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[1].healthy", () -> "true");
    }

    @AfterAll
    static void stopUpstream() {
        HEALTHY_BACKEND.stop();
    }

    @Test
    void configuredUnhealthyUpstreamIsSkippedBeforeForwarding() throws Exception {
        mockMvc.perform(get("/proxy/health-aware"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", "healthy-backend"))
                .andExpect(content().string(containsString("healthy-backend GET /health-aware")));
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
