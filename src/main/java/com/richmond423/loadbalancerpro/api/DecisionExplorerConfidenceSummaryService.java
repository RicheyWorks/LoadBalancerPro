package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
        List<DecisionExplorerCandidateConfidenceV1> candidateConfidenceDetails =
                candidateConfidenceDetails(comparisons, factors);
        List<DecisionExplorerFactorStatusV1> factorStatusDetails = factorStatusDetails(factors);

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
                selectedCandidateId,
                routingEvidenceUnavailable,
                selectedCandidateConfirmed,
                comparisons,
                availableFactorCount,
                partialFactorCount,
                unknownFactorCount,
                candidateConfidenceDetails,
                factorStatusDetails,
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
                sourceReferenceIds.size(),
                candidateConfidenceDetails.size(),
                factorStatusDetails.size());

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
                candidateConfidenceDetails,
                factorStatusDetails,
                evidenceSignals,
                distinctSorted(reasons),
                statusWarnings,
                statusUnknowns,
                sourceReferenceIds,
                boundaryNote);
    }

    private static String classifyStatus(
            String decisionStatus,
            String selectedCandidateId,
            boolean routingEvidenceUnavailable,
            boolean selectedCandidateConfirmed,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            int availableFactorCount,
            int partialFactorCount,
            int unknownFactorCount,
            List<DecisionExplorerCandidateConfidenceV1> candidateConfidenceDetails,
            List<DecisionExplorerFactorStatusV1> factorStatusDetails,
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
        if (selectedCandidateConfidenceHasStatus(
                candidateConfidenceDetails,
                DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED)) {
            reasons.add("SELECTED_CANDIDATE_CONFIDENCE_DEGRADED");
            return DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED;
        }
        if (selectedFactorStatusHasStatus(
                factorStatusDetails,
                selectedCandidateId,
                DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED)) {
            reasons.add("SELECTED_FACTOR_STATUS_DEGRADED");
            return DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED;
        }
        if (selectedCandidateConfidenceHasStatus(
                candidateConfidenceDetails,
                DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL)) {
            reasons.add("SELECTED_CANDIDATE_CONFIDENCE_PARTIAL");
        }
        if (selectedFactorStatusHasStatus(
                factorStatusDetails,
                selectedCandidateId,
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN)) {
            reasons.add("SELECTED_FACTOR_STATUS_UNKNOWN");
        }
        if (selectedFactorStatusHasStatus(
                factorStatusDetails,
                selectedCandidateId,
                DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL)) {
            reasons.add("SELECTED_FACTOR_STATUS_PARTIAL");
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
        if (factorStatusHasStatus(factorStatusDetails, DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL)) {
            reasons.add("FACTOR_STATUS_PARTIAL");
        }
        if (factorStatusHasStatus(factorStatusDetails, DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN)) {
            reasons.add("FACTOR_STATUS_UNKNOWN");
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

    private static List<DecisionExplorerFactorStatusV1> factorStatusDetails(
            List<DecisionFactorDrilldownV1> factorDrilldowns) {
        List<DecisionFactorDrilldownV1> sortedFactors = factorDrilldowns.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(DecisionFactorDrilldownV1::candidateId)
                        .thenComparing(DecisionFactorDrilldownV1::factorName)
                        .thenComparing(DecisionFactorDrilldownV1::observedValueOrStatus))
                .toList();
        List<DecisionExplorerFactorStatusV1> details = new ArrayList<>();
        for (int index = 0; index < sortedFactors.size(); index++) {
            details.add(factorStatus(sortedFactors.get(index), index + 1));
        }
        return List.copyOf(details);
    }

    private static DecisionExplorerFactorStatusV1 factorStatus(
            DecisionFactorDrilldownV1 factor,
            int displayOrder) {
        List<String> warnings = distinctSorted(factor.warnings());
        List<String> unknowns = nonBoundaryUnknowns(factor.unknowns());
        List<String> sourceReferenceIds = distinctSorted(factor.sourceReferenceIds());
        List<String> reasons = new ArrayList<>();
        String factorStatus = factorStatus(factor, warnings, unknowns, sourceReferenceIds, reasons);
        return new DecisionExplorerFactorStatusV1(
                factor.candidateId(),
                factor.factorName(),
                displayOrder,
                factorStatus,
                factor.evidenceStatus(),
                factor.observedValueOrStatus(),
                factor.influenceCategory(),
                factorInterpretation(factorStatus, factor, reasons),
                distinctSorted(reasons),
                warnings,
                unknowns,
                sourceReferenceIds,
                factor.boundaryNote());
    }

    private static String factorStatus(
            DecisionFactorDrilldownV1 factor,
            List<String> warnings,
            List<String> unknowns,
            List<String> sourceReferenceIds,
            List<String> reasons) {
        if ("UNKNOWN".equals(DecisionExplorerDtoSupport.valueOrUnknown(factor.candidateId()))
                || "UNKNOWN".equals(DecisionExplorerDtoSupport.valueOrUnknown(factor.factorName()))) {
            reasons.add("FACTOR_IDENTITY_UNKNOWN");
            return DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
        }
        if (isDegradedFactor(factor)) {
            reasons.add("FACTOR_EVIDENCE_DEGRADED");
            return DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED;
        }
        String evidenceStatus = DecisionExplorerDtoSupport.valueOrUnknown(factor.evidenceStatus());
        if ("UNKNOWN".equals(evidenceStatus)) {
            reasons.add("FACTOR_EVIDENCE_UNKNOWN");
            return DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
        }
        if ("PARTIAL".equals(evidenceStatus)) {
            reasons.add("FACTOR_EVIDENCE_PARTIAL");
        } else if (!"AVAILABLE".equals(evidenceStatus)) {
            reasons.add("FACTOR_EVIDENCE_STATUS_" + evidenceStatus);
        }
        if ("UNKNOWN".equals(DecisionExplorerDtoSupport.valueOrUnknown(factor.observedValueOrStatus()))) {
            reasons.add("FACTOR_OBSERVED_VALUE_UNKNOWN");
        }
        if ("UNKNOWN".equals(DecisionExplorerDtoSupport.valueOrUnknown(factor.influenceCategory()))
                || "UNKNOWN_INFLUENCE".equals(DecisionExplorerDtoSupport.valueOrUnknown(factor.influenceCategory()))) {
            reasons.add("FACTOR_INFLUENCE_UNKNOWN");
        }
        if ("UNKNOWN".equals(DecisionExplorerDtoSupport.valueOrUnknown(factor.explanation()))) {
            reasons.add("FACTOR_EXPLANATION_UNKNOWN");
        }
        if (sourceReferenceIds.isEmpty()) {
            reasons.add("FACTOR_SOURCE_REFERENCES_UNKNOWN");
        }
        if (!warnings.isEmpty()) {
            reasons.add("FACTOR_WARNINGS_PRESENT");
        }
        if (!unknowns.isEmpty()) {
            reasons.add("FACTOR_UNKNOWNS_PRESENT");
        }
        if (!reasons.isEmpty()) {
            return DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL;
        }
        reasons.add("FACTOR_EVIDENCE_AVAILABLE");
        reasons.add("FACTOR_SOURCE_REFERENCES_AVAILABLE");
        return DecisionExplorerConfidenceSummaryV1.STATUS_STRONG;
    }

    private static boolean isDegradedFactor(DecisionFactorDrilldownV1 factor) {
        String evidenceStatus = DecisionExplorerDtoSupport.valueOrUnknown(factor.evidenceStatus());
        if (List.of("DEGRADED", "FAILED", "UNAVAILABLE").contains(evidenceStatus)) {
            return true;
        }
        return isHealthFactor(factor.factorName()) && isDegradedHealthValue(factor.observedValueOrStatus());
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

    private static String factorInterpretation(
            String factorStatus,
            DecisionFactorDrilldownV1 factor,
            List<String> reasons) {
        String candidateId = DecisionExplorerDtoSupport.valueOrUnknown(factor.candidateId());
        String factorName = DecisionExplorerDtoSupport.valueOrUnknown(factor.factorName());
        return switch (factorStatus) {
            case DecisionExplorerConfidenceSummaryV1.STATUS_STRONG ->
                    "Factor " + factorName + " has available evidence for candidate " + candidateId + ".";
            case DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED ->
                    "Factor " + factorName + " indicates degraded evidence for candidate " + candidateId + ".";
            case DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN ->
                    "Factor " + factorName + " has unknown evidence for candidate " + candidateId + ".";
            default ->
                    "Factor " + factorName + " has partial evidence for candidate " + candidateId
                            + " because " + String.join(", ", distinctSorted(reasons)) + ".";
        };
    }

    private static List<DecisionExplorerCandidateConfidenceV1> candidateConfidenceDetails(
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            List<DecisionFactorDrilldownV1> factorDrilldowns) {
        return candidateComparisons.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingInt(DecisionExplorerCandidateComparisonRowV1::displayOrder)
                        .thenComparing(DecisionExplorerCandidateComparisonRowV1::candidateId))
                .map(candidate -> candidateConfidence(candidate, factorsForCandidate(
                        factorDrilldowns,
                        candidate.candidateId())))
                .toList();
    }

    private static DecisionExplorerCandidateConfidenceV1 candidateConfidence(
            DecisionExplorerCandidateComparisonRowV1 candidate,
            List<DecisionFactorDrilldownV1> candidateFactors) {
        int availableFactorCount = countFactors(candidateFactors, "AVAILABLE");
        int partialFactorCount = countFactors(candidateFactors, "PARTIAL");
        int unknownFactorCount = countFactors(candidateFactors, "UNKNOWN");
        List<String> warnings = candidateWarnings(candidate, candidateFactors);
        List<String> unknowns = candidateUnknowns(candidate, candidateFactors);
        List<String> sourceReferenceIds = candidateSourceReferenceIds(candidate, candidateFactors);
        String healthEvidenceState = healthEvidenceState(candidate.visibleSignals());
        List<String> reasons = new ArrayList<>();
        String confidenceStatus = candidateConfidenceStatus(
                candidate,
                healthEvidenceState,
                availableFactorCount,
                partialFactorCount,
                unknownFactorCount,
                warnings,
                unknowns,
                sourceReferenceIds,
                reasons);
        return new DecisionExplorerCandidateConfidenceV1(
                candidate.candidateId(),
                candidate.candidateLabel(),
                candidate.selected(),
                candidate.displayOrder(),
                confidenceStatus,
                healthEvidenceState,
                candidate.comparisonStatus(),
                candidate.finalScore(),
                candidate.scoreDeltaFromSelected(),
                candidate.visibleSignals().size(),
                candidate.unknownSignals().size(),
                availableFactorCount,
                partialFactorCount,
                unknownFactorCount,
                distinctSorted(reasons),
                warnings,
                unknowns,
                sourceReferenceIds,
                candidate.boundaryNote());
    }

    private static String candidateConfidenceStatus(
            DecisionExplorerCandidateComparisonRowV1 candidate,
            String healthEvidenceState,
            int availableFactorCount,
            int partialFactorCount,
            int unknownFactorCount,
            List<String> warnings,
            List<String> unknowns,
            List<String> sourceReferenceIds,
            List<String> reasons) {
        if ("UNKNOWN".equals(DecisionExplorerDtoSupport.valueOrUnknown(candidate.candidateId()))) {
            reasons.add("CANDIDATE_ID_UNKNOWN");
            return DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
        }
        if (DecisionExplorerCandidateConfidenceV1.DEGRADED.equals(healthEvidenceState)) {
            reasons.add("HEALTH_SIGNAL_DEGRADED");
            return DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED;
        }
        String comparisonStatus = DecisionExplorerDtoSupport.valueOrUnknown(candidate.comparisonStatus());
        if ("PARTIAL_EVIDENCE".equals(comparisonStatus)) {
            reasons.add("PARTIAL_CANDIDATE_COMPARISON_EVIDENCE");
        }
        if ("SELECTED_SCORE_UNKNOWN".equals(comparisonStatus)) {
            reasons.add("SELECTED_SCORE_UNKNOWN");
        }
        if (candidate.finalScore() == null || !Double.isFinite(candidate.finalScore())) {
            reasons.add("FINAL_SCORE_UNKNOWN");
        }
        if (DecisionExplorerCandidateConfidenceV1.UNKNOWN.equals(healthEvidenceState)) {
            reasons.add("HEALTH_SIGNAL_UNKNOWN");
        }
        if (partialFactorCount > 0) {
            reasons.add("PARTIAL_FACTOR_EVIDENCE");
        }
        if (unknownFactorCount > 0) {
            reasons.add("UNKNOWN_FACTOR_EVIDENCE");
        }
        if (availableFactorCount == 0 && partialFactorCount == 0 && unknownFactorCount == 0) {
            reasons.add("FACTOR_EVIDENCE_UNKNOWN");
        }
        if (sourceReferenceIds.isEmpty()) {
            reasons.add("SOURCE_REFERENCES_UNKNOWN");
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
        reasons.add("CANDIDATE_COMPARISON_AVAILABLE");
        reasons.add("FACTOR_EVIDENCE_AVAILABLE");
        reasons.add("HEALTH_SIGNAL_HEALTHY");
        return DecisionExplorerConfidenceSummaryV1.STATUS_STRONG;
    }

    private static boolean selectedCandidateConfidenceHasStatus(
            List<DecisionExplorerCandidateConfidenceV1> candidateConfidenceDetails,
            String status) {
        return candidateConfidenceDetails.stream()
                .filter(DecisionExplorerCandidateConfidenceV1::selected)
                .map(DecisionExplorerCandidateConfidenceV1::confidenceStatus)
                .map(DecisionExplorerDtoSupport::valueOrUnknown)
                .anyMatch(status::equals);
    }

    private static boolean selectedFactorStatusHasStatus(
            List<DecisionExplorerFactorStatusV1> factorStatusDetails,
            String selectedCandidateId,
            String status) {
        String normalizedSelectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateId);
        return factorStatusDetails.stream()
                .filter(factor -> normalizedSelectedCandidateId.equals(DecisionExplorerDtoSupport.valueOrUnknown(
                        factor.candidateId())))
                .map(DecisionExplorerFactorStatusV1::factorStatus)
                .map(DecisionExplorerDtoSupport::valueOrUnknown)
                .anyMatch(status::equals);
    }

    private static boolean factorStatusHasStatus(
            List<DecisionExplorerFactorStatusV1> factorStatusDetails,
            String status) {
        return factorStatusDetails.stream()
                .map(DecisionExplorerFactorStatusV1::factorStatus)
                .map(DecisionExplorerDtoSupport::valueOrUnknown)
                .anyMatch(status::equals);
    }

    private static List<DecisionFactorDrilldownV1> factorsForCandidate(
            List<DecisionFactorDrilldownV1> factorDrilldowns,
            String candidateId) {
        String normalizedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        return factorDrilldowns.stream()
                .filter(factor -> normalizedCandidateId.equals(DecisionExplorerDtoSupport.valueOrUnknown(
                        factor.candidateId())))
                .toList();
    }

    private static List<String> candidateWarnings(
            DecisionExplorerCandidateComparisonRowV1 candidate,
            List<DecisionFactorDrilldownV1> candidateFactors) {
        List<String> warnings = new ArrayList<>(candidate.warnings());
        candidateFactors.stream()
                .flatMap(factor -> factor.warnings().stream())
                .forEach(warnings::add);
        return distinctSorted(warnings);
    }

    private static List<String> candidateUnknowns(
            DecisionExplorerCandidateComparisonRowV1 candidate,
            List<DecisionFactorDrilldownV1> candidateFactors) {
        List<String> unknowns = new ArrayList<>();
        candidate.unknowns().stream()
                .filter(unknown -> !isBoundaryLimitation(unknown))
                .forEach(unknowns::add);
        candidateFactors.stream()
                .flatMap(factor -> factor.unknowns().stream())
                .filter(unknown -> !isBoundaryLimitation(unknown))
                .forEach(unknowns::add);
        return distinctSorted(unknowns);
    }

    private static List<String> candidateSourceReferenceIds(
            DecisionExplorerCandidateComparisonRowV1 candidate,
            List<DecisionFactorDrilldownV1> candidateFactors) {
        List<String> references = new ArrayList<>(candidate.evidenceReferenceIds());
        candidateFactors.stream()
                .flatMap(factor -> factor.sourceReferenceIds().stream())
                .forEach(references::add);
        return distinctSorted(references);
    }

    private static String healthEvidenceState(List<String> visibleSignals) {
        if (visibleSignals == null) {
            return DecisionExplorerCandidateConfidenceV1.UNKNOWN;
        }
        return visibleSignals.stream()
                .filter(signal -> signal != null)
                .map(String::trim)
                .filter(signal -> signal.toLowerCase(Locale.ROOT).startsWith("healthstate="))
                .findFirst()
                .map(DecisionExplorerConfidenceSummaryService::healthEvidenceValue)
                .orElse(DecisionExplorerCandidateConfidenceV1.UNKNOWN);
    }

    private static String healthEvidenceValue(String signal) {
        String normalized = signal.substring(signal.indexOf('=') + 1).trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "healthy" -> DecisionExplorerCandidateConfidenceV1.HEALTHY;
            case "false", "unhealthy", "degraded" -> DecisionExplorerCandidateConfidenceV1.DEGRADED;
            default -> DecisionExplorerCandidateConfidenceV1.UNKNOWN;
        };
    }

    private static List<String> nonBoundaryLimitations(List<String> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .filter(value -> !isBoundaryLimitation(value))
                        .toList();
    }

    private static List<String> nonBoundaryUnknowns(List<String> values) {
        return distinctSorted(nonBoundaryLimitations(values));
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
            int sourceReferenceCount,
            int candidateConfidenceDetailCount,
            int factorStatusDetailCount) {
        return List.of(
                "availableFactorCount=" + availableFactorCount,
                "candidateComparisonCount=" + candidateComparisonCount,
                "candidateConfidenceDetailCount=" + candidateConfidenceDetailCount,
                "candidateCount=" + candidateCount,
                "decisionStatus=" + decisionStatus,
                "factorStatusDetailCount=" + factorStatusDetailCount,
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
