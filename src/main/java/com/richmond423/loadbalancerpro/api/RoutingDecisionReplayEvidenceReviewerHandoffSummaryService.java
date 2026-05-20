package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RoutingDecisionReplayEvidenceReviewerHandoffSummaryService {
    private static final String SCHEMA_VERSION = "decision-replay-evidence-reviewer-handoff-summary/v1";
    private static final String SOURCE =
            "Derived only from already-built results[].decisionReplayEvidenceStatusRollup, "
                    + "results[].decisionReplayEvidenceLaneDependencyMap, "
                    + "results[].decisionReplayEvidenceLaneReferenceIndex, "
                    + "results[].decisionReplayEvidenceLaneDependencySummary, "
                    + "results[].decisionReplayEvidenceLaneConsistencySummary, "
                    + "results[].decisionReplayEvidenceReviewerSnapshot, and "
                    + "results[].decisionReplayEvidenceReviewerGuidance metadata.";
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
            "decisionReplayEvidenceReviewerSnapshot",
            "decisionReplayEvidenceReviewerGuidance");
    private static final List<String> LIMITATIONS = List.of(
            "Read-only reviewer handoff metadata derived only from existing decision replay reviewer metadata.",
            "Not replay proof and does not execute replay.",
            "Not scoring proof and does not recompute scores.",
            "Not correctness validation, not production readiness, not production certification, "
                    + "not guaranteed replay, and not production validation.");
    private static final String BOUNDARY_NOTE =
            "Decision Replay Evidence Reviewer Handoff Summary is read-only lab reviewer metadata derived only "
                    + "from already-built status rollup, lane dependency map, lane reference index, lane dependency "
                    + "summary, lane consistency summary, reviewer snapshot, and reviewer guidance response surfaces; "
                    + "it does not inspect raw server input, does not inspect raw request payloads, does not execute "
                    + "replay, does not perform what-if mutation, does not persist reviewer-handoff data or audit logs, "
                    + "does not generate a new fingerprint, does not use reflective field inspection, does not "
                    + "recompute scores, does not infer hidden scoring, does not retune weights, "
                    + "does not change routing behavior, does not add telemetry, does not add external calls, does not add "
                    + "upload/share/download flows, and does not add server-side export/PDF/ZIP generation.";

    public RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse reviewerHandoffSummary(
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance) {
        List<String> missingSurfaces = missingSurfaces(
                statusRollup,
                laneDependencyMap,
                laneReferenceIndex,
                laneDependencySummary,
                laneConsistencySummary,
                reviewerSnapshot,
                reviewerGuidance);
        int checkedSurfaceCount = EXPECTED_SURFACES.size() - missingSurfaces.size();
        String reviewerSnapshotStatus = normalizeLaneStatus(reviewerSnapshot == null ? null : reviewerSnapshot.status());
        String reviewerGuidanceStatus = normalizeLaneStatus(
                reviewerGuidance == null ? null : reviewerGuidance.status());
        String consistencyStatus = normalizeConsistencyStatus(
                laneConsistencySummary == null ? null : laneConsistencySummary.status());
        String selectedCandidateId = selectedCandidateId(
                reviewerGuidance,
                reviewerSnapshot,
                laneConsistencySummary,
                laneReferenceIndex,
                laneDependencySummary,
                laneDependencyMap,
                statusRollup);
        int candidateCount = candidateCount(
                reviewerGuidance,
                reviewerSnapshot,
                laneConsistencySummary,
                laneReferenceIndex,
                laneDependencySummary,
                laneDependencyMap,
                statusRollup);
        boolean unavailableDecision = STATUS_UNKNOWN.equals(reviewerGuidanceStatus)
                && STATUS_UNKNOWN.equals(reviewerSnapshotStatus)
                && isBlank(selectedCandidateId)
                && candidateCount == 0;
        int totalLaneCount = unavailableDecision
                ? 0
                : laneCount(reviewerGuidance, reviewerSnapshot, laneConsistencySummary, laneDependencySummary,
                        laneReferenceIndex);
        int availableLaneCount = unavailableDecision
                ? 0
                : availableLaneCount(reviewerGuidance, reviewerSnapshot, laneConsistencySummary,
                        laneDependencySummary, laneReferenceIndex);
        int partialLaneCount = unavailableDecision
                ? 0
                : partialLaneCount(reviewerGuidance, reviewerSnapshot, laneConsistencySummary,
                        laneDependencySummary, laneReferenceIndex);
        int unknownLaneCount = unavailableDecision
                ? 0
                : unknownLaneCount(reviewerGuidance, reviewerSnapshot, laneConsistencySummary,
                        laneDependencySummary, laneReferenceIndex);
        String status = status(
                unavailableDecision,
                missingSurfaces,
                checkedSurfaceCount,
                reviewerSnapshotStatus,
                reviewerGuidanceStatus,
                consistencyStatus,
                availableLaneCount,
                partialLaneCount,
                unknownLaneCount);
        String handoffPriority = handoffPriority(
                status,
                missingSurfaces,
                reviewerSnapshotStatus,
                reviewerGuidanceStatus,
                consistencyStatus,
                partialLaneCount,
                unknownLaneCount);
        List<String> referencedSurfaces = evidenceSurfacesReferenced(missingSurfaces);
        List<String> bullets = handoffBullets(
                status,
                reviewerSnapshotStatus,
                reviewerGuidanceStatus,
                consistencyStatus,
                totalLaneCount,
                availableLaneCount,
                partialLaneCount,
                unknownLaneCount,
                checkedSurfaceCount);
        List<String> followUpItems = operatorFollowUpItems(status, unavailableDecision, missingSurfaces,
                partialLaneCount, unknownLaneCount);
        List<String> cautionNotes = cautionNotes(
                unavailableDecision,
                missingSurfaces,
                reviewerSnapshotStatus,
                reviewerGuidanceStatus,
                consistencyStatus,
                partialLaneCount,
                unknownLaneCount);

        return new RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse(
                true,
                SCHEMA_VERSION,
                SOURCE,
                status,
                handoffPriority,
                reviewerSnapshotStatus,
                reviewerGuidanceStatus,
                consistencyStatus,
                strategyId(reviewerGuidance, reviewerSnapshot, laneConsistencySummary, laneReferenceIndex,
                        laneDependencySummary, laneDependencyMap, statusRollup),
                isBlank(selectedCandidateId) ? null : selectedCandidateId,
                candidateCount,
                totalLaneCount,
                availableLaneCount,
                partialLaneCount,
                unknownLaneCount,
                bullets,
                followUpItems,
                referencedSurfaces,
                cautionNotes,
                summaryText(status, handoffPriority, reviewerSnapshotStatus, reviewerGuidanceStatus,
                        consistencyStatus, totalLaneCount, availableLaneCount, partialLaneCount, unknownLaneCount),
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
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance) {
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
        return List.copyOf(missing);
    }

    private static String status(
            boolean unavailableDecision,
            List<String> missingSurfaces,
            int checkedSurfaceCount,
            String reviewerSnapshotStatus,
            String reviewerGuidanceStatus,
            String consistencyStatus,
            int availableLaneCount,
            int partialLaneCount,
            int unknownLaneCount) {
        if (unavailableDecision || checkedSurfaceCount == 0
                || missingSurfaces.contains("decisionReplayEvidenceReviewerSnapshot")
                || missingSurfaces.contains("decisionReplayEvidenceReviewerGuidance")
                || missingSurfaces.contains("decisionReplayEvidenceLaneConsistencySummary")) {
            return STATUS_UNKNOWN;
        }
        boolean allLaneCountsAvailable = availableLaneCount > 0 && partialLaneCount == 0 && unknownLaneCount == 0;
        return missingSurfaces.isEmpty()
                && allLaneCountsAvailable
                && STATUS_AVAILABLE.equals(reviewerSnapshotStatus)
                && STATUS_AVAILABLE.equals(reviewerGuidanceStatus)
                && STATUS_CONSISTENT.equals(consistencyStatus)
                        ? STATUS_AVAILABLE
                        : STATUS_PARTIAL;
    }

    private static String handoffPriority(
            String status,
            List<String> missingSurfaces,
            String reviewerSnapshotStatus,
            String reviewerGuidanceStatus,
            String consistencyStatus,
            int partialLaneCount,
            int unknownLaneCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return PRIORITY_UNKNOWN;
        }
        return STATUS_PARTIAL.equals(status)
                || !missingSurfaces.isEmpty()
                || !STATUS_AVAILABLE.equals(reviewerSnapshotStatus)
                || !STATUS_AVAILABLE.equals(reviewerGuidanceStatus)
                || !STATUS_CONSISTENT.equals(consistencyStatus)
                || partialLaneCount > 0
                || unknownLaneCount > 0
                        ? PRIORITY_REVIEW
                        : PRIORITY_INFORMATIONAL;
    }

    private static List<String> handoffBullets(
            String status,
            String reviewerSnapshotStatus,
            String reviewerGuidanceStatus,
            String consistencyStatus,
            int totalLaneCount,
            int availableLaneCount,
            int partialLaneCount,
            int unknownLaneCount,
            int checkedSurfaceCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return checkedSurfaceCount == 0
                    ? List.of("0 reviewer handoff surfaces checked")
                    : List.of(
                            checkedSurfaceCount + " reviewer handoff surfaces checked",
                            "Reviewer handoff summary status is UNKNOWN");
        }
        return List.of(
                "Reviewer snapshot is " + reviewerSnapshotStatus + ".",
                "Reviewer guidance is " + reviewerGuidanceStatus + ".",
                "Consistency summary is " + consistencyStatus + ".",
                totalLaneCount + " evidence lanes summarized: " + availableLaneCount + " available, "
                        + partialLaneCount + " partial, " + unknownLaneCount + " unknown.",
                checkedSurfaceCount + " handoff surfaces referenced.");
    }

    private static List<String> operatorFollowUpItems(
            String status,
            boolean unavailableDecision,
            List<String> missingSurfaces,
            int partialLaneCount,
            int unknownLaneCount) {
        if (STATUS_UNKNOWN.equals(status) && !missingSurfaces.isEmpty()) {
            return List.of("Run routing comparison before using reviewer handoff metadata.");
        }
        if (STATUS_UNKNOWN.equals(status) && unavailableDecision) {
            return List.of(
                    "Confirm no selected candidate evidence is available before using reviewer handoff notes.",
                    "Keep no-healthy evidence boundaries attached to any operator note.");
        }
        if (STATUS_UNKNOWN.equals(status)) {
            return List.of("Run routing comparison before using reviewer handoff metadata.");
        }
        List<String> items = new ArrayList<>();
        if (!missingSurfaces.isEmpty()) {
            items.add("Review missing reviewer handoff surfaces before operator handoff.");
        }
        if (partialLaneCount > 0 || unknownLaneCount > 0) {
            items.add("Review partial or unknown evidence lanes before operator handoff.");
        }
        items.add("Keep lab-only limitations attached to the handoff.");
        return List.copyOf(items);
    }

    private static List<String> evidenceSurfacesReferenced(List<String> missingSurfaces) {
        return EXPECTED_SURFACES.stream()
                .filter(surface -> !missingSurfaces.contains(surface))
                .toList();
    }

    private static List<String> cautionNotes(
            boolean unavailableDecision,
            List<String> missingSurfaces,
            String reviewerSnapshotStatus,
            String reviewerGuidanceStatus,
            String consistencyStatus,
            int partialLaneCount,
            int unknownLaneCount) {
        List<String> notes = new ArrayList<>();
        if (unavailableDecision) {
            notes.add("No selected candidate evidence is available for reviewer handoff.");
        }
        missingSurfaces.forEach(surface -> notes.add("Missing surface: " + surface));
        if (!STATUS_AVAILABLE.equals(reviewerSnapshotStatus)) {
            notes.add("Reviewer snapshot status is " + reviewerSnapshotStatus + ".");
        }
        if (!STATUS_AVAILABLE.equals(reviewerGuidanceStatus)) {
            notes.add("Reviewer guidance status is " + reviewerGuidanceStatus + ".");
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
        notes.add("Handoff summary is read-only reviewer metadata, not replay proof, scoring proof, "
                + "correctness validation, production readiness, or production validation.");
        return List.copyOf(notes);
    }

    private static String summaryText(
            String status,
            String handoffPriority,
            String reviewerSnapshotStatus,
            String reviewerGuidanceStatus,
            String consistencyStatus,
            int totalLaneCount,
            int availableLaneCount,
            int partialLaneCount,
            int unknownLaneCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Decision replay evidence reviewer handoff summary is UNKNOWN because required reviewer "
                    + "metadata is missing or no selected candidate evidence is available.";
        }
        return "Reviewer handoff summary is " + status + " with " + handoffPriority + " priority, "
                + "snapshot status " + reviewerSnapshotStatus + ", guidance status " + reviewerGuidanceStatus
                + ", consistency status " + consistencyStatus + ", and " + totalLaneCount + " lanes ("
                + availableLaneCount + " available, " + partialLaneCount + " partial, " + unknownLaneCount
                + " unknown).";
    }

    private static int laneCount(
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
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
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
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
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
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
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
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
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return firstNonBlank(
                reviewerGuidance == null ? null : reviewerGuidance.selectedCandidateId(),
                reviewerSnapshot == null ? null : reviewerSnapshot.selectedCandidateId(),
                laneConsistencySummary == null ? null : laneConsistencySummary.selectedCandidateId(),
                laneReferenceIndex == null ? null : laneReferenceIndex.selectedCandidateId(),
                laneDependencySummary == null ? null : laneDependencySummary.selectedCandidateId(),
                laneDependencyMap == null ? null : laneDependencyMap.selectedCandidateId(),
                statusRollup == null ? null : statusRollup.selectedCandidateId());
    }

    private static int candidateCount(
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return firstPositive(
                reviewerGuidance == null ? 0 : reviewerGuidance.candidateCount(),
                reviewerSnapshot == null ? 0 : reviewerSnapshot.candidateCount(),
                laneConsistencySummary == null ? 0 : laneConsistencySummary.candidateCount(),
                laneReferenceIndex == null ? 0 : laneReferenceIndex.candidateCount(),
                laneDependencySummary == null ? 0 : laneDependencySummary.candidateCount(),
                laneDependencyMap == null ? 0 : laneDependencyMap.candidateCount(),
                statusRollup == null ? 0 : statusRollup.candidateCount());
    }

    private static String strategyId(
            RoutingDecisionReplayEvidenceReviewerGuidanceResponse reviewerGuidance,
            RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return safeValue(firstNonBlank(
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
