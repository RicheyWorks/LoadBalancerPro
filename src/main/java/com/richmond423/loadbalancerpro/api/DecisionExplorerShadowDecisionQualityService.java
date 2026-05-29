package com.richmond423.loadbalancerpro.api;

import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticListSupport.distinctSorted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class DecisionExplorerShadowDecisionQualityService {
    private final DecisionExplorerShadowQualityLabelEvaluator qualityLabelEvaluator =
            new DecisionExplorerShadowQualityLabelEvaluator();
    private final DecisionExplorerShadowCandidateOutcomeBuilder candidateOutcomeBuilder =
            new DecisionExplorerShadowCandidateOutcomeBuilder();
    private final DecisionExplorerShadowPolicySensitivityEvaluator policySensitivityEvaluator =
            new DecisionExplorerShadowPolicySensitivityEvaluator();
    private final DecisionExplorerShadowScenarioInputQualityEvaluator scenarioInputQualityEvaluator =
            new DecisionExplorerShadowScenarioInputQualityEvaluator();
    private final DecisionExplorerShadowQualityFingerprintBuilder fingerprintBuilder =
            new DecisionExplorerShadowQualityFingerprintBuilder();
    private final DecisionExplorerShadowQualityExplanationBuilder explanationBuilder =
            new DecisionExplorerShadowQualityExplanationBuilder();

    public DecisionExplorerShadowDecisionQualityEvaluationV1 buildEvaluation(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 routeTradeoffAnalysis,
            String boundaryNote) {
        if (confidenceSummary == null && routingDiagnostics == null && routeTradeoffAnalysis == null) {
            return DecisionExplorerShadowDecisionQualityEvaluationV1.unknown(boundaryNote);
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
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = tradeoff.evidenceSufficiency();
        DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness = tradeoff.replayReadinessDiagnostic();

        String qualityLabel = qualityLabelEvaluator.qualityLabel(
                summary, diagnostics, tradeoff, sufficiency, replayReadiness);
        int qualityScore = qualityLabelEvaluator.qualityScore(qualityLabel, sufficiency);
        List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes =
                candidateOutcomeBuilder.build(tradeoff.candidateTradeoffs(), boundaryNote);
        DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity =
                policySensitivityEvaluator.evaluate(summary, diagnostics, tradeoff, sufficiency, replayReadiness,
                        candidateOutcomes, boundaryNote);
        DecisionExplorerShadowScenarioInputQualityV1 scenarioInputQuality =
                scenarioInputQualityEvaluator.evaluate(summary, diagnostics, tradeoff, sufficiency, replayReadiness,
                        candidateOutcomes,
                        policySensitivity, boundaryNote);
        List<String> evidenceBasis = evidenceBasis(summary, diagnostics, tradeoff, sufficiency, replayReadiness);
        List<String> selectedCandidateBasis = selectedCandidateBasis(diagnostics, tradeoff);
        List<String> qualityReasons = qualityReasons(qualityLabel, summary, diagnostics, tradeoff, sufficiency,
                replayReadiness, candidateOutcomes, policySensitivity, scenarioInputQuality);
        List<String> warnings = warnings(summary, diagnostics, tradeoff, replayReadiness);
        List<String> unknowns = unknowns(summary, diagnostics, tradeoff, replayReadiness);
        List<String> sourceReferenceIds = sourceReferenceIds(summary, diagnostics, tradeoff);
        List<String> fingerprintInputs = fingerprintBuilder.fingerprintInputs(
                qualityLabel,
                qualityScore,
                summary,
                diagnostics,
                tradeoff,
                sufficiency,
                replayReadiness,
                candidateOutcomes,
                policySensitivity,
                scenarioInputQuality,
                evidenceBasis,
                selectedCandidateBasis,
                qualityReasons,
                warnings,
                unknowns,
                sourceReferenceIds);
        String reproducibilityKey = fingerprintBuilder.reproducibilityKey(
                qualityLabel,
                summary,
                tradeoff,
                sufficiency,
                replayReadiness,
                candidateOutcomes,
                policySensitivity,
                scenarioInputQuality);
        String explanationText = explanationBuilder.explanationText(
                qualityLabel,
                summary,
                tradeoff,
                sufficiency,
                replayReadiness,
                candidateOutcomes,
                policySensitivity,
                scenarioInputQuality,
                reproducibilityKey);
        String evidenceBasisSummary = explanationBuilder.evidenceBasisSummary(
                qualityLabel, summary, tradeoff, sufficiency, replayReadiness);
        String selectedCandidateBasisSummary = explanationBuilder.selectedCandidateBasisSummary(diagnostics, tradeoff);

        return new DecisionExplorerShadowDecisionQualityEvaluationV1(
                true,
                true,
                DecisionExplorerShadowDecisionQualityEvaluationV1.EVALUATION_OBJECT,
                DecisionExplorerShadowDecisionQualityEvaluationV1.CONTRACT_VERSION,
                qualityLabel,
                DecisionExplorerShadowDecisionQualityEvaluationV1.bandFor(qualityLabel),
                qualityScore,
                summary.selectedCandidateId(),
                summary.status(),
                summary.evidenceQuality(),
                tradeoff.tradeoffCategory(),
                sufficiency.sufficiencyLevel(),
                replayReadiness.readinessStatus(),
                candidateOutcomes.size(),
                candidateOutcomes,
                policySensitivity,
                scenarioInputQuality,
                evidenceBasis.size(),
                selectedCandidateBasis.size(),
                evidenceBasisSummary,
                selectedCandidateBasisSummary,
                evidenceBasis,
                selectedCandidateBasis,
                qualityReasons,
                warnings,
                unknowns,
                sourceReferenceIds,
                explanationText,
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                fingerprintBuilder.diagnosticFingerprint(
                        DecisionExplorerShadowDecisionQualityEvaluationV1.FINGERPRINT_NAMESPACE,
                        fingerprintInputs),
                reproducibilityKey,
                fingerprintInputs,
                boundaryNote);
    }

    private static List<String> evidenceBasis(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        List<String> basis = new ArrayList<>();
        basis.add("confidenceStatus=" + summary.status());
        basis.add("evidenceQuality=" + summary.evidenceQuality());
        basis.add("diagnosticsStatus=" + diagnostics.overallStatus());
        basis.add("routeTradeoffCategory=" + tradeoff.tradeoffCategory());
        basis.add("candidateTradeoffCount=" + tradeoff.candidateTradeoffCount());
        basis.add("evidenceSufficiency=" + sufficiency.sufficiencyLevel());
        basis.add("evidenceReadinessScore=" + sufficiency.readinessScore());
        basis.add("replayReadiness=" + replayReadiness.readinessStatus());
        basis.add("replayExecutionAvailable=" + replayReadiness.replayExecutionAvailable());
        return distinctSorted(basis);
    }

    private static List<String> selectedCandidateBasis(
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff) {
        List<String> basis = new ArrayList<>();
        DecisionExplorerCandidateDiagnosticV1 selectedDiagnostic = diagnostics.selectedCandidateDiagnostic();
        if (selectedDiagnostic != null) {
            basis.add("selectedCandidateId=" + selectedDiagnostic.candidateId());
            basis.add("selectedDiagnosticStatus=" + selectedDiagnostic.diagnosticStatus());
            basis.add("selectedRiskLevel=" + selectedDiagnostic.riskLevel());
            basis.add("selectedHealthEvidenceState=" + selectedDiagnostic.healthEvidenceState());
        }
        tradeoff.candidateTradeoffs().stream()
                .filter(DecisionExplorerRouteTradeoffRowV1::selected)
                .findFirst()
                .ifPresent(row -> {
                    basis.add("selectedTradeoffCategory=" + row.tradeoffCategory());
                    basis.add("selectedScoreGapCategory=" + row.scoreGapCategory());
                    basis.add("selectedFinalScore=" + value(row.finalScore()));
                });
        basis.add("closestAlternativeCandidateId=" + tradeoff.closestAlternativeCandidateId());
        basis.add("closestAlternativeScoreDelta=" + value(tradeoff.closestAlternativeScoreDelta()));
        return distinctSorted(basis);
    }

    private static List<String> qualityReasons(
            String qualityLabel,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity,
            DecisionExplorerShadowScenarioInputQualityV1 scenarioInputQuality) {
        List<String> reasons = new ArrayList<>();
        reasons.add("SHADOW_DECISION_QUALITY_" + qualityLabel);
        reasons.add("CONFIDENCE_STATUS_" + summary.status());
        reasons.add("ROUTE_TRADEOFF_CATEGORY_" + tradeoff.tradeoffCategory());
        reasons.add("EVIDENCE_SUFFICIENCY_" + sufficiency.sufficiencyLevel());
        reasons.add("REPLAY_READINESS_" + replayReadiness.readinessStatus());
        reasons.addAll(summary.statusReasons());
        reasons.addAll(diagnostics.diagnosticReasons());
        reasons.addAll(tradeoff.tradeoffReasons());
        candidateOutcomes.stream()
                .map(DecisionExplorerShadowCandidateOutcomeV1::reasonCodes)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .forEach(reasons::add);
        reasons.addAll(policySensitivity.reasonCodes());
        reasons.addAll(scenarioInputQuality.reasonCodes());
        return distinctSorted(reasons);
    }

    private static List<String> warnings(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        return distinctSorted(Stream.of(
                        summary.warnings(),
                        diagnostics.warnings(),
                        tradeoff.warnings(),
                        replayReadiness.limitationSignals())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList());
    }

    private static List<String> unknowns(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        return distinctSorted(Stream.of(
                        summary.unknowns(),
                        diagnostics.unknowns(),
                        tradeoff.unknowns(),
                        replayReadiness.missingEvidenceSignals())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList());
    }

    private static List<String> sourceReferenceIds(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff) {
        return distinctSorted(Stream.of(
                        summary.sourceReferenceIds(),
                        diagnostics.sourceReferenceIds(),
                        tradeoff.sourceReferenceIds())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList());
    }

    private static String value(Double value) {
        return value == null || !Double.isFinite(value) ? "UNKNOWN" : value.toString();
    }

}
