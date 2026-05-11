package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class LocalArtifactVerificationTest {
    private static final Path LOCAL_ARTIFACT_DOC = Path.of("docs/LOCAL_ARTIFACT_VERIFICATION.md");
    private static final Path DISTRIBUTION_SMOKE_DOC = Path.of("docs/OPERATOR_DISTRIBUTION_SMOKE_KIT.md");
    private static final Path OPERATOR_PACKAGING_DOC = Path.of("docs/OPERATOR_PACKAGING.md");
    private static final Path WINDOWS_HELPER = Path.of("scripts/local-artifact-verify.ps1");
    private static final Path UNIX_HELPER = Path.of("scripts/local-artifact-verify.sh");
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
    void localArtifactVerificationDocCoversChecksumJarInspectionAndRunCommands() throws Exception {
        String doc = read(LOCAL_ARTIFACT_DOC);

        assertTrue(doc.contains("# Local Artifact Verification"));
        assertTrue(doc.contains("no tags, no GitHub releases, no release assets"));
        assertTrue(doc.contains("does not touch `release-downloads/`"));
        assertTrue(doc.contains("mvn -B -DskipTests package"));
        assertTrue(doc.contains("Get-FileHash -Algorithm SHA256"));
        assertTrue(doc.contains("sha256sum target/LoadBalancerPro-2.4.2.jar"));
        assertTrue(doc.contains("shasum -a 256 target/LoadBalancerPro-2.4.2.jar"));
        assertTrue(doc.contains("jar tf target/LoadBalancerPro-2.4.2.jar"));
        assertTrue(doc.contains("META-INF/MANIFEST.MF"));
        assertTrue(doc.contains("BOOT-INF/classes/static/proxy-status.html"));
        assertTrue(doc.contains("BOOT-INF/classes/static/load-balancing-cockpit.html"));
        assertTrue(doc.contains("BOOT-INF/classes/application-proxy-demo-round-robin.properties"));
        assertTrue(doc.contains("BOOT-INF/classes/application-proxy-demo-weighted-round-robin.properties"));
        assertTrue(doc.contains("BOOT-INF/classes/application-proxy-demo-failover.properties"));
        assertTrue(doc.contains("BOOT-INF/classes/com/richmond423/loadbalancerpro/demo/ProxyDemoFixtureLauncher.class"));
        assertTrue(doc.contains("java -jar target/LoadBalancerPro-2.4.2.jar"));
        assertTrue(doc.contains("compile exec:java"));
        assertTrue(doc.contains("ProxyDemoFixtureLauncher"));
        assertTrue(doc.contains("packaged-artifact-smoke"));
        assertTrue(doc.contains("artifact-sha256.txt"));
        assertTrue(doc.contains("jar-resource-list.txt"));
        assertNoUnsafeArtifactContent(doc, LOCAL_ARTIFACT_DOC);
    }

    @Test
    void artifactHelpersComputeChecksumsInspectJarEntriesAndAvoidReleaseCommands() throws Exception {
        String powershell = read(WINDOWS_HELPER);
        String unix = read(UNIX_HELPER);
        String combined = powershell + "\n" + unix;
        String normalized = combined.toLowerCase(Locale.ROOT);

        assertTrue(powershell.contains("Get-FileHash -Algorithm SHA256"));
        assertTrue(unix.contains("sha256sum"));
        assertTrue(unix.contains("shasum -a 256"));
        assertTrue(combined.contains("jar tf"));
        assertTrue(combined.contains("BOOT-INF/classes/static/proxy-status.html"));
        assertTrue(combined.contains("BOOT-INF/classes/static/load-balancing-cockpit.html"));
        assertTrue(combined.contains("BOOT-INF/classes/application-proxy-demo-round-robin.properties"));
        assertTrue(combined.contains("BOOT-INF/classes/application-proxy-demo-weighted-round-robin.properties"));
        assertTrue(combined.contains("BOOT-INF/classes/application-proxy-demo-failover.properties"));
        assertTrue(combined.contains("BOOT-INF/classes/com/richmond423/loadbalancerpro/demo/ProxyDemoFixtureLauncher.class"));
        assertTrue(combined.contains("java -jar"));
        assertTrue(combined.contains("exec:java"));
        assertTrue(combined.contains("packaged-artifact-smoke"));
        assertFalse(normalized.contains("gh release"), "artifact helpers must not create or upload releases");
        assertFalse(normalized.contains("git tag"), "artifact helpers must not create tags");
        assertFalse(normalized.contains("softprops/action-gh-release"), "artifact helpers must not call release actions");
        assertFalse(normalized.contains("release-downloads"), "artifact helpers must not touch release-downloads");
        assertNoUnsafeArtifactContent(combined, WINDOWS_HELPER);
    }

    @Test
    void packageResourcesAreAvailableOnTheTestClasspath() {
        assertClasspathResource("static/proxy-status.html");
        assertClasspathResource("static/load-balancing-cockpit.html");
        assertClasspathResource("application-proxy-demo-round-robin.properties");
        assertClasspathResource("application-proxy-demo-weighted-round-robin.properties");
        assertClasspathResource("application-proxy-demo-failover.properties");
        assertClasspathResource("com/richmond423/loadbalancerpro/demo/ProxyDemoFixtureLauncher.class");
    }

    @Test
    void sourceResourcesExamplesAndDefaultProxyConfigurationRemainSafe() throws Exception {
        assertTrue(Files.exists(PROXY_STATUS_PAGE));
        assertTrue(Files.exists(COCKPIT_PAGE));

        String defaults = read(DEFAULT_PROPERTIES);
        assertTrue(defaults.contains("loadbalancerpro.proxy.enabled=false"));
        assertFalse(defaults.contains("loadbalancerpro.proxy.enabled=true"));

        for (Path profile : DEMO_PROFILES) {
            assertTrue(Files.exists(profile), profile + " should exist");
            String content = read(profile);
            assertTrue(content.contains("loadbalancerpro.proxy.enabled=true"));
            assertTrue(content.contains("http://127.0.0.1:18081"));
            assertTrue(content.contains("http://127.0.0.1:18082"));
            assertNoUnsafeArtifactContent(content, profile);
        }

        for (Path example : REAL_BACKEND_EXAMPLES) {
            assertTrue(Files.exists(example), example + " should exist");
            String content = read(example);
            assertTrue(content.contains("http://localhost:9001"));
            assertTrue(content.contains("http://localhost:9002"));
            assertNoUnsafeArtifactContent(content, example);
        }
    }

    @Test
    void existingOperatorDocsPointToLocalArtifactVerification() throws Exception {
        assertTrue(read(DISTRIBUTION_SMOKE_DOC).contains("LOCAL_ARTIFACT_VERIFICATION.md"));
        assertTrue(read(OPERATOR_PACKAGING_DOC).contains("LOCAL_ARTIFACT_VERIFICATION.md"));
    }

    private static void assertClasspathResource(String resourceName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        assertNotNull(loader.getResource(resourceName), resourceName + " should be available on the test classpath");
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void assertNoUnsafeArtifactContent(String content, Path source) {
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
