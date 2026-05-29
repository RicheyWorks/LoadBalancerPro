package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class DecisionExplorerShadowCandidateOutcomeBuilder {
    List<DecisionExplorerShadowCandidateOutcomeV1> build(
            List<DecisionExplorerRouteTradeoffRowV1> tradeoffRows,
            String boundaryNote) {
        return DecisionExplorerDtoSupport.copyOrEmpty(tradeoffRows).stream()
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

    private static List<String> distinctSorted(List<String> values) {
        Set<String> distinct = new LinkedHashSet<>(DecisionExplorerDtoSupport.copyOrEmpty(values));
        return distinct.stream()
                .filter(value -> !DecisionExplorerDtoSupport.valueOrUnknown(value).equals("UNKNOWN"))
                .sorted()
                .toList();
    }

    private static String value(Double value) {
        return value == null ? "UNKNOWN" : Double.toString(value);
    }
}
