package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerFactorDiagnosticV1(
        String candidateId,
        String factorName,
        int displayOrder,
        String contribution,
        String factorStatus,
        String evidenceStatus,
        String observedValueOrStatus,
        String influenceCategory,
        String severity,
        int warningCount,
        int unknownCount,
        int degradedSignalCount,
        int missingSignalCount,
        String summaryText,
        List<String> supportingSignals,
        List<String> warningSignals,
        List<String> unknownSignals,
        List<String> degradedSignals,
        List<String> reasonCodes,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String CONTRIBUTION_SUPPORTING = "SUPPORTING";
    public static final String CONTRIBUTION_WARNING = "WARNING";
    public static final String CONTRIBUTION_UNKNOWN = "UNKNOWN";
    public static final String CONTRIBUTION_DEGRADED = "DEGRADED";
    public static final String CONTRIBUTION_NEUTRAL = "NEUTRAL";

    public DecisionExplorerFactorDiagnosticV1 {
        candidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        factorName = DecisionExplorerDtoSupport.valueOrUnknown(factorName);
        displayOrder = Math.max(0, displayOrder);
        contribution = normalizeContribution(contribution);
        factorStatus = normalizeFactorStatus(factorStatus);
        evidenceStatus = DecisionExplorerDtoSupport.valueOrUnknown(evidenceStatus);
        observedValueOrStatus = DecisionExplorerDtoSupport.valueOrUnknown(observedValueOrStatus);
        influenceCategory = DecisionExplorerDtoSupport.valueOrUnknown(influenceCategory);
        severity = DecisionExplorerDtoSupport.valueOrDefault(severity, severityFor(contribution));
        warningCount = Math.max(0, warningCount);
        unknownCount = Math.max(0, unknownCount);
        degradedSignalCount = Math.max(0, degradedSignalCount);
        missingSignalCount = Math.max(0, missingSignalCount);
        summaryText = DecisionExplorerDtoSupport.valueOrUnknown(summaryText);
        supportingSignals = DecisionExplorerDtoSupport.copyOrEmpty(supportingSignals);
        warningSignals = DecisionExplorerDtoSupport.copyOrEmpty(warningSignals);
        unknownSignals = DecisionExplorerDtoSupport.copyOrEmpty(unknownSignals);
        degradedSignals = DecisionExplorerDtoSupport.copyOrEmpty(degradedSignals);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    static String severityFor(String contribution) {
        return switch (normalizeContribution(contribution)) {
            case CONTRIBUTION_SUPPORTING, CONTRIBUTION_NEUTRAL -> "INFO";
            case CONTRIBUTION_WARNING, CONTRIBUTION_UNKNOWN -> "REVIEW";
            case CONTRIBUTION_DEGRADED -> "BLOCKING";
            default -> "REVIEW";
        };
    }

    private static String normalizeContribution(String contribution) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(contribution);
        return switch (normalized) {
            case CONTRIBUTION_SUPPORTING,
                    CONTRIBUTION_WARNING,
                    CONTRIBUTION_UNKNOWN,
                    CONTRIBUTION_DEGRADED,
                    CONTRIBUTION_NEUTRAL -> normalized;
            default -> CONTRIBUTION_UNKNOWN;
        };
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
