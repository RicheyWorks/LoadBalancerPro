package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class ReverseProxyConsistentHashAffinityTest {
    private static final Instant NOW = Instant.parse("2026-07-31T00:00:00Z");
    private static final String AFFINITY_COOKIE = "LB_AFFINITY";
    private static final String TEST_HMAC_KEY = "bounded-local-test-key-32-bytes-minimum";

    @Test
    void consistentHashUsesConfiguredHeaderAndKeepsSameKeyOnSameHealthyUpstream() throws Exception {
        ReverseProxyService service = service(consistentHashProperties(), responseHeaders(Map.of()));
        String selected = null;

        for (int requestNumber = 0; requestNumber < 50; requestNumber++) {
            ReverseProxyResponse response = service.forward(
                    request("tenant-a", "198.51.100." + requestNumber, null, false), new byte[0]);
            String current = response.headers().getFirst("X-LoadBalancerPro-Upstream");
            if (selected == null) {
                selected = current;
            }
            assertEquals(selected, current);
        }

        ReverseProxyStatusResponse.RouteStatus status = service.statusSnapshot().routes().get(0);
        assertEquals("CONSISTENT_HASH", status.strategy());
        assertEquals("header:X-Tenant-ID", status.hashOn());
        assertFalse(status.affinityEnabled());
        assertFalse(service.statusSnapshot().toString().contains(TEST_HMAC_KEY));
    }

    @Test
    void clientIpHashIgnoresCallerSuppliedForwardingMetadata() throws Exception {
        ReverseProxyService service = service(baseProperties("CONSISTENT_HASH"), responseHeaders(Map.of()));
        HttpServletRequest firstRequest = request(null, "198.51.100.10", null, false);
        HttpServletRequest secondRequest = request(null, "198.51.100.10", null, false);
        when(firstRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10");
        when(secondRequest.getHeader("X-Forwarded-For")).thenReturn("192.0.2.55");

        ReverseProxyResponse first = service.forward(firstRequest, new byte[0]);
        ReverseProxyResponse second = service.forward(secondRequest, new byte[0]);

        assertEquals(upstream(first), upstream(second));
        assertEquals("client-ip", service.statusSnapshot().routes().get(0).hashOn());
    }

    @Test
    void signedCookiePinsIndependentlyOfStrategyThenFailsOverAndIsReplaced() throws Exception {
        ReverseProxyProperties initial = affinityProperties(true);
        ReverseProxyService service = service(
                initial, responseHeaders(Map.of("Set-Cookie", List.of(
                        "backend=ok; Path=/",
                        AFFINITY_COOKIE + "=unsigned-upstream-value; Path=/"))));

        ReverseProxyResponse first = service.forward(request(null, "198.51.100.10", null, false), new byte[0]);
        assertEquals("alpha", upstream(first));
        String alphaCookie = affinityCookie(first);
        assertTrue(first.headers().get("Set-Cookie").stream().anyMatch(value -> value.startsWith("backend=")));
        assertEquals(1, first.headers().get("Set-Cookie").stream()
                .filter(value -> value.startsWith(AFFINITY_COOKIE + "="))
                .count(), "the upstream cannot replace the proxy-owned affinity value");
        assertTrue(first.headers().get("Set-Cookie").stream().anyMatch(value -> value.contains("HttpOnly")));
        assertTrue(first.headers().get("Set-Cookie").stream().anyMatch(value -> value.contains("SameSite=Lax")));
        assertFalse(first.headers().get("Set-Cookie").stream().anyMatch(value -> value.contains("Secure")));

        ReverseProxyResponse pinned = service.forward(
                request(null, "203.0.113.99", new Cookie(AFFINITY_COOKIE, alphaCookie), false), new byte[0]);
        assertEquals("alpha", upstream(pinned), "valid affinity must bypass round-robin's next position");

        ReverseProxyReloadResponse reload = service.reload(affinityProperties(false));
        assertTrue(reload.success());
        ReverseProxyResponse failedOver = service.forward(
                request(null, "203.0.113.99", new Cookie(AFFINITY_COOKIE, alphaCookie), true), new byte[0]);
        String failoverUpstream = upstream(failedOver);
        assertNotEquals("alpha", failoverUpstream);
        String failoverCookie = affinityCookie(failedOver);
        assertNotEquals(alphaCookie, failoverCookie);
        assertTrue(failedOver.headers().get("Set-Cookie").stream().anyMatch(value -> value.contains("Secure")));

        ReverseProxyResponse repinned = service.forward(
                request(null, "192.0.2.55", new Cookie(AFFINITY_COOKIE, failoverCookie), false), new byte[0]);
        assertEquals(failoverUpstream, upstream(repinned));
    }

    @Test
    void tamperedCookieFallsBackToTheConfiguredStrategy() throws Exception {
        ReverseProxyService service = service(affinityProperties(true), responseHeaders(Map.of()));
        ReverseProxyResponse first = service.forward(request(null, "198.51.100.10", null, false), new byte[0]);
        String signed = affinityCookie(first);
        String tampered = signed.substring(0, signed.length() - 1)
                + (signed.endsWith("A") ? "B" : "A");

        ReverseProxyResponse response = service.forward(
                request(null, "198.51.100.10", new Cookie(AFFINITY_COOKIE, tampered), false), new byte[0]);

        assertEquals("beta", upstream(response));
    }

    @Test
    void failedUpstreamResponseCannotIssueOrReplaceTheProxyOwnedCookie() throws Exception {
        ReverseProxyService service = service(
                affinityProperties(true),
                responseHeaders(Map.of("Set-Cookie", List.of(
                        AFFINITY_COOKIE + "=unsigned-upstream-value; Path=/"))),
                503);

        ReverseProxyResponse response = service.forward(
                request(null, "198.51.100.10", null, false), new byte[0]);

        assertEquals(503, response.statusCode());
        assertTrue(response.headers().getOrEmpty("Set-Cookie").isEmpty());
    }

    @Test
    void invalidAffinityAndHashConfigurationFailClosedWithoutEchoingTheSecret() {
        ReverseProxyProperties missingKey = affinityProperties(true);
        missingKey.getRoutes().get("api").getAffinity().setHmacKey("short-private-value");

        IllegalStateException keyFailure = assertThrows(IllegalStateException.class,
                () -> ReverseProxyRoutePlanner.buildEnabledRoutes(
                        missingKey, RoutingStrategyRegistry.defaultRegistry()));
        assertTrue(keyFailure.getMessage().contains("hmac-key"));
        assertFalse(keyFailure.getMessage().contains("short-private-value"));

        ReverseProxyProperties sensitiveHeader = consistentHashProperties();
        sensitiveHeader.getRoutes().get("api").setHashOn("header:Authorization");
        IllegalStateException headerFailure = assertThrows(IllegalStateException.class,
                () -> ReverseProxyRoutePlanner.buildEnabledRoutes(
                        sensitiveHeader, RoutingStrategyRegistry.defaultRegistry()));
        assertTrue(headerFailure.getMessage().contains("cannot use"));

        ReverseProxyProperties forwardingHeader = consistentHashProperties();
        forwardingHeader.getRoutes().get("api").setHashOn("header:X-Forwarded-For");
        IllegalStateException forwardingFailure = assertThrows(IllegalStateException.class,
                () -> ReverseProxyRoutePlanner.buildEnabledRoutes(
                        forwardingHeader, RoutingStrategyRegistry.defaultRegistry()));
        assertTrue(forwardingFailure.getMessage().contains("cannot use"));
    }

    private static ReverseProxyService service(
            ReverseProxyProperties properties, HttpHeaders upstreamHeaders) throws Exception {
        return service(properties, upstreamHeaders, 200);
    }

    private static ReverseProxyService service(
            ReverseProxyProperties properties, HttpHeaders upstreamHeaders, int statusCode) throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.headers()).thenReturn(upstreamHeaders);
        when(response.body()).thenReturn(new byte[0]);
        when(client.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenReturn(response);
        return new ReverseProxyService(
                properties,
                client,
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static HttpHeaders responseHeaders(Map<String, List<String>> values) {
        return HttpHeaders.of(values, (name, value) -> true);
    }

    private static ReverseProxyProperties consistentHashProperties() {
        ReverseProxyProperties properties = baseProperties("CONSISTENT_HASH");
        properties.getRoutes().get("api").setHashOn("header:X-Tenant-ID");
        return properties;
    }

    private static ReverseProxyProperties affinityProperties(boolean alphaHealthy) {
        ReverseProxyProperties properties = baseProperties("ROUND_ROBIN");
        ReverseProxyProperties.Route route = properties.getRoutes().get("api");
        route.getTargets().get(0).setHealthy(alphaHealthy);
        ReverseProxyProperties.Affinity affinity = new ReverseProxyProperties.Affinity();
        affinity.setCookieName(AFFINITY_COOKIE);
        affinity.setHmacKey(TEST_HMAC_KEY);
        route.setAffinity(affinity);
        return properties;
    }

    private static ReverseProxyProperties baseProperties(String strategy) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        ReverseProxyProperties.Route route = new ReverseProxyProperties.Route();
        route.setPathPrefix("/api");
        route.setStrategy(strategy);
        route.setTargets(List.of(
                upstream("alpha", "http://127.0.0.1:18081"),
                upstream("beta", "http://127.0.0.1:18082"),
                upstream("gamma", "http://127.0.0.1:18083")));
        properties.setRoutes(Map.of("api", route));
        return properties;
    }

    private static ReverseProxyProperties.Upstream upstream(String id, String url) {
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId(id);
        upstream.setUrl(url);
        upstream.setWeight(1.0);
        return upstream;
    }

    private static HttpServletRequest request(
            String tenant, String remoteAddress, Cookie cookie, boolean secure) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/proxy/api/resource");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getHeader("X-Tenant-ID")).thenReturn(tenant);
        when(request.getRemoteAddr()).thenReturn(remoteAddress);
        when(request.getCookies()).thenReturn(cookie == null ? null : new Cookie[]{cookie});
        when(request.getQueryString()).thenReturn(null);
        when(request.isSecure()).thenReturn(secure);
        return request;
    }

    private static String upstream(ReverseProxyResponse response) {
        return response.headers().getFirst("X-LoadBalancerPro-Upstream");
    }

    private static String affinityCookie(ReverseProxyResponse response) {
        return response.headers().get("Set-Cookie").stream()
                .filter(value -> value.startsWith(AFFINITY_COOKIE + "="))
                .map(value -> value.substring((AFFINITY_COOKIE + "=").length(), value.indexOf(';')))
                .findFirst()
                .orElseThrow();
    }
}
