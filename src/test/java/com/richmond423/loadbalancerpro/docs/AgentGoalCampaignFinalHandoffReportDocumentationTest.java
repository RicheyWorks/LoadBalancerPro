package com.richmond423.loadbalancerpro.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AgentGoalCampaignFinalHandoffReportDocumentationTest {
    private static final Path FINAL_HANDOFF = Path.of("docs/agent/GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md");
    private static final Path BOARD = Path.of("docs/agent/GOAL_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path README = Path.of("README.md");
    private static final Path AGENTS = Path.of("AGENTS.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path FINAL_TEMPLATE = Path.of("docs/agent/GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md");

    @Test
    void finalHandoffExistsAndDefinesCloseoutRules() throws IOException {
        String handoff = read(FINAL_HANDOFF).toLowerCase();

        for (String required : new String[] {
                "goal campaign final handoff report",
                "loadbalancerpro goal mode 10-pr trial",
                "documentation only",
                "slot 10 closeout artifact",
                "completed before this closeout branch: 9 / 10",
                "count only after merge and green main",
                "post-merge main verification passes",
                "main ci/codeql are green",
                "update session_manager.md at checkpoints",
                "log every local, remote, scope, or tooling failure in failure_log.md before continuing",
                "run focused checks while editing",
                "run full local verification before opening or merging",
                "merge only when the latest required pr checks are green",
                "do not accept failed, cancelled, stale, pending, or duplicate-only required checks",
                "do not claim green main while remote checks are pending" }) {
            assertTrue(handoff.contains(required), "Missing final handoff closeout wording: " + required);
        }
    }

    @Test
    void finalHandoffPreservesVerificationAndScopeBoundaries() throws IOException {
        String handoff = read(FINAL_HANDOFF).toLowerCase();

        for (String required : new String[] {
                "build/test/package/smoke",
                "analyze java / codeql",
                "dependency review",
                "docs/test-only",
                "does not change src/main/java",
                "maven config",
                "ci/workflow files",
                "dockerfile",
                "compose behavior",
                "runtime behavior",
                "endpoint behavior",
                "k6 behavior",
                "bruno behavior",
                "toxiproxy behavior",
                "secrets",
                "external/cloud/tenant targets",
                "automation" }) {
            assertTrue(handoff.contains(required), "Missing scope or verification boundary: " + required);
        }
    }

    @Test
    void finalHandoffPreservesNotProvenBoundaries() throws IOException {
        String handoff = read(FINAL_HANDOFF).toLowerCase();

        for (String boundary : new String[] {
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "broader automation" }) {
            assertTrue(handoff.contains(boundary), "Missing not-proven boundary: " + boundary);
        }
    }

    @Test
    void campaignNavigationLinksFinalHandoffAndAdvancesToSlotTen() throws IOException {
        String board = read(BOARD).toLowerCase();
        String session = read(SESSION).toLowerCase();
        String readme = read(README).toLowerCase();
        String agents = read(AGENTS).toLowerCase();
        String trustMap = read(TRUST_MAP).toLowerCase();
        String finalTemplate = read(FINAL_TEMPLATE).toLowerCase();

        assertTrue(board.contains("completed campaign prs: 9 / 10"));
        assertTrue(board.contains("current pr slot: 10"));
        assertTrue(board.contains("codex/goal-campaign-final-handoff-report"));
        assertTrue(board.contains("goal_campaign_final_handoff_report.md"));
        assertTrue(board.contains("#314"));
        assertTrue(board.contains("09d0ab9ee4ab508846165bbab51756b83d43814c"));
        assertTrue(board.contains("b045b4669ab736cfc0c707fae058ad2e73d7cd20"));
        assertTrue(session.contains("slot 9 merged and main green"));
        assertTrue(session.contains("current pr slot: 10"));
        assertTrue(readme.contains("goal_campaign_final_handoff_report.md"));
        assertTrue(agents.contains("goal_campaign_final_handoff_report.md"));
        assertTrue(trustMap.contains("goal_campaign_final_handoff_report.md"));
        assertTrue(finalTemplate.contains("goal_campaign_final_handoff_report.md"));
    }

    @Test
    void guardOnlyReadsTrackedFiles() throws IOException {
        String source = read(Path.of("src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignFinalHandoffReportDocumentationTest.java"));

        for (String forbidden : new String[] {
                "Files." + "write",
                "Files." + "create",
                "Files." + "delete",
                "Process" + "Builder",
                "Runtime." + "getRuntime",
                ".ex" + "ec(",
                "Http" + "Client",
                "URL" + "Connection",
                "So" + "cket" }) {
            assertFalse(source.contains(forbidden), "Guard test should not use side-effecting API: " + forbidden);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
