package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.profiles.active=prod",
        "loadbalancerpro.api.key=TEST_PROD_API_KEY"
})
@AutoConfigureMockMvc
class EnterpriseLabProdApiKeyProtectionTest {
    private static final String API_KEY = "TEST_PROD_API_KEY";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prodApiKeyModeProtectsLabScenarioCatalog() throws Exception {
        mockMvc.perform(get("/api/lab/scenarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path", is("/api/lab/scenarios")));

        mockMvc.perform(get("/api/lab/scenarios").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(10)));
    }

    @Test
    void prodApiKeyModeProtectsLabRunCreationAndRetrieval() throws Exception {
        String body = "{\"mode\":\"shadow\",\"scenarioIds\":[\"normal-balanced-load\"]}";

        mockMvc.perform(post("/api/lab/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path", is("/api/lab/runs")));

        mockMvc.perform(post("/api/lab/runs")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId", is("lab-run-0001")));

        mockMvc.perform(get("/api/lab/runs/lab-run-0001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path", is("/api/lab/runs/lab-run-0001")));

        mockMvc.perform(get("/api/lab/runs/lab-run-0001").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId", is("lab-run-0001")));
    }

    @Test
    void prodApiKeyModeProtectsPolicyStatusAndAuditEvents() throws Exception {
        mockMvc.perform(get("/api/lab/policy"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path", is("/api/lab/policy")));

        mockMvc.perform(get("/api/lab/policy").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMode", is("off")));

        mockMvc.perform(get("/api/lab/audit-events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path", is("/api/lab/audit-events")));

        mockMvc.perform(get("/api/lab/audit-events").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageMode", is("process-local bounded audit log")));
    }

    @Test
    void prodApiKeyModeProtectsLabMetricsEndpoints() throws Exception {
        mockMvc.perform(get("/api/lab/metrics"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path", is("/api/lab/metrics")));

        mockMvc.perform(get("/api/lab/metrics").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warning", containsString("lab-grade")));

        mockMvc.perform(get("/api/lab/metrics/prometheus"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path", is("/api/lab/metrics/prometheus")));

        mockMvc.perform(get("/api/lab/metrics/prometheus").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("loadbalancerpro_lab_runs_total")));
    }
}

