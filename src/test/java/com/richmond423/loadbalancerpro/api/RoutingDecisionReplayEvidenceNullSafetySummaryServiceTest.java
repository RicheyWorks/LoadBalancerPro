package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RoutingDecisionReplayEvidenceNullSafetySummaryServiceTest {
    private final RoutingDecisionReplayEvidenceNullSafetySummaryService service =
            new RoutingDecisionReplayEvidenceNullSafetySummaryService();

    @Test
    void nullSafetyItemsAreDeterministicForEquivalentAlreadyBuiltEvidence() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true, contribution("p95LatencyMillis", -2.0)),
                candidate("alt-edge", false, contribution("p95LatencyMillis", -5.0)));
        DominantFactorAnalysisResponse dominant = dominant("AVAILABLE");
        RoutingDecisionDeltaAnalysisResponse delta = delta("AVAILABLE");
        RoutingDecisionReplaySnapshotResponse snapshot = snapshot("AVAILABLE", "snapshot-fingerprint");
        RoutingDecisionReplayReconstructionTraceResponse trace = trace("AVAILABLE", "trace-fingerprint",
                "snapshot-fingerprint");
        RoutingDecisionReplayCapsuleResponse capsule = capsule("AVAILABLE", "capsule-fingerprint",
                "snapshot-fingerprint", "trace-fingerprint");
        RoutingDecisionReplayReadinessChecklistResponse checklist = checklist("AVAILABLE",
                "snapshot-fingerprint", "trace-fingerprint", "capsule-fingerprint");
        RoutingDecisionReplayEvidenceSourceMapResponse sourceMap = sourceMap("AVAILABLE",
                "snapshot-fingerprint", "trace-fingerprint", "capsule-fingerprint");
        RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary = boundarySummary("AVAILABLE");
        RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory = fieldInventory("AVAILABLE");

        RoutingDecisionReplayEvidenceNullSafetySummaryResponse first = service.nullSafetySummary(
                "TAIL_LATENCY_POWER_OF_TWO", vector, dominant, delta, snapshot, trace, capsule, checklist,
                sourceMap, boundarySummary, fieldInventory);
        RoutingDecisionReplayEvidenceNullSafetySummaryResponse second = service.nullSafetySummary(
                "TAIL_LATENCY_POWER_OF_TWO", vector, dominant, delta, snapshot, trace, capsule, checklist,
                sourceMap, boundarySummary, fieldInventory);

        assertEquals(first, second);
        assertTrue(first.readOnly());
        assertEquals("decision-replay-evidence-null-safety-summary/v1", first.nullSafetySchemaVersion());
        assertTrue(List.of("AVAILABLE", "PARTIAL").contains(first.status()));
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", first.strategyId());
        assertEquals("selected-edge", first.selectedCandidateId());
        assertEquals(2, first.candidateCount());
        assertEquals("AVAILABLE", first.decisionVectorStatus());
        assertEquals("AVAILABLE", first.decisionReplayEvidenceFieldInventoryStatus());
        assertEquals(List.of(
                        "selected-candidate-null-safety",
                        "candidate-set-null-safety",
                        "score-gap-null-safety",
                        "closest-alternative-null-safety",
                        "largest-delta-factor-null-safety",
                        "linked-fingerprint-null-safety",
                        "candidate-evidence-null-safety",
                        "factor-evidence-null-safety",
                        "field-inventory-null-safety",
                        "no-healthy-path-null-safety",
                        "boundary-text-null-safety",
                        "production-not-proven-null-safety"),
                first.nullSafetyItems().stream()
                        .map(DecisionReplayEvidenceNullSafetyItemResponse::nullSafetyId)
                        .toList());
        assertEquals("selected candidate fields across already-built compare evidence",
                first.nullSafetyItems().get(0).sourceFieldPath());
        assertTrue(first.nullSafetyItems().get(0).checkedFieldPaths()
                .contains("decisionReplaySnapshot.selectedCandidateId"));
        assertTrue(first.nullSafetyItems().get(5).checkedFieldPaths()
                .contains("decisionReplayCapsule.capsuleFingerprint"));
        assertTrue(first.nullSafetyItems().get(6).checkedFieldPaths()
                .contains("decisionReplayCapsule.candidateEvidence"));
        assertTrue(first.nullSafetyItems().get(7).checkedFieldPaths()
                .contains("decisionReplayCapsule.factorEvidence"));
        assertTrue(first.availableNullSafetyItemCount() > 0);
        assertTrue(first.partialNullSafetyItemCount() >= 0);
        assertTrue(first.unknownNullSafetyItemCount() >= 0);
        assertThrows(UnsupportedOperationException.class,
                () -> first.nullSafetyItems().add(first.nullSafetyItems().get(0)));
        assertThrows(UnsupportedOperationException.class,
                () -> first.nullSafetyItems().get(0).checkedFieldPaths().add("mutated"));
        assertTrue(first.explanation().contains("derived from already-built lab compare evidence only"));
        assertTrue(first.boundaryNote().contains("does not execute replay"));
        assertTrue(first.boundaryNote().contains("does not perform what-if mutation"));
        assertTrue(first.boundaryNote().contains("does not change routing behavior"));
        assertTrue(first.boundaryNote().contains("does not recompute scores"));
        assertTrue(first.productionNotProvenBoundary().contains("not production certification"));
        assertTrue(first.productionNotProvenBoundary().contains("not guaranteed replay"));
        assertFalse(first.toString().contains("nullSafetyFingerprint"));
    }

    @Test
    void partialEvidenceReturnsPartialWithoutInventingUnavailableValues() {
        RoutingDecisionVectorResponse vector = vector(candidate("selected-edge", true));
        DominantFactorAnalysisResponse dominant = dominant("AVAILABLE");
        RoutingDecisionDeltaAnalysisResponse delta = unknownDelta();
        RoutingDecisionReplaySnapshotResponse snapshot = snapshot("PARTIAL", "snapshot-fingerprint");
        RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory = partialFieldInventory();

        RoutingDecisionReplayEvidenceNullSafetySummaryResponse summary = service.nullSafetySummary(
                "TAIL_LATENCY_POWER_OF_TWO", vector, dominant, delta, snapshot,
                null, null, null, null, null, fieldInventory);

        assertEquals("PARTIAL", summary.status());
        assertEquals("selected-edge", summary.selectedCandidateId());
        assertEquals(1, summary.candidateCount());
        assertTrue(summary.partialNullSafetyItemCount() > 0);
        assertTrue(summary.unknownNullSafetyItemCount() > 0);
        assertEquals("PARTIAL", summary.nullSafetyItems().get(0).status());
        assertTrue(summary.nullSafetyItems().get(0).unavailableFieldPaths()
                .contains("decisionReplayCapsule.selectedCandidateId"));
        assertEquals("PARTIAL", summary.nullSafetyItems().get(3).status());
        assertTrue(summary.nullSafetyItems().get(2).safetySummary().contains("gaps are not computed or invented"));
        assertFalse(summary.toString().contains("nullSafetyFingerprint"));
    }

    @Test
    void missingEvidenceReturnsUnknownWithoutInventingFailurePathDetails() {
        DominantFactorAnalysisResponse dominant = dominant("UNKNOWN");
        RoutingDecisionDeltaAnalysisResponse delta = unknownDelta();
        RoutingDecisionReplaySnapshotResponse snapshot = snapshot("UNKNOWN", null);
        RoutingDecisionReplayReconstructionTraceResponse trace = trace("UNKNOWN", null, null);
        RoutingDecisionReplayCapsuleResponse capsule = capsule("UNKNOWN", null, null, null);
        RoutingDecisionReplayReadinessChecklistResponse checklist = checklist("UNKNOWN", null, null, null);
        RoutingDecisionReplayEvidenceSourceMapResponse sourceMap = sourceMap("UNKNOWN", null, null, null);
        RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary = boundarySummary("UNKNOWN");
        RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory = fieldInventory("UNKNOWN");

        RoutingDecisionReplayEvidenceNullSafetySummaryResponse summary = service.nullSafetySummary(
                "TAIL_LATENCY_POWER_OF_TWO", null, dominant, delta, snapshot, trace, capsule, checklist,
                sourceMap, boundarySummary, fieldInventory);

        assertEquals("UNKNOWN", summary.status());
        assertNull(summary.selectedCandidateId());
        assertEquals(0, summary.candidateCount());
        assertEquals("UNKNOWN", summary.decisionVectorStatus());
        assertEquals("UNKNOWN", summary.decisionReplayCapsuleStatus());
        assertEquals("UNKNOWN", summary.decisionReplayEvidenceFieldInventoryStatus());
        assertEquals("UNKNOWN", summary.nullSafetyItems().get(0).status());
        assertEquals("UNKNOWN", summary.nullSafetyItems().get(1).status());
        assertEquals("AVAILABLE", summary.nullSafetyItems().get(9).status());
        assertTrue(List.of("AVAILABLE", "PARTIAL").contains(summary.nullSafetyItems().get(10).status()));
        assertTrue(List.of("AVAILABLE", "PARTIAL").contains(summary.nullSafetyItems().get(11).status()));
        assertTrue(summary.nullSafetyItems().get(9).checkedFieldPaths()
                .contains("decisionReplaySnapshot.selectedCandidateId"));
        assertTrue(summary.explanation().contains("No replay execution"));
        assertTrue(summary.explanation().contains("no selected candidate"));
        assertTrue(summary.explanation().contains("guaranteed replay"));
        assertFalse(summary.explanation().contains("guaranteed replay is proven"));
        assertFalse(summary.explanation().contains("production certification is proven"));
    }

    @Test
    void nullSafetySummaryDoesNotUseScoringReflectionFingerprintPersistenceExportOrEnvironmentInputs()
            throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayEvidenceNullSafetySummaryService.java"),
                StandardCharsets.UTF_8);
        String normalized = source.toLowerCase(Locale.ROOT);

        assertFalse(source.contains("ServerScoreCalculator"));
        assertFalse(source.contains("ServerStateVector"));
        assertFalse(source.contains("MessageDigest"));
        assertFalse(source.contains("SHA-256"));
        assertFalse(normalized.contains("sha256"));
        assertFalse(normalized.contains("instant.now"));
        assertFalse(normalized.contains("system.getenv"));
        assertFalse(normalized.contains("system.getproperty"));
        assertFalse(normalized.contains("randomuuid"));
        assertFalse(normalized.contains("hostname"));
        assertFalse(normalized.contains("files.write"));
        assertFalse(normalized.contains("@postmapping"));
        assertFalse(normalized.contains("@getmapping"));
        assertFalse(normalized.contains("zipoutputstream"));
        assertFalse(normalized.contains("processbuilder"));
        assertFalse(normalized.contains("runtime.getruntime"));
        assertFalse(source.contains(".getDeclared"));
        assertFalse(source.contains(".getFields"));
        assertFalse(source.contains(".getMethods"));
        assertFalse(normalized.contains("executed replay"));
        assertFalse(normalized.contains("what-if mutation is performed"));
        assertFalse(normalized.contains("nullsafetyfingerprint"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("quality ranking"));
        assertTrue(source.contains("does not inspect raw server input"));
        assertTrue(source.contains("does not inspect raw request payloads"));
        assertTrue(source.contains("does not recompute scores"));
        assertTrue(source.contains("not production certification"));
        assertTrue(source.contains("not guaranteed replay"));
    }

    private static RoutingDecisionVectorResponse vector(CandidateDecisionVectorResponse... candidates) {
        List<CandidateDecisionVectorResponse> candidateList = List.of(candidates);
        CandidateDecisionVectorResponse selected = candidateList.stream()
                .filter(CandidateDecisionVectorResponse::selected)
                .findFirst()
                .orElse(null);
        return new RoutingDecisionVectorResponse(
                true,
                "/api/routing/compare",
                "not exposed",
                "TAIL_LATENCY_POWER_OF_TWO",
                selected == null ? null : selected.candidateId(),
                candidateList.size(),
                candidateList,
                selected,
                candidateList.stream().filter(candidate -> !candidate.selected()).toList(),
                List.of("healthState=true"),
                List.of("hidden routing internals not exposed"),
                "current calculator components only",
                List.of("selected-vs-alternative note"),
                "controlled lab evidence only",
                productionBoundary(),
                "exposed for current calculator components; hidden scoring is not inferred and exact production "
                        + "scoring is not claimed",
                "future/not implemented; read-only Decision Vector exposure does not execute replay",
                "future/not implemented; read-only Decision Vector exposure does not execute what-if experiments",
                "future/not implemented; this response is not persistent structured decision logging");
    }

    private static CandidateDecisionVectorResponse candidate(
            String candidateId,
            boolean selected,
            ScoreFactorContributionResponse... contributions) {
        return new CandidateDecisionVectorResponse(
                candidateId,
                selected,
                List.of("healthState=true"),
                List.of("hidden routing internals not exposed"),
                List.of(contributions),
                "candidate explanation",
                "current calculator components only",
                "controlled lab evidence only",
                productionBoundary());
    }

    private static ScoreFactorContributionResponse contribution(String factorName, Double value) {
        return new ScoreFactorContributionResponse(
                factorName,
                "raw",
                "weight",
                "WEAKENS_SELECTION",
                "contribution",
                value,
                value == null ? "NOT_EXPOSED" : "EXACT_FROM_CALCULATOR",
                "factor explanation",
                "boundary");
    }

    private static DominantFactorAnalysisResponse dominant(String status) {
        return new DominantFactorAnalysisResponse(
                true,
                "dominant source",
                status,
                "UNKNOWN".equals(status) ? List.of() : List.of(dominantCandidate()),
                "UNKNOWN".equals(status) ? null : dominantCandidate(),
                "dominant explanation",
                boundaryNote(),
                productionBoundary());
    }

    private static CandidateDominantFactorResponse dominantCandidate() {
        DominantFactorResponse factor = new DominantFactorResponse(
                "p95LatencyMillis",
                "WEAKENS_SELECTION",
                -2.0,
                2.0,
                "contribution",
                "explanation",
                "boundary");
        return new CandidateDominantFactorResponse(
                "selected-edge",
                true,
                true,
                1,
                List.of("p95LatencyMillis"),
                factor,
                factor,
                factor,
                "dominant candidate explanation",
                "dominant candidate boundary");
    }

    private static RoutingDecisionDeltaAnalysisResponse delta(String status) {
        return new RoutingDecisionDeltaAnalysisResponse(
                true,
                "delta source",
                status,
                new CandidateDecisionDeltaResponse(
                        "selected-edge",
                        "alt-edge",
                        10.0,
                        8.5,
                        1.5,
                        1.5,
                        1,
                        1,
                        1,
                        List.of("p95LatencyMillis"),
                        List.of(),
                        "comparison explanation"),
                List.of(new ScoreFactorDeltaResponse(
                        "p95LatencyMillis",
                        -2.0,
                        -5.0,
                        3.0,
                        3.0,
                        "WEAKENS_SELECTION",
                        "WEAKENS_SELECTION",
                        "delta explanation")),
                new ScoreFactorDeltaResponse(
                        "p95LatencyMillis",
                        -2.0,
                        -5.0,
                        3.0,
                        3.0,
                        "WEAKENS_SELECTION",
                        "WEAKENS_SELECTION",
                        "largest delta explanation"),
                "delta explanation",
                boundaryNote(),
                productionBoundary());
    }

    private static RoutingDecisionDeltaAnalysisResponse unknownDelta() {
        return new RoutingDecisionDeltaAnalysisResponse(
                true,
                "delta source",
                "UNKNOWN",
                null,
                List.of(),
                null,
                "delta unavailable",
                boundaryNote(),
                productionBoundary());
    }

    private static RoutingDecisionReplaySnapshotResponse snapshot(String status, String fingerprint) {
        return new RoutingDecisionReplaySnapshotResponse(
                true,
                "decision-replay-snapshot/v1",
                "snapshot source",
                status,
                fingerprint,
                "existing fingerprint algorithm",
                "UNKNOWN".equals(status) ? null : "selected-edge",
                "UNKNOWN".equals(status) ? List.of() : List.of("alt-edge", "selected-edge"),
                "UNKNOWN".equals(status) ? 0 : 2,
                "TAIL_LATENCY_POWER_OF_TWO",
                "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                status,
                status,
                "UNKNOWN".equals(status) ? null : "alt-edge",
                "UNKNOWN".equals(status) ? null : 1.5,
                "UNKNOWN".equals(status) ? null : "p95LatencyMillis",
                "snapshot explanation",
                boundaryNote(),
                productionBoundary());
    }

    private static RoutingDecisionReplayReconstructionTraceResponse trace(
            String status,
            String fingerprint,
            String snapshotFingerprint) {
        Map<String, Double> scores = new LinkedHashMap<>();
        if (!"UNKNOWN".equals(status)) {
            scores.put("selected-edge", 10.0);
            scores.put("alt-edge", 8.5);
        }
        return new RoutingDecisionReplayReconstructionTraceResponse(
                true,
                "decision-replay-reconstruction-trace/v1",
                "trace source",
                status,
                fingerprint,
                "existing fingerprint algorithm",
                snapshotFingerprint,
                "UNKNOWN".equals(status) ? null : "selected-edge",
                "UNKNOWN".equals(status) ? List.of() : List.of("alt-edge", "selected-edge"),
                "UNKNOWN".equals(status) ? 0 : 2,
                scores,
                "TAIL_LATENCY_POWER_OF_TWO",
                "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                status,
                status,
                status,
                "UNKNOWN".equals(status) ? null : "alt-edge",
                "UNKNOWN".equals(status) ? null : 1.5,
                "UNKNOWN".equals(status) ? null : "p95LatencyMillis",
                "UNKNOWN".equals(status)
                        ? List.of()
                        : List.of(new DecisionReplayReconstructionStepResponse(
                                "candidate-set-observed",
                                status,
                                "decisionReplayReconstructionTrace.candidateIdsConsidered",
                                "step explanation",
                                null)),
                "trace explanation",
                boundaryNote(),
                productionBoundary());
    }

    private static RoutingDecisionReplayCapsuleResponse capsule(
            String status,
            String fingerprint,
            String snapshotFingerprint,
            String traceFingerprint) {
        return new RoutingDecisionReplayCapsuleResponse(
                true,
                "decision-replay-capsule/v1",
                "capsule source",
                status,
                fingerprint,
                "existing fingerprint algorithm",
                "UNKNOWN".equals(status) ? null : "selected-edge",
                "UNKNOWN".equals(status) ? List.of() : List.of("alt-edge", "selected-edge"),
                "UNKNOWN".equals(status) ? 0 : 2,
                "UNKNOWN".equals(status) ? null : "alt-edge",
                "UNKNOWN".equals(status) ? null : 1.5,
                "UNKNOWN".equals(status) ? null : "p95LatencyMillis",
                snapshotFingerprint,
                traceFingerprint,
                "TAIL_LATENCY_POWER_OF_TWO",
                "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                status,
                status,
                status,
                status,
                "UNKNOWN".equals(status) ? List.of() : List.of("candidate-set-observed"),
                "UNKNOWN".equals(status)
                        ? List.of()
                        : List.of(new DecisionReplayCapsuleCandidateEvidenceResponse(
                                "selected-edge",
                                true,
                                10.0,
                                List.of("p95LatencyMillis"),
                                1,
                                List.of("p95LatencyMillis"),
                                status)),
                "UNKNOWN".equals(status)
                        ? List.of()
                        : List.of(new DecisionReplayCapsuleFactorEvidenceResponse(
                                "p95LatencyMillis",
                                true,
                                true,
                                -2.0,
                                -5.0,
                                3.0,
                                status)),
                "capsule explanation",
                boundaryNote(),
                productionBoundary());
    }

    private static RoutingDecisionReplayReadinessChecklistResponse checklist(
            String status,
            String snapshotFingerprint,
            String traceFingerprint,
            String capsuleFingerprint) {
        return new RoutingDecisionReplayReadinessChecklistResponse(
                true,
                "decision-replay-readiness-checklist/v1",
                "checklist source",
                status,
                "TAIL_LATENCY_POWER_OF_TWO",
                "UNKNOWN".equals(status) ? null : "selected-edge",
                "UNKNOWN".equals(status) ? 0 : 2,
                snapshotFingerprint,
                traceFingerprint,
                capsuleFingerprint,
                "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                status,
                status,
                status,
                status,
                status,
                "UNKNOWN".equals(status) ? 0 : 9,
                0,
                "UNKNOWN".equals(status) ? 9 : 0,
                "UNKNOWN".equals(status) ? List.of() : List.of(new DecisionReplayReadinessChecklistItemResponse(
                        "decision-vector-evidence",
                        "Decision Vector evidence",
                        "AVAILABLE",
                        "decisionVector",
                        "item explanation",
                        null)),
                "checklist explanation",
                boundaryNote(),
                productionBoundary());
    }

    private static RoutingDecisionReplayEvidenceSourceMapResponse sourceMap(
            String status,
            String snapshotFingerprint,
            String traceFingerprint,
            String capsuleFingerprint) {
        return new RoutingDecisionReplayEvidenceSourceMapResponse(
                true,
                "decision-replay-evidence-source-map/v1",
                "source-map source",
                status,
                "TAIL_LATENCY_POWER_OF_TWO",
                "UNKNOWN".equals(status) ? null : "selected-edge",
                "UNKNOWN".equals(status) ? 0 : 2,
                snapshotFingerprint,
                traceFingerprint,
                capsuleFingerprint,
                "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                status,
                status,
                status,
                status,
                status,
                status,
                "UNKNOWN".equals(status) ? List.of() : List.of(new DecisionReplayEvidenceSourceMapEntryResponse(
                        "read-only-boundary-source",
                        "Read-only boundary source",
                        "AVAILABLE",
                        "decisionReplayEvidenceSourceMap.boundaryNote",
                        List.of(),
                        null,
                        "source map evidence summary",
                        boundaryNote())),
                "source map explanation",
                boundaryNote(),
                productionBoundary());
    }

    private static RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary(String status) {
        return new RoutingDecisionReplayEvidenceBoundarySummaryResponse(
                true,
                "decision-replay-evidence-boundary-summary/v1",
                "boundary-summary source",
                status,
                "TAIL_LATENCY_POWER_OF_TWO",
                "UNKNOWN".equals(status) ? null : "selected-edge",
                "UNKNOWN".equals(status) ? 0 : 2,
                "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                status,
                status,
                status,
                status,
                status,
                status,
                status,
                "UNKNOWN".equals(status) ? List.of() : List.of(new DecisionReplayEvidenceBoundarySummaryItemResponse(
                        "lab-only-boundary",
                        "Lab-only boundary",
                        "AVAILABLE",
                        "decisionVector.labProofBoundary",
                        List.of("decisionVector.labProofBoundary"),
                        "boundary summary evidence",
                        boundaryNote())),
                "boundary summary explanation",
                boundaryNote(),
                productionBoundary());
    }

    private static RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory(String status) {
        return new RoutingDecisionReplayEvidenceFieldInventoryResponse(
                true,
                "decision-replay-evidence-field-inventory/v1",
                "field-inventory source",
                status,
                "TAIL_LATENCY_POWER_OF_TWO",
                "UNKNOWN".equals(status) ? null : "selected-edge",
                "UNKNOWN".equals(status) ? 0 : 2,
                "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                status,
                status,
                status,
                status,
                status,
                status,
                status,
                status,
                "UNKNOWN".equals(status) ? 0 : 10,
                0,
                "UNKNOWN".equals(status) ? 10 : 0,
                "UNKNOWN".equals(status) ? unknownInventoryEntries() : availableInventoryEntries(),
                "field inventory explanation",
                boundaryNote(),
                productionBoundary());
    }

    private static RoutingDecisionReplayEvidenceFieldInventoryResponse partialFieldInventory() {
        return new RoutingDecisionReplayEvidenceFieldInventoryResponse(
                true,
                "decision-replay-evidence-field-inventory/v1",
                "field-inventory source",
                "PARTIAL",
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                1,
                "AVAILABLE",
                "AVAILABLE",
                "UNKNOWN",
                "PARTIAL",
                "UNKNOWN",
                "UNKNOWN",
                "UNKNOWN",
                "UNKNOWN",
                "UNKNOWN",
                2,
                1,
                9,
                List.of(
                        inventoryEntry("decision-vector-fields", "AVAILABLE",
                                List.of("decisionVector.candidateSummaries"),
                                List.of()),
                        inventoryEntry("replay-capsule-fields", "UNKNOWN",
                                List.of(),
                                List.of("decisionReplayCapsule.candidateEvidence",
                                        "decisionReplayCapsule.factorEvidence"))),
                "field inventory explanation",
                boundaryNote(),
                productionBoundary());
    }

    private static List<DecisionReplayEvidenceFieldInventoryEntryResponse> availableInventoryEntries() {
        return List.of(
                inventoryEntry("decision-vector-fields", "AVAILABLE",
                        List.of("decisionVector.candidateSummaries"),
                        List.of()),
                inventoryEntry("replay-capsule-fields", "AVAILABLE",
                        List.of("decisionReplayCapsule.candidateEvidence", "decisionReplayCapsule.factorEvidence"),
                        List.of()),
                inventoryEntry("production-not-proven-boundary-fields", "AVAILABLE",
                        List.of("decisionReplayEvidenceBoundarySummary.productionNotProvenBoundary"),
                        List.of()));
    }

    private static List<DecisionReplayEvidenceFieldInventoryEntryResponse> unknownInventoryEntries() {
        return List.of(
                inventoryEntry("decision-vector-fields", "UNKNOWN",
                        List.of(),
                        List.of("decisionVector.candidateSummaries")),
                inventoryEntry("replay-capsule-fields", "UNKNOWN",
                        List.of(),
                        List.of("decisionReplayCapsule.candidateEvidence",
                                "decisionReplayCapsule.factorEvidence")),
                inventoryEntry("production-not-proven-boundary-fields", "AVAILABLE",
                        List.of("decisionReplayEvidenceBoundarySummary.productionNotProvenBoundary"),
                        List.of()));
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse inventoryEntry(
            String inventoryId,
            String status,
            List<String> observed,
            List<String> missing) {
        return new DecisionReplayEvidenceFieldInventoryEntryResponse(
                inventoryId,
                inventoryId,
                status,
                inventoryId,
                observed,
                missing,
                observed.size(),
                missing.size(),
                "inventory evidence",
                boundaryNote());
    }

    private static String boundaryNote() {
        return "read-only lab boundary; does not execute replay; does not perform what-if mutation; does not "
                + "persist audit logs; does not recompute scores; does not retune weights; does not change "
                + "routing behavior; does not add telemetry; does not add external calls; does not add "
                + "upload/share/download flows; does not add server-side export/PDF/ZIP generation; does not "
                + "generate fingerprints or add a new fingerprint";
    }

    private static String productionBoundary() {
        return "not production certification; not live-cloud proof; not real-tenant proof; not SLA/SLO proof; "
                + "not registry publication proof; not signing proof; not governance application proof; not exact "
                + "production scoring proof; not cryptographic production proof; not guaranteed replay; not "
                + "production traffic validation";
    }
}
