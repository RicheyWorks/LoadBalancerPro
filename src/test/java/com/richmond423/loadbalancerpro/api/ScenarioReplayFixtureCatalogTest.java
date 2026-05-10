package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.core.CloudManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ScenarioReplayFixtureCatalogTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> VALID_FIXTURES = List.of(
            "normal-baseline.json",
            "overload-scale-recommendation.json",
            "single-server-failure-recovery.json",
            "all-unhealthy-degradation.json",
            "mixed-incident-replay.json");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void catalogIndexListsEveryCuratedIncidentFixture() throws Exception {
        JsonNode catalog = readFixture("catalog.json");

        assertEquals(1, catalog.path("catalogVersion").asInt());
        assertEquals(6, catalog.path("fixtures").size());
        assertEquals(List.of(
                "normal-baseline",
                "overload-scale-recommendation",
                "single-server-failure-recovery",
                "all-unhealthy-degradation",
                "mixed-incident-replay",
                "invalid-scenario"), catalogNames(catalog));

        for (JsonNode fixture : catalog.path("fixtures")) {
            String path = fixture.path("path").asText();
            assertTrue(new ClassPathResource(path).exists(), () -> "Missing catalog fixture " + path);
            assertFalse(fixture.path("purpose").asText().isBlank(), () -> path + " should describe its purpose");
        }
    }

    @Test
    void normalBaselineFixtureCoversHealthyAllocationEvaluationAndRouting() throws Exception {
        JsonNode response = replay("normal-baseline.json");

        assertReplayEnvelope(response, "normal-baseline");
        assertStepTypes(response, "ALLOCATE", "EVALUATE", "ROUTE");
        assertCloseTo(50.0, step(response, 0).path("acceptedLoad").asDouble());
        assertCloseTo(0.0, step(response, 0).path("unallocatedLoad").asDouble());
        assertEquals(0, step(response, 0).path("recommendedAdditionalServers").asInt());
        assertFalse(step(response, 0).path("metricsPreview").path("emitted").asBoolean());
        assertCloseTo(40.0, step(response, 1).path("acceptedLoad").asDouble());
        assertCloseTo(0.0, step(response, 1).path("unallocatedLoad").asDouble());
        assertEquals("ALLOW", step(response, 1).path("loadShedding").path("action").asText());
        assertFalse(step(response, 1).path("metricsPreview").path("emitted").asBoolean());
        assertEquals("green", step(response, 2).path("selectedServerId").asText());
        assertNoNegativeAllocations(response);
    }

    @Test
    void overloadFixtureCapturesScaleRecommendationAndLoadSheddingDecision() throws Exception {
        JsonNode response = replay("overload-scale-recommendation.json");

        assertReplayEnvelope(response, "overload-scale-recommendation");
        assertStepTypes(response, "OVERLOAD");
        JsonNode overload = step(response, 0);
        assertCloseTo(100.0, overload.path("acceptedLoad").asDouble());
        assertCloseTo(50.0, overload.path("rejectedLoad").asDouble());
        assertCloseTo(50.0, overload.path("unallocatedLoad").asDouble());
        assertEquals(1, overload.path("recommendedAdditionalServers").asInt());
        assertEquals(1, overload.path("scalingSimulation").path("recommendedAdditionalServers").asInt());
        assertEquals("BACKGROUND", overload.path("loadShedding").path("priority").asText());
        assertEquals("SHED", overload.path("loadShedding").path("action").asText());
        assertFalse(overload.path("metricsPreview").path("emitted").asBoolean());
        assertNoNegativeAllocations(response);
    }

    @Test
    void failureRecoveryFixtureRoutesAroundFailedServerThenReEntersIt() throws Exception {
        JsonNode response = replay("single-server-failure-recovery.json");

        assertReplayEnvelope(response, "single-server-failure-recovery");
        assertStepTypes(response, "MARK_UNHEALTHY", "ROUTE", "ALLOCATE", "MARK_HEALTHY", "ROUTE");
        assertFalse(step(response, 0).path("serverStates").get(0).path("healthy").asBoolean());
        assertEquals("blue", step(response, 1).path("selectedServerId").asText());
        assertTrue(step(response, 2).path("allocations").path("green").isMissingNode());
        assertCloseTo(60.0, step(response, 2).path("allocations").path("blue").asDouble());
        assertTrue(step(response, 3).path("serverStates").get(0).path("healthy").asBoolean());
        assertEquals("green", step(response, 4).path("selectedServerId").asText());
        assertNoNegativeAllocations(response);
    }

    @Test
    void allUnhealthyFixtureGracefullyDegradesWithoutRoutingChoice() throws Exception {
        JsonNode response = replay("all-unhealthy-degradation.json");

        assertReplayEnvelope(response, "all-unhealthy-degradation");
        assertStepTypes(response, "EVALUATE", "ROUTE");
        assertCloseTo(0.0, step(response, 0).path("acceptedLoad").asDouble());
        assertCloseTo(80.0, step(response, 0).path("rejectedLoad").asDouble());
        assertCloseTo(80.0, step(response, 0).path("unallocatedLoad").asDouble());
        assertEquals(0, step(response, 0).path("recommendedAdditionalServers").asInt());
        assertFalse(step(response, 0).path("metricsPreview").path("emitted").asBoolean());
        assertTrue(step(response, 1).path("selectedServerId").isNull());
        assertTrue(step(response, 1).path("routingResults").get(0).path("chosenServerId").isNull());
        assertNoNegativeAllocations(response);
    }

    @Test
    void mixedIncidentFixtureProducesDeterministicOrderedReplay() throws Exception {
        JsonNode first = replay("mixed-incident-replay.json");
        JsonNode second = replay("mixed-incident-replay.json");

        assertEquals(first, second, "Curated incident replay output should be deterministic.");
        assertReplayEnvelope(first, "mixed-incident-replay");
        assertStepTypes(first, "ROUTE", "OVERLOAD", "MARK_UNHEALTHY", "EVALUATE", "MARK_HEALTHY", "ALLOCATE");
        assertEquals("green", step(first, 0).path("selectedServerId").asText());
        assertTrue(step(first, 1).path("unallocatedLoad").asDouble() > 0.0);
        assertTrue(step(first, 1).path("recommendedAdditionalServers").asInt() > 0);
        assertFalse(step(first, 2).path("serverStates").get(1).path("healthy").asBoolean());
        assertFalse(step(first, 3).path("metricsPreview").path("emitted").asBoolean());
        assertTrue(step(first, 4).path("serverStates").get(1).path("healthy").asBoolean());
        assertTrue(step(first, 5).path("acceptedLoad").asDouble() > 0.0);
        assertNoNegativeAllocations(first);
    }

    @Test
    void validFixturesRemainReadOnlyAndNeverConstructCloudManager() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            for (String fixtureName : VALID_FIXTURES) {
                JsonNode response = replay(fixtureName);
                assertTrue(response.path("readOnly").asBoolean(), fixtureName + " should be read-only");
                assertFalse(response.path("cloudMutation").asBoolean(), fixtureName + " should avoid cloud mutation");
                assertNoNegativeAllocations(response);
            }

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Curated scenario replay fixtures must not construct CloudManager.");
        }
    }

    @Test
    void invalidScenarioFixtureReturnsControlledBadRequest() throws Exception {
        JsonNode error = replayBadRequest("invalid-scenario.json");

        assertEquals("bad_request", error.path("error").asText());
        assertTrue(error.path("message").asText().contains("Unsupported scenario step type"));
        assertEquals("/api/scenarios/replay", error.path("path").asText());
        assertFalse(error.has("trace"));
        assertFalse(error.has("exception"));
    }

    private JsonNode replay(String fixtureName) throws Exception {
        String content = mockMvc.perform(post("/api/scenarios/replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fixtureText(fixtureName)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return OBJECT_MAPPER.readTree(content);
    }

    private JsonNode replayBadRequest(String fixtureName) throws Exception {
        String content = mockMvc.perform(post("/api/scenarios/replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fixtureText(fixtureName)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return OBJECT_MAPPER.readTree(content);
    }

    private JsonNode readFixture(String fixtureName) throws IOException {
        return OBJECT_MAPPER.readTree(fixtureText(fixtureName));
    }

    private String fixtureText(String fixtureName) throws IOException {
        ClassPathResource resource = new ClassPathResource("scenarios/replay/" + fixtureName);
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void assertReplayEnvelope(JsonNode response, String scenarioId) {
        assertEquals(scenarioId, response.path("scenarioId").asText());
        assertTrue(response.path("readOnly").asBoolean());
        assertFalse(response.path("cloudMutation").asBoolean());
        assertTrue(response.path("steps").isArray());
    }

    private void assertStepTypes(JsonNode response, String... expectedTypes) {
        assertEquals(List.of(expectedTypes), stepTypes(response));
    }

    private List<String> stepTypes(JsonNode response) {
        List<String> types = new ArrayList<>();
        for (JsonNode step : response.path("steps")) {
            types.add(step.path("type").asText());
        }
        return types;
    }

    private List<String> catalogNames(JsonNode catalog) {
        List<String> names = new ArrayList<>();
        for (JsonNode fixture : catalog.path("fixtures")) {
            names.add(fixture.path("name").asText());
        }
        return names;
    }

    private JsonNode step(JsonNode response, int index) {
        JsonNode step = response.path("steps").get(index);
        assertNotNull(step, () -> "Missing replay step at index " + index);
        return step;
    }

    private void assertNoNegativeAllocations(JsonNode response) {
        for (JsonNode step : response.path("steps")) {
            assertTrue(step.path("acceptedLoad").asDouble() >= 0.0, () -> step.path("stepId") + " accepted load");
            assertTrue(step.path("rejectedLoad").asDouble() >= 0.0, () -> step.path("stepId") + " rejected load");
            assertTrue(step.path("unallocatedLoad").asDouble() >= 0.0, () -> step.path("stepId") + " unallocated load");
            assertTrue(step.path("recommendedAdditionalServers").asInt() >= 0,
                    () -> step.path("stepId") + " recommended server count");

            JsonNode allocations = step.path("allocations");
            if (allocations.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = allocations.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> allocation = fields.next();
                    assertTrue(allocation.getValue().asDouble() >= 0.0,
                            () -> step.path("stepId") + " allocation " + allocation.getKey());
                }
            }
        }
    }

    private void assertCloseTo(double expected, double actual) {
        assertEquals(expected, actual, 0.01);
    }
}
