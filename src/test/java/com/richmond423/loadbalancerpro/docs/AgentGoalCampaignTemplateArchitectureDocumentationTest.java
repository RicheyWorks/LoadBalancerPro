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

class AgentGoalCampaignTemplateArchitectureDocumentationTest {
    private static final Path CONTRACT = Path.of("docs/agent/GOAL_CAMPAIGN_CONTRACT.md");
    private static final Path BOARD = Path.of("docs/agent/GOAL_CAMPAIGN_BOARD.md");
    private static final Path PR_TEMPLATE = Path.of("docs/agent/GOAL_CAMPAIGN_PR_TEMPLATE.md");
    private static final Path CHECKPOINT_TEMPLATE = Path.of("docs/agent/GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md");
    private static final Path FINAL_REPORT = Path.of("docs/agent/GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignTemplateArchitectureDocumentationTest.java");

    @Test
    void goalCampaignTemplateDocsExist() {
        for (Path path : List.of(CONTRACT, BOARD, PR_TEMPLATE, CHECKPOINT_TEMPLATE, FINAL_REPORT)) {
            assertTrue(Files.exists(path), path + " should exist");
        }
    }

    @Test
    void contractDefinesCampaignArchitecture() throws Exception {
        String normalized = read(CONTRACT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "campaign contract",
                "10 pr slots",
                "one scoped pr at a time",
                "required session_manager.md updates",
                "required failure_log.md entries",
                "verification levels",
                "remote check rules",
                "merge rules",
                "stop conditions")) {
            assertTrue(normalized.contains(expected), "contract should define " + expected);
        }
    }

    @Test
    void boardAndTemplatesDefineRequiredTrialRecords() throws Exception {
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String prTemplate = read(PR_TEMPLATE).toLowerCase(Locale.ROOT);
        String checkpoint = read(CHECKPOINT_TEMPLATE).toLowerCase(Locale.ROOT);
        String report = read(FINAL_REPORT).toLowerCase(Locale.ROOT);

        for (String expected : List.of("10 pr slots", "slot", "status", "head sha", "merge sha")) {
            assertTrue(board.contains(expected), "board should include " + expected);
        }

        for (String expected : List.of("focused verification", "remote checks", "scope/safety audit",
                "remaining not-proven boundaries")) {
            assertTrue(prTemplate.contains(expected), "PR template should include " + expected);
        }

        for (String expected : List.of("timestamp", "goal name", "current pr slot", "current branch",
                "checks run", "remote status", "decision: continue / pause / merge / abandon")) {
            assertTrue(checkpoint.contains(expected), "checkpoint template should include " + expected);
        }

        for (String expected : List.of("overall classification: pass / warn / fail", "prs attempted",
                "prs merged", "current main head", "main ci/codeql", "recommended next goal")) {
            assertTrue(report.contains(expected), "final report should include " + expected);
        }
    }

    @Test
    void templatesPreserveVerificationMergeAndFailureRules() throws Exception {
        String all = readAllTemplates();

        for (String expected : List.of(
                "merge only when latest required checks are green",
                "failed/cancelled/stale/pending required checks are not acceptable",
                "duplicate-only",
                "update session_manager.md after every checkpoint",
                "log failures in failure_log.md",
                "mvn -q test",
                "mvn -q \"-dskiptests\" package",
                "mvn -b package",
                "git diff --check",
                "enterprise-lab-workflow.ps1 -package")) {
            assertTrue(all.contains(expected), "templates should preserve " + expected);
        }
    }

    @Test
    void templatesPreserveScopeAndNotProvenBoundaries() throws Exception {
        String all = readAllTemplates();

        for (String expected : List.of(
                "preserve not-proven boundaries",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof unless implemented and verified",
                "broader automation")) {
            assertTrue(all.contains(expected), "templates should preserve " + expected);
        }
    }

    @Test
    void sessionManagerRecordsActiveTrialCheckpointAndTemplateLinks() throws Exception {
        String normalized = read(SESSION_MANAGER).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "loadbalancerpro goal mode 10-pr trial",
                "current pr slot:",
                "goal_campaign_contract.md",
                "goal_campaign_board.md",
                "goal_campaign_pr_template.md",
                "goal_campaign_checkpoint_template.md",
                "goal_campaign_final_report_template.md")) {
            assertTrue(normalized.contains(expected), "session manager should record " + expected);
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

    private static String readAllTemplates() throws IOException {
        return String.join("\n", List.of(
                read(CONTRACT),
                read(BOARD),
                read(PR_TEMPLATE),
                read(CHECKPOINT_TEMPLATE),
                read(FINAL_REPORT))).toLowerCase(Locale.ROOT);
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
