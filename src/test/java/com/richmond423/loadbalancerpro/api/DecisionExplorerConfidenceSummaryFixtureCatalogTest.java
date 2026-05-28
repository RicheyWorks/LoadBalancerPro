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

class DecisionExplorerConfidenceSummaryFixtureCatalogTest {
    private final DecisionExplorerConfidenceSummaryService service = new DecisionExplorerConfidenceSummaryService();

    @Test
    void fixtureCatalogCoversEveryConfidenceStatusInStableOrder() {
        List<DecisionExplorerConfidenceSummaryFixtureCatalog.StatusFixture> fixtures =
                DecisionExplorerConfidenceSummaryFixtureCatalog.fixtures();

        assertEquals(List.of(
                "strong-confirmed-selection",
                "partial-candidate-and-factor-evidence",
                "unknown-no-routing-evidence",
                "degraded-selected-health-evidence"),
                fixtures.stream().map(DecisionExplorerConfidenceSummaryFixtureCatalog.StatusFixture::fixtureId)
                        .toList());
        assertEquals(List.of("STRONG", "PARTIAL", "UNKNOWN", "DEGRADED"),
                fixtures.stream().map(DecisionExplorerConfidenceSummaryFixtureCatalog.StatusFixture::expectedStatus)
                        .toList());
    }

    @Test
    void fixturesBuildExpectedSummariesCandidateDetailsFactorDetailsAndExplanations() {
        for (DecisionExplorerConfidenceSummaryFixtureCatalog.StatusFixture fixture
                : DecisionExplorerConfidenceSummaryFixtureCatalog.fixtures()) {
            DecisionExplorerConfidenceSummaryV1 summary = fixture.build(service);

            assertEquals(fixture.expectedStatus(), summary.status(), fixture.fixtureId());
            assertEquals(DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(fixture.expectedStatus()),
                    summary.evidenceQuality(), fixture.fixtureId());
            assertEquals(fixture.expectedSelectedCandidateId(), summary.selectedCandidateId(), fixture.fixtureId());
            assertEquals(fixture.expectedCandidateConfidenceRows(), summary.candidateConfidenceDetails().size(),
                    fixture.fixtureId());
            assertEquals(fixture.expectedFactorStatusRows(), summary.factorStatusDetails().size(),
                    fixture.fixtureId());
            assertTrue(summary.statusReasons().contains(fixture.expectedPrimaryReason()), fixture.fixtureId());
            assertEquals(fixture.expectedStatus(), summary.statusExplanation().status(), fixture.fixtureId());
            assertEquals(summary.evidenceQuality(), summary.statusExplanation().evidenceQuality(),
                    fixture.fixtureId());
            assertTrue(summary.statusExplanation().summaryText().contains(fixture.expectedExplanationFragment()),
                    fixture.fixtureId());
            assertTrue(summary.statusExplanation().evidenceHighlights()
                    .contains("candidateConfidenceDetailCount=" + fixture.expectedCandidateConfidenceRows()),
                    fixture.fixtureId());
            assertTrue(summary.statusExplanation().evidenceHighlights()
                    .contains("factorStatusDetailCount=" + fixture.expectedFactorStatusRows()),
                    fixture.fixtureId());
            assertEquals(DecisionExplorerConfidenceSummaryFixtureCatalog.BOUNDARY_NOTE, summary.boundaryNote(),
                    fixture.fixtureId());
        }
    }

    @Test
    void fixtureSummariesHaveDeterministicFingerprintsAcrossRuns() {
        List<String> first = fingerprints();
        List<String> second = fingerprints();

        assertEquals(first, second);
        assertEquals(List.of(
                "strong-confirmed-selection|STRONG|COMPLETE|edge-a|"
                        + "CANDIDATE_COMPARISONS_AVAILABLE,FACTOR_EVIDENCE_AVAILABLE,NO_STATUS_WARNINGS,"
                        + "SELECTED_CANDIDATE_CONFIRMED|edge-a:STRONG:HEALTHY;edge-b:STRONG:HEALTHY|"
                        + "edge-a:healthState:STRONG;edge-b:latency:STRONG|STRONG",
                "partial-candidate-and-factor-evidence|PARTIAL|PARTIAL|edge-a|"
                        + "FACTOR_STATUS_PARTIAL,PARTIAL_CANDIDATE_COMPARISON_EVIDENCE,PARTIAL_FACTOR_EVIDENCE,"
                        + "SELECTED_CANDIDATE_CONFIDENCE_PARTIAL,STATUS_UNKNOWNS_PRESENT,"
                        + "STATUS_WARNINGS_PRESENT|edge-a:PARTIAL:HEALTHY;edge-b:PARTIAL:HEALTHY|"
                        + "edge-b:latency:PARTIAL|PARTIAL",
                "unknown-no-routing-evidence|UNKNOWN|UNKNOWN|UNKNOWN|NO_ROUTING_EVIDENCE_RETURNED|||UNKNOWN",
                "degraded-selected-health-evidence|DEGRADED|DEGRADED|edge-a|"
                        + "SELECTED_CANDIDATE_CONFIDENCE_DEGRADED|edge-a:DEGRADED:DEGRADED|"
                        + "edge-a:healthState:DEGRADED|DEGRADED"),
                first);
    }

    @Test
    void fixtureHarnessStaysLocalAndDoesNotUseExternalOrProductionHooks() throws Exception {
        String source = Files.readString(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/"
                + "DecisionExplorerConfidenceSummaryFixtureCatalog.java"), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);

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
            assertFalse(source.contains(forbidden), "fixture harness must not contain " + forbidden);
        }
    }

    private List<String> fingerprints() {
        return DecisionExplorerConfidenceSummaryFixtureCatalog.fixtures().stream()
                .map(fixture -> fixture.fixtureId() + "|" + fingerprint(fixture.build(service)))
                .toList();
    }

    private static String fingerprint(DecisionExplorerConfidenceSummaryV1 summary) {
        return String.join("|",
                summary.status(),
                summary.evidenceQuality(),
                summary.selectedCandidateId(),
                String.join(",", summary.statusReasons()),
                detailFingerprint(summary.candidateConfidenceDetails()),
                factorFingerprint(summary.factorStatusDetails()),
                summary.statusExplanation().factorStatusRollup());
    }

    private static String detailFingerprint(List<DecisionExplorerCandidateConfidenceV1> details) {
        return String.join(";",
                details.stream()
                        .map(detail -> detail.candidateId() + ":" + detail.confidenceStatus() + ":"
                                + detail.healthEvidenceState())
                        .toList());
    }

    private static String factorFingerprint(List<DecisionExplorerFactorStatusV1> details) {
        return String.join(";",
                details.stream()
                        .map(detail -> detail.candidateId() + ":" + detail.factorName() + ":"
                                + detail.factorStatus())
                        .toList());
    }
}
