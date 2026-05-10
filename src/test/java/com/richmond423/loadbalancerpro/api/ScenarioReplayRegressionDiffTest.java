package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
class ScenarioReplayRegressionDiffTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String EXPECTED_ROOT = "scenarios/replay/expected/";
    private static final List<String> VALID_EXPECTED_DESCRIPTORS = List.of(
            "normal-baseline.expected.json",
            "overload-scale-recommendation.expected.json",
            "single-server-failure-recovery.expected.json",
            "all-unhealthy-degradation.expected.json",
            "mixed-incident-replay.expected.json");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void catalogLinksEveryFixtureToAnExpectedDescriptor() throws Exception {
        JsonNode catalog = readJson("scenarios/replay/catalog.json");

        assertEquals(6, catalog.path("fixtures").size(), diff("catalog", "/fixtures", 6,
                catalog.path("fixtures").size()));
        for (JsonNode entry : catalog.path("fixtures")) {
            String fixtureName = entry.path("name").asText();
            String requestPath = entry.path("path").asText();
            String expectedPath = entry.path("expectedPath").asText();
            JsonNode descriptor = readJson(expectedPath);

            assertResourceExists(requestPath, fixtureName + " request fixture");
            assertResourceExists(expectedPath, fixtureName + " expected descriptor");
            assertEquals(fixtureName, descriptor.path("fixtureName").asText(),
                    diff(fixtureName, "/fixtureName", fixtureName, descriptor.path("fixtureName").asText()));
            assertEquals(requestPath, descriptor.path("requestPath").asText(),
                    diff(fixtureName, "/requestPath", requestPath, descriptor.path("requestPath").asText()));
            assertEquals(entry.path("expectedStatus").asInt(), descriptor.path("expectedStatus").asInt(),
                    diff(fixtureName, "/expectedStatus", entry.path("expectedStatus").asInt(),
                            descriptor.path("expectedStatus").asInt()));
        }
    }

    @Test
    void expectedDescriptorsMatchCurrentScenarioReplayOutput() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            for (String descriptorName : VALID_EXPECTED_DESCRIPTORS) {
                JsonNode descriptor = readJson(EXPECTED_ROOT + descriptorName);
                JsonNode actual = replayOk(descriptor.path("requestPath").asText());

                compareReplay(descriptor, actual);
            }

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Regression-diff fixtures must remain read-only and never construct CloudManager.");
        }
    }

    @Test
    void invalidScenarioDescriptorMatchesControlledBadRequest() throws Exception {
        JsonNode descriptor = readJson(EXPECTED_ROOT + "invalid-scenario.expected.json");
        JsonNode error = replayBadRequest(descriptor.path("requestPath").asText());
        String fixtureName = descriptor.path("fixtureName").asText();

        assertEquals(descriptor.path("error").asText(), error.path("error").asText(),
                diff(fixtureName, "/error", descriptor.path("error").asText(), error.path("error").asText()));
        assertTrue(error.path("message").asText().contains(descriptor.path("messageContains").asText()),
                diff(fixtureName, "/message", descriptor.path("messageContains").asText(),
                        error.path("message").asText()));
        assertEquals(descriptor.path("path").asText(), error.path("path").asText(),
                diff(fixtureName, "/path", descriptor.path("path").asText(), error.path("path").asText()));
        assertFalse(error.has("trace"), diff(fixtureName, "/trace", "absent", error.path("trace")));
        assertFalse(error.has("exception"),
                diff(fixtureName, "/exception", "absent", error.path("exception")));
    }

    private void compareReplay(JsonNode descriptor, JsonNode actual) {
        String fixtureName = descriptor.path("fixtureName").asText();
        assertEquals(descriptor.path("fixtureName").asText(), actual.path("scenarioId").asText(),
                diff(fixtureName, "/scenarioId", descriptor.path("fixtureName").asText(),
                        actual.path("scenarioId").asText()));
        assertEquals(descriptor.path("expectedReadOnly").asBoolean(), actual.path("readOnly").asBoolean(),
                diff(fixtureName, "/readOnly", descriptor.path("expectedReadOnly").asBoolean(),
                        actual.path("readOnly").asBoolean()));
        assertEquals(descriptor.path("expectedCloudMutation").asBoolean(), actual.path("cloudMutation").asBoolean(),
                diff(fixtureName, "/cloudMutation", descriptor.path("expectedCloudMutation").asBoolean(),
                        actual.path("cloudMutation").asBoolean()));
        assertEquals(expectedStepTypes(descriptor), actualStepTypes(actual),
                diff(fixtureName, "/steps[*].type", expectedStepTypes(descriptor), actualStepTypes(actual)));
        assertEquals(descriptor.path("steps").size(), actual.path("steps").size(),
                diff(fixtureName, "/steps.length", descriptor.path("steps").size(), actual.path("steps").size()));

        for (JsonNode stepExpectation : descriptor.path("steps")) {
            compareStep(fixtureName, stepExpectation, actual.path("steps").get(stepExpectation.path("index").asInt()));
        }
        if (descriptor.path("expectNoNegativeAllocations").asBoolean(false)) {
            assertNoNegativeAllocations(fixtureName, actual);
        }
    }

    private void compareStep(String fixtureName, JsonNode expectedStep, JsonNode actualStep) {
        int index = expectedStep.path("index").asInt();
        String basePath = "/steps/" + index;
        assertEquals(expectedStep.path("stepId").asText(), actualStep.path("stepId").asText(),
                diff(fixtureName, basePath + "/stepId", expectedStep.path("stepId").asText(),
                        actualStep.path("stepId").asText()));
        assertEquals(expectedStep.path("type").asText(), actualStep.path("type").asText(),
                diff(fixtureName, basePath + "/type", expectedStep.path("type").asText(),
                        actualStep.path("type").asText()));

        compareOptionalDouble(fixtureName, expectedStep, actualStep, basePath, "acceptedLoad");
        compareOptionalDouble(fixtureName, expectedStep, actualStep, basePath, "rejectedLoad");
        compareOptionalDouble(fixtureName, expectedStep, actualStep, basePath, "unallocatedLoad");
        compareOptionalInt(fixtureName, expectedStep, actualStep, basePath, "recommendedAdditionalServers");
        compareOptionalNestedInt(fixtureName, expectedStep, actualStep, basePath,
                "scalingRecommendedAdditionalServers", "scalingSimulation", "recommendedAdditionalServers");
        compareOptionalText(fixtureName, expectedStep, actualStep, basePath, "selectedServerId");
        compareOptionalNestedText(fixtureName, expectedStep, actualStep, basePath,
                "routingFirstChosenServerId", "routingResults", 0, "chosenServerId");
        compareOptionalNestedText(fixtureName, expectedStep, actualStep, basePath,
                "loadSheddingPriority", "loadShedding", "priority");
        compareOptionalNestedText(fixtureName, expectedStep, actualStep, basePath,
                "loadSheddingAction", "loadShedding", "action");
        compareOptionalNestedBoolean(fixtureName, expectedStep, actualStep, basePath,
                "metricsPreviewEmitted", "metricsPreview", "emitted");
        compareAllocationKeys(fixtureName, expectedStep, actualStep, basePath);
        compareServerHealth(fixtureName, expectedStep, actualStep, basePath);
    }

    private void compareOptionalDouble(
            String fixtureName, JsonNode expectedStep, JsonNode actualStep, String basePath, String field) {
        if (expectedStep.has(field)) {
            assertEquals(expectedStep.path(field).asDouble(), actualStep.path(field).asDouble(), 0.01,
                    diff(fixtureName, basePath + "/" + field, expectedStep.path(field).asDouble(),
                            actualStep.path(field).asDouble()));
        }
    }

    private void compareOptionalInt(
            String fixtureName, JsonNode expectedStep, JsonNode actualStep, String basePath, String field) {
        if (expectedStep.has(field)) {
            assertEquals(expectedStep.path(field).asInt(), actualStep.path(field).asInt(),
                    diff(fixtureName, basePath + "/" + field, expectedStep.path(field).asInt(),
                            actualStep.path(field).asInt()));
        }
    }

    private void compareOptionalNestedInt(String fixtureName, JsonNode expectedStep, JsonNode actualStep,
                                          String basePath, String expectedField, String parent, String field) {
        if (expectedStep.has(expectedField)) {
            assertEquals(expectedStep.path(expectedField).asInt(), actualStep.path(parent).path(field).asInt(),
                    diff(fixtureName, basePath + "/" + parent + "/" + field,
                            expectedStep.path(expectedField).asInt(),
                            actualStep.path(parent).path(field).asInt()));
        }
    }

    private void compareOptionalText(
            String fixtureName, JsonNode expectedStep, JsonNode actualStep, String basePath, String field) {
        if (expectedStep.has(field)) {
            compareNullableText(fixtureName, basePath + "/" + field, expectedStep.path(field), actualStep.path(field));
        }
    }

    private void compareOptionalNestedText(String fixtureName, JsonNode expectedStep, JsonNode actualStep,
                                           String basePath, String expectedField, String parent, String field) {
        if (expectedStep.has(expectedField)) {
            compareNullableText(fixtureName, basePath + "/" + parent + "/" + field,
                    expectedStep.path(expectedField), actualStep.path(parent).path(field));
        }
    }

    private void compareOptionalNestedText(String fixtureName, JsonNode expectedStep, JsonNode actualStep,
                                           String basePath, String expectedField, String parent, int index,
                                           String field) {
        if (expectedStep.has(expectedField)) {
            compareNullableText(fixtureName, basePath + "/" + parent + "/" + index + "/" + field,
                    expectedStep.path(expectedField), actualStep.path(parent).path(index).path(field));
        }
    }

    private void compareNullableText(String fixtureName, String path, JsonNode expected, JsonNode actual) {
        if (expected.isNull()) {
            assertTrue(actual.isNull(), diff(fixtureName, path, null, actual));
        } else {
            assertEquals(expected.asText(), actual.asText(), diff(fixtureName, path, expected.asText(), actual.asText()));
        }
    }

    private void compareOptionalNestedBoolean(String fixtureName, JsonNode expectedStep, JsonNode actualStep,
                                              String basePath, String expectedField, String parent, String field) {
        if (expectedStep.has(expectedField)) {
            assertEquals(expectedStep.path(expectedField).asBoolean(), actualStep.path(parent).path(field).asBoolean(),
                    diff(fixtureName, basePath + "/" + parent + "/" + field,
                            expectedStep.path(expectedField).asBoolean(),
                            actualStep.path(parent).path(field).asBoolean()));
        }
    }

    private void compareAllocationKeys(String fixtureName, JsonNode expectedStep, JsonNode actualStep, String basePath) {
        for (JsonNode key : expectedStep.path("allocationKeysPresent")) {
            assertTrue(actualStep.path("allocations").has(key.asText()),
                    diff(fixtureName, basePath + "/allocations/" + key.asText(), "present", "absent"));
        }
        for (JsonNode key : expectedStep.path("allocationKeysAbsent")) {
            assertFalse(actualStep.path("allocations").has(key.asText()),
                    diff(fixtureName, basePath + "/allocations/" + key.asText(), "absent", "present"));
        }
    }

    private void compareServerHealth(String fixtureName, JsonNode expectedStep, JsonNode actualStep, String basePath) {
        Iterator<Map.Entry<String, JsonNode>> expectedHealth = expectedStep.path("serverHealth").fields();
        while (expectedHealth.hasNext()) {
            Map.Entry<String, JsonNode> expectation = expectedHealth.next();
            JsonNode server = findServerState(actualStep, expectation.getKey());
            assertEquals(expectation.getValue().asBoolean(), server.path("healthy").asBoolean(),
                    diff(fixtureName, basePath + "/serverStates/" + expectation.getKey() + "/healthy",
                            expectation.getValue().asBoolean(), server.path("healthy").asBoolean()));
        }
    }

    private JsonNode findServerState(JsonNode actualStep, String serverId) {
        for (JsonNode server : actualStep.path("serverStates")) {
            if (serverId.equals(server.path("id").asText())) {
                return server;
            }
        }
        throw new AssertionError("Missing server state for " + serverId);
    }

    private void assertNoNegativeAllocations(String fixtureName, JsonNode actual) {
        for (JsonNode step : actual.path("steps")) {
            String stepId = step.path("stepId").asText();
            assertTrue(step.path("acceptedLoad").asDouble() >= 0.0,
                    diff(fixtureName, "/steps/" + stepId + "/acceptedLoad", ">= 0", step.path("acceptedLoad")));
            assertTrue(step.path("rejectedLoad").asDouble() >= 0.0,
                    diff(fixtureName, "/steps/" + stepId + "/rejectedLoad", ">= 0", step.path("rejectedLoad")));
            assertTrue(step.path("unallocatedLoad").asDouble() >= 0.0,
                    diff(fixtureName, "/steps/" + stepId + "/unallocatedLoad", ">= 0",
                            step.path("unallocatedLoad")));
            assertTrue(step.path("recommendedAdditionalServers").asInt() >= 0,
                    diff(fixtureName, "/steps/" + stepId + "/recommendedAdditionalServers", ">= 0",
                            step.path("recommendedAdditionalServers")));

            Iterator<Map.Entry<String, JsonNode>> allocations = step.path("allocations").fields();
            while (allocations.hasNext()) {
                Map.Entry<String, JsonNode> allocation = allocations.next();
                assertTrue(allocation.getValue().asDouble() >= 0.0,
                        diff(fixtureName, "/steps/" + stepId + "/allocations/" + allocation.getKey(),
                                ">= 0", allocation.getValue()));
            }
        }
    }

    private JsonNode replayOk(String requestPath) throws Exception {
        String content = mockMvc.perform(post("/api/scenarios/replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceText(requestPath)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return OBJECT_MAPPER.readTree(content);
    }

    private JsonNode replayBadRequest(String requestPath) throws Exception {
        String content = mockMvc.perform(post("/api/scenarios/replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceText(requestPath)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return OBJECT_MAPPER.readTree(content);
    }

    private JsonNode readJson(String resourcePath) throws IOException {
        return OBJECT_MAPPER.readTree(resourceText(resourcePath));
    }

    private String resourceText(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void assertResourceExists(String resourcePath, String description) {
        assertTrue(new ClassPathResource(resourcePath).exists(), description + " should exist: " + resourcePath);
    }

    private List<String> expectedStepTypes(JsonNode descriptor) {
        List<String> types = new ArrayList<>();
        for (JsonNode type : descriptor.path("expectedStepTypes")) {
            types.add(type.asText());
        }
        return types;
    }

    private List<String> actualStepTypes(JsonNode response) {
        List<String> types = new ArrayList<>();
        for (JsonNode step : response.path("steps")) {
            types.add(step.path("type").asText());
        }
        return types;
    }

    private String diff(String fixtureName, String path, Object expected, Object actual) {
        return "Scenario replay regression diff mismatch for fixture '%s' at %s: expected <%s> but was <%s>"
                .formatted(fixtureName, path, expected, actual);
    }
}
