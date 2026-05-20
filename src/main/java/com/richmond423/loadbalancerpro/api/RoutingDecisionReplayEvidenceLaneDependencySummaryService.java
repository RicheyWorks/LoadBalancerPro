package com.richmond423.loadbalancerpro.api;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class RoutingDecisionReplayEvidenceLaneDependencySummaryService {
    private static final String SCHEMA_VERSION = "decision-replay-evidence-lane-dependency-summary/v1";
    private static final String SOURCE =
            "Derived only from results[].decisionReplayEvidenceLaneReferenceIndex reviewer metadata.";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Decision Replay Evidence Lane Dependency Summary is read-only lab reviewer metadata derived only "
                    + "from the already-built lane reference index; it does not inspect raw server input, does "
                    + "not inspect raw request payloads, does not execute replay, does not perform what-if "
                    + "mutation, does not persist lane-dependency-summary data or audit logs, does not generate "
                    + "a new fingerprint, does not use reflective field inspection, does not recompute scores, "
                    + "does not infer hidden scoring, does not retune weights, does not change routing behavior, "
                    + "does not add telemetry, does not add external calls, does not add upload/share/download "
                    + "flows, and does not add server-side export/PDF/ZIP generation.";
    private static final List<String> LIMITATIONS = List.of(
            "Read-only reviewer metadata derived only from the existing lane reference index.",
            "Does not execute replay or perform what-if mutation.",
            "Does not recompute scores and is not scoring proof.",
            "Not correctness validation and not production readiness.");

    public RoutingDecisionReplayEvidenceLaneDependencySummaryResponse dependencySummary(
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse referenceIndex) {
        if (referenceIndex == null) {
            return new RoutingDecisionReplayEvidenceLaneDependencySummaryResponse(
                    true,
                    SCHEMA_VERSION,
                    SOURCE,
                    STATUS_UNKNOWN,
                    "UNKNOWN",
                    null,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    List.of(),
                    List.of(),
                    "Reference index is UNKNOWN with 0 lanes, 0 available, 0 partial, and 0 unknown.",
                    LIMITATIONS,
                    BOUNDARY_NOTE);
        }

        List<DecisionReplayEvidenceLaneReferenceIndexItemResponse> items =
                referenceIndex.referenceItems() == null ? List.of() : referenceIndex.referenceItems();
        int totalLaneCount = items.size();
        int availableLaneCount = Math.max(0, referenceIndex.availableLaneCount());
        int partialLaneCount = Math.max(0, referenceIndex.partialLaneCount());
        int unknownLaneCount = Math.max(0, referenceIndex.unknownLaneCount());
        int rootLaneCount = (int) items.stream()
                .filter(item -> item.dependencyCount() == 0)
                .count();
        int terminalLaneCount = (int) items.stream()
                .filter(item -> item.downstreamCount() == 0)
                .count();
        int maxDependencyCount = items.stream()
                .map(DecisionReplayEvidenceLaneReferenceIndexItemResponse::dependencyCount)
                .max(Comparator.naturalOrder())
                .orElse(0);
        int maxDownstreamCount = items.stream()
                .map(DecisionReplayEvidenceLaneReferenceIndexItemResponse::downstreamCount)
                .max(Comparator.naturalOrder())
                .orElse(0);

        return new RoutingDecisionReplayEvidenceLaneDependencySummaryResponse(
                true,
                SCHEMA_VERSION,
                SOURCE,
                normalizeStatus(referenceIndex.status()),
                safeValue(referenceIndex.strategyId()),
                isBlank(referenceIndex.selectedCandidateId()) ? null : referenceIndex.selectedCandidateId(),
                Math.max(0, referenceIndex.candidateCount()),
                totalLaneCount,
                availableLaneCount,
                partialLaneCount,
                unknownLaneCount,
                rootLaneCount,
                terminalLaneCount,
                maxDependencyCount,
                maxDownstreamCount,
                laneIdsWithDependencyCount(items, maxDependencyCount),
                laneIdsWithDownstreamCount(items, maxDownstreamCount),
                summaryText(referenceIndex, totalLaneCount, availableLaneCount, partialLaneCount, unknownLaneCount),
                LIMITATIONS,
                BOUNDARY_NOTE);
    }

    private static List<String> laneIdsWithDependencyCount(
            List<DecisionReplayEvidenceLaneReferenceIndexItemResponse> items,
            int maxDependencyCount) {
        if (items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item.dependencyCount() == maxDependencyCount)
                .map(DecisionReplayEvidenceLaneReferenceIndexItemResponse::laneId)
                .toList();
    }

    private static List<String> laneIdsWithDownstreamCount(
            List<DecisionReplayEvidenceLaneReferenceIndexItemResponse> items,
            int maxDownstreamCount) {
        if (items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item.downstreamCount() == maxDownstreamCount)
                .map(DecisionReplayEvidenceLaneReferenceIndexItemResponse::laneId)
                .toList();
    }

    private static String summaryText(
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse referenceIndex,
            int totalLaneCount,
            int availableLaneCount,
            int partialLaneCount,
            int unknownLaneCount) {
        return "Reference index is " + normalizeStatus(referenceIndex.status())
                + " with " + totalLaneCount
                + " lanes, " + availableLaneCount
                + " available, " + partialLaneCount
                + " partial, and " + unknownLaneCount
                + " unknown.";
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_UNKNOWN;
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case STATUS_AVAILABLE -> STATUS_AVAILABLE;
            case STATUS_PARTIAL -> STATUS_PARTIAL;
            default -> STATUS_UNKNOWN;
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safeValue(String value) {
        return isBlank(value) ? "UNKNOWN" : value.trim();
    }
}
