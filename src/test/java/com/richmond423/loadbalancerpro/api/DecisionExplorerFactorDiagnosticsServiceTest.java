package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerFactorDiagnosticsServiceTest {
    private static final String BOUNDARY_NOTE = "read-only factor diagnostics";
    private final DecisionExplorerConfidenceSummaryService confidenceService =
            new DecisionExplorerConfidenceSummaryService();
    private final DecisionExplorerFactorDiagnosticsService factorDiagnosticsService =
            new DecisionExplorerFactorDiagnosticsService();

    @Test
    void factorDiagnosticsClassifySupportingAndNeutralContributionsDeterministically() {
        FactorFixture fixture = fixture(List.of(
                factor("edge-a", "healthState", "true", "SUPPORTS_SELECTION", "AVAILABLE", List.of(), List.of()),
                factor("edge-a", "latencyWindow", "22ms", "NEUTRAL", "AVAILABLE", List.of(), List.of())));

        List<DecisionExplorerFactorDiagnosticV1> diagnostics =
                factorDiagnosticsService.buildFactorDiagnostics(
                        fixture.summary(),
                        fixture.factorDrilldowns(),
                        BOUNDARY_NOTE);

        assertEquals(List.of(
                "edge-a:healthState:SUPPORTING:INFO:STRONG:AVAILABLE:0:0:0:0",
                "edge-a:latencyWindow:NEUTRAL:INFO:STRONG:AVAILABLE:0:0:0:0"),
                fingerprint(diagnostics));
        assertTrue(diagnostics.get(0).supportingSignals().contains("influenceCategory=SUPPORTS_SELECTION"));
        assertEquals(List.of(), factorDiagnosticsService.degradationReasons(diagnostics));
        assertEquals(List.of(), factorDiagnosticsService.partialEvidenceReasons(diagnostics));
        assertEquals(List.of(), factorDiagnosticsService.unknownEvidenceReasons(diagnostics));
    }

    @Test
    void factorDiagnosticsSurfaceWarningAndPartialEvidenceReasons() {
        FactorFixture fixture = fixture(List.of(
                factor("edge-a", "latency", "UNKNOWN", "SUPPORTS_SELECTION", "PARTIAL",
                        List.of("factor evidence is partial"),
                        List.of("numeric contribution value")),
                factor("edge-a", "queueDepth", "82%", "WEAKENS_SELECTION", "AVAILABLE", List.of(), List.of())));

        List<DecisionExplorerFactorDiagnosticV1> diagnostics =
                factorDiagnosticsService.buildFactorDiagnostics(
                        fixture.summary(),
                        fixture.factorDrilldowns(),
                        BOUNDARY_NOTE);

        assertEquals(List.of(
                "edge-a:latency:WARNING:REVIEW:PARTIAL:PARTIAL:1:4:0:1",
                "edge-a:queueDepth:WARNING:REVIEW:STRONG:AVAILABLE:0:0:0:0"),
                fingerprint(diagnostics));
        assertTrue(diagnostics.get(0).warningSignals().contains("evidenceStatus=PARTIAL"));
        assertTrue(diagnostics.get(0).unknownSignals().contains("factor observed value was not returned"));
        assertTrue(diagnostics.get(1).warningSignals().contains("influenceCategory=WEAKENS_SELECTION"));
        assertEquals(List.of(
                "edge-a:latency:FACTOR_EVIDENCE_PARTIAL|FACTOR_WARNINGS_PRESENT|evidenceStatus=PARTIAL|factor evidence is partial|factorStatus=PARTIAL",
                "edge-a:queueDepth:influenceCategory=WEAKENS_SELECTION"),
                factorDiagnosticsService.partialEvidenceReasons(diagnostics));
    }

    @Test
    void factorDiagnosticsSurfaceUnknownEvidenceReasons() {
        FactorFixture fixture = fixture(List.of(
                factor("edge-a", "latency", "UNKNOWN", "UNKNOWN_INFLUENCE", "UNKNOWN", List.of(),
                        List.of("factor measurement was unavailable"))));

        List<DecisionExplorerFactorDiagnosticV1> diagnostics =
                factorDiagnosticsService.buildFactorDiagnostics(
                        fixture.summary(),
                        fixture.factorDrilldowns(),
                        BOUNDARY_NOTE);

        assertEquals(List.of(
                "edge-a:latency:UNKNOWN:REVIEW:UNKNOWN:UNKNOWN:0:5:0:2"),
                fingerprint(diagnostics));
        assertTrue(diagnostics.get(0).unknownSignals().contains("factor evidence status is unknown"));
        assertTrue(diagnostics.get(0).unknownSignals().contains("factor influence category is unknown"));
        assertEquals(List.of(
                "edge-a:latency:FACTOR_EVIDENCE_UNKNOWN|factor evidence status is unknown|factor influence category is unknown|factor measurement was unavailable|factor observed value was not returned"),
                factorDiagnosticsService.unknownEvidenceReasons(diagnostics));
    }

    @Test
    void factorDiagnosticsSurfaceDegradationReasons() {
        FactorFixture fixture = fixture(List.of(
                factor("edge-a", "healthState", "false", "SUPPORTS_SELECTION", "AVAILABLE", List.of(), List.of())));

        List<DecisionExplorerFactorDiagnosticV1> diagnostics =
                factorDiagnosticsService.buildFactorDiagnostics(
                        fixture.summary(),
                        fixture.factorDrilldowns(),
                        BOUNDARY_NOTE);

        assertEquals(List.of(
                "edge-a:healthState:DEGRADED:BLOCKING:DEGRADED:AVAILABLE:0:0:3:0"),
                fingerprint(diagnostics));
        assertTrue(diagnostics.get(0).degradedSignals().contains("health evidence value is degraded"));
        assertTrue(diagnostics.get(0).reasonCodes().contains("FACTOR_CONTRIBUTION_DEGRADED"));
        assertEquals(List.of(
                "edge-a:healthState:FACTOR_EVIDENCE_DEGRADED|factorStatus=DEGRADED|health evidence value is degraded"),
                factorDiagnosticsService.degradationReasons(diagnostics));
    }

    @Test
    void routingDiagnosticsIncludesFactorAndEvidenceReasonLists() {
        FactorFixture fixture = fixture(List.of(
                factor("edge-a", "healthState", "false", "SUPPORTS_SELECTION", "AVAILABLE", List.of(), List.of()),
                factor("edge-a", "latency", "UNKNOWN", "SUPPORTS_SELECTION", "PARTIAL",
                        List.of("factor evidence is partial"),
                        List.of("numeric contribution value"))));
        DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics =
                new DecisionExplorerRoutingDiagnosticsService().buildDiagnostics(
                        fixture.summary(),
                        fixture.candidateSet(),
                        fixture.candidateComparisons(),
                        fixture.factorDrilldowns(),
                        List.of(),
                        List.of(),
                        BOUNDARY_NOTE);

        assertEquals(2, routingDiagnostics.factorDiagnostics().size());
        assertEquals(1, routingDiagnostics.degradationReasons().size());
        assertEquals(1, routingDiagnostics.partialEvidenceReasons().size());
        assertEquals(List.of(), routingDiagnostics.unknownEvidenceReasons());
    }

    private FactorFixture fixture(List<DecisionFactorDrilldownV1> factors) {
        List<CandidateReadoutV1> candidates = List.of(candidate("edge-a", true));
        List<DecisionExplorerCandidateComparisonRowV1> comparisons = List.of(comparison("edge-a", true));
        DecisionExplorerConfidenceSummaryV1 summary = confidenceService.buildSummary(
                decisionReadout("SUCCESS", "edge-a"),
                candidates.get(0),
                candidates,
                comparisons,
                factors,
                List.of(),
                List.of("hidden routing internals"),
                BOUNDARY_NOTE);
        return new FactorFixture(summary, candidates, comparisons, factors);
    }

    private static List<String> fingerprint(List<DecisionExplorerFactorDiagnosticV1> diagnostics) {
        return diagnostics.stream()
                .map(diagnostic -> diagnostic.candidateId() + ":" + diagnostic.factorName() + ":"
                        + diagnostic.contribution() + ":" + diagnostic.severity() + ":"
                        + diagnostic.factorStatus() + ":" + diagnostic.evidenceStatus() + ":"
                        + diagnostic.warningCount() + ":" + diagnostic.unknownCount() + ":"
                        + diagnostic.degradedSignalCount() + ":" + diagnostic.missingSignalCount())
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
                10.0,
                List.of("healthState=healthy"),
                List.of("hidden routing internals"),
                List.of(selected ? "SELECTED_CANDIDATE" : "NON_SELECTED_CANDIDATE"),
                List.of("boundary-read-only", "boundary-simulation-only"),
                List.of("decision-vector:" + candidateId, "scores:" + candidateId),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerCandidateComparisonRowV1 comparison(String candidateId, boolean selected) {
        return new DecisionExplorerCandidateComparisonRowV1(
                candidateId,
                candidateId,
                selected,
                1,
                "SELECTED",
                10.0,
                0.0,
                List.of("healthState=healthy"),
                List.of(),
                List.of("SELECTED_CANDIDATE"),
                List.of("boundary-read-only", "boundary-simulation-only"),
                List.of("decision-vector:" + candidateId, "scores:" + candidateId),
                List.of(),
                List.of(),
                BOUNDARY_NOTE);
    }

    private static DecisionFactorDrilldownV1 factor(
            String candidateId,
            String factorName,
            String observedValue,
            String influenceCategory,
            String evidenceStatus,
            List<String> warnings,
            List<String> unknowns) {
        return new DecisionFactorDrilldownV1(
                factorName,
                candidateId,
                observedValue,
                influenceCategory,
                evidenceStatus,
                "factor explanation",
                warnings,
                unknowns,
                List.of("decision-vector:" + candidateId, "factor-contribution:" + candidateId + ":" + factorName),
                BOUNDARY_NOTE);
    }

    private record FactorFixture(
            DecisionExplorerConfidenceSummaryV1 summary,
            List<CandidateReadoutV1> candidateSet,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            List<DecisionFactorDrilldownV1> factorDrilldowns) {
    }
}
