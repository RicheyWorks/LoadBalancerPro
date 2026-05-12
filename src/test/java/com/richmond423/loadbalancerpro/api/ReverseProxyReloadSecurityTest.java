package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.profiles.active=prod",
        "loadbalancerpro.api.key=TEST_PROD_RELOAD_KEY"
})
@AutoConfigureMockMvc
class ReverseProxyReloadSecurityTest {
    private static final String API_KEY = "TEST_PROD_RELOAD_KEY";
    private static final TestUpstream STARTUP_BACKEND = TestUpstream.start("startup-prod");
    private static final TestUpstream RELOAD_BACKEND = TestUpstream.start("reload-prod");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.upstreams[0].id", () -> "startup-prod");
        registry.add("loadbalancerpro.proxy.upstreams[0].url", STARTUP_BACKEND::baseUrl);
    }

    @AfterAll
    static void stopUpstreams() {
        STARTUP_BACKEND.stop();
        RELOAD_BACKEND.stop();
    }

    @Test
    void prodApiKeyBoundaryProtectsReloadEndpointAndAllowsAuthenticatedReload() throws Exception {
        mockMvc.perform(post("/api/proxy/reload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReloadBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path").value("/api/proxy/reload"));

        mockMvc.perform(post("/api/proxy/reload")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReloadBody()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(API_KEY))))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.activeConfigGeneration").value(2));
    }

    private static String validReloadBody() {
        return """
                {
                  "enabled": true,
                  "strategy": "ROUND_ROBIN",
                  "routes": {
                    "prod": {
                      "pathPrefix": "/prod",
                      "targets": [
                        {
                          "id": "reload-prod",
                          "url": "%s",
                          "healthy": true,
                          "weight": 1.0
                        }
                      ]
                    }
                  }
                }
                """.formatted(RELOAD_BACKEND.baseUrl());
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
            byte[] response = (id + " " + exchange.getRequestURI()).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }
    }
}
