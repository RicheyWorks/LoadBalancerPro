package com.richmond423.loadbalancerpro.docs;

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

class AgentEvidenceAuditProxyDemoFixtureAuditDocumentationTest {
    private static final Path AUDIT = Path.of("docs/agent/EVIDENCE_AUDIT_PROXY_DEMO_FIXTURE_AUDIT.md");
    private static final Path LAUNCHER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/demo/ProxyDemoFixtureLauncher.java");
    private static final Path POWERSHELL_SCRIPT = Path.of("scripts/proxy-demo.ps1");
    private static final Path UNIX_SCRIPT = Path.of("scripts/proxy-demo.sh");
    private static final Path ROUND_ROBIN_PROFILE =
            Path.of("src/main/resources/application-proxy-demo-round-robin.properties");
    private static final Path WEIGHTED_PROFILE =
            Path.of("src/main/resources/application-proxy-demo-weighted-round-robin.properties");
    private static final Path FAILOVER_PROFILE =
            Path.of("src/main/resources/application-proxy-demo-failover.properties");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path EVIDENCE_MAP = Path.of("docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md");
    private static final Path BOARD = Path.of("docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentEvidenceAuditProxyDemoFixtureAuditDocumentationTest.java");

    private static final Pattern NON_LOOPBACK_URL =
            Pattern.compile("https?://(?!127\\.0\\.0\\.1(?::|/|$)|localhost(?::|/|$))[^\\s\"'`)]+");
    private static final Pattern SECRET_ASSIGNMENT =
            Pattern.compile("(?im)^\\s*[^#\\r\\n]*(password|secret|token|api[-_]?key|x-api-key)\\s*=");

    @Test
    void auditExistsAndNamesSlotTenScope() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "slot 10",
                "proxy demo fixture audit",
                "documentation/test-only",
                "proxydemofixturelauncher.java",
                "scripts/proxy-demo.ps1",
                "scripts/proxy-demo.sh",
                "codex/evidence-audit-proxy-demo-fixture",
                "6f5d0d88502fb86fdc94f5261c709a2356dee65a",
                "ecc0dbca270ff4f6b96c1f41c4ca7c0037569681",
                "does not start the fixture",
                "does not run scripts",
                "does not call proxy endpoints",
                "does not claim production gateway readiness")) {
            assertTrue(audit.contains(expected), "Missing slot 10 audit scope: " + expected);
        }
    }

    @Test
    void auditCoversProxyDemoFixtureBoundaries() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "default fixture host is `127.0.0.1`",
                "`--host` accepts only `127.0.0.1` or `localhost`",
                "non-loopback hostnames",
                "default fixed ports",
                "`backend-a`: `127.0.0.1:18081`",
                "`backend-b`: `127.0.0.1:18082`",
                "server.address=127.0.0.1",
                "server.port=8080",
                "port-conflict behavior should be reviewed",
                "proxy-demo-round-robin",
                "proxy-demo-weighted-round-robin",
                "proxy-demo-failover",
                "loadbalancerpro.proxy.enabled=true",
                "default application profile keeps proxy mode disabled",
                "/fixture/health/fail",
                "/fixture/health/ok",
                "/health",
                "helper scripts are source-visible local helpers",
                "no cloud or external network claim",
                "reviewer questions",
                "remaining limits")) {
            assertTrue(audit.contains(expected), "Missing proxy demo audit wording: " + expected);
        }
    }

    @Test
    void proxyDemoFixtureSourceStillUsesLoopbackHostValidationAndFixedPorts() throws IOException {
        String source = read(LAUNCHER);
        String normalized = source.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "private static final string default_host = \"127.0.0.1\"",
                "private static final int default_backend_a_port = 18081",
                "private static final int default_backend_b_port = 18082",
                "\"--host must be 127.0.0.1 or localhost\"",
                "if (!\"127.0.0.1\".equals(normalized) && !\"localhost\".equals(normalized))",
                "\"backend-a\"",
                "\"backend-b\"",
                "\"/fixture/health/fail\"",
                "\"/fixture/health/ok\"",
                "\"/health\"",
                "failover mode starts backend-b health as failing",
                "does not require python, node, docker",
                "no production gateway")) {
            assertTrue(normalized.contains(expected), "Missing launcher source boundary: " + expected);
        }

        assertFalse(normalized.contains("new cloudmanager"), "fixture must not construct CloudManager");
        assertFalse(normalized.contains("cloudmanager("), "fixture must not construct CloudManager");
        assertFalse(NON_LOOPBACK_URL.matcher(source).find(), "fixture source must not contain external URL defaults");
        assertFalse(SECRET_ASSIGNMENT.matcher(source).find(), "fixture source must not assign secrets");
    }

    @Test
    void demoProfilesStayLoopbackOnlyAndExplicitlyOptInProxyMode() throws IOException {
        List<Path> profiles = List.of(ROUND_ROBIN_PROFILE, WEIGHTED_PROFILE, FAILOVER_PROFILE);

        for (Path profile : profiles) {
            String content = read(profile).toLowerCase(Locale.ROOT);
            assertTrue(content.contains("server.address=127.0.0.1"), profile + " should bind locally");
            assertTrue(content.contains("server.port=8080"), profile + " should use the demo app port");
            assertTrue(content.contains("loadbalancerpro.proxy.enabled=true"),
                    profile + " should be explicit demo proxy configuration");
            assertTrue(content.contains("loadbalancerpro.proxy.upstreams[0].url=http://127.0.0.1:18081"),
                    profile + " should use backend-a loopback URL");
            assertTrue(content.contains("loadbalancerpro.proxy.upstreams[1].url=http://127.0.0.1:18082"),
                    profile + " should use backend-b loopback URL");
            assertFalse(content.contains("0.0.0.0"), profile + " must not expose 0.0.0.0");
            assertFalse(NON_LOOPBACK_URL.matcher(content).find(), profile + " must not contain external URLs");
            assertFalse(SECRET_ASSIGNMENT.matcher(content).find(), profile + " must not assign secrets");
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
    void demoScriptsRemainLocalManualHelpers() throws IOException {
        String powershell = read(POWERSHELL_SCRIPT);
        String unix = read(UNIX_SCRIPT);
        String combined = powershell + "\n" + unix;
        String normalized = combined.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "round-robin",
                "weighted-round-robin",
                "failover",
                "status",
                "proxydemofixturelauncher",
                "java -cp target/classes",
                "proxy-status.html",
                "/api/proxy/status",
                "127.0.0.1:8080",
                "127.0.0.1:$backendaport",
                "127.0.0.1:$backendbport")) {
            assertTrue(normalized.contains(expected), "Missing proxy script local helper wording: " + expected);
        }

        assertFalse(NON_LOOPBACK_URL.matcher(combined).find(), "proxy demo scripts must not contain external URLs");
        assertFalse(SECRET_ASSIGNMENT.matcher(combined).find(), "proxy demo scripts must not assign secrets");
        assertFalse(normalized.contains("git tag"), "proxy demo scripts must not create release tags");
        assertFalse(normalized.contains("gh release"), "proxy demo scripts must not create GitHub releases");
    }

    @Test
    void navigationAndCampaignStateReferenceProxyDemoFixtureAudit() throws IOException {
        String readme = read(README).toLowerCase(Locale.ROOT);
        String trustMap = read(TRUST_MAP).toLowerCase(Locale.ROOT);
        String evidenceMap = read(EVIDENCE_MAP).toLowerCase(Locale.ROOT);
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION).toLowerCase(Locale.ROOT);

        assertTrue(readme.contains("docs/agent/evidence_audit_proxy_demo_fixture_audit.md"),
                "README should link to the proxy demo fixture audit");
        assertTrue(trustMap.contains("agent/evidence_audit_proxy_demo_fixture_audit.md"),
                "Reviewer Trust Map should link to the proxy demo fixture audit");
        assertTrue(evidenceMap.contains("evidence_audit_proxy_demo_fixture_audit.md"),
                "repository evidence map should link to the proxy demo fixture audit");

        for (String expected : List.of(
                "completed campaign prs: 9 / 20",
                "current pr slot: 10",
                "proxy demo fixture audit",
                "codex/evidence-audit-proxy-demo-fixture",
                "slot 9 result",
                "#324",
                "ecc0dbca270ff4f6b96c1f41c4ca7c0037569681",
                "6f5d0d88502fb86fdc94f5261c709a2356dee65a",
                "post-merge main ci and codeql were green")) {
            assertTrue(board.contains(expected) || session.contains(expected),
                    "Missing slot 10 campaign checkpoint: " + expected);
        }
    }

    @Test
    void auditPreservesNotProvenBoundaries() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not prove production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "production gateway readiness",
                "tls termination",
                "waf behavior",
                "identity integration",
                "production telemetry",
                "production monitoring",
                "registry publication",
                "container signing",
                "broader automation")) {
            assertTrue(audit.contains(expected), "Missing proxy demo audit boundary: " + expected);
        }
    }

    @Test
    void guardTestOnlyReadsTrackedFiles() throws IOException {
        String source = read(SOURCE);

        for (String forbidden : List.of(
                "Files." + "write",
                "Files." + "create",
                "Files." + "delete",
                "Process" + "Builder",
                "Runtime." + "getRuntime",
                ".ex" + "ec(",
                "Http" + "Client",
                "URL" + "Connection",
                "Socket" + "(")) {
            assertFalse(source.contains(forbidden), "guard test must not use " + forbidden);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
