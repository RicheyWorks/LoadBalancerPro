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

class ProxyDemoStackUsabilityTest {
    private static final Path DEMO_STACK_DOC = Path.of("docs/PROXY_DEMO_STACK.md");
    private static final Path LAUNCHER_DOC = Path.of("docs/PROXY_DEMO_FIXTURE_LAUNCHER.md");
    private static final Path DEFAULT_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final Path ROUND_ROBIN_PROFILE =
            Path.of("src/main/resources/application-proxy-demo-round-robin.properties");
    private static final Path WEIGHTED_PROFILE =
            Path.of("src/main/resources/application-proxy-demo-weighted-round-robin.properties");
    private static final Path FAILOVER_PROFILE =
            Path.of("src/main/resources/application-proxy-demo-failover.properties");
    private static final Path POWERSHELL_SCRIPT = Path.of("scripts/proxy-demo.ps1");
    private static final Path UNIX_SCRIPT = Path.of("scripts/proxy-demo.sh");

    private static final Pattern NON_LOOPBACK_URL =
            Pattern.compile("https?://(?!127\\.0\\.0\\.1(?::|/|$)|localhost(?::|/|$))[^\\s\"'`]+");
    private static final Pattern SECRET_ASSIGNMENT =
            Pattern.compile("(?im)^\\s*[^#\\r\\n]*(password|secret|token|api[-_]?key|x-api-key)\\s*=");

    @Test
    void demoStackDocsExposeSingleOperatorPathAndEvidence() throws Exception {
        String doc = read(DEMO_STACK_DOC);
        String launcherDoc = read(LAUNCHER_DOC);

        assertTrue(doc.contains("# Proxy Demo Stack"));
        assertTrue(doc.contains("ProxyDemoFixtureLauncher"));
        assertTrue(doc.contains("java -cp target/classes"));
        assertTrue(launcherDoc.contains("# Proxy Demo Fixture Launcher"));
        assertTrue(launcherDoc.contains("ProxyDemoFixtureLauncher"));
        assertTrue(launcherDoc.contains("--mode round-robin"));
        assertTrue(launcherDoc.contains("--mode weighted-round-robin"));
        assertTrue(launcherDoc.contains("--mode failover"));
        assertTrue(launcherDoc.contains("X-Fixture-Upstream"));
        assertTrue(launcherDoc.contains("X-LoadBalancerPro-Upstream"));
        assertTrue(launcherDoc.contains("X-LoadBalancerPro-Strategy"));
        assertTrue(launcherDoc.toLowerCase(Locale.ROOT).contains("no cloud"));
        assertTrue(launcherDoc.toLowerCase(Locale.ROOT).contains("no production gateway"));
        assertTrue(doc.contains(".\\scripts\\proxy-demo.ps1 -Mode round-robin"));
        assertTrue(doc.contains("bash scripts/proxy-demo.sh --mode round-robin"));
        assertTrue(doc.contains("proxy-demo-round-robin"));
        assertTrue(doc.contains("proxy-demo-weighted-round-robin"));
        assertTrue(doc.contains("proxy-demo-failover"));
        assertTrue(doc.contains("/proxy-status.html"));
        assertTrue(doc.contains("/api/proxy/status"));
        assertTrue(doc.contains("X-LoadBalancerPro-Upstream"));
        assertTrue(doc.contains("X-LoadBalancerPro-Strategy"));
        assertTrue(doc.toLowerCase(Locale.ROOT).contains("cleanup"));
        assertTrue(doc.toLowerCase(Locale.ROOT).contains("no cloud"));
        assertTrue(doc.toLowerCase(Locale.ROOT).contains("no production gateway"));
        assertTrue(doc.toLowerCase(Locale.ROOT).contains("not benchmark")
                || doc.toLowerCase(Locale.ROOT).contains("no benchmark"));
        assertNoUnsafeDemoContent(launcherDoc, LAUNCHER_DOC);
    }

    @Test
    void demoProfilesExistAndStayLoopbackOnly() throws Exception {
        List<Path> profiles = List.of(ROUND_ROBIN_PROFILE, WEIGHTED_PROFILE, FAILOVER_PROFILE);

        for (Path profile : profiles) {
            String content = read(profile);
            assertTrue(content.contains("server.address=127.0.0.1"), profile + " should bind locally");
            assertTrue(content.contains("loadbalancerpro.proxy.enabled=true"),
                    profile + " should be explicit proxy demo config");
            assertTrue(content.contains("loadbalancerpro.proxy.upstreams[0].url=http://127.0.0.1:18081"),
                    profile + " should use backend-a loopback URL");
            assertTrue(content.contains("loadbalancerpro.proxy.upstreams[1].url=http://127.0.0.1:18082"),
                    profile + " should use backend-b loopback URL");
            assertNoUnsafeDemoContent(content, profile);
        }

        assertTrue(read(ROUND_ROBIN_PROFILE).contains("loadbalancerpro.proxy.strategy=ROUND_ROBIN"));
        String weighted = read(WEIGHTED_PROFILE);
        assertTrue(weighted.contains("loadbalancerpro.proxy.strategy=WEIGHTED_ROUND_ROBIN"));
        assertTrue(weighted.contains("loadbalancerpro.proxy.upstreams[0].weight=3.0"));
        assertTrue(weighted.contains("loadbalancerpro.proxy.upstreams[1].weight=1.0"));
        String failover = read(FAILOVER_PROFILE);
        assertTrue(failover.contains("loadbalancerpro.proxy.strategy=ROUND_ROBIN"));
        assertTrue(failover.contains("loadbalancerpro.proxy.health-check.enabled=true"));
    }

    @Test
    void defaultApplicationPropertiesKeepsProxyDisabled() throws Exception {
        String defaults = read(DEFAULT_PROPERTIES);

        assertTrue(defaults.contains("loadbalancerpro.proxy.enabled=false"));
        assertFalse(defaults.contains("loadbalancerpro.proxy.enabled=true"));
    }

    @Test
    void demoScriptsExposeModesAndAvoidCloudStorageOrMutationControls() throws Exception {
        String powershell = read(POWERSHELL_SCRIPT);
        String unix = read(UNIX_SCRIPT);
        String combined = powershell + "\n" + unix;

        assertTrue(powershell.contains("[ValidateSet(\"round-robin\", \"weighted-round-robin\", \"failover\", \"status\")]"));
        assertTrue(unix.contains("round-robin|weighted-round-robin|failover|status"));
        assertTrue(combined.contains("proxy-demo-round-robin"));
        assertTrue(combined.contains("proxy-demo-weighted-round-robin"));
        assertTrue(combined.contains("proxy-demo-failover"));
        assertTrue(combined.contains("ProxyDemoFixtureLauncher"));
        assertTrue(combined.contains("java -cp target/classes"));
        assertTrue(combined.contains("proxy-status.html"));
        assertTrue(combined.contains("/api/proxy/status"));
        assertTrue(combined.contains("X-Fixture-Upstream"));
        assertTrue(combined.contains("/proxy/weighted?step=1"));
        assertTrue(combined.contains("/proxy/failover?step=1"));
        assertFalse(unix.contains("python3"));
        assertNoUnsafeDemoContent(combined, POWERSHELL_SCRIPT);
        assertFalse(combined.toLowerCase(Locale.ROOT).contains("reset metrics"));
        assertFalse(combined.toLowerCase(Locale.ROOT).contains("reset cooldown"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void assertNoUnsafeDemoContent(String content, Path source) {
        String normalized = content.toLowerCase(Locale.ROOT);
        assertFalse(NON_LOOPBACK_URL.matcher(content).find(), source + " should not contain external URLs");
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
