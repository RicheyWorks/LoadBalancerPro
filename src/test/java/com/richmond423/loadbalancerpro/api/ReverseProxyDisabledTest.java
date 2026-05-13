package com.richmond423.loadbalancerpro.api;

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
                .andExpect(jsonPath("$.securityBoundary.proxyForwardingProtected").value(false))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.liveValidationEnabled").value(false))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.operatorApproved").value(false))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.configValidationEnabled").value(false))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.proxyEnabled").value(false))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.gateStatus").value("NOT_ENABLED"))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.allowedByGate").value(false))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.trafficExecuted").value(false))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.trafficExecution")
                        .value("traffic not executed by this report"))
                .andExpect(jsonPath("$.privateNetworkLiveValidation.reasonCodes[0]")
                        .value("LIVE_VALIDATION_DISABLED"))
                .andExpect(jsonPath("$.reload.configReloadSupported").value(false))
                .andExpect(jsonPath("$.reload.activeConfigGeneration").value(0))
                .andExpect(jsonPath("$.reload.lastReloadStatus").value("unsupported"));
    }

    @Test
    void privateNetworkLiveValidationCommandReportsBlockedDefaultsWithoutExecution() throws Exception {
        mockMvc.perform(post("/api/proxy/private-network-live-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestPath\":\"/health\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.executable").value(false))
                .andExpect(jsonPath("$.trafficExecuted").value(false))
                .andExpect(jsonPath("$.status").value("BLOCKED_BY_GATE"))
                .andExpect(jsonPath("$.message")
                        .value("private-network live validation command is blocked by the offline gate; "
                                + "traffic execution is not wired in this release"))
                .andExpect(jsonPath("$.requestPath").value("/health"))
                .andExpect(jsonPath("$.evidenceWritten").value(false))
                .andExpect(jsonPath("$.gate.gateStatus").value("NOT_ENABLED"))
                .andExpect(jsonPath("$.reasonCodes[0]").value("LIVE_VALIDATION_DISABLED"));
    }
}
