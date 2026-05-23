package com.richmond423.loadbalancerpro.lab;

import java.util.List;

final class LocalLabPassiveReviewerChecklistMapper {
    private static final String PRESENT = "PRESENT";
    private static final String BOUNDARY_ONLY = "BOUNDARY_ONLY";
    private static final String NOT_PROVEN = "NOT_PROVEN";
    private static final String PRODUCTION_PROOF_WARNING =
            "Local simulation is useful reviewer context, but local simulation is not production proof.";

    private LocalLabPassiveReviewerChecklistMapper() {
    }

    static List<LocalLabPassiveReviewerChecklist> checklists() {
        return map(LocalLabPassiveTranscriptSummaryRenderer.summaries());
    }

    static List<LocalLabPassiveReviewerChecklist> map(List<LocalLabPassiveTranscriptSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            throw new IllegalArgumentException("summaries are required");
        }
        return summaries.stream()
                .map(LocalLabPassiveReviewerChecklistMapper::checklist)
                .toList();
    }

    private static LocalLabPassiveReviewerChecklist checklist(LocalLabPassiveTranscriptSummary summary) {
        return new LocalLabPassiveReviewerChecklist(
                "checklist-" + summary.transcriptId(),
                summary.scenarioId(),
                summary.transcriptId(),
                List.of(
                        item(
                                summary,
                                "scenario-identity",
                                "What scenario was modeled?",
                                summary.scenarioId(),
                                PRESENT),
                        item(
                                summary,
                                "backend-behavior",
                                "What backend behavior was represented?",
                                summary.scenarioBehaviorType(),
                                PRESENT),
                        item(
                                summary,
                                "backend-ids-observed",
                                "What backend ids were observed?",
                                String.join(" | ", summary.backendIdsObserved()),
                                PRESENT),
                        item(
                                summary,
                                "request-labels-observed",
                                "What request labels were observed?",
                                String.join(" | ", summary.requestLabelsObserved()),
                                PRESENT),
                        item(
                                summary,
                                "response-status-labels-observed",
                                "What response status labels were observed?",
                                String.join(" | ", summary.responseStatusLabelsObserved()),
                                PRESENT),
                        item(
                                summary,
                                "latency-labels-observed",
                                "What latency labels were observed?",
                                String.join(" | ", summary.latencyLabelsObserved()),
                                PRESENT),
                        item(
                                summary,
                                "error-load-labels-observed",
                                "What error/load labels were observed?",
                                String.join(" | ", summary.errorLoadLabelsObserved()),
                                PRESENT),
                        item(
                                summary,
                                "evidence-notes",
                                "What evidence was present?",
                                summary.evidenceNoteSummary(),
                                PRESENT),
                        item(
                                summary,
                                "safety-boundary-notes",
                                "What safety boundary was stated?",
                                summary.safetyBoundarySummary(),
                                BOUNDARY_ONLY),
                        item(
                                summary,
                                "not-proven-boundaries",
                                "What was not proven?",
                                summary.notProvenBoundarySummary(),
                                NOT_PROVEN),
                        item(
                                summary,
                                "production-proof-warning",
                                "Why is this still not production proof?",
                                PRODUCTION_PROOF_WARNING,
                                NOT_PROVEN)));
    }

    private static LocalLabPassiveReviewerChecklistItem item(
            LocalLabPassiveTranscriptSummary summary,
            String itemSuffix,
            String reviewerQuestion,
            String observedSummaryValue,
            String reviewerDispositionLabel) {
        return new LocalLabPassiveReviewerChecklistItem(
                "checklist-item-" + summary.transcriptId() + "-" + itemSuffix,
                summary.scenarioId(),
                summary.transcriptId(),
                reviewerQuestion,
                observedSummaryValue,
                "Reviewer should confirm this passive summary value is present and bounded by local-lab context.",
                summary.safetyBoundarySummary(),
                summary.notProvenBoundarySummary() + " " + PRODUCTION_PROOF_WARNING,
                reviewerDispositionLabel);
    }
}
