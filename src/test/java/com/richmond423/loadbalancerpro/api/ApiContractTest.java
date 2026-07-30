package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiContractTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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
            "decisionReplayEvidenceReviewerClosureSummary",
            "decisionReplaySnapshot",
            "decisionReplayReconstructionTrace",
            "decisionReplayCapsule",
            "decisionReplayReadinessChecklist",
            "decisionReplayEvidenceSourceMap");
    private static final String CAPACITY_AWARE_REQUEST = """
            {
              "requestedLoad": 75.0,
              "servers": [
                {"id":"api-1","cpuUsage":90.0,"memoryUsage":90.0,"diskUsage":90.0,
                 "capacity":100.0,"weight":1.0,"healthy":true},
                {"id":"worker-1","cpuUsage":80.0,"memoryUsage":80.0,"diskUsage":80.0,
                 "capacity":100.0,"weight":1.0,"healthy":true}
              ]
            }
            """;
    private static final String EVALUATION_REQUEST = """
            {
              "requestedLoad": 150.0,
              "strategy": "CAPACITY_AWARE",
              "priority": "BACKGROUND",
              "currentInFlightRequestCount": 95,
              "concurrencyLimit": 100,
              "queueDepth": 25,
              "observedP95LatencyMillis": 300.0,
              "observedErrorRate": 0.20,
              "servers": [
                {"id":"primary","cpuUsage":30.0,"memoryUsage":30.0,"diskUsage":30.0,
                 "capacity":100.0,"weight":1.0,"healthy":true},
                {"id":"fallback","cpuUsage":70.0,"memoryUsage":70.0,"diskUsage":70.0,
                 "capacity":100.0,"weight":1.0,"healthy":true},
                {"id":"failed","cpuUsage":0.0,"memoryUsage":0.0,"diskUsage":0.0,
                 "capacity":500.0,"weight":10.0,"healthy":false}
              ]
            }
            """;
    private static final String ROUTING_REQUEST = """
            {
              "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
              "servers": [
                {"serverId":"green","healthy":true,"inFlightRequestCount":5,
                 "configuredCapacity":100.0,"estimatedConcurrencyLimit":100.0,
                 "averageLatencyMillis":20.0,"p95LatencyMillis":40.0,
                 "p99LatencyMillis":80.0,"recentErrorRate":0.01,"queueDepth":1},
                {"serverId":"blue","healthy":true,"inFlightRequestCount":75,
                 "configuredCapacity":100.0,"estimatedConcurrencyLimit":100.0,
                 "averageLatencyMillis":35.0,"p95LatencyMillis":120.0,
                 "p99LatencyMillis":220.0,"recentErrorRate":0.15,"queueDepth":10}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocumentExposesCoreApiPathsAndCompactRoutingSchemas() throws Exception {
        JsonNode docs = openApiDocs();
        for (String path : List.of(
                "/api/allocate/capacity-aware",
                "/api/allocate/predictive",
                "/api/allocate/evaluate",
                "/api/routing/compare",
                "/api/scenarios/replay",
                "/api/remediation/report")) {
            assertTrue(docs.at("/paths/" + path.replace("/", "~1") + "/post").isObject(), path);
        }
        JsonNode response =
                docs.at("/components/schemas/RoutingComparisonResponse/properties");
        JsonNode result =
                docs.at("/components/schemas/RoutingComparisonResultResponse/properties");
        assertEquals(4, response.size());
        assertTrue(response.has("results"));
        assertFalse(response.has("decisionReplayEvidenceReviewerClosureRollup"));
        assertFalse(response.has("decisionReplayEvidenceReviewerClosureChecklist"));
        assertEquals(10, result.size());
        assertTrue(result.has("decisionFingerprint"));
        assertTrue(result.has("decisionVector"));
        assertTrue(result.has("dominantFactorAnalysis"));
        assertTrue(result.has("decisionDeltaAnalysis"));
        DELETED_RESULT_FIELDS.forEach(field -> assertFalse(result.has(field), field));
    }

    @Test
    void capacityAwareAllocationResponseShapeIsStable() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CAPACITY_AWARE_REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.allocations").isMap())
                .andExpect(jsonPath("$.allocations['api-1']", closeTo(10.0, 0.01)))
                .andExpect(jsonPath("$.allocations['worker-1']", closeTo(20.0, 0.01)))
                .andExpect(jsonPath("$.unallocatedLoad", closeTo(45.0, 0.01)))
                .andExpect(jsonPath("$.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void evaluationResponseShapeIsStableReadOnlyAndDeterministic() throws Exception {
        String first = mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVALUATION_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy", is("CAPACITY_AWARE")))
                .andExpect(jsonPath("$.allocations.primary", closeTo(70.0, 0.01)))
                .andExpect(jsonPath("$.allocations.fallback", closeTo(30.0, 0.01)))
                .andExpect(jsonPath("$.acceptedLoad", closeTo(100.0, 0.01)))
                .andExpect(jsonPath("$.rejectedLoad", closeTo(50.0, 0.01)))
                .andExpect(jsonPath("$.readOnly", is(true)))
                .andExpect(jsonPath("$.remediationPlan.advisoryOnly", is(true)))
                .andExpect(jsonPath("$.remediationPlan.cloudMutation", is(false)))
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVALUATION_REQUEST))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(first, second);
    }

    @Test
    void routingComparisonResponseShapeIsStableAndMetadataIsAbsent() throws Exception {
        String body = mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROUTING_REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requestedStrategies[0]", is("TAIL_LATENCY_POWER_OF_TWO")))
                .andExpect(jsonPath("$.candidateCount", is(2)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup").doesNotExist())
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist").doesNotExist())
                .andExpect(jsonPath("$.results[0].strategyId", is("TAIL_LATENCY_POWER_OF_TWO")))
                .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$.results[0].chosenServerId", is("green")))
                .andExpect(jsonPath("$.results[0].decisionFingerprint",
                        org.hamcrest.Matchers.matchesPattern("sha256:v1:[0-9a-f]{64}")))
                .andExpect(jsonPath("$.results[0].decisionVector.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].dominantFactorAnalysis.status", is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionDeltaAnalysis.status").exists())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        JsonNode result = OBJECT_MAPPER.readTree(body).at("/results/0");
        DELETED_RESULT_FIELDS.forEach(field -> assertFalse(result.has(field), field));
    }

    @Test
    void routingAllUnhealthyResponseShapeIsStable() throws Exception {
        String body = mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategies":["TAIL_LATENCY_POWER_OF_TWO"],
                                  "servers":[{
                                    "serverId":"green","healthy":false,"inFlightRequestCount":1,
                                    "averageLatencyMillis":10.0,"p95LatencyMillis":20.0,
                                    "p99LatencyMillis":30.0,"recentErrorRate":0.0
                                  }]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount", is(1)))
                .andExpect(jsonPath("$.results[0].chosenServerId", nullValue()))
                .andExpect(jsonPath("$.results[0].candidateServersConsidered").isEmpty())
                .andExpect(jsonPath("$.results[0].scores").isEmpty())
                .andExpect(jsonPath("$.results[0].decisionFingerprint",
                        org.hamcrest.Matchers.matchesPattern("sha256:v1:[0-9a-f]{64}")))
                .andExpect(jsonPath("$.results[0].decisionVector", nullValue()))
                .andExpect(jsonPath("$.results[0].dominantFactorAnalysis.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionDeltaAnalysis.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].reason", containsString("No healthy eligible servers")))
                .andReturn().getResponse().getContentAsString();
        JsonNode response = OBJECT_MAPPER.readTree(body);
        assertFalse(response.has("decisionReplayEvidenceReviewerClosureRollup"));
        assertFalse(response.has("decisionReplayEvidenceReviewerClosureChecklist"));
        JsonNode result = response.at("/results/0");
        DELETED_RESULT_FIELDS.forEach(field -> assertFalse(result.has(field), field));
    }

    @Test
    void invalidRequestErrorShapeIsStableAcrossApiContracts() throws Exception {
        mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad":10.0,
                                  "strategy":"CAPACITY_AWARE",
                                  "priority":"gold",
                                  "servers":[{
                                    "id":"api-1","cpuUsage":10.0,"memoryUsage":20.0,
                                    "diskUsage":20.0,"capacity":100.0,"weight":1.0,"healthy":true
                                  }]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("priority must be one of")))
                .andExpect(jsonPath("$.path", is("/api/allocate/evaluate")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    private JsonNode openApiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return OBJECT_MAPPER.readTree(body);
    }
}
