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

class ReverseProxyObservabilityHardeningTest {
    private static final Path STATUS_RESPONSE =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyStatusResponse.java");
    private static final Path STATUS_SUMMARIES =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyStatusSummaries.java");
    private static final Path REVERSE_PROXY_SERVICE =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyService.java");
    private static final Path PROXY_STATUS_PAGE = Path.of("src/main/resources/static/proxy-status.html");
    private static final Path HEALTH_METRICS_DOC = Path.of("docs/REVERSE_PROXY_HEALTH_AND_METRICS.md");
    private static final Path STATUS_UI_DOC = Path.of("docs/PROXY_OPERATOR_STATUS_UI.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path SMOKE_DOC = Path.of("docs/DEPLOYMENT_SMOKE_KIT.md");
    private static final Pattern CLOUD_MANAGER_CONSTRUCTION =
            Pattern.compile("new\\s+" + "CloudManager\\s*\\(|" + "CloudManager\\s*\\(");
    private static final Pattern RELEASE_COMMAND =
            Pattern.compile("(?im)^\\s*(gh\\s+release|git\\s+tag)\\b");

    @Test
    void statusResponseDefinesAdditiveObservabilityAndSecurityBoundaryFields() throws Exception {
        String response = read(STATUS_RESPONSE);
        String summaries = read(STATUS_SUMMARIES);

        assertTrue(response.contains("ObservabilitySummary"));
        assertTrue(response.contains("SecurityBoundaryStatus"));
        assertTrue(response.contains("routeCount"));
        assertTrue(response.contains("backendTargetCount"));
        assertTrue(response.contains("cooldownActiveBackendCount"));
        assertTrue(response.contains("apiKeyConfigured"));
        assertTrue(response.contains("proxyStatusProtected"));
        assertTrue(summaries.contains("apiKeyConfigured reports presence only"));
        assertFalse(summaries.contains("configuredApiKey()"),
                "status should report only API-key presence, never a secret accessor");
    }

    @Test
    void proxyServiceEmitsStructuredStartupFailureRetryAndCooldownLogMarkers() throws Exception {
        String service = read(REVERSE_PROXY_SERVICE);

        assertTrue(service.contains("proxy.observability.startup"));
        assertTrue(service.contains("proxy.observability.route"));
        assertTrue(service.contains("proxy.forward.failure"));
        assertTrue(service.contains("proxy.forward.retry"));
        assertTrue(service.contains("proxy.forward.retryable_status"));
        assertTrue(service.contains("proxy.cooldown.activated"));
        assertFalse(service.contains("configuredApiKey"));
        assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(service).find(),
                "observability hardening must not construct CloudManager");
    }

    @Test
    void proxyStatusPageDisplaysAdditiveObservabilitySummaryWithoutExternalTelemetryOrCdn() throws Exception {
        String page = read(PROXY_STATUS_PAGE);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("Configured routes"));
        assertTrue(page.contains("Backend targets"));
        assertTrue(page.contains("Readiness signal"));
        assertTrue(page.contains("Access boundary"));
        assertTrue(page.contains("status.observability"));
        assertTrue(page.contains("status.securityBoundary"));
        assertFalse(normalized.contains("<script src="));
        assertFalse(normalized.contains("cdn."));
        assertFalse(normalized.contains("fonts.googleapis"));
        assertFalse(normalized.contains("localstorage"));
        assertFalse(normalized.contains("sessionstorage"));
    }

    @Test
    void docsExplainStatusSignalsSmokeInterpretationAndNoExternalTelemetry() throws Exception {
        String healthMetrics = read(HEALTH_METRICS_DOC);
        String statusUi = read(STATUS_UI_DOC);
        String runbook = read(RUNBOOK);
        String smoke = read(SMOKE_DOC);

        assertTrue(healthMetrics.contains("observability"));
        assertTrue(healthMetrics.contains("route count"));
        assertTrue(healthMetrics.contains("backend target count"));
        assertTrue(healthMetrics.contains("securityBoundary"));
        assertTrue(healthMetrics.contains("retry/cooldown/failure"));
        assertTrue(statusUi.contains("observability summary"));
        assertTrue(statusUi.contains("Access boundary"));
        assertTrue(runbook.contains("proxy.observability.startup"));
        assertTrue(runbook.contains("proxy.forward.failure"));
        assertTrue(smoke.contains("proxy.observability.startup"));
        assertTrue(smoke.contains("proxy.forward.retryable_status"));
        assertTrue(healthMetrics.contains("No external telemetry"));
    }

    @Test
    void observabilitySprintFilesAvoidReleaseMutationFakeEvidenceAndInflatedClaims() throws Exception {
        for (Path path : List.of(STATUS_RESPONSE, STATUS_SUMMARIES, REVERSE_PROXY_SERVICE, PROXY_STATUS_PAGE,
                HEALTH_METRICS_DOC, STATUS_UI_DOC, RUNBOOK, SMOKE_DOC)) {
            String content = read(path);
            String normalized = content.toLowerCase(Locale.ROOT);

            assertFalse(RELEASE_COMMAND.matcher(content).find(), path + " should not create tags or releases");
            assertFalse(content.contains("release-downloads"), path + " should not mutate release-downloads");
            assertFalse(content.contains("SHA256="), path + " should not add fake evidence hashes");
            assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(content).find(),
                    path + " must not construct CloudManager");
            assertFalse(normalized.contains("production-grade observability"),
                    path + " should not add production observability claims");
            assertFalse(normalized.contains("benchmark results"), path + " should not add benchmark claims");
            assertFalse(normalized.contains("certified observability"),
                    path + " should not add certification claims");
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
