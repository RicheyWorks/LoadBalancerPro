package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

final class DecisionExplorerShadowScenarioInputQualityEvaluator {
    DecisionExplorerShadowScenarioInputQualityV1 evaluate(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity,
            String boundaryNote) {
        List<DecisionExplorerShadowCandidateOutcomeV1> outcomes =
                DecisionExplorerDtoSupport.copyOrEmpty(candidateOutcomes);
        int candidateEvidenceCount = candidateEvidenceCount(summary, diagnostics, tradeoff, sufficiency);
        int factorEvidenceCount = factorEvidenceCount(summary, diagnostics, tradeoff, sufficiency);
        List<String> candidateSignals = scenarioCandidateSignals(summary, diagnostics, tradeoff, sufficiency,
                outcomes, candidateEvidenceCount);
        List<String> factorSignals = scenarioFactorSignals(summary, diagnostics, tradeoff, sufficiency,
                factorEvidenceCount);
        List<String> partialSignals = scenarioPartialSignals(summary, diagnostics, sufficiency, replayReadiness,
                outcomes, policySensitivity);
        List<String> missingSignals = scenarioMissingSignals(summary, diagnostics, tradeoff, sufficiency,
                replayReadiness, outcomes, candidateEvidenceCount, factorEvidenceCount);
        List<String> degradedSignals = scenarioDegradedSignals(summary, diagnostics, sufficiency, replayReadiness,
                outcomes, policySensitivity);
        String label = scenarioInputQualityLabel(summary, diagnostics, tradeoff, sufficiency, replayReadiness,
                outcomes, candidateEvidenceCount, factorEvidenceCount, partialSignals, missingSignals,
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
                outcomes.size(),
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

    private static boolean isUnknownValue(String value) {
        return "UNKNOWN".equals(DecisionExplorerDtoSupport.valueOrUnknown(value));
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
