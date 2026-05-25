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

class AgentGoalCampaignBoardInitializationDocumentationTest {
    private static final Path BOARD = Path.of("docs/agent/GOAL_CAMPAIGN_BOARD.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignBoardInitializationDocumentationTest.java");

    @Test
    void boardRecordsTheActiveTrialAndSlotCount() throws Exception {
        String board = read(BOARD).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "loadbalancerpro goal mode 10-pr trial",
                "total target: 10 merged prs",
                "completed campaign prs: 1 / 10",
                "current pr slot: 2",
                "codex/goal-campaign-board-initialization",
                "current main head",
                "trial board")) {
            assertTrue(board.contains(expected), "board should record " + expected);
        }
    }

    @Test
    void boardRecordsSlotOneAsMergedAndMainGreen() throws Exception {
        String board = read(BOARD).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "#306",
                "codex/goal-campaign-template-architecture",
                "30828f89a41d64e30d1acc668714e5455a6e8a9f",
                "9b0efc0dc0d6654c0e8f95294e77e7de72bd7941",
                "post-merge main green",
                "main ci/codeql green")) {
            assertTrue(board.contains(expected), "slot 1 should record " + expected);
        }
    }

    @Test
    void boardPreservesCampaignRulesAndRemoteCheckDiscipline() throws Exception {
        String board = read(BOARD).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "work one scoped pr at a time",
                "update session_manager.md after every checkpoint",
                "log failures in failure_log.md",
                "merge only when latest required checks are green",
                "failed/cancelled/stale/pending required checks are not acceptable",
                "duplicate-only checks do not count as green")) {
            assertTrue(board.contains(expected), "board should preserve " + expected);
        }
    }

    @Test
    void boardPreservesNotProvenBoundaries() throws Exception {
        String board = read(BOARD).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof unless implemented and verified",
                "broader automation")) {
            assertTrue(board.contains(expected), "board should preserve " + expected);
        }
    }

    @Test
    void sessionManagerRecordsSlotTwoCheckpoint() throws Exception {
        String session = read(SESSION_MANAGER).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "loadbalancerpro goal mode 10-pr trial",
                "current pr slot: 2",
                "slot 1 merged and main green",
                "codex/goal-campaign-board-initialization",
                "9b0efc0dc0d6654c0e8f95294e77e7de72bd7941",
                "main ci and codeql",
                "slot 2 branch created from clean main")) {
            assertTrue(session.contains(expected), "session manager should record " + expected);
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
