package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class RoutingDecisionReplaySnapshotServiceTest {
    private final RoutingDecisionReplaySnapshotService service = new RoutingDecisionReplaySnapshotService();

    @Test
    void snapshotFingerprintAndCandidateOrderingAreDeterministic() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true),
                candidate("alt-z", false),
                candidate("alt-a", false));
        RoutingDecisionDeltaAnalysisResponse delta = delta("AVAILABLE", "selected-edge", "alt-a",
                -2.5, factorDelta("queueDepth"));

        RoutingDecisionReplaySnapshotResponse first = service.snapshot(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("selected-edge", "alt-z", "alt-a"),
                vector,
                dominant("AVAILABLE"),
                delta);
        RoutingDecisionReplaySnapshotResponse second = service.snapshot(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("alt-z", "selected-edge", "alt-a"),
                vector,
                dominant("AVAILABLE"),
                delta);

        assertEquals(first, second, "equivalent snapshot evidence should produce identical snapshot output");
        assertEquals("AVAILABLE", first.status());
        assertEquals(List.of("alt-a", "alt-z", "selected-edge"), first.candidateIdsConsidered());
        assertEquals("selected-edge", first.selectedCandidateId());
        assertEquals("alt-a", first.closestAlternativeCandidateId());
        assertEquals("queueDepth", first.largestDeltaFactorName());
        assertEquals(64, first.snapshotFingerprint().length());
        assertTrue(first.snapshotFingerprint().matches("[0-9a-f]{64}"));
        assertTrue(first.explanation().contains("derived from existing lab compare evidence only"));
    }

    @Test
    void missingSelectedOrDecisionVectorEvidenceReturnsUnknownWithoutInventingData() {
        RoutingDecisionReplaySnapshotResponse snapshot = service.snapshot(
                "TAIL_LATENCY_POWER_OF_TWO",
                null,
                List.of(),
                null,
                dominant("UNKNOWN"),
                unknownDelta());

        assertEquals("UNKNOWN", snapshot.status());
        assertNull(snapshot.selectedCandidateId());
        assertTrue(snapshot.candidateIdsConsidered().isEmpty());
        assertEquals(0, snapshot.candidateCount());
        assertNull(snapshot.closestAlternativeCandidateId());
        assertNull(snapshot.finalScoreGap());
        assertNull(snapshot.largestDeltaFactorName());
        assertTrue(snapshot.explanation().contains("No replay execution"));
    }

    @Test
    void partialAndNonFiniteDeltaDataRemainSafeWithoutInventedValues() {
        RoutingDecisionReplaySnapshotResponse snapshot = service.snapshot(
                "WEIGHTED_LEAST_CONNECTIONS",
                "selected-edge",
                List.of("selected-edge", "alternative-edge"),
                vector(candidate("selected-edge", true), candidate("alternative-edge", false)),
                dominant("AVAILABLE"),
                delta("PARTIAL", "selected-edge", "alternative-edge", Double.NaN, null));

        assertEquals("PARTIAL", snapshot.status());
        assertEquals("alternative-edge", snapshot.closestAlternativeCandidateId());
        assertNull(snapshot.finalScoreGap(), "non-finite score gaps must stay unknown");
        assertNull(snapshot.largestDeltaFactorName());
        assertFalse(snapshot.explanation().contains("NaN"));
    }

    @Test
    void closestAlternativeIsNotInventedWhenDeltaReferencesUnknownCandidate() {
        RoutingDecisionReplaySnapshotResponse snapshot = service.snapshot(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("selected-edge", "known-alt"),
                vector(candidate("selected-edge", true), candidate("known-alt", false)),
                dominant("AVAILABLE"),
                delta("AVAILABLE", "selected-edge", "missing-alt", 1.0, factorDelta("p95LatencyMillis")));

        assertEquals("AVAILABLE", snapshot.decisionDeltaAnalysisStatus());
        assertNull(snapshot.closestAlternativeCandidateId());
        assertEquals(1.0, snapshot.finalScoreGap());
        assertTrue(snapshot.candidateIdsConsidered().contains("known-alt"));
        assertFalse(snapshot.candidateIdsConsidered().contains("missing-alt"));
    }

    @Test
    void snapshotDoesNotUseEnvironmentSpecificOrScoringImplementationInputs() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplaySnapshotService.java"),
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
    }

    @Test
    void languageStaysBoundedToReadOnlyLabSnapshotEvidence() {
        String normalized = service.snapshot(
                        "TAIL_LATENCY_POWER_OF_TWO",
                        "selected-edge",
                        List.of("selected-edge", "alternative-edge"),
                        vector(candidate("selected-edge", true), candidate("alternative-edge", false)),
                        dominant("AVAILABLE"),
                        delta("AVAILABLE", "selected-edge", "alternative-edge", 1.0,
                                factorDelta("p95LatencyMillis")))
                .toString()
                .toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("read-only lab evidence"));
        assertTrue(normalized.contains("does not persist audit logs"));
        assertTrue(normalized.contains("execute replay"));
        assertTrue(normalized.contains("perform what-if mutation"));
        assertTrue(normalized.contains("recompute scores"));
        assertTrue(normalized.contains("no production certification"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("complete replay"));
        assertFalse(normalized.contains("exact production decision"));
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

    private static CandidateDecisionVectorResponse candidate(String candidateId, boolean selected) {
        return new CandidateDecisionVectorResponse(
                candidateId,
                selected,
                List.of("healthState=true"),
                List.of("hidden routing internals not exposed"),
                List.of(),
                "candidate explanation",
                "current calculator components only",
                "controlled lab evidence only",
                "no production certification");
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
}
