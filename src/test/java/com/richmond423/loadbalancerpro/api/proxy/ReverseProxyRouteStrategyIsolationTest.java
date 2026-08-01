package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.richmond423.loadbalancerpro.core.RoutingDecision;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import com.richmond423.loadbalancerpro.core.ServerStateVector;
import com.richmond423.loadbalancerpro.core.WeightedRoundRobinRoutingStrategy;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class ReverseProxyRouteStrategyIsolationTest {
    private static final int REQUESTS_PER_ROUTE = 1_000;
    private static final int MAX_PRIMARY_DEVIATION = 50;
    private static final Instant TIMESTAMP = Instant.parse("2026-07-29T00:00:00Z");

    @Test
    void interleavedWeightedRoutesKeepIndependentThreeToOneDistributions() {
        List<ReverseProxyRoutePlanner.ConfiguredRoute> routes =
                ReverseProxyRoutePlanner.buildEnabledRoutes(
                        twoWeightedRoutes(), RoutingStrategyRegistry.defaultRegistry());
        ReverseProxyRoutePlanner.ConfiguredRoute alpha = routes.get(0);
        ReverseProxyRoutePlanner.ConfiguredRoute beta = routes.get(1);

        assertNotSame(alpha.strategy(), beta.strategy(),
                "each configured route must own its routing strategy instance");

        Map<String, Integer> alphaSelections = new LinkedHashMap<>();
        Map<String, Integer> betaSelections = new LinkedHashMap<>();
        List<ServerStateVector> candidates = List.of(
                candidate("primary", 3.0),
                candidate("secondary", 1.0));

        for (int request = 0; request < REQUESTS_PER_ROUTE; request++) {
            recordSelection(alphaSelections, alpha.strategy().choose(candidates));
            recordSelection(betaSelections, beta.strategy().choose(candidates));
        }

        assertThreeToOne(alpha.name(), alphaSelections);
        assertThreeToOne(beta.name(), betaSelections);
    }

    @Test
    void serviceForwardsInterleavedRoutesWithIndependentThreeToOneDistributions() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<InputStream> upstreamResponse = mock(HttpResponse.class);
        when(upstreamResponse.statusCode()).thenReturn(200);
        when(upstreamResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
        when(upstreamResponse.body()).thenAnswer(ignored -> new ByteArrayInputStream(new byte[0]));
        when(httpClient.send(
                any(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(upstreamResponse);
        ReverseProxyService service = new ReverseProxyService(
                twoWeightedRoutes(),
                httpClient,
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.fixed(TIMESTAMP, ZoneOffset.UTC));
        HttpServletRequest alphaRequest = request("/proxy/alpha/acceptance");
        HttpServletRequest betaRequest = request("/proxy/beta/acceptance");
        Map<String, Integer> alphaSelections = new LinkedHashMap<>();
        Map<String, Integer> betaSelections = new LinkedHashMap<>();

        for (int request = 0; request < REQUESTS_PER_ROUTE; request++) {
            recordSelection(alphaSelections, service.forward(alphaRequest, new byte[0]));
            recordSelection(betaSelections, service.forward(betaRequest, new byte[0]));
        }

        assertThreeToOne("alpha", alphaSelections);
        assertThreeToOne("beta", betaSelections);
    }

    @Test
    void reloadCarriesOnlyUnchangedRouteStrategyInstances() {
        RoutingStrategyRegistry registry = RoutingStrategyRegistry.defaultRegistry();
        List<ReverseProxyRoutePlanner.ConfiguredRoute> initial =
                ReverseProxyRoutePlanner.buildEnabledRoutes(twoWeightedRoutes(), registry);

        List<ReverseProxyRoutePlanner.ConfiguredRoute> unchanged =
                ReverseProxyRoutePlanner.buildEnabledRoutes(twoWeightedRoutes(), registry, initial);
        assertSame(initial.get(0).strategy(), unchanged.get(0).strategy());
        assertSame(initial.get(1).strategy(), unchanged.get(1).strategy());

        ReverseProxyProperties reordered = twoWeightedRoutes();
        reordered.getRoutes().get("alpha").setTargets(List.of(
                upstream("secondary", "http://127.0.0.1:18082", 1.0),
                upstream("primary", "http://127.0.0.1:18081", 3.0)));
        List<ReverseProxyRoutePlanner.ConfiguredRoute> afterReorder =
                ReverseProxyRoutePlanner.buildEnabledRoutes(reordered, registry, unchanged);
        assertSame(unchanged.get(0).strategy(), afterReorder.get(0).strategy(),
                "upstream identity is a set, so order-only changes preserve strategy state");

        ReverseProxyProperties strategyChanged = twoWeightedRoutes();
        strategyChanged.getRoutes().get("beta").setStrategy("ROUND_ROBIN");
        List<ReverseProxyRoutePlanner.ConfiguredRoute> afterStrategyChange =
                ReverseProxyRoutePlanner.buildEnabledRoutes(strategyChanged, registry, afterReorder);
        assertSame(afterReorder.get(0).strategy(), afterStrategyChange.get(0).strategy());
        assertNotSame(afterReorder.get(1).strategy(), afterStrategyChange.get(1).strategy());

        ReverseProxyProperties upstreamSetChanged = twoWeightedRoutes();
        upstreamSetChanged.getRoutes().get("alpha").setTargets(List.of(
                upstream("primary", "http://127.0.0.1:18081", 3.0),
                upstream("replacement", "http://127.0.0.1:18083", 1.0)));
        List<ReverseProxyRoutePlanner.ConfiguredRoute> afterUpstreamSetChange =
                ReverseProxyRoutePlanner.buildEnabledRoutes(
                        upstreamSetChanged, registry, afterStrategyChange);
        assertNotSame(afterStrategyChange.get(0).strategy(), afterUpstreamSetChange.get(0).strategy());
        assertNotSame(afterStrategyChange.get(1).strategy(), afterUpstreamSetChange.get(1).strategy(),
                "beta changed back from round-robin to weighted round-robin");
    }

    @Test
    void plannerResolvesRouteTimeoutOverrideAndGlobalFallback() {
        ReverseProxyProperties properties = twoWeightedRoutes();
        properties.setRequestTimeout(Duration.ofSeconds(2));
        properties.getRoutes().get("alpha").setRequestTimeout(Duration.ofMillis(125));

        List<ReverseProxyRoutePlanner.ConfiguredRoute> routes =
                ReverseProxyRoutePlanner.buildEnabledRoutes(
                        properties, RoutingStrategyRegistry.defaultRegistry());

        assertEquals(Duration.ofMillis(125), routes.get(0).requestTimeout());
        assertEquals(Duration.ofSeconds(2), routes.get(1).requestTimeout());
    }

    @Test
    void plannerRejectsFactoryThatSharesOneInstanceAcrossRoutes() {
        RoutingStrategyRegistry singletonRegistry = new RoutingStrategyRegistry(List.of(
                new WeightedRoundRobinRoutingStrategy()));

        assertThrows(
                IllegalStateException.class,
                () -> ReverseProxyRoutePlanner.buildEnabledRoutes(twoWeightedRoutes(), singletonRegistry));
    }

    private static ReverseProxyProperties twoWeightedRoutes() {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setStrategy("WEIGHTED_ROUND_ROBIN");
        Map<String, ReverseProxyProperties.Route> routes = new LinkedHashMap<>();
        routes.put("alpha", route("/alpha"));
        routes.put("beta", route("/beta"));
        properties.setRoutes(routes);
        return properties;
    }

    private static ReverseProxyProperties.Route route(String pathPrefix) {
        ReverseProxyProperties.Route route = new ReverseProxyProperties.Route();
        route.setPathPrefix(pathPrefix);
        route.setStrategy("WEIGHTED_ROUND_ROBIN");
        route.setTargets(List.of(
                upstream("primary", "http://127.0.0.1:18081", 3.0),
                upstream("secondary", "http://127.0.0.1:18082", 1.0)));
        return route;
    }

    private static ReverseProxyProperties.Upstream upstream(String id, String url, double weight) {
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId(id);
        upstream.setUrl(url);
        upstream.setWeight(weight);
        return upstream;
    }

    private static ServerStateVector candidate(String id, double weight) {
        return new ServerStateVector(
                id,
                true,
                0,
                OptionalDouble.of(100.0),
                OptionalDouble.of(100.0),
                weight,
                1.0,
                1.0,
                1.0,
                0.0,
                OptionalInt.of(0),
                TIMESTAMP);
    }

    private static void recordSelection(Map<String, Integer> selections, RoutingDecision decision) {
        String selectedId = decision.explanation().chosenServerId().orElseThrow();
        selections.merge(selectedId, 1, Integer::sum);
    }

    private static void recordSelection(
            Map<String, Integer> selections,
            ReverseProxyResponse response) {
        String selectedId = response.headers().getFirst("X-LoadBalancerPro-Upstream");
        selections.merge(selectedId, 1, Integer::sum);
    }

    private static HttpServletRequest request(String requestUri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn(requestUri);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getQueryString()).thenReturn(null);
        return request;
    }

    private static void assertThreeToOne(String routeName, Map<String, Integer> selections) {
        int primary = selections.getOrDefault("primary", 0);
        int secondary = selections.getOrDefault("secondary", 0);
        assertEquals(REQUESTS_PER_ROUTE, primary + secondary, routeName + " selection count");
        assertTrue(Math.abs(750 - primary) <= MAX_PRIMARY_DEVIATION,
                () -> routeName + " primary selections must remain within five percentage points of 75%: "
                        + selections);
        assertTrue(Math.abs(250 - secondary) <= MAX_PRIMARY_DEVIATION,
                () -> routeName + " secondary selections must remain within five percentage points of 25%: "
                        + selections);
    }
}
