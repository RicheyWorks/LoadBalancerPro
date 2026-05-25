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

class AgentEvidenceAuditRepositoryEvidenceMapDocumentationTest {
    private static final Path MAP = Path.of("docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path BOARD = Path.of("docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentEvidenceAuditRepositoryEvidenceMapDocumentationTest.java");

    @Test
    void repositoryEvidenceMapExistsAndNamesSlotThreeScope() throws IOException {
        String map = read(MAP).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "slot 3",
                "repository evidence map",
                "documentation/test-only",
                "richeyworks/loadbalancerpro",
                "codex/evidence-audit-repository-map",
                "7dd64becaefd589ff94ed2fea93b017397b4a747",
                "reviewer navigation",
                "later campaign slots perform deeper")) {
            assertTrue(map.contains(expected), "Missing slot 3 evidence-map scope: " + expected);
        }
    }

    @Test
    void repositoryEvidenceMapCoversRequiredSurfaces() throws IOException {
        String map = read(MAP).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "readme.md",
                "docs/reviewer_trust_map.md",
                "agents.md",
                "build_contract.md",
                ".github/workflows/ci.yml",
                ".github/workflows/codeql.yml",
                ".github/workflows/release-artifacts.yml",
                "pom.xml",
                "dockerfile",
                "lab/docker-compose/local-lab-compose.yml",
                "scripts/smoke/enterprise-lab-workflow.ps1",
                "src/main/resources/application.properties",
                "src/main/resources/application-prod.properties",
                "docs/agent/evidence_audit_campaign_board.md",
                "session_manager.md",
                "failure_log.md",
                "evidence_audit_open_pr_hygiene.md")) {
            assertTrue(map.contains(expected), "Missing mapped evidence surface: " + expected);
        }
    }

    @Test
    void readmeAndTrustMapLinkToRepositoryEvidenceMap() throws IOException {
        String readme = read(README).toLowerCase(Locale.ROOT);
        String trustMap = read(TRUST_MAP).toLowerCase(Locale.ROOT);

        assertTrue(readme.contains("docs/agent/evidence_audit_repository_evidence_map.md"),
                "README should link to the repository evidence map");
        assertTrue(trustMap.contains("agent/evidence_audit_repository_evidence_map.md"),
                "Reviewer Trust Map should link to the repository evidence map");
    }

    @Test
    void campaignBoardAndSessionPreserveSlotThreeHistory() throws IOException {
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "slot 3",
                "repository evidence map",
                "codex/evidence-audit-repository-map",
                "#318",
                "e411c2fa6dc2c7d65c90093c3472dd30fd9a7bab",
                "65fad4a65f0297ba6e7d085bd84cacf5aa966f38",
                "post-merge main ci and codeql were green",
                "slot 3 branch created")) {
            assertTrue(board.contains(expected) || session.contains(expected),
                    "Missing slot 3 campaign checkpoint: " + expected);
        }
    }

    @Test
    void evidenceMapPreservesScopeAndNotProvenBoundaries() throws IOException {
        String map = read(MAP).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "without changing code, maven, ci, docker, compose, scripts, runtime resources, endpoints",
                "runner services",
                "automation",
                "secrets",
                "external targets",
                "production behavior",
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
            assertTrue(map.contains(expected), "Missing evidence-map boundary: " + expected);
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
