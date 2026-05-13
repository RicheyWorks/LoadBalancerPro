package com.richmond423.loadbalancerpro.api.proxy;

import static com.richmond423.loadbalancerpro.api.proxy.PrivateNetworkLiveValidationExecutor.Status.BLOCKED;
import static com.richmond423.loadbalancerpro.api.proxy.PrivateNetworkLiveValidationExecutor.Status.INVALID_REQUEST;
import static com.richmond423.loadbalancerpro.api.proxy.PrivateNetworkLiveValidationExecutor.Status.SUCCESS;
import static com.richmond423.loadbalancerpro.api.proxy.PrivateNetworkLiveValidationGate.Status.ALLOWED;
import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.LOOPBACK_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

class PrivateNetworkLiveValidationExecutorTest {
    private static final Path EXECUTOR_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/PrivateNetworkLiveValidationExecutor.java");
    private static final Path GATE_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/PrivateNetworkLiveValidationGate.java");
    private static final Path APPLICATION_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final Path SMOKE_SCRIPTS = Path.of("scripts/smoke");
    private static final Path POSTMAN_DOCS = Path.of("docs/postman");
    private static final Path GITIGNORE = Path.of(".gitignore");
    private static final Path EVIDENCE_DIR = Path.of("target", "proxy-evidence");
    private static final Path MARKDOWN_EVIDENCE =
            EVIDENCE_DIR.resolve("private-network-live-loopback-validation.md");
    private static final Path JSON_EVIDENCE =
            EVIDENCE_DIR.resolve("private-network-live-loopback-validation.json");
    private static final String API_KEY_SENTINEL = "TEST_PRIVATE_NETWORK_LIVE_API_KEY";

    @Test
    void executesOneLoopbackRequestAndExportsRedactedEvidence() throws Exception {
        try (LiveLoopbackBackend backend = LiveLoopbackBackend.start()) {
            ReverseProxyProperties properties = propertiesWithTarget("loopback-live", backend.baseUrl());
            enableAllLiveGateFlags(properties);
            PrivateNetworkLiveValidationGate gate = new PrivateNetworkLiveValidationGate();
            PrivateNetworkLiveValidationGate.Result gateResult = gate.evaluate(properties);

            assertEquals(ALLOWED, gateResult.status());
            assertTrue(gateResult.allowed());
            assertEquals(LOOPBACK_ALLOWED, gateResult.backendDecisions().get(0).status());
            assertEquals(backend.baseUrl(), gateResult.backendDecisions().get(0).normalizedUrl());

            PrivateNetworkLiveValidationExecutor executor = new PrivateNetworkLiveValidationExecutor(
                    gate, new LoopbackHttpTransport(), Duration.ofSeconds(2));
            PrivateNetworkLiveValidationExecutor.ValidationRequest request =
                    new PrivateNetworkLiveValidationExecutor.ValidationRequest(
                            "GET",
                            "/live-validation/proof?mode=loopback",
                            Map.of(
                                    "X-LoadBalancerPro-Live-Validation", "loopback-only",
                                    "X-Reviewer-Trace", "private-network-live-loopback"),
                            "");

            int requestsBefore = backend.requestCount();
            PrivateNetworkLiveValidationExecutor.Result result =
                    executor.executeFirstAllowed(properties, request);

            assertEquals(SUCCESS, result.status());
            assertTrue(result.success());
            assertEquals(requestsBefore + 1, backend.requestCount());
            CapturedRequest captured = backend.lastRequest();
            assertEquals("GET", captured.method());
            assertEquals("/live-validation/proof?mode=loopback", captured.pathAndQuery());
            assertEquals("loopback-only", captured.liveValidationHeader());
            assertEquals("private-network-live-loopback", captured.reviewerTraceHeader());
            assertEquals(200, result.statusCode());
            assertEquals("upstreams[0].loopback-live", result.backendLabel());
            assertTrue(result.bodySnippet().contains("private-live-loopback-ok"));
            assertTrue(result.headers().getOrDefault("x-private-network-live-proof", List.of())
                    .contains("loopback-only"));
            assertEquals(Duration.ofSeconds(2), result.timeout());

            writeEvidence(gateResult, result, captured);

            String markdown = read(MARKDOWN_EVIDENCE);
            String json = read(JSON_EVIDENCE);
            String combined = markdown + "\n" + json;
            assertAll(
                    () -> assertTrue(markdown.contains("# Private-Network Live Loopback Validation")),
                    () -> assertTrue(markdown.contains("loopbackOnly=true")),
                    () -> assertTrue(markdown.contains("trafficSent=true")),
                    () -> assertTrue(markdown.contains("requestCount=1")),
                    () -> assertTrue(markdown.contains("boundedTimeoutMs=2000")),
                    () -> assertTrue(markdown.contains("target/proxy-evidence/private-network-live-loopback-validation.md")),
                    () -> assertTrue(markdown.contains("target/proxy-evidence/private-network-live-loopback-validation.json")),
                    () -> assertTrue(markdown.contains("API key value: `<REDACTED>`")),
                    () -> assertTrue(json.contains("\"generatedBy\": \"PrivateNetworkLiveValidationExecutorTest\"")),
                    () -> assertTrue(json.contains("\"loopbackOnly\": true")),
                    () -> assertTrue(json.contains("\"trafficSent\": true")),
                    () -> assertTrue(json.contains("\"requestCount\": 1")),
                    () -> assertTrue(json.contains("\"boundedTimeoutMs\": 2000")),
                    () -> assertTrue(json.contains("\"apiKeyRedacted\": \"<REDACTED>\"")),
                    () -> assertTrue(json.contains("\"dnsResolution\": false")),
                    () -> assertTrue(json.contains("\"discovery\": false")),
                    () -> assertTrue(json.contains("\"portScanning\": false")),
                    () -> assertTrue(json.contains("\"postmanExecution\": false")),
                    () -> assertTrue(json.contains("\"smokeExecution\": false")),
                    () -> assertTrue(json.contains("\"releaseDownloadsMutated\": false")),
                    () -> assertFalse(combined.contains(API_KEY_SENTINEL)),
                    () -> assertFalse(combined.contains(backend.baseUrl())),
                    () -> assertFalse(combined.contains("X-API-Key")),
                    () -> assertFalse(combined.contains("Authorization")),
                    () -> assertFalse(combined.contains("Bearer")),
                    () -> assertFalse(combined.contains("release-" + "downloads")));
        }
    }

    @Test
    void blockedGateDoesNotInvokeTransport() {
        ReverseProxyProperties properties = propertiesWithTarget("public", "http://8.8.8.8:18081");
        enableAllLiveGateFlags(properties);
        CountingTransport transport = new CountingTransport();
        PrivateNetworkLiveValidationExecutor executor = new PrivateNetworkLiveValidationExecutor(
                new PrivateNetworkLiveValidationGate(), transport, Duration.ofSeconds(2));

        PrivateNetworkLiveValidationExecutor.Result result =
                executor.executeFirstAllowed(properties,
                        PrivateNetworkLiveValidationExecutor.ValidationRequest.get("/live-validation/proof"));

        assertEquals(BLOCKED, result.status());
        assertEquals(0, transport.count());
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.contains("PUBLIC_NETWORK_REJECTED")));
    }

    @Test
    void invalidValidationRequestDoesNotInvokeTransport() {
        ReverseProxyProperties properties = propertiesWithTarget("local", "http://127.0.0.1:18081");
        enableAllLiveGateFlags(properties);
        CountingTransport transport = new CountingTransport();
        PrivateNetworkLiveValidationExecutor executor = new PrivateNetworkLiveValidationExecutor(
                new PrivateNetworkLiveValidationGate(), transport, Duration.ofSeconds(2));

        PrivateNetworkLiveValidationExecutor.Result result =
                executor.executeFirstAllowed(properties,
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest(
                                "GET", "http://127.0.0.1:18081/bad", Map.of(), ""));

        assertEquals(INVALID_REQUEST, result.status());
        assertEquals(0, transport.count());
        assertTrue(result.reasons().contains("validation request path must be relative"));
    }

    @Test
    void liveExecutorIsNotWiredIntoStartupSmokeOrPostman() throws Exception {
        String executorSource = read(EXECUTOR_SOURCE);
        assertFalse(executorSource.contains("@Component"));
        assertFalse(executorSource.contains("@Service"));
        assertFalse(executorSource.contains("@Bean"));

        try (var sources = Files.walk(Path.of("src/main/java"))) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                boolean containsExecutor = read(source).contains("PrivateNetworkLiveValidationExecutor");
                if (containsExecutor) {
                    assertEquals(EXECUTOR_SOURCE, source, source + " must not wire the live executor");
                }
            }
        }

        String combined = readTree(SMOKE_SCRIPTS) + "\n" + readTree(POSTMAN_DOCS);
        assertFalse(combined.contains("private-network-live-validation"));
        assertFalse(combined.contains("PrivateNetworkLiveValidationExecutor"));
        assertFalse(combined.contains("private-network-live-loopback-validation"));
    }

    @Test
    void productionExecutorAndGateSourcesStayFreeOfNetworkDiscoveryApis() throws Exception {
        String source = read(EXECUTOR_SOURCE) + "\n" + read(GATE_SOURCE);

        for (String forbidden : List.of(
                "InetAddress",
                "getByName",
                "HttpClient",
                "URLConnection",
                "new Socket",
                "DatagramSocket",
                ".connect(",
                "isReachable")) {
            assertFalse(source.contains(forbidden), "live gate/executor source must stay controlled; found " + forbidden);
        }
    }

    @Test
    void evidencePathsStayUnderIgnoredTargetOutputAndDefaultsStayFalse() throws Exception {
        String properties = read(APPLICATION_PROPERTIES);

        assertTrue(MARKDOWN_EVIDENCE.startsWith(Path.of("target")));
        assertTrue(JSON_EVIDENCE.startsWith(Path.of("target")));
        assertTrue(read(GITIGNORE).contains("target/"));
        assertTrue(properties.contains("loadbalancerpro.proxy.private-network-live-validation.enabled=false"));
        assertTrue(properties.contains("loadbalancerpro.proxy.private-network-live-validation.operator-approved=false"));
    }

    private static ReverseProxyProperties propertiesWithTarget(String id, String url) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setUpstreams(List.of(upstream(id, url)));
        return properties;
    }

    private static ReverseProxyProperties.Upstream upstream(String id, String url) {
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId(id);
        upstream.setUrl(url);
        upstream.setWeight(1.0);
        return upstream;
    }

    private static void enableAllLiveGateFlags(ReverseProxyProperties properties) {
        properties.getPrivateNetworkLiveValidation().setEnabled(true);
        properties.getPrivateNetworkLiveValidation().setOperatorApproved(true);
        properties.getPrivateNetworkValidation().setEnabled(true);
    }

    private static void writeEvidence(PrivateNetworkLiveValidationGate.Result gateResult,
                                      PrivateNetworkLiveValidationExecutor.Result result,
                                      CapturedRequest captured) throws IOException {
        Files.createDirectories(EVIDENCE_DIR);
        String markdown = String.join(System.lineSeparator(),
                "# Private-Network Live Loopback Validation",
                "",
                "- Generated by: `PrivateNetworkLiveValidationExecutorTest`",
                "- Evidence Markdown: `target/proxy-evidence/private-network-live-loopback-validation.md`",
                "- Evidence JSON: `target/proxy-evidence/private-network-live-loopback-validation.json`",
                "- loopbackOnly=true",
                "- trafficSent=true",
                "- requestCount=1",
                "- boundedTimeoutMs=" + result.timeout().toMillis(),
                "- gateStatus=`" + gateResult.status() + "`",
                "- classifierStatus=`" + gateResult.backendDecisions().get(0).status() + "`",
                "- backendLabel=`" + result.backendLabel() + "`",
                "- requestPath=`" + captured.pathAndQuery() + "`",
                "- backendHeader `X-LoadBalancerPro-Live-Validation`: `" + captured.liveValidationHeader() + "`",
                "- responseStatus=`" + result.statusCode() + "`",
                "- responseHeader `X-Private-Network-Live-Proof`: `loopback-only`",
                "- responseBodyLabel=`private-live-loopback-ok`",
                "- API key value: `<REDACTED>`",
                "- Safety: JUnit-only, JDK HttpServer loopback fixture, ignored target output, "
                        + "no Postman execution, no smoke execution, no discovery, no scanning",
                "");

        String json = """
                {
                  "generatedBy": "PrivateNetworkLiveValidationExecutorTest",
                  "evidenceOutputScope": "target/proxy-evidence",
                  "markdownEvidence": "target/proxy-evidence/private-network-live-loopback-validation.md",
                  "jsonEvidence": "target/proxy-evidence/private-network-live-loopback-validation.json",
                  "loopbackOnly": true,
                  "trafficSent": true,
                  "requestCount": 1,
                  "boundedTimeoutMs": %d,
                  "gateStatus": "%s",
                  "classifierStatus": "%s",
                  "backendLabel": "%s",
                  "requestPath": "%s",
                  "responseStatus": %d,
                  "responseBodyLabel": "private-live-loopback-ok",
                  "apiKeyRedacted": "<REDACTED>",
                  "safety": {
                    "junitOnly": true,
                    "ignoredTargetOutput": true,
                    "dnsResolution": false,
                    "discovery": false,
                    "portScanning": false,
                    "postmanExecution": false,
                    "smokeExecution": false,
                    "releaseDownloadsMutated": false,
                    "secretPersisted": false
                  }
                }
                """.formatted(
                result.timeout().toMillis(),
                gateResult.status(),
                gateResult.backendDecisions().get(0).status(),
                jsonEscape(result.backendLabel()),
                jsonEscape(captured.pathAndQuery()),
                result.statusCode());

        Files.writeString(MARKDOWN_EVIDENCE, markdown, StandardCharsets.UTF_8);
        Files.writeString(JSON_EVIDENCE, json, StandardCharsets.UTF_8);
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String readTree(Path root) throws IOException {
        assertTrue(Files.exists(root), root + " should exist");
        StringBuilder content = new StringBuilder();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                content.append(read(path)).append('\n');
            }
        }
        return content.toString();
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private record CapturedRequest(
            String method,
            String pathAndQuery,
            String liveValidationHeader,
            String reviewerTraceHeader) {
    }

    private static final class LoopbackHttpTransport implements PrivateNetworkLiveValidationExecutor.Transport {
        private final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        @Override
        public PrivateNetworkLiveValidationExecutor.AttemptResponse send(
                PrivateNetworkLiveValidationExecutor.Attempt attempt) throws IOException, InterruptedException {
            HttpRequest.Builder builder = HttpRequest.newBuilder(attempt.uri())
                    .timeout(attempt.timeout());
            attempt.headers().forEach(builder::header);
            HttpRequest.BodyPublisher body = attempt.body().isBlank()
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(attempt.body(), StandardCharsets.UTF_8);
            HttpRequest request = builder.method(attempt.method(), body).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new PrivateNetworkLiveValidationExecutor.AttemptResponse(
                    response.statusCode(),
                    response.headers().map(),
                    response.body());
        }
    }

    private static final class CountingTransport implements PrivateNetworkLiveValidationExecutor.Transport {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public PrivateNetworkLiveValidationExecutor.AttemptResponse send(
                PrivateNetworkLiveValidationExecutor.Attempt attempt) {
            count.incrementAndGet();
            return new PrivateNetworkLiveValidationExecutor.AttemptResponse(200, Map.of(), "unexpected");
        }

        private int count() {
            return count.get();
        }
    }

    private static final class LiveLoopbackBackend implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;
        private final AtomicInteger requestCount = new AtomicInteger();
        private final AtomicReference<CapturedRequest> lastRequest = new AtomicReference<>();

        private LiveLoopbackBackend(HttpServer server, ExecutorService executor) {
            this.server = server;
            this.executor = executor;
        }

        private static LiveLoopbackBackend start() {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                LiveLoopbackBackend backend = new LiveLoopbackBackend(server, executor);
                server.createContext("/", backend::handle);
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

        private int requestCount() {
            return requestCount.get();
        }

        private CapturedRequest lastRequest() {
            return lastRequest.get();
        }

        private void handle(HttpExchange exchange) throws IOException {
            requestCount.incrementAndGet();
            String liveValidationHeader =
                    exchange.getRequestHeaders().getFirst("X-LoadBalancerPro-Live-Validation");
            String reviewerTraceHeader = exchange.getRequestHeaders().getFirst("X-Reviewer-Trace");
            lastRequest.set(new CapturedRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().toString(),
                    liveValidationHeader == null ? "" : liveValidationHeader,
                    reviewerTraceHeader == null ? "" : reviewerTraceHeader));

            String responseBody = "private-live-loopback-ok";
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.getResponseHeaders().set("X-Private-Network-Live-Proof", "loopback-only");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }

        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
