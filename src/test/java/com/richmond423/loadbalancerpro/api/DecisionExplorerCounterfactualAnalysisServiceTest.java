package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class DecisionExplorerCounterfactualAnalysisServiceTest {
    private static final String BOUNDARY_NOTE = "local-only counterfactual diagnostics";
    private final DecisionExplorerConfidenceSummaryService confidenceSummaryService =
            new DecisionExplorerConfidenceSummaryService();
    private final DecisionExplorerRoutingDiagnosticsService routingDiagnosticsService =
            new DecisionExplorerRoutingDiagnosticsService();
    private final DecisionExplorerRouteTradeoffService routeTradeoffService =
            new DecisionExplorerRouteTradeoffService();
    private final DecisionExplorerShadowDecisionQualityService shadowDecisionQualityService =
            new DecisionExplorerShadowDecisionQualityService();
    private final DecisionExplorerCounterfactualAnalysisService counterfactualService =
            new DecisionExplorerCounterfactualAnalysisService();

    @Test
    void strongSelectedAdvantageClassifiesAsStableWithoutPolicyMutation() {
        DecisionExplorerCounterfactualAnalysisV1 analysis = counterfactual(strongFixture());

        assertTrue(analysis.readOnly());
        assertTrue(analysis.simulationOnly());
        assertTrue(analysis.localOnly());
        assertEquals("DecisionExplorerCounterfactualAnalysisV1", analysis.analysisObject());
        assertEquals("v1", analysis.contractVersion());
        assertEquals("edge-a", analysis.selectedCandidateId());
        assertEquals("STABLE", analysis.counterfactualLabel());
        assertEquals("LOW", analysis.sensitivityBand());
        assertEquals("STRONG", analysis.confidenceStatus());
        assertEquals("ACCEPTABLE", analysis.decisionQualityLabel());
        assertEquals("SELECTED_ADVANTAGE", analysis.tradeoffCategory());
        assertEquals("REPLAY_STYLE_READY", analysis.evidenceSufficiencyLevel());
        assertEquals("READY", analysis.replayReadinessStatus());
        assertEquals("RETURNED_EVIDENCE_WEIGHTS", analysis.baselinePolicyProfile());
        assertEquals(3, analysis.policyWeightScenarioCount());
        assertEquals(2, analysis.candidateOutcomeCount());
        assertEquals(1, analysis.factorDeltaCount());
        assertEquals(1, analysis.factorWeightDeltaCount());
        assertEquals(List.of(
                        "BASELINE_RETURNED_EVIDENCE",
                        "SELECTED_SUPPORT_PLUS_10",
                        "ALTERNATIVE_SUPPORT_PLUS_10"),
                analysis.policyWeightScenarios().stream()
                        .map(DecisionExplorerCounterfactualPolicyWeightScenarioV1::scenarioId)
                        .toList());
        assertEquals(List.of("STABLE", "STABLE", "STABLE"),
                analysis.policyWeightScenarios().stream()
                        .map(DecisionExplorerCounterfactualPolicyWeightScenarioV1::sensitivityLabel)
                        .toList());
        assertEquals("LOCAL_ALTERNATIVE_SUPPORT_PLUS_10",
                analysis.policyWeightScenarios().get(2).policyWeightProfile());
        assertFalse(analysis.policyWeightScenarios().get(2).alternativeBecomesCloseOrPreferable());
        assertTrue(analysis.policyWeightScenarios().get(0).summaryText()
                .contains("does not change production scoring or routing"));
        assertEquals(2, analysis.counterfactualCandidateOutcomeCount());
        assertEquals(List.of("SELECTED_STABLE", "ALTERNATIVE_TRAILING"),
                analysis.counterfactualCandidateOutcomes().stream()
                        .map(DecisionExplorerCounterfactualCandidateOutcomeV1::counterfactualOutcomeLabel)
                        .toList());
        assertEquals(List.of("STABILIZING"), analysis.factorWeightDeltas().stream()
                .map(DecisionExplorerCounterfactualFactorWeightDeltaV1::factorWeightDeltaClassification)
                .toList());
        assertTrue(analysis.factorWeightDeltas().get(0).selectedSupportStabilizesDecision());
        assertTrue(analysis.stableSignals().contains("confidence status is STRONG"));
        assertTrue(analysis.stableSignals()
                .contains("selected candidate has returned-evidence tradeoff advantage"));
        assertTrue(analysis.summaryText().contains("no production routing, scoring, proxying"));
        assertTrue(analysis.reasonCodes().contains("COUNTERFACTUAL_ANALYSIS_STABLE"));
        assertTrue(analysis.diagnosticFingerprint().startsWith("counterfactual-analysis|v1|"));
        assertEquals("counterfactual:v1:STABLE:edge-a:SELECTED_ADVANTAGE:quality=ACCEPTABLE:"
                + "sufficiency=REPLAY_STYLE_READY:replay=READY:scenarios=3:outcomes=2:factorWeightDeltas=1",
                analysis.reproducibilityKey());
    }

    @Test
    void closeAlternativeClassifiesAsCloseCallFromComputedTradeoff() {
        DecisionExplorerCounterfactualAnalysisV1 analysis = counterfactual(closeAlternativeFixture());

        assertEquals("CLOSE_CALL", analysis.counterfactualLabel());
        assertEquals("MEDIUM", analysis.sensitivityBand());
        assertEquals("CLOSE_ALTERNATIVE", analysis.tradeoffCategory());
        assertTrue(analysis.sensitivitySignals()
                .contains("route tradeoff category is CLOSE_ALTERNATIVE"));
        assertTrue(analysis.sensitivitySignals()
                .contains("closest alternative score delta is within local close-call band"));
        assertEquals(3, analysis.policyWeightScenarioCount());
        assertTrue(analysis.policyWeightScenarios().get(2).alternativeBecomesCloseOrPreferable());
        assertEquals("CLOSE_CALL", analysis.policyWeightScenarios().get(2).sensitivityLabel());
        assertEquals(List.of("SELECTED_SENSITIVE", "ALTERNATIVE_CLOSE_CALL"),
                analysis.counterfactualCandidateOutcomes().stream()
                        .map(DecisionExplorerCounterfactualCandidateOutcomeV1::counterfactualOutcomeLabel)
                        .toList());
        assertEquals(List.of("NEUTRAL"), analysis.factorWeightDeltas().stream()
                .map(DecisionExplorerCounterfactualFactorWeightDeltaV1::factorWeightDeltaClassification)
                .toList());
        assertTrue(analysis.reasonCodes().contains("COUNTERFACTUAL_ANALYSIS_CLOSE_CALL"));
    }

    @Test
    void partialTradeoffClassifiesAsSensitiveWithSafeLimitations() {
        DecisionExplorerCounterfactualAnalysisV1 analysis = counterfactual(partialAlternativeFixture());

        assertEquals("SENSITIVE", analysis.counterfactualLabel());
        assertEquals("MEDIUM", analysis.sensitivityBand());
        assertEquals("PARTIAL", analysis.confidenceStatus());
        assertEquals("PARTIAL_TRADEOFF", analysis.tradeoffCategory());
        assertTrue(analysis.sensitivitySignals().contains("confidence status is PARTIAL"));
        assertEquals(3, analysis.policyWeightScenarioCount());
        assertEquals(List.of("SENSITIVE", "SENSITIVE", "SENSITIVE"),
                analysis.policyWeightScenarios().stream()
                        .map(DecisionExplorerCounterfactualPolicyWeightScenarioV1::sensitivityLabel)
                        .toList());
        assertEquals(List.of("SELECTED_SENSITIVE", "ALTERNATIVE_UNKNOWN"),
                analysis.counterfactualCandidateOutcomes().stream()
                        .map(DecisionExplorerCounterfactualCandidateOutcomeV1::counterfactualOutcomeLabel)
                        .toList());
        assertEquals(List.of("UNKNOWN"), analysis.factorWeightDeltas().stream()
                .map(DecisionExplorerCounterfactualFactorWeightDeltaV1::factorWeightDeltaClassification)
                .toList());
        assertTrue(analysis.limitationSignals().contains("score-comparable alternative evidence was not returned"));
        assertTrue(analysis.unknowns().contains("score delta from selected candidate"));
    }

    @Test
    void degradedEvidenceClassifiesAsDegradedBeforeSensitivity() {
        DecisionExplorerCounterfactualAnalysisV1 analysis = counterfactual(degradedSelectedFixture());

        assertEquals("DEGRADED", analysis.counterfactualLabel());
        assertEquals("HIGH", analysis.sensitivityBand());
        assertEquals("DEGRADED", analysis.confidenceStatus());
        assertEquals("DEGRADED_DECISION", analysis.decisionQualityLabel());
        assertEquals("DEGRADED", analysis.tradeoffCategory());
        assertTrue(analysis.limitationSignals().contains("confidence status is DEGRADED"));
        assertEquals(3, analysis.policyWeightScenarioCount());
        assertEquals(List.of("DEGRADED", "DEGRADED", "DEGRADED"),
                analysis.policyWeightScenarios().stream()
                        .map(DecisionExplorerCounterfactualPolicyWeightScenarioV1::sensitivityLabel)
                        .toList());
        assertEquals(List.of("SELECTED_DEGRADED"),
                analysis.counterfactualCandidateOutcomes().stream()
                        .map(DecisionExplorerCounterfactualCandidateOutcomeV1::counterfactualOutcomeLabel)
                        .toList());
        assertEquals(0, analysis.factorWeightDeltaCount());
        assertTrue(analysis.reasonCodes().contains("COUNTERFACTUAL_ANALYSIS_DEGRADED"));
    }

    @Test
    void insufficientShadowQualityClassifiesAsInsufficientEvidence() {
        DecisionExplorerConfidenceSummaryV1 summary = DecisionExplorerConfidenceSummaryV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerRoutingDiagnosticsV1 diagnostics = DecisionExplorerRoutingDiagnosticsV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerRouteTradeoffAnalysisV1 tradeoff =
                DecisionExplorerRouteTradeoffAnalysisV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerShadowDecisionQualityEvaluationV1 quality = insufficientQuality();

        DecisionExplorerCounterfactualAnalysisV1 analysis = counterfactualService.buildAnalysis(
                summary,
                diagnostics,
                tradeoff,
                quality,
                BOUNDARY_NOTE);

        assertEquals("INSUFFICIENT_EVIDENCE", analysis.counterfactualLabel());
        assertEquals("INSUFFICIENT", analysis.sensitivityBand());
        assertEquals("INSUFFICIENT_EVIDENCE", analysis.decisionQualityLabel());
        assertTrue(analysis.limitationSignals()
                .contains("shadow decision quality reports INSUFFICIENT_EVIDENCE"));
        assertEquals(1, analysis.policyWeightScenarioCount());
        assertEquals("INSUFFICIENT_EVIDENCE", analysis.policyWeightScenarios().get(0).sensitivityLabel());
        assertEquals(0, analysis.counterfactualCandidateOutcomeCount());
        assertEquals(0, analysis.factorWeightDeltaCount());
        assertTrue(analysis.reasonCodes().contains("COUNTERFACTUAL_ANALYSIS_INSUFFICIENT_EVIDENCE"));
    }

    @Test
    void nullInputsReturnUnknownWithoutInventingCounterfactualEvidence() {
        DecisionExplorerCounterfactualAnalysisV1 analysis =
                counterfactualService.buildAnalysis(null, null, null, null, null);

        assertEquals("UNKNOWN", analysis.counterfactualLabel());
        assertEquals("UNKNOWN", analysis.sensitivityBand());
        assertEquals("UNKNOWN", analysis.selectedCandidateId());
        assertEquals(0, analysis.policyWeightScenarioCount());
        assertEquals(0, analysis.counterfactualCandidateOutcomeCount());
        assertEquals(0, analysis.factorWeightDeltaCount());
        assertEquals(0, analysis.candidateOutcomeCount());
        assertEquals(0, analysis.factorDeltaCount());
        assertTrue(analysis.limitationSignals()
                .contains("computed Decision Explorer evidence was unavailable"));
        assertEquals("counterfactual:v1:UNKNOWN:UNKNOWN:UNKNOWN:quality=UNKNOWN:"
                + "sufficiency=INSUFFICIENT:replay=UNKNOWN:scenarios=0:outcomes=0:factorWeightDeltas=0",
                analysis.reproducibilityKey());
    }

    @Test
    void fingerprintsAreStableAndChangeWhenComputedEvidenceChanges() {
        DecisionExplorerCounterfactualAnalysisV1 first = counterfactual(strongFixture());
        DecisionExplorerCounterfactualAnalysisV1 second = counterfactual(strongFixture());
        DecisionExplorerCounterfactualAnalysisV1 close = counterfactual(closeAlternativeFixture());

        assertEquals(first.diagnosticFingerprint(), second.diagnosticFingerprint());
        assertEquals(first.reproducibilityKey(), second.reproducibilityKey());
        assertEquals(first.fingerprintInputs(), second.fingerprintInputs());
        assertNotEquals(first.diagnosticFingerprint(), close.diagnosticFingerprint());
        assertNotEquals(first.reproducibilityKey(), close.reproducibilityKey());
    }

    @Test
    void counterfactualFoundationDoesNotUseProductionRoutingMutationOrExternalServices() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualAnalysisService.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualLabelEvaluator.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualPolicyWeightScenarioBuilder.java"),
                        StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualPolicyWeightScenarioV1.java"),
                        StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualCandidateOutcomeEvaluator.java"),
                        StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualCandidateOutcomeV1.java"),
                        StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualFactorWeightDeltaEvaluator.java"),
                        StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualFactorWeightDeltaV1.java"),
                        StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualAnalysisV1.java"), StandardCharsets.UTF_8);
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
                "distributeload",
                "addserver",
                "removeserver",
                "proxyclient",
                "trafficshifter")) {
            assertFalse(normalized.contains(forbidden), "counterfactual source must not contain " + forbidden);
        }
    }

    private DecisionExplorerCounterfactualAnalysisV1 counterfactual(CounterfactualFixture fixture) {
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
        DecisionExplorerRouteTradeoffAnalysisV1 tradeoff = routeTradeoffService.buildTradeoffs(
                summary,
                diagnostics,
                BOUNDARY_NOTE);
        DecisionExplorerShadowDecisionQualityEvaluationV1 quality = shadowDecisionQualityService.buildEvaluation(
                summary,
                diagnostics,
                tradeoff,
                BOUNDARY_NOTE);
        return counterfactualService.buildAnalysis(summary, diagnostics, tradeoff, quality, BOUNDARY_NOTE);
    }

    private static CounterfactualFixture strongFixture() {
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
                        factor("edge-a", "latency", "SUPPORTS_SELECTION", "AVAILABLE", List.of(), List.of()),
                        factor("edge-b", "latency", "WEAKENS_SELECTION", "AVAILABLE", List.of(), List.of())),
                List.of(),
                List.of("hidden routing internals"));
    }

    private static CounterfactualFixture closeAlternativeFixture() {
        CandidateReadoutV1 selected = candidate("edge-a", true, 10.0, List.of("healthState=healthy"));
        CandidateReadoutV1 alternative = candidate("edge-b", false, 10.5, List.of("healthState=healthy"));
        return fixture(
                selected,
                List.of(selected, alternative),
                List.of(
                        comparison("edge-a", true, "SELECTED", 10.0, 0.0,
                                List.of("healthState=healthy"), List.of(), List.of()),
                        comparison("edge-b", false, "COMPARED_TO_SELECTED", 10.5, 0.5,
                                List.of("healthState=healthy"), List.of(), List.of())),
                List.of(
                        factor("edge-a", "latency", "SUPPORTS_SELECTION", "AVAILABLE", List.of(), List.of()),
                        factor("edge-b", "latency", "SUPPORTS_SELECTION", "AVAILABLE", List.of(), List.of())),
                List.of(),
                List.of("hidden routing internals"));
    }

    private static CounterfactualFixture partialAlternativeFixture() {
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
                List.of(factor("edge-b", "latency", "SUPPORTS_SELECTION", "PARTIAL",
                        List.of("factor evidence is partial"), List.of("numeric contribution value"))),
                List.of(),
                List.of("hidden routing internals"));
    }

    private static CounterfactualFixture degradedSelectedFixture() {
        CandidateReadoutV1 selected = candidate("edge-a", true, 10.0, List.of("healthState=false"));
        return fixture(
                selected,
                List.of(selected),
                List.of(comparison("edge-a", true, "SELECTED", 10.0, 0.0,
                        List.of("healthState=false"), List.of(), List.of())),
                List.of(factor("edge-a", "healthState", "SUPPORTS_SELECTION", "AVAILABLE",
                        List.of("health evidence value is degraded"), List.of())),
                List.of(),
                List.of());
    }

    private static CounterfactualFixture fixture(
            CandidateReadoutV1 selected,
            List<CandidateReadoutV1> candidates,
            List<DecisionExplorerCandidateComparisonRowV1> comparisons,
            List<DecisionFactorDrilldownV1> factors,
            List<String> warnings,
            List<String> unknowns) {
        return new CounterfactualFixture(
                new DecisionReadoutV1(
                        "decision-1",
                        "SUCCESS",
                        selected.candidateId(),
                        "TAIL_LATENCY_POWER_OF_TWO",
                        "summary",
                        List.of("SELECTED_CANDIDATE_RETURNED"),
                        List.of("routing-comparison-result", "decision-vector"),
                        BOUNDARY_NOTE),
                selected,
                candidates,
                comparisons,
                factors,
                warnings,
                unknowns);
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
            String influenceCategory,
            String evidenceStatus,
            List<String> warnings,
            List<String> unknowns) {
        return new DecisionFactorDrilldownV1(
                factorName,
                candidateId,
                "raw value",
                influenceCategory,
                evidenceStatus,
                "factor explanation",
                warnings,
                unknowns,
                List.of("decision-vector:" + candidateId, "factor-contribution:" + candidateId + ":" + factorName),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerShadowDecisionQualityEvaluationV1 insufficientQuality() {
        return new DecisionExplorerShadowDecisionQualityEvaluationV1(
                true,
                true,
                DecisionExplorerShadowDecisionQualityEvaluationV1.EVALUATION_OBJECT,
                DecisionExplorerShadowDecisionQualityEvaluationV1.CONTRACT_VERSION,
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_INSUFFICIENT_EVIDENCE,
                DecisionExplorerShadowDecisionQualityEvaluationV1.BAND_INSUFFICIENT,
                15,
                "UNKNOWN",
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_UNKNOWN,
                "UNKNOWN",
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT,
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN,
                0,
                List.of(),
                DecisionExplorerShadowPolicySensitivityDiagnosticV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerShadowScenarioInputQualityV1.unknown(BOUNDARY_NOTE),
                0,
                0,
                "insufficient evidence",
                "selected basis unavailable",
                List.of(),
                List.of(),
                List.of("SHADOW_DECISION_QUALITY_INSUFFICIENT_EVIDENCE"),
                List.of(),
                List.of("shadow quality evidence was insufficient"),
                List.of("shadow-quality"),
                "insufficient explanation",
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "shadow-decision-quality|test|insufficient",
                "shadow-decision-quality:v1:INSUFFICIENT_EVIDENCE",
                List.of("qualityLabel=INSUFFICIENT_EVIDENCE"),
                BOUNDARY_NOTE);
    }

    private record CounterfactualFixture(
            DecisionReadoutV1 decisionReadout,
            CandidateReadoutV1 selectedCandidate,
            List<CandidateReadoutV1> candidateSet,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            List<DecisionFactorDrilldownV1> factorDrilldowns,
            List<String> warnings,
            List<String> unknowns) {
    }
}
