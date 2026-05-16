package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RoutingDecisionVectorReadOnlyExposureTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path COMPARE_FIXTURE = Path.of("src/test/resources/routing-demo/compare-strategies-sample.json");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void compareResponseExposesDecisionVectorAdditivelyWithoutBreakingExistingFields() throws Exception {
        JsonNode response = postCompare(Files.readString(COMPARE_FIXTURE, StandardCharsets.UTF_8));

        assertEquals("TAIL_LATENCY_POWER_OF_TWO", response.at("/requestedStrategies/0").asText());
        assertEquals(3, response.path("candidateCount").asInt());
        JsonNode result = response.at("/results/0");
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", result.path("strategyId").asText());
        assertEquals("SUCCESS", result.path("status").asText());
        assertEquals("edge-alpha", result.path("chosenServerId").asText());
        assertFalse(result.path("reason").asText().isBlank());
        assertEquals("edge-alpha", result.at("/candidateServersConsidered/0").asText());
        assertTrue(result.path("scores").has("edge-alpha"));

        JsonNode vector = result.path("decisionVector");
        assertTrue(vector.path("readOnly").asBoolean());
        assertEquals("/api/routing/compare", vector.path("localLabResponsePath").asText());
        assertEquals("not exposed by this read-only local lab response",
                vector.path("decisionIdOrLabRunId").asText());
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", vector.path("selectedStrategy").asText());
        assertEquals("edge-alpha", vector.path("selectedBackend").asText());
        assertEquals(3, vector.path("candidateCount").asInt());
        assertEquals(3, vector.path("candidateSummaries").size());
        assertEquals("edge-alpha", vector.at("/selectedCandidateVector/candidateId").asText());
        assertTrue(vector.at("/selectedCandidateVector/selected").asBoolean());
        assertEquals(2, vector.path("nonSelectedCandidateVectors").size());
    }

    @Test
    void exposedDecisionVectorCarriesCandidateContributionsKnownUnknownSignalsAndBoundaries() throws Exception {
        JsonNode vector = postCompare(Files.readString(COMPARE_FIXTURE, StandardCharsets.UTF_8))
                .at("/results/0/decisionVector");

        String vectorText = vector.toString();
        String normalized = vectorText.toLowerCase(Locale.ROOT);

        assertTrue(vectorText.contains("healthState=true"));
        assertTrue(vectorText.contains("p95LatencyMillis=40.000000"));
        assertTrue(vectorText.contains("hidden routing internals not exposed"));
        assertTrue(vectorText.contains("exact production scoring not exposed"));
        assertTrue(vectorText.contains("production telemetry not exposed"));
        assertTrue(vectorText.contains("p95LatencyMillis"));
        assertTrue(vectorText.contains("EXACT_FROM_CALCULATOR"));
        assertTrue(vectorText.contains("hiddenRoutingInternals"));
        assertTrue(vectorText.contains("NOT_EXPOSED"));
        assertTrue(vector.path("factorContributionAvailability").asText()
                .contains("exposed for current ServerScoreCalculator components"));
        assertTrue(vector.path("replayReadiness").asText().contains("future/not implemented"));
        assertTrue(vector.path("whatIfReadiness").asText().contains("future/not implemented"));
        assertTrue(vector.path("structuredDecisionLoggingReadiness").asText().contains("future/not implemented"));
        assertTrue(normalized.contains("hidden scoring is not inferred"));
        assertTrue(normalized.contains("exact production scoring is not claimed"));
        assertTrue(normalized.contains("no production certification"));
        assertTrue(normalized.contains("production telemetry proof"));
        assertFalse(normalized.contains("hidden scoring is available"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("replay execution is implemented"));
        assertFalse(normalized.contains("what-if execution is implemented"));
    }

    @Test
    void allUnhealthyResultKeepsExistingFailureShapeAndNoInventedDecisionVector() throws Exception {
        JsonNode response = postCompare("""
                {
                  "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
                  "servers": [
                    {
                      "serverId": "edge-drain",
                      "healthy": false,
                      "inFlightRequestCount": 1,
                      "averageLatencyMillis": 10.0,
                      "p95LatencyMillis": 20.0,
                      "p99LatencyMillis": 30.0,
                      "recentErrorRate": 0.0
                    }
                  ]
                }
                """);

        JsonNode result = response.at("/results/0");
        assertEquals("SUCCESS", result.path("status").asText());
        assertTrue(result.path("chosenServerId").isNull());
        assertTrue(result.path("candidateServersConsidered").isEmpty());
        assertTrue(result.path("scores").isEmpty());
        assertTrue(result.path("decisionVector").isNull());
        assertTrue(result.path("reason").asText().contains("No healthy eligible servers"));
    }

    private JsonNode postCompare(String body) throws Exception {
        String responseBody = mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return OBJECT_MAPPER.readTree(responseBody);
    }
}
