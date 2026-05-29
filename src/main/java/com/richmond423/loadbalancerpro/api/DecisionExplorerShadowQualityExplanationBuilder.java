package com.richmond423.loadbalancerpro.api;

import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticListSupport.copyNonNull;

import java.util.List;

final class DecisionExplorerShadowQualityExplanationBuilder {
    String evidenceBasisSummary(
            String qualityLabel,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        return "Shadow decision-quality evaluation classified selected candidate "
                + DecisionExplorerDtoSupport.valueOrUnknown(summary.selectedCandidateId())
                + " as " + qualityLabel
                + " from confidence " + summary.status()
                + ", route tradeoff " + tradeoff.tradeoffCategory()
                + ", evidence sufficiency " + sufficiency.sufficiencyLevel()
                + ", and replay readiness " + replayReadiness.readinessStatus()
                + "; no production routing decision is changed.";
    }

    String selectedCandidateBasisSummary(
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff) {
        DecisionExplorerCandidateDiagnosticV1 selectedDiagnostic = diagnostics.selectedCandidateDiagnostic();
        String selectedId = selectedDiagnostic == null
                ? tradeoff.selectedCandidateId()
                : selectedDiagnostic.candidateId();
        return "Selected candidate " + DecisionExplorerDtoSupport.valueOrUnknown(selectedId)
                + " is evaluated as the returned decision baseline; closest alternative "
                + DecisionExplorerDtoSupport.valueOrUnknown(tradeoff.closestAlternativeCandidateId())
                + " has score delta " + value(tradeoff.closestAlternativeScoreDelta()) + ".";
    }

    String explanationText(
            String qualityLabel,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity,
            DecisionExplorerShadowScenarioInputQualityV1 scenarioInputQuality,
            String reproducibilityKey) {
        return "Shadow decision-quality explanation is " + qualityLabel
                + " for selected candidate "
                + DecisionExplorerDtoSupport.valueOrUnknown(summary.selectedCandidateId())
                + " from confidence " + summary.status()
                + ", route tradeoff " + tradeoff.tradeoffCategory()
                + ", evidence sufficiency " + sufficiency.sufficiencyLevel()
                + ", replay readiness " + replayReadiness.readinessStatus()
                + ", policy sensitivity " + policySensitivity.sensitivityLevel()
                + "/" + policySensitivity.sensitivityCategory()
                + ", and scenario input " + scenarioInputQuality.inputQualityLabel()
                + "/" + scenarioInputQuality.supportBand()
                + ". " + candidateOutcomeExplanation(candidateOutcomes)
                + " Reproducibility key " + DecisionExplorerDtoSupport.valueOrUnknown(reproducibilityKey)
                + " is derived from returned diagnostic fields only; no production routing decision is changed.";
    }

    private static String candidateOutcomeExplanation(
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes) {
        List<DecisionExplorerShadowCandidateOutcomeV1> rows = copyNonNull(candidateOutcomes);
        if (rows.isEmpty()) {
            return "Candidate outcome comparison rows were unavailable.";
        }
        DecisionExplorerShadowCandidateOutcomeV1 selected = rows.stream()
                .filter(DecisionExplorerShadowCandidateOutcomeV1::selected)
                .findFirst()
                .orElse(null);
        DecisionExplorerShadowCandidateOutcomeV1 reviewAlternative = rows.stream()
                .filter(row -> !row.selected())
                .filter(row -> DecisionExplorerShadowCandidateOutcomeV1.IMPACT_REVIEW_SIGNAL.equals(
                        row.qualityImpact())
                        || DecisionExplorerShadowCandidateOutcomeV1.IMPACT_RISK_SIGNAL.equals(row.qualityImpact())
                        || DecisionExplorerShadowCandidateOutcomeV1.IMPACT_UNKNOWN.equals(row.qualityImpact()))
                .findFirst()
                .orElse(null);
        String selectedText = selected == null
                ? "selected outcome UNKNOWN"
                : "selected outcome " + selected.outcomeLabel() + " for " + selected.candidateId();
        if (reviewAlternative == null) {
            return "Candidate outcomes show " + selectedText + " with no alternative review signal.";
        }
        return "Candidate outcomes show " + selectedText + " and alternative "
                + reviewAlternative.candidateId() + " as " + reviewAlternative.outcomeLabel()
                + "/" + reviewAlternative.qualityImpact() + ".";
    }

    private static String value(Double value) {
        return value == null || !Double.isFinite(value) ? "UNKNOWN" : value.toString();
    }

}
