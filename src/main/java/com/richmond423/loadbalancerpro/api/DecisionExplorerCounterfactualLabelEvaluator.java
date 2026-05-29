package com.richmond423.loadbalancerpro.api;

final class DecisionExplorerCounterfactualLabelEvaluator {
    String label(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRouteTradeoffAnalysisV1 routeTradeoffAnalysis,
            DecisionExplorerShadowDecisionQualityEvaluationV1 shadowDecisionQualityEvaluation) {
        if (confidenceSummary == null && routeTradeoffAnalysis == null && shadowDecisionQualityEvaluation == null) {
            return DecisionExplorerCounterfactualAnalysisV1.LABEL_UNKNOWN;
        }

        DecisionExplorerEvidenceSufficiencyV1 sufficiency = routeTradeoffAnalysis == null
                ? null
                : routeTradeoffAnalysis.evidenceSufficiency();
        DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness = routeTradeoffAnalysis == null
                ? null
                : routeTradeoffAnalysis.replayReadinessDiagnostic();
        if (isDegraded(confidenceSummary, routeTradeoffAnalysis, shadowDecisionQualityEvaluation,
                sufficiency, replayReadiness)) {
            return DecisionExplorerCounterfactualAnalysisV1.LABEL_DEGRADED;
        }
        if (isUnknown(confidenceSummary, routeTradeoffAnalysis, shadowDecisionQualityEvaluation)) {
            return DecisionExplorerCounterfactualAnalysisV1.LABEL_UNKNOWN;
        }
        if (isInsufficient(shadowDecisionQualityEvaluation, sufficiency)) {
            return DecisionExplorerCounterfactualAnalysisV1.LABEL_INSUFFICIENT_EVIDENCE;
        }
        if (isCloseCall(routeTradeoffAnalysis, shadowDecisionQualityEvaluation)) {
            return DecisionExplorerCounterfactualAnalysisV1.LABEL_CLOSE_CALL;
        }
        if (isStable(confidenceSummary, routeTradeoffAnalysis, shadowDecisionQualityEvaluation, sufficiency,
                replayReadiness)) {
            return DecisionExplorerCounterfactualAnalysisV1.LABEL_STABLE;
        }
        if (isSensitive(confidenceSummary, routeTradeoffAnalysis, shadowDecisionQualityEvaluation, replayReadiness)) {
            return DecisionExplorerCounterfactualAnalysisV1.LABEL_SENSITIVE;
        }
        return DecisionExplorerCounterfactualAnalysisV1.LABEL_UNKNOWN;
    }

    private static boolean isDegraded(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRouteTradeoffAnalysisV1 routeTradeoffAnalysis,
            DecisionExplorerShadowDecisionQualityEvaluationV1 shadowDecisionQualityEvaluation,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        return hasStatus(confidenceSummary, DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED)
                || hasTradeoffCategory(routeTradeoffAnalysis, "DEGRADED")
                || hasQualityLabel(shadowDecisionQualityEvaluation,
                        DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_DEGRADED_DECISION)
                || hasSufficiency(sufficiency, DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED)
                || hasReplayReadiness(replayReadiness, DecisionExplorerReplayReadinessDiagnosticV1.STATUS_DEGRADED);
    }

    private static boolean isUnknown(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRouteTradeoffAnalysisV1 routeTradeoffAnalysis,
            DecisionExplorerShadowDecisionQualityEvaluationV1 shadowDecisionQualityEvaluation) {
        return hasStatus(confidenceSummary, DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN)
                && hasTradeoffCategory(routeTradeoffAnalysis, "UNKNOWN")
                && hasQualityLabel(shadowDecisionQualityEvaluation,
                        DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_UNKNOWN);
    }

    private static boolean isInsufficient(
            DecisionExplorerShadowDecisionQualityEvaluationV1 shadowDecisionQualityEvaluation,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency) {
        return hasQualityLabel(shadowDecisionQualityEvaluation,
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_INSUFFICIENT_EVIDENCE)
                || hasSufficiency(sufficiency, DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT);
    }

    private static boolean isCloseCall(
            DecisionExplorerRouteTradeoffAnalysisV1 routeTradeoffAnalysis,
            DecisionExplorerShadowDecisionQualityEvaluationV1 shadowDecisionQualityEvaluation) {
        DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity =
                shadowDecisionQualityEvaluation == null
                        ? null
                        : shadowDecisionQualityEvaluation.policySensitivityDiagnostic();
        return hasTradeoffCategory(routeTradeoffAnalysis, "CLOSE_ALTERNATIVE")
                || hasTradeoffCategory(routeTradeoffAnalysis, "SELECTED_CHALLENGED")
                || policySensitivity != null
                        && DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_CLOSE_ALTERNATIVE.equals(
                                policySensitivity.sensitivityCategory());
    }

    private static boolean isSensitive(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRouteTradeoffAnalysisV1 routeTradeoffAnalysis,
            DecisionExplorerShadowDecisionQualityEvaluationV1 shadowDecisionQualityEvaluation,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity =
                shadowDecisionQualityEvaluation == null
                        ? null
                        : shadowDecisionQualityEvaluation.policySensitivityDiagnostic();
        return hasStatus(confidenceSummary, DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL)
                || hasTradeoffCategory(routeTradeoffAnalysis, "PARTIAL_TRADEOFF")
                || hasReplayReadiness(replayReadiness, DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL)
                || policySensitivity != null
                        && DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_MEDIUM.equals(
                                policySensitivity.sensitivityLevel());
    }

    private static boolean isStable(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRouteTradeoffAnalysisV1 routeTradeoffAnalysis,
            DecisionExplorerShadowDecisionQualityEvaluationV1 shadowDecisionQualityEvaluation,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        return hasStatus(confidenceSummary, DecisionExplorerConfidenceSummaryV1.STATUS_STRONG)
                && hasTradeoffCategory(routeTradeoffAnalysis, "SELECTED_ADVANTAGE")
                && hasQualityLabel(shadowDecisionQualityEvaluation,
                        DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_ACCEPTABLE)
                && (hasSufficiency(sufficiency, DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY)
                        || hasSufficiency(sufficiency, DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY))
                && hasReplayReadiness(replayReadiness, DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY);
    }

    private static boolean hasStatus(DecisionExplorerConfidenceSummaryV1 summary, String status) {
        return summary != null && status.equals(summary.status());
    }

    private static boolean hasTradeoffCategory(DecisionExplorerRouteTradeoffAnalysisV1 analysis, String category) {
        return analysis != null && category.equals(analysis.tradeoffCategory());
    }

    private static boolean hasQualityLabel(
            DecisionExplorerShadowDecisionQualityEvaluationV1 evaluation,
            String label) {
        return evaluation != null && label.equals(evaluation.qualityLabel());
    }

    private static boolean hasSufficiency(DecisionExplorerEvidenceSufficiencyV1 sufficiency, String level) {
        return sufficiency != null && level.equals(sufficiency.sufficiencyLevel());
    }

    private static boolean hasReplayReadiness(
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            String status) {
        return replayReadiness != null && status.equals(replayReadiness.readinessStatus());
    }
}
