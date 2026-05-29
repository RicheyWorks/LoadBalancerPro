package com.richmond423.loadbalancerpro.api;

import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticListSupport.distinctSorted;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

final class DecisionExplorerRouteTradeoffRowBuilder {
    static final Comparator<DecisionExplorerRouteTradeoffRowV1> BY_SELECTED_THEN_ORDER = Comparator
            .comparing(DecisionExplorerRouteTradeoffRowV1::selected)
            .reversed()
            .thenComparingInt(row -> row.displayOrder() > 0 ? row.displayOrder() : Integer.MAX_VALUE)
            .thenComparing(DecisionExplorerRouteTradeoffRowV1::candidateId);

    List<DecisionExplorerRouteTradeoffRowV1> build(
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            String boundaryNote) {
        if (routingDiagnostics == null) {
            return List.of();
        }
        return routingDiagnostics.candidateDiagnostics().stream()
                .filter(Objects::nonNull)
                .map(diagnostic -> tradeoffRow(diagnostic, boundaryNote))
                .sorted(BY_SELECTED_THEN_ORDER)
                .toList();
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

}
