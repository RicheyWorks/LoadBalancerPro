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

class RoutingDecisionReplayEvidenceSourceMapServiceTest {
    private final RoutingDecisionReplayEvidenceSourceMapService service =
            new RoutingDecisionReplayEvidenceSourceMapService();

    @Test
    void sourceMapEntriesAreDeterministicForEquivalentAlreadyBuiltEvidence() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true, contribution("p95LatencyMillis", -2.0)),
                candidate("alt-edge", false, contribution("p95LatencyMillis", -5.0)));
        DominantFactorAnalysisResponse dominant = dominant("AVAILABLE");
        RoutingDecisionDeltaAnalysisResponse delta = delta("PARTIAL");
        RoutingDecisionReplaySnapshotResponse snapshot = snapshot("PARTIAL", "snapshot-fingerprint");
        RoutingDecisionReplayReconstructionTraceResponse trace = trace("PARTIAL", "trace-fingerprint",
                "snapshot-fingerprint");
        RoutingDecisionReplayCapsuleResponse capsule = capsule("PARTIAL", "capsule-fingerprint",
                "snapshot-fingerprint", "trace-fingerprint");
        RoutingDecisionReplayReadinessChecklistResponse checklist = checklist("PARTIAL",
                "snapshot-fingerprint", "trace-fingerprint", "capsule-fingerprint");

        RoutingDecisionReplayEvidenceSourceMapResponse first = service.sourceMap(
                "TAIL_LATENCY_POWER_OF_TWO", vector, dominant, delta, snapshot, trace, capsule, checklist);
        RoutingDecisionReplayEvidenceSourceMapResponse second = service.sourceMap(
                "TAIL_LATENCY_POWER_OF_TWO", vector, dominant, delta, snapshot, trace, capsule, checklist);

        assertEquals(first, second);
        assertTrue(first.readOnly());
        assertEquals("decision-replay-evidence-source-map/v1", first.sourceMapSchemaVersion());
        assertEquals("PARTIAL", first.status());
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", first.strategyId());
        assertEquals("selected-edge", first.selectedCandidateId());
        assertEquals(2, first.candidateCount());
        assertEquals("snapshot-fingerprint", first.linkedReplaySnapshotFingerprint());
        assertEquals("trace-fingerprint", first.linkedReconstructionTraceFingerprint());
        assertEquals("capsule-fingerprint", first.linkedReplayCapsuleFingerprint());
        assertEquals("AVAILABLE", first.decisionVectorStatus());
        assertEquals("AVAILABLE", first.dominantFactorAnalysisStatus());
        assertEquals("PARTIAL", first.decisionDeltaAnalysisStatus());
        assertEquals("PARTIAL", first.decisionReplaySnapshotStatus());
        assertEquals("PARTIAL", first.decisionReplayReconstructionTraceStatus());
        assertEquals("PARTIAL", first.decisionReplayCapsuleStatus());
        assertEquals("PARTIAL", first.decisionReplayReadinessChecklistStatus());
        assertEquals(List.of(
                        "decision-vector-source",
                        "dominant-factor-analysis-source",
                        "decision-delta-analysis-source",
                        "replay-snapshot-source",
                        "reconstruction-trace-source",
                        "replay-capsule-source",
                        "readiness-checklist-source",
                        "linked-fingerprint-source",
                        "read-only-boundary-source"),
                first.sourceMapEntries().stream()
                        .map(DecisionReplayEvidenceSourceMapEntryResponse::sourceId)
                        .toList());
        assertEquals("decisionVector", first.sourceMapEntries().get(0).sourceFieldPath());
        assertTrue(first.sourceMapEntries().get(0).downstreamEvidenceFieldPaths()
                .contains("decisionReplayCapsule.decisionVectorStatus"));
        assertEquals("snapshot-fingerprint", first.sourceMapEntries().get(3).linkedFingerprint());
        assertEquals("trace-fingerprint", first.sourceMapEntries().get(4).linkedFingerprint());
        assertEquals("capsule-fingerprint", first.sourceMapEntries().get(5).linkedFingerprint());
        assertEquals("snapshot-fingerprint", first.sourceMapEntries().get(7).linkedFingerprint());
        assertTrue(first.sourceMapEntries().get(7).downstreamEvidenceFieldPaths()
                .contains("decisionReplayEvidenceSourceMap.linkedReplayCapsuleFingerprint"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.sourceMapEntries().add(first.sourceMapEntries().get(0)));
        assertThrows(UnsupportedOperationException.class,
                () -> first.sourceMapEntries().get(0).downstreamEvidenceFieldPaths().add("mutated"));
        assertTrue(first.explanation().contains("derived from already-built lab compare evidence only"));
        assertTrue(first.boundaryNote().contains("does not execute replay"));
        assertTrue(first.boundaryNote().contains("generate fingerprints"));
    }

    @Test
    void missingEvidenceReturnsUnknownWithoutInventingSourceMapEvidence() {
        DominantFactorAnalysisResponse dominant = dominant("UNKNOWN");
        RoutingDecisionDeltaAnalysisResponse delta = unknownDelta();
        RoutingDecisionReplaySnapshotResponse snapshot = snapshot("UNKNOWN", null);
        RoutingDecisionReplayReconstructionTraceResponse trace = trace("UNKNOWN", null, null);
        RoutingDecisionReplayCapsuleResponse capsule = capsule("UNKNOWN", null, null, null);
        RoutingDecisionReplayReadinessChecklistResponse checklist = checklist("UNKNOWN", null, null, null);

        RoutingDecisionReplayEvidenceSourceMapResponse sourceMap = service.sourceMap(
                "TAIL_LATENCY_POWER_OF_TWO", null, dominant, delta, snapshot, trace, capsule, checklist);

        assertEquals("UNKNOWN", sourceMap.status());
        assertNull(sourceMap.selectedCandidateId());
        assertEquals(0, sourceMap.candidateCount());
        assertNull(sourceMap.linkedReplaySnapshotFingerprint());
        assertNull(sourceMap.linkedReconstructionTraceFingerprint());
        assertNull(sourceMap.linkedReplayCapsuleFingerprint());
        assertEquals("UNKNOWN", sourceMap.decisionVectorStatus());
        assertEquals("UNKNOWN", sourceMap.decisionReplaySnapshotStatus());
        assertEquals("UNKNOWN", sourceMap.decisionReplayReconstructionTraceStatus());
        assertEquals("UNKNOWN", sourceMap.decisionReplayCapsuleStatus());
        assertEquals("UNKNOWN", sourceMap.decisionReplayReadinessChecklistStatus());
        assertEquals("UNKNOWN", sourceMap.sourceMapEntries().get(0).status());
        assertEquals("UNKNOWN", sourceMap.sourceMapEntries().get(3).status());
        assertEquals("UNKNOWN", sourceMap.sourceMapEntries().get(7).status());
        assertEquals("AVAILABLE", sourceMap.sourceMapEntries().get(8).status());
        assertTrue(sourceMap.explanation().contains("No replay execution"));
        assertTrue(sourceMap.explanation().contains("is invented"));
    }

    @Test
    void linkedFingerprintsAreNotIncludedWhenSourceStatusIsUnknown() {
        RoutingDecisionReplaySnapshotResponse snapshot = snapshot("UNKNOWN", "snapshot-fingerprint");
        RoutingDecisionReplayReconstructionTraceResponse trace = trace("UNKNOWN", "trace-fingerprint",
                "snapshot-fingerprint");
        RoutingDecisionReplayCapsuleResponse capsule = capsule("UNKNOWN", "capsule-fingerprint",
                "snapshot-fingerprint", "trace-fingerprint");
        RoutingDecisionReplayReadinessChecklistResponse checklist = checklist("UNKNOWN",
                "snapshot-fingerprint", "trace-fingerprint", "capsule-fingerprint");

        RoutingDecisionReplayEvidenceSourceMapResponse sourceMap = service.sourceMap(
                "TAIL_LATENCY_POWER_OF_TWO", null, dominant("UNKNOWN"), unknownDelta(),
                snapshot, trace, capsule, checklist);

        assertNull(sourceMap.linkedReplaySnapshotFingerprint());
        assertNull(sourceMap.linkedReconstructionTraceFingerprint());
        assertNull(sourceMap.linkedReplayCapsuleFingerprint());
        assertNull(sourceMap.sourceMapEntries().get(3).linkedFingerprint());
        assertNull(sourceMap.sourceMapEntries().get(4).linkedFingerprint());
        assertNull(sourceMap.sourceMapEntries().get(5).linkedFingerprint());
        assertNull(sourceMap.sourceMapEntries().get(7).linkedFingerprint());
    }

    @Test
    void sourceMapDoesNotUseScoringFingerprintPersistenceExportOrEnvironmentInputs() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayEvidenceSourceMapService.java"),
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
        assertFalse(normalized.contains("executed replay"));
        assertFalse(normalized.contains("production certification is proven"));
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
                "no production certification",
                "exposed for current calculator components",
                "future/not implemented",
                "future/not implemented",
                "future/not implemented");
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
                "no production certification");
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
                List.of(),
                null,
                "dominant explanation",
                "dominant boundary",
                "no production certification");
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
                List.of(),
                null,
                "delta explanation",
                "delta boundary",
                "no production certification");
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
                "delta boundary",
                "no production certification");
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
                "snapshot boundary",
                "no production certification");
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
                "trace boundary",
                "no production certification");
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
                "capsule boundary",
                "no production certification");
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
                "UNKNOWN".equals(status) ? 0 : 4,
                "PARTIAL".equals(status) ? 4 : 0,
                "UNKNOWN".equals(status) ? 9 : 1,
                List.of(new DecisionReplayReadinessChecklistItemResponse(
                        "decision-vector-evidence",
                        "Decision Vector evidence",
                        "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                        "decisionVector",
                        "item explanation",
                        null)),
                "checklist explanation",
                "checklist boundary",
                "no production certification");
    }
}
