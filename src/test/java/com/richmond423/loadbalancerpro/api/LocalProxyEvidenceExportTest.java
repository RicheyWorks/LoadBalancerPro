package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.profiles.active=prod",
        "loadbalancerpro.api.key=TEST_PROXY_EVIDENCE_KEY"
})
@AutoConfigureMockMvc
class LocalProxyEvidenceExportTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String API_KEY = "TEST_PROXY_EVIDENCE_KEY";
    private static final String RELEASE_DOWNLOADS_PATH = "release-" + "downloads";
    private static final Path EVIDENCE_DIR = Path.of("target", "proxy-evidence");
    private static final Path MARKDOWN_EVIDENCE = EVIDENCE_DIR.resolve("local-proxy-evidence.md");
    private static final Path JSON_EVIDENCE = EVIDENCE_DIR.resolve("local-proxy-evidence.json");
    private static final EvidenceBackend BACKEND = EvidenceBackend.start("evidence-local-backend");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void proxyProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancerpro.proxy.enabled", () -> "true");
        registry.add("loadbalancerpro.proxy.strategy", () -> "ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.routes.evidence.path-prefix", () -> "/evidence");
        registry.add("loadbalancerpro.proxy.routes.evidence.strategy", () -> "ROUND_ROBIN");
        registry.add("loadbalancerpro.proxy.routes.evidence.targets[0].id", () -> "evidence-local-backend");
        registry.add("loadbalancerpro.proxy.routes.evidence.targets[0].url", BACKEND::baseUrl);
        registry.add("loadbalancerpro.proxy.routes.evidence.targets[0].healthy", () -> "true");
    }

    @AfterAll
    static void stopBackend() {
        BACKEND.stop();
    }

    @Test
    void exportsMarkdownAndJsonEvidenceForLocalOnlyProxyForwarding() throws Exception {
        int requestsBefore = BACKEND.requestCount();

        MvcResult missingProxyKey = mockMvc.perform(post("/proxy/evidence/review?trace=local-only")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("payload=source-visible"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        assertEquals(requestsBefore, BACKEND.requestCount(),
                "Prod API-key boundary must reject unauthenticated proxy calls before the backend is reached.");

        MvcResult proxied = mockMvc.perform(post("/proxy/evidence/review?trace=local-only")
                        .header("X-API-Key", API_KEY)
                        .header("X-Reviewer-Trace", "evidence-export")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("payload=source-visible"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-LoadBalancerPro-Upstream", "evidence-local-backend"))
                .andExpect(header().string("X-LoadBalancerPro-Strategy", "ROUND_ROBIN"))
                .andExpect(header().string("X-Local-Proxy-Evidence", "evidence-local-backend"))
                .andExpect(header().string("X-Backend-Fixture-Mode", "loopback-only"))
                .andExpect(content().string(containsString("id=evidence-local-backend")))
                .andExpect(content().string(containsString("method=POST")))
                .andExpect(content().string(containsString("uri=/evidence/review?trace=local-only")))
                .andExpect(content().string(containsString("body=payload=source-visible")))
                .andExpect(content().string(containsString("x-reviewer-trace=evidence-export")))
                .andReturn();

        EvidenceRequest capturedRequest = BACKEND.lastRequest();
        assertNotNull(capturedRequest, "Loopback backend should capture the authenticated proxied request.");

        MvcResult missingStatusKey = mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        MvcResult statusWithKey = mockMvc.perform(get("/api/proxy/status").header("X-API-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(API_KEY))))
                .andExpect(jsonPath("$.proxyEnabled").value(true))
                .andExpect(jsonPath("$.securityBoundary.authMode").value("api-key"))
                .andExpect(jsonPath("$.securityBoundary.apiKeyConfigured").value(true))
                .andExpect(jsonPath("$.securityBoundary.proxyStatusProtected").value(true))
                .andExpect(jsonPath("$.securityBoundary.proxyForwardingProtected").value(true))
                .andReturn();

        writeEvidence(missingProxyKey, proxied, capturedRequest, missingStatusKey, statusWithKey);

        String markdown = Files.readString(MARKDOWN_EVIDENCE, StandardCharsets.UTF_8);
        String json = Files.readString(JSON_EVIDENCE, StandardCharsets.UTF_8);
        JsonNode evidence = JSON.readTree(json);
        assertAll(
                () -> assertTrue(markdown.contains("Local Proxy Evidence")),
                () -> assertTrue(markdown.contains("backend started on loopback")),
                () -> assertTrue(markdown.contains("/proxy/evidence/review?trace=local-only")),
                () -> assertTrue(markdown.contains("/evidence/review?trace=local-only")),
                () -> assertTrue(markdown.contains("X-LoadBalancerPro-Upstream")),
                () -> assertTrue(markdown.contains("missing proxy API key status")),
                () -> assertTrue(markdown.contains("missing proxy status API key status")),
                () -> assertTrue(markdown.contains("authenticated proxy status: `200`")),
                () -> assertTrue(markdown.contains("API key value: `<REDACTED>`")),
                () -> assertTrue(json.contains("\"generatedBy\": \"LocalProxyEvidenceExportTest\"")),
                () -> assertTrue(json.contains("\"evidenceOutputScope\": \"target/proxy-evidence\"")),
                () -> assertTrue(json.contains("\"loopbackOnly\": true")),
                () -> assertTrue(json.contains("\"prodApiKeyMode\": true")),
                () -> assertTrue(json.contains("\"missingProxyApiKeyStatus\": 401")),
                () -> assertTrue(json.contains("\"authenticatedStatusRequestStatus\": 200")),
                () -> assertTrue(json.contains("\"backendReceived\": true")),
                () -> assertTrue(json.contains("\"apiKeyRedacted\": \"<REDACTED>\"")),
                () -> assertEquals("LocalProxyEvidenceExportTest", evidence.path("generatedBy").asText()),
                () -> assertEquals("target/proxy-evidence", evidence.path("evidenceOutputScope").asText()),
                () -> assertEquals("evidence-local-backend", evidence.path("backend").path("id").asText()),
                () -> assertEquals("127.0.0.1", evidence.path("backend").path("host").asText()),
                () -> assertEquals("java-assigned-ephemeral",
                        evidence.path("backend").path("portPolicy").asText()),
                () -> assertTrue(evidence.path("backend").path("startedOnLoopback").asBoolean()),
                () -> assertEquals("POST", evidence.path("proxyRequest").path("method").asText()),
                () -> assertEquals("/proxy/evidence/review?trace=local-only",
                        evidence.path("proxyRequest").path("pathAndQuery").asText()),
                () -> assertTrue(evidence.path("backendRequest").path("backendReceived").asBoolean()),
                () -> assertEquals("/evidence/review?trace=local-only",
                        evidence.path("backendRequest").path("pathAndQuery").asText()),
                () -> assertEquals(202, evidence.path("proxyResponse").path("status").asInt()),
                () -> assertTrue(evidence.path("securityBoundary").path("prodApiKeyMode").asBoolean()),
                () -> assertEquals(401,
                        evidence.path("securityBoundary").path("missingProxyApiKeyStatus").asInt()),
                () -> assertEquals(401,
                        evidence.path("securityBoundary").path("missingStatusApiKeyStatus").asInt()),
                () -> assertEquals(200,
                        evidence.path("securityBoundary").path("authenticatedStatusRequestStatus").asInt()),
                () -> assertEquals("<REDACTED>",
                        evidence.path("securityBoundary").path("apiKeyRedacted").asText()),
                () -> assertTrue(evidence.path("safety").path("loopbackOnly").asBoolean()),
                () -> assertTrue(evidence.path("safety").path("sourceVisible").asBoolean()),
                () -> assertFalse(evidence.path("safety").path("nativeTools").asBoolean()),
                () -> assertFalse(evidence.path("safety").path("downloads").asBoolean()),
                () -> assertFalse(evidence.path("safety").path("portScanning").asBoolean()),
                () -> assertFalse(evidence.path("safety").path("persistence").asBoolean()),
                () -> assertFalse(markdown.contains(API_KEY)),
                () -> assertFalse(json.contains(API_KEY)),
                () -> assertFalse(markdown.contains("http://127.0.0.1:")),
                () -> assertFalse(json.contains("http://127.0.0.1:")),
                () -> assertFalse(markdown.contains(RELEASE_DOWNLOADS_PATH)),
                () -> assertFalse(json.contains(RELEASE_DOWNLOADS_PATH))
        );
        PrivateNetworkEvidenceRedactor.assertNoSensitiveEvidence(
                markdown + "\n" + json,
                API_KEY,
                RELEASE_DOWNLOADS_PATH,
                "http://127.0.0.1:");
    }

    private static void writeEvidence(MvcResult missingProxyKey,
                                      MvcResult proxied,
                                      EvidenceRequest capturedRequest,
                                      MvcResult missingStatusKey,
                                      MvcResult statusWithKey) throws IOException {
        Files.createDirectories(EVIDENCE_DIR);
        String upstream = proxied.getResponse().getHeader("X-LoadBalancerPro-Upstream");
        String strategy = proxied.getResponse().getHeader("X-LoadBalancerPro-Strategy");
        String localEvidenceHeader = proxied.getResponse().getHeader("X-Local-Proxy-Evidence");
        String responseBody = proxied.getResponse().getContentAsString();

        String markdown = String.join(System.lineSeparator(),
                "# Local Proxy Evidence",
                "",
                "- Generated by: `LocalProxyEvidenceExportTest`",
                "- Evidence output scope: `target/proxy-evidence`",
                "- Backend fixture: backend started on loopback with Java-assigned ephemeral port",
                "- Proxy request: `POST /proxy/evidence/review?trace=local-only`",
                "- Backend received: `POST " + capturedRequest.pathAndQuery() + "`",
                "- Proxy response status: `" + proxied.getResponse().getStatus() + "`",
                "- Proxy response header `X-LoadBalancerPro-Upstream`: `" + upstream + "`",
                "- Proxy response header `X-LoadBalancerPro-Strategy`: `" + strategy + "`",
                "- Backend evidence header `X-Local-Proxy-Evidence`: `" + localEvidenceHeader + "`",
                "- Backend response body includes: `"
                        + singleLine(responseBody.replace("payload=source-visible", "<source-visible-payload>")) + "`",
                "- missing proxy API key status: `" + missingProxyKey.getResponse().getStatus() + "`",
                "- missing proxy status API key status: `" + missingStatusKey.getResponse().getStatus() + "`",
                "- authenticated proxy status: `" + statusWithKey.getResponse().getStatus()
                        + "` with `proxyEnabled=true`, `authMode=api-key`, and `apiKeyConfigured=true`",
                "- API key value: `<REDACTED>`",
                "- Safety: loopback only, source-visible test fixture, no native tools, no downloads, no scanning, "
                        + "no persistence, no scheduled tasks, no service installation",
                "");

        String json = """
                {
                  "generatedBy": "LocalProxyEvidenceExportTest",
                  "evidenceOutputScope": "target/proxy-evidence",
                  "backend": {
                    "id": "evidence-local-backend",
                    "host": "127.0.0.1",
                    "portPolicy": "java-assigned-ephemeral",
                    "startedOnLoopback": true
                  },
                  "proxyRequest": {
                    "method": "POST",
                    "pathAndQuery": "/proxy/evidence/review?trace=local-only"
                  },
                  "backendRequest": {
                    "backendReceived": true,
                    "method": "%s",
                    "pathAndQuery": "%s",
                    "body": "%s",
                    "reviewerTraceHeader": "%s"
                  },
                  "proxyResponse": {
                    "status": %d,
                    "upstreamHeader": "%s",
                    "strategyHeader": "%s",
                    "backendEvidenceHeader": "%s",
                    "bodyContainsForwardedPayload": %s
                  },
                  "securityBoundary": {
                    "prodApiKeyMode": true,
                    "missingProxyApiKeyStatus": %d,
                    "missingStatusApiKeyStatus": %d,
                    "authenticatedStatusRequestStatus": %d,
                    "apiKeyRedacted": "<REDACTED>"
                  },
                  "safety": {
                    "loopbackOnly": true,
                    "sourceVisible": true,
                    "nativeTools": false,
                    "downloads": false,
                    "portScanning": false,
                    "persistence": false,
                    "scheduledTasks": false,
                    "serviceInstallation": false
                  }
                }
                """.formatted(
                jsonEscape(capturedRequest.method()),
                jsonEscape(capturedRequest.pathAndQuery()),
                jsonEscape(capturedRequest.body()),
                jsonEscape(capturedRequest.reviewerTrace()),
                proxied.getResponse().getStatus(),
                jsonEscape(upstream),
                jsonEscape(strategy),
                jsonEscape(localEvidenceHeader),
                responseBody.contains("payload=source-visible"),
                missingProxyKey.getResponse().getStatus(),
                missingStatusKey.getResponse().getStatus(),
                statusWithKey.getResponse().getStatus());

        Files.writeString(MARKDOWN_EVIDENCE, markdown, StandardCharsets.UTF_8);
        Files.writeString(JSON_EVIDENCE, json, StandardCharsets.UTF_8);
    }

    private static String singleLine(String value) {
        return value.replace("\r", "").replace("\n", "; ");
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private record EvidenceRequest(String method, String pathAndQuery, String body, String reviewerTrace) {
    }

    private static final class EvidenceBackend {
        private final String id;
        private final HttpServer server;
        private final ExecutorService executor;
        private final AtomicInteger requestCount = new AtomicInteger();
        private final AtomicReference<EvidenceRequest> lastRequest = new AtomicReference<>();

        private EvidenceBackend(String id, HttpServer server, ExecutorService executor) {
            this.id = id;
            this.server = server;
            this.executor = executor;
        }

        private static EvidenceBackend start(String id) {
            try {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                EvidenceBackend backend = new EvidenceBackend(id, server, executor);
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

        private EvidenceRequest lastRequest() {
            return lastRequest.get();
        }

        private void stop() {
            server.stop(0);
            executor.shutdownNow();
        }

        private void handle(HttpExchange exchange) throws IOException {
            requestCount.incrementAndGet();
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            String body = new String(requestBody, StandardCharsets.UTF_8);
            String reviewerTrace = exchange.getRequestHeaders().getFirst("X-Reviewer-Trace");
            lastRequest.set(new EvidenceRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().toString(),
                    body,
                    reviewerTrace == null ? "" : reviewerTrace));

            String responseBody = "id=" + id
                    + "\nmethod=" + exchange.getRequestMethod()
                    + "\nuri=" + exchange.getRequestURI()
                    + "\nbody=" + body
                    + "\nx-reviewer-trace=" + (reviewerTrace == null ? "" : reviewerTrace);
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.getResponseHeaders().set("X-Local-Proxy-Evidence", id);
            exchange.getResponseHeaders().set("X-Backend-Fixture-Mode", "loopback-only");
            exchange.sendResponseHeaders(202, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }
    }
}
