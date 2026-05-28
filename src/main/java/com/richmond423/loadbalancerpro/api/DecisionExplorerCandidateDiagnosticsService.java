package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DecisionExplorerCandidateDiagnosticsService {

    public List<DecisionExplorerCandidateDiagnosticV1> buildCandidateDiagnostics(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            List<CandidateReadoutV1> candidateSet,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            String boundaryNote) {
        if (confidenceSummary == null) {
            return List.of();
        }

        Map<String, CandidateEvidence> evidenceByCandidateId = new HashMap<>();
        for (CandidateReadoutV1 readout : copyNonNull(candidateSet)) {
            if (readout == null) {
                continue;
            }
            CandidateEvidence evidence = evidenceFor(evidenceByCandidateId, readout.candidateId());
            evidence.label(readout.candidateLabel());
            evidence.selected(readout.selected());
            evidence.finalScore(readout.finalScore());
            evidence.visibleSignals.addAll(readout.visibleSignals());
            evidence.unknownSignals.addAll(readout.unknownSignals());
            evidence.reasonCodes.addAll(readout.reasonCodes());
            evidence.policyGateIds.addAll(readout.policyGateIds());
            evidence.sourceReferenceIds.addAll(readout.evidenceReferenceIds());
            evidence.readoutStatus(readout.candidateStatus());
        }

        for (DecisionExplorerCandidateComparisonRowV1 comparison : copyNonNull(candidateComparisons)) {
            if (comparison == null) {
                continue;
            }
            CandidateEvidence evidence = evidenceFor(evidenceByCandidateId, comparison.candidateId());
            evidence.label(comparison.candidateLabel());
            evidence.selected(comparison.selected());
            evidence.displayOrder(comparison.displayOrder());
            evidence.finalScore(comparison.finalScore());
            evidence.scoreDeltaFromSelected(comparison.scoreDeltaFromSelected());
            evidence.comparisonStatus(comparison.comparisonStatus());
            evidence.visibleSignals.addAll(comparison.visibleSignals());
            evidence.unknownSignals.addAll(comparison.unknownSignals());
            evidence.reasonCodes.addAll(comparison.reasonCodes());
            evidence.policyGateIds.addAll(comparison.policyGateIds());
            evidence.sourceReferenceIds.addAll(comparison.evidenceReferenceIds());
            evidence.warnings.addAll(comparison.warnings());
            evidence.unknowns.addAll(comparison.unknowns());
        }

        for (DecisionExplorerCandidateConfidenceV1 confidence : confidenceSummary.candidateConfidenceDetails()) {
            if (confidence == null) {
                continue;
            }
            CandidateEvidence evidence = evidenceFor(evidenceByCandidateId, confidence.candidateId());
            evidence.label(confidence.candidateLabel());
            evidence.selected(confidence.selected());
            evidence.displayOrder(confidence.displayOrder());
            evidence.finalScore(confidence.finalScore());
            evidence.scoreDeltaFromSelected(confidence.scoreDeltaFromSelected());
            evidence.comparisonStatus(confidence.comparisonStatus());
            evidence.confidenceStatus(confidence.confidenceStatus());
            evidence.healthEvidenceState(confidence.healthEvidenceState());
            evidence.confidenceReasons.addAll(confidence.confidenceReasons());
            evidence.warnings.addAll(confidence.warnings());
            evidence.unknowns.addAll(confidence.unknowns());
            evidence.sourceReferenceIds.addAll(confidence.sourceReferenceIds());
        }

        String selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(confidenceSummary.selectedCandidateId());
        evidenceByCandidateId.values().forEach(evidence -> {
            if (selectedCandidateId.equals(evidence.candidateId)) {
                evidence.selected(true);
            }
        });

        return evidenceByCandidateId.values().stream()
                .filter(evidence -> !"UNKNOWN".equals(evidence.candidateId))
                .sorted(Comparator
                        .comparing((CandidateEvidence evidence) -> evidence.selected ? 0 : 1)
                        .thenComparingInt(CandidateEvidence::sortOrder)
                        .thenComparing(evidence -> evidence.candidateId))
                .map(evidence -> candidateDiagnostic(evidence, boundaryNote))
                .toList();
    }

    public DecisionExplorerCandidateDiagnosticV1 selectedCandidateDiagnostic(
            List<DecisionExplorerCandidateDiagnosticV1> candidateDiagnostics,
            String boundaryNote) {
        return copyNonNull(candidateDiagnostics).stream()
                .filter(DecisionExplorerCandidateDiagnosticV1::selected)
                .findFirst()
                .orElseGet(() -> DecisionExplorerCandidateDiagnosticV1.unknownSelected(boundaryNote));
    }

    public List<DecisionExplorerCandidateDiagnosticV1> alternativeCandidateDiagnostics(
            List<DecisionExplorerCandidateDiagnosticV1> candidateDiagnostics) {
        return copyNonNull(candidateDiagnostics).stream()
                .filter(diagnostic -> !diagnostic.selected())
                .sorted(Comparator
                        .comparingInt((DecisionExplorerCandidateDiagnosticV1 diagnostic) ->
                                diagnostic.displayOrder() > 0 ? diagnostic.displayOrder() : Integer.MAX_VALUE)
                        .thenComparing(DecisionExplorerCandidateDiagnosticV1::candidateId))
                .toList();
    }

    private static DecisionExplorerCandidateDiagnosticV1 candidateDiagnostic(
            CandidateEvidence evidence,
            String boundaryNote) {
        List<String> degradedSignals = degradedSignals(evidence);
        List<String> unknownSignals = unknownSignals(evidence);
        List<String> weakSignals = weakSignals(evidence, unknownSignals);
        String diagnosticStatus = diagnosticStatus(evidence, degradedSignals, weakSignals, unknownSignals);
        String scoreInterpretation = scoreInterpretation(evidence);
        int warningCount = distinctSorted(evidence.warnings).size();
        int unknownSignalCount = unknownSignals.size();
        int degradedSignalCount = degradedSignals.size();
        String riskLevel = DecisionExplorerCandidateDiagnosticV1.riskFor(
                diagnosticStatus,
                warningCount,
                unknownSignalCount,
                degradedSignalCount,
                scoreInterpretation);
        List<String> reasonCodes = reasonCodes(
                evidence,
                diagnosticStatus,
                riskLevel,
                scoreInterpretation,
                weakSignals,
                unknownSignals,
                degradedSignals);
        List<String> strengthSignals = strengthSignals(evidence, degradedSignals);
        return new DecisionExplorerCandidateDiagnosticV1(
                evidence.candidateId,
                evidence.candidateLabel(),
                evidence.selected,
                evidence.displayOrder(),
                evidence.selected
                        ? DecisionExplorerCandidateDiagnosticV1.ROLE_SELECTED
                        : DecisionExplorerCandidateDiagnosticV1.ROLE_ALTERNATIVE,
                diagnosticStatus,
                riskLevel,
                evidence.healthEvidenceState(),
                evidence.comparisonStatus(),
                evidence.finalScore(),
                evidence.scoreDeltaFromSelected(),
                distinctSorted(evidence.visibleSignals).size(),
                warningCount,
                unknownSignalCount,
                degradedSignalCount,
                scoreInterpretation,
                summaryText(evidence, diagnosticStatus, riskLevel, scoreInterpretation),
                strengthSignals,
                weakSignals,
                degradedSignals,
                unknownSignals,
                reasonCodes,
                distinctSorted(evidence.sourceReferenceIds),
                boundaryNote);
    }

    private static String diagnosticStatus(
            CandidateEvidence evidence,
            List<String> degradedSignals,
            List<String> weakSignals,
            List<String> unknownSignals) {
        if (!degradedSignals.isEmpty()) {
            return DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED;
        }
        if (!"UNKNOWN".equals(evidence.confidenceStatus())) {
            return evidence.confidenceStatus();
        }
        if (!weakSignals.isEmpty()) {
            return DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL;
        }
        if (!unknownSignals.isEmpty()) {
            return DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
        }
        if (evidence.hasAnyEvidence()) {
            return DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL;
        }
        return DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
    }

    private static List<String> strengthSignals(CandidateEvidence evidence, List<String> degradedSignals) {
        Set<String> degraded = new LinkedHashSet<>(degradedSignals);
        List<String> signals = new ArrayList<>();
        evidence.visibleSignals.stream()
                .filter(signal -> !isDegradedSignal(signal))
                .forEach(signals::add);
        evidence.confidenceReasons.stream()
                .filter(reason -> !isWeakReason(reason) && !isDegradedSignal(reason))
                .forEach(signals::add);
        evidence.reasonCodes.stream()
                .filter(reason -> !isWeakReason(reason) && !isDegradedSignal(reason))
                .forEach(signals::add);
        signals.removeIf(degraded::contains);
        return distinctSorted(signals);
    }

    private static List<String> weakSignals(CandidateEvidence evidence, List<String> unknownSignals) {
        List<String> signals = new ArrayList<>();
        signals.addAll(evidence.warnings);
        evidence.reasonCodes.stream()
                .filter(DecisionExplorerCandidateDiagnosticsService::isWeakReason)
                .forEach(signals::add);
        evidence.confidenceReasons.stream()
                .filter(DecisionExplorerCandidateDiagnosticsService::isWeakReason)
                .forEach(signals::add);
        String comparisonStatus = evidence.comparisonStatus();
        if ("PARTIAL_EVIDENCE".equals(comparisonStatus) || "SELECTED_SCORE_UNKNOWN".equals(comparisonStatus)) {
            signals.add("comparisonStatus=" + comparisonStatus);
        }
        if ("UNKNOWN".equals(evidence.confidenceStatus())) {
            signals.add("candidate confidence row was not returned");
        }
        if (evidence.finalScore() == null) {
            signals.add("candidate final score was not returned");
        }
        signals.addAll(unknownSignals.stream()
                .filter(signal -> signal.startsWith("score delta"))
                .toList());
        return distinctSorted(signals);
    }

    private static List<String> unknownSignals(CandidateEvidence evidence) {
        List<String> signals = new ArrayList<>();
        signals.addAll(evidence.unknownSignals);
        signals.addAll(evidence.unknowns);
        if (DecisionExplorerCandidateConfidenceV1.UNKNOWN.equals(evidence.healthEvidenceState())) {
            signals.add("health evidence state is unknown");
        }
        if (!evidence.selected && evidence.scoreDeltaFromSelected() == null) {
            signals.add("score delta from selected candidate was not returned");
        }
        if (evidence.finalScore() == null) {
            signals.add("candidate final score was not returned");
        }
        return distinctSorted(signals);
    }

    private static List<String> degradedSignals(CandidateEvidence evidence) {
        List<String> signals = new ArrayList<>();
        if (DecisionExplorerCandidateConfidenceV1.DEGRADED.equals(evidence.healthEvidenceState())) {
            signals.add("health evidence state is degraded");
        }
        evidence.visibleSignals.stream()
                .filter(DecisionExplorerCandidateDiagnosticsService::isDegradedSignal)
                .forEach(signals::add);
        evidence.reasonCodes.stream()
                .filter(DecisionExplorerCandidateDiagnosticsService::isDegradedSignal)
                .forEach(signals::add);
        evidence.confidenceReasons.stream()
                .filter(DecisionExplorerCandidateDiagnosticsService::isDegradedSignal)
                .forEach(signals::add);
        return distinctSorted(signals);
    }

    private static String scoreInterpretation(CandidateEvidence evidence) {
        if (evidence.selected) {
            return evidence.finalScore() == null
                    ? "SELECTED_SCORE_UNKNOWN"
                    : "SELECTED_BASELINE_SCORE_PRESENT";
        }
        Double scoreDelta = evidence.scoreDeltaFromSelected();
        if (scoreDelta == null) {
            return "ALTERNATIVE_DELTA_UNKNOWN";
        }
        if (scoreDelta < 0.0d) {
            return "ALTERNATIVE_BEATS_SELECTED";
        }
        if (scoreDelta == 0.0d) {
            return "ALTERNATIVE_TIED_SELECTED";
        }
        return "ALTERNATIVE_TRAILS_SELECTED";
    }

    private static List<String> reasonCodes(
            CandidateEvidence evidence,
            String diagnosticStatus,
            String riskLevel,
            String scoreInterpretation,
            List<String> weakSignals,
            List<String> unknownSignals,
            List<String> degradedSignals) {
        List<String> reasons = new ArrayList<>();
        reasons.add(evidence.selected ? "ROLE_SELECTED" : "ROLE_ALTERNATIVE");
        reasons.add("CANDIDATE_DIAGNOSTIC_STATUS_" + diagnosticStatus);
        reasons.add("CANDIDATE_RISK_" + riskLevel);
        reasons.add("HEALTH_EVIDENCE_" + evidence.healthEvidenceState());
        reasons.add(scoreInterpretation);
        reasons.add(evidence.finalScore() == null ? "FINAL_SCORE_MISSING" : "FINAL_SCORE_PRESENT");
        if (!evidence.selected) {
            reasons.add(evidence.scoreDeltaFromSelected() == null ? "SCORE_DELTA_MISSING" : "SCORE_DELTA_PRESENT");
        }
        reasons.add("VISIBLE_SIGNAL_COUNT_" + distinctSorted(evidence.visibleSignals).size());
        reasons.add("WARNING_COUNT_" + distinctSorted(evidence.warnings).size());
        reasons.add("UNKNOWN_SIGNAL_COUNT_" + unknownSignals.size());
        reasons.add("DEGRADED_SIGNAL_COUNT_" + degradedSignals.size());
        if (!weakSignals.isEmpty()) {
            reasons.add("WEAK_SIGNAL_COUNT_" + weakSignals.size());
        }
        reasons.addAll(evidence.confidenceReasons);
        reasons.addAll(evidence.reasonCodes);
        return distinctSorted(reasons);
    }

    private static String summaryText(
            CandidateEvidence evidence,
            String diagnosticStatus,
            String riskLevel,
            String scoreInterpretation) {
        String role = evidence.selected ? "Selected candidate " : "Alternative candidate ";
        String scoreText = evidence.finalScore() == null
                ? "without a returned final score"
                : "with final score " + evidence.finalScore();
        String deltaText = evidence.selected || evidence.scoreDeltaFromSelected() == null
                ? ""
                : " and score delta " + evidence.scoreDeltaFromSelected() + " from selected";
        return role + evidence.candidateId + " has " + diagnosticStatus + " diagnostics, "
                + riskLevel + " risk, " + evidence.healthEvidenceState() + " health evidence, "
                + scoreInterpretation + ", " + scoreText + deltaText + ".";
    }

    private static boolean isWeakReason(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return normalized.contains("PARTIAL")
                || normalized.contains("UNKNOWN")
                || normalized.contains("MISSING")
                || normalized.contains("SCORE_UNKNOWN");
    }

    private static boolean isDegradedSignal(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value).toLowerCase();
        return normalized.contains("degraded")
                || normalized.contains("unhealthy")
                || normalized.contains("healthstate=false")
                || normalized.contains("health=false");
    }

    private static CandidateEvidence evidenceFor(Map<String, CandidateEvidence> evidenceByCandidateId,
            String candidateId) {
        String normalizedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        return evidenceByCandidateId.computeIfAbsent(normalizedCandidateId, CandidateEvidence::new);
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

    private static final class CandidateEvidence {
        private final String candidateId;
        private String candidateLabel;
        private boolean selected;
        private int displayOrder;
        private String confidenceStatus;
        private String healthEvidenceState;
        private String comparisonStatus;
        private String readoutStatus;
        private Double finalScore;
        private Double scoreDeltaFromSelected;
        private final List<String> visibleSignals = new ArrayList<>();
        private final List<String> unknownSignals = new ArrayList<>();
        private final List<String> confidenceReasons = new ArrayList<>();
        private final List<String> reasonCodes = new ArrayList<>();
        private final List<String> policyGateIds = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> unknowns = new ArrayList<>();
        private final List<String> sourceReferenceIds = new ArrayList<>();

        private CandidateEvidence(String candidateId) {
            this.candidateId = candidateId;
        }

        private String candidateLabel() {
            return DecisionExplorerDtoSupport.valueOrDefault(candidateLabel, candidateId);
        }

        private int displayOrder() {
            return Math.max(0, displayOrder);
        }

        private int sortOrder() {
            return displayOrder > 0 ? displayOrder : Integer.MAX_VALUE;
        }

        private String confidenceStatus() {
            return DecisionExplorerDtoSupport.valueOrUnknown(confidenceStatus);
        }

        private String healthEvidenceState() {
            String normalized = DecisionExplorerDtoSupport.valueOrUnknown(healthEvidenceState);
            return switch (normalized) {
                case DecisionExplorerCandidateConfidenceV1.HEALTHY,
                        DecisionExplorerCandidateConfidenceV1.DEGRADED,
                        DecisionExplorerCandidateConfidenceV1.UNKNOWN -> normalized;
                default -> DecisionExplorerCandidateConfidenceV1.UNKNOWN;
            };
        }

        private String comparisonStatus() {
            String status = DecisionExplorerDtoSupport.valueOrUnknown(comparisonStatus);
            return "UNKNOWN".equals(status)
                    ? DecisionExplorerDtoSupport.valueOrUnknown(readoutStatus)
                    : status;
        }

        private Double finalScore() {
            return finiteOrNull(finalScore);
        }

        private Double scoreDeltaFromSelected() {
            return finiteOrNull(scoreDeltaFromSelected);
        }

        private boolean hasAnyEvidence() {
            return finalScore() != null
                    || !visibleSignals.isEmpty()
                    || !unknownSignals.isEmpty()
                    || !confidenceReasons.isEmpty()
                    || !reasonCodes.isEmpty()
                    || !warnings.isEmpty()
                    || !unknowns.isEmpty()
                    || !sourceReferenceIds.isEmpty();
        }

        private void label(String value) {
            if (candidateLabel == null || candidateLabel.isBlank() || "UNKNOWN".equals(candidateLabel)) {
                candidateLabel = value;
            }
        }

        private void selected(boolean value) {
            selected = selected || value;
        }

        private void displayOrder(int value) {
            if (value > 0 && (displayOrder <= 0 || value < displayOrder)) {
                displayOrder = value;
            }
        }

        private void finalScore(Double value) {
            if (finalScore == null && finiteOrNull(value) != null) {
                finalScore = value;
            }
        }

        private void scoreDeltaFromSelected(Double value) {
            if (scoreDeltaFromSelected == null && finiteOrNull(value) != null) {
                scoreDeltaFromSelected = value;
            }
        }

        private void comparisonStatus(String value) {
            if (comparisonStatus == null || comparisonStatus.isBlank() || "UNKNOWN".equals(comparisonStatus)) {
                comparisonStatus = value;
            }
        }

        private void confidenceStatus(String value) {
            if (confidenceStatus == null || confidenceStatus.isBlank() || "UNKNOWN".equals(confidenceStatus)) {
                confidenceStatus = value;
            }
        }

        private void healthEvidenceState(String value) {
            if (healthEvidenceState == null || healthEvidenceState.isBlank() || "UNKNOWN".equals(healthEvidenceState)) {
                healthEvidenceState = value;
            }
        }

        private void readoutStatus(String value) {
            if (readoutStatus == null || readoutStatus.isBlank() || "UNKNOWN".equals(readoutStatus)) {
                readoutStatus = value;
            }
        }

        private static Double finiteOrNull(Double value) {
            return value == null || !Double.isFinite(value) ? null : value;
        }
    }
}
