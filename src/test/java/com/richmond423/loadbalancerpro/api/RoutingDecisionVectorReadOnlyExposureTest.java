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
        JsonNode closureRollup = response.path("decisionReplayEvidenceReviewerClosureRollup");
        assertEquals("COMPLETE", closureRollup.path("status").asText());
        assertEquals("REVIEW_COMPLETE_WITH_LIMITATIONS", closureRollup.path("disposition").asText());
        assertEquals(response.path("results").size(), closureRollup.path("resultCount").asInt());
        assertEquals(response.path("results").size(), closureRollup.path("resultsWithClosureSummary").asInt());
        assertEquals(0, closureRollup.path("resultsMissingClosureSummary").asInt());
        assertEquals(response.path("results").size(), closureRollup.path("completeWithLimitationsCount").asInt());
        assertEquals(0, closureRollup.path("unknownCount").asInt());
        assertTrue(closureRollup.path("reviewerReady").asBoolean());
        assertTrue(closureRollup.path("summary").asText().contains("results include closure summaries"));
        assertEquals("not replay proof", closureRollup.at("/notProvenBoundaries/0").asText());
        assertEquals("not production validation", closureRollup.at("/notProvenBoundaries/6").asText());
        String closureRollupText = closureRollup.toString();
        assertFalse(closureRollupText.contains("reviewerClosureRollupFingerprint"));
        assertFalse(closureRollupText.contains("production certification is proven"));
        assertFalse(closureRollupText.contains("guaranteed replay is proven"));
        assertFalse(closureRollupText.contains("correctness validation is proven"));
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

        JsonNode statusRollup = result.path("decisionReplayEvidenceStatusRollup");
        assertTrue(statusRollup.path("readOnly").asBoolean());
        assertEquals("decision-replay-evidence-status-rollup/v1",
                statusRollup.path("statusRollupSchemaVersion").asText());
        assertTrue(List.of("AVAILABLE", "PARTIAL", "UNKNOWN").contains(statusRollup.path("status").asText()));
        assertEquals("edge-alpha", statusRollup.path("selectedCandidateId").asText());
        assertEquals(3, statusRollup.path("candidateCount").asInt());
        assertTrue(statusRollup.path("availableLaneCount").asInt() > 0);
        assertTrue(statusRollup.path("partialLaneCount").asInt() >= 0);
        assertEquals(0, statusRollup.path("unknownLaneCount").asInt());
        assertEquals("decision-vector-status",
                statusRollup.at("/statusItems/0/laneId").asText());
        assertEquals("dominant-factor-analysis-status",
                statusRollup.at("/statusItems/1/laneId").asText());
        assertEquals("decision-delta-analysis-status",
                statusRollup.at("/statusItems/2/laneId").asText());
        assertEquals("evidence-null-safety-status",
                statusRollup.at("/statusItems/10/laneId").asText());
        assertEquals("read-only-boundary-status",
                statusRollup.at("/statusItems/11/laneId").asText());
        assertEquals("production-not-proven-status",
                statusRollup.at("/statusItems/12/laneId").asText());
        assertTrue(statusRollup.at("/statusItems/0/selectedCandidatePresent").asBoolean());
        assertEquals(3, statusRollup.at("/statusItems/0/candidateCount").asInt());
        assertTrue(statusRollup.at("/statusItems/0/boundaryPresent").asBoolean());
        assertTrue(statusRollup.path("explanation").asText()
                .contains("derived from already-built lab compare evidence only"));
        assertTrue(statusRollup.path("boundaryNote").asText().contains("does not execute replay"));
        assertTrue(statusRollup.path("boundaryNote").asText().contains("does not perform what-if mutation"));
        assertTrue(statusRollup.path("boundaryNote").asText().contains("does not change routing behavior"));
        assertTrue(statusRollup.path("boundaryNote").asText().contains("does not recompute scores"));
        assertTrue(statusRollup.path("productionNotProvenBoundary").asText()
                .contains("not production certification"));
        assertTrue(statusRollup.path("productionNotProvenBoundary").asText()
                .contains("not guaranteed replay"));

        JsonNode laneNavigation = result.path("decisionReplayEvidenceLaneNavigationSummary");
        assertTrue(laneNavigation.path("readOnly").asBoolean());
        assertEquals("decision-replay-evidence-lane-navigation-summary/v1",
                laneNavigation.path("laneNavigationSchemaVersion").asText());
        assertTrue(List.of("AVAILABLE", "PARTIAL", "UNKNOWN").contains(laneNavigation.path("status").asText()));
        assertEquals("edge-alpha", laneNavigation.path("selectedCandidateId").asText());
        assertEquals(3, laneNavigation.path("candidateCount").asInt());
        assertTrue(laneNavigation.path("availableLaneCount").asInt() > 0);
        assertTrue(laneNavigation.path("partialLaneCount").asInt() >= 0);
        assertEquals(0, laneNavigation.path("unknownLaneCount").asInt());
        assertEquals("decision-vector-navigation",
                laneNavigation.at("/navigationItems/0/laneId").asText());
        assertEquals("results[].decisionVector",
                laneNavigation.at("/navigationItems/0/responseFieldPath").asText());
        assertEquals("Decision Vector",
                laneNavigation.at("/navigationItems/0/uiSectionLabel").asText());
        assertEquals("Enterprise Lab Decision Vector",
                laneNavigation.at("/navigationItems/0/docsReferenceLabel").asText());
        assertEquals("evidence-status-rollup-navigation",
                laneNavigation.at("/navigationItems/11/laneId").asText());
        assertEquals("results[].decisionReplayEvidenceStatusRollup",
                laneNavigation.at("/navigationItems/11/responseFieldPath").asText());
        assertTrue(laneNavigation.at("/navigationItems/0/readOnly").asBoolean());
        assertTrue(laneNavigation.at("/navigationItems/0/boundaryPresent").asBoolean());
        assertTrue(laneNavigation.path("explanation").asText()
                .contains("derived from already-built lab compare evidence only"));
        assertTrue(laneNavigation.path("boundaryNote").asText().contains("does not execute replay"));
        assertTrue(laneNavigation.path("boundaryNote").asText().contains("does not perform what-if mutation"));
        assertTrue(laneNavigation.path("boundaryNote").asText().contains("does not change routing behavior"));
        assertTrue(laneNavigation.path("boundaryNote").asText().contains("does not recompute scores"));
        assertTrue(laneNavigation.path("productionNotProvenBoundary").asText()
                .contains("not production certification"));
        assertTrue(laneNavigation.path("productionNotProvenBoundary").asText()
                .contains("not guaranteed replay"));

        JsonNode laneDependencyMap = result.path("decisionReplayEvidenceLaneDependencyMap");
        assertTrue(laneDependencyMap.path("readOnly").asBoolean());
        assertEquals("decision-replay-evidence-lane-dependency-map/v1",
                laneDependencyMap.path("laneDependencyMapSchemaVersion").asText());
        assertTrue(List.of("AVAILABLE", "PARTIAL", "UNKNOWN").contains(laneDependencyMap.path("status").asText()));
        assertEquals("edge-alpha", laneDependencyMap.path("selectedCandidateId").asText());
        assertEquals(3, laneDependencyMap.path("candidateCount").asInt());
        assertTrue(laneDependencyMap.path("availableLaneCount").asInt() > 0);
        assertTrue(laneDependencyMap.path("partialLaneCount").asInt() >= 0);
        assertEquals(0, laneDependencyMap.path("unknownLaneCount").asInt());
        assertEquals("decision-vector-dependency",
                laneDependencyMap.at("/dependencyItems/0/laneId").asText());
        assertEquals("results[].decisionVector",
                laneDependencyMap.at("/dependencyItems/0/responseFieldPath").asText());
        assertEquals(0, laneDependencyMap.at("/dependencyItems/0/dependencyCount").asInt());
        assertEquals(12, laneDependencyMap.at("/dependencyItems/0/downstreamCount").asInt());
        assertEquals("dominant-factor-analysis",
                laneDependencyMap.at("/dependencyItems/0/downstreamLaneIds/0").asText());
        assertEquals("evidence-lane-navigation-dependency",
                laneDependencyMap.at("/dependencyItems/12/laneId").asText());
        assertEquals(12, laneDependencyMap.at("/dependencyItems/12/dependencyCount").asInt());
        assertEquals(0, laneDependencyMap.at("/dependencyItems/12/downstreamCount").asInt());
        assertTrue(laneDependencyMap.at("/dependencyItems/0/readOnly").asBoolean());
        assertTrue(laneDependencyMap.at("/dependencyItems/0/boundaryPresent").asBoolean());
        assertTrue(laneDependencyMap.path("explanation").asText()
                .contains("derived from already-built lab compare evidence only"));
        assertTrue(laneDependencyMap.path("boundaryNote").asText().contains("does not execute replay"));
        assertTrue(laneDependencyMap.path("boundaryNote").asText().contains("does not perform what-if mutation"));
        assertTrue(laneDependencyMap.path("boundaryNote").asText().contains("does not change routing behavior"));
        assertTrue(laneDependencyMap.path("boundaryNote").asText().contains("does not recompute scores"));
        assertTrue(laneDependencyMap.path("productionNotProvenBoundary").asText()
                .contains("not production certification"));
        assertTrue(laneDependencyMap.path("productionNotProvenBoundary").asText()
                .contains("not guaranteed replay"));

        JsonNode laneReferenceIndex = result.path("decisionReplayEvidenceLaneReferenceIndex");
        assertTrue(laneReferenceIndex.path("readOnly").asBoolean());
        assertEquals("decision-replay-evidence-lane-reference-index/v1",
                laneReferenceIndex.path("laneReferenceIndexSchemaVersion").asText());
        assertTrue(List.of("AVAILABLE", "PARTIAL", "UNKNOWN").contains(laneReferenceIndex.path("status").asText()));
        assertEquals("edge-alpha", laneReferenceIndex.path("selectedCandidateId").asText());
        assertEquals(3, laneReferenceIndex.path("candidateCount").asInt());
        assertTrue(laneReferenceIndex.path("availableLaneCount").asInt() > 0);
        assertTrue(laneReferenceIndex.path("partialLaneCount").asInt() >= 0);
        assertEquals(0, laneReferenceIndex.path("unknownLaneCount").asInt());
        assertEquals(List.of(
                        "decision-vector-reference",
                        "dominant-factor-analysis-reference",
                        "decision-delta-analysis-reference",
                        "replay-snapshot-reference",
                        "reconstruction-trace-reference",
                        "replay-capsule-reference",
                        "readiness-checklist-reference",
                        "evidence-source-map-reference",
                        "evidence-boundary-summary-reference",
                        "evidence-field-inventory-reference",
                        "evidence-null-safety-reference",
                        "evidence-status-rollup-reference",
                        "evidence-lane-navigation-reference",
                        "evidence-lane-dependency-map-reference"),
                streamTextValues(laneReferenceIndex.path("referenceItems"), "laneId"));
        assertEquals("results[].decisionVector",
                laneReferenceIndex.at("/referenceItems/0/responseFieldPath").asText());
        assertEquals("Decision Vector",
                laneReferenceIndex.at("/referenceItems/0/uiSectionLabel").asText());
        assertEquals("Enterprise Lab Decision Vector",
                laneReferenceIndex.at("/referenceItems/0/docsReferenceLabel").asText());
        assertEquals(0, laneReferenceIndex.at("/referenceItems/0/dependencyCount").asInt());
        assertEquals(12, laneReferenceIndex.at("/referenceItems/0/downstreamCount").asInt());
        assertEquals("results[].decisionReplayEvidenceLaneDependencyMap",
                laneReferenceIndex.at("/referenceItems/13/responseFieldPath").asText());
        assertEquals("Decision Evidence Lane Dependency Map",
                laneReferenceIndex.at("/referenceItems/13/uiSectionLabel").asText());
        assertEquals("Decision Replay Evidence Lane Dependency Map",
                laneReferenceIndex.at("/referenceItems/13/docsReferenceLabel").asText());
        assertEquals(13, laneReferenceIndex.at("/referenceItems/13/dependencyCount").asInt());
        assertEquals(0, laneReferenceIndex.at("/referenceItems/13/downstreamCount").asInt());
        assertTrue(laneReferenceIndex.at("/referenceItems/0/readOnly").asBoolean());
        assertTrue(laneReferenceIndex.at("/referenceItems/0/boundaryPresent").asBoolean());
        assertTrue(laneReferenceIndex.path("explanation").asText()
                .contains("derived from already-built lab compare evidence"));
        assertTrue(laneReferenceIndex.path("boundaryNote").asText().contains("does not execute replay"));
        assertTrue(laneReferenceIndex.path("boundaryNote").asText().contains("does not perform what-if mutation"));
        assertTrue(laneReferenceIndex.path("boundaryNote").asText().contains("does not change routing behavior"));
        assertTrue(laneReferenceIndex.path("boundaryNote").asText().contains("does not recompute scores"));
        assertTrue(laneReferenceIndex.path("productionNotProvenBoundary").asText()
                .contains("not production certification"));
        assertTrue(laneReferenceIndex.path("productionNotProvenBoundary").asText()
                .contains("not guaranteed replay"));
        assertFalse(laneReferenceIndex.toString().contains("laneReferenceIndexFingerprint"));
        JsonNode laneDependencySummary = result.path("decisionReplayEvidenceLaneDependencySummary");
        assertTrue(laneDependencySummary.path("readOnly").asBoolean());
        assertEquals("decision-replay-evidence-lane-dependency-summary/v1",
                laneDependencySummary.path("laneDependencySummarySchemaVersion").asText());
        assertEquals(laneReferenceIndex.path("status").asText(), laneDependencySummary.path("status").asText());
        assertEquals(laneReferenceIndex.path("availableLaneCount").asInt(),
                laneDependencySummary.path("availableLaneCount").asInt());
        assertEquals(laneReferenceIndex.path("partialLaneCount").asInt(),
                laneDependencySummary.path("partialLaneCount").asInt());
        assertEquals(laneReferenceIndex.path("unknownLaneCount").asInt(),
                laneDependencySummary.path("unknownLaneCount").asInt());
        assertEquals(14, laneDependencySummary.path("totalLaneCount").asInt());
        assertEquals(1, laneDependencySummary.path("rootLaneCount").asInt());
        assertEquals(2, laneDependencySummary.path("terminalLaneCount").asInt());
        assertEquals(13, laneDependencySummary.path("maxDependencyCount").asInt());
        assertEquals(12, laneDependencySummary.path("maxDownstreamCount").asInt());
        assertEquals("evidence-lane-dependency-map-reference",
                laneDependencySummary.at("/densestDependencyLaneIds/0").asText());
        assertEquals("decision-vector-reference",
                laneDependencySummary.at("/widestDownstreamLaneIds/0").asText());
        assertTrue(laneDependencySummary.path("summaryText").asText()
                .contains("Reference index is " + laneReferenceIndex.path("status").asText()));
        assertTrue(laneDependencySummary.toString().contains("Does not execute replay"));
        assertTrue(laneDependencySummary.toString().contains("Not correctness validation"));
        assertFalse(laneDependencySummary.toString().contains("dependencySummaryFingerprint"));
        JsonNode laneConsistencySummary = result.path("decisionReplayEvidenceLaneConsistencySummary");
        assertTrue(laneConsistencySummary.path("readOnly").asBoolean());
        assertEquals("decision-replay-evidence-lane-consistency-summary/v1",
                laneConsistencySummary.path("laneConsistencySummarySchemaVersion").asText());
        assertEquals("CONSISTENT", laneConsistencySummary.path("status").asText());
        assertEquals("PARTIAL", laneConsistencySummary.path("referenceIndexStatus").asText());
        assertEquals("PARTIAL", laneConsistencySummary.path("dependencySummaryStatus").asText());
        assertEquals("PARTIAL", laneConsistencySummary.path("statusRollupStatus").asText());
        assertEquals("PARTIAL", laneConsistencySummary.path("dependencyMapStatus").asText());
        assertEquals(14, laneConsistencySummary.path("totalLaneCount").asInt());
        assertEquals(4, laneConsistencySummary.path("availableLaneCount").asInt());
        assertEquals(10, laneConsistencySummary.path("partialLaneCount").asInt());
        assertEquals(0, laneConsistencySummary.path("unknownLaneCount").asInt());
        assertEquals(13, laneConsistencySummary.path("dependencyMapLaneCount").asInt());
        assertEquals(14, laneConsistencySummary.path("referenceIndexLaneCount").asInt());
        assertEquals(14, laneConsistencySummary.path("dependencySummaryLaneCount").asInt());
        assertTrue(laneConsistencySummary.path("mismatchedCountFields").isEmpty());
        assertTrue(laneConsistencySummary.path("missingSurfaces").isEmpty());
        assertEquals("status-rollup-present",
                laneConsistencySummary.at("/consistencyChecks/0/name").asText());
        assertEquals("lane-count-alignment",
                laneConsistencySummary.at("/consistencyChecks/4/name").asText());
        assertTrue(laneConsistencySummary.path("summaryText").asText()
                .contains("Lane evidence surfaces are consistent"));
        assertTrue(laneConsistencySummary.toString().contains("Does not execute replay"));
        assertTrue(laneConsistencySummary.toString().contains("not scoring proof"));
        assertTrue(laneConsistencySummary.toString().contains("Not correctness validation"));
        assertTrue(laneConsistencySummary.toString().contains("not guaranteed replay"));
        assertFalse(laneConsistencySummary.toString().contains("dependencyConsistencyFingerprint"));
        JsonNode reviewerSnapshot = result.path("decisionReplayEvidenceReviewerSnapshot");
        assertTrue(reviewerSnapshot.path("readOnly").asBoolean());
        assertEquals("decision-replay-evidence-reviewer-snapshot/v1",
                reviewerSnapshot.path("reviewerSnapshotSchemaVersion").asText());
        assertEquals("PARTIAL", reviewerSnapshot.path("status").asText());
        assertEquals("CONSISTENT", reviewerSnapshot.path("consistencyStatus").asText());
        assertEquals("PARTIAL", reviewerSnapshot.path("referenceIndexStatus").asText());
        assertEquals("PARTIAL", reviewerSnapshot.path("dependencySummaryStatus").asText());
        assertEquals("PARTIAL", reviewerSnapshot.path("statusRollupStatus").asText());
        assertEquals("PARTIAL", reviewerSnapshot.path("dependencyMapStatus").asText());
        assertEquals("edge-alpha", reviewerSnapshot.path("selectedCandidateId").asText());
        assertEquals(3, reviewerSnapshot.path("candidateCount").asInt());
        assertEquals(14, reviewerSnapshot.path("totalLaneCount").asInt());
        assertEquals(4, reviewerSnapshot.path("availableLaneCount").asInt());
        assertEquals(10, reviewerSnapshot.path("partialLaneCount").asInt());
        assertEquals(0, reviewerSnapshot.path("unknownLaneCount").asInt());
        assertEquals(5, reviewerSnapshot.path("checkedSurfaceCount").asInt());
        assertEquals(0, reviewerSnapshot.path("missingSurfaceCount").asInt());
        assertTrue(reviewerSnapshot.path("missingSurfaces").isEmpty());
        assertEquals("14 evidence lanes summarized",
                reviewerSnapshot.at("/reviewerHighlights/0").asText());
        assertEquals("Consistency summary reports CONSISTENT",
                reviewerSnapshot.at("/reviewerHighlights/5").asText());
        assertEquals("10 lanes remain PARTIAL.",
                reviewerSnapshot.at("/reviewerWarnings/0").asText());
        assertTrue(reviewerSnapshot.path("summaryText").asText()
                .contains("Reviewer snapshot is PARTIAL"));
        assertTrue(reviewerSnapshot.toString().contains("Does not execute replay"));
        assertTrue(reviewerSnapshot.toString().contains("not scoring proof"));
        assertTrue(reviewerSnapshot.toString().contains("Not correctness validation"));
        assertTrue(reviewerSnapshot.toString().contains("not production certification"));
        assertTrue(reviewerSnapshot.toString().contains("not guaranteed replay"));
        assertFalse(reviewerSnapshot.toString().contains("reviewerSnapshotFingerprint"));
        JsonNode reviewerGuidance = result.path("decisionReplayEvidenceReviewerGuidance");
        assertTrue(reviewerGuidance.path("readOnly").asBoolean());
        assertEquals("decision-replay-evidence-reviewer-guidance/v1",
                reviewerGuidance.path("reviewerGuidanceSchemaVersion").asText());
        assertEquals("PARTIAL", reviewerGuidance.path("status").asText());
        assertEquals("REVIEW", reviewerGuidance.path("reviewerPriority").asText());
        assertEquals("edge-alpha", reviewerGuidance.path("selectedCandidateId").asText());
        assertEquals(3, reviewerGuidance.path("candidateCount").asInt());
        assertEquals(14, reviewerGuidance.path("totalLaneCount").asInt());
        assertEquals(4, reviewerGuidance.path("availableLaneCount").asInt());
        assertEquals(10, reviewerGuidance.path("partialLaneCount").asInt());
        assertEquals(0, reviewerGuidance.path("unknownLaneCount").asInt());
        assertEquals(6, reviewerGuidance.path("checkedSurfaceCount").asInt());
        assertEquals(0, reviewerGuidance.path("missingSurfaceCount").asInt());
        assertTrue(reviewerGuidance.path("missingSurfaces").isEmpty());
        assertEquals("Review partial or unknown evidence lanes before citing the lab explanation.",
                reviewerGuidance.path("primaryReviewerFocus").asText());
        assertEquals("Inspect Decision Replay Evidence Reviewer Snapshot warnings and highlights.",
                reviewerGuidance.at("/suggestedReviewSteps/0").asText());
        assertEquals("decisionReplayEvidenceReviewerSnapshot",
                reviewerGuidance.at("/evidenceSurfacesToInspect/5").asText());
        assertEquals("Reviewer snapshot status is PARTIAL.",
                reviewerGuidance.at("/cautionNotes/0").asText());
        assertTrue(reviewerGuidance.path("summaryText").asText()
                .contains("Reviewer guidance is PARTIAL"));
        assertTrue(reviewerGuidance.toString().contains("Not replay proof"));
        assertTrue(reviewerGuidance.toString().contains("Not scoring proof"));
        assertTrue(reviewerGuidance.toString().contains("Not correctness validation"));
        assertTrue(reviewerGuidance.toString().contains("not production certification"));
        assertTrue(reviewerGuidance.toString().contains("not guaranteed replay"));
        assertFalse(reviewerGuidance.toString().contains("reviewerGuidanceFingerprint"));
        JsonNode reviewerHandoff = result.path("decisionReplayEvidenceReviewerHandoffSummary");
        assertTrue(reviewerHandoff.path("readOnly").asBoolean());
        assertEquals("decision-replay-evidence-reviewer-handoff-summary/v1",
                reviewerHandoff.path("reviewerHandoffSummarySchemaVersion").asText());
        assertEquals("PARTIAL", reviewerHandoff.path("status").asText());
        assertEquals("REVIEW", reviewerHandoff.path("handoffPriority").asText());
        assertEquals("PARTIAL", reviewerHandoff.path("reviewerSnapshotStatus").asText());
        assertEquals("PARTIAL", reviewerHandoff.path("reviewerGuidanceStatus").asText());
        assertEquals("CONSISTENT", reviewerHandoff.path("consistencyStatus").asText());
        assertEquals("edge-alpha", reviewerHandoff.path("selectedCandidateId").asText());
        assertEquals(3, reviewerHandoff.path("candidateCount").asInt());
        assertEquals(14, reviewerHandoff.path("totalLaneCount").asInt());
        assertEquals(4, reviewerHandoff.path("availableLaneCount").asInt());
        assertEquals(10, reviewerHandoff.path("partialLaneCount").asInt());
        assertEquals(0, reviewerHandoff.path("unknownLaneCount").asInt());
        assertEquals("Reviewer snapshot is PARTIAL.",
                reviewerHandoff.at("/handoffBullets/0").asText());
        assertEquals("Review partial or unknown evidence lanes before operator handoff.",
                reviewerHandoff.at("/operatorFollowUpItems/0").asText());
        assertEquals("decisionReplayEvidenceReviewerGuidance",
                reviewerHandoff.at("/evidenceSurfacesReferenced/6").asText());
        assertEquals("Reviewer snapshot status is PARTIAL.",
                reviewerHandoff.at("/cautionNotes/0").asText());
        assertTrue(reviewerHandoff.path("summaryText").asText()
                .contains("Reviewer handoff summary is PARTIAL"));
        assertTrue(reviewerHandoff.toString().contains("Not replay proof"));
        assertTrue(reviewerHandoff.toString().contains("Not scoring proof"));
        assertTrue(reviewerHandoff.toString().contains("Not correctness validation"));
        assertTrue(reviewerHandoff.toString().contains("not production certification"));
        assertTrue(reviewerHandoff.toString().contains("not guaranteed replay"));
        assertTrue(reviewerHandoff.toString().contains("not production validation"));
        assertFalse(reviewerHandoff.toString().contains("reviewerHandoffFingerprint"));
        JsonNode reviewerClosure = result.path("decisionReplayEvidenceReviewerClosureSummary");
        assertTrue(reviewerClosure.path("readOnly").asBoolean());
        assertEquals("decision-replay-evidence-reviewer-closure-summary/v1",
                reviewerClosure.path("reviewerClosureSummarySchemaVersion").asText());
        assertEquals("PARTIAL", reviewerClosure.path("status").asText());
        assertEquals("REVIEW_COMPLETE_WITH_LIMITATIONS", reviewerClosure.path("closureDisposition").asText());
        assertEquals("PARTIAL", reviewerClosure.path("reviewerSnapshotStatus").asText());
        assertEquals("PARTIAL", reviewerClosure.path("reviewerGuidanceStatus").asText());
        assertEquals("PARTIAL", reviewerClosure.path("reviewerHandoffStatus").asText());
        assertEquals("CONSISTENT", reviewerClosure.path("consistencyStatus").asText());
        assertEquals("edge-alpha", reviewerClosure.path("selectedCandidateId").asText());
        assertEquals(3, reviewerClosure.path("candidateCount").asInt());
        assertEquals(14, reviewerClosure.path("totalLaneCount").asInt());
        assertEquals(4, reviewerClosure.path("availableLaneCount").asInt());
        assertEquals(10, reviewerClosure.path("partialLaneCount").asInt());
        assertEquals(0, reviewerClosure.path("unknownLaneCount").asInt());
        assertEquals("Reviewer snapshot is PARTIAL.",
                reviewerClosure.at("/closureBullets/0").asText());
        assertEquals("Reviewer metadata was generated deterministically from exposed compare surfaces.",
                reviewerClosure.at("/safeConclusions/0").asText());
        assertEquals("Not replay proof.",
                reviewerClosure.at("/unresolvedBoundaries/0").asText());
        assertEquals("decisionReplayEvidenceReviewerHandoffSummary",
                reviewerClosure.at("/evidenceSurfacesReferenced/7").asText());
        assertTrue(reviewerClosure.path("summaryText").asText()
                .contains("Reviewer closure summary is PARTIAL"));
        assertTrue(reviewerClosure.toString().contains("Not replay proof"));
        assertTrue(reviewerClosure.toString().contains("Not scoring proof"));
        assertTrue(reviewerClosure.toString().contains("Not correctness validation"));
        assertTrue(reviewerClosure.toString().contains("not production certification"));
        assertTrue(reviewerClosure.toString().contains("not guaranteed replay"));
        assertTrue(reviewerClosure.toString().contains("not production validation"));
        assertFalse(reviewerClosure.toString().contains("reviewerClosureFingerprint"));
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
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceStatusRollup/status").asText());
        assertTrue(result.at("/decisionReplayEvidenceStatusRollup/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayEvidenceStatusRollup/candidateCount").asInt());
        assertEquals("decision-vector-status",
                result.at("/decisionReplayEvidenceStatusRollup/statusItems/0/laneId").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceStatusRollup/statusItems/0/status").asText());
        assertEquals("evidence-null-safety-status",
                result.at("/decisionReplayEvidenceStatusRollup/statusItems/10/laneId").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceStatusRollup/statusItems/10/status").asText());
        assertEquals("read-only-boundary-status",
                result.at("/decisionReplayEvidenceStatusRollup/statusItems/11/laneId").asText());
        assertEquals("production-not-proven-status",
                result.at("/decisionReplayEvidenceStatusRollup/statusItems/12/laneId").asText());
        assertFalse(result.at("/decisionReplayEvidenceStatusRollup/statusItems/0/selectedCandidatePresent")
                .asBoolean());
        assertTrue(result.at("/decisionReplayEvidenceStatusRollup/statusItems/12/boundaryPresent").asBoolean());
        assertTrue(result.at("/decisionReplayEvidenceStatusRollup/explanation").asText()
                .contains("No replay execution"));
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceLaneNavigationSummary/status").asText());
        assertTrue(result.at("/decisionReplayEvidenceLaneNavigationSummary/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayEvidenceLaneNavigationSummary/candidateCount").asInt());
        assertEquals("decision-vector-navigation",
                result.at("/decisionReplayEvidenceLaneNavigationSummary/navigationItems/0/laneId").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceLaneNavigationSummary/navigationItems/0/status").asText());
        assertEquals("results[].decisionVector",
                result.at("/decisionReplayEvidenceLaneNavigationSummary/navigationItems/0/responseFieldPath")
                        .asText());
        assertEquals("evidence-status-rollup-navigation",
                result.at("/decisionReplayEvidenceLaneNavigationSummary/navigationItems/11/laneId").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceLaneNavigationSummary/navigationItems/11/status").asText());
        assertTrue(result.at("/decisionReplayEvidenceLaneNavigationSummary/navigationItems/11/boundaryPresent")
                .asBoolean());
        assertTrue(result.at("/decisionReplayEvidenceLaneNavigationSummary/explanation").asText()
                .contains("No replay execution"));
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceLaneDependencyMap/status").asText());
        assertTrue(result.at("/decisionReplayEvidenceLaneDependencyMap/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayEvidenceLaneDependencyMap/candidateCount").asInt());
        assertEquals("decision-vector-dependency",
                result.at("/decisionReplayEvidenceLaneDependencyMap/dependencyItems/0/laneId").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceLaneDependencyMap/dependencyItems/0/status").asText());
        assertEquals("results[].decisionVector",
                result.at("/decisionReplayEvidenceLaneDependencyMap/dependencyItems/0/responseFieldPath")
                        .asText());
        assertEquals(0, result.at("/decisionReplayEvidenceLaneDependencyMap/dependencyItems/0/dependencyCount")
                .asInt());
        assertEquals(12, result.at("/decisionReplayEvidenceLaneDependencyMap/dependencyItems/0/downstreamCount")
                .asInt());
        assertEquals("evidence-lane-navigation-dependency",
                result.at("/decisionReplayEvidenceLaneDependencyMap/dependencyItems/12/laneId").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceLaneDependencyMap/dependencyItems/12/status").asText());
        assertEquals(12, result.at("/decisionReplayEvidenceLaneDependencyMap/dependencyItems/12/dependencyCount")
                .asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceLaneDependencyMap/dependencyItems/12/downstreamCount")
                .asInt());
        assertTrue(result.at("/decisionReplayEvidenceLaneDependencyMap/dependencyItems/12/boundaryPresent")
                .asBoolean());
        assertTrue(result.at("/decisionReplayEvidenceLaneDependencyMap/explanation").asText()
                .contains("No replay execution"));
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceLaneReferenceIndex/status").asText());
        assertTrue(result.at("/decisionReplayEvidenceLaneReferenceIndex/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayEvidenceLaneReferenceIndex/candidateCount").asInt());
        assertEquals("decision-vector-reference",
                result.at("/decisionReplayEvidenceLaneReferenceIndex/referenceItems/0/laneId").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceLaneReferenceIndex/referenceItems/0/status").asText());
        assertEquals("results[].decisionVector",
                result.at("/decisionReplayEvidenceLaneReferenceIndex/referenceItems/0/responseFieldPath").asText());
        assertEquals(0, result.at("/decisionReplayEvidenceLaneReferenceIndex/referenceItems/0/dependencyCount")
                .asInt());
        assertEquals(12, result.at("/decisionReplayEvidenceLaneReferenceIndex/referenceItems/0/downstreamCount")
                .asInt());
        assertEquals("evidence-lane-dependency-map-reference",
                result.at("/decisionReplayEvidenceLaneReferenceIndex/referenceItems/13/laneId").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceLaneReferenceIndex/referenceItems/13/status").asText());
        assertEquals(13, result.at("/decisionReplayEvidenceLaneReferenceIndex/referenceItems/13/dependencyCount")
                .asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceLaneReferenceIndex/referenceItems/13/downstreamCount")
                .asInt());
        assertTrue(result.at("/decisionReplayEvidenceLaneReferenceIndex/referenceItems/13/boundaryPresent")
                .asBoolean());
        assertTrue(result.at("/decisionReplayEvidenceLaneReferenceIndex/explanation").asText()
                .contains("No selected candidate"));
        String laneReferenceText = result.at("/decisionReplayEvidenceLaneReferenceIndex").toString();
        assertFalse(laneReferenceText.contains("laneReferenceIndexFingerprint"));
        assertFalse(laneReferenceText.contains("production certification is proven"));
        assertFalse(laneReferenceText.contains("guaranteed replay is proven"));
        assertFalse(laneReferenceText.contains("quality ranking is proven"));
        assertFalse(laneReferenceText.contains("approval is granted"));
        assertFalse(laneReferenceText.contains("correctness validation is proven"));
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceLaneDependencySummary/status").asText());
        assertTrue(result.at("/decisionReplayEvidenceLaneDependencySummary/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayEvidenceLaneDependencySummary/candidateCount").asInt());
        assertEquals(14, result.at("/decisionReplayEvidenceLaneDependencySummary/totalLaneCount").asInt());
        assertEquals(1, result.at("/decisionReplayEvidenceLaneDependencySummary/rootLaneCount").asInt());
        assertEquals(2, result.at("/decisionReplayEvidenceLaneDependencySummary/terminalLaneCount").asInt());
        assertEquals("evidence-lane-dependency-map-reference",
                result.at("/decisionReplayEvidenceLaneDependencySummary/densestDependencyLaneIds/0").asText());
        assertEquals("decision-vector-reference",
                result.at("/decisionReplayEvidenceLaneDependencySummary/widestDownstreamLaneIds/0").asText());
        String laneDependencySummaryText = result.at("/decisionReplayEvidenceLaneDependencySummary").toString();
        assertFalse(laneDependencySummaryText.contains("dependencySummaryFingerprint"));
        assertFalse(laneDependencySummaryText.contains("production certification is proven"));
        assertFalse(laneDependencySummaryText.contains("guaranteed replay is proven"));
        assertFalse(laneDependencySummaryText.contains("quality ranking is proven"));
        assertFalse(laneDependencySummaryText.contains("approval is granted"));
        assertFalse(laneDependencySummaryText.contains("correctness validation is proven"));
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceLaneConsistencySummary/status").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceLaneConsistencySummary/referenceIndexStatus").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceLaneConsistencySummary/dependencySummaryStatus").asText());
        assertTrue(result.at("/decisionReplayEvidenceLaneConsistencySummary/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayEvidenceLaneConsistencySummary/candidateCount").asInt());
        assertEquals(14, result.at("/decisionReplayEvidenceLaneConsistencySummary/referenceIndexLaneCount").asInt());
        assertEquals(14, result.at("/decisionReplayEvidenceLaneConsistencySummary/dependencySummaryLaneCount")
                .asInt());
        assertEquals(13, result.at("/decisionReplayEvidenceLaneConsistencySummary/dependencyMapLaneCount").asInt());
        assertTrue(result.at("/decisionReplayEvidenceLaneConsistencySummary/missingSurfaces").isEmpty());
        assertTrue(result.at("/decisionReplayEvidenceLaneConsistencySummary/mismatchedCountFields").isEmpty());
        String laneConsistencySummaryText = result.at("/decisionReplayEvidenceLaneConsistencySummary").toString();
        assertFalse(laneConsistencySummaryText.contains("dependencyConsistencyFingerprint"));
        assertFalse(laneConsistencySummaryText.contains("production certification is proven"));
        assertFalse(laneConsistencySummaryText.contains("guaranteed replay is proven"));
        assertFalse(laneConsistencySummaryText.contains("quality ranking is proven"));
        assertFalse(laneConsistencySummaryText.contains("approval is granted"));
        assertFalse(laneConsistencySummaryText.contains("correctness validation is proven"));
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceReviewerSnapshot/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceReviewerSnapshot/consistencyStatus").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceReviewerSnapshot/referenceIndexStatus").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceReviewerSnapshot/dependencySummaryStatus").asText());
        assertTrue(result.at("/decisionReplayEvidenceReviewerSnapshot/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerSnapshot/candidateCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerSnapshot/totalLaneCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerSnapshot/availableLaneCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerSnapshot/partialLaneCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerSnapshot/unknownLaneCount").asInt());
        assertEquals(5, result.at("/decisionReplayEvidenceReviewerSnapshot/checkedSurfaceCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerSnapshot/missingSurfaceCount").asInt());
        assertTrue(result.at("/decisionReplayEvidenceReviewerSnapshot/missingSurfaces").isEmpty());
        assertEquals("No selected candidate evidence is available for reviewer snapshot.",
                result.at("/decisionReplayEvidenceReviewerSnapshot/reviewerWarnings/0").asText());
        String reviewerSnapshotText = result.at("/decisionReplayEvidenceReviewerSnapshot").toString();
        assertFalse(reviewerSnapshotText.contains("reviewerSnapshotFingerprint"));
        assertFalse(reviewerSnapshotText.contains("production certification is proven"));
        assertFalse(reviewerSnapshotText.contains("guaranteed replay is proven"));
        assertFalse(reviewerSnapshotText.contains("quality ranking is proven"));
        assertFalse(reviewerSnapshotText.contains("approval is granted"));
        assertFalse(reviewerSnapshotText.contains("correctness validation is proven"));
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceReviewerGuidance/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceReviewerGuidance/reviewerPriority").asText());
        assertTrue(result.at("/decisionReplayEvidenceReviewerGuidance/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerGuidance/candidateCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerGuidance/totalLaneCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerGuidance/availableLaneCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerGuidance/partialLaneCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerGuidance/unknownLaneCount").asInt());
        assertEquals(6, result.at("/decisionReplayEvidenceReviewerGuidance/checkedSurfaceCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerGuidance/missingSurfaceCount").asInt());
        assertTrue(result.at("/decisionReplayEvidenceReviewerGuidance/missingSurfaces").isEmpty());
        assertEquals("No selected candidate evidence is available for reviewer guidance.",
                result.at("/decisionReplayEvidenceReviewerGuidance/cautionNotes/0").asText());
        String reviewerGuidanceText = result.at("/decisionReplayEvidenceReviewerGuidance").toString();
        assertFalse(reviewerGuidanceText.contains("reviewerGuidanceFingerprint"));
        assertFalse(reviewerGuidanceText.contains("production certification is proven"));
        assertFalse(reviewerGuidanceText.contains("guaranteed replay is proven"));
        assertFalse(reviewerGuidanceText.contains("quality ranking is proven"));
        assertFalse(reviewerGuidanceText.contains("approval is granted"));
        assertFalse(reviewerGuidanceText.contains("correctness validation is proven"));
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceReviewerHandoffSummary/status").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceReviewerHandoffSummary/handoffPriority").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceReviewerHandoffSummary/reviewerSnapshotStatus").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceReviewerHandoffSummary/reviewerGuidanceStatus").asText());
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceReviewerHandoffSummary/consistencyStatus").asText());
        assertTrue(result.at("/decisionReplayEvidenceReviewerHandoffSummary/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerHandoffSummary/candidateCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerHandoffSummary/totalLaneCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerHandoffSummary/availableLaneCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerHandoffSummary/partialLaneCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerHandoffSummary/unknownLaneCount").asInt());
        assertEquals("No selected candidate evidence is available for reviewer handoff.",
                result.at("/decisionReplayEvidenceReviewerHandoffSummary/cautionNotes/0").asText());
        String reviewerHandoffText = result.at("/decisionReplayEvidenceReviewerHandoffSummary").toString();
        assertFalse(reviewerHandoffText.contains("reviewerHandoffFingerprint"));
        assertFalse(reviewerHandoffText.contains("production certification is proven"));
        assertFalse(reviewerHandoffText.contains("guaranteed replay is proven"));
        assertFalse(reviewerHandoffText.contains("quality ranking is proven"));
        assertFalse(reviewerHandoffText.contains("approval is granted"));
        assertFalse(reviewerHandoffText.contains("correctness validation is proven"));
        assertEquals("UNKNOWN", result.at("/decisionReplayEvidenceReviewerClosureSummary/status").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceReviewerClosureSummary/closureDisposition").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceReviewerClosureSummary/reviewerSnapshotStatus").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceReviewerClosureSummary/reviewerGuidanceStatus").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceReviewerClosureSummary/reviewerHandoffStatus").asText());
        assertEquals("UNKNOWN",
                result.at("/decisionReplayEvidenceReviewerClosureSummary/consistencyStatus").asText());
        assertTrue(result.at("/decisionReplayEvidenceReviewerClosureSummary/selectedCandidateId").isNull());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerClosureSummary/candidateCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerClosureSummary/totalLaneCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerClosureSummary/availableLaneCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerClosureSummary/partialLaneCount").asInt());
        assertEquals(0, result.at("/decisionReplayEvidenceReviewerClosureSummary/unknownLaneCount").asInt());
        assertEquals("Reviewer closure summary status is UNKNOWN",
                result.at("/decisionReplayEvidenceReviewerClosureSummary/closureBullets/1").asText());
        assertTrue(result.at("/decisionReplayEvidenceReviewerClosureSummary/safeConclusions").isEmpty());
        assertEquals("Not replay proof.",
                result.at("/decisionReplayEvidenceReviewerClosureSummary/unresolvedBoundaries/0").asText());
        String reviewerClosureText = result.at("/decisionReplayEvidenceReviewerClosureSummary").toString();
        assertFalse(reviewerClosureText.contains("reviewerClosureFingerprint"));
        assertFalse(reviewerClosureText.contains("production certification is proven"));
        assertFalse(reviewerClosureText.contains("guaranteed replay is proven"));
        assertFalse(reviewerClosureText.contains("quality ranking is proven"));
        assertFalse(reviewerClosureText.contains("approval is granted"));
        assertFalse(reviewerClosureText.contains("correctness validation is proven"));
        JsonNode closureRollup = response.path("decisionReplayEvidenceReviewerClosureRollup");
        assertEquals("UNKNOWN", closureRollup.path("status").asText());
        assertEquals("UNKNOWN", closureRollup.path("disposition").asText());
        assertEquals(1, closureRollup.path("resultCount").asInt());
        assertEquals(1, closureRollup.path("resultsWithClosureSummary").asInt());
        assertEquals(0, closureRollup.path("resultsMissingClosureSummary").asInt());
        assertEquals(0, closureRollup.path("completeWithLimitationsCount").asInt());
        assertEquals(1, closureRollup.path("unknownCount").asInt());
        assertFalse(closureRollup.path("reviewerReady").asBoolean());
        assertEquals("not replay proof", closureRollup.at("/notProvenBoundaries/0").asText());
        assertEquals("not production validation", closureRollup.at("/notProvenBoundaries/6").asText());
        assertFalse(closureRollup.toString().contains("reviewerClosureRollupFingerprint"));
        assertFalse(closureRollup.toString().contains("production certification is proven"));
        assertFalse(closureRollup.toString().contains("guaranteed replay is proven"));
        assertFalse(closureRollup.toString().contains("correctness validation is proven"));
        assertTrue(result.path("reason").asText().contains("No healthy eligible servers"));
    }

    private static List<String> streamTextValues(JsonNode values, String fieldName) {
        List<String> result = new java.util.ArrayList<>();
        values.forEach(value -> result.add(value.path(fieldName).asText()));
        return result;
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
