import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FixtureBackend {
    private static final int PORT = 8080;
    private static final int MAX_REQUEST_BYTES = 1_048_576;
    private static final long MAX_DELAY_MILLIS = 10_000;

    private FixtureBackend() {
    }

    public static void main(String[] args) throws Exception {
        String backendId = requiredEnvironment("FIXTURE_ID");
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        ExecutorService executor = Executors.newCachedThreadPool();
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
                    pause(delayMillis(uri));
                }
                byte[] requestBody = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
                if (requestBody.length > MAX_REQUEST_BYTES) {
                    respond(exchange, backendId, 413, backendId + " request too large");
                    return;
                }
                respond(exchange, backendId, 200,
                        backendId + " handled " + exchange.getRequestMethod() + " " + uri.getPath());
            } catch (IllegalArgumentException exception) {
                respond(exchange, backendId, 400, backendId + " invalid delay");
            }
        }
    }

    private static long delayMillis(URI uri) {
        String query = uri.getRawQuery();
        if (query == null) {
            return 2_500;
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
