package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class ApiContractTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String CAPACITY_AWARE_REQUEST = """
            {
              "requestedLoad": 75.0,
              "servers": [
                {
                  "id": "api-1",
                  "cpuUsage": 90.0,
                  "memoryUsage": 90.0,
                  "diskUsage": 90.0,
                  "capacity": 100.0,
                  "weight": 1.0,
                  "healthy": true
                },
                {
                  "id": "worker-1",
                  "cpuUsage": 80.0,
                  "memoryUsage": 80.0,
                  "diskUsage": 80.0,
                  "capacity": 100.0,
                  "weight": 1.0,
                  "healthy": true
                }
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
                {
                  "id": "primary",
                  "cpuUsage": 30.0,
                  "memoryUsage": 30.0,
                  "diskUsage": 30.0,
                  "capacity": 100.0,
                  "weight": 1.0,
                  "healthy": true
                },
                {
                  "id": "fallback",
                  "cpuUsage": 70.0,
                  "memoryUsage": 70.0,
                  "diskUsage": 70.0,
                  "capacity": 100.0,
                  "weight": 1.0,
                  "healthy": true
                },
                {
                  "id": "failed",
                  "cpuUsage": 0.0,
                  "memoryUsage": 0.0,
                  "diskUsage": 0.0,
                  "capacity": 500.0,
                  "weight": 10.0,
                  "healthy": false
                }
              ]
            }
            """;

    private static final String ROUTING_REQUEST = """
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

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocumentExposesCoreApiPathsAndSchemas() throws Exception {
        JsonNode docs = openApiDocs();

        assertPathRequestSchemaAndOkResponse(docs, "/api/allocate/capacity-aware", "AllocationRequest");
        assertPathRequestSchemaAndOkResponse(docs, "/api/allocate/predictive", "AllocationRequest");
        assertPathRequestSchemaAndOkResponse(docs, "/api/allocate/evaluate", "AllocationEvaluationRequest");
        assertPathRequestSchemaAndOkResponse(docs, "/api/routing/compare", "RoutingComparisonRequest");

        assertSchemaProperties(docs, "AllocationRequest", "requestedLoad", "servers");
        assertSchemaProperties(docs, "AllocationResponse", "allocations", "unallocatedLoad",
                "recommendedAdditionalServers", "scalingSimulation");
        assertSchemaProperties(docs, "AllocationEvaluationRequest", "requestedLoad", "servers",
                "strategy", "priority", "currentInFlightRequestCount", "concurrencyLimit",
                "queueDepth", "observedP95LatencyMillis", "observedErrorRate");
        assertSchemaProperties(docs, "AllocationEvaluationResponse", "strategy", "allocations",
                "acceptedLoad", "rejectedLoad", "unallocatedLoad", "recommendedAdditionalServers",
                "scalingSimulation", "loadShedding", "metricsPreview", "readOnly", "decisionReason");
        assertSchemaProperties(docs, "LoadSheddingEvaluation", "priority", "action", "reason",
                "targetId", "currentInFlightRequestCount", "concurrencyLimit", "queueDepth",
                "utilization", "observedP95LatencyMillis", "observedErrorRate");
        assertSchemaProperties(docs, "AllocationEvaluationMetricsPreview", "strategy",
                "evaluatedHealthyServerCount", "acceptedLoad", "rejectedLoad", "unallocatedLoad",
                "recommendedAdditionalServers", "metricNames", "emitted");
        assertSchemaProperties(docs, "RoutingComparisonRequest", "strategies", "servers");
        assertSchemaProperties(docs, "RoutingComparisonResponse", "requestedStrategies", "candidateCount",
                "timestamp", "results");
        assertSchemaProperties(docs, "RoutingComparisonResultResponse", "strategyId", "status",
                "chosenServerId", "reason", "candidateServersConsidered", "scores");
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
                .andExpect(jsonPath("$.scalingSimulation").isMap())
                .andExpect(jsonPath("$.scalingSimulation.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.scalingSimulation.reason", containsString("simulated scale-up")))
                .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void evaluationResponseShapeIsStableAndReadOnly() throws Exception {
        String first = mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVALUATION_REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.strategy", is("CAPACITY_AWARE")))
                .andExpect(jsonPath("$.allocations").isMap())
                .andExpect(jsonPath("$.allocations.primary", closeTo(70.0, 0.01)))
                .andExpect(jsonPath("$.allocations.fallback", closeTo(30.0, 0.01)))
                .andExpect(jsonPath("$.allocations.failed").doesNotExist())
                .andExpect(jsonPath("$.acceptedLoad", closeTo(100.0, 0.01)))
                .andExpect(jsonPath("$.rejectedLoad", closeTo(50.0, 0.01)))
                .andExpect(jsonPath("$.unallocatedLoad", closeTo(50.0, 0.01)))
                .andExpect(jsonPath("$.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)))
                .andExpect(jsonPath("$.loadShedding.priority", is("BACKGROUND")))
                .andExpect(jsonPath("$.loadShedding.action", is("SHED")))
                .andExpect(jsonPath("$.loadShedding.reason", containsString("overload pressure")))
                .andExpect(jsonPath("$.loadShedding.utilization", closeTo(0.95, 0.01)))
                .andExpect(jsonPath("$.metricsPreview.emitted", is(false)))
                .andExpect(jsonPath("$.metricsPreview.metricNames").isArray())
                .andExpect(jsonPath("$.readOnly", is(true)))
                .andExpect(jsonPath("$.decisionReason", containsString("Read-only evaluation")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String second = mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVALUATION_REQUEST))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(first, second, "evaluation responses must stay deterministic for generated clients");
    }

    @Test
    void routingComparisonResponseShapeIsStable() throws Exception {
        mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROUTING_REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requestedStrategies[0]", is("TAIL_LATENCY_POWER_OF_TWO")))
                .andExpect(jsonPath("$.candidateCount", is(2)))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].strategyId", is("TAIL_LATENCY_POWER_OF_TWO")))
                .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$.results[0].chosenServerId", is("green")))
                .andExpect(jsonPath("$.results[0].reason", containsString("Chose green")))
                .andExpect(jsonPath("$.results[0].candidateServersConsidered[0]", is("green")))
                .andExpect(jsonPath("$.results[0].candidateServersConsidered[1]", is("blue")))
                .andExpect(jsonPath("$.results[0].scores.green").isNumber())
                .andExpect(jsonPath("$.results[0].scores.blue").isNumber())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void routingAllUnhealthyResponseShapeIsStable() throws Exception {
        mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
                                  "servers": [
                                    {
                                      "serverId": "green",
                                      "healthy": false,
                                      "inFlightRequestCount": 1,
                                      "averageLatencyMillis": 10.0,
                                      "p95LatencyMillis": 20.0,
                                      "p99LatencyMillis": 30.0,
                                      "recentErrorRate": 0.0
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount", is(1)))
                .andExpect(jsonPath("$.results[0].strategyId", is("TAIL_LATENCY_POWER_OF_TWO")))
                .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$.results[0].chosenServerId", nullValue()))
                .andExpect(jsonPath("$.results[0].candidateServersConsidered").isEmpty())
                .andExpect(jsonPath("$.results[0].scores").isEmpty())
                .andExpect(jsonPath("$.results[0].reason", containsString("No healthy eligible servers")));
    }

    @Test
    void invalidRequestErrorShapeIsStableAcrossApiContracts() throws Exception {
        mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 10.0,
                                  "strategy": "CAPACITY_AWARE",
                                  "priority": "gold",
                                  "servers": [
                                    {
                                      "id": "api-1",
                                      "cpuUsage": 10.0,
                                      "memoryUsage": 20.0,
                                      "diskUsage": 20.0,
                                      "capacity": 100.0,
                                      "weight": 1.0,
                                      "healthy": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("priority must be one of")))
                .andExpect(jsonPath("$.path", is("/api/allocate/evaluate")))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    private JsonNode openApiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return OBJECT_MAPPER.readTree(body);
    }

    private static void assertPathRequestSchemaAndOkResponse(JsonNode docs, String path, String requestSchema) {
        String encodedPath = path.replace("/", "~1");
        JsonNode operation = required(docs, "/paths/" + encodedPath + "/post");
        JsonNode requestContent = required(operation, "/requestBody/content/application~1json/schema");
        assertRef(requestContent, "#/components/schemas/" + requestSchema);

        required(operation, "/responses/200");
    }

    private static void assertSchemaProperties(JsonNode docs, String schemaName, String... properties) {
        JsonNode schemaProperties = required(docs, "/components/schemas/" + schemaName + "/properties");
        for (String property : properties) {
            assertFalse(schemaProperties.path(property).isMissingNode(),
                    () -> schemaName + " should expose property " + property);
        }
    }

    private static JsonNode required(JsonNode node, String pointer) {
        JsonNode value = node.at(pointer);
        assertFalse(value.isMissingNode(), () -> "Expected OpenAPI node at " + pointer);
        return value;
    }

    private static void assertRef(JsonNode schema, String expectedRef) {
        assertEquals(expectedRef, required(schema, "/$ref").asText());
    }
}
