package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "loadbalancerpro.auth.mode=none")
@AutoConfigureMockMvc
class DefaultActuatorExposureTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Test
    void defaultProfileExposesOnlyHealthAndInfo() throws Exception {
        assertEquals("health,info", environment.getProperty("management.endpoints.web.exposure.include"));
        assertEquals("false", environment.getProperty("management.prometheus.metrics.export.enabled"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isNotFound());
    }
}
