package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
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

    @Autowired
    private MockMvc mockMvc;

    @Test
    void endpointPayloadKeepsStableFieldPresenceAndAdditivePhaseTwoArrays() throws Exception {
        JsonNode payload = endpointPayload();

        assertEquals(List.of(
                "readOnly",
                "simulationOnly",
                "payloadObject",
                "contractVersion",
                "source",
                "decisionId",
                "decisionReadout",
                "selectedCandidate",
                "candidateSet",
                "candidateComparisons",
                "confidenceSummary",
                "routingDiagnostics",
                "routeTradeoffAnalysis",
                "shadowDecisionQualityEvaluation",
                "counterfactualAnalysis",
                "factorContributions",
                "factorDrilldowns",
                "policyGateReadouts",
                "decisionDiffReadouts",
                "evidencePacketReadouts",
                "agentStructuredOutput",
                "warnings",
                "unknowns",
                "notProvenBoundaries",
                "boundaryNote"), fieldNames(payload));
        assertTrue(payload.path("readOnly").asBoolean());
        assertTrue(payload.path("simulationOnly").asBoolean());
        assertEquals("DecisionExplorerPayloadV1", payload.path("payloadObject").asText());
        assertEquals("v1", payload.path("contractVersion").asText());
        assertEquals("green", payload.at("/selectedCandidate/candidateId").asText());
        assertEquals("green", payload.at("/candidateSet/0/candidateId").asText());
        assertEquals("green", payload.at("/candidateComparisons/0/candidateId").asText());
        assertEquals(1, payload.at("/candidateComparisons/0/displayOrder").asInt());
        assertEquals("SELECTED", payload.at("/candidateComparisons/0/comparisonStatus").asText());
        assertEquals("PARTIAL", payload.at("/confidenceSummary/status").asText());
        assertEquals("PARTIAL", payload.at("/confidenceSummary/evidenceQuality").asText());
        assertEquals("green", payload.at("/confidenceSummary/selectedCandidateId").asText());
        assertTrue(payload.at("/confidenceSummary/candidateConfidenceDetails").isArray());
        assertEquals("green", payload.at("/confidenceSummary/candidateConfidenceDetails/0/candidateId").asText());
        assertEquals("HEALTHY", payload.at("/confidenceSummary/candidateConfidenceDetails/0/healthEvidenceState")
                .asText());
        assertTrue(payload.at("/confidenceSummary/factorStatusDetails").isArray());
        assertTrue(payload.at("/confidenceSummary/factorStatusDetails").size() > 0);
        assertFalse(payload.at("/confidenceSummary/factorStatusDetails/0/factorStatus").isMissingNode());
        assertEquals("DecisionExplorerStatusExplanationV1",
                payload.at("/confidenceSummary/statusExplanation/explanationObject").asText());
        assertEquals("PARTIAL", payload.at("/confidenceSummary/statusExplanation/status").asText());
        assertFalse(payload.at("/confidenceSummary/statusExplanation/summaryText").asText().isBlank());
        assertEquals("DecisionExplorerRoutingDiagnosticsV1",
                payload.at("/routingDiagnostics/diagnosticsObject").asText());
        assertEquals("PARTIAL", payload.at("/routingDiagnostics/overallStatus").asText());
        assertEquals("green", payload.at("/routingDiagnostics/selectedCandidateId").asText());
        assertTrue(payload.at("/routingDiagnostics/evidenceDiagnostics").isArray());
        assertTrue(payload.at("/routingDiagnostics/evidenceDiagnostics").size() > 0);
        assertEquals("green", payload.at("/routingDiagnostics/selectedCandidateDiagnostic/candidateId").asText());
        assertTrue(payload.at("/routingDiagnostics/candidateDiagnostics").isArray());
        assertTrue(payload.at("/routingDiagnostics/candidateDiagnostics").size() > 0);
        assertTrue(payload.at("/routingDiagnostics/factorDiagnostics").isArray());
        assertTrue(payload.at("/routingDiagnostics/factorDiagnostics").size() > 0);
        assertTrue(payload.at("/routingDiagnostics/partialEvidenceReasons").isArray());
        assertTrue(payload.at("/routingDiagnostics/explanationText").asText()
                .contains("selected candidate green as PARTIAL"));
        assertTrue(payload.at("/routingDiagnostics/unknowns").isArray());
        assertEquals("DecisionExplorerRouteTradeoffAnalysisV1",
                payload.at("/routeTradeoffAnalysis/analysisObject").asText());
        assertEquals("PARTIAL", payload.at("/routeTradeoffAnalysis/overallStatus").asText());
        assertEquals("green", payload.at("/routeTradeoffAnalysis/selectedCandidateId").asText());
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                payload.at("/routeTradeoffAnalysis/fingerprintAlgorithm").asText());
        assertTrue(payload.at("/routeTradeoffAnalysis/diagnosticFingerprint").asText()
                .startsWith("route-tradeoff|v1|"));
        assertFalse(payload.at("/routeTradeoffAnalysis/reproducibilityKey").asText().isBlank());
        assertTrue(payload.at("/routeTradeoffAnalysis/explanationText").asText()
                .contains("selected candidate green is PARTIAL"));
        assertTrue(payload.at("/routeTradeoffAnalysis/explanationText").asText()
                .contains("replay execution unavailable"));
        assertTrue(payload.at("/routeTradeoffAnalysis/fingerprintInputs").isArray());
        assertTrue(payload.at("/routeTradeoffAnalysis/fingerprintInputs").size() > 0);
        assertTrue(payload.at("/routeTradeoffAnalysis/candidateTradeoffs").isArray());
        assertTrue(payload.at("/routeTradeoffAnalysis/candidateTradeoffs").size() > 0);
        assertTrue(payload.at("/routeTradeoffAnalysis/candidateScoringExplanations").isArray());
        assertTrue(payload.at("/routeTradeoffAnalysis/candidateScoringExplanations").size() > 0);
        assertTrue(payload.at("/routeTradeoffAnalysis/factorTradeoffDeltas").isArray());
        assertEquals("DecisionExplorerEvidenceSufficiencyV1",
                payload.at("/routeTradeoffAnalysis/evidenceSufficiency/diagnosticObject").asText());
        assertFalse(payload.at("/routeTradeoffAnalysis/evidenceSufficiency/sufficiencyLevel").asText().isBlank());
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                payload.at("/routeTradeoffAnalysis/evidenceSufficiency/fingerprintAlgorithm").asText());
        assertTrue(payload.at("/routeTradeoffAnalysis/evidenceSufficiency/diagnosticFingerprint").asText()
                .startsWith("evidence-sufficiency|v1|"));
        assertFalse(payload.at("/routeTradeoffAnalysis/evidenceSufficiency/reproducibilityKey")
                .asText().isBlank());
        assertTrue(payload.at("/routeTradeoffAnalysis/evidenceSufficiency/fingerprintInputs").isArray());
        assertEquals("DecisionExplorerReplayReadinessDiagnosticV1",
                payload.at("/routeTradeoffAnalysis/replayReadinessDiagnostic/diagnosticObject").asText());
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                payload.at("/routeTradeoffAnalysis/replayReadinessDiagnostic/fingerprintAlgorithm").asText());
        assertTrue(payload.at("/routeTradeoffAnalysis/replayReadinessDiagnostic/diagnosticFingerprint").asText()
                .startsWith("replay-readiness|v1|"));
        assertFalse(payload.at("/routeTradeoffAnalysis/replayReadinessDiagnostic/reproducibilityKey")
                .asText().isBlank());
        assertTrue(payload.at("/routeTradeoffAnalysis/replayReadinessDiagnostic/fingerprintInputs").isArray());
        assertFalse(payload.at("/routeTradeoffAnalysis/replayReadinessDiagnostic/replayExecutionAvailable")
                .asBoolean());
        assertFalse(payload.at("/routeTradeoffAnalysis/replayReadinessDiagnostic/replayStorageAvailable")
                .asBoolean());
        assertFalse(payload.at("/routeTradeoffAnalysis/replayReadinessDiagnostic/replayExportAvailable")
                .asBoolean());
        assertEquals("DecisionExplorerShadowDecisionQualityEvaluationV1",
                payload.at("/shadowDecisionQualityEvaluation/evaluationObject").asText());
        assertTrue(payload.at("/shadowDecisionQualityEvaluation/readOnly").asBoolean());
        assertTrue(payload.at("/shadowDecisionQualityEvaluation/simulationOnly").asBoolean());
        assertEquals("REVIEW_RECOMMENDED",
                payload.at("/shadowDecisionQualityEvaluation/qualityLabel").asText());
        assertEquals("MEDIUM", payload.at("/shadowDecisionQualityEvaluation/qualityBand").asText());
        assertEquals("green", payload.at("/shadowDecisionQualityEvaluation/selectedCandidateId").asText());
        assertEquals("PARTIAL", payload.at("/shadowDecisionQualityEvaluation/confidenceStatus").asText());
        assertEquals("PARTIAL", payload.at("/shadowDecisionQualityEvaluation/evidenceQuality").asText());
        assertEquals("READY", payload.at("/shadowDecisionQualityEvaluation/replayReadinessStatus").asText());
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                payload.at("/shadowDecisionQualityEvaluation/fingerprintAlgorithm").asText());
        assertTrue(payload.at("/shadowDecisionQualityEvaluation/diagnosticFingerprint").asText()
                .startsWith("shadow-decision-quality|v1|"));
        assertFalse(payload.at("/shadowDecisionQualityEvaluation/reproducibilityKey").asText().isBlank());
        assertTrue(payload.at("/shadowDecisionQualityEvaluation/fingerprintInputs").isArray());
        assertTrue(payload.at("/shadowDecisionQualityEvaluation/fingerprintInputs").size() > 0);
        assertTrue(payload.at("/shadowDecisionQualityEvaluation/explanationText").asText()
                .contains("Shadow decision-quality explanation is REVIEW_RECOMMENDED"));
        assertTrue(payload.at("/shadowDecisionQualityEvaluation/candidateOutcomeComparisons").isArray());
        assertEquals("green",
                payload.at("/shadowDecisionQualityEvaluation/candidateOutcomeComparisons/0/candidateId")
                        .asText());
        assertFalse(payload.at("/shadowDecisionQualityEvaluation/evidenceBasisSummary").asText().isBlank());
        assertFalse(payload.at("/shadowDecisionQualityEvaluation/selectedCandidateBasisSummary").asText().isBlank());
        assertEquals("DecisionExplorerShadowPolicySensitivityDiagnosticV1",
                payload.at("/shadowDecisionQualityEvaluation/policySensitivityDiagnostic/diagnosticObject")
                        .asText());
        assertEquals("DecisionExplorerShadowScenarioInputQualityV1",
                payload.at("/shadowDecisionQualityEvaluation/scenarioInputQuality/evaluationObject").asText());
        assertTrue(payload.at("/shadowDecisionQualityEvaluation/qualityReasons").isArray());
        assertTrue(payload.at("/shadowDecisionQualityEvaluation/sourceReferenceIds").isArray());
        assertEquals("DecisionExplorerCounterfactualAnalysisV1",
                payload.at("/counterfactualAnalysis/analysisObject").asText());
        assertTrue(payload.at("/counterfactualAnalysis/readOnly").asBoolean());
        assertTrue(payload.at("/counterfactualAnalysis/simulationOnly").asBoolean());
        assertTrue(payload.at("/counterfactualAnalysis/localOnly").asBoolean());
        assertEquals("SENSITIVE", payload.at("/counterfactualAnalysis/counterfactualLabel").asText());
        assertEquals("MEDIUM", payload.at("/counterfactualAnalysis/sensitivityBand").asText());
        assertEquals("green", payload.at("/counterfactualAnalysis/selectedCandidateId").asText());
        assertEquals("PARTIAL", payload.at("/counterfactualAnalysis/confidenceStatus").asText());
        assertEquals("REVIEW_RECOMMENDED",
                payload.at("/counterfactualAnalysis/decisionQualityLabel").asText());
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                payload.at("/counterfactualAnalysis/fingerprintAlgorithm").asText());
        assertTrue(payload.at("/counterfactualAnalysis/diagnosticFingerprint").asText()
                .startsWith("counterfactual-analysis|v1|"));
        assertFalse(payload.at("/counterfactualAnalysis/reproducibilityKey").asText().isBlank());
        assertTrue(payload.at("/counterfactualAnalysis/policyWeightScenarios").isArray());
        assertTrue(payload.at("/counterfactualAnalysis/policyWeightScenarios").size() > 0);
        assertTrue(payload.at("/counterfactualAnalysis/counterfactualCandidateOutcomes").isArray());
        assertTrue(payload.at("/counterfactualAnalysis/counterfactualCandidateOutcomes").size() > 0);
        assertTrue(payload.at("/counterfactualAnalysis/factorWeightDeltas").isArray());
        assertTrue(payload.at("/counterfactualAnalysis/summaryText").asText()
                .contains("no production routing, scoring, proxying"));
        assertTrue(payload.path("factorDrilldowns").isArray());
        assertTrue(payload.path("factorDrilldowns").size() > 0);
        assertTrue(payload.path("notProvenBoundaries").isArray());
        assertStringArrayContains(payload.path("notProvenBoundaries"), "no production readiness");
        assertStringArrayContains(payload.path("notProvenBoundaries"), "no storage proof");
        assertStringArrayContains(payload.path("notProvenBoundaries"), "no evidence-packet generation");
        assertNoUnsupportedClaims(payload);
    }

    @Test
    void legacyConstructorSerializesAdditiveFieldsAsPresentEmptyArrays() throws Exception {
        DecisionExplorerPayloadV1 payload = new DecisionExplorerPayloadV1(
                true,
                true,
                null,
                null,
                "/api/routing/compare",
                "decision-legacy",
                decisionReadout(),
                selectedCandidate(),
                List.of(selectedCandidate()),
                List.of(factorContribution()),
                List.of(policyGate()),
                List.of(),
                List.of(evidencePacketReadout()),
                agentStructuredOutput(),
                List.of(),
                List.of(),
                notProvenBoundaries(),
                "legacy constructor remains read-only and simulation-only");

        JsonNode json = OBJECT_MAPPER.valueToTree(payload);

        assertEquals("DecisionExplorerPayloadV1", json.path("payloadObject").asText());
        assertTrue(json.path("candidateComparisons").isArray());
        assertEquals(0, json.path("candidateComparisons").size());
        assertTrue(json.path("factorDrilldowns").isArray());
        assertEquals(0, json.path("factorDrilldowns").size());
        assertEquals("UNKNOWN", json.at("/confidenceSummary/status").asText());
        assertEquals(0, json.at("/confidenceSummary/candidateConfidenceDetails").size());
        assertEquals(0, json.at("/confidenceSummary/factorStatusDetails").size());
        assertEquals("UNKNOWN", json.at("/confidenceSummary/statusExplanation/status").asText());
        assertEquals("UNKNOWN", json.at("/routingDiagnostics/overallStatus").asText());
        assertEquals("DecisionExplorerRoutingDiagnosticsV1",
                json.at("/routingDiagnostics/diagnosticsObject").asText());
        assertTrue(json.at("/routingDiagnostics/evidenceDiagnostics").isArray());
        assertTrue(json.at("/routingDiagnostics/explanationText").asText()
                .contains("NO_CONFIDENCE_SUMMARY_RETURNED"));
        assertEquals("DecisionExplorerRouteTradeoffAnalysisV1",
                json.at("/routeTradeoffAnalysis/analysisObject").asText());
        assertEquals("UNKNOWN", json.at("/routeTradeoffAnalysis/overallStatus").asText());
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                json.at("/routeTradeoffAnalysis/fingerprintAlgorithm").asText());
        assertTrue(json.at("/routeTradeoffAnalysis/diagnosticFingerprint").asText()
                .startsWith("route-tradeoff|v1|"));
        assertFalse(json.at("/routeTradeoffAnalysis/reproducibilityKey").asText().isBlank());
        assertTrue(json.at("/routeTradeoffAnalysis/explanationText").asText()
                .contains("Route tradeoff explanation is UNKNOWN"));
        assertTrue(json.at("/routeTradeoffAnalysis/fingerprintInputs").isArray());
        assertTrue(json.at("/routeTradeoffAnalysis/candidateTradeoffs").isArray());
        assertEquals(0, json.at("/routeTradeoffAnalysis/candidateTradeoffs").size());
        assertEquals("INSUFFICIENT",
                json.at("/routeTradeoffAnalysis/evidenceSufficiency/sufficiencyLevel").asText());
        assertTrue(json.at("/routeTradeoffAnalysis/evidenceSufficiency/diagnosticFingerprint").asText()
                .startsWith("evidence-sufficiency|v1|"));
        assertTrue(json.at("/routeTradeoffAnalysis/replayReadinessDiagnostic/diagnosticFingerprint").asText()
                .startsWith("replay-readiness|v1|"));
        assertFalse(json.at("/routeTradeoffAnalysis/replayReadinessDiagnostic/replayExecutionAvailable")
                .asBoolean());
        assertEquals("DecisionExplorerShadowDecisionQualityEvaluationV1",
                json.at("/shadowDecisionQualityEvaluation/evaluationObject").asText());
        assertEquals("UNKNOWN", json.at("/shadowDecisionQualityEvaluation/qualityLabel").asText());
        assertEquals("UNKNOWN", json.at("/shadowDecisionQualityEvaluation/qualityBand").asText());
        assertTrue(json.at("/shadowDecisionQualityEvaluation/diagnosticFingerprint").asText()
                .startsWith("shadow-decision-quality|v1|"));
        assertFalse(json.at("/shadowDecisionQualityEvaluation/reproducibilityKey").asText().isBlank());
        assertTrue(json.at("/shadowDecisionQualityEvaluation/fingerprintInputs").isArray());
        assertTrue(json.at("/shadowDecisionQualityEvaluation/explanationText").asText()
                .contains("Shadow decision-quality explanation is UNKNOWN"));
        assertTrue(json.at("/shadowDecisionQualityEvaluation/candidateOutcomeComparisons").isArray());
        assertEquals(0, json.at("/shadowDecisionQualityEvaluation/candidateOutcomeComparisons").size());
        assertEquals("UNKNOWN",
                json.at("/shadowDecisionQualityEvaluation/policySensitivityDiagnostic/sensitivityLevel")
                        .asText());
        assertEquals("UNKNOWN",
                json.at("/shadowDecisionQualityEvaluation/scenarioInputQuality/inputQualityLabel").asText());
        assertEquals("DecisionExplorerCounterfactualAnalysisV1",
                json.at("/counterfactualAnalysis/analysisObject").asText());
        assertEquals("UNKNOWN", json.at("/counterfactualAnalysis/counterfactualLabel").asText());
        assertEquals("UNKNOWN", json.at("/counterfactualAnalysis/sensitivityBand").asText());
        assertTrue(json.at("/counterfactualAnalysis/policyWeightScenarios").isArray());
        assertEquals(0, json.at("/counterfactualAnalysis/policyWeightScenarios").size());
        assertTrue(json.at("/counterfactualAnalysis/counterfactualCandidateOutcomes").isArray());
        assertEquals(0, json.at("/counterfactualAnalysis/counterfactualCandidateOutcomes").size());
        assertTrue(json.at("/counterfactualAnalysis/factorWeightDeltas").isArray());
        assertEquals(0, json.at("/counterfactualAnalysis/factorWeightDeltas").size());
        assertTrue(json.at("/counterfactualAnalysis/diagnosticFingerprint").asText()
                .startsWith("counterfactual-analysis|v1|"));
        assertTrue(json.path("factorContributions").isArray());
        assertEquals(1, json.path("factorContributions").size());
        assertNoUnsupportedClaims(json);
    }

    @Test
    void unknownEvidenceSerializationKeepsArraysPresentWithoutInventingSignals() {
        DecisionExplorerPayloadV1 payload = new DecisionExplorerPayloadService().buildPayloads(null).get(0);
        JsonNode json = OBJECT_MAPPER.valueToTree(payload);

        assertEquals("routing-compare/unknown/unknown", json.path("decisionId").asText());
        assertEquals("UNKNOWN", json.at("/selectedCandidate/candidateId").asText());
        assertTrue(json.path("candidateSet").isArray());
        assertTrue(json.path("candidateComparisons").isArray());
        assertEquals("UNKNOWN", json.at("/confidenceSummary/status").asText());
        assertEquals(0, json.at("/confidenceSummary/candidateConfidenceDetails").size());
        assertEquals(0, json.at("/confidenceSummary/factorStatusDetails").size());
        assertEquals("UNKNOWN", json.at("/confidenceSummary/statusExplanation/status").asText());
        assertEquals("UNKNOWN", json.at("/routingDiagnostics/overallStatus").asText());
        assertEquals("UNKNOWN", json.at("/routingDiagnostics/evidenceQuality").asText());
        assertEquals(1, json.at("/routingDiagnostics/unknownEvidenceCount").asInt());
        assertTrue(json.at("/routingDiagnostics/explanationText").asText()
                .contains("NO_CONFIDENCE_SUMMARY_RETURNED"));
        assertTrue(json.at("/routingDiagnostics/unknowns").isArray());
        assertEquals("UNKNOWN", json.at("/routeTradeoffAnalysis/overallStatus").asText());
        assertEquals("UNKNOWN", json.at("/routeTradeoffAnalysis/tradeoffCategory").asText());
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                json.at("/routeTradeoffAnalysis/fingerprintAlgorithm").asText());
        assertTrue(json.at("/routeTradeoffAnalysis/diagnosticFingerprint").asText()
                .startsWith("route-tradeoff|v1|"));
        assertTrue(json.at("/routeTradeoffAnalysis/explanationText").asText()
                .contains("Route tradeoff explanation is UNKNOWN"));
        assertEquals("INSUFFICIENT",
                json.at("/routeTradeoffAnalysis/evidenceSufficiency/sufficiencyLevel").asText());
        assertEquals("UNKNOWN",
                json.at("/routeTradeoffAnalysis/replayReadinessDiagnostic/readinessStatus").asText());
        assertTrue(json.at("/routeTradeoffAnalysis/replayReadinessDiagnostic/diagnosticFingerprint").asText()
                .startsWith("replay-readiness|v1|"));
        assertFalse(json.at("/routeTradeoffAnalysis/replayReadinessDiagnostic/replayExecutionAvailable")
                .asBoolean());
        assertEquals("UNKNOWN", json.at("/shadowDecisionQualityEvaluation/qualityLabel").asText());
        assertEquals("UNKNOWN", json.at("/shadowDecisionQualityEvaluation/replayReadinessStatus").asText());
        assertTrue(json.at("/shadowDecisionQualityEvaluation/diagnosticFingerprint").asText()
                .startsWith("shadow-decision-quality|v1|"));
        assertTrue(json.at("/shadowDecisionQualityEvaluation/explanationText").asText()
                .contains("computed Decision Explorer evidence was unavailable"));
        assertTrue(json.at("/shadowDecisionQualityEvaluation/unknowns").isArray());
        assertStringArrayContains(json.at("/shadowDecisionQualityEvaluation/unknowns"),
                "shadow decision-quality input evidence was unavailable");
        assertEquals("UNKNOWN", json.at("/counterfactualAnalysis/counterfactualLabel").asText());
        assertEquals("INSUFFICIENT", json.at("/counterfactualAnalysis/evidenceSufficiencyLevel").asText());
        assertTrue(json.at("/counterfactualAnalysis/unknowns").isArray());
        assertStringArrayContains(json.at("/counterfactualAnalysis/unknowns"),
                "counterfactual analysis input evidence was unavailable");
        assertTrue(json.path("factorContributions").isArray());
        assertTrue(json.path("factorDrilldowns").isArray());
        assertEquals(0, json.path("candidateSet").size());
        assertEquals(0, json.path("candidateComparisons").size());
        assertEquals(0, json.path("factorContributions").size());
        assertEquals(0, json.path("factorDrilldowns").size());
        assertTrue(json.path("warnings").size() > 0);
        assertTrue(json.path("unknowns").size() > 0);
        assertStringArrayContains(json.path("unknowns"), "routing comparison result evidence was unavailable");
        assertStringArrayContains(json.path("notProvenBoundaries"), "no replay/export proof");
        assertNoUnsupportedClaims(json);
    }

    private JsonNode endpointPayload() throws Exception {
        String body = mockMvc.perform(post("/api/routing/decision-explorer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = OBJECT_MAPPER.readTree(body);
        assertTrue(root.isArray());
        assertEquals(1, root.size());
        return root.get(0);
    }

    private static void assertStringArrayContains(JsonNode array, String expected) {
        assertTrue(array.isArray(), "expected an array containing " + expected);
        for (JsonNode value : array) {
            if (expected.equals(value.asText())) {
                return;
            }
        }
        throw new AssertionError("Expected array to contain " + expected + " but was " + array);
    }

    private static void assertNoUnsupportedClaims(JsonNode payload) {
        String normalized = payload.toString().toLowerCase(Locale.ROOT);
        for (String forbidden : List.of(
                "production readiness proven",
                "certification complete",
                "live-cloud validation complete",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "replay export is implemented",
                "autonomous production action enabled")) {
            assertFalse(normalized.contains(forbidden), "Decision Explorer API must not overclaim " + forbidden);
        }
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static DecisionReadoutV1 decisionReadout() {
        return new DecisionReadoutV1(
                "decision-legacy",
                "AVAILABLE",
                "candidate-a",
                "TAIL_LATENCY_POWER_OF_TWO",
                "Legacy constructor compatibility keeps Phase 1 fields stable.",
                List.of("VISIBLE_SIGNAL_MATCH"),
                List.of("routing-comparison-result"),
                "read-only simulation-only decision readout");
    }

    private static CandidateReadoutV1 selectedCandidate() {
        return new CandidateReadoutV1(
                "candidate-a",
                "candidate-a",
                true,
                "SELECTED",
                10.0,
                List.of("healthState=healthy"),
                List.of("hidden routing internals"),
                List.of("SELECTED_CANDIDATE"),
                List.of("boundary-read-only"),
                List.of("decision-vector:candidate-a"),
                "candidate readout is display-only");
    }

    private static FactorContributionV1 factorContribution() {
        return new FactorContributionV1(
                "healthState",
                "candidate-a",
                "SUPPORTS_SELECTION",
                -8.0,
                "EXACT_FROM_RETURNED_EVIDENCE",
                "Health contribution is copied from visible returned evidence.",
                List.of("decision-vector:candidate-a"),
                "factor readout is not benchmark/load/stress evidence");
    }

    private static PolicyGateReadoutV1 policyGate() {
        return new PolicyGateReadoutV1(
                "boundary-read-only",
                "Read-only boundary",
                "AVAILABLE",
                "PASS",
                "Decision Explorer reshapes already-built compare evidence.",
                List.of("docs/agent/DECISION_EXPLORER_PHASE1_ARCHITECTURE_SCOPE.md"),
                "policy gate visualization is read-only");
    }

    private static EvidencePacketReadoutV1 evidencePacketReadout() {
        return new EvidencePacketReadoutV1(
                "future-evidence-packet",
                "NOT_IMPLEMENTED",
                "docs/agent/DECISION_EXPLORER_EVIDENCE_LANE.md",
                "PLANNED",
                List.of("no evidence packet generation in this API contract"),
                "evidence packet readout is a reference only");
    }

    private static AgentStructuredOutputV1 agentStructuredOutput() {
        return new AgentStructuredOutputV1(
                "AgentStructuredOutputV1",
                "v1",
                List.of("payloadObject", "decisionReadout", "candidateSet", "notProvenBoundaries"),
                List.of("use stable field names", "do not infer hidden internals from null values"),
                List.of("Which candidate was selected?", "Which boundaries remain not proven?"),
                List.of("no autonomous production action", "no live mutation", "no hidden side effects"),
                notProvenBoundaries(),
                "agent output is for structured understanding only");
    }

    private static List<String> notProvenBoundaries() {
        return List.of(
                "no production readiness",
                "no production certification",
                "no live-cloud validation",
                "no real-tenant validation",
                "no benchmark/load/stress proof",
                "no throughput/p95/p99 proof",
                "no replay/export proof",
                "no storage proof",
                "no evidence-packet generation",
                "no autonomous production action");
    }
}
