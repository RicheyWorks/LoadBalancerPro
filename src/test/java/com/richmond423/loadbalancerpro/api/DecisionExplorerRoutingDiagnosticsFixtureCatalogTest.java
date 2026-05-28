package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class DecisionExplorerRoutingDiagnosticsFixtureCatalogTest {
    private final DecisionExplorerConfidenceSummaryService summaryService =
            new DecisionExplorerConfidenceSummaryService();
    private final DecisionExplorerRoutingDiagnosticsService diagnosticsService =
            new DecisionExplorerRoutingDiagnosticsService();

    @Test
    void fixtureCatalogBuildsDiagnosticsForMajorStatusesInStableOrder() {
        List<DecisionExplorerRoutingDiagnosticsV1> diagnostics = diagnostics();

        assertEquals(List.of("STRONG", "PARTIAL", "UNKNOWN", "DEGRADED"),
                diagnostics.stream().map(DecisionExplorerRoutingDiagnosticsV1::overallStatus).toList());
        assertEquals(List.of("COMPLETE", "PARTIAL", "UNKNOWN", "DEGRADED"),
                diagnostics.stream().map(DecisionExplorerRoutingDiagnosticsV1::evidenceQuality).toList());
        assertEquals(List.of("edge-a", "edge-a", "UNKNOWN", "edge-a"),
                diagnostics.stream().map(DecisionExplorerRoutingDiagnosticsV1::selectedCandidateId).toList());
    }

    @Test
    void fixtureExplanationsAreGeneratedFromComputedDiagnostics() {
        List<String> explanations = explanationFingerprints();

        assertEquals(List.of(
                "strong-confirmed-selection|STRONG|Routing diagnostics mark selected candidate edge-a as STRONG "
                        + "because selected candidate diagnostics are STRONG/LOW, evidence quality is COMPLETE, "
                        + "1 alternative candidate(s) are available for comparison, and 2 factor diagnostic row(s) "
                        + "were computed.",
                "partial-candidate-and-factor-evidence|PARTIAL|Routing diagnostics mark selected candidate edge-a "
                        + "as PARTIAL because edge-b:latency:FACTOR_EVIDENCE_PARTIAL|FACTOR_WARNINGS_PRESENT|"
                        + "evidenceStatus=PARTIAL|factor evidence is partial|factorStatus=PARTIAL; selected risk "
                        + "is REVIEW, 1 alternative candidate(s) are available for comparison, and evidence counts "
                        + "are present=1, partial=7, missing=0, degraded=0, unknown=1.",
                "unknown-no-routing-evidence|UNKNOWN|Routing diagnostics mark selected candidate UNKNOWN as UNKNOWN "
                        + "because CANDIDATE_COMPARISONS_MISSING; selected risk is UNKNOWN and evidence counts are "
                        + "present=2, partial=0, missing=6, degraded=0, unknown=1.",
                "degraded-selected-health-evidence|DEGRADED|Routing diagnostics mark selected candidate edge-a "
                        + "as DEGRADED because edge-a:healthState:FACTOR_EVIDENCE_DEGRADED|factorStatus=DEGRADED|"
                        + "health evidence value is degraded; "
                        + "selected risk is HIGH and evidence counts are present=4, partial=0, missing=0, "
                        + "degraded=5, unknown=0."),
                explanations);
    }

    @Test
    void fixtureDiagnosticsExposeLocalOnlySafetyBoundaries() {
        for (DecisionExplorerRoutingDiagnosticsV1 diagnostics : diagnostics()) {
            assertTrue(diagnostics.readOnly());
            assertTrue(diagnostics.simulationOnly());
            assertEquals(DecisionExplorerConfidenceSummaryFixtureCatalog.BOUNDARY_NOTE, diagnostics.boundaryNote());
            assertFalse(diagnostics.explanationText().toLowerCase(Locale.ROOT)
                    .contains("production readiness proven"));
        }
    }

    @Test
    void fixtureHarnessDoesNotUseExternalProductionOrPersistenceHooks() throws Exception {
        String source = Files.readString(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/"
                + "DecisionExplorerConfidenceSummaryFixtureCatalog.java"), StandardCharsets.UTF_8);
        String normalized = source.toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "cloudmanager",
                "http://",
                "https://",
                "system.getenv",
                "system.getproperty",
                "files.write",
                "socket",
                "urlconnection",
                "httpclient",
                "production readiness proven")) {
            assertFalse(normalized.contains(forbidden), "diagnostic fixture harness must not contain " + forbidden);
        }
    }

    private List<DecisionExplorerRoutingDiagnosticsV1> diagnostics() {
        return DecisionExplorerConfidenceSummaryFixtureCatalog.fixtures().stream()
                .map(fixture -> fixture.buildDiagnostics(summaryService, diagnosticsService))
                .toList();
    }

    private List<String> explanationFingerprints() {
        List<DecisionExplorerConfidenceSummaryFixtureCatalog.StatusFixture> fixtures =
                DecisionExplorerConfidenceSummaryFixtureCatalog.fixtures();
        List<DecisionExplorerRoutingDiagnosticsV1> diagnostics = diagnostics();
        return java.util.stream.IntStream.range(0, fixtures.size())
                .mapToObj(index -> fixtures.get(index).fixtureId() + "|"
                        + diagnostics.get(index).overallStatus() + "|"
                        + diagnostics.get(index).explanationText())
                .toList();
    }
}
