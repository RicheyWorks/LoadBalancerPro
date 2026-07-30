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
import java.util.List;
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
    private static final Path COMPARE_FIXTURE =
            Path.of("src/test/resources/routing-demo/compare-strategies-sample.json");
    private static final List<String> DELETED_RESULT_FIELDS = List.of(
            "decisionReplayEvidenceBoundarySummary",
            "decisionReplayEvidenceFieldInventory",
            "decisionReplayEvidenceNullSafetySummary",
            "decisionReplayEvidenceStatusRollup",
            "decisionReplayEvidenceLaneNavigationSummary",
            "decisionReplayEvidenceLaneDependencyMap",
            "decisionReplayEvidenceLaneReferenceIndex",
            "decisionReplayEvidenceLaneDependencySummary",
            "decisionReplayEvidenceLaneConsistencySummary",
            "decisionReplayEvidenceReviewerSnapshot",
            "decisionReplayEvidenceReviewerGuidance",
            "decisionReplayEvidenceReviewerHandoffSummary",
            "decisionReplayEvidenceReviewerClosureSummary");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void compareResponseKeepsDecisionEvidenceAndDeletesMetadataAboutMetadata() throws Exception {
        JsonNode response = postCompare(Files.readString(COMPARE_FIXTURE, StandardCharsets.UTF_8));
        JsonNode result = response.at("/results/0");

        assertEquals("TAIL_LATENCY_POWER_OF_TWO", response.at("/requestedStrategies/0").asText());
        assertEquals(3, response.path("candidateCount").asInt());
        assertFalse(response.has("decisionReplayEvidenceReviewerClosureRollup"));
        assertFalse(response.has("decisionReplayEvidenceReviewerClosureChecklist"));
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", result.path("strategyId").asText());
        assertEquals("SUCCESS", result.path("status").asText());
        assertEquals("edge-alpha", result.path("chosenServerId").asText());
        assertTrue(result.path("scores").has("edge-alpha"));
        DELETED_RESULT_FIELDS.forEach(field -> assertFalse(result.has(field), field));

        JsonNode vector = result.path("decisionVector");
        assertTrue(vector.path("readOnly").asBoolean());
        assertEquals("/api/routing/compare", vector.path("localLabResponsePath").asText());
        assertEquals("edge-alpha", vector.path("selectedBackend").asText());
        assertEquals(3, vector.path("candidateCount").asInt());
        assertEquals("edge-alpha", vector.at("/selectedCandidateVector/candidateId").asText());
        assertTrue(vector.at("/selectedCandidateVector/factorContributions").isArray());

        assertEquals("AVAILABLE", result.at("/dominantFactorAnalysis/status").asText());
        assertEquals("PARTIAL", result.at("/decisionDeltaAnalysis/status").asText());
        assertEquals("PARTIAL", result.at("/decisionReplaySnapshot/status").asText());
        assertEquals("PARTIAL", result.at("/decisionReplayReconstructionTrace/status").asText());
        assertEquals("PARTIAL", result.at("/decisionReplayCapsule/status").asText());
        assertEquals("PARTIAL", result.at("/decisionReplayReadinessChecklist/status").asText());
        assertEquals("PARTIAL", result.at("/decisionReplayEvidenceSourceMap/status").asText());
    }

    @Test
    void exposedDecisionVectorCarriesVisibleSignalsAndNotProvenBoundaries() throws Exception {
        JsonNode vector = postCompare(Files.readString(COMPARE_FIXTURE, StandardCharsets.UTF_8))
                .at("/results/0/decisionVector");
        String vectorText = vector.toString();
        String normalized = vectorText.toLowerCase(Locale.ROOT);

        assertTrue(vectorText.contains("healthState=true"));
        assertTrue(vectorText.contains("p95LatencyMillis=40.000000"));
        assertTrue(vectorText.contains("hidden routing internals not exposed"));
        assertTrue(vectorText.contains("exact production scoring not exposed"));
        assertTrue(vectorText.contains("production telemetry not exposed"));
        assertTrue(vectorText.contains("No production certification"));
        assertTrue(vectorText.contains("live-cloud proof"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("replay execution is implemented"));
        assertFalse(normalized.contains("what-if execution is implemented"));
    }

    @Test
    void allUnhealthyResultKeepsFailureShapeWithoutInventingDecisionVector() throws Exception {
        JsonNode response = postCompare("""
                {
                  "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
                  "servers": [{
                    "serverId": "edge-drain",
                    "healthy": false,
                    "inFlightRequestCount": 1,
                    "averageLatencyMillis": 10.0,
                    "p95LatencyMillis": 20.0,
                    "p99LatencyMillis": 30.0,
                    "recentErrorRate": 0.0
                  }]
                }
                """);
        JsonNode result = response.at("/results/0");

        assertEquals("SUCCESS", result.path("status").asText());
        assertTrue(result.path("chosenServerId").isNull());
        assertTrue(result.path("candidateServersConsidered").isEmpty());
        assertTrue(result.path("scores").isEmpty());
        assertTrue(result.path("decisionVector").isNull());
        assertEquals("UNKNOWN", result.at("/dominantFactorAnalysis/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionDeltaAnalysis/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplaySnapshot/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayReconstructionTrace/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayCapsule/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayReadinessChecklist/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceSourceMap/status").asText());
        assertTrue(result.path("reason").asText().contains("No healthy eligible servers"));
        assertFalse(response.has("decisionReplayEvidenceReviewerClosureRollup"));
        assertFalse(response.has("decisionReplayEvidenceReviewerClosureChecklist"));
        DELETED_RESULT_FIELDS.forEach(field -> assertFalse(result.has(field), field));
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
