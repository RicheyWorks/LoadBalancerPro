package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DecisionExplorerApiContractHardeningTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String VALID_REQUEST = """
            {
              "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
              "servers": [
                {
                  "serverId": "green",
                  "healthy": true,
                  "inFlightRequestCount": 5,
                  "configuredCapacity": 100.0,
                  "estimatedConcurrencyLimit": 100.0,
                  "averageLatencyMillis": 20.0,
                  "p95LatencyMillis": 40.0,
                  "p99LatencyMillis": 80.0,
                  "recentErrorRate": 0.01,
                  "queueDepth": 1
                },
                {
                  "serverId": "blue",
                  "healthy": true,
                  "inFlightRequestCount": 75,
                  "configuredCapacity": 100.0,
                  "estimatedConcurrencyLimit": 100.0,
                  "averageLatencyMillis": 35.0,
                  "p95LatencyMillis": 120.0,
                  "p99LatencyMillis": 220.0,
                  "recentErrorRate": 0.15,
                  "queueDepth": 10
                }
              ]
            }
            """;
    private static final Set<String> RETIRED_FIELDS = Set.of(
            "payloadObject",
            "decisionReadout",
            "selectedCandidate",
            "candidateSet",
            "candidateComparisons",
            "confidenceSummary",
            "routingDiagnostics",
            "routeTradeoffAnalysis",
            "shadowDecisionQualityEvaluation",
            "counterfactualAnalysis",
            "factorDrilldowns",
            "policyGateReadouts",
            "decisionDiffReadouts",
            "evidencePacketReadouts",
            "agentStructuredOutput",
            "warnings",
            "unknowns",
            "notProvenBoundaries");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void endpointReturnsOnlyTheCompactRoutingExplanationContract() throws Exception {
        JsonNode payload = endpointPayload();

        assertEquals(List.of(
                        "readOnly",
                        "simulationOnly",
                        "contractVersion",
                        "strategyId",
                        "status",
                        "selectedCandidateId",
                        "candidates",
                        "dominantFactors",
                        "decisionDelta",
                        "counterfactualWeightScenarios",
                        "boundaryNote"),
                iterable(payload.fieldNames()));
        assertTrue(payload.path("readOnly").asBoolean());
        assertTrue(payload.path("simulationOnly").asBoolean());
        assertEquals("v2", payload.path("contractVersion").asText());
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", payload.path("strategyId").asText());
        assertEquals("green", payload.path("selectedCandidateId").asText());
        assertEquals(2, payload.path("candidates").size());
        assertTrue(payload.path("candidates").get(0).path("factors").isArray());
        assertEquals("AVAILABLE", payload.at("/dominantFactors/status").asText());
        assertFalse(payload.at("/decisionDelta/status").asText().isBlank());
        assertTrue(payload.path("counterfactualWeightScenarios").isArray());
        assertFalse(payload.path("boundaryNote").asText().isBlank());
        for (String retired : RETIRED_FIELDS) {
            assertFalse(payload.has(retired), "retired derivational field should be absent: " + retired);
        }
    }

    @Test
    void endpointRetainsReadOnlyBoundariesWithoutReplayOrMutationClaims() throws Exception {
        String normalized = endpointPayload().toString().toLowerCase(java.util.Locale.ROOT);

        for (String expected : List.of(
                "read-only",
                "simulation-only",
                "does not execute replay",
                "mutate weights or traffic",
                "does not",
                "prove production behavior")) {
            assertTrue(normalized.contains(expected), "payload should preserve boundary: " + expected);
        }
        for (String forbidden : List.of(
                "production readiness is proven",
                "live-cloud validated",
                "real tenant validated",
                "traffic shifting enabled",
                "replay executed")) {
            assertFalse(normalized.contains(forbidden), "payload must not overclaim: " + forbidden);
        }
    }

    private JsonNode endpointPayload() throws Exception {
        String body = mockMvc.perform(post("/api/routing/decision-explorer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = OBJECT_MAPPER.readTree(body);
        assertTrue(root.isArray());
        assertEquals(1, root.size());
        return root.get(0);
    }

    private static List<String> iterable(java.util.Iterator<String> values) {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        values.forEachRemaining(result::add);
        return List.copyOf(result);
    }
}
