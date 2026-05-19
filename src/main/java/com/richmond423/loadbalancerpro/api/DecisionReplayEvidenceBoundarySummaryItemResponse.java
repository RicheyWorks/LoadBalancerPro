package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionReplayEvidenceBoundarySummaryItemResponse(
        String boundaryId,
        String label,
        String status,
        String sourceFieldPath,
        List<String> supportingEvidenceFieldPaths,
        String evidenceSummary,
        String boundaryNote) {

    public DecisionReplayEvidenceBoundarySummaryItemResponse {
        supportingEvidenceFieldPaths = supportingEvidenceFieldPaths == null
                ? List.of()
                : List.copyOf(supportingEvidenceFieldPaths);
    }
}
