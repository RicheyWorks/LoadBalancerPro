package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class AllocatorControllerTest {
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
    void healthReturnsStatusAndVersion() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.version", is("2.4.2")));
    }

    @Test
    void capacityAwareAllocationReturnsUnallocatedLoadAndRecommendation() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations.api-1", closeTo(10.0, 0.01)))
                .andExpect(jsonPath("$.allocations.worker-1", closeTo(20.0, 0.01)))
                .andExpect(jsonPath("$.unallocatedLoad", closeTo(45.0, 0.01)))
                .andExpect(jsonPath("$.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.scalingSimulation.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)))
                .andExpect(jsonPath("$.scalingSimulation.reason", containsString("simulated scale-up")));

        assertEquals(1.0, registry.counter(
                DomainMetrics.ALLOCATION_REQUESTS, "strategy", "CAPACITY_AWARE").count());
        assertEquals(2.0, registry.summary(
                DomainMetrics.ALLOCATION_SERVER_COUNT, "strategy", "CAPACITY_AWARE").totalAmount(), 0.01);
        assertEquals(30.0, registry.summary(
                DomainMetrics.ALLOCATION_ACCEPTED_LOAD, "strategy", "CAPACITY_AWARE").totalAmount(), 0.01);
        assertEquals(45.0, registry.summary(
                DomainMetrics.ALLOCATION_REJECTED_LOAD, "strategy", "CAPACITY_AWARE").totalAmount(), 0.01);
        assertEquals(45.0, registry.summary(
                DomainMetrics.ALLOCATION_UNALLOCATED_LOAD, "strategy", "CAPACITY_AWARE").totalAmount(), 0.01);
        assertEquals(1.0, registry.summary(
                DomainMetrics.ALLOCATION_SCALING_RECOMMENDED_SERVERS, "strategy", "CAPACITY_AWARE")
                .totalAmount(), 0.01);
    }

    @Test
    void capacityAwareSuccessResponseHasStableBrowserContractShape() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 10.0,
                                  "servers": [
                                    {
                                      "id": "api-1",
                                      "cpuUsage": 10.0,
                                      "memoryUsage": 20.0,
                                      "diskUsage": 30.0,
                                      "capacity": 100.0,
                                      "weight": 1.0,
                                      "healthy": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.allocations").isMap())
                .andExpect(jsonPath("$.unallocatedLoad").isNumber())
                .andExpect(jsonPath("$.recommendedAdditionalServers").isNumber())
                .andExpect(jsonPath("$.scalingSimulation").isMap())
                .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void predictiveAllocationReturnsUnallocatedLoadAndRecommendation() throws Exception {
        mockMvc.perform(post("/api/allocate/predictive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 20.0,
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
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations.api-1", closeTo(1.0, 0.01)))
                .andExpect(jsonPath("$.allocations.worker-1", closeTo(12.0, 0.01)))
                .andExpect(jsonPath("$.unallocatedLoad", closeTo(7.0, 0.01)))
                .andExpect(jsonPath("$.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.scalingSimulation.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)));

        assertEquals(1.0, registry.counter(
                DomainMetrics.ALLOCATION_REQUESTS, "strategy", "PREDICTIVE").count());
        assertEquals(2.0, registry.summary(
                DomainMetrics.ALLOCATION_SERVER_COUNT, "strategy", "PREDICTIVE").totalAmount(), 0.01);
        assertEquals(7.0, registry.summary(
                DomainMetrics.ALLOCATION_UNALLOCATED_LOAD, "strategy", "PREDICTIVE").totalAmount(), 0.01);
        assertEquals(1.0, registry.summary(
                DomainMetrics.ALLOCATION_SCALING_RECOMMENDED_SERVERS, "strategy", "PREDICTIVE")
                .totalAmount(), 0.01);
    }

    @Test
    void capacityAwareAllocationWithNoUnallocatedLoadRecommendsNoScaleUp() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 25.0,
                                  "servers": [
                                    {
                                      "id": "api-1",
                                      "cpuUsage": 10.0,
                                      "memoryUsage": 10.0,
                                      "diskUsage": 10.0,
                                      "capacity": 100.0,
                                      "weight": 1.0,
                                      "healthy": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unallocatedLoad", closeTo(0.0, 0.01)))
                .andExpect(jsonPath("$.recommendedAdditionalServers", is(0)))
                .andExpect(jsonPath("$.scalingSimulation.recommendedAdditionalServers", is(0)))
                .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)))
                .andExpect(jsonPath("$.scalingSimulation.reason", containsString("No unallocated load")));
    }

    @Test
    void capacityAwareAllocationExcludesUnhealthyServersAtApiBoundary() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 120.0,
                                  "servers": [
                                    {
                                      "id": "steady",
                                      "cpuUsage": 20.0,
                                      "memoryUsage": 20.0,
                                      "diskUsage": 20.0,
                                      "capacity": 100.0,
                                      "weight": 1.0,
                                      "healthy": true
                                    },
                                    {
                                      "id": "retired",
                                      "cpuUsage": 0.0,
                                      "memoryUsage": 0.0,
                                      "diskUsage": 0.0,
                                      "capacity": 1000.0,
                                      "weight": 10.0,
                                      "healthy": false
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations.steady", closeTo(80.0, 0.01)))
                .andExpect(jsonPath("$.allocations.retired").doesNotExist())
                .andExpect(jsonPath("$.unallocatedLoad", closeTo(40.0, 0.01)))
                .andExpect(jsonPath("$.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.scalingSimulation.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)));
    }

    @Test
    void capacityAwareAllocationWithAllServersUnhealthyReturnsUnallocatedLoadWithoutScaleCount() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(post("/api/allocate/capacity-aware")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "requestedLoad": 50.0,
                                      "servers": [
                                        {
                                          "id": "unhealthy",
                                          "cpuUsage": 10.0,
                                          "memoryUsage": 10.0,
                                          "diskUsage": 10.0,
                                          "capacity": 100.0,
                                          "weight": 1.0,
                                          "healthy": false
                                        }
                                      ]
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allocations").isMap())
                    .andExpect(jsonPath("$.allocations.unhealthy").doesNotExist())
                    .andExpect(jsonPath("$.unallocatedLoad", closeTo(50.0, 0.01)))
                    .andExpect(jsonPath("$.recommendedAdditionalServers", is(0)))
                    .andExpect(jsonPath("$.scalingSimulation.recommendedAdditionalServers", is(0)))
                    .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)))
                    .andExpect(jsonPath("$.scalingSimulation.reason", containsString("target capacity is unavailable")));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "All-unhealthy allocation must not construct CloudManager or call AWS paths.");
        }

        assertEquals(1.0, registry.counter(
                DomainMetrics.ALLOCATION_REQUESTS, "strategy", "CAPACITY_AWARE").count());
        assertEquals(0.0, registry.summary(
                DomainMetrics.ALLOCATION_SERVER_COUNT, "strategy", "CAPACITY_AWARE").totalAmount(), 0.01);
        assertEquals(0.0, registry.summary(
                DomainMetrics.ALLOCATION_ACCEPTED_LOAD, "strategy", "CAPACITY_AWARE").totalAmount(), 0.01);
        assertEquals(50.0, registry.summary(
                DomainMetrics.ALLOCATION_REJECTED_LOAD, "strategy", "CAPACITY_AWARE").totalAmount(), 0.01);
        assertEquals(50.0, registry.summary(
                DomainMetrics.ALLOCATION_UNALLOCATED_LOAD, "strategy", "CAPACITY_AWARE").totalAmount(), 0.01);
        assertEquals(0.0, registry.summary(
                DomainMetrics.ALLOCATION_SCALING_RECOMMENDED_SERVERS, "strategy", "CAPACITY_AWARE")
                .totalAmount(), 0.01);
    }

    @Test
    void repeatedOverloadedCapacityAwareRequestsRemainDeterministicObservableAndCloudSafe() throws Exception {
        String requestBody = """
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

        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            expectOverloadedCapacityAwareResponse(requestBody);
            expectOverloadedCapacityAwareResponse(requestBody);

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Degraded allocation and scale recommendation must not construct CloudManager or call AWS paths.");
        }

        assertEquals(2.0, registry.counter(
                DomainMetrics.ALLOCATION_REQUESTS, "strategy", "CAPACITY_AWARE").count());
        assertEquals(4.0, registry.summary(
                DomainMetrics.ALLOCATION_SERVER_COUNT, "strategy", "CAPACITY_AWARE").totalAmount(), 0.01);
        assertEquals(200.0, registry.summary(
                DomainMetrics.ALLOCATION_ACCEPTED_LOAD, "strategy", "CAPACITY_AWARE").totalAmount(), 0.01);
        assertEquals(100.0, registry.summary(
                DomainMetrics.ALLOCATION_REJECTED_LOAD, "strategy", "CAPACITY_AWARE").totalAmount(), 0.01);
        assertEquals(100.0, registry.summary(
                DomainMetrics.ALLOCATION_UNALLOCATED_LOAD, "strategy", "CAPACITY_AWARE").totalAmount(), 0.01);
        assertEquals(2.0, registry.summary(
                DomainMetrics.ALLOCATION_SCALING_RECOMMENDED_SERVERS, "strategy", "CAPACITY_AWARE")
                .totalAmount(), 0.01);
    }

    @Test
    void evaluationEndpointReturnsReadOnlyCapacityRecommendationWithoutAllocationMetrics() throws Exception {
        double beforeRequests = allocationRequestCount("CAPACITY_AWARE");

        mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 25.0,
                                  "strategy": "capacity-aware",
                                  "priority": "USER",
                                  "currentInFlightRequestCount": 25,
                                  "concurrencyLimit": 100,
                                  "queueDepth": 0,
                                  "observedP95LatencyMillis": 80.0,
                                  "observedErrorRate": 0.01,
                                  "servers": [
                                    {
                                      "id": "api-1",
                                      "cpuUsage": 10.0,
                                      "memoryUsage": 10.0,
                                      "diskUsage": 10.0,
                                      "capacity": 100.0,
                                      "weight": 1.0,
                                      "healthy": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy", is("CAPACITY_AWARE")))
                .andExpect(jsonPath("$.allocations.api-1", closeTo(25.0, 0.01)))
                .andExpect(jsonPath("$.acceptedLoad", closeTo(25.0, 0.01)))
                .andExpect(jsonPath("$.rejectedLoad", closeTo(0.0, 0.01)))
                .andExpect(jsonPath("$.unallocatedLoad", closeTo(0.0, 0.01)))
                .andExpect(jsonPath("$.recommendedAdditionalServers", is(0)))
                .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)))
                .andExpect(jsonPath("$.loadShedding.priority", is("USER")))
                .andExpect(jsonPath("$.loadShedding.action", is("ALLOW")))
                .andExpect(jsonPath("$.loadShedding.utilization", closeTo(0.25, 0.01)))
                .andExpect(jsonPath("$.metricsPreview.emitted", is(false)))
                .andExpect(jsonPath("$.metricsPreview.acceptedLoad", closeTo(25.0, 0.01)))
                .andExpect(jsonPath("$.metricsPreview.metricNames").isArray())
                .andExpect(jsonPath("$.readOnly", is(true)))
                .andExpect(jsonPath("$.remediationPlan.status", is("HEALTHY")))
                .andExpect(jsonPath("$.remediationPlan.advisoryOnly", is(true)))
                .andExpect(jsonPath("$.remediationPlan.cloudMutation", is(false)))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].action", is("NO_ACTION")))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].priority", is("LOW")))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].executable", is(false)))
                .andExpect(jsonPath("$.decisionReason", containsString("Read-only evaluation")));

        assertEquals(beforeRequests, allocationRequestCount("CAPACITY_AWARE"), 0.01,
                "Evaluation previews must not increment allocation request metrics.");
    }

    @Test
    void evaluationEndpointReportsOverloadPriorityDecisionAndStaysCloudSafe() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(post("/api/allocate/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(overloadedEvaluationRequest()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.strategy", is("CAPACITY_AWARE")))
                    .andExpect(jsonPath("$.allocations.primary", closeTo(70.0, 0.01)))
                    .andExpect(jsonPath("$.allocations.fallback", closeTo(30.0, 0.01)))
                    .andExpect(jsonPath("$.allocations.failed").doesNotExist())
                    .andExpect(jsonPath("$.acceptedLoad", closeTo(100.0, 0.01)))
                    .andExpect(jsonPath("$.rejectedLoad", closeTo(50.0, 0.01)))
                    .andExpect(jsonPath("$.unallocatedLoad", closeTo(50.0, 0.01)))
                    .andExpect(jsonPath("$.recommendedAdditionalServers", is(1)))
                    .andExpect(jsonPath("$.scalingSimulation.recommendedAdditionalServers", is(1)))
                    .andExpect(jsonPath("$.loadShedding.priority", is("BACKGROUND")))
                    .andExpect(jsonPath("$.loadShedding.action", is("SHED")))
                    .andExpect(jsonPath("$.loadShedding.reason", containsString("hard utilization")))
                    .andExpect(jsonPath("$.metricsPreview.evaluatedHealthyServerCount", is(2)))
                    .andExpect(jsonPath("$.metricsPreview.rejectedLoad", closeTo(50.0, 0.01)))
                    .andExpect(jsonPath("$.metricsPreview.emitted", is(false)))
                    .andExpect(jsonPath("$.readOnly", is(true)))
                    .andExpect(jsonPath("$.remediationPlan.status", is("OVERLOADED")))
                    .andExpect(jsonPath("$.remediationPlan.generatedFrom", is("allocation-evaluation")))
                    .andExpect(jsonPath("$.remediationPlan.advisoryOnly", is(true)))
                    .andExpect(jsonPath("$.remediationPlan.readOnly", is(true)))
                    .andExpect(jsonPath("$.remediationPlan.cloudMutation", is(false)))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[0].rank", is(1)))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[0].action", is("SCALE_UP")))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[0].priority", is("HIGH")))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[0].serverCount", is(1)))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[0].loadAmount", closeTo(50.0, 0.01)))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[0].executable", is(false)))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[1].action", is("SHED_LOAD")))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[1].priority", is("MEDIUM")))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[2].action", is("INVESTIGATE_UNHEALTHY")))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[2].serverCount", is(1)));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Evaluation endpoint must not construct CloudManager or call cloud mutation paths.");
        }
    }

    @Test
    void evaluationEndpointGracefullyReportsAllUnhealthyNoCapacity() throws Exception {
        mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 50.0,
                                  "servers": [
                                    {
                                      "id": "unhealthy",
                                      "cpuUsage": 10.0,
                                      "memoryUsage": 10.0,
                                      "diskUsage": 10.0,
                                      "capacity": 100.0,
                                      "weight": 1.0,
                                      "healthy": false
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations").isMap())
                .andExpect(jsonPath("$.allocations.unhealthy").doesNotExist())
                .andExpect(jsonPath("$.acceptedLoad", closeTo(0.0, 0.01)))
                .andExpect(jsonPath("$.rejectedLoad", closeTo(50.0, 0.01)))
                .andExpect(jsonPath("$.recommendedAdditionalServers", is(0)))
                .andExpect(jsonPath("$.scalingSimulation.reason", containsString("target capacity is unavailable")))
                .andExpect(jsonPath("$.loadShedding.action", is("SHED")))
                .andExpect(jsonPath("$.metricsPreview.evaluatedHealthyServerCount", is(0)))
                .andExpect(jsonPath("$.readOnly", is(true)))
                .andExpect(jsonPath("$.remediationPlan.status", is("NO_HEALTHY_CAPACITY")))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].action", is("RESTORE_CAPACITY")))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].priority", is("HIGH")))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].loadAmount", closeTo(50.0, 0.01)))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].executable", is(false)))
                .andExpect(jsonPath("$.remediationPlan.recommendations[1].action", is("RETRY_WHEN_HEALTHY")))
                .andExpect(jsonPath("$.remediationPlan.cloudMutation", is(false)));
    }

    @Test
    void repeatedEvaluationRequestsRemainDeterministicAndDoNotMutateMetrics() throws Exception {
        double beforeRequests = allocationRequestCount("CAPACITY_AWARE");

        String first = mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overloadedEvaluationRequest()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String second = mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overloadedEvaluationRequest()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(first, second, "Read-only evaluations should be deterministic for identical inputs.");
        assertEquals(beforeRequests, allocationRequestCount("CAPACITY_AWARE"), 0.01,
                "Repeated evaluation previews must not mutate allocation metrics.");
    }

    @Test
    void evaluationEndpointRejectsInvalidPriorityWithSafeErrorShape() throws Exception {
        mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 10.0,
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
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("priority must be one of")))
                .andExpect(jsonPath("$.path", is("/api/allocate/evaluate")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void scalingSimulationDoesNotConstructCloudManager() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(post("/api/allocate/capacity-aware")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
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
                                        }
                                      ]
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unallocatedLoad", closeTo(65.0, 0.01)))
                    .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Scaling simulation must not construct CloudManager or call AWS paths.");
        }
    }

    @Test
    void predictiveAllocationSkipsExhaustedServersAndDoesNotConstructCloudManager() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(post("/api/allocate/predictive")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "requestedLoad": 90.0,
                                      "servers": [
                                        {
                                          "id": "available",
                                          "cpuUsage": 20.0,
                                          "memoryUsage": 20.0,
                                          "diskUsage": 20.0,
                                          "capacity": 100.0,
                                          "weight": 1.0,
                                          "healthy": true
                                        },
                                        {
                                          "id": "exhausted",
                                          "cpuUsage": 100.0,
                                          "memoryUsage": 100.0,
                                          "diskUsage": 100.0,
                                          "capacity": 100.0,
                                          "weight": 1.0,
                                          "healthy": true
                                        }
                                      ]
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allocations.available", closeTo(78.0, 0.01)))
                    .andExpect(jsonPath("$.allocations.exhausted").doesNotExist())
                    .andExpect(jsonPath("$.unallocatedLoad", closeTo(12.0, 0.01)))
                    .andExpect(jsonPath("$.recommendedAdditionalServers", is(1)))
                    .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Predictive allocation must not construct CloudManager or call AWS paths.");
        }
    }

    @Test
    void allocationRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": -1.0,
                                  "servers": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.message", is("Request validation failed")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void allocationRejectsNullServerListWithSafeErrorShape() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 10.0,
                                  "servers": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void allocationRejectsEmptyServerListWithSafeErrorShape() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 10.0,
                                  "servers": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void missingRequestBodyReturnsSafeErrorShape() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", is("Malformed JSON request body")))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void allocationRejectsOmittedRequestedLoadWithSafeValidationShape() throws Exception {
        String responseBody = mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "servers": [
                                    {
                                      "id": "api-1",
                                      "cpuUsage": 10.0,
                                      "memoryUsage": 20.0,
                                      "diskUsage": 30.0,
                                      "capacity": 100.0,
                                      "weight": 1.0,
                                      "healthy": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(responseBody.contains("requestedLoad"), "omitted requestedLoad must be named in validation output");
        assertFalse(responseBody.contains("stackTrace"), "validation output must not expose stack traces");
    }

    @Test
    void evaluationRejectsOmittedRequestedLoadWithSafeValidationShape() throws Exception {
        String responseBody = mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "CAPACITY_AWARE",
                                  "servers": [
                                    {
                                      "id": "api-1",
                                      "cpuUsage": 10.0,
                                      "memoryUsage": 20.0,
                                      "diskUsage": 30.0,
                                      "capacity": 100.0,
                                      "weight": 1.0,
                                      "healthy": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/api/allocate/evaluate")))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(responseBody.contains("requestedLoad"), "omitted requestedLoad must be named in validation output");
        assertFalse(responseBody.contains("stackTrace"), "validation output must not expose stack traces");
    }

    @Test
    void allocationRejectsOmittedServerNumericFieldWithSafeValidationShape() throws Exception {
        String responseBody = mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 10.0,
                                  "servers": [
                                    {
                                      "id": "api-1",
                                      "memoryUsage": 20.0,
                                      "diskUsage": 30.0,
                                      "capacity": 100.0,
                                      "weight": 1.0,
                                      "healthy": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(responseBody.contains("cpuUsage"), "omitted cpuUsage must be named in validation output");
        assertFalse(responseBody.contains("stackTrace"), "validation output must not expose stack traces");
    }

    @Test
    void allocationRejectsOmittedServerHealthFlagWithSafeValidationShape() throws Exception {
        String responseBody = mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 10.0,
                                  "servers": [
                                    {
                                      "id": "api-1",
                                      "cpuUsage": 10.0,
                                      "memoryUsage": 20.0,
                                      "diskUsage": 30.0,
                                      "capacity": 100.0,
                                      "weight": 1.0
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(responseBody.contains("healthy"), "omitted healthy flag must be named in validation output");
        assertFalse(responseBody.contains("stackTrace"), "validation output must not expose stack traces");
    }

    @Test
    void allocationAcceptsExplicitZeroRequestedLoadAndServerTelemetry() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 0.0,
                                  "servers": [
                                    {
                                      "id": "api-1",
                                      "cpuUsage": 0.0,
                                      "memoryUsage": 0.0,
                                      "diskUsage": 0.0,
                                      "capacity": 0.0,
                                      "weight": 0.0,
                                      "healthy": true
                                    }
                                  ]
                                }
                """))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.allocations").isMap())
        .andExpect(jsonPath("$.unallocatedLoad", closeTo(0.0, 0.01)))
        .andExpect(jsonPath("$.recommendedAdditionalServers", is(0)))
        .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void veryLargeFiniteRequestedLoadReturnsSafeFiniteJson() throws Exception {
        String responseBody = mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 1000000000.0,
                                  "servers": [
                                    {
                                      "id": "api-1",
                                      "cpuUsage": 10.0,
                                      "memoryUsage": 20.0,
                                      "diskUsage": 30.0,
                                      "capacity": 100.0,
                                      "weight": 1.0,
                                      "healthy": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.allocations.api-1").isNumber())
                .andExpect(jsonPath("$.unallocatedLoad").isNumber())
                .andExpect(jsonPath("$.recommendedAdditionalServers").isNumber())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(responseBody.contains("Infinity"), "Response must not serialize overflow-looking Infinity values.");
        assertFalse(responseBody.contains("NaN"), "Response must not serialize NaN values.");
        assertFalse(responseBody.contains("stackTrace"), "Response must not leak stack traces.");
    }

    @Test
    void allocationRejectsInvalidServerInput() throws Exception {
        mockMvc.perform(post("/api/allocate/predictive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 10.0,
                                  "servers": [
                                    {
                                      "id": "",
                                      "cpuUsage": 150.0,
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
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void allocationRejectsNegativeServerCapacityWithSafeValidationShape() throws Exception {
        mockMvc.perform(post("/api/allocate/predictive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 10.0,
                                  "servers": [
                                    {
                                      "id": "api-1",
                                      "cpuUsage": 10.0,
                                      "memoryUsage": 20.0,
                                      "diskUsage": 20.0,
                                      "capacity": -1.0,
                                      "weight": 1.0,
                                      "healthy": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/api/allocate/predictive")))
                .andExpect(jsonPath("$.details[0]", containsString("capacity")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());

        assertEquals(1.0, registry.counter(
                DomainMetrics.ALLOCATION_VALIDATION_FAILURES,
                "path", "/api/allocate/predictive",
                "reason", "validation_failed").count());
        assertEquals(0.0, registry.counter(
                DomainMetrics.ALLOCATION_REQUESTS, "strategy", "PREDICTIVE").count());
    }

    @Test
    void invalidHttpMethodReturnsSafeErrorShape() throws Exception {
        mockMvc.perform(put("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 10.0,
                                  "servers": []
                                }
                                """))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(405)))
                .andExpect(jsonPath("$.error", is("method_not_allowed")))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void invalidContentTypeReturnsSafeErrorShape() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("""
                                {
                                  "requestedLoad": 10.0,
                                  "servers": []
                                }
                                """))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(415)))
                .andExpect(jsonPath("$.error", is("unsupported_media_type")))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void unknownApiRouteReturnsFramework404WithoutBodyOrDiagnostics() throws Exception {
        String responseBody = mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Content-Type"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(responseBody.isEmpty(), "Framework 404 currently returns no response body.");
        assertFalse(responseBody.contains("trace"), "Framework 404 response must not expose trace data.");
        assertFalse(responseBody.contains("exception"), "Framework 404 response must not expose exception data.");
        assertFalse(responseBody.contains("stackTrace"), "Framework 404 response must not expose stack traces.");
    }

    @Test
    void malformedJsonReturnsConsistentSafeErrorShape() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
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
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());

        assertEquals(1.0, registry.counter(
                DomainMetrics.ALLOCATION_VALIDATION_FAILURES,
                "path", "/api/allocate/capacity-aware",
                "reason", "bad_request").count());
        assertEquals(0.0, registry.counter(
                DomainMetrics.ALLOCATION_REQUESTS, "strategy", "CAPACITY_AWARE").count());
    }

    @Test
    void oversizedJsonRequestIsRejectedWithSafeErrorShape() throws Exception {
        String oversizedId = "S".repeat(20_000);
        String oversizedBody = """
                {
                  "requestedLoad": 10.0,
                  "servers": [
                    {
                      "id": "%s",
                      "cpuUsage": 10.0,
                      "memoryUsage": 20.0,
                      "diskUsage": 30.0,
                      "capacity": 100.0,
                      "weight": 1.0,
                      "healthy": true
                    }
                  ]
                }
                """.formatted(oversizedId);

        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversizedBody))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(413)))
                .andExpect(jsonPath("$.error", is("payload_too_large")))
                .andExpect(jsonPath("$.message", containsString("Request body exceeds")))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void corsPreflightAllowsConfiguredBrowserOrigins() throws Exception {
        mockMvc.perform(options("/api/allocate/capacity-aware")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Content-Type")));
    }

    @Test
    void corsPreflightDeniesUnconfiguredOrigins() throws Exception {
        mockMvc.perform(options("/api/allocate/capacity-aware")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void apiResponsesIncludeExpectedSecurityHeadersWithoutLocalHsts() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    @Test
    void actuatorHealthInfoAndMetricsAreAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name", is("LoadBalancerPro")));

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_info")));
    }

    @Test
    void actuatorDoesNotExposeEnvironmentEndpointByDefault() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actuatorReadinessIsAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void swaggerUiAndOpenApiDocsAreAvailable() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths./api/allocate/capacity-aware").exists())
                .andExpect(jsonPath("$.paths./api/allocate/evaluate").exists());
    }

    private String overloadedEvaluationRequest() {
        return """
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
    }

    private double allocationRequestCount(String strategy) {
        var counter = registry.find(DomainMetrics.ALLOCATION_REQUESTS)
                .tag("strategy", strategy)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }

    private void expectOverloadedCapacityAwareResponse(String requestBody) throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations.primary", closeTo(70.0, 0.01)))
                .andExpect(jsonPath("$.allocations.fallback", closeTo(30.0, 0.01)))
                .andExpect(jsonPath("$.allocations.failed").doesNotExist())
                .andExpect(jsonPath("$.unallocatedLoad", closeTo(50.0, 0.01)))
                .andExpect(jsonPath("$.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.scalingSimulation.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)))
                .andExpect(jsonPath("$.scalingSimulation.reason", containsString("simulated scale-up")));
    }
}
