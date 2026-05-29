package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

final class DecisionExplorerFactorTradeoffDeltaBuilder {
    List<DecisionExplorerFactorTradeoffDeltaV1> build(
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
                .sorted(DecisionExplorerRouteTradeoffRowBuilder.BY_SELECTED_THEN_ORDER)
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

    private static <T> List<T> copyNonNull(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null)
                .toList();
    }

    private static List<String> distinctSorted(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> distinct = new LinkedHashSet<>();
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .sorted()
                .forEach(distinct::add);
        return List.copyOf(distinct);
    }
}
