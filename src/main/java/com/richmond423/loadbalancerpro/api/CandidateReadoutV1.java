package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record CandidateReadoutV1(
        String candidateId,
        String candidateLabel,
        boolean selected,
        String candidateStatus,
        Double finalScore,
        List<String> visibleSignals,
        List<String> unknownSignals,
        List<String> reasonCodes,
        List<String> policyGateIds,
        List<String> evidenceReferenceIds,
        String boundaryNote) {

    public CandidateReadoutV1 {
        candidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        candidateLabel = DecisionExplorerDtoSupport.valueOrUnknown(candidateLabel);
        candidateStatus = DecisionExplorerDtoSupport.valueOrUnknown(candidateStatus);
        visibleSignals = DecisionExplorerDtoSupport.copyOrEmpty(visibleSignals);
        unknownSignals = DecisionExplorerDtoSupport.copyOrEmpty(unknownSignals);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        policyGateIds = DecisionExplorerDtoSupport.copyOrEmpty(policyGateIds);
        evidenceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(evidenceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }
}
