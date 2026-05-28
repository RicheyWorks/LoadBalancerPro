package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerConfidenceSummaryServiceTest {
    private static final String BOUNDARY_NOTE = "read-only simulation-only summary";
    private final DecisionExplorerConfidenceSummaryService service = new DecisionExplorerConfidenceSummaryService();

    @Test
    void strongSummaryRequiresConfirmedSelectionComparisonsAndAvailableFactors() {
        DecisionExplorerConfidenceSummaryV1 summary = service.buildSummary(
                decisionReadout("SUCCESS", "edge-a"),
                candidate("edge-a", true),
                List.of(candidate("edge-a", true), candidate("edge-b", false)),
                List.of(
                        comparison("edge-a", true, "SELECTED", List.of(), List.of("hidden routing internals")),
                        comparison("edge-b", false, "COMPARED_TO_SELECTED", List.of(),
                                List.of("hidden routing internals"))),
                List.of(factor("edge-a", "healthState", "AVAILABLE", List.of(), List.of("hidden routing internals"))),
                List.of("Decision Explorer payload is read-only and simulation-only; it does not change routing."),
                List.of("hidden routing internals", "live-cloud behavior"),
                BOUNDARY_NOTE);

        assertEquals("STRONG", summary.status());
        assertEquals("COMPLETE", summary.evidenceQuality());
        assertEquals("edge-a", summary.selectedCandidateId());
        assertEquals(2, summary.candidateCount());
        assertEquals(2, summary.candidateComparisonCount());
        assertEquals(1, summary.availableFactorCount());
        assertEquals(0, summary.warningCount());
        assertEquals(0, summary.unknownCount());
        assertEquals(List.of(
                "CANDIDATE_COMPARISONS_AVAILABLE",
                "FACTOR_EVIDENCE_AVAILABLE",
                "NO_STATUS_WARNINGS",
                "SELECTED_CANDIDATE_CONFIRMED"), summary.statusReasons());
    }

    @Test
    void partialSummaryReflectsPartialCandidateAndFactorEvidence() {
        DecisionExplorerConfidenceSummaryV1 summary = service.buildSummary(
                decisionReadout("SUCCESS", "edge-a"),
                candidate("edge-a", true),
                List.of(candidate("edge-a", true), candidate("edge-b", false)),
                List.of(
                        comparison("edge-a", true, "SELECTED", List.of(), List.of("hidden routing internals")),
                        comparison("edge-b", false, "PARTIAL_EVIDENCE",
                                List.of("candidate final score was not returned"),
                                List.of("score delta from selected candidate"))),
                List.of(factor("edge-b", "latency", "PARTIAL",
                        List.of("factor evidence is partial"),
                        List.of("numeric contribution value"))),
                List.of("Decision Explorer payload is read-only and simulation-only; it does not change routing."),
                List.of("hidden routing internals"),
                BOUNDARY_NOTE);

        assertEquals("PARTIAL", summary.status());
        assertEquals("PARTIAL", summary.evidenceQuality());
        assertEquals(1, summary.partialFactorCount());
        assertEquals(2, summary.warningCount());
        assertEquals(2, summary.unknownCount());
        assertTrue(summary.statusReasons().contains("PARTIAL_CANDIDATE_COMPARISON_EVIDENCE"));
        assertTrue(summary.statusReasons().contains("PARTIAL_FACTOR_EVIDENCE"));
        assertTrue(summary.statusReasons().contains("STATUS_WARNINGS_PRESENT"));
        assertTrue(summary.statusReasons().contains("STATUS_UNKNOWNS_PRESENT"));
    }

    @Test
    void unknownSummaryDoesNotInventEvidenceForNullPayloadParts() {
        DecisionExplorerConfidenceSummaryV1 summary = service.buildSummary(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertEquals("UNKNOWN", summary.status());
        assertEquals("UNKNOWN", summary.evidenceQuality());
        assertEquals("UNKNOWN", summary.selectedCandidateId());
        assertEquals(0, summary.candidateCount());
        assertEquals(0, summary.candidateComparisonCount());
        assertEquals(0, summary.sourceReferenceCount());
        assertEquals(List.of("NO_ROUTING_EVIDENCE_RETURNED"), summary.statusReasons());
        assertEquals("UNKNOWN", summary.boundaryNote());
    }

    @Test
    void degradedSummaryUsesReturnedFailureStatusBeforePartialEvidenceReasons() {
        DecisionExplorerConfidenceSummaryV1 summary = service.buildSummary(
                decisionReadout("FAILED", "edge-a"),
                candidate("edge-a", true),
                List.of(candidate("edge-a", true)),
                List.of(comparison("edge-a", true, "SELECTED_SCORE_UNKNOWN",
                        List.of("candidate final score was not returned"),
                        List.of("hidden routing internals"))),
                List.of(factor("edge-a", "healthState", "AVAILABLE", List.of(), List.of())),
                List.of(),
                List.of(),
                BOUNDARY_NOTE);

        assertEquals("DEGRADED", summary.status());
        assertEquals("DEGRADED", summary.evidenceQuality());
        assertEquals(List.of("DECISION_STATUS_FAILED"), summary.statusReasons());
    }

    @Test
    void returnedPartialDecisionStatusStaysPartialWhenSelectionIsConfirmed() {
        DecisionExplorerConfidenceSummaryV1 summary = service.buildSummary(
                decisionReadout("PARTIAL", "edge-a"),
                candidate("edge-a", true),
                List.of(candidate("edge-a", true)),
                List.of(comparison("edge-a", true, "SELECTED", List.of(), List.of("hidden routing internals"))),
                List.of(factor("edge-a", "healthState", "AVAILABLE", List.of(), List.of())),
                List.of(),
                List.of(),
                BOUNDARY_NOTE);

        assertEquals("PARTIAL", summary.status());
        assertEquals("PARTIAL", summary.evidenceQuality());
        assertTrue(summary.statusReasons().contains("DECISION_STATUS_PARTIAL"));
    }

    private static DecisionReadoutV1 decisionReadout(String status, String selectedCandidateId) {
        return new DecisionReadoutV1(
                "decision-1",
                status,
                selectedCandidateId,
                "TAIL_LATENCY_POWER_OF_TWO",
                "summary",
                List.of("SELECTED_CANDIDATE_RETURNED"),
                List.of("routing-comparison-result", "decision-vector"),
                BOUNDARY_NOTE);
    }

    private static CandidateReadoutV1 candidate(String candidateId, boolean selected) {
        return new CandidateReadoutV1(
                candidateId,
                candidateId,
                selected,
                selected ? "SELECTED" : "NOT_SELECTED",
                selected ? 10.0 : 15.0,
                List.of("healthState=healthy"),
                List.of("hidden routing internals"),
                List.of(selected ? "SELECTED_CANDIDATE" : "NON_SELECTED_CANDIDATE"),
                List.of("boundary-read-only", "boundary-simulation-only"),
                List.of("decision-vector:" + candidateId, "scores:" + candidateId),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerCandidateComparisonRowV1 comparison(
            String candidateId,
            boolean selected,
            String comparisonStatus,
            List<String> warnings,
            List<String> unknowns) {
        return new DecisionExplorerCandidateComparisonRowV1(
                candidateId,
                candidateId,
                selected,
                selected ? 1 : 2,
                comparisonStatus,
                selected ? 10.0 : 15.0,
                selected ? 0.0 : 5.0,
                List.of("healthState=healthy"),
                unknowns,
                List.of(selected ? "SELECTED_CANDIDATE" : "NON_SELECTED_CANDIDATE"),
                List.of("boundary-read-only", "boundary-simulation-only"),
                List.of("decision-vector:" + candidateId, "scores:" + candidateId),
                warnings,
                unknowns,
                BOUNDARY_NOTE);
    }

    private static DecisionFactorDrilldownV1 factor(
            String candidateId,
            String factorName,
            String evidenceStatus,
            List<String> warnings,
            List<String> unknowns) {
        return new DecisionFactorDrilldownV1(
                factorName,
                candidateId,
                "raw value",
                "SUPPORTS_SELECTION",
                evidenceStatus,
                "factor explanation",
                warnings,
                unknowns,
                List.of("decision-vector:" + candidateId, "factor-contribution:" + candidateId + ":" + factorName),
                BOUNDARY_NOTE);
    }
}
