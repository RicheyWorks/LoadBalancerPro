package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RoutingDominantFactorAnalysisServiceTest {
    private final RoutingDominantFactorAnalysisService service = new RoutingDominantFactorAnalysisService();

    @Test
    void analysisIsDeterministicAndUsesStableFactorNameTieBreaks() {
        RoutingDecisionVectorResponse vector = vector(candidate("edge-a", true,
                contribution("betaRisk", "WEAKENS_SELECTION", 40.0),
                contribution("alphaRisk", "WEAKENS_SELECTION", 40.0),
                contribution("healthPenalty", "SUPPORTS_SELECTION", 0.0),
                contribution("hiddenRoutingInternals", "UNKNOWN", null)));

        DominantFactorAnalysisResponse first = service.analyze(vector);
        DominantFactorAnalysisResponse second = service.analyze(vector);

        assertEquals(first, second, "dominant factor analysis should be deterministic");
        CandidateDominantFactorResponse selected = first.selectedDecisionAnalysis();
        assertEquals("edge-a", selected.candidateId());
        assertEquals("healthPenalty", selected.largestPositiveContributor().factorName());
        assertEquals("alphaRisk", selected.largestPenaltyContributor().factorName());
        assertEquals("alphaRisk", selected.largestAbsoluteImpact().factorName());
        assertEquals(List.of("alphaRisk", "betaRisk", "healthPenalty", "hiddenRoutingInternals"),
                selected.sourceFactorNames());
    }

    @Test
    void emptyContributionDataReturnsSafeUnknownCandidateAnalysis() {
        DominantFactorAnalysisResponse analysis = service.analyze(vector(candidate("edge-empty", true)));

        assertEquals("UNKNOWN", analysis.status());
        assertEquals(1, analysis.candidateAnalyses().size());
        CandidateDominantFactorResponse selected = analysis.selectedDecisionAnalysis();
        assertFalse(selected.available());
        assertEquals(0, selected.sourceContributionCount());
        assertTrue(selected.sourceFactorNames().isEmpty());
        assertNull(selected.largestPositiveContributor());
        assertNull(selected.largestPenaltyContributor());
        assertNull(selected.largestAbsoluteImpact());
        assertTrue(selected.explanation().contains("dominant factors are unknown"));
    }

    @Test
    void nullDecisionVectorReturnsUnknownAnalysisWithoutInventedCandidates() {
        DominantFactorAnalysisResponse analysis = service.analyze(null);

        assertEquals("UNKNOWN", analysis.status());
        assertTrue(analysis.candidateAnalyses().isEmpty());
        assertNull(analysis.selectedDecisionAnalysis());
        assertTrue(analysis.selectedDecisionExplanation().contains("no Decision Vector candidate contributions"));
    }

    @Test
    void selectedDecisionAnalysisUsesOnlySelectedCandidateContributionData() {
        RoutingDecisionVectorResponse vector = vector(
                candidate("selected-edge", true,
                        contribution("selectedRisk", "WEAKENS_SELECTION", 5.0),
                        contribution("selectedSupport", "SUPPORTS_SELECTION", 0.0)),
                candidate("non-selected-edge", false,
                        contribution("largerAlternativeRisk", "WEAKENS_SELECTION", 500.0)));

        DominantFactorAnalysisResponse analysis = service.analyze(vector);

        assertEquals("AVAILABLE", analysis.status());
        assertEquals("selected-edge", analysis.selectedDecisionAnalysis().candidateId());
        assertEquals("selectedRisk", analysis.selectedDecisionAnalysis().largestPenaltyContributor().factorName());
        assertFalse(analysis.selectedDecisionExplanation().contains("largerAlternativeRisk"),
                "selected-decision explanation must not borrow non-selected candidate factors");
    }

    @Test
    void languageStaysBoundedToReadOnlyLabExplainability() {
        String normalized = service.analyze(vector(candidate("edge-a", true,
                contribution("risk", "WEAKENS_SELECTION", 1.0))))
                .toString()
                .toLowerCase(java.util.Locale.ROOT);

        assertTrue(normalized.contains("read-only"));
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
