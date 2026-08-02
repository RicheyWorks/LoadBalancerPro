package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.api.LaseShadowRuntime;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ReverseProxyAccessLogLifecycleIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path temporaryDirectory;

    @Test
    void retryingRequestEmitsOneTerminalRecordWithActualLifecycleValues() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<InputStream> success = response(
                200, "ok".getBytes(StandardCharsets.UTF_8));
        when(client.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenThrow(new IOException("synthetic transport failure with secret text"))
                .thenReturn(success);

        ReverseProxyProperties properties = properties("alpha", "beta");
        properties.getRetry().setEnabled(true);
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setBudgetPercent(100);
        properties.getCooldown().setEnabled(true);
        properties.getCooldown().setConsecutiveFailureThreshold(1);
        MemoryWriter writer = new MemoryWriter();

        try (Fixture fixture = fixture(properties, client, writer)) {
            ReverseProxyResponse response = fixture.service.forward(
                    request("GET"), "request".getBytes(StandardCharsets.UTF_8));
            assertEquals(200, response.statusCode());
            Set<String> establishedMeters = Set.of(
                            ReverseProxyMetrics.REQUESTS,
                            ReverseProxyMetrics.LATENCY,
                            ReverseProxyMetrics.INFLIGHT,
                            ReverseProxyMetrics.ATTEMPTS,
                            ReverseProxyMetrics.RETRIES,
                            ReverseProxyMetrics.REQUEST_BYTES,
                            ReverseProxyMetrics.RESPONSE_BYTES,
                            ReverseProxyMetrics.LIMIT_REJECTIONS,
                            ReverseProxyMetrics.SHEDS,
                            ReverseProxyMetrics.HEALTH,
                            ReverseProxyMetrics.COOLDOWN_TRIPS);
            Set<String> actualMeters = fixture.registry.getMeters().stream()
                    .map(meter -> meter.getId().getName())
                    .collect(java.util.stream.Collectors.toSet());
            assertTrue(actualMeters.containsAll(establishedMeters));
        }

        assertEquals(1, writer.lines.size());
        JsonNode event = JSON.readTree(writer.lines.get(0));
        assertEquals("GET", event.get("method").asText());
        assertEquals(ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME, event.get("route").asText());
        assertEquals("beta", event.get("upstream").asText());
        assertEquals(200, event.get("status").asInt());
        assertEquals(7, event.get("bytes_in").asLong());
        assertEquals(2, event.get("bytes_out").asLong());
        assertEquals(1, event.get("retries").asInt());
        assertTrue(event.get("cooldown").asBoolean());
        assertEquals("SUCCESS", event.get("outcome").asText());
        assertEquals("-", event.get("client").asText());
        assertEquals("-", event.get("path").asText());
        assertTrue(writer.lines.stream().noneMatch(line -> line.contains("secret")));
    }

    @Test
    void localFailureStillEmitsExactlyOneRecordWithoutDispatch() throws Exception {
        ReverseProxyProperties properties = properties("alpha");
        properties.setMaxRequestBytes(4);
        MemoryWriter writer = new MemoryWriter();

        try (Fixture fixture = fixture(properties, mock(HttpClient.class), writer)) {
            ReverseProxyResponse response = fixture.service.forward(request("POST"), new byte[5]);
            assertEquals(413, response.statusCode());
        }

        assertEquals(1, writer.lines.size());
        JsonNode event = JSON.readTree(writer.lines.get(0));
        assertEquals("UNMATCHED", event.get("route").asText());
        assertEquals("NONE", event.get("upstream").asText());
        assertEquals(413, event.get("status").asInt());
        assertEquals(0, event.get("retries").asInt());
        assertEquals("REQUEST_SIZE_LIMIT", event.get("outcome").asText());
    }

    @Test
    void accessWriterFailureCannotChangeSuccessfulProxyOutcome() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<InputStream> success = response(200, "ok".getBytes(StandardCharsets.UTF_8));
        when(client.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(success);
        ReverseProxyProperties properties = properties("alpha");
        FailingWriter writer = new FailingWriter();

        try (Fixture fixture = fixture(properties, client, writer)) {
            assertEquals(200, fixture.service.forward(request("GET"), new byte[0]).statusCode());
        }

        assertEquals(1, writer.attempts);
    }

    private Fixture fixture(
            ReverseProxyProperties properties,
            HttpClient client,
            ReverseProxyAccessLog.EventWriter writer) {
        properties.getAccessLog().setEnabled(true);
        properties.getAccessLog().setPath(temporaryDirectory.resolve("access.log").toString());
        ReverseProxyAccessLog accessLog = new ReverseProxyAccessLog(
                properties.getAccessLog(), Clock.systemUTC(), 64, ignored -> writer);
        accessLog.start();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ReverseProxyMetrics metrics = new ReverseProxyMetrics(registry);
        ReverseProxyService service = new ReverseProxyService(
                properties,
                client,
                metrics,
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.systemUTC(),
                LaseShadowRuntime.disabled(),
                accessLog);
        return new Fixture(service, accessLog, registry);
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
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/proxy/private?secret=true");
        request.setRequestURI("/proxy/private");
        request.setQueryString("secret=true");
        request.setRemoteAddr("10.1.2.3");
        return request;
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<InputStream> response(int status, byte[] body) {
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(new ByteArrayInputStream(body));
        when(response.headers()).thenReturn(java.net.http.HttpHeaders.of(Map.of(), (name, value) -> true));
        return response;
    }

    private record Fixture(
            ReverseProxyService service,
            ReverseProxyAccessLog accessLog,
            SimpleMeterRegistry registry)
            implements AutoCloseable {
        @Override
        public void close() {
            service.stop();
            accessLog.stop();
        }
    }

    private static class MemoryWriter implements ReverseProxyAccessLog.EventWriter {
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

    private static final class FailingWriter implements ReverseProxyAccessLog.EventWriter {
        private int attempts;

        @Override
        public void append(String line) throws IOException {
            attempts++;
            throw new IOException("synthetic secret writer failure");
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
