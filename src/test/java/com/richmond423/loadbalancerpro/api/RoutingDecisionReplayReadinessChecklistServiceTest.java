package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RoutingDecisionReplayReadinessChecklistServiceTest {
    private final RoutingDecisionReplaySnapshotService snapshotService = new RoutingDecisionReplaySnapshotService();
    private final RoutingDecisionReplayReconstructionTraceService traceService =
            new RoutingDecisionReplayReconstructionTraceService();
    private final RoutingDecisionReplayCapsuleService capsuleService = new RoutingDecisionReplayCapsuleService();
    private final RoutingDecisionReplayReadinessChecklistService service =
            new RoutingDecisionReplayReadinessChecklistService();

    @Test
    void checklistItemsAreDeterministicForEquivalentAlreadyBuiltEvidence() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true,
                        contribution("p95LatencyMillis", -2.0),
                        contribution("queueDepth", -1.0)),
                candidate("alt-z", false,
                        contribution("p95LatencyMillis", -5.0),
                        contribution("queueDepth", -4.0)),
                candidate("alt-a", false,
                        contribution("p95LatencyMillis", -3.0),
                        contribution("queueDepth", -2.0)));
        DominantFactorAnalysisResponse dominant = dominant("AVAILABLE",
                candidateDominant("selected-edge", true, "p95LatencyMillis"),
                candidateDominant("alt-a", false, "queueDepth"),
                candidateDominant("alt-z", false, "queueDepth"));
        RoutingDecisionDeltaAnalysisResponse delta = delta(
                "AVAILABLE",
                "selected-edge",
                "alt-a",
                1.5,
                factorDelta("p95LatencyMillis", -2.0, -3.0, 1.0),
                factorDelta("queueDepth", -1.0, -2.0, 1.0));
        RoutingDecisionReplaySnapshotResponse snapshot = snapshotService.snapshot(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("selected-edge", "alt-z", "alt-a"),
                vector,
                dominant,
                delta);
        RoutingDecisionReplayReconstructionTraceResponse trace = traceService.trace(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("alt-z", "selected-edge", "alt-a"),
                scores("alt-z", 8.0, "selected-edge", 10.0, "alt-a", 8.5),
                vector,
                dominant,
                delta,
                snapshot);
        RoutingDecisionReplayCapsuleResponse capsule = capsuleService.capsule(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("selected-edge", "alt-z", "alt-a"),
                scores("selected-edge", 10.0, "alt-z", 8.0, "alt-a", 8.5),
                vector,
                dominant,
                delta,
                snapshot,
                trace);

        RoutingDecisionReplayReadinessChecklistResponse first = service.checklist(
                vector, dominant, delta, snapshot, trace, capsule);
        RoutingDecisionReplayReadinessChecklistResponse second = service.checklist(
                vector, dominant, delta, snapshot, trace, capsule);

        assertEquals(first, second);
        assertEquals("AVAILABLE", first.status());
        assertEquals("decision-replay-readiness-checklist/v1", first.checklistSchemaVersion());
        assertEquals("selected-edge", first.selectedCandidateId());
        assertEquals(3, first.candidateCount());
        assertEquals(snapshot.snapshotFingerprint(), first.linkedReplaySnapshotFingerprint());
        assertEquals(trace.traceFingerprint(), first.linkedReconstructionTraceFingerprint());
        assertEquals(capsule.capsuleFingerprint(), first.linkedReplayCapsuleFingerprint());
        assertEquals(9, first.availableItemCount());
        assertEquals(0, first.partialItemCount());
        assertEquals(0, first.unknownItemCount());
        assertEquals(List.of(
                        "decision-vector-evidence",
                        "dominant-factor-evidence",
                        "decision-delta-evidence",
                        "replay-snapshot-evidence",
                        "reconstruction-trace-evidence",
                        "replay-capsule-evidence",
                        "candidate-evidence",
                        "factor-evidence",
                        "read-only-boundary-evidence"),
                first.checklistItems().stream()
                        .map(DecisionReplayReadinessChecklistItemResponse::itemId)
                        .toList());
        assertTrue(first.explanation().contains("derived from already-built lab compare evidence only"));
    }

    @Test
    void missingEvidenceReturnsUnknownWithoutInventedReplayReadinessClaims() {
        DominantFactorAnalysisResponse dominant = dominant("UNKNOWN");
        RoutingDecisionDeltaAnalysisResponse delta = unknownDelta();
        RoutingDecisionReplaySnapshotResponse snapshot = snapshotService.snapshot(
                "TAIL_LATENCY_POWER_OF_TWO",
                null,
                List.of(),
                null,
                dominant,
                delta);
        RoutingDecisionReplayReconstructionTraceResponse trace = traceService.trace(
                "TAIL_LATENCY_POWER_OF_TWO",
                null,
                List.of(),
                Map.of(),
                null,
                dominant,
                delta,
                snapshot);
        RoutingDecisionReplayCapsuleResponse capsule = capsuleService.capsule(
                "TAIL_LATENCY_POWER_OF_TWO",
                null,
                List.of(),
                Map.of(),
                null,
                dominant,
                delta,
                snapshot,
                trace);

        RoutingDecisionReplayReadinessChecklistResponse checklist = service.checklist(
                null, dominant, delta, snapshot, trace, capsule);

        assertEquals("UNKNOWN", checklist.status());
        assertNull(checklist.linkedReplaySnapshotFingerprint());
        assertNull(checklist.linkedReconstructionTraceFingerprint());
        assertNull(checklist.linkedReplayCapsuleFingerprint());
        assertEquals("UNKNOWN", checklist.decisionVectorStatus());
        assertEquals("UNKNOWN", checklist.decisionReplayCapsuleStatus());
        assertNull(checklist.selectedCandidateId());
        assertEquals(0, checklist.candidateCount());
        assertTrue(checklist.checklistItems().stream()
                .anyMatch(item -> "candidate-evidence".equals(item.itemId())
                        && "UNKNOWN".equals(item.status())));
        assertTrue(checklist.explanation().contains("No replay execution"));
        assertTrue(checklist.explanation().contains("selected candidate"));
        assertTrue(checklist.explanation().contains("is invented"));
    }

    @Test
    void partialAndNonFiniteEvidenceStaysPartialWithoutLeakingNonFiniteValues() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true,
                        contribution("finiteShared", 4.0),
                        contribution("selectedNaN", Double.NaN)),
                candidate("alt-edge", false,
                        contribution("finiteShared", 1.0),
                        contribution("altInfinity", Double.POSITIVE_INFINITY)));
        DominantFactorAnalysisResponse dominant = dominant("PARTIAL",
                candidateDominant("selected-edge", true, "finiteShared"));
        RoutingDecisionDeltaAnalysisResponse delta = delta(
                "PARTIAL",
                "selected-edge",
                "alt-edge",
                Double.NaN,
                factorDelta("finiteShared", 4.0, 1.0, 3.0));
        RoutingDecisionReplaySnapshotResponse snapshot = snapshotService.snapshot(
                "WEIGHTED_LEAST_CONNECTIONS",
                "selected-edge",
                List.of("selected-edge", "alt-edge"),
                vector,
                dominant,
                delta);
        RoutingDecisionReplayReconstructionTraceResponse trace = traceService.trace(
                "WEIGHTED_LEAST_CONNECTIONS",
                "selected-edge",
                List.of("selected-edge", "alt-edge"),
                scores("selected-edge", 10.0, "alt-edge", Double.POSITIVE_INFINITY),
                vector,
                dominant,
                delta,
                snapshot);
        RoutingDecisionReplayCapsuleResponse capsule = capsuleService.capsule(
                "WEIGHTED_LEAST_CONNECTIONS",
                "selected-edge",
                List.of("selected-edge", "alt-edge"),
                scores("selected-edge", 10.0, "alt-edge", Double.POSITIVE_INFINITY),
                vector,
                dominant,
                delta,
                snapshot,
                trace);

        RoutingDecisionReplayReadinessChecklistResponse checklist = service.checklist(
                vector, dominant, delta, snapshot, trace, capsule);

        assertEquals("PARTIAL", checklist.status());
        assertEquals(snapshot.snapshotFingerprint(), checklist.linkedReplaySnapshotFingerprint());
        assertEquals(trace.traceFingerprint(), checklist.linkedReconstructionTraceFingerprint());
        assertEquals(capsule.capsuleFingerprint(), checklist.linkedReplayCapsuleFingerprint());
        assertTrue(checklist.checklistItems().stream()
                .anyMatch(item -> "candidate-evidence".equals(item.itemId())
                        && "PARTIAL".equals(item.status())));
        assertTrue(checklist.checklistItems().stream()
                .anyMatch(item -> "dominant-factor-evidence".equals(item.itemId())
                        && "PARTIAL".equals(item.status())));
        assertFalse(checklist.toString().contains("Infinity"));
        assertFalse(checklist.toString().contains("NaN"));
    }

    @Test
    void checklistDoesNotUseScoringReplayPersistenceExportOrEnvironmentInputs() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayReadinessChecklistService.java"),
                StandardCharsets.UTF_8);
        String normalized = source.toLowerCase(Locale.ROOT);

        assertFalse(source.contains("ServerScoreCalculator"));
        assertFalse(source.contains("ServerStateVector"));
        assertFalse(source.contains("scoreCalculator"));
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
        List<CandidateDecisionVectorResponse> nonSelected = candidateList.stream()
                .filter(candidate -> !candidate.selected())
                .toList();
        return new RoutingDecisionVectorResponse(
                true,
                "/api/routing/compare",
                "not exposed",
                "TAIL_LATENCY_POWER_OF_TWO",
                selected == null ? null : selected.candidateId(),
                candidateList.size(),
                candidateList,
                selected,
                nonSelected,
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

    private static DominantFactorAnalysisResponse dominant(
            String status,
            CandidateDominantFactorResponse... candidates) {
        return new DominantFactorAnalysisResponse(
                true,
                "dominant source",
                status,
                List.of(candidates),
                candidates.length == 0 ? null : candidates[0],
                "dominant explanation",
                "dominant boundary",
                "no production certification");
    }

    private static CandidateDominantFactorResponse candidateDominant(
            String candidateId,
            boolean selected,
            String factorName) {
        DominantFactorResponse factor = new DominantFactorResponse(
                factorName,
                "SUPPORTING",
                1.0,
                1.0,
                "contribution",
                "dominant explanation",
                "boundary");
        return new CandidateDominantFactorResponse(
                candidateId,
                selected,
                true,
                1,
                List.of(factorName),
                factor,
                null,
                factor,
                "candidate dominant explanation",
                "boundary");
    }

    private static RoutingDecisionDeltaAnalysisResponse delta(
            String status,
            String selectedCandidateId,
            String closestAlternativeCandidateId,
            Double finalScoreGap,
            ScoreFactorDeltaResponse... factorDeltas) {
        List<ScoreFactorDeltaResponse> deltas = List.of(factorDeltas);
        return new RoutingDecisionDeltaAnalysisResponse(
                true,
                "delta source",
                status,
                new CandidateDecisionDeltaResponse(
                        selectedCandidateId,
                        closestAlternativeCandidateId,
                        10.0,
                        finalScoreGap == null ? 9.0 : 10.0 - finalScoreGap,
                        finalScoreGap,
                        finalScoreGap == null || !Double.isFinite(finalScoreGap) ? null : Math.abs(finalScoreGap),
                        1,
                        1,
                        deltas.size(),
                        deltas.stream().map(ScoreFactorDeltaResponse::factorName).sorted().toList(),
                        List.of(),
                        "comparison explanation"),
                deltas,
                deltas.isEmpty() ? null : deltas.get(0),
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

    private static ScoreFactorDeltaResponse factorDelta(
            String factorName,
            Double selectedContribution,
            Double alternativeContribution,
            Double contributionDelta) {
        return new ScoreFactorDeltaResponse(
                factorName,
                selectedContribution,
                alternativeContribution,
                contributionDelta,
                contributionDelta == null || !Double.isFinite(contributionDelta)
                        ? null
                        : Math.abs(contributionDelta),
                "WEAKENS_SELECTION",
                "WEAKENS_SELECTION",
                "factor delta explanation");
    }

    private static Map<String, Double> scores(String firstId, Double firstScore, String secondId, Double secondScore) {
        LinkedHashMap<String, Double> scores = new LinkedHashMap<>();
        scores.put(firstId, firstScore);
        scores.put(secondId, secondScore);
        return scores;
    }

    private static Map<String, Double> scores(
            String firstId,
            Double firstScore,
            String secondId,
            Double secondScore,
            String thirdId,
            Double thirdScore) {
        LinkedHashMap<String, Double> scores = new LinkedHashMap<>();
        scores.put(firstId, firstScore);
        scores.put(secondId, secondScore);
        scores.put(thirdId, thirdScore);
        return scores;
    }
}
