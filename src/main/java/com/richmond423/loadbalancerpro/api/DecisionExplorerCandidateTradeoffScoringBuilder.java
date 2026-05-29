package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class DecisionExplorerCandidateTradeoffScoringBuilder {
    List<DecisionExplorerCandidateTradeoffScoringExplanationV1> build(
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<DecisionExplorerFactorDiagnosticV1> factorDiagnostics,
            String boundaryNote) {
        Map<String, List<DecisionExplorerFactorDiagnosticV1>> factorsByCandidateId =
                copyNonNull(factorDiagnostics).stream()
                        .collect(Collectors.groupingBy(
                                DecisionExplorerFactorDiagnosticV1::candidateId,
                                java.util.TreeMap::new,
                                Collectors.toList()));
        return copyNonNull(rows).stream()
                .sorted(DecisionExplorerRouteTradeoffRowBuilder.BY_SELECTED_THEN_ORDER)
                .map(row -> scoringExplanation(row, factorsByCandidateId.get(row.candidateId()), boundaryNote))
                .toList();
    }

    private static DecisionExplorerCandidateTradeoffScoringExplanationV1 scoringExplanation(
            DecisionExplorerRouteTradeoffRowV1 row,
            List<DecisionExplorerFactorDiagnosticV1> factorDiagnostics,
            String boundaryNote) {
        String factorStatusRollup = factorStatusRollup(factorDiagnostics);
        String scoreEvidenceState = DecisionExplorerCandidateTradeoffScoringExplanationV1.scoreEvidenceStateFor(
                row.selected(),
                row.finalScore(),
                row.scoreDeltaFromSelected());
        String explanationStatus = explanationStatus(row, factorStatusRollup, scoreEvidenceState);
        List<String> scoringSignals = scoringSignals(row, factorDiagnostics, scoreEvidenceState, factorStatusRollup);
        List<String> limitationSignals = limitationSignals(row, factorDiagnostics, scoreEvidenceState);
        List<String> reasonCodes = new ArrayList<>(row.reasonCodes());
        reasonCodes.add("SCORING_EXPLANATION_STATUS_" + explanationStatus);
        reasonCodes.add("SCORE_EVIDENCE_" + scoreEvidenceState);
        reasonCodes.add("FACTOR_STATUS_ROLLUP_" + factorStatusRollup);
        return new DecisionExplorerCandidateTradeoffScoringExplanationV1(
                row.candidateId(),
                row.candidateLabel(),
                row.selected(),
                row.displayOrder(),
                explanationStatus,
                scoreEvidenceState,
                row.scoreGapCategory(),
                row.tradeoffCategory(),
                row.riskBenefitClassification(),
                row.diagnosticStatus(),
                factorStatusRollup,
                row.finalScore(),
                row.scoreDeltaFromSelected(),
                scoringSummary(row, explanationStatus, scoreEvidenceState, factorStatusRollup),
                scoringSignals,
                limitationSignals,
                distinctSorted(reasonCodes),
                row.sourceReferenceIds(),
                boundaryNote);
    }

    private static String factorStatusRollup(List<DecisionExplorerFactorDiagnosticV1> factorDiagnostics) {
        List<DecisionExplorerFactorDiagnosticV1> factors = copyNonNull(factorDiagnostics);
        if (factors.isEmpty()) {
            return DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
        }
        if (factors.stream().anyMatch(factor -> DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED
                .equals(factor.factorStatus())
                || DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_DEGRADED.equals(factor.contribution()))) {
            return DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED;
        }
        if (factors.stream().anyMatch(factor -> DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL
                .equals(factor.factorStatus())
                || DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_WARNING.equals(factor.contribution()))) {
            return DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL;
        }
        if (factors.stream().anyMatch(factor -> DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN
                .equals(factor.factorStatus())
                || DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_UNKNOWN.equals(factor.contribution()))) {
            return DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
        }
        return DecisionExplorerConfidenceSummaryV1.STATUS_STRONG;
    }

    private static String explanationStatus(
            DecisionExplorerRouteTradeoffRowV1 row,
            String factorStatusRollup,
            String scoreEvidenceState) {
        if (DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_RISK.equals(row.riskBenefitClassification())
                || DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(row.diagnosticStatus())
                || DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(factorStatusRollup)) {
            return DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED;
        }
        if (DecisionExplorerCandidateTradeoffScoringExplanationV1.SCORE_EVIDENCE_SELECTED_BASELINE_UNKNOWN
                .equals(scoreEvidenceState)
                || DecisionExplorerCandidateTradeoffScoringExplanationV1.SCORE_EVIDENCE_ALTERNATIVE_DELTA_UNKNOWN
                        .equals(scoreEvidenceState)
                || DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(row.diagnosticStatus())
                || DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(factorStatusRollup)
                || DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_UNKNOWN.equals(row.tradeoffCategory())) {
            return DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
        }
        if (DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(row.diagnosticStatus())
                || DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(factorStatusRollup)
                || DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_TRADEOFF
                        .equals(row.riskBenefitClassification())) {
            return DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL;
        }
        return DecisionExplorerConfidenceSummaryV1.STATUS_STRONG;
    }

    private static List<String> scoringSignals(
            DecisionExplorerRouteTradeoffRowV1 row,
            List<DecisionExplorerFactorDiagnosticV1> factorDiagnostics,
            String scoreEvidenceState,
            String factorStatusRollup) {
        List<String> signals = new ArrayList<>(row.benefitSignals());
        signals.add("scoreEvidence=" + scoreEvidenceState);
        signals.add("factorStatusRollup=" + factorStatusRollup);
        if (row.selected() && row.finalScore() != null) {
            signals.add("selected final score is present");
        }
        if (!row.selected() && row.scoreDeltaFromSelected() != null) {
            signals.add("alternative score delta is present");
        }
        if (!copyNonNull(factorDiagnostics).isEmpty()) {
            signals.add("factor diagnostics returned for candidate");
        }
        return distinctSorted(signals);
    }

    private static List<String> limitationSignals(
            DecisionExplorerRouteTradeoffRowV1 row,
            List<DecisionExplorerFactorDiagnosticV1> factorDiagnostics,
            String scoreEvidenceState) {
        List<String> signals = new ArrayList<>(row.riskSignals());
        signals.addAll(row.unknownSignals());
        signals.addAll(row.degradedSignals());
        if (DecisionExplorerCandidateTradeoffScoringExplanationV1.SCORE_EVIDENCE_SELECTED_BASELINE_UNKNOWN
                .equals(scoreEvidenceState)
                || DecisionExplorerCandidateTradeoffScoringExplanationV1.SCORE_EVIDENCE_ALTERNATIVE_DELTA_UNKNOWN
                        .equals(scoreEvidenceState)) {
            signals.add("score evidence is incomplete for tradeoff explanation");
        }
        if (copyNonNull(factorDiagnostics).isEmpty()) {
            signals.add("factor diagnostics were not returned for candidate");
        }
        return distinctSorted(signals);
    }

    private static String scoringSummary(
            DecisionExplorerRouteTradeoffRowV1 row,
            String explanationStatus,
            String scoreEvidenceState,
            String factorStatusRollup) {
        if (row.selected()) {
            return "Candidate " + row.candidateId() + " is the selected scoring baseline with "
                    + explanationStatus + " tradeoff explanation, " + scoreEvidenceState
                    + ", score gap " + row.scoreGapCategory() + ", and factor rollup "
                    + factorStatusRollup + ".";
        }
        return "Candidate " + row.candidateId() + " has " + explanationStatus
                + " alternative scoring explanation, " + scoreEvidenceState
                + ", score delta " + DecisionExplorerDtoSupport.valueOrUnknown(
                        row.scoreDeltaFromSelected() == null ? null : row.scoreDeltaFromSelected().toString())
                + ", tradeoff category " + row.tradeoffCategory() + ", and factor rollup "
                + factorStatusRollup + ".";
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
