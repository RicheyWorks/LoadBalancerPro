package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DecisionExplorerStaticPageTest {
    private static final Path PAGE = Path.of("src/main/resources/static/decision-explorer.html");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pageIsServedWithTheCompactExplanationWorkflow() throws Exception {
        mockMvc.perform(get("/decision-explorer.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Decision Explorer")))
                .andExpect(content().string(containsString("RoutingExplanation")))
                .andExpect(content().string(containsString("data-action=\"run-decision-explorer\"")))
                .andExpect(content().string(containsString("/api/routing/decision-explorer")));
    }

    @Test
    void pageRendersOnlyTheRetainedEvidenceSections() throws Exception {
        String page = readPage();

        for (String expected : List.of(
                "Decision summary",
                "Candidate factor contributions",
                "Dominant + delta analysis",
                "Dominant factors",
                "Selected vs closest alternative",
                "Counterfactual ±weight scenarios",
                "Raw compact payload",
                "Not-proven boundaries",
                "Scenario Catalog",
                "Load scenarios",
                "Copy summary",
                "RoutingExplanation",
                "contract v2",
                "read-only",
                "simulation-only",
                "same-origin",
                "page memory only",
                "X-API-Key")) {
            assertTrue(page.contains(expected), "page should contain " + expected);
        }
        for (String retired : List.of(
                "Confidence Summary",
                "Routing Diagnostics",
                "Route Tradeoff Intelligence",
                "Shadow Decision Quality",
                "Replay Readiness",
                "Evidence Packet Readouts",
                "Agent Structured Output",
                "DecisionExplorerPayloadV1")) {
            assertFalse(page.contains(retired), "page should not retain " + retired);
        }
    }

    @Test
    void pageUsesSameOriginFetchAndNoPersistentOrUnsafeRendering() throws Exception {
        String page = readPage();

        assertTrue(page.contains("const DECISION_EXPLORER_ENDPOINT = \"/api/routing/decision-explorer\""));
        assertTrue(page.contains("const DECISION_EXPLORER_SCENARIOS_ENDPOINT = "
                + "\"/api/routing/decision-explorer/scenarios\""));
        assertTrue(page.contains("fetch(DECISION_EXPLORER_ENDPOINT"));
        assertTrue(page.contains("fetch(DECISION_EXPLORER_SCENARIOS_ENDPOINT"));
        assertTrue(page.contains("textContent"));
        assertTrue(page.contains("createElement"));
        assertFalse(page.contains("innerHTML"));
        assertFalse(page.contains("localStorage"));
        assertFalse(page.contains("sessionStorage"));
        assertFalse(page.contains("indexedDB"));
        assertFalse(page.contains("document.cookie"));
        assertFalse(page.contains("http://"));
        assertFalse(page.contains("https://"));
    }

    @Test
    void scenarioCatalogRemainsReadOnlyAndSimulationOnly() throws Exception {
        mockMvc.perform(get("/api/routing/decision-explorer/scenarios"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.readOnly").value(true))
                .andExpect(jsonPath("$.simulationOnly").value(true))
                .andExpect(jsonPath("$.scenarios").isArray())
                .andExpect(jsonPath("$.scenarios[0].scenarioId").exists());
    }

    @Test
    void pagePreservesSafetyAndNotProvenBoundaries() throws Exception {
        String normalized = readPage().toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no replay execution",
                "no persistence",
                "no external calls",
                "no production proof",
                "no production readiness",
                "no live-cloud validation",
                "no real-tenant validation",
                "no benchmark",
                "no routing mutation",
                "no production readiness or production certification")) {
            assertTrue(normalized.contains(expected), "page should preserve " + expected);
        }
        for (String forbidden : List.of(
                "production readiness is proven",
                "certified production",
                "live-cloud validated",
                "real tenant validated",
                "throughput proven",
                "traffic shifting enabled",
                "replay executed")) {
            assertFalse(normalized.contains(forbidden), "page must not overclaim " + forbidden);
        }
    }

    private static String readPage() throws Exception {
        assertTrue(Files.exists(PAGE));
        return Files.readString(PAGE, StandardCharsets.UTF_8);
    }
}
