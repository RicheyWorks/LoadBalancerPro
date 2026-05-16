package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionVectorResponse(
        boolean readOnly,
        String localLabResponsePath,
        String decisionIdOrLabRunId,
        String selectedStrategy,
        String selectedBackend,
        int candidateCount,
        List<CandidateDecisionVectorResponse> candidateSummaries,
        CandidateDecisionVectorResponse selectedCandidateVector,
        List<CandidateDecisionVectorResponse> nonSelectedCandidateVectors,
        List<String> knownVisibleSignals,
        List<String> unknownOrUnexposedSignals,
        String exactnessBoundary,
        List<String> selectedVsAlternativeExplanationNotes,
        String labProofBoundary,
        String productionNotProvenBoundary,
        String factorContributionAvailability,
        String replayReadiness,
        String whatIfReadiness,
        String structuredDecisionLoggingReadiness) {

    public RoutingDecisionVectorResponse {
        candidateSummaries = List.copyOf(candidateSummaries);
        nonSelectedCandidateVectors = List.copyOf(nonSelectedCandidateVectors);
        knownVisibleSignals = List.copyOf(knownVisibleSignals);
        unknownOrUnexposedSignals = List.copyOf(unknownOrUnexposedSignals);
        selectedVsAlternativeExplanationNotes = List.copyOf(selectedVsAlternativeExplanationNotes);
    }
}
