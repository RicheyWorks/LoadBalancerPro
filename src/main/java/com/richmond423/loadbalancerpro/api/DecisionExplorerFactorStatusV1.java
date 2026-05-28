package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerFactorStatusV1(
        String candidateId,
        String factorName,
        int displayOrder,
        String factorStatus,
        String evidenceStatus,
        String observedValueOrStatus,
        String influenceCategory,
        String interpretation,
        List<String> statusReasons,
        List<String> warnings,
        List<String> unknowns,
        List<String> sourceReferenceIds,
        String boundaryNote) {

    public DecisionExplorerFactorStatusV1 {
        candidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        factorName = DecisionExplorerDtoSupport.valueOrUnknown(factorName);
        displayOrder = Math.max(0, displayOrder);
        factorStatus = normalizeFactorStatus(factorStatus);
        evidenceStatus = DecisionExplorerDtoSupport.valueOrUnknown(evidenceStatus);
        observedValueOrStatus = DecisionExplorerDtoSupport.valueOrUnknown(observedValueOrStatus);
        influenceCategory = DecisionExplorerDtoSupport.valueOrUnknown(influenceCategory);
        interpretation = DecisionExplorerDtoSupport.valueOrUnknown(interpretation);
        statusReasons = DecisionExplorerDtoSupport.copyOrEmpty(statusReasons);
        warnings = DecisionExplorerDtoSupport.copyOrEmpty(warnings);
        unknowns = DecisionExplorerDtoSupport.copyOrEmpty(unknowns);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    private static String normalizeFactorStatus(String factorStatus) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(factorStatus);
        return switch (normalized) {
            case DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                    DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL,
                    DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                    DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED -> normalized;
            default -> DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
        };
    }
}
