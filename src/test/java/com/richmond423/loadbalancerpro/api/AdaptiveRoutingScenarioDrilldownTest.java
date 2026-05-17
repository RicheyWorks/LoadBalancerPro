package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioDetail;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioDrilldown;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioRunner;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioSummary;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingStrategyExplanation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdaptiveRoutingScenarioDrilldownTest {
    private static final Path PAGE = Path.of("src/main/resources/static/adaptive-routing-scenarios.html");
    private static final Path CONTROLLER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/"
                    + "EnterpriseLabAdaptiveRoutingScenarioController.java");
    private static final Path RUNNER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/AdaptiveRoutingScenarioRunner.java");
    private static final Path CANDIDATE_SIGNAL = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/AdaptiveRoutingCandidateSignal.java");
    private static final Path SCENARIO_DETAIL = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/AdaptiveRoutingScenarioDetail.java");
    private static final Path DRILLDOWN = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/AdaptiveRoutingScenarioDrilldown.java");
    private static final Path STRATEGY_EXPLANATION = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/AdaptiveRoutingStrategyExplanation.java");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path READINESS = Path.of("docs/CI_EVIDENCE_GATE_READINESS_LANE.md");
    private static final Path CONTRACT = Path.of("docs/CI_EVIDENCE_GATE_ARTIFACT_CONTRACT.md");
    private static final Path READINESS_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path CI_GATE_PAGE = Path.of("src/main/resources/static/ci-evidence-gate.html");
    private static final Path INDEX = Path.of("src/main/resources/static/index.html");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void drilldownReturnsDeterministicScenarioExplanations() {
        AdaptiveRoutingScenarioRunner runner = new AdaptiveRoutingScenarioRunner();

        AdaptiveRoutingScenarioDrilldown first = runner.runDrilldown();
        AdaptiveRoutingScenarioDrilldown second = runner.runDrilldown();

        assertEquals(first, second, "drilldown should be deterministic across repeated calls");
        assertEquals("Adaptive Routing Scenario Runner", first.runnerName());
        assertEquals("adaptive-routing-scenario-runner/v1", first.runnerVersion());
        assertEquals("local-synthetic", first.mode());
        assertTrue(first.deterministic());
        assertEquals("/adaptive-routing-scenarios.html", first.dashboardPath());
        assertEquals("/api/enterprise-lab/adaptive-routing-scenario-summary", first.summaryApiPath());
        assertEquals("/api/enterprise-lab/adaptive-routing-scenario-detail", first.detailApiPath());
        assertEquals(3, first.scenarioCount());
        assertEquals(100, first.totalDecisions());
        assertTrue(first.localEvidencePaths().stream().allMatch(path -> path.startsWith("target/")),
                "drilldown local evidence path shape should stay under ignored target/");
        assertTrue(first.notProvenBoundaries().contains("No production benchmark or machine-speed claim"));

        for (AdaptiveRoutingScenarioDetail scenario : first.scenarios()) {
            assertEquals("local-synthetic", scenario.mode());
            assertTrue(scenario.deterministic());
            assertEquals(5, scenario.strategyExplanations().size());
            assertFalse(scenario.candidates().isEmpty(), scenario.scenarioName() + " should expose candidate signals");
            assertTrue(scenario.explanationPolicy().contains("exposed strategy decision reasons"));
            assertTrue(scenario.warnings().stream()
                    .anyMatch(warning -> warning.contains("Local synthetic measurement only")));

            int selectedCountSum = scenario.selectedServerCounts().values().stream()
                    .flatMap(counts -> counts.values().stream())
                    .mapToInt(Integer::intValue)
                    .sum();
            assertEquals(scenario.totalDecisions(), selectedCountSum,
                    scenario.scenarioName() + " selected counts should sum to total decisions");

            for (AdaptiveRoutingStrategyExplanation explanation : scenario.strategyExplanations()) {
                assertEquals(scenario.scenarioName(), explanation.scenarioName());
                assertTrue(explanation.totalSelections() > 0,
                        explanation.strategyName() + " should have deterministic selected-server output");
                assertFalse(explanation.selectedServerCounts().isEmpty(),
                        explanation.strategyName() + " should expose selected-server distribution");
                assertFalse(explanation.dominantSelectedServer().isBlank());
                assertFalse(explanation.candidateServersConsidered().isEmpty());
                assertFalse(explanation.observedInputSignals().isEmpty());
                assertFalse(explanation.explanationNotes().isEmpty());
                assertTrue(explanation.explanationNotes().stream()
                        .anyMatch(note -> note.contains("observed output distribution")));
                assertTrue(explanation.cautionNotes().stream()
                        .anyMatch(note -> note.contains("Local synthetic explanation only")));
                assertTrue(explanation.notProvenBoundaries().contains("No live traffic validation"));
                assertEquals(explanation.totalSelections(),
                        explanation.selectedServerCounts().values().stream().mapToInt(Integer::intValue).sum());
            }
        }
    }

    @Test
    void detailEndpointReturnsDeterministicDrilldown() throws Exception {
        String first = mockMvc.perform(get("/api/enterprise-lab/adaptive-routing-scenario-detail"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.runnerName", is("Adaptive Routing Scenario Runner")))
                .andExpect(jsonPath("$.runnerVersion", is("adaptive-routing-scenario-runner/v1")))
                .andExpect(jsonPath("$.mode", is("local-synthetic")))
                .andExpect(jsonPath("$.deterministic", is(true)))
                .andExpect(jsonPath("$.summaryApiPath",
                        is("/api/enterprise-lab/adaptive-routing-scenario-summary")))
                .andExpect(jsonPath("$.detailApiPath",
                        is("/api/enterprise-lab/adaptive-routing-scenario-detail")))
                .andExpect(jsonPath("$.scenarioCount", is(3)))
                .andExpect(jsonPath("$.totalDecisions", is(100)))
                .andExpect(jsonPath("$.scenarios[0].mode", is("local-synthetic")))
                .andExpect(jsonPath("$.scenarios[0].deterministic", is(true)))
                .andExpect(jsonPath("$.scenarios[0].strategyExplanations[0].scenarioName",
                        is("balanced-weighted-local-synthetic")))
                .andExpect(jsonPath("$.scenarios[0].strategyExplanations[0].strategyName",
                        is("TAIL_LATENCY_POWER_OF_TWO")))
                .andExpect(jsonPath("$.scenarios[0].strategyExplanations[0].totalSelections", is(8)))
                .andExpect(jsonPath("$.scenarios[0].strategyExplanations[0].dominantSelectedServer",
                        containsString("edge-")))
                .andExpect(jsonPath("$.scenarios[0].strategyExplanations[0].observedInputSignals[0]",
                        containsString("Healthy candidates")))
                .andExpect(jsonPath("$.scenarios[0].strategyExplanations[0].explanationNotes[0]",
                        containsString("Observed distribution")))
                .andExpect(jsonPath("$.scenarios[0].strategyExplanations[0].cautionNotes[0]",
                        containsString("Local synthetic explanation only")))
                .andExpect(jsonPath("$.scenarios[2].selectedServerCounts.ROUND_ROBIN.capacity-a", is(3)))
                .andExpect(jsonPath("$.notProvenBoundaries[0]", is("No production certification")))
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(get("/api/enterprise-lab/adaptive-routing-scenario-detail"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(first, second, "detail endpoint response should be deterministic");
    }

    @Test
    void summaryEndpointRemainsStableWhileDrilldownIsAdded() {
        AdaptiveRoutingScenarioSummary summary = new AdaptiveRoutingScenarioRunner().runSummary();

        assertEquals("Adaptive Routing Scenario Runner", summary.runnerName());
        assertEquals("local-synthetic", summary.mode());
        assertTrue(summary.deterministic());
        assertEquals(3, summary.scenarioCount());
        assertEquals(100, summary.totalDecisions());
        assertEquals(List.of(
                "TAIL_LATENCY_POWER_OF_TWO",
                "WEIGHTED_LEAST_LOAD",
                "WEIGHTED_LEAST_CONNECTIONS",
                "WEIGHTED_ROUND_ROBIN",
                "ROUND_ROBIN"), summary.strategiesCompared());
    }

    @Test
    void pageIsPackagedAndReferencesSameOriginSummaryAndDetailEndpoints() throws Exception {
        assertTrue(Files.exists(PAGE), "adaptive routing scenario page should be source-controlled");
        assertTrue(new ClassPathResource("static/adaptive-routing-scenarios.html").exists(),
                "adaptive routing scenario page should be packaged");

        mockMvc.perform(get("/adaptive-routing-scenarios.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Adaptive Routing Scenario Runner")))
                .andExpect(content().string(containsString("Scenario Drilldown")))
                .andExpect(content().string(containsString("Strategy Explanations")))
                .andExpect(content().string(containsString(
                        "/api/enterprise-lab/adaptive-routing-scenario-summary")))
                .andExpect(content().string(containsString(
                        "/api/enterprise-lab/adaptive-routing-scenario-detail")))
                .andExpect(content().string(containsString("local synthetic reasoning")))
                .andExpect(content().string(containsString("not a production benchmark")))
                .andExpect(content().string(containsString("/ci-evidence-gate.html")))
                .andExpect(content().string(containsString("/evidence-timeline.html")))
                .andExpect(content().string(containsString("/enterprise-lab-reviewer.html")))
                .andExpect(content().string(containsString("/operator-evidence-dashboard.html")))
                .andExpect(content().string(containsString("/evidence-export-packet.html")));

        String page = read(PAGE);
        assertTrue(page.contains("const endpoint = \"/api/enterprise-lab/adaptive-routing-scenario-summary\""));
        assertTrue(page.contains("const detailEndpoint = \"/api/enterprise-lab/adaptive-routing-scenario-detail\""));
        assertTrue(page.contains("fetch(endpoint"));
        assertTrue(page.contains("fetch(detailEndpoint"));
        for (String prohibited : List.of(
                "http://",
                "https://",
                "XMLHttpRequest",
                "sendBeacon",
                "WebSocket",
                "EventSource",
                "localStorage",
                "sessionStorage",
                "importScripts")) {
            assertFalse(page.contains(prohibited), "page should not include external or persistent browser call "
                    + prohibited);
        }
    }

    @Test
    void reviewerAndCiEvidenceLinksIncludeTheDetailEndpointWithoutNoise() throws Exception {
        for (Path path : List.of(README, TRUST_MAP, READINESS, CONTRACT, READINESS_AUDIT, CI_GATE_PAGE, INDEX)) {
            String content = read(path);
            assertTrue(content.contains("/adaptive-routing-scenarios.html"),
                    path + " should link the scenario runner page");
            assertTrue(content.contains("/api/enterprise-lab/adaptive-routing-scenario-summary"),
                    path + " should reference the scenario runner summary API");
            assertTrue(content.contains("/api/enterprise-lab/adaptive-routing-scenario-detail"),
                    path + " should reference the scenario runner detail API");
        }
    }

    @Test
    void drilldownEndpointCodeAvoidsSideEffectsAndUnsafeClaims() throws Exception {
        String endpointCode = read(CONTROLLER) + "\n"
                + read(RUNNER) + "\n"
                + read(CANDIDATE_SIGNAL) + "\n"
                + read(SCENARIO_DETAIL) + "\n"
                + read(DRILLDOWN) + "\n"
                + read(STRATEGY_EXPLANATION);
        for (String prohibited : List.of(
                "Files.",
                "Path.of",
                "FileInputStream",
                "FileOutputStream",
                "Files.write",
                "ProcessBuilder",
                "Runtime.getRuntime",
                "System.getenv",
                "System.getProperty",
                "@Value",
                "Environment",
                "RestTemplate",
                "WebClient",
                "HttpClient",
                "URLConnection",
                "new URL",
                "java.io",
                "java.nio.file",
                "Thread.sleep",
                "Executor")) {
            assertFalse(endpointCode.contains(prohibited),
                    "scenario detail endpoint and model must not include side-effect behavior: " + prohibited);
        }

        String combined = (read(PAGE) + "\n"
                + endpointCode + "\n"
                + read(README) + "\n"
                + read(TRUST_MAP) + "\n"
                + read(CONTRACT))
                .toLowerCase(Locale.ROOT);
        for (String prohibited : List.of(
                "production benchmark complete",
                "production performance is proven",
                "slo/sla proof complete",
                "live traffic validated",
                "live cloud validated",
                "real tenant validation complete",
                "real enterprise idp validation complete",
                "registry publish complete",
                "signed container published",
                "github governance settings applied",
                "branch protection has been changed",
                "required checks were changed",
                "benchmark result:",
                "requests per second is")) {
            assertFalse(combined.contains(prohibited), "scenario drilldown surface must not include " + prohibited);
        }

        String newSurface = (read(PAGE) + "\n" + endpointCode).toLowerCase(Locale.ROOT);
        for (String prohibitedCommand : List.of(
                "gh release",
                "git tag",
                "docker push",
                "docker login",
                "cosign sign",
                "aws ",
                "az ",
                "gcloud",
                "kubectl",
                "terraform",
                "pulumi")) {
            assertFalse(newSurface.contains(prohibitedCommand),
                    "new scenario drilldown surface must not include unsafe command: " + prohibitedCommand);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
