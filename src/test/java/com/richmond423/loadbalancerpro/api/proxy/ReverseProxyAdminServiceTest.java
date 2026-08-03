package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import org.junit.jupiter.api.Test;

class ReverseProxyAdminServiceTest {

    @Test
    void namedRouteMutationsReuseTheValidatedGenerationSwapAndRemainRedacted() throws Exception {
        ReverseProxyService service = service(namedRouteProperties());
        try {
            ReverseProxyAdminMutationResponse added = service.addUpstream(
                    new ReverseProxyUpstreamAddRequest(
                            1L, "api", "beta", "http://127.0.0.1:18082", true, 1.0, 7));
            assertTrue(added.success());
            assertEquals(2, added.generation());
            assertEquals(2, added.config().backendTargetCount());
            assertEquals(List.of("alpha", "beta"), added.config().routes().get(0).upstreams().stream()
                    .map(ReverseProxyAdminConfigResponse.UpstreamConfig::id)
                    .toList());
            String serialized = new ObjectMapper().writeValueAsString(added);
            assertFalse(serialized.contains("127.0.0.1"));
            assertFalse(serialized.contains("url"));

            ReverseProxyAdminMutationResponse patched = service.patchUpstream(
                    "beta", new ReverseProxyUpstreamPatchRequest(2L, 2.0, false, false));
            assertTrue(patched.success());
            assertEquals(3, patched.generation());
            ReverseProxyAdminConfigResponse.UpstreamConfig beta = patched.config().routes().get(0)
                    .upstreams().get(1);
            assertEquals(2.0, beta.weight());
            assertFalse(beta.healthy());
            assertFalse(beta.draining());

            ReverseProxyAdminMutationResponse stale = service.patchUpstream(
                    "beta", new ReverseProxyUpstreamPatchRequest(2L, null, true, null));
            assertFalse(stale.success());
            assertEquals("generation_conflict", stale.status());
            assertEquals(3, stale.generation());

            ReverseProxyAdminMutationResponse deleted = service.deleteUpstream("beta", 3L);
            assertTrue(deleted.success());
            assertEquals(4, deleted.generation());
            assertEquals(1, deleted.config().backendTargetCount());
            assertEquals("alpha", deleted.config().routes().get(0).upstreams().get(0).id());
        } finally {
            service.stop();
        }
    }

    private static ReverseProxyService service(ReverseProxyProperties properties) {
        return new ReverseProxyService(
                properties,
                mock(HttpClient.class),
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.systemUTC());
    }

    private static ReverseProxyProperties namedRouteProperties() {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setStrategy("ROUND_ROBIN");
        ReverseProxyProperties.Route route = new ReverseProxyProperties.Route();
        route.setPathPrefix("/api");
        route.setTargets(List.of(upstream("alpha", "http://127.0.0.1:18081")));
        Map<String, ReverseProxyProperties.Route> routes = new LinkedHashMap<>();
        routes.put("api", route);
        properties.setRoutes(routes);
        return properties;
    }

    private static ReverseProxyProperties.Upstream upstream(String id, String url) {
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId(id);
        upstream.setUrl(url);
        upstream.setHealthy(true);
        upstream.setWeight(1.0);
        return upstream;
    }
}
