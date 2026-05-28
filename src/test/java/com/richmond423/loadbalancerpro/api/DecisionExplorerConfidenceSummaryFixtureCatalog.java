package com.richmond423.loadbalancerpro.api;

import java.util.List;

final class DecisionExplorerConfidenceSummaryFixtureCatalog {
    static final String BOUNDARY_NOTE = "read-only local status fixture; no production routing behavior changes";

    private DecisionExplorerConfidenceSummaryFixtureCatalog() {
    }

    static List<StatusFixture> fixtures() {
        return List.of(strongFixture(), partialFixture(), unknownFixture(), degradedFixture());
    }

    private static StatusFixture strongFixture() {
        return new StatusFixture(
                "strong-confirmed-selection",
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                "edge-a",
                "CANDIDATE_COMPARISONS_AVAILABLE",
                "candidate confidence is STRONG",
                2,
                2,
                decisionReadout("SUCCESS", "edge-a"),
                candidate("edge-a", true),
                List.of(candidate("edge-a", true), candidate("edge-b", false)),
                List.of(
                        comparison("edge-a", true, "SELECTED", List.of(), List.of("hidden routing internals")),
                        comparison("edge-b", false, "COMPARED_TO_SELECTED", List.of(),
                                List.of("hidden routing internals"))),
                List.of(
                        factor("edge-a", "healthState", "AVAILABLE", List.of(), List.of()),
                        factor("edge-b", "latency", "AVAILABLE", List.of(), List.of())),
                List.of(),
                List.of("hidden routing internals"));
    }

    private static StatusFixture partialFixture() {
        return new StatusFixture(
                "partial-candidate-and-factor-evidence",
                DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL,
                "edge-a",
                "FACTOR_STATUS_PARTIAL",
                "FACTOR_STATUS_PARTIAL",
                2,
                1,
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
                List.of(),
                List.of("hidden routing internals"));
    }

    private static StatusFixture unknownFixture() {
        return new StatusFixture(
                "unknown-no-routing-evidence",
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                "UNKNOWN",
                "NO_ROUTING_EVIDENCE_RETURNED",
                "NO_ROUTING_EVIDENCE_RETURNED",
                0,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static StatusFixture degradedFixture() {
        return new StatusFixture(
                "degraded-selected-health-evidence",
                DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED,
                "edge-a",
                "SELECTED_CANDIDATE_CONFIDENCE_DEGRADED",
                "SELECTED_CANDIDATE_CONFIDENCE_DEGRADED",
                1,
                1,
                decisionReadout("SUCCESS", "edge-a"),
                candidate("edge-a", true),
                List.of(candidate("edge-a", true)),
                List.of(comparisonWithSignals("edge-a", true, "SELECTED",
                        List.of("healthState=false"), List.of(), List.of())),
                List.of(factorWithObserved("edge-a", "healthState", "false", "AVAILABLE",
                        List.of(), List.of())),
                List.of(),
                List.of());
    }

    record StatusFixture(
            String fixtureId,
            String expectedStatus,
            String expectedSelectedCandidateId,
            String expectedPrimaryReason,
            String expectedExplanationFragment,
            int expectedCandidateConfidenceRows,
            int expectedFactorStatusRows,
            DecisionReadoutV1 decisionReadout,
            CandidateReadoutV1 selectedCandidate,
            List<CandidateReadoutV1> candidateSet,
            List<DecisionExplorerCandidateComparisonRowV1> candidateComparisons,
            List<DecisionFactorDrilldownV1> factorDrilldowns,
            List<String> warnings,
            List<String> unknowns) {
        DecisionExplorerConfidenceSummaryV1 build(DecisionExplorerConfidenceSummaryService service) {
            return service.buildSummary(
                    decisionReadout,
                    selectedCandidate,
                    candidateSet,
                    candidateComparisons,
                    factorDrilldowns,
                    warnings,
                    unknowns,
                    BOUNDARY_NOTE);
        }

        DecisionExplorerRoutingDiagnosticsV1 buildDiagnostics(
                DecisionExplorerConfidenceSummaryService summaryService,
                DecisionExplorerRoutingDiagnosticsService diagnosticsService) {
            DecisionExplorerConfidenceSummaryV1 summary = build(summaryService);
            return diagnosticsService.buildDiagnostics(
                    summary,
                    candidateSet,
                    candidateComparisons,
                    factorDrilldowns,
                    warnings,
                    unknowns,
                    BOUNDARY_NOTE);
        }
    }

    private static DecisionReadoutV1 decisionReadout(String status, String selectedCandidateId) {
        return new DecisionReadoutV1(
                "fixture-decision",
                status,
                selectedCandidateId,
                "TAIL_LATENCY_POWER_OF_TWO",
                "fixture summary",
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
        return comparisonWithSignals(
                candidateId,
                selected,
                comparisonStatus,
                List.of("healthState=healthy"),
                warnings,
                unknowns);
    }

    private static DecisionExplorerCandidateComparisonRowV1 comparisonWithSignals(
            String candidateId,
            boolean selected,
            String comparisonStatus,
            List<String> visibleSignals,
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
                visibleSignals,
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
        return factorWithObserved(candidateId, factorName, "raw value", evidenceStatus, warnings, unknowns);
    }

    private static DecisionFactorDrilldownV1 factorWithObserved(
            String candidateId,
            String factorName,
            String observedValue,
            String evidenceStatus,
            List<String> warnings,
            List<String> unknowns) {
        return new DecisionFactorDrilldownV1(
                factorName,
                candidateId,
                observedValue,
                "SUPPORTS_SELECTION",
                evidenceStatus,
                "factor explanation",
                warnings,
                unknowns,
                List.of("decision-vector:" + candidateId, "factor-contribution:" + candidateId + ":" + factorName),
                BOUNDARY_NOTE);
    }
}
