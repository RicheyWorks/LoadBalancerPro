package com.richmond423.loadbalancerpro.api;

final class DecisionExplorerShadowQualityLabelEvaluator {
    String qualityLabel(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        if (isInsufficient(sufficiency)) {
            return DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_INSUFFICIENT_EVIDENCE;
        }
        if (isDegraded(summary, diagnostics, tradeoff, sufficiency, replayReadiness)) {
            return DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_DEGRADED_DECISION;
        }
        if (isUnknown(summary, diagnostics, tradeoff, replayReadiness)) {
            return DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_UNKNOWN;
        }
        if (isReviewRecommended(summary, diagnostics, tradeoff, sufficiency, replayReadiness)) {
            return DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_REVIEW_RECOMMENDED;
        }
        return DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_ACCEPTABLE;
    }

    int qualityScore(String qualityLabel, DecisionExplorerEvidenceSufficiencyV1 sufficiency) {
        int readiness = sufficiency.readinessScore();
        return switch (qualityLabel) {
            case DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_ACCEPTABLE -> Math.max(85, readiness);
            case DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_REVIEW_RECOMMENDED ->
                    Math.max(45, Math.min(75, readiness));
            case DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_DEGRADED_DECISION ->
                    Math.max(15, Math.min(40, readiness));
            case DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_INSUFFICIENT_EVIDENCE ->
                    Math.min(30, readiness);
            default -> 0;
        };
    }

    private static boolean isInsufficient(DecisionExplorerEvidenceSufficiencyV1 sufficiency) {
        return DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT.equals(sufficiency.sufficiencyLevel());
    }

    private static boolean isDegraded(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        return DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(summary.status())
                || DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(diagnostics.overallStatus())
                || DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(tradeoff.overallStatus())
                || DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED.equals(sufficiency.sufficiencyLevel())
                || DecisionExplorerReplayReadinessDiagnosticV1.STATUS_DEGRADED.equals(
                        replayReadiness.readinessStatus())
                || tradeoff.candidateTradeoffs().stream().anyMatch(row -> row.selected()
                        && (DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(row.diagnosticStatus())
                                || !row.degradedSignals().isEmpty()
                                || DecisionExplorerCandidateDiagnosticV1.RISK_HIGH.equals(row.riskLevel())));
    }

    private static boolean isUnknown(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        return DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(summary.status())
                || DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(diagnostics.overallStatus())
                || DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(tradeoff.overallStatus())
                || DecisionExplorerRouteTradeoffRowV1.TRADEOFF_UNKNOWN.equals(tradeoff.tradeoffCategory())
                || DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN.equals(
                        replayReadiness.readinessStatus());
    }

    private static boolean isReviewRecommended(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        return DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(summary.status())
                || DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(diagnostics.overallStatus())
                || DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(tradeoff.overallStatus())
                || DecisionExplorerEvidenceSufficiencyV1.LEVEL_BASIC_DIAGNOSTICS_ONLY.equals(
                        sufficiency.sufficiencyLevel())
                || DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL.equals(
                        replayReadiness.readinessStatus())
                || "SELECTED_CHALLENGED".equals(tradeoff.tradeoffCategory())
                || "CLOSE_ALTERNATIVE".equals(tradeoff.tradeoffCategory())
                || "PARTIAL_TRADEOFF".equals(tradeoff.tradeoffCategory())
                || "NO_ALTERNATIVE".equals(tradeoff.tradeoffCategory())
                || tradeoff.candidateTradeoffs().stream().anyMatch(row -> !row.selected()
                        && (DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_RISK.equals(
                                row.riskBenefitClassification())
                                || DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_TRADEOFF.equals(
                                        row.riskBenefitClassification())
                                || DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_BEATS_SELECTED.equals(
                                        row.tradeoffCategory())
                                || DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_CLOSE.equals(
                                        row.tradeoffCategory())));
    }
}
