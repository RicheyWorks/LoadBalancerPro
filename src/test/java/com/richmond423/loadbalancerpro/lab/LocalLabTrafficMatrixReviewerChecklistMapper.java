package com.richmond423.loadbalancerpro.lab;

import java.util.List;

final class LocalLabTrafficMatrixReviewerChecklistMapper {
    private static final String PRESENT = "PRESENT";
    private static final String BOUNDARY_ONLY = "BOUNDARY_ONLY";
    private static final String NOT_PROVEN = "NOT_PROVEN";
    private static final String SAFETY_BOUNDARY =
            "Traffic matrix reviewer checklist is in-memory test-scope reviewer context only; it does not "
                    + "call endpoints, bind servers, open ports, write files, execute tools, persist data, "
                    + "export data, or change runtime behavior.";
    private static final String NOT_PROVEN_BOUNDARY =
            "Local simulation is not production proof; not load testing; not stress testing; not production "
                    + "certification; not live-cloud validation; not real-tenant validation; not runtime "
                    + "enforcement; no Docker/k6/Bruno/Toxiproxy implementation; no replay execution, "
                    + "evidence/report generation, storage, export, autonomous production traffic shifting, "
                    + "carbon-aware routing, GPU orchestration, power/grid control, or facility automation.";

    private LocalLabTrafficMatrixReviewerChecklistMapper() {
    }

    static LocalLabTrafficMatrixReviewerChecklist checklist(LocalLabTrafficMatrixSummary summary) {
        if (summary == null) {
            throw new IllegalArgumentException("summary is required");
        }
        return new LocalLabTrafficMatrixReviewerChecklist(
                "checklist-" + summary.summaryId(),
                summary.summaryId(),
                List.of(
                        item(
                                summary,
                                "traffic-matrix-case-count",
                                "How many traffic matrix cases were exercised?",
                                Integer.toString(summary.matrixCaseCount()),
                                Integer.toString(LocalLabTrafficMatrixCatalog.cases().size()),
                                PRESENT),
                        item(
                                summary,
                                "scenario-profile-coverage",
                                "Which scenarios and profiles were covered by the matrix?",
                                String.join(" | ", summary.scenarioIdsCovered()),
                                String.join(" | ", requiredScenarioIds()),
                                PRESENT),
                        item(
                                summary,
                                "backend-coverage",
                                "Which backend ids were covered by the matrix?",
                                String.join(" | ", summary.backendIdsCovered()),
                                String.join(" | ", requiredBackendIds()),
                                PRESENT),
                        item(
                                summary,
                                "matched-fixture-count",
                                "How many matrix responses matched expected fixtures?",
                                Integer.toString(summary.matchedFixtureCount()),
                                Integer.toString(LocalLabFakeBackendResponseFixtureCatalog.fixtures().size()),
                                PRESENT),
                        item(
                                summary,
                                "boundary-case-count",
                                "How many matrix boundary cases were observed?",
                                Integer.toString(summary.boundaryCaseCount()),
                                "1 stable unknown-label boundary case",
                                BOUNDARY_ONLY),
                        item(
                                summary,
                                "loopback-only-confirmation",
                                "Did the matrix stay loopback-only?",
                                summary.loopbackOnlyConfirmation(),
                                "All matrix observations should target 127.0.0.1 loopback-only harness URLs.",
                                PRESENT),
                        item(
                                summary,
                                "ephemeral-port-confirmation",
                                "Did the matrix use harness-assigned ephemeral ports?",
                                summary.ephemeralPortConfirmation(),
                                "All matrix observations should use positive harness-assigned ephemeral ports and no common fixed ports.",
                                PRESENT),
                        item(
                                summary,
                                "deterministic-output-confirmation",
                                "Is the matrix output deterministic?",
                                summary.deterministicOutputConfirmation(),
                                "Traffic matrix output should be deterministic in memory across repeated calls.",
                                PRESENT),
                        item(
                                summary,
                                "no-production-proof-warning",
                                "Why is the traffic matrix not production proof?",
                                summary.noProductionProofWarning(),
                                "Local simulation is not production proof.",
                                NOT_PROVEN),
                        item(
                                summary,
                                "no-load-stress-testing-warning",
                                "Did the matrix avoid load or stress testing claims?",
                                summary.noProductionProofWarning(),
                                "Traffic matrix tests are not load testing and not stress testing.",
                                NOT_PROVEN),
                        item(
                                summary,
                                "no-docker-k6-bruno-toxiproxy-warning",
                                "Did the matrix avoid external tooling implementation claims?",
                                summary.futureToolingBoundarySummary(),
                                "Docker/k6/Bruno/Toxiproxy remain not implemented by this checklist.",
                                NOT_PROVEN),
                        item(
                                summary,
                                "no-replay-report-storage-export-warning",
                                "Did the matrix avoid replay/report/storage/export claims?",
                                summary.futureToolingBoundarySummary(),
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

    static List<LocalLabTrafficMatrixReviewerChecklist> checklists(
            List<LocalLabTrafficMatrixSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            throw new IllegalArgumentException("summaries are required");
        }
        return summaries.stream()
                .map(LocalLabTrafficMatrixReviewerChecklistMapper::checklist)
                .toList();
    }

    private static LocalLabTrafficMatrixReviewerChecklistItem item(
            LocalLabTrafficMatrixSummary summary,
            String suffix,
            String reviewerQuestion,
            String observedValue,
            String expectedValue,
            String dispositionLabel) {
        return new LocalLabTrafficMatrixReviewerChecklistItem(
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
        return LocalLabTrafficMatrixCatalog.cases().stream()
                .map(LocalLabTrafficMatrixCase::scenarioId)
                .toList();
    }

    private static List<String> requiredBackendIds() {
        return LocalLabTrafficMatrixCatalog.cases().stream()
                .map(LocalLabTrafficMatrixCase::backendId)
                .toList();
    }
}
