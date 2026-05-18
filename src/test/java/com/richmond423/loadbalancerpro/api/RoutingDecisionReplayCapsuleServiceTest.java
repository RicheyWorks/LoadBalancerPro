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

class RoutingDecisionReplayCapsuleServiceTest {
    private final RoutingDecisionReplaySnapshotService snapshotService = new RoutingDecisionReplaySnapshotService();
    private final RoutingDecisionReplayReconstructionTraceService traceService =
            new RoutingDecisionReplayReconstructionTraceService();
    private final RoutingDecisionReplayCapsuleService service = new RoutingDecisionReplayCapsuleService();

    @Test
    void capsuleFingerprintCandidatesFactorsAndLinkedFingerprintsAreDeterministic() {
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
                List.of("selected-edge", "alt-z", "alt-a"),
                scores("selected-edge", 10.0, "alt-z", 8.0, "alt-a", 8.5),
                vector,
                dominant,
                delta,
                snapshot);

        RoutingDecisionReplayCapsuleResponse first = service.capsule(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("selected-edge", "alt-z", "alt-a"),
                scores("selected-edge", 10.0, "alt-z", 8.0, "alt-a", 8.5),
                vector,
                dominant,
                delta,
                snapshot,
                trace);
        RoutingDecisionReplayCapsuleResponse second = service.capsule(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("alt-z", "selected-edge", "alt-a"),
                scores("alt-z", 8.0, "selected-edge", 10.0, "alt-a", 8.5),
                vector,
                dominant,
                delta,
                snapshot,
                trace);

        assertEquals(first, second, "equivalent already-built evidence should produce identical capsule output");
        assertEquals("AVAILABLE", first.status());
        assertEquals(List.of("alt-a", "alt-z", "selected-edge"), first.candidateIdsConsidered());
        assertEquals("selected-edge", first.selectedCandidateId());
        assertEquals("alt-a", first.closestAlternativeCandidateId());
        assertEquals(1.5, first.finalScoreGap());
        assertEquals("p95LatencyMillis", first.largestDeltaFactorName());
        assertEquals(snapshot.snapshotFingerprint(), first.linkedReplaySnapshotFingerprint());
        assertEquals(trace.traceFingerprint(), first.linkedReconstructionTraceFingerprint());
        assertEquals(64, first.capsuleFingerprint().length());
        assertTrue(first.capsuleFingerprint().matches("[0-9a-f]{64}"));
        assertEquals(List.of("alt-a", "alt-z", "selected-edge"),
                first.candidateEvidence().stream()
                        .map(DecisionReplayCapsuleCandidateEvidenceResponse::candidateId)
                        .toList());
        assertEquals(List.of("p95LatencyMillis", "queueDepth"),
                first.factorEvidence().stream()
                        .map(DecisionReplayCapsuleFactorEvidenceResponse::factorName)
                        .toList());
        assertTrue(first.explanation().contains("packaged from existing lab compare evidence only"));
    }

    @Test
    void missingEvidenceReturnsUnknownWithoutInventingDecisionDetails() {
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

        RoutingDecisionReplayCapsuleResponse capsule = service.capsule(
                "TAIL_LATENCY_POWER_OF_TWO",
                null,
                List.of(),
                Map.of(),
                null,
                dominant,
                delta,
                snapshot,
                trace);

        assertEquals("UNKNOWN", capsule.status());
        assertNull(capsule.selectedCandidateId());
        assertTrue(capsule.candidateIdsConsidered().isEmpty());
        assertTrue(capsule.candidateEvidence().isEmpty());
        assertTrue(capsule.factorEvidence().isEmpty());
        assertNull(capsule.closestAlternativeCandidateId());
        assertNull(capsule.finalScoreGap());
        assertNull(capsule.largestDeltaFactorName());
        assertTrue(capsule.explanation().contains("No replay execution"));
        assertTrue(capsule.explanation().contains("persisted capsule"));
    }

    @Test
    void nonFiniteAndPartialEvidenceStaysPartialWithoutInventedValues() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true,
                        contribution("finiteShared", 4.0),
                        contribution("selectedNaN", Double.NaN),
                        contribution("selectedOnly", 2.0)),
                candidate("alt-edge", false,
                        contribution("finiteShared", 1.0),
                        contribution("altInfinity", Double.POSITIVE_INFINITY),
                        contribution("altOnly", 3.0)));
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

        RoutingDecisionReplayCapsuleResponse capsule = service.capsule(
                "WEIGHTED_LEAST_CONNECTIONS",
                "selected-edge",
                List.of("selected-edge", "alt-edge"),
                scores("selected-edge", 10.0, "alt-edge", Double.POSITIVE_INFINITY),
                vector,
                dominant,
                delta,
                snapshot,
                trace);

        assertEquals("PARTIAL", capsule.status());
        assertEquals("alt-edge", capsule.closestAlternativeCandidateId());
        assertNull(capsule.finalScoreGap(), "non-finite score gap must stay unknown");
        assertTrue(capsule.factorEvidence().stream()
                .anyMatch(factor -> "finiteShared".equals(factor.factorName())
                        && "AVAILABLE".equals(factor.status())));
        assertTrue(capsule.factorEvidence().stream()
                .anyMatch(factor -> "selectedOnly".equals(factor.factorName())
                        && "PARTIAL".equals(factor.status())
                        && factor.closestAlternativeContribution() == null));
        assertFalse(capsule.toString().contains("Infinity"));
        assertFalse(capsule.toString().contains("NaN"));
    }

    @Test
    void capsuleUsesOnlySelectedClosestAlternativeAndAlreadyBuiltEvidence() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true,
                        contribution("selectedOnly", 2.0),
                        contribution("sharedClosest", 7.0)),
                candidate("closest-alt", false,
                        contribution("sharedClosest", 3.0)),
                candidate("far-alt", false,
                        contribution("sharedClosest", 1000.0),
                        contribution("farOnly", 1000.0)));
        DominantFactorAnalysisResponse dominant = dominant("PARTIAL",
                candidateDominant("selected-edge", true, "sharedClosest"),
                candidateDominant("closest-alt", false, "sharedClosest"),
                candidateDominant("far-alt", false, "farOnly"));
        RoutingDecisionDeltaAnalysisResponse delta = delta(
                "PARTIAL",
                "selected-edge",
                "closest-alt",
                1.0,
                factorDelta("sharedClosest", 7.0, 3.0, 4.0));
        RoutingDecisionReplaySnapshotResponse snapshot = snapshotService.snapshot(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("selected-edge", "closest-alt", "far-alt"),
                vector,
                dominant,
                delta);
        RoutingDecisionReplayReconstructionTraceResponse trace = traceService.trace(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("selected-edge", "closest-alt", "far-alt"),
                scores("selected-edge", 10.0, "closest-alt", 9.0, "far-alt", 1.0),
                vector,
                dominant,
                delta,
                snapshot);

        RoutingDecisionReplayCapsuleResponse capsule = service.capsule(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("selected-edge", "closest-alt", "far-alt"),
                scores("selected-edge", 10.0, "closest-alt", 9.0, "far-alt", 1.0),
                vector,
                dominant,
                delta,
                snapshot,
                trace);

        assertEquals("closest-alt", capsule.closestAlternativeCandidateId());
        assertTrue(capsule.candidateEvidence().stream()
                .anyMatch(candidate -> "far-alt".equals(candidate.candidateId())
                        && candidate.factorNames().contains("farOnly")));
        assertFalse(capsule.factorEvidence().stream()
                        .map(DecisionReplayCapsuleFactorEvidenceResponse::factorName)
                        .toList()
                        .contains("farOnly"),
                "factor evidence should not borrow selected-vs-alternative values from non-closest candidates");
        assertTrue(capsule.factorEvidence().stream()
                .anyMatch(factor -> "sharedClosest".equals(factor.factorName())
                        && Double.valueOf(4.0).equals(factor.contributionDelta())));
    }

    @Test
    void capsuleDoesNotUseEnvironmentSpecificScoringReplayPersistenceOrExportInputs() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayCapsuleService.java"),
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
    }

    @Test
    void languageStaysBoundedToReadOnlyLabCapsuleEvidence() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true, contribution("queueDepth", -1.0)),
                candidate("alternative-edge", false, contribution("queueDepth", -3.0)));
        DominantFactorAnalysisResponse dominant = dominant("AVAILABLE",
                candidateDominant("selected-edge", true, "queueDepth"),
                candidateDominant("alternative-edge", false, "queueDepth"));
        RoutingDecisionDeltaAnalysisResponse delta = delta(
                "AVAILABLE",
                "selected-edge",
                "alternative-edge",
                1.0,
                factorDelta("queueDepth", -1.0, -3.0, 2.0));
        RoutingDecisionReplaySnapshotResponse snapshot = snapshotService.snapshot(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("selected-edge", "alternative-edge"),
                vector,
                dominant,
                delta);
        RoutingDecisionReplayReconstructionTraceResponse trace = traceService.trace(
                "TAIL_LATENCY_POWER_OF_TWO",
                "selected-edge",
                List.of("selected-edge", "alternative-edge"),
                scores("selected-edge", 9.0, "alternative-edge", 8.0),
                vector,
                dominant,
                delta,
                snapshot);

        String normalized = service.capsule(
                        "TAIL_LATENCY_POWER_OF_TWO",
                        "selected-edge",
                        List.of("selected-edge", "alternative-edge"),
                        scores("selected-edge", 9.0, "alternative-edge", 8.0),
                        vector,
                        dominant,
                        delta,
                        snapshot,
                        trace)
                .toString()
                .toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("read-only canonical lab evidence packaging"));
        assertTrue(normalized.contains("does not persist capsules or audit logs"));
        assertTrue(normalized.contains("execute replay"));
        assertTrue(normalized.contains("perform what-if mutation"));
        assertTrue(normalized.contains("upload/share/download"));
        assertTrue(normalized.contains("server-side export/pdf/zip"));
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
