package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerRouteTradeoffRowBuilderTest {
    private static final String BOUNDARY_NOTE = "read-only route tradeoff diagnostics";

    private final DecisionExplorerRouteTradeoffRowBuilder builder = new DecisionExplorerRouteTradeoffRowBuilder();

    @Test
    void buildsSelectedFirstRowsWithTradeoffReasonCodesAndSignals() {
        DecisionExplorerCandidateDiagnosticV1 selected = candidate(
                "edge-a",
                true,
                3,
                10.0,
                0.0,
                DecisionExplorerCandidateDiagnosticV1.RISK_LOW,
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                0,
                0,
                List.of("selected-strength"),
                List.of(),
                List.of());
        DecisionExplorerCandidateDiagnosticV1 alternative = candidate(
                "edge-b",
                false,
                1,
                8.0,
                -2.0,
                DecisionExplorerCandidateDiagnosticV1.RISK_HIGH,
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                0,
                0,
                List.of("alternative-strength"),
                List.of("alternative-weakness"),
                List.of());

        List<DecisionExplorerRouteTradeoffRowV1> rows = builder.build(
                diagnostics(selected, List.of(alternative), List.of(selected, alternative)),
                BOUNDARY_NOTE);

        assertEquals(List.of("edge-a", "edge-b"), rows.stream()
                .map(DecisionExplorerRouteTradeoffRowV1::candidateId)
                .toList());
        DecisionExplorerRouteTradeoffRowV1 selectedRow = rows.get(0);
        assertEquals(DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE,
                selectedRow.tradeoffCategory());
        assertEquals(DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE,
                selectedRow.riskBenefitClassification());
        assertTrue(selectedRow.benefitSignals().contains("selected candidate is the comparison baseline"));
        assertTrue(selectedRow.reasonCodes().contains("TRADEOFF_CATEGORY_SELECTED_BASELINE"));

        DecisionExplorerRouteTradeoffRowV1 alternativeRow = rows.get(1);
        assertEquals(DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_BEATS_SELECTED,
                alternativeRow.tradeoffCategory());
        assertEquals(DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_RISK,
                alternativeRow.riskBenefitClassification());
        assertEquals("MATERIAL", alternativeRow.scoreGapCategory());
        assertTrue(alternativeRow.scoringExplanation().contains("score delta -2.0 from selected"));
        assertTrue(alternativeRow.riskSignals().contains("alternative beats selected by returned score delta"));
        assertTrue(alternativeRow.riskSignals().contains("candidate diagnostic risk is HIGH"));
        assertTrue(alternativeRow.reasonCodes().contains("TRADEOFF_CLASSIFICATION_RISK"));
        assertTrue(alternativeRow.reasonCodes().contains("SCORE_GAP_MATERIAL"));
        assertFalse(alternativeRow.reasonCodes().contains(""));
    }

    @Test
    void missingAlternativeScoreUsesUnknownTradeoffFallbacks() {
        DecisionExplorerCandidateDiagnosticV1 alternative = candidate(
                "edge-b",
                false,
                1,
                null,
                null,
                DecisionExplorerCandidateDiagnosticV1.RISK_UNKNOWN,
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                1,
                0,
                List.of(),
                List.of("missing score evidence"),
                List.of("score delta from selected candidate"));

        DecisionExplorerRouteTradeoffRowV1 row = builder.build(
                diagnostics(null, List.of(alternative), List.of(alternative)),
                BOUNDARY_NOTE).get(0);

        assertEquals(DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_UNKNOWN, row.tradeoffCategory());
        assertEquals(DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_UNKNOWN, row.riskBenefitClassification());
        assertEquals("UNKNOWN_GAP", row.scoreGapCategory());
        assertTrue(row.scoringExplanation()
                .contains("could not be score-compared because its score delta from selected was unavailable"));
        assertTrue(row.unknownSignals().contains("score delta from selected candidate"));
        assertTrue(row.reasonCodes().contains("TRADEOFF_CATEGORY_ALTERNATIVE_UNKNOWN"));
    }

    @Test
    void nullDiagnosticsReturnEmptyRows() {
        assertTrue(builder.build(null, BOUNDARY_NOTE).isEmpty());
    }

    private static DecisionExplorerRoutingDiagnosticsV1 diagnostics(
            DecisionExplorerCandidateDiagnosticV1 selected,
            List<DecisionExplorerCandidateDiagnosticV1> alternatives,
            List<DecisionExplorerCandidateDiagnosticV1> candidates) {
        return new DecisionExplorerRoutingDiagnosticsV1(
                true,
                true,
                DecisionExplorerRoutingDiagnosticsV1.DIAGNOSTICS_OBJECT,
                DecisionExplorerRoutingDiagnosticsV1.CONTRACT_VERSION,
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_COMPLETE,
                selected == null ? "UNKNOWN" : selected.candidateId(),
                candidates.size(),
                candidates.size(),
                0,
                0,
                0,
                0,
                List.of(),
                selected,
                alternatives,
                candidates,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "summary",
                List.of("DIAGNOSTICS_READY"),
                List.of(),
                List.of(),
                List.of("source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerCandidateDiagnosticV1 candidate(
            String candidateId,
            boolean selected,
            int displayOrder,
            Double finalScore,
            Double scoreDelta,
            String riskLevel,
            String diagnosticStatus,
            int unknownCount,
            int degradedCount,
            List<String> strengths,
            List<String> weaknesses,
            List<String> unknowns) {
        return new DecisionExplorerCandidateDiagnosticV1(
                candidateId,
                candidateId,
                selected,
                displayOrder,
                selected ? DecisionExplorerCandidateDiagnosticV1.ROLE_SELECTED
                        : DecisionExplorerCandidateDiagnosticV1.ROLE_ALTERNATIVE,
                diagnosticStatus,
                riskLevel,
                DecisionExplorerCandidateConfidenceV1.HEALTHY,
                selected ? "SELECTED" : "COMPARED_TO_SELECTED",
                finalScore,
                scoreDelta,
                strengths.size() + weaknesses.size() + unknowns.size(),
                weaknesses.size(),
                unknownCount,
                degradedCount,
                selected ? "SELECTED" : "COMPARED_TO_SELECTED",
                "candidate summary",
                strengths,
                weaknesses,
                List.of(),
                unknowns,
                List.of("CANDIDATE_" + candidateId),
                List.of("source"),
                BOUNDARY_NOTE);
    }
}
