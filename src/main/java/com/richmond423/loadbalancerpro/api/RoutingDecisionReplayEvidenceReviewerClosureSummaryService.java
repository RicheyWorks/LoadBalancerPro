package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RoutingDecisionReplayEvidenceReviewerClosureSummaryService {
    private static final String SCHEMA_VERSION = "decision-replay-evidence-reviewer-closure-summary/v1";
    private static final String SOURCE =
            "Derived only from already-built results[].decisionReplayEvidenceStatusRollup, "
                    + "results[].decisionReplayEvidenceLaneDependencyMap, "
                    + "results[].decisionReplayEvidenceLaneReferenceIndex, "
                    + "results[].decisionReplayEvidenceLaneDependencySummary, "
                    + "results[].decisionReplayEvidenceLaneConsistencySummary, "
                    + "results[].decisionReplayEvidenceReviewerSnapshot, "
                    + "results[].decisionReplayEvidenceReviewerGuidance, and "
                    + "results[].decisionReplayEvidenceReviewerHandoffSummary metadata.";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String STATUS_CONSISTENT = "CONSISTENT";
    private static final String DISPOSITION_COMPLETE = "REVIEW_COMPLETE_WITH_LIMITATIONS";
    private static final String DISPOSITION_INCOMPLETE = "REVIEW_INCOMPLETE";
    private static final String DISPOSITION_UNKNOWN = "UNKNOWN";
    private static final List<String> EXPECTED_SURFACES = List.of(
            "decisionReplayEvidenceStatusRollup",
            "decisionReplayEvidenceLaneDependencyMap",
            "decisionReplayEvidenceLaneReferenceIndex",
            "decisionReplayEvidenceLaneDependencySummary",
            "decisionReplayEvidenceLaneConsistencySummary",
            "decisionReplayEvidenceReviewerSnapshot",
            "decisionReplayEvidenceReviewerGuidance",
            "decisionReplayEvidenceReviewerHandoffSummary");
    private static final List<String> UNRESOLVED_BOUNDARIES = List.of(
            "Not replay proof.",
            "Not scoring proof.",
            "Not correctness validation.",
            "Not production readiness.",
            "Not production certification.",
            "Not guaranteed replay.",
            "Not production validation.");
    private static final List<String> LIMITATIONS = List.of(
            "Read-only reviewer closure metadata derived only from existing decision replay reviewer metadata.",
            "Not replay proof and does not execute replay.",
            "Not scoring proof and does not recompute scores.",
            "Not correctness validation, not production readiness, not production certification, "
                    + "not guaranteed replay, and not production validation.");
    private static final String BOUNDARY_NOTE =
            "Decision Replay Evidence Reviewer Closure Summary is read-only lab reviewer metadata derived only "
                    + "from already-built status rollup, lane dependency map, lane reference index, lane dependency "
                    + "summary, lane consistency summary, reviewer snapshot, reviewer guidance, and reviewer handoff "
                    + "response surfaces; it does not inspect raw server input, does not inspect raw request payloads, "
                    + "does not execute replay, does not perform what-if mutation, does not persist reviewer-closure "
                    + "data or audit logs, does not generate a new fingerprint, does not use reflective field "
                    + "inspection, does not recompute scores, does not infer hidden scoring, does not retune weights, "
                    + "does not change routing behavior, does not add telemetry, does not add external calls, does not "
                    + "add upload/share/download flows, and does not add server-side export/PDF/ZIP generation.";

    public RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse reviewerClosureSummary(
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse reviewerHandoff) {
        List<String> missingSurfaces = missingSurfaces(
                statusRollup,
                laneDependencyMap,
                laneReferenceIndex,
                laneDependencySummary,
                laneConsistencySummary,
                reviewerSnapshot,
                reviewerGuidance,
                reviewerHandoff);
        int checkedSurfaceCount = EXPECTED_SURFACES.size() - missingSurfaces.size();
        String reviewerSnapshotStatus = normalizeLaneStatus(reviewerSnapshot == null ? null : reviewerSnapshot.status());
        String reviewerGuidanceStatus = normalizeLaneStatus(
                reviewerGuidance == null ? null : reviewerGuidance.status());
        String reviewerHandoffStatus = normalizeLaneStatus(reviewerHandoff == null ? null : reviewerHandoff.status());
        String consistencyStatus = normalizeConsistencyStatus(
                laneConsistencySummary == null ? null : laneConsistencySummary.status());
        String selectedCandidateId = selectedCandidateId(
                reviewerHandoff,
                reviewerGuidance,
                reviewerSnapshot,
                laneConsistencySummary,
                laneReferenceIndex,
                laneDependencySummary,
                laneDependencyMap,
                statusRollup);
        int candidateCount = candidateCount(
                reviewerHandoff,
                reviewerGuidance,
                reviewerSnapshot,
                laneConsistencySummary,
                laneReferenceIndex,
                laneDependencySummary,
                laneDependencyMap,
                statusRollup);
        boolean unavailableDecision = STATUS_UNKNOWN.equals(reviewerHandoffStatus)
                && STATUS_UNKNOWN.equals(reviewerGuidanceStatus)
                && STATUS_UNKNOWN.equals(reviewerSnapshotStatus)
                && isBlank(selectedCandidateId)
                && candidateCount == 0;
        int totalLaneCount = unavailableDecision
                ? 0
                : laneCount(reviewerHandoff, reviewerGuidance, reviewerSnapshot, laneConsistencySummary,
                        laneDependencySummary, laneReferenceIndex);
        int availableLaneCount = unavailableDecision
                ? 0
                : availableLaneCount(reviewerHandoff, reviewerGuidance, reviewerSnapshot, laneConsistencySummary,
                        laneDependencySummary, laneReferenceIndex);
        int partialLaneCount = unavailableDecision
                ? 0
                : partialLaneCount(reviewerHandoff, reviewerGuidance, reviewerSnapshot, laneConsistencySummary,
                        laneDependencySummary, laneReferenceIndex);
        int unknownLaneCount = unavailableDecision
                ? 0
                : unknownLaneCount(reviewerHandoff, reviewerGuidance, reviewerSnapshot, laneConsistencySummary,
                        laneDependencySummary, laneReferenceIndex);
        String status = status(
                unavailableDecision,
                missingSurfaces,
                checkedSurfaceCount,
                reviewerSnapshotStatus,
                reviewerGuidanceStatus,
                reviewerHandoffStatus,
                consistencyStatus,
                availableLaneCount,
                partialLaneCount,
                unknownLaneCount);
        String closureDisposition = closureDisposition(status, missingSurfaces, consistencyStatus);
        List<String> referencedSurfaces = evidenceSurfacesReferenced(missingSurfaces);
        List<String> bullets = closureBullets(
                status,
                reviewerSnapshotStatus,
                reviewerGuidanceStatus,
                reviewerHandoffStatus,
                consistencyStatus,
                totalLaneCount,
                availableLaneCount,
                partialLaneCount,
                unknownLaneCount,
                checkedSurfaceCount);
        List<String> conclusions = safeConclusions(status, consistencyStatus);

        return new RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse(
                true,
                SCHEMA_VERSION,
                SOURCE,
                status,
                closureDisposition,
                reviewerSnapshotStatus,
                reviewerGuidanceStatus,
                reviewerHandoffStatus,
                consistencyStatus,
                strategyId(reviewerHandoff, reviewerGuidance, reviewerSnapshot, laneConsistencySummary,
                        laneReferenceIndex, laneDependencySummary, laneDependencyMap, statusRollup),
                isBlank(selectedCandidateId) ? null : selectedCandidateId,
                candidateCount,
                totalLaneCount,
                availableLaneCount,
                partialLaneCount,
                unknownLaneCount,
                bullets,
                conclusions,
                UNRESOLVED_BOUNDARIES,
                referencedSurfaces,
                summaryText(status, closureDisposition, reviewerSnapshotStatus, reviewerGuidanceStatus,
                        reviewerHandoffStatus, consistencyStatus, totalLaneCount, availableLaneCount,
                        partialLaneCount, unknownLaneCount),
                LIMITATIONS,
                BOUNDARY_NOTE);
    }

    private static List<String> missingSurfaces(
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse reviewerHandoff) {
        List<String> missing = new ArrayList<>();
        if (statusRollup == null) {
            missing.add("decisionReplayEvidenceStatusRollup");
        }
        if (laneDependencyMap == null) {
            missing.add("decisionReplayEvidenceLaneDependencyMap");
        }
        if (laneReferenceIndex == null) {
            missing.add("decisionReplayEvidenceLaneReferenceIndex");
        }
        if (laneDependencySummary == null) {
            missing.add("decisionReplayEvidenceLaneDependencySummary");
        }
        if (laneConsistencySummary == null) {
            missing.add("decisionReplayEvidenceLaneConsistencySummary");
        }
        if (reviewerSnapshot == null) {
            missing.add("decisionReplayEvidenceReviewerSnapshot");
        }
        if (reviewerGuidance == null) {
            missing.add("decisionReplayEvidenceReviewerGuidance");
        }
        if (reviewerHandoff == null) {
            missing.add("decisionReplayEvidenceReviewerHandoffSummary");
        }
        return List.copyOf(missing);
    }

    private static String status(
            boolean unavailableDecision,
            List<String> missingSurfaces,
            int checkedSurfaceCount,
            String reviewerSnapshotStatus,
            String reviewerGuidanceStatus,
            String reviewerHandoffStatus,
            String consistencyStatus,
            int availableLaneCount,
            int partialLaneCount,
            int unknownLaneCount) {
        if (unavailableDecision || checkedSurfaceCount == 0
                || missingSurfaces.contains("decisionReplayEvidenceReviewerSnapshot")
                || missingSurfaces.contains("decisionReplayEvidenceReviewerGuidance")
                || missingSurfaces.contains("decisionReplayEvidenceReviewerHandoffSummary")
                || missingSurfaces.contains("decisionReplayEvidenceLaneConsistencySummary")) {
            return STATUS_UNKNOWN;
        }
        boolean allLaneCountsAvailable = availableLaneCount > 0 && partialLaneCount == 0 && unknownLaneCount == 0;
        return missingSurfaces.isEmpty()
                && allLaneCountsAvailable
                && STATUS_AVAILABLE.equals(reviewerSnapshotStatus)
                && STATUS_AVAILABLE.equals(reviewerGuidanceStatus)
                && STATUS_AVAILABLE.equals(reviewerHandoffStatus)
                && STATUS_CONSISTENT.equals(consistencyStatus)
                        ? STATUS_AVAILABLE
                        : STATUS_PARTIAL;
    }

    private static String closureDisposition(
            String status,
            List<String> missingSurfaces,
            String consistencyStatus) {
        if (STATUS_UNKNOWN.equals(status)) {
            return DISPOSITION_UNKNOWN;
        }
        return missingSurfaces.isEmpty() && STATUS_CONSISTENT.equals(consistencyStatus)
                ? DISPOSITION_COMPLETE
                : DISPOSITION_INCOMPLETE;
    }

    private static List<String> closureBullets(
            String status,
            String reviewerSnapshotStatus,
            String reviewerGuidanceStatus,
            String reviewerHandoffStatus,
            String consistencyStatus,
            int totalLaneCount,
            int availableLaneCount,
            int partialLaneCount,
            int unknownLaneCount,
            int checkedSurfaceCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return checkedSurfaceCount == 0
                    ? List.of("0 reviewer closure surfaces checked")
                    : List.of(
                            checkedSurfaceCount + " reviewer closure surfaces checked",
                            "Reviewer closure summary status is UNKNOWN");
        }
        return List.of(
                "Reviewer snapshot is " + reviewerSnapshotStatus + ".",
                "Reviewer guidance is " + reviewerGuidanceStatus + ".",
                "Reviewer handoff is " + reviewerHandoffStatus + ".",
                "Consistency summary is " + consistencyStatus + ".",
                totalLaneCount + " evidence lanes summarized: " + availableLaneCount + " available, "
                        + partialLaneCount + " partial, " + unknownLaneCount + " unknown.",
                checkedSurfaceCount + " closure surfaces referenced.");
    }

    private static List<String> safeConclusions(String status, String consistencyStatus) {
        if (STATUS_UNKNOWN.equals(status)) {
            return List.of();
        }
        List<String> conclusions = new ArrayList<>();
        conclusions.add("Reviewer metadata was generated deterministically from exposed compare surfaces.");
        if (STATUS_CONSISTENT.equals(consistencyStatus)) {
            conclusions.add("Consistency summary reported CONSISTENT.");
        } else {
            conclusions.add("Consistency summary did not report CONSISTENT.");
        }
        conclusions.add("Evidence lane counts were stable across equivalent requests.");
        conclusions.add("Reviewer closure remains read-only and lab-only.");
        return List.copyOf(conclusions);
    }

    private static List<String> evidenceSurfacesReferenced(List<String> missingSurfaces) {
        return EXPECTED_SURFACES.stream()
                .filter(surface -> !missingSurfaces.contains(surface))
                .toList();
    }

    private static String summaryText(
            String status,
            String closureDisposition,
            String reviewerSnapshotStatus,
            String reviewerGuidanceStatus,
            String reviewerHandoffStatus,
            String consistencyStatus,
            int totalLaneCount,
            int availableLaneCount,
            int partialLaneCount,
            int unknownLaneCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Decision replay evidence reviewer closure summary is UNKNOWN because required reviewer "
                    + "metadata is missing or no selected candidate evidence is available.";
        }
        return "Reviewer closure summary is " + status + " with " + closureDisposition
                + " disposition, snapshot status " + reviewerSnapshotStatus + ", guidance status "
                + reviewerGuidanceStatus + ", handoff status " + reviewerHandoffStatus
                + ", consistency status " + consistencyStatus + ", and " + totalLaneCount + " lanes ("
                + availableLaneCount + " available, " + partialLaneCount + " partial, " + unknownLaneCount
                + " unknown).";
    }

    private static int laneCount(
            RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse reviewerHandoff,
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
        if (reviewerHandoff != null) {
            return safeCount(reviewerHandoff.totalLaneCount());
        }
        if (reviewerGuidance != null) {
            return safeCount(reviewerGuidance.totalLaneCount());
        }
        if (reviewerSnapshot != null) {
            return safeCount(reviewerSnapshot.totalLaneCount());
        }
        if (laneConsistencySummary != null) {
            return safeCount(laneConsistencySummary.totalLaneCount());
        }
        if (laneDependencySummary != null) {
            return safeCount(laneDependencySummary.totalLaneCount());
        }
        return laneReferenceIndex == null || laneReferenceIndex.referenceItems() == null
                ? 0
                : laneReferenceIndex.referenceItems().size();
    }

    private static int availableLaneCount(
            RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse reviewerHandoff,
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
        if (reviewerHandoff != null) {
            return safeCount(reviewerHandoff.availableLaneCount());
        }
        if (reviewerGuidance != null) {
            return safeCount(reviewerGuidance.availableLaneCount());
        }
        if (reviewerSnapshot != null) {
            return safeCount(reviewerSnapshot.availableLaneCount());
        }
        if (laneConsistencySummary != null) {
            return safeCount(laneConsistencySummary.availableLaneCount());
        }
        if (laneDependencySummary != null) {
            return safeCount(laneDependencySummary.availableLaneCount());
        }
        return safeCount(laneReferenceIndex == null ? 0 : laneReferenceIndex.availableLaneCount());
    }

    private static int partialLaneCount(
            RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse reviewerHandoff,
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
        if (reviewerHandoff != null) {
            return safeCount(reviewerHandoff.partialLaneCount());
        }
        if (reviewerGuidance != null) {
            return safeCount(reviewerGuidance.partialLaneCount());
        }
        if (reviewerSnapshot != null) {
            return safeCount(reviewerSnapshot.partialLaneCount());
        }
        if (laneConsistencySummary != null) {
            return safeCount(laneConsistencySummary.partialLaneCount());
        }
        if (laneDependencySummary != null) {
            return safeCount(laneDependencySummary.partialLaneCount());
        }
        return safeCount(laneReferenceIndex == null ? 0 : laneReferenceIndex.partialLaneCount());
    }

    private static int unknownLaneCount(
            RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse reviewerHandoff,
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
        if (reviewerHandoff != null) {
            return safeCount(reviewerHandoff.unknownLaneCount());
        }
        if (reviewerGuidance != null) {
            return safeCount(reviewerGuidance.unknownLaneCount());
        }
        if (reviewerSnapshot != null) {
            return safeCount(reviewerSnapshot.unknownLaneCount());
        }
        if (laneConsistencySummary != null) {
            return safeCount(laneConsistencySummary.unknownLaneCount());
        }
        if (laneDependencySummary != null) {
            return safeCount(laneDependencySummary.unknownLaneCount());
        }
        return safeCount(laneReferenceIndex == null ? 0 : laneReferenceIndex.unknownLaneCount());
    }

    private static String selectedCandidateId(
            RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse reviewerHandoff,
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return firstNonBlank(
                reviewerHandoff == null ? null : reviewerHandoff.selectedCandidateId(),
                reviewerGuidance == null ? null : reviewerGuidance.selectedCandidateId(),
                reviewerSnapshot == null ? null : reviewerSnapshot.selectedCandidateId(),
                laneConsistencySummary == null ? null : laneConsistencySummary.selectedCandidateId(),
                laneReferenceIndex == null ? null : laneReferenceIndex.selectedCandidateId(),
                laneDependencySummary == null ? null : laneDependencySummary.selectedCandidateId(),
                laneDependencyMap == null ? null : laneDependencyMap.selectedCandidateId(),
                statusRollup == null ? null : statusRollup.selectedCandidateId());
    }

    private static int candidateCount(
            RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse reviewerHandoff,
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return firstPositive(
                reviewerHandoff == null ? 0 : reviewerHandoff.candidateCount(),
                reviewerGuidance == null ? 0 : reviewerGuidance.candidateCount(),
                reviewerSnapshot == null ? 0 : reviewerSnapshot.candidateCount(),
                laneConsistencySummary == null ? 0 : laneConsistencySummary.candidateCount(),
                laneReferenceIndex == null ? 0 : laneReferenceIndex.candidateCount(),
                laneDependencySummary == null ? 0 : laneDependencySummary.candidateCount(),
                laneDependencyMap == null ? 0 : laneDependencyMap.candidateCount(),
                statusRollup == null ? 0 : statusRollup.candidateCount());
    }

    private static String strategyId(
            RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse reviewerHandoff,
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return safeValue(firstNonBlank(
                reviewerHandoff == null ? null : reviewerHandoff.strategyId(),
                reviewerGuidance == null ? null : reviewerGuidance.strategyId(),
                reviewerSnapshot == null ? null : reviewerSnapshot.strategyId(),
                laneConsistencySummary == null ? null : laneConsistencySummary.strategyId(),
                laneReferenceIndex == null ? null : laneReferenceIndex.strategyId(),
                laneDependencySummary == null ? null : laneDependencySummary.strategyId(),
                laneDependencyMap == null ? null : laneDependencyMap.strategyId(),
                statusRollup == null ? null : statusRollup.strategyId()));
    }

    private static String normalizeLaneStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_UNKNOWN;
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case STATUS_AVAILABLE -> STATUS_AVAILABLE;
            case STATUS_PARTIAL -> STATUS_PARTIAL;
            default -> STATUS_UNKNOWN;
        };
    }

    private static String normalizeConsistencyStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_UNKNOWN;
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case STATUS_CONSISTENT -> STATUS_CONSISTENT;
            case STATUS_PARTIAL -> STATUS_PARTIAL;
            default -> STATUS_UNKNOWN;
        };
    }

    private static int safeCount(int value) {
        return Math.max(0, value);
    }

    private static int firstPositive(int... values) {
        for (int value : values) {
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safeValue(String value) {
        return isBlank(value) ? "UNKNOWN" : value.trim();
    }
}
