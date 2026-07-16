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

class AgentEvidenceAuditDockerfileRuntimeAuditDocumentationTest {
    private static final String PATCHED_RUNTIME_IMAGE = "eclipse-temurin:17-jre-jammy@sha256:"
            + "475d8e96b4b2bfe08999e5e854755c773af1581acdf959a4545d88f0696a2339";
    private static final Path AUDIT = Path.of("docs/agent/EVIDENCE_AUDIT_DOCKERFILE_RUNTIME_AUDIT.md");
    private static final Path DOCKERFILE = Path.of("Dockerfile");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path EVIDENCE_MAP = Path.of("docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md");
    private static final Path BOARD = Path.of("docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentEvidenceAuditDockerfileRuntimeAuditDocumentationTest.java");

    @Test
    void auditExistsAndNamesSlotSevenScope() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "slot 7",
                "dockerfile runtime audit",
                "documentation/test-only",
                "dockerfile",
                "codex/evidence-audit-dockerfile-runtime",
                "06d800c478b308ef836b0ab01d8b641d8b1a35f0",
                "without changing dockerfile contents",
                "not a dockerfile behavior change",
                "not a container publishing lane",
                "not a container signing lane")) {
            assertTrue(audit.contains(expected), "Missing slot 7 audit scope: " + expected);
        }
    }

    @Test
    void auditCoversRequiredDockerfileRuntimePosture() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "digest-pinned base images",
                "maven:3-eclipse-temurin-26",
                "eclipse-temurin:17-jre-jammy",
                "maven builder image",
                "workdir /workspace",
                "dependency:go-offline",
                "spring-boot:repackage",
                "java 17 jre jammy",
                "non-root runtime user",
                "loadbalancer:loadbalancer",
                "spring_profiles_active=prod",
                "expose 8080",
                "healthcheck",
                "127.0.0.1",
                "/api/health",
                "entrypoint",
                "--server.address=0.0.0.0",
                "builder jdk versus runtime jdk parity question",
                "reviewer questions",
                "remaining limits")) {
            assertTrue(audit.contains(expected), "Missing Dockerfile runtime posture wording: " + expected);
        }
    }

    @Test
    void dockerfileStillDeclaresAuditedBuildAndRuntimeControls() throws IOException {
        String dockerfile = read(DOCKERFILE).replace("\r\n", "\n");
        String lower = dockerfile.toLowerCase(Locale.ROOT);

        assertTrue(Pattern.compile("FROM maven:3-eclipse-temurin-26@sha256:[0-9a-f]{64} AS build")
                .matcher(dockerfile).find(), "builder base image should remain digest pinned");
        assertTrue(Pattern.compile("FROM eclipse-temurin:17-jre-jammy@sha256:[0-9a-f]{64}")
                .matcher(dockerfile).find(), "runtime base image should remain digest pinned");
        assertTrue(dockerfile.contains("FROM " + PATCHED_RUNTIME_IMAGE),
                "runtime base image should retain the reviewed patched digest");
        assertTrue(read(AUDIT).contains("runtime image: `" + PATCHED_RUNTIME_IMAGE + "`"),
                "runtime audit should match the Dockerfile's reviewed patched digest");

        for (String expected : List.of(
                "workdir /workspace",
                "copy pom.xml .",
                "mvn -q -dskiptests dependency:go-offline",
                "copy src ./src",
                "mvn -q -dskiptests package spring-boot:repackage",
                "cp \"$jar\" /workspace/app.jar",
                "workdir /app",
                "apt-get install -y --no-install-recommends curl",
                "groupadd --system loadbalancer",
                "useradd --system --gid loadbalancer --home-dir /app --shell /usr/sbin/nologin loadbalancer",
                "copy --from=build --chown=loadbalancer:loadbalancer /workspace/app.jar app.jar",
                "env spring_profiles_active=prod",
                "user loadbalancer:loadbalancer",
                "expose 8080",
                "healthcheck --interval=30s --timeout=5s --start-period=20s --retries=3 cmd curl -fss -o /dev/null http://127.0.0.1:8080/api/health || exit 1",
                "entrypoint [\"java\", \"-jar\", \"/app/app.jar\"]",
                "cmd [\"--server.address=0.0.0.0\"]")) {
            assertTrue(lower.contains(expected), "Missing Dockerfile control: " + expected);
        }
    }

    @Test
    void navigationAndCampaignStateReferenceDockerfileAudit() throws IOException {
        String readme = read(README).toLowerCase(Locale.ROOT);
        String trustMap = read(TRUST_MAP).toLowerCase(Locale.ROOT);
        String evidenceMap = read(EVIDENCE_MAP).toLowerCase(Locale.ROOT);
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION).toLowerCase(Locale.ROOT);

        assertTrue(readme.contains("docs/agent/evidence_audit_dockerfile_runtime_audit.md"),
                "README should link to the Dockerfile runtime audit");
        assertTrue(trustMap.contains("agent/evidence_audit_dockerfile_runtime_audit.md"),
                "Reviewer Trust Map should link to the Dockerfile runtime audit");
        assertTrue(evidenceMap.contains("evidence_audit_dockerfile_runtime_audit.md"),
                "repository evidence map should link to the Dockerfile runtime audit");

        for (String expected : List.of(
                "slot 7 result",
                "codex/evidence-audit-dockerfile-runtime",
                "#322",
                "933717e7fe5a59004353fb90f0718ba8b5ecd6ef",
                "399f83ba0fec96542c544643ad214d8e4937072d",
                "dockerfile runtime posture audited",
                "post-merge main ci and codeql green")) {
            assertTrue(board.contains(expected) || session.contains(expected),
                    "Missing slot 7 campaign checkpoint: " + expected);
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
                "vulnerability remediation completeness",
                "incident response readiness",
                "remediation sla compliance",
                "broader automation")) {
            assertTrue(audit.contains(expected), "Missing Dockerfile audit boundary: " + expected);
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
