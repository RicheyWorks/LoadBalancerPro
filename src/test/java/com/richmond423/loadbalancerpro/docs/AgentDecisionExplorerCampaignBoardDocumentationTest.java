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

class AgentDecisionExplorerCampaignBoardDocumentationTest {
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md");
    private static final Path INDEX = Path.of("docs/agent/CAMPAIGN_SYSTEM_INDEX.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerCampaignBoardDocumentationTest.java");

    @Test
    void campaignBoardExistsAndNamesDecisionExplorerBootstrapCampaign() throws IOException {
        String normalized = read(BOARD).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "decision explorer architecture bootstrap campaign",
                "warn / decision-explorer-bootstrap",
                "interactive decision explorer",
                "repo-native",
                "one scoped pr at a time",
                "docs/test-only")) {
            assertTrue(normalized.contains(expected), "board should contain " + expected);
        }
    }

    @Test
    void campaignBoardNamesDxG01ThroughDxG10AndBranches() throws IOException {
        String board = read(BOARD);

        for (String goal : List.of(
                "DX-G01", "DX-G02", "DX-G03", "DX-G04", "DX-G05",
                "DX-G06", "DX-G07", "DX-G08", "DX-G09", "DX-G10")) {
            assertTrue(board.contains(goal), "board should name " + goal);
        }

        for (String branch : List.of(
                "codex/dx-g01-campaign-index-and-board",
                "codex/dx-g02-readme-trust-map-links",
                "codex/dx-g03-adr-decision-explorer-architecture",
                "codex/dx-g04-decision-explorer-data-contract",
                "codex/dx-g05-agent-readable-schema-contract",
                "codex/dx-g06-evidence-lane-and-source-cards",
                "codex/dx-g07-phase0-verification-guardrails",
                "codex/dx-g08-decision-explorer-implementation-plan",
                "codex/dx-g09-reviewer-walkthrough",
                "codex/dx-g10-bootstrap-closeout")) {
            assertTrue(board.contains(branch), "board should name branch " + branch);
        }
    }

    @Test
    void campaignBoardPreservesReadOnlySimulationOnlyAndNotProvenBoundaries() throws IOException {
        String normalized = read(BOARD).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "read-only",
                "simulation-only",
                "no live mutation",
                "no autonomous production action",
                "no production readiness",
                "no production certification",
                "no live-cloud validation",
                "no real-tenant validation",
                "no load/stress/benchmarking evidence",
                "no throughput/p95/p99 production evidence",
                "no replay/evidence/report/storage/export proof",
                "no broader automation")) {
            assertTrue(normalized.contains(expected), "board should preserve boundary " + expected);
        }
    }

    @Test
    void campaignBoardDefinesPrTrackingCheckpointAndVerificationColumns() throws IOException {
        String board = read(BOARD);
        String normalized = board.toLowerCase(Locale.ROOT);

        for (String heading : List.of(
                "| Slot | Title | Branch | PR | Head SHA | Status | Evidence / guard | Checkpoint |",
                "## Checkpoint Rules",
                "## Verification Expectations",
                "## Closeout Criteria")) {
            assertTrue(board.contains(heading), "board should contain section or table heading " + heading);
        }

        for (String expected : List.of(
                "pr-opened",
                "merged-awaiting-main-green",
                "merged-main-green",
                "mvn -q test",
                "mvn -b package",
                "git diff --check",
                "enterprise-lab-workflow.ps1 -package")) {
            assertTrue(normalized.contains(expected), "board should define tracking or verification item " + expected);
        }
    }

    @Test
    void campaignSystemIndexLinksDecisionExplorerBoard() throws IOException {
        assertTrue(read(INDEX).contains("DECISION_EXPLORER_CAMPAIGN_BOARD.md"),
                "campaign system index should link the Decision Explorer board");
    }

    @Test
    void campaignBoardAvoidsUnsupportedOverclaims() throws IOException {
        String normalized = read(BOARD).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "production ready",
                "production readiness is proven",
                "certified production",
                "guaranteed p99",
                "real tenant validated",
                "live-cloud validated",
                "benchmark proven",
                "load test proven",
                "stress test proven",
                "autonomous production action is enabled")) {
            assertFalse(normalized.contains(forbidden), "board must not overclaim " + forbidden);
        }
    }

    @Test
    void guardTestOnlyReadsTrackedFiles() throws IOException {
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
