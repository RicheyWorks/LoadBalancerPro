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
                .andExpect(jsonPath("$.supportedModes[0]", is("off")))
                .andExpect(jsonPath("$.supportedModes[1]", is("shadow")))
                .andExpect(jsonPath("$.supportedModes[2]", is("recommend")))
                .andExpect(jsonPath("$.supportedModes[3]", is("active-experiment")))
                .andExpect(jsonPath("$.scenarios[0].scenarioId", is("normal-balanced-load")))
                .andExpect(jsonPath("$.scenarios[0].signalsInvolved[0]", is("request")))
                .andExpect(jsonPath("$.scenarios[0].expectedGuardrails[0]", containsString("active-experiment")))
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
                                      "mode": "active-experiment",
                                      "scenarioIds": [
                                        "normal-balanced-load",
                                        "tail-latency-pressure",
                                        "stale-signal"
                                      ]
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.runId").isString())
                    .andExpect(jsonPath("$.mode", is("active-experiment")))
                    .andExpect(jsonPath("$.activeInfluenceEnabled", is(true)))
                    .andExpect(jsonPath("$.selectedScenarioIds[0]", is("normal-balanced-load")))
                    .andExpect(jsonPath("$.scorecard.totalScenarios", is(3)))
                    .andExpect(jsonPath("$.scorecard.finalRecommendation",
                            is("controlled active-experiment evidence only / not production activation")))
                    .andExpect(jsonPath("$.policyAuditEvents[0].mode").isString())
                    .andExpect(jsonPath("$.results[0].policyDecision.mode").isString())
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

            org.junit.jupiter.api.Assertions.assertTrue(response.contains("not production activation"));
            org.junit.jupiter.api.Assertions.assertTrue(listResponse.contains(runId));
            org.junit.jupiter.api.Assertions.assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Enterprise Lab runs must not construct CloudManager or call AWS paths.");
        }
    }

    @Test
    void noBodyRunDefaultsToShadowMode() throws Exception {
        mockMvc.perform(post("/api/lab/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode", is("shadow")))
                .andExpect(jsonPath("$.activeInfluenceEnabled", is(false)))
                .andExpect(jsonPath("$.results[0].resultChanged", is(false)));
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
                        .content("{\"mode\":\"active-experiment\",\"scenarioIds\":[\"missing-scenario\"]}"))
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

    @Test
    void policyStatusAndAuditEndpointsAreLocalReadableAndSecretFree() throws Exception {
        mockMvc.perform(get("/api/lab/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuredMode", is("off")))
                .andExpect(jsonPath("$.currentMode", is("off")))
                .andExpect(jsonPath("$.activeExperimentEnabled", is(false)))
                .andExpect(jsonPath("$.allowedModes[0]", is("off")))
                .andExpect(jsonPath("$.allowedModes[3]", is("active-experiment")))
                .andExpect(jsonPath("$.warning", containsString("not production certification")));

        mockMvc.perform(post("/api/lab/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"recommend\",\"scenarioIds\":[\"tail-latency-pressure\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode", is("recommend")))
                .andExpect(jsonPath("$.results[0].resultChanged", is(false)))
                .andExpect(jsonPath("$.results[0].rollbackReason", containsString("recommendation")));

        String audit = mockMvc.perform(get("/api/lab/audit-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").isNumber())
                .andExpect(jsonPath("$.events[0].mode").isString())
                .andReturn().getResponse().getContentAsString().toLowerCase();

        org.junit.jupiter.api.Assertions.assertFalse(audit.contains("x-api-key"));
        org.junit.jupiter.api.Assertions.assertFalse(audit.contains("bearer "));
        org.junit.jupiter.api.Assertions.assertFalse(audit.contains("password"));
    }

    @Test
    void metricsEndpointsExposeLabGradeProcessLocalCounters() throws Exception {
        mockMvc.perform(post("/api/lab/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"shadow\",\"scenarioIds\":[\"normal-balanced-load\"]}"))
                .andExpect(status().isOk());

        String metrics = mockMvc.perform(get("/api/lab/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labRunsCreated").isNumber())
                .andExpect(jsonPath("$.labScenariosExecuted").isNumber())
                .andExpect(jsonPath("$.policyDecisionsByMode.shadow").isNumber())
                .andExpect(jsonPath("$.warning", containsString("lab-grade")))
                .andReturn().getResponse().getContentAsString().toLowerCase();

        org.junit.jupiter.api.Assertions.assertFalse(metrics.contains("x-api-key"));
        org.junit.jupiter.api.Assertions.assertFalse(metrics.contains("bearer "));
        org.junit.jupiter.api.Assertions.assertFalse(metrics.contains("password"));

        mockMvc.perform(get("/api/lab/metrics/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("loadbalancerpro_lab_runs_total")))
                .andExpect(content().string(containsString("loadbalancerpro_lase_policy_decisions_total")));
    }
}
