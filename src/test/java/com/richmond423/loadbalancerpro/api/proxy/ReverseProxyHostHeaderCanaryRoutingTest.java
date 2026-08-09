package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class ReverseProxyHostHeaderCanaryRoutingTest {
    private static final Instant NOW = Instant.parse("2026-08-09T00:00:00Z");

    @Test
    void hostThenPathThenHeaderSpecificityAndRouteNameDefineTotalPrecedence() throws Exception {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        Map<String, ReverseProxyProperties.Route> routes = new LinkedHashMap<>();
        routes.put("path-only", route("/api/deep", upstream("path", 18081)));
        routes.put("host", matchedRoute("/api", "API.Example.", Map.of(), upstream("host", 18082)));
        routes.put("header", matchedRoute(
                "/api", "api.example", Map.of("X-Channel", "canary"), upstream("header", 18083)));
        properties.setRoutes(routes);
        ReverseProxyService service = service(properties, successClient());
        try {
            assertEquals("header", selected(service,
                    request("/proxy/api/deep/item", "api.example:8443", Map.of("x-channel", "canary"), "client-a")));
            assertEquals("host", selected(service,
                    request("/proxy/api/deep/item", "Api.Example", Map.of(), "client-b")));
            assertEquals("path", selected(service,
                    request("/proxy/api/deep/item", "other.example", Map.of(), "client-c")));
            assertEquals("host", selected(service,
                    request("/proxy/api/deep/item", "api.example", Map.of("x-channel", "CANARY"), "client-d")));
        } finally {
            service.closeHealthProber();
        }

        ReverseProxyProperties tied = new ReverseProxyProperties();
        tied.setEnabled(true);
        Map<String, ReverseProxyProperties.Route> tiedRoutes = new LinkedHashMap<>();
        tiedRoutes.put("zeta", route("/tie", upstream("zeta", 18084)));
        tiedRoutes.put("alpha", route("/tie", upstream("alpha", 18085)));
        tied.setRoutes(tiedRoutes);
        ReverseProxyService tiedService = service(tied, successClient());
        try {
            assertEquals("alpha", selected(tiedService,
                    request("/proxy/tie/value", "tie.example", Map.of(), "client-e")));
        } finally {
            tiedService.closeHealthProber();
        }
    }

    @Test
    void invalidRequestHostCannotSelectHostSpecificRoute() throws Exception {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        Map<String, ReverseProxyProperties.Route> routes = new LinkedHashMap<>();
        routes.put("fallback", route("/", upstream("fallback", 18081)));
        routes.put("host", matchedRoute("/", "api.example", Map.of(), upstream("host", 18082)));
        properties.setRoutes(routes);
        ReverseProxyService service = service(properties, successClient());
        try {
            assertEquals("fallback", selected(service,
                    request("/proxy/value", "api.example/path", Map.of(), "client-a")));
            assertEquals("fallback", selected(service,
                    request("/proxy/value", null, Map.of(), "client-b")));
        } finally {
            service.closeHealthProber();
        }
    }

    @Test
    void splitAssignmentIsStableAndTracksConfiguredNinetyTenBoundary() {
        ReverseProxyProperties properties = splitProperties();
        ReverseProxyRoutePlanner.ConfiguredRoute route = ReverseProxyRoutePlanner.buildEnabledRoutes(
                properties, RoutingStrategyRegistry.defaultRegistry()).get(0);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int index = 0; index < 10_000; index++) {
            String key = "client-" + index;
            String first = route.splitFor(key).orElseThrow().name();
            String second = route.splitFor(key).orElseThrow().name();
            assertEquals(first, second);
            counts.merge(first, 1, Integer::sum);
        }
        assertTrue(counts.getOrDefault("stable", 0) >= 8_700, () -> counts.toString());
        assertTrue(counts.getOrDefault("stable", 0) <= 9_300, () -> counts.toString());
        assertEquals(10_000, counts.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void retriesRemainInsideTheAssignedSplitGroup() throws Exception {
        ReverseProxyProperties properties = splitProperties();
        properties.getRetry().setEnabled(true);
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setBudgetPercent(100);
        properties.getRetry().getBackoff().setBase(Duration.ofMillis(1));
        properties.getRetry().getBackoff().setMax(Duration.ofMillis(1));
        ReverseProxyRoutePlanner.ConfiguredRoute planned = ReverseProxyRoutePlanner.buildEnabledRoutes(
                properties, RoutingStrategyRegistry.defaultRegistry()).get(0);
        String stableKey = keyFor(planned, "stable");
        List<URI> attemptedUris = new ArrayList<>();
        AtomicInteger attempts = new AtomicInteger();
        HttpClient client = client(request -> {
            attemptedUris.add(request.uri());
            return response(attempts.getAndIncrement() == 0 ? 503 : 200);
        });
        ReverseProxyService service = service(properties, client);
        try {
            ReverseProxyResponse response = service.forward(
                    request("/proxy/api/retry", "api.example", Map.of(), stableKey), new byte[0]);
            assertEquals(200, response.statusCode());
            assertEquals("stable-b", response.headers().getFirst("X-LoadBalancerPro-Upstream"));
            assertEquals(List.of(18081, 18082), attemptedUris.stream().map(URI::getPort).toList());
            assertFalse(attemptedUris.stream().anyMatch(uri -> uri.getPort() == 18083));
        } finally {
            service.closeHealthProber();
        }
    }

    @Test
    void plannerRejectsAmbiguousOrUnsafeSplitAndHeaderConfiguration() {
        assertEquals("2001:db8::1", ReverseProxyRoutePlanner.normalizedHost(
                "[2001:DB8::1]:8443", "test host"));
        assertThrows(IllegalStateException.class, () -> ReverseProxyRoutePlanner.normalizedHost(
                "[::::]:8443", "test host"));

        ReverseProxyProperties wrongTotal = splitProperties();
        wrongTotal.getRoutes().get("api").getSplit().get("canary").setPercentage(9);
        assertRejected(wrongTotal, "percentages must total exactly 100");

        ReverseProxyProperties duplicate = splitProperties();
        duplicate.getRoutes().get("api").getSplit().get("canary")
                .setTargetIds(List.of("stable-a", "canary"));
        assertRejected(duplicate, "exactly one group");

        ReverseProxyProperties unknown = splitProperties();
        unknown.getRoutes().get("api").getSplit().get("canary").setTargetIds(List.of("unknown"));
        assertRejected(unknown, "unknown target id");

        ReverseProxyProperties unassigned = splitProperties();
        ReverseProxyProperties.SplitGroup stable = unassigned.getRoutes().get("api").getSplit().get("stable");
        stable.setPercentage(100);
        stable.setTargetIds(List.of("stable-a"));
        unassigned.getRoutes().get("api").getSplit().remove("canary");
        assertRejected(unassigned, "assign every route target exactly once");

        ReverseProxyProperties sensitive = splitProperties();
        sensitive.getRoutes().get("api").getMatch().setHeader(Map.of("Authorization", "secret"));
        assertRejected(sensitive, "cannot use sensitive, forwarding, or hop-by-hop headers");

        ReverseProxyProperties wildcardHost = splitProperties();
        wildcardHost.getRoutes().get("api").getMatch().setHost("*.example.com");
        assertRejected(wildcardHost, "invalid DNS host");
    }

    @Test
    void unchangedReloadRetainsGroupStrategiesAndMembershipChangeReplacesThem() {
        RoutingStrategyRegistry registry = RoutingStrategyRegistry.defaultRegistry();
        List<ReverseProxyRoutePlanner.ConfiguredRoute> initial = ReverseProxyRoutePlanner.buildEnabledRoutes(
                splitProperties(), registry);
        List<ReverseProxyRoutePlanner.ConfiguredRoute> unchanged = ReverseProxyRoutePlanner.buildEnabledRoutes(
                splitProperties(), registry, initial);
        assertSame(initial.get(0).splits().get(0).strategy(), unchanged.get(0).splits().get(0).strategy());
        assertSame(initial.get(0).splits().get(1).strategy(), unchanged.get(0).splits().get(1).strategy());

        ReverseProxyProperties changed = splitProperties();
        changed.getRoutes().get("api").getSplit().get("stable")
                .setTargetIds(List.of("stable-a", "stable-b", "canary"));
        changed.getRoutes().get("api").getSplit().remove("canary");
        changed.getRoutes().get("api").getSplit().get("stable").setPercentage(100);
        List<ReverseProxyRoutePlanner.ConfiguredRoute> replaced = ReverseProxyRoutePlanner.buildEnabledRoutes(
                changed, registry, unchanged);
        assertNotSame(unchanged.get(0).splits().get(1).strategy(), replaced.get(0).splits().get(0).strategy());
    }

    @Test
    void inFlightRequestRetainsItsSplitSnapshotAcrossReload() throws Exception {
        ReverseProxyProperties initial = splitProperties();
        ReverseProxyProperties changed = splitProperties();
        changed.getRoutes().get("api").getSplit().get("stable").setPercentage(10);
        changed.getRoutes().get("api").getSplit().get("canary").setPercentage(90);
        ReverseProxyRoutePlanner.ConfiguredRoute initialPlan = ReverseProxyRoutePlanner.buildEnabledRoutes(
                initial, RoutingStrategyRegistry.defaultRegistry()).get(0);
        ReverseProxyRoutePlanner.ConfiguredRoute changedPlan = ReverseProxyRoutePlanner.buildEnabledRoutes(
                changed, RoutingStrategyRegistry.defaultRegistry()).get(0);
        String crossingKey = null;
        for (int index = 0; index < 10_000; index++) {
            String candidate = "reload-client-" + index;
            if ("stable".equals(initialPlan.splitFor(candidate).orElseThrow().name())
                    && "canary".equals(changedPlan.splitFor(candidate).orElseThrow().name())) {
                crossingKey = candidate;
                break;
            }
        }
        assertTrue(crossingKey != null, "a deterministic key must cross the changed split boundary");

        CountDownLatch firstAttemptStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstAttempt = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        HttpClient client = client(request -> {
            if (calls.getAndIncrement() == 0) {
                firstAttemptStarted.countDown();
                assertTrue(releaseFirstAttempt.await(5, TimeUnit.SECONDS));
            }
            return response(200);
        });
        ReverseProxyService service = service(initial, client);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            String requestKey = crossingKey;
            Future<ReverseProxyResponse> inFlight = executor.submit(() -> service.forward(
                    request("/proxy/api/reload", "api.example", Map.of(), requestKey), new byte[0]));
            assertTrue(firstAttemptStarted.await(5, TimeUnit.SECONDS));
            assertTrue(service.reload(changed).success());
            releaseFirstAttempt.countDown();
            assertEquals("stable-a", inFlight.get(5, TimeUnit.SECONDS)
                    .headers().getFirst("X-LoadBalancerPro-Upstream"));
            assertEquals("canary", service.forward(
                    request("/proxy/api/reload", "api.example", Map.of(), requestKey), new byte[0])
                    .headers().getFirst("X-LoadBalancerPro-Upstream"));
        } finally {
            releaseFirstAttempt.countDown();
            executor.shutdownNow();
            service.closeHealthProber();
        }
    }

    @Test
    void statusAndAdminResponsesExposeRulesWithoutHeaderValuesAndReloadCopiesThem() throws Exception {
        ReverseProxyProperties properties = splitProperties();
        properties.getRoutes().get("api").getMatch().setHost("API.Example:443");
        properties.getRoutes().get("api").getMatch().setHeader(Map.of("X-Channel", "private-canary-value"));
        ReverseProxyService service = service(properties, successClient());
        try {
            properties.getRoutes().get("api").getMatch().setHost("mutated.example");
            ReverseProxyStatusResponse.RouteStatus status = service.statusSnapshot().routes().get(0);
            ReverseProxyAdminConfigResponse.RouteConfig admin = service.adminConfigSnapshot().routes().get(0);
            assertEquals("api.example", status.hostMatch());
            assertEquals(List.of("x-channel"), status.headerMatchNames());
            assertEquals(List.of("canary", "stable"), status.splits().stream()
                    .map(ReverseProxyStatusResponse.SplitStatus::name).toList());
            assertEquals("api.example", admin.hostMatch());
            assertEquals(List.of("x-channel"), admin.headerMatchNames());
            String json = new ObjectMapper().writeValueAsString(service.adminConfigSnapshot());
            assertFalse(json.contains("private-canary-value"));

            ReverseProxyProperties candidate = splitProperties();
            candidate.getRoutes().get("api").getMatch().setHost("next.example");
            candidate.getRoutes().get("api").getMatch().setHeader(Map.of("X-Channel", "next-private-value"));
            assertTrue(service.reload(candidate).success());
            candidate.getRoutes().get("api").getMatch().setHost("mutated-after-reload.example");
            assertEquals("next.example", service.statusSnapshot().routes().get(0).hostMatch());
            assertFalse(new ObjectMapper().writeValueAsString(service.adminConfigSnapshot())
                    .contains("next-private-value"));
        } finally {
            service.closeHealthProber();
        }
    }

    @Test
    void springBindingSupportsMatchAndNamedSplitGroups() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("loadbalancerpro.proxy.enabled", "true");
        values.put("loadbalancerpro.proxy.routes.api.path-prefix", "/api");
        values.put("loadbalancerpro.proxy.routes.api.match.host", "api.example:8443");
        values.put("loadbalancerpro.proxy.routes.api.match.header.x-channel", "canary");
        values.put("loadbalancerpro.proxy.routes.api.targets[0].id", "stable");
        values.put("loadbalancerpro.proxy.routes.api.targets[0].url", "http://127.0.0.1:18081");
        values.put("loadbalancerpro.proxy.routes.api.targets[1].id", "canary");
        values.put("loadbalancerpro.proxy.routes.api.targets[1].url", "http://127.0.0.1:18082");
        values.put("loadbalancerpro.proxy.routes.api.split.stable.percentage", "90");
        values.put("loadbalancerpro.proxy.routes.api.split.stable.target-ids[0]", "stable");
        values.put("loadbalancerpro.proxy.routes.api.split.canary.percentage", "10");
        values.put("loadbalancerpro.proxy.routes.api.split.canary.target-ids[0]", "canary");
        ReverseProxyProperties properties = new Binder(new MapConfigurationPropertySource(values))
                .bind("loadbalancerpro.proxy", Bindable.of(ReverseProxyProperties.class))
                .orElseThrow(() -> new AssertionError("proxy properties did not bind"));
        ReverseProxyProperties.Route route = properties.getRoutes().get("api");
        assertEquals("api.example:8443", route.getMatch().getHost());
        assertEquals("canary", route.getMatch().getHeader().get("x-channel"));
        assertEquals(90, route.getSplit().get("stable").getPercentage());
        assertEquals(List.of("canary"), route.getSplit().get("canary").getTargetIds());
        assertEquals(List.of("canary", "stable"), ReverseProxyRoutePlanner.buildEnabledRoutes(
                properties, RoutingStrategyRegistry.defaultRegistry()).get(0).splits().stream()
                .map(ReverseProxyRoutePlanner.ConfiguredSplit::name).toList());
    }

    private static ReverseProxyProperties splitProperties() {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        ReverseProxyProperties.Route route = route("/api",
                upstream("stable-a", 18081),
                upstream("stable-b", 18082),
                upstream("canary", 18083));
        ReverseProxyProperties.SplitGroup stable = split(90, "stable-a", "stable-b");
        ReverseProxyProperties.SplitGroup canary = split(10, "canary");
        Map<String, ReverseProxyProperties.SplitGroup> groups = new LinkedHashMap<>();
        groups.put("stable", stable);
        groups.put("canary", canary);
        route.setSplit(groups);
        properties.setRoutes(Map.of("api", route));
        return properties;
    }

    private static ReverseProxyProperties.SplitGroup split(int percentage, String... targetIds) {
        ReverseProxyProperties.SplitGroup split = new ReverseProxyProperties.SplitGroup();
        split.setPercentage(percentage);
        split.setTargetIds(List.of(targetIds));
        return split;
    }

    private static ReverseProxyProperties.Route matchedRoute(
            String path, String host, Map<String, String> headers, ReverseProxyProperties.Upstream... targets) {
        ReverseProxyProperties.Route route = route(path, targets);
        ReverseProxyProperties.Match match = new ReverseProxyProperties.Match();
        match.setHost(host);
        match.setHeader(headers);
        route.setMatch(match);
        return route;
    }

    private static ReverseProxyProperties.Route route(
            String path, ReverseProxyProperties.Upstream... targets) {
        ReverseProxyProperties.Route route = new ReverseProxyProperties.Route();
        route.setPathPrefix(path);
        route.setStrategy("ROUND_ROBIN");
        route.setTargets(List.of(targets));
        return route;
    }

    private static ReverseProxyProperties.Upstream upstream(String id, int port) {
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId(id);
        upstream.setUrl("http://127.0.0.1:" + port);
        upstream.setWeight(1.0);
        return upstream;
    }

    private static ReverseProxyService service(ReverseProxyProperties properties, HttpClient client) {
        return new ReverseProxyService(
                properties,
                client,
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static String selected(ReverseProxyService service, HttpServletRequest request) {
        return service.forward(request, new byte[0]).headers().getFirst("X-LoadBalancerPro-Upstream");
    }

    private static String keyFor(ReverseProxyRoutePlanner.ConfiguredRoute route, String groupName) {
        for (int index = 0; index < 10_000; index++) {
            String key = "split-client-" + index;
            if (groupName.equals(route.splitFor(key).orElseThrow().name())) {
                return key;
            }
        }
        throw new AssertionError("No routing key found for split group " + groupName);
    }

    private static HttpServletRequest request(
            String requestUri, String host, Map<String, String> headers, String remoteAddress) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, String> normalizedHeaders = new LinkedHashMap<>();
        headers.forEach((name, value) -> normalizedHeaders.put(name.toLowerCase(Locale.ROOT), value));
        if (host != null) {
            normalizedHeaders.put("host", host);
        }
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn(requestUri);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn(remoteAddress);
        when(request.getQueryString()).thenReturn(null);
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(normalizedHeaders.keySet()));
        when(request.getHeader(any(String.class))).thenAnswer(invocation ->
                normalizedHeaders.get(invocation.getArgument(0, String.class).toLowerCase(Locale.ROOT)));
        when(request.getHeaders(any(String.class))).thenAnswer(invocation -> {
            String value = normalizedHeaders.get(
                    invocation.getArgument(0, String.class).toLowerCase(Locale.ROOT));
            return value == null
                    ? Collections.emptyEnumeration()
                    : Collections.enumeration(List.of(value));
        });
        return request;
    }

    private static HttpClient successClient() throws Exception {
        return client(ignored -> response(200));
    }

    @SuppressWarnings("unchecked")
    private static HttpClient client(ResponseFactory factory) throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class))).thenAnswer(invocation ->
                factory.response(invocation.getArgument(0, HttpRequest.class)));
        return client;
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<InputStream> response(int status) {
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
        when(response.body()).thenReturn(new ByteArrayInputStream(new byte[0]));
        return response;
    }

    private static void assertRejected(ReverseProxyProperties properties, String expectedMessage) {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                ReverseProxyRoutePlanner.buildEnabledRoutes(
                        properties, RoutingStrategyRegistry.defaultRegistry()));
        assertTrue(exception.getMessage().contains(expectedMessage), exception::getMessage);
    }

    @FunctionalInterface
    private interface ResponseFactory {
        HttpResponse<InputStream> response(HttpRequest request) throws Exception;
    }
}
