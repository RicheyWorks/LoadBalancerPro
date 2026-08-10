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
    private static final Path PAGE = Path.of("src/main/resources/static/decision-explorer.html");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootPageLinksTheCurrentDecisionExplorer() throws Exception {
        String index = read(INDEX);

        assertTrue(index.contains("href=\"/decision-explorer.html\""));
        assertTrue(index.contains("Open Decision Explorer"));
        assertTrue(index.contains("RoutingExplanation"));
        assertFalse(index.contains("DecisionExplorerPayloadV1"));
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/decision-explorer.html")))
                .andExpect(content().string(containsString("Open Decision Explorer")));
    }

    @Test
    void pageKeepsReviewerNavigationAndCompactSections() throws Exception {
        String page = read(PAGE);

        for (String expected : List.of(
                "Reviewer Navigation",
                "Routing proof",
                "Web cockpit",
                "Reviewer dashboard",
                "Operator evidence",
                "Scenario Catalog",
                "Candidate factor contributions",
                "Dominant + delta analysis",
                "Counterfactual ±weight scenarios",
                "Raw compact payload")) {
            assertTrue(page.contains(expected), "page should contain " + expected);
        }
    }

    @Test
    void decisionExplorerPagePreservesSafetyBoundaries() throws Exception {
        String normalized = read(PAGE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "production readiness",
                "live-cloud validation",
                "real-tenant validation",
                "no replay execution",
                "persistent storage")) {
            assertTrue(normalized.contains(expected), "decision explorer should preserve " + expected);
        }
        for (String forbidden : List.of(
                "production readiness is proven",
                "certified production",
                "live-cloud validated",
                "real tenant validated",
                "throughput proven",
                "traffic shifting enabled",
                "autonomous production action enabled")) {
            assertFalse(normalized.contains(forbidden), "reviewer path must not overclaim " + forbidden);
        }
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
