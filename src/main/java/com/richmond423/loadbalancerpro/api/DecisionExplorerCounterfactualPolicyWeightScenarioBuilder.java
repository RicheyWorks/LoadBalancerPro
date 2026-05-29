package com.richmond423.loadbalancerpro.api;

import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticListSupport.distinctSorted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

final class DecisionExplorerCounterfactualPolicyWeightScenarioBuilder {
    List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> buildScenarios(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            String counterfactualLabel,
            String boundaryNote) {
        if (hasNoComputedCounterfactualBasis(summary, diagnostics, tradeoff, quality, counterfactualLabel)) {
            return List.of();
        }

        DecisionExplorerEvidenceSufficiencyV1 sufficiency = tradeoff.evidenceSufficiency();
        DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness = tradeoff.replayReadinessDiagnostic();
        List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios = new ArrayList<>();
        scenarios.add(scenario(
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_BASELINE_RETURNED_EVIDENCE,
                1,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_RETURNED_EVIDENCE_WEIGHTS,
                0,
                counterfactualLabel,
                summary,
                diagnostics,
                tradeoff,
                quality,
                sufficiency,
                replayReadiness,
                boundaryNote));

        if (isInsufficientEvidence(sufficiency, quality)) {
            return scenarios;
        }

        scenarios.add(scenario(
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_SELECTED_SUPPORT_PLUS_10,
                2,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_LOCAL_SELECTED_SUPPORT_PLUS_10,
                10,
                selectedSupportLabel(counterfactualLabel, summary, tradeoff, quality, sufficiency, replayReadiness),
                summary,
                diagnostics,
                tradeoff,
                quality,
                sufficiency,
                replayReadiness,
                boundaryNote));
        scenarios.add(scenario(
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_ALTERNATIVE_SUPPORT_PLUS_10,
                3,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_LOCAL_ALTERNATIVE_SUPPORT_PLUS_10,
                10,
                alternativeSupportLabel(counterfactualLabel, tradeoff, quality, sufficiency, replayReadiness),
                summary,
                diagnostics,
                tradeoff,
                quality,
                sufficiency,
                replayReadiness,
                boundaryNote));

        return scenarios.stream()
                .sorted(Comparator.comparingInt(DecisionExplorerCounterfactualPolicyWeightScenarioV1::displayOrder)
                        .thenComparing(DecisionExplorerCounterfactualPolicyWeightScenarioV1::scenarioId))
                .toList();
    }

    private static DecisionExplorerCounterfactualPolicyWeightScenarioV1 scenario(
            String scenarioId,
            int displayOrder,
            String policyWeightProfile,
            int localAssumptionWeightShiftPercent,
            String label,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            String boundaryNote) {
        List<String> stabilizingSignals = stabilizingSignals(scenarioId, summary, tradeoff, quality, sufficiency,
                replayReadiness);
        List<String> sensitivitySignals = sensitivitySignals(scenarioId, summary, diagnostics, tradeoff, quality,
                replayReadiness);
        List<String> limitationSignals = limitationSignals(summary, diagnostics, tradeoff, quality, sufficiency,
                replayReadiness);
        List<String> reasonCodes = reasonCodes(scenarioId, label, summary, tradeoff, quality, sufficiency,
                replayReadiness);
        return new DecisionExplorerCounterfactualPolicyWeightScenarioV1(
                true,
                true,
                true,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_OBJECT,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.CONTRACT_VERSION,
                scenarioId,
                displayOrder,
                policyWeightProfile,
                localAssumptionWeightShiftPercent,
                label,
                DecisionExplorerCounterfactualAnalysisV1.bandFor(label),
                summary.selectedCandidateId(),
                referenceAlternativeCandidateId(tradeoff),
                tradeoff.closestAlternativeScoreDelta(),
                selectedCandidateRemainsSupported(label, tradeoff),
                alternativeBecomesCloseOrPreferable(label, tradeoff),
                tradeoff.tradeoffCategory(),
                sufficiency.sufficiencyLevel(),
                replayReadiness.readinessStatus(),
                summaryText(scenarioId, label, summary, tradeoff, sufficiency, replayReadiness),
                stabilizingSignals,
                sensitivitySignals,
                limitationSignals,
                reasonCodes,
                sourceReferenceIds(summary, diagnostics, tradeoff, quality),
                boundaryNote);
    }

    private static boolean hasNoComputedCounterfactualBasis(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            String counterfactualLabel) {
        return DecisionExplorerCounterfactualAnalysisV1.LABEL_UNKNOWN.equals(counterfactualLabel)
                && DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(summary.status())
                && "UNKNOWN".equals(tradeoff.tradeoffCategory())
                && tradeoff.candidateTradeoffCount() == 0
                && quality.candidateOutcomeCount() == 0
                && DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_UNKNOWN.equals(quality.qualityLabel())
                && sourceReferenceIds(summary, diagnostics, tradeoff, quality).isEmpty();
    }

    private static String selectedSupportLabel(
            String counterfactualLabel,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        if (isDegraded(summary, tradeoff, quality, sufficiency, replayReadiness)) {
            return DecisionExplorerCounterfactualAnalysisV1.LABEL_DEGRADED;
        }
        if (DecisionExplorerCounterfactualAnalysisV1.LABEL_CLOSE_CALL.equals(counterfactualLabel)
                || "SELECTED_CHALLENGED".equals(tradeoff.tradeoffCategory())) {
            return DecisionExplorerCounterfactualAnalysisV1.LABEL_SENSITIVE;
        }
        return counterfactualLabel;
    }

    private static String alternativeSupportLabel(
            String counterfactualLabel,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        if (isDegraded(null, tradeoff, quality, sufficiency, replayReadiness)) {
            return DecisionExplorerCounterfactualAnalysisV1.LABEL_DEGRADED;
        }
        if (isInsufficientEvidence(sufficiency, quality)) {
            return DecisionExplorerCounterfactualAnalysisV1.LABEL_INSUFFICIENT_EVIDENCE;
        }
        if (tradeoff.alternativeCount() == 0) {
            return DecisionExplorerCounterfactualAnalysisV1.LABEL_UNKNOWN;
        }
        if (alternativeBecomesCloseOrPreferable(counterfactualLabel, tradeoff)) {
            return DecisionExplorerCounterfactualAnalysisV1.LABEL_CLOSE_CALL;
        }
        return counterfactualLabel;
    }

    private static List<String> stabilizingSignals(
            String scenarioId,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        List<String> signals = new ArrayList<>();
        if (DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_BASELINE_RETURNED_EVIDENCE.equals(
                scenarioId)) {
            signals.add("baseline uses returned evidence weights without changing production scoring");
        }
        if (DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_SELECTED_SUPPORT_PLUS_10.equals(
                scenarioId)) {
            signals.add("local selected-support scenario applies a bounded +10 percent diagnostic assumption");
        }
        if (DecisionExplorerConfidenceSummaryV1.STATUS_STRONG.equals(summary.status())) {
            signals.add("confidence summary is STRONG");
        }
        if ("SELECTED_ADVANTAGE".equals(tradeoff.tradeoffCategory())) {
            signals.add("selected candidate has returned-evidence tradeoff advantage");
        }
        if (DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_ACCEPTABLE.equals(quality.qualityLabel())) {
            signals.add("shadow decision quality is ACCEPTABLE");
        }
        if (sufficiency.tradeoffAnalysisReady()) {
            signals.add("tradeoff evidence is sufficient for bounded local sensitivity analysis");
        }
        if (DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY.equals(replayReadiness.readinessStatus())) {
            signals.add("replay-readiness is READY while replay execution remains unavailable");
        }
        return distinctSorted(signals);
    }

    private static List<String> sensitivitySignals(
            String scenarioId,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        List<String> signals = new ArrayList<>();
        if (DecisionExplorerCounterfactualPolicyWeightScenarioV1.SCENARIO_ALTERNATIVE_SUPPORT_PLUS_10.equals(
                scenarioId)) {
            signals.add("local alternative-support scenario applies a bounded +10 percent diagnostic assumption");
        }
        if (DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(summary.status())) {
            signals.add("confidence summary is PARTIAL");
        }
        if ("CLOSE_ALTERNATIVE".equals(tradeoff.tradeoffCategory())
                || "SELECTED_CHALLENGED".equals(tradeoff.tradeoffCategory())
                || "PARTIAL_TRADEOFF".equals(tradeoff.tradeoffCategory())) {
            signals.add("route tradeoff category is " + tradeoff.tradeoffCategory());
        }
        if (tradeoff.closestAlternativeScoreDelta() != null
                && Math.abs(tradeoff.closestAlternativeScoreDelta()) <= 1.0d) {
            signals.add("closest alternative is inside the local close-call score band");
        }
        DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity =
                quality.policySensitivityDiagnostic();
        if (!DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_LOW.equals(
                policySensitivity.sensitivityLevel())
                && !DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_UNKNOWN.equals(
                        policySensitivity.sensitivityLevel())) {
            signals.add("shadow policy sensitivity level is " + policySensitivity.sensitivityLevel());
        }
        if (DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL.equals(replayReadiness.readinessStatus())) {
            signals.add("replay-readiness is PARTIAL");
        }
        signals.addAll(diagnostics.partialEvidenceReasons());
        return distinctSorted(signals);
    }

    private static List<String> limitationSignals(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        List<String> limitations = new ArrayList<>();
        if (DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(summary.status())) {
            limitations.add("confidence summary is UNKNOWN");
        }
        if (DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(summary.status())) {
            limitations.add("confidence summary is DEGRADED");
        }
        if (DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT.equals(sufficiency.sufficiencyLevel())) {
            limitations.add("evidence sufficiency is INSUFFICIENT");
        }
        if (DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED.equals(sufficiency.sufficiencyLevel())) {
            limitations.add("evidence sufficiency is DEGRADED");
        }
        if (DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_INSUFFICIENT_EVIDENCE.equals(
                quality.qualityLabel())) {
            limitations.add("shadow decision quality is INSUFFICIENT_EVIDENCE");
        }
        limitations.addAll(diagnostics.degradationReasons());
        limitations.addAll(diagnostics.unknownEvidenceReasons());
        limitations.addAll(tradeoff.unknowns());
        limitations.addAll(replayReadiness.missingEvidenceSignals());
        limitations.addAll(replayReadiness.limitationSignals());
        return distinctSorted(limitations);
    }

    private static List<String> reasonCodes(
            String scenarioId,
            String label,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        List<String> reasons = new ArrayList<>();
        reasons.add("COUNTERFACTUAL_POLICY_WEIGHT_SCENARIO_" + scenarioId);
        reasons.add("COUNTERFACTUAL_POLICY_WEIGHT_LABEL_" + label);
        reasons.add("CONFIDENCE_STATUS_" + summary.status());
        reasons.add("ROUTE_TRADEOFF_CATEGORY_" + tradeoff.tradeoffCategory());
        reasons.add("SHADOW_DECISION_QUALITY_" + quality.qualityLabel());
        reasons.add("EVIDENCE_SUFFICIENCY_" + sufficiency.sufficiencyLevel());
        reasons.add("REPLAY_READINESS_" + replayReadiness.readinessStatus());
        return distinctSorted(reasons);
    }

    private static List<String> sourceReferenceIds(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality) {
        return distinctSorted(Stream.of(summary.sourceReferenceIds(), diagnostics.sourceReferenceIds(),
                        tradeoff.sourceReferenceIds(), quality.sourceReferenceIds())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList());
    }

    private static boolean selectedCandidateRemainsSupported(
            String label,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff) {
        return DecisionExplorerCounterfactualAnalysisV1.LABEL_STABLE.equals(label)
                || DecisionExplorerCounterfactualAnalysisV1.LABEL_SENSITIVE.equals(label)
                || DecisionExplorerCounterfactualAnalysisV1.LABEL_CLOSE_CALL.equals(label)
                        && !"SELECTED_CHALLENGED".equals(tradeoff.tradeoffCategory());
    }

    private static boolean alternativeBecomesCloseOrPreferable(
            String label,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff) {
        return DecisionExplorerCounterfactualAnalysisV1.LABEL_CLOSE_CALL.equals(label)
                || "CLOSE_ALTERNATIVE".equals(tradeoff.tradeoffCategory())
                || "SELECTED_CHALLENGED".equals(tradeoff.tradeoffCategory())
                || tradeoff.closestAlternativeScoreDelta() != null
                        && tradeoff.closestAlternativeScoreDelta() <= 1.0d;
    }

    private static String referenceAlternativeCandidateId(DecisionExplorerRouteTradeoffAnalysisV1 tradeoff) {
        if (!"UNKNOWN".equals(tradeoff.closestAlternativeCandidateId())) {
            return tradeoff.closestAlternativeCandidateId();
        }
        return tradeoff.candidateTradeoffs().stream()
                .filter(row -> !row.selected())
                .map(DecisionExplorerRouteTradeoffRowV1::candidateId)
                .findFirst()
                .orElse("UNKNOWN");
    }

    private static boolean isInsufficientEvidence(
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality) {
        return DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT.equals(sufficiency.sufficiencyLevel())
                || DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_INSUFFICIENT_EVIDENCE.equals(
                        quality.qualityLabel());
    }

    private static boolean isDegraded(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        return summary != null && DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(summary.status())
                || "DEGRADED".equals(tradeoff.tradeoffCategory())
                || DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_DEGRADED_DECISION.equals(
                        quality.qualityLabel())
                || DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED.equals(sufficiency.sufficiencyLevel())
                || DecisionExplorerReplayReadinessDiagnosticV1.STATUS_DEGRADED.equals(
                        replayReadiness.readinessStatus());
    }

    private static String summaryText(
            String scenarioId,
            String label,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        return "Local policy-weight scenario " + scenarioId + " classifies selected candidate "
                + summary.selectedCandidateId() + " as " + label
                + " from returned-evidence tradeoff " + tradeoff.tradeoffCategory()
                + ", closest alternative " + referenceAlternativeCandidateId(tradeoff)
                + ", evidence sufficiency " + sufficiency.sufficiencyLevel()
                + ", and replay-readiness " + replayReadiness.readinessStatus()
                + "; the scenario is diagnostic-only and does not change production scoring or routing.";
    }
}
