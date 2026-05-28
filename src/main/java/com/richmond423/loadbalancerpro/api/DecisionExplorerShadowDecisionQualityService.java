package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class DecisionExplorerShadowDecisionQualityService {
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

        String qualityLabel = qualityLabel(summary, diagnostics, tradeoff, sufficiency, replayReadiness);
        int qualityScore = qualityScore(qualityLabel, sufficiency);
        List<String> evidenceBasis = evidenceBasis(summary, diagnostics, tradeoff, sufficiency, replayReadiness);
        List<String> selectedCandidateBasis = selectedCandidateBasis(diagnostics, tradeoff);
        List<String> qualityReasons = qualityReasons(qualityLabel, summary, diagnostics, tradeoff, sufficiency,
                replayReadiness);
        List<String> warnings = warnings(summary, diagnostics, tradeoff, replayReadiness);
        List<String> unknowns = unknowns(summary, diagnostics, tradeoff, replayReadiness);
        List<String> sourceReferenceIds = sourceReferenceIds(summary, diagnostics, tradeoff);

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
                tradeoff.candidateTradeoffCount(),
                evidenceBasis.size(),
                selectedCandidateBasis.size(),
                evidenceBasisSummary(qualityLabel, summary, tradeoff, sufficiency, replayReadiness),
                selectedCandidateBasisSummary(diagnostics, tradeoff),
                evidenceBasis,
                selectedCandidateBasis,
                qualityReasons,
                warnings,
                unknowns,
                sourceReferenceIds,
                boundaryNote);
    }

    private static String qualityLabel(
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

    private static int qualityScore(String qualityLabel, DecisionExplorerEvidenceSufficiencyV1 sufficiency) {
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
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        List<String> reasons = new ArrayList<>();
        reasons.add("SHADOW_DECISION_QUALITY_" + qualityLabel);
        reasons.add("CONFIDENCE_STATUS_" + summary.status());
        reasons.add("ROUTE_TRADEOFF_CATEGORY_" + tradeoff.tradeoffCategory());
        reasons.add("EVIDENCE_SUFFICIENCY_" + sufficiency.sufficiencyLevel());
        reasons.add("REPLAY_READINESS_" + replayReadiness.readinessStatus());
        reasons.addAll(summary.statusReasons());
        reasons.addAll(diagnostics.diagnosticReasons());
        reasons.addAll(tradeoff.tradeoffReasons());
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

    private static String evidenceBasisSummary(
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

    private static String selectedCandidateBasisSummary(
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

    private static String value(Double value) {
        return value == null || !Double.isFinite(value) ? "UNKNOWN" : value.toString();
    }

    private static List<String> distinctSorted(Collection<String> values) {
        if (values == null) {
            return List.of();
        }
        Set<String> distinct = new LinkedHashSet<>();
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .forEach(distinct::add);
        return distinct.stream().sorted().toList();
    }
}
