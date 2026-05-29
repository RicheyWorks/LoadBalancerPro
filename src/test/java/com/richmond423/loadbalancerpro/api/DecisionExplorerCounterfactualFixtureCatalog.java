package com.richmond423.loadbalancerpro.api;

import java.util.List;

final class DecisionExplorerCounterfactualFixtureCatalog {
    static final String BOUNDARY_NOTE = "local-only counterfactual diagnostics";

    private static final DecisionExplorerConfidenceSummaryService CONFIDENCE_SUMMARY_SERVICE =
            new DecisionExplorerConfidenceSummaryService();
    private static final DecisionExplorerRoutingDiagnosticsService ROUTING_DIAGNOSTICS_SERVICE =
            new DecisionExplorerRoutingDiagnosticsService();
    private static final DecisionExplorerRouteTradeoffService ROUTE_TRADEOFF_SERVICE =
            new DecisionExplorerRouteTradeoffService();
    private static final DecisionExplorerShadowDecisionQualityService SHADOW_DECISION_QUALITY_SERVICE =
            new DecisionExplorerShadowDecisionQualityService();
    private static final DecisionExplorerCounterfactualAnalysisService COUNTERFACTUAL_SERVICE =
            new DecisionExplorerCounterfactualAnalysisService();

    private DecisionExplorerCounterfactualFixtureCatalog() {
    }

    static List<CounterfactualFixture> fixtures() {
        return List.of(
                stableSelectedAdvantage(),
                sensitivePartialEvidence(),
                closeAlternative(),
                degradedSelected(),
                insufficientEvidence(),
                unknownEmptyEvidence());
    }

    static CounterfactualFixture stableSelectedAdvantage() {
        CandidateReadoutV1 selected = candidate("edge-a", true, 10.0, List.of("healthState=healthy"));
        CandidateReadoutV1 alternative = candidate("edge-b", false, 15.0, List.of("healthState=healthy"));
        return fixture(
                "counterfactual-stable-selected-advantage",
                DecisionExplorerCounterfactualAnalysisV1.LABEL_STABLE,
                DecisionExplorerCounterfactualAnalysisV1.BAND_LOW,
                3,
                2,
                1,
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

    static CounterfactualFixture sensitivePartialEvidence() {
        CandidateReadoutV1 selected = candidate("edge-a", true, 10.0, List.of("healthState=healthy"));
        CandidateReadoutV1 alternative = candidate("edge-b", false, null, List.of());
        return fixture(
                "counterfactual-sensitive-partial-evidence",
                DecisionExplorerCounterfactualAnalysisV1.LABEL_SENSITIVE,
                DecisionExplorerCounterfactualAnalysisV1.BAND_MEDIUM,
                3,
                2,
                1,
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

    static CounterfactualFixture closeAlternative() {
        CandidateReadoutV1 selected = candidate("edge-a", true, 10.0, List.of("healthState=healthy"));
        CandidateReadoutV1 alternative = candidate("edge-b", false, 10.5, List.of("healthState=healthy"));
        return fixture(
                "counterfactual-close-alternative",
                DecisionExplorerCounterfactualAnalysisV1.LABEL_CLOSE_CALL,
                DecisionExplorerCounterfactualAnalysisV1.BAND_MEDIUM,
                3,
                2,
                1,
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

    static CounterfactualFixture degradedSelected() {
        CandidateReadoutV1 selected = candidate("edge-a", true, 10.0, List.of("healthState=false"));
        return fixture(
                "counterfactual-degraded-selected",
                DecisionExplorerCounterfactualAnalysisV1.LABEL_DEGRADED,
                DecisionExplorerCounterfactualAnalysisV1.BAND_HIGH,
                3,
                1,
                0,
                selected,
                List.of(selected),
                List.of(comparison("edge-a", true, "SELECTED", 10.0, 0.0,
                        List.of("healthState=false"), List.of(), List.of())),
                List.of(factor("edge-a", "healthState", "SUPPORTS_SELECTION", "AVAILABLE",
                        List.of("health evidence value is degraded"), List.of())),
                List.of(),
                List.of());
    }

    static CounterfactualFixture insufficientEvidence() {
        DecisionExplorerCounterfactualAnalysisV1 analysis = COUNTERFACTUAL_SERVICE.buildAnalysis(
                DecisionExplorerConfidenceSummaryV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerRoutingDiagnosticsV1.unknown(BOUNDARY_NOTE),
                DecisionExplorerRouteTradeoffAnalysisV1.unknown(BOUNDARY_NOTE),
                insufficientQuality(),
                BOUNDARY_NOTE);
        return new CounterfactualFixture(
                "counterfactual-insufficient-evidence",
                DecisionExplorerCounterfactualAnalysisV1.LABEL_INSUFFICIENT_EVIDENCE,
                DecisionExplorerCounterfactualAnalysisV1.BAND_INSUFFICIENT,
                1,
                0,
                0,
                analysis);
    }

    static CounterfactualFixture unknownEmptyEvidence() {
        return new CounterfactualFixture(
                "counterfactual-unknown-empty-evidence",
                DecisionExplorerCounterfactualAnalysisV1.LABEL_UNKNOWN,
                DecisionExplorerCounterfactualAnalysisV1.BAND_UNKNOWN,
                0,
                0,
                0,
                COUNTERFACTUAL_SERVICE.buildAnalysis(null, null, null, null, BOUNDARY_NOTE));
    }

    private static CounterfactualFixture fixture(
            String fixtureId,
            String expectedLabel,
            String expectedBand,
            int expectedPolicyWeightScenarioCount,
            int expectedCandidateOutcomeCount,
            int expectedFactorWeightDeltaCount,
            CandidateReadoutV1 selected,
            List<CandidateReadoutV1> candidates,
            List<DecisionExplorerCandidateComparisonRowV1> comparisons,
            List<DecisionFactorDrilldownV1> factors,
            List<String> warnings,
            List<String> unknowns) {
        DecisionExplorerConfidenceSummaryV1 summary = CONFIDENCE_SUMMARY_SERVICE.buildSummary(
                new DecisionReadoutV1(
                        "decision-" + fixtureId,
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
                unknowns,
                BOUNDARY_NOTE);
        DecisionExplorerRoutingDiagnosticsV1 diagnostics = ROUTING_DIAGNOSTICS_SERVICE.buildDiagnostics(
                summary,
                candidates,
                comparisons,
                factors,
                warnings,
                unknowns,
                BOUNDARY_NOTE);
        DecisionExplorerRouteTradeoffAnalysisV1 tradeoff = ROUTE_TRADEOFF_SERVICE.buildTradeoffs(
                summary,
                diagnostics,
                BOUNDARY_NOTE);
        DecisionExplorerShadowDecisionQualityEvaluationV1 quality = SHADOW_DECISION_QUALITY_SERVICE.buildEvaluation(
                summary,
                diagnostics,
                tradeoff,
                BOUNDARY_NOTE);
        return new CounterfactualFixture(
                fixtureId,
                expectedLabel,
                expectedBand,
                expectedPolicyWeightScenarioCount,
                expectedCandidateOutcomeCount,
                expectedFactorWeightDeltaCount,
                COUNTERFACTUAL_SERVICE.buildAnalysis(summary, diagnostics, tradeoff, quality, BOUNDARY_NOTE));
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

    record CounterfactualFixture(
            String fixtureId,
            String expectedLabel,
            String expectedBand,
            int expectedPolicyWeightScenarioCount,
            int expectedCandidateOutcomeCount,
            int expectedFactorWeightDeltaCount,
            DecisionExplorerCounterfactualAnalysisV1 analysis) {
    }
}
