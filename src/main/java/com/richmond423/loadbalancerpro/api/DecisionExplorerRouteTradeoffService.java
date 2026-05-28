package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DecisionExplorerRouteTradeoffService {
    private static final Comparator<DecisionExplorerRouteTradeoffRowV1> BY_SELECTED_THEN_ORDER = Comparator
            .comparing(DecisionExplorerRouteTradeoffRowV1::selected)
            .reversed()
            .thenComparingInt(row -> row.displayOrder() > 0 ? row.displayOrder() : Integer.MAX_VALUE)
            .thenComparing(DecisionExplorerRouteTradeoffRowV1::candidateId);

    public DecisionExplorerRouteTradeoffAnalysisV1 buildTradeoffs(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            String boundaryNote) {
        if (confidenceSummary == null || routingDiagnostics == null) {
            return DecisionExplorerRouteTradeoffAnalysisV1.unknown(boundaryNote);
        }

        List<DecisionExplorerRouteTradeoffRowV1> rows = routingDiagnostics.candidateDiagnostics().stream()
                .filter(Objects::nonNull)
                .map(diagnostic -> tradeoffRow(diagnostic, boundaryNote))
                .sorted(BY_SELECTED_THEN_ORDER)
                .toList();
        List<DecisionExplorerRouteTradeoffRowV1> alternatives = rows.stream()
                .filter(row -> !row.selected())
                .toList();
        DecisionExplorerRouteTradeoffRowV1 closestAlternative = closestAlternative(alternatives);
        String tradeoffCategory = tradeoffCategory(confidenceSummary, alternatives);

        return new DecisionExplorerRouteTradeoffAnalysisV1(
                true,
                true,
                DecisionExplorerRouteTradeoffAnalysisV1.ANALYSIS_OBJECT,
                DecisionExplorerRouteTradeoffAnalysisV1.CONTRACT_VERSION,
                confidenceSummary.status(),
                confidenceSummary.evidenceQuality(),
                confidenceSummary.selectedCandidateId(),
                tradeoffCategory,
                selectedSummary(routingDiagnostics.selectedCandidateDiagnostic(), rows),
                alternativeSummary(alternatives, closestAlternative, tradeoffCategory),
                rows.size(),
                alternatives.size(),
                comparedAlternativeCount(alternatives),
                closestAlternative == null ? "UNKNOWN" : closestAlternative.candidateId(),
                closestAlternative == null ? null : closestAlternative.scoreDeltaFromSelected(),
                rows,
                tradeoffReasons(confidenceSummary, routingDiagnostics, rows, tradeoffCategory),
                distinctSorted(routingDiagnostics.warnings()),
                tradeoffUnknowns(routingDiagnostics, rows),
                distinctSorted(routingDiagnostics.sourceReferenceIds()),
                boundaryNote);
    }

    private static DecisionExplorerRouteTradeoffRowV1 tradeoffRow(
            DecisionExplorerCandidateDiagnosticV1 diagnostic,
            String boundaryNote) {
        String tradeoffCategory = DecisionExplorerRouteTradeoffRowV1.categoryFor(
                diagnostic.selected(),
                diagnostic.scoreDeltaFromSelected());
        String scoreGapCategory = DecisionExplorerRouteTradeoffRowV1.scoreGapCategoryFor(
                diagnostic.selected(),
                diagnostic.scoreDeltaFromSelected());
        String classification = DecisionExplorerRouteTradeoffRowV1.classificationFor(
                tradeoffCategory,
                diagnostic.riskLevel(),
                diagnostic.unknownSignalCount(),
                diagnostic.degradedSignalCount());
        List<String> benefitSignals = benefitSignals(diagnostic, tradeoffCategory);
        List<String> riskSignals = riskSignals(diagnostic, tradeoffCategory);
        List<String> reasonCodes = new ArrayList<>(diagnostic.reasonCodes());
        reasonCodes.add("TRADEOFF_CATEGORY_" + tradeoffCategory);
        reasonCodes.add("TRADEOFF_CLASSIFICATION_" + classification);
        reasonCodes.add("SCORE_GAP_" + scoreGapCategory);

        return new DecisionExplorerRouteTradeoffRowV1(
                diagnostic.candidateId(),
                diagnostic.candidateLabel(),
                diagnostic.selected(),
                diagnostic.displayOrder(),
                tradeoffCategory,
                classification,
                diagnostic.diagnosticStatus(),
                diagnostic.riskLevel(),
                diagnostic.healthEvidenceState(),
                diagnostic.finalScore(),
                diagnostic.scoreDeltaFromSelected(),
                scoreGapCategory,
                scoringExplanation(diagnostic, tradeoffCategory, scoreGapCategory),
                evidenceSummary(diagnostic),
                benefitSignals,
                riskSignals,
                diagnostic.unknownSignals(),
                diagnostic.degradedSignals(),
                distinctSorted(reasonCodes),
                diagnostic.sourceReferenceIds(),
                boundaryNote);
    }

    private static String tradeoffCategory(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            List<DecisionExplorerRouteTradeoffRowV1> alternatives) {
        if (DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(confidenceSummary.status())) {
            return "DEGRADED";
        }
        if (alternatives.isEmpty()) {
            return "NO_ALTERNATIVE";
        }
        if (alternatives.stream().anyMatch(row -> DecisionExplorerRouteTradeoffRowV1
                .TRADEOFF_ALTERNATIVE_BEATS_SELECTED.equals(row.tradeoffCategory()))) {
            return "SELECTED_CHALLENGED";
        }
        if (alternatives.stream().anyMatch(row -> DecisionExplorerRouteTradeoffRowV1
                .TRADEOFF_ALTERNATIVE_CLOSE.equals(row.tradeoffCategory()))) {
            return "CLOSE_ALTERNATIVE";
        }
        if (alternatives.stream().anyMatch(row -> DecisionExplorerRouteTradeoffRowV1
                .TRADEOFF_ALTERNATIVE_UNKNOWN.equals(row.tradeoffCategory()))) {
            return "PARTIAL_TRADEOFF";
        }
        if (alternatives.stream().allMatch(row -> DecisionExplorerRouteTradeoffRowV1
                .TRADEOFF_ALTERNATIVE_TRAILS_SELECTED.equals(row.tradeoffCategory()))) {
            return "SELECTED_ADVANTAGE";
        }
        return "UNKNOWN";
    }

    private static DecisionExplorerRouteTradeoffRowV1 closestAlternative(
            List<DecisionExplorerRouteTradeoffRowV1> alternatives) {
        return alternatives.stream()
                .filter(row -> row.scoreDeltaFromSelected() != null)
                .min(Comparator
                        .comparingDouble((DecisionExplorerRouteTradeoffRowV1 row) ->
                                Math.abs(row.scoreDeltaFromSelected()))
                        .thenComparingInt(row -> row.displayOrder() > 0 ? row.displayOrder() : Integer.MAX_VALUE)
                        .thenComparing(DecisionExplorerRouteTradeoffRowV1::candidateId))
                .orElse(null);
    }

    private static int comparedAlternativeCount(List<DecisionExplorerRouteTradeoffRowV1> alternatives) {
        return (int) alternatives.stream()
                .filter(row -> row.scoreDeltaFromSelected() != null)
                .count();
    }

    private static String selectedSummary(
            DecisionExplorerCandidateDiagnosticV1 selectedDiagnostic,
            List<DecisionExplorerRouteTradeoffRowV1> rows) {
        DecisionExplorerRouteTradeoffRowV1 selectedRow = rows.stream()
                .filter(DecisionExplorerRouteTradeoffRowV1::selected)
                .findFirst()
                .orElse(null);
        if (selectedRow == null) {
            return "Selected candidate tradeoff baseline was unavailable.";
        }
        String diagnosticSummary = selectedDiagnostic == null
                ? "selected candidate diagnostic summary was unavailable"
                : DecisionExplorerDtoSupport.valueOrUnknown(selectedDiagnostic.summaryText());
        return "Selected candidate " + selectedRow.candidateId() + " is the tradeoff baseline with "
                + selectedRow.diagnosticStatus() + " diagnostics, " + selectedRow.riskLevel()
                + " risk, and score gap category " + selectedRow.scoreGapCategory() + ". "
                + diagnosticSummary;
    }

    private static String alternativeSummary(
            List<DecisionExplorerRouteTradeoffRowV1> alternatives,
            DecisionExplorerRouteTradeoffRowV1 closestAlternative,
            String tradeoffCategory) {
        if (alternatives.isEmpty()) {
            return "No alternative candidate diagnostics were available for route tradeoff analysis.";
        }
        String closest = closestAlternative == null
                ? "no score-comparable closest alternative"
                : "closest alternative " + closestAlternative.candidateId() + " with score delta "
                        + closestAlternative.scoreDeltaFromSelected();
        return alternatives.size() + " alternative candidate(s) were analyzed; " + closest
                + "; route tradeoff category is " + tradeoffCategory + ".";
    }

    private static String scoringExplanation(
            DecisionExplorerCandidateDiagnosticV1 diagnostic,
            String tradeoffCategory,
            String scoreGapCategory) {
        if (diagnostic.selected()) {
            return "Selected candidate " + diagnostic.candidateId()
                    + " provides the baseline score for read-only route tradeoff analysis.";
        }
        if (diagnostic.scoreDeltaFromSelected() == null) {
            return "Alternative candidate " + diagnostic.candidateId()
                    + " could not be score-compared because its score delta from selected was unavailable.";
        }
        return "Alternative candidate " + diagnostic.candidateId() + " has score delta "
                + diagnostic.scoreDeltaFromSelected() + " from selected; tradeoff category "
                + tradeoffCategory + " and score gap " + scoreGapCategory + ".";
    }

    private static String evidenceSummary(DecisionExplorerCandidateDiagnosticV1 diagnostic) {
        return "Candidate " + diagnostic.candidateId() + " has " + diagnostic.visibleSignalCount()
                + " visible signal(s), " + diagnostic.warningCount() + " warning(s), "
                + diagnostic.unknownSignalCount() + " unknown signal(s), and "
                + diagnostic.degradedSignalCount() + " degraded signal(s).";
    }

    private static List<String> benefitSignals(
            DecisionExplorerCandidateDiagnosticV1 diagnostic,
            String tradeoffCategory) {
        List<String> signals = new ArrayList<>(diagnostic.strengthSignals());
        if (diagnostic.selected()) {
            signals.add("selected candidate is the comparison baseline");
        } else if (DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_TRAILS_SELECTED
                .equals(tradeoffCategory)) {
            signals.add("alternative trails selected by returned score delta");
        }
        return distinctSorted(signals);
    }

    private static List<String> riskSignals(
            DecisionExplorerCandidateDiagnosticV1 diagnostic,
            String tradeoffCategory) {
        List<String> signals = new ArrayList<>(diagnostic.weakSignals());
        signals.addAll(diagnostic.degradedSignals());
        if (DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_BEATS_SELECTED.equals(tradeoffCategory)) {
            signals.add("alternative beats selected by returned score delta");
        }
        if (DecisionExplorerCandidateDiagnosticV1.RISK_HIGH.equals(diagnostic.riskLevel())) {
            signals.add("candidate diagnostic risk is HIGH");
        }
        return distinctSorted(signals);
    }

    private static List<String> tradeoffReasons(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            String tradeoffCategory) {
        List<String> reasons = new ArrayList<>();
        reasons.add("ROUTE_TRADEOFF_CATEGORY_" + tradeoffCategory);
        reasons.add("SELECTED_CANDIDATE_" + confidenceSummary.selectedCandidateId());
        reasons.add("ROUTE_TRADEOFF_ROW_COUNT_" + rows.size());
        reasons.addAll(confidenceSummary.statusReasons());
        reasons.addAll(routingDiagnostics.diagnosticReasons());
        rows.stream()
                .flatMap(row -> row.reasonCodes().stream())
                .forEach(reasons::add);
        return distinctSorted(reasons);
    }

    private static List<String> tradeoffUnknowns(
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            List<DecisionExplorerRouteTradeoffRowV1> rows) {
        List<String> unknowns = new ArrayList<>(routingDiagnostics.unknowns());
        rows.stream()
                .flatMap(row -> row.unknownSignals().stream())
                .forEach(unknowns::add);
        return distinctSorted(unknowns);
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
