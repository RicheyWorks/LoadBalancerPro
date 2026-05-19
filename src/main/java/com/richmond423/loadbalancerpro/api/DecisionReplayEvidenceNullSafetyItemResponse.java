package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionReplayEvidenceNullSafetyItemResponse(
        String nullSafetyId,
        String label,
        String status,
        String sourceFieldPath,
        List<String> checkedFieldPaths,
        List<String> unavailableFieldPaths,
        int checkedFieldCount,
        int unavailableFieldCount,
        String safetySummary,
        String boundaryNote) {

    public DecisionReplayEvidenceNullSafetyItemResponse {
        checkedFieldPaths = checkedFieldPaths == null ? List.of() : List.copyOf(checkedFieldPaths);
        unavailableFieldPaths = unavailableFieldPaths == null ? List.of() : List.copyOf(unavailableFieldPaths);
        checkedFieldCount = checkedFieldPaths.size();
        unavailableFieldCount = unavailableFieldPaths.size();
    }
}
