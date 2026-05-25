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

class AgentGoalCampaignReadmeSummaryDocumentationTest {
    private static final Path README = Path.of("README.md");
    private static final Path BOARD = Path.of("docs/agent/GOAL_CAMPAIGN_BOARD.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignReadmeSummaryDocumentationTest.java");

    @Test
    void readmeContainsGoalModeCampaignSummary() throws Exception {
        String readme = read(README);
        String normalized = readme.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "Goal Mode Campaign Summary",
                "LoadBalancerPro Goal Mode 10-PR Trial",
                "public trust surface",
                "one scoped PR at a time",
                "docs/test-only by default",
                "SESSION_MANAGER.md checkpoints",
                "FAILURE_LOG.md recovery entries",
                "focused checks while editing",
                "full local verification before merge",
                "current-head remote checks")) {
            assertTrue(readme.contains(expected), "README should summarize " + expected);
        }

        for (String expected : List.of(
                "goal_campaign_board.md",
                "goal_campaign_contract.md",
                "goal_campaign_build_contract_example.md",
                "goal_campaign_verification_protocol_refinement.md",
                "goal_campaign_final_report_template.md")) {
            assertTrue(normalized.contains(expected), "README should link " + expected);
        }
    }

    @Test
    void readmeCampaignSummaryPreservesScopeAndNotProvenBoundaries() throws Exception {
        String normalized = read(README).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not relax scope",
                "does not authorize production code changes",
                "maven config changes",
                "ci/workflow changes",
                "dockerfile changes",
                "compose behavior changes",
                "runtime behavior changes",
                "endpoint changes",
                "runner services",
                "automation",
                "secrets",
                "external/cloud/tenant targets",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "README should preserve " + expected);
        }
    }

    @Test
    void campaignBoardPreservesSlotSixHistoryAndStartsSlotSeven() throws Exception {
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION_MANAGER).toLowerCase(Locale.ROOT);

        assertTrue(board.contains("#311"), "board should preserve slot 6 PR history");
        assertTrue(board.contains("27ec0aaf6cef0cf2525802aa4a94db563567de92"),
                "board should preserve slot 6 head history");
        assertTrue(board.contains("734c7f2068420152ac4f50ae988924575ff03f8a"),
                "board should preserve slot 6 merge history");
        assertTrue(board.contains("codex/goal-campaign-readme-summary"),
                "board should record slot 7 branch history");
        assertTrue(session.contains("current pr slot:"),
                "session manager should keep the moving active checkpoint");
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
