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

class AgentEvidenceAuditCiWorkflowAuditDocumentationTest {
    private static final Path AUDIT = Path.of("docs/agent/EVIDENCE_AUDIT_CI_WORKFLOW_AUDIT.md");
    private static final Path CI = Path.of(".github/workflows/ci.yml");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path EVIDENCE_MAP = Path.of("docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md");
    private static final Path BOARD = Path.of("docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentEvidenceAuditCiWorkflowAuditDocumentationTest.java");

    @Test
    void ciWorkflowAuditExistsAndNamesSlotFourScope() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "slot 4",
                "ci workflow audit",
                "documentation/test-only",
                ".github/workflows/ci.yml",
                "codex/evidence-audit-ci-workflow",
                "65fad4a65f0297ba6e7d085bd84cacf5aa966f38",
                "without changing workflow behavior",
                "not a ci behavior change",
                "not production hardening")) {
            assertTrue(audit.contains(expected), "Missing slot 4 CI audit scope: " + expected);
        }
    }

    @Test
    void ciWorkflowAuditCoversRequiredWorkflowPosture() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "contents: read",
                "pull-requests: read",
                "pinned action",
                "actions/checkout",
                "actions/setup-java",
                "actions/upload-artifact",
                "aquasecurity/trivy-action",
                "actions/dependency-review-action",
                "dependency tree",
                "mvn -b test",
                "zero skipped tests",
                "jacoco",
                "mvn -b package",
                "packaged-artifact-smoke",
                "cyclonedx sbom",
                "lase demo",
                "packaged jar",
                "127.0.0.1:18080",
                "docker build",
                "127.0.0.1:18081:8080",
                "container dry-run evidence",
                "trivy",
                "dependency review",
                "retention-days: 30",
                "remaining limits")) {
            assertTrue(audit.contains(expected), "Missing CI workflow audit posture: " + expected);
        }
    }

    @Test
    void auditedWorkflowStillContainsSourceVisibleControls() throws IOException {
        String ci = read(CI).replace("\r\n", "\n").toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "permissions:\n  contents: read",
                "uses: actions/checkout@de0fac2e4500dabe0009e67214ff5f5447ce83dd",
                "uses: actions/setup-java@be666c2fcd27ec809703dec50e508c2fdc7f6654",
                "java-version: '17'",
                "run: mvn -b -dskiptests dependency:tree",
                "run: mvn -b test",
                "verify zero skipped tests",
                "mvn -b jacoco:report",
                "uses: actions/upload-artifact@043fb46d1a93c77aae656e7c1c64a875d1fc6a0a",
                "mvn -b package",
                "org.cyclonedx:cyclonedx-maven-plugin:2.9.1",
                "--lase-demo=healthy",
                "--lase-demo=overloaded",
                "127.0.0.1:18080",
                "docker build -t loadbalancerpro:ci .",
                "127.0.0.1:18081:8080",
                "uses: aquasecurity/trivy-action@ed142fd0673e97e23eac54620cfb913e5ce36c25",
                "uses: actions/dependency-review-action@a1d282b36b6f3519aa1f3fc636f609c47dddb294",
                "fail-on-severity: high")) {
            assertTrue(ci.contains(expected), "Missing audited CI workflow control: " + expected);
        }
    }

    @Test
    void navigationAndCampaignStateReferenceCiAudit() throws IOException {
        String readme = read(README).toLowerCase(Locale.ROOT);
        String trustMap = read(TRUST_MAP).toLowerCase(Locale.ROOT);
        String evidenceMap = read(EVIDENCE_MAP).toLowerCase(Locale.ROOT);
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION).toLowerCase(Locale.ROOT);

        assertTrue(readme.contains("docs/agent/evidence_audit_ci_workflow_audit.md"),
                "README should link to the CI workflow audit");
        assertTrue(trustMap.contains("agent/evidence_audit_ci_workflow_audit.md"),
                "Reviewer Trust Map should link to the CI workflow audit");
        assertTrue(evidenceMap.contains("evidence_audit_ci_workflow_audit.md"),
                "repository evidence map should link to the CI workflow audit");

        for (String expected : List.of(
                "slot 4",
                "ci workflow audit",
                "codex/evidence-audit-ci-workflow",
                "pr #319",
                "e1c40e904730a9e24875424aa312c68fc62d1fa3",
                "bc62bef7fb5843e2ab143a47a65f81dd6fc46f8f",
                "post-merge main ci and codeql were green")) {
            assertTrue(board.contains(expected) || session.contains(expected),
                    "Missing slot 4 campaign checkpoint: " + expected);
        }
    }

    @Test
    void ciWorkflowAuditPreservesNotProvenBoundaries() throws IOException {
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
                "release approval",
                "broader automation")) {
            assertTrue(audit.contains(expected), "Missing CI audit boundary: " + expected);
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
