package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.core.CloudManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@SpringBootTest
@AutoConfigureMockMvc
class RoutingDecisionDemoTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path ROUTING_DEMO_PAGE = Path.of("src/main/resources/static/routing-demo.html");
    private static final Path COMPARE_FIXTURE =
            Path.of("src/test/resources/routing-demo/compare-strategies-sample.json");
    private static final Path LEAST_CONNECTIONS_FIXTURE =
            Path.of("src/test/resources/routing-demo/least-connections-sample.json");
    private static final List<Path> ROUTING_FIXTURES = List.of(
            COMPARE_FIXTURE,
            Path.of("src/test/resources/routing-demo/round-robin-sample.json"),
            Path.of("src/test/resources/routing-demo/weighted-sample.json"),
            LEAST_CONNECTIONS_FIXTURE,
            Path.of("src/test/resources/routing-demo/tail-latency-sample.json"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void routingDemoPageExistsAndIsServed() throws Exception {
        assertTrue(Files.exists(ROUTING_DEMO_PAGE), "routing browser demo page should be source-controlled");

        mockMvc.perform(get("/routing-demo.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Routing Decision Demo")))
                .andExpect(content().string(containsString("data-action=\"compare\"")))
                .andExpect(content().string(containsString("/api/routing/compare")));
    }

    @Test
    void routingDemoPageContainsExpectedEndpointsControlsAndStrategyText() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);

        assertTrue(page.contains("/api/health"));
        assertTrue(page.contains("/actuator/health/readiness"));
        assertTrue(page.contains("/api/routing/compare"));
        assertTrue(page.contains("Strategy comparison"));
        assertTrue(page.contains("Sample request editor/viewer"));
        assertTrue(page.contains("Results table/cards"));
        assertTrue(page.contains("Why this server?"));
        assertTrue(page.contains("Copy curl"));
        assertTrue(page.contains("Copy payload"));
        assertTrue(page.contains("Copy summary"));
        assertTrue(page.contains("Copy raw response"));
        assertTrue(page.contains("Reset demo"));
        assertTrue(page.contains("Load sample scenario"));
        assertTrue(page.contains("Compare strategies"));
        assertTrue(page.contains("TAIL_LATENCY_POWER_OF_TWO"));
        assertTrue(page.contains("WEIGHTED_LEAST_LOAD"));
        assertTrue(page.contains("WEIGHTED_LEAST_CONNECTIONS"));
        assertTrue(page.contains("WEIGHTED_ROUND_ROBIN"));
        assertTrue(page.contains("ROUND_ROBIN"));
        assertFalse(page.contains("RESPONSE_TIME"));
    }

    @Test
    void routingDemoPageContainsSafetyLimitationsAndPostmanParity() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("Postman parity: run the <strong>Routing Decision Demo</strong> folder"));
        assertTrue(normalized.contains("local/operator demo only"));
        assertTrue(normalized.contains("not certification"));
        assertTrue(normalized.contains("not benchmark proof"));
        assertTrue(normalized.contains("not legal compliance proof"));
        assertTrue(normalized.contains("not identity proof"));
        assertTrue(normalized.contains("no cloud mutation"));
        assertTrue(normalized.contains("no cloudmanager required for routing demo"));
        assertTrue(normalized.contains("no external services/dependencies"));
        assertTrue(normalized.contains("no external scripts/cdns"));
        assertTrue(normalized.contains("api server required for browser/postman demo"));
        assertTrue(normalized.contains("no runtime reports or demo transcripts are written"));
    }

    @Test
    void routingDemoPageHasNoExternalScriptsStorageSecretsOrMutableControls() throws Exception {
        String page = Files.readString(ROUTING_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertFalse(normalized.contains("<script src="));
        assertFalse(normalized.contains("<link rel=\"stylesheet\""));
        assertFalse(normalized.contains("<img"));
        assertFalse(normalized.contains("background-image"));
        assertFalse(normalized.contains("cdn."));
        assertFalse(normalized.contains("fonts.googleapis"));
        assertFalse(normalized.contains("fonts.gstatic"));
        assertFalse(normalized.contains("localstorage"));
        assertFalse(normalized.contains("sessionstorage"));
        assertFalse(normalized.contains("date.now"));
        assertFalse(normalized.contains("new date"));
        assertFalse(normalized.contains("math.random"));
        assertFalse(normalized.contains("randomuuid"));
        assertFalse(normalized.contains("crypto.randomuuid"));
        assertFalse(normalized.contains("x-api-key"));
        assertFalse(normalized.contains("bearer"));
        assertFalse(normalized.contains("authorization"));
        assertFalse(normalized.contains("type=\"password\""));
        assertFalse(normalized.contains("password"));
        assertFalse(normalized.contains("access key"));
        assertFalse(normalized.contains("secret key"));
        assertFalse(normalized.contains("data-action=\"admin"));
        assertFalse(normalized.contains("data-action=\"release"));
        assertFalse(normalized.contains("data-action=\"ruleset"));
        assertFalse(normalized.contains("data-action=\"cloud"));
        assertFalse(normalized.contains("/rulesets"));
        assertFalse(normalized.contains("/repos/"));
        assertFalse(normalized.contains("create release"));
        assertFalse(normalized.contains("create tag"));
        assertFalse(normalized.contains("delete-branch"));
        assertFalse(normalized.contains("new cloudmanager"));
        assertFalse(normalized.contains("construct cloudmanager"));
        assertFalse(normalized.contains("certified operator"));
        assertFalse(normalized.contains("production benchmark"));
        assertFalse(normalized.contains("legal training compliance"));
        assertFalse(normalized.contains("identity verified"));
        assertTrue(countOccurrences(normalized, "cloudmanager") <= 2,
                "CloudManager may appear only in safety limitation text");
    }

    @Test
    void routingDemoFixturesAreValidJsonAndContainSafeSyntheticInputs() throws Exception {
        for (Path fixture : ROUTING_FIXTURES) {
            JsonNode body = readJson(fixture);
            String normalized = Files.readString(fixture, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);

            assertTrue(body.path("strategies").isArray(), fixture + " should list strategies");
            assertTrue(body.path("servers").isArray(), fixture + " should list servers");
            assertTrue(body.path("servers").size() >= 1, fixture + " should include a sample server");
            assertFalse(normalized.contains("http://"));
            assertFalse(normalized.contains("https://"));
            assertFalse(normalized.contains("arn:"));
            assertFalse(normalized.contains("prod-"));
            assertFalse(normalized.contains("password"));
            assertFalse(normalized.contains("secret"));
        }

        JsonNode compare = readJson(COMPARE_FIXTURE);
        assertEquals(5, compare.path("strategies").size());
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", compare.at("/strategies/0").asText());
        assertEquals("ROUND_ROBIN", compare.at("/strategies/4").asText());
        assertEquals("edge-alpha", compare.at("/servers/0/serverId").asText());
        assertEquals("edge-drain", compare.at("/servers/2/serverId").asText());
        assertFalse(compare.at("/servers/2/healthy").asBoolean());
    }

    @Test
    void packagedLeastConnectionsFixtureProducesDeterministicRoutingResultWithoutCloudManager() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(post("/api/routing/compare")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Files.readString(LEAST_CONNECTIONS_FIXTURE, StandardCharsets.UTF_8)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestedStrategies[0]", is("WEIGHTED_LEAST_CONNECTIONS")))
                    .andExpect(jsonPath("$.candidateCount", is(2)))
                    .andExpect(jsonPath("$.results[0].strategyId", is("WEIGHTED_LEAST_CONNECTIONS")))
                    .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                    .andExpect(jsonPath("$.results[0].chosenServerId", is("edge-weighted")))
                    .andExpect(jsonPath("$.results[0].reason", containsString("weighted least-connections")))
                    .andExpect(jsonPath("$.results[0].scores.edge-standard", is(5.0)))
                    .andExpect(jsonPath("$.results[0].scores.edge-weighted", is(3.0)));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "routing demo comparison must not construct CloudManager");
        }
    }

    @Test
    void unsupportedStrategyAndMalformedRoutingRequestsReturnControlledErrors() throws Exception {
        mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategies": ["NOT_A_STRATEGY"],
                                  "servers": [
                                    {
                                      "serverId": "edge-alpha",
                                      "healthy": true,
                                      "inFlightRequestCount": 1,
                                      "averageLatencyMillis": 10.0,
                                      "p95LatencyMillis": 20.0,
                                      "p99LatencyMillis": 30.0,
                                      "recentErrorRate": 0.0
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("Unsupported routing strategy")))
                .andExpect(jsonPath("$.path", is("/api/routing/compare")));

        mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategies": ["WEIGHTED_LEAST_LOAD"],
                                  "servers": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.path", is("/api/routing/compare")));
    }

    @Test
    void routingDecisionDemoPostmanFolderIsValidAndReadOnly() throws Exception {
        JsonNode collection = readJson(Path.of("postman/LoadBalancerPro.postman_collection.json"));
        JsonNode folder = findFolder(collection, "Routing Decision Demo");
        assertNotNull(folder, "Postman collection should include a Routing Decision Demo folder");
        assertEquals(6, folder.path("item").size());

        List<String> expectedNames = List.of(
                "GET Routing Demo Health Check",
                "GET Routing Demo Readiness Check",
                "POST Compare All Supported Strategies",
                "POST Weighted Strategy Sample",
                "POST Least Connections Sample",
                "POST Tail Latency Sample");
        for (int i = 0; i < expectedNames.size(); i++) {
            assertEquals(expectedNames.get(i), folder.at("/item/" + i + "/name").asText());
        }

        assertEquals("{{baseUrl}}/api/routing/compare", folder.at("/item/2/request/url/raw").asText());
        assertTrue(folder.at("/item/2/request/body/raw").asText().contains("TAIL_LATENCY_POWER_OF_TWO"));
        assertTrue(folder.at("/item/2/request/body/raw").asText().contains("WEIGHTED_ROUND_ROBIN"));
        assertTrue(folder.at("/item/2/request/body/raw").asText().contains("ROUND_ROBIN"));

        String normalized = Files.readString(Path.of("postman/LoadBalancerPro.postman_collection.json"),
                StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertFalse(normalized.contains("x-api-key"));
        assertFalse(normalized.contains("bearer"));
        assertFalse(normalized.contains("authorization"));
        assertFalse(normalized.contains("/rulesets"));
        assertFalse(normalized.contains("create release"));
        assertFalse(normalized.contains("create tag"));
        assertFalse(normalized.contains("delete-branch"));
    }

    private static JsonNode readJson(Path path) throws Exception {
        return OBJECT_MAPPER.readTree(Files.readString(path, StandardCharsets.UTF_8));
    }

    private static JsonNode findFolder(JsonNode node, String folderName) {
        if (folderName.equals(node.path("name").asText()) && node.path("item").isArray()) {
            return node;
        }
        for (JsonNode item : node.path("item")) {
            JsonNode found = findFolder(item, folderName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
