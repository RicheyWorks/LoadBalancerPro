package com.richmond423.loadbalancerpro.lab;

import java.util.List;

record LocalLabImplementationReadinessAssessment(
        String gateId,
        String statusLabel,
        int criteriaCount,
        int passedCriteriaCount,
        int blockedCriteriaCount,
        List<LocalLabImplementationReadinessCriterion> criteria,
        String readinessSummary,
        String nextSafeImplementationCandidate,
        String safetyBoundarySummary,
        String notProvenBoundarySummary) {

    LocalLabImplementationReadinessAssessment {
        requireText("gateId", gateId);
        requireText("statusLabel", statusLabel);
        if (criteria == null || criteria.isEmpty()) {
            throw new IllegalArgumentException("criteria are required");
        }
        criteria = List.copyOf(criteria);
        if (criteriaCount != criteria.size()) {
            throw new IllegalArgumentException("criteriaCount must match criteria size");
        }
        if (passedCriteriaCount != criteria.stream().filter(LocalLabImplementationReadinessCriterion::passed)
                .count()) {
            throw new IllegalArgumentException("passedCriteriaCount must match passed criteria");
        }
        if (blockedCriteriaCount != criteriaCount - passedCriteriaCount) {
            throw new IllegalArgumentException("blockedCriteriaCount must match blocked criteria");
        }
        requireText("readinessSummary", readinessSummary);
        requireText("nextSafeImplementationCandidate", nextSafeImplementationCandidate);
        requireText("safetyBoundarySummary", safetyBoundarySummary);
        requireText("notProvenBoundarySummary", notProvenBoundarySummary);
    }

    String deterministicText() {
        return String.join(" ",
                gateId,
                statusLabel,
                Integer.toString(criteriaCount),
                Integer.toString(passedCriteriaCount),
                Integer.toString(blockedCriteriaCount),
                String.join(" | ", criteria.stream()
                        .map(LocalLabImplementationReadinessCriterion::deterministicText)
                        .toList()),
                readinessSummary,
                nextSafeImplementationCandidate,
                safetyBoundarySummary,
                notProvenBoundarySummary);
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
