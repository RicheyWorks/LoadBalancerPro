package com.richmond423.loadbalancerpro.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "loadbalancerpro.auth.mode=none",
        "loadbalancerpro.api.key=TEST_ROUTE_STRATEGY_RELOAD_KEY"
})
@AutoConfigureMockMvc
@DirtiesContext
class ReverseProxyRouteStrategyReloadIntegrationTest {
    private static final String API_KEY = "TEST_ROUTE_STRATEGY_RELOAD_KEY";
    private static final ProxyStrategyDemoUpstream PRIMARY =
            ProxyStrategyDemoUpstream.start("reload-primary", 200);
    private static final ProxyStrategyDemoUpstream SECONDARY =
            ProxyStrategyDemoUpstream.start("reload-secondary", 200);

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.strategy", () -> "WEIGHTED_ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.routes.api.path-prefix", () -> "/api");
        registry.add("loadbalancerpro.proxy.routes.api.strategy", () -> "WEIGHTED_ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.routes.api.targets[0].id", () -> "reload-primary");
        registry.add("loadbalancerpro.proxy.routes.api.targets[0].url", PRIMARY::baseUrl);
        registry.add("loadbalancerpro.proxy.routes.api.targets[0].weight", () -> "3.0");
        registry.add("loadbalancerpro.proxy.routes.api.targets[1].id", () -> "reload-secondary");
        registry.add("loadbalancerpro.proxy.routes.api.targets[1].url", SECONDARY::baseUrl);
        registry.add("loadbalancerpro.proxy.routes.api.targets[1].weight", () -> "1.0");
    }

    @AfterAll
    static void stopUpstreams() {
        PRIMARY.stop();
        SECONDARY.stop();
    }

    @Test
    void unchangedReloadPreservesSmoothWeightedRoundRobinProgress() throws Exception {
        expectUpstream("/proxy/api/first", "reload-primary");
        expectUpstream("/proxy/api/second", "reload-primary");

        mockMvc.perform(post("/api/proxy/reload")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unchangedRouteBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.activeConfigGeneration").value(2));

        expectUpstream("/proxy/api/third", "reload-secondary");
    }

    private void expectUpstream(String path, String upstreamId) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", upstreamId))
                .andExpect(header().string("X-LoadBalancerPro-Strategy", "WEIGHTED_ROUND_ROBIN"));
    }

    private static String unchangedRouteBody() {
        return """
                {
                  "enabled": true,
                  "strategy": "WEIGHTED_ROUND_ROBIN",
                  "routes": {
                    "api": {
                      "pathPrefix": "/api",
                      "strategy": "WEIGHTED_ROUND_ROBIN",
                      "targets": [
                        {
                          "id": "reload-primary",
                          "url": "%s",
                          "healthy": true,
                          "weight": 3.0
                        },
                        {
                          "id": "reload-secondary",
                          "url": "%s",
                          "healthy": true,
                          "weight": 1.0
                        }
                      ]
                    }
                  }
                }
                """.formatted(PRIMARY.baseUrl(), SECONDARY.baseUrl());
    }
}
