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
import com.richmond423.loadbalancerpro.core.RoutingDecisionExplanation;
import com.richmond423.loadbalancerpro.core.RoutingStrategy;
import com.richmond423.loadbalancerpro.core.RoutingStrategyId;
import com.richmond423.loadbalancerpro.core.RoutingStrategyIdentifier;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import com.richmond423.loadbalancerpro.core.ServerStateVector;
import com.richmond423.loadbalancerpro.core.StatefulRoutingStrategy;
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

    @Test
    void liveProxyExecutesARegisteredExternalStrategy() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<InputStream> upstreamResponse = mock(HttpResponse.class);
        when(upstreamResponse.statusCode()).thenReturn(200);
        when(upstreamResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
        when(upstreamResponse.body()).thenAnswer(ignored -> new ByteArrayInputStream(new byte[0]));
        when(httpClient.send(
                any(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(upstreamResponse);
        RecordingStrategy strategy = new RecordingStrategy(
                RoutingStrategyIdentifier.of("daedalus-topology"));
        ReverseProxyService service = new ReverseProxyService(
                seededProperties("daedalus-topology"),
                httpClient,
                new ReverseProxyMetrics(),
                new RoutingStrategyRegistry(List.of(strategy)),
                Clock.fixed(TIMESTAMP, ZoneOffset.UTC));

        ReverseProxyResponse response = service.forward(request(), new byte[0]);

        assertEquals(200, response.statusCode());
        assertEquals("seeded", response.headers().getFirst("X-LoadBalancerPro-Upstream"));
        assertEquals(1, strategy.candidates().size());
    }

    @Test
    void liveProxyUsesTheStatefulNoListKeyedPath() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<InputStream> upstreamResponse = mock(HttpResponse.class);
        when(upstreamResponse.statusCode()).thenReturn(200);
        when(upstreamResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
        when(upstreamResponse.body()).thenAnswer(ignored -> new ByteArrayInputStream(new byte[0]));
        when(httpClient.send(
                any(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(upstreamResponse);
        StatefulRecordingStrategy strategy = new StatefulRecordingStrategy(
                RoutingStrategyIdentifier.of("daedalus-stateful"));
        ReverseProxyService service = new ReverseProxyService(
                seededProperties("daedalus-stateful"),
                httpClient,
                new ReverseProxyMetrics(),
                new RoutingStrategyRegistry(List.of(strategy)),
                Clock.fixed(TIMESTAMP, ZoneOffset.UTC));

        ReverseProxyResponse response = service.forward(request(), new byte[0]);
        ReverseProxyResponse secondResponse = service.forward(request(), new byte[0]);

        assertEquals(200, response.statusCode());
        assertEquals(200, secondResponse.statusCode());
        assertEquals("seeded", response.headers().getFirst("X-LoadBalancerPro-Upstream"));
        assertEquals(2, strategy.snapshotCount());
        assertEquals(2, strategy.chooseCount());
        assertEquals("unknown-immediate-client", strategy.lastKey());
    }

    private static ReverseProxyProperties seededProperties() {
        return seededProperties("ROUND_ROBIN");
    }

    private static ReverseProxyProperties seededProperties(String strategy) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setStrategy(strategy);
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
        private final RoutingStrategyIdentifier id;

        private RecordingStrategy() {
            this(RoutingStrategyId.ROUND_ROBIN);
        }

        private RecordingStrategy(RoutingStrategyIdentifier id) {
            this.id = id;
        }

        @Override
        public RoutingStrategyIdentifier id() {
            return id;
        }

        @Override
        public RoutingDecision choose(List<ServerStateVector> servers) {
            candidates.add(List.copyOf(servers));
            RoutingDecision delegated = delegate.choose(servers);
            RoutingDecisionExplanation explanation = delegated.explanation();
            return new RoutingDecision(
                    delegated.chosenServer(),
                    new RoutingDecisionExplanation(
                            id.externalName(),
                            explanation.candidateServersConsidered(),
                            explanation.chosenServerId(),
                            explanation.scores(),
                            explanation.factorContributions(),
                            explanation.reason(),
                            explanation.timestamp()));
        }

        private List<List<ServerStateVector>> candidates() {
            return List.copyOf(candidates);
        }
    }

    private static final class StatefulRecordingStrategy implements StatefulRoutingStrategy {
        private final RoundRobinRoutingStrategy delegate = new RoundRobinRoutingStrategy();
        private final List<ServerStateVector> currentCandidates = new ArrayList<>();
        private final RoutingStrategyIdentifier id;
        private int snapshotCount;
        private int chooseCount;
        private String lastKey;

        private StatefulRecordingStrategy(RoutingStrategyIdentifier id) {
            this.id = id;
        }

        @Override
        public RoutingStrategyIdentifier id() {
            return id;
        }

        @Override
        public void onServerState(ServerStateVector updated) {
            currentCandidates.add(updated);
        }

        @Override
        public void onServerStates(List<ServerStateVector> currentServers) {
            if (!Thread.holdsLock(this)) {
                throw new AssertionError("proxy did not serialize the stateful selection cycle");
            }
            snapshotCount++;
            currentCandidates.clear();
            StatefulRoutingStrategy.super.onServerStates(currentServers);
        }

        @Override
        public RoutingDecision choose() {
            chooseCount++;
            RoutingDecision delegated = delegate.choose(currentCandidates);
            RoutingDecisionExplanation explanation = delegated.explanation();
            return new RoutingDecision(
                    delegated.chosenServer(),
                    new RoutingDecisionExplanation(
                            id.externalName(),
                            explanation.candidateServersConsidered(),
                            explanation.chosenServerId(),
                            explanation.scores(),
                            explanation.factorContributions(),
                            explanation.reason(),
                            explanation.timestamp()));
        }

        @Override
        public RoutingDecision chooseForKey(String key) {
            if (!Thread.holdsLock(this)) {
                throw new AssertionError("proxy did not serialize the stateful selection cycle");
            }
            lastKey = key;
            return choose();
        }

        @Override
        public RoutingDecision choose(List<ServerStateVector> servers) {
            throw new AssertionError("proxy used the list compatibility path");
        }

        int snapshotCount() {
            return snapshotCount;
        }

        int chooseCount() {
            return chooseCount;
        }

        String lastKey() {
            return lastKey;
        }
    }
}
