package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LocalLabPassiveTranscriptFixtureTest {
    private static final Path ADR_0009 =
            Path.of("docs/adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md");
    private static final Path MATRIX = Path.of("docs/LOCAL_LAB_SCENARIO_MATRIX.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final List<Path> TRANSCRIPT_SOURCES = List.of(
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabPassiveTranscriptEntry.java"),
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabPassiveTranscriptScenario.java"),
            Path.of("src/test/java/com/richmond423/loadbalancerpro/lab/LocalLabPassiveTranscriptCatalog.java"));

    @Test
    void everyScenarioInCatalogHasAPassiveTranscript() {
        List<LocalLabFakeBackendNodeScenario> scenarios = LocalLabScenarioCatalog.scenarios();
        List<LocalLabPassiveTranscriptScenario> transcripts = LocalLabPassiveTranscriptCatalog.transcripts();

        assertEquals(scenarios.stream().map(LocalLabFakeBackendNodeScenario::backendId).toList(),
                transcripts.stream().map(LocalLabPassiveTranscriptScenario::scenarioId).toList());
        assertEquals(scenarios.stream().map(LocalLabFakeBackendNodeScenario::behaviorProfile).toList(),
                transcripts.stream().map(LocalLabPassiveTranscriptScenario::behaviorProfile).toList());

        for (LocalLabFakeBackendNodeScenario scenario : scenarios) {
            LocalLabPassiveTranscriptScenario transcript =
                    LocalLabPassiveTranscriptCatalog.findByScenarioId(scenario.backendId()).orElseThrow();
            assertEquals(scenario.backendId(), transcript.scenarioId());
            assertEquals(scenario.behaviorProfile(), transcript.behaviorProfile());
            assertEquals(scenario.behaviorType(), transcript.behaviorType());
            assertFalse(transcript.entries().isEmpty());
        }
    }

    @Test
    void everyTranscriptReferencesAnExistingResponseFixture() {
        for (LocalLabPassiveTranscriptScenario transcript : LocalLabPassiveTranscriptCatalog.transcripts()) {
            LocalLabFakeBackendResponseFixture fixture =
                    LocalLabFakeBackendResponseFixtureCatalog.findByScenarioId(transcript.scenarioId()).orElseThrow();

            assertEquals(fixture.fixtureId(), transcript.fixtureId());
            assertEquals(fixture.scenarioId(), transcript.scenarioId());
            assertEquals(fixture.behaviorProfile(), transcript.behaviorProfile());

            for (LocalLabPassiveTranscriptEntry entry : transcript.entries()) {
                assertEquals(transcript.transcriptId(), entry.transcriptId());
                assertEquals(fixture.fixtureId(), entry.fixtureId());
                assertEquals(fixture.scenarioId(), entry.scenarioId());
                assertEquals(fixture.backendId(), entry.backendId());
                assertEquals(fixture.responseStatusCode(), entry.expectedResponseStatusCode());
                assertEquals(fixture.responseLatencyLabel(), entry.expectedLatencyLabel());
                assertEquals(fixture.responseBodySummary(), entry.expectedResponseBodySummary());
                assertEquals(fixture.simulatedErrorLabel(), entry.expectedErrorLabel());
                assertEquals(fixture.simulatedLoadLabel(), entry.expectedLoadLabel());
            }
        }
    }

    @Test
    void transcriptOrderingIdsAndStepNumbersAreDeterministic() {
        List<LocalLabPassiveTranscriptScenario> transcripts = LocalLabPassiveTranscriptCatalog.transcripts();
        List<String> transcriptIds = transcripts.stream().map(LocalLabPassiveTranscriptScenario::transcriptId)
                .toList();

        assertEquals(List.of(
                "transcript-backend-healthy-fast",
                "transcript-backend-slow-tail-latency",
                "transcript-backend-partial-degradation",
                "transcript-backend-error-prone",
                "transcript-backend-overloaded-queue-pressure",
                "transcript-backend-all-unhealthy-no-good-choice",
                "transcript-backend-recovery"), transcriptIds);
        assertEquals(transcriptIds.size(), Set.copyOf(transcriptIds).size());
        assertEquals(transcriptIds, LocalLabPassiveTranscriptCatalog.transcripts().stream()
                .map(LocalLabPassiveTranscriptScenario::transcriptId)
                .toList());
        assertThrows(UnsupportedOperationException.class, () -> transcripts.add(transcripts.get(0)));

        for (LocalLabPassiveTranscriptScenario transcript : transcripts) {
            List<Integer> steps = transcript.entries().stream().map(LocalLabPassiveTranscriptEntry::stepNumber)
                    .toList();
            List<Integer> sorted = new ArrayList<>(steps);
            sorted.sort(Integer::compareTo);

            assertEquals(sorted, steps);
            assertEquals(steps.size(), Set.copyOf(steps).size());
            assertEquals(1, steps.get(0));
            assertThrows(UnsupportedOperationException.class, () -> transcript.entries().add(
                    transcript.entries().get(0)));
        }
    }

    @Test
    void transcriptEntriesCarryRequestResponseEvidenceAndSafetyLabels() {
        for (LocalLabPassiveTranscriptScenario transcript : LocalLabPassiveTranscriptCatalog.transcripts()) {
            for (LocalLabPassiveTranscriptEntry entry : transcript.entries()) {
                assertFalse(entry.simulatedRequestMethodLabel().isBlank());
                assertFalse(entry.simulatedRequestPathLabel().isBlank());
                assertTrue(entry.simulatedRequestMethodLabel().contains("label"));
                assertTrue(entry.simulatedRequestPathLabel().contains("path label"));
                assertTrue(entry.expectedResponseStatusCode() >= 100);
                assertFalse(entry.expectedLatencyLabel().isBlank());
                assertFalse(entry.expectedResponseBodySummary().isBlank());
                assertFalse(entry.expectedErrorLabel().isBlank());
                assertFalse(entry.expectedLoadLabel().isBlank());
                assertFalse(entry.routingEvidenceObservationNote().isBlank());
                assertTrue(entry.routingEvidenceObservationNote().contains("Passive observation should"));
                assertFalse(entry.safetyBoundaryNote().isBlank());
                assertTrue(entry.safetyBoundaryNote().contains("Safety note: observe-only"));
                assertTrue(entry.safetyBoundaryNote().contains("no listener"));
                assertTrue(entry.safetyBoundaryNote().contains("traffic"));
                assertTrue(entry.safetyBoundaryNote().contains("replay"));
                assertTrue(entry.safetyBoundaryNote().contains("storage"));
                assertTrue(entry.notProvenBoundary().contains("not production proof"));
            }
        }
    }

    @Test
    void transcriptsCarryNotProvenBoundariesAndAvoidRuntimeToolingRequirements() {
        for (LocalLabPassiveTranscriptScenario transcript : LocalLabPassiveTranscriptCatalog.transcripts()) {
            assertFalse(transcript.notProvenBoundary().isBlank());
            assertTrue(transcript.notProvenBoundary().contains("not production proof"));
            assertTrue(transcript.notProvenBoundary().contains("not live-cloud validation"));
            assertTrue(transcript.notProvenBoundary().contains("not real-tenant validation"));
            assertTrue(transcript.notProvenBoundary().contains(
                    "no Docker, k6, Bruno, or Toxiproxy execution is required"));
            assertTrue(transcript.notProvenBoundary().contains("no fake backend server"));
            assertTrue(transcript.notProvenBoundary().contains("listener"));
            assertTrue(transcript.notProvenBoundary().contains("port"));
            assertTrue(transcript.notProvenBoundary().contains("loopback HTTP call"));
            assertTrue(transcript.notProvenBoundary().contains("generated traffic"));
            assertTrue(transcript.notProvenBoundary().contains("replay execution"));
            assertTrue(transcript.notProvenBoundary().contains("storage"));
            assertTrue(transcript.notProvenBoundary().contains("report"));
            assertTrue(transcript.notProvenBoundary().contains("export"));
            assertTrue(transcript.notProvenBoundary().contains("runtime behavior"));
        }
    }

    @Test
    void transcriptsAvoidValidationRuntimeExecutionStorageAndExportOverclaims() {
        for (LocalLabPassiveTranscriptScenario transcript : LocalLabPassiveTranscriptCatalog.transcripts()) {
            String normalized = transcriptText(transcript).toLowerCase(Locale.ROOT);

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
                    "storage is implemented",
                    "export behavior is implemented",
                    "runtime behavior is changed",
                    "routing behavior is changed")) {
                assertFalse(normalized.contains(forbidden), transcript.transcriptId() + " must not overclaim "
                        + forbidden);
            }
        }
    }

    @Test
    void transcriptCatalogSourcesStayPassiveAndAvoidRuntimeSideEffects() throws Exception {
        for (Path source : TRANSCRIPT_SOURCES) {
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
    void docsDescribePassiveTranscriptSprintAsTestScopeOnly() throws Exception {
        String adr = read(ADR_0009);
        String matrix = read(MATRIX);
        String trustMap = read(TRUST_MAP);

        for (String doc : List.of(adr, matrix, trustMap)) {
            assertTrue(doc.contains("This PR adds test-scope passive transcript fixtures only."));
            assertTrue(doc.contains("Transcripts describe future request/response evidence expectations."));
            assertTrue(doc.contains("Transcripts do not execute HTTP requests."));
            assertTrue(doc.contains("Transcripts do not implement fake backend servers."));
            assertTrue(doc.contains(
                    "Transcripts do not start listeners, open ports, call localhost, generate traffic, run replay, write reports, persist storage, or run tools."));
            assertTrue(doc.contains("Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling"));
            assertTrue(doc.contains("This is still not production proof."));
        }
    }

    private static String transcriptText(LocalLabPassiveTranscriptScenario transcript) {
        List<String> parts = new ArrayList<>();
        parts.add(transcript.transcriptId());
        parts.add(transcript.scenarioId());
        parts.add(transcript.fixtureId());
        parts.add(transcript.behaviorType());
        parts.add(transcript.notProvenBoundary());
        for (LocalLabPassiveTranscriptEntry entry : transcript.entries()) {
            parts.add(entry.transcriptId());
            parts.add(entry.scenarioId());
            parts.add(entry.fixtureId());
            parts.add(Integer.toString(entry.stepNumber()));
            parts.add(entry.simulatedRequestMethodLabel());
            parts.add(entry.simulatedRequestPathLabel());
            parts.add(entry.backendId());
            parts.add(Integer.toString(entry.expectedResponseStatusCode()));
            parts.add(entry.expectedLatencyLabel());
            parts.add(entry.expectedResponseBodySummary());
            parts.add(entry.expectedErrorLabel());
            parts.add(entry.expectedLoadLabel());
            parts.add(entry.routingEvidenceObservationNote());
            parts.add(entry.safetyBoundaryNote());
            parts.add(entry.notProvenBoundary());
        }
        return String.join(" ", parts);
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
