package com.richmond423.loadbalancerpro.api;

import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticFingerprintSupport.input;
import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticListSupport.distinctSorted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class DecisionExplorerCounterfactualAnalysisService {
    private final DecisionExplorerCounterfactualLabelEvaluator labelEvaluator =
            new DecisionExplorerCounterfactualLabelEvaluator();
    private final DecisionExplorerCounterfactualPolicyWeightScenarioBuilder policyWeightScenarioBuilder =
            new DecisionExplorerCounterfactualPolicyWeightScenarioBuilder();
    private final DecisionExplorerCounterfactualCandidateOutcomeEvaluator candidateOutcomeEvaluator =
            new DecisionExplorerCounterfactualCandidateOutcomeEvaluator();
    private final DecisionExplorerCounterfactualFactorWeightDeltaEvaluator factorWeightDeltaEvaluator =
            new DecisionExplorerCounterfactualFactorWeightDeltaEvaluator();
    private final DecisionExplorerCounterfactualExplanationBuilder explanationBuilder =
            new DecisionExplorerCounterfactualExplanationBuilder();

    public DecisionExplorerCounterfactualAnalysisV1 buildAnalysis(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 routeTradeoffAnalysis,
            DecisionExplorerShadowDecisionQualityEvaluationV1 shadowDecisionQualityEvaluation,
            String boundaryNote) {
        if (confidenceSummary == null
                && routingDiagnostics == null
                && routeTradeoffAnalysis == null
                && shadowDecisionQualityEvaluation == null) {
            return DecisionExplorerCounterfactualAnalysisV1.unknown(boundaryNote);
        }

        DecisionExplorerConfidenceSummaryV1 summary = confidenceSummary == null
                ? DecisionExplorerConfidenceSummaryV1.unknown(boundaryNote)
                : confidenceSummary;
        DecisionExplorerRoutingDiagnosticsV1 diagnostics = routingDiagnostics == null
                ? DecisionExplorerRoutingDiagnosticsV1.unknown(boundaryNote)
                : routingDiagnostics;
        DecisionExplorerRouteTradeoffAnalysisV1 tradeoff = routeTradeoffAnalysis == null
                ? DecisionExplorerRouteTradeoffAnalysisV1.unknown(boundaryNote)
                : routeTradeoffAnalysis;
        DecisionExplorerShadowDecisionQualityEvaluationV1 quality = shadowDecisionQualityEvaluation == null
                ? DecisionExplorerShadowDecisionQualityEvaluationV1.unknown(boundaryNote)
                : shadowDecisionQualityEvaluation;
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = tradeoff.evidenceSufficiency();
        DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness = tradeoff.replayReadinessDiagnostic();

        String counterfactualLabel = labelEvaluator.label(summary, tradeoff, quality);
        List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> policyWeightScenarios =
                policyWeightScenarioBuilder.buildScenarios(summary, diagnostics, tradeoff, quality,
                        counterfactualLabel, boundaryNote);
        List<DecisionExplorerCounterfactualCandidateOutcomeV1> counterfactualCandidateOutcomes =
                candidateOutcomeEvaluator.evaluate(tradeoff, quality, policyWeightScenarios, boundaryNote);
        List<DecisionExplorerCounterfactualFactorWeightDeltaV1> factorWeightDeltas =
                factorWeightDeltaEvaluator.evaluate(tradeoff, policyWeightScenarios, boundaryNote);
        List<String> stableSignals = stableSignals(summary, tradeoff, quality, sufficiency, replayReadiness);
        List<String> sensitivitySignals = sensitivitySignals(summary, diagnostics, tradeoff, quality, replayReadiness);
        List<String> limitationSignals = limitationSignals(summary, diagnostics, tradeoff, quality, sufficiency,
                replayReadiness);
        List<String> reasonCodes = reasonCodes(counterfactualLabel, summary, diagnostics, tradeoff, quality,
                sufficiency, replayReadiness);
        List<String> warnings = warnings(summary, diagnostics, tradeoff, quality);
        List<String> unknowns = unknowns(summary, diagnostics, tradeoff, quality, replayReadiness);
        List<String> sourceReferenceIds = sourceReferenceIds(summary, diagnostics, tradeoff, quality);
        List<String> fingerprintInputs = fingerprintInputs(
                counterfactualLabel,
                summary,
                tradeoff,
                quality,
                sufficiency,
                replayReadiness,
                policyWeightScenarios,
                counterfactualCandidateOutcomes,
                factorWeightDeltas,
                stableSignals,
                sensitivitySignals,
                limitationSignals,
                reasonCodes,
                warnings,
                unknowns,
                sourceReferenceIds);
        String reproducibilityKey = reproducibilityKey(
                counterfactualLabel,
                summary,
                tradeoff,
                quality,
                sufficiency,
                replayReadiness,
                policyWeightScenarios,
                counterfactualCandidateOutcomes,
                factorWeightDeltas);
        String explanationText = explanationBuilder.build(
                counterfactualLabel,
                summary,
                tradeoff,
                sufficiency,
                replayReadiness,
                policyWeightScenarios,
                counterfactualCandidateOutcomes,
                factorWeightDeltas,
                reproducibilityKey);

        return new DecisionExplorerCounterfactualAnalysisV1(
                true,
                true,
                true,
                DecisionExplorerCounterfactualAnalysisV1.ANALYSIS_OBJECT,
                DecisionExplorerCounterfactualAnalysisV1.CONTRACT_VERSION,
                summary.selectedCandidateId(),
                counterfactualLabel,
                DecisionExplorerCounterfactualAnalysisV1.bandFor(counterfactualLabel),
                summary.status(),
                quality.qualityLabel(),
                tradeoff.tradeoffCategory(),
                sufficiency.sufficiencyLevel(),
                replayReadiness.readinessStatus(),
                "RETURNED_EVIDENCE_WEIGHTS",
                policyWeightScenarios,
                policyWeightScenarios.size(),
                counterfactualCandidateOutcomes,
                counterfactualCandidateOutcomes.size(),
                factorWeightDeltas,
                factorWeightDeltas.size(),
                quality.candidateOutcomeCount(),
                tradeoff.factorTradeoffDeltas().size(),
                explanationText,
                stableSignals,
                sensitivitySignals,
                limitationSignals,
                reasonCodes,
                warnings,
                unknowns,
                sourceReferenceIds,
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                DecisionExplorerDiagnosticFingerprintSupport.diagnosticFingerprint(
                        DecisionExplorerCounterfactualAnalysisV1.FINGERPRINT_NAMESPACE,
                        fingerprintInputs),
                reproducibilityKey,
                fingerprintInputs,
                boundaryNote);
    }

    private static List<String> stableSignals(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        List<String> signals = new ArrayList<>();
        if (DecisionExplorerConfidenceSummaryV1.STATUS_STRONG.equals(summary.status())) {
            signals.add("confidence status is STRONG");
        }
        if ("SELECTED_ADVANTAGE".equals(tradeoff.tradeoffCategory())) {
            signals.add("selected candidate has returned-evidence tradeoff advantage");
        }
        if (DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_ACCEPTABLE.equals(quality.qualityLabel())) {
            signals.add("shadow decision quality is ACCEPTABLE");
        }
        if (sufficiency.tradeoffAnalysisReady()) {
            signals.add("tradeoff evidence is ready for local analysis");
        }
        if (DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY.equals(replayReadiness.readinessStatus())) {
            signals.add("replay-readiness diagnostics are READY while replay execution remains unavailable");
        }
        return distinctSorted(signals);
    }

    private static List<String> sensitivitySignals(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        List<String> signals = new ArrayList<>();
        if (DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(summary.status())) {
            signals.add("confidence status is PARTIAL");
        }
        if ("CLOSE_ALTERNATIVE".equals(tradeoff.tradeoffCategory())
                || "SELECTED_CHALLENGED".equals(tradeoff.tradeoffCategory())
                || "PARTIAL_TRADEOFF".equals(tradeoff.tradeoffCategory())) {
            signals.add("route tradeoff category is " + tradeoff.tradeoffCategory());
        }
        if (tradeoff.closestAlternativeScoreDelta() != null
                && Math.abs(tradeoff.closestAlternativeScoreDelta()) <= 1.0d) {
            signals.add("closest alternative score delta is within local close-call band");
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
            signals.add("replay-readiness diagnostics are PARTIAL");
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
            limitations.add("confidence status is UNKNOWN");
        }
        if (DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(summary.status())) {
            limitations.add("confidence status is DEGRADED");
        }
        if (DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT.equals(sufficiency.sufficiencyLevel())) {
            limitations.add("evidence sufficiency is INSUFFICIENT");
        }
        if (DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED.equals(sufficiency.sufficiencyLevel())) {
            limitations.add("evidence sufficiency is DEGRADED");
        }
        if (DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_INSUFFICIENT_EVIDENCE.equals(
                quality.qualityLabel())) {
            limitations.add("shadow decision quality reports INSUFFICIENT_EVIDENCE");
        }
        limitations.addAll(diagnostics.degradationReasons());
        limitations.addAll(diagnostics.unknownEvidenceReasons());
        limitations.addAll(tradeoff.unknowns());
        limitations.addAll(replayReadiness.missingEvidenceSignals());
        limitations.addAll(replayReadiness.limitationSignals());
        return distinctSorted(limitations);
    }

    private static List<String> reasonCodes(
            String counterfactualLabel,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        List<String> reasons = new ArrayList<>();
        reasons.add("COUNTERFACTUAL_ANALYSIS_" + counterfactualLabel);
        reasons.add("CONFIDENCE_STATUS_" + summary.status());
        reasons.add("ROUTE_TRADEOFF_CATEGORY_" + tradeoff.tradeoffCategory());
        reasons.add("SHADOW_DECISION_QUALITY_" + quality.qualityLabel());
        reasons.add("EVIDENCE_SUFFICIENCY_" + sufficiency.sufficiencyLevel());
        reasons.add("REPLAY_READINESS_" + replayReadiness.readinessStatus());
        reasons.addAll(summary.statusReasons());
        reasons.addAll(diagnostics.diagnosticReasons());
        reasons.addAll(tradeoff.tradeoffReasons());
        reasons.addAll(quality.qualityReasons());
        return distinctSorted(reasons);
    }

    private static List<String> warnings(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality) {
        return distinctSorted(Stream.of(summary.warnings(), diagnostics.warnings(), tradeoff.warnings(),
                        quality.warnings())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList());
    }

    private static List<String> unknowns(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        return distinctSorted(Stream.of(summary.unknowns(), diagnostics.unknowns(), tradeoff.unknowns(),
                        quality.unknowns(), replayReadiness.missingEvidenceSignals())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList());
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

    private static List<String> fingerprintInputs(
            String counterfactualLabel,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> policyWeightScenarios,
            List<DecisionExplorerCounterfactualCandidateOutcomeV1> counterfactualCandidateOutcomes,
            List<DecisionExplorerCounterfactualFactorWeightDeltaV1> factorWeightDeltas,
            List<String> stableSignals,
            List<String> sensitivitySignals,
            List<String> limitationSignals,
            List<String> reasonCodes,
            List<String> warnings,
            List<String> unknowns,
            List<String> sourceReferenceIds) {
        return List.of(
                input("analysisObject", DecisionExplorerCounterfactualAnalysisV1.ANALYSIS_OBJECT),
                input("contractVersion", DecisionExplorerCounterfactualAnalysisV1.CONTRACT_VERSION),
                input("counterfactualLabel", counterfactualLabel),
                input("sensitivityBand", DecisionExplorerCounterfactualAnalysisV1.bandFor(counterfactualLabel)),
                input("selectedCandidateId", summary.selectedCandidateId()),
                input("confidenceStatus", summary.status()),
                input("decisionQualityLabel", quality.qualityLabel()),
                input("tradeoffCategory", tradeoff.tradeoffCategory()),
                input("evidenceSufficiencyLevel", sufficiency.sufficiencyLevel()),
                input("replayReadinessStatus", replayReadiness.readinessStatus()),
                input("policyWeightScenarioCount", policyWeightScenarios.size()),
                input("policyWeightScenarios", policyWeightScenarios.stream()
                        .map(DecisionExplorerCounterfactualPolicyWeightScenarioV1::fingerprintInput)
                        .toList()),
                input("counterfactualCandidateOutcomeCount", counterfactualCandidateOutcomes.size()),
                input("counterfactualCandidateOutcomes", counterfactualCandidateOutcomes.stream()
                        .map(DecisionExplorerCounterfactualCandidateOutcomeV1::fingerprintInput)
                        .toList()),
                input("factorWeightDeltaCount", factorWeightDeltas.size()),
                input("factorWeightDeltas", factorWeightDeltas.stream()
                        .map(DecisionExplorerCounterfactualFactorWeightDeltaV1::fingerprintInput)
                        .toList()),
                input("candidateOutcomeCount", quality.candidateOutcomeCount()),
                input("factorDeltaCount", tradeoff.factorTradeoffDeltas().size()),
                input("stableSignals", stableSignals),
                input("sensitivitySignals", sensitivitySignals),
                input("limitationSignals", limitationSignals),
                input("reasonCodes", reasonCodes),
                input("warnings", warnings),
                input("unknowns", unknowns),
                input("sourceReferenceIds", sourceReferenceIds));
    }

    private static String reproducibilityKey(
            String counterfactualLabel,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> policyWeightScenarios,
            List<DecisionExplorerCounterfactualCandidateOutcomeV1> counterfactualCandidateOutcomes,
            List<DecisionExplorerCounterfactualFactorWeightDeltaV1> factorWeightDeltas) {
        return "counterfactual:v1:" + counterfactualLabel + ":" + summary.selectedCandidateId()
                + ":" + tradeoff.tradeoffCategory()
                + ":quality=" + quality.qualityLabel()
                + ":sufficiency=" + sufficiency.sufficiencyLevel()
                + ":replay=" + replayReadiness.readinessStatus()
                + ":scenarios=" + policyWeightScenarios.size()
                + ":outcomes=" + counterfactualCandidateOutcomes.size()
                + ":factorWeightDeltas=" + factorWeightDeltas.size();
    }

}
