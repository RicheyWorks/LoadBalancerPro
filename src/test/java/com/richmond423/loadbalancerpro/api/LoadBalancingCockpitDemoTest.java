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
import com.richmond423.loadbalancerpro.core.CloudManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loadBalancingCockpitPageExistsAndIsServed() throws Exception {
        assertTrue(Files.exists(COCKPIT_PAGE), "load-balancing cockpit page should be source-controlled");

        mockMvc.perform(get("/load-balancing-cockpit.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Load-Balancing Cockpit")))
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
        assertTrue(normalized.contains("no cloudmanager required for cockpit demo"));
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

            postJson("/api/routing/compare", ROUTING_FIXTURE)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestedStrategies[0]", is("TAIL_LATENCY_POWER_OF_TWO")))
                    .andExpect(jsonPath("$.candidateCount", is(3)))
                    .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                    .andExpect(jsonPath("$.results[1].strategyId", is("WEIGHTED_LEAST_LOAD")))
                    .andExpect(jsonPath("$.results[2].strategyId", is("WEIGHTED_LEAST_CONNECTIONS")))
                    .andExpect(jsonPath("$.results[3].strategyId", is("WEIGHTED_ROUND_ROBIN")))
                    .andExpect(jsonPath("$.results[4].strategyId", is("ROUND_ROBIN")));

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

    private org.springframework.test.web.servlet.ResultActions postJson(String path, Path fixture) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(Files.readString(fixture, StandardCharsets.UTF_8)));
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
