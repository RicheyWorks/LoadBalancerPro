package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerShadowQualityLabelEvaluatorTest {
    private static final String BOUNDARY_NOTE = "local-only shadow decision-quality evaluation";
    private final DecisionExplorerShadowQualityLabelEvaluator evaluator =
            new DecisionExplorerShadowQualityLabelEvaluator();

    @Test
    void labelsAcceptableDecisionFromStrongCompleteEvidence() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY, 100);

        assertEquals(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_ACCEPTABLE,
                evaluator.qualityLabel(
                        summary(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG),
                        diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG),
                        tradeoff(
                                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                                "SELECTED_ADVANTAGE",
                                sufficiency,
                                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY),
                        sufficiency,
                        replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY, sufficiency)));
    }

    @Test
    void labelsReviewRecommendedForChallengedOrPartialEvidence() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY, 80);

        assertEquals(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_REVIEW_RECOMMENDED,
                evaluator.qualityLabel(
                        summary(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG),
                        diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG),
                        tradeoff(
                                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                                "SELECTED_CHALLENGED",
                                sufficiency,
                                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL),
                        sufficiency,
                        replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL, sufficiency)));
    }

    @Test
    void labelsInsufficientEvidenceBeforeUnknownStatus() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT, 0);

        assertEquals(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_INSUFFICIENT_EVIDENCE,
                evaluator.qualityLabel(
                        summary(DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN),
                        diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN),
                        tradeoff(
                                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_UNKNOWN,
                                sufficiency,
                                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN),
                        sufficiency,
                        replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN, sufficiency)));
    }

    @Test
    void labelsDegradedDecisionBeforeUnknownStatus() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED, 60);

        assertEquals(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_DEGRADED_DECISION,
                evaluator.qualityLabel(
                        summary(DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN),
                        diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED),
                        tradeoff(
                                DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED,
                                "DEGRADED",
                                sufficiency,
                                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_DEGRADED),
                        sufficiency,
                        replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_DEGRADED, sufficiency)));
    }

    @Test
    void labelsUnknownWhenEvidenceIsNotInsufficientOrDegraded() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = sufficiency(
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY, 75);

        assertEquals(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_UNKNOWN,
                evaluator.qualityLabel(
                        summary(DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN),
                        diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG),
                        tradeoff(
                                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                                "SELECTED_ADVANTAGE",
                                sufficiency,
                                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY),
                        sufficiency,
                        replayReadiness(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY, sufficiency)));
    }

    @Test
    void qualityScoresPreserveExistingBandsAndCaps() {
        assertEquals(100, evaluator.qualityScore(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_ACCEPTABLE,
                sufficiency(DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY, 100)));
        assertEquals(85, evaluator.qualityScore(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_ACCEPTABLE,
                sufficiency(DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY, 50)));
        assertEquals(75, evaluator.qualityScore(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_REVIEW_RECOMMENDED,
                sufficiency(DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY, 90)));
        assertEquals(45, evaluator.qualityScore(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_REVIEW_RECOMMENDED,
                sufficiency(DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY, 20)));
        assertEquals(40, evaluator.qualityScore(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_DEGRADED_DECISION,
                sufficiency(DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED, 80)));
        assertEquals(15, evaluator.qualityScore(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_DEGRADED_DECISION,
                sufficiency(DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED, 5)));
        assertEquals(30, evaluator.qualityScore(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_INSUFFICIENT_EVIDENCE,
                sufficiency(DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT, 60)));
        assertEquals(0, evaluator.qualityScore(
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_UNKNOWN,
                sufficiency(DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY, 75)));
    }

    private static DecisionExplorerConfidenceSummaryV1 summary(String status) {
        return new DecisionExplorerConfidenceSummaryV1(
                true,
                true,
                DecisionExplorerConfidenceSummaryV1.SUMMARY_OBJECT,
                DecisionExplorerConfidenceSummaryV1.CONTRACT_VERSION,
                status,
                DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(status),
                "edge-a",
                1,
                1,
                1,
                0,
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(status) ? 1 : 0,
                0,
                0,
                1,
                List.of(),
                List.of(),
                DecisionExplorerStatusExplanationV1.unknown(BOUNDARY_NOTE),
                List.of("confidenceStatus=" + status),
                List.of("CONFIDENCE_STATUS_" + status),
                List.of(),
                List.of(),
                List.of("confidence-summary"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRoutingDiagnosticsV1 diagnostics(String status) {
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
                DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(status) ? 1 : 0,
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(status) ? 1 : 0,
                List.of(),
                selected,
                List.of(),
                List.of(selected),
                List.of(),
                DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(status)
                        ? List.of("selected candidate evidence degraded")
                        : List.of(),
                List.of(),
                List.of(),
                "diagnostics " + status,
                List.of("DIAGNOSTICS_STATUS_" + status),
                List.of(),
                List.of(),
                List.of("routing-diagnostics"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRouteTradeoffAnalysisV1 tradeoff(
            String status,
            String tradeoffCategory,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            String replayStatus) {
        DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness = replayReadiness(replayStatus, sufficiency);
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
                "no alternative",
                0,
                0,
                0,
                "UNKNOWN",
                null,
                List.of(),
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
                List.of(),
                List.of(),
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
                0,
                0,
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
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN.equals(status) ? "UNKNOWN" : "AVAILABLE",
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN.equals(status) ? "UNKNOWN" : "AVAILABLE",
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN.equals(status) ? "UNKNOWN" : "AVAILABLE",
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN.equals(status) ? "UNKNOWN" : "AVAILABLE",
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN.equals(status) ? "UNKNOWN" : "AVAILABLE",
                List.of("candidate evidence present"),
                List.of(),
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN.equals(status)
                        ? List.of("route tradeoff evidence was unavailable")
                        : List.of(),
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_DEGRADED.equals(status)
                        ? List.of("selected candidate evidence degraded")
                        : List.of(),
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
