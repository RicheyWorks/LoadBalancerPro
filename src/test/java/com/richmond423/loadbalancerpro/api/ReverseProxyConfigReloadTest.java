package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

@SpringBootTest(properties = "loadbalancerpro.api.key=TEST_RELOAD_API_KEY")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReverseProxyConfigReloadTest {
    private static final String API_KEY = "TEST_RELOAD_API_KEY";
    private static final TestUpstream STARTUP_BACKEND = TestUpstream.start("startup-backend");
    private static final TestUpstream RELOADED_BACKEND = TestUpstream.start("reloaded-backend");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.strategy", () -> "ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.upstreams[0].id", () -> "startup-backend");
        registry.add("loadbalancerpro.proxy.upstreams[0].url", STARTUP_BACKEND::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[0].healthy", () -> "true");
    }

    @AfterAll
    static void stopUpstreams() {
        STARTUP_BACKEND.stop();
        RELOADED_BACKEND.stop();
    }

    @Test
    void validReloadUpdatesGenerationCountsAndRoutingTarget() throws Exception {
        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reload.configReloadSupported").value(true))
                .andExpect(jsonPath("$.reload.activeConfigGeneration").value(1))
                .andExpect(jsonPath("$.reload.lastReloadStatus").value("not_attempted"))
                .andExpect(jsonPath("$.reload.activeRouteCount").value(1))
                .andExpect(jsonPath("$.reload.activeBackendTargetCount").value(1));

        mockMvc.perform(get("/proxy/startup?step=before"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", "startup-backend"))
                .andExpect(content().string(containsString("startup-backend GET /startup?step=before")));

        mockMvc.perform(post("/api/proxy/reload")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReloadBody()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(API_KEY))))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.activeConfigGeneration").value(2))
                .andExpect(jsonPath("$.activeRouteCount").value(1))
                .andExpect(jsonPath("$.activeBackendTargetCount").value(1));

        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes[0].name").value("api"))
                .andExpect(jsonPath("$.routes[0].pathPrefix").value("/api"))
                .andExpect(jsonPath("$.routes[0].targetIds[0]").value("reloaded-backend"))
                .andExpect(jsonPath("$.observability.routeCount").value(1))
                .andExpect(jsonPath("$.observability.backendTargetCount").value(1))
                .andExpect(jsonPath("$.reload.activeConfigGeneration").value(2))
                .andExpect(jsonPath("$.reload.lastReloadStatus").value("success"))
                .andExpect(jsonPath("$.reload.lastReloadSucceededAt").isString());

        mockMvc.perform(get("/proxy/api/reloaded?step=after"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", "reloaded-backend"))
                .andExpect(content().string(containsString("reloaded-backend GET /api/reloaded?step=after")));
    }

    @Test
    void invalidReloadReportsValidationErrorsAndPreservesLastKnownGoodConfig() throws Exception {
        mockMvc.perform(post("/api/proxy/reload")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReloadBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeConfigGeneration").value(2));

        mockMvc.perform(post("/api/proxy/reload")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidReloadBody()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString(API_KEY))))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("failure"))
                .andExpect(jsonPath("$.activeConfigGeneration").value(2))
                .andExpect(jsonPath("$.validationErrors[0]").value(containsString(".id must not be blank")));

        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(API_KEY))))
                .andExpect(jsonPath("$.routes[0].name").value("api"))
                .andExpect(jsonPath("$.routes[0].targetIds[0]").value("reloaded-backend"))
                .andExpect(jsonPath("$.reload.activeConfigGeneration").value(2))
                .andExpect(jsonPath("$.reload.lastReloadStatus").value("failure"))
                .andExpect(jsonPath("$.reload.lastReloadFailedAt").isString())
                .andExpect(jsonPath("$.reload.lastReloadValidationErrors[0]")
                        .value(containsString(".id must not be blank")));

        mockMvc.perform(get("/proxy/api/still-good"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", "reloaded-backend"))
                .andExpect(content().string(containsString("reloaded-backend GET /api/still-good")));
    }

    @Test
    void privateNetworkValidationRejectsUnsafeReloadAndPreservesLastKnownGoodConfig() throws Exception {
        mockMvc.perform(post("/api/proxy/reload")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unsafePrivateNetworkValidationReloadBody()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString(API_KEY))))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("failure"))
                .andExpect(jsonPath("$.activeConfigGeneration").value(1))
                .andExpect(jsonPath("$.validationErrors[0]").value(containsString("PUBLIC_NETWORK_REJECTED")));

        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes[0].targetIds[0]").value("startup-backend"))
                .andExpect(jsonPath("$.reload.activeConfigGeneration").value(1))
                .andExpect(jsonPath("$.reload.lastReloadStatus").value("failure"))
                .andExpect(jsonPath("$.reload.lastReloadValidationErrors[0]")
                        .value(containsString("PUBLIC_NETWORK_REJECTED")));

        mockMvc.perform(get("/proxy/startup?step=after-rejected-reload"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", "startup-backend"))
                .andExpect(content().string(containsString(
                        "startup-backend GET /startup?step=after-rejected-reload")));
    }

    @Test
    void reloadEndpointRejectsUnauthenticatedMutationEvenInLocalProfile() throws Exception {
        mockMvc.perform(post("/api/proxy/reload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReloadBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("unauthorized"));
    }

    private static String validReloadBody() {
        return """
                {
                  "enabled": true,
                  "strategy": "ROUND_ROBIN",
                  "routes": {
                    "api": {
                      "pathPrefix": "/api",
                      "strategy": "ROUND_ROBIN",
                      "targets": [
                        {
                          "id": "reloaded-backend",
                          "url": "%s",
                          "healthy": true,
                          "weight": 1.0
                        }
                      ]
                    }
                  }
                }
                """.formatted(RELOADED_BACKEND.baseUrl());
    }

    private static String unsafePrivateNetworkValidationReloadBody() {
        return """
                {
                  "enabled": true,
                  "strategy": "ROUND_ROBIN",
                  "privateNetworkValidation": {
                    "enabled": true
                  },
                  "routes": {
                    "api": {
                      "pathPrefix": "/api",
                      "targets": [
                        {
                          "id": "public-backend",
                          "url": "http://8.8.8.8:18081",
                          "healthy": true,
                          "weight": 1.0
                        }
                      ]
                    }
                  }
                }
                """;
    }

    private static String invalidReloadBody() {
        return """
                {
                  "enabled": true,
                  "strategy": "ROUND_ROBIN",
                  "routes": {
                    "api": {
                      "pathPrefix": "/api",
                      "targets": [
                        {
                          "id": "   ",
                          "url": "%s",
                          "healthy": true,
                          "weight": 1.0
                        }
                      ]
                    }
                  }
                }
                """.formatted(RELOADED_BACKEND.baseUrl());
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
