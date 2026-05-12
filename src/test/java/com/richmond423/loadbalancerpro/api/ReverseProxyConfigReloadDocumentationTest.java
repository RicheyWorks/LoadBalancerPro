package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ReverseProxyConfigReloadDocumentationTest {
    private static final Path STATUS_RESPONSE =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyStatusResponse.java");
    private static final Path RELOAD_RESPONSE =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyReloadResponse.java");
    private static final Path STATUS_CONTROLLER =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyStatusController.java");
    private static final Path REVERSE_PROXY_SERVICE =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyService.java");
    private static final Path API_SECURITY =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/config/ApiSecurityConfiguration.java");
    private static final Path PROXY_STATUS_PAGE = Path.of("src/main/resources/static/proxy-status.html");
    private static final Path REVERSE_PROXY_MODE = Path.of("docs/REVERSE_PROXY_MODE.md");
    private static final Path HEALTH_METRICS = Path.of("docs/REVERSE_PROXY_HEALTH_AND_METRICS.md");
    private static final Path STATUS_UI = Path.of("docs/PROXY_OPERATOR_STATUS_UI.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path HARDENING = Path.of("docs/DEPLOYMENT_HARDENING_GUIDE.md");
    private static final Pattern CLOUD_MANAGER_CONSTRUCTION =
            Pattern.compile("new\\s+" + "CloudManager\\s*\\(|" + "CloudManager\\s*\\(");
    private static final Pattern RELEASE_COMMAND =
            Pattern.compile("(?im)^\\s*(gh\\s+release|git\\s+tag)\\b");

    @Test
    void reloadStatusFieldsAndEndpointAreAdditiveAndOperatorProtected() throws Exception {
        String statusResponse = read(STATUS_RESPONSE);
        String reloadResponse = read(RELOAD_RESPONSE);
        String controller = read(STATUS_CONTROLLER);
        String security = read(API_SECURITY);

        assertTrue(statusResponse.contains("ReloadStatus"));
        assertTrue(statusResponse.contains("activeConfigGeneration"));
        assertTrue(statusResponse.contains("lastReloadValidationErrors"));
        assertTrue(reloadResponse.contains("ReverseProxyReloadResponse"));
        assertTrue(controller.contains("@PostMapping(\"/reload\")"));
        assertTrue(controller.contains("X-API-Key is required for proxy config reload"));
        assertTrue(security.contains("HttpMethod.POST, \"/api/proxy/reload\""));
    }

    @Test
    void reloadImplementationFailsSafeAndDoesNotSupportExternalConfigBackends() throws Exception {
        String service = read(REVERSE_PROXY_SERVICE);
        String normalized = service.toLowerCase(Locale.ROOT);

        assertTrue(service.contains("AtomicReference<ActiveProxyConfig>"));
        assertTrue(service.contains("activeConfig.set(candidateConfig)"));
        assertTrue(service.contains("ReloadState.failure"));
        assertTrue(service.contains("previousConfig"));
        assertTrue(service.contains("loadbalancerpro.proxy.enabled must be true for runtime reload"));
        assertFalse(normalized.contains("spring cloud config"));
        assertFalse(normalized.contains("consul"));
        assertFalse(normalized.contains("etcd"));
        assertFalse(normalized.contains("configserver"));
        assertFalse(normalized.contains("webclient"));
        assertFalse(normalized.contains("resttemplate"));
        assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(service).find());
    }

    @Test
    void statusPageDisplaysReloadStatusReadOnlyWithoutMutationControls() throws Exception {
        String page = read(PROXY_STATUS_PAGE);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("Reload supported"));
        assertTrue(page.contains("Config generation"));
        assertTrue(page.contains("Last reload status"));
        assertTrue(page.contains("Reload validation errors"));
        assertTrue(page.contains("status.reload"));
        assertFalse(normalized.contains("method: \"post\""));
        assertFalse(normalized.contains("/api/proxy/reload"));
        assertFalse(normalized.contains("x-api-key"));
    }

    @Test
    void docsExplainLocalReloadBoundaryAndAvoidInflatedClaims() throws Exception {
        String reverseProxyMode = read(REVERSE_PROXY_MODE);
        String healthMetrics = read(HEALTH_METRICS);
        String statusUi = read(STATUS_UI);
        String runbook = read(RUNBOOK);
        String hardening = read(HARDENING);

        assertTrue(reverseProxyMode.contains("Operator Config Reload"));
        assertTrue(reverseProxyMode.contains("last known-good active config"));
        assertTrue(reverseProxyMode.contains("does not read remote URLs"));
        assertTrue(healthMetrics.contains("activeConfigGeneration"));
        assertTrue(statusUi.contains("Reload status is displayed read-only"));
        assertTrue(runbook.contains("/api/proxy/status.reload"));
        assertTrue(hardening.contains("Proxy config reload is local and operator-controlled"));

        for (Path path : List.of(REVERSE_PROXY_MODE, HEALTH_METRICS, STATUS_UI, RUNBOOK, HARDENING)) {
            String content = read(path);
            String normalized = content.toLowerCase(Locale.ROOT);
            assertFalse(normalized.contains("production-grade hot reload"), path + " must avoid inflated claims");
            assertFalse(normalized.contains("hot-reload certification"), path + " must avoid certification claims");
            assertFalse(normalized.contains("benchmark results"), path + " must avoid benchmark claims");
            assertFalse(RELEASE_COMMAND.matcher(content).find(), path + " must not add release/tag commands");
            assertFalse(content.contains("release-downloads"), path + " must not mutate release-downloads");
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
