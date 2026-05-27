package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DecisionExplorerReviewerNavigationTest {
    private static final Path INDEX = Path.of("src/main/resources/static/index.html");
    private static final Path DECISION_EXPLORER_PAGE =
            Path.of("src/main/resources/static/decision-explorer.html");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path API_CONTRACTS = Path.of("docs/API_CONTRACTS.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/api/DecisionExplorerReviewerNavigationTest.java");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootPageLinksDecisionExplorerFromNavigationActionsAndCards() throws Exception {
        String index = read(INDEX);

        assertTrue(index.contains("href=\"/decision-explorer.html\""));
        assertTrue(index.contains("Open Decision Explorer"));
        assertTrue(index.contains("DecisionExplorerPayloadV1"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/decision-explorer.html")))
                .andExpect(content().string(containsString("Open Decision Explorer")));
    }

    @Test
    void decisionExplorerPageContainsReviewerNavigationAndPolishedSections() throws Exception {
        String page = read(DECISION_EXPLORER_PAGE);

        for (String expected : List.of(
                "Reviewer Navigation",
                "Open Web Cockpit",
                "Open Routing Proof",
                "Open Reviewer Dashboard",
                "Open Operator Evidence",
                "Display order is stable and follows the service-sorted payload order",
                "Unavailable sections stay explicit",
                "Scenario Catalog",
                "Load Scenario Catalog",
                "Category filter",
                "Evidence filter",
                "/api/routing/decision-explorer/scenarios",
                "Candidate Comparison",
                "Factor Drill-Down",
                "No candidate comparison rows returned.",
                "No factor drill-down readouts returned.",
                "Decision Diffs",
                "Evidence Packet Readouts",
                "Agent Structured Output",
                "No decision diff readouts returned.",
                "No evidence packet readouts returned.",
                "No agent structured output returned.",
                "selected-row",
                "Rank")) {
            assertTrue(page.contains(expected), "Decision Explorer page should contain " + expected);
        }
    }

    @Test
    void readmeTrustMapAndApiContractsLinkCurrentDecisionExplorerSurface() throws Exception {
        String combined = read(README) + "\n" + read(TRUST_MAP) + "\n" + read(API_CONTRACTS);

        for (String expected : List.of(
                "http://localhost:8080/decision-explorer.html",
                "/decision-explorer.html",
                "POST /api/routing/decision-explorer",
                "docs/API_CONTRACTS.md",
                "API_CONTRACTS.md",
                "Decision Explorer Implementation Phase 1",
                "same-origin",
                "memory-only",
                "read-only",
                "simulation-only",
                "DecisionExplorerPayloadV1")) {
            assertTrue(combined.contains(expected), "reviewer docs should contain " + expected);
        }
    }

    @Test
    void reviewerNavigationPreservesDecisionExplorerSafetyBoundaries() throws Exception {
        String normalized = (read(README) + "\n" + read(TRUST_MAP) + "\n" + read(API_CONTRACTS) + "\n"
                + read(DECISION_EXPLORER_PAGE)).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not shift traffic",
                "mutate routing",
                "call cloud or tenant systems",
                "persist storage",
                "execute replay",
                "export files",
                "generate evidence packets",
                "production readiness",
                "live-cloud validation",
                "real-tenant validation",
                "throughput/p95/p99")) {
            assertTrue(normalized.contains(expected), "navigation docs should preserve boundary " + expected);
        }

        for (String forbidden : List.of(
                "production readiness is proven",
                "certified production",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "traffic shifting enabled",
                "autonomous production action enabled",
                "evidence packet generated")) {
            assertFalse(normalized.contains(forbidden), "Decision Explorer navigation must not overclaim " + forbidden);
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

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
