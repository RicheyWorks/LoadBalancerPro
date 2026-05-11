package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.richmond423.loadbalancerpro.core.CloudManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LoadBalancingCockpitDemoTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path COCKPIT_PAGE =
            Path.of("src/main/resources/static/load-balancing-cockpit.html");
    private static final Path ALLOCATION_FIXTURE =
            Path.of("src/test/resources/load-balancing-cockpit/allocation-scenario.json");
    private static final Path OVERLOAD_FIXTURE =
            Path.of("src/test/resources/load-balancing-cockpit/overload-scenario.json");
    private static final Path ROUTING_FIXTURE =
            Path.of("src/test/resources/load-balancing-cockpit/unified-routing-scenario.json");
    private static final Path REMEDIATION_FIXTURE =
            Path.of("src/test/resources/load-balancing-cockpit/remediation-hints-scenario.json");
    private static final Path SUMMARY_FIXTURE =
            Path.of("src/test/resources/load-balancing-cockpit/cockpit-summary-scenario.json");
    private static final List<Path> COCKPIT_FIXTURES = List.of(
            ALLOCATION_FIXTURE, OVERLOAD_FIXTURE, ROUTING_FIXTURE, REMEDIATION_FIXTURE, SUMMARY_FIXTURE);
    private static final Path SCENARIO_GALLERY_DIR =
            Path.of("src/test/resources/load-balancing-cockpit/scenarios");
    private static final List<Path> SCENARIO_GALLERY_FIXTURES = List.of(
            SCENARIO_GALLERY_DIR.resolve("normal-load-scenario.json"),
            SCENARIO_GALLERY_DIR.resolve("overload-pressure-scenario.json"),
            SCENARIO_GALLERY_DIR.resolve("all-unhealthy-degradation-scenario.json"),
            SCENARIO_GALLERY_DIR.resolve("recovery-capacity-restored-scenario.json"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loadBalancingCockpitPageExistsAndIsServed() throws Exception {
        assertTrue(Files.exists(COCKPIT_PAGE), "load-balancing cockpit page should be source-controlled");

        mockMvc.perform(get("/load-balancing-cockpit.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Load-Balancing Cockpit")))
                .andExpect(content().string(containsString("Cockpit Visual Summary")))
                .andExpect(content().string(containsString("Scenario Gallery")))
                .andExpect(content().string(containsString("data-action=\"full-sequence\"")))
                .andExpect(content().string(containsString("/api/allocate/capacity-aware")))
                .andExpect(content().string(containsString("/api/routing/compare")));
    }

    @Test
    void cockpitPageContainsExpectedSectionsControlsAndEndpointPaths() throws Exception {
        String page = Files.readString(COCKPIT_PAGE, StandardCharsets.UTF_8);

        assertTrue(page.contains("/api/health"));
        assertTrue(page.contains("/actuator/health/readiness"));
        assertTrue(page.contains("/api/allocate/capacity-aware"));
        assertTrue(page.contains("/api/allocate/evaluate"));
        assertTrue(page.contains("/api/routing/compare"));
        assertTrue(page.contains("Scenario Gallery"));
        assertTrue(page.contains("Normal Load"));
        assertTrue(page.contains("Overload Pressure"));
        assertTrue(page.contains("All-Unhealthy Degradation"));
        assertTrue(page.contains("Recovery / Capacity Restored"));
        assertTrue(page.contains("Load scenario"));
        assertTrue(page.contains("Run selected scenario"));
        assertTrue(page.contains("Compare with previous scenario"));
        assertTrue(page.contains("Copy selected payload"));
        assertTrue(page.contains("Copy selected curl"));
        assertTrue(page.contains("Copy scenario summary"));
        assertTrue(page.contains("Cockpit Navigation &amp; Readiness"));
        assertTrue(page.contains("section-index"));
        assertTrue(page.contains("data-nav-target=\"visual-summary-panel\""));
        assertTrue(page.contains("Current-panel mini-map / orientation"));
        assertTrue(page.contains("readiness-badges"));
        assertTrue(page.contains("Refresh readiness"));
        assertTrue(page.contains("Copy readiness summary"));
        assertTrue(page.contains("data-nav-target=\"scenario-gallery-panel\""));
        assertTrue(page.contains("data-nav-target=\"explanation-drilldown-panel\""));
        assertTrue(page.contains("data-nav-target=\"comparison-matrix-panel\""));
        assertTrue(page.contains("data-nav-target=\"replay-mode-panel\""));
        assertTrue(page.contains("data-nav-target=\"review-packet-panel\""));
        assertTrue(page.contains("data-nav-target=\"api-contract-trace-panel\""));
        assertTrue(page.contains("data-nav-target=\"guided-walkthrough-panel\""));
        assertTrue(page.contains("data-nav-target=\"allocation-results-panel\""));
        assertTrue(page.contains("Scenario selected"));
        assertTrue(page.contains("Scenario run"));
        assertTrue(page.contains("Raw JSON available"));
        assertTrue(page.contains("Explanation generated"));
        assertTrue(page.contains("Matrix generated"));
        assertTrue(page.contains("Replay generated"));
        assertTrue(page.contains("Trace generated"));
        assertTrue(page.contains("Review packet generated"));
        assertTrue(page.contains("Walkthrough started"));
        assertTrue(page.contains("Walkthrough completed or partially completed"));
        assertTrue(page.contains("navigation-highlight"));
        assertTrue(page.contains("Cockpit Navigation Readiness Summary"));
        assertTrue(page.contains("Operator Guided Walkthrough"));
        assertTrue(page.contains("Start walkthrough"));
        assertTrue(page.contains("Previous step"));
        assertTrue(page.contains("Next step"));
        assertTrue(page.contains("Mark step complete"));
        assertTrue(page.contains("Clear walkthrough"));
        assertTrue(page.contains("Copy walkthrough summary"));
        assertTrue(page.contains("walkthrough-current-step"));
        assertTrue(page.contains("Checklist/progress display"));
        assertTrue(page.contains("Jump to panel"));
        assertTrue(page.contains("Expected evidence"));
        assertTrue(page.contains("not generated yet"));
        assertTrue(page.contains("Select packaged scenario"));
        assertTrue(page.contains("Run scenario / endpoint evaluation"));
        assertTrue(page.contains("Review raw JSON output"));
        assertTrue(page.contains("Review explanation drill-down"));
        assertTrue(page.contains("Generate comparison matrix"));
        assertTrue(page.contains("Replay selected pair"));
        assertTrue(page.contains("Generate API Contract Trace"));
        assertTrue(page.contains("Verify raw-vs-derived fields"));
        assertTrue(page.contains("Generate Operator Review Packet"));
        assertTrue(page.contains("Copy or print final handoff"));
        assertTrue(page.contains("walkthrough-highlight"));
        assertTrue(page.contains("Operator Guided Walkthrough Summary"));
        assertTrue(page.contains("Operator Comparison Matrix"));
        assertTrue(page.contains("Run all scenarios"));
        assertTrue(page.contains("Clear matrix"));
        assertTrue(page.contains("Copy matrix summary"));
        assertTrue(page.contains("Copy matrix curls"));
        assertTrue(page.contains("Copy matrix payloads"));
        assertTrue(page.contains("Expected pressure / incident type"));
        assertTrue(page.contains("Routing strategy summary"));
        assertTrue(page.contains("Selected server / outcome label"));
        assertTrue(page.contains("Allocation pressure summary"));
        assertTrue(page.contains("Load-shedding / overload signal"));
        assertTrue(page.contains("Remediation hint summary"));
        assertTrue(page.contains("Explanation / rationale summary"));
        assertTrue(page.contains("Delta vs prior scenario"));
        assertTrue(page.contains("Raw status / error state"));
        assertTrue(page.contains("Operator Replay Mode"));
        assertTrue(page.contains("Baseline scenario selector"));
        assertTrue(page.contains("Comparison scenario selector"));
        assertTrue(page.contains("Replay selected pair"));
        assertTrue(page.contains("Clear replay"));
        assertTrue(page.contains("Copy reviewer note"));
        assertTrue(page.contains("Copy replay curls"));
        assertTrue(page.contains("Copy replay payloads"));
        assertTrue(page.contains("Baseline scenario"));
        assertTrue(page.contains("Comparison scenario"));
        assertTrue(page.contains("Changed-fields summary"));
        assertTrue(page.contains("Routing diff - strategy / selected route summary"));
        assertTrue(page.contains("Selected server / outcome label diff"));
        assertTrue(page.contains("Allocation pressure diff"));
        assertTrue(page.contains("Load-shedding / overload diff"));
        assertTrue(page.contains("Remediation hint diff"));
        assertTrue(page.contains("Explanation / rationale diff"));
        assertTrue(page.contains("Scenario delta diff"));
        assertTrue(page.contains("Error state diff"));
        assertTrue(page.contains("Replay error panel"));
        assertTrue(page.contains("Operator Review Packet"));
        assertTrue(page.contains("Generate review packet"));
        assertTrue(page.contains("Clear review packet"));
        assertTrue(page.contains("Copy review packet"));
        assertTrue(page.contains("Print review packet"));
        assertTrue(page.contains("review-packet-preview"));
        assertTrue(page.contains("Review Packet Summary"));
        assertTrue(page.contains("Selected Scenario"));
        assertTrue(page.contains("Comparison Matrix Summary"));
        assertTrue(page.contains("Replay Delta Summary"));
        assertTrue(page.contains("Explanation / Rationale Summary"));
        assertTrue(page.contains("Endpoint References"));
        assertTrue(page.contains("Payload / Curl References"));
        assertTrue(page.contains("Raw JSON Reference Notes"));
        assertTrue(page.contains("Safety Notes"));
        assertTrue(page.contains("Limitations / Unavailable Fields"));
        assertTrue(page.contains("API Contract Trace"));
        assertTrue(page.contains("Generate trace"));
        assertTrue(page.contains("Clear trace"));
        assertTrue(page.contains("Copy trace summary"));
        assertTrue(page.contains("api-contract-trace-preview"));
        assertTrue(page.contains("API Contract Trace Summary"));
        assertTrue(page.contains("Panel name"));
        assertTrue(page.contains("Endpoint path"));
        assertTrue(page.contains("Request payload source"));
        assertTrue(page.contains("Raw response source"));
        assertTrue(page.contains("Displayed raw fields"));
        assertTrue(page.contains("Derived fields / labels"));
        assertTrue(page.contains("Unavailable / missing fields"));
        assertTrue(page.contains("Mutation behavior / safety notes"));
        assertTrue(page.contains("panelName: \"Cockpit Navigation & Readiness\""));
        assertTrue(page.contains("panelName: \"Scenario Gallery\""));
        assertTrue(page.contains("panelName: \"Operator Guided Walkthrough\""));
        assertTrue(page.contains("panelName: \"Explanation Drill-Down\""));
        assertTrue(page.contains("panelName: \"Operator Comparison Matrix\""));
        assertTrue(page.contains("panelName: \"Operator Replay Mode\""));
        assertTrue(page.contains("panelName: \"Operator Review Packet\""));
        assertTrue(page.contains("Expected outcome hints"));
        assertTrue(page.contains("What-changed summary"));
        assertTrue(page.contains("Allocation results"));
        assertTrue(page.contains("Routing decisions"));
        assertTrue(page.contains("Load-shedding / overload signals"));
        assertTrue(page.contains("Remediation-plan hints"));
        assertTrue(page.contains("Side-by-side summary"));
        assertTrue(page.contains("Copy curl"));
        assertTrue(page.contains("Copy payload"));
        assertTrue(page.contains("Copy summary"));
        assertTrue(page.contains("Run full cockpit sequence"));
        assertTrue(page.contains("Run allocation preview"));
        assertTrue(page.contains("Run routing comparison"));
        assertTrue(page.contains("Run load-shedding preview"));
        assertTrue(page.contains("Run remediation hints"));
        assertTrue(page.contains("Not available in current API"));
        assertTrue(page.contains("Explanation Drill-Down"));
        assertTrue(page.contains("Routing Strategy Explanation"));
        assertTrue(page.contains("Allocation Math / Capacity Explanation"));
        assertTrue(page.contains("Load-Shedding / Overload Reason Breakdown"));
        assertTrue(page.contains("Remediation Rationale"));
        assertTrue(page.contains("Scenario Delta Explanation"));
        assertTrue(page.contains("Copy drill-down summary"));
        assertTrue(page.contains("Copy explanation curl"));
        assertTrue(page.contains("Copy operator rationale"));
        assertTrue(page.contains("Exact internal score not exposed by the current API"));
        assertTrue(page.contains("derived from visible request/response fields"));
        assertTrue(page.contains("No prior scenario comparison available"));
    }

    @Test
    void cockpitPageContainsVisualSummaryBusyStateAndAccessibilityMarkers() throws Exception {
        String page = Files.readString(COCKPIT_PAGE, StandardCharsets.UTF_8);

        assertTrue(page.contains("id=\"visual-summary-panel\""));
        assertTrue(page.contains("Cockpit Visual Summary"));
        assertTrue(page.contains("id=\"visual-status-summary\""));
        assertTrue(page.contains("id=\"visual-scenario-summary\""));
        assertTrue(page.contains("id=\"visual-strategy-highlight\""));
        assertTrue(page.contains("id=\"visual-health-badges\""));
        assertTrue(page.contains("id=\"visual-delta-bars\""));
        assertTrue(page.contains("Scenario delta mini-bars"));
        assertTrue(page.contains("mini-bar"));
        assertTrue(page.contains("selected-strategy"));
        assertTrue(page.contains("Health/metrics badges"));
        assertTrue(page.contains("Run a scenario to populate compact visual summaries."));
        assertTrue(page.contains("id=\"action-status\""));
        assertTrue(page.contains("role=\"status\""));
        assertTrue(page.contains("aria-live=\"polite\""));
        assertTrue(page.contains("aria-busy"));
        assertTrue(page.contains("setActionButtonBusy"));
        assertTrue(page.contains("button[aria-busy=\"true\"]"));
        assertTrue(page.contains("button:focus-visible"));
        assertTrue(page.contains("refreshVisualSummary"));
    }

    @Test
    void cockpitPageContainsSafetyLimitationsAndPostmanParity() throws Exception {
        String page = Files.readString(COCKPIT_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("Unified Load-Balancing Cockpit"));
        assertTrue(normalized.contains("local/operator demo only"));
        assertTrue(normalized.contains("not certification"));
        assertTrue(normalized.contains("not benchmark proof"));
        assertTrue(normalized.contains("not legal compliance proof"));
        assertTrue(normalized.contains("not identity proof"));
        assertTrue(normalized.contains("no cloud mutation"));
        assertTrue(normalized.contains("no cloudmanager required for cockpit/gallery/replay/drill-down/walkthrough demo"));
        assertTrue(normalized.contains("no external services/dependencies"));
        assertTrue(normalized.contains("no external scripts/cdns"));
        assertTrue(normalized.contains("api server required for browser/postman demo"));
        assertTrue(normalized.contains("no server-side report writing"));
        assertTrue(normalized.contains("does not fabricate allocation, routing, load-shedding, or remediation behavior"));
    }

    @Test
    void cockpitPageHasNoExternalScriptsStorageSecretsOrMutableControls() throws Exception {
        String page = Files.readString(COCKPIT_PAGE, StandardCharsets.UTF_8);
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
        assertFalse(normalized.contains("gh release"));
        assertFalse(normalized.contains("git tag"));
        assertFalse(normalized.contains("create release"));
        assertFalse(normalized.contains("create tag"));
        assertFalse(normalized.contains("release asset"));
        assertFalse(normalized.contains("delete-branch"));
        assertFalse(normalized.contains("new cloudmanager"));
        assertFalse(normalized.contains("construct cloudmanager"));
        assertFalse(normalized.contains("production-grade"));
        assertFalse(normalized.contains("production gateway"));
        assertFalse(normalized.contains("certified operator"));
        assertFalse(normalized.contains("production benchmark"));
        assertFalse(normalized.contains("matrix score"));
        assertFalse(normalized.contains("benchmark score"));
        assertFalse(normalized.contains("certification score"));
        assertFalse(normalized.contains("performance score"));
        assertFalse(normalized.contains("replay score"));
        assertFalse(normalized.contains("review score"));
        assertFalse(normalized.contains("trace score"));
        assertFalse(normalized.contains("saved to server"));
        assertFalse(normalized.contains("server-side packet"));
        assertFalse(normalized.contains("persist packet"));
        assertFalse(normalized.contains("persist trace"));
        assertFalse(normalized.contains("persist walkthrough"));
        assertFalse(normalized.contains("persist readiness"));
        assertFalse(normalized.contains("persist navigation"));
        assertFalse(normalized.contains("walkthrough score"));
        assertFalse(normalized.contains("readiness score"));
        assertFalse(normalized.contains("navigation score"));
        assertFalse(normalized.contains("legal training compliance"));
        assertFalse(normalized.contains("identity verified"));
        assertFalse(normalized.contains("sha256"));
        assertFalse(normalized.contains("fake hash"));
        assertFalse(normalized.contains("placeholder hash"));
        assertFalse(normalized.contains("fake evidence"));
        assertTrue(countOccurrences(normalized, "cloudmanager") <= 2,
                "CloudManager may appear only in safety limitation text");
    }

    @Test
    void cockpitFixturesAreValidJsonAndContainSafeSyntheticInputs() throws Exception {
        for (Path fixture : COCKPIT_FIXTURES) {
            JsonNode body = readJson(fixture);
            String normalized = Files.readString(fixture, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);

            assertFalse(body.isMissingNode(), fixture + " should be valid JSON");
            assertFalse(normalized.contains("http://"));
            assertFalse(normalized.contains("https://"));
            assertFalse(normalized.contains("arn:"));
            assertFalse(normalized.contains("prod-"));
            assertFalse(normalized.contains("password"));
            assertFalse(normalized.contains("secret"));
            assertFalse(normalized.contains("access key"));
            assertFalse(normalized.contains("cloudmanager"));
        }

        JsonNode summary = readJson(SUMMARY_FIXTURE);
        assertEquals("safe-local-load-balancing-cockpit", summary.path("scenarioName").asText());
        assertEquals("edge-alpha", summary.at("/allocationRequest/servers/0/id").asText());
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", summary.at("/routingRequest/strategies/0").asText());
        assertEquals("BACKGROUND", summary.at("/evaluationRequest/priority").asText());
    }

    @Test
    void scenarioGalleryFixturesAreValidSafeAndMapToExistingEndpointShapes() throws Exception {
        List<String> expectedScenarioNames = List.of(
                "normal-load-scenario",
                "overload-pressure-scenario",
                "all-unhealthy-degradation-scenario",
                "recovery-capacity-restored-scenario");
        List<String> actualScenarioNames = new ArrayList<>();

        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            for (Path fixture : SCENARIO_GALLERY_FIXTURES) {
                assertTrue(Files.exists(fixture), fixture + " should exist");
                JsonNode scenario = readJson(fixture);
                String normalized = Files.readString(fixture, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                actualScenarioNames.add(scenario.path("scenarioName").asText());

                assertFalse(scenario.path("scenarioName").asText().isBlank());
                assertTrue(scenario.path("expectedHints").has("routing"));
                assertTrue(scenario.path("expectedHints").has("allocation"));
                assertTrue(scenario.path("expectedHints").has("loadShedding"));
                assertTrue(scenario.path("expectedHints").has("remediation"));
                assertTrue(scenario.path("allocationRequest").path("servers").isArray());
                assertTrue(scenario.path("evaluationRequest").path("servers").isArray());
                assertTrue(scenario.path("routingRequest").path("servers").isArray());

                assertFalse(normalized.contains("http://"));
                assertFalse(normalized.contains("https://"));
                assertFalse(normalized.contains("arn:"));
                assertFalse(normalized.contains("cloudmanager"));
                assertFalse(normalized.contains("cloud id"));
                assertFalse(normalized.contains("prod-"));
                assertFalse(normalized.contains("production"));
                assertFalse(normalized.contains("password"));
                assertFalse(normalized.contains("secret"));
                assertFalse(normalized.contains("access key"));

                postJsonNode("/api/allocate/capacity-aware", scenario.path("allocationRequest"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)));
                postJsonNode("/api/allocate/evaluate", scenario.path("evaluationRequest"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.readOnly", is(true)))
                        .andExpect(jsonPath("$.metricsPreview.emitted", is(false)))
                        .andExpect(jsonPath("$.remediationPlan.cloudMutation", is(false)));
                postJsonNode("/api/routing/compare", statelessRoutingRequest(scenario.path("routingRequest")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.requestedStrategies[0]", is("WEIGHTED_LEAST_CONNECTIONS")))
                        .andExpect(jsonPath("$.results[0].strategyId", is("WEIGHTED_LEAST_CONNECTIONS")));
            }
            assertEquals(expectedScenarioNames, actualScenarioNames);
            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "scenario gallery endpoints must not construct CloudManager");
        }
    }

    @Test
    void cockpitExistingEndpointsReturnSafeOutputsWithoutCloudManager() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            String firstAllocation = postJson("/api/allocate/capacity-aware", ALLOCATION_FIXTURE)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allocations.edge-alpha").exists())
                    .andExpect(jsonPath("$.allocations.edge-beta").exists())
                    .andExpect(jsonPath("$.allocations.edge-drain").doesNotExist())
                    .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            String secondAllocation = postJson("/api/allocate/capacity-aware", ALLOCATION_FIXTURE)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            assertEquals(firstAllocation, secondAllocation, "Allocation fixture output should be deterministic.");

            String firstEvaluation = postJson("/api/allocate/evaluate", OVERLOAD_FIXTURE)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.readOnly", is(true)))
                    .andExpect(jsonPath("$.rejectedLoad", greaterThan(0.0)))
                    .andExpect(jsonPath("$.unallocatedLoad", greaterThan(0.0)))
                    .andExpect(jsonPath("$.loadShedding.action", is("SHED")))
                    .andExpect(jsonPath("$.metricsPreview.emitted", is(false)))
                    .andExpect(jsonPath("$.remediationPlan.advisoryOnly", is(true)))
                    .andExpect(jsonPath("$.remediationPlan.cloudMutation", is(false)))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[0].executable", is(false)))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            String secondEvaluation = postJson("/api/allocate/evaluate", OVERLOAD_FIXTURE)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            assertEquals(firstEvaluation, secondEvaluation,
                    "Read-only evaluation fixture output should be deterministic.");

            mockMvc.perform(post("/api/routing/compare")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(statelessRoutingRequest()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestedStrategies[0]", is("WEIGHTED_LEAST_CONNECTIONS")))
                    .andExpect(jsonPath("$.candidateCount", is(2)))
                    .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                    .andExpect(jsonPath("$.results[0].strategyId", is("WEIGHTED_LEAST_CONNECTIONS")))
                    .andExpect(jsonPath("$.results[0].chosenServerId", is("edge-weighted")));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "cockpit demo endpoints must not construct CloudManager");
        }
    }

    @Test
    void cockpitMalformedEvaluationRequestReturnsControlledError() throws Exception {
        mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 10.0,
                                  "servers": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.path", is("/api/allocate/evaluate")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void cockpitRemediationFixtureReturnsExpectedOverloadHints() throws Exception {
        postJson("/api/allocate/evaluate", REMEDIATION_FIXTURE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remediationPlan.status", is("OVERLOADED")))
                .andExpect(jsonPath("$.remediationPlan.generatedFrom", is("allocation-evaluation")))
                .andExpect(jsonPath("$.remediationPlan.cloudMutation", is(false)))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].action", is("SCALE_UP")))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].loadAmount", closeTo(123.333, 0.01)))
                .andExpect(jsonPath("$.remediationPlan.recommendations[1].action", is("SHED_LOAD")));
    }

    @Test
    void cockpitPostmanFolderIsValidAndReadOnly() throws Exception {
        JsonNode collection = readJson(Path.of("postman/LoadBalancerPro.postman_collection.json"));
        JsonNode folder = findFolder(collection, "Unified Load-Balancing Cockpit");
        assertNotNull(folder, "Postman collection should include a Unified Load-Balancing Cockpit folder");
        assertEquals(6, folder.path("item").size());

        List<String> expectedNames = List.of(
                "GET Unified Cockpit Health Check",
                "GET Unified Cockpit Readiness Check",
                "POST Cockpit Routing Comparison",
                "POST Cockpit Capacity-Aware Allocation",
                "POST Cockpit Predictive Allocation",
                "POST Cockpit Load-Shedding Evaluation");
        for (int i = 0; i < expectedNames.size(); i++) {
            assertEquals(expectedNames.get(i), folder.at("/item/" + i + "/name").asText());
        }

        assertEquals("{{baseUrl}}/api/routing/compare", folder.at("/item/2/request/url/raw").asText());
        assertEquals("{{baseUrl}}/api/allocate/capacity-aware", folder.at("/item/3/request/url/raw").asText());
        assertEquals("{{baseUrl}}/api/allocate/predictive", folder.at("/item/4/request/url/raw").asText());
        assertEquals("{{baseUrl}}/api/allocate/evaluate", folder.at("/item/5/request/url/raw").asText());

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

    @Test
    void operatorScenarioGalleryPostmanFolderIsValidAndReadOnly() throws Exception {
        JsonNode collection = readJson(Path.of("postman/LoadBalancerPro.postman_collection.json"));
        JsonNode folder = findFolder(collection, "Operator Scenario Gallery");
        assertNotNull(folder, "Postman collection should include an Operator Scenario Gallery folder");
        assertEquals(14, folder.path("item").size());

        List<String> expectedNames = List.of(
                "GET Scenario Gallery Health Check",
                "GET Scenario Gallery Readiness Check",
                "POST Normal Load Routing Comparison",
                "POST Normal Load Allocation Preview",
                "POST Normal Load Overload Evaluation Preview",
                "POST Overload Pressure Routing Comparison",
                "POST Overload Pressure Allocation Preview",
                "POST Overload Pressure Overload Evaluation Preview",
                "POST All-Unhealthy Degradation Routing Comparison",
                "POST All-Unhealthy Degradation Allocation Preview",
                "POST All-Unhealthy Degradation Overload Evaluation Preview",
                "POST Recovery Capacity Restored Routing Comparison",
                "POST Recovery Capacity Restored Allocation Preview",
                "POST Recovery Capacity Restored Overload Evaluation Preview");
        for (int i = 0; i < expectedNames.size(); i++) {
            assertEquals(expectedNames.get(i), folder.at("/item/" + i + "/name").asText());
        }

        assertEquals("{{baseUrl}}/api/health", folder.at("/item/0/request/url/raw").asText());
        assertEquals("{{baseUrl}}/actuator/health/readiness", folder.at("/item/1/request/url/raw").asText());
        assertEquals("{{baseUrl}}/api/routing/compare", folder.at("/item/2/request/url/raw").asText());
        assertEquals("{{baseUrl}}/api/allocate/capacity-aware", folder.at("/item/3/request/url/raw").asText());
        assertEquals("{{baseUrl}}/api/allocate/evaluate", folder.at("/item/4/request/url/raw").asText());
        assertTrue(folder.toString().contains("edge-normal-a"));
        assertTrue(folder.toString().contains("edge-overload-a"));
        assertTrue(folder.toString().contains("edge-down-a"));
        assertTrue(folder.toString().contains("edge-recovery-a"));

        String normalized = Files.readString(Path.of("postman/LoadBalancerPro.postman_collection.json"),
                StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(normalized.contains("{{baseurl}}/api/routing/compare"));
        assertTrue(normalized.contains("{{baseurl}}/api/allocate/capacity-aware"));
        assertTrue(normalized.contains("{{baseurl}}/api/allocate/evaluate"));
        assertFalse(normalized.contains("x-api-key"));
        assertFalse(normalized.contains("bearer"));
        assertFalse(normalized.contains("authorization"));
        assertFalse(normalized.contains("/rulesets"));
        assertFalse(normalized.contains("create release"));
        assertFalse(normalized.contains("create tag"));
        assertFalse(normalized.contains("delete-branch"));
    }

    @Test
    void operatorExplanationDrillDownPostmanFolderIsValidAndReadOnly() throws Exception {
        JsonNode collection = readJson(Path.of("postman/LoadBalancerPro.postman_collection.json"));
        JsonNode folder = findFolder(collection, "Operator Explanation Drill-Down");
        assertNotNull(folder, "Postman collection should include an Operator Explanation Drill-Down folder");
        assertEquals(14, folder.path("item").size());

        List<String> expectedNames = List.of(
                "GET Explanation Drill-Down Health Check",
                "GET Explanation Drill-Down Readiness Check",
                "POST Normal Load Routing Explanation",
                "POST Normal Load Allocation Explanation",
                "POST Normal Load Overload And Remediation Explanation",
                "POST Overload Pressure Routing Explanation",
                "POST Overload Pressure Allocation Explanation",
                "POST Overload Pressure Overload And Remediation Explanation",
                "POST All-Unhealthy Degradation Routing Explanation",
                "POST All-Unhealthy Degradation Allocation Explanation",
                "POST All-Unhealthy Degradation Overload And Remediation Explanation",
                "POST Recovery Capacity Restored Routing Explanation",
                "POST Recovery Capacity Restored Allocation Explanation",
                "POST Recovery Capacity Restored Overload And Remediation Explanation");
        for (int i = 0; i < expectedNames.size(); i++) {
            assertEquals(expectedNames.get(i), folder.at("/item/" + i + "/name").asText());
        }

        assertEquals("{{baseUrl}}/api/health", folder.at("/item/0/request/url/raw").asText());
        assertEquals("{{baseUrl}}/actuator/health/readiness", folder.at("/item/1/request/url/raw").asText());
        assertEquals("{{baseUrl}}/api/routing/compare", folder.at("/item/2/request/url/raw").asText());
        assertEquals("{{baseUrl}}/api/allocate/capacity-aware", folder.at("/item/3/request/url/raw").asText());
        assertEquals("{{baseUrl}}/api/allocate/evaluate", folder.at("/item/4/request/url/raw").asText());
        assertTrue(folder.toString().contains("chosen server"));
        assertTrue(folder.toString().contains("loadShedding"));
        assertTrue(folder.toString().contains("remediationPlan"));
        assertTrue(folder.toString().contains("edge-normal-a"));
        assertTrue(folder.toString().contains("edge-overload-a"));
        assertTrue(folder.toString().contains("edge-down-a"));
        assertTrue(folder.toString().contains("edge-recovery-a"));

        String normalized = Files.readString(Path.of("postman/LoadBalancerPro.postman_collection.json"),
                StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(normalized.contains("{{baseurl}}/api/routing/compare"));
        assertTrue(normalized.contains("{{baseurl}}/api/allocate/capacity-aware"));
        assertTrue(normalized.contains("{{baseurl}}/api/allocate/evaluate"));
        assertFalse(normalized.contains("x-api-key"));
        assertFalse(normalized.contains("bearer"));
        assertFalse(normalized.contains("authorization"));
        assertFalse(normalized.contains("/rulesets"));
        assertFalse(normalized.contains("create release"));
        assertFalse(normalized.contains("create tag"));
        assertFalse(normalized.contains("delete-branch"));
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String path, Path fixture) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(Files.readString(fixture, StandardCharsets.UTF_8)));
    }

    private org.springframework.test.web.servlet.ResultActions postJsonNode(String path, JsonNode body)
            throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(body)));
    }

    private static JsonNode statelessRoutingRequest(JsonNode routingRequest) {
        ObjectNode request = (ObjectNode) routingRequest.deepCopy();
        ArrayNode strategies = OBJECT_MAPPER.createArrayNode();
        strategies.add("WEIGHTED_LEAST_CONNECTIONS");
        request.set("strategies", strategies);
        return request;
    }

    private static String statelessRoutingRequest() {
        return """
                {
                  "strategies": [
                    "WEIGHTED_LEAST_CONNECTIONS"
                  ],
                  "servers": [
                    {
                      "serverId": "edge-standard",
                      "healthy": true,
                      "inFlightRequestCount": 5,
                      "configuredCapacity": 100.0,
                      "estimatedConcurrencyLimit": 100.0,
                      "weight": 1.0,
                      "averageLatencyMillis": 16.0,
                      "p95LatencyMillis": 30.0,
                      "p99LatencyMillis": 60.0,
                      "recentErrorRate": 0.0,
                      "queueDepth": 1
                    },
                    {
                      "serverId": "edge-weighted",
                      "healthy": true,
                      "inFlightRequestCount": 12,
                      "configuredCapacity": 100.0,
                      "estimatedConcurrencyLimit": 100.0,
                      "weight": 4.0,
                      "averageLatencyMillis": 18.0,
                      "p95LatencyMillis": 34.0,
                      "p99LatencyMillis": 70.0,
                      "recentErrorRate": 0.0,
                      "queueDepth": 2
                    }
                  ]
                }
                """;
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
