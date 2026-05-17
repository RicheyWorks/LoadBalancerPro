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

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioEvidencePacketBuilder;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioGateEvaluation;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioGateEvaluator;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioRunner;

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
class AdaptiveRoutingScenarioGateEvaluationTest {
    private static final String SUMMARY_ENDPOINT =
            "/api/enterprise-lab/adaptive-routing-scenario-summary";
    private static final String DETAIL_ENDPOINT =
            "/api/enterprise-lab/adaptive-routing-scenario-detail";
    private static final String PACKET_ENDPOINT =
            "/api/enterprise-lab/adaptive-routing-scenario-evidence-packet";
    private static final String GATE_ENDPOINT =
            "/api/enterprise-lab/adaptive-routing-scenario-gate-evaluation";
    private static final Path PAGE = Path.of("src/main/resources/static/adaptive-routing-scenarios.html");
    private static final Path CONTROLLER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/"
                    + "EnterpriseLabAdaptiveRoutingScenarioController.java");
    private static final Path CI_GATE_CONTROLLER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/"
                    + "EnterpriseLabCiEvidenceGateSummaryController.java");
    private static final Path RUNNER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/AdaptiveRoutingScenarioRunner.java");
    private static final Path PACKET = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/"
                    + "AdaptiveRoutingScenarioEvidencePacket.java");
    private static final Path PACKET_BUILDER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/"
                    + "AdaptiveRoutingScenarioEvidencePacketBuilder.java");
    private static final Path PACKET_SECTION = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/"
                    + "AdaptiveRoutingScenarioEvidenceSection.java");
    private static final Path GATE_EVALUATION = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/"
                    + "AdaptiveRoutingScenarioGateEvaluation.java");
    private static final Path GATE_EVALUATOR = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/"
                    + "AdaptiveRoutingScenarioGateEvaluator.java");
    private static final Path GATE_FINDING = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/core/"
                    + "AdaptiveRoutingScenarioGateFinding.java");
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
    void gateEvaluatorReturnsDeterministicLocalFindings() {
        AdaptiveRoutingScenarioGateEvaluator evaluator = new AdaptiveRoutingScenarioGateEvaluator(
                new AdaptiveRoutingScenarioEvidencePacketBuilder(new AdaptiveRoutingScenarioRunner()));
        AdaptiveRoutingScenarioGateEvaluation first = evaluator.evaluate();
        AdaptiveRoutingScenarioGateEvaluation second = evaluator.evaluate();

        assertEquals(first, second, "gate evaluation should be deterministic across repeated builds");
        assertEquals("Adaptive Routing Scenario Gate Evaluation", first.evaluationName());
        assertEquals("adaptive-routing-scenario-gate-evaluation/v1", first.evaluationVersion());
        assertEquals("local-synthetic", first.mode());
        assertTrue(first.deterministic());
        assertEquals("NOT_ENFORCED", first.enforcementStatus());
        assertEquals("REVIEW_WARNINGS_PRESENT", first.decision());
        assertEquals("/adaptive-routing-scenarios.html", first.dashboardPath());
        assertEquals(GATE_ENDPOINT, first.apiPath());
        assertEquals(PACKET_ENDPOINT, first.packetSourceEndpoint());
        assertEquals("adaptive-routing-scenario-evidence-packet/v1", first.packetVersion());
        assertEquals(List.of(SUMMARY_ENDPOINT, DETAIL_ENDPOINT, PACKET_ENDPOINT, GATE_ENDPOINT),
                first.sourceEndpoints());
        assertEquals(3, first.scenarioCount());
        assertEquals(5, first.strategyCount());
        assertEquals(100, first.totalDecisions());
        assertEquals(12, first.totalFindings());
        assertEquals(10, first.passedFindings());
        assertEquals(2, first.warningFindings());
        assertEquals(0, first.failedFindings());
        assertEquals(first.totalFindings(),
                first.passedFindings() + first.warningFindings() + first.failedFindings());
        assertTrue(first.findings().stream().anyMatch(finding -> "PASS".equals(finding.status())));
        assertTrue(first.findings().stream().anyMatch(finding -> "WARN".equals(finding.status())));
        assertFalse(first.findings().stream().anyMatch(finding -> "FAIL".equals(finding.status())));
        assertTrue(first.reviewerActions().stream().anyMatch(action -> action.contains("Gate evaluation is not enforced")));
        assertTrue(first.notProvenBoundaries().contains("No live traffic validation"));
        assertTrue(first.notProvenBoundaries().contains("No GitHub governance-applied proof"));
        assertTrue(first.safetyBoundaries().contains("no file reads or writes"));
    }

    @Test
    void gateEvaluationEndpointReturnsDeterministicLocalEvaluation() throws Exception {
        String first = mockMvc.perform(get(GATE_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.evaluationName", is("Adaptive Routing Scenario Gate Evaluation")))
                .andExpect(jsonPath("$.evaluationVersion", is("adaptive-routing-scenario-gate-evaluation/v1")))
                .andExpect(jsonPath("$.mode", is("local-synthetic")))
                .andExpect(jsonPath("$.deterministic", is(true)))
                .andExpect(jsonPath("$.enforcementStatus", is("NOT_ENFORCED")))
                .andExpect(jsonPath("$.decision", is("REVIEW_WARNINGS_PRESENT")))
                .andExpect(jsonPath("$.apiPath", is(GATE_ENDPOINT)))
                .andExpect(jsonPath("$.packetSourceEndpoint", is(PACKET_ENDPOINT)))
                .andExpect(jsonPath("$.packetVersion", is("adaptive-routing-scenario-evidence-packet/v1")))
                .andExpect(jsonPath("$.sourceEndpoints[0]", is(SUMMARY_ENDPOINT)))
                .andExpect(jsonPath("$.sourceEndpoints[1]", is(DETAIL_ENDPOINT)))
                .andExpect(jsonPath("$.sourceEndpoints[2]", is(PACKET_ENDPOINT)))
                .andExpect(jsonPath("$.sourceEndpoints[3]", is(GATE_ENDPOINT)))
                .andExpect(jsonPath("$.scenarioCount", is(3)))
                .andExpect(jsonPath("$.strategyCount", is(5)))
                .andExpect(jsonPath("$.totalDecisions", is(100)))
                .andExpect(jsonPath("$.totalFindings", is(12)))
                .andExpect(jsonPath("$.passedFindings", is(10)))
                .andExpect(jsonPath("$.warningFindings", is(2)))
                .andExpect(jsonPath("$.failedFindings", is(0)))
                .andExpect(jsonPath("$.findings[0].status", is("PASS")))
                .andExpect(jsonPath("$.findings[2].status", is("WARN")))
                .andExpect(jsonPath("$.reviewerActions[0]", containsString("Review packet version")))
                .andExpect(jsonPath("$.notProvenBoundaries[0]", is("No production certification")))
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(get(GATE_ENDPOINT))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(first, second, "gate evaluation endpoint response should be deterministic");
    }

    @Test
    void pageIsPackagedAndReferencesSameOriginGateEvaluationEndpoint() throws Exception {
        assertTrue(Files.exists(PAGE), "adaptive routing scenario page should be source-controlled");
        assertTrue(new ClassPathResource("static/adaptive-routing-scenarios.html").exists(),
                "adaptive routing scenario page should be packaged");

        mockMvc.perform(get("/adaptive-routing-scenarios.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Adaptive Routing Scenario Runner")))
                .andExpect(content().string(containsString("Gate Evaluation")))
                .andExpect(content().string(containsString(GATE_ENDPOINT)))
                .andExpect(content().string(containsString("NOT_ENFORCED")))
                .andExpect(content().string(containsString("pass/warn/fail")))
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
        assertTrue(page.contains("fetch(endpoint"));
        assertTrue(page.contains("fetch(detailEndpoint"));
        assertTrue(page.contains("fetch(packetEndpoint"));
        assertTrue(page.contains("fetch(gateEvaluationEndpoint"));
        assertNoExternalBrowserCalls(page);
    }

    @Test
    void ciEvidenceGateAndDocsLinkGateEvaluationAsPrototypeOnly() throws Exception {
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
            assertTrue(content.contains(SUMMARY_ENDPOINT),
                    path + " should reference the summary API");
            assertTrue(content.contains(DETAIL_ENDPOINT),
                    path + " should reference the detail API");
            assertTrue(content.contains(PACKET_ENDPOINT),
                    path + " should reference the evidence packet API");
            assertTrue(content.contains(GATE_ENDPOINT),
                    path + " should reference the gate evaluation API");
        }

        assertTrue(read(CI_GATE_PAGE).contains("NOT_ENFORCED"),
                "CI evidence gate page should keep not-enforced status visible");
        assertTrue(read(READINESS).contains("NOT_ENFORCED"),
                "readiness lane should keep not-enforced status visible");
    }

    @Test
    void gateEvaluationEndpointCodeAvoidsSideEffectsAndUnsafeClaims() throws Exception {
        String endpointCode = read(CONTROLLER) + "\n"
                + read(CI_GATE_CONTROLLER) + "\n"
                + read(RUNNER) + "\n"
                + read(PACKET) + "\n"
                + read(PACKET_BUILDER) + "\n"
                + read(PACKET_SECTION) + "\n"
                + read(GATE_EVALUATION) + "\n"
                + read(GATE_EVALUATOR) + "\n"
                + read(GATE_FINDING);
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
                    "gate endpoint and model must not include side-effect behavior: " + prohibited);
        }

        String combined = (read(PAGE) + "\n"
                + endpointCode + "\n"
                + read(README) + "\n"
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
                "live ci enforcement")) {
            assertFalse(combined.contains(prohibited), "gate evaluation surface must not include " + prohibited);
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
                    "new gate evaluation surface must not include unsafe command: " + prohibitedCommand);
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
            assertFalse(newSurface.contains(secretLike), "new gate evaluation surface should not include " + secretLike);
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
