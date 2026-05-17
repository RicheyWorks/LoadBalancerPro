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

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioResult;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioRunner;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioSummary;

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
class AdaptiveRoutingScenarioRunnerTest {
    private static final Path PAGE = Path.of("src/main/resources/static/adaptive-routing-scenarios.html");
    private static final Path CONTROLLER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/"
                    + "EnterpriseLabAdaptiveRoutingScenarioController.java");
    private static final Path RUNNER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/AdaptiveRoutingScenarioRunner.java");
    private static final Path INDEX = Path.of("src/main/resources/static/index.html");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path READINESS = Path.of("docs/CI_EVIDENCE_GATE_READINESS_LANE.md");
    private static final Path CONTRACT = Path.of("docs/CI_EVIDENCE_GATE_ARTIFACT_CONTRACT.md");
    private static final Path READINESS_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final List<Path> REVIEWER_SURFACES = List.of(
            Path.of("src/main/resources/static/ci-evidence-gate.html"),
            Path.of("src/main/resources/static/enterprise-lab-reviewer.html"),
            Path.of("src/main/resources/static/operator-evidence-dashboard.html"),
            Path.of("src/main/resources/static/evidence-timeline.html"),
            Path.of("src/main/resources/static/evidence-export-packet.html"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void scenarioRunnerReturnsDeterministicLocalSyntheticSummary() {
        AdaptiveRoutingScenarioRunner runner = new AdaptiveRoutingScenarioRunner();

        AdaptiveRoutingScenarioSummary first = runner.runSummary();
        AdaptiveRoutingScenarioSummary second = runner.runSummary();

        assertEquals(first, second, "scenario runner output should be deterministic across repeated calls");
        assertEquals("Adaptive Routing Scenario Runner", first.runnerName());
        assertEquals("adaptive-routing-scenario-runner/v1", first.runnerVersion());
        assertEquals("local-synthetic", first.mode());
        assertTrue(first.deterministic());
        assertEquals(3, first.scenarioCount());
        assertEquals(List.of(
                "TAIL_LATENCY_POWER_OF_TWO",
                "WEIGHTED_LEAST_LOAD",
                "WEIGHTED_LEAST_CONNECTIONS",
                "WEIGHTED_ROUND_ROBIN",
                "ROUND_ROBIN"), first.strategiesCompared());
        assertEquals(100, first.totalDecisions());
        assertTrue(first.scenarios().stream()
                .map(AdaptiveRoutingScenarioResult::scenarioName)
                .toList()
                .containsAll(List.of(
                        "balanced-weighted-local-synthetic",
                        "tail-latency-degradation-local-synthetic",
                        "capacity-pressure-local-synthetic")));
    }

    @Test
    void selectedServerCountsAreStableAndSumToDecisionTotals() {
        AdaptiveRoutingScenarioSummary summary = new AdaptiveRoutingScenarioRunner().runSummary();

        for (AdaptiveRoutingScenarioResult scenario : summary.scenarios()) {
            int selectedCountSum = scenario.selectedServerCounts().values().stream()
                    .flatMap(counts -> counts.values().stream())
                    .mapToInt(Integer::intValue)
                    .sum();
            assertEquals(scenario.totalDecisions(), selectedCountSum,
                    scenario.scenarioName() + " selected counts should sum to total decisions");
            assertTrue(scenario.totalDecisions() > 0, scenario.scenarioName() + " should select at least once");
            assertEquals(scenario.measurementSummary().selectedDecisionCount(), scenario.totalDecisions());
            assertEquals(scenario.measurementSummary().attemptedDecisions(),
                    scenario.measurementSummary().selectedDecisionCount()
                            + scenario.measurementSummary().noSelectionDecisionCount());
            assertTrue(scenario.decisionNotes().stream().anyMatch(note -> note.contains("ROUND_ROBIN")),
                    "decision notes should include strategy output");
            assertTrue(scenario.warnings().stream()
                    .anyMatch(warning -> warning.contains("Local synthetic measurement only")));
            assertTrue(scenario.notProvenBoundaries().contains("No production benchmark or machine-speed claim"));
            assertEquals("READY_FOR_LOCAL_CI_GATE_REVIEW", scenario.readinessForCiGate());
            assertTrue(scenario.localEvidencePaths().stream().allMatch(path -> path.startsWith("target/")),
                    "local evidence path shape should stay under ignored target/");
        }
    }

    @Test
    void endpointReturnsDeterministicScenarioSummary() throws Exception {
        String first = mockMvc.perform(get("/api/enterprise-lab/adaptive-routing-scenario-summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.runnerName", is("Adaptive Routing Scenario Runner")))
                .andExpect(jsonPath("$.runnerVersion", is("adaptive-routing-scenario-runner/v1")))
                .andExpect(jsonPath("$.mode", is("local-synthetic")))
                .andExpect(jsonPath("$.deterministic", is(true)))
                .andExpect(jsonPath("$.dashboardPath", is("/adaptive-routing-scenarios.html")))
                .andExpect(jsonPath("$.apiPath",
                        is("/api/enterprise-lab/adaptive-routing-scenario-summary")))
                .andExpect(jsonPath("$.scenarioCount", is(3)))
                .andExpect(jsonPath("$.strategiesCompared[0]", is("TAIL_LATENCY_POWER_OF_TWO")))
                .andExpect(jsonPath("$.strategiesCompared[4]", is("ROUND_ROBIN")))
                .andExpect(jsonPath("$.totalDecisions", is(100)))
                .andExpect(jsonPath("$.scenarios[0].mode", is("local-synthetic")))
                .andExpect(jsonPath("$.scenarios[0].deterministic", is(true)))
                .andExpect(jsonPath("$.scenarios[0].totalDecisions", is(40)))
                .andExpect(jsonPath("$.scenarios[0].measurementSummary.attemptedDecisions", is(40)))
                .andExpect(jsonPath("$.scenarios[0].selectedServerCounts.ROUND_ROBIN.edge-a", is(3)))
                .andExpect(jsonPath("$.localEvidencePaths[0]",
                        is("target/adaptive-routing-scenarios/adaptive-routing-scenario-summary.json")))
                .andExpect(jsonPath("$.notProvenBoundaries[0]", is("No production certification")))
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(get("/api/enterprise-lab/adaptive-routing-scenario-summary"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(first, second, "endpoint response should be deterministic");
    }

    @Test
    void scenarioRunnerPageIsPackagedAndUsesSameOriginEndpointOnly() throws Exception {
        assertTrue(Files.exists(PAGE), "adaptive routing scenario page should be source-controlled");
        assertTrue(new ClassPathResource("static/adaptive-routing-scenarios.html").exists(),
                "adaptive routing scenario page should be packaged");

        mockMvc.perform(get("/adaptive-routing-scenarios.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Adaptive Routing Scenario Runner")))
                .andExpect(content().string(containsString(
                        "/api/enterprise-lab/adaptive-routing-scenario-summary")))
                .andExpect(content().string(containsString("local-synthetic")))
                .andExpect(content().string(containsString("not a production benchmark")))
                .andExpect(content().string(containsString("/ci-evidence-gate.html")))
                .andExpect(content().string(containsString("/evidence-timeline.html")))
                .andExpect(content().string(containsString("/enterprise-lab-reviewer.html")))
                .andExpect(content().string(containsString("/operator-evidence-dashboard.html")))
                .andExpect(content().string(containsString("/evidence-export-packet.html")));

        String page = read(PAGE);
        assertTrue(page.contains("fetch(endpoint"),
                "page should fetch through the same-origin endpoint constant");
        assertTrue(page.contains("const endpoint = \"/api/enterprise-lab/adaptive-routing-scenario-summary\""));
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
            assertFalse(page.contains(prohibited), "page should not include external/browser persistence call "
                    + prohibited);
        }
    }

    @Test
    void integrationLinksArePresentWithoutMakingTheSurfaceNoisy() throws Exception {
        for (Path path : List.of(INDEX, README, TRUST_MAP, READINESS, CONTRACT, READINESS_AUDIT)) {
            String content = read(path);
            assertTrue(content.contains("/adaptive-routing-scenarios.html"),
                    path + " should link the scenario runner page");
            assertTrue(content.contains("/api/enterprise-lab/adaptive-routing-scenario-summary"),
                    path + " should reference the scenario runner API");
        }

        for (Path page : REVIEWER_SURFACES) {
            String content = read(page);
            assertTrue(content.contains("/adaptive-routing-scenarios.html"),
                    page + " should link the scenario runner page");
        }
    }

    @Test
    void runnerEndpointAndPageAvoidUnsafeBehaviorAndOverclaims() throws Exception {
        String endpointCode = read(CONTROLLER) + "\n" + read(RUNNER);
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
                    "scenario endpoint/runner must not include side-effect behavior: " + prohibited);
        }

        String combined = (read(PAGE) + "\n" + endpointCode + "\n" + read(README) + "\n" + read(TRUST_MAP))
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
                "benchmark result:",
                "requests per second is")) {
            assertFalse(combined.contains(prohibited), "scenario runner surface must not include " + prohibited);
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
                    "new scenario runner surface must not include unsafe command: " + prohibitedCommand);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
