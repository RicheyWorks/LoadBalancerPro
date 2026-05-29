package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerReplayReadinessEvaluatorTest {
    private static final String BOUNDARY_NOTE = "read-only route tradeoff diagnostics";

    private final DecisionExplorerReplayReadinessEvaluator evaluator =
            new DecisionExplorerReplayReadinessEvaluator();

    @Test
    void replayStyleEvidenceProducesReadyDiagnosticDeterministically() {
        DecisionExplorerReplayReadinessDiagnosticV1 first = evaluator.build(
                sufficiency(
                        DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY,
                        100,
                        true,
                        true,
                        true,
                        2,
                        1,
                        1,
                        List.of("candidate evidence present"),
                        List.of(),
                        List.of(),
                        List.of()),
                List.of(
                        row("edge-b", false, 2, 9.5, 5.0),
                        row("edge-a", true, 1, 10.0, 0.0)),
                List.of(
                        scoring("edge-b", false,
                                DecisionExplorerCandidateTradeoffScoringExplanationV1
                                        .SCORE_EVIDENCE_ALTERNATIVE_DELTA_PRESENT),
                        scoring("edge-a", true,
                                DecisionExplorerCandidateTradeoffScoringExplanationV1
                                        .SCORE_EVIDENCE_SELECTED_BASELINE_PRESENT)),
                List.of(delta(DecisionExplorerFactorTradeoffDeltaV1.DELTA_NEUTRAL)),
                diagnostics(List.of("z-source", "a-source")),
                BOUNDARY_NOTE);
        DecisionExplorerReplayReadinessDiagnosticV1 second = evaluator.build(
                sufficiency(
                        DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY,
                        100,
                        true,
                        true,
                        true,
                        2,
                        1,
                        1,
                        List.of("candidate evidence present"),
                        List.of(),
                        List.of(),
                        List.of()),
                List.of(
                        row("edge-b", false, 2, 9.5, 5.0),
                        row("edge-a", true, 1, 10.0, 0.0)),
                List.of(
                        scoring("edge-b", false,
                                DecisionExplorerCandidateTradeoffScoringExplanationV1
                                        .SCORE_EVIDENCE_ALTERNATIVE_DELTA_PRESENT),
                        scoring("edge-a", true,
                                DecisionExplorerCandidateTradeoffScoringExplanationV1
                                        .SCORE_EVIDENCE_SELECTED_BASELINE_PRESENT)),
                List.of(delta(DecisionExplorerFactorTradeoffDeltaV1.DELTA_NEUTRAL)),
                diagnostics(List.of("z-source", "a-source")),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY, first.readinessStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                first.candidateEvidenceStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                first.alternativeEvidenceStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                first.scoreEvidenceStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                first.factorEvidenceStatus());
        assertEquals("replay-readiness:v1:READY:REPLAY_STYLE_READY:score=100:candidate=AVAILABLE:"
                + "alternative=AVAILABLE:scoreEvidence=AVAILABLE:factor=AVAILABLE:fingerprint=AVAILABLE",
                first.reproducibilityKey());
        assertEquals(List.of("a-source", "z-source"), first.sourceReferenceIds());
        assertEquals(first.diagnosticFingerprint(), second.diagnosticFingerprint());
        assertEquals(first.fingerprintInputs(), second.fingerprintInputs());
        assertFalse(first.replayExecutionAvailable());
        assertFalse(first.replayStorageAvailable());
        assertFalse(first.replayExportAvailable());
        assertTrue(first.readinessChecklist().contains("replay execution: UNAVAILABLE_READ_ONLY"));
        assertTrue(first.limitationSignals()
                .contains("server-side replay export is intentionally unavailable"));
    }

    @Test
    void partialEvidenceKeepsPartialStatusAndLimitations() {
        DecisionExplorerReplayReadinessDiagnosticV1 diagnostic = evaluator.build(
                sufficiency(
                        DecisionExplorerEvidenceSufficiencyV1.LEVEL_BASIC_DIAGNOSTICS_ONLY,
                        60,
                        true,
                        false,
                        false,
                        2,
                        0,
                        1,
                        List.of("candidate evidence present"),
                        List.of("score evidence is partial for candidate edge-b"),
                        List.of("score-comparable alternative evidence was not returned"),
                        List.of()),
                List.of(
                        row("edge-a", true, 1, 10.0, 0.0),
                        row("edge-b", false, 2, null, null)),
                List.of(
                        scoring("edge-a", true,
                                DecisionExplorerCandidateTradeoffScoringExplanationV1
                                        .SCORE_EVIDENCE_SELECTED_BASELINE_PRESENT),
                        scoring("edge-b", false,
                                DecisionExplorerCandidateTradeoffScoringExplanationV1
                                        .SCORE_EVIDENCE_ALTERNATIVE_DELTA_UNKNOWN)),
                List.of(delta(DecisionExplorerFactorTradeoffDeltaV1.DELTA_UNKNOWN)),
                diagnostics(List.of("diagnostic-source")),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL,
                diagnostic.readinessStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                diagnostic.candidateEvidenceStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_PARTIAL,
                diagnostic.alternativeEvidenceStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_PARTIAL,
                diagnostic.scoreEvidenceStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_PARTIAL,
                diagnostic.factorEvidenceStatus());
        assertTrue(diagnostic.limitationSignals()
                .contains("score evidence is partial for candidate edge-b"));
        assertTrue(diagnostic.limitationSignals()
                .contains("score-comparable alternative evidence was not returned"));
        assertTrue(diagnostic.readinessChecklist().contains("factor evidence: PARTIAL"));
    }

    @Test
    void degradedEvidenceProducesDegradedReadinessWithoutReplayCapabilities() {
        DecisionExplorerReplayReadinessDiagnosticV1 diagnostic = evaluator.build(
                sufficiency(
                        DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED,
                        55,
                        true,
                        false,
                        false,
                        1,
                        0,
                        1,
                        List.of("selected candidate evidence is present"),
                        List.of(),
                        List.of("alternative candidate evidence was not returned"),
                        List.of("overall confidence status is DEGRADED")),
                List.of(row("edge-a", true, 1, 10.0, 0.0)),
                List.of(scoring("edge-a", true,
                        DecisionExplorerCandidateTradeoffScoringExplanationV1
                                .SCORE_EVIDENCE_SELECTED_BASELINE_PRESENT)),
                List.of(delta(DecisionExplorerFactorTradeoffDeltaV1.DELTA_DEGRADED)),
                diagnostics(List.of("diagnostic-source")),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_DEGRADED,
                diagnostic.readinessStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_DEGRADED,
                diagnostic.factorEvidenceStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_MISSING,
                diagnostic.alternativeEvidenceStatus());
        assertTrue(diagnostic.degradedEvidenceSignals().contains("overall confidence status is DEGRADED"));
        assertTrue(diagnostic.explanationText()
                .contains("This does not execute replay, persist replay state, export replay evidence"));
        assertFalse(diagnostic.replayExecutionAvailable());
        assertFalse(diagnostic.replayStorageAvailable());
        assertFalse(diagnostic.replayExportAvailable());
    }

    @Test
    void insufficientEvidenceProducesUnknownReadinessAndSafeEvidenceFallbacks() {
        DecisionExplorerReplayReadinessDiagnosticV1 diagnostic = evaluator.build(
                sufficiency(
                        DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT,
                        0,
                        false,
                        false,
                        false,
                        0,
                        0,
                        0,
                        List.of(),
                        List.of(),
                        List.of("candidate tradeoff rows were not returned"),
                        List.of()),
                List.of(),
                List.of(),
                List.of(),
                diagnostics(List.of()),
                BOUNDARY_NOTE);

        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN,
                diagnostic.readinessStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_UNKNOWN,
                diagnostic.candidateEvidenceStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_MISSING,
                diagnostic.alternativeEvidenceStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_UNKNOWN,
                diagnostic.scoreEvidenceStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_MISSING,
                diagnostic.factorEvidenceStatus());
        assertEquals("replay-readiness:v1:UNKNOWN:INSUFFICIENT:score=0:candidate=UNKNOWN:"
                + "alternative=MISSING:scoreEvidence=UNKNOWN:factor=MISSING:fingerprint=AVAILABLE",
                diagnostic.reproducibilityKey());
        assertTrue(diagnostic.missingEvidenceSignals()
                .contains("candidate tradeoff rows were not returned"));
    }

    @Test
    void nullEvidenceReturnsUnknownReadOnlyDiagnostic() {
        DecisionExplorerReplayReadinessDiagnosticV1 diagnostic =
                evaluator.build(null, null, null, null, null, null);

        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN,
                diagnostic.readinessStatus());
        assertEquals(DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_UNKNOWN,
                diagnostic.candidateEvidenceStatus());
        assertTrue(diagnostic.missingEvidenceSignals()
                .contains("route tradeoff evidence was unavailable"));
        assertFalse(diagnostic.replayExecutionAvailable());
        assertFalse(diagnostic.replayStorageAvailable());
        assertFalse(diagnostic.replayExportAvailable());
        assertEquals("UNKNOWN", diagnostic.boundaryNote());
    }

    private static DecisionExplorerEvidenceSufficiencyV1 sufficiency(
            String level,
            int readinessScore,
            boolean basicReady,
            boolean tradeoffReady,
            boolean replayStyleReady,
            int candidateEvidenceCount,
            int comparableAlternativeCount,
            int factorDeltaCount,
            List<String> presentSignals,
            List<String> partialSignals,
            List<String> missingSignals,
            List<String> degradedSignals) {
        return new DecisionExplorerEvidenceSufficiencyV1(
                true,
                true,
                DecisionExplorerEvidenceSufficiencyV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerEvidenceSufficiencyV1.CONTRACT_VERSION,
                level,
                readinessScore,
                basicReady,
                tradeoffReady,
                replayStyleReady,
                candidateEvidenceCount,
                comparableAlternativeCount,
                factorDeltaCount,
                presentSignals.size(),
                partialSignals.size(),
                missingSignals.size(),
                degradedSignals.size(),
                0,
                presentSignals,
                partialSignals,
                missingSignals,
                degradedSignals,
                List.of(),
                List.of("EVIDENCE_SUFFICIENCY_LEVEL_" + level),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "evidence-sufficiency|v1|" + level + "|score=" + readinessScore,
                "evidence-sufficiency:v1:" + level + ":score=" + readinessScore,
                List.of("sufficiencyLevel=" + level, "readinessScore=" + readinessScore),
                List.of("sufficiency-source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRoutingDiagnosticsV1 diagnostics(List<String> sourceReferenceIds) {
        return new DecisionExplorerRoutingDiagnosticsV1(
                true,
                true,
                DecisionExplorerRoutingDiagnosticsV1.DIAGNOSTICS_OBJECT,
                DecisionExplorerRoutingDiagnosticsV1.CONTRACT_VERSION,
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(
                        DecisionExplorerConfidenceSummaryV1.STATUS_STRONG),
                "edge-a",
                sourceReferenceIds.isEmpty() ? 0 : 2,
                sourceReferenceIds.isEmpty() ? 0 : 2,
                0,
                0,
                0,
                0,
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "diagnostic summary",
                List.of("DIAGNOSTICS_STATUS_STRONG"),
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
                scoreDelta == null && !selected ? List.of("score delta from selected candidate") : List.of(),
                List.of(),
                List.of("ROW_" + candidateId),
                List.of("row-source"),
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
