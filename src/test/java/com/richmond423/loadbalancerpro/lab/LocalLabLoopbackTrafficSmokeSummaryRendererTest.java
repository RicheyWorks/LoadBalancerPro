package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalLabLoopbackTrafficSmokeSummaryRendererTest {
    @Test
    void summaryIncludesRequestCountsCoverageAndBoundaryCount() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            List<LocalLabLoopbackTrafficSmokeObservation> observations =
                    LocalLabLoopbackTrafficSmokeClient.smoke(harness);
            LocalLabLoopbackTrafficSmokeSummary summary =
                    LocalLabLoopbackTrafficSmokeSummaryRenderer.summarize(observations);

            assertEquals("summary-local-lab-loopback-traffic-smoke", summary.summaryId());
            assertEquals(8, summary.totalRequests());
            assertEquals(7, summary.matchedFixtureCount());
            assertEquals(1, summary.boundaryResponseCount());
            assertEquals(observations.stream().map(LocalLabLoopbackTrafficSmokeObservation::scenarioId).toList(),
                    summary.scenarioIdsCovered());
            assertEquals(observations.stream().map(LocalLabLoopbackTrafficSmokeObservation::backendId).toList(),
                    summary.backendIdsCovered());
            assertTrue(summary.scenarioIdsCovered().contains("unknown-loopback-traffic-smoke-scenario"));
            assertTrue(summary.backendIdsCovered().contains("unknown-loopback-traffic-smoke-backend"));
        }
    }

    @Test
    void summaryIncludesLoopbackEphemeralPortNoProductionProofAndNotProvenBoundaries() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            LocalLabLoopbackTrafficSmokeSummary summary = LocalLabLoopbackTrafficSmokeSummaryRenderer.summarize(
                    LocalLabLoopbackTrafficSmokeClient.smoke(harness));
            String normalized = normalize(summary.deterministicText());

            assertTrue(normalized.contains("all smoke observations targeted 127.0.0.1 loopback-only"));
            assertTrue(normalized.contains("harness-assigned ephemeral ports"));
            assertTrue(normalized.contains("not production proof"));
            assertTrue(normalized.contains("not production certification"));
            assertTrue(normalized.contains("not live-cloud validation"));
            assertTrue(normalized.contains("not real-tenant validation"));
            assertTrue(normalized.contains("not proven: docker/k6/bruno/toxiproxy implementation"));
            assertTrue(normalized.contains("replay execution"));
            assertTrue(normalized.contains("evidence/report generation"));
            assertTrue(normalized.contains("file writing"));
            assertTrue(normalized.contains("storage"));
            assertTrue(normalized.contains("export/download/upload/pdf/zip behavior"));
            assertTrue(normalized.contains("runtime behavior"));
            assertTrue(normalized.contains("production routing/proxy/scoring/api behavior"));
            assertTrue(normalized.contains("separately scoped local-lab step"));
        }
    }

    @Test
    void summaryOutputIsDeterministicAcrossRepeatedCalls() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            List<LocalLabLoopbackTrafficSmokeObservation> observations =
                    LocalLabLoopbackTrafficSmokeClient.smoke(harness);
            LocalLabLoopbackTrafficSmokeSummary first =
                    LocalLabLoopbackTrafficSmokeSummaryRenderer.summarize(observations);
            LocalLabLoopbackTrafficSmokeSummary second =
                    LocalLabLoopbackTrafficSmokeSummaryRenderer.summarize(observations);

            assertEquals(first, second);
            assertEquals(first.deterministicText(), second.deterministicText());
            assertThrows(UnsupportedOperationException.class, () -> first.scenarioIdsCovered().add("extra"));
            assertThrows(UnsupportedOperationException.class, () -> first.backendIdsCovered().add("extra"));
        }
    }

    @Test
    void summaryRejectsMissingObservations() {
        assertThrows(IllegalArgumentException.class, () -> LocalLabLoopbackTrafficSmokeSummaryRenderer.summarize(null));
        assertThrows(IllegalArgumentException.class, () -> LocalLabLoopbackTrafficSmokeSummaryRenderer.summarize(
                List.of()));
    }

    @Test
    void summaryDoesNotImplyToolExecutionReplayReportsStorageExportRuntimeOrValidationClaims() throws Exception {
        try (LocalLabMultiBackendLoopbackHarness harness = LocalLabMultiBackendLoopbackHarness.start()) {
            LocalLabLoopbackTrafficSmokeSummary summary = LocalLabLoopbackTrafficSmokeSummaryRenderer.summarize(
                    LocalLabLoopbackTrafficSmokeClient.smoke(harness));
            String normalized = normalize(summary.deterministicText());

            assertTrue(normalized.contains("docker/k6/bruno/toxiproxy implementation"));
            assertTrue(normalized.contains("not production proof"));

            for (String forbidden : List.of(
                    "production-ready",
                    "production certified",
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
                    "replay execution is implemented",
                    "evidence report is generated",
                    "report generation is implemented",
                    "storage is implemented",
                    "export behavior is implemented",
                    "runtime behavior is changed",
                    "routing behavior is changed",
                    "production api behavior is changed")) {
                assertFalse(normalized.contains(forbidden), "summary must not overclaim " + forbidden);
            }
        }
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
