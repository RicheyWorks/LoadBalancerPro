package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record PolicyGateReadoutV1(
        String gateId,
        String gateName,
        String gateStatus,
        String outcome,
        String explanation,
        List<String> sourceReferenceIds,
        String boundaryNote) {

    public PolicyGateReadoutV1 {
        gateId = DecisionExplorerDtoSupport.valueOrUnknown(gateId);
        gateName = DecisionExplorerDtoSupport.valueOrUnknown(gateName);
        gateStatus = DecisionExplorerDtoSupport.valueOrUnknown(gateStatus);
        outcome = DecisionExplorerDtoSupport.valueOrUnknown(outcome);
        explanation = DecisionExplorerDtoSupport.valueOrUnknown(explanation);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }
}
