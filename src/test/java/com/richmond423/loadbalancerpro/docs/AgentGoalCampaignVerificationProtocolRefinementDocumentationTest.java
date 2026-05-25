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

class AgentGoalCampaignVerificationProtocolRefinementDocumentationTest {
    private static final Path REFINEMENT = Path.of("docs/agent/GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md");
    private static final Path VERIFICATION_PROTOCOL = Path.of("docs/agent/VERIFICATION_PROTOCOL.md");
    private static final Path BOARD = Path.of("docs/agent/GOAL_CAMPAIGN_BOARD.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentGoalCampaignVerificationProtocolRefinementDocumentationTest.java");

    @Test
    void refinementExistsAndDefinesCampaignVerificationOrder() throws Exception {
        String refinement = read(REFINEMENT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "verification_protocol.md",
                "loadbalancerpro goal mode 10-pr trial",
                "one scoped pr at a time",
                "current head sha",
                "focused documentation guard",
                "focused selector bundle",
                "mvn -b dependency:tree",
                "mvn -q test",
                "mvn -q \"-dskiptests\" package",
                "mvn -b package",
                "git diff --check origin/main...head",
                "enterprise-lab-workflow.ps1 -package",
                "remote pr checks",
                "post-merge main checks")) {
            assertTrue(refinement.contains(expected), "refinement should define " + expected);
        }
    }

    @Test
    void refinementPreservesRemoteAndMainCheckRules() throws Exception {
        String refinement = read(REFINEMENT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "latest/current head sha",
                "build/test/package/smoke must pass",
                "analyze java / codeql must pass",
                "dependency review must pass where applicable",
                "failed, cancelled, stale, pending, or duplicate-only required checks are not acceptable",
                "older head sha does not make the current head green",
                "do not claim green main while remote checks are pending",
                "do not start the next slot until main")) {
            assertTrue(refinement.contains(expected), "refinement should preserve " + expected);
        }
    }

    @Test
    void refinementRequiresCheckpointAndFailureDiscipline() throws Exception {
        String refinement = read(REFINEMENT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "update session_manager.md after every checkpoint",
                "final-head verification",
                "remote checks green",
                "post-merge main green",
                "log failures in failure_log.md before continuing",
                "scope audit",
                "github operation",
                "merge decision",
                "pause instead of improvising")) {
            assertTrue(refinement.contains(expected), "refinement should require " + expected);
        }
    }

    @Test
    void refinementPreservesScopeAndNotProvenBoundaries() throws Exception {
        String refinement = read(REFINEMENT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not authorize production code changes",
                "maven config changes",
                "ci/workflow changes",
                "dockerfile changes",
                "compose behavior changes",
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
                "replay/evidence/report/storage/export proof unless implemented and verified",
                "broader automation")) {
            assertTrue(refinement.contains(expected), "refinement should preserve " + expected);
        }
    }

    @Test
    void campaignDocsLinkRefinementAndPreserveDurableHistory() throws Exception {
        String protocol = read(VERIFICATION_PROTOCOL).toLowerCase(Locale.ROOT);
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION_MANAGER).toLowerCase(Locale.ROOT);

        assertTrue(protocol.contains("goal_campaign_verification_protocol_refinement.md"),
                "verification protocol should link to refinement");
        assertTrue(board.contains("goal_campaign_verification_protocol_refinement.md"),
                "board should link to refinement");
        assertTrue(session.contains("goal_campaign_verification_protocol_refinement.md"),
                "session manager should link to refinement");
        assertTrue(board.contains("#310"),
                "board should preserve slot 5 PR history");
        assertTrue(board.contains("0f028c10984084d3b04f7b742969f79d5c32ff4d"),
                "board should preserve slot 5 head history");
        assertTrue(board.contains("702070aa6b0db90743986176bb96d1bf9208381b"),
                "board should preserve slot 5 merge history");
        assertTrue(board.contains("codex/goal-campaign-verification-protocol-refinement"),
                "board should record slot 6 branch history");
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
