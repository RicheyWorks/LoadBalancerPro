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

class AgentGoalCampaignReviewerTrustNavigationDocumentationTest {
    private static final Path NAVIGATION = Path.of("docs/agent/GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path BOARD = Path.of("docs/agent/GOAL_CAMPAIGN_BOARD.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentGoalCampaignReviewerTrustNavigationDocumentationTest.java");

    @Test
    void reviewerTrustNavigationDefinesCampaignReviewPath() throws Exception {
        String navigation = read(NAVIGATION);
        String normalized = navigation.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "LoadBalancerPro Goal Mode 10-PR Trial",
                "reviewer-facing navigation lane",
                "docs/test-only",
                "README.md",
                "docs/REVIEWER_TRUST_MAP.md",
                "GOAL_CAMPAIGN_CONTRACT.md",
                "GOAL_CAMPAIGN_BOARD.md",
                "GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md",
                "VERIFICATION_PROTOCOL.md",
                "SESSION_MANAGER.md",
                "FAILURE_LOG.md",
                "GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md")) {
            assertTrue(navigation.contains(expected), "navigation should contain " + expected);
        }

        for (String expected : List.of(
                "one scoped pr at a time",
                "session_manager.md after every checkpoint",
                "failure_log.md before continuing",
                "focused checks while editing",
                "full local verification before opening or merging",
                "latest/current-head required checks are green",
                "failed, cancelled, stale, pending, or duplicate-only required checks are not acceptable",
                "post-merge main ci and codeql are green",
                "preserve the not-proven boundaries")) {
            assertTrue(normalized.contains(expected), "navigation should preserve campaign rule " + expected);
        }
    }

    @Test
    void reviewerTrustNavigationPreservesScopeAndNotProvenBoundaries() throws Exception {
        String normalized = read(NAVIGATION).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not add automation",
                "ci/maven wiring",
                "dockerfile changes",
                "compose behavior changes",
                "runtime behavior changes",
                "endpoint changes",
                "k6/bruno/toxiproxy behavior changes",
                "runner services",
                "secrets",
                "external/cloud/tenant targets",
                "does not authorize production code changes",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof unless implemented and verified",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "navigation should preserve " + expected);
        }
    }

    @Test
    void reviewerTrustMapAndCampaignCheckpointLinkNavigation() throws Exception {
        String trustMap = read(TRUST_MAP).toLowerCase(Locale.ROOT);
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION_MANAGER).toLowerCase(Locale.ROOT);

        assertTrue(trustMap.contains("agent/goal_campaign_reviewer_trust_navigation.md"),
                "Reviewer Trust Map should link to campaign reviewer trust navigation");
        assertTrue(trustMap.contains("loadbalancerpro goal mode 10-pr trial"),
                "Reviewer Trust Map should name the campaign");
        assertTrue(trustMap.contains("one scoped pr at a time"),
                "Reviewer Trust Map should preserve scoped campaign review");
        assertTrue(trustMap.contains("current-head remote checks"),
                "Reviewer Trust Map should preserve current-head remote check rule");

        assertTrue(board.contains("goal_campaign_reviewer_trust_navigation.md"),
                "board should link reviewer trust navigation");
        assertTrue(board.contains("#312"), "board should preserve slot 7 PR history");
        assertTrue(board.contains("29f19ef9823ba19807e170be59c8032e283c6862"),
                "board should preserve slot 7 head history");
        assertTrue(board.contains("ca16382638dbbc118aeab7070a4b8bbf585ae827"),
                "board should preserve slot 7 merge history");
        assertTrue(board.contains("codex/goal-campaign-reviewer-trust-navigation"),
                "board should record slot 8 branch history");

        assertTrue(session.contains("goal_campaign_reviewer_trust_navigation.md"),
                "session manager should link reviewer trust navigation");
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
