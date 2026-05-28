package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DecisionExplorerRoutingDiagnosticsService {
    private final DecisionExplorerCandidateDiagnosticsService candidateDiagnosticsService =
            new DecisionExplorerCandidateDiagnosticsService();

    public DecisionExplorerRoutingDiagnosticsV1 buildDiagnostics(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            List<CandidateReadoutV1> candidateSet,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            List<DecisionFactorDrilldownV1> factorDrilldowns,
            List<String> payloadWarnings,
            List<String> payloadUnknowns,
            String boundaryNote) {
        if (confidenceSummary == null) {
            return DecisionExplorerRoutingDiagnosticsV1.unknown(boundaryNote);
        }

        List<DecisionExplorerEvidenceDiagnosticV1> diagnostics = new ArrayList<>();
        diagnostics.add(decisionStatusDiagnostic(confidenceSummary, boundaryNote));
        diagnostics.add(selectedCandidateDiagnostic(confidenceSummary, boundaryNote));
        diagnostics.add(candidateComparisonDiagnostic(confidenceSummary, candidateComparisons, boundaryNote));
        diagnostics.add(candidateConfidenceDiagnostic(confidenceSummary, boundaryNote));
        diagnostics.add(factorEvidenceDiagnostic(confidenceSummary, factorDrilldowns, boundaryNote));
        diagnostics.add(factorStatusDiagnostic(confidenceSummary, boundaryNote));
        diagnostics.add(sourceReferenceDiagnostic(confidenceSummary, boundaryNote));
        diagnostics.add(statusWarningDiagnostic(confidenceSummary, payloadWarnings, boundaryNote));
        diagnostics.add(statusUnknownDiagnostic(confidenceSummary, payloadUnknowns, boundaryNote));

        List<DecisionExplorerEvidenceDiagnosticV1> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator
                        .comparing(DecisionExplorerEvidenceDiagnosticV1::category)
                        .thenComparing(DecisionExplorerEvidenceDiagnosticV1::diagnosticId))
                .toList();
        List<String> warnings = distinctSorted(concat(confidenceSummary.warnings(), payloadWarnings));
        List<String> unknowns = distinctSorted(concat(confidenceSummary.unknowns(), payloadUnknowns));
        List<String> diagnosticReasons = diagnosticReasons(confidenceSummary, sortedDiagnostics);
        List<DecisionExplorerCandidateDiagnosticV1> candidateDiagnostics =
                candidateDiagnosticsService.buildCandidateDiagnostics(
                        confidenceSummary, candidateSet, candidateComparisons, boundaryNote);

        return new DecisionExplorerRoutingDiagnosticsV1(
                true,
                true,
                DecisionExplorerRoutingDiagnosticsV1.DIAGNOSTICS_OBJECT,
                DecisionExplorerRoutingDiagnosticsV1.CONTRACT_VERSION,
                confidenceSummary.status(),
                confidenceSummary.evidenceQuality(),
                confidenceSummary.selectedCandidateId(),
                sortedDiagnostics.size(),
                countStatus(sortedDiagnostics, DecisionExplorerEvidenceDiagnosticV1.STATUS_PRESENT),
                countStatus(sortedDiagnostics, DecisionExplorerEvidenceDiagnosticV1.STATUS_PARTIAL),
                countStatus(sortedDiagnostics, DecisionExplorerEvidenceDiagnosticV1.STATUS_MISSING),
                countStatus(sortedDiagnostics, DecisionExplorerEvidenceDiagnosticV1.STATUS_DEGRADED),
                countStatus(sortedDiagnostics, DecisionExplorerEvidenceDiagnosticV1.STATUS_UNKNOWN),
                sortedDiagnostics,
                candidateDiagnosticsService.selectedCandidateDiagnostic(candidateDiagnostics, boundaryNote),
                candidateDiagnosticsService.alternativeCandidateDiagnostics(candidateDiagnostics),
                candidateDiagnostics,
                diagnosticReasons,
                warnings,
                unknowns,
                confidenceSummary.sourceReferenceIds(),
                boundaryNote);
    }

    private static DecisionExplorerEvidenceDiagnosticV1 decisionStatusDiagnostic(
            DecisionExplorerConfidenceSummaryV1 summary,
            String boundaryNote) {
        List<String> reasons = new ArrayList<>();
        reasons.add("CONFIDENCE_STATUS_" + summary.status());
        reasons.addAll(summary.statusReasons());
        String status = switch (summary.status()) {
            case DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED ->
                    DecisionExplorerEvidenceDiagnosticV1.STATUS_DEGRADED;
            case DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN ->
                    DecisionExplorerEvidenceDiagnosticV1.STATUS_UNKNOWN;
            case DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL ->
                    DecisionExplorerEvidenceDiagnosticV1.STATUS_PARTIAL;
            default -> DecisionExplorerEvidenceDiagnosticV1.STATUS_PRESENT;
        };
        return diagnostic(
                "decision-status",
                "DECISION",
                status,
                1,
                "Decision status diagnostic is " + status + " for confidence status " + summary.status() + ".",
                reasons,
                summary.sourceReferenceIds(),
                boundaryNote);
    }

    private static DecisionExplorerEvidenceDiagnosticV1 selectedCandidateDiagnostic(
            DecisionExplorerConfidenceSummaryV1 summary,
            String boundaryNote) {
        List<String> reasons = new ArrayList<>();
        String selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(summary.selectedCandidateId());
        if ("UNKNOWN".equals(selectedCandidateId)) {
            reasons.add("SELECTED_CANDIDATE_UNKNOWN");
            return diagnostic(
                    "selected-candidate",
                    "CANDIDATE",
                    DecisionExplorerEvidenceDiagnosticV1.STATUS_MISSING,
                    0,
                    "Selected candidate evidence was not returned.",
                    reasons,
                    summary.sourceReferenceIds(),
                    boundaryNote);
        }

        DecisionExplorerCandidateConfidenceV1 selectedConfidence = selectedCandidateConfidence(summary);
        String status = DecisionExplorerEvidenceDiagnosticV1.STATUS_PRESENT;
        if (selectedConfidence == null) {
            reasons.add("SELECTED_CANDIDATE_CONFIDENCE_MISSING");
            status = DecisionExplorerEvidenceDiagnosticV1.STATUS_PARTIAL;
        } else {
            reasons.add("SELECTED_CANDIDATE_CONFIDENCE_" + selectedConfidence.confidenceStatus());
            status = diagnosticStatusForConfidence(selectedConfidence.confidenceStatus());
        }
        return diagnostic(
                "selected-candidate",
                "CANDIDATE",
                status,
                1,
                "Selected candidate " + selectedCandidateId + " diagnostic is " + status + ".",
                reasons,
                summary.sourceReferenceIds(),
                boundaryNote);
    }

    private static DecisionExplorerEvidenceDiagnosticV1 candidateComparisonDiagnostic(
            DecisionExplorerConfidenceSummaryV1 summary,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            String boundaryNote) {
        List<String> reasons = new ArrayList<>();
        if (summary.candidateComparisonCount() <= 0) {
            reasons.add("CANDIDATE_COMPARISONS_MISSING");
            return diagnostic(
                    "candidate-comparisons",
                    "CANDIDATE",
                    DecisionExplorerEvidenceDiagnosticV1.STATUS_MISSING,
                    0,
                    "Candidate comparison evidence was not returned.",
                    reasons,
                    summary.sourceReferenceIds(),
                    boundaryNote);
        }
        reasons.add("CANDIDATE_COMPARISON_COUNT_" + summary.candidateComparisonCount());
        reasons.addAll(matchingReasons(summary, "PARTIAL_CANDIDATE_COMPARISON_EVIDENCE", "SELECTED_SCORE_UNKNOWN"));
        String status = reasons.stream().anyMatch(reason -> reason.startsWith("PARTIAL_")
                || reason.equals("SELECTED_SCORE_UNKNOWN"))
                ? DecisionExplorerEvidenceDiagnosticV1.STATUS_PARTIAL
                : DecisionExplorerEvidenceDiagnosticV1.STATUS_PRESENT;
        int returnedRows = copyNonNull(candidateComparisons).size();
        return diagnostic(
                "candidate-comparisons",
                "CANDIDATE",
                status,
                returnedRows,
                "Candidate comparison diagnostic is " + status + " with "
                        + summary.candidateComparisonCount() + " comparison row(s).",
                reasons,
                summary.sourceReferenceIds(),
                boundaryNote);
    }

    private static DecisionExplorerEvidenceDiagnosticV1 candidateConfidenceDiagnostic(
            DecisionExplorerConfidenceSummaryV1 summary,
            String boundaryNote) {
        List<DecisionExplorerCandidateConfidenceV1> rows = summary.candidateConfidenceDetails();
        if (rows.isEmpty()) {
            return diagnostic(
                    "candidate-confidence",
                    "CANDIDATE",
                    DecisionExplorerEvidenceDiagnosticV1.STATUS_MISSING,
                    0,
                    "Candidate confidence details were not returned.",
                    List.of("CANDIDATE_CONFIDENCE_DETAILS_MISSING"),
                    summary.sourceReferenceIds(),
                    boundaryNote);
        }
        String status = confidenceRollup(rows.stream()
                .map(DecisionExplorerCandidateConfidenceV1::confidenceStatus)
                .toList());
        List<String> reasons = rows.stream()
                .flatMap(row -> row.confidenceReasons().stream())
                .toList();
        return diagnostic(
                "candidate-confidence",
                "CANDIDATE",
                status,
                rows.size(),
                "Candidate confidence diagnostic is " + status + " across " + rows.size() + " row(s).",
                reasons.isEmpty() ? List.of("CANDIDATE_CONFIDENCE_DETAILS_RETURNED") : reasons,
                summary.sourceReferenceIds(),
                boundaryNote);
    }

    private static DecisionExplorerEvidenceDiagnosticV1 factorEvidenceDiagnostic(
            DecisionExplorerConfidenceSummaryV1 summary,
            List<DecisionFactorDrilldownV1> factorDrilldowns,
            String boundaryNote) {
        int factorEvidenceCount = summary.availableFactorCount()
                + summary.partialFactorCount()
                + summary.unknownFactorCount();
        if (factorEvidenceCount <= 0) {
            return diagnostic(
                    "factor-evidence",
                    "FACTOR",
                    DecisionExplorerEvidenceDiagnosticV1.STATUS_MISSING,
                    copyNonNull(factorDrilldowns).size(),
                    "Factor evidence was not returned.",
                    List.of("FACTOR_EVIDENCE_MISSING"),
                    summary.sourceReferenceIds(),
                    boundaryNote);
        }
        List<String> reasons = new ArrayList<>();
        reasons.add("AVAILABLE_FACTOR_COUNT_" + summary.availableFactorCount());
        reasons.add("PARTIAL_FACTOR_COUNT_" + summary.partialFactorCount());
        reasons.add("UNKNOWN_FACTOR_COUNT_" + summary.unknownFactorCount());
        reasons.addAll(matchingReasons(summary, "SELECTED_FACTOR_STATUS_DEGRADED", "PARTIAL_FACTOR_EVIDENCE",
                "UNKNOWN_FACTOR_EVIDENCE"));
        String status = factorEvidenceStatus(summary, reasons);
        return diagnostic(
                "factor-evidence",
                "FACTOR",
                status,
                factorEvidenceCount,
                "Factor evidence diagnostic is " + status + " with " + factorEvidenceCount
                        + " factor evidence row(s).",
                reasons,
                summary.sourceReferenceIds(),
                boundaryNote);
    }

    private static DecisionExplorerEvidenceDiagnosticV1 factorStatusDiagnostic(
            DecisionExplorerConfidenceSummaryV1 summary,
            String boundaryNote) {
        List<DecisionExplorerFactorStatusV1> rows = summary.factorStatusDetails();
        if (rows.isEmpty()) {
            return diagnostic(
                    "factor-status",
                    "FACTOR",
                    DecisionExplorerEvidenceDiagnosticV1.STATUS_MISSING,
                    0,
                    "Factor status details were not returned.",
                    List.of("FACTOR_STATUS_DETAILS_MISSING"),
                    summary.sourceReferenceIds(),
                    boundaryNote);
        }
        String status = confidenceRollup(rows.stream()
                .map(DecisionExplorerFactorStatusV1::factorStatus)
                .toList());
        List<String> reasons = rows.stream()
                .flatMap(row -> row.statusReasons().stream())
                .toList();
        return diagnostic(
                "factor-status",
                "FACTOR",
                status,
                rows.size(),
                "Factor status diagnostic is " + status + " across " + rows.size() + " row(s).",
                reasons.isEmpty() ? List.of("FACTOR_STATUS_DETAILS_RETURNED") : reasons,
                summary.sourceReferenceIds(),
                boundaryNote);
    }

    private static DecisionExplorerEvidenceDiagnosticV1 sourceReferenceDiagnostic(
            DecisionExplorerConfidenceSummaryV1 summary,
            String boundaryNote) {
        if (summary.sourceReferenceCount() <= 0) {
            return diagnostic(
                    "source-references",
                    "SOURCE",
                    DecisionExplorerEvidenceDiagnosticV1.STATUS_MISSING,
                    0,
                    "Source reference evidence was not returned.",
                    List.of("SOURCE_REFERENCES_MISSING"),
                    List.of(),
                    boundaryNote);
        }
        return diagnostic(
                "source-references",
                "SOURCE",
                DecisionExplorerEvidenceDiagnosticV1.STATUS_PRESENT,
                summary.sourceReferenceCount(),
                "Source reference evidence is present.",
                List.of("SOURCE_REFERENCES_RETURNED"),
                summary.sourceReferenceIds(),
                boundaryNote);
    }

    private static DecisionExplorerEvidenceDiagnosticV1 statusWarningDiagnostic(
            DecisionExplorerConfidenceSummaryV1 summary,
            List<String> payloadWarnings,
            String boundaryNote) {
        List<String> warnings = distinctSorted(concat(summary.warnings(), payloadWarnings));
        if (warnings.isEmpty()) {
            return diagnostic(
                    "status-warnings",
                    "CAUTION",
                    DecisionExplorerEvidenceDiagnosticV1.STATUS_PRESENT,
                    0,
                    "No diagnostic warnings were returned.",
                    List.of("NO_DIAGNOSTIC_WARNINGS"),
                    summary.sourceReferenceIds(),
                    boundaryNote);
        }
        return diagnostic(
                "status-warnings",
                "CAUTION",
                DecisionExplorerEvidenceDiagnosticV1.STATUS_PARTIAL,
                warnings.size(),
                "Diagnostic warnings are present.",
                warnings,
                summary.sourceReferenceIds(),
                boundaryNote);
    }

    private static DecisionExplorerEvidenceDiagnosticV1 statusUnknownDiagnostic(
            DecisionExplorerConfidenceSummaryV1 summary,
            List<String> payloadUnknowns,
            String boundaryNote) {
        List<String> unknowns = distinctSorted(concat(summary.unknowns(), payloadUnknowns));
        if (unknowns.isEmpty()) {
            return diagnostic(
                    "status-unknowns",
                    "CAUTION",
                    DecisionExplorerEvidenceDiagnosticV1.STATUS_PRESENT,
                    0,
                    "No diagnostic unknowns were returned.",
                    List.of("NO_DIAGNOSTIC_UNKNOWNS"),
                    summary.sourceReferenceIds(),
                    boundaryNote);
        }
        return diagnostic(
                "status-unknowns",
                "CAUTION",
                DecisionExplorerEvidenceDiagnosticV1.STATUS_UNKNOWN,
                unknowns.size(),
                "Diagnostic unknowns are present.",
                unknowns,
                summary.sourceReferenceIds(),
                boundaryNote);
    }

    private static DecisionExplorerEvidenceDiagnosticV1 diagnostic(
            String diagnosticId,
            String category,
            String status,
            int evidenceCount,
            String summaryText,
            List<String> reasonCodes,
            List<String> sourceReferenceIds,
            String boundaryNote) {
        return new DecisionExplorerEvidenceDiagnosticV1(
                diagnosticId,
                category,
                status,
                DecisionExplorerEvidenceDiagnosticV1.severityFor(status),
                evidenceCount,
                summaryText,
                distinctSorted(reasonCodes),
                distinctSorted(sourceReferenceIds),
                boundaryNote);
    }

    private static String factorEvidenceStatus(
            DecisionExplorerConfidenceSummaryV1 summary,
            List<String> reasons) {
        if (reasons.contains("SELECTED_FACTOR_STATUS_DEGRADED")
                || factorStatusHas(summary, DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED)) {
            return DecisionExplorerEvidenceDiagnosticV1.STATUS_DEGRADED;
        }
        if (summary.partialFactorCount() > 0) {
            return DecisionExplorerEvidenceDiagnosticV1.STATUS_PARTIAL;
        }
        if (summary.unknownFactorCount() > 0) {
            return DecisionExplorerEvidenceDiagnosticV1.STATUS_UNKNOWN;
        }
        return DecisionExplorerEvidenceDiagnosticV1.STATUS_PRESENT;
    }

    private static String confidenceRollup(List<String> statuses) {
        if (statuses.isEmpty()) {
            return DecisionExplorerEvidenceDiagnosticV1.STATUS_UNKNOWN;
        }
        if (statuses.stream().anyMatch(DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED::equals)) {
            return DecisionExplorerEvidenceDiagnosticV1.STATUS_DEGRADED;
        }
        if (statuses.stream().anyMatch(DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL::equals)) {
            return DecisionExplorerEvidenceDiagnosticV1.STATUS_PARTIAL;
        }
        if (statuses.stream().anyMatch(DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN::equals)) {
            return DecisionExplorerEvidenceDiagnosticV1.STATUS_UNKNOWN;
        }
        return DecisionExplorerEvidenceDiagnosticV1.STATUS_PRESENT;
    }

    private static String diagnosticStatusForConfidence(String confidenceStatus) {
        return switch (DecisionExplorerDtoSupport.valueOrUnknown(confidenceStatus)) {
            case DecisionExplorerConfidenceSummaryV1.STATUS_STRONG ->
                    DecisionExplorerEvidenceDiagnosticV1.STATUS_PRESENT;
            case DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL ->
                    DecisionExplorerEvidenceDiagnosticV1.STATUS_PARTIAL;
            case DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED ->
                    DecisionExplorerEvidenceDiagnosticV1.STATUS_DEGRADED;
            default -> DecisionExplorerEvidenceDiagnosticV1.STATUS_UNKNOWN;
        };
    }

    private static DecisionExplorerCandidateConfidenceV1 selectedCandidateConfidence(
            DecisionExplorerConfidenceSummaryV1 summary) {
        String selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(summary.selectedCandidateId());
        return summary.candidateConfidenceDetails().stream()
                .filter(DecisionExplorerCandidateConfidenceV1::selected)
                .filter(row -> selectedCandidateId.equals(DecisionExplorerDtoSupport.valueOrUnknown(row.candidateId())))
                .findFirst()
                .orElse(null);
    }

    private static boolean factorStatusHas(DecisionExplorerConfidenceSummaryV1 summary, String status) {
        return summary.factorStatusDetails().stream()
                .map(DecisionExplorerFactorStatusV1::factorStatus)
                .anyMatch(status::equals);
    }

    private static List<String> matchingReasons(DecisionExplorerConfidenceSummaryV1 summary, String... reasons) {
        List<String> expected = List.of(reasons);
        return summary.statusReasons().stream()
                .filter(expected::contains)
                .toList();
    }

    private static List<String> diagnosticReasons(
            DecisionExplorerConfidenceSummaryV1 summary,
            List<DecisionExplorerEvidenceDiagnosticV1> diagnostics) {
        List<String> reasons = new ArrayList<>(summary.statusReasons());
        diagnostics.stream()
                .flatMap(diagnostic -> diagnostic.reasonCodes().stream())
                .forEach(reasons::add);
        return distinctSorted(reasons);
    }

    private static int countStatus(List<DecisionExplorerEvidenceDiagnosticV1> diagnostics, String status) {
        return (int) diagnostics.stream()
                .filter(diagnostic -> status.equals(diagnostic.status()))
                .count();
    }

    private static <T> List<T> copyNonNull(List<T> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }

    private static List<String> concat(List<String> first, List<String> second) {
        List<String> values = new ArrayList<>();
        if (first != null) {
            values.addAll(first);
        }
        if (second != null) {
            values.addAll(second);
        }
        return values;
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
