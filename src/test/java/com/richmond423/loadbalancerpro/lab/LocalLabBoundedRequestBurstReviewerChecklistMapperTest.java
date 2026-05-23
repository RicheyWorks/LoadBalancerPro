package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LocalLabBoundedRequestBurstReviewerChecklistMapperTest {
    private static final List<String> REQUIRED_QUESTIONS = List.of(
            "How many bounded burst cases were exercised?",
            "How many total bounded burst requests were issued?",
            "What fixed repetition count was used?",
            "Which scenarios and profiles were covered by the bounded burst?",
            "Which backend ids were covered by the bounded burst?",
            "How many bounded burst responses matched expected fixtures?",
            "How many bounded burst boundary responses were observed?",
            "Did the bounded burst stay loopback-only?",
            "Did the bounded burst use harness-assigned ephemeral ports?",
            "Is the bounded burst output deterministic?",
            "Why is the bounded burst not production proof?",
            "Did the bounded burst avoid load, stress, and benchmark claims?",
            "Did the bounded burst avoid throughput and p95/p99 evidence claims?",
            "Did the bounded burst avoid external tooling implementation claims?",
            "Did the bounded burst avoid replay/report/storage/export claims?",
            "What is the next safe step?");

    @Test
    void everyBoundedBurstSummaryProducesReviewerChecklist() {
        LocalLabBoundedRequestBurstSummary summary = burstSummary();
        LocalLabBoundedRequestBurstReviewerChecklist checklist =
                LocalLabBoundedRequestBurstReviewerChecklistMapper.checklist(summary);
        List<LocalLabBoundedRequestBurstReviewerChecklist> checklists =
                LocalLabBoundedRequestBurstReviewerChecklistMapper.checklists(List.of(summary));

        assertEquals(List.of(checklist), checklists);
        assertEquals("checklist-summary-local-lab-bounded-request-burst-smoke", checklist.checklistId());
        assertEquals(summary.summaryId(), checklist.burstSummaryId());
        assertEquals(REQUIRED_QUESTIONS.size(), checklist.items().size());
        assertEquals(REQUIRED_QUESTIONS, checklist.items().stream()
                .map(LocalLabBoundedRequestBurstReviewerChecklistItem::reviewerQuestion)
                .toList());
    }

    @Test
    void checklistOrderingAndIdsAreDeterministicStableUniqueAndImmutable() {
        LocalLabBoundedRequestBurstReviewerChecklist first =
                LocalLabBoundedRequestBurstReviewerChecklistMapper.checklist(burstSummary());
        LocalLabBoundedRequestBurstReviewerChecklist second =
                LocalLabBoundedRequestBurstReviewerChecklistMapper.checklist(burstSummary());
        List<String> itemIds = first.items().stream()
                .map(LocalLabBoundedRequestBurstReviewerChecklistItem::itemId)
                .toList();

        assertEquals(first, second);
        assertEquals(first.deterministicText(), second.deterministicText());
        assertEquals(itemIds.size(), Set.copyOf(itemIds).size());
        assertEquals(List.of(
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-burst-case-count",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-total-request-count",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-fixed-repetition-count",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-scenario-profile-coverage",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-backend-coverage",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-matched-fixture-count",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-boundary-response-count",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-loopback-only-confirmation",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-ephemeral-port-confirmation",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-deterministic-output-confirmation",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-no-production-proof-warning",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-no-load-stress-benchmark-warning",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-no-throughput-p95-p99-evidence-warning",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-no-docker-k6-bruno-toxiproxy-warning",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-no-replay-report-storage-export-warning",
                "checklist-item-summary-local-lab-bounded-request-burst-smoke-next-safe-step-recommendation"),
                itemIds);
        assertThrows(UnsupportedOperationException.class, () -> first.items().add(first.items().get(0)));
    }

    @Test
    void checklistIncludesBurstCountsCoverageLoopbackEphemeralAndDeterministicChecks() {
        LocalLabBoundedRequestBurstReviewerChecklist checklist =
                LocalLabBoundedRequestBurstReviewerChecklistMapper.checklist(burstSummary());

        assertItemContains(checklist, "How many bounded burst cases were exercised?", "8");
        assertItemContains(checklist, "How many total bounded burst requests were issued?", "16");
        assertItemContains(checklist, "What fixed repetition count was used?", "2");
        assertItemContains(checklist, "How many bounded burst responses matched expected fixtures?", "14");
        assertItemContains(checklist, "How many bounded burst boundary responses were observed?", "2");
        assertItemContains(checklist, "Did the bounded burst stay loopback-only?", "127.0.0.1 loopback-only");
        assertItemContains(checklist, "Did the bounded burst use harness-assigned ephemeral ports?",
                "harness-assigned ephemeral ports");
        assertItemContains(checklist, "Is the bounded burst output deterministic?", "deterministic in memory");

        for (String scenarioId : LocalLabBoundedRequestBurstCatalog.cases().stream()
                .map(LocalLabBoundedRequestBurstCase::scenarioId)
                .toList()) {
            assertItemContains(checklist, "Which scenarios and profiles were covered by the bounded burst?",
                    scenarioId);
        }

        for (String backendId : LocalLabBoundedRequestBurstCatalog.cases().stream()
                .map(LocalLabBoundedRequestBurstCase::backendId)
                .toList()) {
            assertItemContains(checklist, "Which backend ids were covered by the bounded burst?", backendId);
        }
    }

    @Test
    void checklistIncludesNoProductionProofLoadBenchmarkToolReplayStorageExportAndNextStepBoundaries() {
        LocalLabBoundedRequestBurstReviewerChecklist checklist =
                LocalLabBoundedRequestBurstReviewerChecklistMapper.checklist(burstSummary());
        String normalized = normalize(checklist.deterministicText());

        assertItemContains(checklist, "Why is the bounded burst not production proof?", "not production proof");
        assertItemContains(checklist, "Did the bounded burst avoid load, stress, and benchmark claims?",
                "not load testing");
        assertItemContains(checklist, "Did the bounded burst avoid throughput and p95/p99 evidence claims?",
                "not throughput evidence");
        assertItemContains(checklist, "Did the bounded burst avoid external tooling implementation claims?",
                "Docker/k6/Bruno/Toxiproxy");
        assertItemContains(checklist, "Did the bounded burst avoid replay/report/storage/export claims?",
                "replay execution");
        assertItemContains(checklist, "What is the next safe step?", "separately scoped");
        assertTrue(normalized.contains("local simulation is not production proof"));
        assertTrue(normalized.contains("not load testing"));
        assertTrue(normalized.contains("not stress testing"));
        assertTrue(normalized.contains("not benchmarking"));
        assertTrue(normalized.contains("not throughput evidence"));
        assertTrue(normalized.contains("not p95/p99 evidence"));
        assertTrue(normalized.contains("not production certification"));
        assertTrue(normalized.contains("not live-cloud validation"));
        assertTrue(normalized.contains("not real-tenant validation"));
        assertTrue(normalized.contains("not runtime enforcement"));
        assertTrue(normalized.contains("no docker/k6/bruno/toxiproxy implementation"));
        assertTrue(normalized.contains("no replay execution"));
        assertTrue(normalized.contains("evidence/report generation"));
        assertTrue(normalized.contains("storage"));
        assertTrue(normalized.contains("export"));

        for (LocalLabBoundedRequestBurstReviewerChecklistItem item : checklist.items()) {
            assertEquals(checklist.burstSummaryId(), item.burstSummaryId());
            assertTrue(List.of("PRESENT", "BOUNDARY_ONLY", "NOT_PROVEN").contains(item.dispositionLabel()));
            assertFalse(item.safetyBoundary().isBlank());
            assertFalse(item.notProvenBoundary().isBlank());
        }
    }

    @Test
    void mapperRejectsMissingInputAndOutputIsDeterministicAcrossRepeatedCalls() {
        LocalLabBoundedRequestBurstSummary summary = burstSummary();

        assertThrows(IllegalArgumentException.class,
                () -> LocalLabBoundedRequestBurstReviewerChecklistMapper.checklist(null));
        assertThrows(IllegalArgumentException.class,
                () -> LocalLabBoundedRequestBurstReviewerChecklistMapper.checklists(null));
        assertThrows(IllegalArgumentException.class,
                () -> LocalLabBoundedRequestBurstReviewerChecklistMapper.checklists(List.of()));
        assertEquals(LocalLabBoundedRequestBurstReviewerChecklistMapper.checklist(summary),
                LocalLabBoundedRequestBurstReviewerChecklistMapper.checklist(summary));
        assertEquals(LocalLabBoundedRequestBurstReviewerChecklistMapper.checklists(List.of(summary)),
                LocalLabBoundedRequestBurstReviewerChecklistMapper.checklists(List.of(summary)));
    }

    @Test
    void mapperDoesNotCallNetworkOrImplyProductionToolReplayReportStorageExportRuntimeBenchmarkOrValidationClaims() {
        String normalized = normalize(LocalLabBoundedRequestBurstReviewerChecklistMapper.checklist(burstSummary())
                .deterministicText());

        assertTrue(normalized.contains("does not call endpoints"));
        assertTrue(normalized.contains("bind servers"));
        assertTrue(normalized.contains("open ports"));
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
                "benchmarking is complete",
                "throughput benchmark",
                "p95/p99 benchmark",
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

    private static LocalLabBoundedRequestBurstSummary burstSummary() {
        List<LocalLabBoundedRequestBurstObservation> observations = new ArrayList<>();
        for (LocalLabBoundedRequestBurstCase burstCase : LocalLabBoundedRequestBurstCatalog.cases()) {
            for (int repetition = 1; repetition <= burstCase.fixedRepetitionCount(); repetition++) {
                observations.add(observation(burstCase, repetition));
            }
        }
        return LocalLabBoundedRequestBurstSummaryRenderer.summarize(observations);
    }

    private static LocalLabBoundedRequestBurstObservation observation(
            LocalLabBoundedRequestBurstCase burstCase,
            int repetitionIndex) {
        return new LocalLabBoundedRequestBurstObservation(
                burstCase.burstCaseId(),
                burstCase.scenarioId(),
                burstCase.backendId(),
                repetitionIndex,
                "127.0.0.1",
                49152,
                burstCase.expectedStatusCode(),
                !burstCase.boundaryCase(),
                true,
                true,
                true,
                burstCase.boundaryCase(),
                "Test-scope deterministic bounded request burst smoke only; fixed small request counts; "
                        + "not k6; not Bruno; not Docker; not Toxiproxy; not load testing; "
                        + "not stress testing; not benchmarking; not throughput evidence; "
                        + "not p95/p99 evidence; not production traffic; not production proof; "
                        + "no fixed ports; no non-loopback target calls; no replay execution; "
                        + "no evidence/report generation; no file writing; no environment reads; "
                        + "no process execution; no storage; no export behavior.");
    }

    private static void assertItemContains(
            LocalLabBoundedRequestBurstReviewerChecklist checklist,
            String reviewerQuestion,
            String expectedValue) {
        LocalLabBoundedRequestBurstReviewerChecklistItem item = checklist.items().stream()
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
