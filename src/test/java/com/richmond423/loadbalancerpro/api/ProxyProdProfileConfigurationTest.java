package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.profiles.active=prod,proxy-prod",
        "loadbalancerpro.api.key=TEST_PROXY_PROD_API_KEY",
        "LBP_UPSTREAM_0_URL=http://127.0.0.1:18081",
        "LBP_UPSTREAM_1_URL=http://127.0.0.1:18082"
})
@AutoConfigureMockMvc
class ProxyProdProfileConfigurationTest {
    private static final String API_KEY = "TEST_PROXY_PROD_API_KEY";

    @Autowired
    private Environment environment;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void profileEnablesTheBoundedProxyDeploymentDefaults() {
        assertEquals("api-key", environment.getProperty("loadbalancerpro.auth.mode"));
        assertEquals("true", environment.getProperty("loadbalancerpro.auth.protect-actuator"));
        assertEquals("true", environment.getProperty("loadbalancerpro.proxy.enabled"));
        assertEquals("true", environment.getProperty("loadbalancerpro.proxy.health-check.enabled"));
        assertEquals("true", environment.getProperty("loadbalancerpro.proxy.cooldown.enabled"));
        assertEquals("100", environment.getProperty("loadbalancerpro.proxy.limits.max-in-flight"));
        assertEquals("http://127.0.0.1:18081",
                environment.getProperty("loadbalancerpro.proxy.upstreams[0].url"));
        assertEquals("http://127.0.0.1:18082",
                environment.getProperty("loadbalancerpro.proxy.upstreams[1].url"));
        assertEquals("true", environment.getProperty("management.prometheus.metrics.export.enabled"));
    }

    @Test
    void profileKeepsHealthPublicButProtectsProxyStatusAndActuator() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")));

        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/proxy/status").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proxyEnabled", is(true)));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_")));
    }
}
