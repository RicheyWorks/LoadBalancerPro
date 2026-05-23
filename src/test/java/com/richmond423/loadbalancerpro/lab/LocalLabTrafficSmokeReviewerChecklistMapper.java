package com.richmond423.loadbalancerpro.lab;

import java.util.List;

final class LocalLabTrafficSmokeReviewerChecklistMapper {
    private static final String PRESENT = "PRESENT";
    private static final String BOUNDARY_ONLY = "BOUNDARY_ONLY";
    private static final String NOT_PROVEN = "NOT_PROVEN";
    private static final String SAFETY_BOUNDARY =
            "Traffic smoke reviewer checklist is in-memory test-scope reviewer context only; it does not "
                    + "call endpoints, open ports, write files, execute tools, persist data, export data, or "
                    + "change runtime behavior.";
    private static final String NOT_PROVEN_BOUNDARY =
            "Local simulation is not production proof; not production certification; not live-cloud validation; "
                    + "not real-tenant validation; not runtime enforcement; no Docker/k6/Bruno/Toxiproxy "
                    + "implementation; no replay execution, evidence/report generation, storage, export, "
                    + "autonomous production traffic shifting, carbon-aware routing, GPU orchestration, "
                    + "power/grid control, or facility automation.";

    private LocalLabTrafficSmokeReviewerChecklistMapper() {
    }

    static LocalLabTrafficSmokeReviewerChecklist checklist(LocalLabLoopbackTrafficSmokeSummary summary) {
        if (summary == null) {
            throw new IllegalArgumentException("summary is required");
        }
        return new LocalLabTrafficSmokeReviewerChecklist(
                "checklist-" + summary.summaryId(),
                summary.summaryId(),
                List.of(
                        item(
                                summary,
                                "loopback-only-target-confirmation",
                                "Did smoke traffic stay loopback-only?",
                                summary.loopbackHostConfirmation(),
                                "All smoke observations should target 127.0.0.1 loopback-only harness URLs.",
                                PRESENT),
                        item(
                                summary,
                                "ephemeral-port-confirmation",
                                "Did smoke traffic use harness-assigned ephemeral ports?",
                                summary.ephemeralPortConfirmation(),
                                "All smoke observations should use positive harness-assigned ephemeral ports and no common fixed ports.",
                                PRESENT),
                        item(
                                summary,
                                "scenario-profile-coverage",
                                "Which scenarios and profiles were covered?",
                                String.join(" | ", summary.scenarioIdsCovered()),
                                String.join(" | ", requiredScenarioIds()) + " | unknown-loopback-traffic-smoke-scenario",
                                PRESENT),
                        item(
                                summary,
                                "backend-coverage",
                                "Which backend ids were covered?",
                                String.join(" | ", summary.backendIdsCovered()),
                                String.join(" | ", requiredBackendIds()) + " | unknown-loopback-traffic-smoke-backend",
                                PRESENT),
                        item(
                                summary,
                                "matched-fixture-count",
                                "How many smoke responses matched expected fixtures?",
                                Integer.toString(summary.matchedFixtureCount()),
                                Integer.toString(requiredScenarioIds().size()),
                                PRESENT),
                        item(
                                summary,
                                "boundary-response-count",
                                "How many boundary responses were observed?",
                                Integer.toString(summary.boundaryResponseCount()),
                                "1 stable unknown-label boundary response",
                                BOUNDARY_ONLY),
                        item(
                                summary,
                                "evidence-label-presence",
                                "Were evidence labels present in the smoke chain?",
                                "Smoke observations feeding " + summary.summaryId()
                                        + " are expected to include evidence labels before checklist mapping.",
                                "Evidence labels should be asserted by the smoke client observations.",
                                PRESENT),
                        item(
                                summary,
                                "safety-boundary-presence",
                                "Were safety boundary labels present?",
                                "Smoke observations feeding " + summary.summaryId()
                                        + " are expected to include safety boundary labels; "
                                        + summary.noProductionProofWarning(),
                                "Safety boundary labels should remain visible to reviewers.",
                                BOUNDARY_ONLY),
                        item(
                                summary,
                                "not-proven-boundary-presence",
                                "Were not-proven boundaries present?",
                                summary.notProvenBoundarySummary(),
                                "Not-proven boundaries should remain explicit.",
                                NOT_PROVEN),
                        item(
                                summary,
                                "no-production-proof-warning",
                                "Why is local loopback smoke not production proof?",
                                summary.noProductionProofWarning(),
                                "Local simulation is not production proof.",
                                NOT_PROVEN),
                        item(
                                summary,
                                "no-docker-k6-bruno-toxiproxy-warning",
                                "Did the smoke summary avoid tool implementation claims?",
                                summary.notProvenBoundarySummary(),
                                "Docker/k6/Bruno/Toxiproxy remain not implemented by this checklist.",
                                NOT_PROVEN),
                        item(
                                summary,
                                "no-replay-report-storage-export-warning",
                                "Did the smoke summary avoid replay/report/storage/export claims?",
                                summary.notProvenBoundarySummary(),
                                "Replay execution, evidence/report generation, storage, and export behavior remain not implemented.",
                                NOT_PROVEN),
                        item(
                                summary,
                                "next-safe-step-recommendation",
                                "What is the next safe step?",
                                summary.nextSafeStepRecommendation(),
                                "Use this checklist only as reviewer context for a separately scoped local-lab step.",
                                BOUNDARY_ONLY)));
    }

    static List<LocalLabTrafficSmokeReviewerChecklist> checklists(
            List<LocalLabLoopbackTrafficSmokeSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            throw new IllegalArgumentException("summaries are required");
        }
        return summaries.stream()
                .map(LocalLabTrafficSmokeReviewerChecklistMapper::checklist)
                .toList();
    }

    private static LocalLabTrafficSmokeReviewerChecklistItem item(
            LocalLabLoopbackTrafficSmokeSummary summary,
            String suffix,
            String reviewerQuestion,
            String observedValue,
            String expectedValue,
            String dispositionLabel) {
        return new LocalLabTrafficSmokeReviewerChecklistItem(
                "checklist-item-" + summary.summaryId() + "-" + suffix,
                summary.summaryId(),
                reviewerQuestion,
                observedValue,
                expectedValue,
                dispositionLabel,
                SAFETY_BOUNDARY,
                NOT_PROVEN_BOUNDARY);
    }

    private static List<String> requiredScenarioIds() {
        return LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                .map(LocalLabFakeBackendResponseFixture::scenarioId)
                .toList();
    }

    private static List<String> requiredBackendIds() {
        return LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                .map(LocalLabFakeBackendResponseFixture::backendId)
                .toList();
    }
}
