package com.richmond423.loadbalancerpro.api;

import java.util.List;

public final class RoutingDecisionReplayEvidenceReviewerClosureRollupService {
    private static final String STATUS_COMPLETE = "COMPLETE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String DISPOSITION_COMPLETE_WITH_LIMITATIONS = "REVIEW_COMPLETE_WITH_LIMITATIONS";
    private static final String DISPOSITION_INCOMPLETE = "REVIEW_INCOMPLETE";
    private static final String DISPOSITION_UNKNOWN = "UNKNOWN";
    private static final List<String> NOT_PROVEN_BOUNDARIES = List.of(
            "not replay proof",
            "not scoring proof",
            "not correctness validation",
            "not production readiness",
            "not production certification",
            "not guaranteed replay",
            "not production validation");

    public RoutingDecisionReplayEvidenceReviewerClosureRollupResponse rollup(
            List<RoutingComparisonResultResponse> results) {
        List<RoutingComparisonResultResponse> safeResults = results == null ? List.of() : results;
        int resultCount = safeResults.size();
        int withClosure = 0;
        int completeWithLimitations = 0;
        int unknown = 0;

        for (RoutingComparisonResultResponse result : safeResults) {
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
            if (STATUS_UNKNOWN.equals(closure.status()) || DISPOSITION_UNKNOWN.equals(closure.closureDisposition())) {
                unknown++;
            }
        }

        int missing = resultCount - withClosure;
        boolean reviewerReady = resultCount > 0
                && missing == 0
                && unknown == 0
                && completeWithLimitations == resultCount;
        String status = status(resultCount, withClosure, unknown, reviewerReady);
        String disposition = disposition(status, reviewerReady);

        return new RoutingDecisionReplayEvidenceReviewerClosureRollupResponse(
                status,
                disposition,
                resultCount,
                withClosure,
                missing,
                completeWithLimitations,
                unknown,
                reviewerReady,
                summary(status, disposition, resultCount, withClosure, missing, completeWithLimitations, unknown,
                        reviewerReady),
                NOT_PROVEN_BOUNDARIES);
    }

    private static String status(int resultCount, int withClosure, int unknown, boolean reviewerReady) {
        if (resultCount == 0 || withClosure == 0 || unknown == resultCount) {
            return STATUS_UNKNOWN;
        }
        return reviewerReady ? STATUS_COMPLETE : STATUS_PARTIAL;
    }

    private static String disposition(String status, boolean reviewerReady) {
        if (STATUS_UNKNOWN.equals(status)) {
            return DISPOSITION_UNKNOWN;
        }
        return reviewerReady ? DISPOSITION_COMPLETE_WITH_LIMITATIONS : DISPOSITION_INCOMPLETE;
    }

    private static String summary(
            String status,
            String disposition,
            int resultCount,
            int withClosure,
            int missing,
            int completeWithLimitations,
            int unknown,
            boolean reviewerReady) {
        return "Decision replay evidence reviewer closure rollup is " + status + " with " + disposition
                + " disposition: " + withClosure + " of " + resultCount + " results include closure summaries, "
                + missing + " results are missing closure summaries, " + completeWithLimitations
                + " are REVIEW_COMPLETE_WITH_LIMITATIONS, " + unknown + " are UNKNOWN or not available, "
                + "and reviewerReady=" + reviewerReady + ".";
    }
}
