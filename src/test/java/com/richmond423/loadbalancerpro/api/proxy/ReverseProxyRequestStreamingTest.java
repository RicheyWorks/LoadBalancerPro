package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.mock.web.MockHttpServletRequest;

class ReverseProxyRequestStreamingTest {
    private static final long ONE_GIB = 1_073_741_824L;
    private static final int REQUEST_LIMIT = 65_536;

    @Test
    void declaredOneGigabyteBodyIsRejectedBeforeOpeningTheStream() {
        AtomicBoolean streamOpened = new AtomicBoolean();
        StreamingRequest request = request(
                ONE_GIB,
                false,
                () -> {
                    streamOpened.set(true);
                    throw new AssertionError("oversized declared body must not be opened");
                });
        ReverseProxyService service = service(properties("http://127.0.0.1:9"), new ReverseProxyMetrics());
        try {
            ReverseProxyResponse response = service.forward(request);

            assertEquals(413, response.statusCode());
            assertFalse(streamOpened.get());
            assertTrue(new String(response.body(), StandardCharsets.UTF_8)
                    .contains("proxy_payload_too_large"));
            assertEquals(0, service.recentDecisionsSnapshot().retainedCount(),
                    "declared oversize must fail before routing or upstream contact");
        } finally {
            service.closeHealthProber();
        }
    }

    @Test
    void unknownLengthPostStreamsByteIdenticallyToLoopbackBackend() {
        byte[] body = new byte[REQUEST_LIMIT];
        for (int index = 0; index < body.length; index++) {
            body[index] = (byte) (index * 31);
        }
        try (RequestBackend backend = RequestBackend.start()) {
            ReverseProxyService service = service(properties(backend.url()), new ReverseProxyMetrics());
            try {
                ReverseProxyResponse response = service.forward(request(
                        -1,
                        true,
                        () -> new ByteArrayInputStream(body)));

                assertEquals(200, response.statusCode());
                assertArrayEquals(body, backend.lastBody());
                assertEquals(1, backend.requestCount());
            } finally {
                service.closeHealthProber();
            }
        }
    }

    @Test
    void chunkedOverflowAbortsAtCapWithoutRetryOrUpstreamPenalty() {
        try (RequestBackend first = RequestBackend.start();
             RequestBackend second = RequestBackend.start()) {
            ReverseProxyProperties properties = properties(first.url(), second.url());
            ReverseProxyProperties.Retry retry = properties.getRetry();
            retry.setEnabled(true);
            retry.setMaxAttempts(2);
            retry.setRetryNonIdempotent(true);
            retry.setMethods(Set.of("POST"));
            ReverseProxyProperties.Cooldown cooldown = properties.getCooldown();
            cooldown.setEnabled(true);
            cooldown.setConsecutiveFailureThreshold(1);
            cooldown.setDuration(Duration.ofMinutes(5));
            ReverseProxyMetrics metrics = new ReverseProxyMetrics();
            ReverseProxyService service = service(properties, metrics);
            try {
                ReverseProxyResponse response = service.forward(request(
                        -1,
                        true,
                        () -> new GeneratedInputStream(REQUEST_LIMIT + 1L)));

                assertEquals(413, response.statusCode());
                assertTrue(first.requestCount() + second.requestCount() <= 1,
                        "one-shot streaming bodies must dispatch to at most one upstream");
                ReverseProxyStatusResponse status = service.statusSnapshot();
                assertTrue(status.upstreams().stream().allMatch(upstream ->
                                upstream.consecutiveFailures() == 0 && !upstream.cooldownActive()),
                        "client overflow must not penalize upstream resilience state");
                assertEquals(0, status.retry().budgetGrantedRetries());
                assertEquals(1, service.recentDecisionsSnapshot().retainedCount(),
                        "streaming overflow must preserve the single authoritative live decision");
                assertEquals(413, service.recentDecisionsSnapshot().decisions().get(0).responseStatus());
            } finally {
                service.closeHealthProber();
            }
        }
    }

    private static ReverseProxyService service(ReverseProxyProperties properties, ReverseProxyMetrics metrics) {
        return new ReverseProxyService(
                properties,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                metrics,
                RoutingStrategyRegistry.defaultRegistry(),
                Clock.systemUTC());
    }

    private static ReverseProxyProperties properties(String... urls) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setMaxRequestBytes(REQUEST_LIMIT);
        for (int index = 0; index < urls.length; index++) {
            ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
            upstream.setId("backend-" + index);
            upstream.setUrl(urls[index]);
            properties.getUpstreams().add(upstream);
        }
        return properties;
    }

    private static StreamingRequest request(long declaredLength,
                                            boolean chunked,
                                            InputStreamSupplier inputStreamSupplier) {
        StreamingRequest request = new StreamingRequest(declaredLength, inputStreamSupplier);
        request.setContextPath("");
        request.setRequestURI("/proxy/upload");
        request.setMethod("POST");
        request.setRemoteAddr("127.0.0.1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        if (chunked) {
            request.addHeader("Transfer-Encoding", "chunked");
        }
        return request;
    }

    @FunctionalInterface
    private interface InputStreamSupplier {
        InputStream open();
    }

    private static final class StreamingRequest extends MockHttpServletRequest {
        private final long declaredLength;
        private final InputStreamSupplier inputStreamSupplier;

        private StreamingRequest(long declaredLength, InputStreamSupplier inputStreamSupplier) {
            this.declaredLength = declaredLength;
            this.inputStreamSupplier = inputStreamSupplier;
        }

        @Override
        public long getContentLengthLong() {
            return declaredLength;
        }

        @Override
        public int getContentLength() {
            return declaredLength > Integer.MAX_VALUE ? -1 : (int) declaredLength;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new InputStreamAdapter(inputStreamSupplier.open());
        }
    }

    private static final class InputStreamAdapter extends ServletInputStream {
        private final InputStream delegate;
        private boolean finished;

        private InputStreamAdapter(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            finished = value == -1;
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            int count = delegate.read(bytes, offset, length);
            finished = count == -1;
            return count;
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("asynchronous reads are not used by the proxy");
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    private static final class GeneratedInputStream extends InputStream {
        private long remaining;

        private GeneratedInputStream(long length) {
            this.remaining = length;
        }

        @Override
        public int read() {
            if (remaining == 0) {
                return -1;
            }
            remaining--;
            return 0x5a;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            if (remaining == 0) {
                return -1;
            }
            int count = (int) Math.min(length, remaining);
            java.util.Arrays.fill(bytes, offset, offset + count, (byte) 0x5a);
            remaining -= count;
            return count;
        }
    }

    private static final class RequestBackend implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;
        private final AtomicInteger requestCount = new AtomicInteger();
        private final AtomicReference<byte[]> lastBody = new AtomicReference<>(new byte[0]);

        private RequestBackend(HttpServer server, ExecutorService executor) {
            this.server = server;
            this.executor = executor;
        }

        private static RequestBackend start() {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                ExecutorService executor = Executors.newCachedThreadPool();
                RequestBackend backend = new RequestBackend(server, executor);
                server.createContext("/", backend::handle);
                server.setExecutor(executor);
                server.start();
                return backend;
            } catch (IOException exception) {
                throw new IllegalStateException("failed to start loopback request backend", exception);
            }
        }

        private String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private int requestCount() {
            return requestCount.get();
        }

        private byte[] lastBody() {
            return lastBody.get();
        }

        private void handle(HttpExchange exchange) throws IOException {
            requestCount.incrementAndGet();
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            try (InputStream input = exchange.getRequestBody()) {
                byte[] buffer = new byte[4_096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    body.write(buffer, 0, count);
                }
                lastBody.set(body.toByteArray());
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().close();
            } catch (IOException exception) {
                lastBody.set(body.toByteArray());
                exchange.close();
            }
        }

        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
