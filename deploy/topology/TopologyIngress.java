import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** A bounded active-active ingress fixture for executable topology proofs only. */
public final class TopologyIngress {
    private static final int MAX_REQUEST_BYTES = 65_536;
    private static final int MAX_RESPONSE_BYTES = 1_048_576;
    private static final Set<String> RESPONSE_HEADERS = Set.of(
            "content-type", "x-fixture-upstream", "retry-after");

    private TopologyIngress() {
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(environment("TOPOLOGY_INGRESS_PORT", "8443"));
        String apiKey = Files.readString(Path.of(required("TOPOLOGY_API_KEY_FILE")), StandardCharsets.UTF_8).trim();
        if (apiKey.isEmpty()) {
            throw new IllegalArgumentException("topology API key file is empty");
        }
        SSLContext serverTls = serverSslContext(
                required("TOPOLOGY_SERVER_KEYSTORE"), required("TOPOLOGY_SERVER_KEYSTORE_PASSWORD_FILE"));
        SSLContext clientTls = clientSslContext(required("TOPOLOGY_CLIENT_CA"));
        List<Replica> replicas = parseReplicas(required("TOPOLOGY_UPSTREAMS"));
        Duration upstreamTimeout = Duration.ofMillis(boundedIntegerEnvironment(
                "TOPOLOGY_UPSTREAM_TIMEOUT_MILLIS", 500, 100, 2_000));

        HttpClient client = HttpClient.newBuilder()
                .sslContext(clientTls)
                .connectTimeout(upstreamTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        ExecutorService executor = Executors.newFixedThreadPool(64);
        ExecutorService responseReaders = Executors.newFixedThreadPool(32);
        HttpsServer server = HttpsServer.create(new InetSocketAddress("0.0.0.0", port), 128);
        server.setHttpsConfigurator(new HttpsConfigurator(serverTls));
        server.setExecutor(executor);
        server.createContext("/", new IngressHandler(client, responseReaders, replicas, apiKey, upstreamTimeout));
        server.start();

        CountDownLatch stopped = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(2);
            executor.shutdownNow();
            responseReaders.shutdownNow();
            stopped.countDown();
        }, "topology-ingress-shutdown"));
        stopped.await();
    }

    private static SSLContext serverSslContext(String storePath, String passwordPath) throws Exception {
        char[] password = Files.readString(Path.of(passwordPath), StandardCharsets.UTF_8).trim().toCharArray();
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(Path.of(storePath))) {
            store.load(input, password);
        }
        KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(store, password);
        TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(store);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keys.getKeyManagers(), trust.getTrustManagers(), null);
        return context;
    }

    private static SSLContext clientSslContext(String caPath) throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        try (InputStream input = Files.newInputStream(Path.of(caPath))) {
            trustStore.setCertificateEntry("topology-ca", CertificateFactory.getInstance("X.509").generateCertificate(input));
        }
        TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(trustStore);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trust.getTrustManagers(), null);
        return context;
    }

    private static List<Replica> parseReplicas(String value) {
        List<Replica> replicas = new ArrayList<>();
        for (String component : value.split(",")) {
            String[] pair = component.trim().split("=", 2);
            if (pair.length != 2 || !pair[0].matches("[a-z0-9][a-z0-9-]{0,31}")) {
                throw new IllegalArgumentException("invalid topology upstream declaration");
            }
            URI uri = URI.create(pair[1]);
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getPort() < 1
                    || !uri.getPath().isEmpty() && !"/".equals(uri.getPath())) {
                throw new IllegalArgumentException("topology upstreams must be HTTPS origins");
            }
            replicas.add(new Replica(pair[0], uri));
        }
        if (replicas.size() != 2) {
            throw new IllegalArgumentException("topology proof ingress requires exactly two replicas");
        }
        return List.copyOf(replicas);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int boundedIntegerEnvironment(String name, int fallback, int minimum, int maximum) {
        int value = Integer.parseInt(environment(name, Integer.toString(fallback)));
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private record Replica(String id, URI origin) {
    }

    private static final class IngressHandler implements HttpHandler {
        private final HttpClient client;
        private final ExecutorService responseReaders;
        private final List<Replica> replicas;
        private final byte[] apiKey;
        private final Duration upstreamTimeout;
        private final AtomicInteger next = new AtomicInteger();

        private IngressHandler(
                HttpClient client,
                ExecutorService responseReaders,
                List<Replica> replicas,
                String apiKey,
                Duration upstreamTimeout) {
            this.client = client;
            this.responseReaders = responseReaders;
            this.replicas = replicas;
            this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
            this.upstreamTimeout = upstreamTimeout;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!authorized(exchange)) {
                    respond(exchange, 401, "unauthorized", null, null);
                    return;
                }
                if ("/health".equals(exchange.getRequestURI().getPath())) {
                    health(exchange);
                    return;
                }
                if (!exchange.getRequestURI().getPath().startsWith("/proxy/")) {
                    respond(exchange, 404, "not found", null, null);
                    return;
                }
                byte[] body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
                if (body.length > MAX_REQUEST_BYTES) {
                    respond(exchange, 413, "request too large", null, null);
                    return;
                }
                forward(exchange, body);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                respond(exchange, 503, "ingress interrupted", null, null);
            } catch (RuntimeException exception) {
                respond(exchange, 502, "ingress forwarding failure", null, null);
            } finally {
                exchange.close();
            }
        }

        private boolean authorized(HttpExchange exchange) {
            String supplied = exchange.getRequestHeaders().getFirst("X-API-Key");
            return supplied != null && MessageDigest.isEqual(apiKey, supplied.getBytes(StandardCharsets.UTF_8));
        }

        private void health(HttpExchange exchange) throws IOException, InterruptedException {
            int healthy = 0;
            for (Replica replica : replicas) {
                try {
                    HttpResponse<InputStream> response = send(replica, "GET", "/actuator/health", new byte[0]);
                    try (InputStream ignored = response.body()) {
                        if (response.statusCode() == 200) {
                            healthy++;
                        }
                    }
                } catch (IOException ignored) {
                    // A single unavailable replica is a condition reported by this fixture, not a process failure.
                }
            }
            int status = healthy > 0 ? 200 : 503;
            respond(exchange, status, "{\"healthyReplicas\":" + healthy + ",\"configuredReplicas\":2}",
                    "application/json", null);
        }

        private void forward(HttpExchange exchange, byte[] body) throws IOException, InterruptedException {
            int start = Math.floorMod(next.getAndIncrement(), replicas.size());
            boolean retryable = "GET".equals(exchange.getRequestMethod()) || "HEAD".equals(exchange.getRequestMethod());
            IOException lastFailure = null;
            for (int attempt = 0; attempt < replicas.size(); attempt++) {
                Replica replica = replicas.get((start + attempt) % replicas.size());
                try {
                    HttpResponse<InputStream> response = send(
                            replica, exchange.getRequestMethod(), exchange.getRequestURI().toASCIIString(), body);
                    byte[] responseBody = readResponseBody(response);
                    if (responseBody.length > MAX_RESPONSE_BYTES) {
                        respond(exchange, 502, "upstream response too large", null, replica.id());
                        return;
                    }
                    copyResponseHeaders(response, exchange.getResponseHeaders());
                    exchange.getResponseHeaders().set("X-Topology-Replica", replica.id());
                    exchange.sendResponseHeaders(response.statusCode(), responseBody.length);
                    exchange.getResponseBody().write(responseBody);
                    return;
                } catch (IOException exception) {
                    lastFailure = exception;
                    if (!retryable) {
                        break;
                    }
                }
            }
            throw lastFailure == null ? new IOException("no topology replicas configured") : lastFailure;
        }

        private byte[] readResponseBody(HttpResponse<InputStream> response) throws IOException, InterruptedException {
            InputStream input = response.body();
            Future<byte[]> read = responseReaders.submit(() -> {
                try (InputStream owned = input) {
                    return owned.readNBytes(MAX_RESPONSE_BYTES + 1);
                }
            });
            try {
                return read.get(upstreamTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException exception) {
                read.cancel(true);
                IOException failure = new IOException("topology upstream response body timed out", exception);
                try {
                    input.close();
                } catch (IOException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
                throw failure;
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof IOException ioException) {
                    throw ioException;
                }
                throw new IOException("topology upstream response body failed", cause);
            } catch (InterruptedException exception) {
                read.cancel(true);
                try {
                    input.close();
                } catch (IOException closeFailure) {
                    exception.addSuppressed(closeFailure);
                }
                throw exception;
            }
        }

        private HttpResponse<InputStream> send(Replica replica, String method, String path, byte[] body)
                throws IOException, InterruptedException {
            URI target = replica.origin().resolve(path);
            HttpRequest.BodyPublisher publisher = body.length == 0
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofByteArray(body);
            HttpRequest.Builder request = HttpRequest.newBuilder(target)
                    .timeout(upstreamTimeout)
                    .header("X-API-Key", new String(apiKey, StandardCharsets.UTF_8))
                    .method(method, publisher);
            return client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
        }

        private static void copyResponseHeaders(HttpResponse<?> source, Headers destination) {
            source.headers().map().forEach((name, values) -> {
                if (RESPONSE_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                    values.forEach(value -> destination.add(name, value));
                }
            });
        }

        private static void respond(
                HttpExchange exchange, int status, String text, String contentType, String replica) throws IOException {
            byte[] body = text.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type",
                    contentType == null ? "text/plain; charset=utf-8" : contentType);
            if (replica != null) {
                exchange.getResponseHeaders().set("X-Topology-Replica", replica);
            }
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        }
    }
}
