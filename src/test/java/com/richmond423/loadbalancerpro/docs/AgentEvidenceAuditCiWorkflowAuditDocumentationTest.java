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
                "mvn -b -dskiptests package",
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
                "uses: actions/setup-java@c1e323688fd81a25caa38c78aa6df2d33d3e20d9",
                "java-version: '17'",
                "run: mvn -b -dskiptests dependency:tree",
                "run: mvn -b test",
                "verify zero skipped tests",
                "mvn -b jacoco:report",
                "uses: actions/upload-artifact@043fb46d1a93c77aae656e7c1c64a875d1fc6a0a",
                "mvn -b -dskiptests package",
                "org.cyclonedx:cyclonedx-maven-plugin:2.9.1",
                "--lase-demo=healthy",
                "--lase-demo=overloaded",
                "127.0.0.1:18080",
                "docker build -t loadbalancerpro:ci .",
                "127.0.0.1:18081:8080",
                "uses: aquasecurity/trivy-action@ed142fd0673e97e23eac54620cfb913e5ce36c25",
                "uses: actions/dependency-review-action@56339e523c0409420f6c2c9a2f4292bbb3c07dd3",
                "fail-on-severity: high")) {
            assertTrue(ci.contains(expected), "Missing audited CI workflow control: " + expected);
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
