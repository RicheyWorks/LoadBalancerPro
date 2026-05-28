package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerCandidateDiagnosticV1(
        String candidateId,
        String candidateLabel,
        boolean selected,
        int displayOrder,
        String selectionRole,
        String diagnosticStatus,
        String riskLevel,
        String healthEvidenceState,
        String comparisonStatus,
        Double finalScore,
        Double scoreDeltaFromSelected,
        int visibleSignalCount,
        int warningCount,
        int unknownSignalCount,
        int degradedSignalCount,
        String scoreInterpretation,
        String summaryText,
        List<String> strengthSignals,
        List<String> weakSignals,
        List<String> degradedSignals,
        List<String> unknownSignals,
        List<String> reasonCodes,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String ROLE_SELECTED = "SELECTED";
    public static final String ROLE_ALTERNATIVE = "ALTERNATIVE";
    public static final String ROLE_UNKNOWN = "UNKNOWN";
    public static final String RISK_LOW = "LOW";
    public static final String RISK_REVIEW = "REVIEW";
    public static final String RISK_UNKNOWN = "UNKNOWN";
    public static final String RISK_HIGH = "HIGH";

    public DecisionExplorerCandidateDiagnosticV1 {
        candidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        candidateLabel = DecisionExplorerDtoSupport.valueOrDefault(candidateLabel, candidateId);
        displayOrder = Math.max(0, displayOrder);
        selectionRole = normalizeRole(selectionRole);
        diagnosticStatus = normalizeConfidenceStatus(diagnosticStatus);
        riskLevel = normalizeRisk(riskLevel);
        healthEvidenceState = normalizeHealthEvidenceState(healthEvidenceState);
        comparisonStatus = DecisionExplorerDtoSupport.valueOrUnknown(comparisonStatus);
        finalScore = finiteOrNull(finalScore);
        scoreDeltaFromSelected = finiteOrNull(scoreDeltaFromSelected);
        visibleSignalCount = Math.max(0, visibleSignalCount);
        warningCount = Math.max(0, warningCount);
        unknownSignalCount = Math.max(0, unknownSignalCount);
        degradedSignalCount = Math.max(0, degradedSignalCount);
        scoreInterpretation = DecisionExplorerDtoSupport.valueOrUnknown(scoreInterpretation);
        summaryText = DecisionExplorerDtoSupport.valueOrUnknown(summaryText);
        strengthSignals = DecisionExplorerDtoSupport.copyOrEmpty(strengthSignals);
        weakSignals = DecisionExplorerDtoSupport.copyOrEmpty(weakSignals);
        degradedSignals = DecisionExplorerDtoSupport.copyOrEmpty(degradedSignals);
        unknownSignals = DecisionExplorerDtoSupport.copyOrEmpty(unknownSignals);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    public static DecisionExplorerCandidateDiagnosticV1 unknownSelected(String boundaryNote) {
        return new DecisionExplorerCandidateDiagnosticV1(
                "UNKNOWN",
                "UNKNOWN",
                false,
                0,
                ROLE_UNKNOWN,
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                RISK_UNKNOWN,
                DecisionExplorerCandidateConfidenceV1.UNKNOWN,
                "UNKNOWN",
                null,
                null,
                0,
                0,
                1,
                0,
                "SELECTED_CANDIDATE_UNKNOWN",
                "Selected candidate diagnostics could not be computed because candidate evidence was unavailable.",
                List.of(),
                List.of(),
                List.of(),
                List.of("selected candidate evidence was unavailable"),
                List.of("SELECTED_CANDIDATE_DIAGNOSTIC_UNKNOWN"),
                List.of(),
                boundaryNote);
    }

    static String riskFor(String diagnosticStatus, int warningCount, int unknownSignalCount,
            int degradedSignalCount, String scoreInterpretation) {
        String normalizedStatus = normalizeConfidenceStatus(diagnosticStatus);
        if (DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(normalizedStatus)
                || degradedSignalCount > 0
                || "ALTERNATIVE_BEATS_SELECTED".equals(scoreInterpretation)) {
            return RISK_HIGH;
        }
        if (DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(normalizedStatus)
                || warningCount > 0) {
            return RISK_REVIEW;
        }
        if (DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(normalizedStatus)) {
            return RISK_UNKNOWN;
        }
        return RISK_LOW;
    }

    private static String normalizeRole(String role) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(role);
        return switch (normalized) {
            case ROLE_SELECTED, ROLE_ALTERNATIVE, ROLE_UNKNOWN -> normalized;
            default -> ROLE_UNKNOWN;
        };
    }

    private static String normalizeRisk(String riskLevel) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(riskLevel);
        return switch (normalized) {
            case RISK_LOW, RISK_REVIEW, RISK_UNKNOWN, RISK_HIGH -> normalized;
            default -> RISK_UNKNOWN;
        };
    }

    private static String normalizeConfidenceStatus(String status) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(status);
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
            case DecisionExplorerCandidateConfidenceV1.HEALTHY,
                    DecisionExplorerCandidateConfidenceV1.DEGRADED,
                    DecisionExplorerCandidateConfidenceV1.UNKNOWN -> normalized;
            default -> DecisionExplorerCandidateConfidenceV1.UNKNOWN;
        };
    }

    private static Double finiteOrNull(Double value) {
        return value == null || !Double.isFinite(value) ? null : value;
    }
}
