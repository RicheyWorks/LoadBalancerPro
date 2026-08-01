package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class ReverseProxyWeightDrainAndInterruptionTest {
    private static final Instant TIMESTAMP = Instant.parse("2026-07-29T00:00:00Z");

    @Test
    void proxyDrainsZeroWeightAndPreservesOnePercentShare() throws Exception {
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
                weightedProperties(),
                httpClient,
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.fixed(TIMESTAMP, ZoneOffset.UTC));
        Map<String, Integer> selections = new LinkedHashMap<>();

        for (int request = 0; request < 10_100; request++) {
            ReverseProxyResponse response = service.forward(request(), new byte[0]);
            String upstreamId = response.headers().getFirst("X-LoadBalancerPro-Upstream");
            selections.merge(upstreamId, 1, Integer::sum);
        }

        assertEquals(0, selections.getOrDefault("drained", 0));
        assertEquals(10_000, selections.getOrDefault("primary", 0));
        assertEquals(100, selections.getOrDefault("one-percent", 0));
        verify(httpClient, times(10_100)).send(
                any(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());
    }

    @Test
    void interruptedForwardRestoresInterruptAndDoesNotRetry() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(
                any(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenThrow(new InterruptedException("bounded test interruption"));
        ReverseProxyMetrics metrics = new ReverseProxyMetrics();
        ReverseProxyService service = new ReverseProxyService(
                retryingProperties(),
                httpClient,
                metrics,
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.fixed(TIMESTAMP, ZoneOffset.UTC));

        try {
            ReverseProxyResponse response = service.forward(request(), new byte[0]);

            assertEquals(502, response.statusCode());
            assertTrue(Thread.currentThread().isInterrupted(),
                    "interrupted forwarding must restore the caller's interrupt flag");
            verify(httpClient, times(1)).send(
                    any(),
                    org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());
            ReverseProxyMetricsSnapshot snapshot = metrics.snapshot(List.of("first", "second"));
            assertEquals(1, snapshot.totalFailures());
            assertEquals(0, snapshot.totalRetryAttempts());
        } finally {
            Thread.interrupted();
        }
    }

    private static ReverseProxyProperties retryingProperties() {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setStrategy("ROUND_ROBIN");
        ReverseProxyProperties.Retry retry = new ReverseProxyProperties.Retry();
        retry.setEnabled(true);
        retry.setMaxAttempts(2);
        retry.setBudgetPercent(100);
        ReverseProxyProperties.Backoff backoff = new ReverseProxyProperties.Backoff();
        backoff.setBase(Duration.ZERO);
        backoff.setMax(Duration.ZERO);
        retry.setBackoff(backoff);
        properties.setRetry(retry);
        properties.setUpstreams(List.of(
                upstream("first", "http://127.0.0.1:18081", 1.0),
                upstream("second", "http://127.0.0.1:18082", 1.0)));
        return properties;
    }

    private static ReverseProxyProperties weightedProperties() {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setStrategy("WEIGHTED_ROUND_ROBIN");
        properties.setUpstreams(List.of(
                upstream("drained", "http://127.0.0.1:18081", 0.0),
                upstream("primary", "http://127.0.0.1:18082", 1.0),
                upstream("one-percent", "http://127.0.0.1:18083", 0.01)));
        return properties;
    }

    private static ReverseProxyProperties.Upstream upstream(String id, String url, double weight) {
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId(id);
        upstream.setUrl(url);
        upstream.setWeight(weight);
        return upstream;
    }

    private static HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/proxy/interrupted");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getQueryString()).thenReturn(null);
        return request;
    }
}
