package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ReverseProxyResponseStreamingTest {
    @Test
    void knownAndUnknownLengthBinaryBodiesStreamByteIdentically() throws Exception {
        byte[] expected = new byte[4 * 1024 * 1024 + 31];
        for (int index = 0; index < expected.length; index++) {
            expected[index] = (byte) (index * 37);
        }

        for (boolean knownLength : List.of(true, false)) {
            TrackingInputStream body = new TrackingInputStream(
                    new ChunkedByteArrayInputStream(expected, 997));
            Map<String, List<String>> headers = knownLength
                    ? Map.of("Content-Length", List.of(Integer.toString(expected.length)))
                    : Map.of();
            ReverseProxyService service = service(
                    properties("alpha"), client(response(200, headers, body)));
            MockHttpServletResponse downstream = new MockHttpServletResponse();
            try {
                ReverseProxyResponse metadata = service.forward(request("GET"), downstream);

                assertEquals(200, metadata.statusCode());
                assertArrayEquals(expected, downstream.getContentAsByteArray());
                assertTrue(body.closed());
            } finally {
                service.closeHealthProber();
            }
        }
    }

    @Test
    void preCommitReadFailureRetriesWithoutMixingResponses() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Queue<HttpResponse<InputStream>> responses = new ArrayDeque<>();
        responses.add(response(200, Map.of(), new FailingBeforeFirstByteInputStream()));
        responses.add(response(200, Map.of(),
                new ByteArrayInputStream("second-attempt-only".getBytes(StandardCharsets.UTF_8))));
        HttpClient client = mock(HttpClient.class);
        when(client.send(
                any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenAnswer(ignored -> {
                    calls.incrementAndGet();
                    return responses.remove();
                });
        ReverseProxyProperties properties = properties("alpha", "beta");
        enableRetry(properties);
        ReverseProxyService service = service(properties, client);
        MockHttpServletResponse downstream = new MockHttpServletResponse();
        try {
            service.forward(request("GET"), downstream);

            assertEquals(2, calls.get());
            assertEquals("second-attempt-only", downstream.getContentAsString());
            assertEquals(1, service.statusSnapshot().metrics().totalFailures());
            assertEquals(1, service.statusSnapshot().metrics().totalRetryAttempts());
            assertEquals(2, service.recentDecisionsSnapshot().totalCaptured());
        } finally {
            service.closeHealthProber();
        }
    }

    @Test
    void connectPhaseFailureRetriesBeforeDownstreamCommitment() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpClient client = mock(HttpClient.class);
        when(client.send(
                any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenAnswer(ignored -> {
                    if (calls.getAndIncrement() == 0) {
                        throw new IOException("fixture connect failure");
                    }
                    return response(200, Map.of(),
                            new ByteArrayInputStream("connected-second".getBytes(StandardCharsets.UTF_8)));
                });
        ReverseProxyProperties properties = properties("alpha", "beta");
        enableRetry(properties);
        ReverseProxyService service = service(properties, client);
        MockHttpServletResponse downstream = new MockHttpServletResponse();
        try {
            service.forward(request("GET"), downstream);

            assertEquals(2, calls.get());
            assertEquals("connected-second", downstream.getContentAsString());
            assertEquals(1, service.statusSnapshot().metrics().totalFailures());
            assertEquals(1, service.statusSnapshot().metrics().totalRetryAttempts());
        } finally {
            service.closeHealthProber();
        }
    }

    @Test
    void completeHttpErrorResponseIsNotAStreamingTransportFailure() throws Exception {
        ReverseProxyService service = service(
                properties("alpha"),
                client(response(500, Map.of(),
                        new ByteArrayInputStream("complete-error".getBytes(StandardCharsets.UTF_8)))));
        MockHttpServletResponse downstream = new MockHttpServletResponse();
        try {
            service.forward(request("GET"), downstream);

            assertEquals(500, downstream.getStatus());
            assertEquals("complete-error", downstream.getContentAsString());
            assertEquals(1, service.statusSnapshot().metrics().totalForwarded());
            assertEquals(0, service.statusSnapshot().metrics().totalFailures());
        } finally {
            service.closeHealthProber();
        }
    }

    @Test
    void postCommitUpstreamFailureAbortsWithoutRetryOrErrorSplicing() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpClient client = mock(HttpClient.class);
        when(client.send(
                any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenAnswer(ignored -> {
                    calls.incrementAndGet();
                    if (calls.get() == 1) {
                        return response(200, Map.of(), new PrefixThenFailureInputStream("prefix-only"));
                    }
                    return response(200, Map.of(),
                            new ByteArrayInputStream("must-not-appear".getBytes(StandardCharsets.UTF_8)));
                });
        ReverseProxyProperties properties = properties("alpha", "beta");
        enableRetry(properties);
        properties.getCooldown().setEnabled(true);
        properties.getCooldown().setConsecutiveFailureThreshold(1);
        properties.getCooldown().setDuration(Duration.ofMinutes(1));
        ReverseProxyService service = service(properties, client);
        MockHttpServletResponse downstream = new MockHttpServletResponse();
        try {
            assertThrows(IOException.class, () -> service.forward(request("GET"), downstream));

            assertEquals(1, calls.get());
            assertEquals("prefix-only", downstream.getContentAsString());
            assertFalse(downstream.getContentAsString().contains("proxy_upstream_failure"));
            assertEquals(1, service.statusSnapshot().metrics().totalFailures());
            ReverseProxyStatusResponse.UpstreamStatus alpha = service.statusSnapshot().upstreams().stream()
                    .filter(upstream -> upstream.id().equals("alpha"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(1, alpha.consecutiveFailures());
            assertTrue(alpha.cooldownActive());
            assertEquals("upstream_stream_failure_after_commit",
                    service.recentDecisionsSnapshot().decisions().get(0).outcome());
        } finally {
            service.closeHealthProber();
        }
    }

    @Test
    void downstreamDisconnectClosesUpstreamWithoutHealthPenalty() throws Exception {
        TrackingInputStream body = new TrackingInputStream(
                new ByteArrayInputStream(new byte[64 * 1024]));
        ReverseProxyService service = service(
                properties("alpha"), client(response(200, Map.of(), body)));
        HttpServletResponse downstream = mock(HttpServletResponse.class);
        when(downstream.getOutputStream()).thenReturn(new DisconnectingServletOutputStream());
        try {
            assertThrows(IOException.class, () -> service.forward(request("GET"), downstream));

            assertTrue(body.closed());
            ReverseProxyStatusResponse status = service.statusSnapshot();
            assertEquals(0, status.metrics().totalFailures());
            ReverseProxyStatusResponse.UpstreamStatus alpha = status.upstreams().get(0);
            assertEquals(0, alpha.consecutiveFailures());
            assertEquals(0, alpha.runtimeStats().inFlightRequestCount());
            assertEquals(0, alpha.runtimeStats().completedRequestCount());
            assertEquals("downstream_disconnect",
                    service.recentDecisionsSnapshot().decisions().get(0).outcome());
        } finally {
            service.closeHealthProber();
        }
    }

    @Test
    void emptyHeadNoContentAndNotModifiedResponsesDoNotLeakBodies() throws Exception {
        List<ResponseCase> cases = List.of(
                new ResponseCase("GET", 200, new byte[0], true),
                new ResponseCase("HEAD", 200, "forbidden".getBytes(StandardCharsets.UTF_8), false),
                new ResponseCase("GET", 204, "forbidden".getBytes(StandardCharsets.UTF_8), false),
                new ResponseCase("GET", 304, "forbidden".getBytes(StandardCharsets.UTF_8), false));
        for (ResponseCase responseCase : cases) {
            TrackingInputStream body = new TrackingInputStream(
                    new ByteArrayInputStream(responseCase.body()));
            ReverseProxyService service = service(
                    properties("alpha"), client(response(responseCase.status(), Map.of(), body)));
            MockHttpServletResponse downstream = new MockHttpServletResponse();
            try {
                service.forward(request(responseCase.method()), downstream);

                assertEquals(responseCase.status(), downstream.getStatus());
                assertEquals(0, downstream.getContentAsByteArray().length);
                assertTrue(body.closed());
                assertEquals(responseCase.readExpected(), body.readAttempted());
            } finally {
                service.closeHealthProber();
            }
        }
    }

    @Test
    void responseLimitRejectsBeforeCommitAndAbortsAfterCommitWithoutRetry() throws Exception {
        ReverseProxyProperties knownProperties = properties("alpha");
        knownProperties.setMaxResponseBytes(10);
        TrackingInputStream knownBody = new TrackingInputStream(
                new ByteArrayInputStream(new byte[11]));
        ReverseProxyService knownService = service(knownProperties, client(response(
                200, Map.of("Content-Length", List.of("11")), knownBody)));
        MockHttpServletResponse knownDownstream = new MockHttpServletResponse();
        try {
            knownService.forward(request("GET"), knownDownstream);
            assertEquals(502, knownDownstream.getStatus());
            assertTrue(knownDownstream.getContentAsString().contains("proxy_response_too_large"));
            assertFalse(knownBody.readAttempted());
            assertTrue(knownBody.closed());
            assertEquals(0, knownService.statusSnapshot().upstreams().get(0).consecutiveFailures());
        } finally {
            knownService.closeHealthProber();
        }

        ReverseProxyProperties chunkedProperties = properties("alpha", "beta");
        chunkedProperties.setMaxResponseBytes(10);
        enableRetry(chunkedProperties);
        AtomicInteger calls = new AtomicInteger();
        HttpClient chunkedClient = mock(HttpClient.class);
        when(chunkedClient.send(
                any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenAnswer(ignored -> {
                    calls.incrementAndGet();
                    return response(200, Map.of(), new TwoChunkInputStream(8, 3));
                });
        ReverseProxyService chunkedService = service(chunkedProperties, chunkedClient);
        MockHttpServletResponse chunkedDownstream = new MockHttpServletResponse();
        try {
            assertThrows(IOException.class,
                    () -> chunkedService.forward(request("GET"), chunkedDownstream));
            assertEquals(1, calls.get());
            assertEquals(8, chunkedDownstream.getContentAsByteArray().length);
            assertFalse(chunkedDownstream.getContentAsString().contains("proxy_response_too_large"));
            assertEquals(0, chunkedService.statusSnapshot().upstreams().get(0).consecutiveFailures());
        } finally {
            chunkedService.closeHealthProber();
        }
    }

    @Test
    void negativeResponseLimitFailsClosed() {
        ReverseProxyProperties properties = properties("alpha");
        properties.setMaxResponseBytes(-1);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service(properties, mock(HttpClient.class)));
        assertTrue(exception.getMessage().contains("max-response-bytes"));
    }

    private static ReverseProxyService service(
            ReverseProxyProperties properties, HttpClient client) {
        return new ReverseProxyService(
                properties,
                client,
                new ReverseProxyMetrics(),
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.systemUTC());
    }

    private static HttpClient client(HttpResponse<InputStream> response) throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.send(
                any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(response);
        return client;
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<InputStream> response(
            int status, Map<String, List<String>> headers, InputStream body) {
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (name, value) -> true));
        when(response.body()).thenReturn(body);
        return response;
    }

    private static ReverseProxyProperties properties(String... upstreamIds) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setStrategy("ROUND_ROBIN");
        List<ReverseProxyProperties.Upstream> upstreams = new ArrayList<>();
        for (int index = 0; index < upstreamIds.length; index++) {
            ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
            upstream.setId(upstreamIds[index]);
            upstream.setUrl("http://127.0.0.1:" + (19080 + index));
            upstream.setHealthy(true);
            upstreams.add(upstream);
        }
        properties.setUpstreams(upstreams);
        return properties;
    }

    private static void enableRetry(ReverseProxyProperties properties) {
        properties.getRetry().setEnabled(true);
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setBudgetPercent(100);
        properties.getRetry().getBackoff().setBase(Duration.ZERO);
        properties.getRetry().getBackoff().setMax(Duration.ZERO);
    }

    private static MockHttpServletRequest request(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/proxy/stream");
        request.setContextPath("");
        request.setServletPath("");
        request.setRemoteAddr("127.0.0.1");
        request.setRemotePort(39000);
        request.setLocalPort(8080);
        request.setScheme("http");
        request.addHeader("Host", "127.0.0.1:8080");
        return request;
    }

    private record ResponseCase(String method, int status, byte[] body, boolean readExpected) {
    }

    private static final class ChunkedByteArrayInputStream extends ByteArrayInputStream {
        private final int maximumChunk;

        private ChunkedByteArrayInputStream(byte[] bytes, int maximumChunk) {
            super(bytes);
            this.maximumChunk = maximumChunk;
        }

        @Override
        public synchronized int read(byte[] bytes, int offset, int length) {
            return super.read(bytes, offset, Math.min(length, maximumChunk));
        }
    }

    private static final class FailingBeforeFirstByteInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException("fixture failure before first byte");
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            throw new IOException("fixture failure before first byte");
        }
    }

    private static final class PrefixThenFailureInputStream extends InputStream {
        private final byte[] prefix;
        private boolean emitted;

        private PrefixThenFailureInputStream(String prefix) {
            this.prefix = prefix.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int read = read(one, 0, 1);
            return read < 0 ? -1 : one[0] & 0xff;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (emitted) {
                throw new IOException("fixture failure after prefix");
            }
            emitted = true;
            System.arraycopy(prefix, 0, bytes, offset, prefix.length);
            return prefix.length;
        }
    }

    private static final class TwoChunkInputStream extends InputStream {
        private final Queue<Integer> chunks = new ArrayDeque<>();

        private TwoChunkInputStream(int... sizes) {
            for (int size : sizes) {
                chunks.add(size);
            }
        }

        @Override
        public int read() {
            return chunks.isEmpty() ? -1 : 0;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            if (chunks.isEmpty()) {
                return -1;
            }
            int count = Math.min(length, chunks.remove());
            for (int index = 0; index < count; index++) {
                bytes[offset + index] = (byte) index;
            }
            return count;
        }
    }

    private static final class TrackingInputStream extends InputStream {
        private final InputStream delegate;
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicBoolean readAttempted = new AtomicBoolean();

        private TrackingInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            readAttempted.set(true);
            return delegate.read();
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            readAttempted.set(true);
            return delegate.read(bytes, offset, length);
        }

        @Override
        public void close() throws IOException {
            closed.set(true);
            delegate.close();
        }

        private boolean closed() {
            return closed.get();
        }

        private boolean readAttempted() {
            return readAttempted.get();
        }
    }

    private static final class DisconnectingServletOutputStream extends ServletOutputStream {
        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }

        @Override
        public void write(int value) throws IOException {
            throw new IOException("fixture downstream disconnect");
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            throw new IOException("fixture downstream disconnect");
        }
    }
}
