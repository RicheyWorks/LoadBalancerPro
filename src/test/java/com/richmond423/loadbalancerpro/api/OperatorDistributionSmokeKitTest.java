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

class OperatorDistributionSmokeKitTest {
    private static final Path DISTRIBUTION_SMOKE_DOC = Path.of("docs/OPERATOR_DISTRIBUTION_SMOKE_KIT.md");
    private static final Path OPERATOR_PACKAGING_DOC = Path.of("docs/OPERATOR_PACKAGING.md");
    private static final Path PROXY_DEMO_STACK_DOC = Path.of("docs/PROXY_DEMO_STACK.md");
    private static final Path FIXTURE_LAUNCHER_DOC = Path.of("docs/PROXY_DEMO_FIXTURE_LAUNCHER.md");
    private static final Path README = Path.of("README.md");
    private static final Path POWERSHELL_SMOKE_SCRIPT = Path.of("scripts/operator-distribution-smoke.ps1");
    private static final Path UNIX_SMOKE_SCRIPT = Path.of("scripts/operator-distribution-smoke.sh");
    private static final Path DEFAULT_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final Path PROXY_STATUS_PAGE = Path.of("src/main/resources/static/proxy-status.html");
    private static final Path COCKPIT_PAGE = Path.of("src/main/resources/static/load-balancing-cockpit.html");
    private static final List<Path> DEMO_PROFILES = List.of(
            Path.of("src/main/resources/application-proxy-demo-round-robin.properties"),
            Path.of("src/main/resources/application-proxy-demo-weighted-round-robin.properties"),
            Path.of("src/main/resources/application-proxy-demo-failover.properties"));
    private static final List<Path> REAL_BACKEND_EXAMPLES = List.of(
            Path.of("docs/examples/proxy/application-proxy-real-backend-example.properties"),
            Path.of("docs/examples/proxy/application-proxy-real-backend-weighted-example.properties"),
            Path.of("docs/examples/proxy/application-proxy-real-backend-failover-example.properties"));

    private static final Pattern PUBLIC_EXTERNAL_URL =
            Pattern.compile("https?://(?!127\\.0\\.0\\.1(?::|/|$)|localhost(?::|/|$))[^\\s\"'`]+");
    private static final Pattern SECRET_ASSIGNMENT =
            Pattern.compile("(?im)^\\s*[^#\\r\\n]*(password|secret|token|api[-_]?key|x-api-key)\\s*=");

    @Test
    void distributionSmokeDocDocumentsReleaseFreePackagedJarAndStatusPath() throws Exception {
        String doc = read(DISTRIBUTION_SMOKE_DOC);

        assertTrue(doc.contains("# Operator Distribution Smoke Kit"));
        assertTrue(doc.contains("No tags, releases, or assets are created"));
        assertTrue(doc.contains("mvn -B -DskipTests package"));
        assertTrue(doc.contains("java -jar target/LoadBalancerPro-2.4.2.jar"));
        assertTrue(doc.contains("compile exec:java"));
        assertTrue(doc.contains("ProxyDemoFixtureLauncher"));
        assertTrue(doc.contains("/proxy-status.html"));
        assertTrue(doc.contains("/api/proxy/status"));
        assertTrue(doc.contains("BOOT-INF/classes/static/proxy-status.html"));
        assertTrue(doc.contains("BOOT-INF/classes/static/load-balancing-cockpit.html"));
        assertTrue(doc.contains("X-LoadBalancerPro-Upstream"));
        assertTrue(doc.contains("X-LoadBalancerPro-Strategy"));
        assertTrue(doc.contains("proxy-demo-round-robin"));
        assertTrue(doc.contains("proxy-demo-weighted-round-robin"));
        assertTrue(doc.contains("proxy-demo-failover"));
        assertTrue(doc.contains("docs/examples/proxy/application-proxy-real-backend-example.properties"));
        assertTrue(doc.contains("docs/examples/proxy/application-proxy-real-backend-weighted-example.properties"));
        assertTrue(doc.contains("docs/examples/proxy/application-proxy-real-backend-failover-example.properties"));
        assertTrue(doc.contains("packaged-artifact-smoke"));
        assertTrue(doc.contains("artifact-smoke-summary.txt"));
        assertTrue(doc.contains("artifact-sha256.txt"));
        assertTrue(doc.contains("jar-resource-list.txt"));
        assertNoUnsafeDistributionContent(doc, DISTRIBUTION_SMOKE_DOC);
    }

    @Test
    void smokeHelpersExistAndAvoidReleaseCommands() throws Exception {
        String powershell = read(POWERSHELL_SMOKE_SCRIPT);
        String unix = read(UNIX_SMOKE_SCRIPT);
        String combined = powershell + "\n" + unix;
        String normalized = combined.toLowerCase(Locale.ROOT);

        assertTrue(powershell.contains("operator distribution smoke kit"));
        assertTrue(unix.contains("operator distribution smoke kit"));
        assertTrue(combined.contains("mvn -B -DskipTests package"));
        assertTrue(combined.contains("ProxyDemoFixtureLauncher"));
        assertTrue(combined.contains("/proxy-status.html"));
        assertTrue(combined.contains("/api/proxy/status"));
        assertTrue(combined.contains("application-proxy-demo-round-robin.properties"));
        assertTrue(combined.contains("application-proxy-demo-weighted-round-robin.properties"));
        assertTrue(combined.contains("application-proxy-demo-failover.properties"));
        assertTrue(combined.contains("application-proxy-real-backend-example.properties"));
        assertTrue(combined.contains("application-proxy-real-backend-weighted-example.properties"));
        assertTrue(combined.contains("application-proxy-real-backend-failover-example.properties"));
        assertFalse(normalized.contains("gh release"), "smoke helpers must not create or upload releases");
        assertFalse(normalized.contains("git tag"), "smoke helpers must not create tags");
        assertFalse(normalized.contains("softprops/action-gh-release"), "smoke helpers must not call release actions");
        assertFalse(normalized.contains("gh api") && normalized.contains("releases"),
                "smoke helpers must not call release APIs");
        assertFalse(normalized.contains("release-downloads"), "smoke helpers must not touch release evidence");
        assertNoUnsafeDistributionContent(combined, POWERSHELL_SMOKE_SCRIPT);
    }

    @Test
    void checkedDistributionAssetsAndProfilesRemainSafe() throws Exception {
        assertTrue(Files.exists(PROXY_STATUS_PAGE), "proxy status page should be packaged as a static resource");
        assertTrue(Files.exists(COCKPIT_PAGE), "browser cockpit should remain packaged as a static resource");

        String defaults = read(DEFAULT_PROPERTIES);
        assertTrue(defaults.contains("loadbalancerpro.proxy.enabled=false"));
        assertFalse(defaults.contains("loadbalancerpro.proxy.enabled=true"));

        for (Path profile : DEMO_PROFILES) {
            String content = read(profile);
            assertTrue(content.contains("loadbalancerpro.proxy.enabled=true"),
                    profile + " should opt in only when explicitly selected");
            assertTrue(content.contains("http://127.0.0.1:18081"));
            assertTrue(content.contains("http://127.0.0.1:18082"));
            assertNoUnsafeDistributionContent(content, profile);
        }

        for (Path example : REAL_BACKEND_EXAMPLES) {
            String content = read(example);
            assertTrue(content.contains("http://localhost:9001"));
            assertTrue(content.contains("http://localhost:9002"));
            assertNoUnsafeDistributionContent(content, example);
        }
    }

    @Test
    void docsPointToDistributionSmokeKitWithoutChangingReleaseOrProxyDefaults() throws Exception {
        String readme = read(README);
        String operatorPackaging = read(OPERATOR_PACKAGING_DOC);
        String demoStack = read(PROXY_DEMO_STACK_DOC);
        String fixtureLauncher = read(FIXTURE_LAUNCHER_DOC);

        assertTrue(readme.contains("OPERATOR_DISTRIBUTION_SMOKE_KIT.md"));
        assertTrue(operatorPackaging.contains("OPERATOR_DISTRIBUTION_SMOKE_KIT.md"));
        assertTrue(demoStack.contains("OPERATOR_DISTRIBUTION_SMOKE_KIT.md"));
        assertTrue(fixtureLauncher.contains("OPERATOR_DISTRIBUTION_SMOKE_KIT.md"));
        assertTrue(operatorPackaging.contains("Default application behavior remains unchanged"));
        assertTrue(operatorPackaging.contains("loadbalancerpro.proxy.enabled=false"));
        assertNoUnsafeDistributionContent(operatorPackaging, OPERATOR_PACKAGING_DOC);
        assertNoUnsafeDistributionContent(demoStack, PROXY_DEMO_STACK_DOC);
        assertNoUnsafeDistributionContent(fixtureLauncher, FIXTURE_LAUNCHER_DOC);
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void assertNoUnsafeDistributionContent(String content, Path source) {
        String normalized = content.toLowerCase(Locale.ROOT);
        assertFalse(PUBLIC_EXTERNAL_URL.matcher(content).find(), source + " should not contain public URLs");
        assertFalse(SECRET_ASSIGNMENT.matcher(content).find(), source + " should not contain secret assignments");
        assertFalse(normalized.contains("new cloudmanager"), source + " should not construct CloudManager");
        assertFalse(normalized.contains("cloudmanager("), source + " should not construct CloudManager");
        assertFalse(normalized.contains("amazonaws"), source + " should not target AWS endpoints");
        assertFalse(normalized.contains("localstorage"), source + " should not use browser storage");
        assertFalse(normalized.contains("sessionstorage"), source + " should not use browser storage");
        assertFalse(normalized.contains("production-grade"), source + " should not add production-grade claims");
        assertFalse(normalized.contains("certification proof"), source + " should not add certification claims");
        assertFalse(normalized.contains("identity proof"), source + " should not add identity claims");
    }
}
