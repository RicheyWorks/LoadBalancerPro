package com.richmond423.loadbalancerpro.api;

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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "loadbalancerpro.auth.mode=api-key",
        "loadbalancerpro.api.key=TEST_DEFAULT_API_KEY"
})
@AutoConfigureMockMvc
class DefaultApiKeyProtectionTest {
    private static final String API_KEY = "TEST_DEFAULT_API_KEY";
    private static final String REQUEST_BODY = """
            {
              "requestedLoad": 10.0,
              "servers": [{
                "id": "api-1",
                "cpuUsage": 10.0,
                "memoryUsage": 20.0,
                "diskUsage": 30.0,
                "capacity": 100.0,
                "weight": 1.0,
                "healthy": true
              }]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void defaultProfileApiKeyModeProtectsApiWithoutProfileGate() throws Exception {
        mockMvc.perform(allocationRequest())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));

        mockMvc.perform(allocationRequest().header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations.api-1").isNumber());
    }

    @Test
    void defaultProfileApiKeyModeKeepsOnlyApiHealthPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")));

        mockMvc.perform(get("/api/evidence-training/onboarding"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path", is("/api/evidence-training/onboarding")));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder allocationRequest() {
        return post("/api/allocate/capacity-aware")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY);
    }
}
