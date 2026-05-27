package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerCandidateComparisonRowV1(
        String candidateId,
        String candidateLabel,
        boolean selected,
        int displayOrder,
        String comparisonStatus,
        Double finalScore,
        Double scoreDeltaFromSelected,
        List<String> visibleSignals,
        List<String> unknownSignals,
        List<String> reasonCodes,
        List<String> policyGateIds,
        List<String> evidenceReferenceIds,
        List<String> warnings,
        List<String> unknowns,
        String boundaryNote) {

    public DecisionExplorerCandidateComparisonRowV1 {
        candidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        candidateLabel = DecisionExplorerDtoSupport.valueOrUnknown(candidateLabel);
        comparisonStatus = DecisionExplorerDtoSupport.valueOrUnknown(comparisonStatus);
        visibleSignals = DecisionExplorerDtoSupport.copyOrEmpty(visibleSignals);
        unknownSignals = DecisionExplorerDtoSupport.copyOrEmpty(unknownSignals);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        policyGateIds = DecisionExplorerDtoSupport.copyOrEmpty(policyGateIds);
        evidenceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(evidenceReferenceIds);
        warnings = DecisionExplorerDtoSupport.copyOrEmpty(warnings);
        unknowns = DecisionExplorerDtoSupport.copyOrEmpty(unknowns);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }
}
