package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionDeltaAnalysisResponse(
        boolean readOnly,
        String source,
        String status,
        CandidateDecisionDeltaResponse comparison,
        List<ScoreFactorDeltaResponse> factorDeltas,
        ScoreFactorDeltaResponse largestAbsoluteFactorDelta,
        String explanation,
        String boundaryNote,
        String productionNotProvenBoundary) {

    public RoutingDecisionDeltaAnalysisResponse {
        factorDeltas = List.copyOf(factorDeltas);
    }
}
