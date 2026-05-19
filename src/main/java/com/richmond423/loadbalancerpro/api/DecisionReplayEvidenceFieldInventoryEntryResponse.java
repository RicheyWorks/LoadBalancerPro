package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionReplayEvidenceFieldInventoryEntryResponse(
        String inventoryId,
        String label,
        String status,
        String sourceFieldPath,
        List<String> observedFieldPaths,
        List<String> missingOrUnavailableFieldPaths,
        int observedFieldCount,
        int missingOrUnavailableFieldCount,
        String evidenceSummary,
        String boundaryNote) {

    public DecisionReplayEvidenceFieldInventoryEntryResponse {
        observedFieldPaths = observedFieldPaths == null ? List.of() : List.copyOf(observedFieldPaths);
        missingOrUnavailableFieldPaths = missingOrUnavailableFieldPaths == null
                ? List.of()
                : List.copyOf(missingOrUnavailableFieldPaths);
        observedFieldCount = observedFieldPaths.size();
        missingOrUnavailableFieldCount = missingOrUnavailableFieldPaths.size();
    }
}
