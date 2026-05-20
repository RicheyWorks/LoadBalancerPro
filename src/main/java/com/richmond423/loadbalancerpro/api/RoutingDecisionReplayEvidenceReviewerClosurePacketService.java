package com.richmond423.loadbalancerpro.api;

import java.util.List;

public final class RoutingDecisionReplayEvidenceReviewerClosurePacketService {
    private static final String STATUS_COMPLETE = "COMPLETE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String SECTION_PASS = "PASS";
    private static final String SECTION_WARN = "WARN";
    private static final String SECTION_UNKNOWN = "UNKNOWN";
    private static final String PACKET_VERSION = "v1";
    private static final List<String> NOT_PROVEN_BOUNDARIES = List.of(
            "not replay proof",
            "not scoring proof",
            "not correctness validation",
            "not production readiness",
            "not production certification",
            "not guaranteed replay",
            "not production validation");

    public RoutingDecisionReplayEvidenceReviewerClosurePacketResponse packet(
            List<RoutingComparisonResultResponse> results,
            RoutingDecisionReplayEvidenceReviewerClosureRollupResponse closureRollup,
            RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse closureChecklist) {
        List<RoutingComparisonResultResponse> safeResults = results == null ? List.of() : results;
        SectionCounts sectionCounts = sectionCounts(safeResults);
        List<RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse> sections = List.of(
                closureSummarySection(sectionCounts),
                closureRollupSection(closureRollup),
                closureChecklistSection(closureChecklist),
                scenarioReplayBoundarySection(closureChecklist),
                notProvenBoundariesSection(closureRollup, closureChecklist));
        boolean reviewerReady = closureRollup != null
                && closureChecklist != null
                && closureRollup.reviewerReady()
                && closureChecklist.reviewerReady()
                && allSectionsPass(sections);
        String status = status(closureRollup, closureChecklist, reviewerReady, sections);
        return new RoutingDecisionReplayEvidenceReviewerClosurePacketResponse(
                status,
                reviewerReady,
                PACKET_VERSION,
                sections,
                summary(status, reviewerReady, sections),
                reviewerGuidance(),
                NOT_PROVEN_BOUNDARIES);
    }

    private static SectionCounts sectionCounts(List<RoutingComparisonResultResponse> results) {
        int resultCount = results.size();
        int withClosureSummary = 0;
        for (RoutingComparisonResultResponse result : results) {
            if (result != null && result.decisionReplayEvidenceReviewerClosureSummary() != null) {
                withClosureSummary++;
            }
        }
        return new SectionCounts(resultCount, withClosureSummary, resultCount - withClosureSummary);
    }

    private static RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse closureSummarySection(
            SectionCounts counts) {
        String status;
        if (counts.resultCount() == 0) {
            status = SECTION_UNKNOWN;
        } else {
            status = counts.missingClosureSummary() == 0 ? SECTION_PASS : SECTION_WARN;
        }
        return section(
                "closureSummary",
                status,
                status.equals(SECTION_PASS)
                        ? "Per-result reviewer closure summary metadata is present for every compare result."
                        : "One or more compare results are missing reviewer closure summary metadata.");
    }

    private static RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse closureRollupSection(
            RoutingDecisionReplayEvidenceReviewerClosureRollupResponse closureRollup) {
        String status = statusFromLane(closureRollup == null ? null : closureRollup.status());
        return section(
                "closureRollup",
                status,
                closureRollup == null
                        ? "Top-level reviewer closure rollup metadata is unavailable."
                        : "Top-level reviewer closure rollup metadata is present with status "
                                + closureRollup.status() + ".");
    }

    private static RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse closureChecklistSection(
            RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse closureChecklist) {
        String status = statusFromLane(closureChecklist == null ? null : closureChecklist.status());
        return section(
                "closureChecklist",
                status,
                closureChecklist == null
                        ? "Top-level reviewer closure checklist metadata is unavailable."
                        : "Top-level reviewer closure checklist metadata is present with status "
                                + closureChecklist.status() + ".");
    }

    private static RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse scenarioReplayBoundarySection(
            RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse closureChecklist) {
        String status = checklistItemStatus(closureChecklist, "scenarioReplayStripped");
        return section(
                "scenarioReplayBoundary",
                status,
                "Scenario replay keeps reviewer closure metadata stripped from embedded routing results; this "
                        + "packet is compare response metadata only and is not replay proof.");
    }

    private static RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse notProvenBoundariesSection(
            RoutingDecisionReplayEvidenceReviewerClosureRollupResponse closureRollup,
            RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse closureChecklist) {
        boolean boundariesPresent = closureRollup != null
                && closureChecklist != null
                && NOT_PROVEN_BOUNDARIES.equals(closureRollup.notProvenBoundaries())
                && NOT_PROVEN_BOUNDARIES.equals(closureChecklist.notProvenBoundaries());
        String status = boundariesPresent ? SECTION_PASS : (closureRollup == null || closureChecklist == null
                ? SECTION_UNKNOWN
                : SECTION_WARN);
        return section(
                "notProvenBoundaries",
                status,
                boundariesPresent
                        ? "Explicit not-proven boundaries remain present across the closure rollup and checklist."
                        : "One or more explicit not-proven boundaries are unavailable in the source metadata.");
    }

    private static String checklistItemStatus(
            RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse closureChecklist,
            String itemName) {
        if (closureChecklist == null || closureChecklist.items() == null) {
            return SECTION_UNKNOWN;
        }
        return closureChecklist.items().stream()
                .filter(item -> itemName.equals(item.name()))
                .findFirst()
                .map(RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse::status)
                .map(RoutingDecisionReplayEvidenceReviewerClosurePacketService::sectionStatusFromChecklistItem)
                .orElse(SECTION_UNKNOWN);
    }

    private static String sectionStatusFromChecklistItem(String status) {
        if (SECTION_PASS.equals(status) || SECTION_WARN.equals(status)) {
            return status;
        }
        return SECTION_UNKNOWN;
    }

    private static String statusFromLane(String laneStatus) {
        if (STATUS_COMPLETE.equals(laneStatus)) {
            return SECTION_PASS;
        }
        if (STATUS_PARTIAL.equals(laneStatus)) {
            return SECTION_WARN;
        }
        return SECTION_UNKNOWN;
    }

    private static boolean allSectionsPass(
            List<RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse> sections) {
        return sections.stream().allMatch(section -> SECTION_PASS.equals(section.status()));
    }

    private static String status(
            RoutingDecisionReplayEvidenceReviewerClosureRollupResponse closureRollup,
            RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse closureChecklist,
            boolean reviewerReady,
            List<RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse> sections) {
        if (closureRollup == null
                || closureChecklist == null
                || STATUS_UNKNOWN.equals(closureRollup.status())
                || STATUS_UNKNOWN.equals(closureChecklist.status())) {
            return STATUS_UNKNOWN;
        }
        if (reviewerReady && allSectionsPass(sections)) {
            return STATUS_COMPLETE;
        }
        return STATUS_PARTIAL;
    }

    private static RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse section(
            String name,
            String status,
            String description) {
        return new RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse(name, status, description);
    }

    private static String summary(
            String status,
            boolean reviewerReady,
            List<RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse> sections) {
        return "Decision replay evidence reviewer closure packet is " + status + ": "
                + sectionSummary(sections)
                + "; reviewerReady=" + reviewerReady
                + ". This packet is in-response reviewer metadata only, not an export/share/download packet, "
                + "and is not replay proof, not scoring proof, not correctness validation, "
                + "not production readiness, not production certification, not guaranteed replay, "
                + "and not production validation.";
    }

    private static List<String> reviewerGuidance() {
        return List.of(
                "Use this packet as an in-response reviewer index over closure summary, rollup, and checklist "
                        + "metadata.",
                "Inspect per-result closure summaries and the top-level rollup/checklist before treating the "
                        + "response as reviewer-ready.",
                "Treat the not-proven boundaries as explicit limits: this packet is not replay proof, not scoring "
                        + "proof, not correctness validation, not production readiness, not production "
                        + "certification, not guaranteed replay, and not production validation.");
    }

    private static String sectionSummary(
            List<RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse> sections) {
        return sections.stream()
                .map(section -> section.name() + "=" + section.status())
                .toList()
                .toString();
    }

    private record SectionCounts(
            int resultCount,
            int withClosureSummary,
            int missingClosureSummary) {
    }
}
