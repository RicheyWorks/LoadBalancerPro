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

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioEvidencePacket;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingScenarioEvidencePacketBuilder;
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
class AdaptiveRoutingScenarioEvidencePacketTest {
    private static final String PACKET_ENDPOINT =
            "/api/enterprise-lab/adaptive-routing-scenario-evidence-packet";
    private static final String SUMMARY_ENDPOINT =
            "/api/enterprise-lab/adaptive-routing-scenario-summary";
    private static final String DETAIL_ENDPOINT =
            "/api/enterprise-lab/adaptive-routing-scenario-detail";
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
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path READINESS = Path.of("docs/CI_EVIDENCE_GATE_READINESS_LANE.md");
    private static final Path CONTRACT = Path.of("docs/CI_EVIDENCE_GATE_ARTIFACT_CONTRACT.md");
    private static final Path READINESS_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path PERFORMANCE_AUTH_LANE =
            Path.of("docs/MEASURED_PERFORMANCE_BASELINE_AND_AUTH_PROOF_LANE.md");
    private static final Path CI_GATE_PAGE = Path.of("src/main/resources/static/ci-evidence-gate.html");
    private static final Path INDEX = Path.of("src/main/resources/static/index.html");
    private static final Path REVIEWER = Path.of("src/main/resources/static/enterprise-lab-reviewer.html");
    private static final Path OPERATOR = Path.of("src/main/resources/static/operator-evidence-dashboard.html");
    private static final Path TIMELINE = Path.of("src/main/resources/static/evidence-timeline.html");
    private static final Path EXPORT_PACKET = Path.of("src/main/resources/static/evidence-export-packet.html");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void evidencePacketBuilderReturnsDeterministicReviewerPacket() {
        AdaptiveRoutingScenarioEvidencePacket first =
                new AdaptiveRoutingScenarioEvidencePacketBuilder(new AdaptiveRoutingScenarioRunner()).build();
        AdaptiveRoutingScenarioEvidencePacket second =
                new AdaptiveRoutingScenarioEvidencePacketBuilder(new AdaptiveRoutingScenarioRunner()).build();

        assertEquals(first, second, "packet should be deterministic across repeated builds");
        assertEquals("Adaptive Routing Scenario Evidence Packet", first.packetName());
        assertEquals("adaptive-routing-scenario-evidence-packet/v1", first.packetVersion());
        assertEquals("local-synthetic", first.mode());
        assertTrue(first.deterministic());
        assertEquals("/adaptive-routing-scenarios.html", first.dashboardPath());
        assertEquals(PACKET_ENDPOINT, first.apiPath());
        assertEquals(List.of(SUMMARY_ENDPOINT, DETAIL_ENDPOINT, PACKET_ENDPOINT), first.sourceEndpoints());
        assertEquals(3, first.scenarioSummary().scenarioCount());
        assertEquals(3, first.scenarioDrilldowns().scenarioCount());
        assertEquals(5, first.strategiesCompared().size());
        assertEquals(100, first.totalDecisions());
        assertEquals(100, selectedServerCountSum(first), "packet selected-server counts should sum to total decisions");
        assertFalse(first.explanationNotes().isEmpty(), "packet should carry explanation notes");
        assertTrue(first.warnings().stream().anyMatch(warning -> warning.contains("in-memory endpoint output only")));
        assertEquals("READY_FOR_LOCAL_CI_GATE_REVIEW", first.readinessForCiGate());
        assertTrue(first.localEvidencePaths().stream().allMatch(path -> path.startsWith("target/")));
        assertTrue(first.localEvidencePaths().contains(
                "target/adaptive-routing-scenarios/adaptive-routing-scenario-evidence-packet.json"));
        assertTrue(first.notProvenBoundaries().contains("No live traffic validation"));
        assertTrue(first.safetyBoundaries().contains("no file reads or writes"));
        assertFalse(first.reviewerChecklist().isEmpty(), "packet should include reviewer checklist items");
        assertFalse(first.recommendedNextSteps().isEmpty(), "packet should include next steps");
        assertTrue(first.evidenceSections().size() >= 4, "packet should include evidence sections");
    }

    @Test
    void packetEndpointReturnsDeterministicPacket() throws Exception {
        String first = mockMvc.perform(get(PACKET_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.packetName", is("Adaptive Routing Scenario Evidence Packet")))
                .andExpect(jsonPath("$.packetVersion", is("adaptive-routing-scenario-evidence-packet/v1")))
                .andExpect(jsonPath("$.mode", is("local-synthetic")))
                .andExpect(jsonPath("$.deterministic", is(true)))
                .andExpect(jsonPath("$.apiPath", is(PACKET_ENDPOINT)))
                .andExpect(jsonPath("$.sourceEndpoints[0]", is(SUMMARY_ENDPOINT)))
                .andExpect(jsonPath("$.sourceEndpoints[1]", is(DETAIL_ENDPOINT)))
                .andExpect(jsonPath("$.sourceEndpoints[2]", is(PACKET_ENDPOINT)))
                .andExpect(jsonPath("$.scenarioSummary.scenarioCount", is(3)))
                .andExpect(jsonPath("$.scenarioDrilldowns.scenarioCount", is(3)))
                .andExpect(jsonPath("$.strategiesCompared.length()", is(5)))
                .andExpect(jsonPath("$.totalDecisions", is(100)))
                .andExpect(jsonPath("$.readinessForCiGate", is("READY_FOR_LOCAL_CI_GATE_REVIEW")))
                .andExpect(jsonPath("$.localEvidencePaths[1]",
                        is("target/adaptive-routing-scenarios/adaptive-routing-scenario-evidence-packet.json")))
                .andExpect(jsonPath("$.reviewerChecklist[0]", containsString("packetVersion")))
                .andExpect(jsonPath("$.notProvenBoundaries[0]", is("No production certification")))
                .andExpect(jsonPath("$.evidenceSections[0].sectionName", is("Packet Overview")))
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(get(PACKET_ENDPOINT))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(first, second, "packet endpoint response should be deterministic");
    }

    @Test
    void pageIsPackagedAndReferencesSameOriginPacketEndpoint() throws Exception {
        assertTrue(Files.exists(PAGE), "adaptive routing scenario page should be source-controlled");
        assertTrue(new ClassPathResource("static/adaptive-routing-scenarios.html").exists(),
                "adaptive routing scenario page should be packaged");

        mockMvc.perform(get("/adaptive-routing-scenarios.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Adaptive Routing Scenario Runner")))
                .andExpect(content().string(containsString("Evidence Packet")))
                .andExpect(content().string(containsString(PACKET_ENDPOINT)))
                .andExpect(content().string(containsString("reviewer packet")))
                .andExpect(content().string(containsString("in-memory only")))
                .andExpect(content().string(containsString("/ci-evidence-gate.html")))
                .andExpect(content().string(containsString("/evidence-timeline.html")))
                .andExpect(content().string(containsString("/enterprise-lab-reviewer.html")))
                .andExpect(content().string(containsString("/operator-evidence-dashboard.html")))
                .andExpect(content().string(containsString("/evidence-export-packet.html")));

        String page = read(PAGE);
        assertTrue(page.contains("const endpoint = \"" + SUMMARY_ENDPOINT + "\""));
        assertTrue(page.contains("const detailEndpoint = \"" + DETAIL_ENDPOINT + "\""));
        assertTrue(page.contains("const packetEndpoint = \"" + PACKET_ENDPOINT + "\""));
        assertTrue(page.contains("fetch(endpoint"));
        assertTrue(page.contains("fetch(detailEndpoint"));
        assertTrue(page.contains("fetch(packetEndpoint"));
        assertNoExternalBrowserCalls(page);
    }

    @Test
    void docsAndReviewerSurfacesLinkThePacketAsLocalPrototypeOnly() throws Exception {
        for (Path path : List.of(
                README,
                TRUST_MAP,
                READINESS,
                CONTRACT,
                READINESS_AUDIT,
                PERFORMANCE_AUTH_LANE,
                CI_GATE_PAGE,
                INDEX,
                REVIEWER,
                OPERATOR,
                TIMELINE,
                EXPORT_PACKET)) {
            String content = read(path);
            assertTrue(content.contains("/adaptive-routing-scenarios.html"),
                    path + " should link the scenario runner page");
            assertTrue(content.contains(SUMMARY_ENDPOINT),
                    path + " should reference the summary API");
            assertTrue(content.contains(DETAIL_ENDPOINT),
                    path + " should reference the detail API");
            assertTrue(content.contains(PACKET_ENDPOINT),
                    path + " should reference the evidence packet API");
        }
    }

    @Test
    void packetEndpointCodeAvoidsSideEffectsAndUnsafeClaims() throws Exception {
        String endpointCode = read(CONTROLLER) + "\n"
                + read(CI_GATE_CONTROLLER) + "\n"
                + read(RUNNER) + "\n"
                + read(PACKET) + "\n"
                + read(PACKET_BUILDER) + "\n"
                + read(PACKET_SECTION);
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
                    "packet endpoint and model must not include side-effect behavior: " + prohibited);
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
                "artifact generated for this run")) {
            assertFalse(combined.contains(prohibited), "packet surface must not include " + prohibited);
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
                    "new evidence packet surface must not include unsafe command: " + prohibitedCommand);
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
            assertFalse(newSurface.contains(secretLike), "new packet surface should not include " + secretLike);
        }
    }

    private static int selectedServerCountSum(AdaptiveRoutingScenarioEvidencePacket packet) {
        return packet.selectedServerDistributions().values().stream()
                .flatMap(strategyCounts -> strategyCounts.values().stream())
                .flatMap(serverCounts -> serverCounts.values().stream())
                .mapToInt(Integer::intValue)
                .sum();
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
