package com.richmond423.loadbalancerpro.api.explain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.api.CandidateDecisionVectorResponse;
import com.richmond423.loadbalancerpro.api.DominantFactorAnalysisResponse;
import com.richmond423.loadbalancerpro.api.RoutingComparisonResponse;
import com.richmond423.loadbalancerpro.api.RoutingComparisonResultResponse;
import com.richmond423.loadbalancerpro.api.RoutingDecisionDeltaAnalysisResponse;
import com.richmond423.loadbalancerpro.api.RoutingDecisionDeltaAnalysisService;
import com.richmond423.loadbalancerpro.api.RoutingDecisionVectorResponse;
import com.richmond423.loadbalancerpro.api.RoutingDominantFactorAnalysisService;
import com.richmond423.loadbalancerpro.api.ScoreFactorContributionResponse;
import org.junit.jupiter.api.Test;

class RoutingExplanationServiceTest {
    private static final long LEGACY_DECISION_EXPLORER_BYTES = 7_784_535L;
    private static final String DECISION_FINGERPRINT =
            "sha256:v1:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final Path GOLDEN =
            Path.of("src/test/resources/api/routing-explanation-v2-golden.json");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final RoutingExplanationService service = new RoutingExplanationService();

    @Test
    void carriesCandidateFactorsAndDominantDeltaAnalysisWithoutParallelRestatements() {
        RoutingExplanation explanation = service.explain(result());

        assertEquals("TAIL_LATENCY_POWER_OF_TWO", explanation.strategyId());
        assertEquals("edge-a", explanation.selectedCandidateId());
        assertEquals(DECISION_FINGERPRINT, explanation.decisionFingerprint());
        assertEquals(List.of("edge-a", "edge-b"),
                explanation.candidates().stream().map(RoutingExplanation.CandidateFactors::candidateId).toList());
        assertEquals(List.of("errors", "latency"),
                explanation.candidates().get(0).factors().stream()
                        .map(RoutingExplanation.FactorContribution::factorName)
                        .toList());
        assertEquals("AVAILABLE", explanation.dominantFactors().status());
        assertEquals("latency",
                explanation.dominantFactors().candidates().get(0).largestAbsoluteImpact().factorName());
        assertEquals("AVAILABLE", explanation.decisionDelta().status());
        assertEquals("edge-b", explanation.decisionDelta().closestAlternativeCandidateId());
        assertEquals(-20.0d, explanation.decisionDelta().finalScoreGap());
        assertEquals(List.of("errors", "latency"),
                explanation.decisionDelta().factors().stream()
                        .map(RoutingExplanation.FactorDelta::factorName)
                        .toList());
    }

    @Test
    void counterfactualWeightScenariosProjectMinusAndPlusTenPercent() {
        List<RoutingExplanation.CounterfactualWeightScenario> scenarios =
                service.explain(result()).counterfactualWeightScenarios();

        assertEquals(List.of(
                        "errors:-10",
                        "errors:10",
                        "latency:-10",
                        "latency:10"),
                scenarios.stream()
                        .map(row -> row.factorName() + ":" + row.weightShiftPercent())
                        .toList());
        RoutingExplanation.CounterfactualWeightScenario latencyMinus = scenarios.get(2);
        assertEquals(9.0d, latencyMinus.adjustedSelectedContribution());
        assertEquals(18.0d, latencyMinus.adjustedAlternativeContribution());
        assertEquals(-9.0d, latencyMinus.adjustedContributionDelta());
        assertEquals(-19.0d, latencyMinus.projectedFinalScoreGap());
        RoutingExplanation.CounterfactualWeightScenario latencyPlus = scenarios.get(3);
        assertEquals(11.0d, latencyPlus.adjustedSelectedContribution());
        assertEquals(22.0d, latencyPlus.adjustedAlternativeContribution());
        assertEquals(-11.0d, latencyPlus.adjustedContributionDelta());
        assertEquals(-21.0d, latencyPlus.projectedFinalScoreGap());
    }

    @Test
    void counterfactualWeightScenariosOmitFactorsWhoseProjectionCannotChangeTheGap() {
        CandidateDecisionVectorResponse selected =
                candidate("edge-a", true, contribution("sameFactor", 3.0));
        CandidateDecisionVectorResponse alternative =
                candidate("edge-b", false, contribution("sameFactor", 3.0));
        RoutingDecisionVectorResponse vector = new RoutingDecisionVectorResponse(
                true, "/api/routing/compare", "fixture", "ROUND_ROBIN", "edge-a", 2,
                List.of(selected, alternative), selected, List.of(alternative), List.of(), List.of(),
                "fixture", List.of(), "local fixture", "no production proof", "available",
                "not implemented", "not implemented", "not implemented");
        RoutingExplanation explanation = service.explain(new RoutingComparisonResultResponse(
                "ROUND_ROBIN", "SUCCESS", "edge-a", "selected", List.of("edge-a", "edge-b"),
                Map.of("edge-a", 1.0, "edge-b", 2.0), DECISION_FINGERPRINT, vector,
                new RoutingDominantFactorAnalysisService().analyze(vector),
                new RoutingDecisionDeltaAnalysisService().analyze(
                        vector, Map.of("edge-a", 1.0, "edge-b", 2.0))));

        assertTrue(explanation.counterfactualWeightScenarios().isEmpty());
    }

    @Test
    void missingEvidenceStaysUnknownAndDoesNotInventCounterfactualRows() {
        RoutingExplanation explanation = service.explain(new RoutingComparisonResultResponse(
                "ROUND_ROBIN", "FAILED", null, "no decision", List.of(), Map.of(),
                null, null, null, null));

        assertEquals("UNKNOWN", explanation.selectedCandidateId());
        assertEquals("UNKNOWN", explanation.decisionFingerprint());
        assertTrue(explanation.candidates().isEmpty());
        assertEquals("UNKNOWN", explanation.dominantFactors().status());
        assertEquals("UNKNOWN", explanation.decisionDelta().status());
        assertNull(explanation.decisionDelta().finalScoreGap());
        assertTrue(explanation.counterfactualWeightScenarios().isEmpty());
        assertTrue(explanation.readOnly());
        assertTrue(explanation.simulationOnly());
    }

    @Test
    void goldenPayloadDocumentsCompactV2Contract() throws Exception {
        List<RoutingExplanation> payload = service.explain(new RoutingComparisonResponse(
                List.of("TAIL_LATENCY_POWER_OF_TWO"), 2, null, List.of(result())));
        byte[] actualBytes = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);
        JsonNode actual = OBJECT_MAPPER.readTree(actualBytes);
        JsonNode expected = OBJECT_MAPPER.readTree(Files.readAllBytes(GOLDEN));

        assertEquals(expected, actual);
        assertTrue(actualBytes.length <= LEGACY_DECISION_EXPLORER_BYTES / 10,
                () -> "compact payload should be at least an order of magnitude smaller; bytes=" + actualBytes.length);
        assertFalse(actual.toString().contains("confidenceSummary"));
        assertFalse(actual.toString().contains("routingDiagnostics"));
        assertFalse(actual.toString().contains("decisionReplaySnapshot"));
    }

    private static RoutingComparisonResultResponse result() {
        RoutingDecisionVectorResponse vector = vector();
        DominantFactorAnalysisResponse dominant = new RoutingDominantFactorAnalysisService().analyze(vector);
        RoutingDecisionDeltaAnalysisResponse delta =
                new RoutingDecisionDeltaAnalysisService().analyze(vector, Map.of("edge-a", 100.0, "edge-b", 120.0));
        return new RoutingComparisonResultResponse(
                "TAIL_LATENCY_POWER_OF_TWO",
                "SUCCESS",
                "edge-a",
                "selected",
                List.of("edge-a", "edge-b"),
                Map.of("edge-a", 100.0, "edge-b", 120.0),
                DECISION_FINGERPRINT,
                vector,
                dominant,
                delta);
    }

    private static RoutingDecisionVectorResponse vector() {
        CandidateDecisionVectorResponse selected = candidate(
                "edge-a",
                true,
                contribution("latency", 10.0),
                contribution("errors", 1.0));
        CandidateDecisionVectorResponse alternative = candidate(
                "edge-b",
                false,
                contribution("latency", 20.0),
                contribution("errors", 5.0));
        return new RoutingDecisionVectorResponse(
                true,
                "/api/routing/compare",
                "fixture",
                "TAIL_LATENCY_POWER_OF_TWO",
                "edge-a",
                2,
                List.of(selected, alternative),
                selected,
                List.of(alternative),
                List.of(),
                List.of(),
                "fixture",
                List.of(),
                "local fixture",
                "no production proof",
                "available",
                "not implemented",
                "not implemented",
                "not implemented");
    }

    private static CandidateDecisionVectorResponse candidate(
            String candidateId,
            boolean selected,
            ScoreFactorContributionResponse... factors) {
        return new CandidateDecisionVectorResponse(
                candidateId,
                selected,
                List.of(),
                List.of(),
                List.of(factors),
                "fixture",
                "fixture",
                "local fixture",
                "no production proof");
    }

    private static ScoreFactorContributionResponse contribution(String name, double value) {
        return new ScoreFactorContributionResponse(
                name,
                "fixture",
                "fixture",
                "WEAKENS_SELECTION",
                "fixture",
                value,
                "EXACT_FROM_RETURNED_EVIDENCE",
                "fixture",
                "local fixture");
    }
}
