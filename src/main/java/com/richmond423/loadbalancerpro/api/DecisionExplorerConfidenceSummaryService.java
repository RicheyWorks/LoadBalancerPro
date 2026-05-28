package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class DecisionExplorerConfidenceSummaryService {

    public DecisionExplorerConfidenceSummaryV1 buildSummary(
            DecisionReadoutV1 decisionReadout,
            CandidateReadoutV1 selectedCandidate,
            List<CandidateReadoutV1> candidateSet,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            List<DecisionFactorDrilldownV1> factorDrilldowns,
            List<String> payloadWarnings,
            List<String> payloadUnknowns,
            String boundaryNote) {
        List<CandidateReadoutV1> candidates = copyNonNull(candidateSet);
        List<DecisionExplorerCandidateComparisonRowV1> comparisons = copyNonNull(candidateComparisons);
        List<DecisionFactorDrilldownV1> factors = copyNonNull(factorDrilldowns);
        List<String> statusWarnings = statusInfluencingWarnings(payloadWarnings, comparisons, factors);
        List<String> statusUnknowns = statusInfluencingUnknowns(payloadUnknowns, comparisons, factors);
        List<String> sourceReferenceIds = sourceReferenceIds(decisionReadout, selectedCandidate, comparisons, factors);

        int availableFactorCount = countFactors(factors, "AVAILABLE");
        int partialFactorCount = countFactors(factors, "PARTIAL");
        int unknownFactorCount = countFactors(factors, "UNKNOWN");
        boolean selectedCandidateConfirmed = selectedCandidateConfirmed(selectedCandidate);
        String selectedCandidateId = selectedCandidateId(decisionReadout, selectedCandidate);
        String decisionStatus = decisionStatus(decisionReadout);
        boolean routingEvidenceUnavailable = routingEvidenceUnavailable(
                decisionReadout,
                candidates,
                comparisons,
                factors,
                sourceReferenceIds);

        List<String> reasons = new ArrayList<>();
        String status = classifyStatus(
                decisionStatus,
                routingEvidenceUnavailable,
                selectedCandidateConfirmed,
                comparisons,
                availableFactorCount,
                partialFactorCount,
                unknownFactorCount,
                statusWarnings,
                statusUnknowns,
                reasons);

        List<String> evidenceSignals = evidenceSignals(
                decisionStatus,
                selectedCandidateId,
                candidates.size(),
                comparisons.size(),
                availableFactorCount,
                partialFactorCount,
                unknownFactorCount,
                statusWarnings.size(),
                statusUnknowns.size(),
                sourceReferenceIds.size());

        return new DecisionExplorerConfidenceSummaryV1(
                true,
                true,
                DecisionExplorerConfidenceSummaryV1.SUMMARY_OBJECT,
                DecisionExplorerConfidenceSummaryV1.CONTRACT_VERSION,
                status,
                DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(status),
                selectedCandidateId,
                candidates.size(),
                comparisons.size(),
                availableFactorCount,
                partialFactorCount,
                unknownFactorCount,
                statusWarnings.size(),
                statusUnknowns.size(),
                sourceReferenceIds.size(),
                evidenceSignals,
                distinctSorted(reasons),
                statusWarnings,
                statusUnknowns,
                sourceReferenceIds,
                boundaryNote);
    }

    private static String classifyStatus(
            String decisionStatus,
            boolean routingEvidenceUnavailable,
            boolean selectedCandidateConfirmed,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            int availableFactorCount,
            int partialFactorCount,
            int unknownFactorCount,
            List<String> warnings,
            List<String> unknowns,
            List<String> reasons) {
        if (routingEvidenceUnavailable) {
            reasons.add("NO_ROUTING_EVIDENCE_RETURNED");
            return DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
        }
        if (isDegradedDecisionStatus(decisionStatus)) {
            reasons.add("DECISION_STATUS_" + decisionStatus);
            return DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED;
        }
        if ("PARTIAL".equals(decisionStatus)) {
            reasons.add("DECISION_STATUS_PARTIAL");
        }
        if (!selectedCandidateConfirmed) {
            reasons.add("SELECTED_CANDIDATE_UNAVAILABLE");
            return DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED;
        }
        if (candidateComparisons.isEmpty()) {
            reasons.add("CANDIDATE_COMPARISONS_UNKNOWN");
            return DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL;
        }
        if (availableFactorCount == 0 && partialFactorCount == 0 && unknownFactorCount == 0) {
            reasons.add("FACTOR_EVIDENCE_UNKNOWN");
            return DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL;
        }
        if (hasPartialCandidateComparison(candidateComparisons)) {
            reasons.add("PARTIAL_CANDIDATE_COMPARISON_EVIDENCE");
        }
        if (hasSelectedScoreUnknown(candidateComparisons)) {
            reasons.add("SELECTED_SCORE_UNKNOWN");
        }
        if (partialFactorCount > 0) {
            reasons.add("PARTIAL_FACTOR_EVIDENCE");
        }
        if (unknownFactorCount > 0) {
            reasons.add("UNKNOWN_FACTOR_EVIDENCE");
        }
        if (!warnings.isEmpty()) {
            reasons.add("STATUS_WARNINGS_PRESENT");
        }
        if (!unknowns.isEmpty()) {
            reasons.add("STATUS_UNKNOWNS_PRESENT");
        }
        if (!reasons.isEmpty()) {
            return DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL;
        }
        reasons.add("SELECTED_CANDIDATE_CONFIRMED");
        reasons.add("CANDIDATE_COMPARISONS_AVAILABLE");
        reasons.add("FACTOR_EVIDENCE_AVAILABLE");
        reasons.add("NO_STATUS_WARNINGS");
        return DecisionExplorerConfidenceSummaryV1.STATUS_STRONG;
    }

    private static boolean routingEvidenceUnavailable(
            DecisionReadoutV1 decisionReadout,
            List<CandidateReadoutV1> candidates,
            List<DecisionExplorerCandidateComparisonRowV1> comparisons,
            List<DecisionFactorDrilldownV1> factors,
            List<String> sourceReferenceIds) {
        return decisionReadout == null
                || ("UNKNOWN".equals(decisionStatus(decisionReadout))
                && candidates.isEmpty()
                && comparisons.isEmpty()
                && factors.isEmpty()
                && sourceReferenceIds.isEmpty());
    }

    private static boolean selectedCandidateConfirmed(CandidateReadoutV1 selectedCandidate) {
        return selectedCandidate != null
                && selectedCandidate.selected()
                && !"UNKNOWN".equals(DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidate.candidateId()));
    }

    private static String selectedCandidateId(
            DecisionReadoutV1 decisionReadout,
            CandidateReadoutV1 selectedCandidate) {
        if (selectedCandidate != null) {
            String selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidate.candidateId());
            if (!"UNKNOWN".equals(selectedCandidateId)) {
                return selectedCandidateId;
            }
        }
        return decisionReadout == null
                ? "UNKNOWN"
                : DecisionExplorerDtoSupport.valueOrUnknown(decisionReadout.selectedCandidateId());
    }

    private static String decisionStatus(DecisionReadoutV1 decisionReadout) {
        return decisionReadout == null
                ? "UNKNOWN"
                : DecisionExplorerDtoSupport.valueOrUnknown(decisionReadout.status());
    }

    private static boolean isDegradedDecisionStatus(String decisionStatus) {
        return !List.of("SUCCESS", "AVAILABLE", "PARTIAL", "UNKNOWN").contains(decisionStatus);
    }

    private static int countFactors(List<DecisionFactorDrilldownV1> factors, String evidenceStatus) {
        return (int) factors.stream()
                .filter(factor -> evidenceStatus.equals(DecisionExplorerDtoSupport.valueOrUnknown(
                        factor.evidenceStatus())))
                .count();
    }

    private static boolean hasPartialCandidateComparison(
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons) {
        return candidateComparisons.stream()
                .map(DecisionExplorerCandidateComparisonRowV1::comparisonStatus)
                .map(DecisionExplorerDtoSupport::valueOrUnknown)
                .anyMatch("PARTIAL_EVIDENCE"::equals);
    }

    private static boolean hasSelectedScoreUnknown(
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons) {
        return candidateComparisons.stream()
                .map(DecisionExplorerCandidateComparisonRowV1::comparisonStatus)
                .map(DecisionExplorerDtoSupport::valueOrUnknown)
                .anyMatch("SELECTED_SCORE_UNKNOWN"::equals);
    }

    private static List<String> statusInfluencingWarnings(
            List<String> payloadWarnings,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            List<DecisionFactorDrilldownV1> factorDrilldowns) {
        List<String> warnings = new ArrayList<>();
        warnings.addAll(nonBoundaryLimitations(payloadWarnings));
        candidateComparisons.stream()
                .flatMap(row -> row.warnings().stream())
                .forEach(warnings::add);
        factorDrilldowns.stream()
                .flatMap(factor -> factor.warnings().stream())
                .forEach(warnings::add);
        return distinctSorted(warnings);
    }

    private static List<String> statusInfluencingUnknowns(
            List<String> payloadUnknowns,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            List<DecisionFactorDrilldownV1> factorDrilldowns) {
        List<String> unknowns = new ArrayList<>();
        unknowns.addAll(nonBoundaryLimitations(payloadUnknowns));
        candidateComparisons.stream()
                .flatMap(row -> row.unknowns().stream())
                .filter(unknown -> !isBoundaryLimitation(unknown))
                .forEach(unknowns::add);
        factorDrilldowns.stream()
                .flatMap(factor -> factor.unknowns().stream())
                .filter(unknown -> !isBoundaryLimitation(unknown))
                .forEach(unknowns::add);
        return distinctSorted(unknowns);
    }

    private static List<String> nonBoundaryLimitations(List<String> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .filter(value -> !isBoundaryLimitation(value))
                        .toList();
    }

    private static boolean isBoundaryLimitation(String value) {
        if (value == null) {
            return true;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("read-only")
                || normalized.contains("simulation-only")
                || normalized.contains("hidden routing internals")
                || normalized.contains("exact production scoring")
                || normalized.contains("live-cloud")
                || normalized.contains("real-tenant")
                || normalized.contains("benchmark/load/stress")
                || normalized.contains("replay/export/storage");
    }

    private static List<String> sourceReferenceIds(
            DecisionReadoutV1 decisionReadout,
            CandidateReadoutV1 selectedCandidate,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            List<DecisionFactorDrilldownV1> factorDrilldowns) {
        List<String> references = new ArrayList<>();
        if (decisionReadout != null) {
            references.addAll(decisionReadout.sourceReferenceIds());
        }
        if (selectedCandidate != null) {
            references.addAll(selectedCandidate.evidenceReferenceIds());
        }
        candidateComparisons.stream()
                .flatMap(row -> row.evidenceReferenceIds().stream())
                .forEach(references::add);
        factorDrilldowns.stream()
                .flatMap(factor -> factor.sourceReferenceIds().stream())
                .forEach(references::add);
        return distinctSorted(references);
    }

    private static List<String> evidenceSignals(
            String decisionStatus,
            String selectedCandidateId,
            int candidateCount,
            int candidateComparisonCount,
            int availableFactorCount,
            int partialFactorCount,
            int unknownFactorCount,
            int warningCount,
            int unknownCount,
            int sourceReferenceCount) {
        return List.of(
                "availableFactorCount=" + availableFactorCount,
                "candidateComparisonCount=" + candidateComparisonCount,
                "candidateCount=" + candidateCount,
                "decisionStatus=" + decisionStatus,
                "partialFactorCount=" + partialFactorCount,
                "selectedCandidateId=" + selectedCandidateId,
                "sourceReferenceCount=" + sourceReferenceCount,
                "statusWarningCount=" + warningCount,
                "statusUnknownCount=" + unknownCount,
                "unknownFactorCount=" + unknownFactorCount);
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
