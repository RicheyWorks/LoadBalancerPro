package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "loadbalancerpro.auth.mode=none")
@AutoConfigureMockMvc
@DirtiesContext
class ReverseProxyRuntimeStatsIntegrationTest {
    private static final BlockingUpstream UPSTREAM = BlockingUpstream.start();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.request-timeout", () -> "5s");
        registry.add("loadbalancerpro.proxy.upstreams[0].id", () -> "blocking-upstream");
        registry.add("loadbalancerpro.proxy.upstreams[0].url", UPSTREAM::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[0].healthy", () -> "true");
    }

    @AfterAll
    static void stopUpstream() {
        UPSTREAM.close();
    }

    @Test
    void statusShowsLiveInFlightRequestAndCompletedLatencyEvidence() throws Exception {
        ExecutorService clientExecutor = Executors.newSingleThreadExecutor();
        Future<MvcResult> request = clientExecutor.submit(
                () -> mockMvc.perform(get("/proxy/blocked")).andReturn());

        try {
            assertTrue(UPSTREAM.awaitRequest(Duration.ofSeconds(5)),
                    "blocking upstream did not receive the proxy request");

            mockMvc.perform(get("/api/proxy/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.inFlightRequestCount").value(1))
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.completedRequestCount").value(0))
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.latencySampleCount").value(0))
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.lastUpdatedAt").doesNotExist());

            UPSTREAM.release();
            MvcResult completed = request.get(5, TimeUnit.SECONDS);
            assertTrue(completed.getResponse().getStatus() == 200,
                    "proxied request did not complete successfully");

            mockMvc.perform(get("/api/proxy/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.inFlightRequestCount").value(0))
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.completedRequestCount").value(1))
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.latencySampleCount").value(1))
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.ewmaLatencyMillis",
                            greaterThan(0.0)))
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.p50LatencyMillis",
                            greaterThan(0.0)))
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.p95LatencyMillis",
                            greaterThan(0.0)))
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.p99LatencyMillis",
                            greaterThan(0.0)))
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.recentSuccessCount").value(1))
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.recentFailureCount").value(0))
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.recentErrorRate").value(0.0))
                    .andExpect(jsonPath("$.upstreams[0].runtimeStats.lastUpdatedAt").isString());
        } finally {
            UPSTREAM.release();
            clientExecutor.shutdownNow();
        }
    }

    private static final class BlockingUpstream implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;
        private final CountDownLatch requestEntered = new CountDownLatch(1);
        private final CountDownLatch releaseRequest = new CountDownLatch(1);

        private BlockingUpstream(HttpServer server, ExecutorService executor) {
            this.server = server;
            this.executor = executor;
        }

        private static BlockingUpstream start() {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                BlockingUpstream upstream = new BlockingUpstream(server, executor);
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

        private boolean awaitRequest(Duration timeout) throws InterruptedException {
            return requestEntered.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        private void release() {
            releaseRequest.countDown();
        }

        private void handle(HttpExchange exchange) throws IOException {
            requestEntered.countDown();
            try {
                if (!releaseRequest.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("test did not release blocking upstream");
                }
                byte[] body = "released".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("blocking upstream interrupted", exception);
            } finally {
                exchange.close();
            }
        }

        @Override
        public void close() {
            release();
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
