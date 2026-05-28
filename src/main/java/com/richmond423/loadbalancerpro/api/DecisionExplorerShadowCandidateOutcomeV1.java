package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerShadowCandidateOutcomeV1(
        String candidateId,
        String candidateLabel,
        boolean selected,
        int displayOrder,
        String outcomeLabel,
        String qualityImpact,
        String tradeoffCategory,
        String riskBenefitClassification,
        String diagnosticStatus,
        String riskLevel,
        String scoreGapCategory,
        Double finalScore,
        Double scoreDeltaFromSelected,
        String summaryText,
        List<String> benefitSignals,
        List<String> riskSignals,
        List<String> unknownSignals,
        List<String> degradedSignals,
        List<String> reasonCodes,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String OUTCOME_SELECTED_BASELINE = "SELECTED_BASELINE";
    public static final String OUTCOME_ACCEPTABLE_ALTERNATIVE = "ACCEPTABLE_ALTERNATIVE";
    public static final String OUTCOME_CLOSE_CALL = "CLOSE_CALL";
    public static final String OUTCOME_SAFER_ALTERNATIVE = "SAFER_ALTERNATIVE";
    public static final String OUTCOME_DEGRADED_SELECTED = "DEGRADED_SELECTED";
    public static final String OUTCOME_UNKNOWN_ALTERNATIVE = "UNKNOWN_ALTERNATIVE";

    public static final String IMPACT_SUPPORTS_DECISION = "SUPPORTS_DECISION";
    public static final String IMPACT_REVIEW_SIGNAL = "REVIEW_SIGNAL";
    public static final String IMPACT_RISK_SIGNAL = "RISK_SIGNAL";
    public static final String IMPACT_UNKNOWN = "UNKNOWN";

    public DecisionExplorerShadowCandidateOutcomeV1 {
        candidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        candidateLabel = DecisionExplorerDtoSupport.valueOrDefault(candidateLabel, candidateId);
        displayOrder = Math.max(0, displayOrder);
        outcomeLabel = normalizeOutcomeLabel(outcomeLabel);
        qualityImpact = normalizeQualityImpact(qualityImpact);
        tradeoffCategory = DecisionExplorerDtoSupport.valueOrUnknown(tradeoffCategory);
        riskBenefitClassification = DecisionExplorerDtoSupport.valueOrUnknown(riskBenefitClassification);
        diagnosticStatus = normalizeStatus(diagnosticStatus);
        riskLevel = DecisionExplorerDtoSupport.valueOrUnknown(riskLevel);
        scoreGapCategory = DecisionExplorerDtoSupport.valueOrUnknown(scoreGapCategory);
        finalScore = finiteOrNull(finalScore);
        scoreDeltaFromSelected = finiteOrNull(scoreDeltaFromSelected);
        summaryText = DecisionExplorerDtoSupport.valueOrUnknown(summaryText);
        benefitSignals = DecisionExplorerDtoSupport.copyOrEmpty(benefitSignals);
        riskSignals = DecisionExplorerDtoSupport.copyOrEmpty(riskSignals);
        unknownSignals = DecisionExplorerDtoSupport.copyOrEmpty(unknownSignals);
        degradedSignals = DecisionExplorerDtoSupport.copyOrEmpty(degradedSignals);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    private static String normalizeOutcomeLabel(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case OUTCOME_SELECTED_BASELINE,
                    OUTCOME_ACCEPTABLE_ALTERNATIVE,
                    OUTCOME_CLOSE_CALL,
                    OUTCOME_SAFER_ALTERNATIVE,
                    OUTCOME_DEGRADED_SELECTED,
                    OUTCOME_UNKNOWN_ALTERNATIVE -> normalized;
            default -> OUTCOME_UNKNOWN_ALTERNATIVE;
        };
    }

    private static String normalizeQualityImpact(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case IMPACT_SUPPORTS_DECISION,
                    IMPACT_REVIEW_SIGNAL,
                    IMPACT_RISK_SIGNAL,
                    IMPACT_UNKNOWN -> normalized;
            default -> IMPACT_UNKNOWN;
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
