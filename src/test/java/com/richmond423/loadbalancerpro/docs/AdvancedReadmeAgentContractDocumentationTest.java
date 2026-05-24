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

class AdvancedReadmeAgentContractDocumentationTest {
    private static final Path README = Path.of("README.md");
    private static final Path AGENTS = Path.of("AGENTS.md");
    private static final Path BUILD_CONTRACT = Path.of("BUILD_CONTRACT.md");
    private static final Path VERIFICATION_PROTOCOL = Path.of("docs/agent/VERIFICATION_PROTOCOL.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path FAILURE_LOG = Path.of("docs/agent/FAILURE_LOG.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AdvancedReadmeAgentContractDocumentationTest.java");

    @Test
    void readmeActsAsAdvancedTrustSurfaceAndLinksAgentContracts() throws Exception {
        String readme = read(README);
        String normalized = readme.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "Advanced README",
                "public trust surface",
                "human front door",
                "reviewer starting point",
                "high-level claim contract",
                "trust-boundary summary",
                "agent-visible context surface",
                "AGENTS.md",
                "BUILD_CONTRACT.md",
                "docs/agent/VERIFICATION_PROTOCOL.md",
                "docs/agent/SESSION_MANAGER.md",
                "docs/agent/FAILURE_LOG.md",
                "docs/REVIEWER_TRUST_MAP.md",
                "docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md",
                "docs/LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md")) {
            assertTrue(readme.contains(expected), "README should contain " + expected);
        }

        for (String expected : List.of(
                "not production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmark",
                "throughput/p95/p99",
                "replay/evidence/report/storage/export")) {
            assertTrue(normalized.contains(expected), "README should preserve boundary language for " + expected);
        }
    }

    @Test
    void readmeDoesNotOverclaimBoundaries() throws Exception {
        String normalized = read(README).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validation is complete",
                "real-tenant validation is complete",
                "runtime enforcement is implemented",
                "load testing is proven",
                "stress testing is proven",
                "benchmarking is proven",
                "throughput evidence is proven",
                "p95 evidence is proven",
                "p99 evidence is proven")) {
            assertFalse(normalized.contains(forbidden), "README must not overclaim " + forbidden);
        }
    }

    @Test
    void agentContractFilesExistAndCarryRequiredSections() throws Exception {
        String agents = read(AGENTS);
        String buildContract = read(BUILD_CONTRACT);
        String verification = read(VERIFICATION_PROTOCOL);
        String session = read(SESSION_MANAGER);
        String failureLog = read(FAILURE_LOG);

        for (String expected : List.of(
                "Codex",
                "Preserve safety boundaries",
                "Do not overclaim production readiness",
                "Respect docs/test-only scope",
                "Use focused verification first",
                "Use full verification before merge",
                "Keep local-lab claims bounded",
                "Do not add CI/Maven/Docker/Compose/runtime behavior",
                "Do not introduce secrets, external targets, cloud/tenant targets",
                "Report honestly")) {
            assertTrue(agents.contains(expected), "AGENTS.md should contain " + expected);
        }

        for (String expected : List.of(
                "## Goal",
                "## Context And Constraints",
                "## Deliverables",
                "## Verification Requirements",
                "## Evidence And Reporting",
                "## Stop Conditions",
                "## Scope Boundaries",
                "## Not-Proven Boundaries",
                "## Final Report Format")) {
            assertTrue(buildContract.contains(expected), "BUILD_CONTRACT.md should contain " + expected);
        }

        for (String expected : List.of(
                "Focused Failing Test Or Focused Doc Guard",
                "Relevant Focused Selector Bundle",
                "mvn -q test",
                "package checks",
                "diff checks",
                "enterprise lab package smoke",
                "Remote PR Checks",
                "Main Post-Merge Checks",
                "Do not accept stale, failed, cancelled, or pending required checks as green",
                "Do not claim fully green main while remote checks are pending")) {
            assertTrue(verification.contains(expected), "verification protocol should contain " + expected);
        }

        for (String expected : List.of(
                "## Current Branch",
                "## Current PR",
                "## Current Goal",
                "## Current Head SHA",
                "## What Changed",
                "## Checks Run",
                "## Blockers",
                "## Next Action",
                "## Recovery Notes")) {
            assertTrue(session.contains(expected), "session manager should contain " + expected);
        }

        for (String expected : List.of(
                "Date/time",
                "Branch/PR",
                "Failure type",
                "Failing check",
                "Suspected cause",
                "Fix attempted",
                "Result",
                "Follow-up action")) {
            assertTrue(failureLog.contains(expected), "failure log should contain " + expected);
        }
    }

    @Test
    void guardTestOnlyReadsTrackedFiles() throws Exception {
        String source = read(SOURCE);

        for (String forbidden : List.of(
                "Files." + "write",
                "Files." + "create",
                "Files." + "delete",
                "Process" + "Builder",
                "Runtime." + "getRuntime",
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
