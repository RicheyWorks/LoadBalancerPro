package com.richmond423.loadbalancerpro.api;

import java.util.List;

import com.richmond423.loadbalancerpro.core.CandidateFactorContributionSummary;

public record CandidateDecisionVectorResponse(
        String candidateId,
        boolean selected,
        List<String> knownVisibleSignals,
        List<String> unknownOrUnexposedSignals,
        List<ScoreFactorContributionResponse> factorContributions,
        String selectedVsAlternativeExplanationNote,
        String exactnessBoundary,
        String labProofBoundary,
        String productionNotProvenBoundary) {

    public CandidateDecisionVectorResponse {
        knownVisibleSignals = List.copyOf(knownVisibleSignals);
        unknownOrUnexposedSignals = List.copyOf(unknownOrUnexposedSignals);
        factorContributions = List.copyOf(factorContributions);
    }

    static CandidateDecisionVectorResponse from(CandidateFactorContributionSummary summary) {
        return new CandidateDecisionVectorResponse(
                summary.candidateId(),
                summary.selected(),
                summary.knownVisibleSignals(),
                summary.unknownOrUnexposedSignals(),
                summary.factorContributions().stream()
                        .map(ScoreFactorContributionResponse::from)
                        .toList(),
                summary.selectedVsAlternativeExplanationNote(),
                summary.exactnessBoundary(),
                summary.labProofBoundary(),
                summary.productionNotProvenBoundary());
    }
}
