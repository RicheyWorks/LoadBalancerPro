package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.richmond423.loadbalancerpro.core.CloudManager;

import java.util.Map;

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
class EnterpriseLabAdaptiveDecisionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void defaultRequestExecutesVersionedShadowDecisionWithoutTrafficAction() throws Exception {
        try (MockedConstruction<CloudManager> cloudManagers = Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(post("/api/lab/decisions"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.contractVersion", is("enterprise-lab-adaptive-decision/v1")))
                    .andExpect(jsonPath("$.scenarioId", is("normal-balanced-load")))
                    .andExpect(jsonPath("$.fixtureVersion", is("adaptive-routing-fixtures-v1")))
                    .andExpect(jsonPath("$.decision.schemaVersion", is("adaptive-traffic-decision/v1")))
                    .andExpect(jsonPath("$.decision.mode", is("SHADOW")))
                    .andExpect(jsonPath("$.decision.request.candidates", hasSize(3)))
                    .andExpect(jsonPath("$.decision.observations.blue", hasSize(5)))
                    .andExpect(jsonPath("$.decision.scoreBreakdowns.blue.factorContributions", hasSize(12)))
                    .andExpect(jsonPath(
                            "$.decision.scoreBreakdowns.blue.factorContributions[0].rawValue").isNumber())
                    .andExpect(jsonPath(
                            "$.decision.scoreBreakdowns.blue.factorContributions[0].normalizedValue").isNumber())
                    .andExpect(jsonPath(
                            "$.decision.scoreBreakdowns.blue.factorContributions[0].weight").isNumber())
                    .andExpect(jsonPath("$.decision.guardrailDecision.influenceAllowed", is(false)))
                    .andExpect(jsonPath("$.decision.guardrailDecision.changed", is(false)))
                    .andExpect(jsonPath("$.trafficActionPerformed", is(false)))
                    .andExpect(jsonPath("$.contentFingerprint", matchesPattern("[0-9a-f]{64}")))
                    .andExpect(jsonPath("$.safetyNotes[4]", containsString("No proxy")));

            assertTrue(cloudManagers.constructed().isEmpty(),
                    "Adaptive Enterprise Lab decisions must not construct CloudManager or call cloud paths.");
        }
    }

    @Test
    void observeRequestReturnsSignalsAndScoresWithoutProposedAllocation() throws Exception {
        mockMvc.perform(post("/api/lab/decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenarioId": "normal-balanced-load",
                                  "mode": "observe"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision.mode", is("OBSERVE")))
                .andExpect(jsonPath("$.decision.rollingStates.blue.evidence", is("SUFFICIENT")))
                .andExpect(jsonPath("$.decision.scoreBreakdowns.blue.totalScore").isNumber())
                .andExpect(jsonPath("$.decision.allocationRecommendation.allocations", anEmptyMap()))
                .andExpect(jsonPath("$.decision.guardrailDecision.action", is("DENY")))
                .andExpect(jsonPath("$.decision.guardrailDecision.influenceAllowed", is(false)))
                .andExpect(jsonPath("$.trafficActionPerformed", is(false)));
    }

    @Test
    void explicitlyOptedInActiveExperimentReturnsBoundedDecisionDataOnly() throws Exception {
        String response = mockMvc.perform(post("/api/lab/decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenarioId": "tail-latency-pressure",
                                  "mode": "active-experiment",
                                  "explicitExperimentContext": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision.mode", is("ACTIVE_EXPERIMENT")))
                .andExpect(jsonPath("$.decision.guardrailDecision.influenceAllowed", is(true)))
                .andExpect(jsonPath("$.decision.guardrailDecision.changed", is(true)))
                .andExpect(jsonPath("$.decision.guardrailDecision.rollbackReason", containsString("rollback target")))
                .andExpect(jsonPath("$.trafficActionPerformed", is(false)))
                .andReturn().getResponse().getContentAsString();

        double blue = ((Number) JsonPath.read(
                response, "$.decision.guardrailDecision.effectiveAllocations.blue")).doubleValue();
        double green = ((Number) JsonPath.read(
                response, "$.decision.guardrailDecision.effectiveAllocations.green")).doubleValue();
        Map<String, Number> effective = JsonPath.read(
                response, "$.decision.guardrailDecision.effectiveAllocations");
        assertTrue(blue < green);
        assertEquals(1.0, effective.values().stream().mapToDouble(Number::doubleValue).sum(), 0.000000001);
    }

    @Test
    void staleAndNonOptedInRequestsFailClosedToRecordedBaseline() throws Exception {
        String stale = mockMvc.perform(post("/api/lab/decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId":"stale-signal","mode":"recommend"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision.rollingStates.blue.evidence", is("STALE")))
                .andExpect(jsonPath("$.decision.allocationRecommendation.fallbackApplied", is(true)))
                .andExpect(jsonPath("$.decision.guardrailDecision.action", is("DENY")))
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> baseline = JsonPath.read(stale, "$.decision.request.baselineAllocations");
        Map<String, Object> effective = JsonPath.read(
                stale, "$.decision.guardrailDecision.effectiveAllocations");
        assertEquals(baseline, effective);

        mockMvc.perform(post("/api/lab/decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId":"tail-latency-pressure","mode":"active-experiment"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision.guardrailDecision.action", is("DENY")))
                .andExpect(jsonPath("$.decision.guardrailDecision.reasons[0]",
                        containsString("explicit bounded experiment context")))
                .andExpect(jsonPath("$.trafficActionPerformed", is(false)));
    }

    @Test
    void invalidScenarioAndModeReturnStableBadRequestEnvelope() throws Exception {
        mockMvc.perform(post("/api/lab/decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId":"missing-scenario","mode":"recommend"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("Unknown enterprise lab scenario")))
                .andExpect(jsonPath("$.path", is("/api/lab/decisions")));

        String invalidMode = mockMvc.perform(post("/api/lab/decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId":"normal-balanced-load","mode":"live"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported lab mode")))
                .andReturn().getResponse().getContentAsString().toLowerCase();
        assertFalse(invalidMode.contains("x-api-key"));
        assertFalse(invalidMode.contains("credential"));
        assertFalse(invalidMode.contains("password"));
    }
}
