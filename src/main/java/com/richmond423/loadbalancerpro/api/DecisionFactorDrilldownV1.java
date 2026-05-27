package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionFactorDrilldownV1(
        String factorName,
        String candidateId,
        String observedValueOrStatus,
        String influenceCategory,
        String evidenceStatus,
        String explanation,
        List<String> warnings,
        List<String> unknowns,
        List<String> sourceReferenceIds,
        String boundaryNote) {

    public DecisionFactorDrilldownV1 {
        factorName = DecisionExplorerDtoSupport.valueOrUnknown(factorName);
        candidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        observedValueOrStatus = DecisionExplorerDtoSupport.valueOrUnknown(observedValueOrStatus);
        influenceCategory = DecisionExplorerDtoSupport.valueOrUnknown(influenceCategory);
        evidenceStatus = DecisionExplorerDtoSupport.valueOrUnknown(evidenceStatus);
        explanation = DecisionExplorerDtoSupport.valueOrUnknown(explanation);
        warnings = DecisionExplorerDtoSupport.copyOrEmpty(warnings);
        unknowns = DecisionExplorerDtoSupport.copyOrEmpty(unknowns);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }
}
