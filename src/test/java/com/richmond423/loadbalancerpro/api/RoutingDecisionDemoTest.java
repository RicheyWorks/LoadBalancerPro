package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.core.CloudManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@SpringBootTest
@AutoConfigureMockMvc
class RoutingDecisionDemoTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path ROUTING_DEMO_PAGE = Path.of("src/main/resources/static/routing-demo.html");
    private static final Path COMPARE_FIXTURE =
            Path.of("src/test/resources/routing-demo/compare-strategies-sample.json");
    private static final Path LEAST_CONNECTIONS_FIXTURE =
            Path.of("src/test/resources/routing-demo/least-connections-sample.json");
    private static final List<Path> ROUTING_FIXTURES = List.of(
            COMPARE_FIXTURE,
            Path.of("src/test/resources/routing-demo/round-robin-sample.json"),
            Path.of("src/test/resources/routing-demo/weighted-sample.json"),
            LEAST_CONNECTIONS_FIXTURE,
            Path.of("src/test/resources/routing-demo/tail-latency-sample.json"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void routingDemoPageExistsAndIsServed() throws Exception {
        assertTrue(Files.exists(ROUTING_DEMO_PAGE), "routing browser demo page should be source-controlled");

        mockMvc.perform(get("/routing-demo.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Routing Decision Demo")))
                .andExpect(content().string(containsString("data-action=\"compare\"")))
                .andExpect(content().string(containsString("/api/routing/compare")));
    }

    @Test
    void routingDemoPageContainsExpectedEndpointsControlsAndStrategyText() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);

        assertTrue(page.contains("/api/health"));
        assertTrue(page.contains("/actuator/health/readiness"));
        assertTrue(page.contains("/api/routing/compare"));
        assertTrue(page.contains("Strategy comparison"));
        assertTrue(page.contains("Sample request editor/viewer"));
        assertTrue(page.contains("Results table/cards"));
        assertTrue(page.contains("Why this server?"));
        assertTrue(page.contains("Routing Proof Summary"));
        assertTrue(page.contains("Selected strategy"));
        assertTrue(page.contains("Selected backend/server"));
        assertTrue(page.contains("Key input signals"));
        assertTrue(page.contains("Fallback/degradation"));
        assertTrue(page.contains("Local/demo boundary"));
        assertTrue(page.contains("What this proves"));
        assertTrue(page.contains("What this does not prove"));
        assertTrue(page.contains("Copy proof summary"));
        assertTrue(page.contains("Copy proof commands"));
        assertTrue(page.contains("proof-summary-output"));
        assertTrue(page.contains("proof-commands"));
        assertTrue(page.contains("Reviewer Workflow Checklist"));
        assertTrue(page.contains("Copy end-to-end reviewer walkthrough"));
        assertTrue(page.contains("reviewer-walkthrough-output"));
        assertTrue(page.contains("Load sample scenario"));
        assertTrue(page.contains("Run routing comparison"));
        assertTrue(page.contains("Inspect Routing Proof Summary"));
        assertTrue(page.contains("Compare scenario deltas"));
        assertTrue(page.contains("Follow Evidence Navigation links"));
        assertTrue(page.contains("Copy reviewer proof note"));
        assertTrue(page.contains("Export/print packet from evidence export page"));
        assertTrue(page.contains("Reviewer Confidence Signals"));
        assertTrue(page.contains("Local repeatability"));
        assertTrue(page.contains("Deterministic sample scenarios"));
        assertTrue(page.contains("Same-origin API usage"));
        assertTrue(page.contains("Static browser-only notes/copy actions"));
        assertTrue(page.contains("Not-production-certified boundary"));
        assertTrue(page.contains("Scenario Comparison"));
        assertTrue(page.contains("Previous scenario"));
        assertTrue(page.contains("Current scenario"));
        assertTrue(page.contains("Selected strategy change"));
        assertTrue(page.contains("Selected backend/server change"));
        assertTrue(page.contains("Key input signal changes"));
        assertTrue(page.contains("Degradation/recovery explanation"));
        assertTrue(page.contains("Copy scenario comparison"));
        assertTrue(page.contains("scenario-comparison-output"));
        assertTrue(page.contains("Evidence Navigation"));
        assertTrue(page.contains("Routing evidence navigation links"));
        assertTrue(page.contains("/load-balancing-cockpit.html"));
        assertTrue(page.contains("/enterprise-lab-reviewer.html"));
        assertTrue(page.contains("/operator-evidence-dashboard.html"));
        assertTrue(page.contains("/evidence-timeline.html"));
        assertTrue(page.contains("/evidence-export-packet.html"));
        assertTrue(page.contains("Reviewer path"));
        assertTrue(page.contains("Where to go next after routing proof review"));
        assertTrue(page.contains("Next: Enterprise Lab reviewer dashboard"));
        assertTrue(page.contains("Next: Operator evidence dashboard"));
        assertTrue(page.contains("Next: Evidence timeline"));
        assertTrue(page.contains("Finish: Evidence export packet"));
        assertTrue(page.contains("after routing proof review"));
        assertTrue(page.contains("Copy curl"));
        assertTrue(page.contains("Copy payload"));
        assertTrue(page.contains("Copy summary"));
        assertTrue(page.contains("Copy raw response"));
        assertTrue(page.contains("Reset demo"));
        assertTrue(page.contains("Load sample scenario"));
        assertTrue(page.contains("Compare strategies"));
        assertTrue(page.contains("TAIL_LATENCY_POWER_OF_TWO"));
        assertTrue(page.contains("WEIGHTED_LEAST_LOAD"));
        assertTrue(page.contains("WEIGHTED_LEAST_CONNECTIONS"));
        assertTrue(page.contains("WEIGHTED_ROUND_ROBIN"));
        assertTrue(page.contains("ROUND_ROBIN"));
        assertFalse(page.contains("RESPONSE_TIME"));
    }

    @Test
    void routingDemoProofSummaryContainsLocalCommandsAndNotProvenBoundaries() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("mvn spring-boot:run"));
        assertTrue(page.contains("curl -fsS http://localhost:8080/api/health"));
        assertTrue(page.contains("curl -fsS http://localhost:8080/actuator/health/readiness"));
        assertTrue(page.contains("curl -fsS -X POST http://localhost:8080/api/routing/compare"));
        assertTrue(page.contains("--data-binary @routing-compare-request.json"));
        assertTrue(page.contains("same-origin local API"));
        assertTrue(page.contains("synthetic request payloads"));
        assertTrue(page.contains("browser-only summaries"));
        assertTrue(page.contains("selectedStrategy: "));
        assertTrue(page.contains("selectedBackend: "));
        assertTrue(page.contains("keyInputSignals: "));
        assertTrue(page.contains("fallbackDegradationBoundary: "));
        assertTrue(page.contains("localOnlyDemoBoundary: same-origin local API"));
        assertTrue(normalized.contains("no production deployment proof"));
        assertTrue(normalized.contains("no service-level agreement, service-level objective, or real tenant evidence"));
        assertTrue(normalized.contains("no live cloud validation, registry publication, or container signing evidence"));
        assertTrue(normalized.contains("no service-level agreement or service-level objective evidence"));
        assertTrue(normalized.contains("no registry publication or container signing evidence"));
        assertTrue(normalized.contains("runtimeReportWritten: false".toLowerCase(Locale.ROOT)));
        assertTrue(normalized.contains("externalServices: false".toLowerCase(Locale.ROOT)));
    }

    @Test
    void routingDemoScenarioComparisonContainsLocalOnlyBoundariesAndCopyableDeltas() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("Packaged normal-load baseline"));
        assertTrue(page.contains("Current request editor payload"));
        assertTrue(page.contains("previousScenario: "));
        assertTrue(page.contains("currentScenario: "));
        assertTrue(page.contains("selectedStrategyChange: "));
        assertTrue(page.contains("selectedBackendChange: "));
        assertTrue(page.contains("keyInputSignalChanges: "));
        assertTrue(page.contains("degradationRecoveryExplanation: "));
        assertTrue(page.contains("localOnlyDemoBoundary: browser-local comparison"));
        assertTrue(page.contains("No primary strategy change"));
        assertTrue(page.contains("Selected backend changed after compare"));
        assertTrue(page.contains("candidates "));
        assertTrue(page.contains("healthy "));
        assertTrue(page.contains("unhealthy "));
        assertTrue(page.contains("inFlight "));
        assertTrue(page.contains("maxP95LatencyMs "));
        assertTrue(page.contains("Degradation pressure increased"));
        assertTrue(page.contains("Recovery visible"));
        assertTrue(page.contains("same-origin local API results only"));
        assertTrue(normalized.contains("browser-local comparison of synthetic payloads"));
        assertTrue(normalized.contains("no production traffic"));
        assertTrue(normalized.contains("no live cloud validation"));
        assertTrue(normalized.contains("no real tenant data"));
        assertTrue(normalized.contains("no upload, share endpoint, or server-side export/pdf/zip generation"));
    }

    @Test
    void routingDemoEvidenceNavigationIsLocalStaticAndBounded() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"routing-evidence-navigation-panel\""));
        assertTrue(page.contains("Where to go next after routing proof review across local static evidence pages."));
        assertTrue(page.contains("Use Routing Proof Summary and Scenario Comparison"));
        assertTrue(page.contains("Review Enterprise Lab posture"));
        assertTrue(page.contains("Check operator evidence locations"));
        assertTrue(page.contains("Compare local and CI evidence stages"));
        assertTrue(page.contains("Use the browser-local packet page"));
        assertTrue(page.contains("Reviewer path after routing proof review"));
        assertTrue(page.contains("inspect the Enterprise Lab reviewer dashboard"));
        assertTrue(page.contains("check the Operator evidence dashboard"));
        assertTrue(page.contains("review the Evidence timeline"));
        assertTrue(page.contains("Evidence export packet page"));
        assertTrue(normalized.contains("local/demo evidence only"));
        assertTrue(normalized.contains("no production traffic proof"));
        assertTrue(normalized.contains("no live cloud proof"));
        assertTrue(normalized.contains("no real tenant proof"));
        assertTrue(normalized.contains("registry publication proof"));
        assertTrue(normalized.contains("container signing proof"));
        assertFalse(page.contains("href=\"http"));
        assertFalse(page.contains("href=\"//"));
        assertFalse(normalized.contains("navigator.sendbeacon"));
        assertFalse(normalized.contains("fetch(\"https://"));
        assertFalse(normalized.contains("fetch('https://"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("server-side packet"));
    }

    @Test
    void routingDemoReviewerWorkflowChecklistAndWalkthroughAreLocalAndBounded() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"reviewer-workflow-checklist-panel\""));
        assertTrue(page.contains("aria-label=\"Reviewer workflow checklist cards\""));
        assertTrue(page.contains("data-copy-target=\"reviewer-walkthrough-output\""));
        assertTrue(page.contains("# End-to-End Routing Reviewer Walkthrough"));
        assertTrue(page.contains("workflowChecklist:"));
        assertTrue(page.contains("1. load sample scenario"));
        assertTrue(page.contains("2. run routing comparison"));
        assertTrue(page.contains("3. inspect Routing Proof Summary"));
        assertTrue(page.contains("4. compare scenario deltas"));
        assertTrue(page.contains("5. follow Evidence Navigation links"));
        assertTrue(page.contains("6. copy reviewer proof note"));
        assertTrue(page.contains("7. export/print packet from /evidence-export-packet.html"));
        assertTrue(page.contains("routingProofSummary:"));
        assertTrue(page.contains("scenarioComparison:"));
        assertTrue(page.contains("evidencePath: /routing-demo.html -> /enterprise-lab-reviewer.html -> /operator-evidence-dashboard.html -> /evidence-timeline.html -> /evidence-export-packet.html"));
        assertTrue(page.contains("copyBoundary: browser-local copy action only; no upload/share endpoint; no server-side export/PDF/ZIP generation"));
        assertTrue(page.contains("notProven:"));
        assertTrue(normalized.contains("browser-local copy actions read visible page text only"));
        assertTrue(normalized.contains("no upload/share endpoint"));
        assertTrue(normalized.contains("no server-side export/pdf/zip generation"));
        assertTrue(normalized.contains("no external services"));
        assertTrue(normalized.contains("no production traffic proof"));
        assertTrue(normalized.contains("no live cloud proof"));
        assertTrue(normalized.contains("no real tenant proof"));
        assertTrue(normalized.contains("no registry publication proof"));
        assertTrue(normalized.contains("no container signing proof"));
        assertTrue(normalized.contains("no service-level agreement or service-level objective evidence"));
    }

    @Test
    void routingDemoReviewerConfidenceSignalsAreStaticLocalAndNotOverclaimed() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"reviewer-confidence-signals-panel\""));
        assertTrue(page.contains("aria-label=\"Reviewer confidence signal cards\""));
        assertTrue(page.contains("Local repeatability"));
        assertTrue(page.contains("Deterministic sample scenarios"));
        assertTrue(page.contains("Same-origin API usage"));
        assertTrue(page.contains("Static browser-only notes/copy actions"));
        assertTrue(page.contains("Not-production-certified boundary"));
        assertTrue(normalized.contains("packaged synthetic inputs"));
        assertTrue(normalized.contains("packaged server names, weights, health states, load, and latency fields"));
        assertTrue(normalized.contains("browser calls target local app paths"));
        assertTrue(normalized.contains("do not create server-side files"));
        assertTrue(normalized.contains("local/demo evidence only"));
        assertTrue(normalized.contains("no production traffic proof"));
        assertTrue(normalized.contains("no live cloud proof"));
        assertTrue(normalized.contains("no real tenant proof"));
        assertTrue(normalized.contains("no registry publication proof"));
        assertTrue(normalized.contains("no container signing proof"));
        assertFalse(normalized.contains("enterprise production ready"));
        assertFalse(normalized.contains("registry published"));
        assertFalse(normalized.contains("signed container"));
        assertFalse(normalized.contains("governance-applied"));
    }

    @Test
    void routingDemoPageContainsSafetyLimitationsAndPostmanParity() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("Postman parity: run the <strong>Routing Decision Demo</strong> folder"));
        assertTrue(normalized.contains("local/operator demo only"));
        assertTrue(normalized.contains("not certification"));
        assertTrue(normalized.contains("not benchmark proof"));
        assertTrue(normalized.contains("not legal compliance proof"));
        assertTrue(normalized.contains("not identity proof"));
        assertTrue(normalized.contains("no cloud mutation"));
        assertTrue(normalized.contains("no cloudmanager required for routing demo"));
        assertTrue(normalized.contains("no external services/dependencies"));
        assertTrue(normalized.contains("no external scripts/cdns"));
        assertTrue(normalized.contains("api server required for browser/postman demo"));
        assertTrue(normalized.contains("no runtime reports or demo transcripts are written"));
    }

    @Test
    void routingDemoPageHasNoExternalScriptsStorageSecretsOrMutableControls() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertFalse(normalized.contains("<script src="));
        assertFalse(normalized.contains("<link rel=\"stylesheet\""));
        assertFalse(normalized.contains("<img"));
        assertFalse(normalized.contains("background-image"));
        assertFalse(normalized.contains("cdn."));
        assertFalse(normalized.contains("fonts.googleapis"));
        assertFalse(normalized.contains("fonts.gstatic"));
        assertFalse(normalized.contains("localstorage"));
        assertFalse(normalized.contains("sessionstorage"));
        assertFalse(normalized.contains("date.now"));
        assertFalse(normalized.contains("new date"));
        assertFalse(normalized.contains("math.random"));
        assertFalse(normalized.contains("randomuuid"));
        assertFalse(normalized.contains("crypto.randomuuid"));
        assertFalse(normalized.contains("x-api-key"));
        assertFalse(normalized.contains("bearer"));
        assertFalse(normalized.contains("authorization"));
        assertFalse(normalized.contains("type=\"password\""));
        assertFalse(normalized.contains("password"));
        assertFalse(normalized.contains("access key"));
        assertFalse(normalized.contains("secret key"));
        assertFalse(normalized.contains("data-action=\"admin"));
        assertFalse(normalized.contains("data-action=\"release"));
        assertFalse(normalized.contains("data-action=\"ruleset"));
        assertFalse(normalized.contains("data-action=\"cloud"));
        assertFalse(normalized.contains("/rulesets"));
        assertFalse(normalized.contains("/repos/"));
        assertFalse(normalized.contains("create release"));
        assertFalse(normalized.contains("create tag"));
        assertFalse(normalized.contains("delete-branch"));
        assertFalse(normalized.contains("new cloudmanager"));
        assertFalse(normalized.contains("construct cloudmanager"));
        assertFalse(normalized.contains("certified operator"));
        assertFalse(normalized.contains("production benchmark"));
        assertFalse(normalized.contains("legal training compliance"));
        assertFalse(normalized.contains("identity verified"));
        assertTrue(countOccurrences(normalized, "cloudmanager") <= 2,
                "CloudManager may appear only in safety limitation text");
    }

    @Test
    void routingDemoFixturesAreValidJsonAndContainSafeSyntheticInputs() throws Exception {
        for (Path fixture : ROUTING_FIXTURES) {
            JsonNode body = readJson(fixture);
            String normalized = Files.readString(fixture, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);

            assertTrue(body.path("strategies").isArray(), fixture + " should list strategies");
            assertTrue(body.path("servers").isArray(), fixture + " should list servers");
            assertTrue(body.path("servers").size() >= 1, fixture + " should include a sample server");
            assertFalse(normalized.contains("http://"));
            assertFalse(normalized.contains("https://"));
            assertFalse(normalized.contains("arn:"));
            assertFalse(normalized.contains("prod-"));
            assertFalse(normalized.contains("password"));
            assertFalse(normalized.contains("secret"));
        }

        JsonNode compare = readJson(COMPARE_FIXTURE);
        assertEquals(5, compare.path("strategies").size());
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", compare.at("/strategies/0").asText());
        assertEquals("ROUND_ROBIN", compare.at("/strategies/4").asText());
        assertEquals("edge-alpha", compare.at("/servers/0/serverId").asText());
        assertEquals("edge-drain", compare.at("/servers/2/serverId").asText());
        assertFalse(compare.at("/servers/2/healthy").asBoolean());
    }

    @Test
    void packagedLeastConnectionsFixtureProducesDeterministicRoutingResultWithoutCloudManager() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(post("/api/routing/compare")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Files.readString(LEAST_CONNECTIONS_FIXTURE, StandardCharsets.UTF_8)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestedStrategies[0]", is("WEIGHTED_LEAST_CONNECTIONS")))
                    .andExpect(jsonPath("$.candidateCount", is(2)))
                    .andExpect(jsonPath("$.results[0].strategyId", is("WEIGHTED_LEAST_CONNECTIONS")))
                    .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                    .andExpect(jsonPath("$.results[0].chosenServerId", is("edge-weighted")))
                    .andExpect(jsonPath("$.results[0].reason", containsString("weighted least-connections")))
                    .andExpect(jsonPath("$.results[0].scores.edge-standard", is(5.0)))
                    .andExpect(jsonPath("$.results[0].scores.edge-weighted", is(3.0)));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "routing demo comparison must not construct CloudManager");
        }
    }

    @Test
    void unsupportedStrategyAndMalformedRoutingRequestsReturnControlledErrors() throws Exception {
        mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategies": ["NOT_A_STRATEGY"],
                                  "servers": [
                                    {
                                      "serverId": "edge-alpha",
                                      "healthy": true,
                                      "inFlightRequestCount": 1,
                                      "averageLatencyMillis": 10.0,
                                      "p95LatencyMillis": 20.0,
                                      "p99LatencyMillis": 30.0,
                                      "recentErrorRate": 0.0
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("Unsupported routing strategy")))
                .andExpect(jsonPath("$.path", is("/api/routing/compare")));

        mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategies": ["WEIGHTED_LEAST_LOAD"],
                                  "servers": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.path", is("/api/routing/compare")));
    }

    @Test
    void routingDecisionDemoPostmanFolderIsValidAndReadOnly() throws Exception {
        JsonNode collection = readJson(Path.of("postman/LoadBalancerPro.postman_collection.json"));
        JsonNode folder = findFolder(collection, "Routing Decision Demo");
        assertNotNull(folder, "Postman collection should include a Routing Decision Demo folder");
        assertEquals(6, folder.path("item").size());

        List<String> expectedNames = List.of(
                "GET Routing Demo Health Check",
                "GET Routing Demo Readiness Check",
                "POST Compare All Supported Strategies",
                "POST Weighted Strategy Sample",
                "POST Least Connections Sample",
                "POST Tail Latency Sample");
        for (int i = 0; i < expectedNames.size(); i++) {
            assertEquals(expectedNames.get(i), folder.at("/item/" + i + "/name").asText());
        }

        assertEquals("{{baseUrl}}/api/routing/compare", folder.at("/item/2/request/url/raw").asText());
        assertTrue(folder.at("/item/2/request/body/raw").asText().contains("TAIL_LATENCY_POWER_OF_TWO"));
        assertTrue(folder.at("/item/2/request/body/raw").asText().contains("WEIGHTED_ROUND_ROBIN"));
        assertTrue(folder.at("/item/2/request/body/raw").asText().contains("ROUND_ROBIN"));

        String normalized = Files.readString(Path.of("postman/LoadBalancerPro.postman_collection.json"),
                StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertFalse(normalized.contains("x-api-key"));
        assertFalse(normalized.contains("bearer"));
        assertFalse(normalized.contains("authorization"));
        assertFalse(normalized.contains("/rulesets"));
        assertFalse(normalized.contains("create release"));
        assertFalse(normalized.contains("create tag"));
        assertFalse(normalized.contains("delete-branch"));
    }

    private static JsonNode readJson(Path path) throws Exception {
        return OBJECT_MAPPER.readTree(Files.readString(path, StandardCharsets.UTF_8));
    }

    private static JsonNode findFolder(JsonNode node, String folderName) {
        if (folderName.equals(node.path("name").asText()) && node.path("item").isArray()) {
            return node;
        }
        for (JsonNode item : node.path("item")) {
            JsonNode found = findFolder(item, folderName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
