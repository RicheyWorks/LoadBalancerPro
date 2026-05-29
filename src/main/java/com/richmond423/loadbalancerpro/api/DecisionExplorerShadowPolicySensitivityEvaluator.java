package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

final class DecisionExplorerShadowPolicySensitivityEvaluator {
    DecisionExplorerShadowPolicySensitivityDiagnosticV1 evaluate(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            String boundaryNote) {
        List<DecisionExplorerShadowCandidateOutcomeV1> outcomes =
                DecisionExplorerDtoSupport.copyOrEmpty(candidateOutcomes);
        String level = policySensitivityLevel(summary, diagnostics, sufficiency, replayReadiness, outcomes);
        String category = policySensitivityCategory(level, sufficiency, outcomes);
        List<String> stableSignals = policyStableSignals(summary, tradeoff, sufficiency, replayReadiness, outcomes);
        List<String> reviewSignals = policyReviewSignals(summary, diagnostics, tradeoff, sufficiency,
                replayReadiness, outcomes);
        List<String> missingSignals = policyMissingSignals(summary, diagnostics, tradeoff, sufficiency,
                replayReadiness, outcomes);
        List<String> degradedSignals = policyDegradedSignals(summary, diagnostics, tradeoff, sufficiency,
                replayReadiness, outcomes);
        List<String> reasonCodes = policySensitivityReasons(level, category, summary, tradeoff, sufficiency,
                replayReadiness, outcomes);
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
                outcomes.size(),
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

    private static boolean isInsufficient(DecisionExplorerEvidenceSufficiencyV1 sufficiency) {
        return DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT.equals(sufficiency.sufficiencyLevel());
    }

    private static List<String> distinctSorted(Collection<String> values) {
        List<String> safeValues = values == null ? List.of() : values.stream().toList();
        Set<String> distinct = new LinkedHashSet<>(DecisionExplorerDtoSupport.copyOrEmpty(safeValues));
        return distinct.stream()
                .filter(value -> !DecisionExplorerDtoSupport.valueOrUnknown(value).equals("UNKNOWN"))
                .sorted()
                .toList();
    }
}
