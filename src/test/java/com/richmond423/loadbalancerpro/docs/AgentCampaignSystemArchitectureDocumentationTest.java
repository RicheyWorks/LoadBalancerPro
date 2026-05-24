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

class AgentCampaignSystemArchitectureDocumentationTest {
    private static final Path CAMPAIGN_ARCHITECTURE = Path.of("docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md");
    private static final Path README = Path.of("README.md");
    private static final Path AGENTS = Path.of("AGENTS.md");
    private static final Path BUILD_CONTRACT = Path.of("BUILD_CONTRACT.md");
    private static final Path QUICKSTART = Path.of("docs/agent/AGENT_WORKFLOW_QUICKSTART.md");
    private static final Path GOAL_PROTOCOL = Path.of("docs/agent/GOAL_MODE_LONG_RUN_PROTOCOL.md");
    private static final Path VERIFICATION_PROTOCOL = Path.of("docs/agent/VERIFICATION_PROTOCOL.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path FAILURE_LOG = Path.of("docs/agent/FAILURE_LOG.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentCampaignSystemArchitectureDocumentationTest.java");

    @Test
    void campaignArchitectureExistsAndDefinesTenPrLoop() throws Exception {
        String architecture = read(CAMPAIGN_ARCHITECTURE);

        for (String expected : List.of(
                "Campaign System Architecture",
                "ten successful merged PRs",
                "one scoped PR at a time",
                "from clean main",
                "Update SESSION_MANAGER.md",
                "FAILURE_LOG.md",
                "current-head remote required checks",
                "post-merge main checks")) {
            assertTrue(architecture.contains(expected), "campaign architecture should contain " + expected);
        }
    }

    @Test
    void campaignArchitecturePreservesFileRolesAndGoalProtocol() throws Exception {
        String architecture = read(CAMPAIGN_ARCHITECTURE);

        for (String expected : List.of(
                "README.md remains the constitutional layer and public trust surface",
                "AGENTS.md remains the operating rules file",
                "BUILD_CONTRACT.md remains the focused execution contract",
                "GOAL_MODE_LONG_RUN_PROTOCOL.md governs `/goal` lifecycle",
                "VERIFICATION_PROTOCOL.md",
                "SESSION_MANAGER.md",
                "FAILURE_LOG.md",
                "/goal pause",
                "/goal resume",
                "/goal clear")) {
            assertTrue(architecture.contains(expected), "campaign architecture should explain " + expected);
        }
    }

    @Test
    void campaignArchitecturePreservesVerificationAndStopConditions() throws Exception {
        String normalized = read(CAMPAIGN_ARCHITECTURE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "focused verification while editing",
                "full local verification before merge",
                "required remote pr checks passed",
                "failed, cancelled, stale, pending",
                "main ci/codeql is red",
                "human approval is needed",
                "pause the campaign instead of improvising")) {
            assertTrue(normalized.contains(expected), "campaign architecture should preserve " + expected);
        }
    }

    @Test
    void campaignArchitecturePreservesNotProvenBoundaries() throws Exception {
        String normalized = read(CAMPAIGN_ARCHITECTURE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "campaign architecture should preserve " + expected);
        }
    }

    @Test
    void agentDocsCrossLinkCampaignArchitecture() throws Exception {
        for (Path path : List.of(
                README,
                AGENTS,
                BUILD_CONTRACT,
                QUICKSTART,
                GOAL_PROTOCOL,
                VERIFICATION_PROTOCOL,
                SESSION_MANAGER,
                FAILURE_LOG,
                REVIEWER_TRUST_MAP)) {
            assertTrue(read(path).contains("CAMPAIGN_SYSTEM_ARCHITECTURE.md"),
                    path + " should link to the campaign system architecture");
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
