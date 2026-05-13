package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PostmanEnterpriseLabCollectionTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path COLLECTION =
            Path.of("docs/postman/LoadBalancerPro Enterprise Lab.postman_collection.json");
    private static final Path ENVIRONMENT =
            Path.of("docs/postman/LoadBalancerPro Local.postman_environment.json");
    private static final Path DOC = Path.of("docs/POSTMAN_COLLECTION.md");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\\\"<>]+");
    private static final List<Pattern> SECRET_PATTERNS = List.of(
            Pattern.compile("AKIA[0-9A-Z]{16}"),
            Pattern.compile("(?i)aws_secret_access_key\\s*[:=]"),
            Pattern.compile("(?i)client_secret\\s*[:=]"),
            Pattern.compile("(?i)ghp_[A-Za-z0-9_]{20,}"),
            Pattern.compile("(?i)xox[baprs]-[A-Za-z0-9-]{20,}"),
            Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----"));
    private static final Set<String> ALLOWED_ENDPOINTS = Set.of(
            "/",
            "/api/health",
            "/actuator/health",
            "/v3/api-docs",
            "/swagger-ui/index.html",
            "/api/routing/compare",
            "/api/allocate/capacity-aware",
            "/api/allocate/evaluate",
            "/api/scenarios/replay",
            "/api/remediation/report",
            "/api/proxy/status",
            "/api/proxy/reload",
            "/api/evidence-training/onboarding",
            "/api/evidence-training/scorecards",
            "/api/evidence-training/scorecards/strict-zero-drift-pass",
            "/api/evidence-training/scorecards/strict-zero-drift-pass/answer-template",
            "/api/evidence-training/scorecards/grade");

    @Test
    void collectionAndEnvironmentExistAndParse() throws Exception {
        assertTrue(Files.exists(COLLECTION), "enterprise lab Postman collection should exist");
        assertTrue(Files.exists(ENVIRONMENT), "local Postman environment should exist");

        JsonNode collection = readJson(COLLECTION);
        JsonNode environment = readJson(ENVIRONMENT);

        assertEquals("LoadBalancerPro Enterprise Lab", collection.at("/info/name").asText());
        assertEquals("https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
                collection.at("/info/schema").asText());
        assertEquals("LoadBalancerPro Local", environment.path("name").asText());
        assertEquals("environment", environment.path("_postman_variable_scope").asText());
    }

    @Test
    void environmentUsesPlaceholdersOnly() throws Exception {
        JsonNode environment = readJson(ENVIRONMENT);
        Map<String, String> values = stream(environment.path("values")).stream()
                .collect(Collectors.toMap(value -> value.path("key").asText(), value -> value.path("value").asText()));

        assertEquals("http://localhost:8080", values.get("baseUrl"));
        assertEquals("<API_KEY>", values.get("apiKey"));
        assertEquals("local", values.get("profile"));
        assertEquals("WEIGHTED_LEAST_CONNECTIONS", values.get("strategy"));
        assertEquals("normal-baseline", values.get("scenarioName"));
        assertEquals("MARKDOWN", values.get("reportFormat"));

        String normalized = read(ENVIRONMENT).toLowerCase(Locale.ROOT);
        assertFalse(normalized.contains("currentvalue"));
        assertFalse(normalized.contains("bearer"));
        assertFalse(normalized.contains("authorization"));
    }

    @Test
    void collectionContainsExpectedFoldersAndUsesBaseUrl() throws Exception {
        JsonNode collection = readJson(COLLECTION);
        List<String> folderNames = stream(collection.path("item")).stream()
                .map(item -> item.path("name").asText())
                .toList();

        assertEquals(List.of(
                "Local / Demo Health",
                "Prod API-Key Boundary",
                "Routing Lab",
                "Allocation / Evaluation Lab",
                "Scenario Replay Lab",
                "Remediation Report Lab",
                "Proxy Operator Lab",
                "Evidence / Offline Tooling Pointers"), folderNames);

        for (JsonNode request : requests(collection)) {
            String raw = request.at("/request/url/raw").asText();
            assertTrue(raw.startsWith("{{baseUrl}}/"), () -> raw + " should use {{baseUrl}}");
            assertTrue(ALLOWED_ENDPOINTS.contains(raw.substring("{{baseUrl}}".length())),
                    () -> raw + " should be an existing documented endpoint");
        }
    }

    @Test
    void protectedProdLikeRequestsUsePlaceholderApiKeyHeader() throws Exception {
        JsonNode collection = readJson(COLLECTION);
        JsonNode prodFolder = findFolder(collection, "Prod API-Key Boundary");
        assertNotNull(prodFolder, "prod API-key boundary folder should exist");

        for (JsonNode item : stream(prodFolder.path("item"))) {
            String name = item.path("name").asText();
            if (name.contains("With API Key")) {
                assertTrue(hasHeader(item, "X-API-Key", "{{apiKey}}"), name + " should use placeholder API key");
            }
        }

        for (JsonNode item : requests(collection)) {
            String method = item.at("/request/method").asText();
            String name = item.path("name").asText();
            if ("POST".equals(method) && !name.contains("Without API Key")) {
                assertTrue(hasHeader(item, "X-API-Key", "{{apiKey}}"),
                        name + " should carry placeholder X-API-Key for prod-like runs");
            }
        }

        JsonNode proxyStatus = findRequest(collection, "GET Proxy Status");
        assertNotNull(proxyStatus, "proxy status request should exist");
        assertTrue(hasHeader(proxyStatus, "X-API-Key", "{{apiKey}}"));
    }

    @Test
    void rawPostBodiesAreDeterministicJson() throws Exception {
        for (JsonNode item : requests(readJson(COLLECTION))) {
            JsonNode rawBody = item.at("/request/body/raw");
            if (!rawBody.isMissingNode()) {
                String body = rawBody.asText().replace("{{strategy}}", "WEIGHTED_LEAST_CONNECTIONS");
                OBJECT_MAPPER.readTree(body);
            }
        }
    }

    @Test
    void collectionAndEnvironmentAvoidUnsafeContent() throws Exception {
        String combined = read(COLLECTION) + "\n" + read(ENVIRONMENT);
        String normalized = combined.toLowerCase(Locale.ROOT);

        assertFalse(normalized.contains("localstorage"));
        assertFalse(normalized.contains("sessionstorage"));
        assertFalse(normalized.contains("authorization"));
        assertFalse(normalized.contains("bearer"));
        assertFalse(normalized.contains("_postman_exported_at"));
        assertFalse(normalized.contains("_postman_exported_using"));
        assertFalse(normalized.contains("date.now"));
        assertFalse(normalized.contains("math.random"));
        assertFalse(normalized.contains("cloudmanager"));
        assertFalse(normalized.contains("release-downloads"));
        assertFalse(normalized.contains("delete-branch"));
        assertFalse(normalized.contains("/rulesets"));

        for (Pattern pattern : SECRET_PATTERNS) {
            assertFalse(pattern.matcher(combined).find(), () -> "forbidden secret-like pattern: " + pattern);
        }

        Matcher matcher = URL_PATTERN.matcher(combined);
        while (matcher.find()) {
            String url = matcher.group();
            assertTrue(url.startsWith("http://localhost:")
                            || url.startsWith("http://127.0.0.1:")
                            || url.startsWith("https://schema.getpostman.com/"),
                    () -> "unexpected live URL: " + url);
        }
    }

    @Test
    void documentationMentionsImportSafetyAndPr98Gating() throws Exception {
        assertTrue(Files.exists(DOC), "Postman collection guide should exist");

        String doc = read(DOC);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("docs/postman/LoadBalancerPro Enterprise Lab.postman_collection.json"));
        assertTrue(doc.contains("docs/postman/LoadBalancerPro Local.postman_environment.json"));
        assertTrue(doc.contains("PR #98"));
        assertTrue(doc.contains("Swagger"));
        assertTrue(doc.contains("OpenAPI"));
        assertTrue(doc.contains("X-API-Key"));
        assertTrue(doc.contains("<API_KEY>"));
        assertTrue(normalized.contains("no real secrets"));
        assertTrue(normalized.contains("cloud state"));
        assertTrue(normalized.contains("oauth2"));
        assertTrue(normalized.contains("not production iam"));
    }

    private static JsonNode readJson(Path path) throws Exception {
        return OBJECT_MAPPER.readTree(read(path));
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static List<JsonNode> requests(JsonNode node) {
        List<JsonNode> requests = new ArrayList<>();
        collectRequests(node, requests);
        return requests;
    }

    private static void collectRequests(JsonNode node, List<JsonNode> requests) {
        if (node.has("request")) {
            requests.add(node);
        }
        for (JsonNode item : node.path("item")) {
            collectRequests(item, requests);
        }
    }

    private static JsonNode findFolder(JsonNode node, String name) {
        if (name.equals(node.path("name").asText()) && node.has("item")) {
            return node;
        }
        for (JsonNode item : node.path("item")) {
            JsonNode found = findFolder(item, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static JsonNode findRequest(JsonNode node, String name) {
        for (JsonNode item : requests(node)) {
            if (name.equals(item.path("name").asText())) {
                return item;
            }
        }
        return null;
    }

    private static boolean hasHeader(JsonNode item, String key, String value) {
        for (JsonNode header : item.at("/request/header")) {
            if (key.equals(header.path("key").asText()) && value.equals(header.path("value").asText())) {
                return true;
            }
        }
        return false;
    }

    private static List<JsonNode> stream(JsonNode array) {
        List<JsonNode> nodes = new ArrayList<>();
        array.forEach(nodes::add);
        return nodes;
    }
}
