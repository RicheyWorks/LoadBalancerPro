package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReverseProxyRequestStreamingHttpIntegrationTest {
    private static final int REQUEST_LIMIT = 65_536;
    private static final LoopbackBackend BACKEND = LoopbackBackend.start();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.auth.mode", () -> "none");
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.max-request-bytes", () -> Integer.toString(REQUEST_LIMIT));
        registry.add("loadbalancerpro.proxy.upstreams[0].id", () -> "stream-backend");
        registry.add("loadbalancerpro.proxy.upstreams[0].url", BACKEND::url);
    }

    @AfterAll
    static void stopBackend() {
        BACKEND.close();
    }

    @Test
    void realChunkedPostIsByteIdenticalAndOverflowReturns413() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        byte[] withinLimit = new byte[REQUEST_LIMIT];
        for (int index = 0; index < withinLimit.length; index++) {
            withinLimit[index] = (byte) (index * 17);
        }
        HttpResponse<byte[]> accepted = client.send(
                request(HttpRequest.BodyPublishers.ofInputStream(
                        () -> new java.io.ByteArrayInputStream(withinLimit))),
                HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(200, accepted.statusCode());
        assertArrayEquals(withinLimit, accepted.body());

        int requestsBeforeOverflow = BACKEND.requestCount();
        HttpResponse<String> rejected = client.send(
                request(HttpRequest.BodyPublishers.ofInputStream(
                        () -> new GeneratedInputStream(REQUEST_LIMIT + 1L))),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(413, rejected.statusCode());
        assertTrue(rejected.body().contains("proxy_payload_too_large"));
        assertEquals(requestsBeforeOverflow + 1, BACKEND.requestCount(),
                "chunked overflow must produce one aborted upstream attempt");
    }

    private HttpRequest request(HttpRequest.BodyPublisher publisher) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/proxy/upload"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/octet-stream")
                .POST(publisher)
                .build();
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
            return 0x41;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            if (remaining == 0) {
                return -1;
            }
            int count = (int) Math.min(length, remaining);
            java.util.Arrays.fill(bytes, offset, offset + count, (byte) 0x41);
            remaining -= count;
            return count;
        }
    }

    private static final class LoopbackBackend implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;
        private final AtomicInteger requestCount = new AtomicInteger();

        private LoopbackBackend(HttpServer server, ExecutorService executor) {
            this.server = server;
            this.executor = executor;
        }

        private static LoopbackBackend start() {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                ExecutorService executor = Executors.newCachedThreadPool();
                LoopbackBackend backend = new LoopbackBackend(server, executor);
                server.createContext("/", backend::handle);
                server.setExecutor(executor);
                server.start();
                return backend;
            } catch (IOException exception) {
                throw new IllegalStateException("failed to start loopback streaming backend", exception);
            }
        }

        private String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private int requestCount() {
            return requestCount.get();
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
                byte[] response = body.toByteArray();
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.getResponseBody().close();
            } catch (IOException exception) {
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
