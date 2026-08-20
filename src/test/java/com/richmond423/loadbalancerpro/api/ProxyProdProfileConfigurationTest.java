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

import io.micrometer.core.instrument.MeterRegistry;

@SpringBootTest(properties = {
        "spring.profiles.active=prod,proxy-prod",
        "loadbalancerpro.api.key=TEST_PROXY_PROD_API_KEY",
        "LBP_UPSTREAM_0_URL=http://127.0.0.1:18081",
        "LBP_UPSTREAM_1_URL=http://127.0.0.1:18082",
        "LBP_RETRY_ENABLED=true",
        "LBP_RETRY_MAX_ATTEMPTS=2",
        "LBP_RETRY_BUDGET_PERCENT=100",
        "LBP_RETRY_BACKOFF_BASE=10ms",
        "LBP_RETRY_BACKOFF_MAX=50ms",
        "LBP_RETRY_NON_IDEMPOTENT=false",
        "LBP_RETRY_METHODS=GET,HEAD",
        "LBP_RETRY_STATUSES=502,503,504"
})
@AutoConfigureMockMvc
class ProxyProdProfileConfigurationTest {
    private static final String API_KEY = "TEST_PROXY_PROD_API_KEY";

    @Autowired
    private Environment environment;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void profileEnablesTheBoundedProxyDeploymentDefaults() {
        assertEquals("api-key", environment.getProperty("loadbalancerpro.auth.mode"));
        assertEquals("true", environment.getProperty("loadbalancerpro.auth.protect-actuator"));
        assertEquals("true", environment.getProperty("loadbalancerpro.proxy.enabled"));
        assertEquals("true", environment.getProperty("server.http2.enabled"));
        assertEquals("false", environment.getProperty("loadbalancerpro.proxy.websocket.enabled"));
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
                .andExpect(jsonPath("$.proxyEnabled", is(true)))
                .andExpect(jsonPath("$.retry.enabled", is(true)))
                .andExpect(jsonPath("$.retry.maxAttempts", is(2)))
                .andExpect(jsonPath("$.retry.budgetPercent", is(100)))
                .andExpect(jsonPath("$.retry.retryNonIdempotent", is(false)));

        double proxyRequestsBefore = meterRegistry.find("lbp.proxy.requests").counters().stream()
                .mapToDouble(io.micrometer.core.instrument.Counter::count)
                .sum();
        mockMvc.perform(get("/proxy/sensitive/path?requestId=private-request")
                        .header("User-Agent", "private-user-agent"))
                .andExpect(status().isUnauthorized());
        assertEquals(proxyRequestsBefore,
                meterRegistry.find("lbp.proxy.requests").counters().stream()
                        .mapToDouble(io.micrometer.core.instrument.Counter::count)
                        .sum());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_")))
                .andExpect(content().string(containsString("lbp_proxy_inflight")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("private-request"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("private-user-agent"))));
        assertEquals(proxyRequestsBefore,
                meterRegistry.find("lbp.proxy.requests").counters().stream()
                        .mapToDouble(io.micrometer.core.instrument.Counter::count)
                        .sum(),
                "health, status, authentication failures, and actuator scrapes are not proxy traffic");

        java.util.Set<String> proxyMeterNames = meterRegistry.getMeters().stream()
                .map(meter -> meter.getId().getName())
                .filter(name -> name.startsWith("lbp.proxy."))
                .collect(java.util.stream.Collectors.toSet());
        org.junit.jupiter.api.Assertions.assertTrue(proxyMeterNames.containsAll(java.util.Set.of(
                "lbp.proxy.requests",
                "lbp.proxy.latency",
                "lbp.proxy.inflight",
                "lbp.proxy.attempts",
                "lbp.proxy.retries",
                "lbp.proxy.request.bytes",
                "lbp.proxy.response.bytes",
                "lbp.proxy.limit.rejections",
                "lbp.proxy.sheds",
                "lbp.proxy.health",
                "lbp.proxy.cooldown.trips")));
    }
}
