package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class ReverseProxySecurityObservabilityTest {
    private static final String API_KEY = "TEST_PROD_API_KEY";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prodApiKeyBoundaryIsVisibleWithoutExposingSecretValues() throws Exception {
        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.path", is("/api/proxy/status")));

        mockMvc.perform(get("/api/proxy/status").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(API_KEY))))
                .andExpect(jsonPath("$.proxyEnabled", is(false)))
                .andExpect(jsonPath("$.securityBoundary.authMode", is("api-key")))
                .andExpect(jsonPath("$.securityBoundary.apiKeyConfigured", is(true)))
                .andExpect(jsonPath("$.securityBoundary.proxyStatusProtected", is(true)))
                .andExpect(jsonPath("$.securityBoundary.proxyForwardingProtected", is(true)))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.gateStatus", is("NOT_ENABLED")))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.allowedByGate", is(false)))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.trafficExecuted", is(false)))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.trafficExecution",
                        is("traffic not executed by this report")));
    }
}
