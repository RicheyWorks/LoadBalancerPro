package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LocalLabTrafficMatrixReviewerChecklistMapperTest {
    private static final List<String> REQUIRED_QUESTIONS = List.of(
            "How many traffic matrix cases were exercised?",
            "Which scenarios and profiles were covered by the matrix?",
            "Which backend ids were covered by the matrix?",
            "How many matrix responses matched expected fixtures?",
            "How many matrix boundary cases were observed?",
            "Did the matrix stay loopback-only?",
            "Did the matrix use harness-assigned ephemeral ports?",
            "Is the matrix output deterministic?",
            "Why is the traffic matrix not production proof?",
            "Did the matrix avoid load or stress testing claims?",
            "Did the matrix avoid external tooling implementation claims?",
            "Did the matrix avoid replay/report/storage/export claims?",
            "What is the next safe step?");

    @Test
    void everyMatrixSummaryProducesReviewerChecklist() {
        LocalLabTrafficMatrixSummary summary = matrixSummary();
        LocalLabTrafficMatrixReviewerChecklist checklist =
                LocalLabTrafficMatrixReviewerChecklistMapper.checklist(summary);
        List<LocalLabTrafficMatrixReviewerChecklist> checklists =
                LocalLabTrafficMatrixReviewerChecklistMapper.checklists(List.of(summary));

        assertEquals(List.of(checklist), checklists);
        assertEquals("checklist-summary-local-lab-deterministic-traffic-matrix", checklist.checklistId());
        assertEquals(summary.summaryId(), checklist.matrixSummaryId());
        assertEquals(REQUIRED_QUESTIONS.size(), checklist.items().size());
        assertEquals(REQUIRED_QUESTIONS, checklist.items().stream()
                .map(LocalLabTrafficMatrixReviewerChecklistItem::reviewerQuestion)
                .toList());
    }

    @Test
    void checklistOrderingAndIdsAreDeterministicStableUniqueAndImmutable() {
        LocalLabTrafficMatrixReviewerChecklist first =
                LocalLabTrafficMatrixReviewerChecklistMapper.checklist(matrixSummary());
        LocalLabTrafficMatrixReviewerChecklist second =
                LocalLabTrafficMatrixReviewerChecklistMapper.checklist(matrixSummary());
        List<String> itemIds = first.items().stream()
                .map(LocalLabTrafficMatrixReviewerChecklistItem::itemId)
                .toList();

        assertEquals(first, second);
        assertEquals(first.deterministicText(), second.deterministicText());
        assertEquals(itemIds.size(), Set.copyOf(itemIds).size());
        assertEquals(List.of(
                "checklist-item-summary-local-lab-deterministic-traffic-matrix-traffic-matrix-case-count",
                "checklist-item-summary-local-lab-deterministic-traffic-matrix-scenario-profile-coverage",
                "checklist-item-summary-local-lab-deterministic-traffic-matrix-backend-coverage",
                "checklist-item-summary-local-lab-deterministic-traffic-matrix-matched-fixture-count",
                "checklist-item-summary-local-lab-deterministic-traffic-matrix-boundary-case-count",
                "checklist-item-summary-local-lab-deterministic-traffic-matrix-loopback-only-confirmation",
                "checklist-item-summary-local-lab-deterministic-traffic-matrix-ephemeral-port-confirmation",
                "checklist-item-summary-local-lab-deterministic-traffic-matrix-deterministic-output-confirmation",
                "checklist-item-summary-local-lab-deterministic-traffic-matrix-no-production-proof-warning",
                "checklist-item-summary-local-lab-deterministic-traffic-matrix-no-load-stress-testing-warning",
                "checklist-item-summary-local-lab-deterministic-traffic-matrix-no-docker-k6-bruno-toxiproxy-warning",
                "checklist-item-summary-local-lab-deterministic-traffic-matrix-no-replay-report-storage-export-warning",
                "checklist-item-summary-local-lab-deterministic-traffic-matrix-next-safe-step-recommendation"),
                itemIds);
        assertThrows(UnsupportedOperationException.class, () -> first.items().add(first.items().get(0)));
    }

    @Test
    void checklistIncludesMatrixCoverageCountsLoopbackEphemeralAndDeterministicChecks() {
        LocalLabTrafficMatrixReviewerChecklist checklist =
                LocalLabTrafficMatrixReviewerChecklistMapper.checklist(matrixSummary());

        assertItemContains(checklist, "How many traffic matrix cases were exercised?", "8");
        assertItemContains(checklist, "How many matrix responses matched expected fixtures?", "7");
        assertItemContains(checklist, "How many matrix boundary cases were observed?", "1");
        assertItemContains(checklist, "Did the matrix stay loopback-only?", "127.0.0.1 loopback-only");
        assertItemContains(checklist, "Did the matrix use harness-assigned ephemeral ports?",
                "harness-assigned ephemeral ports");
        assertItemContains(checklist, "Is the matrix output deterministic?", "deterministic in memory");

        for (String scenarioId : LocalLabTrafficMatrixCatalog.cases().stream()
                .map(LocalLabTrafficMatrixCase::scenarioId)
                .toList()) {
            assertItemContains(checklist, "Which scenarios and profiles were covered by the matrix?", scenarioId);
        }

        for (String backendId : LocalLabTrafficMatrixCatalog.cases().stream()
                .map(LocalLabTrafficMatrixCase::backendId)
                .toList()) {
            assertItemContains(checklist, "Which backend ids were covered by the matrix?", backendId);
        }
    }

    @Test
    void checklistIncludesNoProductionProofLoadToolReplayStorageExportAndNextStepBoundaries() {
        LocalLabTrafficMatrixReviewerChecklist checklist =
                LocalLabTrafficMatrixReviewerChecklistMapper.checklist(matrixSummary());
        String normalized = normalize(checklist.deterministicText());

        assertItemContains(checklist, "Why is the traffic matrix not production proof?", "not production proof");
        assertItemContains(checklist, "Did the matrix avoid load or stress testing claims?", "not load testing");
        assertItemContains(checklist, "Did the matrix avoid external tooling implementation claims?",
                "Docker/k6/Bruno/Toxiproxy");
        assertItemContains(checklist, "Did the matrix avoid replay/report/storage/export claims?",
                "replay execution");
        assertItemContains(checklist, "What is the next safe step?", "separately scoped");
        assertTrue(normalized.contains("local simulation is not production proof"));
        assertTrue(normalized.contains("not load testing"));
        assertTrue(normalized.contains("not stress testing"));
        assertTrue(normalized.contains("not production certification"));
        assertTrue(normalized.contains("not live-cloud validation"));
        assertTrue(normalized.contains("not real-tenant validation"));
        assertTrue(normalized.contains("not runtime enforcement"));
        assertTrue(normalized.contains("no docker/k6/bruno/toxiproxy implementation"));
        assertTrue(normalized.contains("no replay execution"));
        assertTrue(normalized.contains("evidence/report generation"));
        assertTrue(normalized.contains("storage"));
        assertTrue(normalized.contains("export"));

        for (LocalLabTrafficMatrixReviewerChecklistItem item : checklist.items()) {
            assertEquals(checklist.matrixSummaryId(), item.matrixSummaryId());
            assertTrue(List.of("PRESENT", "BOUNDARY_ONLY", "NOT_PROVEN").contains(item.dispositionLabel()));
            assertFalse(item.safetyBoundary().isBlank());
            assertFalse(item.notProvenBoundary().isBlank());
        }
    }

    @Test
    void mapperRejectsMissingInputAndOutputIsDeterministicAcrossRepeatedCalls() {
        LocalLabTrafficMatrixSummary summary = matrixSummary();

        assertThrows(IllegalArgumentException.class, () -> LocalLabTrafficMatrixReviewerChecklistMapper.checklist(null));
        assertThrows(IllegalArgumentException.class, () -> LocalLabTrafficMatrixReviewerChecklistMapper.checklists(null));
        assertThrows(IllegalArgumentException.class, () -> LocalLabTrafficMatrixReviewerChecklistMapper.checklists(
                List.of()));
        assertEquals(LocalLabTrafficMatrixReviewerChecklistMapper.checklist(summary),
                LocalLabTrafficMatrixReviewerChecklistMapper.checklist(summary));
        assertEquals(LocalLabTrafficMatrixReviewerChecklistMapper.checklists(List.of(summary)),
                LocalLabTrafficMatrixReviewerChecklistMapper.checklists(List.of(summary)));
    }

    @Test
    void mapperDoesNotCallNetworkOrImplyProductionToolReplayReportStorageExportRuntimeOrValidationClaims() {
        String normalized = normalize(LocalLabTrafficMatrixReviewerChecklistMapper.checklist(matrixSummary())
                .deterministicText());

        assertTrue(normalized.contains("does not call endpoints"));
        assertTrue(normalized.contains("bind servers"));
        for (String forbidden : List.of(
                "production-ready",
                "production certified",
                "production validation is complete",
                "live-cloud validated",
                "live-cloud validation is complete",
                "real-tenant validated",
                "real-tenant validation is complete",
                "runtime enforcement is implemented",
                "load testing is complete",
                "stress testing is complete",
                "docker compose is implemented",
                "k6 scenario is implemented",
                "bruno collection is implemented",
                "toxiproxy config is implemented",
                "prometheus/grafana dashboard is implemented",
                "replay execution is implemented",
                "evidence report is generated",
                "report generation is implemented",
                "storage is implemented",
                "export behavior is implemented",
                "runtime behavior is changed",
                "routing behavior is changed",
                "production api behavior is changed",
                "production endpoint is added",
                "non-loopback target is called")) {
            assertFalse(normalized.contains(forbidden), "checklist must not overclaim " + forbidden);
        }
    }

    private static LocalLabTrafficMatrixSummary matrixSummary() {
        return LocalLabTrafficMatrixSummaryRenderer.summarize(List.of(
                observation("matrix-case-backend-healthy-fast", "backend-healthy-fast", "backend-healthy-fast", 200, true,
                        false),
                observation("matrix-case-backend-slow-tail-latency", "backend-slow-tail-latency",
                        "backend-slow-tail-latency", 200, true, false),
                observation("matrix-case-backend-partial-degradation", "backend-partial-degradation",
                        "backend-partial-degradation", 200, true, false),
                observation("matrix-case-backend-error-prone", "backend-error-prone", "backend-error-prone", 500,
                        true, false),
                observation("matrix-case-backend-overloaded-queue-pressure", "backend-overloaded-queue-pressure",
                        "backend-overloaded-queue-pressure", 503, true, false),
                observation("matrix-case-backend-all-unhealthy-no-good-choice",
                        "backend-all-unhealthy-no-good-choice", "backend-all-unhealthy-no-good-choice", 503, true,
                        false),
                observation("matrix-case-backend-recovery", "backend-recovery", "backend-recovery", 200, true,
                        false),
                observation("matrix-case-unknown-label-boundary",
                        LocalLabTrafficMatrixCatalog.UNKNOWN_MATRIX_BACKEND_ID,
                        LocalLabTrafficMatrixCatalog.UNKNOWN_MATRIX_SCENARIO_ID, 404, false, true)));
    }

    private static LocalLabTrafficMatrixObservation observation(
            String caseId,
            String backendId,
            String scenarioId,
            int statusCode,
            boolean fixtureMatched,
            boolean boundaryResponse) {
        return new LocalLabTrafficMatrixObservation(
                caseId,
                scenarioId,
                backendId,
                "127.0.0.1",
                49152,
                statusCode,
                fixtureMatched,
                true,
                true,
                true,
                boundaryResponse,
                "Test-scope deterministic traffic matrix only; not k6; not Bruno; not Docker; not Toxiproxy; "
                        + "not load testing; not stress testing; not production traffic; not production proof; "
                        + "no fixed ports; no non-loopback target calls; no replay execution; no evidence/report "
                        + "generation; no file writing; no environment reads; no process execution; no storage; "
                        + "no export behavior.");
    }

    private static void assertItemContains(
            LocalLabTrafficMatrixReviewerChecklist checklist,
            String reviewerQuestion,
            String expectedValue) {
        LocalLabTrafficMatrixReviewerChecklistItem item = checklist.items().stream()
                .filter(candidate -> candidate.reviewerQuestion().equals(reviewerQuestion))
                .findFirst()
                .orElseThrow();
        assertTrue(item.deterministicText().contains(expectedValue), reviewerQuestion + " should contain "
                + expectedValue);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
