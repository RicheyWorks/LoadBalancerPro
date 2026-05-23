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

class LocalLabTrafficSmokeReviewerChecklistMapperTest {
    private static final List<String> REQUIRED_QUESTIONS = List.of(
            "Did smoke traffic stay loopback-only?",
            "Did smoke traffic use harness-assigned ephemeral ports?",
            "Which scenarios and profiles were covered?",
            "Which backend ids were covered?",
            "How many smoke responses matched expected fixtures?",
            "How many boundary responses were observed?",
            "Were evidence labels present in the smoke chain?",
            "Were safety boundary labels present?",
            "Were not-proven boundaries present?",
            "Why is local loopback smoke not production proof?",
            "Did the smoke summary avoid tool implementation claims?",
            "Did the smoke summary avoid replay/report/storage/export claims?",
            "What is the next safe step?");

    @Test
    void everySmokeSummaryProducesReviewerChecklist() throws Exception {
        LocalLabLoopbackTrafficSmokeSummary summary = smokeSummary();
        LocalLabTrafficSmokeReviewerChecklist checklist =
                LocalLabTrafficSmokeReviewerChecklistMapper.checklist(summary);
        List<LocalLabTrafficSmokeReviewerChecklist> checklists =
                LocalLabTrafficSmokeReviewerChecklistMapper.checklists(List.of(summary));

        assertEquals(List.of(checklist), checklists);
        assertEquals("checklist-summary-local-lab-loopback-traffic-smoke", checklist.checklistId());
        assertEquals(summary.summaryId(), checklist.smokeSummaryId());
        assertEquals(REQUIRED_QUESTIONS.size(), checklist.items().size());
        assertEquals(REQUIRED_QUESTIONS, checklist.items().stream()
                .map(LocalLabTrafficSmokeReviewerChecklistItem::reviewerQuestion)
                .toList());
    }

    @Test
    void checklistOrderingAndIdsAreDeterministicStableUniqueAndImmutable() throws Exception {
        LocalLabTrafficSmokeReviewerChecklist first =
                LocalLabTrafficSmokeReviewerChecklistMapper.checklist(smokeSummary());
        LocalLabTrafficSmokeReviewerChecklist second =
                LocalLabTrafficSmokeReviewerChecklistMapper.checklist(smokeSummary());
        List<String> itemIds = first.items().stream()
                .map(LocalLabTrafficSmokeReviewerChecklistItem::itemId)
                .toList();

        assertEquals(first, second);
        assertEquals(first.deterministicText(), second.deterministicText());
        assertEquals(itemIds.size(), Set.copyOf(itemIds).size());
        assertEquals(List.of(
                "checklist-item-summary-local-lab-loopback-traffic-smoke-loopback-only-target-confirmation",
                "checklist-item-summary-local-lab-loopback-traffic-smoke-ephemeral-port-confirmation",
                "checklist-item-summary-local-lab-loopback-traffic-smoke-scenario-profile-coverage",
                "checklist-item-summary-local-lab-loopback-traffic-smoke-backend-coverage",
                "checklist-item-summary-local-lab-loopback-traffic-smoke-matched-fixture-count",
                "checklist-item-summary-local-lab-loopback-traffic-smoke-boundary-response-count",
                "checklist-item-summary-local-lab-loopback-traffic-smoke-evidence-label-presence",
                "checklist-item-summary-local-lab-loopback-traffic-smoke-safety-boundary-presence",
                "checklist-item-summary-local-lab-loopback-traffic-smoke-not-proven-boundary-presence",
                "checklist-item-summary-local-lab-loopback-traffic-smoke-no-production-proof-warning",
                "checklist-item-summary-local-lab-loopback-traffic-smoke-no-docker-k6-bruno-toxiproxy-warning",
                "checklist-item-summary-local-lab-loopback-traffic-smoke-no-replay-report-storage-export-warning",
                "checklist-item-summary-local-lab-loopback-traffic-smoke-next-safe-step-recommendation"), itemIds);
        assertThrows(UnsupportedOperationException.class, () -> first.items().add(first.items().get(0)));
    }

    @Test
    void checklistIncludesLoopbackEphemeralScenarioBackendAndCountChecks() throws Exception {
        LocalLabLoopbackTrafficSmokeSummary summary = smokeSummary();
        LocalLabTrafficSmokeReviewerChecklist checklist =
                LocalLabTrafficSmokeReviewerChecklistMapper.checklist(summary);

        assertItemContains(checklist, "Did smoke traffic stay loopback-only?", "127.0.0.1 loopback-only");
        assertItemContains(checklist, "Did smoke traffic use harness-assigned ephemeral ports?",
                "harness-assigned ephemeral ports");
        assertItemContains(checklist, "How many smoke responses matched expected fixtures?", "7");
        assertItemContains(checklist, "How many boundary responses were observed?", "1");
        assertItemContains(checklist, "Why is local loopback smoke not production proof?",
                "not production proof");

        for (String scenarioId : LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                .map(LocalLabFakeBackendResponseFixture::scenarioId)
                .toList()) {
            assertItemContains(checklist, "Which scenarios and profiles were covered?", scenarioId);
        }
        assertItemContains(checklist, "Which scenarios and profiles were covered?",
                "unknown-loopback-traffic-smoke-scenario");

        for (String backendId : LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                .map(LocalLabFakeBackendResponseFixture::backendId)
                .toList()) {
            assertItemContains(checklist, "Which backend ids were covered?", backendId);
        }
        assertItemContains(checklist, "Which backend ids were covered?",
                "unknown-loopback-traffic-smoke-backend");
    }

    @Test
    void checklistIncludesEvidenceSafetyNotProvenToolReplayStorageExportAndNextStepBoundaries() throws Exception {
        LocalLabTrafficSmokeReviewerChecklist checklist =
                LocalLabTrafficSmokeReviewerChecklistMapper.checklist(smokeSummary());
        String normalized = normalize(checklist.deterministicText());

        assertItemContains(checklist, "Were evidence labels present in the smoke chain?", "evidence labels");
        assertItemContains(checklist, "Were safety boundary labels present?", "safety boundary labels");
        assertItemContains(checklist, "Were not-proven boundaries present?", "Not-proven");
        assertItemContains(checklist, "Did the smoke summary avoid tool implementation claims?",
                "Docker/k6/Bruno/Toxiproxy");
        assertItemContains(checklist, "Did the smoke summary avoid replay/report/storage/export claims?",
                "replay execution");
        assertItemContains(checklist, "What is the next safe step?", "separately scoped local-lab step");
        assertTrue(normalized.contains("local simulation is not production proof"));
        assertTrue(normalized.contains("not production certification"));
        assertTrue(normalized.contains("not live-cloud validation"));
        assertTrue(normalized.contains("not real-tenant validation"));
        assertTrue(normalized.contains("not runtime enforcement"));
        assertTrue(normalized.contains("no docker/k6/bruno/toxiproxy implementation"));
        assertTrue(normalized.contains("no replay execution"));
        assertTrue(normalized.contains("evidence/report generation"));
        assertTrue(normalized.contains("storage"));
        assertTrue(normalized.contains("export"));

        for (LocalLabTrafficSmokeReviewerChecklistItem item : checklist.items()) {
            assertEquals(checklist.smokeSummaryId(), item.smokeSummaryId());
            assertTrue(List.of("PRESENT", "BOUNDARY_ONLY", "NOT_PROVEN").contains(item.dispositionLabel()));
            assertFalse(item.safetyBoundary().isBlank());
            assertFalse(item.notProvenBoundary().isBlank());
        }
    }

    @Test
    void mapperRejectsMissingInputAndOutputIsDeterministicAcrossRepeatedCalls() throws Exception {
        LocalLabLoopbackTrafficSmokeSummary summary = smokeSummary();

        assertThrows(IllegalArgumentException.class, () -> LocalLabTrafficSmokeReviewerChecklistMapper.checklist(null));
        assertThrows(IllegalArgumentException.class, () -> LocalLabTrafficSmokeReviewerChecklistMapper.checklists(null));
        assertThrows(IllegalArgumentException.class, () -> LocalLabTrafficSmokeReviewerChecklistMapper.checklists(
                List.of()));
        assertEquals(LocalLabTrafficSmokeReviewerChecklistMapper.checklist(summary),
                LocalLabTrafficSmokeReviewerChecklistMapper.checklist(summary));
        assertEquals(LocalLabTrafficSmokeReviewerChecklistMapper.checklists(List.of(summary)),
                LocalLabTrafficSmokeReviewerChecklistMapper.checklists(List.of(summary)));
    }

    @Test
    void mapperDoesNotImplyNetworkToolReplayReportStorageExportRuntimeOrValidationClaims() throws Exception {
        String normalized = normalize(LocalLabTrafficSmokeReviewerChecklistMapper.checklist(smokeSummary())
                .deterministicText());

        for (String forbidden : List.of(
                "production-ready",
                "production certified",
                "production validation is complete",
                "live-cloud validated",
                "live-cloud validation is complete",
                "real-tenant validated",
                "real-tenant validation is complete",
                "runtime enforcement is implemented",
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

    private static LocalLabLoopbackTrafficSmokeSummary smokeSummary() throws Exception {
        List<String> scenarioIds = new ArrayList<>(LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                .map(LocalLabFakeBackendResponseFixture::scenarioId)
                .toList());
        scenarioIds.add("unknown-loopback-traffic-smoke-scenario");
        List<String> backendIds = new ArrayList<>(LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                .map(LocalLabFakeBackendResponseFixture::backendId)
                .toList());
        backendIds.add("unknown-loopback-traffic-smoke-backend");

        return new LocalLabLoopbackTrafficSmokeSummary(
                "summary-local-lab-loopback-traffic-smoke",
                8,
                7,
                1,
                scenarioIds,
                backendIds,
                "All smoke observations targeted 127.0.0.1 loopback-only harness URLs.",
                "All smoke observations used positive harness-assigned ephemeral ports and no common fixed ports.",
                "Deterministic loopback smoke observations are local test-scope context only; not production "
                        + "proof; not production certification; not live-cloud validation; not real-tenant "
                        + "validation.",
                "Not proven: Docker/k6/Bruno/Toxiproxy implementation, replay execution, evidence/report "
                        + "generation, file writing, storage, export/download/upload/PDF/ZIP behavior, runtime "
                        + "behavior, production routing/proxy/scoring/API behavior, production endpoint behavior, "
                        + "live-cloud validation, real-tenant validation, production certification, or runtime "
                        + "enforcement.",
                "Use the in-memory smoke summary as reviewer context for a separately scoped local-lab step; "
                        + "keep future tool, replay, report, storage, export, and production validation work "
                        + "separately reviewed.");
    }

    private static void assertItemContains(
            LocalLabTrafficSmokeReviewerChecklist checklist,
            String reviewerQuestion,
            String expectedValue) {
        LocalLabTrafficSmokeReviewerChecklistItem item = checklist.items().stream()
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
