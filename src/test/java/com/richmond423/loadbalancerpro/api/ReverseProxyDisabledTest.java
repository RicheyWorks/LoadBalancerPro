package com.richmond423.loadbalancerpro.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
