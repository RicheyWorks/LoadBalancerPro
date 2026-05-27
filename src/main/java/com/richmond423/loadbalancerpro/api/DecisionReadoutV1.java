package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionReadoutV1(
        String decisionId,
        String status,
        String selectedCandidateId,
        String selectedStrategy,
        String summary,
        List<String> reasonCodes,
        List<String> sourceReferenceIds,
        String boundaryNote) {

    public DecisionReadoutV1 {
        decisionId = DecisionExplorerDtoSupport.valueOrUnknown(decisionId);
        status = DecisionExplorerDtoSupport.valueOrUnknown(status);
        selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateId);
        selectedStrategy = DecisionExplorerDtoSupport.valueOrUnknown(selectedStrategy);
        summary = DecisionExplorerDtoSupport.valueOrUnknown(summary);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }
}
