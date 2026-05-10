package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.richmond423.loadbalancerpro.core.CloudManager;
import com.richmond423.loadbalancerpro.core.DomainMetrics;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiAbuseResistanceTest {
    private static final String VALID_SERVER = """
            {
              "id": "api-1",
              "cpuUsage": 10.0,
              "memoryUsage": 20.0,
              "diskUsage": 30.0,
              "capacity": 100.0,
              "weight": 1.0,
              "healthy": true
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUpMetrics() {
        registry = new SimpleMeterRegistry();
        Metrics.addRegistry(registry);
    }

    @AfterEach
    void tearDownMetrics() {
        Metrics.removeRegistry(registry);
        registry.close();
    }

    @Test
    void repeatedMalformedEvaluationRequestsRemainControlledAndCloudSafe() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(post("/api/allocate/evaluate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "requestedLoad": 10.0,
                                          "servers": [
                                        }
                                        """))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is(400)))
                        .andExpect(jsonPath("$.error", is("bad_request")))
                        .andExpect(jsonPath("$.message", is("Malformed JSON request body")))
                        .andExpect(jsonPath("$.path", is("/api/allocate/evaluate")))
                        .andExpect(jsonPath("$.trace").doesNotExist())
                        .andExpect(jsonPath("$.exception").doesNotExist());
            }

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Malformed evaluation requests must not construct CloudManager.");
        }

        assertEquals(3.0, registry.counter(
                DomainMetrics.ALLOCATION_VALIDATION_FAILURES,
                "path", "/api/allocate/evaluate",
                "reason", "bad_request").count());
        assertEquals(0.0, registry.counter(
                DomainMetrics.ALLOCATION_REQUESTS, "strategy", "CAPACITY_AWARE").count());
    }

    @Test
    void nonFiniteRequestedLoadIsRejectedBeforeAllocationOrCloudMutation() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(post("/api/allocate/capacity-aware")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "requestedLoad": 1e309,
                                      "servers": [%s]
                                    }
                                    """.formatted(VALID_SERVER)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("bad_request")))
                    .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")))
                    .andExpect(jsonPath("$.trace").doesNotExist())
                    .andExpect(jsonPath("$.exception").doesNotExist());

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Non-finite load must be rejected before allocation or cloud construction.");
        }

        assertEquals(1.0, registry.counter(
                DomainMetrics.ALLOCATION_VALIDATION_FAILURES,
                "path", "/api/allocate/capacity-aware",
                "reason", "bad_request").count());
        assertEquals(0.0, registry.counter(
                DomainMetrics.ALLOCATION_REQUESTS, "strategy", "CAPACITY_AWARE").count());
    }

    @Test
    void evaluationRejectsUnsafeLoadSheddingRangesWithoutEmittingAllocationMetrics() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(post("/api/allocate/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "requestedLoad": 10.0,
                                      "strategy": "CAPACITY_AWARE",
                                      "currentInFlightRequestCount": -1,
                                      "concurrencyLimit": 0,
                                      "queueDepth": -1,
                                      "observedP95LatencyMillis": -5.0,
                                      "observedErrorRate": 1.5,
                                      "servers": [%s]
                                    }
                                    """.formatted(VALID_SERVER)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("validation_failed")))
                    .andExpect(jsonPath("$.path", is("/api/allocate/evaluate")))
                    .andExpect(jsonPath("$.details").isArray())
                    .andExpect(jsonPath("$.trace").doesNotExist())
                    .andExpect(jsonPath("$.exception").doesNotExist());

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Invalid load-shedding metadata must not construct CloudManager.");
        }

        assertEquals(1.0, registry.counter(
                DomainMetrics.ALLOCATION_VALIDATION_FAILURES,
                "path", "/api/allocate/evaluate",
                "reason", "validation_failed").count());
        assertEquals(0.0, registry.counter(
                DomainMetrics.ALLOCATION_REQUESTS, "strategy", "CAPACITY_AWARE").count());
    }

    @Test
    void routingOversizedPayloadIsRejectedBeforeRoutingOrCloudMutation() throws Exception {
        String oversizedStrategy = "S".repeat(20_000);

        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(post("/api/routing/compare")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "strategies": ["%s"],
                                      "servers": [
                                        {
                                          "serverId": "green",
                                          "healthy": true,
                                          "inFlightRequestCount": 1,
                                          "averageLatencyMillis": 10.0,
                                          "p95LatencyMillis": 20.0,
                                          "p99LatencyMillis": 30.0,
                                          "recentErrorRate": 0.0
                                        }
                                      ]
                                    }
                                    """.formatted(oversizedStrategy)))
                    .andExpect(status().isPayloadTooLarge())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status", is(413)))
                    .andExpect(jsonPath("$.error", is("payload_too_large")))
                    .andExpect(jsonPath("$.message", containsString("Request body exceeds")))
                    .andExpect(jsonPath("$.path", is("/api/routing/compare")))
                    .andExpect(jsonPath("$.trace").doesNotExist())
                    .andExpect(jsonPath("$.exception").doesNotExist());

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Oversized routing payloads must not reach routing or cloud construction.");
        }
    }

    @Test
    void routingRejectsUnsafeMetricRangesWithSafeErrorShapeAndNoCloudMutation() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(post("/api/routing/compare")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
                                      "servers": [
                                        {
                                          "serverId": "green",
                                          "healthy": true,
                                          "inFlightRequestCount": -1,
                                          "averageLatencyMillis": 10.0,
                                          "p95LatencyMillis": 20.0,
                                          "p99LatencyMillis": 30.0,
                                          "recentErrorRate": 1.5
                                        }
                                      ]
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("bad_request")))
                    .andExpect(jsonPath("$.message", containsString("inFlightRequestCount")))
                    .andExpect(jsonPath("$.path", is("/api/routing/compare")))
                    .andExpect(jsonPath("$.trace").doesNotExist())
                    .andExpect(jsonPath("$.exception").doesNotExist());

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Invalid routing metrics must not construct CloudManager.");
        }
    }
}
