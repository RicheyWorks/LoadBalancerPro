package com.richmond423.loadbalancerpro.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReverseProxyDisabledTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void proxyModeIsDisabledByDefault() throws Exception {
        mockMvc.perform(get("/proxy/anything"))
                .andExpect(status().isNotFound());
    }

    @Test
    void proxyStatusReportsDisabledDefaults() throws Exception {
        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proxyEnabled").value(false))
                .andExpect(jsonPath("$.strategy").value("ROUND_ROBIN"))
                .andExpect(jsonPath("$.healthCheck.enabled").value(false))
                .andExpect(jsonPath("$.healthCheck.path").value("/health"))
                .andExpect(jsonPath("$.retry.enabled").value(false))
                .andExpect(jsonPath("$.retry.maxAttempts").value(2))
                .andExpect(jsonPath("$.retry.retryNonIdempotent").value(false))
                .andExpect(jsonPath("$.cooldown.enabled").value(false))
                .andExpect(jsonPath("$.cooldown.consecutiveFailureThreshold").value(2))
                .andExpect(jsonPath("$.metrics.totalForwarded").value(0))
                .andExpect(jsonPath("$.metrics.totalFailures").value(0))
                .andExpect(jsonPath("$.observability.routeCount").value(0))
                .andExpect(jsonPath("$.observability.backendTargetCount").value(0))
                .andExpect(jsonPath("$.observability.effectiveHealthyBackendCount").value(0))
                .andExpect(jsonPath("$.observability.cooldownActiveBackendCount").value(0))
                .andExpect(jsonPath("$.observability.readiness").value("proxy_disabled"))
                .andExpect(jsonPath("$.securityBoundary.authMode").value("api-key"))
                .andExpect(jsonPath("$.securityBoundary.proxyStatusProtected").value(false))
                .andExpect(jsonPath("$.securityBoundary.proxyForwardingProtected").value(false));
    }
}
