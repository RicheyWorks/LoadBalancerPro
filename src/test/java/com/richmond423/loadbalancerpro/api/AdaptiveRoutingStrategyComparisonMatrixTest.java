package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioGateEvaluator;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioRunner;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingStrategyComparisonMatrix;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingStrategyComparisonMatrixBuilder;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingStrategyComparisonRow;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingStrategyScenarioCell;

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
class AdaptiveRoutingStrategyComparisonMatrixTest {
    private static final String SUMMARY_ENDPOINT =
            "/api/enterprise-lab/adaptive-routing-scenario-summary";
    private static final String DETAIL_ENDPOINT =
            "/api/enterprise-lab/adaptive-routing-scenario-detail";
    private static final String PACKET_ENDPOINT =
            "/api/enterprise-lab/adaptive-routing-scenario-evidence-packet";
    private static final String GATE_ENDPOINT =
            "/api/enterprise-lab/adaptive-routing-scenario-gate-evaluation";
    private static final String MATRIX_ENDPOINT =
            "/api/enterprise-lab/adaptive-routing-strategy-comparison-matrix";
    private static final Path PAGE = Path.of("src/main/resources/static/adaptive-routing-scenarios.html");
    private static final Path CONTROLLER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/"
                    + "EnterpriseLabAdaptiveRoutingScenarioController.java");
    private static final Path CI_GATE_CONTROLLER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/"
                    + "EnterpriseLabCiEvidenceGateSummaryController.java");
    private static final Path RUNNER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/AdaptiveRoutingScenarioRunner.java");
    private static final Path MATRIX = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/"
                    + "AdaptiveRoutingStrategyComparisonMatrix.java");
    private static final Path MATRIX_BUILDER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/"
                    + "AdaptiveRoutingStrategyComparisonMatrixBuilder.java");
    private static final Path MATRIX_ROW = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/"
                    + "AdaptiveRoutingStrategyComparisonRow.java");
    private static final Path MATRIX_CELL = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/"
                    + "AdaptiveRoutingStrategyScenarioCell.java");
    private static final Path MATRIX_INSIGHT = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/"
                    + "AdaptiveRoutingStrategyComparisonInsight.java");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path READINESS = Path.of("docs/CI_EVIDENCE_GATE_READINESS_LANE.md");
    private static final Path CONTRACT = Path.of("docs/CI_EVIDENCE_GATE_ARTIFACT_CONTRACT.md");
    private static final Path READINESS_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path PERFORMANCE_AUTH_LANE =
            Path.of("docs/MEASURED_PERFORMANCE_BASELINE_AND_AUTH_PROOF_LANE.md");
    private static final Path CI_GATE_PAGE = Path.of("src/main/resources/static/ci-evidence-gate.html");
    private static final Path INDEX = Path.of("src/main/resources/static/index.html");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void matrixBuilderReturnsDeterministicStrategyByScenarioMatrix() {
        AdaptiveRoutingStrategyComparisonMatrix first =
                new AdaptiveRoutingStrategyComparisonMatrixBuilder(new AdaptiveRoutingScenarioRunner()).build();
        AdaptiveRoutingStrategyComparisonMatrix second =
                new AdaptiveRoutingStrategyComparisonMatrixBuilder(new AdaptiveRoutingScenarioRunner()).build();

        assertEquals(first, second, "matrix should be deterministic across repeated builds");
        assertEquals("Adaptive Routing Strategy Comparison Matrix", first.matrixName());
        assertEquals("adaptive-routing-strategy-comparison-matrix/v1", first.matrixVersion());
        assertEquals("local-synthetic", first.mode());
        assertTrue(first.deterministic());
        assertEquals(MATRIX_ENDPOINT, first.apiPath());
        assertEquals(List.of(SUMMARY_ENDPOINT, DETAIL_ENDPOINT, MATRIX_ENDPOINT), first.sourceEndpoints());
        assertEquals(3, first.scenarioCount());
        assertEquals(5, first.strategyCount());
        assertEquals(100, first.totalDecisions());
        assertEquals(5, first.rows().size());
        assertEquals(100, first.rows().stream().mapToInt(AdaptiveRoutingStrategyComparisonRow::totalDecisions).sum());
        assertFalse(first.insights().isEmpty(), "matrix should include reviewer insights");
        assertTrue(first.localEvidencePaths().stream().allMatch(path -> path.startsWith("target/")));
        assertTrue(first.localEvidencePaths().contains(
                "target/adaptive-routing-scenarios/adaptive-routing-strategy-comparison-matrix.json"));
        assertTrue(first.notProvenBoundaries().contains("No live traffic validation"));
        assertTrue(first.safetyBoundaries().contains("no file reads or writes"));

        for (AdaptiveRoutingStrategyComparisonRow row : first.rows()) {
            assertEquals(3, row.scenarioCount(), row.strategyName() + " should cover every scenario");
            assertEquals(3, row.cells().size(), row.strategyName() + " should have one cell per scenario");
            assertTrue(row.totalDecisions() > 0, row.strategyName() + " should have selected decisions");
            assertTrue(row.warnings().stream().anyMatch(warning -> warning.contains("output distribution only")));
            for (AdaptiveRoutingStrategyScenarioCell cell : row.cells()) {
                assertEquals(row.strategyName(), cell.strategyName());
                assertTrue(cell.decisionCount() > 0, "cell decisions should be nonzero");
                assertEquals(cell.decisionCount(),
                        cell.selectedServerDistribution().values().stream().mapToInt(Integer::intValue).sum());
                assertFalse(cell.selectedServerDistribution().isEmpty(), "cell distribution should exist");
                assertFalse("none".equals(cell.dominantSelectedServer()), "dominant server should be present");
                assertTrue(cell.distinctSelectedServers() > 0, "distinct selected-server count should be nonzero");
                assertFalse(cell.diversitySignal().isBlank(), "diversity signal should be present");
                assertTrue(cell.consistencySignal().contains("consistency"));
                assertTrue(cell.explanationNotes().stream()
                        .anyMatch(note -> note.contains("inferred from selected-server output distribution")));
                assertTrue(cell.notProvenBoundaries().contains("No production certification"));
            }
        }
    }

    @Test
    void matrixEndpointReturnsDeterministicLocalMatrix() throws Exception {
        String first = mockMvc.perform(get(MATRIX_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.matrixName", is("Adaptive Routing Strategy Comparison Matrix")))
                .andExpect(jsonPath("$.matrixVersion", is("adaptive-routing-strategy-comparison-matrix/v1")))
                .andExpect(jsonPath("$.mode", is("local-synthetic")))
                .andExpect(jsonPath("$.deterministic", is(true)))
                .andExpect(jsonPath("$.apiPath", is(MATRIX_ENDPOINT)))
                .andExpect(jsonPath("$.sourceEndpoints[0]", is(SUMMARY_ENDPOINT)))
                .andExpect(jsonPath("$.sourceEndpoints[1]", is(DETAIL_ENDPOINT)))
                .andExpect(jsonPath("$.sourceEndpoints[2]", is(MATRIX_ENDPOINT)))
                .andExpect(jsonPath("$.scenarioCount", is(3)))
                .andExpect(jsonPath("$.strategyCount", is(5)))
                .andExpect(jsonPath("$.totalDecisions", is(100)))
                .andExpect(jsonPath("$.rows.length()", is(5)))
                .andExpect(jsonPath("$.rows[0].cells.length()", is(3)))
                .andExpect(jsonPath("$.rows[0].cells[0].decisionCount", greaterThan(0)))
                .andExpect(jsonPath("$.rows[0].cells[0].selectedServerDistribution").exists())
                .andExpect(jsonPath("$.rows[0].cells[0].dominantSelectedServer").exists())
                .andExpect(jsonPath("$.rows[0].cells[0].diversitySignal").exists())
                .andExpect(jsonPath("$.rows[0].cells[0].consistencySignal").exists())
                .andExpect(jsonPath("$.rows[0].cells[0].reviewerActions[0]",
                        containsString("Review")))
                .andExpect(jsonPath("$.insights[0].title", containsString("Matrix")))
                .andExpect(jsonPath("$.warnings[0]", containsString("in-memory endpoint output")))
                .andExpect(jsonPath("$.notProvenBoundaries[0]", is("No production certification")))
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(get(MATRIX_ENDPOINT))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(first, second, "matrix endpoint response should be deterministic");
    }

    @Test
    void pageIsPackagedAndReferencesSameOriginMatrixEndpoint() throws Exception {
        assertTrue(Files.exists(PAGE), "adaptive routing scenario page should be source-controlled");
        assertTrue(new ClassPathResource("static/adaptive-routing-scenarios.html").exists(),
                "adaptive routing scenario page should be packaged");

        mockMvc.perform(get("/adaptive-routing-scenarios.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Adaptive Routing Scenario Runner")))
                .andExpect(content().string(containsString("Strategy Comparison Matrix")))
                .andExpect(content().string(containsString(MATRIX_ENDPOINT)))
                .andExpect(content().string(containsString("not hidden causality")))
                .andExpect(content().string(containsString("/ci-evidence-gate.html")))
                .andExpect(content().string(containsString("/evidence-timeline.html")))
                .andExpect(content().string(containsString("/enterprise-lab-reviewer.html")))
                .andExpect(content().string(containsString("/operator-evidence-dashboard.html")))
                .andExpect(content().string(containsString("/evidence-export-packet.html")));

        String page = read(PAGE);
        assertTrue(page.contains("const endpoint = \"" + SUMMARY_ENDPOINT + "\""));
        assertTrue(page.contains("const detailEndpoint = \"" + DETAIL_ENDPOINT + "\""));
        assertTrue(page.contains("const packetEndpoint = \"" + PACKET_ENDPOINT + "\""));
        assertTrue(page.contains("const gateEvaluationEndpoint = \"" + GATE_ENDPOINT + "\""));
        assertTrue(page.contains("const matrixEndpoint = \"" + MATRIX_ENDPOINT + "\""));
        assertTrue(page.contains("fetch(endpoint"));
        assertTrue(page.contains("fetch(detailEndpoint"));
        assertTrue(page.contains("fetch(packetEndpoint"));
        assertTrue(page.contains("fetch(gateEvaluationEndpoint"));
        assertTrue(page.contains("fetch(matrixEndpoint"));
        assertNoExternalBrowserCalls(page);
    }

    @Test
    void docsAndCiGateSurfacesLinkMatrixAsLocalPrototypeOnly() throws Exception {
        for (Path path : List.of(
                README,
                TRUST_MAP,
                READINESS,
                CONTRACT,
                READINESS_AUDIT,
                PERFORMANCE_AUTH_LANE,
                CI_GATE_PAGE,
                INDEX)) {
            String content = read(path);
            assertTrue(content.contains("/adaptive-routing-scenarios.html"),
                    path + " should link the scenario runner page");
            assertTrue(content.contains(MATRIX_ENDPOINT),
                    path + " should reference the strategy comparison matrix API");
        }

        assertTrue(read(CI_GATE_CONTROLLER).contains(MATRIX_ENDPOINT),
                "CI evidence gate summary should expose the matrix as a local/prototype evidence input");
        assertTrue(read(CI_GATE_PAGE).contains("NOT_ENFORCED"),
                "CI evidence gate page should keep not-enforced status visible");
    }

    @Test
    void matrixEndpointCodeAvoidsSideEffectsAndUnsafeClaims() throws Exception {
        String endpointCode = read(CONTROLLER) + "\n"
                + read(CI_GATE_CONTROLLER) + "\n"
                + read(RUNNER) + "\n"
                + read(MATRIX) + "\n"
                + read(MATRIX_BUILDER) + "\n"
                + read(MATRIX_ROW) + "\n"
                + read(MATRIX_CELL) + "\n"
                + read(MATRIX_INSIGHT);
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
                    "matrix endpoint and model must not include side-effect behavior: " + prohibited);
        }

        String combined = (read(PAGE) + "\n"
                + endpointCode + "\n"
                + read(TRUST_MAP) + "\n"
                + read(READINESS) + "\n"
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
                "requests per second is",
                "artifact generated for this run",
                "blocks merges today",
                "ci evidence gate is enforced",
                "live ci enforcement",
                "proves hidden causality")) {
            assertFalse(combined.contains(prohibited), "matrix surface must not include " + prohibited);
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
                    "new matrix surface must not include unsafe command: " + prohibitedCommand);
        }

        for (String secretLike : List.of(
                "ghp_",
                "github_pat_",
                "akia",
                "-----begin",
                "xoxb-",
                "xoxp-",
                "client_secret=",
                "password=",
                "bearer ")) {
            assertFalse(newSurface.contains(secretLike), "new matrix surface should not include " + secretLike);
        }
    }

    private static void assertNoExternalBrowserCalls(String page) {
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

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
