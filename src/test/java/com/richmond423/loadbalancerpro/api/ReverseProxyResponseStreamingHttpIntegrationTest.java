package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReverseProxyResponseStreamingHttpIntegrationTest {
    private static final int LARGE_RESPONSE_BYTES = 8 * 1024 * 1024 + 113;
    private static final StreamingBackend BACKEND = StreamingBackend.start();

    @LocalServerPort
    private int port;

    @Value("${loadbalancerpro.proxy.max-response-bytes}")
    private long maxResponseBytes;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.auth.mode", () -> "none");
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.request-timeout", () -> "10s");
        registry.add("loadbalancerpro.proxy.max-response-bytes", () -> "0");
        registry.add("loadbalancerpro.proxy.upstreams[0].id", () -> "streaming-backend");
        registry.add("loadbalancerpro.proxy.upstreams[0].url", BACKEND::baseUrl);
        registry.add("loadbalancerpro.proxy.upstreams[0].healthy", () -> "true");
    }

    @AfterAll
    static void stopBackend() {
        BACKEND.stop();
    }

    @Test
    void firstDownstreamChunkArrivesBeforeUpstreamCompletionOnRealTomcat() throws Exception {
        StreamingBackend.DelayedExchange delayed = BACKEND.prepareDelayedExchange();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        HttpRequest request = HttpRequest.newBuilder(proxyUri("/delayed"))
                .timeout(Duration.ofSeconds(5))
                .build();

        try {
            HttpResponse<InputStream> response = client.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(200, response.statusCode());
            assertTrue(delayed.firstChunkSent().await(2, TimeUnit.SECONDS));
            byte[] firstChunk = response.body().readNBytes(StreamingBackend.FIRST_CHUNK.length);

            assertArrayEquals(StreamingBackend.FIRST_CHUNK, firstChunk);
            assertFalse(delayed.completed().get(),
                    "downstream must receive the first chunk while the upstream is still open");

            delayed.release().countDown();
            assertArrayEquals(StreamingBackend.SECOND_CHUNK, response.body().readAllBytes());
            assertTrue(delayed.completedLatch().await(2, TimeUnit.SECONDS));
        } finally {
            delayed.release().countDown();
        }
    }

    @Test
    void sseEventsAddLessThanOneHundredMillisecondsPerEventOnRealTomcat() throws Exception {
        StreamingBackend.SseExchange sse = BACKEND.prepareSseExchange();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        HttpRequest request = HttpRequest.newBuilder(proxyUri("/sse"))
                .timeout(Duration.ofSeconds(5))
                .build();
        List<Double> addedLatencyMillis = new ArrayList<>();

        HttpResponse<InputStream> response = client.send(
                request, HttpResponse.BodyHandlers.ofInputStream());
        assertEquals(200, response.statusCode());
        assertEquals("text/event-stream", response.headers().firstValue("Content-Type").orElseThrow());
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            for (int event = 0; event < StreamingBackend.SSE_EVENT_COUNT; event++) {
                String dataLine = reader.readLine();
                assertEquals("data: event-" + event, dataLine);
                assertEquals("", reader.readLine());
                long receivedAt = System.nanoTime();
                long sentAt = sse.awaitSentAt(event);
                addedLatencyMillis.add((receivedAt - sentAt) / 1_000_000.0);
            }
        }

        assertTrue(addedLatencyMillis.stream().allMatch(latency -> latency >= 0 && latency < 100),
                "SSE added latency must remain below 100ms per event: " + addedLatencyMillis);
    }

    @Test
    void largeKnownAndChunkedBinaryResponsesRemainByteIdentical() throws Exception {
        byte[] expected = generatedBytes(LARGE_RESPONSE_BYTES);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpResponse<byte[]> known = client.send(
                HttpRequest.newBuilder(proxyUri("/known-binary")).timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        HttpResponse<byte[]> chunked = client.send(
                HttpRequest.newBuilder(proxyUri("/chunked-binary")).timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(0, maxResponseBytes);
        assertEquals(200, known.statusCode());
        assertEquals(200, chunked.statusCode());
        assertEquals("application/octet-stream", known.headers().firstValue("Content-Type").orElseThrow());
        assertEquals("known", known.headers().firstValue("X-Response-Mode").orElseThrow());
        assertEquals("chunked", chunked.headers().firstValue("X-Response-Mode").orElseThrow());
        assertArrayEquals(expected, known.body());
        assertArrayEquals(expected, chunked.body());
    }

    @Test
    void postCommitUpstreamTruncationAbortsRealTomcatResponseWithoutJsonSplicing() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        HttpResponse<InputStream> response = client.send(
                HttpRequest.newBuilder(proxyUri("/truncated")).timeout(Duration.ofSeconds(5)).build(),
                HttpResponse.BodyHandlers.ofInputStream());
        ByteArrayOutputStream received = new ByteArrayOutputStream();

        assertEquals(200, response.statusCode());
        try {
            byte[] buffer = new byte[128];
            int read;
            while ((read = response.body().read(buffer)) >= 0) {
                received.write(buffer, 0, read);
            }
        } catch (IOException expectedForAbruptContainerClose) {
            // A servlet container may surface the committed-response abort to the client as an early EOF or an error.
        }
        assertArrayEquals(StreamingBackend.TRUNCATED_PREFIX, received.toByteArray());
        assertEquals(Integer.toString(StreamingBackend.TRUNCATED_PREFIX.length + 1024),
                response.headers().firstValue("X-Fixture-Expected-Length").orElseThrow());
        assertFalse(received.toString(StandardCharsets.UTF_8).contains("proxy_upstream_failure"));
    }

    @Test
    void emptyHeadNoContentAndNotModifiedRemainBodyless() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        List<BodylessCase> cases = List.of(
                new BodylessCase("GET", "/empty", 200),
                new BodylessCase("HEAD", "/head", 200),
                new BodylessCase("GET", "/no-content", 204),
                new BodylessCase("GET", "/not-modified", 304));

        for (BodylessCase bodylessCase : cases) {
            HttpRequest request = HttpRequest.newBuilder(proxyUri(bodylessCase.path()))
                    .method(bodylessCase.method(), HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            assertEquals(bodylessCase.status(), response.statusCode());
            assertEquals(0, response.body().length);
            assertEquals("preserved", response.headers().firstValue("X-Bodyless-Fixture").orElseThrow());
        }
    }

    private URI proxyUri(String path) {
        return URI.create("http://127.0.0.1:" + port + "/proxy" + path);
    }

    private static byte[] generatedBytes(int length) {
        byte[] bytes = new byte[length];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) (index * 29 + 7);
        }
        return bytes;
    }

    private record BodylessCase(String method, String path, int status) {
    }

    private static final class StreamingBackend {
        private static final byte[] FIRST_CHUNK = "first-chunk\n".getBytes(StandardCharsets.UTF_8);
        private static final byte[] SECOND_CHUNK = "second-chunk\n".getBytes(StandardCharsets.UTF_8);
        private static final byte[] TRUNCATED_PREFIX = "truncated-prefix\n".getBytes(StandardCharsets.UTF_8);
        private static final int SSE_EVENT_COUNT = 4;

        private final HttpServer server;
        private final ExecutorService executor;
        private volatile DelayedExchange delayedExchange = DelayedExchange.create();
        private volatile SseExchange sseExchange = SseExchange.create();

        private StreamingBackend(HttpServer server, ExecutorService executor) {
            this.server = server;
            this.executor = executor;
        }

        private static StreamingBackend start() {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newCachedThreadPool();
                StreamingBackend backend = new StreamingBackend(server, executor);
                server.createContext("/delayed", backend::delayed);
                server.createContext("/sse", backend::sse);
                server.createContext("/known-binary", exchange -> backend.binary(exchange, true));
                server.createContext("/chunked-binary", exchange -> backend.binary(exchange, false));
                server.createContext("/truncated", backend::truncated);
                server.createContext("/empty", exchange -> backend.bodyless(exchange, 200));
                server.createContext("/head", exchange -> backend.bodyless(exchange, 200));
                server.createContext("/no-content", exchange -> backend.bodyless(exchange, 204));
                server.createContext("/not-modified", exchange -> backend.bodyless(exchange, 304));
                server.setExecutor(executor);
                server.start();
                return backend;
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private DelayedExchange prepareDelayedExchange() {
            DelayedExchange prepared = DelayedExchange.create();
            delayedExchange = prepared;
            return prepared;
        }

        private SseExchange prepareSseExchange() {
            SseExchange prepared = SseExchange.create();
            sseExchange = prepared;
            return prepared;
        }

        private void delayed(HttpExchange exchange) throws IOException {
            DelayedExchange state = delayedExchange;
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(FIRST_CHUNK);
                output.flush();
                state.firstChunkSent().countDown();
                if (!state.release().await(4, TimeUnit.SECONDS)) {
                    throw new IOException("delayed fixture release timed out");
                }
                output.write(SECOND_CHUNK);
                output.flush();
                state.completed().set(true);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("delayed fixture interrupted", exception);
            } finally {
                state.completedLatch().countDown();
                exchange.close();
            }
        }

        private void sse(HttpExchange exchange) throws IOException {
            SseExchange state = sseExchange;
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream output = exchange.getResponseBody()) {
                for (int event = 0; event < SSE_EVENT_COUNT; event++) {
                    byte[] bytes = ("data: event-" + event + "\n\n").getBytes(StandardCharsets.UTF_8);
                    state.recordSentAt(System.nanoTime());
                    output.write(bytes);
                    output.flush();
                    if (event + 1 < SSE_EVENT_COUNT) {
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            throw new IOException("SSE fixture interrupted", exception);
                        }
                    }
                }
            } finally {
                exchange.close();
            }
        }

        private void binary(HttpExchange exchange, boolean knownLength) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.getResponseHeaders().set("X-Response-Mode", knownLength ? "known" : "chunked");
            exchange.sendResponseHeaders(200, knownLength ? LARGE_RESPONSE_BYTES : 0);
            try (OutputStream output = exchange.getResponseBody()) {
                byte[] chunk = new byte[8192];
                int written = 0;
                while (written < LARGE_RESPONSE_BYTES) {
                    int count = Math.min(chunk.length, LARGE_RESPONSE_BYTES - written);
                    for (int index = 0; index < count; index++) {
                        chunk[index] = (byte) ((written + index) * 29 + 7);
                    }
                    output.write(chunk, 0, count);
                    written += count;
                }
            } finally {
                exchange.close();
            }
        }

        private void bodyless(HttpExchange exchange, int status) throws IOException {
            exchange.getResponseHeaders().set("X-Bodyless-Fixture", "preserved");
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        }

        private void truncated(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.getResponseHeaders().set(
                    "X-Fixture-Expected-Length", Integer.toString(TRUNCATED_PREFIX.length + 1024));
            exchange.sendResponseHeaders(200, TRUNCATED_PREFIX.length + 1024L);
            OutputStream output = exchange.getResponseBody();
            output.write(TRUNCATED_PREFIX);
            output.flush();
            exchange.close();
        }

        private void stop() {
            delayedExchange.release().countDown();
            server.stop(0);
            executor.shutdownNow();
        }

        private record DelayedExchange(
                CountDownLatch firstChunkSent,
                CountDownLatch release,
                AtomicBoolean completed,
                CountDownLatch completedLatch) {
            private static DelayedExchange create() {
                return new DelayedExchange(
                        new CountDownLatch(1), new CountDownLatch(1),
                        new AtomicBoolean(), new CountDownLatch(1));
            }
        }

        private static final class SseExchange {
            private final List<Long> sentAtNanos = new ArrayList<>();

            private static SseExchange create() {
                return new SseExchange();
            }

            private synchronized void recordSentAt(long sentAt) {
                sentAtNanos.add(sentAt);
                notifyAll();
            }

            private synchronized long awaitSentAt(int index) throws InterruptedException {
                long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
                while (sentAtNanos.size() <= index && System.nanoTime() < deadline) {
                    wait(10);
                }
                if (sentAtNanos.size() <= index) {
                    throw new AssertionError("SSE fixture did not publish timestamp for event " + index);
                }
                return sentAtNanos.get(index);
            }
        }
    }
}
