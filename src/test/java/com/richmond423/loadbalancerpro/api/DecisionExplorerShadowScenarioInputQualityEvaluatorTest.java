package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerShadowScenarioInputQualityEvaluatorTest {
    private static final String BOUNDARY_NOTE = "local-only shadow decision-quality evaluation";
    private final DecisionExplorerShadowScenarioInputQualityEvaluator evaluator =
            new DecisionExplorerShadowScenarioInputQualityEvaluator();

    @Test
    void classifiesCompleteCandidateAndFactorEvidenceAsEvaluable() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY, 100, 2, 1, List.of(),
                List.of(), List.of(), List.of());
        DecisionExplorerReplayReadinessDiagnosticV1 replay =
                replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY, sufficiency, List.of(),
                        List.of(), List.of());

        DecisionExplorerShadowScenarioInputQualityV1 quality = evaluator.evaluate(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, "edge-a", 2, 1, 0, 0,
                        List.of(), List.of()),
                diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, "edge-a", 2, 1,
                        List.of(), List.of(), List.of()),
                tradeoff(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, "edge-a", "SELECTED_ADVANTAGE",
                        sufficiency, replay, List.of("route-tradeoff")),
                sufficiency,
                replay,
                List.of(
                        outcome("edge-a", true, DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SELECTED_BASELINE,
                                DecisionExplorerShadowCandidateOutcomeV1.IMPACT_SUPPORTS_DECISION, List.of(),
                                List.of()),
                        outcome("edge-b", false,
                                DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_ACCEPTABLE_ALTERNATIVE,
                                DecisionExplorerShadowCandidateOutcomeV1.IMPACT_SUPPORTS_DECISION, List.of(),
                                List.of())),
                policy(DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_LOW,
                        DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_STABLE, List.of()),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerShadowScenarioInputQualityV1.LABEL_EVALUABLE, quality.inputQualityLabel());
        assertEquals(DecisionExplorerShadowScenarioInputQualityV1.BAND_HIGH, quality.supportBand());
        assertEquals(100, quality.inputQualityScore());
        assertEquals(2, quality.candidateEvidenceCount());
        assertEquals(1, quality.factorEvidenceCount());
        assertTrue(quality.candidateInputSignals().contains("candidateOutcomeCount=2"));
        assertTrue(quality.factorInputSignals().contains("factorEvidenceCount=1"));
        assertTrue(quality.reasonCodes().contains("SHADOW_SCENARIO_INPUT_QUALITY_EVALUABLE"));
        assertTrue(quality.sourceReferenceIds().contains("route-tradeoff"));
    }

    @Test
    void classifiesUnknownAlternativeAndPartialEvidenceAsPartialInput() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_BASIC_DIAGNOSTICS_ONLY, 55, 2, 1,
                List.of("score evidence is partial"), List.of("candidate score evidence missing"), List.of(),
                List.of("score evidence unknown"));
        DecisionExplorerReplayReadinessDiagnosticV1 replay =
                replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL, sufficiency,
                        List.of("replay candidate evidence partial"), List.of("replay score evidence missing"),
                        List.of());

        DecisionExplorerShadowScenarioInputQualityV1 quality = evaluator.evaluate(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL, "edge-a", 2, 0, 1, 0,
                        List.of(), List.of("summary unknown")),
                diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL, "edge-a", 2, 1,
                        List.of(), List.of("diagnostic evidence partial"), List.of("diagnostic unknown")),
                tradeoff(DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL, "edge-a", "PARTIAL_TRADEOFF",
                        sufficiency, replay, List.of()),
                sufficiency,
                replay,
                List.of(outcome("edge-b", false,
                        DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_UNKNOWN_ALTERNATIVE,
                        DecisionExplorerShadowCandidateOutcomeV1.IMPACT_UNKNOWN,
                        List.of("candidate score evidence unknown"), List.of())),
                policy(DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_MEDIUM,
                        DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_MISSING_EVIDENCE, List.of()),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerShadowScenarioInputQualityV1.LABEL_PARTIAL_INPUT, quality.inputQualityLabel());
        assertEquals(DecisionExplorerShadowScenarioInputQualityV1.BAND_MEDIUM, quality.supportBand());
        assertEquals(55, quality.inputQualityScore());
        assertTrue(quality.partialInputSignals()
                .contains("candidate edge-b alternative comparison is unknown"));
        assertTrue(quality.partialInputSignals().contains("policy sensitivity depends on missing evidence"));
        assertTrue(quality.missingInputSignals().contains("candidate score evidence unknown"));
    }

    @Test
    void classifiesMissingCandidateEvidenceBeforeMissingFactorEvidence() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_BASIC_DIAGNOSTICS_ONLY, 40, 0, 1,
                List.of(), List.of("candidate input evidence unavailable"), List.of(), List.of());
        DecisionExplorerReplayReadinessDiagnosticV1 replay =
                replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL, sufficiency, List.of(),
                        List.of("candidate input evidence unavailable"), List.of());

        DecisionExplorerShadowScenarioInputQualityV1 quality = evaluator.evaluate(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, "UNKNOWN", 0, 1, 0, 0,
                        List.of(), List.of("candidate input evidence unavailable")),
                diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, "UNKNOWN", 0, 1,
                        List.of(), List.of(), List.of("selected candidate input identity was unavailable")),
                tradeoff(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, "UNKNOWN", "NO_ALTERNATIVE",
                        sufficiency, replay, List.of()),
                sufficiency,
                replay,
                List.of(),
                policy(DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_MEDIUM,
                        DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_MISSING_EVIDENCE, List.of()),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerShadowScenarioInputQualityV1.LABEL_MISSING_CANDIDATE_INPUT,
                quality.inputQualityLabel());
        assertEquals(DecisionExplorerShadowScenarioInputQualityV1.BAND_INSUFFICIENT, quality.supportBand());
        assertEquals(35, quality.inputQualityScore());
        assertEquals(0, quality.candidateEvidenceCount());
        assertEquals(1, quality.factorEvidenceCount());
        assertTrue(quality.missingInputSignals().contains("candidate input evidence was unavailable"));
        assertTrue(quality.missingInputSignals().contains("candidate outcome comparison rows were unavailable"));
        assertTrue(quality.reasonCodes().contains("SHADOW_SCENARIO_INPUT_CANDIDATE_EVIDENCE_MISSING"));
    }

    @Test
    void classifiesMissingFactorEvidenceSeparately() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY, 75, 2, 0,
                List.of(), List.of("factor evidence missing"), List.of(), List.of());
        DecisionExplorerReplayReadinessDiagnosticV1 replay =
                replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL, sufficiency, List.of(),
                        List.of("factor evidence missing"), List.of());

        DecisionExplorerShadowScenarioInputQualityV1 quality = evaluator.evaluate(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, "edge-a", 2, 0, 0, 0,
                        List.of(), List.of()),
                diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, "edge-a", 2, 0,
                        List.of(), List.of(), List.of()),
                tradeoff(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, "edge-a", "SELECTED_ADVANTAGE",
                        sufficiency, replay, List.of()),
                sufficiency,
                replay,
                List.of(outcome("edge-a", true,
                        DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SELECTED_BASELINE,
                        DecisionExplorerShadowCandidateOutcomeV1.IMPACT_SUPPORTS_DECISION, List.of(), List.of())),
                policy(DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_LOW,
                        DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_STABLE, List.of()),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerShadowScenarioInputQualityV1.LABEL_MISSING_FACTOR_INPUT,
                quality.inputQualityLabel());
        assertEquals(35, quality.inputQualityScore());
        assertEquals(2, quality.candidateEvidenceCount());
        assertEquals(0, quality.factorEvidenceCount());
        assertTrue(quality.missingInputSignals().contains("factor input evidence was unavailable"));
        assertTrue(quality.reasonCodes().contains("SHADOW_SCENARIO_INPUT_FACTOR_EVIDENCE_MISSING"));
    }

    @Test
    void classifiesDegradedEvidenceAsDegradedInput() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED, 60, 1, 1,
                List.of(), List.of(), List.of("health evidence state is degraded"), List.of());
        DecisionExplorerReplayReadinessDiagnosticV1 replay =
                replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_DEGRADED, sufficiency, List.of(),
                        List.of(), List.of("replay health evidence degraded"));

        DecisionExplorerShadowScenarioInputQualityV1 quality = evaluator.evaluate(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED, "edge-a", 1, 1, 0, 0,
                        List.of("selected candidate health evidence is degraded"), List.of()),
                diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED, "edge-a", 1, 1,
                        List.of("selected candidate health evidence is degraded"), List.of(), List.of()),
                tradeoff(DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED, "edge-a", "DEGRADED",
                        sufficiency, replay, List.of()),
                sufficiency,
                replay,
                List.of(outcome("edge-a", true,
                        DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_DEGRADED_SELECTED,
                        DecisionExplorerShadowCandidateOutcomeV1.IMPACT_RISK_SIGNAL, List.of(),
                        List.of("candidate outcome degraded"))),
                policy(DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_HIGH,
                        DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_DEGRADED_EVIDENCE,
                        List.of("policy degraded")),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerShadowScenarioInputQualityV1.LABEL_DEGRADED_INPUT, quality.inputQualityLabel());
        assertEquals(40, quality.inputQualityScore());
        assertTrue(quality.degradedInputSignals().contains("candidate outcome degraded"));
        assertTrue(quality.degradedInputSignals().contains("policy degraded"));
        assertTrue(quality.degradedInputSignals().contains("evidence sufficiency is DEGRADED"));
    }

    @Test
    void classifiesAllUnknownComputedEvidenceAsUnknown() {
        DecisionExplorerConfidenceSummaryV1 summary = DecisionExplorerConfidenceSummaryV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerRoutingDiagnosticsV1 diagnostics =
                DecisionExplorerRoutingDiagnosticsV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerRouteTradeoffAnalysisV1 tradeoff =
                DecisionExplorerRouteTradeoffAnalysisV1.unknown(BOUNDARY_NOTE);

        DecisionExplorerShadowScenarioInputQualityV1 quality = evaluator.evaluate(
                summary,
                diagnostics,
                tradeoff,
                tradeoff.evidenceSufficiency(),
                tradeoff.replayReadinessDiagnostic(),
                List.of(),
                DecisionExplorerShadowPolicySensitivityDiagnosticV1.unknown(BOUNDARY_NOTE),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerShadowScenarioInputQualityV1.LABEL_UNKNOWN, quality.inputQualityLabel());
        assertEquals(0, quality.inputQualityScore());
        assertEquals(0, quality.candidateEvidenceCount());
        assertEquals(0, quality.factorEvidenceCount());
    }

    private static DecisionExplorerConfidenceSummaryV1 summary(
            String status,
            String selectedCandidateId,
            int candidateCount,
            int availableFactorCount,
            int partialFactorCount,
            int unknownFactorCount,
            List<String> warnings,
            List<String> unknowns) {
        return new DecisionExplorerConfidenceSummaryV1(
                true,
                true,
                DecisionExplorerConfidenceSummaryV1.SUMMARY_OBJECT,
                DecisionExplorerConfidenceSummaryV1.CONTRACT_VERSION,
                status,
                DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(status),
                selectedCandidateId,
                candidateCount,
                Math.max(0, candidateCount - 1),
                availableFactorCount,
                partialFactorCount,
                unknownFactorCount,
                warnings.size(),
                unknowns.size(),
                1,
                List.of("confidenceStatus=" + status),
                List.of("CONFIDENCE_STATUS_" + status),
                warnings,
                unknowns,
                List.of("confidence-summary"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRoutingDiagnosticsV1 diagnostics(
            String status,
            String selectedCandidateId,
            int candidateDiagnosticCount,
            int factorDiagnosticCount,
            List<String> degradationReasons,
            List<String> partialReasons,
            List<String> unknownReasons) {
        return new DecisionExplorerRoutingDiagnosticsV1(
                true,
                true,
                DecisionExplorerRoutingDiagnosticsV1.DIAGNOSTICS_OBJECT,
                DecisionExplorerRoutingDiagnosticsV1.CONTRACT_VERSION,
                status,
                DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(status),
                selectedCandidateId,
                candidateDiagnosticCount + factorDiagnosticCount,
                candidateDiagnosticCount + factorDiagnosticCount,
                partialReasons.size(),
                unknownReasons.size(),
                degradationReasons.size(),
                unknownReasons.size(),
                List.of(),
                DecisionExplorerCandidateDiagnosticV1.unknownSelected(BOUNDARY_NOTE),
                List.of(),
                placeholderCandidates(candidateDiagnosticCount),
                placeholderFactors(factorDiagnosticCount),
                degradationReasons,
                partialReasons,
                unknownReasons,
                "diagnostics " + status,
                List.of("DIAGNOSTICS_STATUS_" + status),
                degradationReasons,
                unknownReasons,
                List.of("routing-diagnostics"),
                BOUNDARY_NOTE);
    }

    private static List<DecisionExplorerCandidateDiagnosticV1> placeholderCandidates(int count) {
        return count <= 0
                ? List.of()
                : List.of(DecisionExplorerCandidateDiagnosticV1.unknownSelected(BOUNDARY_NOTE)).subList(0, 1);
    }

    private static List<DecisionExplorerFactorDiagnosticV1> placeholderFactors(int count) {
        return count <= 0
                ? List.of()
                : List.of(new DecisionExplorerFactorDiagnosticV1(
                        "edge-a",
                        "health",
                        0,
                        DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_SUPPORTING,
                        DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                        "PRESENT",
                        "healthy",
                        "SUPPORTING",
                        "INFO",
                        0,
                        0,
                        0,
                        0,
                        "health evidence present",
                        List.of("health evidence present"),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of("FACTOR_HEALTH_PRESENT"),
                        List.of("factor:health"),
                        BOUNDARY_NOTE)).subList(0, 1);
    }

    private static DecisionExplorerEvidenceSufficiencyV1 sufficiency(
            String level,
            int score,
            int candidateEvidenceCount,
            int factorDeltaCount,
            List<String> partialSignals,
            List<String> missingSignals,
            List<String> degradedSignals,
            List<String> unknownSignals) {
        return new DecisionExplorerEvidenceSufficiencyV1(
                true,
                true,
                DecisionExplorerEvidenceSufficiencyV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerEvidenceSufficiencyV1.CONTRACT_VERSION,
                level,
                score,
                score > 0,
                score >= 50,
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY.equals(level),
                candidateEvidenceCount,
                Math.max(0, candidateEvidenceCount - 1),
                factorDeltaCount,
                candidateEvidenceCount + factorDeltaCount,
                partialSignals.size(),
                missingSignals.size(),
                degradedSignals.size(),
                unknownSignals.size(),
                List.of("present evidence"),
                partialSignals,
                missingSignals,
                degradedSignals,
                unknownSignals,
                List.of("EVIDENCE_SUFFICIENCY_" + level),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "sufficiency-" + level,
                "sufficiency:" + level,
                List.of("sufficiencyLevel=" + level),
                List.of("evidence-sufficiency"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness(
            String status,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            List<String> partialSignals,
            List<String> missingSignals,
            List<String> degradedSignals) {
        return new DecisionExplorerReplayReadinessDiagnosticV1(
                true,
                true,
                DecisionExplorerReplayReadinessDiagnosticV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerReplayReadinessDiagnosticV1.CONTRACT_VERSION,
                status,
                sufficiency.sufficiencyLevel(),
                sufficiency.readinessScore(),
                false,
                false,
                false,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                List.of("replay evidence present"),
                partialSignals,
                missingSignals,
                degradedSignals,
                List.of(),
                List.of("replay readiness " + status),
                List.of("read-only replay diagnostics"),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "replay-" + status,
                "replay:" + status,
                List.of("replayReadiness=" + status),
                List.of("replay-readiness"),
                "replay readiness " + status,
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRouteTradeoffAnalysisV1 tradeoff(
            String status,
            String selectedCandidateId,
            String tradeoffCategory,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replay,
            List<String> sourceReferenceIds) {
        return new DecisionExplorerRouteTradeoffAnalysisV1(
                true,
                true,
                DecisionExplorerRouteTradeoffAnalysisV1.ANALYSIS_OBJECT,
                DecisionExplorerRouteTradeoffAnalysisV1.CONTRACT_VERSION,
                status,
                DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(status),
                selectedCandidateId,
                tradeoffCategory,
                "selected " + selectedCandidateId,
                "alternatives",
                0,
                0,
                0,
                "UNKNOWN",
                null,
                List.of(),
                List.of(),
                List.of(),
                sufficiency,
                replay,
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "tradeoff-" + tradeoffCategory,
                "tradeoff:" + tradeoffCategory,
                "tradeoff " + tradeoffCategory,
                List.of("tradeoffCategory=" + tradeoffCategory),
                List.of("ROUTE_TRADEOFF_CATEGORY_" + tradeoffCategory),
                List.of(),
                List.of(),
                sourceReferenceIds,
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerShadowCandidateOutcomeV1 outcome(
            String candidateId,
            boolean selected,
            String outcomeLabel,
            String impact,
            List<String> unknownSignals,
            List<String> degradedSignals) {
        return new DecisionExplorerShadowCandidateOutcomeV1(
                candidateId,
                candidateId,
                selected,
                selected ? 0 : 1,
                outcomeLabel,
                impact,
                "SELECTED_ADVANTAGE",
                "BASELINE",
                DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_DEGRADED_SELECTED.equals(outcomeLabel)
                        ? DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED
                        : DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                "LOW",
                "BASELINE",
                selected ? 10.0 : 15.0,
                selected ? 0.0 : 5.0,
                "candidate " + outcomeLabel,
                List.of("benefit"),
                List.of(),
                unknownSignals,
                degradedSignals,
                List.of("SHADOW_CANDIDATE_OUTCOME_" + outcomeLabel),
                List.of("candidate-outcome:" + candidateId),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerShadowPolicySensitivityDiagnosticV1 policy(
            String level,
            String category,
            List<String> degradedSignals) {
        return new DecisionExplorerShadowPolicySensitivityDiagnosticV1(
                true,
                true,
                DecisionExplorerShadowPolicySensitivityDiagnosticV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerShadowPolicySensitivityDiagnosticV1.CONTRACT_VERSION,
                level,
                category,
                DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_LOW.equals(level) ? 15 : 55,
                "edge-a",
                "SELECTED_ADVANTAGE",
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY,
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY,
                1,
                "policy " + level,
                List.of(),
                List.of(),
                List.of(),
                degradedSignals,
                List.of("SHADOW_POLICY_SENSITIVITY_" + level),
                List.of("policy-sensitivity"),
                BOUNDARY_NOTE);
    }
}
