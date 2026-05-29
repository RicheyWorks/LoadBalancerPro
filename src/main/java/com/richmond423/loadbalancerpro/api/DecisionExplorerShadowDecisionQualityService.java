package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DecisionExplorerShadowDecisionQualityService {
    private final DecisionExplorerShadowQualityLabelEvaluator qualityLabelEvaluator =
            new DecisionExplorerShadowQualityLabelEvaluator();
    private final DecisionExplorerShadowCandidateOutcomeBuilder candidateOutcomeBuilder =
            new DecisionExplorerShadowCandidateOutcomeBuilder();
    private final DecisionExplorerShadowPolicySensitivityEvaluator policySensitivityEvaluator =
            new DecisionExplorerShadowPolicySensitivityEvaluator();

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
        String reproducibilityKey = shadowQualityReproducibilityKey(
                qualityLabel,
                summary,
                tradeoff,
                sufficiency,
                replayReadiness,
                candidateOutcomes,
                policySensitivity,
                scenarioInputQuality);
        String explanationText = shadowQualityExplanationText(
                qualityLabel,
                summary,
                tradeoff,
                sufficiency,
                replayReadiness,
                candidateOutcomes,
                policySensitivity,
                scenarioInputQuality,
                reproducibilityKey);

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
                explanationText,
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                diagnosticFingerprint(
                        DecisionExplorerShadowDecisionQualityEvaluationV1.FINGERPRINT_NAMESPACE,
                        fingerprintInputs),
                reproducibilityKey,
                fingerprintInputs,
                boundaryNote);
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

    private static boolean isInsufficient(DecisionExplorerEvidenceSufficiencyV1 sufficiency) {
        return DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT.equals(sufficiency.sufficiencyLevel());
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

    private static String shadowQualityExplanationText(
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
