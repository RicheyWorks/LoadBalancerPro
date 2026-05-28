package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
        List<DecisionExplorerCandidateTradeoffScoringExplanationV1> scoringExplanations =
                scoringExplanations(rows, routingDiagnostics.factorDiagnostics(), boundaryNote);
        List<DecisionExplorerFactorTradeoffDeltaV1> factorTradeoffDeltas =
                factorTradeoffDeltas(rows, routingDiagnostics.factorDiagnostics(), boundaryNote);

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
                scoringExplanations,
                factorTradeoffDeltas,
                tradeoffReasons(confidenceSummary, routingDiagnostics, rows, tradeoffCategory),
                distinctSorted(routingDiagnostics.warnings()),
                tradeoffUnknowns(routingDiagnostics, rows),
                distinctSorted(routingDiagnostics.sourceReferenceIds()),
                boundaryNote);
    }

    private static List<DecisionExplorerCandidateTradeoffScoringExplanationV1> scoringExplanations(
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
                .sorted(BY_SELECTED_THEN_ORDER)
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

    private static List<DecisionExplorerFactorTradeoffDeltaV1> factorTradeoffDeltas(
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<DecisionExplorerFactorDiagnosticV1> factorDiagnostics,
            String boundaryNote) {
        DecisionExplorerRouteTradeoffRowV1 selectedRow = copyNonNull(rows).stream()
                .filter(DecisionExplorerRouteTradeoffRowV1::selected)
                .findFirst()
                .orElse(null);
        if (selectedRow == null) {
            return List.of();
        }

        Map<String, Map<String, DecisionExplorerFactorDiagnosticV1>> factorsByCandidate =
                factorsByCandidateAndName(factorDiagnostics);
        Map<String, DecisionExplorerFactorDiagnosticV1> selectedFactors =
                factorsByCandidate.getOrDefault(selectedRow.candidateId(), Map.of());
        List<DecisionExplorerRouteTradeoffRowV1> alternatives = copyNonNull(rows).stream()
                .filter(row -> !row.selected())
                .sorted(BY_SELECTED_THEN_ORDER)
                .toList();

        List<DecisionExplorerFactorTradeoffDeltaV1> deltas = new ArrayList<>();
        for (DecisionExplorerRouteTradeoffRowV1 alternativeRow : alternatives) {
            Map<String, DecisionExplorerFactorDiagnosticV1> alternativeFactors =
                    factorsByCandidate.getOrDefault(alternativeRow.candidateId(), Map.of());
            TreeSet<String> factorNames = new TreeSet<>();
            factorNames.addAll(selectedFactors.keySet());
            factorNames.addAll(alternativeFactors.keySet());
            for (String factorName : factorNames) {
                deltas.add(factorTradeoffDelta(
                        factorName,
                        selectedRow,
                        alternativeRow,
                        selectedFactors.get(factorName),
                        alternativeFactors.get(factorName),
                        boundaryNote));
            }
        }
        return List.copyOf(deltas);
    }

    private static Map<String, Map<String, DecisionExplorerFactorDiagnosticV1>> factorsByCandidateAndName(
            List<DecisionExplorerFactorDiagnosticV1> factorDiagnostics) {
        TreeMap<String, Map<String, DecisionExplorerFactorDiagnosticV1>> factorsByCandidate = new TreeMap<>();
        copyNonNull(factorDiagnostics).stream()
                .sorted(Comparator
                        .comparingInt((DecisionExplorerFactorDiagnosticV1 factor) ->
                                factor.displayOrder() > 0 ? factor.displayOrder() : Integer.MAX_VALUE)
                        .thenComparing(DecisionExplorerFactorDiagnosticV1::candidateId)
                        .thenComparing(DecisionExplorerFactorDiagnosticV1::factorName))
                .forEach(factor -> factorsByCandidate
                        .computeIfAbsent(factor.candidateId(), ignored -> new TreeMap<>())
                        .putIfAbsent(factor.factorName(), factor));
        return Map.copyOf(factorsByCandidate);
    }

    private static DecisionExplorerFactorTradeoffDeltaV1 factorTradeoffDelta(
            String factorName,
            DecisionExplorerRouteTradeoffRowV1 selectedRow,
            DecisionExplorerRouteTradeoffRowV1 alternativeRow,
            DecisionExplorerFactorDiagnosticV1 selectedFactor,
            DecisionExplorerFactorDiagnosticV1 alternativeFactor,
            String boundaryNote) {
        String classification = DecisionExplorerFactorTradeoffDeltaV1.classificationFor(
                selectedFactor,
                alternativeFactor);
        List<String> selectedSignals = factorSignals("selected", selectedFactor);
        List<String> alternativeSignals = factorSignals("alternative", alternativeFactor);
        List<String> limitationSignals = factorLimitationSignals(classification, selectedFactor, alternativeFactor);
        List<String> reasonCodes = factorDeltaReasonCodes(
                classification,
                selectedRow,
                alternativeRow,
                selectedFactor,
                alternativeFactor);
        List<String> sourceReferenceIds = factorDeltaSourceReferences(selectedFactor, alternativeFactor,
                alternativeRow);
        return new DecisionExplorerFactorTradeoffDeltaV1(
                factorName,
                selectedRow.candidateId(),
                alternativeRow.candidateId(),
                factorDisplayOrder(selectedFactor, alternativeFactor, alternativeRow),
                classification,
                contributionOf(selectedFactor),
                contributionOf(alternativeFactor),
                factorStatusOf(selectedFactor),
                factorStatusOf(alternativeFactor),
                evidenceStatusOf(selectedFactor),
                evidenceStatusOf(alternativeFactor),
                observedValueOf(selectedFactor),
                observedValueOf(alternativeFactor),
                alternativeRow.tradeoffCategory(),
                alternativeRow.scoreGapCategory(),
                alternativeRow.scoreDeltaFromSelected(),
                factorDeltaSummary(factorName, selectedRow, alternativeRow, selectedFactor, alternativeFactor,
                        classification),
                selectedSignals,
                alternativeSignals,
                limitationSignals,
                reasonCodes,
                sourceReferenceIds,
                boundaryNote);
    }

    private static int factorDisplayOrder(
            DecisionExplorerFactorDiagnosticV1 selectedFactor,
            DecisionExplorerFactorDiagnosticV1 alternativeFactor,
            DecisionExplorerRouteTradeoffRowV1 alternativeRow) {
        int selectedOrder = selectedFactor == null ? Integer.MAX_VALUE : selectedFactor.displayOrder();
        int alternativeOrder = alternativeFactor == null ? Integer.MAX_VALUE : alternativeFactor.displayOrder();
        int order = Math.min(
                selectedOrder > 0 ? selectedOrder : Integer.MAX_VALUE,
                alternativeOrder > 0 ? alternativeOrder : Integer.MAX_VALUE);
        return order == Integer.MAX_VALUE ? alternativeRow.displayOrder() : order;
    }

    private static List<String> factorSignals(String prefix, DecisionExplorerFactorDiagnosticV1 factor) {
        List<String> signals = new ArrayList<>();
        if (factor == null) {
            signals.add(prefix + " factor evidence was not returned");
            return signals;
        }
        signals.add(prefix + " contribution=" + factor.contribution());
        signals.add(prefix + " factorStatus=" + factor.factorStatus());
        signals.add(prefix + " evidenceStatus=" + factor.evidenceStatus());
        factor.supportingSignals().stream()
                .map(signal -> prefix + " supporting: " + signal)
                .forEach(signals::add);
        factor.warningSignals().stream()
                .map(signal -> prefix + " warning: " + signal)
                .forEach(signals::add);
        return distinctSorted(signals);
    }

    private static List<String> factorLimitationSignals(
            String classification,
            DecisionExplorerFactorDiagnosticV1 selectedFactor,
            DecisionExplorerFactorDiagnosticV1 alternativeFactor) {
        List<String> signals = new ArrayList<>();
        if (selectedFactor == null) {
            signals.add("selected factor evidence was not returned");
        }
        if (alternativeFactor == null) {
            signals.add("alternative factor evidence was not returned");
        }
        if (DecisionExplorerFactorTradeoffDeltaV1.DELTA_UNKNOWN.equals(classification)) {
            signals.add("factor tradeoff delta is unknown from returned evidence");
        }
        if (DecisionExplorerFactorTradeoffDeltaV1.DELTA_DEGRADED.equals(classification)) {
            signals.add("factor tradeoff delta includes degraded evidence");
        }
        addFactorLimitations(signals, "selected", selectedFactor);
        addFactorLimitations(signals, "alternative", alternativeFactor);
        return distinctSorted(signals);
    }

    private static void addFactorLimitations(
            List<String> signals,
            String prefix,
            DecisionExplorerFactorDiagnosticV1 factor) {
        if (factor == null) {
            return;
        }
        factor.unknownSignals().stream()
                .map(signal -> prefix + " unknown: " + signal)
                .forEach(signals::add);
        factor.degradedSignals().stream()
                .map(signal -> prefix + " degraded: " + signal)
                .forEach(signals::add);
        factor.warningSignals().stream()
                .map(signal -> prefix + " warning: " + signal)
                .forEach(signals::add);
    }

    private static List<String> factorDeltaReasonCodes(
            String classification,
            DecisionExplorerRouteTradeoffRowV1 selectedRow,
            DecisionExplorerRouteTradeoffRowV1 alternativeRow,
            DecisionExplorerFactorDiagnosticV1 selectedFactor,
            DecisionExplorerFactorDiagnosticV1 alternativeFactor) {
        List<String> reasons = new ArrayList<>();
        reasons.add("FACTOR_TRADEOFF_DELTA_" + classification);
        reasons.add("SELECTED_CANDIDATE_" + selectedRow.candidateId());
        reasons.add("ALTERNATIVE_CANDIDATE_" + alternativeRow.candidateId());
        reasons.add("CANDIDATE_TRADEOFF_CATEGORY_" + alternativeRow.tradeoffCategory());
        reasons.add("SCORE_GAP_" + alternativeRow.scoreGapCategory());
        if (selectedFactor == null) {
            reasons.add("SELECTED_FACTOR_MISSING");
        } else {
            selectedFactor.reasonCodes().stream()
                    .map(reason -> "SELECTED_" + reason)
                    .forEach(reasons::add);
        }
        if (alternativeFactor == null) {
            reasons.add("ALTERNATIVE_FACTOR_MISSING");
        } else {
            alternativeFactor.reasonCodes().stream()
                    .map(reason -> "ALTERNATIVE_" + reason)
                    .forEach(reasons::add);
        }
        return distinctSorted(reasons);
    }

    private static List<String> factorDeltaSourceReferences(
            DecisionExplorerFactorDiagnosticV1 selectedFactor,
            DecisionExplorerFactorDiagnosticV1 alternativeFactor,
            DecisionExplorerRouteTradeoffRowV1 alternativeRow) {
        List<String> references = new ArrayList<>(alternativeRow.sourceReferenceIds());
        if (selectedFactor != null) {
            references.addAll(selectedFactor.sourceReferenceIds());
        }
        if (alternativeFactor != null) {
            references.addAll(alternativeFactor.sourceReferenceIds());
        }
        return distinctSorted(references);
    }

    private static String factorDeltaSummary(
            String factorName,
            DecisionExplorerRouteTradeoffRowV1 selectedRow,
            DecisionExplorerRouteTradeoffRowV1 alternativeRow,
            DecisionExplorerFactorDiagnosticV1 selectedFactor,
            DecisionExplorerFactorDiagnosticV1 alternativeFactor,
            String classification) {
        if (selectedFactor == null || alternativeFactor == null) {
            return "Factor " + factorName + " could not be fully compared between selected candidate "
                    + selectedRow.candidateId() + " and alternative " + alternativeRow.candidateId()
                    + " because matching factor evidence was missing.";
        }
        return "Factor " + factorName + " is " + classification + " for selected candidate "
                + selectedRow.candidateId() + " versus alternative " + alternativeRow.candidateId()
                + " using returned factor contributions selected=" + selectedFactor.contribution()
                + " and alternative=" + alternativeFactor.contribution() + ".";
    }

    private static String contributionOf(DecisionExplorerFactorDiagnosticV1 factor) {
        return factor == null
                ? DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_UNKNOWN
                : factor.contribution();
    }

    private static String factorStatusOf(DecisionExplorerFactorDiagnosticV1 factor) {
        return factor == null
                ? DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN
                : factor.factorStatus();
    }

    private static String evidenceStatusOf(DecisionExplorerFactorDiagnosticV1 factor) {
        return factor == null ? "UNKNOWN" : factor.evidenceStatus();
    }

    private static String observedValueOf(DecisionExplorerFactorDiagnosticV1 factor) {
        return factor == null ? "UNKNOWN" : factor.observedValueOrStatus();
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
        if (DecisionExplorerCandidateTradeoffScoringExplanationV1.SCORE_EVIDENCE_ALTERNATIVE_DELTA_UNKNOWN
                .equals(scoreEvidenceState)
                || DecisionExplorerCandidateTradeoffScoringExplanationV1.SCORE_EVIDENCE_SELECTED_BASELINE_UNKNOWN
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
        if (DecisionExplorerCandidateTradeoffScoringExplanationV1.SCORE_EVIDENCE_ALTERNATIVE_DELTA_UNKNOWN
                .equals(scoreEvidenceState)
                || DecisionExplorerCandidateTradeoffScoringExplanationV1.SCORE_EVIDENCE_SELECTED_BASELINE_UNKNOWN
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

    private static <T> List<T> copyNonNull(List<T> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .filter(Objects::nonNull)
                        .toList();
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
