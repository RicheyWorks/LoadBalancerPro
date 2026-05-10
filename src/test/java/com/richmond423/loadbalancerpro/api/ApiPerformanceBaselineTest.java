package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiPerformanceBaselineTest {
    private static final Duration CORE_ROUTING_BUDGET = Duration.ofSeconds(4);
    private static final Duration API_SMOKE_BUDGET = Duration.ofSeconds(15);
    private static final int CORE_ROUTING_ITERATIONS = 500;
    private static final int API_ITERATIONS = 12;

    private static final String OVERLOADED_ALLOCATION_REQUEST = """
            {
              "requestedLoad": 150.0,
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
                }
              ]
            }
            """;

    private static final String ROUTING_REQUEST = """
            {
              "strategies": ["TAIL_LATENCY_POWER_OF_TWO", "WEIGHTED_LEAST_LOAD"],
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

    private static final String ALL_UNHEALTHY_ROUTING_REQUEST = """
            {
              "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
              "servers": [
                {
                  "serverId": "red",
                  "healthy": false,
                  "inFlightRequestCount": 3,
                  "averageLatencyMillis": 10.0,
                  "p95LatencyMillis": 20.0,
                  "p99LatencyMillis": 30.0,
                  "recentErrorRate": 0.0
                }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void coreRoutingComparisonStaysInsideCiSmokeBudget() {
        RoutingComparisonService service = new RoutingComparisonService();
        RoutingComparisonRequest request = coreRoutingRequest();

        assertTimeout(CORE_ROUTING_BUDGET, () -> {
            for (int i = 0; i < CORE_ROUTING_ITERATIONS; i++) {
                RoutingComparisonResponse response = service.compare(request);
                assertEquals(3, response.requestedStrategies().size());
                assertEquals(3, response.candidateCount());
                assertEquals(3, response.results().size());
                assertTrue(response.results().stream().allMatch(result -> "SUCCESS".equals(result.status())));
                assertFalse(response.results().get(0).candidateServersConsidered().isEmpty());
            }
        });
    }

    @Test
    void allocationApiOverloadPathStaysInsideCiSmokeBudget() {
        assertTimeout(API_SMOKE_BUDGET, () -> {
            for (int i = 0; i < API_ITERATIONS; i++) {
                mockMvc.perform(post("/api/allocate/capacity-aware")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(OVERLOADED_ALLOCATION_REQUEST))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.allocations.primary", closeTo(70.0, 0.01)))
                        .andExpect(jsonPath("$.allocations.fallback", closeTo(30.0, 0.01)))
                        .andExpect(jsonPath("$.allocations.failed").doesNotExist())
                        .andExpect(jsonPath("$.unallocatedLoad", closeTo(50.0, 0.01)))
                        .andExpect(jsonPath("$.recommendedAdditionalServers", is(1)))
                        .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)));
            }
        });
    }

    @Test
    void readOnlyEvaluationApiStaysInsideCiSmokeBudget() {
        assertTimeout(API_SMOKE_BUDGET, () -> {
            for (int i = 0; i < API_ITERATIONS; i++) {
                mockMvc.perform(post("/api/allocate/evaluate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(EVALUATION_REQUEST))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.acceptedLoad", closeTo(100.0, 0.01)))
                        .andExpect(jsonPath("$.rejectedLoad", closeTo(50.0, 0.01)))
                        .andExpect(jsonPath("$.unallocatedLoad", closeTo(50.0, 0.01)))
                        .andExpect(jsonPath("$.recommendedAdditionalServers", is(1)))
                        .andExpect(jsonPath("$.loadShedding.action", is("SHED")))
                        .andExpect(jsonPath("$.metricsPreview.emitted", is(false)))
                        .andExpect(jsonPath("$.readOnly", is(true)));
            }
        });
    }

    @Test
    void routingApiHealthyAndDegradedPathsStayInsideCiSmokeBudget() {
        assertTimeout(API_SMOKE_BUDGET, () -> {
            for (int i = 0; i < API_ITERATIONS; i++) {
                mockMvc.perform(post("/api/routing/compare")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ROUTING_REQUEST))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.candidateCount", is(2)))
                        .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                        .andExpect(jsonPath("$.results[0].chosenServerId", is("green")));

                mockMvc.perform(post("/api/routing/compare")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ALL_UNHEALTHY_ROUTING_REQUEST))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.candidateCount", is(1)))
                        .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                        .andExpect(jsonPath("$.results[0].chosenServerId", nullValue()))
                        .andExpect(jsonPath("$.results[0].reason", containsString("No healthy eligible servers")));
            }
        });
    }

    private static RoutingComparisonRequest coreRoutingRequest() {
        return new RoutingComparisonRequest(
                List.of("WEIGHTED_LEAST_LOAD", "WEIGHTED_LEAST_CONNECTIONS", "ROUND_ROBIN"),
                List.of(
                        server("green", 5, 1.0, 20.0, 40.0, 80.0, 0.01),
                        server("blue", 75, 1.0, 35.0, 120.0, 220.0, 0.15),
                        server("gold", 20, 4.0, 25.0, 50.0, 90.0, 0.02)));
    }

    private static RoutingServerStateInput server(String id,
                                                  int inFlight,
                                                  double weight,
                                                  double averageLatency,
                                                  double p95Latency,
                                                  double p99Latency,
                                                  double errorRate) {
        return new RoutingServerStateInput(
                id,
                true,
                inFlight,
                100.0,
                100.0,
                weight,
                averageLatency,
                p95Latency,
                p99Latency,
                errorRate,
                1,
                null);
    }
}
