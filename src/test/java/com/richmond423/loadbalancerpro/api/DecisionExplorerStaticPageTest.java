package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

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
class DecisionExplorerStaticPageTest {
    private static final Path DECISION_EXPLORER_PAGE =
            Path.of("src/main/resources/static/decision-explorer.html");
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

    @Autowired
    private MockMvc mockMvc;

    @Test
    void decisionExplorerPageExistsAndIsServed() throws Exception {
        assertTrue(Files.exists(DECISION_EXPLORER_PAGE), "Decision Explorer page should be source-controlled");

        mockMvc.perform(get("/decision-explorer.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Decision Explorer")))
                .andExpect(content().string(containsString("data-action=\"run-decision-explorer\"")))
                .andExpect(content().string(containsString("/api/routing/decision-explorer")));
    }

    @Test
    void decisionExplorerPageContainsFirstPassReviewerPanelsAndControls() throws Exception {
        String page = readPage();

        for (String expected : List.of(
                "Decision Summary",
                "Routing Intelligence Status",
                "Routing Diagnostics",
                "Route Tradeoff Intelligence",
                "Evidence Sufficiency",
                "Replay Readiness",
                "Shadow Decision Quality",
                "Counterfactual Analysis",
                "Selected Candidate",
                "Candidate Set",
                "Candidate Comparison",
                "Candidate Confidence",
                "Candidate Diagnostics",
                "Route Candidate Tradeoffs",
                "Candidate Tradeoff Scoring",
                "Shadow Candidate Outcomes",
                "Counterfactual Policy Scenarios",
                "Counterfactual Candidate Outcomes",
                "Counterfactual Factor Weight Deltas",
                "Policy Sensitivity",
                "Scenario Input Quality",
                "Evidence Diagnostics",
                "Factor Contributions",
                "Factor Drill-Down",
                "Factor Status",
                "Factor Diagnostics",
                "Factor Tradeoff Deltas",
                "Degradation Reasons",
                "Partial Evidence Reasons",
                "Unknown Evidence Reasons",
                "Policy Gates",
                "Decision Diffs",
                "Evidence Packet Readouts",
                "Agent Structured Output",
                "Warnings",
                "Unknowns",
                "Not-Proven Boundaries",
                "Raw Payload",
                "Reviewer Navigation",
                "Run Decision Explorer",
                "Scenario Catalog",
                "Load Scenario Catalog",
                "Scenario selector",
                "Category filter",
                "Evidence filter",
                "Reviewer Explanation Badges",
                "Selected route",
                "Confidence status",
                "Diagnostics status",
                "Tradeoff status",
                "Evidence sufficiency",
                "Replay readiness",
                "Decision quality",
                "Counterfactual sensitivity",
                "Degraded signals",
                "Warning",
                "Unknown",
                "Partial evidence",
                "Deterministic evidence",
                "Not-proven boundaries",
                "Copy Summary",
                "DecisionExplorerPayloadV1",
                "read-only",
                "simulation-only",
                "X-API-Key",
                "same-origin",
                "page memory only")) {
            assertTrue(page.contains(expected), "Decision Explorer page should contain " + expected);
        }
    }

    @Test
    void decisionExplorerPageUsesSameOriginApiAndNoPersistentBrowserStorage() throws Exception {
        String page = readPage();

        assertTrue(page.contains("const DECISION_EXPLORER_ENDPOINT = \"/api/routing/decision-explorer\""));
        assertTrue(page.contains("const DECISION_EXPLORER_SCENARIOS_ENDPOINT = "
                + "\"/api/routing/decision-explorer/scenarios\""));
        assertTrue(page.contains("fetch(DECISION_EXPLORER_ENDPOINT"));
        assertTrue(page.contains("fetch(DECISION_EXPLORER_SCENARIOS_ENDPOINT"));
        assertTrue(page.contains("data-action=\"load-decision-explorer-scenarios\""));
        assertTrue(page.contains("scenario-category-filter"));
        assertTrue(page.contains("scenario-evidence-filter"));
        assertTrue(page.contains("applyScenarioFilters"));
        assertTrue(page.contains("renderScenarioCatalog"));
        assertTrue(page.contains("candidate-comparisons"));
        assertTrue(page.contains("factor-drilldowns"));
        assertTrue(page.contains("confidence-summary"));
        assertTrue(page.contains("routing-diagnostics"));
        assertTrue(page.contains("route-tradeoff-summary"));
        assertTrue(page.contains("evidence-sufficiency"));
        assertTrue(page.contains("replay-readiness"));
        assertTrue(page.contains("shadow-decision-quality"));
        assertTrue(page.contains("counterfactual-analysis"));
        assertTrue(page.contains("shadow-candidate-outcomes"));
        assertTrue(page.contains("counterfactual-policy-scenarios"));
        assertTrue(page.contains("counterfactual-candidate-outcomes"));
        assertTrue(page.contains("counterfactual-factor-weight-deltas"));
        assertTrue(page.contains("shadow-policy-sensitivity"));
        assertTrue(page.contains("shadow-scenario-input-quality"));
        assertTrue(page.contains("candidate-confidence"));
        assertTrue(page.contains("candidate-diagnostics"));
        assertTrue(page.contains("route-candidate-tradeoffs"));
        assertTrue(page.contains("candidate-tradeoff-scoring"));
        assertTrue(page.contains("evidence-diagnostics"));
        assertTrue(page.contains("factor-status"));
        assertTrue(page.contains("factor-diagnostics"));
        assertTrue(page.contains("factor-tradeoff-deltas"));
        assertTrue(page.contains("degradation-reasons"));
        assertTrue(page.contains("partial-evidence-reasons"));
        assertTrue(page.contains("unknown-evidence-reasons"));
        assertTrue(page.contains("first.confidenceSummary"));
        assertTrue(page.contains("first.routingDiagnostics"));
        assertTrue(page.contains("first.routeTradeoffAnalysis"));
        assertTrue(page.contains("first.shadowDecisionQualityEvaluation"));
        assertTrue(page.contains("first.counterfactualAnalysis"));
        assertTrue(page.contains("renderConfidenceSummary"));
        assertTrue(page.contains("renderRoutingDiagnosticsSummary"));
        assertTrue(page.contains("renderRouteTradeoffSummary"));
        assertTrue(page.contains("renderEvidenceSufficiencySummary"));
        assertTrue(page.contains("renderReplayReadinessSummary"));
        assertTrue(page.contains("renderShadowDecisionQualitySummary"));
        assertTrue(page.contains("renderCounterfactualAnalysisSummary"));
        assertTrue(page.contains("renderShadowCandidateOutcomeTable"));
        assertTrue(page.contains("renderCounterfactualPolicyScenarioTable"));
        assertTrue(page.contains("renderCounterfactualCandidateOutcomeTable"));
        assertTrue(page.contains("renderCounterfactualFactorWeightDeltaTable"));
        assertTrue(page.contains("renderShadowPolicySensitivitySummary"));
        assertTrue(page.contains("renderShadowScenarioInputQualitySummary"));
        assertTrue(page.contains("renderCandidateConfidenceTable"));
        assertTrue(page.contains("renderCandidateDiagnosticsTable"));
        assertTrue(page.contains("renderRouteTradeoffTable"));
        assertTrue(page.contains("renderCandidateTradeoffScoringTable"));
        assertTrue(page.contains("renderEvidenceDiagnosticsTable"));
        assertTrue(page.contains("renderFactorStatusTable"));
        assertTrue(page.contains("renderFactorDiagnosticsTable"));
        assertTrue(page.contains("renderFactorTradeoffDeltaTable"));
        assertTrue(page.contains("statusExplanation"));
        assertTrue(page.contains("routingDiagnostics"));
        assertTrue(page.contains("routeTradeoffAnalysis"));
        assertTrue(page.contains("fingerprintAlgorithm"));
        assertTrue(page.contains("diagnosticFingerprint"));
        assertTrue(page.contains("reproducibilityKey"));
        assertTrue(page.contains("analysis.explanationText"));
        assertTrue(page.contains("routeTradeoffExplanation"));
        assertTrue(page.contains("routeTradeoffFingerprint"));
        assertTrue(page.contains("replayReadinessFingerprint"));
        assertTrue(page.contains("shadowDecisionQuality"));
        assertTrue(page.contains("shadowDecisionQualityExplanation"));
        assertTrue(page.contains("shadowDecisionQualityReasons"));
        assertTrue(page.contains("shadowDecisionQualityFingerprint"));
        assertTrue(page.contains("shadowDecisionQualityReproducibilityKey"));
        assertTrue(page.contains("counterfactualAnalysis"));
        assertTrue(page.contains("counterfactualSummary"));
        assertTrue(page.contains("counterfactualReasons"));
        assertTrue(page.contains("counterfactualFingerprint"));
        assertTrue(page.contains("counterfactualReproducibilityKey"));
        assertTrue(page.contains("policySensitivity"));
        assertTrue(page.contains("scenarioInputQuality"));
        assertTrue(page.contains("diagnostics.explanationText"));
        assertTrue(page.contains("degradationReasons"));
        assertTrue(page.contains("partialEvidenceReasons"));
        assertTrue(page.contains("unknownEvidenceReasons"));
        assertTrue(page.contains("confidenceStatusType"));
        assertTrue(page.contains("sufficiencyStatusType"));
        assertTrue(page.contains("replayReadinessStatusType"));
        assertTrue(page.contains("shadowQualityStatusType"));
        assertTrue(page.contains("counterfactualStatusType"));
        assertTrue(page.contains("policySensitivityStatusType"));
        assertTrue(page.contains("scenarioInputStatusType"));
        assertTrue(page.contains("Candidate confidence rows"));
        assertTrue(page.contains("Factor status rows"));
        assertTrue(page.contains("Candidate outcomes"));
        assertTrue(page.contains("Policy sensitivity"));
        assertTrue(page.contains("Scenario input quality"));
        assertTrue(page.contains("closestAlternativeText"));
        assertTrue(page.contains("readinessScoreText"));
        assertTrue(page.contains("Caution notes"));
        assertTrue(page.contains("renderCandidateComparisonTable"));
        assertTrue(page.contains("renderFactorDrilldownTable"));
        assertTrue(page.contains("reviewer-badges"));
        assertTrue(page.contains("renderReviewerBadges"));
        assertTrue(page.contains("reviewerBadgeDefinitions"));
        assertTrue(page.contains("collectReviewerWarnings"));
        assertTrue(page.contains("collectReviewerUnknowns"));
        assertTrue(page.contains("hasPartialEvidence"));
        assertTrue(page.contains("singular.endsWith(\"y\")"));
        assertTrue(page.contains("mergeLists"));
        assertTrue(page.contains("headers[\"X-API-Key\"] = key"));
        assertTrue(page.contains("textContent"));
        assertTrue(page.contains("createElement"));
        assertFalse(page.contains("window.localStorage"));
        assertFalse(page.contains("localStorage."));
        assertFalse(page.contains("window.sessionStorage"));
        assertFalse(page.contains("sessionStorage."));
        assertFalse(page.contains("innerHTML"));
        assertFalse(page.contains("document.write"));
        assertFalse(page.contains("https://"));
        assertFalse(page.contains("http://"));
    }

    @Test
    void decisionExplorerScenarioCatalogEndpointReturnsReadOnlySimulationOnlyMetadata() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(get("/api/routing/decision-explorer/scenarios"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.readOnly", is(true)))
                    .andExpect(jsonPath("$.simulationOnly", is(true)))
                    .andExpect(jsonPath("$.payloadObject", is("DecisionExplorerScenarioCatalogV1")))
                    .andExpect(jsonPath("$.scenarios[0].scenarioObject", is("DecisionExplorerScenarioV1")))
                    .andExpect(jsonPath("$.scenarios[0].scenarioId", is("normal-balanced-load")))
                    .andExpect(jsonPath("$.scenarios[0].scenarioCategory", is("HEALTHY_BASELINE")))
                    .andExpect(jsonPath("$.scenarios[0].evidenceStatus", is("AVAILABLE")))
                    .andExpect(jsonPath("$.notProvenBoundaries", hasItem("no production readiness")));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Scenario catalog page-backed endpoint call must not construct CloudManager.");
        }
    }

    @Test
    void decisionExplorerPageAndEndpointReturnReadOnlySimulationOnlyPayload() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(post("/api/routing/decision-explorer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REQUEST))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].readOnly", is(true)))
                    .andExpect(jsonPath("$[0].simulationOnly", is(true)))
                    .andExpect(jsonPath("$[0].payloadObject", is("DecisionExplorerPayloadV1")))
                    .andExpect(jsonPath("$[0].decisionReadout.selectedCandidateId", is("green")))
                    .andExpect(jsonPath("$[0].selectedCandidate.candidateId", is("green")))
                    .andExpect(jsonPath("$[0].candidateSet[0].selected", is(true)))
                    .andExpect(jsonPath("$[0].confidenceSummary.summaryObject",
                            is("DecisionExplorerConfidenceSummaryV1")))
                    .andExpect(jsonPath("$[0].confidenceSummary.status", is("PARTIAL")))
                    .andExpect(jsonPath("$[0].confidenceSummary.statusExplanation.explanationObject",
                            is("DecisionExplorerStatusExplanationV1")))
                    .andExpect(jsonPath("$[0].confidenceSummary.statusExplanation.status", is("PARTIAL")))
                    .andExpect(jsonPath("$[0].confidenceSummary.candidateConfidenceDetails[0].candidateId",
                            is("green")))
                    .andExpect(jsonPath("$[0].confidenceSummary.factorStatusDetails[0].factorName").exists())
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
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.analysisObject",
                            is("DecisionExplorerRouteTradeoffAnalysisV1")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.overallStatus", is("PARTIAL")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.selectedCandidateId", is("green")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.fingerprintAlgorithm",
                            is(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM)))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.diagnosticFingerprint",
                            containsString("route-tradeoff|v1|")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.reproducibilityKey").exists())
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.explanationText",
                            containsString("selected candidate green is PARTIAL")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.fingerprintInputs[0]").exists())
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.candidateTradeoffs[0].candidateId",
                            is("green")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.candidateScoringExplanations[0].candidateId",
                            is("green")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.factorTradeoffDeltas[0].factorName").exists())
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.evidenceSufficiency.diagnosticObject",
                            is("DecisionExplorerEvidenceSufficiencyV1")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.evidenceSufficiency.readinessScore").exists())
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.evidenceSufficiency.diagnosticFingerprint",
                            containsString("evidence-sufficiency|v1|")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.replayReadinessDiagnostic.diagnosticObject",
                            is("DecisionExplorerReplayReadinessDiagnosticV1")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.replayReadinessDiagnostic.diagnosticFingerprint",
                            containsString("replay-readiness|v1|")))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.replayReadinessDiagnostic.replayExecutionAvailable",
                            is(false)))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.replayReadinessDiagnostic.replayStorageAvailable",
                            is(false)))
                    .andExpect(jsonPath("$[0].routeTradeoffAnalysis.replayReadinessDiagnostic.replayExportAvailable",
                            is(false)))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.evaluationObject",
                            is("DecisionExplorerShadowDecisionQualityEvaluationV1")))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.qualityLabel",
                            is("REVIEW_RECOMMENDED")))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.fingerprintAlgorithm",
                            is(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM)))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.diagnosticFingerprint",
                            containsString("shadow-decision-quality|v1|")))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.reproducibilityKey").exists())
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.fingerprintInputs[0]").exists())
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.explanationText",
                            containsString("Shadow decision-quality explanation is REVIEW_RECOMMENDED")))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.candidateOutcomeComparisons[0].candidateId",
                            is("green")))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.policySensitivityDiagnostic.diagnosticObject",
                            is("DecisionExplorerShadowPolicySensitivityDiagnosticV1")))
                    .andExpect(jsonPath("$[0].shadowDecisionQualityEvaluation.scenarioInputQuality.evaluationObject",
                            is("DecisionExplorerShadowScenarioInputQualityV1")))
                    .andExpect(jsonPath("$[0].counterfactualAnalysis.analysisObject",
                            is("DecisionExplorerCounterfactualAnalysisV1")))
                    .andExpect(jsonPath("$[0].counterfactualAnalysis.readOnly", is(true)))
                    .andExpect(jsonPath("$[0].counterfactualAnalysis.localOnly", is(true)))
                    .andExpect(jsonPath("$[0].counterfactualAnalysis.counterfactualLabel").exists())
                    .andExpect(jsonPath("$[0].counterfactualAnalysis.sensitivityBand").exists())
                    .andExpect(jsonPath("$[0].counterfactualAnalysis.policyWeightScenarios[0].scenarioObject",
                            is("DecisionExplorerCounterfactualPolicyWeightScenarioV1")))
                    .andExpect(jsonPath("$[0].counterfactualAnalysis.counterfactualCandidateOutcomes[0].outcomeObject",
                            is("DecisionExplorerCounterfactualCandidateOutcomeV1")))
                    .andExpect(jsonPath("$[0].counterfactualAnalysis.factorWeightDeltas[0].deltaObject",
                            is("DecisionExplorerCounterfactualFactorWeightDeltaV1")))
                    .andExpect(jsonPath("$[0].counterfactualAnalysis.diagnosticFingerprint",
                            containsString("counterfactual-analysis|v1|")))
                    .andExpect(jsonPath("$[0].counterfactualAnalysis.reproducibilityKey",
                            containsString("counterfactual:v1:")))
                    .andExpect(jsonPath("$[0].policyGateReadouts[0].gateId", is("boundary-read-only")))
                    .andExpect(jsonPath("$[0].notProvenBoundaries", hasItem("no production readiness")))
                    .andExpect(jsonPath("$[0].notProvenBoundaries", hasItem("no storage proof")))
                    .andExpect(jsonPath("$[0].notProvenBoundaries", not(hasItem(
                            "no Decision Explorer endpoint, UI, storage, export, replay execution, or evidence-packet generation"))));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Decision Explorer page-backed endpoint call must not construct CloudManager.");
        }
    }

    @Test
    void decisionExplorerPagePreservesSafetyAndNotProvenBoundaries() throws Exception {
        String normalized = readPage().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        for (String expected : List.of(
                "does not shift traffic",
                "mutate routing",
                "call cloud or tenant systems",
                "persist storage",
                "execute replay",
                "generate evidence packets",
                "export files",
                "prove production readiness")) {
            assertTrue(normalized.contains(expected), "Decision Explorer page should preserve boundary " + expected);
        }

        for (String forbidden : List.of(
                "production readiness is proven",
                "production certified",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "autonomous production action enabled",
                "traffic shifting enabled",
                "evidence packet generated")) {
            assertFalse(normalized.contains(forbidden), "Decision Explorer page must not overclaim " + forbidden);
        }
    }

    private static String readPage() throws Exception {
        return Files.readString(DECISION_EXPLORER_PAGE, StandardCharsets.UTF_8);
    }
}
