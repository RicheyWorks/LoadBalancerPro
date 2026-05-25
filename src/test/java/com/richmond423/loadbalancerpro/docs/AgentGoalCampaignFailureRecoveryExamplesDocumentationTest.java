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

class AgentGoalCampaignFailureRecoveryExamplesDocumentationTest {
    private static final Path EXAMPLES = Path.of("docs/agent/GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md");
    private static final Path FAILURE_LOG = Path.of("docs/agent/FAILURE_LOG.md");
    private static final Path BOARD = Path.of("docs/agent/GOAL_CAMPAIGN_BOARD.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignFailureRecoveryExamplesDocumentationTest.java");

    @Test
    void failureRecoveryExamplesExistAndDefineCampaignLoggingPurpose() throws Exception {
        String examples = read(EXAMPLES).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "failure_log.md examples",
                "loadbalancerpro goal mode 10-pr trial",
                "durable record",
                "timestamp",
                "branch/pr",
                "head sha",
                "failure type",
                "failing check",
                "suspected cause",
                "fix attempted",
                "result",
                "recovery status",
                "next action")) {
            assertTrue(examples.contains(expected), "examples should define " + expected);
        }
    }

    @Test
    void examplesCoverLocalRemoteScopeAndMergeFailures() throws Exception {
        String examples = read(EXAMPLES).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "focused documentation guard test",
                "relevant focused selector bundle",
                "mvn -q test",
                "mvn -q \"-dskiptests\" package",
                "mvn -b package",
                "git diff --check origin/main...head",
                "enterprise-lab-workflow.ps1 -package",
                "remote pr build/test/package/smoke",
                "remote pr analyze java / codeql",
                "dependency review",
                "main post-merge ci or codeql",
                "scope audit",
                "github operation",
                "merge decision")) {
            assertTrue(examples.contains(expected), "examples should cover " + expected);
        }
    }

    @Test
    void examplesPreserveRemoteCheckRulesAndStopConditions() throws Exception {
        String examples = read(EXAMPLES).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "merge only when the latest required checks are green",
                "failed, cancelled, stale, pending, or duplicate-only required checks are not acceptable",
                "do not claim green main while remote checks are pending",
                "main becomes red",
                "github check state is ambiguous",
                "human approval is needed",
                "pause instead of improvising")) {
            assertTrue(examples.contains(expected), "examples should preserve " + expected);
        }
    }

    @Test
    void examplesPreserveNotProvenBoundaries() throws Exception {
        String examples = read(EXAMPLES).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "production readiness",
                "production certification",
                "live-cloud/real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof unless implemented and verified",
                "broader automation")) {
            assertTrue(examples.contains(expected), "examples should preserve " + expected);
        }
    }

    @Test
    void campaignDocsLinkRecoveryExamplesAndPreserveSlotHistory() throws Exception {
        String failureLog = read(FAILURE_LOG).toLowerCase(Locale.ROOT);
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION_MANAGER).toLowerCase(Locale.ROOT);

        assertTrue(failureLog.contains("goal_campaign_failure_recovery_examples.md"),
                "failure log should link to recovery examples");
        assertTrue(board.contains("goal_campaign_failure_recovery_examples.md"),
                "board should link to recovery examples");
        assertTrue(session.contains("goal_campaign_failure_recovery_examples.md"),
                "session manager should link to recovery examples");
        assertTrue(board.contains("#309"),
                "board should preserve slot 4 PR history");
        assertTrue(board.contains("3b0353b66e974a939ae8235ef32f564bf630b9d1"),
                "board should preserve slot 4 head history");
        assertTrue(board.contains("13fad31cd6cbc34efdf58c0a75ec5fa0f66d478e"),
                "board should preserve slot 4 merge history");
        assertTrue(board.contains("codex/goal-campaign-failure-log-recovery-examples"),
                "board should record slot 5 branch history");
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
