package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerCandidateConfidenceV1(
        String candidateId,
        String candidateLabel,
        boolean selected,
        int displayOrder,
        String confidenceStatus,
        String healthEvidenceState,
        String comparisonStatus,
        Double finalScore,
        Double scoreDeltaFromSelected,
        int visibleSignalCount,
        int unknownSignalCount,
        int availableFactorCount,
        int partialFactorCount,
        int unknownFactorCount,
        List<String> confidenceReasons,
        List<String> warnings,
        List<String> unknowns,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String HEALTHY = "HEALTHY";
    public static final String DEGRADED = "DEGRADED";
    public static final String UNKNOWN = "UNKNOWN";

    public DecisionExplorerCandidateConfidenceV1 {
        candidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        candidateLabel = DecisionExplorerDtoSupport.valueOrDefault(candidateLabel, candidateId);
        displayOrder = Math.max(0, displayOrder);
        confidenceStatus = normalizeConfidenceStatus(confidenceStatus);
        healthEvidenceState = normalizeHealthEvidenceState(healthEvidenceState);
        comparisonStatus = DecisionExplorerDtoSupport.valueOrUnknown(comparisonStatus);
        finalScore = finiteOrNull(finalScore);
        scoreDeltaFromSelected = finiteOrNull(scoreDeltaFromSelected);
        visibleSignalCount = Math.max(0, visibleSignalCount);
        unknownSignalCount = Math.max(0, unknownSignalCount);
        availableFactorCount = Math.max(0, availableFactorCount);
        partialFactorCount = Math.max(0, partialFactorCount);
        unknownFactorCount = Math.max(0, unknownFactorCount);
        confidenceReasons = DecisionExplorerDtoSupport.copyOrEmpty(confidenceReasons);
        warnings = DecisionExplorerDtoSupport.copyOrEmpty(warnings);
        unknowns = DecisionExplorerDtoSupport.copyOrEmpty(unknowns);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    private static String normalizeConfidenceStatus(String confidenceStatus) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(confidenceStatus);
        return switch (normalized) {
            case DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                    DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL,
                    DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                    DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED -> normalized;
            default -> DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
        };
    }

    private static String normalizeHealthEvidenceState(String healthEvidenceState) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(healthEvidenceState);
        return switch (normalized) {
            case HEALTHY, DEGRADED, UNKNOWN -> normalized;
            default -> UNKNOWN;
        };
    }

    private static Double finiteOrNull(Double value) {
        return value == null || !Double.isFinite(value) ? null : value;
    }
}
