package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DecisionExplorerFactorDiagnosticsService {

    public List<DecisionExplorerFactorDiagnosticV1> buildFactorDiagnostics(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            List<DecisionFactorDrilldownV1> factorDrilldowns,
            String boundaryNote) {
        if (confidenceSummary == null) {
            return List.of();
        }

        Map<String, FactorEvidence> evidenceByKey = new HashMap<>();
        for (DecisionFactorDrilldownV1 drilldown : copyNonNull(factorDrilldowns)) {
            FactorEvidence evidence = evidenceFor(evidenceByKey, drilldown.candidateId(), drilldown.factorName());
            evidence.observedValueOrStatus(drilldown.observedValueOrStatus());
            evidence.influenceCategory(drilldown.influenceCategory());
            evidence.evidenceStatus(drilldown.evidenceStatus());
            evidence.explanation(drilldown.explanation());
            evidence.warnings.addAll(drilldown.warnings());
            evidence.unknowns.addAll(drilldown.unknowns());
            evidence.sourceReferenceIds.addAll(drilldown.sourceReferenceIds());
        }

        for (DecisionExplorerFactorStatusV1 factorStatus : confidenceSummary.factorStatusDetails()) {
            if (factorStatus == null) {
                continue;
            }
            FactorEvidence evidence = evidenceFor(evidenceByKey, factorStatus.candidateId(), factorStatus.factorName());
            evidence.displayOrder(factorStatus.displayOrder());
            evidence.factorStatus(factorStatus.factorStatus());
            evidence.evidenceStatus(factorStatus.evidenceStatus());
            evidence.observedValueOrStatus(factorStatus.observedValueOrStatus());
            evidence.influenceCategory(factorStatus.influenceCategory());
            evidence.interpretation(factorStatus.interpretation());
            evidence.statusReasons.addAll(factorStatus.statusReasons());
            evidence.warnings.addAll(factorStatus.warnings());
            evidence.unknowns.addAll(factorStatus.unknowns());
            evidence.sourceReferenceIds.addAll(factorStatus.sourceReferenceIds());
        }

        return evidenceByKey.values().stream()
                .filter(evidence -> !"UNKNOWN".equals(evidence.candidateId)
                        || !"UNKNOWN".equals(evidence.factorName))
                .sorted(Comparator
                        .comparingInt(FactorEvidence::sortOrder)
                        .thenComparing(evidence -> evidence.candidateId)
                        .thenComparing(evidence -> evidence.factorName)
                        .thenComparing((FactorEvidence evidence) -> evidence.observedValueOrStatus()))
                .map(evidence -> factorDiagnostic(evidence, boundaryNote))
                .toList();
    }

    public List<String> degradationReasons(List<DecisionExplorerFactorDiagnosticV1> factorDiagnostics) {
        return factorDiagnosticsWithContribution(factorDiagnostics, DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_DEGRADED)
                .stream()
                .map(diagnostic -> diagnostic.candidateId() + ":" + diagnostic.factorName() + ":"
                        + String.join("|", diagnostic.degradedSignals()))
                .toList();
    }

    public List<String> partialEvidenceReasons(List<DecisionExplorerFactorDiagnosticV1> factorDiagnostics) {
        return factorDiagnosticsWithContribution(factorDiagnostics, DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_WARNING)
                .stream()
                .map(diagnostic -> diagnostic.candidateId() + ":" + diagnostic.factorName() + ":"
                        + String.join("|", diagnostic.warningSignals()))
                .toList();
    }

    public List<String> unknownEvidenceReasons(List<DecisionExplorerFactorDiagnosticV1> factorDiagnostics) {
        return factorDiagnosticsWithContribution(factorDiagnostics, DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_UNKNOWN)
                .stream()
                .map(diagnostic -> diagnostic.candidateId() + ":" + diagnostic.factorName() + ":"
                        + String.join("|", diagnostic.unknownSignals()))
                .toList();
    }

    private static List<DecisionExplorerFactorDiagnosticV1> factorDiagnosticsWithContribution(
            List<DecisionExplorerFactorDiagnosticV1> factorDiagnostics,
            String contribution) {
        return copyNonNull(factorDiagnostics).stream()
                .filter(diagnostic -> contribution.equals(diagnostic.contribution()))
                .sorted(Comparator
                        .comparingInt((DecisionExplorerFactorDiagnosticV1 diagnostic) ->
                                diagnostic.displayOrder() > 0 ? diagnostic.displayOrder() : Integer.MAX_VALUE)
                        .thenComparing(DecisionExplorerFactorDiagnosticV1::candidateId)
                        .thenComparing(DecisionExplorerFactorDiagnosticV1::factorName))
                .toList();
    }

    private static DecisionExplorerFactorDiagnosticV1 factorDiagnostic(
            FactorEvidence evidence,
            String boundaryNote) {
        List<String> degradedSignals = degradedSignals(evidence);
        List<String> unknownSignals = unknownSignals(evidence);
        List<String> warningSignals = warningSignals(evidence);
        List<String> supportingSignals = supportingSignals(evidence, warningSignals, unknownSignals, degradedSignals);
        String contribution = contribution(evidence, warningSignals, unknownSignals, degradedSignals);
        List<String> reasonCodes = reasonCodes(
                evidence,
                contribution,
                supportingSignals,
                warningSignals,
                unknownSignals,
                degradedSignals);
        return new DecisionExplorerFactorDiagnosticV1(
                evidence.candidateId,
                evidence.factorName,
                evidence.displayOrder(),
                contribution,
                evidence.factorStatus(),
                evidence.evidenceStatus(),
                evidence.observedValueOrStatus(),
                evidence.influenceCategory(),
                DecisionExplorerFactorDiagnosticV1.severityFor(contribution),
                distinctSorted(evidence.warnings).size(),
                unknownSignals.size(),
                degradedSignals.size(),
                missingSignalCount(evidence, unknownSignals),
                summaryText(evidence, contribution),
                supportingSignals,
                warningSignals,
                unknownSignals,
                degradedSignals,
                reasonCodes,
                distinctSorted(evidence.sourceReferenceIds),
                boundaryNote);
    }

    private static String contribution(
            FactorEvidence evidence,
            List<String> warningSignals,
            List<String> unknownSignals,
            List<String> degradedSignals) {
        if (!degradedSignals.isEmpty()
                || DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(evidence.factorStatus())) {
            return DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_DEGRADED;
        }
        if (DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(evidence.factorStatus())
                || "UNKNOWN".equals(evidence.evidenceStatus())) {
            return DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_UNKNOWN;
        }
        if (!warningSignals.isEmpty()
                || DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(evidence.factorStatus())
                || "WEAKENS_SELECTION".equals(evidence.influenceCategory())) {
            return DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_WARNING;
        }
        if (!unknownSignals.isEmpty()) {
            return DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_UNKNOWN;
        }
        if ("NEUTRAL".equals(evidence.influenceCategory())) {
            return DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_NEUTRAL;
        }
        if ("SUPPORTS_SELECTION".equals(evidence.influenceCategory())) {
            return DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_SUPPORTING;
        }
        return DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_UNKNOWN;
    }

    private static List<String> supportingSignals(
            FactorEvidence evidence,
            List<String> warningSignals,
            List<String> unknownSignals,
            List<String> degradedSignals) {
        Set<String> excluded = new LinkedHashSet<>();
        excluded.addAll(warningSignals);
        excluded.addAll(unknownSignals);
        excluded.addAll(degradedSignals);
        List<String> signals = new ArrayList<>();
        if ("AVAILABLE".equals(evidence.evidenceStatus())) {
            signals.add("evidenceStatus=AVAILABLE");
        }
        if ("SUPPORTS_SELECTION".equals(evidence.influenceCategory())) {
            signals.add("influenceCategory=SUPPORTS_SELECTION");
        }
        if ("NEUTRAL".equals(evidence.influenceCategory())) {
            signals.add("influenceCategory=NEUTRAL");
        }
        evidence.statusReasons.stream()
                .filter(reason -> !isWeakReason(reason) && !isDegradedSignal(reason))
                .forEach(signals::add);
        signals.removeIf(excluded::contains);
        return distinctSorted(signals);
    }

    private static List<String> warningSignals(FactorEvidence evidence) {
        List<String> signals = new ArrayList<>();
        signals.addAll(evidence.warnings);
        if (DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(evidence.factorStatus())) {
            signals.add("factorStatus=PARTIAL");
        }
        if ("PARTIAL".equals(evidence.evidenceStatus())) {
            signals.add("evidenceStatus=PARTIAL");
        }
        if ("WEAKENS_SELECTION".equals(evidence.influenceCategory())) {
            signals.add("influenceCategory=WEAKENS_SELECTION");
        }
        evidence.statusReasons.stream()
                .filter(DecisionExplorerFactorDiagnosticsService::isWeakReason)
                .forEach(signals::add);
        return distinctSorted(signals);
    }

    private static List<String> unknownSignals(FactorEvidence evidence) {
        List<String> signals = new ArrayList<>();
        signals.addAll(evidence.unknowns);
        if ("UNKNOWN".equals(evidence.candidateId)) {
            signals.add("factor candidate id was not returned");
        }
        if ("UNKNOWN".equals(evidence.factorName)) {
            signals.add("factor name was not returned");
        }
        if ("UNKNOWN".equals(evidence.evidenceStatus())) {
            signals.add("factor evidence status is unknown");
        }
        if ("UNKNOWN".equals(evidence.observedValueOrStatus())) {
            signals.add("factor observed value was not returned");
        }
        if ("UNKNOWN".equals(evidence.influenceCategory())
                || "UNKNOWN_INFLUENCE".equals(evidence.influenceCategory())) {
            signals.add("factor influence category is unknown");
        }
        if ("UNKNOWN".equals(evidence.explanation())
                && "UNKNOWN".equals(evidence.interpretation())) {
            signals.add("factor explanation was not returned");
        }
        if (evidence.sourceReferenceIds.isEmpty()) {
            signals.add("factor source references were not returned");
        }
        evidence.statusReasons.stream()
                .filter(reason -> DecisionExplorerDtoSupport.valueOrUnknown(reason).contains("UNKNOWN"))
                .forEach(signals::add);
        return distinctSorted(signals);
    }

    private static List<String> degradedSignals(FactorEvidence evidence) {
        List<String> signals = new ArrayList<>();
        if (DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(evidence.factorStatus())) {
            signals.add("factorStatus=DEGRADED");
        }
        if (isDegradedEvidenceStatus(evidence.evidenceStatus())) {
            signals.add("evidenceStatus=" + evidence.evidenceStatus());
        }
        if (isHealthFactor(evidence.factorName) && isDegradedHealthValue(evidence.observedValueOrStatus())) {
            signals.add("health evidence value is degraded");
        }
        evidence.statusReasons.stream()
                .filter(DecisionExplorerFactorDiagnosticsService::isDegradedSignal)
                .forEach(signals::add);
        return distinctSorted(signals);
    }

    private static int missingSignalCount(FactorEvidence evidence, List<String> unknownSignals) {
        int count = 0;
        if ("UNKNOWN".equals(evidence.observedValueOrStatus())) {
            count++;
        }
        if ("UNKNOWN".equals(evidence.influenceCategory()) || "UNKNOWN_INFLUENCE".equals(evidence.influenceCategory())) {
            count++;
        }
        if ("UNKNOWN".equals(evidence.explanation()) && "UNKNOWN".equals(evidence.interpretation())) {
            count++;
        }
        if (evidence.sourceReferenceIds.isEmpty()) {
            count++;
        }
        return Math.max(count, unknownSignals.isEmpty() ? 0 : 1);
    }

    private static List<String> reasonCodes(
            FactorEvidence evidence,
            String contribution,
            List<String> supportingSignals,
            List<String> warningSignals,
            List<String> unknownSignals,
            List<String> degradedSignals) {
        List<String> reasons = new ArrayList<>();
        reasons.add("FACTOR_CONTRIBUTION_" + contribution);
        reasons.add("FACTOR_STATUS_" + evidence.factorStatus());
        reasons.add("EVIDENCE_STATUS_" + evidence.evidenceStatus());
        reasons.add("INFLUENCE_" + evidence.influenceCategory());
        reasons.add("SUPPORTING_SIGNAL_COUNT_" + supportingSignals.size());
        reasons.add("WARNING_SIGNAL_COUNT_" + warningSignals.size());
        reasons.add("UNKNOWN_SIGNAL_COUNT_" + unknownSignals.size());
        reasons.add("DEGRADED_SIGNAL_COUNT_" + degradedSignals.size());
        reasons.addAll(evidence.statusReasons);
        return distinctSorted(reasons);
    }

    private static String summaryText(FactorEvidence evidence, String contribution) {
        return "Factor " + evidence.factorName + " for candidate " + evidence.candidateId
                + " is " + contribution + " with " + evidence.factorStatus()
                + " factor status, " + evidence.evidenceStatus() + " evidence status, and "
                + evidence.influenceCategory() + " influence.";
    }

    private static boolean isWeakReason(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return normalized.contains("PARTIAL")
                || normalized.contains("WARNING")
                || normalized.contains("WEAKENS")
                || normalized.contains("MISSING");
    }

    private static boolean isDegradedSignal(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value).toLowerCase(Locale.ROOT);
        return normalized.contains("degraded")
                || normalized.contains("failed")
                || normalized.contains("unavailable")
                || normalized.contains("unhealthy")
                || normalized.contains("health=false")
                || normalized.contains("healthstate=false");
    }

    private static boolean isDegradedEvidenceStatus(String evidenceStatus) {
        return List.of("DEGRADED", "FAILED", "UNAVAILABLE")
                .contains(DecisionExplorerDtoSupport.valueOrUnknown(evidenceStatus));
    }

    private static boolean isHealthFactor(String factorName) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(factorName).toLowerCase(Locale.ROOT);
        return normalized.equals("healthstate")
                || normalized.equals("health")
                || normalized.equals("healthy")
                || normalized.contains("healthstate");
    }

    private static boolean isDegradedHealthValue(String observedValueOrStatus) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(observedValueOrStatus)
                .trim()
                .toLowerCase(Locale.ROOT);
        return List.of("false", "unhealthy", "degraded", "failed", "down").contains(normalized)
                || normalized.endsWith("=false")
                || normalized.endsWith("=unhealthy")
                || normalized.endsWith("=degraded");
    }

    private static FactorEvidence evidenceFor(
            Map<String, FactorEvidence> evidenceByKey,
            String candidateId,
            String factorName) {
        String normalizedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        String normalizedFactorName = DecisionExplorerDtoSupport.valueOrUnknown(factorName);
        return evidenceByKey.computeIfAbsent(
                normalizedCandidateId + "\u0000" + normalizedFactorName,
                ignored -> new FactorEvidence(normalizedCandidateId, normalizedFactorName));
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

    private static final class FactorEvidence {
        private final String candidateId;
        private final String factorName;
        private int displayOrder;
        private String factorStatus;
        private String evidenceStatus;
        private String observedValueOrStatus;
        private String influenceCategory;
        private String explanation;
        private String interpretation;
        private final List<String> statusReasons = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> unknowns = new ArrayList<>();
        private final List<String> sourceReferenceIds = new ArrayList<>();

        private FactorEvidence(String candidateId, String factorName) {
            this.candidateId = candidateId;
            this.factorName = factorName;
        }

        private int displayOrder() {
            return Math.max(0, displayOrder);
        }

        private int sortOrder() {
            return displayOrder > 0 ? displayOrder : Integer.MAX_VALUE;
        }

        private String factorStatus() {
            return DecisionExplorerDtoSupport.valueOrUnknown(factorStatus);
        }

        private String evidenceStatus() {
            return DecisionExplorerDtoSupport.valueOrUnknown(evidenceStatus);
        }

        private String observedValueOrStatus() {
            return DecisionExplorerDtoSupport.valueOrUnknown(observedValueOrStatus);
        }

        private String influenceCategory() {
            return DecisionExplorerDtoSupport.valueOrUnknown(influenceCategory);
        }

        private String explanation() {
            return DecisionExplorerDtoSupport.valueOrUnknown(explanation);
        }

        private String interpretation() {
            return DecisionExplorerDtoSupport.valueOrUnknown(interpretation);
        }

        private void displayOrder(int value) {
            if (value > 0 && (displayOrder <= 0 || value < displayOrder)) {
                displayOrder = value;
            }
        }

        private void factorStatus(String value) {
            if (factorStatus == null || factorStatus.isBlank() || "UNKNOWN".equals(factorStatus)) {
                factorStatus = value;
            }
        }

        private void evidenceStatus(String value) {
            if (evidenceStatus == null || evidenceStatus.isBlank() || "UNKNOWN".equals(evidenceStatus)) {
                evidenceStatus = value;
            }
        }

        private void observedValueOrStatus(String value) {
            if (observedValueOrStatus == null || observedValueOrStatus.isBlank()
                    || "UNKNOWN".equals(observedValueOrStatus)) {
                observedValueOrStatus = value;
            }
        }

        private void influenceCategory(String value) {
            if (influenceCategory == null || influenceCategory.isBlank() || "UNKNOWN".equals(influenceCategory)) {
                influenceCategory = value;
            }
        }

        private void explanation(String value) {
            if (explanation == null || explanation.isBlank() || "UNKNOWN".equals(explanation)) {
                explanation = value;
            }
        }

        private void interpretation(String value) {
            if (interpretation == null || interpretation.isBlank() || "UNKNOWN".equals(interpretation)) {
                interpretation = value;
            }
        }
    }
}
