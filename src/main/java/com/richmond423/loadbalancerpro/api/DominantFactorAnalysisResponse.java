package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DominantFactorAnalysisResponse(
        boolean readOnly,
        String source,
        String status,
        List<CandidateDominantFactorResponse> candidateAnalyses,
        CandidateDominantFactorResponse selectedDecisionAnalysis,
        String selectedDecisionExplanation,
        String boundaryNote,
        String productionNotProvenBoundary) {

    public DominantFactorAnalysisResponse {
        candidateAnalyses = List.copyOf(candidateAnalyses);
    }
}
