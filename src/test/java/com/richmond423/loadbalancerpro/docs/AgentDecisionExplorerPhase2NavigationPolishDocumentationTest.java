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

class AgentDecisionExplorerPhase2NavigationPolishDocumentationTest {
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path API_CONTRACTS = Path.of("docs/API_CONTRACTS.md");
    private static final Path PAGE = Path.of("src/main/resources/static/decision-explorer.html");
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_PHASE2_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerPhase2NavigationPolishDocumentationTest.java");

    @Test
    void readmeAndTrustMapExposePhaseTwoReviewerNavigation() throws IOException {
        String combined = read(README) + "\n" + read(TRUST_MAP);

        for (String expected : List.of(
                "Decision Explorer Phase 2 reviewer navigation",
                "Decision Explorer Phase 2 reviewer path",
                "DECISION_EXPLORER_PHASE2_CAMPAIGN_BOARD.md",
                "DECISION_EXPLORER_PHASE2_REVIEWER_EXAMPLES.md",
                "API_CONTRACTS.md",
                "/decision-explorer.html",
                "scenario catalog",
                "factor drill-down",
                "candidate comparison",
                "reviewer explanation badges",
                "additive API hardening",
                "unknown/partial states",
                "not-proven boundaries")) {
            assertTrue(combined.contains(expected), "reviewer docs should expose " + expected);
        }
    }

    @Test
    void decisionExplorerPagePointsToRepositoryDocsWithoutExternalLinks() throws IOException {
        String page = read(PAGE);

        for (String expected : List.of(
                "Repository docs for this reviewer path",
                "docs/API_CONTRACTS.md",
                "docs/agent/DECISION_EXPLORER_PHASE2_REVIEWER_EXAMPLES.md",
                "docs/agent/DECISION_EXPLORER_PHASE2_CAMPAIGN_BOARD.md",
                "Reviewer Navigation",
                "Open Web Cockpit",
                "Open Routing Proof",
                "Open Reviewer Dashboard",
                "Open Operator Evidence")) {
            assertTrue(page.contains(expected), "Decision Explorer page should expose " + expected);
        }

        assertFalse(page.contains("https://"), "Decision Explorer page must not add external HTTPS links");
        assertFalse(page.contains("http://"), "Decision Explorer page must not add external HTTP links");
    }

    @Test
    void phaseTwoNavigationIsTrackedByCampaignSurfaces() throws IOException {
        String board = read(BOARD);
        String session = read(SESSION);

        for (String expected : List.of(
                "DX-P2-G11",
                "codex/decision-explorer-phase2-final-polish",
                "final hardening and navigation polish",
                "AgentDecisionExplorerPhase2NavigationPolishDocumentationTest",
                "567cf77643a0d56a683cea86104972715b97fa40",
                "PR #378",
                "merged-main-green",
                "phase2-final-polish")) {
            assertTrue(board.contains(expected), "campaign board should track " + expected);
            assertTrue(session.contains(expected), "session manager should track " + expected);
        }
    }

    @Test
    void navigationPolishPreservesSafetyAndNotProvenBoundaries() throws IOException {
        String normalized = (read(README) + "\n" + read(TRUST_MAP) + "\n" + read(API_CONTRACTS) + "\n"
                + read(PAGE)).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        for (String expected : List.of(
                "read-only",
                "same-origin",
                "simulation-only",
                "memory-only",
                "no-storage",
                "no-export",
                "no-replay",
                "no-evidence-packet",
                "no-production-proof",
                "does not shift traffic",
                "mutate routing",
                "call cloud or tenant systems",
                "persist storage",
                "execute replay",
                "export files",
                "generate evidence packets",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "benchmark/load/stress",
                "throughput/p95/p99",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "navigation polish should preserve boundary " + expected);
        }

        for (String forbidden : List.of(
                "decision explorer is implemented",
                "decision explorer endpoint",
                "production readiness is proven",
                "certified production",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "traffic shifting enabled",
                "autonomous production action enabled",
                "evidence packet generated")) {
            assertFalse(normalized.contains(forbidden), "navigation polish must not overclaim " + forbidden);
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
