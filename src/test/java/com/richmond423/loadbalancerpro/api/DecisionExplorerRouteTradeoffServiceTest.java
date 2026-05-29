package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        assertTrue(analysis.explanationText()
                .contains("selected candidate edge-a is STRONG with category SELECTED_ADVANTAGE"));
        assertTrue(analysis.explanationText()
                .contains("closest alternative edge-b has score delta 5.0"));
        assertTrue(analysis.explanationText()
                .contains("evidence sufficiency REPLAY_STYLE_READY with readiness score 100"));
        assertTrue(analysis.explanationText()
                .contains("replay readiness READY with replay execution unavailable"));
        assertTrue(analysis.explanationText().contains(analysis.reproducibilityKey()));
        assertEquals(List.of(
                "edge-a:SELECTED_BASELINE:BASELINE:BASELINE:0.0",
                "edge-b:ALTERNATIVE_TRAILS_SELECTED:SELECTED_ADVANTAGE:MATERIAL:5.0"),
                fingerprint(analysis));
        assertEquals(List.of(
                "edge-a:STRONG:SELECTED_BASELINE_SCORE_PRESENT:STRONG:BASELINE",
                "edge-b:STRONG:ALTERNATIVE_DELTA_PRESENT:STRONG:MATERIAL"),
                scoringFingerprint(analysis));
        assertEquals(List.of("edge-b:latency:NEUTRAL:SUPPORTING:SUPPORTING:MATERIAL:5.0"),
                factorDeltaFingerprint(analysis));
        assertEquals("REPLAY_STYLE_READY", analysis.evidenceSufficiency().sufficiencyLevel());
        assertEquals(100, analysis.evidenceSufficiency().readinessScore());
        assertTrue(analysis.evidenceSufficiency().basicDiagnosticsReady());
        assertTrue(analysis.evidenceSufficiency().tradeoffAnalysisReady());
        assertTrue(analysis.evidenceSufficiency().replayStyleAnalysisReady());
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                analysis.evidenceSufficiency().fingerprintAlgorithm());
        assertEquals("evidence-sufficiency:v1:REPLAY_STYLE_READY:score=100:candidates=2:alternatives=1:"
                + "factorDeltas=1", analysis.evidenceSufficiency().reproducibilityKey());
        assertTrue(analysis.evidenceSufficiency().diagnosticFingerprint()
                .startsWith("evidence-sufficiency|v1|"));
        assertTrue(analysis.evidenceSufficiency().fingerprintInputs()
                .contains("sufficiencyLevel=REPLAY_STYLE_READY"));
        assertEquals("READY", analysis.replayReadinessDiagnostic().readinessStatus());
        assertEquals("AVAILABLE", analysis.replayReadinessDiagnostic().candidateEvidenceStatus());
        assertEquals("AVAILABLE", analysis.replayReadinessDiagnostic().alternativeEvidenceStatus());
        assertEquals("AVAILABLE", analysis.replayReadinessDiagnostic().scoreEvidenceStatus());
        assertEquals("AVAILABLE", analysis.replayReadinessDiagnostic().factorEvidenceStatus());
        assertEquals("AVAILABLE", analysis.replayReadinessDiagnostic().fingerprintEvidenceStatus());
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                analysis.replayReadinessDiagnostic().fingerprintAlgorithm());
        assertEquals("replay-readiness:v1:READY:REPLAY_STYLE_READY:score=100:candidate=AVAILABLE:"
                + "alternative=AVAILABLE:scoreEvidence=AVAILABLE:factor=AVAILABLE:fingerprint=AVAILABLE",
                analysis.replayReadinessDiagnostic().reproducibilityKey());
        assertTrue(analysis.replayReadinessDiagnostic().diagnosticFingerprint()
                .startsWith("replay-readiness|v1|"));
        assertTrue(analysis.replayReadinessDiagnostic().fingerprintInputs()
                .contains("fingerprintEvidenceStatus=AVAILABLE"));
        assertFalse(analysis.replayReadinessDiagnostic().replayExecutionAvailable());
        assertFalse(analysis.replayReadinessDiagnostic().missingEvidenceSignals()
                .contains("diagnostic fingerprint evidence has not been computed yet"));
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                analysis.fingerprintAlgorithm());
        assertEquals("route-tradeoff:v1:STRONG:edge-a:SELECTED_ADVANTAGE:rows=2:alternatives=1:"
                + "sufficiency=REPLAY_STYLE_READY:replay=READY", analysis.reproducibilityKey());
        assertTrue(analysis.diagnosticFingerprint().startsWith("route-tradeoff|v1|"));
        assertTrue(analysis.fingerprintInputs().contains("selectedCandidateId=edge-a"));
        assertTrue(analysis.fingerprintInputs().contains("replayReadiness.readinessStatus=READY"));
        assertTrue(analysis.tradeoffReasons().contains("ROUTE_TRADEOFF_CATEGORY_SELECTED_ADVANTAGE"));
        assertTrue(analysis.candidateTradeoffs().get(1).benefitSignals()
                .contains("alternative trails selected by returned score delta"));
        assertTrue(analysis.candidateScoringExplanations().get(1).summaryText()
                .contains("alternative scoring explanation"));
    }

    @Test
    void alternativeBeatingSelectedMarksSelectedChallengedWithoutChangingRoutingDecision() {
        DecisionExplorerRouteTradeoffAnalysisV1 analysis = tradeoffs(alternativeBeatsSelectedFixture());

        assertEquals("STRONG", analysis.overallStatus());
        assertEquals("edge-a", analysis.selectedCandidateId());
        assertEquals("SELECTED_CHALLENGED", analysis.tradeoffCategory());
        assertEquals("edge-b", analysis.closestAlternativeCandidateId());
        assertEquals(-2.0, analysis.closestAlternativeScoreDelta());
        assertTrue(analysis.explanationText()
                .contains("selected candidate edge-a is STRONG with category SELECTED_CHALLENGED"));
        assertTrue(analysis.explanationText()
                .contains("closest alternative edge-b has score delta -2.0"));
        DecisionExplorerRouteTradeoffRowV1 alternative = analysis.candidateTradeoffs().get(1);
        assertEquals("edge-b", alternative.candidateId());
        assertFalse(alternative.selected());
        assertEquals("ALTERNATIVE_BEATS_SELECTED", alternative.tradeoffCategory());
        assertEquals("RISK", alternative.riskBenefitClassification());
        assertEquals("MATERIAL", alternative.scoreGapCategory());
        assertTrue(alternative.scoringExplanation().contains("score delta -2.0 from selected"));
        assertTrue(alternative.riskSignals().contains("alternative beats selected by returned score delta"));
        assertTrue(alternative.reasonCodes().contains("TRADEOFF_CATEGORY_ALTERNATIVE_BEATS_SELECTED"));
        DecisionExplorerCandidateTradeoffScoringExplanationV1 alternativeExplanation =
                analysis.candidateScoringExplanations().get(1);
        assertEquals("DEGRADED", alternativeExplanation.explanationStatus());
        assertEquals("ALTERNATIVE_DELTA_PRESENT", alternativeExplanation.scoreEvidenceState());
        assertEquals("STRONG", alternativeExplanation.factorStatusRollup());
        assertTrue(alternativeExplanation.limitationSignals()
                .contains("alternative beats selected by returned score delta"));
        assertTrue(alternativeExplanation.reasonCodes()
                .contains("SCORING_EXPLANATION_STATUS_DEGRADED"));
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
        assertTrue(analysis.explanationText()
                .contains("selected candidate edge-a is PARTIAL with category PARTIAL_TRADEOFF"));
        assertTrue(analysis.explanationText()
                .contains("no score-comparable alternative was returned"));
        assertTrue(analysis.explanationText()
                .contains("evidence sufficiency BASIC_DIAGNOSTICS_ONLY"));
        DecisionExplorerRouteTradeoffRowV1 alternative = analysis.candidateTradeoffs().get(1);
        assertEquals("ALTERNATIVE_UNKNOWN", alternative.tradeoffCategory());
        assertEquals("UNKNOWN", alternative.riskBenefitClassification());
        assertTrue(alternative.scoringExplanation()
                .contains("could not be score-compared because its score delta from selected was unavailable"));
        DecisionExplorerCandidateTradeoffScoringExplanationV1 alternativeExplanation =
                analysis.candidateScoringExplanations().get(1);
        assertEquals("UNKNOWN", alternativeExplanation.explanationStatus());
        assertEquals("ALTERNATIVE_DELTA_UNKNOWN", alternativeExplanation.scoreEvidenceState());
        assertTrue(alternativeExplanation.limitationSignals()
                .contains("score evidence is incomplete for tradeoff explanation"));
        assertEquals(List.of("edge-b:latency:UNKNOWN:UNKNOWN:WARNING:UNKNOWN_GAP:null"),
                factorDeltaFingerprint(analysis));
        assertEquals("BASIC_DIAGNOSTICS_ONLY", analysis.evidenceSufficiency().sufficiencyLevel());
        assertTrue(analysis.evidenceSufficiency().basicDiagnosticsReady());
        assertFalse(analysis.evidenceSufficiency().tradeoffAnalysisReady());
        assertFalse(analysis.evidenceSufficiency().replayStyleAnalysisReady());
        assertTrue(analysis.evidenceSufficiency().partialEvidenceSignals()
                .contains("score evidence is partial for candidate edge-b"));
        assertTrue(analysis.evidenceSufficiency().missingEvidenceSignals()
                .contains("score-comparable alternative evidence was not returned"));
        assertEquals("PARTIAL", analysis.replayReadinessDiagnostic().readinessStatus());
        assertEquals("PARTIAL", analysis.replayReadinessDiagnostic().alternativeEvidenceStatus());
        assertEquals("PARTIAL", analysis.replayReadinessDiagnostic().scoreEvidenceStatus());
        assertEquals("PARTIAL", analysis.replayReadinessDiagnostic().factorEvidenceStatus());
        DecisionExplorerFactorTradeoffDeltaV1 factorDelta = analysis.factorTradeoffDeltas().get(0);
        assertTrue(factorDelta.limitationSignals().contains("selected factor evidence was not returned"));
        assertTrue(factorDelta.reasonCodes().contains("SELECTED_FACTOR_MISSING"));
        assertTrue(analysis.unknowns().contains("score delta from selected candidate"));
    }

    @Test
    void factorTradeoffDeltasClassifySelectedAdvantageAndDisadvantageFromFactorEvidence() {
        DecisionExplorerRouteTradeoffAnalysisV1 advantage = tradeoffs(selectedFactorAdvantageFixture());
        DecisionExplorerFactorTradeoffDeltaV1 advantageDelta = advantage.factorTradeoffDeltas().get(0);

        assertEquals("ADVANTAGE", advantageDelta.deltaClassification());
        assertEquals("latency", advantageDelta.factorName());
        assertEquals("SUPPORTING", advantageDelta.selectedContribution());
        assertEquals("WARNING", advantageDelta.alternativeContribution());
        assertTrue(advantageDelta.summaryText().contains("selected=edge-a versus alternative edge-b")
                || advantageDelta.summaryText().contains("selected candidate edge-a versus alternative edge-b"));
        assertTrue(advantageDelta.limitationSignals()
                .contains("alternative warning: influenceCategory=WEAKENS_SELECTION"));
        assertTrue(advantageDelta.reasonCodes().contains("FACTOR_TRADEOFF_DELTA_ADVANTAGE"));

        DecisionExplorerRouteTradeoffAnalysisV1 disadvantage = tradeoffs(selectedFactorDisadvantageFixture());
        DecisionExplorerFactorTradeoffDeltaV1 disadvantageDelta = disadvantage.factorTradeoffDeltas().get(0);

        assertEquals("DISADVANTAGE", disadvantageDelta.deltaClassification());
        assertEquals("WARNING", disadvantageDelta.selectedContribution());
        assertEquals("SUPPORTING", disadvantageDelta.alternativeContribution());
        assertTrue(disadvantageDelta.limitationSignals()
                .contains("selected warning: influenceCategory=WEAKENS_SELECTION"));
        assertTrue(disadvantageDelta.reasonCodes().contains("FACTOR_TRADEOFF_DELTA_DISADVANTAGE"));
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
        assertEquals(List.of("edge-a:DEGRADED:SELECTED_BASELINE_SCORE_PRESENT:DEGRADED:BASELINE"),
                scoringFingerprint(analysis));
        assertEquals("DEGRADED", analysis.evidenceSufficiency().sufficiencyLevel());
        assertFalse(analysis.evidenceSufficiency().tradeoffAnalysisReady());
        assertEquals("DEGRADED", analysis.replayReadinessDiagnostic().readinessStatus());
        assertEquals("DEGRADED", analysis.replayReadinessDiagnostic().sufficiencyLevel());
        assertTrue(analysis.candidateScoringExplanations().get(0).limitationSignals()
                .stream()
                .anyMatch(signal -> signal.contains("healthState=false")
                        || signal.contains("health evidence state is degraded")));
    }

    @Test
    void degradedFactorTradeoffDeltaDoesNotInventHealthyComparison() {
        DecisionExplorerRouteTradeoffAnalysisV1 analysis = tradeoffs(degradedFactorComparisonFixture());

        assertEquals(List.of("edge-b:healthState:DEGRADED:DEGRADED:SUPPORTING:MATERIAL:5.0"),
                factorDeltaFingerprint(analysis));
        DecisionExplorerFactorTradeoffDeltaV1 delta = analysis.factorTradeoffDeltas().get(0);
        assertTrue(delta.limitationSignals().contains("factor tradeoff delta includes degraded evidence"));
        assertTrue(delta.limitationSignals().stream()
                .anyMatch(signal -> signal.contains("selected degraded: health evidence value is degraded")));
        assertTrue(delta.reasonCodes().contains("FACTOR_TRADEOFF_DELTA_DEGRADED"));
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
        assertTrue(analysis.candidateScoringExplanations().isEmpty());
        assertTrue(analysis.factorTradeoffDeltas().isEmpty());
        assertEquals("INSUFFICIENT", analysis.evidenceSufficiency().sufficiencyLevel());
        assertEquals(0, analysis.evidenceSufficiency().readinessScore());
        assertFalse(analysis.evidenceSufficiency().basicDiagnosticsReady());
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                analysis.fingerprintAlgorithm());
        assertTrue(analysis.diagnosticFingerprint().startsWith("route-tradeoff|v1|"));
        assertEquals("route-tradeoff:v1:UNKNOWN:UNKNOWN:0:INSUFFICIENT:UNKNOWN",
                analysis.reproducibilityKey());
        assertTrue(analysis.fingerprintInputs().contains("candidateTradeoffCount=0"));
        assertTrue(analysis.evidenceSufficiency().diagnosticFingerprint()
                .startsWith("evidence-sufficiency|v1|"));
        assertEquals("UNKNOWN", analysis.replayReadinessDiagnostic().readinessStatus());
        assertEquals("UNKNOWN", analysis.replayReadinessDiagnostic().candidateEvidenceStatus());
        assertTrue(analysis.replayReadinessDiagnostic().diagnosticFingerprint()
                .startsWith("replay-readiness|v1|"));
        assertTrue(analysis.explanationText().contains("Route tradeoff explanation is UNKNOWN"));
        assertTrue(analysis.explanationText().contains("routing diagnostics were unavailable"));
        assertFalse(analysis.replayReadinessDiagnostic().replayStorageAvailable());
        assertFalse(analysis.replayReadinessDiagnostic().replayExportAvailable());
        assertEquals(List.of("ROUTING_DIAGNOSTICS_UNAVAILABLE"), analysis.tradeoffReasons());
        assertTrue(analysis.unknowns().contains("route tradeoff diagnostics were unavailable"));
        assertEquals("UNKNOWN", analysis.boundaryNote());
    }

    @Test
    void diagnosticFingerprintsAreStableAndReflectComputedEvidenceChanges() {
        DecisionExplorerRouteTradeoffAnalysisV1 first = tradeoffs(strongFixture());
        DecisionExplorerRouteTradeoffAnalysisV1 second = tradeoffs(strongFixture());
        DecisionExplorerRouteTradeoffAnalysisV1 partial = tradeoffs(unknownAlternativeFixture());

        assertEquals(first.diagnosticFingerprint(), second.diagnosticFingerprint());
        assertEquals(first.reproducibilityKey(), second.reproducibilityKey());
        assertEquals(first.evidenceSufficiency().diagnosticFingerprint(),
                second.evidenceSufficiency().diagnosticFingerprint());
        assertEquals(first.replayReadinessDiagnostic().diagnosticFingerprint(),
                second.replayReadinessDiagnostic().diagnosticFingerprint());
        assertEquals(first.explanationText(), second.explanationText());

        assertNotEquals(first.diagnosticFingerprint(), partial.diagnosticFingerprint());
        assertNotEquals(first.explanationText(), partial.explanationText());
        assertNotEquals(first.evidenceSufficiency().diagnosticFingerprint(),
                partial.evidenceSufficiency().diagnosticFingerprint());
        assertNotEquals(first.replayReadinessDiagnostic().diagnosticFingerprint(),
                partial.replayReadinessDiagnostic().diagnosticFingerprint());
    }

    @Test
    void tradeoffSourceDoesNotUseRoutingMutationExternalServicesOrPersistence() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                + "DecisionExplorerRouteTradeoffService.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerRouteTradeoffRowBuilder.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCandidateTradeoffScoringBuilder.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerFactorTradeoffDeltaBuilder.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCandidateTradeoffScoringExplanationV1.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerRouteTradeoffAnalysisV1.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerFactorTradeoffDeltaV1.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerEvidenceSufficiencyV1.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerReplayReadinessDiagnosticV1.java"), StandardCharsets.UTF_8)
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

    private static List<String> scoringFingerprint(DecisionExplorerRouteTradeoffAnalysisV1 analysis) {
        return analysis.candidateScoringExplanations().stream()
                .map(explanation -> explanation.candidateId() + ":" + explanation.explanationStatus() + ":"
                        + explanation.scoreEvidenceState() + ":" + explanation.factorStatusRollup() + ":"
                        + explanation.scoreGapCategory())
                .toList();
    }

    private static List<String> factorDeltaFingerprint(DecisionExplorerRouteTradeoffAnalysisV1 analysis) {
        return analysis.factorTradeoffDeltas().stream()
                .map(delta -> delta.alternativeCandidateId() + ":" + delta.factorName() + ":"
                        + delta.deltaClassification() + ":" + delta.selectedContribution() + ":"
                        + delta.alternativeContribution() + ":" + delta.scoreGapCategory() + ":"
                        + delta.alternativeScoreDeltaFromSelected())
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
                        factor("edge-a", "latency", "AVAILABLE", List.of(), List.of()),
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

    private static TradeoffFixture selectedFactorAdvantageFixture() {
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
                        factorWithInfluence("edge-a", "latency", "raw value", "SUPPORTS_SELECTION",
                                "AVAILABLE", List.of(), List.of()),
                        factorWithInfluence("edge-b", "latency", "raw value", "WEAKENS_SELECTION",
                                "AVAILABLE", List.of(), List.of())),
                List.of(),
                List.of("hidden routing internals"));
    }

    private static TradeoffFixture selectedFactorDisadvantageFixture() {
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
                        factorWithInfluence("edge-a", "latency", "raw value", "WEAKENS_SELECTION",
                                "AVAILABLE", List.of(), List.of()),
                        factorWithInfluence("edge-b", "latency", "raw value", "SUPPORTS_SELECTION",
                                "AVAILABLE", List.of(), List.of())),
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

    private static TradeoffFixture degradedFactorComparisonFixture() {
        CandidateReadoutV1 selected = candidate("edge-a", true, 10.0, List.of("healthState=false"));
        CandidateReadoutV1 alternative = candidate("edge-b", false, 15.0, List.of("healthState=healthy"));
        return fixture(
                selected,
                List.of(selected, alternative),
                List.of(
                        comparison("edge-a", true, "SELECTED", 10.0, 0.0,
                                List.of("healthState=false"), List.of(), List.of()),
                        comparison("edge-b", false, "COMPARED_TO_SELECTED", 15.0, 5.0,
                                List.of("healthState=healthy"), List.of(), List.of())),
                List.of(
                        factorWithObserved("edge-a", "healthState", "false", "AVAILABLE",
                                List.of(), List.of()),
                        factorWithObserved("edge-b", "healthState", "true", "AVAILABLE",
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
        return factorWithInfluence(candidateId, factorName, observedValue, "SUPPORTS_SELECTION", evidenceStatus,
                warnings, unknowns);
    }

    private static DecisionFactorDrilldownV1 factorWithInfluence(
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
