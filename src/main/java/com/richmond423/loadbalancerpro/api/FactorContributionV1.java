package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record FactorContributionV1(
        String factorName,
        String candidateId,
        String direction,
        Double contributionValue,
        String exactness,
        String explanation,
        List<String> evidenceReferenceIds,
        String boundaryNote) {

    public FactorContributionV1 {
        factorName = DecisionExplorerDtoSupport.valueOrUnknown(factorName);
        candidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        direction = DecisionExplorerDtoSupport.valueOrUnknown(direction);
        exactness = DecisionExplorerDtoSupport.valueOrUnknown(exactness);
        explanation = DecisionExplorerDtoSupport.valueOrUnknown(explanation);
        evidenceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(evidenceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }
}
