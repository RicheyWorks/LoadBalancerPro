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
