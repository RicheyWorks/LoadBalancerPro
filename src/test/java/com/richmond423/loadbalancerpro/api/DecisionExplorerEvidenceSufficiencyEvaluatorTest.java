package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerEvidenceSufficiencyEvaluatorTest {
    private static final String BOUNDARY_NOTE = "read-only route tradeoff diagnostics";

    private final DecisionExplorerEvidenceSufficiencyEvaluator evaluator =
            new DecisionExplorerEvidenceSufficiencyEvaluator();

    @Test
    void completeEvidenceProducesReplayStyleReadySummaryDeterministically() {
        DecisionExplorerEvidenceSufficiencyV1 first = evaluator.build(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, "edge-a"),
                diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, 3, 0, List.of(), List.of(),
                        List.of("diagnostic-source")),
                List.of(
                        row("edge-b", false, 2, 9.5, 5.0, List.of("alternative-source")),
                        row("edge-a", true, 1, 10.0, 0.0, List.of("selected-source"))),
                List.of(
                        scoring("edge-b", false,
                                DecisionExplorerCandidateTradeoffScoringExplanationV1
                                        .SCORE_EVIDENCE_ALTERNATIVE_DELTA_PRESENT),
                        scoring("edge-a", true,
                                DecisionExplorerCandidateTradeoffScoringExplanationV1
                                        .SCORE_EVIDENCE_SELECTED_BASELINE_PRESENT)),
                List.of(delta(DecisionExplorerFactorTradeoffDeltaV1.DELTA_NEUTRAL)),
                BOUNDARY_NOTE);
        DecisionExplorerEvidenceSufficiencyV1 second = evaluator.build(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, "edge-a"),
                diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, 3, 0, List.of(), List.of(),
                        List.of("diagnostic-source")),
                List.of(
                        row("edge-b", false, 2, 9.5, 5.0, List.of("alternative-source")),
                        row("edge-a", true, 1, 10.0, 0.0, List.of("selected-source"))),
                List.of(
                        scoring("edge-b", false,
                                DecisionExplorerCandidateTradeoffScoringExplanationV1
                                        .SCORE_EVIDENCE_ALTERNATIVE_DELTA_PRESENT),
                        scoring("edge-a", true,
                                DecisionExplorerCandidateTradeoffScoringExplanationV1
                                        .SCORE_EVIDENCE_SELECTED_BASELINE_PRESENT)),
                List.of(delta(DecisionExplorerFactorTradeoffDeltaV1.DELTA_NEUTRAL)),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY,
                first.sufficiencyLevel());
        assertEquals(100, first.readinessScore());
        assertTrue(first.basicDiagnosticsReady());
        assertTrue(first.tradeoffAnalysisReady());
        assertTrue(first.replayStyleAnalysisReady());
        assertEquals(2, first.candidateEvidenceCount());
        assertEquals(1, first.comparableAlternativeCount());
        assertEquals(1, first.factorDeltaCount());
        assertEquals("evidence-sufficiency:v1:REPLAY_STYLE_READY:score=100:candidates=2:"
                + "alternatives=1:factorDeltas=1", first.reproducibilityKey());
        assertEquals(first.diagnosticFingerprint(), second.diagnosticFingerprint());
        assertEquals(first.fingerprintInputs(), second.fingerprintInputs());
        assertTrue(first.presentEvidenceSignals()
                .contains("1 score-comparable alternative(s) are present"));
        assertTrue(first.readinessReasons().contains("SCORE_EVIDENCE_COMPLETE"));
    }

    @Test
    void partialAlternativeEvidenceKeepsBasicDiagnosticsOnlyFallbacks() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = evaluator.build(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL, "edge-a"),
                diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL, 2, 0,
                        List.of("diagnostic evidence is partial"),
                        List.of("diagnostic evidence includes unknown factor state"),
                        List.of("diagnostic-source")),
                List.of(
                        row("edge-a", true, 1, 10.0, 0.0, List.of("selected-source")),
                        row("edge-b", false, 2, null, null, List.of("alternative-source"))),
                List.of(
                        scoring("edge-a", true,
                                DecisionExplorerCandidateTradeoffScoringExplanationV1
                                        .SCORE_EVIDENCE_SELECTED_BASELINE_PRESENT),
                        scoring("edge-b", false,
                                DecisionExplorerCandidateTradeoffScoringExplanationV1
                                        .SCORE_EVIDENCE_ALTERNATIVE_DELTA_UNKNOWN)),
                List.of(delta(DecisionExplorerFactorTradeoffDeltaV1.DELTA_UNKNOWN)),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerEvidenceSufficiencyV1.LEVEL_BASIC_DIAGNOSTICS_ONLY,
                sufficiency.sufficiencyLevel());
        assertEquals(60, sufficiency.readinessScore());
        assertTrue(sufficiency.basicDiagnosticsReady());
        assertFalse(sufficiency.tradeoffAnalysisReady());
        assertFalse(sufficiency.replayStyleAnalysisReady());
        assertTrue(sufficiency.partialEvidenceSignals()
                .contains("score evidence is partial for candidate edge-b"));
        assertTrue(sufficiency.partialEvidenceSignals()
                .contains("factor delta is unknown for edge-a versus edge-b on latency"));
        assertTrue(sufficiency.missingEvidenceSignals()
                .contains("score-comparable alternative evidence was not returned"));
        assertTrue(sufficiency.unknownEvidenceSignals()
                .contains("score evidence unknown for candidate edge-b"));
        assertTrue(sufficiency.readinessReasons().contains("SCORE_EVIDENCE_PARTIAL_OR_UNKNOWN"));
    }

    @Test
    void degradedEvidenceProducesDegradedSummaryWithoutInventingReadiness() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = evaluator.build(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED, "edge-a"),
                diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED, 2, 1,
                        List.of(), List.of(), List.of("diagnostic-source")),
                List.of(row("edge-a", true, 1, 10.0, 0.0, List.of("selected-source"))),
                List.of(scoring("edge-a", true,
                        DecisionExplorerCandidateTradeoffScoringExplanationV1
                                .SCORE_EVIDENCE_SELECTED_BASELINE_PRESENT)),
                List.of(delta(DecisionExplorerFactorTradeoffDeltaV1.DELTA_DEGRADED)),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED,
                sufficiency.sufficiencyLevel());
        assertEquals(55, sufficiency.readinessScore());
        assertTrue(sufficiency.basicDiagnosticsReady());
        assertFalse(sufficiency.tradeoffAnalysisReady());
        assertFalse(sufficiency.replayStyleAnalysisReady());
        assertTrue(sufficiency.degradedEvidenceSignals().contains("overall confidence status is DEGRADED"));
        assertTrue(sufficiency.degradedEvidenceSignals()
                .contains("degraded factor delta for edge-a versus edge-b on latency"));
        assertTrue(sufficiency.readinessReasons().contains("DEGRADED_EVIDENCE_PRESENT"));
    }

    @Test
    void missingEvidenceProducesInsufficientSummary() {
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = evaluator.build(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN, "UNKNOWN"),
                diagnostics(DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN, 0, 0,
                        List.of(), List.of("routing evidence unknown"), List.of()),
                List.of(),
                List.of(),
                List.of(),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT,
                sufficiency.sufficiencyLevel());
        assertEquals(0, sufficiency.readinessScore());
        assertFalse(sufficiency.basicDiagnosticsReady());
        assertFalse(sufficiency.tradeoffAnalysisReady());
        assertFalse(sufficiency.replayStyleAnalysisReady());
        assertTrue(sufficiency.missingEvidenceSignals()
                .contains("selected candidate evidence was not returned"));
        assertTrue(sufficiency.missingEvidenceSignals()
                .contains("candidate tradeoff rows were not returned"));
        assertTrue(sufficiency.missingEvidenceSignals()
                .contains("source reference evidence was not returned"));
        assertTrue(sufficiency.unknownEvidenceSignals().contains("routing evidence unknown"));
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
                "UNKNOWN".equals(selectedCandidateId) ? 0 : 2,
                "UNKNOWN".equals(selectedCandidateId) ? 0 : 1,
                1,
                0,
                0,
                0,
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(status) ? 1 : 0,
                1,
                List.of("summary evidence"),
                List.of("SUMMARY_STATUS_" + status),
                List.of(),
                List.of(),
                List.of("summary-source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRoutingDiagnosticsV1 diagnostics(
            String status,
            int diagnosticCount,
            int degradedEvidenceCount,
            List<String> partialEvidenceReasons,
            List<String> unknownEvidenceReasons,
            List<String> sourceReferenceIds) {
        return new DecisionExplorerRoutingDiagnosticsV1(
                true,
                true,
                DecisionExplorerRoutingDiagnosticsV1.DIAGNOSTICS_OBJECT,
                DecisionExplorerRoutingDiagnosticsV1.CONTRACT_VERSION,
                status,
                DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(status),
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(status) ? "UNKNOWN" : "edge-a",
                diagnosticCount,
                Math.max(0, diagnosticCount - partialEvidenceReasons.size() - degradedEvidenceCount),
                partialEvidenceReasons.size(),
                0,
                degradedEvidenceCount,
                unknownEvidenceReasons.size(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                degradedEvidenceCount > 0 ? List.of("diagnostic evidence is degraded") : List.of(),
                partialEvidenceReasons,
                unknownEvidenceReasons,
                "diagnostic summary",
                List.of("DIAGNOSTICS_STATUS_" + status),
                List.of(),
                List.of(),
                sourceReferenceIds,
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRouteTradeoffRowV1 row(
            String candidateId,
            boolean selected,
            int displayOrder,
            Double finalScore,
            Double scoreDelta,
            List<String> sourceReferenceIds) {
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
                scoreDelta == null && !selected ? List.of("score delta from selected candidate") : List.of(),
                List.of(),
                List.of("ROW_" + candidateId),
                sourceReferenceIds,
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerCandidateTradeoffScoringExplanationV1 scoring(
            String candidateId,
            boolean selected,
            String scoreEvidenceState) {
        return new DecisionExplorerCandidateTradeoffScoringExplanationV1(
                candidateId,
                candidateId,
                selected,
                selected ? 1 : 2,
                scoreEvidenceState.endsWith("_UNKNOWN")
                        ? DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN
                        : DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                scoreEvidenceState,
                selected ? "BASELINE" : "MATERIAL",
                selected ? DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE
                        : DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_TRAILS_SELECTED,
                selected ? DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE
                        : DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_TRADEOFF,
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                selected ? 10.0 : 9.5,
                selected ? 0.0 : 5.0,
                "scoring summary",
                List.of("score evidence"),
                scoreEvidenceState.endsWith("_UNKNOWN") ? List.of("score evidence is incomplete") : List.of(),
                List.of("SCORING_" + candidateId),
                List.of("scoring-source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerFactorTradeoffDeltaV1 delta(String classification) {
        return new DecisionExplorerFactorTradeoffDeltaV1(
                "latency",
                "edge-a",
                "edge-b",
                1,
                classification,
                DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_SUPPORTING,
                DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_SUPPORTING,
                DecisionExplorerFactorTradeoffDeltaV1.DELTA_DEGRADED.equals(classification)
                        ? DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED
                        : DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                "AVAILABLE",
                "AVAILABLE",
                "10ms",
                "15ms",
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_TRAILS_SELECTED,
                "MATERIAL",
                5.0,
                "factor delta summary",
                List.of("selected factor support"),
                List.of("alternative factor support"),
                limitationSignals(classification),
                List.of("FACTOR_TRADEOFF_DELTA_" + classification),
                List.of("factor-source"),
                BOUNDARY_NOTE);
    }

    private static List<String> limitationSignals(String classification) {
        if (DecisionExplorerFactorTradeoffDeltaV1.DELTA_UNKNOWN.equals(classification)) {
            return List.of("selected factor evidence was not returned");
        }
        if (DecisionExplorerFactorTradeoffDeltaV1.DELTA_DEGRADED.equals(classification)) {
            return List.of("factor tradeoff delta includes degraded evidence");
        }
        return List.of();
    }
}
