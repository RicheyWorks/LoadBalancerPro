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

class AgentEvidenceAuditCodeqlDependencyReviewAuditDocumentationTest {
    private static final Path AUDIT = Path.of(
            "docs/agent/EVIDENCE_AUDIT_CODEQL_DEPENDENCY_REVIEW_AUDIT.md");
    private static final Path CODEQL = Path.of(".github/workflows/codeql.yml");
    private static final Path CI = Path.of(".github/workflows/ci.yml");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path EVIDENCE_MAP = Path.of("docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md");
    private static final Path BOARD = Path.of("docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentEvidenceAuditCodeqlDependencyReviewAuditDocumentationTest.java");

    @Test
    void auditExistsAndNamesSlotFiveScope() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "slot 5",
                "codeql",
                "dependency review",
                "documentation/test-only",
                ".github/workflows/codeql.yml",
                ".github/workflows/ci.yml",
                "codex/evidence-audit-codeql-dependency-review",
                "bc62bef7fb5843e2ab143a47a65f81dd6fc46f8f",
                "without changing workflow behavior",
                "not a workflow behavior change")) {
            assertTrue(audit.contains(expected), "Missing slot 5 audit scope: " + expected);
        }
    }

    @Test
    void auditCoversCodeqlAndDependencyReviewPosture() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "push",
                "pull_request",
                "scheduled scan",
                "workflow_dispatch",
                "actions: read",
                "contents: read",
                "security-events: write",
                "analyze java",
                "ubuntu-latest",
                "30",
                "java-kotlin",
                "manual build mode",
                "mvn -b -dskiptests package",
                "pull-requests: read",
                "fail-on-severity",
                "high",
                "trivy",
                "sbom",
                "remaining limits",
                "reviewer questions")) {
            assertTrue(audit.contains(expected), "Missing CodeQL/dependency-review posture: " + expected);
        }
    }

    @Test
    void auditedWorkflowSourcesStillExposeExpectedControls() throws IOException {
        String codeql = read(CODEQL).replace("\r\n", "\n").toLowerCase(Locale.ROOT);
        String ci = read(CI).replace("\r\n", "\n").toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "name: codeql",
                "branches:\n      - main",
                "cron: \"0 12 * * 1\"",
                "workflow_dispatch",
                "actions: read",
                "contents: read",
                "security-events: write",
                "name: analyze java",
                "timeout-minutes: 30",
                "github.repository == 'richeyworks/loadbalancerpro'",
                "language: [java-kotlin]",
                "uses: actions/checkout@de0fac2e4500dabe0009e67214ff5f5447ce83dd",
                "uses: actions/setup-java@be666c2fcd27ec809703dec50e508c2fdc7f6654",
                "java-version: '17'",
                "github/codeql-action/init@68bde559dea0fdcac2102bfdf6230c5f70eb485e",
                "build-mode: manual",
                "run: mvn -b -dskiptests package",
                "github/codeql-action/analyze@68bde559dea0fdcac2102bfdf6230c5f70eb485e")) {
            assertTrue(codeql.contains(expected), "Missing audited CodeQL control: " + expected);
        }

        for (String expected : List.of(
                "name: dependency review",
                "github.event_name == 'pull_request'",
                "github.repository == 'richeyworks/loadbalancerpro'",
                "contents: read",
                "pull-requests: read",
                "uses: actions/dependency-review-action@a1d282b36b6f3519aa1f3fc636f609c47dddb294",
                "fail-on-severity: high")) {
            assertTrue(ci.contains(expected), "Missing audited dependency-review control: " + expected);
        }
    }

    @Test
    void navigationAndCampaignStateReferenceCodeqlAudit() throws IOException {
        String readme = read(README).toLowerCase(Locale.ROOT);
        String trustMap = read(TRUST_MAP).toLowerCase(Locale.ROOT);
        String evidenceMap = read(EVIDENCE_MAP).toLowerCase(Locale.ROOT);
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION).toLowerCase(Locale.ROOT);

        assertTrue(readme.contains("docs/agent/evidence_audit_codeql_dependency_review_audit.md"),
                "README should link to the CodeQL/dependency-review audit");
        assertTrue(trustMap.contains("agent/evidence_audit_codeql_dependency_review_audit.md"),
                "Reviewer Trust Map should link to the CodeQL/dependency-review audit");
        assertTrue(evidenceMap.contains("evidence_audit_codeql_dependency_review_audit.md"),
                "repository evidence map should link to the CodeQL/dependency-review audit");

        for (String expected : List.of(
                "completed campaign prs: 4 / 20",
                "current pr slot: 5",
                "codex/evidence-audit-codeql-dependency-review",
                "pr #319 merged",
                "bc62bef7fb5843e2ab143a47a65f81dd6fc46f8f",
                "post-merge main ci and codeql were green",
                "slot 5 branch created")) {
            assertTrue(board.contains(expected) || session.contains(expected),
                    "Missing slot 5 campaign checkpoint: " + expected);
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
                "full vulnerability management",
                "incident response readiness",
                "remediation sla compliance",
                "broader automation")) {
            assertTrue(audit.contains(expected), "Missing CodeQL/dependency-review boundary: " + expected);
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
