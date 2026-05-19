package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionReplayEvidenceSourceMapEntryResponse(
        String sourceId,
        String label,
        String status,
        String sourceFieldPath,
        List<String> downstreamEvidenceFieldPaths,
        String linkedFingerprint,
        String evidenceSummary,
        String boundaryNote) {

    public DecisionReplayEvidenceSourceMapEntryResponse {
        downstreamEvidenceFieldPaths = downstreamEvidenceFieldPaths == null
                ? List.of()
                : List.copyOf(downstreamEvidenceFieldPaths);
    }
}
