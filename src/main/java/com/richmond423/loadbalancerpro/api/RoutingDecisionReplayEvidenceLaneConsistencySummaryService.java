package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RoutingDecisionReplayEvidenceLaneConsistencySummaryService {
    private static final String SCHEMA_VERSION = "decision-replay-evidence-lane-consistency-summary/v1";
    private static final String SOURCE =
            "Derived only from already-built results[].decisionReplayEvidenceStatusRollup, "
                    + "results[].decisionReplayEvidenceLaneDependencyMap, "
                    + "results[].decisionReplayEvidenceLaneReferenceIndex, and "
                    + "results[].decisionReplayEvidenceLaneDependencySummary metadata.";
    private static final String STATUS_CONSISTENT = "CONSISTENT";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String CHECK_PASS = "PASS";
    private static final String CHECK_WARN = "WARN";
    private static final String CHECK_UNKNOWN = "UNKNOWN";
    private static final String LANE_STATUS_AVAILABLE = "AVAILABLE";
    private static final String LANE_STATUS_PARTIAL = "PARTIAL";
    private static final String BOUNDARY_NOTE =
            "Decision Replay Evidence Lane Consistency Summary is read-only lab reviewer metadata derived only "
                    + "from already-built lane status, dependency-map, reference-index, and dependency-summary "
                    + "response surfaces; it does not inspect raw server input, does not inspect raw request "
                    + "payloads, does not execute replay, does not perform what-if mutation, does not persist "
                    + "lane-consistency-summary data or audit logs, does not generate a new fingerprint, does "
                    + "not use reflective field inspection, does not recompute scores, does not infer hidden "
                    + "scoring, does not retune weights, does not change routing behavior, does not add "
                    + "telemetry, does not add external calls, does not add upload/share/download flows, and "
                    + "does not add server-side export/PDF/ZIP generation.";
    private static final List<String> LIMITATIONS = List.of(
            "Read-only reviewer consistency metadata derived only from existing lane surfaces.",
            "Does not execute replay or perform what-if mutation.",
            "Does not recompute scores and is not scoring proof.",
            "Not correctness validation, not production readiness, and not guaranteed replay.");

    public RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse consistencySummary(
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary) {
        List<String> missingSurfaces = missingSurfaces(
                statusRollup,
                laneDependencyMap,
                laneReferenceIndex,
                laneDependencySummary);
        int referenceIndexLaneCount = laneReferenceIndex == null || laneReferenceIndex.referenceItems() == null
                ? 0
                : laneReferenceIndex.referenceItems().size();
        int dependencySummaryLaneCount = laneDependencySummary == null
                ? 0
                : Math.max(0, laneDependencySummary.totalLaneCount());
        int dependencyMapLaneCount = laneDependencyMap == null || laneDependencyMap.dependencyItems() == null
                ? 0
                : laneDependencyMap.dependencyItems().size();
        int totalLaneCount = laneReferenceIndex == null
                ? dependencySummaryLaneCount
                : referenceIndexLaneCount;
        int availableLaneCount = laneReferenceIndex == null
                ? safeCount(laneDependencySummary == null ? 0 : laneDependencySummary.availableLaneCount())
                : safeCount(laneReferenceIndex.availableLaneCount());
        int partialLaneCount = laneReferenceIndex == null
                ? safeCount(laneDependencySummary == null ? 0 : laneDependencySummary.partialLaneCount())
                : safeCount(laneReferenceIndex.partialLaneCount());
        int unknownLaneCount = laneReferenceIndex == null
                ? safeCount(laneDependencySummary == null ? 0 : laneDependencySummary.unknownLaneCount())
                : safeCount(laneReferenceIndex.unknownLaneCount());
        String referenceIndexStatus = normalizeLaneStatus(laneReferenceIndex == null ? null : laneReferenceIndex.status());
        String dependencySummaryStatus = normalizeLaneStatus(
                laneDependencySummary == null ? null : laneDependencySummary.status());
        String statusRollupStatus = normalizeLaneStatus(statusRollup == null ? null : statusRollup.status());
        String dependencyMapStatus = normalizeLaneStatus(laneDependencyMap == null ? null : laneDependencyMap.status());
        String selectedCandidateId = selectedCandidateId(
                laneReferenceIndex,
                laneDependencySummary,
                laneDependencyMap,
                statusRollup);
        int candidateCount = candidateCount(laneReferenceIndex, laneDependencySummary, laneDependencyMap, statusRollup);
        List<String> mismatchedCountFields = mismatchedCountFields(laneReferenceIndex, laneDependencySummary);
        List<DecisionReplayEvidenceLaneConsistencyCheckResponse> checks = consistencyChecks(
                statusRollup,
                laneDependencyMap,
                laneReferenceIndex,
                laneDependencySummary,
                referenceIndexStatus,
                dependencySummaryStatus,
                statusRollupStatus,
                dependencyMapStatus,
                referenceIndexLaneCount,
                dependencySummaryLaneCount,
                dependencyMapLaneCount);
        String status = summaryStatus(
                missingSurfaces,
                mismatchedCountFields,
                checks,
                referenceIndexStatus,
                dependencySummaryStatus,
                selectedCandidateId,
                candidateCount);

        return new RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse(
                true,
                SCHEMA_VERSION,
                SOURCE,
                status,
                referenceIndexStatus,
                dependencySummaryStatus,
                statusRollupStatus,
                dependencyMapStatus,
                strategyId(laneReferenceIndex, laneDependencySummary, laneDependencyMap, statusRollup),
                isBlank(selectedCandidateId) ? null : selectedCandidateId,
                candidateCount,
                totalLaneCount,
                availableLaneCount,
                partialLaneCount,
                unknownLaneCount,
                dependencyMapLaneCount,
                referenceIndexLaneCount,
                dependencySummaryLaneCount,
                mismatchedCountFields,
                missingSurfaces,
                checks,
                summaryText(status, totalLaneCount, availableLaneCount, partialLaneCount, unknownLaneCount,
                        mismatchedCountFields, missingSurfaces),
                LIMITATIONS,
                BOUNDARY_NOTE);
    }

    private static List<String> missingSurfaces(
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary) {
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
        return List.copyOf(missing);
    }

    private static List<String> mismatchedCountFields(
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary) {
        if (laneReferenceIndex == null || laneDependencySummary == null) {
            return List.of();
        }
        List<String> mismatches = new ArrayList<>();
        int referenceIndexLaneCount = laneReferenceIndex.referenceItems() == null
                ? 0
                : laneReferenceIndex.referenceItems().size();
        addMismatch(mismatches, "totalLaneCount", referenceIndexLaneCount,
                safeCount(laneDependencySummary.totalLaneCount()));
        addMismatch(mismatches, "availableLaneCount", safeCount(laneReferenceIndex.availableLaneCount()),
                safeCount(laneDependencySummary.availableLaneCount()));
        addMismatch(mismatches, "partialLaneCount", safeCount(laneReferenceIndex.partialLaneCount()),
                safeCount(laneDependencySummary.partialLaneCount()));
        addMismatch(mismatches, "unknownLaneCount", safeCount(laneReferenceIndex.unknownLaneCount()),
                safeCount(laneDependencySummary.unknownLaneCount()));
        return List.copyOf(mismatches);
    }

    private static List<DecisionReplayEvidenceLaneConsistencyCheckResponse> consistencyChecks(
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            String referenceIndexStatus,
            String dependencySummaryStatus,
            String statusRollupStatus,
            String dependencyMapStatus,
            int referenceIndexLaneCount,
            int dependencySummaryLaneCount,
            int dependencyMapLaneCount) {
        List<DecisionReplayEvidenceLaneConsistencyCheckResponse> checks = new ArrayList<>();
        checks.add(surfaceCheck("status-rollup-present", "decisionReplayEvidenceStatusRollup", statusRollup != null,
                statusRollup == null ? "missing" : statusRollupStatus));
        checks.add(surfaceCheck("dependency-map-present", "decisionReplayEvidenceLaneDependencyMap",
                laneDependencyMap != null, laneDependencyMap == null ? "missing" : dependencyMapStatus));
        checks.add(surfaceCheck("reference-index-present", "decisionReplayEvidenceLaneReferenceIndex",
                laneReferenceIndex != null, laneReferenceIndex == null ? "missing" : referenceIndexStatus));
        checks.add(surfaceCheck("dependency-summary-present", "decisionReplayEvidenceLaneDependencySummary",
                laneDependencySummary != null, laneDependencySummary == null ? "missing" : dependencySummaryStatus));
        checks.add(countCheck("lane-count-alignment", referenceIndexLaneCount, dependencySummaryLaneCount));
        checks.add(countCheck(
                "available-count-alignment",
                laneReferenceIndex == null ? 0 : laneReferenceIndex.availableLaneCount(),
                laneDependencySummary == null ? 0 : laneDependencySummary.availableLaneCount()));
        checks.add(countCheck(
                "partial-count-alignment",
                laneReferenceIndex == null ? 0 : laneReferenceIndex.partialLaneCount(),
                laneDependencySummary == null ? 0 : laneDependencySummary.partialLaneCount()));
        checks.add(countCheck(
                "unknown-count-alignment",
                laneReferenceIndex == null ? 0 : laneReferenceIndex.unknownLaneCount(),
                laneDependencySummary == null ? 0 : laneDependencySummary.unknownLaneCount()));
        checks.add(statusCheck(referenceIndexStatus, dependencySummaryStatus, statusRollupStatus, dependencyMapStatus));
        checks.add(new DecisionReplayEvidenceLaneConsistencyCheckResponse(
                "dependency-map-context-count",
                laneDependencyMap == null ? CHECK_UNKNOWN : CHECK_PASS,
                "dependency map surface present as source context",
                laneDependencyMap == null ? "missing" : dependencyMapLaneCount + " dependency items",
                "Dependency map item count is reported as existing source metadata and is not treated as the "
                        + "reference-index total lane count."));
        return List.copyOf(checks);
    }

    private static DecisionReplayEvidenceLaneConsistencyCheckResponse surfaceCheck(
            String name,
            String expected,
            boolean present,
            String actual) {
        return new DecisionReplayEvidenceLaneConsistencyCheckResponse(
                name,
                present ? CHECK_PASS : CHECK_UNKNOWN,
                expected + " present",
                actual,
                present ? "Expected surface is present." : "Expected surface is missing or null.");
    }

    private static DecisionReplayEvidenceLaneConsistencyCheckResponse countCheck(
            String name,
            int referenceValue,
            int summaryValue) {
        int safeReference = safeCount(referenceValue);
        int safeSummary = safeCount(summaryValue);
        boolean matches = safeReference == safeSummary;
        return new DecisionReplayEvidenceLaneConsistencyCheckResponse(
                name,
                matches ? CHECK_PASS : CHECK_WARN,
                "reference index " + safeReference,
                "dependency summary " + safeSummary,
                matches
                        ? "Reference index and dependency summary count surfaces agree."
                        : "Reference index and dependency summary count surfaces differ.");
    }

    private static DecisionReplayEvidenceLaneConsistencyCheckResponse statusCheck(
            String referenceIndexStatus,
            String dependencySummaryStatus,
            String statusRollupStatus,
            String dependencyMapStatus) {
        boolean matches = referenceIndexStatus.equals(dependencySummaryStatus)
                && referenceIndexStatus.equals(statusRollupStatus)
                && referenceIndexStatus.equals(dependencyMapStatus);
        return new DecisionReplayEvidenceLaneConsistencyCheckResponse(
                "lane-status-alignment",
                matches ? CHECK_PASS : CHECK_WARN,
                referenceIndexStatus,
                "referenceIndex=" + referenceIndexStatus
                        + "; dependencySummary=" + dependencySummaryStatus
                        + "; statusRollup=" + statusRollupStatus
                        + "; dependencyMap=" + dependencyMapStatus,
                matches
                        ? "Exposed lane metadata surfaces report the same normalized status."
                        : "One or more exposed lane metadata surfaces reports a different normalized status.");
    }

    private static String summaryStatus(
            List<String> missingSurfaces,
            List<String> mismatchedCountFields,
            List<DecisionReplayEvidenceLaneConsistencyCheckResponse> checks,
            String referenceIndexStatus,
            String dependencySummaryStatus,
            String selectedCandidateId,
            int candidateCount) {
        boolean unavailableDecision = STATUS_UNKNOWN.equals(referenceIndexStatus)
                && STATUS_UNKNOWN.equals(dependencySummaryStatus)
                && isBlank(selectedCandidateId)
                && candidateCount == 0;
        if (unavailableDecision || missingSurfaces.contains("decisionReplayEvidenceLaneReferenceIndex")
                || missingSurfaces.contains("decisionReplayEvidenceLaneDependencySummary")) {
            return STATUS_UNKNOWN;
        }
        boolean allPass = checks.stream().allMatch(check -> CHECK_PASS.equals(check.status()));
        return missingSurfaces.isEmpty() && mismatchedCountFields.isEmpty() && allPass
                ? STATUS_CONSISTENT
                : STATUS_PARTIAL;
    }

    private static String summaryText(
            String status,
            int totalLaneCount,
            int availableLaneCount,
            int partialLaneCount,
            int unknownLaneCount,
            List<String> mismatchedCountFields,
            List<String> missingSurfaces) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Lane evidence consistency summary is UNKNOWN because required lane metadata is missing or "
                    + "unavailable.";
        }
        if (mismatchedCountFields.isEmpty() && missingSurfaces.isEmpty()) {
            return "Lane evidence surfaces are consistent: reference index and dependency summary both report "
                    + totalLaneCount + " lanes with " + availableLaneCount + " available, "
                    + partialLaneCount + " partial, and " + unknownLaneCount + " unknown.";
        }
        return "Lane evidence surfaces are partially aligned: " + mismatchedCountFields.size()
                + " count field(s) differ and " + missingSurfaces.size()
                + " expected surface(s) are missing.";
    }

    private static String selectedCandidateId(
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return firstNonBlank(
                laneReferenceIndex == null ? null : laneReferenceIndex.selectedCandidateId(),
                laneDependencySummary == null ? null : laneDependencySummary.selectedCandidateId(),
                laneDependencyMap == null ? null : laneDependencyMap.selectedCandidateId(),
                statusRollup == null ? null : statusRollup.selectedCandidateId());
    }

    private static int candidateCount(
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return firstPositive(
                laneReferenceIndex == null ? 0 : laneReferenceIndex.candidateCount(),
                laneDependencySummary == null ? 0 : laneDependencySummary.candidateCount(),
                laneDependencyMap == null ? 0 : laneDependencyMap.candidateCount(),
                statusRollup == null ? 0 : statusRollup.candidateCount());
    }

    private static String strategyId(
            RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex,
            RoutingDecisionReplayEvidenceLaneDependencySummaryResponse laneDependencySummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return safeValue(firstNonBlank(
                laneReferenceIndex == null ? null : laneReferenceIndex.strategyId(),
                laneDependencySummary == null ? null : laneDependencySummary.strategyId(),
                laneDependencyMap == null ? null : laneDependencyMap.strategyId(),
                statusRollup == null ? null : statusRollup.strategyId()));
    }

    private static void addMismatch(List<String> mismatches, String fieldName, int expected, int actual) {
        if (safeCount(expected) != safeCount(actual)) {
            mismatches.add(fieldName);
        }
    }

    private static String normalizeLaneStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_UNKNOWN;
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case LANE_STATUS_AVAILABLE -> LANE_STATUS_AVAILABLE;
            case LANE_STATUS_PARTIAL -> LANE_STATUS_PARTIAL;
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
