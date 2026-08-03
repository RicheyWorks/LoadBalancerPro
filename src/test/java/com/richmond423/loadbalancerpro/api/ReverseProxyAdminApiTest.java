package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "loadbalancerpro.auth.mode=none",
        "loadbalancerpro.api.key=TEST_ADMIN_API_KEY"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(OutputCaptureExtension.class)
class ReverseProxyAdminApiTest {
    private static final String API_KEY = "TEST_ADMIN_API_KEY";
    private static final int HELD_REQUEST_COUNT = 12;
    private static final TestUpstream STARTUP_BACKEND = TestUpstream.start("admin-a", 0);
    private static final TestUpstream ADDED_BACKEND = TestUpstream.start("admin-b", HELD_REQUEST_COUNT);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.strategy", () -> "WEIGHTED_ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.upstreams[0].id", () -> "admin-a");
        registry.add("loadbalancerpro.proxy.upstreams[0].url", STARTUP_BACKEND::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[0].healthy", () -> "true");
    }

    @AfterAll
    static void stopUpstreams() {
        STARTUP_BACKEND.stop();
        ADDED_BACKEND.stop();
    }

    @Test
    void redactedConfigAndMutationsRequireTheReloadAuthenticationBoundary() throws Exception {
        mockMvc.perform(get("/api/proxy/config"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));

        mockMvc.perform(get("/api/proxy/config").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(API_KEY))))
                .andExpect(content().string(not(containsString(STARTUP_BACKEND.baseUrl()))))
                .andExpect(content().string(not(containsString("url"))))
                .andExpect(jsonPath("$.generation").value(1))
                .andExpect(jsonPath("$.routeCount").value(1))
                .andExpect(jsonPath("$.backendTargetCount").value(1))
                .andExpect(jsonPath("$.routes[0].name").value("legacy-upstreams"))
                .andExpect(jsonPath("$.routes[0].upstreams[0].id").value("admin-a"))
                .andExpect(jsonPath("$.drainingUpstreamIds", hasSize(0)));

        mockMvc.perform(post("/api/proxy/upstreams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody("unauthorized-add", 1)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/proxy/upstreams/missing")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedGeneration\":1,\"healthy\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.generation").value(1));

        mockMvc.perform(get("/api/proxy/config").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generation").value(1))
                .andExpect(jsonPath("$.backendTargetCount").value(1));
    }

    @Test
    void addDrainDeleteKeepsBoundedConcurrentLoopbackRequestsAlive(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/api/proxy/upstreams")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody("admin-b", 1)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(ADDED_BACKEND.baseUrl()))))
                .andExpect(jsonPath("$.generation").value(2))
                .andExpect(jsonPath("$.config.backendTargetCount").value(2));

        patchDrain("admin-a", 2, true, 3);

        ExecutorService clients = Executors.newFixedThreadPool(HELD_REQUEST_COUNT);
        List<Future<MvcResult>> heldRequests = new ArrayList<>();
        try {
            for (int index = 0; index < HELD_REQUEST_COUNT; index++) {
                int requestIndex = index;
                heldRequests.add(clients.submit(() -> mockMvc.perform(
                                get("/proxy/held?request=" + requestIndex))
                        .andReturn()));
            }
            assertTrue(ADDED_BACKEND.awaitHeldRequests(Duration.ofSeconds(5)),
                    "all bounded loopback requests should reach the added upstream");

            patchDrain("admin-b", 3, true, 4);

            mockMvc.perform(delete("/api/proxy/upstreams/admin-b")
                            .header("X-API-Key", API_KEY)
                            .queryParam("expectedGeneration", "4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.generation").value(5))
                    .andExpect(jsonPath("$.config.backendTargetCount").value(1))
                    .andExpect(jsonPath("$.config.drainingUpstreamIds[0]").value("admin-b"));

            ADDED_BACKEND.releaseHeldRequests();
            for (Future<MvcResult> request : heldRequests) {
                MvcResult result = request.get(5, TimeUnit.SECONDS);
                assertEquals(200, result.getResponse().getStatus());
                assertEquals("admin-b", result.getResponse().getHeader("X-LoadBalancerPro-Upstream"));
            }
        } finally {
            ADDED_BACKEND.releaseHeldRequests();
            clients.shutdownNow();
        }

        awaitNoDrainingUpstreams();
        patchDrain("admin-a", 5, false, 6);

        mockMvc.perform(get("/proxy/after-admin-cycle"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", "admin-a"));
        assertEquals(5, occurrences(output.getOut(), "proxy.admin.audit action="),
                "each successful mutation should emit one bounded audit event");
        assertFalse(output.getOut().contains(ADDED_BACKEND.baseUrl()));
        assertFalse(output.getOut().contains(API_KEY));
        assertFalse(output.getOut().contains("request="));
    }

    @Test
    void concurrentSameGenerationMutationsYieldOneSuccessAndOneConflict(CapturedOutput output) throws Exception {
        ExecutorService clients = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<MvcResult> first = clients.submit(() -> addConcurrently("concurrent-a", start));
            Future<MvcResult> second = clients.submit(() -> addConcurrently("concurrent-b", start));
            start.countDown();

            List<Integer> statuses = new ArrayList<>(List.of(
                    first.get(5, TimeUnit.SECONDS).getResponse().getStatus(),
                    second.get(5, TimeUnit.SECONDS).getResponse().getStatus()));
            Collections.sort(statuses);
            assertEquals(List.of(200, 409), statuses);
        } finally {
            clients.shutdownNow();
        }

        mockMvc.perform(get("/api/proxy/config").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(ADDED_BACKEND.baseUrl()))))
                .andExpect(jsonPath("$.generation").value(2))
                .andExpect(jsonPath("$.backendTargetCount").value(2));
        assertEquals(1, occurrences(output.getOut(), "proxy.admin.audit action="));
        assertFalse(output.getOut().contains(ADDED_BACKEND.baseUrl()));
        assertFalse(output.getOut().contains(API_KEY));
    }

    private MvcResult addConcurrently(String id, CountDownLatch start) throws Exception {
        assertTrue(start.await(5, TimeUnit.SECONDS));
        return mockMvc.perform(post("/api/proxy/upstreams")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(id, 1)))
                .andExpect(content().string(not(containsString(ADDED_BACKEND.baseUrl()))))
                .andReturn();
    }

    private void patchDrain(String upstreamId, long expectedGeneration, boolean drain, long nextGeneration)
            throws Exception {
        mockMvc.perform(patch("/api/proxy/upstreams/{id}", upstreamId)
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedGeneration":%d,"drain":%s}
                                """.formatted(expectedGeneration, drain)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generation").value(nextGeneration));
    }

    private void awaitNoDrainingUpstreams() throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (System.nanoTime() < deadline) {
            MvcResult result = mockMvc.perform(get("/api/proxy/config").header("X-API-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            if (root.path("drainingUpstreamIds").isEmpty()) {
                return;
            }
            Thread.yield();
        }
        throw new AssertionError("removed upstream did not finish draining within the bounded wait");
    }

    private static String addBody(String id, long expectedGeneration) {
        return """
                {
                  "expectedGeneration": %d,
                  "route": "legacy-upstreams",
                  "id": "%s",
                  "url": "%s",
                  "healthy": true,
                  "weight": 1.0,
                  "maxInFlight": 0
                }
                """.formatted(expectedGeneration, id, ADDED_BACKEND.baseUrl());
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }

    private static final class TestUpstream {
        private final String id;
        private final HttpServer server;
        private final ExecutorService executor;
        private final CountDownLatch heldRequestsStarted;
        private final CountDownLatch releaseHeldRequests = new CountDownLatch(1);

        private TestUpstream(
                String id,
                HttpServer server,
                ExecutorService executor,
                int expectedHeldRequests) {
            this.id = id;
            this.server = server;
            this.executor = executor;
            this.heldRequestsStarted = new CountDownLatch(expectedHeldRequests);
        }

        private static TestUpstream start(String id, int expectedHeldRequests) {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newCachedThreadPool();
                TestUpstream upstream = new TestUpstream(id, server, executor, expectedHeldRequests);
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

        private boolean awaitHeldRequests(Duration timeout) throws InterruptedException {
            return heldRequestsStarted.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        private void releaseHeldRequests() {
            releaseHeldRequests.countDown();
        }

        private void stop() {
            server.stop(0);
            executor.shutdownNow();
        }

        private void handle(HttpExchange exchange) throws IOException {
            if ("/held".equals(exchange.getRequestURI().getPath())) {
                heldRequestsStarted.countDown();
                try {
                    releaseHeldRequests.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    exchange.close();
                    return;
                }
            }
            byte[] body = id.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        }
    }
}
