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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.api.PrivateNetworkEvidenceRedactor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

class PrivateNetworkLiveValidationExecutorTest {
    private static final ObjectMapper JSON = new ObjectMapper();
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
    private static final String AUTH_HEADER_SENTINEL = "Bearer TEST_PRIVATE_NETWORK_LIVE_BEARER_TOKEN";
    private static final String COOKIE_SENTINEL = "SESSION=TEST_PRIVATE_NETWORK_LIVE_COOKIE";
    private static final String TOKEN_SENTINEL = "TEST_PRIVATE_NETWORK_LIVE_TOKEN";
    private static final String PUBLIC_REDIRECT_LOCATION = "http://public.example/live-validation-redirect";

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
                            "/live-validation/proof",
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
            assertEquals("/live-validation/proof", captured.pathAndQuery());
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
            JsonNode evidence = JSON.readTree(json);
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
                    () -> assertTrue(json.contains("\"broaderPrivateLanValidation\": false")),
                    () -> assertTrue(json.contains("\"dnsResolution\": false")),
                    () -> assertTrue(json.contains("\"discovery\": false")),
                    () -> assertTrue(json.contains("\"portScanning\": false")),
                    () -> assertTrue(json.contains("\"postmanExecution\": false")),
                    () -> assertTrue(json.contains("\"smokeExecution\": false")),
                    () -> assertTrue(json.contains("\"releaseDownloadsMutated\": false")),
                    () -> assertEquals("PrivateNetworkLiveValidationExecutorTest",
                            evidence.path("generatedBy").asText()),
                    () -> assertEquals("target/proxy-evidence", evidence.path("evidenceOutputScope").asText()),
                    () -> assertEquals("target/proxy-evidence/private-network-live-loopback-validation.md",
                            evidence.path("markdownEvidence").asText()),
                    () -> assertEquals("target/proxy-evidence/private-network-live-loopback-validation.json",
                            evidence.path("jsonEvidence").asText()),
                    () -> assertTrue(evidence.path("loopbackOnly").asBoolean()),
                    () -> assertTrue(evidence.path("trafficSent").asBoolean()),
                    () -> assertEquals(1, evidence.path("requestCount").asInt()),
                    () -> assertEquals(2000, evidence.path("boundedTimeoutMs").asInt()),
                    () -> assertEquals("ALLOWED", evidence.path("gateStatus").asText()),
                    () -> assertEquals("LOOPBACK_ALLOWED", evidence.path("classifierStatus").asText()),
                    () -> assertEquals("upstreams[0].loopback-live", evidence.path("backendLabel").asText()),
                    () -> assertEquals("/live-validation/proof", evidence.path("requestPath").asText()),
                    () -> assertEquals(200, evidence.path("responseStatus").asInt()),
                    () -> assertEquals("private-live-loopback-ok", evidence.path("responseBodyLabel").asText()),
                    () -> assertEquals("<REDACTED>", evidence.path("apiKeyRedacted").asText()),
                    () -> assertFalse(evidence.path("broaderPrivateLanValidation").asBoolean()),
                    () -> assertTrue(evidence.path("safety").path("junitOnly").asBoolean()),
                    () -> assertTrue(evidence.path("safety").path("ignoredTargetOutput").asBoolean()),
                    () -> assertFalse(evidence.path("safety").path("dnsResolution").asBoolean()),
                    () -> assertFalse(evidence.path("safety").path("discovery").asBoolean()),
                    () -> assertFalse(evidence.path("safety").path("portScanning").asBoolean()),
                    () -> assertFalse(evidence.path("safety").path("postmanExecution").asBoolean()),
                    () -> assertFalse(evidence.path("safety").path("smokeExecution").asBoolean()),
                    () -> assertFalse(evidence.path("safety").path("releaseDownloadsMutated").asBoolean()),
                    () -> assertFalse(evidence.path("safety").path("secretPersisted").asBoolean()),
                    () -> assertFalse(combined.contains(API_KEY_SENTINEL)),
                    () -> assertFalse(combined.contains(AUTH_HEADER_SENTINEL)),
                    () -> assertFalse(combined.contains(COOKIE_SENTINEL)),
                    () -> assertFalse(combined.contains(TOKEN_SENTINEL)),
                    () -> assertFalse(combined.contains(PUBLIC_REDIRECT_LOCATION)),
                    () -> assertFalse(combined.contains(backend.baseUrl())),
                    () -> assertFalse(combined.contains("X-API-Key")),
                    () -> assertFalse(combined.contains("Authorization")),
                    () -> assertFalse(combined.contains("Bearer")),
                    () -> assertFalse(combined.contains("Cookie")),
                    () -> assertFalse(combined.contains("Set-Cookie")),
                    () -> assertFalse(combined.contains("release-" + "downloads")));
            PrivateNetworkEvidenceRedactor.assertNoSensitiveEvidence(
                    combined,
                    API_KEY_SENTINEL,
                    AUTH_HEADER_SENTINEL,
                    COOKIE_SENTINEL,
                    TOKEN_SENTINEL,
                    PUBLIC_REDIRECT_LOCATION,
                    backend.baseUrl(),
                    "release-" + "downloads");
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
    void invalidValidationRequestsDoNotInvokeTransport() {
        List<InvalidRequestCase> cases = List.of(
                new InvalidRequestCase(null, "validation request is required"),
                new InvalidRequestCase(
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest("GET", null, Map.of(), ""),
                        "validation request path must not be blank"),
                new InvalidRequestCase(
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest("GET", "", Map.of(), ""),
                        "validation request path must not be blank"),
                new InvalidRequestCase(
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest("GET", " ", Map.of(), ""),
                        "validation request path must not be blank"),
                new InvalidRequestCase(
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest(
                                "GET", "http://127.0.0.1:18081/bad", Map.of(), ""),
                        "validation request path must be relative"),
                new InvalidRequestCase(
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest(
                                "GET", "//public.example/bad", Map.of(), ""),
                        "validation request path must be relative"),
                new InvalidRequestCase(
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest(
                                "GET", "/safe/../private", Map.of(), ""),
                        "validation request path must not contain traversal segments"),
                new InvalidRequestCase(
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest(
                                "GET", "/safe/%2e%2e/private", Map.of(), ""),
                        "validation request path must not contain traversal segments"),
                new InvalidRequestCase(
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest(
                                "GET", "/safe/%2f/private", Map.of(), ""),
                        "validation request path must not contain traversal segments"),
                new InvalidRequestCase(
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest(
                                "GET", "/safe\\private", Map.of(), ""),
                        "validation request path must not contain backslash characters"),
                new InvalidRequestCase(
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest(
                                "GET", "/live-validation/proof#fragment", Map.of(), ""),
                        "validation request path must not include a fragment"),
                new InvalidRequestCase(
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest(
                                "GET", "/live-validation/proof?token=" + TOKEN_SENTINEL, Map.of(), ""),
                        "validation request path must not include a query string"),
                new InvalidRequestCase(
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest(
                                "GET", "/live-validation/proof\r\nX-Injected: true", Map.of(), ""),
                        "validation request path must not contain control characters"),
                new InvalidRequestCase(
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest(
                                "GET", "/live-validation/%0dproof", Map.of(), ""),
                        "validation request path must not contain encoded control characters"));

        for (InvalidRequestCase requestCase : cases) {
            ReverseProxyProperties properties = propertiesWithTarget("local", "http://127.0.0.1:18081");
            enableAllLiveGateFlags(properties);
            CountingTransport transport = new CountingTransport();
            PrivateNetworkLiveValidationExecutor executor = new PrivateNetworkLiveValidationExecutor(
                    new PrivateNetworkLiveValidationGate(), transport, Duration.ofSeconds(2));

            PrivateNetworkLiveValidationExecutor.Result result =
                    executor.executeFirstAllowed(properties, requestCase.request());

            assertEquals(INVALID_REQUEST, result.status(), requestCase.reason());
            assertEquals(0, transport.count(), requestCase.reason());
            assertTrue(result.reasons().contains(requestCase.reason()),
                    () -> "path=" + requestCase.pathForMessage()
                            + ", expected=" + requestCase.reason()
                            + ", actual=" + result.reasons());
        }
    }

    @Test
    void validationHeadersAndResponseHeadersAreSafetyFiltered() {
        ReverseProxyProperties properties = propertiesWithTarget("local", "http://127.0.0.1:18081");
        enableAllLiveGateFlags(properties);
        CapturingTransport transport = new CapturingTransport(new PrivateNetworkLiveValidationExecutor.AttemptResponse(
                200,
                Map.of(
                        "Content-Type", List.of("text/plain; charset=utf-8"),
                        "X-Private-Network-Live-Proof", List.of("loopback-only"),
                        "Location", List.of(PUBLIC_REDIRECT_LOCATION),
                        "Set-Cookie", List.of(COOKIE_SENTINEL),
                        "Authorization", List.of(AUTH_HEADER_SENTINEL),
                        "X-API-Key", List.of(API_KEY_SENTINEL)),
                "header-filter-ok"));
        PrivateNetworkLiveValidationExecutor executor = new PrivateNetworkLiveValidationExecutor(
                new PrivateNetworkLiveValidationGate(), transport, Duration.ofSeconds(2));

        PrivateNetworkLiveValidationExecutor.Result result =
                executor.executeFirstAllowed(properties,
                        new PrivateNetworkLiveValidationExecutor.ValidationRequest(
                                "GET",
                                "/live-validation/proof",
                                Map.of(
                                        "X-LoadBalancerPro-Live-Validation", "loopback-only",
                                        "X-Reviewer-Trace", "private-network-live-loopback",
                                        "Authorization", AUTH_HEADER_SENTINEL,
                                        "X-API-Key", API_KEY_SENTINEL,
                                        "Cookie", COOKIE_SENTINEL,
                                        "X-Auth-Token", TOKEN_SENTINEL),
                                ""));

        assertEquals(SUCCESS, result.status());
        assertEquals(1, transport.count());
        Map<String, String> sentHeaders = transport.lastAttempt().headers();
        assertEquals("loopback-only", sentHeaders.get("X-LoadBalancerPro-Live-Validation"));
        assertEquals("private-network-live-loopback", sentHeaders.get("X-Reviewer-Trace"));
        assertFalse(sentHeaders.containsKey("Authorization"));
        assertFalse(sentHeaders.containsKey("X-API-Key"));
        assertFalse(sentHeaders.containsKey("Cookie"));
        assertFalse(sentHeaders.containsKey("X-Auth-Token"));
        assertTrue(result.headers().containsKey("content-type"));
        assertTrue(result.headers().containsKey("x-private-network-live-proof"));
        assertFalse(result.headers().containsKey("location"));
        assertFalse(result.headers().containsKey("set-cookie"));
        assertFalse(result.headers().containsKey("authorization"));
        assertFalse(result.headers().containsKey("x-api-key"));
    }

    @Test
    void loopbackRedirectIsReportedWithoutFollowingPublicLocation() throws Exception {
        try (LiveLoopbackBackend backend = LiveLoopbackBackend.startRedirect()) {
            ReverseProxyProperties properties = propertiesWithTarget("loopback-redirect", backend.baseUrl());
            enableAllLiveGateFlags(properties);
            PrivateNetworkLiveValidationExecutor executor = new PrivateNetworkLiveValidationExecutor(
                    new PrivateNetworkLiveValidationGate(), new LoopbackHttpTransport(), Duration.ofSeconds(2));

            PrivateNetworkLiveValidationExecutor.Result result =
                    executor.executeFirstAllowed(properties,
                            PrivateNetworkLiveValidationExecutor.ValidationRequest.get("/redirect-proof"));

            assertEquals(SUCCESS, result.status());
            assertEquals(302, result.statusCode());
            assertEquals(1, backend.requestCount());
            assertEquals("/redirect-proof", backend.lastRequest().pathAndQuery());
            assertFalse(result.headers().containsKey("location"));
            assertFalse(result.bodySnippet().contains(PUBLIC_REDIRECT_LOCATION));
        }
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

        String testSource = read(Path.of(
                "src/test/java/com/richmond423/loadbalancerpro/api/proxy/PrivateNetworkLiveValidationExecutorTest.java"));
        assertFalse(testSource.contains("http://10" + "."));
        assertFalse(testSource.contains("http://192" + ".168."));
        assertFalse(testSource.contains("http://172" + ".16."));
        assertFalse(testSource.contains("http://172" + ".31."));
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
                "- broaderPrivateLanValidation=false",
                "- API key value: `<REDACTED>`",
                "- Safety: JUnit-only, JDK HttpServer loopback fixture, ignored target output, "
                        + "no Postman execution, no smoke execution, no discovery, no scanning, "
                        + "no broader private-LAN execution",
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
                  "broaderPrivateLanValidation": false,
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

    private record InvalidRequestCase(
            PrivateNetworkLiveValidationExecutor.ValidationRequest request,
            String reason) {
        private String pathForMessage() {
            return request == null ? "<null>" : request.pathAndQuery();
        }
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

    private static final class CapturingTransport implements PrivateNetworkLiveValidationExecutor.Transport {
        private final AtomicInteger count = new AtomicInteger();
        private final AtomicReference<PrivateNetworkLiveValidationExecutor.Attempt> lastAttempt =
                new AtomicReference<>();
        private final PrivateNetworkLiveValidationExecutor.AttemptResponse response;

        private CapturingTransport(PrivateNetworkLiveValidationExecutor.AttemptResponse response) {
            this.response = response;
        }

        @Override
        public PrivateNetworkLiveValidationExecutor.AttemptResponse send(
                PrivateNetworkLiveValidationExecutor.Attempt attempt) {
            count.incrementAndGet();
            lastAttempt.set(attempt);
            return response;
        }

        private int count() {
            return count.get();
        }

        private PrivateNetworkLiveValidationExecutor.Attempt lastAttempt() {
            return lastAttempt.get();
        }
    }

    private static final class LiveLoopbackBackend implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;
        private final boolean redirect;
        private final AtomicInteger requestCount = new AtomicInteger();
        private final AtomicReference<CapturedRequest> lastRequest = new AtomicReference<>();

        private LiveLoopbackBackend(HttpServer server, ExecutorService executor, boolean redirect) {
            this.server = server;
            this.executor = executor;
            this.redirect = redirect;
        }

        private static LiveLoopbackBackend start() {
            return start(false);
        }

        private static LiveLoopbackBackend startRedirect() {
            return start(true);
        }

        private static LiveLoopbackBackend start(boolean redirect) {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                LiveLoopbackBackend backend = new LiveLoopbackBackend(server, executor, redirect);
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

            if (redirect) {
                String responseBody = "redirect-not-followed";
                byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Location", PUBLIC_REDIRECT_LOCATION);
                exchange.getResponseHeaders().set("Set-Cookie", COOKIE_SENTINEL);
                exchange.sendResponseHeaders(302, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
                return;
            }

            String responseBody = "private-live-loopback-ok";
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.getResponseHeaders().set("X-Private-Network-Live-Proof", "loopback-only");
            exchange.getResponseHeaders().set("Set-Cookie", COOKIE_SENTINEL);
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
