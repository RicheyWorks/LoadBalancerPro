package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.richmond423.loadbalancerpro.core.RoundRobinRoutingStrategy;
import com.richmond423.loadbalancerpro.core.RoutingDecision;
import com.richmond423.loadbalancerpro.core.RoutingStrategy;
import com.richmond423.loadbalancerpro.core.RoutingStrategyId;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import com.richmond423.loadbalancerpro.core.ServerStateVector;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class ReverseProxyLiveTelemetryRoutingTest {
    private static final Instant TIMESTAMP = Instant.parse("2026-07-31T00:00:00Z");

    @Test
    void configuredTelemetrySeedsColdStartThenLiveMeasurementsReplaceIt() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<InputStream> upstreamResponse = mock(HttpResponse.class);
        when(upstreamResponse.statusCode()).thenReturn(200);
        when(upstreamResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
        when(upstreamResponse.body()).thenAnswer(ignored -> new ByteArrayInputStream(new byte[0]));
        when(httpClient.send(
                any(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(upstreamResponse);
        RecordingStrategy strategy = new RecordingStrategy();
        ReverseProxyService service = new ReverseProxyService(
                seededProperties(),
                httpClient,
                new ReverseProxyMetrics(),
                new RoutingStrategyRegistry(List.of(strategy)),
                Clock.fixed(TIMESTAMP, ZoneOffset.UTC));

        service.forward(request(), new byte[0]);
        ServerStateVector coldStart = strategy.candidates().get(0).get(0);
        assertEquals(7, coldStart.inFlightRequestCount());
        assertEquals(11.0, coldStart.averageLatencyMillis());
        assertEquals(22.0, coldStart.p95LatencyMillis());
        assertEquals(33.0, coldStart.p99LatencyMillis());
        assertEquals(0.4, coldStart.recentErrorRate());
        assertEquals(9, coldStart.queueDepth().orElseThrow());

        ReverseProxyStatusResponse.UpstreamRuntimeStatus measured =
                service.statusSnapshot().upstreams().get(0).runtimeStats();
        assertEquals(1, measured.completedRequestCount());
        assertEquals(1, measured.latencySampleCount());

        service.forward(request(), new byte[0]);
        ServerStateVector live = strategy.candidates().get(1).get(0);
        assertEquals(0, live.inFlightRequestCount());
        assertEquals(measured.ewmaLatencyMillis(), live.averageLatencyMillis());
        assertEquals(measured.p95LatencyMillis(), live.p95LatencyMillis());
        assertEquals(measured.p99LatencyMillis(), live.p99LatencyMillis());
        assertEquals(0.0, live.recentErrorRate());
        assertEquals(0, live.queueDepth().orElseThrow());
    }

    private static ReverseProxyProperties seededProperties() {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setStrategy("ROUND_ROBIN");
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId("seeded");
        upstream.setUrl("http://127.0.0.1:18081");
        upstream.setInFlightRequestCount(7);
        upstream.setAverageLatencyMillis(11.0);
        upstream.setP95LatencyMillis(22.0);
        upstream.setP99LatencyMillis(33.0);
        upstream.setRecentErrorRate(0.4);
        upstream.setQueueDepth(9);
        properties.setUpstreams(List.of(upstream));
        return properties;
    }

    private static HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/proxy/live");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getQueryString()).thenReturn(null);
        return request;
    }

    private static final class RecordingStrategy implements RoutingStrategy {
        private final RoundRobinRoutingStrategy delegate = new RoundRobinRoutingStrategy();
        private final List<List<ServerStateVector>> candidates = new ArrayList<>();

        @Override
        public RoutingStrategyId id() {
            return RoutingStrategyId.ROUND_ROBIN;
        }

        @Override
        public RoutingDecision choose(List<ServerStateVector> servers) {
            candidates.add(List.copyOf(servers));
            return delegate.choose(servers);
        }

        private List<List<ServerStateVector>> candidates() {
            return List.copyOf(candidates);
        }
    }
}
