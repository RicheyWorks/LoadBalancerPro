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

class AgentDecisionExplorerReadmeTrustMapDocumentationTest {
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerReadmeTrustMapDocumentationTest.java");

    @Test
    void readmeLinksDecisionExplorerCampaignBoardWithBoundaries() throws IOException {
        String readme = read(README);
        String normalized = readme.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "Decision Explorer Architecture Bootstrap Campaign",
                "docs/agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md",
                "DX-G01 through DX-G10",
                "Interactive Decision Explorer",
                "reviewer/operator and agent-readable explanation surface",
                "planned",
                "read-only",
                "simulation-only",
                "docs/test-only")) {
            assertTrue(readme.contains(expected), "README should contain " + expected);
        }

        for (String expected : List.of(
                "does not create endpoints",
                "runtime behavior",
                "storage/export behavior",
                "automation",
                "deployment behavior",
                "cloud behavior",
                "tenant behavior",
                "production traffic-control behavior",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "load/stress/benchmarking evidence",
                "throughput/p95/p99 evidence",
                "replay/export/storage proof",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "README should preserve boundary " + expected);
        }
    }

    @Test
    void reviewerTrustMapLinksDecisionExplorerCampaignBoardWithBoundaries() throws IOException {
        String trustMap = read(TRUST_MAP);
        String normalized = trustMap.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "Decision Explorer Architecture Bootstrap Campaign",
                "agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md",
                "DX-G01 through DX-G10",
                "Interactive Decision Explorer",
                "reviewer-facing navigation",
                "planned",
                "read-only",
                "simulation-only",
                "docs/test-only")) {
            assertTrue(trustMap.contains(expected), "Reviewer Trust Map should contain " + expected);
        }

        for (String expected : List.of(
                "does not add endpoints",
                "runtime behavior",
                "storage/export behavior",
                "automation",
                "deployment behavior",
                "cloud behavior",
                "tenant behavior",
                "production traffic-control behavior",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "load/stress/benchmarking evidence",
                "throughput/p95/p99 evidence",
                "replay/export/storage proof",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "Reviewer Trust Map should preserve boundary " + expected);
        }
    }

    @Test
    void linkedBoardStillDefinesDecisionExplorerBootstrapCampaign() throws IOException {
        String board = read(BOARD);

        for (String expected : List.of(
                "Decision Explorer Architecture Bootstrap Campaign",
                "DX-G02 - README and Reviewer Trust Map links",
                "codex/dx-g02-readme-trust-map-links",
                "AgentDecisionExplorerReadmeTrustMapDocumentationTest",
                "read-only",
                "simulation-only")) {
            assertTrue(board.contains(expected), "board should contain " + expected);
        }
    }

    @Test
    void readmeAndTrustMapAvoidUnsupportedDecisionExplorerOverclaims() throws IOException {
        String normalized = (read(README) + "\n" + read(TRUST_MAP)).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "decision explorer is implemented",
                "decision explorer endpoint",
                "production readiness is proven",
                "certified production",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "autonomous production action")) {
            assertFalse(normalized.contains(forbidden), "Decision Explorer docs must not overclaim " + forbidden);
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
