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

class AgentWorkflowQuickstartDocumentationTest {
    private static final Path QUICKSTART = Path.of("docs/agent/AGENT_WORKFLOW_QUICKSTART.md");
    private static final Path README = Path.of("README.md");
    private static final Path AGENTS = Path.of("AGENTS.md");
    private static final Path BUILD_CONTRACT = Path.of("BUILD_CONTRACT.md");
    private static final Path VERIFICATION_PROTOCOL = Path.of("docs/agent/VERIFICATION_PROTOCOL.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path FAILURE_LOG = Path.of("docs/agent/FAILURE_LOG.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentWorkflowQuickstartDocumentationTest.java");

    @Test
    void quickstartExplainsAgentContractFileRoles() throws Exception {
        String quickstart = read(QUICKSTART);

        for (String expected : List.of(
                "README.md is the Advanced README / public trust surface",
                "AGENTS.md is the Codex/agent operating rules file",
                "BUILD_CONTRACT.md is the current task contract template",
                "VERIFICATION_PROTOCOL.md defines focused-vs-full verification",
                "SESSION_MANAGER.md tracks long-running session state",
                "FAILURE_LOG.md tracks failures and recovery",
                "human front door",
                "reviewer starting point",
                "trust-boundary summary",
                "high-level claim contract",
                "agent-visible context surface")) {
            assertTrue(quickstart.contains(expected), "quickstart should explain " + expected);
        }
    }

    @Test
    void quickstartPreservesVerificationAndRemoteCheckRules() throws Exception {
        String normalized = read(QUICKSTART).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "use focused checks while editing",
                "use full checks before merge",
                "do not claim green main while remote checks are pending",
                "do not accept failed, cancelled, stale required checks",
                "pending required checks as green",
                "current head sha",
                "merge commit")) {
            assertTrue(normalized.contains(expected), "quickstart should contain verification rule " + expected);
        }
    }

    @Test
    void quickstartPreservesNotProvenBoundariesAndStartupPrompt() throws Exception {
        String quickstart = read(QUICKSTART);
        String normalized = quickstart.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "preserve not-proven boundaries",
                "do not overclaim production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof")) {
            assertTrue(normalized.contains(expected), "quickstart should preserve boundary " + expected);
        }

        for (String expected : List.of(
                "Read README.md",
                "Read AGENTS.md",
                "Read BUILD_CONTRACT.md",
                "Read docs/agent/VERIFICATION_PROTOCOL.md",
                "Follow the requested scope exactly",
                "Update evidence honestly",
                "Stop if blocked, unsafe")) {
            assertTrue(quickstart.contains(expected), "quickstart startup prompt should contain " + expected);
        }
    }

    @Test
    void relatedDocsCrossLinkToQuickstart() throws Exception {
        for (Path path : List.of(
                README,
                AGENTS,
                BUILD_CONTRACT,
                VERIFICATION_PROTOCOL,
                SESSION_MANAGER,
                FAILURE_LOG,
                REVIEWER_TRUST_MAP)) {
            assertTrue(read(path).contains("AGENT_WORKFLOW_QUICKSTART.md"),
                    path + " should link to the agent workflow quickstart");
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
