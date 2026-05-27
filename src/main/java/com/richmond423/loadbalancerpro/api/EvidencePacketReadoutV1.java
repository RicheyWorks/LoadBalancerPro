package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record EvidencePacketReadoutV1(
        String referenceId,
        String packetStatus,
        String sourceReferenceId,
        String freshnessStatus,
        List<String> unavailableReasons,
        String boundaryNote) {

    public EvidencePacketReadoutV1 {
        referenceId = DecisionExplorerDtoSupport.valueOrUnknown(referenceId);
        packetStatus = DecisionExplorerDtoSupport.valueOrUnknown(packetStatus);
        sourceReferenceId = DecisionExplorerDtoSupport.valueOrUnknown(sourceReferenceId);
        freshnessStatus = DecisionExplorerDtoSupport.valueOrUnknown(freshnessStatus);
        unavailableReasons = DecisionExplorerDtoSupport.copyOrEmpty(unavailableReasons);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }
}
