package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class RoutingControllerTest {
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
                  "queueDepth": 1,
                  "networkAwareness": {
                    "timeoutRate": 0.0,
                    "retryRate": 0.0,
                    "connectionFailureRate": 0.0,
                    "latencyJitterMillis": 4.0,
                    "recentErrorBurst": false,
                    "requestTimeoutCount": 0,
                    "sampleSize": 120
                  }
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
    void validLocalRequestReturnsTailLatencyPowerOfTwoResultWithoutCloudMutationPath() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(routingCompare(VALID_REQUEST))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.requestedStrategies[0]", is("TAIL_LATENCY_POWER_OF_TWO")))
                    .andExpect(jsonPath("$.candidateCount", is(2)))
                    .andExpect(jsonPath("$.results[0].strategyId", is("TAIL_LATENCY_POWER_OF_TWO")))
                    .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                    .andExpect(jsonPath("$.results[0].chosenServerId", is("green")))
                    .andExpect(jsonPath("$.results[0].reason", containsString("Chose green")))
                    .andExpect(jsonPath("$.results[0].candidateServersConsidered[0]", is("green")))
                    .andExpect(jsonPath("$.results[0].candidateServersConsidered[1]", is("blue")))
                    .andExpect(jsonPath("$.results[0].scores.green").isNumber())
                    .andExpect(jsonPath("$.results[0].scores.blue").isNumber())
                    .andExpect(jsonPath("$.error").doesNotExist());

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Routing comparison must not construct CloudManager or call AWS paths.");
        }
    }

    @Test
    void decisionExplorerEndpointReturnsReadOnlySimulationOnlyPayloadWithoutCloudMutationPath() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(routingDecisionExplorer(VALID_REQUEST))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].readOnly", is(true)))
                    .andExpect(jsonPath("$[0].simulationOnly", is(true)))
                    .andExpect(jsonPath("$[0].payloadObject", is("DecisionExplorerPayloadV1")))
                    .andExpect(jsonPath("$[0].contractVersion", is("v1")))
                    .andExpect(jsonPath("$[0].source", containsString("/api/routing/compare")))
                    .andExpect(jsonPath("$[0].decisionReadout.selectedCandidateId", is("green")))
                    .andExpect(jsonPath("$[0].selectedCandidate.candidateId", is("green")))
                    .andExpect(jsonPath("$[0].candidateSet[0].candidateId", is("green")))
                    .andExpect(jsonPath("$[0].candidateSet[0].selected", is(true)))
                    .andExpect(jsonPath("$[0].candidateSet[1].candidateId", is("blue")))
                    .andExpect(jsonPath("$[0].factorContributions[0].factorName").exists())
                    .andExpect(jsonPath("$[0].routingDiagnostics.readOnly", is(true)))
                    .andExpect(jsonPath("$[0].routingDiagnostics.simulationOnly", is(true)))
                    .andExpect(jsonPath("$[0].routingDiagnostics.diagnosticsObject",
                            is("DecisionExplorerRoutingDiagnosticsV1")))
                    .andExpect(jsonPath("$[0].routingDiagnostics.overallStatus", is("PARTIAL")))
                    .andExpect(jsonPath("$[0].routingDiagnostics.selectedCandidateId", is("green")))
                    .andExpect(jsonPath("$[0].routingDiagnostics.explanationText",
                            containsString("selected candidate green as PARTIAL")))
                    .andExpect(jsonPath("$[0].routingDiagnostics.evidenceDiagnostics[0].diagnosticId").exists())
                    .andExpect(jsonPath("$[0].routingDiagnostics.selectedCandidateDiagnostic.candidateId",
                            is("green")))
                    .andExpect(jsonPath("$[0].routingDiagnostics.candidateDiagnostics[0].candidateId",
                            is("green")))
                    .andExpect(jsonPath("$[0].routingDiagnostics.factorDiagnostics[0].factorName").exists())
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.readOnly", is(true)))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.simulationOnly", is(true)))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.analysisObject",
                            is("DecisionExplorerRouteTradeoffAnalysisV1")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.overallStatus", is("PARTIAL")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.selectedCandidateId", is("green")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.fingerprintAlgorithm",
                            is(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM)))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.diagnosticFingerprint",
                            containsString("route-tradeoff|v1|")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.explanationText",
                            containsString("selected candidate green is PARTIAL")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.evidenceSufficiency.diagnosticFingerprint",
                            containsString("evidence-sufficiency|v1|")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.replayReadinessDiagnostic.diagnosticFingerprint",
                            containsString("replay-readiness|v1|")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.candidateTradeoffs[0].candidateId",
                            is("green")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.candidateScoringExplanations[0].candidateId",
                            is("green")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.evidenceSufficiency.diagnosticObject",
                            is("DecisionExplorerEvidenceSufficiencyV1")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.replayReadinessDiagnostic.diagnosticObject",
                            is("DecisionExplorerReplayReadinessDiagnosticV1")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.replayReadinessDiagnostic.replayExecutionAvailable",
                            is(false)))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.readOnly", is(true)))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.simulationOnly", is(true)))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.evaluationObject",
                            is("DecisionExplorerShadowDecisionQualityEvaluationV1")))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.qualityLabel",
                            is("REVIEW_RECOMMENDED")))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.selectedCandidateId",
                            is("green")))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.fingerprintAlgorithm",
                            is(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM)))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.diagnosticFingerprint",
                            containsString("shadow-decision-quality|v1|")))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.reproducibilityKey").exists())
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.fingerprintInputs[0]").exists())
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.candidateOutcomeComparisons[0].candidateId",
                            is("green")))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.policySensitivityDiagnostic.diagnosticObject",
                            is("DecisionExplorerShadowPolicySensitivityDiagnosticV1")))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.scenarioInputQuality.evaluationObject",
                            is("DecisionExplorerShadowScenarioInputQualityV1")))
                    .andExpect(jsonPath("$[0].policyGateReadouts[0].gateId", is("boundary-read-only")))
                    .andExpect(jsonPath("$[0].policyGateReadouts[0].outcome", is("PASS")))
                    .andExpect(jsonPath("$[0].agentStructuredOutput.schemaName", is("AgentStructuredOutputV1")))
                    .andExpect(jsonPath("$[0].notProvenBoundaries", hasItem("no production readiness")))
                    .andExpect(jsonPath("$[0].notProvenBoundaries", hasItem("no storage proof")))
                    .andExpect(jsonPath("$[0].notProvenBoundaries", hasItem("no evidence-packet generation")))
                    .andExpect(jsonPath("$[0].notProvenBoundaries", not(hasItem(
                            "no Decision Explorer endpoint, UI, storage, export, replay execution, or evidence-packet generation"))))
                    .andExpect(jsonPath("$[0].boundaryNote", containsString("does not change routing behavior")));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Decision Explorer endpoint must not construct CloudManager or call AWS paths.");
        }
    }

    @Test
    void decisionExplorerScenarioCatalogEndpointReturnsReadOnlyLocalSyntheticCatalog() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(get("/api/routing/decision-explorer/scenarios"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.readOnly", is(true)))
                    .andExpect(jsonPath("$.simulationOnly", is(true)))
                    .andExpect(jsonPath("$.payloadObject", is("DecisionExplorerScenarioCatalogV1")))
                    .andExpect(jsonPath("$.contractVersion", is("v1")))
                    .andExpect(jsonPath("$.source", containsString("AdaptiveRoutingExperimentFixtureCatalog")))
                    .andExpect(jsonPath("$.scenarios[0].scenarioObject", is("DecisionExplorerScenarioV1")))
                    .andExpect(jsonPath("$.scenarios[0].scenarioId", is("normal-balanced-load")))
                    .andExpect(jsonPath("$.scenarios[0].scenarioCategory", is("HEALTHY_BASELINE")))
                    .andExpect(jsonPath("$.scenarios[0].evidenceStatus", is("AVAILABLE")))
                    .andExpect(jsonPath("$.scenarios[3].scenarioId", is("stale-signal")))
                    .andExpect(jsonPath("$.scenarios[3].scenarioCategory", is("PARTIAL_EVIDENCE")))
                    .andExpect(jsonPath("$.scenarios[5].scenarioId", is("all-unhealthy-degradation")))
                    .andExpect(jsonPath("$.scenarios[5].scenarioCategory", is("NO_HEALTHY_SERVER")))
                    .andExpect(jsonPath("$.scenarios[5].evidenceStatus", is("UNKNOWN")))
                    .andExpect(jsonPath("$.notProvenBoundaries", hasItem("no production readiness")))
                    .andExpect(jsonPath("$.notProvenBoundaries", hasItem("no storage proof")))
                    .andExpect(jsonPath("$.notProvenBoundaries", hasItem("no evidence-packet generation")))
                    .andExpect(jsonPath("$.boundaryNote", containsString("does not run routing")));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Decision Explorer scenario catalog endpoint must not construct CloudManager or call AWS paths.");
        }
    }

    @Test
    void missingStrategiesDefaultsToRegisteredRoutingStrategiesInOrder() throws Exception {
        mockMvc.perform(routingCompare("""
                        {
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
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedStrategies[0]", is("TAIL_LATENCY_POWER_OF_TWO")))
                .andExpect(jsonPath("$.requestedStrategies[1]", is("WEIGHTED_LEAST_LOAD")))
                .andExpect(jsonPath("$.requestedStrategies[2]", is("WEIGHTED_LEAST_CONNECTIONS")))
                .andExpect(jsonPath("$.requestedStrategies[3]", is("WEIGHTED_ROUND_ROBIN")))
                .andExpect(jsonPath("$.requestedStrategies[4]", is("ROUND_ROBIN")))
                .andExpect(jsonPath("$.results[0].strategyId", is("TAIL_LATENCY_POWER_OF_TWO")))
                .andExpect(jsonPath("$.results[0].chosenServerId", is("green")))
                .andExpect(jsonPath("$.results[1].strategyId", is("WEIGHTED_LEAST_LOAD")))
                .andExpect(jsonPath("$.results[1].chosenServerId", is("green")))
                .andExpect(jsonPath("$.results[2].strategyId", is("WEIGHTED_LEAST_CONNECTIONS")))
                .andExpect(jsonPath("$.results[2].chosenServerId", is("green")))
                .andExpect(jsonPath("$.results[3].strategyId", is("WEIGHTED_ROUND_ROBIN")))
                .andExpect(jsonPath("$.results[3].chosenServerId", is("green")))
                .andExpect(jsonPath("$.results[4].strategyId", is("ROUND_ROBIN")))
                .andExpect(jsonPath("$.results[4].chosenServerId", is("green")));
    }

    @Test
    void explicitMultiStrategyRequestPreservesCallerOrder() throws Exception {
        mockMvc.perform(routingCompare("""
                        {
                          "strategies": [
                            "WEIGHTED_LEAST_CONNECTIONS",
                            "WEIGHTED_LEAST_LOAD"
                          ],
                          "servers": [
                            {
                              "serverId": "green",
                              "healthy": true,
                              "inFlightRequestCount": 1,
                              "weight": 1.0,
                              "averageLatencyMillis": 10.0,
                              "p95LatencyMillis": 20.0,
                              "p99LatencyMillis": 30.0,
                              "recentErrorRate": 0.0
                            },
                            {
                              "serverId": "blue",
                              "healthy": true,
                              "inFlightRequestCount": 4,
                              "weight": 2.0,
                              "averageLatencyMillis": 12.0,
                              "p95LatencyMillis": 24.0,
                              "p99LatencyMillis": 36.0,
                              "recentErrorRate": 0.0
                            }
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedStrategies[0]", is("WEIGHTED_LEAST_CONNECTIONS")))
                .andExpect(jsonPath("$.requestedStrategies[1]", is("WEIGHTED_LEAST_LOAD")))
                .andExpect(jsonPath("$.results[0].strategyId", is("WEIGHTED_LEAST_CONNECTIONS")))
                .andExpect(jsonPath("$.results[1].strategyId", is("WEIGHTED_LEAST_LOAD")));
    }

    @Test
    void explicitWeightedLeastLoadRequestUsesRoutingWeightWithoutCloudMutationPath() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(routingCompare("""
                            {
                              "strategies": ["WEIGHTED_LEAST_LOAD"],
                              "servers": [
                                {
                                  "serverId": "base",
                                  "healthy": true,
                                  "inFlightRequestCount": 20,
                                  "configuredCapacity": 100.0,
                                  "estimatedConcurrencyLimit": 100.0,
                                  "weight": 1.0,
                                  "averageLatencyMillis": 10.0,
                                  "p95LatencyMillis": 20.0,
                                  "p99LatencyMillis": 40.0,
                                  "recentErrorRate": 0.0,
                                  "queueDepth": 0
                                },
                                {
                                  "serverId": "weighted",
                                  "healthy": true,
                                  "inFlightRequestCount": 20,
                                  "configuredCapacity": 100.0,
                                  "estimatedConcurrencyLimit": 100.0,
                                  "weight": 4.0,
                                  "averageLatencyMillis": 10.0,
                                  "p95LatencyMillis": 20.0,
                                  "p99LatencyMillis": 40.0,
                                  "recentErrorRate": 0.0,
                                  "queueDepth": 0
                                }
                              ]
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestedStrategies[0]", is("WEIGHTED_LEAST_LOAD")))
                    .andExpect(jsonPath("$.candidateCount", is(2)))
                    .andExpect(jsonPath("$.results[0].strategyId", is("WEIGHTED_LEAST_LOAD")))
                    .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                    .andExpect(jsonPath("$.results[0].chosenServerId", is("weighted")))
                    .andExpect(jsonPath("$.results[0].candidateServersConsidered[0]", is("base")))
                    .andExpect(jsonPath("$.results[0].candidateServersConsidered[1]", is("weighted")))
                    .andExpect(jsonPath("$.results[0].scores.base").isNumber())
                    .andExpect(jsonPath("$.results[0].scores.weighted").isNumber())
                    .andExpect(jsonPath("$.error").doesNotExist());

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Weighted routing comparison must not construct CloudManager or call AWS paths.");
        }
    }

    @Test
    void explicitWeightedRoundRobinRequestUsesRoutingWeightWithoutCloudMutationPath() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(routingCompare("""
                            {
                              "strategies": ["WEIGHTED_ROUND_ROBIN"],
                              "servers": [
                                {
                                  "serverId": "base",
                                  "healthy": true,
                                  "inFlightRequestCount": 20,
                                  "configuredCapacity": 100.0,
                                  "estimatedConcurrencyLimit": 100.0,
                                  "weight": 1.0,
                                  "averageLatencyMillis": 10.0,
                                  "p95LatencyMillis": 20.0,
                                  "p99LatencyMillis": 40.0,
                                  "recentErrorRate": 0.0,
                                  "queueDepth": 0
                                },
                                {
                                  "serverId": "weighted",
                                  "healthy": true,
                                  "inFlightRequestCount": 20,
                                  "configuredCapacity": 100.0,
                                  "estimatedConcurrencyLimit": 100.0,
                                  "weight": 4.0,
                                  "averageLatencyMillis": 10.0,
                                  "p95LatencyMillis": 20.0,
                                  "p99LatencyMillis": 40.0,
                                  "recentErrorRate": 0.0,
                                  "queueDepth": 0
                                }
                              ]
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestedStrategies[0]", is("WEIGHTED_ROUND_ROBIN")))
                    .andExpect(jsonPath("$.candidateCount", is(2)))
                    .andExpect(jsonPath("$.results[0].strategyId", is("WEIGHTED_ROUND_ROBIN")))
                    .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                    .andExpect(jsonPath("$.results[0].chosenServerId", is("weighted")))
                    .andExpect(jsonPath("$.results[0].candidateServersConsidered[0]", is("base")))
                    .andExpect(jsonPath("$.results[0].candidateServersConsidered[1]", is("weighted")))
                    .andExpect(jsonPath("$.results[0].scores.base", is(1.0)))
                    .andExpect(jsonPath("$.results[0].scores.weighted", is(4.0)))
                    .andExpect(jsonPath("$.results[0].reason", containsString("smooth weighted round-robin")))
                    .andExpect(jsonPath("$.error").doesNotExist());

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Weighted round-robin routing comparison must not construct CloudManager or call AWS paths.");
        }
    }

    @Test
    void explicitWeightedLeastConnectionsRequestUsesRoutingWeightWithoutCloudMutationPath() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(routingCompare("""
                            {
                              "strategies": ["WEIGHTED_LEAST_CONNECTIONS"],
                              "servers": [
                                {
                                  "serverId": "base",
                                  "healthy": true,
                                  "inFlightRequestCount": 5,
                                  "configuredCapacity": 100.0,
                                  "estimatedConcurrencyLimit": 100.0,
                                  "weight": 1.0,
                                  "averageLatencyMillis": 10.0,
                                  "p95LatencyMillis": 20.0,
                                  "p99LatencyMillis": 40.0,
                                  "recentErrorRate": 0.0,
                                  "queueDepth": 0
                                },
                                {
                                  "serverId": "weighted",
                                  "healthy": true,
                                  "inFlightRequestCount": 12,
                                  "configuredCapacity": 100.0,
                                  "estimatedConcurrencyLimit": 100.0,
                                  "weight": 4.0,
                                  "averageLatencyMillis": 10.0,
                                  "p95LatencyMillis": 20.0,
                                  "p99LatencyMillis": 40.0,
                                  "recentErrorRate": 0.0,
                                  "queueDepth": 0
                                }
                              ]
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestedStrategies[0]", is("WEIGHTED_LEAST_CONNECTIONS")))
                    .andExpect(jsonPath("$.candidateCount", is(2)))
                    .andExpect(jsonPath("$.results[0].strategyId", is("WEIGHTED_LEAST_CONNECTIONS")))
                    .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                    .andExpect(jsonPath("$.results[0].chosenServerId", is("weighted")))
                    .andExpect(jsonPath("$.results[0].candidateServersConsidered[0]", is("base")))
                    .andExpect(jsonPath("$.results[0].candidateServersConsidered[1]", is("weighted")))
                    .andExpect(jsonPath("$.results[0].scores.base", is(5.0)))
                    .andExpect(jsonPath("$.results[0].scores.weighted", is(3.0)))
                    .andExpect(jsonPath("$.results[0].reason", containsString("weighted least-connections")))
                    .andExpect(jsonPath("$.error").doesNotExist());

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Weighted least-connections routing comparison must not construct CloudManager or call AWS paths.");
        }
    }

    @Test
    void explicitRoundRobinRequestUsesRequestOrderWithoutCloudMutationPath() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(routingCompare("""
                            {
                              "strategies": ["ROUND_ROBIN"],
                              "servers": [
                                {
                                  "serverId": "green",
                                  "healthy": true,
                                  "inFlightRequestCount": 20,
                                  "averageLatencyMillis": 10.0,
                                  "p95LatencyMillis": 20.0,
                                  "p99LatencyMillis": 40.0,
                                  "recentErrorRate": 0.0
                                },
                                {
                                  "serverId": "blue",
                                  "healthy": true,
                                  "inFlightRequestCount": 5,
                                  "averageLatencyMillis": 10.0,
                                  "p95LatencyMillis": 20.0,
                                  "p99LatencyMillis": 40.0,
                                  "recentErrorRate": 0.0
                                }
                              ]
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestedStrategies[0]", is("ROUND_ROBIN")))
                    .andExpect(jsonPath("$.candidateCount", is(2)))
                    .andExpect(jsonPath("$.results[0].strategyId", is("ROUND_ROBIN")))
                    .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                    .andExpect(jsonPath("$.results[0].chosenServerId", is("green")))
                    .andExpect(jsonPath("$.results[0].candidateServersConsidered[0]", is("green")))
                    .andExpect(jsonPath("$.results[0].candidateServersConsidered[1]", is("blue")))
                    .andExpect(jsonPath("$.results[0].scores").isEmpty())
                    .andExpect(jsonPath("$.results[0].reason", containsString("round-robin position 1 of 2")))
                    .andExpect(jsonPath("$.error").doesNotExist());

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Round-robin routing comparison must not construct CloudManager or call AWS paths.");
        }
    }

    @Test
    void emptyServersReturnsStructuredBadRequest() throws Exception {
        mockMvc.perform(routingCompare("""
                        {
                          "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
                          "servers": []
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.path", is("/api/routing/compare")))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void decisionExplorerEndpointReturnsStructuredBadRequestWithEndpointPath() throws Exception {
        mockMvc.perform(routingDecisionExplorer("""
                        {
                          "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
                          "servers": []
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("validation_failed")))
                .andExpect(jsonPath("$.path", is("/api/routing/decision-explorer")))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void duplicateServerIdsReturnStructuredBadRequest() throws Exception {
        mockMvc.perform(routingCompare("""
                        {
                          "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
                          "servers": [
                            {
                              "serverId": "green",
                              "healthy": true,
                              "inFlightRequestCount": 1,
                              "averageLatencyMillis": 10.0,
                              "p95LatencyMillis": 20.0,
                              "p99LatencyMillis": 30.0,
                              "recentErrorRate": 0.0
                            },
                            {
                              "serverId": "green",
                              "healthy": true,
                              "inFlightRequestCount": 2,
                              "averageLatencyMillis": 11.0,
                              "p95LatencyMillis": 21.0,
                              "p99LatencyMillis": 31.0,
                              "recentErrorRate": 0.0
                            }
                          ]
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("serverId must be unique")))
                .andExpect(jsonPath("$.path", is("/api/routing/compare")));
    }

    @Test
    void unknownStrategyIdReturnsStructuredBadRequest() throws Exception {
        mockMvc.perform(routingCompare("""
                        {
                          "strategies": ["NOT_A_REAL_STRATEGY"],
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
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("Unsupported routing strategy")))
                .andExpect(jsonPath("$.path", is("/api/routing/compare")));
    }

    @Test
    void invalidRoutingWeightReturnsStructuredBadRequest() throws Exception {
        mockMvc.perform(routingCompare("""
                        {
                          "strategies": ["WEIGHTED_LEAST_LOAD"],
                          "servers": [
                            {
                              "serverId": "green",
                              "healthy": true,
                              "inFlightRequestCount": 1,
                              "weight": -1.0,
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
                .andExpect(jsonPath("$.message", containsString("weight")))
                .andExpect(jsonPath("$.path", is("/api/routing/compare")));
    }

    @Test
    void duplicateStrategyIdsReturnStructuredBadRequest() throws Exception {
        mockMvc.perform(routingCompare("""
                        {
                          "strategies": ["TAIL_LATENCY_POWER_OF_TWO", "tail-latency-power-of-two"],
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
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("duplicate")));
    }

    @Test
    void invalidMetricRangesReturnStructuredBadRequest() throws Exception {
        mockMvc.perform(routingCompare("""
                        {
                          "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
                          "servers": [
                            {
                              "serverId": "green",
                              "healthy": true,
                              "inFlightRequestCount": 1,
                              "averageLatencyMillis": 30.0,
                              "p95LatencyMillis": 20.0,
                              "p99LatencyMillis": 40.0,
                              "recentErrorRate": 0.0
                            }
                          ]
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("averageLatencyMillis")));
    }

    @Test
    void allUnhealthyCandidatesReturnSafeNoDecisionResult() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                     Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(routingCompare("""
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
                    .andExpect(jsonPath("$.results[0].strategyId", is("TAIL_LATENCY_POWER_OF_TWO")))
                    .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                    .andExpect(jsonPath("$.results[0].chosenServerId", nullValue()))
                    .andExpect(jsonPath("$.results[0].candidateServersConsidered").isEmpty())
                    .andExpect(jsonPath("$.results[0].reason", containsString("No healthy eligible servers")));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "All-unhealthy routing comparison must not construct CloudManager or call AWS paths.");
        }
    }

    @Test
    void unsupportedMediaTypeReturnsStructuredError() throws Exception {
        mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(VALID_REQUEST))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(415)))
                .andExpect(jsonPath("$.error", is("unsupported_media_type")))
                .andExpect(jsonPath("$.path", is("/api/routing/compare")));
    }

    @Test
    void wrongHttpMethodReturnsStructuredError() throws Exception {
        mockMvc.perform(put("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(405)))
                .andExpect(jsonPath("$.error", is("method_not_allowed")))
                .andExpect(jsonPath("$.path", is("/api/routing/compare")));
    }

    private static MockHttpServletRequestBuilder routingCompare(String requestBody) {
        return post("/api/routing/compare")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody);
    }

    private static MockHttpServletRequestBuilder routingDecisionExplorer(String requestBody) {
        return post("/api/routing/decision-explorer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody);
    }
}
