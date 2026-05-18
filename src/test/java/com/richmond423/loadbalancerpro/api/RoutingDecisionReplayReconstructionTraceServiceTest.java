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

class RoutingDecisionReplayReconstructionTraceServiceTest {
    private final RoutingDecisionReplaySnapshotService snapshotService = new RoutingDecisionReplaySnapshotService();
    private final RoutingDecisionReplayReconstructionTraceService service =
            new RoutingDecisionReplayReconstructionTraceService();

    @Test
    void traceFingerprintStepsScoresAndCandidateOrderingAreDeterministic() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true, contribution("p95LatencyMillis", -2.0)),
                candidate("alt-z", false, contribution("p95LatencyMillis", -4.0)),
                candidate("alt-a", false, contribution("p95LatencyMillis", -3.0)));
        RoutingDecisionDeltaAnalysisResponse delta = delta("AVAILABLE", "selected-edge", "alt-a",
                1.5, factorDelta("p95LatencyMillis"));
        RoutingDecisionReplaySnapshotResponse snapshot = snapshotService.snapshot(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("selected-edge", "alt-z", "alt-a"),
                vector,
                dominant("AVAILABLE"),
                delta);

        RoutingDecisionReplayReconstructionTraceResponse first = service.trace(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("selected-edge", "alt-z", "alt-a"),
                scores("selected-edge", 10.0, "alt-z", 8.0, "alt-a", 8.5),
                vector,
                dominant("AVAILABLE"),
                delta,
                snapshot);
        RoutingDecisionReplayReconstructionTraceResponse second = service.trace(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("alt-z", "selected-edge", "alt-a"),
                scores("alt-z", 8.0, "selected-edge", 10.0, "alt-a", 8.5),
                vector,
                dominant("AVAILABLE"),
                delta,
                snapshot);

        assertEquals(first, second, "equivalent already-built evidence should produce identical trace output");
        assertEquals("AVAILABLE", first.status());
        assertEquals(List.of("alt-a", "alt-z", "selected-edge"), first.candidateIdsConsidered());
        assertEquals(List.of(
                        "candidate-set-observed",
                        "selected-candidate-observed",
                        "candidate-final-scores-observed",
                        "decision-vector-observed",
                        "candidate-factor-contributions-observed",
                        "dominant-factors-observed",
                        "closest-alternative-observed",
                        "selected-vs-alternative-delta-observed",
                        "replay-snapshot-fingerprint-observed"),
                first.reconstructionSteps().stream()
                        .map(DecisionReplayReconstructionStepResponse::stepId)
                        .toList());
        assertEquals("selected-edge", first.selectedCandidateId());
        assertEquals("alt-a", first.closestAlternativeCandidateId());
        assertEquals(1.5, first.finalScoreGap());
        assertEquals("p95LatencyMillis", first.largestDeltaFactorName());
        assertEquals(snapshot.snapshotFingerprint(), first.snapshotFingerprint());
        assertEquals(64, first.traceFingerprint().length());
        assertTrue(first.traceFingerprint().matches("[0-9a-f]{64}"));
        assertEquals(List.of("alt-a", "alt-z", "selected-edge"),
                first.candidateFinalScores().keySet().stream().toList());
        assertTrue(first.explanation().contains("derived from existing lab compare evidence only"));
    }

    @Test
    void missingEvidenceReturnsUnknownWithoutInventingDecisionDetails() {
        RoutingDecisionReplaySnapshotResponse snapshot = snapshotService.snapshot(
                "TAIL_LATENCY_POWER_OF_TWO",
                null,
                List.of(),
                null,
                dominant("UNKNOWN"),
                unknownDelta());

        RoutingDecisionReplayReconstructionTraceResponse trace = service.trace(
                "TAIL_LATENCY_POWER_OF_TWO",
                null,
                List.of(),
                Map.of(),
                null,
                dominant("UNKNOWN"),
                unknownDelta(),
                snapshot);

        assertEquals("UNKNOWN", trace.status());
        assertNull(trace.selectedCandidateId());
        assertTrue(trace.candidateIdsConsidered().isEmpty());
        assertTrue(trace.candidateFinalScores().isEmpty());
        assertNull(trace.closestAlternativeCandidateId());
        assertNull(trace.finalScoreGap());
        assertNull(trace.largestDeltaFactorName());
        assertTrue(trace.reconstructionSteps().stream()
                .allMatch(step -> "UNKNOWN".equals(step.status()) || "PARTIAL".equals(step.status())));
        assertTrue(trace.explanation().contains("No replay execution"));
        assertTrue(trace.explanation().contains("what-if mutation"));
        assertTrue(trace.explanation().contains("persisted trace"));
    }

    @Test
    void nonFiniteAndPartialEvidenceStaysPartialWithoutInventedValues() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true, contribution("p95LatencyMillis", -2.0)),
                candidate("alt-edge", false, nonFiniteContribution("p95LatencyMillis")));
        RoutingDecisionDeltaAnalysisResponse delta = delta("PARTIAL", "selected-edge", "alt-edge",
                Double.NaN, null);
        RoutingDecisionReplaySnapshotResponse snapshot = snapshotService.snapshot(
                "WEIGHTED_LEAST_CONNECTIONS",
                "selected-edge",
                List.of("selected-edge", "alt-edge"),
                vector,
                dominant("PARTIAL"),
                delta);

        RoutingDecisionReplayReconstructionTraceResponse trace = service.trace(
                "WEIGHTED_LEAST_CONNECTIONS",
                "selected-edge",
                List.of("selected-edge", "alt-edge"),
                scores("selected-edge", 10.0, "alt-edge", Double.POSITIVE_INFINITY),
                vector,
                dominant("PARTIAL"),
                delta,
                snapshot);

        assertEquals("PARTIAL", trace.status());
        assertEquals("selected-edge", trace.selectedCandidateId());
        assertEquals("alt-edge", trace.closestAlternativeCandidateId());
        assertNull(trace.finalScoreGap());
        assertNull(trace.largestDeltaFactorName());
        assertEquals(List.of("selected-edge"), trace.candidateFinalScores().keySet().stream().toList());
        assertTrue(trace.reconstructionSteps().stream()
                .anyMatch(step -> "candidate-final-scores-observed".equals(step.stepId())
                        && "PARTIAL".equals(step.status())));
        assertFalse(trace.toString().contains("Infinity"));
        assertFalse(trace.toString().contains("NaN"));
    }

    @Test
    void traceDoesNotUseEnvironmentSpecificScoringReplayOrPersistenceInputs() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayReconstructionTraceService.java"),
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
    }

    @Test
    void languageStaysBoundedToReadOnlyLabReconstructionEvidence() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true, contribution("queueDepth", -1.0)),
                candidate("alternative-edge", false, contribution("queueDepth", -3.0)));
        RoutingDecisionDeltaAnalysisResponse delta = delta("AVAILABLE", "selected-edge", "alternative-edge",
                1.0, factorDelta("queueDepth"));
        RoutingDecisionReplaySnapshotResponse snapshot = snapshotService.snapshot(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("selected-edge", "alternative-edge"),
                vector,
                dominant("AVAILABLE"),
                delta);

        String normalized = service.trace(
                        "TAIL_LATENCY_POWER_OF_TWO",
                        "selected-edge",
                        List.of("selected-edge", "alternative-edge"),
                        scores("selected-edge", 9.0, "alternative-edge", 8.0),
                        vector,
                        dominant("AVAILABLE"),
                        delta,
                        snapshot)
                .toString()
                .toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("read-only lab evidence"));
        assertTrue(normalized.contains("does not persist traces or audit logs"));
        assertTrue(normalized.contains("execute replay"));
        assertTrue(normalized.contains("perform what-if mutation"));
        assertTrue(normalized.contains("recompute scores"));
        assertTrue(normalized.contains("no production certification"));
        assertTrue(normalized.contains("cryptographic production proof"));
        assertTrue(normalized.contains("guaranteed replay"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("exact production decision"));
        assertFalse(normalized.contains("executed replay"));
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
            ScoreFactorContributionResponse contribution) {
        return new CandidateDecisionVectorResponse(
                candidateId,
                selected,
                List.of("healthState=true"),
                List.of("hidden routing internals not exposed"),
                List.of(contribution),
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
                "EXACT_FROM_CALCULATOR",
                "factor explanation",
                "boundary");
    }

    private static ScoreFactorContributionResponse nonFiniteContribution(String factorName) {
        return contribution(factorName, Double.NaN);
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

    private static RoutingDecisionDeltaAnalysisResponse delta(
            String status,
            String selectedCandidateId,
            String closestAlternativeCandidateId,
            Double finalScoreGap,
            ScoreFactorDeltaResponse largestFactorDelta) {
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
                        largestFactorDelta == null ? 0 : 1,
                        largestFactorDelta == null ? List.of() : List.of(largestFactorDelta.factorName()),
                        List.of(),
                        "comparison explanation"),
                largestFactorDelta == null ? List.of() : List.of(largestFactorDelta),
                largestFactorDelta,
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

    private static ScoreFactorDeltaResponse factorDelta(String factorName) {
        return new ScoreFactorDeltaResponse(
                factorName,
                2.0,
                1.0,
                1.0,
                1.0,
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
