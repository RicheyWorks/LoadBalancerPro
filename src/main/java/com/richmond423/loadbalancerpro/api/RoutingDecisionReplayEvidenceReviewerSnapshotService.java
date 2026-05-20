package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RoutingDecisionReplayEvidenceReviewerSnapshotService {
    private static final String SCHEMA_VERSION = "decision-replay-evidence-reviewer-snapshot/v1";
    private static final String SOURCE =
            "Derived only from already-built results[].decisionReplayEvidenceStatusRollup, "
                    + "results[].decisionReplayEvidenceLaneDependencyMap, "
                    + "results[].decisionReplayEvidenceLaneReferenceIndex, "
                    + "results[].decisionReplayEvidenceLaneDependencySummary, and "
                    + "results[].decisionReplayEvidenceLaneConsistencySummary metadata.";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String STATUS_CONSISTENT = "CONSISTENT";
    private static final List<String> EXPECTED_SURFACES = List.of(
            "decisionReplayEvidenceStatusRollup",
            "decisionReplayEvidenceLaneDependencyMap",
            "decisionReplayEvidenceLaneReferenceIndex",
            "decisionReplayEvidenceLaneDependencySummary",
            "decisionReplayEvidenceLaneConsistencySummary");
    private static final List<String> LIMITATIONS = List.of(
            "Read-only reviewer snapshot metadata derived only from existing decision replay evidence surfaces.",
            "Does not execute replay or perform what-if mutation.",
            "Does not recompute scores and is not scoring proof.",
            "Not correctness validation, not production readiness, not production certification, and not guaranteed replay.");
    private static final String BOUNDARY_NOTE =
            "Decision Replay Evidence Reviewer Snapshot is read-only lab reviewer metadata derived only from "
                    + "already-built status rollup, lane dependency map, lane reference index, lane dependency "
                    + "summary, and lane consistency summary response surfaces; it does not inspect raw server input, "
                    + "does not inspect raw request payloads, does not execute replay, does not perform "
                    + "what-if mutation, does not persist reviewer-snapshot data or audit logs, does not generate "
                    + "a new fingerprint, does not use reflective field inspection, does not recompute scores, "
                    + "does not infer hidden scoring, does not retune weights, does not change routing behavior, "
                    + "does not add telemetry, does not add external calls, does not add upload/share/download "
                    + "flows, and does not add server-side export/PDF/ZIP generation.";

    public RoutingDecisionReplayEvidenceReviewerSnapshotResponse reviewerSnapshot(
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary) {
        List<String> missingSurfaces = missingSurfaces(
                statusRollup,
                laneDependencyMap,
                laneReferenceIndex,
                laneDependencySummary,
                laneConsistencySummary);
        int checkedSurfaceCount = EXPECTED_SURFACES.size() - missingSurfaces.size();
        String consistencyStatus = normalizeConsistencyStatus(
                laneConsistencySummary == null ? null : laneConsistencySummary.status());
        String referenceIndexStatus = normalizeLaneStatus(laneReferenceIndex == null ? null : laneReferenceIndex.status());
        String dependencySummaryStatus = normalizeLaneStatus(
                laneDependencySummary == null ? null : laneDependencySummary.status());
        String statusRollupStatus = normalizeLaneStatus(statusRollup == null ? null : statusRollup.status());
        String dependencyMapStatus = normalizeLaneStatus(laneDependencyMap == null ? null : laneDependencyMap.status());
        String selectedCandidateId = selectedCandidateId(
                laneConsistencySummary,
                laneReferenceIndex,
                laneDependencySummary,
                laneDependencyMap,
                statusRollup);
        int candidateCount = candidateCount(
                laneConsistencySummary,
                laneReferenceIndex,
                laneDependencySummary,
                laneDependencyMap,
                statusRollup);
        boolean unavailableDecision = STATUS_UNKNOWN.equals(consistencyStatus)
                && STATUS_UNKNOWN.equals(referenceIndexStatus)
                && STATUS_UNKNOWN.equals(dependencySummaryStatus)
                && isBlank(selectedCandidateId)
                && candidateCount == 0;
        int totalLaneCount = unavailableDecision
                ? 0
                : laneCount(laneConsistencySummary, laneDependencySummary, laneReferenceIndex);
        int availableLaneCount = unavailableDecision
                ? 0
                : availableLaneCount(laneConsistencySummary, laneDependencySummary, laneReferenceIndex);
        int partialLaneCount = unavailableDecision
                ? 0
                : partialLaneCount(laneConsistencySummary, laneDependencySummary, laneReferenceIndex);
        int unknownLaneCount = unavailableDecision
                ? 0
                : unknownLaneCount(laneConsistencySummary, laneDependencySummary, laneReferenceIndex);
        String status = status(
                unavailableDecision,
                missingSurfaces,
                checkedSurfaceCount,
                consistencyStatus,
                availableLaneCount,
                partialLaneCount,
                unknownLaneCount);
        List<String> highlights = reviewerHighlights(
                status,
                consistencyStatus,
                totalLaneCount,
                availableLaneCount,
                partialLaneCount,
                unknownLaneCount,
                checkedSurfaceCount);
        List<String> warnings = reviewerWarnings(
                unavailableDecision,
                missingSurfaces,
                consistencyStatus,
                partialLaneCount,
                unknownLaneCount);

        return new RoutingDecisionReplayEvidenceReviewerSnapshotResponse(
                true,
                SCHEMA_VERSION,
                SOURCE,
                status,
                consistencyStatus,
                referenceIndexStatus,
                dependencySummaryStatus,
                statusRollupStatus,
                dependencyMapStatus,
                strategyId(laneConsistencySummary, laneReferenceIndex, laneDependencySummary, laneDependencyMap,
                        statusRollup),
                isBlank(selectedCandidateId) ? null : selectedCandidateId,
                candidateCount,
                totalLaneCount,
                availableLaneCount,
                partialLaneCount,
                unknownLaneCount,
                checkedSurfaceCount,
                missingSurfaces.size(),
                missingSurfaces,
                highlights,
                warnings,
                summaryText(status, consistencyStatus, totalLaneCount, availableLaneCount, partialLaneCount,
                        unknownLaneCount, missingSurfaces),
                LIMITATIONS,
                BOUNDARY_NOTE);
    }

    private static List<String> missingSurfaces(
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary) {
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
        return List.copyOf(missing);
    }

    private static String status(
            boolean unavailableDecision,
            List<String> missingSurfaces,
            int checkedSurfaceCount,
            String consistencyStatus,
            int availableLaneCount,
            int partialLaneCount,
            int unknownLaneCount) {
        if (unavailableDecision || checkedSurfaceCount == 0
                || missingSurfaces.contains("decisionReplayEvidenceLaneReferenceIndex")
                || missingSurfaces.contains("decisionReplayEvidenceLaneDependencySummary")
                || missingSurfaces.contains("decisionReplayEvidenceLaneConsistencySummary")) {
            return STATUS_UNKNOWN;
        }
        boolean allLaneCountsAvailable = availableLaneCount > 0 && partialLaneCount == 0 && unknownLaneCount == 0;
        return missingSurfaces.isEmpty() && allLaneCountsAvailable && STATUS_CONSISTENT.equals(consistencyStatus)
                ? STATUS_AVAILABLE
                : STATUS_PARTIAL;
    }

    private static List<String> reviewerHighlights(
            String status,
            String consistencyStatus,
            int totalLaneCount,
            int availableLaneCount,
            int partialLaneCount,
            int unknownLaneCount,
            int checkedSurfaceCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return checkedSurfaceCount == 0
                    ? List.of("0 reviewer evidence surfaces checked")
                    : List.of(checkedSurfaceCount + " reviewer evidence surfaces checked",
                            "Reviewer snapshot status is UNKNOWN");
        }
        return List.of(
                totalLaneCount + " evidence lanes summarized",
                availableLaneCount + " lanes available",
                partialLaneCount + " lanes partial",
                unknownLaneCount + " lanes unknown",
                checkedSurfaceCount + " reviewer evidence surfaces checked",
                "Consistency summary reports " + consistencyStatus);
    }

    private static List<String> reviewerWarnings(
            boolean unavailableDecision,
            List<String> missingSurfaces,
            String consistencyStatus,
            int partialLaneCount,
            int unknownLaneCount) {
        List<String> warnings = new ArrayList<>();
        if (unavailableDecision) {
            warnings.add("No selected candidate evidence is available for reviewer snapshot.");
        }
        missingSurfaces.forEach(surface -> warnings.add("Missing surface: " + surface));
        if (partialLaneCount > 0) {
            warnings.add(partialLaneCount + " lanes remain PARTIAL.");
        }
        if (unknownLaneCount > 0) {
            warnings.add(unknownLaneCount + " lanes remain UNKNOWN.");
        }
        if (!STATUS_CONSISTENT.equals(consistencyStatus)) {
            warnings.add("Consistency summary reports " + consistencyStatus + ".");
        }
        return List.copyOf(warnings);
    }

    private static String summaryText(
            String status,
            String consistencyStatus,
            int totalLaneCount,
            int availableLaneCount,
            int partialLaneCount,
            int unknownLaneCount,
            List<String> missingSurfaces) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Decision replay evidence reviewer snapshot is UNKNOWN because required reviewer evidence "
                    + "surfaces are missing or no selected candidate evidence is available.";
        }
        return "Reviewer snapshot is " + status + " with " + totalLaneCount + " lanes, "
                + availableLaneCount + " available, " + partialLaneCount + " partial, "
                + unknownLaneCount + " unknown, " + missingSurfaces.size() + " missing surface(s), and "
                + "consistency status " + consistencyStatus + ".";
    }

    private static int laneCount(
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
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
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
        if (laneConsistencySummary != null) {
            return safeCount(laneConsistencySummary.availableLaneCount());
        }
        if (laneDependencySummary != null) {
            return safeCount(laneDependencySummary.availableLaneCount());
        }
        return safeCount(laneReferenceIndex == null ? 0 : laneReferenceIndex.availableLaneCount());
    }

    private static int partialLaneCount(
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
        if (laneConsistencySummary != null) {
            return safeCount(laneConsistencySummary.partialLaneCount());
        }
        if (laneDependencySummary != null) {
            return safeCount(laneDependencySummary.partialLaneCount());
        }
        return safeCount(laneReferenceIndex == null ? 0 : laneReferenceIndex.partialLaneCount());
    }

    private static int unknownLaneCount(
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex) {
        if (laneConsistencySummary != null) {
            return safeCount(laneConsistencySummary.unknownLaneCount());
        }
        if (laneDependencySummary != null) {
            return safeCount(laneDependencySummary.unknownLaneCount());
        }
        return safeCount(laneReferenceIndex == null ? 0 : laneReferenceIndex.unknownLaneCount());
    }

    private static String selectedCandidateId(
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return firstNonBlank(
                laneConsistencySummary == null ? null : laneConsistencySummary.selectedCandidateId(),
                laneReferenceIndex == null ? null : laneReferenceIndex.selectedCandidateId(),
                laneDependencySummary == null ? null : laneDependencySummary.selectedCandidateId(),
                laneDependencyMap == null ? null : laneDependencyMap.selectedCandidateId(),
                statusRollup == null ? null : statusRollup.selectedCandidateId());
    }

    private static int candidateCount(
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return firstPositive(
                laneConsistencySummary == null ? 0 : laneConsistencySummary.candidateCount(),
                laneReferenceIndex == null ? 0 : laneReferenceIndex.candidateCount(),
                laneDependencySummary == null ? 0 : laneDependencySummary.candidateCount(),
                laneDependencyMap == null ? 0 : laneDependencyMap.candidateCount(),
                statusRollup == null ? 0 : statusRollup.candidateCount());
    }

    private static String strategyId(
            RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse laneConsistencySummary,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return safeValue(firstNonBlank(
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
