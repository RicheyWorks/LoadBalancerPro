package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
class RoutingDecisionVectorReadOnlyExposureTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path COMPARE_FIXTURE = Path.of("src/test/resources/routing-demo/compare-strategies-sample.json");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void compareResponseExposesDecisionVectorAdditivelyWithoutBreakingExistingFields() throws Exception {
        JsonNode response = postCompare(Files.readString(COMPARE_FIXTURE, StandardCharsets.UTF_8));

        assertEquals("TAIL_LATENCY_POWER_OF_TWO", response.at("/requestedStrategies/0").asText());
        assertEquals(3, response.path("candidateCount").asInt());
        JsonNode result = response.at("/results/0");
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", result.path("strategyId").asText());
        assertEquals("SUCCESS", result.path("status").asText());
        assertEquals("edge-alpha", result.path("chosenServerId").asText());
        assertFalse(result.path("reason").asText().isBlank());
        assertEquals("edge-alpha", result.at("/candidateServersConsidered/0").asText());
        assertTrue(result.path("scores").has("edge-alpha"));

        JsonNode vector = result.path("decisionVector");
        assertTrue(vector.path("readOnly").asBoolean());
        assertEquals("/api/routing/compare", vector.path("localLabResponsePath").asText());
        assertEquals("not exposed by this read-only local lab response",
                vector.path("decisionIdOrLabRunId").asText());
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", vector.path("selectedStrategy").asText());
        assertEquals("edge-alpha", vector.path("selectedBackend").asText());
        assertEquals(3, vector.path("candidateCount").asInt());
        assertEquals(3, vector.path("candidateSummaries").size());
        assertEquals("edge-alpha", vector.at("/selectedCandidateVector/candidateId").asText());
        assertTrue(vector.at("/selectedCandidateVector/selected").asBoolean());
        assertEquals(2, vector.path("nonSelectedCandidateVectors").size());

        JsonNode dominant = result.path("dominantFactorAnalysis");
        assertTrue(dominant.path("readOnly").asBoolean());
        assertEquals("AVAILABLE", dominant.path("status").asText());
        assertEquals("edge-alpha", dominant.at("/selectedDecisionAnalysis/candidateId").asText());
        assertEquals(3, dominant.path("candidateAnalyses").size());
        assertTrue(dominant.at("/selectedDecisionAnalysis/largestPenaltyContributor/factorName").isTextual());
        assertTrue(dominant.path("boundaryNote").asText().contains("does not change routing behavior"));

        JsonNode delta = result.path("decisionDeltaAnalysis");
        assertTrue(delta.path("readOnly").asBoolean());
        assertEquals("PARTIAL", delta.path("status").asText());
        assertEquals("edge-alpha", delta.at("/comparison/selectedCandidateId").asText());
        assertEquals("edge-beta", delta.at("/comparison/closestAlternativeCandidateId").asText());
        assertTrue(delta.at("/comparison/finalScoreGap").isNumber());
        assertTrue(delta.at("/largestAbsoluteFactorDelta/factorName").isTextual());
        assertTrue(delta.path("explanation").asText().contains("returned lab score and contribution data"));
        assertTrue(delta.path("boundaryNote").asText().contains("does not change routing behavior"));

        JsonNode snapshot = result.path("decisionReplaySnapshot");
        assertTrue(snapshot.path("readOnly").asBoolean());
        assertEquals("decision-replay-snapshot/v1", snapshot.path("snapshotSchemaVersion").asText());
        assertEquals("PARTIAL", snapshot.path("status").asText());
        assertEquals("edge-alpha", snapshot.path("selectedCandidateId").asText());
        assertEquals("edge-beta", snapshot.path("closestAlternativeCandidateId").asText());
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", snapshot.path("strategyId").asText());
        assertEquals("AVAILABLE", snapshot.path("decisionVectorStatus").asText());
        assertEquals("AVAILABLE", snapshot.path("dominantFactorAnalysisStatus").asText());
        assertEquals("PARTIAL", snapshot.path("decisionDeltaAnalysisStatus").asText());
        assertEquals(3, snapshot.path("candidateCount").asInt());
        assertEquals("edge-alpha", snapshot.at("/candidateIdsConsidered/0").asText());
        assertTrue(snapshot.path("finalScoreGap").isNumber());
        assertTrue(snapshot.path("snapshotFingerprint").asText().matches("[0-9a-f]{64}"));
        assertTrue(snapshot.path("explanation").asText()
                .contains("derived from existing lab compare evidence only"));

        JsonNode trace = result.path("decisionReplayReconstructionTrace");
        assertTrue(trace.path("readOnly").asBoolean());
        assertEquals("decision-replay-reconstruction-trace/v1", trace.path("traceSchemaVersion").asText());
        assertEquals("PARTIAL", trace.path("status").asText());
        assertEquals("edge-alpha", trace.path("selectedCandidateId").asText());
        assertEquals("edge-beta", trace.path("closestAlternativeCandidateId").asText());
        assertEquals(snapshot.path("snapshotFingerprint").asText(), trace.path("snapshotFingerprint").asText());
        assertEquals("AVAILABLE", trace.path("decisionVectorStatus").asText());
        assertEquals("AVAILABLE", trace.path("factorContributionStatus").asText());
        assertEquals("AVAILABLE", trace.path("dominantFactorAnalysisStatus").asText());
        assertEquals("PARTIAL", trace.path("decisionDeltaAnalysisStatus").asText());
        assertEquals("PARTIAL", trace.path("decisionReplaySnapshotStatus").asText());
        assertEquals(3, trace.path("candidateCount").asInt());
        assertEquals("edge-alpha", trace.at("/candidateIdsConsidered/0").asText());
        assertTrue(trace.at("/candidateFinalScores/edge-alpha").isNumber());
        assertTrue(trace.path("finalScoreGap").isNumber());
        assertTrue(trace.path("traceFingerprint").asText().matches("[0-9a-f]{64}"));
        assertEquals("candidate-set-observed", trace.at("/reconstructionSteps/0/stepId").asText());
        assertEquals("replay-snapshot-fingerprint-observed", trace.at("/reconstructionSteps/8/stepId").asText());
        assertTrue(trace.path("explanation").asText()
                .contains("derived from existing lab compare evidence only"));

        JsonNode capsule = result.path("decisionReplayCapsule");
        assertTrue(capsule.path("readOnly").asBoolean());
        assertEquals("decision-replay-capsule/v1", capsule.path("capsuleSchemaVersion").asText());
        assertEquals("PARTIAL", capsule.path("status").asText());
        assertEquals("edge-alpha", capsule.path("selectedCandidateId").asText());
        assertEquals("edge-beta", capsule.path("closestAlternativeCandidateId").asText());
        assertEquals(snapshot.path("snapshotFingerprint").asText(),
                capsule.path("linkedReplaySnapshotFingerprint").asText());
        assertEquals(trace.path("traceFingerprint").asText(),
                capsule.path("linkedReconstructionTraceFingerprint").asText());
        assertEquals("AVAILABLE", capsule.path("decisionVectorStatus").asText());
        assertEquals("AVAILABLE", capsule.path("factorContributionStatus").asText());
        assertEquals("AVAILABLE", capsule.path("dominantFactorAnalysisStatus").asText());
        assertEquals("PARTIAL", capsule.path("decisionDeltaAnalysisStatus").asText());
        assertEquals("PARTIAL", capsule.path("decisionReplaySnapshotStatus").asText());
        assertEquals("PARTIAL", capsule.path("decisionReplayReconstructionTraceStatus").asText());
        assertEquals(3, capsule.path("candidateCount").asInt());
        assertEquals("edge-alpha", capsule.at("/candidateIdsConsidered/0").asText());
        assertTrue(capsule.path("finalScoreGap").isNumber());
        assertTrue(capsule.path("capsuleFingerprint").asText().matches("[0-9a-f]{64}"));
        assertTrue(capsule.path("candidateEvidence").isArray());
        assertTrue(capsule.path("factorEvidence").isArray());
        assertTrue(capsule.path("explanation").asText()
                .contains("packaged from existing lab compare evidence only"));

        JsonNode checklist = result.path("decisionReplayReadinessChecklist");
        assertTrue(checklist.path("readOnly").asBoolean());
        assertEquals("decision-replay-readiness-checklist/v1",
                checklist.path("checklistSchemaVersion").asText());
        assertEquals("PARTIAL", checklist.path("status").asText());
        assertEquals(snapshot.path("snapshotFingerprint").asText(),
                checklist.path("linkedReplaySnapshotFingerprint").asText());
        assertEquals(trace.path("traceFingerprint").asText(),
                checklist.path("linkedReconstructionTraceFingerprint").asText());
        assertEquals(capsule.path("capsuleFingerprint").asText(),
                checklist.path("linkedReplayCapsuleFingerprint").asText());
        assertEquals("edge-alpha", checklist.path("selectedCandidateId").asText());
        assertEquals(3, checklist.path("candidateCount").asInt());
        assertEquals("AVAILABLE", checklist.path("decisionVectorStatus").asText());
        assertEquals("AVAILABLE", checklist.path("dominantFactorAnalysisStatus").asText());
        assertEquals("PARTIAL", checklist.path("decisionDeltaAnalysisStatus").asText());
        assertEquals("PARTIAL", checklist.path("decisionReplayCapsuleStatus").asText());
        assertEquals("decision-vector-evidence", checklist.at("/checklistItems/0/itemId").asText());
        assertEquals("read-only-boundary-evidence", checklist.at("/checklistItems/8/itemId").asText());
        assertTrue(checklist.path("explanation").asText()
                .contains("derived from already-built lab compare evidence only"));

        JsonNode sourceMap = result.path("decisionReplayEvidenceSourceMap");
        assertTrue(sourceMap.path("readOnly").asBoolean());
        assertEquals("decision-replay-evidence-source-map/v1",
                sourceMap.path("sourceMapSchemaVersion").asText());
        assertEquals("PARTIAL", sourceMap.path("status").asText());
        assertEquals(snapshot.path("snapshotFingerprint").asText(),
                sourceMap.path("linkedReplaySnapshotFingerprint").asText());
        assertEquals(trace.path("traceFingerprint").asText(),
                sourceMap.path("linkedReconstructionTraceFingerprint").asText());
        assertEquals(capsule.path("capsuleFingerprint").asText(),
                sourceMap.path("linkedReplayCapsuleFingerprint").asText());
        assertEquals("edge-alpha", sourceMap.path("selectedCandidateId").asText());
        assertEquals(3, sourceMap.path("candidateCount").asInt());
        assertEquals("AVAILABLE", sourceMap.path("decisionVectorStatus").asText());
        assertEquals("AVAILABLE", sourceMap.path("dominantFactorAnalysisStatus").asText());
        assertEquals("PARTIAL", sourceMap.path("decisionDeltaAnalysisStatus").asText());
        assertEquals("PARTIAL", sourceMap.path("decisionReplayReadinessChecklistStatus").asText());
        assertEquals("decision-vector-source", sourceMap.at("/sourceMapEntries/0/sourceId").asText());
        assertEquals("linked-fingerprint-source", sourceMap.at("/sourceMapEntries/7/sourceId").asText());
        assertEquals("read-only-boundary-source", sourceMap.at("/sourceMapEntries/8/sourceId").asText());
        assertTrue(sourceMap.path("explanation").asText()
                .contains("derived from already-built lab compare evidence only"));

        JsonNode boundarySummary = result.path("decisionReplayEvidenceBoundarySummary");
        assertTrue(boundarySummary.path("readOnly").asBoolean());
        assertEquals("decision-replay-evidence-boundary-summary/v1",
                boundarySummary.path("boundarySummarySchemaVersion").asText());
        assertEquals("AVAILABLE", boundarySummary.path("status").asText());
        assertEquals("edge-alpha", boundarySummary.path("selectedCandidateId").asText());
        assertEquals(3, boundarySummary.path("candidateCount").asInt());
        assertEquals("AVAILABLE", boundarySummary.path("decisionVectorStatus").asText());
        assertEquals("PARTIAL", boundarySummary.path("decisionReplayEvidenceSourceMapStatus").asText());
        assertEquals("lab-only-boundary", boundarySummary.at("/boundaryItems/0/boundaryId").asText());
        assertEquals("fingerprint-boundary", boundarySummary.at("/boundaryItems/8/boundaryId").asText());
        assertEquals("production-not-proven-boundary", boundarySummary.at("/boundaryItems/9/boundaryId").asText());
        assertTrue(boundarySummary.path("explanation").asText()
                .contains("already-built lab compare evidence boundary fields"));

        JsonNode fieldInventory = result.path("decisionReplayEvidenceFieldInventory");
        assertTrue(fieldInventory.path("readOnly").asBoolean());
        assertEquals("decision-replay-evidence-field-inventory/v1",
                fieldInventory.path("fieldInventorySchemaVersion").asText());
        assertEquals("AVAILABLE", fieldInventory.path("status").asText());
        assertEquals("edge-alpha", fieldInventory.path("selectedCandidateId").asText());
        assertEquals(3, fieldInventory.path("candidateCount").asInt());
        assertEquals("AVAILABLE", fieldInventory.path("decisionVectorStatus").asText());
        assertEquals("AVAILABLE", fieldInventory.path("decisionReplayEvidenceBoundarySummaryStatus").asText());
        assertEquals(12, fieldInventory.path("availableInventoryGroupCount").asInt());
        assertEquals(0, fieldInventory.path("partialInventoryGroupCount").asInt());
        assertEquals(0, fieldInventory.path("unknownInventoryGroupCount").asInt());
        assertEquals("decision-vector-fields", fieldInventory.at("/inventoryEntries/0/inventoryId").asText());
        assertEquals("linked-fingerprint-fields", fieldInventory.at("/inventoryEntries/9/inventoryId").asText());
        assertEquals("read-only-boundary-fields", fieldInventory.at("/inventoryEntries/10/inventoryId").asText());
        assertEquals("production-not-proven-boundary-fields",
                fieldInventory.at("/inventoryEntries/11/inventoryId").asText());
        assertTrue(fieldInventory.at("/inventoryEntries/0/observedFieldPaths").toString()
                .contains("decisionVector.candidateSummaries"));
        assertEquals(15, fieldInventory.at("/inventoryEntries/0/observedFieldCount").asInt());
        assertEquals(0, fieldInventory.at("/inventoryEntries/0/missingOrUnavailableFieldCount").asInt());
        assertTrue(fieldInventory.path("explanation").asText()
                .contains("derived from already-built lab compare evidence only"));
        assertTrue(fieldInventory.path("boundaryNote").asText().contains("does not execute replay"));
        assertTrue(fieldInventory.path("boundaryNote").asText().contains("does not perform what-if mutation"));
        assertTrue(fieldInventory.path("boundaryNote").asText().contains("does not change routing behavior"));
        assertTrue(fieldInventory.path("boundaryNote").asText().contains("does not recompute scores"));
        assertTrue(fieldInventory.path("productionNotProvenBoundary").asText()
                .contains("not production certification"));
        assertTrue(fieldInventory.path("productionNotProvenBoundary").asText()
                .contains("not guaranteed replay"));

        JsonNode nullSafety = result.path("decisionReplayEvidenceNullSafetySummary");
        assertTrue(nullSafety.path("readOnly").asBoolean());
        assertEquals("decision-replay-evidence-null-safety-summary/v1",
                nullSafety.path("nullSafetySchemaVersion").asText());
        assertTrue(List.of("AVAILABLE", "PARTIAL", "UNKNOWN").contains(nullSafety.path("status").asText()));
        assertEquals("edge-alpha", nullSafety.path("selectedCandidateId").asText());
        assertEquals(3, nullSafety.path("candidateCount").asInt());
        assertEquals("AVAILABLE", nullSafety.path("decisionVectorStatus").asText());
        assertEquals("AVAILABLE", nullSafety.path("decisionReplayEvidenceFieldInventoryStatus").asText());
        assertEquals("selected-candidate-null-safety",
                nullSafety.at("/nullSafetyItems/0/nullSafetyId").asText());
        assertEquals("candidate-set-null-safety",
                nullSafety.at("/nullSafetyItems/1/nullSafetyId").asText());
        assertEquals("score-gap-null-safety",
                nullSafety.at("/nullSafetyItems/2/nullSafetyId").asText());
        assertEquals("linked-fingerprint-null-safety",
                nullSafety.at("/nullSafetyItems/5/nullSafetyId").asText());
        assertEquals("field-inventory-null-safety",
                nullSafety.at("/nullSafetyItems/8/nullSafetyId").asText());
        assertEquals("no-healthy-path-null-safety",
                nullSafety.at("/nullSafetyItems/9/nullSafetyId").asText());
        assertEquals("production-not-proven-null-safety",
                nullSafety.at("/nullSafetyItems/11/nullSafetyId").asText());
        assertTrue(nullSafety.at("/nullSafetyItems/0/checkedFieldCount").asInt() > 0);
        assertTrue(nullSafety.at("/nullSafetyItems/0/unavailableFieldCount").asInt() >= 0);
        assertTrue(nullSafety.path("explanation").asText()
                .contains("derived from already-built lab compare evidence only"));
        assertTrue(nullSafety.path("boundaryNote").asText().contains("does not execute replay"));
        assertTrue(nullSafety.path("boundaryNote").asText().contains("does not perform what-if mutation"));
        assertTrue(nullSafety.path("boundaryNote").asText().contains("does not change routing behavior"));
        assertTrue(nullSafety.path("boundaryNote").asText().contains("does not recompute scores"));
        assertTrue(nullSafety.path("productionNotProvenBoundary").asText()
                .contains("not production certification"));
        assertTrue(nullSafety.path("productionNotProvenBoundary").asText()
                .contains("not guaranteed replay"));
    }

    @Test
    void exposedDecisionVectorCarriesCandidateContributionsKnownUnknownSignalsAndBoundaries() throws Exception {
        JsonNode vector = postCompare(Files.readString(COMPARE_FIXTURE, StandardCharsets.UTF_8))
                .at("/results/0/decisionVector");

        String vectorText = vector.toString();
        String normalized = vectorText.toLowerCase(Locale.ROOT);

        assertTrue(vectorText.contains("healthState=true"));
        assertTrue(vectorText.contains("p95LatencyMillis=40.000000"));
        assertTrue(vectorText.contains("hidden routing internals not exposed"));
        assertTrue(vectorText.contains("exact production scoring not exposed"));
        assertTrue(vectorText.contains("production telemetry not exposed"));
        assertTrue(vectorText.contains("p95LatencyMillis"));
        assertTrue(vectorText.contains("EXACT_FROM_CALCULATOR"));
        assertTrue(vectorText.contains("hiddenRoutingInternals"));
        assertTrue(vectorText.contains("NOT_EXPOSED"));
        assertTrue(vector.path("factorContributionAvailability").asText()
                .contains("exposed for current ServerScoreCalculator components"));
        assertTrue(vector.path("replayReadiness").asText().contains("future/not implemented"));
        assertTrue(vector.path("whatIfReadiness").asText().contains("future/not implemented"));
        assertTrue(vector.path("structuredDecisionLoggingReadiness").asText().contains("future/not implemented"));
        assertTrue(normalized.contains("hidden scoring is not inferred"));
        assertTrue(normalized.contains("exact production scoring is not claimed"));
        assertTrue(normalized.contains("no production certification"));
        assertTrue(normalized.contains("production telemetry proof"));
        assertFalse(normalized.contains("hidden scoring is available"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("replay execution is implemented"));
        assertFalse(normalized.contains("what-if execution is implemented"));
    }

    @Test
    void allUnhealthyResultKeepsExistingFailureShapeAndNoInventedDecisionVector() throws Exception {
        JsonNode response = postCompare("""
                {
                  "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
                  "servers": [
                    {
                      "serverId": "edge-drain",
                      "healthy": false,
                      "inFlightRequestCount": 1,
                      "averageLatencyMillis": 10.0,
                      "p95LatencyMillis": 20.0,
                      "p99LatencyMillis": 30.0,
                      "recentErrorRate": 0.0
                    }
                  ]
                }
                """);

        JsonNode result = response.at("/results/0");
        assertEquals("SUCCESS", result.path("status").asText());
        assertTrue(result.path("chosenServerId").isNull());
        assertTrue(result.path("candidateServersConsidered").isEmpty());
        assertTrue(result.path("scores").isEmpty());
        assertTrue(result.path("decisionVector").isNull());
        assertEquals("UNKNOWN", result.at("/dominantFactorAnalysis/status").asText());
        assertTrue(result.at("/dominantFactorAnalysis/candidateAnalyses").isEmpty());
        assertEquals("UNKNOWN", result.at("/decisionDeltaAnalysis/status").asText());
        assertTrue(result.at("/decisionDeltaAnalysis/factorDeltas").isEmpty());
        assertTrue(result.at("/decisionDeltaAnalysis/explanation").asText()
                .contains("Decision delta analysis is unavailable"));
        assertEquals("UNKNOWN", result.at("/decisionReplaySnapshot/status").asText());
        assertTrue(result.at("/decisionReplaySnapshot/selectedCandidateId").isNull());
        assertTrue(result.at("/decisionReplaySnapshot/candidateIdsConsidered").isEmpty());
        assertTrue(result.at("/decisionReplaySnapshot/closestAlternativeCandidateId").isNull());
        assertTrue(result.at("/decisionReplaySnapshot/finalScoreGap").isNull());
        assertTrue(result.at("/decisionReplaySnapshot/explanation").asText()
                .contains("No replay execution"));
        assertEquals("UNKNOWN", result.at("/decisionReplayReconstructionTrace/status").asText());
        assertTrue(result.at("/decisionReplayReconstructionTrace/selectedCandidateId").isNull());
        assertTrue(result.at("/decisionReplayReconstructionTrace/candidateIdsConsidered").isEmpty());
        assertTrue(result.at("/decisionReplayReconstructionTrace/candidateFinalScores").isEmpty());
        assertTrue(result.at("/decisionReplayReconstructionTrace/closestAlternativeCandidateId").isNull());
        assertTrue(result.at("/decisionReplayReconstructionTrace/finalScoreGap").isNull());
        assertTrue(result.at("/decisionReplayReconstructionTrace/explanation").asText()
                .contains("No replay execution"));
        assertEquals("UNKNOWN", result.at("/decisionReplayCapsule/status").asText());
        assertTrue(result.at("/decisionReplayCapsule/selectedCandidateId").isNull());
        assertTrue(result.at("/decisionReplayCapsule/candidateIdsConsidered").isEmpty());
        assertTrue(result.at("/decisionReplayCapsule/candidateEvidence").isEmpty());
        assertTrue(result.at("/decisionReplayCapsule/factorEvidence").isEmpty());
        assertTrue(result.at("/decisionReplayCapsule/closestAlternativeCandidateId").isNull());
        assertTrue(result.at("/decisionReplayCapsule/finalScoreGap").isNull());
        assertTrue(result.at("/decisionReplayCapsule/explanation").asText()
                .contains("No replay execution"));
        assertEquals("UNKNOWN", result.at("/decisionReplayReadinessChecklist/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayReadinessChecklist/decisionVectorStatus").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayReadinessChecklist/decisionReplayCapsuleStatus").asText());
        assertTrue(result.at("/decisionReplayReadinessChecklist/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayReadinessChecklist/candidateCount").asInt());
        assertTrue(result.at("/decisionReplayReadinessChecklist/linkedReplaySnapshotFingerprint").isNull());
        assertTrue(result.at("/decisionReplayReadinessChecklist/linkedReconstructionTraceFingerprint").isNull());
        assertTrue(result.at("/decisionReplayReadinessChecklist/linkedReplayCapsuleFingerprint").isNull());
        assertEquals("UNKNOWN", result.at("/decisionReplayReadinessChecklist/checklistItems/6/status").asText());
        assertTrue(result.at("/decisionReplayReadinessChecklist/explanation").asText()
                .contains("No replay execution"));
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceSourceMap/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceSourceMap/decisionVectorStatus").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceSourceMap/decisionReplayCapsuleStatus").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceSourceMap/decisionReplayReadinessChecklistStatus").asText());
        assertTrue(result.at("/decisionReplayEvidenceSourceMap/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayEvidenceSourceMap/candidateCount").asInt());
        assertTrue(result.at("/decisionReplayEvidenceSourceMap/linkedReplaySnapshotFingerprint").isNull());
        assertTrue(result.at("/decisionReplayEvidenceSourceMap/linkedReconstructionTraceFingerprint").isNull());
        assertTrue(result.at("/decisionReplayEvidenceSourceMap/linkedReplayCapsuleFingerprint").isNull());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceSourceMap/sourceMapEntries/0/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceSourceMap/sourceMapEntries/7/status").asText());
        assertEquals("AVAILABLE", result.at("/decisionReplayEvidenceSourceMap/sourceMapEntries/8/status").asText());
        assertTrue(result.at("/decisionReplayEvidenceSourceMap/explanation").asText()
                .contains("No replay execution"));
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceBoundarySummary/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceBoundarySummary/decisionVectorStatus").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceBoundarySummary/decisionReplayEvidenceSourceMapStatus").asText());
        assertTrue(result.at("/decisionReplayEvidenceBoundarySummary/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayEvidenceBoundarySummary/candidateCount").asInt());
        assertEquals("lab-only-boundary",
                result.at("/decisionReplayEvidenceBoundarySummary/boundaryItems/0/boundaryId").asText());
        assertEquals("AVAILABLE",
                result.at("/decisionReplayEvidenceBoundarySummary/boundaryItems/8/status").asText());
        assertEquals("AVAILABLE",
                result.at("/decisionReplayEvidenceBoundarySummary/boundaryItems/9/status").asText());
        assertTrue(result.at("/decisionReplayEvidenceBoundarySummary/explanation").asText()
                .contains("No replay execution"));
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceFieldInventory/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceFieldInventory/decisionVectorStatus").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceFieldInventory/decisionReplayEvidenceBoundarySummaryStatus")
                        .asText());
        assertTrue(result.at("/decisionReplayEvidenceFieldInventory/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayEvidenceFieldInventory/candidateCount").asInt());
        assertEquals(10, result.at("/decisionReplayEvidenceFieldInventory/unknownInventoryGroupCount").asInt());
        assertEquals("decision-vector-fields",
                result.at("/decisionReplayEvidenceFieldInventory/inventoryEntries/0/inventoryId").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceFieldInventory/inventoryEntries/0/status")
                .asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceFieldInventory/inventoryEntries/9/status")
                .asText());
        assertEquals("production-not-proven-boundary-fields",
                result.at("/decisionReplayEvidenceFieldInventory/inventoryEntries/11/inventoryId").asText());
        assertTrue(result.at("/decisionReplayEvidenceFieldInventory/explanation").asText()
                .contains("No replay execution"));
        assertTrue(result.at("/decisionReplayEvidenceFieldInventory/explanation").asText()
                .contains("no selected candidate"));
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceNullSafetySummary/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceNullSafetySummary/decisionVectorStatus").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceNullSafetySummary/decisionReplayEvidenceFieldInventoryStatus")
                        .asText());
        assertTrue(result.at("/decisionReplayEvidenceNullSafetySummary/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayEvidenceNullSafetySummary/candidateCount").asInt());
        assertEquals("selected-candidate-null-safety",
                result.at("/decisionReplayEvidenceNullSafetySummary/nullSafetyItems/0/nullSafetyId").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceNullSafetySummary/nullSafetyItems/0/status").asText());
        assertEquals("candidate-set-null-safety",
                result.at("/decisionReplayEvidenceNullSafetySummary/nullSafetyItems/1/nullSafetyId").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceNullSafetySummary/nullSafetyItems/1/status").asText());
        assertEquals("no-healthy-path-null-safety",
                result.at("/decisionReplayEvidenceNullSafetySummary/nullSafetyItems/9/nullSafetyId").asText());
        assertEquals("AVAILABLE",
                result.at("/decisionReplayEvidenceNullSafetySummary/nullSafetyItems/9/status").asText());
        assertEquals("production-not-proven-null-safety",
                result.at("/decisionReplayEvidenceNullSafetySummary/nullSafetyItems/11/nullSafetyId").asText());
        assertTrue(result.at("/decisionReplayEvidenceNullSafetySummary/explanation").asText()
                .contains("No replay execution"));
        assertTrue(result.at("/decisionReplayEvidenceNullSafetySummary/explanation").asText()
                .contains("no selected candidate"));
        assertTrue(result.path("reason").asText().contains("No healthy eligible servers"));
    }

    private JsonNode postCompare(String body) throws Exception {
        String responseBody = mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return OBJECT_MAPPER.readTree(responseBody);
    }
}
