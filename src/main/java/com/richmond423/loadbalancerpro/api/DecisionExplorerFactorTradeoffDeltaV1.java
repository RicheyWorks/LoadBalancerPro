package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerFactorTradeoffDeltaV1(
        String factorName,
        String selectedCandidateId,
        String alternativeCandidateId,
        int displayOrder,
        String deltaClassification,
        String selectedContribution,
        String alternativeContribution,
        String selectedFactorStatus,
        String alternativeFactorStatus,
        String selectedEvidenceStatus,
        String alternativeEvidenceStatus,
        String selectedObservedValueOrStatus,
        String alternativeObservedValueOrStatus,
        String candidateTradeoffCategory,
        String scoreGapCategory,
        Double alternativeScoreDeltaFromSelected,
        String summaryText,
        List<String> selectedSignals,
        List<String> alternativeSignals,
        List<String> limitationSignals,
        List<String> reasonCodes,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String DELTA_ADVANTAGE = "ADVANTAGE";
    public static final String DELTA_DISADVANTAGE = "DISADVANTAGE";
    public static final String DELTA_NEUTRAL = "NEUTRAL";
    public static final String DELTA_UNKNOWN = "UNKNOWN";
    public static final String DELTA_DEGRADED = "DEGRADED";

    public DecisionExplorerFactorTradeoffDeltaV1 {
        factorName = DecisionExplorerDtoSupport.valueOrUnknown(factorName);
        selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateId);
        alternativeCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(alternativeCandidateId);
        displayOrder = Math.max(0, displayOrder);
        deltaClassification = normalizeDeltaClassification(deltaClassification);
        selectedContribution = normalizeContribution(selectedContribution);
        alternativeContribution = normalizeContribution(alternativeContribution);
        selectedFactorStatus = normalizeStatus(selectedFactorStatus);
        alternativeFactorStatus = normalizeStatus(alternativeFactorStatus);
        selectedEvidenceStatus = DecisionExplorerDtoSupport.valueOrUnknown(selectedEvidenceStatus);
        alternativeEvidenceStatus = DecisionExplorerDtoSupport.valueOrUnknown(alternativeEvidenceStatus);
        selectedObservedValueOrStatus = DecisionExplorerDtoSupport.valueOrUnknown(selectedObservedValueOrStatus);
        alternativeObservedValueOrStatus = DecisionExplorerDtoSupport.valueOrUnknown(alternativeObservedValueOrStatus);
        candidateTradeoffCategory = DecisionExplorerDtoSupport.valueOrUnknown(candidateTradeoffCategory);
        scoreGapCategory = DecisionExplorerDtoSupport.valueOrUnknown(scoreGapCategory);
        alternativeScoreDeltaFromSelected = finiteOrNull(alternativeScoreDeltaFromSelected);
        summaryText = DecisionExplorerDtoSupport.valueOrUnknown(summaryText);
        selectedSignals = DecisionExplorerDtoSupport.copyOrEmpty(selectedSignals);
        alternativeSignals = DecisionExplorerDtoSupport.copyOrEmpty(alternativeSignals);
        limitationSignals = DecisionExplorerDtoSupport.copyOrEmpty(limitationSignals);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    static String classificationFor(
            DecisionExplorerFactorDiagnosticV1 selectedFactor,
            DecisionExplorerFactorDiagnosticV1 alternativeFactor) {
        if (selectedFactor == null || alternativeFactor == null) {
            return DELTA_UNKNOWN;
        }
        if (isDegraded(selectedFactor) || isDegraded(alternativeFactor)) {
            return DELTA_DEGRADED;
        }
        int selectedRank = contributionRank(selectedFactor.contribution());
        int alternativeRank = contributionRank(alternativeFactor.contribution());
        if (selectedRank < 0 || alternativeRank < 0) {
            return DELTA_UNKNOWN;
        }
        if (selectedRank > alternativeRank) {
            return DELTA_ADVANTAGE;
        }
        if (selectedRank < alternativeRank) {
            return DELTA_DISADVANTAGE;
        }
        return DELTA_NEUTRAL;
    }

    private static boolean isDegraded(DecisionExplorerFactorDiagnosticV1 factor) {
        return DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(factor.factorStatus())
                || DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_DEGRADED.equals(factor.contribution())
                || factor.degradedSignalCount() > 0;
    }

    private static int contributionRank(String contribution) {
        return switch (normalizeContribution(contribution)) {
            case DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_SUPPORTING -> 3;
            case DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_NEUTRAL -> 2;
            case DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_WARNING -> 1;
            default -> -1;
        };
    }

    private static String normalizeDeltaClassification(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case DELTA_ADVANTAGE,
                    DELTA_DISADVANTAGE,
                    DELTA_NEUTRAL,
                    DELTA_UNKNOWN,
                    DELTA_DEGRADED -> normalized;
            default -> DELTA_UNKNOWN;
        };
    }

    private static String normalizeContribution(String contribution) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(contribution);
        return switch (normalized) {
            case DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_SUPPORTING,
                    DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_WARNING,
                    DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_UNKNOWN,
                    DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_DEGRADED,
                    DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_NEUTRAL -> normalized;
            default -> DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_UNKNOWN;
        };
    }

    private static String normalizeStatus(String status) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(status);
        return switch (normalized) {
            case DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                    DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL,
                    DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                    DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED -> normalized;
            default -> DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
        };
    }

    private static Double finiteOrNull(Double value) {
        return value == null || !Double.isFinite(value) ? null : value;
    }
}
