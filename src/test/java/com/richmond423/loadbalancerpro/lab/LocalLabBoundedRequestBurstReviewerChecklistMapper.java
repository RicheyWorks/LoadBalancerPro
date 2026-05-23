package com.richmond423.loadbalancerpro.lab;

import java.util.List;

final class LocalLabBoundedRequestBurstReviewerChecklistMapper {
    private static final String PRESENT = "PRESENT";
    private static final String BOUNDARY_ONLY = "BOUNDARY_ONLY";
    private static final String NOT_PROVEN = "NOT_PROVEN";
    private static final String SAFETY_BOUNDARY =
            "Bounded burst reviewer checklist is in-memory test-scope reviewer context only; it does not "
                    + "call endpoints, bind servers, open ports, write files, execute tools, persist data, "
                    + "export data, or change runtime behavior.";
    private static final String NOT_PROVEN_BOUNDARY =
            "Local simulation is not production proof; not load testing; not stress testing; not "
                    + "benchmarking; not throughput evidence; not p95/p99 evidence; not production "
                    + "certification; not live-cloud validation; not real-tenant validation; not runtime "
                    + "enforcement; no Docker/k6/Bruno/Toxiproxy implementation; no replay execution, "
                    + "evidence/report generation, storage, export, autonomous production traffic shifting, "
                    + "carbon-aware routing, GPU orchestration, power/grid control, or facility automation.";

    private LocalLabBoundedRequestBurstReviewerChecklistMapper() {
    }

    static LocalLabBoundedRequestBurstReviewerChecklist checklist(
            LocalLabBoundedRequestBurstSummary summary) {
        if (summary == null) {
            throw new IllegalArgumentException("summary is required");
        }
        return new LocalLabBoundedRequestBurstReviewerChecklist(
                "checklist-" + summary.summaryId(),
                summary.summaryId(),
                List.of(
                        item(
                                summary,
                                "burst-case-count",
                                "How many bounded burst cases were exercised?",
                                Integer.toString(summary.burstCaseCount()),
                                Integer.toString(LocalLabBoundedRequestBurstCatalog.cases().size()),
                                PRESENT),
                        item(
                                summary,
                                "total-request-count",
                                "How many total bounded burst requests were issued?",
                                Integer.toString(summary.totalRequestCount()),
                                Integer.toString(LocalLabBoundedRequestBurstCatalog.cases().size()
                                        * LocalLabBoundedRequestBurstCatalog.FIXED_REPETITIONS_PER_CASE),
                                PRESENT),
                        item(
                                summary,
                                "fixed-repetition-count",
                                "What fixed repetition count was used?",
                                Integer.toString(summary.fixedRepetitionCount()),
                                Integer.toString(LocalLabBoundedRequestBurstCatalog.FIXED_REPETITIONS_PER_CASE),
                                PRESENT),
                        item(
                                summary,
                                "scenario-profile-coverage",
                                "Which scenarios and profiles were covered by the bounded burst?",
                                String.join(" | ", summary.scenarioIdsCovered()),
                                String.join(" | ", requiredScenarioIds()),
                                PRESENT),
                        item(
                                summary,
                                "backend-coverage",
                                "Which backend ids were covered by the bounded burst?",
                                String.join(" | ", summary.backendIdsCovered()),
                                String.join(" | ", requiredBackendIds()),
                                PRESENT),
                        item(
                                summary,
                                "matched-fixture-count",
                                "How many bounded burst responses matched expected fixtures?",
                                Integer.toString(summary.matchedFixtureCount()),
                                Integer.toString(LocalLabFakeBackendResponseFixtureCatalog.fixtures().size()
                                        * LocalLabBoundedRequestBurstCatalog.FIXED_REPETITIONS_PER_CASE),
                                PRESENT),
                        item(
                                summary,
                                "boundary-response-count",
                                "How many bounded burst boundary responses were observed?",
                                Integer.toString(summary.boundaryResponseCount()),
                                Integer.toString(LocalLabBoundedRequestBurstCatalog.FIXED_REPETITIONS_PER_CASE)
                                        + " stable unknown-label boundary responses",
                                BOUNDARY_ONLY),
                        item(
                                summary,
                                "loopback-only-confirmation",
                                "Did the bounded burst stay loopback-only?",
                                summary.loopbackOnlyConfirmation(),
                                "All bounded burst observations should target 127.0.0.1 loopback-only harness URLs.",
                                PRESENT),
                        item(
                                summary,
                                "ephemeral-port-confirmation",
                                "Did the bounded burst use harness-assigned ephemeral ports?",
                                summary.ephemeralPortConfirmation(),
                                "All bounded burst observations should use positive harness-assigned ephemeral ports and no common fixed ports.",
                                PRESENT),
                        item(
                                summary,
                                "deterministic-output-confirmation",
                                "Is the bounded burst output deterministic?",
                                summary.deterministicOutputConfirmation(),
                                "Bounded burst output should be deterministic in memory across repeated calls.",
                                PRESENT),
                        item(
                                summary,
                                "no-production-proof-warning",
                                "Why is the bounded burst not production proof?",
                                summary.noProductionProofWarning(),
                                "Local simulation is not production proof.",
                                NOT_PROVEN),
                        item(
                                summary,
                                "no-load-stress-benchmark-warning",
                                "Did the bounded burst avoid load, stress, and benchmark claims?",
                                summary.noLoadTestWarning(),
                                "Bounded burst checks are not load testing, not stress testing, and not benchmarking.",
                                NOT_PROVEN),
                        item(
                                summary,
                                "no-throughput-p95-p99-evidence-warning",
                                "Did the bounded burst avoid throughput and p95/p99 evidence claims?",
                                summary.noLoadTestWarning(),
                                "Bounded burst checks are not throughput evidence and not p95/p99 evidence.",
                                NOT_PROVEN),
                        item(
                                summary,
                                "no-docker-k6-bruno-toxiproxy-warning",
                                "Did the bounded burst avoid external tooling implementation claims?",
                                summary.futureToolingBoundarySummary(),
                                "Docker/k6/Bruno/Toxiproxy remain not implemented by this checklist.",
                                NOT_PROVEN),
                        item(
                                summary,
                                "no-replay-report-storage-export-warning",
                                "Did the bounded burst avoid replay/report/storage/export claims?",
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

    static List<LocalLabBoundedRequestBurstReviewerChecklist> checklists(
            List<LocalLabBoundedRequestBurstSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            throw new IllegalArgumentException("summaries are required");
        }
        return summaries.stream()
                .map(LocalLabBoundedRequestBurstReviewerChecklistMapper::checklist)
                .toList();
    }

    private static LocalLabBoundedRequestBurstReviewerChecklistItem item(
            LocalLabBoundedRequestBurstSummary summary,
            String suffix,
            String reviewerQuestion,
            String observedValue,
            String expectedValue,
            String dispositionLabel) {
        return new LocalLabBoundedRequestBurstReviewerChecklistItem(
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
        return LocalLabBoundedRequestBurstCatalog.cases().stream()
                .map(LocalLabBoundedRequestBurstCase::scenarioId)
                .toList();
    }

    private static List<String> requiredBackendIds() {
        return LocalLabBoundedRequestBurstCatalog.cases().stream()
                .map(LocalLabBoundedRequestBurstCase::backendId)
                .toList();
    }
}
