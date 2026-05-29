package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerFactorTradeoffDeltaBuilderTest {
    private static final String BOUNDARY_NOTE = "read-only route tradeoff diagnostics";

    private final DecisionExplorerFactorTradeoffDeltaBuilder builder =
            new DecisionExplorerFactorTradeoffDeltaBuilder();

    @Test
    void buildsFactorDeltasWithDeterministicOrderingAndReasonCodes() {
        DecisionExplorerRouteTradeoffRowV1 selected = row(
                "edge-a",
                true,
                1,
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE,
                "BASELINE",
                0.0,
                List.of("selected-row-source"));
        DecisionExplorerRouteTradeoffRowV1 alternative = row(
                "edge-b",
                false,
                2,
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_CLOSE,
                "CLOSE",
                0.5,
                List.of("alternative-row-source"));

        List<DecisionExplorerFactorTradeoffDeltaV1> deltas = builder.build(
                List.of(alternative, selected),
                List.of(
                        factor("edge-b", "cost", 3, "SUPPORTING", "STRONG", 0, List.of("alternative-cost")),
                        factor("edge-a", "latency", 2, "SUPPORTING", "STRONG", 0, List.of("selected-latency")),
                        factor("edge-b", "latency", 1, "WARNING", "PARTIAL", 0, List.of("alternative-latency"))),
                BOUNDARY_NOTE);

        assertEquals(List.of("cost", "latency"), deltas.stream()
                .map(DecisionExplorerFactorTradeoffDeltaV1::factorName)
                .toList());
        DecisionExplorerFactorTradeoffDeltaV1 cost = deltas.get(0);
        assertEquals("UNKNOWN", cost.deltaClassification());
        assertEquals("edge-a", cost.selectedCandidateId());
        assertEquals("edge-b", cost.alternativeCandidateId());
        assertEquals(3, cost.displayOrder());
        assertEquals("UNKNOWN", cost.selectedContribution());
        assertEquals("SUPPORTING", cost.alternativeContribution());
        assertTrue(cost.limitationSignals().contains("selected factor evidence was not returned"));
        assertTrue(cost.reasonCodes().contains("SELECTED_FACTOR_MISSING"));
        assertTrue(cost.reasonCodes().contains("FACTOR_TRADEOFF_DELTA_UNKNOWN"));

        DecisionExplorerFactorTradeoffDeltaV1 latency = deltas.get(1);
        assertEquals("ADVANTAGE", latency.deltaClassification());
        assertEquals(1, latency.displayOrder());
        assertEquals("SUPPORTING", latency.selectedContribution());
        assertEquals("WARNING", latency.alternativeContribution());
        assertEquals("CLOSE", latency.scoreGapCategory());
        assertEquals(0.5, latency.alternativeScoreDeltaFromSelected());
        assertTrue(latency.selectedSignals().contains("selected contribution=SUPPORTING"));
        assertTrue(latency.alternativeSignals().contains("alternative warning: influenceCategory=WEAKENS_SELECTION"));
        assertTrue(latency.limitationSignals().contains("alternative warning: influenceCategory=WEAKENS_SELECTION"));
        assertTrue(latency.reasonCodes().contains("FACTOR_TRADEOFF_DELTA_ADVANTAGE"));
        assertEquals(List.of("alternative-latency", "alternative-row-source", "selected-latency"),
                latency.sourceReferenceIds());
    }

    @Test
    void classifiesDisadvantageNeutralAndDegradedWithoutChangingFallbacks() {
        DecisionExplorerRouteTradeoffRowV1 selected = row(
                "edge-a",
                true,
                1,
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE,
                "BASELINE",
                0.0,
                List.of("selected-row-source"));
        DecisionExplorerRouteTradeoffRowV1 alternative = row(
                "edge-b",
                false,
                2,
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_TRAILS_SELECTED,
                "MATERIAL",
                5.0,
                List.of("alternative-row-source"));

        List<DecisionExplorerFactorTradeoffDeltaV1> deltas = builder.build(
                List.of(selected, alternative),
                List.of(
                        factor("edge-a", "capacity", 1, "WARNING", "PARTIAL", 0, List.of("selected-capacity")),
                        factor("edge-b", "capacity", 1, "SUPPORTING", "STRONG", 0,
                                List.of("alternative-capacity")),
                        factor("edge-a", "healthState", 2, "SUPPORTING", "DEGRADED", 1,
                                List.of("selected-health")),
                        factor("edge-b", "healthState", 2, "SUPPORTING", "STRONG", 0,
                                List.of("alternative-health")),
                        factor("edge-a", "region", 3, "NEUTRAL", "STRONG", 0, List.of("selected-region")),
                        factor("edge-b", "region", 3, "NEUTRAL", "STRONG", 0, List.of("alternative-region"))),
                BOUNDARY_NOTE);

        assertEquals("DISADVANTAGE", delta(deltas, "capacity").deltaClassification());
        assertEquals("DEGRADED", delta(deltas, "healthState").deltaClassification());
        assertTrue(delta(deltas, "healthState").limitationSignals()
                .contains("factor tradeoff delta includes degraded evidence"));
        assertEquals("NEUTRAL", delta(deltas, "region").deltaClassification());
    }

    @Test
    void missingSelectedRowOrNullInputsReturnEmptyDeltas() {
        DecisionExplorerRouteTradeoffRowV1 alternative = row(
                "edge-b",
                false,
                2,
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_CLOSE,
                "CLOSE",
                0.5,
                List.of("alternative-row-source"));

        assertTrue(builder.build(null, List.of(), BOUNDARY_NOTE).isEmpty());
        assertTrue(builder.build(List.of(alternative), List.of(
                factor("edge-b", "latency", 1, "SUPPORTING", "STRONG", 0, List.of("alternative-latency"))),
                BOUNDARY_NOTE).isEmpty());
    }

    private static DecisionExplorerFactorTradeoffDeltaV1 delta(
            List<DecisionExplorerFactorTradeoffDeltaV1> deltas,
            String factorName) {
        return deltas.stream()
                .filter(delta -> factorName.equals(delta.factorName()))
                .findFirst()
                .orElseThrow();
    }

    private static DecisionExplorerRouteTradeoffRowV1 row(
            String candidateId,
            boolean selected,
            int displayOrder,
            String tradeoffCategory,
            String scoreGapCategory,
            Double scoreDelta,
            List<String> sourceReferenceIds) {
        return new DecisionExplorerRouteTradeoffRowV1(
                candidateId,
                candidateId,
                selected,
                displayOrder,
                tradeoffCategory,
                selected ? DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE
                        : DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_TRADEOFF,
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                DecisionExplorerCandidateDiagnosticV1.RISK_LOW,
                DecisionExplorerCandidateConfidenceV1.HEALTHY,
                selected ? 10.0 : 9.5,
                scoreDelta,
                scoreGapCategory,
                "scoring explanation",
                "evidence summary",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("ROW_" + candidateId),
                sourceReferenceIds,
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerFactorDiagnosticV1 factor(
            String candidateId,
            String factorName,
            int displayOrder,
            String contribution,
            String factorStatus,
            int degradedSignalCount,
            List<String> sourceReferenceIds) {
        return new DecisionExplorerFactorDiagnosticV1(
                candidateId,
                factorName,
                displayOrder,
                contribution,
                factorStatus,
                factorStatus,
                factorName + "=" + contribution,
                "WEAKENS_SELECTION",
                "REVIEW",
                DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_WARNING.equals(contribution) ? 1 : 0,
                0,
                degradedSignalCount,
                0,
                "factor summary",
                List.of("returned factor evidence"),
                DecisionExplorerFactorDiagnosticV1.CONTRIBUTION_WARNING.equals(contribution)
                        ? List.of("influenceCategory=WEAKENS_SELECTION")
                        : List.of(),
                List.of(),
                degradedSignalCount > 0 ? List.of(factorName + " evidence value is degraded") : List.of(),
                List.of("FACTOR_" + candidateId + "_" + factorName),
                sourceReferenceIds,
                BOUNDARY_NOTE);
    }
}
