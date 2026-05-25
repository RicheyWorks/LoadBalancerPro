package com.richmond423.loadbalancerpro.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class AgentEvidenceAuditComposeLocalLabAuditDocumentationTest {
    private static final Path AUDIT = Path.of("docs/agent/EVIDENCE_AUDIT_COMPOSE_LOCAL_LAB_AUDIT.md");
    private static final Path COMPOSE = Path.of("lab/docker-compose/local-lab-compose.yml");
    private static final Path TOXIPROXY_CONFIG = Path.of("lab/toxiproxy/local-lab-toxiproxy.json");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path EVIDENCE_MAP = Path.of("docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md");
    private static final Path BOARD = Path.of("docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentEvidenceAuditComposeLocalLabAuditDocumentationTest.java");

    @Test
    void auditExistsAndNamesSlotEightScope() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "slot 8",
                "compose local-lab audit",
                "documentation/test-only",
                "lab/docker-compose/local-lab-compose.yml",
                "codex/evidence-audit-compose-local-lab",
                "399f83ba0fec96542c544643ad214d8e4937072d",
                "without changing compose behavior",
                "does not edit",
                "does not run docker",
                "does not run compose",
                "does not add services")) {
            assertTrue(audit.contains(expected), "Missing slot 8 audit scope: " + expected);
        }
    }

    @Test
    void auditCoversRequiredComposeAndLocalLabBoundaries() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "app-under-test",
                "toxiproxy",
                "eclipse-temurin:21-jre",
                "shopify/toxiproxy:2.12.0",
                "127.0.0.1:8080:8080",
                "127.0.0.1:8474:8474",
                "127.0.0.1:18080:18080",
                "127.0.0.1:18081:18081",
                "../../target:/opt/loadbalancerpro:ro",
                "local-lab-toxiproxy.json",
                "no k6 runner service exists",
                "no bruno runner service exists",
                "not ci-gated",
                "not maven-wired",
                "java 21 compose runtime versus java 17 project/runtime parity question",
                "reviewer questions",
                "remaining limits")) {
            assertTrue(audit.contains(expected), "Missing Compose/local-lab boundary wording: " + expected);
        }
    }

    @Test
    void composeFileStillMatchesAuditedLocalLabShape() throws IOException {
        String compose = read(COMPOSE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "app-under-test:",
                "image: eclipse-temurin:21-jre",
                "working_dir: /opt/loadbalancerpro",
                "command: [\"java\", \"-jar\", \"/opt/loadbalancerpro/loadbalancerpro-2.5.0.jar\"]",
                "\"127.0.0.1:8080:8080\"",
                "\"../../target:/opt/loadbalancerpro:ro\"",
                "local-lab-scope=local-lab-only",
                "local-lab-execution=manual-only",
                "local-lab-ci=not-ci-gated",
                "local-lab-maven=not-wired",
                "local-lab-packaging=manual-package-first",
                "toxiproxy:",
                "image: shopify/toxiproxy:2.12.0",
                "toxiproxy-server",
                "\"127.0.0.1:8474:8474\"",
                "\"127.0.0.1:18080:18080\"",
                "\"127.0.0.1:18081:18081\"",
                "../toxiproxy/local-lab-toxiproxy.json:/etc/toxiproxy/local-lab-toxiproxy.json:ro")) {
            assertTrue(compose.contains(expected), "Missing Compose control: " + expected);
        }

        assertFalse(compose.contains("0.0.0.0"), "local-lab Compose must not expose 0.0.0.0 defaults");
        assertFalse(compose.contains("http://"), "local-lab Compose must not add external URL defaults");
        assertFalse(compose.contains("https://"), "local-lab Compose must not add external URL defaults");
        assertFalse(compose.contains("k6"), "local-lab Compose must not add a k6 runner service");
        assertFalse(compose.contains("bruno"), "local-lab Compose must not add a Bruno runner service");
        assertFalse(compose.contains("secret"), "local-lab Compose must not add secrets");
        assertFalse(compose.contains("credential"), "local-lab Compose must not add credentials");
    }

    @Test
    void toxiproxyConfigStillUsesLoopbackTargetsOnly() throws IOException {
        String config = read(TOXIPROXY_CONFIG).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "local-lab-app-loopback",
                "\"listen\": \"127.0.0.1:18080\"",
                "\"upstream\": \"127.0.0.1:8080\"",
                "local-lab-backend-loopback",
                "\"listen\": \"127.0.0.1:18081\"",
                "\"upstream\": \"127.0.0.1:18082\"",
                "\"toxics\": []")) {
            assertTrue(config.contains(expected), "Missing Toxiproxy loopback config: " + expected);
        }

        assertFalse(config.contains("0.0.0.0"), "Toxiproxy local-lab config must not expose 0.0.0.0");
        assertFalse(config.contains("http://"), "Toxiproxy local-lab config must not add external URLs");
        assertFalse(config.contains("https://"), "Toxiproxy local-lab config must not add external URLs");
        assertFalse(config.contains("secret"), "Toxiproxy local-lab config must not add secrets");
        assertFalse(config.contains("credential"), "Toxiproxy local-lab config must not add credentials");
    }

    @Test
    void navigationAndCampaignStateReferenceComposeLocalLabAudit() throws IOException {
        String readme = read(README).toLowerCase(Locale.ROOT);
        String trustMap = read(TRUST_MAP).toLowerCase(Locale.ROOT);
        String evidenceMap = read(EVIDENCE_MAP).toLowerCase(Locale.ROOT);
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION).toLowerCase(Locale.ROOT);

        assertTrue(readme.contains("docs/agent/evidence_audit_compose_local_lab_audit.md"),
                "README should link to the Compose/local-lab audit");
        assertTrue(trustMap.contains("agent/evidence_audit_compose_local_lab_audit.md"),
                "Reviewer Trust Map should link to the Compose/local-lab audit");
        assertTrue(evidenceMap.contains("evidence_audit_compose_local_lab_audit.md"),
                "repository evidence map should link to the Compose/local-lab audit");

        for (String expected : List.of(
                "completed campaign prs: 7 / 20",
                "current pr slot: 8",
                "codex/evidence-audit-compose-local-lab",
                "pr #322 merged",
                "933717e7fe5a59004353fb90f0718ba8b5ecd6ef",
                "399f83ba0fec96542c544643ad214d8e4937072d",
                "post-merge main ci and codeql were green",
                "slot 8 branch created")) {
            assertTrue(board.contains(expected) || session.contains(expected),
                    "Missing slot 8 campaign checkpoint: " + expected);
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
                "registry publication",
                "container signing",
                "production telemetry",
                "production monitoring",
                "broader automation")) {
            assertTrue(audit.contains(expected), "Missing Compose/local-lab audit boundary: " + expected);
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
