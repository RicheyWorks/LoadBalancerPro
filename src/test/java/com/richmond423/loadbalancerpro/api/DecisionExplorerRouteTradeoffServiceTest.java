package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class DecisionExplorerRouteTradeoffServiceTest {
    private static final String BOUNDARY_NOTE = "read-only route tradeoff diagnostics";
    private final DecisionExplorerConfidenceSummaryService confidenceSummaryService =
            new DecisionExplorerConfidenceSummaryService();
    private final DecisionExplorerRoutingDiagnosticsService routingDiagnosticsService =
            new DecisionExplorerRoutingDiagnosticsService();
    private final DecisionExplorerRouteTradeoffService tradeoffService =
            new DecisionExplorerRouteTradeoffService();

    @Test
    void strongTradeoffsSummarizeSelectedAdvantageDeterministically() {
        DecisionExplorerRouteTradeoffAnalysisV1 analysis = tradeoffs(strongFixture());

        assertEquals("STRONG", analysis.overallStatus());
        assertEquals("COMPLETE", analysis.evidenceQuality());
        assertEquals("edge-a", analysis.selectedCandidateId());
        assertEquals("SELECTED_ADVANTAGE", analysis.tradeoffCategory());
        assertEquals(2, analysis.candidateTradeoffCount());
        assertEquals(1, analysis.alternativeCount());
        assertEquals(1, analysis.comparedAlternativeCount());
        assertEquals("edge-b", analysis.closestAlternativeCandidateId());
        assertEquals(5.0, analysis.closestAlternativeScoreDelta());
        assertTrue(analysis.selectedCandidateSummary().contains("Selected candidate edge-a is the tradeoff baseline"));
        assertTrue(analysis.alternativeCandidateSummary()
                .contains("closest alternative edge-b with score delta 5.0"));
        assertEquals(List.of(
                "edge-a:SELECTED_BASELINE:BASELINE:BASELINE:0.0",
                "edge-b:ALTERNATIVE_TRAILS_SELECTED:SELECTED_ADVANTAGE:MATERIAL:5.0"),
                fingerprint(analysis));
        assertTrue(analysis.tradeoffReasons().contains("ROUTE_TRADEOFF_CATEGORY_SELECTED_ADVANTAGE"));
        assertTrue(analysis.candidateTradeoffs().get(1).benefitSignals()
                .contains("alternative trails selected by returned score delta"));
    }

    @Test
    void alternativeBeatingSelectedMarksSelectedChallengedWithoutChangingRoutingDecision() {
        DecisionExplorerRouteTradeoffAnalysisV1 analysis = tradeoffs(alternativeBeatsSelectedFixture());

        assertEquals("STRONG", analysis.overallStatus());
        assertEquals("edge-a", analysis.selectedCandidateId());
        assertEquals("SELECTED_CHALLENGED", analysis.tradeoffCategory());
        assertEquals("edge-b", analysis.closestAlternativeCandidateId());
        assertEquals(-2.0, analysis.closestAlternativeScoreDelta());
        DecisionExplorerRouteTradeoffRowV1 alternative = analysis.candidateTradeoffs().get(1);
        assertEquals("edge-b", alternative.candidateId());
        assertFalse(alternative.selected());
        assertEquals("ALTERNATIVE_BEATS_SELECTED", alternative.tradeoffCategory());
        assertEquals("RISK", alternative.riskBenefitClassification());
        assertEquals("MATERIAL", alternative.scoreGapCategory());
        assertTrue(alternative.scoringExplanation().contains("score delta -2.0 from selected"));
        assertTrue(alternative.riskSignals().contains("alternative beats selected by returned score delta"));
        assertTrue(alternative.reasonCodes().contains("TRADEOFF_CATEGORY_ALTERNATIVE_BEATS_SELECTED"));
    }

    @Test
    void unknownAlternativeKeepsPartialTradeoffAndSafeClosestFallback() {
        DecisionExplorerRouteTradeoffAnalysisV1 analysis = tradeoffs(unknownAlternativeFixture());

        assertEquals("PARTIAL", analysis.overallStatus());
        assertEquals("PARTIAL_TRADEOFF", analysis.tradeoffCategory());
        assertEquals(1, analysis.alternativeCount());
        assertEquals(0, analysis.comparedAlternativeCount());
        assertEquals("UNKNOWN", analysis.closestAlternativeCandidateId());
        assertNull(analysis.closestAlternativeScoreDelta());
        DecisionExplorerRouteTradeoffRowV1 alternative = analysis.candidateTradeoffs().get(1);
        assertEquals("ALTERNATIVE_UNKNOWN", alternative.tradeoffCategory());
        assertEquals("UNKNOWN", alternative.riskBenefitClassification());
        assertTrue(alternative.scoringExplanation()
                .contains("could not be score-compared because its score delta from selected was unavailable"));
        assertTrue(analysis.unknowns().contains("score delta from selected candidate"));
    }

    @Test
    void degradedSelectedEvidenceRollsUpToDegradedTradeoff() {
        DecisionExplorerRouteTradeoffAnalysisV1 analysis = tradeoffs(degradedSelectedFixture());

        assertEquals("DEGRADED", analysis.overallStatus());
        assertEquals("DEGRADED", analysis.evidenceQuality());
        assertEquals("DEGRADED", analysis.tradeoffCategory());
        assertEquals("edge-a", analysis.selectedCandidateId());
        assertTrue(analysis.tradeoffReasons().contains("ROUTE_TRADEOFF_CATEGORY_DEGRADED"));
        assertTrue(analysis.tradeoffReasons().contains("CANDIDATE_DIAGNOSTIC_STATUS_DEGRADED"));
        assertEquals(List.of("edge-a:SELECTED_BASELINE:BASELINE:BASELINE:0.0"),
                fingerprint(analysis));
    }

    @Test
    void nullInputsReturnUnknownWithoutInventingTradeoffEvidence() {
        DecisionExplorerRouteTradeoffAnalysisV1 analysis =
                tradeoffService.buildTradeoffs(null, null, null);

        assertEquals("UNKNOWN", analysis.overallStatus());
        assertEquals("UNKNOWN", analysis.evidenceQuality());
        assertEquals("UNKNOWN", analysis.tradeoffCategory());
        assertEquals("UNKNOWN", analysis.selectedCandidateId());
        assertEquals(0, analysis.candidateTradeoffCount());
        assertEquals(0, analysis.alternativeCount());
        assertTrue(analysis.candidateTradeoffs().isEmpty());
        assertEquals(List.of("ROUTING_DIAGNOSTICS_UNAVAILABLE"), analysis.tradeoffReasons());
        assertTrue(analysis.unknowns().contains("route tradeoff diagnostics were unavailable"));
        assertEquals("UNKNOWN", analysis.boundaryNote());
    }

    @Test
    void tradeoffSourceDoesNotUseRoutingMutationExternalServicesOrPersistence() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                + "DecisionExplorerRouteTradeoffService.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerRouteTradeoffAnalysisV1.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerRouteTradeoffRowV1.java"), StandardCharsets.UTF_8);
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
                "route(",
                "proxy",
                "production readiness proven")) {
            assertFalse(normalized.contains(forbidden), "tradeoff source must not contain " + forbidden);
        }
    }

    private DecisionExplorerRouteTradeoffAnalysisV1 tradeoffs(TradeoffFixture fixture) {
        DecisionExplorerConfidenceSummaryV1 summary = confidenceSummaryService.buildSummary(
                fixture.decisionReadout(),
                fixture.selectedCandidate(),
                fixture.candidateSet(),
                fixture.candidateComparisons(),
                fixture.factorDrilldowns(),
                fixture.warnings(),
                fixture.unknowns(),
                BOUNDARY_NOTE);
        DecisionExplorerRoutingDiagnosticsV1 diagnostics = routingDiagnosticsService.buildDiagnostics(
                summary,
                fixture.candidateSet(),
                fixture.candidateComparisons(),
                fixture.factorDrilldowns(),
                fixture.warnings(),
                fixture.unknowns(),
                BOUNDARY_NOTE);
        return tradeoffService.buildTradeoffs(summary, diagnostics, BOUNDARY_NOTE);
    }

    private static List<String> fingerprint(DecisionExplorerRouteTradeoffAnalysisV1 analysis) {
        return analysis.candidateTradeoffs().stream()
                .map(row -> row.candidateId() + ":" + row.tradeoffCategory() + ":"
                        + row.riskBenefitClassification() + ":" + row.scoreGapCategory() + ":"
                        + row.scoreDeltaFromSelected())
                .toList();
    }

    private static TradeoffFixture strongFixture() {
        CandidateReadoutV1 selected = candidate("edge-a", true, 10.0, List.of("healthState=healthy"));
        CandidateReadoutV1 alternative = candidate("edge-b", false, 15.0, List.of("healthState=healthy"));
        return fixture(
                selected,
                List.of(selected, alternative),
                List.of(
                        comparison("edge-a", true, "SELECTED", 10.0, 0.0,
                                List.of("healthState=healthy"), List.of(), List.of()),
                        comparison("edge-b", false, "COMPARED_TO_SELECTED", 15.0, 5.0,
                                List.of("healthState=healthy"), List.of(), List.of())),
                List.of(
                        factor("edge-a", "healthState", "AVAILABLE", List.of(), List.of()),
                        factor("edge-b", "latency", "AVAILABLE", List.of(), List.of())),
                List.of(),
                List.of("hidden routing internals"));
    }

    private static TradeoffFixture alternativeBeatsSelectedFixture() {
        CandidateReadoutV1 selected = candidate("edge-a", true, 10.0, List.of("healthState=healthy"));
        CandidateReadoutV1 alternative = candidate("edge-b", false, 8.0, List.of("healthState=healthy"));
        return fixture(
                selected,
                List.of(selected, alternative),
                List.of(
                        comparison("edge-a", true, "SELECTED", 10.0, 0.0,
                                List.of("healthState=healthy"), List.of(), List.of()),
                        comparison("edge-b", false, "COMPARED_TO_SELECTED", 8.0, -2.0,
                                List.of("healthState=healthy"), List.of(), List.of())),
                List.of(
                        factor("edge-a", "healthState", "AVAILABLE", List.of(), List.of()),
                        factor("edge-b", "latency", "AVAILABLE", List.of(), List.of())),
                List.of(),
                List.of("hidden routing internals"));
    }

    private static TradeoffFixture unknownAlternativeFixture() {
        CandidateReadoutV1 selected = candidate("edge-a", true, 10.0, List.of("healthState=healthy"));
        CandidateReadoutV1 alternative = candidate("edge-b", false, null, List.of());
        return fixture(
                selected,
                List.of(selected, alternative),
                List.of(
                        comparison("edge-a", true, "SELECTED", 10.0, 0.0,
                                List.of("healthState=healthy"), List.of(), List.of()),
                        comparison("edge-b", false, "PARTIAL_EVIDENCE", null, null,
                                List.of(), List.of("candidate final score was not returned"),
                                List.of("score delta from selected candidate"))),
                List.of(factor("edge-b", "latency", "PARTIAL",
                        List.of("factor evidence is partial"),
                        List.of("numeric contribution value"))),
                List.of(),
                List.of("hidden routing internals"));
    }

    private static TradeoffFixture degradedSelectedFixture() {
        CandidateReadoutV1 selected = candidate("edge-a", true, 10.0, List.of("healthState=false"));
        return fixture(
                selected,
                List.of(selected),
                List.of(comparison("edge-a", true, "SELECTED", 10.0, 0.0,
                        List.of("healthState=false"), List.of(), List.of())),
                List.of(factorWithObserved("edge-a", "healthState", "false", "AVAILABLE",
                        List.of(), List.of())),
                List.of(),
                List.of());
    }

    private static TradeoffFixture fixture(
            CandidateReadoutV1 selected,
            List<CandidateReadoutV1> candidates,
            List<DecisionExplorerCandidateComparisonRowV1> comparisons,
            List<DecisionFactorDrilldownV1> factors,
            List<String> warnings,
            List<String> unknowns) {
        return new TradeoffFixture(
                decisionReadout("SUCCESS", selected.candidateId()),
                selected,
                candidates,
                comparisons,
                factors,
                warnings,
                unknowns);
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

    private static CandidateReadoutV1 candidate(
            String candidateId,
            boolean selected,
            Double finalScore,
            List<String> visibleSignals) {
        return new CandidateReadoutV1(
                candidateId,
                candidateId,
                selected,
                selected ? "SELECTED" : "NOT_SELECTED",
                finalScore,
                visibleSignals,
                List.of("hidden routing internals"),
                List.of(selected ? "SELECTED_CANDIDATE" : "NON_SELECTED_CANDIDATE"),
                List.of("boundary-read-only", "boundary-simulation-only"),
                finalScore == null
                        ? List.of("decision-vector:" + candidateId)
                        : List.of("decision-vector:" + candidateId, "scores:" + candidateId),
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
                finalScore == null
                        ? List.of("decision-vector:" + candidateId)
                        : List.of("decision-vector:" + candidateId, "scores:" + candidateId),
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

    private record TradeoffFixture(
            DecisionReadoutV1 decisionReadout,
            CandidateReadoutV1 selectedCandidate,
            List<CandidateReadoutV1> candidateSet,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            List<DecisionFactorDrilldownV1> factorDrilldowns,
            List<String> warnings,
            List<String> unknowns) {
    }
}
