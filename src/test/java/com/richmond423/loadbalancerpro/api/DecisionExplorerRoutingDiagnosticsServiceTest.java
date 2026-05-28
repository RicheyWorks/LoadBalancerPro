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

class DecisionExplorerRoutingDiagnosticsServiceTest {
    private static final String BOUNDARY_NOTE = "read-only routing diagnostics";
    private final DecisionExplorerConfidenceSummaryService confidenceService =
            new DecisionExplorerConfidenceSummaryService();
    private final DecisionExplorerRoutingDiagnosticsService diagnosticsService =
            new DecisionExplorerRoutingDiagnosticsService();

    @Test
    void strongDiagnosticsSummarizePresentEvidenceDeterministically() {
        SummaryFixture fixture = strongFixture();
        DecisionExplorerRoutingDiagnosticsV1 diagnostics = diagnosticsService.buildDiagnostics(
                fixture.summary(),
                fixture.candidateSet(),
                fixture.candidateComparisons(),
                fixture.factorDrilldowns(),
                fixture.payloadWarnings(),
                fixture.payloadUnknowns(),
                BOUNDARY_NOTE);

        assertEquals("STRONG", diagnostics.overallStatus());
        assertEquals("COMPLETE", diagnostics.evidenceQuality());
        assertEquals("edge-a", diagnostics.selectedCandidateId());
        assertEquals(9, diagnostics.diagnosticCount());
        assertEquals(9, diagnostics.presentEvidenceCount());
        assertEquals(0, diagnostics.partialEvidenceCount());
        assertEquals(0, diagnostics.missingEvidenceCount());
        assertEquals(0, diagnostics.degradedEvidenceCount());
        assertEquals(0, diagnostics.unknownEvidenceCount());
        assertEquals(List.of(
                "CANDIDATE:candidate-comparisons:PRESENT:2",
                "CANDIDATE:candidate-confidence:PRESENT:2",
                "CANDIDATE:selected-candidate:PRESENT:1",
                "CAUTION:status-unknowns:PRESENT:0",
                "CAUTION:status-warnings:PRESENT:0",
                "DECISION:decision-status:PRESENT:1",
                "FACTOR:factor-evidence:PRESENT:2",
                "FACTOR:factor-status:PRESENT:2",
                "SOURCE:source-references:PRESENT:8"),
                fingerprint(diagnostics));
        assertEquals(List.of("AVAILABLE_FACTOR_COUNT_2", "CANDIDATE_COMPARISONS_AVAILABLE",
                "CANDIDATE_COMPARISON_AVAILABLE", "CANDIDATE_COMPARISON_COUNT_2", "CONFIDENCE_STATUS_STRONG",
                "FACTOR_EVIDENCE_AVAILABLE", "FACTOR_SOURCE_REFERENCES_AVAILABLE", "HEALTH_SIGNAL_HEALTHY",
                "NO_DIAGNOSTIC_UNKNOWNS", "NO_DIAGNOSTIC_WARNINGS", "NO_STATUS_WARNINGS",
                "PARTIAL_FACTOR_COUNT_0", "SELECTED_CANDIDATE_CONFIDENCE_STRONG",
                "SELECTED_CANDIDATE_CONFIRMED", "SOURCE_REFERENCES_RETURNED", "UNKNOWN_FACTOR_COUNT_0"),
                diagnostics.diagnosticReasons());
    }

    @Test
    void partialDiagnosticsExposePartialUnknownAndWarningEvidence() {
        SummaryFixture fixture = partialFixture();
        DecisionExplorerRoutingDiagnosticsV1 diagnostics = diagnosticsService.buildDiagnostics(
                fixture.summary(),
                fixture.candidateSet(),
                fixture.candidateComparisons(),
                fixture.factorDrilldowns(),
                fixture.payloadWarnings(),
                fixture.payloadUnknowns(),
                BOUNDARY_NOTE);

        assertEquals("PARTIAL", diagnostics.overallStatus());
        assertEquals("PARTIAL", diagnostics.evidenceQuality());
        assertEquals(1, diagnostics.presentEvidenceCount());
        assertEquals(7, diagnostics.partialEvidenceCount());
        assertEquals(0, diagnostics.missingEvidenceCount());
        assertEquals(0, diagnostics.degradedEvidenceCount());
        assertEquals(1, diagnostics.unknownEvidenceCount());
        assertEquals(List.of(
                "CANDIDATE:candidate-comparisons:PARTIAL:2",
                "CANDIDATE:candidate-confidence:PARTIAL:2",
                "CANDIDATE:selected-candidate:PARTIAL:1",
                "CAUTION:status-unknowns:UNKNOWN:2",
                "CAUTION:status-warnings:PARTIAL:2",
                "DECISION:decision-status:PARTIAL:1",
                "FACTOR:factor-evidence:PARTIAL:1",
                "FACTOR:factor-status:PARTIAL:1",
                "SOURCE:source-references:PRESENT:7"),
                fingerprint(diagnostics));
        assertTrue(diagnostics.diagnosticReasons().contains("PARTIAL_CANDIDATE_COMPARISON_EVIDENCE"));
        assertTrue(diagnostics.diagnosticReasons().contains("PARTIAL_FACTOR_EVIDENCE"));
        assertTrue(diagnostics.warnings().contains("candidate final score was not returned"));
        assertTrue(diagnostics.unknowns().contains("score delta from selected candidate"));
    }

    @Test
    void degradedDiagnosticsExtractSelectedCandidateAndFactorRisk() {
        SummaryFixture fixture = degradedFixture();
        DecisionExplorerRoutingDiagnosticsV1 diagnostics = diagnosticsService.buildDiagnostics(
                fixture.summary(),
                fixture.candidateSet(),
                fixture.candidateComparisons(),
                fixture.factorDrilldowns(),
                fixture.payloadWarnings(),
                fixture.payloadUnknowns(),
                BOUNDARY_NOTE);

        assertEquals("DEGRADED", diagnostics.overallStatus());
        assertEquals("DEGRADED", diagnostics.evidenceQuality());
        assertEquals(5, diagnostics.degradedEvidenceCount());
        assertTrue(diagnostics.diagnosticReasons().contains("SELECTED_CANDIDATE_CONFIDENCE_DEGRADED"));
        assertTrue(diagnostics.diagnosticReasons().contains("FACTOR_EVIDENCE_DEGRADED"));
        assertEquals("BLOCKING", diagnostics.evidenceDiagnostics().stream()
                .filter(diagnostic -> diagnostic.diagnosticId().equals("candidate-confidence"))
                .findFirst()
                .orElseThrow()
                .severity());
        assertEquals("BLOCKING", diagnostics.evidenceDiagnostics().stream()
                .filter(diagnostic -> diagnostic.diagnosticId().equals("factor-status"))
                .findFirst()
                .orElseThrow()
                .severity());
    }

    @Test
    void nullSummaryBuildsSafeUnknownDiagnosticsWithoutInventingEvidence() {
        DecisionExplorerRoutingDiagnosticsV1 diagnostics =
                diagnosticsService.buildDiagnostics(null, null, null, null, null, null, null);

        assertEquals("UNKNOWN", diagnostics.overallStatus());
        assertEquals("UNKNOWN", diagnostics.evidenceQuality());
        assertEquals("UNKNOWN", diagnostics.selectedCandidateId());
        assertEquals(1, diagnostics.diagnosticCount());
        assertEquals(1, diagnostics.unknownEvidenceCount());
        assertEquals(List.of("EVIDENCE:routing-evidence:UNKNOWN:0"), fingerprint(diagnostics));
        assertEquals(List.of("NO_CONFIDENCE_SUMMARY_RETURNED"), diagnostics.diagnosticReasons());
        assertTrue(diagnostics.unknowns().contains("confidence summary evidence was unavailable"));
        assertEquals("UNKNOWN", diagnostics.boundaryNote());
    }

    @Test
    void diagnosticsSourceDoesNotUseExternalServicesOrProductionHooks() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                + "DecisionExplorerRoutingDiagnosticsService.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerRoutingDiagnosticsV1.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerEvidenceDiagnosticV1.java"), StandardCharsets.UTF_8);
        String normalized = source.toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "serverscorecalculator",
                "serverstatevector",
                "instant.now",
                "system.getenv",
                "system.getproperty",
                "randomuuid",
                "httpclient",
                "urlconnection",
                "socket",
                "files.write",
                "production readiness proven")) {
            assertFalse(normalized.contains(forbidden), "diagnostics source must not contain " + forbidden);
        }
    }

    private SummaryFixture strongFixture() {
        List<CandidateReadoutV1> candidates = List.of(candidate("edge-a", true), candidate("edge-b", false));
        List<DecisionExplorerCandidateComparisonRowV1> comparisons = List.of(
                comparison("edge-a", true, "SELECTED", List.of(), List.of("hidden routing internals")),
                comparison("edge-b", false, "COMPARED_TO_SELECTED", List.of(),
                        List.of("hidden routing internals")));
        List<DecisionFactorDrilldownV1> factors = List.of(
                factor("edge-a", "healthState", "AVAILABLE", List.of(), List.of("hidden routing internals")),
                factor("edge-b", "latency", "AVAILABLE", List.of(), List.of("hidden routing internals")));
        DecisionExplorerConfidenceSummaryV1 summary = confidenceService.buildSummary(
                decisionReadout("SUCCESS", "edge-a"),
                candidates.get(0),
                candidates,
                comparisons,
                factors,
                List.of("Decision Explorer payload is read-only and simulation-only; it does not change routing."),
                List.of("hidden routing internals", "live-cloud behavior"),
                BOUNDARY_NOTE);
        return new SummaryFixture(summary, candidates, comparisons, factors, List.of(), List.of());
    }

    private SummaryFixture partialFixture() {
        List<CandidateReadoutV1> candidates = List.of(candidate("edge-a", true), candidate("edge-b", false));
        List<DecisionExplorerCandidateComparisonRowV1> comparisons = List.of(
                comparison("edge-a", true, "SELECTED", List.of(), List.of("hidden routing internals")),
                comparison("edge-b", false, "PARTIAL_EVIDENCE",
                        List.of("candidate final score was not returned"),
                        List.of("score delta from selected candidate")));
        List<DecisionFactorDrilldownV1> factors = List.of(factor(
                "edge-b",
                "latency",
                "PARTIAL",
                List.of("factor evidence is partial"),
                List.of("numeric contribution value")));
        DecisionExplorerConfidenceSummaryV1 summary = confidenceService.buildSummary(
                decisionReadout("SUCCESS", "edge-a"),
                candidates.get(0),
                candidates,
                comparisons,
                factors,
                List.of(),
                List.of("hidden routing internals"),
                BOUNDARY_NOTE);
        return new SummaryFixture(summary, candidates, comparisons, factors, List.of(), List.of());
    }

    private SummaryFixture degradedFixture() {
        List<CandidateReadoutV1> candidates = List.of(candidate("edge-a", true));
        List<DecisionExplorerCandidateComparisonRowV1> comparisons = List.of(comparisonWithSignals(
                "edge-a",
                true,
                "SELECTED",
                List.of("healthState=false"),
                List.of(),
                List.of()));
        List<DecisionFactorDrilldownV1> factors = List.of(factorWithObserved(
                "edge-a",
                "healthState",
                "false",
                "AVAILABLE",
                List.of(),
                List.of()));
        DecisionExplorerConfidenceSummaryV1 summary = confidenceService.buildSummary(
                decisionReadout("SUCCESS", "edge-a"),
                candidates.get(0),
                candidates,
                comparisons,
                factors,
                List.of(),
                List.of(),
                BOUNDARY_NOTE);
        return new SummaryFixture(summary, candidates, comparisons, factors, List.of(), List.of());
    }

    private static List<String> fingerprint(DecisionExplorerRoutingDiagnosticsV1 diagnostics) {
        return diagnostics.evidenceDiagnostics().stream()
                .map(diagnostic -> diagnostic.category() + ":" + diagnostic.diagnosticId() + ":"
                        + diagnostic.status() + ":" + diagnostic.evidenceCount())
                .toList();
    }

    private static DecisionReadoutV1 decisionReadout(String status, String selectedCandidateId) {
        return new DecisionReadoutV1(
                "decision-1",
                status,
                selectedCandidateId,
                "TAIL_LATENCY_POWER_OF_TWO",
                "summary",
                List.of("SELECTED_CANDIDATE_RETURNED"),
                List.of("routing-comparison-result", "decision-vector"),
                BOUNDARY_NOTE);
    }

    private static CandidateReadoutV1 candidate(String candidateId, boolean selected) {
        return new CandidateReadoutV1(
                candidateId,
                candidateId,
                selected,
                selected ? "SELECTED" : "NOT_SELECTED",
                selected ? 10.0 : 15.0,
                List.of("healthState=healthy"),
                List.of("hidden routing internals"),
                List.of(selected ? "SELECTED_CANDIDATE" : "NON_SELECTED_CANDIDATE"),
                List.of("boundary-read-only", "boundary-simulation-only"),
                List.of("decision-vector:" + candidateId, "scores:" + candidateId),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerCandidateComparisonRowV1 comparison(
            String candidateId,
            boolean selected,
            String comparisonStatus,
            List<String> warnings,
            List<String> unknowns) {
        return comparisonWithSignals(
                candidateId,
                selected,
                comparisonStatus,
                List.of("healthState=healthy"),
                warnings,
                unknowns);
    }

    private static DecisionExplorerCandidateComparisonRowV1 comparisonWithSignals(
            String candidateId,
            boolean selected,
            String comparisonStatus,
            List<String> visibleSignals,
            List<String> warnings,
            List<String> unknowns) {
        return new DecisionExplorerCandidateComparisonRowV1(
                candidateId,
                candidateId,
                selected,
                selected ? 1 : 2,
                comparisonStatus,
                selected ? 10.0 : 15.0,
                selected ? 0.0 : 5.0,
                visibleSignals,
                unknowns,
                List.of(selected ? "SELECTED_CANDIDATE" : "NON_SELECTED_CANDIDATE"),
                List.of("boundary-read-only", "boundary-simulation-only"),
                List.of("decision-vector:" + candidateId, "scores:" + candidateId),
                warnings,
                unknowns,
                BOUNDARY_NOTE);
    }

    private static DecisionFactorDrilldownV1 factor(
            String candidateId,
            String factorName,
            String evidenceStatus,
            List<String> warnings,
            List<String> unknowns) {
        return factorWithObserved(candidateId, factorName, "raw value", evidenceStatus, warnings, unknowns);
    }

    private static DecisionFactorDrilldownV1 factorWithObserved(
            String candidateId,
            String factorName,
            String observedValue,
            String evidenceStatus,
            List<String> warnings,
            List<String> unknowns) {
        return new DecisionFactorDrilldownV1(
                factorName,
                candidateId,
                observedValue,
                "SUPPORTS_SELECTION",
                evidenceStatus,
                "factor explanation",
                warnings,
                unknowns,
                List.of("decision-vector:" + candidateId, "factor-contribution:" + candidateId + ":" + factorName),
                BOUNDARY_NOTE);
    }

    private record SummaryFixture(
            DecisionExplorerConfidenceSummaryV1 summary,
            List<CandidateReadoutV1> candidateSet,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            List<DecisionFactorDrilldownV1> factorDrilldowns,
            List<String> payloadWarnings,
            List<String> payloadUnknowns) {
    }
}
