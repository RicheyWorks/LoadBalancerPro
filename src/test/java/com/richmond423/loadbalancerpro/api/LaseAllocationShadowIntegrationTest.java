package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "loadbalancerpro.lase.shadow.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class LaseAllocationShadowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void evaluationResponseIncludesShadowOnlyLaseSummaryWithoutChangingAllocation() throws Exception {
        mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 25.0,
                                  "strategy": "CAPACITY_AWARE",
                                  "priority": "USER",
                                  "currentInFlightRequestCount": 10,
                                  "concurrencyLimit": 100,
                                  "queueDepth": 0,
                                  "observedP95LatencyMillis": 80.0,
                                  "observedErrorRate": 0.01,
                                  "servers": [
                                    {
                                      "id": "api-1",
                                      "cpuUsage": 10.0,
                                      "memoryUsage": 10.0,
                                      "diskUsage": 10.0,
                                      "capacity": 100.0,
                                      "weight": 1.0,
                                      "healthy": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations.api-1", closeTo(25.0, 0.01)))
                .andExpect(jsonPath("$.unallocatedLoad", closeTo(0.0, 0.01)))
                .andExpect(jsonPath("$.readOnly", is(true)))
                .andExpect(jsonPath("$.laseShadow.enabled", is(true)))
                .andExpect(jsonPath("$.laseShadow.mode", is("shadow-only")))
                .andExpect(jsonPath("$.laseShadow.observationRecorded", is(true)))
                .andExpect(jsonPath("$.laseShadow.alteredLiveDecision", is(false)))
                .andExpect(jsonPath("$.lasePolicy.mode", is("off")))
                .andExpect(jsonPath("$.lasePolicy.changed", is(false)))
                .andExpect(jsonPath("$.laseShadow.recommendedServerId", is("api-1")))
                .andExpect(jsonPath("$.laseShadow.recommendedAction").isString())
                .andExpect(jsonPath("$.laseShadow.signalsConsidered", hasItem("tail latency")))
                .andExpect(jsonPath("$.laseShadow.signalsConsidered", hasItem("adaptive concurrency")))
                .andExpect(jsonPath("$.laseShadow.decisionImpact", containsString("live allocation was not altered")))
                .andExpect(jsonPath("$.laseShadow.reason", containsString("Evaluation lase-shadow")));

        mockMvc.perform(get("/api/lase/shadow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalEvaluations", is(1)))
                .andExpect(jsonPath("$.recentEvents[0].recommendedServerId", is("api-1")))
                .andExpect(jsonPath("$.recentEvents[0].failSafe", is(false)));
    }
}
