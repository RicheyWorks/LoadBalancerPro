package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "loadbalancerpro.api.rate-limit.enabled=true",
        "loadbalancerpro.api.rate-limit.capacity=1",
        "loadbalancerpro.api.rate-limit.refill-tokens=1",
        "loadbalancerpro.api.rate-limit.refill-period=PT1H"
})
@AutoConfigureMockMvc
class ApiRateLimitIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void enabledLimiterProtectsAllocationEvaluationEndpointAfterOneRequest() throws Exception {
        mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluationRequest()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluationRequest()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "3600"))
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.error", is("rate_limited")))
                .andExpect(jsonPath("$.path", is("/api/allocate/evaluate")));
    }

    private static String evaluationRequest() {
        return """
                {
                  "requestedLoad": 30.0,
                  "strategy": "CAPACITY_AWARE",
                  "priority": "USER",
                  "currentInFlightRequestCount": 10,
                  "concurrencyLimit": 100,
                  "queueDepth": 0,
                  "observedP95LatencyMillis": 90.0,
                  "observedErrorRate": 0.01,
                  "servers": [
                    {
                      "id": "api-1",
                      "cpuUsage": 20.0,
                      "memoryUsage": 20.0,
                      "diskUsage": 20.0,
                      "capacity": 100.0,
                      "weight": 1.0,
                      "healthy": true
                    }
                  ]
                }
                """;
    }
}
