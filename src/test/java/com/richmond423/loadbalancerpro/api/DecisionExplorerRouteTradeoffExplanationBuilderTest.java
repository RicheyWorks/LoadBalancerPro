package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerRouteTradeoffExplanationBuilderTest {
    private static final String BOUNDARY_NOTE = "read-only route tradeoff diagnostics";

    private final DecisionExplorerRouteTradeoffExplanationBuilder builder =
            new DecisionExplorerRouteTradeoffExplanationBuilder();

    @Test
    void explanationReflectsSelectedCandidateClosestAlternativeAndReadiness() {
        String explanation = builder.build(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, "edge-a"),
                row("edge-b", false, 2, 9.5, 5.0),
                "SELECTED_ADVANTAGE",
                sufficiency(DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY, 100),
                replay(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY, false),
                List.of("ROUTE_TRADEOFF_CATEGORY_SELECTED_ADVANTAGE"),
                List.of("warning-one", "warning-two"),
                List.of("hidden routing internals"),
                "route-tradeoff:v1:STRONG:edge-a:SELECTED_ADVANTAGE:rows=2:alternatives=1:"
                        + "sufficiency=REPLAY_STYLE_READY:replay=READY");

        assertEquals("Route tradeoff explanation: selected candidate edge-a is STRONG with category "
                + "SELECTED_ADVANTAGE; closest alternative edge-b has score delta 5.0; evidence sufficiency "
                + "REPLAY_STYLE_READY with readiness score 100; replay readiness READY with replay execution "
                + "unavailable; primary reason ROUTE_TRADEOFF_CATEGORY_SELECTED_ADVANTAGE; warnings 2; "
                + "unknowns 1; reproducibility key route-tradeoff:v1:STRONG:edge-a:SELECTED_ADVANTAGE:"
                + "rows=2:alternatives=1:sufficiency=REPLAY_STYLE_READY:replay=READY.", explanation);
    }

    @Test
    void missingAlternativeAndReasonsUseSafeUnknownFallbacks() {
        String explanation = builder.build(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL, "edge-a"),
                null,
                "PARTIAL_TRADEOFF",
                sufficiency(DecisionExplorerEvidenceSufficiencyV1.LEVEL_BASIC_DIAGNOSTICS_ONLY, 60),
                replay(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL, false),
                List.of(),
                null,
                null,
                "route-tradeoff:v1:PARTIAL:edge-a:PARTIAL_TRADEOFF:rows=2:alternatives=1:"
                        + "sufficiency=BASIC_DIAGNOSTICS_ONLY:replay=PARTIAL");

        assertTrue(explanation.contains("no score-comparable alternative was returned"));
        assertTrue(explanation.contains("primary reason UNKNOWN"));
        assertTrue(explanation.contains("warnings 0; unknowns 0"));
        assertTrue(explanation.contains("evidence sufficiency BASIC_DIAGNOSTICS_ONLY with readiness score 60"));
        assertTrue(explanation.contains("replay readiness PARTIAL with replay execution unavailable"));
    }

    @Test
    void nullComputedObjectsUseUnknownLabelsWithoutClaimingReplayExecution() {
        String explanation = builder.build(null, null, null, null, null, null, null, null, null);

        assertTrue(explanation.contains("selected candidate UNKNOWN is UNKNOWN with category UNKNOWN"));
        assertTrue(explanation.contains("evidence sufficiency UNKNOWN with readiness score 0"));
        assertTrue(explanation.contains("replay readiness UNKNOWN with replay execution unavailable"));
        assertTrue(explanation.contains("reproducibility key UNKNOWN"));
    }

    private static DecisionExplorerConfidenceSummaryV1 summary(String status, String selectedCandidateId) {
        return new DecisionExplorerConfidenceSummaryV1(
                true,
                true,
                DecisionExplorerConfidenceSummaryV1.SUMMARY_OBJECT,
                DecisionExplorerConfidenceSummaryV1.CONTRACT_VERSION,
                status,
                DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(status),
                selectedCandidateId,
                2,
                1,
                1,
                0,
                0,
                0,
                0,
                1,
                List.of("summary evidence"),
                List.of("SUMMARY_STATUS_" + status),
                List.of(),
                List.of(),
                List.of("summary-source"),
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
                true,
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY.equals(level),
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY.equals(level),
                2,
                1,
                1,
                1,
                0,
                0,
                0,
                0,
                List.of("candidate evidence present"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("EVIDENCE_SUFFICIENCY_LEVEL_" + level),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "evidence-sufficiency|v1|" + level,
                "evidence-sufficiency:v1:" + level,
                List.of("sufficiencyLevel=" + level, "readinessScore=" + readinessScore),
                List.of("sufficiency-source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerReplayReadinessDiagnosticV1 replay(
            String readinessStatus,
            boolean replayExecutionAvailable) {
        return new DecisionExplorerReplayReadinessDiagnosticV1(
                true,
                true,
                DecisionExplorerReplayReadinessDiagnosticV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerReplayReadinessDiagnosticV1.CONTRACT_VERSION,
                readinessStatus,
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY,
                100,
                replayExecutionAvailable,
                false,
                false,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                List.of("candidate evidence present"),
                List.of(),
                List.of(),
                List.of(),
                List.of("replay execution is intentionally unavailable"),
                List.of("candidate evidence: AVAILABLE"),
                List.of("replay execution is intentionally unavailable"),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "replay-readiness|v1|" + readinessStatus,
                "replay-readiness:v1:" + readinessStatus,
                List.of("readinessStatus=" + readinessStatus),
                List.of("replay-source"),
                "replay readiness summary",
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRouteTradeoffRowV1 row(
            String candidateId,
            boolean selected,
            int displayOrder,
            Double finalScore,
            Double scoreDelta) {
        return new DecisionExplorerRouteTradeoffRowV1(
                candidateId,
                candidateId,
                selected,
                displayOrder,
                selected ? DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE
                        : DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_TRAILS_SELECTED,
                selected ? DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE
                        : DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_TRADEOFF,
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                DecisionExplorerCandidateDiagnosticV1.RISK_LOW,
                DecisionExplorerCandidateConfidenceV1.HEALTHY,
                finalScore,
                scoreDelta,
                DecisionExplorerRouteTradeoffRowV1.scoreGapCategoryFor(selected, scoreDelta),
                "scoring explanation",
                "evidence summary",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("ROW_" + candidateId),
                List.of("row-source"),
                BOUNDARY_NOTE);
    }
}
