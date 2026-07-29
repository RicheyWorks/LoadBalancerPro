package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.profiles.active=local")
@AutoConfigureMockMvc
class LocalProfileConfigurationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AllocatorService allocatorService;

    @Autowired
    private Environment environment;

    @Test
    void localProfileKeepsHealthAndInfoAvailableButDoesNotExposeMetrics() throws Exception {
        assertEquals("none", environment.getProperty("loadbalancerpro.auth.mode"));
        assertEquals("health,info", environment.getProperty("management.endpoints.web.exposure.include"));
        assertEquals("false", environment.getProperty("management.prometheus.metrics.export.enabled"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isNotFound());

        assertFalse(Boolean.parseBoolean(environment.getProperty("management.otlp.metrics.export.enabled")));
    }

    @Test
    void localProfileKeepsDemoCorsOriginsAvailable() throws Exception {
        mockMvc.perform(options("/api/allocate/capacity-aware")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void localProfileKeepsLaseShadowAdvisorDisabledByDefault() {
        assertFalse(allocatorService.isLaseShadowEnabledForTesting());
    }

    @Test
    void localProfileDoesNotRequireApiKeyForAllocationRequests() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 10.0,
                                  "servers": [
                                    {
                                      "id": "api-1",
                                      "cpuUsage": 10.0,
                                      "memoryUsage": 20.0,
                                      "diskUsage": 30.0,
                                      "capacity": 100.0,
                                      "weight": 1.0,
                                      "healthy": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations.api-1").isNumber())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void localProfileKeepsReadOnlyTrainingApiPublicForDemos() throws Exception {
        mockMvc.perform(get("/api/evidence-training/onboarding"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/evidence-training/templates"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }
}
