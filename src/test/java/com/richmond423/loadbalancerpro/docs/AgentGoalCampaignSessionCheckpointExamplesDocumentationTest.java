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

class AgentGoalCampaignSessionCheckpointExamplesDocumentationTest {
    private static final Path EXAMPLES = Path.of("docs/agent/GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md");
    private static final Path CHECKPOINT_TEMPLATE = Path.of("docs/agent/GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md");
    private static final Path BOARD = Path.of("docs/agent/GOAL_CAMPAIGN_BOARD.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignSessionCheckpointExamplesDocumentationTest.java");

    @Test
    void sessionCheckpointExamplesExistAndExplainMovingActiveCheckpoint() throws Exception {
        String examples = read(EXAMPLES).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "session_manager.md",
                "active checkpoint",
                "durable slot history belongs in goal_campaign_board.md",
                "branch created checkpoint",
                "edit batch completed checkpoint",
                "focused verification checkpoint",
                "full local verification checkpoint",
                "pr opened checkpoint",
                "remote checks green checkpoint",
                "post-merge main green checkpoint",
                "pause checkpoint")) {
            assertTrue(examples.contains(expected), "examples should include " + expected);
        }
    }

    @Test
    void examplesPreserveVerificationAndRemoteRules() throws Exception {
        String examples = read(EXAMPLES).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "mvn -b dependency:tree",
                "mvn -q test",
                "mvn -q \"-dskiptests\" package",
                "mvn -b package",
                "git diff --check origin/main...head",
                "enterprise-lab-workflow.ps1 -package",
                "build/test/package/smoke passed",
                "analyze java / codeql passed",
                "dependency review passed where applicable",
                "do not claim green main while remote checks are pending",
                "failed, cancelled, stale, pending, or duplicate-only required checks")) {
            assertTrue(examples.contains(expected), "examples should preserve " + expected);
        }
    }

    @Test
    void examplesRequireFailureLogEntriesAndStopDiscipline() throws Exception {
        String examples = read(EXAMPLES).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "update failure_log.md before continuing",
                "scope audit",
                "github operation",
                "merge decision fails",
                "decision: pause",
                "blocker",
                "next action")) {
            assertTrue(examples.contains(expected), "examples should preserve " + expected);
        }
    }

    @Test
    void examplesPreserveNotProvenBoundaries() throws Exception {
        String examples = read(EXAMPLES).toLowerCase(Locale.ROOT);

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
            assertTrue(examples.contains(expected), "examples should preserve " + expected);
        }
    }

    @Test
    void campaignDocsLinkTheSessionExamplesAndAdvanceSlotFour() throws Exception {
        String checkpoint = read(CHECKPOINT_TEMPLATE).toLowerCase(Locale.ROOT);
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION_MANAGER).toLowerCase(Locale.ROOT);

        assertTrue(checkpoint.contains("goal_campaign_session_checkpoint_examples.md"),
                "checkpoint template should link to examples");
        assertTrue(board.contains("goal_campaign_session_checkpoint_examples.md"),
                "board should link to examples");
        assertTrue(board.contains("completed campaign prs: 3 / 10"),
                "board should record three completed PRs");
        assertTrue(board.contains("current pr slot: 4"),
                "board should advance the active slot");
        assertTrue(session.contains("goal_campaign_session_checkpoint_examples.md"),
                "session manager should link to examples");
        assertTrue(session.contains("current pr slot: 4"),
                "session manager should record slot 4");
        assertTrue(session.contains("slot 3 merged and main green"),
                "session manager should record the slot 3 merge checkpoint");
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
