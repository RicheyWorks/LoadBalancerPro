package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerShadowPolicySensitivityEvaluatorTest {
    private static final String BOUNDARY_NOTE = "local-only shadow decision-quality evaluation";
    private final DecisionExplorerShadowPolicySensitivityEvaluator evaluator =
            new DecisionExplorerShadowPolicySensitivityEvaluator();

    @Test
    void classifiesStrongSelectedAdvantageAsLowStable() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY, 100);
        DecisionExplorerReplayReadinessDiagnosticV1 replay =
                replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY, sufficiency);

        DecisionExplorerShadowPolicySensitivityDiagnosticV1 diagnostic = evaluator.evaluate(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, List.of(), List.of()),
                diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, List.of(), List.of()),
                tradeoff("SELECTED_ADVANTAGE", DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, sufficiency, replay,
                        List.of(), List.of()),
                sufficiency,
                replay,
                List.of(
                        outcome("edge-a", DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SELECTED_BASELINE,
                                DecisionExplorerShadowCandidateOutcomeV1.IMPACT_SUPPORTS_DECISION, List.of(),
                                List.of()),
                        outcome("edge-b", DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_ACCEPTABLE_ALTERNATIVE,
                                DecisionExplorerShadowCandidateOutcomeV1.IMPACT_SUPPORTS_DECISION, List.of(),
                                List.of())),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_LOW,
                diagnostic.sensitivityLevel());
        assertEquals(DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_STABLE,
                diagnostic.sensitivityCategory());
        assertEquals(15, diagnostic.sensitivityScore());
        assertTrue(diagnostic.stableSignals().contains("selected candidate has route tradeoff advantage"));
        assertTrue(diagnostic.stableSignals()
                .contains("replay-readiness diagnostic is READY without enabling replay execution"));
        assertTrue(diagnostic.reasonCodes().contains("SHADOW_POLICY_SENSITIVITY_LOW"));
        assertTrue(diagnostic.summaryText().contains("no routing policy, scoring weights, or production routing "
                + "decision is changed"));
    }

    @Test
    void classifiesCloseOrSaferAlternativeAsMediumCloseAlternative() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY, 80);
        DecisionExplorerReplayReadinessDiagnosticV1 replay =
                replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL, sufficiency);

        DecisionExplorerShadowPolicySensitivityDiagnosticV1 diagnostic = evaluator.evaluate(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, List.of(), List.of()),
                diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, List.of(), List.of()),
                tradeoff("SELECTED_CHALLENGED", DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, sufficiency,
                        replay, List.of(), List.of()),
                sufficiency,
                replay,
                List.of(outcome("edge-b", DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SAFER_ALTERNATIVE,
                        DecisionExplorerShadowCandidateOutcomeV1.IMPACT_REVIEW_SIGNAL, List.of(), List.of())),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_MEDIUM,
                diagnostic.sensitivityLevel());
        assertEquals(DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_CLOSE_ALTERNATIVE,
                diagnostic.sensitivityCategory());
        assertEquals(55, diagnostic.sensitivityScore());
        assertTrue(diagnostic.reviewSignals().contains("candidate edge-b is a SAFER_ALTERNATIVE review signal"));
        assertTrue(diagnostic.reasonCodes().contains("SHADOW_POLICY_SENSITIVITY_CATEGORY_CLOSE_ALTERNATIVE"));
    }

    @Test
    void classifiesUnknownAlternativeAsMediumMissingEvidence() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_BASIC_DIAGNOSTICS_ONLY, 55);
        DecisionExplorerReplayReadinessDiagnosticV1 replay =
                replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL, sufficiency);

        DecisionExplorerShadowPolicySensitivityDiagnosticV1 diagnostic = evaluator.evaluate(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL, List.of(), List.of("summary unknown")),
                diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL, List.of(), List.of()),
                tradeoff("PARTIAL_TRADEOFF", DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL, sufficiency, replay,
                        List.of(), List.of("tradeoff unknown")),
                sufficiency,
                replay,
                List.of(outcome("edge-b", DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_UNKNOWN_ALTERNATIVE,
                        DecisionExplorerShadowCandidateOutcomeV1.IMPACT_UNKNOWN,
                        List.of("candidate score evidence unknown"), List.of())),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_MEDIUM,
                diagnostic.sensitivityLevel());
        assertEquals(DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_MISSING_EVIDENCE,
                diagnostic.sensitivityCategory());
        assertTrue(diagnostic.missingEvidenceSignals()
                .contains("candidate edge-b has unknown alternative outcome evidence"));
        assertTrue(diagnostic.missingEvidenceSignals().contains("summary unknown"));
        assertTrue(diagnostic.missingEvidenceSignals().contains("tradeoff unknown"));
    }

    @Test
    void classifiesDegradedSelectedEvidenceAsHighDegradedEvidence() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED, 60);
        DecisionExplorerReplayReadinessDiagnosticV1 replay =
                replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_DEGRADED, sufficiency);

        DecisionExplorerShadowPolicySensitivityDiagnosticV1 diagnostic = evaluator.evaluate(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED,
                        List.of("selected candidate health evidence is degraded"), List.of()),
                diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED,
                        List.of("selected candidate health evidence is degraded"), List.of()),
                tradeoff("DEGRADED", DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED, sufficiency, replay,
                        List.of("selected candidate health evidence is degraded"), List.of()),
                sufficiency,
                replay,
                List.of(outcome("edge-a", DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_DEGRADED_SELECTED,
                        DecisionExplorerShadowCandidateOutcomeV1.IMPACT_RISK_SIGNAL, List.of(),
                        List.of("health evidence state is degraded"))),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_HIGH,
                diagnostic.sensitivityLevel());
        assertEquals(DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_DEGRADED_EVIDENCE,
                diagnostic.sensitivityCategory());
        assertEquals(90, diagnostic.sensitivityScore());
        assertTrue(diagnostic.degradedSignals()
                .contains("selected candidate edge-a has degraded outcome evidence"));
        assertTrue(diagnostic.degradedSignals().contains("health evidence state is degraded"));
    }

    @Test
    void classifiesFullyMissingComputedEvidenceAsUnknown() {
        DecisionExplorerConfidenceSummaryV1 summary = DecisionExplorerConfidenceSummaryV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerRoutingDiagnosticsV1 diagnostics =
                DecisionExplorerRoutingDiagnosticsV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerRouteTradeoffAnalysisV1 tradeoff =
                DecisionExplorerRouteTradeoffAnalysisV1.unknown(BOUNDARY_NOTE);
        DecisionExplorerShadowPolicySensitivityDiagnosticV1 diagnostic = evaluator.evaluate(
                summary,
                diagnostics,
                tradeoff,
                tradeoff.evidenceSufficiency(),
                tradeoff.replayReadinessDiagnostic(),
                List.of(),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerShadowPolicySensitivityDiagnosticV1.LEVEL_UNKNOWN,
                diagnostic.sensitivityLevel());
        assertEquals(DecisionExplorerShadowPolicySensitivityDiagnosticV1.CATEGORY_UNKNOWN,
                diagnostic.sensitivityCategory());
        assertEquals(0, diagnostic.sensitivityScore());
    }

    private static DecisionExplorerShadowCandidateOutcomeV1 outcome(
            String candidateId,
            String outcomeLabel,
            String impact,
            List<String> unknownSignals,
            List<String> degradedSignals) {
        return new DecisionExplorerShadowCandidateOutcomeV1(
                candidateId,
                candidateId,
                DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_SELECTED_BASELINE.equals(outcomeLabel)
                        || DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_DEGRADED_SELECTED.equals(outcomeLabel),
                1,
                outcomeLabel,
                impact,
                "SELECTED_ADVANTAGE",
                "BASELINE",
                DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_DEGRADED_SELECTED.equals(outcomeLabel)
                        ? DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED
                        : DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                DecisionExplorerShadowCandidateOutcomeV1.OUTCOME_DEGRADED_SELECTED.equals(outcomeLabel)
                        ? DecisionExplorerCandidateDiagnosticV1.RISK_HIGH
                        : DecisionExplorerCandidateDiagnosticV1.RISK_LOW,
                "BASELINE",
                10.0,
                0.0,
                "candidate outcome " + outcomeLabel,
                List.of("benefit signal"),
                List.of(),
                unknownSignals,
                degradedSignals,
                List.of("SHADOW_CANDIDATE_OUTCOME_" + outcomeLabel),
                List.of("candidate-outcome:" + candidateId),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerConfidenceSummaryV1 summary(
            String status,
            List<String> warnings,
            List<String> unknowns) {
        return new DecisionExplorerConfidenceSummaryV1(
                true,
                true,
                DecisionExplorerConfidenceSummaryV1.SUMMARY_OBJECT,
                DecisionExplorerConfidenceSummaryV1.CONTRACT_VERSION,
                status,
                DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(status),
                "edge-a",
                2,
                2,
                DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(status) ? 0 : 1,
                DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL.equals(status) ? 1 : 0,
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(status) ? 1 : 0,
                warnings.size(),
                unknowns.size(),
                2,
                List.of(),
                List.of(),
                DecisionExplorerStatusExplanationV1.unknown(BOUNDARY_NOTE),
                List.of("confidenceStatus=" + status),
                List.of("CONFIDENCE_STATUS_" + status),
                warnings,
                unknowns,
                List.of("confidence-summary"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRoutingDiagnosticsV1 diagnostics(
            String status,
            List<String> degradationReasons,
            List<String> unknowns) {
        DecisionExplorerCandidateDiagnosticV1 selected =
                DecisionExplorerCandidateDiagnosticV1.unknownSelected(BOUNDARY_NOTE);
        return new DecisionExplorerRoutingDiagnosticsV1(
                true,
                true,
                DecisionExplorerRoutingDiagnosticsV1.DIAGNOSTICS_OBJECT,
                DecisionExplorerRoutingDiagnosticsV1.CONTRACT_VERSION,
                status,
                DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(status),
                "edge-a",
                1,
                1,
                0,
                0,
                degradationReasons.size(),
                unknowns.size(),
                List.of(),
                selected,
                List.of(),
                List.of(selected),
                List.of(),
                degradationReasons,
                List.of(),
                unknowns,
                "diagnostics " + status,
                List.of("DIAGNOSTICS_STATUS_" + status),
                List.of(),
                unknowns,
                List.of("routing-diagnostics"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRouteTradeoffAnalysisV1 tradeoff(
            String tradeoffCategory,
            String status,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replay,
            List<String> warnings,
            List<String> unknowns) {
        return new DecisionExplorerRouteTradeoffAnalysisV1(
                true,
                true,
                DecisionExplorerRouteTradeoffAnalysisV1.ANALYSIS_OBJECT,
                DecisionExplorerRouteTradeoffAnalysisV1.CONTRACT_VERSION,
                status,
                DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(status),
                "edge-a",
                tradeoffCategory,
                "selected edge-a baseline",
                "alternative edge-b",
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
                1,
                1,
                1,
                1,
                0,
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT.equals(level) ? 1 : 0,
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED.equals(level) ? 1 : 0,
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
            DecisionExplorerEvidenceSufficiencyV1 sufficiency) {
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
}
