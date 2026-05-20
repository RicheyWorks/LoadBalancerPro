package com.richmond423.loadbalancerpro.api;

import java.util.List;

public final class RoutingDecisionReplayEvidenceReviewerClosureChecklistService {
    private static final String STATUS_COMPLETE = "COMPLETE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String ITEM_PASS = "PASS";
    private static final String ITEM_WARN = "WARN";
    private static final String ITEM_UNKNOWN = "UNKNOWN";
    private static final String DISPOSITION_COMPLETE_WITH_LIMITATIONS = "REVIEW_COMPLETE_WITH_LIMITATIONS";
    private static final List<String> NOT_PROVEN_BOUNDARIES = List.of(
            "not replay proof",
            "not scoring proof",
            "not correctness validation",
            "not production readiness",
            "not production certification",
            "not guaranteed replay",
            "not production validation");

    public RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse checklist(
            List<RoutingComparisonResultResponse> results,
            RoutingDecisionReplayEvidenceReviewerClosureRollupResponse decisionReplayEvidenceReviewerClosureRollup) {
        List<RoutingComparisonResultResponse> safeResults = results == null ? List.of() : results;
        ClosureCounts counts = counts(safeResults);
        boolean rollupPresent = decisionReplayEvidenceReviewerClosureRollup != null;
        boolean countsMatch = countsMatch(counts, decisionReplayEvidenceReviewerClosureRollup);
        boolean boundariesPresent = rollupPresent
                && NOT_PROVEN_BOUNDARIES.equals(decisionReplayEvidenceReviewerClosureRollup.notProvenBoundaries());

        List<RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse> items = List.of(
                closureSummaryPresent(counts, rollupPresent),
                closureRollupPresent(rollupPresent),
                countsMatchResultMetadata(countsMatch, rollupPresent),
                scenarioReplayStripped(),
                notProvenBoundariesPresent(boundariesPresent, rollupPresent));

        boolean checklistReady = rollupPresent
                && decisionReplayEvidenceReviewerClosureRollup.reviewerReady()
                && items.stream().allMatch(item -> ITEM_PASS.equals(item.status()));
        String status = status(decisionReplayEvidenceReviewerClosureRollup, checklistReady, items);
        return new RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse(
                status,
                checklistReady,
                items,
                summary(status, checklistReady, items),
                NOT_PROVEN_BOUNDARIES);
    }

    private static ClosureCounts counts(List<RoutingComparisonResultResponse> results) {
        int resultCount = results.size();
        int withClosure = 0;
        int completeWithLimitations = 0;
        int unknown = 0;
        for (RoutingComparisonResultResponse result : results) {
            RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse closure =
                    result == null ? null : result.decisionReplayEvidenceReviewerClosureSummary();
            if (closure == null) {
                unknown++;
                continue;
            }
            withClosure++;
            if (DISPOSITION_COMPLETE_WITH_LIMITATIONS.equals(closure.closureDisposition())) {
                completeWithLimitations++;
            }
            if (STATUS_UNKNOWN.equals(closure.status()) || STATUS_UNKNOWN.equals(closure.closureDisposition())) {
                unknown++;
            }
        }
        return new ClosureCounts(
                resultCount,
                withClosure,
                resultCount - withClosure,
                completeWithLimitations,
                unknown);
    }

    private static boolean countsMatch(
            ClosureCounts counts,
            RoutingDecisionReplayEvidenceReviewerClosureRollupResponse rollup) {
        return rollup != null
                && rollup.resultCount() == counts.resultCount()
                && rollup.resultsWithClosureSummary() == counts.withClosureSummary()
                && rollup.resultsMissingClosureSummary() == counts.missingClosureSummary()
                && rollup.completeWithLimitationsCount() == counts.completeWithLimitations()
                && rollup.unknownCount() == counts.unknown();
    }

    private static RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse closureSummaryPresent(
            ClosureCounts counts,
            boolean rollupPresent) {
        String status;
        if (!rollupPresent || counts.resultCount() == 0) {
            status = ITEM_UNKNOWN;
        } else {
            status = counts.missingClosureSummary() == 0 ? ITEM_PASS : ITEM_WARN;
        }
        return item(
                "closureSummaryPresent",
                status,
                status.equals(ITEM_PASS)
                        ? "Every compare result includes reviewer closure summary metadata."
                        : "One or more compare results are missing reviewer closure summary metadata.");
    }

    private static RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse closureRollupPresent(
            boolean rollupPresent) {
        return item(
                "closureRollupPresent",
                rollupPresent ? ITEM_PASS : ITEM_UNKNOWN,
                rollupPresent
                        ? "Top-level reviewer closure rollup metadata is present on the compare response."
                        : "Top-level reviewer closure rollup metadata is unavailable.");
    }

    private static RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse countsMatchResultMetadata(
            boolean countsMatch,
            boolean rollupPresent) {
        String status = rollupPresent ? (countsMatch ? ITEM_PASS : ITEM_WARN) : ITEM_UNKNOWN;
        return item(
                "countsMatchResultMetadata",
                status,
                countsMatch
                        ? "Closure rollup counts match the per-result reviewer closure metadata."
                        : "Closure rollup counts do not match the per-result reviewer closure metadata.");
    }

    private static RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse scenarioReplayStripped() {
        return item(
                "scenarioReplayStripped",
                ITEM_PASS,
                "Scenario replay keeps reviewer closure metadata stripped from embedded routing results; this "
                        + "checklist is compare response metadata only and is not replay proof.");
    }

    private static RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse notProvenBoundariesPresent(
            boolean boundariesPresent,
            boolean rollupPresent) {
        String status = rollupPresent ? (boundariesPresent ? ITEM_PASS : ITEM_WARN) : ITEM_UNKNOWN;
        return item(
                "notProvenBoundariesPresent",
                status,
                boundariesPresent
                        ? "Explicit not-proven boundaries remain present for replay, scoring, correctness, "
                                + "production readiness, production certification, guaranteed replay, and "
                                + "production validation."
                        : "One or more explicit not-proven boundaries are unavailable.");
    }

    private static RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse item(
            String name,
            String status,
            String description) {
        return new RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse(name, status, description);
    }

    private static String status(
            RoutingDecisionReplayEvidenceReviewerClosureRollupResponse rollup,
            boolean checklistReady,
            List<RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse> items) {
        if (rollup == null || STATUS_UNKNOWN.equals(rollup.status())) {
            return STATUS_UNKNOWN;
        }
        if (checklistReady && items.stream().allMatch(item -> ITEM_PASS.equals(item.status()))) {
            return STATUS_COMPLETE;
        }
        return STATUS_PARTIAL;
    }

    private static String summary(
            String status,
            boolean reviewerReady,
            List<RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse> items) {
        return "Decision replay evidence reviewer closure checklist is " + status + ": "
                + itemSummary(items)
                + "; reviewerReady=" + reviewerReady
                + ". This checklist is reviewer metadata only and is not replay proof, not scoring proof, "
                + "not correctness validation, not production readiness, not production certification, "
                + "not guaranteed replay, and not production validation.";
    }

    private static String itemSummary(
            List<RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse> items) {
        return items.stream()
                .map(item -> item.name() + "=" + item.status())
                .toList()
                .toString();
    }

    private record ClosureCounts(
            int resultCount,
            int withClosureSummary,
            int missingClosureSummary,
            int completeWithLimitations,
            int unknown) {
    }
}
