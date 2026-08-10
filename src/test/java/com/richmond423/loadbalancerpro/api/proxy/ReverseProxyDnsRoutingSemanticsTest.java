package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.richmond423.loadbalancerpro.api.LaseShadowRuntime;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ReverseProxyDnsRoutingSemanticsTest {
    private static final String AFFINITY_COOKIE = "LB_DNS_AFFINITY";
    private static final String TEST_HMAC_KEY = "bounded-dns-affinity-test-key-32-bytes";

    @Test
    void retriesRemainInsideTheSelectedLogicalSplitAfterExpansion() throws Exception {
        ReverseProxyProperties properties = splitProperties();
        properties.getRetry().setEnabled(true);
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setBudgetPercent(100);
        properties.getRetry().getBackoff().setBase(Duration.ofMillis(1));
        properties.getRetry().getBackoff().setMax(Duration.ofMillis(1));
        ReverseProxyRoutePlanner.ConfiguredRoute logical = ReverseProxyRoutePlanner.buildEnabledRoutes(
                properties, RoutingStrategyRegistry.defaultRegistry()).get(0);
        String canaryKey = keyFor(logical, "canary");
        List<URI> attempts = new ArrayList<>();
        AtomicInteger call = new AtomicInteger();
        HttpClient client = client(request -> {
            attempts.add(request.uri());
            return call.getAndIncrement() == 0 ? 503 : 200;
        });
        ReverseProxyService service = service(properties, client,
                name -> List.of(literal(127, 0, 0, 1), literal(127, 0, 0, 2)));
        try {
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 4,
                    Duration.ofSeconds(3)));

            ReverseProxyResponse response = service.forward(request(canaryKey, null), new byte[0]);

            assertEquals(200, response.statusCode());
            assertTrue(response.headers().getFirst("X-LoadBalancerPro-Upstream").startsWith("canary-dns-"));
            assertEquals(List.of(18082, 18082), attempts.stream().map(URI::getPort).toList());
            assertFalse(attempts.stream().anyMatch(uri -> uri.getPort() == 18081));
        } finally {
            service.stop();
        }
    }

    @Test
    void affinityFailsOverAndRepinsWhenItsResolvedMemberDisappears() throws Exception {
        AtomicReference<List<InetAddress>> answers = new AtomicReference<>(List.of(
                literal(127, 0, 0, 1), literal(127, 0, 0, 2)));
        ReverseProxyProperties properties = singleRoute("ROUND_ROBIN");
        ReverseProxyProperties.Affinity affinity = new ReverseProxyProperties.Affinity();
        affinity.setCookieName(AFFINITY_COOKIE);
        affinity.setHmacKey(TEST_HMAC_KEY);
        properties.getRoutes().get("api").setAffinity(affinity);
        ReverseProxyService service = service(properties, client(ignored -> 200), name -> answers.get());
        try {
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 2,
                    Duration.ofSeconds(3)));
            ReverseProxyResponse first = service.forward(request("client-a", null), new byte[0]);
            String departedId = upstream(first);
            String oldCookie = affinityCookie(first);
            Map<String, String> addressById = service.statusSnapshot().dnsDiscovery().get(0).members().stream()
                    .collect(Collectors.toMap(
                            ReverseProxyStatusResponse.DnsMemberStatus::id,
                            ReverseProxyStatusResponse.DnsMemberStatus::address));
            String retainedAddress = addressById.get(departedId).equals("127.0.0.1")
                    ? "127.0.0.2"
                    : "127.0.0.1";
            answers.set(List.of("127.0.0.1".equals(retainedAddress)
                    ? literal(127, 0, 0, 1)
                    : literal(127, 0, 0, 2)));
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 1
                            && !service.statusSnapshot().upstreams().get(0).id().equals(departedId),
                    Duration.ofSeconds(3)));

            ReverseProxyResponse failedOver = service.forward(
                    request("client-b", new Cookie(AFFINITY_COOKIE, oldCookie)), new byte[0]);
            String retainedId = upstream(failedOver);
            String replacementCookie = affinityCookie(failedOver);

            assertNotEquals(departedId, retainedId);
            assertEquals(service.statusSnapshot().upstreams().get(0).id(), retainedId);
            assertNotEquals(oldCookie, replacementCookie);
            assertEquals(retainedId, upstream(service.forward(
                    request("client-c", new Cookie(AFFINITY_COOKIE, replacementCookie)), new byte[0])));
        } finally {
            service.stop();
        }
    }

    @Test
    void consistentHashOnlyMovesChangedKeysToTheAddedResolvedMember() throws Exception {
        AtomicReference<List<InetAddress>> answers = new AtomicReference<>(List.of(
                literal(127, 0, 0, 1), literal(127, 0, 0, 2)));
        ReverseProxyProperties properties = singleRoute("CONSISTENT_HASH");
        properties.getRoutes().get("api").setHashOn("header:X-Tenant-ID");
        ReverseProxyService service = service(properties, client(ignored -> 200), name -> answers.get());
        try {
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 2,
                    Duration.ofSeconds(3)));
            Map<String, String> before = new LinkedHashMap<>();
            for (int index = 0; index < 256; index++) {
                String key = "tenant-" + index;
                before.put(key, upstream(service.forward(request(key, null), new byte[0])));
            }
            assertEquals(before.get("tenant-7"),
                    upstream(service.forward(request("tenant-7", null), new byte[0])));
            Set<String> previousIds = Set.copyOf(service.statusSnapshot().upstreams().stream()
                    .map(ReverseProxyStatusResponse.UpstreamStatus::id).toList());

            answers.set(List.of(
                    literal(127, 0, 0, 1), literal(127, 0, 0, 2), literal(127, 0, 0, 3)));
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 3,
                    Duration.ofSeconds(3)));
            String addedId = service.statusSnapshot().upstreams().stream()
                    .map(ReverseProxyStatusResponse.UpstreamStatus::id)
                    .filter(id -> !previousIds.contains(id))
                    .findFirst().orElseThrow();

            Map<String, String> after = new LinkedHashMap<>();
            for (String key : before.keySet()) {
                after.put(key, upstream(service.forward(request(key, null), new byte[0])));
            }
            List<String> changedKeys = before.keySet().stream()
                    .filter(key -> !before.get(key).equals(after.get(key)))
                    .toList();

            assertFalse(changedKeys.isEmpty());
            assertTrue(changedKeys.stream().allMatch(key -> addedId.equals(after.get(key))));
            assertEquals(after.get("tenant-7"),
                    upstream(service.forward(request("tenant-7", null), new byte[0])));
        } finally {
            service.stop();
        }
    }

    @Test
    void metricsAndAccessLogsUseBoundedMemberIdsAndRetireDepartedCardinality() throws Exception {
        AtomicReference<List<InetAddress>> answers = new AtomicReference<>(
                List.of(literal(127, 0, 0, 1)));
        ReverseProxyProperties properties = singleRoute("ROUND_ROBIN");
        properties.getAccessLog().setEnabled(true);
        properties.getAccessLog().setSampleRate(1.0);
        properties.getAccessLog().setPath("target/dns-observability-test.log");
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ReverseProxyMetrics metrics = new ReverseProxyMetrics(registry);
        MemoryWriter writer = new MemoryWriter();
        ReverseProxyAccessLog accessLog = new ReverseProxyAccessLog(
                properties.getAccessLog(), Clock.systemUTC(), 16, ignored -> writer);
        accessLog.start();
        ReverseProxyService service = new ReverseProxyService(
                properties,
                client(ignored -> 200),
                metrics,
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.systemUTC(),
                LaseShadowRuntime.disabled(),
                accessLog,
                name -> answers.get());
        try {
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 1,
                    Duration.ofSeconds(3)));
            String departedId = service.statusSnapshot().upstreams().get(0).id();
            assertTrue(departedId.length() <= 64);
            assertTrue(departedId.matches("[A-Za-z0-9][A-Za-z0-9._-]*"));
            assertEquals(200, service.forward(request("metrics-a", null), new byte[0]).statusCode());

            answers.set(List.of(literal(127, 0, 0, 2)));
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 1
                            && !service.statusSnapshot().upstreams().get(0).id().equals(departedId),
                    Duration.ofSeconds(3)));
            String activeId = service.statusSnapshot().upstreams().get(0).id();
            assertEquals(200, service.forward(request("metrics-b", null), new byte[0]).statusCode());
            accessLog.stop();

            ReverseProxyMetricsSnapshot snapshot = metrics.snapshot(List.of(activeId));
            assertEquals(List.of(activeId), snapshot.upstreams().stream()
                    .map(ReverseProxyMetricsSnapshot.UpstreamCounters::upstreamId).toList());
            assertEquals(1, snapshot.upstreams().get(0).forwarded());
            assertEquals(2, snapshot.totalForwarded());
            assertFalse(registry.getMeters().stream()
                    .flatMap(meter -> meter.getId().getTags().stream())
                    .anyMatch(tag -> tag.getValue().equals(departedId)
                            || tag.getValue().contains("orders.internal")
                            || tag.getValue().contains("127.0.0.")));
            assertTrue(registry.getMeters().stream()
                    .flatMap(meter -> meter.getId().getTags().stream())
                    .anyMatch(tag -> tag.getValue().equals(activeId)));
            assertEquals(2, writer.lines.size());
            assertTrue(writer.lines.get(0).contains("\"upstream\":\"" + departedId + "\""));
            assertTrue(writer.lines.get(1).contains("\"upstream\":\"" + activeId + "\""));
            assertFalse(writer.lines.stream().anyMatch(line -> line.contains("orders.internal")
                    || line.contains("127.0.0.")));
        } finally {
            service.stop();
            accessLog.stop();
        }
    }

    @Test
    void unchangedMemberKeepsCooldownStateWhileAnAddedMemberStartsFresh() throws Exception {
        AtomicReference<List<InetAddress>> answers = new AtomicReference<>(
                List.of(literal(127, 0, 0, 1)));
        ReverseProxyProperties properties = singleRoute("ROUND_ROBIN");
        properties.getCooldown().setEnabled(true);
        properties.getCooldown().setConsecutiveFailureThreshold(1);
        properties.getCooldown().setDuration(Duration.ofMinutes(1));
        ReverseProxyService service = service(properties, client(ignored -> {
            throw new UncheckedIOException(new IOException("synthetic transport failure"));
        }), name -> answers.get());
        try {
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 1,
                    Duration.ofSeconds(3)));
            String unchangedId = service.statusSnapshot().upstreams().get(0).id();
            assertEquals(502, service.forward(request("cooldown-a", null), new byte[0]).statusCode());
            assertTrue(service.statusSnapshot().upstreams().get(0).cooldownActive());

            answers.set(List.of(literal(127, 0, 0, 1), literal(127, 0, 0, 2)));
            assertTrue(waitUntil(() -> service.statusSnapshot().upstreams().size() == 2,
                    Duration.ofSeconds(3)));
            Map<String, ReverseProxyStatusResponse.UpstreamStatus> statusById =
                    service.statusSnapshot().upstreams().stream().collect(Collectors.toMap(
                            ReverseProxyStatusResponse.UpstreamStatus::id,
                            Function.identity()));
            ReverseProxyStatusResponse.UpstreamStatus added = statusById.values().stream()
                    .filter(status -> !status.id().equals(unchangedId))
                    .findFirst().orElseThrow();

            assertTrue(statusById.get(unchangedId).cooldownActive());
            assertFalse(added.cooldownActive());
            assertEquals(0, added.consecutiveFailures());
        } finally {
            service.stop();
        }
    }

    private static ReverseProxyProperties splitProperties() {
        ReverseProxyProperties properties = baseProperties();
        ReverseProxyProperties.Route route = new ReverseProxyProperties.Route();
        route.setPathPrefix("/api");
        route.setStrategy("ROUND_ROBIN");
        route.setTargets(List.of(
                discovered("stable", "stable.internal", 18081),
                discovered("canary", "canary.internal", 18082)));
        ReverseProxyProperties.SplitGroup stable = new ReverseProxyProperties.SplitGroup();
        stable.setPercentage(50);
        stable.setTargetIds(List.of("stable"));
        ReverseProxyProperties.SplitGroup canary = new ReverseProxyProperties.SplitGroup();
        canary.setPercentage(50);
        canary.setTargetIds(List.of("canary"));
        Map<String, ReverseProxyProperties.SplitGroup> splits = new LinkedHashMap<>();
        splits.put("stable", stable);
        splits.put("canary", canary);
        route.setSplit(splits);
        properties.setRoutes(Map.of("api", route));
        return properties;
    }

    private static ReverseProxyProperties singleRoute(String strategy) {
        ReverseProxyProperties properties = baseProperties();
        ReverseProxyProperties.Route route = new ReverseProxyProperties.Route();
        route.setPathPrefix("/api");
        route.setStrategy(strategy);
        route.setTargets(List.of(discovered("orders", "orders.internal", 18080)));
        properties.setRoutes(Map.of("api", route));
        return properties;
    }

    private static ReverseProxyProperties baseProperties() {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.getPrivateNetworkValidation().setEnabled(true);
        properties.getDnsDiscovery().setTtlFloor(Duration.ofSeconds(1));
        properties.getDnsDiscovery().setStaleAfter(Duration.ofSeconds(5));
        properties.getDnsDiscovery().setResolutionTimeout(Duration.ofMillis(100));
        return properties;
    }

    private static ReverseProxyProperties.Upstream discovered(String id, String name, int port) {
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId(id);
        upstream.setUrl("http://" + name + ":" + port);
        upstream.setDiscovery("dns:" + name + ":" + port);
        upstream.setDiscoveryAuthority("address");
        return upstream;
    }

    private static String keyFor(ReverseProxyRoutePlanner.ConfiguredRoute route, String group) {
        for (int index = 0; index < 10_000; index++) {
            String candidate = "client-" + index;
            if (group.equals(route.splitFor(candidate).orElseThrow().name())) {
                return candidate;
            }
        }
        throw new AssertionError("no deterministic key selected split " + group);
    }

    private static ReverseProxyService service(
            ReverseProxyProperties properties,
            HttpClient client,
            ProxyDnsDiscoveryRuntime.Resolver resolver) {
        return new ReverseProxyService(
                properties,
                client,
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.systemUTC(),
                LaseShadowRuntime.disabled(),
                ReverseProxyAccessLog.disabled(),
                resolver);
    }

    @SuppressWarnings("unchecked")
    private static HttpClient client(Function<HttpRequest, Integer> responseStatus) throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0, HttpRequest.class);
            int status = responseStatus.apply(request);
            HttpResponse<InputStream> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(status);
            when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
            when(response.body()).thenReturn(new ByteArrayInputStream(new byte[0]));
            return response;
        });
        return client;
    }

    private static MockHttpServletRequest request(String key, Cookie cookie) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/proxy/api/resource");
        request.setRequestURI("/proxy/api/resource");
        request.setRemoteAddr(key);
        request.addHeader("X-Tenant-ID", key);
        if (cookie != null) {
            request.setCookies(cookie);
        }
        return request;
    }

    private static String upstream(ReverseProxyResponse response) {
        return response.headers().getFirst("X-LoadBalancerPro-Upstream");
    }

    private static String affinityCookie(ReverseProxyResponse response) {
        return response.headers().getOrEmpty("Set-Cookie").stream()
                .filter(value -> value.startsWith(AFFINITY_COOKIE + "="))
                .map(value -> value.substring((AFFINITY_COOKIE + "=").length(), value.indexOf(';')))
                .findFirst().orElseThrow();
    }

    private static InetAddress literal(int... octets) throws Exception {
        byte[] bytes = new byte[octets.length];
        for (int index = 0; index < octets.length; index++) {
            bytes[index] = (byte) octets[index];
        }
        return InetAddress.getByAddress(bytes);
    }

    private static boolean waitUntil(Check check, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!check.evaluate() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        return check.evaluate();
    }

    @FunctionalInterface
    private interface Check {
        boolean evaluate() throws Exception;
    }

    private static final class MemoryWriter implements ReverseProxyAccessLog.EventWriter {
        private final List<String> lines = new CopyOnWriteArrayList<>();

        @Override
        public void append(String line) {
            lines.add(line);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
