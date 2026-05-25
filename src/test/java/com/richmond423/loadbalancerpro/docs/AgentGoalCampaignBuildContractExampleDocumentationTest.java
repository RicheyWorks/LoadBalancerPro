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

class AgentGoalCampaignBuildContractExampleDocumentationTest {
    private static final Path EXAMPLE = Path.of("docs/agent/GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md");
    private static final Path BUILD_CONTRACT = Path.of("BUILD_CONTRACT.md");
    private static final Path BOARD = Path.of("docs/agent/GOAL_CAMPAIGN_BOARD.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignBuildContractExampleDocumentationTest.java");

    @Test
    void buildContractExampleExistsAndDefinesTheTrialGoal() throws Exception {
        String example = read(EXAMPLE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "loadbalancerpro goal mode 10-pr trial",
                "filled example",
                "build_contract.md",
                "one scoped pr at a time",
                "10 successfully merged prs",
                "pause if scope, safety, verification, github state, or human decision-making requires it")) {
            assertTrue(example.contains(expected), "example should define " + expected);
        }
    }

    @Test
    void examplePreservesScopeAndStopConditions() throws Exception {
        String example = read(EXAMPLE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "prefer docs/test-only changes",
                "src/main/java",
                "maven config",
                "ci/workflow files",
                "dockerfile",
                "compose behavior",
                "runner services",
                "automation",
                "secrets",
                "external/cloud/tenant targets",
                "stop conditions")) {
            assertTrue(example.contains(expected), "example should preserve " + expected);
        }
    }

    @Test
    void examplePreservesVerificationAndRemoteRules() throws Exception {
        String example = read(EXAMPLE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "use focused checks while editing",
                "use full verification before opening a merge decision",
                "mvn -b dependency:tree",
                "mvn -q test",
                "mvn -q \"-dskiptests\" package",
                "mvn -b package",
                "git diff --check",
                "enterprise-lab-workflow.ps1 -package",
                "build/test/package/smoke",
                "analyze java / codeql",
                "dependency review",
                "failed/cancelled/stale/pending required checks are not acceptable",
                "duplicate-only checks do not count as green")) {
            assertTrue(example.contains(expected), "example should preserve " + expected);
        }
    }

    @Test
    void examplePreservesNotProvenBoundaries() throws Exception {
        String example = read(EXAMPLE).toLowerCase(Locale.ROOT);

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
            assertTrue(example.contains(expected), "example should preserve " + expected);
        }
    }

    @Test
    void buildContractBoardAndSessionLinkTheExample() throws Exception {
        String buildContract = read(BUILD_CONTRACT).toLowerCase(Locale.ROOT);
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION_MANAGER).toLowerCase(Locale.ROOT);

        assertTrue(buildContract.contains("goal_campaign_build_contract_example.md"),
                "BUILD_CONTRACT.md should link to the filled example");
        assertTrue(board.contains("goal_campaign_build_contract_example.md"),
                "board should link to the filled example");
        assertTrue(board.contains("completed campaign prs:"),
                "board should record completed PR count");
        assertTrue(board.contains("current pr slot:"),
                "board should record the active slot");
        assertTrue(board.contains("codex/goal-campaign-build-contract-example"),
                "board should preserve slot 3 branch history");
        assertTrue(board.contains("#308"),
                "board should preserve slot 3 PR history");
        assertTrue(session.contains("current pr slot:"),
                "session should record the active slot");
        assertTrue(session.contains("goal_campaign_build_contract_example.md"),
                "session should keep the example visible to later checkpoints");
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
