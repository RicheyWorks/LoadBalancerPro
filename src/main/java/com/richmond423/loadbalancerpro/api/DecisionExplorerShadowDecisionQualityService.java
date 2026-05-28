package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
        List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes =
                candidateOutcomeComparisons(tradeoff, boundaryNote);
        DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity =
                policySensitivityDiagnostic(summary, diagnostics, tradeoff, sufficiency, replayReadiness,
                        candidateOutcomes, boundaryNote);
        List<String> evidenceBasis = evidenceBasis(summary, diagnostics, tradeoff, sufficiency, replayReadiness);
        List<String> selectedCandidateBasis = selectedCandidateBasis(diagnostics, tradeoff);
        List<String> qualityReasons = qualityReasons(qualityLabel, summary, diagnostics, tradeoff, sufficiency,
                replayReadiness, candidateOutcomes, policySensitivity);
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
                candidateOutcomes.size(),
                candidateOutcomes,
                policySensitivity,
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

    private static DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivityDiagnostic(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            String boundaryNote) {
        String level = policySensitivityLevel(summary, diagnostics, sufficiency, replayReadiness, candidateOutcomes);
        String category = policySensitivityCategory(level, sufficiency, candidateOutcomes);
        List<String> stableSignals = policyStableSignals(summary, tradeoff, sufficiency, replayReadiness,
                candidateOutcomes);
        List<String> reviewSignals = policyReviewSignals(summary, diagnostics, tradeoff, sufficiency,
                replayReadiness, candidateOutcomes);
        List<String> missingSignals = policyMissingSignals(summary, diagnostics, tradeoff, sufficiency,
                replayReadiness, candidateOutcomes);
        List<String> degradedSignals = policyDegradedSignals(summary, diagnostics, tradeoff, sufficiency,
                replayReadiness, candidateOutcomes);
        List<String> reasonCodes = policySensitivityReasons(level, category, summary, tradeoff, sufficiency,
                replayReadiness, candidateOutcomes);
        return new DecisionExplorerShadowPolicySensitivityDiagnosticV1(
                true,
                true,
                DecisionExplorerShadowPolicySensitivityDiagnosticV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerShadowPolicySensitivityDiagnosticV1.CONTRACT_VERSION,
                level,
                category,
                policySensitivityScore(level),
                summary.selectedCandidateId(),
                tradeoff.tradeoffCategory(),
                sufficiency.sufficiencyLevel(),
                replayReadiness.readinessStatus(),
                candidateOutcomes.size(),
                policySensitivitySummary(level, category, summary, tradeoff, sufficiency, replayReadiness),
                stableSignals,
                reviewSignals,
                missingSignals,
                degradedSignals,
                reasonCodes,
                sourceReferenceIds(summary, diagnostics, tradeoff),
                boundaryNote);
    }

    private static String policySensitivityLevel(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes) {
        if (isPolicyUnknown(summary, diagnostics, sufficiency, replayReadiness, candidateOutcomes)) {
            return DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_UNKNOWN;
        }
        if (hasPolicyDegradedEvidence(summary, diagnostics, sufficiency, replayReadiness, candidateOutcomes)
                || isInsufficient(sufficiency)) {
            return DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_HIGH;
        }
        if (hasCloseOrSaferAlternative(candidateOutcomes)
                || hasPolicyMissingEvidence(summary, diagnostics, sufficiency, replayReadiness, candidateOutcomes)) {
            return DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_MEDIUM;
        }
        return DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_LOW;
    }

    private static String policySensitivityCategory(
            String level,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes) {
        if (DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_UNKNOWN.equals(level)) {
            return DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_UNKNOWN;
        }
        if (candidateOutcomes.stream()
                .anyMatch(row -> DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_DEGRADED_SELECTED.equals(
                        row.outcomeLabel()))) {
            return DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_DEGRADED_EVIDENCE;
        }
        if (DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED.equals(sufficiency.sufficiencyLevel())) {
            return DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_DEGRADED_EVIDENCE;
        }
        if (isInsufficient(sufficiency) || candidateOutcomes.stream()
                .anyMatch(row -> DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_UNKNOWN_ALTERNATIVE.equals(
                        row.outcomeLabel()))) {
            return DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_MISSING_EVIDENCE;
        }
        if (hasCloseOrSaferAlternative(candidateOutcomes)) {
            return DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_CLOSE_ALTERNATIVE;
        }
        if (DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_MEDIUM.equals(level)) {
            return DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_MISSING_EVIDENCE;
        }
        return DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_STABLE;
    }

    private static boolean isPolicyUnknown(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes) {
        return candidateOutcomes.isEmpty()
                && DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(summary.status())
                && DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(diagnostics.overallStatus())
                && DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT.equals(sufficiency.sufficiencyLevel())
                && DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN.equals(
                        replayReadiness.readinessStatus());
    }

    private static boolean hasPolicyDegradedEvidence(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes) {
        return DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(summary.status())
                || DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(diagnostics.overallStatus())
                || DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED.equals(sufficiency.sufficiencyLevel())
                || DecisionExplorerReplayReadinessDiagnosticV1.STATUS_DEGRADED.equals(
                        replayReadiness.readinessStatus())
                || candidateOutcomes.stream().anyMatch(row ->
                        DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_DEGRADED_SELECTED.equals(row.outcomeLabel())
                                || !row.degradedSignals().isEmpty());
    }

    private static boolean hasPolicyMissingEvidence(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes) {
        return DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(summary.status())
                || DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(diagnostics.overallStatus())
                || DecisionExplorerEvidenceSufficiencyV1.LEVEL_BASIC_DIAGNOSTICS_ONLY.equals(
                        sufficiency.sufficiencyLevel())
                || DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL.equals(
                        replayReadiness.readinessStatus())
                || candidateOutcomes.stream().anyMatch(row ->
                        DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_UNKNOWN_ALTERNATIVE.equals(
                                row.outcomeLabel())
                                || !row.unknownSignals().isEmpty());
    }

    private static boolean hasCloseOrSaferAlternative(List<DecisionExplorerShadowCandidateOutcomeV1> outcomes) {
        return outcomes.stream().anyMatch(row ->
                DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_CLOSE_CALL.equals(row.outcomeLabel())
                        || DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SAFER_ALTERNATIVE.equals(
                                row.outcomeLabel()));
    }

    private static int policySensitivityScore(String level) {
        return switch (level) {
            case DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_LOW -> 15;
            case DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_MEDIUM -> 55;
            case DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_HIGH -> 90;
            default -> 0;
        };
    }

    private static List<String> policyStableSignals(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes) {
        List<String> signals = new ArrayList<>();
        if (DecisionExplorerConfidenceSummaryV1.STATUS_STRONG.equals(summary.status())) {
            signals.add("confidence summary is STRONG");
        }
        if ("SELECTED_ADVANTAGE".equals(tradeoff.tradeoffCategory())) {
            signals.add("selected candidate has route tradeoff advantage");
        }
        if (DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY.equals(sufficiency.sufficiencyLevel())) {
            signals.add("evidence sufficiency is replay-style ready");
        }
        if (DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY.equals(replayReadiness.readinessStatus())) {
            signals.add("replay-readiness diagnostic is READY without enabling replay execution");
        }
        if (candidateOutcomes.stream().anyMatch(row ->
                DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_ACCEPTABLE_ALTERNATIVE.equals(row.outcomeLabel()))) {
            signals.add("alternatives trail selected candidate in returned score evidence");
        }
        return distinctSorted(signals);
    }

    private static List<String> policyReviewSignals(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes) {
        List<String> signals = new ArrayList<>();
        if (DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(summary.status())
                || DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(diagnostics.overallStatus())) {
            signals.add("partial confidence or diagnostics can make policy weighting interpretation sensitive");
        }
        if ("SELECTED_CHALLENGED".equals(tradeoff.tradeoffCategory())
                || "CLOSE_ALTERNATIVE".equals(tradeoff.tradeoffCategory())) {
            signals.add("route tradeoff category indicates the selected candidate is close or challenged");
        }
        if (DecisionExplorerEvidenceSufficiencyV1.LEVEL_BASIC_DIAGNOSTICS_ONLY.equals(sufficiency.sufficiencyLevel())
                || DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL.equals(
                        replayReadiness.readinessStatus())) {
            signals.add("evidence only supports bounded diagnostics, not stronger replay-style analysis");
        }
        candidateOutcomes.stream()
                .filter(row -> DecisionExplorerShadowCandidateOutcomeV1.IMPACT_REVIEW_SIGNAL.equals(
                        row.qualityImpact()))
                .map(row -> "candidate " + row.candidateId() + " is a " + row.outcomeLabel() + " review signal")
                .forEach(signals::add);
        return distinctSorted(signals);
    }

    private static List<String> policyMissingSignals(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes) {
        List<String> signals = new ArrayList<>();
        signals.addAll(summary.unknowns());
        signals.addAll(diagnostics.unknowns());
        signals.addAll(tradeoff.unknowns());
        signals.addAll(replayReadiness.missingEvidenceSignals());
        if (isInsufficient(sufficiency)) {
            signals.add("evidence sufficiency is INSUFFICIENT");
        }
        candidateOutcomes.stream()
                .filter(row -> DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_UNKNOWN_ALTERNATIVE.equals(
                        row.outcomeLabel()))
                .map(row -> "candidate " + row.candidateId() + " has unknown alternative outcome evidence")
                .forEach(signals::add);
        return distinctSorted(signals);
    }

    private static List<String> policyDegradedSignals(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes) {
        List<String> signals = new ArrayList<>();
        signals.addAll(summary.warnings());
        signals.addAll(diagnostics.degradationReasons());
        signals.addAll(tradeoff.warnings());
        signals.addAll(replayReadiness.degradedEvidenceSignals());
        if (DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED.equals(sufficiency.sufficiencyLevel())) {
            signals.add("evidence sufficiency is DEGRADED");
        }
        candidateOutcomes.stream()
                .filter(row -> DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_DEGRADED_SELECTED.equals(
                        row.outcomeLabel()))
                .map(row -> "selected candidate " + row.candidateId() + " has degraded outcome evidence")
                .forEach(signals::add);
        candidateOutcomes.stream()
                .map(DecisionExplorerShadowCandidateOutcomeV1::degradedSignals)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .forEach(signals::add);
        return distinctSorted(signals);
    }

    private static List<String> policySensitivityReasons(
            String level,
            String category,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes) {
        List<String> reasons = new ArrayList<>();
        reasons.add("SHADOW_POLICY_SENSITIVITY_" + level);
        reasons.add("SHADOW_POLICY_SENSITIVITY_CATEGORY_" + category);
        reasons.add("CONFIDENCE_STATUS_" + summary.status());
        reasons.add("ROUTE_TRADEOFF_CATEGORY_" + tradeoff.tradeoffCategory());
        reasons.add("EVIDENCE_SUFFICIENCY_" + sufficiency.sufficiencyLevel());
        reasons.add("REPLAY_READINESS_" + replayReadiness.readinessStatus());
        candidateOutcomes.stream()
                .map(DecisionExplorerShadowCandidateOutcomeV1::reasonCodes)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .forEach(reasons::add);
        return distinctSorted(reasons);
    }

    private static String policySensitivitySummary(
            String level,
            String category,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness) {
        return "Local shadow policy-sensitivity diagnostic classified selected candidate "
                + DecisionExplorerDtoSupport.valueOrUnknown(summary.selectedCandidateId())
                + " as " + level
                + " / " + category
                + " from route tradeoff " + tradeoff.tradeoffCategory()
                + ", evidence sufficiency " + sufficiency.sufficiencyLevel()
                + ", and replay readiness " + replayReadiness.readinessStatus()
                + "; no routing policy, scoring weights, or production routing decision is changed.";
    }

    private static List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomeComparisons(
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            String boundaryNote) {
        return tradeoff.candidateTradeoffs().stream()
                .map(row -> candidateOutcome(row, boundaryNote))
                .sorted(Comparator
                        .comparingInt((DecisionExplorerShadowCandidateOutcomeV1 row) -> row.selected() ? 0 : 1)
                        .thenComparingInt(DecisionExplorerShadowCandidateOutcomeV1::displayOrder)
                        .thenComparing(DecisionExplorerShadowCandidateOutcomeV1::candidateId))
                .toList();
    }

    private static DecisionExplorerShadowCandidateOutcomeV1 candidateOutcome(
            DecisionExplorerRouteTradeoffRowV1 row,
            String boundaryNote) {
        String outcomeLabel = outcomeLabel(row);
        String qualityImpact = qualityImpact(outcomeLabel);
        List<String> reasonCodes = candidateOutcomeReasonCodes(row, outcomeLabel, qualityImpact);
        return new DecisionExplorerShadowCandidateOutcomeV1(
                row.candidateId(),
                row.candidateLabel(),
                row.selected(),
                row.displayOrder(),
                outcomeLabel,
                qualityImpact,
                row.tradeoffCategory(),
                row.riskBenefitClassification(),
                row.diagnosticStatus(),
                row.riskLevel(),
                row.scoreGapCategory(),
                row.finalScore(),
                row.scoreDeltaFromSelected(),
                candidateOutcomeSummary(row, outcomeLabel),
                row.benefitSignals(),
                row.riskSignals(),
                row.unknownSignals(),
                row.degradedSignals(),
                reasonCodes,
                row.sourceReferenceIds(),
                boundaryNote);
    }

    private static String outcomeLabel(DecisionExplorerRouteTradeoffRowV1 row) {
        if (row.selected()) {
            if (DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(row.diagnosticStatus())
                    || DecisionExplorerCandidateDiagnosticV1.RISK_HIGH.equals(row.riskLevel())
                    || !row.degradedSignals().isEmpty()) {
                return DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_DEGRADED_SELECTED;
            }
            return DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SELECTED_BASELINE;
        }
        if (DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_BEATS_SELECTED.equals(row.tradeoffCategory())) {
            return DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SAFER_ALTERNATIVE;
        }
        if (DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_CLOSE.equals(row.tradeoffCategory())) {
            return DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_CLOSE_CALL;
        }
        if (DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_UNKNOWN.equals(row.tradeoffCategory())
                || DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(row.diagnosticStatus())
                || DecisionExplorerCandidateDiagnosticV1.RISK_UNKNOWN.equals(row.riskLevel())) {
            return DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_UNKNOWN_ALTERNATIVE;
        }
        return DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_ACCEPTABLE_ALTERNATIVE;
    }

    private static String qualityImpact(String outcomeLabel) {
        return switch (outcomeLabel) {
            case DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SELECTED_BASELINE,
                    DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_ACCEPTABLE_ALTERNATIVE ->
                    DecisionExplorerShadowCandidateOutcomeV1.IMPACT_SUPPORTS_DECISION;
            case DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_CLOSE_CALL,
                    DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SAFER_ALTERNATIVE ->
                    DecisionExplorerShadowCandidateOutcomeV1.IMPACT_REVIEW_SIGNAL;
            case DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_DEGRADED_SELECTED ->
                    DecisionExplorerShadowCandidateOutcomeV1.IMPACT_RISK_SIGNAL;
            default -> DecisionExplorerShadowCandidateOutcomeV1.IMPACT_UNKNOWN;
        };
    }

    private static List<String> candidateOutcomeReasonCodes(
            DecisionExplorerRouteTradeoffRowV1 row,
            String outcomeLabel,
            String qualityImpact) {
        List<String> reasons = new ArrayList<>();
        reasons.add("SHADOW_CANDIDATE_OUTCOME_" + outcomeLabel);
        reasons.add("SHADOW_CANDIDATE_IMPACT_" + qualityImpact);
        reasons.add("ROUTE_TRADEOFF_CATEGORY_" + row.tradeoffCategory());
        reasons.addAll(row.reasonCodes());
        return distinctSorted(reasons);
    }

    private static String candidateOutcomeSummary(DecisionExplorerRouteTradeoffRowV1 row, String outcomeLabel) {
        String candidateId = DecisionExplorerDtoSupport.valueOrUnknown(row.candidateId());
        String delta = value(row.scoreDeltaFromSelected());
        return switch (outcomeLabel) {
            case DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SELECTED_BASELINE ->
                    "Selected candidate " + candidateId
                            + " is the returned decision baseline for local shadow quality comparison; "
                            + "no production routing decision is changed.";
            case DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_DEGRADED_SELECTED ->
                    "Selected candidate " + candidateId
                            + " is the returned decision baseline, but degraded or high-risk evidence makes it "
                            + "a local shadow quality risk signal; no production routing decision is changed.";
            case DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SAFER_ALTERNATIVE ->
                    "Alternative candidate " + candidateId
                            + " has a returned score delta of " + delta
                            + " against the selected candidate, so the local shadow evaluator marks it as a "
                            + "review signal rather than changing routing.";
            case DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_CLOSE_CALL ->
                    "Alternative candidate " + candidateId
                            + " is close to the selected candidate with returned score delta " + delta
                            + ", so local shadow quality review is recommended.";
            case DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_UNKNOWN_ALTERNATIVE ->
                    "Alternative candidate " + candidateId
                            + " cannot be fully compared because score or diagnostic evidence is unknown.";
            default ->
                    "Alternative candidate " + candidateId
                            + " trails the selected candidate in returned score evidence with delta " + delta
                            + ".";
        };
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
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity) {
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
