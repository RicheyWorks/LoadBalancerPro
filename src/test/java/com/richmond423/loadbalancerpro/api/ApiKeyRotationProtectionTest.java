package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
        "loadbalancerpro.auth.mode=api-key",
        "loadbalancerpro.api.key=TEST_ROTATION_PRIMARY_KEY",
        "loadbalancerpro.api.rotation-key=TEST_ROTATION_OVERLAP_KEY"
})
@AutoConfigureMockMvc
class ApiKeyRotationProtectionTest {
    private static final String PRIMARY_KEY = "TEST_ROTATION_PRIMARY_KEY";
    private static final String ROTATION_KEY = "TEST_ROTATION_OVERLAP_KEY";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void boundedOverlapAcceptsPrimaryAndRotationKeysWithoutExposingEither() throws Exception {
        for (String apiKey : new String[] {PRIMARY_KEY, ROTATION_KEY}) {
            mockMvc.perform(get("/api/proxy/status").header("X-API-Key", apiKey))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString(PRIMARY_KEY))))
                    .andExpect(content().string(not(containsString(ROTATION_KEY))))
                    .andExpect(jsonPath("$.securityBoundary.apiKeyConfigured", is(true)));
        }
    }

    @Test
    void rotationKeyAlsoPassesTheIndependentProxyAdministrationCheck() throws Exception {
        mockMvc.perform(post("/api/proxy/reload")
                        .header("X-API-Key", ROTATION_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is("unsupported")))
                .andExpect(content().string(not(containsString(PRIMARY_KEY))))
                .andExpect(content().string(not(containsString(ROTATION_KEY))));
    }

    @Test
    void missingUnknownAndWhitespaceKeysRemainDenied() throws Exception {
        for (String apiKey : new String[] {"", " ", "TEST_UNKNOWN_API_KEY"}) {
            mockMvc.perform(get("/api/proxy/status").header("X-API-Key", apiKey))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isUnauthorized());
    }
}
