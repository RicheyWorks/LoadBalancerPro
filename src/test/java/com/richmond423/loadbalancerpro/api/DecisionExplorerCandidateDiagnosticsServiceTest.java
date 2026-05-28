package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerCandidateDiagnosticsServiceTest {
    private static final String BOUNDARY_NOTE = "read-only candidate diagnostics";
    private final DecisionExplorerConfidenceSummaryService confidenceService =
            new DecisionExplorerConfidenceSummaryService();
    private final DecisionExplorerCandidateDiagnosticsService candidateDiagnosticsService =
            new DecisionExplorerCandidateDiagnosticsService();

    @Test
    void candidateDiagnosticsSummarizeSelectedAndAlternativeRowsDeterministically() {
        CandidateFixture fixture = strongFixture();
        List<DecisionExplorerCandidateDiagnosticV1> diagnostics =
                candidateDiagnosticsService.buildCandidateDiagnostics(
                        fixture.summary(),
                        fixture.candidateSet(),
                        fixture.candidateComparisons(),
                        BOUNDARY_NOTE);

        assertEquals(List.of(
                "edge-a:SELECTED:STRONG:LOW:HEALTHY:SELECTED_BASELINE_SCORE_PRESENT:1:0",
                "edge-b:ALTERNATIVE:STRONG:LOW:HEALTHY:ALTERNATIVE_TRAILS_SELECTED:1:0"),
                fingerprint(diagnostics));
        DecisionExplorerCandidateDiagnosticV1 selected =
                candidateDiagnosticsService.selectedCandidateDiagnostic(diagnostics, BOUNDARY_NOTE);
        assertEquals("edge-a", selected.candidateId());
        assertEquals(List.of("CANDIDATE_COMPARISON_AVAILABLE", "CANDIDATE_DIAGNOSTIC_STATUS_STRONG",
                "CANDIDATE_RISK_LOW", "DEGRADED_SIGNAL_COUNT_0", "FACTOR_EVIDENCE_AVAILABLE",
                "FINAL_SCORE_PRESENT", "HEALTH_EVIDENCE_HEALTHY", "HEALTH_SIGNAL_HEALTHY",
                "ROLE_SELECTED", "SELECTED_BASELINE_SCORE_PRESENT", "SELECTED_CANDIDATE",
                "UNKNOWN_SIGNAL_COUNT_1", "VISIBLE_SIGNAL_COUNT_1", "WARNING_COUNT_0"),
                selected.reasonCodes());
        assertTrue(selected.summaryText().contains("Selected candidate edge-a has STRONG diagnostics"));
        assertEquals("edge-b", candidateDiagnosticsService.alternativeCandidateDiagnostics(diagnostics).get(0)
                .candidateId());
    }

    @Test
    void partialAlternativeDiagnosticsExposeWeakAndUnknownSignals() {
        CandidateFixture fixture = partialFixture();
        List<DecisionExplorerCandidateDiagnosticV1> diagnostics =
                candidateDiagnosticsService.buildCandidateDiagnostics(
                        fixture.summary(),
                        fixture.candidateSet(),
                        fixture.candidateComparisons(),
                        BOUNDARY_NOTE);

        assertEquals(List.of(
                "edge-a:SELECTED:STRONG:LOW:HEALTHY:SELECTED_BASELINE_SCORE_PRESENT:1:0",
                "edge-b:ALTERNATIVE:PARTIAL:REVIEW:HEALTHY:ALTERNATIVE_DELTA_UNKNOWN:3:0"),
                fingerprint(diagnostics));
        DecisionExplorerCandidateDiagnosticV1 alternative =
                candidateDiagnosticsService.alternativeCandidateDiagnostics(diagnostics).get(0);
        assertTrue(alternative.weakSignals().contains("candidate final score was not returned"));
        assertTrue(alternative.weakSignals().contains("comparisonStatus=PARTIAL_EVIDENCE"));
        assertTrue(alternative.unknownSignals().contains("score delta from selected candidate"));
        assertTrue(alternative.reasonCodes().contains("SCORE_DELTA_MISSING"));
    }

    @Test
    void degradedSelectedCandidateDiagnosticsSurfaceHighRisk() {
        CandidateFixture fixture = degradedFixture();
        List<DecisionExplorerCandidateDiagnosticV1> diagnostics =
                candidateDiagnosticsService.buildCandidateDiagnostics(
                        fixture.summary(),
                        fixture.candidateSet(),
                        fixture.candidateComparisons(),
                        BOUNDARY_NOTE);

        assertEquals(List.of(
                "edge-a:SELECTED:DEGRADED:HIGH:DEGRADED:SELECTED_BASELINE_SCORE_PRESENT:1:3"),
                fingerprint(diagnostics));
        DecisionExplorerCandidateDiagnosticV1 selected =
                candidateDiagnosticsService.selectedCandidateDiagnostic(diagnostics, BOUNDARY_NOTE);
        assertTrue(selected.degradedSignals().contains("health evidence state is degraded"));
        assertTrue(selected.degradedSignals().contains("healthState=false"));
        assertTrue(selected.reasonCodes().contains("CANDIDATE_RISK_HIGH"));
    }

    @Test
    void emptyCandidateEvidenceReturnsUnknownSelectedWithoutInventingRows() {
        DecisionExplorerConfidenceSummaryV1 summary = DecisionExplorerConfidenceSummaryV1.unknown(BOUNDARY_NOTE);
        List<DecisionExplorerCandidateDiagnosticV1> diagnostics =
                candidateDiagnosticsService.buildCandidateDiagnostics(
                        summary,
                        null,
                        null,
                        BOUNDARY_NOTE);

        assertEquals(List.of(), diagnostics);
        DecisionExplorerCandidateDiagnosticV1 selected =
                candidateDiagnosticsService.selectedCandidateDiagnostic(diagnostics, BOUNDARY_NOTE);
        assertEquals("UNKNOWN", selected.candidateId());
        assertEquals("UNKNOWN", selected.selectionRole());
        assertEquals("UNKNOWN", selected.diagnosticStatus());
        assertEquals(List.of("SELECTED_CANDIDATE_DIAGNOSTIC_UNKNOWN"), selected.reasonCodes());
    }

    @Test
    void routingDiagnosticsIncludesCandidateDiagnosticsAdditively() {
        CandidateFixture fixture = strongFixture();
        DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics =
                new DecisionExplorerRoutingDiagnosticsService().buildDiagnostics(
                        fixture.summary(),
                        fixture.candidateSet(),
                        fixture.candidateComparisons(),
                        fixture.factorDrilldowns(),
                        List.of(),
                        List.of(),
                        BOUNDARY_NOTE);

        assertEquals("edge-a", routingDiagnostics.selectedCandidateDiagnostic().candidateId());
        assertEquals(1, routingDiagnostics.alternativeCandidateDiagnostics().size());
        assertEquals(2, routingDiagnostics.candidateDiagnostics().size());
        assertEquals(fingerprint(routingDiagnostics.candidateDiagnostics()),
                fingerprint(List.of(routingDiagnostics.selectedCandidateDiagnostic(),
                        routingDiagnostics.alternativeCandidateDiagnostics().get(0))));
    }

    private CandidateFixture strongFixture() {
        List<CandidateReadoutV1> candidates = List.of(candidate("edge-a", true), candidate("edge-b", false));
        List<DecisionExplorerCandidateComparisonRowV1> comparisons = List.of(
                comparison("edge-a", true, "SELECTED", 10.0, 0.0, List.of("healthState=healthy"),
                        List.of(), List.of()),
                comparison("edge-b", false, "COMPARED_TO_SELECTED", 15.0, 5.0,
                        List.of("healthState=healthy"), List.of(), List.of()));
        List<DecisionFactorDrilldownV1> factors = List.of(
                factor("edge-a", "healthState", "true", "AVAILABLE"),
                factor("edge-b", "latency", "22ms", "AVAILABLE"));
        DecisionExplorerConfidenceSummaryV1 summary = confidenceService.buildSummary(
                decisionReadout("SUCCESS", "edge-a"),
                candidates.get(0),
                candidates,
                comparisons,
                factors,
                List.of(),
                List.of("hidden routing internals"),
                BOUNDARY_NOTE);
        return new CandidateFixture(summary, candidates, comparisons, factors);
    }

    private CandidateFixture partialFixture() {
        List<CandidateReadoutV1> candidates = List.of(candidate("edge-a", true), candidate("edge-b", false));
        List<DecisionExplorerCandidateComparisonRowV1> comparisons = List.of(
                comparison("edge-a", true, "SELECTED", 10.0, 0.0, List.of("healthState=healthy"),
                        List.of(), List.of()),
                comparison("edge-b", false, "PARTIAL_EVIDENCE", null, null,
                        List.of("healthState=healthy"),
                        List.of("candidate final score was not returned"),
                        List.of("score delta from selected candidate")));
        List<DecisionFactorDrilldownV1> factors = List.of(
                factor("edge-a", "healthState", "true", "AVAILABLE"),
                factor("edge-b", "latency", "UNKNOWN", "PARTIAL"));
        DecisionExplorerConfidenceSummaryV1 summary = confidenceService.buildSummary(
                decisionReadout("SUCCESS", "edge-a"),
                candidates.get(0),
                candidates,
                comparisons,
                factors,
                List.of(),
                List.of("hidden routing internals"),
                BOUNDARY_NOTE);
        return new CandidateFixture(summary, candidates, comparisons, factors);
    }

    private CandidateFixture degradedFixture() {
        List<CandidateReadoutV1> candidates = List.of(candidate("edge-a", true));
        List<DecisionExplorerCandidateComparisonRowV1> comparisons = List.of(
                comparison("edge-a", true, "SELECTED", 10.0, 0.0, List.of("healthState=false"),
                        List.of(), List.of()));
        List<DecisionFactorDrilldownV1> factors = List.of(
                factor("edge-a", "healthState", "false", "AVAILABLE"));
        DecisionExplorerConfidenceSummaryV1 summary = confidenceService.buildSummary(
                decisionReadout("SUCCESS", "edge-a"),
                candidates.get(0),
                candidates,
                comparisons,
                factors,
                List.of(),
                List.of(),
                BOUNDARY_NOTE);
        return new CandidateFixture(summary, candidates, comparisons, factors);
    }

    private static List<String> fingerprint(List<DecisionExplorerCandidateDiagnosticV1> diagnostics) {
        return diagnostics.stream()
                .map(diagnostic -> diagnostic.candidateId() + ":" + diagnostic.selectionRole() + ":"
                        + diagnostic.diagnosticStatus() + ":" + diagnostic.riskLevel() + ":"
                        + diagnostic.healthEvidenceState() + ":" + diagnostic.scoreInterpretation() + ":"
                        + diagnostic.unknownSignalCount() + ":" + diagnostic.degradedSignalCount())
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
            Double finalScore,
            Double scoreDelta,
            List<String> visibleSignals,
            List<String> warnings,
            List<String> unknowns) {
        return new DecisionExplorerCandidateComparisonRowV1(
                candidateId,
                candidateId,
                selected,
                selected ? 1 : 2,
                comparisonStatus,
                finalScore,
                scoreDelta,
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
            String observedValue,
            String evidenceStatus) {
        return new DecisionFactorDrilldownV1(
                factorName,
                candidateId,
                observedValue,
                "SUPPORTS_SELECTION",
                evidenceStatus,
                "factor explanation",
                List.of(),
                List.of(),
                List.of("decision-vector:" + candidateId, "factor-contribution:" + candidateId + ":" + factorName),
                BOUNDARY_NOTE);
    }

    private record CandidateFixture(
            DecisionExplorerConfidenceSummaryV1 summary,
            List<CandidateReadoutV1> candidateSet,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            List<DecisionFactorDrilldownV1> factorDrilldowns) {
    }
}
