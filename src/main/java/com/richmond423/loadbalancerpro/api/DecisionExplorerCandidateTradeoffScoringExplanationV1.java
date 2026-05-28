package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerCandidateTradeoffScoringExplanationV1(
        String candidateId,
        String candidateLabel,
        boolean selected,
        int displayOrder,
        String explanationStatus,
        String scoreEvidenceState,
        String scoreGapCategory,
        String tradeoffCategory,
        String riskBenefitClassification,
        String candidateDiagnosticStatus,
        String factorStatusRollup,
        Double finalScore,
        Double scoreDeltaFromSelected,
        String summaryText,
        List<String> scoringSignals,
        List<String> limitationSignals,
        List<String> reasonCodes,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String SCORE_EVIDENCE_SELECTED_BASELINE_PRESENT = "SELECTED_BASELINE_SCORE_PRESENT";
    public static final String SCORE_EVIDENCE_SELECTED_BASELINE_UNKNOWN = "SELECTED_BASELINE_SCORE_UNKNOWN";
    public static final String SCORE_EVIDENCE_ALTERNATIVE_DELTA_PRESENT = "ALTERNATIVE_DELTA_PRESENT";
    public static final String SCORE_EVIDENCE_ALTERNATIVE_DELTA_UNKNOWN = "ALTERNATIVE_DELTA_UNKNOWN";

    public DecisionExplorerCandidateTradeoffScoringExplanationV1 {
        candidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        candidateLabel = DecisionExplorerDtoSupport.valueOrDefault(candidateLabel, candidateId);
        displayOrder = Math.max(0, displayOrder);
        explanationStatus = normalizeStatus(explanationStatus);
        scoreEvidenceState = normalizeScoreEvidenceState(scoreEvidenceState);
        scoreGapCategory = DecisionExplorerDtoSupport.valueOrUnknown(scoreGapCategory);
        tradeoffCategory = DecisionExplorerDtoSupport.valueOrUnknown(tradeoffCategory);
        riskBenefitClassification = DecisionExplorerDtoSupport.valueOrUnknown(riskBenefitClassification);
        candidateDiagnosticStatus = normalizeStatus(candidateDiagnosticStatus);
        factorStatusRollup = normalizeStatus(factorStatusRollup);
        finalScore = finiteOrNull(finalScore);
        scoreDeltaFromSelected = finiteOrNull(scoreDeltaFromSelected);
        summaryText = DecisionExplorerDtoSupport.valueOrUnknown(summaryText);
        scoringSignals = DecisionExplorerDtoSupport.copyOrEmpty(scoringSignals);
        limitationSignals = DecisionExplorerDtoSupport.copyOrEmpty(limitationSignals);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    static String scoreEvidenceStateFor(boolean selected, Double finalScore, Double scoreDeltaFromSelected) {
        if (selected) {
            return finiteOrNull(finalScore) == null
                    ? SCORE_EVIDENCE_SELECTED_BASELINE_UNKNOWN
                    : SCORE_EVIDENCE_SELECTED_BASELINE_PRESENT;
        }
        return finiteOrNull(scoreDeltaFromSelected) == null
                ? SCORE_EVIDENCE_ALTERNATIVE_DELTA_UNKNOWN
                : SCORE_EVIDENCE_ALTERNATIVE_DELTA_PRESENT;
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

    private static String normalizeScoreEvidenceState(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case SCORE_EVIDENCE_SELECTED_BASELINE_PRESENT,
                    SCORE_EVIDENCE_SELECTED_BASELINE_UNKNOWN,
                    SCORE_EVIDENCE_ALTERNATIVE_DELTA_PRESENT,
                    SCORE_EVIDENCE_ALTERNATIVE_DELTA_UNKNOWN -> normalized;
            default -> "UNKNOWN";
        };
    }

    private static Double finiteOrNull(Double value) {
        return value == null || !Double.isFinite(value) ? null : value;
    }
}
