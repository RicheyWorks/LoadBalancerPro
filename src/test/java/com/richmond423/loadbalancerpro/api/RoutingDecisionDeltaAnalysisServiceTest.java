package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RoutingDecisionDeltaAnalysisServiceTest {
    private final RoutingDecisionDeltaAnalysisService service = new RoutingDecisionDeltaAnalysisService();

    @Test
    void closestAlternativeSelectionAndFactorTieHandlingAreDeterministic() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true,
                        contribution("beta", "WEAKENS_SELECTION", 8.0),
                        contribution("alpha", "WEAKENS_SELECTION", 5.0)),
                candidate("alt-beta", false,
                        contribution("alpha", "WEAKENS_SELECTION", 1.0),
                        contribution("beta", "WEAKENS_SELECTION", 4.0)),
                candidate("alt-alpha", false,
                        contribution("beta", "WEAKENS_SELECTION", 4.0),
                        contribution("alpha", "WEAKENS_SELECTION", 1.0)));
        Map<String, Double> scores = Map.of(
                "selected-edge", 10.0,
                "alt-beta", 12.0,
                "alt-alpha", 8.0);

        RoutingDecisionDeltaAnalysisResponse first = service.analyze(vector, scores);
        RoutingDecisionDeltaAnalysisResponse second = service.analyze(vector, scores);

        assertEquals(first, second, "decision delta analysis should be deterministic");
        assertEquals("AVAILABLE", first.status());
        assertEquals("alt-alpha", first.comparison().closestAlternativeCandidateId(),
                "equal score-gap alternatives should tie-break by candidate id");
        assertEquals(2.0, first.comparison().finalScoreGap());
        assertEquals("alpha", first.largestAbsoluteFactorDelta().factorName(),
                "equal factor deltas should tie-break by factor name");
        assertEquals(List.of("alpha", "beta"), first.comparison().comparedFactorNames());
    }

    @Test
    void selectedComparisonUsesOnlySelectedAndClosestAlternativeContributionData() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true,
                        contribution("selectedFactor", "WEAKENS_SELECTION", 10.0),
                        contribution("sharedClosest", "WEAKENS_SELECTION", 7.0)),
                candidate("closest-alt", false,
                        contribution("sharedClosest", "WEAKENS_SELECTION", 3.0)),
                candidate("far-alt", false,
                        contribution("sharedClosest", "WEAKENS_SELECTION", 1000.0),
                        contribution("farOnly", "WEAKENS_SELECTION", 1000.0)));
        Map<String, Double> scores = Map.of(
                "selected-edge", 10.0,
                "closest-alt", 11.0,
                "far-alt", 200.0);

        RoutingDecisionDeltaAnalysisResponse analysis = service.analyze(vector, scores);

        assertEquals("PARTIAL", analysis.status());
        assertEquals("closest-alt", analysis.comparison().closestAlternativeCandidateId());
        assertEquals("sharedClosest", analysis.largestAbsoluteFactorDelta().factorName());
        assertFalse(analysis.toString().contains("farOnly"),
                "selected-vs-alternative analysis must not borrow contribution data from non-closest alternatives");
        assertTrue(analysis.comparison().omittedFactorNames().contains("selectedFactor"));
    }

    @Test
    void emptyContributionDataReturnsSafePartialWithoutInventedFactorDeltas() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true),
                candidate("alternative-edge", false));
        Map<String, Double> scores = Map.of("selected-edge", 10.0, "alternative-edge", 12.0);

        RoutingDecisionDeltaAnalysisResponse analysis = service.analyze(vector, scores);

        assertEquals("PARTIAL", analysis.status());
        assertEquals("alternative-edge", analysis.comparison().closestAlternativeCandidateId());
        assertTrue(analysis.factorDeltas().isEmpty());
        assertNull(analysis.largestAbsoluteFactorDelta());
        assertEquals(0, analysis.comparison().comparedFactorCount());
        assertTrue(analysis.explanation().contains("no shared finite factor contribution deltas"));
    }

    @Test
    void nonFiniteAndMissingContributionValuesAreOmittedWithoutInventingZeros() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true,
                        contribution("finiteShared", "WEAKENS_SELECTION", 4.0),
                        contribution("selectedNaN", "WEAKENS_SELECTION", Double.NaN),
                        contribution("selectedInfinity", "WEAKENS_SELECTION", Double.POSITIVE_INFINITY),
                        contribution("selectedOnly", "WEAKENS_SELECTION", 2.0)),
                candidate("alternative-edge", false,
                        contribution("finiteShared", "WEAKENS_SELECTION", 1.0),
                        contribution("selectedNaN", "WEAKENS_SELECTION", 1.0),
                        contribution("selectedInfinity", "WEAKENS_SELECTION", 1.0),
                        contribution("alternativeOnly", "WEAKENS_SELECTION", 3.0)));
        Map<String, Double> scores = Map.of("selected-edge", 10.0, "alternative-edge", 11.0);

        RoutingDecisionDeltaAnalysisResponse analysis = service.analyze(vector, scores);

        assertEquals("PARTIAL", analysis.status());
        assertEquals(1, analysis.factorDeltas().size());
        assertEquals("finiteShared", analysis.factorDeltas().get(0).factorName());
        assertEquals(List.of("alternativeOnly", "selectedInfinity", "selectedNaN", "selectedOnly"),
                analysis.comparison().omittedFactorNames());
        assertFalse(analysis.toString().contains("0.0, alternativeOnly"),
                "missing factors must not be represented as invented zero values");
    }

    @Test
    void missingDecisionVectorOrScoreDataReturnsUnknown() {
        assertEquals("UNKNOWN", service.analyze(null, Map.of("selected", 1.0)).status());
        assertEquals("UNKNOWN", service.analyze(vector(candidate("selected", true)), Map.of()).status());
        assertEquals("UNKNOWN", service.analyze(
                vector(candidate("selected", true), candidate("alternative", false)),
                Map.of("selected", Double.NaN, "alternative", 1.0)).status());
        assertEquals("UNKNOWN", service.analyze(
                vector(candidate("selected", true), candidate("alternative", false)),
                Map.of("selected", 1.0, "alternative", Double.POSITIVE_INFINITY)).status());
    }

    @Test
    void languageStaysBoundedToReadOnlyLabExplainability() {
        String normalized = service.analyze(
                        vector(
                                candidate("selected", true, contribution("risk", "WEAKENS_SELECTION", 2.0)),
                                candidate("alternative", false, contribution("risk", "WEAKENS_SELECTION", 1.0))),
                        Map.of("selected", 10.0, "alternative", 11.0))
                .toString()
                .toLowerCase(java.util.Locale.ROOT);

        assertTrue(normalized.contains("read-only"));
        assertTrue(normalized.contains("derived only from returned"));
        assertTrue(normalized.contains("does not change routing behavior"));
        assertTrue(normalized.contains("no production certification"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("live-cloud proof is complete"));
        assertFalse(normalized.contains("real-tenant proof is complete"));
        assertFalse(normalized.contains("governance application proof is complete"));
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
                selected == null ? "not selected" : selected.candidateId(),
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

    private static CandidateDecisionVectorResponse candidate(String candidateId,
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

    private static ScoreFactorContributionResponse contribution(String factorName,
                                                                String direction,
                                                                Double contributionValue) {
        return new ScoreFactorContributionResponse(
                factorName,
                factorName + "=test",
                "test weight",
                direction,
                factorName + " contribution",
                contributionValue,
                contributionValue == null ? "NOT_EXPOSED" : "EXACT_FROM_CALCULATOR",
                factorName + " contribution explanation",
                "controlled lab boundary");
    }
}
