package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabTrafficMatrixSummaryRendererTest {
    @Test
    void summaryIncludesCaseFixtureBoundaryScenarioAndBackendCoverageCounts() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            LocalLabTrafficMatrixSummary summary =
                    LocalLabTrafficMatrixSummaryRenderer.summarize(LocalLabTrafficMatrixRunner.run(harness));

            assertEquals("summary-local-lab-deterministic-traffic-matrix", summary.summaryId());
            assertEquals(8, summary.matrixCaseCount());
            assertEquals(7, summary.matchedFixtureCount());
            assertEquals(1, summary.boundaryCaseCount());
            assertEquals(7, summary.scenarioCoverageCount());
            assertEquals(7, summary.backendCoverageCount());

            for (String scenarioId : LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                    .map(LocalLabFakeBackendResponseFixture::scenarioId)
                    .toList()) {
                assertTrue(summary.scenarioIdsCovered().contains(scenarioId));
            }
            assertTrue(summary.scenarioIdsCovered().contains(
                    LocalLabTrafficMatrixCatalog.UNKNOWN_MATRIX_SCENARIO_ID));

            for (String backendId : LocalLabFakeBackendResponseFixtureCatalog.fixtures().stream()
                    .map(LocalLabFakeBackendResponseFixture::backendId)
                    .toList()) {
                assertTrue(summary.backendIdsCovered().contains(backendId));
            }
            assertTrue(summary.backendIdsCovered().contains(
                    LocalLabTrafficMatrixCatalog.UNKNOWN_MATRIX_BACKEND_ID));
        }
    }

    @Test
    void summaryIncludesLoopbackEphemeralDeterministicNoProductionProofAndFutureToolingBoundaries()
            throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            LocalLabTrafficMatrixSummary summary =
                    LocalLabTrafficMatrixSummaryRenderer.summarize(LocalLabTrafficMatrixRunner.run(harness));
            String normalized = normalize(summary.deterministicText());

            assertTrue(normalized.contains("127.0.0.1 loopback-only"));
            assertTrue(normalized.contains("harness-assigned ephemeral ports"));
            assertTrue(normalized.contains("deterministic in memory"));
            assertTrue(normalized.contains("not load testing"));
            assertTrue(normalized.contains("not stress testing"));
            assertTrue(normalized.contains("not production traffic"));
            assertTrue(normalized.contains("not production proof"));
            assertTrue(normalized.contains("not production certification"));
            assertTrue(normalized.contains("not live-cloud validation"));
            assertTrue(normalized.contains("not real-tenant validation"));
            assertTrue(normalized.contains("no docker/k6/bruno/toxiproxy implementation"));
            assertTrue(normalized.contains("no replay execution"));
            assertTrue(normalized.contains("no evidence/report generation"));
            assertTrue(normalized.contains("no file writing"));
            assertTrue(normalized.contains("no environment reads"));
            assertTrue(normalized.contains("no process execution"));
            assertTrue(normalized.contains("no fixed ports"));
            assertTrue(normalized.contains("no non-loopback target calls or binds"));
            assertTrue(normalized.contains("no storage/export/download/upload/pdf/zip behavior"));
            assertTrue(normalized.contains("no runtime behavior"));
            assertTrue(normalized.contains("no production routing/proxy/scoring/api behavior"));
            assertTrue(normalized.contains("separately scoped k6/bruno planning docs"));
        }
    }

    @Test
    void summaryOutputIsDeterministicAcrossRepeatedCallsAndImmutable() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            List<LocalLabTrafficMatrixObservation> observations = LocalLabTrafficMatrixRunner.run(harness);
            LocalLabTrafficMatrixSummary first = LocalLabTrafficMatrixSummaryRenderer.summarize(observations);
            LocalLabTrafficMatrixSummary second = LocalLabTrafficMatrixSummaryRenderer.summarize(observations);

            assertEquals(first, second);
            assertEquals(first.deterministicText(), second.deterministicText());
            assertThrows(UnsupportedOperationException.class, () -> first.scenarioIdsCovered().add("extra"));
            assertThrows(UnsupportedOperationException.class, () -> first.backendIdsCovered().add("extra"));
        }
    }

    @Test
    void summaryRejectsMissingObservations() {
        assertThrows(IllegalArgumentException.class, () -> LocalLabTrafficMatrixSummaryRenderer.summarize(null));
        assertThrows(IllegalArgumentException.class, () -> LocalLabTrafficMatrixSummaryRenderer.summarize(List.of()));
    }

    @Test
    void summaryDoesNotImplyLoadToolReplayReportStorageExportRuntimeOrValidationClaims() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            String normalized = normalize(LocalLabTrafficMatrixSummaryRenderer.summarize(
                    LocalLabTrafficMatrixRunner.run(harness)).deterministicText());

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
                    "production endpoint is added")) {
                assertFalse(normalized.contains(forbidden), "matrix summary must not overclaim " + forbidden);
            }
        }
    }

    @Test
    void existingTrafficSmokeReviewerChecklistStillPassesWhenMatrixSummaryExists() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            LocalLabTrafficMatrixSummary matrixSummary =
                    LocalLabTrafficMatrixSummaryRenderer.summarize(LocalLabTrafficMatrixRunner.run(harness));
            LocalLabLoopbackTrafficSmokeSummary smokeSummary =
                    LocalLabLoopbackTrafficSmokeSummaryRenderer.summarize(
                            LocalLabLoopbackTrafficSmokeClient.smoke(harness));
            LocalLabTrafficSmokeReviewerChecklist checklist =
                    LocalLabTrafficSmokeReviewerChecklistMapper.checklist(smokeSummary);

            assertEquals("summary-local-lab-deterministic-traffic-matrix", matrixSummary.summaryId());
            assertFalse(checklist.items().isEmpty());
            assertTrue(normalize(checklist.deterministicText()).contains("local simulation is not production proof"));
        }
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
