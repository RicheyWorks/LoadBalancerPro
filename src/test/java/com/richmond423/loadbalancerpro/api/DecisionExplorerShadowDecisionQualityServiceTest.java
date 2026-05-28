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

class DecisionExplorerShadowDecisionQualityServiceTest {
    private static final String BOUNDARY_NOTE = "local-only shadow decision-quality evaluation";
    private final DecisionExplorerShadowDecisionQualityService service =
            new DecisionExplorerShadowDecisionQualityService();

    @Test
    void acceptableDecisionUsesStrongSelectedAdvantageEvidence() {
        DecisionExplorerShadowDecisionQualityEvaluationV1 evaluation = evaluate(strongSelectedAdvantage());

        assertEquals("ACCEPTABLE", evaluation.qualityLabel());
        assertEquals("HIGH", evaluation.qualityBand());
        assertEquals(100, evaluation.qualityScore());
        assertEquals("edge-a", evaluation.selectedCandidateId());
        assertEquals("STRONG", evaluation.confidenceStatus());
        assertEquals("COMPLETE", evaluation.evidenceQuality());
        assertEquals("SELECTED_ADVANTAGE", evaluation.tradeoffCategory());
        assertEquals("REPLAY_STYLE_READY", evaluation.evidenceSufficiencyLevel());
        assertEquals("READY", evaluation.replayReadinessStatus());
        assertEquals(2, evaluation.candidateOutcomeCount());
        assertEquals(2, evaluation.candidateOutcomeComparisons().size());
        assertEquals("SELECTED_BASELINE", evaluation.candidateOutcomeComparisons().get(0).outcomeLabel());
        assertEquals("SUPPORTS_DECISION", evaluation.candidateOutcomeComparisons().get(0).qualityImpact());
        assertEquals("edge-a", evaluation.candidateOutcomeComparisons().get(0).candidateId());
        assertEquals("ACCEPTABLE_ALTERNATIVE", evaluation.candidateOutcomeComparisons().get(1).outcomeLabel());
        assertEquals("edge-b", evaluation.candidateOutcomeComparisons().get(1).candidateId());
        assertTrue(evaluation.evidenceBasis().contains("routeTradeoffCategory=SELECTED_ADVANTAGE"));
        assertTrue(evaluation.evidenceBasis().contains("replayExecutionAvailable=false"));
        assertTrue(evaluation.selectedCandidateBasis().contains("selectedCandidateId=edge-a"));
        assertTrue(evaluation.selectedCandidateBasis().contains("closestAlternativeScoreDelta=5.0"));
        assertTrue(evaluation.qualityReasons().contains("SHADOW_DECISION_QUALITY_ACCEPTABLE"));
        assertTrue(evaluation.evidenceBasisSummary()
                .contains("classified selected candidate edge-a as ACCEPTABLE"));
        assertTrue(evaluation.evidenceBasisSummary().contains("no production routing decision is changed"));
        assertFalse(evaluation.warnings().contains("production routing decision changed"));
    }

    @Test
    void challengedOrCloseAlternativeRecommendsReviewWithoutMutatingRouting() {
        DecisionExplorerShadowDecisionQualityEvaluationV1 evaluation = evaluate(selectedChallenged());

        assertEquals("REVIEW_RECOMMENDED", evaluation.qualityLabel());
        assertEquals("MEDIUM", evaluation.qualityBand());
        assertEquals(75, evaluation.qualityScore());
        assertEquals("SELECTED_CHALLENGED", evaluation.tradeoffCategory());
        assertEquals("SAFER_ALTERNATIVE", evaluation.candidateOutcomeComparisons().get(1).outcomeLabel());
        assertEquals("REVIEW_SIGNAL", evaluation.candidateOutcomeComparisons().get(1).qualityImpact());
        assertTrue(evaluation.candidateOutcomeComparisons().get(1).summaryText()
                .contains("returned score delta of -2.0"));
        assertTrue(evaluation.qualityReasons().contains("SHADOW_CANDIDATE_OUTCOME_SAFER_ALTERNATIVE"));
        assertTrue(evaluation.qualityReasons().contains("ROUTE_TRADEOFF_CATEGORY_SELECTED_CHALLENGED"));
        assertTrue(evaluation.evidenceBasisSummary()
                .contains("route tradeoff SELECTED_CHALLENGED"));
        assertTrue(evaluation.selectedCandidateBasisSummary()
                .contains("closest alternative edge-b has score delta -2.0"));
    }

    @Test
    void insufficientEvidenceIsClassifiedConservatively() {
        DecisionExplorerShadowDecisionQualityEvaluationV1 evaluation = evaluate(insufficientEvidence());

        assertEquals("INSUFFICIENT_EVIDENCE", evaluation.qualityLabel());
        assertEquals("INSUFFICIENT", evaluation.qualityBand());
        assertEquals(0, evaluation.qualityScore());
        assertEquals("INSUFFICIENT", evaluation.evidenceSufficiencyLevel());
        assertEquals("UNKNOWN", evaluation.replayReadinessStatus());
        assertTrue(evaluation.qualityReasons().contains("EVIDENCE_SUFFICIENCY_INSUFFICIENT"));
        assertTrue(evaluation.unknowns().contains("route tradeoff evidence was unavailable"));
    }

    @Test
    void degradedSelectedDecisionUsesDegradedLabel() {
        DecisionExplorerShadowDecisionQualityEvaluationV1 evaluation = evaluate(degradedSelected());

        assertEquals("DEGRADED_DECISION", evaluation.qualityLabel());
        assertEquals("LOW", evaluation.qualityBand());
        assertEquals(40, evaluation.qualityScore());
        assertEquals("DEGRADED", evaluation.confidenceStatus());
        assertEquals("DEGRADED", evaluation.evidenceSufficiencyLevel());
        assertEquals("DEGRADED", evaluation.replayReadinessStatus());
        assertTrue(evaluation.selectedCandidateBasis().contains("selectedRiskLevel=HIGH"));
        assertEquals("DEGRADED_SELECTED", evaluation.candidateOutcomeComparisons().get(0).outcomeLabel());
        assertEquals("RISK_SIGNAL", evaluation.candidateOutcomeComparisons().get(0).qualityImpact());
        assertTrue(evaluation.candidateOutcomeComparisons().get(0).degradedSignals()
                .contains("health evidence state is degraded"));
        assertTrue(evaluation.qualityReasons().contains("SHADOW_DECISION_QUALITY_DEGRADED_DECISION"));
    }

    @Test
    void unknownAlternativeOutcomeIsDeterministicAndConservative() {
        DecisionExplorerShadowDecisionQualityEvaluationV1 evaluation = evaluate(unknownAlternative());

        assertEquals("REVIEW_RECOMMENDED", evaluation.qualityLabel());
        assertEquals(2, evaluation.candidateOutcomeCount());
        assertEquals("SELECTED_BASELINE", evaluation.candidateOutcomeComparisons().get(0).outcomeLabel());
        DecisionExplorerShadowCandidateOutcomeV1 unknownAlternative =
                evaluation.candidateOutcomeComparisons().get(1);
        assertEquals("edge-b", unknownAlternative.candidateId());
        assertEquals("UNKNOWN_ALTERNATIVE", unknownAlternative.outcomeLabel());
        assertEquals("UNKNOWN", unknownAlternative.qualityImpact());
        assertEquals("UNKNOWN_GAP", unknownAlternative.scoreGapCategory());
        assertTrue(unknownAlternative.unknownSignals().contains("candidate score evidence unknown"));
        assertTrue(unknownAlternative.summaryText()
                .contains("cannot be fully compared because score or diagnostic evidence is unknown"));
        assertTrue(evaluation.qualityReasons().contains("SHADOW_CANDIDATE_OUTCOME_UNKNOWN_ALTERNATIVE"));
    }

    @Test
    void nullInputsReturnUnknownWithoutInventingEvidence() {
        DecisionExplorerShadowDecisionQualityEvaluationV1 evaluation =
                service.buildEvaluation(null, null, null, null);

        assertEquals("UNKNOWN", evaluation.qualityLabel());
        assertEquals("UNKNOWN", evaluation.qualityBand());
        assertEquals(0, evaluation.qualityScore());
        assertEquals("UNKNOWN", evaluation.selectedCandidateId());
        assertEquals("UNKNOWN", evaluation.confidenceStatus());
        assertEquals("UNKNOWN", evaluation.replayReadinessStatus());
        assertEquals(0, evaluation.candidateOutcomeCount());
        assertTrue(evaluation.candidateOutcomeComparisons().isEmpty());
        assertTrue(evaluation.evidenceBasis().isEmpty());
        assertTrue(evaluation.selectedCandidateBasis().isEmpty());
        assertTrue(evaluation.unknowns().contains("shadow decision-quality input evidence was unavailable"));
        assertEquals("UNKNOWN", evaluation.boundaryNote());
    }

    @Test
    void dtoNormalizesInvalidValuesToSafeReadOnlyFallbacks() {
        DecisionExplorerShadowDecisionQualityEvaluationV1 evaluation =
                new DecisionExplorerShadowDecisionQualityEvaluationV1(
                        false,
                        false,
                        "",
                        "",
                        "INVALID",
                        "INVALID",
                        500,
                        "",
                        "INVALID",
                        "INVALID",
                        "",
                        "INVALID",
                        "INVALID",
                        -1,
                        -1,
                        -1,
                        "",
                        "",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "");

        assertTrue(evaluation.readOnly());
        assertTrue(evaluation.simulationOnly());
        assertEquals("DecisionExplorerShadowDecisionQualityEvaluationV1", evaluation.evaluationObject());
        assertEquals("v1", evaluation.contractVersion());
        assertEquals("UNKNOWN", evaluation.qualityLabel());
        assertEquals("UNKNOWN", evaluation.qualityBand());
        assertEquals(100, evaluation.qualityScore());
        assertEquals("UNKNOWN", evaluation.selectedCandidateId());
        assertEquals("UNKNOWN", evaluation.confidenceStatus());
        assertEquals("UNKNOWN", evaluation.evidenceQuality());
        assertEquals("INSUFFICIENT", evaluation.evidenceSufficiencyLevel());
        assertEquals("UNKNOWN", evaluation.replayReadinessStatus());
        assertEquals(0, evaluation.evidenceBasisCount());
        assertEquals(0, evaluation.selectedCandidateBasisCount());
        assertTrue(evaluation.candidateOutcomeComparisons().isEmpty());
        assertTrue(evaluation.evidenceBasis().isEmpty());
        assertTrue(evaluation.qualityReasons().isEmpty());
    }

    @Test
    void evaluatorSourceDoesNotUseRoutingMutationReplayStorageOrExternalServices() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                + "DecisionExplorerShadowDecisionQualityService.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerShadowDecisionQualityEvaluationV1.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerShadowCandidateOutcomeV1.java"), StandardCharsets.UTF_8);
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
                "replayexecutionavailable = true",
                "replaystorageavailable = true",
                "replayexportavailable = true",
                "production readiness proven")) {
            assertFalse(normalized.contains(forbidden), "shadow quality source must not contain " + forbidden);
        }
    }

    private DecisionExplorerShadowDecisionQualityEvaluationV1 evaluate(FoundationFixture fixture) {
        return service.buildEvaluation(fixture.summary(), fixture.diagnostics(), fixture.tradeoff(), BOUNDARY_NOTE);
    }

    private static FoundationFixture strongSelectedAdvantage() {
        return fixture(
                "STRONG",
                "COMPLETE",
                "SELECTED_ADVANTAGE",
                "REPLAY_STYLE_READY",
                100,
                "READY",
                List.of(
                        row("edge-a", true, "SELECTED_BASELINE", "BASELINE", "STRONG", "LOW", 10.0, 0.0),
                        row("edge-b", false, "ALTERNATIVE_TRAILS_SELECTED", "SELECTED_ADVANTAGE", "STRONG",
                                "LOW", 15.0, 5.0)),
                candidateDiagnostic("edge-a", true, "STRONG", "LOW", "HEALTHY", 10.0, 0.0),
                List.of(candidateDiagnostic("edge-b", false, "STRONG", "LOW", "HEALTHY", 15.0, 5.0)),
                List.of(),
                List.of());
    }

    private static FoundationFixture selectedChallenged() {
        return fixture(
                "STRONG",
                "COMPLETE",
                "SELECTED_CHALLENGED",
                "TRADEOFF_READY",
                80,
                "PARTIAL",
                List.of(
                        row("edge-a", true, "SELECTED_BASELINE", "BASELINE", "STRONG", "LOW", 10.0, 0.0),
                        row("edge-b", false, "ALTERNATIVE_BEATS_SELECTED", "RISK", "STRONG", "LOW",
                                8.0, -2.0)),
                candidateDiagnostic("edge-a", true, "STRONG", "LOW", "HEALTHY", 10.0, 0.0),
                List.of(candidateDiagnostic("edge-b", false, "STRONG", "LOW", "HEALTHY", 8.0, -2.0)),
                List.of(),
                List.of());
    }

    private static FoundationFixture insufficientEvidence() {
        DecisionExplorerConfidenceSummaryV1 summary = DecisionExplorerConfidenceSummaryV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerRoutingDiagnosticsV1 diagnostics = DecisionExplorerRoutingDiagnosticsV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerRouteTradeoffAnalysisV1 tradeoff =
                DecisionExplorerRouteTradeoffAnalysisV1.unknown(BOUNDARY_NOTE);
        return new FoundationFixture(summary, diagnostics, tradeoff);
    }

    private static FoundationFixture degradedSelected() {
        return fixture(
                "DEGRADED",
                "DEGRADED",
                "DEGRADED",
                "DEGRADED",
                60,
                "DEGRADED",
                List.of(row("edge-a", true, "SELECTED_BASELINE", "BASELINE", "DEGRADED", "HIGH",
                        10.0, 0.0, List.of("health evidence state is degraded"))),
                candidateDiagnostic("edge-a", true, "DEGRADED", "HIGH", "DEGRADED", 10.0, 0.0),
                List.of(),
                List.of("selected candidate health evidence is degraded"),
                List.of());
    }

    private static FoundationFixture unknownAlternative() {
        return fixture(
                "PARTIAL",
                "PARTIAL",
                "PARTIAL_TRADEOFF",
                "BASIC_DIAGNOSTICS_ONLY",
                55,
                "PARTIAL",
                List.of(
                        row("edge-a", true, "SELECTED_BASELINE", "BASELINE", "PARTIAL", "REVIEW", 10.0, 0.0),
                        row("edge-b", false, "ALTERNATIVE_UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN",
                                null, null, List.of(), List.of("candidate score evidence unknown"))),
                candidateDiagnostic("edge-a", true, "PARTIAL", "REVIEW", "HEALTHY", 10.0, 0.0),
                List.of(candidateDiagnostic("edge-b", false, "UNKNOWN", "UNKNOWN", "UNKNOWN", null, null)),
                List.of("alternative candidate score evidence is partial"),
                List.of("candidate score evidence unknown"));
    }

    private static FoundationFixture fixture(
            String status,
            String evidenceQuality,
            String tradeoffCategory,
            String sufficiencyLevel,
            int readinessScore,
            String replayStatus,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            DecisionExplorerCandidateDiagnosticV1 selectedDiagnostic,
            List<DecisionExplorerCandidateDiagnosticV1> alternativeDiagnostics,
            List<String> warnings,
            List<String> unknowns) {
        DecisionExplorerConfidenceSummaryV1 summary = summary(status, evidenceQuality, warnings, unknowns);
        DecisionExplorerRoutingDiagnosticsV1 diagnostics = diagnostics(
                status,
                evidenceQuality,
                selectedDiagnostic,
                alternativeDiagnostics,
                warnings,
                unknowns);
        DecisionExplorerRouteTradeoffAnalysisV1 tradeoff = tradeoff(
                status,
                evidenceQuality,
                tradeoffCategory,
                sufficiencyLevel,
                readinessScore,
                replayStatus,
                rows,
                warnings,
                unknowns);
        return new FoundationFixture(summary, diagnostics, tradeoff);
    }

    private static DecisionExplorerConfidenceSummaryV1 summary(
            String status,
            String evidenceQuality,
            List<String> warnings,
            List<String> unknowns) {
        return new DecisionExplorerConfidenceSummaryV1(
                true,
                true,
                DecisionExplorerConfidenceSummaryV1.SUMMARY_OBJECT,
                DecisionExplorerConfidenceSummaryV1.CONTRACT_VERSION,
                status,
                evidenceQuality,
                "edge-a",
                2,
                2,
                "DEGRADED".equals(status) ? 0 : 1,
                "PARTIAL".equals(status) ? 1 : 0,
                "UNKNOWN".equals(status) ? 1 : 0,
                warnings.size(),
                unknowns.size(),
                2,
                List.of(),
                List.of(),
                DecisionExplorerStatusExplanationV1.unknown(BOUNDARY_NOTE),
                List.of("confidenceStatus=" + status, "evidenceQuality=" + evidenceQuality),
                List.of("CONFIDENCE_STATUS_" + status),
                warnings,
                unknowns,
                List.of("routing-comparison-result", "decision-vector"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRoutingDiagnosticsV1 diagnostics(
            String status,
            String evidenceQuality,
            DecisionExplorerCandidateDiagnosticV1 selectedDiagnostic,
            List<DecisionExplorerCandidateDiagnosticV1> alternativeDiagnostics,
            List<String> warnings,
            List<String> unknowns) {
        List<DecisionExplorerCandidateDiagnosticV1> candidates = new java.util.ArrayList<>();
        candidates.add(selectedDiagnostic);
        candidates.addAll(alternativeDiagnostics);
        return new DecisionExplorerRoutingDiagnosticsV1(
                true,
                true,
                DecisionExplorerRoutingDiagnosticsV1.DIAGNOSTICS_OBJECT,
                DecisionExplorerRoutingDiagnosticsV1.CONTRACT_VERSION,
                status,
                evidenceQuality,
                "edge-a",
                candidates.size(),
                2,
                0,
                0,
                "DEGRADED".equals(status) ? 1 : 0,
                "UNKNOWN".equals(status) ? 1 : 0,
                List.of(),
                selectedDiagnostic,
                alternativeDiagnostics,
                candidates,
                List.of(),
                "DEGRADED".equals(status) ? List.of("selected candidate health evidence is degraded") : List.of(),
                List.of(),
                unknowns,
                "diagnostics " + status,
                List.of("DIAGNOSTICS_STATUS_" + status),
                warnings,
                unknowns,
                List.of("routing-diagnostics"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRouteTradeoffAnalysisV1 tradeoff(
            String status,
            String evidenceQuality,
            String tradeoffCategory,
            String sufficiencyLevel,
            int readinessScore,
            String replayStatus,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<String> warnings,
            List<String> unknowns) {
        DecisionExplorerRouteTradeoffRowV1 closestAlternative = rows.stream()
                .filter(row -> !row.selected())
                .findFirst()
                .orElse(null);
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(sufficiencyLevel, readinessScore);
        DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness = replayReadiness(
                replayStatus,
                sufficiency,
                readinessScore);
        return new DecisionExplorerRouteTradeoffAnalysisV1(
                true,
                true,
                DecisionExplorerRouteTradeoffAnalysisV1.ANALYSIS_OBJECT,
                DecisionExplorerRouteTradeoffAnalysisV1.CONTRACT_VERSION,
                status,
                evidenceQuality,
                "edge-a",
                tradeoffCategory,
                "selected edge-a baseline",
                closestAlternative == null ? "no alternative" : "alternative " + closestAlternative.candidateId(),
                rows.size(),
                (int) rows.stream().filter(row -> !row.selected()).count(),
                (int) rows.stream().filter(row -> !row.selected() && row.scoreDeltaFromSelected() != null).count(),
                closestAlternative == null ? "UNKNOWN" : closestAlternative.candidateId(),
                closestAlternative == null ? null : closestAlternative.scoreDeltaFromSelected(),
                rows,
                List.of(),
                List.of(),
                sufficiency,
                replayReadiness,
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "route-tradeoff|test|" + tradeoffCategory,
                "route-tradeoff:test:" + tradeoffCategory,
                "tradeoff " + tradeoffCategory,
                List.of("tradeoffCategory=" + tradeoffCategory),
                List.of("ROUTE_TRADEOFF_CATEGORY_" + tradeoffCategory),
                warnings,
                unknowns,
                List.of("route-tradeoff-analysis"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerEvidenceSufficiencyV1 sufficiency(String level, int readinessScore) {
        return new DecisionExplorerEvidenceSufficiencyV1(
                true,
                true,
                DecisionExplorerEvidenceSufficiencyV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerEvidenceSufficiencyV1.CONTRACT_VERSION,
                level,
                readinessScore,
                !DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT.equals(level),
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY.equals(level)
                        || DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY.equals(level),
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY.equals(level),
                2,
                1,
                1,
                0,
                0,
                0,
                0,
                0,
                List.of("selected candidate evidence present"),
                List.of(),
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT.equals(level)
                        ? List.of("score-comparable alternative evidence was unavailable")
                        : List.of(),
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED.equals(level)
                        ? List.of("selected candidate evidence degraded")
                        : List.of(),
                List.of(),
                List.of("EVIDENCE_SUFFICIENCY_" + level),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "evidence-sufficiency|test|" + level,
                "evidence-sufficiency:test:" + level,
                List.of("sufficiencyLevel=" + level),
                List.of("evidence-sufficiency"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness(
            String status,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            int readinessScore) {
        return new DecisionExplorerReplayReadinessDiagnosticV1(
                true,
                true,
                DecisionExplorerReplayReadinessDiagnosticV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerReplayReadinessDiagnosticV1.CONTRACT_VERSION,
                status,
                sufficiency.sufficiencyLevel(),
                readinessScore,
                false,
                false,
                false,
                "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                "UNKNOWN".equals(status) ? "UNKNOWN" : "AVAILABLE",
                List.of("candidate evidence present"),
                List.of(),
                "UNKNOWN".equals(status) ? List.of("route tradeoff evidence was unavailable") : List.of(),
                "DEGRADED".equals(status) ? List.of("selected candidate evidence degraded") : List.of(),
                List.of("replay execution, storage, and export are intentionally unavailable"),
                List.of("candidate evidence checked"),
                List.of("Replay-readiness diagnostics are read-only and do not execute replay."),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "replay-readiness|test|" + status,
                "replay-readiness:test:" + status,
                List.of("readinessStatus=" + status),
                List.of("replay-readiness"),
                "replay readiness " + status,
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRouteTradeoffRowV1 row(
            String candidateId,
            boolean selected,
            String tradeoffCategory,
            String classification,
            String status,
            String riskLevel,
            Double finalScore,
            Double scoreDelta) {
        return row(candidateId, selected, tradeoffCategory, classification, status, riskLevel, finalScore,
                scoreDelta, List.of(), List.of());
    }

    private static DecisionExplorerRouteTradeoffRowV1 row(
            String candidateId,
            boolean selected,
            String tradeoffCategory,
            String classification,
            String status,
            String riskLevel,
            Double finalScore,
            Double scoreDelta,
            List<String> degradedSignals) {
        return row(candidateId, selected, tradeoffCategory, classification, status, riskLevel, finalScore,
                scoreDelta, degradedSignals, List.of());
    }

    private static DecisionExplorerRouteTradeoffRowV1 row(
            String candidateId,
            boolean selected,
            String tradeoffCategory,
            String classification,
            String status,
            String riskLevel,
            Double finalScore,
            Double scoreDelta,
            List<String> degradedSignals,
            List<String> unknownSignals) {
        return new DecisionExplorerRouteTradeoffRowV1(
                candidateId,
                candidateId,
                selected,
                selected ? 1 : 2,
                tradeoffCategory,
                classification,
                status,
                riskLevel,
                "DEGRADED".equals(status) ? "DEGRADED" : "HEALTHY",
                finalScore,
                scoreDelta,
                DecisionExplorerRouteTradeoffRowV1.scoreGapCategoryFor(selected, scoreDelta),
                "scoring explanation",
                "evidence summary",
                selected ? List.of("selected candidate is baseline") : List.of("alternative evidence returned"),
                "RISK".equals(classification) ? List.of("alternative beats selected by returned score delta")
                        : List.of(),
                unknownSignals,
                degradedSignals,
                List.of("TRADEOFF_CATEGORY_" + tradeoffCategory),
                List.of("route-tradeoff:" + candidateId),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerCandidateDiagnosticV1 candidateDiagnostic(
            String candidateId,
            boolean selected,
            String status,
            String riskLevel,
            String healthState,
            Double finalScore,
            Double scoreDelta) {
        return new DecisionExplorerCandidateDiagnosticV1(
                candidateId,
                candidateId,
                selected,
                selected ? 1 : 2,
                selected ? "SELECTED" : "ALTERNATIVE",
                status,
                riskLevel,
                healthState,
                selected ? "SELECTED" : "COMPARED_TO_SELECTED",
                finalScore,
                scoreDelta,
                1,
                0,
                "UNKNOWN".equals(status) ? 1 : 0,
                "DEGRADED".equals(status) ? 1 : 0,
                selected ? "SELECTED_BASELINE" : "ALTERNATIVE_DELTA_PRESENT",
                "candidate diagnostic " + status,
                List.of("healthState=" + healthState),
                List.of(),
                "DEGRADED".equals(status) ? List.of("health evidence state is degraded") : List.of(),
                "UNKNOWN".equals(status) ? List.of("candidate score evidence unknown") : List.of(),
                List.of("CANDIDATE_DIAGNOSTIC_STATUS_" + status),
                List.of("candidate-diagnostic:" + candidateId),
                BOUNDARY_NOTE);
    }

    private record FoundationFixture(
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff) {
    }
}
