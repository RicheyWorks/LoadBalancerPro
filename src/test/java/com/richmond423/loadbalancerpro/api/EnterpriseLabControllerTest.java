package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.richmond423.loadbalancerpro.core.CloudManager;
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
class EnterpriseLabControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void localProfileListsDeterministicScenarios() throws Exception {
        mockMvc.perform(get("/api/lab/scenarios"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.count", is(10)))
                .andExpect(jsonPath("$.deterministicFixtureVersion", is("adaptive-routing-fixtures-v1")))
                .andExpect(jsonPath("$.supportedModes[0]", is("shadow")))
                .andExpect(jsonPath("$.supportedModes[1]", is("influence")))
                .andExpect(jsonPath("$.supportedModes[2]", is("all")))
                .andExpect(jsonPath("$.scenarios[0].scenarioId", is("normal-balanced-load")))
                .andExpect(jsonPath("$.scenarios[0].signalsInvolved[0]", is("request")))
                .andExpect(jsonPath("$.scenarios[0].expectedGuardrails[0]", containsString("influence")))
                .andExpect(jsonPath("$.scenarios[0].safeForInfluenceExperiment", is(true)));
    }

    @Test
    void localProfileReturnsSingleScenarioAndStableNotFoundError() throws Exception {
        mockMvc.perform(get("/api/lab/scenarios/stale-signal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioId", is("stale-signal")))
                .andExpect(jsonPath("$.category", is("signal-quality")))
                .andExpect(jsonPath("$.safeForInfluenceExperiment", is(false)));

        mockMvc.perform(get("/api/lab/scenarios/missing-scenario"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("not_found")))
                .andExpect(jsonPath("$.path", is("/api/lab/scenarios/missing-scenario")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void localProfileCreatesAndRetrievesBoundedLabRunWithoutCloudMutation() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager = Mockito.mockConstruction(CloudManager.class)) {
            String response = mockMvc.perform(post("/api/lab/runs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "mode": "all",
                                      "scenarioIds": [
                                        "normal-balanced-load",
                                        "tail-latency-pressure",
                                        "stale-signal"
                                      ]
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.runId").isString())
                    .andExpect(jsonPath("$.mode", is("all")))
                    .andExpect(jsonPath("$.activeInfluenceEnabled", is(true)))
                    .andExpect(jsonPath("$.selectedScenarioIds[0]", is("normal-balanced-load")))
                    .andExpect(jsonPath("$.scorecard.totalScenarios", is(3)))
                    .andExpect(jsonPath("$.scorecard.finalRecommendation",
                            is("lab evidence only / not production activation")))
                    .andExpect(jsonPath("$.storageMode", containsString("process-local")))
                    .andExpect(jsonPath("$.results[0].baselineSelectedBackend").isString())
                    .andReturn().getResponse().getContentAsString();
            String runId = JsonPath.read(response, "$.runId");

            mockMvc.perform(get("/api/lab/runs/" + runId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.runId", is(runId)));

            String listResponse = mockMvc.perform(get("/api/lab/runs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").isNumber())
                    .andReturn().getResponse().getContentAsString();

            org.junit.jupiter.api.Assertions.assertTrue(response.contains("lab evidence only"));
            org.junit.jupiter.api.Assertions.assertTrue(listResponse.contains(runId));
            org.junit.jupiter.api.Assertions.assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Enterprise Lab runs must not construct CloudManager or call AWS paths.");
        }
    }

    @Test
    void invalidRunRequestsFailClosedWithStableErrorEnvelope() throws Exception {
        mockMvc.perform(post("/api/lab/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"live\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("Unsupported lab mode")))
                .andExpect(jsonPath("$.path", is("/api/lab/runs")));

        mockMvc.perform(post("/api/lab/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"all\",\"scenarioIds\":[\"missing-scenario\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unknown enterprise lab scenario")));

        mockMvc.perform(get("/api/lab/runs/missing-run"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("not_found")))
                .andExpect(jsonPath("$.path", is("/api/lab/runs/missing-run")));
    }

    @Test
    void runResponseDoesNotContainSecretsOrEnvironmentData() throws Exception {
        String response = mockMvc.perform(post("/api/lab/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"shadow\",\"scenarioIds\":[\"normal-balanced-load\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].scenarioName", is("normal-balanced-load")))
                .andReturn().getResponse().getContentAsString().toLowerCase();

        org.junit.jupiter.api.Assertions.assertFalse(response.contains("x-api-key"));
        org.junit.jupiter.api.Assertions.assertFalse(response.contains("bearer "));
        org.junit.jupiter.api.Assertions.assertFalse(response.contains("credential"));
        org.junit.jupiter.api.Assertions.assertFalse(response.contains("release-downloads"));
        org.junit.jupiter.api.Assertions.assertFalse(response.contains("password"));
    }
}
