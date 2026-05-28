package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerRouteTradeoffRowV1(
        String candidateId,
        String candidateLabel,
        boolean selected,
        int displayOrder,
        String tradeoffCategory,
        String riskBenefitClassification,
        String diagnosticStatus,
        String riskLevel,
        String healthEvidenceState,
        Double finalScore,
        Double scoreDeltaFromSelected,
        String scoreGapCategory,
        String scoringExplanation,
        String evidenceSummary,
        List<String> benefitSignals,
        List<String> riskSignals,
        List<String> unknownSignals,
        List<String> degradedSignals,
        List<String> reasonCodes,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String TRADEOFF_SELECTED_BASELINE = "SELECTED_BASELINE";
    public static final String TRADEOFF_ALTERNATIVE_TRAILS_SELECTED = "ALTERNATIVE_TRAILS_SELECTED";
    public static final String TRADEOFF_ALTERNATIVE_CLOSE = "ALTERNATIVE_CLOSE";
    public static final String TRADEOFF_ALTERNATIVE_BEATS_SELECTED = "ALTERNATIVE_BEATS_SELECTED";
    public static final String TRADEOFF_ALTERNATIVE_UNKNOWN = "ALTERNATIVE_UNKNOWN";
    public static final String TRADEOFF_NO_ALTERNATIVE = "NO_ALTERNATIVE";
    public static final String TRADEOFF_UNKNOWN = "UNKNOWN";

    public static final String CLASSIFICATION_BASELINE = "BASELINE";
    public static final String CLASSIFICATION_SELECTED_ADVANTAGE = "SELECTED_ADVANTAGE";
    public static final String CLASSIFICATION_TRADEOFF = "TRADEOFF";
    public static final String CLASSIFICATION_RISK = "RISK";
    public static final String CLASSIFICATION_UNKNOWN = "UNKNOWN";

    public DecisionExplorerRouteTradeoffRowV1 {
        candidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        candidateLabel = DecisionExplorerDtoSupport.valueOrDefault(candidateLabel, candidateId);
        displayOrder = Math.max(0, displayOrder);
        tradeoffCategory = normalizeTradeoffCategory(tradeoffCategory);
        riskBenefitClassification = normalizeClassification(riskBenefitClassification);
        diagnosticStatus = normalizeStatus(diagnosticStatus);
        riskLevel = DecisionExplorerDtoSupport.valueOrUnknown(riskLevel);
        healthEvidenceState = DecisionExplorerDtoSupport.valueOrUnknown(healthEvidenceState);
        finalScore = finiteOrNull(finalScore);
        scoreDeltaFromSelected = finiteOrNull(scoreDeltaFromSelected);
        scoreGapCategory = DecisionExplorerDtoSupport.valueOrUnknown(scoreGapCategory);
        scoringExplanation = DecisionExplorerDtoSupport.valueOrUnknown(scoringExplanation);
        evidenceSummary = DecisionExplorerDtoSupport.valueOrUnknown(evidenceSummary);
        benefitSignals = DecisionExplorerDtoSupport.copyOrEmpty(benefitSignals);
        riskSignals = DecisionExplorerDtoSupport.copyOrEmpty(riskSignals);
        unknownSignals = DecisionExplorerDtoSupport.copyOrEmpty(unknownSignals);
        degradedSignals = DecisionExplorerDtoSupport.copyOrEmpty(degradedSignals);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    static String categoryFor(boolean selected, Double scoreDeltaFromSelected) {
        if (selected) {
            return TRADEOFF_SELECTED_BASELINE;
        }
        Double delta = finiteOrNull(scoreDeltaFromSelected);
        if (delta == null) {
            return TRADEOFF_ALTERNATIVE_UNKNOWN;
        }
        if (delta < 0.0d) {
            return TRADEOFF_ALTERNATIVE_BEATS_SELECTED;
        }
        if (delta <= 1.0d) {
            return TRADEOFF_ALTERNATIVE_CLOSE;
        }
        return TRADEOFF_ALTERNATIVE_TRAILS_SELECTED;
    }

    static String scoreGapCategoryFor(boolean selected, Double scoreDeltaFromSelected) {
        if (selected) {
            return "BASELINE";
        }
        Double delta = finiteOrNull(scoreDeltaFromSelected);
        if (delta == null) {
            return "UNKNOWN_GAP";
        }
        double absDelta = Math.abs(delta);
        if (absDelta == 0.0d) {
            return "TIED";
        }
        if (absDelta <= 1.0d) {
            return "CLOSE";
        }
        return "MATERIAL";
    }

    static String classificationFor(String tradeoffCategory, String riskLevel, int unknownSignalCount,
            int degradedSignalCount) {
        String category = normalizeTradeoffCategory(tradeoffCategory);
        String normalizedRisk = DecisionExplorerDtoSupport.valueOrUnknown(riskLevel);
        if (TRADEOFF_SELECTED_BASELINE.equals(category)) {
            return CLASSIFICATION_BASELINE;
        }
        if (DecisionExplorerCandidateDiagnosticV1.RISK_HIGH.equals(normalizedRisk)
                || degradedSignalCount > 0
                || TRADEOFF_ALTERNATIVE_BEATS_SELECTED.equals(category)) {
            return CLASSIFICATION_RISK;
        }
        if (TRADEOFF_ALTERNATIVE_UNKNOWN.equals(category)
                || DecisionExplorerCandidateDiagnosticV1.RISK_UNKNOWN.equals(normalizedRisk)) {
            return CLASSIFICATION_UNKNOWN;
        }
        if (TRADEOFF_ALTERNATIVE_CLOSE.equals(category)
                || DecisionExplorerCandidateDiagnosticV1.RISK_REVIEW.equals(normalizedRisk)) {
            return CLASSIFICATION_TRADEOFF;
        }
        return CLASSIFICATION_SELECTED_ADVANTAGE;
    }

    private static String normalizeTradeoffCategory(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case TRADEOFF_SELECTED_BASELINE,
                    TRADEOFF_ALTERNATIVE_TRAILS_SELECTED,
                    TRADEOFF_ALTERNATIVE_CLOSE,
                    TRADEOFF_ALTERNATIVE_BEATS_SELECTED,
                    TRADEOFF_ALTERNATIVE_UNKNOWN,
                    TRADEOFF_NO_ALTERNATIVE,
                    TRADEOFF_UNKNOWN -> normalized;
            default -> TRADEOFF_UNKNOWN;
        };
    }

    private static String normalizeClassification(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case CLASSIFICATION_BASELINE,
                    CLASSIFICATION_SELECTED_ADVANTAGE,
                    CLASSIFICATION_TRADEOFF,
                    CLASSIFICATION_RISK,
                    CLASSIFICATION_UNKNOWN -> normalized;
            default -> CLASSIFICATION_UNKNOWN;
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
