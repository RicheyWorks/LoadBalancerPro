package com.richmond423.loadbalancerpro.api.proxy;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
class PrivateNetworkLiveValidationStatusReportTest {
    private static final CountingLoopbackBackend BACKEND = CountingLoopbackBackend.start();
    private static final Path STATUS_REPORT_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/"
                    + "PrivateNetworkLiveValidationStatusResponse.java");
    private static final Path STATUS_RESPONSE_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyStatusResponse.java");
    private static final Path STATUS_CONTROLLER_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyStatusController.java");
    private static final Path REVERSE_PROXY_SERVICE_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyService.java");
    private static final Path SMOKE_SCRIPTS = Path.of("scripts/smoke");
    private static final Path POSTMAN_DOCS = Path.of("docs/postman");
    private static final String API_KEY_SENTINEL = "TEST_PRIVATE_NETWORK_STATUS_API_KEY";

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void statusReportProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.private-network-validation.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.private-network-live-validation.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.private-network-live-validation.operator-approved", () -> "true");
        registry.add("loadbalancerpro.proxy.health-check.enabled", () -> "false");
        registry.add("loadbalancerpro.proxy.upstreams[0].id", () -> "status-loopback");
        registry.add("loadbalancerpro.proxy.upstreams[0].url", BACKEND::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[0].healthy", () -> "true");
    }

    @AfterAll
    static void stopBackend() {
        BACKEND.close();
    }

    @Test
    void statusReportShowsAllowedGateWithoutExecutingTraffic() throws Exception {
        assertEquals(0, BACKEND.requestCount());

        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.privateNetworkLiveValidation.liveValidationEnabled").value(true))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.operatorApproved").value(true))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.configValidationEnabled").value(true))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.proxyEnabled").value(true))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.gateStatus").value("ALLOWED"))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.allowedByGate").value(true))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.trafficExecuted").value(false))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.trafficExecution")
                        .value("traffic not executed by this report"))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.reasonCodes[0]").value("ALLOWED_BY_GATE"))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.backends[0].label")
                        .value("upstreams[0].status-loopback"))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.backends[0].classifierStatus")
                        .value("LOOPBACK_ALLOWED"))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.backends[0].classifierApproved").value(true))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.backends[0].normalizedUrl")
                        .value(BACKEND.baseUrl()))
                .andExpect(content().string(not(containsString("PrivateNetworkLiveValidationExecutor"))))
                .andExpect(content().string(not(containsString("Authorization"))))
                .andExpect(content().string(not(containsString("X-API-Key"))));

        assertEquals(0, BACKEND.requestCount(), "status report must not send validation traffic");
    }

    @Test
    void statusReportFactoryShowsDefaultMissingApprovalUnsafeAndPrivateAllowedStates() {
        PrivateNetworkLiveValidationStatusResponse defaults =
                PrivateNetworkLiveValidationStatusResponse.from(propertiesWithTarget(
                        "local", "http://127.0.0.1:18081"));
        assertEquals("NOT_ENABLED", defaults.gateStatus());
        assertFalse(defaults.allowedByGate());
        assertFalse(defaults.trafficExecuted());
        assertTrue(defaults.reasonCodes().contains("LIVE_VALIDATION_DISABLED"));
        assertEquals("LOOPBACK_ALLOWED", defaults.backends().get(0).classifierStatus());
        assertEquals("http://127.0.0.1:18081", defaults.backends().get(0).normalizedUrl());

        ReverseProxyProperties missingApproval = propertiesWithTarget("local", "http://127.0.0.1:18081");
        missingApproval.getPrivateNetworkLiveValidation().setEnabled(true);
        missingApproval.getPrivateNetworkValidation().setEnabled(true);
        PrivateNetworkLiveValidationStatusResponse missingApprovalReport =
                PrivateNetworkLiveValidationStatusResponse.from(missingApproval);
        assertEquals("BLOCKED", missingApprovalReport.gateStatus());
        assertTrue(missingApprovalReport.reasonCodes().contains("OPERATOR_APPROVAL_REQUIRED"));

        ReverseProxyProperties unsafe = propertiesWithTarget("unsafe", "http://user:"
                + API_KEY_SENTINEL + "@127.0.0.1:18081");
        enableAllLiveGateFlags(unsafe);
        PrivateNetworkLiveValidationStatusResponse unsafeReport =
                PrivateNetworkLiveValidationStatusResponse.from(unsafe);
        assertEquals("BLOCKED", unsafeReport.gateStatus());
        assertTrue(unsafeReport.reasonCodes().contains("BACKEND_CLASSIFIER_REJECTED"));
        assertEquals("USERINFO_REJECTED", unsafeReport.backends().get(0).classifierStatus());
        assertEquals("", unsafeReport.backends().get(0).normalizedUrl());
        assertFalse(unsafeReport.toString().contains(API_KEY_SENTINEL));

        ReverseProxyProperties privateLiteral = propertiesWithTarget("private", "http://10.1.2.3:18082");
        enableAllLiveGateFlags(privateLiteral);
        PrivateNetworkLiveValidationStatusResponse privateReport =
                PrivateNetworkLiveValidationStatusResponse.from(privateLiteral);
        assertEquals("ALLOWED", privateReport.gateStatus());
        assertTrue(privateReport.allowedByGate());
        assertFalse(privateReport.trafficExecuted());
        assertEquals("PRIVATE_NETWORK_ALLOWED", privateReport.backends().get(0).classifierStatus());
        assertEquals("http://10.1.2.3:18082", privateReport.backends().get(0).normalizedUrl());
    }

    @Test
    void statusReportSourcesStayOfflineAndDoNotWireExecutorTraffic() throws Exception {
        String statusReport = read(STATUS_REPORT_SOURCE);
        String statusResponse = read(STATUS_RESPONSE_SOURCE);
        String statusController = read(STATUS_CONTROLLER_SOURCE);
        String reverseProxyService = read(REVERSE_PROXY_SERVICE_SOURCE);
        String combinedStatusSources = statusReport + "\n" + statusResponse + "\n" + statusController;

        assertTrue(statusResponse.contains("PrivateNetworkLiveValidationStatusResponse"));
        assertTrue(statusReport.contains("traffic not executed by this report"));
        assertTrue(statusReport.contains("trafficExecuted"));
        assertFalse(combinedStatusSources.contains("PrivateNetworkLiveValidationExecutor"),
                "status/report path must not invoke the live executor");
        assertFalse(reverseProxyService.contains("PrivateNetworkLiveValidationExecutor"),
                "proxy runtime service must not invoke the live executor");

        for (String forbidden : List.of(
                "InetAddress",
                "getByName",
                "HttpClient",
                "URLConnection",
                "new Socket",
                "DatagramSocket",
                ".connect(",
                "isReachable")) {
            assertFalse(statusReport.contains(forbidden), "status report must stay offline; found " + forbidden);
        }
    }

    @Test
    void smokeAndPostmanPathsDoNotGainPrivateNetworkLiveExecution() throws Exception {
        String combined = readTree(SMOKE_SCRIPTS) + "\n" + readTree(POSTMAN_DOCS);

        assertFalse(combined.contains("private-network-live-validation"),
                "smoke/Postman paths must not add private-network live execution");
        assertFalse(combined.contains("PrivateNetworkLiveValidationExecutor"),
                "smoke/Postman paths must not invoke the live executor");
        assertFalse(combined.contains("PrivateNetworkLiveValidationStatusResponse"),
                "smoke/Postman paths should not couple to the status report DTO");
    }

    @Test
    void statusReportDoesNotExposeApiKeysOrSecretFields() throws Exception {
        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(API_KEY_SENTINEL))))
                .andExpect(content().string(not(containsString("Authorization"))))
                .andExpect(content().string(not(containsString("X-API-Key"))))
                .andExpect(content().string(not(containsString("Bearer"))))
                .andExpect(content().string(not(containsString("Cookie"))));
    }

    private static ReverseProxyProperties propertiesWithTarget(String id, String url) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setUpstreams(List.of(upstream(id, url)));
        return properties;
    }

    private static ReverseProxyProperties.Upstream upstream(String id, String url) {
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId(id);
        upstream.setUrl(url);
        upstream.setWeight(1.0);
        return upstream;
    }

    private static void enableAllLiveGateFlags(ReverseProxyProperties properties) {
        properties.getPrivateNetworkLiveValidation().setEnabled(true);
        properties.getPrivateNetworkLiveValidation().setOperatorApproved(true);
        properties.getPrivateNetworkValidation().setEnabled(true);
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String readTree(Path root) throws IOException {
        assertTrue(Files.exists(root), root + " should exist");
        StringBuilder content = new StringBuilder();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                content.append(read(path)).append('\n');
            }
        }
        return content.toString();
    }

    private static final class CountingLoopbackBackend implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;
        private final AtomicInteger requestCount = new AtomicInteger();

        private CountingLoopbackBackend(HttpServer server, ExecutorService executor) {
            this.server = server;
            this.executor = executor;
        }

        private static CountingLoopbackBackend start() {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress("127.0.0.1", 0), 0);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                CountingLoopbackBackend backend = new CountingLoopbackBackend(server, executor);
                server.createContext("/", exchange -> {
                    backend.requestCount.incrementAndGet();
                    byte[] response = "status-report-backend-should-not-be-called"
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                });
                server.setExecutor(executor);
                server.start();
                return backend;
            } catch (IOException exception) {
                throw new IllegalStateException("Could not start loopback backend for status report test", exception);
            }
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private int requestCount() {
            return requestCount.get();
        }

        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
