package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.richmond423.loadbalancerpro.core.CloudManager;
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
class ScenarioReplayControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void normalScenarioReplayReturnsDeterministicReadOnlyAllocationPreview() throws Exception {
        String first = mockMvc.perform(scenarioReplay("""
                        {
                          "scenarioId": "normal",
                          "servers": [
                            {
                              "id": "green",
                              "cpuUsage": 20.0,
                              "memoryUsage": 20.0,
                              "diskUsage": 20.0,
                              "capacity": 100.0,
                              "weight": 1.0,
                              "healthy": true
                            },
                            {
                              "id": "blue",
                              "cpuUsage": 20.0,
                              "memoryUsage": 20.0,
                              "diskUsage": 20.0,
                              "capacity": 100.0,
                              "weight": 1.0,
                              "healthy": true
                            }
                          ],
                          "steps": [
                            {
                              "stepId": "allocate-normal",
                              "type": "ALLOCATE",
                              "requestedLoad": 50.0,
                              "strategy": "CAPACITY_AWARE"
                            }
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.scenarioId", is("normal")))
                .andExpect(jsonPath("$.readOnly", is(true)))
                .andExpect(jsonPath("$.cloudMutation", is(false)))
                .andExpect(jsonPath("$.remediationPlan.status", is("HEALTHY")))
                .andExpect(jsonPath("$.remediationPlan.advisoryOnly", is(true)))
                .andExpect(jsonPath("$.remediationPlan.cloudMutation", is(false)))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].action", is("NO_ACTION")))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].executable", is(false)))
                .andExpect(jsonPath("$.steps[0].stepId", is("allocate-normal")))
                .andExpect(jsonPath("$.steps[0].type", is("ALLOCATE")))
                .andExpect(jsonPath("$.steps[0].strategy", is("CAPACITY_AWARE")))
                .andExpect(jsonPath("$.steps[0].acceptedLoad", closeTo(50.0, 0.01)))
                .andExpect(jsonPath("$.steps[0].unallocatedLoad", closeTo(0.0, 0.01)))
                .andExpect(jsonPath("$.steps[0].recommendedAdditionalServers", is(0)))
                .andExpect(jsonPath("$.steps[0].metricsPreview.emitted", is(false)))
                .andExpect(jsonPath("$.steps[0].serverStates[0].healthy", is(true)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String second = mockMvc.perform(scenarioReplay("""
                        {
                          "scenarioId": "normal",
                          "servers": [
                            {
                              "id": "green",
                              "cpuUsage": 20.0,
                              "memoryUsage": 20.0,
                              "diskUsage": 20.0,
                              "capacity": 100.0,
                              "weight": 1.0,
                              "healthy": true
                            },
                            {
                              "id": "blue",
                              "cpuUsage": 20.0,
                              "memoryUsage": 20.0,
                              "diskUsage": 20.0,
                              "capacity": 100.0,
                              "weight": 1.0,
                              "healthy": true
                            }
                          ],
                          "steps": [
                            {
                              "stepId": "allocate-normal",
                              "type": "ALLOCATE",
                              "requestedLoad": 50.0,
                              "strategy": "CAPACITY_AWARE"
                            }
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(first, second, "Scenario replay output should be deterministic for the same input.");
    }

    @Test
    void overloadScenarioReportsUnallocatedLoadAndScalingRecommendation() throws Exception {
        mockMvc.perform(scenarioReplay(overloadScenario("OVERLOAD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[0].type", is("OVERLOAD")))
                .andExpect(jsonPath("$.steps[0].acceptedLoad", closeTo(100.0, 0.01)))
                .andExpect(jsonPath("$.steps[0].rejectedLoad", closeTo(50.0, 0.01)))
                .andExpect(jsonPath("$.steps[0].unallocatedLoad", closeTo(50.0, 0.01)))
                .andExpect(jsonPath("$.steps[0].recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.steps[0].loadShedding.action", is("SHED")))
                .andExpect(jsonPath("$.steps[0].metricsPreview.emitted", is(false)))
                .andExpect(jsonPath("$.steps[0].reason", containsString("Simulated overload preview")))
                .andExpect(jsonPath("$.remediationPlan.status", is("OVERLOADED")))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].action", is("SCALE_UP")))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].serverCount", is(1)))
                .andExpect(jsonPath("$.remediationPlan.recommendations[1].action", is("SHED_LOAD")))
                .andExpect(jsonPath("$.remediationPlan.recommendations[1].loadAmount", closeTo(50.0, 0.01)));
    }

    @Test
    void failureRecoveryScenarioUpdatesOnlyReplayLocalServerStateAndRoutesDeterministically() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(scenarioReplay("""
                            {
                              "scenarioId": "failure-recovery",
                              "servers": [
                                {
                                  "id": "green",
                                  "cpuUsage": 5.0,
                                  "memoryUsage": 10.0,
                                  "diskUsage": 10.0,
                                  "capacity": 100.0,
                                  "weight": 1.0,
                                  "healthy": true
                                },
                                {
                                  "id": "blue",
                                  "cpuUsage": 15.0,
                                  "memoryUsage": 10.0,
                                  "diskUsage": 10.0,
                                  "capacity": 100.0,
                                  "weight": 1.0,
                                  "healthy": true
                                }
                              ],
                              "steps": [
                                {
                                  "stepId": "fail-green",
                                  "type": "MARK_UNHEALTHY",
                                  "serverId": "green"
                                },
                                {
                                  "stepId": "route-around",
                                  "type": "ROUTE",
                                  "routingStrategies": ["ROUND_ROBIN"]
                                },
                                {
                                  "stepId": "recover-green",
                                  "type": "MARK_HEALTHY",
                                  "serverId": "green"
                                },
                                {
                                  "stepId": "route-after-recovery",
                                  "type": "ROUTE",
                                  "routingStrategies": ["ROUND_ROBIN"]
                                }
                              ]
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.steps[0].serverStates[0].healthy", is(false)))
                    .andExpect(jsonPath("$.steps[1].selectedServerId", is("blue")))
                    .andExpect(jsonPath("$.steps[1].routingResults[0].chosenServerId", is("blue")))
                    .andExpect(jsonPath("$.steps[2].serverStates[0].healthy", is(true)))
                    .andExpect(jsonPath("$.steps[3].selectedServerId", is("green")))
                    .andExpect(jsonPath("$.steps[3].routingResults[0].chosenServerId", is("green")))
                    .andExpect(jsonPath("$.remediationPlan.status", is("DEGRADED")))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[0].action", is("INVESTIGATE_UNHEALTHY")))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[0].serverCount", is(1)));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Scenario replay must not construct CloudManager or call cloud mutation paths.");
        }
    }

    @Test
    void allUnhealthyScenarioDegradesGracefullyWithoutCloudMutation() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(scenarioReplay("""
                            {
                              "scenarioId": "all-unhealthy",
                              "servers": [
                                {
                                  "id": "red",
                                  "cpuUsage": 20.0,
                                  "memoryUsage": 20.0,
                                  "diskUsage": 20.0,
                                  "capacity": 100.0,
                                  "weight": 1.0,
                                  "healthy": false
                                }
                              ],
                              "steps": [
                                {
                                  "stepId": "evaluate-outage",
                                  "type": "EVALUATE",
                                  "requestedLoad": 80.0,
                                  "strategy": "CAPACITY_AWARE"
                                },
                                {
                                  "stepId": "route-outage",
                                  "type": "ROUTE",
                                  "routingStrategies": ["TAIL_LATENCY_POWER_OF_TWO"]
                                }
                              ]
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.steps[0].acceptedLoad", closeTo(0.0, 0.01)))
                    .andExpect(jsonPath("$.steps[0].unallocatedLoad", closeTo(80.0, 0.01)))
                    .andExpect(jsonPath("$.steps[0].recommendedAdditionalServers", is(0)))
                    .andExpect(jsonPath("$.steps[1].selectedServerId", nullValue()))
                    .andExpect(jsonPath("$.steps[1].routingResults[0].chosenServerId", nullValue()))
                    .andExpect(jsonPath("$.steps[1].reason", containsString("no healthy")))
                    .andExpect(jsonPath("$.remediationPlan.status", is("NO_HEALTHY_CAPACITY")))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[0].action", is("RESTORE_CAPACITY")))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[0].priority", is("HIGH")))
                    .andExpect(jsonPath("$.remediationPlan.recommendations[1].action", is("RETRY_WHEN_HEALTHY")));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "All-unhealthy replay must not construct CloudManager or call cloud mutation paths.");
        }
    }

    @Test
    void invalidScenarioStepReturnsControlledBadRequest() throws Exception {
        mockMvc.perform(scenarioReplay("""
                        {
                          "servers": [
                            {
                              "id": "green",
                              "cpuUsage": 20.0,
                              "memoryUsage": 20.0,
                              "diskUsage": 20.0,
                              "capacity": 100.0,
                              "weight": 1.0,
                              "healthy": true
                            }
                          ],
                          "steps": [
                            {
                              "type": "TELEPORT",
                              "requestedLoad": 50.0
                            }
                          ]
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("Unsupported scenario step type")))
                .andExpect(jsonPath("$.path", is("/api/scenarios/replay")));
    }

    @Test
    void malformedScenarioJsonReturnsControlledBadRequest() throws Exception {
        mockMvc.perform(post("/api/scenarios/replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "servers": [
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", is("Malformed JSON request body")))
                .andExpect(jsonPath("$.path", is("/api/scenarios/replay")));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder scenarioReplay(String body) {
        return post("/api/scenarios/replay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String overloadScenario(String type) {
        return """
                {
                  "scenarioId": "overload",
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
                  ],
                  "steps": [
                    {
                      "stepId": "overload-check",
                      "type": "%s",
                      "requestedLoad": 150.0,
                      "strategy": "CAPACITY_AWARE",
                      "priority": "BACKGROUND",
                      "currentInFlightRequestCount": 95,
                      "concurrencyLimit": 100,
                      "queueDepth": 25,
                      "observedP95LatencyMillis": 300.0,
                      "observedErrorRate": 0.20
                    }
                  ]
                }
                """.formatted(type);
    }
}
