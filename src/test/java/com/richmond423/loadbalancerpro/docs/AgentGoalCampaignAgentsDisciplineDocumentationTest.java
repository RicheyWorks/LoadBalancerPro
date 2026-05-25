package com.richmond423.loadbalancerpro.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AgentGoalCampaignAgentsDisciplineDocumentationTest {
    private static final Path AGENTS = Path.of("AGENTS.md");
    private static final Path DISCIPLINE = Path.of("docs/agent/GOAL_CAMPAIGN_AGENT_DISCIPLINE.md");
    private static final Path BOARD = Path.of("docs/agent/GOAL_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path FAILURE_LOG = Path.of("docs/agent/FAILURE_LOG.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");

    @Test
    void agentsFileLinksCampaignDisciplineAndPreservesOperatingRules() throws IOException {
        String agents = read(AGENTS).toLowerCase();

        assertTrue(agents.contains("goal campaign discipline"));
        assertTrue(agents.contains("docs/agent/goal_campaign_agent_discipline.md"));
        assertTrue(agents.contains("one scoped pr at a time"));
        assertTrue(agents.contains("session_manager.md"));
        assertTrue(agents.contains("failure_log.md"));
        assertTrue(agents.contains("pending, failed, cancelled, stale, or duplicate-only required checks"));
        assertTrue(agents.contains("do not use the campaign format as permission to change production behavior"));
        assertTrue(agents.contains("no overclaiming"));
    }

    @Test
    void disciplineDocDefinesCampaignRulesAndStopConditions() throws IOException {
        String discipline = read(DISCIPLINE).toLowerCase();

        for (String required : new String[] {
                "documentation only",
                "one scoped pr at a time",
                "docs/test-only by default",
                "current-head local verification",
                "current-head remote verification",
                "post-merge main verification",
                "session_manager.md updates after every checkpoint",
                "failure_log.md entries before continuing",
                "merge only when the latest active required checks are green",
                "do not accept pending, failed, cancelled, stale, or duplicate-only required checks",
                "do not claim green main",
                "pause the goal instead of improvising",
                "branch diff moves outside the slot scope",
                "local verification fails",
                "remote checks fail",
                "main is red or still pending",
                "human decision" }) {
            assertTrue(discipline.contains(required), "Missing campaign discipline wording: " + required);
        }
    }

    @Test
    void disciplineDocPreservesScopeAndNotProvenBoundaries() throws IOException {
        String discipline = read(DISCIPLINE).toLowerCase();

        for (String forbiddenClaimBoundary : new String[] {
                "does not add automation",
                "ci/maven wiring",
                "dockerfile changes",
                "compose behavior",
                "runtime behavior",
                "endpoints",
                "runner services",
                "secrets",
                "external targets",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "broader automation" }) {
            assertTrue(discipline.contains(forbiddenClaimBoundary), "Missing not-proven boundary: " + forbiddenClaimBoundary);
        }
    }

    @Test
    void boardAndSessionPreserveSlotNineHistoryAfterCampaignPointerMoves() throws IOException {
        String board = read(BOARD).toLowerCase();
        String session = read(SESSION).toLowerCase();

        assertTrue(board.contains("codex/goal-campaign-agents-discipline"));
        assertTrue(board.contains("#314"));
        assertTrue(board.contains("09d0ab9ee4ab508846165bbab51756b83d43814c"));
        assertTrue(board.contains("b045b4669ab736cfc0c707fae058ad2e73d7cd20"));
        assertTrue(board.contains("agent discipline merged"));
        assertTrue(board.contains("goal_campaign_agent_discipline.md"));
        assertTrue(session.contains("slot 9 merged and main green"));
        assertTrue(session.contains("b045b4669ab736cfc0c707fae058ad2e73d7cd20"));
        assertTrue(session.contains("goal_campaign_agent_discipline.md"));
        assertTrue(read(TRUST_MAP).toLowerCase().contains("goal_campaign_agent_discipline.md"));
    }

    @Test
    void guardOnlyReadsTrackedFiles() throws IOException {
        String source = read(Path.of("src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignAgentsDisciplineDocumentationTest.java"));

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
