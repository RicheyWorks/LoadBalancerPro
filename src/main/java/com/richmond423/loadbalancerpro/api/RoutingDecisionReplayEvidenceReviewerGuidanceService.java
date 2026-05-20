package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RoutingDecisionReplayEvidenceReviewerGuidanceService {
    private static final String SCHEMA_VERSION = "decision-replay-evidence-reviewer-guidance/v1";
    private static final String SOURCE =
            "Derived only from already-built results[].decisionReplayEvidenceStatusRollup, "
                    + "results[].decisionReplayEvidenceLaneDependencyMap, "
                    + "results[].decisionReplayEvidenceLaneReferenceIndex, "
                    + "results[].decisionReplayEvidenceLaneDependencySummary, "
                    + "results[].decisionReplayEvidenceLaneConsistencySummary, and "
                    + "results[].decisionReplayEvidenceReviewerSnapshot metadata.";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String STATUS_CONSISTENT = "CONSISTENT";
    private static final String PRIORITY_REVIEW = "REVIEW";
    private static final String PRIORITY_INFORMATIONAL = "INFORMATIONAL";
    private static final String PRIORITY_UNKNOWN = "UNKNOWN";
    private static final List<String> EXPECTED_SURFACES = List.of(
            "decisionReplayEvidenceStatusRollup",
            "decisionReplayEvidenceLaneDependencyMap",
            "decisionReplayEvidenceLaneReferenceIndex",
            "decisionReplayEvidenceLaneDependencySummary",
            "decisionReplayEvidenceLaneConsistencySummary",
            "decisionReplayEvidenceReviewerSnapshot");
    private static final List<String> LIMITATIONS = List.of(
            "Read-only reviewer guidance derived only from existing decision replay reviewer metadata.",
            "Not replay proof and does not execute replay.",
            "Not scoring proof and does not recompute scores.",
            "Not correctness validation, not production readiness, not production certification, and not guaranteed replay.");
    private static final String BOUNDARY_NOTE =
            "Decision Replay Evidence Reviewer Guidance is read-only lab reviewer metadata derived only from "
                    + "already-built status rollup, lane dependency map, lane reference index, lane dependency "
                    + "summary, lane consistency summary, and reviewer snapshot response surfaces; it "
                    + "does not inspect raw server input, does not inspect raw request payloads, does not execute replay, "
                    + "does not perform what-if mutation, does not persist reviewer-guidance data or audit logs, "
                    + "does not generate a new fingerprint, does not use reflective field inspection, does not "
                    + "recompute scores, does not infer hidden scoring, does not retune weights, "
                    + "does not change routing behavior, does not add telemetry, does not add external calls, does not add "
                    + "upload/share/download flows, and does not add server-side export/PDF/ZIP generation.";

    public RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance(
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot) {
        List<String> missingSurfaces = missingSurfaces(
                statusRollup,
                laneDependencyMap,
                laneReferenceIndex,
                laneDependencySummary,
                laneConsistencySummary,
                reviewerSnapshot);
        int checkedSurfaceCount = EXPECTED_SURFACES.size() - missingSurfaces.size();
        String reviewerSnapshotStatus = normalizeLaneStatus(reviewerSnapshot == null ? null : reviewerSnapshot.status());
        String consistencyStatus = normalizeConsistencyStatus(
                laneConsistencySummary == null ? null : laneConsistencySummary.status());
        String selectedCandidateId = selectedCandidateId(
                reviewerSnapshot,
                laneConsistencySummary,
                laneReferenceIndex,
                laneDependencySummary,
                laneDependencyMap,
                statusRollup);
        int candidateCount = candidateCount(
                reviewerSnapshot,
                laneConsistencySummary,
                laneReferenceIndex,
                laneDependencySummary,
                laneDependencyMap,
                statusRollup);
        boolean unavailableDecision = STATUS_UNKNOWN.equals(reviewerSnapshotStatus)
                && isBlank(selectedCandidateId)
                && candidateCount == 0;
        int totalLaneCount = unavailableDecision
                ? 0
                : laneCount(reviewerSnapshot, laneConsistencySummary, laneDependencySummary, laneReferenceIndex);
        int availableLaneCount = unavailableDecision
                ? 0
                : availableLaneCount(reviewerSnapshot, laneConsistencySummary, laneDependencySummary, laneReferenceIndex);
        int partialLaneCount = unavailableDecision
                ? 0
                : partialLaneCount(reviewerSnapshot, laneConsistencySummary, laneDependencySummary, laneReferenceIndex);
        int unknownLaneCount = unavailableDecision
                ? 0
                : unknownLaneCount(reviewerSnapshot, laneConsistencySummary, laneDependencySummary, laneReferenceIndex);
        String status = status(
                unavailableDecision,
                missingSurfaces,
                checkedSurfaceCount,
                reviewerSnapshotStatus,
                consistencyStatus,
                availableLaneCount,
                partialLaneCount,
                unknownLaneCount);
        String reviewerPriority = reviewerPriority(
                status,
                missingSurfaces,
                reviewerSnapshotStatus,
                consistencyStatus,
                partialLaneCount,
                unknownLaneCount);
        List<String> evidenceSurfacesToInspect = evidenceSurfacesToInspect(missingSurfaces);
        List<String> cautionNotes = cautionNotes(
                unavailableDecision,
                missingSurfaces,
                reviewerSnapshotStatus,
                consistencyStatus,
                partialLaneCount,
                unknownLaneCount);

        return new RoutingDecisionReplayEvidenceReviewerGuidanceResponse(
                true,
                SCHEMA_VERSION,
                SOURCE,
                status,
                reviewerPriority,
                strategyId(reviewerSnapshot, laneConsistencySummary, laneReferenceIndex, laneDependencySummary,
                        laneDependencyMap, statusRollup),
                isBlank(selectedCandidateId) ? null : selectedCandidateId,
                candidateCount,
                totalLaneCount,
                availableLaneCount,
                partialLaneCount,
                unknownLaneCount,
                checkedSurfaceCount,
                missingSurfaces.size(),
                missingSurfaces,
                primaryReviewerFocus(status, partialLaneCount, unknownLaneCount, missingSurfaces),
                suggestedReviewSteps(status, checkedSurfaceCount, unavailableDecision),
                evidenceSurfacesToInspect,
                cautionNotes,
                summaryText(status, reviewerPriority, reviewerSnapshotStatus, checkedSurfaceCount, missingSurfaces),
                LIMITATIONS,
                BOUNDARY_NOTE);
    }

    private static List<String> missingSurfaces(
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot) {
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
        return List.copyOf(missing);
    }

    private static String status(
            boolean unavailableDecision,
            List<String> missingSurfaces,
            int checkedSurfaceCount,
            String reviewerSnapshotStatus,
            String consistencyStatus,
            int availableLaneCount,
            int partialLaneCount,
            int unknownLaneCount) {
        if (unavailableDecision || checkedSurfaceCount == 0
                || missingSurfaces.contains("decisionReplayEvidenceLaneReferenceIndex")
                || missingSurfaces.contains("decisionReplayEvidenceLaneDependencySummary")
                || missingSurfaces.contains("decisionReplayEvidenceLaneConsistencySummary")
                || missingSurfaces.contains("decisionReplayEvidenceReviewerSnapshot")) {
            return STATUS_UNKNOWN;
        }
        boolean allLaneCountsAvailable = availableLaneCount > 0 && partialLaneCount == 0 && unknownLaneCount == 0;
        return missingSurfaces.isEmpty()
                && allLaneCountsAvailable
                && STATUS_AVAILABLE.equals(reviewerSnapshotStatus)
                && STATUS_CONSISTENT.equals(consistencyStatus)
                        ? STATUS_AVAILABLE
                        : STATUS_PARTIAL;
    }

    private static String reviewerPriority(
            String status,
            List<String> missingSurfaces,
            String reviewerSnapshotStatus,
            String consistencyStatus,
            int partialLaneCount,
            int unknownLaneCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return PRIORITY_UNKNOWN;
        }
        return STATUS_PARTIAL.equals(status)
                || !missingSurfaces.isEmpty()
                || !STATUS_AVAILABLE.equals(reviewerSnapshotStatus)
                || !STATUS_CONSISTENT.equals(consistencyStatus)
                || partialLaneCount > 0
                || unknownLaneCount > 0
                        ? PRIORITY_REVIEW
                        : PRIORITY_INFORMATIONAL;
    }

    private static String primaryReviewerFocus(
            String status,
            int partialLaneCount,
            int unknownLaneCount,
            List<String> missingSurfaces) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Confirm reviewer guidance is unavailable before citing decision replay evidence.";
        }
        if (!missingSurfaces.isEmpty()) {
            return "Review missing reviewer evidence surfaces before citing the lab explanation.";
        }
        if (partialLaneCount > 0 || unknownLaneCount > 0) {
            return "Review partial or unknown evidence lanes before citing the lab explanation.";
        }
        return "Inspect reviewer snapshot and consistency surfaces as informational lab evidence.";
    }

    private static List<String> suggestedReviewSteps(
            String status,
            int checkedSurfaceCount,
            boolean unavailableDecision) {
        if (STATUS_UNKNOWN.equals(status) && checkedSurfaceCount == 0) {
            return List.of("Run routing comparison before using reviewer guidance.");
        }
        if (STATUS_UNKNOWN.equals(status) || unavailableDecision) {
            return List.of(
                    "Confirm no selected candidate evidence is available for reviewer guidance.",
                    "Keep no-healthy evidence boundaries attached to any reviewer note.");
        }
        return List.of(
                "Inspect Decision Replay Evidence Reviewer Snapshot warnings and highlights.",
                "Compare lane consistency, dependency summary, and reference index counts.",
                "Review partial or unknown lane statuses before citing reviewer evidence.",
                "Keep limitations with any reviewer-facing explanation.");
    }

    private static List<String> evidenceSurfacesToInspect(List<String> missingSurfaces) {
        return EXPECTED_SURFACES.stream()
                .filter(surface -> !missingSurfaces.contains(surface))
                .toList();
    }

    private static List<String> cautionNotes(
            boolean unavailableDecision,
            List<String> missingSurfaces,
            String reviewerSnapshotStatus,
            String consistencyStatus,
            int partialLaneCount,
            int unknownLaneCount) {
        List<String> notes = new ArrayList<>();
        if (unavailableDecision) {
            notes.add("No selected candidate evidence is available for reviewer guidance.");
        }
        missingSurfaces.forEach(surface -> notes.add("Missing surface: " + surface));
        if (!STATUS_AVAILABLE.equals(reviewerSnapshotStatus)) {
            notes.add("Reviewer snapshot status is " + reviewerSnapshotStatus + ".");
        }
        if (!STATUS_CONSISTENT.equals(consistencyStatus)) {
            notes.add("Consistency summary reports " + consistencyStatus + ".");
        }
        if (partialLaneCount > 0) {
            notes.add(partialLaneCount + " evidence lanes remain PARTIAL.");
        }
        if (unknownLaneCount > 0) {
            notes.add(unknownLaneCount + " evidence lanes remain UNKNOWN.");
        }
        notes.add("Guidance is read-only reviewer metadata, not replay proof or scoring proof.");
        return List.copyOf(notes);
    }

    private static String summaryText(
            String status,
            String reviewerPriority,
            String reviewerSnapshotStatus,
            int checkedSurfaceCount,
            List<String> missingSurfaces) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Decision replay evidence reviewer guidance is UNKNOWN because required reviewer metadata "
                    + "is missing or no selected candidate evidence is available.";
        }
        return "Reviewer guidance is " + status + " with " + reviewerPriority + " priority, "
                + checkedSurfaceCount + " checked surfaces, " + missingSurfaces.size()
                + " missing surfaces, and reviewer snapshot status " + reviewerSnapshotStatus + ".";
    }

    private static int laneCount(
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
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
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
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
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
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
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
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
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return firstNonBlank(
                reviewerSnapshot == null ? null : reviewerSnapshot.selectedCandidateId(),
                laneConsistencySummary == null ? null : laneConsistencySummary.selectedCandidateId(),
                laneReferenceIndex == null ? null : laneReferenceIndex.selectedCandidateId(),
                laneDependencySummary == null ? null : laneDependencySummary.selectedCandidateId(),
                laneDependencyMap == null ? null : laneDependencyMap.selectedCandidateId(),
                statusRollup == null ? null : statusRollup.selectedCandidateId());
    }

    private static int candidateCount(
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return firstPositive(
                reviewerSnapshot == null ? 0 : reviewerSnapshot.candidateCount(),
                laneConsistencySummary == null ? 0 : laneConsistencySummary.candidateCount(),
                laneReferenceIndex == null ? 0 : laneReferenceIndex.candidateCount(),
                laneDependencySummary == null ? 0 : laneDependencySummary.candidateCount(),
                laneDependencyMap == null ? 0 : laneDependencyMap.candidateCount(),
                statusRollup == null ? 0 : statusRollup.candidateCount());
    }

    private static String strategyId(
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return safeValue(firstNonBlank(
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
