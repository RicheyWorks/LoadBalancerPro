package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LocalLabPassiveReviewerChecklistMapperTest {
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final List<Path> CHECKLIST_SOURCES = List.of(
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabPassiveReviewerChecklistItem.java"),
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabPassiveReviewerChecklist.java"),
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabPassiveReviewerChecklistMapper.java"));
    private static final List<String> REQUIRED_QUESTIONS = List.of(
            "What scenario was modeled?",
            "What backend behavior was represented?",
            "What backend ids were observed?",
            "What request labels were observed?",
            "What response status labels were observed?",
            "What latency labels were observed?",
            "What error/load labels were observed?",
            "What evidence was present?",
            "What safety boundary was stated?",
            "What was not proven?",
            "Why is this still not production proof?");

    @Test
    void everyPassiveTranscriptSummaryHasAReviewerChecklist() {
        List<LocalLabPassiveTranscriptSummary> summaries = LocalLabPassiveTranscriptSummaryRenderer.summaries();
        List<LocalLabPassiveReviewerChecklist> checklists = LocalLabPassiveReviewerChecklistMapper.checklists();

        assertEquals(summaries.stream().map(LocalLabPassiveTranscriptSummary::scenarioId).toList(),
                checklists.stream().map(LocalLabPassiveReviewerChecklist::scenarioId).toList());
        assertEquals(summaries.stream().map(LocalLabPassiveTranscriptSummary::transcriptId).toList(),
                checklists.stream().map(LocalLabPassiveReviewerChecklist::transcriptId).toList());

        for (LocalLabPassiveReviewerChecklist checklist : checklists) {
            assertFalse(checklist.items().isEmpty());
            assertEquals(REQUIRED_QUESTIONS.size(), checklist.items().size());
        }
    }

    @Test
    void checklistOrderingAndIdsAreDeterministicStableUniqueAndImmutable() {
        List<LocalLabPassiveReviewerChecklist> checklists = LocalLabPassiveReviewerChecklistMapper.checklists();
        List<String> checklistIds = checklists.stream().map(LocalLabPassiveReviewerChecklist::checklistId)
                .toList();

        assertEquals(List.of(
                "checklist-transcript-backend-healthy-fast",
                "checklist-transcript-backend-slow-tail-latency",
                "checklist-transcript-backend-partial-degradation",
                "checklist-transcript-backend-error-prone",
                "checklist-transcript-backend-overloaded-queue-pressure",
                "checklist-transcript-backend-all-unhealthy-no-good-choice",
                "checklist-transcript-backend-recovery"), checklistIds);
        assertEquals(checklistIds.size(), Set.copyOf(checklistIds).size());
        assertEquals(checklistIds, LocalLabPassiveReviewerChecklistMapper.checklists().stream()
                .map(LocalLabPassiveReviewerChecklist::checklistId)
                .toList());
        assertThrows(UnsupportedOperationException.class, () -> checklists.add(checklists.get(0)));

        for (LocalLabPassiveReviewerChecklist checklist : checklists) {
            assertThrows(UnsupportedOperationException.class, () -> checklist.items().add(
                    checklist.items().get(0)));
            List<String> itemIds = checklist.items().stream()
                    .map(LocalLabPassiveReviewerChecklistItem::checklistItemId)
                    .toList();
            assertEquals(itemIds.size(), Set.copyOf(itemIds).size());
            assertEquals(REQUIRED_QUESTIONS, checklist.items().stream()
                    .map(LocalLabPassiveReviewerChecklistItem::reviewerQuestion)
                    .toList());
        }
    }

    @Test
    void eachChecklistIncludesScenarioTranscriptEvidenceSafetyAndNotProvenValues() {
        List<LocalLabPassiveTranscriptSummary> summaries = LocalLabPassiveTranscriptSummaryRenderer.summaries();
        List<LocalLabPassiveReviewerChecklist> checklists = LocalLabPassiveReviewerChecklistMapper.checklists();

        for (int i = 0; i < summaries.size(); i++) {
            LocalLabPassiveTranscriptSummary summary = summaries.get(i);
            LocalLabPassiveReviewerChecklist checklist = checklists.get(i);

            assertEquals(summary.scenarioId(), checklist.scenarioId());
            assertEquals(summary.transcriptId(), checklist.transcriptId());

            assertItemContains(checklist, "What scenario was modeled?", summary.scenarioId());
            assertItemContains(checklist, "What backend behavior was represented?", summary.scenarioBehaviorType());
            assertItemContains(checklist, "What backend ids were observed?",
                    String.join(" | ", summary.backendIdsObserved()));
            assertItemContains(checklist, "What request labels were observed?",
                    String.join(" | ", summary.requestLabelsObserved()));
            assertItemContains(checklist, "What response status labels were observed?",
                    String.join(" | ", summary.responseStatusLabelsObserved()));
            assertItemContains(checklist, "What latency labels were observed?",
                    String.join(" | ", summary.latencyLabelsObserved()));
            assertItemContains(checklist, "What error/load labels were observed?",
                    String.join(" | ", summary.errorLoadLabelsObserved()));
            assertItemContains(checklist, "What evidence was present?", summary.evidenceNoteSummary());
            assertItemContains(checklist, "What safety boundary was stated?", summary.safetyBoundarySummary());
            assertItemContains(checklist, "What was not proven?", summary.notProvenBoundarySummary());
            assertItemContains(checklist, "Why is this still not production proof?",
                    "Local simulation is useful reviewer context");

            for (LocalLabPassiveReviewerChecklistItem item : checklist.items()) {
                assertEquals(summary.scenarioId(), item.scenarioId());
                assertEquals(summary.transcriptId(), item.transcriptId());
                assertFalse(item.evidenceExpectation().isBlank());
                assertTrue(item.evidenceExpectation().contains("Reviewer should confirm"));
                assertTrue(item.safetyBoundary().contains(summary.safetyBoundarySummary()));
                assertTrue(item.notProvenBoundary().contains(summary.notProvenBoundarySummary()));
                assertTrue(item.notProvenBoundary().contains("local simulation is not production proof"));
                assertTrue(List.of("PRESENT", "BOUNDARY_ONLY", "NOT_PROVEN").contains(
                        item.reviewerDispositionLabel()));
            }
        }
    }

    @Test
    void mapperOutputIsDeterministicAcrossRepeatedCalls() {
        List<LocalLabPassiveReviewerChecklist> first = LocalLabPassiveReviewerChecklistMapper.checklists();
        List<LocalLabPassiveReviewerChecklist> second = LocalLabPassiveReviewerChecklistMapper.checklists();

        assertEquals(first, second);
        assertEquals(first.stream().map(LocalLabPassiveReviewerChecklist::deterministicText).toList(),
                second.stream().map(LocalLabPassiveReviewerChecklist::deterministicText).toList());
        assertEquals(LocalLabPassiveTranscriptSummaryRenderer.summaries(),
                LocalLabPassiveTranscriptSummaryRenderer.summaries());
    }

    @Test
    void checklistsAvoidValidationExecutionReportStorageExportAndRuntimeOverclaims() {
        for (LocalLabPassiveReviewerChecklist checklist : LocalLabPassiveReviewerChecklistMapper.checklists()) {
            String normalized = checklist.deterministicText().toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "production-ready",
                    "production certified",
                    "production certification is proven",
                    "production validation is complete",
                    "live-cloud validated",
                    "live-cloud validation is complete",
                    "real-tenant validated",
                    "real-tenant validation is complete",
                    "docker compose is implemented",
                    "k6 scenario is implemented",
                    "bruno collection is implemented",
                    "toxiproxy config is implemented",
                    "prometheus/grafana dashboard is implemented",
                    "fake backend server is implemented",
                    "actual traffic is generated",
                    "http request is executed",
                    "replay execution is implemented",
                    "evidence report is generated",
                    "report generation is implemented",
                    "storage is implemented",
                    "export behavior is implemented",
                    "runtime behavior is changed",
                    "routing behavior is changed")) {
                assertFalse(normalized.contains(forbidden), checklist.checklistId() + " must not overclaim "
                        + forbidden);
            }
        }
    }

    @Test
    void checklistMapperSourcesStayPassiveAndAvoidRuntimeSideEffects() throws Exception {
        for (Path source : CHECKLIST_SOURCES) {
            String text = Files.readString(source, StandardCharsets.UTF_8);
            String normalized = text.toLowerCase(Locale.ROOT);

            for (String forbidden : List.of(
                    "thread.sleep",
                    "java.time",
                    "random",
                    "uuid",
                    "system.getenv",
                    "system.getproperty",
                    "currenttimemillis",
                    "nanotime",
                    "processbuilder",
                    "runtime.getruntime",
                    "files.",
                    "path.",
                    "java.io",
                    "socket",
                    "serversocket",
                    "httpclient",
                    "urlconnection",
                    "localhost",
                    "127.0.0.1",
                    "http://",
                    "https://",
                    "new thread",
                    "executor")) {
                assertFalse(normalized.contains(forbidden), source + " must not use " + forbidden);
            }
        }
    }

    @Test
    void docsDescribePassiveReviewerChecklistMapperSprintAsTestScopeOnly() throws Exception {
        String adr = read(ADR_0009);
        String matrix = read(MATRIX);
        String trustMap = read(TRUST_MAP);

        for (String doc : List.of(adr, matrix, trustMap)) {
            assertTrue(doc.contains("This PR adds a test-scope passive reviewer checklist mapper only."));
            assertTrue(doc.contains(
                    "The mapper turns existing passive transcript summaries into in-memory reviewer checklist entries."));
            assertTrue(doc.contains("It does not execute replay."));
            assertTrue(doc.contains("It does not generate evidence reports."));
            assertTrue(doc.contains("It does not write files."));
            assertTrue(doc.contains("It does not persist storage."));
            assertTrue(doc.contains("It does not export/download/upload/PDF/ZIP anything."));
            assertTrue(doc.contains("It does not implement fake backend servers."));
            assertTrue(doc.contains(
                    "It does not start listeners, open ports, call localhost, generate traffic, or run tools."));
            assertTrue(doc.contains("Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling."));
            assertTrue(doc.contains("This is still not production proof."));
        }
    }

    private static void assertItemContains(
            LocalLabPassiveReviewerChecklist checklist,
            String reviewerQuestion,
            String expectedValue) {
        LocalLabPassiveReviewerChecklistItem item = checklist.items().stream()
                .filter(candidate -> candidate.reviewerQuestion().equals(reviewerQuestion))
                .findFirst()
                .orElseThrow();
        assertTrue(item.observedSummaryValue().contains(expectedValue), reviewerQuestion + " should contain "
                + expectedValue);
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
