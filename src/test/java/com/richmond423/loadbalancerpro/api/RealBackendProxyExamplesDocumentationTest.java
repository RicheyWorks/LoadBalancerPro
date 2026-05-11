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

class RealBackendProxyExamplesDocumentationTest {
    private static final Path GUIDE = Path.of("docs/REAL_BACKEND_PROXY_EXAMPLES.md");
    private static final Path README = Path.of("README.md");
    private static final Path REVERSE_PROXY_MODE = Path.of("docs/REVERSE_PROXY_MODE.md");
    private static final Path HEALTH_METRICS = Path.of("docs/REVERSE_PROXY_HEALTH_AND_METRICS.md");
    private static final Path RESILIENCE = Path.of("docs/REVERSE_PROXY_RESILIENCE.md");
    private static final Path DEMO_STACK = Path.of("docs/PROXY_DEMO_STACK.md");
    private static final Path STRATEGY_LAB = Path.of("docs/PROXY_STRATEGY_DEMO_LAB.md");
    private static final Path STATUS_UI = Path.of("docs/PROXY_OPERATOR_STATUS_UI.md");
    private static final Path INSTALL_MATRIX = Path.of("docs/OPERATOR_INSTALL_RUN_MATRIX.md");
    private static final Path PACKAGING = Path.of("docs/OPERATOR_PACKAGING.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path TESTING = Path.of("docs/TESTING_COVERAGE.md");
    private static final Path API_SECURITY = Path.of("docs/API_SECURITY.md");
    private static final Path RELEASE_INTENT = Path.of("docs/RELEASE_INTENT_CHECKLIST.md");
    private static final Path DEFAULT_PROPERTIES = Path.of("src/main/resources/application.properties");

    private static final List<Path> EXAMPLES = List.of(
            Path.of("docs/examples/proxy/application-proxy-real-backend-example.properties"),
            Path.of("docs/examples/proxy/application-proxy-real-backend-round-robin-example.properties"),
            Path.of("docs/examples/proxy/application-proxy-real-backend-weighted-example.properties"),
            Path.of("docs/examples/proxy/application-proxy-real-backend-failover-example.properties"),
            Path.of("docs/examples/proxy/application-proxy-real-backend-resilience-example.properties"));

    private static final Pattern PUBLIC_EXTERNAL_URL =
            Pattern.compile("https?://(?!127\\.0\\.0\\.1(?::|/|$)|localhost(?::|/|$))[^\\s\"'`]+");
    private static final Pattern SECRET_ASSIGNMENT =
            Pattern.compile("(?im)^\\s*[^#\\r\\n]*(password|secret|token|credential|api[-_]?key|x-api-key)\\s*=");
    private static final Pattern CLOUD_CONFIG_ASSIGNMENT =
            Pattern.compile("(?im)^\\s*[^#\\r\\n]*(aws|amazonaws|cloud\\.)[^=]*=");

    @Test
    void realBackendGuideDocumentsCopyAdaptVerificationAndEvidenceLinks() throws Exception {
        String guide = read(GUIDE);

        assertTrue(guide.contains("# Real-Backend Proxy Examples"));
        assertTrue(guide.contains("local or private HTTP services"));
        assertTrue(guide.contains("ROUND_ROBIN Example"));
        assertTrue(guide.contains("WEIGHTED_ROUND_ROBIN Example"));
        assertTrue(guide.contains("Health-Aware Failover Example"));
        assertTrue(guide.contains("Retry And Cooldown Example"));
        assertTrue(guide.contains("X-LoadBalancerPro-Upstream"));
        assertTrue(guide.contains("X-LoadBalancerPro-Strategy"));
        assertTrue(guide.contains("/proxy-status.html"));
        assertTrue(guide.contains("/api/proxy/status"));
        assertTrue(guide.contains("CI_ARTIFACT_CONSUMER_GUIDE.md"));
        assertTrue(guide.contains("LOCAL_ARTIFACT_VERIFICATION.md"));
        assertTrue(guide.contains("RELEASE_CANDIDATE_DRY_RUN.md"));
        assertTrue(guide.contains("RELEASE_INTENT_CHECKLIST.md"));
        assertNoUnsafeContent(guide, GUIDE);
    }

    @Test
    void realBackendExampleProfilesExistAndUseOnlySafePlaceholders() throws Exception {
        for (Path example : EXAMPLES) {
            String content = read(example);

            assertTrue(content.contains("loadbalancerpro.proxy.enabled=true"),
                    example + " should require explicit proxy opt-in");
            assertTrue(content.contains("http://localhost:9001"),
                    example + " should use backend-a loopback placeholder");
            assertTrue(content.contains("http://localhost:9002"),
                    example + " should use backend-b loopback placeholder");
            assertTrue(content.contains("loadbalancerpro.proxy.health-check.path=/health"),
                    example + " should document health endpoint expectations");
            assertNoUnsafeContent(content, example);
            assertFalse(CLOUD_CONFIG_ASSIGNMENT.matcher(content).find(), example + " should not contain cloud config");
        }
    }

    @Test
    void realBackendProfilesCoverRoundRobinWeightedFailoverAndResilience() throws Exception {
        String roundRobin = read(Path.of("docs/examples/proxy/application-proxy-real-backend-round-robin-example.properties"));
        String weighted = read(Path.of("docs/examples/proxy/application-proxy-real-backend-weighted-example.properties"));
        String failover = read(Path.of("docs/examples/proxy/application-proxy-real-backend-failover-example.properties"));
        String resilience = read(Path.of("docs/examples/proxy/application-proxy-real-backend-resilience-example.properties"));

        assertTrue(roundRobin.contains("loadbalancerpro.proxy.strategy=ROUND_ROBIN"));
        assertTrue(weighted.contains("loadbalancerpro.proxy.strategy=WEIGHTED_ROUND_ROBIN"));
        assertTrue(weighted.contains("loadbalancerpro.proxy.upstreams[0].weight=3.0"));
        assertTrue(weighted.contains("loadbalancerpro.proxy.upstreams[1].weight=1.0"));
        assertTrue(failover.contains("loadbalancerpro.proxy.health-check.enabled=true"));
        assertTrue(failover.contains("loadbalancerpro.proxy.health-check.interval=5s"));
        assertTrue(resilience.contains("loadbalancerpro.proxy.retry.enabled=true"));
        assertTrue(resilience.contains("loadbalancerpro.proxy.retry.retry-non-idempotent=false"));
        assertTrue(resilience.contains("loadbalancerpro.proxy.cooldown.enabled=true"));
    }

    @Test
    void defaultPropertiesKeepProxyDisabled() throws Exception {
        String defaults = read(DEFAULT_PROPERTIES);

        assertTrue(defaults.contains("loadbalancerpro.proxy.enabled=false"));
        assertFalse(defaults.contains("loadbalancerpro.proxy.enabled=true"));
    }

    @Test
    void operatorDocsLinkToRealBackendGuide() throws Exception {
        for (Path doc : List.of(README, REVERSE_PROXY_MODE, HEALTH_METRICS, RESILIENCE, DEMO_STACK,
                STRATEGY_LAB, STATUS_UI, INSTALL_MATRIX, PACKAGING, RUNBOOK, TESTING, API_SECURITY,
                RELEASE_INTENT)) {
            assertTrue(read(doc).contains("REAL_BACKEND_PROXY_EXAMPLES.md"), doc + " should link to guide");
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void assertNoUnsafeContent(String content, Path source) {
        String normalized = content.toLowerCase(Locale.ROOT);
        assertFalse(PUBLIC_EXTERNAL_URL.matcher(content).find(), source + " should not contain public external URLs");
        assertFalse(SECRET_ASSIGNMENT.matcher(content).find(), source + " should not contain secret assignments");
        assertFalse(normalized.contains("new cloudmanager"), source + " should not construct CloudManager");
        assertFalse(normalized.contains("cloudmanager("), source + " should not construct CloudManager");
        assertFalse(normalized.contains("production-grade"), source + " should not add production-grade claims");
        assertFalse(normalized.contains("benchmark proof"), source + " should not add benchmark proof claims");
        assertFalse(normalized.contains("certification proof"), source + " should not add certification proof claims");
    }
}
