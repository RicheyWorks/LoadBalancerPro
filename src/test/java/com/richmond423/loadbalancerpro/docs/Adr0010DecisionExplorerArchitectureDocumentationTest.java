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

class Adr0010DecisionExplorerArchitectureDocumentationTest {
    private static final Path ADR =
            Path.of("docs/adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md");
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "Adr0010DecisionExplorerArchitectureDocumentationTest.java");

    @Test
    void adr0010ExistsAndStatesPlanningOnlyBoundaries() throws IOException {
        String adr = read(ADR);

        for (String expected : List.of(
                "# ADR-0010 Interactive Decision Explorer Architecture",
                "Proposed / planning-only.",
                "Decision type: architecture planning.",
                "Implementation status: not implemented.",
                "Decision Explorer Architecture Bootstrap Campaign",
                "../agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md",
                "planned",
                "read-only",
                "simulation-only",
                "docs/test-only",
                "human reviewer understanding",
                "AI-agent structured understanding")) {
            assertTrue(adr.contains(expected), "ADR-0010 should state boundary or purpose " + expected);
        }
    }

    @Test
    void adr0010DefinesExplanationArchitectureWithoutAuthority() throws IOException {
        String adr = read(ADR);

        for (String expected : List.of(
                "Decision Intake Snapshot",
                "Explanation Model",
                "Structured Agent View",
                "Evidence And Source Lane",
                "Boundary And Authority Guard",
                "selected candidate",
                "rejected candidates",
                "routing signals",
                "policy gates",
                "safety mode",
                "not-proven boundaries",
                "separates explanation from authority",
                "must not choose live production routes",
                "must not mutate routing, scoring, strategy, proxy, cloud, tenant, deployment, or production traffic state")) {
            assertTrue(adr.contains(expected), "ADR-0010 should define architecture item " + expected);
        }
    }

    @Test
    void adr0010PreservesNonImplementationAndNotProvenBoundaries() throws IOException {
        String normalized = read(ADR).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not add endpoints",
                "does not add ui implementation",
                "does not add runtime behavior",
                "does not add storage behavior",
                "does not add evidence-packet implementation",
                "does not add replay execution",
                "does not add export behavior",
                "does not prove production readiness",
                "does not prove live-cloud or real-tenant validation",
                "does not prove benchmark/load/stress",
                "throughput/p95/p99",
                "runtime endpoint/ui/storage/evidence-packet implementation",
                "broader automation",
                "autonomous production action")) {
            assertTrue(normalized.contains(expected), "ADR-0010 should preserve boundary " + expected);
        }
    }

    @Test
    void adr0010DefinesCampaignSlotRelationships() throws IOException {
        String adr = read(ADR);

        for (String expected : List.of(
                "DX-G03 records this proposed architecture ADR",
                "DX-G04 should define the future Decision Explorer data contract",
                "DX-G05 should define the future AI-agent-readable schema contract",
                "DX-G06 should define the future evidence lane and source cards",
                "DX-G07 should define Phase 0 verification guardrails",
                "DX-G08 should define implementation slices",
                "DX-G09 should provide a reviewer walkthrough",
                "DX-G10 should close the bootstrap")) {
            assertTrue(adr.contains(expected), "ADR-0010 should define slot relationship " + expected);
        }
    }

    @Test
    void campaignBoardStillNamesDxG03AdrGuard() throws IOException {
        String board = read(BOARD);

        for (String expected : List.of(
                "DX-G03 - ADR for Interactive Decision Explorer architecture",
                "codex/dx-g03-adr-decision-explorer-architecture",
                "Adr0010DecisionExplorerArchitectureDocumentationTest",
                "read-only",
                "simulation-only")) {
            assertTrue(board.contains(expected), "board should preserve DX-G03 item " + expected);
        }
    }

    @Test
    void adr0010AvoidsUnsupportedImplementationAndProductionOverclaims() throws IOException {
        String normalized = read(ADR).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "decision explorer is implemented",
                "decision explorer endpoint is implemented",
                "decision explorer ui is implemented",
                "decision explorer storage is implemented",
                "evidence packet is implemented",
                "replay export is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "autonomous production action is enabled")) {
            assertFalse(normalized.contains(forbidden), "ADR-0010 must not overclaim " + forbidden);
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
