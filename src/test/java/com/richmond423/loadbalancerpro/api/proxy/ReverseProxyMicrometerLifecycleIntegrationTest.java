package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ReverseProxyMicrometerLifecycleIntegrationTest {
    private static final String ROUTE = ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME;

    @Test
    void knownChunkedAndLargeStreamingResponsesRecordActualBytesAndOneTerminalTimerEach() throws Exception {
        byte[] known = "known-body".getBytes(StandardCharsets.UTF_8);
        byte[] chunked = "unknown-length-body".getBytes(StandardCharsets.UTF_8);
        byte[] large = new byte[2 * 1024 * 1024];
        java.util.Arrays.fill(large, (byte) 'x');
        HttpClient client = mock(HttpClient.class);
        HttpResponse<InputStream> knownResponse = response(200, known, (long) known.length);
        HttpResponse<InputStream> chunkedResponse = response(200, chunked, null);
        HttpResponse<InputStream> largeResponse = response(200, large, null);
        when(client.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(knownResponse, chunkedResponse, largeResponse);

        try (Fixture fixture = fixture(properties("alpha"), client)) {
            assertEquals(200, fixture.service.forward(request("GET"), new byte[] {1, 2, 3}).statusCode());
            assertEquals(200, fixture.service.forward(request("GET"), new byte[] {4, 5}).statusCode());
            assertEquals(200, fixture.service.forward(request("GET"), new byte[] {6}).statusCode());

            assertEquals(3.0, requests(fixture.registry, "alpha", "2xx", "SUCCESS").count());
            assertEquals(3L, fixture.registry.get(ReverseProxyMetrics.LATENCY)
                    .tags("route", ROUTE, "upstream", "alpha").timer().count());
            assertEquals(6.0, fixture.registry.get(ReverseProxyMetrics.REQUEST_BYTES)
                    .tags("route", ROUTE, "upstream", "alpha").summary().totalAmount());
            assertEquals(known.length + chunked.length + large.length,
                    fixture.registry.get(ReverseProxyMetrics.RESPONSE_BYTES)
                            .tags("route", ROUTE, "upstream", "alpha").summary().totalAmount());
            assertEquals(0.0, gauge(fixture.registry, ReverseProxyMetrics.INFLIGHT, "alpha").value());
        }
    }

    @Test
    void concurrentRequestsExposeAccurateInflightAndReturnToZero() throws Exception {
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenAnswer(invocation -> {
                    entered.countDown();
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IOException("fixture timeout");
                    }
                    return response(200, "ok".getBytes(StandardCharsets.UTF_8), null);
                });

        try (Fixture fixture = fixture(properties("alpha"), client)) {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<ReverseProxyResponse> first = executor.submit(
                        () -> fixture.service.forward(request("GET"), new byte[0]));
                Future<ReverseProxyResponse> second = executor.submit(
                        () -> fixture.service.forward(request("GET"), new byte[0]));
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertEquals(2.0, gauge(fixture.registry, ReverseProxyMetrics.INFLIGHT, "alpha").value());
                release.countDown();
                assertEquals(200, first.get(5, TimeUnit.SECONDS).statusCode());
                assertEquals(200, second.get(5, TimeUnit.SECONDS).statusCode());
                assertEquals(0.0, gauge(fixture.registry, ReverseProxyMetrics.INFLIGHT, "alpha").value());
                assertEquals(2.0, requests(fixture.registry, "alpha", "2xx", "SUCCESS").count());
            } finally {
                release.countDown();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void upstream4xxAnd5xxUseFiniteTerminalOutcomes() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<InputStream> notFound = response(
                404, "missing".getBytes(StandardCharsets.UTF_8), null);
        HttpResponse<InputStream> unavailable = response(
                503, "unavailable".getBytes(StandardCharsets.UTF_8), null);
        when(client.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(notFound, unavailable);

        try (Fixture fixture = fixture(properties("alpha"), client)) {
            fixture.service.forward(request("GET"), new byte[0]);
            fixture.service.forward(request("GET"), new byte[0]);
            assertEquals(1.0, requests(fixture.registry, "alpha", "4xx", "UPSTREAM_4XX").count());
            assertEquals(1.0, requests(fixture.registry, "alpha", "5xx", "UPSTREAM_5XX").count());
            assertEquals(2L, fixture.registry.get(ReverseProxyMetrics.LATENCY)
                    .tags("route", ROUTE, "upstream", "alpha").timer().count());
        }
    }

    @Test
    void precommitTransportFailureRetriesOnceAndCountsOnlyActualDispatches() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<InputStream> success = response(
                200, "ok".getBytes(StandardCharsets.UTF_8), null);
        when(client.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenThrow(new IOException("synthetic first transport failure"))
                .thenReturn(success);
        ReverseProxyProperties properties = properties("alpha", "beta");
        properties.getRetry().setEnabled(true);
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setBudgetPercent(100);

        try (Fixture fixture = fixture(properties, client)) {
            assertEquals(200, fixture.service.forward(
                    request("GET"), "request".getBytes(StandardCharsets.UTF_8)).statusCode());

            assertEquals(1.0, counter(fixture.registry, ReverseProxyMetrics.ATTEMPTS,
                    "route", ROUTE, "upstream", "alpha",
                    "kind", "PRIMARY", "reason", "INITIAL").count());
            assertEquals(1.0, counter(fixture.registry, ReverseProxyMetrics.ATTEMPTS,
                    "route", ROUTE, "upstream", "beta",
                    "kind", "RETRY", "reason", "TRANSPORT_FAILURE").count());
            assertEquals(1.0, counter(fixture.registry, ReverseProxyMetrics.RETRIES,
                    "route", ROUTE, "upstream", "beta",
                    "reason", "TRANSPORT_FAILURE").count());
            assertEquals(1.0, requests(fixture.registry, "beta", "2xx", "SUCCESS").count());
            assertEquals(7.0, fixture.registry.get(ReverseProxyMetrics.REQUEST_BYTES)
                    .tags("route", ROUTE, "upstream", "beta").summary().totalAmount());
        }
    }

    @Test
    void precommitAndPostcommitUpstreamAbortsAreDistinctAndPostcommitIsNotRetried() throws Exception {
        HttpClient precommitClient = mock(HttpClient.class);
        HttpResponse<InputStream> precommitResponse = response(
                200, new AlwaysFailingInputStream(), null);
        when(precommitClient.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(precommitResponse);
        try (Fixture fixture = fixture(properties("alpha"), precommitClient)) {
            assertEquals(502, fixture.service.forward(request("GET"), new byte[0]).statusCode());
            assertEquals(1.0, requests(
                    fixture.registry, "alpha", "5xx", "UPSTREAM_ABORT_PRECOMMIT").count());
        }

        HttpClient postcommitClient = mock(HttpClient.class);
        HttpResponse<InputStream> postcommitResponse = response(
                200, new PartialThenFailingInputStream(), null);
        when(postcommitClient.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(postcommitResponse);
        ReverseProxyProperties retrying = properties("alpha", "beta");
        retrying.getRetry().setEnabled(true);
        retrying.getRetry().setMaxAttempts(2);
        retrying.getRetry().setBudgetPercent(100);
        try (Fixture fixture = fixture(retrying, postcommitClient)) {
            assertThrows(UncheckedIOException.class,
                    () -> fixture.service.forward(request("GET"), new byte[0]));
            assertEquals(1.0, requests(
                    fixture.registry, "alpha", "2xx", "UPSTREAM_ABORT_POSTCOMMIT").count());
            assertEquals(0.0, totalCounters(fixture.registry, ReverseProxyMetrics.RETRIES));
            assertEquals(4.0, fixture.registry.get(ReverseProxyMetrics.RESPONSE_BYTES)
                    .tags("route", ROUTE, "upstream", "alpha").summary().totalAmount());
        }
    }

    @Test
    void downstreamDisconnectIsDistinctAndDoesNotPenalizeUpstreamHealth() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<InputStream> upstreamResponse = response(
                200, "response".getBytes(StandardCharsets.UTF_8), null);
        when(client.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(upstreamResponse);

        try (Fixture fixture = fixture(properties("alpha"), client)) {
            HttpServletResponse downstream = failingDownstreamResponse();
            assertThrows(IOException.class, () -> fixture.service.forward(request("GET"), downstream));
            assertEquals(1.0, requests(
                    fixture.registry, "alpha", "2xx", "DOWNSTREAM_DISCONNECT").count());
            assertEquals(0.0, fixture.registry.get(ReverseProxyMetrics.RESPONSE_BYTES)
                    .tags("route", ROUTE, "upstream", "alpha").summary().totalAmount());
            ReverseProxyMetricsSnapshot.UpstreamCounters counters = fixture.service.statusSnapshot()
                    .metrics().upstreams().stream()
                    .filter(upstream -> upstream.upstreamId().equals("alpha"))
                    .findFirst().orElseThrow();
            assertEquals(0L, counters.failures());
            assertEquals(0, fixture.service.statusSnapshot().upstreams().get(0)
                    .runtimeStats().recentFailureCount());
        }
    }

    @Test
    void requestAndResponseLimitRejectionsAreBoundedAndReturnInflightToZero() throws Exception {
        HttpClient unused = mock(HttpClient.class);
        ReverseProxyProperties requestLimited = properties("alpha");
        requestLimited.setMaxRequestBytes(4);
        try (Fixture fixture = fixture(requestLimited, unused)) {
            assertEquals(413, fixture.service.forward(request("POST"), new byte[5]).statusCode());
            assertEquals(1.0, counter(fixture.registry, ReverseProxyMetrics.LIMIT_REJECTIONS,
                    "route", ReverseProxyMetrics.UNMATCHED, "upstream", ReverseProxyMetrics.NONE,
                    "direction", "REQUEST", "phase", "PRECOMMIT").count());
            assertEquals(0.0, totalGauge(fixture.registry, ReverseProxyMetrics.INFLIGHT));
            assertEquals(0.0, totalCounters(fixture.registry, ReverseProxyMetrics.ATTEMPTS));
        }

        HttpClient responseClient = mock(HttpClient.class);
        HttpResponse<InputStream> oversizedResponse = response(
                200, "12345".getBytes(StandardCharsets.UTF_8), 5L);
        when(responseClient.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(oversizedResponse);
        ReverseProxyProperties responseLimited = properties("alpha");
        responseLimited.setMaxResponseBytes(4);
        try (Fixture fixture = fixture(responseLimited, responseClient)) {
            assertEquals(502, fixture.service.forward(request("GET"), new byte[0]).statusCode());
            assertEquals(1.0, counter(fixture.registry, ReverseProxyMetrics.LIMIT_REJECTIONS,
                    "route", ROUTE, "upstream", "alpha",
                    "direction", "RESPONSE", "phase", "PRECOMMIT").count());
            assertEquals(0.0, totalGauge(fixture.registry, ReverseProxyMetrics.INFLIGHT));
        }
    }

    @Test
    void disabledProxyAndRegistryFailureDoNotChangeRequestBehaviorOrEmitTerminalTraffic() throws Exception {
        ReverseProxyProperties disabled = new ReverseProxyProperties();
        disabled.setEnabled(false);
        try (Fixture fixture = fixture(disabled, mock(HttpClient.class))) {
            assertEquals(503, fixture.service.forward(request("GET"), new byte[0]).statusCode());
            assertEquals(0.0, totalCounters(fixture.registry, ReverseProxyMetrics.REQUESTS));
            assertEquals(0L, fixture.registry.find(ReverseProxyMetrics.LATENCY).timers().stream()
                    .mapToLong(io.micrometer.core.instrument.Timer::count).sum());
        }

        SimpleMeterRegistry failingRegistry = new SimpleMeterRegistry();
        failingRegistry.config().onMeterAdded(meter -> {
            throw new IllegalStateException("synthetic instrumentation failure");
        });
        HttpClient client = mock(HttpClient.class);
        HttpResponse<InputStream> success = response(
                200, "ok".getBytes(StandardCharsets.UTF_8), null);
        when(client.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(success);
        ReverseProxyMetrics failingMetrics = new ReverseProxyMetrics(failingRegistry);
        ReverseProxyService service = new ReverseProxyService(
                properties("alpha"), client, failingMetrics,
                RoutingStrategyRegistry.defaultRegistry(), Clock.systemUTC());
        try {
            assertEquals(200, service.forward(request("GET"), new byte[0]).statusCode());
        } finally {
            service.stop();
        }
    }

    private static Fixture fixture(ReverseProxyProperties properties, HttpClient client) {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ReverseProxyMetrics metrics = new ReverseProxyMetrics(registry);
        ReverseProxyService service = new ReverseProxyService(
                properties, client, metrics, RoutingStrategyRegistry.defaultRegistry(), Clock.systemUTC());
        return new Fixture(registry, service);
    }

    private static ReverseProxyProperties properties(String... ids) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setUpstreams(java.util.Arrays.stream(ids).map(id -> {
            ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
            upstream.setId(id);
            upstream.setUrl("http://127.0.0.1:18080");
            return upstream;
        }).toList());
        return properties;
    }

    private static MockHttpServletRequest request(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/proxy/data");
        request.setRequestURI("/proxy/data");
        return request;
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<InputStream> response(
            int status, byte[] body, Long contentLength) {
        return response(status, new ByteArrayInputStream(body), contentLength);
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<InputStream> response(
            int status, InputStream body, Long contentLength) {
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        Map<String, List<String>> headers = contentLength == null
                ? Map.of()
                : Map.of("Content-Length", List.of(Long.toString(contentLength)));
        when(response.headers()).thenReturn(java.net.http.HttpHeaders.of(headers, (name, value) -> true));
        return response;
    }

    private static HttpServletResponse failingDownstreamResponse() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream output = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }

            @Override
            public void write(int value) throws IOException {
                throw new IOException("synthetic downstream disconnect");
            }

            @Override
            public void write(byte[] bytes, int offset, int length) throws IOException {
                throw new IOException("synthetic downstream disconnect");
            }
        };
        when(response.getOutputStream()).thenReturn(output);
        return response;
    }

    private static Counter requests(
            SimpleMeterRegistry registry, String upstream, String statusClass, String outcome) {
        return counter(registry, ReverseProxyMetrics.REQUESTS,
                "route", ROUTE, "upstream", upstream,
                "status_class", statusClass, "outcome", outcome);
    }

    private static Counter counter(SimpleMeterRegistry registry, String name, String... tags) {
        return registry.get(name).tags(tags).counter();
    }

    private static Gauge gauge(SimpleMeterRegistry registry, String name, String upstream) {
        return registry.get(name).tags("route", ROUTE, "upstream", upstream).gauge();
    }

    private static double totalGauge(SimpleMeterRegistry registry, String name) {
        return registry.find(name).gauges().stream().mapToDouble(Gauge::value).sum();
    }

    private static double totalCounters(SimpleMeterRegistry registry, String name) {
        return registry.find(name).counters().stream().mapToDouble(Counter::count).sum();
    }

    private record Fixture(SimpleMeterRegistry registry, ReverseProxyService service) implements AutoCloseable {
        @Override
        public void close() {
            service.stop();
        }
    }

    private static final class AlwaysFailingInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException("synthetic precommit truncation");
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            throw new IOException("synthetic precommit truncation");
        }
    }

    private static final class PartialThenFailingInputStream extends InputStream {
        private boolean delivered;

        @Override
        public int read() throws IOException {
            if (delivered) {
                throw new IOException("synthetic postcommit truncation");
            }
            delivered = true;
            return 'd';
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (delivered) {
                throw new IOException("synthetic postcommit truncation");
            }
            delivered = true;
            byte[] partial = "data".getBytes(StandardCharsets.UTF_8);
            System.arraycopy(partial, 0, bytes, offset, partial.length);
            return partial.length;
        }
    }
}
