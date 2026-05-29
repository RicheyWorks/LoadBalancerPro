package com.richmond423.loadbalancerpro.api;

import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticListSupport.copyNonNull;

import java.util.List;

final class DecisionExplorerCounterfactualExplanationBuilder {
    String build(
            String counterfactualLabel,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> policyWeightScenarios,
            List<DecisionExplorerCounterfactualCandidateOutcomeV1> candidateOutcomes,
            List<DecisionExplorerCounterfactualFactorWeightDeltaV1> factorWeightDeltas,
            String reproducibilityKey) {
        return "Counterfactual explanation: selected candidate "
                + DecisionExplorerDtoSupport.valueOrUnknown(summary == null ? null : summary.selectedCandidateId())
                + " is " + DecisionExplorerDtoSupport.valueOrUnknown(counterfactualLabel)
                + " with confidence " + DecisionExplorerDtoSupport.valueOrUnknown(
                        summary == null ? null : summary.status())
                + ", route tradeoff " + DecisionExplorerDtoSupport.valueOrUnknown(
                        tradeoff == null ? null : tradeoff.tradeoffCategory())
                + ", evidence sufficiency " + DecisionExplorerDtoSupport.valueOrUnknown(
                        sufficiency == null ? null : sufficiency.sufficiencyLevel())
                + ", and replay readiness " + DecisionExplorerDtoSupport.valueOrUnknown(
                        replayReadiness == null ? null : replayReadiness.readinessStatus())
                + ". " + policyScenarioExplanation(policyWeightScenarios)
                + " " + candidateOutcomeExplanation(candidateOutcomes)
                + " " + factorWeightDeltaExplanation(factorWeightDeltas)
                + " Reproducibility key " + DecisionExplorerDtoSupport.valueOrUnknown(reproducibilityKey)
                + " is derived from returned diagnostic fields only; no production routing, scoring, proxying, "
                + "replay execution, storage, export, or traffic shifting is performed.";
    }

    private static String policyScenarioExplanation(
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> policyWeightScenarios) {
        List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios = copyNonNull(policyWeightScenarios);
        if (scenarios.isEmpty()) {
            return "Policy-weight scenarios were unavailable.";
        }
        DecisionExplorerCounterfactualPolicyWeightScenarioV1 baseline = scenarios.stream()
                .filter(scenario -> DecisionExplorerCounterfactualPolicyWeightScenarioV1
                        .SCENARIO_BASELINE_RETURNED_EVIDENCE.equals(scenario.scenarioId()))
                .findFirst()
                .orElse(scenarios.get(0));
        DecisionExplorerCounterfactualPolicyWeightScenarioV1 alternativeSupport = scenarios.stream()
                .filter(scenario -> DecisionExplorerCounterfactualPolicyWeightScenarioV1
                        .SCENARIO_ALTERNATIVE_SUPPORT_PLUS_10.equals(scenario.scenarioId()))
                .findFirst()
                .orElse(null);
        String alternativeText = alternativeSupport == null
                ? "alternative-support scenario unavailable"
                : "alternative-support " + alternativeSupport.scenarioId() + "/"
                        + alternativeSupport.sensitivityLabel()
                        + " with alternativeClose="
                        + alternativeSupport.alternativeBecomesCloseOrPreferable();
        return "Policy scenarios show baseline " + baseline.scenarioId() + "/" + baseline.sensitivityLabel()
                + " across " + scenarios.size() + " bounded local scenarios and " + alternativeText + ".";
    }

    private static String candidateOutcomeExplanation(
            List<DecisionExplorerCounterfactualCandidateOutcomeV1> candidateOutcomes) {
        List<DecisionExplorerCounterfactualCandidateOutcomeV1> outcomes = copyNonNull(candidateOutcomes);
        if (outcomes.isEmpty()) {
            return "Counterfactual candidate outcomes were unavailable.";
        }
        DecisionExplorerCounterfactualCandidateOutcomeV1 selected = outcomes.stream()
                .filter(DecisionExplorerCounterfactualCandidateOutcomeV1::selected)
                .findFirst()
                .orElse(null);
        DecisionExplorerCounterfactualCandidateOutcomeV1 alternative = outcomes.stream()
                .filter(row -> !row.selected())
                .filter(row -> DecisionExplorerCounterfactualCandidateOutcomeV1
                        .OUTCOME_ALTERNATIVE_CHALLENGES_SELECTED.equals(row.counterfactualOutcomeLabel())
                        || DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_ALTERNATIVE_CLOSE_CALL.equals(
                                row.counterfactualOutcomeLabel())
                        || DecisionExplorerCounterfactualCandidateOutcomeV1.OUTCOME_ALTERNATIVE_UNKNOWN.equals(
                                row.counterfactualOutcomeLabel()))
                .findFirst()
                .orElseGet(() -> outcomes.stream()
                        .filter(row -> !row.selected())
                        .findFirst()
                        .orElse(null));
        String selectedText = selected == null
                ? "selected outcome UNKNOWN"
                : "selected " + selected.candidateId() + " as " + selected.counterfactualOutcomeLabel();
        if (alternative == null) {
            return "Candidate outcomes show " + selectedText + " with no alternative outcome row.";
        }
        return "Candidate outcomes show " + selectedText + " and alternative "
                + alternative.candidateId() + " as " + alternative.counterfactualOutcomeLabel() + ".";
    }

    private static String factorWeightDeltaExplanation(
            List<DecisionExplorerCounterfactualFactorWeightDeltaV1> factorWeightDeltas) {
        List<DecisionExplorerCounterfactualFactorWeightDeltaV1> deltas = copyNonNull(factorWeightDeltas);
        if (deltas.isEmpty()) {
            return "Factor-weight deltas were unavailable.";
        }
        DecisionExplorerCounterfactualFactorWeightDeltaV1 primary = primaryFactorDelta(deltas);
        return "Factor-weight deltas show STABILIZING="
                + count(deltas, DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_STABILIZING)
                + ", DESTABILIZING="
                + count(deltas, DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_DESTABILIZING)
                + ", NEUTRAL="
                + count(deltas, DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_NEUTRAL)
                + ", DEGRADED="
                + count(deltas, DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_DEGRADED)
                + ", UNKNOWN="
                + count(deltas, DecisionExplorerCounterfactualFactorWeightDeltaV1.CLASSIFICATION_UNKNOWN)
                + " with primary factor " + primary.factorName() + "/"
                + primary.factorWeightDeltaClassification() + ".";
    }

    private static DecisionExplorerCounterfactualFactorWeightDeltaV1 primaryFactorDelta(
            List<DecisionExplorerCounterfactualFactorWeightDeltaV1> deltas) {
        return deltas.stream()
                .filter(delta -> DecisionExplorerCounterfactualFactorWeightDeltaV1
                        .CLASSIFICATION_DESTABILIZING.equals(delta.factorWeightDeltaClassification()))
                .findFirst()
                .or(() -> deltas.stream()
                        .filter(delta -> DecisionExplorerCounterfactualFactorWeightDeltaV1
                                .CLASSIFICATION_DEGRADED.equals(delta.factorWeightDeltaClassification()))
                        .findFirst())
                .or(() -> deltas.stream()
                        .filter(delta -> DecisionExplorerCounterfactualFactorWeightDeltaV1
                                .CLASSIFICATION_UNKNOWN.equals(delta.factorWeightDeltaClassification()))
                        .findFirst())
                .or(() -> deltas.stream()
                        .filter(delta -> DecisionExplorerCounterfactualFactorWeightDeltaV1
                                .CLASSIFICATION_STABILIZING.equals(delta.factorWeightDeltaClassification()))
                        .findFirst())
                .orElse(deltas.get(0));
    }

    private static long count(
            List<DecisionExplorerCounterfactualFactorWeightDeltaV1> deltas,
            String classification) {
        return deltas.stream()
                .filter(delta -> classification.equals(delta.factorWeightDeltaClassification()))
                .count();
    }
}
