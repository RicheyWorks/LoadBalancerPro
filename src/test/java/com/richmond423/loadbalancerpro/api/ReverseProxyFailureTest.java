package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReverseProxyFailureTest {
    private static final int UNUSED_LOCAL_PORT = unusedLocalPort();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.strategy", () -> "ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.upstreams[0].id", () -> "downstream-offline");
        registry.add("loadbalancerpro.proxy.upstreams[0].url",
                () -> "http://127.0.0.1:" + UNUSED_LOCAL_PORT);
        registry.add("loadbalancerpro.proxy.upstreams[0].healthy", () -> "true");
    }

    @Test
    void failingLocalUpstreamReturnsDeterministicBadGateway() throws Exception {
        mockMvc.perform(get("/proxy/offline"))
                .andExpect(status().isBadGateway())
                .andExpect(content().string(containsString("\"error\":\"proxy_upstream_failure\"")))
                .andExpect(content().string(containsString(
                        "\"message\":\"Proxy could not reach upstream downstream-offline\"")));
    }

    private static int unusedLocalPort() {
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to allocate unused local test port", exception);
        }
    }
}
