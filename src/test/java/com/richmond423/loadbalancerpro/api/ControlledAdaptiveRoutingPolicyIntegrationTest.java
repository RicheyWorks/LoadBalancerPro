package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
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

@SpringBootTest(properties = {
        "loadbalancerpro.lase.policy.mode=recommend",
        "loadbalancerpro.lase.policy.active-experiment-enabled=false"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ControlledAdaptiveRoutingPolicyIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void allocationEvaluationExposesRecommendPolicyWithoutChangingBaselineAllocation() throws Exception {
        mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 50.0,
                                  "strategy": "CAPACITY_AWARE",
                                  "servers": [
                                    {
                                      "id": "blue",
                                      "cpuUsage": 90.0,
                                      "memoryUsage": 90.0,
                                      "diskUsage": 90.0,
                                      "capacity": 100.0,
                                      "weight": 1.0,
                                      "healthy": true
                                    },
                                    {
                                      "id": "green",
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
                .andExpect(jsonPath("$.readOnly", is(true)))
                .andExpect(jsonPath("$.lasePolicy.mode", is("recommend")))
                .andExpect(jsonPath("$.lasePolicy.changed", is(false)))
                .andExpect(jsonPath("$.lasePolicy.finalDecision").isString())
                .andExpect(jsonPath("$.lasePolicy.guardrailReasons[0]",
                        containsString("recommend mode requires explicit operator acceptance")))
                .andExpect(jsonPath("$.lasePolicy.rollbackReason",
                        containsString("baseline retained until recommendation is accepted")));
    }
}
