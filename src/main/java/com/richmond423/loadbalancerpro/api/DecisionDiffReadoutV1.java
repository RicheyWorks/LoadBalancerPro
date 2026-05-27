package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionDiffReadoutV1(
        String baselineCandidateId,
        String comparisonCandidateId,
        String diffStatus,
        Double finalScoreGap,
        List<String> comparedFactorNames,
        List<String> omittedFactorNames,
        List<String> sourceReferenceIds,
        String explanation,
        String boundaryNote) {

    public DecisionDiffReadoutV1 {
        baselineCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(baselineCandidateId);
        comparisonCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(comparisonCandidateId);
        diffStatus = DecisionExplorerDtoSupport.valueOrUnknown(diffStatus);
        comparedFactorNames = DecisionExplorerDtoSupport.copyOrEmpty(comparedFactorNames);
        omittedFactorNames = DecisionExplorerDtoSupport.copyOrEmpty(omittedFactorNames);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        explanation = DecisionExplorerDtoSupport.valueOrUnknown(explanation);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }
}
