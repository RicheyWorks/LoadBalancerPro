package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
        DecisionExplorerShadowScenarioInputQualityV1 scenarioInputQuality =
                scenarioInputQuality(summary, diagnostics, tradeoff, sufficiency, replayReadiness, candidateOutcomes,
                        policySensitivity, boundaryNote);
        List<String> evidenceBasis = evidenceBasis(summary, diagnostics, tradeoff, sufficiency, replayReadiness);
        List<String> selectedCandidateBasis = selectedCandidateBasis(diagnostics, tradeoff);
        List<String> qualityReasons = qualityReasons(qualityLabel, summary, diagnostics, tradeoff, sufficiency,
                replayReadiness, candidateOutcomes, policySensitivity, scenarioInputQuality);
        List<String> warnings = warnings(summary, diagnostics, tradeoff, replayReadiness);
        List<String> unknowns = unknowns(summary, diagnostics, tradeoff, replayReadiness);
        List<String> sourceReferenceIds = sourceReferenceIds(summary, diagnostics, tradeoff);
        List<String> fingerprintInputs = shadowQualityFingerprintInputs(
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
                evidenceBasisSummary(qualityLabel, summary, tradeoff, sufficiency, replayReadiness),
                selectedCandidateBasisSummary(diagnostics, tradeoff),
                evidenceBasis,
                selectedCandidateBasis,
                qualityReasons,
                warnings,
                unknowns,
                sourceReferenceIds,
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                diagnosticFingerprint(
                        DecisionExplorerShadowDecisionQualityEvaluationV1.FINGERPRINT_NAMESPACE,
                        fingerprintInputs),
                shadowQualityReproducibilityKey(
                        qualityLabel,
                        summary,
                        tradeoff,
                        sufficiency,
                        replayReadiness,
                        candidateOutcomes,
                        policySensitivity,
                        scenarioInputQuality),
                fingerprintInputs,
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

    private static DecisionExplorerShadowScenarioInputQualityV1 scenarioInputQuality(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity,
            String boundaryNote) {
        int candidateEvidenceCount = candidateEvidenceCount(summary, diagnostics, tradeoff, sufficiency);
        int factorEvidenceCount = factorEvidenceCount(summary, diagnostics, tradeoff, sufficiency);
        List<String> candidateSignals = scenarioCandidateSignals(summary, diagnostics, tradeoff, sufficiency,
                candidateOutcomes, candidateEvidenceCount);
        List<String> factorSignals = scenarioFactorSignals(summary, diagnostics, tradeoff, sufficiency,
                factorEvidenceCount);
        List<String> partialSignals = scenarioPartialSignals(summary, diagnostics, sufficiency, replayReadiness,
                candidateOutcomes, policySensitivity);
        List<String> missingSignals = scenarioMissingSignals(summary, diagnostics, tradeoff, sufficiency,
                replayReadiness, candidateOutcomes, candidateEvidenceCount, factorEvidenceCount);
        List<String> degradedSignals = scenarioDegradedSignals(summary, diagnostics, sufficiency, replayReadiness,
                candidateOutcomes, policySensitivity);
        String label = scenarioInputQualityLabel(summary, diagnostics, tradeoff, sufficiency, replayReadiness,
                candidateOutcomes, candidateEvidenceCount, factorEvidenceCount, partialSignals, missingSignals,
                degradedSignals);
        List<String> reasonCodes = scenarioInputReasonCodes(label, summary, sufficiency, replayReadiness,
                policySensitivity, candidateEvidenceCount, factorEvidenceCount);
        return new DecisionExplorerShadowScenarioInputQualityV1(
                true,
                true,
                DecisionExplorerShadowScenarioInputQualityV1.EVALUATION_OBJECT,
                DecisionExplorerShadowScenarioInputQualityV1.CONTRACT_VERSION,
                label,
                DecisionExplorerShadowScenarioInputQualityV1.supportBandFor(label),
                scenarioInputQualityScore(label, sufficiency),
                summary.selectedCandidateId(),
                summary.status(),
                sufficiency.sufficiencyLevel(),
                replayReadiness.readinessStatus(),
                candidateEvidenceCount,
                factorEvidenceCount,
                candidateOutcomes.size(),
                partialSignals.size(),
                missingSignals.size(),
                degradedSignals.size(),
                scenarioInputSummary(label, summary, candidateEvidenceCount, factorEvidenceCount),
                candidateSignals,
                factorSignals,
                partialSignals,
                missingSignals,
                degradedSignals,
                reasonCodes,
                sourceReferenceIds(summary, diagnostics, tradeoff),
                boundaryNote);
    }

    private static String scenarioInputQualityLabel(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            int candidateEvidenceCount,
            int factorEvidenceCount,
            List<String> partialSignals,
            List<String> missingSignals,
            List<String> degradedSignals) {
        if (isScenarioInputUnknown(summary, diagnostics, sufficiency, replayReadiness, candidateOutcomes,
                candidateEvidenceCount, factorEvidenceCount)) {
            return DecisionExplorerShadowScenarioInputQualityV1.LABEL_UNKNOWN;
        }
        if (candidateEvidenceCount == 0 || candidateOutcomes.isEmpty()
                || isUnknownValue(summary.selectedCandidateId()) || isUnknownValue(tradeoff.selectedCandidateId())) {
            return DecisionExplorerShadowScenarioInputQualityV1.LABEL_MISSING_CANDIDATE_INPUT;
        }
        if (factorEvidenceCount == 0) {
            return DecisionExplorerShadowScenarioInputQualityV1.LABEL_MISSING_FACTOR_INPUT;
        }
        if (!degradedSignals.isEmpty()) {
            return DecisionExplorerShadowScenarioInputQualityV1.LABEL_DEGRADED_INPUT;
        }
        if (!partialSignals.isEmpty() || !missingSignals.isEmpty()) {
            return DecisionExplorerShadowScenarioInputQualityV1.LABEL_PARTIAL_INPUT;
        }
        return DecisionExplorerShadowScenarioInputQualityV1.LABEL_EVALUABLE;
    }

    private static boolean isScenarioInputUnknown(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            int candidateEvidenceCount,
            int factorEvidenceCount) {
        return candidateEvidenceCount == 0
                && factorEvidenceCount == 0
                && candidateOutcomes.isEmpty()
                && DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(summary.status())
                && DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(diagnostics.overallStatus())
                && DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT.equals(sufficiency.sufficiencyLevel())
                && DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN.equals(
                        replayReadiness.readinessStatus());
    }

    private static int scenarioInputQualityScore(
            String label,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency) {
        int readiness = sufficiency.readinessScore();
        return switch (label) {
            case DecisionExplorerShadowScenarioInputQualityV1.LABEL_EVALUABLE -> Math.max(85, readiness);
            case DecisionExplorerShadowScenarioInputQualityV1.LABEL_PARTIAL_INPUT ->
                    Math.max(45, Math.min(75, readiness));
            case DecisionExplorerShadowScenarioInputQualityV1.LABEL_DEGRADED_INPUT ->
                    Math.max(15, Math.min(40, readiness));
            case DecisionExplorerShadowScenarioInputQualityV1.LABEL_MISSING_CANDIDATE_INPUT,
                    DecisionExplorerShadowScenarioInputQualityV1.LABEL_MISSING_FACTOR_INPUT ->
                    Math.min(35, readiness);
            default -> 0;
        };
    }

    private static int candidateEvidenceCount(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency) {
        return Stream.of(
                        summary.candidateCount(),
                        diagnostics.candidateDiagnostics().size(),
                        tradeoff.candidateTradeoffCount(),
                        sufficiency.candidateEvidenceCount())
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    private static int factorEvidenceCount(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency) {
        int confidenceFactorCount = summary.availableFactorCount()
                + summary.partialFactorCount()
                + summary.unknownFactorCount();
        return Stream.of(
                        confidenceFactorCount,
                        summary.factorStatusDetails().size(),
                        diagnostics.factorDiagnostics().size(),
                        tradeoff.factorTradeoffDeltas().size(),
                        sufficiency.factorDeltaCount())
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    private static List<String> scenarioCandidateSignals(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            int candidateEvidenceCount) {
        List<String> signals = new ArrayList<>();
        if (candidateEvidenceCount > 0) {
            signals.add("candidateEvidenceCount=" + candidateEvidenceCount);
        }
        if (!isUnknownValue(summary.selectedCandidateId())) {
            signals.add("selectedCandidateId=" + summary.selectedCandidateId());
        }
        if (tradeoff.alternativeCount() > 0) {
            signals.add("alternativeCount=" + tradeoff.alternativeCount());
        }
        if (tradeoff.comparedAlternativeCount() > 0) {
            signals.add("comparedAlternativeCount=" + tradeoff.comparedAlternativeCount());
        }
        if (!candidateOutcomes.isEmpty()) {
            signals.add("candidateOutcomeCount=" + candidateOutcomes.size());
        }
        if (!diagnostics.candidateDiagnostics().isEmpty()) {
            signals.add("candidateDiagnosticCount=" + diagnostics.candidateDiagnostics().size());
        }
        if (sufficiency.candidateEvidenceCount() > 0) {
            signals.add("sufficiencyCandidateEvidenceCount=" + sufficiency.candidateEvidenceCount());
        }
        return distinctSorted(signals);
    }

    private static List<String> scenarioFactorSignals(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            int factorEvidenceCount) {
        List<String> signals = new ArrayList<>();
        if (factorEvidenceCount > 0) {
            signals.add("factorEvidenceCount=" + factorEvidenceCount);
        }
        if (summary.availableFactorCount() > 0) {
            signals.add("availableFactorCount=" + summary.availableFactorCount());
        }
        if (summary.partialFactorCount() > 0) {
            signals.add("partialFactorCount=" + summary.partialFactorCount());
        }
        if (summary.unknownFactorCount() > 0) {
            signals.add("unknownFactorCount=" + summary.unknownFactorCount());
        }
        if (!diagnostics.factorDiagnostics().isEmpty()) {
            signals.add("factorDiagnosticCount=" + diagnostics.factorDiagnostics().size());
        }
        if (!tradeoff.factorTradeoffDeltas().isEmpty()) {
            signals.add("factorTradeoffDeltaCount=" + tradeoff.factorTradeoffDeltas().size());
        }
        if (sufficiency.factorDeltaCount() > 0) {
            signals.add("sufficiencyFactorDeltaCount=" + sufficiency.factorDeltaCount());
        }
        return distinctSorted(signals);
    }

    private static List<String> scenarioPartialSignals(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity) {
        List<String> signals = new ArrayList<>();
        if (summary.partialFactorCount() > 0) {
            signals.add("confidence summary has partial factor inputs");
        }
        signals.addAll(diagnostics.partialEvidenceReasons());
        signals.addAll(sufficiency.partialEvidenceSignals());
        signals.addAll(replayReadiness.partialEvidenceSignals());
        if (DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(summary.status())
                || DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(diagnostics.overallStatus())) {
            signals.add("partial confidence or diagnostics status");
        }
        if (DecisionExplorerEvidenceSufficiencyV1.LEVEL_BASIC_DIAGNOSTICS_ONLY.equals(
                sufficiency.sufficiencyLevel())) {
            signals.add("input evidence only supports basic diagnostics");
        }
        if (DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL.equals(replayReadiness.readinessStatus())) {
            signals.add("replay-readiness diagnostic reports PARTIAL input evidence");
        }
        if (DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_MISSING_EVIDENCE.equals(
                policySensitivity.sensitivityCategory())) {
            signals.add("policy sensitivity depends on missing evidence");
        }
        candidateOutcomes.stream()
                .filter(row -> DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_UNKNOWN_ALTERNATIVE.equals(
                        row.outcomeLabel()))
                .map(row -> "candidate " + row.candidateId() + " alternative comparison is unknown")
                .forEach(signals::add);
        return distinctSorted(signals);
    }

    private static List<String> scenarioMissingSignals(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            int candidateEvidenceCount,
            int factorEvidenceCount) {
        List<String> signals = new ArrayList<>();
        if (candidateEvidenceCount == 0) {
            signals.add("candidate input evidence was unavailable");
        }
        if (candidateOutcomes.isEmpty()) {
            signals.add("candidate outcome comparison rows were unavailable");
        }
        if (isUnknownValue(summary.selectedCandidateId()) || isUnknownValue(tradeoff.selectedCandidateId())) {
            signals.add("selected candidate input identity was unavailable");
        }
        if (factorEvidenceCount == 0) {
            signals.add("factor input evidence was unavailable");
        }
        signals.addAll(summary.unknowns());
        signals.addAll(diagnostics.unknownEvidenceReasons());
        signals.addAll(sufficiency.missingEvidenceSignals());
        signals.addAll(sufficiency.unknownEvidenceSignals());
        signals.addAll(replayReadiness.missingEvidenceSignals());
        candidateOutcomes.stream()
                .map(DecisionExplorerShadowCandidateOutcomeV1::unknownSignals)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .forEach(signals::add);
        return distinctSorted(signals);
    }

    private static List<String> scenarioDegradedSignals(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity) {
        List<String> signals = new ArrayList<>();
        signals.addAll(diagnostics.degradationReasons());
        signals.addAll(sufficiency.degradedEvidenceSignals());
        signals.addAll(replayReadiness.degradedEvidenceSignals());
        if (DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_DEGRADED_EVIDENCE.equals(
                policySensitivity.sensitivityCategory())) {
            signals.addAll(policySensitivity.degradedSignals());
        }
        if (DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(summary.status())
                || DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(diagnostics.overallStatus())) {
            signals.add("degraded confidence or diagnostics status");
        }
        if (DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED.equals(sufficiency.sufficiencyLevel())) {
            signals.add("evidence sufficiency is DEGRADED");
        }
        if (DecisionExplorerReplayReadinessDiagnosticV1.STATUS_DEGRADED.equals(replayReadiness.readinessStatus())) {
            signals.add("replay-readiness diagnostic reports DEGRADED input evidence");
        }
        candidateOutcomes.stream()
                .map(DecisionExplorerShadowCandidateOutcomeV1::degradedSignals)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .forEach(signals::add);
        return distinctSorted(signals);
    }

    private static List<String> scenarioInputReasonCodes(
            String label,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity,
            int candidateEvidenceCount,
            int factorEvidenceCount) {
        List<String> reasons = new ArrayList<>();
        reasons.add("SHADOW_SCENARIO_INPUT_QUALITY_" + label);
        reasons.add(candidateEvidenceCount > 0
                ? "SHADOW_SCENARIO_INPUT_CANDIDATE_EVIDENCE_PRESENT"
                : "SHADOW_SCENARIO_INPUT_CANDIDATE_EVIDENCE_MISSING");
        reasons.add(factorEvidenceCount > 0
                ? "SHADOW_SCENARIO_INPUT_FACTOR_EVIDENCE_PRESENT"
                : "SHADOW_SCENARIO_INPUT_FACTOR_EVIDENCE_MISSING");
        reasons.add("CONFIDENCE_STATUS_" + summary.status());
        reasons.add("EVIDENCE_SUFFICIENCY_" + sufficiency.sufficiencyLevel());
        reasons.add("REPLAY_READINESS_" + replayReadiness.readinessStatus());
        reasons.add("SHADOW_POLICY_SENSITIVITY_" + policySensitivity.sensitivityLevel());
        return distinctSorted(reasons);
    }

    private static String scenarioInputSummary(
            String label,
            DecisionExplorerConfidenceSummaryV1 summary,
            int candidateEvidenceCount,
            int factorEvidenceCount) {
        return "Local scenario-input quality is " + label
                + " for selected candidate " + DecisionExplorerDtoSupport.valueOrUnknown(summary.selectedCandidateId())
                + " using candidate evidence count " + candidateEvidenceCount
                + " and factor evidence count " + factorEvidenceCount
                + "; no production routing decision is changed.";
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

    private static List<String> shadowQualityFingerprintInputs(
            String qualityLabel,
            int qualityScore,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity,
            DecisionExplorerShadowScenarioInputQualityV1 scenarioInputQuality,
            List<String> evidenceBasis,
            List<String> selectedCandidateBasis,
            List<String> qualityReasons,
            List<String> warnings,
            List<String> unknowns,
            List<String> sourceReferenceIds) {
        List<String> inputs = new ArrayList<>();
        inputs.add(input("evaluationObject", DecisionExplorerShadowDecisionQualityEvaluationV1.EVALUATION_OBJECT));
        inputs.add(input("contractVersion", DecisionExplorerShadowDecisionQualityEvaluationV1.CONTRACT_VERSION));
        inputs.add(input("qualityLabel", qualityLabel));
        inputs.add(input("qualityBand", DecisionExplorerShadowDecisionQualityEvaluationV1.bandFor(qualityLabel)));
        inputs.add(input("qualityScore", qualityScore));
        inputs.add(input("selectedCandidateId", summary.selectedCandidateId()));
        inputs.add(input("confidenceStatus", summary.status()));
        inputs.add(input("evidenceQuality", summary.evidenceQuality()));
        inputs.add(input("diagnosticsStatus", diagnostics.overallStatus()));
        inputs.add(input("tradeoffCategory", tradeoff.tradeoffCategory()));
        inputs.add(input("evidenceSufficiencyLevel", sufficiency.sufficiencyLevel()));
        inputs.add(input("replayReadinessStatus", replayReadiness.readinessStatus()));
        inputs.add(input("candidateOutcomeCount", copyNonNull(candidateOutcomes).size()));
        copyNonNull(candidateOutcomes)
                .forEach(row -> inputs.add(input("candidateOutcome", candidateOutcomeFingerprint(row))));
        inputs.add(input("policySensitivity", policySensitivityFingerprint(policySensitivity)));
        inputs.add(input("scenarioInputQuality", scenarioInputQualityFingerprint(scenarioInputQuality)));
        inputs.add(input("evidenceBasis", evidenceBasis));
        inputs.add(input("selectedCandidateBasis", selectedCandidateBasis));
        inputs.add(input("qualityReasons", qualityReasons));
        inputs.add(input("warnings", warnings));
        inputs.add(input("unknowns", unknowns));
        inputs.add(input("sourceReferenceIds", sourceReferenceIds));
        inputs.add(input("routeTradeoffFingerprint", tradeoff.diagnosticFingerprint()));
        inputs.add(input("evidenceSufficiencyFingerprint", sufficiency.diagnosticFingerprint()));
        inputs.add(input("replayReadinessFingerprint", replayReadiness.diagnosticFingerprint()));
        return canonicalInputs(inputs);
    }

    private static String shadowQualityReproducibilityKey(
            String qualityLabel,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity,
            DecisionExplorerShadowScenarioInputQualityV1 scenarioInputQuality) {
        return String.join(":",
                "shadow-decision-quality",
                DecisionExplorerShadowDecisionQualityEvaluationV1.CONTRACT_VERSION,
                fingerprintValue(qualityLabel),
                fingerprintValue(summary.selectedCandidateId()),
                fingerprintValue(tradeoff.tradeoffCategory()),
                "outcomes=" + copyNonNull(candidateOutcomes).size(),
                "policy=" + fingerprintValue(policySensitivity.sensitivityLevel()),
                "scenario=" + fingerprintValue(scenarioInputQuality.inputQualityLabel()),
                "sufficiency=" + fingerprintValue(sufficiency.sufficiencyLevel()),
                "replay=" + fingerprintValue(replayReadiness.readinessStatus()));
    }

    private static String candidateOutcomeFingerprint(DecisionExplorerShadowCandidateOutcomeV1 row) {
        return String.join(",",
                "candidate=" + fingerprintValue(row.candidateId()),
                "selected=" + row.selected(),
                "order=" + row.displayOrder(),
                "outcome=" + fingerprintValue(row.outcomeLabel()),
                "impact=" + fingerprintValue(row.qualityImpact()),
                "tradeoff=" + fingerprintValue(row.tradeoffCategory()),
                "riskBenefit=" + fingerprintValue(row.riskBenefitClassification()),
                "status=" + fingerprintValue(row.diagnosticStatus()),
                "risk=" + fingerprintValue(row.riskLevel()),
                "gap=" + fingerprintValue(row.scoreGapCategory()),
                "finalScore=" + fingerprintValue(row.finalScore()),
                "delta=" + fingerprintValue(row.scoreDeltaFromSelected()),
                "reasons=" + fingerprintValue(row.reasonCodes()));
    }

    private static String policySensitivityFingerprint(
            DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity) {
        return String.join(",",
                "level=" + fingerprintValue(policySensitivity.sensitivityLevel()),
                "category=" + fingerprintValue(policySensitivity.sensitivityCategory()),
                "score=" + policySensitivity.sensitivityScore(),
                "candidateOutcomes=" + policySensitivity.candidateOutcomeCount(),
                "stable=" + fingerprintValue(policySensitivity.stableSignals()),
                "review=" + fingerprintValue(policySensitivity.reviewSignals()),
                "missing=" + fingerprintValue(policySensitivity.missingEvidenceSignals()),
                "degraded=" + fingerprintValue(policySensitivity.degradedSignals()),
                "reasons=" + fingerprintValue(policySensitivity.reasonCodes()));
    }

    private static String scenarioInputQualityFingerprint(
            DecisionExplorerShadowScenarioInputQualityV1 scenarioInputQuality) {
        return String.join(",",
                "label=" + fingerprintValue(scenarioInputQuality.inputQualityLabel()),
                "band=" + fingerprintValue(scenarioInputQuality.supportBand()),
                "score=" + scenarioInputQuality.inputQualityScore(),
                "candidates=" + scenarioInputQuality.candidateEvidenceCount(),
                "factors=" + scenarioInputQuality.factorEvidenceCount(),
                "partial=" + scenarioInputQuality.partialSignalCount(),
                "missing=" + scenarioInputQuality.missingSignalCount(),
                "degraded=" + scenarioInputQuality.degradedSignalCount(),
                "candidateSignals=" + fingerprintValue(scenarioInputQuality.candidateInputSignals()),
                "factorSignals=" + fingerprintValue(scenarioInputQuality.factorInputSignals()),
                "partialSignals=" + fingerprintValue(scenarioInputQuality.partialInputSignals()),
                "missingSignals=" + fingerprintValue(scenarioInputQuality.missingInputSignals()),
                "degradedSignals=" + fingerprintValue(scenarioInputQuality.degradedInputSignals()),
                "reasons=" + fingerprintValue(scenarioInputQuality.reasonCodes()));
    }

    private static String diagnosticFingerprint(String namespace, List<String> inputs) {
        List<String> canonicalInputs = canonicalInputs(inputs);
        String safeNamespace = namespace == null || namespace.isBlank()
                ? "diagnostic|v1"
                : namespace.trim().replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ");
        if (canonicalInputs.isEmpty()) {
            return safeNamespace + "|inputs=none";
        }
        return safeNamespace + "|" + String.join("|", canonicalInputs);
    }

    private static String input(String key, Object value) {
        return fingerprintValue(key) + "=" + fingerprintValue(value);
    }

    private static List<String> canonicalInputs(Collection<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(DecisionExplorerShadowDecisionQualityService::fingerprintValue)
                .toList();
    }

    private static String fingerprintValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                return "[]";
            }
            return collection.stream()
                    .map(DecisionExplorerShadowDecisionQualityService::fingerprintValue)
                    .sorted()
                    .collect(Collectors.joining(";"));
        }
        if (value instanceof Double number && !Double.isFinite(number)) {
            return "null";
        }
        return String.valueOf(value)
                .trim()
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('|', '/')
                .replaceAll("\\s+", " ");
    }

    private static String value(Double value) {
        return value == null || !Double.isFinite(value) ? "UNKNOWN" : value.toString();
    }

    private static boolean isUnknownValue(String value) {
        return DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(
                DecisionExplorerDtoSupport.valueOrUnknown(value));
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

    private static <T> List<T> copyNonNull(List<T> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }
}
