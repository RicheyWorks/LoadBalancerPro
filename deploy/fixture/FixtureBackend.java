import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class FixtureBackend {
    private static final int PORT = 8080;
    private static final int MAX_REQUEST_BYTES = 1_048_576;
    private static final int MAX_RESPONSE_BYTES = 1_048_576;
    private static final long MAX_DELAY_MILLIS = 10_000;
    private static final int MAX_REQUEST_THREADS = 32;
    private static final int MAX_PENDING_REQUESTS = 256;

    private FixtureBackend() {
    }

    public static void main(String[] args) throws Exception {
        String backendId = requiredEnvironment("FIXTURE_ID");
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                MAX_REQUEST_THREADS,
                MAX_REQUEST_THREADS,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(MAX_PENDING_REQUESTS),
                new ThreadPoolExecutor.CallerRunsPolicy());
        executor.allowCoreThreadTimeOut(true);
        CountDownLatch stopped = new CountDownLatch(1);
        server.createContext("/", exchange -> handle(exchange, backendId));
        server.setExecutor(executor);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            executor.shutdownNow();
            stopped.countDown();
        }, "fixture-backend-shutdown"));
        server.start();
        System.out.println(backendId + " listening on 0.0.0.0:" + PORT);
        stopped.await();
    }

    private static void handle(HttpExchange exchange, String backendId) throws IOException {
        try (exchange) {
            try {
                URI uri = exchange.getRequestURI();
                if ("/health".equals(uri.getPath())) {
                    respond(exchange, backendId, 200, backendId + " healthy");
                    return;
                }
                if ("/slow".equals(uri.getPath())) {
                    pause(delayMillis(uri, backendId));
                }
                byte[] requestBody = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
                if (requestBody.length > MAX_REQUEST_BYTES) {
                    respond(exchange, backendId, 413, backendId + " request too large");
                    return;
                }
                String response = backendId + " handled " + exchange.getRequestMethod() + " " + uri.getPath();
                respond(exchange, backendId, 200, response, responseBytes(uri, response.length()));
            } catch (IllegalArgumentException exception) {
                respond(exchange, backendId, 400, backendId + " invalid fixture parameter");
            }
        }
    }

    private static long delayMillis(URI uri, String backendId) {
        String query = uri.getRawQuery();
        if (query == null) {
            return 2_500;
        }
        String slowBackend = null;
        for (String component : query.split("&")) {
            if (component.startsWith("slowBackend=")) {
                slowBackend = component.substring("slowBackend=".length());
            }
        }
        if (slowBackend != null && !slowBackend.equals(backendId)) {
            return 0;
        }
        for (String component : query.split("&")) {
            if (component.startsWith("millis=")) {
                long delay = Long.parseLong(component.substring("millis=".length()));
                if (delay < 0 || delay > MAX_DELAY_MILLIS) {
                    throw new IllegalArgumentException("delay out of range");
                }
                return delay;
            }
        }
        return 2_500;
    }

    private static int responseBytes(URI uri, int defaultBytes) {
        String query = uri.getRawQuery();
        if (query == null) {
            return defaultBytes;
        }
        for (String component : query.split("&")) {
            if (component.startsWith("lbpResponseBytes=")) {
                int bytes = Integer.parseInt(component.substring("lbpResponseBytes=".length()));
                if (bytes < 0 || bytes > MAX_RESPONSE_BYTES) {
                    throw new IllegalArgumentException("response size out of range");
                }
                return bytes;
            }
        }
        return defaultBytes;
    }

    private static void pause(long delayMillis) throws IOException {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("fixture delay interrupted", exception);
        }
    }

    private static void respond(HttpExchange exchange, String backendId, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        respond(exchange, backendId, status, bytes);
    }

    private static void respond(
            HttpExchange exchange, String backendId, int status, String body, int responseBytes) throws IOException {
        byte[] prefix = body.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[responseBytes];
        Arrays.fill(bytes, (byte) 'x');
        System.arraycopy(prefix, 0, bytes, 0, Math.min(prefix.length, bytes.length));
        respond(exchange, backendId, status, bytes);
    }

    private static void respond(HttpExchange exchange, String backendId, int status, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.getResponseHeaders().set("X-Fixture-Upstream", backendId);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured");
        }
        return value.trim();
    }
}
