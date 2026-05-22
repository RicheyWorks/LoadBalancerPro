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

class LocalLabPassiveTranscriptSummaryRendererTest {
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final List<Path> RENDERER_SOURCES = List.of(
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabPassiveTranscriptSummary.java"),
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabPassiveTranscriptSummaryRenderer.java"));

    @Test
    void everyPassiveTranscriptScenarioHasASummary() {
        List<LocalLabPassiveTranscriptScenario> transcripts = LocalLabPassiveTranscriptCatalog.transcripts();
        List<LocalLabPassiveTranscriptSummary> summaries = LocalLabPassiveTranscriptSummaryRenderer.summaries();

        assertEquals(transcripts.stream().map(LocalLabPassiveTranscriptScenario::scenarioId).toList(),
                summaries.stream().map(LocalLabPassiveTranscriptSummary::scenarioId).toList());
        assertEquals(transcripts.stream().map(LocalLabPassiveTranscriptScenario::transcriptId).toList(),
                summaries.stream().map(LocalLabPassiveTranscriptSummary::transcriptId).toList());
        assertEquals(transcripts.stream().map(LocalLabPassiveTranscriptScenario::behaviorType).toList(),
                summaries.stream().map(LocalLabPassiveTranscriptSummary::scenarioBehaviorType).toList());
    }

    @Test
    void summaryOrderingAndIdsAreDeterministicStableUniqueAndImmutable() {
        List<LocalLabPassiveTranscriptSummary> summaries = LocalLabPassiveTranscriptSummaryRenderer.summaries();
        List<String> summaryIds = summaries.stream().map(LocalLabPassiveTranscriptSummary::summaryId).toList();

        assertEquals(List.of(
                "summary-transcript-backend-healthy-fast",
                "summary-transcript-backend-slow-tail-latency",
                "summary-transcript-backend-partial-degradation",
                "summary-transcript-backend-error-prone",
                "summary-transcript-backend-overloaded-queue-pressure",
                "summary-transcript-backend-all-unhealthy-no-good-choice",
                "summary-transcript-backend-recovery"), summaryIds);
        assertEquals(summaryIds.size(), Set.copyOf(summaryIds).size());
        assertEquals(summaryIds, LocalLabPassiveTranscriptSummaryRenderer.summaries().stream()
                .map(LocalLabPassiveTranscriptSummary::summaryId)
                .toList());
        assertThrows(UnsupportedOperationException.class, () -> summaries.add(summaries.get(0)));

        for (LocalLabPassiveTranscriptSummary summary : summaries) {
            assertThrows(UnsupportedOperationException.class, () -> summary.backendIdsObserved().add("extra"));
            assertThrows(UnsupportedOperationException.class, () -> summary.requestLabelsObserved().add("extra"));
            assertThrows(UnsupportedOperationException.class, () -> summary.responseStatusLabelsObserved()
                    .add("extra"));
            assertThrows(UnsupportedOperationException.class, () -> summary.latencyLabelsObserved().add("extra"));
            assertThrows(UnsupportedOperationException.class, () -> summary.errorLoadLabelsObserved().add("extra"));
        }
    }

    @Test
    void summariesCarryExpectedTranscriptObservationFields() {
        List<LocalLabPassiveTranscriptScenario> transcripts = LocalLabPassiveTranscriptCatalog.transcripts();
        List<LocalLabPassiveTranscriptSummary> summaries = LocalLabPassiveTranscriptSummaryRenderer.summaries();

        for (int i = 0; i < transcripts.size(); i++) {
            LocalLabPassiveTranscriptScenario transcript = transcripts.get(i);
            LocalLabPassiveTranscriptSummary summary = summaries.get(i);

            assertEquals(transcript.scenarioId(), summary.scenarioId());
            assertEquals(transcript.behaviorType(), summary.scenarioBehaviorType());
            assertEquals(transcript.transcriptId(), summary.transcriptId());
            assertEquals(transcript.entries().size(), summary.orderedStepCount());
            assertEquals(transcript.entries().stream().map(LocalLabPassiveTranscriptEntry::backendId).toList(),
                    summary.backendIdsObserved());
            assertEquals(transcript.entries().stream()
                    .map(entry -> entry.simulatedRequestMethodLabel() + " " + entry.simulatedRequestPathLabel())
                    .toList(), summary.requestLabelsObserved());
            assertEquals(transcript.entries().stream()
                    .map(entry -> entry.expectedResponseStatusCode() + " status label")
                    .toList(), summary.responseStatusLabelsObserved());
            assertEquals(transcript.entries().stream().map(LocalLabPassiveTranscriptEntry::expectedLatencyLabel)
                    .toList(), summary.latencyLabelsObserved());
            assertEquals(transcript.entries().stream()
                    .map(entry -> entry.expectedErrorLabel() + " | " + entry.expectedLoadLabel())
                    .toList(), summary.errorLoadLabelsObserved());

            for (LocalLabPassiveTranscriptEntry entry : transcript.entries()) {
                assertTrue(summary.evidenceNoteSummary().contains(entry.routingEvidenceObservationNote()));
                assertTrue(summary.safetyBoundarySummary().contains(entry.safetyBoundaryNote()));
                assertTrue(summary.notProvenBoundarySummary().contains(entry.notProvenBoundary()));
            }
        }
    }

    @Test
    void summariesCarrySafetyAndNotProvenBoundariesWithoutRuntimeClaims() {
        for (LocalLabPassiveTranscriptSummary summary : LocalLabPassiveTranscriptSummaryRenderer.summaries()) {
            assertFalse(summary.evidenceNoteSummary().isBlank());
            assertFalse(summary.safetyBoundarySummary().isBlank());
            assertTrue(summary.safetyBoundarySummary().contains("observe-only"));
            assertTrue(summary.safetyBoundarySummary().contains("no listener"));
            assertTrue(summary.notProvenBoundarySummary().contains("not production proof"));
            assertTrue(summary.notProvenBoundarySummary().contains("not live-cloud validation"));
            assertTrue(summary.notProvenBoundarySummary().contains("not real-tenant validation"));
            assertTrue(summary.notProvenBoundarySummary().contains(
                    "no Docker, k6, Bruno, or Toxiproxy execution is required"));
            assertTrue(summary.notProvenBoundarySummary().contains("no fake backend server"));
            assertTrue(summary.notProvenBoundarySummary().contains("listener"));
            assertTrue(summary.notProvenBoundarySummary().contains("port"));
            assertTrue(summary.notProvenBoundarySummary().contains("loopback HTTP call"));
            assertTrue(summary.notProvenBoundarySummary().contains("generated traffic"));
            assertTrue(summary.notProvenBoundarySummary().contains("replay execution"));
            assertTrue(summary.notProvenBoundarySummary().contains("evidence report generation"));
            assertTrue(summary.notProvenBoundarySummary().contains("file writing"));
            assertTrue(summary.notProvenBoundarySummary().contains("storage"));
            assertTrue(summary.notProvenBoundarySummary().contains("export"));
            assertTrue(summary.notProvenBoundarySummary().contains("runtime behavior"));
        }
    }

    @Test
    void rendererOutputIsDeterministicAcrossRepeatedCalls() {
        List<LocalLabPassiveTranscriptSummary> first = LocalLabPassiveTranscriptSummaryRenderer.summaries();
        List<LocalLabPassiveTranscriptSummary> second = LocalLabPassiveTranscriptSummaryRenderer.summaries();

        assertEquals(first, second);
        assertEquals(first.stream().map(LocalLabPassiveTranscriptSummary::deterministicText).toList(),
                second.stream().map(LocalLabPassiveTranscriptSummary::deterministicText).toList());
        assertEquals(LocalLabPassiveTranscriptCatalog.transcripts().stream()
                .map(LocalLabPassiveTranscriptScenario::transcriptId)
                .toList(), LocalLabPassiveTranscriptCatalog.transcripts().stream()
                .map(LocalLabPassiveTranscriptScenario::transcriptId)
                .toList());
    }

    @Test
    void summariesAvoidValidationExecutionReportStorageExportAndRuntimeOverclaims() {
        for (LocalLabPassiveTranscriptSummary summary : LocalLabPassiveTranscriptSummaryRenderer.summaries()) {
            String normalized = summary.deterministicText().toLowerCase(Locale.ROOT);

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
                assertFalse(normalized.contains(forbidden), summary.summaryId() + " must not overclaim "
                        + forbidden);
            }
        }
    }

    @Test
    void rendererSourcesStayPassiveAndAvoidRuntimeSideEffects() throws Exception {
        for (Path source : RENDERER_SOURCES) {
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
    void docsDescribePassiveTranscriptSummaryRendererSprintAsTestScopeOnly() throws Exception {
        String adr = read(ADR_0009);
        String matrix = read(MATRIX);
        String trustMap = read(TRUST_MAP);

        for (String doc : List.of(adr, matrix, trustMap)) {
            assertTrue(doc.contains("This PR adds a test-scope passive transcript summary renderer only."));
            assertTrue(doc.contains("The renderer summarizes existing passive transcript fixtures in memory."));
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

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
